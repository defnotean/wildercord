"""Build locally importable Modrinth packs using pinned dependencies and the release jar."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROFILES = ("performance", "balanced", "cinematic")
PRESET_KEYS = {"own", "others", "reduced_flash", "camera_shake", "blade_trails", "body_aura", "impact", "banners"}


def build(profile: str, jar: Path, destination: Path) -> Path:
    lock = json.loads((ROOT / "profiles/dependencies.lock.json").read_text(encoding="utf-8-sig"))
    with zipfile.ZipFile(jar) as release:
        mod = json.loads(release.read("fabric.mod.json"))
    if mod["id"] != "wildercord" or "sources" in jar.stem:
        raise ValueError("Supply the built Wildercord release jar")
    selected = {"fabric-api", "sodium"}
    if profile == "cinematic":
        selected.add("iris")
    dependencies = [entry for entry in lock["dependencies"] if entry["project"] in selected]
    selected_ids = {entry["version_id"] for entry in dependencies}
    if {entry["project"] for entry in dependencies} != selected:
        raise ValueError("Incomplete dependency lock")
    files = []
    for entry in dependencies:
        if lock["minecraft"] not in entry["game_versions"] or "fabric" not in entry["loaders"]:
            raise ValueError(f"Incompatible dependency: {entry['project']}")
        if not set(entry["required_versions"]).issubset(selected_ids):
            raise ValueError(f"Missing required dependency of {entry['project']}")
        files.append({"path": "mods/" + entry["filename"], "hashes": entry["hashes"],
                      "env": {"client": "required", "server": "required" if entry["project"] == "fabric-api" else "unsupported"},
                      "downloads": [entry["url"]], "fileSize": entry["size"]})
    manifest = {"formatVersion": 1, "game": "minecraft", "versionId": mod["version"] + "-" + profile,
                "name": "Wildercord " + profile.title(), "summary": "Wildercord with pinned Fabric dependencies and " + profile + " visuals",
                "files": files, "dependencies": {"minecraft": lock["minecraft"], "fabric-loader": lock["fabric_loader"]}}
    destination.mkdir(parents=True, exist_ok=True)
    output = destination / ("wildercord-" + profile + "-" + mod["version"] + ".mrpack")
    config = ROOT / "profiles" / profile / "config/wildercord-visuals.json"
    settings = json.loads(config.read_text(encoding="utf-8"))
    # A profile sets everything the in-game profile buttons set (MagicQuality.PRESETS); a missing key would keep the default.
    missing = PRESET_KEYS - settings.keys()
    if missing:
        raise ValueError(f"{config} lacks preset settings: {', '.join(sorted(missing))}")
    # Stable timestamps and entry order make identical inputs reproducible.
    entries = {"modrinth.index.json": json.dumps(manifest, indent=2).encode(),
               "overrides/mods/" + jar.name: jar.read_bytes(),
               "client-overrides/config/wildercord-visuals.json": config.read_bytes(),
               "client-overrides/options.txt": ("renderDistance:" + ("10" if profile == "performance" else "12") + "\nsimulationDistance:6\nmaxFps:120\n").encode(),
               "overrides/WILDERCORD-PROFILE.md": (ROOT / "profiles/README.md").read_bytes()}
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 9, 30, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data)
    with zipfile.ZipFile(output) as archive:
        if archive.testzip() is not None:
            raise ValueError("Pack archive CRC verification failed")
        assert archive.read("overrides/mods/" + jar.name) == jar.read_bytes()
        assert json.loads(archive.read("modrinth.index.json")) == manifest
    print(f"{output}: {len(files)} pinned dependencies, SHA256 {hashlib.sha256(output.read_bytes()).hexdigest()}")
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True, help="Release jar produced by gradlew build")
    parser.add_argument("--output", type=Path, default=ROOT / "artifacts/profiles")
    args = parser.parse_args()
    for preset in PROFILES:
        build(preset, args.jar.resolve(), args.output.resolve())
