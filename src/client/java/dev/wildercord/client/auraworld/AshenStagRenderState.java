package dev.wildercord.client.auraworld;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * What the Ashen Stag's model needs: its move, how long into it, its tell, which way a bound takes it, and how far up out of its bed
 * it is; the move before and how far it has blended out of it; how fresh the last blow it took is (1 just struck, 0 gone); and
 * whether it has kindled.
 */
public final class AshenStagRenderState extends LivingEntityRenderState {
	public int move,tell,side,prevMove,prevTell;
	public float elapsed,rise,prevElapsed,blend=1,hurt;
	public boolean kindled;
}
