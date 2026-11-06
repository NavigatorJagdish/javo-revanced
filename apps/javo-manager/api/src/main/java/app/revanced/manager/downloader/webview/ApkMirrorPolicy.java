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

    public static boolean isDownloadFile(String address) {
        try {
            URI uri = URI.create(address);
            String path = uri.getPath().toLowerCase(Locale.ROOT);
            boolean source = isMirror(address);
            // Dedicated bucket observed in APKMirror's own signed file redirect, not all R2 tenants.
            boolean storage = "https".equalsIgnoreCase(uri.getScheme())
                    && "eb5e7388c3df147b74dd2379b7cf8323.r2.cloudflarestorage.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && path.startsWith("/downloadprod/");
            return (source || storage) && (path.endsWith(".apk") || path.endsWith(".apkm") || path.endsWith(".apks") || path.endsWith(".xapk"));
        } catch (IllegalArgumentException | NullPointerException error) { return false; }
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
        return script(blockAds, activateDownload, false);
    }

    /** Resolve a user-selected control for the native host; no browser download event is needed. */
    public static String downloadActionScript(boolean blockAds) {
        return script(blockAds, true, true);
    }

    public static String browserUserAgent(String original) {
        return original.replace("; wv", "").replace(" Version/4.0", "");
    }

    private static String script(boolean blockAds, boolean activateDownload, boolean resolve) {
        return "(()=>{if(!/(^|\\.)apkmirror\\.com$/i.test(location.hostname))return;"
                + "let old=document.getElementById('javo-ad-style');if(old)old.remove();"
                + (blockAds ? "let s=document.createElement('style');s.id='javo-ad-style';s.textContent='ins.adsbygoogle,.ains,.ad-container,.ad-placement,[id^=div-gpt-ad],iframe[src*=doubleclick]{display:none!important}';document.head.appendChild(s);" : "")
                + "let safe=a=>{try{let u=new URL(a.href,location.href);return u.protocol==='https:'&&u.origin===location.origin&&u.pathname.startsWith('/apk/')}catch(e){return false}};"
                + "let ready=a=>!a.hasAttribute('disabled')&&a.getAttribute('aria-disabled')!=='true'&&!a.classList.contains('disabled');"
                + "let finalLink=document.querySelector('a#download-link[href]');"
                + "let trustedFinal=a=>{try{let u=new URL(a.href,location.href);return u.protocol==='https:'&&u.origin===location.origin&&u.pathname==='/wp-content/themes/APKMirror/download.php'}catch(e){return false}};"
                + "let button=(finalLink&&trustedFinal(finalLink)?finalLink:null)||[...document.querySelectorAll('a[href]')].find(a=>safe(a)&&/\\/download\\/\\?/.test(a.href));"
                + "if(button){button.target='_self';button.style.outline='3px solid #00e5a8';button.scrollIntoView({block:'center'});"
                + (resolve ? "if(!ready(button))return JSON.stringify({kind:'waiting'});return JSON.stringify({kind:trustedFinal(button)?'download':'navigate',url:button.href});" : "if(!ready(button))return 'waiting';")
                + (activateDownload ? "location.assign(button.href);return 'navigating';" : "return 'ready';") + "}"
                + "let apk=[...document.querySelectorAll('.table-row')].filter(r=>/\\bAPK\\b/.test(r.innerText)&&!/\\bBUNDLE\\b/.test(r.innerText)).reduce((all,r)=>all.concat([...r.querySelectorAll('a[href]')]),[]).filter(a=>safe(a)&&new URL(a.href).pathname.startsWith(location.pathname)&&/android-apk-download\\/$/.test(new URL(a.href).pathname));"
                + "let urls=[...new Set(apk.map(a=>a.href))];if(urls.length===1){"
                + (resolve ? "return JSON.stringify({kind:'navigate',url:urls[0]});" : "location.href=urls[0];return 'variant';") + "}"
                + "let d=document.getElementById('downloads')||document.querySelector('.downloadButton');if(d)d.scrollIntoView({block:'center'});" + (resolve ? "return JSON.stringify({kind:'missing'});" : "return 'no-control';") + "})();";
    }
}
