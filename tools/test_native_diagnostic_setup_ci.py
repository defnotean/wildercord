"""Check diagnostic tooling dependencies without importing those dependencies."""
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]


class DiagnosticSetupTests(unittest.TestCase):
    def test_requested_diagnostic_installs_images_before_importing_tooling(self):
        workflow = (ROOT / ".github/workflows/build.yml").read_text()
        job = workflow.split("\n  native-diagnostic:\n", 1)[1]
        job = re.split(r"\n  [a-z][a-z-]*:\n", job, maxsplit=1)[0]
        steps = re.split(r"(?=^      - )", job, flags=re.MULTILINE)[1:]

        def step(prefix):
            matches = [(index, block.rstrip()) for index, block in enumerate(steps)
                       if block.startswith("      - " + prefix + "\n")]
            self.assertEqual(len(matches), 1, f"Expected one diagnostic step: {prefix}")
            return matches[0]

        request_index, _ = step("name: Validate explicit diagnostic request and exact source revision")
        python_index, python = step("uses: actions/setup-python@v7")
        image_index, image = step("name: Install existing image-verification dependency")
        tooling_index, tooling = step("name: Check diagnostic request, scope and provenance tooling")
        self.assertLess(request_index, python_index)
        self.assertLess(python_index, image_index)
        self.assertLess(image_index, tooling_index)

        # The shared supervisor fixtures import PIL during discovery. Use the
        # configured Python rather than trying to install into system Python.
        enabled = "        if: steps.request.outputs.enabled == 'true'\n"
        self.assertEqual(python, "      - uses: actions/setup-python@v7\n" + enabled
                         + '        with:\n          python-version: "3.12"')
        self.assertEqual(image, "      - name: Install existing image-verification dependency\n"
                         + enabled + "        run: pip install pillow")
        self.assertEqual(tooling, "      - name: Check diagnostic request, scope and provenance tooling\n"
                         + enabled + "        run: python -m unittest discover -s tools -p 'test_*ci*.py' -v")


if __name__ == "__main__":
    unittest.main()
