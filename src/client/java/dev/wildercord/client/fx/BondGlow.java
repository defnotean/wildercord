package dev.wildercord.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BladeRules;
import dev.wildercord.aura.BondedBlades;
import dev.wildercord.client.compat.ShaderCompat;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.UUID;

/**
 * A bonded blade's own glow, in the colour of its swordsman's aura, growing by its tier: drawn on the blade itself wherever it's drawn
 * (in a hand, first person or third, and lying on the ground), whoever holds it.
 * <ul>
 * <li><b>Bonded</b>: a vein of light down the blade's middle, beating slowly, twice and a rest, like a heart;</li>
 * <li><b>Named</b>: a row of marks along it lighting one after another from hilt to tip, as if its name ran up the steel;</li>
 * <li><b>Awakened</b>: its aura licking off the edges in flickering tongues, the vein brighter;</li>
 * <li><b>Soulforged</b>: a slow-turning ring of light about the guard, two sparks winding up the blade, a corona round all of it.</li>
 * </ul>
 * In anyone else's hands it's cold: the vein alone, grey and faint. Lying on the ground it lights a pool under itself (a column of light
 * rising from a Soulforged one, to be found from afar) and lets motes drift up ({@link #motes}). During the bond ceremony the blade
 * kindles: the vein grows up the blade as the ceremony goes. Through its swordsman's own eyes everything is held close and soft, as the
 * aura's own blade glow is ({@link AuraBlade}): it stays thin and low in the corner of the view.
 */
public final class BondGlow {
	private BondGlow() {}

	private static final Identifier SOFT = Wildercord.id("textures/entity/aura/soft.png");
	private static final Identifier HAZE = Wildercord.id("textures/entity/aura/haze.png");
	private static final RenderType SOFT_TYPE = RenderTypes.eyes(SOFT);
	private static final RenderType HAZE_TYPE = RenderTypes.eyes(HAZE);
	private static final RenderType SHADE_TYPE = RenderTypes.entityTranslucent(SOFT);
	private static final RenderType SOFT_PACK_TYPE = RenderTypes.entityTranslucentEmissive(SOFT);
	private static final RenderType HAZE_PACK_TYPE = RenderTypes.entityTranslucentEmissive(HAZE);

	/**
	 * How a blade's bond looks this frame.
	 *
	 * @param color    its colour (0xRRGGBB)
	 * @param tier     the tier it shows (0 while it's only being kindled)
	 * @param strength how bright (1 in its swordsman's hands or on the ground; faint in anyone else's)
	 * @param kindle   during the bond ceremony, how far along (0 to 1), else 1
	 * @param blaze    its swordsman awakened: it blazes
	 * @param cold     in someone else's hands: grey and faint
	 */
	public record Look(int color, int tier, float strength, float kindle, boolean blaze, boolean cold) {}

	/** What a holder's render state carries for its blade's glow: who holds it, their stage, awakened, and any ceremony under way. */
	public record Holder(UUID id, int stage, boolean awakened, int rite, long riteStart, int riteTicks, int riteColor) {}

	public static final RenderStateDataKey<Holder> HOLDER = RenderStateDataKey.create(() -> "wildercord:blade_holder");
	/** A bonded blade lying on the ground: its look, carried into the item's render state. */
	public static final RenderStateDataKey<Look> GROUND = RenderStateDataKey.create(() -> "wildercord:blade_ground");
	/** Whether it lies under a bright sky (its glow gets a shade under it). */
	public static final RenderStateDataKey<Boolean> GROUND_DAY = RenderStateDataKey.create(() -> "wildercord:blade_ground_day");

	/** The blade being drawn now (the render thread only), and whether it lies on the ground. */
	private static Look current;
	private static boolean onGround;

	static Look current() {
		return current;
	}

	static boolean onGround() {
		return onGround;
	}

	static void clear() {
		current = null;
		onGround = false;
	}

	// ------------------------------------------------------------------ whose and how

