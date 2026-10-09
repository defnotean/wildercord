package dev.wildercord.aura;

import static dev.wildercord.aura.MastersArtAnimation.*;

/**
 * Echo, Dawn and Venom's Classic body and hand timelines (presentation only; ids from {@link MethodsBStyles}). Echo strikes and
 * holds still as if listening; Dawn lifts and opens upward like light breaking; Venom coils low and bites from the hip.
 */
final class MethodsBStyleAnimation {
	private MethodsBStyleAnimation() {}

	// Ringing Cut: a short level cut, the blade held out after it so the sound can come back.
	private static final Motion RINGING_CUT = new Motion(
		pose(j(.06F, .42F, -.06F), j(-.02F, -.18F, 0), j(-1.42F, .72F, -.22F), j(-.95F, -.30F, .14F),
			j(-.30F, -.10F, -.06F), j(.22F, .10F, .06F), .22F, .08F, h(.12F, .06F, .10F, -12, 34, -18)),
		pose(j(.10F, -.46F, .08F), j(-.04F, .16F, 0), j(-1.48F, -.82F, -.16F), j(-.92F, -.12F, .22F),
			j(-.40F, .08F, -.06F), j(.26F, -.06F, .06F), .24F, -.52F, h(-.30F, .04F, -.30F, -4, -40, 22)),
		pose(j(.06F, -.36F, .05F), j(-.02F, .12F, 0), j(-1.44F, -.70F, -.12F), j(-.98F, -.16F, .18F),
			j(-.30F, .06F, -.05F), j(.22F, -.05F, .05F), .18F, -.34F, h(-.26F, .06F, -.24F, -6, -34, 18)));

	// Resonant Chord: both hands strike the blade flat across the chest, a wave pushed out ahead.
	private static final Motion RESONANT_CHORD = new Motion(
		pose(j(-.08F, .30F, -.04F), j(.04F, -.12F, 0), j(-1.95F, .40F, -.30F), j(-1.70F, -.40F, .30F),
			j(-.36F, -.12F, -.08F), j(.30F, .10F, .08F), .30F, .20F, h(.06F, .22F, .16F, -70, 22, -14)),
		pose(j(.24F, -.10F, .02F), j(-.10F, .04F, 0), j(-1.62F, -.18F, -.10F), j(-1.48F, .10F, .10F),
			j(-.62F, .04F, -.08F), j(.44F, -.04F, .08F), .55F, -.78F, h(-.04F, .10F, -.46F, -84, -8, 4)),
		pose(j(.16F, -.06F, .01F), j(-.06F, .02F, 0), j(-1.50F, -.12F, -.08F), j(-1.36F, .06F, .12F),
			j(-.48F, .02F, -.06F), j(.36F, -.02F, .06F), .42F, -.48F, h(-.06F, .12F, -.34F, -76, -6, 6)));

	// Counterpoint: the blade turned up beside the face to take the blow, then a short answering cut.
	private static final Motion COUNTERPOINT = new Motion(
		pose(j(-.04F, .18F, -.02F), j(.02F, -.06F, 0), j(-2.30F, .25F, -.40F), j(-1.20F, -.45F, .20F),
			j(-.28F, -.06F, -.05F), j(.24F, .06F, .05F), .28F, .14F, h(.04F, .38F, .06F, -36, 12, -60)),
		pose(j(.18F, -.38F, .06F), j(-.08F, .14F, 0), j(-1.30F, -.62F, -.30F), j(-.70F, -.25F, -.20F),
			j(-.48F, .06F, -.06F), j(.34F, -.05F, .06F), .36F, -.58F, h(-.22F, .08F, -.36F, 6, -30, 30)),
		pose(j(.08F, -.20F, .03F), j(-.04F, .08F, 0), j(-1.60F, -.36F, -.28F), j(-.95F, -.30F, .05F),
			j(-.32F, .04F, -.05F), j(.26F, -.04F, .05F), .26F, -.26F, h(-.14F, .20F, -.18F, -14, -20, 12)));

