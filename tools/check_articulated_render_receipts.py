#!/usr/bin/env python3
"""Run pure Java receipt rules plus artifact and launch-gate tests; no native game launch."""
from pathlib import Path
import unittest

from test_articulated_render_receipts import run_pure_checks


def main():
    run_pure_checks()
    root = str(Path(__file__).resolve().parent)
    suite = unittest.TestSuite()
    for pattern in ("test_verify_articulated_render_receipts.py", "test_run_articulated_receipt_gate.py"):
        suite.addTests(unittest.defaultTestLoader.discover(root, pattern=pattern))
    return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
