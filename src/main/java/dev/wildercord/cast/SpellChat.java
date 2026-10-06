package dev.wildercord.cast;

import dev.wildercord.content.RuneItem;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCodes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNames;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Spell codes in chat: a {@code wc:...} code in a chat message becomes a coloured spell card.
 * Hover it to read the spell (its runes, what it does, its cost); click it to copy the code, then
 * paste it into the Cord screen to load it.
 */
public final class SpellChat {
	private SpellChat() {}

	public static void init() {
		ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, (player, message) -> decorate(message));
	}

	/** Chat cards and editor paste use the same complete token, including composite ids and refused overlong rows. */
	static Component decorate(Component message) {
		String text = message.getString();
		if (!text.contains(SpellCodes.PREFIX)) return message;
		Matcher m = SpellCodes.PATTERN.matcher(text);
		MutableComponent out = Component.empty().withStyle(message.getStyle());
		int last = 0;
		boolean any = false;
		while (m.find()) {
			String code = m.group();
			List<RuneDef> runes = runes(code);
			if (runes.isEmpty()) continue;
			out.append(Component.literal(text.substring(last, m.start())));
			out.append(card(code, runes));
			last = m.end(); any = true;
		}
		if (!any) return message;
		out.append(Component.literal(text.substring(last)));
		return out;
	}

	private static List<RuneDef> runes(String code) {
		List<RuneDef> runes = new ArrayList<>();
		List<String> ids = SpellCodes.decode(code);
		for (String id : ids) Runes.get(id).ifPresent(runes::add);
		if (dev.wildercord.spell.RelayRules.containsIds(ids) && (runes.size() != ids.size() || !dev.wildercord.spell.RelayRules.valid(runes))) return List.of(Runes.RELAY);
		if (dev.wildercord.spell.ExciseRules.containsIds(ids) && (runes.size() != ids.size() || !dev.wildercord.spell.ExciseRules.valid(runes))) return List.of(Runes.EXCISE);
		if (dev.wildercord.spell.ReweaveRules.containsIds(ids) && (runes.size() != ids.size() || !dev.wildercord.spell.ReweaveRules.valid(runes))) return List.of(Runes.REWEAVE);
		return runes;
	}

	/** "[✦ Splitting Frost Bolt]": the spell's name in its colour, with the readout on hover. */
	public static MutableComponent card(String code, List<RuneDef> runes) {
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		int color = Runebound.elementColor(runes);
		MutableComponent hover = Component.literal(SpellNames.auto(runes)).withColor(color);
		MutableComponent row = Component.empty();
		for (int i = 0; i < runes.size(); i++) {
			if (i > 0) {
				row.append(Component.literal(" · ").withColor(0x5A5470));
			}
			row.append(RuneItem.runeName(runes.get(i)).withColor(RuneColors.of(runes.get(i))));
		}
		hover.append("\n").append(row);
		hover.append("\n").append(Component.translatable("chat.wildercord.spell_cost", compiled.manaCost(),
			String.format(java.util.Locale.ROOT, "%.1f", compiled.cooldownTicks() / 20.0)).withColor(0x7FE0F0));
		for (String line : compiled.lines()) {
			hover.append("\n").append(Component.literal(line).withColor(0xC8C4D8));
		}
		hover.append("\n").append(Component.translatable("chat.wildercord.spell_copy").withColor(0x8A84A0));
		return Component.literal("[✦ " + SpellNames.auto(runes) + "]").withStyle(Style.EMPTY.withColor(color)
			.withHoverEvent(new HoverEvent.ShowText(hover))
			.withClickEvent(new ClickEvent.CopyToClipboard(code))
			.withUnderlined(false));
	}
}
