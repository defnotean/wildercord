package dev.wildercord.client.familiar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.wildercord.Wildercord;
import dev.wildercord.client.fx.Glimmer;
import dev.wildercord.familiar.Wisp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A wisp: a small glowing orb of light. It is drawn as sprites that always face the viewer (see
 * {@code wisp_texture} in {@code tools/familiar_art.py}):
 * <ul>
 *   <li>a soft halo of light in its element's colour, added to the world (so it glows at night and
 *       tints what's behind it by day), breathing gently and swelling when it flares, with a slow
 *       four-point shimmer turning behind it;</li>
 *   <li>its body, a pale orb washed with its colour and never lit or shaded by the world, with two
 *       little eyes that turn the way it faces (it blinks now and then, and squints happily when it
 *       flares; from behind there are no eyes);</li>
 *   <li>a tail of light: beads of glow along the way it came, thinning and flickering, curling up
 *       behind it like a candle's flame when it hovers; sparks twinkle along it, and motes of its
 *       colour drift off it.</li>
 * </ul>
 * It bobs as it floats. A familiar grows a little and burns a little brighter with each level. Its
 * nameplate is small, without the dark plate, and dims unless you look at it.
 */
public class WispRenderer extends MobRenderer<Wisp, WispRenderState, WispModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("wisp"), "main");
	private static final Identifier BODY = Wildercord.id("textures/entity/wisp.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/wisp_glow.png");

	/** Light added to the world (source × alpha + destination), never hiding what's behind, like the spell glows. */
	private static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET)
		.withLocation(Wildercord.id("pipeline/wisp_glow"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();
	private static final OitPipelineSet GLOW_OIT = OitPipelineSet.builder("wildercord_wisp_glow",
		RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build();
	private static final RenderType GLOW_TYPE = RenderType.create("wildercord_wisp_glow",
		RenderSetup.builder(GLOW_PIPELINE).setOitPipelines(GLOW_OIT).withTexture("Sampler0", GLOW).sortOnUpload().createRenderSetup());
	private static final RenderType BODY_TYPE = RenderTypes.entityTranslucentEmissive(BODY);

	/** The middle of the orb above the entity's feet (its box is 0.45 tall). */
	private static final float MIDDLE = 0.24F;
	/** The body's half-width at level 1. */
	private static final float SIZE = 0.16F;
	private static final int TAIL = 9;
	private static final int FULL = LightCoordsUtil.FULL_BRIGHT;

	/** Where each wisp in view has been, a tick at a time: its tail. */
	private static final Map<Wisp, Trail> TRAILS = new WeakHashMap<>();

	public WispRenderer(EntityRendererProvider.Context context) {
		super(context, new WispModel(context.bakeLayer(LAYER)), 0.0F);
	}

	@Override
	public Identifier getTextureLocation(WispRenderState state) {
		return BODY;
	}

	@Override
	public WispRenderState createRenderState() {
		return new WispRenderState();
	}

	@Override
	public void extractRenderState(Wisp wisp, WispRenderState state, float partial) {
		super.extractRenderState(wisp, state, partial);
		state.color = wisp.color();
		state.level = wisp.familiarLevel();
		state.flare = Mth.clamp(1 - wisp.sinceFlare(partial) / 10F, 0, 1);
		state.seed = (wisp.getId() * 0.6180339F) % 1.0F * 40F;
		state.speed = (float) wisp.getDeltaMovement().length();
		state.looked = Minecraft.getInstance().crosshairPickEntity == wisp;
		// It never turns red or topples: it's light.
		state.hasRedOverlay = false;
		state.deathTime = 0;

		Trail trail = TRAILS.computeIfAbsent(wisp, w -> new Trail());
		if (trail.lastTick != wisp.tickCount) {
			trail.lastTick = wisp.tickCount;
			trail.push(wisp.getX(), wisp.getY(), wisp.getZ());
			motes(wisp, state, trail);
		}
		state.tail = trail.offsets(state.x, state.y, state.z, state);
	}

	/** No model of boxes: {@link #submit} draws the wisp itself. */
	@Override
	protected @Nullable RenderType getRenderType(WispRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
		return null;
	}

	@Override
	protected int getBlockLightLevel(Wisp wisp, BlockPos pos) {
		return 15;
	}

	@Override
	public void submit(WispRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) {
			return;
		}
		float t = state.ageInTicks + state.seed;
		float grow = 1.0F + 0.12F * (state.level - 1);
		float size = SIZE * grow;
		float bob = Mth.sin(t * 0.12F) * 0.05F;
		float flare = state.flare;
		float breath = 0.5F + 0.5F * Mth.sin(t * 0.21F);
		// A candle's unsteadiness on top of the slow breath.
		float flicker = 0.93F + 0.07F * Mth.sin(t * 1.37F) * Mth.sin(t * 0.73F + 1.1F);
		float bright = Math.min(1.25F, (0.72F + 0.12F * breath + 0.06F * (state.level - 1)) * flicker + 0.55F * flare);

		Vector3f right = camera.orientation.transform(new Vector3f(1, 0, 0));
		Vector3f up = camera.orientation.transform(new Vector3f(0, 1, 0));
		int color = state.color;
		int pale = mix(color, 0xFFFFFF, 0.55F);
		int hot = mix(color, 0xFFFFFF, 0.8F);
		float[] tail = state.tail;
		int frame = frame(state, camera, t);

		poseStack.pushPose();
		poseStack.translate(0.0F, MIDDLE + bob, 0.0F);
		collector.order(0).submitCustomGeometry(poseStack, GLOW_TYPE, (pose, buffer) -> {
			// The tail, farthest first: beads of light shrinking and flickering along the way it came.
			for (int i = tail.length / 3 - 1; i >= 0; i--) {
				float along = (i + 1F) / (TAIL + 1F);
				float fade = (float) Math.pow(1 - along, 1.35) * (0.7F + 0.3F * Mth.sin(t * 0.9F + i * 1.7F));
				float r = size * (0.95F - 0.75F * along);
				float x = tail[i * 3];
				float y = tail[i * 3 + 1];
				float z = tail[i * 3 + 2];
				quad(buffer, pose, right, up, x, y, z, r * 2.3F, 0, PUFF, scale(color, 0.7F * fade * bright));
				quad(buffer, pose, right, up, x, y, z, r * 0.9F, 0, PUFF, scale(hot, 0.55F * fade * bright));
			}
			// Sparks twinkling along it, each a few ticks, somewhere new each time.
			int beads = tail.length / 3;
			for (int k = 0; k < 3 && beads > 0; k++) {
				int slot = Mth.floor(t / 4F) * 3 + k;
				float life = (t / 4F) % 1F;
				int i = Math.floorMod(hash(slot), beads);
				float jx = (hashF(slot * 3 + 1) - 0.5F) * size * 1.2F;
				float jy = (hashF(slot * 3 + 2) - 0.5F) * size * 1.2F;
				float jz = (hashF(slot * 3 + 3) - 0.5F) * size * 1.2F;
				float glint = Mth.sin(life * Mth.PI) * (1 - (i + 1F) / (beads + 1F));
				quad(buffer, pose, right, up, tail[i * 3] + jx, tail[i * 3 + 1] + jy, tail[i * 3 + 2] + jz, size * 0.75F, t * 0.1F, SPARK,
					scale(hot, 0.9F * glint * bright));
			}
			// The halo, a brighter heart, and a slow shimmer turning behind it.
			float halo = size * (3.6F + 0.25F * breath + 1.3F * flare);
			quad(buffer, pose, right, up, 0, 0, 0, halo, 0, HALO, scale(color, 0.62F * bright));
			quad(buffer, pose, right, up, 0, 0, 0, size * 1.9F, 0, HALO, scale(pale, 0.75F * bright));
			quad(buffer, pose, right, up, 0, 0, 0, size * (2.6F + 1.5F * flare), t * 0.03F, SPARK, scale(hot, (0.35F + 0.6F * flare) * bright));
		});
		collector.order(1).submitCustomGeometry(poseStack, BODY_TYPE, (pose, buffer) -> {
			float half = size * (1.0F + 0.04F * breath + 0.12F * flare);
			quad(buffer, pose, right, up, 0, 0, 0, half, 0, bodyFrame(frame), 0xFF000000 | mix(pale, 0xFFFFFF, 0.25F * flare));
		});
		poseStack.popPose();
	}

	// ------------------------------------------------------------------ the nameplate

	/** Small, with no dark plate behind it; clear when you look at it, dimmer otherwise. */
	@Override
	protected void submitNameDisplay(WispRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		Component name = state.nameTag;
		if (name == null || state.nameTagAttachment == null) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		float alpha = state.looked || state.distanceToCameraSq < 25 ? 1.0F : 0.55F;
		poseStack.pushPose();
		poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.3, state.nameTagAttachment.z);
		poseStack.rotate(camera.orientation);
		poseStack.scale(0.017F, -0.017F, 0.017F);
		float x = -font.width(name) / 2.0F;
		collector.submitText(poseStack, x, 0, name.getVisualOrderText(), true, Font.DisplayMode.NORMAL, FULL,
			((int) (alpha * 255) << 24) | 0xFFFFFF, 0, 0);
		poseStack.popPose();
	}

	// ------------------------------------------------------------------ drawing

	/** Where in its texture each sprite lies: u0, v0, u1, v1. */
	private static final float[] HALO = {0, 0, 0.5F, 1};
	private static final float[] SPARK = {0.5F, 0, 0.75F, 0.5F};
	private static final float[] PUFF = {0.75F, 0, 1, 0.5F};
	private static final float[][] FRAMES = new float[8][];

	static {
		for (int i = 0; i < 8; i++) {
			FRAMES[i] = new float[] {i / 8F, 0, (i + 1) / 8F, 1};
		}
	}

	private static final int FAR_LEFT = 0;
	private static final int AHEAD = 2;
	private static final int BACK = 5;
	private static final int BLINK = 6;
	private static final int HAPPY = 7;

	private static float[] bodyFrame(int frame) {
		return FRAMES[frame];
	}

	/** Which of the body's frames to show: its eyes turned the way it faces, as seen from the camera. */
	private static int frame(WispRenderState state, CameraRenderState camera, float t) {
		float yaw = state.bodyRot * Mth.DEG_TO_RAD;
		float fx = -Mth.sin(yaw);
		float fz = Mth.cos(yaw);
		double cx = camera.pos.x - state.x;
		double cz = camera.pos.z - state.z;
		double dist = Math.sqrt(cx * cx + cz * cz);
		float front = dist < 1.0E-3 ? 1 : (float) ((fx * cx + fz * cz) / dist);
		if (front < -0.35F) {
			return BACK;
		}
		if (state.flare > 0.3F) {
			return HAPPY;
		}
		if ((int) t % 83 < 3) {
			return BLINK;
		}
		Vector3f right = camera.orientation.transform(new Vector3f(1, 0, 0));
		float side = fx * right.x + fz * right.z;
		if (side < -0.75F) {
			return FAR_LEFT;
		}
		if (side < -0.3F) {
			return FAR_LEFT + 1;
		}
		if (side <= 0.3F) {
			return AHEAD;
		}
		return side <= 0.75F ? AHEAD + 1 : AHEAD + 2;
	}

	/** A square sprite of half-width {@code half} at ({@code x}, {@code y}, {@code z}), facing the camera, turned {@code roll}. */
	private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vector3f right, Vector3f up, float x, float y, float z, float half,
			float roll, float[] uv, int argb) {
		if ((argb & 0xFFFFFF) == 0) {
			return;
		}
		float c = Mth.cos(roll) * half;
		float s = Mth.sin(roll) * half;
		// The sprite's own right and up, turned in the camera's plane.
		float rx = right.x * c + up.x * s;
		float ry = right.y * c + up.y * s;
		float rz = right.z * c + up.z * s;
		float ux = up.x * c - right.x * s;
		float uy = up.y * c - right.y * s;
		float uz = up.z * c - right.z * s;
		vertex(buffer, pose, x - rx - ux, y - ry - uy, z - rz - uz, uv[0], uv[3], argb);
		vertex(buffer, pose, x + rx - ux, y + ry - uy, z + rz - uz, uv[2], uv[3], argb);
		vertex(buffer, pose, x + rx + ux, y + ry + uy, z + rz + uz, uv[2], uv[1], argb);
		vertex(buffer, pose, x - rx + ux, y - ry + uy, z - rz + uz, uv[0], uv[1], argb);
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
		buffer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(pose, 0, 1, 0);
	}

	/** A light's colour at {@code k} strength: its channels scaled (light adds by colour), fully opaque. */
	private static int scale(int rgb, float k) {
		k = Mth.clamp(k, 0, 1);
		int r = Math.round(((rgb >> 16) & 0xFF) * k);
		int g = Math.round(((rgb >> 8) & 0xFF) * k);
		int b = Math.round((rgb & 0xFF) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
		int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
		int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private static int hash(int n) {
		n = (n ^ 61) ^ (n >>> 16);
		n *= 9;
		n ^= n >>> 4;
		n *= 0x27d4eb2d;
		n ^= n >>> 15;
		return n & 0x7FFFFFFF;
	}

	private static float hashF(int n) {
		return (hash(n) & 0xFFFF) / 65535F;
	}

	// ------------------------------------------------------------------ the tail

	/** Now and then a mote of its colour drifts off its tail (on the client, a tick at a time). */
	private static void motes(Wisp wisp, WispRenderState state, Trail trail) {
		if (!(wisp.level() instanceof ClientLevel level) || wisp.isInvisible()) {
			return;
		}
		RandomSource random = wisp.getRandom();
		if (random.nextInt(3) != 0) {
			return;
		}
		int back = Math.min(trail.count - 1, 2 + random.nextInt(4));
		int at = Math.floorMod(trail.head - back, Trail.LENGTH);
		Vec3 p = new Vec3(trail.xs[at], trail.ys[at] + MIDDLE, trail.zs[at]).add((random.nextDouble() - 0.5) * 0.18,
			(random.nextDouble() - 0.5) * 0.18, (random.nextDouble() - 0.5) * 0.18);
		int life = 16 + random.nextInt(16);
		int tint = random.nextInt(3) == 0 ? mix(state.color, 0xFFFFFF, 0.7F) : state.color;
		Minecraft.getInstance().particleEngine.add(Glimmer.mote(level, p, tint, 0.07F + random.nextFloat() * 0.05F, 0.75F, life,
			0, 0.006 + random.nextDouble() * 0.008, 0, 0.004F));
	}

	/** The last few places a wisp has been, a tick apart, newest at {@code head}. */
	private static final class Trail {
		static final int LENGTH = TAIL + 1;
		final double[] xs = new double[LENGTH];
		final double[] ys = new double[LENGTH];
		final double[] zs = new double[LENGTH];
		int head = -1;
		int count;
		int lastTick = Integer.MIN_VALUE;

		void push(double x, double y, double z) {
			head = (head + 1) % LENGTH;
			xs[head] = x;
			ys[head] = y;
			zs[head] = z;
			count = Math.min(LENGTH, count + 1);
		}

		/**
		 * The tail's beads relative to the wisp's middle: where it was, a tick apart; when it hovers
		 * (and those all bunch up), a little flame of a tail curling up behind it, swaying.
		 */
		float[] offsets(double x, double y, double z, WispRenderState state) {
			float t = state.ageInTicks + state.seed;
			float yaw = state.bodyRot * Mth.DEG_TO_RAD;
			float bx = Mth.sin(yaw);
			float bz = -Mth.cos(yaw);
			float idle = Mth.clamp(1 - state.speed * 9F, 0, 1);
			int n = Math.min(TAIL, Math.max(0, count - 1));
			float[] out = new float[TAIL * 3];
			for (int i = 0; i < TAIL; i++) {
				float px = 0;
				float py = 0;
				float pz = 0;
				if (i < n) {
					int at = Math.floorMod(head - 1 - i, LENGTH);
					px = (float) (xs[at] - x);
					py = (float) (ys[at] - y);
					pz = (float) (zs[at] - z);
				} else if (n > 0) {
					int at = Math.floorMod(head - n, LENGTH);
					px = (float) (xs[at] - x);
					py = (float) (ys[at] - y);
					pz = (float) (zs[at] - z);
				}
				float k = i + 1F;
				float sway = Mth.sin(t * 0.23F - i * 0.55F) * 0.018F * k;
				float lift = Mth.sin(t * 0.17F - i * 0.4F) * 0.008F * k;
				// Behind it and a little up, curling like a flame, swaying side to side.
				px += (bx * 0.055F * k - bz * sway) * idle + (-bz * sway * 0.4F) * (1 - idle);
				py += (0.022F * k + 0.004F * k * k + lift) * idle + lift * 0.5F * (1 - idle);
				pz += (bz * 0.055F * k + bx * sway) * idle + (bx * sway * 0.4F) * (1 - idle);
				out[i * 3] = px;
				out[i * 3 + 1] = py;
				out[i * 3 + 2] = pz;
			}
			return out;
		}
	}
}
