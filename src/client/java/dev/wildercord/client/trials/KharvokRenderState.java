package dev.wildercord.client.trials;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Client render snapshot for Kharvok, the Crucible Sovereign and its foundry telegraphs. */
public final class KharvokRenderState extends EntityRenderState {
	public float bodyYaw;
	public int move;
	public float elapsedTicks;
	public float aimYaw;
	public boolean phaseTwo;
	public boolean staggered;
	public float hurtFlash;
	public int ventMask;
	public float centerOffsetX, centerOffsetZ;
	public int soakCount;
	public int soakFilledMask;
}
