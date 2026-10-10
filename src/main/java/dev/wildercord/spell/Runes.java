package dev.wildercord.spell;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dev.wildercord.spell.Trait.BOUNCE;
import static dev.wildercord.spell.Trait.CHAIN;
import static dev.wildercord.spell.Trait.DURATION;
import static dev.wildercord.spell.Trait.FRUGAL;
import static dev.wildercord.spell.Trait.HOMING;
import static dev.wildercord.spell.Trait.LINGER;
import static dev.wildercord.spell.Trait.VOLLEY;
import static dev.wildercord.spell.Trait.PIERCE;
import static dev.wildercord.spell.Trait.POWER;
import static dev.wildercord.spell.Trait.RADIUS;
import static dev.wildercord.spell.Trait.SHARE;
import static dev.wildercord.spell.Trait.SPEED;
import static dev.wildercord.spell.Trait.SPLIT;

/** The built-in rune roster, in Codex order. Numbers match docs/DESIGN.md. */
public final class Runes {
	private Runes() {}

	private static final Map<String, RuneDef> ALL = new LinkedHashMap<>();

	// ---- Shapes
	public static final RuneDef SELF = shape("self", "Self", 1, 0, 1.0, "Targets you.");
	public static final RuneDef TOUCH = shape("touch", "Touch", 1, 1, 1.0, "Targets what you're looking at, within reach.", CHAIN);
	public static final RuneDef BOLT = shape("bolt", "Bolt", 1, 3, 1.1, "Fires a flying bolt, up to 48 blocks.", SPEED, PIERCE, BOUNCE, HOMING, CHAIN, SPLIT, VOLLEY);
	public static final RuneDef BEAM = shape("beam", "Beam", 2, 3, 1.2, "An instant line that hits the first thing within 24 blocks.", PIERCE, CHAIN, SPLIT, VOLLEY);
	public static final RuneDef BURST = shape("burst", "Burst", 2, 6, 1.5, "Hits everything within 4 blocks.", RADIUS, SPLIT);
	public static final RuneDef ZONE = shape("zone", "Zone", 3, 8, 2.75, "A 3-block field where you look. Re-applies every second for 6 seconds.", RADIUS, DURATION, SPEED, SPLIT);
	public static final RuneDef RAIN = shape("rain", "Rain", 3, 10, 2.5, "5 strikes from the sky over 2 seconds, around where you look.", RADIUS, SPLIT);
	public static final RuneDef ARC = shape("arc", "Arc", 1, 3, 1.25, "Lobs a bolt that falls and bursts where it lands.", SPEED, BOUNCE, SPLIT, VOLLEY);
	public static final RuneDef CONE = shape("cone", "Cone", 2, 5, 1.4, "Sweeps everything in a 60-degree cone up to 6 blocks in front of you.", RADIUS);
	public static final RuneDef TRAIL = shape("trail", "Trail", 2, 7, 2.85, "For 5 seconds your footsteps leave a path that hits whatever steps on it.", DURATION);
	public static final RuneDef WALL = shape("wall", "Wall", 3, 10, 2.95, "A 7-block wall across where you look. Hits whatever crosses it every second for 5 seconds, slows what touches it and turns projectiles aside.", RADIUS, DURATION, SPEED);
	public static final RuneDef ORBIT = shape("orbit", "Orbit", 3, 9, 3.05, "Three orbs circle you for 8 seconds and hit whatever they touch.", DURATION, SPLIT);
	public static final RuneDef RING = shape("ring", "Ring", 2, 6, 1.7, "A hollow ring expands from you out to 7 blocks, hitting everything it passes but sparing the ground right around you.", RADIUS);
	public static final RuneDef PILLAR = shape("pillar", "Pillar", 2, 5, 1.4, "A column erupts where you look: hits everything within 1.5 blocks, 6 high.", RADIUS, SPLIT);
	public static final RuneDef WAVE = shape("wave", "Wave", 2, 6, 1.5, "A 3-wide wave rolls 14 blocks forward along the ground.", RADIUS, SPEED);
	public static final RuneDef MINE = shape("mine", "Mine", 2, 5, 1.3, "Hides a rune where you look. It fires when an enemy steps near (lasts 30 seconds).", RADIUS, SPLIT);
	public static final RuneDef TOTEM = shape("totem", "Totem", 3, 10, 3.15, "A floating totem where you look pulses every 2 seconds for 10 seconds.", RADIUS, DURATION, SPEED);
	public static final RuneDef DOMAIN = shape("domain", "Domain", 4, 20, 3.9, "Expands a 9-block domain around you for 6 seconds. Every second everything inside is struck, and enemies inside are slowed.", RADIUS, DURATION, SPEED);
	public static final RuneDef CRESCENT = shape("crescent", "Crescent", 2, 5, 1.4, "A crescent slash flies 16 blocks forward, cutting everything in its 5-wide path.", RADIUS, SPEED, SPLIT, VOLLEY);
	public static final RuneDef BARRAGE = shape("barrage", "Barrage", 2, 5, 1.85, "A flurry of 8 blows in one second on everything right in front of you, each at 35% power.", SPEED);
	public static final RuneDef ORB = shape("orb", "Orb", 3, 9, 2.0, "A slow, heavy orb you steer with your aim drifts up to 30 blocks through creatures, striking everything within 2 blocks of it once a second.", RADIUS, SPEED, SPLIT);
	public static final RuneDef BLITZ = shape("blitz", "Blitz", 2, 6, 1.5, "You flash up to 8 blocks forward in an instant, striking everything you pass through.", RADIUS);
	// Batch 6: sparks, energy balls and beams.
	public static final RuneDef SPARK = shape("spark", "Spark", 1, 1, 1.0, "A quick spark darts up to 16 blocks and hits the first thing in its path, at 75% power.", SPEED, SPLIT, VOLLEY);
	public static final RuneDef RAY = shape("ray", "Ray", 1, 2, 1.15, "An instant, short ray that hits the first thing within 10 blocks.", PIERCE, CHAIN);
	public static final RuneDef NOVA = shape("nova", "Nova", 1, 3, 1.3, "A small nova bursts from you, hitting everything within 2.5 blocks.", RADIUS);
	public static final RuneDef WISP = shape("wisp", "Wisp", 2, 4, 1.3, "A wisp drifts out and chases the nearest enemy within 16 blocks for up to 4 seconds, striking the first thing it touches.", SPEED, SPLIT);
	public static final RuneDef COMET = shape("comet", "Comet", 2, 5, 1.6, "A heavy ball of energy flies up to 24 blocks and bursts on the first thing it touches, hitting everything within 3 blocks.", RADIUS, SPEED, SPLIT, VOLLEY);
	public static final RuneDef RICOCHET = shape("ricochet", "Ricochet", 2, 5, 1.5, "An orb that bounces off the ground and walls 4 times, passing through creatures and hitting each once.", BOUNCE, SPEED, SPLIT);
	public static final RuneDef CLUSTER = shape("cluster", "Cluster", 2, 6, 1.5, "A ball of energy that breaks into five shards where it hits; each shard strikes everything within 1.5 blocks of where it lands.", RADIUS, SPEED, SPLIT);
	public static final RuneDef LANCE = shape("lance", "Lance", 2, 5, 1.5, "A thick lance of light drives 16 blocks forward, through every creature in its path.", RADIUS, SPLIT);
	public static final RuneDef SWEEP = shape("sweep", "Sweep", 2, 5, 1.8, "A 10-block beam sweeps across in front of you in half a second, hitting everything it crosses once.", RADIUS, SPEED);
	public static final RuneDef PRISM = shape("prism", "Prism", 2, 5, 1.5, "A beam that splits into three at the first thing it hits, each ray striking the next creature behind it.", SPLIT);
	public static final RuneDef STREAM = shape("stream", "Stream", 2, 5, 1.95, "A steady stream of energy follows your aim for a second, striking the first thing within 20 blocks 6 times at 35% power.", SPEED);

	/** Not a real rune: the implicit shape after a link, meaning "whatever triggered it". */
	public static final RuneDef TRIGGER = new RuneDef("wildercord:trigger", "Target", RuneFamily.SHAPE, 0, 0, 1.0, "", EffectKind.NONE, Set.of(), "", "Whatever triggered the link.", "personal");

	// ---- Effects
	// ---- Selectable circle disciplines. Kept on one line for asset/recipe generation.
	public static final RuneDef NEEDLE_CIRCLE = modifier("needle_circle", "Needle Circle", 2, 1.2, Trait.CIRCLE, "A closing iris: 35% smaller shape radius, 20% more power. Costs 20% more mana. One circle discipline per shape.");
	public static final RuneDef BLOOM_CIRCLE = modifier("bloom_circle", "Bloom Circle", 2, 1.15, Trait.CIRCLE, "Six unfolding petals: 35% larger shape radius, 20% less power. Costs 15% more mana.");
	public static final RuneDef GYRE_CIRCLE = modifier("gyre_circle", "Gyre Circle", 2, 1.15, Trait.CIRCLE, "Counter-turning turbines: flying shapes travel 30% faster at 15% less power. Costs 15% more mana.");
	public static final RuneDef ANCHOR_CIRCLE = modifier("anchor_circle", "Anchor Circle", 2, 1.25, Trait.CIRCLE, "A locked square lattice: effect durations last 40% longer at 15% less power. Costs 25% more mana; terrain lifetime caps still apply.");
	public static final RuneDef RESERVOIR_CIRCLE = modifier("reservoir_circle", "Reservoir Circle", 1, 0.75, Trait.CIRCLE, "Filling concentric basins: 25% less mana, 20% less power and 15% shorter effect durations.");
	public static final RuneDef CRUCIBLE_CIRCLE = modifier("crucible_circle", "Crucible Circle", 2, 1.2, Trait.CIRCLE, "A breathing furnace hexagon: 15% more power and 25% shorter effect durations. Costs 20% more mana.");
	public static final RuneDef CONFLUENCE_CIRCLE = modifier("confluence_circle", "Confluence Circle", 3, 1.2, Trait.CIRCLE, "Braided elemental satellites: power starts at 90%, gaining 8% per distinct visual element in this group, up to 130%. Costs 20% more mana.");
	public static final RuneDef PILGRIM_CIRCLE = modifier("pilgrim_circle", "Pilgrim Circle", 2, 1.1, Trait.CIRCLE, "A rolling compass: 15% more power when released while moving horizontally, otherwise 10% less; 10% shorter effect durations. Costs 10% more mana.");
	public static final RuneDef VIGIL_CIRCLE = modifier("vigil_circle", "Vigil Circle", 2, 1.15, Trait.CIRCLE, "An opening watchful eye: 20% more power if crouching on release, otherwise 10% less. Flying shapes move 15% slower. Costs 15% more mana.");
	public static final RuneDef MERCY_CIRCLE = modifier("mercy_circle", "Mercy Circle", 2, 1.15, Trait.CIRCLE, "Paired sheltering crescents: helpful effects gain 20% power; other effects lose 25%. Costs 15% more mana.");
	public static final RuneDef TEMPEST_CIRCLE = modifier("tempest_circle", "Tempest Circle", 3, 1.2, Trait.CIRCLE, "Forked storm spokes: 20% more power when wet or exposed to rain on release, otherwise 10% less. Flying shapes move 10% faster. Costs 20% more mana.");
	public static final RuneDef ECLIPSE_CIRCLE = modifier("eclipse_circle", "Eclipse Circle", 3, 1.15, Trait.CIRCLE, "A moon passing its sun: 20% more power at night on release, otherwise 10% less. Costs 15% more mana.");
	public static final RuneDef FEATHER_FALL = effect("feather_fall", "Feather Fall", 1, 6, "wind", EffectKind.HELPFUL, "Slow falling and no fall damage for 12 seconds, and you drift the way you look while you fall (sneak to stop).", DURATION);
	public static final RuneDef SWIFT = effect("swift", "Swift", 1, 6, "wind", EffectKind.HELPFUL, "Speed III for 10 seconds, and it shakes off Slowness and frozen skin.", DURATION, POWER);
	public static final RuneDef NIGHT_EYE = effect("night_eye", "Night Eye", 1, 3, "arcane", EffectKind.HELPFUL, "Night vision for 60 seconds.", DURATION);
	public static final RuneDef HEAL = effect("heal", "Heal", 1, 12, "life", EffectKind.HELPFUL, "Restores 8 health (4 hearts; repeats within one cast heal less). What the target can't use becomes a shield of up to 2 hearts for 10 seconds.", POWER, LINGER);
	public static final RuneDef HARM = effect("harm", "Harm", 1, 8, "arcane", EffectKind.HARMFUL, "7 magic damage, and it leaves the target exposed for 3 seconds (an arcane mark: Unweave and Prismatic Burst count it).", POWER, LINGER);
	public static final RuneDef PUSH = effect("push", "Push", 1, 4, "wind", EffectKind.HARMFUL, "Hurls targets away from the spell.", POWER);
	public static final RuneDef LIGHT = effect("light", "Light", 1, 2, "arcane", EffectKind.WORLD, "A light source at the point for 60 seconds.", DURATION);
	public static final RuneDef GROW = effect("grow", "Grow", 1, 4, "life", EffectKind.WORLD, "Bone-meals the block that was hit and everything around it, and young animals there grow up.", POWER);
	public static final RuneDef SHIELD = effect("shield", "Shield", 2, 12, "earth", EffectKind.HELPFUL, "For 30 seconds, the next harmful spell cast at the target meets magic circles that spawn in front of it and stop it. A spell that cost more mana than the one that raised the Shield shatters them and goes through. The stronger the Shield, the more circles stack.", POWER, DURATION);
	public static final RuneDef LAUNCH = effect("launch", "Launch", 2, 8, "wind", EffectKind.HARMFUL, "Flings targets high into the air, where every spell hits them harder. On Self it rockets you up and forward.", POWER);
	public static final RuneDef DASH = effect("dash", "Dash", 2, 6, "wind", EffectKind.HARMFUL, "Shoves targets the way you're facing. On Self it's a precise dash of about ten blocks: level, and it stops where it should.", POWER);
	public static final RuneDef PULL = effect("pull", "Pull", 2, 5, "void", EffectKind.HARMFUL, "Pulls targets toward the spell, leaving them staggered and marked as pulled for 4 seconds.", POWER);
	public static final RuneDef FIRE = effect("fire", "Fire", 2, 8, "fire", EffectKind.HARMFUL, "5 fire damage and sets alight for 6 seconds.", POWER, DURATION, LINGER);
	public static final RuneDef FROST = effect("frost", "Frost", 2, 8, "frost", EffectKind.HARMFUL, "5 freeze damage and Slowness III for 4 seconds; the frost leaves it brittle for Shatter for 4 seconds.", POWER, DURATION, LINGER);
	public static final RuneDef BREAK = effect("break", "Break", 2, 4, "earth", EffectKind.WORLD, "Mines the block that was hit (up to iron-pickaxe hardness; Amplify for diamond).", POWER);
	public static final RuneDef LIGHTNING = effect("lightning", "Lightning", 3, 20, "storm", EffectKind.HARMFUL, "A 12-damage lightning strike on each target that slows and burns. An enemy takes the strongest strike of a cast once, however many land beside it. You and your allies are immune.", POWER, LINGER);
	public static final RuneDef BLINK = effect("blink", "Blink", 3, 15, "void", EffectKind.MOVEMENT, "Teleports you to where the spell landed (max 40 blocks).");
	public static final RuneDef EXPLODE = effect("explode", "Explode", 3, 18, "fire", EffectKind.HARMFUL, "12 damage in a 3.5-block blast that throws what it hits. Blasts of one cast never stack on one enemy. Never breaks blocks.", POWER, RADIUS);
	public static final RuneDef SONIC_BOOM = effect("sonic_boom", "Sonic Boom", 4, 35, "void", EffectKind.HARMFUL, "16 damage that ignores armour, and everything else on the line between you and the target takes 8, through walls.", POWER);
	public static final RuneDef WITHER = effect("wither", "Wither", 4, 25, "void", EffectKind.HARMFUL, "Wither IV for 6 seconds: it spreads to whoever strikes it in melee, and the withered can't heal.", DURATION);
	public static final RuneDef DRAGON_BREATH = effect("dragon_breath", "Dragon Breath", 4, 30, "void", EffectKind.HARMFUL, "A 3-block cloud that rolls on along the way you blew it: 5 damage per second for 5 seconds.", POWER, DURATION, RADIUS);
	public static final RuneDef SHOCK = effect("shock", "Shock", 1, 7, "storm", EffectKind.HARMFUL, "4 lightning damage that arcs to one more enemy nearby: it finds a wet or metal-armoured one first.", POWER, LINGER);
	public static final RuneDef HASTE = effect("haste", "Haste", 1, 4, "arcane", EffectKind.HELPFUL, "Haste II for 30 seconds: mine and swing faster, and a charged cast fills 30% sooner.", DURATION, POWER);
	public static final RuneDef REVEAL = effect("reveal", "Reveal", 1, 3, "arcane", EffectKind.HARMFUL, "Makes targets glow through walls for 15 seconds, strips their invisibility and leaves them exposed for as long (an arcane mark).", DURATION);
	public static final RuneDef REGROWTH = effect("regrowth", "Regrowth", 2, 10, "life", EffectKind.HELPFUL, "Regeneration that takes hold: I for 3 seconds, II for 3, III for 2 (about 7 health).", POWER, DURATION);
	public static final RuneDef CLEANSE = effect("cleanse", "Cleanse", 2, 8, "life", EffectKind.HELPFUL, "Washes away harmful effects, fire and every elemental mark.");
	public static final RuneDef STONESKIN = effect("stoneskin", "Stoneskin", 2, 12, "earth", EffectKind.HELPFUL, "Resistance II for 10 seconds, and Slowness I: stone is heavy.", POWER, DURATION);
	public static final RuneDef ROOT = effect("root", "Root", 2, 9, "earth", EffectKind.HARMFUL, "Vines hold targets in place for 3 seconds.", DURATION, LINGER);
	public static final RuneDef VEIL = effect("veil", "Veil", 2, 10, "void", EffectKind.HELPFUL, "Invisibility for 12 seconds, and nearby monsters lose track of you. The first damage you deal from it lands half again as hard, and ends it.", DURATION);
	public static final RuneDef EMPOWER = effect("empower", "Empower", 2, 12, "arcane", EffectKind.HELPFUL, "Strength II for 10 seconds, then Weakness I for 4 (a passive carries only Strength I).", POWER, DURATION);
	public static final RuneDef LEVITATE = effect("levitate", "Levitate", 2, 8, "wind", EffectKind.HARMFUL, "Targets hang in the air for 3 seconds, their drift stopped, and every spell hits them harder while they're off the ground. On Self you float.", DURATION, LINGER);
	public static final RuneDef FREEZE = effect("freeze", "Freeze", 3, 14, "frost", EffectKind.HARMFUL, "Freezes targets solid for 2.5 seconds: they can't move or fight back.", DURATION, POWER, LINGER);
	public static final RuneDef METEOR = effect("meteor", "Meteor", 3, 24, "fire", EffectKind.HARMFUL, "A burning meteor falls on each target (two at most) 1.2 seconds later: 12 damage in a 3.5-block blast, and the crater burns on for a moment.", POWER, RADIUS, LINGER);
	public static final RuneDef TREMOR = effect("tremor", "Tremor", 3, 18, "earth", EffectKind.HARMFUL, "The ground erupts: 8 damage to enemies within 4 blocks, throwing them up.", POWER, RADIUS, LINGER);
	public static final RuneDef GRAVITY_WELL = effect("gravity_well", "Gravity Well", 3, 16, "void", EffectKind.HARMFUL, "Drags every enemy within 7 blocks into the point for 2 seconds and pulls what hovers over it down; they're left pulled.", POWER, RADIUS, DURATION);
	public static final RuneDef SUMMON = effect("summon", "Summon", 4, 30, "arcane", EffectKind.HELPFUL, "Three spirit wolves fight at your side for 20 seconds. While they live, your mana regenerates a quarter slower.", DURATION, POWER);
	public static final RuneDef VENOM = effect("venom", "Venom", 2, 8, "life", EffectKind.HARMFUL, "2 damage now and 0.75 a second for 4 seconds, with Poison I: it can kill and works on undead and spiders. The poisoned pass it on, once, to up to 3 enemies within 2.5 blocks.", POWER, DURATION, LINGER);
	public static final RuneDef SMITE = effect("smite", "Smite", 3, 16, "arcane", EffectKind.HARMFUL, "A ring closes at the target's feet; 0.7 seconds later a column of light deals 13 holy damage (doubled against undead) and strips Absorption.", POWER, LINGER);
	public static final RuneDef INFERNO = effect("inferno", "Inferno", 3, 20, "fire", EffectKind.HARMFUL, "Everything within 4 blocks burns: 3 fire damage a second for 4 seconds.", POWER, DURATION, RADIUS);
	public static final RuneDef THUNDERCLAP = effect("thunderclap", "Thunderclap", 2, 12, "storm", EffectKind.HARMFUL, "A flash, then a crack of thunder: 5 damage within 3 blocks, and everything hit is stunned for half a second and, if it is a monster, forgets who it was hunting.", POWER, RADIUS, LINGER);
	public static final RuneDef STARFALL = effect("starfall", "Starfall", 4, 32, "arcane", EffectKind.HARMFUL, "Eight falling stars around the point over 2 seconds: 6 damage each. The first stars go to exposed enemies.", POWER, RADIUS);
	public static final RuneDef BLIND = effect("blind", "Blind", 1, 5, "void", EffectKind.HARMFUL, "Blindness and darkness for 5 seconds (3 on players). A blinded monster lashes out at whatever stands next to it.", DURATION);
	public static final RuneDef CHILL = effect("chill", "Chill", 1, 4, "frost", EffectKind.HARMFUL, "Slowness II for 6 seconds and 1 freeze damage. Chill again within 6 seconds and the cold deepens (III, then IV).", DURATION, LINGER);
	public static final RuneDef SILENCE = effect("silence", "Silence", 2, 9, "arcane", EffectKind.HARMFUL, "Casters can't cast: a cast in hand is cut short and none can follow for 4 seconds (3 on players). Monsters are weakened for 6 seconds.", DURATION);
	public static final RuneDef FIREWARD = effect("fireward", "Fireward", 2, 8, "fire", EffectKind.HELPFUL, "Fire resistance for 30 seconds.", DURATION);
	public static final RuneDef NOURISH = effect("nourish", "Nourish", 1, 6, "life", EffectKind.HELPFUL, "Restores 6 hunger and some saturation, and ends Hunger. Fed pets heal 6 and are ready to breed.", POWER);
	public static final RuneDef TIDEBREATH = effect("tidebreath", "Tidebreath", 1, 4, "frost", EffectKind.HELPFUL, "Water breathing and faster swimming for 30 seconds, and it douses you: fire goes out, and fire hits are softer while you drip.", DURATION);
	public static final RuneDef LEAP = effect("leap", "Leap", 1, 4, "wind", EffectKind.HELPFUL, "Jump Boost III for 15 seconds.", DURATION, POWER);
	public static final RuneDef GRAPPLE = effect("grapple", "Grapple", 2, 8, "void", EffectKind.MOVEMENT, "Pulls you to where the spell hit, and stops you there.", POWER);
	public static final RuneDef WATCHWEFT = effect("watchweft", "Watchweft", 2, 8, "arcane", EffectKind.WORLD, "Watches a visible dry floor for forty-five seconds. Gives its owner one private warning only after a visible monster targeting them crosses into three blocks in consecutive complete samples. Remain within sixteen blocks; ninety-second rest.");
	public static final RuneDef MANABRAID = effect("manabraid", "Manabraid", 2, 4, "arcane", EffectKind.HELPFUL, "Offers an allied connected caster a three-second mana gift. They must release and freshly crouch to accept. After the spell price, spends at most twenty-four extra mana to restore at most sixteen, keeping two. Donor rests thirty seconds; receiver ten.");
	public static final RuneDef ROOT_CARRY = effect("root_carry", "Root Carry", 2, 8, "life", EffectKind.WORLD, "Two separate casts select and move one unchanged young Cinder Fern onto nearby visible soil. Selection lasts fifteen seconds; successful relocation starts a twenty-second rest. Refuses grown, changed, hidden, protected or unloaded roots.");
	public static final RuneDef HARVEST = effect("harvest", "Harvest", 1, 3, "life", EffectKind.WORLD, "Harvests grown crops around the block hit, and replants them.", RADIUS);
	public static final RuneDef BASINFILL = effect("basinfill", "Basinfill", 1, 6, "frost", EffectKind.WORLD, "Fills an enclosed, one-block-deep hole with permanent source water: at most 16 connected cells, within three blocks of the impact. Aim at its floor with Touch or Bolt. Refuses open edges, deep pits, protected ground and the Nether. One basin per paid cast.");
	public static final RuneDef ICEPATH = effect("icepath", "Icepath", 1, 3, "frost", EffectKind.WORLD, "Freezes water within 3 blocks into ice you can walk on. On Self it lays a strip of ice ten blocks long the way you look.", RADIUS);
	public static final RuneDef COLLECT = effect("collect", "Collect", 1, 3, "void", EffectKind.WORLD, "Pulls up to 48 items and experience within 8 blocks from ground you can edit. Other owners' drops and pickup reservations stay put.", RADIUS);
	public static final RuneDef EXCAVATE = effect("excavate", "Excavate", 2, 10, "earth", EffectKind.WORLD, "Mines a 3x3 area of blocks (up to iron-pickaxe hardness; Amplify for diamond).", POWER);
	public static final RuneDef CLEAVE = effect("cleave", "Cleave", 3, 18, "blood", EffectKind.HARMFUL, "Cuts in proportion to the target: 6 damage plus 10% of its max health (up to 20 more); enemies beside it take half (three at most).", POWER, LINGER);
	public static final RuneDef DISMANTLE = effect("dismantle", "Dismantle", 2, 10, "blood", EffectKind.HARMFUL, "Three unseen slashes a tenth of a second apart: 3 damage each, straight through armour; the last is twice as deep against something that isn't facing you.", POWER, LINGER);
	public static final RuneDef BLACKSPARK = effect("blackspark", "Blackspark", 3, 16, "void", EffectKind.HARMFUL, "8 damage. One hit in four sparks black: 2.5x damage, an arc of 8 to the nearest other enemy, and you're in the zone (Strength and Speed) for 6 seconds.", POWER, LINGER);
	public static final RuneDef AFTERSHOCK = effect("aftershock", "Aftershock", 2, 9, "earth", EffectKind.HARMFUL, "5 damage, then half a second later the same spot is struck again for 5: whoever is still there takes it.", POWER, LINGER);
	public static final RuneDef RESONANCE = effect("resonance", "Resonance", 3, 14, "arcane", EffectKind.HARMFUL, "5 damage and a cursed mark for 10 seconds. Up to 4 other marked enemies within 16 blocks take half of it too, once each per cast.", POWER, DURATION, LINGER);
	public static final RuneDef RIPPLE = effect("ripple", "Ripple", 2, 10, "storm", EffectKind.HARMFUL, "Sunlight through the body: 6 damage, doubled against undead, and you heal a quarter of what it took. Half a second later it ripples out: 3 to every other enemy within 2.5 blocks, and you heal 1 for each.", POWER, LINGER);
	public static final RuneDef PRIMER = effect("primer", "Primer", 3, 18, "fire", EffectKind.HARMFUL, "Turns each target into a bomb that goes off 2 seconds later, or the moment it dies: 10 damage within 3 blocks (an enemy takes only the strongest bomb). Never breaks blocks.", POWER, RADIUS);
	public static final RuneDef BLACKFLAME = effect("blackflame", "Blackflame", 3, 18, "void", EffectKind.HARMFUL, "Black flames that water can't put out: 3 damage a second for 6 seconds. If the target dies burning, they spread.", POWER, DURATION);
	public static final RuneDef HOLLOW = effect("hollow", "Hollow", 4, 36, "void", EffectKind.HARMFUL, "Erases what it hits: it vanishes for a moment and returns for 20 damage, and everything within 4 blocks is dragged into the gap for 8 more.", POWER, RADIUS);
	public static final RuneDef REPEL = effect("repel", "Repel", 2, 9, "wind", EffectKind.HARMFUL, "A violent outward blast: 4 damage and hurls everything within 3 blocks away (light creatures far, heavy ones barely). On enemies just pulled in, it sets off Collapse.", POWER, RADIUS, LINGER);
	public static final RuneDef DECREE = effect("decree", "Decree", 2, 10, "arcane", EffectKind.HARMFUL, "A spoken command: everything hit is stunned for 2 seconds and condemned: your next 2 spell hits on it deal 40% more. Speaking it costs you 2 health, unless it holds 3 or more.", DURATION);
	public static final RuneDef WEIGH = effect("weigh", "Weigh", 2, 8, "earth", EffectKind.HARMFUL, "Crushingly heavy for 5 seconds: triple gravity, barely able to move or jump, and fliers are dragged down.", DURATION, LINGER);
	public static final RuneDef SHACKLE = effect("shackle", "Shackle", 2, 9, "earth", EffectKind.HARMFUL, "Chains each target to the spot for 5 seconds: it's yanked back if it strays more than 2 blocks, and the chain bites for 2.", DURATION);
	public static final RuneDef BUBBLE = effect("bubble", "Bubble", 2, 9, "frost", EffectKind.HARMFUL, "Traps targets in a floating bubble that pops for 4 damage and leaves them soaked. It holds small creatures 2.5 seconds, bigger ones 2, the largest 1.5.", DURATION, POWER);
	public static final RuneDef INFINITY = effect("infinity", "Infinity", 4, 32, "void", EffectKind.HELPFUL, "For 6 seconds the closer a hostile thing comes the slower it moves: enemies within 5 blocks are slowed harder the nearer they are, and projectiles slow to a stop in the air.", DURATION);
	public static final RuneDef REVERSAL = effect("reversal", "Reversal", 4, 28, "life", EffectKind.HELPFUL, "For 30 seconds, one killing blow is reversed: back to half health instead of dying. Once death has been cheated, nothing turns it back again for a minute.", DURATION);
	public static final RuneDef REFLECT = effect("reflect", "Reflect", 3, 16, "arcane", EffectKind.HELPFUL, "For 10 seconds, whatever hurts the target takes 60% of the damage back as arcane damage that ignores armour. Each reflection cracks it (6 at most).", DURATION, POWER);
	public static final RuneDef OVERDRIVE = effect("overdrive", "Overdrive", 2, 10, "blood", EffectKind.HELPFUL, "Past your limits for 10 seconds: Strength II, Speed II and Haste II, but you lose 1 health every 2 seconds. The weaker it leaves you, the harder you hit: Strength III under half health, IV under a quarter.", DURATION, POWER);
	public static final RuneDef FORESIGHT = effect("foresight", "Foresight", 3, 14, "time", EffectKind.HELPFUL, "Sees the next 2 attacks coming (for 15 seconds): each is dodged with a sidestep. A dodge turns away 12 damage at most, and casting it again in the same spell doesn't refill it.", DURATION, POWER);
	public static final RuneDef RESTORE = effect("restore", "Restore", 3, 14, "life", EffectKind.HELPFUL, "Puts things back: heals 4, puts out fire, and mends 8% of every worn and held item's durability (an item once a minute).", POWER);
	public static final RuneDef SWAP = effect("swap", "Swap", 2, 6, "arcane", EffectKind.MOVEMENT, "You and the first creature hit trade places, instantly.");
	public static final RuneDef ZIPPER = effect("zipper", "Zipper", 2, 7, "void", EffectKind.MOVEMENT, "Unzips the wall in front of you and steps you through up to 6 blocks of solid wall (never one that isn't yours to open).");
	public static final RuneDef SHADOWSTEP = effect("shadowstep", "Shadowstep", 3, 12, "void", EffectKind.MOVEMENT, "You vanish and reappear right behind the first creature hit, facing its back. Your next blow on it within 3 seconds lands half again as hard.");
	public static final RuneDef STASIS = effect("stasis", "Stasis", 4, 34, "time", EffectKind.HARMFUL, "Time stops for everything hit for 5 seconds. Every hit meanwhile is held, then lands all at once when time moves again.", DURATION);
	public static final RuneDef REWIND = effect("rewind", "Rewind", 4, 26, "time", EffectKind.HELPFUL, "Turns back the clock: return to where you were 5 seconds ago, if it is safe, with the health you had then if it was more.");
	public static final RuneDef ACCELERATE = effect("accelerate", "Accelerate", 3, 14, "time", EffectKind.HELPFUL, "Time runs faster for 10 seconds: Speed II, Haste III, Jump Boost II and Regeneration, and your spells charge 40% faster and your bolts and arcs fly 50% faster.", DURATION, POWER);
	public static final RuneDef TIME_SKIP = effect("time_skip", "Time Skip", 3, 14, "time", EffectKind.MOVEMENT, "Time skips ahead: you vanish, reappear up to 8 blocks forward on safe ground, and nearby monsters lose track of you. For a moment after, nothing can hurt you (not again for 5 seconds).");
	public static final RuneDef RAMPART = effect("rampart", "Rampart", 2, 8, "earth", EffectKind.WORLD, "Raises a 5-wide, 3-high wall of earth at the point for 10 seconds.", DURATION, RADIUS);
	public static final RuneDef SHADES = effect("shades", "Shades", 3, 22, "void", EffectKind.HELPFUL, "Two shadow hounds rise from your shadow and hunt at your side for 15 seconds. They are frail (20 health) and bite hard only where the light is dim (level 7 or less); each bite leaves its target shadowed.", DURATION, POWER);
	public static final RuneDef THUNDERBIRD = effect("thunderbird", "Thunderbird", 3, 20, "storm", EffectKind.HELPFUL, "A storm bird circles above you for 12 seconds. Every 2 seconds it marks the enemy you last hit (or the nearest within 12 blocks) and dives on that spot for 4.5 damage: step aside and it misses. Two at most.", DURATION, POWER);
	// Batch 6: protection.
	public static final RuneDef BARRIER = effect("barrier", "Barrier", 1, 6, "arcane", EffectKind.HELPFUL, "A thin barrier of light: 2 absorption hearts for 20 seconds.", POWER, DURATION);
	public static final RuneDef BRACE = effect("brace", "Brace", 1, 4, "earth", EffectKind.HELPFUL, "Braces for the blow: 80% less damage for 2 seconds. Bracing again takes 6 seconds.", DURATION);
	public static final RuneDef ANCHOR = effect("anchor", "Anchor", 1, 4, "void", EffectKind.HELPFUL, "Holds you fast for 15 seconds: blows and blasts can't knock you back, no spell can move you, and 4 more armour (8 once you've stood still a second).", DURATION);
	public static final RuneDef BRAMBLE = effect("bramble", "Bramble", 1, 6, "life", EffectKind.HELPFUL, "Thorns for 10 seconds: the next 4 things that hurt you from within 4 blocks take 3 damage and are shoved away.", POWER, DURATION);
	public static final RuneDef FROSTWARD = effect("frostward", "Frostward", 1, 3, "frost", EffectKind.HELPFUL, "For 60 seconds you can't freeze, not even in powder snow, and a frost hold on you lasts a second at most.", DURATION);
	public static final RuneDef CUSHION = effect("cushion", "Cushion", 1, 5, "wind", EffectKind.HELPFUL, "For 30 seconds falls can't hurt you, and a hard landing throws out a gust that knocks enemies back and hurts them: three quarters of a point for every block you fell past four (6 at most).", DURATION);
	public static final RuneDef DEFLECT = effect("deflect", "Deflect", 2, 9, "wind", EffectKind.HELPFUL, "For 8 seconds a whirl of wind sends arrows and other projectiles coming at the target back at whoever shot them.", DURATION);
	public static final RuneDef HAVEN = effect("haven", "Haven", 2, 14, "life", EffectKind.HELPFUL, "Raises living leaf shutters across a 4-block haven for 8 seconds: enemies inside are shoved out once a second, and enemy projectiles glance off its boundary.", DURATION, RADIUS);
	// Batch 6: mining and building.
	public static final RuneDef CHISEL = effect("chisel", "Chisel", 1, 2, "earth", EffectKind.WORLD, "Mines the block that was hit (up to stone-pickaxe hardness; Amplify for iron).", POWER);
	public static final RuneDef GLIMMER = effect("glimmer", "Glimmer", 1, 2, "life", EffectKind.WORLD, "Grows glowing lichen over the block that was hit and up to 4 around it: a light that stays.", RADIUS);
	public static final RuneDef PRUNE = effect("prune", "Prune", 1, 2, "wind", EffectKind.WORLD, "A gust clears leaves, grass, flowers, vines and cobwebs within 3 blocks, and they drop as they would to shears.", RADIUS);
	public static final RuneDef TUNNEL = effect("tunnel", "Tunnel", 2, 8, "earth", EffectKind.WORLD, "Bores a tunnel 2 high and 4 deep into the wall that was hit (up to iron-pickaxe hardness; Amplify for diamond).", POWER);
	public static final RuneDef VEIN = effect("vein", "Vein", 2, 10, "earth", EffectKind.WORLD, "Mines the block that was hit and, if it's an ore, every matching ore joined to it (up to 16; iron-pickaxe hardness; Amplify for diamond).", POWER);
	public static final RuneDef SMELT = effect("smelt", "Smelt", 2, 6, "fire", EffectKind.WORLD, "Mines the block that was hit and drops it smelted, as a furnace would (iron-pickaxe hardness; Amplify for diamond).", POWER);
	public static final RuneDef FELL = effect("fell", "Fell", 2, 8, "earth", EffectKind.WORLD, "Fells the tree that was hit: the log and every log joined to it above, up to 32.");
	public static final RuneDef SPAN = effect("span", "Span", 2, 8, "arcane", EffectKind.WORLD, "A bridge of glass grows from your feet toward the point, up to 16 blocks, and shatters 30 seconds later.", DURATION, RADIUS);
	// Batch 6: a simple spell for every element.
	public static final RuneDef EMBER = effect("ember", "Ember", 1, 6, "fire", EffectKind.HARMFUL, "3 fire damage and sets alight for 3 seconds. On something already burning it adds 3 seconds instead (10 at most).", POWER, DURATION, LINGER);
	public static final RuneDef ICICLE = effect("icicle", "Icicle", 1, 6, "frost", EffectKind.HARMFUL, "4 freeze damage, or 6 against a target that's already slowed. The icicle melts after 2 seconds and leaves it soaked.", POWER, LINGER);
	public static final RuneDef PELT = effect("pelt", "Pelt", 1, 5, "earth", EffectKind.HARMFUL, "Pelts targets with stones: 4 damage and a hard shove.", POWER, LINGER);
	public static final RuneDef WINDCUT = effect("windcut", "Windcut", 1, 6, "wind", EffectKind.HARMFUL, "A cutting wind: 4 damage and a light shove, and it breaks what the target is winding up: a charge, a bow's draw, a creeper's fuse, a spell.", POWER, LINGER);
	public static final RuneDef LEECH = effect("leech", "Leech", 1, 8, "blood", EffectKind.HARMFUL, "3 damage, and you heal for what it takes; what a full heart can't hold becomes a shield of up to 4.", POWER, LINGER);
	public static final RuneDef HEX = effect("hex", "Hex", 1, 5, "void", EffectKind.HARMFUL, "Hexes targets for 6 seconds: your spells hit them 25% harder, and they fix on you.", DURATION);
	public static final RuneDef REND = effect("rend", "Rend", 1, 5, "blood", EffectKind.HARMFUL, "Rends armour: targets lose 4 armour for 10 seconds, and what they naturally resist they take at full strength.", DURATION);
	public static final RuneDef COUNTDOWN = effect("countdown", "Countdown", 1, 7, "time", EffectKind.HARMFUL, "Marks targets: 1.5 seconds later the moment catches up with them for 6 damage. If the mark dies first, it finds the nearest enemy within 6 blocks.", POWER, LINGER);
	public static final RuneDef JOLT = effect("jolt", "Jolt", 2, 9, "storm", EffectKind.HARMFUL, "4 lightning damage that stuns for 1 second: no moving or fighting back. A caster caught mid-charge loses the spell.", POWER, DURATION, LINGER);
	public static final RuneDef BLEED = effect("bleed", "Bleed", 2, 8, "blood", EffectKind.HARMFUL, "Opens a wound: 2 damage, then 1 more every half second for 4 seconds (half as much again while it moves).", POWER, DURATION);
	public static final RuneDef COLDSNAP = effect("coldsnap", "Coldsnap", 2, 11, "frost", EffectKind.HARMFUL, "A cold snap racing out from the point: 4 freeze damage and Slowness II for 4 seconds to every enemy within 3 blocks, all left brittle for Shatter for 4 seconds.", POWER, RADIUS, DURATION);
	public static final RuneDef FLASHFIRE = effect("flashfire", "Flashfire", 2, 11, "fire", EffectKind.HARMFUL, "A flash of heat: 5 fire damage to every enemy within 3 blocks, setting them alight for 4 seconds. Allies in it are thawed and dried.", POWER, RADIUS);
	public static final RuneDef BANISH = effect("banish", "Banish", 2, 8, "void", EffectKind.HARMFUL, "Banishes targets: they vanish and reappear up to 8 blocks further away from you, dazed.", POWER);
	public static final RuneDef CYCLONE = effect("cyclone", "Cyclone", 2, 10, "wind", EffectKind.HARMFUL, "A whirlwind spins every enemy within 3 blocks around the point for 2 seconds, then flings them all the way you were facing for 3 damage.", POWER, RADIUS, DURATION);

