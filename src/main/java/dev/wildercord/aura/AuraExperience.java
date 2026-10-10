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
			// Fellow guild members training close by.
			xp *= dev.wildercord.guild.Guilds.bonus(player, dev.wildercord.guild.GuildRules.Kind.GUILD);
			// A Way chosen after a change settles by what's earned walking it (before the stage's cap: one waiting at a threshold still does).
			Ways.earned(player, xp * rate);
		}
		return add(player, xp * rate, practice);
	}

	/**
	 * 0.12's re-tempering: a stage this save's experience no longer reaches under today's thresholds is given back (the
	 * experience stays). Returns the stage before and after.
	 */
	public static int[] retemper(ServerPlayer player) {
		AuraAttachments.Data data = Aura.data(player);
		if (!data.learned()) {
			return new int[] {0, 0};
		}
		int keeps = AuraRules.retemper(data.stage(), data.xp(), AuraStages::threshold);
		if (keeps < data.stage()) {
			AuraAttachments.Data lower = data.withStage(keeps);
			Aura.set(player, new AuraAttachments.Data(lower.method(), keeps, lower.xp(), Math.min(lower.aura(), AuraStages.capacity(keeps)), lower.practice()));
		}
		return new int[] {data.stage(), keeps};
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
			if (data.practice() < AuraRules.PRACTICE_CAP && practised >= AuraRules.PRACTICE_CAP) {
				player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.aura.practice_complete")
					.withColor(0xE8D8B0));
			}
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