	// Reverb Step: a long low lunge, the blade trailing level behind the step.
	private static final Motion REVERB_STEP = new Motion(
		pose(j(.30F, .50F, -.10F), j(-.12F, -.22F, 0), j(-.95F, .90F, -.40F), j(-.40F, -.30F, -.70F),
			j(-.70F, -.14F, -.10F), j(.50F, .14F, .10F), .75F, .40F, h(.20F, -.04F, .08F, 18, 50, -30)),
		pose(j(.52F, -.30F, .08F), j(-.22F, .12F, 0), j(-1.55F, -.45F, -.20F), j(.05F, -.20F, -.60F),
			j(-1.05F, .10F, -.10F), j(.72F, -.10F, .10F), 1.00F, -1.45F, h(-.20F, -.06F, -.52F, -40, -24, 14)),
		pose(j(.36F, -.36F, .06F), j(-.14F, .14F, 0), j(-1.40F, -.60F, -.18F), j(-.10F, -.18F, -.55F),
			j(-.80F, .08F, -.08F), j(.56F, -.08F, .08F), .78F, -.95F, h(-.24F, -.04F, -.40F, -30, -32, 18)));

	// Grand Resonance: the blade raised high and brought down onto the ground point-first, rings going out.
	private static final Motion GRAND_RESONANCE = new Motion(
		pose(j(-.16F, .10F, -.02F), j(.08F, -.04F, 0), j(-2.85F, .30F, -.10F), j(-2.40F, -.30F, .10F),
			j(-.38F, -.10F, -.10F), j(.34F, .10F, .10F), .36F, .22F, h(.04F, .42F, .14F, -120, 10, -4)),
		pose(j(.60F, .06F, .02F), j(-.26F, -.02F, 0), j(-.70F, .05F, -.05F), j(-.80F, -.10F, .08F),
			j(-.98F, -.10F, -.14F), j(.60F, .10F, .14F), 1.42F, -1.05F, h(.02F, .06F, -.36F, 96, 4, -2)),
		pose(j(.44F, .04F, .01F), j(-.18F, -.02F, 0), j(-.55F, .04F, -.04F), j(-.70F, -.08F, .06F),
			j(-.74F, -.08F, -.10F), j(.46F, .08F, .10F), 1.10F, -.70F, h(.01F, .10F, -.22F, 86, 2, -1)));

	// First Light: a bright upward flick from the low guard, the free hand opening beside it.
	private static final Motion FIRST_LIGHT = new Motion(
		pose(j(.20F, .36F, -.08F), j(-.08F, -.14F, 0), j(-.30F, .55F, .30F), j(-.50F, -.40F, -.20F),
			j(-.50F, -.10F, -.08F), j(.34F, .10F, .08F), .66F, .12F, h(.14F, -.14F, .02F, 50, 30, -24)),
		pose(j(-.12F, -.28F, .06F), j(.06F, .10F, 0), j(-2.40F, -.40F, -.30F), j(-1.30F, .30F, -.60F),
			j(-.24F, .08F, -.05F), j(.18F, -.06F, .05F), .10F, -.40F, h(-.14F, .40F, -.24F, -100, -26, 20)),
		pose(j(-.06F, -.20F, .04F), j(.03F, .08F, 0), j(-2.10F, -.30F, -.24F), j(-1.10F, .22F, -.50F),
			j(-.20F, .06F, -.04F), j(.16F, -.04F, .04F), .06F, -.22F, h(-.10F, .34F, -.16F, -92, -18, 14)));

