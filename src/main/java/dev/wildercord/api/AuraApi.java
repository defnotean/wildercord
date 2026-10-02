package dev.wildercord.api;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.MethodSources;
import dev.wildercord.aura.StringReader;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Aura, the swordsman's path, for the rest of the mod and for add-ons: the stage registry, the technique registry, hooks on
 * aura gained and spent, and breathing methods and where they're found. Everything here runs on the server thread unless it
 * says otherwise; register while the mod initialises (the client reads the registries too, for the Aura page).
 *
 * <ul>
 *   <li><b>Stages</b> ({@link #registerStage}, {@link #allowTrial}, {@link #completeTrial}): all five are open, Glow to
 *       Sovereign. A stage needs a breakthrough trial to be reached: the built-in ones (see {@link #allowTrial}) can be allowed
 *       for any stage, or a trial of your own can call {@link #completeTrial} when it's met.</li>
 *   <li><b>Techniques</b> ({@link #registerTechnique}): what the Aura key does. A tap, a tap while sneaking, a double tap and
 *       a hold each go to the highest-stage technique for that trigger the player has reached. Built in: Aura Guard (Flow,
 *       sneak and tap), Aura Slash (Edge, tap), Aura Step (Form, double tap) and Dominion (Sovereign, hold).</li>
 *   <li><b>Aura</b> ({@link #gain}, {@link #spend}, {@link #onGain}, {@link #onSpend}): fill or spend a player's aura with
 *       every rule applying (the server's rate, the method's passive, the stage's capacity, backlash when spent past empty),
 *       and hear of or change each change.</li>
 *   <li><b>Methods</b> ({@link #registerMethod}, {@link #addMethodSource}, {@link #grantMethod}, {@link #manual}): more
 *       breathing methods, more places manuals turn up, and a way to teach one outright (a duelist's lesson).</li>
 *   <li><b>Sword strings</b> ({@link #registerString}, {@link #onString}, {@link #addStringSource}): arts set off by a short
 *       run of ordinary swings ({@link SwordString}), read by the player's client and checked and performed by the server.</li>
 *   <li><b>A method's arts</b> ({@link #registerArts}, {@link ArtSlot}, {@link #arts}, {@link #gateFinalArts}): each breathing
 *       method answers the five art strings (one a stage, the same for everyone) with arts of its own; a method without any
 *       plays the five common arts ({@code aura.PlaceholderArts}). Ember, Rime, Thunder, Gale and Stone have theirs
 *       ({@code aura.arts}).</li>
 *   <li><b>Feel</b> ({@link AuraFx}, {@link #registerSounds}): how aura looks and sounds, shared by every technique: a blade's
 *       trail, an impact (a flash, and a brief hit-stop for the striker and a struck player), a technique's banner, a burst of light,
 *       the body's aura flaring, and each method's own swing, impact and technique sounds. Each client draws them as it sees them,
 *       small and low in a swordsman's own first-person view. An art performed through a sword string gets its banner, a flare and
 *       its method's technique sound by itself; {@code AuraFx.art(player)} adds its trail, impacts and bursts.</li>
 * </ul>
 *
 * @since 0.9
 */
public final class AuraApi {
	private AuraApi() {}

	// ------------------------------------------------------------------ techniques

	/** How a technique is set off with the Aura key. */
	public enum Trigger {
		/** A tap (pressed and let go within a quarter of a second). */
		TAP,
		/** Pressed while sneaking: goes off at once, on the press. */
		SNEAK_TAP,
		/**
		 * Two taps within a little under half a second. For a player who has a double-tap technique (Aura Step, from Form) a lone
		 * tap waits out that moment before it goes, so the pair goes as the double tap alone; for anyone else the first tap still
		 * went off as a tap.
		 */
		DOUBLE_TAP,
		/** Held a quarter of a second or more: sent once as the hold begins. */
		HOLD
	}

	/** What a technique does when set off; returns whether it went off (a refusal says why above the hotbar itself). */
	@FunctionalInterface
	public interface Performer {
		boolean perform(ServerPlayer player);
	}

	/**
	 * A technique of the Aura key.
	 *
	 * @param id        its id (a plain word for the mod's own, {@code namespace:path} for an add-on's); its name and description
	 *                  are the language keys {@code aura.wildercord.technique.<id>} and {@code .desc} (with ':' as '.')
	 * @param stage     the stage it opens at
	 * @param trigger   how it's set off
	 * @param cost      its usual price in aura, shown on the Aura page (the performer pays, through {@link #spend})
	 * @param performer what it does
	 */
	public record Technique(String id, int stage, Trigger trigger, double cost, Performer performer) {
		/** The language key of its name. */
		public String nameKey() {
			return "aura.wildercord.technique." + id.replace(':', '.');
		}
	}

	private static final Map<String, Technique> TECHNIQUES = new LinkedHashMap<>();

	/** Adds a technique (or replaces the one with its id). */
	public static synchronized void registerTechnique(Technique technique) {
		TECHNIQUES.put(technique.id(), technique);
	}

	/** Every technique, by stage then registration. Safe on both sides. */
	public static synchronized List<Technique> techniques() {
		List<Technique> out = new ArrayList<>(TECHNIQUES.values());
		out.sort(Comparator.comparingInt(Technique::stage));
		return out;
	}

	public static synchronized Optional<Technique> technique(String id) {
		return Optional.ofNullable(TECHNIQUES.get(id));
	}

	/** The technique {@code trigger} sets off for a player at {@code stage}: the highest-stage one reached. */
	public static synchronized Optional<Technique> techniqueFor(int stage, Trigger trigger) {
		Technique best = null;
		for (Technique t : TECHNIQUES.values()) {
			if (t.trigger() == trigger && t.stage() <= stage && (best == null || t.stage() > best.stage())) {
				best = t;
			}
		}
		return Optional.ofNullable(best);
	}

	// ------------------------------------------------------------------ stages and trials

	/** Opens a stage, or changes one (all five are open already: their capacity and threshold can be changed this way). */
	public static void registerStage(AuraStages.Stage stage) {
		AuraStages.register(stage);
	}

	private static final Map<Integer, Set<String>> TRIALS = new LinkedHashMap<>();

	static {
		for (int s = AuraRules.FLOW; s <= AuraRules.EDGE; s++) {
			allowTrial(s, AuraBreakthroughs.STILLNESS);
			allowTrial(s, AuraBreakthroughs.STRONGER_FOE);
		}
		// The top stages ask more: the stance held through a thunderstorm at a ley crossing, or a boss felled by the blade.
		for (int s = AuraRules.FORM; s <= AuraRules.SOVEREIGN; s++) {
			allowTrial(s, AuraBreakthroughs.TEMPEST);
			allowTrial(s, AuraBreakthroughs.GUARDIAN);
		}
	}

	/**
	 * Lets {@code trial} make the breakthrough into {@code stage}. The built-in trials are {@link AuraBreakthroughs#STILLNESS}
	 * (the breathing stance held unbroken at a place of power) and {@link AuraBreakthroughs#STRONGER_FOE} (a stronger foe felled
	 * by melee and aura alone) for Flow and Edge, and {@link AuraBreakthroughs#TEMPEST} (the stance held through a thunderstorm at
	 * a ley crossing) and {@link AuraBreakthroughs#GUARDIAN} (a boss felled by blade and aura alone) for Form and Sovereign. Any
	 * other id is a trial of the caller's own, met by calling {@link #completeTrial}: a won aura duel, for one, is
	 * {@link AuraBreakthroughs#DUEL}, which the duelists allow for the stage they teach.
	 */
	public static synchronized void allowTrial(int stage, String trial) {
		TRIALS.computeIfAbsent(stage, k -> new LinkedHashSet<>()).add(trial);
	}

	/** The trials that can make the breakthrough into {@code stage}, in the order they were allowed. Safe on both sides. */
	public static synchronized Set<String> trials(int stage) {
		return java.util.Collections.unmodifiableSet(new LinkedHashSet<>(TRIALS.getOrDefault(stage, Set.of())));
	}

	/**
	 * Tells aura a player has met {@code trial}: if a breakthrough waits for them and the trial is allowed for the stage it
	 * leads to, it's made now. Returns whether it was.
	 */
	public static boolean completeTrial(ServerPlayer player, String trial) {
		return AuraBreakthroughs.complete(player, trial);
	}

	// ------------------------------------------------------------------ gaining and spending

	/** Changes (or only hears of) aura about to be gained: returns the amount to gain instead. */
	@FunctionalInterface
	public interface GainHook {
		double modify(ServerPlayer player, double amount, String source);
	}

	/** Hears of aura spent: how much was paid, for what, and whether it went past empty. */
	@FunctionalInterface
	public interface SpendHook {
		void spent(ServerPlayer player, double paid, String reason, boolean backlash);
	}

	private static final List<GainHook> GAIN_HOOKS = new CopyOnWriteArrayList<>();
	private static final List<SpendHook> SPEND_HOOKS = new CopyOnWriteArrayList<>();

	public static void onGain(GainHook hook) {
		GAIN_HOOKS.add(hook);
	}

	public static void onSpend(SpendHook hook) {
		SPEND_HOOKS.add(hook);
	}

	/** The hooks registered, in order (the mod's own use). */
	public static List<GainHook> gainHooks() {
		return GAIN_HOOKS;
	}

	public static List<SpendHook> spendHooks() {
		return SPEND_HOOKS;
	}

	/**
	 * Fills a player's aura by {@code amount} (before the server's rate and the method's passive), up to their stage's
	 * capacity. {@code source} names where it came from, for the hooks ("hit", "stance", "beat", or the caller's own).
	 * Returns what was really gained.
	 */
	public static double gain(ServerPlayer player, double amount, String source) {
		return Aura.gain(player, amount, source);
	}

	/**
	 * Spends {@code cost} of a player's aura for {@code reason}. Whatever is there pays what it can; anything short of the
	 * price is backlash (brief exhaustion, never damage). Returns whether it was paid in full.
	 */
	public static boolean spend(ServerPlayer player, double cost, String reason) {
		return !Aura.spend(player, cost, reason).backlash();
	}

	/** Brings backlash on a player at once (a technique of yours failing at nothing). */
	public static void backlash(ServerPlayer player) {
		Aura.backlash(player);
	}

	// ------------------------------------------------------------------ methods and their sources

	/** Adds a breathing method (give it a namespaced id). Its manual, loot and trades work at once. */
	public static BreathingMethod registerMethod(BreathingMethod method) {
		return BreathingMethods.register(method);
	}

	/**
	 * Adds a place manuals turn up: a loot table (read as loot tables load, so register before the world starts), or a
	 * source in code, which hands its methods out through {@link #grantMethod} or {@link MethodSources.Source#draw}.
	 */
	public static MethodSources.Source addMethodSource(MethodSources.Source source) {
		return MethodSources.register(source);
	}

	/**
	 * Teaches a player a method outright, from {@code sourceId} (a duelist's lesson): exactly as reading its manual, switching
	 * cost and all, but without asking twice. Returns whether they learned it.
	 */
	public static boolean grantMethod(ServerPlayer player, String methodId, String sourceId) {
		return BreathingMethods.byId(methodId).map(method -> dev.wildercord.aura.AuraMethods.learn(player, method, sourceId, true)).orElse(false);
	}

	/** A manual of {@code methodId}. */
	public static ItemStack manual(String methodId) {
		return BreathingManualItem.of(methodId);
	}

	// ------------------------------------------------------------------ sword strings

	/**
	 * What an art does when its string is played, on the server: returns whether it went off. Its price and rest are paid only
	 * then, by the mod ({@link StringArt#cost} from the player's aura, never past empty, and {@link StringArt#cooldownTicks});
	 * a refusal of its own (nothing to act on) should say why above the hotbar and return false.
	 */
	@FunctionalInterface
	public interface ArtPerformer {
		boolean perform(ServerPlayer player, StringContext context);
	}

	/**
	 * Something an art waits on besides its stage, its rest and its price: a full aura pool, peak momentum, awakening. It's
	 * checked on both sides (the client before it asks, the server before it performs), so it must read only what the player's
	 * own client knows too: their synced attachments.
	 */
	@FunctionalInterface
	public interface ArtCondition {
		/** Never anything to wait on. */
		ArtCondition ALWAYS = player -> true;

		boolean met(Player player);

		/** The language key of the line that says what it waits on (given the art's name as {@code %s}), shown when it isn't met. */
		default String hintKey() {
			return "message.wildercord.aura.art.condition";
		}

		/** A condition with its own line. */
		static ArtCondition of(Predicate<Player> test, String hintKey) {
			return new ArtCondition() {
				@Override
				public boolean met(Player player) {
					return test.test(player);
				}

				@Override
				public String hintKey() {
					return hintKey;
				}
			};
		}
	}

	/**
	 * An art set off by a sword string. Its name and description are the language keys {@code aura.wildercord.art.<id>} and
	 * {@code .desc} (with ':' as '.').
	 *
	 * @param id            its id (a plain word for the mod's own, {@code namespace:path} for an add-on's)
	 * @param string        the swings that set it off
	 * @param stage         the stage it opens at (Glow to Sovereign)
	 * @param cost          its price in aura, paid when it goes off (the client won't ask with less, and nothing spends past empty)
	 * @param cooldownTicks how long it rests after going off
	 * @param available     whether the player has it at all, besides the stage (a method's own art: {@link #forMethod}); both sides
	 * @param condition     what else it waits on before it can go ({@link ArtCondition}); both sides
	 * @param performer     what it does, on the server
	 */
	public record StringArt(String id, SwordString string, int stage, double cost, int cooldownTicks, Predicate<Player> available,
			ArtCondition condition, ArtPerformer performer) implements StringReader.Spelled {
		public StringArt {
			if (id == null || id.isBlank()) {
				throw new IllegalArgumentException("an art needs an id");
			}
			if (string == null) {
				throw new IllegalArgumentException("art " + id + " needs a sword string");
			}
			stage = Math.max(AuraRules.GLOW, AuraRules.clampStage(stage));
			cost = Math.max(0, cost);
			cooldownTicks = Math.max(0, cooldownTicks);
			available = available == null ? player -> true : available;
			condition = condition == null ? ArtCondition.ALWAYS : condition;
			performer = performer == null ? (player, context) -> false : performer;
		}

		/** An art open to everyone who reaches its stage, waiting on nothing else. {@code string} is written as {@link SwordString#parse} reads it. */
		public static StringArt of(String id, String string, int stage, double cost, int cooldownTicks, ArtPerformer performer) {
			return new StringArt(id, SwordString.parse(string), stage, cost, cooldownTicks, null, null, performer);
		}

		/** The same art, open only to players {@code available} lets through (besides its stage). */
		public StringArt onlyFor(Predicate<Player> available) {
			return new StringArt(id, string, stage, cost, cooldownTicks, available, condition, performer);
		}

		/** The same art, open only to players who breathe {@code methodId}: a method's own art. */
		public StringArt forMethod(String methodId) {
			return onlyFor(player -> Aura.data(player).method().equals(methodId));
		}

		/** The same art, waiting on {@code condition} too. */
		public StringArt when(ArtCondition condition) {
			return new StringArt(id, string, stage, cost, cooldownTicks, available, condition, performer);
		}

		/** The language key of its name. */
		public String nameKey() {
			return "aura.wildercord.art." + id.replace(':', '.');
		}
	}

	/**
	 * What an art is told as it's performed.
	 *
	 * @param art    the art
	 * @param marks  its string's swings as the player's client read them, oldest first ({@link SwordString.Token#bit}s: whether
	 *               each was full, low, leaping, running, a counter or a step cut)
	 * @param struck the creature the string's last swing struck, by the server's own record (null for a swing at nothing); it may
	 *               have died of the blow
	 * @param at     the game time
	 */
	public record StringContext(StringArt art, List<Integer> marks, LivingEntity struck, long at) {
		public StringContext {
			marks = List.copyOf(marks);
		}

		/** Whether the string's last swing was of this kind. */
		public boolean released(SwordString.Token token) {
			return !marks.isEmpty() && token.fits(marks.getLast());
		}
	}

	/** Hears of every art performed (after it went off and was paid for): momentum, a bonded blade's resonance, a trial. */
	@FunctionalInterface
	public interface StringHook {
		void performed(ServerPlayer player, StringArt art, StringContext context);
	}

	/**
	 * Arts a player has of their own, besides the registered ones (techniques they wrote, say): asked on both sides each time a
	 * string is read or checked, so it must read only what the player's own client knows too, and be quick.
	 */
	@FunctionalInterface
	public interface StringSource {
		List<StringArt> strings(Player player);
	}

	private static final Map<String, StringArt> ARTS = new LinkedHashMap<>();
	private static final List<StringSource> STRING_SOURCES = new CopyOnWriteArrayList<>();
	private static final List<StringHook> STRING_HOOKS = new CopyOnWriteArrayList<>();

	/**
	 * Adds an art set off by a sword string (or replaces the one with its id). Register while the mod initialises, on both
	 * sides: the client reads strings and the server performs them. A string that a shorter one cuts short (see
	 * {@link SwordString#cutBy}), or that another art already uses at a stage both reach, is allowed but logged: check
	 * {@link #conflicts} first. A breathing method's own arts go in through {@link #registerArts} instead.
	 */
	public static synchronized void registerString(StringArt art) {
		for (StringArt other : conflicts(art.string(), art.id(), OWNERS.getOrDefault(art.id(), ""))) {
			dev.wildercord.Wildercord.LOGGER.warn("Sword string '{}' of art {} meets '{}' of art {}: one may never be played as written",
				art.string(), art.id(), other.string(), other.id());
		}
		ARTS.put(art.id(), art);
	}

	/** Takes an art out (a method's own leaves its set too). Returns whether there was one. */
	public static synchronized boolean unregisterString(String id) {
		String method = OWNERS.remove(id);
		if (method != null) {
			List<String> set = METHOD_ARTS.get(method);
			if (set != null) {
				set.remove(id);
				if (set.isEmpty()) {
					METHOD_ARTS.remove(method);
				}
			}
		}
		return ARTS.remove(id) != null;
	}

	// ------------------------------------------------------------------ a breathing method's own arts

	/**
	 * The five arts' places, one a stage, the same strings for every method so a swordsman learns them once: each method answers
	 * them its own way ({@link #registerArts}). A method without its own arts plays the five common ones ({@code aura.PlaceholderArts})
	 * on the same strings.
	 */
	public enum ArtSlot {
		/** The First Art (Glow): swing, swing, a low swing. */
		FIRST("swing swing low", AuraRules.GLOW),
		/** The Second Art (Flow): a leaping swing, then a low swing. */
		SECOND("leap low", AuraRules.FLOW),
		/** The Third Art (Edge): a counter, the first swing after a perfect Aura Guard. */
		THIRD("counter", AuraRules.EDGE),
		/** The Fourth Art (Form): a step cut, the first swing after an Aura Step. */
		FOURTH("step", AuraRules.FORM),
		/** The Final Art (Sovereign): three full swings and a low one, gated by {@link #FINAL_GATE}. */
		FINAL("full full full low", AuraRules.SOVEREIGN);

		/** The slot's string. */
		public final SwordString string;
		/** The stage it opens at. */
		public final int stage;

		ArtSlot(String string, int stage) {
			this.string = SwordString.parse(string);
			this.stage = stage;
		}

		/** An art in this slot: its string and stage, the price and rest given, and for the Final Art its gate ({@link #FINAL_GATE}). */
		public StringArt art(String id, double cost, int cooldownTicks, ArtPerformer performer) {
			StringArt art = new StringArt(id, string, stage, cost, cooldownTicks, null, null, performer);
			return this == FINAL ? art.when(FINAL_GATE) : art;
		}

		/** The slot an art sits in (its string and stage), if it sits in one. */
		public static Optional<ArtSlot> of(StringArt art) {
			for (ArtSlot slot : values()) {
				if (slot.string.equals(art.string()) && slot.stage == art.stage()) {
					return Optional.of(slot);
				}
			}
			return Optional.empty();
		}
	}

	private static volatile ArtCondition finalGate = null;

	/**
	 * What every Final Art waits on: a full aura pool by default (nine tenths, as {@code aura.PlaceholderArts.FULL_POOL}). One
	 * condition for them all, so the momentum and awakening still to come change it in one place ({@link #gateFinalArts}).
	 */
	public static final ArtCondition FINAL_GATE = new ArtCondition() {
		@Override
		public boolean met(Player player) {
			return gate().met(player);
		}

		@Override
		public String hintKey() {
			return gate().hintKey();
		}
	};

	private static ArtCondition gate() {
		ArtCondition gate = finalGate;
		return gate == null ? dev.wildercord.aura.PlaceholderArts.FULL_POOL : gate;
	}

	/** Changes what every Final Art waits on (null puts the full pool back). Both sides must agree: call it while the mod initialises. */
	public static void gateFinalArts(ArtCondition gate) {
		finalGate = gate;
	}

	/** Each method's own arts (by method, in slot order) and whose each art is. */
	private static final Map<String, List<String>> METHOD_ARTS = new LinkedHashMap<>();
	private static final Map<String, String> OWNERS = new LinkedHashMap<>();

	/**
	 * Gives breathing method {@code methodId} its own arts (normally five, one a slot: {@link ArtSlot#art}), replacing any it had.
	 * Each is registered as a sword string open only to the method's swordsmen ({@link StringArt#forMethod}), and the common arts
	 * step aside for them. Register on both sides while the mod initialises. Returns the arts as registered.
	 */
	public static synchronized List<StringArt> registerArts(String methodId, List<StringArt> arts) {
		if (methodId == null || methodId.isBlank()) {
			throw new IllegalArgumentException("a method's arts need its id");
		}
		List<String> old = METHOD_ARTS.remove(methodId);
		if (old != null) {
			for (String id : old) {
				OWNERS.remove(id);
				ARTS.remove(id);
			}
		}
		List<StringArt> registered = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		for (StringArt art : arts) {
			StringArt own = art.forMethod(methodId);
			OWNERS.put(own.id(), methodId);
			ids.add(own.id());
			registerString(own);
			registered.add(own);
		}
		METHOD_ARTS.put(methodId, ids);
		return List.copyOf(registered);
	}

	/** Whether breathing method {@code methodId} has arts of its own (otherwise its swordsmen play the common ones). Both sides. */
	public static synchronized boolean hasArts(String methodId) {
		return methodId != null && METHOD_ARTS.containsKey(methodId);
	}

	/**
	 * The arts a swordsman of {@code methodId} plays, by stage: the method's own, or the common arts for a method without any.
	 * Safe on both sides (the Aura page lists every method's this way).
	 */
	public static synchronized List<StringArt> arts(String methodId) {
		List<String> ids = methodId == null ? null : METHOD_ARTS.get(methodId);
		List<StringArt> out = new ArrayList<>();
		if (ids == null) {
			for (String id : dev.wildercord.aura.PlaceholderArts.IDS) {
				StringArt art = ARTS.get(id);
				if (art != null) {
					out.add(art);
				}
			}
		} else {
			for (String id : ids) {
				StringArt art = ARTS.get(id);
				if (art != null) {
					out.add(art);
				}
			}
		}
		out.sort(Comparator.comparingInt(StringArt::stage));
		return out;
	}

	/** The breathing method whose own art {@code artId} is, or "" (a common art, or anything else). */
	public static synchronized String artMethod(String artId) {
		return OWNERS.getOrDefault(artId, "");
	}

	/** Whether {@code id} is one of the common arts a method without its own plays. */
	private static boolean common(String id) {
		return dev.wildercord.aura.PlaceholderArts.IDS.contains(id);
	}

	/**
	 * Whether two arts could ever be open to the same swordsman: the same method, or one open to every method; a common art steps
	 * aside for a method's own (it's only played where there are none).
	 */
	private static boolean overlap(String idA, String methodA, String idB, String methodB) {
		if (!methodA.isEmpty() && !methodB.isEmpty()) {
			return methodA.equals(methodB);
		}
		if (common(idA) && !methodB.isEmpty() || common(idB) && !methodA.isEmpty()) {
			return false;
		}
		return true;
	}

	/** Every registered art, by stage then registration. Safe on both sides. */
	public static synchronized List<StringArt> strings() {
		List<StringArt> out = new ArrayList<>(ARTS.values());
		out.sort(Comparator.comparingInt(StringArt::stage));
		return out;
	}

	public static synchronized Optional<StringArt> string(String id) {
		return Optional.ofNullable(ARTS.get(id));
	}

	/**
	 * Every art {@code player} can play now (their stage reached, open to them), the registered ones by stage, then their own
	 * from the sources: what their strings are read against. Safe on both sides.
	 */
	public static List<StringArt> stringsOf(Player player) {
		int stage = Aura.stage(player);
		List<StringArt> out = new ArrayList<>();
		for (StringArt art : withSources(player)) {
			if (art.stage() <= stage && art.available().test(player)) {
				out.add(art);
			}
		}
		return out;
	}

	/**
	 * Every art open to {@code player} whatever their stage (registered, then their own), by stage: what the Aura page lists, the
	 * ones not yet reached faint. Safe on both sides.
	 */
	public static List<StringArt> allStringsOf(Player player) {
		List<StringArt> out = new ArrayList<>();
		for (StringArt art : withSources(player)) {
			if (art.available().test(player)) {
				out.add(art);
			}
		}
		out.sort(Comparator.comparingInt(StringArt::stage));
		return out;
	}

	/** The art with this id that {@code player} could have (registered, or one of their own), whatever their stage. */
	public static Optional<StringArt> artOf(Player player, String id) {
		for (StringArt art : withSources(player)) {
			if (art.id().equals(id)) {
				return Optional.of(art);
			}
		}
		return Optional.empty();
	}

	private static List<StringArt> withSources(Player player) {
		List<StringArt> all = strings();
		for (StringSource source : STRING_SOURCES) {
			try {
				List<StringArt> own = source.strings(player);
				if (own != null) {
					all.addAll(own);
				}
			} catch (RuntimeException e) {
				dev.wildercord.Wildercord.LOGGER.warn("A sword string source threw; skipping it", e);
			}
		}
		return all;
	}

	/**
	 * The registered arts whose strings would get in the way of {@code string}: the same swings, a shorter string that cuts it
	 * short, or a longer one it cuts short ({@link SwordString#cutBy}). An art with id {@code except} is left out (the one being
	 * replaced). For a writing screen to warn before a player settles on a string.
	 */
	public static synchronized List<StringArt> conflicts(SwordString string, String except) {
		return conflicts(string, except, "");
	}

	/**
	 * The registered arts whose strings would get in the way of {@code string} for a swordsman of {@code method} ("" for an art
	 * open to every method): the other methods' own arts never do.
	 */
	public static synchronized List<StringArt> conflicts(SwordString string, String except, String method) {
		List<StringArt> out = new ArrayList<>();
		String own = method == null ? "" : method;
		for (StringArt other : ARTS.values()) {
			if (other.id().equals(except) || !overlap(except, own, other.id(), OWNERS.getOrDefault(other.id(), ""))) {
				continue;
			}
			if (other.string().equals(string) || string.cutBy(other.string()) || other.string().cutBy(string)) {
				out.add(other);
			}
		}
		return out;
	}

	/**
	 * The arts {@code player} has (registered and their own, whatever their stage) whose strings would get in the way of
	 * {@code string}: what a screen for writing a technique of one's own should warn of. Safe on both sides.
	 */
	public static List<StringArt> conflicts(Player player, SwordString string, String except) {
		List<StringArt> out = new ArrayList<>();
		for (StringArt other : allStringsOf(player)) {
			if (other.id().equals(except)) {
				continue;
			}
			if (other.string().equals(string) || string.cutBy(other.string()) || other.string().cutBy(string)) {
				out.add(other);
			}
		}
		return out;
	}

	/** Adds arts players have of their own (see {@link StringSource}). */
	public static void addStringSource(StringSource source) {
		STRING_SOURCES.add(source);
	}

	/** Hears of every art performed. */
	public static void onString(StringHook hook) {
		STRING_HOOKS.add(hook);
	}

	/** The string hooks registered, in order (the mod's own use). */
	public static List<StringHook> stringHooks() {
		return STRING_HOOKS;
	}

	/** When {@code player}'s art {@code id} is ready again (game time; past or 0 when it's ready). Safe on both sides for the player's own client. */
	public static long artReadyAt(Player player, String id) {
		return SwordStrings.readyAt(player, id);
	}

	// ------------------------------------------------------------------ feel

	/**
	 * Gives a breathing method its own sounds: a blade's swing, a blow landing and a technique loosed, as kit sound names (an
	 * add-on's own, shipped in its sounds.json, or another method's). A method without any rings plain steel
	 * ({@link AuraFx#STEEL}). The built-in methods have theirs ({@code aura_<method>_swing}, {@code _impact}, {@code _art}).
	 */
	public static void registerSounds(String methodId, AuraFx.SoundFamily family) {
		AuraFx.registerFamily(methodId, family);
	}

	// ------------------------------------------------------------------ reading (both sides)

	public static int stage(Player player) {
		return Aura.stage(player);
	}

	/** The player's breathing method's id, or "". */
	public static String method(Player player) {
		return Aura.data(player).method();
	}

	public static float aura(Player player) {
		return Aura.aura(player);
	}

	public static int capacity(Player player) {
		return Aura.capacity(player);
	}

	/** The player's aura's colour now (0xRRGGBB), or 0 without a method. */
	public static int color(Player player) {
		return Aura.color(player);
	}
}
