package dev.wildercord.client.render;

import static dev.wildercord.cast.StarEaterRules.CAGE;
import static dev.wildercord.cast.StarEaterRules.GATHER;
import static dev.wildercord.cast.StarEaterRules.GAZE;
import static dev.wildercord.cast.StarEaterRules.GROUNDED;
import static dev.wildercord.cast.StarEaterRules.HORIZON;
import static dev.wildercord.cast.StarEaterRules.LANCE;
import static dev.wildercord.cast.StarEaterRules.LASH;
import static dev.wildercord.cast.StarEaterRules.LASH_RETELL;
import static dev.wildercord.cast.StarEaterRules.LASH_SPIN;
import static dev.wildercord.cast.StarEaterRules.NOVA;
import static dev.wildercord.cast.StarEaterRules.PLUNGE;
import static dev.wildercord.cast.StarEaterRules.STARFALL;
import static dev.wildercord.cast.StarEaterRules.VOLLEY;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Star-Eater: a floating knot of void with one great eye, a crown of four spikes, four tendrils
 * trailing under it, and six star shards circling it. Its shield up, the shards whirl in close and
 * fast; broken, they drift far out and sag, slow; between phases they spin round it like the rings of
 * a gyroscope; as it dies they fall in on it and the knot shrinks away to nothing.
 *
 * <p>Each of its moves has its own shape. It fans its shards before its eye to loose them, folds them
 * into a spear, streams them behind it as it drops, lies sprawled with them strewn about the floor,
 * spins them flat and wide to send out its rings, sets them in a halo behind its staring eye, flings
 * them off to cage its prey, raises them in a crown to call down the stars, draws them in tight as it
 * swallows the light, reaches out to call them home, and curls its tendrils up to whip them round.</p>
 *
 * <p>Laid out on a 64x64 skin (see {@code star_eater_texture} in {@code tools/dungeon_assets.py}):</p>
 * <pre>
 *   core (0,0) 12x12x12    shard (48,0) 3x6x3    spike (48,10) 2x6x2
 *   tendril (0,24) 2x8x2   tendril tip (8,24) 2x7x2
 * </pre>
 */
public class StarEaterModel extends EntityModel<DungeonBossRenderState> {
	private static final int SHARDS = 6;
	private static final int TENDRILS = 4;
	private static final float HALF_PI = Mth.HALF_PI;

	private final ModelPart core;
	private final ModelPart orbit;
	private final ModelPart[] shards = new ModelPart[SHARDS];
	private final ModelPart[] spikes = new ModelPart[4];
	private final ModelPart[] tendrils = new ModelPart[TENDRILS];
	private final ModelPart[] tips = new ModelPart[TENDRILS];

	// Where each shard sits this frame, worked out move by move before it is set.
	private final float[] px = new float[SHARDS];
	private final float[] py = new float[SHARDS];
	private final float[] pz = new float[SHARDS];
	private final float[] rx = new float[SHARDS];
	private final float[] ry = new float[SHARDS];
	private final float[] rz = new float[SHARDS];
	private final float[] sc = new float[SHARDS];

	// And each tendril: its swing forward and back, its swing side to side, and its tip's curl both ways.
	private final float[] tx = new float[TENDRILS];
	private final float[] tz = new float[TENDRILS];
	private final float[] kx = new float[TENDRILS];
	private final float[] kz = new float[TENDRILS];

