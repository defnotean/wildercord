package dev.wildercord.aura.world;

import dev.wildercord.aura.world.MasterAnimationRules.Joint;
import dev.wildercord.aura.world.MasterAnimationRules.Pose;

/**
 * Classic (rigid rig) choreography for the Tide, Iron and Dune signatures. Each form is a short key list on the
 * executor's own clock, so every authored beat lands on the same tick as the server's strike.
 */
public final class MethodsAMasterAnimation {
	private MethodsAMasterAnimation() {}

	/** Wire ids follow the moves' ordinals, so they stay right wherever the moves end up after other packs append theirs. */
	public static final int TIDE_UNDERTOW_RING = MastersRules.Move.TIDE_UNDERTOW_RING.ordinal() + 1,
		IRON_ANVIL_VERDICT = MastersRules.Move.IRON_ANVIL_VERDICT.ordinal() + 1,
		DUNE_SHIFTING_SANDS = MastersRules.Move.DUNE_SHIFTING_SANDS.ordinal() + 1;

	public static boolean owns(int attack) {
		return attack == TIDE_UNDERTOW_RING || attack == IRON_ANVIL_VERDICT || attack == DUNE_SHIFTING_SANDS;
	}

	private record Key(float at, Pose pose) {}

	private static final Pose[] SWEEP = MasterAnimationRules.keys(MasterAnimationRules.SWEEP);
	private static final Pose[] THRUST = MasterAnimationRules.keys(MasterAnimationRules.THRUST);
	private static final Pose[] KILN = MasterAnimationRules.keys(MasterAnimationRules.KILN_RING);
	private static final Pose[] FRACTURE = MasterAnimationRules.keys(MasterAnimationRules.STONE_FRACTURE);
	private static final float TURN = -(float) (Math.PI * 2);

	// Tide: the blade swings out in a full turn as the wave breaks, then is drawn back low to the feet as the undertow pulls.
	private static final Key[] TIDE = {
		new Key(0, KILN[0]), new Key(MethodsASignatureRules.WAVE - 5, KILN[0]),
		new Key(MethodsASignatureRules.WAVE, spun(KILN[1])), new Key(MethodsASignatureRules.WAVE + 3, spun(KILN[2])),
		new Key(MethodsASignatureRules.UNDERTOW - 6, lowered(SWEEP[0])), new Key(MethodsASignatureRules.UNDERTOW, lowered(SWEEP[1])),
		new Key(MethodsASignatureRules.UNDERTOW + 3, lowered(SWEEP[2]))};

	// Iron: raised overhead like a smith's hammer, brought down on the spot, then the pommel driven into the ground.
	private static final Key[] IRON = {
		new Key(0, FRACTURE[0]), new Key(MethodsASignatureRules.HAMMER - 4, raised(FRACTURE[0])),
		new Key(MethodsASignatureRules.HAMMER, FRACTURE[1]), new Key(MethodsASignatureRules.HAMMER + 3, FRACTURE[2]),
		new Key(MethodsASignatureRules.SHOCK - 6, raised(FRACTURE[0])), new Key(MethodsASignatureRules.SHOCK, lowered(FRACTURE[1])),
		new Key(MethodsASignatureRules.SHOCK + 3, lowered(FRACTURE[2]))};

	// Dune: a long low thrust sends the sand down the lane, then a full turn whips the storm round everything else.
	private static final Key[] DUNE = {
		new Key(0, lowered(THRUST[0])), new Key(MethodsASignatureRules.LANE - 4, lowered(THRUST[0])),
		new Key(MethodsASignatureRules.LANE, THRUST[1]), new Key(MethodsASignatureRules.LANE + 3, THRUST[2]),
		new Key(MethodsASignatureRules.STORM - 6, KILN[0]), new Key(MethodsASignatureRules.STORM, spun(KILN[1])),
		new Key(MethodsASignatureRules.STORM + 3, spun(KILN[2]))};

	/** Null for any attack this pack does not author; otherwise the eased pose, or NONE outside its timeline. */
	public static Pose sample(int attack, float age, int tell, int active, int recovery) {
		Key[] keys = attack == TIDE_UNDERTOW_RING ? TIDE : attack == IRON_ANVIL_VERDICT ? IRON : attack == DUNE_SHIFTING_SANDS ? DUNE : null;
		if (keys == null) return null;
		int end = tell + active + recovery;
		if (!Float.isFinite(age) || age < 0 || tell < 1 || tell > 80 || active < 1 || active > 10
			|| recovery < 1 || recovery > 120 || age >= end) return MasterAnimationRules.NONE;
		Pose pose = keys[keys.length - 1].pose();
		for (int i = 0; i + 1 < keys.length; i++) if (age < keys[i + 1].at()) {
			Key from = keys[i], to = keys[i + 1];
			pose = from.pose().toward(to.pose(), smooth((age - from.at()) / Math.max(1, to.at() - from.at())));
			break;
		}
		float last = keys[keys.length - 1].at();
		float weight = smooth(age / 4) * (1 - smooth((age - last) / Math.max(1, end - last)));
		return pose.weight(weight);
	}

	/** The beat ticks the classic form strikes on, matching {@link MethodsASignatureRules#beats}. */
	public static float[] beats(int attack) {
		MastersRules.Move move = attack == TIDE_UNDERTOW_RING ? MastersRules.Move.TIDE_UNDERTOW_RING
			: attack == IRON_ANVIL_VERDICT ? MastersRules.Move.IRON_ANVIL_VERDICT
			: attack == DUNE_SHIFTING_SANDS ? MastersRules.Move.DUNE_SHIFTING_SANDS : null;
		if (move == null) return new float[0];
		int[] beats = MethodsASignatureRules.beats(move);
		float[] out = new float[beats.length];
		for (int i = 0; i < beats.length; i++) out[i] = beats[i];
		return out;
	}

	private static Pose raised(Pose p) {
		return new Pose(p.weight(), new Joint(p.body().x() - .10F, p.body().y(), p.body().z()), new Joint(p.head().x() - .15F, p.head().y(), p.head().z()),
			new Joint(Math.max(-3.0F, p.sword().x() - .45F), p.sword().y(), p.sword().z()), p.offhand(), p.stance(), p.bladeTilt(), p.rootYaw());
	}
	private static Pose lowered(Pose p) {
		return new Pose(p.weight(), new Joint(p.body().x() + .30F, p.body().y(), p.body().z()), new Joint(p.head().x() - .20F, p.head().y(), p.head().z()),
			new Joint(p.sword().x() * .55F, p.sword().y(), p.sword().z()), p.offhand(), p.stance() + .10F, p.bladeTilt(), p.rootYaw());
	}
	private static Pose spun(Pose p) {
		return new Pose(p.weight(), p.body(), p.head(), p.sword(), p.offhand(), p.stance(), p.bladeTilt(), TURN);
	}

	private static float smooth(float t) {
		t = Float.isFinite(t) ? Math.max(0, Math.min(1, t)) : 0;
		return t * t * (3 - 2 * t);
	}
}
