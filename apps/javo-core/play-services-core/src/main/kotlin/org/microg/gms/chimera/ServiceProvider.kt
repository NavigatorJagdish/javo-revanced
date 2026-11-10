/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.chimera

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import org.microg.gms.DummyService
import org.microg.gms.common.GmsService
import org.microg.gms.common.RemoteListenerProxy

class ServiceProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        when (method) {
            "serviceIntentCall" -> {
                val serviceAction = extras?.getString("serviceActionBundleKey") ?: return null
                val context = context!!
                var intent = Intent(serviceAction).apply { `package` = context.packageName }
                var resolveInfo = context.packageManager.resolveService(intent, 0)
                if (resolveInfo == null && GmsService.byAction(serviceAction).ACTION != null) {
                    // Try again with action as defined in GmsService
                    val overrideAction = GmsService.byAction(serviceAction).ACTION
                    val overrideActionIntent = Intent(overrideAction).apply { `package` = context.packageName }
                    resolveInfo = context.packageManager.resolveService(overrideActionIntent, 0)
                }
                if (resolveInfo != null) {
                    intent.setClassName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)
                } else {
                    intent.setClass(context, DummyService::class.java)
                }
                return bundleOf(
                        "serviceResponseIntentKey" to intent
                )
            }
            else -> {
                return super.call(method, arg, extras)
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val cursor = MatrixCursor(COLUMNS)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return uri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String {
        return "vnd.android.cursor.item/com.google.android.gms.chimera"
    }

    companion object {
        private val COLUMNS = arrayOf("version", "apkPath", "loaderPath", "apkDescStr")
    }
}
