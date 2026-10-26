"""Every Python file in tools/ parses, so a broken one-off or helper fails the build instead of its first use."""

import ast
from pathlib import Path
import unittest

TOOLS = Path(__file__).resolve().parent


class ToolsParseTest(unittest.TestCase):
    def test_every_tool_parses(self):
        broken = []
        for path in sorted(TOOLS.glob("*.py")):
            try:
                ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
            except SyntaxError as error:
                broken.append(f"{path.name}:{error.lineno}: {error.msg}")
        self.assertEqual([], broken)


if __name__ == "__main__":
    unittest.main()
