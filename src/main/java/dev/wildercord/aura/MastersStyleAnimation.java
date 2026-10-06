package dev.wildercord.aura;

import static dev.wildercord.aura.MastersArtAnimation.*;

/**
 * Original first-form choreographies and selected second-form releases. These change
 * stance, blade path, rhythm and recovery, rather than recolouring a shared swing. Their IDs are
 * presentation-only and match {@link MastersStyleRules}; no new input or damage rule lives here.
 */
public final class MastersStyleAnimation {
	private MastersStyleAnimation() {}

	// Ember draws from the low rear hip, cuts across the knees, then lets the trailing arm open.
	private static final Motion KINDLING = new Motion(
		pose(j(.22F, .58F, -.12F), j(-.10F, -.25F, 0), j(-.35F, 1.05F, .75F), j(-.55F, -.25F, -.35F),
			j(-.65F, -.12F, -.12F), j(.35F, .15F, .10F), .9F, .15F, h(.18F, .12F, .05F, 35, 52, -55)),
		pose(j(.32F, -.68F, .15F), j(-.16F, .20F, -.08F), j(-.85F, -1.15F, -.35F), j(.15F, -.10F, -.90F),
			j(-.75F, .12F, -.08F), j(.40F, -.12F, .08F), .8F, -.9F, h(-.42F, .08F, -.25F, 40, -55, 40)),
		pose(j(.18F, -.82F, .12F), j(-.08F, .30F, 0), j(-.25F, -1.30F, .10F), j(.28F, -.15F, -1.0F),
			j(-.40F, .15F, -.08F), j(.28F, -.12F, .08F), .5F, -.5F, h(-.50F, .10F, -.05F, 52, -62, 52)));

	// Rime stays upright and compact: a measured horizontal cut, then a precise closed guard.
	private static final Motion FROSTBITE = new Motion(
		pose(j(.02F, .30F, 0), j(0, -.12F, 0), j(-1.30F, .65F, -.15F), j(-1.10F, -.35F, .10F),
			j(-.22F, -.06F, -.05F), j(.15F, .05F, .05F), .15F, 0, h(.10F, .02F, .14F, -15, 25, -12)),
		pose(j(.05F, -.28F, .03F), j(-.02F, .10F, 0), j(-1.35F, -.75F, -.12F), j(-1.0F, -.10F, .18F),
			j(-.35F, .06F, -.05F), j(.20F, -.05F, .05F), .18F, -.45F, h(-.23F, .0F, -.28F, -5, -32, 18)),
		pose(j(.02F, -.12F, 0), j(0, .05F, 0), j(-1.20F, -.38F, -.12F), j(-1.15F, -.22F, .12F),
			j(-.20F, .04F, -.04F), j(.15F, -.03F, .04F), .10F, -.15F, h(-.12F, -.05F, -.08F, -8, -15, 10)));

	// Thunder's first two cuts reverse without settling; the third beat punches the point forward.
	private static final Motion CRACKLE = new Motion(
		pose(j(.05F, .35F, -.08F), j(0, -.15F, 0), j(-1.55F, .80F, -.40F), j(-1.05F, -.20F, -.25F),
			j(-.30F, -.08F, -.06F), j(.25F, .08F, .06F), .3F, .05F, h(.16F, .08F, .12F, -20, 30, -25)),
		pose(j(.16F, -.45F, .07F), j(-.05F, .15F, 0), j(-1.35F, -.95F, -.30F), j(-.75F, -.20F, -.45F),
			j(-.45F, .08F, -.06F), j(.30F, -.06F, .06F), .25F, -.45F, h(-.26F, -.05F, -.28F, 12, -38, 35)),
		pose(j(.20F, -.20F, .03F), j(-.08F, .08F, 0), j(-1.58F, -.12F, -.08F), j(-.30F, -.15F, -.30F),
			j(-.50F, .05F, -.05F), j(.35F, -.05F, .05F), .3F, -.7F, h(-.05F, .02F, -.50F, -110, -5, 8)));
	private static final Pose CRACKLE_BACK = pose(j(.10F, .35F, -.08F), j(-.03F, -.12F, 0), j(-1.50F, .90F, -.32F),
		j(-.85F, -.20F, .20F), j(-.35F, -.08F, -.06F), j(.25F, .08F, .06F), .25F, -.35F, h(.24F, .03F, -.24F, -10, 38, -32));

