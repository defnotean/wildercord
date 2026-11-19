package dev.wildercord.client.ferryman;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class FerrymanRenderState extends LivingEntityRenderState {
	public int move;
	public float elapsed, aim, markX, markZ, markX2, markZ2;
	public boolean acrossX, second, paired;
	public float hurt;
}
