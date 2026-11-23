package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-side snapshot of {@link dev.wildercord.aura.world.OathboundHalberdier} animation state. */
public final class OathboundHalberdierRenderState extends LivingEntityRenderState {
	public int move;
	public int tell;
	public float elapsed;
	public boolean empowered;
	public float hurt;

	public int prevMove = -1;
	public int prevTell;
	public float prevElapsed;
	public float blend = 1.0F;
}
