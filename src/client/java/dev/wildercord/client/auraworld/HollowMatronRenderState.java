package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the Hollow Matron's model needs: her move and how far into it, the move before (to blend out of), how far through her drop she is, and how far up her thread runs. */
public final class HollowMatronRenderState extends LivingEntityRenderState {
	public int move,tell,side,prevMove,prevTell;
	public float elapsed,drop,prevElapsed,blend=1,hurt,threadUp;
}
