package dev.wildercord.client;

import dev.wildercord.aura.MasterFormLessons;
import dev.wildercord.aura.MasterForms;
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
	private final java.util.Map<String, net.minecraft.client.gui.components.Button> controls = new java.util.LinkedHashMap<>();
	public MasterFormsScreen(Screen parent) { this(parent, 0, false); }
	public MasterFormsScreen(Screen parent, long offer, boolean teacher) {
		super(Component.translatable("screen.wildercord.master_forms.title"));
		this.parent = parent; this.offer = offer; this.teacher = teacher; this.page = teacher ? 1 : 0;
	}
	private float scale() {
		double fit = Math.min(1, Math.min((width - 8) / (double) W, (height - 8) / (double) H));
		int gui = Math.max(1, minecraft.getWindow().getGuiScale());
		int physical = (int) Math.floor(gui * fit);
		return physical >= 1 ? physical / (float) gui : (float) fit;
	}
	private int left() { return Math.max(4, Math.round((width - W * scale()) / 2)); }
	private int top() { return Math.max(4, Math.round((height - H * scale()) / 2)); }
	private boolean learned() { return minecraft.player != null && MasterForms.data(minecraft.player).learned(); }
	private Component text(String suffix, Object... args) { return Component.translatable("screen.wildercord.master_forms." + suffix, args); }
	@Override protected void init() {
		super.init();
		controls.clear();
		lastLearned = learned(); lastEquipped = minecraft.player == null ? 0 : MasterForms.data(minecraft.player).equipped();
		if (page == 0 && learned()) control("equip", 16, H - 76, W - 32, text(lastEquipped == 0 ? "equip" : "unequip"),
			() -> MasterFormsClient.send(MasterForms.data(minecraft.player).equipped() == 0 ? WallTurnRules.EQUIP : WallTurnRules.UNEQUIP));
		if (page == 3 && teacher && !learned()) control("accept", 16, H - 76, W - 32, text("accept"), () -> {
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
	}
	/** Screen calls this after both initial construction and a rebuild; choosing in init would advance focus twice. */
	@Override protected void setInitialFocus() {
		setInitialFocus(controls.get(page == 3 && teacher && !learned() ? "accept" : page == 0 && learned() ? "equip" : "next"));
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
		Component body = page == 0 ? text(learned() ? "learned" : "locked") : page == 4 ? text("source_help")
			: Component.translatable("book.wildercord.wall_turn." + page);
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
			g.item(MasterFormLessons.book(), 17, y);
			g.text(font, text("wall_turn"), 40, y + 4, TEXT, false); y += 25;
			y = paragraph(g, text("slot"), y, GOLD);
			y = paragraph(g, text(learned() ? "learned" : "locked"), y + 5, TEXT);
			y = paragraph(g, text("controls", MasterFormsClient.binding()), y + 8, TEXT);
			y = paragraph(g, text("limits"), y + 8, TEXT);
			contentBottom = paragraph(g, text("cost"), y + 8, 0xFFA8CFC0);
		} else if (page == 4) {
			g.centeredText(font, text("source"), W / 2, y, GOLD);
			contentBottom = paragraph(g, text("source_help"), y + 20, TEXT);
		} else {
			g.centeredText(font, text("book_title"), W / 2, y, GOLD); y += 16;
			g.centeredText(font, text("author"), W / 2, y, 0xFF9CB4AD); y += 19;
			y = paragraph(g, Component.translatable("book.wildercord.wall_turn." + page), y, TEXT);
			contentBottom = y;
			if (page == 3) {
				// An instructional diagram is deliberately labelled. The player still practices with production cost and controls.
				int dy = Math.max(y + 6, 177);
				demonstration(g, dy);
				contentBottom = paragraph(g, text("practice", MasterFormsClient.binding()), dy + 42, 0xFFC4D8C8);
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
	/** Native mouse, focus and keyboard tests address the actual registered control. */
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
}
