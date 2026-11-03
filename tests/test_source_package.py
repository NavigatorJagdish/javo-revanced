"""Ensure archive filtering preserves real packages and excludes local tooling."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec=importlib.util.spec_from_file_location('pack',Path(__file__).resolve().parents[1]/'infrastructure/scripts/package.py')
pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)

class SourcePackageTests(unittest.TestCase):
    def test_source_packages_survive_and_generated_files_do_not(self):
        keep=['apps/core/src/main/java/org/microg/tools/Ui.kt',
              'upstream/patcher/src/main/java/com/android/tools/Tool.java',
              'apps/core/src/main/java/example/build/Builder.java']
        drop=['tools/sdk/bin/tool.exe','deliverables/old.zip','apps/core/build/tmp/File.kt',
              'apps/site/dist/index.html','apps/core/.gradle/cache','apps/core/local.properties',
              'apps/core/secret.jks','inputs/upstream.zip']
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            for name in keep+drop:
                p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('fixture')
            self.assertEqual(set(keep),{name for _,name in pack.source_files(root)})
    def test_real_core_ui_helper_is_packaged(self):
        names={name for _,name in pack.source_files()}
        self.assertTrue(any('/org/microg/tools/' in n and n.endswith('.java') for n in names))

if __name__=='__main__':unittest.main()

