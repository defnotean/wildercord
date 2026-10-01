package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.MasteryChoices;
import dev.wildercord.content.RuneItem;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.RuneDef;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The Cord screen's mastery panel, over the window: how far the edited spell has grown with you. Its circle with your
 * sigil, its rank and a bar toward the next, the trait in each of its four slots (one for each of ranks II to V) and,
 * when a rank has been reached and its trait not yet chosen, the three traits offered for it, each a card to click.
 * Each rank allows one change of mind for a few experience levels: re-roll the offer before choosing, or unbind the
 * trait chosen to choose again. It opens from the rank badge at the end of a spell's row, and by itself when the Cord
 * screen opens with a trait waiting.
 *
 * <p>Everything is asked of the server (see {@link MasteryChoices}), which checks it against its own record and answers
 * above the hotbar; what's shown is the synced {@link MasteryBook}. Drawn in the Cord screen's own local space, with its
 * sprites and colours.</p>
 */
final class MasteryPanel {
	private static final int X = 16;
	private static final int TOP = 22;
	private static final int PAD = 8;
	private static final int BUTTON = 14;
	private static final int LEFT_W = 112;
	private static final int SLOT_H = 20;
	/** The help at the foot: up to this many lines. */
	private static final int HELP_LINES = 3;

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int WARN = 0xFFF0C440;
	private static final int LAVENDER = 0xFFB8A8FF;
	private static final int CYAN = 0xFF7FE0F0;

	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_ROW = Wildercord.id("cord/row");
	private static final Identifier SPR_ROW_SELECTED = Wildercord.id("cord/row_selected");
	private static final Identifier SPR_TAB = Wildercord.id("cord/tab");
	private static final Identifier SPR_TAB_ACTIVE = Wildercord.id("cord/tab_active");

	/** The colour of each rank, from Kindled to Mythic. */
	static final int[] RANK_COLORS = {0xFFB8A890, 0xFF9FD8C0, 0xFF8FB8FF, 0xFFE8C46A, 0xFFF0A8FF};

	/** What the panel shows: the spell in Cord slot {@code spell}, its runes as they fire, its name and its record (null for none yet). */
	record Spell(int spell, List<RuneDef> runes, String name, MasteryBook.Entry entry, long seed) {}

	private boolean open;
	private int spell = -1;
	/** When the panel opened, for its circle's opening. */
	private long openedAt;

	boolean isOpen() {
		return open;
	}

	int spell() {
		return spell;
	}

	void open(int spell) {
		this.open = true;
		this.spell = spell;
		this.openedAt = net.minecraft.util.Util.getMillis();
	}

	void close() {
		open = false;
	}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	// ------------------------------------------------------------------ layout

	private static int right(int windowW) {
		return windowW - X;
	}

	private static int bottom(int windowH) {
		return windowH - 14;
	}

	private static int closeX(int windowW) {
		return right(windowW) - PAD - BUTTON;
	}

	private static int columnX() {
		return X + PAD + LEFT_W + 6;
	}

	private static int columnW(int windowW) {
		return right(windowW) - PAD - columnX();
	}

	private static int slotY(int slot) {
		return TOP + 30 + slot * SLOT_H;
	}

	private static int choiceTop() {
		return slotY(MasteryRules.SLOTS) + 6;
	}

	private static int cardTop() {
		return choiceTop() + 12;
	}

	/** A card's height: its name and two lines of what it does (one line when a borrowed trait makes four cards). */
	private static int cardH(int n) {
		return n <= MasteryRules.OFFER ? 29 : 20;
	}

	/** The cards stack down the right column, one under another, so each trait's words have room. */
	private static int cardY(int i, int n) {
		return cardTop() + i * (cardH(n) + 2);
	}

	/** The re-roll button sits in the left column, under the bar, while a trait is waiting. */
	private static int rerollX(int windowW, Font font) {
		return X + PAD + 4;
	}

	private static int rerollW(Font font) {
		return LEFT_W - 8;
	}

	private static int rerollY() {
		return TOP + 24 + 96 + 56;
	}

