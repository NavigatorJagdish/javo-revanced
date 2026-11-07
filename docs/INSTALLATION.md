# Javo ReVanced installation aur setup guide

Ye guide **v0.1.0-alpha.7** ke liye hai. Manager ke liye Android 8.0 ya naya version chahiye. Root ki zaroorat nahi hai. Manager development certificate aur Core test certificate use karte hain. Release ZIP mein APKs aur unki verification reports milengi.

## 1. GitHub se files download karein

Private repository ka access rakhne wale GitHub account se sign in karein. Phone par setup karne ke liye individual files download karein. Complete ZIP tab chahiye jab aap source code, website aur test reports bhi rakhna chahte hain.

| File | Kab chahiye | Download |
| --- | --- | --- |
| Manager APK | Manager install karne ke liye | [Manager APK][manager] |
| Core APK | Jab aapki selected patch setup mein **GmsCore support** chahiye | [Core APK][core] |
| Complete ZIP | Source, website, APKs aur evidence ke liye | [Project ZIP][zip] |

Agar GitHub par **404** aaye, pehle account aur repository access check karein. Saari files release ke **Assets** section mein bhi milengi. [GitHub Release][release]

## Manager update

Alpha.6 se alpha.7 Manager APK seedha update karein. Alpha.5 ya purani build se signature conflict aaye to pehle **Settings → Import & export** se **settings aur patch-signing keystore** export karein. Phir naya Manager install karke dono backups import karein. Patched app update karne ke liye usi patch-signing keystore ka use karein.

## 2. Manager install karein

