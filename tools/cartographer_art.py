"""Procedural textures, block/item models, and text for The Glass Cartographer and Survey Astrolabe."""
from PIL import Image

LANG = {
    "entity.wildercord.glass_cartographer": "The Glass Cartographer",
    "block.wildercord.survey_astrolabe": "Survey Astrolabe",
    "item.wildercord.survey_astrolabe": "Survey Astrolabe",
    "item.wildercord.high_meridian_compass": "Compass of the High Meridian",
    "item.wildercord.high_meridian_compass.lore": "A brass surveyor's compass calibrated at a Highland Survey Ring. Its needle holds the meridian of that stone court.",
    "item.wildercord.high_meridian_compass.use": "Points back to the Highland Survey Ring where it was earned, without needing a lodestone.",
    "guide.wildercord.glass_cartographer": "An old Marchkeeper survey construct of weathered brass, slate and refracted prism glass anchored at a Highland Survey Ring.",
    "guide.wildercord.glass_cartographer.hint": "Found at Highland Survey Rings in the Windscar Highlands. Step out of its locked transit arc and plumb circle, and cross inside the unlit meridian lane.",
    "journal.wildercord.entry.learned.glass_cartographer": "Completed the High Meridian survey against the Glass Cartographer and earned a compass bound to that stone ring.",
    "boss.wildercord.cartographer.status": "%s \u2014 %s",
    "boss.wildercord.cartographer.idle": "Levelled",
    "boss.wildercord.cartographer.sweep": "Transit sightline locked",
    "boss.wildercord.cartographer.plumb": "Plumb crosshair marked",
    "boss.wildercord.cartographer.grid": "Stand in the unlit meridian lane",
    "message.wildercord.cartographer.decorative": "This astrolabe has no survey ring to sight.",
    "message.wildercord.cartographer.finished": "The meridian survey is complete. The stone ring is yours to keep.",
    "message.wildercord.cartographer.quiet": "The astrolabe rests. Its survey needs a survival traveller, guardians enabled, and a world beyond Peaceful.",
    "message.wildercord.cartographer.active": "The Glass Cartographer is already sighting this ring.",
    "message.wildercord.cartographer.waiting": "The pedestal remembers its Cartographer. Wait for the ring to return to view.",
}


def write(gen):
    # 1. Entity texture (256x128) matching GlassCartographerModel UV layout
    img = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    px = img.load()
    for y in range(128):
        for x in range(256):
            n = ((x * 37 + y * 53) ^ (x * 13)) & 15
            if y < 64 and x < 128:
                # Pedestal stone & bronze base
                base = (94, 98, 104) if y < 36 else (138, 112, 66)
                px[x, y] = (max(0, min(255, base[0] + n - 7)), max(0, min(255, base[1] + n - 7)), max(0, min(255, base[2] + n - 7)), 255)
            elif 64 <= y < 94 and x < 104:
                # Torso: weathered slate & brass ribs
                rib = (x % 8 in (0, 7)) or (y % 10 in (0, 9))
                base = (176, 142, 78) if rib else (68, 76, 86)
                px[x, y] = (base[0] + n // 2, base[1] + n // 2, base[2] + n // 2, 255)
            elif 94 <= y < 128 and x < 104:
                # Head & astrolabe ring: polished brass and cyan-gold prism glass
                prism = (x + y) % 5 == 0
                base = (145, 224, 232) if prism else (196, 158, 84)
                px[x, y] = (min(255, base[0] + n // 2), min(255, base[1] + n // 2), min(255, base[2] + n // 2), 255)
            elif 64 <= y < 98 and 104 <= x < 140:
                # Prism wings
                edge = x in (104, 139) or y in (64, 97)
                base = (188, 150, 82) if edge else (126, 206, 220)
                px[x, y] = (base[0], base[1], base[2], 235)
            elif x >= 128 and y < 64:
                # Arms, transit staff, plumb weight, and central lens
                lens = 128 <= x < 152 and 48 <= y < 64
                base = (238, 214, 124) if lens else (164, 128, 68)
                px[x, y] = (min(255, base[0] + n // 2), min(255, base[1] + n // 2), min(255, base[2] + n // 2), 255)
    out_entity = gen.ASSETS / "textures/entity/glass_cartographer.png"
    out_entity.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_entity)
    glow = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    for y in range(48, 64):
        for x in range(128, 152):
            glow.putpixel((x, y), (238, 214, 124, 220))
    for y in range(64, 98):
        for x in range(104, 140):
            if (x + y) % 4 == 0:
                glow.putpixel((x, y), (145, 224, 232, 180))
    glow.save(gen.ASSETS / "textures/entity/glass_cartographer_glow.png")
    Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(gen.ASSETS / "textures/entity/glass_cartographer_mark.png")

    # 2. Survey Astrolabe block texture (16x16)
    blk = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bpx = blk.load()
    for y in range(16):
        for x in range(16):
            border = x in (0, 15) or y in (0, 15)
            ring = 4 <= x <= 11 and 3 <= y <= 10 and (x in (4, 11) or y in (3, 10) or x == y + 1)
            core = 7 <= x <= 8 and 6 <= y <= 7
            if core:
                bpx[x, y] = (162, 236, 242, 255)
            elif ring or border:
                bpx[x, y] = (194, 156, 82, 255)
            else:
                bpx[x, y] = (76, 82, 90, 255)
    out_blk = gen.ASSETS / "textures/block/survey_astrolabe.png"
    out_blk.parent.mkdir(parents=True, exist_ok=True)
    blk.save(out_blk)

    # 3. High Meridian Compass item texture (16x16)
    cmp_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cpx = cmp_img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 6.2:
                if d >= 4.8:
                    cpx[x, y] = (204, 166, 88, 255)
                elif abs(dx + dy) < 1.1 and dx <= 1 and dy <= 1:
                    cpx[x, y] = (146, 236, 244, 255)
                elif abs(dx - dy) < 1.0:
                    cpx[x, y] = (232, 196, 102, 255)
                else:
                    cpx[x, y] = (46, 54, 66, 255)
    out_cmp = gen.ASSETS / "textures/item/high_meridian_compass.png"
    out_cmp.parent.mkdir(parents=True, exist_ok=True)
    cmp_img.save(out_cmp)

    # 4. Blockstate, block model, item models, and client item definitions
    gen.write_json(gen.ASSETS / "blockstates/survey_astrolabe.json", {
        "variants": {"": {"model": "wildercord:block/survey_astrolabe"}}
    })
    gen.write_json(gen.ASSETS / "models/block/survey_astrolabe.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "wildercord:block/survey_astrolabe"},
    })
    gen.write_json(gen.ASSETS / "models/item/survey_astrolabe.json", {
        "parent": "wildercord:block/survey_astrolabe"
    })
    gen.write_json(gen.ASSETS / "items/survey_astrolabe.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/survey_astrolabe"}
    })
    gen.write_json(gen.ASSETS / "models/item/high_meridian_compass.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "wildercord:item/high_meridian_compass"},
    })
    gen.write_json(gen.ASSETS / "items/high_meridian_compass.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/high_meridian_compass"}
    })
    gen.write_json(gen.DATA / "loot_table/blocks/survey_astrolabe.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": "wildercord:survey_astrolabe"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
