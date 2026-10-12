package dev.wildercord.client.render;

import dev.wildercord.cast.TideScribeRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.EnumSet;

/**
 * The Tide Scribe: a drowned sorcerer from the waist up, hooded, in a sodden robe hung with kelp, a
 * quill in one hand; below the waist, six tendrils of ink in three joints each. An open scroll floats
 * before it, its writing glowing. Swimming in the flood it leans forward and its tendrils stream out
 * behind; casting, it raises the scroll and its quill scribbles; stranded in ice it slumps and its
 * tendrils hang; between phases it throws its arms wide and the tendrils writhe; dying, it sinks with
 * its head bowed and the scroll drifts down.
 *
 * <p>Laid out on a 128x64 skin (see {@code tide_scribe_texture} in {@code tools/dungeon_assets.py}):</p>
 * <pre>
 *   head (0,0) 8x8x8        hood (32,0) 9x9x9       torso (0,18) 8x12x4     collar (24,18) 10x3x6
 *   arm (56,18) 4x12x4      tendril (0,36) 3x7x3    tendril mid (12,36) 2x7x2   tendril tip (20,36) 1x6x1
 *   scroll (32,36) 12x8     scroll rod (58,36) 1x10x1   quill (64,36) 1x7x1
 * </pre>
 */
public class TideScribeModel extends EntityModel<DungeonBossRenderState> {
	private static final int TENDRILS = 6;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart scroll;
	private final ModelPart[] tendrils = new ModelPart[TENDRILS];
	private final ModelPart[] mids = new ModelPart[TENDRILS];
	private final ModelPart[] tips = new ModelPart[TENDRILS];

