package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.cast.Effects;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** A real enrolled connected challenger takes Stone's naturally selected opening THRUST. No attack/timer fields are written. */
public final class StoneHingeMasterReleaseChecks {
	private StoneHingeMasterReleaseChecks() {}
	public static void run(ClientGameTestContext context, TestSingleplayerContext world, ServerPlayer player, Vec3 origin) {
		SwordMaster[] master = new SwordMaster[1];
		StoneHingeImpulseProbe.Trial[] warning = new StoneHingeImpulseProbe.Trial[1];
		StoneHingeImpulseProbe.Trial[] trial = new StoneHingeImpulseProbe.Trial[1];
		try {
			world.getServer().runOnServer(server -> {
				player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
				player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
				player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); player.setHealth(200);
				warning[0] = StoneHingeImpulseProbe.start(player);
				warningReceipt("setup-reset", player, master[0], warning[0]);
				player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
				player.teleportTo(player.level(), origin.x, origin.y, origin.z + 3, Set.of(), 180, 0, false);
				player.setDeltaMovement(Vec3.ZERO); Effects.readyToHurt(player);
				master[0] = AuraWorld.SWORD_MASTER.create(player.level(), EntitySpawnReason.COMMAND);
				check(master[0] != null, "Actual current Master is constructible");
				master[0].setDiscipline(MastersRules.STONE); master[0].snapTo(origin.x, origin.y, origin.z, 0, 0);
				player.level().addFreshEntity(master[0]);
				master[0].mobInteract(player, InteractionHand.MAIN_HAND); master[0].mobInteract(player, InteractionHand.MAIN_HAND);
				check(SwordMaster.ready(player) == 1, "The actual connected Survival challenger enrolls and closes the real trial");
			});
			world.getServer().waitFor(server -> {
				boolean ready = master[0].started() && master[0].state(AuraFighter.WINDUP);
				if (ready) warningReceipt("windup-observed", player, master[0], warning[0]);
				return ready;
			}, 80);
			world.getServer().runOnServer(server -> {
				check(master[0].canHarmParticipant(player) && master[0].challengerCount() == 1,
					"The source is an active opted-in encounter with this real connected body");
				MastersRules.Move selected = MasterMoveCatalog.legacy().byWireId(master[0].attackAnimation()).orElseThrow().legacyMove();
				check(selected == MastersRules.Move.THRUST, "Unmodified Stone opening chooses its existing native THRUST: " + selected);
				warningReceipt("before-health-assertion", player, master[0], warning[0]);
				StoneHingeImpulseProbe.stop(warning[0]); warning[0] = null;
				check(player.getHealth() == 200, "The real full warning has not dealt an early wound");
				trial[0] = StoneHingeImpulseProbe.start(player);
			});
			world.getServer().waitFor(server -> trial[0].completeHit(), MastersRules.Move.THRUST.tell + 10);
			world.getServer().runOnServer(server -> {
				StoneHingeImpulseProbe.stop(trial[0]);
				var result = trial[0]; trial[0] = null;
				check(result.hits().size() == 1 && result.onlyHit().melee() && result.onlyHit().healthLost() > 0 && !result.onlyHit().lethal(),
					"Exact native Master THRUST release has its own nonlethal melee wound: " + result.summary());
				check(result.impulses().size() == 1 && result.impulses().getFirst().hit() == result.onlyHit()
					&& result.impulses().getFirst().horizontalMagnitude() > 0,
					"Current Master melee actually produces positive native default knockback for that exact wound");
				check(result.onlyHit().source().getEntity() == master[0] && result.onlyHit().source().getDirectEntity() == master[0],
					"Direct and owning source identities remain the actual encounter Master");
				// By this later test assertion a client movement packet may already have applied the native impulse.
				// Therefore do not falsely require that its post-write velocity/position still equals the synchronous snapshot.
				Wildercord.LOGGER.info("STONE_HINGE_NATIVE case=current-native-master-thrust {}", result.summary());
				master[0].setNoAi(true); player.setHealth(200); player.setAbsorptionAmount(0); Effects.readyToHurt(player);
				var projected = StoneHingeImpulseProbe.capture(player, () -> master[0].projected(player, 8));
				check(projected.onlyHit().healthLost() > 0 && !projected.onlyHit().melee() && !projected.eligibleReceipt(),
					"The identical nearby Master/Aura source outside the exact melee release stays unclassified: " + projected.summary());
				Wildercord.LOGGER.info("STONE_HINGE_NATIVE case=nearby-master-unclassified-aura {}", projected.summary());
			});
		} finally {
			world.getServer().runOnServer(server -> {
				try {
					if (warning[0] != null) {
						try { warningReceipt("incomplete-warning-cleanup", player, master[0], warning[0]); }
						finally { StoneHingeImpulseProbe.stop(warning[0]); }
					}
					if (trial[0] != null) StoneHingeImpulseProbe.stop(trial[0]);
				} finally {
					warning[0] = null;
					trial[0] = null;
					if (master[0] != null) master[0].discard();
				}
			});
		}
	}
	/** Read-only snapshots; the last damage source can predate this interval, whose exact hits are logged separately. */
	private static void warningReceipt(String phase, ServerPlayer player, SwordMaster master, StoneHingeImpulseProbe.Trial warning) {
		Wildercord.LOGGER.info("STONE_HINGE_WARNING phase={} tick={} player={} health={} maxHealth={} absorption={} position={} velocity={} fallDistance={} grounded={} fireTicks={} frozenTicks={} air={} effects={} food={} saturation={} aura={} auraState={} mana={} circles={} cracks={} chargePresent={} master={} lastDamageAtSnapshot={} interval={}",
			phase, player.level().getGameTime(), StoneHingeImpulseProbe.entityIdentity(player), player.getHealth(), player.getMaxHealth(), player.getAbsorptionAmount(),
			player.position(), player.getDeltaMovement(), player.fallDistance, player.onGround(), player.getRemainingFireTicks(), player.getTicksFrozen(), player.getAirSupply(), player.getActiveEffects(),
			player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel(), player.getAttached(AuraAttachments.AURA), player.getAttached(AuraAttachments.STATE),
			Spellbooks.mana(player), player.getAttached(WildercordAttachments.CIRCLES), player.getAttached(WildercordAttachments.CRACKS), player.hasAttached(WildercordAttachments.CHARGE),
			StoneHingeImpulseProbe.entityIdentity(master), StoneHingeImpulseProbe.sourceIdentity(player.getLastDamageSource()), warning.warningSummary());
	}
	private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
