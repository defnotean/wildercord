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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
public class AuraScreen extends Screen {
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
	/** Whether the list shows the sword strings' arts instead of the techniques (kept while the game runs). */
	private static boolean arts;
	/** Where the two tabs were drawn last (the page's own coordinates), for a click: their row, and each one's left and right. */
	private int tabsY = -1;
	private int techLeft;
	private int techRight;
	private int artsLeft;
	private int artsRight;

	public AuraScreen(Screen parent) {
		super(Component.translatable("screen.wildercord.aura.title"));
		this.parent = parent;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
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
		int physical = Math.max(1, (int) Math.floor(guiScale * fit));
		return physical / (float) guiScale;
	}

	private int left() {
		return Math.round((width - W * scale()) / 2);
	}

	private int top() {
		return Math.round((height - H * scale()) / 2);
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
		if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && tabsY >= 0) {
			float s = scale();
			int mx = (int) Math.floor((event.x() - left()) / s);
			int my = (int) Math.floor((event.y() - top()) / s);
			if (my >= tabsY - 2 && my < tabsY + 10) {
				boolean toArts = mx >= artsLeft && mx < artsRight;
				boolean toTech = mx >= techLeft && mx < techRight;
				if ((toArts && !arts) || (toTech && arts)) {
					arts = toArts;
					minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
						net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Opens the page on its Sword strings tab ({@code true}) or its techniques (the game tests put it back as they found it). */
	public static void listArts(boolean show) {
		arts = show;
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

		// ---- the road to the next stage.
		int y = 92;
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
		}
		y += 22;
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

		// ---- the techniques, or (the second tab) the sword strings' arts.
		y = tabs(g, y, mx, my, color);
		if (arts) {
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
		// ---- the passive, at the foot.
		Component flavour = Component.translatable(method.nameKey() + ".flavour");
		int fy = H - 30;
		g.text(font, Component.translatable("screen.wildercord.aura.flavour"), 14, fy, GOLD, true);
		for (net.minecraft.util.FormattedCharSequence line : font.split(flavour, W - 28 - font.width(Component.translatable("screen.wildercord.aura.flavour")) - 6)) {
			g.text(font, line, 20 + font.width(Component.translatable("screen.wildercord.aura.flavour")), fy, 0xFFB8A8D8, false);
			fy += 10;
		}
		return tooltip;
	}

	/** The two tabs over the list: Techniques and Sword strings, the open one in gold and underlined. Returns where the list starts. */
	private int tabs(GuiGraphicsExtractor g, int y, int mx, int my, int color) {
		Component tech = Component.translatable("screen.wildercord.aura.techniques");
		Component strings = Component.translatable("screen.wildercord.aura.arts");
		tabsY = y;
		techLeft = 14;
		techRight = techLeft + font.width(tech);
		artsLeft = techRight + 14;
		artsRight = artsLeft + font.width(strings);
		boolean overTech = inside(mx, my, techLeft, y - 2, techRight - techLeft, 12);
		boolean overArts = inside(mx, my, artsLeft, y - 2, artsRight - artsLeft, 12);
		g.text(font, tech, techLeft, y, !arts ? GOLD : overTech ? TEXT : DIM, true);
		g.text(font, strings, artsLeft, y, arts ? GOLD : overArts ? TEXT : DIM, true);
		int under = arts ? artsLeft : techLeft;
		int underRight = arts ? artsRight : techRight;
		g.fill(under, y + 9, underRight, y + 10, 0xFF000000 | (GOLD & 0xFFFFFF));
		g.fill(techRight + 6, y + 1, techRight + 7, y + 8, FAINT);
		return y + 13;
	}

	/**
	 * The sword strings' arts the player has, by stage: each one's name, its string drawn in the indicator's marks (faint until its
	 * stage is reached), its price (or how long it still rests), and on hover what it does, its string in words, its rest and what
	 * else it waits on. Under them, a line on how strings work and what each mark means. Returns the tooltip, if any.
	 */
	private List<Component> arts(GuiGraphicsExtractor g, LocalPlayer player, int y, int mx, int my, int color, int stage, long now) {
		List<Component> tooltip = null;
		int lit = 0xFF000000 | AuraRules.color(color & 0xFFFFFF, 0xFFFFFF, 3);
		for (AuraApi.StringArt art : AuraApi.allStringsOf(player)) {
			boolean reached = stage >= art.stage();
			g.fill(16, y + 2, 20, y + 6, reached ? color : 0xFF3A3450);
			Component name = Component.translatable(art.nameKey());
			g.text(font, name, 24, y, reached ? TEXT : FAINT, false);
			StringHud.string(g, art.string(), 120, y, reached ? lit & 0xFFFFFF : 0x5A5470, reached ? 1.0F : 0.75F);
			long rest = dev.wildercord.aura.SwordStrings.readyAt(player, art.id()) - now;
			String right = reached && rest > 0
				? Component.translatable("screen.wildercord.aura.art_resting", (rest + 19) / 20).getString()
				: Component.translatable("screen.wildercord.aura.cost", trim(art.cost())).getString();
			g.text(font, right, W - 14 - font.width(right), y, !reached ? FAINT : rest > 0 ? DIM : 0xFFB8A8FF, false);
			if (inside(mx, my, 14, y - 1, W - 28, 10)) {
				List<Component> tip = new ArrayList<>();
				tip.add(name.copy().withColor(reached ? color : DIM));
				tip.add(Component.translatable(art.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
				List<Component> words = new ArrayList<>();
				for (dev.wildercord.aura.SwordString.Token token : art.string().tokens()) {
					words.add(Component.translatable("aura.wildercord.token." + token.id));
				}
				tip.add(Component.translatable("screen.wildercord.aura.art_string", net.minecraft.network.chat.ComponentUtils.formatList(words,
					Component.literal(", "))).withColor(lit));
				if (art.cooldownTicks() > 0) {
					tip.add(Component.translatable("screen.wildercord.aura.art_rest", trim(art.cooldownTicks() / 20.0)).withStyle(ChatFormatting.DARK_GRAY));
				}
				if (art.condition() == dev.wildercord.aura.PlaceholderArts.FULL_POOL) {
					tip.add(Component.translatable("screen.wildercord.aura.art_needs_full").withStyle(ChatFormatting.DARK_GRAY));
				}
				if (!reached) {
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
		y += 3;
		String window = trim(dev.wildercord.config.Config.stringWindow(player) / 20.0);
		for (net.minecraft.util.FormattedCharSequence line : font.split(Component.translatable("screen.wildercord.aura.arts_hint", window), W - 32)) {
			g.text(font, line, 16, y, DIM, false);
			y += 10;
		}
		// What each mark means: the mark, then its word, wrapping as the page runs out.
		y += 2;
		int x = 16;
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
		Component key = WildercordKeys.auraKey();
		for (AuraApi.Technique t : AuraApi.techniques()) {
			Component how = switch (t.trigger()) {
				case TAP -> Component.translatable("screen.wildercord.aura.key_tap", key);
				case SNEAK_TAP -> Component.translatable("screen.wildercord.aura.key_sneak", key);
				case DOUBLE_TAP -> Component.translatable("screen.wildercord.aura.key_double", key);
				case HOLD -> Component.translatable("screen.wildercord.aura.key_hold", key);
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
