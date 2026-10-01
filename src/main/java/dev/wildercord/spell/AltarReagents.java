package dev.wildercord.spell;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Reagents at the Fusion Altar: one laid in the third rune socket while two effects fuse (with an amethyst
 * shard) or weave (with an amethyst block) steadies or strengthens what comes out, each in one way of its
 * element's:
 * <ul>
 *   <li><b>Tempered</b> (fire, Cinder Ash): the result keeps the higher of the two ranks put in, not the lower;</li>
 *   <li><b>Stilled</b> (frost, Everfrost Shard): half the XP levels, rounded up;</li>
 *   <li><b>Charged</b> (storm, Fulgurite Shard): a rank I result comes out rank II;</li>
 *   <li><b>Unbound</b> (wind, Bottled Gale): a signature pair makes its elements' fusion instead (Chill and Shock make Hail);</li>
 *   <li><b>Grounded</b> (earth, Geode Grit): the amethyst stays on the altar;</li>
 *   <li><b>Bountiful</b> (life, Wildbloom Petal): two of the result;</li>
 *   <li><b>Hollowed</b> (void, Hollow Dust): the lower-tier of the two runes stays on the altar (an extended weave keeps the rune it gained);</li>
 *   <li><b>Exalted</b> (arcane, Star Dust): one rank higher (up to III), for 3 more XP levels;</li>
 *   <li><b>Familiar</b> (time, Hourglass Sand): a named fusion you've made before costs no XP;</li>
 *   <li><b>Bloodbound</b> (blood, Sanguine Bead): up to 6 of the XP levels paid in health instead, one health (half a heart) each,
 *       never your last heart.</li>
 * </ul>
 * A reagent that would change nothing is refused (and the panel says why), so it's never spent for nothing.
 * Pure: the altar's screen shows the same plan the server checks before anything is used up.
 */
public final class AltarReagents {
	private AltarReagents() {}

	/** What a reagent does to a fusion. Its id is its lang key's end ({@code screen.wildercord.altar.reagent.<id>}). */
	public enum Effect {
		TEMPERED("fire"), STILLED("frost"), CHARGED("storm"), UNBOUND("wind"), GROUNDED("earth"), BOUNTIFUL("life"), HOLLOWED("void"),
		EXALTED("arcane"), FAMILIAR("time"), BLOODBOUND("blood");

		public final String element;

		Effect(String element) {
			this.element = element;
		}

		public String id() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** Exalted's extra price. */
	public static final int EXALT_XP = 3;
	/** The most XP levels Bloodbound pays in health. */
	public static final int BLOOD_LEVELS = 6;
	/** Health Bloodbound never takes a caster below: their last heart. */
	public static final float BLOOD_FLOOR = 2.0F;

	/** The reagent of an element. */
	public static Optional<Effect> of(String element) {
		for (Effect effect : Effect.values()) {
			if (effect.element.equals(element)) {
				return Optional.of(effect);
			}
		}
		return Optional.empty();
	}

	/**
	 * What the reagent makes of a plan.
	 *
	 * @param plan         the plan with the reagent's change (its rank, XP, result and recipe), or its refusal in {@code problem}
	 * @param copies       how many of the result come out
	 * @param keepCatalyst whether the amethyst stays on the altar
	 * @param keepSlot     the rune socket (0 to 2) whose rune stays on the altar, or -1
	 * @param health       health the caster pays (Bloodbound)
	 */
	public record Result(Effect effect, Fusions.Plan plan, int copies, boolean keepCatalyst, int keepSlot, int health) {
		public boolean ready() {
			return plan.ready();
		}
	}

	/** What the caster brings that a reagent may ask about: the fusions they've made (their Grimoire) and their health. */
	public record Context(Collection<String> grimoire, float health) {
		public static final Context NONE = new Context(List.of(), 20.0F);
	}

