package app.javo.integration

import android.content.Context
import app.javo.patcher.JavoPlaybackPatch
import app.javo.patcher.JavoUpdateLink
import app.javo.security.UpdateSecurity
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef.Companion.toMutable
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile
import dalvik.system.DexClassLoader
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONObject

/** Default CI coverage uses the real pinned extension with first-party responses. */
fun checkStreamFallbackFixture(bundle: File, folder: File, target: Context): JSONObject {
    check(UpdateSecurity.sha256(bundle) == "50f41e4656b7374a1c8fe5579fd107f697f86d7f13fc6cd194cf3821761dc4b4")
    val source = File(folder, "stream-original.dex")
    ZipFile(bundle).use { zip ->
        val entry = checkNotNull(zip.getEntry("extensions/shared.rve"))
        check(entry.size in 1..2_097_152)
        zip.getInputStream(entry).use { input -> source.outputStream().use { input.copyTo(it) } }
    }
    val original = DexFileFactory.loadDexFile(source, Opcodes.getDefault())
    val request = original.classes.single { it.type == JavoPlaybackPatch.REQUEST }.toMutable()
    JavoPlaybackPatch.patchStreamingDataClass(request)
    val help = original.classes.single { it.type == JavoUpdateLink.TYPE }.toMutable()
    JavoUpdateLink.apply(help)
    val output = File(folder, "stream-guard.dex")
    DexFileFactory.writeDexFile(output.path, ImmutableDexFile(original.opcodes,
        original.classes.filterNot { it.type == request.type || it.type == help.type } + request + help))
    check(output.setReadOnly())
    val loader = DexClassLoader(output.path, target.codeCacheDir.path, null, target.classLoader)
    return StreamFallbackProbe.runChecks(loader, target)
        .put("bundleSha256", UpdateSecurity.sha256(bundle))
        .put("transformedExtensionSha256", UpdateSecurity.sha256(output))
}
