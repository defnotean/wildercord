package dev.wildercord.spell;

import java.util.Set;

/**
 * Magic that changes the world: which effects touch the ground they land on, how far, how much,
 * and how being wet changes a hit. Pure numbers and rules, shared by the server (which does it),
 * the Cord screen (which says so in a rune's tooltip) and the unit tests.
 * <ul>
 *   <li><b>Fire</b> (harmful fire) sets flammable blocks by the impact alight, lights candles and
 *       campfires, primes TNT, melts snow, ice and powder snow, and flashes a puddle into a cloud of
 *       steam that blinds.</li>
 *   <li><b>Frost</b> (harmful frost) freezes the surface of water into frosted ice you can walk
 *       on, cools the surface of lava into a crust that melts back, puts out fire and snuffs campfires
 *       and candles.</li>
 *   <li><b>Storm</b> (harmful storm) that lands in water runs through it, shocking every creature
 *       in the same water; it scrapes a stage of oxidation off copper, pulses lightning rods, and may
 *       charge a creeper it strikes.</li>
 *   <li><b>Wind</b> (harmful wind) knocks projectiles out of the air, blows out small fires and
 *       candles, and scatters loose items and experience.</li>
 *   <li><b>Earth</b> (harmful earth) heaves the ground up in slabs (block displays only) and
 *       throws creatures standing on it upward.</li>
 *   <li><b>Life</b> (any life effect that lands on something) makes grass and flowers bloom, and
 *       starts curing a weakened zombie villager.</li>
 *   <li><b>Void</b> (harmful void) draws loose items and experience in toward where it lands, and
 *       anchors an enderman so it can't teleport.</li>
 *   <li><b>Time</b> (any time effect but Stasis and Rewind) ages the world: crops and saplings grow,
 *       copper weathers, babies grow up and a furnace jumps ahead in its smelt.</li>
 *   <li><b>Arcane</b> (harmful arcane) makes bookshelves and enchanting tables shimmer and shows
 *       invisible creatures for a moment.</li>
 *   <li><b>Blood</b> (harmful blood) feeds the Nether's growth: nether wart and crimson fungus grow.</li>
 * </ul>
 * Block changes only ever happen for a player allowed to build there, never for a monster, and
 * are vanilla's own temporary or natural ones: fire (which spreads and burns out by vanilla's
 * rules), frosted ice (which melts back), a crust on lava (which melts back), grass and flowers,
 * growth, and copper's own weathering.
 */
public final class WorldRules {
	private WorldRules() {}

	/** What an effect does to the world where it lands. */
	public enum Interaction {
		NONE, IGNITE, FREEZE, CONDUCT, GUST, HEAVE, BLOOM, DRAW, AGE, SHIMMER, FEED;

		/** The lang key of its line in a rune's tooltip. */
		public String tooltipKey() {
			return "tooltip.wildercord.world." + name().toLowerCase(java.util.Locale.ROOT);
		}

		/**
		 * Whether it ever changes blocks (those changes need building rights, and never happen for a
		 * monster; the rest of it, a shock through water, say, still does).
		 */
		public boolean editsBlocks() {
			return this == IGNITE || this == FREEZE || this == CONDUCT || this == GUST || this == BLOOM || this == AGE || this == FEED;
		}
	}

	/**
	 * Effects of an interacting element that leave the world alone anyway: World runes that already
	 * change blocks their own way (Grow, Harvest, Icepath, Smelt...), Bubble (it soaks, it doesn't
	 * freeze), the pins (Root, Weigh, Shackle hold creatures down rather than heave them up), and
	 * Stasis and Rewind (they stop time or turn it back, never on).
	 */
	private static final Set<String> QUIET = Set.of("bubble", "root", "weigh", "shackle", "kindling", "stasis", "rewind");

	/**
	 * Storm runes that call down a real (if harmless) lightning bolt: vanilla's own strike already
	 * scrapes the copper and pulses the rod it lands on, so storm's world magic leaves those to it.
	 */
	private static final Set<String> BOLTS = Set.of("lightning", "tempest");

