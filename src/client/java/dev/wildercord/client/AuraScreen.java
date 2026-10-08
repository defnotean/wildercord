package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.Techniques;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Aura page: the swordsman's path at a glance. The method (its manual's cover, its element and passive), the stage and the
 * aura held, the road to the next breakthrough (experience, and when it waits, the trials that make it and how far one has
 * got), and every technique: what it does (on hover), what it costs, how it's set off, and the stage it opens at. A second tab
 * beside the techniques lists the sword strings' arts: each one's string drawn in the indicator's marks, its price, its rest,
 * and the stage it opens at, with the marks' meanings at the foot. Opened from the Cord screen's Aura badge (with or without a
 * Cord), drawn in the Cord screen's stone and gold.
 */
public class AuraScreen extends Screen implements CordEditorParent {
	private static final int W = 320;
	private static final int H = 340;
	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_BAR = Wildercord.id("hud/bar_frame");
	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E0F0;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;

	private final Screen parent;
 @Override public Screen cordEditorParent(){return parent;}
	private net.minecraft.client.gui.components.Button masterFormsEntry;
	/** Whether the list shows the sword strings' arts instead of the techniques (kept while the game runs). */
	private static boolean arts;
	/** Whether it shows the Way tree instead (kept while the game runs; wins over {@link #arts}). */
	private static boolean way;
	/** Whether it shows the writing page instead (kept while the game runs; wins over the others). */
	private static boolean writing;
	/** Whether it shows the bonded blade's page instead (kept while the game runs; wins over all of them). */
	private static boolean blade;
	private static boolean lineage;
	private LineagePage lineagePage;
	private int lineageLeft, lineageRight;
	/** Where the tabs were drawn last (the page's own coordinates), for a click: their row, and each one's left and right. */
	private int tabsY = -1;
	private int techLeft;
	private int techRight;
	private int artsLeft;
	private int artsRight;
	private int wayLeft;
	private int wayRight;
	private int writeLeft;
	private int writeRight;
	private int bladeLeft;
	private int bladeRight;
	/** The writing page (techniques of one's own), drawn in place of everything under the tabs while it's open. */
	private TechniquePage page;
	/** The bonded blade's page, drawn in place of everything under the tabs while it's open. */
	private BladePage bladePage;
	/** The Way tree's cells as drawn last (the page's own coordinates: x, y, size), by node id, for a click. */
	private final java.util.Map<String, int[]> cells = new java.util.LinkedHashMap<>();
	/** The node picked on the Way tab (its details shown under the tree), or null for the one that matters most now. */
	private static String picked;

	public AuraScreen(Screen parent) {
		super(Component.translatable("screen.wildercord.aura.title"));
		this.parent = parent;
	}

	/** The control-help label's centre, for native accessibility and presentation tests. */
	public double[] mastersHelpPoint() {
		int labelWidth = font.width(Component.translatable("screen.wildercord.aura.masters_help"));
		return new double[] {left() + (W - 14 - labelWidth / 2.0) * scale(), top() + 14 * scale()};
	}

	@Override
	protected void init() {
		super.init();
		page = new TechniquePage(minecraft, font);
		bladePage = new BladePage(minecraft, font);
		lineagePage = new LineagePage(minecraft, font);
		float s = scale();
		Component forms = Component.translatable("screen.wildercord.master_forms.title");
		// Register the existing label as a native control. Its established painting remains below.
		masterFormsEntry = addWidget(net.minecraft.client.gui.components.Button.builder(forms.copy().append(". ")
			.append(Component.translatable("screen.wildercord.master_forms.open", MasterFormsClient.binding())),
			ignored -> minecraft.gui.setScreen(new MasterFormsScreen(this)))
			.bounds(left() + Math.round(13 * s), top() + Math.round(21 * s), Math.round((font.width(forms) + 4) * s), Math.max(1, Math.round(12 * s))).build());
		// Since 26.x typed characters only arrive while a screen asks for them: the writing page's name is typed here (and the blade's).
		minecraft.textInputManager().startTextInput(this);
	}

