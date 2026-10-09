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
					.then(Commands.literal("stone").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersRules.STONE)))
					// ---- masters-b pack
					.then(Commands.literal("starlit").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersPackB.STARLIT)))
					.then(Commands.literal("hourglass").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersPackB.HOURGLASS)))
					.then(Commands.literal("crimson").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MastersPackB.CRIMSON)))
					// ---- methods-a pack
					.then(Commands.literal("tide").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MethodsAMasters.TIDE)))
					.then(Commands.literal("iron").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MethodsAMasters.IRON)))
					.then(Commands.literal("dune").executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), MethodsAMasters.DUNE))))
				.then(Commands.literal("victories").executes(ctx -> MasterVictories.describe(ctx.getSource().getPlayerOrException())))
				.then(Commands.literal("ready").executes(ctx -> SwordMaster.ready(ctx.getSource().getPlayerOrException())))
				.then(Commands.literal("join").executes(ctx -> SwordMaster.join(ctx.getSource().getPlayerOrException())))));
		// ---- masters-a pack: /master challenge rime|thunder|verdant|hollow (Brigadier merges the shared literal)
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
			var challenge = Commands.literal("challenge");
			for (int school : ElementalMasters.SCHOOL_IDS) challenge.then(Commands.literal(ElementalMasters.name(school))
				.executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), school)));
			dispatcher.register(Commands.literal("master").then(challenge));
		});
		// ---- methods-b pack: /master challenge echo|dawn|venom (Brigadier merges this into the tree above).
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
			var challenge = Commands.literal("challenge");
			for (int school : MethodsBMasters.SCHOOLS) challenge.then(Commands.literal(MethodsBMasters.id(school))
				.executes(ctx -> SwordMaster.challenge(ctx.getSource().getPlayerOrException(), school)));
			dispatcher.register(Commands.literal("master").then(challenge));
		});
	}
}
