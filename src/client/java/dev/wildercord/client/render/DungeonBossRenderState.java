package dev.wildercord.client.render;

import dev.wildercord.cast.DungeonBoss;
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
	public int rootBindings = 3;
	/** Its own move under way (0: none, see {@code DungeonBoss#currentMove}), ticks into it with the partial tick, and its tell. */
	public int move;
	public float since;
	public int tell;
	/** How far each of its own moves' poses has eased in, by move id, so one blends into the next. */
	public final float[] moves = new float[DungeonBoss.MOVES];
	/** Whatever else a boss's pose wants of it, its own way (see {@code DungeonBoss#extra}). */
	public final float[] extra = new float[DungeonBoss.EXTRA];
}
