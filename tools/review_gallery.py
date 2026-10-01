"""Index archived game captures and compare equipment icons without modifying any images."""
from pathlib import Path
from html import escape

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "artifacts/review"
OUTPUT.mkdir(parents=True, exist_ok=True)
cards = []
# Prefer the final full run and its focused fixes; retain older archives on disk.
sources = [OUTPUT / name for name in ("complete", "expeditions_final", "magic_final", "expanded_complete", "shaders", "physical", "expanded_magic", "circles", "circle_magic")]
paths = [path for source in sources if source.exists() for path in source.rglob("*.png")
         if source.name != "expeditions_final" or path.name.startswith("expedition_")]
paths = list({path.name: path for path in paths}.values())
if not (OUTPUT / "complete").exists():
    paths = list(OUTPUT.rglob("*.png"))
for path in sorted(paths):
    if path.name.startswith("animation_"):
        continue
    src = path.relative_to(OUTPUT).as_posix()
    label = src.removesuffix(".png").replace("_", " ")
    cards.append(f'<a class="card" href="{escape(src)}" data-label="{escape(label.lower())}"><img loading="lazy" src="{escape(src)}" alt="{escape(label)}"><span>{escape(label)}</span></a>')
icons = []
for stem in ["focus_of_resolve", "focus_of_reprieve", "focus_of_grounding", "keepers_hourglass", "living_seedpod", "sky_feather", "cinnamon_toy", "emberweave", "rimebound", "stonebound", "mirror_thread_mantle"]:
    for path in sorted((ROOT / "src/main/resources/assets/wildercord/textures/item").glob(stem + "*.png")):
        src = "../../" + path.relative_to(ROOT).as_posix()
        icons.append(f'<div class="icon"><img src="{escape(src)}" alt="{escape(path.stem)}"><span>{escape(path.stem.replace("_", " "))}</span></div>')
def asset_icon(path, label):
    src = "../../" + path.relative_to(ROOT).as_posix()
    return f'<div class="icon"><div class="sprite"><img src="{escape(src)}" alt="{escape(label)}"></div><span>{escape(label)}</span></div>'

new_runes = ["strata_rise", "tidal_lift", "wind_steps", "cinder_bulwark", "root_bulwark", "boiling_surge", "thunder_tide", "rime_causeway", "thunder_walk"]
rune_icons = [asset_icon(ROOT / f"src/main/resources/assets/wildercord/textures/item/rune/{name}.png", name.replace("_", " ")) for name in new_runes]
circle_names = ["needle", "bloom", "gyre", "anchor", "reservoir", "crucible", "confluence", "pilgrim", "vigil", "mercy", "tempest", "eclipse"]
circle_icons = [asset_icon(ROOT / f"src/main/resources/assets/wildercord/textures/item/rune/{name}_circle.png", name.title()+" Circle") for name in circle_names]
material_names = ["ember", "frost", "storm", "wind", "stone", "petal", "void", "arcane", "time", "blood", "water", "vapour"]
material_icons = [asset_icon(ROOT / f"src/main/resources/assets/wildercord/textures/particle/material_{style}_0.png", name) for style, name in enumerate(material_names)]

gallery = next((path for path in (OUTPUT / "expanded_complete/animation-gallery.html", OUTPUT / "complete/animation-gallery.html", OUTPUT / "integrated/animation-gallery.html") if path.exists()), None)
link = f'<p><a href="{escape(gallery.relative_to(OUTPUT).as_posix())}">Open the complete labelled release and impact animation gallery</a></p>' if gallery else ""
html = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>WilderCord review</title>
<style>body{margin:0;background:#13121a;color:#f3e9d5;font:16px system-ui}main{max-width:1440px;margin:auto;padding:28px}h1,h2{color:#e9bf6b}a{color:#99e6dc}input{background:#242331;color:white;border:1px solid #59546b;padding:12px;width:min(600px,90%);margin:12px 0 24px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(330px,1fr));gap:16px}.card{display:block;background:#242331;border-radius:8px;overflow:hidden;text-decoration:none}.card img{width:100%;display:block}.card span{display:block;padding:12px}.icons{display:flex;flex-wrap:wrap;gap:14px}.icon{width:135px;text-align:center;background:#242331;padding:12px}.icon img{width:96px;height:96px;image-rendering:pixelated}.icon span{display:block;font-size:13px}.card[hidden]{display:none}</style>
<main><h1>WilderCord implementation review</h1><p>Archived screenshots from the running Minecraft client. Search by spell, shape, dungeon, armour, Cinnamon, or notebook. Screenshots show selected moments; the animation gallery gives labelled timing frames.</p>'''
html += '<style>.sprite{width:96px;height:96px;overflow:hidden;margin:auto}.sprite img{height:auto}</style>'
html += '<h2>Circle disciplines</h2><p>Search the captures for circle to compare opening, formed, motion and first-person release frames for all twelve disciplines.</p><div class="icons">' + "".join(circle_icons) + '</div>'
html += link + '<h2>Equipment artwork</h2><div class="icons">' + "".join(icons) + '</div><h2>New physical runes</h2><div class="icons">' + "".join(rune_icons) + '</div><h2>Original spell materials</h2><div class="icons">' + "".join(material_icons) + '</div><h2>In-game captures</h2><input aria-label="Filter screenshots" placeholder="Filter screenshots…" oninput="const q=this.value.toLowerCase();document.querySelectorAll(\'.card\').forEach(c=>c.hidden=!c.dataset.label.includes(q))"><div class="grid">' + "".join(cards) + '</div></main></html>'
(OUTPUT / "index.html").write_text(html, encoding="utf-8")
print(f"Indexed {len(cards)} captures, {len(icons)} equipment icons, {len(rune_icons)} physical runes, {len(circle_icons)} circle disciplines and {len(material_icons)} original material sprites")
