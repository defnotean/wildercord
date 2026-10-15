"""Original reed-raft atlas, persistent attack markings and crossing-bell furniture."""
from PIL import Image

LANG = {
    "entity.wildercord.reed_crowned_ferryman": "Reed-Crowned Ferryman",
    "block.wildercord.crossing_bell": "Crossing Bell",
    "item.wildercord.crossing_compass": "Compass of the Last Crossing",
    "boss.wildercord.ferryman.status": "%s — %s",
    "boss.wildercord.ferryman.sweep": "Oar drawn back",
    "boss.wildercord.ferryman.anchor": "Anchor marked",
    "boss.wildercord.ferryman.wake": "Leave the crossing clear",
    "boss.wildercord.ferryman.idle": "Moored",
    "message.wildercord.ferryman.decorative": "This bell has no crossing to keep.",
    "message.wildercord.ferryman.finished": "The second count is complete. The crossing is yours to keep.",
    "message.wildercord.ferryman.quiet": "The bell rests. Its challenge needs a survival traveller, guardians enabled, and a world beyond Peaceful.",
    "message.wildercord.ferryman.active": "The Ferryman is already keeping this crossing.",
    "message.wildercord.ferryman.waiting": "The mooring remembers its Ferryman. Wait for the crossing to return to view.",
    "journal.wildercord.entry.learned.last_crossing": "The Last Crossing. A guardian of reeds and old timber answered the bell. Its compass remembers this landing. The boatkeepers' promise survives in whoever leaves the way clear for the next traveller.",
    "book.wildercord.last_crossing.1": "THE LAST CROSSING\n\nThe road ends here. The duty does not.\n\nRest without ringing the bell. The guardian answers a deliberate challenge.\n\nFour dry paths lead back to shore.",
    "book.wildercord.last_crossing.2": "Watch the oar. Leave the anchor's circle. When the lantern opens, stand in the clear crossing between its lines.\n\nThose who finish the second count receive a compass back to this landing.",
}


def atlas():
    image = Image.new("RGBA", (512, 512), (45, 52, 39, 255))
    pixels = image.load()
    for y in range(384):
        for x in range(384):
            noise = ((x * 37 + y * 67 + x * y * 11) % 11) - 5
            if y < 128 and x < 256:  # wet spruce, long grain and join lines
                base = (98, 70, 43)
                shade = -20 if x % 9 == 0 else (9 if x % 9 == 1 else noise)
            elif y < 128:  # the pale blue-green flame behind bronze bars
                base = (146, 235, 201)
                shade = -95 if x % 6 == 0 or y % 9 == 0 else noise
            elif y < 256 and x < 256:  # layered reed stalks and dark nodes
                base = (129, 145, 66)
                shade = -30 if y % 13 == 0 else (14 if x % 5 == 1 else noise)
            elif y >= 256 and x < 128:  # crossed warp and weft in the mantle
                base = (65, 112, 92)
                shade = (14 if (x // 2 + y // 2) % 2 == 0 else -8) + noise
            else:  # age-darkened iron, with copper-green edge flecks
                base = (69, 85, 78)
                shade = 15 if (x * 7 + y * 3) % 19 == 0 else noise
            pixels[x, y] = tuple(max(0, min(255, c + shade)) for c in base) + (255,)
    return image


def write(g):
    g.save(atlas(), g.ASSETS / "textures/entity/ferryman.png")
    glow = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    for y in range(14):
        for x in range(256, 280):
            if x % 6 != 0 and y % 9 != 0:
                glow.putpixel((x, y), (146, 235, 201, 220))
    g.save(glow, g.ASSETS / "textures/entity/ferryman_glow.png")
    g.save(Image.new("RGBA", (2, 2), (255, 255, 255, 255)), g.ASSETS / "textures/entity/ferryman_mark.png")
    g.write_json(g.ASSETS / "blockstates/crossing_bell.json", {"variants": {"": {"model": "wildercord:block/crossing_bell"}}})
    parts = [([1, 0, 2], [4, 16, 14], "wood"), ([12, 0, 2], [15, 16, 14], "wood"),
             ([1, 14, 2], [15, 16, 14], "wood"), ([5, 5, 4], [11, 13, 12], "bell"),
             ([4, 4, 3], [12, 6, 13], "bell"), ([7, 2, 7], [9, 5, 9], "dark")]
    g.write_json(g.ASSETS / "models/block/crossing_bell.json", {
        "textures": {"wood": "minecraft:block/stripped_spruce_log", "bell": "minecraft:block/exposed_copper",
                     "dark": "minecraft:block/dark_prismarine", "particle": "minecraft:block/exposed_copper"},
        "elements": [{"from": lo, "to": hi, "faces": {side: {"texture": "#" + texture} for side in
                     ("down", "up", "north", "south", "west", "east")}} for lo, hi, texture in parts]})
