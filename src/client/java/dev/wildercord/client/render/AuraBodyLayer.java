package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.compat.ShaderCompat;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The body's aura, by stage, for everyone to see (see {@code tools/aura_art.py}, {@code body.png}, for its shapes):
 * <ul>
 * <li><b>Glow</b>: a faint shimmer, only a glint now and then at rest (the client's wisps and glints, {@code AuraFxClient}); in a
 * fight a soft haze round the silhouette;</li>
 * <li><b>Flow</b>: wisps rising from the shoulders and blade (also the client's), the haze a little stronger in a fight;</li>
 * <li><b>Edge</b>: a steady haze round the whole body, and the aura pooling faintly on the ground under it;</li>
 * <li><b>Form</b>: a flowing mantle, ribbons of light streaming up and back off the shoulders, waving, longer in a fight and
 * blown back as you run;</li>
 * <li><b>Sovereign</b>: a blazing corona, tongues of the aura's fire round the body, taller and brighter in a fight, embers
 * flying off it, and eyes that burn with it.</li>
 * </ul>
 * Calm when idle, flaring in a fight (a blow given or taken in the last few seconds), blazing up for a moment when an art or a
 * perfect guard surges it ({@link AuraFxRules#intensity}); faint while the aura is too low to coat a blow. Never drawn in your
 * own first-person view (the body isn't), where a whisper at the bottom edge of the screen stands for it ({@code AuraFxClient}).
 *
 * <p><b>Awakened</b> ({@code aura.Awakening}) it climbs past its stage into its awakened form: drawn in for a breath, then bursting
 * up into it ({@link AwakeningRules#form}): the eyes burn from Edge, a mantle streams off an Edge swordsman's shoulders and a low
 * corona licks round them, a Form swordsman's corona rises, a Sovereign's blazes past the head, and streamers of the aura's light
 * race up round the body from the feet, the pool under them wide and bright. In its last two seconds it gutters, flickering down.
 * <b>Spent</b>, all of it falls to a faint ash-grey haze.</p>
 *
 * <p>It's light: added to the world, left out of a shader pack's shadows, and under a pack drawn as vanilla's glowing eyes are
 * (see {@link ShaderCompat}). The player's "Body aura" setting calms it or switches it off.</p>
 */
public class AuraBodyLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/aura/body.png");

	/** Light added to the world (source × alpha + destination), never hiding what's behind, like the spells' and the wisps' glows. */
	public static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET)
		.withLocation(Wildercord.id("pipeline/aura_body_glow"))
		.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
		.withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
		.build();
	private static final OitPipelineSet GLOW_OIT = OitPipelineSet.builder("wildercord_aura_body_glow",
		RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build();
	private static final RenderType GLOW_TYPE = RenderType.create("wildercord_aura_body_glow",
		RenderSetup.builder(GLOW_PIPELINE).setOitPipelines(GLOW_OIT).withTexture("Sampler0", TEXTURE).sortOnUpload().createRenderSetup());
	/** Under a shader pack, which can't add light: vanilla's glowing eyes, laid over what's behind. */
	private static final RenderType PLAIN_TYPE = RenderTypes.eyes(TEXTURE);
	private static final int FULL = LightCoordsUtil.FULL_BRIGHT;

	// Where each shape lies in body.png (u0, v0, u1, v1): see tools/aura_art.py body().
	private static final float[] HAZE = {0, 0, 0.25F, 1};
	private static final float[][] FLAMES = new float[4][];
	private static final float[] RIBBON = {0.75F, 0, 1, 0.25F};
	private static final float[] EYE = {0.75F, 0.25F, 0.875F, 0.5F};
	private static final float[] POOL = {0.75F, 0.5F, 1, 1};

	static {
		for (int i = 0; i < 4; i++) {
			FLAMES[i] = new float[] {0.25F + i * 0.125F, 0, 0.25F + (i + 1) * 0.125F, 1};
		}
	}

	/**
	 * How a player's body aura looks this frame: its colour and stage, how strongly it shows ({@link AuraFxRules#intensity}), the
	 * time (ticks) for its motion, how fast they move (blocks a tick, level), whether they stand on the ground, how far into its
	 * awakened form it is ({@code awaken}, 0 to about 1.25: {@link AwakeningRules#form}, guttering already in) and whether they're
	 * spent.
	 */
	public record Body(int color, int stage, float intensity, float time, float speed, boolean grounded, float awaken, boolean spent) {
		/** Whether it burns in its awakened form now. */
		boolean awakened() {
			return awaken > 0.02F;
		}
	}

	public static final RenderStateDataKey<Body> BODY = RenderStateDataKey.create(() -> "wildercord:aura_body");

	public AuraBodyLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	// ------------------------------------------------------------------ extraction

	/** Carries a player's body aura into their render state as it's extracted. */
	public static void extract(Avatar avatar, AvatarRenderState state, float partial) {
		state.setData(BODY, null);
		if (!(avatar instanceof Player player) || player.isInvisible() || player.isSpectator() || MagicQuality.bodyAura == MagicQuality.BodyAura.OFF) {
			return;
		}
		AuraAttachments.Look look = Aura.look(player);
		if (look.stage() <= AuraRules.NONE) {
			return;
		}
		float time = player.level().getGameTime() + partial;
		float intensity = AuraFxClient.bodyIntensity(player, time);
		if (intensity <= 0.01F) {
			return;
		}
		double dx = player.getX() - player.xo;
		double dz = player.getZ() - player.zo;
		float awaken = AuraFxClient.awakenedForm(player, time);
		boolean spent = Awakening.spent(player);
		int color = spent ? mix(look.color(), 0x7A7080, 0.6F) : look.color();
		state.setData(BODY, new Body(color, look.stage(), intensity, time + player.getId() * 7.3F, (float) Math.sqrt(dx * dx + dz * dz),
			player.onGround(), awaken, spent));
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float yRot, float xRot) {
		Body body = state.getData(BODY);
		if (body == null || state.isInvisible || ShaderCompat.shadowPass()) {
			return;
		}
		// Two inks without a shader pack: the colour laid over the world in a deeper shade (so it reads against a bright sky by day),
		// and light added to it (so it glows by night). Under a pack, the laid-over colour alone, as vanilla's glowing eyes are drawn.
		boolean shaders = ShaderCompat.active();
		Ink[] inks = shaders ? new Ink[] {new Ink(true, 1.0F, false)} : new Ink[] {new Ink(true, 0.5F, true), new Ink(false, 0.85F, false)};
		if (body.stage() >= AuraRules.SOVEREIGN && !body.spent() || body.awakened()) {
			// The eyes (Sovereign's, and anyone's awakened), in the head's own frame so they turn with it.
			pose.pushPose();
			getParentModel().head.translateAndRotate(pose);
			for (Ink ink : inks) {
				nodes.order(2).submitCustomGeometry(pose, ink.plain() ? PLAIN_TYPE : GLOW_TYPE, (p, buffer) -> eyes(p, buffer, body, ink));
			}
			pose.popPose();
		}
		if (!upright(state)) {
			return;
		}
		pose.pushPose();
		// Back out of the model's flip and the body's own turn, to the feet, level with the world (as the afterimages do).
		pose.translate(0.0F, 1.501F, 0.0F);
		pose.scale(1 / 0.9375F, 1 / 0.9375F, 1 / 0.9375F);
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.rotateDegrees(Axis.YP, -(180.0F - state.bodyRot));
		Minecraft mc = Minecraft.getInstance();
		var camera = mc.gameRenderer.mainCamera();
		Vec3 cam = camera.position();
		Vector3f camRight = camera.leftVector().negate(new Vector3f());
		Vector3f camUp = new Vector3f(camera.upVector());
		float scale = state.scale <= 0 ? 1 : state.scale;
		Vector3f toCamera = new Vector3f((float) ((cam.x - state.x) / scale), (float) ((cam.y - state.y) / scale), (float) ((cam.z - state.z) / scale));
		float yaw = state.bodyRot * Mth.DEG_TO_RAD;
		Frame frame = new Frame(-Mth.sin(yaw), Mth.cos(yaw), camRight, camUp, toCamera);
		for (Ink ink : inks) {
			nodes.order(2).submitCustomGeometry(pose, ink.plain() ? PLAIN_TYPE : GLOW_TYPE, (p, buffer) -> body(p, buffer, body, frame, ink));
		}
		pose.popPose();
	}

	/**
	 * How the body's aura is put on the world: laid over it ({@code plain}, its strength in its alpha) or added to it as light, at
	 * {@code gain} of its strength, in a {@code deep}er shade of its colour or its own.
	 */
	private record Ink(boolean plain, float gain, boolean deep) {
		int of(int rgb, float k) {
			return glow(plain, scale(deep ? deepen(rgb) : rgb, k * gain));
		}
	}

	/** A colour's deeper shade: pushed toward its pure hue and darkened a little, so it still shows laid over a pale sky. */
	static int deepen(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		if (max - min < 8) {
			return mix(rgb, 0x000000, 0.25F);
		}
		int pure = ((r - min) * max / (max - min) << 16) | ((g - min) * max / (max - min) << 8) | (b - min) * max / (max - min);
		return mix(mix(rgb, pure, 0.7F), 0x000000, 0.22F);
	}

	/** Only upright (not swimming, gliding, sleeping or in a spin), where the body's turn is all there is. */
	private static boolean upright(AvatarRenderState state) {
		return !state.isAutoSpinAttack && !state.isFallFlying && !state.hasPose(Pose.SWIMMING) && !state.hasPose(Pose.SLEEPING) && state.deathTime <= 0
			&& !state.isUpsideDown;
	}

	/** The body's facing (level forward), the camera's right and up, and where the camera is from the feet. */
	private record Frame(float fx, float fz, Vector3f camRight, Vector3f camUp, Vector3f toCamera) {
		float rx() {
			return -fz;
		}

		float rz() {
			return fx;
		}
	}

	private static void body(PoseStack.Pose pose, VertexConsumer buffer, Body b, Frame f, Ink ink) {
		int stage = b.stage();
		float k = b.intensity();
		float t = b.time();
		float breath = 0.88F + 0.12F * Mth.sin(t * 0.1F);
		int color = b.color();
		int hot = mix(color, 0xFFFFFF, 0.55F);
		// The haze round the silhouette: soft glows standing in the body (the body itself hides their middles, so only a halo shows).
		// The haze stops growing past a full flare (a surge or an awakening blazes in the flames, not in a glare over the face).
		float kh = Math.min(1.15F, k);
		float haze = switch (stage) {
			case AuraRules.GLOW -> Math.max(0, kh - 0.4F) * 0.3F;
			case AuraRules.FLOW -> Math.max(0, kh - 0.3F) * 0.34F;
			case AuraRules.EDGE -> 0.13F + 0.22F * kh;
			case AuraRules.FORM -> 0.14F + 0.24F * kh;
			default -> 0.15F + 0.26F * kh;
		} * breath;
		if (ink.deep()) {
			// Laid over a pale sky the haze's own colour must carry it (added light alone turns white there).
			haze *= 1.5F;
		}
		if (haze > 0.01F) {
			float grow = 1 + 0.15F * kh;
			billboard(buffer, pose, f, 0, 0.5F, 0, 0.5F * grow, 0.62F * grow, HAZE, ink.of(color, haze));
			billboard(buffer, pose, f, 0, 1.08F, 0, 0.64F * grow, 0.72F * grow, HAZE, ink.of(color, haze * 1.1F));
			// Round the head softer where the eyes burn (from Sovereign, or awakened), so they show through it.
			boolean eyes = stage >= AuraRules.SOVEREIGN || b.awakened();
			billboard(buffer, pose, f, 0, 1.6F, 0, 0.44F * grow, 0.46F * grow, HAZE, ink.of(eyes ? color : hot, haze * (eyes ? 0.5F : 0.8F)));
		}
		if (b.spent()) {
			// Spent: only the faint haze above, in ash.
			return;
		}
		float w = Math.min(1.25F, b.awaken());
		if (stage >= AuraRules.EDGE && b.grounded()) {
			// The aura pooling on the ground under the feet (wide and bright while awakened).
			float pool = (0.09F + 0.09F * k + 0.03F * (stage - AuraRules.EDGE) + 0.08F * w) * breath;
			float r = 0.8F + 0.25F * k + 0.1F * (stage - AuraRules.EDGE) + 0.55F * w;
			ground(buffer, pose, r, POOL, ink.of(color, pool));
		}
		// Awakened, it climbs past its stage: an Edge swordsman takes on a mantle, a Form swordsman a corona.
		float mantle = stage >= AuraRules.FORM ? (stage >= AuraRules.SOVEREIGN ? 0.7F : 1.0F) : stage >= AuraRules.EDGE ? 0.8F * Math.min(1, w) : 0;
		if (mantle > 0.02F) {
			mantle(buffer, pose, b, f, ink, mantle);
		}
		float corona = stage >= AuraRules.SOVEREIGN ? 1.0F : stage >= AuraRules.FORM ? 0.85F * Math.min(1, w) : stage >= AuraRules.EDGE ? 0.55F * Math.min(1, w) : 0;
		if (corona > 0.02F) {
			corona(buffer, pose, b, f, ink, corona);
		}
		if (b.awakened()) {
			updraft(buffer, pose, b, f, ink, w);
		}
	}

	/**
	 * Awakened: streamers of the aura's light racing up round the body from the feet past the head, each fading in low and out high,
	 * a hot white heart down each, turning slowly round as they rise.
	 */
	private static void updraft(VertexConsumer buffer, PoseStack.Pose pose, Body b, Frame f, Ink ink, float w) {
		float t = b.time();
		int color = b.color();
		int hot = mix(color, 0xFFFFFF, 0.65F);
		int count = 12;
		float top = 2.5F + 0.15F * (b.stage() - AuraRules.EDGE);
		for (int i = 0; i < count; i++) {
			float h1 = hash(i * 5 + 11);
			float h2 = hash(i * 5 + 12);
			// Each rises at its own pace, starting over at the feet.
			float phase = (t * (0.045F + 0.03F * h2) + h1) % 1.0F;
			float a = Mth.TWO_PI * i / count + t * 0.02F + h1;
			float out = 0.42F + 0.14F * h2 + 0.1F * phase;
			float x = Mth.cos(a) * out;
			float z = Mth.sin(a) * out * 0.8F;
			float wx = f.rx() * x + f.fx() * z;
			float wz = f.rz() * x + f.fz() * z;
			float y = phase * top - 0.3F;
			float length = (0.55F + 0.45F * h1) * (0.7F + 0.3F * Math.min(1, w));
			float fade = Mth.sin(Mth.PI * phase);
			float strength = (0.3F + 0.25F * Math.min(1.2F, w)) * fade * fade;
			int frame = Math.floorMod((int) (t * 0.6F + i * 1.3F), 4);
			flame(buffer, pose, f, wx, Math.max(0.0F, y), wz, 0.13F, length, FLAMES[frame], ink.of(color, strength));
			flame(buffer, pose, f, wx, Math.max(0.0F, y) + 0.05F, wz, 0.06F, length * 0.8F, FLAMES[(frame + 1) % 4], ink.of(hot, strength * 0.9F));
		}
	}

	/**
	 * Form: a mantle of light off the shoulders and the back, broad ribbons overlapping into a cape that streams up and back,
	 * rippling as it goes, longer and brighter in a fight and blown back as you run.
	 */
	private static void mantle(VertexConsumer buffer, PoseStack.Pose pose, Body b, Frame f, Ink ink, float share) {
		float k = b.intensity();
		float t = b.time();
		float lean = Mth.clamp(b.speed() * 2.5F, 0, 0.6F);
		int color = b.color();
		int hot = mix(color, 0xFFFFFF, 0.45F);
		float length = (0.6F + 0.5F * k) * share;
		// Roots across the shoulders and the upper back: side, height, how far back, how far out it fans.
		float[][] roots = {{-0.32F, 1.34F, 0.04F, -0.26F}, {-0.21F, 1.4F, 0.08F, -0.15F}, {-0.08F, 1.38F, 0.13F, -0.05F}, {0.08F, 1.38F, 0.13F, 0.05F},
			{0.21F, 1.4F, 0.08F, 0.15F}, {0.32F, 1.34F, 0.04F, 0.26F}, {0.0F, 1.2F, 0.16F, 0.0F}};
		int n = 10;
		Vector3f[] points = new Vector3f[n + 1];
		float[] widths = new float[n + 1];
		int[] colors = new int[n + 1];
		for (int j = 0; j < roots.length; j++) {
			float[] root = roots[j];
			float seed = j * 1.91F;
			float ribbonLength = length * (j == 6 ? 1.25F : 0.9F + 0.1F * hash(j));
			for (int i = 0; i <= n; i++) {
				float s = i / (float) n;
				float ripple = Mth.sin(t * 0.26F - s * 5.0F + seed);
				float back = root[2] + ribbonLength * 0.62F * (float) Math.pow(s, 1.3) + lean * s + 0.1F * s * ripple;
				float side = root[0] + root[3] * s + 0.05F * s * Mth.sin(t * 0.19F - s * 4F + seed * 1.7F);
				float up = root[1] + ribbonLength * 0.9F * s - lean * 0.35F * s * s;
				points[i] = new Vector3f(f.rx() * side - f.fx() * back, up, f.rz() * side - f.fz() * back);
				widths[i] = (0.16F + 0.07F * k) * (float) Math.pow(1 - s, 0.7) + 0.01F;
				float fade = (float) (1 - Math.pow(s, 1.6)) * Math.min(1, s / 0.1F + 0.1F);
				colors[i] = ink.of(i == 0 ? hot : color, (0.24F + 0.24F * Math.min(1.3F, k)) * fade * (0.85F + 0.15F * ripple));
			}
			strip(buffer, pose, f, points, widths, colors, RIBBON);
		}
	}

	/**
	 * Sovereign: a corona, tongues of the aura's fire standing round the body from the feet, flickering and licking up past the
	 * shoulders in a fight (low and calm at rest), a hotter heart inside each, and a crown of them behind the head.
	 */
	private static void corona(VertexConsumer buffer, PoseStack.Pose pose, Body b, Frame f, Ink ink, float share) {
		float k = b.intensity();
		float t = b.time();
		int color = b.color();
		int hot = mix(color, 0xFFFFFF, b.awakened() ? 0.68F : 0.6F);
		// How bright it burns (held where a Sovereign's corona always is, so the burning eyes still show through it), and awakened how
		// much taller it stands (a Sovereign's past the head; an Edge or Form swordsman's borrowed corona lower).
		float blaze = Math.min(1.4F, k) * share;
		float tall = b.awakened() ? 1 + 0.3F * Math.min(1, b.awaken()) : 1;
		int count = 16;
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + t * 0.01F;
			float h1 = hash(i * 3 + 1);
			float h2 = hash(i * 3 + 2);
			float side = Mth.cos(a) * 0.46F;
			float fore = Mth.sin(a) * 0.34F;
			float x = f.rx() * side + f.fx() * fore;
			float z = f.rz() * side + f.fz() * fore;
			// Standing at every height up the body, so the whole of it burns, not only the legs.
			float base = 0.02F + 0.95F * h1 * h1;
			float flicker = 0.78F + 0.22F * Mth.sin(t * 0.9F + i * 2.1F) * Mth.sin(t * 0.37F + i);
			// Calm, they lick round the legs; in a fight they climb past the shoulders; surging, over the head.
			float height = (0.7F + 0.55F * h2) * (0.35F + 0.95F * blaze) * flicker * tall;
			float width = (0.34F + 0.1F * hash(i * 3 + 3)) * (0.85F + 0.2F * blaze);
			int frame = Math.floorMod((int) (t * 0.45F + i * 1.7F), 4);
			// The tongues standing in front of the face burn lower, so the eyes read through the fire.
			float face = fore > 0.12F && base + height * 0.5F > 1.1F ? 0.45F : 1.0F;
			float strength = Math.min(1, share * 1.3F) * face;
			flame(buffer, pose, f, x, base, z, width, height, FLAMES[frame], ink.of(color, (0.26F + 0.26F * blaze) * strength));
			flame(buffer, pose, f, x, base, z, width * 0.55F, height * 0.72F, FLAMES[(frame + 2) % 4], ink.of(hot, (0.24F + 0.24F * blaze) * strength));
		}
		// A crown of three behind the head, taller.
		for (int i = 0; i < 3; i++) {
			float side = (i - 1) * 0.18F;
			float x = f.rx() * side - f.fx() * 0.16F;
			float z = f.rz() * side - f.fz() * 0.16F;
			float flicker = 0.8F + 0.2F * Mth.sin(t * 0.7F + i * 2.6F);
			float height = (0.7F + 0.25F * (i == 1 ? 1 : 0)) * (0.4F + 0.85F * blaze) * flicker * tall;
			int frame = Math.floorMod((int) (t * 0.4F + i * 2.3F), 4);
			flame(buffer, pose, f, x, 1.2F, z, 0.4F, height, FLAMES[frame], ink.of(color, 0.24F + 0.26F * blaze));
		}
	}

	/** Sovereign's burning eyes, on the face (the head's frame: pixels over 16, y down, the face toward -z). */
	private static void eyes(PoseStack.Pose pose, VertexConsumer buffer, Body b, Ink ink) {
		float k = Math.min(1.3F, b.intensity());
		float pulse = 0.85F + 0.15F * Mth.sin(b.time() * 0.23F);
		float w = Math.min(1.0F, b.awaken());
		int hot = mix(b.color(), 0xFFFFFF, 0.6F + 0.25F * w);
		float z = -4.15F / 16F;
		float y = -3.5F / 16F;
		// Awakened, below Sovereign they open as the form rises; at any stage they burn brighter and their glow spreads.
		float open = b.stage() >= AuraRules.SOVEREIGN ? 1.0F : w;
		float strength = (0.55F + 0.45F * k) * pulse * open * (1 + 0.25F * w);
		float spread = 1 + 0.45F * w;
		for (int side = -1; side <= 1; side += 2) {
			float x = side * 2.0F / 16F;
			face(buffer, pose, x, y, z, 0.07F, 0.055F, EYE, ink.of(hot, strength));
			face(buffer, pose, x, y, z - 0.002F, 0.16F * spread, 0.1F * spread, EYE, ink.of(b.color(), strength * 0.55F));
			if (w > 0.02F) {
				// A wisp of light trailing back off each eye.
				face(buffer, pose, x + side * 0.1F * w, y - 0.01F, z + 0.02F, 0.12F * w, 0.035F, EYE, ink.of(b.color(), strength * 0.35F));
			}
		}
	}

	// ------------------------------------------------------------------ shapes

	/** A sprite facing the camera (its right and up), {@code hw} by {@code hh} half-extents, centred at ({@code x}, {@code y}, {@code z}). */
	private static void billboard(VertexConsumer buffer, PoseStack.Pose pose, Frame f, float x, float y, float z, float hw, float hh, float[] uv, int argb) {
		Vector3f r = f.camRight();
		Vector3f u = f.camUp();
		quad(buffer, pose, x - r.x * hw - u.x * hh, y - r.y * hw - u.y * hh, z - r.z * hw - u.z * hh,
			x + r.x * hw - u.x * hh, y + r.y * hw - u.y * hh, z + r.z * hw - u.z * hh,
			x + r.x * hw + u.x * hh, y + r.y * hw + u.y * hh, z + r.z * hw + u.z * hh,
			x - r.x * hw + u.x * hh, y - r.y * hw + u.y * hh, z - r.z * hw + u.z * hh, uv, argb, argb);
	}

	/** A tongue of fire standing at ({@code x}, {@code y}, {@code z}), turned about the upright to face the camera. */
	private static void flame(VertexConsumer buffer, PoseStack.Pose pose, Frame f, float x, float y, float z, float width, float height, float[] uv, int argb) {
		float wx = f.toCamera().z - z;
		float wz = -(f.toCamera().x - x);
		float len = Mth.sqrt(wx * wx + wz * wz);
		if (len < 1.0E-4F) {
			wx = f.camRight().x;
			wz = f.camRight().z;
			len = Mth.sqrt(wx * wx + wz * wz);
			if (len < 1.0E-4F) {
				return;
			}
		}
		float hx = wx / len * width / 2;
		float hz = wz / len * width / 2;
		quad(buffer, pose, x - hx, y, z - hz, x + hx, y, z + hz, x + hx, y + height, z + hz, x - hx, y + height, z - hz, uv, argb, argb);
	}

	/** A glow lying on the ground at the feet, {@code r} out. */
	private static void ground(VertexConsumer buffer, PoseStack.Pose pose, float r, float[] uv, int argb) {
		float y = 0.03F;
		quad(buffer, pose, -r, y, -r, r, y, -r, r, y, r, -r, y, r, uv, argb, argb);
	}

	/** A sprite on the face, facing out of it (-z), {@code hw} by {@code hh}. */
	private static void face(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float hw, float hh, float[] uv, int argb) {
		quad(buffer, pose, x - hw, y + hh, z, x + hw, y + hh, z, x + hw, y - hh, z, x - hw, y - hh, z, uv, argb, argb);
	}

	/**
	 * A ribbon through {@code points}, each {@code widths} across and in {@code colors}, turned about its own line to face the
	 * camera as squarely as it can; the sprite's length runs along it (u) and its soft band across it (v).
	 */
	private static void strip(VertexConsumer buffer, PoseStack.Pose pose, Frame f, Vector3f[] points, float[] widths, int[] colors, float[] uv) {
		Vector3f side0 = side(points, 0, f);
		for (int i = 0; i < points.length - 1; i++) {
			Vector3f a = points[i];
			Vector3f b = points[i + 1];
			Vector3f side1 = side(points, i + 1, f);
			float wa = widths[i] / 2;
			float wb = widths[i + 1] / 2;
			float u0 = uv[0] + (uv[2] - uv[0]) * i / (points.length - 1);
			float u1 = uv[0] + (uv[2] - uv[0]) * (i + 1) / (points.length - 1);
			float[] piece = {u0, uv[1], u1, uv[3]};
			quad(buffer, pose, a.x - side0.x * wa, a.y - side0.y * wa, a.z - side0.z * wa, b.x - side1.x * wb, b.y - side1.y * wb, b.z - side1.z * wb,
				b.x + side1.x * wb, b.y + side1.y * wb, b.z + side1.z * wb, a.x + side0.x * wa, a.y + side0.y * wa, a.z + side0.z * wa, piece,
				colors[i], colors[i + 1]);
			side0 = side1;
		}
	}

	/** The way across a ribbon at point {@code i}: square to its line and to the camera. */
	private static Vector3f side(Vector3f[] points, int i, Frame f) {
		Vector3f a = points[Math.max(0, i - 1)];
		Vector3f b = points[Math.min(points.length - 1, i + 1)];
		Vector3f along = new Vector3f(b).sub(a);
		Vector3f view = new Vector3f(f.toCamera()).sub(points[i]);
		Vector3f side = along.cross(view, new Vector3f());
		if (side.lengthSquared() < 1.0E-8F) {
			side.set(f.camRight());
		}
		return side.normalize();
	}

	/**
	 * A quad from both sides. Corners 0 and 3 take {@code argb0}, 1 and 2 {@code argb1}; the texture runs u from corner 0 to 1 and v
	 * from the bottom (corners 0, 1) to the top (2, 3), so a sprite stands upright and a ribbon's length runs along u.
	 */
	private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2,
			float z2, float x3, float y3, float z3, float[] uv, int argb0, int argb1) {
		if ((argb0 & 0xFFFFFF) == 0 && (argb1 & 0xFFFFFF) == 0) {
			return;
		}
		float[][] c = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
		float[][] t = {{uv[0], uv[3]}, {uv[2], uv[3]}, {uv[2], uv[1]}, {uv[0], uv[1]}};
		int[] col = {argb0, argb1, argb1, argb0};
		for (int i = 0; i < 4; i++) {
			vertex(buffer, pose, c[i], t[i], col[i]);
		}
		for (int i = 3; i >= 0; i--) {
			vertex(buffer, pose, c[i], t[i], col[i]);
		}
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float[] uv, int argb) {
		buffer.addVertex(pose, p[0], p[1], p[2]).setColor(argb).setUv(uv[0], uv[1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL)
			.setNormal(pose, 0, 1, 0);
	}

	// ------------------------------------------------------------------ colour

	/** A light's colour at {@code k} strength: its channels scaled (light adds by colour), fully opaque. */
	static int scale(int rgb, float k) {
		k = Mth.clamp(k, 0, 1);
		int r = Math.round(((rgb >> 16) & 0xFF) * k);
		int g = Math.round(((rgb >> 8) & 0xFF) * k);
		int b = Math.round((rgb & 0xFF) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	/** Added light as a colour laid over what's behind (for {@link #PLAIN_TYPE}): its strength in its alpha. */
	static int glow(boolean plain, int argb) {
		if (!plain) {
			return argb;
		}
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		int a = Math.max(r, Math.max(g, b));
		if (a == 0) {
			return 0;
		}
		return (a << 24) | (r * 255 / a << 16) | (g * 255 / a << 8) | b * 255 / a;
	}

	static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}

	/** A number from 0 to 1, the same every time for the same {@code i}. */
	private static float hash(int i) {
		int h = i * 0x27D4EB2D;
		h ^= h >>> 15;
		h *= 0x165667B1;
		h ^= h >>> 13;
		return (h & 0xFFFF) / 65535F;
	}
}
