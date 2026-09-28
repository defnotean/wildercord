package dev.wildercord.spell;

import java.util.Set;

/**
 * Magic that changes the world: which effects touch the ground they land on, how far, how much,
 * and how being wet changes a hit. Pure numbers and rules, shared by the server (which does it),
 * the Cord screen (which says so in a rune's tooltip) and the unit tests.
 * <ul>
 *   <li><b>Fire</b> (harmful fire) sets flammable blocks by the impact alight, melts snow, ice and
 *       powder snow, and flashes a puddle into a cloud of steam that blinds.</li>
 *   <li><b>Frost</b> (harmful frost) freezes the surface of water into frosted ice you can walk
 *       on, puts out fire and snuffs campfires.</li>
 *   <li><b>Storm</b> (harmful storm) that lands in water runs through it, shocking every creature
 *       in the same water.</li>
 *   <li><b>Wind</b> (harmful wind) knocks projectiles out of the air, blows out small fires and
 *       scatters loose items and experience.</li>
 *   <li><b>Earth</b> (harmful earth) heaves the ground up in slabs (block displays only) and
 *       throws creatures standing on it upward.</li>
 *   <li><b>Life</b> (any life effect that lands on something) makes grass and flowers bloom.</li>
 *   <li><b>Void</b> (harmful void) draws loose items and experience in toward where it lands.</li>
 * </ul>
 * Block changes only ever happen for a player allowed to build there, never for a monster, and
 * are vanilla's own temporary or natural ones: fire (which spreads and burns out by vanilla's
 * rules), frosted ice (which melts back), grass and flowers.
 */
public final class WorldRules {
	private WorldRules() {}

	/** What an effect does to the world where it lands. */
	public enum Interaction {
		NONE, IGNITE, FREEZE, CONDUCT, GUST, HEAVE, BLOOM, DRAW;

		/** The lang key of its line in a rune's tooltip. */
		public String tooltipKey() {
			return "tooltip.wildercord.world." + name().toLowerCase(java.util.Locale.ROOT);
		}

		/** Whether it ever changes blocks (and so needs building rights, and never happens for a monster). */
		public boolean editsBlocks() {
			return this == IGNITE || this == FREEZE || this == GUST || this == BLOOM;
		}
	}

	/**
	 * Effects of an interacting element that leave the world alone anyway: World runes that already
	 * change blocks their own way (Grow, Harvest, Icepath, Smelt...), Bubble (it soaks, it doesn't
	 * freeze), and the pins (Root, Weigh, Shackle hold creatures down rather than heave them up).
	 */
	private static final Set<String> QUIET = Set.of("bubble", "root", "weigh", "shackle", "kindling");

	public static Interaction of(RuneDef rune) {
		if (rune == null || rune.family() != RuneFamily.EFFECT || rune.kind() == EffectKind.WORLD || QUIET.contains(rune.path())) {
			return Interaction.NONE;
		}
		boolean harmful = rune.kind() == EffectKind.HARMFUL;
		return switch (rune.element()) {
			case "fire" -> harmful ? Interaction.IGNITE : Interaction.NONE;
			case "frost" -> harmful ? Interaction.FREEZE : Interaction.NONE;
			case "storm" -> harmful ? Interaction.CONDUCT : Interaction.NONE;
			case "wind" -> harmful ? Interaction.GUST : Interaction.NONE;
			case "earth" -> harmful ? Interaction.HEAVE : Interaction.NONE;
			case "life" -> harmful || rune.kind() == EffectKind.HELPFUL ? Interaction.BLOOM : Interaction.NONE;
			case "void" -> harmful ? Interaction.DRAW : Interaction.NONE;
			default -> Interaction.NONE;
		};
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
	/** Frost: fires and campfires put out per hit. */
	public static final int SNUFF_MAX = 6;
	/** Frost: frosted ice still standing this long after it froze melts back, even in the dark. */
	public static final int THAW_TICKS = 600;

	/** Storm: how far through water it runs, how much water it searches, and how many creatures it shocks. */
	public static final int CONDUCT_RADIUS = 8;
	public static final int CONDUCT_WATER_MAX = 384;
	public static final int CONDUCT_TARGETS = 8;
	/** Storm: conductions one whole cast may set off (a Zone in a lake doesn't shock it every second forever). */
	public static final int CONDUCTS_PER_CAST = 3;
	/** Shocking this many creatures through water at once earns Conductor. */
	public static final int CONDUCTOR_FEAT = 5;

	/** Wind: how far from the impact it reaches, and how much it moves at most. */
	public static final double GUST_RADIUS = 3.5;
	public static final int GUST_PROJECTILES = 8;
	public static final int GUST_LOOSE = 16;
	public static final int GUST_FIRES = 4;

	/** Earth: slabs of ground heaved up, how far out, and the upward throw for creatures on them. */
	public static final int HEAVE_SLABS = 5;
	public static final double HEAVE_RADIUS = 2.0;
	public static final double HEAVE_LIFT = 0.45;

	/** Life: how far it blooms, and grass and flowers grown per hit. */
	public static final double BLOOM_RADIUS = 2.0;
	public static final int BLOOM_MAX = 3;

	/** Void: how far it draws loose items and experience from, and how many. */
	public static final double DRAW_RADIUS = 4.0;
	public static final int DRAW_LOOSE = 16;

	/** How many of {@code wanted} this hit may have: its own cap and what's left of the cast's allowance. */
	public static int allowance(int wanted, int perHit, int castLeft) {
		return Math.max(0, Math.min(wanted, Math.min(perHit, castLeft)));
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
