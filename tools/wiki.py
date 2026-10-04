"""The player wiki's reference pages, written from the same data the game uses (Runes.java, the recipes,
the loot and the fusions), so they can never drift from the mod:

    wiki/runes/*.md             every rune, by family (effects by element): what it does, how to get it,
                                what it costs and which modifiers work with it
    wiki/items/rune-recipes.md  every rune recipe, by tier
    wiki/assets/runes/*.png     every rune's icon, enlarged

The rest of the wiki is written by hand. Run from the project root:  python tools/wiki.py
"""
import re
import shutil
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate_assets as g  # noqa: E402
import wiki_recipes  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
WIKI = ROOT / "wiki"
RUNES_JAVA = ROOT / "src/main/java/dev/wildercord/spell/Runes.java"
TEXTURES = ROOT / "src/main/resources/assets/wildercord/textures/item/rune"

def _vanilla_names():
    """Vanilla items' English names from the game jar (so the wiki says "Hay Bale", as the game does), if it's there."""
    import json
    import zipfile
    jars = sorted((Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft").glob("minecraft-clientonly-deobf/*/minecraft-clientonly-deobf-*.jar"))
    if not jars:
        return {}
    with zipfile.ZipFile(jars[-1]) as jar:
        lang = json.loads(jar.read("assets/minecraft/lang/en_us.json").decode("utf-8"))
    names = {}
    for key, value in lang.items():
        parts = key.split(".")
        if len(parts) == 3 and parts[0] in ("item", "block") and parts[1] == "minecraft":
            names.setdefault("minecraft:" + parts[2], value)
    return names


_VANILLA = _vanilla_names()
_item_name = g.item_name


def _named(item_id):
    return _VANILLA.get(item_id) or _item_name(item_id)


g.item_name = _named

ELEMENTS = ["fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"]
ELEMENT_BLURB = {
    "fire": "Burning, blasts and heat. Fire lights what it touches and boils water into steam.",
    "frost": "Cold, ice and water. Frost slows, freezes and shatters, and freezes water you can walk on.",
    "storm": "Lightning and shock. Storm strikes hard, chains between foes and runs through water.",
    "wind": "Air and motion. Wind throws, lifts, dashes, turns arrows aside and carries you through the sky.",
    "earth": "Stone and ground. Earth shields, roots, heaves the ground and breaks blocks.",
    "life": "Healing and growth. Life mends allies, cleanses poisons and makes plants grow.",
    "void": "Darkness, gravity and space. Void pulls, blinks, withers and swallows light.",
    "arcane": "Pure magic. Arcane strikes, reveals, silences, summons and bends the rules.",
    "time": "Slowing, speeding and rewinding. Time stops foes and turns back the clock.",
    "blood": "Life paid for power. Blood cuts, drains, bleeds and turns wounds into strength.",
}
KIND = {"HELPFUL": "Helps you and your allies", "HARMFUL": "Harms enemies", "WORLD": "Works on the world",
        "MOVEMENT": "Moves you", "NONE": ""}
TIER_NAMES = {1: "I", 2: "II", 3: "III", 4: "IV"}
TIER_EXTRAS = {1: "nothing extra", 2: "2 Lapis Lazuli and a Gold Ingot", 3: "a Mana Crystal and a Diamond"}
CORD_FOR_TIER = {1: "any Cord", 2: "a Copper Cord or better", 3: "an Amethyst Cord or better", 4: "an Echo Cord"}
# The modifier that needs each trait, as a player knows it.
FAMILY_TITLE = {"shape": "Shapes", "modifier": "Modifiers", "link": "Links"}


def read():
    """Every rune in Runes.java, in its order: family, id, name, tier, cost, element, kind, traits, description."""
    src = RUNES_JAVA.read_text(encoding="utf-8")
    # Helpful effects can be shared (Kindred), bar the few Runes.java lists as unshared.
    unshared = set(re.findall(r'"(\w+)"', re.search(r"PATHS = Set\.of\((.*?)\);", src).group(1)))
    runes = []
    pattern = re.compile(r'public static final RuneDef (\w+) = (shape|effect|modifier|link)\((.*)\);\s*$', re.M)
    for m in pattern.finditer(src):
        const, family, args = m.groups()
        strings = re.findall(r'"((?:[^"\\]|\\.)*)"', args)
        path, name = strings[0], strings[1]
        rest = re.sub(r'"((?:[^"\\]|\\.)*)"', '""', args)
        parts = [p.strip() for p in rest.split(",")]
        tier = int(parts[2])
        r = {"const": const, "family": family, "path": path, "name": name, "tier": tier, "element": "", "kind": "NONE",
             "desc": strings[-1].replace('\\"', '"'), "traits": set(), "needs": ""}
        idents = [p for p in parts[3:] if re.fullmatch(r"[A-Z_]+", p)]
        if family == "shape":
            r["cost"], r["mult"] = float(parts[3]), float(parts[4])
            r["traits"] = set(idents) | {"COOLDOWN"}
        elif family == "effect":
            r["cost"] = float(parts[3])
            r["element"] = strings[2]
            r["kind"] = re.search(r"EffectKind\.(\w+)", args).group(1)
            r["traits"] = {i for i in idents if not i.startswith("EffectKind")} | {"FRUGAL"}
            if r["kind"] == "HELPFUL" and path not in unshared:
                r["traits"].add("SHARE")
        elif family == "modifier":
            r["mult"] = float(parts[3])
            r["needs"] = idents[0] if idents else ""
        else:
            r["cost"] = float(parts[3])
            r["traits"] = set(idents)
        runes.append(r)
    return runes


def icon(path):
    """The rune's icon (its first frame), four times as big, into the wiki's assets."""
    out = WIKI / "assets/runes"
    out.mkdir(parents=True, exist_ok=True)
    im = Image.open(TEXTURES / f"{path}.png").convert("RGBA")
    im = im.crop((0, 0, 16, 16)).resize((64, 64), Image.NEAREST)
    im.save(out / f"{path}.png")


def img(path, size=32):
    return (f'<img src="{{{{ \'/assets/runes/{path}.png\' | relative_url }}}}" alt="" width="{size}" height="{size}" '
            f'class="rune-icon">')


def number(x):
    return str(int(x)) if abs(x - round(x)) < 1e-9 else f"{x:g}"


# The signature fusions (path -> its two runes' paths) and every rune by path, read in main().
SIGNATURES = {}
BY_PATH = {}


def rune_link(path, world):
    """A link to a rune's entry on its own page (effects by element, the runes of the world on theirs)."""
    r = BY_PATH[path]
    if path in world:
        page_url = "/runes/world/"
    elif r["family"] == "effect":
        page_url = f"/runes/effects/{r['element']}/"
    else:
        page_url = f"/runes/{FAMILY_TITLE[r['family']].lower()}/"
    return f"[{r['name']}]({{{{ '{page_url}' | relative_url }}}}#{path})"


def how_to_get(r, fused, found, world):
    path = r["path"]
    if path in g.INNATE:
        return "Never crafted or found: one innate rune wakes in each caster's heart at the 1st Heart Circle, chosen at random."
    if path in SIGNATURES:
        a, b = SIGNATURES[path]
        over = next((p for p, pair in fused.items() if set(pair) == {BY_PATH[a]["element"], BY_PATH[b]["element"]}), None)
        instead = f" Any other {BY_PATH[a]['element'].title()} and {BY_PATH[b]['element'].title()} effects make {BY_PATH[over]['name']} instead." if over else ""
        return (f"A signature fusion: fused at the [Fusion Altar]({{{{ '/fusion-altar/' | relative_url }}}}) from {rune_link(a, world)} and "
                f"{rune_link(b, world)} themselves, with an amethyst shard (3 XP levels).{instead}")
    if path in fused:
        a, b = fused[path]
        if a == b:
            return (f"Fused at the [Fusion Altar]({{{{ '/fusion-altar/' | relative_url }}}}) from any two {a.title()} effects and an amethyst "
                    "shard (3 XP levels).")
        return (f"Fused at the [Fusion Altar]({{{{ '/fusion-altar/' | relative_url }}}}) from any {a.title()} effect and any {b.title()} "
                "effect, with an amethyst shard (3 XP levels).")
    if path in world or r["tier"] == 4:
        places = found.get(path, ["?"])
        return "Found only, never crafted: " + "; ".join(places) + "."
    extra = "" if r["tier"] == 1 else f", plus {TIER_EXTRAS[r['tier']]}"
    also = found.get(path, [])
    also_text = f" Also found: {'; '.join(also)}." if also else ""
    return f"Craft: a Blank Rune, {items_text(path)}{extra}. The recipe is shapeless: any layout, any crafting grid.{also_text}"


def items_text(path):
    """A rune's own ingredients in plain words: "Coal and Flint", "2x Feather, Sugar and Redstone Dust"."""
    parts = g.recipe_text(["wildercord:blank_rune", *g.RUNE_RECIPES[path]]).split(", ")
    return parts[0] if len(parts) == 1 else ", ".join(parts[:-1]) + " and " + parts[-1]


def recipe_img(recipe_id, alt):
    """A recipe's crafting grid, as the game draws it."""
    alt = alt.replace('"', "'")
    src = "{{ '/assets/recipes/" + recipe_id + ".png' | relative_url }}"
    return f'<img src="{src}" alt="{alt}" class="recipe-grid" loading="lazy">'


def works_with(r, modifiers):
    names = [m["name"] for m in modifiers if m["needs"] and m["needs"] in r["traits"]]
    return ", ".join(names)


def entry(r, fused, found, world, modifiers):
    # An explicit anchor (the rune's id), so other pages can link straight to the entry.
    lines = [f"### {img(r['path'])} {r['name']}", f"{{: #{r['path']}}}", ""]
    facts = [f"Tier {TIER_NAMES[r['tier']]}"]
    if r["family"] == "effect":
        facts.append(r["element"].title())
        if KIND[r["kind"]]:
            facts.append(KIND[r["kind"]])
        facts.append(f"{number(r['cost'])} mana")
    elif r["family"] == "shape":
        facts.append(f"{number(r['cost'])} mana")
        if abs(r["mult"] - 1.0) > 1e-9:
            facts.append(f"its effects cost x{number(r['mult'])}")
    elif r["family"] == "modifier":
        facts.append(f"cost x{number(r['mult'])}")
    else:
        facts.append(f"{number(r['cost'])} mana")
    facts.append(f"needs {CORD_FOR_TIER[r['tier']]}")
    lines += [f"*{' · '.join(facts)}*", "", r["desc"], "",
              f"**How to get it:** {how_to_get(r, fused, found, world)}", ""]
    if (WIKI / f"assets/recipes/rune_{r['path']}.png").exists() and r["path"] not in fused and r["path"] not in SIGNATURES and r["path"] not in world \
            and r["path"] not in g.INNATE:
        lines += [recipe_img(f"rune_{r['path']}", f"Crafting {r['name']}: a Blank Rune and {items_text(r['path'])}"
                             + ("" if r["tier"] == 1 else f", plus {TIER_EXTRAS[r['tier']]}")), ""]
    if r["family"] == "modifier":
        targets = {"POWER": "anything with power (damage, healing, force)", "DURATION": "anything that lasts",
                   "RADIUS": "anything with an area", "SPEED": "anything that flies", "PIERCE": "projectiles and beams",
                   "BOUNCE": "projectiles", "SPLIT": "projectiles and beams", "HOMING": "projectiles", "CHAIN": "anything that can jump to a new target",
                   "LINGER": "effects that can land again over time", "FRUGAL": "any effect", "VOLLEY": "projectiles and beams",
                   "SHARE": "a helpful effect that lands on each creature it touches (healing, a buff, a ward)",
                   "COOLDOWN": "any shape (it changes the whole spell, so its cost multiplies the whole spell's, wherever it sits)"}.get(r["needs"], "")
        if targets:
            lines += [f"**Attaches to:** the closest rune on its left that is {targets}.", ""]
    else:
        ww = works_with(r, modifiers)
        if ww:
            lines += [f"**Modifiers that work on it:** {ww}", ""]
    return lines


def page(path, front, intro, body):
    path.parent.mkdir(parents=True, exist_ok=True)
    head = ["---"] + [f"{k}: {v}" for k, v in front.items()] + ["---", ""]
    note = ["<!-- Generated by tools/wiki.py from the game's own data: edit that, not this page. -->", ""]
    path.write_text("\n".join(head + note + intro + [""] + body) + "\n", encoding="utf-8", newline="\n")


def main():
    runes = read()
    fused = g.read_fusions()
    SIGNATURES.update(g.read_signatures())
    BY_PATH.update({r["path"]: r for r in runes})
    world = g.found_only()
    found = g.loot_sources()
    modifiers = [r for r in runes if r["family"] == "modifier"]
    for r in runes:
        icon(r["path"])
    recipes_dir = WIKI / "assets/recipes"
    if recipes_dir.exists():
        shutil.rmtree(recipes_dir)
    wiki_recipes.render(recipes_dir)
    out = WIKI / "runes"
    if out.exists():
        for f in out.rglob("*.md"):
            if "Generated by tools/wiki.py" in f.read_text(encoding="utf-8"):
                f.unlink()
    special = set(fused) | set(SIGNATURES) | set(world) | set(g.INNATE)

    def body_of(rs):
        lines = []
        for r in sorted(rs, key=lambda r: (r["tier"], r["name"])):
            lines += entry(r, fused, found, world, modifiers)
        return lines

    # Shapes, modifiers, links: the craftable and Tier IV ones (runes of the world have their own page).
    order = {"shape": 1, "modifier": 3, "link": 4}
    intros = {
        "shape": ["A **shape** decides *where* a spell goes and *who* it touches: yourself, a bolt that flies, a beam, a burst around you, "
                  "a zone on the ground. Every shape starts a new group in the spell, and the effects after it act on whatever it hits. "
                  "Its mana is added to the spell's cost, and some shapes make the effects after them cost more (or less)."],
        "modifier": ["A **modifier** changes the closest rune on its *left* that it can change. Amplify needs something with power, "
                     "so in `Bolt · Fire · Amplify` it strengthens Fire; Split needs something that can split, so in `Bolt · Fire · Split` "
                     "it skips Fire and doubles the Bolt. A modifier never reaches back past a link. Modifiers multiply the cost of what they change."],
        "link": ["A **link** ends a segment of the spell: everything after it happens *later*, or somewhere else. `On Hit` fires the rest "
                 "where the spell struck, `Delay` fires it from you a moment later, `Echo` repeats everything before it, and the "
                 "conditions (If Sneaking, If Airborne...) let one spell do two different things."],
    }
    for family in ("shape", "modifier", "link"):
        rs = [r for r in runes if r["family"] == family and r["path"] not in special]
        page(out / f"{FAMILY_TITLE[family].lower()}.md",
             {"title": FAMILY_TITLE[family], "parent": "Runes", "nav_order": order[family]},
             [f"# {FAMILY_TITLE[family]}", ""] + intros[family] + ["", f"{len(rs)} {FAMILY_TITLE[family].lower()}, by tier."],
             body_of(rs))

    # Effects: one page per element.
    page(out / "effects/index.md",
         {"title": "Effects", "parent": "Runes", "nav_order": 2, "has_children": "true"},
         ["# Effects", "",
          "An **effect** decides *what happens* to whatever the shape hit: damage, healing, a push, a freeze, a blink. "
          "Every effect belongs to one of ten **elements**, and elements matter: they set the spell's colour and sound, "
          "they set off reactions together, they decide what fuses at the Fusion Altar, and casting one grows your "
          "affinity with its element. Each element has its own page:", ""],
         [f"- [{e.title()}]({{{{ '/runes/effects/{e}/' | relative_url }}}}): {ELEMENT_BLURB[e]}" for e in ELEMENTS])
    for i, element in enumerate(ELEMENTS):
        rs = [r for r in runes if r["family"] == "effect" and r["element"] == element and r["path"] not in special]
        page(out / f"effects/{element}.md",
             {"title": element.title(), "parent": "Effects", "grand_parent": "Runes", "nav_order": i + 1},
             [f"# {element.title()} effects", "", ELEMENT_BLURB[element], "",
              f"{len(rs)} {element} effects you can craft or find in the usual way. {element.title()} also has runes of the world, "
              "fused runes and innate runes: see their own pages."] +
             (["", "Read [Reading Life Magic]({{ '/spellcraft/life-outcomes/' | relative_url }}) for the illustrated journal of actual healing, repair, gardens and living ward responses."] if element == "life" else []),
             body_of(rs))

    # Runes of the world.
    ws = [r for r in runes if r["path"] in world]
    page(out / "world.md", {"title": "Runes of the World", "parent": "Runes", "nav_order": 5},
         ["# Runes of the world", "",
          f"{len(ws)} runes that can't be crafted at all: each is found only in its own places. Some wait in the chests of "
          "vanilla structures, some are carried by bosses and dungeon guards, some fall with stars or come out of rifts, and "
          "some are drawn out of the land itself by **Attunement**: hold a Blank Rune and meditate in the right biome at the "
          f"right moment. See [Runes of the World and Attunement]({{{{ '/world/runes-of-the-world/' | relative_url }}}}) for how "
          "to hunt them."],
         body_of(ws))

    # Fused runes, with the grid.
    fs = [r for r in runes if r["path"] in fused]
    names = {r["path"]: r["name"] for r in runes}
    by_pair = {frozenset(pair): path for path, pair in fused.items()}
    grid = ["| | " + " | ".join(e.title() for e in ELEMENTS) + " |", "|---" * (len(ELEMENTS) + 1) + "|"]
    for a in ELEMENTS:
        grid.append(f"| **{a.title()}** | " + " | ".join(names.get(by_pair.get(frozenset((a, b)), ""), "") for b in ELEMENTS) + " |")
    # The signature fusions: two particular runes each, in their own section with a table of their pairs.
    ss = [r for r in runes if r["path"] in SIGNATURES]
    signature_lines = []
    if ss:
        by_pair = {frozenset(pair): path for path, pair in fused.items()}
        signature_lines = ["## Signature fusions", "",
                           f"{len(ss)} more fused runes, each made from two *particular* effects rather than any two of their elements. "
                           "The altar asks for a signature first, so the pair below makes its own rune **in place of** its elements' "
                           "fusion, while any other effects of those two elements still make that one. They take modifiers, ranks and "
                           f"further fusions like every fused rune. See [Combining]({{{{ '/fusion-altar/combining/' | relative_url }}}}#signature-fusions).",
                           "", "| | Put in | Makes | Counts as | In place of |", "|---|---|---|---|---|"]
        for r in ss:
            a, b = SIGNATURES[r["path"]]
            over = by_pair.get(frozenset((BY_PATH[a]["element"], BY_PATH[b]["element"])))
            signature_lines.append(f"| {img(r['path'], 24)} | {rune_link(a, world)} + {rune_link(b, world)} | [{r['name']}](#{r['path']}) "
                                   f"| {r['element'].title()} | {names[over] if over else ''} |")
        signature_lines += [""] + body_of(ss)
    page(out / "fused.md", {"title": "Fused Runes", "parent": "Runes", "nav_order": 6},
         ["# Fused runes", "",
          f"{len(fs)} runes made only at the [Fusion Altar]({{{{ '/fusion-altar/' | relative_url }}}}): one for every pair of the ten "
          "elements, and one for each element with itself. Put any effect of one element and any effect of the other in "
          "the altar with an amethyst shard. Read the grid by row and column: Fire with Wind makes Firestorm."
          + (f" A few particular pairs of effects make [signature fusions](#signature-fusions) instead ({len(ss)} more)." if ss else ""), ""] + grid,
         (["## Element fusions", ""] if ss else []) + body_of(fs) + signature_lines)

    # Innate runes.
    ins = [r for r in runes if r["path"] in g.INNATE]
    page(out / "innate.md", {"title": "Innate Runes", "parent": "Runes", "nav_order": 7},
         ["# Innate runes", "",
          f"{len(ins)} runes that nobody can craft, find or trade. When you form your 1st Heart Circle, one of them wakes in "
          "your heart, chosen at random, and it's yours alone. It grows stronger with every Heart Circle you form. "
          "Your Grimoire shows which one you have."],
         body_of(ins))

    # Every rune recipe, by tier.
    rec = []
    for tier in (1, 2, 3):
        rs = [r for r in runes if r["tier"] == tier and r["path"] not in special]
        extra = "nothing else" if tier == 1 else TIER_EXTRAS[tier]
        rec += [f"## Tier {TIER_NAMES[tier]} ({len(rs)} runes)", "",
                f"Each needs a Blank Rune and its own items, plus **{extra}**.", "",
                '<div class="recipe-gallery">']
        for r in sorted(rs, key=lambda r: (r["family"], r["name"])):
            page_url = f"/runes/effects/{r['element']}/" if r["family"] == "effect" else f"/runes/{FAMILY_TITLE[r['family']].lower()}/"
            family = r["family"].title() + (f", {r['element'].title()}" if r["element"] else "")
            rec += ['<figure class="recipe-card">',
                    recipe_img(f"rune_{r['path']}", f"Crafting {r['name']}: a Blank Rune and {items_text(r['path'])}"),
                    f'<figcaption>{img(r["path"], 24)} <a href="' + "{{ '" + page_url + "' | relative_url }}" + f'#{r["path"]}">{r["name"]}</a>'
                    f'<br><span class="recipe-family">{family}</span></figcaption>',
                    '</figure>']
        rec += ["</div>", ""]
    page(WIKI / "items/rune-recipes.md", {"title": "Rune Recipes", "parent": "Items and Crafting", "nav_order": 2},
         ["# Rune recipes", "",
          "Craftable runes up to Tier III use a **Blank Rune** plus a few items that suit them, in any crafting grid "
          "(the recipes are shapeless, so the layout doesn't matter). Higher tiers cost a little extra. Recipes show up in "
          "the recipe book once you've held a Blank Rune. Tier IV runes, runes of the world, fused runes and innate runes "
          "can't be crafted.", "",
          "**Blank Rune:** 4 Cobblestone around 1 Lapis Lazuli, makes 4."],
         rec)
    print(f"{len(runes)} runes written to the wiki")


if __name__ == "__main__":
    main()
