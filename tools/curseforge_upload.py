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
    metadata["gameVersions"] = [17045, 7499, 14454, 9638, 9639]
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
