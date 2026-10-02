package dev.wildercord.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * The bonded blade, the pure part: its tiers and what each asks and gives, the ceremonies' timing, what every deed is worth in
 * resonance, its traits (each tied to a fighting habit, scored from the blade's own history, weighed for balance and held for PvP),
 * and the names it suggests for itself. No game objects: the server ({@code BondedBlades}, {@code BladeCeremony}), the client's page
 * and tooltip, and {@code BladeRulesTest} all read these numbers.
 *
 * <p><b>Tiers.</b> A blade is <i>Bonded</i> by the ceremony (from Edge), then grows by its <i>resonance</i>: <i>Named</i> at 300
 * (it takes a name), <i>Awakened</i> at 1200 (it takes a trait) and <i>Soulforged</i> at 3600 (its trait half again as strong, its
 * look at its fullest). Each tier also waits on its swordsman's stage, as a breakthrough does on its trial: Named on Edge, Awakened on
 * Form, Soulforged on Sovereign and a boss felled by the blade. Resonance keeps gathering while a tier waits, so it comes the moment
 * the swordsman is ready for it. A blade passed to a disciple keeps its tier, but only gives them what their own stage allows
 * ({@link #effective}).</p>
 */
public final class BladeRules {
	private BladeRules() {}

	// ================================================================== tiers

	public static final int NONE = 0;
	public static final int BONDED = 1;
	public static final int NAMED = 2;
	public static final int AWAKENED = 3;
	public static final int SOULFORGED = 4;
	public static final int MAX_TIER = SOULFORGED;

	/** The stage a blade can first be bonded at. */
	public static final int FROM = AuraRules.EDGE;
	/** The resonance each tier asks for. */
	private static final double[] THRESHOLD = {0, 0, 300, 1200, 3600};
	/** The stage each tier waits on (its gifts too, for a passed blade). */
	private static final int[] GATE = {AuraRules.NONE, AuraRules.EDGE, AuraRules.EDGE, AuraRules.FORM, AuraRules.SOVEREIGN};
	/** Soulforged asks a great deed besides: this many bosses felled by the blade. */
	public static final int SOULFORGED_BOSSES = 1;
	private static final List<String> TIER_IDS = List.of("none", "bonded", "named", "awakened", "soulforged");

	public static int clampTier(int tier) {
		return Math.max(NONE, Math.min(MAX_TIER, tier));
	}

	/** The id of {@code tier} for language keys and sprites: bonded, named, awakened, soulforged ("none" below Bonded). */
	public static String tierId(int tier) {
		return TIER_IDS.get(clampTier(tier));
	}

	/** The resonance {@code tier} asks for. */
	public static double threshold(int tier) {
		return THRESHOLD[clampTier(tier)];
	}

	/** The stage {@code tier} waits on. */
	public static int gate(int tier) {
		return GATE[clampTier(tier)];
	}

	/** The tier a blade's resonance and deeds have earned, whatever its swordsman's stage (Bonded at least: it is bonded). */
	public static int earned(double resonance, int bosses) {
		int tier = BONDED;
		for (int t = NAMED; t <= MAX_TIER; t++) {
			if (resonance + 1.0E-6 < THRESHOLD[t] || t == SOULFORGED && bosses < SOULFORGED_BOSSES) {
				break;
			}
			tier = t;
		}
		return tier;
	}

	/** The highest tier a swordsman of {@code stage} can be given (0 below Edge: a passed blade sleeps until its disciple reaches it). */
	public static int allowed(int stage) {
		int tier = NONE;
		for (int t = BONDED; t <= MAX_TIER; t++) {
			if (stage >= GATE[t]) {
				tier = t;
			}
		}
		return tier;
	}

	/** The tier a blade reaches for a swordsman of {@code stage}: what it has earned, as far as their stage allows. */
	public static int tier(double resonance, int bosses, int stage) {
		return Math.min(earned(resonance, bosses), Math.max(BONDED, allowed(stage)));
	}

	/** What a blade of stored tier {@code stored} gives a swordsman of {@code stage} (0: it sleeps). */
	public static int effective(int stored, int stage) {
		return Math.min(clampTier(stored), allowed(stage));
	}

	/** How far a blade at {@code tier} has come toward the next tier's resonance (0 to 1; 1 at Soulforged). */
	public static double progress(double resonance, int tier) {
		int t = clampTier(tier);
		if (t >= MAX_TIER) {
			return 1;
		}
		double from = THRESHOLD[Math.max(BONDED, t)];
		double to = THRESHOLD[t + 1];
		return Math.max(0, Math.min(1, (resonance - from) / Math.max(1, to - from)));
	}

	/** What the next tier waits on now. */
	public enum Waiting { NOTHING, RESONANCE, STAGE, BOSS, TOP }

	/** What a blade at {@code tier} waits on before the next: more resonance, its swordsman's stage, a boss felled by it, or nothing. */
	public static Waiting waiting(int tier, double resonance, int bosses, int stage) {
		int next = clampTier(tier) + 1;
		if (next > MAX_TIER) {
			return Waiting.TOP;
		}
		if (resonance + 1.0E-6 < THRESHOLD[next]) {
			return Waiting.RESONANCE;
		}
		if (stage < GATE[next]) {
			return Waiting.STAGE;
		}
		if (next == SOULFORGED && bosses < SOULFORGED_BOSSES) {
			return Waiting.BOSS;
		}
		return Waiting.NOTHING;
	}

	// ================================================================== what each tier gives

	/** Named: blows with it draw a little more aura, "it knows the hand". Soulforged: more. */
	public static final double NAMED_HIT_GAIN = 1.10;
	public static final double SOULFORGED_HIT_GAIN = 1.20;
	/** Soulforged: its trait half again as strong. */
	public static final double SOULFORGED_TRAIT = 1.5;

	/** What a coated blow's aura is times, with the blade at {@code tier}. */
	public static double hitGain(int tier) {
		return tier >= SOULFORGED ? SOULFORGED_HIT_GAIN : tier >= NAMED ? NAMED_HIT_GAIN : 1.0;
	}

	/** How strong its trait is at {@code tier}: whole once Awakened, half again at Soulforged, nothing before. */
	public static double traitStrength(int tier) {
		return tier >= SOULFORGED ? SOULFORGED_TRAIT : tier >= AWAKENED ? 1.0 : 0.0;
	}

	/** A factor's change ({@code factor} - 1) scaled by {@code strength}: a 15% discount at 1.5 is 22.5%. */
	public static double scaled(double factor, double strength) {
		return 1.0 + (factor - 1.0) * Math.max(0, strength);
	}

	// ================================================================== the ceremonies

	/** The breathing stance settles this long (ticks) before the bond ceremony begins: a quick crouch never starts it. */
	public static final int SETTLE_BEFORE = 10;
	/** The bond ceremony's length (ticks): kindling, joining, sealing. */
	public static final int BOND_TICKS = 200;
	public static final int KINDLE_END = 60;
	public static final int JOIN_END = 140;
	/** The passing ceremony's length (ticks). */
	public static final int PASS_TICKS = 160;
	/** How near a disciple kneels to receive a blade, and how squarely the two face each other (the dot of their looks, flat). */
	public static final double PASS_REACH = 2.75;
	public static final double PASS_FACING = 0.5;
	/** After a ceremony ends, the crossroads waits this long before it may rise in the same stance. */
	public static final int AFTER_CEREMONY = 60;

	/** Which part of the bond ceremony {@code ticks} in is: 0 kindling, 1 joining, 2 sealing, 3 sealed. */
	public static int phase(int ticks) {
		return ticks < KINDLE_END ? 0 : ticks < JOIN_END ? 1 : ticks < BOND_TICKS ? 2 : 3;
	}

	// ================================================================== resonance

	/** A worthy foe felled by the blade: this, times its worth (half to twice; a player half) and how fresh it is (repetition). */
	public static final double KILL = 1.0;
	/** ...and a boss, this on top. */
	public static final double BOSS_KILL = 30.0;
	/** One player's fall counts once a day: a second within this many ticks gives nothing. */
	public static final int PLAYER_KILL_REST = 24000;
	/** An art landing on a worthy foe, by slot (First to Final), on its first foe; each further foe a little more, up to three. */
	private static final double[] ART = {0.6, 0.8, 0.8, 1.0, 3.0};
	public static final double ART_MORE = 0.2;
	public static final int ART_MORE_FOES = 3;
	/** A finisher landed (on a boss more). */
	public static final double FINISHER = 2.0;
	public static final double BOSS_FINISHER = 5.0;
	/** A stance broken, a perfect guard against a worthy foe. */
	public static final double BROKEN = 0.4;
	public static final double GUARD = 0.5;
	/** An awakening begun with it in hand. */
	public static final double AWAKENING = 4.0;
	/** A duelist beaten with it. */
	public static final double DUEL = 15.0;
	/** A technique of the swordsman's reaching Peerless while they carry it. */
	public static final double PEERLESS = 25.0;
	/** A breakthrough made carrying it: into Form, into Sovereign. */
	public static final double FORM_BREAKTHROUGH = 120.0;
	public static final double SOVEREIGN_BREAKTHROUGH = 240.0;

	/** What a worthy kill gives: {@code worth} (0 for no foe), {@code repetition} (1 fresh), a boss's bonus on top. */
	public static double kill(double worth, double repetition, boolean boss) {
		if (worth <= 0) {
			return 0;
		}
		return KILL * worth * Math.max(0, Math.min(1, repetition)) + (boss ? BOSS_KILL : 0);
	}

	/** What an art in {@code slot} (0 First to 4 Final) gives as it lands on its {@code foe}th foe (1 the first). */
	public static double art(int slot, int foe) {
		if (foe <= 0) {
			return 0;
		}
		if (foe == 1) {
			return ART[Math.max(0, Math.min(ART.length - 1, slot))];
		}
		return foe <= 1 + ART_MORE_FOES ? ART_MORE : 0;
	}

	/** What a breakthrough into {@code stage} gives (only the top stages: a blade is bonded from Edge). */
	public static double breakthrough(int stage) {
		return stage >= AuraRules.SOVEREIGN ? SOVEREIGN_BREAKTHROUGH : stage == AuraRules.FORM ? FORM_BREAKTHROUGH : 0;
	}

	/**
	 * The most one foe can give through arts, finishers, techniques, stances broken and guards against it (its kill aside): a little
	 * more for a stronger one, much more for a boss. Pounding one patient foe never grows a blade.
	 */
	public static double foeCap(double worth, boolean boss) {
		return boss ? 40.0 : 4.0 + 2.0 * Math.max(0, worth);
	}

	// ================================================================== the history

	/** What a blade counts of its deeds (its {@code counts}). */
	public static final String KILLS = "kills";
	public static final String BOSSES = "bosses";
	public static final String STRONG = "strong";
	public static final String PLAYERS = "players";
	public static final String UNDEAD = "undead";
	public static final String NIGHT = "night";
	public static final String LOW = "low_health";
	public static final String ALLIED = "allied";
	public static final String ARTS = "arts";
	public static final String FINISHERS = "finishers";
	public static final String TECHNIQUES = "techniques";
	public static final String GUARDS = "guards";
	public static final String STEPS = "steps";
	public static final String SLASHES = "slashes";
	public static final String BROKEN_STANCES = "broken";
	public static final String AWAKENINGS = "awakenings";
	public static final String DUELS = "duels";
	/** Every count a blade keeps, in the tooltip's and the page's order. */
	public static final List<String> COUNTS = List.of(KILLS, BOSSES, STRONG, PLAYERS, UNDEAD, NIGHT, LOW, ALLIED, ARTS, FINISHERS, TECHNIQUES, GUARDS,
		STEPS, SLASHES, BROKEN_STANCES, AWAKENINGS, DUELS);
	/** The counts the tooltip shows (the rest are for the traits). */
	public static final List<String> SHOWN_COUNTS = List.of(KILLS, BOSSES, ARTS, FINISHERS, TECHNIQUES, GUARDS, DUELS);

	/** The most arts it remembers by name, notable deeds it keeps, and swordsmen it remembers having passed through. */
	public static final int MAX_ARTS = 12;
	public static final int MAX_DEEDS = 10;
	public static final int MAX_LINEAGE = 8;

	/** The notable deeds a blade keeps a line for. */
	public static final String DEED_BOSS = "boss";
	public static final String DEED_BREAKTHROUGH = "breakthrough";
	public static final String DEED_TIER = "tier";
	public static final String DEED_DUEL = "duel";
	public static final String DEED_PEERLESS = "peerless";
	public static final String DEED_WAY = "way";
	public static final String DEED_PASSED = "passed";
	public static final String DEED_NAMED = "named";
	public static final List<String> DEEDS = List.of(DEED_BOSS, DEED_BREAKTHROUGH, DEED_TIER, DEED_DUEL, DEED_PEERLESS, DEED_WAY, DEED_PASSED, DEED_NAMED);

	/** {@code arts} with {@code id} counted once more, the least played let go past {@link #MAX_ARTS} (never {@code id}). */
	public static Map<String, Integer> countArt(Map<String, Integer> arts, String id) {
		Map<String, Integer> next = new LinkedHashMap<>(arts == null ? Map.of() : arts);
		next.merge(id, 1, Integer::sum);
		while (next.size() > MAX_ARTS) {
			String least = null;
			for (Map.Entry<String, Integer> e : next.entrySet()) {
				if (!e.getKey().equals(id) && (least == null || e.getValue() < next.get(least))) {
					least = e.getKey();
				}
			}
			if (least == null) {
				break;
			}
			next.remove(least);
		}
		return next;
	}

	/** The art played most (by count; ties go to the first in id order), or "" for none. */
	public static String favourite(Map<String, Integer> arts) {
		String best = "";
		int most = 0;
		if (arts == null) {
			return best;
		}
		List<String> ids = new ArrayList<>(arts.keySet());
		Collections.sort(ids);
		for (String id : ids) {
			int n = arts.getOrDefault(id, 0);
			if (n > most) {
				most = n;
				best = id;
			}
		}
		return best;
	}

	// ================================================================== traits

	public static final String WELL_WORN = "well_worn";
	public static final String CLOSING_STROKE = "closing_stroke";
	public static final String SUNDERING_STEEL = "sundering_steel";
	public static final String RIPOSTE = "riposte";
	public static final String WIND_STEP = "wind_step";
	public static final String LONG_CRESCENT = "long_crescent";
	public static final String INKBOUND = "inkbound";
	public static final String SECOND_BLAZE = "second_blaze";
	public static final String MOUNTAINFELLER = "mountainfeller";
	public static final String GRAVEWARDEN = "gravewarden";
	public static final String LAST_LIGHT = "last_light";
	public static final String MOONWAKE = "moonwake";
	public static final String RALLYING_STEEL = "rallying_steel";

	// ---- what each does (at Awakened; Soulforged scales each change by SOULFORGED_TRAIT through scaled())
	/** Well-Worn Verse: its favourite art costs and rests this share of its usual. */
	public static final double WELL_WORN_PRICE = 0.85;
	public static final double WELL_WORN_REST = 0.85;
	/** Closing Stroke: a finisher gives back this many times its aura, and this much more momentum. */
	public static final double CLOSING_AURA = 1.5;
	public static final double CLOSING_MOMENTUM = 4.0;
	/** Sundering Steel: blows and arts wear a stance this much harder (half that against a player). */
	public static final double SUNDERING_WEAR = 1.12;
	/** Riposte: for this long after a perfect guard, the next coated blow lands this much harder (half that against a player). */
	public static final int RIPOSTE_TICKS = 40;
	public static final double RIPOSTE_DAMAGE = 1.15;
	/** Wind Step: Aura Step costs and rests this share of its usual. */
	public static final double WIND_STEP_PRICE = 0.75;
	public static final double WIND_STEP_REST = 0.85;
	/** Long Crescent: Aura Slash costs this share and flies this much further. */
	public static final double LONG_CRESCENT_PRICE = 0.8;
	public static final double LONG_CRESCENT_REACH = 1.2;
	/** Inkbound Steel: techniques cost this share and rank this much faster. */
	public static final double INKBOUND_PRICE = 0.9;
	public static final double INKBOUND_XP = 1.2;
	/**
	 * Second Blaze: the awakening's rest and the spent time after it, this share of their usual (more awakenings, never a longer one: the
	 * Final Art still comes at most once in each, {@code AwakeningRulesTest}).
	 */
	public static final double SECOND_BLAZE_REST = 0.75;
	public static final double SECOND_BLAZE_SPENT = 0.75;
	/** Mountainfeller: against bosses and Runebound foes, coated blows and arts this much harder, and stance worn this much harder. */
	public static final double MOUNTAINFELLER_DAMAGE = 1.10;
	public static final double MOUNTAINFELLER_STANCE = 1.15;
	/** Gravewarden: coated blows against the undead this much harder. */
	public static final double GRAVEWARDEN_DAMAGE = 1.12;
	/** Last Light: below this share of health, harm from foes is this share of what it was. */
	public static final double LAST_LIGHT_BELOW = 1.0 / 3.0;
	public static final double LAST_LIGHT_HARM = 0.92;
	/** Moonwake: at night, blows with it draw this much more aura. */
	public static final double MOONWAKE_GAIN = 1.25;
	/** Rallying Steel: a finisher gives allied swordsmen this near this much momentum. */
	public static final double RALLYING_MOMENTUM = 5.0;
	public static final double RALLYING_RANGE = 10.0;

	/**
	 * What a blade remembers of how it was used, as its traits read it: its counts, the arts it played by name, its swordsman's method
	 * and Way.
	 */
	public record History(Map<String, Integer> counts, Map<String, Integer> arts, String method, String way) {
		public History {
			counts = counts == null ? Map.of() : Map.copyOf(counts);
			arts = arts == null ? Map.of() : Map.copyOf(arts);
			method = method == null ? "" : method;
			way = way == null ? "" : way;
		}

		public int count(String id) {
			return counts.getOrDefault(id, 0);
		}

		/** Arts landed, by count of every art named (the {@link #ARTS} count can lag when many arts were let go). */
		public int artPlays() {
			return Math.max(count(ARTS), arts.values().stream().mapToInt(Integer::intValue).sum());
		}
	}

	/**
	 * A trait a blade can take at Awakened.
	 *
	 * @param id     its id ({@code namespace:path} for an add-on's); its name and lines are {@code aura.wildercord.blade_trait.<id>},
	 *               {@code .desc} and {@code .habit} (':' as '.')
	 * @param habit  how strongly the blade's history shows the habit it's drawn from, 0 (not at all: never offered) to 1
	 * @param worth  its worth as a share of a swordsman's strength, alone against creatures (the balance test holds these together)
	 * @param pvp    its worth against another player (0 for one that never touches a player)
	 * @param reason why {@link #worth} is what it is, in a line
	 */
	public record Trait(String id, ToDoubleFunction<History> habit, double worth, double pvp, String reason) {
		public String nameKey() {
			return "aura.wildercord.blade_trait." + id.replace(':', '.');
		}
	}

	private static double clamp01(double x) {
		return Math.max(0, Math.min(1, x));
	}

	private static double share(int part, int whole) {
		return whole <= 0 ? 0 : part / (double) whole;
	}

	private static final Map<String, Trait> TRAITS = new LinkedHashMap<>();

	private static void builtIn(Trait trait) {
		TRAITS.put(trait.id(), trait);
	}

	static {
		builtIn(new Trait(WELL_WORN, h -> {
			String fav = favourite(h.arts());
			int plays = h.arts().getOrDefault(fav, 0);
			return plays < 25 ? 0 : clamp01(share(plays, h.artPlays()) * 1.4) * clamp01(plays / 80.0);
		}, 0.040, 0.030, "Its favourite art (often a third of the arts played) a sixth cheaper and back sooner: about a twentieth more of what "
			+ "that art does, a little less of the whole"));
		builtIn(new Trait(CLOSING_STROKE, h -> {
			int f = h.count(FINISHERS);
			return f < 15 ? 0 : 0.8 * clamp01(f / 60.0) + 0.2 * clamp01(3.0 * share(f, Math.max(20, h.count(KILLS))));
		}, 0.035, 0.020, "Half again the aura back from a finisher (8 to 13 more at the top stages) and 4 momentum: an art more every few "
			+ "finishers, and its own aura only"));
		builtIn(new Trait(SUNDERING_STEEL, h -> {
			int b = h.count(BROKEN_STANCES);
			return b < 25 ? 0 : clamp01(b / 90.0);
		}, 0.040, 0.025, "Stances a ninth sooner broken: a finisher and its opening sooner in every fight (half that on a player, under "
			+ "the stance caps)"));
		builtIn(new Trait(RIPOSTE, h -> {
			int g = h.count(GUARDS);
			return g < 20 ? 0 : clamp01(g / 70.0);
		}, 0.030, 0.030, "A blow a sixth harder after each perfect guard, which comes every few seconds in a hard fight; against a player half "
			+ "that, inside the bonus cap"));
		builtIn(new Trait(WIND_STEP, h -> {
			int s = h.count(STEPS);
			return s < 30 ? 0 : clamp01(s / 120.0);
		}, 0.030, 0.025, "Aura Step a quarter cheaper and back sooner: more ways out and in, no damage of its own"));
		builtIn(new Trait(LONG_CRESCENT, h -> {
			int s = h.count(SLASHES);
			return s < 40 ? 0 : clamp01(s / 150.0);
		}, 0.035, 0.025, "Aura Slash a fifth cheaper and a fifth further: more slashes in a fight, and foes reached sooner"));
		builtIn(new Trait(INKBOUND, h -> {
			int t = h.count(TECHNIQUES);
			return t < 40 ? 0 : clamp01(t / 150.0) * clamp01(0.5 + share(t, t + h.artPlays()));
		}, 0.035, 0.025, "Techniques a tenth cheaper and ranked a fifth faster: their worth is already the arts' own, so a tenth of their "
			+ "share of a fight"));
		builtIn(new Trait(SECOND_BLAZE, h -> {
			int a = h.count(AWAKENINGS);
			return a < 5 ? 0 : clamp01(a / 20.0);
		}, 0.025, 0.025, "The awakening back a quarter sooner and a quarter less spent after it: a third more awakenings in a long day of "
			+ "fighting, never a longer one"));
		builtIn(new Trait(MOUNTAINFELLER, h -> {
			int bosses = h.count(BOSSES);
			int strong = h.count(STRONG);
			return bosses < 2 && strong < 15 ? 0 : clamp01((bosses * 6.0 + strong) / 60.0);
		}, 0.035, 0.0, "A tenth harder and stances worn faster, but only against bosses and Runebound foes: never a player"));
		builtIn(new Trait(GRAVEWARDEN, h -> {
			int u = h.count(UNDEAD);
			int k = h.count(KILLS);
			return u < 50 || share(u, k) < 0.4 ? 0 : clamp01(u / 150.0) * clamp01(share(u, k) * 1.5);
		}, 0.040, 0.0, "An eighth harder against the undead (about a third of what a swordsman fights, more by night): never a player"));
		builtIn(new Trait(LAST_LIGHT, h -> {
			int l = h.count(LOW);
			return l < 10 || share(l, h.count(KILLS)) < 0.06 ? 0 : clamp01(l / 40.0);
		}, 0.030, 0.030, "A twelfth less harm while below a third of your health, where it matters most; against everyone alike"));
		builtIn(new Trait(MOONWAKE, h -> {
			int n = h.count(NIGHT);
			int k = h.count(KILLS);
			return n < 60 || share(n, k) < 0.5 ? 0 : clamp01(n / 200.0) * clamp01(share(n, k) * 1.4);
		}, 0.025, 0.015, "A quarter more aura from blows at night, half the time: a few more arts each night, nothing on the blows themselves"));
		builtIn(new Trait(RALLYING_STEEL, h -> {
			int al = h.count(ALLIED);
			return al < 30 ? 0 : clamp01(al / 100.0);
		}, 0.030, 0.0, "Allied swordsmen near get 5 momentum from each of your finishers: nothing alone, a little for each ally"));
	}

	/** The built-in traits, in the page's order. */
	public static final List<String> BUILT_IN_TRAITS = List.copyOf(TRAITS.keySet());

	/** Every trait (built in, then an add-on's), in order. */
	public static synchronized List<Trait> traits() {
		return List.copyOf(TRAITS.values());
	}

	public static synchronized Optional<Trait> trait(String id) {
		return Optional.ofNullable(id == null ? null : TRAITS.get(id));
	}

	/**
	 * Adds an add-on's trait (its id namespaced, so it never takes a built-in one's place). What it does is the add-on's own: it reads
	 * {@code AuraApi.bladeTrait(player)} where its effect lives.
	 */
	public static synchronized void register(Trait trait) {
		if (!trait.id().contains(":")) {
			throw new IllegalArgumentException("A trait added from outside needs a namespaced id: " + trait.id());
		}
		if (TRAITS.containsKey(trait.id())) {
			throw new IllegalArgumentException("Trait already registered: " + trait.id());
		}
		TRAITS.put(trait.id(), trait);
	}

	/** How each method leans: a trait it favours is offered more readily (an add-on's method leans no way). */
	private static final Map<String, Map<String, Double>> METHOD_LEAN = Map.of(
		"ember", Map.of(WELL_WORN, 1.2, CLOSING_STROKE, 1.1),
		"rime", Map.of(RIPOSTE, 1.3, SUNDERING_STEEL, 1.05),
		"thunder", Map.of(RIPOSTE, 1.2, LONG_CRESCENT, 1.1),
		"gale", Map.of(LONG_CRESCENT, 1.3, WIND_STEP, 1.2),
		"stone", Map.of(SUNDERING_STEEL, 1.4, MOUNTAINFELLER, 1.1),
		"verdant", Map.of(LAST_LIGHT, 1.3, RALLYING_STEEL, 1.1),
		"hollow", Map.of(SUNDERING_STEEL, 1.2, WIND_STEP, 1.1),
		"starlit", Map.of(CLOSING_STROKE, 1.3, INKBOUND, 1.1),
		"hourglass", Map.of(SECOND_BLAZE, 1.3, WELL_WORN, 1.1),
		"crimson", Map.of(LAST_LIGHT, 1.3, CLOSING_STROKE, 1.1));
	/** How each Way leans. */
	private static final Map<String, Map<String, Double>> WAY_LEAN = Map.of(
		WayRules.BLADE, Map.of(CLOSING_STROKE, 1.3, LONG_CRESCENT, 1.2, SUNDERING_STEEL, 1.1),
		WayRules.BULWARK, Map.of(RIPOSTE, 1.3, LAST_LIGHT, 1.2),
		WayRules.SHADOWSTEP, Map.of(WIND_STEP, 1.3, CLOSING_STROKE, 1.1),
		WayRules.BANNER, Map.of(RALLYING_STEEL, 1.5, LAST_LIGHT, 1.1));

	public static double methodLean(String method, String trait) {
		return METHOD_LEAN.getOrDefault(method, Map.of()).getOrDefault(trait, 1.0);
	}

	public static double wayLean(String way, String trait) {
		return WAY_LEAN.getOrDefault(way, Map.of()).getOrDefault(trait, 1.0);
	}

	/** How readily {@code trait} is offered to a blade with this history: its habit, leaned by the method and the Way (0: never). */
	public static double weight(Trait trait, History history) {
		double habit;
		try {
			habit = trait.habit().applyAsDouble(history);
		} catch (RuntimeException e) {
			return 0;
		}
		if (!(habit > 0)) {
			return 0;
		}
		return Math.min(1, habit) * methodLean(history.method(), trait.id()) * wayLean(history.way(), trait.id());
	}

	/** How many traits a blade offers when it awakens. */
	public static final int OFFER = 3;
	/** The offer when nothing shows strongly enough (a blade that somehow awakened without a habit). */
	private static final List<String> FALLBACK = List.of(CLOSING_STROKE, SUNDERING_STEEL, RIPOSTE);

	/**
	 * The traits a blade offers: the {@link #OFFER} its history shows most strongly, the same every time for the same history and
	 * blade ({@code seed}: ties go by it). Well-Worn Verse only when an art has been played. When nothing shows at all, three that
	 * suit any blade.
	 */
	public static List<String> offer(History history, long seed) {
		List<Map.Entry<String, Double>> scored = new ArrayList<>();
		for (Trait trait : traits()) {
			double w = weight(trait, history);
			if (w > 0) {
				scored.add(Map.entry(trait.id(), w));
			}
		}
		scored.sort(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed()
			.thenComparingLong(e -> mixed(seed, e.getKey())));
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, Double> e : scored) {
			if (out.size() >= OFFER) {
				break;
			}
			out.add(e.getKey());
		}
		return out.isEmpty() ? FALLBACK : List.copyOf(out);
	}

	/** A trait's id hashed with a blade's seed, for ties that don't depend on the order things were registered in. */
	private static long mixed(long seed, String id) {
		long z = seed ^ (id.hashCode() * 0x9E3779B97F4A7C15L);
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** The worth of each built-in trait, for the balance test: id to (alone, against a player). */
	public static Map<String, double[]> worths() {
		Map<String, double[]> out = new LinkedHashMap<>();
		for (Trait t : traits()) {
			if (!t.id().contains(":")) {
				out.put(t.id(), new double[] {t.worth(), t.pvp()});
			}
		}
		return out;
	}

	/** Changing a chosen trait for another the blade offered costs this many experience levels (the first choice, and one at Soulforged, nothing). */
	public static final int RECHOOSE_LEVELS = 10;

	// ================================================================== names

	/** A name's longest (as a technique's: the same cleaning, {@link TechniqueRules#cleanName}). */
	public static final int MAX_NAME = TechniqueRules.MAX_NAME;

	/** A name as its swordsman typed it, made safe to show anyone. */
	public static String cleanName(String typed) {
		return TechniqueRules.cleanName(typed);
	}

	/**
	 * What a blade's suggested names are drawn from: its swordsman's element and Way, its favourite art's own word, where it was bonded
	 * (the biome's id), what it has felled most, and its own seed.
	 */
	public record NameSeed(String element, String way, String artWord, String biome, String foe, long seed) {
		public NameSeed {
			element = element == null ? "" : element;
			way = way == null ? "" : way;
			artWord = artWord == null ? "" : artWord;
			biome = biome == null ? "" : biome;
			foe = foe == null ? "" : foe;
		}
	}

	/** First words, by element (the method's own); a blade of no element rings plain steel. */
	private static final Map<String, List<String>> ELEMENT_WORDS = Map.ofEntries(
		Map.entry("fire", List.of("Ember", "Cinder", "Ash", "Kindle", "Pyre", "Soot", "Flare")),
		Map.entry("frost", List.of("Rime", "Hoar", "Sleet", "Floe", "Chill", "Snowfall")),
		Map.entry("storm", List.of("Spark", "Thunder", "Static", "Stormlit", "Crackle", "Rumble")),
		Map.entry("wind", List.of("Gale", "Squall", "Gust", "Breeze", "Draft", "Whirl")),
		Map.entry("earth", List.of("Crag", "Granite", "Basalt", "Flint", "Shale", "Boulder")),
		Map.entry("life", List.of("Thorn", "Briar", "Bramble", "Fern", "Moss", "Bloom")),
		Map.entry("void", List.of("Hollow", "Gloam", "Umbra", "Rift", "Null", "Dim")),
		Map.entry("arcane", List.of("Star", "Comet", "Lumen", "Meteor", "Glimmer", "Nova")),
		Map.entry("time", List.of("Hour", "Dial", "Echo", "Chime", "Moment", "Dusk")),
		Map.entry("blood", List.of("Crimson", "Scarlet", "Ruby", "Vein", "Garnet", "Rust")));
	private static final List<String> PLAIN_WORDS = List.of("Steel", "Iron", "Grey", "Whet", "Hilt", "Quiet");
	/** First words by Way. */
	private static final Map<String, List<String>> WAY_WORDS = Map.of(
		WayRules.BLADE, List.of("Keen", "Bright", "True"),
		WayRules.BULWARK, List.of("Ward", "Bastion", "Steadfast"),
		WayRules.SHADOWSTEP, List.of("Shade", "Hush", "Wisp"),
		WayRules.BANNER, List.of("Rally", "Herald", "Kinsman"));
	/** A word for where it was bonded, by the biome's path (contains): checked in order. */
	private static final List<String[]> PLACE_WORDS = List.of(
		new String[] {"cherry", "Petal", "the Petals"}, new String[] {"mushroom", "Spore", "the Spores"}, new String[] {"deep_dark", "Hush", "the Deep"},
		new String[] {"dark_forest", "Gloam", "the Gloaming"}, new String[] {"mangrove", "Mire", "the Mire"}, new String[] {"swamp", "Mire", "the Mire"},
		new String[] {"badlands", "Mesa", "the Mesas"}, new String[] {"desert", "Dune", "the Dunes"}, new String[] {"savanna", "Sun", "the Long Sun"},
		new String[] {"jungle", "Vine", "the Vines"}, new String[] {"bamboo", "Vine", "the Vines"}, new String[] {"snow", "Winter", "Winter"},
		new String[] {"frozen", "Winter", "Winter"}, new String[] {"ice", "Winter", "Winter"}, new String[] {"taiga", "Pine", "the Pines"},
		new String[] {"grove", "Pine", "the Pines"}, new String[] {"peak", "Summit", "the Peaks"}, new String[] {"hills", "Summit", "the Hills"},
		new String[] {"slopes", "Summit", "the Slopes"}, new String[] {"ocean", "Tide", "the Tide"}, new String[] {"beach", "Tide", "the Shore"},
		new String[] {"river", "Ford", "the Ford"}, new String[] {"birch", "Birch", "the Birches"}, new String[] {"forest", "Grove", "the Grove"},
		new String[] {"meadow", "Meadow", "the Meadow"}, new String[] {"plains", "Meadow", "the Meadow"}, new String[] {"cave", "Delve", "the Deep"},
		new String[] {"nether", "Brimstone", "the Brimstone"}, new String[] {"end", "Farlight", "the Far Lands"});
	/** A word for what it has felled most. */
	private static final Map<String, String> FOE_WORDS = Map.of(UNDEAD, "Grave", BOSSES, "Giant", PLAYERS, "Duel", NIGHT, "Moon", STRONG, "Mighty",
		LOW, "Last");
	/** What a compound name ends in. */
	private static final List<String> ENDINGS = List.of("wake", "song", "call", "fang", "heart", "fall", "light", "veil", "thorn", "mark", "crest",
		"sworn", "reach", "bite", "verse", "glow", "spire", "tide", "kin", "rest", "wing", "shard", "vow", "watch", "stride", "gleam", "ward",
		"path", "bloom", "flight");
	/** What a two-word name ends in. */
	private static final List<String> NOUNS = List.of("Verse", "Oath", "Promise", "Vigil", "Requiem", "Answer", "Lantern", "Hymn", "Witness",
		"Ember", "Thread", "Covenant", "Ballad", "Refrain", "Errand", "Pilgrim", "Vow", "Sentinel", "Wayfarer", "Memory", "Testament",
		"Hearth", "Keepsake", "Reckoning");
	/** What an "X of Y" name begins with. */
	private static final List<String> OF_NOUNS = List.of("Oath", "Song", "Vigil", "Hymn", "Edge", "Lantern", "Ballad", "Witness");

	/**
	 * A name the blade suggests for itself, built from its history, the same every time for the same seed and {@code salt} (0 the first
	 * it offers; the page's "another" counts up). Always within {@link #MAX_NAME} letters and clean; every word is the mod's own (the
	 * banks above), so a suggestion is never a name borrowed from anywhere else.
	 */
	public static String suggest(NameSeed seed, int salt) {
		for (int attempt = 0; attempt < 24; attempt++) {
			String name = compose(seed, salt * 31 + attempt);
			if (!name.isEmpty() && name.length() <= MAX_NAME && cleanName(name).equals(name)) {
				return name;
			}
		}
		return "Quiet Steel";
	}

	/** One try at a name: one of four shapes, its words drawn from the history's banks by the seed. */
	private static String compose(NameSeed seed, int salt) {
		long r = mixed(seed.seed(), "name:" + salt);
		List<String> firsts = new ArrayList<>(ELEMENT_WORDS.getOrDefault(seed.element(), PLAIN_WORDS));
		String place = null;
		String placeOf = null;
		for (String[] p : PLACE_WORDS) {
			if (!seed.biome().isEmpty() && seed.biome().contains(p[0])) {
				place = p[1];
				placeOf = p[2];
				break;
			}
		}
		String foe = FOE_WORDS.get(seed.foe());
		// The other banks join the element's now and then, so a blade's names speak of its Way, its place, its foes and its art too.
		List<String> extra = new ArrayList<>();
		extra.addAll(WAY_WORDS.getOrDefault(seed.way(), List.of()));
		if (place != null) {
			extra.add(place);
		}
		if (foe != null) {
			extra.add(foe);
		}
		if (!seed.artWord().isEmpty()) {
			extra.add(seed.artWord());
		}
		int shape = (int) Math.floorMod(r, 4L);
		String first = pick(firsts, r >>> 8);
		if (!extra.isEmpty() && Math.floorMod(r >>> 20, 5L) < 2) {
			first = pick(extra, r >>> 24);
		}
		return switch (shape) {
			// A long first word runs on badly into an ending ("Snowfallmark"): it stands as a word of its own instead.
			case 0, 1 -> first.length() <= 6 ? compound(first, pick(ENDINGS, r >>> 32)) : first + " " + pick(NOUNS, r >>> 36);
			case 2 -> first + " " + pick(NOUNS, r >>> 36);
			default -> {
				String of = pick(OF_NOUNS, r >>> 40);
				if (placeOf != null && Math.floorMod(r >>> 44, 2L) == 0) {
					yield of + " of " + placeOf;
				}
				yield first + " " + pick(NOUNS, r >>> 44);
			}
		};
	}

	/** Two words run together as one name ("Cinder" and "wake": Cinderwake), never doubling a letter where they meet into three. */
	private static String compound(String first, String ending) {
		String a = first.replace(" ", "");
		if (!a.isEmpty() && !ending.isEmpty() && Character.toLowerCase(a.charAt(a.length() - 1)) == ending.charAt(0)
				&& a.length() > 1 && Character.toLowerCase(a.charAt(a.length() - 2)) == ending.charAt(0)) {
			a = a.substring(0, a.length() - 1);
		}
		return a + ending;
	}

	private static String pick(List<String> words, long r) {
		return words.get((int) Math.floorMod(r, (long) words.size()));
	}

	/** The word an art lends a blade's name (Sunfall: "Sun"), from its id's first part; "" for none. */
	public static String artWord(String artId) {
		if (artId == null || artId.isBlank() || artId.startsWith("technique_")) {
			return "";
		}
		String path = artId.contains(":") ? artId.substring(artId.indexOf(':') + 1) : artId;
		String first = path.split("_")[0];
		// The common arts (first_art...) lend nothing: their names are only their place in the line.
		if (first.length() < 3 || first.length() > 9 || Set.of("first", "second", "third", "fourth", "final", "art").contains(first)) {
			return "";
		}
		return Character.toUpperCase(first.charAt(0)) + first.substring(1);
	}

	/** What a blade has felled most, as a name's word reads it: undead, bosses, players, night, strong or low ("" for nothing notable). */
	public static String foeKind(Map<String, Integer> counts) {
		int kills = counts == null ? 0 : counts.getOrDefault(KILLS, 0);
		if (kills <= 0) {
			return "";
		}
		if (counts.getOrDefault(BOSSES, 0) >= 3) {
			return BOSSES;
		}
		String best = "";
		double most = 0.3;
		for (String id : List.of(UNDEAD, NIGHT, PLAYERS, STRONG, LOW)) {
			double s = share(counts.getOrDefault(id, 0), kills);
			if (s > most) {
				most = s;
				best = id;
			}
		}
		return best;
	}

	// ================================================================== which weapons bond

	/**
	 * Whether a weapon of these kinds can be bonded: it carries aura (the {@code aura_weapons} tag), it wears (has durability: a blade is
	 * kept, not used up) and it stays in the hand (the trident, thrown and left lying, is out: {@code bondable_blades} leaves it out). The
	 * server decides with the {@code wildercord:bondable_blades} tag; this is the part that isn't data.
	 */
	public static boolean bondable(boolean tagged, boolean damageable, int count) {
		return tagged && damageable && count == 1;
	}
}
