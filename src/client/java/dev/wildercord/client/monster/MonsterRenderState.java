package dev.wildercord.client.monster;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** A monster of the wilds' pose, eased on the client (see {@code WildMonster}): each blend runs 0 to 1. */
public class MonsterRenderState extends LivingEntityRenderState {
	/** The tell before its signature attack. */
	public float windup;
	/** The attack itself. */
	public float acting;
	/** Its defence up (curled), or a frog's gulp. */
	public float guard;
	/** Caught out, open to a counterblow. */
	public float stunned;
	/** Hidden in the dark (the Gloomstalker): 1 fully hidden. */
	public float veil;
	/** A second move (calling lightning, a mouth opening, slinking away). */
	public float alt;
	/** A frog that has just eaten (its throat stays swollen). */
	public boolean engorged;
	/** 0.12: the tint of its biome variant (white for none). */
	public int variantTint = 0xFFFFFF;
}
