package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraStages;
import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BladeCeremony;
import dev.wildercord.aura.BladeRules;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.aura.Ways;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Aura page's Blade tab: the bonded blade. Before one is bonded, how the bond is made (the ceremony, which blades bond, what a bond
 * gives) and whether the blade in hand can be. Once bonded: the blade large in its frame and colour, its name and tier, its resonance
 * toward the next tier (and what that waits on), its name to give (typed, or one of the names it suggests from its own story), its trait
 * (the habits it's reading so far, then the three it offers when it awakens, to choose from), its story (where and when, its deeds
 * counted, its favourite art, its notable deeds), and releasing the bond. While the blade is away, where it was last with them.
 */
final class BladePage {
	private static final int W = 320;
	private static final int H = 340;
	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E0F0;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int RED = 0xFFE08070;
	private static final int AMBER = 0xFFF0C060;
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");

	/** The name being written, whether it's being typed, how many suggestions have been asked for, and two armed clicks. */
	private static String draft;
	private static String draftFor = "";
	private static boolean typing;
	private static int salt;
	private static long releaseArmedAt = -1000;
	private static String traitArmed = "";
	private static long traitArmedAt = -1000;

	private final Minecraft minecraft;
	private final Font font;
	/** Everything clickable as drawn last (the page's own coordinates: x, y, w, h), by what it does. */
	final Map<String, int[]> targets = new LinkedHashMap<>();

	BladePage(Minecraft minecraft, Font font) {
		this.minecraft = minecraft;
		this.font = font;
	}

	/** Whether the name is being typed (the game tests ask), and the name as written so far. */
	static boolean typing() {
		return typing;
	}

	static String draftName() {
		return draft == null ? "" : draft;
	}

	/** Starts afresh (the tab opened). */
	static void opened() {
		typing = false;
		draftFor = "";
	}

	static Identifier tierSprite(int tier) {
		return Wildercord.id("aura/blade/tier_" + BladeRules.tierId(Math.max(BladeRules.BONDED, tier)));
	}

	static Identifier traitSprite(String trait) {
		return BladeRules.BUILT_IN_TRAITS.contains(trait) ? Wildercord.id("aura/blade/trait_" + trait) : Wildercord.id("aura/blade/trait_unknown");
	}

	private static boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void target(String key, int x, int y, int w, int h) {
		targets.put(key, new int[] {x, y, w, h});
	}

