package dev.wildercord.aura;

/** Original plant, catch and turn poses sampled only from accepted phase events. The weapon never swings. */
public final class StoneHingeAnimation {
	private StoneHingeAnimation() {}
	private static MastersArtAnimation.Joint j(float x, float y, float z) { return new MastersArtAnimation.Joint(x, y, z); }
	private static final MastersArtAnimation.Pose PLANT = new MastersArtAnimation.Pose(1,
		j(.20F, 0, 0), j(-.08F, 0, 0), j(-1.1F, -.35F, .20F), j(-1.2F, .35F, -.20F),
		j(-.35F, 0, -.22F), j(.30F, 0, .22F), .8F, -.10F,
		new MastersArtAnimation.Hand(.04F, -.04F, -.06F, -8, 6, -6));
	private static final MastersArtAnimation.Pose CATCH = new MastersArtAnimation.Pose(1,
		j(.28F, 0, 0), j(-.12F, 0, 0), j(-1.4F, -.55F, .25F), j(-1.45F, .55F, -.25F),
		j(-.45F, 0, -.26F), j(.38F, 0, .26F), 1.1F, -.15F,
		new MastersArtAnimation.Hand(.06F, -.02F, -.10F, -12, 10, -8));
	private static final MastersArtAnimation.Pose TURN = new MastersArtAnimation.Pose(1,
		j(.15F, .45F, 0), j(-.05F, -.30F, 0), j(-.9F, .15F, .35F), j(-1.3F, .75F, -.10F),
		j(-.30F, .20F, -.20F), j(.25F, -.20F, .20F), .7F, 0,
		new MastersArtAnimation.Hand(-.10F, -.02F, -.08F, 6, -16, 12));
	public static MastersArtAnimation.Pose sample(int phase, int remaining, float age) {
		if (!Float.isFinite(age) || age < 0 || remaining < 0 || remaining > StoneHingeRules.CATCH_TICKS) return MastersArtAnimation.NONE;
		return switch (phase) {
			case StoneHingeRules.BRACE -> age >= remaining ? MastersArtAnimation.NONE : PLANT.weight(Math.min(1, (age + 1) / 2));
			case StoneHingeRules.CATCH -> age >= remaining ? MastersArtAnimation.NONE : PLANT.toward(CATCH, Math.min(1, (age + 1) / 2));
			case StoneHingeRules.TURN -> age >= remaining ? MastersArtAnimation.NONE : TURN.weight(1 - age / Math.max(1, remaining));
			case StoneHingeRules.SPENT -> age >= 4 ? MastersArtAnimation.NONE : CATCH.weight((1 - age / 4) * .5F);
			default -> MastersArtAnimation.NONE;
		};
	}
	/** One dispatch for the shared Master form event channel. */
	public static MastersArtAnimation.Pose sampleForm(int phase, int remaining, float age) {
		return phase >= StoneHingeRules.BRACE ? sample(phase, remaining, age) : WallTurnAnimation.sample(phase, remaining, age);
	}
}
