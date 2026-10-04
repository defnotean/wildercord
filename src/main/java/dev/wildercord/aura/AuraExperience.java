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
		if (rate <= 0) {
			return 0;
		}
		if (!practice) {
			// A disciple near their master learns faster.
			xp = Lineage.near(player, xp);
			// A Way chosen after a change settles by what's earned walking it (before the stage's cap: one waiting at a threshold still does).
			Ways.earned(player, xp * rate);
		}
		return add(player, xp * rate, practice);
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
		}
		int cap = AuraStages.cap(data.stage());
		boolean waiting = AuraRules.ready(data.xp(), cap);
		double next = AuraRules.fill(data.xp(), gained, cap);
		if (practice) {
			// Only what was really learned uses the allowance: practice while experience waits at a threshold spends none of it.
			practised += Math.max(0, next - data.xp());
			if (data.practice() < AuraRules.PRACTICE_CAP && practised >= AuraRules.PRACTICE_CAP) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.aura.practice_complete")
					.withColor(0xE8D8B0));
			}
		}
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