1. Phone ke **Files → Downloads** mein Manager APK kholein aur **Install** tap karein.
2. Agar Android installer permission maange, prompt se **Settings → Allow from this source** kholein aur sirf APK kholne wale app ko permission dein. Wapas aakar installation complete karein. [Android installation help](https://support.google.com/pixelphone/answer/7391672?hl=en)
3. **Javo ReVanced Manager** launch karein aur initial setup screen follow karein. Agar Manager ke andar se patched APK install karte waqt Android permission maange, tab Manager ke liye bhi installer permission dein.

## 3. Core install karein, agar setup mein zaroorat ho

1. Core APK kholein, **Install** tap karein aur phir **Javo Core Settings** ek baar launch karein.
2. Agar pehle se ReVanced GmsCore ya koi aur build `app.revanced.android.gms` ke saath installed hai, Android update ke liye matching signature maangega. **App not installed** aaye to existing Core ko turant remove na karein. Pehle check karein ki selected patch us Core ke saath compatible hai, aur Core badalne se pehle zaroori settings aur account setup ka backup plan rakhein.
3. Patched app agar Core ke battery-optimization prompt par ruke, **Continue** se Core ki battery setting kholein aur optimization exemption allow karein. Android 10 test mein ye setup zaroori tha.
4. Agar patched YouTube launch hote hi band ho aur logs mein **missing google package permission or GET_ACCOUNTS** aaye, patched app ki **App info → Permissions → Contacts** setting check karein. Account discovery ki ye permission dene ke baad emulator par main navigation khula. Menu ka naam phone ke hisaab se badal sakta hai.

Play Protect scan maange to scan complete hone dein. Scan safe result de aur Android **Install** dikhaye, tab installation complete karein. Agar completed scan exact GitHub release APK ko harmful batakar block kare, warning ka exact text aur Android version note karein. Release checksum verify karein aur owner Google se review maang sakta hai. [Google Play Protect help](https://support.google.com/googleplay/answer/2812853?hl=en) · [App verification appeal](https://support.google.com/googleplay/android-developer/answer/2992033?hl=en)

## 4. APK patch karein

1. APKMirror screen mein **Download APK** tap karein. HTTP 503 aaye to isi button se retry karein. Agar source phir bhi unavailable hai, **Open browser** mein file download karein. Manager ki source screen par **Use downloaded APK** se APK ya APKM import karein. Manager file validate karke wahi patching session continue karega.
2. Apne paas compatible standalone APK ho to **Apps → Select from storage** se select karein. Non-root setup ke liye compatible **GmsCore support** patch select karein.
3. **Patch** tap karein. Manager YouTube output mein **Javo Video** aur Music output mein **Javo Music** branding khud lagata hai. Alag branding bundle import karne ki zaroorat nahi hai.
4. Signing complete hone par **Install** tap karein. Non-root install ke liye **Default** choose karein. APK ki copy rakhne ke liye **Save APK** use karein.

## 5. Common errors

| Problem | Kya check karein |
| --- | --- |
| APK install nahi hota | Download dobara verify karein. Android version, installer permission aur existing app ka signing conflict check karein. |
| Purana app abhi bhi ReVanced naam dikhata hai | Alpha.7 Manager mein original compatible APK phir se patch karein. Usi patch-signing keystore se bana output install karein. |
| **Incompatible patch** ya patching failure | Selected bundle ki version requirement check karein aur **Save logs** se error report save karein. |
| Patched app purani copy ko update nahi kar paata | Same package ke update ke liye matching signing key chahiye. Manager mein **Settings → Import & export → Export keystore** se apni patch signing key ka private backup rakh sakte hain. |
| Google login ya playback issue | APK version aur **GmsCore support** patch ki compatibility check karein. Is alpha mein account aur playback testing pending hai. |

## 6. Optional checksum verification

Windows PowerShell mein downloaded files wale folder se ye command chalayein. Output ke hashes release ki `SHA256SUMS` file se match karein. [Download SHA256SUMS][checksums]

```powershell
Get-FileHash .\javo-manager-0.1.0-alpha.7-debug.apk, .\javo-core-0.3.13.2.250932-javo.2-test.apk -Algorithm SHA256
```

Baad mein APK updates isi GitHub Releases page se check karein. Alpha mein production auto-update feed configure nahi hai. [GitHub Release][release]

[release]: https://github.com/NavigatorJagdish/javo-revanced/releases/tag/v0.1.0-alpha.7
[manager]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.7/javo-manager-0.1.0-alpha.7-debug.apk
[core]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.7/javo-core-0.3.13.2.250932-javo.2-test.apk
[zip]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.7/javo-revanced-development.zip
[checksums]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.7/SHA256SUMS


## Alpha 2 ke changes

Core ka consumer build SMS, overlay aur ROM signature-spoofing permissions request nahi karta. SMS autofill ab is build mein supported nahi hai. Core release runtime flags aur test signing certificate ke saath distribute hota hai.

APKMirror downloader se bundle lene par Manager download, merge aur write ki alag stages dikhata hai. Manual **Select from storage** input ke liye ab bhi standalone APK chahiye. Download fail ho to **Save logs** patching ke dauraan bhi available hai.

Play Protect ka exact verdict device aur Google ki classification par depend karta hai. Agar naya Core bhi block ho, exact warning text, Android version aur release filename record karein. Is update ki verification report mein permission audit aur emulator results dekhein.

## Alpha 3 Manager flow

APKMirror browser mein ad protection default on hai. Suggested YouTube version ka release page seedha khulta hai. Unique standalone APK variant milne par browser us page ko select karta hai. Toolbar ke **Download APK** se asli download control use karein. **Search APKMirror** aur **Ad protection** toggle bhi available hain. Website ke wait timer aur verification challenge follow karein.

Preparing, patching aur writing mein elapsed time aur measured memory usage dikhegi. Default separate patcher process ka requested heap limit 700 MB hai. Android 10 ya purane version par 700 MB upper cap aur device RAM ka one-quarter cap lagta hai, kyunki ART heap ke liye extra address space reserve karta hai. Advanced settings mein runtime aur memory limit adjust kar sakte hain. Process band hone par error aur Save logs se report milegi. Agar error aaye, exact step, Android version aur latest logs record karein.

Core asset alpha.2 ka verified, matching-certificate binary hai. Manager update ke liye apni existing compatible Core installation use kar sakte hain. Manager development signing certificate pehle release se differ kar sakta hai. Installation conflict par settings aur patch-signing keystore ka export sambhal kar rakhein.

Asli YouTube 20.40.45 ka local test teen selected patches ke saath preparation, patching aur signing tak lagbhag 4 minute 49 second chala. Device ke hisaab se waqt badlega. Elapsed time aur memory badal rahe hon to kaam abhi chal raha hai. Playback aur account sign-in is test ka hissa nahi the.


## Alpha.6 ka APKMirror flow

Browser page ke upar native **Download APK**, **Open browser** aur **Use downloaded APK** buttons hamesha dikhte hain. **Download APK** final link ko Manager ko seedha deta hai; page ka apna automatic download callback zaroori nahi hai. Agar site verification ya error aaye, **Open browser** mein download karein. Manager ki isi screen par wapas aa kar **Use downloaded APK** se file select karein. APK/APKM validate aur zaroorat par merge hone ke baad wahi patching session continue hoga. Source download safal hone se pehle verification challenges poore karne pad sakte hain.
