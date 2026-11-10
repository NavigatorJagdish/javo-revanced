/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import androidx.annotation.GuardedBy
import dalvik.system.DexClassLoader
import java.io.File
import java.io.IOException
import java.util.*

open class HandleProxyFactory(private val context: Context) {
    @GuardedBy("CLASS_LOCK")
    protected val classMap = hashMapOf<String, Class<*>>()

    fun createHandle(vmKey: String, pfd: ParcelFileDescriptor, extras: Bundle): HandleProxy {
        fetchFromFileDescriptor(pfd, vmKey)
        return createHandleProxy(vmKey, extras)
    }

    private fun fetchFromFileDescriptor(pfd: ParcelFileDescriptor, vmKey: String) {
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
            synchronized(CLASS_LOCK) {
                if (isValidCache(vmKey)) return
                val temp = File.createTempFile("droidguard-", ".apk", getCacheDir())
                try {
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > DroidGuardApkVerifier.MAX_APK_BYTES) throw IOException("DroidGuard APK is too large")
                            output.write(buffer, 0, read)
                        }
                    }
                    if (!verifyApkSignature(temp)) throw SecurityException("DroidGuard APK signature verification failed")
                    getOptDir(vmKey).mkdirs()
                    if (!temp.renameTo(getTheApkFile(vmKey))) throw IOException("Could not store DroidGuard APK")
                    updateCacheTimestamp(vmKey)
                } finally {
                    temp.delete()
                }
            }
        }
    }

    private fun createHandleProxy(
        vmKey: String,
        extras: Parcelable
    ): HandleProxy {
        val clazz = loadClass(vmKey)
        return HandleProxy(clazz, context, vmKey, extras)
    }

    fun getTheApkFile(vmKey: String) = File(getCacheDir(vmKey), "the.apk")
    protected fun getCacheDir() = context.getDir(CACHE_FOLDER_NAME, Context.MODE_PRIVATE)
    protected fun getCacheDir(vmKey: String): File {
        require(VM_KEY.matches(vmKey)) { "Invalid DroidGuard VM key" }
        return File(getCacheDir(), vmKey)
    }
    protected fun getOptDir(vmKey: String) = File(getCacheDir(vmKey), "opt")
    protected fun isValidCache(vmKey: String) = getTheApkFile(vmKey).isFile && getOptDir(vmKey).isDirectory

    protected fun updateCacheTimestamp(vmKey: String) {
        try {
            val timestampFile = File(getCacheDir(vmKey), "t")
            if (!timestampFile.exists() && !timestampFile.createNewFile()) {
                throw Exception("Failed to touch last-used file for $vmKey.")
            }
            if (!timestampFile.setLastModified(System.currentTimeMillis())) {
                throw Exception("Failed to update last-used timestamp for $vmKey.")
            }
        } catch (e: IOException) {
            throw Exception("Failed to touch last-used file for $vmKey.")
        }
    }

    private fun verifyApkSignature(apk: File) =
        DroidGuardApkVerifier.isTrusted(apk, Build.VERSION.SDK_INT)

    protected fun loadClass(vmKey: String, bytes: ByteArray = ByteArray(0)): Class<*> {
        synchronized(CLASS_LOCK) {
            val cachedClass = classMap[vmKey]
            if (cachedClass != null) {
                updateCacheTimestamp(vmKey)
                return cachedClass
            }
            val weakClass = weakClassMap[vmKey]
            if (weakClass != null) {
                classMap[vmKey] = weakClass
                updateCacheTimestamp(vmKey)
                return weakClass
            }
            if (!isValidCache(vmKey)) {
                throw BytesException(bytes, "VM key $vmKey not found in cache")
            }
            if (!verifyApkSignature(getTheApkFile(vmKey))) {
                getCacheDir(vmKey).deleteRecursively()
                throw ClassNotFoundException("APK signature verification failed")
            }
            val loader = DexClassLoader(getTheApkFile(vmKey).absolutePath, getOptDir(vmKey).absolutePath, null, context.classLoader)
            val clazz = loader.loadClass(CLASS_NAME)
            classMap[vmKey] = clazz
            weakClassMap[vmKey] = clazz
            return clazz
        }
    }

    companion object {
        const val CLASS_NAME = "com.google.ccc.abuse.droidguard.DroidGuard"
        const val CACHE_FOLDER_NAME = "cache_dg"
        private val VM_KEY = Regex("[A-Za-z0-9_-]{1,128}")
        val CLASS_LOCK = Object()
        @GuardedBy("CLASS_LOCK")
        val weakClassMap = WeakHashMap<String, Class<*>>()
    }
}
