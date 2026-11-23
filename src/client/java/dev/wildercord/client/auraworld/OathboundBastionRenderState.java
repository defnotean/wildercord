package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-side render state for {@link dev.wildercord.aura.world.OathboundBastion}. */
public final class OathboundBastionRenderState extends LivingEntityRenderState {
	public int move;
	public int tell;
	public float elapsed;
	public int prevMove;
	public int prevTell;
	public float prevElapsed;
	public float blend = 1.0F;
	public float hurt;
	public float barrier;
	public boolean empowered;
}
