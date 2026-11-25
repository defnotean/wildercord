package dev.wildercord.client.trials;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Client render snapshot for Nyxara, the Eclipse Sovereign and its astral telegraphs. */
public final class NyxaraRenderState extends EntityRenderState {
	public float bodyYaw;
	public int move;
	public float elapsedTicks;
	public float aimYaw;
	public boolean phaseTwo;
	public boolean staggered;
	public float hurtFlash;
	public int prismMask;
	public float centerOffsetX, centerOffsetZ;
	public int soakCount;
	public int soakFilledMask;
}
