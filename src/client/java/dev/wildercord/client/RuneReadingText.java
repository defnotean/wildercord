package dev.wildercord.client;

import dev.wildercord.cast.RuneReadings;
import dev.wildercord.content.RuneItem;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneHints;
import dev.wildercord.spell.RuneReading;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * A rune's text as the local player has read it (see {@link RuneReading}): a hint while it's unread, its text with the
 * numbers veiled (shimmering, unreadable) once glimpsed, and in full once understood. The Codex and every rune item's
 * tooltip go through here, so nothing on the client tells more than the player has made out.
 */
public final class RuneReadingText {
	private RuneReadingText() {}

	/** The colour veiled numbers are drawn in: a pale violet shimmer. */
	private static final int VEIL = 0xB8A8E8;

	public static RuneReading.Stage stage(RuneDef rune) {
		return stage(Minecraft.getInstance().player, rune);
	}

	/** How far {@code player} has read the rune (a screen passes its own player, so it can be asked from any thread). */
	public static RuneReading.Stage stage(net.minecraft.world.entity.player.Player player, RuneDef rune) {
		return RuneReadings.stage(player, rune);
	}

	/** The rune's text, as far as it's been read. */
	public static MutableComponent describe(RuneDef rune) {
		return switch (stage(rune)) {
			case UNDERSTOOD -> RuneItem.runeDescription(rune);
			case GLIMPSED -> glimpse(rune);
			case UNREAD -> Component.literal(RuneHints.hint(rune)).withStyle(ChatFormatting.ITALIC);
		};
	}

	/** The text with every number veiled. */
	static MutableComponent glimpse(RuneDef rune) {
		MutableComponent out = Component.empty();
		for (RuneHints.Piece piece : RuneHints.glimpse(RuneItem.runeDescription(rune).getString())) {
			out.append(piece.veiled() ? Component.literal(piece.text()).withStyle(ChatFormatting.OBFUSCATED).withColor(VEIL) : Component.literal(piece.text()));
		}
		return out;
	}

	/** A line saying how far a rune has been read, or null once it's understood. */
	public static Component stageLine(RuneDef rune) {
		return switch (stage(rune)) {
			case UNREAD -> Component.translatable("screen.wildercord.rune_unread").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
			case GLIMPSED -> Component.translatable("screen.wildercord.rune_glimpsed").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
			case UNDERSTOOD -> null;
		};
	}

	/** What a search may match in a rune's text: only what the player can read of it. */
	public static String searchable(net.minecraft.world.entity.player.Player player, RuneDef rune) {
		return switch (stage(player, rune)) {
			case UNDERSTOOD -> RuneItem.runeDescription(rune).getString().toLowerCase(Locale.ROOT);
			case GLIMPSED -> RuneHints.glimpseText(RuneItem.runeDescription(rune).getString(), " ").toLowerCase(Locale.ROOT);
			case UNREAD -> RuneHints.hint(rune).toLowerCase(Locale.ROOT);
		};
	}

	/** Tells rune items' tooltips to go through here. */
	public static void init() {
		RuneItem.describe = RuneReadingText::describe;
	}
}
