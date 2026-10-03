package dev.wildercord.aura;

import java.util.*;
import java.util.function.BiPredicate;

/** A short, paid meeting of blade and spell. No world references survive in the ledger. */
public final class ResonantRules {
	private ResonantRules() {}
	public static final int WINDOW = 24, REST = 80, LIMIT = 1024;
	public static final double COST = 4;
	public static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood");
	public record Hit(UUID player, String element, float taken, long tick, boolean blade) {
		public boolean valid() { return player != null && ELEMENTS.contains(element) && Float.isFinite(taken) && taken > 0; }
	}
	public record Pair(Hit spell, Hit blade) {}
	public static String sound(String element) { return ELEMENTS.contains(element) ? "aura_resonant_" + element : ""; }
	public static boolean within(long now, long then, int duration) { return now >= then && now - then >= 0 && now - then <= duration; }
	/** Extra damage uses the weaker actual hit, with a small independent ceiling. */
	public static float damage(float spell, float blade, boolean player) {
		if (!Float.isFinite(spell) || !Float.isFinite(blade)) return 0;
		return Math.max(0, Math.min(player ? .75F : 3F, Math.min(spell, blade) * .25F));
	}
	public static String name(String spell, String blade) {
		return switch (spell) {
			case "fire" -> blade.equals("wind") ? "bellows_cut" : "ember_shear";
			case "frost" -> blade.equals("fire") ? "thermal_cleave" : "rime_splinter";
			case "storm" -> blade.equals("earth") ? "grounding_stroke" : "forked_edge";
			case "wind" -> blade.equals("time") ? "borrowed_step" : "crosswind";
			case "earth" -> blade.equals("frost") ? "gravel_rime" : "faultline";
			case "life" -> blade.equals("blood") ? "grafted_edge" : "greenwake";
			case "void" -> blade.equals("arcane") ? "veiled_prism" : "hollow_seam";
			case "arcane" -> blade.equals("time") ? "second_hand" : "prism_cut";
			case "time" -> blade.equals("wind") ? "slipstream_echo" : "echo_beat";
			case "blood" -> blade.equals("void") ? "hollow_pulse" : "red_thread";
			default -> "";
		};
	}
	public static final class Ledger {
		private final LinkedHashMap<UUID, Hit> pending = new LinkedHashMap<>();
		private final LinkedHashMap<UUID, Long> targets = new LinkedHashMap<>(), blades = new LinkedHashMap<>();
		private long swept = Long.MIN_VALUE;
		/** Invalid/hostile pairings cannot consume a friendly opening or refresh another player's cooldown. */
		public Pair offer(UUID target, Hit hit, BiPredicate<Hit, Hit> allowed) {
			if (target == null || hit == null || !hit.valid()) return null;
			long now = hit.tick();
			if (swept == Long.MIN_VALUE || now < swept || now - swept >= 20) {
				pending.values().removeIf(h -> !within(now, h.tick(), WINDOW));
				targets.values().removeIf(t -> !within(now, t, REST - 1));
				blades.values().removeIf(t -> !within(now, t, REST - 1));
				swept = now;
			}
			if (targets.containsKey(target) && within(now, targets.get(target), REST - 1)) return null;
			Hit previous = pending.get(target);
			if (previous != null && !within(now, previous.tick(), WINDOW)) previous = null;
			if (previous != null && previous.blade() != hit.blade()) {
				Hit spell = hit.blade() ? previous : hit, blade = hit.blade() ? hit : previous;
				if (blades.containsKey(blade.player()) && within(now, blades.get(blade.player()), REST - 1) || !allowed.test(spell, blade)) return null;
				pending.remove(target);
				targets.put(target, now); blades.put(blade.player(), now);
				bound(targets); bound(blades);
				return new Pair(spell, blade);
			}
			pending.put(target, hit); bound(pending);
			return null;
		}
		private static <T> void bound(LinkedHashMap<UUID,T> map) { while (map.size() > LIMIT) map.pollFirstEntry(); }
		public void forget(UUID player) { pending.values().removeIf(h -> h.player().equals(player)); }
		public int size() { return pending.size() + targets.size() + blades.size(); }
	}
}
