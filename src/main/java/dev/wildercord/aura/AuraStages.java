package dev.wildercord.aura;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The stage registry: which stages of aura are open, and what each holds and needs. Glow, Flow and Edge are registered
 * here; Form and Sovereign keep their numbers in {@link AuraRules} (and their default capacity and threshold) until a later
 * wave registers them with their trials and techniques. A stage that isn't registered can't be broken through to, but the
 * experience toward it still fills (and waits at its threshold), so nothing earned is lost when it opens.
 *
 * <p>Pure, so the HUD, the Aura page, the server and the tests agree. Registration happens at start-up, on the main
 * thread (see {@code api.AuraApi.registerStage}).</p>
 */
public final class AuraStages {
	private AuraStages() {}

	/**
	 * A stage of aura.
	 *
	 * @param number    1 (Glow) to {@link AuraRules#MAX_STAGE}
	 * @param id        its id, for language keys ({@code aura.wildercord.stage.<id>}) and the Grimoire ({@code aura:<id>})
	 * @param capacity  how much aura it holds
	 * @param threshold the experience its breakthrough needs (a running total)
	 */
	public record Stage(int number, String id, int capacity, int threshold) {
		public Stage {
			if (number < AuraRules.GLOW || number > AuraRules.MAX_STAGE) {
				throw new IllegalArgumentException("aura stages run from 1 to " + AuraRules.MAX_STAGE + " (got " + number + ")");
			}
			capacity = Math.max(1, capacity);
			threshold = Math.max(0, threshold);
		}
	}

	private static final Stage[] STAGES = new Stage[AuraRules.MAX_STAGE + 1];

	static {
		for (int s = AuraRules.GLOW; s <= AuraRules.BUILT_IN_STAGES; s++) {
			register(new Stage(s, AuraRules.id(s), AuraRules.capacity(s), AuraRules.threshold(s)));
		}
	}

	/** Opens a stage (or replaces it). */
	public static synchronized Stage register(Stage stage) {
		STAGES[stage.number()] = stage;
		return stage;
	}

	public static synchronized Optional<Stage> get(int number) {
		return number < AuraRules.GLOW || number > AuraRules.MAX_STAGE ? Optional.empty() : Optional.ofNullable(STAGES[number]);
	}

	/** The highest stage that can be reached: the last of an unbroken run from Glow. */
	public static synchronized int highest() {
		int s = AuraRules.NONE;
		while (s < AuraRules.MAX_STAGE && STAGES[s + 1] != null) {
			s++;
		}
		return s;
	}

	/** Every open stage, from Glow up. */
	public static synchronized List<Stage> open() {
		List<Stage> out = new ArrayList<>();
		for (int s = AuraRules.GLOW; s <= highest(); s++) {
			out.add(STAGES[s]);
		}
		return out;
	}

	/** How much aura {@code stage} holds (its default if it isn't open). */
	public static int capacity(int stage) {
		return get(stage).map(Stage::capacity).orElse(AuraRules.capacity(stage));
	}

	/** The experience the breakthrough into {@code stage} needs (its default if it isn't open). */
	public static int threshold(int stage) {
		return get(stage).map(Stage::threshold).orElse(AuraRules.threshold(stage));
	}

	/** The id of {@code stage} (glow, flow, edge...). */
	public static String id(int stage) {
		return get(stage).map(Stage::id).orElse(AuraRules.id(stage));
	}

	/**
	 * Where experience stops filling at {@code stage}: the next stage's threshold (registered or not, so the road is ready
	 * when it opens), or -1 past the last stage of all.
	 */
	public static int cap(int stage) {
		return stage >= AuraRules.MAX_STAGE ? -1 : threshold(stage + 1);
	}

	/** Whether a breakthrough out of {@code stage} can be made at all: the next stage is open. */
	public static boolean canBreakThrough(int stage) {
		return stage >= AuraRules.GLOW && stage < AuraRules.MAX_STAGE && get(stage + 1).isPresent();
	}
}
