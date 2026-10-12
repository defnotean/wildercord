package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What {@link LanternBearerModel} poses from: its move and how far into it, the move it is blending out of, and how far up out of its grave it is. */
public final class LanternBearerRenderState extends LivingEntityRenderState {
	public int move,tell,side,prevMove,prevTell;
	public float elapsed,rise,prevElapsed,blend=1,hurt;
	/** Whether it has held its rite, half beaten, and fights on roused. */
	public boolean roused;
	/** How solid it is, 1 whole to near 0 gone to smoke. */
	public float solid=1;
}
