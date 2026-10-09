package dev.wildercord.client;

import dev.wildercord.aura.FormDash;
import dev.wildercord.aura.FormDashLessons;
import dev.wildercord.aura.FormDashRules;
import dev.wildercord.aura.WallTurnRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A field form's slot control, retained lesson and labelled set/travel/recovery diagram. Same layout rules as Wall Turn's page. */
public final class FormDashScreen extends Screen implements CordEditorParent {
	private static final int W = 320, H = 340, GOLD = 0xFFE8C46A, TEXT = 0xFFE7EEE8;
	private final Screen parent;
	private final long offer;
	private final boolean teacher;
	private int form, page, contentBottom, lastLearned, lastEquipped;
	private boolean rebuildPending;
	private final java.util.Map<String, net.minecraft.client.gui.components.Button> controls = new java.util.LinkedHashMap<>();
	public FormDashScreen(Screen parent, int form) { this(parent, form, 0, false); }
	public FormDashScreen(Screen parent, int form, long offer, boolean teacher) {
		super(Component.translatable("screen.wildercord.field_forms.title"));
		this.parent = parent; this.form = FormDashRules.form(form) ? form : FormDashRules.CINDER_LUNGE; this.offer = offer; this.teacher = teacher; page = teacher ? 1 : 0;
	}
	/** The first learned field form, else Cinder Lunge. */
	public static int preferred(net.minecraft.world.entity.player.Player player) {
		var data = FormDash.data(player);
		if (data.equipped() != 0) return data.equipped();
		for (int form : ORDER) if (data.knows(form)) return form;
		return FormDashRules.CINDER_LUNGE;
	}
	// ---- moves pack
	/** Every field form in page order: the slot's four, then the follow-ups and Spell Cut, which need no slot. */
	private static final int[] ORDER = {FormDashRules.CINDER_LUNGE, FormDashRules.REED_SLIP, FormDashRules.AIR_STEP, FormDashRules.PLUNGE,
		FormDashRules.RIPOSTE, FormDashRules.SHOVE, FormDashRules.GUARD_BREAK, FormDashRules.SPELL_CUT};
	private int learnedCount() { int n = 0; for (int f : ORDER) if (data().knows(f)) n++; return n; }
	private float scale() {
		double fit = Math.min(1, Math.min((width - 8) / (double) W, (height - 8) / (double) H));
		int gui = Math.max(1, minecraft.getWindow().getGuiScale());
		int physical = (int) Math.floor(gui * fit);
		return physical >= 1 ? physical / (float) gui : (float) fit;
	}
	private int left() { return Math.max(4, Math.round((width - W * scale()) / 2)); }
	private int top() { return Math.max(4, Math.round((height - H * scale()) / 2)); }
	private FormDash.Progress data() { return minecraft.player == null ? FormDash.Progress.NONE : FormDash.data(minecraft.player); }
	private boolean learned() { return data().knows(form); }
	private String name() { return FormDash.name(form); }
	private Component text(String suffix, Object... args) { return Component.translatable("screen.wildercord.field_forms." + suffix, args); }
	@Override protected void init() {
		super.init();
		controls.clear();
		lastLearned = data().learned(); lastEquipped = data().equipped();
		boolean both = learnedCount() >= 2 && other() != form;
		if (page == 0 && learned() && FormDashRules.slot(form)) control("equip", 16, H - 76, both ? 140 : W - 32, text(lastEquipped == form ? "unequip" : "equip"),
			() -> FormDashClient.send(data().equipped() == form ? WallTurnRules.UNEQUIP : WallTurnRules.EQUIP, form));
		if (page == 3 && teacher && !learned()) control("accept", 16, H - 76, W - 32, text("accept"), () -> {
			if (ClientPlayNetworking.canSend(FormDashLessons.Accept.TYPE)) ClientPlayNetworking.send(new FormDashLessons.Accept(offer));
			page = 0; refreshControls();
		});
		control("previous", 16, H - 49, 89, text("previous"), () -> { page = page == 4 && !(learned() || teacher) ? 0 : Math.max(0, page - 1); turn(); });
		control("next", 115, H - 49, 89, text(page == 4 ? "overview" : learned() || teacher ? "next" : "source"), () -> {
			page = page == 4 ? 0 : learned() || teacher ? (page + 1) % 5 : 4; turn();
		});
		control("back", 214, H - 49, 89, parent != null ? text("back") : Component.translatable("gui.done"), this::onClose);
		if (page == 0 && both) control("switch", 164, H - 76, 140, text("switch." + FormDash.name(other())), () -> { form = other(); turn(); });
	}
	/** The next learned form after this one, wrapping; Cinder and Reed alone still swap with each other. */
	private int other() {
		int at = 0;
		for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == form) at = i;
		for (int i = 1; i <= ORDER.length; i++) { int f = ORDER[(at + i) % ORDER.length]; if (data().knows(f)) return f; }
		return form;
	}
	private void turn() {
		minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
		refreshControls();
	}
	@Override protected void setInitialFocus() {
		setInitialFocus(controls.get(page == 3 && teacher && !learned() ? "accept" : controls.containsKey("equip") ? "equip" : "next"));
	}
	private void refreshControls() { rebuildPending = true; }
	@Override public void tick() {
		if (rebuildPending || lastLearned != data().learned() || lastEquipped != data().equipped()) { rebuildPending = false; rebuildWidgets(); }
	}
	private void control(String id, int x, int y, int logicalWidth, Component label, Runnable action) {
		float factor = scale();
		var button = new net.minecraft.client.gui.components.Button(left() + Math.round(x * factor), top() + Math.round(y * factor),
			Math.max(1, Math.round(logicalWidth * factor)), Math.max(1, Math.round(20 * factor)), label, ignored -> action.run(),
			supplier -> supplier.get()) {
			@Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float partial) {
				g.pose().pushMatrix(); g.pose().translate(getX(), getY()); g.pose().scale(factor, factor);
				g.fill(0, 0, logicalWidth, 20, isHoveredOrFocused() ? 0xFF6E5A46 : 0xFF46362A);
				if (isFocused()) { g.fill(0, 0, logicalWidth, 1, GOLD); g.fill(0, 19, logicalWidth, 20, GOLD); }
				g.centeredText(font, getMessage(), logicalWidth / 2, 6, TEXT); g.pose().popMatrix();
			}
		};
		controls.put(id, addRenderableWidget(button));
	}
	@Override public Component getNarrationMessage() {
		Component body = page == 0 ? text(name() + (learned() ? ".learned" : ".locked")) : page == 4 ? text("source_help")
			: Component.translatable("book.wildercord." + name() + "." + page);
		return title.copy().append(". ").append(text(name())).append(". ").append(FormDashClient.state()).append(". ").append(body);
	}
	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }
	@Override public Screen cordEditorParent() { return parent; }
	@Override public void onClose() { minecraft.gui.setScreen(parent); }
	@Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		g.pose().pushMatrix(); g.pose().translate(left(), top()); g.pose().scale(scale(), scale());
		g.fill(0, 0, W, H, 0xF0241A16); g.fill(1, 1, W - 1, 3, GOLD);
		g.centeredText(font, title, W / 2, 12, GOLD);
		g.centeredText(font, font.plainSubstrByWidth(FormDashClient.state().getString(), W - 24), W / 2, 28, TEXT);
		int y = 49;
		if (page == 0) {
			g.item(FormDashLessons.book(form), 17, y);
			g.text(font, text(name()), 40, y + 4, TEXT, false); y += 25;
			y = paragraph(g, text(FormDashRules.slot(form) ? "slot" : "free"), y, GOLD);
			y = paragraph(g, text(name() + (learned() ? ".learned" : ".locked")), y + 5, TEXT);
			y = paragraph(g, text(name() + ".controls", MasterFormsClient.binding()), y + 8, TEXT);
			y = paragraph(g, text(name() + ".limits"), y + 8, TEXT);
			contentBottom = paragraph(g, text(name() + ".cost"), y + 8, 0xFFE0C0A0);
		} else if (page == 4) {
			g.centeredText(font, text("source"), W / 2, y, GOLD);
			contentBottom = paragraph(g, text("source_help"), y + 20, TEXT);
		} else {
			g.centeredText(font, text(name() + ".book_title"), W / 2, y, GOLD); y += 16;
			g.centeredText(font, text(name() + ".author"), W / 2, y, 0xFFB4A49C); y += 19;
			y = paragraph(g, Component.translatable("book.wildercord." + name() + "." + page), y, TEXT);
			contentBottom = y;
			if (page == 3) {
				int dy = Math.max(y + 6, 177);
				demonstration(g, dy);
				contentBottom = paragraph(g, text("practice", MasterFormsClient.binding()), dy + 42, 0xFFE0D0C0);
			}
		}
		g.centeredText(font, page == 4 ? text("source") : text("page", page + 1, 4), W / 2, H - 16, 0xFFB4A49C);
		g.pose().popMatrix();
		super.extractRenderState(g, mouseX, mouseY, partial);
	}
	private int paragraph(GuiGraphicsExtractor g, Component body, int y, int color) {
		for (var line : font.split(body, W - 36)) { g.text(font, line, 18, y, color, false); y += 10; }
		return y;
	}
	/** Three labelled states (set or start, travel, recovery) that stay legible without particles or camera motion. */
	private void demonstration(GuiGraphicsExtractor g, int y) {
		boolean cinder = FormDashRules.setTicks(form) > 0;
		for (int i = 0; i < 3; i++) {
			int x = 24 + i * 99;
			g.fill(x, y + 27, x + 74, y + 30, 0xFF7A6A5E);
			int bx = x + (cinder ? i == 0 ? 8 : i == 1 ? 34 : 52 : i == 0 ? 30 : 46), by = y + (i == 0 && cinder ? 11 : 7);
			// Reed Slip: a mark keeps the old line, so the step aside reads without motion.
			if (!cinder && i > 0) g.fill(x + 31, y + 23, x + 33, y + 27, 0xFF9C8C7E);
			g.fill(bx, by - 4, bx + 5, by + 1, GOLD); g.fill(bx + 1, by + 2, bx + 4, by + 10, TEXT);
			g.fill(bx - 3, by + 4, bx + 9, by + 6, TEXT);
			g.fill(bx - 1, by + 10, bx + 2, by + 14 + (by == y + 11 ? -4 : 0), TEXT); g.fill(bx + 4, by + 10, bx + 7, by + 14 + (by == y + 11 ? -4 : 0), TEXT);
			if (cinder && i == 2) g.fill(bx + 9, by + 3, bx + 22, by + 5, GOLD);
			g.text(font, text(name() + ".diagram." + i), x, y + 32, TEXT, false);
		}
	}
	public double[] point(String action) {
		var button = controls.get(action);
		if (button == null) throw new IllegalArgumentException("No current control: " + action);
		return new double[] {button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0};
	}
	public boolean layoutFits() {
		int buttonsTop = controls.containsKey("equip") || controls.containsKey("accept") ? H - 76 : H - 49;
		return contentBottom <= buttonsTop - 2 && left() >= 0 && top() >= 0 && left() + W * scale() <= width && top() + H * scale() <= height;
	}
	public String focusedControl() {
		return controls.entrySet().stream().filter(entry -> entry.getValue().isFocused()).map(java.util.Map.Entry::getKey).findFirst().orElse("");
	}
	public int form() { return form; }
}
