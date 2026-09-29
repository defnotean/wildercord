package dev.wildercord.client;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tooltips wrapped to a readable width. The game draws every tooltip line on one line however long
 * it is, and many rune descriptions run to a sentence or three (Imbue's to several hundred pixels),
 * so on anything but a wide screen they ran off its side. The Cord screen and the other screens wrap
 * their own tooltips here, and the tooltips of the mod's items are wrapped as they're built.
 */
public final class Tooltips {
	private Tooltips() {}

	/** The widest a tooltip grows, in GUI pixels: a comfortable line for reading. */
	private static final int MAX_WIDTH = 280;
	/** After everything else (this mod's and others') has added its lines to an item's tooltip. */
	private static final Identifier WRAP_PHASE = Wildercord.id("wrap_tooltips");

	/** How wide a tooltip may be on a screen {@code screenWidth} wide: never wider than fits on it. */
	public static int maxWidth(int screenWidth) {
		return Math.max(100, Math.min(MAX_WIDTH, screenWidth - 8));
	}

	/**
	 * A screen's own tooltip, wrapped for a screen {@code screenWidth} by {@code screenHeight}. One taller
	 * than the screen (the mana badge's, well equipped, on a short window) ends in "…" instead: the game
	 * would push its top, where its title and main numbers are, off the screen.
	 */
	public static List<FormattedCharSequence> fit(Font font, List<Component> lines, int screenWidth, int screenHeight) {
		List<FormattedCharSequence> wrapped = wrap(font, lines, maxWidth(screenWidth));
		// Each line is 10 pixels, and the frame takes a few more above and below.
		int room = Math.max(2, (screenHeight - 8) / 10);
		if (wrapped.size() > room) {
			wrapped = new ArrayList<>(wrapped.subList(0, room - 1));
			wrapped.add(Component.literal("…").withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
		}
		return wrapped;
	}

	/** Every line wrapped to {@code width}; lines that fit stay as they are. */
	public static List<FormattedCharSequence> wrap(Font font, List<Component> lines, int width) {
		List<FormattedCharSequence> out = new ArrayList<>();
		for (Component line : lines) {
			if (font.width(line) <= width) {
				out.add(line.getVisualOrderText());
			} else {
				out.addAll(font.split(line, width));
			}
		}
		return out;
	}

	public static void init() {
		ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, WRAP_PHASE);
		ItemTooltipCallback.EVENT.register(WRAP_PHASE, (stack, context, flag, lines) -> {
			Minecraft mc = Minecraft.getInstance();
			// Creative search reads tooltips on other threads, where the font mustn't be touched (and
			// wrapping doesn't matter to a search).
			if (!mc.isSameThread() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Wildercord.MOD_ID)) {
				return;
			}
			int width = maxWidth(mc.getWindow().getGuiScaledWidth());
			for (int i = 0; i < lines.size(); i++) {
				Component line = lines.get(i);
				if (mc.font.width(line) <= width) {
					continue;
				}
				List<Component> pieces = split(mc.font, line, width);
				lines.remove(i);
				lines.addAll(i, pieces);
				i += pieces.size() - 1;
			}
		});
	}

	/** A line cut into lines no wider than {@code width}, each keeping its styles. */
	private static List<Component> split(Font font, Component line, int width) {
		List<Component> pieces = new ArrayList<>();
		for (FormattedText piece : font.getSplitter().splitLines(line, width, Style.EMPTY)) {
			MutableComponent part = Component.empty();
			piece.visit((style, text) -> {
				part.append(Component.literal(text).withStyle(style));
				return Optional.empty();
			}, Style.EMPTY);
			pieces.add(part);
		}
		return pieces;
	}
}
