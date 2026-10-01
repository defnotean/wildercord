package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStages;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The aura bar, beside the mana display: a slim strip on top of the spell panel (or, without a Cord, in its place right of
 * the hotbar), in the method's colour:
 * <pre>
 *  ◆◆◇  [████████░░░░░|░░]  34
 * </pre>
 * The diamonds are the stages (filled as reached; the next one pulses gold while a breakthrough waits), the bar is the aura
 * held (a gold mark at Aura Slash's price from Edge), and the number what's held. In the breathing stance a ring closes on the
 * diamonds with each breath (let sneak up and press it again as it closes: a breath on the beat), and the bar shimmers; a
 * trial under way writes its progress above the strip; backlash dims it; a raised guard edges it in gold. At the top stages
 * Form's diamond dims while Aura Step recharges and Sovereign's burns while a Dominion stands (a thread under it filling back
 * as it rests); a spell riding the blade and a Dominion's time left are written above the strip.
 */
public final class AuraHud {
	private AuraHud() {}

	private static final Identifier FRAME = Wildercord.id("hud/frame");
	private static final Identifier BAR = Wildercord.id("hud/bar_frame");
	private static final Identifier BEAT = Wildercord.id("hud/beat_ring");

	/** The strip's height, and its width when it stands alone. */
	public static final int HEIGHT = 12;
	private static final int ALONE_WIDTH = 90;
	private static final int GOLD = 0xFFE8C46A;

	/** Smoothed aura, so the bar glides. */
	private static float shown = -1;

	/** Whether the strip shows for {@code player}: aura works here and they've learned a method. */
	public static boolean showing(LocalPlayer player) {
		return player != null && !player.isSpectator() && Aura.enabled(player) && Aura.stage(player) > AuraRules.NONE;
	}

	/** How much room the strip takes above the spell panel (0 when it isn't showing). */
	public static int height(LocalPlayer player) {
		return showing(player) ? HEIGHT + 1 : 0;
	}

	/** Without a Cord: the strip alone, right of the hotbar, clear of an offhand slot or attack indicator. */
	static void alone(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (!showing(player)) {
			shown = -1;
			return;
		}
		int aside = 0;
		HumanoidArm offhandSide = player.getMainArm().getOpposite();
		if (offhandSide == HumanoidArm.RIGHT && !player.getOffhandItem().isEmpty()) {
			aside += 29;
		}
		if (offhandSide == HumanoidArm.LEFT && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
			aside += 23;
		}
		int x = g.guiWidth() / 2 + 91 + 5 + aside;
		int width = Math.min(ALONE_WIDTH, g.guiWidth() - x - 2);
		if (width < 40) {
			x = g.guiWidth() - ALONE_WIDTH - 2;
			width = ALONE_WIDTH;
		}
		draw(g, player, x, g.guiHeight() - HEIGHT - 1, width, delta.getGameTimeDeltaPartialTick(false));
	}

	/** Draws the strip at ({@code x}, {@code y}), {@code width} across. Returns the top of anything drawn above it. */
	static int draw(GuiGraphicsExtractor g, LocalPlayer player, int x, int y, int width, float partial) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		AuraAttachments.State state = Aura.state(player);
		long now = player.level().getGameTime();
		int stage = Aura.stage(player);
		int capacity = Math.max(1, Aura.capacity(player));
		float aura = Aura.aura(player);
		shown = shown < 0 ? aura : shown + (aura - shown) * 0.3F;
		int color = Aura.color(player);
		boolean backlash = now < state.backlashUntil();
		boolean guarding = state.guarding(now);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, x, y, width, HEIGHT);
		if (guarding) {
			// A raised guard: the strip edged in gold.
			int a = 0xFF;
			g.fill(x + 1, y + 1, x + width - 1, y + 2, (a << 24) | 0xF5D56A);
			g.fill(x + 1, y + HEIGHT - 2, x + width - 1, y + HEIGHT - 1, (a << 24) | 0xF5D56A);
		}

		// ---- the stages: a diamond for each open one, filled as reached; the next pulses gold while a breakthrough waits.
		// Form's diamond dims while Aura Step recharges; Sovereign's burns while a Dominion stands and fills back as it rests.
		int open = Math.max(AuraStages.highest(), stage);
		int step = open >= 5 ? 6 : 7;
		int px = x + 4;
		int py = y + 3;
		boolean ready = AuraBreakthroughs.ready(player);
		dev.wildercord.aura.AuraPresence.Timers timers = dev.wildercord.aura.AuraPresence.timers(player);
		for (int s = 1; s <= open; s++) {
			int fill;
			if (s <= stage) {
				fill = 0xFF000000 | color;
				if (s == AuraRules.FORM && now < timers.stepReadyAt()) {
					fill = 0xFF000000 | mix(color, 0x2A2438, 0.65);
				} else if (s == AuraRules.SOVEREIGN && now <= timers.dominionUntil()) {
					double pulse = 0.5 + 0.5 * Math.sin((now + partial) * 0.5);
					fill = 0xFF000000 | mix(color, 0xFFFFFF, 0.25 + 0.45 * pulse);
				} else if (s == AuraRules.SOVEREIGN && now < timers.dominionReadyAt()) {
					fill = 0xFF000000 | mix(color, 0x2A2438, 0.65);
				}
			} else if (s == stage + 1 && ready) {
				double pulse = 0.5 + 0.5 * Math.sin((now + partial) * 0.3);
				fill = 0xFF000000 | mix(0x6A5A3A, 0xF5D56A, pulse);
			} else {
				fill = 0xFF3A3450;
			}
			diamond(g, px, py, fill, s <= stage ? 0xFF000000 | AuraRules.color(color, 0xFFFFFF, 3) : 0xFF6A6080);
			if (s == AuraRules.SOVEREIGN && stage >= AuraRules.SOVEREIGN && now > timers.dominionUntil() && now < timers.dominionReadyAt()) {
				// Dominion resting: a thread under its diamond, filling back as the rest runs out.
				double rest = AuraRules.DOMINION_COOLDOWN;
				double share = 1 - (timers.dominionReadyAt() - now) / rest;
				g.fill(px, py + 6, px + 5, py + 7, 0xFF2A2438);
				g.fill(px, py + 6, px + (int) Math.round(5 * Math.max(0, Math.min(1, share))), py + 7, 0xFF000000 | color);
			}
			px += step;
		}
		int pipsRight = px;
		// The breath's beat: a ring closing on the diamonds as each breath comes, glowing on it.
		if (state.breathing()) {
			long next = AuraRules.nextBeat(state.settledAt(), now);
			double until = next - now - partial;
			boolean onBeat = AuraRules.onBeat(state.settledAt(), now);
			int cx = x + 4 + (open * step) / 2 - 1;
			int cy = y + HEIGHT / 2;
			if (onBeat) {
				g.blitSprite(RenderPipelines.GUI_TEXTURED, BEAT, cx - 9, cy - 9, 18, 18, 0xFF000000 | AuraRules.color(color, 0xFFFFFF, 4));
			} else if (until <= 16) {
				int size = 18 + (int) Math.round(16 * Math.min(1, until / 16.0));
				int alpha = (int) (0x50 + 0xAF * (1 - Math.min(1, until / 16.0)));
				g.blitSprite(RenderPipelines.GUI_TEXTURED, BEAT, cx - size / 2, cy - size / 2, size, size, (alpha << 24) | color);
			}
		}

		// ---- the bar: the aura held, in the method's colour; a gold mark at the slash's price from Edge.
		String count = Integer.toString((int) aura);
		int countW = font.width(count);
		int bx = pipsRight + 2;
		int bw = Math.max(12, x + width - 4 - countW - 3 - bx);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, BAR, bx, y + 3, bw, 6);
		int inner = bw - 2;
		int filled = (int) (inner * Math.max(0, Math.min(1, shown / capacity)));
		if (filled > 0) {
			int c = backlash ? mix(color, 0x5A4A5A, 0.6) : color;
			g.fill(bx + 1, y + 4, bx + 1 + filled, y + 5, 0xFF000000 | AuraRules.color(c, 0xFFFFFF, 4));
			g.fill(bx + 1, y + 5, bx + 1 + filled, y + 7, 0xFF000000 | c);
			g.fill(bx + 1, y + 7, bx + 1 + filled, y + 8, 0xFF000000 | mix(c, 0x000000, 0.35));
			if (state.breathing()) {
				// Breathing in: a soft highlight sweeping along the bar.
				int sweep = (int) ((now * 2) % (inner + 12)) - 6;
				int from = Math.max(0, sweep);
				int to = Math.min(filled, sweep + 6);
				if (to > from) {
					g.fill(bx + 1 + from, y + 4, bx + 1 + to, y + 8, 0x70FFFFFF);
				}
			}
		}
		if (stage >= AuraRules.EDGE) {
			double price = dev.wildercord.config.Config.slashCost(player);
			if (price > 0 && price < capacity) {
				int mark = bx + 1 + (int) Math.min(inner - 1, inner * price / capacity);
				g.fill(mark, y + 2, mark + 1, y + 10, aura >= price ? GOLD : 0xFFE06060);
			}
		}
		g.text(font, count, x + width - 4 - countW, y + 2, backlash ? 0xFFC8A0A0 : 0xFF000000 | AuraRules.color(color, 0xFFFFFF, 3), true);

		// ---- above the strip: a trial under way, a spell riding the blade, a Dominion standing.
		int top = y;
		String trial = null;
		if (state.stillness() > 0) {
			trial = net.minecraft.network.chat.Component.translatable("screen.wildercord.aura.trial.progress", state.stillness() / 20,
				AuraRules.stillnessTicks(stage + 1) / 20).getString();
		} else if (state.trialUntil() > now) {
			trial = net.minecraft.network.chat.Component.translatable("screen.wildercord.aura.trial.foe_left", (state.trialUntil() - now + 19) / 20).getString();
		}
		int lineColor = 0xFF000000 | AuraRules.color(color, 0xFFFFFF, 3);
		if (trial != null) {
			top -= 10;
			g.text(font, font.plainSubstrByWidth(trial, Math.max(40, g.guiWidth() - x - 4)), x + 3, top, lineColor, true);
		}
		dev.wildercord.aura.AuraPresence.Look presence = dev.wildercord.aura.AuraPresence.look(player);
		if (presence.spellHeld(now)) {
			top -= 10;
			String held = net.minecraft.network.chat.Component.translatable("screen.wildercord.aura.spellblade_held",
				String.format(java.util.Locale.ROOT, "%.1f", Math.max(0, presence.bladeUntil() - now - partial) / 20.0)).getString();
			double pulse = 0.5 + 0.5 * Math.sin((now + partial) * 0.6);
			g.text(font, font.plainSubstrByWidth(held, Math.max(40, g.guiWidth() - x - 4)), x + 3, top,
				0xFF000000 | mix(presence.bladeSpell(), 0xFFFFFF, 0.2 + 0.3 * pulse), true);
		}
		if (now <= timers.dominionUntil()) {
			top -= 10;
			String dominion = net.minecraft.network.chat.Component.translatable("screen.wildercord.aura.dominion_left",
				(timers.dominionUntil() - now + 19) / 20).getString();
			g.text(font, font.plainSubstrByWidth(dominion, Math.max(40, g.guiWidth() - x - 4)), x + 3, top, lineColor, true);
		}
		return top;
	}

	/** A small diamond, 5 pixels across, filled with {@code fill} and edged with {@code edge}. */
	private static void diamond(GuiGraphicsExtractor g, int x, int y, int fill, int edge) {
		g.fill(x + 2, y, x + 3, y + 1, edge);
		g.fill(x + 1, y + 1, x + 4, y + 2, edge);
		g.fill(x, y + 2, x + 5, y + 3, edge);
		g.fill(x + 1, y + 3, x + 4, y + 4, edge);
		g.fill(x + 2, y + 4, x + 3, y + 5, edge);
		g.fill(x + 2, y + 1, x + 3, y + 2, fill);
		g.fill(x + 1, y + 2, x + 4, y + 3, fill);
		g.fill(x + 2, y + 3, x + 3, y + 4, fill);
	}

	static int mix(int a, int b, double t) {
		int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int gr = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (gr << 8) | bl;
	}

	/** On leaving a world. */
	static void forget() {
		shown = -1;
	}
}
