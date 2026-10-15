"""Procedural textures, block/item models, and text for The Hollow Bell and Listening Belfry."""
from PIL import Image

LANG = {
    "entity.wildercord.hollow_bell": "The Hollow Bell",
    "block.wildercord.listening_belfry": "Listening Belfry",
    "item.wildercord.listening_belfry": "Listening Belfry",
    "item.wildercord.quiet_bell_compass": "Compass of the Quiet Bell",
    "item.wildercord.quiet_bell_compass.lore": "A verdigris-and-bronze acoustic compass tuned inside a Belowkeeper Bell Sanctum. Its needle settles toward the quiet eye beneath the bell.",
    "item.wildercord.quiet_bell_compass.use": "Points back to the Belowkeeper Bell Sanctum where it was earned, without needing a lodestone.",
    "guide.wildercord.hollow_bell": "A suspended Belowkeeper resonance bell of oxidized copper, deepslate and bronze clapper weights anchored in a Bell Sanctum.",
    "guide.wildercord.hollow_bell.hint": "Found at Belowkeeper Bell Sanctums in the Belowkeeper Hollows. Stand in the quiet eye during its toll, avoid its clapper drop, and hold the damped lane during its chord.",
    "journal.wildercord.entry.learned.hollow_bell": "Endured the resonance trial of The Hollow Bell and earned a compass tuned to its Bell Sanctum.",
    "boss.wildercord.hollow_bell.status": "%s \u2014 %s",
    "boss.wildercord.hollow_bell.idle": "Damped",
    "boss.wildercord.hollow_bell.toll": "Annular toll \u2014 stand in the quiet eye",
    "boss.wildercord.hollow_bell.clapper": "Clapper drop marked",
    "boss.wildercord.hollow_bell.chord": "Stand in the damped acoustic lane",
    "message.wildercord.hollow_bell.decorative": "This belfry has no resonance sanctum to wake.",
    "message.wildercord.hollow_bell.finished": "The resonance trial is complete. The sanctum rests in quiet.",
    "message.wildercord.hollow_bell.quiet": "The belfry is damped. Its trial needs a survival traveller, guardians enabled, and a world beyond Peaceful.",
    "message.wildercord.hollow_bell.active": "The Hollow Bell is already tolling in this sanctum.",
    "message.wildercord.hollow_bell.waiting": "The plinth remembers its Bell. Wait for the sanctum to return to view.",
}


def write(gen):
    # 1. Entity texture (256x128) matching HollowBellModel UV layout
    img = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    px = img.load()
    for y in range(128):
        for x in range(256):
            n = ((x * 31 + y * 59) ^ (x * 17)) & 15
            if y < 64 and x < 128:
                # Plinth base: polished deepslate & oxidized copper rim
                base = (56, 60, 68) if y < 36 else (74, 146, 134)
                px[x, y] = (max(0, min(255, base[0] + n - 7)), max(0, min(255, base[1] + n - 7)), max(0, min(255, base[2] + n - 7)), 255)
            elif 64 <= y < 128 and x < 128:
                # Yoke, crown, bell mantle, and skirt: bronze with verdigris bands
                band = (x % 8 in (0, 7)) or (y % 9 in (0, 8))
                base = (82, 168, 152) if band else (158, 118, 66)
                px[x, y] = (min(255, base[0] + n // 2), min(255, base[1] + n // 2), min(255, base[2] + n // 2), 255)
            elif x >= 128 and y < 64:
                # Shutters, clapper rod/weight, and resonance gauge
                gauge = 128 <= x < 152 and 48 <= y < 64
                base = (124, 232, 214) if gauge else (178, 134, 74)
                px[x, y] = (min(255, base[0] + n // 2), min(255, base[1] + n // 2), min(255, base[2] + n // 2), 255)
    out_entity = gen.ASSETS / "textures/entity/hollow_bell.png"
    out_entity.parent.mkdir(parents=True, exist_ok=True)
    img.save(out_entity)
    glow = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    for y in range(48, 64):
        for x in range(128, 152):
            glow.putpixel((x, y), (124, 232, 214, 220))
    for y in range(64, 118):
        for x in range(0, 128):
            if (x + y) % 6 == 0:
                glow.putpixel((x, y), (112, 224, 204, 175))
    glow.save(gen.ASSETS / "textures/entity/hollow_bell_glow.png")
    Image.new("RGBA", (2, 2), (255, 255, 255, 255)).save(gen.ASSETS / "textures/entity/hollow_bell_mark.png")

    # 2. Listening Belfry block texture (16x16)
    blk = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bpx = blk.load()
    for y in range(16):
        for x in range(16):
            border = x in (0, 15) or y in (0, 15)
            bell = 4 <= x <= 11 and 3 <= y <= 11 and (y >= 9 or 5 <= x <= 10)
            core = 7 <= x <= 8 and 11 <= y <= 13
            if core:
                bpx[x, y] = (132, 238, 222, 255)
            elif bell:
                bpx[x, y] = (176, 132, 72, 255)
            elif border:
                bpx[x, y] = (82, 168, 152, 255)
            else:
                bpx[x, y] = (52, 56, 64, 255)
    out_blk = gen.ASSETS / "textures/block/listening_belfry.png"
    out_blk.parent.mkdir(parents=True, exist_ok=True)
    blk.save(out_blk)

    # 3. Quiet Bell Compass item texture (16x16)
    cmp_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cpx = cmp_img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 6.2:
                if d >= 4.8:
                    cpx[x, y] = (86, 176, 160, 255)
                elif abs(dx + dy) < 1.1 and dx <= 1 and dy <= 1:
                    cpx[x, y] = (138, 242, 226, 255)
                elif abs(dx - dy) < 1.0:
                    cpx[x, y] = (196, 152, 84, 255)
                else:
                    cpx[x, y] = (42, 48, 56, 255)
    out_cmp = gen.ASSETS / "textures/item/quiet_bell_compass.png"
    out_cmp.parent.mkdir(parents=True, exist_ok=True)
    cmp_img.save(out_cmp)

    # 4. Blockstate, block model, item models, and client item definitions
    gen.write_json(gen.ASSETS / "blockstates/listening_belfry.json", {
        "variants": {"": {"model": "wildercord:block/listening_belfry"}}
    })
    gen.write_json(gen.ASSETS / "models/block/listening_belfry.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "wildercord:block/listening_belfry"},
    })
    gen.write_json(gen.ASSETS / "models/item/listening_belfry.json", {
        "parent": "wildercord:block/listening_belfry"
    })
    gen.write_json(gen.ASSETS / "items/listening_belfry.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/listening_belfry"}
    })
    gen.write_json(gen.ASSETS / "models/item/quiet_bell_compass.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "wildercord:item/quiet_bell_compass"},
    })
    gen.write_json(gen.ASSETS / "items/quiet_bell_compass.json", {
        "model": {"type": "minecraft:model", "model": "wildercord:item/quiet_bell_compass"}
    })
    gen.write_json(gen.DATA / "loot_table/blocks/listening_belfry.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": "wildercord:listening_belfry"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
