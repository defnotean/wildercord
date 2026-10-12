package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What the Frost Wyrmling's model needs: its move, how long into it, its tell, which way a bound or a tail sweep goes, and how far
 * up out of its curl it is; the move before and how far it has blended out of it; and how fresh the last blow it took is (1 just
 * struck, 0 gone).
 */
public final class FrostWyrmlingRenderState extends LivingEntityRenderState {
	public int move,tell,side,prevMove,prevTell;
	public float elapsed,rise,prevElapsed,blend=1,hurt;
}
