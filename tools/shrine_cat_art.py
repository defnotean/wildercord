"""Shrine Cats (Yuki, Odie, Freya, Kuro), Sakura Villagers, Karakuri Sentinel, Paper Lantern Spirit,
and Suzu Cat-Bell Charm skins, icons, spawn eggs, loot tables, recipes, and text."""
from __future__ import annotations

import random
from pathlib import Path
from PIL import Image, ImageDraw

from bobcat_art import egg_icon

LANG = {
    "entity.wildercord.shrine_cat": "Shrine Cat",
    "item.wildercord.shrine_cat_spawn_egg": "Shrine Cat Spawn Egg",
    "guide.wildercord.shrine_cat.hint": "Four guardian cats — Yuki, Odie, Freya, and Kuro — presiding over the four tiers of the Cat Sanctuary and strolling the Sakura Village.",
    "guide.wildercord.shrine_cat": "Four unique cats named Yuki, Odie, Freya, and Kuro. Each guards a floor of the Cat Sanctuary with a gentle trial, grants a shrine buff upon yielding, and may be tamed once Kuro's fourth-floor trial is complete.",
    "entity.wildercord.sakura_villager": "Sakura Villager",
    "item.wildercord.sakura_villager_spawn_egg": "Sakura Villager Spawn Egg",
    "guide.wildercord.sakura_villager.hint": "Kimono-clad Tea Masters and Shrine Maidens tending cherry-blossom villages.",
    "guide.wildercord.sakura_villager": "Peaceful residents of Japanese Sakura Villages who bow to visitors and trade Suzu Cat-Bell Charms and cooked salmon for emeralds.",
    "entity.wildercord.karakuri_sentinel": "Karakuri Sentinel",
    "item.wildercord.karakuri_sentinel_spawn_egg": "Karakuri Sentinel Spawn Egg",
    "guide.wildercord.karakuri_sentinel.hint": "Lacquered clockwork automata patrolling pagoda halls and cedar bathhouses.",
    "guide.wildercord.karakuri_sentinel": "Clockwork wooden-and-brass puppets that chime as their chest gears spin up before delivering a measured sweeping blow.",
    "entity.wildercord.paper_lantern_spirit": "Paper Lantern Spirit",
    "item.wildercord.paper_lantern_spirit_spawn_egg": "Paper Lantern Spirit Spawn Egg",
    "guide.wildercord.paper_lantern_spirit.hint": "Drifting crimson-and-gold paper lanterns hovering through bamboo courts and bathhouses.",
    "guide.wildercord.paper_lantern_spirit": "Floating chochin lantern spirits that swell with warm candlelight before pulsing an ember ring.",
    "item.wildercord.cat_bell_charm": "Suzu Cat-Bell Charm",
}


