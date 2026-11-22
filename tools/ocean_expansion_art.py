"""1:1 procedural pixel art, entity skins, glow masks, 3D held weapons/relics, block models,
armor layers, recipes, entity loot tables, advancements, and language entries for the
Wildercord Deep Ocean Expansion."""

from PIL import Image

LANG = {
    # ---- Blocks ----
    "block.wildercord.siren_pearl_lantern": "Siren-Pearl Lantern",
    "block.wildercord.trench_anvil": "Trench Anvil",
    "block.wildercord.sovereign_altar": "Sovereign Awakening Altar",

    # ---- Equipment, Weapons & Relics ----
    "item.wildercord.abyssal_diving_helm": "Abyssal Diving Helm",
    "item.wildercord.abyssal_diving_helm.lore": "Riveted antique bronze and glowing trench-quartz glass.",
    "item.wildercord.abyssal_diving_helm.use": "Grants crisp underwater night vision and halves tank oxygen drain.",
    "item.wildercord.bathysphere_chestplate": "Bathysphere Chestplate",
    "item.wildercord.bathysphere_chestplate.lore": "Ribbed dark-prismarine carapace strapped with pressurized twin brass tanks.",
    "item.wildercord.bathysphere_chestplate.use": "Feeds compressed oxygen while submerged; full suit grants Conduit Power and 25% abyssal mitigation.",
    "item.wildercord.kelp_weave_greaves": "Kelp-Weave Greaves",
    "item.wildercord.kelp_weave_greaves.lore": "Toughened sea-kelp plates reinforced with riveted brass kneecaps.",
    "item.wildercord.kelp_weave_greaves.use": "Boosts underwater stride and agility.",
    "item.wildercord.trench_boots": "Trench Boots",
    "item.wildercord.trench_boots.lore": "Lead-weighted diver boots built to hold the abyssal floor.",
    "item.wildercord.trench_boots.use": "Hastens underwater excavation and increases step height.",

    "item.wildercord.tide_cleaver_harpoon": "Tide-Cleaver Harpoon",
    "item.wildercord.tide_cleaver_harpoon.lore": "Barbed crucible-silver point trailed by an iron retrieval chain.",
    "item.wildercord.tide_cleaver_harpoon.use": "Throw to skewer and reel foes back; hold sneak when anchored to zip yourself to blocks.",
    "item.wildercord.leviathan_glaive": "Leviathan Glaive",
    "item.wildercord.leviathan_glaive.lore": "Cradles a singing Heart-Pearl inside prismarine sea-fins.",
    "item.wildercord.leviathan_glaive.use": "+1.25 attack reach; right-click sweeps a 75-degree Hydro-Slash wave.",
    "item.wildercord.siren_pearl_lantern": "Siren-Pearl Lantern",
    "item.wildercord.siren_pearl_lantern.lore": "Brass-caged pearl swirling with magenta-cyan tide-light.",
    "item.wildercord.siren_pearl_lantern.use": "Hold or place underwater to push back minor hostiles and pierce the murk.",
    "item.wildercord.tidecaller_conch": "Tidecaller Conch",
    "item.wildercord.tidecaller_conch.lore": "Spiral ivory-and-ochre shell humming with calm tidal pressure.",
    "item.wildercord.tidecaller_conch.use": "Blow to conjure a 4.5-block breathable Air Dome and push small foes clear.",

    "item.wildercord.trench_anvil.lore": "Basalt-and-brass hydro-foundry core forged in ocean fissures.",
    "item.wildercord.trench_anvil.use": "Right-click with base gear or Abyssal Iron Plates to forge or repair Deep Ocean equipment.",
    "item.wildercord.sovereign_altar.lore": "Inlaid dark-prismarine pedestal tuned to the Trench Sovereign.",
    "item.wildercord.sovereign_altar.use": "Offer 1 Leviathan Heart-Pearl or 3 Abyssal Pearls to awaken Thalassor.",

    # ---- Materials & Sea Cuisine ----
    "item.wildercord.abyssal_pearl": "Abyssal Pearl",
    "item.wildercord.abyssal_pearl.lore": "Luminous lavender-teal orb gathered from deep citadels and merchants.",
    "item.wildercord.abyssal_pearl.use": "Key reagent at the Trench Anvil and currency for sovereign rituals.",
    "item.wildercord.leviathan_heart_pearl": "Leviathan Heart-Pearl",
    "item.wildercord.leviathan_heart_pearl.lore": "Pulsing cyan-core relic torn from Thalassor's breastplate.",
    "item.wildercord.leviathan_heart_pearl.use": "Forges the Leviathan Glaive or re-summons Thalassor immediately.",
    "item.wildercord.coral_edge_fang": "Coral-Edge Fang",
    "item.wildercord.coral_edge_fang.lore": "Serrated rose-coral tooth shed by a Coral-Spine Mako.",
    "item.wildercord.coral_edge_fang.use": "Forges Tide-Cleaver Harpoons and Leviathan Glaives at a Trench Anvil.",
    "item.wildercord.voltaic_jelly_sac": "Voltaic Jelly Sac",
    "item.wildercord.voltaic_jelly_sac.lore": "Crackling violet bladder humming with abyssal bio-charge.",
    "item.wildercord.voltaic_jelly_sac.use": "Catalyzes Siren-Pearl Lanterns at a Trench Anvil.",
    "item.wildercord.abyssal_iron_plate": "Abyssal Iron Plate",
    "item.wildercord.abyssal_iron_plate.lore": "Hammered corrosion-proof bronze-slate hull plating.",
    "item.wildercord.abyssal_iron_plate.use": "Forges and repairs Abyssal Diving Suit gear at a Trench Anvil.",
    "item.wildercord.star_barnacle_ambergris": "Star-Barnacle Ambergris",
    "item.wildercord.star_barnacle_ambergris.lore": "Sapphire-and-gold crystal shed by Singing Moon-Whales.",
    "item.wildercord.star_barnacle_ambergris.use": "Fuses with Nautilus Shells into a Tidecaller Conch and seasons sea-salt broth.",
    "item.wildercord.kelp_wrapped_sashimi": "Kelp-Wrapped Sashimi",
    "item.wildercord.kelp_wrapped_sashimi.lore": "Chilled salmon slices and nori rolls plated on glazed stoneware.",
    "item.wildercord.kelp_wrapped_sashimi.use": "Grants brief Conduit Power and Dolphin's Grace; tames Pearl-Pups.",
    "item.wildercord.sea_salt_ramen": "Sea-Salt Ramen",
    "item.wildercord.sea_salt_ramen.lore": "Steaming golden broth topped with kelp sheets, scallions, and soft egg.",
    "item.wildercord.sea_salt_ramen.use": "Grants Water Breathing, Night Vision, and strong Regeneration.",

    # ---- Messages ----
    "message.wildercord.trench_anvil.hint": "Hold Diamond Armor, Trident, Netherite Ingot, Lantern, or Nautilus Shell with Abyssal materials to forge — or hold damaged Abyssal gear + 1 Abyssal Iron Plate to repair.",
    "message.wildercord.trench_anvil.forged": "Forged %s at the Trench Anvil!",
    "message.wildercord.trench_anvil.missing": "Need %s x %s and %s x %s to forge this item.",
    "message.wildercord.trench_anvil.repaired": "Repaired Abyssal equipment with 1 Abyssal Iron Plate.",
    "message.wildercord.sovereign_altar.active": "Thalassor still prowls the Colosseum above.",
    "message.wildercord.sovereign_altar.summoned": "The Sovereign Awakening Altar surges — Thalassor rises into the Colosseum!",
    "message.wildercord.sovereign_altar.cooldown": "Thalassor reforms in %s seconds, or offer 1 Leviathan Heart-Pearl / 3 Abyssal Pearls now.",
    "message.wildercord.sovereign_altar.offering": "Hold 1 Leviathan Heart-Pearl or 3 Abyssal Pearls to summon Thalassor.",

    # ---- Entities ----
    "entity.wildercord.barnacle_hulk_dredger": "Barnacle-Hulk Dredger",
    "entity.wildercord.siren_veil_jelly": "Siren-Veil Jelly",
    "entity.wildercord.coral_spine_mako": "Coral-Spine Mako",
    "entity.wildercord.dread_nautilus_sentinel": "Dread-Nautilus Sentinel",
    "entity.wildercord.thalassor": "Thalassor, Abyssal Sovereign",
    "entity.wildercord.brine_merchant": "Tidefolk Brine-Merchant",
    "entity.wildercord.brine_merchant.pearl_jeweler": "Tidefolk Pearl Jeweler",
    "entity.wildercord.brine_merchant.abyssal_cartographer": "Tidefolk Abyssal Cartographer",
    "entity.wildercord.brine_merchant.kelp_alchemist": "Tidefolk Kelp Alchemist",
    "entity.wildercord.lumi_seahorse": "Lumi-Seahorse",
    "entity.wildercord.pearl_pup": "Pearl-Pup",
    "entity.wildercord.singing_moon_whale": "Singing Moon-Whale",
    "entity.wildercord.thrown_harpoon": "Tide-Cleaver Harpoon",

    # ---- Spawn Eggs ----
    "item.wildercord.barnacle_hulk_dredger_spawn_egg": "Barnacle-Hulk Dredger Spawn Egg",
    "item.wildercord.siren_veil_jelly_spawn_egg": "Siren-Veil Jelly Spawn Egg",
    "item.wildercord.coral_spine_mako_spawn_egg": "Coral-Spine Mako Spawn Egg",
    "item.wildercord.dread_nautilus_sentinel_spawn_egg": "Dread-Nautilus Sentinel Spawn Egg",
    "item.wildercord.thalassor_spawn_egg": "Thalassor Spawn Egg",
    "item.wildercord.brine_merchant_spawn_egg": "Tidefolk Brine-Merchant Spawn Egg",
    "item.wildercord.lumi_seahorse_spawn_egg": "Lumi-Seahorse Spawn Egg",
    "item.wildercord.pearl_pup_spawn_egg": "Pearl-Pup Spawn Egg",
    "item.wildercord.singing_moon_whale_spawn_egg": "Singing Moon-Whale Spawn Egg",

    # ---- Field Guide ----
    "guide.wildercord.barnacle_hulk_dredger": "Heavy sunken diver revenants overgrown with coral, slamming chains to reel divers close.",
    "guide.wildercord.barnacle_hulk_dredger.hint": "Patrols sunken galleons, deep ocean floors, and hydrothermal vents.",
    "guide.wildercord.siren_veil_jelly": "Bioluminescent violet medusae that contract to loose stinging electric rings.",
    "guide.wildercord.siren_veil_jelly.hint": "Drifts in mid-water ocean depths and Citadel colosseums.",
    "guide.wildercord.coral_spine_mako": "Armored open-sea sharks whose serrated coral dorsal fins telegraph a torpedo lunge.",
    "guide.wildercord.coral_spine_mako.hint": "Hunts across deep oceans and circling Sunken Sovereign Galleons.",
    "guide.wildercord.dread_nautilus_sentinel": "Bronze-shelled ruin guardians whose spiral armor turns back rear blows before firing a focal beam.",
    "guide.wildercord.dread_nautilus_sentinel.hint": "Guards the outer colosseum walls of Abyssal Trench Citadels.",
    "guide.wildercord.thalassor": "A fourteen-segment obsidian trench sovereign wielding sweeping hydro-beams and maelstrom pulls.",
    "guide.wildercord.thalassor.hint": "Awakens above the Sovereign Altar in an Abyssal Trench Citadel.",
    "guide.wildercord.brine_merchant": "Diving-suited Tidefolk artisans trading pearls, conches, charts, and steaming ramen.",
    "guide.wildercord.brine_merchant.hint": "Inhabits dry glass domes inside Coral-Dome Tidefolk Sanctuaries.",
    "guide.wildercord.lumi_seahorse": "Luminous reef-crowned steeds capable of 3D underwater steering and burst dashes.",
    "guide.wildercord.lumi_seahorse.hint": "Swims calmly through coral and deep oceans; tempt with kelp or cod.",
    "guide.wildercord.pearl_pup": "Tameable lantern seals whose glowing forehead lure seeks sunken treasure chests.",
    "guide.wildercord.pearl_pup.hint": "Found around Coral-Dome Sanctuaries and coastal shelves.",
    "guide.wildercord.singing_moon_whale": "Colossal star-barnacle leviathans whose song bestows Water Breathing and sheds ambergris.",
    "guide.wildercord.singing_moon_whale.hint": "Glides serenely through open ocean waters.",

    # ---- Advancements ----
    "advancements.wildercord.ocean.deep_diver.title": "Pressurized Plunge",
    "advancements.wildercord.ocean.deep_diver.description": "Forge and wear any piece of the Abyssal Diving Suit",
    "advancements.wildercord.ocean.trench_forged.title": "Trench-Forged Arsenal",
    "advancements.wildercord.ocean.trench_forged.description": "Obtain a Tide-Cleaver Harpoon or Leviathan Glaive",
    "advancements.wildercord.ocean.sanctuary_trader.title": "Salt & Pearl Commerce",
    "advancements.wildercord.ocean.sanctuary_trader.description": "Trade with a Tidefolk Brine-Merchant inside the depths",
    "advancements.wildercord.ocean.conch_shelter.title": "Pocket of Calm",
    "advancements.wildercord.ocean.conch_shelter.description": "Sound a Tidecaller Conch to conjure a breathable Air Dome",
    "advancements.wildercord.ocean.sovereign_slayer.title": "Sovereign of the Trench",
    "advancements.wildercord.ocean.sovereign_slayer.description": "Defeat Thalassor, Abyssal Sovereign and claim its Heart-Pearl",
}


