package dev.wildercord.player;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.spell.CodexRules;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * The stats page (0.13; what it shows is {@link StatsRules}): {@code /stats} sums up how far a player has come, and
 * {@code /stats <player>} someone else, as {@code /duel stats} already does for duels.
 */
public final class Stats {
	private Stats() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(Commands.literal("stats")
			.executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
			.then(Commands.argument("player", EntityArgument.player()).executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
	}

	public static StatsRules.Snapshot snapshot(ServerPlayer player) {
		var stats = player.getStats();
		List<String> grimoire = Heart.grimoire(player);
		LoreJournalDataView journal = new LoreJournalDataView(LoreJournal.data(player).entries());
		var tally = Codex.tally(player);
		int slain = tally.values().stream().mapToInt(Integer::intValue).sum();
		int mastered = (int) tally.values().stream().filter(k -> CodexRules.rank(k) == CodexRules.Rank.MASTERED).count();
		return new StatsRules.Snapshot(
			stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME)),
			Heart.circles(player), Heart.ascension(player),
			Aura.method(player).isPresent() ? Aura.stage(player) : -1, Aura.method(player).map(BreathingMethod::id).orElse(""),
			journal.count("won:"), journal.count("victory:"),
			dev.wildercord.spell.Feats.count(grimoire, "feat:"),
			FieldGuide.met(grimoire).size(), FieldGuide.all().size(), slain, mastered,
			stats.getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DEATHS)));
	}

	private record LoreJournalDataView(List<String> entries) {
		int count(String prefix) {
			return (int) entries.stream().filter(e -> e.startsWith(prefix)).count();
		}
	}

	private static int show(CommandSourceStack source, ServerPlayer player) {
		source.sendSystemMessage(Component.translatable("stats.wildercord.title", player.getDisplayName()).withStyle(ChatFormatting.GOLD));
		for (StatsRules.Row row : StatsRules.rows(snapshot(player))) {
			Object[] args = row.args().toArray();
			if (row.key().equals("aura")) {
				args = new Object[] {LoreJournal.methodName((String) args[0]),
					Component.translatable("aura.wildercord.stage." + AuraStages.id((Integer) args[1]))};
			}
			source.sendSystemMessage(Component.translatable("stats.wildercord." + row.key(), args).withStyle(ChatFormatting.GRAY));
		}
		return 1;
	}
}