	// ---- Innate runes: one is awakened in each caster's heart at the 1st Circle. They can't be
	// crafted, found or taught, and they grow stronger with every circle.
	public static final RuneDef BLOOD_THREAD = effect("blood_thread", "Blood Thread", 1, 10, "blood", EffectKind.HARMFUL, "Threads everything hit together (4 at most) for 8 seconds: 40% of any damage one of them takes is dealt to each of the rest (three times a second at most).", DURATION);
	public static final RuneDef KINDLING = effect("kindling", "Kindling", 1, 7, "fire", EffectKind.HARMFUL, "3 fire damage and a stack of Kindling. The fifth stack ignites: 14 damage in a 3-block burst, and everything the burst reaches starts at two stacks.", POWER, LINGER);
	public static final RuneDef TWIN_STAR = effect("twin_star", "Twin Star", 1, 12, "arcane", EffectKind.HELPFUL, "Your next spell within 6 seconds is cast twice.");
	public static final RuneDef BORROWED_TIME = effect("borrowed_time", "Borrowed Time", 1, 14, "time", EffectKind.HELPFUL, "Heals the damage you took in the last 5 seconds, as much as you're missing. Over the next 10 seconds it comes back with a fifth on top, but never kills you, unless you slay a monster.");
	public static final RuneDef GALE_MANTLE = effect("gale_mantle", "Gale Mantle", 1, 10, "wind", EffectKind.HELPFUL, "For 12 seconds, jump again in midair to dash the way you're steering (up to 3 dashes), shoving aside whoever you pass.", DURATION);
	public static final RuneDef STONEFORM = effect("stoneform", "Stoneform", 1, 12, "earth", EffectKind.HELPFUL, "For 8 seconds: no knockback, 20% less damage, and every blow you take from an attacker sends out an aftershock (at most once a second).", DURATION);
	public static final RuneDef MIRRORFROST = effect("mirrorfrost", "Mirrorfrost", 1, 12, "frost", EffectKind.HELPFUL, "Casts back the last spell that hit you in the past 30 seconds, as your own at 70% power. A spell cast back can't be cast back again.");
	public static final RuneDef FORTUNE = effect("fortune", "Fortune", 1, 10, "life", EffectKind.HELPFUL, "For 10 seconds, every hit you deal has a 1 in 4 chance to strike for double, and a kill has a 1 in 4 chance to drop extra experience.", DURATION);
	public static final RuneDef PHANTOM = effect("phantom", "Phantom", 1, 12, "void", EffectKind.HELPFUL, "Leaves an afterimage of you that every monster within 16 blocks turns on for 4 seconds, then it bursts for 8 damage, and 1 more for every 6 it took.", DURATION);
	public static final RuneDef STORMHEART = effect("stormheart", "Stormheart", 1, 12, "storm", EffectKind.HELPFUL, "For 10 seconds, whatever hurts you (a blow of 2 or more) is struck by lightning (at most once a second).", DURATION);

	// ---- Modifiers
	public static final RuneDef AMPLIFY = modifier("amplify", "Amplify", 1, 1.5, POWER, "+50% power (damage, healing, force, blast).");
	public static final RuneDef EXTEND = modifier("extend", "Extend", 1, 1.4, DURATION, "+100% duration. At most three per target rune.");
	public static final RuneDef WIDEN = modifier("widen", "Widen", 2, 1.5, RADIUS, "+50% radius.");
	public static final RuneDef QUICKEN = modifier("quicken", "Quicken", 2, 1.2, SPEED, "Bolts fly twice as fast; delays are halved. On fields, walls, totems, domains, latches, streams and barrages it makes the same strikes come twice as fast, so the spell ends sooner.");
	public static final RuneDef PIERCE_MOD = modifier("pierce", "Pierce", 2, 1.3, PIERCE, "Passes through up to 3 targets.");
	public static final RuneDef BOUNCE_MOD = modifier("bounce", "Bounce", 2, 1.3, BOUNCE, "Bounces off blocks up to 3 times.");
	public static final RuneDef SPLIT_MOD = modifier("split", "Split", 3, 2.4, SPLIT, "Three copies of the shape.");
	public static final RuneDef HOMING_MOD = modifier("homing", "Homing", 3, 1.4, HOMING, "Steers toward the nearest enemy within 12 blocks.");
	public static final RuneDef CHAIN_MOD = modifier("chain", "Chain", 3, 1.8, CHAIN, "After a hit, jumps to up to 3 more enemies within 6 blocks.");
	public static final RuneDef FRUGAL_MOD = modifier("frugal", "Frugal", 1, 0.5, FRUGAL, "Half the mana, but 40% weaker and shorter.");
	public static final RuneDef LINGER_MOD = modifier("linger", "Linger", 2, 1.8, LINGER, "The effect lands twice more, a second apart.");
	public static final RuneDef VOLLEY_MOD = modifier("volley", "Volley", 2, 2.4, VOLLEY, "Fires three times in quick succession.");
	public static final RuneDef FOCUS_MOD = modifier("focus", "Focus", 2, 1.2, RADIUS, "Half the radius, +50% power. At most two per target rune.");
	public static final RuneDef OVERCHARGE_MOD = modifier("overcharge", "Overcharge", 3, 2.6, POWER, "+150% power, but 2.6 times the mana.");
	public static final RuneDef RAPID_MOD = modifier("rapid", "Rapid", 2, 1.4, Trait.COOLDOWN, "Halves the whole spell's cooldown.");
	public static final RuneDef VOW_MOD = modifier("vow", "Vow", 3, 1.0, Trait.COOLDOWN, "A binding vow: the shape's effects hit twice as hard, but the whole spell's cooldown is 5x longer. One per shape.");
	public static final RuneDef BLOOD_PRICE_MOD = modifier("blood_price", "Blood Price", 3, 1.0, Trait.COOLDOWN, "Pay for the whole spell in health instead of mana: 1 health per 4 mana. Never lethal.");
	public static final RuneDef EXECUTE_MOD = modifier("execute", "Execute", 2, 1.3, POWER, "Double power against targets under half health. One per effect.");

	// ---- Links
	public static final RuneDef DELAY = link("delay", "Delay", 1, 2, "The rest fires 1 second later, from you.", DURATION, SPEED);
	public static final RuneDef ON_HIT = link("on_hit", "On Hit", 2, 2, "The rest fires wherever the shape before it hits.");
	public static final RuneDef ON_LAND = link("on_land", "On Land", 2, 2, "The rest fires when you next touch the ground.");
	public static final RuneDef ON_KILL = link("on_kill", "On Kill", 3, 2, "The rest fires at each creature the shape before it kills.");
	public static final RuneDef ECHO = link("echo", "Echo", 3, 2, "Everything before it fires again 0.5 seconds later. After On Hit or On Kill, only for the first hit or kill.");
	public static final RuneDef PULSE = link("pulse", "Pulse", 2, 2, "The rest fires three times, one second apart, from you. After On Hit or On Kill, only for the first hit or kill.", SPEED);
	public static final RuneDef ON_HURT = link("on_hurt", "On Hurt", 2, 2, "The rest fires at whatever next hurts you (within 15 seconds).");
	public static final RuneDef IF_SNEAKING = link("if_sneaking", "If Sneaking", 2, 1, "The rest fires only if you're sneaking. Build two spells in one.");
	public static final RuneDef ON_LOW_HEALTH = link("on_low_health", "On Low Health", 3, 2, "The rest fires when your health drops below 30% (within 30 seconds).");
	public static final RuneDef IF_AIRBORNE = link("if_airborne", "If Airborne", 2, 1, "The rest fires only if you're in the air. Build aerial finishers.");
	public static final RuneDef COMBO = link("combo", "Combo", 3, 2, "The rest fires only on every third cast of this spell: a finisher.");
	public static final RuneDef IMBUE = link("imbue", "Imbue", 2, 3, "The rest isn't cast: it's stored, with 3 charges, in what the shape before it touches. With Self, the item in your hand (a weapon's hits, a bow's arrows, a tool's blocks, armour when you're hurt, a block where it's placed, anything else when used), or with empty hands the block you're looking at. Any block becomes a glyph that goes off at whoever steps on, uses, shoots or breaks it, or when it's powered. The stored part costs 3 times as much; releasing it costs no mana, but everything you've imbued shares one cooldown as long as the stored spell's, and you keep up to 6 imbued items.");