def _rect(img, x0, y0, w, h, c):
    px = img.load()
    iw, ih = img.size
    for y in range(max(0, y0), min(ih, y0 + h)):
        for x in range(max(0, x0), min(iw, x0 + w)):
            px[x, y] = c


def _paint_box(img, u, v, w, h, d, body, shade=None, hi=None):
    if shade is None:
        shade = (max(0, body[0] - 28), max(0, body[1] - 28), max(0, body[2] - 24), body[3] if len(body) > 3 else 255)
    if hi is None:
        hi = (min(255, body[0] + 22), min(255, body[1] + 22), min(255, body[2] + 24), body[3] if len(body) > 3 else 255)
    c_body = body if len(body) == 4 else (*body, 255)
    c_sh = shade if len(shade) == 4 else (*shade, 255)
    c_hi = hi if len(hi) == 4 else (*hi, 255)
    # Top, Bottom
    _rect(img, u + d, v, w, d, c_hi)
    _rect(img, u + d + w, v, w, d, c_sh)
    # Right, Front, Left, Back
    _rect(img, u, v + d, d, h, c_body)
    _rect(img, u + d, v + d, w, h, c_body)
    _rect(img, u + d + w, v + d, d, h, c_sh)
    _rect(img, u + d + w + d, v + d, w, h, c_sh)


def _draw_from_grid(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows[:16]):
        for x, ch in enumerate(row[:16]):
            if ch in palette:
                px[x, y] = palette[ch]
    return img


def _build_icons():
    out = {}
    out["abyssal_pearl"] = _draw_from_grid([
        "................",
        "......OOOO......",
        "....OOLLLLOO....",
        "...OHHHLLLTOO...",
        "..OHWWLTTTBBBO..",
        "..OHWLTTTBBBBO..",
        ".OLLTTTBBBBPPPO.",
        ".OLLTTBBPPPPPPPO",
        ".OTTTBPPPPPPVVPO",
        ".OTTBPPPVVVVVVVO",
        "..OBBPVVVVPPPPO.",
        "..OBPPVVVPPPPO..",
        "...OOVVVPPPPO...",
        "....OOVPPPOO....",
        "......OOOO......",
        "................",
    ], {
        "O": (26, 20, 44, 255), "W": (245, 255, 255, 255), "H": (192, 250, 255, 255),
        "L": (122, 232, 240, 255), "T": (74, 178, 198, 255), "B": (88, 134, 186, 255),
        "P": (106, 84, 162, 255), "V": (66, 48, 114, 255),
    })

    out["leviathan_heart_pearl"] = _draw_from_grid([
        "................",
        ".....OOOOOO.....",
        "...OORRRRRROO...",
        "..ORRDDDDDDRRO..",
        "..ORDOOOOOODRO..",
        ".ORDOCWWWWCODRO.",
        ".ORDOWCCCCWODRO.",
        ".ORDOWCCCCWODRO.",
        ".ORDOWCCCBBODRO.",
        ".ORDOCCBBBBODRO.",
        "..ORDDOOOODDRO..",
        "..ORRRDDDDDRRO..",
        "...OORRRRRROO...",
        ".....OOOOOO.....",
        "................",
        "................",
    ], {
        "O": (10, 40, 56, 255), "R": (44, 232, 245, 255), "D": (16, 128, 150, 255),
        "W": (232, 255, 255, 255), "C": (72, 242, 255, 255), "B": (28, 178, 212, 255),
    })

    out["coral_edge_fang"] = _draw_from_grid([
        "................",
        ".............OO.",
        "............OCCO",
        "...........OCCPO",
        ".........OOCCPO.",
        "........OCCPPO..",
        ".......OCCPPO...",
        "......OCPPPO....",
        ".....OEEPPPO....",
        "....OEEEPPO.....",
        "...OEEEEPO......",
        "..OEEEEEO.......",
        "..OWWEEEO.......",
        "..OOWWOO........",
        "...OOO..........",
        "................",
    ], {
        "O": (92, 32, 42, 255), "C": (255, 160, 172, 255), "P": (218, 92, 110, 255),
        "E": (235, 198, 182, 255), "W": (255, 242, 232, 255),
    })

    out["voltaic_jelly_sac"] = _draw_from_grid([
        "................",
        ".....OOOOOO.....",
        "...OOMMMMMMOO...",
        "..OMMWWWMMMMMO..",
        ".OMMWWWMMMMBBMO.",
        ".OMMWWMMMMBBBBMO",
        ".OMMMMMMMMMBBBMO",
        ".OMMMMMBBBBBBBMO",
        ".OMMBBBBBBBBBBMO",
        "..OMBBBBVVVVBMO.",
        "..OMMBVVVVVVBMO.",
        "...OOBVVVVVBOO..",
        "....OOVVVVVOO...",
        ".....OOOOOO.....",
        "................",
        "................",
    ], {
        "O": (72, 10, 96, 255), "W": (255, 224, 255, 255), "M": (244, 120, 255, 255),
        "B": (196, 46, 238, 255), "V": (128, 20, 172, 255),
    })

    out["abyssal_iron_plate"] = _draw_from_grid([
        "................",
        ".OOOOOOOOOOOOOO.",
        ".OLLLLLBBBBBBBO.",
        ".OLWWLBSSSSSSBO.",
        ".OLWWBSSSSSSSBO.",
        ".OLBBSSSSLLLSSO.",
        ".OBSSSSLLLLSSBO.",
        ".OBSSLLLLSSSSBO.",
        ".OBLLLLSSSSSSBO.",
        ".OBSSSSSSLLLLBO.",
        ".OBSSSSSSSSSSBO.",
        ".OBWWSSSSSSWWBO.",
        ".OBWWBBBBBBWWBO.",
        ".OBBBBBBBBBBBBO.",
        ".OOOOOOOOOOOOOO.",
        "................",
    ], {
        "O": (34, 24, 24, 255), "B": (72, 54, 52, 255), "S": (94, 72, 68, 255),
        "L": (126, 98, 92, 255), "W": (208, 220, 232, 255),
    })

    out["star_barnacle_ambergris"] = _draw_from_grid([
        "................",
        "......OOOO......",
        "....OOBGGGO.....",
        "...OBBGGGGGOO...",
        "..OBBYYGGBBBBO..",
        ".OBBYYYYBBBBGGO.",
        ".OBBBYYBBBBGGGO.",
        ".OBBBBBBBBBBYYO.",
        ".OOOBBBBYYYYYYO.",
        "..OBBBGGGGYYBBO.",
        "..OBBGGGBBBBBO..",
        "...OOBBBBBBOO...",
        "....OOOBBBOO....",
        "......OOOO......",
        "................",
        "................",
    ], {
        "O": (22, 32, 68, 255), "B": (46, 92, 186, 255), "G": (86, 152, 232, 255),
        "Y": (255, 226, 86, 255),
    })

    out["kelp_wrapped_sashimi"] = _draw_from_grid([
        "................",
        "................",
        "....OOOOOOO.....",
        "..OOKKKKKKKOO...",
        ".OKKKSSSKNNKKO..",
        ".OKKSSWSSNWNKKO.",
        ".OPPSSSSKNWNKPO.",
        "OPPPPWWSOKKNPKPO",
        "OPLLLGGWGGGGLLPO",
        "OPLLLLGGGGLLLLPO",
        ".OPLLLLLLLLLLPO.",
        ".OPLLLLLLLLLLPO.",
        "..OODDDDDDDDDO..",
        "...OOOOOOOOOO...",
        "................",
        "................",
    ], {
        "O": (28, 36, 48, 255), "K": (32, 92, 46, 255), "S": (255, 118, 86, 255),
        "W": (255, 236, 226, 255), "N": (20, 48, 28, 255), "G": (118, 212, 86, 255),
        "P": (184, 206, 224, 255), "L": (134, 160, 182, 255), "D": (78, 96, 116, 255),
    })

    out["sea_salt_ramen"] = _draw_from_grid([
        "................",
        "....S...S...S...",
        "...S...S...S....",
        "........OOOOOO..",
        "..OOOOOOKKKKKKO.",
        ".OWWYYOGKKGGKKO.",
        ".OYWYYNNNNNNKKO.",
        ".OYYYYGGGGYYYYO.",
        "OWWWWWWWWWWWWWWO",
        "OBBBBBBBBBBBBBBO",
        ".OWWWWWWWWWWWWO.",
        ".OWLLLLLLLLLLWO.",
        "..OWWLLLLLLWWOO.",
        "...OOWWWWWWO....",
        ".....OOOOOO.....",
        "................",
    ], {
        "S": (235, 248, 255, 255), "O": (34, 34, 48, 255), "K": (26, 70, 38, 255),
        "G": (82, 198, 76, 255), "N": (244, 196, 76, 255), "Y": (252, 212, 94, 255),
        "W": (244, 246, 250, 255), "B": (46, 112, 186, 255), "L": (182, 194, 210, 255),
    })

    out["abyssal_diving_helm"] = _draw_from_grid([
        "......OOOO......",
        ".....OBBBBO.....",
        "...OOBBBBBBOO...",
        "..OBBDDDDDDBBO..",
        ".OBBDOOOOOODBBO.",
        ".OGBDOCCCCODBGO.",
        ".OGGDOCWWCODGGO.",
        ".OGGDOCCCCODGGO.",
        ".OBBDOCCCCODBBO.",
        "..OBBDOOOODBBO..",
        "..OBDDDDDDDDBO..",
        "...OOBBBBBBOO...",
        "..OBBDDDDDDBBO..",
        "..OOOOOOOOOOOO..",
        "................",
        "................",
    ], {
        "O": (42, 28, 12, 255), "B": (188, 134, 56, 255), "D": (124, 84, 34, 255),
        "G": (218, 168, 78, 255), "C": (48, 245, 255, 255), "W": (212, 255, 255, 255),
    })

    out["bathysphere_chestplate"] = _draw_from_grid([
        "................",
        "..OOOO....OOOO..",
        ".OBBBBO..OBBBBO.",
        ".OBPPPPOOPPPPOB.",
        ".OPPPCCCCCCCPO..",
        "..OPPPDDDDDPO...",
        "..OPPPPPPCCPO...",
        "..OPPPDDDDDPO...",
        "..OPPPPPPPPO....",
        "..OPPPDDDDPO....",
        "..OBBBBBBBBO....",
        "...OOOOOOOO.....",
        "................",
        "................",
        "................",
        "................",
    ], {
        "O": (14, 36, 40, 255), "B": (196, 144, 58, 255), "P": (32, 108, 112, 255),
        "D": (20, 68, 74, 255), "C": (66, 245, 232, 255),
    })

    out["kelp_weave_greaves"] = _draw_from_grid([
        "................",
        "..OOOOOOOOOOOO..",
        "..OEEEEEEEEEEO..",
        "..OEWWEEEEGGGO..",
        "..OEEGGEEEEGGO..",
        "..OEEEEOOEEEEO..",
        "..OBBBBOOBBBBO..",
        "..ODDDDOODDDDO..",
        "..OEEGEOOGEEEO..",
        "..OGEEEOOEEEGO..",
        "..OEEGEOOGEEEO..",
        "..OGEEEOOEEEGO..",
        "..OOOOOO.OOOOO..",
        "................",
        "................",
        "................",
    ], {
        "O": (12, 42, 28, 255), "E": (32, 152, 98, 255), "G": (56, 224, 136, 255),
        "W": (182, 255, 212, 255), "B": (198, 146, 56, 255), "D": (128, 88, 30, 255),
    })

    out["trench_boots"] = _draw_from_grid([
        "................",
        "................",
        "..OOOO....OOOO..",
        "..OLBO....OBLO..",
        "..OLBO....OBLO..",
        "..OLBO....OBLO..",
        "..OLBO....OBLO..",
        "..OLBOOO..OBLO..",
        "..OLLLLBO.OLLO..",
        "..OLLLBBOOOOOO..",
        "..ODDDDDDDDDDO..",
        "..OOOOOOOOOOOO..",
        "................",
        "................",
        "................",
        "................",
    ], {
        "O": (36, 20, 12, 255), "L": (166, 94, 50, 255), "B": (108, 58, 30, 255),
        "D": (42, 82, 78, 255),
    })

    out["tide_cleaver_harpoon"] = _draw_from_grid([
        "............OO..",
        "...........OSSO.",
        "..........OSSSO.",
        ".........OSSSSO.",
        "........OOBBBO..",
        "........OBBBO...",
        ".......OGTBO....",
        "......OGTOOO....",
        ".....OBTO.OCOO..",
        "....OBTO.OC..CO.",
        "...OGTO..OC..CO.",
        "..OGTO....OCOO..",
        ".OBBOO..........",
        ".OBO............",
        "..OO............",
        "................",
    ], {
        "O": (28, 34, 42, 255), "S": (212, 226, 236, 255), "B": (188, 134, 54, 255),
        "G": (48, 186, 174, 255), "T": (108, 72, 42, 255), "C": (132, 146, 162, 255),
    })

    out["leviathan_glaive"] = _draw_from_grid([
        "............OO..",
        "...........OCCO.",
        "..........OCWCO.",
        ".........OCCWCO.",
        "........OCCWCO..",
        ".......OGBPPBO..",
        "......OGPPWPPBO.",
        ".....OGOBPPPO...",
        "....OTTO........",
        "...OGTO.........",
        "..OTTO..........",
        ".OGTO...........",
        "OBBO............",
        "OBO.............",
        ".OO.............",
        "................",
    ], {
        "O": (12, 44, 52, 255), "C": (72, 240, 230, 255), "W": (224, 255, 255, 255),
        "P": (48, 232, 255, 255), "G": (24, 134, 128, 255), "B": (212, 162, 62, 255),
        "T": (30, 92, 98, 255),
    })

    out["tidecaller_conch"] = _draw_from_grid([
        "................",
        "......OOOOO.....",
        "....OOWWWWWOO...",
        "...OWWCCCCCCWO..",
        "..OWWCCPPPPCCWO.",
        "..OWCCPPWPPPCCWO",
        ".OWWCPPPWWPPPCWO",
        ".OWCCPPPPPPPPCWO",
        "..OWWCCCCCCCCWO.",
        "..OOWWCCCCCCCWO.",
        "...OOOWWWWWWWOO.",
        ".....OOOOOOOOO..",
        "................",
        "................",
        "................",
        "................",
    ], {
        "O": (88, 62, 42, 255), "W": (250, 240, 222, 255), "C": (222, 184, 136, 255),
        "P": (244, 168, 164, 255),
    })

    return out


