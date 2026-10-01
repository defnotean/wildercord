package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** A duelist's or a fallen knight's pose, eased on the client (see {@code aura.world.AuraFighter}): each blend runs 0 to 1. */
public class AuraFighterRenderState extends HumanoidRenderState {
	/** Its aura's colour (0xRRGGBB) and stage. */
	public int color = 0xFFFFFF;
	public int stage = 1;
	/** Its blade raised high: a slash coming. */
	public float windup;
	/** Its guard up. */
	public float guard;
	/** Caught out. */
	public float stagger;
	/** A bow. */
	public float bow;
	/** On one knee (a duelist yielding). */
	public float yield;
	/** Its blade drawn. */
	public float drawn;
	/** Sitting by its fire. */
	public float sit;
	/** Crouched for a dash. */
	public float dash;
	/** Which duelist it is (its method's id: its cloak's texture). */
	public String method = "ember";
}
