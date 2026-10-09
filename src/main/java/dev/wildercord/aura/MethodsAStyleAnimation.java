package dev.wildercord.aura;

import static dev.wildercord.aura.MastersArtAnimation.*;

/**
 * Classic body and first-person choreography for Tide, Iron and Dune's fifteen arts (the methods-a pack). Each
 * method has its own body: Tide rolls the hips and lets the blade flow round them, Iron plants deep and drops
 * its weight through the edge, Dune stays low and flicks from the ground. Presentation only, like
 * {@link MastersStyleAnimation}; the ids match {@link MastersStyleRules}.
 */
public final class MethodsAStyleAnimation {
	private MethodsAStyleAnimation() {}

	public static final int RIPTIDE_CUT = 110, BREAKER = 111, WHIRLPOOL = 112, SURGE = 113, MAELSTROM = 114;
	public static final int SUNDER_CUT = 115, ANVIL_FALL = 116, BULWARK = 117, FORGE_CHARGE = 118, WORLDFORGE = 119;
	public static final int GRIT_FLICK = 120, QUICKSAND = 121, SANDVEIL = 122, DUNE_RUNNER = 123, SEA_OF_SAND = 124;

	// ------------------------------------------------------------------ Tide

	// Riptide reaches wide to the outside, sweeps level across the waist, then draws the blade back in toward the body like an undertow.
	private static final Motion RIPTIDE = new Motion(
		pose(j(.10F, .70F, -.06F), j(-.04F, -.30F, 0), j(-1.15F, 1.20F, -.30F), j(-.70F, -.40F, -.30F),
			j(-.45F, -.18F, -.08F), j(.40F, .14F, .08F), .55F, .10F, h(.20F, .14F, .04F, -20, 48, -30)),
		pose(j(.16F, -.55F, .08F), j(-.06F, .22F, -.04F), j(-1.25F, -1.10F, -.20F), j(-.40F, .20F, -.50F),
			j(-.62F, .10F, -.08F), j(.42F, -.12F, .08F), .62F, -.70F, h(-.38F, .10F, -.22F, 12, -50, 34)),
		pose(j(.08F, -.20F, .04F), j(-.02F, .10F, 0), j(-1.05F, -.30F, -.10F), j(-.85F, -.05F, -.20F),
			j(-.40F, .06F, -.06F), j(.30F, -.06F, .06F), .35F, -.30F, h(-.10F, .06F, -.10F, -6, -18, 12)));

	// Breaker rises with the blade over the head, then breaks forward and down like a wave on the shore, ending long and low.
	private static final Motion BREAKER_FORM = new Motion(
		pose(j(-.22F, .18F, -.04F), j(.12F, -.08F, 0), j(-2.85F, .30F, -.20F), j(-2.10F, -.30F, .30F),
			j(-.30F, -.06F, -.06F), j(.25F, .06F, .06F), -.30F, .30F, h(.06F, .40F, .02F, -92, 12, -10)),
		pose(j(.55F, -.10F, .04F), j(-.24F, .06F, 0), j(-.70F, -.25F, -.12F), j(-.35F, .15F, -.35F),
			j(-1.00F, .06F, -.10F), j(.72F, -.06F, .08F), 1.15F, -1.40F, h(-.10F, .02F, -.50F, 62, -10, 8)),
		pose(j(.40F, -.16F, .05F), j(-.16F, .08F, 0), j(-.55F, -.40F, -.10F), j(-.25F, .10F, -.40F),
			j(-.80F, .08F, -.08F), j(.55F, -.08F, .08F), .85F, -1.00F, h(-.16F, .00F, -.38F, 54, -18, 14)));

