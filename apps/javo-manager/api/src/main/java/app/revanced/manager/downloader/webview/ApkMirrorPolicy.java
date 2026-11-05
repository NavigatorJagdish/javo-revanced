package app.revanced.manager.downloader.webview;

import java.net.URI;
import java.util.Locale;

/** APKMirror navigation and resource filtering shared by the in-app browser and tests. */
public final class ApkMirrorPolicy {
    private ApkMirrorPolicy() {}

    public static boolean isMirror(String address) {
        try {
            URI uri = URI.create(address);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null &&
                    (host.equalsIgnoreCase("apkmirror.com") || host.toLowerCase(Locale.ROOT).endsWith(".apkmirror.com"));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public static boolean isAd(String address) {
        String host;
        try { host = URI.create(address).getHost(); }
        catch (IllegalArgumentException error) { return false; }
        if (host == null) return false;
        host = host.toLowerCase(Locale.ROOT);
        for (String domain : new String[]{"doubleclick.net", "googlesyndication.com", "googleadservices.com",
                "amazon-adsystem.com", "adnxs.com", "adsrvr.org", "pubmatic.com", "rubiconproject.com",
                "openx.net", "casalemedia.com", "criteo.com", "criteo.net", "quantserve.com", "scorecardresearch.com"}) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        return false;
    }

    public static String startPage(String original) {
        if (!isMirror(original)) return original;
        String query = URI.create(original).getQuery();
        if (query == null) return original;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2 || !parts[0].equals("s")) continue;
            String[] search = parts[1].replace('+', ' ').trim().split("\\s+");
            String slug;
            if (search[0].equals("com.google.android.youtube")) slug = "youtube";
            else if (search[0].equals("com.google.android.apps.youtube.music")) slug = "youtube-music";
            else return original;
            String base = "https://www.apkmirror.com/apk/google-inc/" + slug + "/";
            if (search.length == 2 && search[1].matches("[0-9]+(?:\\.[0-9]+){1,3}"))
                return base + slug + "-" + search[1].replace('.', '-') + "-release/#downloads";
            return search.length == 1 ? base + "#downloads" : original;
        }
        return original;
    }

    /** Keeps download controls visible. Site countdowns and browser challenges retain their behavior. */
    public static String pageScript(boolean blockAds, boolean activateDownload) {
        return "(()=>{if(!/(^|\\.)apkmirror\\.com$/i.test(location.hostname))return;"
                + "let old=document.getElementById('javo-ad-style');if(old)old.remove();"
                + (blockAds ? "let s=document.createElement('style');s.id='javo-ad-style';s.textContent='ins.adsbygoogle,.ad-container,.ad-placement,[id^=div-gpt-ad],iframe[src*=doubleclick]{display:none!important}';document.head.appendChild(s);" : "")
                + "let safe=a=>{try{let u=new URL(a.href,location.href);return u.protocol==='https:'&&u.origin===location.origin&&u.pathname.startsWith('/apk/')}catch(e){return false}};"
                + "let button=[...document.querySelectorAll('a[href]')].find(a=>safe(a)&&/\\/download\\/\\?/.test(a.href));"
                + "if(button){button.style.outline='3px solid #00e5a8';button.scrollIntoView({block:'center'});"
                + (activateDownload ? "button.click();" : "") + "return;}"
                + "let apk=[...document.querySelectorAll('.table-row')].filter(r=>/\\bAPK\\b/.test(r.innerText)&&!/\\bBUNDLE\\b/.test(r.innerText)).reduce((all,r)=>all.concat([...r.querySelectorAll('a[href]')]),[]).filter(a=>safe(a)&&new URL(a.href).pathname.startsWith(location.pathname)&&/android-apk-download\\/$/.test(new URL(a.href).pathname));"
                + "let urls=[...new Set(apk.map(a=>a.href))];if(urls.length===1){location.href=urls[0];return;}"
                + "let d=document.getElementById('downloads')||document.querySelector('.downloadButton');if(d)d.scrollIntoView({block:'center'});})();";
    }
}