def _egg_icon(primary, spot):
    return _draw_from_grid([
        "................",
        "......OOOO......",
        "....OOBBBBOO....",
        "...OBBBSSBBBO...",
        "..OBBBSSSSBBBO..",
        "..OBBBBBBBBBBO..",
        ".OBSSBBBBSSBBBO.",
        ".OBSSBBBBSSBBBO.",
        ".OBBBBBBBBBBBBO.",
        ".OBBBBSSBBBBBSO.",
        ".OBBBBSSBBBBSSO.",
        "..OBBBBBBBBBBO..",
        "..OOBBBBBBBBOO..",
        "...OOBBBBBBOO...",
        "....OOOOOOOO....",
        "................",
    ], {
        "O": (22, 26, 34, 255),
        "B": (*primary, 255),
        "S": (*spot, 255),
    })


import sys as _sys
from pathlib import Path as _Path

_TOOLS_DIR = str(_Path(__file__).resolve().parent)
if _TOOLS_DIR not in _sys.path:
    _sys.path.insert(0, _TOOLS_DIR)

import world_art as _wa  # noqa: E402
from item_art import mix as _mix  # noqa: E402


def _display_poses():
    return {
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 7.5, 0.5], "scale": [0.9, 0.9, 0.9]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 7.5, 0.5], "scale": [0.9, 0.9, 0.9]},
        "firstperson_righthand": {"rotation": [0, -92, 42], "translation": [0, 0.4, -1.6], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 88, -42], "translation": [0, 0.4, -1.6], "scale": [0.6, 0.6, 0.6]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }


def _elem(x0, y0, z0, x1, y1, z1, tex, uv=None):
    if uv is None:
        u0 = max(0.0, min(15.5, x0))
        v0 = max(0.0, min(15.5, 16.0 - y1))
        u1 = max(u0 + 0.5, min(16.0, x1))
        v1 = max(v0 + 0.5, min(16.0, 16.0 - y0))
        uv = [u0, v0, u1, v1]
    face = {"texture": f"#{tex}", "uv": uv}
    return {
        "from": [x0, y0, z0],
        "to": [x1, y1, z1],
        "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")},
    }


def _shade_tile(base, hi, lo, accent=None, mode="metal", seed=17):
    tile = Image.new("RGBA", (32, 32), (*base, 255))
    px = tile.load()
    for y in range(32):
        for x in range(32):
            n = _wa.noise(x, y, seed)
            border = x in (0, 31) or y in (0, 31)
            rivet = (x in (2, 29) and y in (2, 29)) or (x in (2, 29) and y == 16)
            col = base
            if mode == "metal":
                col = _mix(base, lo, 0.38 if border else (0.15 if n < 0.35 else 0.0))
                if rivet:
                    col = hi
                elif (x + y) % 8 == 0:
                    col = _mix(base, hi, 0.32)
            elif mode == "pearl":
                dx, dy = (x - 15.5) / 15.5, (y - 15.5) / 15.5
                d = (dx * dx + dy * dy) ** 0.5
                if d < 0.45:
                    col = hi
                elif d < 0.85:
                    col = _mix(hi, base, (d - 0.45) / 0.4)
                else:
                    col = lo
                if accent and ((x + y) % 5 == 0) and d > 0.35:
                    col = _mix(col, accent, 0.45)
            elif mode == "molten":
                col = lo if n < 0.5 else base
                if accent and (abs(x - 15.5) < 5 or (n > 0.82)):
                    col = _mix(accent, hi, 0.6 if n > 0.7 else 0.2)
            elif mode == "shell":
                band = (x + y * 2) % 6 < 2
                col = _mix(base, hi if band else lo, 0.35 + n * 0.25)
            else:
                col = _mix(base, hi if n > 0.65 else lo, 0.28 if n > 0.65 or n < 0.3 else 0.0)
                if border:
                    col = _mix(col, lo, 0.3)
            px[x, y] = (*col, 255)
    return tile


