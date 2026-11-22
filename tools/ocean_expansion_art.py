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


def _elem(x0, y0, z0, x1, y1, z1, tex):
    u0, v0 = max(0.0, min(16.0, x0)), max(0.0, min(16.0, 16.0 - y1))
    u1, v1 = max(0.1, min(16.0, max(u0 + 0.5, x1))), max(0.1, min(16.0, max(v0 + 0.5, 16.0 - y0)))
    face = {"texture": f"#{tex}", "uv": [u0, v0, u1, v1]}
    return {
        "from": [x0, y0, z0],
        "to": [x1, y1, z1],
        "faces": {d: dict(face) for d in ("north", "south", "east", "west", "up", "down")},
    }


def _build_3d_weapon_models(g):
    # Shared block textures for 3D held models
    tex_defs = {
        "tide_cleaver_harpoon_steel": (188, 206, 222, 255),
        "tide_cleaver_harpoon_brass": (188, 134, 54, 255),
        "tide_cleaver_harpoon_teal": (36, 148, 136, 255),
        "leviathan_glaive_blade": (84, 240, 232, 255),
        "leviathan_glaive_gold": (212, 162, 62, 255),
        "leviathan_glaive_pearl": (132, 255, 255, 255),
        "leviathan_glaive_haft": (26, 98, 102, 255),
        "tidecaller_conch_shell": (244, 226, 196, 255),
        "tidecaller_conch_pink": (244, 168, 164, 255),
    }
    for name, c in tex_defs.items():
        tile = Image.new("RGBA", (16, 16), c)
        px = tile.load()
        for y in range(16):
            for x in range(16):
                if (x + y) % 4 == 0:
                    px[x, y] = (min(255, c[0] + 18), min(255, c[1] + 18), min(255, c[2] + 18), 255)
        g.save(tile, g.ASSETS / f"textures/block/{name}.png")

    # 1. Tide-Cleaver Harpoon 3D held
    harpoon_in_hand = {
        "textures": {
            "steel": "wildercord:block/tide_cleaver_harpoon_steel",
            "brass": "wildercord:block/tide_cleaver_harpoon_brass",
            "teal": "wildercord:block/tide_cleaver_harpoon_teal",
            "particle": "wildercord:block/tide_cleaver_harpoon_steel",
        },
        "elements": [
            _elem(7.4, -8.0, 7.4, 8.6, 14.0, 8.6, "brass"),       # main shaft
            _elem(7.2, -3.0, 7.2, 8.8, 4.0, 8.8, "teal"),         # wrapped grip
            _elem(5.5, 13.5, 7.2, 10.5, 15.0, 8.8, "brass"),      # crescent crossguard
            _elem(6.8, 15.0, 7.4, 9.2, 25.0, 8.6, "steel"),       # spear tip
            _elem(5.4, 17.5, 7.5, 10.6, 20.0, 8.5, "steel"),      # barbed wings
            _elem(7.0, -9.8, 7.0, 9.0, -8.0, 9.0, "brass"),       # pommel ring
            _elem(9.0, 4.0, 7.5, 12.2, 13.5, 8.5, "steel"),       # hanging retrieval chain arc
        ],
        "display": _display_poses(),
    }
    g.write_json(g.ASSETS / "models/item/tide_cleaver_harpoon_in_hand.json", harpoon_in_hand)

    # 2. Leviathan Glaive 3D held (glowing Heart-Pearl visible front & back!)
    glaive_in_hand = {
        "textures": {
            "blade": "wildercord:block/leviathan_glaive_blade",
            "gold": "wildercord:block/leviathan_glaive_gold",
            "pearl": "wildercord:block/leviathan_glaive_pearl",
            "haft": "wildercord:block/leviathan_glaive_haft",
            "particle": "wildercord:block/leviathan_glaive_blade",
        },
        "elements": [
            _elem(7.4, -9.0, 7.4, 8.6, 13.0, 8.6, "haft"),        # polearm haft
            _elem(6.5, -11.5, 7.3, 9.5, -9.0, 8.7, "gold"),       # pommel spike
            _elem(5.2, 12.0, 6.8, 10.8, 15.5, 9.2, "gold"),       # finned sea-basket guard
            _elem(6.5, 13.0, 6.2, 9.5, 16.0, 9.8, "pearl"),       # Leviathan Heart-Pearl orb (front + back)
            _elem(6.8, 16.0, 7.5, 9.6, 29.0, 8.5, "blade"),       # gleaming crescent blade
            _elem(5.8, 22.0, 7.5, 8.2, 30.5, 8.5, "blade"),       # curved tip
        ],
        "display": _display_poses(),
    }
    g.write_json(g.ASSETS / "models/item/leviathan_glaive_in_hand.json", glaive_in_hand)

    # 3. Tidecaller Conch 3D held (10x8x8 stepped cream & pink-ventral spiral voxel shell)
    conch_in_hand = {
        "textures": {
            "shell": "wildercord:block/tidecaller_conch_shell",
            "pink": "wildercord:block/tidecaller_conch_pink",
            "particle": "wildercord:block/tidecaller_conch_shell",
        },
        "elements": [
            _elem(3.0, 5.0, 4.0, 13.0, 12.0, 12.0, "shell"),      # outer main whorl [10x7x8]
            _elem(5.0, 4.0, 5.0, 12.0, 7.0, 11.0, "pink"),        # pink-cream ventral lip opening
            _elem(1.0, 6.0, 5.5, 3.0, 10.5, 10.5, "shell"),       # mid spiral step
            _elem(-1.0, 7.0, 6.8, 1.0, 9.5, 9.2, "shell"),        # apex spire
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
    # Custom Block Textures (16x16)
    brass_tex = Image.new("RGBA", (16, 16), (176, 124, 48, 255))
    pearl_tex = Image.new("RGBA", (16, 16), (186, 68, 255, 255))
    anvil_tex = Image.new("RGBA", (16, 16), (44, 42, 54, 255))
    altar_tex = Image.new("RGBA", (16, 16), (22, 62, 68, 255))
    for i in range(16):
        brass_tex.putpixel((i, 0), (112, 76, 28, 255))
        pearl_tex.putpixel((i, i), (56, 245, 255, 255))
        anvil_tex.putpixel((i, 2), (218, 122, 42, 255))
        altar_tex.putpixel((i, 3), (222, 178, 62, 255))
        altar_tex.putpixel((3, i), (64, 245, 232, 255))
    g.save(brass_tex, g.ASSETS / "textures/block/siren_pearl_cage.png")
    g.save(pearl_tex, g.ASSETS / "textures/block/siren_pearl_core.png")
    g.save(anvil_tex, g.ASSETS / "textures/block/trench_anvil.png")
    g.save(altar_tex, g.ASSETS / "textures/block/sovereign_altar.png")

    # Siren-Pearl Lantern block (exact 8x10x8 cage + central 4x4x4 glowing pearl core)
    lantern_model = {
        "parent": "minecraft:block/block",
        "textures": {
            "cage": "wildercord:block/siren_pearl_cage",
            "core": "wildercord:block/siren_pearl_core",
            "particle": "wildercord:block/siren_pearl_cage",
        },
        "elements": [
            _elem(4, 0, 4, 12, 1, 12, "cage"),     # base 8x1x8
            _elem(4, 8, 4, 12, 9, 12, "cage"),     # top rim 8x1x8
            _elem(6, 9, 6, 10, 10, 10, "cage"),    # crown ring 4x1x4 (total 10 tall)
            _elem(4, 1, 4, 5, 8, 5, "cage"),
            _elem(11, 1, 4, 12, 8, 5, "cage"),
            _elem(4, 1, 11, 5, 8, 12, "cage"),
            _elem(11, 1, 11, 12, 8, 12, "cage"),
            _elem(6, 2, 6, 10, 6, 10, "core"),     # magenta-cyan swirling pearl core
        ],
    }
    g.write_json(g.ASSETS / "models/block/siren_pearl_lantern.json", lantern_model)
    g.write_json(g.ASSETS / "blockstates/siren_pearl_lantern.json", {"variants": {"": {"model": "wildercord:block/siren_pearl_lantern"}}})
    g.write_json(g.ASSETS / "items/siren_pearl_lantern.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/siren_pearl_lantern"}})

    # Trench Anvil block
    anvil_model = {
        "parent": "minecraft:block/block",
        "textures": {"anvil": "wildercord:block/trench_anvil", "brass": "wildercord:block/siren_pearl_cage", "particle": "wildercord:block/trench_anvil"},
        "elements": [
            _elem(2, 0, 2, 14, 4, 14, "anvil"),    # basalt plinth
            _elem(4, 4, 5, 12, 10, 11, "brass"),   # brass waist
            _elem(1, 10, 3, 15, 16, 13, "anvil"),  # horned forge face
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

    # Sovereign Altar block
    altar_model = {
        "parent": "minecraft:block/block",
        "textures": {"altar": "wildercord:block/sovereign_altar", "core": "wildercord:block/leviathan_glaive_pearl", "particle": "wildercord:block/sovereign_altar"},
        "elements": [
            _elem(0, 0, 0, 16, 10, 16, "altar"),
            _elem(2, 10, 2, 14, 12, 14, "altar"),
            _elem(5, 12, 5, 11, 14, 11, "core"),
        ],
    }
    g.write_json(g.ASSETS / "models/block/sovereign_altar.json", altar_model)
    g.write_json(g.ASSETS / "blockstates/sovereign_altar.json", {"variants": {"": {"model": "wildercord:block/sovereign_altar"}}})
    g.write_json(g.ASSETS / "items/sovereign_altar.json", {"model": {"type": "minecraft:model", "model": "wildercord:block/sovereign_altar"}})

    # Worn Armor Material Layers (abyssal_diving)
    g.write_json(g.ASSETS / "equipment/abyssal_diving.json", {
        "layers": {layer: [{"texture": "wildercord:abyssal_diving"}] for layer in ("humanoid", "humanoid_leggings")}
    })
    humanoid = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    _paint_box(humanoid, 0, 0, 8, 8, 8, (186, 132, 54, 255), (124, 84, 34, 255), (218, 168, 78, 255))     # helm base
    _paint_box(humanoid, 16, 16, 8, 12, 4, (28, 96, 100, 255), (18, 62, 68, 255), (62, 162, 158, 255))   # chest base
    _paint_box(humanoid, 40, 16, 4, 12, 4, (172, 122, 50, 255), (118, 80, 30, 255), (204, 156, 72, 255)) # arms
    _paint_box(humanoid, 0, 16, 4, 12, 4, (152, 84, 42, 255), (96, 52, 24, 255), (186, 112, 60, 255))    # boots
    g.save(humanoid, g.ASSETS / "textures/entity/equipment/humanoid/abyssal_diving.png")

    legs = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    _paint_box(legs, 0, 16, 4, 12, 4, (32, 148, 94, 255), (20, 96, 62, 255), (60, 212, 132, 255))       # kelp greaves
    _paint_box(legs, 16, 16, 8, 12, 4, (26, 118, 78, 255), (18, 80, 52, 255), (48, 178, 116, 255))     # waist belt
    g.save(legs, g.ASSETS / "textures/entity/equipment/humanoid_leggings/abyssal_diving.png")

    # 3D Helm Porthole & Back O2 Tanks layer texture (64x64)
    layer3d = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    glow3d = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    _paint_box(layer3d, 0, 0, 6, 6, 1, (188, 134, 54, 255), (124, 84, 34, 255), (218, 168, 78, 255))      # bezel
    _paint_box(layer3d, 14, 0, 4, 4, 2, (48, 245, 255, 255), (24, 168, 198, 255), (192, 255, 255, 255))   # porthole glass
    _paint_box(glow3d, 14, 0, 4, 4, 2, (48, 245, 255, 255), (32, 200, 232, 255), (210, 255, 255, 255))    # glowing porthole
    _paint_box(layer3d, 26, 0, 2, 4, 4, (158, 108, 42, 255), (104, 70, 26, 255), (192, 142, 62, 255))     # ear-ports
    _paint_box(layer3d, 0, 16, 3, 8, 2, (202, 148, 56, 255), (138, 92, 32, 255), (234, 186, 86, 255))     # brass O2 tanks
    _paint_box(layer3d, 10, 16, 6, 2, 2, (118, 128, 138, 255), (78, 86, 94, 255), (156, 168, 182, 255))   # valve bridge
    _paint_box(layer3d, 26, 16, 2, 2, 1, (48, 245, 255, 255), (24, 168, 198, 255), (186, 255, 255, 255))   # pressure gauge
    _paint_box(glow3d, 26, 16, 2, 2, 1, (48, 245, 255, 255), (32, 200, 232, 255), (210, 255, 255, 255))
    g.save(layer3d, g.ASSETS / "textures/entity/ocean/abyssal_diving_3d.png")
    g.save(glow3d, g.ASSETS / "textures/entity/ocean/abyssal_diving_3d_glow.png")


def _build_entity_skins(g):
    def save_pair(name, skin, glow=None):
        g.save(skin, g.ASSETS / f"textures/entity/ocean/{name}.png")
        if glow is not None:
            g.save(glow, g.ASSETS / f"textures/entity/ocean/{name}_glow.png")

    # 1. Barnacle-Hulk Dredger (128x128)
    s = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 12, 14, 8, (46, 84, 78, 255), (32, 58, 54, 255), (72, 122, 114, 255))          # torso
    _paint_box(s, 40, 0, 12, 3, 10, (168, 114, 48, 255), (112, 74, 30, 255), (206, 150, 72, 255))      # collar
    _paint_box(s, 0, 22, 10, 10, 10, (176, 122, 50, 255), (116, 78, 30, 255), (212, 158, 76, 255))     # bronze head
    _rect(s, 12, 34, 6, 4, (48, 245, 255, 255))                                                         # cyan grille visor on front face
    _rect(gl, 12, 34, 6, 4, (48, 245, 255, 255))
    _paint_box(s, 40, 22, 7, 6, 7, (192, 108, 126, 255), (132, 68, 86, 255), (234, 148, 168, 255))     # R coral pauldron
    _paint_box(s, 68, 22, 7, 6, 7, (76, 134, 152, 255), (52, 92, 108, 255), (118, 178, 198, 255))      # L pale-blue pauldron
    _paint_box(s, 0, 42, 5, 14, 5, (52, 92, 86, 255))
    _paint_box(s, 20, 42, 5, 14, 5, (52, 92, 86, 255))
    _paint_box(s, 40, 42, 6, 5, 6, (186, 128, 52, 255))
    _paint_box(s, 64, 42, 4, 10, 4, (192, 138, 58, 255))                                                # twin back tanks
    _paint_box(s, 80, 42, 6, 2, 3, (94, 106, 118, 255))
    _paint_box(s, 0, 62, 5, 12, 5, (48, 82, 78, 255))
    _paint_box(s, 20, 62, 6, 4, 2, (176, 122, 50, 255))
    _paint_box(s, 40, 62, 1, 12, 1, (104, 114, 124, 255))
    _paint_box(s, 48, 62, 12, 12, 3, (96, 108, 120, 255), (58, 66, 76, 255), (144, 158, 172, 255))      # Anchor Flail head
    _paint_box(s, 84, 0, 3, 5, 2, (242, 124, 154, 255))                                                 # pink coral branch
    _paint_box(s, 94, 0, 2, 4, 2, (148, 224, 245, 255))                                                 # blue coral sprig
    save_pair("barnacle_hulk_dredger", s, gl)

    # 2. Siren-Veil Jelly (64x64 translucent + glowing core)
    s = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    gl = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 14, 12, 14, (74, 58, 186, 165), (48, 36, 138, 150), (118, 108, 232, 180))       # outer translucent indigo bell
    _paint_box(s, 0, 26, 10, 3, 10, (124, 92, 238, 220))                                                # crown cap
    _paint_box(s, 0, 39, 8, 6, 8, (224, 62, 235, 200))                                                  # magenta inner bell
    _paint_box(s, 32, 39, 4, 4, 4, (196, 255, 255, 255))                                                # crystal core
    _paint_box(gl, 32, 39, 4, 4, 4, (196, 255, 255, 255))
    _paint_box(gl, 0, 39, 8, 6, 8, (200, 48, 220, 160))
    _paint_box(s, 48, 26, 1, 16, 1, (96, 178, 248, 220))
    _paint_box(s, 52, 26, 1, 12, 1, (224, 118, 255, 235))
    save_pair("siren_veil_jelly", s, gl)

    # 3. Coral-Spine Mako (128x64)
    s = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    gl = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 10, 7, 12, (28, 72, 136, 255), (20, 50, 98, 255), (44, 104, 178, 255))
    _rect(s, 2, 14, 2, 2, (255, 196, 48, 255))                                                           # amber eyes on flanks
    _rect(s, 22, 14, 2, 2, (255, 196, 48, 255))
    _rect(gl, 2, 14, 2, 2, (255, 196, 48, 255))
    _rect(gl, 22, 14, 2, 2, (255, 196, 48, 255))
    _paint_box(s, 44, 0, 8, 3, 10, (174, 196, 212, 255))                                                # pale belly jaw
    _paint_box(s, 0, 19, 10, 9, 14, (32, 82, 148, 255), (168, 192, 208, 255), (48, 112, 186, 255))      # cobalt dorsal / pale belly
    _paint_box(s, 48, 19, 8, 7, 10, (32, 82, 148, 255), (168, 192, 208, 255), (48, 112, 186, 255))
    _paint_box(s, 84, 19, 5, 5, 8, (30, 74, 136, 255))
    _paint_box(s, 0, 42, 2, 12, 6, (26, 68, 128, 255))
    _paint_box(s, 16, 42, 2, 8, 5, (40, 94, 158, 255))
    _paint_box(s, 30, 42, 2, 8, 6, (26, 68, 128, 255))
    _paint_box(s, 46, 42, 10, 2, 6, (34, 88, 154, 255))
    _paint_box(s, 46, 50, 5, 1, 4, (96, 142, 192, 255))
    _paint_box(s, 78, 42, 2, 4, 6, (252, 116, 138, 255), (214, 78, 102, 255), (255, 168, 182, 255))     # coral spine crest
    _paint_box(gl, 78, 42, 2, 4, 6, (238, 92, 120, 255))
    save_pair("coral_spine_mako", s, gl)

    # 4. Dread-Nautilus Sentinel (128x64)
    s = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    gl = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 10, 20, 20, (56, 124, 112, 255), (38, 86, 78, 255), (176, 118, 54, 255))        # verdigris shell + bronze keel
    _paint_box(s, 60, 0, 2, 6, 6, (188, 134, 54, 255), (124, 84, 34, 255), (222, 168, 78, 255))         # bronze spiral hubs
    _paint_box(s, 60, 12, 8, 5, 5, (32, 68, 66, 255))
    _paint_box(s, 60, 22, 6, 6, 4, (20, 44, 46, 255))
    _paint_box(s, 80, 22, 4, 4, 2, (56, 248, 255, 255))                                                 # sea-lantern cyan eye
    _paint_box(gl, 80, 22, 4, 4, 2, (56, 248, 255, 255))
    _paint_box(s, 60, 32, 2, 2, 8, (168, 116, 48, 255))
    _paint_box(s, 80, 32, 3, 3, 6, (202, 148, 62, 255))
    _paint_box(gl, 80, 32, 3, 3, 6, (48, 212, 224, 160))
    _paint_box(s, 98, 32, 2, 2, 5, (72, 178, 168, 255))
    save_pair("dread_nautilus_sentinel", s, gl)

    # 5. Thalassor Boss (128x128)
    s = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 14, 10, 14, (24, 34, 48, 255), (14, 22, 32, 255), (42, 64, 84, 255))
    _paint_box(s, 56, 0, 10, 6, 10, (26, 42, 58, 255))
    _paint_box(s, 0, 24, 10, 4, 12, (20, 62, 74, 255), (14, 44, 52, 255), (68, 245, 232, 255))          # glowing aquamarine inner maw
    _paint_box(gl, 0, 24, 10, 4, 12, (64, 245, 235, 190))
    _paint_box(s, 44, 24, 10, 6, 2, (54, 228, 222, 255))                                                # crown
    _paint_box(gl, 44, 24, 10, 6, 2, (54, 228, 222, 255))
    _paint_box(s, 68, 24, 3, 10, 3, (172, 144, 72, 255))                                                # curved horns
    _paint_box(s, 80, 24, 1, 10, 8, (48, 224, 212, 220))
    _paint_box(gl, 80, 24, 1, 10, 8, (48, 224, 212, 220))
    _paint_box(s, 0, 42, 16, 2, 10, (24, 68, 82, 255), (14, 42, 54, 255), (64, 224, 212, 255))
    _paint_box(s, 0, 56, 12, 12, 10, (22, 32, 46, 255), (18, 58, 66, 255), (34, 52, 72, 255))           # repeating obsidian segments
    _rect(s, 2, 68, 6, 3, (48, 245, 255, 255))                                                           # cyan flank runes on segments
    _rect(s, 24, 68, 6, 3, (48, 245, 255, 255))
    _rect(gl, 2, 68, 6, 3, (48, 245, 255, 255))
    _rect(gl, 24, 68, 6, 3, (48, 245, 255, 255))
    _paint_box(s, 44, 56, 2, 7, 8, (54, 232, 224, 225))
    _paint_box(gl, 44, 56, 2, 7, 8, (54, 232, 224, 255))
    _paint_box(s, 64, 56, 10, 2, 12, (28, 72, 86, 255))
    save_pair("thalassor", s, gl)

    # 6. Brine-Merchants x3 professions (64x64 each)
    profs = {
        "brine_merchant_jeweler": ((186, 134, 56), (34, 72, 118), (64, 232, 245)),
        "brine_merchant_cartographer": ((156, 112, 48), (28, 96, 86), (222, 198, 132)),
        "brine_merchant_alchemist": ((32, 98, 48), (38, 124, 68), (206, 98, 248)),
    }
    for pname, (helm_c, robe_c, prop_c) in profs.items():
        m = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        _paint_box(m, 0, 0, 8, 10, 8, (182, 138, 108, 255))
        _paint_box(m, 0, 18, 10, 10 if "alchemist" not in pname else 11, 10, (*helm_c, 220))
        _rect(m, 12, 30, 6, 4, (64, 232, 245, 210))
        _paint_box(m, 32, 0, 8, 12, 6, (*robe_c, 255))
        _paint_box(m, 0, 39, 8, 10, 6, (*robe_c, 255))
        _paint_box(m, 28, 39, 4, 12, 4, (*robe_c, 255))
        _paint_box(m, 44, 39, 4, 12, 4, (58, 44, 34, 255))
        _paint_box(m, 40, 18, 3, 8, 3, (192, 138, 56, 255))
        _paint_box(m, 0, 55, 3, 4, 3, (255, 224, 96, 255))
        _paint_box(m, 12, 55, 2, 4, 4, (148, 92, 48, 255))
        _paint_box(m, 24, 55, 10, 7, 1, (226, 212, 168, 255))
        _paint_box(m, 46, 55, 4, 4, 4, (*prop_c, 255))
        save_pair(pname, m)

    # 7. Lumi-Seahorse (64x64 translucent + bio-glow)
    s = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    gl = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 6, 10, 6, (48, 182, 198, 245), (32, 126, 146, 245), (96, 236, 238, 255))
    _paint_box(s, 24, 0, 5, 6, 5, (52, 196, 206, 245))
    _paint_box(s, 0, 16, 6, 6, 6, (58, 204, 212, 245))
    _paint_box(s, 24, 16, 2, 2, 5, (86, 234, 236, 245))
    _paint_box(s, 38, 16, 1, 4, 5, (92, 255, 226, 220))
    _paint_box(gl, 38, 16, 1, 4, 5, (92, 255, 226, 255))
    _paint_box(s, 44, 0, 2, 3, 1, (92, 255, 226, 220))
    _paint_box(s, 0, 28, 1, 6, 6, (88, 255, 218, 210))                                                  # translucent sea-mint dorsal fin
    _paint_box(gl, 0, 28, 1, 6, 6, (88, 255, 218, 235))
    _paint_box(s, 14, 28, 4, 4, 4, (44, 168, 186, 245))
    _paint_box(s, 30, 28, 3, 3, 4, (40, 148, 168, 245))
    _paint_box(s, 44, 28, 2, 2, 3, (36, 128, 152, 245))
    _paint_box(s, 0, 40, 7, 4, 7, (176, 122, 48, 255), (122, 82, 30, 255), (218, 164, 72, 255))         # saddle
    _paint_box(s, 28, 40, 1, 4, 1, (208, 158, 62, 255))
    save_pair("lumi_seahorse", s, gl)

    # 8. Pearl-Pup (64x32 + glowing lure pearl)
    s = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    gl = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 8, 7, 12, (238, 244, 248, 255), (248, 252, 255, 255), (168, 188, 206, 255))     # white belly, slate dorsal spots
    _rect(s, 14, 2, 2, 2, (98, 118, 138, 255))
    _rect(s, 16, 7, 2, 3, (98, 118, 138, 255))
    _paint_box(s, 0, 19, 7, 6, 7, (238, 244, 248, 255), (248, 252, 255, 255), (168, 188, 206, 255))
    _rect(s, 8, 28, 1, 1, (22, 28, 36, 255))
    _rect(s, 12, 28, 1, 1, (22, 28, 36, 255))
    _paint_box(s, 28, 19, 6, 4, 5, (232, 238, 244, 255))
    _paint_box(s, 50, 19, 3, 2, 2, (250, 252, 255, 255))
    _paint_box(s, 50, 23, 3, 3, 3, (112, 252, 255, 255))                                                # glowing forehead lure pearl
    _paint_box(gl, 50, 23, 3, 3, 3, (112, 252, 255, 255))
    _paint_box(s, 60, 19, 1, 5, 1, (132, 168, 182, 255))
    _paint_box(s, 40, 0, 5, 2, 4, (206, 218, 228, 255))
    _paint_box(s, 40, 6, 4, 2, 5, (192, 206, 218, 255))
    save_pair("pearl_pup", s, gl)

    # 9. Singing Moon-Whale (256x128 + emissive cyan star-barnacles)
    s = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    gl = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
    _paint_box(s, 0, 0, 22, 16, 26, (34, 62, 118, 255), (22, 42, 86, 255), (52, 92, 162, 255))
    _paint_box(s, 0, 42, 20, 8, 24, (218, 206, 168, 255), (178, 162, 124, 255), (244, 228, 178, 255))   # pale-gold baleen jaw
    _paint_box(s, 96, 0, 8, 2, 8, (64, 114, 186, 255))
    _paint_box(s, 0, 74, 22, 18, 28, (32, 58, 112, 255), (176, 188, 212, 255), (48, 86, 152, 255))
    _paint_box(s, 132, 0, 2, 5, 7, (44, 82, 148, 255))
    _paint_box(s, 132, 16, 22, 2, 10, (42, 78, 142, 255))
    _paint_box(s, 100, 32, 12, 10, 18, (30, 56, 108, 255))
    _paint_box(s, 100, 64, 26, 2, 10, (44, 82, 148, 255))
    # Emissive 4-point cyan star-barnacles on whale dorsal surface & fin rims
    for bx, by in ((32, 12), (48, 18), (42, 84), (62, 90), (144, 18), (112, 44)):
        _rect(s, bx, by, 3, 3, (124, 250, 255, 255))
        _rect(gl, bx, by, 3, 3, (124, 250, 255, 255))
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
    g.adv(
        "ocean/sanctuary_trader", "ocean/deep_diver", g.item("wildercord:abyssal_pearl"),
        LANG["advancements.wildercord.ocean.sanctuary_trader.title"],
        LANG["advancements.wildercord.ocean.sanctuary_trader.description"],
        {"trigger": "minecraft:villager_trade", "conditions": {"villager": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "wildercord:brine_merchant"}}]}},
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
        {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "wildercord:thalassor"}}]}},
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