	// Whirlpool turns a caught blow into a full circle: the hips wheel through, the blade flat and low round the ankles.
	private static final Motion WHIRLPOOL_FORM = new Motion(
		pose(j(.05F, .90F, -.04F), j(-.02F, -.42F, 0), j(-1.55F, .65F, -.45F), j(-1.30F, -.20F, .10F),
			j(-.30F, -.10F, -.06F), j(.28F, .10F, .06F), .30F, .05F, h(.12F, .20F, .00F, -36, 30, -24)),
		pose(j(.30F, -1.10F, .10F), j(-.12F, .45F, -.04F), j(-.62F, -1.30F, -.55F), j(-.30F, .30F, -.70F),
			j(-.72F, .16F, -.10F), j(.52F, -.16F, .10F), 1.05F, -.40F, h(-.44F, -.02F, -.18F, 34, -58, 48)),
		pose(j(.18F, -.80F, .06F), j(-.08F, .32F, 0), j(-.80F, -1.00F, -.30F), j(-.40F, .20F, -.55F),
			j(-.52F, .12F, -.08F), j(.40F, -.12F, .08F), .70F, -.25F, h(-.32F, .04F, -.12F, 22, -40, 30)));

	// Surge rides the wave: a long, low forward lunge with the point leading and the free arm streaming behind.
	private static final Motion SURGE_FORM = new Motion(
		pose(j(-.08F, .45F, -.05F), j(.06F, -.22F, 0), j(-.95F, .55F, -.15F), j(-.50F, -.45F, -.40F),
			j(-.30F, -.15F, -.08F), j(.40F, .12F, .08F), .35F, .40F, h(.16F, .00F, .06F, -96, 18, -6)),
		pose(j(.58F, -.18F, .04F), j(-.26F, .08F, 0), j(-1.55F, -.05F, -.06F), j(.65F, -.40F, -.30F),
			j(-1.15F, .06F, -.10F), j(.78F, -.06F, .08F), 1.00F, -1.90F, h(-.08F, .05F, -.62F, -104, -4, 4)),
		pose(j(.32F, -.24F, .05F), j(-.14F, .10F, 0), j(-1.40F, -.15F, -.08F), j(.40F, -.25F, -.35F),
			j(-.72F, .10F, -.08F), j(.50F, -.08F, .08F), .60F, -1.05F, h(-.12F, .02F, -.44F, -96, -10, 10)));

	// Maelstrom winds the blade up round the head in a rising spiral, then brings the whole sea down in front.
	private static final Motion MAELSTROM_FORM = new Motion(
		pose(j(-.18F, .80F, -.08F), j(.10F, -.36F, 0), j(-2.65F, .85F, -.55F), j(-2.30F, -.55F, .45F),
			j(-.25F, -.12F, -.06F), j(.22F, .10F, .06F), -.20F, .20F, h(.10F, .44F, .00F, -84, 40, -36)),
		pose(j(.48F, -.62F, .08F), j(-.20F, .26F, -.04F), j(-.85F, -.90F, -.40F), j(-.60F, .40F, -.60F),
			j(-.92F, .12F, -.10F), j(.66F, -.12F, .10F), 1.20F, -1.10F, h(-.30F, .00F, -.42F, 58, -44, 40)),
		pose(j(.34F, -.48F, .06F), j(-.14F, .20F, 0), j(-.70F, -.70F, -.30F), j(-.50F, .30F, -.50F),
			j(-.70F, .10F, -.08F), j(.52F, -.10F, .08F), .90F, -.70F, h(-.26F, .02F, -.30F, 50, -34, 32)));

	// ------------------------------------------------------------------ Iron

	// Sunder Cut loads the blade on the rear shoulder and drives one heavy diagonal down through the plates.
	private static final Motion SUNDER = new Motion(
		pose(j(-.10F, .62F, -.10F), j(.06F, -.28F, 0), j(-2.45F, .80F, -.70F), j(-1.45F, -.10F, .20F),
			j(-.40F, -.14F, -.08F), j(.38F, .12F, .08F), .50F, .15F, h(.18F, .36F, .02F, -70, 38, -44)),
		pose(j(.42F, -.40F, .12F), j(-.18F, .16F, -.04F), j(-.60F, -.85F, -.30F), j(-.70F, .10F, -.45F),
			j(-.88F, .08F, -.10F), j(.62F, -.10F, .10F), 1.10F, -.95F, h(-.30F, -.02F, -.32F, 66, -36, 46)),
		pose(j(.30F, -.30F, .10F), j(-.12F, .12F, 0), j(-.45F, -.70F, -.22F), j(-.62F, .06F, -.40F),
			j(-.66F, .08F, -.08F), j(.48F, -.08F, .08F), .85F, -.60F, h(-.24F, .00F, -.24F, 60, -28, 40)));

