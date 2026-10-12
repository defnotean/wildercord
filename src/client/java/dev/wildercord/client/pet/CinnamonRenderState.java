package dev.wildercord.client.pet;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class CinnamonRenderState extends LivingEntityRenderState {
	public boolean sitting;
	public boolean sleeping, playing, greeting, ringing, tongue, wearingBow, exhausted;
	/** How far into a lick she is, in ticks (with the partial tick); negative when she isn't licking. */
	public float lick = -1;
}
