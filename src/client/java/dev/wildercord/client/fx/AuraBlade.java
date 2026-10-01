package dev.wildercord.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.client.compat.ShaderCompat;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The blade's aura, drawn on the weapon itself in first and third person, growing with the stage:
 * <ul>
 * <li>Glow: a haze hugging the weapon's own silhouette, a soft rim of the aura's colour that shimmers along it;</li>
 * <li>Flow: the rim wider and brighter, light running along it from hilt to tip, and ripples leaving the blade one after
 * another, fading as they go;</li>
 * <li>Edge: a solid, translucent crystal blade of aura along the weapon, reaching past its point, bright at its facets.</li>
 * </ul>
 * The weapon's silhouette comes from its own model: the side faces a flat item's model is built from trace its outline, and
 * their spread gives the line of the blade (its point is the end toward the top of the sprite, where every handheld weapon's
 * is). A weapon drawn by a model of its own (the trident in hand) gets the glow along its length instead.
 *
 * <p>Everything is vanilla's glowing-eyes type ({@code RenderTypes.eyes}), so it draws under shader packs as it does without, and is left
 * out of a pack's shadows: it's light, not a thing (see {@link ShaderCompat}). Drawn from the synced aura look: everyone sees
 * everyone's blade.</p>
 */
public final class AuraBlade {
	private AuraBlade() {}

	private static final Identifier HAZE = Wildercord.id("textures/entity/aura/haze.png");
	private static final Identifier CRYSTAL = Wildercord.id("textures/entity/aura/crystal.png");
	private static final Identifier SOFT = Wildercord.id("textures/entity/aura/soft.png");
	/** Vanilla's glowing eyes: full bright, no shading by the light's direction, laid over what's behind. */
	private static final RenderType HAZE_TYPE = RenderTypes.eyes(HAZE);
	private static final RenderType CRYSTAL_TYPE = RenderTypes.eyes(CRYSTAL);
	private static final RenderType SOFT_TYPE = RenderTypes.eyes(SOFT);
	private static final int FULL = LightCoordsUtil.FULL_BRIGHT;

	/**
	 * How a blade's aura looks this frame.
	 *
	 * @param spell the colour of a spell riding the blade (the spellblade), or 0
	 */
	public record Glow(int color, int stage, float strength, boolean guarding, HumanoidArm arm, int spell) {
		public Glow(int color, int stage, float strength, boolean guarding, HumanoidArm arm) {
			this(color, stage, strength, guarding, arm, 0);
		}
	}

	/** The glow a player's main-hand weapon has, carried into their render state (absent: none). */
	public static final RenderStateDataKey<Glow> GLOW = RenderStateDataKey.create(() -> "wildercord:aura_blade");

	/** The blade being drawn now (the render thread only), and whether its aura has been drawn yet. */
	private static Glow current;
	private static boolean drawn;

	/** The glow {@code player}'s main-hand item has, or null. */
	public static Glow of(Player player) {
		if (player == null || player.isInvisible() || !Aura.holdsWeapon(player)) {
			return null;
		}
		AuraAttachments.Look look = Aura.look(player);
		if (look.stage() <= AuraRules.NONE) {
			return null;
		}
		dev.wildercord.aura.AuraPresence.Look presence = dev.wildercord.aura.AuraPresence.look(player);
		int spell = presence.spellHeld(player.level().getGameTime()) ? presence.bladeSpell() : 0;
		return new Glow(look.color(), look.stage(), look.lit() ? 1.0F : 0.35F, look.guarding(), player.getMainArm(), spell);
	}

	/** Carries a player's blade glow into their render state as it's extracted. */
	public static void extract(net.minecraft.world.entity.Avatar avatar, EntityRenderState state) {
		state.setData(GLOW, avatar instanceof Player player ? of(player) : null);
	}

	/** A hand's item is about to be drawn in third person: its aura goes with it if it's the main hand's. */
	public static void beginThirdPerson(EntityRenderState state, HumanoidArm arm, ItemStack stack) {
		Glow glow = state.getData(GLOW);
		current = glow != null && glow.arm() == arm && stack.is(Aura.WEAPONS) ? glow : null;
		drawn = false;
	}