	// Anvil Fall gathers both hands high as the body rises, then hammers straight down into a deep, square stance.
	private static final Motion ANVIL = new Motion(
		pose(j(-.30F, .06F, 0), j(.16F, -.02F, 0), j(-3.05F, .10F, -.08F), j(-2.95F, -.10F, .08F),
			j(-.20F, -.04F, -.04F), j(.20F, .04F, .04F), -.55F, .35F, h(.00F, .45F, .04F, -108, 4, -4)),
		pose(j(.70F, -.04F, .02F), j(-.32F, .02F, 0), j(-.35F, -.10F, -.06F), j(-.45F, .10F, .06F),
			j(-1.20F, .04F, -.12F), j(.85F, -.04F, .10F), 1.75F, -1.10F, h(-.04F, -.04F, -.56F, 80, -4, 4)),
		pose(j(.55F, -.06F, .02F), j(-.24F, .04F, 0), j(-.30F, -.18F, -.06F), j(-.40F, .14F, .06F),
			j(-1.00F, .06F, -.10F), j(.72F, -.06F, .08F), 1.45F, -.85F, h(-.06F, -.02F, -.46F, 74, -8, 8)));

	// Bulwark sets the blade upright before the face, braced on the back foot, then shoulders the guard outward.
	private static final Motion BULWARK_FORM = new Motion(
		pose(j(-.06F, .30F, -.02F), j(.04F, -.14F, 0), j(-1.70F, .55F, .25F), j(-1.65F, -.45F, -.25F),
			j(-.22F, -.08F, -.06F), j(.55F, .10F, .08F), .80F, .45F, h(-.06F, .30F, .06F, -48, 10, 62)),
		pose(j(.36F, -.12F, .02F), j(-.14F, .06F, 0), j(-1.55F, -.20F, .35F), j(-1.40F, .30F, -.35F),
			j(-.78F, .06F, -.08F), j(.60F, -.06F, .08F), 1.30F, -.80F, h(-.14F, .26F, -.30F, -40, -6, 70)),
		pose(j(.20F, -.08F, .02F), j(-.08F, .04F, 0), j(-1.50F, -.10F, .30F), j(-1.45F, .20F, -.30F),
			j(-.50F, .04F, -.06F), j(.50F, -.04F, .06F), 1.05F, -.40F, h(-.10F, .28F, -.18F, -44, -2, 66)));

	// Forge Charge drops the shoulder and drives through, the blade held low at the hip like a ram behind the shoulder.
	private static final Motion FORGE = new Motion(
		pose(j(.20F, .50F, -.10F), j(-.08F, -.26F, 0), j(-.40F, .70F, .30F), j(-.95F, -.30F, -.55F),
			j(-.50F, -.14F, -.08F), j(.50F, .12F, .08F), .90F, .30F, h(.22F, .02F, .08F, 30, 34, -40)),
		pose(j(.75F, -.30F, -.08F), j(-.34F, .12F, 0), j(-.80F, -.30F, .20F), j(-1.10F, .45F, -.80F),
			j(-1.25F, .08F, -.12F), j(.82F, -.08F, .10F), 1.40F, -2.00F, h(-.06F, .04F, -.60F, -98, -14, -6)),
		pose(j(.48F, -.20F, -.04F), j(-.20F, .08F, 0), j(-.75F, -.20F, .12F), j(-.95F, .30F, -.65F),
			j(-.85F, .08F, -.08F), j(.62F, -.08F, .08F), 1.05F, -1.20F, h(-.06F, .02F, -.46F, -54, -12, -10)));

