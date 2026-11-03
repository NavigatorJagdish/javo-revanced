"""Export scalable brand artwork to transparent PNGs with resvg-py 0.5.0."""
from pathlib import Path
import re,struct
import resvg_py
ROOT=Path(__file__).resolve().parents[2]
LOGOS=ROOT/'assets/logos'
for name in ['manager','video','music','core']:
    svg=(LOGOS/f'javo-{name}.svg').read_text()
    mono=re.sub(r'(fill|stroke)="#[A-Fa-f0-9]{6}"',r'\1="#FFFFFF"',svg)
    (LOGOS/f'javo-{name}-monochrome.svg').write_text(mono)
    for variant,source in [('',svg),('-monochrome',mono)]:
        for size in [192,512]:
            data=resvg_py.svg_to_bytes(svg_string=source,width=size,height=size)
            assert data[:8]==b'\x89PNG\r\n\x1a\n'
            assert data[25]==6, 'Expected PNG with alpha channel'
            (LOGOS/f'javo-{name}{variant}-{size}.png').write_bytes(data)
    adaptive=ROOT/f'assets/icons/{name}/mipmap-anydpi-v26';adaptive.mkdir(parents=True,exist_ok=True)
    (adaptive/'ic_launcher.xml').write_text(f'<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/javo_icon_background"/><foreground android:drawable="@drawable/javo_{name}"/><monochrome android:drawable="@drawable/javo_{name}_monochrome"/></adaptive-icon>')
    drawable=adaptive.parent/'drawable';drawable.mkdir(exist_ok=True)
    for suffix in ['', '-monochrome']:
        (drawable/f'javo_{name}{suffix.replace("-","_")}.xml').write_bytes((LOGOS/f'javo-{name}{suffix if suffix else "-android"}.xml').read_bytes())
    values=adaptive.parent/'values';values.mkdir(exist_ok=True)
    (values/'colors.xml').write_text('<resources><color name="javo_icon_background">#060710</color></resources>')
    if name=='core':
        res=ROOT/'apps/javo-core/play-services-core/src/main/res'
        # Raster fallbacks keep launcher icons usable on Core's API19/20 minimum.
        for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
            folder=res/f'mipmap-{density}';folder.mkdir(exist_ok=True)
            png=resvg_py.svg_to_bytes(svg_string=svg,width=size,height=size)
            for filename in ['ic_app.png','ic_app_settings.png']:(folder/filename).write_bytes(png)
        adaptive=res/'mipmap-anydpi-v26';adaptive.mkdir(exist_ok=True)
        for filename in ['ic_app.xml','ic_app_settings.xml']:
            (adaptive/filename).write_text('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/javo_icon_background"/><foreground android:drawable="@drawable/javo_core"/></adaptive-icon>')
        (res/'values/javo_colors.xml').write_text('<resources><color name="javo_icon_background">#060710</color></resources>')
        manifest=ROOT/'apps/javo-core/play-services-core/src/main/AndroidManifest.xml'
        manifest.write_text(manifest.read_text().replace('@drawable/javo_core"','@mipmap/ic_app"'))
body=re.search(r'<svg[^>]*>(.*)</svg>',(LOGOS/'javo-manager.svg').read_text()).group(1)
wordmark=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 110"><g transform="translate(5 5)">{body}</g><text x="125" y="48" fill="#F8F9FF" font-family="sans-serif" font-size="33" font-weight="700" letter-spacing="2">JAVO REVANCED</text><text x="127" y="80" fill="#A2A8C1" font-family="sans-serif" font-size="18" letter-spacing="5">MANAGER</text></svg>'
(LOGOS/'javo-wordmark.svg').write_text(wordmark)
(LOGOS/'javo-wordmark.png').write_bytes(resvg_py.svg_to_bytes(svg_string=wordmark,width=1200))
print('Exported 16 transparent PNG icons, wordmark and four adaptive icon resource sets.')
