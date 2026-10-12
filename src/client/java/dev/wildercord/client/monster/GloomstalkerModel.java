package dev.wildercord.client.monster;

import dev.wildercord.monster.GiantRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The Gloomstalker: a long, low panther of shadow, deep chest tapering to lean hips, a small wedge of a head with swept-back
 * ears, a long, low-hanging tail that curls up at the tip and trails wisps of dark, and a crest of smoky fur along its spine. It trots
 * lightly; crouching to pounce it sinks low on folded legs, head down, ears flat, tail lashing; mid-pounce it stretches out
 * full length, forelegs reaching; a missed pounce leaves it sprawled flat, legs splayed. Grown into a giant it fights by a
 * whole set of moves ({@link GiantRules.Matriarch}), each with its own pose: the pounce, a paw raised and raked across, a look
 * back over her shoulder and her tail drawn round and flung, sitting back to howl, drawing breath and sweeping it across her
 * front, diving low into the snow, a lunge and a skid, rearing to call down icicles and stamping as they fall, and a roar.
 *
 * <p>Drawn translucent (its renderer sets how much shows), so the default render type is one that blends.</p>
 *
 * <p>Laid out on a 128x64 skin (see {@code gloomstalker} in {@code tools/monster_art.py}):</p>
 * <pre>
 *   chest (0,0) 7x6x9     hips (0,16) 6x5x10    head (48,0) 5x4x5     snout (72,0) 3x2x3    ear (86,0) 2x2x1
 *   jaw (92,0) 3x1x3      leg (48,12) 3x7x3     paw (60,12) 3x2x4     tail (76,12) 2x2x12   tip (104,12) 2x2x8
 *   crest (0,32) 0x3x16   wisp (34,32) 0x4x5
 * </pre>
 */
public class GloomstalkerModel extends EntityModel<MonsterRenderState> {
	/** Where its legs hang from (they're 7 long, to the ground at 24), and where its body sits. */
	private static final float LEG_Y = 17.0F;
	private static final float BODY_Y = 14.5F;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart tip;
	private final ModelPart frontRight;
	private final ModelPart frontLeft;
	private final ModelPart backRight;
	private final ModelPart backLeft;
	/** 0.13: the giant's moves laid over the pose, summed each frame. */
	private final Giant giant = new Giant();

