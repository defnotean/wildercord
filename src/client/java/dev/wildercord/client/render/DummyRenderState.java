package dev.wildercord.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DummyRenderState extends LivingEntityRenderState {
	/** Ticks left of the last hit's shake (10 to 0). */
	public float hurt;
}
