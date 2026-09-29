package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.PassiveCaster;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.RuneRanks;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.Ranks;
import dev.wildercord.spell.Passives;
import dev.wildercord.spell.RuneCategories;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The Cord screen (K): thread learned runes into spells, within what the worn Cord allows.
 * Click a rune to add it to the selected spell, drag it onto a socket to insert it there,
 * drag threaded runes to reorder, and click a threaded rune to take it out.
 *
 * <p>The Codex can be searched (just start typing, or Ctrl+F) and narrowed by family and by
 * category (Effects: Damage, Control, Support...). It lists runes under category headers.</p>
 *
 * <p>Everything is drawn in a fixed local space; on screens too small for it the whole window
 * is scaled down by a ratio that keeps pixels crisp. Every piece of text is measured: it is
 * wrapped, clipped, or left out rather than allowed to spill.</p>
 */
public class CordScreen extends Screen {
	private static final int W = 372;
	private static final int BASE_H = 292;
	private static final int CELL = 18;
	private static final int PITCH = 20;
	private static final int SPELL_TOP = 28;
	private static final int SPELL_ROW = 22;
	private static final int SOCKET_X = 34;
	private static final int CODEX_HEIGHT = 4 * CELL;
	private static final int CODEX_X = 14;
	/** Width of the category label at the start of each Codex row. */
	private static final int LABEL_W = 70;
	private static final int CODEX_COLS = (W - 24 - CODEX_X - LABEL_W) / CELL;
	// Everything below the rows moves down a row while the Tome of the Fifth Page is in the off-hand
	// (its spell gets a row of its own), so these are worked out again by layout() every frame.
	private int H = BASE_H;
	private int TABS_TOP = SPELL_TOP + 4 * SPELL_ROW + 4;
	private int CHIPS_TOP = TABS_TOP + 16;
	private int CODEX_TOP = CHIPS_TOP + 16;
	private int CODEX_BOTTOM = CODEX_TOP + CODEX_HEIGHT;
	private int READOUT_TOP = CODEX_BOTTOM + 9;
	private int READOUT_BOTTOM = H - 12;
	private static final int TEXT_X = 16;
	private static final int TEXT_RIGHT = W - 18;
	private static final int LINE = 10;
	private static final int SEARCH_W = 90;

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int QUIET = 0xFFE05050;
	private static final int WARN = 0xFFF0C440;
	private static final int CYAN = 0xFF7FE0F0;

	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_ROW = Wildercord.id("cord/row");
	private static final Identifier SPR_ROW_SELECTED = Wildercord.id("cord/row_selected");
	private static final Identifier SPR_ROW_LOCKED = Wildercord.id("cord/row_locked");
	private static final Identifier SPR_SOCKET = Wildercord.id("cord/socket");
	private static final Identifier SPR_SOCKET_HOVER = Wildercord.id("cord/socket_hover");
	private static final Identifier SPR_SOCKET_QUIET = Wildercord.id("cord/socket_quiet");
	private static final Identifier SPR_THREAD = Wildercord.id("cord/thread");
	private static final Identifier SPR_TAB = Wildercord.id("cord/tab");
	private static final Identifier SPR_TAB_ACTIVE = Wildercord.id("cord/tab_active");
	private static final Identifier SPR_LOCK = Wildercord.id("cord/lock");
	private static final Identifier SPR_SCROLLER = Wildercord.id("cord/scroller");
	private static final Identifier SPR_ATTACH = Wildercord.id("cord/attach");
	private static final Identifier SPR_BADGE = Wildercord.id("hud/badge");
	private static final Identifier SPR_MANA = Wildercord.id("cord/mana_badge");
	private static final Identifier SPR_HEART = Wildercord.id("cord/heart_badge");
	private static final int LAVENDER = 0xFFB8A8FF;

	private static final String[] TAB_KEYS = {"all", "shape", "effect", "modifier", "link"};
	private static final RuneFamily[] TAB_FAMILIES = {null, RuneFamily.SHAPE, RuneFamily.EFFECT, RuneFamily.MODIFIER, RuneFamily.LINK};

	private final List<List<String>> spells = new ArrayList<>();
	private int editing;
	/** The Passives page: the same rows, but for the (up to) two always-on passives. */
	private final List<List<String>> passives = new ArrayList<>();
	private boolean passivePage;
	/** The Grimoire page: everything discovered, in place of the rows, Codex and readout. */
	private boolean grimoirePage;
	private int grimoireScroll;
	private int editingPassive;
	/** Renaming the selected spell: the name as typed so far. */
	private boolean renaming;
	private String renameText = "";
	private RuneFamily filter;
	private String category;
	private String query = "";
	private boolean searchFocused;
	private int codexScroll;
	private int readoutScroll;

	// Drag state: what was pressed, and whether it has moved far enough to count as a drag.
	private String pressedRune;
	private int pressedSpell = -1;
	private int pressedSocket = -1;
	/** The mouse button that started the press: only letting go of that one ends it. */
	private int pressButton;
	/** Why the last click was refused, shown at the top of the readout for a few seconds. */
	private Component refused;
	private long refusedUntil;
	private double pressX;
	private double pressY;
	private boolean dragging;

	public CordScreen() {
		super(Component.translatable("screen.wildercord.cord"));
	}

	/** Lays the window out for the rows showing: a fifth spell row while the tome is held. */
	private void layout() {
		int extra = tomeRow() ? SPELL_ROW : 0;
		H = BASE_H + extra;
		TABS_TOP = SPELL_TOP + 4 * SPELL_ROW + 4 + extra;
		CHIPS_TOP = TABS_TOP + 16;
		CODEX_TOP = CHIPS_TOP + 16;
		CODEX_BOTTOM = CODEX_TOP + CODEX_HEIGHT;
		READOUT_TOP = CODEX_BOTTOM + 9;
		READOUT_BOTTOM = H - 12;
		if (!passivePage && !spellOpen(editing) && spellCount() > 0) {
			// The tome left the hand while its spell was being edited.
			editing = 0;
			readoutScroll = 0;
		}
	}

	@Override
	protected void init() {
		layout();
		// Since 26.x (SDL input) typed characters only arrive while a screen asks for text input.
		// Typing anywhere searches the Codex, so ask for the whole time the screen is open; the
		// game turns it off again when the screen closes. The IME window sits by the search box.
		minecraft.textInputManager().startTextInput(this);
		float s = scale();
		int sx = Math.round(left() + searchX() * s);
		int sy = Math.round(top() + TABS_TOP * s);
		minecraft.textInputManager().setTextInputArea(sx, sy, sx + Math.round(SEARCH_W * s), sy + Math.round(13 * s));
		Player player = minecraft.player;
		if (player == null) {
			return;
		}
		Spellbook book = Spellbooks.get(player);
		spells.clear();
		for (List<String> spell : book.spells()) {
			spells.add(new ArrayList<>(spell));
		}
		passives.clear();
		for (List<String> passive : book.passives()) {
			passives.add(new ArrayList<>(passive));
		}
		editing = spellOpen(book.selected()) ? book.selected() : Math.max(0, Math.min(book.selected(), spellCount() - 1));
	}

