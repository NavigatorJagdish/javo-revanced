package app.javo.integration

import android.app.Activity
import android.app.Instrumentation
import android.content.pm.PackageManager
import android.os.Bundle
import app.javo.security.UpdateSecurity
import app.revanced.library.ApkUtils
import app.revanced.manager.domain.repository.PatchBundleRepository
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.patcher.runtime.ProcessRuntime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Optional local check. The proprietary input and patched output are never release assets. */
fun checkRealYoutube(instrumentation: Instrumentation, input: File) {
    val target=instrumentation.targetContext
    val folder=File(target.filesDir,"real-integration").also { it.mkdirs() }
    val report=JSONObject().put("success",false).put("playback","not-tested").put("accountLogin","not-tested")
    val started=System.currentTimeMillis()
    try {
        check(input.canonicalPath.startsWith(target.filesDir.canonicalPath+File.separator))
        report.put("managerApkSha256",UpdateSecurity.sha256(File(target.applicationInfo.sourceDir)))
            .put("sourceApkSha256",UpdateSecurity.sha256(input))
        @Suppress("DEPRECATION")
        val original=checkNotNull(target.packageManager.getPackageArchiveInfo(input.path,0))
        check(original.packageName=="com.google.android.youtube" && original.versionName=="20.40.45")
        val logger=object: Logger() {
            override fun log(level:LogLevel,message:String) {
                android.util.Log.i("JavoRealIntegration","$level: $message")
                File(folder,"progress.log").appendText("$level: $message\n")
            }
        }
        val unsigned=File(folder,"youtube-unsigned.apk")
        runBlocking {
            val repository=org.koin.core.context.GlobalContext.get().get<PatchBundleRepository>()
            for (name in listOf("official-inspected.rvp","javo-branding.rvp")) {
                repository.createLocal { instrumentation.context.assets.open(name) }
            }
            val bundles=withTimeout(60_000) { repository.bundles.first { values ->
                values.values.any { it.manifestAttributes?.name=="ReVanced Patches" } &&
                values.values.any { it.manifestAttributes?.name=="Javo resource branding" }
            } }
            val official=bundles.entries.first { it.value.manifestAttributes?.name=="ReVanced Patches" }.key
            val branding=bundles.entries.first { it.value.manifestAttributes?.name=="Javo resource branding" }.key
            val selection=mapOf(official to setOf("GmsCore support","Spoof video streams"),branding to setOf("Javo Video branding"))
            val metadata=withTimeout(60_000) { repository.bundleInfoFlow.first { it.keys.containsAll(selection.keys) } }
            for ((uid,names) in selection) {
                check(metadata.getValue(uid).patches.filter { it.supports(original.packageName,original.versionName) }
                    .map { it.name }.toSet().containsAll(names)) { "Missing or incompatible selected patches: $names" }
            }
            report.put("selectedPatches",JSONArray(selection.values.flatten()))
            withTimeout(1_200_000) {
                ProcessRuntime(target).execute(input.path,unsigned.path,"com.google.android.youtube",selection,emptyMap(),logger,{ event ->
                    File(folder,"events.log").appendText("${System.currentTimeMillis()-started}: $event\n")
                })
            }
        }
        report.put("separateProcessPatch",true)
        val signed=File(folder,"youtube-signed.apk")
        val key=File(folder,"local-test.keystore")
        ApkUtils.signApk(unsigned,signed,"Javo local integration",ApkUtils.KeyStoreDetails(key,alias="local",password="local-test"))
        @Suppress("DEPRECATION")
        val info=checkNotNull(target.packageManager.getPackageArchiveInfo(signed.path,PackageManager.GET_SIGNATURES))
        check(info.packageName=="app.revanced.android.youtube")
        val app=checkNotNull(info.applicationInfo).apply { sourceDir=signed.path;publicSourceDir=signed.path }
        check(app.loadLabel(target.packageManager).toString()=="Javo Video")
        check(!info.signatures.isNullOrEmpty())
        report.put("patchedPackage",info.packageName).put("brandingLabelVerified",true)
            .put("signedOutput",true).put("signedApkSha256",UpdateSecurity.sha256(signed)).put("success",true)
        key.delete()
    } catch(error:Throwable) {
        report.put("failure",error.stackTraceToString())
        android.util.Log.e("JavoRealIntegration","Real APK check failed",error)
    }
    report.put("elapsedSeconds",(System.currentTimeMillis()-started)/1000)
    File(folder,"result.json").writeText(report.toString(2))
    instrumentation.finish(if(report.optBoolean("success")) Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply { putString("result",report.toString()) })
}

