package dev.wildercord.client.familiar;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WispRenderState extends LivingEntityRenderState {
	/** The element's colour (0xRRGGBB). */
	public int color = 0xFFFFFF;
	/** Its level as a familiar (1-3; wild wisps are 1): higher levels are drawn a little bigger and brighter. */
	public int level = 1;
	/** 1 just as it flares (a cast, a chime, a bond), fading to 0 over half a second. */
	public float flare;
	/** Where its tail lies: points behind it (x, y, z, relative to its middle), nearest first. */
	public float[] tail = new float[0];
	/** How fast it's moving (blocks a tick). */
	public float speed;
	/** A number of its own, so wisps side by side don't pulse and flicker in step. */
	public float seed;
	/** Whether the player is looking right at it (its nameplate shows clearly). */
	public boolean looked;
}
