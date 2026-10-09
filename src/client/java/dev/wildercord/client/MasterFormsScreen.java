package dev.wildercord.client;

import dev.wildercord.aura.MasterFormLessons;
import dev.wildercord.aura.MasterForms;
import dev.wildercord.aura.StoneHingeRules;
import dev.wildercord.aura.WallTurnRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The one Master-form slot, retained lesson and a teacher's illustrated brace/kick/landing demonstration. */
public final class MasterFormsScreen extends Screen implements CordEditorParent {
	private static final int W = 320, H = 340, GOLD = 0xFFE8C46A, TEXT = 0xFFE7EEE8;
	private final Screen parent;
	private final long offer;
	private final boolean teacher;
	private int page;
	private int contentBottom;
	private boolean lastLearned;
	private boolean rebuildPending;
	private int lastEquipped;
	/** The form shown: the teacher's offer, else the equipped or only learned form. Both share the one slot. */
	private int form;
	private final java.util.Map<String, net.minecraft.client.gui.components.Button> controls = new java.util.LinkedHashMap<>();
	public MasterFormsScreen(Screen parent) { this(parent, 0, false, 0); }
	public MasterFormsScreen(Screen parent, long offer, boolean teacher) { this(parent, offer, teacher, MasterForms.WALL_TURN); }
	public MasterFormsScreen(Screen parent, long offer, boolean teacher, int form) {
		super(Component.translatable("screen.wildercord.master_forms.title"));
		this.parent = parent; this.offer = offer; this.teacher = teacher; this.page = teacher ? 1 : 0;
		var player = net.minecraft.client.Minecraft.getInstance().player;
		var data = player == null ? MasterForms.Progress.NONE : MasterForms.data(player);
		this.form = form == MasterForms.WALL_TURN || form == MasterForms.STONE_HINGE ? form : data.equipped() != 0 ? data.equipped()
			: !data.learned() && data.hingeLearned() ? MasterForms.STONE_HINGE : MasterForms.WALL_TURN;
	}
	private boolean hinge() { return form == MasterForms.STONE_HINGE; }
	/** Stone Hinge while the server keeps it in testing: shown locked, never offered for equipping (an equipped one can still come off). */
	private boolean testing() { return hinge() && !MasterForms.testedHinge(minecraft.player); }
	private Component state() { return lesson(testing() ? "testing" : learned() ? "learned" : "locked"); }
	/** Form-specific text; Wall Turn keeps its original keys. */
	private Component lesson(String suffix, Object... args) { return text((hinge() ? "stone_hinge." : "") + suffix, args); }
	private Component bookPage(int page) { return Component.translatable("book.wildercord." + MasterFormLessons.key(form) + "." + page); }
	private boolean both() { return minecraft.player != null && MasterForms.data(minecraft.player).learned() && MasterForms.data(minecraft.player).hingeLearned(); }
	private float scale() {
		double fit = Math.min(1, Math.min((width - 8) / (double) W, (height - 8) / (double) H));
		int gui = Math.max(1, minecraft.getWindow().getGuiScale());
		int physical = (int) Math.floor(gui * fit);
		return physical >= 1 ? physical / (float) gui : (float) fit;
	}
	private int left() { return Math.max(4, Math.round((width - W * scale()) / 2)); }
	private int top() { return Math.max(4, Math.round((height - H * scale()) / 2)); }
	private boolean learned() { return minecraft.player != null && MasterForms.data(minecraft.player).knows(form); }
	private Component text(String suffix, Object... args) { return Component.translatable("screen.wildercord.master_forms." + suffix, args); }
	@Override protected void init() {
		super.init();
		controls.clear();
		lastLearned = learned(); lastEquipped = minecraft.player == null ? 0 : MasterForms.data(minecraft.player).equipped();
		boolean field = page == 0 && minecraft.player != null && dev.wildercord.aura.FormDash.data(minecraft.player).learned() != 0;
		// One row: equip, the Master-form switch once both are learned, and the field-form page. Shared widths keep each layout's old size.
		boolean swap = page == 0 && learned() && both();
		boolean equip = page == 0 && learned() && (!testing() || lastEquipped == form);
		int row = (equip ? 1 : 0) + (swap ? 1 : 0) + (field ? 1 : 0), w = row <= 1 ? W - 32 : row == 2 ? 140 : 92, gap = row == 2 ? 8 : 6;
		int slot = 0;
		if (equip) control("equip", 16 + (w + gap) * slot++, H - 76, w, lastEquipped == form ? text("unequip") : lesson("equip"),
			() -> MasterFormsClient.send(MasterForms.data(minecraft.player).equipped() == form ? WallTurnRules.UNEQUIP : hinge() ? StoneHingeRules.EQUIP : WallTurnRules.EQUIP));
		if (swap) control("form", 16 + (w + gap) * slot++, H - 76, w,
			text("switch", text(hinge() ? "wall_turn" : "stone_hinge")), () -> { form = hinge() ? MasterForms.WALL_TURN : MasterForms.STONE_HINGE; refreshControls(); });
		int fieldX = 16 + (w + gap) * slot;
		if (page == 3 && teacher && !learned()) control("accept", 16, H - 76, W - 32, lesson("accept"), () -> {
			if (ClientPlayNetworking.canSend(MasterFormLessons.Accept.TYPE)) ClientPlayNetworking.send(new MasterFormLessons.Accept(offer));
			page = 0; refreshControls();
		});
		control("previous", 16, H - 49, 89, text("previous"), () -> {
			page = page == 4 && !(learned() || teacher) ? 0 : Math.max(0, page - 1);
			minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
			refreshControls();
		});
		control("next", 115, H - 49, 89, text(page == 4 ? "overview" : learned() || teacher ? "next" : "source"), () -> {
			page = page == 4 ? 0 : learned() || teacher ? (page + 1) % 5 : 4;
			minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
			refreshControls();
		});
		control("back", 214, H - 49, 89, parent != null ? text("back") : Component.translatable("gui.done"), this::onClose);
		// Field forms share this slot; their page is reached from here once one is learned.
		if (field) control("field", fieldX, H - 76, w, text("field"),
			() -> minecraft.gui.setScreen(new FormDashScreen(this, FormDashScreen.preferred(minecraft.player))));
	}
	/** Screen calls this after both initial construction and a rebuild; choosing in init would advance focus twice. */
	@Override protected void setInitialFocus() {
		setInitialFocus(controls.get(page == 3 && teacher && !learned() ? "accept" : controls.containsKey("equip") ? "equip" : "next"));
	}
	private void refreshControls() { rebuildPending = true; }
	@Override public void tick() {
		if (rebuildPending || lastLearned != learned() || minecraft.player != null && lastEquipped != MasterForms.data(minecraft.player).equipped()) {
			rebuildPending = false; rebuildWidgets();
		}
	}
	private void control(String id, int x, int y, int logicalWidth, Component label, Runnable action) {
		float factor = scale();
		var button = new net.minecraft.client.gui.components.Button(left() + Math.round(x * factor), top() + Math.round(y * factor),
			Math.max(1, Math.round(logicalWidth * factor)), Math.max(1, Math.round(20 * factor)), label, ignored -> action.run(),
			supplier -> supplier.get()) {
			@Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float partial) {
				g.pose().pushMatrix(); g.pose().translate(getX(), getY()); g.pose().scale(factor, factor);
				g.fill(0, 0, logicalWidth, 20, isHoveredOrFocused() ? 0xFF587264 : 0xFF2A4648);
				if (isFocused()) { g.fill(0, 0, logicalWidth, 1, GOLD); g.fill(0, 19, logicalWidth, 20, GOLD); }
				g.centeredText(font, getMessage(), logicalWidth / 2, 6, TEXT); g.pose().popMatrix();
			}
		};
		controls.put(id, addRenderableWidget(button));
	}
	@Override public Component getNarrationMessage() {
		Component body = page == 0 ? state() : page == 4 ? lesson("source_help") : bookPage(page);
		return title.copy().append(". ").append(MasterFormsClient.state()).append(". ").append(body);
	}
	@Override public boolean isPauseScreen() { return false; }
	@Override public boolean isInGameUi() { return true; }
	@Override public Screen cordEditorParent() { return parent; }
	@Override public void onClose() { minecraft.gui.setScreen(parent); }
	@Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		g.pose().pushMatrix(); g.pose().translate(left(), top()); g.pose().scale(scale(), scale());
		g.fill(0, 0, W, H, 0xF0182428); g.fill(1, 1, W - 1, 3, GOLD);
		g.centeredText(font, title, W / 2, 12, GOLD);
		g.centeredText(font, font.plainSubstrByWidth(MasterFormsClient.state().getString(), W - 24), W / 2, 28, TEXT);
		int y = 49;
		if (page == 0) {
			g.item(MasterFormLessons.book(form), 17, y);
			g.text(font, text(MasterFormLessons.key(form)), 40, y + 4, TEXT, false); y += 25;
			y = paragraph(g, text("slot"), y, GOLD);
			y = paragraph(g, state(), y + 5, TEXT);
			y = paragraph(g, lesson("controls", MasterFormsClient.binding()), y + 8, TEXT);
			y = paragraph(g, lesson("limits"), y + 8, TEXT);
			contentBottom = paragraph(g, lesson("cost"), y + 8, 0xFFA8CFC0);
		} else if (page == 4) {
			g.centeredText(font, text("source"), W / 2, y, GOLD);
			contentBottom = paragraph(g, lesson("source_help"), y + 20, TEXT);
		} else {
			g.centeredText(font, lesson("book_title"), W / 2, y, GOLD); y += 16;
			g.centeredText(font, lesson("author"), W / 2, y, 0xFF9CB4AD); y += 19;
			y = paragraph(g, bookPage(page), y, TEXT);
			contentBottom = y;
			if (page == 3) {
				// An instructional diagram is deliberately labelled. The player still practices with production cost and controls.
				int dy = Math.max(y + 6, 177);
				demonstration(g, dy);
				contentBottom = paragraph(g, lesson("practice", MasterFormsClient.binding()), dy + 42, 0xFFC4D8C8);
			}
		}
		g.centeredText(font, page == 4 ? text("source") : text("page", page + 1, 4), W / 2, H - 16, 0xFF9CB4AD);
		g.pose().popMatrix();
		super.extractRenderState(g, mouseX, mouseY, partial);
	}

	private int paragraph(GuiGraphicsExtractor g, Component body, int y, int color) {
		for (var line : font.split(body, W - 36)) { g.text(font, line, 18, y, color, false); y += 10; }
		return y;
	}
	/** Three legible states remain visible in reduced presentation, rather than relying on particles or camera motion. */
	private void demonstration(GuiGraphicsExtractor g, int y) {
		if (hinge()) { hingeDemonstration(g, y); return; }
		for (int i = 0; i < 3; i++) {
			int x = 24 + i * 99;
			g.fill(x, y, x + 4, y + 27, 0xFF6B827A); g.fill(x, y + 27, x + 74, y + 30, 0xFF6B827A);
			int bx = x + (i == 0 ? 9 : i == 1 ? 36 : 55), by = y + (i == 2 ? 18 : 7);
			g.fill(bx, by - 4, bx + 5, by + 1, GOLD); g.fill(bx + 1, by + 2, bx + 4, by + 10, TEXT);
			g.fill(bx - 3, by + 4, bx + 9, by + 6, TEXT);
			g.fill(bx - 1, by + 10, bx + 2, by + 14, TEXT); g.fill(bx + 4, by + 10, bx + 7, by + 14, TEXT);
			g.text(font, text("diagram." + i), x, y + 32, TEXT, false);
		}
	}
	/** Planted stance, the frontal blow, and the blow turned aside along the held strafe. */
	private void hingeDemonstration(GuiGraphicsExtractor g, int y) {
		for (int i = 0; i < 3; i++) {
			int x = 24 + i * 99, bx = x + 30, by = y + 7;
			g.fill(x, y + 27, x + 74, y + 30, 0xFF6B827A);
			g.fill(bx, by - 4, bx + 5, by + 1, GOLD); g.fill(bx + 1, by + 2, bx + 4, by + 10, TEXT);
			g.fill(bx - 3, by + 4, bx + 9, by + 6, TEXT);
			g.fill(bx - 3, by + 10, bx, by + 20, TEXT); g.fill(bx + 5, by + 10, bx + 8, by + 20, TEXT);
			if (i >= 1) g.fill(x + 50, by + 4, x + 70, by + 6, 0xFFC86A4A);
			if (i == 2) { g.fill(bx - 1, by + 22, bx + 6, by + 24, GOLD); g.fill(bx + 6, by + 18, bx + 8, by + 26, GOLD); }
			g.text(font, lesson("diagram." + i), x, y + 32, TEXT, false);
		}
	}
	/** Native mouse, focus and keyboard tests address the actual registered control. */
	public double[] point(String action) {
		var button = controls.get(action);
		if (button == null) throw new IllegalArgumentException("No current control: " + action);
		return new double[] {button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0};
	}
	public boolean layoutFits() {
		int buttonsTop = controls.containsKey("equip") || controls.containsKey("accept") || controls.containsKey("field") ? H - 76 : H - 49;
		return contentBottom <= buttonsTop - 2 && left() >= 0 && top() >= 0 && left() + W * scale() <= width && top() + H * scale() <= height;
	}
	public String focusedControl() {
		return controls.entrySet().stream().filter(entry -> entry.getValue().isFocused()).map(java.util.Map.Entry::getKey).findFirst().orElse("");
	}
}
