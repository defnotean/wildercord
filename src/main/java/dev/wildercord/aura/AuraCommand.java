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
			}))
			// A Way set outright (owing nothing), cleared ("none": as if never chosen), or the crossroads raised round the player.
			.then(Commands.literal("way")
				.then(Commands.literal("crossroads").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					if (!Crossroads.open(player, Crossroads.Reason.API)) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.way.no_crossroads"));
						return 0;
					}
					return report(ctx);
				}))
				.then(Commands.argument("way", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("none"),
						dev.wildercord.api.AuraApi.ways().stream().map(dev.wildercord.api.AuraApi.Way::id)), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String id = StringArgumentType.getString(ctx, "way");
						if (!id.equals("none") && dev.wildercord.api.AuraApi.way(id).isEmpty()) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.way.unknown", id));
							return 0;
						}
						Crossroads.close(player, false);
						Ways.set(player, id.equals("none") ? "" : id);
						return report(ctx);
					})))
			// Techniques of one's own: a part learned or forgotten ("all" for every part a scroll carries), a technique's experience set,
			// or everything cleared.
			.then(Commands.literal("technique")
				.then(Commands.literal("learn").then(Commands.argument("part", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
						TechniqueRules.allParts().stream()), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String part = StringArgumentType.getString(ctx, "part");
						if (part.equals("all")) {
							TechniqueRules.scrollParts().forEach(p -> Techniques.teach(player, p, "command"));
						} else if (TechniqueRules.family(part).isEmpty()) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.technique.unknown", part));
							return 0;
						} else {
							Techniques.teach(player, part, "command");
						}
						return techniques(ctx);
					})))
				.then(Commands.literal("forget").then(Commands.argument("part", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),
						TechniqueRules.allParts().stream()), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						String part = StringArgumentType.getString(ctx, "part");
						if (part.equals("all")) {
							TechniqueRules.allParts().forEach(p -> Techniques.forget(player, p));
						} else {
							Techniques.forget(player, part);
						}
						return techniques(ctx);
					})))
				.then(Commands.literal("xp").then(Commands.argument("slot", IntegerArgumentType.integer(1, TechniqueRules.MAX_SLOTS))
					.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						if (!Techniques.setXp(player, IntegerArgumentType.getInteger(ctx, "slot") - 1, DoubleArgumentType.getDouble(ctx, "amount"))) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.aura.technique.empty"));
							return 0;
						}
						return techniques(ctx);
					}))))
				.then(Commands.literal("clear").executes(ctx -> {
					Techniques.clear(ctx.getSource().getPlayerOrException());
					return techniques(ctx);
				})));
	}

	/** What a player's techniques stand at: the parts they may write with, then each slot's technique and its rank. */
	private static int techniques(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		java.util.List<Component> parts = new java.util.ArrayList<>();
		for (String part : Techniques.known(player)) {
			parts.add(Component.translatable(TechniqueRules.nameKey(part)));
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.technique.parts",
			net.minecraft.network.chat.ComponentUtils.formatList(parts, Component.literal(", "))), false);
		Techniques.Book book = Techniques.book(player);
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			Techniques.Written w = book.slot(i);
			if (w.empty()) {
				continue;
			}
			int slot = i + 1;
			Techniques.Honing h = book.honing(w.key());
			ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.technique.slot", slot, Component.literal(w.shownName()),
				Component.translatable(Techniques.rankKey(h.rank())), (int) h.xp()), false);
		}
		return 1;
	}

	private static int report(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AuraAttachments.Data data = Aura.data(player);
		Component method = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey())).orElse(Component.literal("-"));
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.state", method,
			Component.translatable("aura.wildercord.stage." + AuraStages.id(Aura.stage(player))), (int) data.xp(), (int) Aura.aura(player),
			Aura.capacity(player)), false);
		Ways.way(player).ifPresent(way -> ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.aura.way.state",
			Component.translatable(way.nameKey()).withColor(0xFF000000 | way.color())), false));
		return 1;
	}
}
