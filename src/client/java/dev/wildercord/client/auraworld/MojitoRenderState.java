package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What Mojito's model needs: his move, how long into it, its tell, which way a sweep, a spin or a hop goes (or how far along the
 * fetch is), and how far up out of his sleep he is; the move before and how far he has blended out of it; how fresh the last blow
 * he took is (1 just struck, 0 gone); and, thrown, where his sword is in the model's own space.
 */
public final class MojitoRenderState extends LivingEntityRenderState {
	public int move,tell,side,prevMove,prevTell;
	public float elapsed,rise,prevElapsed,blend=1,hurt;
	public boolean thrown;
	public float swordX,swordY,swordZ;
}
