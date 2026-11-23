"""Upload a release using a process-only CURSEFORGE_TOKEN; never log the token.

Use --parent FILE_ID for a sources attachment. This API has no idempotency key,
so every upload's SHA-256 is recorded in docs/releases/curseforge-uploads.json and
the same file is refused a second time unless --again is given.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("file", type=Path)
parser.add_argument("--project", type=int, default=1716203)
parser.add_argument("--parent", type=int)
parser.add_argument("--version", help="Release version for an unexpanded sources JAR")
parser.add_argument("--again", action="store_true", help="Upload even though this exact file was uploaded before")
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
ledger = root / "docs/releases/curseforge-uploads.json"
uploads = json.loads(ledger.read_text(encoding="utf-8")) if ledger.exists() else []
digest = hashlib.sha256(args.file.read_bytes()).hexdigest()
earlier = [u for u in uploads if u["sha256"] == digest and u["project"] == args.project]
if earlier and not args.again:
    sys.exit(f"{args.file.name} was already uploaded as file {earlier[-1]['fileID']}; pass --again to upload it anyway.")
token = os.environ.get("CURSEFORGE_TOKEN")
if not token:
    sys.exit("CURSEFORGE_TOKEN is required in the process environment.")
# Game-version ids from minecraft.curseforge.com/api/game/versions (read 2026-11-23 with
# the upload token; the Core API key is not needed for that endpoint). Each Minecraft
# version has several records there; the 26.3 upload used the dedicated record for the
# version family, so the map below keeps that same record per version. Java is 25 on the
# 26.x line and 21 on 1.21.x, Fabric/Client/Server are shared ids.
MC_GAME_VERSION = {
    "26.3": 17045, "26.2": 16498, "26.1.2": 16082, "26.1": 15933,
    "1.21.11": 14406, "1.21.10": 13964, "1.21.9": 13927, "1.21.8": 13620,
    "1.21.7": 13506, "1.21.6": 13422, "1.21.5": 12934, "1.21.4": 12281,
    "1.21.3": 12084, "1.21.2": 12079, "1.21.1": 11779,
}
FABRIC_ID = 7499
JAVA_ID = {"26": 14454, "1.21": 11135}
CLIENT_ID, SERVER_ID = 9638, 9639

with zipfile.ZipFile(args.file) as archive:
    version = args.version or json.loads(archive.read("fabric.mod.json"))["version"].split("+mc")[0]
metadata = {
    "changelog": (root / f"docs/releases/{version}.md").read_text(encoding="utf-8"),
    "changelogType": "markdown",
    "displayName": f"Wildercord {version}" + (" — Sources" if args.parent else ""),
    "releaseType": "alpha",
    "isMarkedForManualRelease": False,
}
if args.parent:
    metadata["parentFileID"] = args.parent
else:
    mc = args.file.name.split("+mc", 1)[1].split("-", 1)[0] if "+mc" in args.file.name else None
    if mc and mc.endswith(".jar"):
        mc = mc[:-len(".jar")]
    if mc not in MC_GAME_VERSION:
        sys.exit(f"{args.file.name} carries no mapped Minecraft version (found {mc!r}); "
                 f"known: {', '.join(MC_GAME_VERSION)}")
    metadata["gameVersions"] = [MC_GAME_VERSION[mc], FABRIC_ID,
                                JAVA_ID["26" if mc.startswith("26") else "1.21"], CLIENT_ID, SERVER_ID]
    metadata["relations"] = {"projects": [{"slug": "fabric-api", "projectID": 306612,
                                             "type": "requiredDependency"}]}
import requests  # only needed once the guards above have passed

with args.file.open("rb") as file:
    response = requests.post(
        f"https://minecraft.curseforge.com/api/projects/{args.project}/upload-file",
        headers={"X-Api-Token": token},
        files={"metadata": (None, json.dumps(metadata)),
               "file": (args.file.name, file, "application/java-archive")},
        timeout=(30, 180), allow_redirects=False,
    )
if response.status_code != 200:
    # Do not dump request objects, headers or redirects containing credentials.
    message = response.text[:2000].replace(token, "[redacted]")
    sys.exit(f"Upload returned HTTP {response.status_code}: {message}; inspect project before retrying.")
result = response.json()
record = {"project": args.project, "fileID": result.get("id"), "file": args.file.name, "sha256": digest}
uploads.append(record)
ledger.write_text(json.dumps(uploads, indent=2) + "\n", encoding="utf-8")
print(json.dumps(record))
