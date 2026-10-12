package dev.wildercord.client.monster;

import dev.wildercord.monster.SandmawRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Sandmaw Wyrm: a column of four great ringed segments rising out of the sand, narrowing upward, a ridge of spines
 * down its back, and a blunt head whose mouth, facing forward, is ringed with four jaw-plates that flare open.
 *
 * <p>Under the sand ({@code veil}) it sinks out of sight below the ground. Every move has its own pose, eased in and out
 * ({@link MonsterRenderState#moves}) and run along by how far into the move it is: rearing back for the slam and whipping
 * into it, stuck heaving after; the throat swelling and the head thrust out for each gout of the spit, turned along each
 * fanned gout roused; reared over the gulp's spot gnashing, crashing down on it, then chewing smugly with a lump going down;
 * head buried and the column churning in the sinkhole, rising jaws wide to snap; plunged and heaving with each geyser;
 * coiled tight and jolting with each spine volley; laid along its arc rippling as it breaches, slumped writhing where it
 * beaches; flailing as it thrashes at half health.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code tools/sandmaw_art.py}):</p>
 * <pre>
 *   seg0 (0,0) 12x10x12    seg1 (48,0) 11x10x11   seg2 (0,22) 10x10x10   seg3 (40,22) 9x10x9
 *   head (76,22) 10x8x10   jaw side (0,44) 1x6x4  jaw lip (12,44) 4x1x4  spine (30,44) 2x3x2
 * </pre>
 */
public class SandmawWyrmModel extends EntityModel<MonsterRenderState> {
	private static final float BODY_Y = 24.0F;
	/** How far it sinks under the sand, fully burrowed. */
	private static final float SINK = 54.0F;

	private final ModelPart body;
	private final ModelPart[] segs = new ModelPart[4];
	private final ModelPart head;
	private final ModelPart[] jaws = new ModelPart[4];

