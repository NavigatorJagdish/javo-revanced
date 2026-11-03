# Replay only against pristine pinned upstream sources, before export-assets.py.
from pathlib import Path
import re, json

ROOT = Path(__file__).resolve().parents[2]
MANAGER = ROOT / 'apps/javo-manager'
CORE = ROOT / 'apps/javo-core'

def replace(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    if old not in text:
        raise ValueError(f'Expected text missing: {path}: {old[:70]}')
    p.write_text(text.replace(old, new), encoding='utf-8')

replace('apps/javo-manager/app/build.gradle.kts', 'applicationId = "app.revanced.manager.flutter"', 'applicationId = "app.javo.manager"')
replace('apps/javo-manager/app/build.gradle.kts', 'val deepLinkScheme = "revanced-manager"', 'val deepLinkScheme = "javo-manager"')
replace('apps/javo-manager/settings.gradle.kts', 'rootProject.name = "revanced-manager"', 'rootProject.name = "javo-manager"')
for p in (MANAGER/'app/src/main/res').glob('values*/strings.xml'):
    s=p.read_text(encoding='utf-8').replace('ReVanced Manager', 'Javo ReVanced Manager')
    p.write_text(s,encoding='utf-8')
replace('apps/javo-manager/app/src/main/res/values/strings.xml', 'Javo ReVanced Manager is an Android application', 'Javo ReVanced Manager is an independent GPLv3 fork of ReVanced Manager. Not affiliated with Google, YouTube, ReVanced or microG. This Android application')
replace('apps/javo-manager/app/src/main/java/app/revanced/manager/domain/manager/PreferencesManager.kt', 'booleanPreference("dynamic_color", true)', 'booleanPreference("dynamic_color", false)')
replace('apps/javo-manager/app/src/main/java/app/revanced/manager/domain/manager/PreferencesManager.kt', 'enumPreference("theme", Theme.SYSTEM)', 'enumPreference("theme", Theme.DARK)')

colors={'PrimaryDark':'D883FF','OnPrimaryDark':'280039','PrimaryContainerDark':'7433A9','OnPrimaryContainerDark':'FFFFFF','SecondaryDark':'73BCFF','OnSecondaryDark':'052D4D','SecondaryContainerDark':'171B31','OnSecondaryContainerDark':'D7E3F8','TertiaryDark':'FF89CE','OnTertiaryDark':'48002D','TertiaryContainerDark':'6B1B50','OnTertiaryContainerDark':'FFDBEE','BackgroundDark':'060710','OnBackgroundDark':'F8F9FF','SurfaceDark':'060710','OnSurfaceDark':'F8F9FF','SurfaceVariantDark':'171B31','OnSurfaceVariantDark':'BBC1D8','SurfaceTintDark':'D883FF','OutlineDark':'858BA2','OutlineVariantDark':'363B54','SurfaceBrightDark':'282C44','SurfaceContainerDark':'111426','SurfaceContainerHighDark':'171B31','SurfaceContainerHighestDark':'20253E','SurfaceContainerLowDark':'0B0D19','SurfaceContainerLowestDark':'03040A','SurfaceDimDark':'060710'}
p=MANAGER/'app/src/main/java/app/revanced/manager/ui/theme/Color.kt'
s=p.read_text()
for key,val in colors.items():
    s,n=re.subn(r'(val RVTheme'+key+r' = Color\()0xFF[0-9A-Fa-f]{6}(\))',r'\g<1>0xFF'+val+r'\2',s)
    assert n==1,key
p.write_text('// Javo modifications 2026-11-03: reference-based dark palette.\n'+s)

# Clean vector interpretations of the supplied reference silhouettes.
shapes={
'manager': [('M48,12 A36,36 0,1 1,16 33','none','#289BFF',5),('M32,22 L80,50 L32,78 Z','none','#C026F5',8),('M29,78 L53,54 C63,58 72,49 69,40 L61,48 L54,45 L51,38 L59,30 C48,29 40,39 44,49 L20,72 Z','#FF2DAA','none',0)],
'video': [('M29,18 L81,48 Q86,51 81,55 L29,85 Q22,89 22,80 L22,24 Q22,15 29,18 Z M37,37 L37,66 L63,51 Z','#FF475F','none',0),('M27,21 L77,50','none','#FF8A38',9)],
'music': [('M66,17 L66,63 C66,82 38,89 35,72 C32,57 48,52 56,57 L56,17 Z M66,18 C86,27 86,39 81,46 C79,36 73,33 66,32 Z','#A855F7','none',0),('M27,33 A35,35 0,0 0,38 86 M23,25 A43,43 0,0 0,33 93','none','#3B82F6',4)],
'core': [('M50,13 L82,32 L82,69 L50,88 L18,69 L18,32 Z','none','#11D9CF',10),('M50,13 L82,32 M18,69 L50,88','none','#25E89A',10),('M63,50 A13,13 0,1 1,37 50 A13,13 0,1 1,63 50','#11D9CF','none',0)]}
logos=ROOT/'assets/logos';logos.mkdir(parents=True,exist_ok=True)
for name,paths in shapes.items():
    body=''.join(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round" fill-rule="evenodd"/>' for d,fill,stroke,width in paths)
    (logos/f'javo-{name}.svg').write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" role="img" aria-label="Javo {name}">{body}</svg>')
    def vector(mono=False):
        b=''.join(f'<path android:pathData="{d}" android:fillColor="{("#FFFFFF" if mono and fill!="none" else fill) if fill!="none" else "#00000000"}" android:strokeColor="{("#FFFFFF" if mono and stroke!="none" else stroke) if stroke!="none" else "#00000000"}" android:strokeWidth="{width}" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillType="evenOdd"/>' for d,fill,stroke,width in paths)
        return f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="100" android:viewportHeight="100"><group android:scaleX="0.7" android:scaleY="0.7" android:translateX="15" android:translateY="15">{b}</group></vector>'
    (logos/f'javo-{name}-android.xml').write_text(vector())
    (logos/f'javo-{name}-monochrome.xml').write_text(vector(True))
    if name=='manager':
        for target in ['ic_launcher_foreground','ic_logo_ring','ic_launcher_splash_screen']:
            (MANAGER/f'app/src/main/res/drawable/{target}.xml').write_text(vector())
        (MANAGER/'app/src/main/res/drawable/ic_launcher_monochrome.xml').write_text(vector(True))
    if name=='core':
        res=CORE/'play-services-core/src/main/res'
        (res/'drawable/javo_core.xml').write_text(vector())
        replace('apps/javo-core/play-services-core/src/main/AndroidManifest.xml','@mipmap/ic_app"','@drawable/javo_core"')
        replace('apps/javo-core/play-services-core/src/main/AndroidManifest.xml','@mipmap/ic_app_settings"','@drawable/javo_core"')

# Preserve GmsCore's compatibility identity, binders, authorities, permissions and account contracts.
for p in (CORE/'play-services-core/src').glob('*/res/values*/strings.xml'):
    s=p.read_text(encoding='utf-8')
    s=re.sub(r'(<string name="gms_app_name">)[^<]*(</string>)',r'\1Javo Core\2',s)
    p.write_text(s,encoding='utf-8')
replace('apps/javo-core/play-services-core/src/main/res/values/themes.xml','Theme.Material3.DynamicColors.DayNight','Theme.Material3.DayNight')
p=CORE/'play-services-core/src/main/res/values/colors.xml'
s=p.read_text().replace('#ff0b57d0','#ff007C78').replace('#ffa8c7fa','#ff11D9CF').replace('#ff0842a0','#ff00625E').replace('#ffd3e3fd','#ffB3F5EA')
p.write_text(s)
# Core exports source-archive builds with a recorded version rather than invented git identity.
replace('apps/javo-core/build.gradle', ': "v0.0.0.$gmsVersionCode"', ': "0.3.13.2.$gmsVersionCode"')
print('Javo branding applied; upstream engine and service contracts retained.')
