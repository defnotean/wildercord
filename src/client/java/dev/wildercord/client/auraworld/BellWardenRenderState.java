package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What the Bell-Warden's model needs: its move, how long into it, its tell, and how far up off its knees it is; the move before and
 * how far it has blended out of it; and how fresh the last blow it took is (1 just struck, 0 gone).
 */
public final class BellWardenRenderState extends LivingEntityRenderState {
	public int move,tell,prevMove,prevTell;
	public float elapsed,rise,prevElapsed,blend=1,hurt;
}
