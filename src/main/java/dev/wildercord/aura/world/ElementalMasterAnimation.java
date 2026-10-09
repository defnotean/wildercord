package dev.wildercord.aura.world;

import dev.wildercord.aura.world.MasterAnimationRules.Joint;
import dev.wildercord.aura.world.MasterAnimationRules.Pose;

/**
 * Classic (rigid humanoid) choreography for the Rime, Thunder, Verdant and Hollow signatures. Each clip follows the
 * executor's own clock: it chambers before every resolving beat, lands on the beat and recoils, so a multi-pulse
 * signature reads as three visible commitments rather than one stretched swing. Angles are radians; tilt is degrees.
 */
public final class ElementalMasterAnimation {
	private ElementalMasterAnimation() {}

	public static final int RIME_LATTICE = 12, THUNDER_CHAIN = 13, VERDANT_BLOOM = 14, HOLLOW_PULL = 15;

	/** chamber, impact and follow for one beat. */
	private record Clip(Pose chamber, Pose impact, Pose follow) {}

	// Rime brushes the blade tip across the ground to mark each plus, then flicks it up flat as the lattice freezes.
	private static final Clip RIME = new Clip(
		p(j(.30F, .20F, 0), j(-.14F, 0, 0), j(-.35F, .55F, -.10F), j(-.90F, -.30F, .40F), .30F, 70),
		p(j(.05F, -.25F, 0), j(.02F, 0, 0), j(-1.90F, -.40F, -.25F), j(-1.20F, .20F, .30F), .26F, -30),
		p(j(.12F, -.10F, 0), j(-.04F, 0, 0), j(-1.20F, -.10F, -.20F), j(-.80F, .10F, .30F), .24F, 10));
	// Thunder holds the blade straight up as a lightning rod, then points it at each conductor in turn.
	private static final Clip THUNDER = new Clip(
		p(j(-.12F, 0, 0), j(.12F, 0, 0), j(-2.95F, 0, -.05F), j(-.40F, .10F, .40F), .22F, 0),
		p(j(.18F, .10F, 0), j(-.06F, 0, 0), j(-1.55F, .15F, 0), j(-.30F, -.20F, .30F), .26F, 85),
		p(j(.10F, -.12F, 0), j(-.02F, 0, 0), j(-2.20F, -.10F, -.05F), j(-.35F, .10F, .35F), .24F, 40));
	// Verdant raises the blade two-handed like a seedling, then drives its point into the earth as the ring blooms.
	private static final Clip VERDANT = new Clip(
		p(j(.04F, 0, 0), j(0, 0, 0), j(-1.30F, .20F, -.10F), j(-1.25F, -.25F, .10F), .20F, 90),
		p(j(.45F, 0, 0), j(-.20F, 0, 0), j(-.55F, .10F, -.05F), j(-.60F, -.10F, .10F), .34F, 150),
		p(j(.38F, 0, 0), j(-.16F, 0, 0), j(-.62F, .08F, -.05F), j(-.66F, -.08F, .10F), .32F, 140));
	// Hollow reaches out with the open off hand and draws the well in, then closes it with a crossing cut.
	private static final Clip HOLLOW = new Clip(
		p(j(.12F, -.30F, 0), j(-.06F, .20F, 0), j(-.90F, .70F, .30F), j(-1.60F, -.10F, -.20F), .30F, -20),
		p(j(.28F, .35F, 0), j(-.10F, -.20F, 0), j(-1.20F, -.80F, -.40F), j(-.70F, .30F, .30F), .30F, -80),
		p(j(.20F, .25F, 0), j(-.08F, -.12F, 0), j(-.95F, -.60F, -.30F), j(-.60F, .20F, .30F), .28F, -60));

	public static boolean owns(int attack) { return attack >= RIME_LATTICE && attack <= HOLLOW_PULL; }

	/** The signature's pose at {@code age} ticks after admission; NONE outside its accepted timeline. */
	public static Pose sample(int attack, float age) {
		if (!owns(attack) || !Float.isFinite(age) || age < 0) return MasterAnimationRules.NONE;
		MastersRules.Move move = MastersRules.Move.values()[attack - 1];
		int end = ElementalMasters.end(move);
		if (age >= end) return MasterAnimationRules.NONE;
		Clip clip = switch (attack) {
			case RIME_LATTICE -> RIME;
			case THUNDER_CHAIN -> THUNDER;
			case VERDANT_BLOOM -> VERDANT;
			default -> HOLLOW;
		};
		int[] beats = ElementalMasters.beats(move);
		Pose pose = beat(clip, age, beats);
		if (attack == THUNDER_CHAIN && age >= beats[0]) {
			// Each later rod sits to one side: the pointing torso swings across to it.
			float swing = .35F * smooth((age - beats[0] - 2) / Math.max(1, beats[1] - beats[0] - 2))
				- .70F * smooth((age - beats[1] - 2) / Math.max(1, beats[2] - beats[1] - 2));
			pose = new Pose(1, new Joint(pose.body().x(), pose.body().y() + swing, pose.body().z()), pose.head(), pose.sword(),
				pose.offhand(), pose.stance(), pose.bladeTilt(), pose.rootYaw());
		}
		int last = beats[beats.length - 1];
		float enter = smooth(age / Math.max(1, beats[0] * .3F));
		float leave = 1 - smooth((age - (last + 3)) / Math.max(1, end - last - 3));
		return pose.weight(enter * leave);
	}

	/** Chamber before each beat, land on it, recoil for three ticks, then re-chamber for the next. */
	private static Pose beat(Clip clip, float age, int[] beats) {
		float last = -1;
		for (int beat : beats) {
			if (age < beat + 3) {
				float strike = beat - Math.min(6, (beat - Math.max(0, last)) * .4F);
				if (age >= beat) return clip.impact.toward(clip.follow, smooth((age - beat) / 3));
				if (age >= strike) return clip.chamber.toward(clip.impact, smooth((age - strike) / (beat - strike)));
				if (last < 0) return clip.chamber;
				return clip.follow.toward(clip.chamber, smooth((age - last - 3) / Math.max(1, strike - last - 3)));
			}
			last = beat;
		}
		return clip.follow;
	}

	private static float smooth(float value) { float t = Math.max(0, Math.min(1, value)); return t * t * (3 - 2 * t); }
	private static Joint j(float x, float y, float z) { return new Joint(x, y, z); }
	private static Pose p(Joint body, Joint head, Joint sword, Joint offhand, float stance, float tilt) {
		return new Pose(1, body, head, sword, offhand, stance, tilt, 0);
	}
}
