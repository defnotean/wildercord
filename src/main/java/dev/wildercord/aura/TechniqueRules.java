package dev.wildercord.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A swordsman's own techniques, the pure part: the parts a technique is written from, what a written technique does (its
 * {@link Profile}), what it's worth in the arts' balance model and so what it costs and how long it rests, the strings it may be
 * given, its name, its ranks, and where each part is found. The runtime is {@code aura.Techniques} (what a swordsman knows and has
 * written) and {@code aura.arts.TechniqueArts} (a technique performed); the writing page is the client's.
 *
 * <h2>Three parts</h2>
 * <p>A technique is a <b>stroke</b> (the motion: a thrust, a rising cut, a falling cut, a sweep, a spin or a draw), a <b>release</b>
 * (how it leaves the blade: on the blade, thrown as a wave, a burst all round, or an afterimage that strikes after you) and an
 * <b>intent</b> (what it means to do: pierce, sunder, bind, echo, ward, rally, or infuse, the method's own element deepened). The
 * stroke gives the shape and the weight of the blow; the release moves the shape; the intent adds its effect and costs a little of the
 * blow. Every technique carries its swordsman's method too ({@link Flavour}): an Ember technique sets alight, a Rime one chills, a
 * Thunder one throws a spark, and so on.</p>
 *
 * <h2>Priced like an art</h2>
 * <p>What a technique does is weighed with the very model the fifty arts were balanced with ({@link ArtRules#power}), and its price
 * and rest follow from that: {@link #AURA_PER_W} aura and {@link #TICKS_PER_W} ticks for each W it's worth, the arts' own rate across
 * the First to the Fourth Art (the Final Art stands apart). The string it's given moves the price a little ({@link #effort}): one that
 * asks more of the hand costs a little less. Every combination is worth between about one and two and a quarter W, among the arts of
 * the first four slots and never near the Final Art, and its worth for its price lies inside the band the arts' own do
 * ({@code TechniqueRulesTest}), at every rank: a technique of your own is another answer, never a better or a worse one. What it gives
 * that an art doesn't is the choosing: its shape, its string, its name, and a technique that grows as it's used.</p>
 *
 * <h2>Ranks</h2>
 * <p>A technique ranks up as it lands on real foes, as a spell grows with mastery: {@link #RANKS} Raw, Honed, Tempered, Keen and
 * Peerless, at {@link #THRESHOLDS}. A rank sharpens it a little ({@link #RANK_STRENGTH}, never more than a twentieth either side of
 * Tempered), Tempered deepens its intent, Honed and Keen each let its writer choose how it's tempered ({@link #SWIFT} or {@link #HEAVY})
 * and its edge ({@link #LONG} or {@link #BROAD}), choices that trade one strength for another and are priced as they change it, and
 * at Peerless its name rings out and its parts can be set down on scrolls for another swordsman.</p>
 */
public final class TechniqueRules {
	private TechniqueRules() {}

	// ================================================================== the parts

	/** The three kinds of part. */
	public enum Family {
		STROKE("stroke"),
		RELEASE("release"),
		INTENT("intent");

		public final String id;

		Family(String id) {
			this.id = id;
		}
	}

	/** What a stroke reaches: a line ahead, a cone in front, or a ring all round. */
	public enum Shape {
		LINE,
		CONE,
		RING
	}

	public static final String THRUST = "thrust";
	public static final String RISING = "rising_cut";
	public static final String FALLING = "falling_cut";
	public static final String SWEEP = "sweep";
	public static final String SPIN = "spin";
	public static final String DRAW = "draw";

	public static final String ON_BLADE = "on_the_blade";
	public static final String WAVE = "wave";
	public static final String BURST = "burst";
	public static final String AFTERIMAGE = "afterimage";

	public static final String PIERCE = "pierce";
	public static final String SUNDER = "sunder";
	public static final String BIND = "bind";
	public static final String ECHO = "echo";
	public static final String WARD = "ward";
	public static final String RALLY = "rally";
	public static final String INFUSE = "infuse";

	/**
	 * A stroke: the motion of the blade, which gives a technique its shape and the weight of its blow.
	 *
	 * @param reach   how far it reaches from the swordsman (blocks): a line's length, a cone's, a ring's radius
	 * @param width   a cone's width (degrees), a line's half-width (blocks); 360 for a ring
	 * @param factor  what its main foe takes, in weapon damage
	 * @param targets how many foes it strikes at most
	 * @param others  the share of {@code factor} the foes after the first take
	 * @param fair    how many other foes it fairly reaches (the balance model's count)
	 * @param lift    how hard it throws its foes up (0 for none)
	 * @param stance  how much harder than an art's it wears a stance (1 the same)
	 * @param trail   its blade trail
	 */
	public record Stroke(String id, Shape shape, double reach, double width, double factor, int targets, double others, double fair, double lift,
			double stance, AuraFxRules.Stroke trail) {}

	/**
	 * A release: how a stroke leaves the blade.
	 *
	 * @param factor  what it leaves of the stroke's blow (the strike at once, for an afterimage)
	 * @param reach   how much further it carries the stroke (blocks: a wave's flight)
	 * @param fair    how many more foes it fairly reaches (a factor)
	 * @param targets how many more it may strike (a factor)
	 * @param later   an afterimage's strike after it, as a share of the stroke's blow (0 for none)
	 * @param delay   ticks before that strike
	 */
	public record Release(String id, double factor, double reach, double fair, double targets, double later, int delay) {}

	/**
	 * An intent: what a technique means to do to its foes, at a little of its blow. Built in: pierce, sunder, bind, echo, ward, rally and
	 * infuse; an add-on's own goes in through {@link #registerIntent} (its effect is the add-on's, through {@code AuraApi}).
	 *
	 * @param factor     what it leaves of the blow
	 * @param targets    how many more foes it may strike
	 * @param reach      how much further it reaches (blocks)
	 * @param fair       how many more it fairly reaches (a factor)
	 * @param stance     how much harder it wears a stance (a factor)
	 * @param bind       how long it roots each foe struck (ticks; a player held to the arts' cap)
	 * @param echo       its strike again a moment later, as a share of the blow (0 for none)
	 * @param ward       how much less its swordsman takes from foes after (a share), and for how long ({@code wardTicks})
	 * @param rally      how much less its swordsman and their allies take from foes after (a share), for {@code rallyTicks}, within
	 *                   {@code rallyRadius}, and the momentum each allied swordsman near builds ({@code rallyMomentum})
	 * @param flavour    how strongly the method's element comes through (a factor; 1 as usual)
	 * @param control    seconds of control an add-on's own effect is worth in the balance model
	 * @param area       what an add-on's own effect is worth to others in the balance model (W)
	 */
	public record Intent(String id, double factor, int targets, double reach, double fair, double stance, int bind, double echo, double ward,
			int wardTicks, double rally, int rallyTicks, double rallyRadius, double rallyMomentum, double flavour, double control, double area) {
		/** An add-on's intent: what it leaves of the blow, and what its own effect is worth (control seconds, W to others). */
		public static Intent own(String id, double factor, double control, double area) {
			return new Intent(id, factor, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, control, area);
		}
	}

	// ---------------------------------------------------------------- strokes

	/** A thrust: a straight lance ahead, following the look up and down, its first foe taking the most. */
	public static final Stroke THRUST_STROKE = new Stroke(THRUST, Shape.LINE, 5.5, 0.7, 1.0, 3, 0.7, 0.6, 0, 1.0, AuraFxRules.Stroke.THRUST);
	/** A rising cut: up across the foes in front, throwing them up. */
	public static final Stroke RISING_STROKE = new Stroke(RISING, Shape.CONE, 3.4, 100, 0.8, 4, 1.0, 0.6, 0.55, 1.0, AuraFxRules.Stroke.RISING);
	/** A falling cut: overhead and down, narrow and heavy, wearing a stance hard. */
	public static final Stroke FALLING_STROKE = new Stroke(FALLING, Shape.CONE, 3.6, 60, 1.12, 2, 0.6, 0.4, 0, 1.4, AuraFxRules.Stroke.FALLING);
	/** A sweep: wide and level round the front, many foes a little each. */
	public static final Stroke SWEEP_STROKE = new Stroke(SWEEP, Shape.CONE, 3.3, 170, 0.62, 6, 1.0, 1.2, 0, 1.0, AuraFxRules.Stroke.SWEEP);
	/** A spin: a whole turn, every foe round about. */
	public static final Stroke SPIN_STROKE = new Stroke(SPIN, Shape.RING, 3.0, 360, 0.58, 8, 1.0, 1.5, 0, 1.0, AuraFxRules.Stroke.SPIN);
	/** A draw: a fast level cut out of the sheathe, the lightest and cheapest stroke. */
	public static final Stroke DRAW_STROKE = new Stroke(DRAW, Shape.CONE, 3.8, 110, 0.72, 4, 1.0, 0.6, 0, 1.0, AuraFxRules.Stroke.DRAW);

	public static final List<Stroke> STROKES = List.of(THRUST_STROKE, RISING_STROKE, FALLING_STROKE, SWEEP_STROKE, SPIN_STROKE, DRAW_STROKE);

	// ---------------------------------------------------------------- releases

	/** On the blade: the stroke lands at once, where it reaches. */
	public static final Release BLADE_RELEASE = new Release(ON_BLADE, 1.0, 0, 1.0, 1.0, 0, 0);
	/** A wave: the stroke thrown on as a crescent of aura flying {@link #WAVE_LENGTH} blocks (a spin's as a ring racing out). */
	public static final Release WAVE_RELEASE = new Release(WAVE, 0.75, 0, 1.15, 1.25, 0, 0);
	/** A burst: the stroke's force breaking out all round its swordsman at once. */
	public static final Release BURST_RELEASE = new Release(BURST, 0.72, 0, 1.8, 1.5, 0, 0);
	/** An afterimage: a lighter stroke now, and an afterimage left standing where it was struck that strikes it again a moment later. */
	public static final Release AFTERIMAGE_RELEASE = new Release(AFTERIMAGE, 0.62, 0, 1.0, 1.0, 0.55, 12);

	public static final List<Release> RELEASES = List.of(BLADE_RELEASE, WAVE_RELEASE, BURST_RELEASE, AFTERIMAGE_RELEASE);

	/** How far a wave flies past its stroke's reach (blocks), and how fast (blocks a tick). A spin's ring races out {@link #WAVE_RING}. */
	public static final double WAVE_LENGTH = 7.0;
	public static final double WAVE_SPEED = 1.4;
	public static final double WAVE_RING = 5.0;
	/** A burst's radius: a ring's own reach and a block more, any other stroke's reach times this. */
	public static final double BURST_RADIUS = 0.85;
	/** How often an afterimage's strike finds its foes still there, and an echo its foe (the balance model's guess). */
	public static final double LATER_LANDS = 0.75;
	public static final double ECHO_LANDS = 0.85;

	// ---------------------------------------------------------------- intents

	/** Pierce: through whatever stands in the way, further and to more foes, a little lighter. */
	public static final Intent PIERCE_INTENT = new Intent(PIERCE, 0.92, 3, 1.5, 1.35, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
	/** Sunder: breaks a stance two and a half times as hard as an art, to open a foe for its finisher. */
	public static final Intent SUNDER_INTENT = new Intent(SUNDER, 0.88, 0, 0, 1, 2.5, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
	/** Bind: roots each foe struck where it stands. */
	public static final Intent BIND_INTENT = new Intent(BIND, 0.85, 0, 0, 1, 1, 24, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
	/** Echo: strikes again, on each foe it struck, a moment later. */
	public static final Intent ECHO_INTENT = new Intent(ECHO, 0.85, 0, 0, 1, 1, 0, 0.5, 0, 0, 0, 0, 0, 0, 1, 0, 0);
	/** Ward: its swordsman takes a fifth less from foes for a few seconds after. */
	public static final Intent WARD_INTENT = new Intent(WARD, 0.88, 0, 0, 1, 1, 0, 0, 0.2, 80, 0, 0, 0, 0, 1, 0, 0);
	/** Rally: its swordsman and their allies near take a tenth less from foes a while, and allied swordsmen build momentum. */
	public static final Intent RALLY_INTENT = new Intent(RALLY, 0.88, 0, 0, 1, 1, 0, 0, 0, 0, 0.1, 100, 8, 5, 1, 0, 0);
	/** Infuse: the method's own element twice as strong. */
	public static final Intent INFUSE_INTENT = new Intent(INFUSE, 0.95, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2.0, 0, 0);

	/** Each built-in intent deepened, as a technique reaches Tempered. */
	private static final Map<String, Intent> DEEP = Map.of(
		PIERCE, new Intent(PIERCE, 0.92, 4, 1.75, 1.35, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
		SUNDER, new Intent(SUNDER, 0.88, 0, 0, 1, 2.65, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
		BIND, new Intent(BIND, 0.85, 0, 0, 1, 1, 27, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0),
		ECHO, new Intent(ECHO, 0.85, 0, 0, 1, 1, 0, 0.54, 0, 0, 0, 0, 0, 0, 1, 0, 0),
		WARD, new Intent(WARD, 0.88, 0, 0, 1, 1, 0, 0, 0.2, 90, 0, 0, 0, 0, 1, 0, 0),
		RALLY, new Intent(RALLY, 0.88, 0, 0, 1, 1, 0, 0, 0, 0, 0.1, 110, 9, 5.5, 1, 0, 0),
		INFUSE, new Intent(INFUSE, 0.95, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2.15, 0, 0));

	public static final List<Intent> BUILT_IN_INTENTS = List.of(PIERCE_INTENT, SUNDER_INTENT, BIND_INTENT, ECHO_INTENT, WARD_INTENT, RALLY_INTENT,
		INFUSE_INTENT);

	/** How long an echo comes after its strike (ticks). */
	public static final int ECHO_DELAY = 10;
	/** How far from where it struck a foe may have gone and still be found by its echo (blocks past the reach). */
	public static final double ECHO_REACH = 2.5;

	private static final Map<String, Intent> INTENTS = Collections.synchronizedMap(new LinkedHashMap<>());

	static {
		for (Intent intent : BUILT_IN_INTENTS) {
			INTENTS.put(intent.id(), intent);
		}
	}

	/**
	 * Adds an intent of an add-on's own (its id namespaced: {@code mymod:frostbind}), or replaces one with its id. What it does is the
	 * add-on's ({@code AuraApi.registerTechniqueIntent}); here it's only weighed and priced. A built-in intent can't be replaced.
	 */
	public static void registerIntent(Intent intent) {
		if (intent == null || intent.id() == null || intent.id().isBlank() || DEEP.containsKey(intent.id())) {
			throw new IllegalArgumentException("an add-on's intent needs an id of its own");
		}
		INTENTS.put(intent.id(), intent);
	}

	public static Optional<Stroke> stroke(String id) {
		for (Stroke s : STROKES) {
			if (s.id().equals(id)) {
				return Optional.of(s);
			}
		}
		return Optional.empty();
	}

	public static Optional<Release> release(String id) {
		for (Release r : RELEASES) {
			if (r.id().equals(id)) {
				return Optional.of(r);
			}
		}
		return Optional.empty();
	}

	public static Optional<Intent> intent(String id) {
		return Optional.ofNullable(id == null ? null : INTENTS.get(id));
	}

	/** Every intent, built-in then an add-on's, in the order added. */
	public static List<Intent> intents() {
		synchronized (INTENTS) {
			return List.copyOf(INTENTS.values());
		}
	}

	/** Every part's id of {@code family}, in the order the writing page shows them. */
	public static List<String> parts(Family family) {
		return switch (family) {
			case STROKE -> STROKES.stream().map(Stroke::id).toList();
			case RELEASE -> RELEASES.stream().map(Release::id).toList();
			case INTENT -> intents().stream().map(Intent::id).toList();
		};
	}

	/** Every part's id, strokes, then releases, then intents. */
	public static List<String> allParts() {
		List<String> out = new ArrayList<>();
		for (Family f : Family.values()) {
			out.addAll(parts(f));
		}
		return out;
	}

	/** The family of part {@code id}, if it's a part at all. */
	public static Optional<Family> family(String id) {
		if (stroke(id).isPresent()) {
			return Optional.of(Family.STROKE);
		}
		if (release(id).isPresent()) {
			return Optional.of(Family.RELEASE);
		}
		return intent(id).isPresent() ? Optional.of(Family.INTENT) : Optional.empty();
	}

	/** Whether {@code id} is a part of {@code family}. */
	public static boolean is(String id, Family family) {
		return family(id).orElse(null) == family;
	}

	/** The language key of a part's name ({@code .desc} its line): {@code aura.wildercord.technique_part.<id>} (':' as '.'). */
	public static String nameKey(String partId) {
		return "aura.wildercord.technique_part." + partId.replace(':', '.');
	}

	// ================================================================== where parts come from

	/** The parts every blade knows once it reaches {@link #FROM}: the draw, the cut on the blade, and the method's own element. */
	public static final List<String> INNATE = List.of(DRAW, ON_BLADE, INFUSE);
	/** Techniques are written from Edge. */
	public static final int FROM = AuraRules.EDGE;

	/** The part each built-in Way lends while its Edge node is in force (see {@link #lent}). */
	public static final Map<String, String> WAY_PARTS = Map.of(WayRules.BLADE, PIERCE, WayRules.BULWARK, WARD, WayRules.SHADOWSTEP, AFTERIMAGE,
		WayRules.BANNER, RALLY);
	/** The parts only a Way gives (never found on a scroll or taught by a duelist). */
	public static final Set<String> WAY_ONLY = Set.of(WARD, RALLY);

	/** The Way's Edge node that lends {@code partId}, or empty when no built-in Way lends it. */
	public static Optional<String> lendingNode(String partId) {
		for (Map.Entry<String, String> e : WAY_PARTS.entrySet()) {
			if (e.getValue().equals(partId)) {
				return Optional.of(WayRules.node(e.getKey(), AuraRules.EDGE));
			}
		}
		return Optional.empty();
	}

	/** The Way that lends {@code partId} ("" for none). */
	public static String lendingWay(String partId) {
		for (Map.Entry<String, String> e : WAY_PARTS.entrySet()) {
			if (e.getValue().equals(partId)) {
				return e.getKey();
			}
		}
		return "";
	}

	/** Whether {@code partId} can be found on a technique scroll (or taught by a duelist): a built-in part that's neither innate nor a Way's alone. */
	public static boolean scrollable(String partId) {
		return family(partId).isPresent() && !INNATE.contains(partId) && !WAY_ONLY.contains(partId)
			&& (stroke(partId).isPresent() || release(partId).isPresent() || DEEP.containsKey(partId));
	}

	/** Every part a scroll can carry, in the writing page's order. */
	public static List<String> scrollParts() {
		return allParts().stream().filter(TechniqueRules::scrollable).toList();
	}

	/**
	 * What a duelist of each method shows the one who beats it: weights over the parts a scroll carries, its method's favourites three
	 * times as likely. A duelist teaches one part its challenger doesn't know yet.
	 */
	public static Map<String, Integer> duelistWeights(BreathingMethod.Flavour flavour) {
		List<String> favoured = switch (flavour) {
			case IGNITE -> List.of(FALLING, WAVE, SUNDER);
			case CHILL -> List.of(THRUST, BIND, ECHO);
			case SPARK -> List.of(THRUST, WAVE, PIERCE);
			case GALE -> List.of(SWEEP, WAVE, PIERCE);
			case STONE -> List.of(FALLING, BURST, SUNDER);
			case MEND -> List.of(SWEEP, AFTERIMAGE, BIND);
			case PULL -> List.of(SPIN, BURST, BIND);
			case STARLIT -> List.of(THRUST, BURST, ECHO);
			case HASTE -> List.of(RISING, AFTERIMAGE, ECHO);
			case LEECH -> List.of(RISING, SPIN, SUNDER);
			case NONE -> List.of();
		};
		Map<String, Integer> out = new LinkedHashMap<>();
		for (String part : scrollParts()) {
			out.put(part, favoured.contains(part) ? 3 : 1);
		}
		return out;
	}

	/**
	 * A part drawn by weight from {@code weights} that {@code known} doesn't hold, or empty when every one is known. {@code roll} is a
	 * random number from 0 (inclusive) to 1.
	 */
	public static Optional<String> draw(Map<String, Integer> weights, Set<String> known, double roll) {
		int total = 0;
		for (Map.Entry<String, Integer> e : weights.entrySet()) {
			if (!known.contains(e.getKey())) {
				total += Math.max(0, e.getValue());
			}
		}
		if (total <= 0) {
			return Optional.empty();
		}
		double pick = Math.max(0, Math.min(0.999999, roll)) * total;
		for (Map.Entry<String, Integer> e : weights.entrySet()) {
			if (known.contains(e.getKey())) {
				continue;
			}
			pick -= Math.max(0, e.getValue());
			if (pick < 0) {
				return Optional.of(e.getKey());
			}
		}
		return Optional.empty();
	}

	// ================================================================== the method's flavour

	/**
	 * What a method's element adds to every technique of its swordsman's (an intent of {@link #INFUSE} makes it twice as strong, more
	 * at Tempered). Each is worth about the same in the balance model (around a seventh of a W), a different answer each.
	 *
	 * @param ignite  ticks a foe struck burns (Ember)
	 * @param chill   ticks a foe struck is slowed under frost (Rime)
	 * @param spark   a spark leaping from the first foe, once the stroke has struck, to the nearest foe it didn't strike, as a share of the blow,
	 *                and the first foe interrupted (Thunder)
	 * @param knock   how much further foes are thrown back, and the blocks more it reaches ({@code reach}) (Gale)
	 * @param stance  how much harder it wears a stance, and the ticks a creature struck staggers ({@code stagger}) (Stone)
	 * @param mend    health it mends its swordsman for each foe struck, at most {@code mendCap} a technique (Verdant)
	 * @param pull    how hard foes struck are drawn in toward the heart of the stroke (Hollow)
	 * @param aura    aura it gives back for each foe struck, at most {@code auraCap} a technique (Starlit)
	 * @param echo    a faint echo of the strike on the first two foes, as a share of the blow, {@code echoDelay} ticks later (Hourglass)
	 * @param bleed   a wound that bleeds {@code bleeds} times, each a share of a weapon, and a drink of {@code drink} of what the technique
	 *                deals, at most {@code drinkCap} health (Crimson)
	 * @param damage  its blow times this (a method with no element of its own)
	 */
	public record Flavour(BreathingMethod.Flavour of, int ignite, int chill, double spark, double knock, double reach, double stance, int stagger,
			double mend, double mendCap, double pull, double aura, double auraCap, double echo, int echoDelay, double bleed, int bleeds, double drink,
			double drinkCap, double damage) {}

	private static Flavour flavour(BreathingMethod.Flavour of, int ignite, int chill, double spark, double knock, double reach, double stance,
			int stagger, double mend, double pull, double aura, double echo, double bleed, double drink, double damage) {
		return new Flavour(of, ignite, chill, spark, knock, reach, stance, stagger, mend, mend * 3, pull, aura, aura * 8 / 3, echo, 6, bleed,
			bleed > 0 ? 2 : 0, drink, drink > 0 ? 1.5 : 0, damage);
	}

	/** Each method's flavour, by its passive (an add-on's method takes the flavour of the passive it chose; none, the plain one). */
	public static Flavour flavour(BreathingMethod.Flavour of) {
		BreathingMethod.Flavour f = of == null ? BreathingMethod.Flavour.NONE : of;
		return switch (f) {
			case IGNITE -> flavour(f, 24, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1);
			case CHILL -> flavour(f, 0, 30, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1);
			case SPARK -> flavour(f, 0, 0, 0.35, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1);
			case GALE -> flavour(f, 0, 0, 0, 0.45, 1.0, 1, 0, 0, 0, 0, 0, 0, 0, 1);
			case STONE -> flavour(f, 0, 0, 0, 0, 0, 1.4, 12, 0, 0, 0, 0, 0, 0, 1);
			case MEND -> flavour(f, 0, 0, 0, 0, 0, 1, 0, 1.0, 0, 0, 0, 0, 0, 1);
			case PULL -> flavour(f, 0, 0, 0, 0, 0, 1, 0, 0, 0.3, 0, 0, 0, 0, 1);
			case STARLIT -> flavour(f, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1.0, 0, 0, 0, 1);
			case HASTE -> flavour(f, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0.18, 0, 0, 1);
			case LEECH -> flavour(f, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0.05, 0.12, 1);
			case NONE -> flavour(f, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1.12);
		};
	}

	// ================================================================== ranks, tempers and edges

	public static final int RAW = 1;
	public static final int HONED = 2;
	public static final int TEMPERED = 3;
	public static final int KEEN = 4;
	public static final int PEERLESS = 5;
	public static final int MAX_RANK = PEERLESS;
	/** The ranks' ids (their names are {@code aura.wildercord.technique_rank.<id>}). */
	public static final List<String> RANKS = List.of("raw", "honed", "tempered", "keen", "peerless");
	/** The experience (a running total) each rank needs. */
	public static final int[] THRESHOLDS = {0, 60, 200, 520, 1300};
	/** How hard a technique of each rank strikes, against what it's priced at (Tempered). */
	public static final double[] RANK_STRENGTH = {0.965, 0.98, 1.0, 1.02, 1.04};

	/** The rank {@code xp} has reached, {@link #RAW} to {@link #PEERLESS}. */
	public static int rank(double xp) {
		int rank = RAW;
		for (int i = 1; i < THRESHOLDS.length; i++) {
			if (xp >= THRESHOLDS[i]) {
				rank = i + 1;
			}
		}
		return rank;
	}

	/** The experience {@code rank} needs. */
	public static int threshold(int rank) {
		return THRESHOLDS[clampRank(rank) - 1];
	}

	/** How far {@code xp} has come from its rank toward the next, 0 to 1 (1 at Peerless). */
	public static double progress(double xp) {
		int rank = rank(xp);
		if (rank >= MAX_RANK) {
			return 1.0;
		}
		double from = threshold(rank);
		return Math.max(0, Math.min(1, (xp - from) / (threshold(rank + 1) - from)));
	}

	public static int clampRank(int rank) {
		return Math.max(RAW, Math.min(MAX_RANK, rank));
	}

	public static String rankId(int rank) {
		return RANKS.get(clampRank(rank) - 1);
	}

	/** How a technique may be tempered from Honed (a fifth lighter, or a quarter harder: its price and rest follow), "" as written. */
	public static final String SWIFT = "swift";
	public static final String HEAVY = "heavy";
	public static final List<String> TEMPERS = List.of(SWIFT, HEAVY);
	public static final double SWIFT_SCALE = 0.8;
	public static final double HEAVY_SCALE = 1.25;
	/** Its edge from Keen: reaching a quarter further, or a third wider, each at a little of the blow; "" as written. */
	public static final String LONG = "long";
	public static final String BROAD = "broad";
	public static final List<String> EDGES = List.of(LONG, BROAD);
	public static final double LONG_REACH = 1.25;
	public static final double LONG_WAVE = 1.2;
	public static final double LONG_FACTOR = 0.93;
	public static final double BROAD_DEGREES = 40;
	public static final double BROAD_LINE = 1.4;
	public static final double BROAD_RING = 1.15;
	public static final double BROAD_FAIR = 1.3;
	public static final int BROAD_TARGETS = 2;
	public static final double BROAD_FACTOR = 0.9;
	/** The rank that opens the temper, and the edge. */
	public static final int TEMPER_RANK = HONED;
	public static final int EDGE_RANK = KEEN;
	/** Experience levels it costs to change a temper or an edge already chosen (the first choice is free). */
	public static final int CHANGE_LEVELS = 2;
	/** The rank from which a technique's parts can be set down on scrolls. */
	public static final int INSCRIBE_RANK = PEERLESS;

	/** What a temper does to a blow's strength (and every hold and burn with it): 1 for none. */
	public static double temperScale(String temper) {
		return SWIFT.equals(temper) ? SWIFT_SCALE : HEAVY.equals(temper) ? HEAVY_SCALE : 1.0;
	}

	/** Whether {@code choice} is a temper ({@code edge} false) or an edge one may choose ("" puts it back as written). */
	public static boolean validChoice(boolean edge, String choice) {
		return choice != null && (choice.isEmpty() || (edge ? EDGES : TEMPERS).contains(choice));
	}

	// ================================================================== a technique, resolved

	/**
	 * What a written technique does, every number resolved from its parts, its swordsman's method, its rank, its temper and its edge.
	 * The runtime performs it from these; {@link #worth} weighs it.
	 *
	 * @param factor        what its main foe takes, in weapon damage (all of it at once; an afterimage's later strike is {@code later})
	 * @param reach         how far the stroke reaches (blocks): a line's length, a cone's, a ring's radius; for a wave, where it sets off
	 * @param width         a cone's width (degrees) or a line's half-width (blocks)
	 * @param shape         the shape it strikes in (a burst always a ring)
	 * @param targets       how many foes it strikes at most
	 * @param others        the share of the blow the foes after the first take
	 * @param fair          how many other foes it fairly reaches (the balance model's count)
	 * @param flight        how far a wave flies past {@code reach} (0 for none)
	 * @param later         an afterimage's strike, as a share of the stroke's whole blow (0 for none), {@code laterDelay} ticks after
	 * @param lift          how hard it throws foes up
	 * @param stance        how much harder than an art's it wears a stance
	 * @param bind          ticks it roots each foe struck
	 * @param echo          its strike again on each foe it struck, as a share, {@link #ECHO_DELAY} ticks after (0 for none)
	 * @param ward          what it steadies its swordsman by, for {@code wardTicks}
	 * @param rally         what it steadies its swordsman and allies within {@code rallyRadius} by, for {@code rallyTicks}, and the
	 *                      momentum each allied swordsman near builds ({@code rallyMomentum})
	 * @param flavourScale  how strongly the method's element comes through
	 * @param temperScale   its temper's scale (holds and burns with it)
	 */
	public record Profile(Stroke stroke, Release release, Intent intent, Flavour flavour, int rank, String temper, String edge, double factor, double reach,
			double width, Shape shape, int targets, double others, double fair, double flight, double later, int laterDelay, double lift, double stance,
			int bind, double echo, double ward, int wardTicks, double rally, int rallyTicks, double rallyRadius, double rallyMomentum, double flavourScale,
			double temperScale) {
		/** How far it reaches from where its swordsman stood, all told (a wave's flight included). */
		public double total() {
			return reach + flight;
		}

		/** Ticks the flavour's burn lasts, chill, stagger (each with the temper and the infusing). */
		public int ignite() {
			return (int) Math.round(flavour.ignite() * flavourScale * temperScale);
		}

		public int chill() {
			return (int) Math.round(flavour.chill() * flavourScale * temperScale);
		}

		public int stagger() {
			return (int) Math.round(flavour.stagger() * flavourScale * temperScale);
		}
	}

	/** {@code technique} resolved: see {@link Profile}. Throws for parts that aren't parts. */
	public static Profile profile(String strokeId, String releaseId, String intentId, BreathingMethod.Flavour method, int rank, String temper, String edge) {
		Stroke s = stroke(strokeId).orElseThrow(() -> new IllegalArgumentException("no stroke " + strokeId));
		Release r = release(releaseId).orElseThrow(() -> new IllegalArgumentException("no release " + releaseId));
		int rk = clampRank(rank);
		Intent base = intent(intentId).orElseThrow(() -> new IllegalArgumentException("no intent " + intentId));
		Intent in = rk >= TEMPERED ? DEEP.getOrDefault(base.id(), base) : base;
		Flavour fl = flavour(method);
		String t = TEMPERS.contains(temper) ? temper : "";
		String e = EDGES.contains(edge) ? edge : "";
		double ts = temperScale(t);
		boolean longEdge = LONG.equals(e);
		boolean broad = BROAD.equals(e);
		double edgeFactor = longEdge ? LONG_FACTOR : broad ? BROAD_FACTOR : 1.0;
		double element = in.flavour();
		double damage = 1 + (fl.damage() - 1) * element;
		double factor = s.factor() * r.factor() * in.factor() * damage * RANK_STRENGTH[rk - 1] * ts * edgeFactor;
		boolean burst = r.id().equals(BURST);
		boolean wave = r.id().equals(WAVE);
		Shape shape = burst ? Shape.RING : s.shape();
		double reach = s.reach() + fl.reach() * element + in.reach() * (s.shape() == Shape.RING || burst ? 0.5 : 1.0);
		if (longEdge) {
			reach *= LONG_REACH;
		}
		if (burst) {
			reach = s.shape() == Shape.RING ? reach + 1.0 : reach * BURST_RADIUS;
		}
		double flight = wave ? (s.shape() == Shape.RING ? WAVE_RING : WAVE_LENGTH) * (longEdge ? LONG_WAVE : 1.0) : 0;
		double width = switch (shape) {
			case LINE -> s.width() * (broad ? BROAD_LINE : 1.0);
			case CONE -> Math.min(300, s.width() + (broad ? BROAD_DEGREES : 0));
			case RING -> 360;
		};
		if (broad && shape == Shape.RING) {
			reach *= BROAD_RING;
		}
		int targets = (int) Math.round(s.targets() * r.targets()) + in.targets() + (broad ? BROAD_TARGETS : 0);
		double fair = s.fair() * r.fair() * in.fair() * (broad ? BROAD_FAIR : 1.0);
		double later = r.later() * s.factor() * in.factor() * damage * RANK_STRENGTH[rk - 1] * ts * edgeFactor;
		return new Profile(s, r, in, fl, rk, t, e, factor, reach, width, shape, targets, s.others(), fair, flight, later, r.delay(), s.lift() * ts,
			stance(s.stance(), in.stance(), 1 + (fl.stance() - 1) * element), (int) Math.round(in.bind() * ts), in.echo(), in.ward(), (int) Math.round(in.wardTicks() * ts),
			in.rally(), (int) Math.round(in.rallyTicks() * ts), in.rallyRadius(), in.rallyMomentum() * ts, in.flavour(), ts);
	}

	/** The most a technique's strikes wear a stance, as a weight against an art's (a sunder's falling cut with Stone's blade comes near it). */
	public static final double MAX_STANCE = 3.0;

	/** How hard a technique wears a stance: what its stroke, its intent and its method's element each add past an art's, together, held to {@link #MAX_STANCE}. */
	public static double stance(double stroke, double intent, double element) {
		return Math.max(1.0, Math.min(MAX_STANCE, 1 + Math.max(0, stroke - 1) + Math.max(0, intent - 1) + Math.max(0, element - 1)));
	}

	// ================================================================== what it's worth, and its price

	/** Control seconds a unit of lift is worth (a full throw up, about 0.6, about a second in the air). */
	public static final double LIFT_SECONDS = 1.6;
	/** Control seconds each unit of stance weight past an art's is worth (it opens a foe sooner: a sunder's two and a half, a second and a half). */
	public static final double STANCE_SECONDS = 1.0;
	/** Control seconds each second of its swordsman steadied by a fifth is worth (an art that hardens its swordsman counts as control too). */
	public static final double WARD_SECONDS = 0.35;
	/** Control seconds each second of a rally's steadying is worth, and what it's worth to the allies it reaches (W). */
	public static final double RALLY_SECONDS = 0.12;
	public static final double RALLY_AREA = 0.3;
	/** What a root on the foes after the first is worth, as a share of the first's. */
	public static final double BIND_OTHERS = 0.3;
	/** What a slow under frost (Rime's flavour) is worth, as a share of a hold. */
	public static final double CHILL_SHARE = 0.25;
	/** Control seconds an interrupted foe (Thunder's spark), a unit of throw back (Gale), a second of stagger (Stone) and a unit of pull (Hollow) are worth. */
	public static final double INTERRUPT_SECONDS = 0.25;
	public static final double KNOCK_SECONDS = 0.5;
	public static final double STAGGER_SHARE = 0.5;
	public static final double PULL_SECONDS = 2.0;

	/** {@code p} in the arts' balance model ({@link ArtRules.Art}): its primary, area, control, reach, mending and aura given back. */
	public static ArtRules.Art model(Profile p) {
		double echoed = 1 + p.echo() * ECHO_LANDS;
		double first = p.factor() * echoed + p.later() * LATER_LANDS;
		double rest = first * p.others() * p.fair();
		Flavour fl = p.flavour();
		double k = p.flavourScale() * p.temperScale();
		double primary = first;
		double area = rest;
		double control = p.lift() * LIFT_SECONDS + Math.max(0, p.stance() - 1) * STANCE_SECONDS;
		control += p.bind() / 20.0 * (1 + BIND_OTHERS * p.fair());
		control += p.ward() / 0.2 * p.wardTicks() / 20.0 * WARD_SECONDS;
		control += p.rally() / 0.1 * p.rallyTicks() / 20.0 * RALLY_SECONDS;
		area += p.rally() > 0 ? RALLY_AREA : 0;
		// An add-on's own intent, as it says it's worth.
		control += p.intent().control();
		area += p.intent().area();
		// The method's element.
		primary += ArtRules.fire((int) Math.round(fl.ignite() * k));
		control += fl.chill() * k / 20.0 * CHILL_SHARE * (1 + 0.5 * p.fair());
		area += fl.spark() * k * p.factor() * 0.6;
		control += fl.spark() > 0 ? INTERRUPT_SECONDS : 0;
		control += fl.knock() * k * KNOCK_SECONDS;
		control += fl.stagger() * k / 20.0 * STAGGER_SHARE;
		control += fl.pull() * k * PULL_SECONDS;
		double mend = ArtRules.health(Math.min(fl.mendCap() * k, fl.mend() * k * (1 + 0.6 * p.fair())));
		double aura = Math.min(fl.auraCap() * k, fl.aura() * k * (1 + 0.6 * p.fair()));
		primary += fl.echo() * k * p.factor() * ECHO_LANDS;
		area += fl.echo() * k * p.factor() * ECHO_LANDS * Math.min(1, p.fair()) * p.others();
		primary += fl.bleed() * k * fl.bleeds();
		mend += ArtRules.health(Math.min(fl.drinkCap() * k, fl.drink() * k * 7 * (first + 0.5 * rest)));
		return new ArtRules.Art("technique", "", 2, 0, 0, primary, area, control, p.total(), mend, aura, 0, Set.of());
	}

	/** What {@code p} is worth in the arts' balance model ({@link ArtRules#power}), in W. */
	public static double worth(Profile p) {
		return ArtRules.power(model(p));
	}

	/** Aura a W of a technique costs, and ticks it rests: the arts' own rate across the First to the Fourth Art. */
	public static final double AURA_PER_W = 4.32;
	public static final double TICKS_PER_W = 43.2;
	/** The least and most a technique costs and rests, whatever it is (an add-on's intent can't price one past these). */
	public static final double MIN_COST = 3.0;
	public static final double MAX_COST = 13.0;
	public static final int MIN_REST = 30;
	public static final int MAX_REST = 130;

	/**
	 * How much a string asks of the hand, for its price: every token's weight, and half a point for every swing past the first.
	 * "full leap low" asks 6; "run low" 4.5.
	 */
	public static double effortOf(SwordString string) {
		return string.weight() + 0.5 * (string.length() - 1);
	}

	/** What a string does to a technique's price and rest: an easy one a little more, a demanding one a little less. */
	public static double effort(SwordString string) {
		double e = effortOf(string);
		return Math.max(EFFORT_LEAST, Math.min(EFFORT_MOST, 1 + EFFORT_STEP * (EFFORT_MID - e)));
	}

	public static final double EFFORT_MID = 5.5;
	public static final double EFFORT_STEP = 0.01;
	public static final double EFFORT_LEAST = 0.98;
	public static final double EFFORT_MOST = 1.02;

	/** What a technique is priced at: its worth at Tempered (with its temper and edge), so a lower rank is a little weaker for the same price. */
	public static double pricedWorth(String stroke, String release, String intent, BreathingMethod.Flavour method, String temper, String edge) {
		return worth(profile(stroke, release, intent, method, TEMPERED, temper, edge));
	}

	/** Its price in aura (to a tenth), from its priced worth and its string. */
	public static double cost(double pricedWorth, SwordString string) {
		double c = pricedWorth * AURA_PER_W * effort(string);
		return Math.round(Math.max(MIN_COST, Math.min(MAX_COST, c)) * 10) / 10.0;
	}

	/** Its rest in ticks, from its priced worth and its string. */
	public static int rest(double pricedWorth, SwordString string) {
		return (int) Math.round(Math.max(MIN_REST, Math.min(MAX_REST, pricedWorth * TICKS_PER_W * effort(string))));
	}

	// ================================================================== strings

	/** A technique's string: two to five swings. */
	public static final int MIN_STRING = 2;
	public static final int MAX_STRING = 5;
	/** ... asking at least this much of the hand (the tokens' weights together). */
	public static final int MIN_WEIGHT = 3;

	/** Why a string can't be a technique's, by itself (before what it meets): too short, too long, too plain, or only full swings. */
	public enum StringProblem {
		TOO_SHORT,
		TOO_LONG,
		TOO_PLAIN,
		NO_MARK
	}

	/**
	 * What's wrong with {@code string} as a technique's, by itself, or empty: at least {@link #MIN_STRING} swings and at most
	 * {@link #MAX_STRING}, weighing {@link #MIN_WEIGHT} or more, and at least one deliberate swing among them (low, leaping, running, a
	 * counter or a step cut): full swings are how a patient swordsman always swings, so a string of only those would go off by itself in
	 * an ordinary fight.
	 */
	public static Optional<StringProblem> problem(SwordString string) {
		if (string.length() < MIN_STRING) {
			return Optional.of(StringProblem.TOO_SHORT);
		}
		if (string.length() > MAX_STRING) {
			return Optional.of(StringProblem.TOO_LONG);
		}
		if (string.weight() < MIN_WEIGHT) {
			return Optional.of(StringProblem.TOO_PLAIN);
		}
		for (SwordString.Token t : string.tokens()) {
			if (t.weight >= 2) {
				return Optional.empty();
			}
		}
		return Optional.of(StringProblem.NO_MARK);
	}

	/** Whether two strings get in each other's way so that one may never be played as written: the same swings, or one cutting the other short. */
	public static boolean clash(SwordString a, SwordString b) {
		return a.equals(b) || a.cutBy(b) || b.cutBy(a);
	}

	/**
	 * Whether one swing can be both of these kinds: always, but for a low swing and a running one (sneaking ends a sprint). A plain swing
	 * fits any.
	 */
	public static boolean together(SwordString.Token a, SwordString.Token b) {
		if (a == SwordString.Token.SWING || b == SwordString.Token.SWING || a == b) {
			return true;
		}
		return !(a == SwordString.Token.LOW && b == SwordString.Token.RUN || a == SwordString.Token.RUN && b == SwordString.Token.LOW);
	}

	/**
	 * Whether the same swings could finish both strings at once (their ends lined up, every swing of the shorter one's length able to be
	 * both kinds): when they do, the reader plays the one that asks more ({@link StringReader#compare}), and the other only if that one
	 * can't go. Not a clash: each can still be played on its own; the writing page says which goes first.
	 */
	public static boolean overlap(SwordString a, SwordString b) {
		int n = Math.min(a.length(), b.length());
		for (int i = 1; i <= n; i++) {
			if (!together(a.token(a.length() - i), b.token(b.length() - i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the only way the same swings finish both strings is a counter or a step cut that {@code mine} doesn't ask for (the other's
	 * cue lined up against a swing of mine that isn't that cue): a perfect guard or an Aura Step is something its swordsman means, never
	 * something that happens on the way, so the page doesn't warn of it.
	 */
	public static boolean incidentalCue(SwordString mine, SwordString other) {
		int n = Math.min(mine.length(), other.length());
		for (int i = 1; i <= n; i++) {
			SwordString.Token theirs = other.token(other.length() - i);
			SwordString.Token own = mine.token(mine.length() - i);
			if ((theirs == SwordString.Token.COUNTER || theirs == SwordString.Token.STEP) && own != theirs) {
				return true;
			}
		}
		return false;
	}

	// ================================================================== names

	/** The longest name a technique may have (in letters). */
	public static final int MAX_NAME = 24;

	/**
	 * A name as its writer typed it, made safe to show anyone: formatting codes taken out (a section sign and the letter after it),
	 * control characters, invisible ones and those that turn text round (bidirectional overrides and isolates, zero-width marks, line
	 * separators, a lone surrogate) dropped, every run of spaces one space, trimmed, and held to {@link #MAX_NAME} letters (never
	 * splitting a letter outside the basic plane).
	 */
	public static String cleanName(String typed) {
		if (typed == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		int letters = 0;
		boolean space = false;
		int i = 0;
		while (i < typed.length() && letters < MAX_NAME) {
			int cp = typed.codePointAt(i);
			int len = Character.charCount(cp);
			i += len;
			if (cp == 0xA7) {
				// A formatting code: drop it and its letter.
				if (i < typed.length()) {
					i += Character.charCount(typed.codePointAt(i));
				}
				continue;
			}
			if (!shown(cp)) {
				continue;
			}
			if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
				if (!out.isEmpty()) {
					space = true;
				}
				continue;
			}
			if (space) {
				if (letters + 1 >= MAX_NAME) {
					break;
				}
				out.append(' ');
				letters++;
				space = false;
			}
			out.appendCodePoint(cp);
			letters++;
		}
		return out.toString().trim();
	}

	/** Whether a character typed may go into a name at all (the writing page takes only these): a space, or anything shown. */
	public static boolean nameCharacter(int cp) {
		return cp != 0xA7 && (cp == ' ' || shown(cp) && !Character.isWhitespace(cp));
	}

	/** Whether a character may stand in a name (a space is kept, as one). */
	private static boolean shown(int cp) {
		if (cp < 0x20 || cp == 0x7F || cp >= 0x80 && cp <= 0x9F) {
			return false;
		}
		if (cp >= 0x200B && cp <= 0x200F || cp >= 0x202A && cp <= 0x202E || cp >= 0x2060 && cp <= 0x2069 || cp == 0x2028 || cp == 0x2029
				|| cp == 0xFEFF || cp == 0x061C || cp == 0x180E) {
			return false;
		}
		if (cp >= 0xD800 && cp <= 0xDFFF) {
			return false;
		}
		int type = Character.getType(cp);
		return type != Character.CONTROL && type != Character.FORMAT && type != Character.PRIVATE_USE && type != Character.UNASSIGNED
			&& type != Character.SURROGATE;
	}

	/** Every part's word in a technique's default name (English: the writing page suggests one in the player's own words). */
	private static final Map<String, String> WORDS = Map.ofEntries(Map.entry(THRUST, "Thrust"), Map.entry(RISING, "Rising Cut"),
		Map.entry(FALLING, "Falling Cut"), Map.entry(SWEEP, "Sweep"), Map.entry(SPIN, "Spin"), Map.entry(DRAW, "Draw"), Map.entry(ON_BLADE, ""),
		Map.entry(WAVE, "Wave"), Map.entry(BURST, "Burst"), Map.entry(AFTERIMAGE, "Shadow"), Map.entry(PIERCE, "Piercing"),
		Map.entry(SUNDER, "Sundering"), Map.entry(BIND, "Binding"), Map.entry(ECHO, "Echoing"), Map.entry(WARD, "Warding"),
		Map.entry(RALLY, "Rallying"), Map.entry(INFUSE, ""));

	/** The method's word for an infused technique. */
	private static String infused(BreathingMethod.Flavour method) {
		return switch (method == null ? BreathingMethod.Flavour.NONE : method) {
			case IGNITE -> "Blazing";
			case CHILL -> "Frozen";
			case SPARK -> "Thundering";
			case GALE -> "Howling";
			case STONE -> "Mountain";
			case MEND -> "Blooming";
			case PULL -> "Hollow";
			case STARLIT -> "Starlit";
			case HASTE -> "Timeless";
			case LEECH -> "Crimson";
			case NONE -> "True";
		};
	}

	/** A technique's default name from its parts ("Sundering Falling Cut Wave", "Blazing Draw"), for a writer who gives it none. */
	public static String autoName(String stroke, String release, String intent, BreathingMethod.Flavour method) {
		String adjective = INFUSE.equals(intent) ? infused(method) : WORDS.getOrDefault(intent, "");
		String noun = WORDS.getOrDefault(stroke, "Cut");
		String tail = WORDS.getOrDefault(release, "");
		StringBuilder out = new StringBuilder();
		for (String word : new String[] {adjective, noun, tail}) {
			if (!word.isEmpty()) {
				if (!out.isEmpty()) {
					out.append(' ');
				}
				out.append(word);
			}
		}
		return cleanName(out.toString());
	}

	// ================================================================== slots

	/** The most techniques a swordsman writes: one at Edge, two at Form, three at Sovereign. */
	public static final int MAX_SLOTS = 3;

	/** How many technique slots {@code stage} opens. */
	public static int slots(int stage) {
		return Math.max(0, Math.min(MAX_SLOTS, AuraRules.clampStage(stage) - AuraRules.EDGE + 1));
	}

	/** The stage that opens slot {@code i} (0 the first). */
	public static int slotStage(int i) {
		return AuraRules.EDGE + Math.max(0, Math.min(MAX_SLOTS - 1, i));
	}

	/** The id a written technique plays under in slot {@code i} (its rest is kept by slot, so writing another there doesn't reset it). */
	public static String artId(int i) {
		return "technique_" + (i + 1);
	}

	/** The slot a technique's art id plays in, or -1 for an id that isn't a technique's. */
	public static int slotOf(String artId) {
		if (artId == null || !artId.startsWith("technique_")) {
			return -1;
		}
		try {
			int i = Integer.parseInt(artId.substring("technique_".length())) - 1;
			return i >= 0 && i < MAX_SLOTS ? i : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/** The key a technique's record is kept by: its three parts (renaming it, or giving it another string, keeps its rank). */
	public static String key(String stroke, String release, String intent) {
		return stroke + "/" + release + "/" + intent;
	}

	/** How many technique records a swordsman keeps (the most recently used; one written in a slot is never forgotten). */
	public static final int MAX_RECORDS = 12;

	/** Which arts' slot a technique's momentum is counted as, by its worth: the nearest of the First to the Fourth Art. */
	public static int momentumSlot(double worth) {
		int best = 0;
		for (int i = 1; i < 4; i++) {
			if (Math.abs(ArtRules.SLOT_POWER[i] - worth) < Math.abs(ArtRules.SLOT_POWER[best] - worth)) {
				best = i;
			}
		}
		return best;
	}

	// ================================================================== experience

	/** Experience for striking a real foe once in a technique, for taking its whole health, and for felling it. */
	public static final double STRIKE = 1.0;
	public static final double DAMAGE = 2.0;
	public static final double KILL = 1.0;
	/** The most one use can earn, after everything that multiplies it. */
	public static final double MAX_PER_USE = 12.0;
	/** The most a technique learns from training dummies and the practice room, in all, and at what rate. */
	public static final double PRACTICE_CAP = 40.0;
	public static final double PRACTICE_RATE = 0.5;
	/** How much each recent use in the same place takes off the next (fading as spell mastery's does). */
	public static final double REPEAT = 0.08;

	/** Experience for one foe struck: worth by what it is (a monster 1, see {@code AuraRules.worth}), the share of its health taken, a kill. */
	public static double strike(double worth, double share, boolean killed) {
		if (worth <= 0) {
			return 0;
		}
		return worth * (STRIKE + DAMAGE * Math.max(0, Math.min(1, share)) + (killed ? KILL : 0));
	}

	/** What is left of a use's worth after {@code recent} recent uses in the same place (never under spell mastery's floor). */
	public static double repetition(double recent) {
		return Math.max(dev.wildercord.spell.MasteryRules.REPEAT_FLOOR, 1.0 / (1.0 + REPEAT * Math.max(0, recent)));
	}

	/** How much of {@code xp} earned in practice a technique takes, having already learned {@code learned} that way. */
	public static double practice(double learned, double xp) {
		return Math.max(0, Math.min(PRACTICE_CAP - learned, xp * PRACTICE_RATE));
	}

	// ================================================================== small helpers

	/** A part's id written as the writing page and the commands read it. */
	public static String normal(String id) {
		return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
	}
}
