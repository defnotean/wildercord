package dev.wildercord.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ArchivistRenderState extends LivingEntityRenderState {
	/** How far into the casting pose: arms raised, tome lifted, its pages flying. 0 to 1. */
	public float casting;
	/** How far into the rewriting pose: arms thrown wide, head back, the pages flung far out. 0 to 1. */
	public float rewriting;
}
