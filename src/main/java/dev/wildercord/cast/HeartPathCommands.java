package dev.wildercord.cast;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.HeartPaths;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Player commands for Heart Paths (0.13). The Tenth Circle offers its three paths as clickable choices when it forms; choosing is
 * free, leaving a path costs {@link HeartPaths#LEAVE_LEVELS} experience levels.
 */
public final class HeartPathCommands {
	private HeartPathCommands() {}

	private static final int GOLD = 0xF5C46A, MUTED = 0xB8A8FF;

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("path")
			.executes(ctx -> list(ctx.getSource().getPlayerOrException()))
			.then(literal("walk").then(argument("path", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(HeartPaths.Path.values()).map(path -> path.id), builder))
				.executes(ctx -> walk(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "path")))))
			.then(literal("leave").executes(ctx -> leave(ctx.getSource().getPlayerOrException())))));
	}

	/** Sent when the Tenth Circle forms (and as a reminder while meditating): the three paths, each a click away. */
	public static void offer(ServerPlayer player) {
		if (HeartPaths.of(Heart.path(player)) != null || HeartPaths.take(0, Heart.active(player)) != null) return;
		player.sendSystemMessage(text("offer", "The Tenth Circle asks which way your heart will grow. Walk one path (/path to review): ").withColor(GOLD)
			.append(buttons()));
	}

	private static MutableComponent buttons() {
		MutableComponent out = Component.empty();
		for (HeartPaths.Path path : HeartPaths.Path.values()) {
			if (path.ordinal() > 0) out.append(Component.literal(" "));
			out.append(Component.literal("[").append(name(path)).append("]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
				.withClickEvent(new ClickEvent.RunCommand("/path walk " + path.id))
				.withHoverEvent(new HoverEvent.ShowText(about(path)))));
		}
		return out;
	}

	static int list(ServerPlayer player) {
		HeartPaths.Path walking = HeartPaths.of(Heart.path(player));
		player.sendSystemMessage(text("header", "Heart Paths: one choice at the Tenth Circle. Leaving a path costs %s levels.", HeartPaths.LEAVE_LEVELS).withColor(GOLD));
		for (HeartPaths.Path path : HeartPaths.Path.values()) {
			MutableComponent line = name(path).withColor(path == walking ? GOLD : MUTED).append(Component.literal(" - "))
				.append(about(path).withStyle(path == walking ? ChatFormatting.WHITE : ChatFormatting.GRAY));
			if (path == walking && Heart.activePath(player) == null)
				line.append(text("silent", " (silent while the Tenth Circle is unformed or cracked)").withStyle(ChatFormatting.GRAY));
			player.sendSystemMessage(line);
		}
		if (walking != null) {
			player.sendSystemMessage(Component.literal("[").append(text("leave_button", "Leave your path")).append("]").withStyle(style -> style.withColor(ChatFormatting.RED)
				.withClickEvent(new ClickEvent.SuggestCommand("/path leave"))
				.withHoverEvent(new HoverEvent.ShowText(text("leave_hover", "Costs %s levels; choose again afterwards for free", HeartPaths.LEAVE_LEVELS)))));
		} else if (Heart.active(player) >= HeartPaths.CIRCLE) {
			player.sendSystemMessage(buttons());
		}
		return 1;
	}

	static int walk(ServerPlayer player, String id) {
		HeartPaths.Path path = HeartPaths.byId(id);
		if (path == null) return refuse(player, text("unknown", "No path is called that. Use /path to see them."));
		int saved = Heart.path(player);
		HeartPaths.Refusal refusal = HeartPaths.take(saved, Heart.active(player));
		if (refusal == HeartPaths.Refusal.NOT_REACHED)
			return refuse(player, text("not_reached", "Form the Tenth Circle first (a cracked circle cannot choose a path)."));
		if (refusal == HeartPaths.Refusal.ALREADY_WALKING)
			return refuse(player, text("walking", "You already walk %s. Leave it first: /path leave", name(HeartPaths.of(saved))));
		player.setAttached(WildercordAttachments.HEART_PATH, path.saved());
		clampMana(player);
		player.sendSystemMessage(text("walked", "You walk %s: %s", name(path), about(path)).withColor(GOLD));
		return 1;
	}

	static int leave(ServerPlayer player) {
		HeartPaths.Refusal refusal = HeartPaths.leave(Heart.path(player), player.experienceLevel, player.isCreative());
		if (refusal == HeartPaths.Refusal.NOT_WALKING) return refuse(player, text("not_walking", "You walk no path."));
		if (refusal == HeartPaths.Refusal.LEVELS) return refuse(player, text("levels", "Leaving a path costs %s experience levels.", HeartPaths.LEAVE_LEVELS));
		if (!player.isCreative()) player.giveExperienceLevels(-HeartPaths.LEAVE_LEVELS);
		player.setAttached(WildercordAttachments.HEART_PATH, 0);
		clampMana(player);
		player.sendSystemMessage(text("left", "You step off your path. Choose again: ").withColor(GOLD).append(buttons()));
		return 1;
	}

	static MutableComponent name(HeartPaths.Path path) {
		return Component.translatableWithFallback("path.wildercord." + path.id, path.name);
	}

	static MutableComponent about(HeartPaths.Path path) {
		return Component.translatableWithFallback("path.wildercord." + path.id + ".text", path.text);
	}

	private static void clampMana(ServerPlayer player) {
		if (Spellbooks.tier(player) != null) Spellbooks.setMana(player, Math.min(Spellbooks.mana(player), Mana.max(player)));
	}

	private static int refuse(ServerPlayer player, MutableComponent message) {
		player.sendSystemMessage(message.withStyle(ChatFormatting.RED));
		return 0;
	}

	private static MutableComponent text(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("message.wildercord.path." + key, fallback, args);
	}
}