def _build_3d_weapon_models(g):
    tiles = {
        "tide_cleaver_harpoon_steel": _shade_tile((192, 212, 228), (240, 250, 255), (96, 114, 132), mode="metal", seed=11),
        "tide_cleaver_harpoon_brass": _shade_tile((188, 134, 52), (236, 186, 86), (112, 74, 28), mode="metal", seed=12),
        "tide_cleaver_harpoon_teal": _shade_tile((32, 132, 124), (72, 198, 182), (18, 76, 72), mode="wrap", seed=13),
        "leviathan_glaive_blade": _shade_tile((72, 224, 218), (192, 255, 255), (24, 134, 136), accent=(232, 255, 255), mode="pearl", seed=21),
        "leviathan_glaive_gold": _shade_tile((212, 162, 58), (255, 224, 128), (134, 92, 26), mode="metal", seed=22),
        "leviathan_glaive_pearl": _shade_tile((64, 240, 255), (220, 255, 255), (24, 148, 182), accent=(180, 255, 255), mode="pearl", seed=23),
        "leviathan_glaive_haft": _shade_tile((22, 86, 92), (48, 148, 152), (12, 46, 52), mode="metal", seed=24),
        "tidecaller_conch_shell": _shade_tile((244, 226, 194), (255, 248, 234), (176, 144, 108), mode="shell", seed=31),
        "tidecaller_conch_pink": _shade_tile((242, 162, 158), (255, 206, 198), (178, 92, 96), mode="shell", seed=32),
    }
    for name, img in tiles.items():
        g.save(img, g.ASSETS / f"textures/block/{name}.png")

    # 1. Tide-Cleaver Harpoon 3D (24 sculpted elements: shaft, grip rings, loop pommel, winged guard, barbed tip + hanging chain)
    harpoon_in_hand = {
        "textures": {
            "steel": "wildercord:block/tide_cleaver_harpoon_steel",
            "brass": "wildercord:block/tide_cleaver_harpoon_brass",
            "teal": "wildercord:block/tide_cleaver_harpoon_teal",
            "particle": "wildercord:block/tide_cleaver_harpoon_steel",
        },
        "elements": [
            # Pommel bronze ring loop
            _elem(6.8, -11.6, 7.2, 9.2, -10.8, 8.8, "brass", [2, 2, 8, 5]),
            _elem(6.4, -10.8, 7.2, 7.3, -9.2, 8.8, "brass", [2, 2, 5, 5]),
            _elem(8.7, -10.8, 7.2, 9.6, -9.2, 8.8, "brass", [2, 2, 5, 5]),
            _elem(6.8, -9.2, 7.2, 9.2, -8.4, 8.8, "brass", [2, 2, 8, 5]),
            # Main fluted bronze shaft & teal leather-wrapped grip bands
            _elem(7.4, -8.4, 7.4, 8.6, 14.0, 8.6, "brass"),
            _elem(7.1, -3.5, 7.1, 8.9, -1.2, 8.9, "teal"),
            _elem(7.1, -0.5, 7.1, 8.9, 2.2, 8.9, "teal"),
            _elem(7.1, 2.8, 7.1, 8.9, 5.2, 8.9, "teal"),
            _elem(7.1, 9.5, 7.1, 8.9, 12.0, 8.9, "teal"),
            # Crescent bronze crossguard collar & upturned horn wings
            _elem(6.4, 13.4, 6.8, 9.6, 14.6, 9.2, "brass"),
            _elem(4.6, 14.0, 7.1, 6.6, 15.2, 8.9, "brass"),
            _elem(9.4, 14.0, 7.1, 11.4, 15.2, 8.9, "brass"),
            _elem(3.8, 14.8, 7.3, 5.0, 16.4, 8.7, "brass"),
            _elem(11.0, 14.8, 7.3, 12.2, 16.4, 8.7, "brass"),
            # Tapered 6-step crucible-silver barbed harpoon spearhead & twin backward barbs
            _elem(7.1, 15.0, 7.3, 8.9, 24.5, 8.7, "steel"),
            _elem(6.3, 17.5, 7.4, 9.7, 22.8, 8.6, "steel"),
            _elem(5.4, 17.8, 7.5, 10.6, 21.0, 8.5, "steel"),
            _elem(7.5, 24.5, 7.6, 8.5, 27.2, 8.4, "steel"),
            _elem(4.6, 16.0, 7.5, 5.6, 18.8, 8.5, "steel"),
            _elem(10.4, 16.0, 7.5, 11.4, 18.8, 8.5, "steel"),
            # Trailing catenary iron retrieval chain loop along shaft side
            _elem(8.8, 12.2, 7.5, 11.2, 13.4, 8.5, "steel"),
            _elem(10.6, 7.6, 7.5, 11.8, 12.4, 8.5, "steel"),
            _elem(9.4, 4.2, 7.5, 11.2, 7.8, 8.5, "steel"),
            _elem(8.6, 1.2, 7.5, 10.2, 4.6, 8.5, "steel"),
        ],
        "display": _display_poses(),
    }
    g.write_json(g.ASSETS / "models/item/tide_cleaver_harpoon_in_hand.json", harpoon_in_hand)

    # 2. Leviathan Glaive 3D (24 sculpted elements: sea-fin crossguard, double-sided glowing Heart-Pearl, curved crescent blade)
    glaive_in_hand = {
        "textures": {
            "blade": "wildercord:block/leviathan_glaive_blade",
            "gold": "wildercord:block/leviathan_glaive_gold",
            "pearl": "wildercord:block/leviathan_glaive_pearl",
            "haft": "wildercord:block/leviathan_glaive_haft",
            "particle": "wildercord:block/leviathan_glaive_blade",
        },
        "elements": [
            # Tapered spiked golden pommel finial + gold haft collar rings
            _elem(7.5, -12.8, 7.6, 8.5, -9.8, 8.4, "gold"),
            _elem(6.4, -10.2, 7.2, 9.6, -8.6, 8.8, "gold"),
            _elem(7.3, -8.6, 7.3, 8.7, 12.2, 8.7, "haft"),
            _elem(7.0, -3.0, 7.0, 9.0, -1.8, 9.0, "gold"),
            _elem(7.0, 4.0, 7.0, 9.0, 5.2, 9.0, "gold"),
            # Sculpted sea-fin crossguard cradling the glowing Heart-Pearl orb
            _elem(5.4, 11.8, 6.7, 10.6, 13.4, 9.3, "gold"),
            _elem(4.2, 12.8, 7.1, 6.2, 16.4, 8.9, "gold"),
            _elem(9.8, 12.8, 7.1, 11.8, 16.4, 8.9, "gold"),
            _elem(3.2, 13.8, 7.4, 4.6, 15.8, 8.6, "blade"),
            _elem(11.4, 13.8, 7.4, 12.8, 15.8, 8.6, "blade"),
            # Dual-sided faceted glowing Leviathan Heart-Pearl orb + inner bezel
            _elem(6.3, 12.6, 6.0, 9.7, 16.0, 10.0, "pearl", [0, 0, 16, 16]),
            _elem(6.8, 13.1, 5.5, 9.2, 15.5, 10.5, "pearl", [2, 2, 14, 14]),
            _elem(6.8, 15.8, 6.9, 9.2, 16.8, 9.1, "gold"),
            # Towering stepped sweeping curved crescent blade + gold dorsal spine
            _elem(7.1, 16.6, 7.4, 9.3, 19.6, 8.6, "blade"),
            _elem(6.8, 19.2, 7.4, 9.7, 23.0, 8.6, "blade"),
            _elem(6.2, 22.4, 7.5, 9.5, 26.2, 8.5, "blade"),
            _elem(5.4, 25.4, 7.5, 8.8, 28.8, 8.5, "blade"),
            _elem(4.6, 28.0, 7.6, 7.6, 30.8, 8.4, "blade"),
            _elem(3.8, 30.0, 7.7, 6.2, 31.8, 8.3, "blade"),
            _elem(8.9, 16.6, 7.3, 9.9, 24.5, 8.7, "gold"),
            _elem(8.4, 24.0, 7.3, 9.4, 27.6, 8.7, "gold"),
        ],
        "display": _display_poses(),
    }
    g.write_json(g.ASSETS / "models/item/leviathan_glaive_in_hand.json", glaive_in_hand)

    # 3. Tidecaller Conch 3D (14 sculpted stepped spiral whorl & flaring pink lip elements)
    conch_in_hand = {
        "textures": {
            "shell": "wildercord:block/tidecaller_conch_shell",
            "pink": "wildercord:block/tidecaller_conch_pink",
            "particle": "wildercord:block/tidecaller_conch_shell",
        },
        "elements": [
            _elem(5.0, 5.0, 4.5, 12.0, 12.0, 11.5, "shell"),
            _elem(7.0, 4.2, 4.0, 13.5, 10.0, 11.0, "pink"),
            _elem(10.5, 4.0, 5.0, 14.5, 8.2, 10.6, "pink"),
            _elem(3.0, 5.8, 5.2, 5.4, 11.4, 10.8, "shell"),
            _elem(1.2, 6.5, 6.0, 3.2, 10.6, 10.0, "shell"),
            _elem(-0.4, 7.2, 6.7, 1.4, 9.8, 9.3, "shell"),
            _elem(-1.8, 7.8, 7.3, -0.2, 9.2, 8.7, "shell"),
            _elem(5.8, 11.6, 5.8, 10.6, 12.6, 10.2, "shell"),
            _elem(6.8, 5.6, 11.2, 10.8, 10.8, 12.2, "shell"),
        ],
        "display": {
            **_display_poses(),
            "firstperson_righthand": {"rotation": [12, -55, 8], "translation": [0, 1.5, -1.5], "scale": [0.75, 0.75, 0.75]},
            "thirdperson_righthand": {"rotation": [20, -45, 0], "translation": [0, 3.0, 1.5], "scale": [0.75, 0.75, 0.75]},
        },
    }
    g.write_json(g.ASSETS / "models/item/tidecaller_conch_in_hand.json", conch_in_hand)

    for wid in ("tide_cleaver_harpoon", "leviathan_glaive", "tidecaller_conch"):
        g.item_model(wid, wid)
        g.write_json(g.ASSETS / f"items/{wid}.json", {
            "model": {
                "type": "minecraft:select",
                "property": "minecraft:display_context",
                "cases": [{
                    "when": ["gui", "fixed", "on_shelf"],
                    "model": {"type": "minecraft:model", "model": f"wildercord:item/{wid}"},
                }],
                "fallback": {"type": "minecraft:model", "model": f"wildercord:item/{wid}_in_hand"},
            }
        })


