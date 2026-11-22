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


def _elem(x0, y0, z0, x1, y1, z1, tex, uv=None, light_emission=0):
    if uv is None:
        u0 = max(0.0, min(15.5, x0))
        v0 = max(0.0, min(15.5, 16.0 - y1))
        u1 = max(u0 + 0.5, min(16.0, x1))
        v1 = max(v0 + 0.5, min(16.0, 16.0 - y0))
        uv = [u0, v0, u1, v1]
    face = {"texture": f"#{tex}", "uv": uv}
    out = {
        "from": [x0, y0, z0],
        "to": [x1, y1, z1],
        "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")},
    }
    if light_emission > 0:
        out["light_emission"] = light_emission
    return out


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
        "tide_cleaver_harpoon_rune": _shade_tile((64, 236, 248), (218, 255, 255), (20, 142, 162), accent=(180, 255, 255), mode="pearl", seed=14),
        "leviathan_glaive_blade": _shade_tile((72, 224, 218), (192, 255, 255), (24, 134, 136), accent=(232, 255, 255), mode="pearl", seed=21),
        "leviathan_glaive_gold": _shade_tile((212, 162, 58), (255, 224, 128), (134, 92, 26), mode="metal", seed=22),
        "leviathan_glaive_pearl": _shade_tile((64, 240, 255), (220, 255, 255), (24, 148, 182), accent=(180, 255, 255), mode="pearl", seed=23),
        "leviathan_glaive_haft": _shade_tile((22, 86, 92), (48, 148, 152), (12, 46, 52), mode="metal", seed=24),
        "tidecaller_conch_shell": _shade_tile((244, 226, 194), (255, 248, 234), (176, 144, 108), mode="shell", seed=31),
        "tidecaller_conch_pink": _shade_tile((242, 162, 158), (255, 206, 198), (178, 92, 96), mode="shell", seed=32),
    }
    for name, img in tiles.items():
        g.save(img, g.ASSETS / f"textures/block/{name}.png")

    # 1. Tide-Cleaver Harpoon 3D (27 elements: brass loop pommel, wrapped shaft, crescent horns, barbed tip + glowing rune fuller & chain)
    harpoon_in_hand = {
        "textures": {
            "steel": "wildercord:block/tide_cleaver_harpoon_steel",
            "brass": "wildercord:block/tide_cleaver_harpoon_brass",
            "teal": "wildercord:block/tide_cleaver_harpoon_teal",
            "rune": "wildercord:block/tide_cleaver_harpoon_rune",
            "particle": "wildercord:block/tide_cleaver_harpoon_steel",
        },
        "elements": [
            # Pommel bronze ring loop + cyan rune stud
            _elem(6.8, -11.6, 7.2, 9.2, -10.8, 8.8, "brass", [2, 2, 8, 5]),
            _elem(6.4, -10.8, 7.2, 7.3, -9.2, 8.8, "brass", [2, 2, 5, 5]),
            _elem(8.7, -10.8, 7.2, 9.6, -9.2, 8.8, "brass", [2, 2, 5, 5]),
            _elem(6.8, -9.2, 7.2, 9.2, -8.4, 8.8, "brass", [2, 2, 8, 5]),
            _elem(7.4, -10.4, 7.1, 8.6, -9.4, 8.9, "rune", [2, 2, 14, 14], light_emission=12),
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
            # Tapered 6-step crucible-silver barbed harpoon spearhead & twin backward barbs + glowing cyan rune fuller
            _elem(7.1, 15.0, 7.3, 8.9, 24.5, 8.7, "steel"),
            _elem(6.3, 17.5, 7.4, 9.7, 22.8, 8.6, "steel"),
            _elem(5.4, 17.8, 7.5, 10.6, 21.0, 8.5, "steel"),
            _elem(7.5, 24.5, 7.6, 8.5, 27.2, 8.4, "steel"),
            _elem(7.6, 16.2, 7.1, 8.4, 23.6, 8.9, "rune", [2, 2, 14, 14], light_emission=12),
            _elem(4.6, 16.0, 7.5, 5.6, 18.8, 8.5, "steel"),
            _elem(10.4, 16.0, 7.5, 11.4, 18.8, 8.5, "steel"),
            _elem(4.1, 15.0, 7.6, 4.9, 17.0, 8.4, "steel"),
            _elem(11.1, 15.0, 7.6, 11.9, 17.0, 8.4, "steel"),
            # Trailing catenary iron retrieval chain loop along shaft side
            _elem(8.8, 12.2, 7.5, 11.2, 13.4, 8.5, "steel"),
            _elem(10.6, 7.6, 7.5, 11.8, 12.4, 8.5, "steel"),
            _elem(9.4, 4.2, 7.5, 11.2, 7.8, 8.5, "steel"),
            _elem(8.6, 1.2, 7.5, 10.2, 4.6, 8.5, "steel"),
        ],
        "display": _display_poses(),
    }
    g.write_json(g.ASSETS / "models/item/tide_cleaver_harpoon_in_hand.json", harpoon_in_hand)

    # 2. Leviathan Glaive 3D (25 elements: sea-fin crossguard, glowing Heart-Pearl, rune spine, crescent blade)
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
            _elem(3.2, 13.8, 7.4, 4.6, 15.8, 8.6, "blade", light_emission=10),
            _elem(11.4, 13.8, 7.4, 12.8, 15.8, 8.6, "blade", light_emission=10),
            _elem(2.5, 14.8, 7.5, 3.6, 16.8, 8.5, "blade"),
            _elem(12.4, 14.8, 7.5, 13.5, 16.8, 8.5, "blade"),
            # Dual-sided faceted glowing Leviathan Heart-Pearl orb + inner bezel
            _elem(6.3, 12.6, 6.0, 9.7, 16.0, 10.0, "pearl", [0, 0, 16, 16], light_emission=15),
            _elem(6.8, 13.1, 5.5, 9.2, 15.5, 10.5, "pearl", [2, 2, 14, 14], light_emission=15),
            _elem(6.8, 15.8, 6.9, 9.2, 16.8, 9.1, "gold"),
            # Towering stepped sweeping curved crescent blade + emissive rune spine
            _elem(7.1, 16.6, 7.4, 9.3, 19.6, 8.6, "blade"),
            _elem(6.8, 19.2, 7.4, 9.7, 23.0, 8.6, "blade"),
            _elem(6.2, 22.4, 7.5, 9.5, 26.2, 8.5, "blade"),
            _elem(5.4, 25.4, 7.5, 8.8, 28.8, 8.5, "blade"),
            _elem(4.6, 28.0, 7.6, 7.6, 30.8, 8.4, "blade"),
            _elem(3.8, 30.0, 7.7, 6.2, 31.8, 8.3, "blade"),
            _elem(7.8, 17.2, 7.2, 8.6, 25.4, 8.8, "pearl", [2, 2, 14, 14], light_emission=12),
            _elem(7.0, 25.0, 7.2, 7.8, 28.6, 8.8, "pearl", [2, 2, 14, 14], light_emission=12),
            _elem(8.9, 16.6, 7.3, 9.9, 24.5, 8.7, "gold"),
            _elem(8.4, 24.0, 7.3, 9.4, 27.6, 8.7, "gold"),
        ],
        "display": _display_poses(),
    }
    g.write_json(g.ASSETS / "models/item/leviathan_glaive_in_hand.json", glaive_in_hand)

    # 3. Tidecaller Conch 3D (22 sculpted elements: 7-whorl spiral, dorsal nodules, rosy flaring inner lip, glowing suture ring)
    conch_in_hand = {
        "textures": {
            "shell": "wildercord:block/tidecaller_conch_shell",
            "pink": "wildercord:block/tidecaller_conch_pink",
            "pearl": "wildercord:block/leviathan_glaive_pearl",
            "particle": "wildercord:block/tidecaller_conch_shell",
        },
        "elements": [
            # 7-whorl stepped spiral conch cone from broad chamber down to pointed apex
            _elem(5.2, 5.0, 4.6, 11.4, 11.8, 11.4, "shell"),
            _elem(3.4, 5.6, 5.2, 5.4, 11.2, 10.8, "shell"),
            _elem(1.8, 6.2, 5.8, 3.6, 10.6, 10.2, "shell"),
            _elem(0.4, 6.8, 6.4, 2.0, 10.0, 9.6, "shell"),
            _elem(-0.8, 7.3, 6.9, 0.6, 9.5, 9.1, "shell"),
            _elem(-1.8, 7.8, 7.3, -0.6, 9.1, 8.7, "shell"),
            _elem(-2.6, 8.1, 7.6, -1.6, 8.8, 8.4, "shell"),
            # Flaring rosy-peach inner lip aperture (top/bottom/front/back rim)
            _elem(9.6, 3.6, 4.0, 13.8, 11.2, 11.2, "pink"),
            _elem(11.4, 3.8, 4.6, 14.8, 9.8, 10.8, "pink"),
            _elem(7.4, 4.2, 3.8, 11.8, 10.6, 5.0, "pink"),
            _elem(7.4, 4.2, 10.6, 11.8, 10.6, 12.2, "pink"),
            _elem(6.2, 11.4, 5.6, 11.6, 12.6, 10.6, "shell"),
            _elem(7.0, 3.8, 5.4, 12.0, 5.0, 10.6, "shell"),
            # Raised dorsal spiral nodules along upper crown
            _elem(8.4, 12.4, 7.2, 9.6, 13.5, 8.8, "shell"),
            _elem(6.0, 12.0, 6.6, 7.2, 13.0, 9.4, "shell"),
            _elem(3.8, 11.0, 6.8, 4.8, 12.1, 9.2, "shell"),
            _elem(2.0, 10.2, 7.0, 2.8, 11.2, 9.0, "shell"),
            # Glowing pearl suture ring & mouthpiece collar
            _elem(5.0, 4.7, 4.3, 5.6, 12.1, 11.7, "pearl", [2, 2, 14, 14], light_emission=10),
            _elem(7.8, 4.7, 4.3, 8.4, 12.1, 11.7, "pearl", [2, 2, 14, 14], light_emission=10),
            _elem(10.6, 4.6, 4.4, 11.2, 11.4, 11.6, "pearl", [2, 2, 14, 14], light_emission=10),
            _elem(-3.2, 7.9, 7.4, -2.4, 9.0, 8.6, "pearl", [2, 2, 14, 14], light_emission=10),
            _elem(8.4, 5.2, 11.8, 10.6, 10.2, 12.6, "shell"),
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

    # Siren-Pearl Lantern block (22 elements: stepped pedestal, corner pillars + studs, crossbars, pagoda crown, emissive pearl core)
    lantern_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "cage": "wildercord:block/siren_pearl_cage",
            "core": "wildercord:block/siren_pearl_core",
            "particle": "wildercord:block/siren_pearl_cage",
        },
        "elements": [
            _elem(2.5, 0.0, 2.5, 13.5, 1.2, 13.5, "cage"),
            _elem(3.6, 1.2, 3.6, 12.4, 2.2, 12.4, "cage"),
            _elem(3.6, 8.2, 3.6, 12.4, 9.4, 12.4, "cage"),
            _elem(4.8, 9.4, 4.8, 11.2, 10.6, 11.2, "cage"),
            _elem(6.2, 10.6, 6.2, 9.8, 11.5, 9.8, "cage"),
            _elem(7.2, 11.5, 7.2, 8.8, 13.2, 8.8, "cage"),
            _elem(3.8, 2.2, 3.8, 4.8, 8.2, 4.8, "cage"),
            _elem(11.2, 2.2, 3.8, 12.2, 8.2, 4.8, "cage"),
            _elem(3.8, 2.2, 11.2, 4.8, 8.2, 12.2, "cage"),
            _elem(11.2, 2.2, 11.2, 12.2, 8.2, 12.2, "cage"),
            # Corner pagoda roof studs
            _elem(3.2, 8.8, 3.2, 4.4, 10.0, 4.4, "cage"),
            _elem(11.6, 8.8, 3.2, 12.8, 10.0, 4.4, "cage"),
            _elem(3.2, 8.8, 11.6, 4.4, 10.0, 12.8, "cage"),
            _elem(11.6, 8.8, 11.6, 12.8, 10.0, 12.8, "cage"),
            # Cage crossbars
            _elem(4.8, 4.8, 4.0, 11.2, 5.6, 4.6, "cage"),
            _elem(4.8, 4.8, 11.4, 11.2, 5.6, 12.0, "cage"),
            _elem(4.0, 4.8, 4.8, 4.6, 5.6, 11.2, "cage"),
            _elem(11.4, 4.8, 4.8, 12.0, 5.6, 11.2, "cage"),
            # Floating faceted glowing Magenta-Cyan Siren Pearl inside cage
            _elem(5.4, 2.8, 5.4, 10.6, 7.6, 10.6, "core", [0, 0, 16, 16], light_emission=15),
            _elem(6.0, 2.2, 6.0, 10.0, 8.2, 10.0, "core", [2, 2, 14, 14], light_emission=15),
            _elem(6.4, 3.4, 4.9, 9.6, 7.0, 11.1, "core", [2, 2, 14, 14], light_emission=15),
            _elem(4.9, 3.4, 6.4, 11.1, 7.0, 9.6, "core", [2, 2, 14, 14], light_emission=15),
        ],
    }
    g.write_json(g.ASSETS / "models/block/siren_pearl_lantern.json", lantern_model)
    g.write_json(g.ASSETS / "blockstates/siren_pearl_lantern.json", {"variants": {"": {"model": "wildercord:block/siren_pearl_lantern"}}})
    g.write_json(g.ASSETS / "items/siren_pearl_lantern.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/siren_pearl_lantern"}})

    # Trench Anvil block (19 elements: basalt plinth steps, molten glowing core, 4 riveted brass struts, horn & glowing forging channel)
    anvil_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "anvil": "wildercord:block/trench_anvil",
            "brass": "wildercord:block/siren_pearl_cage",
            "lava": "wildercord:block/trench_anvil_lava",
            "particle": "wildercord:block/trench_anvil",
        },
        "elements": [
            _elem(1.2, 0.0, 1.2, 14.8, 2.4, 14.8, "anvil"),
            _elem(2.6, 2.4, 2.6, 13.4, 4.4, 13.4, "anvil"),
            _elem(4.2, 4.4, 4.6, 11.8, 9.8, 11.4, "lava", [0, 0, 16, 16], light_emission=15),
            # 4 Corner Brass Struts & Riveted Collar framing molten waist
            _elem(3.4, 4.4, 4.0, 4.6, 9.8, 5.4, "brass"),
            _elem(11.4, 4.4, 4.0, 12.6, 9.8, 5.4, "brass"),
            _elem(3.4, 4.4, 10.6, 4.6, 9.8, 12.0, "brass"),
            _elem(11.4, 4.4, 10.6, 12.6, 9.8, 12.0, "brass"),
            _elem(3.6, 6.4, 3.8, 12.4, 7.6, 4.6, "brass"),
            _elem(3.6, 6.4, 11.4, 12.4, 7.6, 12.2, "brass"),
            _elem(3.8, 9.8, 3.0, 12.2, 11.4, 13.0, "anvil"),
            _elem(2.0, 11.2, 2.4, 14.2, 15.2, 13.6, "anvil"),
            # Stepped Horn & Heel + Brass Side Rivets
            _elem(-0.4, 12.0, 4.4, 2.2, 15.0, 11.6, "anvil"),
            _elem(-2.2, 12.8, 6.0, -0.4, 14.6, 10.0, "lava", [2, 2, 14, 14], light_emission=15),
            _elem(14.0, 11.8, 3.8, 15.8, 15.0, 12.2, "anvil"),
            _elem(3.0, 12.6, 2.0, 4.2, 13.8, 2.5, "brass"),
            _elem(12.0, 12.6, 2.0, 13.2, 13.8, 2.5, "brass"),
            _elem(3.0, 12.6, 13.5, 4.2, 13.8, 14.0, "brass"),
            _elem(12.0, 12.6, 13.5, 13.2, 13.8, 14.0, "brass"),
            # Glowing lava forging channel inlaid flush atop anvil face
            _elem(3.2, 15.2, 5.6, 13.0, 16.0, 10.4, "lava", [0, 0, 16, 16], light_emission=15),
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

    # Sovereign Altar block (19 elements: corner buttressed prismarine plinth, gold bezel, 4 lit pylons, floating Sovereign Crystal)
    altar_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "altar": "wildercord:block/sovereign_altar",
            "gold": "wildercord:block/leviathan_glaive_gold",
            "core": "wildercord:block/leviathan_glaive_pearl",
            "particle": "wildercord:block/sovereign_altar",
        },
        "elements": [
            _elem(0.0, 0.0, 0.0, 16.0, 5.5, 16.0, "altar"),
            _elem(0.5, 5.5, 0.5, 15.5, 7.2, 15.5, "gold"),
            _elem(1.4, 7.2, 1.4, 14.6, 11.0, 14.6, "altar"),
            # 4 Corner Stepped Buttress Plinths
            _elem(0.0, 5.5, 0.0, 3.0, 9.0, 3.0, "altar"),
            _elem(13.0, 5.5, 0.0, 16.0, 9.0, 3.0, "altar"),
            _elem(0.0, 5.5, 13.0, 3.0, 9.0, 16.0, "altar"),
            _elem(13.0, 5.5, 13.0, 16.0, 9.0, 16.0, "altar"),
            # 4 Corner Obsidian-Gold Runic Pylons + Glowing Tips
            _elem(1.0, 11.0, 1.0, 3.4, 15.0, 3.4, "altar"),
            _elem(12.6, 11.0, 1.0, 15.0, 15.0, 3.4, "altar"),
            _elem(1.0, 11.0, 12.6, 3.4, 15.0, 15.0, "altar"),
            _elem(12.6, 11.0, 12.6, 15.0, 15.0, 15.0, "altar"),
            _elem(1.3, 14.8, 1.3, 3.1, 16.2, 3.1, "core", [2, 2, 14, 14], light_emission=15),
            _elem(12.9, 14.8, 1.3, 14.7, 16.2, 3.1, "core", [2, 2, 14, 14], light_emission=15),
            _elem(1.3, 14.8, 12.9, 3.1, 16.2, 14.7, "core", [2, 2, 14, 14], light_emission=15),
            _elem(12.9, 14.8, 12.9, 14.7, 16.2, 14.7, "core", [2, 2, 14, 14], light_emission=15),
            # Central recessed golden basin & levitating 3-part faceted Cyan Sovereign Crystal
            _elem(4.2, 11.0, 4.2, 11.8, 12.2, 11.8, "gold"),
            _elem(5.4, 12.6, 5.4, 10.6, 16.4, 10.6, "core", [0, 0, 16, 16], light_emission=15),
            _elem(6.3, 16.4, 6.3, 9.7, 18.0, 9.7, "core", [2, 2, 14, 14], light_emission=15),
            _elem(6.3, 11.6, 6.3, 9.7, 12.7, 9.7, "core", [2, 2, 14, 14], light_emission=15),
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
                    col = (188, 134, 54)
                elif ly <= 1:
                    col = (132, 82, 38)
                px_l[x0 + lx, y0 + ly] = (*col, 255)
    g.save(legs, g.ASSETS / "textures/entity/equipment/humanoid_leggings/abyssal_diving.png")

    # 3D Full 6-Limb Abyssal Diving Suit (256x256 sheet matching AbyssalDivingSuitLayer 1:1)
    layer3d = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    glow3d = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    B_BASE, B_HI, B_LO = (182, 128, 48), (232, 184, 86), (108, 70, 24)
    TEAL_M = _verdigris_metal((30, 98, 104), (62, 162, 166), (16, 56, 62), patina=(142, 104, 46), seed=81)
    BRASS_M = _verdigris_metal(B_BASE, B_HI, B_LO, patina=(58, 124, 114), seed=82)
    DARK_BR = _verdigris_metal((146, 96, 38), B_HI, B_LO, patina=(62, 120, 108), seed=83)

    def suit_helm_dome(face, lx, ly, fw, fh, sh, gx, gy):
        if face == "front" and (5 <= lx < 15) and (5 <= ly < 15):
            return (0, 0, 0, 0)
        return DARK_BR(face, lx, ly, fw, fh, sh, gx, gy)

    def suit_helm_bezel(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("front", "back") and (2 <= lx < 12) and (2 <= ly < 12):
            if (lx, ly) not in ((2, 2), (11, 2), (2, 11), (11, 11)):
                return (0, 0, 0, 0)
        return BRASS_M(face, lx, ly, fw, fh, sh, gx, gy)

    def suit_helm_glass(face, lx, ly, fw, fh, sh, gx, gy):
        if lx in (0, fw - 1) or ly in (0, fh - 1):
            return (156, 248, 255, 190)
        if lx - ly in (1, 2) and 1 <= lx <= 4:
            return (236, 255, 255, 175)
        return (56, 218, 242, 105)

    def suit_helm_glass_glow(face, lx, ly, fw, fh, sh, gx, gy):
        # Corner/rim HUD ticks only so center faceplate stays crisp and unobstructed
        if face == "front" and (
            (lx in (0, fw - 1) and (ly <= 2 or ly >= fh - 3))
            or (ly in (0, fh - 1) and (lx <= 2 or lx >= fw - 3))
        ):
            return (92, 248, 255, 185)
        return None

    def suit_cuirass(face, lx, ly, fw, fh, sh, gx, gy):
        col = TEAL_M(face, lx, ly, fw, fh, sh, gx, gy)
        if face in ("front", "back") and ly in (3, 7, 11, 15):
            return (*B_HI, 255)
        if face == "front" and lx in (8, 9) and 2 <= ly <= 16:
            return (*B_BASE, 255)
        return col

    def suit_tank(face, lx, ly, fw, fh, sh, gx, gy):
        col = BRASS_M(face, lx, ly, fw, fh, sh, gx, gy)
        if face not in ("top", "bottom") and ly % 4 == 0:
            return (*B_HI, 255)
        return col

    def suit_gauge(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("front", "back"):
            cx, cy = (fw - 1) * 0.5, (fh - 1) * 0.5
            if (lx - cx) ** 2 + (ly - cy) ** 2 <= (fw * 0.36) ** 2:
                return (88, 250, 255, 255)
        return BRASS_M(face, lx, ly, fw, fh, sh, gx, gy)

    def suit_gauge_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("front", "back"):
            cx, cy = (fw - 1) * 0.5, (fh - 1) * 0.5
            if (lx - cx) ** 2 + (ly - cy) ** 2 <= (fw * 0.36) ** 2:
                return (96, 252, 255, 255)
        return None

    def suit_greave(face, lx, ly, fw, fh, sh, gx, gy):
        weave = (lx + ly) % 2 == 0
        c = (32, 146, 94) if weave else (20, 96, 62)
        if ly <= 1:
            c = (128, 78, 34)
        elif lx in (0, fw - 1) or ly == fh - 1:
            c = (16, 64, 44)
        return (*c, 255)

    def suit_boot(face, lx, ly, fw, fh, sh, gx, gy):
        if ly >= fh - 3 or face == "bottom":
            return (*_mix((36, 56, 64), (58, 86, 94), 0.25 if _wa.noise(gx, gy, 88) > 0.5 else 0.0), 255)
        return DARK_BR(face, lx, ly, fw, fh, sh, gx, gy)

    def suit_arm_gauntlet_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face not in ("top", "bottom") and ly == 2 and (1 <= lx <= fw - 2):
            return (64, 242, 248, 255)
        return None

    for ub, fn, gfn in (
        ((0, 0, 20, 20, 20), suit_helm_dome, None),
        ((0, 66, 14, 4, 14), BRASS_M, None),
        ((222, 66, 6, 3, 6), BRASS_M, None),
        ((142, 0, 22, 4, 22), DARK_BR, None),
        ((143, 66, 14, 14, 3), suit_helm_bezel, None),
        ((178, 66, 3, 7, 7), BRASS_M, None),
        ((0, 85, 6, 6, 3), BRASS_M, None),
        ((199, 66, 10, 10, 1), suit_helm_glass, suit_helm_glass_glow),
        ((81, 0, 18, 20, 12), suit_cuirass, None),
        ((152, 41, 20, 4, 14), _verdigris_metal((122, 74, 34), (168, 112, 54), (74, 42, 18), patina=(96, 62, 30), seed=84), None),
        ((106, 85, 5, 5, 2), suit_gauge, suit_gauge_glow),
        ((90, 41, 6, 16, 6), suit_tank, None),
        ((19, 85, 14, 3, 5), _verdigris_metal((112, 124, 136), (166, 182, 196), (68, 78, 88), seed=85), None),
        ((121, 85, 4, 4, 2), suit_gauge, suit_gauge_glow),
        ((57, 66, 11, 7, 11), BRASS_M, None),
        ((115, 41, 9, 12, 9), TEAL_M, None),
        ((102, 66, 10, 8, 10), DARK_BR, suit_arm_gauntlet_glow),
        ((0, 41, 10, 14, 10), suit_greave, None),
        ((58, 85, 8, 5, 3), BRASS_M, None),
        ((41, 41, 11, 10, 13), suit_boot, None),
        ((81, 85, 9, 4, 3), BRASS_M, None),
    ):
        _paint_textured_box(layer3d, ub, fn, glow3d, gfn)
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


def _glow_fill(color, alpha=255, core=(232, 255, 255)):
    def p(face, lx, ly, fw, fh, sh, gx, gy):
        cx, cy = (fw - 1) * 0.5, (fh - 1) * 0.5
        d = ((lx - cx) ** 2 + (ly - cy) ** 2) ** 0.5
        rim = max(1.0, min(fw, fh) * 0.5)
        col = _mix(core, color, min(1.0, d / rim))
        return (*col, alpha)
    return p


_RUNE_GLYPHS = (
    # 5 hand-crafted 7x9 symmetric abyssal runes
    [
        "...X...",
        "..X.X..",
        ".X...X.",
        "X..X..X",
        ".XX.XX.",
        "...X...",
        "..X.X..",
        ".X...X.",
        "...X...",
    ],
    [
        ".XXXXX.",
        "X..X..X",
        ".X.X.X.",
        "..XXX..",
        "XXX.XXX",
        "..XXX..",
        ".X.X.X.",
        "X..X..X",
        ".XXXXX.",
    ],
    [
        "..XXX..",
        ".X...X.",
        "X..X..X",
        "X.XXX.X",
        ".X.X.X.",
        "...X...",
        "..XXX..",
        ".X...X.",
        "XX...XX",
    ],
    [
        "X.....X",
        ".X...X.",
        "..X.X..",
        ".XXXXX.",
        "...X...",
        ".XX.XX.",
        "X..X..X",
        ".X...X.",
        "..XXX..",
    ],
    [
        "...X...",
        ".XX.XX.",
        "X..X..X",
        "XX.X.XX",
        ".X...X.",
        "XX.X.XX",
        "X..X..X",
        ".XX.XX.",
        "...X...",
    ],
)


def _build_entity_skins(g):
    def save_pair(name, skin, glow=None):
        g.save(skin, g.ASSETS / f"textures/entity/ocean/{name}.png")
        if glow is not None:
            g.save(glow, g.ASSETS / f"textures/entity/ocean/{name}_glow.png")

    # 1. Barnacle-Hulk Dredger (256x256, 34 boxes matching BarnacleHulkModel verbatim)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    verdigris_suit = _verdigris_metal((46, 108, 92), (82, 162, 138), (22, 60, 52), patina=(142, 104, 56), seed=101)
    aged_bronze = _verdigris_metal((184, 128, 50), (232, 182, 86), (108, 70, 24), patina=(58, 128, 114), seed=102)
    steel_iron = _verdigris_metal((92, 104, 118), (146, 162, 178), (48, 56, 66), patina=(64, 88, 92), seed=103)
    leather_belt = _verdigris_metal((112, 72, 34), (154, 104, 52), (66, 40, 16), seed=104)
    barnacle_sh = _organic_shaded((232, 228, 216), (252, 250, 244), (154, 150, 140), spot=True, seed=105)
    coral_pink = _organic_shaded((244, 112, 148), (255, 176, 198), (174, 56, 92), seed=106)
    furnace_col = _glow_fill((255, 148, 28), core=(255, 244, 168))
    furnace_glow = _glow_fill((255, 162, 38), core=(255, 248, 186))

    def hulk_chest(face, lx, ly, fw, fh, sh, gx, gy):
        c = verdigris_suit(face, lx, ly, fw, fh, sh, gx, gy)
        # Diagonal bronze chest baldric + sternal bronze plate trim
        if face in ("front", "back") and abs((lx * 20) // max(1, fw) - ly) <= 2:
            return (*aged_bronze(face, lx, ly, fw, fh, sh, gx, gy)[:3], 255)
        return c

    for ub, fn, gfn in (
        ((0, 0, 22, 20, 14), hulk_chest, None),
        ((31, 78, 12, 12, 3), aged_bronze, None),
        ((208, 78, 8, 8, 2), furnace_col, furnace_glow),
        ((0, 35, 24, 5, 16), leather_belt, None),
        ((133, 94, 6, 6, 2), aged_bronze, None),
        ((191, 35, 5, 16, 5), aged_bronze, None),
        ((98, 94, 13, 4, 4), steel_iron, None),
        ((81, 35, 20, 5, 16), aged_bronze, None),
        ((73, 0, 16, 16, 14), aged_bronze, None),
        ((62, 78, 10, 4, 10), aged_bronze, None),
        ((229, 78, 4, 6, 4), aged_bronze, None),
        ((0, 78, 12, 12, 3), aged_bronze, None),
        ((187, 78, 8, 8, 2), furnace_col, furnace_glow),
        ((180, 78, 2, 10, 1), steel_iron, None),
        ((183, 94, 10, 2, 1), steel_iron, None),
        ((103, 78, 3, 7, 7), aged_bronze, None),
        ((0, 57, 12, 8, 12), aged_bronze, None),
        ((137, 57, 8, 11, 8), verdigris_suit, None),
        ((124, 78, 9, 4, 9), aged_bronze, None),
        ((134, 0, 10, 12, 10), verdigris_suit, None),
        ((175, 0, 3, 12, 10), aged_bronze, None),
        ((56, 94, 5, 4, 5), aged_bronze, None),
        ((150, 94, 3, 5, 2), steel_iron, None),
        ((170, 57, 4, 14, 4), steel_iron, None),
        ((13, 94, 16, 4, 5), steel_iron, None),
        ((161, 78, 4, 7, 5), steel_iron, None),
        ((154, 35, 9, 12, 9), verdigris_suit, None),
        ((187, 57, 10, 5, 10), aged_bronze, None),
        ((100, 57, 9, 10, 9), verdigris_suit, None),
        ((49, 57, 11, 5, 14), aged_bronze, None),
        ((77, 94, 5, 4, 5), barnacle_sh, None),
        ((170, 94, 3, 3, 3), barnacle_sh, None),
        ((0, 94, 3, 7, 3), coral_pink, None),
        ((161, 94, 2, 5, 2), coral_pink, None),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("barnacle_hulk_dredger", s, gl)

    # 2. Siren-Veil Jelly (256x256, 14 boxes matching SirenVeilJellyModel verbatim, translucent glass canopy + glowing crystal nucleus)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    def jelly_glass_bell(base, hi, edge_col, interior_alpha=130):
        def p(face, lx, ly, fw, fh, sh, gx, gy):
            if lx in (0, fw - 1) or ly in (0, fh - 1):
                return (*edge_col, 235)
            n = _wa.noise(gx, gy, 202)
            col = _mix(base, hi, 0.35 if n > 0.6 else 0.0)
            if face == "top" and (lx in (1, fw - 2) or ly in (1, fh - 2)):
                return (*_mix(hi, edge_col, 0.5), 185)
            return (*col, interior_alpha)
        return p

    def jelly_bell_rim_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if lx in (0, fw - 1) or ly in (0, fh - 1):
            return (112, 246, 255, 220)
        return None

    bell_glass_hi = jelly_glass_bell((104, 64, 212), (168, 128, 255), (128, 246, 255), 145)
    bell_glass_mid = jelly_glass_bell((92, 52, 198), (156, 112, 248), (232, 112, 255), 130)
    bell_glass_skirt = jelly_glass_bell((82, 48, 186), (142, 98, 242), (108, 248, 255), 125)
    for ub, fn, gfn in (
        ((94, 35, 6, 11, 6), _glow_fill((142, 248, 255), alpha=245), _glow_fill((168, 252, 255), alpha=245)),
        ((152, 35, 10, 7, 4), _glow_fill((228, 86, 255), alpha=240, core=(245, 228, 255)), _glow_fill((240, 108, 255), alpha=235)),
        ((65, 35, 4, 7, 10), _glow_fill((228, 86, 255), alpha=240, core=(245, 228, 255)), _glow_fill((240, 108, 255), alpha=235)),
        ((208, 35, 4, 5, 4), _glow_fill((242, 255, 255), alpha=255), _glow_fill((255, 255, 255), alpha=255)),
        ((126, 35, 5, 11, 2), _organic_shaded((222, 74, 246), (255, 168, 255), (144, 32, 178), alpha=230, stripe=True), _glow_fill((228, 92, 248), alpha=180)),
        ((141, 35, 3, 11, 2), _organic_shaded((104, 228, 255), (218, 252, 255), (62, 146, 224), alpha=225), _glow_fill((128, 242, 255), alpha=190)),
        ((0, 35, 16, 5, 16), bell_glass_hi, jelly_bell_rim_glow),
        ((113, 0, 24, 8, 24), bell_glass_mid, jelly_bell_rim_glow),
        ((0, 0, 28, 6, 28), bell_glass_skirt, jelly_bell_rim_glow),
        ((225, 35, 6, 3, 3), _glow_fill((226, 118, 255), alpha=205), _glow_fill((236, 142, 255), alpha=210)),
        ((119, 35, 2, 13, 1), _glow_fill((142, 248, 255), alpha=235), _glow_fill((168, 252, 255), alpha=245)),
        ((181, 35, 2, 9, 2), _organic_shaded((192, 86, 252), (242, 168, 255), (112, 42, 178), alpha=220), _glow_fill((204, 98, 255), alpha=190)),
        ((190, 35, 2, 9, 2), _organic_shaded((118, 184, 255), (192, 238, 255), (68, 108, 208), alpha=215), _glow_fill((126, 206, 255), alpha=195)),
        ((199, 35, 2, 8, 2), _glow_fill((98, 248, 255), alpha=225), _glow_fill((132, 252, 255), alpha=235)),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("siren_veil_jelly", s, gl)

    # 3. Coral-Spine Mako (256x256, 22 boxes matching CoralSpineMakoModel verbatim)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    def mako_body(gills=False, jaw=False, eyes=False):
        def p(face, lx, ly, fw, fh, sh, gx, gy):
            if eyes and face in ("left", "right") and (1 <= ly <= 3) and (
                (face == "left" and fw - 4 <= lx <= fw - 2) or (face == "right" and 1 <= lx <= 3)
            ):
                return (255, 86, 56, 255) if ly == 2 else (255, 208, 72, 255)
            n = _wa.noise(gx, gy, 303)
            if jaw or face == "bottom" or (face in ("left", "right", "front", "back") and ly >= fh * 0.65):
                col = _mix((204, 222, 232), (236, 246, 252), 0.25 if n > 0.6 else 0.0)
            else:
                col = _mix((22, 76, 134), (44, 118, 182), 0.28 if (sh > 0 or n > 0.66) else 0.0)
            if gills and face in ("left", "right") and (1 <= ly <= fh - 3) and (lx in (1, 3, 5) if face == "right" else lx in (fw - 2, fw - 4, fw - 6)):
                col = (128, 28, 44)
            return (*col, 255)
        return p

    def mako_eye_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("left", "right") and (1 <= ly <= 3) and (
            (face == "left" and fw - 4 <= lx <= fw - 2) or (face == "right" and 1 <= lx <= 3)
        ):
            return (255, 96, 62, 255) if ly == 2 else (255, 214, 82, 255)
        return None

    def mako_teeth(face, lx, ly, fw, fh, sh, gx, gy):
        # Crisp jagged ivory shark teeth around jaw perimeter
        if face in ("left", "right", "front"):
            if (lx + ly) % 2 == 0:
                return (248, 252, 255, 255)
            return (148, 48, 62, 255)
        return (206, 98, 108, 255)

    coral_crimson = _organic_shaded((236, 86, 118), (255, 168, 186), (156, 36, 64), seed=305)
    coral_plate = _organic_shaded((168, 42, 68), (218, 86, 108), (108, 22, 42), spot=True, seed=306)
    coral_glow = _glow_fill((248, 96, 128), alpha=150, core=(255, 198, 212))
    for ub, fn, gfn in (
        ((0, 0, 12, 12, 22), mako_body(gills=True), None),
        ((0, 35, 13, 11, 6), mako_body(gills=True), None),
        ((114, 0, 8, 3, 18), mako_body(jaw=True), None),
        ((69, 0, 10, 10, 12), mako_body(eyes=True), mako_eye_glow),
        ((120, 35, 8, 7, 7), mako_body(), None),
        ((0, 53, 5, 4, 5), mako_body(), None),
        ((83, 35, 8, 4, 10), mako_body(jaw=True), None),
        ((151, 35, 7, 2, 9), mako_teeth, None),
        ((193, 35, 6, 2, 8), mako_teeth, None),
        ((21, 53, 3, 3, 6), coral_plate, None),
        ((167, 0, 2, 12, 9), mako_body(), None),
        ((53, 53, 10, 2, 6), mako_body(), None),
        ((95, 53, 7, 2, 4), coral_plate, None),
        ((118, 53, 5, 2, 4), mako_body(), None),
        ((207, 0, 8, 9, 10), mako_body(), None),
        ((39, 35, 5, 6, 9), mako_body(), None),
        ((40, 53, 2, 5, 4), mako_body(), None),
        ((190, 0, 2, 15, 6), mako_body(), None),
        ((68, 35, 2, 10, 5), mako_body(), None),
        ((222, 35, 4, 3, 7), coral_plate, None),
        ((184, 35, 2, 9, 2), coral_crimson, coral_glow),
        ((86, 53, 2, 6, 2), coral_crimson, coral_glow),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("coral_spine_mako", s, gl)

    # 4. Dread-Nautilus Sentinel (256x256, 11 boxes matching DreadNautilusModel verbatim)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    def nautilus_nacre(face, lx, ly, fw, fh, sh, gx, gy):
        n = _wa.noise(gx, gy, 401)
        band = ((lx + ly * 2) % 6 < 2)
        c = _mix((228, 212, 184), (174, 112, 64), 0.72 if band else 0.0)
        if n > 0.75:
            c = _mix(c, (248, 240, 224), 0.35)
        return (*c, 255)

    def nautilus_aperture(face, lx, ly, fw, fh, sh, gx, gy):
        c = _verdigris_metal((176, 122, 48), (228, 178, 86), (104, 68, 24), patina=(58, 132, 118), seed=403)(face, lx, ly, fw, fh, sh, gx, gy)
        if face == "front" and (lx in (2, fw - 3) or ly in (2, fh - 3)) and (lx + ly) % 3 == 0:
            return (64, 245, 248, 255)
        return c

    def nautilus_aperture_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face == "front" and (lx in (2, fw - 3) or ly in (2, fh - 3)) and (lx + ly) % 3 == 0:
            return (78, 248, 255, 255)
        return None

    def nautilus_eye(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("left", "right", "front") and (1 <= lx <= fw - 2) and (1 <= ly <= fh - 2):
            return (255, 206, 48, 255) if not (lx == fw // 2 and ly == fh // 2) else (42, 16, 8, 255)
        return (34, 18, 44, 255)

    def nautilus_eye_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("left", "right", "front") and (1 <= lx <= fw - 2) and (1 <= ly <= fh - 2):
            if not (lx == fw // 2 and ly == fh // 2):
                return (255, 212, 58, 255)
        return None

    def nautilus_tentacle(face, lx, ly, fw, fh, sh, gx, gy):
        if face == "bottom":
            return (226, 174, 172, 255) if (gy % 2 == 0) else (168, 92, 98, 255)
        return (*_mix((158, 42, 64), (212, 82, 98), 0.35 if _wa.noise(gx, gy, 405) > 0.6 else 0.0), 255)

    bronze_rib = _verdigris_metal((56, 128, 114), (212, 164, 78), (32, 74, 68), patina=(178, 122, 52), seed=402)
    hood_col = _verdigris_metal((48, 24, 64), (82, 46, 108), (24, 12, 34), patina=(58, 114, 116), seed=404)
    for ub, fn, gfn in (
        ((0, 0, 17, 22, 20), nautilus_nacre, None),
        ((75, 0, 15, 12, 14), nautilus_nacre, None),
        ((99, 43, 19, 4, 6), bronze_rib, None),
        ((0, 43, 21, 6, 6), _verdigris_metal((198, 146, 54), (248, 208, 106), (118, 82, 28), seed=406), None),
        ((134, 0, 20, 16, 4), nautilus_aperture, nautilus_aperture_glow),
        ((183, 0, 14, 7, 10), hood_col, None),
        ((78, 43, 5, 6, 5), nautilus_eye, nautilus_eye_glow),
        ((150, 43, 3, 3, 6), _organic_shaded((88, 46, 98), (134, 78, 148), (48, 22, 56)), None),
        ((188, 43, 4, 4, 3), _organic_shaded((38, 32, 36), (68, 58, 62), (18, 14, 18)), None),
        ((55, 43, 3, 3, 8), nautilus_tentacle, None),
        ((169, 43, 2, 2, 7), nautilus_tentacle, None),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("dread_nautilus_sentinel", s, gl)

    # 5. Thalassor, Abyssal Sovereign (256x256, 22 boxes matching ThalassorModel verbatim + 7x9 cyan runes & translucent fins)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    abyss_scale = _verdigris_metal((18, 32, 52), (42, 76, 108), (8, 16, 28), patina=(28, 114, 120), seed=501)
    gold_horn = _verdigris_metal((198, 158, 62), (252, 218, 118), (118, 88, 32), seed=502)
    scute_ivory = _organic_shaded((192, 178, 128), (236, 222, 168), (124, 112, 76), stripe=True, seed=503)

    def thalassor_runed(glyph_idx=0, is_head=False):
        glyph = _RUNE_GLYPHS[glyph_idx % len(_RUNE_GLYPHS)]

        def _is_rune_px(face, lx, ly, fw, fh):
            if face in ("left", "right") and fw >= 14 and fh >= 10:
                rx = lx - (fw - 7) // 2
                ry = ly - (fh - 9) // 2
                if 0 <= rx < 7 and 0 <= ry < 9 and glyph[ry][rx] == "X":
                    return True
            if is_head and face == "top" and fw >= 12 and fh >= 14:
                rx = lx - (fw - 7) // 2
                ry = ly - (fh - 9) // 2
                if 0 <= rx < 7 and 0 <= ry < 9 and glyph[ry][rx] == "X":
                    return True
            return False

        def p(face, lx, ly, fw, fh, sh, gx, gy):
            if is_head and face in ("left", "right") and (5 <= ly <= 7) and (
                (face == "left" and fw - 6 <= lx <= fw - 3) or (face == "right" and 2 <= lx <= 5)
            ):
                return (142, 255, 252, 255)
            if _is_rune_px(face, lx, ly, fw, fh):
                return (62, 246, 242, 255)
            return abyss_scale(face, lx, ly, fw, fh, sh, gx, gy)

        def gp(face, lx, ly, fw, fh, sh, gx, gy):
            if is_head and face in ("left", "right") and (5 <= ly <= 7) and (
                (face == "left" and fw - 6 <= lx <= fw - 3) or (face == "right" and 2 <= lx <= 5)
            ):
                return (176, 255, 255, 255)
            if _is_rune_px(face, lx, ly, fw, fh):
                return (74, 248, 245, 255)
            return None

        return p, gp

    def thalassor_veined_fin(face, lx, ly, fw, fh, sh, gx, gy):
        edge = lx in (0, fw - 1) or ly in (0, fh - 1)
        vein = (lx + ly) % 5 == 0
        if edge or vein:
            return (78, 244, 236, 255)
        n = _wa.noise(gx, gy, 508)
        col = _mix((24, 148, 152), (54, 212, 206), 0.35 if n > 0.5 else 0.0)
        return (*col, 185)

    def thalassor_fin_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if lx in (0, fw - 1) or ly in (0, fh - 1) or (lx + ly) % 5 == 0:
            return (86, 248, 240, 225)
        return None

    for ub, fn, gfn in (
        ((0, 0, 22, 20, 24), *thalassor_runed(1)),
        ((150, 45, 16, 16, 18), *thalassor_runed(2)),
        ((0, 45, 16, 14, 22), *thalassor_runed(0, is_head=True)),
        ((198, 112, 12, 10, 10), abyss_scale, None),
        ((84, 138, 4, 3, 14), gold_horn, None),
        ((121, 138, 4, 11, 4), gold_horn, _glow_fill((96, 250, 242), alpha=110)),
        ((0, 138, 3, 16, 4), gold_horn, None),
        ((0, 112, 11, 5, 20), abyss_scale, None),
        ((210, 138, 2, 4, 2), _organic_shaded((248, 252, 255), (255, 255, 255), (192, 212, 224)), None),
        ((61, 82, 16, 4, 22), scute_ivory, None),
        ((63, 112, 12, 3, 20), scute_ivory, None),
        ((138, 138, 10, 4, 8), abyss_scale, None),
        ((138, 0, 2, 14, 24), thalassor_veined_fin, thalassor_fin_glow),
        ((191, 82, 2, 12, 14), thalassor_veined_fin, thalassor_fin_glow),
        ((128, 112, 1, 10, 12), thalassor_veined_fin, thalassor_fin_glow),
        ((15, 138, 18, 2, 16), thalassor_veined_fin, thalassor_fin_glow),
        ((175, 138, 9, 2, 8), thalassor_veined_fin, thalassor_fin_glow),
        ((77, 45, 18, 16, 18), *thalassor_runed(3)),
        ((0, 82, 14, 13, 16), *thalassor_runed(4)),
        ((138, 82, 10, 10, 16), *thalassor_runed(0)),
        ((155, 112, 7, 7, 14), abyss_scale, None),
        ((93, 0, 2, 24, 20), thalassor_veined_fin, thalassor_fin_glow),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("thalassor", s, gl)

    # 6. Tidefolk Brine-Merchants x3 Professions (256x256 + glow maps, 32 boxes matching BrineMerchantModel verbatim)
    profs = {
        "brine_merchant_jeweler": ((176, 122, 46), (28, 88, 118), (64, 236, 248), False),
        "brine_merchant_cartographer": ((44, 112, 106), (26, 94, 98), (228, 206, 152), True),
        "brine_merchant_alchemist": ((48, 112, 46), (32, 96, 82), (82, 248, 224), False),
    }
    for pname, (helm_c, robe_c, prop_c, has_sonar) in profs.items():
        m = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        mgl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        brass = _verdigris_metal((188, 134, 52), (236, 188, 92), (112, 74, 24), patina=(60, 124, 112), seed=601)
        helm_metal = _verdigris_metal(
            helm_c,
            (min(255, helm_c[0] + 44), min(255, helm_c[1] + 44), min(255, helm_c[2] + 44)),
            (max(0, helm_c[0] - 34), max(0, helm_c[1] - 34), max(0, helm_c[2] - 34)),
            seed=603,
        )
        coat = _organic_shaded(
            robe_c,
            (min(255, robe_c[0] + 32), min(255, robe_c[1] + 32), min(255, robe_c[2] + 32)),
            (max(0, robe_c[0] - 20), max(0, robe_c[1] - 20), max(0, robe_c[2] - 20)),
            stripe=True,
            seed=602,
        )

        def villager_face(face, lx, ly, fw, fh, sh, gx, gy):
            c = _organic_shaded((172, 146, 124), (198, 174, 148), (128, 104, 84))(face, lx, ly, fw, fh, sh, gx, gy)
            if face == "front":
                if ly == 6 and (2 <= lx <= 11):
                    return (78, 56, 40, 255)
                if ly in (7, 8) and lx in (3, 4, 9, 10):
                    return (242, 250, 255, 255) if lx in (3, 10) else (36, 168, 128, 255)
                if ly == 10 and (5 <= lx <= 8):
                    return (126, 86, 68, 255)
            return c

        def merchant_helm_shell(face, lx, ly, fw, fh, sh, gx, gy):
            if face == "front" and (5 <= lx < 15) and (5 <= ly < 15):
                return (0, 0, 0, 0)
            return helm_metal(face, lx, ly, fw, fh, sh, gx, gy)

        def merchant_helm_bezel(face, lx, ly, fw, fh, sh, gx, gy):
            if face in ("front", "back") and (2 <= lx < 12) and (2 <= ly < 12):
                if (lx, ly) not in ((2, 2), (11, 2), (2, 11), (11, 11)):
                    return (0, 0, 0, 0)
            return brass(face, lx, ly, fw, fh, sh, gx, gy)

        def merchant_helm_glass(face, lx, ly, fw, fh, sh, gx, gy):
            if lx in (0, fw - 1) or ly in (0, fh - 1):
                return (148, 245, 255, 185)
            if has_sonar and face == "front" and ((lx == 5 and ly in (1, 2, 7, 8)) or (ly == 5 and lx in (1, 2, 7, 8))):
                return (68, 248, 255, 195)
            if lx - ly in (1, 2) and 1 <= lx <= 4:
                return (236, 255, 255, 165)
            return (64, 218, 242, 105 if has_sonar else 95)

        def merchant_glass_glow(face, lx, ly, fw, fh, sh, gx, gy):
            if has_sonar and face == "front" and (
                (lx == 5 and ly in (1, 2, 7, 8)) or (ly == 5 and lx in (1, 2, 7, 8))
                or (lx in (0, fw - 1) and ly in (0, fh - 1))
            ):
                return (78, 248, 255, 210)
            return None

        def archipelago_scroll(face, lx, ly, fw, fh, sh, gx, gy):
            if face != "front":
                return (196, 174, 134, 255)
            if lx in (0, fw - 1) or ly in (0, fh - 1):
                return (176, 146, 98, 255)
            # Painted ocean map with emerald archipelago islands, cyan depth currents & red X treasure marker
            c = (48, 128, 146)
            if (lx in (4, 5, 6) and ly in (3, 4)) or (lx in (11, 12, 13) and ly in (6, 7, 8)):
                c = (86, 168, 82)
            elif (lx + ly) % 4 == 0 and 2 <= lx <= fw - 3:
                c = (112, 232, 242)
            elif lx == 12 and ly == 7:
                c = (232, 68, 68)
            return (*c, 255)

        def scroll_glow(face, lx, ly, fw, fh, sh, gx, gy):
            if face == "front" and (lx + ly) % 4 == 0 and 2 <= lx <= fw - 3 and 2 <= ly <= fh - 3:
                return (96, 242, 252, 195)
            return None

        def trimmed_coat(face, lx, ly, fw, fh, sh, gx, gy):
            c = coat(face, lx, ly, fw, fh, sh, gx, gy)
            if face not in ("top", "bottom") and (ly == fh - 1 or (face == "front" and lx in (fw // 2 - 1, fw // 2))):
                return (212, 168, 68, 255)
            return c

        for ub, fn, gfn in (
            ((0, 43, 14, 18, 14), villager_face, None),
            ((227, 97, 3, 7, 4), _organic_shaded((154, 126, 104), (182, 154, 128), (114, 88, 70)), None),
            ((0, 0, 21, 21, 21), _organic_shaded((62, 118, 44), (96, 158, 62), (36, 74, 26), stripe=True, spot=True), None),
            ((205, 43, 12, 8, 12), _organic_shaded((72, 128, 52), (108, 168, 72), (42, 82, 30), stripe=True), None),
            ((90, 43, 22, 8, 16), _organic_shaded((52, 102, 40), (84, 138, 54), (32, 64, 24), stripe=True), None),
            ((85, 0, 20, 20, 20), merchant_helm_shell, None),
            ((90, 76, 14, 14, 3), merchant_helm_bezel, None),
            ((89, 97, 3, 7, 7), brass, None),
            ((135, 97, 8, 3, 8), brass, None),
            ((168, 97, 10, 10, 1), merchant_helm_glass, merchant_glass_glow),
            ((166, 0, 16, 24, 12), trimmed_coat, None),
            ((21, 76, 20, 4, 14), brass, None),
            ((88, 114, 5, 4, 2), _glow_fill((108, 248, 162)), _glow_fill((118, 255, 172))),
            ((0, 97, 7, 14, 2), trimmed_coat, None),
            ((177, 76, 16, 14, 2), trimmed_coat, None),
            ((210, 97, 4, 7, 4), _glow_fill((64, 245, 255)), _glow_fill((84, 250, 255))),
            ((191, 97, 4, 6, 5), leather_belt, None),
            ((0, 114, 5, 5, 4), brass, _glow_fill((92, 245, 255), alpha=150)),
            ((0, 76, 5, 15, 5), brass, None),
            ((36, 114, 14, 3, 5), leather_belt, None),
            ((167, 43, 2, 22, 2), leather_belt, None),
            ((103, 114, 2, 4, 2), _glow_fill((86, 248, 212)), _glow_fill((98, 252, 224))),
            ((176, 43, 7, 14, 7), coat, None),
            ((214, 76, 8, 8, 8), brass, None),
            ((57, 43, 8, 20, 8), _organic_shaded((58, 44, 34), (84, 64, 50), (34, 26, 20)), None),
            ((138, 76, 9, 6, 10), brass, None),
            ((110, 97, 6, 6, 6), _glow_fill(prop_c), _glow_fill(prop_c)),
            ((19, 114, 4, 5, 4), gold_horn, None),
            ((19, 97, 18, 12, 2), archipelago_scroll, scroll_glow),
            ((125, 76, 3, 14, 3), brass, None),
            ((60, 97, 7, 7, 7), _glow_fill((86, 248, 238)), _glow_fill((96, 252, 248))),
            ((75, 114, 3, 4, 3), _organic_shaded((164, 118, 68), (206, 158, 96), (104, 72, 40)), None),
        ):
            _paint_textured_box(m, ub, fn, mgl, gfn)
        save_pair(pname, m, mgl)

    # 7. Lumi-Seahorse (256x256, 20 boxes matching LumiSeahorseModel verbatim, translucent fins + emerald eyes)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    seahorse_jade = _organic_shaded((38, 174, 156), (98, 234, 204), (18, 94, 96), stripe=True, seed=701)
    seahorse_gold = _organic_shaded((238, 186, 62), (255, 228, 124), (164, 116, 28), seed=702)

    def seahorse_head(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("left", "right") and (3 <= ly <= 5) and (
            (face == "left" and fw - 4 <= lx <= fw - 2) or (face == "right" and 1 <= lx <= 3)
        ):
            return (118, 255, 224, 255)
        return seahorse_jade(face, lx, ly, fw, fh, sh, gx, gy)

    def seahorse_head_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if face in ("left", "right") and (3 <= ly <= 5) and (
            (face == "left" and fw - 4 <= lx <= fw - 2) or (face == "right" and 1 <= lx <= 3)
        ):
            return (138, 255, 232, 255)
        return None

    def seahorse_fin(face, lx, ly, fw, fh, sh, gx, gy):
        if lx in (0, fw - 1) or ly in (0, fh - 1) or (ly % 3 == 0):
            return (118, 255, 228, 255)
        return (56, 212, 218, 170)

    def seahorse_fin_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if lx in (0, fw - 1) or ly in (0, fh - 1) or (ly % 3 == 0):
            return (132, 255, 236, 230)
        return None

    for ub, fn, gfn in (
        ((0, 0, 12, 16, 12), seahorse_jade, None),
        ((152, 0, 11, 3, 13), seahorse_gold, None),
        ((56, 29, 8, 9, 4), _glow_fill((118, 255, 212), core=(255, 248, 182)), _glow_fill((142, 255, 224), core=(255, 252, 198))),
        ((49, 0, 1, 14, 10), seahorse_fin, seahorse_fin_glow),
        ((201, 0, 13, 3, 12), leather_belt, None),
        ((174, 29, 2, 7, 3), aged_bronze, None),
        ((113, 0, 9, 10, 10), seahorse_jade, None),
        ((72, 0, 10, 10, 10), seahorse_head, seahorse_head_glow),
        ((29, 29, 4, 4, 9), seahorse_jade, None),
        ((209, 29, 5, 5, 2), seahorse_gold, None),
        ((142, 29, 3, 7, 4), seahorse_gold, _glow_fill((255, 232, 128), alpha=150)),
        ((198, 29, 2, 5, 3), seahorse_gold, None),
        ((106, 29, 1, 7, 6), seahorse_fin, seahorse_fin_glow),
        ((0, 29, 7, 8, 7), seahorse_jade, None),
        ((81, 29, 6, 7, 6), seahorse_jade, None),
        ((121, 29, 5, 6, 5), seahorse_jade, None),
        ((157, 29, 4, 6, 4), seahorse_jade, None),
        ((185, 29, 3, 5, 3), seahorse_jade, None),
        ((224, 29, 3, 4, 3), seahorse_jade, None),
        ((237, 29, 2, 4, 2), seahorse_jade, None),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("lumi_seahorse", s, gl)

    # 8. Pearl-Pup Seal (256x256, 12 boxes matching PearlPupModel verbatim, glossy 3x3 eyes w/ catchlight & rosy cheek frills)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    seal_coat = _organic_shaded((232, 242, 248), (252, 254, 255), (168, 194, 214), spot=True, seed=801)

    def pup_head(face, lx, ly, fw, fh, sh, gx, gy):
        c = seal_coat(face, lx, ly, fw, fh, sh, gx, gy)
        # 3x3 glossy dark puppy eyes with white top-left catchlight pixel
        if face == "front" and (3 <= ly <= 5) and (lx in (1, 2, 3, 7, 8, 9)):
            if (lx, ly) in ((1, 3), (7, 3)):
                return (248, 252, 255, 255)
            return (18, 24, 36, 255)
        return c

    def pup_muzzle(face, lx, ly, fw, fh, sh, gx, gy):
        c = seal_coat(face, lx, ly, fw, fh, sh, gx, gy)
        if face == "front" and ly in (0, 1) and lx in (2, 3):
            return (226, 98, 134, 255)
        return c

    for ub, fn, gfn in (
        ((0, 0, 13, 11, 16), seal_coat, None),
        ((102, 0, 11, 8, 7), _organic_shaded((248, 252, 255), (255, 255, 255), (212, 224, 236)), None),
        ((59, 0, 11, 10, 10), pup_head, None),
        ((9, 28, 6, 4, 4), pup_muzzle, None),
        ((47, 28, 4, 5, 2), _organic_shaded((244, 152, 178), (255, 198, 214), (186, 92, 126)), None),
        ((0, 28, 2, 7, 2), _organic_shaded((134, 188, 206), (186, 228, 242), (82, 134, 152)), None),
        ((60, 28, 2, 5, 2), _organic_shaded((134, 188, 206), (186, 228, 242), (82, 134, 152)), None),
        ((30, 28, 4, 4, 4), _glow_fill((148, 252, 255)), _glow_fill((188, 255, 255))),
        ((172, 0, 5, 5, 5), _glow_fill((255, 178, 242), alpha=115, core=(228, 252, 255)), _glow_fill((255, 196, 248), alpha=110)),
        ((193, 0, 9, 3, 6), seal_coat, None),
        ((139, 0, 8, 6, 8), seal_coat, None),
        ((224, 0, 5, 2, 7), seal_coat, None),
    ):
        _paint_textured_box(s, ub, fn, gl, gfn)
    save_pair("pearl_pup", s, gl)

    # 9. Singing Moon-Whale (256x256, 15 boxes matching SingingMoonWhaleModel verbatim + '+' constellation stars & golden baleen)
    s = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 256), (0, 0, 0, 0))

    def _is_star_px(face, lx, ly, fw, fh):
        if face not in ("top", "left", "right") or fw < 8 or fh < 8:
            return False, False
        sx = (lx + 3) % 10 - 5
        sy = (ly + 2) % 9 - 4
        is_plus = (abs(sx) <= 1 and sy == 0) or (sx == 0 and abs(sy) <= 1)
        is_constellation_line = (abs(lx - ly) <= 0) and (lx % 2 == 0) and not is_plus and (fw >= 14)
        return is_plus, is_constellation_line

    def whale_body(stars=True, is_head=False):
        def p(face, lx, ly, fw, fh, sh, gx, gy):
            if is_head and face in ("left", "right") and (6 <= ly <= 8) and (
                (face == "left" and fw - 5 <= lx <= fw - 3) or (face == "right" and 2 <= lx <= 4)
            ):
                return (148, 252, 255, 255)
            n = _wa.noise(gx, gy, 901)
            if stars:
                plus, line = _is_star_px(face, lx, ly, fw, fh)
                if plus:
                    return (176, 254, 255, 255)
                if line:
                    return (226, 206, 128, 255)
            col = _mix((28, 56, 114), (54, 96, 168), 0.28 if (sh > 0 or n > 0.65) else 0.0)
            return (*col, 255)

        def gp(face, lx, ly, fw, fh, sh, gx, gy):
            if is_head and face in ("left", "right") and (6 <= ly <= 8) and (
                (face == "left" and fw - 5 <= lx <= fw - 3) or (face == "right" and 2 <= lx <= 4)
            ):
                return (176, 255, 255, 255)
            if stars:
                plus, line = _is_star_px(face, lx, ly, fw, fh)
                if plus:
                    return (182, 255, 255, 255)
                if line:
                    return (218, 198, 124, 155)
            return None

        return p, gp

    def whale_throat_pleats(face, lx, ly, fw, fh, sh, gx, gy):
        c = (158, 174, 206) if (lx % 2 == 0) else (92, 112, 148)
        if face in ("left", "right", "bottom") and (lx % 4 == 0):
            c = (218, 198, 126)
        return (*c, 255)

    def whale_fin_rim(face, lx, ly, fw, fh, sh, gx, gy):
        if lx <= 1 or lx >= fw - 2 or ly <= 1 or ly >= fh - 2:
            return (124, 248, 255, 255)
        n = _wa.noise(gx, gy, 902)
        return (*_mix((34, 64, 122), (56, 100, 172), 0.25 if n > 0.6 else 0.0), 255)

    def whale_fin_glow(face, lx, ly, fw, fh, sh, gx, gy):
        if lx <= 1 or lx >= fw - 2 or ly <= 1 or ly >= fh - 2:
            return (132, 250, 255, 225)
        return None

    for ub, fn, gfn in (
        ((0, 0, 24, 20, 26), *whale_body(stars=True)),
        ((57, 110, 3, 7, 9), whale_fin_rim, whale_fin_glow),
        ((0, 47, 22, 15, 20), *whale_body(stars=True, is_head=True)),
        ((77, 83, 18, 11, 12), *whale_body(stars=True)),
        ((143, 110, 13, 8, 7), *whale_body(stars=False)),
        ((183, 83, 2, 5, 16), lambda f, lx, ly, fw, fh, sh, gx, gy: (238, 218, 148, 255) if ((lx + gx) % 2 == 0) else (146, 124, 78, 255), None),
        ((85, 47, 20, 5, 28), whale_throat_pleats, None),
        ((0, 83, 16, 4, 22), whale_throat_pleats, None),
        ((0, 110, 12, 3, 16), whale_throat_pleats, None),
        ((184, 110, 22, 3, 10), whale_fin_rim, whale_fin_glow),
        ((0, 130, 14, 3, 7), whale_fin_rim, whale_fin_glow),
        ((101, 0, 19, 16, 20), *whale_body(stars=True)),
        ((182, 47, 13, 12, 16), *whale_body(stars=True)),
        ((138, 83, 8, 8, 14), *whale_body(stars=False)),
        ((82, 110, 18, 3, 12), whale_fin_rim, whale_fin_glow),
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
