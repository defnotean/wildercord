"""Keep recent boss guidance intact when the ordinary asset pipeline runs."""

import json
from pathlib import Path
import unittest

import boss_encounter_text

ROOT = Path(__file__).resolve().parent.parent


class BossEncounterTextTest(unittest.TestCase):
    def test_packaged_language_preserves_authored_encounters(self):
        language = json.loads((ROOT / "src/main/resources/assets/wildercord/lang/en_us.json").read_text())
        for key, value in boss_encounter_text.LANG.items():
            with self.subTest(key=key):
                self.assertEqual(value, language.get(key), "Regeneration lost current encounter guidance")
        for key in boss_encounter_text.RETIRED_KEYS:
            self.assertNotIn(key, language, "A retired move's label must not return")

    def test_each_recent_boss_keeps_its_readable_warning(self):
        for boss in (
            "archivist", "cinder_warden", "star_eater", "tide_scribe", "root_guardian", "storm_conductor",
            "rootcoil_serpent", "drowned_bell_warden", "ashen_stag", "mojito", "gravekeeper", "hollow_matron",
            "frost_wyrmling", "lantern_bearer",
        ):
            with self.subTest(boss=boss):
                warnings = [text for key, text in boss_encounter_text.LANG.items()
                            if key.startswith(f"boss.wildercord.{boss}.move.")]
                self.assertGreaterEqual(len(warnings), 6)
                self.assertTrue(all(text.strip() for text in warnings))


if __name__ == "__main__":
    unittest.main()
