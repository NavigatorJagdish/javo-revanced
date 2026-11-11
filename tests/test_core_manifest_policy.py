import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('core_policy', Path(__file__).resolve().parents[1] / 'infrastructure/scripts/core_manifest_policy.py')
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)

def manifest(exported='false', extra=''):
    return '\n'.join(f'    E: service (line=1)\n      A: android:name(0x01010003)="{name}"\n      A: android:exported(0x01010010)={exported}\n{extra}' for name in sorted(policy.PRIVATE_COMPONENTS))

class CoreManifestPolicyTests(unittest.TestCase):
    def test_explicit_private_components_pass(self):
        self.assertEqual(policy.enforce_private_components(manifest()), 8)

    def test_exported_boundary_is_rejected(self):
        with self.assertRaises(ValueError):
            policy.enforce_private_components(manifest().replace('=false', '=true', 1))

    def test_missing_boundary_is_rejected(self):
        with self.assertRaises(ValueError): policy.enforce_private_components('')

    def test_nested_attribute_cannot_certify_parent(self):
        with self.assertRaises(ValueError):
            policy.enforce_private_components(manifest('true', '        E: intent-filter\n          A: android:exported(0x01010010)=false'))
