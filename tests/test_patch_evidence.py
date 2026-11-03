"""A successful old or partial patch test must not certify a new bundle/Manager."""
import hashlib,importlib.util,tempfile,unittest
from pathlib import Path

spec=importlib.util.spec_from_file_location('pack',Path(__file__).resolve().parents[1]/'infrastructure/scripts/package.py')
pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)

class PatchEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.bundle=Path(self.temp.name)/'bundle.rvp';self.bundle.write_bytes(b'compiled bundle test bytes')
        self.catalog={'artifacts':[{'id':'manager','sha256':'a'*64}]}
        self.report={'success':True,'bundleSha256':hashlib.sha256(self.bundle.read_bytes()).hexdigest(),'managerApkSha256':'a'*64,
            'products':[{'id':p,'success':True,'patchedFixtureInstalled':True,'fixtureProcessRunning':True,'fixtureCrashObserved':False} for p in ['video','music']]}
    def check(self):pack.verify_branding_evidence(self.bundle,self.report,self.catalog)
    def test_matching_complete_result_accepted(self):self.check()
    def test_changed_bundle_rejected(self):
        self.bundle.write_bytes(b'new untested bundle')
        with self.assertRaises(ValueError):self.check()
    def test_new_manager_cannot_inherit_prior_integration(self):
        self.catalog['artifacts'][0]['sha256']='b'*64
        with self.assertRaises(ValueError):self.check()
    def test_instrumentation_without_fixture_launch_is_rejected(self):
        self.report['products'][1].pop('patchedFixtureInstalled')
        with self.assertRaises(ValueError):self.check()
    def test_failed_check_is_rejected(self):
        self.report['success']=False
        with self.assertRaises(ValueError):self.check()

if __name__=='__main__':unittest.main()
