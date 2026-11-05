# Javo ReVanced installation aur setup guide

Ye guide **v0.1.0-alpha.3** ke liye hai. Basic Manager setup ke liye Android 8.0 ya naya version chahiye. Root ki zaroorat nahi hai. Manager debug-signed hai. Core release runtime flags aur test certificate use karta hai. Har release ke saath usi APK ki emulator installation aur launch evidence shamil hai.

## 1. GitHub se files download karein

Private repository ka access rakhne wale GitHub account se sign in karein. Phone par setup karne ke liye individual files download karein. Complete ZIP tab chahiye jab aap source code, website aur test reports bhi rakhna chahte hain.

| File | Kab chahiye | Download |
| --- | --- | --- |
| Manager APK | Manager install karne ke liye | [Manager APK][manager] |
| Core APK | Jab aapki selected patch setup mein **GmsCore support** chahiye | [Core APK][core] |
| Javo branding bundle | Video ya Music ka Javo naam aur icon lagane ke liye | [Branding RVP][branding] |
| Complete ZIP | Source, website, APKs aur evidence ke liye | [Project ZIP][zip] |

Agar GitHub par **404** aaye, pehle account aur repository access check karein. Saari files release ke **Assets** section mein bhi milengi. [GitHub Release][release]

## 2. Manager install karein

1. Phone ke **Files → Downloads** mein Manager APK kholein aur **Install** tap karein.
2. Agar Android installer permission maange, prompt se **Settings → Allow from this source** kholein aur sirf APK kholne wale app ko permission dein. Wapas aakar installation complete karein. [Android installation help](https://support.google.com/pixelphone/answer/7391672?hl=en)
3. **Javo ReVanced Manager** launch karein aur initial setup screen follow karein. Agar Manager ke andar se patched APK install karte waqt Android permission maange, tab Manager ke liye bhi installer permission dein.

## 3. Core install karein, agar setup mein zaroorat ho

1. Core APK kholein, **Install** tap karein aur phir **Javo Core Settings** ek baar launch karein.
2. Agar pehle se ReVanced GmsCore ya koi aur build `app.revanced.android.gms` ke saath installed hai, Android update ke liye matching signature maangega. **App not installed** aaye to existing Core ko turant remove na karein. Pehle check karein ki selected patch us Core ke saath compatible hai, aur Core badalne se pehle zaroori settings aur account setup ka backup plan rakhein.

## 4. Branding bundle import karein aur APK patch karein

1. Manager mein **Patches → Edit** (pencil icon) → **Add patches → Select from storage** kholein. Download ki hui `javo-branding-0.1.0-alpha.1.rvp` file select karein.
2. **Apps → Select from storage** se apna compatible, standalone YouTube ya YouTube Music APK select karein. Source APK aapke paas authorized tareeke se hona chahiye. Release mein ye source APKs shamil nahi hain. Split APK, `.apkm`, `.apks` aur `.xapk` ko seedha input ke taur par use na karein.
3. Selected app ki patch list mein **Javo Video branding** ya **Javo Music branding** select karein. Doosra name ya icon patch deselect karein. Agar non-root setup ke liye GmsCore support chahiye, trusted upstream catalog se apne APK version ke liye compatible **GmsCore support** patch select karein.
4. **Patch** tap karein aur patching aur signing complete hone dein. Output ki copy rakhne ke liye **Save APK** use karein. Phone par install karne ke liye **Install** use karein. Agar install mode poochha jaye, non-root setup ke liye **Default** choose karein.
5. Patched app ka naam aur icon check karein aur app launch karein. Branding ka test first-party fixtures par hua hai. Actual YouTube aur Music APKs, Google sign-in aur playback abhi verify nahi hue hain.

## 5. Common errors

| Problem | Kya check karein |
| --- | --- |
| APK install nahi hota | Download dobara verify karein. Android version, installer permission aur existing app ka signing conflict check karein. |
| Branding patch list mein nahi dikh raha | RVP import aur selected APK ka package check karein. Video aur Music ke liye alag branding patches hain. |
| **Incompatible patch** ya patching failure | Selected bundle ki version requirement check karein. Standalone APK use karein, overlapping branding patches hataayein aur **Save logs** se error report save karein. |
| Patched app purani copy ko update nahi kar paata | Same package ke update ke liye matching signing key chahiye. Manager mein **Settings → Import & export → Export keystore** se apni patch signing key ka private backup rakh sakte hain. |
| Google login ya playback issue | APK version aur **GmsCore support** patch ki compatibility check karein. Is alpha mein account aur playback testing pending hai. |

## 6. Optional checksum verification

Windows PowerShell mein downloaded files wale folder se ye command chalayein. Output ke hashes release ki `SHA256SUMS` file se match karein. [Download SHA256SUMS][checksums]

```powershell
Get-FileHash .\javo-manager-0.1.0-alpha.3-debug.apk, .\javo-core-0.3.13.2.250932-javo.2-test.apk, .\javo-branding-0.1.0-alpha.1.rvp -Algorithm SHA256
```

Baad mein APK updates isi GitHub Releases page se check karein. Alpha mein production auto-update feed configure nahi hai. [GitHub Release][release]

[release]: https://github.com/NavigatorJagdish/javo-revanced/releases/tag/v0.1.0-alpha.3
[manager]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.3/javo-manager-0.1.0-alpha.3-debug.apk
[core]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.3/javo-core-0.3.13.2.250932-javo.2-test.apk
[branding]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.3/javo-branding-0.1.0-alpha.1.rvp
[zip]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.3/javo-revanced-development.zip
[checksums]: https://github.com/NavigatorJagdish/javo-revanced/releases/download/v0.1.0-alpha.3/SHA256SUMS


## Alpha 2 ke changes

Core ka consumer build SMS, overlay aur ROM signature-spoofing permissions request nahi karta. SMS autofill ab is build mein supported nahi hai. Core release runtime flags aur test signing certificate ke saath distribute hota hai.

APKMirror downloader se bundle lene par Manager download, merge aur write ki alag stages dikhata hai. Manual **Select from storage** input ke liye ab bhi standalone APK chahiye. Download fail ho to **Save logs** patching ke dauraan bhi available hai.

Play Protect ka exact verdict device aur Google ki classification par depend karta hai. Agar naya Core bhi block ho, exact warning text, Android version aur release filename record karein. Is update ki verification report mein permission audit aur emulator results dekhein.

## Alpha 3 Manager flow

APKMirror browser mein ad protection default on hai. Suggested YouTube version ka release page seedha khulta hai. Unique standalone APK variant milne par browser us page ko select karta hai. Toolbar ke **Download APK** se asli download control use karein. **Search APKMirror** aur **Ad protection** toggle bhi available hain. Website ke wait timer aur verification challenge follow karein.

Preparing, patching aur writing mein elapsed time aur measured memory usage dikhegi. Default separate patcher process ka requested heap limit 1024 MB hai. Device RAM ka one-quarter upper cap lagta hai, kyunki ART heap ke liye extra address space reserve karta hai. Advanced settings mein runtime aur memory limit adjust kar sakte hain. Process band hone par error aur Save logs se report milegi. Agar error aaye, exact step, Android version aur latest logs record karein.

Core asset alpha.2 ka verified, matching-certificate binary hai. Manager update ke liye apni existing compatible Core installation use kar sakte hain. Manager development signing certificate pehle release se differ kar sakta hai. Installation conflict par settings aur patch-signing keystore ka export sambhal kar rakhein.
