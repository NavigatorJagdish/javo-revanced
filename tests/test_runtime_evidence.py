"""Prevent old emulator results from being attached to a different APK build."""
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('verify_apk',Path(__file__).resolve().parents[1]/'infrastructure/scripts/verify-apk.py')
verify_apk=importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify_apk)

class RuntimeEvidenceTests(unittest.TestCase):
    digest='a'*64
    package='app.javo.manager'
    def record(self,**changes):
        return dict({'package':self.package,'installedApkSha256':self.digest,'installedArtifactMatchesDeliverable':True,'processRunning':True,'crashObserved':False},**changes)
    def check(self,record):return verify_apk.runtime_for_artifact(record,self.digest,self.package)
    def test_matching_installed_apk_is_accepted(self):self.assertIsNotNone(self.check(self.record()))
    def test_older_apk_result_is_rejected(self):self.assertIsNone(self.check(self.record(installedApkSha256='b'*64)))
    def test_other_package_is_rejected(self):self.assertIsNone(self.check(self.record(package='app.other.manager')))
    def test_missing_hash_is_rejected(self):
        record=self.record();del record['installedApkSha256'];self.assertIsNone(self.check(record))
    def test_crashed_process_is_rejected(self):self.assertIsNone(self.check(self.record(crashObserved=True)))
    def test_stopped_process_is_rejected(self):self.assertIsNone(self.check(self.record(processRunning=False)))
    def test_unconfirmed_installed_bytes_are_rejected(self):self.assertIsNone(self.check(self.record(installedArtifactMatchesDeliverable=False)))

if __name__=='__main__':unittest.main()