	private void small(GuiGraphicsExtractor g, Component text, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	private void small(GuiGraphicsExtractor g, FormattedCharSequence text, int x, int y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(font, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	/** {@code text} wrapped to {@code width} at {@code scale}, from ({@code x}, {@code y}), at most {@code most} lines; returns the y after. */
	private int para(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color, float scale, int most) {
		List<FormattedCharSequence> lines = font.split(text, Math.round(width / scale));
		int step = Math.round(10 * scale);
		for (int i = 0; i < Math.min(most, lines.size()); i++) {
			small(g, lines.get(i), x, y, color, scale);
			y += step;
		}
		return y;
	}

	private void button(GuiGraphicsExtractor g, int x, int y, int w, Component label, boolean enabled, boolean hover, int textColor) {
		g.fill(x, y, x + w, y + 14, enabled && hover ? 0xFF3A3050 : 0xFF221E30);
		g.fill(x, y, x + w, y + 1, enabled ? 0xFF000000 | AuraHud.mix(0xE8C46A, 0x221E30, hover ? 0.0 : 0.4) : 0xFF3A3450);
		g.fill(x, y + 13, x + w, y + 14, 0xFF14101C);
		float s = font.width(label) > w - 6 ? 0.8F : 1.0F;
		int lw = Math.round(font.width(label) * s);
		small(g, label, x + (w - lw) / 2, y + (s < 1 ? 4 : 3), textColor, s);
	}

	// ------------------------------------------------------------------ drawing

	/** The whole page under the tabs (from {@code top}); returns the tooltip, if any. */
	List<Component> draw(GuiGraphicsExtractor g, LocalPlayer player, int mx, int my, float partial, int color, int top) {
		targets.clear();
		long now = player.level().getGameTime();
		if (!BondedBlades.on(player)) {
			para(g, Component.translatable("screen.wildercord.aura.blade.off"), 16, top + 6, W - 32, DIM, 1.0F, 3);
			return null;
		}
		BondedBlades.State state = BondedBlades.state(player);
		if (!state.bonded()) {
			return unbonded(g, player, mx, my, color, top);
		}
		ItemStack blade = BondedBlades.carried(player);
		BladeBond b = BondedBlades.bond(blade);
		if (b == null) {
			return away(g, player, state, mx, my, color, top, now);
		}
		return bonded(g, player, blade, b, mx, my, partial, top, now);
	}

	// ---------------------------------------------------------------- before a bond

	private List<Component> unbonded(GuiGraphicsExtractor g, LocalPlayer player, int mx, int my, int color, int top) {
		List<Component> tip = null;
		int stage = Aura.stage(player);
		int y = top + 4;
		g.text(font, Component.translatable("screen.wildercord.aura.blade.bond_title"), 14, y, GOLD, true);
		y += 13;
		if (stage < BladeRules.FROM) {
			y = para(g, Component.translatable("screen.wildercord.aura.blade.before_edge", Component.translatable("aura.wildercord.stage."
				+ AuraStages.id(BladeRules.FROM))), 16, y, W - 32, TEXT, 0.9F, 4) + 4;
		} else {
			y = para(g, Component.translatable("screen.wildercord.aura.blade.how"), 16, y, W - 32, TEXT, 0.9F, 4) + 4;
		}
		// The three steps, each with its picture.
		ItemStack hand = player.getMainHandItem();
		boolean canBond = BondedBlades.canBond(hand);
		ItemStack shown = canBond ? hand : new ItemStack(Items.IRON_SWORD);
		String[] steps = {"step_blade", "step_crossing", "step_kneel"};
		for (int i = 0; i < steps.length; i++) {
			int sy = y + i * 24;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_INSET, 14, sy, 20, 20);
			if (i == 0) {
				g.item(shown, 16, sy + 2);
			} else {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, Wildercord.id(i == 1 ? "aura/blade/crossing" : "aura/blade/kneel"), 16, sy + 2, 16, 16,
					0xFF000000 | AuraHud.mix(color, 0xFFFFFF, 0.25));
			}
			small(g, Component.literal((i + 1) + ".").withColor(GOLD), 38, sy + 2, GOLD, 0.9F);
			para(g, Component.translatable("screen.wildercord.aura.blade." + steps[i]), 48, sy + 2, W - 64, TEXT, 0.8F, 2);
		}
		y += 3 * 24 + 4;
		// The blade in hand now: can it be bonded?
		Component inHand;
		int handColor;
		if (hand.isEmpty() || !hand.is(Aura.WEAPONS) && !hand.is(BondedBlades.BONDABLE)) {
			inHand = Component.translatable("screen.wildercord.aura.blade.hand_none");
			handColor = DIM;
		} else if (canBond) {
			inHand = Component.translatable("screen.wildercord.aura.blade.hand_yes", hand.getHoverName());
			handColor = 0xFF9FE08A;
		} else if (BondedBlades.bonded(hand)) {
			inHand = Component.translatable("screen.wildercord.aura.blade.hand_bonded", hand.getHoverName());
			handColor = RED;
		} else {
			inHand = Component.translatable("screen.wildercord.aura.blade.hand_no", hand.getHoverName());
			handColor = AMBER;
		}
		y = para(g, inHand, 16, y, W - 32, handColor, 0.85F, 2) + 6;
		// What a bond gives, and the tiers' ladder.
		g.text(font, Component.translatable("screen.wildercord.aura.blade.gives_title"), 14, y, GOLD, true);
		y += 12;
		for (String line : List.of("gives_kept", "gives_yours", "gives_grows")) {
			g.fill(17, y + 3, 20, y + 6, 0xFF000000 | color);
			y = para(g, Component.translatable("screen.wildercord.aura.blade." + line), 24, y, W - 40, TEXT, 0.8F, 3) + 2;
		}
		y += 4;
		tip = ladder(g, 14, y, mx, my, color, -1);
		return tip;
	}

	/** The four tiers in a row, each with what it asks and gives (the one reached lit). */
	private List<Component> ladder(GuiGraphicsExtractor g, int x, int y, int mx, int my, int color, int reached) {
		List<Component> tip = null;
		int cell = (W - 2 * x) / 4;
		for (int t = BladeRules.BONDED; t <= BladeRules.MAX_TIER; t++) {
			int cx = x + (t - 1) * cell;
			boolean lit = reached >= t;
			int tint = lit ? 0xFF000000 | color : reached < 0 ? 0xFF000000 | AuraHud.mix(color, 0x8A84A0, 0.5) : 0xFF3A3450;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, tierSprite(t), cx + cell / 2 - 8, y, 16, 16, tint);
			Component name = Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(t));
			int nw = Math.round(font.width(name) * 0.8F);
			small(g, name, cx + (cell - nw) / 2, y + 18, lit || reached < 0 ? TEXT : FAINT, 0.8F);
			Component asks = t == BladeRules.BONDED ? Component.translatable("screen.wildercord.aura.blade.ladder_bond")
				: Component.translatable("screen.wildercord.aura.blade.ladder_asks", (int) BladeRules.threshold(t),
					Component.translatable("aura.wildercord.stage." + AuraStages.id(BladeRules.gate(t))));
			int aw = Math.round(font.width(asks) * 0.65F);
			small(g, asks, cx + Math.max(0, (cell - aw) / 2), y + 26, DIM, 0.65F);
			if (inside(mx, my, cx, y - 2, cell, 34)) {
				List<Component> lines = new ArrayList<>();
				lines.add(name.copy().withColor(GOLD));
				lines.add(Component.translatable("screen.wildercord.aura.blade.tier_gives." + BladeRules.tierId(t)).withStyle(ChatFormatting.GRAY));
				tip = lines;
			}
		}
		return tip;
	}

	// ---------------------------------------------------------------- the blade away

	private List<Component> away(GuiGraphicsExtractor g, LocalPlayer player, BondedBlades.State state, int mx, int my, int color, int top, long now) {
		BondedBlades.Shown shown = state.shown();
		int bc = 0xFF000000 | (shown.color() == 0 ? color : shown.color());
		int y = top + 2;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_INSET, 12, y, 64, 64);
		ItemStack icon = icon(shown);
		g.pose().pushMatrix();
		g.pose().translate(20, y + 8);
		g.pose().scale(3, 3);
		g.item(icon, 0, 0);
		g.pose().popMatrix();
		g.fill(13, y + 1, 75, y + 63, 0x99140F1C);
		int tx = 86;
		Component name = shown.name().isEmpty() ? Component.translatable("screen.wildercord.aura.blade.unnamed") : Component.literal(BladeRules.cleanName(shown.name()));
		g.text(font, name, tx, y + 2, shown.name().isEmpty() ? DIM : bc, true);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, tierSprite(shown.tier()), tx, y + 15, 12, 12, bc);
		small(g, Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(Math.max(1, shown.tier()))), tx + 15, y + 17, TEXT, 0.9F);
		int ay = y + 32;
		ay = para(g, Component.translatable("screen.wildercord.aura.blade.away"), tx, ay, W - tx - 14, AMBER, 0.85F, 2);
		if (!state.lastDim().isEmpty()) {
			ay = para(g, Component.translatable("screen.wildercord.aura.blade.last_with_you", state.lastX(), state.lastY(), state.lastZ(), dimension(state.lastDim())),
				tx, ay + 2, W - tx - 14, TEXT, 0.8F, 2);
		}
		int py = y + 74;
		py = para(g, Component.translatable("screen.wildercord.aura.blade.away_how"), 16, py, W - 32, DIM, 0.8F, 6) + 6;
		return release(g, player, state.bond(), mx, my, now);
	}

	/** The blade's picture from what the page last saw of it (its item and its model). */
	private static ItemStack icon(BondedBlades.Shown shown) {
		ItemStack icon;
		try {
			icon = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(shown.item().isEmpty() ? "minecraft:iron_sword" : shown.item())));
			if (!shown.model().isEmpty()) {
				icon.set(DataComponents.ITEM_MODEL, Identifier.parse(shown.model()));
			}
		} catch (RuntimeException e) {
			icon = new ItemStack(Items.IRON_SWORD);
		}
		return icon;
	}

	private static Component dimension(String id) {
		return switch (id) {
			case "minecraft:overworld" -> Component.translatable("screen.wildercord.aura.blade.dim.overworld");
			case "minecraft:the_nether" -> Component.translatable("screen.wildercord.aura.blade.dim.nether");
			case "minecraft:the_end" -> Component.translatable("screen.wildercord.aura.blade.dim.end");
			default -> Component.literal(id);
		};
	}

	// ---------------------------------------------------------------- the blade with them

	private List<Component> bonded(GuiGraphicsExtractor g, LocalPlayer player, ItemStack blade, BladeBond b, int mx, int my, float partial, int top, long now) {
		List<Component> tip = null;
		int stage = Aura.stage(player);
		int tier = b.tier();
		int shownTier = BladeRules.effective(tier, stage);
		int bc = 0xFF000000 | (b.color() == 0 ? 0xD8D0F0 : b.color());
		int y = top + 2;
		// ---- the blade in its frame: a glow of its colour behind it, breathing, brighter by tier.
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_INSET, 12, y, 64, 64);
		float breath = 0.5F + 0.5F * (float) Math.sin((now + partial) * 0.08);
		int glowA = (int) ((0x18 + 0x10 * tier) * (0.7F + 0.3F * breath));
		for (int r = 0; r < 4; r++) {
			int inset = 4 + r * 6;
			g.fill(13 + inset, y + 1 + inset, 75 - inset, y + 63 - inset, (Math.min(255, glowA) << 24) | (bc & 0xFFFFFF));
		}
		g.fill(13, y + 1, 75, y + 2, bc);
		g.fill(13, y + 62, 75, y + 63, bc);
		g.pose().pushMatrix();
		g.pose().translate(20, y + 8);
		g.pose().scale(3, 3);
		g.item(blade, 0, 0);
		g.pose().popMatrix();
		target("blade", 12, y, 64, 64);
		if (inside(mx, my, 12, y, 64, 64)) {
			tip = BladeTooltip.lines(blade, player, true);
			List<Component> all = new ArrayList<>();
			all.add(blade.getHoverName());
			all.addAll(tip);
			tip = all;
		}
		int tx = 86;
		int right = W - 14;
		// ---- its name and tier.
		Component name = b.shownName().isEmpty() ? Component.translatable("screen.wildercord.aura.blade.unnamed") : Component.literal(b.shownName());
		g.pose().pushMatrix();
		g.pose().translate(tx, y + 1);
		g.pose().scale(1.25F, 1.25F);
		g.text(font, name, 0, 0, b.shownName().isEmpty() ? DIM : bc, true);
		g.pose().popMatrix();
		g.blitSprite(RenderPipelines.GUI_TEXTURED, tierSprite(tier), tx, y + 15, 12, 12, bc);
		Component tierLine = Component.translatable("screen.wildercord.aura.blade.tier_line", Component.translatable("aura.wildercord.blade.tier."
			+ BladeRules.tierId(tier)).withColor(GOLD), b.origin().day());
		small(g, tierLine, tx + 15, y + 17, TEXT, 0.85F);
		if (shownTier < tier) {
			small(g, Component.translatable("screen.wildercord.aura.blade.sleeps", Component.translatable("aura.wildercord.stage."
				+ AuraStages.id(BladeRules.gate(tier)))), tx + 15, y + 26, AMBER, 0.7F);
		}
		// ---- its resonance toward the next tier, and what that waits on.
		int ry = y + 34;
		double resonance = b.growth().resonance();
		int bw = right - tx;
		g.fill(tx, ry, tx + bw, ry + 5, 0xFF1E1A2A);
		int filled = (int) Math.round(bw * BladeRules.progress(resonance, tier));
		g.fill(tx, ry, tx + filled, ry + 5, bc);
		g.fill(tx, ry, tx + filled, ry + 1, 0xFF000000 | AuraHud.mix(bc & 0xFFFFFF, 0xFFFFFF, 0.4));
		String numbers = tier >= BladeRules.MAX_TIER ? String.format(Locale.ROOT, "%,d", (int) resonance)
			: String.format(Locale.ROOT, "%,d / %,d", (int) resonance, (int) BladeRules.threshold(tier + 1));
		small(g, Component.translatable("screen.wildercord.aura.blade.resonance"), tx, ry + 8, DIM, 0.75F);
		int nw = Math.round(font.width(numbers) * 0.75F);
		small(g, Component.literal(numbers), right - nw, ry + 8, TEXT, 0.75F);
		if (inside(mx, my, tx, ry - 2, bw, 16)) {
			tip = List.of(Component.translatable("screen.wildercord.aura.blade.resonance").withColor(GOLD),
				Component.translatable("screen.wildercord.aura.blade.resonance_tip").withStyle(ChatFormatting.GRAY));
		}
		BladeRules.Waiting waiting = BladeRules.waiting(tier, resonance, b.history().count(BladeRules.BOSSES), stage);
		Component wait = switch (waiting) {
			case TOP -> Component.translatable("screen.wildercord.aura.blade.waits_top");
			case STAGE -> Component.translatable("screen.wildercord.aura.blade.waits_stage", Component.translatable("aura.wildercord.blade.tier."
				+ BladeRules.tierId(tier + 1)), Component.translatable("aura.wildercord.stage." + AuraStages.id(BladeRules.gate(tier + 1))));
			case BOSS -> Component.translatable("screen.wildercord.aura.blade.waits_boss");
			default -> Component.translatable("screen.wildercord.aura.blade.waits_resonance", Component.translatable("aura.wildercord.blade.tier."
				+ BladeRules.tierId(tier + 1)));
		};
		small(g, wait, tx, ry + 16, waiting == BladeRules.Waiting.RESONANCE ? FAINT : AMBER, 0.72F);
		// ---- its name to give.
		int ny = y + 70;
		List<Component> t = nameRow(g, player, b, ny, mx, my, bc, tier, now);
		tip = t != null ? t : tip;
		g.fill(12, ny + 20, W - 12, ny + 21, 0x33000000 | (GOLD & 0xFFFFFF));
		// ---- its trait.
		int trY = ny + 25;
		t = traits(g, player, b, trY, mx, my, bc, shownTier, now);
		tip = t != null ? t : tip;
		// ---- its story.
		int sy = 214;
		t = story(g, b, sy, mx, my, bc);
		tip = t != null ? t : tip;
		// ---- releasing it.
		t = release(g, player, b.id().toString(), mx, my, now);
		return t != null ? t : tip;
	}

	/** The name row: from Named, the name being written (click to type), Suggest (one of its own names), and Name it. */
	private List<Component> nameRow(GuiGraphicsExtractor g, LocalPlayer player, BladeBond b, int y, int mx, int my, int bc, int tier, long now) {
		List<Component> tip = null;
		if (BladeRules.effective(tier, Aura.stage(player)) < BladeRules.NAMED) {
			para(g, Component.translatable("screen.wildercord.aura.blade.name_later", (int) BladeRules.threshold(BladeRules.NAMED)), 14, y + 3, W - 28, FAINT, 0.8F, 2);
			return null;
		}
		String key = b.id().toString();
		if (draft == null || !draftFor.equals(key)) {
			draft = b.shownName();
			draftFor = key;
			salt = 0;
		}
		small(g, Component.translatable("screen.wildercord.aura.blade.name_label"), 14, y + 4, GOLD, 0.85F);
		int fx = 14 + Math.round(font.width(Component.translatable("screen.wildercord.aura.blade.name_label")) * 0.85F) + 6;
		int bwSuggest = 50;
		int bwName = 50;
		int fw = W - 14 - fx - bwSuggest - bwName - 8;
		boolean hoverField = inside(mx, my, fx, y, fw, 15);
		g.fill(fx, y, fx + fw, y + 15, typing ? 0xFF2A2438 : hoverField ? 0xFF241F32 : 0xFF1A1624);
		g.fill(fx, y + 14, fx + fw, y + 15, typing ? GOLD : 0xFF3A3450);
		String shown = draft;
		boolean caret = typing && (now / 8) % 2 == 0;
		Component text = Component.literal(font.plainSubstrByWidth(shown, fw - 8, true) + (caret ? "_" : "")).withColor(bc);
		g.text(font, text, fx + 4, y + 4, bc, false);
		if (shown.isEmpty() && !typing) {
			small(g, Component.translatable("screen.wildercord.aura.blade.name_hint"), fx + 4, y + 4, FAINT, 0.8F);
		}
		target("name", fx, y, fw, 15);
		int sx = fx + fw + 4;
		boolean hoverSuggest = inside(mx, my, sx, y, bwSuggest, 14);
		button(g, sx, y, bwSuggest, Component.translatable("screen.wildercord.aura.blade.suggest"), true, hoverSuggest, TEXT);
		target("suggest", sx, y, bwSuggest, 14);
		if (hoverSuggest) {
			tip = List.of(Component.translatable("screen.wildercord.aura.blade.suggest").withColor(GOLD),
				Component.translatable("screen.wildercord.aura.blade.suggest_tip").withStyle(ChatFormatting.GRAY));
		}
		int nx = sx + bwSuggest + 4;
		String clean = BladeRules.cleanName(draft);
		boolean changed = !clean.isEmpty() && !clean.equals(b.shownName());
		boolean hoverName = inside(mx, my, nx, y, bwName, 14);
		button(g, nx, y, bwName, Component.translatable("screen.wildercord.aura.blade.name_it"), changed, hoverName, changed ? GOLD : FAINT);
		target("name_it", nx, y, bwName, 14);
		return tip;
	}

	/** Why each trait is offered, by its habit's count (its arguments for {@code .habit}). */
	static Object[] habitArgs(String trait, BladeBond b) {
		BladeBond.History h = b.history();
		return switch (trait) {
			case BladeRules.WELL_WORN -> {
				String fav = BladeRules.favourite(h.arts());
				yield new Object[] {BladeTooltip.artName(fav), h.arts().getOrDefault(fav, 0)};
			}
			case BladeRules.CLOSING_STROKE -> new Object[] {h.count(BladeRules.FINISHERS)};
			case BladeRules.SUNDERING_STEEL -> new Object[] {h.count(BladeRules.BROKEN_STANCES)};
			case BladeRules.RIPOSTE -> new Object[] {h.count(BladeRules.GUARDS)};
			case BladeRules.WIND_STEP -> new Object[] {h.count(BladeRules.STEPS)};
			case BladeRules.LONG_CRESCENT -> new Object[] {h.count(BladeRules.SLASHES)};
			case BladeRules.INKBOUND -> new Object[] {h.count(BladeRules.TECHNIQUES)};
			case BladeRules.SECOND_BLAZE -> new Object[] {h.count(BladeRules.AWAKENINGS)};
			case BladeRules.MOUNTAINFELLER -> new Object[] {h.count(BladeRules.BOSSES), h.count(BladeRules.STRONG)};
			case BladeRules.GRAVEWARDEN -> new Object[] {h.count(BladeRules.UNDEAD)};
			case BladeRules.LAST_LIGHT -> new Object[] {h.count(BladeRules.LOW)};
			case BladeRules.MOONWAKE -> new Object[] {h.count(BladeRules.NIGHT)};
			case BladeRules.RALLYING_STEEL -> new Object[] {h.count(BladeRules.ALLIED)};
			default -> new Object[0];
		};
	}

	/** A trait's name (Well-Worn Verse with its art). */
	private static Component traitName(String trait, BladeBond b, int color) {
		BladeRules.Trait t = BladeRules.trait(trait).orElse(null);
		if (t == null) {
			return Component.literal(trait).withColor(color);
		}
		MutableComponent name = Component.translatable(t.nameKey());
		return name.withColor(color);
	}

	/**
	 * Its trait: before Awakened, the habits it reads so far (what it would offer now); awakened without one, the three it offers as cards
	 * to choose from; with one, that one in full, and the others it offered (changed for levels, or free once at Soulforged).
	 */
	private List<Component> traits(GuiGraphicsExtractor g, LocalPlayer player, BladeBond b, int y, int mx, int my, int bc, int shownTier, long now) {
		List<Component> tip = null;
		String trait = b.growth().trait();
		List<String> offer = b.growth().offer();
		boolean on = Config.bladeTraits(player);
		if (shownTier < BladeRules.AWAKENED) {
			g.text(font, Component.translatable("screen.wildercord.aura.blade.trait_title"), 14, y, GOLD, true);
			int ly = para(g, Component.translatable("screen.wildercord.aura.blade.trait_later", (int) BladeRules.threshold(BladeRules.AWAKENED),
				Component.translatable("aura.wildercord.stage." + AuraStages.id(BladeRules.gate(BladeRules.AWAKENED)))), 14, y + 11, W - 28, DIM, 0.72F, 2);
			// The habits it reads so far: what it would offer if it awakened now.
			List<String> reading = BladeRules.offer(b.habits(Aura.data(player).method(), Ways.state(player).way()), b.seed());
			BladeRules.History habits = b.habits(Aura.data(player).method(), Ways.state(player).way());
			small(g, Component.translatable("screen.wildercord.aura.blade.trait_reading"), 14, ly + 2, TEXT, 0.75F);
			int cy = ly + 12;
			for (String id : reading) {
				BladeRules.Trait t = BladeRules.trait(id).orElse(null);
				if (t == null) {
					continue;
				}
				double w = BladeRules.weight(t, habits);
				g.blitSprite(RenderPipelines.GUI_TEXTURED, traitSprite(id), 16, cy, 10, 10, w > 0 ? 0xFF000000 | AuraHud.mix(bc, 0x8A84A0, 0.4) : 0x663A3450);
				small(g, traitName(id, b, w > 0 ? TEXT : FAINT), 30, cy + 2, TEXT, 0.75F);
				Component why = w > 0 ? Component.translatable(t.nameKey() + ".habit", habitArgs(id, b)) : Component.translatable("screen.wildercord.aura.blade.trait_faint");
				small(g, why, 130, cy + 2, DIM, 0.7F);
				int bx = W - 60;
				g.fill(bx, cy + 3, bx + 44, cy + 6, 0xFF1E1A2A);
				g.fill(bx, cy + 3, bx + (int) Math.round(44 * Math.min(1, w)), cy + 6, 0xFF000000 | AuraHud.mix(bc, 0x8A84A0, 0.3));
				if (inside(mx, my, 14, cy - 1, W - 28, 12)) {
					tip = List.of(Component.translatable(t.nameKey()).withColor(GOLD), Component.translatable(t.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
				}
				cy += 12;
			}
			return tip;
		}
		g.text(font, Component.translatable(trait.isEmpty() ? "screen.wildercord.aura.blade.trait_choose" : "screen.wildercord.aura.blade.trait_title"), 14, y, GOLD, true);
		if (!on) {
			small(g, Component.translatable("screen.wildercord.aura.blade.traits_off"), 120, y + 1, AMBER, 0.75F);
		} else if (!trait.isEmpty() && b.growth().rechoose()) {
			small(g, Component.translatable("screen.wildercord.aura.blade.trait_free_again"), 120, y + 1, 0xFF9FE08A, 0.75F);
		}
		// The three it offers, as cards: the chosen one framed in gold.
		int cw = (W - 28 - 8) / 3;
		int ch = 62;
		int cy = y + 12;
		for (int i = 0; i < Math.min(3, offer.size()); i++) {
			String id = offer.get(i);
			BladeRules.Trait t = BladeRules.trait(id).orElse(null);
			int cx = 14 + i * (cw + 4);
			boolean chosen = id.equals(trait);
			boolean hover = inside(mx, my, cx, cy, cw, ch);
			boolean armed = id.equals(traitArmed) && now - traitArmedAt < 60;
			g.fill(cx, cy, cx + cw, cy + ch, chosen ? 0xFF2E2840 : hover ? 0xFF262036 : 0xFF1A1624);
			int edge = chosen ? GOLD : armed ? AMBER : hover ? 0xFF000000 | AuraHud.mix(bc, 0x3A3450, 0.4) : 0xFF3A3450;
			g.fill(cx, cy, cx + cw, cy + 1, edge);
			g.fill(cx, cy + ch - 1, cx + cw, cy + ch, edge);
			g.fill(cx, cy, cx + 1, cy + ch, edge);
			g.fill(cx + cw - 1, cy, cx + cw, cy + ch, edge);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, traitSprite(id), cx + cw / 2 - 7, cy + 3, 14, 14, chosen ? GOLD : 0xFF000000 | AuraHud.mix(bc, 0xFFFFFF, 0.2));
			Component nm = traitName(id, b, chosen ? GOLD : TEXT);
			float ns = font.width(nm) > cw - 6 ? 0.7F : 0.8F;
			int nw = Math.round(font.width(nm) * ns);
			small(g, nm, cx + Math.max(2, (cw - nw) / 2), cy + 19, TEXT, ns);
			if (t != null) {
				List<FormattedCharSequence> desc = font.split(Component.translatable(t.nameKey() + ".short"), Math.round((cw - 6) / 0.62F));
				int shown = Math.min(3, desc.size());
				for (int k = 0; k < shown; k++) {
					small(g, desc.get(k), cx + 3, cy + 28 + k * 7, DIM, 0.62F);
				}
				// Why it's offered, at the card's foot (one line under a three-line description).
				List<FormattedCharSequence> habit = font.split(Component.translatable(t.nameKey() + ".habit", habitArgs(id, b)), Math.round((cw - 6) / 0.6F));
				int lines = Math.min(shown >= 3 ? 1 : 2, habit.size());
				for (int k = 0; k < lines; k++) {
					small(g, habit.get(k), cx + 3, cy + ch - 2 - 7 * (lines - k), 0xFF000000 | AuraHud.mix(bc, 0x8A84A0, 0.35), 0.6F);
				}
			}
			target("trait:" + id, cx, cy, cw, ch);
			if (hover && t != null) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(t.nameKey()).withColor(GOLD));
				lines.add(Component.translatable(t.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable(t.nameKey() + ".habit", habitArgs(id, b)).withColor(DIM));
				if (!chosen) {
					boolean free = trait.isEmpty() || b.growth().rechoose();
					lines.add(free ? Component.translatable("screen.wildercord.aura.blade.trait_click").withColor(0xFF9FE08A)
						: Component.translatable("screen.wildercord.aura.blade.trait_change", BladeRules.RECHOOSE_LEVELS).withColor(AMBER));
				}
				tip = lines;
			}
		}
		return tip;
	}

	/** Its story, in an inset: where and when, its deeds counted, its favourite art, and its latest notable deeds. */
	private List<Component> story(GuiGraphicsExtractor g, BladeBond b, int y, int mx, int my, int bc) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_INSET, 10, y, W - 20, 90);
		g.fill(13, y + 1, W - 13, y + 2, 0x66000000 | (GOLD & 0xFFFFFF));
		small(g, Component.translatable("screen.wildercord.aura.blade.story"), 16, y + 5, GOLD, 0.85F);
		BladeBond.Origin o = b.origin();
		int ly = y + 16;
		ly = para(g, Component.translatable("tooltip.wildercord.bonded_blade.bonded_on", o.day(), Component.translatable(BladeCeremony.biomeKey(o.biome())), o.x(), o.z()),
			16, ly, W - 32, TEXT, 0.72F, 2);
		if (!b.who().lineage().isEmpty()) {
			ly = para(g, Component.translatable("tooltip.wildercord.bonded_blade.lineage", String.join(", ", b.who().lineage())), 16, ly, W - 32, TEXT, 0.72F, 1);
		}
		// Its deeds counted: three to a row.
		int col = (W - 32) / 3;
		int i = 0;
		for (String id : BladeRules.SHOWN_COUNTS) {
			int n = b.history().count(id);
			int cx = 16 + (i % 3) * col;
			int cy = ly + 1 + (i / 3) * 8;
			small(g, Component.translatable("tooltip.wildercord.bonded_blade.count." + id, String.format(Locale.ROOT, "%,d", n)), cx, cy, n > 0 ? DIM : FAINT, 0.68F);
			i++;
		}
		ly += 1 + ((BladeRules.SHOWN_COUNTS.size() + 2) / 3) * 8 + 2;
		String fav = BladeRules.favourite(b.history().arts());
		if (!fav.isEmpty()) {
			small(g, Component.translatable("tooltip.wildercord.bonded_blade.favourite", BladeTooltip.artName(fav), b.history().arts().getOrDefault(fav, 0)), 16, ly,
				DIM, 0.68F);
			ly += 8;
		}
		List<BladeBond.Deed> deeds = b.history().deeds();
		int room = Math.max(0, (y + 88 - ly) / 8);
		for (int k = Math.max(0, deeds.size() - room); k < deeds.size(); k++) {
			small(g, BladeTooltip.deed(deeds.get(k)), 18, ly, TEXT, 0.68F);
			ly += 8;
		}
		return null;
	}

	/** Releasing the bond: a button at the foot (clicked twice), and what it means. */
	private List<Component> release(GuiGraphicsExtractor g, LocalPlayer player, String bond, int mx, int my, long now) {
		int bw = 92;
		int bx = W - 14 - bw;
		int by = H - 30;
		boolean armed = now >= releaseArmedAt && now - releaseArmedAt < 60;
		boolean hover = inside(mx, my, bx, by, bw, 14);
		button(g, bx, by, bw, Component.translatable(armed ? "screen.wildercord.aura.blade.release_sure" : "screen.wildercord.aura.blade.release"), true, hover,
			armed ? RED : DIM);
		target("release", bx, by, bw, 14);
		para(g, Component.translatable("screen.wildercord.aura.blade.release_note"), 14, by + 1, bx - 20, FAINT, 0.7F, 2);
		if (hover) {
			return List.of(Component.translatable("screen.wildercord.aura.blade.release").withColor(RED),
				Component.translatable("screen.wildercord.aura.blade.release_tip").withStyle(ChatFormatting.GRAY));
		}
		return null;
	}

	// ------------------------------------------------------------------ input

	private void click(float pitch) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
	}

	private void deny() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.4F, 0.4F));
	}

	/** A click at ({@code mx}, {@code my}) in the page's own coordinates; returns whether the page took it. */
	boolean mouseClicked(LocalPlayer player, int mx, int my, int button) {
		String hit = null;
		for (Map.Entry<String, int[]> e : targets.entrySet()) {
			int[] r = e.getValue();
			if (inside(mx, my, r[0], r[1], r[2], r[3])) {
				hit = e.getKey();
			}
		}
		if (hit == null) {
			typing = false;
			return false;
		}
		if (!hit.equals("name")) {
			typing = false;
		}
		long now = player.level().getGameTime();
		BladeBond b = BondedBlades.bond(BondedBlades.carried(player));
		switch (hit) {
			case "name" -> {
				typing = true;
				click(1.3F);
				return true;
			}
			case "suggest" -> {
				if (b == null) {
					return true;
				}
				salt++;
				draft = BladeRules.suggest(b.nameSeed(Aura.element(player), Ways.state(player).way()), salt);
				click(1.15F);
				return true;
			}
			case "name_it" -> {
				String clean = BladeRules.cleanName(draft == null ? "" : draft);
				if (b == null || clean.isEmpty() || clean.equals(b.shownName())) {
					deny();
					return true;
				}
				ClientPlayNetworking.send(new BondedBlades.Name(clean));
				click(1.0F);
				return true;
			}
			case "release" -> {
				String bond = BondedBlades.state(player).bond();
				if (bond.isEmpty()) {
					return true;
				}
				if (now >= releaseArmedAt && now - releaseArmedAt < 60) {
					ClientPlayNetworking.send(new BondedBlades.Release(bond));
					releaseArmedAt = -1000;
					click(0.6F);
				} else {
					releaseArmedAt = now;
					click(0.9F);
				}
				return true;
			}
			default -> {
				if (hit.startsWith("trait:") && b != null) {
					String id = hit.substring(6);
					if (id.equals(b.growth().trait())) {
						return true;
					}
					boolean free = b.growth().trait().isEmpty() || b.growth().rechoose();
					if (!free && !(id.equals(traitArmed) && now - traitArmedAt < 60)) {
						// Changing one already chosen costs levels: the first click asks, the second pays.
						traitArmed = id;
						traitArmedAt = now;
						click(0.9F);
						return true;
					}
					ClientPlayNetworking.send(new BondedBlades.Choose(id));
					traitArmed = "";
					click(1.1F);
					return true;
				}
				return false;
			}
		}
	}

	/** A key on the page; returns whether it took it. */
	boolean keyPressed(KeyEvent event) {
		if (!typing) {
			return false;
		}
		int key = event.key();
		if (key == InputConstants.KEY_BACKSPACE) {
			if (draft != null && !draft.isEmpty()) {
				draft = event.hasControlDown() ? "" : draft.substring(0, draft.offsetByCodePoints(draft.length(), -1));
			}
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_TAB || event.isEscape()) {
			typing = false;
			click(1.1F);
			return true;
		}
		return true;
	}

	/** A letter typed into the name (only what can be shown; never past its length once made safe). */
	boolean charTyped(CharacterEvent event) {
		if (!typing || !event.isAllowedChatCharacter()) {
			return false;
		}
		String typed = event.codepointAsString();
		if (typed.codePoints().anyMatch(cp -> !dev.wildercord.aura.TechniqueRules.nameCharacter(cp))) {
			return true;
		}
		String next = (draft == null ? "" : draft) + typed;
		if (BladeRules.cleanName(next).length() <= BladeRules.MAX_NAME && next.length() <= BladeRules.MAX_NAME * 2) {
			draft = next;
		}
		return true;
	}
}
