package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.loadout.Loadout;
import dev.wildercord.loadout.LoadoutData;
import dev.wildercord.loadout.LoadoutRules;
import dev.wildercord.loadout.Loadouts;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Cord screen's loadouts: a compact panel over the window, opened from the list badge in the
 * header (or Ctrl+L). Each saved loadout is a row with its name, the first runes of its selected spell
 * and four buttons: Load, save the Cord as it is now over it, rename it and delete it (the last two
 * that lose something ask for a second click). The first empty row saves the Cord as a new loadout.
 *
 * <p>Everything is asked of the server, which checks it and answers above the hotbar; the list itself
 * is the synced {@link Loadouts#DATA}. After a load the Cord screen reads the spellbook again once the
 * new one arrives (see {@link #loaded}). Drawn in the Cord screen's own local space, with its sprites
 * and colours.</p>
 */
final class LoadoutPanel {
	private static final int X = 22;
	private static final int TOP = 24;
	private static final int ROW = 20;
	private static final int PAD = 8;
	private static final int TITLE_H = 18;
	private static final int NAME_X = 28;
	private static final int NAME_W = 92;
	private static final int ICONS = 5;
	private static final int ICON_PITCH = 17;
	private static final int BUTTON = 14;
	private static final String[] GLYPHS = {"", "\u21E9", "\u270E", "\u2715"};
	private static final String[] BUTTON_KEYS = {"load", "save_here", "rename", "delete"};
	/** How long a button that asks for a second click waits for it. */
	private static final long CONFIRM_MS = 3000;

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int WARN = 0xFFF0C440;
	private static final int CYAN = 0xFF7FE0F0;

	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_ROW = Wildercord.id("cord/row");
	private static final Identifier SPR_ROW_SELECTED = Wildercord.id("cord/row_selected");
	private static final Identifier SPR_TAB = Wildercord.id("cord/tab");
	private static final Identifier SPR_TAB_ACTIVE = Wildercord.id("cord/tab_active");

	private boolean open;
	/** The row the keyboard is on. */
	private int cursor;
	/** Typing a name: -1 for a new loadout, a row for renaming it, -2 when not typing. */
	private int naming = -2;
	private String typed = "";
	/** A button waiting for its second click: its row and which one, and until when. */
	private int confirmRow = -1;
	private int confirmButton = -1;
	private long confirmUntil;
	/** Why the last request was refused here, shown under the list for a few seconds. */
	private Component status;
	private long statusUntil;
	/** A load sent: the spellbook as it was, until a different one arrives (or it's given up on). */
	private Spellbook pendingFrom;
	private long pendingUntil;

	boolean isOpen() {
		return open;
	}

	void toggle() {
		if (open) {
			close();
		} else {
			open = true;
			cursor = Math.max(0, Math.min(cursor, data().size() - 1));
			naming = -2;
			confirmRow = -1;
		}
	}

	void close() {
		open = false;
		naming = -2;
		confirmRow = -1;
	}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	private static LoadoutData data() {
		return mc().player == null ? LoadoutData.EMPTY : Loadouts.data(mc().player);
	}

	/** Rows drawn: at least one for each loadout that can be kept, so the empty places show what's left. */
	private static int rows(LoadoutData data) {
		return Math.max(LoadoutRules.MAX, data.size());
	}

	// ------------------------------------------------------------------ layout

	private static int right(int windowW) {
		return windowW - X;
	}

	private static int listTop() {
		return TOP + PAD + TITLE_H;
	}

	private static int rowY(int row) {
		return listTop() + row * ROW;
	}

	private static int bottom(LoadoutData data) {
		return listTop() + rows(data) * ROW + 6 + 2 * 10 + PAD;
	}

	/** The x of button {@code button} (0 load, 1 save here, 2 rename, 3 delete) on a row. */
	private static int buttonX(int button, int windowW, Font font) {
		int r = right(windowW) - PAD - 2;
		if (button > 0) {
			return r - (4 - button) * (BUTTON + 2);
		}
		return r - 3 * (BUTTON + 2) - 4 - loadWidth(font);
	}

	private static int buttonW(int button, Font font) {
		return button == 0 ? loadWidth(font) : BUTTON;
	}

	private static int loadWidth(Font font) {
		return font.width(Component.translatable("screen.wildercord.loadouts.load")) + 12;
	}

	private static int closeX(int windowW) {
		return right(windowW) - PAD - BUTTON;
	}

	/** The middle of button {@code button} on loadout {@code row}, in the Cord screen's local space, or null if there's no such loadout. */
	double[] buttonPoint(int row, int button, int windowW) {
		if (row < 0 || row >= data().size()) {
			return null;
		}
		Font font = mc().font;
		return new double[] {buttonX(button, windowW, font) + buttonW(button, font) / 2.0, rowY(row) + 2 + BUTTON / 2.0};
	}

	/** The middle of the "Save current as new" button, in local space, or null when every place is taken. */
	double[] saveNewPoint() {
		LoadoutData data = data();
		if (data.size() >= LoadoutRules.MAX) {
			return null;
		}
		int w = mc().font.width(Component.translatable("screen.wildercord.loadouts.save_new")) + 12;
		return new double[] {X + NAME_X + w / 2.0, rowY(data.size()) + 2 + 13 / 2.0};
	}

	// ------------------------------------------------------------------ after a load

	/**
	 * Whether a load sent from here has come back: the synced spellbook is no longer the one it was
	 * sent from. The Cord screen then reads its rows again.
	 */
	boolean loaded(Spellbook now) {
		if (pendingFrom == null) {
			return false;
		}
		if (!now.equals(pendingFrom)) {
			pendingFrom = null;
			return true;
		}
		if (net.minecraft.util.Util.getMillis() > pendingUntil) {
			// Nothing changed: refused, or it held the same runes. Nothing to read again.
			pendingFrom = null;
		}
		return false;
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

	/** Draws the panel over the window (in its local space) and returns the tooltip under the mouse, if any. */
	List<Component> draw(GuiGraphicsExtractor g, int mx, int my, int windowW, int windowH) {
		Font font = mc().font;
		LoadoutData data = data();
		long now = net.minecraft.util.Util.getMillis();
		if (confirmRow >= 0 && now > confirmUntil) {
			confirmRow = -1;
		}
		cursor = Math.max(0, Math.min(cursor, Math.max(0, data.size() - 1)));
		// The window behind is dimmed, like the world behind the window.
		g.fill(0, 0, windowW, windowH, 0x90080610);
		int right = right(windowW);
		int bottom = bottom(data);
		sprite(g, SPR_PANEL, X, TOP, right - X, bottom - TOP);
		List<Component> tip = null;

		// The title, and a close button.
		g.text(font, Component.translatable("screen.wildercord.loadouts.title", data.size(), LoadoutRules.MAX), X + PAD + 2, TOP + PAD, GOLD, true);
		int cx = closeX(windowW);
		boolean closeHover = inside(mx, my, cx, TOP + PAD - 3, BUTTON, BUTTON);
		sprite(g, closeHover ? SPR_TAB_ACTIVE : SPR_TAB, cx, TOP + PAD - 3, BUTTON, BUTTON);
		g.text(font, "\u00D7", cx + BUTTON / 2 - font.width("\u00D7") / 2 + 1, TOP + PAD, closeHover ? TEXT : DIM, false);
		if (closeHover) {
			tip = List.of(Component.translatable("screen.wildercord.loadouts.close").withStyle(ChatFormatting.GRAY));
		}

		int rows = rows(data);
		sprite(g, SPR_INSET, X + PAD - 2, listTop() - 3, right - X - 2 * PAD + 4, rows * ROW + 4);
		for (int i = 0; i < rows; i++) {
			List<Component> rowTip = i < data.size() ? drawRow(g, font, data, i, mx, my, windowW) : drawEmpty(g, font, data, i, mx, my);
			if (rowTip != null) {
				tip = rowTip;
			}
		}

		// Under the list: what loading does, and the keys (or why the last request was refused).
		int y = listTop() + rows * ROW + 6;
		fitText(g, font, Component.translatable("screen.wildercord.loadouts.note"), X + PAD + 2, y, right - X - 2 * PAD - 4, FAINT, false);
		Component last = status != null && now < statusUntil ? status : null;
		fitText(g, font, last != null ? last : Component.translatable("screen.wildercord.loadouts.keys"), X + PAD + 2, y + 10, right - X - 2 * PAD - 4,
			last != null ? WARN : FAINT, false);
		return tip;
	}

	private List<Component> drawRow(GuiGraphicsExtractor g, Font font, LoadoutData data, int i, int mx, int my, int windowW) {
		Loadout loadout = data.get(i);
		int y = rowY(i);
		int right = right(windowW);
		boolean selected = i == cursor;
		sprite(g, selected ? SPR_ROW_SELECTED : SPR_ROW, X + PAD, y, right - X - 2 * PAD, ROW - 1);
		boolean current = i == data.current();
		g.text(font, Integer.toString(i + 1), X + PAD + 6, y + 6, current ? CYAN : selected ? GOLD : DIM, true);
		List<Component> tip = null;
		int loadX = buttonX(0, windowW, font);
		int iconsX = X + NAME_X + NAME_W + 4;
		boolean typing = naming == i;
		if (typing) {
			// Renaming: only the Rename button stays, and the hint takes the others' room.
			String shown = font.plainSubstrByWidth(typed, NAME_W - 6, true) + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
			g.text(font, shown, X + NAME_X, y + 6, TEXT, false);
			fitText(g, font, Component.translatable("screen.wildercord.loadouts.typing"), iconsX, y + 6, buttonX(2, windowW, font) - 4 - iconsX, FAINT, false);
		} else if (confirmRow == i) {
			// Asking for the second click: the question takes the name's and the runes' place.
			String key = confirmButton == 1 ? "screen.wildercord.loadouts.confirm_save" : "screen.wildercord.loadouts.confirm_delete";
			fitText(g, font, Component.translatable(key, loadout.name()), X + NAME_X, y + 6, loadX - 4 - (X + NAME_X), WARN, false);
		} else {
			fitText(g, font, Component.literal(loadout.name()), X + NAME_X, y + 6, NAME_W, selected ? GOLD : TEXT, false);
			List<String> runes = shownRunes(loadout);
			for (int k = 0; k < Math.min(ICONS, runes.size()); k++) {
				g.item(RuneItem.stack(runes.get(k)), iconsX + k * ICON_PITCH, y + 1);
			}
			if (runes.size() > ICONS) {
				g.text(font, "+" + (runes.size() - ICONS), iconsX + ICONS * ICON_PITCH + 1, y + 6, FAINT, false);
			}
			if (inside(mx, my, X + PAD, y, loadX - 4 - (X + PAD), ROW - 1)) {
				tip = describe(loadout, current);
			}
		}
		for (int b = 0; b < 4; b++) {
			if (typing && b != 2) {
				continue;
			}
			int bx = buttonX(b, windowW, font);
			int bw = buttonW(b, font);
			boolean hover = inside(mx, my, bx, y + 2, bw, BUTTON);
			boolean asking = confirmRow == i && confirmButton == b;
			sprite(g, hover || asking || typing && b == 2 ? SPR_TAB_ACTIVE : SPR_TAB, bx, y + 2, bw, BUTTON);
			int color = asking ? GOLD : hover ? TEXT : b == 0 ? CYAN : DIM;
			if (b == 0) {
				Component label = Component.translatable("screen.wildercord.loadouts.load");
				g.text(font, label, bx + bw / 2 - font.width(label) / 2, y + 5, color, false);
			} else {
				g.text(font, GLYPHS[b], bx + bw / 2 - font.width(GLYPHS[b]) / 2 + 1, y + 5, color, false);
			}
			if (hover) {
				tip = List.of(Component.translatable("screen.wildercord.loadouts." + BUTTON_KEYS[b]).withStyle(ChatFormatting.GOLD),
					Component.translatable("screen.wildercord.loadouts." + BUTTON_KEYS[b] + ".hint").withStyle(ChatFormatting.GRAY));
			}
		}
		return tip;
	}

	/** An empty place: the first one saves the Cord as a new loadout, the rest just show how many are left. */
	private List<Component> drawEmpty(GuiGraphicsExtractor g, Font font, LoadoutData data, int i, int mx, int my) {
		int y = rowY(i);
		g.text(font, Integer.toString(i + 1), X + PAD + 6, y + 6, FAINT, false);
		if (i != data.size()) {
			return null;
		}
		if (naming == -1) {
			String shown = font.plainSubstrByWidth(typed, NAME_W - 6, true) + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
			sprite(g, SPR_TAB_ACTIVE, X + NAME_X - 4, y + 2, NAME_W + 4, 14);
			g.text(font, shown, X + NAME_X, y + 5, TEXT, false);
			g.text(font, Component.translatable("screen.wildercord.loadouts.typing"), X + NAME_X + NAME_W + 8, y + 5, FAINT, false);
			return null;
		}
		Component label = Component.translatable("screen.wildercord.loadouts.save_new");
		int w = font.width(label) + 12;
		boolean hover = inside(mx, my, X + NAME_X - 4, y + 2, w, 13);
		sprite(g, hover ? SPR_TAB_ACTIVE : SPR_TAB, X + NAME_X - 4, y + 2, w, 13);
		g.text(font, label, X + NAME_X + 2, y + 5, hover ? GOLD : TEXT, false);
		if (hover) {
			return List.of(Component.translatable("screen.wildercord.loadouts.save_new").withStyle(ChatFormatting.GOLD),
				Component.translatable("screen.wildercord.loadouts.save_new.hint").withStyle(ChatFormatting.GRAY));
		}
		return null;
	}

	/** The runes shown on a loadout's row: its selected spell's, or the first spell with any. */
	private static List<String> shownRunes(Loadout loadout) {
		List<String> runes = loadout.spells().get(loadout.selected());
		if (!runes.isEmpty()) {
			return runes;
		}
		for (List<String> spell : loadout.spells()) {
			if (!spell.isEmpty()) {
				return spell;
			}
		}
		return List.of();
	}

	/** A loadout's tooltip: every spell and passive in it, and what loading it does. */
	private static List<Component> describe(Loadout loadout, boolean current) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(loadout.name()).withColor(GOLD));
		if (current) {
			lines.add(Component.translatable("screen.wildercord.loadouts.current").withColor(CYAN));
		}
		boolean any = false;
		for (int s = 0; s < loadout.spells().size(); s++) {
			List<String> spell = loadout.spells().get(s);
			if (spell.isEmpty()) {
				continue;
			}
			any = true;
			MutableComponent line = Component.translatable(s == loadout.selected() ? "screen.wildercord.loadouts.spell_selected" : "screen.wildercord.loadouts.spell",
				s + 1).withStyle(s == loadout.selected() ? ChatFormatting.WHITE : ChatFormatting.GRAY);
			String custom = loadout.names().get(s);
			if (!custom.isEmpty()) {
				line.append(Component.literal(custom + ": ").withColor(GOLD));
			}
			lines.add(line.append(runeList(spell)));
		}
		for (int p = 0; p < loadout.passives().size(); p++) {
			List<String> passive = loadout.passives().get(p);
			if (passive.isEmpty()) {
				continue;
			}
			any = true;
			MutableComponent line = Component.translatable(loadout.passiveOn(p) ? "screen.wildercord.loadouts.passive" : "screen.wildercord.loadouts.passive_off",
				p + 1).withColor(0xB8A8FF);
			lines.add(line.append(runeList(passive)));
		}
		if (!any) {
			lines.add(Component.translatable("screen.wildercord.loadouts.nothing").withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.add(Component.translatable("screen.wildercord.loadouts.load.hint").withStyle(ChatFormatting.DARK_AQUA));
		return lines;
	}

	/** Runes by name in their colours, a quiet one (unknown here, or a rune this game doesn't have) greyed. */
	private static MutableComponent runeList(List<String> ids) {
		MutableComponent out = Component.empty();
		Spellbook book = mc().player == null ? Spellbook.EMPTY : Spellbooks.get(mc().player);
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) {
				out.append(Component.literal(" \u00B7 ").withStyle(ChatFormatting.DARK_GRAY));
			}
			Optional<RuneDef> rune = Runes.get(ids.get(i));
			if (rune.isEmpty()) {
				out.append(Component.translatable("item.wildercord.rune.silent").withStyle(ChatFormatting.DARK_GRAY));
			} else if (!book.knows(rune.get().id())) {
				out.append(RuneItem.runeName(rune.get()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
			} else {
				out.append(RuneItem.runeName(rune.get()).withColor(RuneColors.of(rune.get())));
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ mouse

	/** A click while the panel is open. Anything outside it closes it. */
	void click(double mx, double my, int windowW) {
		Font font = mc().font;
		LoadoutData data = data();
		int right = right(windowW);
		if (!inside(mx, my, X, TOP, right - X, bottom(data) - TOP)) {
			close();
			return;
		}
		if (inside(mx, my, closeX(windowW), TOP + PAD - 3, BUTTON, BUTTON)) {
			close();
			sound();
			return;
		}
		for (int i = 0; i < rows(data); i++) {
			int y = rowY(i);
			if (my < y || my >= y + ROW) {
				continue;
			}
			if (i >= data.size()) {
				if (i == data.size() && naming != -1) {
					int w = font.width(Component.translatable("screen.wildercord.loadouts.save_new")) + 12;
					if (inside(mx, my, X + NAME_X - 4, y + 2, w, 13)) {
						startNaming(-1);
						sound();
					}
				}
				return;
			}
			for (int b = 0; b < 4; b++) {
				// While a row is renamed only its Rename button is there.
				if ((naming != i || b == 2) && inside(mx, my, buttonX(b, windowW, font), y + 2, buttonW(b, font), BUTTON)) {
					cursor = i;
					press(i, b);
					return;
				}
			}
			if (naming != i) {
				naming = -2;
			}
			cursor = i;
			return;
		}
	}

	/** Button {@code button} (0 load, 1 save here, 2 rename, 3 delete) on loadout {@code row}. */
	private void press(int row, int button) {
		LoadoutData data = data();
		if (row < 0 || row >= data.size()) {
			return;
		}
		switch (button) {
			case 0 -> {
				naming = -2;
				pendingFrom = Spellbooks.get(mc().player);
				pendingUntil = net.minecraft.util.Util.getMillis() + 3000;
				send(Loadouts.LOAD, row, "");
				close();
			}
			case 1, 3 -> {
				naming = -2;
				if (confirmRow == row && confirmButton == button && net.minecraft.util.Util.getMillis() <= confirmUntil) {
					confirmRow = -1;
					send(button == 1 ? Loadouts.SAVE_OVER : Loadouts.DELETE, row, "");
				} else {
					confirmRow = row;
					confirmButton = button;
					confirmUntil = net.minecraft.util.Util.getMillis() + CONFIRM_MS;
				}
			}
			case 2 -> {
				if (naming == row) {
					naming = -2;
				} else {
					startNaming(row);
				}
			}
			default -> { }
		}
		sound();
	}

	private void startNaming(int row) {
		confirmRow = -1;
		naming = row;
		LoadoutData data = data();
		typed = row >= 0 ? data.get(row).name()
			: Component.translatable("screen.wildercord.loadouts.default_name", data.size() + 1).getString();
	}

	/** Sends the name being typed: a new loadout, or a new name for one. */
	private void submitName() {
		LoadoutData data = data();
		String name = LoadoutRules.name(typed);
		if (name == null) {
			refuse(Component.translatable("message.wildercord.loadout.bad_name", LoadoutRules.MAX_NAME));
			return;
		}
		int other = LoadoutRules.find(data.names(), name);
		if (other >= 0 && other != naming) {
			refuse(Component.translatable("message.wildercord.loadout.name_taken", data.get(other).name()));
			return;
		}
		if (naming == -1) {
			if (data.size() >= LoadoutRules.MAX) {
				refuse(Component.translatable("message.wildercord.loadout.full", LoadoutRules.MAX));
				return;
			}
			send(Loadouts.SAVE_NEW, -1, name);
			cursor = data.size();
		} else {
			send(Loadouts.RENAME, naming, name);
		}
		naming = -2;
		sound();
	}

	private static void send(int kind, int index, String name) {
		ClientPlayNetworking.send(new WildercordNetworking.LoadoutRequest(kind, index, name));
	}

	private void refuse(Component why) {
		status = why;
		statusUntil = net.minecraft.util.Util.getMillis() + 4000;
		mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6F));
	}

	private static void sound() {
		mc().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	// ------------------------------------------------------------------ keyboard

	/** A typed character while the panel is open: part of a name being typed, otherwise ignored (it mustn't search behind). */
	void charTyped(CharacterEvent event) {
		if (naming != -2 && event.isAllowedChatCharacter() && typed.length() < LoadoutRules.MAX_NAME) {
			typed += event.codepointAsString();
		}
	}

	/**
	 * A key while the panel is open. Typing a name: Enter saves it, Esc cancels. Otherwise the arrows pick
	 * a loadout, Enter loads it, Ctrl+R renames it, Delete deletes it and Ctrl+S saves over it (both asking
	 * twice), Ctrl+N saves the Cord as a new one, and Esc or Ctrl+L closes the panel.
	 */
	void key(KeyEvent event) {
		LoadoutData data = data();
		int key = event.key();
		if (naming != -2) {
			if (key == InputConstants.KEY_BACKSPACE) {
				if (!typed.isEmpty()) {
					typed = event.hasControlDown() ? "" : typed.substring(0, typed.length() - 1);
				}
			} else if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
				submitName();
			} else if (event.isEscape()) {
				naming = -2;
			}
			return;
		}
		if (event.isEscape() || event.hasControlDown() && key == InputConstants.KEY_L) {
			close();
		} else if (key == InputConstants.KEY_UP && data.size() > 0) {
			cursor = Math.floorMod(cursor - 1, data.size());
			confirmRow = -1;
		} else if (key == InputConstants.KEY_DOWN && data.size() > 0) {
			cursor = Math.floorMod(cursor + 1, data.size());
			confirmRow = -1;
		} else if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) && cursor < data.size()) {
			press(cursor, 0);
		} else if (event.hasControlDown() && key == InputConstants.KEY_R && cursor < data.size()) {
			press(cursor, 2);
		} else if (key == InputConstants.KEY_DELETE && cursor < data.size()) {
			press(cursor, 3);
		} else if (event.hasControlDown() && key == InputConstants.KEY_S && cursor < data.size()) {
			press(cursor, 1);
		} else if (event.hasControlDown() && key == InputConstants.KEY_N) {
			if (data.size() >= LoadoutRules.MAX) {
				refuse(Component.translatable("message.wildercord.loadout.full", LoadoutRules.MAX));
			} else {
				startNaming(-1);
				sound();
			}
		}
	}

	/** Whether a name is being typed (so the Cord screen's own shortcuts stay out of the way). */
	boolean typing() {
		return naming != -2;
	}
}