	// Gale turns through an open stance and long, level draw; its free arm counterbalances the blade.
	private static final Motion BREEZE = new Motion(
		pose(j(.05F, .75F, -.18F), j(0, -.35F, .05F), j(-1.40F, 1.10F, -.05F), j(-.30F, -.45F, -.80F),
			j(-.45F, -.25F, -.12F), j(.35F, .20F, .12F), .3F, .10F, h(.10F, .12F, -.25F, 10, 45, -30)),
		pose(j(.08F, -.85F, .18F), j(-.02F, .35F, -.06F), j(-1.24F, -1.30F, -.05F), j(-.25F, .35F, -1.05F),
			j(-.50F, .25F, -.14F), j(.38F, -.20F, .12F), .25F, -.6F, h(-.48F, -.10F, -.24F, 18, -60, 40)),
		pose(j(.03F, -.95F, .12F), j(0, .40F, -.03F), j(-1.24F, -1.40F, -.05F), j(.10F, .25F, -.85F),
			j(-.25F, .20F, -.10F), j(.20F, -.15F, .08F), .1F, -.3F, h(-.55F, -.20F, -.05F, 28, -68, 48)));

	// Stone lifts both arms over a broad base, drops the blade vertically, and absorbs the stop in its stance.
	private static final Motion ROCKBREAKER = new Motion(
		pose(j(-.18F, .06F, 0), j(.10F, 0, 0), j(-2.80F, .16F, -.22F), j(-2.55F, -.25F, .25F),
			j(-.40F, -.18F, -.18F), j(.35F, .18F, .18F), .4F, .3F, h(.05F, .35F, .22F, -115, 5, -8)),
		pose(j(.65F, -.08F, .03F), j(-.28F, .02F, 0), j(-.40F, -.08F, -.15F), j(-.55F, -.12F, .18F),
			j(-.95F, -.15F, -.20F), j(.55F, .12F, .18F), 1.5F, -1.15F, h(-.03F, .12F, -.42F, 72, -3, 5)),
		pose(j(.45F, -.10F, .02F), j(-.20F, .04F, 0), j(-.15F, -.10F, -.12F), j(-.40F, -.15F, .18F),
			j(-.70F, -.12F, -.16F), j(.40F, .10F, .15F), 1.15F, -.7F, h(-.05F, .16F, -.20F, 82, -5, 6)));

	// Verdant rolls its shoulders through a long lashing cut, then recoils along the same blade path.
	private static final Motion THORN = new Motion(
		pose(j(.05F, .55F, -.25F), j(-.02F, -.20F, .12F), j(-.70F, .95F, -.85F), j(-.45F, -.15F, -.90F),
			j(-.45F, -.14F, -.12F), j(.28F, .15F, .10F), .45F, .20F, h(.10F, -.08F, -.35F, 12, 40, -55)),
		pose(j(.26F, -.55F, .30F), j(-.12F, .20F, -.12F), j(-1.20F, -.85F, -.65F), j(.20F, .15F, -1.20F),
			j(-.65F, .15F, -.15F), j(.40F, -.12F, .12F), .55F, -1.0F, h(-.38F, -.08F, -.42F, 18, -38, 55)),
		pose(j(.10F, -.25F, .18F), j(-.05F, .12F, -.05F), j(-.65F, -.50F, .12F), j(-.25F, -.10F, -.80F),
			j(-.40F, .12F, -.10F), j(.25F, -.10F, .08F), .3F, -.30F, h(-.20F, -.05F, .02F, 25, -20, 35)));