def _build_blocks_and_armor_layers(g):
    # Custom 32x32 Shaded Block Textures
    brass_tex = _shade_tile((186, 132, 52), (236, 186, 86), (112, 74, 28), mode="metal", seed=41)
    pearl_tex = _shade_tile((196, 68, 255), (124, 252, 255), (112, 24, 186), accent=(64, 245, 255), mode="pearl", seed=42)
    anvil_tex = _shade_tile((42, 42, 54), (92, 96, 116), (22, 22, 30), mode="metal", seed=43)
    anvil_lava = _shade_tile((244, 128, 32), (255, 228, 112), (168, 52, 18), accent=(255, 196, 48), mode="molten", seed=44)
    altar_tex = _shade_tile((22, 62, 68), (58, 148, 152), (12, 34, 40), mode="metal", seed=45)
    g.save(brass_tex, g.ASSETS / "textures/block/siren_pearl_cage.png")
    g.save(pearl_tex, g.ASSETS / "textures/block/siren_pearl_core.png")
    g.save(anvil_tex, g.ASSETS / "textures/block/trench_anvil.png")
    g.save(anvil_lava, g.ASSETS / "textures/block/trench_anvil_lava.png")
    g.save(altar_tex, g.ASSETS / "textures/block/sovereign_altar.png")

    # Siren-Pearl Lantern block (22 elements: pedestal, corner bars, pagoda brass crown, ring loop & swirling Pearl)
    lantern_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "cage": "wildercord:block/siren_pearl_cage",
            "core": "wildercord:block/siren_pearl_core",
            "particle": "wildercord:block/siren_pearl_cage",
        },
        "elements": [
            _elem(3.0, 0.0, 3.0, 13.0, 1.2, 13.0, "cage"),
            _elem(4.0, 1.2, 4.0, 12.0, 2.0, 12.0, "cage"),
            _elem(4.0, 8.2, 4.0, 12.0, 9.2, 12.0, "cage"),
            _elem(5.0, 9.2, 5.0, 11.0, 10.4, 11.0, "cage"),
            _elem(6.5, 10.4, 6.5, 9.5, 11.2, 9.5, "cage"),
            _elem(7.4, 11.2, 7.6, 8.6, 12.8, 8.4, "cage"),
            # 4 corner brass pillars + crossbars
            _elem(4.0, 2.0, 4.0, 5.0, 8.2, 5.0, "cage"),
            _elem(11.0, 2.0, 4.0, 12.0, 8.2, 5.0, "cage"),
            _elem(4.0, 2.0, 11.0, 5.0, 8.2, 12.0, "cage"),
            _elem(11.0, 2.0, 11.0, 12.0, 8.2, 12.0, "cage"),
            _elem(5.0, 4.6, 4.2, 11.0, 5.4, 4.8, "cage"),
            _elem(5.0, 4.6, 11.2, 11.0, 5.4, 11.8, "cage"),
            _elem(4.2, 4.6, 5.0, 4.8, 5.4, 11.0, "cage"),
            _elem(11.2, 4.6, 5.0, 11.8, 5.4, 11.0, "cage"),
            # Floating faceted glowing Magenta-Cyan Siren Pearl inside cage
            _elem(5.6, 2.8, 5.6, 10.4, 7.6, 10.4, "core", [0, 0, 16, 16]),
            _elem(6.2, 2.2, 6.2, 9.8, 8.2, 9.8, "core", [2, 2, 14, 14]),
        ],
    }
    g.write_json(g.ASSETS / "models/block/siren_pearl_lantern.json", lantern_model)
    g.write_json(g.ASSETS / "blockstates/siren_pearl_lantern.json", {"variants": {"": {"model": "wildercord:block/siren_pearl_lantern"}}})
    g.write_json(g.ASSETS / "items/siren_pearl_lantern.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/siren_pearl_lantern"}})

    # Trench Anvil block (16 elements matching 1:1 Voxel Callout: basalt step base, molten core waist, horned anvil face & lava channel)
    anvil_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "anvil": "wildercord:block/trench_anvil",
            "brass": "wildercord:block/siren_pearl_cage",
            "lava": "wildercord:block/trench_anvil_lava",
            "particle": "wildercord:block/trench_anvil",
        },
        "elements": [
            _elem(1.5, 0.0, 1.5, 14.5, 2.5, 14.5, "anvil"),
            _elem(3.0, 2.5, 3.0, 13.0, 4.5, 13.0, "anvil"),
            _elem(4.2, 4.5, 4.8, 11.8, 9.8, 11.2, "lava", [0, 0, 16, 16]),
            _elem(3.6, 5.0, 4.4, 4.6, 9.4, 11.6, "brass"),
            _elem(11.4, 5.0, 4.4, 12.4, 9.4, 11.6, "brass"),
            _elem(4.0, 9.8, 3.2, 12.0, 11.4, 12.8, "anvil"),
            _elem(2.0, 11.2, 2.5, 14.2, 15.2, 13.5, "anvil"),
            _elem(-0.5, 12.0, 4.5, 2.2, 15.0, 11.5, "anvil"),
            _elem(-2.2, 12.8, 6.2, -0.4, 14.6, 9.8, "lava", [2, 2, 14, 14]),
            _elem(14.0, 11.8, 4.0, 15.6, 15.0, 12.0, "anvil"),
            # Glowing lava forging groove inlaid on top tabletop
            _elem(3.2, 15.2, 5.5, 13.0, 16.0, 10.5, "lava", [0, 0, 16, 16]),
        ],
    }
    g.write_json(g.ASSETS / "models/block/trench_anvil.json", anvil_model)
    g.write_json(g.ASSETS / "blockstates/trench_anvil.json", {
        "variants": {
            f"facing={facing},waterlogged={wl}": {"model": "wildercord:block/trench_anvil", **({"y": rot} if rot else {})}
            for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
            for wl in ("false", "true")
        }
    })
    g.write_json(g.ASSETS / "items/trench_anvil.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/trench_anvil"}})

    # Sovereign Altar block (stepped prismarine plinth, gold inlay & corner mini-obelisks + floating cyan Heart-Crystal)
    altar_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "altar": "wildercord:block/sovereign_altar",
            "gold": "wildercord:block/leviathan_glaive_gold",
            "core": "wildercord:block/leviathan_glaive_pearl",
            "particle": "wildercord:block/sovereign_altar",
        },
        "elements": [
            _elem(0.0, 0.0, 0.0, 16.0, 6.0, 16.0, "altar"),
            _elem(0.5, 6.0, 0.5, 15.5, 7.5, 15.5, "gold"),
            _elem(1.5, 7.5, 1.5, 14.5, 11.0, 14.5, "altar"),
            # 4 Corner Obsidian-Gold Runic Pylons
            _elem(1.0, 11.0, 1.0, 3.5, 15.0, 3.5, "altar"),
            _elem(12.5, 11.0, 1.0, 15.0, 15.0, 3.5, "altar"),
            _elem(1.0, 11.0, 12.5, 3.5, 15.0, 15.0, "altar"),
            _elem(12.5, 11.0, 12.5, 15.0, 15.0, 15.0, "altar"),
            _elem(1.3, 14.8, 1.3, 3.2, 16.0, 3.2, "core", [2, 2, 14, 14]),
            _elem(12.8, 14.8, 1.3, 14.7, 16.0, 3.2, "core", [2, 2, 14, 14]),
            _elem(1.3, 14.8, 12.8, 3.2, 16.0, 14.7, "core", [2, 2, 14, 14]),
            _elem(12.8, 14.8, 12.8, 14.7, 16.0, 14.7, "core", [2, 2, 14, 14]),
            # Central recessed basin & levitating faceted Cyan Sovereign Crystal
            _elem(4.5, 11.0, 4.5, 11.5, 12.2, 11.5, "gold"),
            _elem(5.5, 12.6, 5.5, 10.5, 16.5, 10.5, "core", [0, 0, 16, 16]),
            _elem(6.4, 16.5, 6.4, 9.6, 18.0, 9.6, "core", [2, 2, 14, 14]),
        ],
    }
    g.write_json(g.ASSETS / "models/block/sovereign_altar.json", altar_model)
    g.write_json(g.ASSETS / "blockstates/sovereign_altar.json", {"variants": {"": {"model": "wildercord:block/sovereign_altar"}}})
    g.write_json(g.ASSETS / "items/sovereign_altar.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/sovereign_altar"}})

    # Detailed Worn Armor Material Layers (abyssal_diving)
    g.write_json(g.ASSETS / "equipment/abyssal_diving.json", {
        "layers": {layer: [{"texture": "wildercord:abyssal_diving"}] for layer in ("humanoid", "humanoid_leggings")}
    })
    humanoid = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px_h = humanoid.load()
    for name, (u, v, w, h, d, c_base, c_hi, c_lo, detail) in (
        ("helm", (0, 0, 8, 8, 8, (176, 122, 48), (222, 168, 78), (108, 70, 24), "rivet")),
        ("chest", (16, 16, 8, 12, 4, (28, 92, 98), (58, 152, 154), (16, 54, 60), "rib_belt")),
        ("arms", (40, 16, 4, 12, 4, (162, 112, 46), (208, 156, 72), (104, 68, 24), "cuff")),
        ("boots", (0, 16, 4, 12, 4, (148, 84, 42), (192, 118, 62), (86, 46, 20), "cuff")),
    ):
        for face, (x0, y0, fw, fh) in _wa.box(u, v, w, h, d).items():
            for ly in range(fh):
                for lx in range(fw):
                    col = _mix(c_base, c_hi if (ly == 0 or lx == 0) else c_lo, 0.25 if _wa.noise(x0 + lx, y0 + ly, 5) > 0.6 else 0.0)
                    if detail == "rib_belt" and face not in ("top", "bottom"):
                        if ly in (2, 5, 8):
                            col = c_hi
                        elif ly >= fh - 3:
                            col = (122, 74, 34) if lx not in (3, 4) else (218, 168, 72)
                    elif detail == "cuff" and face not in ("top", "bottom") and ly in (3, 4, fh - 2):
                        col = c_hi
                    px_h[x0 + lx, y0 + ly] = (*col, 255)
    g.save(humanoid, g.ASSETS / "textures/entity/equipment/humanoid/abyssal_diving.png")

    legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px_l = legs.load()
    for face, (x0, y0, fw, fh) in {**_wa.box(0, 16, 4, 12, 4), **_wa.box(16, 16, 8, 12, 4)}.items():
        for ly in range(fh):
            for lx in range(fw):
                weave = (lx + ly) % 2 == 0
                col = (28, 134, 86) if weave else (20, 98, 66)
                if ly in (4, 5) and face in ("front", "left", "right"):
                    col = (188, 134, 54)  # brass kneecaps
                elif ly <= 1:
                    col = (132, 82, 38)   # leather belt
                px_l[x0 + lx, y0 + ly] = (*col, 255)
    g.save(legs, g.ASSETS / "textures/entity/equipment/humanoid_leggings/abyssal_diving.png")

    # 3D Helm Porthole & Back O2 Tanks (128x128 sub-voxel sheet matching AbyssalDivingSuitLayer 1:1)
    layer3d = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    glow3d = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    def _paint_metal_box(img, u, v, w, h, d, base, hi, lo, rivets=True, stripes=False, gauge=False, glow_img=None):
        px = img.load()
        gpx = glow_img.load() if glow_img else None
        for face, (x0, y0, fw, fh) in _wa.box(u, v, w, h, d).items():
            for ly in range(fh):
                for lx in range(fw):
                    n = _wa.noise(x0 + lx, y0 + ly, u + v)
                    col = _mix(base, hi if face == "top" else lo, 0.25 if face in ("top", "bottom") else (0.18 if n < 0.3 else 0.0))
                    if face not in ("top", "bottom"):
                        if lx in (0, fw - 1) or ly in (0, fh - 1):
                            col = lo
                        elif rivets and (lx in (1, fw - 2) and ly in (1, fh - 2)):
                            col = hi
                        elif stripes and (ly % 4 == 0):
                            col = hi
                    if gauge and face == "front":
                        dx, dy = (lx - (fw - 1) / 2.0), (ly - (fh - 1) / 2.0)
                        if (dx * dx + dy * dy) <= (fw * 0.42) ** 2:
                            col = (64, 248, 255)
                            if gpx:
                                gpx[x0 + lx, y0 + ly] = (92, 250, 255, 255)
                    px[x0 + lx, y0 + ly] = (*col, 255)

    B_BASE, B_HI, B_LO = (182, 128, 48), (232, 184, 86), (108, 70, 24)
    _paint_metal_box(layer3d, 0, 0, 18, 18, 18, (166, 114, 42), B_HI, B_LO)                 # helm dome + rivets
    _paint_metal_box(layer3d, 74, 0, 12, 3, 12, (192, 138, 56), B_HI, B_LO)                # crown cap
    _paint_metal_box(layer3d, 0, 38, 13, 12, 3, B_BASE, B_HI, B_LO)                        # front bezel
    _paint_metal_box(layer3d, 34, 38, 9, 8, 2, (42, 232, 252), (186, 255, 255), (18, 148, 186), rivets=False, glow_img=layer3d)
    _paint_metal_box(glow3d, 34, 38, 9, 8, 2, (52, 242, 255), (210, 255, 255), (28, 188, 218), rivets=False)
    _paint_metal_box(layer3d, 58, 38, 1, 9, 2, B_BASE, B_HI, B_LO, rivets=False)           # vertical grille bar
    _paint_metal_box(layer3d, 66, 38, 9, 1, 2, B_BASE, B_HI, B_LO, rivets=False)           # horizontal grille bar
    _paint_metal_box(layer3d, 90, 38, 3, 8, 8, (148, 98, 38), B_HI, B_LO)                  # ear-port grate
    _paint_metal_box(layer3d, 0, 56, 20, 3, 18, (158, 106, 40), B_HI, B_LO)                # shoulder collar
    _paint_metal_box(layer3d, 0, 80, 6, 17, 6, (206, 154, 58), (248, 206, 108), B_LO, stripes=True)
    _paint_metal_box(layer3d, 26, 80, 4, 2, 4, B_BASE, B_HI, B_LO, rivets=False)
    _paint_metal_box(layer3d, 44, 80, 7, 2, 7, (124, 78, 38), (176, 118, 62), (76, 44, 20), rivets=False)
    _paint_metal_box(layer3d, 74, 80, 14, 3, 4, (116, 126, 138), (168, 182, 198), (72, 82, 92))
    _paint_metal_box(layer3d, 26, 90, 2, 10, 2, (162, 102, 58), (212, 142, 86), (104, 62, 34), rivets=False)
    _paint_metal_box(layer3d, 36, 90, 5, 5, 2, B_BASE, B_HI, B_LO, rivets=False, gauge=True, glow_img=glow3d)
    g.save(layer3d, g.ASSETS / "textures/entity/ocean/abyssal_diving_3d.png")
    g.save(glow3d, g.ASSETS / "textures/entity/ocean/abyssal_diving_3d_glow.png")


def _paint_textured_box(img, uv_box, painter, glow_img=None, glow_painter=None):
    u, v, w, h, d = uv_box
    px = img.load()
    gpx = glow_img.load() if glow_img else None
    iw, ih = img.size
    for face, (x0, y0, fw, fh) in _wa.box(u, v, w, h, d).items():
        if fw <= 0 or fh <= 0:
            continue
        sh = 1 if face == "top" else (-2 if face == "bottom" else (0 if face == "front" else -1))
        for ly in range(fh):
            for lx in range(fw):
                gx, gy = x0 + lx, y0 + ly
                if 0 <= gx < iw and 0 <= gy < ih:
                    col = painter(face, lx, ly, fw, fh, sh, gx, gy)
                    if col is not None:
                        px[gx, gy] = col
                    if gpx is not None and glow_painter is not None:
                        gc = glow_painter(face, lx, ly, fw, fh, sh, gx, gy)
                        if gc is not None:
                            gpx[gx, gy] = gc


def _verdigris_metal(base, hi, lo, patina=(58, 124, 114), seed=1, alpha=255):
    def p(face, lx, ly, fw, fh, sh, gx, gy):
        n = _wa.noise(gx, gy, seed)
        border = lx in (0, fw - 1) or ly in (0, fh - 1)
        rivet = (fw >= 5 and fh >= 5) and (lx in (1, fw - 2) and ly in (1, fh - 2))
        col = _mix(base, hi if sh > 0 else lo, 0.25 if sh != 0 else 0.0)
        if n > 0.68:
            col = _mix(col, patina, 0.55)
        if border:
            col = _mix(col, lo, 0.48)
        elif rivet:
            col = hi
        return (*col, alpha)
    return p


def _organic_shaded(base, hi, lo, alpha=255, stripe=False, spot=False, seed=7):
    def p(face, lx, ly, fw, fh, sh, gx, gy):
        n = _wa.noise(gx, gy, seed)
        col = _mix(base, hi if sh > 0 else lo, 0.22 if sh != 0 else 0.0)
        if stripe and face not in ("top", "bottom") and (ly % 3 == 0):
            col = _mix(col, lo, 0.32)
        if spot and n > 0.84:
            col = _mix(col, lo, 0.65)
        elif n > 0.72:
            col = _mix(col, hi, 0.22)
        return (*col, alpha)
    return p


