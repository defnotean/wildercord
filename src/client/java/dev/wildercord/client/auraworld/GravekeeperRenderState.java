package dev.wildercord.client.auraworld;
/** What the Gravekeeper's model needs: its move, how far into it, its tell, the move before (to blend out of, or to know which blow a recovery follows), and how fresh the last hit it took is. */
public final class GravekeeperRenderState extends net.minecraft.client.renderer.entity.state.LivingEntityRenderState {
	public int move,tell=dev.wildercord.aura.world.SwordTombRules.WINDUP,prevMove,prevTell;
	public float elapsed,prevElapsed,blend=1,hurt;
}
