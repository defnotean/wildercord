package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-side snapshot of {@link dev.wildercord.aura.world.OathboundBladeChampion} animation and riposte state. */
public final class OathboundBladeChampionRenderState extends LivingEntityRenderState {
	public int move;
	public int tell;
	public float elapsed;
	public boolean riposteTriggered;
	public float riposteCounterElapsed;
	public boolean empowered;
	public float hurt;

	public int prevMove = -1;
	public int prevTell;
	public float prevElapsed;
	public boolean prevRiposteTriggered;
	public float prevRiposteCounterElapsed;
	public float blend = 1.0F;
}