def _glow_fill(color, alpha=255):
    def p(face, lx, ly, fw, fh, sh, gx, gy):
        cx, cy = (fw - 1) * 0.5, (fh - 1) * 0.5
        d = ((lx - cx) ** 2 + (ly - cy) ** 2) ** 0.5
        rim = max(1.0, min(fw, fh) * 0.5)
        col = _mix((232, 255, 255), color, min(1.0, d / rim))
        return (*col, alpha)
    return p


def _build_entity_skins(g):
    def save_pair(name, skin, glow=None):
        g.save(skin, g.ASSETS / f"textures/entity/ocean/{name}.png")
        if glow is not None:
            g.save(glow, g.ASSETS / f"textures/entity/ocean/{name}_glow.png")

    # 1. Barnacle-Hulk Dredger (256x256)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    verdigris_suit = _verdigris_metal((48, 88, 82), (82, 136, 126), (26, 52, 48), patina=(118, 92, 58), seed=101)
    aged_bronze = _verdigris_metal((176, 122, 48), (224, 172, 82), (104, 68, 24), patina=(64, 132, 118), seed=102)
    steel = _verdigris_metal((104, 116, 128), (156, 172, 188), (58, 66, 74), patina=(74, 96, 98), seed=103)
    for ub, fn in (
        ((0, 0, 22, 26, 14), verdigris_suit),
        ((74, 0, 18, 12, 2), aged_bronze),
        ((116, 0, 24, 4, 18), aged_bronze),
        ((84, 130, 20, 2, 16), aged_bronze),
        ((202, 0, 6, 18, 6), _verdigris_metal((198, 146, 54), (242, 198, 96), (124, 84, 28), patina=(148, 112, 42), seed=104)),
        ((228, 0, 4, 2, 4), aged_bronze),
        ((202, 24, 8, 2, 8), _verdigris_metal((112, 72, 34), (152, 102, 52), (68, 42, 18), seed=105)),
        ((0, 42, 20, 20, 20), aged_bronze),
        ((82, 42, 14, 3, 14), aged_bronze),
        ((140, 42, 6, 2, 6), aged_bronze),
        ((82, 60, 14, 12, 3), aged_bronze),
        ((142, 60, 1, 9, 2), aged_bronze),
        ((150, 60, 3, 8, 8), aged_bronze),
        ((174, 60, 2, 6, 6), steel),
        ((192, 60, 12, 3, 4), steel),
        ((226, 60, 3, 8, 3), _organic_shaded((96, 64, 38), (138, 94, 54), (56, 38, 20), stripe=True)),
        ((0, 84, 14, 11, 14), aged_bronze),
        ((58, 84, 15, 3, 15), aged_bronze),
        ((120, 84, 24, 5, 16), _verdigris_metal((118, 76, 38), (162, 108, 56), (68, 44, 20), seed=106)),
        ((202, 84, 4, 6, 3), _verdigris_metal((136, 88, 44), (178, 122, 66), (82, 50, 22), seed=107)),
        ((218, 84, 5, 5, 2), _verdigris_metal((212, 162, 62), (248, 208, 106), (138, 98, 32), seed=108)),
        ((0, 112, 9, 14, 9), verdigris_suit),
        ((38, 112, 11, 14, 11), aged_bronze),
        ((84, 112, 12, 4, 12), aged_bronze),
        ((134, 112, 8, 6, 8), verdigris_suit),
        ((168, 112, 10, 13, 10), verdigris_suit),
        ((210, 112, 8, 7, 3), aged_bronze),
        ((0, 150, 9, 11, 9), verdigris_suit),
        ((38, 150, 11, 5, 13), aged_bronze),
        ((88, 150, 3, 12, 3), _organic_shaded((242, 124, 154), (255, 178, 196), (176, 68, 96))),
        ((102, 150, 2, 7, 2), _organic_shaded((242, 124, 154), (255, 178, 196), (176, 68, 96))),
        ((112, 150, 3, 10, 3), _organic_shaded((132, 212, 238), (192, 244, 255), (76, 152, 182))),
        ((126, 150, 2, 6, 2), _organic_shaded((132, 212, 238), (192, 244, 255), (76, 152, 182))),
        ((136, 150, 6, 3, 6), _organic_shaded((226, 224, 214), (248, 248, 242), (162, 160, 152), spot=True)),
        ((162, 150, 3, 3, 3), _organic_shaded((232, 228, 216), (252, 250, 244), (146, 144, 134))),
        ((176, 150, 3, 5, 1), steel),
        ((186, 150, 1, 5, 3), steel),
        ((196, 150, 3, 16, 3), steel),
        ((210, 150, 14, 3, 3), steel),
        ((0, 172, 18, 4, 4), steel),
        ((46, 172, 4, 11, 4), steel),
        ((64, 172, 6, 4, 2), steel),
    ):
        _paint_textured_box(s, ub, fn)
    _paint_textured_box(s, (118, 60, 10, 8, 1), _glow_fill((36, 224, 245)), gl, _glow_fill((52, 245, 255)))
    save_pair("barnacle_hulk_dredger", s, gl)

    # 2. Siren-Veil Jelly (128x128 translucent indigo bell + glowing magenta core + electric shock ring)
    s = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for ub, a in (((0, 0, 22, 16, 22), 145), ((0, 38, 18, 3, 18), 165), ((0, 59, 12, 2, 12), 180), ((0, 73, 24, 3, 24), 175)):
        _paint_textured_box(s, ub, _organic_shaded((84, 72, 206), (142, 132, 255), (46, 36, 138), alpha=a, stripe=True))
    _paint_textured_box(s, (72, 38, 14, 11, 14), _organic_shaded((216, 58, 232), (248, 134, 255), (142, 26, 176), alpha=185), gl, _glow_fill((212, 64, 236), alpha=150))
    _paint_textured_box(s, (46, 100, 10, 3, 10), _organic_shaded((224, 72, 242), (255, 162, 255), (148, 32, 182), alpha=200), gl, _glow_fill((224, 82, 245), alpha=170))
    _paint_textured_box(s, (96, 0, 8, 8, 8), _glow_fill((142, 252, 255), alpha=235), gl, _glow_fill((168, 255, 255), alpha=245))
    _paint_textured_box(s, (96, 16, 4, 4, 4), _glow_fill((236, 255, 255)), gl, _glow_fill((255, 255, 255)))
    _paint_textured_box(s, (48, 59, 2, 2, 9), _organic_shaded((206, 78, 238), (248, 148, 255), (124, 42, 168), alpha=220))
    _paint_textured_box(s, (0, 100, 2, 10, 2), _organic_shaded((86, 186, 252), (178, 236, 255), (52, 106, 208), alpha=225), gl, _glow_fill((96, 212, 255), alpha=190))
    _paint_textured_box(s, (10, 100, 2, 10, 2), _organic_shaded((224, 96, 248), (252, 174, 255), (148, 42, 186), alpha=225), gl, _glow_fill((232, 108, 255), alpha=190))
    _paint_textured_box(s, (20, 100, 1, 9, 1), _glow_fill((148, 240, 255), alpha=220), gl, _glow_fill((168, 248, 255), alpha=230))
    _paint_textured_box(s, (26, 100, 6, 2, 3), _glow_fill((118, 248, 255), alpha=220), gl, _glow_fill((162, 255, 255), alpha=240))
    save_pair("siren_veil_jelly", s, gl)

    # 3. Coral-Spine Mako (256x256)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    def mako_skin(gills=False, jaw=False):
        def p(face, lx, ly, fw, fh, sh, gx, gy):
            n = _wa.noise(gx, gy, 303)
            if jaw or face == "bottom" or (face in ("left", "right", "back", "front") and ly >= fh * 0.66):
                col = _mix((178, 202, 218), (218, 232, 242), 0.25 if n > 0.6 else 0.0)
            else:
                col = _mix((28, 74, 138), (48, 108, 182), 0.25 if (sh > 0 or n > 0.68) else 0.0)
            if gills and face in ("left", "right") and fw >= 14 and (2 <= lx <= 10) and (lx % 3 == 0) and (2 <= ly <= fh - 4):
                col = (14, 34, 68)
            return (*col, 255)
        return p

    for ub, fn in (
        ((0, 0, 16, 14, 20), mako_skin(gills=True)),
        ((72, 0, 14, 3, 18), mako_skin()),
        ((0, 34, 15, 11, 14), mako_skin()),
        ((58, 34, 13, 7, 8), mako_skin()),
        ((100, 34, 9, 5, 5), mako_skin()),
        ((58, 50, 11, 4, 14), mako_skin(jaw=True)),
        ((110, 50, 12, 11, 16), mako_skin()),
        ((168, 50, 8, 8, 12), mako_skin()),
        ((136, 10, 2, 2, 6), _organic_shaded((16, 36, 72), (24, 52, 98), (10, 20, 44))),
        ((136, 20, 10, 2, 10), _organic_shaded((142, 34, 48), (178, 54, 68), (94, 18, 28))),
        ((186, 20, 9, 1, 10), _organic_shaded((142, 34, 48), (178, 54, 68), (94, 18, 28))),
        ((176, 20, 1, 2, 1), _organic_shaded((248, 250, 252), (255, 255, 255), (206, 214, 224))),
        ((0, 82, 3, 6, 10), mako_skin()), ((26, 82, 2, 5, 8), mako_skin()),
        ((46, 82, 2, 4, 6), mako_skin()), ((62, 82, 1, 3, 4), mako_skin()),
        ((80, 82, 2, 4, 4), mako_skin()), ((94, 82, 7, 2, 6), mako_skin()),
        ((122, 82, 3, 12, 5), mako_skin()), ((140, 82, 2, 8, 5), mako_skin()),
        ((156, 82, 2, 6, 5), mako_skin()), ((172, 82, 1, 5, 4), mako_skin()),
        ((184, 82, 2, 7, 5), mako_skin()), ((200, 82, 1, 5, 4), mako_skin()),
        ((0, 102, 8, 2, 10), mako_skin()), ((36, 102, 7, 2, 8), mako_skin()), ((66, 102, 6, 1, 6), mako_skin()),
    ):
        _paint_textured_box(s, ub, fn)
    for ub in ((136, 0, 3, 5, 3), (150, 0, 2, 4, 3), (162, 0, 3, 6, 3), (176, 0, 2, 5, 3), (188, 0, 2, 5, 3)):
        _paint_textured_box(s, ub, _organic_shaded((248, 114, 138), (255, 178, 192), (186, 62, 86)), gl, _glow_fill((242, 92, 122), alpha=180))
    _paint_textured_box(s, (154, 10, 1, 3, 4), _glow_fill((255, 206, 48)), gl, _glow_fill((255, 212, 56)))
    save_pair("coral_spine_mako", s, gl)

    # 4. Dread-Nautilus Sentinel (256x128)
    s = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    for ub, fn in (
        ((0, 0, 18, 11, 10), _verdigris_metal((166, 106, 48), (218, 152, 78), (94, 58, 24), patina=(54, 132, 118), seed=401)),
        ((60, 0, 18, 11, 10), _verdigris_metal((52, 126, 114), (86, 172, 156), (32, 76, 70), patina=(156, 104, 52), seed=402)),
        ((128, 0, 22, 12, 12), _verdigris_metal((184, 128, 52), (232, 178, 86), (108, 72, 26), patina=(58, 128, 116), seed=403)),
        ((128, 50, 26, 7, 7), _verdigris_metal((198, 144, 58), (242, 192, 96), (118, 82, 30), seed=404)),
        ((128, 26, 15, 7, 9), _verdigris_metal((32, 68, 66), (54, 104, 100), (18, 40, 42), patina=(148, 96, 42), seed=405)),
        ((176, 26, 12, 11, 7), _verdigris_metal((22, 46, 48), (40, 74, 76), (12, 26, 28), seed=406)),
        ((0, 48, 4, 4, 13), _verdigris_metal((188, 136, 56), (236, 186, 88), (116, 80, 28), seed=407)),
        ((34, 48, 3, 4, 11), _verdigris_metal((202, 148, 62), (244, 198, 98), (126, 86, 32), seed=408)),
        ((64, 48, 2, 2, 10), _organic_shaded((86, 192, 178), (142, 236, 222), (48, 124, 116))),
    ):
        _paint_textured_box(s, ub, fn)
    _paint_textured_box(s, (214, 26, 7, 7, 4), _glow_fill((56, 248, 255)), gl, _glow_fill((78, 252, 255)))
    save_pair("dread_nautilus_sentinel", s, gl)

    # 5. Thalassor Boss (512x256)
    s = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    obsidian = _verdigris_metal((22, 32, 48), (44, 68, 92), (10, 16, 26), patina=(36, 112, 116), seed=501)
    turq_scale = _organic_shaded((26, 98, 104), (62, 178, 172), (14, 58, 66), stripe=True, seed=502)

    def rune_carapace(face, lx, ly, fw, fh, sh, gx, gy):
        col = obsidian(face, lx, ly, fw, fh, sh, gx, gy)
        if face in ("left", "right") and (3 <= lx <= fw - 4) and (3 <= ly <= fh - 4):
            if ((lx + ly) % 5 == 0) or (ly == fh // 2 and lx % 2 == 0):
                return (52, 245, 242, 255)
        return col

    def rune_carapace_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("left", "right") and (3 <= lx <= fw - 4) and (3 <= ly <= fh - 4):
            if ((lx + ly) % 5 == 0) or (ly == fh // 2 and lx % 2 == 0):
                return (64, 248, 245, 255)
        return None

    for ub, fn in (
        ((0, 0, 24, 16, 22), obsidian),
        ((94, 0, 18, 9, 20), turq_scale),
        ((172, 0, 16, 6, 18), turq_scale),
        ((60, 40, 4, 12, 4), _verdigris_metal((164, 142, 74), (218, 194, 108), (96, 82, 40), seed=503)),
        ((78, 40, 3, 10, 3), _verdigris_metal((184, 162, 88), (236, 212, 124), (112, 96, 48), seed=504)),
        ((0, 68, 2, 3, 2), _organic_shaded((248, 250, 252), (255, 255, 255), (192, 214, 222))),
        ((10, 68, 18, 4, 14), _organic_shaded((58, 138, 128), (112, 208, 192), (28, 84, 78), stripe=True)),
        ((152, 68, 12, 3, 12), _organic_shaded((58, 138, 128), (112, 208, 192), (28, 84, 78), stripe=True)),
        ((384, 40, 30, 4, 16), _organic_shaded((24, 104, 112), (72, 234, 226), (14, 58, 68), stripe=True)),
        ((94, 68, 16, 3, 12), _organic_shaded((32, 132, 136), (86, 242, 232), (18, 72, 78))),
    ):
        _paint_textured_box(s, ub, fn)

    for seg_uv in ((296, 0, 22, 20, 16), (374, 0, 18, 17, 15), (442, 0, 14, 13, 14)):
        _paint_textured_box(s, seg_uv, rune_carapace, gl, rune_carapace_glow)
    for glow_uv, col in (
        ((242, 0, 12, 8, 14), (72, 248, 242)),
        ((0, 40, 20, 8, 2), (62, 242, 232)),
        ((46, 40, 3, 9, 3), (78, 250, 242)),
        ((76, 68, 2, 4, 6), (86, 252, 255)),
        ((162, 40, 2, 2, 18), (64, 238, 228)),
        ((298, 40, 2, 3, 22), (64, 238, 228)),
    ):
        _paint_textured_box(s, glow_uv, _glow_fill(col), gl, _glow_fill(col))
    for translucent_uv in ((92, 40, 18, 2, 16), (204, 40, 28, 3, 18), (348, 40, 3, 11, 14)):
        _paint_textured_box(s, translucent_uv, _organic_shaded((32, 184, 182), (88, 248, 238), (18, 96, 102), alpha=210, stripe=True), gl, _glow_fill((54, 232, 222), alpha=155))
    save_pair("thalassor", s, gl)

    # 6. Brine-Merchants x3 Professions (256x256 + glow maps)
    profs = {
        "brine_merchant_jeweler": ((166, 114, 44), (34, 82, 112), (178, 232, 248), (64, 236, 248), False),
        "brine_merchant_cartographer": ((44, 108, 104), (32, 92, 98), (52, 242, 255), (224, 210, 162), True),
        "brine_merchant_alchemist": ((48, 108, 44), (34, 96, 84), (56, 245, 255), (86, 245, 242), False),
    }
    for pname, (helm_c, robe_c, visor_c, prop_c, visor_emissive) in profs.items():
        m = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        mgl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        brass = _verdigris_metal((182, 128, 48), (228, 178, 84), (108, 70, 26), patina=(62, 124, 112), seed=601)
        coat = _organic_shaded(robe_c, (min(255, robe_c[0] + 28), min(255, robe_c[1] + 28), min(255, robe_c[2] + 28)), (max(0, robe_c[0] - 18), max(0, robe_c[1] - 18), max(0, robe_c[2] - 18)), stripe=True, seed=602)

        def villager_face(face, lx, ly, fw, fh, sh, gx, gy):
            col = _organic_shaded((186, 142, 112), (208, 166, 134), (144, 104, 78))(face, lx, ly, fw, fh, sh, gx, gy)
            if face == "front":
                if ly in (8, 9) and lx in (3, 4, 11, 12):
                    return (42, 152, 96, 255)
                if ly == 6 and (2 <= lx <= 13):
                    return (104, 72, 48, 255)
            return col

        for ub, fn in (
            ((0, 0, 16, 20, 16), villager_face),
            ((66, 0, 4, 8, 4), _organic_shaded((168, 122, 94), (192, 146, 116), (128, 88, 66))),
            ((84, 0, 20, 20, 20), _verdigris_metal(helm_c, (min(255, helm_c[0] + 42), min(255, helm_c[1] + 42), min(255, helm_c[2] + 42)), (max(0, helm_c[0] - 32), max(0, helm_c[1] - 32), max(0, helm_c[2] - 32)), seed=603)),
            ((166, 0, 14, 14, 3), brass),
            ((228, 0, 3, 7, 7), brass),
            ((0, 42, 20, 4, 14), brass),
            ((70, 42, 21, 22, 21), _organic_shaded((68, 116, 46), (102, 158, 64), (42, 74, 28), stripe=True, spot=True)),
            ((156, 42, 22, 10, 18), _organic_shaded((58, 104, 42), (88, 142, 56), (36, 66, 26), stripe=True)),
            ((0, 62, 5, 5, 4), brass),
            ((34, 62, 4, 7, 6), _organic_shaded((186, 162, 128), (218, 194, 158), (124, 104, 78))),
            ((0, 86, 16, 24, 12), coat),
            ((58, 86, 12, 2, 2), brass),
            ((88, 86, 17, 16, 13), coat),
            ((150, 86, 5, 15, 5), brass),
            ((172, 86, 14, 3, 5), _verdigris_metal((124, 82, 38), (166, 114, 58), (74, 46, 20))),
            ((212, 86, 7, 22, 7), coat),
            ((0, 124, 8, 24, 8), _organic_shaded((62, 46, 36), (88, 68, 54), (38, 28, 22))),
            ((34, 124, 9, 6, 10), brass),
            ((164, 124, 3, 14, 3), brass),
            ((208, 124, 3, 4, 3), _organic_shaded((164, 118, 68), (206, 158, 96), (104, 72, 40))),
            ((222, 124, 2, 22, 2), _organic_shaded((96, 62, 34), (132, 88, 48), (58, 36, 18))),
        ):
            _paint_textured_box(m, ub, fn)

        _paint_textured_box(m, (202, 0, 10, 10, 2), _glow_fill(visor_c, alpha=165 if not visor_emissive else 235), mgl, _glow_fill(visor_c, alpha=90 if not visor_emissive else 230) if visor_emissive else None)
        _paint_textured_box(m, (20, 62, 4, 4, 2), _glow_fill((108, 248, 162)), mgl, _glow_fill((118, 255, 172)))
        _paint_textured_box(m, (74, 124, 4, 6, 4), _glow_fill((64, 245, 255)), mgl, _glow_fill((84, 250, 255)))
        _paint_textured_box(m, (92, 124, 7, 7, 7), _glow_fill(prop_c), mgl, _glow_fill(prop_c))
        _paint_textured_box(m, (122, 124, 18, 12, 2), _glow_fill((78, 242, 248)), mgl, _glow_fill((92, 248, 255)))
        _paint_textured_box(m, (178, 124, 7, 7, 7), _glow_fill((86, 248, 242)), mgl, _glow_fill((96, 252, 248)))
        save_pair(pname, m, mgl)

    # 7. Lumi-Seahorse (256x128)
    s = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    seahorse_jade = _organic_shaded((42, 178, 152), (104, 236, 198), (22, 106, 98), stripe=True, seed=701)
    for ub, fn in (
        ((0, 0, 10, 10, 10), seahorse_jade),
        ((42, 0, 5, 5, 10), seahorse_jade),
        ((74, 0, 4, 4, 3), seahorse_jade),
        ((90, 0, 2, 7, 2), _organic_shaded((238, 188, 62), (255, 228, 118), (168, 118, 28))),
        ((128, 0, 2, 6, 2), _organic_shaded((238, 188, 62), (255, 228, 118), (168, 118, 28))),
        ((0, 22, 10, 8, 10), seahorse_jade),
        ((42, 22, 12, 14, 12), seahorse_jade),
        ((0, 50, 10, 3, 13), _organic_shaded((112, 234, 196), (178, 252, 224), (58, 168, 142))),
        ((128, 50, 9, 3, 13), _organic_shaded((112, 234, 196), (178, 252, 224), (58, 168, 142))),
        ((0, 78, 8, 8, 8), seahorse_jade), ((140, 0, 7, 7, 8), seahorse_jade),
        ((34, 78, 6, 7, 6), seahorse_jade), ((172, 0, 5, 6, 6), seahorse_jade),
        ((60, 78, 4, 6, 4), seahorse_jade), ((196, 0, 3, 5, 4), seahorse_jade),
        ((0, 96, 14, 4, 14), _verdigris_metal((142, 86, 38), (192, 126, 62), (84, 48, 20), seed=702)),
        ((58, 96, 12, 4, 4), _verdigris_metal((186, 134, 52), (232, 182, 86), (112, 74, 26), seed=703)),
        ((92, 96, 2, 10, 3), _organic_shaded((118, 72, 32), (162, 104, 52), (72, 42, 18))),
        ((104, 96, 3, 3, 1), _verdigris_metal((186, 198, 212), (232, 242, 252), (108, 118, 132))),
    ):
        _paint_textured_box(s, ub, fn)
    for ub in ((100, 0, 2, 5, 6), (48, 50, 2, 14, 12), (78, 50, 1, 16, 2), (86, 50, 2, 8, 9)):
        _paint_textured_box(s, ub, _glow_fill((106, 255, 222), alpha=215), gl, _glow_fill((128, 255, 232), alpha=235))
    save_pair("lumi_seahorse", s, gl)

    # 8. Pearl-Pup (128x128)
    s = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    seal_coat = _organic_shaded((238, 244, 248), (252, 254, 255), (176, 194, 210), spot=True, seed=801)
    for ub, fn in (
        ((0, 0, 16, 13, 16), seal_coat),
        ((66, 0, 14, 12, 12), seal_coat),
        ((58, 31, 10, 6, 6), _organic_shaded((172, 186, 198), (212, 222, 232), (118, 132, 146))),
        ((92, 31, 4, 2, 2), _organic_shaded((24, 32, 42), (54, 68, 86), (12, 16, 22))),
        ((58, 45, 5, 1, 1), _organic_shaded((124, 138, 152), (168, 182, 194), (82, 94, 108))),
        ((72, 45, 2, 8, 2), _organic_shaded((148, 188, 198), (192, 226, 236), (96, 134, 146))),
        ((108, 45, 2, 7, 2), _organic_shaded((148, 188, 198), (192, 226, 236), (96, 134, 146))),
        ((0, 60, 11, 9, 10), seal_coat), ((44, 60, 8, 7, 8), seal_coat),
        ((78, 60, 12, 4, 8), seal_coat), ((0, 81, 8, 3, 11), seal_coat), ((40, 81, 8, 3, 6), seal_coat),
    ):
        _paint_textured_box(s, ub, fn)

    def pup_head(face, lx, ly, fw, fh, sh, gx, gy):
        col = seal_coat(face, lx, ly, fw, fh, sh, gx, gy)
        if face == "front" and ly in (4, 5) and lx in (3, 4, 9, 10):
            return (24, 30, 40, 255) if (lx in (3, 9) or ly == 5) else (240, 248, 255, 255)
        return col

    _paint_textured_box(s, (0, 31, 14, 12, 14), pup_head)
    _paint_textured_box(s, (82, 45, 6, 6, 6), _glow_fill((128, 252, 255)), gl, _glow_fill((168, 255, 255)))
    save_pair("pearl_pup", s, gl)

    # 9. Singing Moon-Whale (512x256)
    s = Image.new("RGBA", (512, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (512, 256), (0, 0, 0, 0))

    def whale_body(stars=True):
        def p(face, lx, ly, fw, fh, sh, gx, gy):
            n = _wa.noise(gx, gy, 901)
            if face == "bottom" or (face in ("left", "right") and ly >= fh * 0.72):
                col = (146, 164, 196) if (lx % 2 == 0) else (106, 124, 158)  # pleated ventral belly grooves
            else:
                col = _mix((34, 62, 118), (58, 98, 168), 0.25 if (sh > 0 or n > 0.65) else 0.0)
                if stars and face in ("top", "left", "right") and n > 0.965:
                    return (152, 252, 255, 255)
            return (*col, 255)
        return p

    def whale_body_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("top", "left", "right") and ly < fh * 0.7 and _wa.noise(gx, gy, 901) > 0.965:
            return (162, 252, 255, 255)
        return None

    def whale_fin_rim(face, lx, ly, fw, fh, sh, gx, gy):
        edge = lx <= 1 or lx >= fw - 2 or ly <= 1 or ly >= fh - 2
        if edge:
            return (118, 248, 255, 255)
        return (*_mix((36, 66, 124), (58, 102, 174), 0.25 if _wa.noise(gx, gy, 902) > 0.6 else 0.0), 255)

    def whale_fin_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if lx <= 1 or lx >= fw - 2 or ly <= 1 or ly >= fh - 2:
            return (118, 248, 255, 255)
        return None

    for ub, fn, gfn in (
        ((0, 0, 36, 26, 40), whale_body(), whale_body_glow),
        ((154, 0, 34, 28, 44), whale_body(), whale_body_glow),
        ((312, 0, 32, 12, 36), whale_body(stars=False), None),
        ((312, 50, 26, 22, 26), whale_body(), whale_body_glow),
        ((418, 50, 18, 16, 24), whale_body(), whale_body_glow),
        ((226, 74, 12, 12, 22), whale_body(), whale_body_glow),
        ((130, 74, 14, 4, 14), whale_body(), None),
        ((188, 74, 4, 10, 14), whale_fin_rim, whale_fin_glow),
        ((0, 74, 30, 8, 34), lambda f, lx, ly, fw, fh, sh, gx, gy: (232, 214, 152, 255) if (lx % 2 == 0) else (156, 134, 88, 255), None),
        ((0, 120, 34, 4, 16), whale_fin_rim, whale_fin_glow),
        ((102, 120, 24, 3, 12), whale_fin_rim, whale_fin_glow),
        ((176, 120, 16, 2, 8), whale_fin_rim, whale_fin_glow),
        ((226, 120, 44, 4, 18), whale_fin_rim, whale_fin_glow),
        ((352, 120, 22, 3, 12), whale_fin_rim, whale_fin_glow),
        ((0, 144, 6, 3, 6), _glow_fill((146, 252, 255)), _glow_fill((182, 255, 255))),
        ((26, 144, 2, 4, 4), _organic_shaded((24, 38, 68), (48, 76, 118), (12, 22, 40)), None),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("singing_moon_whale", s, gl)



def _write_entity_loot(g):
    def drop(item_id, lo=1, hi=1, chance=None):
        entry = {"type": "minecraft:item", "name": item_id}
        fns = []
        if lo != 1 or hi != 1:
            fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        if fns:
            entry["functions"] = fns
        pool = {"rolls": 1, "entries": [entry]}
        if chance is not None:
            pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        return pool

    tables = {
        "barnacle_hulk_dredger": [drop("wildercord:abyssal_iron_plate", 1, 2), drop("minecraft:prismarine_shard", 1, 3), drop("wildercord:coral_edge_fang", 1, 1, 0.35)],
        "siren_veil_jelly": [drop("wildercord:voltaic_jelly_sac", 1, 2), drop("minecraft:glow_ink_sac", 1, 1)],
        "coral_spine_mako": [drop("wildercord:coral_edge_fang", 1, 2), drop("minecraft:cod", 1, 2)],
        "dread_nautilus_sentinel": [drop("minecraft:nautilus_shell", 1, 1), drop("minecraft:copper_ingot", 1, 2), drop("wildercord:abyssal_pearl", 1, 1, 0.5)],
        "thalassor": [drop("wildercord:leviathan_heart_pearl", 2, 2), drop("wildercord:abyssal_pearl", 3, 5), drop("wildercord:abyssal_iron_plate", 4, 4)],
        "singing_moon_whale": [drop("wildercord:star_barnacle_ambergris", 1, 2)],
        "pearl_pup": [drop("minecraft:cod", 1, 2)],
        "lumi_seahorse": [drop("minecraft:prismarine_crystals", 1, 2)],
    }
    for eid, pools in tables.items():
        g.write_json(g.DATA / f"loot_table/entities/{eid}.json", {"type": "minecraft:entity", "pools": pools})


def _write_recipes_and_advancements(g):
    # Direct crafting recipes (Trench Anvil handles armored/weapon forging in hydro-forges)
    recipes = {
        "kelp_wrapped_sashimi": {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": ["minecraft:dried_kelp", "minecraft:salmon", "minecraft:cod"],
            "result": {"id": "wildercord:kelp_wrapped_sashimi", "count": 2},
            "trigger": "minecraft:salmon",
        },
        "sea_salt_ramen": {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": ["minecraft:bowl", "minecraft:dried_kelp", "minecraft:cooked_salmon", "minecraft:egg", "wildercord:star_barnacle_ambergris"],
            "result": {"id": "wildercord:sea_salt_ramen", "count": 1},
            "trigger": "wildercord:star_barnacle_ambergris",
        },
        "siren_pearl_lantern": {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "pattern": [" C ", "VPV", " C "],
            "key": {"C": "minecraft:copper_ingot", "V": "wildercord:voltaic_jelly_sac", "P": "wildercord:abyssal_pearl"},
            "result": {"id": "wildercord:siren_pearl_lantern", "count": 1},
            "trigger": "wildercord:voltaic_jelly_sac",
        },
        "trench_anvil": {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "pattern": ["BBB", " A ", "DDD"],
            "key": {"B": "wildercord:abyssal_iron_plate", "A": "minecraft:anvil", "D": "minecraft:polished_blackstone_bricks"},
            "result": {"id": "wildercord:trench_anvil", "count": 1},
            "trigger": "wildercord:abyssal_iron_plate",
        },
    }
    for rid, data in recipes.items():
        trig = data.pop("trigger")
        g.write_json(g.DATA / f"recipe/{rid}.json", data)
        g.unlock_advancement(f"wildercord:{rid}", trig)

    # 5 Deep Ocean progression advancements
    g.adv(
        "ocean/deep_diver", "world/ley_line", g.item("wildercord:abyssal_diving_helm"),
        LANG["advancements.wildercord.ocean.deep_diver.title"],
        LANG["advancements.wildercord.ocean.deep_diver.description"],
        {
            "helm": g.has_item("wildercord:abyssal_diving_helm"),
            "chest": g.has_item("wildercord:bathysphere_chestplate"),
            "legs": g.has_item("wildercord:kelp_weave_greaves"),
            "boots": g.has_item("wildercord:trench_boots"),
        },
        any_of=True, xp=25,
    )
    g.adv(
        "ocean/trench_forged", "ocean/deep_diver", g.item("wildercord:leviathan_glaive"),
        LANG["advancements.wildercord.ocean.trench_forged.title"],
        LANG["advancements.wildercord.ocean.trench_forged.description"],
        {
            "harpoon": g.has_item("wildercord:tide_cleaver_harpoon"),
            "glaive": g.has_item("wildercord:leviathan_glaive"),
        },
        any_of=True, xp=25,
    )
    def entity_cond(entity_id):
        if g.profile.is_26_2:
            return [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity_id}}]
        return {"type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": entity_id}}

    g.adv(
        "ocean/sanctuary_trader", "ocean/deep_diver", g.item("wildercord:abyssal_pearl"),
        LANG["advancements.wildercord.ocean.sanctuary_trader.title"],
        LANG["advancements.wildercord.ocean.sanctuary_trader.description"],
        {"trigger": "minecraft:villager_trade", "conditions": {"villager": entity_cond("wildercord:brine_merchant")}},
        xp=20,
    )
    g.adv(
        "ocean/conch_shelter", "ocean/deep_diver", g.item("wildercord:tidecaller_conch"),
        LANG["advancements.wildercord.ocean.conch_shelter.title"],
        LANG["advancements.wildercord.ocean.conch_shelter.description"],
        g.has_item("wildercord:tidecaller_conch"), xp=20,
    )
    g.adv(
        "ocean/sovereign_slayer", "ocean/trench_forged", g.item("wildercord:leviathan_heart_pearl"),
        LANG["advancements.wildercord.ocean.sovereign_slayer.title"],
        LANG["advancements.wildercord.ocean.sovereign_slayer.description"],
        {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": entity_cond("wildercord:thalassor")}},
        frame="goal", xp=100,
    )


def write(g):
    icons = _build_icons()
    for name, img in icons.items():
        g.save(img, g.ASSETS / f"textures/item/{name}.png")

    # Plain flat-icon items
    plain_items = (
        "abyssal_diving_helm", "bathysphere_chestplate", "kelp_weave_greaves", "trench_boots",
        "abyssal_pearl", "leviathan_heart_pearl", "coral_edge_fang", "voltaic_jelly_sac",
        "abyssal_iron_plate", "star_barnacle_ambergris", "kelp_wrapped_sashimi", "sea_salt_ramen",
    )
    for iid in plain_items:
        g.item_model(iid, iid)
        g.write_json(g.ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{iid}"}})

    # Spawn eggs (9 species)
    eggs = {
        "barnacle_hulk_dredger": ((46, 84, 78), (242, 124, 154)),
        "siren_veil_jelly": ((74, 58, 186), (112, 248, 255)),
        "coral_spine_mako": ((28, 72, 136), (252, 116, 138)),
        "dread_nautilus_sentinel": ((56, 124, 112), (202, 148, 62)),
        "thalassor": ((24, 34, 48), (54, 240, 232)),
        "brine_merchant": ((34, 72, 118), (186, 134, 56)),
        "lumi_seahorse": ((48, 182, 198), (92, 255, 226)),
        "pearl_pup": ((228, 236, 244), (112, 252, 255)),
        "singing_moon_whale": ((34, 62, 118), (255, 226, 86)),
    }
    for eid, (c1, c2) in eggs.items():
        egg_id = f"{eid}_spawn_egg"
        g.save(_egg_icon(c1, c2), g.ASSETS / f"textures/item/{egg_id}.png")
        g.item_model(egg_id, egg_id)
        g.write_json(g.ASSETS / f"items/{egg_id}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg_id}"}})

    _build_3d_weapon_models(g)
    _build_blocks_and_armor_layers(g)
    _build_entity_skins(g)
    _write_entity_loot(g)
    _write_recipes_and_advancements(g)