	// ---- Fused effects: made only at the Fusion Altar, from two effects of the right elements (see Fusions). Never crafted or found.
	public static final RuneDef FIRESTORM = effect("firestorm", "Firestorm", 3, 18, "fire", EffectKind.HARMFUL, "Sets targets alight for 6 seconds and deals 5 damage, and the fire leaps to every enemy within 2 blocks of them (2 damage), then a moment later to those within 2 blocks of those (1.5).", POWER, DURATION, RADIUS, LINGER);
	public static final RuneDef STEAM = effect("steam", "Steam", 3, 14, "fire", EffectKind.HARMFUL, "A scalding burst of steam: 5 damage and Blindness for 3 seconds, and a cloud hangs where it burst: everything in it is blinded and left wet.", POWER, DURATION, LINGER);
	public static final RuneDef MAGMA = effect("magma", "Magma", 3, 16, "earth", EffectKind.HARMFUL, "The ground under the target turns to magma for 4 seconds: 2 damage a second to every enemy standing on it, and the pool widens as it burns.", POWER, DURATION, RADIUS);
	public static final RuneDef TEMPEST = effect("tempest", "Tempest", 3, 22, "storm", EffectKind.HARMFUL, "A lightning strike for 8 damage, and a gale that hurls targets far away; where they come down a second bolt strikes for 4.", POWER, LINGER);
	public static final RuneDef PLASMA = effect("plasma", "Plasma", 3, 20, "storm", EffectKind.HARMFUL, "10 damage, half of it ignoring armour, and the target is ionised for 5 seconds: the next storm damage it takes conducts as if it were wet.", POWER, LINGER);
	public static final RuneDef HAIL = effect("hail", "Hail", 3, 16, "frost", EffectKind.HARMFUL, "Five hailstones of 2 damage each, each staggering the target, and Slowness II for 4 seconds.", POWER, DURATION, LINGER);
	public static final RuneDef GLACIER = effect("glacier", "Glacier", 3, 16, "frost", EffectKind.HARMFUL, "Freezes targets in place for 2 seconds (1 second on players). The ice spreads: up to 3 other enemies within 2.5 blocks freeze for 1 second, and when it cracks everything frozen takes 2.", DURATION, LINGER);
	public static final RuneDef LIFESTEAL = effect("lifesteal", "Lifesteal", 3, 16, "blood", EffectKind.HARMFUL, "5 damage, and you heal for what it dealt. For 6 seconds everything the target suffers, from anyone, heals you for a quarter of it.", POWER, LINGER);
	public static final RuneDef WARP = effect("warp", "Warp", 3, 12, "void", EffectKind.MOVEMENT, "You and the first creature hit swap places through the void, and you come out unseen for a second. An enemy is left pulled and reeling: Slowness II and Nausea for 2 seconds.", DURATION);
	public static final RuneDef BLOOM = effect("bloom", "Bloom", 3, 14, "life", EffectKind.HELPFUL, "Regeneration II for 6 seconds, plants grow around the first 3 allies it touches, and allies near them catch Regeneration I for 5.", POWER, DURATION);
	public static final RuneDef SURGE = effect("surge", "Surge", 3, 16, "storm", EffectKind.HELPFUL, "Speed I and Strength I for 8 seconds; the blows you land in that time arc on to a nearby enemy for 2 (four arcs at most).", POWER, DURATION);
	public static final RuneDef NULLIFY = effect("nullify", "Nullify", 3, 14, "arcane", EffectKind.HARMFUL, "Strips an enemy's good effects, or an ally's bad effects. Vexes, and spirit wolves and shades that aren't yours, dissolve.");

	// ---- Fused effects, the full chorus: every other pair of elements, and each element with itself (see Fusions).
	public static final RuneDef PHOENIX_PYRE = effect("phoenix_pyre", "Phoenix Pyre", 3, 18, "fire", EffectKind.HELPFUL, "Wreathes allies in healing flame for 6 seconds: Regeneration I and Fire Resistance, and every second enemies within 2 blocks of them are set alight and take 1 damage. The first time an ally falls under 35% health the pyre flares: 6 health, and 4 damage to enemies within 3 blocks.", POWER, DURATION, RADIUS);
	public static final RuneDef HELLMOUTH = effect("hellmouth", "Hellmouth", 3, 20, "fire", EffectKind.HARMFUL, "Opens a pit of black fire for 3 seconds that drags enemies within 3 blocks toward it and burns those at its core for 2 damage a second, then caves in for 4 damage.", POWER, DURATION, RADIUS);
	public static final RuneDef STARFIRE = effect("starfire", "Starfire", 3, 16, "fire", EffectKind.HARMFUL, "Five motes of starfire seek up to five enemies within 6 blocks: 2 damage each, and they burn for 3 seconds.", POWER, DURATION, RADIUS);
	public static final RuneDef EVERBURN = effect("everburn", "Everburn", 3, 16, "fire", EffectKind.HARMFUL, "Sets targets alight for 5 seconds with a fire that burns two and a half times as fast (1.5 more damage a second), and rekindles once for 3 more when it goes out.", POWER, DURATION, LINGER);
	public static final RuneDef BLOODBOIL = effect("bloodboil", "Bloodboil", 3, 16, "blood", EffectKind.HARMFUL, "3 damage, and for 5 seconds the target's blood boils: each time it's hurt it takes 2 more fire damage (up to 5 times).", POWER, DURATION, LINGER);
	public static final RuneDef CONFLAGRATION = effect("conflagration", "Conflagration", 3, 20, "fire", EffectKind.HARMFUL, "Sets targets alight for 6 seconds, and every burning enemy within 6 blocks flares up for 4 damage (1 more for each other one flaring, up to 3) and burns 2 seconds longer.", POWER, DURATION, RADIUS);
	public static final RuneDef BLIZZARD = effect("blizzard", "Blizzard", 3, 18, "frost", EffectKind.HARMFUL, "A blizzard howls 3 blocks around where it lands for 4 seconds, walking 6 blocks the way you faced: enemies in it are slowed (Slowness II), chilled and take 1.5 damage a second.", POWER, DURATION, RADIUS);
	public static final RuneDef FROSTBLOOM = effect("frostbloom", "Frostbloom", 3, 14, "frost", EffectKind.HELPFUL, "Regeneration II for 5 seconds, and for 8 seconds anything that strikes the ally is frozen stiff (Slowness III for 2 seconds) and heals them a point (4 at most).", POWER, DURATION);
	public static final RuneDef BLACK_ICE = effect("black_ice", "Black Ice", 3, 16, "frost", EffectKind.HARMFUL, "Freezes targets for 1.5 seconds (1 on players) and leaves them weak (Weakness II for 5 seconds). One that dies in the next 5 seconds shatters: 4 damage to enemies within 3 blocks, which turn brittle in their turn.", POWER, DURATION, LINGER);
	public static final RuneDef RIME_SEAL = effect("rime_seal", "Rime Seal", 3, 16, "frost", EffectKind.HARMFUL, "Writes a frost seal 3 blocks across on the ground for 6 seconds. An enemy that stands in it for a second freezes solid for 1.5 seconds (1 on players) and takes 3 damage, once each.", POWER, DURATION, RADIUS);
	public static final RuneDef CRYOSTASIS = effect("cryostasis", "Cryostasis", 3, 18, "frost", EffectKind.HELPFUL, "Seals an ally in ice for 2 seconds (4 at most, however it's extended): they can't move, cast or be hurt, and heal 6 health while they wait. When it opens it bursts: enemies within 3 blocks are thrown back, take 3, and are slowed and left brittle. Not again on the same creature for 10 seconds.", POWER, DURATION);
	public static final RuneDef FROSTBITE = effect("frostbite", "Frostbite", 3, 16, "frost", EffectKind.HARMFUL, "3 damage, then 1 damage a second for 5 seconds as the cold sets in (Slowness I, then II, then III); at the end the target freezes for a second and a half.", POWER, DURATION, LINGER);
	public static final RuneDef ABSOLUTE_ZERO = effect("absolute_zero", "Absolute Zero", 3, 18, "frost", EffectKind.HARMFUL, "Slowness IV for 3 seconds. A target showing signs of cold (slowed, brittle, frozen skin, held) freezes solid and takes damage for each: 4.5 for one, 7 for two, 9.5 for three, 12 for four, and is held 1.5 to 3 seconds (half as long on players), then not again until 3 seconds after it thaws.", POWER, DURATION, LINGER);
	public static final RuneDef MAGNETIZE = effect("magnetize", "Magnetize", 3, 16, "storm", EffectKind.HARMFUL, "Magnetizes a target for 4 seconds: enemies within 5 blocks are drawn to it, and any that touch it are shocked for 3 damage (once a second).", POWER, DURATION, RADIUS);
	public static final RuneDef RIFTBOLT = effect("riftbolt", "Riftbolt", 3, 18, "storm", EffectKind.HARMFUL, "A black bolt for 7 damage that tears the target through a rift up to 5 blocks away, left in Darkness for 3 seconds.", POWER, DURATION, LINGER);
	public static final RuneDef STORMWEAVE = effect("stormweave", "Stormweave", 3, 18, "storm", EffectKind.HARMFUL, "Marks up to 4 enemies within 6 blocks; a moment later lightning weaves between them all: 4 damage each, and 1 more for every other one caught in the web; a target alone takes 2 more.", POWER, RADIUS);
	public static final RuneDef STORMCLOCK = effect("stormclock", "Stormclock", 3, 16, "storm", EffectKind.HARMFUL, "4 damage, and lightning strikes the same spot again 2 and 4 seconds later: 3 damage to enemies within 1.5 blocks each time.", POWER, RADIUS);
	public static final RuneDef HEARTSTOPPER = effect("heartstopper", "Heartstopper", 3, 18, "blood", EffectKind.HARMFUL, "5 damage, and for 6 seconds the target's heart skips every 2 seconds: a stumble, a lurch, then a full stop (a quarter, half, then one and a half seconds of stun).", POWER, DURATION, LINGER);
	public static final RuneDef THUNDERHEAD = effect("thunderhead", "Thunderhead", 3, 20, "storm", EffectKind.HARMFUL, "A thundercloud gathers over the target for 4 seconds and strikes an enemy within 4 blocks of it every second: 2 damage, and the rain soaks it so the bolt conducts.", POWER, DURATION, RADIUS);
	public static final RuneDef DOWNDRAFT = effect("downdraft", "Downdraft", 3, 14, "wind", EffectKind.HARMFUL, "Slams every airborne enemy within 6 blocks to the ground: 4 damage, and 1 more for every block it fell (up to 6). Flyers lose their lift. With nothing in the air it pins whoever stands under it (Slowness III for a second, 2 damage).", POWER, RADIUS);
	public static final RuneDef UPDRAFT = effect("updraft", "Updraft", 3, 16, "wind", EffectKind.HARMFUL, "Hurls enemies within 2.5 blocks high into the air, where every spell hits them harder; a moment later a downdraft smashes them back down for 4 damage.", POWER, RADIUS);
	public static final RuneDef SKYGLYPH = effect("skyglyph", "Skyglyph", 3, 12, "wind", EffectKind.WORLD, "Writes a wind glyph where it lands for 10 seconds: an ally who steps on it is launched high and forward, an enemy thrown back 4 blocks.", POWER, DURATION);
	public static final RuneDef RECOIL = effect("recoil", "Recoil", 3, 14, "wind", EffectKind.HARMFUL, "Hurls targets 5 blocks back; 2 seconds later the wind snaps them back to where they stood, for 5 damage.", POWER);
	public static final RuneDef ZEPHYR = effect("zephyr", "Zephyr", 3, 14, "wind", EffectKind.HELPFUL, "A warm breeze that clears the air: allies within 4 blocks get Speed I, Jump Boost I and Regeneration I for 10 seconds, and it blows away blindness, darkness, nausea and slowness.", POWER, DURATION, RADIUS);
	public static final RuneDef CRIMSON_MIST = effect("crimson_mist", "Crimson Mist", 3, 18, "blood", EffectKind.HARMFUL, "A red mist 3 blocks around where it lands for 5 seconds: enemies in it bleed for 2 damage a second, allies in it heal 1.5 health a second and are hidden from monsters more than 4 blocks away.", POWER, DURATION, RADIUS);
	public static final RuneDef SINKHOLE = effect("sinkhole", "Sinkhole", 3, 18, "earth", EffectKind.HARMFUL, "The ground gives way: enemies within 3 blocks are dragged to its middle and pinned for 2 seconds (Slowness IV, no jumping), then crushed for 6 damage.", POWER, DURATION, RADIUS);
	public static final RuneDef GEODE = effect("geode", "Geode", 3, 16, "earth", EffectKind.HELPFUL, "Crystal armour: Resistance II for 5 seconds, and anything that strikes the ally is cut by crystal shards for 2 damage and left cracked (every spell hits it 20% harder for 5 seconds).", POWER, DURATION);
	public static final RuneDef FOSSILIZE = effect("fossilize", "Fossilize", 3, 16, "earth", EffectKind.HARMFUL, "The target turns slowly to stone: Slowness I, II, then III over 3 seconds, then it's stone, held for 2 seconds (1 on players), brittle while it stands (every spell hits it 20% harder), and cracked for 6 damage.", POWER, DURATION);
	public static final RuneDef BONESPUR = effect("bonespur", "Bonespur", 3, 16, "earth", EffectKind.HARMFUL, "Spurs of bone burst from the ground under up to 4 enemies within 4 blocks: 5 damage each, and they bleed for 3 seconds.", POWER, DURATION, RADIUS);
	public static final RuneDef MONOLITH = effect("monolith", "Monolith", 3, 20, "earth", EffectKind.HARMFUL, "A pillar of stone bursts up under the target: 8 damage, and it's thrown 3 blocks into the air; where it lands, 3 more to whatever is there. The pillar crumbles after 4 seconds.", POWER);
	public static final RuneDef SOULBOND = effect("soulbond", "Soulbond", 3, 16, "life", EffectKind.HELPFUL, "Binds you and an ally for 10 seconds: any damage either of you takes is split between you. The bond breaks beyond 16 blocks.", DURATION);
	public static final RuneDef SECOND_WIND = effect("second_wind", "Second Wind", 3, 20, "life", EffectKind.HELPFUL, "For 20 seconds, the first blow that would kill the ally leaves them at 4 health instead, with Regeneration II and Speed II for 4 seconds and a gust that shoves enemies away. Once death has been cheated (by this or any other spell), it isn't again on them for a minute.", DURATION);
	public static final RuneDef TRANSFUSION = effect("transfusion", "Transfusion", 3, 12, "blood", EffectKind.HELPFUL, "You give up to 4 of your own health (never below 2), and the ally heals three times what you gave and is cured of one harmful effect.", POWER);
	public static final RuneDef LIFEBLOOM = effect("lifebloom", "Lifebloom", 3, 16, "life", EffectKind.HELPFUL, "Heals 4, then 1 a second for 5 seconds; when it fades it bursts, healing every ally within 3 blocks for 3.", POWER, DURATION, RADIUS);
	public static final RuneDef SANGUINE_RITE = effect("sanguine_rite", "Sanguine Rite", 3, 14, "blood", EffectKind.HARMFUL, "You pay 3 of your own health (never your last; more with Amplify, Overcharge and a crowd) for 12 damage that ignores armour.", POWER, LINGER);
	public static final RuneDef ENTROPY = effect("entropy", "Entropy", 3, 16, "void", EffectKind.HARMFUL, "The target unravels: 1, 1.5, 2, 2.5 and 3 damage over 5 seconds, straight through armour, and each wound strips a point of its armour until it ends.", POWER, DURATION, LINGER);
	public static final RuneDef DEVOUR = effect("devour", "Devour", 3, 16, "void", EffectKind.HARMFUL, "5 damage, and 1 more for every tenth of its health the target is missing (up to 10). If it kills, you feed: 10 mana and 4 absorption (twice a cast at most).", POWER, LINGER);
	public static final RuneDef TIMESTEAL = effect("timesteal", "Timesteal", 3, 16, "time", EffectKind.HARMFUL, "Steals up to 2 of the target's good effects, with the time they had left (at most 30 seconds), and gives them to you. With nothing to steal it steals a moment: the target drags, and you are quickened, for 2 seconds.", DURATION);
	public static final RuneDef HEMOMANCY = effect("hemomancy", "Hemomancy", 3, 16, "blood", EffectKind.HARMFUL, "4 magic damage, and 1 more for every 1.5 health you're missing (up to 8 more). Under half health you heal a quarter of what it deals.", POWER, LINGER);
	public static final RuneDef RECKONING = effect("reckoning", "Reckoning", 3, 18, "time", EffectKind.HARMFUL, "For 4 seconds, every wound the target takes is counted; then half of it comes due again at once (at most 12), and half of what comes due heals you (at most 6).", POWER, DURATION);
	public static final RuneDef SINGULARITY = effect("singularity", "Singularity", 3, 22, "void", EffectKind.HARMFUL, "A black hole opens where it lands for 2.5 seconds, drawing in enemies within 5 blocks and swallowing arrows and bolts, then bursts: 6 damage (1 more for each thing swallowed, up to 5), and they're flung outward.", POWER, RADIUS);
	public static final RuneDef PRISMATIC_BURST = effect("prismatic_burst", "Prismatic Burst", 3, 16, "arcane", EffectKind.HARMFUL, "5 damage, and 4 more for every mark on the target (burning, frozen, windswept, pulled, soaked, wet, cracked, shadowed, bleeding or exposed), up to 5, each used up and passed on to up to 3 enemies within 4 blocks: up to 25.", POWER, LINGER);
	public static final RuneDef CHRONOSHIFT = effect("chronoshift", "Chronoshift", 3, 18, "time", EffectKind.HELPFUL, "Turns an ally's clock forward: their other spells come off cooldown 3 seconds sooner, a third of the mana they spent in the last 5 seconds comes back (30 at most), and they get Haste I and Speed I for 5 seconds.", POWER, DURATION);

	/** Fused effects: made only by combining two effects at the Fusion Altar. */
	public static final java.util.List<RuneDef> FUSED = java.util.List.of(FIRESTORM, STEAM, MAGMA, TEMPEST, PLASMA, HAIL, GLACIER, LIFESTEAL, WARP, BLOOM, SURGE, NULLIFY, PHOENIX_PYRE, HELLMOUTH,
		STARFIRE, EVERBURN, BLOODBOIL, CONFLAGRATION, BLIZZARD, FROSTBLOOM, BLACK_ICE, RIME_SEAL, CRYOSTASIS, FROSTBITE, ABSOLUTE_ZERO,
		MAGNETIZE, RIFTBOLT, STORMWEAVE, STORMCLOCK, HEARTSTOPPER, THUNDERHEAD, DOWNDRAFT, UPDRAFT, SKYGLYPH, RECOIL, ZEPHYR,
		CRIMSON_MIST, SINKHOLE, GEODE, FOSSILIZE, BONESPUR, MONOLITH, SOULBOND, SECOND_WIND, TRANSFUSION, LIFEBLOOM, SANGUINE_RITE,
		ENTROPY, DEVOUR, TIMESTEAL, HEMOMANCY, RECKONING, SINGULARITY, PRISMATIC_BURST, CHRONOSHIFT);

	/** Whether a rune is made only at the Fusion Altar: an element fusion or a signature one. */
	public static boolean fused(RuneDef rune) {
		return FUSED.contains(rune) || SIGNATURE.contains(rune) || WovenRunes.isWoven(rune) || PairRunes.isPair(rune);
	}


	// Physical magic: cover, borrowed source water, and collision platforms.
	public static final RuneDef STRATA_RISE = effect("strata_rise", "Strata Rise", 2, 9, "earth", EffectKind.WORLD, "Raises a real five-wide, three-high stone wall over seven ticks. Lasts eight seconds, drops no material, respects protected land and cannot trap creatures.", DURATION);
	public static final RuneDef TIDAL_LIFT = effect("tidal_lift", "Tidal Lift", 2, 12, "frost", EffectKind.HARMFUL, "Borrows up to three real water sources within four blocks, lifts them along a ten-block arc, then returns them. Each enemy takes 4 damage once and is soaked for five seconds. Needs nearby source water.", POWER);
	public static final RuneDef WIND_STEPS = effect("wind_steps", "Wind Steps", 2, 8, "wind", EffectKind.WORLD, "Forms five real wind platforms ahead, ascending one block every two steps. Lasts eight seconds; allies standing on expiring steps get three seconds of slow falling.", DURATION);
	public static final RuneDef CINDER_BULWARK = effect("cinder_bulwark", "Cinder Bulwark", 3, 16, "fire", EffectKind.WORLD, "Raises a cracked, glowing wall. Enemies brushing against it take 2 magic damage once per cast. Lasts eight seconds and drops nothing.", DURATION);
	public static final RuneDef ROOT_BULWARK = effect("root_bulwark", "Root Bulwark", 3, 16, "life", EffectKind.WORLD, "Raises a living root wall that gives nearby allies brief Regeneration I. Lasts eight seconds, respects land protections and drops no wood.", DURATION);
	public static final RuneDef BOILING_SURGE = effect("boiling_surge", "Boiling Surge", 3, 19, "fire", EffectKind.HARMFUL, "Lifts nearby real water through a scalding arc, returning each source afterwards. Each enemy takes 5 damage once and two seconds of Weakness. Original steam curls trace the crest.", POWER);
	public static final RuneDef THUNDER_TIDE = effect("thunder_tide", "Thunder Tide", 3, 19, "storm", EffectKind.HARMFUL, "Suspends real borrowed water inside electrical rings and drives it forward. Each enemy takes 5 damage once, is soaked and slowed for one second. Water returns safely.", POWER);
	public static final RuneDef RIME_CAUSEWAY = effect("rime_causeway", "Rime Causeway", 3, 16, "frost", EffectKind.WORLD, "Condenses wind into an ascending three-wide ice causeway. Lasts eight seconds, leaves no farmable ice and cushions allies when it fades.", DURATION);
	public static final RuneDef THUNDER_WALK = effect("thunder_walk", "Thunder Walk", 3, 16, "storm", EffectKind.WORLD, "Builds five copper-lit wind stepping stones. Each enemy touching a stone takes 2 magic damage once per cast; allies receive slow falling before expiry.", DURATION);

	public static final RuneDef SPRINGBED = effect("springbed", "Springbed", 3, 14, "frost", EffectKind.WORLD, "Pours one safe Basinfill vessel, then grows up to eight existing bank crops or moist Moonreed buds by one stage. The new water must hydrate their soil. Never creates plants or opens mature Moonreed without pollinators. Once per paid cast.");
	public static final RuneDef CINDER_SIEVE = effect("cinder_sieve", "Cinder Sieve", 3, 12, "fire", EffectKind.WORLD, "Consumes one carried coal or charcoal to furnace-process up to sixteen visible loose inputs within four blocks. Only commits when complete outputs fit your inventory. Preserves owned, delayed and protected drops; grants no XP. Once per paid cast.");
	public static final RuneDef ASHEN_MERCY = effect("ashen_mercy", "Ashen Mercy", 3, 16, "life", EffectKind.HELPFUL, "Extinguishes an ally and removes harmful conditions, converting only actual conditions removed into at most four restored health. Gives five seconds of heat protection. At most eight allies, once per target and paid cast; clean targets receive no healing.");
	public static final RuneDef CLOCKROOT = effect("clockroot", "Clockroot", 3, 16, "earth", EffectKind.HARMFUL, "Roots remember safe ground for four seconds. A foe fleeing more than two blocks is returned once if its original floor and route are still safe, loaded and permitted. Refuses bosses, anchored or mounted targets and warded arenas. At most eight foes per paid cast; eight-second target rest.");
	public static final RuneDef SKYLATCH = effect("skylatch", "Skylatch", 3, 14, "wind", EffectKind.HELPFUL, "Lifts an ally about one block and holds vertical height for four seconds while lateral movement remains free. Crouch to release into two seconds of gentle descent. Refuses ceilings, flight abilities, anchors and warded arenas. At most eight allies per paid cast; eight-second target rest.");
	public static final RuneDef THRESHERWIND = effect("thresherwind", "Thresherwind", 3, 12, "wind", EffectKind.WORLD, "Three travelling shear lanes harvest up to nine mature crops ahead of the impact. Replants only by consuming a real seed from each crop's drops. Collects this harvest, preserves full-inventory leftovers, respects loaded land and claims. Once per paid cast.");

