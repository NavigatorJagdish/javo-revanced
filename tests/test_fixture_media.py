import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'infrastructure/scripts'))
from fixture_media import copy_media, VECTORS

class FixtureMediaTests(unittest.TestCase):
    def test_real_vectors_copy_exactly(self):
        with tempfile.TemporaryDirectory() as folder:
            out = Path(folder) / 'out'
            copy_media(ROOT / 'tests/fixture/media', out)
            self.assertEqual(set(VECTORS), {p.name for p in out.iterdir()})
            for name in VECTORS:
                self.assertEqual((out / name).read_bytes(), (ROOT / 'tests/fixture/media' / name).read_bytes())

    def test_corrupt_and_missing_vectors_fail_before_writing(self):
        for missing in (False, True):
            with self.subTest(missing=missing), tempfile.TemporaryDirectory() as folder:
                src, out = Path(folder) / 'src', Path(folder) / 'out'
                copy_media(ROOT / 'tests/fixture/media', src)
                target = src / 'javo-fixture-video-only.mp4'
                if missing:
                    target.unlink()
                else:
                    target.write_bytes(b'corrupt')
                with self.assertRaises((ValueError, FileNotFoundError)):
                    copy_media(src, out)
                self.assertFalse(out.exists())
