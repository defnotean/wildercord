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
	public static final RuneDef HARVEST = effect("harvest", "Harvest", 1, 3, "life", EffectKind.WORLD, "Harvests grown crops around the block hit, and replants them.", RADIUS);
	public static final RuneDef BASINFILL = effect("basinfill", "Basinfill", 1, 6, "frost", EffectKind.WORLD, "Fills an enclosed, one-block-deep hole with permanent source water: at most 16 connected cells, within three blocks of the impact. Aim at its floor with Touch or Bolt. Refuses open edges, deep pits, protected ground and the Nether. One basin per paid cast.");
	public static final RuneDef ICEPATH = effect("icepath", "Icepath", 1, 3, "frost", EffectKind.WORLD, "Freezes water within 3 blocks into ice you can walk on. On Self it lays a strip of ice ten blocks long the way you look.", RADIUS);
	public static final RuneDef COLLECT = effect("collect", "Collect", 1, 3, "void", EffectKind.WORLD, "Pulls up to 48 items and experience within 8 blocks to you.", RADIUS);
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
	public static final RuneDef HAVEN = effect("haven", "Haven", 2, 14, "life", EffectKind.HELPFUL, "Raises a 4-block dome of light for 8 seconds: enemies inside are shoved out once a second, and enemy projectiles glance off it.", DURATION, RADIUS);
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
		return FUSED.contains(rune) || SIGNATURE.contains(rune) || WovenRunes.isWoven(rune);
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
		DOOMCLOCK, THUNDERSTEP, HALO, THUNDERQUAKE, COMETFALL, RIPOSTE, DUST_DEVIL, MALISON, AVALANCHE, CINDER_BULWARK, ROOT_BULWARK, BOILING_SURGE, THUNDER_TIDE, RIME_CAUSEWAY, THUNDER_WALK);
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
		static final Set<String> PATHS = Set.of("summon", "shades", "thunderbird", "haven", "warcry", "zephyr", "soulbond", "transfusion", "reversal", "second_wind", "cryostasis", "shulkershell", "rewind", "overdrive", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom", "stormheart");
	}
}
