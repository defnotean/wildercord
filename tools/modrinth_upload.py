"""Upload a release to Modrinth using a process-only MODRINTH_TOKEN; never log the token.

Pass --create on the first upload to create the project (it starts as a draft and is
submitted for Modrinth's review once it has a version). Every upload's SHA-256 is
recorded in docs/releases/modrinth-uploads.json and the same file is refused a second
time unless --again is given. A sources JAR can be attached with --sources.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

API = "https://api.modrinth.com/v2"
FABRIC_API = "P7dR8mSH"

parser = argparse.ArgumentParser()
parser.add_argument("file", type=Path)
parser.add_argument("--sources", type=Path)
parser.add_argument("--project", default="wildercord")
parser.add_argument("--create", action="store_true", help="Create the project before uploading")
parser.add_argument("--again", action="store_true", help="Upload even though this exact file was uploaded before")
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
ledger = root / "docs/releases/modrinth-uploads.json"
uploads = json.loads(ledger.read_text(encoding="utf-8")) if ledger.exists() else []
digest = hashlib.sha256(args.file.read_bytes()).hexdigest()
earlier = [u for u in uploads if u["sha256"] == digest]
if earlier and not args.again:
    sys.exit(f"{args.file.name} was already uploaded as version {earlier[-1]['versionID']}; pass --again to upload it anyway.")
token = os.environ.get("MODRINTH_TOKEN")
if not token:
    sys.exit("MODRINTH_TOKEN is required in the process environment.")
with zipfile.ZipFile(args.file) as archive:
    full_version = json.loads(archive.read("fabric.mod.json"))["version"]
version = full_version.split("+mc")[0]
import requests  # only needed once the guards above have passed

session = requests.Session()
session.headers.update({"Authorization": token, "User-Agent": "defnotean/wildercord release tool"})


def check(response, what):
    if response.status_code not in (200, 201, 204):
        message = response.text[:2000].replace(token, "[redacted]")
        sys.exit(f"{what} returned HTTP {response.status_code}: {message}; inspect the project before retrying.")
    return response.json() if response.content else None


if args.create:
    project = {
        "slug": args.project,
        "title": "Wildercord",
        "description": (root / "docs/curseforge-summary.txt").read_text(encoding="utf-8").strip(),
        "body": (root / "docs/curseforge-description.md").read_text(encoding="utf-8"),
        "project_type": "mod",
        "categories": ["magic", "adventure", "mobs"],
        "additional_categories": ["worldgen", "equipment"],
        "client_side": "required",
        "server_side": "required",
        "license_id": "MIT",
        "source_url": "https://github.com/defnotean/wildercord",
        "issues_url": "https://github.com/defnotean/wildercord/issues",
        "wiki_url": "https://defnotean.github.io/wildercord/",
        "is_draft": True,
        "initial_versions": [],
    }
    icon = root / "src/main/resources/assets/wildercord/icon.png"
    with icon.open("rb") as image:
        created = check(session.post(f"{API}/project", files={
            "data": (None, json.dumps(project)), "icon": ("icon.png", image, "image/png")}, timeout=(30, 180)),
            "Creating the project")
    print(json.dumps({"project": created["id"], "slug": created["slug"]}))

data = {
    "name": f"Wildercord {version}",
    "version_number": full_version,
    "changelog": (root / f"docs/releases/{version}.md").read_text(encoding="utf-8"),
    "dependencies": [{"project_id": FABRIC_API, "dependency_type": "required"}],
    "game_versions": ["26.3"],
    "version_type": "alpha",
    "loaders": ["fabric"],
    "featured": True,
    "status": "listed",
    "project_id": session.get(f"{API}/project/{args.project}", timeout=30).json()["id"],
    "file_parts": ["jar"] + (["sources"] if args.sources else []),
    "primary_file": "jar",
}
# requests stalls on large multipart uploads to Modrinth (SSLWantWriteError); curl sends them in under a minute.
with tempfile.TemporaryDirectory() as scratch:
    data_file, out_file = Path(scratch, "data.json"), Path(scratch, "out.json")
    data_file.write_text(json.dumps(data), encoding="utf-8")
    form = ["-F", f"data=<{data_file};type=application/json",
            "-F", f"jar=@{args.file};type=application/java-archive"]
    if args.sources:
        form += ["-F", f"sources=@{args.sources};type=application/java-archive"]
    status = subprocess.run(["curl", "-sS", "-m", "1800", "-o", str(out_file), "-w", "%{http_code}",
                             "-H", f"Authorization: {token}", "-H", f"User-Agent: {session.headers['User-Agent']}",
                             *form, f"{API}/version"], capture_output=True, text=True).stdout.strip()
    body = out_file.read_text(encoding="utf-8") if out_file.exists() else ""
if status not in ("200", "201"):
    sys.exit(f"Uploading the version returned HTTP {status}: {body[:2000]}; inspect the project before retrying.")
result = json.loads(body)
if args.create:
    # Modrinth ignores the sides given at creation and refuses review while the environment is unknown.
    check(session.patch(f"{API}/project/{data['project_id']}", json={"client_side": "required", "server_side": "required"},
                        timeout=30), "Setting the environment")
    check(session.patch(f"{API}/project/{data['project_id']}", json={"status": "processing"}, timeout=30),
          "Submitting the project for review")
record = {"project": data["project_id"], "versionID": result["id"], "file": args.file.name, "sha256": digest}
uploads.append(record)
ledger.write_text(json.dumps(uploads, indent=2) + "\n", encoding="utf-8")
print(json.dumps(record))