	public static final RuneDef NULLCATCH = effect("nullcatch", "Nullcatch", 3, 20, "void", EffectKind.HELPFUL, "An open mirror pocket catches one hostile front projectile over three seconds. One payment buys one capture; rear, allied, reflected and ownerless attacks pass. No stored attack or refund.");
	public static final RuneDef SECOND_BELL = effect("second_bell", "Second Bell", 3, 16, "time", EffectKind.HARMFUL, "Two visible electrodes ring after one and two seconds. Leaving the marked spot evades the second discharge. Two targets at most, eight total damage per payment after bonuses; no movement hold.");
	public static final RuneDef RED_LEDGER = effect("red_ledger", "Red Ledger", 3, 18, "blood", EffectKind.HARMFUL, "Three visible blood gates measure ordinary movement over three seconds. Moving one block spends a gate for a small cut. Standing still avoids all cuts; teleports do not count. Six total damage per payment after bonuses.");
	public static final RuneDef QUIETUS = effect("quietus", "Quietus", 3, 20, "arcane", EffectKind.HARMFUL, "A visible short escrow waits for one newly cast hostile magic projectile, closes on it and taxes up to eight of its player's current mana. No hit, free ammunition or mana reward; old and reflected spells pass.");
	public static final RuneDef BLOOD_ESCROW = effect("blood_escrow", "Blood Escrow", 3, 14, "blood", EffectKind.HELPFUL, "Spend up to three actual health, never below four, to buy one ally a six-second absorption chamber. Crouching or an existing ward refuses. One gift per payment; no refund or renewal.");
	public static final RuneDef FROST_MOLT = effect("frost_molt", "Frost Molt", 3, 14, "frost", EffectKind.HELPFUL, "Peel an actual frozen condition into one three-second ice plate. It stops one ordinary projectile of six damage or less; larger, bypass and other attacks pass. Clean allies cannot mint a plate; crouching sheds it.");
	public static final RuneDef PULSE_FERRY = effect("pulse_ferry", "Pulse Ferry", 3, 18, "life", EffectKind.HELPFUL, "One sap parcel visits two different wounded eligible allies within four blocks on separate beats. Each receives at most three actual healing, six total. Crouching refuses a visit. No overheal or repeated recovery.");
	public static final RuneDef LAST_LANTERN = effect("last_lantern", "Last Lantern", 3, 18, "arcane", EffectKind.HELPFUL, "Record one allied player's safe ground for six seconds. Their next fresh crouch requests one safe return within eight blocks. Invalid floor, route, dimension or permission refuses. No health rewind or forced return.");
	public static final RuneDef POCKET_CURRENT = effect("pocket_current", "Pocket Current", 3, 16, "frost", EffectKind.WORLD, "Water envelopes carry up to sixteen ready, permitted loose items into your own tracked chest. Both halves need ownership and permission; full slots, foreign drops, locks and loot tables refuse without item loss.");
	public static final RuneDef WAYLINE = effect("wayline", "Wayline", 3, 18, "arcane", EffectKind.MOVEMENT, "Latch a short line to owned safe ground within six blocks. Hold forward for a bounded two-second tow; crouch to release. Normal collision remains, no one else is moved, and no flight ability is granted.");
	public static final RuneDef NIGHT_SEAM = effect("night_seam", "Night Seam", 3, 12, "void", EffectKind.WORLD, "Inspect beyond at most three owned ordinary stone cells for one open-ground and nearby-threat clue. No blocks change, no one moves, and protected, special, block-entity, warded or unloaded walls refuse.");
	public static final RuneDef SHARD_COMPASS = effect("shard_compass", "Shard Compass", 3, 12, "arcane", EffectKind.WORLD, "Offer one carried raw iron, copper or gold sample to locate a nearby matching loaded permitted deposit with an accessible side. A successful find spends the sample and points a short graded pebble route; refusal preserves it. Never mines or creates rewards.");

	// ---- Signature fusions: each made only from two particular effects at the Fusion Altar (see Fusions.SIGNATURES),
	// before their elements' own fusion. Never crafted or found. Fused runes take no designs from the others' circles,
	// so where these sit changes no older rune's.

	public static final RuneDef FROSTWIRE = effect("frostwire", "Frostwire", 3, 18, "storm", EffectKind.HARMFUL, "Chills each target (Slowness II for 4 seconds); a moment later a current races through every chilled or frozen enemy within 6 blocks of it, 6 at most: 5 damage each, 7 to one frozen solid.", POWER, RADIUS, LINGER);
	public static final RuneDef SEETHE = effect("seethe", "Seethe", 3, 16, "fire", EffectKind.HARMFUL, "Traps each target in a bubble of boiling water for 2 seconds (1 fire damage every half second), then it bursts into scalding steam: 4 damage to every enemy within 2.5 blocks, blinded for 2 seconds and left soaked.", POWER, DURATION, RADIUS);
	public static final RuneDef BLOOMSTEP = effect("bloomstep", "Bloomstep", 3, 14, "life", EffectKind.MOVEMENT, "Steps you through a door of blossoms to where the spell landed (up to 32 blocks). Grass and flowers spring up where you left and where you arrive, and you and your allies within 3 blocks of where you arrive get Regeneration I for 5 seconds.", DURATION, RADIUS);
	public static final RuneDef SKYBURST = effect("skyburst", "Skyburst", 3, 20, "fire", EffectKind.HARMFUL, "Flings each target high into the air (3 at most); at the top of its flight it explodes and rains fire down: 7 damage to it and every enemy within 3 blocks of it or beneath it, setting them alight, and the wind that carried it fans the flames (Wildfire). Never breaks blocks.", POWER, RADIUS);
	public static final RuneDef STITCHTIME = effect("stitchtime", "Stitchtime", 3, 16, "life", EffectKind.HELPFUL, "Heals the ally 4 and stitches the next 4 seconds: every wound they take meanwhile is counted, and when the time is up it all heals back at once (12 at most).", POWER, DURATION);
	public static final RuneDef PARASITE = effect("parasite", "Parasite", 3, 16, "blood", EffectKind.HARMFUL, "Plants a parasite in each target for 6 seconds: Poison I, and every second it drains 1 health from it into you. If its host dies with it inside, it leaps to the nearest enemy within 6 blocks for the time it had left (once).", POWER, DURATION);
	public static final RuneDef RAZORGALE = effect("razorgale", "Razorgale", 3, 18, "wind", EffectKind.HARMFUL, "A whirl of blades round where it lands: every enemy within 3 blocks is cut for 2 and left bleeding; half a second later the gale comes back round for 2 more, tearing every wound open (Rupture).", POWER, RADIUS);
	public static final RuneDef DOOMCLOCK = effect("doomclock", "Doomclock", 4, 28, "time", EffectKind.HARMFUL, "Sets a clock ticking on each target (3 at most) for 3 seconds: every blow it takes meanwhile (up to four a second) winds it 2 tighter, 12 at most. At zero it bursts: 8 damage and all it was wound, to it and every enemy within 3 blocks (an enemy takes one burst a cast). Never breaks blocks.", POWER, DURATION, RADIUS);
	public static final RuneDef THUNDERSTEP = effect("thunderstep", "Thunderstep", 3, 18, "storm", EffectKind.MOVEMENT, "You come down as a bolt of lightning where the spell landed (up to 24 blocks), right behind the first enemy it hit: 8 damage to every enemy within 2.5 blocks of you, stunned for half a second. On Self it strikes where you stand.", POWER, RADIUS);
	public static final RuneDef HALO = effect("halo", "Halo", 3, 18, "arcane", EffectKind.HELPFUL, "A halo crowns the ally for 8 seconds: every 1.5 seconds it smites an enemy within 6 blocks of them (whoever hurt them, or else the nearest) for 3 holy damage (tripled against undead), and the ally heals 1 each time.", POWER, DURATION, RADIUS);
	public static final RuneDef THUNDERQUAKE = effect("thunderquake", "Thunderquake", 3, 20, "earth", EffectKind.HARMFUL, "The ground booms like thunder: three shockwaves roll out from where it lands over a second, reaching 2, 4 and 6 blocks. Each strikes every enemy it reaches for 4 and tosses it up, so the nearer, the harder: 12 at the heart.", POWER, RADIUS);
	public static final RuneDef COMETFALL = effect("cometfall", "Cometfall", 4, 32, "arcane", EffectKind.HARMFUL, "A comet streaks down on the point a second later: 16 damage to every enemy within 4 blocks, setting them alight, and five shards of it scatter into the nearest other enemies within 10 blocks for 4 each. Never breaks blocks.", POWER, RADIUS);
	public static final RuneDef RIPOSTE = effect("riposte", "Riposte", 3, 16, "time", EffectKind.HELPFUL, "For 10 seconds the ally sees the next 2 blows coming: each is sidestepped, and answered at once with the blow's own damage (4 to 12) to whoever struck.", POWER, DURATION);
	public static final RuneDef DUST_DEVIL = effect("dust_devil", "Dust Devil", 4, 26, "wind", EffectKind.HARMFUL, "A dust devil touches down where it lands and chases the nearest enemy for 5 seconds. Enemies within 2 blocks of it are caught up and whirled round it, blinded and scoured for 3 damage a second; when it blows out it flings them high.", POWER, DURATION, RADIUS);
	public static final RuneDef MALISON = effect("malison", "Malison", 3, 16, "void", EffectKind.HARMFUL, "4 damage and a curse for 8 seconds: your spells hit it 25% harder, and it's shadowed. If it dies cursed, the curse passes on, 5% stronger each time (up to 45%), for the time it had left, to up to 3 enemies within 6 blocks of it.", POWER, DURATION, RADIUS);
	public static final RuneDef AVALANCHE = effect("avalanche", "Avalanche", 3, 18, "frost", EffectKind.HARMFUL, "Snow and ice crash down round where it lands: 6 damage to every enemy within 3 blocks (half again on a bare head), buried in snow (Slowness III for 3 seconds). Drifts of snow lie where it fell for 10 seconds.", POWER, RADIUS, DURATION);

	/** Signature fused effects: made only from their own two runes at the Fusion Altar. */
	public static final java.util.List<RuneDef> SIGNATURE = java.util.List.of(FROSTWIRE, SEETHE, BLOOMSTEP, SKYBURST, STITCHTIME, PARASITE, RAZORGALE,
		DOOMCLOCK, THUNDERSTEP, HALO, THUNDERQUAKE, COMETFALL, RIPOSTE, DUST_DEVIL, MALISON, AVALANCHE, CINDER_BULWARK, ROOT_BULWARK, BOILING_SURGE, THUNDER_TIDE, RIME_CAUSEWAY, THUNDER_WALK, SPRINGBED, CINDER_SIEVE, ASHEN_MERCY, CLOCKROOT, SKYLATCH, THRESHERWIND, NULLCATCH, SECOND_BELL, RED_LEDGER, QUIETUS, BLOOD_ESCROW, FROST_MOLT, PULSE_FERRY, LAST_LANTERN, POCKET_CURRENT, WAYLINE, NIGHT_SEAM, SHARD_COMPASS);
	// ---- Runes of the world: never crafted, only found in particular places (see RuneSources and
	// Attunements). Defined after everything else, so their magic circles never change an older rune's.
	// Vanilla structures.
	public static final RuneDef ECHOLOCATE = effect("echolocate", "Echolocate", 2, 6, "void", EffectKind.HARMFUL, "A sonar pulse from the point: every enemy within 16 blocks glows through walls for 10 seconds, and those it hits are dazed (Slowness II) for 3.", DURATION, RADIUS);
	public static final RuneDef RESONANT_SHRIEK = effect("resonant_shriek", "Resonant Shriek", 3, 18, "void", EffectKind.HARMFUL, "A sculk shriek: 8 damage that ignores armour, a stagger, and Darkness for 6 seconds. A second later it echoes for half as much.", POWER, DURATION, LINGER);
	public static final RuneDef TIDECALL = effect("tidecall", "Tidecall", 3, 16, "frost", EffectKind.HARMFUL, "The tide crashes in at the point: 6 damage to every enemy within 3.5 blocks, dragging them into the middle (as a Pull does) and leaving them soaked. A crowd it bunches takes 1 more for each neighbour (4 at most).", POWER, RADIUS, LINGER);
	public static final RuneDef INFEST = effect("infest", "Infest", 2, 9, "earth", EffectKind.HARMFUL, "Silverfish burrow out of the stone around each target: 1 damage every half second for 4 seconds, and Slowness I.", POWER, DURATION);
	public static final RuneDef SANDSTORM = effect("sandstorm", "Sandstorm", 3, 18, "earth", EffectKind.HARMFUL, "A sandstorm whirls at the point for 4 seconds: every enemy within 3.5 blocks takes 2 damage a second, can't see and is slowed.", POWER, RADIUS, DURATION);
	public static final RuneDef VINELASH = effect("vinelash", "Vinelash", 2, 9, "life", EffectKind.HARMFUL, "A thorned vine lashes each target: 5 damage, and it's yanked 4 blocks toward you and tripped (Slowness II for 2 seconds).", POWER, LINGER);
	public static final RuneDef REMEDY = effect("remedy", "Remedy", 2, 10, "life", EffectKind.HELPFUL, "Cures what ails and turns it to good (poison to Regeneration, slowness to Speed, weakness to Strength...) for half its time; heals 4 and gives Regeneration I for 6 seconds. A zombie villager it touches is weakened, ready for a golden apple.", POWER, DURATION);
	public static final RuneDef WARCRY = effect("warcry", "Warcry", 2, 10, "blood", EffectKind.HELPFUL, "A war horn sounds: you and your allies within 8 blocks of the target gain Strength I and Speed I for 12 seconds, and each kill one of you makes meanwhile heals that one for 1.", DURATION, RADIUS);
	public static final RuneDef FANGS = effect("fangs", "Fangs", 2, 10, "arcane", EffectKind.HARMFUL, "A ring of evoker fangs snaps up around each target: 6 damage from below.", POWER, LINGER);
	public static final RuneDef UNDERTOW = effect("undertow", "Undertow", 2, 9, "frost", EffectKind.HARMFUL, "Drags each target down: Slowness III for 3 seconds and soaked. Out of the water it hauls the target toward the nearest water within 6 blocks (with none near, the ground turns to slurry for 3 damage); in water it's pulled under and takes 5.", POWER, DURATION);
	public static final RuneDef TREASURE_SENSE = effect("treasure_sense", "Treasure Sense", 1, 4, "arcane", EffectKind.HELPFUL, "For 60 seconds, Luck II, and the nearest unopened treasure chests and suspicious blocks within 24 blocks sparkle now and then.", DURATION);
	public static final RuneDef TUSK_CHARGE = effect("tusk_charge", "Tusk Charge", 2, 9, "earth", EffectKind.MOVEMENT, "You charge like a hoglin, up to 8 blocks the way you look, tossing everything in your path into the air: 3 damage, and 0.8 more for every block you ran up (9 at most).", POWER);
	public static final RuneDef BLAZECALL = effect("blazecall", "Blazecall", 2, 11, "fire", EffectKind.HARMFUL, "Three blaze fireballs fall on each target over a second: 3 fire damage each, setting it alight and staggering it.", POWER, LINGER);
	public static final RuneDef SHULKERSHELL = effect("shulkershell", "Shulkershell", 3, 14, "void", EffectKind.HELPFUL, "Shuts the target in a shulker's shell for 4 seconds: 80% less damage and no knockback, but it can't move. When it opens, what it turned aside leaves as up to 3 bullets that seek the nearest enemies (6 damage each at most, and they float), and enemies within 3 blocks float up for 2 seconds.", DURATION);
	public static final RuneDef PORTALFALL = effect("portalfall", "Portalfall", 2, 9, "void", EffectKind.HARMFUL, "A portal opens under each target and drops it from up to 7 blocks up, with 2 damage on the way through; where it lands, enemies within 2 blocks take 3 and stagger.", POWER);
	public static final RuneDef ANCIENT_SEED = effect("ancient_seed", "Ancient Seed", 1, 4, "life", EffectKind.WORLD, "Plants an ancient seed at the point: a torchflower or pitcher plant blooms, and crops within 4 blocks grow a stage now and every 3 seconds for 12 seconds more.", RADIUS);
	public static final RuneDef VORTEX = shape("vortex", "Vortex", 3, 9, 2.8, "A whirling vortex opens where you look for 3 seconds, dragging creatures within 5 blocks into its eye and striking everything in the eye twice a second.", RADIUS, DURATION);
	public static final RuneDef SNARE = shape("snare", "Snare", 2, 4, 1.4, "Strings an unseen tripwire from your feet to where you look (up to 12 blocks). The first enemy to cross it within 30 seconds springs it on everything within 2.5 blocks, and whoever it catches stumbles for a second.", RADIUS);
	public static final RuneDef TRIAL_KEY = modifier("trial_key", "Trial Key", 2, 1.3, POWER, "Opens a fight: +60% power against targets at full health. One per effect.");
	public static final RuneDef IF_WOUNDED = link("if_wounded", "If Wounded", 2, 1, "The rest fires only if you're below half health. Build a last stand into any spell.");
	public static final RuneDef IF_OUTNUMBERED = link("if_outnumbered", "If Outnumbered", 3, 1, "The rest fires only if 3 or more enemies are within 8 blocks of you.");
	// Attuned from a Blank Rune in the right biome (see Attunements).
	public static final RuneDef MOONPETAL = effect("moonpetal", "Moonpetal", 2, 12, "life", EffectKind.HARMFUL, "A storm of moonlit petals at the point: 5 damage to every enemy within 3 blocks, and 4 health to you and your allies there. Stronger under a full moon and at night, weaker under a new moon.", POWER, RADIUS);
	public static final RuneDef HOARFROST = effect("hoarfrost", "Hoarfrost", 3, 16, "frost", EffectKind.HARMFUL, "Rime creeps over each target for 3 seconds, slowing it more every second; then it freezes solid for 2 seconds and takes 6 damage, and the frost blooms: enemies within 2 blocks take 3 and are slowed. Cast again on a creeping target, it starts nothing new.", POWER, DURATION);
	public static final RuneDef HUSH = effect("hush", "Hush", 2, 10, "void", EffectKind.HARMFUL, "A pocket of silence at the point for 6 seconds (4 blocks): monsters inside lose their targets and are weakened, and enemy casters can't cast.", DURATION, RADIUS);
	public static final RuneDef SPOREBLOOM = effect("sporebloom", "Sporebloom", 2, 10, "life", EffectKind.HARMFUL, "Spores burst from a giant mushroom at the point: enemies within 3 blocks take Poison I and spore damage, and monsters among them turn on each other for 5 seconds (players get Nausea). Your allies there get 4 hunger back.", DURATION, RADIUS);
	public static final RuneDef SUNSCORCH = effect("sunscorch", "Sunscorch", 3, 16, "fire", EffectKind.HARMFUL, "The noon sun, focused: 8 fire damage and alight for 5 seconds, and the target glows for 6. Under open sky by day it burns 50% hotter and blinds; bright light of its own counts as dusk (25% hotter).", POWER, DURATION, LINGER);
	public static final RuneDef MIRE = effect("mire", "Mire", 2, 8, "earth", EffectKind.HARMFUL, "The ground turns to mire under each target for 5 seconds: it sinks (Slowness IV, no jumping) and is soaked.", DURATION, LINGER);
	public static final RuneDef GLOWVINE = effect("glowvine", "Glowvine", 1, 3, "life", EffectKind.WORLD, "Glowing cave vines heavy with glow berries grow down from the ceiling around the point: a light that stays. Vines already there bear berries again.", RADIUS);
	public static final RuneDef ROOTSNARE = effect("rootsnare", "Rootsnare", 2, 11, "life", EffectKind.HARMFUL, "Mangrove roots burst up around the point: every enemy within 3 blocks is held for 1.5 seconds and takes 3 damage, then slowed for 4 seconds and cut by 1 damage for every 1.5 blocks it moves (5 at most).", POWER, RADIUS, DURATION);
	public static final RuneDef STALACTITE = effect("stalactite", "Stalactite", 2, 9, "earth", EffectKind.HARMFUL, "A stalactite drops on the spot each target stands on: 7 damage, 30% more against a bare head. Step aside and it misses.", POWER, LINGER);
	public static final RuneDef SUMMIT_WIND = effect("summit_wind", "Summit Wind", 3, 14, "wind", EffectKind.HARMFUL, "A howling mountain wind: 5 damage and hurls every enemy within 3 blocks up and away, holding them aloft so they glide down out of the fight (a fifth stronger above y=120). On Self it carries you 12 blocks up and lets you glide down.", POWER, RADIUS);
	public static final RuneDef SOULFIRE = effect("soulfire", "Soulfire", 3, 16, "fire", EffectKind.HARMFUL, "Blue soul flames: 3 fire damage a second for 5 seconds that water can't dull and the fire-proof can't shrug off, and the damage they deal gives you back a little mana (up to 5 a cast).", POWER, DURATION);
	public static final RuneDef WARP_STEP = effect("warp_step", "Warp Step", 2, 8, "void", EffectKind.MOVEMENT, "Steps you to where the spell landed (up to 24 blocks). 3 seconds later you're pulled back, unless you're sneaking.");
	public static final RuneDef BLOOD_MOSS = effect("blood_moss", "Blood Moss", 2, 10, "blood", EffectKind.HARMFUL, "Crimson moss spreads over each target: 1 damage a second for 6 seconds, and the most wounded of you and your allies nearby heals for all of it.", POWER, DURATION);
	public static final RuneDef BASALT_SURGE = effect("basalt_surge", "Basalt Surge", 3, 18, "earth", EffectKind.HARMFUL, "Basalt columns burst up in a line from you to the point: 7 damage and a toss into the air for everything along it.", POWER, RADIUS);
	public static final RuneDef STARLIGHT_TETHER = effect("starlight_tether", "Starlight Tether", 3, 14, "arcane", EffectKind.HARMFUL, "Tethers each target to the point with a thread of starlight for 5 seconds: it's dragged back if it strays 3 blocks, taking 1 damage each time (once a second at most).", POWER, DURATION);
	// The Ember Sanctum and the Cinder Warden.
	public static final RuneDef CINDERBRAND = effect("cinderbrand", "Cinderbrand", 2, 9, "fire", EffectKind.HARMFUL, "Brands each target: 3 fire damage, and for 6 seconds your fire spells burn it 50% hotter and its burning hurts a little more.", POWER, DURATION);
	public static final RuneDef ASHEN_VEIL = effect("ashen_veil", "Ashen Veil", 3, 14, "fire", EffectKind.HELPFUL, "Wreathes the target in ash for 10 seconds: fire can't hurt it, and whatever strikes it up close is set alight for 4 seconds and blinded by the ash for a second.", DURATION);
	public static final RuneDef KINDLED = modifier("kindled", "Kindled", 3, 1.4, POWER, "+30% power, and the effect sets what it hits alight for 4 seconds.");
	public static final RuneDef CINDERHEART = effect("cinderheart", "Cinderheart", 4, 30, "fire", EffectKind.HELPFUL, "Your heart burns for 12 seconds: Strength II, fire can't hurt you, and every enemy within 4 blocks takes 3 fire damage a second. It can't be lit again until 24 seconds after it goes out.", DURATION, POWER);
	// The Astral Observatory and the Star Eater.
	public static final RuneDef CONSTELLATION = shape("constellation", "Constellation", 3, 8, 2.4, "Joins up to 5 enemies within 12 blocks of you in a constellation of light and strikes them all at once.", RADIUS);
	public static final RuneDef ECLIPSE = effect("eclipse", "Eclipse", 3, 18, "void", EffectKind.HARMFUL, "A dark disc eclipses the point for 5 seconds: enemies beneath it (4 blocks) are blinded, take 2 damage a second, and your spells hit them 20% harder. Under it the light counts as dim.", POWER, DURATION, RADIUS);
	public static final RuneDef STARMAW = effect("starmaw", "Starmaw", 4, 32, "void", EffectKind.HARMFUL, "Devours the light: 14 damage, and it swallows each of the target's good effects and wards (Foresight, Riposte, Reflect, Reversal, Infinity, Anchor) for 4 more damage apiece, and its absorption for 1 per heart.", POWER);
	// The Drowned Scriptorium and the Tide Scribe.
	public static final RuneDef DROWNING_WORD = effect("drowning_word", "Drowning Word", 3, 16, "frost", EffectKind.HARMFUL, "A drowning word: for 5 seconds the target's lungs fill with water (it loses its air, takes 2 damage a second and can't cast), and it's soaked.", POWER, DURATION);
	public static final RuneDef IF_WET = link("if_wet", "If Wet", 2, 1, "The rest fires only if you're in water or rain.");
	public static final RuneDef TIDEWRIT = effect("tidewrit", "Tidewrit", 4, 30, "frost", EffectKind.HARMFUL, "Writes the tide: a 7-wide wall of water rolls from you through the point, 10 damage to everything in it, sweeping it about 11 blocks on, soaked.", POWER, RADIUS);
	// World events: the Fallen Star, Rift sieges and mana storms.
	public static final RuneDef STARSHARD = effect("starshard", "Starshard", 3, 16, "arcane", EffectKind.HARMFUL, "A shard of the fallen star: 11 damage, then it splinters into 3 sparks that strike the nearest other enemies within 8 blocks for 4.", POWER, LINGER);
	public static final RuneDef RIFTCALL = effect("riftcall", "Riftcall", 3, 18, "void", EffectKind.HARMFUL, "Opens a rift at the point for 3 seconds: it drags enemies within 5 blocks toward it for 2 damage a second, then snaps shut on them for 6. It gapes wider for every creature it holds (up to 5): half a block of reach and 1 more damage each.", POWER, RADIUS, DURATION);
	public static final RuneDef UNSTABLE = modifier("unstable", "Unstable", 3, 1.2, POWER, "Rift-touched: the effect's power swings anywhere from 50% to 200% each time it lands.");
	public static final RuneDef MANABURN = effect("manaburn", "Manaburn", 2, 10, "arcane", EffectKind.HARMFUL, "Burns magic: 5 damage. A spellcaster (a player wearing a Cord, or a Runebound) also takes 4 more, a player loses up to 20 mana (less from a weaker hit), and a charge or telegraphed cast in hand is cut short.", POWER);
	public static final RuneDef MANATIDE = effect("manatide", "Manatide", 3, 10, "arcane", EffectKind.HELPFUL, "Drinks in the storm: for 10 seconds, every spell you and your allies hit cast gives back a quarter of its mana (30 at most, however extended). Each player can drink only once a minute.", DURATION);
	// Fished from open water.
	public static final RuneDef TIDEHOOK = effect("tidehook", "Tidehook", 2, 9, "frost", EffectKind.HARMFUL, "A hook of water snags each target and reels it in to your feet in three tugs (a flyer is reeled down): 4 damage, it's left soaked and gasping for half a second when it lands.", POWER, LINGER);
	public static final RuneDef CURRENT = effect("current", "Current", 2, 6, "frost", EffectKind.MOVEMENT, "Only in water or rain: a current sweeps you and the allies within 2 blocks about 15 blocks the way you look, and you land without fall damage. On dry land it fizzles.", POWER);

