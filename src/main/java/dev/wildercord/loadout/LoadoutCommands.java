package dev.wildercord.loadout;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Stream;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /loadout}, for every player: {@code save <name>}, {@code load <name>}, {@code delete <name>}
 * and {@code list} (or {@code /loadout} alone). Names may have spaces, and the player's own
 * loadouts are suggested as they type. Saving under a name already used saves over that loadout.
 */
final class LoadoutCommands {
	private LoadoutCommands() {}

	private static final String NAME = "name";

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("loadout")
			.executes(ctx -> list(player(ctx)))
			.then(literal("list").executes(ctx -> list(player(ctx))))
			.then(literal("save").then(argument(NAME, StringArgumentType.greedyString()).suggests(LOADOUTS)
				.executes(ctx -> answer(ctx, Loadouts.save(player(ctx), name(ctx), true)))))
			.then(literal("load").then(argument(NAME, StringArgumentType.greedyString()).suggests(LOADOUTS)
				.executes(ctx -> byName(ctx, true))))
			.then(literal("delete").then(argument(NAME, StringArgumentType.greedyString()).suggests(LOADOUTS)
				.executes(ctx -> byName(ctx, false)))));
	}

	/** {@code load} or {@code delete}: the loadout named, or why there's none. */
	private static int byName(CommandContext<CommandSourceStack> ctx, boolean load) throws CommandSyntaxException {
		ServerPlayer player = player(ctx);
		int index = Loadouts.find(player, name(ctx));
		if (index < 0) {
			return answer(ctx, Loadouts.Result.refused("missing", Component.literal(name(ctx).trim())));
		}
		return answer(ctx, load ? Loadouts.load(player, index) : Loadouts.delete(player, index));
	}

	/** Every loadout, each a button that loads it. */
	private static int list(ServerPlayer player) {
		LoadoutData data = Loadouts.data(player);
		if (data.size() == 0) {
			player.sendSystemMessage(Loadouts.text("list_none").withStyle(ChatFormatting.GRAY));
			return 0;
		}
		MutableComponent line = Loadouts.text("list", data.size(), LoadoutRules.MAX).withStyle(ChatFormatting.GRAY);
		for (int i = 0; i < data.size(); i++) {
			String name = data.get(i).name();
			if (i > 0) {
				line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
			}
			MutableComponent button = Loadouts.name(name).withStyle(style -> style.withUnderlined(true)
				.withClickEvent(new ClickEvent.RunCommand("/loadout load " + name))
				.withHoverEvent(new HoverEvent.ShowText(Loadouts.text("list_hover", name))));
			if (i == data.current()) {
				button.append(Component.literal(" \u2022").withColor(0x7FE0F0));
			}
			line.append(button);
		}
		player.sendSystemMessage(line);
		return data.size();
	}

	/** Tells the player how it went, in chat: red if refused. */
	private static int answer(CommandContext<CommandSourceStack> ctx, Loadouts.Result result) throws CommandSyntaxException {
		ServerPlayer player = player(ctx);
		player.sendSystemMessage(result.ok() ? result.message().copy().withColor(0x7FE0F0) : result.message().copy().withStyle(ChatFormatting.RED));
		return result.ok() ? 1 : 0;
	}

	private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return ctx.getSource().getPlayerOrException();
	}

	private static String name(CommandContext<CommandSourceStack> ctx) {
		return StringArgumentType.getString(ctx, NAME);
	}

	private static final SuggestionProvider<CommandSourceStack> LOADOUTS = (ctx, builder) -> {
		ServerPlayer player = ctx.getSource().getPlayer();
		return SharedSuggestionProvider.suggest(player == null ? Stream.empty() : Loadouts.data(player).names().stream(), builder);
	};
}
