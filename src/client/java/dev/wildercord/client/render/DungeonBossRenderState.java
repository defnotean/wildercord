package dev.wildercord.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** A dungeon boss's pose, eased on the client (see {@code DungeonBoss}): each blend runs 0 to 1. */
public class DungeonBossRenderState extends LivingEntityRenderState {
	/** Telegraphing a spell. */
	public float casting;
	/** Between phases. */
	public float shifting;
	/** Its signature defence up (the Star-Eater's shard shield). */
	public float guarded;
	/** Open to harm: cracked, shield broken, stranded. */
	public float exposed;
	/** Winding up a heavy blow. */
	public float slamming;
	/** Its phase, 1 to 3. */
	public int phase = 1;
}
