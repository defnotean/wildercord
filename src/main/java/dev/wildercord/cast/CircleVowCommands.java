package dev.wildercord.cast;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.CircleVows;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Player commands for Circle Vows. Every change takes its actor from the command source; the offer after a
 * vow circle forms is the same two clickable choices. Taking is free, releasing costs
 * {@link CircleVows#RELEASE_LEVELS} experience levels.
 */
public final class CircleVowCommands {
	private CircleVowCommands() {}

	private static final int GOLD = 0xF5C46A, MUTED = 0xB8A8FF;

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(literal("vow")
			.executes(ctx -> list(ctx.getSource().getPlayerOrException()))
			.then(literal("take").then(argument("vow", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CircleVows.ALL.stream()
					.flatMap(vow -> java.util.stream.Stream.of(vow.first().id(), vow.second().id())), builder))
				.executes(ctx -> take(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "vow")))))
			.then(literal("release").then(argument("circle", IntegerArgumentType.integer(1, dev.wildercord.spell.Circles.MAX))
				.executes(ctx -> release(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "circle")))))));
	}

	/** Sent when a vow circle forms: its two sides, each a click away. */
	public static void offer(ServerPlayer player, int circle) {
		CircleVows.Vow vow = CircleVows.at(circle);
		if (vow == null || CircleVows.choice(Heart.vows(player), vow) != CircleVows.NONE) return;
		player.sendSystemMessage(text("offer", "The %s Circle asks for a vow. Choose one (/vow to review): ", vow.numeral()).withColor(GOLD)
			.append(button(vow.first())).append(Component.literal(" ")).append(button(vow.second())));
	}

	private static MutableComponent button(CircleVows.Option option) {
		return Component.literal("[" + option.name() + "]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
			.withClickEvent(new ClickEvent.RunCommand("/vow take " + option.id()))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal(option.text()))));
	}

	static int list(ServerPlayer player) {
		int saved = Heart.vows(player), active = Heart.active(player);
		player.sendSystemMessage(text("header", "Circle Vows: one choice at each of Circles IX, XI, XIII, XV, XVII, XIX and XX. Releasing one costs %s levels.",
			CircleVows.RELEASE_LEVELS).withColor(GOLD));
		for (CircleVows.Vow vow : CircleVows.ALL) {
			int choice = CircleVows.choice(saved, vow);
			MutableComponent line = Component.literal(vow.numeral() + ": ").withColor(MUTED);
			if (choice != CircleVows.NONE) {
				CircleVows.Option option = vow.option(choice);
				line.append(Component.literal(option.name() + " - " + option.text()).withStyle(ChatFormatting.WHITE));
				if (active < vow.circle()) line.append(text("silent", " (silent while this circle is unformed or cracked)").withStyle(ChatFormatting.GRAY));
				line.append(Component.literal(" ")).append(Component.literal("[Release]").withStyle(style -> style.withColor(ChatFormatting.RED)
					.withClickEvent(new ClickEvent.SuggestCommand("/vow release " + vow.circle()))
					.withHoverEvent(new HoverEvent.ShowText(text("release_hover", "Costs %s levels; choose again afterwards for free", CircleVows.RELEASE_LEVELS)))));
			} else if (active >= vow.circle()) {
				line.append(button(vow.first())).append(Component.literal(" or ")).append(button(vow.second()));
			} else {
				line.append(Component.literal(vow.first().name() + " (" + vow.first().text() + ") or " + vow.second().name() + " (" + vow.second().text() + ")")
					.withStyle(ChatFormatting.DARK_GRAY));
			}
			player.sendSystemMessage(line);
		}
		return 1;
	}

	static int take(ServerPlayer player, String id) {
		CircleVows.Vow vow = null;
		int choice = CircleVows.NONE;
		for (CircleVows.Vow candidate : CircleVows.ALL) {
			int found = CircleVows.choice(candidate, id);
			if (found != CircleVows.NONE) { vow = candidate; choice = found; }
		}
		if (vow == null) return refuse(player, text("unknown", "No vow is called that. Use /vow to see them."));
		int saved = Heart.vows(player);
		CircleVows.Refusal refusal = CircleVows.take(saved, Heart.active(player), vow.circle());
		if (refusal == CircleVows.Refusal.NOT_REACHED)
			return refuse(player, text("not_reached", "Form the %s Circle first (a cracked circle cannot take a vow).", vow.numeral()));
		if (refusal == CircleVows.Refusal.ALREADY_TAKEN)
			return refuse(player, text("taken", "You already hold the %s Circle's vow. Release it first: /vow release %s", vow.numeral(), vow.circle()));
		player.setAttached(WildercordAttachments.CIRCLE_VOWS, CircleVows.with(saved, vow, choice));
		clampMana(player);
		CircleVows.Option option = vow.option(choice);
		player.sendSystemMessage(text("taken_now", "You vow %s: %s.", option.name(), option.text()).withColor(GOLD));
		return 1;
	}

	static int release(ServerPlayer player, int circle) {
		int saved = Heart.vows(player);
		CircleVows.Refusal refusal = CircleVows.release(saved, circle, player.experienceLevel, player.isCreative());
		if (refusal == CircleVows.Refusal.NO_VOW) return refuse(player, text("no_vow", "That circle asks for no vow."));
		if (refusal == CircleVows.Refusal.NOT_TAKEN) return refuse(player, text("not_taken", "You hold no vow at that circle."));
		if (refusal == CircleVows.Refusal.LEVELS)
			return refuse(player, text("levels", "Releasing a vow costs %s experience levels.", CircleVows.RELEASE_LEVELS));
		CircleVows.Vow vow = CircleVows.at(circle);
		if (!player.isCreative()) player.giveExperienceLevels(-CircleVows.RELEASE_LEVELS);
		player.setAttached(WildercordAttachments.CIRCLE_VOWS, CircleVows.with(saved, vow, CircleVows.NONE));
		clampMana(player);
		player.sendSystemMessage(text("released", "The %s Circle's vow is released. Choose again: ", vow.numeral()).withColor(GOLD)
			.append(button(vow.first())).append(Component.literal(" ")).append(button(vow.second())));
		return 1;
	}

	private static void clampMana(ServerPlayer player) {
		if (Spellbooks.tier(player) != null) Spellbooks.setMana(player, Math.min(Spellbooks.mana(player), Mana.max(player)));
	}

	private static int refuse(ServerPlayer player, MutableComponent message) {
		player.sendSystemMessage(message.withStyle(ChatFormatting.RED));
		return 0;
	}

	private static MutableComponent text(String key, String fallback, Object... args) {
		return Component.translatableWithFallback("message.wildercord.vow." + key, fallback, args);
	}
}