	/** The local player's main-hand item is about to be drawn in first person. */
	public static void beginFirstPerson(boolean mainHand, ItemStack stack) {
		Glow glow = mainHand ? of(Minecraft.getInstance().player) : null;
		current = glow != null && stack.is(Aura.WEAPONS) ? glow : null;
		drawn = false;
	}

	public static void end() {
		current = null;
		drawn = false;
	}

	/**
	 * One layer of the item's model has been submitted, with the pose still on it (in the model's own space: a flat item
	 * fills x and y from 0 to 1 at z 0.5). The aura is drawn once, round the first layer.
	 */
	public static void layer(PoseStack pose, SubmitNodeCollector collector, ItemQuads quads, Supplier<Vector3fc[]> extents) {
		Glow glow = current;
		if (glow == null || drawn || ShaderCompat.shadowPass()) {
			return;
		}
		drawn = true;
		Shape shape = quads == null || quads.isEmpty() ? null : shape(quads);
		if (shape == null) {
			shape = fallback(extents.get());
			if (shape == null) {
				return;
			}
		}
		Minecraft mc = Minecraft.getInstance();
		float time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Shape s = shape;
		collector.order(1).submitCustomGeometry(pose, SOFT_TYPE, (p, buffer) -> soft(p, buffer, s, glow, time));
		collector.order(1).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> haze(p, buffer, s, glow, time));
		if (glow.stage() >= AuraRules.EDGE) {
			collector.order(2).submitCustomGeometry(pose, CRYSTAL_TYPE, (p, buffer) -> crystal(p, buffer, s, glow, time));
		}
		if (glow.spell() != 0) {
			collector.order(3).submitCustomGeometry(pose, SOFT_TYPE, (p, buffer) -> spellGlow(p, buffer, s, glow, time));
			collector.order(3).submitCustomGeometry(pose, HAZE_TYPE, (p, buffer) -> spellBands(p, buffer, s, glow, time));
		}
	}

	// ------------------------------------------------------------------ a spell riding the blade

	/** A spell riding the blade: a soft glow of its colour all along the weapon, beating quickly, as if it can barely be held. */
	private static void spellGlow(PoseStack.Pose pose, VertexConsumer buffer, Shape s, Glow glow, float time) {
		float beat = 0.7F + 0.3F * Mth.sin(time * 0.6F);
		float len = s.length();
		float from = -0.05F * len;
		float to = len * (glow.stage() >= AuraRules.EDGE ? 1.4F : 1.1F);
		float half = s.spread() + 0.24F;
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float b0x = s.baseX() + ax * from;
		float b0y = s.baseY() + ay * from;
		float t0x = s.baseX() + ax * to;
		float t0y = s.baseY() + ay * to;
		float z = s.z();
		int color = glow.spell();
		textured(buffer, pose, b0x - px * half, b0y - py * half, z, t0x - px * half, t0y - py * half, z, t0x + px * half, t0y + py * half, z,
			b0x + px * half, b0y + py * half, z, color, 0.55F * beat);
		float deep = half * 0.8F;
		textured(buffer, pose, b0x, b0y, z - deep, t0x, t0y, z - deep, t0x, t0y, z + deep, b0x, b0y, z + deep, color, 0.45F * beat);
	}

	/**
	 * A spell riding the blade: two bands of its colour winding up the blade from hilt to tip, white-hot at their middle, over
	 * and over, in the blade's plane and across it.
	 */
	private static void spellBands(PoseStack.Pose pose, VertexConsumer buffer, Shape s, Glow glow, float time) {
		float len = s.length() * (glow.stage() >= AuraRules.EDGE ? 1.3F : 1.05F);
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float width = s.spread() + 0.09F;
		int color = glow.spell();
		int hot = mix(color, 0xFFFFFF, 0.55F);
		int pieces = 14;
		for (int band = 0; band < 2; band++) {
			float phase = band * Mth.PI;
			for (int i = 0; i < pieces; i++) {
				float t0 = i / (float) pieces;
				float t1 = (i + 1) / (float) pieces;
				// Winding round the blade: across it in the sprite's plane and in depth, a quarter turn out of step.
				float w0 = Mth.sin(t0 * 9.0F - time * 0.45F + phase);
				float w1 = Mth.sin(t1 * 9.0F - time * 0.45F + phase);
				float d0 = Mth.cos(t0 * 9.0F - time * 0.45F + phase);
				float d1 = Mth.cos(t1 * 9.0F - time * 0.45F + phase);
				float x0 = s.baseX() + ax * len * t0 + px * width * w0;
				float y0 = s.baseY() + ay * len * t0 + py * width * w0;
				float x1 = s.baseX() + ax * len * t1 + px * width * w1;
				float y1 = s.baseY() + ay * len * t1 + py * width * w1;
				float z0 = s.z() + width * 0.7F * d0;
				float z1 = s.z() + width * 0.7F * d1;
				// Brighter on the near side of each turn, fading toward the tip.
				float a0 = (0.45F + 0.35F * Math.max(0, d0)) * (1 - 0.5F * t0);
				float a1 = (0.45F + 0.35F * Math.max(0, d1)) * (1 - 0.5F * t1);
				float th = 0.035F;
				quadRaw(buffer, pose, x0 - px * th, y0 - py * th, z0, x1 - px * th, y1 - py * th, z1, x1 + px * th, y1 + py * th, z1,
					x0 + px * th, y0 + py * th, z0, i % 3 == 0 ? hot : color, a0, a1, a1, a0);
				quadRaw(buffer, pose, x0, y0, z0 - th, x1, y1, z1 - th, x1, y1, z1 + th, x0, y0, z0 + th, i % 3 == 0 ? hot : color, a0, a1, a1, a0);
			}
		}
	}

	// ------------------------------------------------------------------ the weapon's shape

	/**
	 * A weapon's outline (each piece an edge of the silhouette in the sprite's plane, with the way out), and the line of its
	 * blade: from {@code base} (the grip's end) along {@code axis}, {@code length} long, {@code across} the perpendicular.
	 */
	record Shape(float[] edges, float baseX, float baseY, float axisX, float axisY, float length, float spread, float z, boolean flat) {}

	private static final Map<ItemQuads, Shape> SHAPES = new IdentityHashMap<>();

	private static Shape shape(ItemQuads quads) {
		Shape cached = SHAPES.get(quads);
		if (cached != null || SHAPES.containsKey(quads)) {
			return cached;
		}
		if (SHAPES.size() > 128) {
			SHAPES.clear();
		}
		Shape made = trace(quads.all());
		SHAPES.put(quads, made);
		return made;
	}

	/** Traces a flat item's outline from its side faces: each is a span of the silhouette's edge, facing out. */
	static Shape trace(List<BakedQuad> all) {
		List<float[]> edges = new ArrayList<>();
		double sx = 0;
		double sy = 0;
		float z = 0.5F;
		for (BakedQuad quad : all) {
			Direction d = quad.direction();
			if (d == Direction.NORTH || d == Direction.SOUTH) {
				z = (quad.position(0).z() + (d == Direction.SOUTH ? -1 : 1) * 0.03125F);
				continue;
			}
			// The two corners on one side of the face's depth: the span in the sprite's plane.
			Vector3fc a = quad.position(0);
			Vector3fc b = null;
			for (int i = 1; i < 4; i++) {
				Vector3fc c = quad.position(i);
				if (Math.abs(c.z() - a.z()) < 1.0E-4 && (Math.abs(c.x() - a.x()) > 1.0E-5 || Math.abs(c.y() - a.y()) > 1.0E-5)) {
					b = c;
					break;
				}
			}
			if (b == null) {
				continue;
			}
			float nx = d.getStepX();
			float ny = d.getStepY();
			if (nx == 0 && ny == 0) {
				continue;
			}
			edges.add(new float[] {a.x(), a.y(), b.x(), b.y(), nx, ny});
			sx += (a.x() + b.x()) * 0.5;
			sy += (a.y() + b.y()) * 0.5;
		}
		if (edges.size() < 4) {
			return null;
		}
		double mx = sx / edges.size();
		double my = sy / edges.size();
		double xx = 0;
		double xy = 0;
		double yy = 0;
		for (float[] e : edges) {
			double px = (e[0] + e[2]) * 0.5 - mx;
			double py = (e[1] + e[3]) * 0.5 - my;
			xx += px * px;
			xy += px * py;
			yy += py * py;
		}
		// The blade's line: the main axis of the outline's spread, its point toward the top of the sprite.
		double angle = 0.5 * Math.atan2(2 * xy, xx - yy);
		double ax = Math.cos(angle);
		double ay = Math.sin(angle);
		if (ay < 0 || Math.abs(ay) < 1.0E-3 && ax < 0) {
			ax = -ax;
			ay = -ay;
		}
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		double spread = 0;
		for (float[] e : edges) {
			for (int k = 0; k < 4; k += 2) {
				double t = (e[k] - mx) * ax + (e[k + 1] - my) * ay;
				double across = Math.abs(-(e[k] - mx) * ay + (e[k + 1] - my) * ax);
				min = Math.min(min, t);
				max = Math.max(max, t);
				spread = Math.max(spread, across);
			}
		}
		float[] flat = new float[edges.size() * 6];
		for (int i = 0; i < edges.size(); i++) {
			System.arraycopy(edges.get(i), 0, flat, i * 6, 6);
		}
		return new Shape(flat, (float) (mx + ax * min), (float) (my + ay * min), (float) ax, (float) ay, (float) (max - min), (float) spread, z, true);
	}

	/** A weapon drawn by a model of its own: its glow along the longest line of its extent. */
	private static Shape fallback(Vector3fc[] points) {
		if (points == null || points.length < 2) {
			return null;
		}
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (Vector3fc p : points) {
			minX = Math.min(minX, p.x());
			minY = Math.min(minY, p.y());
			minZ = Math.min(minZ, p.z());
			maxX = Math.max(maxX, p.x());
			maxY = Math.max(maxY, p.y());
			maxZ = Math.max(maxZ, p.z());
		}
		float dy = maxY - minY;
		float dx = maxX - minX;
		if (Math.max(dx, dy) < 1.0E-3) {
			return null;
		}
		boolean vertical = dy >= dx;
		float length = vertical ? dy : dx;
		float cx = (minX + maxX) / 2;
		float cy = (minY + maxY) / 2;
		return new Shape(new float[0], vertical ? cx : minX, vertical ? minY : cy, vertical ? 0 : 1, vertical ? 1 : 0, length,
			(vertical ? dx : dy) / 2, (minZ + maxZ) / 2, false);
	}

	// ------------------------------------------------------------------ drawing

	/**
	 * The aura hanging round the whole weapon: a soft glow stretched along the blade's line, in the sprite's plane and across
	 * it (so it holds its shape from every side), breathing slowly; longer from Edge, where the crystal reaches past the point.
	 */
	private static void soft(PoseStack.Pose pose, VertexConsumer buffer, Shape s, Glow glow, float time) {
		int stage = glow.stage();
		float strength = glow.strength() * (glow.guarding() ? 1.3F : 1.0F);
		float breath = 0.85F + 0.15F * Mth.sin(time * 0.11F);
		float alpha = (stage >= AuraRules.EDGE ? 0.42F : stage >= AuraRules.FLOW ? 0.5F : 0.38F) * strength * breath;
		float len = s.length();
		float from = -0.12F * len;
		float to = len * (stage >= AuraRules.EDGE ? 1.45F : 1.12F);
		float half = s.spread() + (stage >= AuraRules.FLOW ? 0.2F : 0.14F);
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		float b0x = s.baseX() + ax * from;
		float b0y = s.baseY() + ay * from;
		float t0x = s.baseX() + ax * to;
		float t0y = s.baseY() + ay * to;
		float z = s.z();
		int color = glow.color();
		// In the sprite's plane, its middle over the blade...
		textured(buffer, pose, b0x - px * half, b0y - py * half, z, t0x - px * half, t0y - py * half, z, t0x + px * half, t0y + py * half, z,
			b0x + px * half, b0y + py * half, z, color, alpha);
		// ...and across it, for a weapon seen edge on.
		float deep = half * 0.8F;
		textured(buffer, pose, b0x, b0y, z - deep, t0x, t0y, z - deep, t0x, t0y, z + deep, b0x, b0y, z + deep, color, alpha * 0.8F);
	}

	/** A quad over the whole of its texture, seen from both sides, one colour and alpha. */
	private static void textured(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, int color, float alpha) {
		vertex(buffer, pose, x0, y0, z0, 0, 1, color, alpha);
		vertex(buffer, pose, x1, y1, z1, 0, 0, color, alpha);
		vertex(buffer, pose, x2, y2, z2, 1, 0, color, alpha);
		vertex(buffer, pose, x3, y3, z3, 1, 1, color, alpha);
		vertex(buffer, pose, x3, y3, z3, 1, 1, color, alpha);
		vertex(buffer, pose, x2, y2, z2, 1, 0, color, alpha);
		vertex(buffer, pose, x1, y1, z1, 0, 0, color, alpha);
		vertex(buffer, pose, x0, y0, z0, 0, 1, color, alpha);
	}

	/** The haze (Glow), and from Flow its wider rim, the light running along it and the ripples leaving it. */
	private static void haze(PoseStack.Pose pose, VertexConsumer buffer, Shape s, Glow glow, float time) {
		int stage = glow.stage();
		float strength = glow.strength() * (glow.guarding() ? 1.25F : 1.0F);
		int color = glow.color();
		int hot = mix(color, 0xFFFFFF, 0.45F);
		if (!s.flat()) {
			rod(pose, buffer, s, color, 0.55F * strength, stage >= AuraRules.FLOW ? 0.16F : 0.11F, time);
			return;
		}
		float rim = stage >= AuraRules.FLOW ? 0.12F : 0.09F;
		float base = (stage >= AuraRules.FLOW ? 0.8F : 0.7F) * strength;
		float[] e = s.edges();
		for (int i = 0; i < e.length; i += 6) {
			float ax = e[i];
			float ay = e[i + 1];
			float bx = e[i + 2];
			float by = e[i + 3];
			float nx = e[i + 4];
			float ny = e[i + 5];
			// Each span stretched a little along itself, so neighbouring spans meet round a corner.
			float dx = bx - ax;
			float dy = by - ay;
			float len = Mth.sqrt(dx * dx + dy * dy);
			if (len < 1.0E-5) {
				continue;
			}
			float ux = dx / len * rim * 0.5F;
			float uy = dy / len * rim * 0.5F;
			float a0x = ax - ux;
			float a0y = ay - uy;
			float b0x = bx + ux;
			float b0y = by + uy;
			float shimmerA = shimmer(s, a0x, a0y, time, stage);
			float shimmerB = shimmer(s, b0x, b0y, time, stage);
			strip(pose, buffer, a0x, a0y, b0x, b0y, nx, ny, 0, rim, s.z(), color, base * shimmerA, base * shimmerB, 0, 0);
			// A thin bright line on the silhouette itself.
			strip(pose, buffer, a0x, a0y, b0x, b0y, nx, ny, 0, rim * 0.28F, s.z(), hot, base * 0.8F * shimmerA, base * 0.8F * shimmerB, 0, 0);
			if (stage >= AuraRules.FLOW) {
				// Ripples: bands leaving the blade one after another, each fading as it goes.
				for (int r = 0; r < 2; r++) {
					float phase = ((time * 0.045F + r * 0.5F) % 1.0F);
					float from = rim * (0.6F + phase * 2.4F);
					float fade = (1 - phase) * (1 - phase) * 0.55F * strength;
					strip(pose, buffer, a0x, a0y, b0x, b0y, nx, ny, from, from + rim * 0.45F, s.z(), hot, fade, fade, fade * 0.2F, fade * 0.2F);
				}
			}
		}
	}

	/** How bright the rim is at a point: a slow shimmer at Glow, and from Flow light running along the blade from hilt to tip. */
	private static float shimmer(Shape s, float x, float y, float time, int stage) {
		float t = ((x - s.baseX()) * s.axisX() + (y - s.baseY()) * s.axisY()) / Math.max(1.0E-3F, s.length());
		if (stage >= AuraRules.FLOW) {
			return 0.62F + 0.38F * Mth.sin(t * 9.0F - time * 0.32F);
		}
		return 0.8F + 0.2F * Mth.sin(t * 5.0F - time * 0.12F);
	}

	/**
	 * A strip along the span a→b in the sprite's plane, from {@code from} to {@code to} out along (nx, ny), its alpha
	 * {@code aIn}/{@code bIn} at its inner edge and {@code aOut}/{@code bOut} at its outer.
	 */
	private static void strip(PoseStack.Pose pose, VertexConsumer buffer, float ax, float ay, float bx, float by, float nx, float ny, float from, float to,
			float z, int color, float aIn, float bIn, float aOut, float bOut) {
		if (aIn <= 0.004F && bIn <= 0.004F && aOut <= 0.004F && bOut <= 0.004F) {
			return;
		}
		vertex(buffer, pose, ax + nx * from, ay + ny * from, z, 0, 0, color, aIn);
		vertex(buffer, pose, bx + nx * from, by + ny * from, z, 1, 0, color, bIn);
		vertex(buffer, pose, bx + nx * to, by + ny * to, z, 1, 1, color, bOut);
		vertex(buffer, pose, ax + nx * to, ay + ny * to, z, 0, 1, color, aOut);
		// Seen from both sides: vanilla's glowing eyes leave out faces turned away.
		vertex(buffer, pose, ax + nx * to, ay + ny * to, z, 0, 1, color, aOut);
		vertex(buffer, pose, bx + nx * to, by + ny * to, z, 1, 1, color, bOut);
		vertex(buffer, pose, bx + nx * from, by + ny * from, z, 1, 0, color, bIn);
		vertex(buffer, pose, ax + nx * from, ay + ny * from, z, 0, 0, color, aIn);
	}

	/** A weapon of its own model: two crossed soft ribbons along its length, glowing at the middle and fading out to the sides. */
	private static void rod(PoseStack.Pose pose, VertexConsumer buffer, Shape s, int color, float alpha, float width, float time) {
		float bx = s.baseX();
		float by = s.baseY();
		float tx = bx + s.axisX() * s.length();
		float ty = by + s.axisY() * s.length();
		float px = -s.axisY();
		float py = s.axisX();
		float pulse = 0.85F + 0.15F * Mth.sin(time * 0.2F);
		float w = width + s.spread() * 0.5F;
		for (int side = -1; side <= 1; side += 2) {
			// In the item's plane...
			quadRaw(buffer, pose, bx, by, s.z(), tx, ty, s.z(), tx + px * w * side, ty + py * w * side, s.z(), bx + px * w * side, by + py * w * side, s.z(),
				color, alpha * pulse, alpha * pulse, 0, 0);
			// ...and across it.
			quadRaw(buffer, pose, bx, by, s.z(), tx, ty, s.z(), tx, ty, s.z() + w * side, bx, by, s.z() + w * side, color, alpha * pulse, alpha * pulse, 0, 0);
		}
	}

	/**
	 * Edge: a crystal blade of solid aura along the weapon, from just past the grip to beyond the point, four-faceted (a
	 * rhombus across), widest a quarter of the way up and drawn to a point, bright at its facets and ridge.
	 */
	private static void crystal(PoseStack.Pose pose, VertexConsumer buffer, Shape s, Glow glow, float time) {
		float strength = glow.strength();
		int color = mix(glow.color(), 0xFFFFFF, 0.22F);
		float alpha = 0.7F * strength * (0.9F + 0.1F * Mth.sin(time * 0.15F));
		float len = s.length();
		float start = s.flat() ? 0.3F * len : 0.15F * len;
		float end = len * 1.32F;
		float half = Math.max(0.08F, Math.min(0.17F, s.spread() * 0.4F + 0.05F));
		float thick = half * 0.42F;
		float[] at = {start, start + (end - start) * 0.28F, end};
		float[] widths = {half * 0.55F, half, 0};
		float ax = s.axisX();
		float ay = s.axisY();
		float px = -ay;
		float py = ax;
		for (int k = 0; k < 2; k++) {
			float c0x = s.baseX() + ax * at[k];
			float c0y = s.baseY() + ay * at[k];
			float c1x = s.baseX() + ax * at[k + 1];
			float c1y = s.baseY() + ay * at[k + 1];
			float w0 = widths[k];
			float w1 = widths[k + 1];
			float t0 = w0 / half * thick;
			float t1 = w1 / half * thick;
			float v0 = at[k] / end;
			float v1 = at[k + 1] / end;
			// The four facets: right-front, front-left, left-back, back-right.
			for (int f = 0; f < 4; f++) {
				float[] a = corner(f, c0x, c0y, s.z(), px, py, w0, t0);
				float[] b = corner(f + 1, c0x, c0y, s.z(), px, py, w0, t0);
				float[] c = corner(f + 1, c1x, c1y, s.z(), px, py, w1, t1);
				float[] d = corner(f, c1x, c1y, s.z(), px, py, w1, t1);
				float shadeF = f % 2 == 0 ? 1.0F : 0.82F;
				int col = mix(color, 0x000000, 1 - shadeF);
				vertexUv(buffer, pose, a, 0, 1 - v0, col, alpha);
				vertexUv(buffer, pose, b, 1, 1 - v0, col, alpha);
				vertexUv(buffer, pose, c, 1, 1 - v1, col, alpha);
				vertexUv(buffer, pose, d, 0, 1 - v1, col, alpha);
				// Its far facets too, through the clear crystal.
				vertexUv(buffer, pose, d, 0, 1 - v1, col, alpha * 0.7F);
				vertexUv(buffer, pose, c, 1, 1 - v1, col, alpha * 0.7F);
				vertexUv(buffer, pose, b, 1, 1 - v0, col, alpha * 0.7F);
				vertexUv(buffer, pose, a, 0, 1 - v0, col, alpha * 0.7F);
			}
		}
		// The bright ridge down the blade's middle, catching the light.
		int hot = mix(glow.color(), 0xFFFFFF, 0.7F);
		float sx0 = s.baseX() + ax * at[0];
		float sy0 = s.baseY() + ay * at[0];
		float sx1 = s.baseX() + ax * at[2];
		float sy1 = s.baseY() + ay * at[2];
		float r = half * 0.12F;
		quadRaw(buffer, pose, sx0 - px * r, sy0 - py * r, s.z() + thick * 0.6F, sx1, sy1, s.z(), sx1, sy1, s.z(), sx0 + px * r, sy0 + py * r, s.z() + thick * 0.6F,
			hot, alpha * 1.2F, alpha * 1.2F, alpha * 0.4F, alpha * 1.2F);
		quadRaw(buffer, pose, sx0 - px * r, sy0 - py * r, s.z() - thick * 0.6F, sx1, sy1, s.z(), sx1, sy1, s.z(), sx0 + px * r, sy0 + py * r, s.z() - thick * 0.6F,
			hot, alpha * 1.2F, alpha * 1.2F, alpha * 0.4F, alpha * 1.2F);
	}

	/** The corner {@code i} (0 right, 1 front, 2 left, 3 back, 4 right again) of the crystal's cross-section at (cx, cy). */
	private static float[] corner(int i, float cx, float cy, float z, float px, float py, float w, float t) {
		return switch (i % 4) {
			case 0 -> new float[] {cx + px * w, cy + py * w, z};
			case 1 -> new float[] {cx, cy, z + t};
			case 2 -> new float[] {cx - px * w, cy - py * w, z};
			default -> new float[] {cx, cy, z - t};
		};
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, float alpha) {
		int a = Mth.clamp(Math.round(alpha * 255), 0, 255);
		buffer.addVertex(pose, x, y, z).setColor((a << 24) | (color & 0xFFFFFF)).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL)
			.setNormal(pose, 0, 0, 1);
	}

	private static void vertexUv(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float u, float v, int color, float alpha) {
		vertex(buffer, pose, p[0], p[1], p[2], u, v, color, alpha);
	}

	private static void quadRaw(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, int color, float a0, float a1, float a2, float a3) {
		vertex(buffer, pose, x0, y0, z0, 0, 0, color, a0);
		vertex(buffer, pose, x1, y1, z1, 1, 0, color, a1);
		vertex(buffer, pose, x2, y2, z2, 1, 1, color, a2);
		vertex(buffer, pose, x3, y3, z3, 0, 1, color, a3);
		vertex(buffer, pose, x3, y3, z3, 0, 1, color, a3);
		vertex(buffer, pose, x2, y2, z2, 1, 1, color, a2);
		vertex(buffer, pose, x1, y1, z1, 1, 0, color, a1);
		vertex(buffer, pose, x0, y0, z0, 0, 0, color, a0);
	}

	static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}
}
