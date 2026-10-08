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
	// Keep the first-person wrist open through the seam: the old turn exposed only its dark edge.
	private static final Motion VOID_CUT = new Motion(
		pose(j(.32F, .22F, .06F), j(-.18F, -.08F, 0), j(-1.15F, .45F, -.18F), j(-1.35F, -.45F, .20F),
			j(-.55F, -.08F, -.08F), j(.45F, .08F, .08F), .85F, .20F, h(.08F, -.05F, -.20F, -5, 18, -10)),
		pose(j(.20F, -.35F, -.06F), j(-.10F, .10F, .02F), j(-1.48F, -.60F, -.15F), j(-1.20F, -.30F, .18F),
			j(-.60F, .08F, -.08F), j(.40F, -.06F, .08F), .70F, -.40F, h(-.20F, -.08F, -.35F, -18, -5, -10)),
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

	// Collapse plants a broad base, drives the raised edge down once, then absorbs its weight.
	// The released fissure lives independently; its pulses never restart the physical strike.
	private static final Motion COLLAPSE = new Motion(
		pose(j(-.12F, .10F, -.02F), j(.05F, -.04F, 0), j(-2.45F, .12F, -.25F), j(-1.72F, -.18F, .15F),
			j(-.48F, -.14F, -.15F), j(.34F, .12F, .13F), .70F, .18F, h(.02F, .46F, -.16F, -92, 6, -12)),
		pose(j(.48F, -.06F, .02F), j(-.22F, .02F, 0), j(-.58F, -.06F, -.16F), j(-.85F, -.12F, .15F),
			j(-.85F, -.12F, -.16F), j(.52F, .10F, .14F), 1.18F, -.88F, h(-.04F, .28F, -.44F, 58, -5, 12)),
		pose(j(.34F, -.08F, .01F), j(-.16F, .03F, 0), j(-.35F, -.08F, -.14F), j(-.64F, -.10F, .15F),
			j(-.68F, -.10F, -.14F), j(.40F, .08F, .12F), .95F, -.48F, h(-.06F, .34F, -.34F, 46, -10, 16)));

	// Red Rain coils above the outside shoulder and rakes diagonally into an open, low finish.
	// Crimson's pool persists after this one release while the blade recovers along the far side.
	// Open the first-person chamber face and carry the whole grip outward. The old narrow wrist
	// turn exposed only the dark sprite extrusion during a steep, opposite-handed free look.
	private static final Motion RED_RAIN = new Motion(
		pose(j(.14F, .48F, -.16F), j(-.08F, -.18F, .05F), j(-2.08F, .72F, -.66F), j(-.82F, -.28F, -.46F),
			j(-.54F, -.16F, -.12F), j(.34F, .12F, .10F), .64F, .14F, h(.22F, .38F, -.14F, -62, 48, -20)),
		pose(j(.34F, -.46F, .20F), j(-.17F, .18F, -.06F), j(-.66F, -.82F, -.38F), j(-.16F, -.12F, -.88F),
			j(-.78F, .14F, -.14F), j(.44F, -.10F, .12F), .98F, -.80F, h(-.28F, .28F, -.40F, 42, -32, 46)),
		pose(j(.23F, -.62F, .14F), j(-.11F, .22F, -.04F), j(-.34F, -1.02F, -.12F), j(-.30F, -.16F, -.68F),
			j(-.55F, .11F, -.11F), j(.32F, -.08F, .09F), .74F, -.42F, h(-.34F, .34F, -.28F, 48, -40, 58)));

	// Moon coils beside the outside hip over a low broad base, then makes one rising-edge
	// sweep. The free hand counterbalances the far-side finish; delayed wounds add no cuts.
	private static final Motion CRIMSON_MOON = new Motion(
		pose(j(.18F, .65F, -.12F), j(-.09F, -.28F, .04F), j(-.22F, .78F, .36F), j(-.64F, -.26F, -.72F),
			j(-.70F, -.24F, -.20F), j(.46F, .21F, .18F), 1.02F, .14F, h(-.05F, .40F, -.35F, 12, 28, -28)),
		pose(j(.12F, -.62F, .12F), j(-.06F, .26F, -.04F), j(-1.20F, -1.10F, -.22F), j(-.18F, .24F, -1.05F),
			j(-.66F, .22F, -.20F), j(.42F, -.18F, .18F), .90F, -.56F, h(-.26F, .26F, -.34F, -12, -42, 34)),
		pose(j(.08F, -.82F, .10F), j(-.04F, .34F, -.03F), j(-1.36F, -1.32F, -.08F), j(.06F, .18F, -.88F),
			j(-.56F, .20F, -.18F), j(.36F, -.16F, .16F), .80F, -.28F, h(-.34F, .30F, -.22F, -24, -56, 48)));

	// Backdraft braces at the ribs, punches a short point and withdraws into the same guard.
	// All displacement is model-local: the paid counter never adds a dash or a second release.
	private static final Motion BACKDRAFT = new Motion(
		pose(j(.12F, .18F, -.04F), j(-.06F, -.08F, .02F), j(-1.22F, .28F, -.22F), j(-1.10F, -.32F, .12F),
			j(-.48F, -.10F, -.12F), j(.32F, .10F, .10F), .62F, .12F, h(.12F, .18F, -.18F, -72, 20, -14)),
		pose(j(.20F, -.08F, .02F), j(-.10F, .04F, -.01F), j(-1.52F, -.06F, -.12F), j(-1.02F, -.24F, .14F),
			j(-.54F, -.08F, -.12F), j(.34F, .08F, .10F), .68F, -.42F, h(.02F, .22F, -.38F, -94, 6, 6)),
		pose(j(.13F, -.04F, .01F), j(-.06F, .02F, 0), j(-1.18F, -.10F, -.22F), j(-1.12F, -.28F, .12F),
			j(-.45F, -.08F, -.10F), j(.30F, .08F, .09F), .58F, -.10F, h(.08F, .18F, -.16F, -66, 10, -8)));

	// Rooted Parry receives over a broad low base, then lifts the edge in one compact reply.
	// The return-to-guard follows the one accepted release; its brace has no invulnerability authority.
	private static final Motion ROOTED_PARRY = new Motion(
		pose(j(.24F, .14F, -.06F), j(-.12F, -.06F, .02F), j(-.52F, .32F, -.28F), j(-1.18F, -.32F, .20F),
			j(-.65F, -.18F, -.18F), j(.42F, .16F, .16F), .96F, .06F, h(.04F, .38F, -.24F, 16, 18, -18)),
		pose(j(.06F, -.16F, .04F), j(-.02F, .07F, -.02F), j(-1.76F, -.24F, -.30F), j(-.96F, -.20F, .22F),
			j(-.44F, -.16F, -.16F), j(.30F, .14F, .14F), .54F, -.28F, h(-.08F, .30F, -.34F, -52, -10, 20)),
		pose(j(.14F, -.12F, .03F), j(-.06F, .05F, -.01F), j(-1.45F, -.20F, -.24F), j(-1.12F, -.24F, .18F),
			j(-.56F, -.16F, -.16F), j(.36F, .14F, .14F), .76F, -.12F, h(-.02F, .24F, -.22F, -34, -6, 12)));

	// Glacier Mirror receives behind an upright oblique edge, then opens the blade outward
	// in one level answer. The square guard closes again without replaying the caught attack.
	private static final Motion GLACIER_MIRROR = new Motion(
		pose(j(.03F, .22F, -.10F), j(-.02F, -.10F, .04F), j(-1.50F, .46F, -.72F), j(-1.30F, -.24F, .18F),
			j(-.32F, -.09F, -.08F), j(.23F, .08F, .08F), .38F, .04F, h(.10F, .24F, -.22F, -28, 26, -32)),
		pose(j(.06F, -.28F, .08F), j(-.03F, .12F, -.03F), j(-1.18F, -.64F, -.54F), j(-1.18F, -.16F, .24F),
			j(-.38F, .08F, -.09F), j(.25F, -.07F, .08F), .42F, -.26F, h(-.20F, .22F, -.32F, -14, -2, 24)),
		pose(j(.03F, -.08F, .02F), j(-.02F, .04F, 0), j(-1.42F, -.20F, -.54F), j(-1.28F, -.22F, .20F),
			j(-.29F, .05F, -.08F), j(.21F, -.04F, .07F), .34F, -.06F, h(-.04F, .22F, -.22F, -24, -4, 8)));

	// Static Riposte folds beside the outside shoulder, snaps a short diagonal edge across
	// the opening, then recoils high. Its asymmetry is all model-local; there is no dash.
	private static final Motion STATIC_RIPOSTE = new Motion(
		pose(j(.18F, .38F, -.16F), j(-.10F, -.16F, .06F), j(-1.92F, .68F, -.46F), j(-.86F, -.28F, -.48F),
			j(-.46F, -.12F, -.11F), j(.34F, .10F, .10F), .62F, .08F, h(.16F, .28F, -.18F, -50, 30, -26)),
		pose(j(.24F, -.34F, .12F), j(-.12F, .14F, -.05F), j(-.96F, -.58F, -.36F), j(-.58F, -.16F, -.64F),
			j(-.53F, .11F, -.11F), j(.34F, -.09F, .10F), .66F, -.40F, h(-.14F, .20F, -.38F, 24, -24, 34)),
		pose(j(.10F, -.18F, .05F), j(-.05F, .08F, -.02F), j(-1.65F, -.32F, -.40F), j(-.82F, -.24F, -.42F),
			j(-.38F, .09F, -.10F), j(.28F, -.08F, .09F), .48F, -.12F, h(.02F, .26F, -.22F, -34, -12, 16)));


	// Unmoved takes the load on a broad, lowered base. The level point drives a short
	// distance from the ribs before withdrawing; the brace never implies a second hit.
	private static final Motion UNMOVED = new Motion(
		pose(j(.27F, .10F, -.025F), j(-.14F, -.04F, .01F), j(-.94F, .12F, -.32F), j(-1.16F, -.42F, .30F),
			j(-.74F, -.23F, -.19F), j(.49F, .20F, .17F), 1.14F, .03F, h(.17F, .25F, -.14F, -78, 12, -18)),
		pose(j(.34F, -.035F, .025F), j(-.18F, .01F, -.01F), j(-1.56F, -.035F, -.18F), j(-1.24F, -.34F, .24F),
			j(-.80F, -.21F, -.19F), j(.52F, .18F, .17F), 1.16F, -.32F, h(.08F, .27F, -.44F, -96, 4, 10)),
		pose(j(.23F, -.02F, .01F), j(-.12F, .015F, 0), j(-1.10F, -.06F, -.28F), j(-1.18F, -.40F, .28F),
			j(-.71F, -.20F, -.18F), j(.47F, .18F, .16F), 1.08F, -.06F, h(.14F, .25F, -.20F, -72, 8, -6)));

	// Null Parry receives on an outside slant, draws the edge inward through a narrow
	// horizontal pocket and closes beside the sternum. Its void pulse has no extra swing.
	private static final Motion NULL_PARRY = new Motion(
		pose(j(.16F, .31F, .09F), j(-.08F, -.13F, -.03F), j(-1.18F, .66F, -.46F), j(-1.05F, -.38F, .34F),
			j(-.48F, -.11F, -.095F), j(.32F, .09F, .085F), .65F, .07F, h(.12F, .25F, -.23F, -24, 32, -30)),
		pose(j(.12F, -.21F, -.045F), j(-.065F, .085F, .02F), j(-1.32F, -.44F, -.31F), j(-1.20F, -.22F, .28F),
			j(-.52F, .075F, -.10F), j(.34F, -.06F, .09F), .68F, -.18F, h(-.17F, .25F, -.34F, -8, 14, 30)),
		pose(j(.20F, -.055F, .025F), j(-.10F, .025F, -.01F), j(-1.12F, -.16F, -.38F), j(-1.28F, -.30F, .31F),
			j(-.46F, .04F, -.09F), j(.31F, -.03F, .08F), .72F, .02F, h(-.02F, .24F, -.18F, -18, 4, 6)));

	// Eye of the Storm winds through a deep rotational coil, sweeps a level 360-degree perimeter slice, then settles in a wide stance.
	private static final Motion EYE_OF_THE_STORM = new Motion(
		pose(j(.06F, .68F, -.14F), j(-.02F, -.32F, .04F), j(-1.48F, .95F, -.30F), j(-.40F, -.35F, -.70F),
			j(-.42F, -.22F, -.10F), j(.32F, .18F, .10F), .35F, .12F, h(.12F, .15F, -.22F, 5, 42, -28)),
		pose(j(.10F, -.82F, .16F), j(-.04F, .32F, -.05F), j(-1.30F, -1.25F, -.10F), j(-.20F, .30F, -1.00F),
			j(-.52F, .22F, -.12F), j(.36F, -.18F, .10F), .28F, -.55F, h(-.44F, -.08F, -.26F, 15, -58, 38)),
		pose(j(.04F, -.92F, .10F), j(0, .38F, -.02F), j(-1.20F, -1.35F, -.08F), j(.12F, .22F, -.80F),
			j(-.28F, .18F, -.08F), j(.22F, -.14F, .07F), .15F, -.25F, h(-.50F, -.18F, -.08F, 24, -65, 44)));

	// Sanguine Parry braces across the chest in a deep crouch, snaps an aggressive rising cross-slash, and recoils high.
	private static final Motion SANGUINE_PARRY = new Motion(
		pose(j(.28F, .36F, -.14F), j(-.14F, -.14F, .05F), j(-1.42F, .72F, -.70F), j(-.82F, -.22F, .15F),
			j(-.68F, -.14F, -.14F), j(.45F, .14F, .12F), .95F, .12F, h(.14F, .22F, -.20F, -45, 28, -42)),
		pose(j(.36F, -.48F, .18F), j(-.18F, .16F, -.06F), j(-.82F, -.88F, -.42F), j(-.22F, -.14F, -.92F),
			j(-.85F, .14F, -.14F), j(.48F, -.10F, .12F), 1.10F, -.75F, h(-.30F, .16F, -.36F, 48, -36, 48)),
		pose(j(.22F, -.60F, .12F), j(-.10F, .20F, -.03F), j(-.42F, -1.05F, -.15F), j(-.12F, -.12F, -.75F),
			j(-.58F, .12F, -.10F), j(.34F, -.08F, .09F), .78F, -.38F, h(-.36F, .22F, -.24F, 56, -44, 55)));

	// Constellation Guard lifts high beside the temple, snaps a crisp angular forward intercept, then withdraws with crystalline poise.
	private static final Motion CONSTELLATION_GUARD = new Motion(
		pose(j(-.06F, .24F, -.04F), j(.03F, -.10F, .02F), j(-1.85F, .38F, -.18F), j(-.92F, -.25F, -.30F),
			j(-.26F, -.08F, -.05F), j(.28F, .08F, .05F), .16F, .20F, h(.12F, .22F, -.16F, -85, 16, -12)),
		pose(j(.18F, -.10F, .03F), j(-.08F, .04F, -.01F), j(-1.62F, -.12F, -.06F), j(-.30F, -.18F, -.52F),
			j(-.58F, .06F, -.06F), j(.38F, -.05F, .06F), .30F, -.85F, h(-.04F, .14F, -.48F, -102, 2, 8)),
		pose(j(.10F, -.08F, .02F), j(-.04F, .03F, 0), j(-1.42F, -.10F, -.08F), j(-.55F, -.16F, -.42F),
			j(-.38F, .04F, -.05F), j(.28F, -.04F, .05F), .18F, -.35F, h(-.02F, .10F, -.26F, -88, -2, 6)));

	// Stopped Moment draws to the floating ribs, drives a level suspended point through temporal stillness, and recoils along the thrust line.
	private static final Motion STOPPED_MOMENT = new Motion(
		pose(j(.08F, .36F, -.05F), j(-.03F, -.15F, .01F), j(-1.18F, .55F, -.22F), j(-1.08F, -.24F, -.25F),
			j(-.35F, -.10F, -.07F), j(.26F, .10F, .07F), .30F, .14F, h(.12F, .06F, -.18F, -45, 24, -14)),
		pose(j(.16F, -.14F, .03F), j(-.07F, .06F, -.01F), j(-1.58F, -.10F, -.08F), j(-.50F, -.14F, -.42F),
			j(-.55F, .08F, -.08F), j(.35F, -.06F, .08F), .40F, -.70F, h(-.06F, .08F, -.52F, -98, -4, 10)),
		pose(j(.10F, -.22F, .03F), j(-.04F, .09F, 0), j(-1.35F, -.22F, -.14F), j(-.65F, -.14F, -.38F),
			j(-.40F, .08F, -.07F), j(.28F, -.06F, .07F), .28F, -.35F, h(-.10F, .04F, -.32F, -78, -12, 14)));

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
			case 17 -> COLLAPSE;
			case 18 -> RED_RAIN;
			case 19 -> CRIMSON_MOON;
			case 20 -> BACKDRAFT;
			case 21 -> ROOTED_PARRY;
			case 22 -> GLACIER_MIRROR;
			case 23 -> STATIC_RIPOSTE;
			case 24 -> UNMOVED;
			case 25 -> NULL_PARRY;
			case 26 -> EYE_OF_THE_STORM;
			case 27 -> SANGUINE_PARRY;
			case 28 -> CONSTELLATION_GUARD;
			case 29 -> STOPPED_MOMENT;
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