def _face(draw: ImageDraw.ImageDraw, rng: random.Random, rect, base, fur=True, light=0):
    x0, y0, x1, y1 = rect
    if x1 <= x0 or y1 <= y0:
        return
    r, g, b = base
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=(max(0, min(255, r + light)), max(0, min(255, g + light)), max(0, min(255, b + light)), 255))
    if fur:
        for _ in range(max(3, (x1 - x0) * (y1 - y0) // 2)):
            x = rng.randrange(x0, x1)
            y = rng.randrange(y0, y1)
            delta = rng.choice([-22, -14, -7, 7, 14, 22])
            c = tuple(max(0, min(255, ch + light + delta)) for ch in base) + (255,)
            draw.point((x, y), fill=c)


def _cube(draw: ImageDraw.ImageDraw, rng: random.Random, u, v, w, h, d, color, fur=True):
    _face(draw, rng, (u + d, v, u + d + w, v + d), color, fur=fur, light=10)
    _face(draw, rng, (u + d + w, v, u + d + 2 * w, v + d), color, fur=fur, light=-16)
    _face(draw, rng, (u, v + d, u + d, v + d + h), color, fur=fur, light=-10)
    _face(draw, rng, (u + d, v + d, u + d + w, v + d + h), color, fur=fur, light=6)
    _face(draw, rng, (u + d + w, v + d, u + 2 * d + w, v + d + h), color, fur=fur, light=-8)
    _face(draw, rng, (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h), color, fur=fur, light=-20)


def _cat_skin(variant: str) -> Image.Image:
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(int.from_bytes(variant.encode("utf-8"), "big"))

    if variant == "yuki":
        coat, dark, bib, eye, nose = (118, 114, 108), (54, 51, 48), (242, 238, 232), (166, 212, 138), (44, 34, 34)
    elif variant == "odie":
        coat, dark, bib, eye, nose = (218, 144, 72), (176, 98, 42), (244, 204, 148), (74, 42, 22), (232, 136, 144)
    elif variant == "freya":
        coat, dark, bib, eye, nose = (122, 128, 138), (94, 98, 110), (146, 152, 164), (176, 206, 86), (68, 66, 74)
    else:  # kuro
        coat, dark, bib, eye, nose = (28, 28, 32), (16, 16, 19), (40, 40, 46), (244, 198, 44), (20, 18, 22)

    _cube(draw, rng, 0, 0, 5, 5, 10, coat)
    _cube(draw, rng, 0, 16, 6, 5, 9, coat)
    _cube(draw, rng, 32, 8, 5, 4, 2, bib if variant in ("yuki", "odie") else coat)
    # Collar & bell (signature pink collar + silver bell on Freya).
    _cube(draw, rng, 32, 0, 5, 3, 2, (230, 146, 164), fur=False)
    _cube(draw, rng, 48, 0, 2, 2, 2, (218, 224, 234), fur=False)
    draw.point((50, 3), fill=(110, 114, 124, 255))

    _cube(draw, rng, 0, 32, 6, 5, 5, coat)
    _cube(draw, rng, 24, 32, 3, 2, 2, bib if variant == "yuki" else coat)
    _cube(draw, rng, 36, 32, 1, 3, 3, bib if variant in ("yuki", "odie") else coat)
    _cube(draw, rng, 24, 38, 2, 2, 1, coat)
    _cube(draw, rng, 32, 38, 1, 2, 1, dark)
    _cube(draw, rng, 0, 46, 2, 6, 2, coat)
    _cube(draw, rng, 12, 46, 2, 2, 5, coat)
    _cube(draw, rng, 28, 46, 2, 2, 4, dark)
    _cube(draw, rng, 12, 54, 3, 3, 5, coat)
    _cube(draw, rng, 28, 54, 3, 3, 4, dark)

    # Mackerel tabby stripes on Yuki & Odie.
    if variant in ("yuki", "odie"):
        for z in (12, 14, 16, 18):
            draw.line((10, z, 14, z), fill=dark + (255,))
        # Forehead M on front of head (5..10, 37..41).
        for x in (6, 9):
            draw.point((x, 37), fill=dark + (255,))
        draw.point((7, 38), fill=dark + (255,))
        draw.point((8, 38), fill=dark + (255,))
        # Delicate white whiskers on whisker panel (46..53, 32..35).
        for y in (33, 34):
            draw.line((47, y, 49, y), fill=(248, 248, 248, 255))

    # Eyes on head front face (5..10, 37..41) & nose on muzzle front (26..28, 34..35).
    if variant == "odie":
        # Odie's signature sleepy squinted contented eyes.
        draw.line((6, 39, 7, 39), fill=eye + (255,))
        draw.line((8, 39, 9, 39), fill=eye + (255,))
    else:
        draw.rectangle((6, 38, 6, 39), fill=eye + (255,))
        draw.rectangle((9, 38, 9, 39), fill=eye + (255,))
        draw.point((6, 39), fill=(18, 18, 22, 255))
        draw.point((9, 39), fill=(18, 18, 22, 255))
    draw.point((27, 34), fill=nose + (255,))
    if variant == "yuki":
        draw.line((26, 35, 28, 35), fill=(246, 244, 240, 255))
    return im


def _villager_skin(maiden: bool) -> Image.Image:
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(2026 if maiden else 1013)
    skin = (242, 206, 178)
    hair = (38, 30, 34)
    robe = (242, 240, 238) if maiden else (88, 122, 92)
    hakama = (188, 48, 56) if maiden else (64, 86, 68)
    obi = (218, 176, 72) if maiden else (228, 212, 168)

    _cube(draw, rng, 0, 0, 8, 8, 8, hair, fur=False)
    _face(draw, rng, (8, 8, 16, 16), skin, fur=False, light=4)
    draw.line((8, 8, 15, 9), fill=hair + (255,))
    draw.point((10, 11), fill=(40, 36, 42, 255))
    draw.point((13, 11), fill=(40, 36, 42, 255))

    _cube(draw, rng, 0, 16, 8, 12, 4, robe, fur=False)
    _cube(draw, rng, 36, 0, 8, 4, 4, obi, fur=False)
    _cube(draw, rng, 36, 16, 4, 10, 4, robe, fur=False)
    _cube(draw, rng, 56, 16, 4, 12, 4, hakama, fur=False)
    _cube(draw, rng, 0, 40, 12, 2, 12, (206, 172, 106), fur=True)
    _cube(draw, rng, 50, 40, 6, 2, 6, (186, 150, 86), fur=True)
    _cube(draw, rng, 80, 0, 3, 3, 2, (244, 158, 182), fur=False)
    return im


def _karakuri_skin() -> Image.Image:
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(7731)
    wood, brass, lacq = (118, 62, 40), (218, 174, 62), (38, 28, 28)
    _cube(draw, rng, 0, 0, 8, 10, 6, wood, fur=False)
    _cube(draw, rng, 30, 0, 6, 6, 1, brass, fur=False)
    _cube(draw, rng, 0, 18, 8, 8, 8, lacq, fur=False)
    draw.point((10, 29), fill=(255, 206, 80, 255))
    draw.point((13, 29), fill=(255, 206, 80, 255))
    _cube(draw, rng, 30, 8, 10, 2, 2, brass, fur=False)
    _cube(draw, rng, 34, 18, 4, 10, 4, wood, fur=False)
    _cube(draw, rng, 0, 38, 4, 10, 4, lacq, fur=False)
    return im


def _lantern_spirit_skin() -> Image.Image:
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(8842)
    cedar = (62, 42, 34)
    paper = (238, 184, 108)
    rib = (196, 64, 52)
    _cube(draw, rng, 0, 0, 8, 2, 8, cedar, fur=False)
    _cube(draw, rng, 0, 14, 10, 12, 10, paper, fur=False)
    for y in (26, 29, 32, 35):
        draw.line((10, y, 19, y), fill=rib + (255,))
    draw.rectangle((12, 28, 13, 30), fill=(255, 248, 200, 255))
    draw.rectangle((16, 28, 17, 30), fill=(255, 248, 200, 255))
    _cube(draw, rng, 44, 14, 2, 6, 2, (236, 188, 62), fur=False)
    _cube(draw, rng, 0, 40, 8, 2, 8, cedar, fur=False)
    _cube(draw, rng, 34, 40, 2, 8, 4, (242, 236, 224), fur=False)
    return im


def _cat_bell_charm_icon() -> Image.Image:
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    # Pink ribbon loop + gleaming silver suzu bell charm.
    draw.arc((4, 1, 11, 7), 180, 360, fill=(236, 134, 164, 255))
    draw.line((6, 5, 9, 5), fill=(214, 96, 134, 255))
    draw.ellipse((4, 6, 11, 13), fill=(210, 218, 228, 255), outline=(126, 136, 152, 255))
    draw.point((6, 8), fill=(250, 252, 255, 255))
    draw.line((6, 11, 9, 11), fill=(72, 78, 92, 255))
    draw.point((7, 10), fill=(72, 78, 92, 255))
    draw.point((8, 10), fill=(72, 78, 92, 255))
    return im


def write(g):
    tex = g.ASSETS / "textures"
    for name in ("yuki", "odie", "freya", "kuro"):
        g.save(_cat_skin(name), tex / f"entity/shrine_cat_{name}.png")
    g.save(_villager_skin(False), tex / "entity/sakura_villager_tea.png")
    g.save(_villager_skin(True), tex / "entity/sakura_villager_shrine.png")
    g.save(_karakuri_skin(), tex / "entity/karakuri_sentinel.png")
    g.save(_lantern_spirit_skin(), tex / "entity/paper_lantern_spirit.png")

    g.save(_cat_bell_charm_icon(), tex / "item/cat_bell_charm.png")
    g.item_model("cat_bell_charm", "cat_bell_charm")
    g.write_json(g.ASSETS / "items/cat_bell_charm.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/cat_bell_charm"}})
    g.write_json(g.DATA / "recipe/cat_bell_charm.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": [" S ", "IAI", " N "],
        "key": {
            "S": "minecraft:string",
            "I": "minecraft:iron_ingot",
            "A": "minecraft:amethyst_shard",
            "N": "minecraft:pink_dye",
        },
        "result": {"id": "wildercord:cat_bell_charm", "count": 1},
    })

    eggs = {
        "shrine_cat_spawn_egg": (("#8D8782", "#D9924A", "#F0ECE6"), "#E67CA0", "shrine_cat"),
        "sakura_villager_spawn_egg": (("#F2C4D0", "#587A5C", "#F6E8EC"), "#D94452", "sakura_villager"),
        "karakuri_sentinel_spawn_egg": (("#6B3A26", "#9E5C38", "#DAAE3E"), "#FFE066", "karakuri_sentinel"),
        "paper_lantern_spirit_spawn_egg": (("#B83228", "#EEB86C", "#FFF0BE"), "#FF6B3D", "paper_lantern_spirit"),
    }
    for egg_name, (shell, spots, label) in eggs.items():
        g.save(egg_icon(shell, spots, name=label), tex / f"item/{egg_name}.png")
        g.write_json(g.ASSETS / f"models/item/{egg_name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{egg_name}"}})
        g.write_json(g.ASSETS / f"items/{egg_name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg_name}"}})

    for mob_id, drop in (("karakuri_sentinel", "minecraft:gold_nugget"), ("paper_lantern_spirit", "minecraft:lantern")):
        g.write_json(g.DATA / f"loot_table/entities/{mob_id}.json", {
            "type": "minecraft:entity",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": drop}]}],
            "random_sequence": f"wildercord:entities/{mob_id}",
        })