	// Sunrise Arc: a wide rising arc from the far hip to high over the lead shoulder.
	private static final Motion SUNRISE_ARC = new Motion(
		pose(j(.26F, .70F, -.16F), j(-.10F, -.30F, .04F), j(-.45F, 1.15F, .55F), j(-.70F, -.30F, -.45F),
			j(-.62F, -.16F, -.10F), j(.42F, .16F, .10F), .82F, .16F, h(.24F, -.10F, .02F, 40, 58, -46)),
		pose(j(-.08F, -.70F, .16F), j(.04F, .30F, -.04F), j(-2.55F, -1.05F, -.50F), j(-.60F, .30F, -.95F),
			j(-.30F, .16F, -.08F), j(.22F, -.14F, .08F), .18F, -.62F, h(-.44F, .36F, -.10F, -86, -60, 46)),
		pose(j(-.04F, -.80F, .12F), j(.02F, .34F, -.03F), j(-2.40F, -1.20F, -.40F), j(-.45F, .26F, -.85F),
			j(-.24F, .14F, -.07F), j(.18F, -.12F, .07F), .12F, -.40F, h(-.50F, .30F, -.02F, -80, -66, 50)));

	// Halo Guard: the blade held flat over the head like a ring of light, then turned down in a short cut.
	private static final Motion HALO_GUARD = new Motion(
		pose(j(-.10F, .05F, 0), j(.05F, -.02F, 0), j(-2.95F, .80F, -.20F), j(-2.60F, -.70F, .20F),
			j(-.26F, -.06F, -.06F), j(.22F, .06F, .06F), .20F, .06F, h(.10F, .48F, .02F, -150, 80, -6)),
		pose(j(.22F, -.24F, .04F), j(-.10F, .08F, 0), j(-1.75F, -.50F, -.26F), j(-1.90F, -.20F, .30F),
			j(-.52F, .06F, -.06F), j(.38F, -.04F, .06F), .44F, -.50F, h(-.16F, .22F, -.30F, -62, -22, 16)),
		pose(j(.10F, -.14F, .02F), j(-.04F, .05F, 0), j(-2.10F, -.30F, -.22F), j(-2.20F, -.30F, .24F),
			j(-.34F, .04F, -.05F), j(.26F, -.03F, .05F), .28F, -.24F, h(-.10F, .30F, -.18F, -84, -14, 10)));

	// Dawnbreak Rush: a straight shining charge, blade point first and the body low behind it.
	private static final Motion DAWNBREAK_RUSH = new Motion(
		pose(j(.36F, .20F, -.04F), j(-.16F, -.08F, 0), j(-1.00F, .40F, -.10F), j(-1.25F, -.40F, .10F),
			j(-.74F, -.08F, -.10F), j(.56F, .08F, .10F), .90F, .50F, h(.10F, .04F, .30F, -90, 18, -6)),
		pose(j(.64F, -.12F, .02F), j(-.30F, .04F, 0), j(-1.70F, -.06F, -.04F), j(.20F, -.36F, -.30F),
			j(-1.18F, .06F, -.10F), j(.82F, -.06F, .10F), 1.12F, -1.85F, h(-.06F, .08F, -.66F, -104, -4, 2)),
		pose(j(.46F, -.18F, .03F), j(-.20F, .06F, 0), j(-1.52F, -.12F, -.06F), j(.10F, -.30F, -.34F),
			j(-.90F, .06F, -.08F), j(.62F, -.05F, .08F), .88F, -1.20F, h(-.08F, .10F, -.48F, -98, -6, 4)));

	// Noon Zenith: both hands lift the blade straight up to the sky, then it falls like the noon sun.
	private static final Motion NOON_ZENITH = new Motion(
		pose(j(-.24F, .02F, 0), j(.12F, 0, 0), j(-3.05F, .06F, -.06F), j(-2.90F, -.06F, .06F),
			j(-.30F, -.08F, -.08F), j(.28F, .08F, .08F), -.10F, .30F, h(.02F, .52F, .08F, -170, 2, 0)),
		pose(j(.48F, -.04F, .01F), j(-.20F, .02F, 0), j(-1.05F, -.04F, -.04F), j(-1.10F, .04F, .04F),
			j(-.86F, .06F, -.12F), j(.58F, -.06F, .12F), 1.28F, -1.30F, h(-.02F, .12F, -.44F, 80, -2, 0)),
		pose(j(.34F, -.03F, .01F), j(-.14F, .01F, 0), j(-.90F, -.03F, -.03F), j(-.95F, .03F, .03F),
			j(-.66F, .05F, -.10F), j(.44F, -.05F, .10F), 1.00F, -.82F, h(-.03F, .14F, -.30F, 74, -1, 0)));

