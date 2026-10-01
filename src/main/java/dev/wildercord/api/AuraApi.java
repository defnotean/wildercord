package dev.wildercord.api;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.MethodSources;
import net.minecraft.server.level.ServerPlayer;
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
		/** Two taps within a little under half a second (the second one only; the first still went off as a tap). */
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
