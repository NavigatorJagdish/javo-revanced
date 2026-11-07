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
            repository.createLocal { instrumentation.context.assets.open("official-inspected.rvp") }
            val bundles=withTimeout(60_000) { repository.bundles.first { values ->
                values.values.any { it.manifestAttributes?.name=="ReVanced Patches" }
            } }
            val official=bundles.entries.first { it.value.manifestAttributes?.name=="ReVanced Patches" }.key
            val selection=mapOf(official to setOf("GmsCore support","Spoof video streams"))
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
        val resources = target.packageManager.getResourcesForApplication(app)
        fun resource(name: String, type: String): Int =
            resources.getIdentifier(name, type, info.packageName).takeIf { it != 0 }
                ?: resources.getIdentifier(name, type, original.packageName)
        val icon = resource("javo_brand_icon", "drawable")
        check(icon != 0 && app.icon == icon)
        val settings = resource("revanced_settings_title", "string")
        check(resources.getString(settings) == "Javo Video settings")
        for (name in listOf("yt_ringo2_wordmark_header", "yt_wordmark_header", "yt_premium_wordmark_header")) {
            for (mode in listOf("light", "dark")) {
                val header = resource("${name}_$mode", "drawable")
                check(resources.getDrawable(header, null) is android.graphics.drawable.VectorDrawable)
            }
        }
        for (name in listOf("product_logo_youtube_color_144", "product_logo_youtube_2024_q4_color_144")) {
            val id = resource(name, "drawable")
            check(resources.getDrawable(id, null) is android.graphics.drawable.VectorDrawable)
        }
        check(!info.signatures.isNullOrEmpty())
        report.put("patchedPackage",info.packageName).put("brandingLabelVerified",true)
            .put("mandatorySettingsAndIconVerified",true).put("lightAndDarkHeaderVerified",true)
            .put("bundledProductLogoVerified",true)
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
