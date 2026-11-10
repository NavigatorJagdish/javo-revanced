/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.List;

final class DroidGuardApkVerifier {
    static final long MAX_APK_BYTES = 32L * 1024 * 1024;
    private static final byte[] PROD_CERT_SHA256 = {
        61, 122, 18, 35, 1, -102, -93, -99, -98, -96, -29, 67, 106, -73, -64, -119,
        107, -5, 79, -74, 121, -12, -34, 95, -25, -62, 63, 50, 108, -113, -103, 74
    };

    private DroidGuardApkVerifier() {}

    static boolean isTrusted(File apk, int sdk) {
        return matchesSigner(apk, sdk, PROD_CERT_SHA256);
    }

    static boolean matchesSigner(File apk, int sdk, byte[] expectedDigest) {
        if (apk.length() < 1 || apk.length() > MAX_APK_BYTES) return false;
        try {
            ApkVerifier.Result result = new ApkVerifier.Builder(apk)
                .setMinCheckedPlatformVersion(sdk)
                .setMaxCheckedPlatformVersion(sdk)
                .build()
                .verify();
            List<X509Certificate> signers = result.getSignerCertificates();
            if (!result.isVerified() || signers.size() != 1) return false;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(signers.get(0).getEncoded());
            return MessageDigest.isEqual(digest, expectedDigest);
        } catch (Exception invalidApk) {
            return false;
        }
    }
}
