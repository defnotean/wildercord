package dev.wildercord.aura;

/** Original brace, kick and landing poses sampled only from accepted phase/step events. No combat release frame exists. */
public final class WallTurnAnimation {
	private WallTurnAnimation() {}
	private static MastersArtAnimation.Joint j(float x, float y, float z) { return new MastersArtAnimation.Joint(x, y, z); }
	private static final MastersArtAnimation.Pose BRACE = new MastersArtAnimation.Pose(1,
		j(.12F, .15F, -.10F), j(-.10F, -.10F, .08F), j(-.6F, -.25F, .35F), j(-1.5F, .10F, -.30F),
		j(-.95F, .12F, -.08F), j(.20F, -.10F, .10F), .5F, -.25F,
		new MastersArtAnimation.Hand(.08F, .06F, -.06F, -14, 12, -12));
	private static final MastersArtAnimation.Pose KICK = new MastersArtAnimation.Pose(1,
		j(-.15F, -.25F, .13F), j(.12F, .12F, -.08F), j(-.8F, -.15F, .30F), j(-.70F, .3F, -.40F),
		j(.55F, -.10F, -.12F), j(-.95F, .10F, .12F), .15F, 0,
		new MastersArtAnimation.Hand(-.12F, -.04F, -.12F, 12, -18, 16));
	private static final MastersArtAnimation.Pose LAND = new MastersArtAnimation.Pose(1,
		j(.30F, 0, 0), j(-.20F, 0, 0), j(-.4F, -.15F, .15F), j(-.5F, .15F, -.15F),
		j(-.45F, 0, -.05F), j(.35F, 0, .05F), .9F, -.2F,
		new MastersArtAnimation.Hand(0, -.12F, -.05F, 10, 0, 0));
	public static MastersArtAnimation.Pose sample(int phase, int remaining, float age) {
		if (!Float.isFinite(age) || age < 0 || remaining < 0 || remaining > WallTurnRules.REST_TICKS) return MastersArtAnimation.NONE;
		return switch (phase) {
			case WallTurnRules.BRACE -> age >= remaining ? MastersArtAnimation.NONE : BRACE.weight(Math.min(1, (age + 1) / 2));
			case WallTurnRules.KICK -> age > 3 ? MastersArtAnimation.NONE : BRACE.toward(KICK,
				Math.clamp((WallTurnRules.KICK_TICKS - remaining + Math.min(1, age)) / WallTurnRules.KICK_TICKS, 0, 1));
			case WallTurnRules.LAND -> age >= remaining ? MastersArtAnimation.NONE : LAND.weight(1 - age / Math.max(1, remaining));
			// Begins exactly at the final push (full weight), so the kick-to-fall handoff never pops.
			case WallTurnRules.FALL -> age >= 5 ? MastersArtAnimation.NONE : KICK.weight(1 - age / 5);
			case WallTurnRules.ABORT -> age >= 3 ? MastersArtAnimation.NONE : BRACE.weight((1 - age / 3) * .3F);
			default -> MastersArtAnimation.NONE;
		};
	}
}
