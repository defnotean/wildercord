package dev.wildercord.client;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.Attunements;
import dev.wildercord.spell.Feats;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The Blank Rune's hint that some lands hold runes of their own (Attunement), and how many of them
 * the player has attuned; the Grimoire keeps their riddles.
 */
public final class BlankRuneTooltip {
	private BlankRuneTooltip() {}

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!stack.is(WildercordItems.BLANK_RUNE)) {
				return;
			}
			lines.add(Component.translatable("tooltip.wildercord.blank_rune.attune").withStyle(ChatFormatting.DARK_AQUA));
			Minecraft minecraft = Minecraft.getInstance();
			// The player's own count only on the game's thread: creative search builds its index from tooltips in the
			// background, and the player's data isn't safe to read from there.
			if (minecraft.player != null && minecraft.isSameThread()) {
				int attuned = Feats.count(Heart.grimoire(minecraft.player), "attune:");
				lines.add(Component.translatable("tooltip.wildercord.blank_rune.attuned", attuned, Attunements.RULES.size())
					.withStyle(ChatFormatting.DARK_GRAY));
			}
		});
	}
}
