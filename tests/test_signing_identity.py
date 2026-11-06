"""Development identities must survive cache eviction without silently changing certificates."""
import io,json,os,runpy,subprocess,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch

SOURCE=Path(__file__).resolve().parents[1]/'infrastructure/scripts/restore-manager-signing.py'
class IdentityTests(unittest.TestCase):
    def scenario(self, artifact=None, released=False, pinned=False, cached=False, zip_name='debug.keystore'):
        temp=tempfile.TemporaryDirectory();self.addCleanup(temp.cleanup)
        root=Path(temp.name);script=root/'infrastructure/scripts/restore.py';script.parent.mkdir(parents=True);script.write_bytes(SOURCE.read_bytes())
        home=root/'home';key=home/'.android/debug.keystore'
        if cached:key.parent.mkdir(parents=True);key.write_bytes(b'cached fixture')
        if pinned:
            receipt=root/'evidence/release-alpha6.json';receipt.parent.mkdir();receipt.write_text(json.dumps({'managerSignerSha256':'a'*64}))
        payload=io.BytesIO()
        with zipfile.ZipFile(payload,'w') as z:z.writestr(zip_name,b'private fixture identity')
        def output(args,**kwargs):
            if args[0]=='keytool':return b'different certificate'
            if args[-1].endswith('/zip'):return payload.getvalue()
            return json.dumps({'artifacts':[] if artifact is None else [{'id':12,'created_at':'2026-11-06','expired':False,'workflow_run':{'head_branch':'main'}}]}).encode()
        def execute():
            with patch('pathlib.Path.home',return_value=home),patch.dict(os.environ,{'GITHUB_REPOSITORY':'owner/private'}),patch('subprocess.check_output',side_effect=output),patch('subprocess.run',return_value=subprocess.CompletedProcess([],0 if released else 1)):
                runpy.run_path(str(script),run_name='__main__')
        return execute,key
    def test_existing_release_without_identity_fails(self):
        execute,key=self.scenario(released=True)
        with self.assertRaisesRegex(RuntimeError,'refusing to rotate'):execute()
        self.assertFalse(key.exists())
    def test_cached_wrong_certificate_is_rejected(self):
        execute,key=self.scenario(cached=True,pinned=True)
        with self.assertRaisesRegex(RuntimeError,'differs from the pinned'):execute()
        self.assertEqual(key.read_bytes(),b'cached fixture')
    def test_private_backup_recovery(self):
        execute,key=self.scenario(artifact=True);execute()
        self.assertEqual(key.read_bytes(),b'private fixture identity')
    def test_unexpected_archive_path_is_rejected(self):
        execute,key=self.scenario(artifact=True,zip_name='../debug.keystore')
        with self.assertRaisesRegex(RuntimeError,'Invalid private signing backup'):execute()
        self.assertFalse(key.exists())