	public TideScribeModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.scroll = root.getChild("scroll");
		for (int i = 0; i < TENDRILS; i++) {
			tendrils[i] = body.getChild("tendril_" + i);
			mids[i] = tendrils[i].getChild("mid");
			tips[i] = mids[i].getChild("tip");
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 18).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F)
				.texOffs(24, 18).addBox(-5.0F, -12.5F, -3.0F, 10.0F, 3.0F, 6.0F),
			PartPose.offset(0.0F, 8.0F, 0.0F));
		body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
				.texOffs(32, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 9.0F, 9.0F),
			PartPose.offset(0.0F, -12.0F, 0.0F));
		PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 18).addBox(-3.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-5.0F, -11.0F, 0.0F));
		rightArm.addOrReplaceChild("quill", CubeListBuilder.create().texOffs(64, 36).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(-1.0F, 10.5F, -1.0F, -0.6F, 0.0F, 0.0F));
		body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(56, 18).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(5.0F, -11.0F, 0.0F));
		// Six tendrils of ink, round the hem, each in three joints.
		for (int i = 0; i < TENDRILS; i++) {
			float a = Mth.TWO_PI * i / TENDRILS;
			PartDefinition tendril = body.addOrReplaceChild("tendril_" + i, CubeListBuilder.create().texOffs(0, 36).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F),
				PartPose.offset(Mth.sin(a) * 2.5F, -0.5F, Mth.cos(a) * 1.2F));
			PartDefinition mid = tendril.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(12, 36).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				PartPose.offset(0.0F, 6.5F, 0.0F));
			mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(20, 36).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
				PartPose.offset(0.0F, 6.5F, 0.0F));
		}
		// The open scroll, floating before it between its two rods.
		PartDefinition scroll = root.addOrReplaceChild("scroll", CubeListBuilder.create()
				.texOffs(32, 36).addBox(-6.0F, -4.0F, 0.0F, 12.0F, 8.0F, 0.0F, EnumSet.of(Direction.NORTH, Direction.SOUTH)),
			PartPose.offset(0.0F, 2.0F, -9.0F));
		scroll.addOrReplaceChild("left_rod", CubeListBuilder.create().texOffs(58, 36).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F),
			PartPose.offset(6.5F, 0.0F, 0.0F));
		scroll.addOrReplaceChild("right_rod", CubeListBuilder.create().texOffs(58, 36).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F),
			PartPose.offset(-6.5F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(DungeonBossRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float sh = state.shifting;
		float swim = state.guarded;
		float slump = state.exposed;
		float dying = Math.min(1, state.deathTime / 40.0F);

		// It hovers, bobbing; swimming it leans into the water; stranded or dying, it slumps.
		body.y = 8.0F + Mth.sin(t * 0.08F) * 0.8F + dying * 6.0F;
		body.xRot = 0.45F * swim + 0.3F * slump + 0.35F * dying - 0.1F * sh;
		body.zRot = Mth.sin(t * 0.05F) * 0.05F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - sh);
		head.xRot = Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD * (1 - sh) - 0.6F * sh + 0.5F * (slump + dying) - 0.3F * swim;

		// Arms: hanging; casting, the left lifts the scroll and the right scribbles; thrown wide between phases.
		float scribble = Mth.sin(t * 1.3F) * 0.15F * c;
		rightArm.xRot = Mth.lerp(c, -0.2F + Mth.sin(t * 0.06F) * 0.05F, -1.3F + scribble);
		rightArm.yRot = Mth.lerp(c, 0, -0.3F + Mth.cos(t * 1.1F) * 0.12F * c);
		rightArm.zRot = Mth.lerp(sh, 0.1F, 1.8F);
		leftArm.xRot = Mth.lerp(c, -0.2F + Mth.sin(t * 0.06F + 1) * 0.05F, -1.5F);
		leftArm.yRot = 0;
		leftArm.zRot = Mth.lerp(sh, -0.1F, -1.8F);
		rightArm.xRot = Mth.lerp(slump + dying * 0.8F, rightArm.xRot, 0.1F);
		leftArm.xRot = Mth.lerp(slump + dying * 0.8F, leftArm.xRot, 0.1F);

		// Tendrils: swaying in a slow wave; streaming out behind while it swims; writhing between phases; limp when stranded.
		float speed = 0.1F + 0.25F * sh + 0.1F * swim;
		float reach = (0.25F + 0.35F * sh) * (1 - 0.7F * slump) * (1 - dying);
		for (int i = 0; i < TENDRILS; i++) {
			float a = Mth.TWO_PI * i / TENDRILS;
			float wave = t * speed + i * 1.1F;
			float outward = 0.35F + 0.25F * sh;
			tendrils[i].xRot = Mth.cos(a) * outward + Mth.sin(wave) * reach + 0.9F * swim;
			tendrils[i].zRot = -Mth.sin(a) * outward + Mth.cos(wave) * reach * 0.6F;
			mids[i].xRot = Mth.sin(wave - 0.9F) * reach * 1.4F + 0.2F * swim;
			tips[i].xRot = Mth.sin(wave - 1.8F) * reach * 1.8F;
		}

		// The scroll floats before it; casting, it rises and spreads; dying, it drifts down.
		scroll.y = 2.0F + Mth.sin(t * 0.07F + 1.3F) * 1.0F - 4.0F * c + dying * 14.0F;
		scroll.z = -9.0F - 2.0F * c;
		scroll.xRot = -0.25F + 0.25F * c + Mth.sin(t * 0.05F) * 0.06F + dying * 0.9F;
		scroll.yRot = state.yRot * Mth.DEG_TO_RAD * 0.3F + Mth.sin(t * 0.04F) * 0.1F;

		// Sunk under the water in its dive: flat out and streaming, its scroll rolled in close.
		float depth = state.extra[0];
		if (depth > 0.01F) {
			body.xRot = Mth.lerp(depth, body.xRot, 1.25F);
			head.xRot = Mth.lerp(depth, head.xRot, -0.9F);
			reach(rightArm, depth, 0.9F, 0, 0.25F);
			reach(leftArm, depth, 0.9F, 0, -0.25F);
			stream(depth, 0.4F, t);
			scroll.y = Mth.lerp(depth, scroll.y, 9.0F);
			scroll.z = Mth.lerp(depth, scroll.z, -3.0F);
			scroll.xRot = Mth.lerp(depth, scroll.xRot, 1.3F);
		}

		// Its own moves, each eased in over the rest of its pose (see TideScribeRules for what each one is).
		float go = state.extra[1];
		for (int m = 1; m <= TideScribeRules.MOVES; m++) {
			float w = Math.min(1, state.moves[m]);
			if (w > 0.01F) {
				boolean current = m == state.move;
				move(m, w, current ? state.since : 999, current ? state.tell : 0, go, t);
			}
		}

		// Between phases it rises with its arms flung high and its scroll held up over its head.
		body.y -= 3.0F * sh;
		scroll.y = Mth.lerp(sh, scroll.y, -17.0F + Mth.sin(t * 0.3F));
		scroll.z = Mth.lerp(sh, scroll.z, -3.0F);
		scroll.xRot = Mth.lerp(sh, scroll.xRot, -1.1F);
	}

	/**
	 * Its pose for its own move {@code m}, eased in by {@code w}: {@code s} ticks in (past its end once it's easing out) with a
	 * tell of {@code tell}; {@code go} is 1 on a second go, which comes off the other hand.
	 */
	private void move(int m, float w, float s, float tell, float go, float t) {
		float wind = phase(s, tell);
		float hit = decay(s - tell, 6);
		boolean thrown = s >= tell;
		switch (m) {
			case TideScribeRules.QUILLS -> {
				// The quill drawn back over its shoulder, leaning away; thrown forward hard. The second go comes off the other hand.
				float k = phase(s - tell, 3);
				float x = thrown ? Mth.lerp(k, -3.0F, -1.1F) : Mth.lerp(wind, -0.3F, -3.0F);
				float twist = thrown ? Mth.lerp(k, 0.45F, -0.35F) : 0.45F * wind;
				float lean = thrown ? Mth.lerp(k, -0.2F, 0.3F) : -0.2F * wind;
				boolean left = go > 0.5F;
				reach(left ? leftArm : rightArm, w, x, 0, left ? -0.2F : 0.2F);
				reach(left ? rightArm : leftArm, w, -0.6F, 0, left ? 0.5F : -0.5F);
				body.yRot += (left ? -twist : twist) * w;
				body.xRot += lean * w;
			}
			case TideScribeRules.SCRIPT -> {
				// Bowed over its scroll, held low and flat, the quill scratching fast; as its words burst it flings both arms up.
				body.xRot += 0.5F * w;
				head.xRot += 0.45F * w;
				reach(rightArm, w, -1.0F + Mth.sin(t * 2.2F) * 0.25F, -0.4F + Mth.cos(t * 1.9F) * 0.2F, 0.1F);
				reach(leftArm, w, -0.9F, 0.3F, -0.1F);
				scroll.y = Mth.lerp(w, scroll.y, 7.0F);
				scroll.z = Mth.lerp(w, scroll.z, -7.0F);
				scroll.xRot = Mth.lerp(w, scroll.xRot, 1.2F);
				float burst = decay(s - tell - TideScribeRules.SCRIPT_FUSE, 8)
					+ decay(s - tell - TideScribeRules.SCRIPT_FUSE - TideScribeRules.SCRIPT_AGAIN, 8);
				if (burst > 0) {
					float b = Math.min(1, burst) * w;
					reach(rightArm, b, -2.7F, 0, 0.4F);
					reach(leftArm, b, -2.7F, 0, -0.4F);
					body.xRot -= 0.6F * b;
				}
			}
			case TideScribeRules.WHIRLPOOL -> {
				// Sunk low with its arms flung out flat and its tendrils spread wide, spinning.
				body.y += 3.0F * w;
				body.xRot += 0.1F * w;
				reach(rightArm, w, -0.2F, 0, 1.45F);
				reach(leftArm, w, -0.2F, 0, -1.45F);
				flare(w, 1.3F * Math.max(0.4F, wind), t);
			}
			case TideScribeRules.RIPTIDE -> {
				// Coiled back with its arms drawn behind it; then flat out and racing, arms swept back, tendrils streaming.
				if (!thrown) {
					body.xRot -= 0.3F * wind * w;
					reach(rightArm, w * wind, 0.7F, 0, 0.3F);
					reach(leftArm, w * wind, 0.7F, 0, -0.3F);
					flare(w * wind, 0.1F, t);
				} else {
					float k = phase(s - tell, 2);
					body.xRot = Mth.lerp(w * k, body.xRot, 1.3F);
					head.xRot = Mth.lerp(w * k, head.xRot, -1.0F);
					reach(rightArm, w, 1.0F, 0, 0.15F);
					reach(leftArm, w, 1.0F, 0, -0.15F);
					stream(w, 0.5F, t);
				}
			}
			case TideScribeRules.INK -> {
				// Rearing back as its head swells dark; thrust forward as it sprays.
				if (!thrown) {
					body.xRot -= 0.3F * wind * w;
					head.xRot -= 0.6F * wind * w;
					reach(rightArm, w * wind, -0.9F, 0.4F, 0.6F);
					reach(leftArm, w * wind, -0.9F, -0.4F, -0.6F);
				} else {
					float k = Math.max(hit, 0.4F);
					body.xRot += 0.4F * k * w;
					head.xRot += 0.5F * k * w;
					reach(rightArm, w, -1.4F, -0.3F, 0.9F);
					reach(leftArm, w, -1.4F, 0.3F, -0.9F);
				}
			}
			case TideScribeRules.PAGES -> {
				// Its arms raised wide and its scroll held up over it, the pages wheeling round it.
				float up = thrown ? 1 : wind;
				reach(rightArm, w * up, -0.3F + Mth.sin(t * 0.2F) * 0.1F, 0, 2.2F);
				reach(leftArm, w * up, -0.3F - Mth.sin(t * 0.2F) * 0.1F, 0, -2.2F);
				head.xRot -= 0.35F * up * w;
				scroll.y = Mth.lerp(w * up, scroll.y, -15.0F);
				scroll.z = Mth.lerp(w * up, scroll.z, -2.0F);
				scroll.xRot = Mth.lerp(w * up, scroll.xRot, -1.3F);
				flare(w * up, 0.7F, t);
			}
			case TideScribeRules.BREAKER -> {
				// Both arms raised high, leaning back, its tendrils curled in under it; then it brings everything down at once.
				if (!thrown) {
					reach(rightArm, w * wind, -2.9F, 0, 0.3F);
					reach(leftArm, w * wind, -2.9F, 0, -0.3F);
					body.xRot -= 0.25F * wind * w;
					head.xRot -= 0.3F * wind * w;
					flare(w * wind, -0.3F, t);
				} else {
					float k = phase(s - tell, 2);
					reach(rightArm, w, Mth.lerp(k, -2.9F, -0.7F), 0, 0.3F);
					reach(leftArm, w, Mth.lerp(k, -2.9F, -0.7F), 0, -0.3F);
					body.xRot += 0.5F * k * w;
					flare(w * Math.max(hit, 0.3F), 1.2F, t);
				}
			}
			case TideScribeRules.DIVE -> {
				// Under the water it lies flat and streams (the depth above); bursting up, it thrusts its arms up over it.
				float burst = decay(s - tell - TideScribeRules.DIVE_HUNT, 8);
				if (burst > 0) {
					reach(rightArm, burst * w, -2.9F, 0, 0.5F);
					reach(leftArm, burst * w, -2.9F, 0, -0.5F);
					body.xRot -= 0.3F * burst * w;
				}
			}
			case TideScribeRules.TIDE_CALL -> {
				// Rising with its arms flung up wide and its head thrown back, calling; then it draws them down and in as the water comes.
				if (!thrown) {
					reach(rightArm, w * wind, -0.5F, 0, 2.6F);
					reach(leftArm, w * wind, -0.5F, 0, -2.6F);
					head.xRot -= 0.6F * wind * w;
					body.xRot -= 0.2F * wind * w;
					flare(w * wind, 1.0F, t);
				} else {
					float k = phase(s - tell, 10);
					reach(rightArm, w, Mth.lerp(k, -0.5F, -1.4F), Mth.lerp(k, 0, -0.5F), Mth.lerp(k, 2.6F, 0.3F));
					reach(leftArm, w, Mth.lerp(k, -0.5F, -1.4F), Mth.lerp(k, 0, 0.5F), Mth.lerp(k, -2.6F, -0.3F));
					head.xRot -= 0.6F * (1 - k) * w;
					flare(w, Mth.lerp(k, 1.0F, 0.4F), t);
				}
			}
			case TideScribeRules.BOWED -> {
				// Slumped over its writing, arms hanging, the scroll sunk low.
				body.xRot += 0.75F * w;
				head.xRot += 0.5F * w;
				reach(rightArm, w, -0.5F, 0, 0.15F);
				reach(leftArm, w, -0.5F, 0, -0.15F);
				scroll.y = Mth.lerp(w, scroll.y, 10.0F);
				scroll.xRot = Mth.lerp(w, scroll.xRot, 1.0F);
			}
			case TideScribeRules.SPUN -> {
				// Reeling, its head lolling round and its arms loose.
				body.zRot += Mth.sin(t * 0.45F) * 0.3F * w;
				body.xRot += Mth.cos(t * 0.35F) * 0.15F * w;
				head.zRot += Mth.sin(t * 0.4F + 1) * 0.35F * w;
				reach(rightArm, w, -0.3F + Mth.sin(t * 0.5F) * 0.3F, 0, 0.9F + Mth.cos(t * 0.4F) * 0.2F);
				reach(leftArm, w, -0.3F + Mth.cos(t * 0.5F) * 0.3F, 0, -0.9F - Mth.sin(t * 0.4F) * 0.2F);
				flare(w, 0.5F, t);
			}
			case TideScribeRules.SURFACED -> {
				// Thrown back on the water, dazed, its head lolling and its arms floating out to the sides.
				body.xRot -= 0.35F * w;
				head.xRot += 0.4F * w;
				head.zRot += Mth.sin(t * 0.15F) * 0.2F * w;
				reach(rightArm, w, -0.2F, 0, 1.2F);
				reach(leftArm, w, -0.2F, 0, -1.2F);
				flare(w, 0.9F, t);
			}
			default -> {
			}
		}
	}

	/** Eases {@code arm} toward {@code x}, {@code y}, {@code z} by {@code w}. */
	private static void reach(ModelPart arm, float w, float x, float y, float z) {
		arm.xRot = Mth.lerp(w, arm.xRot, x);
		arm.yRot = Mth.lerp(w, arm.yRot, y);
		arm.zRot = Mth.lerp(w, arm.zRot, z);
	}

	/** Eases its tendrils out round it by {@code w}, {@code out} radians off hanging (in under it if less than nothing). */
	private void flare(float w, float out, float t) {
		for (int i = 0; i < TENDRILS; i++) {
			float a = Mth.TWO_PI * i / TENDRILS;
			float wave = t * 0.3F + i * 1.1F;
			tendrils[i].xRot = Mth.lerp(w, tendrils[i].xRot, Mth.cos(a) * out + Mth.sin(wave) * 0.1F);
			tendrils[i].zRot = Mth.lerp(w, tendrils[i].zRot, -Mth.sin(a) * out);
			mids[i].xRot = Mth.lerp(w, mids[i].xRot, Mth.sin(wave - 0.9F) * 0.3F);
			tips[i].xRot = Mth.lerp(w, tips[i].xRot, Mth.sin(wave - 1.8F) * 0.4F);
		}
	}

	/** Eases its tendrils back behind it by {@code w}, {@code back} radians, streaming. */
	private void stream(float w, float back, float t) {
		for (int i = 0; i < TENDRILS; i++) {
			float wave = t * 0.5F + i * 1.1F;
			tendrils[i].xRot = Mth.lerp(w, tendrils[i].xRot, back + Mth.sin(wave) * 0.15F);
			tendrils[i].zRot = Mth.lerp(w, tendrils[i].zRot, Mth.cos(wave) * 0.1F);
			mids[i].xRot = Mth.lerp(w, mids[i].xRot, Mth.sin(wave - 0.9F) * 0.25F);
			tips[i].xRot = Mth.lerp(w, tips[i].xRot, Mth.sin(wave - 1.8F) * 0.35F);
		}
	}

	/** How far through {@code length} ticks {@code s} is, eased, 0 to 1 (1 if there's no length to it). */
	private static float phase(float s, float length) {
		if (length <= 0) {
			return 1;
		}
		float x = Mth.clamp(s / length, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/** 1 the moment {@code s} reaches 0, falling to nothing over {@code length} ticks (0 before). */
	private static float decay(float s, float length) {
		return s < 0 ? 0 : Math.max(0, 1 - s / length);
	}
}