	private static int unbindX(int windowW) {
		return right(windowW) - PAD - BUTTON - 2;
	}

	/** The middle of offered trait {@code i}'s card, in the Cord screen's local space, or null when nothing is offered. */
	double[] cardPoint(Spell shown, int i, int windowW) {
		MasteryBook.Entry entry = shown.entry();
		if (entry == null || entry.pendingSlot() < 0 || i >= entry.offer().size()) {
			return null;
		}
		int n = entry.offer().size();
		return new double[] {columnX() + columnW(windowW) / 2.0, cardY(i, n) + cardH(n) / 2.0};
	}

	/** The middle of the re-roll button, in local space. */
	double[] rerollPoint(int windowW) {
		return new double[] {rerollX(windowW, mc().font) + rerollW(mc().font) / 2.0, rerollY() + 6.5};
	}

	// ------------------------------------------------------------------ drawing

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	private static void fitText(GuiGraphicsExtractor g, Font font, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
		if (maxWidth <= 0) {
			return;
		}
		if (font.width(text) <= maxWidth) {
			g.text(font, text, x, y, color, shadow);
			return;
		}
		String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("…"))) + "…";
		g.text(font, Component.literal(plain).withStyle(text.getStyle()), x, y, color, shadow);
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	static int rankColor(int rank) {
		return RANK_COLORS[Math.max(1, Math.min(MasteryRules.MAX_RANK, rank)) - 1];
	}

	static Component rankName(int rank) {
		return Component.translatable("mastery.wildercord.rank." + Math.max(1, Math.min(MasteryRules.MAX_RANK, rank)));
	}

	static Component traitName(String id) {
		return MasteryTraits.get(id).map(MasteryChoices::traitName).orElse(Component.literal(id));
	}

	static Component traitDesc(String id) {
		return MasteryTraits.get(id).map(t -> (Component) Component.translatableWithFallback("mastery.wildercord.trait." + t.id().replace(':', '.') + ".desc",
			t.desc())).orElse(Component.empty());
	}

	/** Draws the panel over the window (in its local space) and returns the tooltip under the mouse, if any. */
	List<Component> draw(GuiGraphicsExtractor g, int mx, int my, int windowW, int windowH, Spell shown) {
		Font font = mc().font;
		g.fill(0, 0, windowW, windowH, 0x90080610);
		int right = right(windowW);
		int bottom = bottom(windowH);
		sprite(g, SPR_PANEL, X, TOP, right - X, bottom - TOP);
		List<Component> tip = null;
		MasteryBook.Entry entry = shown.entry();
		int rank = entry == null ? MasteryRules.FIRST : entry.rank();
		int rankColor = rankColor(rank);

		// The title: the spell's name, and a close button.
		Component title = Component.translatable("screen.wildercord.mastery.title", Component.literal(shown.name()).withColor(TEXT));
		fitText(g, font, title, X + PAD + 2, TOP + PAD, closeX(windowW) - 6 - (X + PAD + 2), GOLD, true);
		int cx = closeX(windowW);
		boolean closeHover = inside(mx, my, cx, TOP + PAD - 3, BUTTON, BUTTON);
		sprite(g, closeHover ? SPR_TAB_ACTIVE : SPR_TAB, cx, TOP + PAD - 3, BUTTON, BUTTON);
		g.text(font, "×", cx + BUTTON / 2 - font.width("×") / 2 + 1, TOP + PAD, closeHover ? TEXT : DIM, false);
		if (closeHover) {
			tip = List.of(Component.translatable("screen.wildercord.mastery.close").withStyle(ChatFormatting.GRAY));
		}

		// Left: the spell's circle with your sigil, its rank and the bar toward the next.
		int left = X + PAD;
		sprite(g, SPR_INSET, left, TOP + 24, LEFT_W, bottom - TOP - 24 - PAD - HELP_LINES * 9 - 4);
		float circleX = left + LEFT_W / 2F;
		float circleY = TOP + 24 + 50;
		float time = (mc().level == null ? 0 : mc().level.getGameTime()) / 20F;
		float opening = Math.min(1, (net.minecraft.util.Util.getMillis() - openedAt) / 700F);
		MasteryAttachments.Look look = new MasteryAttachments.Look(0, rank, shown.seed(),
			entry != null && entry.active().contains(MasteryTraits.DEEP_HUE.id()) ? MasteryAttachments.Look.HUE : 0);
		GuiSpellCircle.draw(g, circleX, circleY, 34, shown.runes(), time, opening, look);
		int y = TOP + 24 + 96;
		Component rankLine = rankName(rank);
		g.centeredText(font, rankLine, Math.round(circleX), y, rankColor);
		g.centeredText(font, Component.translatable("screen.wildercord.mastery.rank_of", RuneItem.roman(rank), RuneItem.roman(MasteryRules.MAX_RANK)),
			Math.round(circleX), y + 11, DIM);
		// The bar toward the next rank.
		int barX = left + 8;
		int barW = LEFT_W - 16;
		int barY = y + 25;
		double total = entry == null ? 0 : entry.total();
		g.fill(barX, barY, barX + barW, barY + 4, 0xFF221D32);
		g.fill(barX, barY, barX + (int) Math.round(barW * MasteryRules.progress(total)), barY + 4, rankColor);
		Component amount = rank >= MasteryRules.MAX_RANK ? Component.translatable("screen.wildercord.mastery.max")
			: Component.translatable("screen.wildercord.mastery.xp", (int) Math.floor(total), MasteryRules.threshold(rank + 1));
		g.centeredText(font, amount, Math.round(circleX), barY + 7, FAINT);
		if (inside(mx, my, barX, barY - 2, barW, 18)) {
			tip = xpTooltip(entry, rank);
		}
		if (entry != null && !entry.teacher().isEmpty()) {
			fitText(g, font, Component.translatable("screen.wildercord.mastery.taught", entry.teacher()), barX - 4, barY + 19, barW + 8, LAVENDER, false);
		}
		if (inside(mx, my, (int) circleX - 34, (int) circleY - 34, 68, 68)) {
			tip = List.of(Component.translatable("screen.wildercord.mastery.sigil").withColor(GOLD),
				Component.translatable("screen.wildercord.mastery.sigil.hint").withStyle(ChatFormatting.GRAY));
		}

		// Right: the four trait slots.
		int colX = columnX();
		int colW = columnW(windowW);
		g.text(font, Component.translatable("screen.wildercord.mastery.traits"), colX, TOP + 22, GOLD, false);
		int pending = entry == null ? -1 : entry.pendingSlot();
		for (int slot = 0; slot < MasteryRules.SLOTS; slot++) {
			List<Component> slotTip = drawSlot(g, font, entry, slot, pending, rank, mx, my, windowW);
			if (slotTip != null) {
				tip = slotTip;
			}
		}

		// Below them: the choice waiting, or how the spell has been used.
		if (pending >= 0 && !entry.offer().isEmpty()) {
			List<Component> choiceTip = drawChoice(g, font, entry, pending, mx, my, windowW);
			if (choiceTip != null) {
				tip = choiceTip;
			}
		} else {
			drawUsage(g, font, entry, colX, choiceTop(), colW);
		}

		// The foot: what experience comes from.
		List<FormattedCharSequence> help = font.split(Component.translatable("screen.wildercord.mastery.help"), right - X - 2 * PAD - 4);
		int helpY = bottom - PAD - HELP_LINES * 9;
		for (int i = 0; i < Math.min(HELP_LINES, help.size()); i++) {
			g.text(font, help.get(i), X + PAD + 2, helpY + i * 9, FAINT, false);
		}
		return tip;
	}

	private List<Component> drawSlot(GuiGraphicsExtractor g, Font font, MasteryBook.Entry entry, int slot, int pending, int rank, int mx, int my, int windowW) {
		int x = columnX();
		int w = columnW(windowW);
		int y = slotY(slot);
		int slotRank = MasteryRules.rankOf(slot);
		boolean reached = rank >= slotRank;
		String trait = entry == null ? "" : entry.traits().get(slot);
		boolean borrowed = entry != null && entry.borrowed(slot);
		boolean settled = entry != null && entry.settled(slot);
		boolean waiting = slot == pending;
		sprite(g, waiting ? SPR_ROW_SELECTED : SPR_ROW, x, y, w, SLOT_H - 2);
		// The rank this slot opens at, in its colour.
		String numeral = RuneItem.roman(slotRank);
		g.text(font, numeral, x + 4, y + 6, reached ? rankColor(slotRank) : FAINT, true);
		int textX = x + 20;
		int textW = w - 24 - (settled ? BUTTON + 4 : 0);
		List<Component> tip = null;
		if (!trait.isEmpty()) {
			fitText(g, font, traitName(trait), textX, y + 2, textW, borrowed ? LAVENDER : GOLD, false);
			fitText(g, font, traitDesc(trait), textX, y + 10, textW, DIM, false);
		} else if (waiting) {
			fitText(g, font, Component.translatable("screen.wildercord.mastery.choose_below"), textX, y + 6, textW, WARN, false);
		} else {
			fitText(g, font, Component.translatable("screen.wildercord.mastery.locked", rankName(slotRank)), textX, y + 6, textW, FAINT, false);
		}
		if (inside(mx, my, x, y, w - (settled ? BUTTON + 6 : 0), SLOT_H - 2)) {
			tip = new ArrayList<>();
			if (!trait.isEmpty()) {
				tip.add(traitName(trait).copy().withColor(borrowed ? LAVENDER : GOLD));
				tip.add(traitDesc(trait).copy().withStyle(ChatFormatting.GRAY));
				if (borrowed) {
					tip.add(Component.translatable("screen.wildercord.mastery.borrowed", rankName(slotRank)).withColor(LAVENDER));
				}
			} else {
				tip.add(Component.translatable("screen.wildercord.mastery.slot", rankName(slotRank)).withColor(GOLD));
				tip.add(Component.translatable(waiting ? "screen.wildercord.mastery.slot.waiting" : "screen.wildercord.mastery.slot.hint")
					.withStyle(ChatFormatting.GRAY));
			}
		}
		if (settled) {
			int bx = unbindX(windowW);
			boolean can = !entry.changed(slot) && pending < 0;
			boolean hover = inside(mx, my, bx, y + 2, BUTTON, BUTTON);
			sprite(g, hover && can ? SPR_TAB_ACTIVE : SPR_TAB, bx, y + 2, BUTTON, BUTTON);
			g.text(font, "✎", bx + BUTTON / 2 - font.width("✎") / 2 + 1, y + 5, can ? hover ? TEXT : DIM : FAINT, false);
			if (hover) {
				tip = List.of(Component.translatable("screen.wildercord.mastery.unbind").withColor(GOLD),
					Component.translatable(can ? "screen.wildercord.mastery.unbind.hint" : entry.changed(slot)
						? "screen.wildercord.mastery.changed" : "screen.wildercord.mastery.unbind.wait", MasteryRules.CHANGE_LEVELS).withStyle(ChatFormatting.GRAY));
			}
		}
		return tip;
	}

	private List<Component> drawChoice(GuiGraphicsExtractor g, Font font, MasteryBook.Entry entry, int slot, int mx, int my, int windowW) {
		List<Component> tip = null;
		int x = columnX();
		int pulse = (net.minecraft.util.Util.getMillis() / 500) % 2 == 0 ? WARN : GOLD;
		fitText(g, font, Component.translatable("screen.wildercord.mastery.choose", rankName(MasteryRules.rankOf(slot))), x, choiceTop(),
			columnW(windowW), pulse, false);
		List<String> offer = entry.offer();
		int n = offer.size();
		String borrowed = entry.borrowed(slot) ? entry.traits().get(slot) : "";
		int cw = columnW(windowW);
		for (int i = 0; i < n; i++) {
			String id = offer.get(i);
			int cy = cardY(i, n);
			int ch = cardH(n);
			boolean hover = inside(mx, my, x, cy, cw, ch);
			boolean keep = id.equals(borrowed);
			sprite(g, hover ? SPR_ROW_SELECTED : SPR_ROW, x, cy, cw, ch);
			Component name = keep ? Component.translatable("screen.wildercord.mastery.keep", traitName(id)) : traitName(id);
			fitText(g, font, name, x + 5, cy + 2, cw - 10, keep ? LAVENDER : GOLD, false);
			List<FormattedCharSequence> desc = font.split(traitDesc(id), cw - 10);
			int lines = (ch - 11) / 9;
			for (int k = 0; k < Math.min(lines, desc.size()); k++) {
				boolean last = k == lines - 1 && desc.size() > lines;
				if (last) {
					fitText(g, font, Component.literal(remainder(font, desc, k)), x + 5, cy + 11 + k * 9, cw - 10, TEXT, false);
				} else {
					g.text(font, desc.get(k), x + 5, cy + 11 + k * 9, TEXT, false);
				}
			}
			if (hover) {
				tip = new ArrayList<>(List.of(traitName(id).copy().withColor(GOLD), traitDesc(id).copy().withStyle(ChatFormatting.GRAY),
					Component.translatable("screen.wildercord.mastery.card_hint").withColor(CYAN)));
			}
		}
		// The one change this rank allows, before choosing: a fresh offer.
		boolean can = !entry.changed(slot);
		Component label = Component.translatable("screen.wildercord.mastery.reroll", MasteryRules.CHANGE_LEVELS);
		int bw = rerollW(font);
		int bx = rerollX(windowW, font);
		boolean hover = inside(mx, my, bx, rerollY(), bw, 13);
		sprite(g, hover && can ? SPR_TAB_ACTIVE : SPR_TAB, bx, rerollY(), bw, 13);
		g.centeredText(font, label, bx + bw / 2, rerollY() + 3, can ? hover ? TEXT : DIM : FAINT);
		if (hover) {
			tip = List.of(label.copy().withColor(GOLD), Component.translatable(can ? "screen.wildercord.mastery.reroll.hint" : "screen.wildercord.mastery.changed",
				MasteryRules.CHANGE_LEVELS).withStyle(ChatFormatting.GRAY));
		}
		return tip;
	}

	/** What's left of a trait's description from wrapped line {@code from} on, as one line (to be cut short with an ellipsis). */
	private static String remainder(Font font, List<FormattedCharSequence> lines, int from) {
		StringBuilder out = new StringBuilder();
		for (int k = from; k < lines.size(); k++) {
			StringBuilder line = new StringBuilder();
			lines.get(k).accept((index, style, codepoint) -> {
				line.appendCodePoint(codepoint);
				return true;
			});
			if (out.length() > 0) {
				out.append(' ');
			}
			out.append(line.toString().strip());
		}
		return out.toString();
	}

	/** How the spell has been used: the circumstances most of its casts were in, which shape what traits are offered. */
	private static void drawUsage(GuiGraphicsExtractor g, Font font, MasteryBook.Entry entry, int x, int y, int w) {
		g.text(font, Component.translatable("screen.wildercord.mastery.usage"), x, y, DIM, false);
		if (entry == null || entry.casts() <= 0) {
			List<FormattedCharSequence> none = font.split(Component.translatable("screen.wildercord.mastery.usage.none"), w);
			for (int i = 0; i < Math.min(3, none.size()); i++) {
				g.text(font, none.get(i), x, y + 12 + i * 9, FAINT, false);
			}
			return;
		}
		List<Map.Entry<String, Integer>> top = entry.counters().entrySet().stream()
			.filter(e -> e.getValue() > 0)
			.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
			.limit(4).toList();
		int line = 0;
		for (Map.Entry<String, Integer> e : top) {
			int percent = (int) Math.round(100.0 * e.getValue() / entry.casts());
			Component text = Component.translatable("screen.wildercord.mastery.usage.line",
				Component.translatableWithFallback("mastery.wildercord.circumstance." + e.getKey(), e.getKey()), percent);
			fitText(g, font, text, x + 4, y + 12 + line * 9, w - 4, TEXT, false);
			g.fill(x + w - 40, y + 14 + line * 9, x + w - 40 + Math.round(36 * Math.min(1, percent / 100F)), y + 17 + line * 9, 0xFF7FE0F0);
			line++;
		}
		List<FormattedCharSequence> hint = font.split(Component.translatable("screen.wildercord.mastery.usage.hint"), w);
		for (int i = 0; i < Math.min(2, hint.size()); i++) {
			g.text(font, hint.get(i), x, y + 14 + line * 9 + i * 9, FAINT, false);
		}
	}

	/** The bar's tooltip: the experience in plain words, and what earns it. */
	private static List<Component> xpTooltip(MasteryBook.Entry entry, int rank) {
		List<Component> tip = new ArrayList<>();
		tip.add(rankName(rank).copy().withColor(rankColor(rank)));
		double xp = entry == null ? 0 : entry.xp();
		double practice = entry == null ? 0 : entry.practice();
		if (rank < MasteryRules.MAX_RANK) {
			tip.add(Component.translatable("screen.wildercord.mastery.next", (int) Math.ceil(MasteryRules.threshold(rank + 1) - xp - practice),
				rankName(rank + 1)).withStyle(ChatFormatting.GRAY));
		}
		tip.add(Component.translatable("screen.wildercord.mastery.practice", (int) Math.floor(practice), (int) MasteryRules.PRACTICE_CAP).withStyle(ChatFormatting.GRAY));
		tip.add(Component.translatable("screen.wildercord.mastery.help").withStyle(ChatFormatting.DARK_GRAY));
		return tip;
	}

	// ------------------------------------------------------------------ input

	/** A click in the Cord screen's local space while the panel is open. */
	void click(double mx, double my, int windowW, int windowH, Spell shown) {
		int cx = closeX(windowW);
		if (inside(mx, my, cx, TOP + PAD - 3, BUTTON, BUTTON) || !inside(mx, my, X, TOP, right(windowW) - X, bottom(windowH) - TOP)) {
			close();
			sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
			return;
		}
		MasteryBook.Entry entry = shown.entry();
		if (entry == null) {
			return;
		}
		int pending = entry.pendingSlot();
		if (pending >= 0 && !entry.offer().isEmpty()) {
			int n = entry.offer().size();
			for (int i = 0; i < n; i++) {
				if (inside(mx, my, columnX(), cardY(i, n), columnW(windowW), cardH(n))) {
					send(MasteryChoices.CHOOSE, shown.spell(), pending, entry.offer().get(i));
					sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F);
					return;
				}
			}
			Font font = mc().font;
			if (inside(mx, my, rerollX(windowW, font), rerollY(), rerollW(font), 13) && !entry.changed(pending)) {
				send(MasteryChoices.REROLL, shown.spell(), pending, "");
				sound(SoundEvents.BOOK_PAGE_TURN, 1.0F);
				return;
			}
		}
		for (int slot = 0; slot < MasteryRules.SLOTS; slot++) {
			if (entry.settled(slot) && inside(mx, my, unbindX(windowW), slotY(slot) + 2, BUTTON, BUTTON) && !entry.changed(slot) && pending < 0) {
				send(MasteryChoices.UNBIND, shown.spell(), slot, "");
				sound(SoundEvents.BOOK_PAGE_TURN, 0.8F);
				return;
			}
		}
	}

	void key(KeyEvent event) {
		if (event.isEscape() || event.key() == InputConstants.KEY_M && event.hasControlDown()) {
			close();
		}
	}

	private static void send(int kind, int spell, int slot, String trait) {
		ClientPlayNetworking.send(new MasteryChoices.Request(kind, spell, slot, trait));
	}

	private static void sound(net.minecraft.sounds.SoundEvent sound, float pitch) {
		mc().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
	}
}
