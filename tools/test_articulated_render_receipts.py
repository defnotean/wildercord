#!/usr/bin/env python3
"""Run the GameTest-only pure receipt checks without putting test-mod code in production/test outputs."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

def run_pure_checks():
    repo = Path(__file__).resolve().parents[1]
    java_home = os.environ.get("JAVA_HOME")
    javac = str(Path(java_home) / "bin/javac") if java_home else shutil.which("javac")
    java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
    if not javac or not java:
        raise SystemExit("Set JAVA_HOME to the project's Java 25 toolchain.")
    sources = repo / "src/gametest/java/dev/wildercord/gametest"
    with tempfile.TemporaryDirectory(prefix="articulated-receipt-classes-") as output:
        subprocess.run([javac, "--release", "25", "-d", output,
                        str(sources / "ArticulatedRenderReceipt.java"),
                        str(sources / "ArticulatedRenderReceiptChecks.java")], check=True)
        subprocess.run([java, "-Djava.awt.headless=true", "-cp", output,
                        "dev.wildercord.gametest.ArticulatedRenderReceiptChecks"], check=True)


if __name__ == "__main__":
    run_pure_checks()
