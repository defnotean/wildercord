"""Actual platform file handles preserve bounded, non-link evidence reads."""
import os
from pathlib import Path
import tempfile
import unittest

from native_evidence import read_text, windows_descriptor


class NativeEvidenceTests(unittest.TestCase):
    def test_exact_size_utf8_bom_and_unchanged_bytes(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "receipt.json"
            raw = b'\xef\xbb\xbf{"snow":"\xe9\x9b\xaa"}\r\n'
            path.write_bytes(raw)
            self.assertEqual(read_text(path, len(raw)), '{"snow":"雪"}\r\n')
            self.assertEqual(path.read_bytes(), raw)
            with self.assertRaises(ValueError):
                read_text(path, len(raw) - 1)

    def test_missing_directory_and_invalid_limits_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for path in (root, root / "missing"):
                with self.assertRaises((ValueError, OSError)):
                    read_text(path, 100)
            for limit in (0, -1, True, 1.5):
                with self.assertRaises(ValueError):
                    read_text(root, limit)

    def test_actual_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "target"
            target.write_text("private-target")
            link = root / "link"
            # This check intentionally fails if the test host cannot create links;
            # it must not silently count unexercised Windows link rejection as a pass.
            link.symlink_to(target)
            with self.assertRaises((ValueError, OSError)):
                read_text(link, 100)
            if os.name == "nt":
                with self.assertRaises((ValueError, OSError)):
                    windows_descriptor(link)

    @unittest.skipUnless(os.name == "nt", "Windows sharing contract")
    def test_existing_writer_is_refused_and_read_handle_denies_new_writer(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "live.log"
            with path.open("wb") as writer:
                writer.write(b"unfinished"); writer.flush()
                with self.assertRaises(OSError):
                    read_text(path, 100)
            descriptor = windows_descriptor(path)
            try:
                with self.assertRaises(OSError):
                    path.open("wb")
                with self.assertRaises(OSError):
                    path.unlink()
                self.assertEqual(os.read(descriptor, 100), b"unfinished")
            finally:
                os.close(descriptor)
            self.assertEqual(read_text(path, 100), "unfinished")


if __name__ == "__main__":
    unittest.main()