	/** Carries {@code avatar}'s part in its blade's glow into its render state. */
	public static void extract(Avatar avatar, EntityRenderState state) {
		if (!(avatar instanceof Player player)) {
			state.setData(HOLDER, null);
			return;
		}
		BondedBlades.Rite rite = player.getAttached(BondedBlades.RITE);
		state.setData(HOLDER, new Holder(player.getUUID(), Aura.look(player).stage(), Awakening.awakened(player), rite == null ? 0 : rite.kind(),
			rite == null ? 0 : rite.start(), rite == null ? 0 : rite.ticks(), rite == null ? 0 : rite.color()));
	}

	/** How {@code stack} looks held by {@code holder} at game time {@code now} (null: no bond and no ceremony). */
	static Look of(Holder holder, ItemStack stack, long now, boolean mainHand) {
		if (holder == null || stack.isEmpty()) {
			return null;
		}
		BladeBond b = BondedBlades.bond(stack);
		if (b == null) {
			if (mainHand && holder.rite() == BondedBlades.Rite.BOND && holder.riteTicks() > 0 && BondedBlades.canBond(stack)) {
				float k = Math.max(0, Math.min(1, (now - holder.riteStart()) / (float) holder.riteTicks()));
				return new Look(holder.riteColor(), 0, 1.0F, k, false, false);
			}
			return null;
		}
		if (!b.ownedBy(holder.id())) {
			return new Look(0x9A94A8, 1, 0.32F, 1, false, true);
		}
		// What a holder's stage allows of it (a passed blade sleeps until its disciple grows into it: a faint vein).
		int tier = holder.stage() <= 0 ? b.tier() : Math.max(1, Math.min(b.tier(), BladeRules.allowed(holder.stage())));
		float strength = holder.stage() > 0 && BladeRules.allowed(holder.stage()) == 0 ? 0.4F : 1.0F;
		int color = b.color() == 0 ? 0xD8D0F0 : b.color();
		float kindle = holder.rite() == BondedBlades.Rite.PASS ? 1.0F : 1.0F;
		return new Look(color, tier, strength, kindle, holder.awakened(), false);
	}

	/** The local player as a holder (first person). */
	static Holder local(LocalPlayer player) {
		BondedBlades.Rite rite = player.getAttached(BondedBlades.RITE);
		return new Holder(player.getUUID(), Math.max(Aura.stage(player), Aura.look(player).stage()), Awakening.awakened(player), rite == null ? 0 : rite.kind(),
			rite == null ? 0 : rite.start(), rite == null ? 0 : rite.ticks(), rite == null ? 0 : rite.color());
	}

	/** Third person: a hand's item is about to be drawn. */
	static void beginThirdPerson(EntityRenderState state, boolean mainHand, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		long now = mc.level == null ? 0 : mc.level.getGameTime();
		current = of(state.getData(HOLDER), stack, now, mainHand);
		onGround = false;
	}

	/** First person: the local player's item is about to be drawn. */
	static void beginFirstPerson(boolean mainHand, ItemStack stack) {
		Minecraft mc = Minecraft.getInstance();
		current = mc.player == null || mc.level == null ? null : of(local(mc.player), stack, mc.level.getGameTime(), mainHand);
		onGround = false;
	}

	/** A bonded blade lying on the ground is about to be drawn. */
	static void beginGround(Look look) {
		current = look;
		onGround = look != null;
	}

	/** An item entity's look, as it's extracted (null: not a bonded blade). */
	public static Look ground(ItemEntity entity) {
		BladeBond b = BondedBlades.bond(entity.getItem());
		if (b == null) {
			return null;
		}
		return new Look(b.color() == 0 ? 0xD8D0F0 : b.color(), Math.max(1, b.tier()), 1.0F, 1, false, false);
	}

	// ------------------------------------------------------------------ on the blade

	/** A heart's beat: two quick pulses, then a rest (0 to 1), over a 48-tick cycle, quicker when it blazes. */
	static float beat(float time, boolean blaze) {
		float period = blaze ? 26F : 48F;
		float t = ((time % period) + period) % period;
		float a = Math.max(0, 1 - Math.abs(t - 3) / 3.0F);
		float b = Math.max(0, 1 - Math.abs(t - 10) / 3.5F) * 0.7F;
		return Math.max(a, b);
	}

