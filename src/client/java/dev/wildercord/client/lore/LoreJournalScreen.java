package dev.wildercord.client.lore;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.lore.LoreJournalData;
import dev.wildercord.lore.LoreQuestRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * The lore journal: open leads with their clue, goal and hint; places found; people met; what was learned;
 * and the lines teachers spoke. It only reads the synced journal and never changes it.
 */
public final class LoreJournalScreen extends Screen {
	public static final List<String> TABS = List.of("quests", "places", "people", "learned", "talk");
	private static String lastTab = "quests";
	private static final int GOLD = 0xFFD7B56D, BODY = 0xFFE8E5D9, MUTED = 0xFFA7B8B6, HINT = 0xFF9FD0C0;

	private String tab = lastTab;
	private int scroll;
	private int left, top, panelWidth, panelHeight, bodyTop, bodyBottom;

	public LoreJournalScreen() {
		super(Component.translatable("screen.wildercord.lore_journal.title"));
	}

	@Override public boolean isPauseScreen() { return false; }

	public String tab() { return tab; }

	public void showTab(String next) {
		if (!TABS.contains(next)) return;
		tab = next;
		lastTab = next;
		scroll = 0;
		rebuildWidgets();
	}

	@Override
	protected void init() {
		panelWidth = Math.min(420, width - 20);
		panelHeight = Math.min(320, height - 16);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		int tabWidth = (panelWidth - 20 - 4 * (TABS.size() - 1)) / TABS.size();
		for (int i = 0; i < TABS.size(); i++) {
			String id = TABS.get(i);
			Button button = addRenderableWidget(Button.builder(Component.translatable("screen.wildercord.lore_journal.tab." + id), b -> showTab(id))
				.bounds(left + 10 + i * (tabWidth + 4), top + 26, tabWidth, 20).build());
			button.active = !id.equals(tab);
		}
		bodyTop = top + 54;
		bodyBottom = top + panelHeight - 34;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
			.bounds(width / 2 - 50, top + panelHeight - 26, 100, 20).build());
	}

	private LoreJournalData journal() {
		return minecraft == null || minecraft.player == null ? LoreJournalData.EMPTY : LoreJournal.data(minecraft.player);
	}

	/** The plain text of a tab, for checks and screen readers. */
	public List<String> text(String which) {
		List<String> out = new ArrayList<>();
		for (Line line : content(which)) out.add(line.text().getString());
		return out;
	}

	private record Line(Component text, int color) {}

	private List<Line> content(String which) {
		LoreJournalData data = journal();
		List<Line> lines = new ArrayList<>();
		if (which.equals("quests")) {
			int hidden = 0;
			for (LoreQuestRules.Quest quest : LoreQuestRules.ALL) {
				int stage = data.stage(quest.id());
				if (stage == LoreQuestRules.UNKNOWN) { hidden++; continue; }
				if (!lines.isEmpty()) lines.add(new Line(Component.empty(), BODY));
				boolean done = stage == LoreQuestRules.DONE;
				lines.add(new Line(Component.translatable(done ? "screen.wildercord.lore_journal.done" : "screen.wildercord.lore_journal.open",
					Component.translatable(quest.key("title"))), GOLD));
				lines.add(new Line(Component.translatable(quest.key("clue")), BODY));
				if (done) {
					lines.add(new Line(Component.translatable(quest.key("done")), MUTED));
				} else {
					lines.add(new Line(Component.translatable("journal.wildercord.objective", Component.translatable(quest.key("objective"))), HINT));
					lines.add(new Line(Component.translatable("screen.wildercord.lore_journal.hint", Component.translatable(quest.key("hint"))), MUTED));
				}
			}
			if (hidden > 0) {
				if (!lines.isEmpty()) lines.add(new Line(Component.empty(), BODY));
				lines.add(new Line(Component.translatable("screen.wildercord.lore_journal.hidden", hidden), MUTED));
			}
			return lines;
		}
		for (String entry : data.entries()) {
			if (LoreEntries.tab(entry).equals(which)) lines.add(new Line(describe(entry), which.equals("talk") ? BODY : BODY));
		}
		if (lines.isEmpty()) lines.add(new Line(Component.translatable("screen.wildercord.lore_journal.empty." + which), MUTED));
		return lines;
	}

	/** One entry's text. Unknown ids from a newer version show their raw id rather than failing. */
	public static Component describe(String entry) {
		int colon = entry.indexOf(':');
		String kind = colon < 0 ? entry : entry.substring(0, colon), value = colon < 0 ? "" : entry.substring(colon + 1);
		return switch (kind) {
			case "method", "duelist", "won", "master", "victory" -> Component.translatable("journal.wildercord.entry." + kind, LoreJournal.methodName(value));
			case "stage" -> Component.translatable("journal.wildercord.entry.stage", Component.translatable("aura.wildercord.stage." + value));
			case "form" -> Component.translatable("journal.wildercord.entry.form", Component.translatableWithFallback("journal.wildercord.form." + value, value));
			case "said" -> {
				String[] parts = value.split("\\.");
				Component speaker = Component.translatable("dialogue.wildercord.speaker." + parts[0], LoreJournal.methodName(parts.length > 1 ? parts[1] : ""));
				yield Component.translatable("dialogue.wildercord.says", speaker, Component.translatable(LoreEntries.lineKey(value)));
			}
			default -> Component.translatableWithFallback(LoreEntries.fixedKey(entry), entry);
		};
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF211242C);
		graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 2, 0xFF62C8C0);
		graphics.centeredText(font, title, width / 2, top + 10, 0xFF9FE1D5);
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		for (Line line : content(tab)) {
			if (line.text().getString().isEmpty()) { wrapped.add(FormattedCharSequence.EMPTY); colors.add(line.color()); continue; }
			for (FormattedCharSequence part : font.split(line.text(), Math.max(80, panelWidth - 32))) { wrapped.add(part); colors.add(line.color()); }
		}
		int visible = Math.max(1, (bodyBottom - bodyTop) / 11);
		int maximum = Math.max(0, wrapped.size() - visible);
		scroll = Math.clamp(scroll, 0, maximum);
		graphics.enableScissor(left + 12, bodyTop, left + panelWidth - 12, bodyBottom);
		for (int index = scroll; index < Math.min(wrapped.size(), scroll + visible); index++) {
			graphics.text(font, wrapped.get(index), left + 16, bodyTop + (index - scroll) * 11, colors.get(index), false);
		}
		graphics.disableScissor();
		if (maximum > 0) graphics.text(font, Component.translatable("screen.wildercord.lore_journal.scroll", scroll + 1, maximum + 1),
			left + panelWidth - 70, top + panelHeight - 20, MUTED, false);
		super.extractRenderState(graphics, mouseX, mouseY, partial);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 3);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (LoreJournalClient.mapping() != null && LoreJournalClient.mapping().matches(event)) {
			onClose();
			return true;
		}
		if (event.key() == InputConstants.KEY_HOME || event.key() == InputConstants.KEY_END) {
			scroll = event.key() == InputConstants.KEY_HOME ? 0 : Integer.MAX_VALUE;
			return true;
		}
		if (event.key() == InputConstants.KEY_PAGEDOWN || event.key() == InputConstants.KEY_DOWN) {
			scroll += event.key() == InputConstants.KEY_PAGEDOWN ? 8 : 1;
			return true;
		}
		if (event.key() == InputConstants.KEY_PAGEUP || event.key() == InputConstants.KEY_UP) {
			scroll = Math.max(0, scroll - (event.key() == InputConstants.KEY_PAGEUP ? 8 : 1));
			return true;
		}
		if (event.key() == InputConstants.KEY_LEFT || event.key() == InputConstants.KEY_RIGHT) {
			int index = TABS.indexOf(tab) + (event.key() == InputConstants.KEY_RIGHT ? 1 : TABS.size() - 1);
			showTab(TABS.get(index % TABS.size()));
			return true;
		}
		return super.keyPressed(event);
	}
}