	// Worldforge lifts the blade straight up over the head and falls with the whole body behind it, kneeling into the ground.
	private static final Motion WORLDFORGE_FORM = new Motion(
		pose(j(-.36F, -.10F, .02F), j(.20F, .04F, 0), j(-3.10F, -.20F, .10F), j(-3.00F, .20F, -.10F),
			j(-.15F, .04F, -.04F), j(.15F, -.04F, .04F), -.70F, .45F, h(.02F, .42F, .06F, -110, -6, 6)),
		pose(j(.82F, .08F, -.02F), j(-.38F, -.04F, 0), j(-.20F, .15F, -.04F), j(-.30F, -.15F, .04F),
			j(-1.40F, -.04F, -.12F), j(1.10F, .04F, .10F), 2.10F, -1.30F, h(.04F, -.05F, -.60F, 86, 6, -6)),
		pose(j(.66F, .06F, -.02F), j(-.30F, -.02F, 0), j(-.18F, .10F, -.04F), j(-.26F, -.10F, .04F),
			j(-1.15F, -.04F, -.10F), j(.92F, .04F, .08F), 1.80F, -1.00F, h(.02F, -.03F, -.50F, 80, 2, -4)));

	// ------------------------------------------------------------------ Dune

	// Grit Flick crouches with the point almost on the sand, then snaps it up and across the eyes from the wrist.
	private static final Motion GRIT = new Motion(
		pose(j(.45F, .35F, .06F), j(-.22F, -.16F, 0), j(.20F, .50F, .45F), j(-.60F, -.30F, -.40F),
			j(-.90F, -.14F, -.14F), j(.70F, .14F, .14F), 1.45F, .05F, h(.10F, .02F, -.20F, 62, 22, -26)),
		pose(j(.20F, -.30F, -.04F), j(-.06F, .14F, 0), j(-2.20F, -.55F, -.35F), j(-.80F, .10F, -.55F),
			j(-.60F, .08F, -.08F), j(.45F, -.08F, .08F), .85F, -.45F, h(-.20F, .34F, -.24F, -84, -30, 24)),
		pose(j(.24F, -.22F, -.02F), j(-.08F, .10F, 0), j(-1.90F, -.40F, -.30F), j(-.70F, .06F, -.45F),
			j(-.66F, .08F, -.08F), j(.48F, -.08F, .08F), .95F, -.30F, h(-.16F, .28F, -.18F, -72, -24, 20)));

	// Quicksand drives the point down into the ground ahead, both hands on the hilt, as the sand gives way.
	private static final Motion QUICKSAND_FORM = new Motion(
		pose(j(-.14F, .24F, -.04F), j(.08F, -.10F, 0), j(-2.70F, .40F, -.30F), j(-2.40F, -.30F, .25F),
			j(-.36F, -.08F, -.06F), j(.30F, .08F, .06F), .10F, .25F, h(.08F, .40F, .04F, -96, 16, -14)),
		pose(j(.62F, -.14F, .04F), j(-.28F, .06F, 0), j(-.15F, -.30F, -.10F), j(-.25F, .25F, .10F),
			j(-1.10F, .06F, -.10F), j(.78F, -.06F, .08F), 1.60F, -1.15F, h(-.08F, .04F, -.54F, 40, -14, 12)),
		pose(j(.50F, -.12F, .04F), j(-.22F, .06F, 0), j(-.10F, -.25F, -.08F), j(-.20F, .20F, .08F),
			j(-.95F, .06F, -.10F), j(.66F, -.06F, .08F), 1.40F, -.90F, h(-.06F, .04F, -.46F, 36, -10, 10)));

	// Sandveil drops into a low pivot, the blade skimming a full circle round the feet to throw the sand up.
	private static final Motion SANDVEIL_FORM = new Motion(
		pose(j(.35F, .75F, .10F), j(-.16F, -.34F, 0), j(-.30F, 1.10F, .55F), j(-.50F, -.60F, -.50F),
			j(-.95F, -.16F, -.14F), j(.75F, .16F, .14F), 1.55F, .10F, h(.24F, .04F, -.10F, 44, 44, -48)),
		pose(j(.40F, -1.25F, -.10F), j(-.18F, .50F, 0), j(-.25F, -1.40F, -.50F), j(-.45F, .55F, -.60F),
			j(-1.00F, .18F, -.12F), j(.78F, -.18F, .12F), 1.70F, -.20F, h(-.42F, .00F, -.16F, 40, -60, 52)),
		pose(j(.32F, -.95F, -.06F), j(-.14F, .40F, 0), j(-.35F, -1.15F, -.40F), j(-.42F, .45F, -.50F),
			j(-.82F, .14F, -.10F), j(.64F, -.14F, .10F), 1.45F, -.10F, h(-.34F, .02F, -.12F, 36, -48, 44)));

