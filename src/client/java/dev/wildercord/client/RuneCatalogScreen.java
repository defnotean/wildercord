package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneCatalog;
import dev.wildercord.spell.RuneCatalog.Filter;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneCompanions;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.RuneReading;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.WovenRunes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The rune catalog: every rune the player knows (or every rune there is), searched by name and by what they can read
 * of it, filtered by family, element, use and reading, and sorted. Picking one shows its text and what goes well with
 * it ({@link RuneCompanions}). Laid out from the window's size, so it fits any GUI scale; the filter is kept until the
 * game closes. Opened from the Cord (the Catalog link or Ctrl+B).
 */
public final class RuneCatalogScreen extends Screen {
	private static final int ROW = 18;
	private static final int BAR = 20;
	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int DIM = 0xFFA0A0A0;
	private static final int FAINT = 0xFF707070;
	private static final int PANEL = 0xC0101018;
	private static final int EDGE = 0xFF3A3450;
	private static final int PICK = 0x5050A0FF;
	private static final int SUGGEST = 6;

	/** Kept for the session, so the catalog opens where it was left. */
	private static Filter saved = Filter.NONE;
	private static int savedPage;
	private static String savedPick;

	private final Screen parent;
	private Filter filter = saved;
	private int page = savedPage;
	private RuneDef picked;
	private int detailScroll;
	private int detailHeight;

	private EditBox search;
	private Button family, element, use, show, sort, prev, next, find;

	// Layout, worked out in init().
	private int m, listX, listY, listW, listH, cols, colW, rows, detailX, detailW;

	/** Clickable rune icons in the detail panel, as drawn last frame. */
	private final List<Hit> hits = new ArrayList<>();

	private record Hit(int x, int y, RuneDef rune) {}

	/** The list as it was last worked out, and what it was worked out for. */
	private List<RuneDef> cached = List.of();
	private Filter cachedFor;
	private int cachedRunes = -1;

	public RuneCatalogScreen(Screen parent) {
		super(Component.translatable("screen.wildercord.catalog.title"));
		this.parent = parent;
		if (savedPick != null) {
			picked = Runes.get(savedPick).orElse(null);
		}
	}

	private static Component t(String key, Object... args) {
		return Component.translatable("screen.wildercord.catalog." + key, args);
	}

	// ------------------------------------------------------------------ what the player knows

	private Spellbook book() {
		return Spellbooks.get(minecraft.player);
	}

	/** Every rune there is, with the Knots and weaves this player has made. */
	private List<RuneDef> everyRune() {
		List<RuneDef> runes = new ArrayList<>(Runes.all());
		if (minecraft.player != null) {
			for (String id : book().learned()) {
				if (Knots.isKnot(id) || WovenRunes.isWoven(id)) {
					Runes.get(id).ifPresent(runes::add);
				}
			}
		}
		return runes;
	}

	private boolean knows(RuneDef rune) {
		return minecraft.player != null && (book().knows(rune.id()) || book().learned().contains(rune.id()));
	}

	private boolean ready(RuneDef rune) {
		CordTier tier = minecraft.player == null ? null : Spellbooks.tier(minecraft.player);
		return tier != null && tier.holds(rune.tier());
	}

	private boolean reading(RuneDef rune) {
		return RuneReadingText.stage(minecraft.player, rune) != RuneReading.Stage.UNDERSTOOD;
	}

	private static String familyKey(RuneFamily family) {
		return family.name().toLowerCase(Locale.ROOT);
	}

	private static Component elementName(String element) {
		return element == null ? t("any") : element.equals("none") ? t("element.none") : Component.translatable("element.wildercord." + element);
	}

	private static Component useName(RuneCatalog.Use use) {
		return use == null ? t("any") : t("use." + use.key());
	}

	/** Words a rune is always found by, whether or not it's been read: never its text. */
	private static String tags(RuneDef rune) {
		StringBuilder out = new StringBuilder();
		out.append(rune.element()).append(' ');
		if (!rune.element().isEmpty()) {
			out.append(Component.translatable("element.wildercord." + rune.element()).getString()).append(' ');
		}
		out.append(Component.translatable("category.wildercord." + familyKey(rune.family()) + "." + rune.category()).getString()).append(' ')
			.append(rune.category()).append(' ')
			.append(Component.translatable("family.wildercord." + familyKey(rune.family())).getString()).append(' ')
			.append(Component.translatable("screen.wildercord.tab." + familyKey(rune.family())).getString());
		for (RuneCatalog.Use use : RuneCatalog.uses(rune)) {
			out.append(' ').append(useName(use).getString());
		}
		return out.toString();
	}