	// ---- new runes (batch 2): more ways to build a spell. Crafted like the rest (see cast/CraftedRunes and
	// cast/CraftedShapes), and defined last, so no older rune's magic circle changes.
	public static final RuneDef GLAIVE = shape("glaive", "Glaive", 2, 5, 1.8, "A spinning glaive flies out up to 12 blocks and curves back to you, striking everything it passes on the way out and again on the way back.", RADIUS, SPEED, SPLIT);
	public static final RuneDef IMPRINT = shape("imprint", "Imprint", 1, 3, 1.3, "Leaves an imprint of the spell where you stand. 2 seconds later it erupts, striking everything within 3 blocks of it.", RADIUS, SPEED, SPLIT);
	public static final RuneDef LATCH = shape("latch", "Latch", 2, 6, 2.15, "A thread of light latches onto the first creature within 16 blocks of your aim and strikes it 4 times, a second apart, at 70% power, while it stays within 24 blocks and in sight.", DURATION, SPEED);
	public static final RuneDef KINDRED = modifier("kindred", "Kindred", 2, 1.4, SHARE, "Shares a helpful effect: it also lands on you and on the nearest ally it missed within 8 blocks, at half power.");
	public static final RuneDef THIRST = modifier("thirst", "Thirst", 2, 1.4, POWER, "You heal for a quarter of the damage the effect deals.");
	public static final RuneDef BELATED = modifier("belated", "Belated", 2, 1.25, POWER, "The effect lands 1.5 seconds late, but 25% stronger.");
	public static final RuneDef ON_REACTION = link("on_reaction", "On Reaction", 3, 2, "The rest fires at each creature the shape before it sets off an element reaction on (Shatter, Conduct, Blight...).");
	public static final RuneDef ON_WEAKNESS = link("on_weakness", "On Weakness", 2, 2, "The rest fires at each creature the shape before it strikes with an element it's weak to.");
	public static final RuneDef SPELLBRAND = effect("spellbrand", "Spellbrand", 2, 8, "arcane", EffectKind.HARMFUL, "Brands each target with a sigil for 8 seconds. The next time your magic hurts it, the sigil bursts for 7 damage of the element of the spell that set it off.", POWER, DURATION);
	public static final RuneDef GASH = effect("gash", "Gash", 2, 9, "blood", EffectKind.HARMFUL, "A wound that won't close: 3 damage, and for 8 seconds the target can't heal, is left bleeding, and weeps 0.4 damage plus 1% of its health a second.", POWER, DURATION, LINGER);
	public static final RuneDef PROSPECT = effect("prospect", "Prospect", 1, 3, "earth", EffectKind.WORLD, "The ground rings out: every ore within 12 blocks of where it lands glows through the rock for 20 seconds.", RADIUS, DURATION);
	public static final RuneDef SEARING_EDGE = effect("searing_edge", "Searing Edge", 2, 8, "fire", EffectKind.HELPFUL, "For 15 seconds the target's weapon sears: each melee hit it lands sets the foe alight for 4 seconds and deals 2 more fire damage.", POWER, DURATION);
	public static final RuneDef FLASH_FREEZE = effect("flash_freeze", "Flash Freeze", 2, 9, "frost", EffectKind.HARMFUL, "4 freeze damage. A soaked target (in water, or after a water rune or Bubble) freezes solid for 3 seconds (1.5 on players), one only rained on for 2 (1); a dry one is slowed and left brittle.", POWER, DURATION, LINGER);
	public static final RuneDef DROWSE = effect("drowse", "Drowse", 3, 14, "life", EffectKind.HARMFUL, "Sleepy pollen lulls each target to sleep for 6 seconds (2 on players): it can't move or fight back, but any damage wakes it, and the blow that does deals 75% more. Bosses only grow drowsy.", DURATION);
	public static final RuneDef GALVANIZE = effect("galvanize", "Galvanize", 1, 3, "storm", EffectKind.WORLD, "Sets a spark of raw power against the block it strikes for 5 seconds: it powers what it touches as a redstone block would (doors open, lamps light, pistons push).", DURATION);
	public static final RuneDef PROLONG = effect("prolong", "Prolong", 3, 12, "time", EffectKind.HELPFUL, "Every good effect on the target (Speed, Strength, Regeneration, a potion's...) lasts 15 seconds longer, up to 5 minutes.", DURATION);
	public static final RuneDef UMBRA = effect("umbra", "Umbra", 1, 6, "void", EffectKind.HARMFUL, "The dark bites: 4 damage, doubled where the light is dim (level 7 or less, or under an Eclipse), and it leaves the target shadowed.", POWER, LINGER);
	public static final RuneDef DISARM = effect("disarm", "Disarm", 2, 7, "wind", EffectKind.HARMFUL, "A snatching gust tears the weapon from each creature's hand for 5 seconds, then it drifts back. Bosses keep hold; a player keeps hold too, but can't use what they're holding for 3 seconds.", DURATION);

	// ---- Flight (see cast/Soar): crafted like the rest, and defined last, so no older rune's magic circle changes.
	public static final RuneDef SOAR = effect("soar", "Soar", 3, 18, "wind", EffectKind.HELPFUL, "Wings of wind let you fly for 20 seconds: double-tap jump to take off, then jump and sneak to rise and sink. It can't be renewed mid-flight, and when the wind fades it sets you down gently, then your wings need 30 seconds' rest. A pull or a grounding wind tears it away, and it won't lift anyone in a warded arena.", DURATION);

	// New shapes are appended too: generated circles retain the roster order of existing runes.
	public static final RuneDef RELAY = shape("relay", "Relay", 4, 24, 1.0, "An Archive lesson for active Circle VIII and an Echo Cord: place a visible focus within 8 blocks, then press cast again within 4 seconds to release one aimed ray. Relay then Harm, Frost or Shock only; 90% normal strength, 16 blocks total path, 8-second shared rest. No modifiers, links, Knots, woven runes or storage.");

	public static final RuneDef EXCISE = effect("excise", "Excise", 4, 27.5, "life", EffectKind.HARMFUL, "Rootbound study teaches active Circle XVI to cut one visible hostile native Zone core within 12 blocks. Hold Cast for 16 ticks; 36 base mana once, 12-second shared rest and 12-tick recovery. Only Beam then Excise. Damage, broken sight or more than one block of movement cancels without refund. Existing poison, fire and sibling fields survive.");
	public static final RuneDef REWEAVE = shape("reweave", "Reweave", 4, 32, 1.0, "The Ebb Ledger teaches active Circle XII to rewrite one paid Harm field once. Place a radius-2 disc within 8 blocks; its four half-Harm beats remain at 0.4, 1.4, 2.4 and 3.4 seconds, expiring at 4 seconds. A fresh cast press fixes a 7 by 1.25 block lane after a silent 0.4-second warning. Forty base mana once, eight-second shared rest. Reweave then Harm only; no modifiers, links, composites or storage.");
	// ---- The first authored lesson pack (see LessonPackRules): appended so no older rune's circle changes.
	public static final RuneDef TOLLGATE = effect("tollgate", "Tollgate", 2, 6.77, "earth", EffectKind.HARMFUL, "The Warden's Threshold teaches active Circle X: set a 5-block gate on visible floor within 10 blocks for 6 seconds. A hostile walking into it is stopped once and slowed; 3 tolls in all. Allies pass and a jump clears it. 30 mana once, 10-second shared rest. Wall then Tollgate only.");
	public static final RuneDef LIFELINE = effect("lifeline", "Lifeline", 3, 20.83, "arcane", EffectKind.HELPFUL, "The Thread Between Stars teaches active Circle XIV: thread one ally, pet or summon you can see within 16 blocks, then press again within 8 seconds to pull them to a safe spot beside you. A crouching ally refuses. 28 mana once, 10-second shared rest. Beam then Lifeline only.");
	public static final RuneDef CONDUIT = effect("conduit", "Conduit", 3, 19.28, "storm", EffectKind.MOVEMENT, "Notes on a Grounded Storm teach active Circle XVIII: plant a rod on visible floor within 20 blocks for 20 seconds, then press again to spark for 0.4 seconds and arrive on it. Damage, a blocked line or a hostile beside the rod stops you. 32 mana once, 15-second shared rest. Pillar then Conduit only.");
	// ---- fx-passive pack (see cast/HearthEffects, HearthRules): gentle long-lasting utility, appended so no older rune's circle changes.
	public static final RuneDef SLOWBURN = effect("slowburn", "Slowburn", 1, 5, "life", EffectKind.HELPFUL, "Hunger drains half as fast for 10 minutes. Can be a passive.");
	public static final RuneDef CAMP_WARD = effect("camp_ward", "Camp Ward", 2, 12, "arcane", EffectKind.WORLD, "Wards an 8-block camp for 5 minutes: monsters that appear inside fade at once, and phantoms nearby lose their prey. One per caster.");
	public static final RuneDef WARM_CLOAK = effect("warm_cloak", "Warm Cloak", 1, 4, "fire", EffectKind.HELPFUL, "You can't freeze for 10 minutes, even in powder snow. Can be a passive.");
	public static final RuneDef SOFTSOLE = effect("softsole", "Softsole", 1, 3, "wind", EffectKind.HELPFUL, "You don't trample farmland when you jump or step down onto it, for 10 minutes. Can be a passive.");
	public static final RuneDef SOFTFOOT = effect("softfoot", "Softfoot", 2, 8, "void", EffectKind.HELPFUL, "For 5 minutes, monsters more than 8 blocks away lose track of you, unless you struck them in the last 10 seconds. Can be a passive.");
	public static final RuneDef HOLLOW_POCKET = effect("hollow_pocket", "Hollow Pocket", 2, 6, "void", EffectKind.WORLD, "Opens your own 9-slot pocket. What you keep there stays yours, through death too.");
	public static final RuneDef LODESTAR = effect("lodestar", "Lodestar", 2, 6, "arcane", EffectKind.WORLD, "Sets your lodestar where it lands, or at your feet. Homeward takes you back to it.");
	public static final RuneDef HOMEWARD = effect("homeward", "Homeward", 4, 22, "void", EffectKind.MOVEMENT, "Hold still for 3 seconds to return to your lodestar, in this world and within 2000 blocks. Moving or getting hurt breaks it. 2-minute rest.");
	public static final RuneDef GRAVEFINDER = effect("gravefinder", "Gravefinder", 1, 3, "arcane", EffectKind.WORLD, "For a minute, pale motes point the way to where you last died.");
	public static final RuneDef SKYREAD = effect("skyread", "Skyread", 1, 2, "storm", EffectKind.WORLD, "Tells you the weather, how long it will hold, the hour and the moon.");
	public static final RuneDef LULLABY = effect("lullaby", "Lullaby", 2, 8, "life", EffectKind.HELPFUL, "You and allies within 8 blocks count as rested, so phantoms leave you be. Tells you how many are asleep.");
	public static final RuneDef STEEDSONG = effect("steedsong", "Steedsong", 2, 8, "wind", EffectKind.HELPFUL, "Your mount, or the animal it touches, gets Speed II and Jump Boost II for 3 minutes.");
	public static final RuneDef GLIDEWIND = effect("glidewind", "Glidewind", 3, 14, "wind", EffectKind.HELPFUL, "For 2 minutes, a tailwind keeps your elytra glide from slowing down, up to a steady cruise.");
	public static final RuneDef WAYMARK = effect("waymark", "Waymark", 1, 3, "arcane", EffectKind.WORLD, "Raises a pillar of light only you can see, from 160 blocks, for 10 minutes. Up to 3 at once.");
	public static final RuneDef EMBER_REST = effect("ember_rest", "Ember Rest", 2, 10, "fire", EffectKind.WORLD, "Kindles a resting fire for 5 minutes: allies within 6 blocks regenerate and stay warm. One per caster.");
	public static final RuneDef ORBCALL = effect("orbcall", "Orbcall", 1, 3, "arcane", EffectKind.HELPFUL, "Experience orbs within 10 blocks drift to you for 5 minutes. Can be a passive.");
	public static final RuneDef TINKER_HUM = effect("tinker_hum", "Tinker's Hum", 2, 10, "earth", EffectKind.HELPFUL, "For 5 minutes, a worn tool or piece of armor you carry mends a point every 10 seconds, up to 30.");
	public static final RuneDef LANTERN_SOUL = effect("lantern_soul", "Lantern Soul", 1, 4, "arcane", EffectKind.HELPFUL, "An unseen light follows you for 5 minutes. Can be a passive.");
	public static final RuneDef KEENKEEP = effect("keenkeep", "Keenkeep", 2, 8, "earth", EffectKind.HELPFUL, "Your held tool wears half as fast for 5 minutes. Can be a passive.");
	public static final RuneDef LANDREAD = effect("landread", "Landread", 1, 2, "earth", EffectKind.WORLD, "Tells you the biome, height and light where it lands, and whether slimes spawn there.");
	public static final RuneDef RALLY_LIGHT = effect("rally_light", "Rally Light", 3, 18, "arcane", EffectKind.WORLD, "Raises a beacon for 3 minutes: allies within 10 blocks get Haste I and Speed I. One per caster.");
	public static final RuneDef DEW_DRINK = effect("dew_drink", "Dew Drink", 1, 4, "life", EffectKind.HELPFUL, "For 10 minutes, rain or water feeds you a hunger point every 15 seconds.");
	public static final RuneDef SUNBASK = effect("sunbask", "Sunbask", 1, 4, "fire", EffectKind.HELPFUL, "For 10 minutes, open daylight heals you a point every 6 seconds while nothing hurts you.");
	public static final RuneDef CURRENTKIN = effect("currentkin", "Currentkin", 2, 6, "frost", EffectKind.HELPFUL, "Swim with the dolphins' grace for 3 minutes. Can be a passive.");
	public static final RuneDef SUREFOOT = effect("surefoot", "Surefoot", 1, 4, "earth", EffectKind.HELPFUL, "Step up full blocks without jumping for 5 minutes. Can be a passive.");
	public static final RuneDef LONG_ARM = effect("long_arm", "Long Arm", 2, 6, "earth", EffectKind.HELPFUL, "Reach 2 blocks farther to build and mine for 5 minutes. Can be a passive.");
	public static final RuneDef NIGHTWATCH = effect("nightwatch", "Nightwatch", 2, 6, "arcane", EffectKind.HELPFUL, "For 10 minutes, a bell warns you when a monster within 16 blocks starts hunting you. Can be a passive.");
	public static final RuneDef TRAILBLAZE = effect("trailblaze", "Trailblaze", 1, 3, "life", EffectKind.WORLD, "For 10 minutes, you leave green crumbs every 6 blocks that only you can see. Up to 40.");
	public static final RuneDef HEARTHPATH = effect("hearthpath", "Hearthpath", 1, 3, "arcane", EffectKind.WORLD, "For a minute, warm motes point the way to your bed or spawn.");
	public static final RuneDef LOSTFIND = effect("lostfind", "Lostfind", 1, 3, "arcane", EffectKind.WORLD, "For 30 seconds, gold motes point to the nearest loose items within 32 blocks.");
	public static final RuneDef STILLWELL = effect("stillwell", "Stillwell", 2, 6, "arcane", EffectKind.HELPFUL, "For 5 minutes, standing still for 3 seconds slowly refills your mana, up to 12.");
	public static final RuneDef STARCHART = effect("starchart", "Starchart", 1, 2, "arcane", EffectKind.WORLD, "Tells you where you are, which way you face, and how far it is to spawn.");
	public static final RuneDef PETWARD = effect("petward", "Petward", 2, 8, "life", EffectKind.WORLD, "Your pets within 16 blocks get Resistance I for 3 minutes.");
	public static final RuneDef WHISTLE = effect("whistle", "Whistle", 1, 3, "wind", EffectKind.WORLD, "Your pets within 48 blocks that aren't sitting or leashed come to your side.");
	public static final RuneDef LUCKCHARM = effect("luckcharm", "Luckcharm", 1, 4, "life", EffectKind.HELPFUL, "Luck I for 5 minutes. Can be a passive.");
	public static final RuneDef SMOKE_SIGNAL = effect("smoke_signal", "Smoke Signal", 1, 2, "fire", EffectKind.WORLD, "A tall column of smoke rises where it lands for 2 minutes.");
	public static final RuneDef WAYFARER_HYMN = effect("wayfarer_hymn", "Wayfarer's Hymn", 3, 16, "wind", EffectKind.HELPFUL, "You and allies within 10 blocks get Speed I and Jump Boost I for 3 minutes. It ends for anyone who strikes.");
	public static final RuneDef STEEDMEND = effect("steedmend", "Steedmend", 1, 4, "life", EffectKind.HELPFUL, "Your mount, or the animal it touches, regenerates for 3 minutes.");
	public static final RuneDef DYNAMO_STRIDE = effect("dynamo_stride", "Dynamo Stride", 2, 6, "storm", EffectKind.HELPFUL, "For 5 minutes, every 24 blocks you walk gives back 2 mana, up to 12.");
	public static final RuneDef TARRY = effect("tarry", "Tarry", 2, 10, "time", EffectKind.HELPFUL, "For 5 minutes, your good effects run down at half speed.");
	public static final RuneDef CLOT = effect("clot", "Clot", 1, 4, "blood", EffectKind.HELPFUL, "For 3 minutes, poison and wither on you wear off twice as fast.");
	public static final RuneDef HEARTSENSE = effect("heartsense", "Heartsense", 2, 6, "blood", EffectKind.HELPFUL, "For 30 seconds, you see the heartbeat of every creature within 24 blocks.");
	public static final RuneDef QUENCH = effect("quench", "Quench", 1, 3, "frost", EffectKind.HELPFUL, "Puts you out, and for 3 minutes fire on you burns out three times as fast.");
	public static final RuneDef HEARTHBOND = effect("hearthbond", "Hearthbond", 3, 16, "life", EffectKind.HELPFUL, "Bonds you and allies within 12 blocks for 3 minutes: when one drops low they regenerate, and the rest hear it. Once a minute each.");
	public static final RuneDef SPRINGSEEK = effect("springseek", "Springseek", 1, 3, "frost", EffectKind.WORLD, "For 30 seconds, blue motes point to the nearest water source within 24 blocks.");
	public static final RuneDef SAVOR = effect("savor", "Savor", 1, 4, "time", EffectKind.HELPFUL, "For 5 minutes, food you eat fills you for longer.");
	public static final RuneDef DEEPWARN = effect("deepwarn", "Deepwarn", 1, 4, "earth", EffectKind.HELPFUL, "For 5 minutes, warns you of lava below or a long drop ahead. Can be a passive.");
	public static final RuneDef ENDERHUSH = effect("enderhush", "Enderhush", 2, 6, "void", EffectKind.HELPFUL, "For 10 minutes, endermen you haven't hurt forget their anger at you. Can be a passive.");