	// Dune Runner skims the ground: body low and forward, blade trailing back at the hip, then a quick cut through on the pass.
	private static final Motion RUNNER = new Motion(
		pose(j(.40F, .40F, -.04F), j(-.20F, -.20F, 0), j(.10F, .85F, .20F), j(-.70F, -.25F, -.45F),
			j(-.70F, -.10F, -.10F), j(.60F, .10F, .10F), 1.20F, .20F, h(.20F, .00F, .02F, 42, 36, -34)),
		pose(j(.68F, -.36F, .04F), j(-.32F, .14F, 0), j(-1.35F, -.60F, -.25F), j(.30F, -.35F, -.20F),
			j(-1.20F, .08F, -.12F), j(.85F, -.08F, .10F), 1.55F, -1.80F, h(-.20F, .03F, -.58F, -86, -26, 16)),
		pose(j(.46F, -.26F, .04F), j(-.22F, .10F, 0), j(-1.20F, -.45F, -.20F), j(.20F, -.25F, -.25F),
			j(-.86F, .08F, -.10F), j(.66F, -.08F, .08F), 1.20F, -1.05F, h(-.18F, .02F, -.44F, -78, -20, 14)));

	// Sea of Sand spirals up from a crouch with both arms sweeping wide, then lets the dunes fall in a broad level cut.
	private static final Motion SEA = new Motion(
		pose(j(.30F, .95F, .08F), j(-.14F, -.42F, 0), j(-.55F, 1.25F, .40F), j(-.40F, -.85F, -.55F),
			j(-.85F, -.16F, -.12F), j(.66F, .16F, .12F), 1.35F, .15F, h(.22F, .10F, -.06F, 20, 50, -40)),
		pose(j(-.05F, -.95F, -.06F), j(.04F, .40F, 0), j(-1.70F, -1.25F, -.10F), j(-1.30F, .85F, .20F),
			j(-.40F, .14F, -.08F), j(.32F, -.14F, .08F), .20F, -.55F, h(-.40F, .24F, -.26F, -26, -56, 38)),
		pose(j(.06F, -.70F, -.04F), j(-.02F, .30F, 0), j(-1.50F, -1.00F, -.10F), j(-1.10F, .65F, .15F),
			j(-.36F, .12F, -.06F), j(.30F, -.12F, .06F), .30F, -.35F, h(-.32F, .20F, -.20F, -20, -44, 30)));

	/** The motion for one of the pack's ids, or null. */
	static Motion motion(int id) {
		return switch (id) {
			case RIPTIDE_CUT -> RIPTIDE;
			case BREAKER -> BREAKER_FORM;
			case WHIRLPOOL -> WHIRLPOOL_FORM;
			case SURGE -> SURGE_FORM;
			case MAELSTROM -> MAELSTROM_FORM;
			case SUNDER_CUT -> SUNDER;
			case ANVIL_FALL -> ANVIL;
			case BULWARK -> BULWARK_FORM;
			case FORGE_CHARGE -> FORGE;
			case WORLDFORGE -> WORLDFORGE_FORM;
			case GRIT_FLICK -> GRIT;
			case QUICKSAND -> QUICKSAND_FORM;
			case SANDVEIL -> SANDVEIL_FORM;
			case DUNE_RUNNER -> RUNNER;
			case SEA_OF_SAND -> SEA;
			default -> null;
		};
	}

	/** The point leads on the three step arts and the surge; Quicksand plants it in the ground like the other fields. */
	static boolean thrust(int id) { return id == SURGE || id == FORGE_CHARGE || id == DUNE_RUNNER; }
	static boolean planted(int id) { return id == QUICKSAND; }
}
