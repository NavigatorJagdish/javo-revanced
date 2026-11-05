import app.revanced.manager.downloader.webview.ApkMirrorPolicy;

public class ApkMirrorPolicyTest {
    private static int checks;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
        System.out.println("PASS " + name);
    }
    public static void main(String[] args) {
        check(ApkMirrorPolicy.isMirror("https://downloadr2.apkmirror.com/file.apk"), "official CDN allowed");
        check(!ApkMirrorPolicy.isMirror("https://apkmirror.com.evil.test/"), "lookalike host rejected");
        check(!ApkMirrorPolicy.isMirror("intent://apkmirror.com/"), "external scheme rejected");
        check(ApkMirrorPolicy.isAd("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"), "ad script blocked");
        check(!ApkMirrorPolicy.isAd("https://challenges.cloudflare.com/turnstile/api.js"), "browser challenge permitted");
        check(!ApkMirrorPolicy.isAd("https://downloadr2.apkmirror.com/a.apk"), "APK delivery permitted");
        check(!ApkMirrorPolicy.isAd("https://notdoubleclick.net/a"), "domain boundary preserved");
        String search = "https://www.apkmirror.com/?s=com.google.android.youtube+20.40.45&post_type=app_release";
        check(ApkMirrorPolicy.startPage(search).equals("https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-40-45-release/#downloads"), "suggested YouTube version opens directly");
        check(ApkMirrorPolicy.startPage(search.replace("+", "%20")).equals(ApkMirrorPolicy.startPage(search)), "encoded search supported");
        check(ApkMirrorPolicy.startPage("https://example.com/?s=com.google.android.youtube+20.40.45").startsWith("https://example.com"), "other downloader navigation retained");
        check(ApkMirrorPolicy.startPage(search.replace("20.40.45", "../other")).equals(search.replace("20.40.45", "../other")), "untrusted version retained in search");
        check(!ApkMirrorPolicy.pageScript(false, false).contains("createElement('style')"), "ad protection can be disabled");
        System.out.println(checks + " APKMirror browser checks passed");
    }
}