	@Override
	public void removed() {
		minecraft.textInputManager().stopTextInput(this);
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Dims the world like the inventory does, instead of blurring it like a menu. */
	@Override
	public boolean isInGameUi() {
		return true;
	}

	// ------------------------------------------------------------------ layout and scale

	/** Content scale: 1, or smaller on tiny screens, snapped so pixels stay crisp. */
	private float scale() {
		double fit = Math.min(1.0, Math.min((width - 8) / (double) W, (height - 8) / (double) H));
		if (fit >= 1.0) {
			return 1.0F;
		}
		int guiScale = Math.max(1, minecraft.getWindow().getGuiScale());
		int physical = Math.max(1, (int) Math.floor(guiScale * fit));
		return physical / (float) guiScale;
	}

	private int left() {
		return Math.round((width - W * scale()) / 2);
	}

	private int top() {
		return Math.round((height - H * scale()) / 2);
	}

	// ------------------------------------------------------------------ for the game tests (read-only, or as typing would)

	/** A point in the screen's own layout, in GUI coordinates. */
	private double[] onScreen(double lx, double ly) {
		return new double[] {left() + lx * scale(), top() + ly * scale()};
	}

	/** The middle of rune socket {@code socket} on row {@code row} (spells or passives), on screen. */
	public double[] socketPoint(int row, int socket) {
		return onScreen(SOCKET_X + socket * PITCH + PITCH / 2.0, SPELL_TOP + row * SPELL_ROW + SPELL_ROW / 2.0 - 2);
	}

	/** A point on row {@code row} left of its sockets: selects the row without touching a rune. */
	public double[] rowPoint(int row) {
		return onScreen(20, SPELL_TOP + row * SPELL_ROW + SPELL_ROW / 2.0 - 2);
	}

	/** The middle of page tab {@code page} (0 Spells, 1 Passives, 2 Grimoire, 3 Cosmetics), on screen. */
	public double[] pagePoint(int page) {
		int x = 13 + font.width(Component.translatable(tier().itemKey())) + 8;
		for (int i = 0; i < page; i++) {
			x += font.width(Component.translatable(PAGE_KEYS[i])) + 10 + 2;
		}
		return onScreen(x + (font.width(Component.translatable(PAGE_KEYS[page])) + 10) / 2.0, 7 + 6.5);
	}

	/** Filters the Codex, as typing would. */
	public void searchFor(String text) {
		query = text;
		codexScroll = 0;
	}

	/** The middle of {@code runeId}'s cell in the Codex, on screen, or null if it isn't showing. */
	public double[] codexPoint(String runeId) {
		int y = CODEX_TOP - codexScroll;
		for (CodexRow row : codexRows()) {
			int h = row.height();
			if (row.runes() != null && y >= CODEX_TOP && y + h <= CODEX_BOTTOM) {
				for (int i = 0; i < row.runes().size(); i++) {
					if (row.runes().get(i).id().equals(runeId)) {
						return onScreen(CODEX_X + LABEL_W + i * CELL + CELL / 2.0, y + h / 2.0);
					}
				}
			}
			y += h;
		}
		return null;
	}

	/** The row being edited on the page showing. */
	public int editingRow() {
		return passivePage ? editingPassive : editing;
	}

	/** The runes on row {@code row} of the page showing, as the screen has them. */
	public List<String> rowRunes(int row) {
		return List.copyOf(rows().get(row));
	}

	/** Why the last click was refused, while that's still shown (else null). */
	public Component lastRefusal() {
		return refused != null && net.minecraft.util.Util.getMillis() < refusedUntil ? refused : null;
	}

	private double localX(double screenX) {
		return (screenX - left()) / scale();
	}

	private double localY(double screenY) {
		return (screenY - top()) / scale();
	}

	// ------------------------------------------------------------------ rules (mirroring the server)

	private CordTier tier() {
		return minecraft.player == null ? null : Spellbooks.tier(minecraft.player);
	}

	private Spellbook book() {
		return Spellbooks.get(minecraft.player);
	}

	private int sockets() {
		CordTier tier = tier();
		return tier == null ? 0 : tier.sockets;
	}

	private int spellCount() {
		CordTier tier = tier();
		return tier == null ? 0 : tier.spells;
	}

	/** Whether the Tome of the Fifth Page is in the off-hand (its spell gets a row). */
	private boolean tomeRow() {
		return minecraft.player != null && dev.wildercord.gear.Gear.tome(minecraft.player);
	}

	/** Whether spell row {@code s} can be used: one of the Cord's, or the tome's while it's held. */
	private boolean spellOpen(int s) {
		CordTier tier = tier();
		return tier != null && dev.wildercord.gear.Gear.spellOpen(minecraft.player, tier, s);
	}

	/** Whether row {@code s} of the page showing can be edited. */
	private boolean rowOpen(int s) {
		return passivePage ? s < openRows() : spellOpen(s);
	}

	private boolean holds(RuneDef rune) {
		CordTier tier = tier();
		return tier != null && tier.holds(rune.tier());
	}

	// ------------------------------------------------------------------ pages: spells or passives

	private List<List<String>> rows() {
		return passivePage ? passives : spells;
	}

	private int rowCount() {
		return passivePage ? Passives.MAX : CordTier.MAX_SPELLS + (tomeRow() ? 1 : 0);
	}

	/** Rows that can be edited: the Cord's spells, or the passive slots the heart has opened. */
	private int openRows() {
		return passivePage ? Passives.slots(Heart.active(minecraft.player)) : spellCount();
	}

	private int current() {
		return passivePage ? editingPassive : editing;
	}

	private int rowSockets() {
		CordTier tier = tier();
		return tier == null ? 0 : passivePage ? PassiveCaster.sockets(tier) : tier.sockets;
	}

	/** Whether a rune may go on the current page: held by the Cord, and sustainable on the Passives page. */
	private boolean fitsPage(RuneDef rune) {
		return holds(rune) && (!passivePage || Passives.allowed(rune));
	}

	/** Why a threaded rune won't fire, or null if it will. */
	private Component quietReason(String id, int socket) {
		Optional<RuneDef> rune = Runes.get(id);
		if (rune.isEmpty()) {
			return Component.translatable("screen.wildercord.quiet_silent");
		}
		if (socket >= sockets()) {
			return Component.translatable("screen.wildercord.quiet_socket", Component.translatable(tier().itemKey()), sockets());
		}
		if (!holds(rune.get())) {
			return Component.translatable("screen.wildercord.quiet_tier", Component.translatable(CordTier.forRuneTier(rune.get().tier()).itemKey()));
		}
		if (!book().knows(id)) {
			return Component.translatable("screen.wildercord.quiet_unlearned");
		}
		return null;
	}

	// ------------------------------------------------------------------ the Codex: search, categories, groups

	private static String familyKey(RuneFamily family) {
		return family.name().toLowerCase(Locale.ROOT);
	}

	private static Component categoryName(RuneDef rune) {
		return Component.translatable("category.wildercord." + familyKey(rune.family()) + "." + rune.category());
	}

	/** What a search always matches against: name, element, category, family and tier. */
	private static String haystack(RuneDef rune) {
		return (RuneItem.runeName(rune).getString() + " " + rune.element() + " "
			+ (rune.element().isEmpty() ? "" : Component.translatable("element.wildercord." + rune.element()).getString()) + " "
			+ categoryName(rune).getString() + " " + rune.category() + " "
			+ Component.translatable("family.wildercord." + familyKey(rune.family())).getString() + " "
			+ Component.translatable("screen.wildercord.tab." + familyKey(rune.family())).getString() + " tier" + rune.tier())
			.toLowerCase(Locale.ROOT);
	}

	private boolean matches(RuneDef rune) {
		if (filter != null && rune.family() != filter) {
			return false;
		}
		if (category != null && !rune.category().equals(category)) {
			return false;
		}
		String q = query.trim().toLowerCase(Locale.ROOT);
		if (q.isEmpty()) {
			return true;
		}
		String hay = haystack(rune);
		String description = null;
		for (String token : q.split("\\s+")) {
			if (hay.contains(token)) {
				continue;
			}
			// Descriptions only count for longer words, so "fire" doesn't match every rune that "fires".
			if (token.length() >= 5) {
				if (description == null) {
					description = RuneItem.runeDescription(rune).getString().toLowerCase(Locale.ROOT);
				}
				if (description.contains(token)) {
					continue;
				}
			}
			return false;
		}
		return true;
	}

	/** A Codex row: a category label (only on its first row) and up to a row of that category's runes. */
	private record CodexRow(Component label, int color, List<RuneDef> runes) {
		int height() {
			return CELL;
		}
	}

	/** Every rune the player knows: the roster's, then the Knots they've learned. */
	private List<RuneDef> known() {
		List<RuneDef> runes = new ArrayList<>();
		Spellbook book = book();
		for (RuneDef rune : Runes.all()) {
			if (book.knows(rune.id())) {
				runes.add(rune);
			}
		}
		for (String id : book.learned()) {
			if (Knots.isKnot(id)) {
				Runes.get(id).ifPresent(runes::add);
			}
		}
		return runes;
	}

	private List<CodexRow> codexRows() {
		List<RuneDef> runes = new ArrayList<>();
		if (minecraft.player == null) {
			return List.of();
		}
		for (RuneDef rune : known()) {
			if (matches(rune)) {
				runes.add(rune);
			}
		}
		runes.sort(Comparator.<RuneDef>comparingInt(r -> r.family().ordinal())
			.thenComparingInt(RuneCategories::order)
			.thenComparing(r -> !holds(r))
			.thenComparingInt(RuneDef::tier)
			.thenComparing(r -> RuneItem.runeName(r).getString()));
		List<CodexRow> rows = new ArrayList<>();
		RuneFamily groupFamily = null;
		String groupCategory = null;
		Component label = null;
		int color = 0;
		List<RuneDef> line = new ArrayList<>();
		for (RuneDef rune : runes) {
			if (rune.family() != groupFamily || !rune.category().equals(groupCategory)) {
				if (!line.isEmpty()) {
					rows.add(new CodexRow(label, color, line));
					line = new ArrayList<>();
				}
				groupFamily = rune.family();
				groupCategory = rune.category();
				label = categoryName(rune);
				color = 0xFF000000 | familyColor(rune.family());
			}
			line.add(rune);
			if (line.size() == CODEX_COLS) {
				rows.add(new CodexRow(label, color, line));
				line = new ArrayList<>();
				label = null;
			}
		}
		if (!line.isEmpty()) {
			rows.add(new CodexRow(label, color, line));
		}
		return rows;
	}

	private int knownMatching() {
		if (minecraft.player == null) {
			return 0;
		}
		int n = 0;
		for (RuneDef rune : known()) {
			if (matches(rune)) {
				n++;
			}
		}
		return n;
	}

	private static int familyColor(RuneFamily family) {
		return switch (family) {
			case SHAPE -> RuneColors.SHAPE;
			case EFFECT -> 0xF06E32;
			case MODIFIER -> RuneColors.MODIFIER;
			case LINK -> RuneColors.LINK;
			case KNOT -> RuneColors.KNOT;
		};
	}

	// ------------------------------------------------------------------ drawing

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	/** Draws text cut to {@code maxWidth} with an ellipsis if it would not fit. */
	private void fitText(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
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

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		layout();
		super.extractRenderState(g, mouseX, mouseY, a);
		drawSideCircle(g, a);
		float s = scale();
		int mx = (int) Math.floor(localX(mouseX));
		int my = (int) Math.floor(localY(mouseY));
		g.pose().pushMatrix();
		g.pose().translate(left(), top());
		g.pose().scale(s, s);
		List<Component> tooltip = draw(g, mx, my);
		g.pose().popMatrix();
		// The dragged rune follows the real mouse, drawn at full size above everything.
		if (dragging) {
			String id = draggedId();
			if (id != null) {
				g.item(RuneItem.stack(id), mouseX - 8, mouseY - 8);
			}
		}
		if (tooltip != null) {
			g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	/** The spell whose circle is showing beside the window, and when it started opening. */
	private List<String> circleShown = List.of();
	private long circleOpened;

	/**
	 * Beside the window, when there's room: the magic circle of the spell being edited, opening again
	 * whenever it changes. On the Grimoire page, the circles of the secret spells found so far, one
	 * after another, each with its name like a plate in a book.
	 */
	private void drawSideCircle(GuiGraphicsExtractor g, float partial) {
		CordTier tier = tier();
		float room = Math.min(left() - 12, height * 0.36F * 2);
		if (tier == null || minecraft.player == null || room < 90) {
			return;
		}
		float radius = Math.min(room / 2 - 6, 78);
		float cx = left() + W * scale() + 8 + radius;
		float cy = top() + H * scale() / 2;
		if (cx + radius > width - 4) {
			return;
		}
		long now = minecraft.player.level().getGameTime();
		List<String> ids;
		Component caption = null;
		if (grimoirePage) {
			List<dev.wildercord.spell.Secrets.Secret> found = dev.wildercord.spell.Secrets.ALL.stream()
				.filter(s -> Heart.discovered(minecraft.player, s.key())).toList();
			if (found.isEmpty()) {
				return;
			}
			dev.wildercord.spell.Secrets.Secret plate = found.get((int) ((now / 100) % found.size()));
			ids = plate.runes().stream().map(RuneDef::id).toList();
			caption = Component.literal(plate.name()).withColor(0xFF000000 | plate.color());
		} else if (!passivePage && editing < spells.size()) {
			ids = spells.get(editing);
		} else {
			return;
		}
		if (!ids.equals(circleShown)) {
			circleShown = List.copyOf(ids);
			circleOpened = now;
		}
		List<RuneDef> runes = new ArrayList<>();
		for (String id : ids) {
			dev.wildercord.spell.Runes.get(id).ifPresent(runes::add);
		}
		float time = (now + partial) / 20F;
		float open = Math.min(1, (now - circleOpened + partial) / 14F);
		GuiSpellCircle.draw(g, cx, cy, radius, runes, time, open);
		if (caption != null) {
			g.centeredText(font, caption, Math.round(cx), Math.round(cy + radius + 8), 0xFFFFFFFF);
		}
	}

	/** Draws the window in local coordinates and returns the tooltip under the mouse, if any. */
	private List<Component> draw(GuiGraphicsExtractor g, int mx, int my) {
		sprite(g, SPR_PANEL, 0, 0, W, H);
		CordTier tier = tier();
		if (tier == null) {
			g.centeredText(font, Component.translatable("screen.wildercord.no_cord"), W / 2, H / 2 - 10, TEXT);
			g.centeredText(font, Component.translatable("screen.wildercord.no_cord_hint"), W / 2, H / 2 + 4, DIM);
			return null;
		}
		List<Component> tooltip = null;

		// Header: Cord name left, its limits right, mana and help badges in the corner.
		int helpX = W - 27;
		int helpY = 7;
		sprite(g, SPR_BADGE, helpX, helpY, 14, 14);
		g.text(font, "?", helpX + 7 - font.width("?") / 2, helpY + 3, GOLD, true);
		if (inside(mx, my, helpX, helpY, 14, 14)) {
			tooltip = helpTooltip();
		}
		int manaX = helpX - 18;
		sprite(g, SPR_BADGE, manaX, helpY, 14, 14);
		sprite(g, SPR_MANA, manaX + 1, helpY + 1, 12, 12);
		if (inside(mx, my, manaX, helpY, 14, 14)) {
			tooltip = manaTooltip();
		}
		// The heart: its circles, their bonuses, and what the next one needs.
		int heartX = manaX - 18;
		int circles = Heart.circles(minecraft.player);
		sprite(g, SPR_BADGE, heartX, helpY, 14, 14);
		sprite(g, SPR_HEART, heartX + 1, helpY + 1, 12, 12);
		if (circles > 0) {
			String count = Integer.toString(circles);
			g.text(font, count, heartX + 14 - font.width(count), helpY + 7, 0xFFFFF3D0, true);
		}
		if (Heart.ready(minecraft.player) && (System.currentTimeMillis() / 400) % 2 == 0) {
			g.fill(heartX + 11, helpY + 1, heartX + 13, helpY + 3, 0xFFF5C46A);
		}
		if (inside(mx, my, heartX, helpY, 14, 14)) {
			tooltip = heartTooltip();
		}
		Component name = Component.translatable(tier.itemKey());
		Component spellsLabel = Component.translatable(tier.spells == 1 ? "screen.wildercord.spells.one" : "screen.wildercord.spells.many", tier.spells);
		Component stats = Component.translatable("screen.wildercord.stats", tier.sockets, spellsLabel, RuneItem.roman(tier.maxRuneTier));
		g.text(font, name, 13, 10, GOLD, true);
		if (inside(mx, my, 13, 8, font.width(name), 10)) {
			tooltip = List.of(name.copy().withColor(GOLD), stats.copy().withStyle(ChatFormatting.GRAY));
		}
		// Spells | Passives | Grimoire
		int pageX = 13 + font.width(name) + 8;
		for (int page = 0; page < PAGE_KEYS.length; page++) {
			Component label = Component.translatable(PAGE_KEYS[page]);
			int w = font.width(label) + 10;
			boolean active = page() == page;
			sprite(g, active ? SPR_TAB_ACTIVE : SPR_TAB, pageX, 7, w, 13);
			g.text(font, label, pageX + 5, 10, active ? GOLD : inside(mx, my, pageX, 7, w, 13) ? TEXT : DIM, false);
			pageX += w + 2;
		}
		int statsRight = heartX - 6;
		int statsW = font.width(stats);
		if (pageX + 8 + statsW <= statsRight) {
			g.text(font, stats, statsRight - statsW, 10, DIM, false);
		}

		if (grimoirePage) {
			List<Component> tip = drawGrimoire(g, mx, my);
			return tip != null ? tip : tooltip;
		}
		if (passivePage) {
			for (int s = 0; s < Passives.MAX; s++) {
				List<Component> rowTip = drawPassiveRow(g, s, SPELL_TOP + s * SPELL_ROW, mx, my, tier);
				if (rowTip != null) {
					tooltip = rowTip;
				}
			}
			drawPassiveSummary(g, SPELL_TOP + Passives.MAX * SPELL_ROW);
		} else {
			for (int s = 0; s < rowCount(); s++) {
				List<Component> rowTip = drawSpellRow(g, s, SPELL_TOP + s * SPELL_ROW, mx, my, tier);
				if (rowTip != null) {
					tooltip = rowTip;
				}
			}
		}

		// Family tabs, then the search box.
		int tx = 12;
		for (int t = 0; t < TAB_KEYS.length; t++) {
			Component label = Component.translatable("screen.wildercord.tab." + TAB_KEYS[t]);
			int w = font.width(label) + 18;
			boolean active = filter == TAB_FAMILIES[t];
			sprite(g, active ? SPR_TAB_ACTIVE : SPR_TAB, tx, TABS_TOP, w, 13);
			sprite(g, Wildercord.id("cord/gem_" + TAB_KEYS[t]), tx + 4, TABS_TOP + 4, 5, 5);
			g.text(font, label, tx + 12, TABS_TOP + 3, active ? GOLD : DIM, false);
			tx += w + 2;
		}
		drawSearch(g, searchX(), TABS_TOP);

		// Category chips for the chosen family, and the match count.
		drawChips(g, mx, my);

		// The Codex, grouped under category headers.
		List<CodexRow> rows = codexRows();
		int total = 0;
		for (CodexRow row : rows) {
			total += row.height();
		}
		int maxScroll = Math.max(0, total - CODEX_HEIGHT);
		codexScroll = Math.max(0, Math.min(codexScroll, maxScroll));
		sprite(g, SPR_INSET, 10, CODEX_TOP - 3, W - 20, CODEX_HEIGHT + 6);
		g.enableScissor(11, CODEX_TOP - 1, W - 11, CODEX_BOTTOM + 1);
		int y = CODEX_TOP - codexScroll;
		for (CodexRow row : rows) {
			int h = row.height();
			if (y + h >= CODEX_TOP - 1 && y <= CODEX_BOTTOM) {
				if (row.label() != null) {
					// A family-coloured tick and the category name.
					g.fill(CODEX_X, y + 3, CODEX_X + 2, y + 15, row.color());
					fitText(g, row.label(), CODEX_X + 5, y + 5, LABEL_W - 8, row.color(), false);
					g.fill(CODEX_X, y, W - 22, y + 1, (row.color() & 0x00FFFFFF) | 0x28000000);
				}
				{
					for (int i = 0; i < row.runes().size(); i++) {
						RuneDef rune = row.runes().get(i);
						int cx = CODEX_X + LABEL_W + i * CELL;
						boolean usable = fitsPage(rune);
						boolean hovered = inside(mx, my, cx, y, CELL, CELL) && my >= CODEX_TOP && my < CODEX_BOTTOM;
						if (hovered && usable) {
							sprite(g, SPR_SOCKET_HOVER, cx, y, CELL, CELL);
						}
						g.item(RuneItem.stack(rune), cx + 1, y + 1);
						if (!usable) {
							g.fill(cx + 1, y + 1, cx + 17, y + 17, 0xB0100C18);
							sprite(g, SPR_LOCK, cx + 10, y + 9, 7, 8);
						}
						if (hovered && !dragging) {
							Component hint = usable
								? Component.translatable("screen.wildercord.codex_hint")
								: !holds(rune)
									? Component.translatable("screen.wildercord.needs_cord", Component.translatable(CordTier.forRuneTier(rune.tier()).itemKey()), RuneItem.roman(rune.tier()))
									: Component.translatable("screen.wildercord.not_sustainable");
							tooltip = runeTooltip(rune.id(), hint, !usable);
						}
					}
				}
			}
			y += h;
		}
		g.disableScissor();
		if (rows.isEmpty()) {
			Component empty = query.isBlank() ? Component.translatable("screen.wildercord.codex_empty") : Component.translatable("screen.wildercord.no_results", query.trim());
			fitText(g, empty, CODEX_X + 4, CODEX_TOP + 30, W - 40, DIM, false);
		}
		if (maxScroll > 0) {
			int thumb = Math.max(10, CODEX_HEIGHT * CODEX_HEIGHT / total);
			int ty = CODEX_TOP + (CODEX_HEIGHT - thumb) * codexScroll / maxScroll;
			sprite(g, SPR_SCROLLER, W - 16, ty, 4, thumb);
		}

		drawReadout(g, tier);
		if (!passivePage) {
			List<Component> toolTip = drawSpellTools(g, mx, my);
			if (toolTip != null) {
				tooltip = toolTip;
			}
		}
		return tooltip;
	}

	private int searchX() {
		return W - 12 - SEARCH_W;
	}

	private void drawSearch(GuiGraphicsExtractor g, int x, int y) {
		sprite(g, searchFocused ? SPR_TAB_ACTIVE : SPR_TAB, x, y, SEARCH_W, 13);
		// Magnifier glyph.
		int gx = x + 4;
		int gy = y + 3;
		int glyph = searchFocused ? GOLD : DIM;
		g.fill(gx + 1, gy, gx + 4, gy + 1, glyph);
		g.fill(gx, gy + 1, gx + 1, gy + 4, glyph);
		g.fill(gx + 4, gy + 1, gx + 5, gy + 4, glyph);
		g.fill(gx + 1, gy + 4, gx + 4, gy + 5, glyph);
		g.fill(gx + 4, gy + 5, gx + 6, gy + 7, glyph);
		int textX = x + 12;
		int textW = SEARCH_W - 16;
		if (query.isEmpty() && !searchFocused) {
			fitText(g, Component.translatable("screen.wildercord.search"), textX, y + 3, textW, FAINT, false);
			return;
		}
		// Show the end of the query if it's longer than the box.
		String shown = font.plainSubstrByWidth(query, textW - 4, true);
		g.text(font, shown, textX, y + 3, TEXT, false);
		if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
			int cx = textX + font.width(shown) + 1;
			g.fill(cx, y + 2, cx + 1, y + 11, GOLD);
		}
	}

	/** Category chips for the chosen family, left to right; the match count only where there's room. */
	private void drawChips(GuiGraphicsExtractor g, int mx, int my) {
		Component countText = Component.translatable("screen.wildercord.matches", knownMatching());
		int countW = font.width(countText);
		int right = W - 14;
		int x = 12;
		if (filter == null) {
			int hintW = right - countW - 8 - 14;
			fitText(g, Component.translatable("screen.wildercord.chips_hint"), 14, CHIPS_TOP + 2, hintW, FAINT, false);
		} else {
			for (Chip chip : chips()) {
				if (x + chip.width() > right) {
					break;
				}
				boolean active = Objects.equals(category, chip.key());
				g.fill(x, CHIPS_TOP, x + chip.width(), CHIPS_TOP + 12, active ? 0xFF4A3C70 : 0xFF221D32);
				g.fill(x, CHIPS_TOP + 11, x + chip.width(), CHIPS_TOP + 12, active ? GOLD : 0xFF3A3052);
				boolean hovered = inside(mx, my, x, CHIPS_TOP, chip.width(), 12);
				g.text(font, chip.label(), x + 4, CHIPS_TOP + 2, active ? GOLD : hovered ? TEXT : DIM, false);
				x += chip.width() + CHIP_GAP;
			}
		}
		if (filter == null || x + 6 + countW <= right) {
			g.text(font, countText, right - countW, CHIPS_TOP + 2, FAINT, false);
		}
	}

	private static final int CHIP_GAP = 2;

	private record Chip(String key, Component label, int width) {}

	private List<Chip> chips() {
		List<Chip> out = new ArrayList<>();
		if (filter == null) {
			return out;
		}
		Component all = Component.translatable("screen.wildercord.chip.all");
		out.add(new Chip(null, all, font.width(all) + 8));
		// Only categories you have learned something in: no empty Summon chip before your first summon.
		java.util.Set<String> learned = new java.util.HashSet<>();
		if (minecraft.player != null) {
			for (RuneDef rune : known()) {
				if (rune.family() == filter) {
					learned.add(rune.category());
				}
			}
		}
		for (String key : RuneCategories.of(filter)) {
			if (!learned.contains(key)) {
				continue;
			}
			Component label = Component.translatable("category.wildercord." + familyKey(filter) + "." + key);
			out.add(new Chip(key, label, font.width(label) + 8));
		}
		if (category != null && !learned.contains(category)) {
			category = null;
		}
		return out;
	}

	/** Draws one spell row and returns a tooltip if the mouse is over something in it. */
	private List<Component> drawSpellRow(GuiGraphicsExtractor g, int s, int ry, int mx, int my, CordTier tier) {
		List<Component> tooltip = null;
		boolean unlocked = spellOpen(s);
		boolean selected = s == editing && unlocked;
		// The tome's row wears the tome's violet.
		boolean tomeSlot = s == dev.wildercord.gear.SpellSlots.TOME;
		sprite(g, selected ? SPR_ROW_SELECTED : unlocked ? SPR_ROW : SPR_ROW_LOCKED, 10, ry - 2, W - 20, SPELL_ROW);
		g.text(font, Integer.toString(s + 1), 19, ry + 5, selected ? GOLD : unlocked ? tomeSlot ? LAVENDER : TEXT : 0xFF4A4460, true);
		if (unlocked && tomeSlot && inside(mx, my, 10, ry - 2, SOCKET_X - 12, SPELL_ROW)) {
			tooltip = List.of(Component.translatable("item.wildercord.tome_of_the_fifth_page").withColor(LAVENDER),
				Component.translatable("screen.wildercord.tome_row").withStyle(ChatFormatting.GRAY));
		}
		List<String> spell = spells.get(s);
		if (!unlocked) {
			sprite(g, SPR_LOCK, SOCKET_X + 2, ry + 5, 7, 8);
			Component needs = Component.translatable("screen.wildercord.row_locked", Component.translatable(CordTier.forSpells(s + 1).itemKey()));
			int right = W - 16;
			if (!spell.isEmpty()) {
				Component kept = Component.translatable("screen.wildercord.row_kept", spell.size());
				int keptW = font.width(kept);
				g.text(font, kept, right - keptW, ry + 5, FAINT, false);
				right -= keptW + 8;
			}
			fitText(g, needs, SOCKET_X + 12, ry + 5, right - (SOCKET_X + 12), FAINT, false);
			return null;
		}

		List<Integer> active = SpellCaster.activeSockets(spell, book(), s, tier);
		SpellCompiler.Compiled compiled = selected ? SpellCompiler.compile(runesAt(spell, active)) : null;
		int shown = Math.min(CordTier.MAX_SOCKETS, Math.max(tier.sockets, spell.size()));
		// The cord itself, threaded through every socket.
		sprite(g, SPR_THREAD, 28, ry + 7, SOCKET_X - 28 + (shown - 1) * PITCH + CELL / 2, 4);
		for (int i = 0; i < shown; i++) {
			int sx = SOCKET_X + i * PITCH;
			boolean open = i < tier.sockets;
			boolean hover = open && inside(mx, my, sx, ry, CELL, CELL);
			sprite(g, !open ? SPR_SOCKET_QUIET : hover ? SPR_SOCKET_HOVER : SPR_SOCKET, sx, ry, CELL, CELL);
			if (i >= spell.size()) {
				continue;
			}
			String id = spell.get(i);
			if (!(dragging && pressedSpell == s && pressedSocket == i)) {
				g.item(RuneItem.stack(id), sx + 1, ry + 1);
			}
			Component quiet = quietReason(id, i);
			if (quiet != null) {
				g.fill(sx + 3, ry + 3, sx + 15, ry + 15, 0x9A1A0810);
				g.fill(sx + 13, ry + 2, sx + 16, ry + 5, QUIET);
			}
			if (compiled != null && active.contains(i)) {
				drawAttachment(g, compiled, active, active.indexOf(i), ry);
			}
			if (inside(mx, my, sx, ry, CELL, CELL) && !dragging) {
				tooltip = runeTooltip(id, quiet != null ? quiet : Component.translatable("screen.wildercord.socket_hint"), quiet != null);
			}
		}
		if (spell.size() <= tier.sockets && tier.sockets < CordTier.MAX_SOCKETS) {
			Component more = Component.translatable("screen.wildercord.more_sockets");
			int hintX = SOCKET_X + tier.sockets * PITCH + 4;
			if (hintX + font.width(more) <= W - 16) {
				g.text(font, more, hintX, ry + 5, 0xFF4A4460, false);
			}
		}
		return tooltip;
	}

	/** One passive row: its runes, an upkeep readout and an on/off switch. */
	private List<Component> drawPassiveRow(GuiGraphicsExtractor g, int s, int ry, int mx, int my, CordTier tier) {
		List<Component> tooltip = null;
		boolean unlocked = s < openRows();
		boolean selected = s == editingPassive && unlocked;
		sprite(g, selected ? SPR_ROW_SELECTED : unlocked ? SPR_ROW : SPR_ROW_LOCKED, 10, ry - 2, W - 20, SPELL_ROW);
		g.text(font, Integer.toString(s + 1), 19, ry + 5, selected ? LAVENDER : unlocked ? 0xFF9A8CD8 : 0xFF4A4460, true);
		List<String> passive = passives.get(s);
		if (!unlocked) {
			sprite(g, SPR_LOCK, SOCKET_X + 2, ry + 5, 7, 8);
			Component needs = Component.translatable("screen.wildercord.passive_locked", Circles.ordinal(Passives.circleFor(s)));
			fitText(g, needs, SOCKET_X + 12, ry + 5, W - 16 - (SOCKET_X + 12), FAINT, false);
			return null;
		}
		int sockets = PassiveCaster.sockets(tier);
		List<Integer> active = passiveSockets(passive, tier);
		SpellCompiler.Compiled compiled = active.isEmpty() ? null : SpellCompiler.compile(runesAt(passive, active));
		int shown = Math.min(Passives.SOCKETS, Math.max(sockets, passive.size()));
		sprite(g, SPR_THREAD, 28, ry + 7, SOCKET_X - 28 + (shown - 1) * PITCH + CELL / 2, 4);
		for (int i = 0; i < shown; i++) {
			int sx = SOCKET_X + i * PITCH;
			boolean open = i < sockets;
			boolean hover = open && inside(mx, my, sx, ry, CELL, CELL);
			sprite(g, !open ? SPR_SOCKET_QUIET : hover ? SPR_SOCKET_HOVER : SPR_SOCKET, sx, ry, CELL, CELL);
			if (i >= passive.size()) {
				continue;
			}
			String id = passive.get(i);
			if (!(dragging && pressedSpell == s && pressedSocket == i)) {
				g.item(RuneItem.stack(id), sx + 1, ry + 1);
			}
			Component quiet = passiveQuietReason(id, i);
			if (quiet != null) {
				g.fill(sx + 3, ry + 3, sx + 15, ry + 15, 0x9A1A0810);
				g.fill(sx + 13, ry + 2, sx + 16, ry + 5, QUIET);
			}
			if (selected && compiled != null && active.contains(i)) {
				drawAttachment(g, compiled, active, active.indexOf(i), ry);
			}
			if (inside(mx, my, sx, ry, CELL, CELL) && !dragging) {
				tooltip = runeTooltip(id, quiet != null ? quiet : Component.translatable("screen.wildercord.socket_hint"), quiet != null);
			}
		}
		// The switch, and what the passive costs to keep up.
		boolean on = book().passiveOn(s);
		int switchW = 28;
		int switchX = W - 16 - switchW;
		boolean switchHover = inside(mx, my, switchX, ry + 2, switchW, 13);
		sprite(g, on ? SPR_TAB_ACTIVE : SPR_TAB, switchX, ry + 2, switchW, 13);
		Component state = Component.translatable(on ? "screen.wildercord.passive.on" : "screen.wildercord.passive.off");
		g.text(font, state, switchX + switchW / 2 - font.width(state) / 2, ry + 5, on ? CYAN : switchHover ? TEXT : DIM, false);
		if (switchHover) {
			tooltip = List.of(Component.translatable("screen.wildercord.passive.switch").withStyle(ChatFormatting.GRAY));
		}
		String problem = compiled == null ? null : Passives.problem(runesAt(passive, active));
		Component status = compiled == null ? null
			: problem != null ? Component.literal("!")
			: Component.translatable("screen.wildercord.passive.upkeep", String.format(Locale.ROOT, "%.1f", Heart.upkeep(minecraft.player, compiled)));
		if (status != null) {
			int statusW = font.width(status);
			int statusX = switchX - 6 - statusW;
			if (statusX > SOCKET_X + shown * PITCH) {
				g.text(font, status, statusX, ry + 5, problem != null ? WARN : !on ? FAINT : LAVENDER, false);
				if (inside(mx, my, statusX, ry + 3, statusW, 10)) {
					tooltip = problem != null
						? List.of(Component.literal(problem).withStyle(ChatFormatting.GOLD))
						: List.of(Component.translatable("screen.wildercord.passive.upkeep_hint").withStyle(ChatFormatting.GRAY));
				}
			}
		}
		return tooltip;
	}

	/** Socket positions of a passive whose runes will run. */
	private List<Integer> passiveSockets(List<String> passive, CordTier tier) {
		List<Integer> sockets = new ArrayList<>();
		for (int i = 0; i < Math.min(passive.size(), PassiveCaster.sockets(tier)); i++) {
			if (passiveQuietReason(passive.get(i), i) == null) {
				sockets.add(i);
			}
		}
		return sockets;
	}

	private Component passiveQuietReason(String id, int socket) {
		CordTier tier = tier();
		if (tier != null && socket >= PassiveCaster.sockets(tier)) {
			return Component.translatable("screen.wildercord.quiet_passive_socket", PassiveCaster.sockets(tier));
		}
		Component reason = quietReason(id, 0);
		if (reason != null) {
			return reason;
		}
		return Passives.allowed(Runes.get(id).orElseThrow()) ? null : Component.translatable("screen.wildercord.not_sustainable");
	}

	/** Under the passive rows: the total upkeep against your regeneration. */
	private void drawPassiveSummary(GuiGraphicsExtractor g, int ry) {
		float upkeep = passiveUpkeep(minecraft.player);
		float regen = Mana.of(minecraft.player).regen();
		Component text = Component.translatable("screen.wildercord.passive.summary", String.format(Locale.ROOT, "%.1f", upkeep),
			String.format(Locale.ROOT, "%.1f", regen));
		fitText(g, text, 14, ry + 4, W - 28, upkeep > regen ? WARN : DIM, false);
	}

	/** Mana per second all running passives cost (open, switched on and valid). */
	static float passiveUpkeep(Player player) {
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return 0;
		}
		Spellbook book = Spellbooks.get(player);
		float total = 0;
		for (int s = 0; s < Passives.slots(Heart.active(player)); s++) {
			if (!book.passiveOn(s)) {
				continue;
			}
			List<RuneDef> runes = PassiveCaster.activeRunes(book.passives().get(s), book, tier);
			if (!runes.isEmpty() && Passives.problem(runes) == null) {
				SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
				if (!compiled.isEmpty()) {
					total += Heart.upkeep(player, compiled);
				}
			}
		}
		return total;
	}

	private static List<RuneDef> runesAt(List<String> spell, List<Integer> sockets) {
		List<RuneDef> runes = new ArrayList<>();
		for (int socket : sockets) {
			runes.add(Runes.get(spell.get(socket)).orElseThrow());
		}
		return runes;
	}

	/** Which rune a modifier attached to: a gold line under the pair, or a red mark if it attaches to nothing. */
	private void drawAttachment(GuiGraphicsExtractor g, SpellCompiler.Compiled compiled, List<Integer> active, int k, int ry) {
		int target = compiled.attachedTo()[k];
		if (target == SpellCompiler.NOT_A_MODIFIER) {
			return;
		}
		int from = SOCKET_X + active.get(k) * PITCH + CELL / 2;
		int y = ry + CELL;
		if (target == SpellCompiler.UNATTACHED) {
			g.fill(from - 2, y - 3, from + 3, y - 1, 0xFFE04040);
			return;
		}
		int to = SOCKET_X + active.get(target) * PITCH + CELL / 2;
		g.fill(Math.min(from, to), y + 1, Math.max(from, to) + 1, y + 2, GOLD);
		sprite(g, SPR_ATTACH, from - 1, y, 3, 3);
		sprite(g, SPR_ATTACH, to - 1, y, 3, 3);
	}

	private record ReadoutLine(FormattedCharSequence text, int x, int color) {}

	/** The selected spell explained: wrapped to the box, clipped to it, and scrollable. */
	private void drawReadout(GuiGraphicsExtractor g, CordTier tier) {
		int boxTop = READOUT_TOP - 3;
		sprite(g, SPR_INSET, 10, boxTop, W - 20, READOUT_BOTTOM + 3 - boxTop);
		List<ReadoutLine> lines = readoutLines(tier);
		int visible = Math.max(1, (READOUT_BOTTOM - READOUT_TOP) / LINE);
		readoutScroll = Math.max(0, Math.min(readoutScroll, lines.size() - visible));
		g.enableScissor(12, READOUT_TOP - 1, W - 12, READOUT_BOTTOM);
		for (int i = 0; i < visible && readoutScroll + i < lines.size(); i++) {
			ReadoutLine line = lines.get(readoutScroll + i);
			g.text(font, line.text(), line.x(), READOUT_TOP + i * LINE, line.color(), false);
		}
		g.disableScissor();
		int ax = W - 18;
		if (readoutScroll > 0) {
			arrow(g, ax, READOUT_TOP + 1, true);
		}
		if (readoutScroll + visible < lines.size()) {
			arrow(g, ax, READOUT_BOTTOM - 4, false);
		}
	}

	private List<ReadoutLine> readoutLines(CordTier tier) {
		List<ReadoutLine> out = new ArrayList<>();
		int width = TEXT_RIGHT - TEXT_X;
		if (passivePage) {
			refusal(out, width);
			return passiveReadout(tier, out, width);
		}
		List<String> spell = spells.get(editing);
		List<RuneDef> runes = runesAt(spell, SpellCaster.activeSockets(spell, book(), editing, tier));
		if (runes.isEmpty()) {
			wrap(out, Component.translatable("screen.wildercord.empty_spell"), 0, width, DIM);
			refusal(out, width);
			return out;
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes, RuneRanks.lookup(minecraft.player));
		// The name line, with room left for the tool buttons on its right.
		java.util.Optional<dev.wildercord.spell.Secrets.Secret> secret = Heart.foundSecret(minecraft.player, runes);
		boolean knownSecret = secret.isPresent();
		String spellName = renaming ? renameText + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : " ")
			: SpellCaster.nameOf(minecraft.player, book(), editing, runes);
		int nameColor = renaming ? TEXT : knownSecret ? 0xFF000000 | secret.get().color() : GOLD;
		out.add(new ReadoutLine(Component.literal(font.plainSubstrByWidth(spellName, width - TOOLS_W - 6)).getVisualOrderText(), TEXT_X, nameColor));
		refusal(out, width);
		if (knownSecret && !renaming) {
			wrap(out, Component.translatable("screen.wildercord.secret_line", secret.get().description()), 0, width, 0xFF000000 | secret.get().color());
		}
		int maxMana = Mana.max(minecraft.player);
		// A secret spell you've found costs and recharges as one (before that, as the ordinary spell).
		double secretCost = Heart.secretCost(minecraft.player, runes);
		int manaCost = Heart.manaCost(minecraft.player, compiled, secretCost);
		int healthCost = Heart.healthCost(minecraft.player, compiled, secretCost);
		String cooldown = String.format(Locale.ROOT, "%.1f", Heart.cooldownTicks(minecraft.player, compiled, Heart.secretCooldown(minecraft.player, runes)) / 20.0);
		boolean tooCostly = compiled.paysInHealth() ? healthCost >= minecraft.player.getMaxHealth() : manaCost > maxMana;
		Component header = compiled.paysInHealth()
			? Component.translatable("screen.wildercord.cost_health", healthCost, cooldown)
			: Component.translatable("screen.wildercord.cost", manaCost, cooldown, maxMana);
		wrap(out, header, 0, width, tooCostly ? QUIET : CYAN);
		gearLines(out, compiled, width);
		for (String text : compiled.lines()) {
			int spaces = 0;
			while (spaces < text.length() && text.charAt(spaces) == ' ') {
				spaces++;
			}
			wrap(out, Component.literal(text.substring(spaces)), font.width(text.substring(0, spaces)), width, TEXT);
		}
		if (tooCostly) {
			wrap(out, compiled.paysInHealth()
				? Component.translatable("screen.wildercord.too_costly_health", (int) minecraft.player.getMaxHealth())
				: Component.translatable("screen.wildercord.too_costly", maxMana), 0, width, WARN);
		}
		for (String warning : compiled.warnings()) {
			wrap(out, Component.literal("! " + warning), 0, width, WARN);
		}
		return out;
	}

