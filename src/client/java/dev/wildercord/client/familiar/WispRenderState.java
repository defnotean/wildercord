package dev.wildercord.client.familiar;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WispRenderState extends LivingEntityRenderState {
	/** The element's colour (0xRRGGBB). */
	public int color = 0xFFFFFF;
	/** Its level as a familiar (1-3; wild wisps are 1): higher levels are drawn a little bigger. */
	public int level = 1;
	/** 1 just as it flares (a cast, a chime, a bond), fading to 0 over half a second. */
	public float flare;
	/** How fast it's climbing (blocks per tick), which lifts its tail. */
	public float climb;
}
