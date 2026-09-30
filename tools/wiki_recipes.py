"""The wiki's visual recipes: every crafting and brewing recipe in the mod, drawn the way the game draws them
(a crafting grid with an arrow to the result, or a brewing stand's potion, ingredient and result), from the
recipe files and the game's own textures. Used by tools/wiki.py, which writes them to wiki/assets/recipes/.

Vanilla textures come from the Minecraft jar Loom downloads; blocks are drawn as the little cubes the inventory
shows, items as their flat sprites.
"""
import json
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "src/main/resources/data/wildercord/recipe"
MOD_TEXTURES = ROOT / "src/main/resources/assets/wildercord/textures"
S = 3  # every game pixel is drawn this big
ICON = 16 * S

# The game's GUI colours.
PANEL = (198, 198, 198, 255)
PANEL_LIGHT = (255, 255, 255, 255)
PANEL_DARK = (85, 85, 85, 255)
EDGE = (0, 0, 0, 255)
SLOT = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_LIGHT = (255, 255, 255, 255)

# A tag in a recipe is shown as one of its items.
TAG_ITEMS = {"#minecraft:logs": "minecraft:oak_log", "#minecraft:saplings": "minecraft:oak_sapling",
             "#minecraft:wool": "minecraft:white_wool", "#minecraft:planks": "minecraft:oak_planks",
             "#minecraft:wooden_slabs": "minecraft:oak_slab"}
# Items whose inventory sprite isn't item/<name> (animated, or drawn from a block or entity texture).
SPRITES = {"minecraft:clock": "item/clock_00", "minecraft:compass": "item/compass_00", "minecraft:crossbow": "item/crossbow_standby",
           "minecraft:glass_pane": "block/glass", "minecraft:sunflower": "block/sunflower_front"}
# Slabs: a half-height cube of this block's texture.
SLABS = {"minecraft:oak_slab": "block/oak_planks"}
POTION_COLOURS = {"minecraft:awkward": 0x385DC6, "minecraft:water": 0x385DC6, "wildercord:clarity": 0xB8A8FF,
                  "wildercord:mana": 0x7C5CFF, "wildercord:long_clarity": 0xB8A8FF, "wildercord:strong_clarity": 0xB8A8FF,
                  "wildercord:strong_mana": 0x7C5CFF}

# A 3x5 font for stack counts.
DIGITS = {
    "0": ["###", "#.#", "#.#", "#.#", "###"], "1": [".#.", "##.", ".#.", ".#.", "###"],
    "2": ["###", "..#", "###", "#..", "###"], "3": ["###", "..#", ".##", "..#", "###"],
    "4": ["#.#", "#.#", "###", "..#", "..#"], "5": ["###", "#..", "###", "..#", "###"],
    "6": ["###", "#..", "###", "#.#", "###"], "7": ["###", "..#", ".#.", ".#.", ".#."],
    "8": ["###", "#.#", "###", "#.#", "###"], "9": ["###", "#.#", "###", "..#", "###"],
}


def _jar():
    jars = sorted((Path.home() / ".gradle/caches/fabric-loom/minecraftMaven/net/minecraft").glob(
        "minecraft-clientonly-deobf/*/minecraft-clientonly-deobf-*.jar"))
    if not jars:
        raise SystemExit("Minecraft's client jar isn't in the Loom cache: run ./gradlew build once first.")
    return zipfile.ZipFile(jars[-1])


JAR = _jar()
NAMES = set(JAR.namelist())


def _first_frame(img):
    img = img.convert("RGBA")
    return img.crop((0, 0, img.width, img.width)) if img.height > img.width else img


def _vanilla(path):
    """A vanilla texture ('block/cobblestone'), or None."""
    name = f"assets/minecraft/textures/{path}.png"
    if name not in NAMES:
        return None
    with JAR.open(name) as f:
        return _first_frame(Image.open(f)).resize((16, 16), Image.NEAREST)


