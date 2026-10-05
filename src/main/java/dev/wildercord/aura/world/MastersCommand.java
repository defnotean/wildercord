package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

/** An explicit alternative to talking to a master; never enrolls another player on the caller's behalf. */
public final class MastersCommand {
	private MastersCommand() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
			Commands.literal("master")
				.then(Commands.literal("challenge")
					.then(Commands.literal("ember").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersRules.EMBER)))
					.then(Commands.literal("gale").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersRules.GALE)))
					.then(Commands.literal("stone").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersRules.STONE))))
				.then(Commands.literal("victories").executes(ctx -> MasterVictories.describe(ctx.getSource().getPlayerOrException())))
				.then(Commands.literal("ready").executes(ctx -> SwordMaster.ready(ctx.getSource().getPlayerOrException())))
				.then(Commands.literal("join").executes(ctx -> SwordMaster.join(ctx.getSource().getPlayerOrException())))));
	}
}
