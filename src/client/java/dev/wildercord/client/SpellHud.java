package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;
import java.util.Locale;

/**
 * The spell panel right of the hotbar, bottom-aligned with it:
 * <pre>
 *  ╭────╮ [rune][rune][rune]…      46
 *  │ 1  │ [██████████|░░░░░░░░░░░░]      ← gold mark: how much this spell costs
 *  ╰────╯ 112/300                 1.2s
 *   • • ·   (one dot per spell the Cord holds)
 * </pre>
 * Everything is laid out from measured text widths and moves aside for an offhand slot or
 * attack indicator on the right, so it stays clean at every GUI scale. Hidden without a Cord.
 */
public final class SpellHud {
	private SpellHud() {}

	private static final Identifier FRAME = Wildercord.id("hud/frame");
	private static final Identifier BADGE = Wildercord.id("hud/badge");
	private static final Identifier BAR = Wildercord.id("hud/bar_frame");
	private static final Identifier FILL = Wildercord.id("hud/mana_fill");

	private static final int HEIGHT = 32;
	private static final int BODY_X = 28;
	private static final int MIN_BODY = 58;

	private static final int GOLD = 0xFFE8C46A;
	private static final int LAVENDER = 0xFFB8A8FF;
	private static final int DIM = 0xFF8A84A0;
	private static final int RED = 0xFFE06060;

	/** Smoothed mana so the bar glides instead of jumping in quarter-second steps. */
	private static float shownMana = -1;

