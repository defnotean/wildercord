package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What Mojito's model needs: his move, how long into it, its tell, which way a slash or a hop goes, and how far up out of his sleep
 * he is; the move before and how far he has blended out of it; and how fresh the last blow he took is (1 just struck, 0 gone).
 */
public final class MojitoRenderState extends LivingEntityRenderState {
	public int move,tell,side,prevMove,prevTell;
	public float elapsed,rise,prevElapsed,blend=1,hurt;
}
