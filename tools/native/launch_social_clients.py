"""Runs the social two-client suite: two real client JVMs on one dedicated server over loopback TCP.

Export the launch first (from a clean, committed tree):
    ./gradlew --offline -I tools/native/export_two_client_launch.init.gradle exportTwoClientLaunch \
        -PtwoClientSuite=social -PtwoClientLaunch=build/native/social-launch.json
then:
    python tools/native/launch_social_clients.py build/native/social-launch.json \
        --output build/native/social-<n> --accepted-eula <an eula.txt inside the repo>

The host client drives every step through files in <output>/ipc; this script only starts both clients, watches for a
failure witness or the deadline, and checks the terminal witnesses. Shares its launch-building with launch_two_clients.py.
"""
import argparse
import json
import sys
import time
import uuid
import subprocess
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import launch_two_clients as base  # noqa: E402

SUITE = "social"
CASES = ["coven", "mentoring", "coop_tribulation", "arena"]
TERMINALS = ["host-social-passed", "peer-social-passed", "peer-disconnected"]
MAX_TIMEOUT = 600


def run(options, root=base.ROOT):
    root = root.resolve()
    require = base.require
    require(1 <= options.timeout <= MAX_TIMEOUT, "Deadline must be between 1 and " + str(MAX_TIMEOUT) + " seconds")
    launch_path, out = base.output_path(options.launch, root), base.output_path(options.output, root)
    require(not out.exists(), "Output must be fresh")
    base.ignored_output(out, root)
    contract = json.loads((root / "src/gametest/resources/social-native-contract.json").read_text(encoding="utf-8"))
    require(contract.get("suite") == SUITE and contract.get("cases") == CASES, "Unexpected social contract")
    launch = json.loads(launch_path.read_text(encoding="utf-8-sig"))
    require(launch.get("suite") == SUITE, "Launch was exported for another suite")
    require(base.source_head(root) == launch["sourceHead"], "Source changed since the launch was exported")
    eula, _ = base.accepted_eula(options.accepted_eula, root)
    with base.supervisor_lock(root):
        out.mkdir(parents=True)
        ipc = out / "ipc"
        ipc.mkdir()
        nonce = str(uuid.uuid4())
        identity = {"nonce": nonce, "suite": SUITE, "sourceHead": launch["sourceHead"]}
        selected = {"suite": SUITE, "profiles": base.PROFILES}
        jobs, logs = [], []
        report = {"status": "failed", "suite": SUITE, "nonce": nonce, "sourceHead": launch["sourceHead"]}
        started = time.monotonic()
        try:
            for role in ("host", "peer"):
                game = out / role
                game.mkdir()
                (game / "tmp").mkdir()
                (game / "cache").mkdir()
                if role == "host":
                    (game / "eula.txt").write_bytes(eula)
                argfile = game / "java.args"
                argfile.write_text("\n".join(base.java_quote(arg) for arg in
                                             base.command(launch, role, game, ipc, identity, options.timeout, selected)) + "\n", encoding="utf-8")
                log = (out / (role + ".log")).open("wb")
                logs.append(log)
                jobs.append((role, subprocess.Popen([launch["java"], "@" + str(argfile)], cwd=game, env=base.environment(launch, game),
                                                    stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, shell=False)))
            while True:
                states = {role: process.poll() for role, process in jobs}
                for role in ("host", "peer"):
                    failure = ipc / (role + "-failure.properties")
                    require(not failure.exists(), role + " failed: " + (failure.read_text(encoding="iso-8859-1") if failure.exists() else ""))
                bad = {role: code for role, code in states.items() if code not in (None, 0)}
                require(not bad, "A client process failed: " + str(bad))
                require(time.monotonic() - started < options.timeout, "The two clients ran past the deadline")
                if all(code == 0 for code in states.values()):
                    break
                time.sleep(.25)
            for name in TERMINALS + ["case-" + case + "-passed" for case in CASES]:
                witness = base.read_properties(ipc / (name + ".properties"))
                require(witness.get("nonce") == nonce, "Witness from another run: " + name)
            report["status"] = "passed"
            report["cases"] = CASES
        except BaseException as error:
            report["error"] = type(error).__name__ + ": " + str(error)
            raise
        finally:
            base.stop_owned(jobs)
            for log in logs:
                log.close()
            report["seconds"] = round(time.monotonic() - started, 1)
            (out / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("launch", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--accepted-eula", type=Path)
    parser.add_argument("--timeout", type=int, default=MAX_TIMEOUT)
    report = run(parser.parse_args(argv))
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
