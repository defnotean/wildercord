package dev.wildercord.aura;

/**
 * Classic body and first-person hand poses for the field forms, sampled from accepted phase events by age alone, so the
 * motion stays continuous between events. No camera motion: stable-camera play sees only the body and the blade hand.
 */
public final class FormDashAnimation {
	private FormDashAnimation() {}
	private static MastersArtAnimation.Joint j(float x, float y, float z) { return new MastersArtAnimation.Joint(x, y, z); }
	/** Cinder Lunge's tell: planted low, blade drawn back to the hip. Readable from the front. */
	private static final MastersArtAnimation.Pose SET = new MastersArtAnimation.Pose(1,
		j(.35F, .30F, 0), j(-.30F, -.25F, 0), j(.55F, .25F, .25F), j(-.90F, .20F, -.35F),
		j(-.65F, 0, -.08F), j(.50F, 0, .08F), 1.4F, -.6F,
		new MastersArtAnimation.Hand(.10F, -.10F, .14F, 18, 22, -10));
	private static final MastersArtAnimation.Pose LUNGE = new MastersArtAnimation.Pose(1,
		j(.55F, .05F, 0), j(-.50F, 0, 0), j(-1.25F, .10F, .20F), j(.45F, 0, -.25F),
		j(-.95F, 0, -.05F), j(.85F, 0, .05F), 1.0F, .8F,
		new MastersArtAnimation.Hand(.04F, .02F, -.18F, -10, 6, 0));
	private static final MastersArtAnimation.Pose CUT = new MastersArtAnimation.Pose(1,
		j(.30F, -.55F, 0), j(-.25F, .45F, 0), j(-1.35F, -.95F, -.35F), j(-.35F, .30F, -.45F),
		j(-.70F, 0, -.05F), j(.55F, 0, .05F), 1.1F, .4F,
		new MastersArtAnimation.Hand(-.18F, .02F, -.10F, -4, -34, 22));
	/** Reed Slip leans off the line with the guard still facing it: the body moves, the blade does not turn away. */
	private static final MastersArtAnimation.Pose SLIP = new MastersArtAnimation.Pose(1,
		j(.10F, .15F, .32F), j(-.05F, -.15F, -.30F), j(-.85F, .20F, .20F), j(-1.05F, -.15F, -.40F),
		j(-.20F, 0, -.45F), j(.15F, 0, .35F), .8F, -.2F,
		new MastersArtAnimation.Hand(.06F, -.06F, -.04F, -6, -14, 16));
	/** Recovery reads as weight still on the heels: the punish window. */
	private static final MastersArtAnimation.Pose SETTLE = new MastersArtAnimation.Pose(1,
		j(.25F, 0, .05F), j(-.15F, 0, 0), j(-.45F, -.10F, .15F), j(-.55F, .15F, -.15F),
		j(-.35F, 0, -.10F), j(.30F, 0, .08F), 1.0F, -.15F,
		new MastersArtAnimation.Hand(0, -.14F, -.04F, 12, 0, 0));
	private static final MastersArtAnimation.Pose STALL = new MastersArtAnimation.Pose(1,
		j(-.20F, .10F, -.10F), j(.25F, 0, 0), j(-.20F, .30F, .45F), j(-.40F, -.20F, -.55F),
		j(.25F, 0, -.10F), j(-.30F, 0, .10F), .5F, -.5F,
		new MastersArtAnimation.Hand(.08F, -.16F, .06F, 20, 10, -8));
	// ---- moves pack
	/** Gale Shove's tell: weight on the back foot, the free palm squared to the foe, the blade kept back and low. */
	private static final MastersArtAnimation.Pose SHOVE_SET = new MastersArtAnimation.Pose(1,
		j(.20F, .35F, 0), j(-.15F, -.30F, 0), j(.35F, .30F, .20F), j(-.70F, -.10F, -.20F),
		j(-.40F, 0, -.10F), j(.45F, 0, .10F), 1.2F, -.4F,
		new MastersArtAnimation.Hand(.12F, -.06F, .10F, 14, 18, -6));
	private static final MastersArtAnimation.Pose SHOVE = new MastersArtAnimation.Pose(1,
		j(.30F, -.30F, 0), j(-.20F, .25F, 0), j(-.40F, -.25F, .10F), j(-1.45F, .20F, -.10F),
		j(-.70F, 0, -.05F), j(.60F, 0, .05F), 1.0F, .5F,
		new MastersArtAnimation.Hand(.02F, -.04F, -.08F, -6, -10, 8));
	/** Stone Break's tell: the blade lifted high in both hands, the longest and plainest wind-up of the three. */
	private static final MastersArtAnimation.Pose BREAK_SET = new MastersArtAnimation.Pose(1,
		j(-.15F, .10F, 0), j(.10F, -.05F, 0), j(-2.60F, .25F, .15F), j(-2.40F, -.25F, -.15F),
		j(-.30F, 0, -.12F), j(.25F, 0, .12F), 1.6F, -.8F,
		new MastersArtAnimation.Hand(.06F, .10F, .12F, 34, 6, -6));
	private static final MastersArtAnimation.Pose BREAK = new MastersArtAnimation.Pose(1,
		j(.60F, 0, 0), j(-.40F, 0, 0), j(-.70F, .10F, .05F), j(-.60F, -.10F, -.05F),
		j(-.75F, 0, -.10F), j(.55F, 0, .10F), 1.2F, .5F,
		new MastersArtAnimation.Hand(-.04F, -.02F, -.16F, -30, -6, 4));
	/** Air Step: knees drawn up, arms spread for balance, the blade level and in view. */
	private static final MastersArtAnimation.Pose RISE = new MastersArtAnimation.Pose(1,
		j(-.10F, 0, 0), j(.15F, 0, 0), j(-.60F, .20F, .55F), j(-.60F, -.20F, -.55F),
		j(-1.10F, 0, -.10F), j(-.40F, 0, .10F), .6F, -.3F,
		new MastersArtAnimation.Hand(.10F, .04F, -.02F, 6, 12, 10));
	/** Plunging Strike: the set holds the blade overhead in the air, the dive drives the point straight down. */
	private static final MastersArtAnimation.Pose PLUNGE_SET = new MastersArtAnimation.Pose(1,
		j(-.20F, 0, 0), j(.20F, 0, 0), j(-2.80F, .10F, .10F), j(-2.70F, -.10F, -.10F),
		j(-.80F, 0, -.10F), j(-.60F, 0, .10F), .8F, -.5F,
		new MastersArtAnimation.Hand(.04F, .12F, .10F, 40, 0, 0));
	private static final MastersArtAnimation.Pose DIVE = new MastersArtAnimation.Pose(1,
		j(.70F, 0, 0), j(-.50F, 0, 0), j(-.30F, 0, .05F), j(-.25F, 0, -.05F),
		j(-.20F, 0, -.05F), j(-.15F, 0, .05F), .8F, .6F,
		new MastersArtAnimation.Hand(0, .04F, -.10F, -48, 0, 0));
	/** The heavy landing: deep crouch, blade driven into the ground in front. */
	private static final MastersArtAnimation.Pose LANDING = new MastersArtAnimation.Pose(1,
		j(.75F, 0, 0), j(-.50F, 0, 0), j(-.85F, 0, .10F), j(-.55F, -.20F, -.30F),
		j(-1.20F, 0, -.15F), j(.30F, 0, .15F), 1.4F, .2F,
		new MastersArtAnimation.Hand(-.02F, -.04F, -.14F, -40, -4, 2));
	/** Spell Cut: one flat cross-cut through the line of the spell. */
	private static final MastersArtAnimation.Pose SEVER = new MastersArtAnimation.Pose(1,
		j(.20F, .60F, 0), j(-.15F, -.45F, 0), j(-1.50F, 1.00F, .30F), j(-.30F, -.30F, -.35F),
		j(-.45F, 0, -.05F), j(.40F, 0, .05F), 1.0F, .3F,
		new MastersArtAnimation.Hand(.16F, .04F, -.08F, -2, 34, -24));
	private static MastersArtAnimation.Pose set(int form) {
		return switch (form) {
			case FormDashRules.SHOVE -> SHOVE_SET; case FormDashRules.GUARD_BREAK -> BREAK_SET; case FormDashRules.PLUNGE -> PLUNGE_SET; default -> SET;
		};
	}
	private static MastersArtAnimation.Pose strike(int form) { return form == FormDashRules.SHOVE ? SHOVE : BREAK; }
	public static MastersArtAnimation.Pose sample(int form, int phase, int remaining, float age) {
		if (!FormDashRules.form(form) || !Float.isFinite(age) || age < 0 || remaining < 0 || remaining > FormDashRules.MAX_TICKS) return MastersArtAnimation.NONE;
		float span = Math.max(1, remaining);
		return switch (phase) {
			case FormDashRules.SET -> age >= remaining + 1 ? MastersArtAnimation.NONE : set(form).weight(Math.min(1, (age + 1) / 2));
			case FormDashRules.LUNGE -> age >= remaining + 2 ? MastersArtAnimation.NONE : SET.toward(LUNGE, smooth(Math.min(1, (age + 1) / 2)));
			case FormDashRules.CUT -> age >= remaining ? MastersArtAnimation.NONE : age < 3 ? LUNGE.toward(CUT, smooth((age + 1) / 3))
				: CUT.toward(SETTLE, smooth((age - 3) / Math.max(1, span - 3))).weight(1 - Math.max(0, age - 3) / Math.max(1, span - 3) * .9F);
			case FormDashRules.SLIP -> age >= remaining + 2 ? MastersArtAnimation.NONE : SLIP.weight(Math.min(1, (age + 1) / 2));
			case FormDashRules.RECOVER -> age >= remaining ? MastersArtAnimation.NONE : SLIP.toward(SETTLE, smooth(Math.min(1, age / 3))).weight(1 - age / span);
			case FormDashRules.STALL -> age >= remaining ? MastersArtAnimation.NONE : STALL.weight(Math.min(1, (age + 1) / 2) * (1 - age / span));
			case FormDashRules.ABORT -> age >= 4 ? MastersArtAnimation.NONE : STALL.weight((1 - age / 4) * .5F);
			case FormDashRules.STRIKE -> age >= remaining ? MastersArtAnimation.NONE : age < 3 ? set(form).toward(strike(form), smooth((age + 1) / 3))
				: strike(form).toward(SETTLE, smooth((age - 3) / Math.max(1, span - 3))).weight(1 - Math.max(0, age - 3) / Math.max(1, span - 3) * .9F);
			case FormDashRules.RISE -> age >= remaining + 4 ? MastersArtAnimation.NONE : RISE.weight(Math.min(1, (age + 1) / 2));
			case FormDashRules.DIVE -> age >= remaining + 2 ? MastersArtAnimation.NONE : PLUNGE_SET.toward(DIVE, smooth(Math.min(1, (age + 1) / 2)));
			case FormDashRules.LAND -> age >= remaining ? MastersArtAnimation.NONE
				: (form == FormDashRules.PLUNGE ? LANDING : RISE.toward(SETTLE, smooth(Math.min(1, age / 2)))).weight(1 - age / span * .9F);
			case FormDashRules.SPLASH -> age >= remaining ? MastersArtAnimation.NONE : STALL.toward(RISE, .4F).weight(Math.min(1, (age + 1) / 2) * (1 - age / span));
			case FormDashRules.SEVER -> age >= remaining ? MastersArtAnimation.NONE : age < 2 ? SET.toward(SEVER, smooth((age + 1) / 2))
				: SEVER.toward(SETTLE, smooth((age - 2) / Math.max(1, span - 2))).weight(1 - Math.max(0, age - 2) / Math.max(1, span - 2) * .9F);
			case FormDashRules.MISS -> age >= remaining ? MastersArtAnimation.NONE : SEVER.toward(STALL, smooth(Math.min(1, age / 3))).weight(1 - age / span);
			default -> MastersArtAnimation.NONE;
		};
	}
	private static float smooth(float t) { t = Math.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
}
