package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellNames;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fusion Altar's screen, in the Cord screen's carved stone and gold. On the left the altar
 * itself: three rune sockets round a catalyst socket, over the magic circle of whatever lies on it.
 * On the right the result, and a panel saying which fusion the altar has worked out, what it will
 * make and what it costs, with the button that asks the server to do it. To tie a Knot, the panel
 * lists your spells to choose from.
 *
 * <p>Everything shown comes from {@link FusionAltarMenu#plan}, the same rules the server checks;
 * the screen never decides anything that matters.</p>
 */
public class FusionAltarScreen extends AbstractContainerScreen<FusionAltarMenu> {
	private static final int W = 260;
	private static final int H = 228;

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int WARN = 0xFFF0C440;
	private static final int BAD = 0xFFE05050;
	private static final int XP = 0xFF80FF20;

	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_SOCKET = Wildercord.id("cord/socket");
	private static final Identifier SPR_SOCKET_HOVER = Wildercord.id("cord/socket_hover");
	private static final Identifier SPR_TAB = Wildercord.id("cord/tab");
	private static final Identifier SPR_TAB_ACTIVE = Wildercord.id("cord/tab_active");
	private static final Identifier SPR_ROW = Wildercord.id("cord/row");
	private static final Identifier SPR_ROW_SELECTED = Wildercord.id("cord/row_selected");

	// The altar on the left, the result and the panel on the right (local coordinates).
	private static final int ALTAR_X = 8;
	private static final int ALTAR_Y = 20;
	private static final int ALTAR_W = 112;
	private static final int ALTAR_H = 112;
	private static final int CIRCLE_X = 64;
	private static final int CIRCLE_Y = 76;
	private static final int PANEL_X = 124;
	private static final int PANEL_Y = 48;
	private static final int PANEL_W = 128;
	private static final int PANEL_H = 84;
	private static final int BUTTON_W = 74;
	private static final int BUTTON_H = 14;
	private static final int ROW_H = 11;

	/** The spell picked to tie into a Knot. */
	private int spell = -1;
	/** What the circle is showing, and when it started opening. */
	private List<String> circleShown = List.of();
	private long circleOpened;

	public FusionAltarScreen(FusionAltarMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, H);
		this.titleLabelX = 12;
		this.titleLabelY = 8;
		this.inventoryLabelX = FusionAltarMenu.INVENTORY_X;
		this.inventoryLabelY = FusionAltarMenu.INVENTORY_Y - 11;
	}

	// ------------------------------------------------------------------ for the game tests

	/** The button's middle, in GUI coordinates. */
	public double[] buttonPoint() {
		return new double[] {leftPos + buttonX() + BUTTON_W / 2.0, topPos + buttonY() + BUTTON_H / 2.0};
	}

	/** Spell row {@code s}'s middle (for a Knot), in GUI coordinates. */
	public double[] spellPoint(int s) {
		return new double[] {leftPos + PANEL_X + PANEL_W / 2.0, topPos + rowY(s) + ROW_H / 2.0};
	}

	// ------------------------------------------------------------------ what the altar would do

	private Fusions.Plan plan() {
		return menu.plan();
	}

	private List<RuneDef> spellRunes(int s) {
		return minecraft.player == null ? null : FusionAltarMenu.spellRunes(minecraft.player, s);
	}

	/** Why spell {@code s} can't be tied, or null if it can. */
	private String knotProblem(int s) {
		List<RuneDef> runes = spellRunes(s);
		return runes == null ? "That spell holds a rune whose add-on is missing." : Knots.problem(runes);
	}

	private int chosenSpell() {
		if (spell >= 0 && knotProblem(spell) == null) {
			return spell;
		}
		for (int s = 0; s < CordTier.MAX_SPELLS; s++) {
			if (knotProblem(s) == null) {
				return s;
			}
		}
		return -1;
	}

	/** XP levels the button would take, or -1 for nothing to do. */
	private int cost(Fusions.Plan plan) {
		if (plan.kind() == Fusions.Kind.KNOT) {
			int s = chosenSpell();
			return s < 0 ? -1 : Knots.xpCost(spellRunes(s));
		}
		return plan.ready() ? plan.xp() : -1;
	}

	private boolean canPay(int levels) {
		return minecraft.player != null && (minecraft.player.hasInfiniteMaterials() || minecraft.player.experienceLevel >= levels);
	}

	/** Why the button won't work, or null when it will. */
	private Component blocked(Fusions.Plan plan) {
		if (!plan.ready()) {
			return plan.problem() == null ? null : Component.literal(plan.problem());
		}
		if (!menu.resultFree()) {
			return Component.translatable("screen.wildercord.altar.take_result");
		}
		if (plan.kind() == Fusions.Kind.KNOT && chosenSpell() < 0) {
			return Component.translatable("screen.wildercord.altar.no_spell");
		}
		int levels = cost(plan);
		if (levels >= 0 && !canPay(levels)) {
			return Component.translatable("screen.wildercord.altar.need_xp", levels);
		}
		return null;
	}

	// ------------------------------------------------------------------ drawing

	/** The button sits beside the result, so the panel below keeps its room for text. */
	private int buttonX() {
		return PANEL_X + PANEL_W - BUTTON_W;
	}

	private int buttonY() {
		return FusionAltarMenu.RESULT_POS[1] + 1;
	}

	private int rowY(int s) {
		return PANEL_Y + 15 + s * ROW_H;
	}

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		super.extractBackground(g, mouseX, mouseY, a);
		int x = leftPos;
		int y = topPos;
		sprite(g, SPR_PANEL, x, y, W, H);
		sprite(g, SPR_INSET, x + ALTAR_X, y + ALTAR_Y, ALTAR_W, ALTAR_H);
		sprite(g, SPR_INSET, x + PANEL_X, y + PANEL_Y, PANEL_W, PANEL_H);
		Fusions.Plan plan = plan();
		drawCircle(g, plan, a);
		// Sockets: the three runes, the catalyst, the result. They glow when the altar knows what to make.
		boolean ready = plan.ready();
		for (int[] pos : FusionAltarMenu.RUNE_POS) {
			sprite(g, ready ? SPR_SOCKET_HOVER : SPR_SOCKET, x + pos[0] - 1, y + pos[1] - 1, 18, 18);
		}
		sprite(g, ready ? SPR_SOCKET_HOVER : SPR_SOCKET, x + FusionAltarMenu.CATALYST_POS[0] - 1, y + FusionAltarMenu.CATALYST_POS[1] - 1, 18, 18);
		int rx = x + FusionAltarMenu.RESULT_POS[0];
		int ry = y + FusionAltarMenu.RESULT_POS[1];
		int glow = ready ? 0x60000000 | resultColor(plan) : 0x30000000;
		g.fill(rx - 4, ry - 4, rx + 20, ry + 20, glow);
		sprite(g, SPR_SOCKET, rx - 1, ry - 1, 18, 18);
		// The arrow from the altar to the result.
		int ay = ry + 8;
		int arrow = ready ? GOLD : FAINT;
		g.fill(x + ALTAR_X + ALTAR_W - 6, ay, rx - 5, ay + 1, arrow);
		for (int i = 0; i < 3; i++) {
			g.fill(rx - 7 + i, ay - 2 + i, rx - 6 + i, ay + 3 - i, arrow);
		}
		// The inventory's slots, in the Cord screen's recessed sockets.
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				sprite(g, SPR_SOCKET, x + FusionAltarMenu.INVENTORY_X + col * 18 - 1, y + FusionAltarMenu.INVENTORY_Y + row * 18 - 1, 18, 18);
			}
		}
		for (int col = 0; col < 9; col++) {
			sprite(g, SPR_SOCKET, x + FusionAltarMenu.INVENTORY_X + col * 18 - 1, y + FusionAltarMenu.INVENTORY_Y + 58 - 1, 18, 18);
		}
		drawButton(g, plan, mouseX, mouseY);
	}

	private int resultColor(Fusions.Plan plan) {
		if (plan.kind() == Fusions.Kind.KNOT) {
			return RuneColors.KNOT;
		}
		return plan.result() != null ? RuneColors.of(plan.result()) : 0xE8C46A;
	}

	/**
	 * The altar's circle: the magic circle of what lies on it (or of the spell to be tied), opening
	 * whenever that changes and turning slowly; with nothing on it, a faint ring and the triangle
	 * joining its sockets.
	 */
	private void drawCircle(GuiGraphicsExtractor g, Fusions.Plan plan, float partial) {
		float cx = leftPos + CIRCLE_X;
		float cy = topPos + CIRCLE_Y;
		List<RuneDef> runes = new ArrayList<>();
		if (plan.kind() == Fusions.Kind.KNOT && chosenSpell() >= 0) {
			runes.addAll(spellRunes(chosenSpell()));
		} else {
			if (plan.ready() && plan.result() != null) {
				runes.add(plan.result());
			}
			for (int i = 0; i < FusionAltarMenu.RUNE_SLOTS; i++) {
				RuneItem.runeOf(menu.input(i)).ifPresent(runes::add);
			}
		}
		long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
		List<String> ids = runes.stream().map(RuneDef::id).toList();
		if (!ids.equals(circleShown)) {
			circleShown = ids;
			circleOpened = now;
		}
		// The triangle and ring the sockets sit on, always there.
		int line = plan.ready() ? 0x90E8C46A : 0x50B8A8FF;
		float[][] points = new float[3][];
		for (int i = 0; i < 3; i++) {
			int[] pos = FusionAltarMenu.RUNE_POS[i];
			points[i] = new float[] {leftPos + pos[0] + 8, topPos + pos[1] + 8};
		}
		GuiSpellCircle.ring(g, cx, cy, 36, 0.7F, line);
		GuiSpellCircle.ring(g, cx, cy, 46, 0.5F, (line & 0x00FFFFFF) | 0x40000000);
		for (int i = 0; i < 3; i++) {
			float[] p = points[i];
			float[] q = points[(i + 1) % 3];
			GuiSpellCircle.line(g, p[0], p[1], q[0], q[1], 0.6F, line);
		}
		if (!runes.isEmpty()) {
			float time = (now + partial) / 20F;
			float open = Math.min(1, (now - circleOpened + partial) / 14F);
			GuiSpellCircle.draw(g, cx, cy, 44, runes, time, open);
		}
	}

	private void drawButton(GuiGraphicsExtractor g, Fusions.Plan plan, int mouseX, int mouseY) {
		if (plan.kind() == Fusions.Kind.NONE) {
			return;
		}
		boolean enabled = blocked(plan) == null;
		int bx = leftPos + buttonX();
		int by = topPos + buttonY();
		boolean hover = inside(mouseX, mouseY, bx, by, BUTTON_W, BUTTON_H);
		sprite(g, enabled ? SPR_TAB_ACTIVE : SPR_TAB, bx, by, BUTTON_W, BUTTON_H);
		if (enabled && hover) {
			g.fill(bx + 2, by + 2, bx + BUTTON_W - 2, by + BUTTON_H - 1, 0x30FFE8B0);
		}
		Component label = Component.translatable(plan.kind() == Fusions.Kind.KNOT ? "screen.wildercord.altar.tie" : "screen.wildercord.altar.fuse");
		g.centeredText(font, label, bx + BUTTON_W / 2, by + 3, enabled ? GOLD : FAINT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, GOLD, true);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);
		// Your XP, top right: fusions are paid in levels.
		Component levels = Component.translatable("screen.wildercord.altar.levels", minecraft.player == null ? 0 : minecraft.player.experienceLevel);
		g.text(font, levels, W - 12 - font.width(levels), titleLabelY, XP, true);
		drawPanel(g, plan(), mouseX - leftPos, mouseY - topPos);
	}

	/** The panel: which fusion, what it makes, what it costs, or why it can't. */
	private void drawPanel(GuiGraphicsExtractor g, Fusions.Plan plan, int mx, int my) {
		int x = PANEL_X + 5;
		int y = PANEL_Y + 4;
		int width = PANEL_W - 10;
		String kind = switch (plan.kind()) {
			case UPGRADE -> "upgrade";
			// Two particular runes with a signature of their own say so: it isn't their elements' usual fusion.
			case COMBINE -> plan.recipe() == null ? "weave" : plan.signature() ? "signature" : "combine";
			case KNOT -> "knot";
			case NONE -> "none";
		};
		fit(g, Component.translatable("screen.wildercord.altar.kind." + kind), x, y, width, GOLD);
		y += 11;
		if (plan.kind() == Fusions.Kind.KNOT && plan.problem() == null) {
			drawSpells(g, mx, my);
			return;
		}
		if (plan.ready() && plan.result() != null) {
			Component name = RuneItem.runeName(plan.result()).append(dev.wildercord.spell.Ranks.suffix(plan.rank())).withColor(RuneColors.of(plan.result()));
			fit(g, name, x, y, width, 0xFFFFFFFF);
			y += 11;
			Component what;
			if (plan.kind() == Fusions.Kind.UPGRADE) {
				what = Component.translatable("screen.wildercord.altar.upgrade_line", Math.round((dev.wildercord.spell.Ranks.power(plan.rank()) - 1) * 100));
			} else if (plan.recipe() instanceof Fusions.Signature signature) {
				what = Component.translatable("screen.wildercord.altar.signature_line", RuneItem.runeName(signature.a()), RuneItem.runeName(signature.b()));
			} else if (plan.recipe() == null) {
				what = Component.literal("Both effects in one socket; costs the mana of both.");
			} else {
				what = Component.translatable("screen.wildercord.altar.combine_line",
					Component.translatable("element.wildercord." + plan.recipe().first()), Component.translatable("element.wildercord." + plan.recipe().second()));
			}
			y = wrap(g, what, x, y, width, DIM, 3);
			Component blocked = blocked(plan);
			int levels = cost(plan);
			Component price = blocked != null ? blocked : Component.translatable("screen.wildercord.altar.cost", levels);
			wrap(g, price, x, Math.max(y + 2, PANEL_Y + PANEL_H - 24), width, blocked != null ? BAD : XP, 2);
			return;
		}
		if (plan.kind() == Fusions.Kind.NONE && (plan.problem() == null || plan.problem().equals(Fusions.HOW))) {
			// An empty altar: the three fusions, one short line each.
			for (int i = 1; i <= 3; i++) {
				y = wrap(g, Component.translatable("screen.wildercord.altar.how." + i), x, y, width, DIM, 2) + 2;
			}
			return;
		}
		wrap(g, Component.literal(plan.problem()), x, y, width, plan.kind() == Fusions.Kind.NONE ? DIM : WARN, 6);
	}

	/** Tying a Knot: your spells to pick from, and what the chosen one costs. */
	private void drawSpells(GuiGraphicsExtractor g, int mx, int my) {
		Spellbook book = Spellbooks.get(minecraft.player);
		int chosen = chosenSpell();
		for (int s = 0; s < CordTier.MAX_SPELLS; s++) {
			int y = rowY(s);
			String problem = knotProblem(s);
			boolean selected = s == chosen;
			if (selected) {
				sprite(g, SPR_ROW_SELECTED, PANEL_X + 3, y - 1, PANEL_W - 6, ROW_H);
			} else if (problem == null && inside(mx, my, PANEL_X + 3, y - 1, PANEL_W - 6, ROW_H)) {
				sprite(g, SPR_ROW, PANEL_X + 3, y - 1, PANEL_W - 6, ROW_H);
			}
			List<RuneDef> runes = spellRunes(s);
			String name = book.name(s);
			if (name.isEmpty() && runes != null) {
				name = SpellNames.auto(runes);
			}
			Component line = Component.literal((s + 1) + "  " + (name.isEmpty() ? "-" : name));
			fit(g, line, PANEL_X + 6, y + 1, PANEL_W - 12, selected ? GOLD : problem == null ? TEXT : FAINT);
		}
		Fusions.Plan plan = plan();
		Component blocked = blocked(plan);
		int levels = cost(plan);
		Component price = blocked != null ? blocked : Component.translatable("screen.wildercord.altar.cost", levels);
		wrap(g, price, PANEL_X + 5, rowY(CordTier.MAX_SPELLS) + 2, PANEL_W - 10, blocked != null ? BAD : XP, 2);
	}

	private void fit(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color) {
		if (font.width(text) <= width) {
			g.text(font, text, x, y, color, false);
			return;
		}
		String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("…"))) + "…";
		g.text(font, Component.literal(plain).withStyle(text.getStyle()), x, y, color, false);
	}

	/** Wraps text into at most {@code max} lines and returns the y under it. */
	private int wrap(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color, int max) {
		List<FormattedCharSequence> lines = font.split(FormattedText.of(text.getString(), text.getStyle()), width);
		for (int i = 0; i < lines.size() && i < max; i++) {
			g.text(font, lines.get(i), x, y, color, false);
			y += 10;
		}
		return y;
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractTooltip(g, mouseX, mouseY);
		if (hoveredSlot != null && hoveredSlot.hasItem() || !menu.getCarried().isEmpty()) {
			return;
		}
		Fusions.Plan plan = plan();
		if (plan.kind() == Fusions.Kind.KNOT && plan.problem() == null) {
			for (int s = 0; s < CordTier.MAX_SPELLS; s++) {
				if (inside(mouseX - leftPos, mouseY - topPos, PANEL_X + 3, rowY(s) - 1, PANEL_W - 6, ROW_H)) {
					List<Component> lines = new ArrayList<>();
					List<RuneDef> runes = spellRunes(s);
					if (runes != null && !runes.isEmpty()) {
						lines.add(Component.literal(Knots.sequence(runes)).withStyle(ChatFormatting.GRAY));
						lines.add(Component.translatable("screen.wildercord.altar.cost", Knots.xpCost(runes)).withColor(XP));
					}
					String problem = knotProblem(s);
					if (problem != null) {
						lines.add(Component.literal(problem).withStyle(ChatFormatting.GOLD));
					}
					if (!lines.isEmpty()) {
						g.setTooltipForNextFrame(font, Tooltips.fit(font, lines, width, height), mouseX, mouseY);
					}
					return;
				}
			}
		}
		if(plan.result()!=null && inside(mouseX-leftPos,mouseY-topPos,PANEL_X,PANEL_Y,PANEL_W,PANEL_H)) {
			List<Component> lines=new ArrayList<>();
			lines.add(RuneItem.runeName(plan.result()));
			lines.add(Component.literal(plan.result().description()).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("screen.wildercord.altar.preview_mana",String.format(java.util.Locale.ROOT,"%.1f",plan.result().cost())));
			for(var element:dev.wildercord.spell.VisualElements.of(List.of(plan.result())))lines.add(Component.translatable("screen.wildercord.altar.preview_material",element));
			g.setTooltipForNextFrame(font,Tooltips.fit(font,lines,width,height),mouseX,mouseY);return;
		}
		if (plan.kind() != Fusions.Kind.NONE && inside(mouseX - leftPos, mouseY - topPos, buttonX(), buttonY(), BUTTON_W, BUTTON_H)) {
			Component blocked = blocked(plan);
			if (blocked != null) {
				g.setTooltipForNextFrame(font, Tooltips.fit(font, List.of(blocked.copy().withStyle(ChatFormatting.RED)), width, height), mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x() - leftPos;
		double my = event.y() - topPos;
		Fusions.Plan plan = plan();
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && plan.kind() == Fusions.Kind.KNOT && plan.problem() == null) {
			for (int s = 0; s < CordTier.MAX_SPELLS; s++) {
				if (inside(mx, my, PANEL_X + 3, rowY(s) - 1, PANEL_W - 6, ROW_H)) {
					if (knotProblem(s) == null) {
						spell = s;
						click();
					}
					return true;
				}
			}
		}
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && plan.kind() != Fusions.Kind.NONE && inside(mx, my, buttonX(), buttonY(), BUTTON_W, BUTTON_H)) {
			int chosen = plan.kind() == Fusions.Kind.KNOT ? chosenSpell() : 0;
			if (plan.kind() == Fusions.Kind.KNOT && chosen < 0) {
				if (minecraft.player != null) minecraft.player.sendOverlayMessage(Component.translatable("screen.wildercord.altar.no_spell").withColor(BAD));
			} else {
				// Let the server decide from its current slots and XP. A stale client view must not silence a valid click.
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
					plan.kind() == Fusions.Kind.KNOT ? FusionAltarMenu.BUTTON_KNOT + chosen : FusionAltarMenu.BUTTON_FUSE);
				click();
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
}