def _mod(path):
    f = MOD_TEXTURES / f"{path}.png"
    return _first_frame(Image.open(f)).resize((16, 16), Image.NEAREST) if f.exists() else None


def _texture(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    return _vanilla(path) if ns == "minecraft" else _mod(path)


def _block_model(ns, name):
    """A block model's resolved textures and whether it's a full cube, following its parents."""
    textures, parents = {}, []
    ref = f"{ns}:block/{name}"
    for _ in range(8):
        ns2, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
        if ns2 == "minecraft":
            fname = f"assets/minecraft/models/{path}.json"
            if fname not in NAMES:
                break
            model = json.loads(JAR.read(fname).decode("utf-8"))
        else:
            f = ROOT / f"src/main/resources/assets/{ns2}/models/{path}.json"
            if not f.exists():
                break
            model = json.loads(f.read_text(encoding="utf-8"))
        for k, v in model.get("textures", {}).items():
            textures.setdefault(k, v)
        parents.append(path)
        if "parent" not in model:
            break
        ref = model["parent"]
    for _ in range(4):
        for k, v in list(textures.items()):
            if isinstance(v, str) and v.startswith("#"):
                textures[k] = textures.get(v[1:], v)
    cube = any(p.split("/")[-1] in ("cube", "cube_all", "cube_column", "cube_bottom_top", "orientable", "cube_column_horizontal",
                                   "orientable_with_bottom", "cube_directional", "cube_mirrored_all", "leaves", "cube_top")
               for p in parents)
    # Our own blocks are all full cubes in the inventory.
    return textures, cube or (ns != "minecraft" and bool(textures))


def _shade(img, f):
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * f), int(g * f), int(b * f), a)
    return img


def _shield():
    """The shield's front face, from its entity texture, standing in the middle of the slot."""
    with JAR.open("assets/minecraft/textures/entity/shield/shield_base_nopattern.png") as f:
        tex = Image.open(f).convert("RGBA")
    scale = tex.width // 64
    face = tex.crop((1 * scale, 1 * scale, 13 * scale, 23 * scale)).resize((8, 15), Image.NEAREST)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    img.alpha_composite(face, (4, 0))
    return _flat(img)


def _chest():
    """A chest as the inventory draws it, from its entity texture: the lid's top, and its front and side (lid over base)."""
    with JAR.open("assets/minecraft/textures/entity/chest/normal.png") as f:
        tex = Image.open(f).convert("RGBA")
    k = tex.width // 64

    def face(u):
        img = Image.new("RGBA", (14, 15), (0, 0, 0, 0))
        img.alpha_composite(tex.crop((u * k, 14 * k, (u + 14) * k, 19 * k)).resize((14, 5), Image.NEAREST), (0, 0))
        img.alpha_composite(tex.crop((u * k, 33 * k, (u + 14) * k, 43 * k)).resize((14, 10), Image.NEAREST), (0, 5))
        return img.resize((16, 16), Image.NEAREST)

    top = tex.crop((28 * k, 0, 42 * k, 14 * k)).resize((16, 16), Image.NEAREST)
    front = face(42)
    # The latch, over the seam between lid and base.
    front.alpha_composite(tex.crop((1 * k, 1 * k, 3 * k, 5 * k)).resize((2, 4), Image.NEAREST), (7, 3))
    return _cube(top, front, face(0))