	/** Draws {@code look} round the blade's outline {@code s} (the item model's own space), {@code fp} through its swordsman's own eyes. */
	static void draw(PoseStack pose, SubmitNodeCollector collector, AuraBlade.Shape s, Look look, float time, boolean fp) {
		if (ShaderCompat.shadowPass()) {
			return;
		}
		Look l = look;
		collector.order(4).submitCustomGeometry(pose, SOFT_TYPE, (p, buffer) -> veinGlow(p, buffer, s, l, time, fp));
		collector.order(4).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> vein(p, buffer, s, l, time, fp));
		if (l.tier() >= BladeRules.NAMED && !l.cold()) {
			collector.order(5).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> marks(p, buffer, s, l, time, fp));
		}
		if (l.tier() >= BladeRules.AWAKENED && !l.cold() && s.flat()) {
			collector.order(5).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> tongues(p, buffer, s, l, time, fp));
		}
		if (l.tier() >= BladeRules.SOULFORGED && !l.cold()) {
			collector.order(4).submitCustomGeometry(pose, SOFT_TYPE, (p, buffer) -> corona(p, buffer, s, l, time, fp));
			if (s.flat()) {
				collector.order(6).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> rim(p, buffer, s, l, time, fp));
			}
			collector.order(6).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> halo(p, buffer, s, l, time, fp));
		}
	}

	/** Soulforged: the blade's whole outline burning white-hot, a light running round it. */
	private static void rim(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float[] e = s.edges();
		float base = alpha(l, fp) * (fp ? 0.4F : 0.75F);
		float th = fp ? 0.008F : 0.016F;
		int white = hot(l, 0.75F);
		float run = (time * 0.02F) % 1.0F;
		for (int i = 0; i < e.length; i += 6) {
			float ax = e[i];
			float ay = e[i + 1];
			float bx = e[i + 2];
			float by = e[i + 3];
			float nx = e[i + 4];
			float ny = e[i + 5];
			float midT = (((ax + bx) * 0.5F - s.baseX()) * s.axisX() + ((ay + by) * 0.5F - s.baseY()) * s.axisY()) / Math.max(1.0E-3F, s.length());
			if (midT < 0.2F) {
				continue;
			}
			// A brighter band runs from the hilt to the tip and round again.
			float near = Math.max(0, 1 - Math.abs(((midT - run) % 1.0F + 1.0F) % 1.0F - 0.5F) * 2.0F);
			float a = base * (0.55F + 0.45F * near * near);
			for (float dz : new float[] {0.034F, -0.034F}) {
				float z = s.z() + dz;
				AuraBlade.quadRaw(buffer, pose, ax - nx * th * 0.3F, ay - ny * th * 0.3F, z, bx - nx * th * 0.3F, by - ny * th * 0.3F, z, bx + nx * th, by + ny * th, z,
					ax + nx * th, ay + ny * th, z, white, a, a, a, a);
			}
		}
	}

	private static int hot(Look l, float k) {
		return AuraBlade.mix(l.color(), 0xFFFFFF, k);
	}

	private static float alpha(Look l, boolean fp) {
		float a = l.strength() * (fp ? 0.55F : 1.0F);
		return l.blaze() ? Math.min(1.4F, a * 1.3F) : a;
	}

	/** The vein: a bright line down the blade's middle, in front of and behind it, beating; kindled from the hilt up during the ceremony. */
	private static void vein(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float len = s.length();
		float from = 0.2F * len;
		float to = from + (0.96F * len - from) * l.kindle();
		if (to - from < 1.0E-3) {
			return;
		}
		float beat = l.kindle() < 1 ? 0.6F + 0.4F * Mth.sin(time * 0.5F) : beat(time, l.blaze());
		float a = alpha(l, fp) * (0.5F + 0.45F * beat) * (l.tier() >= BladeRules.AWAKENED ? 1.15F : 1.0F);
		float w = (fp ? 0.016F : 0.026F) * (l.tier() >= BladeRules.AWAKENED ? 1.35F : 1.0F);
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float x0 = s.baseX() + ax * from;
		float y0 = s.baseY() + ay * from;
		float x1 = s.baseX() + ax * to;
		float y1 = s.baseY() + ay * to;
		int c = l.cold() ? 0xC8C4D4 : hot(l, 0.55F);
		for (float dz : new float[] {0.036F, -0.036F}) {
			float z = s.z() + dz;
			AuraBlade.quadRaw(buffer, pose, x0 - px * w, y0 - py * w, z, x1 - px * w * 0.4F, y1 - py * w * 0.4F, z, x1 + px * w * 0.4F, y1 + py * w * 0.4F, z,
				x0 + px * w, y0 + py * w, z, c, a * 0.7F, a, a, a * 0.7F);
		}
	}

	/** A soft glow round the vein. */
	private static void veinGlow(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float len = s.length();
		float from = 0.1F * len;
		float to = from + (1.02F * len - from) * l.kindle();
		float beat = l.kindle() < 1 ? 0.7F : beat(time, l.blaze());
		float a = alpha(l, fp) * (0.22F + 0.16F * beat) * (0.8F + 0.12F * l.tier());
		float half = (fp ? 0.06F : 0.09F) + 0.015F * l.tier();
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float bx = s.baseX() + ax * from;
		float by = s.baseY() + ay * from;
		float tx = s.baseX() + ax * to;
		float ty = s.baseY() + ay * to;
		float z = s.z();
		int c = l.cold() ? 0x8A8698 : l.color();
		AuraBlade.textured(buffer, pose, bx - px * half, by - py * half, z, tx - px * half, ty - py * half, z, tx + px * half, ty + py * half, z,
			bx + px * half, by + py * half, z, c, a);
		AuraBlade.textured(buffer, pose, bx, by, z - half, tx, ty, z - half, tx, ty, z + half, bx, by, z + half, c, a * 0.8F);
	}

	/** Named: six marks up the blade, lighting one after another from hilt to tip, its name running up the steel. */
	private static void marks(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float len = s.length();
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float wave = (time * 0.035F) % 1.8F;
		float size = fp ? 0.022F : 0.04F;
		int c = hot(l, 0.75F);
		for (int i = 0; i < 6; i++) {
			float t = 0.3F + 0.105F * i;
			if (t > l.kindle()) {
				break;
			}
			float glow = Math.max(0, 1 - Math.abs(wave - i * 0.2F) * 3.0F);
			float a = alpha(l, fp) * (0.4F + 0.6F * glow);
			float side = (i % 2 == 0 ? 1 : -1) * (fp ? 0.03F : 0.042F);
			float cx = s.baseX() + ax * len * t + px * side;
			float cy = s.baseY() + ay * len * t + py * side;
			for (float dz : new float[] {0.04F, -0.04F}) {
				float z = s.z() + dz;
				// A small diamond, its long axis along the blade.
				AuraBlade.quadRaw(buffer, pose, cx - ax * size * 1.6F, cy - ay * size * 1.6F, z, cx + px * size, cy + py * size, z,
					cx + ax * size * 1.6F, cy + ay * size * 1.6F, z, cx - px * size, cy - py * size, z, c, a, a, a, a);
			}
		}
	}

	/** Awakened: its aura licking off the edges in tongues that flicker and grow, brightest near the tip. */
	private static void tongues(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float[] e = s.edges();
		float base = alpha(l, fp) * (fp ? 0.45F : 0.85F);
		int c = l.color();
		int hot = hot(l, 0.65F);
		float reach = (fp ? 0.06F : 0.2F) * (l.tier() >= BladeRules.SOULFORGED ? 1.3F : 1.0F) * (l.blaze() ? 1.25F : 1.0F);
		for (int i = 0; i < e.length; i += 6) {
			float ax = e[i];
			float ay = e[i + 1];
			float bx = e[i + 2];
			float by = e[i + 3];
			float nx = e[i + 4];
			float ny = e[i + 5];
			float midT = (((ax + bx) * 0.5F - s.baseX()) * s.axisX() + ((ay + by) * 0.5F - s.baseY()) * s.axisY()) / Math.max(1.0E-3F, s.length());
			if (midT < 0.18F) {
				continue;
			}
			// Each edge's tongue flickers on its own, longer toward the tip.
			float seed = (ax * 37.1F + ay * 91.7F) % 6.28F;
			float flick = 0.55F + 0.45F * Mth.sin(time * 0.42F + seed) * Mth.sin(time * 0.17F + seed * 1.7F);
			float len = reach * (0.5F + 0.7F * Math.min(1, midT)) * flick;
			float a = base * (0.6F + 0.4F * flick) * Math.min(1, midT * 1.6F);
			float z = s.z();
			// Out along the edge's normal, leaning a little toward the tip.
			float ox = nx + s.axisX() * 0.6F;
			float oy = ny + s.axisY() * 0.6F;
			float ol = Mth.sqrt(ox * ox + oy * oy);
			ox /= ol;
			oy /= ol;
			float mx = (ax + bx) * 0.5F;
			float my = (ay + by) * 0.5F;
			AuraBlade.quadRaw(buffer, pose, ax, ay, z, bx, by, z, mx + ox * len, my + oy * len, z, mx + ox * len, my + oy * len, z, hot, a, a, 0, 0);
			AuraBlade.quadRaw(buffer, pose, ax, ay, z, bx, by, z, mx + ox * len * 1.5F, my + oy * len * 1.5F, z, mx + ox * len * 1.5F, my + oy * len * 1.5F, z,
				c, a * 0.5F, a * 0.5F, 0, 0);
		}
	}

	/** Soulforged: a soft corona round the whole blade, breathing. */
	private static void corona(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float breath = 0.8F + 0.2F * Mth.sin(time * 0.09F);
		float a = alpha(l, fp) * (fp ? 0.3F : 0.42F) * breath;
		float len = s.length();
		float from = -0.1F * len;
		float to = len * 1.25F;
		float half = s.spread() + (fp ? 0.12F : 0.4F);
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float bx = s.baseX() + ax * from;
		float by = s.baseY() + ay * from;
		float tx = s.baseX() + ax * to;
		float ty = s.baseY() + ay * to;
		float z = s.z();
		AuraBlade.textured(buffer, pose, bx - px * half, by - py * half, z, tx - px * half, ty - py * half, z, tx + px * half, ty + py * half, z,
			bx + px * half, by + py * half, z, l.color(), a);
		AuraBlade.textured(buffer, pose, bx, by, z - half, tx, ty, z - half, tx, ty, z + half, bx, by, z + half, l.color(), a * 0.7F);
	}

	/** Soulforged: a ring of light turning slowly about the guard, and two sparks winding up the blade. */
	private static void halo(PoseStack.Pose pose, VertexConsumer buffer, AuraBlade.Shape s, Look l, float time, boolean fp) {
		float len = s.length();
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float a = alpha(l, fp) * (fp ? 0.5F : 0.85F);
		int hot = hot(l, 0.5F);
		// The ring: a circle of dashes about the guard, lying on the blade's flat (front and back), turning slowly.
		float gx = s.baseX() + ax * len * 0.26F;
		float gy = s.baseY() + ay * len * 0.26F;
		float r = (fp ? 0.11F : 0.2F) + 0.012F * Mth.sin(time * 0.1F);
		int dashes = 12;
		float spin = time * 0.05F;
		float th = fp ? 0.008F : 0.016F;
		for (float dz : new float[] {0.045F, -0.045F}) {
			float z = s.z() + dz;
			for (int i = 0; i < dashes; i++) {
				float t0 = spin + Mth.TWO_PI * i / dashes;
				float t1 = t0 + Mth.TWO_PI / dashes * 0.6F;
				float c0 = Mth.cos(t0);
				float s0 = Mth.sin(t0);
				float c1 = Mth.cos(t1);
				float s1 = Mth.sin(t1);
				float shine = 0.65F + 0.35F * Mth.sin(t0 * 2 + time * 0.1F);
				AuraBlade.quadRaw(buffer, pose, gx + c0 * (r - th), gy + s0 * (r - th), z, gx + c1 * (r - th), gy + s1 * (r - th), z, gx + c1 * (r + th),
					gy + s1 * (r + th), z, gx + c0 * (r + th), gy + s0 * (r + th), z, hot, a * shine, a * shine, a * shine, a * shine);
			}
		}
		// Two sparks winding up the blade, a half turn apart.
		for (int k = 0; k < 2; k++) {
			float phase = ((time * 0.012F + k * 0.5F) % 1.0F);
			float t = 0.25F + 0.95F * phase;
			float turn = phase * Mth.TWO_PI * 2.2F + k * Mth.PI;
			float rad = s.spread() * 0.9F + 0.04F;
			float cx = s.baseX() + ax * len * t + px * rad * Mth.cos(turn);
			float cy = s.baseY() + ay * len * t + py * rad * Mth.cos(turn);
			float cz = s.z() + rad * Mth.sin(turn);
			float size = fp ? 0.014F : 0.034F;
			float fade = Math.min(1, Math.min(phase * 6, (1 - phase) * 4));
			AuraBlade.quadRaw(buffer, pose, cx - size, cy, cz, cx, cy + size, cz, cx + size, cy, cz, cx, cy - size, cz, 0xFFFFFF, a * fade, a * fade, a * fade,
				a * fade);
			AuraBlade.quadRaw(buffer, pose, cx, cy, cz - size, cx, cy + size, cz, cx, cy, cz + size, cx, cy - size, cz, hot, a * fade, a * fade, a * fade, a * fade);
		}
	}

	// ------------------------------------------------------------------ on the ground

	/**
	 * Under a bonded blade lying on the ground (the item entity's own space, its feet at the origin): a pool of its light, wider by tier;
	 * from a Soulforged one, a thin column of light rising, so it can be found from afar.
	 */
	public static void groundExtras(PoseStack pose, SubmitNodeCollector collector, Look look, float time, Quaternionf facing, boolean day) {
		if (look == null || ShaderCompat.shadowPass()) {
			return;
		}
		if (day) {
			// By day added light alone washes out on bright ground: a soft shade of its colour lies under the pool, so the light reads.
			collector.order(0).submitCustomGeometry(pose, SHADE_TYPE, (p, buffer) -> shade(p, buffer, look));
		}
		// Under a shader pack the ground's lights are drawn as emissive translucency (a pack lights glowing eyes as it pleases, and a
		// tall column drawn that way came out dark at night).
		boolean pack = ShaderCompat.active();
		RenderType soft = pack ? SOFT_PACK_TYPE : SOFT_TYPE;
		collector.order(1).submitCustomGeometry(pose, soft, (p, buffer) -> pool(p, buffer, look, time));
		// A soft glow round it, turned to whoever looks, so a blade lying in the grass is seen from a way off.
		pose.pushPose();
		pose.translate(0, 0.3F, 0);
		pose.rotate(facing);
		collector.order(1).submitCustomGeometry(pose, soft, (p, buffer) -> aura(p, buffer, look, time));
		pose.popPose();
		if (look.tier() >= BladeRules.SOULFORGED) {
			collector.order(1).submitCustomGeometry(pose, pack ? HAZE_PACK_TYPE : HAZE_TYPE, (p, buffer) -> column(p, buffer, look, time));
		}
	}

	/** A deep shade of the blade's colour, wider than its pool (drawn as plain translucency: it darkens what's under it). */
	private static void shade(PoseStack.Pose pose, VertexConsumer buffer, Look l) {
		float r = (0.38F + 0.12F * l.tier()) * 1.35F;
		float y = 0.02F;
		int deep = AuraBlade.mix(l.color(), 0x000000, 0.82F);
		AuraBlade.textured(buffer, pose, -r, y, -r, -r, y, r, r, y, r, r, y, -r, deep, 0.55F);
	}

	/** Whether a blade lying at {@code entity} lies under a bright sky (day, nothing overhead). */
	public static boolean day(ItemEntity entity) {
		return entity.level().isBrightOutside() && entity.level().canSeeSky(entity.blockPosition());
	}

	private static void pool(PoseStack.Pose pose, VertexConsumer buffer, Look l, float time) {
		float r = 0.38F + 0.12F * l.tier();
		float a = (0.4F + 0.08F * l.tier()) * (0.75F + 0.25F * beat(time, false));
		float y = 0.03F;
		// Two quads, crossed, so the soft blob reads round.
		AuraBlade.textured(buffer, pose, -r, y, -r, -r, y, r, r, y, r, r, y, -r, l.color(), a);
		AuraBlade.textured(buffer, pose, -r, y, r, r, y, r, r, y, -r, -r, y, -r, l.color(), a * 0.8F);
	}

	/** The glow round a blade on the ground, facing the camera: wider and brighter by tier, beating with it. */
	private static void aura(PoseStack.Pose pose, VertexConsumer buffer, Look l, float time) {
		float r = 0.34F + 0.08F * l.tier();
		float a = (0.2F + 0.06F * l.tier()) * (0.7F + 0.3F * beat(time, false));
		AuraBlade.textured(buffer, pose, -r, -r, 0, -r, r, 0, r, r, 0, r, -r, 0, l.color(), a);
		float k = r * 0.45F;
		AuraBlade.textured(buffer, pose, -k, -k, 0, -k, k, 0, k, k, 0, k, -k, 0, hot(l, 0.5F), a * 0.8F);
	}

	private static void column(PoseStack.Pose pose, VertexConsumer buffer, Look l, float time) {
		float h = 3.6F;
		float w = 0.07F + 0.015F * Mth.sin(time * 0.15F);
		float a = 0.45F;
		int hot = hot(l, 0.65F);
		for (int k = 0; k < 2; k++) {
			float dx = k == 0 ? w : 0;
			float dz = k == 0 ? 0 : w;
			AuraBlade.quadRaw(buffer, pose, -dx, 0.05F, -dz, -dx, h, -dz, dx, h, dz, dx, 0.05F, dz, hot, a, 0, 0, a);
			AuraBlade.quadRaw(buffer, pose, -dx * 2.5F, 0.05F, -dz * 2.5F, -dx * 2.5F, h * 0.7F, -dz * 2.5F, dx * 2.5F, h * 0.7F, dz * 2.5F, dx * 2.5F, 0.05F,
				dz * 2.5F, l.color(), a * 0.4F, 0, 0, a * 0.4F);
		}
	}

	/** Motes drifting up off bonded blades lying near the player, a few a second (more by tier). */
	public static void motes(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || mc.player == null || mc.isPaused() || level.getGameTime() % 4 != 0) {
			return;
		}
		RandomSource random = level.getRandom();
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(32), e -> BondedBlades.bonded(e.getItem()))) {
			Look look = ground(entity);
			if (look == null || random.nextFloat() > 0.25F + 0.15F * look.tier()) {
				continue;
			}
			double a = random.nextDouble() * Math.PI * 2;
			double r = 0.15 + random.nextDouble() * 0.25;
			Vec3 at = entity.position().add(Math.cos(a) * r, 0.15 + random.nextDouble() * 0.3, Math.sin(a) * r);
			int life = 26 + random.nextInt(18);
			int color = random.nextInt(3) == 0 ? AuraBlade.mix(look.color(), 0xFFFFFF, 0.5F) : look.color();
			mc.particleEngine.add(Glimmer.mote(level, at, color, 0.05F + random.nextFloat() * 0.03F, 0.7F, life, 0, 0.012 + 0.004 * look.tier(), 0, 0.003F));
		}
	}
}
