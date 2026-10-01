package dev.wildercord.aura;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /wildercord aura ...}, for operators testing the path: teach a method outright, set the stage, add experience, fill
 * the aura, or make the waiting breakthrough. Each says where the player's aura stands afterwards.
 */
public final class AuraCommand {
	private AuraCommand() {}

	public static LiteralArgumentBuilder<CommandSourceStack> node() {
		return Commands.literal("aura")
			.then(Commands.literal("method").then(Commands.argument("method", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(BreathingMethods.all().stream().map(BreathingMethod::id), builder))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					BreathingMethod method = BreathingMethods.byId(StringArgumentType.getString(ctx, "method")).orElse(null);
					if (method == null) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.no_method"));
						return 0;
					}
					AuraMethods.learn(player, method, "command", true);
					return report(ctx);
				})))
			.then(Commands.literal("stage").then(Commands.argument("stage", IntegerArgumentType.integer(AuraRules.GLOW, AuraRules.MAX_STAGE))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					AuraAttachments.Data data = Aura.data(player);
					if (!data.learned()) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.no_method"));
						return 0;
					}
					int stage = Math.min(AuraStages.highest(), IntegerArgumentType.getInteger(ctx, "stage"));
					Aura.set(player, data.withStage(stage).withXp(Math.max(data.xp(), AuraStages.threshold(stage)), data.practice()));
					return report(ctx);
				})))
			.then(Commands.literal("xp").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
				.executes(ctx -> {
					AuraExperience.grant(ctx.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(ctx, "amount"));
					return report(ctx);
				})))
			.then(Commands.literal("fill").executes(ctx -> {
				ServerPlayer player = ctx.getSource().getPlayerOrException();
				Aura.set(player, Aura.data(player).withAura(Aura.capacity(player)));
				return report(ctx);
			}))
			.then(Commands.literal("breakthrough").executes(ctx -> {
				AuraBreakthroughs.breakThrough(ctx.getSource().getPlayerOrException());
				return report(ctx);
			}));
	}

	private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuraAttachments.Data data = Aura.data(player);
		Component method = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey())).orElse(Component.literal("-"));
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.state", method,
			Component.translatable("aura.wildercord.stage." + AuraStages.id(Aura.stage(player))), (int) data.xp(), (int) Aura.aura(player),
			Aura.capacity(player)), false);
		return 1;
	}
}