	private RuneCatalog.View view() {
		return new RuneCatalog.View(r -> RuneItem.runeName(r).getString(), RuneCatalogScreen::tags,
			r -> RuneReadingText.searchable(minecraft.player, r), this::knows, this::ready, this::reading);
	}

	/** The runes the filter lets through, in its order. Worked out again only when the filter or the roster changes. */
	public List<RuneDef> shown() {
		if (minecraft.player == null) {
			return List.of();
		}
		int count = Runes.all().size() + book().learned().size();
		if (!filter.equals(cachedFor) || count != cachedRunes) {
			cached = RuneCatalog.select(everyRune(), filter, view());
			cachedFor = filter;
			cachedRunes = count;
		}
		return cached;
	}

	private int pageSize() {
		return Math.max(1, cols * rows);
	}

	// ------------------------------------------------------------------ layout and widgets

	@Override
	protected void init() {
		m = width < 360 ? 4 : 8;
		int gap = 2;
		int y1 = 4;
		int y2 = y1 + BAR + gap;
		int sortW = Math.min(90, Math.max(56, width / 7));
		int doneW = Math.min(60, Math.max(40, width / 10));
		int searchW = width - 2 * m - sortW - doneW - 2 * gap;

		String typed = search == null ? filter.query() : search.getValue();
		search = new EditBox(font, m, y1, searchW, BAR, Component.translatable("screen.wildercord.search"));
		search.setMaxLength(48);
		search.setHint(Component.translatable("screen.wildercord.search").copy().withStyle(ChatFormatting.DARK_GRAY));
		search.setValue(typed);
		search.setResponder(value -> {
			if (!value.equals(filter.query())) {
				setFilter(filter.withQuery(value));
			}
		});
		addRenderableWidget(search);
		sort = addRenderableWidget(Button.builder(Component.empty(), b -> cycleSort()).bounds(m + searchW + gap, y1, sortW, BAR).build());
		addRenderableWidget(Button.builder(t("back"), b -> onClose()).bounds(width - m - doneW, y1, doneW, BAR).build());

		int fw = (width - 2 * m - 3 * gap) / 4;
		family = addRenderableWidget(Button.builder(Component.empty(), b -> cycleFamily()).bounds(m, y2, fw, BAR).build());
		element = addRenderableWidget(Button.builder(Component.empty(), b -> cycleElement()).bounds(m + (fw + gap), y2, fw, BAR).build());
		use = addRenderableWidget(Button.builder(Component.empty(), b -> cycleUse()).bounds(m + 2 * (fw + gap), y2, fw, BAR).build());
		show = addRenderableWidget(Button.builder(Component.empty(), b -> cycleShow()).bounds(m + 3 * (fw + gap), y2, width - 2 * m - 3 * (fw + gap), BAR).build());

		int bottom = height - BAR - 4;
		listX = m;
		listY = y2 + BAR + 6;
		listW = (width - 3 * m) / 2;
		listH = Math.max(ROW, bottom - 4 - listY);
		cols = Math.max(1, listW / 110);
		colW = listW / cols;
		rows = Math.max(1, (listH - 2) / ROW);
		detailX = listX + listW + m;
		detailW = width - m - detailX;

		prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(listX, bottom, BAR, BAR).build());
		next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(listX + listW - BAR, bottom, BAR, BAR).build());
		find = addRenderableWidget(Button.builder(t("find"), b -> showInCord()).bounds(detailX, bottom, Math.min(detailW, 120), BAR).build());
		refresh();
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(search);
	}

	private void refresh() {
		sort.setMessage(t("sort", t("sort." + filter.sort().key())));
		family.setMessage(t("family", filter.family() == null ? t("any")
			: Component.translatable("screen.wildercord.tab." + familyKey(filter.family()))));
		element.setMessage(t("element", elementName(filter.element())));
		use.setMessage(t("use", useName(filter.use())));
		show.setMessage(t("show", t("show." + filter.show().key())));
		page = RuneCatalog.clampPage(shown().size(), page, pageSize());
		int pages = RuneCatalog.pages(shown().size(), pageSize());
		prev.active = page > 0;
		next.active = page < pages - 1;
		find.active = picked != null && knows(picked) && parent instanceof CordScreen;
		saved = filter;
		savedPage = page;
		savedPick = picked == null ? null : picked.id();
	}

	private void setFilter(Filter f) {
		filter = f;
		page = 0;
		refresh();
	}

	private static <T> T step(List<T> values, T current) {
		int i = values.indexOf(current);
		return values.get((i + 1) % values.size());
	}

	private void cycleSort() {
		setFilter(filter.withSort(step(List.of(RuneCatalog.Sort.values()), filter.sort())));
	}

	private void cycleFamily() {
		List<RuneFamily> values = new ArrayList<>();
		values.add(null);
		values.addAll(List.of(RuneFamily.values()));
		setFilter(filter.withFamily(step(values, filter.family())));
	}

	private void cycleElement() {
		List<String> values = new ArrayList<>();
		values.add(null);
		values.addAll(RuneCatalog.elements(Runes.all()));
		setFilter(filter.withElement(step(values, filter.element())));
	}

	private void cycleUse() {
		List<RuneCatalog.Use> values = new ArrayList<>();
		values.add(null);
		values.addAll(List.of(RuneCatalog.Use.values()));
		setFilter(filter.withUse(step(values, filter.use())));
	}

	private void cycleShow() {
		setFilter(filter.withShow(step(List.of(RuneCatalog.Show.values()), filter.show())));
	}

	private void turn(int by) {
		page += by;
		refresh();
	}

	/** Picks {@code rune} to show on the right. */
	public void pick(RuneDef rune) {
		picked = rune;
		detailScroll = 0;
		refresh();
	}

	public RuneDef picked() {
		return picked;
	}

	public Filter filter() {
		return filter;
	}

	/** Sets the search, as typing would. */
	public void searchFor(String text) {
		search.setValue(text);
	}

	private void showInCord() {
		if (picked != null && parent instanceof CordScreen cord) {
			cord.searchFor(RuneItem.runeName(picked).getString());
			minecraft.gui.setScreen(parent);
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		List<RuneDef> all = shown();
		List<RuneDef> runes = RuneCatalog.page(all, page, pageSize());
		Component tip = null;

		// The list.
		g.fill(listX, listY, listX + listW, listY + listH, PANEL);
		g.outline(listX, listY, listW, listH, EDGE);
		for (int i = 0; i < runes.size(); i++) {
			RuneDef rune = runes.get(i);
			int x = listX + 1 + (i % cols) * colW;
			int y = listY + 1 + (i / cols) * ROW;
			boolean over = inside(mouseX, mouseY, x, y, colW - 2, ROW);
			if (rune.equals(picked)) {
				g.fill(x, y, x + colW - 2, y + ROW, PICK);
			} else if (over) {
				g.fill(x, y, x + colW - 2, y + ROW, 0x30FFFFFF);
			}
			boolean known = knows(rune);
			g.item(RuneItem.stack(rune), x + 1, y + 1);
			if (!known) {
				g.fill(x + 1, y + 1, x + 17, y + 17, 0xA0100C18);
			} else if (reading(rune)) {
				g.text(font, "?", x + 13, y, 0xFFC8B4FF, true);
			}
			int color = known ? 0xFF000000 | RuneColors.of(rune) : FAINT;
			fit(g, RuneItem.runeName(rune), x + 20, y + 5, colW - 24, color);
		}
		if (all.isEmpty()) {
			Component empty = minecraft.player != null && filter.equals(Filter.NONE) ? t("none_known") : t("empty");
			int y = listY + 6;
			for (FormattedCharSequence line : font.split(empty, listW - 8)) {
				g.text(font, line, listX + 4, y, DIM, false);
				y += 10;
			}
		}

		// Under the list: the page and how many match.
		int pages = RuneCatalog.pages(all.size(), pageSize());
		Component count = Component.translatable("screen.wildercord.matches", all.size());
		Component pageText = t("page", page + 1, pages);
		int midX = listX + BAR + 4;
		int midW = listW - 2 * BAR - 8;
		int by = height - BAR - 4 + 6;
		if (font.width(pageText) + font.width(count) + 8 <= midW) {
			g.text(font, pageText, midX, by, DIM, false);
			g.text(font, count, midX + midW - font.width(count), by, FAINT, false);
		} else {
			fit(g, pageText, midX, by, midW, DIM);
		}

		// The picked rune.
		g.fill(detailX, listY, detailX + detailW, listY + listH, PANEL);
		g.outline(detailX, listY, detailW, listH, EDGE);
		hits.clear();
		g.enableScissor(detailX + 1, listY + 1, detailX + detailW - 1, listY + listH - 1);
		int top = listY + 4 - detailScroll;
		int end = picked == null ? drawHelp(g, top) : drawDetail(g, top);
		g.disableScissor();
		detailHeight = end + detailScroll - (listY + 4);
		detailScroll = Math.max(0, Math.min(detailScroll, Math.max(0, detailHeight - (listH - 8))));
		for (Hit hit : hits) {
			if (inside(mouseX, mouseY, hit.x(), hit.y(), 18, 18) && mouseY >= listY && mouseY < listY + listH) {
				tip = RuneItem.runeName(hit.rune()).withColor(0xFF000000 | RuneColors.of(hit.rune()));
			}
		}
		if (tip != null) {
			g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
		}
	}

	private int drawHelp(GuiGraphicsExtractor g, int y) {
		int x = detailX + 5;
		int w = detailW - 10;
		for (FormattedCharSequence line : font.split(t("pick"), w)) {
			g.text(font, line, x, y, DIM, false);
			y += 10;
		}
		y += 4;
		for (FormattedCharSequence line : font.split(t("keys"), w)) {
			g.text(font, line, x, y, FAINT, false);
			y += 10;
		}
		return y;
	}

	private int drawDetail(GuiGraphicsExtractor g, int y) {
		RuneDef rune = picked;
		int x = detailX + 5;
		int w = detailW - 10;
		boolean known = knows(rune);
		g.item(RuneItem.stack(rune), x, y);
		fit(g, RuneItem.runeName(rune), x + 20, y + 4, w - 20, 0xFF000000 | RuneColors.of(rune));
		y += 20;
		MutableComponent meta = Component.translatable("family.wildercord." + familyKey(rune.family())).copy()
			.append(" · ").append(Component.translatable("category.wildercord." + familyKey(rune.family()) + "." + rune.category()));
		if (!rune.element().isEmpty()) {
			meta.append(" · ").append(elementName(rune.element()));
		}
		meta.append(" · ").append(t("tier", RuneItem.roman(rune.tier())));
		y = lines(g, meta, x, y, w, DIM);
		var uses = RuneCatalog.uses(rune);
		Component purpose = uses.isEmpty() ? t("general")
			: t("for", uses.stream().sorted().map(u -> useName(u).getString()).collect(Collectors.joining(", ")));
		y = lines(g, purpose, x, y, w, 0xFF9FC8A0);
		if (!known) {
			y = lines(g, t("unknown"), x, y, w, 0xFFB08080);
		} else {
			Component stage = RuneReadingText.stageLine(rune);
			if (stage != null) {
				y = lines(g, stage, x, y, w, 0xFFB8A8E8);
			}
		}
		y += 3;
		y = lines(g, RuneReadingText.describe(rune), x, y, w, TEXT);
		y += 6;

		// What goes well with it.
		List<RuneDef> pool = filter.show() == RuneCatalog.Show.ALL ? everyRune() : everyRune().stream().filter(this::knows).toList();
		RuneCompanions.Suggestions s = RuneCompanions.suggest(rune, pool, SUGGEST);
		g.text(font, t("goes_with"), x, y, GOLD, false);
		y += 11;
		if (s.fixed()) {
			y = lines(g, t("fixed"), x, y, w, DIM);
		} else if (s.anyEffect()) {
			y = lines(g, t("any_spell"), x, y, w, DIM);
		}
		if (s.groups().isEmpty() && !s.anyEffect()) {
			y = lines(g, pool.size() <= 1 ? t("none_yet") : t("nothing"), x, y, w, FAINT);
		}
		for (RuneCompanions.Group group : s.groups()) {
			g.text(font, Component.translatable("screen.wildercord.tab." + familyKey(group.family())), x, y, DIM, false);
			y += 10;
			int cx = x;
			for (RuneDef other : group.runes()) {
				if (cx + 18 > x + w) {
					cx = x;
					y += 18;
				}
				g.item(RuneItem.stack(other), cx + 1, y + 1);
				hits.add(new Hit(cx, y, other));
				cx += 18;
			}
			if (group.more() > 0) {
				Component more = t("more", group.more());
				if (cx + font.width(more) + 2 > x + w) {
					cx = x;
					y += 18;
				}
				g.text(font, more, cx + 2, y + 5, FAINT, false);
			}
			y += 20;
		}
		return y;
	}

	private int lines(GuiGraphicsExtractor g, Component text, int x, int y, int w, int color) {
		for (FormattedCharSequence line : font.split(text, Math.max(10, w))) {
			g.text(font, line, x, y, color, false);
			y += 10;
		}
		return y;
	}

	private void fit(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color) {
		if (maxWidth <= 0) {
			return;
		}
		if (font.width(text) <= maxWidth) {
			g.text(font, text, x, y, color, false);
			return;
		}
		String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("…"))) + "…";
		g.text(font, Component.literal(plain).withStyle(text.getStyle()), x, y, color, false);
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ------------------------------------------------------------------ input

	/** The rune under a point in the list, if any. */
	private Optional<RuneDef> runeAt(double mx, double my) {
		if (!inside(mx, my, listX + 1, listY + 1, cols * colW, rows * ROW)) {
			return Optional.empty();
		}
		int col = (int) ((mx - listX - 1) / colW);
		int row = (int) ((my - listY - 1) / ROW);
		int i = row * cols + col;
		List<RuneDef> runes = RuneCatalog.page(shown(), page, pageSize());
		return i >= 0 && i < runes.size() ? Optional.of(runes.get(i)) : Optional.empty();
	}

	/** The middle of {@code runeId}'s row in the list, or null if it isn't on this page. */
	public double[] listPoint(String runeId) {
		List<RuneDef> runes = RuneCatalog.page(shown(), page, pageSize());
		for (int i = 0; i < runes.size(); i++) {
			if (runes.get(i).id().equals(runeId)) {
				return new double[] {listX + 1 + (i % cols) * colW + colW / 2.0, listY + 1 + (i / cols) * ROW + ROW / 2.0};
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
			return false;
		}
		Optional<RuneDef> rune = runeAt(event.x(), event.y());
		if (rune.isPresent()) {
			pick(rune.get());
			if (doubleClick) {
				showInCord();
			}
			return true;
		}
		if (event.y() >= listY && event.y() < listY + listH) {
			for (Hit hit : hits) {
				if (inside(event.x(), event.y(), hit.x(), hit.y(), 18, 18)) {
					pick(hit.rune());
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (inside(mx, my, detailX, listY, detailW, listH)) {
			detailScroll = Math.max(0, Math.min(detailScroll - (int) Math.round(v * 12), Math.max(0, detailHeight - (listH - 8))));
			return true;
		}
		if (inside(mx, my, listX, listY, listW, listH) && v != 0) {
			turn(v > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mx, my, h, v);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == InputConstants.KEY_PAGEUP) {
			turn(-1);
			return true;
		}
		if (key == InputConstants.KEY_PAGEDOWN) {
			turn(1);
			return true;
		}
		if (event.hasControlDown() && key == InputConstants.KEY_F) {
			setFocused(search);
			search.setFocused(true);
			return true;
		}
		if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
			move(key == InputConstants.KEY_UP ? -1 : 1);
			return true;
		}
		if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) && getFocused() == search) {
			List<RuneDef> runes = RuneCatalog.page(shown(), page, pageSize());
			if (!runes.isEmpty()) {
				pick(runes.contains(picked) ? picked : runes.getFirst());
			}
			return true;
		}
		if (event.isEscape() && getFocused() == search && !search.getValue().isEmpty()) {
			// First Escape clears the search, the next one closes.
			search.setValue("");
			return true;
		}
		return super.keyPressed(event);
	}

	/** Moves the pick up or down the list, turning the page when it runs off one. */
	private void move(int by) {
		List<RuneDef> all = shown();
		if (all.isEmpty()) {
			return;
		}
		int at = picked == null ? -1 : all.indexOf(picked);
		int to = at < 0 ? page * pageSize() : Math.max(0, Math.min(all.size() - 1, at + by * cols));
		page = to / pageSize();
		pick(all.get(to));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
