package dev.wildercord.client.ferryman;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class FerrymanRenderState extends LivingEntityRenderState {
	public int move;
	public float elapsed, aim, markX, markZ;
	public boolean acrossX, second;
	public float hurt;
}
