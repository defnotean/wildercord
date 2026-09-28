package dev.wildercord.client;

/**
 * What a player's model needs to strike a casting pose, carried on its render state: the shape
 * being cast or charged, how far through the pose it is, and whether it's a charge being held.
 */
public interface CastingPose {
	String wildercord$shape();

	float wildercord$progress();

	boolean wildercord$charging();

	void wildercord$setPose(String shape, float progress, boolean charging);

	/** The worn Cord's look (see {@code WildercordAttachments.CordLook}), for the Cord layer. */
	dev.wildercord.player.WildercordAttachments.CordLook wildercord$cord();

	void wildercord$setCord(dev.wildercord.player.WildercordAttachments.CordLook cord);

	/** How bright the beads burn right now: 1 at rest, brighter while charging or just after a cast. */
	float wildercord$glow();

	void wildercord$setGlow(float glow);
}