	// Hollow closes around the hilt, slices a narrow seam, then draws the weapon inward again.
	private static final Motion VOID_CUT = new Motion(
		pose(j(.32F, .22F, .06F), j(-.18F, -.08F, 0), j(-1.15F, .45F, -.18F), j(-1.35F, -.45F, .20F),
			j(-.55F, -.08F, -.08F), j(.45F, .08F, .08F), .85F, .20F, h(.08F, -.05F, -.20F, -5, 18, -10)),
		pose(j(.20F, -.35F, -.06F), j(-.10F, .10F, .02F), j(-1.48F, -.60F, -.15F), j(-1.20F, -.30F, .18F),
			j(-.60F, .08F, -.08F), j(.40F, -.06F, .08F), .70F, -.40F, h(-.20F, -.08F, -.35F, -18, -25, 14)),
		pose(j(.28F, -.10F, 0), j(-.15F, .05F, 0), j(-1.15F, -.10F, -.25F), j(-1.35F, -.35F, .25F),
			j(-.45F, .04F, -.08F), j(.35F, -.03F, .08F), .80F, .10F, h(-.02F, -.16F, -.10F, -2, -3, 8)));

	// Starlit aligns the point at eye height, makes a small precise lunge, then returns to that line.
	private static final Motion NEEDLE = new Motion(
		pose(j(-.04F, .22F, -.02F), j(.02F, -.08F, 0), j(-1.55F, .25F, -.07F), j(-.75F, -.20F, -.35F),
			j(-.22F, -.07F, -.04F), j(.25F, .07F, .04F), .12F, .25F, h(.08F, .06F, .20F, -100, 10, -3)),
		pose(j(.22F, -.06F, .02F), j(-.10F, .02F, 0), j(-1.72F, -.02F, -.04F), j(.10F, -.15F, -.50F),
			j(-.65F, .04F, -.05F), j(.40F, -.04F, .05F), .25F, -.95F, h(-.02F, .08F, -.58F, -110, 0, 3)),
		pose(j(.10F, -.08F, .02F), j(-.04F, .03F, 0), j(-1.55F, -.04F, -.05F), j(-.40F, -.18F, -.40F),
			j(-.35F, .03F, -.04F), j(.25F, -.03F, .04F), .12F, -.30F, h(-.02F, .03F, -.20F, -95, -2, 3)));

	// Hourglass cuts once, holds the escaped motion still, then answers its afterimage on the return beat.
	private static final Motion ECHO = new Motion(
		pose(j(.06F, .42F, -.04F), j(-.02F, -.18F, 0), j(-1.10F, .75F, -.25F), j(-1.0F, -.20F, -.30F),
			j(-.32F, -.10F, -.06F), j(.24F, .10F, .06F), .25F, .10F, h(.14F, -.02F, -.20F, -8, 28, -18)),
		pose(j(.12F, -.42F, .05F), j(-.05F, .16F, 0), j(-1.28F, -.82F, -.25F), j(-.75F, -.10F, -.40F),
			j(-.45F, .10F, -.06F), j(.28F, -.08F, .06F), .25F, -.50F, h(-.26F, -.06F, -.28F, 10, -32, 26)),
		pose(j(.08F, -.55F, .04F), j(-.03F, .20F, 0), j(-.80F, -.90F, -.20F), j(-.65F, -.12F, -.40F),
			j(-.35F, .10F, -.06F), j(.25F, -.08F, .06F), .20F, -.25F, h(-.32F, -.16F, -.08F, 24, -38, 32)));
	private static final Pose ECHO_RETURN = pose(j(.08F, .35F, -.04F), j(-.03F, -.12F, 0), j(-1.25F, .65F, -.28F),
		j(-.80F, -.25F, .30F), j(-.35F, -.08F, -.06F), j(.25F, .08F, .06F), .20F, -.30F, h(.22F, -.02F, -.25F, -5, 28, -25));

	// Crimson sinks into an aggressive cross-body slice, extending the off hand as the blade rakes low.
	private static final Motion BLOODLETTING = new Motion(
		pose(j(.35F, .45F, -.20F), j(-.20F, -.15F, .06F), j(-1.60F, .90F, -.90F), j(-.70F, -.20F, -.65F),
			j(-.75F, -.15F, -.14F), j(.50F, .12F, .12F), 1.1F, .10F, h(.10F, .06F, -.10F, -38, 35, -58)),
		pose(j(.45F, -.58F, .25F), j(-.22F, .20F, -.08F), j(-.60F, -1.0F, -.55F), j(-.15F, -.10F, -1.0F),
			j(-.95F, .15F, -.15F), j(.55F, -.12F, .12F), 1.25F, -1.1F, h(-.36F, .10F, -.35F, 60, -42, 58)),
		pose(j(.25F, -.70F, .18F), j(-.12F, .25F, -.04F), j(-.25F, -1.10F, .10F), j(.05F, -.10F, -.80F),
			j(-.60F, .12F, -.12F), j(.38F, -.10F, .10F), .80F, -.55F, h(-.42F, .12F, -.12F, 70, -50, 65)));