def _dyed_layers(name):
    """A mod item drawn in two layers with its first tinted, as leather is (a backpack): None if it isn't one."""
    item_file = ROOT / f"src/main/resources/assets/wildercord/items/{name}.json"
    model_file = ROOT / f"src/main/resources/assets/wildercord/models/item/{name}.json"
    if not item_file.exists() or not model_file.exists():
        return None
    tints = json.loads(item_file.read_text(encoding="utf-8"))["model"].get("tints", [])
    layers = json.loads(model_file.read_text(encoding="utf-8")).get("textures", {})
    if not tints or tints[0].get("type") != "minecraft:dye" or "layer1" not in layers:
        return None
    colour = tints[0]["default"] & 0xFFFFFF
    base = _texture(layers["layer0"]).copy()
    px = base.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = (r * (colour >> 16) // 255, g * ((colour >> 8) & 255) // 255, b * (colour & 255) // 255, a)
    base.alpha_composite(_texture(layers["layer1"]))
    return _flat(base)


def _cube(top, left, right, half=False):
    """A block as the inventory draws it: a little cube seen from above, its sides shaded (a slab: half as tall)."""
    out = Image.new("RGBA", (ICON, ICON), (0, 0, 0, 0))
    o = out.load()
    t, l, r = top.load(), _shade(left.copy(), 0.8).load(), _shade(right.copy(), 0.62).load()
    h = ICON / 2  # half width
    q = ICON / 4  # a quarter: the top face's half height
    drop = h / 2 if half else 0  # a slab's top sits half a block lower
    for y in range(ICON):
        for x in range(ICON):
            fx, fy = x + 0.5, y + 0.5 - drop
            if half and y + 0.5 < drop:
                continue
            # Top face: (h,0) + a*(h,q) + b*(-h,q).
            a = ((fx - h) / h + fy / q) / 2
            b = (fy / q - (fx - h) / h) / 2
            if 0 <= a < 1 and 0 <= b < 1:
                o[x, y] = t[int(a * 16), int(b * 16)]
                continue
            limit = 0.5 if half else 1.0
            if fx < h:
                u = fx / h
                v = (fy - q - fx / 2) / h
                if 0 <= u < 1 and 0 <= v < limit:
                    o[x, y] = l[int(u * 16), int((v + 1 - limit) * 16)]
            else:
                u = (fx - h) / h
                v = (fy - 2 * q + (fx - h) / 2) / h
                if 0 <= u < 1 and 0 <= v < limit:
                    o[x, y] = r[int(u * 16), int((v + 1 - limit) * 16)]
    return out


def _flat(img):
    return img.resize((ICON, ICON), Image.NEAREST)


def _potion(kind, potion):
    base = _vanilla(f"item/{kind}")
    overlay = _vanilla("item/potion_overlay")
    colour = POTION_COLOURS.get(potion, 0xF800F8)
    tint = Image.new("RGBA", (16, 16), ((colour >> 16) & 255, (colour >> 8) & 255, colour & 255, 255))
    over = Image.composite(tint, Image.new("RGBA", (16, 16), (0, 0, 0, 0)), overlay.split()[3])
    over.putalpha(overlay.split()[3])
    # Multiply the overlay's greys by the colour, as the game tints it.
    op, cp = overlay.load(), over.load()
    for y in range(16):
        for x in range(16):
            gr, gg, gb, ga = op[x, y]
            if ga:
                cp[x, y] = (gr * ((colour >> 16) & 255) // 255, gg * ((colour >> 8) & 255) // 255, gb * (colour & 255) // 255, ga)
    img = base.copy()
    img.alpha_composite(over)
    return _flat(img)


_ICONS = {}


def icon(item, rune=None, potion=None, kind="potion"):
    """An item's icon at the guide's scale: a rune's own icon, a block's cube, an item's sprite."""
    key = (item, rune, potion, kind)
    if key in _ICONS:
        return _ICONS[key]
    item = TAG_ITEMS.get(item, item)
    ns, name = item.split(":", 1)
    img = None
    if item == "wildercord:rune" and rune:
        img = _flat(_mod(f"item/rune/{rune.split(':', 1)[1]}"))
    elif item == "minecraft:potion" or item.endswith("_potion"):
        img = _potion(name if name != "potion" else kind, potion)
    elif item in SPRITES:
        img = _flat(_vanilla(SPRITES[item]))
    elif item in SLABS:
        face = _vanilla(SLABS[item])
        img = _cube(face, face, face, half=True)
    elif item == "minecraft:shield":
        img = _shield()
    elif item == "minecraft:chest":
        img = _chest()
    elif ns == "wildercord" and _dyed_layers(name) is not None:
        img = _dyed_layers(name)
    else:
        sprite = _vanilla(f"item/{name}") if ns == "minecraft" else _mod(f"item/{name}")
        if sprite is not None:
            img = _flat(sprite)
        else:
            textures, cube = _block_model(ns, name)
            pick = lambda *keys: next((_texture(textures[k]) for k in keys if k in textures and _texture(textures[k]) is not None), None)
            top = pick("up", "top", "end", "all", "particle")
            left = pick("north", "front", "side", "sides", "all", "particle")
            right = pick("east", "side", "sides", "all", "particle")
            if cube and top is not None and left is not None and right is not None:
                img = _cube(top, left, right)
            else:
                flat = pick("particle", "all", "side", "texture", "cross") or _vanilla(f"block/{name}")
                img = _flat(flat) if flat is not None else None
    if img is None:
        img = Image.new("RGBA", (ICON, ICON), (248, 0, 248, 255))
        print("  no texture for", item)
    _ICONS[key] = img
    return img


def _bevel(d, x0, y0, x1, y1, fill, light, dark):
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=fill)
    d.rectangle([x0, y0, x1 - 1, y0 + S - 1], fill=dark)
    d.rectangle([x0, y0, x0 + S - 1, y1 - 1], fill=dark)
    d.rectangle([x0, y1 - S, x1 - 1, y1 - 1], fill=light)
    d.rectangle([x1 - S, y0, x1 - 1, y1 - 1], fill=light)


def _panel(w, h):
    img = Image.new("RGBA", (w * S, h * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([S, 0, w * S - S - 1, h * S - 1], fill=EDGE)
    d.rectangle([0, S, w * S - 1, h * S - S - 1], fill=EDGE)
    d.rectangle([S, S, w * S - S - 1, h * S - S - 1], fill=PANEL)
    d.rectangle([S, S, w * S - 2 * S - 1, 2 * S - 1], fill=PANEL_LIGHT)
    d.rectangle([S, S, 2 * S - 1, h * S - 2 * S - 1], fill=PANEL_LIGHT)
    d.rectangle([2 * S, h * S - 2 * S, w * S - S - 1, h * S - S - 1], fill=PANEL_DARK)
    d.rectangle([w * S - 2 * S, 2 * S, w * S - S - 1, h * S - S - 1], fill=PANEL_DARK)
    return img, d


def _slot(img, d, x, y, size=18):
    _bevel(d, x * S, y * S, (x + size) * S, (y + size) * S, SLOT, SLOT_LIGHT, SLOT_DARK)


def _put(img, picture, x, y, size=18):
    off = (size - 16) // 2
    img.alpha_composite(picture, ((x + off) * S, (y + off) * S))


def _count(img, n, x, y, size=18):
    """A stack count in the slot's corner, white with a dark shadow, as the game prints it."""
    text = str(n)
    d = ImageDraw.Draw(img)
    width = len(text) * 4 - 1
    left = x + size - 1 - width
    top = y + size - 1 - 5
    for shadow, colour in ((1, (63, 63, 63, 255)), (0, (255, 255, 255, 255))):
        cx = left
        for ch in text:
            for j, row in enumerate(DIGITS[ch]):
                for i, c in enumerate(row):
                    if c == "#":
                        px, py = (cx + i + shadow) * S, (top + j + shadow) * S
                        d.rectangle([px, py, px + S - 1, py + S - 1], fill=colour)
            cx += 4


def _arrow(d, x, y, down=False):
    """The grey arrow between ingredients and result."""
    colour = SLOT
    if down:
        d.rectangle([(x + 4) * S, y * S, (x + 11) * S - 1, (y + 8) * S - 1], fill=colour)
        d.polygon([(x * S, (y + 8) * S), ((x + 15) * S, (y + 8) * S), ((x + 7.5) * S, (y + 15) * S)], fill=colour)
    else:
        d.rectangle([x * S, (y + 4) * S, (x + 14) * S - 1, (y + 11) * S - 1], fill=colour)
        d.polygon([((x + 14) * S, y * S), ((x + 14) * S, (y + 15) * S), ((x + 22) * S, (y + 7.5) * S)], fill=colour)


def _ingredient(value):
    """A recipe ingredient (a string, a tag, or a list of choices): the item to show."""
    if isinstance(value, list):
        value = value[0]
    if isinstance(value, dict):
        value = value.get("item") or ("#" + value["tag"] if "tag" in value else None)
    return value


def crafting(recipe):
    """A crafting recipe drawn as the crafting table draws it."""
    cells = [[None] * 3 for _ in range(3)]
    if recipe["type"] == "minecraft:crafting_shaped":
        key = recipe["key"]
        for y, row in enumerate(recipe["pattern"]):
            for x, ch in enumerate(row):
                if ch != " ":
                    cells[y][x] = _ingredient(key[ch])
    else:
        for i, value in enumerate(recipe["ingredients"]):
            cells[i // 3][i % 3] = _ingredient(value)
    result = recipe["result"]
    rune = result.get("components", {}).get("wildercord:rune")
    w, h = 7 + 54 + 7 + 22 + 7 + 26 + 7, 7 + 54 + 7
    img, d = _panel(w, h)
    for y in range(3):
        for x in range(3):
            _slot(img, d, 7 + x * 18, 7 + y * 18)
            if cells[y][x]:
                _put(img, icon(cells[y][x]), 7 + x * 18, 7 + y * 18)
    _arrow(d, 7 + 54 + 7, 7 + 18 + 1)
    rx, ry = 7 + 54 + 7 + 22 + 7, 7 + 14
    _slot(img, d, rx, ry, 26)
    _put(img, icon(result["id"], rune=rune), rx, ry, 26)
    if result.get("count", 1) > 1:
        _count(img, result["count"], rx, ry, 26)
    return img


def brewing(recipe):
    """A brewing recipe: the potion in, the ingredient above, the potion out."""
    def potion(entry):
        contents = entry.get("potion_contents") or entry.get("components", {}).get("minecraft:potion_contents", {})
        name = contents.get("potions") or contents.get("potion")
        return icon(entry.get("item") or entry.get("id"), potion=name)
    w, h = 7 + 18 + 6 + 22 + 6 + 18 + 7, 7 + 18 + 4 + 15 + 4 + 18 + 7
    img, d = _panel(w, h)
    top = 7
    mid_y = top + 18 + 4 + 15 + 4
    reagent_x = 7 + 18 + 6 + 2
    _slot(img, d, reagent_x, top)
    _put(img, icon(_ingredient(recipe["reagent"])), reagent_x, top)
    _arrow(d, reagent_x + 1, top + 18 + 2, down=True)
    _slot(img, d, 7, mid_y)
    _put(img, potion(recipe["input"]), 7, mid_y)
    _arrow(d, 7 + 18 + 6, mid_y + 1)
    _slot(img, d, 7 + 18 + 6 + 22 + 6, mid_y)
    _put(img, potion(recipe["output"]), 7 + 18 + 6 + 22 + 6, mid_y)
    return img


def render(out_dir):
    """Draws every recipe into {@code out_dir}: crafting as <recipe>.png, brewing as brewing_<recipe>.png."""
    out_dir.mkdir(parents=True, exist_ok=True)
    made = {}
    for f in sorted(RECIPES.glob("*.json")):
        recipe = json.loads(f.read_text(encoding="utf-8"))
        if recipe["type"] in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless", "wildercord:upgrade"):
            crafting(recipe).save(out_dir / f"{f.stem}.png")
            made[f.stem] = recipe
    for f in sorted((RECIPES / "brewing").glob("*.json")):
        recipe = json.loads(f.read_text(encoding="utf-8"))
        brewing(recipe).save(out_dir / f"brewing_{f.stem}.png")
        made["brewing_" + f.stem] = recipe
    return made


if __name__ == "__main__":
    print(len(render(ROOT / "build/recipe-preview")), "recipes drawn to build/recipe-preview")