	@Override
	public void removed() {
		minecraft.textInputManager().stopTextInput(this);
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (masterFormsEntry != null && masterFormsEntry.isFocused()
				&& (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER
					|| event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE)) return super.keyPressed(event);
		if (bladeOpen() && bladePage.keyPressed(event)) {
			return true;
		}
		if (writingOpen() && page.keyPressed(event)) {
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		setFocused(null);
		if (bladeOpen() && bladePage.charTyped(event)) {
			return true;
		}
		if (writingOpen() && page.charTyped(event)) {
			return true;
		}
		return super.charTyped(event);
	}

	/** Whether the writing page is showing (it's open, and techniques work for the player). */
	private boolean writingOpen() {
		return writing && !lineage && !bladeOpen() && page != null && minecraft.player != null && Techniques.on(minecraft.player)
			&& Aura.stage(minecraft.player) > AuraRules.NONE;
	}

	/** Whether the bonded blade's page is showing (it's open, and bonded blades work for the player). */
	private boolean bladeOpen() {
		return blade && !lineage && bladePage != null && minecraft.player != null && dev.wildercord.aura.BondedBlades.on(minecraft.player)
			&& Aura.stage(minecraft.player) > AuraRules.NONE;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	private float scale() {
		double fit = Math.min(1.0, Math.min((width - 8) / (double) W, (height - 8) / (double) H));
		if (fit >= 1.0) {
			return 1.0F;
		}
		int guiScale = Math.max(1, minecraft.getWindow().getGuiScale());
		int physical = (int) Math.floor(guiScale * fit);
		return physical >= 1 ? physical / (float) guiScale : (float) fit;
	}

	private int left() {
		return Math.max(4, Math.round((width - W * scale()) / 2));
	}

	private int top() {
		return Math.max(4, Math.round((height - H * scale()) / 2));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		float s = scale();
		int mx = (int) Math.floor((mouseX - left()) / s);
		int my = (int) Math.floor((mouseY - top()) / s);
		g.pose().pushMatrix();
		g.pose().translate(left(), top());
		g.pose().scale(s, s);
		List<Component> tooltip = draw(g, mx, my, partial);
		// A fixed, rebinding-aware help label remains visible on every Aura tab, without squeezing
		// three more rows into the stage-dependent technique list.
		Component masters = Component.translatable("screen.wildercord.aura.masters_help");
		int mastersWidth = font.width(masters);
		int mastersX = W - 14 - mastersWidth;
		boolean mastersHover = inside(mx, my, mastersX - 2, 8, mastersWidth + 4, 12);
		g.text(font, masters, mastersX, 10, mastersHover ? GOLD : DIM, true);
		if (mastersHover) tooltip = MastersArtsClient.help();
		Component forms = Component.translatable("screen.wildercord.master_forms.title");
		g.text(font, forms, 15, 23, GOLD, true);
		if (masterFormsEntry != null && masterFormsEntry.isFocused()) {
			g.fill(13, 21, 17 + font.width(forms), 22, GOLD); g.fill(13, 32, 17 + font.width(forms), 33, GOLD);
		}
		if (inside(mx, my, 13, 21, font.width(forms) + 4, 12) || masterFormsEntry != null && masterFormsEntry.isFocused())
			tooltip = List.of(Component.translatable("screen.wildercord.master_forms.open", MasterFormsClient.binding()));
		g.pose().popMatrix();
		if (tooltip != null) {
			g.setTooltipForNextFrame(font, Tooltips.fit(font, tooltip, width, height), mouseX, mouseY);
		}
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (masterFormsEntry != null && masterFormsEntry.isMouseOver(event.x(), event.y())) return super.mouseClicked(event, doubleClick);
		setFocused(null);
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && tabsY >= 0) {
			float s = scale();
			int mx = (int) Math.floor((event.x() - left()) / s);
			int my = (int) Math.floor((event.y() - top()) / s);
			if (!lineage && !bladeOpen() && !writingOpen() && inside(mx,my,14,86,W-28,14) && minecraft.player!=null) {
				net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new dev.wildercord.aura.Unity.Activate());
				minecraft.gui.setScreen(null);
				return true;
			}
			if (my >= tabsY - 2 && my < tabsY + 10) {
				boolean toArts = mx >= artsLeft && mx < artsRight;
				boolean toTech = mx >= techLeft && mx < techRight;
				boolean toWay = wayRight > wayLeft && mx >= wayLeft && mx < wayRight;
				boolean toWrite = writeRight > writeLeft && mx >= writeLeft && mx < writeRight;
				boolean toBlade = bladeRight > bladeLeft && mx >= bladeLeft && mx < bladeRight;
				boolean toLineage = mx >= lineageLeft && mx < lineageRight;
				boolean onBlade = bladeOpen();
				boolean onWrite = writingOpen();
				boolean onArts = arts && !way && !onWrite && !onBlade && !lineage;
				boolean onTech = !arts && !way && !onWrite && !onBlade && !lineage;
				boolean onWay = way && !onWrite && !onBlade && !lineage;
				if (toArts && !onArts || toTech && !onTech || toWay && !onWay || toWrite && !onWrite || toBlade && !onBlade || toLineage && !lineage) {
					arts = toArts;
					way = toWay;
					writing = toWrite;
					blade = toBlade;
					lineage = toLineage;
					if (toWrite) {
						TechniquePage.opened(minecraft.level.getGameTime());
					}
					if (toBlade) {
						BladePage.opened();
					}
					minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
						net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
			if (lineage && lineagePage.click(mx, my)) return true;
			// The blade's page and the writing page: everything under the tabs is their own.
			if (bladeOpen() && bladePage.mouseClicked(minecraft.player, mx, my, event.button())) {
				return true;
			}
			if (writingOpen() && page.mouseClicked(minecraft.player, mx, my, event.button())) {
				return true;
			}
			// A node on the Way tab: its details, kept under the tree.
			if (way) {
				for (java.util.Map.Entry<String, int[]> cell : cells.entrySet()) {
					int[] c = cell.getValue();
					if (inside(mx, my, c[0] - 2, c[1] - 2, c[2] + 4, c[2] + 4)) {
						picked = cell.getKey().equals(picked) ? null : cell.getKey();
						minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
							net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2F));
						return true;
					}
				}
			}
			// A method's swatch on the Sword strings tab: its arts.
			if (arts && chipsY >= 0) {
				for (int i = 0; i < chips.size(); i++) {
					int[] c = chips.get(i);
					if (inside(mx, my, c[0], c[1], c[2], c[3])) {
						String id = chipIds.get(i);
						browsing = id.equals(Aura.data(minecraft.player).method()) ? null : id;
						minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
							net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2F));
						return true;
					}
				}
			}
		}
		// A right click on the writing page (on the string's arrow: the whole string taken back).
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && writingOpen()) {
			float s = scale();
			int mx = (int) Math.floor((event.x() - left()) / s);
			int my = (int) Math.floor((event.y() - top()) / s);
			if (page.mouseClicked(minecraft.player, mx, my, event.button())) {
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Opens the page on its Sword strings tab ({@code true}) or its techniques (the game tests put it back as they found it). */
	public static void listArts(boolean show) {
		lineage = false;
		arts = show;
		way = false;
		writing = false;
		blade = false;
	}

	/** Opens the page on its Way tab ({@code true}), or back on the techniques, with {@code node} picked (null for the default). */
	public static void showWay(boolean show, String node) {
		lineage = false;
		way = show;
		arts = false;
		writing = false;
		blade = false;
		picked = node;
	}

	/** Opens the page on its writing page ({@code true}) at slot {@code slot} (0 the first), or back on the techniques. */
	public static void showWriting(boolean show, int slot) {
		lineage = false;
		writing = show;
		arts = false;
		way = false;
		blade = false;
		if (show) {
			TechniquePage.select(slot);
		}
	}

	/** Opens the page on its Blade tab ({@code true}), or back on the techniques. */
	public static void showBlade(boolean show) {
		lineage = false;
		blade = show;
		arts = false;
		way = false;
		writing = false;
		if (show) {
			BladePage.opened();
		}
	}

	/** Open the lineage record and its two-click release controls. */
	public static void showLineage(boolean show) {
		lineage = show; arts = false; way = false; writing = false; blade = false;
	}
	public double[] lineageTabPoint() {
		if (tabsY < 0 || lineageRight <= lineageLeft) return null;
		return new double[] {left() + (lineageLeft + lineageRight) / 2.0 * scale(), top() + (tabsY + 4) * scale()};
	}
	/** Center of the rendered Unity control, used by actual mouse interaction tests. */
	public double[] unityPoint() {
		return lineage || bladeOpen() || writingOpen()?null:new double[]{left()+W*.5*scale(),top()+93*scale()};
	}
	public double[] lineagePoint(String key) {
		int[] r = lineagePage == null ? null : lineagePage.targets.get(key);
		if (r == null) return null;
		return new double[] {left() + (r[0] + r[2] / 2.0) * scale(), top() + (r[1] + r[3] / 2.0) * scale()};
	}

	/** Whether the page is on its Blade tab (the game tests ask). */
	public static boolean showingBlade() {
		return blade;
	}

	/** The middle of the Blade tab, in screen coordinates (the game tests click it), or null before it's drawn. */
	public double[] bladeTabPoint() {
		if (tabsY < 0 || bladeRight <= bladeLeft) {
			return null;
		}
		float s = scale();
		return new double[] {left() + (bladeLeft + bladeRight) / 2.0 * s, top() + (tabsY + 4) * s};
	}

	/**
	 * The middle of something clickable on the Blade tab, in screen coordinates (the game tests click them): "name", "suggest", "name_it",
	 * "trait:riposte", "release", "blade"; null if it isn't drawn.
	 */
	public double[] bladePoint(String key) {
		if (bladePage == null) {
			return null;
		}
		int[] r = bladePage.targets.get(key);
		if (r == null) {
			return null;
		}
		float s = scale();
		return new double[] {left() + (r[0] + r[2] / 2.0) * s, top() + (r[1] + r[3] / 2.0) * s};
	}

	/** Whether the page is on its writing page (the game tests ask). */
	public static boolean showingWriting() {
		return writing;
	}

	/** The middle of the writing tab, in screen coordinates (the game tests click it), or null before it's drawn. */
	public double[] writingTabPoint() {
		if (tabsY < 0 || writeRight <= writeLeft) {
			return null;
		}
		float s = scale();
		return new double[] {left() + (writeLeft + writeRight) / 2.0 * s, top() + (tabsY + 4) * s};
	}

	/**
	 * The middle of something clickable on the writing page, in screen coordinates (the game tests click them): "slot:0", "name",
	 * "seal:stroke", "part:thrust", "token:low", "back", "write", "erase", "temper:swift", "edge:long", "inscribe:wave"; null if it isn't
	 * drawn.
	 */
	public double[] writingPoint(String key) {
		if (page == null) {
			return null;
		}
		int[] r = page.targets.get(key);
		if (r == null) {
			return null;
		}
		float s = scale();
		return new double[] {left() + (r[0] + r[2] / 2.0) * s, top() + (r[1] + r[3] / 2.0) * s};
	}

	/** Whether the page is on its Way tab (the game tests ask). */
	public static boolean showingWay() {
		return way;
	}

	/** The middle of the Way tab, in screen coordinates (the game tests click it), or null before it's drawn. */
	public double[] wayTabPoint() {
		if (tabsY < 0 || wayRight <= wayLeft) {
			return null;
		}
		float s = scale();
		return new double[] {left() + (wayLeft + wayRight) / 2.0 * s, top() + (tabsY + 4) * s};
	}

	/** The middle of {@code methodId}'s swatch on the Sword strings tab, in screen coordinates (the game tests click it), or null. */
	public double[] chipPoint(String methodId) {
		int i = chipIds.indexOf(methodId);
		if (i < 0) {
			return null;
		}
		int[] c = chips.get(i);
		float s = scale();
		return new double[] {left() + (c[0] + c[2] / 2.0) * s, top() + (c[1] + c[3] / 2.0) * s};
	}

	/** The middle of the Sword strings tab, in screen coordinates (the game tests click it), or null before it's drawn. */
	public double[] artsTabPoint() {
		if (tabsY < 0) {
			return null;
		}
		float s = scale();
		return new double[] {left() + (artsLeft + artsRight) / 2.0 * s, top() + (tabsY + 4) * s};
	}

	/** One line of the techniques list. */
	private record Row(Component name, int stage, Component how, double cost, Component desc) {}

	private List<Component> draw(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PANEL, 0, 0, W, H);
		LocalPlayer player = minecraft.player;
		g.text(font, title, 13, 10, GOLD, true);
		if (player == null) {
			return null;
		}
		if (!Aura.enabled(player)) {
			g.centeredText(font, Component.translatable("screen.wildercord.aura.disabled"), W / 2, H / 2 - 4, TEXT);
			return null;
		}
		BreathingMethod method = Aura.method(player).orElse(null);
		int stage = Aura.stage(player);
		if (method == null || stage <= AuraRules.NONE) {
			g.centeredText(font, Component.translatable("screen.wildercord.aura.none"), W / 2, 96, TEXT);
			int y = 114;
			for (net.minecraft.util.FormattedCharSequence line : font.split(Component.translatable("screen.wildercord.aura.none_hint"), W - 60)) {
				g.centeredText(font, line, W / 2, y, DIM);
				y += 10;
			}
			// Every method's cover, in a row: what there is to find.
			List<BreathingMethod> all = dev.wildercord.aura.BreathingMethods.BUILT_IN;
			// Two rows of five, twice their size.
			List<Component> tip = null;
			for (int i = 0; i < all.size(); i++) {
				BreathingMethod m = all.get(i);
				int x = W / 2 - 5 * 18 + (i % 5) * 36;
				int iy = y + 14 + (i / 5) * 38;
				g.pose().pushMatrix();
				g.pose().translate(x + 2, iy);
				g.pose().scale(2, 2);
				g.item(BreathingManualItem.of(m.id()), 0, 0);
				g.pose().popMatrix();
				if (inside(mx, my, x + 2, iy, 32, 32)) {
					tip = List.of(Component.translatable(m.nameKey()).withColor(m.color()),
						Component.translatable(m.nameKey() + ".flavour").withStyle(ChatFormatting.GRAY));
				}
			}
			return tip;
		}
		List<Component> tooltip = null;
		int color = 0xFF000000 | Aura.color(player);
		AuraAttachments.Data data = Aura.data(player);
		AuraAttachments.State state = Aura.state(player);
		long now = player.level().getGameTime();
		if (lineage || bladeOpen()) {
			return bladeView(g, player, method, stage, mx, my, partial, color);
		}
		if (writingOpen()) {
			return writingPage(g, player, method, stage, mx, my, partial, color);
		}

		// ---- the method: its cover, large, in a frame of its colour.
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_INSET, 12, 26, 58, 58);
		g.fill(13, 27, 69, 28, color);
		g.fill(13, 82, 69, 83, color);
		g.pose().pushMatrix();
		g.pose().translate(17, 31);
		g.pose().scale(3, 3);
		g.item(BreathingManualItem.of(method.id()), 0, 0);
		g.pose().popMatrix();
		if (inside(mx, my, 12, 26, 58, 58)) {
			tooltip = List.of(Component.translatable(method.nameKey()).withColor(method.color()),
				Component.translatable(method.nameKey() + ".lore").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
		}
		int tx = 80;
		g.text(font, Component.translatable("screen.wildercord.aura.method", Component.translatable(method.nameKey()).withColor(color)), tx, 28, TEXT, true);
		if (!method.element().isEmpty()) {
			g.text(font, Component.translatable("screen.wildercord.aura.element",
				Component.translatable("element.wildercord." + method.element()).withColor(dev.wildercord.spell.RuneColors.element(method.element()))), tx, 40, TEXT, true);
		}
		Component stageName = Component.translatable("aura.wildercord.stage." + AuraStages.id(stage)).withColor(color);
		g.text(font, Component.translatable("screen.wildercord.aura.stage", stageName), tx, 52, TEXT, true);
		int stageW = font.width(Component.translatable("screen.wildercord.aura.stage", stageName));
		if (inside(mx, my, tx, 51, stageW, 10)) {
			tooltip = List.of(stageName, Component.translatable("aura.wildercord.stage." + AuraStages.id(stage) + ".desc").withStyle(ChatFormatting.GRAY));
		}
		// The aura held: a bar in its colour.
		int capacity = Math.max(1, Aura.capacity(player));
		float held = Aura.aura(player);
		g.text(font, Component.translatable("screen.wildercord.aura.held", (int) held, capacity), tx, 64, TEXT, true);
		bar(g, tx, 75, W - tx - 14, held / capacity, color);
		// Awakening as it stands (from Edge), at the end of the same line: burning, spent, resting, ready, or what it waits on.
		Component waking = awakening(player, stage, now);
		if (waking != null) {
			int ww = font.width(waking);
			int wx = W - 14 - ww;
			dev.wildercord.aura.Awakening.State ws = dev.wildercord.aura.Awakening.state(player);
			int wc = ws.awakened(now) ? 0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.35 + 0.25 * Math.sin((now + partial) * 0.4))
				: ws.spent(now) ? 0xFFC8A0A0 : dev.wildercord.aura.Awakening.ready(player) ? GOLD : DIM;
			g.text(font, waking, wx, 64, wc, true);
			if (inside(mx, my, wx, 63, ww, 10)) {
				tooltip = List.of(Component.translatable("aura.wildercord.technique.awaken").withColor(color),
					Component.translatable("aura.wildercord.technique.awaken.desc").withStyle(ChatFormatting.GRAY));
			}
		}

		// ---- the road to the next stage.
		var unity=dev.wildercord.aura.Unity.state(player);
		String refusal=dev.wildercord.aura.Unity.refusal(player);
		boolean together=dev.wildercord.aura.Unity.active(player);
		g.blitSprite(RenderPipelines.GUI_TEXTURED,SPR_INSET,14,86,W-28,14);
		g.blitSprite(RenderPipelines.GUI_TEXTURED,Wildercord.id("aura/unity"),17,87,12,12);
		Component unityText=together?Component.translatable("screen.wildercord.unity.active",(unity.until()-now+19)/20)
			: now<unity.readyAt()?Component.translatable("screen.wildercord.unity.rest",(unity.readyAt()-now+19)/20)
			: Component.translatable("screen.wildercord.unity.start");
		g.text(font,unityText,33,89,together || refusal==null?GOLD:DIM,true);
		if(inside(mx,my,14,86,W-28,14))tooltip=List.of(Component.translatable("screen.wildercord.unity.desc"),
			Component.translatable(refusal==null?"message.wildercord.unity.ready":"message.wildercord.unity."+refusal).withStyle(ChatFormatting.GRAY));
		int y = 108;
		int next = stage + 1;
		int cap = AuraStages.cap(stage);
		boolean open = AuraStages.canBreakThrough(stage);
		if (cap < 0) {
			g.text(font, Component.translatable("screen.wildercord.aura.xp_max", (int) data.xp()), 14, y, DIM, true);
		} else {
			Component nextName = Component.translatable("aura.wildercord.stage." + AuraStages.id(next));
			g.text(font, Component.translatable(open ? "screen.wildercord.aura.road" : "screen.wildercord.aura.road_closed", nextName), 14, y, GOLD, true);
			String xp = String.format(Locale.ROOT, "%d / %d", (int) data.xp(), cap);
			g.text(font, xp, W - 14 - font.width(xp), y, DIM, true);
			double from = AuraStages.threshold(stage);
			bar(g, 14, y + 11, W - 28, AuraRules.progress(data.xp(), from, cap), 0xFFB8A8FF);
			if (inside(mx, my, 14, y, W - 28, 20)) {
				tooltip = List.of(Component.translatable("screen.wildercord.aura.progress_help").withColor(GOLD),
					Component.translatable("screen.wildercord.aura.xp_help").withStyle(ChatFormatting.GRAY),
					Component.translatable("screen.wildercord.aura.practice_help", trim(data.practice()), trim(AuraRules.PRACTICE_CAP))
						.withStyle(ChatFormatting.GRAY),
					Component.translatable("screen.wildercord.aura.threshold_help").withStyle(ChatFormatting.GRAY));
			}
		}
		y += 22;
		if (cap >= 0 && !AuraBreakthroughs.ready(player)) {
			String hint = !Aura.enabled(player) ? "disabled" : !Aura.holdsWeapon(player) ? "weapon"
				: data.practice() >= AuraRules.PRACTICE_CAP ? "practice_full" : "fight";
			g.text(font, Component.translatable("screen.wildercord.aura.progress_" + hint), 14, y, DIM, false);
			y += 12;
		}
		if (AuraBreakthroughs.ready(player)) {
			double pulse = 0.5 + 0.5 * Math.sin((now + partial) * 0.25);
			g.text(font, Component.translatable("screen.wildercord.aura.ready"), 14, y, 0xFF000000 | AuraHud.mix(0xC8A050, 0xFFF0B0, pulse), true);
			y += 11;
			for (String trial : AuraApi.trials(next)) {
				// A trial of a later wave's own names itself in its own language key.
				Component line = Component.translatable("screen.wildercord.aura.trial." + trial.replace(':', '.'));
				g.fill(16, y + 2, 19, y + 5, color);
				for (net.minecraft.util.FormattedCharSequence part : font.split(line, W - 40)) {
					g.text(font, part, 22, y, TEXT, false);
					y += 10;
				}
			}
			if (state.stillness() > 0) {
				g.text(font, Component.translatable("screen.wildercord.aura.trial.progress", state.stillness() / 20, AuraRules.stillnessTicks(next) / 20),
					22, y, color, true);
				y += 10;
			} else if (state.trialUntil() > now) {
				g.text(font, Component.translatable("screen.wildercord.aura.trial.foe_left", (state.trialUntil() - now + 19) / 20), 22, y, color, true);
				y += 10;
			}
			y += 2;
		}

		// ---- the techniques, or (the second tab) the sword strings' arts, or (the third, from Edge or where Ways are on) the Way tree.
		y = tabs(g, y, mx, my, color);
		if (way && wayRight > wayLeft) {
			List<Component> tip = way(g, player, y, mx, my, color, stage, now);
			if (tip != null) {
				tooltip = tip;
			}
		} else if (arts) {
			List<Component> tip = arts(g, player, y, mx, my, color, stage, now);
			if (tip != null) {
				tooltip = tip;
			}
		} else for (Row row : rows()) {
			boolean reached = stage >= row.stage();
			int rowColor = reached ? TEXT : FAINT;
			int dot = reached ? color : 0xFF3A3450;
			g.fill(16, y + 2, 20, y + 6, dot);
			g.text(font, row.name(), 24, y, rowColor, false);
			Component how = reached ? row.how() : Component.translatable("screen.wildercord.aura.locked",
				Component.translatable("aura.wildercord.stage." + AuraStages.id(row.stage())));
			g.text(font, how, 120, y, reached ? DIM : FAINT, false);
			if (row.cost() > 0) {
				String cost = Component.translatable("screen.wildercord.aura.cost", trim(row.cost())).getString();
				g.text(font, cost, W - 14 - font.width(cost), y, reached ? 0xFFB8A8FF : FAINT, false);
			}
			if (inside(mx, my, 14, y - 1, W - 28, 10)) {
				List<Component> tip = new ArrayList<>();
				tip.add(row.name().copy().withColor(reached ? color : DIM));
				tip.add(row.desc().copy().withStyle(ChatFormatting.GRAY));
				if (!reached) {
					tip.add(how.copy().withStyle(ChatFormatting.DARK_GRAY));
				}
				tooltip = tip;
			}
			y += 10;
			if (y > H - 40) {
				break;
			}
		}
		// ---- the passive, at the foot (the Way tab uses the room for its nodes in full).
		if (way && wayRight > wayLeft) {
			return tooltip;
		}
		Component flavour = Component.translatable(method.nameKey() + ".flavour");
		int fy = H - 30;
		g.text(font, Component.translatable("screen.wildercord.aura.flavour"), 14, fy, GOLD, true);
		for (net.minecraft.util.FormattedCharSequence line : font.split(flavour, W - 28 - font.width(Component.translatable("screen.wildercord.aura.flavour")) - 6)) {
			g.text(font, line, 20 + font.width(Component.translatable("screen.wildercord.aura.flavour")), fy, 0xFFB8A8D8, false);
			fy += 10;
		}
		return tooltip;
	}

	/**
	 * The tabs over the list: Techniques, Sword strings and (where Ways are on) Way, the open one in gold and underlined, the Way tab
	 * breathing gold while a swordsman past the crossroads walks none. Returns where the list starts.
	 */
	private int tabs(GuiGraphicsExtractor g, int y, int mx, int my, int color) {
		Component tech = Component.translatable("screen.wildercord.aura.skills_tab");
		Component strings = Component.translatable("screen.wildercord.aura.arts_tab");
		Component wayTab = Component.translatable("screen.wildercord.aura.way");
		Component writeTab = Component.translatable("screen.wildercord.aura.writing");
		Component bladeTab = Component.translatable("screen.wildercord.aura.blade");
		boolean ways = dev.wildercord.aura.Ways.on(minecraft.player) && !dev.wildercord.api.AuraApi.ways().isEmpty();
		boolean writes = Techniques.on(minecraft.player);
		boolean blades = dev.wildercord.aura.BondedBlades.on(minecraft.player);
		boolean onBlade = blade && blades && !lineage;
		boolean onWrite = writing && writes && !onBlade && !lineage;
		boolean onWay = way && ways && !onWrite && !onBlade && !lineage;
		boolean onArts = arts && !onWay && !onWrite && !onBlade && !lineage;
		Component lineageTab = Component.translatable("screen.wildercord.aura.lineage.tab");
		int totalTabWidth = font.width(tech) + font.width(strings)
			+ (ways ? font.width(wayTab) : 0)
			+ (writes ? font.width(writeTab) : 0)
			+ (blades ? font.width(bladeTab) : 0)
			+ font.width(lineageTab);
		int tabCount = 2 + (ways ? 1 : 0) + (writes ? 1 : 0) + (blades ? 1 : 0) + 1;
		int gap = tabCount > 1 ? Math.max(4, Math.min(14, ((W - 28) - totalTabWidth) / (tabCount - 1))) : 14;
		int divOffset = gap / 2;
		tabsY = y;
		techLeft = 14;
		techRight = techLeft + font.width(tech);
		artsLeft = techRight + gap;
		artsRight = artsLeft + font.width(strings);
		wayLeft = artsRight + gap;
		wayRight = ways ? wayLeft + font.width(wayTab) : wayLeft;
		writeLeft = (ways ? wayRight : artsRight) + gap;
		writeRight = writes ? writeLeft + font.width(writeTab) : writeLeft;
		bladeLeft = (writes ? writeRight : ways ? wayRight : artsRight) + gap;
		bladeRight = blades ? bladeLeft + font.width(bladeTab) : bladeLeft;
		lineageLeft = (blades ? bladeRight : writes ? writeRight : ways ? wayRight : artsRight) + gap;
		lineageRight = lineageLeft + font.width(lineageTab);
		g.text(font, lineageTab, lineageLeft, y, lineage ? GOLD : DIM, true);
		boolean overTech = inside(mx, my, techLeft, y - 2, techRight - techLeft, 12);
		boolean overArts = inside(mx, my, artsLeft, y - 2, artsRight - artsLeft, 12);
		boolean overWay = ways && inside(mx, my, wayLeft, y - 2, wayRight - wayLeft, 12);
		boolean overWrite = writes && inside(mx, my, writeLeft, y - 2, writeRight - writeLeft, 12);
		boolean overBlade = blades && inside(mx, my, bladeLeft, y - 2, bladeRight - bladeLeft, 12);
		g.text(font, tech, techLeft, y, !onArts && !onWay && !onWrite && !onBlade && !lineage ? GOLD : overTech ? TEXT : DIM, true);
		g.text(font, strings, artsLeft, y, onArts ? GOLD : overArts ? TEXT : DIM, true);
		g.fill(techRight + divOffset - 1, y + 1, techRight + divOffset, y + 8, FAINT);
		if (ways) {
			int wayColor = onWay ? GOLD : overWay ? TEXT : DIM;
			if (!onWay && dev.wildercord.aura.Ways.wayless(minecraft.player)) {
				// Past the crossroads with no Way: the tab breathes gold.
				double pulse = 0.5 + 0.5 * Math.sin(minecraft.level.getGameTime() * 0.2);
				wayColor = 0xFF000000 | AuraHud.mix(0x8A84A0, 0xFFE8A0, pulse);
			}
			g.text(font, wayTab, wayLeft, y, wayColor, true);
			g.fill(artsRight + divOffset - 1, y + 1, artsRight + divOffset, y + 8, FAINT);
		}
		if (writes) {
			int writeColor = onWrite ? GOLD : overWrite ? TEXT : DIM;
			if (!onWrite && Techniques.slots(minecraft.player) > 0 && Techniques.book(minecraft.player).slots().stream().allMatch(Techniques.Written::empty)) {
				// An empty slot waiting at Edge and nothing written yet: the tab breathes gold too.
				double pulse = 0.5 + 0.5 * Math.sin(minecraft.level.getGameTime() * 0.2);
				writeColor = 0xFF000000 | AuraHud.mix(0x8A84A0, 0xFFE8A0, pulse);
			}
			g.text(font, writeTab, writeLeft, y, writeColor, true);
			g.fill(writeLeft - divOffset - 1, y + 1, writeLeft - divOffset, y + 8, FAINT);
		}
		if (blades) {
			int bladeColor = onBlade ? GOLD : overBlade ? TEXT : DIM;
			if (!onBlade && waitsOnBlade()) {
				// A trait waits to be chosen (or a blade can be bonded at Edge and none is): the tab breathes gold.
				double pulse = 0.5 + 0.5 * Math.sin(minecraft.level.getGameTime() * 0.2);
				bladeColor = 0xFF000000 | AuraHud.mix(0x8A84A0, 0xFFE8A0, pulse);
			}
			g.text(font, bladeTab, bladeLeft, y, bladeColor, true);
			g.fill(bladeLeft - divOffset - 1, y + 1, bladeLeft - divOffset, y + 8, FAINT);
		}
		g.fill(lineageLeft - divOffset - 1, y + 1, lineageLeft - divOffset, y + 8, FAINT);
		int under = lineage ? lineageLeft : onBlade ? bladeLeft : onWrite ? writeLeft : onWay ? wayLeft : onArts ? artsLeft : techLeft;
		int underRight = lineage ? lineageRight : onBlade ? bladeRight : onWrite ? writeRight : onWay ? wayRight : onArts ? artsRight : techRight;
		g.fill(under, y + 9, underRight, y + 10, 0xFF000000 | (GOLD & 0xFFFFFF));
		return y + 13;
	}

	/** Whether the bonded blade asks something of its swordsman now: a trait to choose. */
	private boolean waitsOnBlade() {
		ItemStack blade = dev.wildercord.aura.BondedBlades.carried(minecraft.player);
		dev.wildercord.aura.BladeBond b = dev.wildercord.aura.BondedBlades.bond(blade);
		return b != null && b.growth().trait().isEmpty() && !b.growth().offer().isEmpty()
			&& dev.wildercord.aura.BladeRules.effective(b.tier(), Aura.stage(minecraft.player)) >= dev.wildercord.aura.BladeRules.AWAKENED;
	}

	/** The blade's page: the method, stage and aura in one line by the title (as the writing page), the tabs, then the page ({@link BladePage}). */
	private List<Component> bladeView(GuiGraphicsExtractor g, LocalPlayer player, BreathingMethod method, int stage, int mx, int my, float partial, int color) {
		Component stageName = Component.translatable("aura.wildercord.stage." + AuraStages.id(stage));
		Component line = Component.translatable("screen.wildercord.aura.writing.header", Component.translatable(method.nameKey()), stageName,
			(int) Aura.aura(player), Aura.capacity(player));
		int lw = (int) (font.width(line) * 0.8F);
		g.pose().pushMatrix();
		g.pose().translate(W - 14 - lw, 11);
		g.pose().scale(0.8F, 0.8F);
		g.text(font, line, 0, 0, color, false);
		g.pose().popMatrix();
		tabs(g, 24, mx, my, color);
		return lineage ? lineagePage.draw(g, player, mx, my, 38) : bladePage.draw(g, player, mx, my, partial, color & 0xFFFFFF, 38);
	}

	/**
	 * The writing page: the method, stage and aura in one line by the title (the page needs the room the header takes on the other tabs),
	 * the tabs under it, then the page itself ({@link TechniquePage}).
	 */
	private List<Component> writingPage(GuiGraphicsExtractor g, LocalPlayer player, BreathingMethod method, int stage, int mx, int my, float partial,
			int color) {
		Component stageName = Component.translatable("aura.wildercord.stage." + AuraStages.id(stage));
		Component line = Component.translatable("screen.wildercord.aura.writing.header", Component.translatable(method.nameKey()), stageName,
			(int) Aura.aura(player), Aura.capacity(player));
		int lw = (int) (font.width(line) * 0.8F);
		g.pose().pushMatrix();
		g.pose().translate(W - 14 - lw, 11);
		g.pose().scale(0.8F, 0.8F);
		g.text(font, line, 0, 0, color, false);
		g.pose().popMatrix();
		tabs(g, 24, mx, my, color);
		return page.draw(g, player, mx, my, partial, color & 0xFFFFFF);
	}

	// ------------------------------------------------------------------ the Way tab

	/** The Way tree's rows apart (a node stage each). */
	private static final int ROW = 22;

	/** A Way's emblem sprite (an add-on's Way, without one of its own, gets the plain one). */
	private static Identifier emblem(String wayId) {
		return dev.wildercord.aura.WayRules.BUILT_IN.contains(wayId) ? Wildercord.id("aura/way_" + wayId) : Wildercord.id("aura/way_unknown");
	}

	/**
	 * The Way tree: what the swordsman walks (or how to choose one), then a column for each Way (its emblem and name) with its three
	 * nodes under it, one row a stage (Edge, Form, Sovereign), each node drawn as it stands: in force (lit, framed in gold), waking after
	 * a change (half lit, filling), still to come (outlined in its Way's colour), another Way's (dark), or open to choose. Under the tree,
	 * the picked node in full: its name, its stage, where it stands, its passive and what it changes. At the foot, how to change Way.
	 */
	private List<Component> way(GuiGraphicsExtractor g, LocalPlayer player, int y, int mx, int my, int color, int stage, long now) {
		List<Component> tooltip = null;
		List<AuraApi.Way> ways = AuraApi.ways();
		ways = ways.subList(0, Math.min(6, ways.size()));
		dev.wildercord.aura.Ways.State state = dev.wildercord.aura.Ways.state(player);
		AuraApi.Way walking = dev.wildercord.aura.Ways.way(player).orElse(null);
		// ---- what they walk.
		if (walking != null) {
			int wc = 0xFF000000 | walking.color();
			g.text(font, Component.translatable("screen.wildercord.aura.way.walking", Component.translatable(walking.nameKey()).withColor(wc)), 14, y, TEXT, true);
			y += 11;
			g.text(font, font.plainSubstrByWidth(Component.translatable(walking.creedKey()).getString(), W - 30), 18, y, 0xFFB8A8D8, false);
			y += 12;
		} else {
			Component line = Component.translatable(stage >= dev.wildercord.aura.WayRules.FROM ? "screen.wildercord.aura.way.none_yet"
				: "screen.wildercord.aura.way.before_edge", Component.translatable("aura.wildercord.stage." + AuraStages.id(dev.wildercord.aura.WayRules.FROM)));
			int lc = stage >= dev.wildercord.aura.WayRules.FROM ? 0xFF000000 | AuraHud.mix(0xC8A050, 0xFFF0B0, 0.5 + 0.5 * Math.sin(now * 0.2)) : TEXT;
			for (net.minecraft.util.FormattedCharSequence part : font.split(line, W - 28)) {
				g.text(font, part, 14, y, lc, true);
				y += 10;
			}
			y += 2;
		}
		// ---- the tree: a column a Way, a row a node stage.
		int rowLabel = 14;
		int gridLeft = 58;
		int colW = (W - 14 - gridLeft) / Math.max(1, ways.size());
		int cell = 18;
		int headerY = y;
		cells.clear();
		for (int i = 0; i < ways.size(); i++) {
			AuraApi.Way w = ways.get(i);
			int cx = gridLeft + colW * i + colW / 2;
			boolean mine = walking != null && walking.id().equals(w.id());
			boolean other = walking != null && !mine;
			int wc = w.color();
			if (mine) {
				// The chosen column: a soft band of its colour behind it.
				g.fill(cx - colW / 2 + 2, headerY - 2, cx + colW / 2 - 2, headerY + 24 + 3 * ROW, 0x1E000000 | (wc & 0xFFFFFF));
			}
			int tint = other ? 0xFF000000 | AuraHud.mix(wc, 0x2A2438, 0.65) : 0xFF000000 | wc;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, emblem(w.id()), cx - 8, headerY, 16, 16, tint);
			Component shortName = Component.translatable(w.nameKey() + ".short");
			String fitted = font.plainSubstrByWidth(shortName.getString(), colW - 2);
			g.pose().pushMatrix();
			g.pose().translate(cx, headerY + 17);
			g.pose().scale(0.75F, 0.75F);
			g.centeredText(font, fitted, 0, 0, other ? FAINT : mine ? 0xFF000000 | AuraHud.mix(wc, 0xFFFFFF, 0.35) : TEXT);
			g.pose().popMatrix();
			if (inside(mx, my, cx - colW / 2, headerY - 2, colW, 24)) {
				List<Component> tip = new ArrayList<>();
				tip.add(Component.translatable(w.nameKey()).withColor(wc));
				tip.add(Component.translatable(w.creedKey()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
				tooltip = tip;
			}
		}
		int rowsY = headerY + 26;
		int[] stages = dev.wildercord.aura.WayRules.NODE_STAGES;
		String auto = null;
		for (int r = 0; r < stages.length; r++) {
			int ry = rowsY + r * ROW;
			Component label = Component.translatable("aura.wildercord.stage." + AuraStages.id(stages[r]));
			g.pose().pushMatrix();
			g.pose().translate(rowLabel, ry + 6);
			g.pose().scale(0.8F, 0.8F);
			g.text(font, font.plainSubstrByWidth(label.getString(), Math.round((gridLeft - rowLabel - 2) / 0.8F)), 0, 0, stage >= stages[r] ? TEXT : FAINT, false);
			g.pose().popMatrix();
			for (int i = 0; i < ways.size(); i++) {
				AuraApi.Way w = ways.get(i);
				AuraApi.WayNode node = w.node(stages[r]).orElse(null);
				if (node == null) {
					continue;
				}
				int cx = gridLeft + colW * i + colW / 2;
				int x0 = cx - cell / 2;
				dev.wildercord.aura.WayRules.NodeState ns = dev.wildercord.aura.Ways.nodeState(player, w, node);
				if (r > 0) {
					// The line down the column from the node above.
					boolean lit = ns == dev.wildercord.aura.WayRules.NodeState.CHOSEN;
					g.fill(cx, ry - (ROW - cell) + 2, cx + 1, ry - 1, lit ? 0xFF000000 | w.color() : 0xFF3A3450);
				}
				node(g, ns, w, node, x0, ry, cell, state, now);
				cells.put(node.id(), new int[] {x0, ry, cell});
				if (auto == null && (ns == dev.wildercord.aura.WayRules.NodeState.UPCOMING || ns == dev.wildercord.aura.WayRules.NodeState.WAKING)) {
					auto = node.id();
				}
				if (inside(mx, my, x0 - 2, ry - 2, cell + 4, cell + 4)) {
					List<Component> tip = new ArrayList<>();
					tip.add(Component.translatable(node.nameKey()).withColor(w.color()));
					tip.add(Component.translatable("screen.wildercord.aura.way.state." + ns.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY));
					tooltip = tip;
				}
			}
		}
		// ---- the picked node, in full.
		String shown = picked != null && cells.containsKey(picked) ? picked : auto != null ? auto : walking != null ? walking.nodes().getFirst().id()
			: ways.getFirst().nodes().getFirst().id();
		int dy = rowsY + stages.length * ROW + 1;
		for (AuraApi.Way w : ways) {
			for (AuraApi.WayNode node : w.nodes()) {
				if (!node.id().equals(shown)) {
					continue;
				}
				int[] c = cells.get(node.id());
				if (c != null) {
					// A gold corner round the node shown below.
					g.fill(c[0] - 2, c[1] - 2, c[0] + c[2] + 2, c[1] - 1, GOLD);
					g.fill(c[0] - 2, c[1] + c[2] + 1, c[0] + c[2] + 2, c[1] + c[2] + 2, GOLD);
				}
				dy = details(g, player, w, node, state, dy, stage);
			}
		}
		// ---- how to change it: small, at the foot, drawn whole or not at all (never a sentence cut off at the frame).
		Component how = Component.translatable(walking != null ? "screen.wildercord.aura.way.change" : "screen.wildercord.aura.way.how",
			Component.translatable("item.wildercord.crossroads_incense"));
		float small = 0.8F;
		List<net.minecraft.util.FormattedCharSequence> howLines = font.split(how, Math.round((W - 30) / small));
		int fy = Math.max(dy + 3, H - 18 - (howLines.size() - 1) * 8);
		if (fy + (howLines.size() - 1) * 8 <= H - 18) {
			for (net.minecraft.util.FormattedCharSequence part : howLines) {
				g.pose().pushMatrix();
				g.pose().translate(16, fy);
				g.pose().scale(small, small);
				g.text(font, part, 0, 0, FAINT, false);
				g.pose().popMatrix();
				fy += 8;
			}
		}
		return tooltip;
	}

	/** One node's cell: framed and lit by how it stands for the swordsman. */
	private void node(GuiGraphicsExtractor g, dev.wildercord.aura.WayRules.NodeState ns, AuraApi.Way w, AuraApi.WayNode node, int x, int y, int size,
			dev.wildercord.aura.Ways.State state, long now) {
		int wc = w.color() & 0xFFFFFF;
		int frame;
		int fill;
		int tint;
		switch (ns) {
			case CHOSEN -> {
				frame = GOLD;
				fill = 0xFF000000 | AuraHud.mix(wc, 0x1A1424, 0.6);
				tint = 0xFF000000 | AuraHud.mix(wc, 0xFFFFFF, 0.15);
			}
			case WAKING -> {
				frame = 0xFF000000 | AuraHud.mix(wc, 0x2A2438, 0.3);
				fill = 0xFF1E1A2A;
				tint = 0xFF000000 | AuraHud.mix(wc, 0x2A2438, 0.35);
			}
			case UPCOMING -> {
				frame = 0xFF000000 | AuraHud.mix(wc, 0x2A2438, 0.45);
				fill = 0xFF16121F;
				tint = 0x88000000 | wc;
			}
			case LOCKED -> {
				frame = 0xFF2A2438;
				fill = 0xFF141019;
				tint = 0xFF000000 | AuraHud.mix(wc, 0x2A2438, 0.8);
			}
			default -> {
				frame = 0xFF000000 | AuraHud.mix(wc, 0x8A84A0, 0.55);
				fill = 0xFF1C1828;
				tint = 0xDD000000 | wc;
			}
		}
		g.fill(x - 1, y - 1, x + size + 1, y + size + 1, frame);
		g.fill(x, y, x + size, y + size, fill);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, emblem(w.id()), x + 1, y + 1, size - 2, size - 2, tint);
		// Its stage as pips in the corner: one at Edge, two at Form, three at Sovereign.
		int pips = Math.max(1, node.stage() - dev.wildercord.aura.WayRules.FROM + 1);
		for (int p = 0; p < pips; p++) {
			g.fill(x + size - 3 - p * 3, y + size - 3, x + size - 1 - p * 3, y + size - 1, ns == dev.wildercord.aura.WayRules.NodeState.CHOSEN ? GOLD : frame);
		}
		if (ns == dev.wildercord.aura.WayRules.NodeState.WAKING) {
			// How far it has come toward waking: a thread under the cell filling in its Way's colour.
			double k = dev.wildercord.aura.WayRules.wakeProgress(node.stage(), state.owed(), state.settle());
			g.fill(x, y + size + 2, x + size, y + size + 3, 0xFF2A2438);
			g.fill(x, y + size + 2, x + (int) Math.round(size * k), y + size + 3, 0xFF000000 | wc);
		}
	}

	/** The picked node in full, from {@code y}: name and stage, where it stands, its passive, what it changes. Returns where it ends. */
	private int details(GuiGraphicsExtractor g, LocalPlayer player, AuraApi.Way w, AuraApi.WayNode node, dev.wildercord.aura.Ways.State state, int y,
			int stage) {
		int wc = 0xFF000000 | w.color();
		dev.wildercord.aura.WayRules.NodeState ns = dev.wildercord.aura.Ways.nodeState(player, w, node);
		Component title = Component.translatable("screen.wildercord.aura.way.node_title", Component.translatable(node.nameKey()).withColor(wc),
			Component.translatable("aura.wildercord.stage." + AuraStages.id(node.stage())));
		g.text(font, font.plainSubstrByWidth(title.getString(), W - 30).equals(title.getString()) ? title : Component.translatable(node.nameKey()).withColor(wc),
			16, y, TEXT, true);
		y += 11;
		Component where = switch (ns) {
			case WAKING -> Component.translatable("screen.wildercord.aura.way.detail.waking",
				(int) Math.ceil(dev.wildercord.aura.WayRules.toWake(node.stage(), state.owed(), state.settle())));
			case UPCOMING -> Component.translatable("screen.wildercord.aura.way.detail.upcoming",
				Component.translatable("aura.wildercord.stage." + AuraStages.id(node.stage())));
			default -> Component.translatable("screen.wildercord.aura.way.detail." + ns.name().toLowerCase(Locale.ROOT), Component.translatable(w.nameKey()));
		};
		int whereColor = switch (ns) {
			case CHOSEN -> GOLD;
			case WAKING -> 0xFF000000 | AuraHud.mix(w.color(), 0xFFFFFF, 0.3);
			default -> DIM;
		};
		g.text(font, font.plainSubstrByWidth(where.getString(), W - 32), 18, y, whereColor, false);
		y += 11;
		// The passive and the change come before the foot's line (which gives way to them): at full size when they fit above the frame,
		// a little smaller when a long node wouldn't, so no node's words are ever cut off.
		int bottom = H - 12;
		Component passive = Component.translatable("screen.wildercord.aura.way.passive", Component.translatable(node.passiveKey()));
		String change = dev.wildercord.aura.WayRules.CHANGES.get(node.id());
		Component changes = change == null ? Component.translatable("screen.wildercord.aura.way.change_line", Component.translatable(node.changeKey()))
			: Component.translatable("screen.wildercord.aura.way.changes", Component.translatable("aura.wildercord.technique." + change),
				Component.translatable(node.changeKey()));
		float scale = 1.0F;
		List<net.minecraft.util.FormattedCharSequence> passiveLines = font.split(passive, W - 34);
		List<net.minecraft.util.FormattedCharSequence> changeLines = font.split(changes, W - 34);
		if (y + (passiveLines.size() + changeLines.size()) * 10 - 1 > bottom) {
			scale = 0.8F;
			passiveLines = font.split(passive, Math.round((W - 34) / scale));
			changeLines = font.split(changes, Math.round((W - 34) / scale));
		}
		int step = scale < 1.0F ? 8 : 10;
		for (int i = 0; i < passiveLines.size() + changeLines.size(); i++) {
			if (y + step - 1 > bottom) {
				return y;
			}
			boolean isPassive = i < passiveLines.size();
			g.pose().pushMatrix();
			g.pose().translate(18, y);
			g.pose().scale(scale, scale);
			g.text(font, isPassive ? passiveLines.get(i) : changeLines.get(i - passiveLines.size()), 0, 0, isPassive ? TEXT : 0xFFC8C0E0, false);
			g.pose().popMatrix();
			y += step;
		}
		return y;
	}

	/** Whose arts the Sword strings tab shows: a method's id, or null for the player's own (kept while the game runs). */
	private static String browsing;
	/** Where the methods' swatches were drawn last (the page's own coordinates), for a click. */
	private int chipsY = -1;
	private final List<int[]> chips = new ArrayList<>();
	private final List<String> chipIds = new ArrayList<>();

	/** Shows {@code methodId}'s arts on the Sword strings tab (null: the player's own); the game tests turn the pages this way. */
	public static void browse(String methodId) {
		browsing = methodId;
	}

	/**
	 * The breathing methods' arts, one method at a time: a row of every method's swatch to choose whose (your own to begin with),
	 * then that method's five arts by stage: each one's name, its string drawn in the indicator's marks, the stage that opens it,
	 * and its price (or how long it still rests); on hover what it does, its string in words, its rest and what else it waits on.
	 * Another method's arts are shown fainter, to read and choose by. Under them, a line on how strings work and what each mark
	 * means. Returns the tooltip, if any.
	 */
	private List<Component> arts(GuiGraphicsExtractor g, LocalPlayer player, int y, int mx, int my, int color, int stage, long now) {
		List<Component> tooltip = null;
		String own = Aura.data(player).method();
		List<BreathingMethod> methods = dev.wildercord.aura.BreathingMethods.all();
		String shown = browsing == null || dev.wildercord.aura.BreathingMethods.byId(browsing).isEmpty() ? own : browsing;
		boolean mine = shown.equals(own);
		BreathingMethod method = dev.wildercord.aura.BreathingMethods.byId(shown).orElse(null);
		int methodColor = method == null ? color & 0xFFFFFF : method.color(mine ? stage : AuraRules.EDGE);
		// The swatches: one a method, its colour, the one shown framed in gold, your own marked beneath.
		chips.clear();
		chipIds.clear();
		chipsY = y;
		int x = 16;
		for (BreathingMethod m : methods) {
			if (x + 11 > W - 120) {
				break;
			}
			boolean selected = m.id().equals(shown);
			g.fill(x - 1, y - 1, x + 10, y + 10, selected ? GOLD : 0xFF2A2438);
			g.fill(x, y, x + 9, y + 9, 0xFF000000 | m.color(AuraRules.EDGE));
			if (!AuraApi.hasArts(m.id())) {
				// A method still playing the common arts: its swatch dimmed.
				g.fill(x, y, x + 9, y + 9, 0x88201C2C);
			}
			if (m.id().equals(own)) {
				g.fill(x + 3, y + 11, x + 6, y + 12, TEXT);
			}
			chips.add(new int[] {x - 1, y - 1, 11, 11});
			chipIds.add(m.id());
			if (inside(mx, my, x - 1, y - 1, 11, 11)) {
				List<Component> tip = new ArrayList<>();
				tip.add(Component.translatable(m.nameKey()).withColor(m.color()));
				tip.add(Component.translatable(AuraApi.hasArts(m.id()) ? "screen.wildercord.aura.method_arts" : "screen.wildercord.aura.method_common")
					.withStyle(ChatFormatting.GRAY));
				tip.add(Component.translatable("aura.wildercord.banner.kicker", Component.translatable("aura.wildercord.banner.finisher"),
					Component.translatable(AuraApi.finisher(m.id()).nameKey())).withStyle(ChatFormatting.DARK_GRAY));
				tooltip = tip;
			}
			x += 13;
		}
		if (method != null) {
			Component label = Component.translatable(method.nameKey()).withColor(methodColor);
			int lx = Math.max(x + 6, W - 14 - font.width(label));
			g.text(font, label, lx, y + 1, TEXT, true);
		}
		y += 16;
		int lit = 0xFF000000 | AuraRules.color(methodColor, 0xFFFFFF, 3);
		for (AuraApi.StringArt art : AuraApi.arts(shown)) {
			boolean reached = mine && stage >= art.stage();
			int dot = reached ? 0xFF000000 | methodColor : mine ? 0xFF3A3450 : 0xFF000000 | AuraHud.mix(methodColor, 0x2A2438, 0.55);
			g.fill(16, y + 2, 20, y + 6, dot);
			Component name = Component.translatable(art.nameKey());
			String fitted = font.plainSubstrByWidth(name.getString(), 92);
			g.text(font, fitted, 24, y, reached ? TEXT : mine ? FAINT : DIM, false);
			StringHud.string(g, art.string(), 122, y, reached || !mine ? lit & 0xFFFFFF : 0x5A5470, reached ? 1.0F : mine ? 0.75F : 0.85F);
			// What opens it: its stage, lit once reached.
			Component opens = Component.translatable("aura.wildercord.stage." + AuraStages.id(art.stage()));
			g.text(font, opens, 182, y, reached ? 0xFF000000 | AuraHud.mix(methodColor, 0xFFFFFF, 0.4) : FAINT, false);
			long rest = mine ? dev.wildercord.aura.SwordStrings.readyAt(player, art.id()) - now : 0;
			String right = reached && rest > 0
				? Component.translatable("screen.wildercord.aura.art_resting", (rest + 19) / 20).getString()
				: Component.translatable("screen.wildercord.aura.cost", trim(mine ? dev.wildercord.aura.SwordStrings.price(player, art) : art.cost())).getString();
			g.text(font, right, W - 14 - font.width(right), y, !reached ? FAINT : rest > 0 ? DIM : 0xFFB8A8FF, false);
			if (inside(mx, my, 14, y - 1, W - 28, 10)) {
				List<Component> tip = new ArrayList<>();
				tip.add(name.copy().withColor(methodColor));
				if (method != null) {
					tip.add(Component.translatable("aura.wildercord.banner.kicker", Component.translatable(method.nameKey()),
						Component.translatable(dev.wildercord.aura.AuraFxRules.ordinalKey(art.stage()))).withStyle(ChatFormatting.DARK_GRAY));
				}
				tip.add(Component.translatable(art.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
				Component counterControls = EarnedCounterHelp.controls(art.id());
				if (!counterControls.getString().isEmpty()) tip.add(counterControls.copy().withStyle(ChatFormatting.GRAY));
				List<Component> words = new ArrayList<>();
				for (dev.wildercord.aura.SwordString.Token token : art.string().tokens()) {
					words.add(Component.translatable("aura.wildercord.token." + token.id));
				}
				tip.add(Component.translatable("screen.wildercord.aura.art_string", net.minecraft.network.chat.ComponentUtils.formatList(words,
					Component.literal(", "))).withColor(lit));
				if (art.cooldownTicks() > 0) {
					tip.add(Component.translatable("screen.wildercord.aura.art_rest", trim(art.cooldownTicks() / 20.0)).withStyle(ChatFormatting.DARK_GRAY));
				}
				if (art.condition() == AuraApi.FINAL_GATE) {
					tip.add(Component.translatable(dev.wildercord.config.Config.momentum(player) ? "screen.wildercord.aura.art_needs_peak"
						: "screen.wildercord.aura.art_needs_full").withStyle(ChatFormatting.DARK_GRAY));
				} else if (art.condition() == dev.wildercord.aura.PlaceholderArts.FULL_POOL) {
					tip.add(Component.translatable("screen.wildercord.aura.art_needs_full").withStyle(ChatFormatting.DARK_GRAY));
				}
				if (!mine && method != null) {
					tip.add(Component.translatable("screen.wildercord.aura.art_other", Component.translatable(method.nameKey())).withStyle(ChatFormatting.DARK_GRAY));
				} else if (!reached) {
					tip.add(Component.translatable("screen.wildercord.aura.locked",
						Component.translatable("aura.wildercord.stage." + AuraStages.id(art.stage()))).withStyle(ChatFormatting.DARK_GRAY));
				}
				tooltip = tip;
			}
			y += 10;
			if (y > H - 70) {
				break;
			}
		}
		// The method's finisher, under its arts: what a full swing on an opened foe becomes.
		if (method != null && y <= H - 70) {
			AuraApi.Finisher finisher = AuraApi.finisher(method.id());
			Component fname = Component.translatable(finisher.nameKey());
			g.fill(16, y + 2, 20, y + 6, 0xFF000000 | (dev.wildercord.aura.Stance.OPENED_COLOR & 0xFFFFFF));
			g.text(font, font.plainSubstrByWidth(fname.getString(), 92), 24, y, mine ? TEXT : DIM, false);
			Component when = Component.translatable("screen.wildercord.aura.finisher_when");
			g.text(font, font.plainSubstrByWidth(when.getString(), W - 14 - 122 - 50), 122, y, FAINT, false);
			String which = Component.translatable("aura.wildercord.banner.finisher").getString();
			g.text(font, which, W - 14 - font.width(which), y, 0xFF000000 | (dev.wildercord.aura.Stance.OPENED_COLOR & 0xFFFFFF), false);
			if (inside(mx, my, 14, y - 1, W - 28, 10)) {
				tooltip = List.of(fname.copy().withColor(methodColor), Component.translatable(finisher.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
			}
			y += 10;
		}
		y += 3;
		// Momentum as it stands: what its tier takes off every price and adds to every strike.
		int tier = dev.wildercord.aura.Momentum.tier(player);
		if (mine && tier > 0) {
			String off = trim(Math.round((1 - dev.wildercord.aura.MomentumRules.priceFactor(tier)) * 100));
			String harder = trim(Math.round((dev.wildercord.aura.MomentumRules.strength(tier) - 1) * 100));
			Component line = tier >= dev.wildercord.aura.MomentumRules.PEAK_TIER
				? Component.translatable("screen.wildercord.aura.momentum_peak", off, harder)
				: Component.translatable("screen.wildercord.aura.momentum_line", tier, off, harder);
			g.text(font, font.plainSubstrByWidth(line.getString(), W - 32), 16, y, 0xFF000000 | AuraRules.color(methodColor, 0xFFD86A, 4), false);
			y += 11;
		}
		String window = trim(dev.wildercord.config.Config.stringWindow(player) / 20.0);
		for (net.minecraft.util.FormattedCharSequence line : font.split(Component.translatable("screen.wildercord.aura.arts_hint", window), W - 32)) {
			g.text(font, line, 16, y, DIM, false);
			y += 10;
		}
		// What each mark means: the mark, then its word, wrapping as the page runs out.
		y += 2;
		x = 16;
		for (dev.wildercord.aura.SwordString.Token token : dev.wildercord.aura.SwordString.Token.values()) {
			String word = token.id;
			int w = StringHud.GLYPH + 3 + font.width(word) + 9;
			if (x + w > W - 14) {
				x = 16;
				y += 11;
			}
			StringHud.glyph(g, token, x, y, token == dev.wildercord.aura.SwordString.Token.COUNTER ? AuraGuard.PERFECT_COLOR : lit & 0xFFFFFF, 1.0F);
			g.text(font, word, x + StringHud.GLYPH + 3, y, TEXT, false);
			if (inside(mx, my, x, y - 1, w - 6, 10)) {
				tooltip = List.of(Component.translatable("aura.wildercord.token." + token.id).withColor(lit));
			}
			x += w;
		}
		return tooltip;
	}

	/**
	 * Awakening as it stands for {@code player}, in a few words (null below Edge or where it's off): how long it still burns, how long
	 * they're spent, how long it rests, or that it's ready (or what it waits on: a full pool, momentum).
	 */
	private static Component awakening(LocalPlayer player, int stage, long now) {
		if (stage < dev.wildercord.aura.AwakeningRules.FROM || !dev.wildercord.config.Config.awakening(player)) {
			return null;
		}
		dev.wildercord.aura.Awakening.State s = dev.wildercord.aura.Awakening.state(player);
		if (s.awakened(now)) {
			return Component.translatable("screen.wildercord.aura.awakened_left", (s.until() - now + 19) / 20);
		}
		if (s.spent(now)) {
			return Component.translatable("screen.wildercord.aura.spent_left", (s.spentUntil() - now + 19) / 20);
		}
		if (s.resting(now)) {
			return Component.translatable("screen.wildercord.aura.awakening_rests", (s.readyAt() - now + 19) / 20);
		}
		dev.wildercord.aura.AwakeningRules.Refusal why = dev.wildercord.aura.Awakening.refusal(player);
		if (why == null) {
			return Component.translatable("screen.wildercord.aura.awakening_ready");
		}
		return switch (why) {
			case POOL -> Component.translatable("screen.wildercord.aura.awakening_pool");
			case MOMENTUM -> Component.translatable("screen.wildercord.aura.awakening_momentum",
				(int) Math.round(dev.wildercord.config.Config.awakeningMomentum(player)));
			default -> Component.translatable("screen.wildercord.aura.awakening_waits");
		};
	}

	/** Every technique: the always-on ones of each stage, then the Aura key's (the registry's, so later stages' show too). */
	private List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		Component always = Component.translatable("screen.wildercord.aura.passive");
		rows.add(new Row(Component.translatable("aura.wildercord.technique.coat"), AuraRules.GLOW, always, AuraRules.COAT_COST,
			Component.translatable("aura.wildercord.technique.coat.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.sense"), AuraRules.GLOW, Component.translatable("screen.wildercord.aura.stance"), 0,
			Component.translatable("aura.wildercord.technique.sense.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.sweep"), AuraRules.FLOW, always, 0,
			Component.translatable("aura.wildercord.technique.sweep.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.edge"), AuraRules.EDGE, always, 0,
			Component.translatable("aura.wildercord.technique.edge.desc")));
		// Aura marks, the spellblade, and Form's own: aura armour and Intent.
		rows.add(new Row(Component.translatable("aura.wildercord.technique.marks"), AuraRules.GLOW, always, 0,
			Component.translatable("aura.wildercord.technique.marks.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.spellblade"), AuraRules.EDGE,
			Component.translatable("screen.wildercord.aura.key_spellblade", WildercordKeys.castKey(), WildercordKeys.auraKey()), 0,
			Component.translatable("aura.wildercord.technique.spellblade.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.armour"), AuraRules.FORM, always, 0,
			Component.translatable("aura.wildercord.technique.armour.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.intent"), AuraRules.FORM, always, 0,
			Component.translatable("aura.wildercord.technique.intent.desc")));
		// Momentum, and the openings it helps make.
		rows.add(new Row(Component.translatable("aura.wildercord.technique.momentum"), AuraRules.GLOW, always, 0,
			Component.translatable("aura.wildercord.technique.momentum.desc")));
		rows.add(new Row(Component.translatable("aura.wildercord.technique.finisher"), AuraRules.GLOW, always, 0,
			Component.translatable("aura.wildercord.technique.finisher.desc")));
		Component key = WildercordKeys.auraKey();
		for (AuraApi.Technique t : AuraApi.techniques()) {
			Component how = switch (t.trigger()) {
				case TAP -> Component.translatable("screen.wildercord.aura.key_tap", key);
				case SNEAK_TAP -> Component.translatable("screen.wildercord.aura.key_sneak", key);
				case DOUBLE_TAP -> Component.translatable("screen.wildercord.aura.key_double", key);
				case HOLD -> Component.translatable("screen.wildercord.aura.key_hold", key);
				case TAP_HOLD -> Component.translatable("screen.wildercord.aura.key_tap_hold", key);
			};
			double cost = t.id().equals("slash") ? dev.wildercord.config.Config.slashCost(minecraft.player) : t.cost();
			rows.add(new Row(Component.translatable(t.nameKey()), t.stage(), how, cost, Component.translatable(t.nameKey() + ".desc")));
		}
		rows.sort(java.util.Comparator.comparingInt(Row::stage));
		return rows;
	}

	private void bar(GuiGraphicsExtractor g, int x, int y, int w, double share, int color) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_BAR, x, y, w, 6);
		int filled = (int) ((w - 2) * Math.max(0, Math.min(1, share)));
		if (filled > 0) {
			int c = color & 0xFFFFFF;
			g.fill(x + 1, y + 1, x + 1 + filled, y + 2, 0xFF000000 | AuraHud.mix(c, 0xFFFFFF, 0.35));
			g.fill(x + 1, y + 2, x + 1 + filled, y + 4, 0xFF000000 | c);
			g.fill(x + 1, y + 4, x + 1 + filled, y + 5, 0xFF000000 | AuraHud.mix(c, 0x000000, 0.35));
		}
	}

	private static String trim(double value) {
		return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
	}
}