	public StarEaterModel(ModelPart root) {
		super(root);
		this.core = root.getChild("core");
		this.orbit = root.getChild("orbit");
		for (int i = 0; i < SHARDS; i++) {
			shards[i] = orbit.getChild("shard_" + i);
		}
		for (int i = 0; i < 4; i++) {
			spikes[i] = core.getChild("spike_" + i);
		}
		for (int i = 0; i < TENDRILS; i++) {
			tendrils[i] = core.getChild("tendril_" + i);
			tips[i] = tendrils[i].getChild("tip");
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F),
			PartPose.offset(0.0F, 11.0F, 0.0F));
		// A crown of spikes, leaning out.
		for (int i = 0; i < 4; i++) {
			float x = i % 2 == 0 ? -3.5F : 3.5F;
			float z = i < 2 ? -3.5F : 3.5F;
			core.addOrReplaceChild("spike_" + i, CubeListBuilder.create().texOffs(48, 10).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(x, -5.0F, z, z < 0 ? -0.35F : 0.35F, 0.0F, x < 0 ? 0.35F : -0.35F));
		}
		// Tendrils trailing beneath, in two joints.
		for (int i = 0; i < TENDRILS; i++) {
			float x = i % 2 == 0 ? -3.0F : 3.0F;
			float z = i < 2 ? -3.0F : 3.0F;
			PartDefinition tendril = core.addOrReplaceChild("tendril_" + i, CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
				PartPose.offset(x, 5.0F, z));
			tendril.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(8, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
				PartPose.offset(0.0F, 7.5F, 0.0F));
		}
		// The shards of its shield; placed each frame.
		PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offset(0.0F, 11.0F, 0.0F));
		for (int i = 0; i < SHARDS; i++) {
			orbit.addOrReplaceChild("shard_" + i, CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(DungeonBossRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float c = state.casting;
		float e = state.exposed;
		float sh = state.shifting;
		float dying = Math.min(1, state.deathTime / 50.0F);
		float pitch = state.extra[0];
		float eye = state.extra[1];
		float limp = state.extra[2];
		float loosed = state.extra[3];
		float dark = state.extra[4];
		float[] m = state.moves;
		float awake = 1 - limp;

		// The knot bobs and turns slowly, its eye tilted where it looks. Folding its spear it draws back from it; calling the stars it lifts;
		// fallen or hanging limp it sinks and sags.
		float lanceDraw = m[LANCE] * loosed;
		float rise = m[STARFALL] * wound(state, STARFALL);
		float bob = Mth.sin(t * 0.07F) * 1.2F * awake;
		core.y = 11.0F + bob + e * 1.5F * awake + limp * 6.0F - rise * 2.0F;
		core.z = lanceDraw * 2.5F;
		core.xRot = (Mth.sin(t * 0.05F) * 0.08F + e * 0.35F) * awake + pitch * Mth.DEG_TO_RAD;
		core.zRot = Mth.cos(t * 0.04F) * 0.06F * awake + sh * Mth.sin(t * 1.4F) * 0.08F + limp * 0.25F;
		core.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;

		// Its eye charging, it swells, and staring it throbs; swallowing the light, it shrinks in on itself; dying, it shrinks to nothing.
		float swell = 1.0F + 0.08F * c * Mth.sin(t * 0.9F) + 0.1F * sh + 0.12F * eye + 0.05F * m[GAZE] * eye * Mth.sin(t * 1.3F);
		float scale = swell * (1 - 0.35F * dark) * (1 - 0.85F * dying);
		core.xScale = scale * (1 + 0.1F * limp);
		core.yScale = scale * (1 - 0.2F * limp);
		core.zScale = scale * (1 + 0.1F * limp);

		// Its crown: flared wide between phases and as it spins out its rings, drawn close as it folds its spear or swallows the light,
		// standing up tall to call the stars, and quivering as it gathers itself.
		float flare = sh + 0.8F * m[NOVA] * wound(state, NOVA) - 0.5F * lanceDraw - 0.7F * rise + 0.5F * limp - 0.6F * m[HORIZON] * dark
			+ 0.3F * m[GAZE] * eye;
		float quiver = sh + m[GATHER] * (1 - thrown(state, GATHER));
		for (int i = 0; i < 4; i++) {
			float x = i % 2 == 0 ? -1 : 1;
			float z = i < 2 ? -1 : 1;
			spikes[i].xRot = z * 0.35F * (1 + flare) + quiver * Mth.sin(t * 2.1F + i) * 0.06F;
			spikes[i].zRot = -x * 0.35F * (1 + flare);
			spikes[i].yScale = 1 + 0.5F * rise + 0.25F * sh;
		}

		poseTendrils(state, t, limp, loosed, eye, dark);
		poseShards(state, t, loosed, dark, dying);
	}

	private void poseTendrils(DungeonBossRenderState state, float t, float limp, float loosed, float eye, float dark) {
		float[] m = state.moves;
		float e = state.exposed;
		float sh = state.shifting;
		for (int i = 0; i < TENDRILS; i++) {
			// At rest they trail and sway.
			float ph = t * 0.09F + i * 1.6F;
			tx[i] = Mth.sin(ph) * 0.25F + (i < 2 ? 0.2F : -0.2F) * (1 + e);
			tz[i] = Mth.cos(ph * 0.8F) * 0.2F;
			kx[i] = Mth.sin(ph - 0.8F) * 0.45F;
			kz[i] = 0;
			float tremble = Mth.sin(t * 2.4F + i * 1.7F) * 0.08F;

			// Fanning its shards it draws them back, and flicks them forward with each one it looses.
			swing(i, m[VOLLEY], 0.5F * wound(state, VOLLEY) - loosed * 0.9F, tz[i], 0.3F);
			// Folding its spear, swept back hard.
			swing(i, m[LANCE], 0.1F + 0.8F * loosed, tz[i] * 0.5F, 0.5F * loosed);
			// Dropping, bundled tight and streaming behind it.
			splay(i, m[PLUNGE], -0.45F, 0);
			// Spinning out its rings, spread like spokes, kicking out with each ring.
			splay(i, m[NOVA], 1.25F * wound(state, NOVA) + 0.3F * eye, 0);
			// Staring, spread wide and back, trembling as its eye opens.
			splay(i, m[GAZE], 0.9F, -0.2F);
			tx[i] += m[GAZE] * (0.35F * thrown(state, GAZE) + tremble * (1 - thrown(state, GAZE)));
			// Casting its cage, reaching out after its shards.
			swing(i, m[CAGE], -0.9F * loosed, tz[i] * 0.5F, -0.4F * loosed);
			// Calling the stars, spread and curled up, shaking.
			splay(i, m[STARFALL] * wound(state, STARFALL), 0.7F, 0.4F);
			tx[i] += m[STARFALL] * tremble * 0.6F;
			// Swallowing the light, curled in tight under it.
			splay(i, m[HORIZON] * Math.max(dark, wound(state, HORIZON)), -0.4F, 1.4F);
			// Calling its shards home, reaching out wide and trembling, then closing like a fist as they come.
			float gw = wound(state, GATHER), gg = thrown(state, GATHER);
			splay(i, m[GATHER], 1.5F * gw * (1 - 0.7F * gg), -0.3F + 0.9F * gg);
			tx[i] += m[GATHER] * tremble * (1 - gg);
			lash(state, i);
			// Between phases, flung out round it and shaking.
			splay(i, sh, 1.6F, -0.2F);
			tx[i] += sh * tremble * 1.2F;
			// Limp, sprawled flat out on the floor.
			splay(i, limp, 1.35F, -0.35F);

			tendrils[i].xRot = tx[i];
			tendrils[i].zRot = tz[i];
			tips[i].xRot = kx[i];
			tips[i].zRot = kz[i];
		}
	}

	/** Its whip: curled up and back over its tell, flung flat out through the spin (twice in its last phase), then let fall limp. */
	private void lash(DungeonBossRenderState state, int i) {
		float b = state.moves[LASH];
		if (b <= 0) {
			return;
		}
		float s = state.move == LASH ? state.since : Float.MAX_VALUE;
		int tell = state.tell;
		int first = tell + LASH_SPIN;
		int second = first + 1 + LASH_RETELL;
		boolean twice = state.phase >= 3;
		float reach;
		float curl;
		if (s < tell) {
			float k = Mth.clamp(s / Math.max(1, tell), 0, 1);
			reach = 2.4F * k;
			curl = 1.4F * k;
		} else if (s <= first) {
			reach = 1.9F;
			curl = 0;
		} else if (twice && s < second) {
			float k = Mth.clamp((s - first - 1) / LASH_RETELL, 0, 1);
			reach = Mth.lerp(k, 1.9F, 2.4F);
			curl = 1.4F * k;
		} else if (twice && s <= second + LASH_SPIN) {
			reach = 1.9F;
			curl = 0;
		} else {
			reach = 1.35F;
			curl = -0.35F;
		}
		splay(i, b, reach, curl);
	}

	private void poseShards(DungeonBossRenderState state, float t, float loosed, float dark, float dying) {
		float[] m = state.moves;
		float c = state.casting;
		float g = state.guarded;
		float e = state.exposed;
		float sh = state.shifting;
		float spin = state.extra[5];
		orbit.y = core.y;
		orbit.yRot = 0;
		float floor = 23.0F - orbit.y;
		float face = core.yRot;
		float cosF = Mth.cos(face);
		float sinF = Mth.sin(face);
		// Whirling close with its shield up, drifting wide and low with it broken, drawn in as it swallows the light.
		float radius = Mth.lerp(g, 20.0F, 12.5F) + 6.0F * e;
		radius = radius * (1 - dying) * (1 - 0.45F * dark);
		float home = state.move == GATHER ? Mth.clamp((state.since - state.tell) / 20.0F, 0, 1) : 1;
		for (int i = 0; i < SHARDS; i++) {
			float at = Mth.TWO_PI * i / SHARDS + spin;
			float tilt = Mth.sin(t * 0.05F + i) * (0.35F + 0.4F * e);
			px[i] = Mth.sin(at) * radius;
			pz[i] = Mth.cos(at) * radius;
			py[i] = Mth.sin(t * 0.08F + i * 1.3F) * 2.0F + tilt * 4.0F + 7.0F * e + dying * 6.0F;
			rx[i] = tilt + c * Mth.sin(t * 0.5F + i) * 0.3F;
			ry[i] = at + t * 0.1F;
			rz[i] = Mth.cos(t * 0.07F + i) * 0.3F;
			sc[i] = 1;

			// A fan before its eye, pointed at its prey; each one it looses is gone until the volley is over.
			float lx = (i - 2.5F) * 4.2F;
			float lz = -14.0F + Math.abs(i - 2.5F) * 1.2F;
			shard(i, m[VOLLEY] * wound(state, VOLLEY), cosF * lx + sinF * lz, -Math.abs(i - 2.5F) * 0.9F, -sinF * lx + cosF * lz, HALF_PI, face, 0, sc[i]);
			sc[i] = Mth.lerp(m[VOLLEY], sc[i], 1 - Mth.clamp(loosed * SHARDS - i, 0, 1));
			// Folded end to end into one long spear before its eye.
			lz = -9.0F - i * 3.2F;
			shard(i, m[LANCE] * loosed, sinF * lz, 0, cosF * lz, HALF_PI, face, 0, 1);
			// Dropping, streaming above and behind it like a falling star's tail.
			float tail = 2.5F;
			shard(i, m[PLUNGE] * wound(state, PLUNGE), Mth.sin(at) * tail, -8.0F - i * 3.0F, Mth.cos(at) * tail + 3.0F, 0, ry[i], 0, 1 - i * 0.1F);
			// Fallen, strewn flat about the floor round it.
			float lie = Mth.TWO_PI * i / SHARDS + 0.4F;
			shard(i, m[GROUNDED] * loosed, Mth.sin(lie) * 16.0F, floor, Mth.cos(lie) * 16.0F, HALF_PI, lie, 0, 1);
			// Spun flat and wide in a low ring, spreading as it sends its rings out.
			float wide = 14.0F + 10.0F * loosed + 3.0F * state.extra[1];
			shard(i, m[NOVA], Mth.sin(at) * wide, 6.0F, Mth.cos(at) * wide, HALF_PI, at + HALF_PI, 0, 1);
			// A halo of rays behind its staring eye.
			float ray = Mth.TWO_PI * i / SHARDS + t * 0.03F;
			float hx = Mth.sin(ray) * 12.0F;
			shard(i, m[GAZE] * wound(state, GAZE), cosF * hx + sinF * 8.0F, -Mth.cos(ray) * 12.0F, -sinF * hx + cosF * 8.0F, 0, face, ray, 1);
			// Flung off to cage its prey, dwindling as they go, growing back once the cage has shut.
			float far = radius + 20.0F * loosed;
			shard(i, m[CAGE], Mth.sin(at) * far, py[i], Mth.cos(at) * far, rx[i], ry[i], rz[i], 1 - loosed);
			// A crown raised high overhead to call the stars, dimming once they are called down.
			float crown = 10.0F + 6.0F * loosed;
			shard(i, m[STARFALL], Mth.sin(at) * crown, -14.0F - 6.0F * loosed, Mth.cos(at) * crown, 0, ry[i], 0, 1 - 0.5F * thrown(state, STARFALL));
			// Swallowing the light, a tight flat ring that sinks under it to grind.
			float grind = 7.0F + 3.0F * (1 - dark);
			shard(i, m[HORIZON], Mth.sin(at) * grind, 2.0F + 4.0F * thrown(state, HORIZON), Mth.cos(at) * grind, HALF_PI, at + HALF_PI, 0, 1);
			// Called home from where they lay strewn, low and far, flying in close.
			float back = Mth.lerp(home, 24.0F, 9.0F);
			shard(i, m[GATHER], Mth.sin(at) * back, Mth.lerp(home, floor, 0), Mth.cos(at) * back, Mth.lerp(home, HALF_PI, 0), ry[i], 0, 1);
			// Whipping its tendrils, drawn in tight out of their way; after, let fall to sag low.
			shard(i, m[LASH] * (1 - state.extra[2]), Mth.sin(at) * 9.0F, 3.0F, Mth.cos(at) * 9.0F, rx[i], ry[i], rz[i], 1);
			// Between phases, spun round it like the rings of a gyroscope, one ring flat and one upright.
			if (i % 2 == 0) {
				shard(i, sh, Mth.sin(at) * 8.0F, 0, Mth.cos(at) * 8.0F, HALF_PI, at + HALF_PI, 0, 1);
			} else {
				float up = at * 1.3F;
				float wx = Mth.sin(up) * 8.0F;
				float turnY = spin * 0.3F;
				shard(i, sh, Mth.cos(turnY) * wx, -Mth.cos(up) * 8.0F, -Mth.sin(turnY) * wx, 0, turnY, up, 1);
			}
			// Dying, they fall in on it.
			shard(i, dying, 0, 6.0F, 0, rx[i], ry[i], rz[i], 1 - dying);

			ModelPart s = shards[i];
			s.x = px[i];
			s.y = py[i];
			s.z = pz[i];
			s.xRot = rx[i];
			s.yRot = ry[i];
			s.zRot = rz[i];
			s.xScale = sc[i];
			s.yScale = sc[i];
			s.zScale = sc[i];
			s.visible = sc[i] > 0.02F;
		}
	}

	/** How far into its wind-up a move is: 0 to 1 over its tell, and 1 once it is done and easing away. */
	private static float wound(DungeonBossRenderState state, int move) {
		return state.move == move ? Mth.clamp(state.since / Math.max(1, state.tell), 0, 1) : 1;
	}

	/** How far through the blow itself a move is, over the few ticks after its tell. */
	private static float thrown(DungeonBossRenderState state, int move) {
		return state.move == move ? Mth.clamp((state.since - state.tell) / 3.0F, 0, 1) : 1;
	}

	/** Eases shard {@code i} toward a place and a turn by {@code b}. */
	private void shard(int i, float b, float x, float y, float z, float xr, float yr, float zr, float s) {
		if (b <= 0) {
			return;
		}
		px[i] = Mth.lerp(b, px[i], x);
		py[i] = Mth.lerp(b, py[i], y);
		pz[i] = Mth.lerp(b, pz[i], z);
		rx[i] = Mth.lerp(b, rx[i], xr);
		ry[i] = turn(b, ry[i], yr);
		rz[i] = Mth.lerp(b, rz[i], zr);
		sc[i] = Mth.lerp(b, sc[i], s);
	}

	/** Eases tendril {@code i} toward reaching out from its body by {@code reach} (past a quarter turn, curling up) and its tip toward {@code curl} inward. */
	private void splay(int i, float b, float reach, float curl) {
		if (b <= 0) {
			return;
		}
		float x = i % 2 == 0 ? -1 : 1;
		float z = i < 2 ? -1 : 1;
		tx[i] = Mth.lerp(b, tx[i], z * reach * 0.7F);
		tz[i] = Mth.lerp(b, tz[i], -x * reach * 0.7F);
		kx[i] = Mth.lerp(b, kx[i], -z * curl * 0.7F);
		kz[i] = Mth.lerp(b, kz[i], x * curl * 0.7F);
	}

	/** Eases tendril {@code i} toward swinging {@code fore} (back positive) and {@code side}, its tip curled {@code tip}. */
	private void swing(int i, float b, float fore, float side, float tip) {
		if (b <= 0) {
			return;
		}
		tx[i] = Mth.lerp(b, tx[i], fore);
		tz[i] = Mth.lerp(b, tz[i], side);
		kx[i] = Mth.lerp(b, kx[i], tip);
		kz[i] = Mth.lerp(b, kz[i], 0);
	}

	/** Eases an angle the short way round. */
	private static float turn(float b, float from, float to) {
		float d = (to - from) % Mth.TWO_PI;
		if (d > Mth.PI) {
			d -= Mth.TWO_PI;
		} else if (d < -Mth.PI) {
			d += Mth.TWO_PI;
		}
		return from + d * b;
	}
}
