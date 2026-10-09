package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.TechniqueRules;
import dev.wildercord.aura.Techniques;
import dev.wildercord.aura.WayRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The Aura page's writing page: where a swordsman composes techniques of their own. Kin to the Cord, not a copy of it: a technique is
 * three parts threaded on a cord between seals (a stroke, a release, an intent) where a spell is a row of runes; the parts are picked from
 * a row of sockets as runes are from the Codex; and the readout says what it will do and cost. Its own: the name inked across the top of
 * the scroll as it's typed, a kata drawn beside the readout (the shape it strikes, seen from above, playing as it would: a wave flying, a
 * burst breaking out, an afterimage striking after), where it sits against the arts, the string composed from the swings themselves,
 * and the rank it has earned with the temper and edge chosen for it.
 *
 * <ul>
 * <li><b>The slots</b> along the top: one at Edge, two at Form, three at Sovereign; click one to write in it.</li>
 * <li><b>The scroll</b>: the name (click it and type), the three seals (click one to open its parts below), the parts as sockets (lit
 * when known, dark with where to find them when not, marked with a Way's colour when it lends them), the readout and the kata.</li>
 * <li><b>The string</b>: click the swings to compose it, the last one taken back with the arrow (or Backspace); anything it would meet
 * is said at once, and what goes first where two strings share swings.</li>
 * <li><b>Write</b> sets it down (the server checks it as the page did); <b>Erase</b> clears the slot (click twice).</li>
 * <li><b>Its rank</b> at the foot: how far it has come, and from Honed its temper, from Keen its edge, at Peerless its parts set down on
 * scrolls.</li>
 * </ul>
 */
public final class TechniquePage {
	static final int W = 320;
	static final int H = 340;
	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E0F0;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int LAVENDER = 0xFFB8A8FF;
	private static final int RED = 0xFFE08070;
	private static final int AMBER = 0xFFF0C060;
	private static final int GREEN = 0xFF9FE08A;
	private static final int INK = 0xFF120E1A;
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_ROW = Wildercord.id("cord/row");
	private static final Identifier SPR_ROW_SELECTED = Wildercord.id("cord/row_selected");
	private static final Identifier SPR_ROW_LOCKED = Wildercord.id("cord/row_locked");
	private static final Identifier SPR_SOCKET = Wildercord.id("cord/socket");
	private static final Identifier SPR_SOCKET_HOVER = Wildercord.id("cord/socket_hover");
	private static final Identifier SPR_THREAD = Wildercord.id("cord/thread");
	private static final Identifier SPR_LOCK = Wildercord.id("cord/lock");
	private static final Identifier SPR_SEAL = Wildercord.id("technique/seal");
	private static final Identifier SPR_SEAL_OPEN = Wildercord.id("technique/seal_open");

	/** Where the slots, the scroll and the strip at the foot sit (the page's own coordinates). */
	static final int SLOTS_Y = 38;
	static final int CARD_W = 92;
	static final int CARD_H = 33;
	static final int SCROLL_Y = 76;
	static final int SCROLL_BOTTOM = 299;
	static final int RANK_Y = 304;
	static final int SEALS_Y = 102;
	static final int SEAL = 26;
	static final int PICKER_Y = 146;
	static final int READOUT_Y = 172;
	static final int KATA_X = 206;
	static final int KATA_W = 96;
	static final int KATA_H = 60;
	static final int STRING_Y = 240;
	static final int TOKENS_Y = 256;
	static final int STATUS_Y = 284;

	// ---- what's being written (kept while the game runs, so a page closed and opened again keeps a draft)

	/** A technique being written: its name as typed, its parts, its string's swings. */
	record Draft(String name, String stroke, String release, String intent, List<SwordString.Token> tokens) {
		Draft {
			tokens = List.copyOf(tokens);
		}

		Draft name(String n) {
			return new Draft(n, stroke, release, intent, tokens);
		}

		Draft part(TechniqueRules.Family family, String id) {
			return switch (family) {
				case STROKE -> new Draft(name, id, release, intent, tokens);
				case RELEASE -> new Draft(name, stroke, id, intent, tokens);
				case INTENT -> new Draft(name, stroke, release, id, tokens);
			};
		}

		Draft tokens(List<SwordString.Token> t) {
			return new Draft(name, stroke, release, intent, t);
		}

		String of(TechniqueRules.Family family) {
			return switch (family) {
				case STROKE -> stroke;
				case RELEASE -> release;
				case INTENT -> intent;
			};
		}

		Optional<SwordString> string() {
			return tokens.isEmpty() || tokens.size() > SwordString.MAX_LENGTH ? Optional.empty() : Optional.of(new SwordString(tokens));
		}

		String key() {
			return TechniqueRules.key(stroke, release, intent);
		}

		/** Whether it says what {@code w} says (the name as it would be kept). */
		boolean same(Techniques.Written w) {
			return !w.empty() && w.stroke().equals(stroke) && w.release().equals(release) && w.intent().equals(intent)
				&& string().map(SwordString::text).orElse("").equals(w.string()) && TechniqueRules.cleanName(name).equals(w.name());
		}
	}

	private static int slot = 0;
	private static Draft draft;
	private static int draftFor = -1;
	private static TechniqueRules.Family open = TechniqueRules.Family.STROKE;
	private static boolean typing;
	/** When a technique was last sent to be written (the ink running into the seals), and when Erase was first clicked. */
	private static long inkAt = -1000;
	private static long eraseArmedAt = -1000;
	private static long openedAt;

	private final Minecraft minecraft;
	private final Font font;
	/** Every clickable thing as drawn last (the page's own coordinates: x, y, w, h), by what it does. */
	final Map<String, int[]> targets = new LinkedHashMap<>();

	TechniquePage(Minecraft minecraft, Font font) {
		this.minecraft = minecraft;
		this.font = font;
	}

	/** The page opened (its draft is the selected slot's, unless one is being written for it). */
	static void opened(long now) {
		openedAt = now;
	}

	/** Shows slot {@code i} (the game tests turn the pages), starting its draft afresh. */
	public static void select(int i) {
		slot = Math.max(0, Math.min(TechniqueRules.MAX_SLOTS - 1, i));
		draftFor = -1;
		typing = false;
	}

	/** Whether the name is being typed (the game tests ask). */
	public static boolean typing() {
		return typing;
	}

	/** The draft's string as written so far, and its name (the game tests read them). */
	public static String draftString() {
		return draft == null ? "" : draft.string().map(SwordString::text).orElse("");
	}

	public static String draftName() {
		return draft == null ? "" : draft.name();
	}

	// ------------------------------------------------------------------ drawing

	private Draft draft(LocalPlayer player) {
		if (draft == null || draftFor != slot) {
			Techniques.Written w = Techniques.book(player).slot(slot);
			if (!w.empty()) {
				draft = new Draft(w.name(), w.stroke(), w.release(), w.intent(), w.sword().map(SwordString::tokens).orElse(List.of()));
			} else {
				String stroke = firstKnown(player, TechniqueRules.Family.STROKE, TechniqueRules.DRAW);
				String release = firstKnown(player, TechniqueRules.Family.RELEASE, TechniqueRules.ON_BLADE);
				String intent = firstKnown(player, TechniqueRules.Family.INTENT, TechniqueRules.INFUSE);
				draft = new Draft("", stroke, release, intent, List.of());
			}
			draftFor = slot;
		}
		return draft;
	}

	private static String firstKnown(LocalPlayer player, TechniqueRules.Family family, String fallback) {
		if (Techniques.knows(player, fallback)) {
			return fallback;
		}
		for (String part : TechniqueRules.parts(family)) {
			if (Techniques.knows(player, part)) {
				return part;
			}
		}
		return fallback;
	}

	/** The whole page below the tabs; returns the tooltip, if any. */
	List<Component> draw(GuiGraphicsExtractor g, LocalPlayer player, int mx, int my, float partial, int color) {
		targets.clear();
		List<Component> tip = null;
		long now = player.level().getGameTime();
		Draft d = draft(player);
		List<Component> t;
		t = slots(g, player, mx, my, color, now);
		tip = t != null ? t : tip;
		// The scroll: the inset the Cord's readout sits in, a gold rule along its top.
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_INSET, 10, SCROLL_Y, W - 20, SCROLL_BOTTOM - SCROLL_Y);
		g.fill(13, SCROLL_Y + 1, W - 13, SCROLL_Y + 2, 0x66000000 | (GOLD & 0xFFFFFF));
		t = name(g, player, d, mx, my, color, now);
		tip = t != null ? t : tip;
		t = seals(g, player, d, mx, my, color, now, partial);
		tip = t != null ? t : tip;
		t = picker(g, player, d, mx, my, color);
		tip = t != null ? t : tip;
		TechniqueRules.Profile p = profile(player, d);
		t = readout(g, player, d, p, mx, my, color);
		tip = t != null ? t : tip;
		kata(g, p, color, now, partial);
		t = string(g, player, d, mx, my, color);
		tip = t != null ? t : tip;
		t = statusAndButtons(g, player, d, mx, my, color, now);
		tip = t != null ? t : tip;
		t = rank(g, player, mx, my, color);
		tip = t != null ? t : tip;
		return tip;
	}

	/** The draft as the swordsman would perform it (their method; its record's rank, temper and edge if it has been written before). */
	private static TechniqueRules.Profile profile(LocalPlayer player, Draft d) {
		try {
			Techniques.Honing h = Techniques.book(player).honing(d.key());
			return TechniqueRules.profile(d.stroke(), d.release(), d.intent(), Techniques.flavour(player), h.rank(), h.temper(), h.edge());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void target(String key, int x, int y, int w, int h) {
		targets.put(key, new int[] {x, y, w, h});
	}

	private void small(GuiGraphicsExtractor g, Component text, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	private void small(GuiGraphicsExtractor g, FormattedCharSequence text, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	private void smallCentered(GuiGraphicsExtractor g, Component text, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.centeredText(font, text, 0, 0, color);
		g.pose().popMatrix();
	}

	private static int argb(int rgb, double alpha) {
		int a = (int) Math.max(0, Math.min(255, Math.round(alpha * 255)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private static Component stage(int stage) {
		return Component.translatable("aura.wildercord.stage." + AuraStages.id(stage));
	}

	/** A part's glyph sprite (an add-on's intent without one of its own gets the plain one). */
	static Identifier glyph(String partId) {
		return TechniqueRules.allParts().contains(partId) && !partId.contains(":") ? Wildercord.id("technique/" + partId) : Wildercord.id("technique/unknown");
	}

	// ---------------------------------------------------------------- the slots

	private List<Component> slots(GuiGraphicsExtractor g, LocalPlayer player, int mx, int my, int color, long now) {
		List<Component> tip = null;
		Techniques.Book book = Techniques.book(player);
		int open = Techniques.slots(player);
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			int x = 14 + i * (CARD_W + 5);
			int y = SLOTS_Y;
			boolean unlocked = i < open;
			boolean selected = i == slot;
			Techniques.Written w = book.slot(i);
			boolean hover = inside(mx, my, x, y, CARD_W, CARD_H);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, !unlocked ? SPR_ROW_LOCKED : selected ? SPR_ROW_SELECTED : SPR_ROW, x, y, CARD_W, CARD_H);
			if (hover && !selected) {
				g.fill(x + 2, y + 2, x + CARD_W - 2, y + CARD_H - 2, 0x14FFFFFF);
			}
			target("slot:" + i, x, y, CARD_W, CARD_H);
			// Its number, in roman, in the corner.
			small(g, Component.literal(roman(i + 1)), x + 4, y + 3, selected ? GOLD : FAINT, 0.7F);
			if (w.empty()) {
				Component line = !unlocked ? Component.translatable("screen.wildercord.aura.writing.opens", stage(TechniqueRules.slotStage(i)))
					: Component.translatable("screen.wildercord.aura.writing.empty");
				smallCentered(g, line, x + CARD_W / 2, y + 13, unlocked ? DIM : FAINT, 0.8F);
				if (hover) {
					tip = List.of(line.copy().withColor(unlocked ? TEXT : DIM),
						Component.translatable(unlocked ? "screen.wildercord.aura.writing.empty_tip" : "screen.wildercord.aura.writing.locked_tip",
							stage(TechniqueRules.slotStage(i))).withStyle(ChatFormatting.GRAY));
				}
				continue;
			}
			boolean usable = Techniques.usable(player, i);
			int nameColor = usable ? 0xFF000000 | color : DIM;
			String name = w.shownName();
			float scale = font.width(name) > CARD_W - 18 ? 0.8F : 1.0F;
			String fitted = font.plainSubstrByWidth(name, Math.round((CARD_W - 16) / scale));
			small(g, Component.literal(fitted), x + 12, y + 4, nameColor, scale);
			// Its rank as pips, and its string in marks.
			Techniques.Honing h = book.honing(w.key());
			int rank = h.rank();
			for (int r = 0; r < TechniqueRules.MAX_RANK; r++) {
				int px = x + 6 + r * 5;
				int py = y + 17;
				boolean lit = r < rank;
				g.fill(px + 1, py, px + 2, py + 3, lit ? GOLD : 0xFF3A3450);
				g.fill(px, py + 1, px + 3, py + 2, lit ? GOLD : 0xFF3A3450);
			}
			w.sword().ifPresent(s -> {
				int sw = s.length() * 9 - 2;
				StringHud.string(g, s, x + CARD_W - 6 - sw, y + 15, usable ? AuraHud.mix(color, 0xFFFFFF, 0.4) : 0x6A6480, usable ? 1.0F : 0.6F);
			});
			// How far to the next rank, a thread along its foot.
			int barW = CARD_W - 12;
			g.fill(x + 6, y + CARD_H - 6, x + 6 + barW, y + CARD_H - 5, 0xFF2A2438);
			g.fill(x + 6, y + CARD_H - 6, x + 6 + (int) Math.round(barW * TechniqueRules.progress(h.xp())), y + CARD_H - 5,
				rank >= TechniqueRules.PEERLESS ? GOLD : 0xFF000000 | AuraHud.mix(color, 0x2A2438, 0.2));
			if (hover) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.literal(name).withColor(0xFF000000 | color));
				lines.add(Component.translatable("screen.wildercord.aura.writing.card_parts", Component.translatable(TechniqueRules.nameKey(w.stroke())),
					Component.translatable(TechniqueRules.nameKey(w.release())), Component.translatable(TechniqueRules.nameKey(w.intent())))
					.withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("screen.wildercord.aura.writing.card_rank", Component.translatable(Techniques.rankKey(rank)),
					(int) h.xp(), rank >= TechniqueRules.MAX_RANK ? "-" : Integer.toString(TechniqueRules.threshold(rank + 1))).withColor(GOLD));
				if (!usable) {
					lines.add(Component.translatable(unusableKey(player, i, w), unusableArg(player, i, w)).withColor(RED));
				}
				tip = lines;
			}
		}
		return tip;
	}

	private static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			default -> "V";
		};
	}

	/** Why a written technique can't be played now, as a language key. */
	private static String unusableKey(LocalPlayer player, int i, Techniques.Written w) {
		if (i >= Techniques.slots(player)) {
			return "screen.wildercord.aura.writing.slot_closed";
		}
		if (!Techniques.whole(w)) {
			return "screen.wildercord.aura.writing.broken";
		}
		return "screen.wildercord.aura.writing.part_missing";
	}

	private static Object unusableArg(LocalPlayer player, int i, Techniques.Written w) {
		if (i >= Techniques.slots(player)) {
			return stage(TechniqueRules.slotStage(i));
		}
		for (String part : w.parts()) {
			if (!Techniques.knows(player, part)) {
				return Component.translatable(TechniqueRules.nameKey(part));
			}
		}
		return "";
	}

	// ---------------------------------------------------------------- the name

	private List<Component> name(GuiGraphicsExtractor g, LocalPlayer player, Draft d, int mx, int my, int color, long now) {
		int x = 20;
		int y = SCROLL_Y + 7;
		int w = W - 40;
		boolean hover = inside(mx, my, x - 2, y - 2, w, 13);
		target("name", x - 2, y - 2, w, 13);
		String suggestion = TechniqueRules.autoName(d.stroke(), d.release(), d.intent(), Techniques.flavour(player));
		String typed = d.name();
		// The ink line it's written on.
		g.fill(x, y + 10, x + w - 46, y + 11, typing ? 0xAA000000 | (GOLD & 0xFFFFFF) : hover ? 0x88B8A8D8 : 0x553A3450);
		if (typed.isEmpty() && !typing) {
			g.text(font, Component.literal(suggestion).withStyle(ChatFormatting.ITALIC), x + 2, y, FAINT, false);
		} else {
			String shown = typed;
			// Each letter written in comes up out of the ink.
			g.text(font, shown, x + 2, y, 0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.25), true);
			if (typing && (now / 6) % 2 == 0) {
				int cx = x + 2 + font.width(shown);
				g.fill(cx + 1, y - 1, cx + 2, y + 9, GOLD);
			}
		}
		String count = TechniqueRules.cleanName(typed).length() + "/" + TechniqueRules.MAX_NAME;
		small(g, Component.literal(count), x + w - 40, y + 2, typing ? GOLD : FAINT, 0.75F);
		if (hover && !typing) {
			return List.of(Component.translatable("screen.wildercord.aura.writing.name_tip").withColor(TEXT),
				Component.translatable("screen.wildercord.aura.writing.name_suggest", suggestion).withStyle(ChatFormatting.GRAY));
		}
		return null;
	}

	// ---------------------------------------------------------------- the seals, threaded

	private static int sealX(int i) {
		return 46 + i * 100;
	}

	private List<Component> seals(GuiGraphicsExtractor g, LocalPlayer player, Draft d, int mx, int my, int color, long now, float partial) {
		List<Component> tip = null;
		TechniqueRules.Family[] families = TechniqueRules.Family.values();
		int cy = SEALS_Y + SEAL / 2;
		// The cord the three are threaded on, as the Cord threads its runes.
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_THREAD, sealX(0) + SEAL / 2, cy - 2, sealX(2) - sealX(0), 4);
		double ink = Math.max(0, 1 - (now + partial - inkAt) / 24.0);
		for (int i = 0; i < 3; i++) {
			TechniqueRules.Family family = families[i];
			String part = d.of(family);
			int x = sealX(i);
			int y = SEALS_Y;
			boolean isOpen = open == family;
			boolean hover = inside(mx, my, x - 2, y - 2, SEAL + 4, SEAL + 4);
			int fam = Techniques.familyColor(family);
			// Its kind over it.
			smallCentered(g, Component.translatable("screen.wildercord.aura.writing." + family.id), x + SEAL / 2, y - 9, 0xFF000000 | fam, 0.75F);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, isOpen ? SPR_SEAL_OPEN : SPR_SEAL, x, y, SEAL, SEAL);
			if (hover) {
				g.fill(x + 4, y + 4, x + SEAL - 4, y + SEAL - 4, 0x18FFFFFF);
			}
			boolean known = Techniques.knows(player, part);
			int tint = known ? 0xFF000000 | AuraHud.mix(fam, 0xFFFFFF, 0.35) : 0xFF4A4458;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, glyph(part), x + 5, y + 5, 16, 16, tint);
			if (ink > 0) {
				// Just written: the aura's ink running through the seals, one after another.
				double k = Math.max(0, Math.min(1, ink * 3 - i * 0.6));
				g.fill(x + 3, y + 3, x + SEAL - 3, y + SEAL - 3, argb(color, 0.45 * k));
			}
			Component name = Component.translatable(TechniqueRules.nameKey(part));
			String fitted = font.plainSubstrByWidth(name.getString(), Math.round(92 / 0.85F));
			smallCentered(g, Component.literal(fitted), x + SEAL / 2, y + SEAL + 3, known ? TEXT : RED, 0.85F);
			target("seal:" + family.id, x - 2, y - 2, SEAL + 4, SEAL + 4);
			if (hover) {
				tip = partTip(player, part, family);
			}
		}
		return tip;
	}

	/** A part's tooltip: its name and kind, what it does, and where it stands for the swordsman. */
	private List<Component> partTip(LocalPlayer player, String part, TechniqueRules.Family family) {
		List<Component> lines = new ArrayList<>();
		int fam = Techniques.familyColor(family);
		lines.add(Component.translatable(TechniqueRules.nameKey(part)).withColor(0xFF000000 | fam));
		lines.add(Component.translatable("screen.wildercord.aura.writing." + family.id).withStyle(ChatFormatting.DARK_GRAY));
		for (FormattedCharSequence line : font.split(Component.translatable(TechniqueRules.nameKey(part) + ".desc"), 220)) {
			lines.add(Component.literal(toString(line)).withStyle(ChatFormatting.GRAY));
		}
		lines.add(where(player, part));
		return lines;
	}

	/** A Way's colour by its id (grey for one that isn't registered). */
	private static int wayColor(String way) {
		return AuraApi.way(way).map(AuraApi.Way::color).orElse(0x8A84A0);
	}

	private static String toString(FormattedCharSequence seq) {
		StringBuilder out = new StringBuilder();
		seq.accept((index, style, cp) -> {
			out.appendCodePoint(cp);
			return true;
		});
		return out.toString();
	}

	/** Where a part stands for {@code player}: learned, innate, lent by a Way, or where to look for it. */
	private static Component where(LocalPlayer player, String part) {
		if (Techniques.learned(player, part)) {
			return Component.translatable("screen.wildercord.aura.writing.where.learned").withColor(GREEN);
		}
		if (TechniqueRules.INNATE.contains(part)) {
			return Aura.stage(player) >= TechniqueRules.FROM ? Component.translatable("screen.wildercord.aura.writing.where.innate").withColor(GREEN)
				: Component.translatable("screen.wildercord.aura.writing.where.innate_later", stage(TechniqueRules.FROM)).withColor(DIM);
		}
		String way = TechniqueRules.lendingWay(part);
		if (!way.isEmpty()) {
			Component wayName = Component.translatable("aura.wildercord.way." + way);
			if (Techniques.lent(player, part)) {
				return Component.translatable("screen.wildercord.aura.writing.where.lent", wayName).withColor(0xFF000000 | wayColor(way));
			}
			if (TechniqueRules.WAY_ONLY.contains(part)) {
				return Component.translatable("screen.wildercord.aura.writing.where.way_only", wayName).withColor(DIM);
			}
			return Component.translatable("screen.wildercord.aura.writing.where.scroll_or_way", wayName).withColor(DIM);
		}
		return Component.translatable(TechniqueRules.scrollable(part) ? "screen.wildercord.aura.writing.where.scroll"
			: "screen.wildercord.aura.writing.where.own").withColor(DIM);
	}

	// ---------------------------------------------------------------- the open seal's parts

	private List<Component> picker(GuiGraphicsExtractor g, LocalPlayer player, Draft d, int mx, int my, int color) {
		List<Component> tip = null;
		List<String> parts = TechniqueRules.parts(open);
		int cell = 18;
		int gap = 4;
		int total = parts.size() * cell + (parts.size() - 1) * gap;
		int x0 = W / 2 - total / 2;
		int y = PICKER_Y;
		String chosen = d.of(open);
		int fam = Techniques.familyColor(open);
		for (int i = 0; i < parts.size(); i++) {
			String part = parts.get(i);
			int x = x0 + i * (cell + gap);
			boolean known = Techniques.knows(player, part);
			boolean hover = inside(mx, my, x, y, cell, cell);
			boolean selected = part.equals(chosen);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, hover || selected ? SPR_SOCKET_HOVER : SPR_SOCKET, x, y, cell, cell);
			int tint = known ? 0xFF000000 | AuraHud.mix(fam, 0xFFFFFF, selected ? 0.5 : 0.2) : 0x664A4458;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, glyph(part), x + 1, y + 1, 16, 16, tint);
			if (!known) {
				// Not known yet: dark, a lock on it.
				g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_LOCK, x + cell - 7, y + cell - 8, 7, 8);
			} else if (Techniques.lent(player, part) && !Techniques.learned(player, part)) {
				// Lent by a Way: a dot of its colour in the corner.
				int wc = 0xFF000000 | wayColor(TechniqueRules.lendingWay(part));
				g.fill(x + cell - 5, y + 2, x + cell - 2, y + 5, wc);
			}
			if (selected) {
				g.fill(x + 3, y + cell + 1, x + cell - 3, y + cell + 2, GOLD);
			}
			target("part:" + part, x, y, cell, cell);
			if (hover) {
				tip = partTip(player, part, open);
			}
		}
		return tip;
	}

	// ---------------------------------------------------------------- what it does

	private static String trim(double v) {
		return v == Math.rint(v) ? Long.toString((long) v) : String.format(Locale.ROOT, "%.1f", v);
	}

	private static String trim2(double v) {
		return String.format(Locale.ROOT, "%.2f", v);
	}

	private List<Component> readout(GuiGraphicsExtractor g, LocalPlayer player, Draft d, TechniqueRules.Profile p, int mx, int my, int color) {
		int x = 20;
		int y = READOUT_Y;
		int width = KATA_X - x - 8;
		float s = 0.75F;
		int pitch = 8;
		if (p == null) {
			small(g, Component.translatable("screen.wildercord.aura.writing.broken"), x, y, RED, s);
			return null;
		}
		double priced = TechniqueRules.pricedWorth(d.stroke(), d.release(), d.intent(), Techniques.flavour(player), p.temper(), p.edge());
		Optional<SwordString> string = d.string();
		double cost = string.map(st -> TechniqueRules.cost(priced, st)).orElse(Math.round(priced * TechniqueRules.AURA_PER_W * 10) / 10.0);
		int rest = string.map(st -> TechniqueRules.rest(priced, st)).orElse((int) Math.round(priced * TechniqueRules.TICKS_PER_W));
		Component price = Component.translatable("screen.wildercord.aura.writing.price", trim(cost), trim(rest / 20.0));
		g.text(font, price, x, y, LAVENDER, true);
		y += 11;
		double blade = blade(player);
		List<Component> lines = new ArrayList<>();
		lines.add(switch (p.shape()) {
			case LINE -> Component.translatable("screen.wildercord.aura.writing.shape.line", trim(p.reach()), p.targets());
			case CONE -> Component.translatable("screen.wildercord.aura.writing.shape.cone", trim(p.reach()), (int) Math.round(p.width()), p.targets());
			case RING -> Component.translatable("screen.wildercord.aura.writing.shape.ring", trim(p.reach()), p.targets());
		});
		lines.add(Component.translatable("screen.wildercord.aura.writing.blow", trim2(p.factor()), trim(p.factor() * blade)));
		switch (p.release().id()) {
			case TechniqueRules.WAVE -> lines.add(Component.translatable("screen.wildercord.aura.writing.release.wave", trim(p.flight())));
			case TechniqueRules.BURST -> lines.add(Component.translatable("screen.wildercord.aura.writing.release.burst"));
			case TechniqueRules.AFTERIMAGE -> lines.add(Component.translatable("screen.wildercord.aura.writing.release.afterimage", trim(p.laterDelay() / 20.0),
				trim2(p.later())));
			default -> {
			}
		}
		lines.add(intentLine(p));
		lines.add(elementLine(player, p));
		List<Component> tip = null;
		boolean cut = false;
		for (Component line : lines) {
			if (line == null) {
				continue;
			}
			for (FormattedCharSequence part : font.split(line, Math.round(width / s))) {
				if (y > READOUT_Y + 44) {
					cut = true;
					break;
				}
				small(g, part, x, y, TEXT, s);
				y += pitch;
			}
		}
		if (cut) {
			// More than fits: a mark at its foot, and every line in full on hover.
			small(g, Component.literal("..."), x + width - 10, READOUT_Y + 49, DIM, s);
			if (inside(mx, my, x, READOUT_Y + 10, width, 44)) {
				List<Component> all = new ArrayList<>();
				for (Component line : lines) {
					if (line != null) {
						all.add(line.copy().withColor(TEXT));
					}
				}
				tip = all;
			}
		}
		// Where it sits against the arts: a scale of the four, its worth a mark on it, the nearest named beside it.
		double worth = TechniqueRules.worth(p);
		int near = TechniqueRules.momentumSlot(worth);
		Component like = Component.translatable("screen.wildercord.aura.writing.worth", Component.translatable(dev.wildercord.aura.AuraFxRules.ordinalKey(near + 1)));
		int gy = READOUT_Y + 57;
		small(g, Component.translatable("screen.wildercord.aura.writing.worth_label"), x, gy, DIM, 0.7F);
		int gx = x + (int) (font.width(Component.translatable("screen.wildercord.aura.writing.worth_label")) * 0.7F) + 4;
		int likeW = (int) (font.width(like) * 0.7F);
		int gw = Math.max(30, width - (gx - x) - likeW - 8);
		small(g, like, gx + gw + 6, gy, 0xFFC8C0E0, 0.7F);
		gy -= 1;
		g.fill(gx, gy + 2, gx + gw, gy + 3, 0xFF3A3450);
		double lo = 0.8;
		double hi = 2.7;
		for (int a = 0; a < 4; a++) {
			int ax = gx + (int) Math.round((dev.wildercord.aura.ArtRules.SLOT_POWER[a] - lo) / (hi - lo) * gw);
			g.fill(ax, gy, ax + 1, gy + 5, DIM);
		}
		int wx = gx + (int) Math.round(Math.max(0, Math.min(1, (worth - lo) / (hi - lo))) * gw);
		g.fill(wx - 1, gy - 1, wx + 2, gy + 6, 0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.3));
		if (inside(mx, my, x - 2, gy - 3, width + 4, 10)) {
			tip = List.of(like.copy().withColor(TEXT), Component.translatable("screen.wildercord.aura.writing.worth_tip", trim2(worth)).withStyle(ChatFormatting.GRAY));
		}
		return tip;
	}

	/** What the blade in hand strikes for (its own modifiers on the player's base): the client's attribute doesn't carry the hand's. */
	private static double blade(LocalPlayer player) {
		double base = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
		double held = player.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
			net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE, base, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
		return Math.max(1.0, Math.max(held, player.getAttributeValue(Attributes.ATTACK_DAMAGE)));
	}

	private static Component intentLine(TechniqueRules.Profile p) {
		return switch (p.intent().id()) {
			case TechniqueRules.PIERCE -> Component.translatable("screen.wildercord.aura.writing.intent.pierce", p.targets(), trim(p.total()));
			case TechniqueRules.SUNDER -> Component.translatable("screen.wildercord.aura.writing.intent.sunder", trim(p.stance()));
			case TechniqueRules.BIND -> Component.translatable("screen.wildercord.aura.writing.intent.bind", trim(p.bind() / 20.0));
			case TechniqueRules.ECHO -> Component.translatable("screen.wildercord.aura.writing.intent.echo", trim2(p.factor() * p.echo()));
			case TechniqueRules.WARD -> Component.translatable("screen.wildercord.aura.writing.intent.ward", Math.round(p.ward() * 100), trim(p.wardTicks() / 20.0));
			case TechniqueRules.RALLY -> Component.translatable("screen.wildercord.aura.writing.intent.rally", trim(p.rallyRadius()), Math.round(p.rally() * 100),
				trim(p.rallyTicks() / 20.0), trim(p.rallyMomentum()));
			case TechniqueRules.INFUSE -> Component.translatable("screen.wildercord.aura.writing.intent.infuse", trim(p.flavourScale()));
			default -> Component.translatable("screen.wildercord.aura.writing.intent.own", Component.translatable(TechniqueRules.nameKey(p.intent().id())));
		};
	}

	private static Component elementLine(LocalPlayer player, TechniqueRules.Profile p) {
		TechniqueRules.Flavour fl = p.flavour();
		double k = p.flavourScale() * p.temperScale();
		Component method = Aura.method(player).<Component>map(m -> Component.translatable(m.nameKey())).orElse(Component.empty());
		Component what = switch (fl.of()) {
			case IGNITE -> Component.translatable("screen.wildercord.aura.writing.element.ignite", trim(p.ignite() / 20.0));
			case CHILL -> Component.translatable("screen.wildercord.aura.writing.element.chill", trim(p.chill() / 20.0));
			case SPARK -> Component.translatable("screen.wildercord.aura.writing.element.spark", trim2(p.factor() * fl.spark() * k));
			case GALE -> Component.translatable("screen.wildercord.aura.writing.element.gale", trim(fl.reach() * p.flavourScale()));
			case STONE -> Component.translatable("screen.wildercord.aura.writing.element.stone", trim(p.stagger() / 20.0));
			case MEND -> Component.translatable("screen.wildercord.aura.writing.element.mend", trim(fl.mend() * k), trim(fl.mendCap() * k));
			case PULL -> Component.translatable("screen.wildercord.aura.writing.element.pull");
			case STARLIT -> Component.translatable("screen.wildercord.aura.writing.element.starlit", trim(fl.aura() * k), trim(fl.auraCap() * k));
			case HASTE -> Component.translatable("screen.wildercord.aura.writing.element.haste", trim2(p.factor() * fl.echo() * k));
			case LEECH -> Component.translatable("screen.wildercord.aura.writing.element.leech", Math.round(fl.drink() * k * 100));
			case NONE -> Component.translatable("screen.wildercord.aura.writing.element.none");
			// ---- methods-a pack
			case CURRENT -> Component.translatable("screen.wildercord.aura.writing.element.current", trim(fl.current() * k));
			case FORGE -> Component.translatable("screen.wildercord.aura.writing.element.forge", trim(fl.sunder() * k));
			case GRIT -> Component.translatable("screen.wildercord.aura.writing.element.grit", trim(fl.grit() * k / 20.0));
		};
		return Component.translatable("screen.wildercord.aura.writing.element", method, what);
	}

	// ---------------------------------------------------------------- the kata: the shape it strikes, seen from above, playing

	/** A few foes standing about the swordsman (blocks: across, ahead), lit when the technique would reach them. */
	private static final double[][] FOES = {{0.3, 2.4}, {-1.6, 3.1}, {1.9, 2.0}, {0.6, 5.6}, {-0.8, 8.8}, {-2.7, 1.1}, {2.3, -1.6}, {-0.4, -2.2}};

	private void kata(GuiGraphicsExtractor g, TechniqueRules.Profile p, int color, long now, float partial) {
		int x = KATA_X;
		int y = READOUT_Y - 2;
		g.fill(x - 1, y - 1, x + KATA_W + 1, y + KATA_H + 1, 0xFF2A2438);
		g.fill(x, y, x + KATA_W, y + KATA_H, INK);
		if (p == null) {
			return;
		}
		boolean round = p.shape() == TechniqueRules.Shape.RING;
		double total = p.total();
		// The swordsman at the foot facing up the box, or in its middle for a shape all round.
		double scale = round ? Math.min((KATA_H / 2.0 - 3) / Math.max(1, total), (KATA_W / 2.0 - 3) / Math.max(1, total))
			: Math.min((KATA_H - 8) / Math.max(1, total + 0.5), (KATA_W / 2.0 - 3) / Math.max(1.5, halfWidth(p, total)));
		scale = Math.min(scale, 9);
		double ox = x + KATA_W / 2.0;
		double oy = round ? y + KATA_H / 2.0 : y + KATA_H - 5;
		// A faint grid, a block apart.
		for (int bz = -12; bz <= 14; bz++) {
			int py = (int) Math.round(oy - bz * scale);
			if (py <= y || py >= y + KATA_H) {
				continue;
			}
			for (int bx = -9; bx <= 9; bx++) {
				int px = (int) Math.round(ox + bx * scale);
				if (px > x && px < x + KATA_W) {
					g.fill(px, py, px + 1, py + 1, 0x22FFFFFF);
				}
			}
		}
		float t = ((now % 48) + partial);
		boolean afterimage = p.release().id().equals(TechniqueRules.AFTERIMAGE);
		boolean wave = p.release().id().equals(TechniqueRules.WAVE);
		boolean burst = p.release().id().equals(TechniqueRules.BURST);
		double flash = Math.max(0, 1 - t / 10.0);
		double reachNow = p.reach();
		if (burst) {
			reachNow = p.reach() * Math.min(1, t / 6.0);
		}
		// The shape it strikes, filled, brightest as it's struck.
		double base = 0.26 + 0.4 * flash;
		fillShape(g, p, x, y, ox, oy, scale, reachNow, 0, argb(color, base));
		if (wave) {
			// The wave racing on past the stroke's reach.
			double front = p.reach() + Math.min(p.flight(), Math.max(0, (t - 3) * TechniqueRules.WAVE_SPEED));
			if (t > 3 && t < 3 + p.flight() / TechniqueRules.WAVE_SPEED + 4) {
				fillShape(g, p, x, y, ox, oy, scale, front, Math.max(p.reach(), front - 1.6), argb(AuraHud.mix(color, 0xFFFFFF, 0.4), 0.65));
			}
			fillShape(g, p, x, y, ox, oy, scale, total, p.reach(), argb(color, 0.08));
		}
		if (afterimage && t >= p.laterDelay() && t < p.laterDelay() + 10) {
			double k = 1 - (t - p.laterDelay()) / 10.0;
			fillShape(g, p, x, y, ox, oy, scale, p.reach(), 0, argb(AuraHud.mix(color, 0xFFFFFF, 0.5), 0.5 * k));
		}
		if (p.echo() > 0 && t >= TechniqueRules.ECHO_DELAY && t < TechniqueRules.ECHO_DELAY + 8) {
			double k = 1 - (t - TechniqueRules.ECHO_DELAY) / 8.0;
			fillShape(g, p, x, y, ox, oy, scale, p.reach(), 0, argb(0xFFFFFF, 0.25 * k));
		}
		// The foes, lit where it reaches them.
		int reached = 0;
		for (double[] f : FOES) {
			int fx = (int) Math.round(ox + f[0] * scale);
			int fy = (int) Math.round(oy - f[1] * scale);
			if (fx < x + 2 || fx > x + KATA_W - 3 || fy < y + 2 || fy > y + KATA_H - 3) {
				continue;
			}
			boolean in = within(p, f[0], f[1], total) && reached < p.targets();
			if (in) {
				reached++;
			}
			int c = in ? 0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.55) : 0xFF6A6480;
			g.fill(fx - 1, fy - 1, fx + 2, fy + 2, c);
			if (in) {
				switch (p.intent().id()) {
					case TechniqueRules.BIND -> {
						g.fill(fx - 3, fy - 3, fx - 2, fy + 4, argb(color, 0.8));
						g.fill(fx + 3, fy - 3, fx + 4, fy + 4, argb(color, 0.8));
					}
					case TechniqueRules.SUNDER -> {
						g.fill(fx - 3, fy - 3, fx - 2, fy - 2, 0xFFFFFFFF);
						g.fill(fx + 3, fy + 3, fx + 4, fy + 4, 0xFFFFFFFF);
						g.fill(fx + 3, fy - 3, fx + 4, fy - 2, 0xFFFFFFFF);
						g.fill(fx - 3, fy + 3, fx - 2, fy + 4, 0xFFFFFFFF);
					}
					case TechniqueRules.PIERCE -> g.fill(fx, fy - 4, fx + 1, fy - 2, 0xFFFFFFFF);
					default -> {
					}
				}
			}
		}
		// The swordsman: a small arrowhead facing the way they strike (and their afterimage, where it stays).
		int sx = (int) Math.round(ox);
		int sy = (int) Math.round(oy);
		if (afterimage) {
			int back = (int) Math.min(6, t * 0.6);
			arrow(g, sx, sy + back, 0x88FFFFFF);
			arrow(g, sx, sy, argb(color, t < p.laterDelay() + 10 ? 0.7 : 0.25));
		} else {
			arrow(g, sx, sy, 0xFFFFFFFF);
		}
		if (p.ward() > 0) {
			int r = 4 + (int) Math.round(Math.sin(t * 0.3) * 0.8);
			ring(g, sx, sy, r, argb(WayRules.BULWARK_COLOR, 0.8));
		}
		if (p.rally() > 0) {
			int r = (int) Math.round(p.rallyRadius() * scale);
			ring(g, sx, sy, Math.min(r, KATA_W / 2 - 2), argb(WayRules.BANNER_COLOR, 0.35));
		}
	}

	/** How wide the technique's shape is at its widest (blocks), for the kata's scale. */
	private static double halfWidth(TechniqueRules.Profile p, double total) {
		return switch (p.shape()) {
			case LINE -> p.width() + 0.5;
			case CONE -> p.width() >= 180 ? p.reach() : p.reach() * Math.sin(Math.toRadians(p.width() / 2)) + (p.flight() > 0 ? 0.5 : 0);
			case RING -> total;
		};
	}

	/** Whether a point (blocks across, ahead) lies in the technique's shape out to {@code reach} (a wave's flight included). */
	private static boolean within(TechniqueRules.Profile p, double bx, double bz, double reach) {
		double d = Math.hypot(bx, bz);
		return switch (p.shape()) {
			case LINE -> bz >= -0.3 && bz <= reach && Math.abs(bx) <= p.width() + 0.3;
			case CONE -> {
				if (bz > p.reach() && p.flight() > 0) {
					double half = p.reach() * Math.sin(Math.toRadians(Math.min(180, p.width()) / 2));
					yield bz <= reach && Math.abs(bx) <= half;
				}
				double a = Math.toDegrees(Math.abs(Math.atan2(bx, bz)));
				yield d <= p.reach() && a <= p.width() / 2;
			}
			case RING -> d <= reach;
		};
	}

	/** Fills the technique's shape between {@code from} and {@code to} blocks out, row by row. */
	private void fillShape(GuiGraphicsExtractor g, TechniqueRules.Profile p, int x, int y, double ox, double oy, double scale, double to, double from, int argb) {
		for (int py = y + 1; py < y + KATA_H - 1; py++) {
			int run = -1;
			for (int px = x + 1; px <= x + KATA_W - 1; px++) {
				boolean in = false;
				if (px < x + KATA_W - 1) {
					double bx = (px + 0.5 - ox) / scale;
					double bz = (oy - py - 0.5) / scale;
					double d = p.shape() == TechniqueRules.Shape.LINE || p.shape() == TechniqueRules.Shape.CONE && bz > p.reach() ? bz : Math.hypot(bx, bz);
					in = d >= from && within(p, bx, bz, to);
				}
				if (in && run < 0) {
					run = px;
				} else if (!in && run >= 0) {
					g.fill(run, py, px, py + 1, argb);
					run = -1;
				}
			}
		}
	}

	private static void arrow(GuiGraphicsExtractor g, int x, int y, int c) {
		g.fill(x, y - 2, x + 1, y + 2, c);
		g.fill(x - 1, y - 1, x + 2, y, c);
		g.fill(x - 2, y + 1, x + 3, y + 2, c);
	}

	private static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int c) {
		for (int a = 0; a < 48; a++) {
			double t = Math.PI * 2 * a / 48;
			int px = cx + (int) Math.round(Math.cos(t) * r);
			int py = cy + (int) Math.round(Math.sin(t) * r);
			g.fill(px, py, px + 1, py + 1, c);
		}
	}

	// ---------------------------------------------------------------- the string

	private List<Component> string(GuiGraphicsExtractor g, LocalPlayer player, Draft d, int mx, int my, int color) {
		List<Component> tip = null;
		int x = 20;
		int y = STRING_Y;
		g.text(font, Component.translatable("screen.wildercord.aura.writing.string"), x, y, GOLD, true);
		int sx = x + font.width(Component.translatable("screen.wildercord.aura.writing.string")) + 8;
		int lit = 0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.45);
		// The swings written so far, twice the indicator's size, a faint place for the next.
		g.pose().pushMatrix();
		g.pose().translate(sx, y - 1);
		g.pose().scale(1.5F, 1.5F);
		for (int i = 0; i < TechniqueRules.MAX_STRING; i++) {
			int gx = i * 10;
			if (i < d.tokens().size()) {
				SwordString.Token token = d.tokens().get(i);
				StringHud.glyph(g, token, gx, 0, token == SwordString.Token.COUNTER ? dev.wildercord.aura.AuraGuard.PERFECT_COLOR : lit & 0xFFFFFF, 1.0F);
			} else {
				g.fill(gx + 1, 6, gx + 6, 7, i == d.tokens().size() ? 0xAA8A84A0 : 0x443A3450);
			}
		}
		g.pose().popMatrix();
		int back = sx + 5 * 15 + 4;
		boolean hoverBack = inside(mx, my, back - 2, y - 2, 14, 12);
		g.text(font, "←", back, y, d.tokens().isEmpty() ? FAINT : hoverBack ? GOLD : DIM, false);
		target("back", back - 2, y - 2, 14, 12);
		if (hoverBack) {
			tip = List.of(Component.translatable("screen.wildercord.aura.writing.back").withColor(TEXT));
		}
		// How much it asks of the hand: a demanding string costs a little less.
		d.string().ifPresent(s -> {
			double e = TechniqueRules.effort(s);
			String key = e < 0.995 ? "screen.wildercord.aura.writing.effort.hard" : e > 1.005 ? "screen.wildercord.aura.writing.effort.easy"
				: "screen.wildercord.aura.writing.effort.even";
			Component line = Component.translatable(key, Math.round(Math.abs(1 - e) * 100));
			int lx = W - 22 - (int) (font.width(line) * 0.75F);
			small(g, line, lx, y + 2, DIM, 0.75F);
		});
		// The swings to write it in: each one's mark and word, a click adding it.
		SwordString.Token[] tokens = SwordString.Token.values();
		int cw = 38;
		int x0 = W / 2 - tokens.length * cw / 2;
		for (int i = 0; i < tokens.length; i++) {
			SwordString.Token token = tokens[i];
			int bx = x0 + i * cw;
			int by = TOKENS_Y;
			boolean hover = inside(mx, my, bx, by, cw - 2, 22);
			boolean full = d.tokens().size() >= TechniqueRules.MAX_STRING;
			g.fill(bx, by, bx + cw - 2, by + 22, hover && !full ? 0xFF2E2840 : 0xFF1E1A2A);
			g.fill(bx, by, bx + cw - 2, by + 1, hover && !full ? GOLD : 0xFF3A3450);
			StringHud.glyph(g, token, bx + (cw - 2) / 2 - 3, by + 3, token == SwordString.Token.COUNTER ? dev.wildercord.aura.AuraGuard.PERFECT_COLOR : lit & 0xFFFFFF,
				full ? 0.4F : 1.0F);
			smallCentered(g, Component.literal(token.id), bx + (cw - 2) / 2, by + 13, full ? FAINT : hover ? TEXT : DIM, 0.7F);
			target("token:" + token.id, bx, by, cw - 2, 22);
			if (hover) {
				tip = List.of(Component.translatable("aura.wildercord.token." + token.id).withColor(lit),
					Component.translatable("screen.wildercord.aura.writing.token_tip").withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		return tip;
	}

	// ---------------------------------------------------------------- what stands in the way, and the buttons

	private List<Component> statusAndButtons(GuiGraphicsExtractor g, LocalPlayer player, Draft d, int mx, int my, int color, long now) {
		List<Component> tip = null;
		Status status = status(player, d);
		int x = 20;
		int y = STATUS_Y;
		int width = 178;
		List<FormattedCharSequence> lines = font.split(status.line(), Math.round(width / 0.8F));
		int ly = lines.size() > 1 ? y - 3 : y;
		for (int i = 0; i < Math.min(2, lines.size()); i++) {
			small(g, lines.get(i), x, ly + i * 8, status.color(), 0.8F);
		}
		// Write, and Erase.
		boolean canWrite = status.writable();
		int wx = 204;
		int ww = 46;
		boolean hoverWrite = inside(mx, my, wx, y - 3, ww, 14);
		button(g, wx, y - 3, ww, Component.translatable(status.same() ? "screen.wildercord.aura.writing.written" : "screen.wildercord.aura.writing.write"),
			canWrite, hoverWrite, canWrite ? GOLD : FAINT);
		target("write", wx, y - 3, ww, 14);
		Techniques.Written w = Techniques.book(player).slot(slot);
		int ex = wx + ww + 4;
		int ew = W - 18 - ex;
		boolean armed = now - eraseArmedAt < 60;
		boolean hoverErase = inside(mx, my, ex, y - 3, ew, 14);
		button(g, ex, y - 3, ew, Component.translatable(armed ? "screen.wildercord.aura.writing.erase_sure" : "screen.wildercord.aura.writing.erase"),
			!w.empty(), hoverErase, armed ? RED : !w.empty() ? DIM : FAINT);
		target("erase", ex, y - 3, ew, 14);
		if (hoverWrite && !canWrite && !status.same()) {
			tip = List.of(status.line().copy().withColor(status.color()));
		}
		return tip;
	}

	private void button(GuiGraphicsExtractor g, int x, int y, int w, Component label, boolean enabled, boolean hover, int textColor) {
		g.fill(x, y, x + w, y + 14, enabled && hover ? 0xFF3A3050 : 0xFF221E30);
		g.fill(x, y, x + w, y + 1, enabled ? 0xFF000000 | AuraHud.mix(0xE8C46A, 0x221E30, hover ? 0.0 : 0.4) : 0xFF3A3450);
		g.fill(x, y + 13, x + w, y + 14, 0xFF14101C);
		float s = font.width(label) > w - 6 ? 0.8F : 1.0F;
		smallCentered(g, label, x + w / 2, y + (s < 1 ? 4 : 3), textColor, s);
	}

	/** What the page says about the draft: a line, its colour, whether it can be written now, and whether it's what's written already. */
	private record Status(Component line, int color, boolean writable, boolean same) {}

	private Status status(LocalPlayer player, Draft d) {
		Techniques.Written w = Techniques.book(player).slot(slot);
		if (d.same(w)) {
			return new Status(Component.translatable("screen.wildercord.aura.writing.status.same"), DIM, false, true);
		}
		Optional<SwordString> string = d.string();
		if (string.isEmpty()) {
			Techniques.Refusal early = Techniques.refusal(player, slot, d.stroke(), d.release(), d.intent(), "swing swing low leap");
			if (early != null && !early.key().endsWith(".clash") && !early.key().contains(".string_")) {
				return new Status(Component.translatable(early.key(), early.args()), RED, false, false);
			}
			return new Status(Component.translatable("screen.wildercord.aura.writing.status.no_string"), AMBER, false, false);
		}
		Techniques.Refusal why = Techniques.refusal(player, slot, d.stroke(), d.release(), d.intent(), string.get().text());
		if (why != null) {
			return new Status(Component.translatable(why.key(), why.args()), RED, false, false);
		}
		for (Techniques.Overlap o : Techniques.overlaps(player, slot, string.get())) {
			if (o.first()) {
				return new Status(Component.translatable("screen.wildercord.aura.writing.status.goes_first", AuraApi.artName(player, o.art())), AMBER, true, false);
			}
		}
		for (Techniques.Overlap o : Techniques.overlaps(player, slot, string.get())) {
			return new Status(Component.translatable("screen.wildercord.aura.writing.status.goes_before", AuraApi.artName(player, o.art())), GREEN, true, false);
		}
		return new Status(Component.translatable("screen.wildercord.aura.writing.status.ready"), GREEN, true, false);
	}

	// ---------------------------------------------------------------- its rank, its temper and its edge

	private List<Component> rank(GuiGraphicsExtractor g, LocalPlayer player, int mx, int my, int color) {
		List<Component> tip = null;
		Techniques.Written w = Techniques.book(player).slot(slot);
		int x = 14;
		int y = RANK_Y;
		if (w.empty()) {
			List<FormattedCharSequence> lines = font.split(Component.translatable(Aura.stage(player) >= TechniqueRules.FROM
				? "screen.wildercord.aura.writing.foot" : "screen.wildercord.aura.writing.foot_before", stage(TechniqueRules.FROM)), Math.round((W - 28) / 0.8F));
			for (int i = 0; i < Math.min(3, lines.size()); i++) {
				small(g, lines.get(i), x + 2, y + 2 + i * 8, FAINT, 0.8F);
			}
			return null;
		}
		Techniques.Honing h = Techniques.book(player).honing(w.key());
		int rank = h.rank();
		Component rankName = Component.translatable(Techniques.rankKey(rank)).withColor(rank >= TechniqueRules.PEERLESS ? GOLD : 0xFF000000 | color);
		g.text(font, Component.translatable("screen.wildercord.aura.writing.rank", rankName), x + 2, y, TEXT, true);
		int bx = x + 2 + font.width(Component.translatable("screen.wildercord.aura.writing.rank", rankName)) + 6;
		int bw = 80;
		g.fill(bx, y + 3, bx + bw, y + 5, 0xFF2A2438);
		g.fill(bx, y + 3, bx + (int) Math.round(bw * TechniqueRules.progress(h.xp())), y + 5, rank >= TechniqueRules.PEERLESS ? GOLD : 0xFF000000 | color);
		String xp = rank >= TechniqueRules.MAX_RANK ? Integer.toString((int) h.xp()) : (int) h.xp() + " / " + TechniqueRules.threshold(rank + 1);
		small(g, Component.literal(xp), bx + bw + 4, y + 2, DIM, 0.75F);
		if (inside(mx, my, x, y - 1, bx + bw + 40 - x, 10)) {
			tip = List.of(rankName.copy(), Component.translatable("screen.wildercord.aura.writing.rank_tip").withStyle(ChatFormatting.GRAY));
		}
		int cy = y + 13;
		if (rank >= TechniqueRules.INSCRIBE_RANK) {
			// Peerless: its parts set down on scrolls.
			small(g, Component.translatable("screen.wildercord.aura.writing.inscribe"), x + 2, cy + 3, GOLD, 0.8F);
			int ix = x + 2 + (int) (font.width(Component.translatable("screen.wildercord.aura.writing.inscribe")) * 0.8F) + 6;
			for (String part : w.parts()) {
				boolean can = TechniqueRules.scrollable(part);
				boolean hover = inside(mx, my, ix, cy, 14, 14);
				g.fill(ix, cy, ix + 14, cy + 14, can && hover ? 0xFF3A3050 : 0xFF221E30);
				g.blitSprite(RenderPipelines.GUI_TEXTURED, glyph(part), ix + 1, cy + 1, 12, 12,
					can ? 0xFF000000 | AuraHud.mix(Techniques.familyColor(TechniqueRules.family(part).orElseThrow()), 0xFFFFFF, 0.3) : 0x663A3450);
				target("inscribe:" + part, ix, cy, 14, 14);
				if (hover) {
					tip = List.of(Component.translatable(TechniqueRules.nameKey(part)).withColor(TEXT),
						Component.translatable(can ? "screen.wildercord.aura.writing.inscribe_tip" : "screen.wildercord.aura.writing.inscribe_no").withStyle(ChatFormatting.GRAY));
				}
				ix += 17;
			}
			cy += 0;
			int tx = ix + 8;
			tip = chips(g, h, rank, tx, cy, mx, my, tip, true);
			return tip;
		}
		return chips(g, h, rank, x + 2, cy, mx, my, tip, false);
	}

	/** The temper's and the edge's choices, each lit once its rank is reached. */
	private List<Component> chips(GuiGraphicsExtractor g, Techniques.Honing h, int rank, int x, int y, int mx, int my, List<Component> tip, boolean compact) {
		int cx = x;
		for (int kind = 0; kind < 2; kind++) {
			boolean edge = kind == 1;
			int needs = edge ? TechniqueRules.EDGE_RANK : TechniqueRules.TEMPER_RANK;
			boolean open = rank >= needs;
			String chosen = edge ? h.edge() : h.temper();
			Component label = Component.translatable(edge ? "screen.wildercord.aura.writing.edge" : "screen.wildercord.aura.writing.temper");
			small(g, label, cx, y + 3, open ? GOLD : FAINT, 0.75F);
			cx += (int) (font.width(label) * 0.75F) + 4;
			List<String> choices = new ArrayList<>();
			choices.add("");
			choices.addAll(edge ? TechniqueRules.EDGES : TechniqueRules.TEMPERS);
			for (String choice : choices) {
				if (compact && choice.isEmpty()) {
					continue;
				}
				Component name = Component.translatable("screen.wildercord.aura.writing." + (edge ? "edge." : "temper.") + (choice.isEmpty() ? "none" : choice));
				int cw = (int) (font.width(name) * 0.75F) + 6;
				boolean selected = chosen.equals(choice);
				boolean hover = open && inside(mx, my, cx, y, cw, 12);
				g.fill(cx, y, cx + cw, y + 12, selected && open ? 0xFF3A3050 : hover ? 0xFF2E2840 : 0xFF1E1A2A);
				if (selected && open) {
					g.fill(cx, y + 11, cx + cw, y + 12, GOLD);
				}
				smallCentered(g, name, cx + cw / 2, y + 3, !open ? FAINT : selected ? TEXT : hover ? TEXT : DIM, 0.75F);
				target((edge ? "edge:" : "temper:") + (choice.isEmpty() ? "none" : choice), cx, y, cw, 12);
				if (inside(mx, my, cx, y, cw, 12)) {
					List<Component> lines = new ArrayList<>();
					lines.add(name.copy().withColor(TEXT));
					lines.add(Component.translatable("screen.wildercord.aura.writing." + (edge ? "edge." : "temper.") + (choice.isEmpty() ? "none" : choice) + ".tip")
						.withStyle(ChatFormatting.GRAY));
					if (!open) {
						lines.add(Component.translatable("screen.wildercord.aura.writing.opens_at", Component.translatable(Techniques.rankKey(needs))).withColor(DIM));
					} else if (!chosen.isEmpty() && !selected) {
						lines.add(Component.translatable("screen.wildercord.aura.writing.change_cost", TechniqueRules.CHANGE_LEVELS).withColor(AMBER));
					}
					tip = lines;
				}
				cx += cw + 2;
			}
			cx += 8;
		}
		return tip;
	}

	// ------------------------------------------------------------------ input

	private void click(float pitch) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
	}

	private void deny() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.4F, 0.4F));
	}

	/** A click at ({@code mx}, {@code my}) in the page's own coordinates; returns whether the page took it. */
	boolean mouseClicked(LocalPlayer player, int mx, int my, int button) {
		Draft d = draft(player);
		String hit = null;
		for (Map.Entry<String, int[]> e : targets.entrySet()) {
			int[] r = e.getValue();
			if (inside(mx, my, r[0], r[1], r[2], r[3])) {
				hit = e.getKey();
			}
		}
		if (hit == null) {
			typing = false;
			return false;
		}
		if (!hit.equals("name")) {
			typing = false;
		}
		long now = player.level().getGameTime();
		if (hit.startsWith("slot:")) {
			int i = Integer.parseInt(hit.substring(5));
			if (i != slot) {
				select(i);
				click(1.1F);
			}
			return true;
		}
		if (hit.equals("name")) {
			typing = true;
			click(1.3F);
			return true;
		}
		if (hit.startsWith("seal:")) {
			for (TechniqueRules.Family f : TechniqueRules.Family.values()) {
				if (hit.equals("seal:" + f.id)) {
					open = f;
				}
			}
			click(1.2F);
			return true;
		}
		if (hit.startsWith("part:")) {
			String part = hit.substring(5);
			if (!Techniques.knows(player, part)) {
				deny();
				return true;
			}
			draft = d.part(open, part);
			click(1.0F + 0.08F * TechniqueRules.parts(open).indexOf(part));
			// Picked a stroke: on to its release; a release: on to its intent (a seal clicked goes back).
			if (open == TechniqueRules.Family.STROKE) {
				open = TechniqueRules.Family.RELEASE;
			} else if (open == TechniqueRules.Family.RELEASE) {
				open = TechniqueRules.Family.INTENT;
			}
			return true;
		}
		if (hit.startsWith("token:")) {
			if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
				return true;
			}
			if (d.tokens().size() >= TechniqueRules.MAX_STRING) {
				deny();
				return true;
			}
			SwordString.Token token = SwordString.Token.byId(hit.substring(6)).orElseThrow();
			List<SwordString.Token> next = new ArrayList<>(d.tokens());
			next.add(token);
			draft = d.tokens(next);
			net.minecraft.sounds.SoundEvent tick = dev.wildercord.content.WildercordSounds.kit("aura_string_tick");
			if (tick != null) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(tick, dev.wildercord.cast.feel.Feels.step(next.size() - 1), 0.6F));
			}
			return true;
		}
		if (hit.equals("back")) {
			takeBack(button == InputConstants.MOUSE_BUTTON_RIGHT);
			return true;
		}
		if (hit.equals("write")) {
			write(player, d, now);
			return true;
		}
		if (hit.equals("erase")) {
			Techniques.Written w = Techniques.book(player).slot(slot);
			if (w.empty()) {
				return true;
			}
			if (now - eraseArmedAt < 60) {
				ClientPlayNetworking.send(new Techniques.Erase(slot));
				eraseArmedAt = -1000;
				draftFor = -1;
				click(0.7F);
			} else {
				eraseArmedAt = now;
				click(0.9F);
			}
			return true;
		}
		if (hit.startsWith("temper:") || hit.startsWith("edge:")) {
			boolean edge = hit.startsWith("edge:");
			String choice = hit.substring(hit.indexOf(':') + 1);
			Techniques.Written w = Techniques.book(player).slot(slot);
			int needs = edge ? TechniqueRules.EDGE_RANK : TechniqueRules.TEMPER_RANK;
			if (w.empty() || Techniques.rank(player, w) < needs) {
				deny();
				return true;
			}
			ClientPlayNetworking.send(new Techniques.Choose(slot, edge, choice.equals("none") ? "" : choice));
			click(1.15F);
			return true;
		}
		if (hit.startsWith("inscribe:")) {
			String part = hit.substring(9);
			if (!TechniqueRules.scrollable(part)) {
				deny();
				return true;
			}
			ClientPlayNetworking.send(new Techniques.Inscribe(slot, part));
			click(0.85F);
			return true;
		}
		return false;
	}

	private void takeBack(boolean all) {
		if (draft == null || draft.tokens().isEmpty()) {
			return;
		}
		List<SwordString.Token> next = new ArrayList<>(draft.tokens());
		if (all) {
			next.clear();
		} else {
			next.removeLast();
		}
		draft = draft.tokens(next);
		click(0.8F);
	}

	private void write(LocalPlayer player, Draft d, long now) {
		Status status = status(player, d);
		if (!status.writable()) {
			deny();
			return;
		}
		String name = d.name().isBlank() ? TechniqueRules.autoName(d.stroke(), d.release(), d.intent(), Techniques.flavour(player)) : d.name();
		ClientPlayNetworking.send(new Techniques.Write(slot, name, d.stroke(), d.release(), d.intent(), d.string().map(SwordString::text).orElse("")));
		draft = d.name(TechniqueRules.cleanName(name));
		inkAt = now;
		typing = false;
	}

	/** A key on the page; returns whether it took it (Escape while typing only stops the typing). */
	boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (typing) {
			if (key == InputConstants.KEY_BACKSPACE) {
				if (draft != null && !draft.name().isEmpty()) {
					String n = draft.name();
					draft = draft.name(event.hasControlDown() ? "" : n.substring(0, n.offsetByCodePoints(n.length(), -1)));
				}
				return true;
			}
			if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_TAB || event.isEscape()) {
				typing = false;
				click(1.1F);
				return true;
			}
			return true;
		}
		if (key == InputConstants.KEY_BACKSPACE) {
			takeBack(event.hasControlDown());
			return true;
		}
		return false;
	}

	/** A letter typed: into the name while it's being typed (never past its length once made safe). */
	boolean charTyped(CharacterEvent event) {
		if (!typing || draft == null || !event.isAllowedChatCharacter()) {
			return false;
		}
		String typed = event.codepointAsString();
		if (typed.codePoints().anyMatch(cp -> !TechniqueRules.nameCharacter(cp))) {
			return true;
		}
		String next = draft.name() + typed;
		if (TechniqueRules.cleanName(next).length() <= TechniqueRules.MAX_NAME && next.length() <= TechniqueRules.MAX_NAME * 2) {
			draft = draft.name(next);
		}
		return true;
	}
}
