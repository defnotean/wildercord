package dev.wildercord.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /wildercord} test and admin tools:
 * <ul>
 *   <li>{@code learnall}: learn every rune</li>
 *   <li>{@code learn <rune>}: learn one rune</li>
 *   <li>{@code spell <1-5> <runes...>}: thread a spell, e.g. {@code spell 1 bolt fire split} (5 is the tome's)</li>
 *   <li>{@code mana}: refill mana</li>
 *   <li>{@code circles <0-8>}, {@code condense <mana>}: set Heart Circles, add condensed mana</li>
 *   <li>{@code innate <rune>}: choose your innate rune</li>
 *   <li>{@code runebound}: bind the nearest monster to a Cord</li>
 *   <li>{@code reset}: forget everything</li>
 *   <li>{@code reload}: read {@code config/wildercord.json} again</li>
 * </ul>
 */
public final class WildercordCommand {
	private WildercordCommand() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("wildercord")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("practice")
					.then(Commands.literal("enter").executes(ctx -> dev.wildercord.cast.PracticeRoom.enter(ctx.getSource().getPlayerOrException())))
					.then(Commands.literal("leave").executes(ctx -> dev.wildercord.cast.PracticeRoom.leave(ctx.getSource().getPlayerOrException())))
					.then(Commands.literal("moving").executes(ctx -> dev.wildercord.cast.PracticeRoom.targets(ctx.getSource().getLevel(),3,true)))
					.then(Commands.literal("reset").executes(ctx -> dev.wildercord.cast.PracticeRoom.targets(ctx.getSource().getLevel(),3,false)))
					.then(Commands.literal("stress").then(Commands.argument("targets",IntegerArgumentType.integer(1,24))
						.executes(ctx -> dev.wildercord.cast.PracticeRoom.targets(ctx.getSource().getLevel(),IntegerArgumentType.getInteger(ctx,"targets"),true))))
					.then(Commands.literal("benchmark").executes(ctx -> dev.wildercord.cast.PracticeRoom.benchmark(ctx.getSource().getPlayerOrException()))))
				.then(Commands.literal("visualstats").executes(ctx -> {
					ctx.getSource().sendSuccess(() -> Component.literal(dev.wildercord.cast.VisualMetrics.report()), false);
					return 1;
				}).then(Commands.literal("reset").executes(ctx -> { dev.wildercord.cast.VisualMetrics.reset(); return 1; })))
				.then(Commands.literal("learnall").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					Spellbook book = Spellbooks.get(player);
					for (RuneDef rune : Runes.all()) {
						book = book.learn(rune.id());
					}
					Spellbooks.set(player, book);
					// Learning the result alone leaves its recipe hidden in the Grimoire. Admin discovery
					// reveals every named recipe in one sync, without minting exploration rewards/toasts.
					var found = new java.util.LinkedHashSet<>(dev.wildercord.player.Heart.grimoire(player));
					dev.wildercord.spell.Fusions.RECIPES.forEach(recipe -> found.add(recipe.key()));
					dev.wildercord.spell.Fusions.SIGNATURES.forEach(recipe -> found.add(recipe.key()));
					player.setAttached(dev.wildercord.player.WildercordAttachments.GRIMOIRE, List.copyOf(found));
					int fusions = dev.wildercord.spell.Fusions.RECIPES.size() + dev.wildercord.spell.Fusions.SIGNATURES.size();
					ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.learnall", Runes.all().size(), fusions), false);
					return Runes.all().size();
				}))
				.then(Commands.literal("learn").then(Commands.argument("rune", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
						Runes.all().stream().map(WildercordCommand::shortId), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						Optional<RuneDef> rune = find(StringArgumentType.getString(ctx, "rune"));
						if (rune.isEmpty()) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.unknown", StringArgumentType.getString(ctx, "rune")));
							return 0;
						}
						Spellbooks.learn(player, rune.get().id());
						ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.learned", rune.get().name()), false);
						return 1;
					})))
				.then(Commands.literal("spell").then(Commands.argument("index", IntegerArgumentType.integer(1, dev.wildercord.gear.SpellSlots.ALL))
					.then(Commands.argument("runes", StringArgumentType.greedyString()).executes(WildercordCommand::setSpell))))
				.then(Commands.literal("mana").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					Spellbooks.setMana(player, dev.wildercord.player.Mana.max(player));
					return 1;
				}))
				.then(Commands.literal("circles").then(Commands.argument("count", IntegerArgumentType.integer(0, dev.wildercord.spell.Circles.MAX))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						int count = IntegerArgumentType.getInteger(ctx, "count");
						player.setAttached(dev.wildercord.player.WildercordAttachments.CIRCLES, count);
						dev.wildercord.advancement.Advancements.circles(player);
						ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.circles", count), false);
						return 1;
					})))
				// Aura, the swordsman's path: a method, a stage, experience, a full pool, a breakthrough.
				.then(dev.wildercord.aura.AuraCommand.node())
				.then(Commands.literal("condense").then(Commands.argument("mana", IntegerArgumentType.integer(0))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						int mana = IntegerArgumentType.getInteger(ctx, "mana");
						player.setAttached(dev.wildercord.player.WildercordAttachments.CONDENSED, dev.wildercord.player.Heart.condensed(player) + mana);
						ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.condensed", mana), false);
						return 1;
					})))
				.then(Commands.literal("innate").then(Commands.argument("rune", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
						Runes.INNATE.stream().map(WildercordCommand::shortId), builder))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						Optional<RuneDef> rune = find(StringArgumentType.getString(ctx, "rune"));
						if (rune.isEmpty() || !Runes.innate(rune.get())) {
							ctx.getSource().sendFailure(Component.translatable("command.wildercord.unknown", StringArgumentType.getString(ctx, "rune")));
							return 0;
						}
						player.setAttached(dev.wildercord.player.WildercordAttachments.INNATE, rune.get().id());
						Spellbooks.learn(player, rune.get().id());
						ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.innate", rune.get().name()), false);
						return 1;
					})))
				.then(Commands.literal("runebound").executes(ctx -> {
					// Binds the monster you're looking at (or the nearest one) to a Cord.
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					net.minecraft.world.entity.Mob best = null;
					for (net.minecraft.world.entity.Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(16),
							e -> e instanceof net.minecraft.world.entity.Mob && e instanceof net.minecraft.world.entity.monster.Enemy)) {
						if (best == null || e.distanceToSqr(player) < best.distanceToSqr(player)) {
							best = (net.minecraft.world.entity.Mob) e;
						}
					}
					if (best == null) {
						ctx.getSource().sendFailure(Component.translatable("command.wildercord.no_monster"));
						return 0;
					}
					dev.wildercord.cast.Runebound.bind(best, false);
					ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.runebound"), false);
					return 1;
				}))
				.then(Commands.literal("reload").executes(WildercordCommand::reload))
				.then(Commands.literal("reset").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					Spellbooks.set(player, Spellbook.EMPTY);
					ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.reset"), false);
					return 1;
				}))
		));
	}

	private static int setSpell(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
		List<String> ids = new ArrayList<>();
		Spellbook book = Spellbooks.get(player);
		for (String word : StringArgumentType.getString(ctx, "runes").trim().split("\\s+")) {
			Optional<RuneDef> rune = find(word);
			if (rune.isEmpty()) {
				ctx.getSource().sendFailure(Component.translatable("command.wildercord.unknown", word));
				return 0;
			}
			// Testing shortcut: threading a rune by command also teaches it.
			book = book.learn(rune.get().id());
			ids.add(rune.get().id());
		}
		Spellbooks.set(player, book);
		// Clear first so the command replaces the spell under the same Cord rules as the editor.
		SpellCaster.edit(player, index, List.of());
		Component problem = SpellCaster.edit(player, index, ids);
		if (problem != null) {
			ctx.getSource().sendFailure(problem);
		}
		int threaded = Spellbooks.get(player).spells().get(index).size();
		if (threaded > 0) {
			ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.spell_set", index + 1, threaded), false);
		}
		return threaded;
	}

	/** Reads the config file again, and says what was wrong with it (each already fixed). */
	private static int reload(CommandContext<CommandSourceStack> ctx) {
		dev.wildercord.spell.CompiledSpellCache.clear();
		List<String> warnings = dev.wildercord.config.Config.reload(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.translatable("command.wildercord.reloaded", dev.wildercord.config.Config.FILE), true);
		for (String warning : warnings) {
			ctx.getSource().sendFailure(Component.translatable("command.wildercord.config_warning", warning));
		}
		return warnings.isEmpty() ? 1 : 0;
	}

	/** Accepts {@code fire}, {@code wildercord:fire} or {@code on_hit}, and an add-on's {@code other:rune}. */
	private static Optional<RuneDef> find(String word) {
		String trimmed = word.trim();
		String id = trimmed.contains(":") ? trimmed : "wildercord:" + trimmed;
		return Runes.get(id);
	}

	/** How a rune is suggested: Wildercord's own by name alone, an add-on's with its namespace. */
	private static String shortId(RuneDef rune) {
		return rune.id().startsWith("wildercord:") ? rune.path() : rune.id();
	}
}
