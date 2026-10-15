package dev.wildercord.client.cartographer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class GlassCartographerRenderState extends LivingEntityRenderState {
	public int move;
	public float elapsed;
	public float tellProgress;
	public float aim;
	public float markX;
	public float markZ;
	public boolean acrossX = true;
	public boolean second;
}