	/** Casting gear in hand that changes this spell, and the server's cost multiplier (the cost above includes both). */
	private void gearLines(List<ReadoutLine> out, SpellCompiler.Compiled compiled, int width) {
		dev.wildercord.gear.GearBonuses gear = dev.wildercord.gear.Gear.of(minecraft.player);
		java.util.Set<String> elements = dev.wildercord.gear.GearBonuses.elements(compiled.root());
		for (dev.wildercord.gear.GearDef piece : gear.pieces()) {
			boolean matters = piece.kind().staff() ? elements.contains(piece.element())
				: piece.cost() != 1 || piece.power() != 1 || piece.echo() > 0 || piece.chargeSpeed() != 1;
			if (matters) {
				wrap(out, Component.translatable("screen.wildercord.gear.readout", Component.translatable(dev.wildercord.gear.Gear.itemKey(piece)),
					dev.wildercord.gear.Gear.effect(piece)), 0, width, LAVENDER);
			}
		}
		double server = dev.wildercord.config.Config.costMultiplier(minecraft.player);
		if (Math.abs(server - 1) > 1e-6) {
			wrap(out, Component.translatable("screen.wildercord.server_cost", String.format(Locale.ROOT, "%.2f", server)), 0, width, DIM);
		}
	}

	private List<ReadoutLine> passiveReadout(CordTier tier, List<ReadoutLine> out, int width) {
		if (editingPassive >= openRows()) {
			wrap(out, Component.translatable("screen.wildercord.passive_locked", Circles.ordinal(Passives.circleFor(editingPassive))), 0, width, DIM);
			wrap(out, Component.translatable("screen.wildercord.passive.locked_hint"), 0, width, DIM);
			return out;
		}
		List<String> passive = passives.get(editingPassive);
		List<RuneDef> runes = runesAt(passive, passiveSockets(passive, tier));
		if (runes.isEmpty()) {
			wrap(out, Component.translatable("screen.wildercord.passive.empty"), 0, width, DIM);
			wrap(out, Component.translatable("screen.wildercord.passive.rules"), 0, width, FAINT);
			return out;
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes, RuneRanks.lookup(minecraft.player));
		String problem = Passives.problem(runes);
		boolean on = book().passiveOn(editingPassive);
		Component header = Component.translatable(on ? "screen.wildercord.passive.header" : "screen.wildercord.passive.header_off",
			String.format(Locale.ROOT, "%.1f", Heart.upkeep(minecraft.player, compiled)),
			String.format(Locale.ROOT, "%.0f", Passives.interval(compiled.root()) / 20.0));
		wrap(out, header, 0, width, problem != null ? QUIET : on ? CYAN : DIM);
		for (String text : compiled.lines()) {
			int spaces = 0;
			while (spaces < text.length() && text.charAt(spaces) == ' ') {
				spaces++;
			}
			wrap(out, Component.literal(text.substring(spaces)), font.width(text.substring(0, spaces)), width, TEXT);
		}
		if (problem != null) {
			wrap(out, Component.literal("! " + problem), 0, width, WARN);
		}
		for (String warning : compiled.warnings()) {
			wrap(out, Component.literal("! " + warning), 0, width, WARN);
		}
		return out;
	}

