package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What the Bell-Warden's model needs: its move, how long into it, its tell, and how far up off its knees it is. */
public final class BellWardenRenderState extends LivingEntityRenderState {
	public int move,tell;
	public float elapsed,rise;
}