	// ---- shapes pack: field and kin shapes (see FieldShapeGeometry, cast/FieldShapes); appended so no older rune's circle changes.
	public static final RuneDef FURROW = shape("furrow", "Furrow", 1, 2, 1.0, "Strikes a row of 9 ground blocks running ahead of you, and whatever stands on them, at 70% power: a crop row to sow, grow or harvest.", RADIUS);
	public static final RuneDef PLOT = shape("plot", "Plot", 1, 2, 1.1, "Strikes a 3x3 patch of ground where you look, and whatever stands on it, at 80% power.", RADIUS);
	public static final RuneDef SEEDBED = shape("seedbed", "Seedbed", 2, 4, 1.4, "Strikes a 5x5 bed of ground where you look, and whatever stands on it, at 60% power: a whole field at once.", RADIUS);
	public static final RuneDef SHAFT = shape("shaft", "Shaft", 2, 4, 1.3, "Strikes a column of 8 blocks straight down from the block you look at, at 80% power: a mine shaft or a well.", RADIUS);
	public static final RuneDef STAIRWELL = shape("stairwell", "Stairwell", 2, 5, 1.5, "Strikes a stair of 8 steps going down ahead of you, three blocks high at each step, at 70% power: dig your way down and walk it.", RADIUS);
	public static final RuneDef CORRIDOR = shape("corridor", "Corridor", 2, 4, 1.4, "Strikes a corridor 2 high and 8 deep into the block you look at, along your facing, at 80% power.", RADIUS);
	public static final RuneDef SEAM = shape("seam", "Seam", 1, 2, 1.1, "Strikes a 7-block line across the block you look at, side to side, and what stands by it, at 90% power.", RADIUS);
	public static final RuneDef FACADE = shape("facade", "Facade", 2, 4, 1.3, "Strikes a 5-wide, 3-high face of blocks where you look, and whatever stands at it, at 80% power.", RADIUS);
	public static final RuneDef DOME = shape("dome", "Dome", 3, 7, 2.0, "Strikes the shell of a dome of radius 2 over where you look, and everything under it, at 90% power: carve a room or light a shelter.", RADIUS);
	public static final RuneDef FOOTING = shape("footing", "Footing", 1, 2, 1.0, "Strikes the 3x3 ground under your feet, and you and whatever stands on it, at 90% power.", RADIUS);
	public static final RuneDef CANOPY = shape("canopy", "Canopy", 2, 3, 1.2, "Strikes a 3x3 layer of blocks 3 above where you look (leaves, a roof), and whatever is beneath, at 90% power.", RADIUS);
	public static final RuneDef SHORELINE = shape("shoreline", "Shoreline", 2, 4, 1.3, "Strikes the water and the banks beside it within 4 blocks of where you look, and what wades there, at 70% power.", RADIUS);
	public static final RuneDef PERIMETER = shape("perimeter", "Perimeter", 2, 4, 1.4, "Strikes the 24-block edge of a 7x7 square where you look, and whatever stands on it, at 80% power: a fence line of light or thorns.", RADIUS);
	public static final RuneDef SPIRE = shape("spire", "Spire", 2, 3, 1.2, "Strikes a column 6 high rising from the block you look at, and whatever stands in it: a trunk, a pillar, a stack of cactus.", RADIUS);
	public static final RuneDef PIT = shape("pit", "Pit", 1, 3, 1.2, "Strikes a 3x3 pit 2 deep where you look, and whatever stands over it, at 90% power.", RADIUS);
	public static final RuneDef CROSSWAY = shape("crossway", "Crossway", 2, 4, 1.3, "Strikes a cross of ground with arms of 4 where you look, and whatever stands on it, at 80% power: two paths at once.", RADIUS);
	public static final RuneDef LODESEEK = shape("lodeseek", "Lodeseek", 2, 6, 1.5, "Seeks up to 8 ores within 3 blocks of the block you look at and strikes each, at 60% power. No ore, no strike.", RADIUS);
	public static final RuneDef VAULT = shape("vault", "Vault", 3, 7, 2.0, "Strikes the 3x3x3 cube of blocks around the block you look at, and whatever is inside, at 90% power.", RADIUS);
	public static final RuneDef LAMPLIT = shape("lamplit", "Lamplit", 1, 2, 1.0, "Strikes the four corners and the middle of a 9x9 square where you look, at 80% power: lamps for a yard.", RADIUS);
	public static final RuneDef FISSURE = shape("fissure", "Fissure", 2, 5, 1.6, "Cracks a jagged line of 10 blocks of ground ahead of you and strikes whatever stands along it, at 110% power.", RADIUS);
	public static final RuneDef SPIRAL = shape("spiral", "Spiral", 3, 7, 2.1, "Strikes a spiral of ground winding out 4 blocks from where you look, and whatever stands on it.", RADIUS);
	public static final RuneDef ROSETTE = shape("rosette", "Rosette", 2, 5, 1.6, "Strikes a rosette of 13 ground blocks across 9 blocks where you look, and whatever stands on them.", RADIUS);
	public static final RuneDef STEPSTONES = shape("stepstones", "Stepstones", 1, 2, 1.0, "Strikes 5 stepping stones of ground, every second block ahead of you, at 80% power: a path across water or dark.", RADIUS);
	public static final RuneDef CAUSEWAY = shape("causeway", "Causeway", 2, 4, 1.4, "Strikes a road of ground 3 wide and 8 long ahead of you, and whatever stands on it, at 70% power.", RADIUS);
	public static final RuneDef HEDGEROW = shape("hedgerow", "Hedgerow", 1, 3, 1.1, "Strikes a 9-block line of ground across where you look, side to side, and whatever stands on it, at 90% power.", RADIUS);
	public static final RuneDef LATTICE = shape("lattice", "Lattice", 2, 3, 1.2, "Strikes every other block of a 5x5 square where you look (13 in a checkerboard), at 80% power: spacing for crops or lights.", RADIUS);
	public static final RuneDef COLLAPSE = shape("collapse", "Collapse", 3, 8, 2.2, "Strikes a 3x3 slab two blocks thick at the block you look at, and everything up to 6 blocks beneath it, at 120% power: bring the ceiling down.", RADIUS);
	public static final RuneDef FAN = shape("fan", "Fan", 1, 3, 1.2, "Strikes five spokes of ground 3 to 6 blocks ahead of you in a 90-degree fan, and whatever stands on them.", RADIUS);
	public static final RuneDef BOBBER = shape("bobber", "Bobber", 1, 2, 1.0, "Lands on the first water within 24 blocks of your aim and strikes it and everything within 2.5 blocks: the fish, the drowned, the catch.", RADIUS);
	public static final RuneDef HERD = shape("herd", "Herd", 1, 3, 1.1, "Strikes every animal within 8 blocks of you, at 90% power: feed, heal or move the herd.", RADIUS);
	public static final RuneDef FELLOWSHIP = shape("fellowship", "Fellowship", 2, 5, 1.6, "Strikes you and every ally within 12 blocks (party, pets and team), at 80% power. Only allies: it never touches a foe.", RADIUS);
	public static final RuneDef SADDLE = shape("saddle", "Saddle", 1, 2, 1.0, "Strikes you, what you ride and whoever rides with you: speed a horse, shield a boat.");
	public static final RuneDef PACKBOND = shape("packbond", "Packbond", 1, 3, 1.1, "Strikes every pet of yours within 24 blocks, and nothing else.", RADIUS);
	public static final RuneDef NURSERY = shape("nursery", "Nursery", 1, 2, 1.0, "Strikes every young animal within 8 blocks of you: grow them up, feed them.", RADIUS);
	public static final RuneDef SHOAL = shape("shoal", "Shoal", 2, 4, 1.3, "Strikes every creature in water within 10 blocks of you, at 90% power: fish, squid, drowned and swimmers.", RADIUS);
	public static final RuneDef REARGUARD = shape("rearguard", "Rearguard", 2, 4, 1.4, "Strikes everything behind you within 8 blocks, at 110% power.", RADIUS);
	public static final RuneDef GRUDGE = shape("grudge", "Grudge", 2, 5, 1.5, "Strikes up to 6 creatures within 24 blocks that hurt you last or are hunting you, at 120% power.", RADIUS);
	public static final RuneDef SENTINEL = shape("sentinel", "Sentinel", 3, 8, 2.1, "Strikes up to 8 enemies within 16 blocks that are hunting you or an ally, at 110% power.", RADIUS);
	public static final RuneDef AUREOLE = shape("aureole", "Aureole", 1, 3, 1.2, "Strikes you and everything within 3 blocks of you, friend and foe alike: each effect finds its own.", RADIUS);
	public static final RuneDef TETHER = shape("tether", "Tether", 2, 4, 1.4, "Strikes the creature you look at (within 24 blocks) and everything within 3 blocks of it, at 110% power.", RADIUS);
	public static final RuneDef FLOCK = shape("flock", "Flock", 2, 4, 1.3, "Strikes every flying creature within 16 blocks: bats, bees, parrots, phantoms, vexes and ghasts.", RADIUS);
	// ---- fx-fish pack
	public static final RuneDef ANGLER_LURE = effect("angler_lure", "Angler's Lure", 1, 3, "frost", EffectKind.WORLD, "Your fishing bobber within 32 blocks: the wait for a bite is cut in half, leaving at least 1 second. Once per bobber.");
	public static final RuneDef BAIT_BLESSING = effect("bait_blessing", "Bait Blessing", 1, 4, "arcane", EffectKind.HELPFUL, "Luck for 60 seconds, Luck II with Amplify. Fishing reads your luck, so better catches come a little more often.", DURATION);
	public static final RuneDef REELING_TIDE = effect("reeling_tide", "Reeling Tide", 1, 2, "frost", EffectKind.WORLD, "Reels in your bobber with the rod in your hand. A fish that is biting is caught as usual.");
	public static final RuneDef SCHOOL_SIGHT = effect("school_sight", "School Sight", 2, 5, "arcane", EffectKind.WORLD, "Fish, squid, dolphins, turtles and axolotls within 16 blocks of the point glow for 12 seconds.", RADIUS, DURATION);
	public static final RuneDef TACKLE_MEND = effect("tackle_mend", "Tackle Mend", 1, 4, "arcane", EffectKind.HELPFUL, "Mends 16 durability of a held fishing rod, more with Power.", POWER);
	public static final RuneDef BOBBER_BELL = effect("bobber_bell", "Bobber Bell", 1, 2, "arcane", EffectKind.WORLD, "For 60 seconds your bobber rings and splashes when a fish bites.", DURATION);
	public static final RuneDef WATER_READING = effect("water_reading", "Water Reading", 1, 1, "arcane", EffectKind.WORLD, "Tells whether your bobber sits in open water (needed for treasure) and whether rain is speeding up bites.");
	public static final RuneDef DOLPHIN_CALL = effect("dolphin_call", "Dolphin Call", 2, 6, "frost", EffectKind.WORLD, "Dolphins within 24 blocks swim to you. If any come, you get Dolphin's Grace for 20 seconds.", DURATION);
	public static final RuneDef AXOLOTL_KINSHIP = effect("axolotl_kinship", "Axolotl Kinship", 2, 6, "frost", EffectKind.WORLD, "Axolotls within 12 blocks are healed and follow you for 30 seconds.", DURATION);
	public static final RuneDef SHOAL_HERD = effect("shoal_herd", "Shoal Herd", 1, 3, "frost", EffectKind.WORLD, "Fish within 10 blocks of the point swim to it for 10 seconds.", RADIUS);
	public static final RuneDef REFLOAT = effect("refloat", "Refloat", 1, 3, "frost", EffectKind.WORLD, "Fish, squid and dolphins stranded on land within 6 blocks are set back in water within 8 blocks.");
	public static final RuneDef REED_CUT = effect("reed_cut", "Reed Cut", 1, 3, "earth", EffectKind.WORLD, "Cuts kelp, seagrass, sugar cane and lily pads within 3 blocks of the point, up to 12. Kelp and cane keep their base to regrow.");
	public static final RuneDef WRING = effect("wring", "Wring", 2, 6, "frost", EffectKind.WORLD, "Soaks up water like a sponge within 2 blocks of the point, up to 24 blocks.");
	public static final RuneDef SPRING_DRAW = effect("spring_draw", "Spring Draw", 1, 2, "frost", EffectKind.WORLD, "Fills an empty bucket in your hand with water. Without one, places a water source where you aim.");
	public static final RuneDef BRIMMING = effect("brimming", "Brimming", 1, 2, "frost", EffectKind.WORLD, "Fills up to 4 cauldrons within 4 blocks of the point with water.");
	public static final RuneDef OCEANS_FAVOR = effect("oceans_favor", "Ocean's Favor", 4, 24, "frost", EffectKind.HELPFUL, "Water Breathing, Dolphin's Grace, Conduit Power and Night Vision for 120 seconds. Found only, never crafted.", DURATION);
	public static final RuneDef DIVING_BELL = effect("diving_bell", "Diving Bell", 3, 12, "frost", EffectKind.WORLD, "Under water: a 1-by-2 pocket of air at the point, walled in glass, for 15 seconds.", DURATION);
	public static final RuneDef TIDE_LANTERN = effect("tide_lantern", "Tide Lantern", 1, 2, "arcane", EffectKind.WORLD, "A light at the point for 60 seconds that shines under water too.", DURATION);
	public static final RuneDef SLUICE = effect("sluice", "Sluice", 1, 3, "frost", EffectKind.WORLD, "Puts out fire within 4 blocks of the point, up to 24 blocks, campfires too, and anything burning there.");
	public static final RuneDef SOAK_THROUGH = effect("soak_through", "Soak Through", 1, 3, "frost", EffectKind.WORLD, "Concrete powder within 2 blocks of the point sets and dirt turns to mud, up to 12 blocks.");
	public static final RuneDef RAIN_CLOUD = effect("rain_cloud", "Raincloud", 2, 8, "storm", EffectKind.WORLD, "A small cloud rains on the point: farmland within 4 blocks is soaked, up to 8 crops grow, fires go out and a cauldron gains water.");
	public static final RuneDef STORM_GLASS = effect("storm_glass", "Storm Glass", 1, 1, "storm", EffectKind.WORLD, "Tells how long until the weather turns.");
	public static final RuneDef KELPSONG = effect("kelpsong", "Kelpsong", 1, 3, "frost", EffectKind.WORLD, "Kelp, seagrass and sea pickles within 4 blocks of the point grow as if bone mealed, up to 8.");
	public static final RuneDef CORAL_MEND = effect("coral_mend", "Coral Mend", 2, 6, "frost", EffectKind.WORLD, "Dead coral touching water within 3 blocks of the point comes back to life, up to 8.");
	public static final RuneDef NEST_TEND = effect("nest_tend", "Nest Tend", 1, 3, "earth", EffectKind.WORLD, "Turtle eggs within 4 blocks move a stage closer to hatching, up to 4, and baby turtles grow.");
	public static final RuneDef LILY_PATH = effect("lily_path", "Lily Path", 1, 3, "frost", EffectKind.WORLD, "Lays up to 10 lily pads across the water the way you look, for 30 seconds.", DURATION);
	public static final RuneDef SANDBAR = effect("sandbar", "Sandbar", 2, 7, "earth", EffectKind.WORLD, "Raises a 12-block sandstone path at the water's surface the way you look, for 20 seconds.", DURATION);
	public static final RuneDef ICE_AUGER = effect("ice_auger", "Ice Auger", 1, 2, "frost", EffectKind.WORLD, "Bores through up to 3 blocks of ice at the point, leaving water to fish in.");
	public static final RuneDef TIDE_MARKER = effect("tide_marker", "Tide Marker", 1, 2, "arcane", EffectKind.WORLD, "Sets a glowing buoy at the point for 5 minutes and tells you where it is. One per caster.");
	public static final RuneDef SHORE_SENSE = effect("shore_sense", "Shore Sense", 1, 2, "arcane", EffectKind.WORLD, "Points to the nearest dry land within 48 blocks.");
	public static final RuneDef FATHOM = effect("fathom", "Fathom", 1, 1, "arcane", EffectKind.WORLD, "Tells how deep the water is at the point and marks the bottom.");
	public static final RuneDef WRECK_SENSE = effect("wreck_sense", "Wreck Sense", 2, 6, "arcane", EffectKind.WORLD, "Chests and barrels under water and suspicious sand and gravel within 20 blocks shine for 15 seconds, up to 12.", DURATION);
	public static final RuneDef DRIFT_NET = effect("drift_net", "Drift Net", 2, 5, "frost", EffectKind.WORLD, "Up to 16 items floating in water within 6 blocks of the point drift to you. Others' drops stay put.");
	public static final RuneDef MOORING_CALL = effect("mooring_call", "Mooring Call", 2, 5, "frost", EffectKind.WORLD, "The nearest empty boat within 24 blocks comes to your side.");
	public static final RuneDef FAIR_WIND = effect("fair_wind", "Fair Wind", 2, 6, "wind", EffectKind.MOVEMENT, "In a boat: sail the way you look for 10 seconds.", DURATION);
	public static final RuneDef UPWELL = effect("upwell", "Upwell", 1, 3, "frost", EffectKind.MOVEMENT, "In water: rush up toward the surface.");
	public static final RuneDef SOUNDING = effect("sounding", "Sounding", 1, 3, "frost", EffectKind.MOVEMENT, "In water: dive fast for 2 seconds.");
	public static final RuneDef PORPOISE = effect("porpoise", "Porpoise Leap", 2, 5, "frost", EffectKind.MOVEMENT, "In water: leap forward like a dolphin, with no fall damage from the leap.");
	public static final RuneDef SKIMSTEP = effect("skimstep", "Skimstep", 2, 8, "frost", EffectKind.HELPFUL, "Run across water for 15 seconds. Crouch to sink.", DURATION);
	public static final RuneDef SKATERS_EDGE = effect("skaters_edge", "Skater's Edge", 1, 4, "frost", EffectKind.HELPFUL, "Speed for 30 seconds, Speed II when standing on ice.", DURATION);
	public static final RuneDef AIR_POCKET = effect("air_pocket", "Air Pocket", 1, 2, "frost", EffectKind.HELPFUL, "Refills your air.");
	public static final RuneDef DROWN_WARD = effect("drown_ward", "Drown Ward", 2, 6, "frost", EffectKind.HELPFUL, "For 60 seconds your air refills when it runs low, up to 3 times.", DURATION);
	public static final RuneDef PEARL_SIGHT = effect("pearl_sight", "Pearl Sight", 2, 6, "arcane", EffectKind.HELPFUL, "Conduit Power for 30 seconds.", DURATION);
	public static final RuneDef SEA_BREEZE = effect("sea_breeze", "Sea Breeze", 2, 6, "wind", EffectKind.HELPFUL, "Clears Mining Fatigue, Nausea and Hunger.");
	public static final RuneDef INKVEIL = effect("inkveil", "Inkveil", 2, 7, "frost", EffectKind.HELPFUL, "Invisible for 10 seconds in water, 3 on land.", DURATION);
	public static final RuneDef SHELLBACK = effect("shellback", "Shellback", 2, 6, "earth", EffectKind.HELPFUL, "Resistance for 15 seconds, 30 in water or rain.", DURATION);
	public static final RuneDef DEWCATCH = effect("dewcatch", "Dewcatch", 1, 2, "frost", EffectKind.WORLD, "Up to 4 glass bottles in your pack fill with water.");
	public static final RuneDef DIVERS_HANDS = effect("divers_hands", "Diver's Hands", 1, 4, "frost", EffectKind.HELPFUL, "Mine at full speed under water for 60 seconds.", DURATION);

