package dev.wildercord.cast.feel;

/** The moments of a cast a {@link Signature} can decorate or take over. */
public enum Phase {
	/** The first 200 ms: the spell leaves the hand (once per cast, for the first group). */
	CUE,
	/** Each tick a projectile flies (a Bolt or Arc today). */
	TRAVEL,
	/** Where a shape lands (Bolt, Beam, Touch, Chain jumps, Bounces). */
	IMPACT,
	/** After an impact of band M or bigger: what the spell leaves behind. */
	AFTERMATH,
	/** Each creature an effect touches (replaces the generic touched glow). */
	HIT
}