	// Cinders scoops from the outside hip across the body, unfurls upward and leaves the blade high.
	// This is a grounded rising cut: the existing art lifts its victims, never the swordsman.
	private static final Motion CINDERS = new Motion(
		pose(j(.26F, .48F, .16F), j(-.12F, -.18F, -.06F), j(.15F, .62F, .55F), j(-.65F, -.18F, -.72F),
			j(-.62F, -.14F, -.12F), j(.38F, .12F, .10F), .72F, .10F, h(.05F, .05F, -.30F, 58, 28, -42)),
		pose(j(-.12F, -.46F, -.16F), j(.04F, .18F, .06F), j(-2.22F, -.72F, -.48F), j(-.35F, .10F, -1.10F),
			j(-.28F, .16F, -.09F), j(.18F, -.10F, .08F), .08F, -.58F, h(-.22F, .26F, -.34F, -72, -26, 25)),
		pose(j(-.08F, -.62F, -.12F), j(.02F, .22F, .04F), j(-2.64F, -.55F, -.65F), j(.08F, .12F, -.82F),
			j(-.25F, .13F, -.08F), j(.16F, -.08F, .07F), .12F, -.26F, h(-.32F, .34F, -.18F, -92, -35, 38)));

	// Blossom gathers over the shoulder, falls diagonally into a planted stance, then lifts the hilt
	// slightly out of the ground-facing finish while the already-released petal field unfolds alone.
	private static final Motion BLOSSOM = new Motion(
		pose(j(-.10F, .32F, -.12F), j(.04F, -.12F, .04F), j(-2.48F, .32F, -.48F), j(-1.25F, -.18F, -.58F),
			j(-.30F, -.12F, -.10F), j(.26F, .10F, .09F), .22F, .18F, h(-.08F, .65F, -.12F, -96, 18, -24)),
		pose(j(.38F, -.30F, .14F), j(-.18F, .12F, -.04F), j(-.62F, -.24F, -.20F), j(-.20F, .10F, -.88F),
			j(-.78F, .12F, -.12F), j(.42F, -.10F, .10F), .92F, -.86F, h(-.16F, .20F, -.46F, 64, -12, 20)),
		pose(j(.25F, -.26F, .10F), j(-.12F, .10F, -.02F), j(-.45F, -.20F, -.18F), j(-.32F, -.08F, -.62F),
			j(-.56F, .10F, -.10F), j(.30F, -.08F, .08F), .60F, -.44F, h(-.12F, .28F, -.40F, 48, -30, 16)));

	// Hailfall opens Rime's folded guard upward once. The cloud's seven drops have their
	// own released lifetime; the swordsman holds the lifted edge, never strikes seven times.
	private static final Motion HAILFALL = new Motion(
		pose(j(.08F, .20F, -.04F), j(-.04F, -.08F, 0), j(-1.04F, .35F, -.18F), j(-1.18F, -.25F, .18F),
			j(-.34F, -.08F, -.07F), j(.22F, .08F, .06F), .38F, .08F, h(.02F, .10F, -.18F, 8, 16, -16)),
		pose(j(-.06F, -.18F, .04F), j(.02F, .08F, -.02F), j(-2.02F, -.32F, -.30F), j(-.85F, -.12F, .30F),
			j(-.25F, .08F, -.06F), j(.18F, -.06F, .06F), .18F, -.32F, h(-.12F, .30F, -.30F, -62, -14, 22)),
		pose(j(-.04F, -.22F, .03F), j(.02F, .10F, -.01F), j(-2.32F, -.30F, -.35F), j(-1.04F, -.18F, .20F),
			j(-.24F, .06F, -.06F), j(.17F, -.05F, .05F), .20F, -.18F, h(-.16F, .36F, -.22F, -78, -20, 28)));

