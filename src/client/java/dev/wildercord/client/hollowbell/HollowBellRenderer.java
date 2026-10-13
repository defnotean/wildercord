package dev.wildercord.client.hollowbell;

import com.mojang.blaze3d.vertex.*;
import dev.wildercord.Wildercord;
import dev.wildercord.world.hollowbell.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import static dev.wildercord.world.hollowbell.HollowBellRules.*;

public final class HollowBellRenderer extends MobRenderer<HollowBell, HollowBellRenderState, HollowBellModel> {
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/hollow_bell.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/hollow_bell_glow.png");
	private static final Identifier MARK = Wildercord.id("textures/entity/hollow_bell_mark.png");

	public HollowBellRenderer(EntityRendererProvider.Context context) {
		super(context, new HollowBellModel(context.bakeLayer(HollowBellModel.LAYER)), 1.3F);
		addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<HollowBellRenderState, HollowBellModel>(this) {
			@Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, HollowBellRenderState state, float yRot, float xRot) {
				if (!state.isInvisible && state.deathTime <= 0) {
					collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null, state.outlineColor);
				}
			}
		});
	}

	@Override public HollowBellRenderState createRenderState() { return new HollowBellRenderState(); }
	@Override public Identifier getTextureLocation(HollowBellRenderState state) { return TEXTURE; }
	@Override protected float getFlipDegrees() { return 0; }
	@Override public boolean shouldRender(HollowBell e, Frustum frustum, double x, double y, double z, float partial) {
		return super.shouldRender(e, frustum, x, y, z, partial) || frustum.isVisible(e.getBoundingBox().inflate(16, 2, 16));
	}

	@Override public void extractRenderState(HollowBell entity, HollowBellRenderState s, float tickDelta) {
		super.extractRenderState(entity, s, tickDelta);
		int move = entity.activeMove();
		s.move = move;
		s.elapsed = entity.elapsed(tickDelta);
		s.aim = entity.aimRadians();
		s.markX = entity.markX();
		s.markZ = entity.markZ();
		s.acrossX = entity.acrossX();
		s.second = entity.isSecondPhase();
		int total = tell(move);
		s.tellProgress = total <= 0 ? 0 : Mth.clamp(s.elapsed / total, 0, 1);
	}

	@Override public void submit(HollowBellRenderState s, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(s, matrices, collector, camera);
		if (s.isInvisible || s.deathTime > 0 || s.move == IDLE || s.elapsed > tell(s.move) + 5) return;
		float pulse = .55F + .35F * Mth.sin(s.ageInTicks * .35F);
		int verdigris = color(s.second ? 0.56F : 0.36F, s.second ? 0.96F : 0.86F, 0.78F, .78F * pulse);
		int amber = color(0.95F, 0.76F, 0.38F, .85F);
		collector.order(1).submitCustomGeometry(matrices, RenderTypes.entityTranslucentEmissive(MARK), (p, consumer) -> {
			if (s.move == TOLL) drawToll(consumer, p, s.tellProgress, verdigris, amber);
			else if (s.move == CLAPPER) drawClapper(consumer, p, s.markX, s.markZ, s.tellProgress, verdigris);
			else if (s.move == CHORD) drawChord(consumer, p, s.acrossX, verdigris, amber);
		});
	}

	private static void drawToll(VertexConsumer c, PoseStack.Pose p, float progress, int danger, int safe) {
		float in = (float) RING_INNER, out = (float) RING_OUTER, wave = in + progress * (out - in);
		ring(c, p, 0, 0, in, .16F, safe);
		ring(c, p, 0, 0, out, .16F, danger);
		ring(c, p, 0, 0, wave, .12F, danger);
	}

	private static void drawClapper(VertexConsumer c, PoseStack.Pose p, float x, float z, float progress, int color) {
		float r = (float) CLAPPER_RADIUS, inner = Math.max(.35F, (1 - progress) * r);
		ring(c, p, x, z, r, .15F, color);
		ring(c, p, x, z, inner, .1F, color);
		line(c, p, x - r * .75F, z, x + r * .75F, z, .1F, color);
		line(c, p, x, z - r * .75F, x, z + r * .75F, .1F, color);
	}

	private static void drawChord(VertexConsumer c, PoseStack.Pose p, boolean acrossX, int danger, int safe) {
		float span = (float) (ARENA - 1), lane = (float) SAFE_HALF_WIDTH;
		if (acrossX) {
			line(c, p, -span, -lane, span, -lane, .18F, safe);
			line(c, p, -span, lane, span, lane, .18F, safe);
			for (float z : new float[]{-8.5F, -5F, 5F, 8.5F}) for (float x : new float[]{-8F, -3F, 3F, 8F}) cross(c, p, x, z, 1.1F, danger);
		} else {
			line(c, p, -lane, -span, -lane, span, .18F, safe);
			line(c, p, lane, -span, lane, span, .18F, safe);
			for (float x : new float[]{-8.5F, -5F, 5F, 8.5F}) for (float z : new float[]{-8F, -3F, 3F, 8F}) cross(c, p, x, z, 1.1F, danger);
		}
	}

	private static void ring(VertexConsumer c, PoseStack.Pose p, float cx, float cz, float r, float w, int color) {
		for (int i = 0; i < 16; i++) {
			double a0 = i * Math.PI * 2 / 16, a1 = (i + 1) * Math.PI * 2 / 16;
			line(c, p, cx + (float) Math.sin(a0) * r, cz + (float) Math.cos(a0) * r, cx + (float) Math.sin(a1) * r, cz + (float) Math.cos(a1) * r, w, color);
		}
	}

	private static void cross(VertexConsumer c, PoseStack.Pose p, float x, float z, float r, int color) {
		line(c, p, x - r, z - r, x + r, z + r, .12F, color);
		line(c, p, x - r, z + r, x + r, z - r, .12F, color);
	}

	private static void line(VertexConsumer c, PoseStack.Pose p, float x0, float z0, float x1, float z1, float width, int color) {
		float dx = x1 - x0, dz = z1 - z0, len = Mth.sqrt(dx * dx + dz * dz);
		if (len < 1e-4F) return;
		float nx = -dz / len * width * .5F, nz = dx / len * width * .5F, y = .08F;
		vertex(c, p, x0 + nx, y, z0 + nz, 0, 0, color);
		vertex(c, p, x1 + nx, y, z1 + nz, 1, 0, color);
		vertex(c, p, x1 - nx, y, z1 - nz, 1, 1, color);
		vertex(c, p, x0 - nx, y, z0 - nz, 0, 1, color);
	}

	private static void vertex(VertexConsumer c, PoseStack.Pose p, float x, float y, float z, float u, float v, int color) {
		c.addVertex(p, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
	}

	private static int color(float r, float g, float b, float a) {
		return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
}