	public GloomstalkerModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.jaw = head.getChild("jaw");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.tail = body.getChild("tail");
		this.tip = tail.getChild("tip");
		this.frontRight = root.getChild("front_right");
		this.frontLeft = root.getChild("front_left");
		this.backRight = root.getChild("back_right");
		this.backLeft = root.getChild("back_left");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// A deep chest and lean hips, one long, low piece, pivoting behind the shoulders.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5F, -3.5F, -9.0F, 7.0F, 6.0F, 9.0F)
				.texOffs(0, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 5.0F, 10.0F),
			PartPose.offset(0.0F, BODY_Y, 0.0F));
		body.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 16.0F),
			PartPose.offset(0.0F, -3.4F, -8.0F));

		// A small, flat wedge of a head with a short muzzle, carried low and level.
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 0).addBox(-2.5F, -2.0F, -4.5F, 5.0F, 4.0F, 5.0F)
				.texOffs(72, 0).addBox(-1.5F, 0.0F, -7.0F, 3.0F, 2.0F, 3.0F),
			PartPose.offset(0.0F, -1.5F, -9.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(92, 0).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 3.0F),
			PartPose.offset(0.0F, 2.0F, -4.0F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(86, 0).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offsetAndRotation(-1.6F, -2.0F, -1.0F, -0.35F, 0.0F, -0.25F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(86, 0).mirror().addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
			PartPose.offsetAndRotation(1.6F, -2.0F, -1.0F, -0.35F, 0.0F, 0.25F));

		// A long tail, hanging low, its tip curling up into wisps of dark.
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(76, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 12.0F),
			PartPose.offsetAndRotation(0.0F, -1.5F, 9.5F, -0.5F, 0.0F, 0.0F));
		PartDefinition tip = tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(104, 12).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F),
			PartPose.offsetAndRotation(0.0F, 0.0F, 11.5F, 0.85F, 0.0F, 0.0F));
		tip.addOrReplaceChild("wisp", CubeListBuilder.create().texOffs(34, 32).addBox(0.0F, -2.0F, 0.0F, 0.0F, 4.0F, 5.0F),
			PartPose.offset(0.0F, 0.0F, 7.0F));

		leg(root, "front_right", -2.0F, -6.5F, false);
		leg(root, "front_left", 2.0F, -6.5F, true);
		leg(root, "back_right", -2.0F, 7.5F, false);
		leg(root, "back_left", 2.0F, 7.5F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder leg = CubeListBuilder.create().texOffs(48, 12);
		PartDefinition part = root.addOrReplaceChild(name, (mirror ? leg.mirror() : leg).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F),
			PartPose.offset(x, LEG_Y, z));
		CubeListBuilder paw = CubeListBuilder.create().texOffs(60, 12);
		part.addOrReplaceChild("paw", (mirror ? paw.mirror() : paw).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 2.0F, 4.0F), PartPose.offset(0.0F, 5.5F, 0.0F));
	}

	@Override
	public void setupAnim(MonsterRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float walk = Math.min(1, state.walkAnimationSpeed);
		float pos = state.walkAnimationPos * 0.75F;
		float crouch = state.windup;
		float leap = state.acting;
		float sprawl = state.stunned;
		float slink = state.alt;
		if (state.giant) {
			moves(state, t);
			crouch = giant.crouch;
			leap = giant.leap;
			sprawl = giant.sprawl;
			slink = giant.slink;
		}

		// A light trot: diagonal pairs together, a little bob.
		float stride = Mth.cos(pos) * 0.9F * walk;
		frontRight.xRot = stride;
		backLeft.xRot = stride;
		frontLeft.xRot = -stride;
		backRight.xRot = -stride;
		body.y = BODY_Y - Math.abs(Mth.sin(pos)) * 0.6F * walk;
		body.xRot = 0.0F;

		// Crouched to pounce: low on folded legs, hindquarters wiggling.
		float sink = 2.4F * crouch;
		body.y += sink;
		body.zRot = crouch * Mth.sin(t * 0.9F) * 0.04F;
		foldLegs(crouch, -0.95F, 0.95F);
		// Mid-pounce: stretched out full length, forelegs reaching, hind legs kicked back.
		body.xRot -= 0.15F * leap;
		frontRight.xRot = Mth.lerp(leap, frontRight.xRot, -1.35F);
		frontLeft.xRot = Mth.lerp(leap, frontLeft.xRot, -1.25F);
		backRight.xRot = Mth.lerp(leap, backRight.xRot, 1.2F);
		backLeft.xRot = Mth.lerp(leap, backLeft.xRot, 1.3F);
		// Sprawled after a miss: flat on its belly, legs splayed.
		body.y += 3.2F * sprawl;
		frontRight.zRot = 1.0F * sprawl;
		backRight.zRot = 1.1F * sprawl;
		frontLeft.zRot = -1.0F * sprawl;
		backLeft.zRot = -1.1F * sprawl;
		for (ModelPart leg : new ModelPart[] {frontRight, frontLeft, backRight, backLeft}) {
			leg.y = LEG_Y + sink + 2.8F * sprawl;
		}

		// The head turns to look; low and level while it crouches, dropped on the ground when sprawled.
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - sprawl);
		head.xRot = Mth.clamp(state.xRot, -30, 30) * Mth.DEG_TO_RAD + 0.25F * crouch - 0.25F * leap + 0.35F * sprawl;
		jaw.xRot = 0.5F * crouch * (0.6F + 0.4F * Mth.sin(t * 0.5F)) + 0.6F * leap;
		// Ears flatten as it crouches, slinks off or leaps.
		float flat = Math.max(crouch, Math.max(slink, leap));
		rightEar.xRot = -0.35F - 0.9F * flat;
		leftEar.xRot = -0.35F - 0.9F * flat;

		// The tail: a slow sway at rest, a lash while it crouches, straight out behind in a leap, low as it slinks away.
		float lash = Mth.sin(t * (0.12F + 0.8F * crouch)) * (0.25F + 0.35F * crouch);
		tail.yRot = lash * (1 - leap);
		tail.xRot = -0.5F + 0.4F * leap - 0.1F * crouch - 0.2F * slink + 0.25F * sprawl + Mth.sin(t * 0.07F) * 0.05F;
		tip.xRot = 0.85F - 0.75F * leap + Mth.sin(t * 0.1F + 1.2F) * 0.15F;
		tip.yRot = lash * 0.8F * (1 - leap);
		if (state.giant) {
			apply(state, t);
		}
	}

	private void foldLegs(float amount, float front, float back) {
		frontRight.xRot = Mth.lerp(amount, frontRight.xRot, front);
		frontLeft.xRot = Mth.lerp(amount, frontLeft.xRot, front);
		backRight.xRot = Mth.lerp(amount, backRight.xRot, back);
		backLeft.xRot = Mth.lerp(amount, backLeft.xRot, back);
	}

	/** 0.13: sums the giant's moves, each weighted by how far it has eased in or out. */
	private void moves(MonsterRenderState state, float t) {
		giant.clear();
		int side = state.alt > 0.5F ? -1 : 1;
		for (int m = 1; m < GiantRules.Matriarch.MOVES; m++) {
			float w = Math.min(1, state.moves[m]);
			if (w >= 0.01F) {
				boolean current = m == state.move;
				fill(giant, m, current ? state.since : 999, current ? state.tell : 0, side, t, w);
			}
		}
	}

	/** 0.13: lays the giant's moves over the pose already set. */
	private void apply(MonsterRenderState state, float t) {
		Giant g = giant;
		float shake = g.shake + (state.roused ? 0.2F : 0);
		// Reared up with her front lifted about her hips, her forelegs going up with it.
		float rear = g.rear;
		body.xRot += rear + g.sit;
		body.y += 7.5F * Mth.sin(rear) + g.sink;
		body.z += 7.5F * (1 - Mth.cos(rear));
		for (ModelPart leg : new ModelPart[] {frontRight, frontLeft}) {
			leg.y += 14.0F * Mth.sin(rear);
			leg.z += 14.0F * (1 - Mth.cos(rear));
			leg.xRot += rear;
		}
		// Sat back with her hips dropped about her shoulders, her hind legs folded under her.
		float sit = g.sit;
		body.y -= 6.5F * Mth.sin(sit);
		body.z -= 6.5F * (1 - Mth.cos(sit));
		for (ModelPart leg : new ModelPart[] {backRight, backLeft}) {
			leg.y -= 14.0F * Mth.sin(sit);
			leg.z -= 14.0F * (1 - Mth.cos(sit));
			leg.xRot += g.backX;
		}
		body.yRot += g.twist;
		body.zRot += g.roll + Mth.sin(t * 2.1F) * 0.03F * shake;
		frontRight.xRot += g.frX;
		frontRight.zRot += g.frZ;
		frontLeft.xRot += g.flX;
		frontLeft.zRot += g.flZ;

		head.yRot = head.yRot * (1 - Math.min(1, g.lock)) + g.headY;
		head.xRot += g.headX + Mth.sin(t * 2.7F) * 0.03F * shake;
		jaw.xRot = Math.min(1.1F, jaw.xRot + g.jaw);
		tail.yRot += g.tailY;
		tail.xRot += g.tailX;
		tip.yRot += g.tailY * 0.4F;
		float flat = Math.min(1, g.flat);
		rightEar.xRot = Math.min(rightEar.xRot, -0.35F - 0.9F * flat);
		leftEar.xRot = Math.min(leftEar.xRot, -0.35F - 0.9F * flat);
	}

	/**
	 * Adds one move's pose at weight {@code w}, {@code s} ticks in (past its end once it's easing out), its tell {@code tell}
	 * ticks long; {@code side} is the paw or the way her tail comes from (1 her right).
	 */
	private static void fill(Giant g, int move, float s, float tell, int side, float t, float w) {
		switch (move) {
			case GiantRules.Matriarch.POUNCE -> {
				// Crouched low on the ring as it follows you; stretched out in the leap.
				if (s < tell) {
					g.crouch += w * phase(s, Math.min(tell, 8));
				} else {
					g.leap += w * (1 - phase(s - tell - GiantRules.Matriarch.POUNCE_LEAP, 4));
				}
			}
			case GiantRules.Matriarch.SPRAWL -> {
				g.sprawl += w;
				g.flat += w;
			}
			case GiantRules.Matriarch.SWIPES -> {
				// Up a little on her haunches with a paw raised high and out, snarling; raked down across her front.
				float r = s < tell ? phase(s, tell) : 1, k = s < tell ? 0 : phase(s - tell, 2);
				g.rear += w * r * Mth.lerp(k, -0.3F, 0.05F);
				g.twist += w * r * side * Mth.lerp(k, 0.25F, -0.3F);
				paw(g, side, w * r, Mth.lerp(k, -1.6F, -0.4F), Mth.lerp(k, 0.5F, -0.5F));
				g.headY += w * r * side * 0.2F * (1 - k);
				g.jaw += w * r * Mth.lerp(k, 0.4F, 0.6F);
				g.flat += w * r;
			}
			case GiantRules.Matriarch.TAIL -> {
				// A look back over her shoulder, hips and tail drawn round to that side; the tail flung across behind her.
				float r = s < tell ? phase(s, tell) : 1, k = s < tell ? 0 : phase(s - tell, 3);
				g.headY += w * r * side * Mth.lerp(k, 0.9F, 0.3F);
				g.lock += w * r;
				g.tailY += w * r * side * Mth.lerp(k, -1.3F, 1.4F);
				g.tailX += w * r * 0.4F;
				g.twist += w * r * side * Mth.lerp(k, -0.3F, 0.35F);
				g.roll += w * r * side * 0.06F * (1 - k);
			}
			case GiantRules.Matriarch.HOWL -> {
				// Sat back on her haunches, head thrown up, her jaw opening as the howl comes; shaking with it.
				float r = phase(s, tell * 0.6F), open = phase(s, tell);
				g.sit -= w * 0.5F * r;
				g.backX -= w * 1.3F * r;
				g.headX -= w * 0.9F * r;
				g.jaw += w * 0.9F * open;
				g.shake += w * (s < tell ? 0.3F * open : 0.7F);
				g.tailX -= w * 0.3F * r;
			}
			case GiantRules.Matriarch.BREATH -> {
				// Head drawn back and up, chest swelling, as she breathes in; the jet sweeping across her front, then a cough.
				if (s < tell) {
					float r = phase(s, tell);
					g.headX -= w * 0.45F * r;
					g.rear -= w * 0.15F * r;
					g.jaw += w * 0.2F * r;
				} else {
					float k = s - tell, ticks = GiantRules.Matriarch.BREATH_TICKS;
					float f = Mth.clamp(k / ticks, 0, 1);
					float angle = (float) GiantRules.Matriarch.BREATH_SWEEP * (2 * f - 1) * side * Mth.DEG_TO_RAD;
					float on = 1 - phase(k - ticks, 4);
					g.headY += w * on * angle * 0.7F;
					g.twist += w * on * angle * 0.3F;
					g.lock += w * on;
					g.headX += w * (0.15F * on + 0.3F * decay(k - ticks, 6));
					g.jaw += w * (0.85F * on + 0.3F * decay(k - ticks, 6));
					g.sink += w * 0.6F * on;
				}
			}
			case GiantRules.Matriarch.VANISH -> {
				// Diving low into the snow (she fades from sight as she goes).
				float r = phase(s, tell);
				g.slink += w * r;
				g.sink += w * 1.5F * r;
				g.headX += w * 0.3F * r;
				g.flat += w * r;
			}
			case GiantRules.Matriarch.LUNGE -> {
				// Crouched to spring; stretched out along the dash; skidding to a stop.
				float ticks = GiantRules.Matriarch.LUNGE_TICKS;
				if (s < tell) {
					g.crouch += w * phase(s, Math.min(tell, 8));
				} else {
					g.leap += w * (1 - phase(s - tell - ticks, 3));
					float skid = phase(s - tell - ticks, 2) * (1 - phase(s - tell - ticks - 4, 4));
					g.rear -= w * 0.2F * skid;
					paw(g, 1, w * skid, -0.6F, 0);
					paw(g, -1, w * skid, -0.6F, 0);
				}
				g.flat += w;
			}
			case GiantRules.Matriarch.ICICLES -> {
				// Reared high with her forepaws up, screeching; brought down hard, and a stamp for each wave that falls.
				if (s < tell) {
					float r = phase(s, tell);
					g.rear -= w * 0.85F * r;
					paw(g, 1, w * r, -1.3F, 0.15F);
					paw(g, -1, w * r, -1.3F, 0.15F);
					g.headX -= w * 0.5F * r;
					g.jaw += w * 0.6F * r;
				} else {
					float k = s - tell, every = GiantRules.Matriarch.ICICLE_EVERY;
					float slam = phase(k, 2);
					int wave = (int) (k / every);
					float lift = wave + 1 < GiantRules.Matriarch.ICICLE_WAVES ? phase(k % every - (every - 4), 3) : 0;
					float up = (1 - slam) * 0.85F + 0.3F * lift;
					g.rear -= w * up;
					paw(g, 1, w, -1.3F * (1 - slam) - 0.7F * lift, 0.15F * (1 - slam));
					paw(g, -1, w, -1.3F * (1 - slam) - 0.7F * lift, 0.15F * (1 - slam));
					g.headX -= w * 0.5F * (1 - slam);
					g.jaw += w * 0.5F;
					g.shake += w * (wave < GiantRules.Matriarch.ICICLE_WAVES ? decay(k % every, 4) : 0);
				}
			}
			case GiantRules.Matriarch.ROAR -> {
				// Reared back roaring, shaking all over.
				float r = phase(s, 6) * (1 - phase(s - (GiantRules.Matriarch.ROAR_TICKS - 10), 10));
				g.rear -= w * 0.4F * r;
				g.headX -= w * 0.6F * r;
				g.jaw += w * r;
				g.roll += w * r * Mth.sin(t * 1.7F) * 0.06F;
				g.shake += w * r;
			}
			default -> {}
		}
	}

	/** Adds to a forepaw (1 her right): raised forward by {@code x}, and out to the side by {@code z} (mirrored for the left). */
	private static void paw(Giant g, int side, float w, float x, float z) {
		if (side > 0) {
			g.frX += w * x;
			g.frZ += w * z;
		} else {
			g.flX += w * x;
			g.flZ -= w * z;
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

	/**
	 * 0.13: the giant's moves summed: how far she crouches, leaps, sprawls and slinks (for the pose above), and what's laid over
	 * it: reared up about her hips or sat back about her shoulders, twist, roll and sink, her head's nod and turn (and how far it
	 * stops following you), jaw, tail, forepaws, hind legs, shaking and ears.
	 */
	private static final class Giant {
		float crouch, leap, sprawl, slink;
		float rear, sit, twist, roll, sink, headX, headY, lock, jaw, tailX, tailY, frX, frZ, flX, flZ, backX, shake, flat;

		void clear() {
			crouch = leap = sprawl = slink = 0;
			rear = sit = twist = roll = sink = headX = headY = lock = jaw = tailX = tailY = 0;
			frX = frZ = flX = flZ = backX = shake = flat = 0;
		}
	}
}
