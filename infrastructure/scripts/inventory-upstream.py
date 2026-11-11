"""Compare preserved upstream archives with the actual packaged source selection."""
from pathlib import Path,PurePosixPath
import hashlib,importlib.util,json,zipfile

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('package',ROOT/'infrastructure/scripts/package.py')
pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
selected={name:file for file,name in pack.source_files()}
security=json.loads((ROOT/'packages/source-provenance/security-backports.json').read_text())
removed=security['removedUpstreamFiles']
modified=[];complete={}
for archive,target in [('core','apps/javo-core'),('manager','apps/javo-manager'),('patcher','upstream/patcher'),('library','upstream/library'),('patches','upstream/patches')]:
    original=set();missing=[];security_removed=[]
    with zipfile.ZipFile(ROOT/'inputs'/f'{archive}-upstream.zip') as source:
        for item in source.infolist():
            if item.is_dir():continue
            name=target+'/'+'/'.join(PurePosixPath(item.filename).parts[1:]);original.add(name)
            if name not in selected:
                if name in removed:
                    security_removed.append({'file':name,'upstreamCommit':removed[name]})
                    modified.append({'file':name,'upstreamSha256':hashlib.sha256(source.read(item)).hexdigest(),'javoSha256':None,'removedByUpstreamSecurityCommit':removed[name]})
                else:missing.append(name)
                continue
            old=hashlib.sha256(source.read(item)).hexdigest()
            current=hashlib.sha256(selected[name].read_bytes()).hexdigest()
            if old!=current:modified.append({'file':name,'upstreamSha256':old,'javoSha256':current})
    for name,file in selected.items():
        if name.startswith(target+'/') and name not in original:
            modified.append({'file':name,'upstreamSha256':None,'javoSha256':hashlib.sha256(file.read_bytes()).hexdigest()})
    complete[archive]={'upstreamFiles':len(original),'missingFromSourceArchive':missing,'removedOrMovedByUpstreamSecurityFix':security_removed}
evidence=ROOT/'evidence'
(evidence/'upstream-source-completeness.json').write_text(json.dumps(complete,indent=2)+'\n',encoding='utf-8')
(evidence/'source-modifications.json').write_text(json.dumps(sorted(modified,key=lambda x:x['file']),indent=2)+'\n',encoding='utf-8')
print(json.dumps({'sourceFilesChangedOrAdded':len(modified),'upstreamCompleteness':complete},indent=2))
if any(x['missingFromSourceArchive'] for x in complete.values()):raise SystemExit('Source archive is incomplete')