	// ---- fx-farm pack: farming, husbandry, kitchen, hive and forestry runes (behaviour in cast/packs/FarmEffects, rules in FarmRules).
	public static final RuneDef TILLAGE = effect("tillage", "Tillage", 1, 3, "earth", EffectKind.WORLD, "Tills the dirt and grass in a 5-by-5 patch around the block it strikes into farmland, as a hoe would. Widen reaches further; never more than 32 blocks a cast.", RADIUS);
	public static final RuneDef DEWFALL = effect("dewfall", "Dewfall", 1, 3, "storm", EffectKind.WORLD, "A soft dew wets every farmland block in a 5-by-5 patch around the point to full moisture.", RADIUS);
	public static final RuneDef TILTH = effect("tilth", "Tilth", 1, 3, "earth", EffectKind.WORLD, "Loosens the coarse and rooted dirt in a 5-by-5 patch into plain dirt; rooted dirt sheds its hanging roots, as a hoe would.", RADIUS);
	public static final RuneDef PLOWLINE = effect("plowline", "Plowline", 2, 6, "earth", EffectKind.WORLD, "Ploughs a straight furrow from the block it strikes along the way you face: up to 8 blocks of dirt or grass tilled into farmland, longer with power (16 at most).", POWER);
	public static final RuneDef SOW = effect("sow", "Sow", 1, 4, "earth", EffectKind.WORLD, "Plants seeds from your own inventory into the empty farmland of a 5-by-5 patch, one seed for each block. Nothing is planted that you don't carry.", RADIUS);
	public static final RuneDef RIPEN = effect("ripen", "Ripen", 2, 7, "time", EffectKind.WORLD, "Hurries each crop, stem, berry bush and nether wart in a 5-by-5 patch one stage on (two at 1.5 power, three at most). Ripe ones are left as they are.", POWER, RADIUS);
	public static final RuneDef DEWKEEP = effect("dewkeep", "Dewkeep", 2, 7, "storm", EffectKind.WORLD, "Hangs a mist over a 5-by-5 patch for 60 seconds: its farmland is wetted now and again every 5 seconds, so a field far from water never dries. Lovely from a Totem or a Zone.", DURATION, RADIUS, LINGER);
	public static final RuneDef FIELDSENSE = effect("fieldsense", "Field Sense", 1, 2, "arcane", EffectKind.WORLD, "Reads the field within 6 blocks: every ripe crop glints, and you're told how many are ripe and how many are still growing.", RADIUS);
	public static final RuneDef THAWFIELD = effect("thawfield", "Thawfield", 1, 3, "fire", EffectKind.WORLD, "A warm breath melts the snow layers in a 5-by-5 patch away from crops and soil. Snow blocks and ice are left alone.", RADIUS);
	public static final RuneDef CLOCHE = effect("cloche", "Cloche", 4, 22, "time", EffectKind.WORLD, "Sets a glass of quickened time over a 5-by-5 patch for 30 seconds: once a second, every plant under it gets the growing tick the world gives now and then. At most 25 plants a second.", DURATION, RADIUS);
	public static final RuneDef SCARECROW = effect("scarecrow", "Scarecrow", 3, 12, "wind", EffectKind.WORLD, "Stands a ward of rustling wind on the spot for 20 seconds: rabbits, foxes and monsters within 5 blocks are shooed out of it once a second. Pets and farm animals stay put.", DURATION, RADIUS);
	public static final RuneDef FALLOW = effect("fallow", "Fallow", 1, 2, "earth", EffectKind.WORLD, "Rests the bare farmland in a 5-by-5 patch back into dirt. Farmland with something growing on it is left as it is.", RADIUS);
	public static final RuneDef DITCHWATER = effect("ditchwater", "Ditchwater", 2, 8, "storm", EffectKind.WORLD, "Fills the hole you strike with one source of water, if it is an open hole with farmland within 4 blocks. Never in the Nether's heat.");
	public static final RuneDef COMPOST = effect("compost", "Compost", 1, 3, "earth", EffectKind.WORLD, "Feeds the composter you strike from your inventory, as if you dropped the items in yourself: up to 8 seeds, leaves or scraps (16 at double power).", POWER);
	public static final RuneDef STALKRISE = effect("stalkrise", "Stalkrise", 2, 6, "earth", EffectKind.WORLD, "Sugar cane and cactus in a 5-by-5 patch grow one block taller, never past three tall; bamboo grows as bone meal would grow it.", RADIUS);
	public static final RuneDef GOURDCALL = effect("gourdcall", "Gourdcall", 2, 8, "earth", EffectKind.WORLD, "Grown pumpkin and melon stems in a 5-by-5 patch try at once to set their fruit, as many times as a day of waiting would give them.", RADIUS);
	public static final RuneDef BERRYBLESS = effect("berrybless", "Berrybless", 1, 4, "earth", EffectKind.WORLD, "Sweet berry bushes in a 5-by-5 patch ripen to full, and the glow berry vines among them come into fruit.", RADIUS);
	public static final RuneDef COURTSHIP = effect("courtship", "Courtship", 3, 12, "arcane", EffectKind.WORLD, "Up to 6 grown farm animals within 5 blocks fall in love at once, as if you'd fed them. Animals that aren't ready yet (or belong to someone else) are left alone.", RADIUS);
	public static final RuneDef HERDCALL = effect("herdcall", "Herdcall", 1, 4, "wind", EffectKind.WORLD, "Farm animals within 8 blocks walk to the spot it lands (to you, cast on yourself). Up to 12 come at once.", RADIUS);
	public static final RuneDef FLEECE = effect("fleece", "Fleece", 1, 3, "wind", EffectKind.WORLD, "Shears every woolly sheep within 5 blocks, as shears would, and the wool falls at their feet.", RADIUS);
	public static final RuneDef MILKMAID = effect("milkmaid", "Milkmaid", 1, 3, "frost", EffectKind.WORLD, "Fills the empty buckets in your inventory with milk, one for each grown cow or goat within 5 blocks.", RADIUS);
	public static final RuneDef HENHOUSE = effect("henhouse", "Henhouse", 2, 6, "time", EffectKind.WORLD, "Each chicken within 5 blocks lays its next egg now. A hen hurried along can't be hurried again for 2 minutes.", RADIUS);
	public static final RuneDef GENTLEHAND = effect("gentlehand", "Gentle Hand", 1, 3, "wind", EffectKind.WORLD, "Farm animals within 5 blocks stop panicking and stand calm for 8 seconds, easy to lead or pen.", DURATION, RADIUS);
	public static final RuneDef FODDER = effect("fodder", "Fodder", 1, 4, "earth", EffectKind.WORLD, "Feeds the animals within 5 blocks from your inventory, each with one item of the food it eats: it heals 4 and a young one grows a tenth of the way up.", RADIUS);
	public static final RuneDef BARNWARMTH = effect("barnwarmth", "Barnwarmth", 1, 3, "fire", EffectKind.WORLD, "A hearth's warmth heals the farm animals and your pets within 5 blocks by 4 (more with power).", POWER, RADIUS);
	public static final RuneDef HERDSENSE = effect("herdsense", "Herdsense", 1, 2, "arcane", EffectKind.WORLD, "Farm animals within 12 blocks glow for 10 seconds, and you're told how many there are and how many are ready to breed.", DURATION, RADIUS);
	public static final RuneDef HEARTHCOOK = effect("hearthcook", "Hearthcook", 3, 12, "fire", EffectKind.WORLD, "Cooks the raw food in your own inventory as a smoker would: up to 8 pieces (16 at double power). Only food; the rest of your pack is left alone.", POWER);
	public static final RuneDef STEWPOT = effect("stewpot", "Stewpot", 1, 3, "fire", EffectKind.WORLD, "Cooks bowls from your inventory into stew, as a crafting table would: a red and a brown mushroom make mushroom stew, six beetroots make beetroot soup. Up to 3 bowls.");
	public static final RuneDef BAKEHOUSE = effect("bakehouse", "Bakehouse", 1, 3, "fire", EffectKind.WORLD, "Bakes from your inventory: three wheat into bread, then a pumpkin, sugar and an egg into a pie. Up to 4 bakes (8 at double power).", POWER);
	public static final RuneDef POLLINATE = effect("pollinate", "Pollinate", 2, 8, "wind", EffectKind.WORLD, "The bees within 8 blocks dust the crops around the point: one crop in a 5-by-5 patch grows a stage for each bee, 2 per bee at double power. No bees, no help.", POWER, RADIUS);
	public static final RuneDef HIVEHUM = effect("hivehum", "Hive Hum", 3, 12, "time", EffectKind.WORLD, "Each beehive and bee nest within 5 blocks fills by one level of honey. A hive hummed into can't be hurried again for a minute.", RADIUS);
	public static final RuneDef CALMSMOKE = effect("calmsmoke", "Calm Smoke", 1, 3, "fire", EffectKind.WORLD, "A puff of campfire smoke settles the angry bees within 6 blocks: they forget their quarrel and go back to work.", RADIUS);
	public static final RuneDef WILDFLOWER = effect("wildflower", "Wildflower", 1, 3, "earth", EffectKind.WORLD, "Sows wild flowers onto the open grass in a 5-by-5 patch: up to 6 flowers, more when widened.", RADIUS);
	public static final RuneDef SAPLINGRISE = effect("saplingrise", "Sapling Rise", 3, 12, "earth", EffectKind.WORLD, "Saplings and mushrooms in a 5-by-5 patch are urged to grow: up to 3 of them each get the growth of 8 bone meal.", RADIUS);
	public static final RuneDef SAPLINGSOW = effect("saplingsow", "Sapling Sow", 1, 3, "earth", EffectKind.WORLD, "Plants saplings from your own inventory onto open dirt and grass in a 5-by-5 patch, two blocks apart: up to 4.", RADIUS);
	public static final RuneDef LEAFFALL = effect("leaffall", "Leaffall", 1, 3, "wind", EffectKind.WORLD, "The wild leaves in a 5-by-5-by-5 block around the point drop now, as if they'd decayed, with what decay would drop. Leaves placed by hand stay.", RADIUS);
	public static final RuneDef BARKSTRIP = effect("barkstrip", "Barkstrip", 1, 2, "earth", EffectKind.WORLD, "Strips the bark off the logs and wood in a 5-by-5-by-5 block around the point, as an axe would.", RADIUS);
	public static final RuneDef COPPICE = effect("coppice", "Coppice", 3, 14, "earth", EffectKind.WORLD, "Fells the tree you strike (its logs, never more than 32, drop as an axe would cut them) and replants the stump with a sapling from your inventory. A log with no living leaves is part of a build and is left alone.");
	public static final RuneDef FEASTDAY = effect("feastday", "Feast Day", 4, 20, "fire", EffectKind.HELPFUL, "A harvest feast for everyone it reaches: 6 hunger and 6 seconds of Saturation each, and Regeneration I for 10 seconds.", DURATION);
	public static final RuneDef PICNIC = effect("picnic", "Picnic", 1, 5, "earth", EffectKind.HELPFUL, "A shared basket: each target eats 3 hunger and heals 2 (more with power).", POWER);
	public static final RuneDef HONEYDEW = effect("honeydew", "Honeydew", 1, 4, "wind", EffectKind.HELPFUL, "Sweet as a honey bottle: cures Poison and heals 3 (more with power).", POWER);
	public static final RuneDef LEAFSHADE = effect("leafshade", "Leafshade", 2, 7, "earth", EffectKind.HELPFUL, "Cool leaf-shade: Fire Resistance for 15 seconds.", DURATION);
	public static final RuneDef BARKHIDE = effect("barkhide", "Barkhide", 2, 8, "earth", EffectKind.HELPFUL, "Skin like old oak: Resistance I for 12 seconds.", DURATION);
	public static final RuneDef SAPFLOW = effect("sapflow", "Sapflow", 2, 7, "earth", EffectKind.HELPFUL, "Spring sap rises in you: Regeneration I for 8 seconds.", DURATION);
	public static final RuneDef TROT = effect("trot", "Trot", 2, 6, "wind", EffectKind.MOVEMENT, "Your mount (or the horse you strike) gets Speed II and Jump Boost I for 20 seconds. Cast on yourself while riding.", DURATION);
	public static final RuneDef BEELINE = effect("beeline", "Beeline", 2, 6, "wind", EffectKind.MOVEMENT, "You zip straight ahead like a bee to a flower, about 6 blocks, and land without a fall.", POWER);
	public static final RuneDef FIELDSTRIDE = effect("fieldstride", "Fieldstride", 1, 4, "wind", EffectKind.MOVEMENT, "A farmhand's long stride: Speed I and Jump Boost I for 20 seconds.", DURATION);
	public static final RuneDef HAYLOFT = effect("hayloft", "Hayloft", 1, 5, "wind", EffectKind.MOVEMENT, "Tosses you up as if from a haystack, about 4 blocks, and you float down for 3 seconds without a fall.", POWER);
	// ---- fx-mine pack: the delving runes (mining, masonry, light, light redstone, hauling); see cast/DelveEffects.
	public static final RuneDef STAIRDELVE = effect("stairdelve", "Stair Delve", 2, 8, "earth", EffectKind.WORLD, "Carves a stair 5 steps down the way you face (iron-pickaxe hardness; Amplify for diamond). Stops short of lava, water or a drop.", POWER);
	public static final RuneDef RISER = effect("riser", "Riser", 2, 8, "earth", EffectKind.WORLD, "Carves a stair 5 steps up the way you face (iron-pickaxe hardness; Amplify for diamond). Stops short of lava or water.", POWER);
	public static final RuneDef PLUMBLINE = effect("plumbline", "Plumb Line", 1, 4, "earth", EffectKind.WORLD, "Digs a shaft up to 8 deep under the point (iron-pickaxe hardness). Stops a block above lava, water or a drop.", POWER);
	public static final RuneDef SIFTFALL = effect("siftfall", "Siftfall", 1, 3, "earth", EffectKind.WORLD, "Sand and gravel hanging over air above the point fall as items, up to 4 high.", RADIUS);
	public static final RuneDef GANGUE = effect("gangue", "Gangue", 3, 14, "earth", EffectKind.WORLD, "Mines plain rock within 2 blocks of the point and leaves the ores standing.", POWER, RADIUS);
	public static final RuneDef OREPLUCK = effect("orepluck", "Ore Pluck", 3, 12, "earth", EffectKind.WORLD, "Mines up to 8 ores with an open face within 4 blocks of the point.", POWER, RADIUS);
	public static final RuneDef LUCKSTRIKE = effect("luckstrike", "Luckstrike", 3, 10, "earth", EffectKind.WORLD, "Mines the ore hit with Fortune I (II with Amplify).", POWER);
	public static final RuneDef SILKLIFT = effect("silklift", "Silklift", 2, 6, "arcane", EffectKind.WORLD, "Mines the block hit whole, as Silk Touch would. Never a chest or anything with contents.");
	public static final RuneDef DEEPSOUND = effect("deepsound", "Deepsound", 1, 2, "earth", EffectKind.WORLD, "Tells you the nearest ore up to 16 blocks under the point, and how deep.");
	public static final RuneDef ORETALLY = effect("oretally", "Ore Tally", 2, 5, "earth", EffectKind.WORLD, "Counts the ores within 6 blocks of the point by kind.", RADIUS);
	public static final RuneDef LAVASEAL = effect("lavaseal", "Lava Seal", 2, 7, "fire", EffectKind.WORLD, "Lava within 3 blocks of the point sets: still lava to obsidian, flowing lava to cobblestone. 16 at most.", RADIUS);
	public static final RuneDef DEEPWAY = effect("deepway", "Deepway", 3, 16, "earth", EffectKind.WORLD, "Bores a 3x3 road 3 deep into the wall hit (diamond-pickaxe hardness) and sets 2 torches from your pack.");
	public static final RuneDef HOLLOWSENSE = effect("hollowsense", "Hollow Sense", 1, 3, "wind", EffectKind.WORLD, "Points you to the nearest open cave within 12 blocks of the point.", RADIUS);
	public static final RuneDef KILNBAKE = effect("kilnbake", "Kiln Bake", 2, 8, "fire", EffectKind.WORLD, "Blocks near the point bake in place when a furnace makes a block of them, like sand to glass. 9 at most.", RADIUS);
	public static final RuneDef BLOCKPACK = effect("blockpack", "Block Pack", 1, 3, "earth", EffectKind.WORLD, "Crafts nine of a kind in your pack into their block, like iron ingots into an iron block. 8 blocks at most.");
	public static final RuneDef UNPACK = effect("unpack", "Unpack", 1, 3, "earth", EffectKind.WORLD, "Turns up to 4 storage blocks in your pack back into nine pieces each.");
	public static final RuneDef MILLSTONE = effect("millstone", "Millstone", 1, 3, "earth", EffectKind.WORLD, "Grinds up to 16 cobblestone in your pack into gravel, or else gravel into sand.");
	public static final RuneDef TOOLMEND = effect("toolmend", "Tool Mend", 2, 8, "arcane", EffectKind.HELPFUL, "Mends the tool in the target's hand with its repair material from your pack: a quarter for each, 4 at most.");
	public static final RuneDef LEVELGROUND = effect("levelground", "Levelground", 2, 10, "earth", EffectKind.WORLD, "Mines away what stands up to 3 blocks over the point within 2 blocks of it.", POWER, RADIUS);
	public static final RuneDef HOLEFILL = effect("holefill", "Holefill", 1, 4, "earth", EffectKind.WORLD, "Fills holes near the point up to its height with stone, dirt or planks from your pack. 16 at most.", RADIUS);
	public static final RuneDef SHOREUP = effect("shoreup", "Shore Up", 1, 3, "earth", EffectKind.WORLD, "Props up hanging sand and gravel near the point with blocks from your pack. 8 at most.", RADIUS);
	public static final RuneDef STILT = effect("stilt", "Stilt", 1, 4, "earth", EffectKind.WORLD, "Raises you on a packed mud column up to 5 high. It crumbles after 30 seconds.", DURATION);
	public static final RuneDef PLANKWAY = effect("plankway", "Plankway", 1, 4, "earth", EffectKind.WORLD, "Lays a lasting bridge up to 12 long the way you face, from planks or stone in your pack.");
	public static final RuneDef POLISH = effect("polish", "Polish", 1, 3, "earth", EffectKind.WORLD, "Polishes stone near the point: andesite, granite, diorite, tuff, basalt and the like. 9 at most.", RADIUS);
	public static final RuneDef BRICKWORK = effect("brickwork", "Brickwork", 2, 5, "earth", EffectKind.WORLD, "Cuts stone near the point into its bricks. 9 at most.", RADIUS);
	public static final RuneDef AGESTONE = effect("agestone", "Agestone", 2, 5, "earth", EffectKind.WORLD, "Ages masonry near the point: moss on cobblestone and stone bricks, cracks in deep bricks. 9 at most.", RADIUS);
	public static final RuneDef CONCRETESET = effect("concreteset", "Concrete Set", 1, 3, "earth", EffectKind.WORLD, "Concrete powder near the point sets into concrete. 16 at most.", RADIUS);
	public static final RuneDef CHALKLINE = effect("chalkline", "Chalk Line", 1, 2, "arcane", EffectKind.WORLD, "Draws a line from you to the point for 20 seconds and tells you its length and rise.", DURATION);
	public static final RuneDef PITFLOOR = effect("pitfloor", "Pit Floor", 2, 6, "earth", EffectKind.WORLD, "Lays a 3x3 packed mud floor over the air at the point. It crumbles after 20 seconds.", DURATION);
	public static final RuneDef TORCHFALL = effect("torchfall", "Torchfall", 2, 6, "fire", EffectKind.WORLD, "Sets up to 6 torches from your pack on the darkest floor near the point, 5 blocks apart.", RADIUS);
	public static final RuneDef GLOOMSIGHT = effect("gloomsight", "Gloomsight", 1, 3, "arcane", EffectKind.WORLD, "Marks the pitch-dark floor near the point, where monsters spawn, for 10 seconds.", RADIUS, DURATION);
	public static final RuneDef LUMENPATH = effect("lumenpath", "Lumen Path", 2, 6, "arcane", EffectKind.WORLD, "Hangs a light every 4 blocks from you to the point, 6 at most, for 120 seconds.", DURATION);
	public static final RuneDef SNUFFOUT = effect("snuffout", "Snuff Out", 1, 3, "frost", EffectKind.WORLD, "Puts out fire, campfires and candles within 4 blocks of the point.", RADIUS);
	public static final RuneDef HEADLAMP = effect("headlamp", "Headlamp", 2, 6, "arcane", EffectKind.HELPFUL, "A light follows the target for 60 seconds.", DURATION);
	public static final RuneDef LEVERFLIP = effect("leverflip", "Lever Flip", 1, 2, "storm", EffectKind.WORLD, "Flips up to 4 levers near the point.", RADIUS);
	public static final RuneDef BUTTONPUSH = effect("buttonpush", "Button Push", 1, 2, "storm", EffectKind.WORLD, "Presses up to 4 buttons near the point.", RADIUS);
	public static final RuneDef DOORCALL = effect("doorcall", "Doorcall", 1, 2, "storm", EffectKind.WORLD, "Opens or shuts up to 4 doors, trapdoors and gates near the point. Iron ones stay put.", RADIUS);
	public static final RuneDef CHESTSORT = effect("chestsort", "Chest Sort", 2, 5, "void", EffectKind.WORLD, "Joins like items in the chest, barrel or shulker box hit and lays them out in order.");
	public static final RuneDef STOW = effect("stow", "Stow", 2, 5, "void", EffectKind.WORLD, "Moves pack items the chest hit already holds into it. Your hotbar stays.");
	public static final RuneDef RESTOCK = effect("restock", "Restock", 2, 5, "void", EffectKind.WORLD, "Tops up your hotbar stacks from the chest hit.");
	public static final RuneDef STOCKTAKE = effect("stocktake", "Stocktake", 1, 3, "arcane", EffectKind.WORLD, "Tells you the biggest stocks across the chests within 4 blocks of the point.", RADIUS);
	public static final RuneDef UNBURDEN = effect("unburden", "Unburden", 2, 6, "void", EffectKind.WORLD, "Moves your whole pack into the chest hit, as far as it has room. Your hotbar stays.");
	public static final RuneDef LODEPULL = effect("lodepull", "Lodepull", 2, 7, "void", EffectKind.HELPFUL, "Loose drops within 6 blocks drift to the target for 30 seconds. Never someone else's drops.", RADIUS, DURATION);
	public static final RuneDef CAVEWARD = effect("caveward", "Caveward", 3, 10, "earth", EffectKind.HELPFUL, "For 60 seconds, sand, gravel or powder snow that buries the target's head crumbles into items.", DURATION);
	public static final RuneDef DELVEMARK = effect("delvemark", "Delvemark", 4, 22, "void", EffectKind.WORLD, "Marks your spot for 10 minutes. Cast again within 128 blocks and hold still 2 seconds to go back to it.");
	public static final RuneDef MOTHERLODE = effect("motherlode", "Motherlode", 4, 26, "earth", EffectKind.WORLD, "Mines up to 16 ores within 3 blocks of the point with Fortune II (III with Amplify).", POWER);
	public static final RuneDef FLOORLAY = effect("floorlay", "Floorlay", 2, 7, "earth", EffectKind.WORLD, "Lays a 5x5 floor at the point from stone, dirt or planks in your pack, over air only.");
	public static final RuneDef PACKTIDY = effect("packtidy", "Pack Tidy", 1, 2, "void", EffectKind.WORLD, "Joins like stacks in your pack. Your hotbar is left alone.");

