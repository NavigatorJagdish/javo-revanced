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
    resources=work/'resources.zip'
    run(build/('aapt2'+suffix),'compile','--dir',ROOT/'tests/fixture/res','-o',resources)
    for product,package in [('video','com.google.android.youtube'),('music','com.google.android.apps.youtube.music')]:
        manifest=work/(product+'-manifest.xml')
        manifest.write_text((ROOT/'tests/fixture/AndroidManifest.xml').read_text().replace('com.google.android.youtube',package))
        apk=out/('fixture-'+product+'.apk')
        run(build/('aapt2'+suffix),'link','-I',android,'--manifest',manifest,'-o',apk,resources)
        with zipfile.ZipFile(apk,'a') as z:z.write(work/'classes.dex','classes.dex')
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
    run(build/('aapt2'+suffix),'link','-I',android,'--manifest',config_manifest,'--no-resource-removal','-o',config_apk,config_resources)
    with zipfile.ZipFile(out/'fixture-video.apkm','w') as bundle:
        bundle.write(out/'fixture-video.apk','base.apk')
        bundle.write(config_apk,'config.javo.apk')
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
