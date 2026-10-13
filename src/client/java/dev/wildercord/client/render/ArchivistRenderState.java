package dev.wildercord.client.render;

import dev.wildercord.cast.ArchivistRules;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ArchivistRenderState extends LivingEntityRenderState {
	/** How far into the casting pose: arms raised, tome lifted, its pages flying. 0 to 1. */
	public float casting;
	/** How far into the rewriting pose: arms thrown wide, head back, the pages flung far out. 0 to 1. */
	public float rewriting;
	/** The trick it is at (see {@link ArchivistRules}), or none. */
	public int move;
	/** How long that trick is told before it is thrown, in ticks. */
	public int tell;
	/** How long that trick has been going, in ticks. */
	public float since;
	/** How far into each trick's pose, 0 to 1, eased in and out as one gives way to the next. */
	public final float[] moves = new float[ArchivistRules.MOVES];
}
