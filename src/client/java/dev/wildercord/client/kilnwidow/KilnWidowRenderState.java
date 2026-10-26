package dev.wildercord.client.kilnwidow;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class KilnWidowRenderState extends LivingEntityRenderState {
	public int move;
	public float elapsed;
	public float tellProgress;
	public float aim;
	public float markX;
	public float markZ;
	public boolean acrossX = true;
	public boolean second;
	public float hurt;
}