	/**
	 * Applies a reagent to the plan the altar would make without it.
	 *
	 * @param slots the three rune sockets (the reagent's own socket reads as empty)
	 */
	public static Result apply(Effect effect, Fusions.Plan base, List<Fusions.Slot> slots, Context context) {
		if (base.kind() != Fusions.Kind.COMBINE) {
			return refuse(effect, base, "A reagent only works in a fusion or a weave: two effects and amethyst, with the reagent in the third socket.");
		}
		if (!base.ready()) {
			return new Result(effect, base, 1, false, -1, 0);
		}
		int[] filled = filled(slots);
		Fusions.Slot a = slots.get(filled[0]);
		Fusions.Slot b = slots.get(filled[1]);
		boolean rankable = Ranks.rankable(base.result());
		Fusions.Plan p = base;
		return switch (effect) {
			case TEMPERED -> {
				int higher = Math.max(a.rank(), b.rank());
				if (!rankable || higher <= base.rank()) {
					yield refuse(effect, base, "Cinder Ash would change nothing: " + (rankable ? "both runes are the same rank." : base.result().name() + " has no ranks."));
				}
				yield new Result(effect, with(p, higher, p.xp(), p.result(), p.recipe()), 1, false, -1, 0);
			}
			case STILLED -> new Result(effect, with(p, p.rank(), (p.xp() + 1) / 2, p.result(), p.recipe()), 1, false, -1, 0);
			case CHARGED -> {
				if (!rankable || base.rank() != 1) {
					yield refuse(effect, base, "A Fulgurite Shard only charges a rank I result" + (rankable ? "." : ": " + base.result().name() + " has no ranks."));
				}
				yield new Result(effect, with(p, 2, p.xp(), p.result(), p.recipe()), 1, false, -1, 0);
			}
			case UNBOUND -> {
				if (!(base.recipe() instanceof Fusions.Signature signature) || signature.overrides().isEmpty()) {
					yield refuse(effect, base, "A Bottled Gale only unbinds a signature pair, into its elements' own fusion.");
				}
				Fusions.Recipe plain = signature.overrides().get();
				int kept = Ranks.rankable(plain.result()) ? Math.max(1, Math.min(a.rank(), b.rank())) : 1;
				yield new Result(effect, with(p, kept, p.xp(), plain.result(), plain), 1, false, -1, 0);
			}
			case GROUNDED -> new Result(effect, p, 1, true, -1, 0);
			case BOUNTIFUL -> new Result(effect, p, 2, false, -1, 0);
			case HOLLOWED -> new Result(effect, p, 1, false, lowerTier(slots, filled), 0);
			case EXALTED -> {
				if (!rankable || base.rank() >= Ranks.MAX) {
					yield refuse(effect, base, "Star Dust can't exalt it: " + (rankable ? "it's already at its highest rank." : base.result().name() + " has no ranks."));
				}
				yield new Result(effect, with(p, base.rank() + 1, p.xp() + EXALT_XP, p.result(), p.recipe()), 1, false, -1, 0);
			}
			case FAMILIAR -> {
				if (base.recipe() == null || !context.grimoire().contains(base.recipe().key())) {
					yield refuse(effect, base, "Hourglass Sand only helps with a named fusion you've made before.");
				}
				yield new Result(effect, with(p, p.rank(), 0, p.result(), p.recipe()), 1, false, -1, 0);
			}
			case BLOODBOUND -> {
				int paid = Math.min(BLOOD_LEVELS, base.xp());
				if (context.health() - paid < BLOOD_FLOOR) {
					yield refuse(effect, base, "A Sanguine Bead would take your last heart: you need more than " + Math.round(BLOOD_FLOOR + paid) + " health.");
				}
				yield new Result(effect, with(p, p.rank(), p.xp() - paid, p.result(), p.recipe()), 1, false, -1, paid);
			}
		};
	}

	private static Result refuse(Effect effect, Fusions.Plan base, String why) {
		return new Result(effect, new Fusions.Plan(base.kind(), null, 0, 0, why, base.recipe()), 1, false, -1, 0);
	}

	private static Fusions.Plan with(Fusions.Plan p, int rank, int xp, RuneDef result, Fusions.Fusion recipe) {
		return new Fusions.Plan(p.kind(), result, Ranks.clamp(rank), Math.max(0, xp), null, recipe);
	}

	/** The two filled sockets' indexes, in order. */
	private static int[] filled(List<Fusions.Slot> slots) {
		int[] out = new int[2];
		int n = 0;
		for (int i = 0; i < slots.size() && n < 2; i++) {
			if (!slots.get(i).empty()) {
				out[n++] = i;
			}
		}
		return out;
	}

	/** The socket whose rune Hollow Dust keeps: the lower-tier one (a lone rune over a weave it extends; the second on a tie). */
	private static int lowerTier(List<Fusions.Slot> slots, int[] filled) {
		RuneDef a = slots.get(filled[0]).rune();
		RuneDef b = slots.get(filled[1]).rune();
		boolean wovenA = WovenRunes.isWoven(a);
		boolean wovenB = WovenRunes.isWoven(b);
		if (wovenA != wovenB) {
			return wovenA ? filled[1] : filled[0];
		}
		return a.tier() < b.tier() ? filled[0] : filled[1];
	}
}
