package dev.wildercord.client;

import dev.wildercord.content.Imbued;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellNames;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What an imbued item holds, under its name: the spell, the charges left, and how it lets go. */
public final class ImbuedTooltip {
	private ImbuedTooltip() {}

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			Imbued imbued = stack.get(WildercordComponents.IMBUED);
			if (imbued == null) {
				return;
			}
			List<RuneDef> runes = new ArrayList<>();
			imbued.runes().forEach(id -> Runes.get(id).ifPresent(runes::add));
			int at = Math.min(1, lines.size());
			lines.add(at, Component.translatable("tooltip.wildercord.imbued", Component.literal(SpellNames.auto(runes)).withColor(imbued.color()))
				.withColor(0xE8E0FF));
			lines.add(at + 1, Component.translatable("tooltip.wildercord.imbued_charges", imbued.charges(),
				Component.translatable("tooltip.wildercord.imbued." + Imbued.release(stack).name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.GRAY));
		});
	}
}
