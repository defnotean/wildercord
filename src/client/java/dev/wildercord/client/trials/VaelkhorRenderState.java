package dev.wildercord.client.trials;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Client render snapshot for Vaelkhor, the Leyline Sovereign and its arena telegraphs. */
public final class VaelkhorRenderState extends EntityRenderState {
	public float bodyYaw;
	public int move;
	public float elapsedTicks;
	public float aimYaw;
	public boolean phaseTwo;
	public boolean staggered;
	public float hurtFlash;
	public int conduitMask;
	public float centerOffsetX, centerOffsetZ;
	public int soakCount;
	public int soakFilledMask;
	public final float[] soakX = new float[3];
	public final float[] soakZ = new float[3];
}
