package dev.wildercord.aura;

import dev.wildercord.config.Config;
import net.minecraft.server.level.ServerPlayer;

/**
 * Aura experience: earned by meaningful blows on real foes ({@link AuraCombat}), filling toward the next stage's threshold and
 * waiting there for a breakthrough ({@link AuraBreakthroughs}). Training dummies and the practice arena teach at half rate,
 * and only so much in all; creative players earn nothing, as with spell mastery.
 */
public final class AuraExperience {
	private AuraExperience() {}

	/**
	 * Adds {@code xp} (after the moment and repetition, before the server's rate). Returns what was really added.
	 *
	 * @param practice whether it was learned on a dummy or in the practice arena
	 */
	public static double earn(ServerPlayer player, double xp, boolean practice) {
		if (xp <= 0 || player.isCreative() || !Aura.enabled(player)) {
			return 0;
		}
		double rate = Config.get().aura().xpMultiplier();
		return rate <= 0 ? 0 : add(player, xp * rate, practice);
	}

	/** Adds experience straight away, at no rate (commands, the game tests and wave 2's teachers). */
	public static double grant(ServerPlayer player, double xp) {
		return add(player, xp, false);
	}

	private static double add(ServerPlayer player, double xp, boolean practice) {
		AuraAttachments.Data data = Aura.data(player);
		if (!data.learned() || xp <= 0) {
			return 0;
		}
		double practised = data.practice();
		double gained = xp;
		if (practice) {
			gained = AuraRules.practice(practised, xp);
			practised += gained;
		}
		int cap = AuraStages.cap(data.stage());
		boolean waiting = AuraRules.ready(data.xp(), cap);
		double next = AuraRules.fill(data.xp(), gained, cap);
		if (next == data.xp() && practised == data.practice()) {
			return 0;
		}
		Aura.set(player, data.withXp(next, practised));
		if (!waiting && AuraRules.ready(next, cap) && AuraStages.canBreakThrough(data.stage())) {
			AuraBreakthroughs.waiting(player);
		}
		return next - data.xp();
	}
}
