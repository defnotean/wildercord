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
	/** A Strawman's stance, the way it stands when it's caught (see {@code StrawmanRules#STANCES}). */
	public int stance;
	/** 0.12: the tint of its biome variant (white for none). */
	public int variantTint = 0xFFFFFF;
	/** 0.13: whether it's a giant, fighting by the moves below ({@code GiantRules.Bramble}, {@code GiantRules.Matriarch}). */
	public boolean giant;
	/** An elite's move under way, by its rules' id (the Sandmaw Wyrm's, {@code SandmawRules}; the Timbermaul Bear's, {@code TimbermaulRules}; a giant's). */
	public int move;
	/** Ticks since that move began. */
	public float since;
	/** How long that move's tell was given. */
	public int tell;
	/** Whether it has gone roused at half health. */
	public boolean roused;
	/** Which side an elite's move comes from: 1 its right, -1 its left (the Timbermaul Bear's rake). */
	public int side = 1;
	/** How far each of an elite's moves' poses has eased in, 0 to 1, by move id. */
	public final float[] moves = new float[24];
}
