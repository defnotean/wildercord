"""Procedural textures, block/item models, and text for The Kiln Widow and Kiln Heartstone."""
from PIL import Image

LANG = {
    "entity.wildercord.kiln_widow": "The Kiln Widow",
    "block.wildercord.kiln_heartstone": "Kiln Heartstone",
    "item.wildercord.kiln_heartstone": "Kiln Heartstone",
    "item.wildercord.cooled_kiln_compass": "Compass of the Cooled Kiln",
    "item.wildercord.cooled_kiln_compass.lore": "A fired-clay and iron damper compass tempered inside an Emberwood Kiln Court. Its needle settles toward the cooled hearth beside the kiln.",
    "item.wildercord.cooled_kiln_compass.use": "Points back to the Emberwood Kiln Court where it was earned, without needing a lodestone.",
    "guide.wildercord.kiln_widow": "A many-legged ceramic kiln guardian of fired brick, glazed terracotta and iron damper shutters anchored in an Emberwood Kiln Court.",
    "guide.wildercord.kiln_widow.hint": "Found at Emberwood Kiln Courts in the Emberwood Reach. Stand on the cooled hearth during its annular draft, step clear of its molten slag drop, and hold the firebreak lane during its flue sweep.",
    "journal.wildercord.entry.learned.kiln_widow": "Endured the firing trial of The Kiln Widow and earned a compass tuned to its Kiln Court.",
    "boss.wildercord.kiln_widow.status": "%s \u2014 %s",
    "boss.wildercord.kiln_widow.idle": "Banked",
    "boss.wildercord.kiln_widow.draft": "Annular kiln draft \u2014 stand on the cooled hearth",
    "boss.wildercord.kiln_widow.slag": "Molten glaze drop marked",
    "boss.wildercord.kiln_widow.sweep": "Stand in the cooled firebreak lane",
    "message.wildercord.kiln_widow.decorative": "This heartstone has no kiln court to wake.",
    "message.wildercord.kiln_widow.finished": "The firing trial is complete. The kiln court rests banked and cool.",
    "message.wildercord.kiln_widow.quiet": "The heartstone is banked. Its trial needs a survival traveller, guardians enabled, and a world beyond Peaceful.",
    "message.wildercord.kiln_widow.active": "The Kiln Widow is already firing in this court.",
    "message.wildercord.kiln_widow.waiting": "The plinth remembers its Kiln Widow. Wait for the court to return to view.",
}


def write(gen):
    # 1. Entity texture (256x128) matching KilnWidowModel UV layout
    img = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    px = img.load()
    for y in range(128):
        for x in range(256):
            n = ((x * 29 + y * 53) ^ (x * 19)) & 15
            if y < 64 and x < 128:
                # Plinth base: dark deepslate bricks & fired terracotta rim
                base = (58, 52, 50) if y < 36 else (154, 82, 54)
                px[x, y] = (max(0, min(255, base[0] + n - 7)), max(0, min(255, base[1] + n - 7)), max(0, min(255, base[2] + n - 7)), 255)
            elif 64 <= y < 128 and x < 128:
                # Hearth, ceramic dome, and chimney: fired brick with iron damper bands
                band = (x % 8 in (0, 7)) or (y % 9 in (0, 8))
                base = (78, 72, 70) if band else (168, 90, 56)
                px[x, y] = (min(255, base[0] + n // 2), min(255, base[1] + n // 2), min(255, base[2] + n // 2), 255)
            elif x >= 128 and y < 64:
                # Damper shutters, ceramic legs, and glowing slag core
                core = 128 <= x < 152 and 48 <= y < 64
                base = (246, 158, 68) if core else (138, 76, 52)
                px[x, y] = (min(255, base[0] + n // 2), min(255, base[1] + n // 2), min(255, base[2] + n // 2), 255)
    out_entity = gen.ASSETS / "textures/entity/kiln_widow.png"
    out_entity.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_entity)
    glow = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    for y in range(48, 64):
        for x in range(128, 152):
            glow.putpixel((x, y), (252, 168, 74, 225))
    for y in range(64, 120):
        for x in range(0, 128):
            if (x + y) % 6 == 0:
                glow.putpixel((x, y), (244, 136, 58, 185))
    glow.save(gen.ASSETS / "textures/entity/kiln_widow_glow.png")
    Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(gen.ASSETS / "textures/entity/kiln_widow_mark.png")

    # 2. Kiln Heartstone block texture (16x16)
    blk = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bpx = blk.load()
    for y in range(16):
        for x in range(16):
            border = x in (0, 15) or y in (0, 15)
            arch = 4 <= x <= 11 and 3 <= y <= 12 and (y >= 9 or 5 <= x <= 10)
            core = 6 <= x <= 9 and 9 <= y <= 12
            if core:
                bpx[x, y] = (248, 162, 72, 255)
            elif arch:
                bpx[x, y] = (168, 90, 56, 255)
            elif border:
                bpx[x, y] = (84, 76, 74, 255)
            else:
                bpx[x, y] = (54, 48, 46, 255)
    out_blk = gen.ASSETS / "textures/block/kiln_heartstone.png"
    out_blk.parent.mkdir(parents=True, exist_ok=True)
    blk.save(out_blk)

    # 3. Cooled Kiln Compass item texture (16x16)
    cmp_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cpx = cmp_img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 6.2:
                if d >= 4.8:
                    cpx[x, y] = (168, 90, 56, 255)
                elif abs(dx + dy) < 1.1 and dx <= 1 and dy <= 1:
                    cpx[x, y] = (248, 168, 78, 255)
                elif abs(dx - dy) < 1.0:
                    cpx[x, y] = (124, 198, 184, 255)
                else:
                    cpx[x, y] = (48, 42, 40, 255)
    out_cmp = gen.ASSETS / "textures/item/cooled_kiln_compass.png"
    out_cmp.parent.mkdir(parents=True, exist_ok=True)
    cmp_img.save(out_cmp)

    # 4. Blockstate, block model, item models, and client item definitions
    gen.write_json(gen.ASSETS / "blockstates/kiln_heartstone.json", {
        "variants": {"": {"model": "wildercord:block/kiln_heartstone"}}
    })
    gen.write_json(gen.ASSETS / "models/block/kiln_heartstone.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "wildercord:block/kiln_heartstone"},
    })
    gen.write_json(gen.ASSETS / "models/item/kiln_heartstone.json", {
        "parent": "wildercord:block/kiln_heartstone"
    })
    gen.write_json(gen.ASSETS / "items/kiln_heartstone.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/kiln_heartstone"}
    })
    gen.write_json(gen.ASSETS / "models/item/cooled_kiln_compass.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "wildercord:item/cooled_kiln_compass"},
    })
    gen.write_json(gen.ASSETS / "items/cooled_kiln_compass.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/cooled_kiln_compass"}
    })
    gen.write_json(gen.DATA / "loot_table/blocks/kiln_heartstone.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": "wildercord:kiln_heartstone"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
