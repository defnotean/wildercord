"""The README's GIFs and the player guide's copies of them stay identical (tools/make_gif.py writes both)."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent


class GifCopiesTest(unittest.TestCase):
    def test_every_readme_gif_matches_the_guide_copy(self):
        gifs = sorted((ROOT / "docs/images").glob("*.gif"))
        self.assertTrue(gifs, "docs/images has no GIFs")
        for gif in gifs:
            copy = ROOT / "wiki/assets/images" / gif.name
            self.assertTrue(copy.exists(), f"{copy.relative_to(ROOT)} is missing")
            self.assertEqual(gif.read_bytes(), copy.read_bytes(), f"{gif.name} differs from the guide's copy")


if __name__ == "__main__":
    unittest.main()
