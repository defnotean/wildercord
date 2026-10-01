package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * The traits a mastered spell can take, the pure part. At ranks II, III, IV and V its caster chooses one of three
 * offered traits. Offers are drawn from this catalogue, filtered by what the spell is made of (its elements, its
 * shapes, whether it harms or helps) and weighted by how it has been used: a fire spell cast often in the rain may be
 * offered Undying Flame, one cast at night a shadowy trait. A trait that asks for a circumstance is only offered once
 * the spell has been used in it often enough.
 *
 * <p>Traits are modest and flavourful: a little cheaper, a little quicker, a chance to leap to one more foe, a small
 * extra effect, a sound or a look. What each does is applied by {@code cast.Mastery} through the ordinary cast, so the
 * spell-defence caps hold against players, and {@link MasteryRules} holds them together so they never stack into a
 * one-shot. Each trait is one line here (the asset generator reads its name and description for the language
 * file).</p>
 *
 * <p>Other parts of the mod and add-ons may add traits of their own ({@link #register}) and offer them
 * ({@code api.SpellMasteryApi}).</p>
 */
public final class MasteryTraits {
	private MasteryTraits() {}

	/** What a trait does: {@code cast.Mastery} reads these. */
	public enum Hook {
		/** Its price times {@code amount} (in {@code param}'s place only, when it has one: nether, end, water, underground). */
		COST,
		/** Its cooldown times {@code amount}. */
		COOLDOWN,
		/** It charges {@code amount} times as fast. */
		CHARGE,
		/** Its bolts, beams and aimed shapes reach {@code amount} times as far. */
		RANGE,
		/** Its hits deal {@code amount} times the damage, when {@code param}'s condition holds ("" always). */
		DAMAGE,
		/** A spell that only helps: its effects {@code amount} times as strong. */
		HEALING,
		/** The caster drinks {@code amount} of the damage it deals. */
		LEECH,
		/** A kill gives back {@code amount} mana. */
		MANA_ON_KILL,
		/** A foe it strikes is touched by {@code param} (slow, glow, ignite, push, soak, shadow, bleed). */
		ON_STRIKE,
		/** A hit may leap to one more foe nearby, at a share of its damage. */
		CHAIN,
		/** An ally it helps is given {@code param} (regeneration, speed, resistance, night_vision, cleanse). */
		ALLY_EFFECT,
		/** Helping an ally gives back {@code amount} mana. */
		ALLY_MANA,
		/** After it moves you, you drift down softly for a few seconds. */
		FEATHER,
		/** Its fire burns at full strength on the wet and in the rain. */
		WET_FIRE,
		/** Cast in danger, {@code amount} of its price comes back. */
		SECOND_WIND,
		/** A change to how it looks or sounds ({@code param}: bell, stars, hue, embers, frost, petals). */
		COSMETIC,
		/** It leaves a residue of its magic where it lands (see {@code api.SpellMasteryApi.ResidueSink}). */
		RESIDUE
	}

	/** Which spells may take a trait. */
	public enum Kind {
		ANY, HARMFUL, HELPFUL, MOVEMENT, WORLD
	}

	/** The circumstances a spell's casts are counted in (see {@code cast.Mastery}). More can be added through the API. */
	public static final List<String> CIRCUMSTANCES = List.of("rain", "thunder", "night", "day", "underground", "deep", "water", "nether", "end",
		"low_health", "allies", "undead", "arthropod", "boss", "dungeon", "crowd", "cold", "hot", "airborne");

	/** The share of a spell's counted casts a circumstance needs before traits asking for it are offered. */
	public static final double MIN_SHARE = 0.15;
	/** ... and at least this many casts in it. */
	public static final int MIN_COUNT = 6;

	/**
	 * One trait.
	 *
	 * @param id           its id ({@code namespace:path} for one added from outside)
	 * @param name         its name, in English (the language file has it too)
	 * @param desc         what it does, in plain words
	 * @param hook         what it does, for the code
	 * @param amount       its number (a factor, a share, mana...)
	 * @param param        a detail of the hook (a condition, an effect), or ""
	 * @param kind         which spells may take it
	 * @param elements     the elements a spell needs one of (empty: any)
	 * @param shapes       the shape categories a spell needs one of (empty: any)
	 * @param circumstance the circumstance a spell must have been used in, or "" for none
	 */
	public record Trait(String id, String name, String desc, Hook hook, double amount, String param, Kind kind, Set<String> elements,
			Set<String> shapes, String circumstance) {
		public Trait {
			elements = Set.copyOf(elements);
			shapes = Set.copyOf(shapes);
		}

		public Trait harmful() {
			return new Trait(id, name, desc, hook, amount, param, Kind.HARMFUL, elements, shapes, circumstance);
		}

		public Trait helpful() {
			return new Trait(id, name, desc, hook, amount, param, Kind.HELPFUL, elements, shapes, circumstance);
		}

		public Trait moving() {
			return new Trait(id, name, desc, hook, amount, param, Kind.MOVEMENT, elements, shapes, circumstance);
		}

		public Trait working() {
			return new Trait(id, name, desc, hook, amount, param, Kind.WORLD, elements, shapes, circumstance);
		}

		public Trait elements(String... elements) {
			return new Trait(id, name, desc, hook, amount, param, kind, Set.of(elements), shapes, circumstance);
		}

		public Trait shapes(String... shapes) {
			return new Trait(id, name, desc, hook, amount, param, kind, elements, Set.of(shapes), circumstance);
		}

		public Trait when(String circumstance) {
			return new Trait(id, name, desc, hook, amount, param, kind, elements, shapes, circumstance);
		}

		public Trait param(String param) {
			return new Trait(id, name, desc, hook, amount, param, kind, elements, shapes, circumstance);
		}

		/** Whether a spell made like {@code spell} may take this trait. */
		public boolean fits(Profile spell) {
			boolean kindFits = switch (kind) {
				case ANY -> true;
				case HARMFUL -> spell.harmful();
				case HELPFUL -> spell.helpful() && !spell.harmful();
				case MOVEMENT -> spell.movement();
				case WORLD -> spell.world();
			};
			if (!kindFits) {
				return false;
			}
			if (!elements.isEmpty() && elements.stream().noneMatch(spell.elements()::contains)) {
				return false;
			}
			return shapes.isEmpty() || shapes.stream().anyMatch(spell.shapes()::contains);
		}
	}

	private static Trait trait(String id, String name, String desc, Hook hook, double amount) {
		return new Trait(id, name, desc, hook, amount, "", Kind.ANY, Set.of(), Set.of(), "");
	}

	// ------------------------------------------------------------------ the catalogue (one line each: tools/generate_assets.py reads them)

	// Any spell.
	public static final Trait THRIFTY = trait("thrifty", "Thrifty Weave", "Costs 10% less mana.", Hook.COST, 0.9);
	public static final Trait QUICK_RETURN = trait("quick_return", "Quick Return", "Its cooldown is 10% shorter.", Hook.COOLDOWN, 0.9);
	public static final Trait READY_BREATH = trait("ready_breath", "Ready Breath", "Charges 15% faster when you hold it.", Hook.CHARGE, 1.15);
	public static final Trait BELLSONG = trait("bellsong", "Bellsong", "A clear bell rings out each time you cast it.", Hook.COSMETIC, 1).param("bell");
	public static final Trait STARLIT = trait("starlit", "Starlit Circle", "Specks of starlight drift up from its circle.", Hook.COSMETIC, 1).param("stars");
	public static final Trait DEEP_HUE = trait("deep_hue", "Deep Hue", "Its circle burns in a deeper, richer colour.", Hook.COSMETIC, 1).param("hue");
	public static final Trait LINGERING_MARK = trait("lingering_mark", "Lingering Mark", "Leaves a faint trace of its magic where it lands.", Hook.RESIDUE, 1);
	public static final Trait SECOND_WIND = trait("second_wind", "Second Wind", "Cast in danger, a third of its mana comes back.", Hook.SECOND_WIND, 0.33).when("low_health");
	public static final Trait FAR_REACH = trait("far_reach", "Far Reach", "Its bolts, beams and aimed shapes reach 20% farther.", Hook.RANGE, 1.2).shapes("projectile", "direct", "area", "lingering");
	public static final Trait EMBERBORN = trait("emberborn", "Emberborn", "In the Nether it costs 15% less mana.", Hook.COST, 0.85).param("nether").when("nether");
	public static final Trait STARBORN = trait("starborn", "Starborn", "In the End it costs 15% less mana.", Hook.COST, 0.85).param("end").when("end");
	public static final Trait TIDESWORN = trait("tidesworn", "Tidesworn", "In water or rain it costs 15% less mana.", Hook.COST, 0.85).param("water").when("rain");
	public static final Trait DEEP_DELVER = trait("deep_delver", "Deep Delver", "Underground it costs 15% less mana.", Hook.COST, 0.85).param("underground").when("underground");

	// Spells that harm.
	public static final Trait KEEN = trait("keen", "Keen Edge", "Deals 6% more damage.", Hook.DAMAGE, 1.06).harmful();
	public static final Trait LEAPING_SPARK = trait("leaping_spark", "Leaping Spark", "A hit has a 15% chance to leap to one more foe nearby, at 40% strength.", Hook.CHAIN, 0.15).harmful();
	public static final Trait FINAL_WORD = trait("final_word", "Final Word", "Deals 12% more damage to foes below a third of their health.", Hook.DAMAGE, 1.12).param("low_target").harmful();
	public static final Trait OPENING_STRIKE = trait("opening_strike", "Opening Strike", "Deals 10% more damage to foes at full health.", Hook.DAMAGE, 1.1).param("full_target").harmful();
	public static final Trait GRAVE_WARD = trait("grave_ward", "Grave Ward", "Deals 15% more damage to the undead.", Hook.DAMAGE, 1.15).param("undead").harmful().when("undead");
	public static final Trait SPIDERBANE = trait("spiderbane", "Spiderbane", "Deals 15% more damage to spiders, bees, silverfish and their kin.", Hook.DAMAGE, 1.15).param("arthropod").harmful().when("arthropod");
	public static final Trait GIANTSBANE = trait("giantsbane", "Giantsbane", "Deals 15% more damage to bosses.", Hook.DAMAGE, 1.15).param("boss").harmful().when("boss");
	public static final Trait ASHEN_IRE = trait("ashen_ire", "Ashen Ire", "Deals 12% more damage in the Nether.", Hook.DAMAGE, 1.12).param("nether").harmful().when("nether");
	public static final Trait VOIDWALKER = trait("voidwalker", "Voidwalker's Ire", "Deals 12% more damage in the End.", Hook.DAMAGE, 1.12).param("end").harmful().when("end");
	public static final Trait CROWDBREAKER = trait("crowdbreaker", "Crowdbreaker", "Deals 10% more damage when four or more foes are near.", Hook.DAMAGE, 1.1).param("crowd").harmful().when("crowd");
	public static final Trait LAST_STAND = trait("last_stand", "Last Stand", "Deals 15% more damage while you are below a third of your health.", Hook.DAMAGE, 1.15).param("low_health").harmful().when("low_health");
	public static final Trait NIGHTSHADE = trait("nightshade", "Nightshade", "Deals 10% more damage at night.", Hook.DAMAGE, 1.1).param("night").harmful().when("night");
	public static final Trait SUNLIT_EDGE = trait("sunlit_edge", "Sunlit Edge", "Deals 10% more damage by day under the open sky.", Hook.DAMAGE, 1.1).param("day").harmful().when("day");
	public static final Trait BEDROCK_WILL = trait("bedrock_will", "Bedrock Will", "Deals 10% more damage underground.", Hook.DAMAGE, 1.1).param("underground").harmful().when("underground");
	public static final Trait THUNDERHEAD = trait("thunderhead", "Thunderhead", "Deals 10% more damage in rain or a thunderstorm.", Hook.DAMAGE, 1.1).param("rain").harmful().elements("storm", "wind", "frost").when("rain");
	public static final Trait SOUL_SIP = trait("soul_sip", "Soul Sip", "Heals you for 5% of the damage it deals (at most two hearts a cast).", Hook.LEECH, 0.05).harmful();
	public static final Trait MANA_HARVEST = trait("mana_harvest", "Mana Harvest", "Each foe it slays gives back 4 mana (at most 12 a cast).", Hook.MANA_ON_KILL, 4).harmful();
	public static final Trait STAGGERING = trait("staggering", "Staggering Blow", "Foes it strikes are slowed for a second.", Hook.ON_STRIKE, 1).param("slow").harmful();
	public static final Trait TELLTALE = trait("telltale", "Telltale Light", "Foes it strikes glow for three seconds, even through walls.", Hook.ON_STRIKE, 1).param("glow").harmful();

	// A harming spell of an element.
	public static final Trait UNDYING_FLAME = trait("undying_flame", "Undying Flame", "Its fire burns at full strength in the rain and on the wet.", Hook.WET_FIRE, 1).harmful().elements("fire").when("rain");
	public static final Trait KINDLING = trait("kindling", "Kindling", "Foes it strikes catch fire for two seconds.", Hook.ON_STRIKE, 1).param("ignite").harmful().elements("fire");
	public static final Trait EMBER_SCRIPT = trait("ember_script", "Ember Script", "Embers drift from its circle as you cast.", Hook.COSMETIC, 1).param("embers").elements("fire");
	public static final Trait RIME = trait("rime", "Rime", "Foes it strikes are slowed for two seconds.", Hook.ON_STRIKE, 2).param("slow").harmful().elements("frost").when("cold");
	public static final Trait FROSTWORK = trait("frostwork", "Frostwork", "Snowflakes of light fall from its circle.", Hook.COSMETIC, 1).param("frost").elements("frost");
	public static final Trait TIDECALLER = trait("tidecaller", "Tidecaller", "Foes it strikes are left Soaked, ready to conduct a storm.", Hook.ON_STRIKE, 1).param("soak").harmful().elements("storm", "frost").when("water");
	public static final Trait GALE_PUSH = trait("gale_push", "Gale's Push", "Its hits push foes back a little.", Hook.ON_STRIKE, 1).param("push").harmful().elements("wind", "earth");
	public static final Trait BLOODLETTING = trait("bloodletting", "Bloodletting", "Foes it strikes are left Bleeding, ready for a gust to rupture.", Hook.ON_STRIKE, 1).param("bleed").harmful().elements("blood");
	public static final Trait UMBRAL_MARK = trait("umbral_mark", "Umbral Mark", "Foes it strikes are left Shadowed, ready for life magic to blight.", Hook.ON_STRIKE, 1).param("shadow").harmful().elements("void", "arcane", "time");
	public static final Trait STATIC_ARC = trait("static_arc", "Static Arc", "A hit has a 15% chance to leap to one more foe nearby, at 40% strength.", Hook.CHAIN, 0.15).harmful().elements("storm").when("thunder");

	// Spells that only help.
	public static final Trait WARM_HANDS = trait("warm_hands", "Warm Hands", "Its healing and warding are 10% stronger.", Hook.HEALING, 1.1).helpful();
	public static final Trait MENDING_GLOW = trait("mending_glow", "Mending Glow", "Allies it helps also regenerate for three seconds.", Hook.ALLY_EFFECT, 60).param("regeneration").helpful();
	public static final Trait SWIFT_MERCY = trait("swift_mercy", "Swift Mercy", "Allies it helps are quickened for four seconds.", Hook.ALLY_EFFECT, 80).param("speed").helpful();
	public static final Trait STEADFAST = trait("steadfast", "Steadfast Ward", "Allies it helps resist harm for three seconds.", Hook.ALLY_EFFECT, 60).param("resistance").helpful().when("allies");
	public static final Trait KIND_LIGHT = trait("kind_light", "Kind Light", "Helping an ally gives you back 3 mana (at most 6 a cast).", Hook.ALLY_MANA, 3).helpful().when("allies");
	public static final Trait LANTERN_HEART = trait("lantern_heart", "Lantern Heart", "Those it helps can see in the dark for twenty seconds.", Hook.ALLY_EFFECT, 400).param("night_vision").helpful().when("underground");
	public static final Trait CLEANSING = trait("cleansing", "Cleansing Touch", "It ends poison and withering on those it helps.", Hook.ALLY_EFFECT, 0).param("cleanse").helpful();
	public static final Trait PETALFALL = trait("petalfall", "Petalfall", "Petals of light fall from its circle.", Hook.COSMETIC, 1).param("petals").elements("life");

	// Spells that move you, or work the world.
	public static final Trait FEATHERSTEP = trait("featherstep", "Featherstep", "After it moves you, you drift down softly for three seconds.", Hook.FEATHER, 60).moving();
	public static final Trait NIMBLE = trait("nimble", "Nimble", "Its cooldown is 15% shorter.", Hook.COOLDOWN, 0.85).moving();
	public static final Trait STEADY_HANDS = trait("steady_hands", "Steady Hands", "Costs 15% less mana.", Hook.COST, 0.85).working();

	/** Every built-in trait, in the order above, then any added from outside. */
	private static final Map<String, Trait> ALL = new LinkedHashMap<>();

	static {
		for (Trait t : List.of(THRIFTY, QUICK_RETURN, READY_BREATH, BELLSONG, STARLIT, DEEP_HUE, LINGERING_MARK, SECOND_WIND, FAR_REACH, EMBERBORN,
				STARBORN, TIDESWORN, DEEP_DELVER, KEEN, LEAPING_SPARK, FINAL_WORD, OPENING_STRIKE, GRAVE_WARD, SPIDERBANE, GIANTSBANE, ASHEN_IRE,
				VOIDWALKER, CROWDBREAKER, LAST_STAND, NIGHTSHADE, SUNLIT_EDGE, BEDROCK_WILL, THUNDERHEAD, SOUL_SIP, MANA_HARVEST, STAGGERING, TELLTALE,
				UNDYING_FLAME, KINDLING, EMBER_SCRIPT, RIME, FROSTWORK, TIDECALLER, GALE_PUSH, BLOODLETTING, UMBRAL_MARK, STATIC_ARC, WARM_HANDS,
				MENDING_GLOW, SWIFT_MERCY, STEADFAST, KIND_LIGHT, LANTERN_HEART, CLEANSING, PETALFALL, FEATHERSTEP, NIMBLE, STEADY_HANDS)) {
			ALL.put(t.id(), t);
		}
	}

	/** Every trait, built-in ones first. */
	public static synchronized Collection<Trait> all() {
		return List.copyOf(ALL.values());
	}

	/** The built-in traits only. */
	public static List<Trait> builtIn() {
		return all().stream().filter(t -> !t.id().contains(":")).toList();
	}

	public static synchronized Optional<Trait> get(String id) {
		return Optional.ofNullable(ALL.get(id));
	}

	/**
	 * Adds a trait from outside the catalogue (a world's rune quirk, a place of power, an add-on). Its id must carry a
	 * namespace ({@code mymod:echoing}) so it can never take a built-in one's place.
	 *
	 * @throws IllegalArgumentException for an id without a namespace, or one already taken
	 */
	public static synchronized void register(Trait trait) {
		if (!trait.id().contains(":")) {
			throw new IllegalArgumentException("A trait added from outside needs a namespaced id: " + trait.id());
		}
		if (ALL.containsKey(trait.id())) {
			throw new IllegalArgumentException("Trait already registered: " + trait.id());
		}
		ALL.put(trait.id(), trait);
	}

	/** Makes a trait for {@link #register}: any spell may take it until {@link Trait#harmful()} and the like narrow it. */
	public static Trait custom(String id, String name, String desc, Hook hook, double amount) {
		return trait(id, name, desc, hook, amount);
	}

	// ------------------------------------------------------------------ what a spell is made of

	/**
	 * What a spell is made of, for choosing its traits.
	 *
	 * @param elements the elements of its effects
	 * @param shapes   the categories of its shapes (personal, direct, projectile, area, lingering)
	 * @param harmful  whether any effect harms
	 * @param helpful  whether any effect helps
	 * @param movement whether any effect moves its caster
	 * @param world    whether any effect works blocks or places
	 */
	public record Profile(Set<String> elements, Set<String> shapes, boolean harmful, boolean helpful, boolean movement, boolean world) {
		public Profile {
			elements = Set.copyOf(elements);
			shapes = Set.copyOf(shapes);
		}

		public static Profile of(List<RuneDef> runes) {
			Set<String> elements = new HashSet<>();
			Set<String> shapes = new HashSet<>();
			boolean harmful = false;
			boolean helpful = false;
			boolean movement = false;
			boolean world = false;
			boolean shaped = false;
			for (RuneDef rune : Knots.flatten(runes)) {
				if (rune.family() == RuneFamily.SHAPE) {
					shapes.add(rune.category());
					shaped = true;
				} else if (rune.family() == RuneFamily.EFFECT) {
					for (RuneDef leaf : WovenRunes.isWoven(rune) ? WovenRunes.contents(rune) : List.of(rune)) {
						if (!leaf.element().isEmpty()) {
							elements.add(leaf.element());
						}
						switch (leaf.kind()) {
							case HARMFUL -> harmful = true;
							case HELPFUL -> helpful = true;
							case MOVEMENT -> movement = true;
							case WORLD -> world = true;
							default -> {}
						}
					}
				}
			}
			if (!shaped) {
				// A spell that starts with an effect has an unwritten Self.
				shapes.add("personal");
			}
			return new Profile(elements, shapes, harmful, helpful, movement, world);
		}
	}

	// ------------------------------------------------------------------ offers

	/** A trait offered from outside (a world's quirk, a residue), with how strongly: 1 is as likely as any other. */
	public record Weighted(String trait, double weight) {}

	/** The share of a spell's counted casts made in {@code circumstance}. */
	public static double share(Map<String, Integer> counters, int casts, String circumstance) {
		if (casts <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(1, counters.getOrDefault(circumstance, 0) / (double) casts));
	}

	/** Whether a spell has been used in {@code circumstance} often enough for traits that ask for it. */
	public static boolean usedIn(Map<String, Integer> counters, int casts, String circumstance) {
		return circumstance.isEmpty()
			|| counters.getOrDefault(circumstance, 0) >= MIN_COUNT && share(counters, casts, circumstance) >= MIN_SHARE;
	}

	/** How likely {@code trait} is to be offered to this spell, or 0 when it can't be. */
	public static double weight(Trait trait, Profile spell, Map<String, Integer> counters, int casts) {
		if (!trait.fits(spell) || !usedIn(counters, casts, trait.circumstance())) {
			return 0;
		}
		double w = 1.0;
		if (!trait.circumstance().isEmpty()) {
			// Earned by how the spell was used: the more of its casts were in that circumstance, the likelier.
			w += 4.0 * share(counters, casts, trait.circumstance());
		}
		if (!trait.elements().isEmpty()) {
			// An element's own traits are the flavourful ones.
			w += 1.0;
		}
		return w;
	}

	/**
	 * The traits offered for a rank: {@link MasteryRules#OFFER} different ones, drawn by weight, the same every time for
	 * the same spell, rank and re-roll.
	 *
	 * @param spell    what the spell is made of
	 * @param counters its circumstance counts
	 * @param casts    how many of its casts were counted
	 * @param seed     the spell's own seed (see {@link MasterySigil#seed})
	 * @param slot     the trait slot being offered (0 for rank II)
	 * @param salt     0, or another number for a re-roll
	 * @param exclude  traits it can't be offered (those it already has, the offer being re-rolled)
	 * @param extra    traits offered from outside, each already registered
	 */
	public static List<String> offer(Profile spell, Map<String, Integer> counters, int casts, long seed, int slot, int salt, Set<String> exclude,
			List<Weighted> extra) {
		Map<String, Double> pool = new LinkedHashMap<>();
		for (Trait trait : all()) {
			if (trait.id().contains(":") || exclude.contains(trait.id())) {
				continue;
			}
			double w = weight(trait, spell, counters, casts);
			if (w > 0) {
				pool.put(trait.id(), w);
			}
		}
		for (Weighted offered : extra) {
			Optional<Trait> trait = get(offered.trait());
			if (trait.isPresent() && !exclude.contains(offered.trait()) && offered.weight() > 0 && trait.get().fits(spell)) {
				pool.merge(offered.trait(), offered.weight(), Double::sum);
			}
		}
		SplittableRandom random = new SplittableRandom(seed ^ (0x9E3779B97F4A7C15L * (slot + 1)) ^ (0xC2B2AE3D27D4EB4FL * (salt + 1)));
		List<String> picked = new ArrayList<>();
		while (picked.size() < MasteryRules.OFFER && !pool.isEmpty()) {
			double total = pool.values().stream().mapToDouble(Double::doubleValue).sum();
			double roll = random.nextDouble() * total;
			String chosen = null;
			for (Map.Entry<String, Double> e : pool.entrySet()) {
				roll -= e.getValue();
				if (roll < 0) {
					chosen = e.getKey();
					break;
				}
			}
			if (chosen == null) {
				chosen = pool.keySet().iterator().next();
			}
			picked.add(chosen);
			pool.remove(chosen);
		}
		return List.copyOf(picked);
	}

	/** The traits among {@code ids} that do {@code hook}. */
	public static List<Trait> withHook(Collection<String> ids, Hook hook) {
		List<Trait> out = new ArrayList<>();
		for (String id : ids) {
			get(id).filter(t -> t.hook() == hook).ifPresent(out::add);
		}
		return out;
	}

	/** The product of {@code hook}'s amounts among {@code ids} (1 when none), held between {@code min} and {@code max}. */
	public static double product(Collection<String> ids, Hook hook, double min, double max) {
		double p = 1.0;
		for (Trait t : withHook(ids, hook)) {
			p *= t.amount();
		}
		return Math.max(min, Math.min(max, p));
	}
}