	// Fang Strike: coiled at the hip, a snapping thrust low and fast, then the blade drawn back.
	private static final Motion FANG_STRIKE = new Motion(
		pose(j(.40F, .40F, -.12F), j(-.18F, -.18F, .02F), j(-.62F, .50F, -.30F), j(-.70F, -.20F, .30F),
			j(-.80F, -.12F, -.10F), j(.58F, .12F, .10F), 1.05F, .45F, h(.16F, -.18F, .26F, -76, 28, -12)),
		pose(j(.58F, -.18F, .04F), j(-.26F, .06F, 0), j(-1.20F, -.10F, -.06F), j(-.36F, -.14F, .36F),
			j(-1.10F, .06F, -.10F), j(.80F, -.06F, .10F), 1.20F, -1.30F, h(-.04F, -.10F, -.62F, -94, -6, 8)),
		pose(j(.48F, .06F, -.02F), j(-.20F, -.02F, 0), j(-.85F, .20F, -.18F), j(-.55F, -.18F, .34F),
			j(-.92F, -.04F, -.10F), j(.66F, .04F, .10F), 1.10F, -.40F, h(.06F, -.14F, -.10F, -82, 14, -4)));

	// Spitting Cobra: the head rears back, then the blade whips forward in a short flicking spit.
	private static final Motion SPITTING_COBRA = new Motion(
		pose(j(-.22F, .26F, -.06F), j(.12F, -.10F, 0), j(-2.20F, .50F, -.50F), j(-1.10F, -.30F, .30F),
			j(-.30F, -.10F, -.06F), j(.30F, .08F, .06F), .30F, .40F, h(.10F, .34F, .22F, -118, 26, -26)),
		pose(j(.34F, -.20F, .05F), j(-.14F, .08F, 0), j(-1.38F, -.30F, -.20F), j(-.80F, -.20F, .40F),
			j(-.70F, .06F, -.08F), j(.52F, -.05F, .08F), .62F, -.92F, h(-.10F, .04F, -.54F, -60, -14, 18)),
		pose(j(.24F, -.12F, .03F), j(-.10F, .05F, 0), j(-1.25F, -.22F, -.24F), j(-.90F, -.24F, .36F),
			j(-.54F, .05F, -.06F), j(.40F, -.04F, .06F), .48F, -.60F, h(-.08F, .08F, -.40F, -52, -10, 14)));

	// Shed Skin: a twist and a backward hop, the blade sweeping low through the space it leaves.
	private static final Motion SHED_SKIN = new Motion(
		pose(j(.10F, -.60F, .14F), j(-.04F, .26F, -.04F), j(-.80F, -.95F, .40F), j(-.30F, .40F, -.80F),
			j(-.36F, .14F, -.08F), j(.28F, -.14F, .08F), .40F, -.30F, h(-.28F, -.08F, .04F, 26, -48, 40)),
		pose(j(.32F, .66F, -.14F), j(-.14F, -.28F, .04F), j(-.50F, 1.05F, -.25F), j(-.25F, -.40F, -.70F),
			j(-.62F, -.16F, -.10F), j(.46F, .16F, .10F), .80F, .62F, h(.34F, -.18F, -.14F, 30, 62, -50)),
		pose(j(.22F, .52F, -.10F), j(-.10F, -.22F, .03F), j(-.60F, .90F, -.30F), j(-.35F, -.32F, -.62F),
			j(-.48F, -.12F, -.08F), j(.36F, .12F, .08F), .60F, .48F, h(.28F, -.14F, -.06F, 22, 54, -42)));