	public static Interaction of(RuneDef rune) {
		if (rune == null || rune.family() != RuneFamily.EFFECT || rune.kind() == EffectKind.WORLD || QUIET.contains(rune.path())) {
			return Interaction.NONE;
		}
		boolean harmful = rune.kind() == EffectKind.HARMFUL;
		boolean helpful = rune.kind() == EffectKind.HELPFUL;
		return switch (rune.element()) {
			case "fire" -> harmful ? Interaction.IGNITE : Interaction.NONE;
			case "frost" -> harmful ? Interaction.FREEZE : Interaction.NONE;
			case "storm" -> harmful ? Interaction.CONDUCT : Interaction.NONE;
			case "wind" -> harmful ? Interaction.GUST : Interaction.NONE;
			case "earth" -> harmful ? Interaction.HEAVE : Interaction.NONE;
			case "life" -> harmful || helpful ? Interaction.BLOOM : Interaction.NONE;
			case "void" -> harmful ? Interaction.DRAW : Interaction.NONE;
			case "time" -> harmful || helpful ? Interaction.AGE : Interaction.NONE;
			case "arcane" -> harmful ? Interaction.SHIMMER : Interaction.NONE;
			case "blood" -> harmful ? Interaction.FEED : Interaction.NONE;
			default -> Interaction.NONE;
		};
	}

	/** Whether a storm rune calls down its own lightning bolt (whose strike does copper and rods vanilla's way). */
	public static boolean callsLightning(RuneDef rune) {
		return rune != null && BOLTS.contains(rune.path());
	}

	/** Whether an effect leaves its target wet (Tidebreath, a water spell). Bubble soaks through its own mark. */
	public static boolean wets(RuneDef rune) {
		return rune != null && rune.path().equals("tidebreath");
	}

	// ------------------------------------------------------------------ the numbers

	/** World changes one whole cast may make, links, pulses and echoes included (on top of the cast's block budget). */
	public static final int EDITS_PER_CAST = 24;

	/** Fire: how far from the impact it looks for something to burn, and how many fires it lights. */
	public static final double IGNITE_RADIUS = 2.0;
	public static final int IGNITE_MAX = 3;
	/** Fire: candles, candle cakes, unlit campfires and TNT it lights (TNT it primes) per hit, in the same reach as its fires. */
	public static final int KINDLE_MAX = 4;
	/** Fire: snow layers, powder snow and ice melted per hit. */
	public static final int MELT_MAX = 6;
	/** Fire: a puddle this small (source blocks, connected) or smaller boils away; anything bigger only steams. */
	public static final int PUDDLE_MAX = 4;
	/** Steam: how long it hangs, how wide it is, and the Blindness it gives each second to creatures inside. */
	public static final int STEAM_TICKS = 80;
	public static final double STEAM_RADIUS = 2.0;
	public static final int STEAM_BLIND_TICKS = 40;

	/** Frost: how far across it freezes water, and how many blocks at most. */
	public static final double FREEZE_RADIUS = 2.5;
	public static final int FREEZE_MAX = 16;
	/** Frost: fires, campfires and candles put out per hit. */
	public static final int SNUFF_MAX = 6;
	/** Frost: frosted ice still standing this long after it froze melts back, even in the dark. */
	public static final int THAW_TICKS = 600;
	/** Frost on lava: how many blocks of its surface cool into a crust (in the same reach as the freeze). */
	public static final int CRUST_MAX = 12;
	/** Frost on lava: how long the crust holds before it melts back into lava (give or take a second). */
	public static final int CRUST_TICKS = 500;
	/** Frost on lava: for this long before it melts, the crust glows and cracks (and burns to stand on), a warning to get off. */
	public static final int CRUST_WARN_TICKS = 100;

	/** Storm: how far through water it runs, how much water it searches, and how many creatures it shocks. */
	public static final int CONDUCT_RADIUS = 8;
	public static final int CONDUCT_WATER_MAX = 384;
	public static final int CONDUCT_TARGETS = 8;
	/** Storm: conductions one whole cast may set off (a Zone in a lake doesn't shock it every second forever). */
	public static final int CONDUCTS_PER_CAST = 3;
	/** Shocking this many creatures through water at once earns Conductor. */
	public static final int CONDUCTOR_FEAT = 5;
	/** Storm: how far from where it lands it scrapes copper and pulses lightning rods (a cube this many blocks out), and how many of each. */
	public static final int STORM_REACH = 1;
	public static final int COPPER_MAX = 4;
	public static final int RODS_MAX = 2;
	/** Storm: the chance each creeper it strikes is charged, as a lightning strike would charge it. */
	public static final double CREEPER_CHARGE_CHANCE = 0.25;

	/** Wind: how far from the impact it reaches, and how much it moves at most. */
	public static final double GUST_RADIUS = 3.5;
	public static final int GUST_PROJECTILES = 8;
	public static final int GUST_LOOSE = 16;
	/** Wind: fires and lit candles blown out per hit. */
	public static final int GUST_FIRES = 4;

	/** Earth: slabs of ground heaved up, how far out, and the upward throw for creatures on them. */
	public static final int HEAVE_SLABS = 5;
	public static final double HEAVE_RADIUS = 2.0;
	public static final double HEAVE_LIFT = 0.45;

