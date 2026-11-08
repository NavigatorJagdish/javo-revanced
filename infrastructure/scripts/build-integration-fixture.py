"""Build a first-party test APK; no proprietary app code is downloaded or included."""
from pathlib import Path
import argparse,os,shutil,subprocess,zipfile,hashlib,json,urllib.request

ROOT=Path(__file__).resolve().parents[2]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--sdk',type=Path,required=True)
    parser.add_argument('--fixture-only',action='store_true');args=parser.parse_args()
    suffix='.exe' if os.name=='nt' else ''
    build=args.sdk/'build-tools/34.0.0';android=args.sdk/'platforms/android-35/android.jar'
    out=ROOT/'apps/javo-manager/app/build/integration-assets';out.mkdir(parents=True,exist_ok=True)
    work=ROOT/'tools/integration-fixture';work.mkdir(parents=True,exist_ok=True)
    classes=work/'classes';classes.mkdir(exist_ok=True)
    def run(*command):subprocess.run([str(x) for x in command],check=True)
    run(Path(os.environ['JAVA_HOME'])/'bin'/('javac'+suffix),'-source','8','-target','8','-classpath',android,'-d',classes,ROOT/'tests/fixture/MainActivity.java')
    jar=work/'fixture.jar'
    with zipfile.ZipFile(jar,'w') as z:
        for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
    run(Path(os.environ['JAVA_HOME'])/'bin'/('java'+suffix),'-cp',build/'lib/d8.jar',
        'com.android.tools.r8.D8','--min-api','26','--lib',android,'--output',work,jar)
    # Large resource table exercises preparation and patch writing beyond a tiny fixture.
    fixture_res=work/'large-res/values'
    fixture_res.mkdir(parents=True,exist_ok=True)
    (fixture_res/'strings.xml').write_text(
        '<resources><string name="app_name">Javo integration fixture</string>'
        '<string name="revanced_settings_title">ReVanced</string>'
        + ''.join(f'<string name="stress_{i:05d}">Fixture resource {i}</string>' for i in range(4096))
        + '</resources>')
    drawable = fixture_res.parent / 'drawable'
    drawable.mkdir(exist_ok=True)
    for name in ('yt_ringo2_wordmark_header_light', 'yt_wordmark_header_light', 'product_logo_youtube_color_144'):
        (drawable / f'{name}.xml').write_text(
            '<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#FF0000"/></shape>')
    resources=work/'resources.zip' 
    run(build/('aapt2'+suffix),'compile','--dir',fixture_res.parent,'-o',resources)
    for product,package in [('video','com.google.android.youtube'),('music','com.google.android.apps.youtube.music')]:
        manifest=work/(product+'-manifest.xml')
        manifest.write_text((ROOT/'tests/fixture/AndroidManifest.xml').read_text().replace('com.google.android.youtube',package))
        apk=out/('fixture-'+product+'.apk')
        run(build/('aapt2'+suffix),'link','-I',android,'--manifest',manifest,
            '--emit-ids',work/f'{product}-resource-ids.txt','-o',apk,resources)
        with zipfile.ZipFile(apk,'a') as z:
            z.write(work/'classes.dex','classes.dex')
            z.writestr('assets/javo-transfer-stress.bin',bytes(32*1024*1024),compress_type=zipfile.ZIP_STORED)
    # Config splits share the base resource IDs and add a configuration.
    (work/'config-res/values/strings.xml').unlink(missing_ok=True)
    config_res=work/'config-res/values-hi'
    config_res.mkdir(parents=True,exist_ok=True)
    (config_res/'strings.xml').write_text('<resources><string name="app_name">merged</string></resources>',encoding='utf-8')
    config_resources=work/'config-resources.zip'
    run(build/('aapt2'+suffix),'compile','--dir',config_res.parent,'-o',config_resources)
    config_manifest=work/'config-manifest.xml'
    config_manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.google.android.youtube" split="config.javo" android:versionCode="1" android:versionName="fixture-1"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="28"/><application android:hasCode="false"/></manifest>',encoding='utf-8')
    config_apk=work/'config.javo.apk'
    run(build/('aapt2'+suffix),'link','-I',android,'--manifest',config_manifest,'--no-resource-removal',
        '--stable-ids',work/'video-resource-ids.txt','-o',config_apk,config_resources)
    with zipfile.ZipFile(out/'fixture-video.apkm','w') as bundle:
        bundle.write(out/'fixture-video.apk','base.apk')
        bundle.write(config_apk,'config.javo.apk')
    # Local HTTPS fixture certificate is generated only for the Android test APK.
    tls=work/'transfer-test.p12'
    tls.unlink(missing_ok=True)
    if not tls.exists():
        run(Path(os.environ['JAVA_HOME'])/'bin'/('keytool'+suffix),'-J-Dkeystore.pkcs12.legacy','-genkeypair','-alias','fixture','-keyalg','RSA','-keysize','2048','-storetype','PKCS12','-keystore',tls,'-storepass','fixture-test','-keypass','fixture-test','-dname','CN=localhost','-ext','SAN=dns:localhost,ip:127.0.0.1','-validity','30','-noprompt')
    shutil.copyfile(tls,out/'transfer-test.p12')
    # Generated media contains a color field and a tone. No upstream media is packaged.
    audio=out/'javo-fixture.m4a'
    run('ffmpeg','-hide_banner','-loglevel','error','-y','-f','lavfi','-i',
        'sine=frequency=440:duration=1','-c:a','aac',audio)
    run('ffmpeg','-hide_banner','-loglevel','error','-y','-f','lavfi','-i',
        'color=c=black:s=160x96:r=24:d=1','-i',audio,'-c:v','libx264','-pix_fmt','yuv420p',
        '-c:a','copy','-shortest','-movflags','+faststart',out/'javo-fixture.mp4')
    run('ffmpeg','-hide_banner','-loglevel','error','-y','-i',out/'javo-fixture.mp4',
        '-map','0:v:0','-c:v','copy','-an','-movflags','+faststart',out/'javo-fixture-video-only.mp4')
    if args.fixture_only:
        print('Built first-party Video/Music test fixtures. No patches executed.');return
    shutil.copyfile(ROOT/'patches/javo-branding/build/libs/javo-branding-0.1.0-alpha.1.rvp',out/'javo-branding.rvp')
    # Test loading the same official snapshot whose detached signature was inspected.
    # This is diagnostic metadata loading, not execution of third-party patches.
    lock=json.loads((ROOT/'evidence/official-patches-signature.json').read_text())
    try:
        cached=ROOT/'inputs/official-patches-6.2.1.rvp'
        if cached.exists():data=cached.read_bytes()
        else:
            request=urllib.request.Request(lock['source'],headers={'User-Agent':'Javo-Integration-Check'})
            with urllib.request.urlopen(request,timeout=90) as response:data=response.read(32*1024*1024+1)
        if len(data)>32*1024*1024 or hashlib.sha256(data).hexdigest()!=lock['sha256']:raise ValueError('Official snapshot changed or exceeded size limit')
        (out/'official-inspected.rvp').write_bytes(data)
        print('Pinned official snapshot prepared for diagnostic Android metadata loading.')
    except Exception as error:
        (out/'official-inspected.rvp').unlink(missing_ok=True)
        print('Official snapshot diagnostic unavailable:',error)
    print('Prepared first-party fixture and branding bundle for Android integration testing.')
if __name__=='__main__':main()
