package dev.wildercord.client.trials;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Render state shared by lightweight BedrockGeoRig creatures and guardians. */
public final class GeoMobRenderState extends EntityRenderState {
	public float bodyYaw;
	public float hurtFlash;
	public float deathTime;
	public float walkPos;
	public float walkSpeed;
	public int move;
	public float elapsedTicks;
}
