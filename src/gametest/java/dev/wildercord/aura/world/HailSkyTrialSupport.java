package dev.wildercord.aura.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Exercises real encounter consent, startup and roster pruning for the delayed Hailfall/Skyfall fixture. */
public final class HailSkyTrialSupport {
	private HailSkyTrialSupport() {}
	public static SwordMaster start(ServerPlayer owner, ServerPlayer other, double x, double z) {
		var level = owner.level();
		var master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		if (master == null) throw new AssertionError("The native trial master must exist");
		master.setNoAi(true); master.setNoGravity(true);
		master.snapTo(x, 100, z, 180, 0); master.setDeltaMovement(Vec3.ZERO);
		level.addFreshEntity(master);
		master.mobInteract(owner, InteractionHand.MAIN_HAND);
		master.mobInteract(owner, InteractionHand.MAIN_HAND);
		master.mobInteract(other, InteractionHand.MAIN_HAND);
		master.mobInteract(other, InteractionHand.MAIN_HAND);
		if (SwordMaster.ready(owner) != 1) throw new AssertionError("The real consenting roster must ready its trial");
		master.customServerAiStep(level);
		master.setNoAi(true);
		if (!master.started() || !master.acceptsHarmFrom(owner) || !master.acceptsHarmFrom(other)) {
			throw new AssertionError("Both actual participants must be admitted by the live started trial");
		}
		return master;
	}
	public static void removeEnrollment(ServerPlayer owner, SwordMaster master) {
		owner.setGameMode(GameType.SPECTATOR);
		try { master.customServerAiStep(owner.level()); }
		finally { owner.setGameMode(GameType.SURVIVAL); }
		if (!master.started() || master.challengers().contains(owner.getUUID()) || master.acceptsHarmFrom(owner)) {
			throw new AssertionError("Actual trial roster pruning must remove the temporarily ineligible participant permanently");
		}
	}
}