	public static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("spell_hud"), SpellHud::extract);
	}

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	private static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.isSpectator()) {
			return;
		}
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			shownMana = -1;
			return;
		}
		Font font = mc.font;
		Spellbook book = Spellbooks.get(player);
		int spell = Math.min(book.selected(), tier.spells - 1);
		List<RuneDef> runes = SpellCaster.activeRunes(book, spell, tier);
		SpellCompiler.Compiled compiled = runes.isEmpty() ? null : SpellCompiler.compile(runes);
		Mana.Stats stats = Mana.of(player);
		int maxMana = Math.max(1, stats.max());
		float mana = Spellbooks.mana(player);
		shownMana = shownMana < 0 ? mana : shownMana + (mana - shownMana) * 0.3F;
		boolean creative = player.isCreative();
		boolean blood = compiled != null && compiled.paysInHealth();
		int manaCost = compiled == null ? 0 : dev.wildercord.player.Heart.manaCost(player, compiled);
		int healthCost = compiled == null ? 0 : dev.wildercord.player.Heart.healthCost(player, compiled);
		boolean affordable = compiled == null || creative || (blood ? player.getHealth() > healthCost : mana >= manaCost);
		long remaining = Spellbooks.readyAt(player, spell) - player.level().getGameTime();
		boolean cooling = compiled != null && remaining > 0;

		// ---- where: right of the hotbar, clear of an offhand slot or attack indicator on that side.
		int center = g.guiWidth() / 2;
		int x0 = center + 91 + 5;
		HumanoidArm offhandSide = player.getMainArm().getOpposite();
		if (offhandSide == HumanoidArm.RIGHT && !player.getOffhandItem().isEmpty()) {
			x0 += 29;
		}
		if (offhandSide == HumanoidArm.LEFT && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
			x0 += 23;
		}
		int y0 = g.guiHeight() - HEIGHT;
		int avail = g.guiWidth() - x0 - 2;

		// ---- how wide: measured, then shrunk to fit.
		// Blood Price spells show their health cost (with a heart) instead of mana.
		String cost = compiled == null ? "" : blood ? healthCost + "\u2764" : Integer.toString(manaCost);
		int costW = font.width(cost);
		int iconSize = 10;
		int shown = runes.size();
		int bodyW = bodyWidth(shown, iconSize, costW, font);
		if (BODY_X + bodyW + 4 > avail) {
			iconSize = 8;
			bodyW = bodyWidth(shown, iconSize, costW, font);
		}
		String more = "";
		while (shown > 1 && BODY_X + bodyW + 4 > avail) {
			shown--;
			more = "+" + (runes.size() - shown);
			bodyW = Math.max(MIN_BODY, shown * iconSize + 2 + font.width(more) + 4 + costW);
		}
		int width = BODY_X + bodyW + 4;
		if (width > avail) {
			// Very narrow window: tuck the panel into the bottom-right corner.
			x0 = Math.max(2, g.guiWidth() - width - 2);
		}

		sprite(g, FRAME, x0, y0, width, HEIGHT);

		// ---- badge: spell number, cooldown shade, and one dot per spell.
		int bx = x0 + 4;
		int by = y0 + 4;
		sprite(g, BADGE, bx, by, 20, 20);
		if (cooling) {
			int total = Math.max(1, dev.wildercord.player.Heart.cooldownTicks(player, compiled));
			int shade = (int) Math.ceil(14 * Math.min(1.0, remaining / (double) total));
			g.fill(bx + 3, by + 3 + (14 - shade), bx + 17, by + 17, 0x90000000);
		}
		String number = Integer.toString(spell + 1);
		int numberColor = compiled == null ? DIM : !affordable ? RED : GOLD;
		g.text(font, number, bx + 10 - font.width(number) / 2, by + 6, numberColor, true);
		if (tier.spells > 1) {
			int dotsW = tier.spells * 4 - 2;
			int dx = bx + 10 - dotsW / 2;
			for (int i = 0; i < tier.spells; i++) {
				g.fill(dx + i * 4, by + 22, dx + i * 4 + 2, by + 24, i == spell ? GOLD : 0xFF4A4060);
			}
		}

		// ---- row 1: runes and cost.
		int rx = x0 + BODY_X;
		int rowY = y0 + 4;
		if (compiled == null) {
			g.text(font, "K", rx, rowY + 1, DIM, true);
		} else {
			float scale = iconSize / 16.0F;
			g.pose().pushMatrix();
			g.pose().translate(rx, rowY + (10 - iconSize) / 2.0F);
			g.pose().scale(scale, scale);
			for (int i = 0; i < shown; i++) {
				g.item(CordScreen.icon(runes.get(i).id()), i * 16, 0);
			}
			g.pose().popMatrix();
			if (!more.isEmpty()) {
				g.text(font, more, rx + shown * iconSize + 2, rowY + 1, DIM, true);
			}
			g.text(font, cost, x0 + width - 4 - costW, rowY + 1, !affordable ? RED : blood ? 0xFFFF6A78 : LAVENDER, true);
		}

		// ---- row 2: the mana bar, with a gold mark at this spell's cost.
		int barY = y0 + 15;
		sprite(g, BAR, rx, barY, bodyW, 6);
		int inner = bodyW - 2;
		int filled = (int) (inner * Math.min(1.0F, Math.max(0.0F, shownMana) / maxMana));
		if (filled > 0) {
			sprite(g, FILL, rx + 1, barY + 1, filled, 4);
			if (stats.meditating() || stats.clarity() > 0) {
				// A soft highlight sweeping along the bar while regeneration is boosted.
				long t = player.level().getGameTime();
				int sweep = (int) ((t * 2) % (inner + 12)) - 6;
				int from = Math.max(0, sweep);
				int to = Math.min(filled, sweep + 6);
				if (to > from) {
					g.fill(rx + 1 + from, barY + 1, rx + 1 + to, barY + 5, 0x70FFFFFF);
				}
			}
		}
		if (compiled != null && !creative && !blood) {
			int mark = rx + 1 + (int) Math.min(inner - 1, inner * manaCost / (float) maxMana);
			g.fill(mark, barY - 1, mark + 1, barY + 7, affordable ? GOLD : RED);
		}

		// ---- row 3: mana count and cooldown.
		int textY = y0 + 22;
		String manaText = (int) mana + "/" + maxMana;
		g.text(font, manaText, rx, textY, stats.boosted() ? 0xFF7FE0F0 : 0xFFA898E8, true);
		if (stats.boosted()) {
			// A small up-chevron: regeneration is boosted right now.
			int cx = rx + font.width(manaText) + 2;
			g.fill(cx + 2, textY + 1, cx + 3, textY + 2, 0xFF7FE0F0);
			g.fill(cx + 1, textY + 2, cx + 4, textY + 3, 0xFF7FE0F0);
			g.fill(cx, textY + 3, cx + 5, textY + 4, 0xFF7FE0F0);
		}
		if (cooling) {
			String time = String.format(Locale.ROOT, "%.1fs", remaining / 20.0);
			int tw = font.width(time);
			if (rx + font.width(manaText) + 4 + tw <= x0 + width - 4) {
				g.text(font, time, x0 + width - 4 - tw, textY, GOLD, true);
			}
		} else {
			// Passives running: how much mana they drain every second.
			float upkeep = CordScreen.passiveUpkeep(player);
			if (upkeep > 0 && !creative) {
				String drain = "-" + String.format(Locale.ROOT, "%.1f", upkeep) + "/s";
				int dw = font.width(drain);
				if (rx + font.width(manaText) + 8 + dw <= x0 + width - 4) {
					g.text(font, drain, x0 + width - 4 - dw, textY, mana < upkeep ? RED : 0xFF9A8CD8, true);
				}
			}
		}
	}

	private static int bodyWidth(int icons, int iconSize, int costW, Font font) {
		return Math.max(MIN_BODY, Math.max(icons * iconSize + (costW > 0 ? 4 + costW : 0), font.width("888/888") + 8));
	}
}
