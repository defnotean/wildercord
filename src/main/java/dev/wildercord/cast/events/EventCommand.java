package dev.wildercord.cast.events;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;

/**
 * {@code /wildercord event <mana_storm|starfall|rift> [here]}: starts a world event for testing.
 * Without {@code here} it's placed as the server would place it (a storm over the nearest ley line,
 * a star 60 to 150 blocks off, a rift a little way away); with it, right by you. Joins the
 * {@code /wildercord} command (Brigadier merges the two), operators only. Refused while the server's
 * config switches world events off.
 */
public final class EventCommand {
	private EventCommand() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("wildercord")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("event")
					.then(kind("mana_storm"))
					.then(kind("starfall"))
					.then(kind("rift")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> kind(String kind) {
		return Commands.literal(kind)
			.executes(ctx -> start(ctx, kind, false))
			.then(Commands.literal("here").executes(ctx -> start(ctx, kind, true)));
	}

	private static int start(CommandContext<CommandSourceStack> ctx, String kind, boolean here) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = player.level();
		if (!WorldEvents.enabled()) {
			ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.off"));
			return 0;
		}
		switch (kind) {
			case "mana_storm" -> {
				ManaStorm storm = WorldEvents.startStorm(level, player, here);
				if (storm == null) {
					ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.no_ley"));
					return 0;
				}
				ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.event.storm", storm.ticksLeft(level.getGameTime()) / 20), true);
				return 1;
			}
			case "starfall" -> {
				if (level.getDifficulty() == Difficulty.PEACEFUL) {
					// Its guards couldn't rise, and it wouldn't open: nothing to test.
					ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.peaceful"));
					return 0;
				}
				BlockPos land = WorldEvents.startStar(level, player, here);
				if (land == null) {
					ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.no_room"));
					return 0;
				}
				ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.event.star", land.getX(), land.getY(), land.getZ()), true);
				return 1;
			}
			default -> {
				if (level.getDifficulty() == Difficulty.PEACEFUL) {
					ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.peaceful"));
					return 0;
				}
				if (WorldEvents.riftIn(level) != null) {
					ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.rift_open"));
					return 0;
				}
				RiftSiege rift = WorldEvents.startRift(level, player, here);
				if (rift == null) {
					ctx.getSource().sendFailure(Component.translatable("command.wildercord.event.no_room"));
					return 0;
				}
				ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.event.rift"), true);
				return 1;
			}
		}
	}
}