	// Serpent Slither: body almost flat to the ground, a weaving dash with the blade held low and level.
	private static final Motion SERPENT_SLITHER = new Motion(
		pose(j(.70F, .28F, -.20F), j(-.30F, -.12F, .06F), j(-.50F, .60F, -.70F), j(-.20F, -.20F, -.90F),
			j(-.95F, -.10F, -.14F), j(.70F, .10F, .14F), 1.35F, .20F, h(.20F, -.26F, .06F, 12, 40, -70)),
		pose(j(.82F, -.34F, .20F), j(-.36F, .16F, -.06F), j(-.70F, -.70F, -.55F), j(-.10F, .20F, -.80F),
			j(-1.20F, .12F, -.14F), j(.86F, -.12F, .14F), 1.48F, -1.60F, h(-.26F, -.28F, -.40F, 8, -44, 66)),
		pose(j(.66F, -.20F, .14F), j(-.28F, .10F, -.04F), j(-.66F, -.50F, -.50F), j(-.16F, .16F, -.72F),
			j(-.98F, .10F, -.12F), j(.72F, -.10F, .12F), 1.30F, -1.05F, h(-.20F, -.24F, -.30F, 10, -36, 58)));

	// Hydra Coil: the blade wheels round the body in a full coil, then bites down in the middle of it.
	private static final Motion HYDRA_COIL = new Motion(
		pose(j(.18F, 1.05F, -.20F), j(-.08F, -.45F, .06F), j(-1.20F, 1.40F, -.30F), j(-.40F, -.60F, -.70F),
			j(-.52F, -.24F, -.12F), j(.40F, .24F, .12F), .70F, .10F, h(.30F, .12F, -.20F, -20, 76, -40)),
		pose(j(.44F, -1.10F, .22F), j(-.18F, .48F, -.06F), j(-1.10F, -1.45F, -.20F), j(-.30F, .55F, -.95F),
			j(-.84F, .24F, -.12F), j(.62F, -.24F, .12F), 1.18F, -.85F, h(-.40F, .02F, -.32F, 44, -72, 36)),
		pose(j(.32F, -.90F, .16F), j(-.12F, .40F, -.04F), j(-1.00F, -1.25F, -.16F), j(-.22F, .45F, -.80F),
			j(-.64F, .20F, -.10F), j(.48F, -.20F, .10F), .92F, -.55F, h(-.34F, .06F, -.22F, 52, -64, 30)));

	/** The Classic timeline for one of the pack's style ids, or null. */
	static Motion motion(int id) {
		return switch (id) {
			case MethodsBStyles.FIRST -> RINGING_CUT;
			case MethodsBStyles.FIRST + 1 -> RESONANT_CHORD;
			case MethodsBStyles.FIRST + 2 -> COUNTERPOINT;
			case MethodsBStyles.FIRST + 3 -> REVERB_STEP;
			case MethodsBStyles.FIRST + 4 -> GRAND_RESONANCE;
			case MethodsBStyles.FIRST + 5 -> FIRST_LIGHT;
			case MethodsBStyles.FIRST + 6 -> SUNRISE_ARC;
			case MethodsBStyles.FIRST + 7 -> HALO_GUARD;
			case MethodsBStyles.FIRST + 8 -> DAWNBREAK_RUSH;
			case MethodsBStyles.FIRST + 9 -> NOON_ZENITH;
			case MethodsBStyles.FIRST + 10 -> FANG_STRIKE;
			case MethodsBStyles.FIRST + 11 -> SPITTING_COBRA;
			case MethodsBStyles.FIRST + 12 -> SHED_SKIN;
			case MethodsBStyles.FIRST + 13 -> SERPENT_SLITHER;
			case MethodsBStyles.FIRST + 14 -> HYDRA_COIL;
			default -> null;
		};
	}
}