	public SandmawWyrmModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		ModelPart parent = body;
		for (int i = 0; i < 4; i++) {
			segs[i] = parent.getChild("seg_" + i);
			parent = segs[i];
		}
		this.head = segs[3].getChild("head");
		for (int i = 0; i < 4; i++) {
			jaws[i] = head.getChild("jaw_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, BODY_Y, 0.0F));

		int[][] uv = {{0, 0}, {48, 0}, {0, 22}, {40, 22}};
		float[] size = {12, 11, 10, 9};
		PartDefinition parent = body;
		for (int i = 0; i < 4; i++) {
			float s = size[i];
			CubeListBuilder cubes = CubeListBuilder.create().texOffs(uv[i][0], uv[i][1]).addBox(-s / 2, -10.0F, -s / 2, s, 10.0F, s);
			if (i > 0) {
				cubes.texOffs(30, 44).addBox(-1.0F, -7.0F, s / 2 - 0.5F, 2.0F, 3.0F, 2.0F);
			}
			parent = parent.addOrReplaceChild("seg_" + i, cubes, PartPose.offset(0.0F, i == 0 ? 0.0F : -9.0F, 0.0F));
		}
		PartDefinition head = parent.addOrReplaceChild("head", CubeListBuilder.create().texOffs(76, 22).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 8.0F, 10.0F),
			PartPose.offset(0.0F, -9.0F, 0.0F));
		// Its jaw-plates, ringing the mouth: two at the sides, one above, one below.
		head.addOrReplaceChild("jaw_0", CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F, -3.0F, -4.0F, 1.0F, 6.0F, 4.0F),
			PartPose.offset(-4.0F, -4.0F, -5.0F));
		head.addOrReplaceChild("jaw_1", CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-0.5F, -3.0F, -4.0F, 1.0F, 6.0F, 4.0F),
			PartPose.offset(4.0F, -4.0F, -5.0F));
		head.addOrReplaceChild("jaw_2", CubeListBuilder.create().texOffs(12, 44).addBox(-2.0F, -0.5F, -4.0F, 4.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, -7.0F, -5.0F));
		head.addOrReplaceChild("jaw_3", CubeListBuilder.create().texOffs(12, 44).addBox(-2.0F, -0.5F, -4.0F, 4.0F, 1.0F, 4.0F),
			PartPose.offset(0.0F, -1.0F, -5.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float[] w = state.moves;
		int move = state.move;
		float s = state.since;
		float tell = Math.max(1, state.tell);
		float idle = w[SandmawRules.IDLE];

		// Each move's pose, weighted by how far it has eased in; for the move under way, by how far into it it is, and for one
		// easing out, as it ended.
		float lean = 0.03F * idle, sink = 0, pitch = 0, shift = 0, lift = 0;
		float headX = 0, headY = 0, headZ = 0;
		float flare = (0.15F + Mth.sin(t * 0.11F) * 0.1F) * idle;
		float writhe = 0, twist = 0, zig = 0, twitch = 0, swell = 0, throat = 0, undulate = 0;
		float[] lump = new float[4];

		// Bursting up: reared straight up, jaws wide.
		float erupt = w[SandmawRules.ERUPT];
		lean -= 0.05F * erupt;
		headX -= 0.6F * erupt;
		flare += 1.1F * erupt;

		// Diving: tipping head first back into the sand.
		float dive = w[SandmawRules.DIVE];
		lean += 0.35F * dive;
		headX += 0.4F * dive;
		flare += 0.3F * dive;

		// The slam: rearing back, head high, then the whole column whipped forward, head to the ground.
		float slamLean = 0.42F, slamHead = 0.25F, slamFlare = -0.2F;
		if (move == SandmawRules.SLAM) {
			if (s < tell) {
				float r = ease(s / tell);
				slamLean = -0.18F * r;
				slamHead = -0.5F * r;
				slamFlare = 0.15F + 0.5F * r;
			} else {
				float k = ease((s - tell) / 2);
				slamLean = Mth.lerp(k, -0.18F, 0.42F);
				slamHead = Mth.lerp(k, -0.5F, 0.25F);
				slamFlare = Mth.lerp(k, 0.65F, -0.2F);
			}
		}
		lean += slamLean * w[SandmawRules.SLAM];
		headX += slamHead * w[SandmawRules.SLAM];
		flare += slamFlare * w[SandmawRules.SLAM];

		// Stuck in the ground after it: heaving to work its head free, twitching.
		float stuck = w[SandmawRules.STUCK];
		lean += (0.42F + Mth.sin(t * 0.35F) * 0.05F) * stuck;
		headX += 0.25F * stuck;
		sink += 3 * stuck;
		twitch += stuck;
		flare += Math.max(0, Mth.sin(t * 0.9F)) * 0.3F * stuck;

		// The spit: head pulled back and throat swelling, jaws flared, then each gout thrust out; roused, the head turned
		// along each of the three fanned gouts.
		float spitLean = 0.02F, spitHead = 0, spitFlare = 0.3F, spitThroat = 0, spitTurn = 0;
		if (move == SandmawRules.SPIT) {
			if (s < tell) {
				float r = ease(s / tell);
				spitLean = -0.12F * r;
				spitHead = -0.4F * r;
				spitFlare = 0.3F + 0.9F * r;
				spitThroat = 0.2F * r;
			} else {
				float into = s - tell;
				int gouts = state.roused ? 3 : 1;
				int k = (int) (into / SandmawRules.SPIT_GOUT);
				if (k < gouts) {
					float kick = 1 - Mth.clamp((into - k * SandmawRules.SPIT_GOUT) / 5.0F, 0, 1);
					spitLean = 0.1F + 0.1F * kick;
					spitHead = 0.15F + 0.15F * kick;
					spitFlare = 1.2F + 0.3F * kick;
					spitThroat = 0.2F * (1 - (k + 1 - kick) / gouts);
					spitTurn = state.roused ? -(k - 1) * (float) SandmawRules.SPIT_FAN * Mth.DEG_TO_RAD : 0;
				}
			}
		}
		lean += spitLean * w[SandmawRules.SPIT];
		headX += spitHead * w[SandmawRules.SPIT];
		headY += spitTurn * w[SandmawRules.SPIT];
		flare += spitFlare * w[SandmawRules.SPIT];
		throat += spitThroat * w[SandmawRules.SPIT];

		// The gulp: reared high over the marked spot, head bent down to it, jaws gaping and gnashing, then crashing down jaws
		// first on it.
		float gulpLean = 0.38F, gulpHead = 0.35F, gulpFlare = 1.5F, gulpSink = 18;
		if (move == SandmawRules.GULP) {
			if (s < tell) {
				float r = ease(s / tell);
				gulpLean = -0.22F * r;
				gulpHead = 0.55F * r;
				gulpFlare = 0.3F + (1.2F + Mth.sin(t * 1.2F) * 0.25F) * r;
				gulpSink = 0;
			} else {
				float q = ease((s - tell) / SandmawRules.GULP_PLUNGE);
				gulpLean = Mth.lerp(q, -0.22F, 0.38F);
				gulpHead = Mth.lerp(q, 0.55F, 0.35F);
				gulpSink = 18 * q;
			}
		}
		lean += gulpLean * w[SandmawRules.GULP];
		headX += gulpHead * w[SandmawRules.GULP];
		flare += gulpFlare * w[SandmawRules.GULP];
		sink += gulpSink * w[SandmawRules.GULP];

		// Someone swallowed: upright and smug, jaws shut and chewing, a lump working its way down its throat.
		float gulped = w[SandmawRules.GULPED];
		lean += 0.05F * gulped;
		headX -= 0.2F * gulped;
		flare += (-0.05F + Math.max(0, Mth.sin(t * 0.6F)) * 0.3F) * gulped;
		writhe += 0.25F * gulped;
		float down = 3.5F - (s * 0.06F % 1) * 4.5F;
		for (int i = 0; i < 4; i++) {
			lump[i] = 0.2F * Math.max(0, 1 - Math.abs(i - down)) * gulped;
		}

		// Choking on a miss, or on a fighter who broke free: lurching, head shaking, jaws gaping, coughing.
		float choke = w[SandmawRules.CHOKING];
		lean += (0.25F + Mth.sin(t * 0.9F) * 0.12F) * choke;
		headX += (0.3F + Math.max(0, Mth.sin(t * 0.45F)) * 0.3F) * choke;
		headZ += Mth.sin(t * 1.4F) * 0.3F * choke;
		flare += (1.2F + Mth.sin(t * 2.1F) * 0.2F) * choke;

		// The sinkhole: head buried, the column churning as it drags the sand in, rising jaws wide at the end and snapping.
		float holeSink = 0, holeLean = -0.05F, holeHead = 0.1F, holeFlare = -0.2F, holeTwist = 0;
		if (move == SandmawRules.SINKHOLE) {
			float pull = state.roused ? SandmawRules.ROUSED_SINK_PULL : SandmawRules.SINK_PULL;
			float snap = tell + pull, mark = snap - SandmawRules.SINK_MARK;
			if (s < tell) {
				float r = ease(s / tell);
				holeSink = 30 * r;
				holeLean = 0.25F * r;
				holeHead = 0.4F * r;
				holeFlare = 0;
				holeTwist = r;
			} else if (s < mark) {
				holeSink = 30;
				holeLean = 0.25F;
				holeHead = 0.4F;
				holeFlare = 0;
				holeTwist = 1;
			} else if (s < snap) {
				float r = ease((s - mark) / SandmawRules.SINK_MARK);
				holeSink = Mth.lerp(r, 30, 12);
				holeLean = Mth.lerp(r, 0.25F, -0.05F);
				holeHead = Mth.lerp(r, 0.4F, -0.5F);
				holeFlare = 1.6F * r;
				holeTwist = 1 - r;
			} else {
				float r = ease((s - snap) / 3);
				holeSink = Mth.lerp(r, 12, 0);
				holeHead = Mth.lerp(r, -0.5F, 0.1F);
				holeFlare = Mth.lerp(r, 1.6F, -0.2F);
			}
		}
		sink += holeSink * w[SandmawRules.SINKHOLE];
		lean += holeLean * w[SandmawRules.SINKHOLE];
		headX += holeHead * w[SandmawRules.SINKHOLE];
		flare += holeFlare * w[SandmawRules.SINKHOLE];
		twist += holeTwist * w[SandmawRules.SINKHOLE];

		// The geysers: head plunged into the sand, the column heaving as each one is sent up, then rising out.
		float geyserDown = 0, geyserHeave = 0;
		if (move == SandmawRules.GEYSER) {
			float every = state.roused ? SandmawRules.ROUSED_GEYSER_EVERY : SandmawRules.GEYSER_EVERY;
			float count = state.roused ? SandmawRules.ROUSED_GEYSER_COUNT : SandmawRules.GEYSER_COUNT;
			float last = tell + (count - 1) * every;
			if (s < tell) {
				geyserDown = ease(s / tell);
			} else if (s <= last) {
				geyserDown = 1;
				geyserHeave = 1 - (s - tell) % every / every;
			} else {
				geyserDown = 1 - ease((s - last - SandmawRules.GEYSER_FUSE) / 8);
			}
		}
		geyserDown *= w[SandmawRules.GEYSER];
		geyserHeave *= w[SandmawRules.GEYSER];
		lean += 0.4F * geyserDown + 0.06F * geyserHeave;
		headX += 0.5F * geyserDown;
		sink += 18 * geyserDown;
		swell += 0.08F * geyserHeave;

		// The spine volley: coiled tight, swelling and shuddering, then jolting as the spines fly; roused, coiling again for
		// the second.
		float quillZig = 0, quillSwell = 0;
		if (move == SandmawRules.QUILLS) {
			float again = state.roused ? SandmawRules.QUILL_AGAIN : 0;
			if (s < tell) {
				float r = ease(s / tell);
				quillZig = 0.22F * r;
				quillSwell = 0.1F * r;
				twitch += 0.5F * r * w[SandmawRules.QUILLS];
			} else {
				float into = s - tell;
				float jolt = Math.max(decay(into, 6), state.roused ? decay(into - again, 6) : 0);
				float coil = into < again ? 1 : 1 - ease((into - again) / 8);
				quillZig = 0.22F * coil;
				quillSwell = 0.1F * coil + 0.22F * jolt;
				flare += 0.8F * jolt * w[SandmawRules.QUILLS];
			}
		}
		zig += quillZig * w[SandmawRules.QUILLS];
		swell += quillSwell * w[SandmawRules.QUILLS];

		// The breach: out of the sand along its furrow and through the air, laid along its arc, rippling; then crashing head
		// down at the end of it.
		float flight = 2.5F, breachUndulate = 0;
		if (move == SandmawRules.BREACH) {
			float p = Mth.clamp((s - tell) / SandmawRules.BREACH_FLIGHT, 0, 1);
			flight = Mth.HALF_PI - (float) Math.atan(1.33F * (1 - 2 * p));
			breachUndulate = s >= tell ? 1 : 0;
		}
		float breach = w[SandmawRules.BREACH];
		pitch += flight * breach;
		shift += 22 * breach;
		lift -= 6 * breach;
		undulate += breachUndulate * breach;
		flare += 1.2F * breach;

		// Beached: slumped along the ground where it crashed, writhing, head lifting and flopping, jaws gasping.
		float beached = w[SandmawRules.BEACHED];
		pitch += 1.1F * beached;
		lean += 0.12F * beached;
		shift += 18 * beached;
		lift -= 4 * beached;
		headX += (-0.3F + Mth.sin(t * 0.2F) * 0.25F) * beached;
		flare += (0.6F + Mth.sin(t * 0.3F) * 0.5F) * beached;
		writhe += 0.6F * beached;
		twist += 0.3F * beached;

		// The thrash at half health: rearing back, then flailing wildly, roaring, as it throws everyone back.
		float thrashWrithe = 0, thrashLean = 0, thrashHead = 0, thrashFlare = 0;
		if (move == SandmawRules.THRASH) {
			if (s < 8) {
				float r = ease(s / 8);
				thrashLean = -0.2F * r;
				thrashWrithe = 0.3F * r;
				thrashHead = -0.6F * r;
				thrashFlare = 1.4F * r;
			} else {
				float fade = 1 - ease((s - (SandmawRules.THRASH_TICKS - 8)) / 8);
				thrashLean = -0.05F * fade;
				thrashWrithe = 1.2F * fade;
				thrashHead = -0.6F * fade;
				thrashFlare = 1.5F * fade;
			}
		}
		lean += thrashLean * w[SandmawRules.THRASH];
		writhe += thrashWrithe * w[SandmawRules.THRASH];
		headX += thrashHead * w[SandmawRules.THRASH];
		flare += thrashFlare * w[SandmawRules.THRASH];

		// Roused, it never quite holds still.
		float speed = state.roused ? 1.6F : 1;

		body.y = BODY_Y + SINK * state.veil + sink + lift;
		body.z = shift;
		body.xRot = pitch;

		float total = 0, scale = 1;
		for (int i = 0; i < 4; i++) {
			float share = i == 0 ? 0.5F : 1.0F;
			segs[i].xRot = lean * share + Mth.cos(t * 0.05F + i * 0.6F) * 0.03F * idle
				+ Mth.sin(t * 0.7F - i * 1.2F) * 0.25F * undulate;
			segs[i].zRot = Mth.sin(t * 0.07F * speed + i * 0.8F) * 0.07F * idle
				+ Mth.sin(t * 0.8F + i * 1.1F) * 0.3F * writhe
				+ Mth.sin(t * 1.3F + i) * 0.06F * twitch
				+ (i % 2 == 0 ? zig : -zig);
			segs[i].yRot = Mth.sin(t * 0.5F + i * 0.9F) * 0.45F * twist;
			// Swelling, and the lump of someone swallowed; each part's scale undoing its parent's.
			float size = 1 + swell + lump[i] + (i == 3 ? throat : 0);
			segs[i].xScale = size / scale;
			segs[i].zScale = size / scale;
			scale = size;
			total += segs[i].xRot;
		}
		head.xScale = 1 / scale;
		head.zScale = 1 / scale;

		// The head: turning to look, held level against the column's lean, when it has nothing else to do.
		head.xRot = (-total * 0.6F + state.xRot * Mth.DEG_TO_RAD * 0.5F) * idle + headX;
		head.yRot = Mth.clamp(state.yRot, -60, 60) * Mth.DEG_TO_RAD * 0.7F * idle + headY;
		head.zRot = Mth.sin(t * 1.7F) * 0.08F * twitch + Mth.sin(t * 0.8F + 4.4F) * 0.2F * writhe + headZ;

		flare = Math.max(-0.1F, flare + (state.roused ? Math.max(0, Mth.sin(t * 0.5F)) * 0.1F * idle : 0));
		jaws[0].yRot = flare;
		jaws[1].yRot = -flare;
		jaws[2].xRot = -flare;
		jaws[3].xRot = flare;
	}

	private static float ease(float x) {
		x = Mth.clamp(x, 0, 1);
		return x * x * (3 - 2 * x);
	}

	/** 1 at the start of a span, falling to 0 at its end, and 0 outside it. */
	private static float decay(float x, float span) {
		return x < 0 || x > span ? 0 : 1 - x / span;
	}
}
