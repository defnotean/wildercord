package dev.wildercord.spell;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rules and numbers of the element reactions that joined the first five, so that every element
 * takes part in one: which element's damage sets each off, what it does, which runes leave the marks
 * they need, and which runes play a part (for a rune's tooltip). Pure data, shared by the server
 * ({@code cast.Reactions}, which sets them off), the Cord screen and the unit tests.
 * <ul>
 *   <li><b>Overload</b>: storm damage on a burning foe. The flames burst: that hit +30%, 4 damage to
 *       every other enemy within 3 blocks, thrown back, and the fire goes out.</li>
 *   <li><b>Fracture</b>: earth damage on a frozen foe. The ice cracks: that hit +40%, it thaws, and
 *       it's left <i>cracked</i> for 5 seconds (every spell hits it 20% harder).</li>
 *   <li><b>Blight</b>: life damage on a <i>shadowed</i> foe (void's curses and darkness). Rot bursts
 *       from it: it and up to 5 other enemies within 4 blocks take 3 damage and are poisoned, and the
 *       caster heals 1 for each (once a second at most).</li>
 *   <li><b>Unweave</b>: arcane damage on a foe with two marks or more. Every mark comes undone: that
 *       hit +30% for each (at most +120%).</li>
 *   <li><b>Rupture</b>: wind damage on a <i>bleeding</i> foe (blood's cuts). The wound tears open: that
 *       hit +50%, 4 damage more straight through armour, and the caster heals 2 (once a second at most).</li>
 *   <li><b>Elapse</b>: time damage on a foe that's burning, poisoned or withering. Their time passes at
 *       once: all the damage they had left, half again, lands now (3 to 16), and they end.</li>
 * </ul>
 */
public final class ReactionRules {
	private ReactionRules() {}

	public static final String OVERLOAD = "overload";
	public static final String FRACTURE = "fracture";
	public static final String BLIGHT = "blight";
	public static final String UNWEAVE = "unweave";
	public static final String RUPTURE = "rupture";
	public static final String ELAPSE = "elapse";

	/** The newer reactions, in the order the Grimoire lists them (after the first five). */
	public static final List<String> NEWER = List.of(OVERLOAD, FRACTURE, BLIGHT, UNWEAVE, RUPTURE, ELAPSE);

	/** The element whose damage sets each newer reaction off. */
	public static final Map<String, String> TRIGGER = Map.of(OVERLOAD, "storm", FRACTURE, "earth", BLIGHT, "life", UNWEAVE, "arcane",
		RUPTURE, "wind", ELAPSE, "time");

	// ---- Overload: storm on a burning foe.
	public static final double OVERLOAD_BONUS = 1.3;
	public static final double OVERLOAD_RADIUS = 3.0;
	public static final double OVERLOAD_DAMAGE = 4.0;

	// ---- Fracture: earth on a frozen foe, and the crack it leaves.
	public static final double FRACTURE_BONUS = 1.4;
	/** Cracked: every spell hits it this much harder while it lasts. */
	public static final double CRACKED_BONUS = 1.2;
	public static final int CRACKED_TICKS = 100;

	// ---- Blight: life on a shadowed foe.
	public static final double BLIGHT_RADIUS = 4.0;
	public static final double BLIGHT_DAMAGE = 3.0;
	public static final int BLIGHT_POISON_TICKS = 100;
	/** Creatures the rot reaches at most, the one it bursts from among them. */
	public static final int BLIGHT_REACH = 6;
	/** Health the caster heals for each creature the rot reaches. */
	public static final float BLIGHT_HEAL = 1.0F;
	/**
	 * A caster heals from Blight, and from Rupture, at most once in this many ticks: a Zone setting either
	 * off on a whole crowd every second heals like one, not like every one of them.
	 */
	public static final int HEAL_EVERY = 20;
	/** How long a void curse leaves a foe shadowed, unless the curse says otherwise. */
	public static final int SHADOWED_TICKS = 120;

	// ---- Unweave: arcane on a foe with two marks or more.
	public static final int UNWEAVE_MIN_MARKS = 2;
	public static final int UNWEAVE_MAX_MARKS = 4;
	public static final double UNWEAVE_PER_MARK = 0.3;

	/** Unweave's damage multiplier for a foe with {@code marks} marks: nothing under two, +30% each, at most four counted. */
	public static double unweave(int marks) {
		return marks < UNWEAVE_MIN_MARKS ? 1.0 : 1.0 + UNWEAVE_PER_MARK * Math.min(marks, UNWEAVE_MAX_MARKS);
	}

	// ---- Rupture: wind on a bleeding foe.
	public static final double RUPTURE_BONUS = 1.5;
	public static final double RUPTURE_DAMAGE = 4.0;
	public static final float RUPTURE_HEAL = 2.0F;
	/** How long a cut leaves a foe bleeding, unless the cut says otherwise. */
	public static final int BLEEDING_TICKS = 80;

	// ---- Elapse: time on a burning, poisoned or withering foe.
	public static final double ELAPSE_BONUS = 1.5;
	public static final double ELAPSE_MIN = 3.0;
	public static final double ELAPSE_MAX = 16.0;

	/** Ticks between poison's wounds at {@code amplifier} (0 = Poison I), as vanilla deals them: 1 damage each. */
	public static int poisonInterval(int amplifier) {
		return Math.max(1, 25 >> Math.max(0, amplifier));
	}

	/** Ticks between the Wither effect's wounds at {@code amplifier}, as vanilla deals them: 1 damage each. */
	public static int witherInterval(int amplifier) {
		return Math.max(1, 40 >> Math.max(0, amplifier));
	}

	/** What burning, poison and withering with this long left would still deal: fire 1 a second, the others 1 per wound. */
	public static double lingering(int fireTicks, int poisonTicks, int poisonAmplifier, int witherTicks, int witherAmplifier) {
		double fire = Math.max(0, fireTicks) / 20.0;
		double poison = poisonTicks > 0 ? Math.floor((double) poisonTicks / poisonInterval(poisonAmplifier)) : 0;
		double wither = witherTicks > 0 ? Math.floor((double) witherTicks / witherInterval(witherAmplifier)) : 0;
		return fire + poison + wither;
	}

	/** Elapse's damage for what was still to come: half again as much, at least 3 and at most 16; none if nothing was. */
	public static double elapse(double lingering) {
		if (lingering <= 0) {
			return 0;
		}
		return Math.max(ELAPSE_MIN, Math.min(ELAPSE_MAX, lingering * ELAPSE_BONUS));
	}

	// ---- Which runes play a part (for their tooltips; the server sets reactions off by element).

	/** The mark a rune leaves on the foes it harms for a newer reaction: {@code shadowed} or {@code bleeding}. */
	public static final String SHADOWED = "shadowed";
	public static final String BLEEDING = "bleeding";

	/** Void's curses and darkness: they leave a foe shadowed, for Blight. */
	public static final Set<String> SHADOWS = Set.of("hex", "blind", "wither", "echolocate", "hush", "eclipse", "resonant_shriek", "blackflame",
		"entropy", "umbra", "malison");
	/** Blood's cuts: they leave a foe bleeding, for Rupture (Razorgale's blades too, which tear their own wounds open). */
	public static final Set<String> BLEEDS = Set.of("bleed", "rend", "cleave", "dismantle", "crimson_mist", "bonespur", "gash", "razorgale");

	/**
	 * Harmful runes of a triggering element that deal no damage of their own, so set nothing off:
	 * earth's holds, life's spores and sleep, arcane's curses, wind's throws and snatches, and time's
	 * stops. Prismatic Burst uses up the marks itself, so it never finds two left to unweave.
	 */
	private static final Set<String> QUIET = Set.of("root", "weigh", "shackle", "mire", "sporebloom", "reveal", "silence", "decree", "nullify",
		"prismatic_burst", "push", "launch", "dash", "levitate", "stasis", "timesteal", "drowse", "disarm");
	/**
	 * Runes that aren't harmful effects of a triggering element but deal its damage all the same:
	 * Heartstopper's and Thunderbird's shocks are storm, Tusk Charge tosses what it runs through, and
	 * Bramble's thorns are life; Thunderstep comes down as storm, Halo smites with arcane, and a Riposte
	 * answers with time.
	 */
	private static final Map<String, String> ALSO = Map.of("heartstopper", OVERLOAD, "thunderbird", OVERLOAD, "tusk_charge", FRACTURE,
		"bramble", BLIGHT, "thunderstep", OVERLOAD, "halo", UNWEAVE, "riposte", ELAPSE);

	/** The mark a rune leaves for a newer reaction ({@link #SHADOWED}, {@link #BLEEDING}), or null. */
	public static String marks(RuneDef rune) {
		if (rune == null || rune.family() != RuneFamily.EFFECT || !rune.id().startsWith("wildercord:")) {
			return null;
		}
		if (SHADOWS.contains(rune.path())) {
			return SHADOWED;
		}
		return BLEEDS.contains(rune.path()) ? BLEEDING : null;
	}

	/** The newer reaction a rune's damage can set off, or null. */
	public static String triggers(RuneDef rune) {
		if (rune == null || rune.family() != RuneFamily.EFFECT || !rune.id().startsWith("wildercord:")) {
			return null;
		}
		if (ALSO.containsKey(rune.path())) {
			return ALSO.get(rune.path());
		}
		if (rune.kind() != EffectKind.HARMFUL || QUIET.contains(rune.path())) {
			return null;
		}
		for (String reaction : NEWER) {
			if (TRIGGER.get(reaction).equals(rune.element())) {
				return reaction;
			}
		}
		return null;
	}

	/** A newer reaction's colour: its name when it goes off, and its lines in rune tooltips. */
	public static int color(String reaction) {
		return switch (reaction) {
			case OVERLOAD -> 0xFF9A3C;
			case FRACTURE -> 0xC8A070;
			case BLIGHT -> 0x8CC850;
			case UNWEAVE -> 0xE678DC;
			case RUPTURE -> 0xE0404F;
			case ELAPSE -> 0xF2D98A;
			default -> 0xFFFFFF;
		};
	}

	/** The reaction a mark leaves a foe open to. */
	public static String reactionFor(String mark) {
		return SHADOWED.equals(mark) ? BLIGHT : RUPTURE;
	}

	/** The lang key of a rune's tooltip line for the mark it leaves. */
	public static String markKey(String mark) {
		return "tooltip.wildercord.mark." + mark;
	}

	/** The lang key of a rune's tooltip line for the reaction its damage sets off. */
	public static String triggerKey(String reaction) {
		return "tooltip.wildercord.trigger." + reaction;
	}
}