	/** Why the last click did nothing, for a few seconds after it (under the spell's name). */
	private void refusal(List<ReadoutLine> out, int width) {
		if (refused != null && net.minecraft.util.Util.getMillis() < refusedUntil) {
			wrap(out, refused, 0, width, WARN);
		}
	}

	/** Wraps text into lines; continuation lines are indented a little further. */
	private void wrap(List<ReadoutLine> out, Component text, int indent, int width, int color) {
		List<FormattedCharSequence> pieces = font.split(FormattedText.of(text.getString(), text.getStyle()), Math.max(20, width - indent));
		for (int i = 0; i < pieces.size(); i++) {
			out.add(new ReadoutLine(pieces.get(i), TEXT_X + indent + (i > 0 ? 6 : 0), color));
		}
	}

	private static void arrow(GuiGraphicsExtractor g, int x, int y, boolean up) {
		for (int row = 0; row < 3; row++) {
			int half = up ? row : 2 - row;
			g.fill(x + 2 - half, y + row, x + 3 + half, y + row + 1, GOLD);
		}
	}

	/** Where your mana comes from, and every way to grow it. */
	private List<Component> manaTooltip() {
		Mana.Stats stats = Mana.of(minecraft.player);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.wildercord.mana.title").withColor(0xB8A8FF));
		lines.add(Component.translatable("screen.wildercord.mana.max", stats.max()).withStyle(ChatFormatting.WHITE));
		lines.add(Component.translatable("screen.wildercord.mana.max_cord", stats.tier().maxMana, Component.translatable(stats.tier().itemKey())).withStyle(ChatFormatting.GRAY));
		if (stats.crystals() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.max_crystals", stats.crystals() * Mana.CRYSTAL_MANA, stats.crystals()).withStyle(ChatFormatting.GRAY));
		}
		if (stats.reservoir() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.max_reservoir", stats.reservoir() * Mana.RESERVOIR_MANA, RuneItem.roman(stats.reservoir())).withStyle(ChatFormatting.GRAY));
		}
		if (stats.circles() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.max_circles", stats.circles() * Circles.MANA_PER_CIRCLE, stats.circles()).withStyle(ChatFormatting.GRAY));
		}
		int gearMana = dev.wildercord.gear.Gear.extraMana(minecraft.player);
		if (gearMana > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.max_gear", gearMana).withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("screen.wildercord.mana.regen", String.format(Locale.ROOT, "%.1f", stats.regen())).withStyle(ChatFormatting.WHITE));
		lines.add(Component.translatable("screen.wildercord.mana.regen_cord", stats.tier().regenPerSecond).withStyle(ChatFormatting.GRAY));
		if (stats.circles() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_circles", String.format(Locale.ROOT, "%.1f", stats.circles() * Circles.REGEN_PER_CIRCLE)).withStyle(ChatFormatting.GRAY));
		}
		float upkeep = passiveUpkeep(minecraft.player);
		if (upkeep > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.upkeep", String.format(Locale.ROOT, "%.1f", upkeep)).withStyle(ChatFormatting.GRAY));
		}
		if (stats.wellspring() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_wellspring", Math.round(stats.wellspring() * Mana.WELLSPRING_BONUS * 100), RuneItem.roman(stats.wellspring())).withStyle(ChatFormatting.GRAY));
		}
		if (stats.clarity() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_clarity", Math.round(stats.clarity() * Mana.CLARITY_BONUS * 100)).withStyle(ChatFormatting.GRAY));
		}
		if (stats.meditating()) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_meditation", Math.round(Mana.MEDITATION_BONUS * 100)).withStyle(ChatFormatting.GRAY));
		}
		if (stats.ley()) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_ley", Math.round(Mana.LEY_BONUS * 100)).withColor(0xB8A0FF));
		}
		if (stats.well()) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_well", Math.round(Mana.WELL_BONUS * 100)).withColor(0xB8A0FF));
		}
		if (stats.siphon() > 0) {
			lines.add(Component.translatable("screen.wildercord.mana.siphon", RuneItem.roman(stats.siphon()), stats.siphon() * Mana.SIPHON_MANA).withStyle(ChatFormatting.WHITE));
		}
		double serverRegen = dev.wildercord.config.Config.regenMultiplier(minecraft.player);
		if (Math.abs(serverRegen - 1) > 1e-6) {
			lines.add(Component.translatable("screen.wildercord.mana.regen_server", String.format(Locale.ROOT, "%.2f", serverRegen)).withStyle(ChatFormatting.GRAY));
		}
		dev.wildercord.gear.GearBonuses gear = dev.wildercord.gear.Gear.of(minecraft.player);
		if (!gear.isEmpty()) {
			lines.add(Component.translatable("screen.wildercord.gear.title").withColor(LAVENDER));
			lines.addAll(dev.wildercord.gear.Gear.describe(gear));
		}
		lines.add(Component.empty());
		lines.add(Component.translatable("screen.wildercord.mana.ways").withStyle(ChatFormatting.GOLD));
		lines.add(Component.translatable("screen.wildercord.mana.way.crystals", Mana.CRYSTAL_MANA, Mana.MAX_CRYSTALS).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.wildercord.mana.way.enchant").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.wildercord.mana.way.potions").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.wildercord.mana.way.meditate").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.wildercord.mana.way.circles").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.wildercord.mana.way.ley").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.wildercord.mana.way.gear").withStyle(ChatFormatting.GRAY));
		return lines;
	}

	/** The heart: circles formed, what they give, and what the next one needs. */
	private List<Component> heartTooltip() {
		Player player = minecraft.player;
		int circles = Heart.circles(player);
		List<Component> lines = new ArrayList<>();
		lines.add(circles == 0
			? Component.translatable("screen.wildercord.heart.none").withColor(0xFFF5C46A)
			: Component.translatable("screen.wildercord.heart.title", Circles.ordinal(circles)).withColor(0xFFF5C46A));
		if (circles > 0) {
			lines.add(Component.translatable("screen.wildercord.heart.bonus", circles * Circles.MANA_PER_CIRCLE,
				String.format(Locale.ROOT, "%.1f", circles * Circles.REGEN_PER_CIRCLE), Math.round(circles * Circles.POWER_PER_CIRCLE * 100)).withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("screen.wildercord.heart.passives", Passives.slots(Heart.active(player)), Passives.MAX).withStyle(ChatFormatting.GRAY));
		int cracked = Heart.cracked(player);
		if (cracked > 0) {
			long left = player.getAttachedOrElse(dev.wildercord.player.WildercordAttachments.CRACKS, dev.wildercord.player.WildercordAttachments.Cracks.NONE).until()
				- player.level().getGameTime();
			lines.add(Component.translatable("screen.wildercord.heart.cracked", cracked, String.format(Locale.ROOT, "%d:%02d", left / 1200, left / 20 % 60))
				.withStyle(ChatFormatting.RED));
		}
		Optional<RuneDef> innate = Runes.get(Heart.innate(player));
		innate.ifPresent(def -> lines.add(Component.translatable("screen.wildercord.heart.innate", RuneItem.runeName(def).withColor(RuneColors.of(def)),
			Math.round(dev.wildercord.cast.Innates.POWER_PER_CIRCLE * 100 * circles)).withStyle(ChatFormatting.GRAY)));
		String leaning = Heart.leaning(player);
		if (!leaning.isEmpty()) {
			lines.add(Component.translatable("screen.wildercord.heart.leaning", Component.translatable("element.wildercord." + leaning).withColor(RuneColors.element(leaning)),
				Math.round(dev.wildercord.spell.Leaning.POWER * 100)).withStyle(ChatFormatting.GRAY));
		}
		for (int perk : new int[] {Circles.MANA_SKIN, Circles.FLOW, Circles.OVERFLOW, Circles.ARCHMAGE}) {
			Component text = Component.translatable("screen.wildercord.heart.perk." + perk, Circles.ordinal(perk));
			lines.add(circles >= perk ? text.copy().withStyle(ChatFormatting.AQUA) : text.copy().withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.add(Component.empty());
		if (circles >= Circles.MAX) {
			lines.add(Component.translatable("screen.wildercord.heart.complete").withStyle(ChatFormatting.GOLD));
			return lines;
		}
		int next = circles + 1;
		lines.add(Component.translatable("screen.wildercord.heart.next", Circles.ordinal(next)).withStyle(ChatFormatting.GOLD));
		int condensed = Heart.condensed(player);
		int needed = Circles.condenseNeeded(next);
		boolean enough = condensed >= needed;
		lines.add(Component.literal(enough ? "\u2714 " : "\u2718 ").append(Component.translatable("screen.wildercord.heart.condense",
			String.format(Locale.ROOT, "%,d", Math.min(condensed, needed)), String.format(Locale.ROOT, "%,d", needed)))
			.withStyle(enough ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		for (Circles.Requirement requirement : Circles.requirements(next)) {
			boolean met = Heart.met(player, requirement);
			Component text = switch (requirement.need()) {
				case RUNES -> Component.translatable("screen.wildercord.heart.need.runes", requirement.amount(), Heart.progress(player, requirement));
				case CORD -> Component.translatable("screen.wildercord.heart.need.cord",
					Component.translatable(CordTier.values()[Math.min(CordTier.values().length - 1, requirement.amount())].itemKey()));
				case KILLS -> Component.translatable("screen.wildercord.heart.need.kills", requirement.amount(), Heart.progress(player, requirement));
				case BOSS -> Component.translatable("screen.wildercord.heart.need.boss");
				case REACTIONS -> Component.translatable("screen.wildercord.heart.need.reactions", requirement.amount(), Heart.progress(player, requirement));
				case RUNEBOUND -> Component.translatable("screen.wildercord.heart.need.runebound", requirement.amount(), Heart.progress(player, requirement));
				case SECRETS -> Component.translatable("screen.wildercord.heart.need.secrets", requirement.amount(), Heart.progress(player, requirement));
				case FEAT -> Component.translatable("screen.wildercord.heart.need.feat", dev.wildercord.spell.Feats.feat(requirement.feat()).name(),
					dev.wildercord.spell.Feats.feat(requirement.feat()).description());
			};
			lines.add(Component.literal(met ? "\u2714 " : "\u2718 ").append(text).withStyle(met ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
		lines.add(Component.translatable(Heart.ready(player) ? "screen.wildercord.heart.ready" : "screen.wildercord.heart.how")
			.withStyle(Heart.ready(player) ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY));
		return lines;
	}

	private List<Component> helpTooltip() {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.wildercord.help.title").withStyle(ChatFormatting.GOLD));
		for (int i = 1; i <= 7; i++) {
			lines.add(Component.translatable("screen.wildercord.help." + i).withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("screen.wildercord.help.keys",
			WildercordKeys.castKey(), WildercordKeys.nextKey(), WildercordKeys.openKey()).withStyle(ChatFormatting.DARK_AQUA));
		return lines;
	}

	private List<Component> runeTooltip(String id, Component extra, boolean warn) {
		List<Component> lines = new ArrayList<>();
		Optional<RuneDef> rune = Runes.get(id);
		if (rune.isEmpty()) {
			lines.add(Component.translatable("item.wildercord.rune.silent").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("tooltip.wildercord.silent", id).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			RuneDef def = rune.get();
			int rank = RuneRanks.rank(minecraft.player, def.id());
			lines.add(RuneItem.runeName(def).append(Ranks.suffix(rank)).withColor(RuneColors.of(def)));
			lines.add(RuneItem.familyLine(def).append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(categoryName(def).copy().withStyle(ChatFormatting.GRAY)));
			if (Knots.isKnot(def)) {
				lines.add(Component.translatable("screen.wildercord.knot_holds", def.description()).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.wildercord.knot.rules", Math.round((1 - Knots.DISCOUNT) * 100)).withStyle(ChatFormatting.DARK_GRAY));
			} else {
				lines.add(RuneItem.runeDescription(def).withStyle(ChatFormatting.GRAY));
			}
			if (rank > 1) {
				lines.add(Component.translatable("tooltip.wildercord.rank", RuneItem.roman(rank), Math.round((Ranks.power(rank) - 1) * 100)).withColor(GOLD));
			}
			// What it does to the ground it lands on: burns grass, freezes water...
			dev.wildercord.spell.WorldRules.Interaction world = dev.wildercord.spell.WorldRules.of(def);
			if (world != dev.wildercord.spell.WorldRules.Interaction.NONE) {
				lines.add(Component.translatable(world.tooltipKey()).withStyle(ChatFormatting.DARK_GREEN));
			}
			// The part it plays in the newer reactions: the mark it leaves, and what its damage sets off.
			String mark = dev.wildercord.spell.ReactionRules.marks(def);
			if (mark != null) {
				lines.add(Component.translatable(dev.wildercord.spell.ReactionRules.markKey(mark))
					.withColor(dev.wildercord.spell.ReactionRules.color(dev.wildercord.spell.ReactionRules.reactionFor(mark))));
			}
			String reaction = dev.wildercord.spell.ReactionRules.triggers(def);
			if (reaction != null) {
				lines.add(Component.translatable(dev.wildercord.spell.ReactionRules.triggerKey(reaction)).withColor(dev.wildercord.spell.ReactionRules.color(reaction)));
			}
			if (def.family() == RuneFamily.MODIFIER) {
				lines.add(Component.translatable("screen.wildercord.modifier_hint").withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		if (extra != null) {
			lines.add(extra.copy().withStyle(warn ? ChatFormatting.RED : ChatFormatting.DARK_AQUA));
		}
		return lines;
	}

	// ------------------------------------------------------------------ keyboard: the search box

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (tier() == null || !event.isAllowedChatCharacter()) {
			return super.charTyped(event);
		}
		if (renaming) {
			if (renameText.length() < dev.wildercord.spell.SpellNames.MAX_LENGTH) {
				renameText += event.codepointAsString();
			}
			return true;
		}
		if (grimoirePage) {
			return true;
		}
		// Typing anywhere starts a search.
		searchFocused = true;
		if (query.length() < 40) {
			query += event.codepointAsString();
			codexScroll = 0;
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (renaming) {
			if (event.key() == InputConstants.KEY_BACKSPACE) {
				if (!renameText.isEmpty()) {
					renameText = event.hasControlDown() ? "" : renameText.substring(0, renameText.length() - 1);
				}
				return true;
			}
			if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
				ClientPlayNetworking.send(new WildercordNetworking.RenameSpell(editing, renameText));
				renaming = false;
				click();
				return true;
			}
			if (event.isEscape()) {
				renaming = false;
				return true;
			}
			return true;
		}
		if (event.hasControlDown() && event.key() == InputConstants.KEY_F) {
			searchFocused = true;
			return true;
		}
		if (searchFocused) {
			if (event.key() == InputConstants.KEY_BACKSPACE) {
				if (!query.isEmpty()) {
					query = event.hasControlDown() ? "" : query.substring(0, query.length() - 1);
					codexScroll = 0;
				}
				return true;
			}
			if (event.isEscape()) {
				// First Escape clears the search, the next one closes the screen.
				if (!query.isEmpty()) {
					query = "";
					codexScroll = 0;
				} else {
					searchFocused = false;
				}
				return true;
			}
		}
		return super.keyPressed(event);
	}

	// ------------------------------------------------------------------ mouse (all in local coordinates)

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = localX(event.x());
		double my = localY(event.y());
		if (tier() == null) {
			return super.mouseClicked(event, doubleClick);
		}
		if (pressedRune != null || pressedSocket >= 0) {
			if (event.button() != pressButton) {
				// Mid-press (or mid-drag): other buttons do nothing until that one's let go.
				return true;
			}
			// The same button pressed again: its last release was missed.
			pressedRune = null;
			pressedSpell = -1;
			pressedSocket = -1;
			dragging = false;
		}
		// Spells | Passives | Grimoire
		int pageX = 13 + font.width(Component.translatable(tier().itemKey())) + 8;
		for (int page = 0; page < PAGE_KEYS.length; page++) {
			int w = font.width(Component.translatable(PAGE_KEYS[page])) + 10;
			if (inside(mx, my, pageX, 7, w, 13)) {
				if (page == 3) {
					// Cosmetics is its own screen, drawn as a page of this one.
					click();
					minecraft.gui.setScreen(new dev.wildercord.client.cosmetic.CordStyleScreen(this));
					return true;
				}
				if (page() != page) {
					passivePage = page == 1;
					grimoirePage = page == 2;
					readoutScroll = 0;
					renaming = false;
					click();
				}
				return true;
			}
			pageX += w + 2;
		}
		if (grimoirePage) {
			return true;
		}
		if (!passivePage && clickSpellTools(mx, my)) {
			return true;
		}
		searchFocused = inside(mx, my, searchX(), TABS_TOP, SEARCH_W, 13);
		if (searchFocused) {
			if (event.button() == 1) {
				query = "";
				codexScroll = 0;
			}
			return true;
		}
		int tx = 12;
		for (int t = 0; t < TAB_KEYS.length; t++) {
			int w = font.width(Component.translatable("screen.wildercord.tab." + TAB_KEYS[t])) + 18;
			if (inside(mx, my, tx, TABS_TOP, w, 13)) {
				filter = TAB_FAMILIES[t];
				category = null;
				codexScroll = 0;
				click();
				return true;
			}
			tx += w + 2;
		}
		if (filter != null && my >= CHIPS_TOP && my < CHIPS_TOP + 12) {
			int x = 12;
			for (Chip chip : chips()) {
				if (x + chip.width() > W - 14) {
					// Past the edge: not drawn, so not clickable.
					break;
				}
				if (inside(mx, my, x, CHIPS_TOP, chip.width(), 12)) {
					category = chip.key();
					codexScroll = 0;
					click();
					return true;
				}
				x += chip.width() + CHIP_GAP;
			}
		}
		RuneDef rune = codexAt(mx, my);
		if (rune != null) {
			if (!fitsPage(rune)) {
				deny(whyNot(Optional.of(rune), current()));
				return true;
			}
			pressButton = event.button();
			pressedRune = rune.id();
			pressedSpell = -1;
			pressedSocket = -1;
			pressX = mx;
			pressY = my;
			dragging = false;
			return true;
		}
		for (int s = 0; s < rowCount(); s++) {
			int ry = SPELL_TOP + s * SPELL_ROW;
			if (!inside(mx, my, 10, ry - 2, W - 20, SPELL_ROW)) {
				continue;
			}
			if (!rowOpen(s)) {
				deny(locked(s));
				return true;
			}
			if (passivePage && inside(mx, my, W - 16 - 28, ry + 2, 28, 13)) {
				ClientPlayNetworking.send(new WildercordNetworking.TogglePassive(s));
				click();
				return true;
			}
			if (s != current()) {
				readoutScroll = 0;
				if (passivePage) {
					editingPassive = s;
				} else {
					editing = s;
					ClientPlayNetworking.send(new WildercordNetworking.SelectSpell(s));
				}
				click();
			}
			int socket = socketAt(mx);
			if (socket >= 0 && socket < rows().get(s).size()) {
				pressButton = event.button();
				pressedRune = null;
				pressedSpell = s;
				pressedSocket = socket;
				pressX = mx;
				pressY = my;
				dragging = false;
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		double mx = localX(event.x());
		double my = localY(event.y());
		if ((pressedRune != null || pressedSocket >= 0) && !dragging && Math.hypot(mx - pressX, my - pressY) > 3) {
			dragging = true;
		}
		return dragging || super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (pressedRune == null && pressedSocket < 0) {
			return super.mouseReleased(event);
		}
		if (event.button() != pressButton) {
			// Only letting go of the button that pressed ends the press.
			return true;
		}
		if (pressedSocket >= 0 && (pressedSpell < 0 || pressedSpell >= rows().size() || pressedSocket >= rows().get(pressedSpell).size())) {
			// What was pressed isn't there any more (the Cord changed under it): nothing to move.
			pressedSpell = -1;
			pressedSocket = -1;
			dragging = false;
			return true;
		}
		if (dragging) {
			drop(localX(event.x()), localY(event.y()));
		} else if (pressedRune != null) {
			add(current(), rows().get(current()).size(), pressedRune);
		} else {
			rows().get(pressedSpell).remove(pressedSocket);
			sync(pressedSpell);
			ui(dev.wildercord.content.WildercordSounds.RUNE_UNTHREAD);
		}
		pressedRune = null;
		pressedSpell = -1;
		pressedSocket = -1;
		dragging = false;
		return true;
	}

	private void drop(double mx, double my) {
		int dropSpell = -1;
		int dropSocket = -1;
		for (int s = 0; s < rowCount(); s++) {
			int ry = SPELL_TOP + s * SPELL_ROW;
			if (rowOpen(s) && inside(mx, my, 10, ry - 2, W - 20, SPELL_ROW)) {
				dropSpell = s;
				dropSocket = Math.max(0, socketAt(mx));
			}
		}
		if (pressedRune != null) {
			if (dropSpell >= 0) {
				add(dropSpell, dropSocket, pressedRune);
			}
			return;
		}
		List<String> from = rows().get(pressedSpell);
		String id = from.get(pressedSocket);
		if (dropSpell < 0) {
			// Dragged a threaded rune off the Cord: take it out.
			from.remove(pressedSocket);
			sync(pressedSpell);
			ui(dev.wildercord.content.WildercordSounds.RUNE_UNTHREAD);
		} else if (dropSpell == pressedSpell) {
			from.remove(pressedSocket);
			from.add(Math.min(dropSocket, from.size()), id);
			sync(dropSpell);
			ui(dev.wildercord.content.WildercordSounds.RUNE_THREAD);
		} else {
			// Moving to another spell counts as a new rune there: it must fit and be allowed.
			Optional<RuneDef> rune = Runes.get(id);
			if (rune.isEmpty() || !fitsPage(rune.get()) || rows().get(dropSpell).size() >= rowSockets()) {
				deny(whyNot(rune, dropSpell));
				return;
			}
			from.remove(pressedSocket);
			sync(pressedSpell);
			add(dropSpell, dropSocket, id);
		}
	}

	/** Adds a rune to a spell if the Cord allows it: the spell exists, a socket is free, and the rune isn't too strong. */
	private void add(int spell, int at, String id) {
		Optional<RuneDef> rune = Runes.get(id);
		List<String> target = rows().get(spell);
		if (!rowOpen(spell) || rune.isEmpty() || !fitsPage(rune.get()) || target.size() >= rowSockets()) {
			deny(whyNot(rune, spell));
			return;
		}
		target.add(Math.min(at, target.size()), id);
		if (passivePage) {
			editingPassive = spell;
		} else {
			editing = spell;
		}
		sync(spell);
		ui(dev.wildercord.content.WildercordSounds.RUNE_THREAD);
	}

	private String draggedId() {
		if (pressedRune != null) {
			return pressedRune;
		}
		if (pressedSpell >= 0 && pressedSocket >= 0 && pressedSocket < rows().get(pressedSpell).size()) {
			return rows().get(pressedSpell).get(pressedSocket);
		}
		return null;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		double my = localY(y);
		int step = (int) Math.signum(scrollY);
		if (grimoirePage) {
			grimoireScroll = Math.max(0, grimoireScroll - step * LINE * 2);
			return true;
		}
		if (my >= READOUT_TOP - 3) {
			readoutScroll = Math.max(0, readoutScroll - step);
		} else {
			codexScroll = Math.max(0, codexScroll - step * CELL);
		}
		return true;
	}

	/** The rune under the mouse in the Codex, walking the same rows the drawing does. */
	private RuneDef codexAt(double mx, double my) {
		if (my < CODEX_TOP || my >= CODEX_BOTTOM) {
			return null;
		}
		int y = CODEX_TOP - codexScroll;
		for (CodexRow row : codexRows()) {
			int h = row.height();
			if (row.runes() != null && my >= y && my < y + h) {
				int i = (int) Math.floor((mx - CODEX_X - LABEL_W) / CELL);
				return i >= 0 && i < row.runes().size() ? row.runes().get(i) : null;
			}
			y += h;
		}
		return null;
	}

	/** Socket index under the mouse in a spell row, or -1 left of the sockets. */
	private static int socketAt(double mx) {
		double rel = mx - SOCKET_X;
		return rel < 0 ? -1 : Math.min(CordTier.MAX_SOCKETS - 1, (int) (rel / PITCH));
	}

	private void sync(int row) {
		if (passivePage) {
			ClientPlayNetworking.send(new WildercordNetworking.EditPassive(row, List.copyOf(passives.get(row))));
		} else {
			ClientPlayNetworking.send(new WildercordNetworking.EditSpell(row, List.copyOf(spells.get(row))));
		}
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	/** One of the mod's own UI sounds, at full volume (they're made quiet already). */
	private void ui(net.minecraft.sounds.SoundEvent sound) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
	}

	private void deny() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6F));
	}

	/** Refuses a click, and says why at the top of the readout. */
	private void deny(Component why) {
		deny();
		refused = why;
		refusedUntil = net.minecraft.util.Util.getMillis() + 4000;
	}

	/** Why a spell (or passive) row can't be used yet. */
	private Component locked(int row) {
		if (passivePage) {
			return Component.translatable("screen.wildercord.passive_locked", Circles.ordinal(Passives.circleFor(row)));
		}
		if (row == dev.wildercord.gear.SpellSlots.TOME) {
			return Component.translatable("message.wildercord.tome_needed");
		}
		return Component.translatable("message.wildercord.spell_needs", row + 1, Component.translatable(CordTier.forSpells(row + 1).itemKey()));
	}

	/** Why {@code rune} can't go on {@code row}. */
	private Component whyNot(Optional<RuneDef> rune, int row) {
		if (!rowOpen(row)) {
			return locked(row);
		}
		if (rune.isEmpty()) {
			return Component.translatable("screen.wildercord.quiet_silent");
		}
		if (!holds(rune.get())) {
			return Component.translatable("message.wildercord.too_strong", RuneItem.runeName(rune.get()),
				Component.translatable(CordTier.forRuneTier(rune.get().tier()).itemKey()));
		}
		if (passivePage && !Passives.allowed(rune.get())) {
			return Component.translatable("screen.wildercord.refused.passive", RuneItem.runeName(rune.get()));
		}
		return Component.translatable("message.wildercord.sockets_full", Component.translatable(tier().itemKey()), rowSockets())
			.append(" ").append(Component.translatable("screen.wildercord.refused.take_out"));
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ------------------------------------------------------------------ pages, spell tools and the Grimoire

	private static final String[] PAGE_KEYS = {"screen.wildercord.page.spells", "screen.wildercord.page.passives", "screen.wildercord.page.grimoire",
		"screen.wildercord.page.cosmetics"};
	private static final int TOOL = 14;
	private static final String[] TOOL_GLYPHS = {"\u270E", "\u29C9", "\u2398", "\u2709"};
	private static final String[] TOOL_KEYS = {"rename", "copy", "paste", "scroll"};
	private static final int TOOLS_W = TOOL_GLYPHS.length * (TOOL + 2);

	private int page() {
		return grimoirePage ? 2 : passivePage ? 1 : 0;
	}

	/** Shows page {@code page} (0 Spells, 1 Passives, 2 Grimoire): how the Cosmetics page comes back. */
	public void showPage(int page) {
		passivePage = page == 1;
		grimoirePage = page == 2;
		readoutScroll = 0;
		renaming = false;
	}

	private int toolX(int i) {
		return W - 16 - TOOLS_W + i * (TOOL + 2);
	}

	private int toolY() {
		return READOUT_TOP - 2;
	}

	/** Rename, copy code, paste code, inscribe a scroll: small buttons at the top right of the readout. */
	private List<Component> drawSpellTools(GuiGraphicsExtractor g, int mx, int my) {
		List<Component> tip = null;
		if (spellCount() == 0) {
			return null;
		}
		for (int i = 0; i < TOOL_GLYPHS.length; i++) {
			int x = toolX(i);
			int y = toolY();
			boolean hover = inside(mx, my, x, y, TOOL, TOOL);
			boolean active = i == 0 && renaming;
			sprite(g, active || hover ? SPR_TAB_ACTIVE : SPR_TAB, x, y, TOOL, TOOL);
			String glyph = TOOL_GLYPHS[i];
			g.text(font, glyph, x + TOOL / 2 - font.width(glyph) / 2 + 1, y + 3, active ? GOLD : hover ? TEXT : DIM, false);
			if (hover) {
				tip = List.of(Component.translatable("screen.wildercord.tool." + TOOL_KEYS[i]).withStyle(ChatFormatting.GOLD),
					Component.translatable("screen.wildercord.tool." + TOOL_KEYS[i] + ".hint").withStyle(ChatFormatting.GRAY));
			}
		}
		return tip;
	}

	private boolean clickSpellTools(double mx, double my) {
		if (spellCount() == 0 || editing >= spells.size()) {
			return false;
		}
		for (int i = 0; i < TOOL_GLYPHS.length; i++) {
			if (!inside(mx, my, toolX(i), toolY(), TOOL, TOOL)) {
				continue;
			}
			switch (i) {
				case 0 -> {
					renaming = !renaming;
					renameText = book().name(editing);
					if (renaming) {
						searchFocused = false;
					}
				}
				case 1 -> {
					String code = dev.wildercord.spell.SpellCodes.encode(spells.get(editing));
					minecraft.keyboardHandler.setClipboard(code);
					minecraft.player.sendOverlayMessage(Component.translatable("message.wildercord.code_copied", code).withColor(0x7FE0F0));
				}
				case 2 -> paste();
				case 3 -> ClientPlayNetworking.send(new WildercordNetworking.InscribeScroll(editing));
				default -> { }
			}
			click();
			return true;
		}
		return false;
	}

	/** Loads a spell code from the clipboard into the selected spell: runes you know, that the Cord holds. */
	private void paste() {
		String code = dev.wildercord.spell.SpellCodes.find(minecraft.keyboardHandler.getClipboard());
		if (code == null) {
			minecraft.player.sendOverlayMessage(Component.translatable("message.wildercord.code_none").withColor(0xE06060));
			return;
		}
		List<String> kept = new ArrayList<>();
		int missing = 0;
		for (String id : dev.wildercord.spell.SpellCodes.decode(code)) {
			Optional<RuneDef> rune = Runes.get(id);
			if (rune.isPresent() && book().knows(id) && holds(rune.get()) && kept.size() < sockets()) {
				kept.add(id);
			} else {
				missing++;
			}
		}
		spells.set(editing, kept);
		sync(editing);
		minecraft.player.sendOverlayMessage(missing == 0
			? Component.translatable("message.wildercord.code_loaded").withColor(0x7FE0F0)
			: Component.translatable("message.wildercord.code_partial", missing).withColor(0xF0C440));
	}

	private record GrimoireLine(Component text, int x, int color, List<Component> tooltip) {}

	/** Everything discovered: reactions, secret spells (and riddles), feats, your innate rune and your leaning. */
	private List<Component> drawGrimoire(GuiGraphicsExtractor g, int mx, int my) {
		Player player = minecraft.player;
		List<String> found = Heart.grimoire(player);
		List<GrimoireLine> lines = new ArrayList<>();
		int top = SPELL_TOP - 4;
		int bottom = H - 12;
		sprite(g, SPR_INSET, 10, top - 3, W - 20, bottom + 3 - (top - 3));
		// Innate rune and leaning.
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.heart"), 0, GOLD, null));
		String innate = Heart.innate(player);
		Optional<RuneDef> innateRune = Runes.get(innate);
		if (innateRune.isPresent()) {
			RuneDef def = innateRune.get();
			lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.innate", RuneItem.runeName(def).withColor(RuneColors.of(def))), 8, TEXT,
				List.of(RuneItem.runeName(def).withColor(RuneColors.of(def)), RuneItem.runeDescription(def).withStyle(ChatFormatting.GRAY))));
		} else {
			lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.innate_none"), 8, DIM, null));
		}
		String leaning = Heart.leaning(player);
		lines.add(new GrimoireLine(leaning.isEmpty()
			? Component.translatable("screen.wildercord.grimoire.leaning_none", dev.wildercord.spell.Leaning.MIN_CASTS)
			: Component.translatable("screen.wildercord.grimoire.leaning", Component.translatable("element.wildercord." + leaning).withColor(RuneColors.element(leaning)),
				Math.round(dev.wildercord.spell.Leaning.POWER * 100)), 8, leaning.isEmpty() ? DIM : TEXT, null));
		// Reactions.
		int reactions = dev.wildercord.spell.Feats.count(found, "reaction:");
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.reactions", reactions, dev.wildercord.spell.Feats.REACTIONS.size()), 0, GOLD, null));
		for (String reaction : dev.wildercord.spell.Feats.REACTIONS) {
			boolean known = found.contains(dev.wildercord.spell.Feats.reactionKey(reaction));
			lines.add(new GrimoireLine(known ? Component.translatable("reaction.wildercord." + reaction) : Component.literal("???"), 8, known ? CYAN : FAINT,
				known ? List.of(Component.translatable("reaction.wildercord." + reaction + ".desc").withStyle(ChatFormatting.GRAY)) : null));
		}
		// Secret spells: found ones in full, hinted ones as their riddle.
		int secrets = dev.wildercord.spell.Feats.count(found, "secret:");
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.secrets", secrets, dev.wildercord.spell.Secrets.ALL.size()), 0, GOLD, null));
		for (dev.wildercord.spell.Secrets.Secret secret : dev.wildercord.spell.Secrets.ALL) {
			if (found.contains(secret.key())) {
				StringBuilder runes = new StringBuilder();
				for (RuneDef rune : secret.runes()) {
					runes.append(runes.isEmpty() ? "" : " \u00B7 ").append(RuneItem.runeName(rune).getString());
				}
				lines.add(new GrimoireLine(Component.literal(secret.name()), 8, 0xFF000000 | secret.color(),
					List.of(Component.literal(secret.name()).withColor(secret.color()), Component.literal(runes.toString()).withStyle(ChatFormatting.GRAY),
						Component.literal(secret.description()).withStyle(ChatFormatting.DARK_GRAY))));
			} else if (found.contains("hint:" + secret.id())) {
				lines.add(new GrimoireLine(Component.literal("\u201C" + secret.riddle() + "\u201D").withStyle(ChatFormatting.ITALIC), 8, 0xFFC8B89A, null));
			} else {
				lines.add(new GrimoireLine(Component.literal("???"), 8, FAINT, List.of(Component.translatable("screen.wildercord.grimoire.secret_unknown").withStyle(ChatFormatting.GRAY))));
			}
		}
		// Duels fought (see /duel): wins and losses, once there's been one.
		dev.wildercord.duel.Duels.Record duels = minecraft.player == null ? dev.wildercord.duel.Duels.Record.NONE
			: minecraft.player.getAttachedOrElse(dev.wildercord.duel.Duels.RECORD, dev.wildercord.duel.Duels.Record.NONE);
		if (duels.wins() + duels.losses() > 0) {
			lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.duels"), 0, GOLD, null));
			lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.duel_record", duels.wins(), duels.losses()), 8, TEXT,
				List.of(Component.translatable("screen.wildercord.grimoire.duel_hint").withStyle(ChatFormatting.GRAY))));
		}
		// Fusions: found ones by name and recipe; the rest as ??? + ???, with a hint of one element.
		int fusions = dev.wildercord.spell.Feats.count(found, Fusions.KEY_PREFIX);
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.fusions", fusions, Fusions.RECIPES.size()), 0, GOLD, null));
		for (Fusions.Recipe recipe : Fusions.RECIPES) {
			RuneDef made = recipe.result();
			Component first = Component.translatable("element.wildercord." + recipe.first()).withColor(RuneColors.element(recipe.first()));
			Component second = Component.translatable("element.wildercord." + recipe.second()).withColor(RuneColors.element(recipe.second()));
			if (found.contains(recipe.key())) {
				lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.fusion", RuneItem.runeName(made).withColor(RuneColors.of(made)), first, second), 8, TEXT,
					List.of(RuneItem.runeName(made).withColor(RuneColors.of(made)), RuneItem.runeDescription(made).withStyle(ChatFormatting.GRAY),
						Component.translatable("screen.wildercord.grimoire.fusion_how", first, second).withStyle(ChatFormatting.DARK_GRAY))));
			} else {
				Component hint = Component.translatable("screen.wildercord.grimoire.fusion_hint", first.copy().withStyle(ChatFormatting.DARK_GRAY));
				lines.add(new GrimoireLine(Component.literal("??? + ???  ").append(hint), 8, FAINT,
					List.of(Component.translatable("screen.wildercord.grimoire.fusion_unknown").withStyle(ChatFormatting.GRAY))));
			}
		}
		// Attunements: found ones by their land and rune, the rest as riddles.
		addAttunements(lines, found);
		// The runes of the world, by where they're found: known ones by name, the rest as a hint.
		addWorldRunes(lines);
		// Feats.
		int feats = dev.wildercord.spell.Feats.count(found, "feat:");
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.feats", feats, dev.wildercord.spell.Feats.FEATS.size()), 0, GOLD, null));
		for (dev.wildercord.spell.Feats.Feat feat : dev.wildercord.spell.Feats.FEATS) {
			boolean done = found.contains(feat.key());
			lines.add(new GrimoireLine(Component.literal((done ? "\u2714 " : "\u2022 ") + feat.name()), 8, done ? 0xFF9CE08C : DIM,
				List.of(Component.literal(feat.name()).withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY),
					Component.literal(feat.description()).withStyle(ChatFormatting.GRAY))));
		}
		int visible = (bottom - top) / LINE;
		grimoireScroll = Math.max(0, Math.min(grimoireScroll, Math.max(0, lines.size() * LINE - visible * LINE)));
		int first = grimoireScroll / LINE;
		List<Component> tip = null;
		g.enableScissor(12, top - 1, W - 12, bottom);
		for (int i = 0; i < visible + 1 && first + i < lines.size(); i++) {
			GrimoireLine line = lines.get(first + i);
			int y = top + i * LINE;
			int x = TEXT_X + line.x();
			if (line.x() == 0) {
				g.fill(TEXT_X - 2, y + 9, W - 20, y + 10, 0x40E8C46A);
			}
			fitText(g, line.text(), x, y, W - 24 - x, line.color(), line.x() == 0);
			if (line.tooltip() != null && inside(mx, my, x, y - 1, Math.min(W - 24 - x, font.width(line.text())), LINE)) {
				tip = line.tooltip();
			}
		}
		g.disableScissor();
		if (first > 0) {
			arrow(g, W - 18, top + 1, true);
		}
		if (first + visible < lines.size()) {
			arrow(g, W - 18, bottom - 4, false);
		}
		return tip;
	}

	private void addAttunements(List<GrimoireLine> lines, List<String> found) {
		List<dev.wildercord.spell.Attunements.Rule> rules = dev.wildercord.spell.Attunements.RULES;
		int attuned = dev.wildercord.spell.Feats.count(found, "attune:");
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.attunements", attuned, rules.size()), 0, GOLD, null));
		for (dev.wildercord.spell.Attunements.Rule rule : rules) {
			RuneDef rune = rule.rune();
			if (found.contains(rule.key())) {
				String biome = rule.biomes().stream().sorted().findFirst().orElse("");
				Component land = Component.translatable("biome." + biome.replace(':', '.'));
				// A land gives its rune once a day: when it's ready again.
				Long last = minecraft.player == null ? null
					: minecraft.player.getAttachedOrElse(dev.wildercord.player.WildercordAttachments.ATTUNED_AT, java.util.Map.<String, Long>of()).get(rule.id());
				long rest = minecraft.level == null ? 0 : dev.wildercord.spell.ExplorerNumbers.attuneRestLeft(last, minecraft.level.getGameTime());
				Component ready = rest > 0
					? Component.translatable("screen.wildercord.grimoire.attune_resting", Math.max(1, (rest + 1199) / 1200)).withStyle(ChatFormatting.DARK_GRAY)
					: Component.translatable("screen.wildercord.grimoire.attune_ready").withStyle(ChatFormatting.DARK_GRAY);
				lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.attunement", land, RuneItem.runeName(rune).withColor(RuneColors.of(rune))),
					8, TEXT, List.of(RuneItem.runeName(rune).withColor(RuneColors.of(rune)), RuneItem.runeDescription(rune).withStyle(ChatFormatting.GRAY),
						Component.literal(land.getString() + ", " + rule.needs()).withStyle(ChatFormatting.DARK_GRAY), ready)));
			} else {
				lines.add(new GrimoireLine(Component.literal("“" + rule.riddle() + "”").withStyle(ChatFormatting.ITALIC), 8, 0xFFB8C8A0,
					List.of(Component.translatable("screen.wildercord.grimoire.attune_hint").withStyle(ChatFormatting.GRAY))));
			}
		}
	}

	private void addWorldRunes(List<GrimoireLine> lines) {
		Spellbook book = book();
		List<RuneDef> world = dev.wildercord.spell.RuneSources.runes();
		long known = world.stream().filter(r -> book.knows(r.id())).count();
		lines.add(new GrimoireLine(Component.translatable("screen.wildercord.grimoire.world", known, world.size()), 0, GOLD, null));
		for (dev.wildercord.spell.RuneSources.Source source : dev.wildercord.spell.RuneSources.all()) {
			if (source.id().startsWith("attunement:")) {
				// Attunements have their own section above.
				continue;
			}
			StringBuilder names = new StringBuilder();
			List<Component> tip = new ArrayList<>();
			tip.add(Component.literal(source.where()).withStyle(ChatFormatting.GOLD));
			tip.add(Component.translatable("screen.wildercord.grimoire.world_hint").withStyle(ChatFormatting.DARK_GRAY));
			for (RuneDef rune : source.runes()) {
				boolean knows = book.knows(rune.id());
				names.append(names.isEmpty() ? "" : ", ").append(knows ? RuneItem.runeName(rune).getString() : "???");
				tip.add(knows ? RuneItem.runeName(rune).withColor(RuneColors.of(rune))
					: Component.translatable("screen.wildercord.grimoire.world_unknown", RuneItem.roman(rune.tier()),
						Component.translatable("family.wildercord." + familyKey(rune.family())).getString().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
			}
			boolean all = source.runes().stream().allMatch(r -> book.knows(r.id()));
			lines.add(new GrimoireLine(Component.literal(source.where() + ": " + names), 8, all ? 0xFF9CE08C : DIM, tip));
		}
	}

	/** Used by the HUD to show an item for a rune id. */
	static ItemStack icon(String id) {
		return RuneItem.stack(id);
	}
}