	// ---- links-mods pack (the hearth pack: everyday links and modifiers; rules in HearthLinkRules, behaviour in cast.HearthLinks / cast.HearthModifiers)
	public static final RuneDef TIDY = modifier("tidy", "Tidy", 1, 1.1, HearthLinkRules.WORLD, "Blocks it breaks drop straight into your pack; what doesn't fit lands at your feet.");
	public static final RuneDef REPLANTING = modifier("replanting", "Replanting", 1, 1.1, HearthLinkRules.WORLD, "Ripe crops it breaks are replanted from their own seeds.");
	public static final RuneDef KILNED = modifier("kilned", "Kilned", 2, 1.25, HearthLinkRules.WORLD, "What it breaks drops already smelted, with no fuel. Only one drop rule per effect.");
	public static final RuneDef SILKEN = modifier("silken", "Silken", 2, 1.3, HearthLinkRules.WORLD, "Blocks it breaks drop themselves, as with Silk Touch. Only one drop rule per effect.");
	public static final RuneDef WINDFALL = modifier("windfall", "Windfall", 3, 1.4, HearthLinkRules.WORLD, "Blocks it breaks drop as if mined with Fortune III. Only one drop rule per effect.");
	public static final RuneDef VEINFOLLOW = modifier("veinfollow", "Veinfollow", 2, 1.3, HearthLinkRules.WORLD, "Breaking an ore also breaks up to 8 more touching blocks of the same ore.");
	public static final RuneDef TIMBERING = modifier("timbering", "Timbering", 2, 1.3, HearthLinkRules.WORLD, "Breaking a log fells up to 24 more logs of the tree above it.");
	public static final RuneDef LEVEL_GROUND = modifier("level_ground", "Level Ground", 1, 1.0, HearthLinkRules.WORLD, "Never breaks a block below your feet, so you can't dig yourself a pit.");
	public static final RuneDef STEADY = modifier("steady", "Steady", 1, 0.9, FRUGAL, "The effect can't break any block, and costs 10% less. Not with block modifiers.");
	public static final RuneDef DAMP = modifier("damp", "Damp", 1, 1.0, HearthLinkRules.BURNS, "Fire it starts on blocks goes straight out; creatures still burn.");
	public static final RuneDef MAGNETIC = modifier("magnetic", "Magnetic", 1, 1.1, FRUGAL, "Items within 6 blocks of where it lands fly to you.");
	public static final RuneDef SOWING = modifier("sowing", "Sowing", 1, 1.1, FRUGAL, "Plants seeds from your pack on bare farmland within 3 blocks of where it lands.");
	public static final RuneDef FURROWING = modifier("furrowing", "Furrowing", 1, 1.1, FRUGAL, "Tills grass and dirt within 2 blocks of where it lands into farmland.");
	public static final RuneDef FERTILE = modifier("fertile", "Fertile", 2, 1.2, FRUGAL, "Crops and saplings within 3 blocks of where it lands grow a stage.");
	public static final RuneDef TORCHSET = modifier("torchset", "Torchset", 1, 1.0, FRUGAL, "If it lands somewhere dark, sets a torch there from your pack.");
	public static final RuneDef ORE_SENSING = modifier("ore_sensing", "Ore Sensing", 2, 1.2, FRUGAL, "Ores within 8 blocks of where it lands glimmer for 10 seconds.");
	public static final RuneDef FETCHING = modifier("fetching", "Fetching", 1, 1.1, HearthLinkRules.HARMS, "The loot of creatures it kills lands at your feet.");
	public static final RuneDef BOUNTIFUL = modifier("bountiful", "Bountiful", 2, 1.2, HearthLinkRules.HARMS, "Creatures it kills drop twice the experience, even untouched by your hand.");
	public static final RuneDef CULLING = modifier("culling", "Culling", 2, 1.1, HearthLinkRules.HARMS, "Lands only on monsters, 20% stronger; everything else is passed over.");
	public static final RuneDef HEADHUNTING = modifier("headhunting", "Headhunting", 3, 1.2, HearthLinkRules.HARMS, "Lands only on the healthiest creature it reaches, 60% stronger.");
	public static final RuneDef HALLOWED = modifier("hallowed", "Hallowed", 2, 1.2, HearthLinkRules.HARMS, "Twice as strong on the undead; the living are passed over.");
	public static final RuneDef TAPERING = modifier("tapering", "Tapering", 2, 1.1, HearthLinkRules.HARMS, "The first creature takes 50% more; each after takes a quarter less than the one before.");
	public static final RuneDef POOLED = modifier("pooled", "Pooled", 2, 1.1, HearthLinkRules.HARMS, "Double power, shared evenly between every creature it reaches.");
	public static final RuneDef SUNLIT = modifier("sunlit", "Sunlit", 2, 1.0, FRUGAL, "Twice as strong under open daylight sky; half as strong anywhere else.");
	public static final RuneDef GENTLE = modifier("gentle", "Gentle", 1, 1.0, HearthLinkRules.HARMS, "Passes over farm animals, pets and villagers.");
	public static final RuneDef SPARING = modifier("sparing", "Sparing", 1, 1.0, HearthLinkRules.HARMS, "Passes over other players and their pets.");
	public static final RuneDef SOOTHING = modifier("soothing", "Soothing", 1, 1.0, HearthLinkRules.HARMS, "Creatures it lands on forget their anger at you; 30% weaker.");
	public static final RuneDef CUSHIONED = modifier("cushioned", "Cushioned", 1, 1.1, HearthLinkRules.MOVES, "Whoever it moves takes no fall damage on their next landing (10 seconds).");
	public static final RuneDef MENDING_MOD = modifier("mending", "Mending", 2, 1.2, HearthLinkRules.HELPS, "Also mends 10 durability on each target's held item and armour.");
	public static final RuneDef NOURISHING = modifier("nourishing", "Nourishing", 1, 1.1, HearthLinkRules.HELPS, "Also feeds each player it lands on 3 hunger.");
	public static final RuneDef PURIFYING = modifier("purifying", "Purifying", 2, 1.2, HearthLinkRules.HELPS, "Also lifts one harmful effect from each target.");
	public static final RuneDef MATCHMAKING = modifier("matchmaking", "Matchmaking", 1, 1.0, HearthLinkRules.HELPS, "Grown animals it lands on fall in love.");
	public static final RuneDef FLEECING = modifier("fleecing", "Fleecing", 1, 1.0, FRUGAL, "Sheep it reaches are shorn; the wool lands at your feet.");
	public static final RuneDef INWARD = modifier("inward", "Inward", 2, 1.0, HearthLinkRules.HELPS, "Lands on you alone, 40% stronger. Not with Selfless.");
	public static final RuneDef SELFLESS = modifier("selfless", "Selfless", 2, 1.0, HearthLinkRules.HELPS, "Skips you and lands on the others 30% stronger. Not with Inward.");
	public static final RuneDef TRIAGE = modifier("triage", "Triage", 2, 1.1, HearthLinkRules.HELPS, "Lands only on the most hurt creature it reaches, 60% stronger.");
	public static final RuneDef IF_NIGHT = link("if_night", "If Night", 1, 1, "The rest fires only at night.");
	public static final RuneDef IF_DAY = link("if_day", "If Day", 1, 1, "The rest fires only by day.");
	public static final RuneDef IF_RAINING = link("if_raining", "If Raining", 1, 1, "The rest fires only while rain or snow falls on you.");
	public static final RuneDef IF_UNDERGROUND = link("if_underground", "If Underground", 1, 1, "The rest fires only with no open sky above you.");
	public static final RuneDef IF_ALONE = link("if_alone", "If Alone", 2, 1, "The rest fires only with no other player or monster within 16 blocks.");
	public static final RuneDef IF_NEAR_ALLY = link("if_near_ally", "If Near Ally", 2, 1, "The rest fires only with a friendly player or your pet within 8 blocks.");
	public static final RuneDef IF_UNHURT = link("if_unhurt", "If Unhurt", 2, 1, "The rest fires only at full health.");
	public static final RuneDef IF_HOLDING_TOOL = link("if_holding_tool", "If Holding Tool", 1, 1, "The rest fires only with a pickaxe, axe, shovel, hoe or shears in hand.");
	public static final RuneDef IF_BRIMMING = link("if_brimming", "If Brimming", 2, 1, "The rest fires only with more than half your mana left.");
	public static final RuneDef IF_IN_FIELDS = link("if_in_fields", "If In Fields", 1, 1, "The rest fires only with farmland within 4 blocks of your feet.");
	public static final RuneDef ON_MINE = link("on_mine", "On Mine", 1, 2, "The rest waits up to 30 seconds for the next block you mine, then fires there.");
	public static final RuneDef ON_HARVEST = link("on_harvest", "On Harvest", 1, 2, "The rest waits up to a minute for the next ripe crop you pick, then fires there.");
	public static final RuneDef ON_CATCH = link("on_catch", "On Catch", 2, 2, "The rest waits up to 2 minutes for your next catch with a rod, then fires where you reeled it in.");
	public static final RuneDef ON_SPRINT = link("on_sprint", "On Sprint", 2, 2, "The rest waits up to 15 seconds for you to start sprinting, then fires from you.");
	public static final RuneDef ON_SPLASH = link("on_splash", "On Splash", 2, 2, "The rest waits up to 30 seconds for you to enter water, then fires there.");
	public static final RuneDef ON_MOUNT = link("on_mount", "On Mount", 2, 2, "The rest waits up to 30 seconds for you to ride something, then fires at your mount.");
	public static final RuneDef ON_WAKE = link("on_wake", "On Wake", 2, 2, "The rest waits up to 10 minutes for you to wake from a bed, then fires from you.");
	// ---- fx-explore pack
	public static final RuneDef LAND_READING = effect("land_reading", "Land Reading", 1, 1, "earth", EffectKind.WORLD, "Reads the ground where it lands: its biome, how warm it is, and its height against sea level.");
	public static final RuneDef DEPTH_SOUNDING = effect("depth_sounding", "Depth Sounding", 1, 2, "earth", EffectKind.WORLD, "Sounds the rock under the point: how far down the first open cave or lava lies, up to 64 blocks.");
	public static final RuneDef SPAWN_BEARING = effect("spawn_bearing", "Spawn Bearing", 1, 1, "wind", EffectKind.WORLD, "Tells which way the world spawn lies and roughly how far.");
	public static final RuneDef HOME_BEARING = effect("home_bearing", "Home Bearing", 1, 2, "wind", EffectKind.WORLD, "Tells which way your bed or respawn anchor lies and roughly how far, when it is in this world.");
	public static final RuneDef GRAVE_BEARING = effect("grave_bearing", "Grave Bearing", 2, 4, "void", EffectKind.WORLD, "Tells which way you last died and roughly how far, when it was in this world.");
	public static final RuneDef PORTAL_RECKONING = effect("portal_reckoning", "Portal Reckoning", 1, 1, "void", EffectKind.WORLD, "Tells where this spot lies in the other realm: one Nether block is eight in the Overworld.");
	public static final RuneDef SLIME_SENSE = effect("slime_sense", "Slime Sense", 1, 2, "life", EffectKind.WORLD, "Tells whether slimes can spawn underground in this chunk.");
	public static final RuneDef SKY_READING = effect("sky_reading", "Sky Reading", 1, 1, "storm", EffectKind.WORLD, "Reads the sky: the weather now and about how many minutes until it turns.");
	public static final RuneDef MOON_READING = effect("moon_reading", "Moon Reading", 1, 1, "time", EffectKind.WORLD, "Reads the moon tonight and how many nights until the next full moon.");
	public static final RuneDef SUN_READING = effect("sun_reading", "Sun Reading", 1, 1, "time", EffectKind.WORLD, "Tells the time of day and how many minutes until dusk or dawn.");
	public static final RuneDef LUX_READING = effect("lux_reading", "Lux Reading", 1, 1, "arcane", EffectKind.WORLD, "Reads the light where it lands, from blocks and from the sky, and whether monsters may spawn there.");
	public static final RuneDef CHALK_LINE = effect("chalk_line", "Dust Line", 1, 1, "arcane", EffectKind.WORLD, "Draws a dust line from you to the point for 10 seconds and tells its length in blocks.", DURATION);
	public static final RuneDef VILLAGE_SENSE = effect("village_sense", "Village Sense", 2, 6, "wind", EffectKind.WORLD, "Senses the nearest village and tells which way and roughly how far. Structure senses share a 30-second rest.", RADIUS);
	public static final RuneDef RUIN_SENSE = effect("ruin_sense", "Ruin Sense", 2, 6, "earth", EffectKind.WORLD, "Senses the nearest trail ruins and tells which way and roughly how far. Structure senses share a 30-second rest.", RADIUS);
	public static final RuneDef SHIPWRECK_SENSE = effect("shipwreck_sense", "Shipwreck Sense", 2, 6, "frost", EffectKind.WORLD, "Senses the nearest shipwreck and tells which way and roughly how far. Structure senses share a 30-second rest.", RADIUS);
	public static final RuneDef PORTAL_SENSE = effect("portal_sense", "Portal Sense", 2, 6, "fire", EffectKind.WORLD, "Senses the nearest ruined portal and tells which way and roughly how far. Structure senses share a 30-second rest.", RADIUS);
	public static final RuneDef FORTRESS_SENSE = effect("fortress_sense", "Fortress Sense", 3, 10, "fire", EffectKind.WORLD, "In the Nether, senses the nearest fortress and tells which way and roughly how far. Structure senses share a 30-second rest.", RADIUS);
	public static final RuneDef STRONGHOLD_COMPASS = effect("stronghold_compass", "Stronghold Compass", 3, 12, "void", EffectKind.WORLD, "In the Overworld, tells which way the nearest stronghold lies, but never how far. Structure senses share a 30-second rest.");
	public static final RuneDef SPIRE_SENSE = effect("spire_sense", "Spire Sense", 3, 10, "void", EffectKind.WORLD, "In the End, senses the nearest End city and tells which way and roughly how far. Structure senses share a 30-second rest.", RADIUS);
	public static final RuneDef TRAIL_BLAZE = effect("trail_blaze", "Trail Blaze", 1, 2, "fire", EffectKind.WORLD, "Sets a glowing end rod on the face it hits for 5 minutes, to mark the way back. It drops nothing.", DURATION);
	public static final RuneDef RELIC_SENSE = effect("relic_sense", "Relic Sense", 1, 3, "earth", EffectKind.WORLD, "Suspicious sand and gravel within 12 blocks glow for 20 seconds, and you hear how many there are.", RADIUS, DURATION);
	public static final RuneDef SPAWNER_SENSE = effect("spawner_sense", "Spawner Sense", 2, 5, "void", EffectKind.WORLD, "Counts the monster and trial spawners within 32 blocks and points to the nearest.", RADIUS);
	public static final RuneDef STEADY_BRUSH = effect("steady_brush", "Steady Brush", 2, 5, "earth", EffectKind.WORLD, "With a brush in hand, brushes up to 3 suspicious blocks near the point at once, hands free. The brush wears a point for each.", RADIUS);
	public static final RuneDef APPRAISE = effect("appraise", "Appraise", 1, 1, "arcane", EffectKind.WORLD, "Reads a villager: their trade, their level, and how many of their trades are sold out.");
	public static final RuneDef TRADE_RENEW = effect("trade_renew", "Trade Renew", 3, 10, "time", EffectKind.WORLD, "A villager with a trade restocks every offer. Each villager only once a day.");
	public static final RuneDef HAGGLE = effect("haggle", "Haggle", 3, 12, "arcane", EffectKind.HELPFUL, "For 20 seconds villagers give you their Hero of the Village prices.", DURATION);
	public static final RuneDef FOLK_CALL = effect("folk_call", "Folk Call", 1, 2, "wind", EffectKind.WORLD, "Villagers within 16 blocks walk over to the point.", RADIUS);
	public static final RuneDef FOLK_CENSUS = effect("folk_census", "Folk Census", 1, 1, "arcane", EffectKind.WORLD, "Counts the villagers within 32 blocks: with a trade, without one, nitwits, children, and golems.", RADIUS);
	public static final RuneDef LAPIS_THRIFT = effect("lapis_thrift", "Lapis Thrift", 2, 6, "arcane", EffectKind.WORLD, "For 60 seconds, your next enchantment at a table gives one lapis back.", DURATION);
	public static final RuneDef QUICKBREW = effect("quickbrew", "Quickbrew", 2, 6, "fire", EffectKind.WORLD, "Brewing stands within 6 blocks brew twice as fast for 20 seconds.", RADIUS, DURATION);
	public static final RuneDef POTION_STEEP = effect("potion_steep", "Potion Steep", 2, 6, "life", EffectKind.HELPFUL, "Good potion effects last a quarter longer, up to 45 seconds more and 8 minutes in all.");
	public static final RuneDef LORE_READING = effect("lore_reading", "Lore Reading", 1, 1, "arcane", EffectKind.WORLD, "Reads the item in your hand: how many enchantments it holds and what an anvil will charge to work it.");
	public static final RuneDef SHELF_COUNT = effect("shelf_count", "Shelf Count", 1, 1, "arcane", EffectKind.WORLD, "At an enchanting table: how many bookshelves feed it, out of 15, and how many are blocked.");
	public static final RuneDef BEACON_SWELL = effect("beacon_swell", "Beacon Swell", 3, 12, "arcane", EffectKind.WORLD, "Beacons within 16 blocks reach half again as far for 60 seconds.", DURATION);
	public static final RuneDef DYE_WASH = effect("dye_wash", "Dye Wash", 1, 3, "life", EffectKind.WORLD, "Recolours up to 16 wool, glass, terracotta, concrete or candle blocks around the point to the dye in your other hand. One dye per 8 blocks.", RADIUS);
	public static final RuneDef CHECKER_DYE = effect("checker_dye", "Checker Dye", 2, 5, "life", EffectKind.WORLD, "Like Dye Wash, but only every other block, for a checkered pattern. One dye per 8 blocks.", RADIUS);
	public static final RuneDef GLYPH_CARVE = effect("glyph_carve", "Glyph Carve", 1, 1, "earth", EffectKind.WORLD, "Carves a glowing glyph of the way you face onto the first blank line of a sign.");
	public static final RuneDef LAMPLIGHTER = effect("lamplighter", "Lamplighter", 1, 2, "fire", EffectKind.WORLD, "Lights the unlit candles and campfires within 6 blocks.", RADIUS);
	public static final RuneDef SNUFF_OUT = effect("snuff_out", "Douse", 1, 1, "wind", EffectKind.WORLD, "Puts out the candles and campfires within 6 blocks.", RADIUS);
	public static final RuneDef SIGN_GLOW = effect("sign_glow", "Sign Glow", 1, 2, "life", EffectKind.WORLD, "Makes the writing on signs within 6 blocks glow, front and back.", RADIUS);
	public static final RuneDef FRAME_VEIL = effect("frame_veil", "Frame Veil", 2, 4, "void", EffectKind.WORLD, "Item frames holding an item within 6 blocks turn invisible. Cast again to show them.", RADIUS);
	public static final RuneDef STAND_POSE = effect("stand_pose", "Stand Pose", 1, 2, "arcane", EffectKind.WORLD, "Armor stands within 6 blocks gain arms and step to their next pose.", RADIUS);
	public static final RuneDef LAVA_CRUST = effect("lava_crust", "Lava Crust", 2, 6, "frost", EffectKind.WORLD, "The lava top around the point hardens to basalt for 20 seconds, a safe place to land.", RADIUS, DURATION);
	public static final RuneDef VOID_STEP = effect("void_step", "Void Step", 2, 6, "void", EffectKind.WORLD, "A platform of end stone forms under your feet and crumbles from the edge after 6 seconds.", DURATION);
	public static final RuneDef LAVA_SENSE = effect("lava_sense", "Lava Sense", 1, 2, "fire", EffectKind.WORLD, "Tells how much lava lies within 8 blocks and which way the nearest is.", RADIUS);
	public static final RuneDef GOLD_PARLEY = effect("gold_parley", "Gold Parley", 2, 5, "fire", EffectKind.WORLD, "Piglins within 12 blocks forget their anger at you. Brutes stay angry.", RADIUS);
	// ---- fx-support pack
	public static final RuneDef WORST_FIRST = effect("worst_first", "Worst First", 1, 10, "arcane", EffectKind.HELPFUL, "Heals the most hurt ally within 8 blocks of where it lands (you too) for 6.", POWER, RADIUS);
	public static final RuneDef SALVE = effect("salve", "Salve", 1, 6, "frost", EffectKind.HELPFUL, "Heals 2, gives Regeneration I for 8 seconds and puts out fire.", POWER, DURATION);
	public static final RuneDef MENDING_MIST = effect("mending_mist", "Mending Mist", 2, 14, "frost", EffectKind.HELPFUL, "A 4-block mist for 8 seconds: allies inside heal 1 each second.", POWER, DURATION, RADIUS);
	public static final RuneDef HEARTHGLOW = effect("hearthglow", "Hearthglow", 2, 12, "fire", EffectKind.HELPFUL, "A 4-block glow for 12 seconds: allies inside are thawed and kept at Regeneration I.", DURATION, RADIUS);
	public static final RuneDef AFTERCARE = effect("aftercare", "Aftercare", 2, 9, "earth", EffectKind.HELPFUL, "For 15 seconds, each time the target is hurt it heals 1 a second later, 6 in all.", POWER, DURATION);
	public static final RuneDef HEARTHSONG = effect("hearthsong", "Hearthsong", 2, 12, "fire", EffectKind.HELPFUL, "Heals each ally within 6 blocks for 2, plus 1 for every ally there (up to 6).", POWER, RADIUS);
	public static final RuneDef GRACE = effect("grace", "Grace", 4, 30, "arcane", EffectKind.HELPFUL, "For 60 seconds a killing blow leaves the target at 2 health with Resistance III for 3 seconds. Once per 10 minutes each; never a boss.", DURATION);
	public static final RuneDef MANAGIFT = effect("managift", "Managift", 2, 4, "arcane", EffectKind.HELPFUL, "Gives up to 20 of your mana to each ally player hit; they get three quarters of it. Never yourself.", POWER);
	public static final RuneDef MANAWELL = effect("manawell", "Manawell", 3, 16, "arcane", EffectKind.HELPFUL, "A 3-block well for 10 seconds: allies inside regain 2 mana each second.", DURATION, RADIUS);
	public static final RuneDef GUARDLINK = effect("guardlink", "Guardlink", 3, 16, "earth", EffectKind.HELPFUL, "For 15 seconds, 40% of the ally's damage comes to you instead. Breaks past 16 blocks or at 6 health. Not on yourself.", DURATION);
	public static final RuneDef RALLY = effect("rally", "Rally", 2, 10, "wind", EffectKind.HELPFUL, "Allies within 8 blocks get Speed I and Jump Boost I for 12 seconds.", DURATION, RADIUS);
	public static final RuneDef MORALE = effect("morale", "Morale", 2, 12, "fire", EffectKind.HELPFUL, "Allies within 6 blocks get Absorption for 20 seconds, one level for each ally there (up to III).", DURATION, RADIUS);
	public static final RuneDef SHRUG_OFF = effect("shrug_off", "Shrug Off", 1, 4, "wind", EffectKind.HELPFUL, "Lifts the harmful effect with the most time left.");
	public static final RuneDef HEXGUARD = effect("hexguard", "Hexguard", 3, 14, "void", EffectKind.HELPFUL, "For 20 seconds, the next harmful effect that would land is refused.", DURATION);
	public static final RuneDef STOUTHEART = effect("stoutheart", "Stoutheart", 1, 5, "earth", EffectKind.HELPFUL, "Resistance I for 10 seconds (Amplify makes it II).", DURATION);
	public static final RuneDef IRONHOLD = effect("ironhold", "Ironhold", 3, 15, "earth", EffectKind.HELPFUL, "For 6 seconds no single blow deals the target more than 4.", DURATION);
	public static final RuneDef EVADE = effect("evade", "Evade", 2, 8, "wind", EffectKind.HELPFUL, "For 10 seconds the next melee blow misses and the target slips aside.", DURATION);
	public static final RuneDef EMBERGUARD = effect("emberguard", "Emberguard", 1, 5, "fire", EffectKind.HELPFUL, "Puts the target out and gives Fire Resistance for 30 seconds.", DURATION);
	public static final RuneDef BEASTGUARD = effect("beastguard", "Beastguard", 1, 6, "arcane", EffectKind.HELPFUL, "Your pet, mount or a farm animal gets Resistance II and Fire Resistance for 60 seconds.", DURATION);
	public static final RuneDef HEARTHGUARD = effect("hearthguard", "Hearthguard", 2, 10, "earth", EffectKind.HELPFUL, "A villager, trader or golem takes 60% less damage from monsters for 5 minutes.", DURATION);
	public static final RuneDef HEEL = effect("heel", "Heel", 2, 6, "wind", EffectKind.HELPFUL, "Your pets within 32 blocks that are not sitting come to your side.");
	public static final RuneDef BELLWARD = effect("bellward", "Bellward", 3, 18, "arcane", EffectKind.WORLD, "For 2 minutes villagers within 12 blocks take 60% less damage from monsters, and monsters there glow.", DURATION, RADIUS);
	public static final RuneDef SANCTUARY = effect("sanctuary", "Sanctuary", 3, 20, "arcane", EffectKind.WORLD, "A 6-block circle for 30 seconds: no monster spawns there, and monsters inside are nudged out.", DURATION, RADIUS);
	public static final RuneDef ARROWVEIL = effect("arrowveil", "Arrowveil", 2, 12, "wind", EffectKind.WORLD, "A 4-block dome for 10 seconds: enemy missiles inside drop out of the air.", DURATION, RADIUS);
	public static final RuneDef BLASTWARD = effect("blastward", "Blastward", 2, 10, "earth", EffectKind.WORLD, "For 60 seconds explosions within 6 blocks break no blocks.", DURATION, RADIUS);
	public static final RuneDef FIREBREAK = effect("firebreak", "Firebreak", 1, 5, "frost", EffectKind.WORLD, "Puts out fire within 5 blocks, and the allies there.", RADIUS);
	public static final RuneDef PACIFY = effect("pacify", "Pacify", 2, 9, "arcane", EffectKind.HARMFUL, "The creature forgets its target and picks none for 6 seconds; harming it ends this. Players get Weakness instead. Not bosses.", DURATION);
	public static final RuneDef LURE = effect("lure", "Lure", 2, 9, "void", EffectKind.HARMFUL, "Enemies drop their target and walk to where it lands for 4 seconds.", DURATION);
	public static final RuneDef STILLBIND = effect("stillbind", "Stillbind", 2, 10, "void", EffectKind.HARMFUL, "Holds the target in place for 3 seconds without harm. Bosses are only slowed.", DURATION);
	public static final RuneDef TAUNT = effect("taunt", "Taunt", 1, 5, "blood", EffectKind.HARMFUL, "Enemies hit turn on you for 6 seconds, and you get Resistance I for as long.", DURATION);
	public static final RuneDef NUDGE = effect("nudge", "Nudge", 1, 3, "wind", EffectKind.HARMFUL, "A gentle push 3 blocks away from you. No harm.", POWER);
	public static final RuneDef HOBBLE = effect("hobble", "Hobble", 1, 4, "void", EffectKind.HARMFUL, "Slowness III for 4 seconds. No harm.", DURATION);
	public static final RuneDef CORRAL = effect("corral", "Corral", 3, 14, "wind", EffectKind.HARMFUL, "Enemies within 5 blocks are kept inside the ring for 6 seconds.", DURATION, RADIUS);
	public static final RuneDef TRUCE = effect("truce", "Truce", 3, 16, "arcane", EffectKind.HARMFUL, "Every enemy within 8 blocks is pacified for 4 seconds. Not bosses.", DURATION, RADIUS);
	public static final RuneDef SPOOK = effect("spook", "Spook", 2, 8, "void", EffectKind.HARMFUL, "Enemies flee from you for 3 seconds. Bosses and players are only slowed.", DURATION);
	public static final RuneDef AEGIS = effect("aegis", "Aegis", 4, 36, "arcane", EffectKind.HELPFUL, "You and allies within 6 blocks take half damage and none from missiles for 6 seconds. Rests 2 minutes.", DURATION, RADIUS);
	public static final RuneDef ACCORD = effect("accord", "Accord", 4, 34, "arcane", EffectKind.WORLD, "Enemies within 16 blocks are pacified for 10 seconds and no monster spawns there meanwhile. Not bosses.", DURATION, RADIUS);
	public static final RuneDef CITADEL = effect("citadel", "Citadel", 4, 38, "earth", EffectKind.WORLD, "An 8-block ward for 60 seconds: explosions break nothing, missiles from outside drop at the edge, and allies inside take 20% less damage.", DURATION, RADIUS);
	public static final RuneDef SHIELDWALL = effect("shieldwall", "Shieldwall", 2, 12, "earth", EffectKind.HELPFUL, "You and allies within 5 blocks can't be knocked back and get Resistance I for 8 seconds.", DURATION, RADIUS);
	public static final RuneDef STAUNCH = effect("staunch", "Staunch", 1, 5, "frost", EffectKind.HELPFUL, "Ends poison and wither and keeps them off for 10 seconds.", DURATION);
	public static final RuneDef SENTRY = effect("sentry", "Sentry", 1, 4, "arcane", EffectKind.HELPFUL, "Monsters within 16 blocks of the target glow for 10 seconds.", DURATION, RADIUS);
	public static final RuneDef TEND = effect("tend", "Tend", 1, 6, "earth", EffectKind.HELPFUL, "Heals a villager, golem, pet or animal for 8 (an iron golem for 16).", POWER);
	public static final RuneDef SOOTHE = effect("soothe", "Soothe", 2, 7, "arcane", EffectKind.HARMFUL, "A neutral creature forgets its anger and its target.");
	public static final RuneDef WITHDRAW = effect("withdraw", "Withdraw", 3, 14, "wind", EffectKind.HELPFUL, "An ally below half health turns invisible with Speed II for 4 seconds, and its hunters lose track of it.", DURATION);
	public static final RuneDef KEEPSAFE = effect("keepsafe", "Keepsafe", 2, 8, "earth", EffectKind.WORLD, "For 10 minutes the block hit can't be broken by anyone outside your party, nor by explosions.", DURATION);
	public static final RuneDef FAITHFUL = effect("faithful", "Faithful", 3, 16, "arcane", EffectKind.HELPFUL, "For 5 minutes, a killing blow leaves your pet at 1 health beside you. Once per 10 minutes each.", DURATION);

	/** Runes you learn the first time you wear a Cord. */
	public static final Set<String> STARTER = Set.of(SELF.id(), BOLT.id(), PUSH.id());

	/** Innate runes: never crafted or found, one per caster. */
	public static final java.util.List<RuneDef> INNATE = java.util.List.of(BLOOD_THREAD, KINDLING, TWIN_STAR, BORROWED_TIME, GALE_MANTLE, STONEFORM,
		MIRRORFROST, FORTUNE, PHANTOM, STORMHEART);

	public static boolean innate(RuneDef rune) {
		return INNATE.contains(rune);
	}

	/**
	 * Whether a rune may turn up anywhere at random (a chest, a trade, a reward, a boss's drop): not
	 * innate, not made only at the Fusion Altar, and not one of the runes of the world, which are found
	 * only in their own places (see {@link RuneSources}).
	 */
	public static boolean common(RuneDef rune) {
		return !innate(rune) && !fused(rune) && !RuneSources.foundOnly(rune);
	}

	/**
	 * Whether a caster can come by {@code rune} at all: found at random (tiers 1 to 4), found in its
	 * own places, or made at the Fusion Altar. Never an innate one (one per caster, never found).
	 */
	public static boolean obtainable(RuneDef rune) {
		if (innate(rune)) {
			return false;
		}
		return fused(rune) || RuneSources.foundOnly(rune) || rune.tier() >= 1 && rune.tier() <= 4;
	}

	/**
	 * How many runes a spellbook knows, as the Heart Circles and the advancements count them: runes of
	 * the roster only, so a Knot (a spell tied into one rune) or a rune of an add-on that isn't loaded
	 * doesn't count.
	 */
	public static int countKnown(Collection<String> learned) {
		int n = 0;
		for (String id : learned) {
			if (ALL.containsKey(id)) {
				n++;
			}
		}
		return n;
	}

	/** A rune by id: one of the roster, a Knot or a woven pair (both dynamic ids carry their contents). */
	public static Optional<RuneDef> get(String id) {
		RuneDef rune = ALL.get(id);
		if (rune != null) {
			return Optional.of(rune);
		}
		if (Knots.isKnot(id)) {
			return Knots.def(id);
		}
		if (PairRunes.isPair(id)) {
			return PairRunes.def(id);
		}
		return WovenRunes.isWoven(id) ? WovenRunes.def(id) : Optional.empty();
	}

	public static Collection<RuneDef> all() {
		return Collections.unmodifiableCollection(ALL.values());
	}

	/**
	 * Adds an add-on's rune to the roster (the add-on API's way in: see {@code dev.wildercord.api}). Its
	 * id must be in the add-on's own namespace, and its category one its family lists.
	 */
	public static synchronized RuneDef registerAddon(RuneDef def) {
		if (def.id().startsWith("wildercord:") || def.id().indexOf(':') <= 0) {
			throw new IllegalArgumentException("An add-on rune needs its own namespace: " + def.id());
		}
		return register(def);
	}

	private static RuneDef register(RuneDef def) {
		if (ALL.put(def.id(), def) != null) {
			throw new IllegalStateException("Duplicate rune " + def.id());
		}
		return def;
	}

	private static String id(String path) {
		return "wildercord:" + path;
	}

	private static RuneDef shape(String path, String name, int tier, double cost, double effectMultiplier, String desc, String... traits) {
		java.util.Set<String> all = new java.util.HashSet<>(Set.of(traits));
		all.add(Trait.COOLDOWN);
		all.add(Trait.CIRCLE);
		return register(new RuneDef(id(path), name, RuneFamily.SHAPE, tier, cost, effectMultiplier, "", EffectKind.NONE, all, "", desc,
			RuneCategories.categoryFor(path, RuneFamily.SHAPE)));
	}

	private static RuneDef effect(String path, String name, int tier, double cost, String element, EffectKind kind, String desc, String... traits) {
		java.util.Set<String> all = new java.util.HashSet<>(Set.of(traits));
		all.add(FRUGAL);
		if (kind == EffectKind.HELPFUL && !Unshared.PATHS.contains(path)) {
			all.add(SHARE);
		}
		return register(new RuneDef(id(path), name, RuneFamily.EFFECT, tier, cost, 1.0, element, kind, all, "", desc,
			RuneCategories.categoryFor(path, RuneFamily.EFFECT)));
	}

	private static RuneDef modifier(String path, String name, int tier, double costMultiplier, String needs, String desc) {
		return register(new RuneDef(id(path), name, RuneFamily.MODIFIER, tier, 0, costMultiplier, "", EffectKind.NONE, Set.of(), needs, desc,
			RuneCategories.categoryFor(path, RuneFamily.MODIFIER)));
	}

	private static RuneDef link(String path, String name, int tier, double cost, String desc, String... traits) {
		return register(new RuneDef(id(path), name, RuneFamily.LINK, tier, cost, 1.0, "", EffectKind.NONE, Set.of(traits), "", desc,
			RuneCategories.categoryFor(path, RuneFamily.LINK)));
	}

	/**
	 * Helpful effects Kindred can't share ({@link Trait#SHARE}): ones that act on a place or only ever on
	 * their caster (summons, a dome, a war horn, a breeze), the death saves, the ones that would trap or move
	 * whoever they were shared with (Cryostasis, Shulkershell, Rewind), cost the one they land on something
	 * (Overdrive, Transfusion), bind two creatures (Soulbond), and the innate runes. Kept in a class of its own
	 * so it's ready whenever the roster above is being built (tools/wiki.py reads this line).
	 */
	private static final class Unshared {
		static final Set<String> PATHS = Set.of("summon", "shades", "thunderbird", "haven", "warcry", "zephyr", "soulbond", "lifeline", "transfusion", "reversal", "second_wind", "cryostasis", "shulkershell", "rewind", "overdrive", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom", "stormheart");
	}
}