	// Skyfall holds a tall lightning-rod line at release. Its later short answer belongs
	// to the already released bolt, not a second physical attack or a new target query.
	private static final Motion SKYFALL = new Motion(
		pose(j(-.08F, .12F, -.08F), j(-.08F, -.04F, .03F), j(-2.26F, .18F, -.32F), j(-.75F, -.25F, -.42F),
			j(-.28F, -.12F, -.09F), j(.24F, .10F, .08F), .26F, .12F, h(.10F, .34F, -.22F, -82, 14, -18)),
		pose(j(-.12F, -.04F, -.05F), j(-.06F, .02F, .02F), j(-2.60F, .06F, -.26F), j(-.48F, -.16F, -.58F),
			j(-.36F, -.10F, -.10F), j(.28F, .08F, .09F), .34F, -.10F, h(.08F, .42F, -.28F, -100, 6, -10)),
		pose(j(.06F, -.10F, .02F), j(-.03F, .04F, 0), j(-1.35F, -.14F, -.22F), j(-.84F, -.18F, -.30F),
			j(-.28F, -.08F, -.08F), j(.20F, .06F, .07F), .38F, -.12F, h(.02F, .16F, -.24F, -28, -8, 10)));
	private static final Pose SKYFALL_ANSWER = pose(j(.16F, -.12F, .03F), j(-.08F, .05F, -.01F),
		j(-1.12F, -.18F, -.20F), j(-.35F, -.18F, -.62F), j(-.48F, -.10F, -.10F), j(.32F, .08F, .09F),
		.62F, -.38F, h(.02F, .12F, -.40F, 18, -10, 14));

	static Motion motion(int id) {
		return switch (id) {
			case 3 -> KINDLING;
			case 4 -> FROSTBITE;
			case 5 -> CRACKLE;
			case 6 -> BREEZE;
			case 7 -> ROCKBREAKER;
			case 8 -> THORN;
			case 9 -> VOID_CUT;
			case 10 -> NEEDLE;
			case 11 -> ECHO;
			case 12 -> BLOODLETTING;
			case 13 -> CINDERS;
			case 14 -> BLOSSOM;
			case 15 -> HAILFALL;
			case 16 -> SKYFALL;
			default -> null;
		};
	}

	/** The release stays at windup; only this cosmetic answer follows the bolt's shared delay. */
	static Pose skyfall(Motion motion, float age, int windup, int recovery) {
		if (age < windup) return MastersArtAnimation.sample(motion, age, windup, recovery);
		float t = age - windup, answerAt = Math.min(ArtRules.SKYFALL_DELAY, recovery * .65F);
		float holdUntil = answerAt * .5F, followAt = Math.min(recovery * .8F, answerAt + 2);
		Pose frame = t < holdUntil ? motion.impact()
			: t < answerAt ? motion.impact().toward(SKYFALL_ANSWER, smooth((t - holdUntil) / (answerAt - holdUntil)))
			: t < followAt ? SKYFALL_ANSWER.toward(motion.follow(), smooth((t - answerAt) / (followAt - answerAt))) : motion.follow();
		return frame.weight(1 - smooth((t - followAt) / (recovery - followAt)));
	}

	/** Repeated blade releases follow the very same beat constants used by the server performers. */
	static Pose repeated(int id, Motion motion, float age, int windup, int recovery) {
		if (age < windup) return MastersArtAnimation.sample(motion, age, windup, recovery);
		float t = age - windup;
		Pose frame;
		float finalBeat;
		if (id == 5) {
			float gap = ArtRules.CRACKLE_GAP;
			finalBeat = gap * 2;
			if (t < gap) frame = motion.impact().toward(CRACKLE_BACK, smooth(t / gap));
			else if (t < finalBeat) frame = CRACKLE_BACK.toward(motion.follow(), smooth((t - gap) / gap));
			else frame = motion.follow();
		} else {
			finalBeat = ArtRules.ECHO_DELAY;
			if (t < 3) frame = motion.impact().toward(motion.follow(), smooth(t / 3));
			else if (t < finalBeat - 3) frame = motion.follow();
			else if (t < finalBeat) frame = motion.follow().toward(ECHO_RETURN, smooth((t - finalBeat + 3) / 3));
			else frame = ECHO_RETURN;
		}
		float settleAt = Math.min(recovery * .8F, finalBeat + 2);
		return frame.weight(1 - smooth((t - settleAt) / (recovery - settleAt)));
	}
}
