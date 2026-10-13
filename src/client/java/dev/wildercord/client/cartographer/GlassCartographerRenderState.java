package dev.wildercord.client.cartographer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public final class GlassCartographerRenderState extends LivingEntityRenderState {
	public final AnimationState idle = new AnimationState();
	public final AnimationState sweep = new AnimationState();
	public final AnimationState plumb = new AnimationState();
	public final AnimationState grid = new AnimationState();
	public int move;
	public float tellProgress;
	public float aim;
	public float markX;
	public float markZ;
	public boolean acrossX = true;
	public boolean second;
}
