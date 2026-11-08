package app.revanced.manager.patcher

import app.revanced.library.ApkUtils.applyTo
import app.revanced.manager.patcher.Session.Companion.component1
import app.revanced.manager.patcher.Session.Companion.component2
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.logger.forStep
import app.revanced.manager.patcher.logger.withJavaLogging
import app.revanced.patcher.PatchesResult
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patcher
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import app.javo.branding.BrandingResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal typealias PatchList = List<Patch>
private typealias Patcher = (emit: (PatchResult) -> Unit) -> PatchesResult

class Session(
    cacheDir: String,
    private val frameworkDir: String,
    private val aaptPath: String,
    private val logger: Logger,
    private val input: File,
    private val onEvent: (ProgressEvent) -> Unit,
    private val minLogLevel: LogLevel,
) : Closeable {
    private val tempDir = File(cacheDir).resolve("patcher").also { it.mkdirs() }


    private suspend fun applyPatchesVerbose(
        patcher: Patcher,
        indices: Map<Patch, Int>,
        phaseLogger: Logger = logger,
    ) =
        withContext(
            Dispatchers.Default
        ) {
            val context = currentCoroutineContext()
            patcher { (patch, exception) ->
                // Make the patching process cancelable.
                context.ensureActive()

                val index = indices[patch]

                if (exception != null) {
                    onEvent(
                        ProgressEvent.Failed(
                            index?.let { StepId.ExecutePatch(it) } ?: StepId.ExecutePatches,
                            exception.toRemoteError(),
                        )
                    )
                    phaseLogger.error("${patch.name ?: "Javo branding or a required patch"} failed:")
                    phaseLogger.error(exception.stackTraceToString())
                    throw exception
                }

                if (index == null) return@patcher
                onEvent(
                    ProgressEvent.Completed(
                        StepId.ExecutePatch(index),
                    )
                )
            }
        }

    suspend fun run(output: File, selectedPatches: PatchList) {
        require(selectedPatches.isNotEmpty()) { "No selected patches loaded. Re-import a compatible patch bundle." }
        val indices = HashMap<Patch, Int>(selectedPatches.size)
        selectedPatches.forEachIndexed { idx, patch -> indices[patch] = idx }
        val prepareLogger = logger.forStep(StepId.ReadAPK, minLogLevel, onEvent)
        val patchingLogger = logger.forStep(StepId.ExecutePatches, minLogLevel, onEvent)
        val writingLogger = logger.forStep(StepId.WriteAPK, minLogLevel, onEvent)

        val patcher = runStep(StepId.ReadAPK, onEvent) {
            withPatcherActivity(StepId.ReadAPK, "Preparing APK", onEvent) {
                prepareLogger.withJavaLogging {
                    patcher(
                        apkFile = input,
                        temporaryFilesPath = tempDir,
                        frameworkFileDirectory = frameworkDir,
                        aaptBinaryPath = File(aaptPath)
                    ) { packageName, _ ->
                        val product = when (packageName) {
                            "com.google.android.youtube" -> "Video"
                            "com.google.android.apps.youtube.music" -> "Music"
                            else -> null
                        }
                        if (product == null) selectedPatches.toSet()
                        else {
                            val label = "Javo $product"
                            // The unnamed root sorts first and its finalizer runs last,
                            // after upstream settings and GmsCore have written resources.
                            val branding = resourcePatch {
                                afterDependents {
                                    patchingLogger.info("Applying $label launcher and interface branding")
                                    val file = "/javo-${product.lowercase(java.util.Locale.ROOT)}.xml"
                                    val icon = checkNotNull(BrandingResources::class.java.getResourceAsStream(file))
                                    icon.use {
                                        BrandingResources.apply(get("").toPath(), packageName, label, it)
                                    }
                                    BrandingResources.applyInterface(get("res").toPath(), packageName, label)
                                }
                            }
                            val conflicting = setOf("Javo Video branding", "Javo Music branding", "Custom branding", "Change header")
                            selectedPatches.filter { it.name in conflicting }.forEach { patch ->
                                patchingLogger.info("Using built-in Javo branding in place of ${patch.name}")
                                indices[patch]?.let { onEvent(ProgressEvent.Completed(StepId.ExecutePatch(it))) }
                            }
                            val defaults = if (product == "Video") videoDefaults(selectedPatches, patchingLogger) else null
                            setOfNotNull(branding, defaults) + selectedPatches.filterNot { it.name in conflicting }
                        }
                    }
                }
            }
        }
        val result = runStep(StepId.ExecutePatches, onEvent) {
            withPatcherActivity(StepId.ExecutePatches, "Applying patches", onEvent) {
                patchingLogger.withJavaLogging {
                    patchingLogger.info("Applying patches...")
                    applyPatchesVerbose(patcher, indices, patchingLogger)
                }
            }
        }
        runStep(StepId.WriteAPK, onEvent) {
            withPatcherActivity(StepId.WriteAPK, "Writing APK", onEvent) {
                writingLogger.withJavaLogging {
                    writingLogger.info("Writing patched files...")
                    val patched = tempDir.resolve("result.apk")
                    withContext(Dispatchers.IO) {
                        Files.copy(input.toPath(), patched.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                    result.applyTo(patched)
                    writingLogger.info("Patched apk saved to $patched")
                    withContext(Dispatchers.IO) {
                        Files.move(patched.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    private fun videoDefaults(selected: PatchList, log: Logger): Patch? {
        val downloads = selected.singleOrNull { it.name == "Downloads" }
        val quality = selected.singleOrNull { it.name == "Video quality" }
        if (downloads == null && quality == null) return null
        return bytecodePatch {
            afterDependents {
                val settings = classDefs["Lapp/revanced/extension/youtube/settings/Settings;"]
                val initializer = settings?.let { classDefs.getOrReplaceMutable(it) }
                    ?.methods?.singleOrNull { it.name == "<clinit>" }
                val code = initializer?.implementation?.instructions
                if (initializer != null && code != null) {
                    fun indexOfString(value: String) = code.indices.filter { index ->
                        ((code[index] as? ReferenceInstruction)?.reference as? StringReference)?.string == value
                    }.singleOrNull()

                    fun settingDefault(key: String, field: String, type: String, boxed: String,
                        before: (Int) -> String, after: (Int) -> String): Boolean {
                        val index = indexOfString(key)?.plus(1) ?: return false
                        val call = code.getOrNull(index) ?: return false
                        val args = call as? FiveRegisterInstruction ?: return false
                        val method = (call as? ReferenceInstruction)?.reference as? MethodReference ?: return false
                        val output = (code.getOrNull(index + 1) as? ReferenceInstruction)?.reference as? FieldReference
                        if (call.opcode != Opcode.INVOKE_DIRECT || args.registerCount != 3 ||
                            method.name != "<init>" || method.definingClass != "Lapp/revanced/extension/shared/settings/$type;" ||
                            method.parameterTypes.map { it.toString() } != listOf("Ljava/lang/String;", "Ljava/lang/$boxed;") ||
                            output?.definingClass != settings?.type || output?.name != field) return false
                        val value = args.registerE
                        initializer.addInstructions(index + 1, after(value))
                        initializer.addInstructions(index, before(value))
                        return true
                    }

                    if (downloads != null) {
                        val slot = indexOfString("com.deniscerri.ytdl")
                        val register = slot?.let { (code[it] as? OneRegisterInstruction)?.registerA }
                        if (slot != null && register != null) {
                            initializer.replaceInstruction(slot, "const-string/jumbo v$register, \"app.javo.manager\"")
                            log.info("Javo Manager is the default video saver")
                            if (settingDefault("revanced_external_downloader_action_button", "EXTERNAL_DOWNLOADER_ACTION_BUTTON",
                                    "BooleanSetting", "Boolean",
                                    { "sget-object v$it, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;" },
                                    { "sget-object v$it, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;" })) {
                                log.info("Javo Video download action is enabled")
                            }
                        }
                    }
                    if (quality != null) {
                        fun boxedInt(register: Int, value: Int) = """
                            const/16 v$register, $value
                            invoke-static {v$register}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                            move-result-object v$register
                        """.trimIndent()
                        if (settingDefault("revanced_video_quality_default_mobile", "VIDEO_QUALITY_DEFAULT_MOBILE",
                                "IntegerSetting", "Integer", { boxedInt(it, 480) }, { boxedInt(it, -2) })) {
                            log.info("Javo cellular playback defaults to 480p")
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        tempDir.deleteRecursively()
    }

    companion object {
        operator fun PatchResult.component1() = patch
        operator fun PatchResult.component2() = exception
    }
}