	/** Life: how far it blooms, and grass and flowers grown per hit. */
	public static final double BLOOM_RADIUS = 2.0;
	public static final int BLOOM_MAX = 3;
	/** Life: how long a zombie villager it starts curing takes to turn back, at least and at most (a golden apple's). */
	public static final int CURE_MIN_TICKS = 3600;
	public static final int CURE_MAX_TICKS = 6000;

	/** Void: how far it draws loose items and experience from, and how many. */
	public static final double DRAW_RADIUS = 4.0;
	public static final int DRAW_LOOSE = 16;
	/** Void: how long an enderman it strikes can't teleport. */
	public static final int ANCHOR_TICKS = 100;

	/** Time: how far it ages plants and copper, and how many blocks per hit. */
	public static final double AGE_RADIUS = 2.0;
	public static final int AGE_MAX = 3;
	/** Time: how far it reaches babies, how many, and how much older each grows (seconds of the 20 minutes growing up takes). */
	public static final double AGE_BABY_RADIUS = 3.0;
	public static final int AGE_BABIES = 4;
	public static final int AGE_BABY_SECONDS = 120;
	/** Time: furnaces, smokers and blast furnaces it jumps ahead per hit, and by how many ticks of smelting (a furnace takes 200 an item). */
	public static final int FURNACE_MAX = 2;
	public static final int FURNACE_SKIP_TICKS = 100;

	/** Arcane: how far bookshelves and enchanting tables shimmer (a box this many blocks out, half as high), and how many. */
	public static final int SHIMMER_REACH = 3;
	public static final int SHIMMER_MAX = 8;
	/** Arcane: how far it shows invisible creatures, how many, and for how long they glow. */
	public static final double REVEAL_RADIUS = 4.0;
	public static final int REVEAL_MAX = 8;
	public static final int REVEAL_TICKS = 60;

	/** Blood: how far it feeds nether wart and crimson fungus, and how many per hit. */
	public static final double FEED_RADIUS = 2.0;
	public static final int FEED_MAX = 3;

	/** How many of {@code wanted} this hit may have: its own cap and what's left of the cast's allowance. */
	public static int allowance(int wanted, int perHit, int castLeft) {
		return Math.max(0, Math.min(wanted, Math.min(perHit, castLeft)));
	}

	/**
	 * How far a furnace's smelt jumps ahead: {@link #FURNACE_SKIP_TICKS}, but never onto the last tick
	 * (the furnace finishes the item itself, and puts it where it belongs) and never past its fuel.
	 */
	public static int furnaceSkip(int timer, int total, int fuelLeft) {
		return Math.max(0, Math.min(FURNACE_SKIP_TICKS, Math.min(total - 1 - timer, fuelLeft - 1)));
	}

	/** A baby's age (negative until it grows up) after time ages it: {@link #AGE_BABY_SECONDS} older, never past grown. */
	public static int agedBaby(int age) {
		return age >= 0 ? age : Math.min(0, age + AGE_BABY_SECONDS * 20);
	}

	/**
	 * Shock damage through water, before the caster's power: 4 within 3 blocks of where the spell
	 * met the water, falling off to 60% at the edge of its reach.
	 */
	public static double conductDamage(double distance) {
		double near = 3.0;
		if (distance <= near) {
			return 4.0;
		}
		double k = Math.min(1.0, (distance - near) / (CONDUCT_RADIUS - near));
		return 4.0 * (1.0 - 0.4 * k);
	}

	// ------------------------------------------------------------------ being wet

	/** How long a creature stays wet after Tidebreath or steam (a popped Bubble soaks it for 5 s through its own mark). */
	public static final int WET_TICKS = 100;
	/** Fire on a wet creature. */
	public static final double WET_FIRE = 0.75;
	/** Frost on a wet creature freezes it this much longer (and a light chill freezes it solid). */
	public static final int WET_FREEZE_TICKS = 60;
	/** Storm on a wet creature: it conducts (the Conduct reaction's +50%). */
	public static final double WET_STORM = 1.5;

	/** Wet: standing in water or rain, or still dripping (Tidebreath, steam, a popped Bubble). */
	public static boolean wet(boolean inWater, boolean inRain, boolean dripping) {
		return inWater || inRain || dripping;
	}

	/** The damage multiplier for an element's hit on a creature that's wet or not. */
	public static double wetDamage(String element, boolean wet) {
		if (!wet) {
			return 1.0;
		}
		return "fire".equals(element) ? WET_FIRE : 1.0;
	}
}
