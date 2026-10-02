package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Technique banners ({@link AuraFx#banner}): a named art, a Dominion, later a finisher, shown briefly in the method's colour.
 * <ul>
 * <li><b>Yours</b> slide in from the left edge of the screen a little above its middle, clear of the crosshair, the chat and the
 * hotbar: a woven nameplate with the Way's crest, a small line above (the method, and which art), the name across it,
 * a bright line wiping in under it; then gone. A grand one (the Final Art, a Dominion) is larger, longer and edged in gold.</li>
 * <li><b>Everyone else's</b> float over their heads, the name alone in their colour, drifting up as it fades (only while they're in
 * view and near; out of view there's nothing).</li>
 * </ul>
 * The "Technique banners" setting shows everyone's, only your own, or none.
 */
public final class AuraBanners {
	private AuraBanners() {}

	private static final Identifier BAND = Wildercord.id("hud/aura_banner");

	/** A banner showing: whose, what it says, its colour and kind, and when it began (game time). */
	record Shown(int entity, Component name, Component kicker, int color, AuraFxRules.BannerKind kind, long start) {}

	private static Shown own;
	private static final List<Shown> OTHERS = new ArrayList<>();
	private static long now;
	private static int shown;
	private static String lastName = "";

	static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("aura_banners"), AuraBanners::extract);
	}

	static void reset() {
		own = null;
		OTHERS.clear();
	}

	static void receive(AuraFx.Banner payload) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || MagicQuality.banners == MagicQuality.Banners.OFF) {
			return;
		}
		long at = mc.level.getGameTime();
		Shown s = new Shown(payload.entity(), payload.name(), payload.kicker(), payload.color(), AuraFxRules.BannerKind.of(payload.kind()), at);
		if (payload.entity() == mc.player.getId()) {
			own = s;
		} else {
			if (MagicQuality.banners != MagicQuality.Banners.ALL) {
				return;
			}
			OTHERS.removeIf(o -> o.entity() == payload.entity());
			OTHERS.add(s);
			while (OTHERS.size() > 4) {
				OTHERS.removeFirst();
			}
		}
		shown++;
		lastName = payload.name().getString();
	}

	static void tick(long time) {
		now = time;
		if (own != null && (time - own.start() > own.kind().ticks || time < own.start())) {
			own = null;
		}
		OTHERS.removeIf(s -> time - s.start() > s.kind().ticks || time < s.start());
	}

	// ------------------------------------------------------------------ drawing

	static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || MagicQuality.banners == MagicQuality.Banners.OFF) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		float time = mc.level.getGameTime() + partial;
		if (own != null) {
			drawOwn(g, mc.font, own, time - own.start());
		}
		for (Shown s : OTHERS) {
			drawOver(g, mc, s, time - s.start(), partial);
		}
	}

	/** Yours: slid in from the left edge, a little above the middle of the screen. */
	private static void drawOwn(GuiGraphicsExtractor g, Font font, Shown s, float age) {
		int ticks = s.kind().ticks;
		float alpha = AuraFxRules.bannerAlpha(age, ticks);
		if (alpha <= 0.01F) {
			return;
		}
		boolean grand = s.kind() != AuraFxRules.BannerKind.ART;
		float nameScale = grand ? 1.7F : 1.5F;
		float kickScale = 1.0F;
		String kicker = s.kicker().getString().toUpperCase(Locale.ROOT);
		String name = s.name().getString();
		// Keep even a long player-written technique outside the central aiming area.
		int available = Math.max(80, Math.round(g.guiWidth() * 0.42F) - 70);
		if (font.width(name) * nameScale > available) {
			name = font.plainSubstrByWidth(name, Math.max(20, (int) (available / nameScale) - font.width("..."))) + "...";
		}
		if (spacedWidth(font, kicker) > available) {
			kicker = font.plainSubstrByWidth(kicker, available * 3 / 4) + "...";
		}
		int nameW = Math.round(font.width(name) * nameScale);
		int kickW = Math.round(spacedWidth(font, kicker) * kickScale);
		int textW = Math.max(nameW, kickW);
		int pad = 36;
		int bandW = Math.min(Math.round(g.guiWidth() * 0.46F), textW + pad + 25);
		int bandH = Math.round(9 * nameScale) + (kicker.isEmpty() ? 10 : 21);
		int h = g.guiHeight();
		int y = Math.round(h * 0.34F) - bandH / 2;
		float slide = AuraFxRules.bannerSlide(age);
		int x = Math.round(-(bandW + 8) * (1 - slide));
		int color = s.color();
		int hot = mix(color, 0xFFFFFF, 0.65F);
		int accent = grand ? AuraGuard.PERFECT_COLOR : hot;
		// Woven fabric and stitched edging, tinted by the method and dark enough for the name to read on any sky.
		g.blitSprite(RenderPipelines.GUI_TEXTURED, BAND, x, y, bandW, bandH, argb(mix(color, 0xFFFFFF, 0.55F), alpha));
		String way = dev.wildercord.api.AuraApi.wayOf(Minecraft.getInstance().player)
			.map(dev.wildercord.api.AuraApi.Way::id).filter(dev.wildercord.aura.WayRules.BUILT_IN::contains).orElse("unknown");
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Wildercord.id("aura/way_" + way), x + 9, y + bandH / 2 - 10, 20, 20, argb(accent, alpha));
		int tx = x + pad;
		int ty = y + 6;
		if (!kicker.isEmpty()) {
			g.pose().pushMatrix();
			g.pose().translate(tx, ty);
			g.pose().scale(kickScale, kickScale);
			spaced(g, font, kicker, argb(grand ? AuraGuard.PERFECT_COLOR : mix(color, 0xFFFFFF, 0.45F), 0.95F * alpha));
			g.pose().popMatrix();
			ty += 11;
		}
		g.pose().pushMatrix();
		g.pose().translate(tx, ty);
		g.pose().scale(nameScale, nameScale);
		g.text(font, name, 0, 0, argb(hot, alpha), true);
		g.pose().popMatrix();
		// The bright line under the name, wiping in from the left, with a small diamond at its leading end.
		float wipe = Mth.clamp((age - 2) / 6F, 0, 1);
		int lineY = ty + Math.round(9 * nameScale) + 1;
		int lineW = Math.round(nameW * (1 - (1 - wipe) * (1 - wipe)));
		if (lineW > 0) {
			g.fill(tx, lineY, tx + lineW, lineY + 1, argb(accent, 0.9F * alpha));
			int dx = tx + lineW;
			g.fill(dx - 1, lineY - 1, dx + 2, lineY + 2, argb(accent, 0.9F * alpha));
			g.fill(dx, lineY - 2, dx + 1, lineY + 3, argb(0xFFFFFF, 0.8F * alpha));
		}
	}

	/** Someone else's: over their head, the name alone in their colour, drifting up as it fades. */
	private static void drawOver(GuiGraphicsExtractor g, Minecraft mc, Shown s, float age, float partial) {
		Entity entity = mc.level.getEntity(s.entity());
		if (entity == null) {
			return;
		}
		float alpha = AuraFxRules.bannerAlpha(age, s.kind().ticks);
		if (alpha <= 0.02F) {
			return;
		}
		float[] at = above(mc, entity, partial, g.guiWidth(), g.guiHeight());
		if (at == null) {
			return;
		}
		Font font = mc.font;
		boolean grand = s.kind() != AuraFxRules.BannerKind.ART;
		float scale = grand ? 1.35F : 1.1F;
		String name = s.name().getString();
		int hot = mix(s.color(), 0xFFFFFF, 0.55F);
		float rise = 10 * age / s.kind().ticks;
		g.pose().pushMatrix();
		g.pose().translate(at[0], at[1] - rise);
		g.pose().scale(scale, scale);
		int w = font.width(name);
		g.centeredText(font, Component.literal(name), 0, -4, argb(hot, alpha));
		float wipe = Mth.clamp((age - 1) / 5F, 0, 1);
		int half = Math.round(w / 2F * wipe);
		g.fill(-half, 6, half, 7, argb(grand ? AuraGuard.PERFECT_COLOR : s.color(), 0.85F * alpha));
		g.pose().popMatrix();
	}

	/** Where above {@code entity}'s head is on the screen, or null when it's out of view (or the camera itself, or far). */
	private static float[] above(Minecraft mc, Entity entity, float partial, int width, int height) {
		var camera = mc.gameRenderer.mainCamera();
		if (entity == mc.getCameraEntity() && !camera.isDetached()) {
			return null;
		}
		Vec3 head = entity.getEyePosition(partial).add(0, 0.95, 0);
		Vec3 offset = head.subtract(camera.position());
		Vector3fc forward = camera.forwardVector();
		if (offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() < 0.5 || offset.lengthSqr() > AuraFxRules.BANNER_SEEN * AuraFxRules.BANNER_SEEN) {
			return null;
		}
		Vec3 ndc = mc.gameRenderer.projectPointToScreen(head);
		if (!Double.isFinite(ndc.x) || !Double.isFinite(ndc.y) || Math.abs(ndc.x) > 0.95 || Math.abs(ndc.y) > 0.95) {
			return null;
		}
		return new float[] {(float) ((ndc.x + 1) / 2 * width), (float) ((1 - ndc.y) / 2 * height)};
	}

	/** How much a pixel between letters adds: a small heading reads spaced out. */
	private static final int SPACING = 1;

	/** Draws {@code text} from (0, 0) with its letters spaced apart. */
	private static void spaced(GuiGraphicsExtractor g, Font font, String text, int argb) {
		int x = 0;
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			g.text(font, ch, x, 0, argb, false);
			x += font.width(ch) + (cp == ' ' ? 1 : SPACING);
			i += Character.charCount(cp);
		}
	}

	/** How wide {@link #spaced} draws {@code text}. */
	private static int spacedWidth(Font font, String text) {
		int x = 0;
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			x += font.width(ch) + (cp == ' ' ? 1 : SPACING);
			i += Character.charCount(cp);
		}
		return x;
	}

	private static int argb(int rgb, float alpha) {
		return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int gr = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (gr << 8) | bl;
	}

	// ------------------------------------------------------------------ for the game tests

	/** Your own banner's name, if one is showing. */
	public static String ownShowing() {
		return own == null ? "" : own.name().getString();
	}

	/** Banners shown since the game started, and the last one's name. */
	public static int shown() {
		return shown;
	}

	public static String lastName() {
		return lastName;
	}

	/** How many of others' banners are showing. */
	public static int othersShowing() {
		return OTHERS.size();
	}
}
