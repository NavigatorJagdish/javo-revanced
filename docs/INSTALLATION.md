# Javo ReVanced installation aur setup guide

> **Manager download abhi hold par hai.** Owner ke phone ne Manager alpha.13 ko bhi harmful indicate kiya. Core javo.6 ke liye owner ne safe result report kiya; ye har phone ki guarantee nahi hai. Sirf [official download status](https://navigatorjagdish.github.io/javo-revanced/downloads/#core) par listed Core file lein. Play Protect ON rakhein; harmful block aaye to installation rok dein.

Manager ke neeche diye steps uski reviewed release available hone ke baad use karein. Manager ke liye Android 8.0 ya naya version chahiye; Core javo.6 ke liye Android 5.0 ya naya version chahiye. Neeche diya default setup bina root ke kaam karta hai.

Core javo.6 file: `javo-core-0.3.13.2.250932-javo.6-production.apk`.
SHA-256: `c5bb18a75cc2b70a6729a43aa92054f81f3f959b586e6fb4af588ba685b96311`.
Account login, push delivery aur real video playback ka full verification abhi baaki hai. Core ke do legacy dependencies ki teen advisories ka review `DEPENDENCY-REVIEW.md` mein hai.

## 1. GitHub se files download karein

Download status open hone par phone setup ke liye approved release se individual APK files lein.

| File | Kab chahiye |
| --- | --- |
| Manager APK | Manager install karne ke liye |
| Core APK | Jab aapki selected patch setup mein **GmsCore support** chahiye |
| `SHA256SUMS` | APK ki download integrity verify karne ke liye |

## Manager update

Matching signature wala naya Manager purani install ko update kar sakta hai. Signature conflict aaye to pehle **Settings → Import & export** se **settings aur patch-signing keystore** export karein. Phir naya Manager install karke dono backups import karein. Patched app update karne ke liye usi patch-signing keystore ka use karein.

Naya Core-download link aur branding lagane ke liye updated Manager se supported original APK dobara patch karein. Manager update karne se pehle se installed Javo Video/Music APK ka code khud nahi badalta. Naye patched output mein Core missing hone par Javo ke official Core download section ka link hota hai; review hold ho to us page par status dikhega.

## 2. Manager install karein

1. Phone ke **Files → Downloads** mein Manager APK kholein aur **Install** tap karein.
2. Agar Android installer permission maange, prompt se **Settings → Allow from this source** kholein aur sirf APK kholne wale app ko permission dein. Wapas aakar installation complete karein. [Android installation help](https://support.google.com/pixelphone/answer/7391672?hl=en)
3. **Javo ReVanced Manager** launch karein aur initial setup screen follow karein. Agar Manager ke andar se patched APK install karte waqt Android permission maange, tab Manager ke liye bhi installer permission dein.

## 3. Core install karein, agar setup mein zaroorat ho

1. Core APK kholein, **Install** tap karein aur phir **Javo Core Settings** ek baar launch karein.
2. Android Core ko update karne ke liye matching signing certificate maangta hai. **App not installed** aaye to pehle check karein ki aapki selected patch setup existing Core ke saath compatible hai. Core ko uninstall karne se uska local data hat jayega. Replace karne se pehle zaroori settings aur account setup ka backup plan rakhein.
3. Patched app agar Core ke battery-optimization prompt par ruke, **Continue** se Core ki battery setting kholein aur optimization exemption allow karein. Android 10 test mein ye setup zaroori tha.
4. Agar patched YouTube launch hote hi band ho aur logs mein **missing google package permission or GET_ACCOUNTS** aaye, patched app ki **App info → Permissions → Contacts** setting check karein. Account discovery ki ye permission dene ke baad emulator par main navigation khula. Menu ka naam phone ke hisaab se badal sakta hai.

**App Scan Recommended** aaye to Play Protect enabled rakh kar scan complete hone dein. Agar warning mein sirf **Cancel** ya **Install anyway** aaye, warning ka title note karein aur **Cancel** choose karein jab tak wajah clear ho. Scan safe result de aur Android **Install** dikhaye, tab installation complete karein. Agar completed scan exact GitHub release APK ko harmful batakar block kare, install rok dein aur Play Protect enabled rakhein. Warning ka exact text, APK filename aur Android version note karein. Release checksum verify karein aur owner Google se review maang sakta hai. [Google Play Protect help](https://support.google.com/googleplay/answer/2812853?hl=en) · [App verification appeal](https://support.google.com/googleplay/android-developer/answer/2992033?hl=en)

## 4. APK patch karein

1. APKMirror screen mein **Download APK** tap karein. HTTP 503 aaye to isi button se retry karein. Agar source phir bhi unavailable hai, **Open browser** mein file download karein. Manager ki source screen par **Use downloaded APK** se APK ya APKM import karein. Manager file validate karke wahi patching session continue karega.
2. Apne paas compatible standalone APK ho to **Apps → Select from storage** se select karein. Non-root setup ke liye compatible **GmsCore support** patch select karein.
3. **Patch** tap karein. Manager YouTube output mein **Javo Video** aur Music output mein **Javo Music** branding khud lagata hai. Alag branding bundle import karne ki zaroorat nahi hai.
4. Signing complete hone par **Install** tap karein. Non-root install ke liye **Default** choose karein. APK ki copy rakhne ke liye **Save APK** use karein.

APK download ke baad **Merging split APKs**, **Preparing the downloaded APK** aur **Checking APK package and version** dikh sakte hain. Split files ko process karne ke liye phone mein kuch extra free storage chahiye. Preparation ke baad patch aur APK writing ke liye Manager 20 minute deta hai. Agar preparation timeout aaye, pehle space check karein, phir suggested standalone APK se retry karein. Problem repeat ho to **Save logs** use karein.

## Video ya audio phone par save karein

1. Javo Video mein ek video kholein: **Share → Save with Javo**. Ya Manager ke **Downloads → Downloaders → Save with Javo** screen mein link paste karein.
2. **Save video (MP4)** ya **Save audio (M4A)** tap karein aur screen khuli rakhein. File **Files → Downloads** mein milegi. Android 8–9 par system file picker mein **Downloads** choose karein.
3. Javo Video ka download control use karna ho to repatch karte waqt **Downloads** patch select karein. Patched app ke **Settings → Javo Video settings → Player → Downloads** mein download control enable karein. Purani settings mein downloader package override ho to use `app.javo.manager` set karein. Share method pehle se kaam karta hai.

Sirf aisi media save karein jise download karne ki permission ho. Jahan compatible video aur audio alag milte hain, Manager phone par dono ko jod kar ek MP4 banata hai. M4A ke liye compatible audio stream chahiye. Quality aur availability source par depend karti hai. Private, restricted, protected aur playlist downloads supported nahi hain.

## Video chalte waqt ruk jaaye

Manager mein recommended YouTube version ko **GmsCore support** aur **Spoof video streams** ke saath repatch karein. **Settings → Javo Video settings → Miscellaneous → Spoof video streams** mein pehle **Android VR** try karein. Video phir bhi rukta hai to available alternate client try karke app restart karein. Slower mobile data par video quality **480p** try karein. [Upstream playback troubleshooting](https://github.com/ReVanced/revanced-documentation/blob/main/docs/revanced-resources/troubleshooting.md#32-youtube-or-youtube-music-playback-does-not-work)

## 5. Common errors

| Problem | Kya check karein |
| --- | --- |
| APK install nahi hota | Download dobara verify karein. Android version, installer permission aur existing app ka signing conflict check karein. |
| Purana app abhi bhi ReVanced naam dikhata hai | Naye Manager mein original compatible APK phir se patch karein. Usi patch-signing keystore se bana output install karein. |
| **Incompatible patch** ya patching failure | Selected bundle ki version requirement check karein aur **Save logs** se error report save karein. |
| Patched app purani copy ko update nahi kar paata | Same package ke update ke liye matching signing key chahiye. Manager mein **Settings → Import & export → Export keystore** se apni patch signing key ka private backup rakh sakte hain. |
| Google login ya playback issue | APK version aur **GmsCore support** patch ki compatibility check karein. Is alpha mein account aur playback testing pending hai. |

## 6. Optional checksum verification

Windows PowerShell mein downloaded files wale folder se ye command chalayein. `MANAGER_FILE.apk` aur `CORE_FILE.apk` ki jagah approved release ke filenames likhein. Output ke hashes usi release ki `SHA256SUMS` file se match karein.

```powershell
Get-FileHash .\MANAGER_FILE.apk, .\CORE_FILE.apk -Algorithm SHA256
```

Approved APK updates [download status page](https://navigatorjagdish.github.io/javo-revanced/downloads/) se check karein.
