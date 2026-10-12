package dev.wildercord.client.auraworld;

/** One frame of the Rootcoil Serpent: its joints as offsets from where it is drawn, the light at each, and its head. */
public final class SerpentRenderState extends net.minecraft.client.renderer.entity.state.EntityRenderState {
	/** Joint offsets, x y z per joint, head first. */
	public float[] joints=new float[0];
	public int[] lights=new int[0];
	/** Which way the head points, and how far its jaw hangs open (0 shut, 1 wide). */
	public float headX,headY,headZ=1,jaw;
	/** How far its hood is spread, 0 folded flat to 1 wide. */
	public float hood;
	public boolean hurt;
	public int move;
}
