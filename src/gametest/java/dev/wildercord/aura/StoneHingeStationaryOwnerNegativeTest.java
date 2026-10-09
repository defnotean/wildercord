package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Effects;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe.Body;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe.Trace;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Expected negative result only: a stationary owner's motion packet cannot carry a server-only lateral displacement.
 * Two real connected worlds compare ordinary native knockback against exactly one proposed step/subtraction/delivery.
 * There is no ability, six-step controller, fake movement/ACK, fabricated input, teleport reconciliation or fall repair.
 */
public final class StoneHingeStationaryOwnerNegativeTest implements FabricClientGameTest {
	private static final double EPS = 1.0E-5, STEP = .3;
	private record Result(Trace trace, Body beforeHit, Body afterHit, Body afterStep, Body delivered) {}

	@Override public void runTest(ClientGameTestContext context) {
		StoneHingeOwnerProbe.register(); neutral(context);
		Result ordinary = scenario(context, false);
		Result candidate = scenario(context, true);
		ordinary.trace.assertHealthy(); candidate.trace.assertHealthy();
		check(close(ordinary.beforeHit.motion().y, candidate.beforeHit.motion().y)
			&& close(ordinary.afterHit.motion().y, candidate.afterHit.motion().y)
			&& ordinary.beforeHit.grounded() && candidate.beforeHit.grounded(), "Paired hits have the same native grounded vertical input and impulse");
		var ordinaryMotion = ordinary.trace.expectedOwnerMotion();
		var candidateMotion = candidate.trace.expectedOwnerMotion();
		check(ordinaryMotion != null && candidateMotion != null && close(ordinaryMotion.after().motion().y, candidateMotion.after().motion().y),
			"Both owners process the same native vertical impulse from their exactly matched expected raw/wire motion delivery");
		Wildercord.LOGGER.info("STONE_HINGE_OWNER_VERTICAL ordinaryPre={} candidatePre={} ordinaryHit={} candidateHit={} ordinaryFirstPacket={} candidateFirstPacket={} ordinaryPeakRise={} candidatePeakRise={} ordinaryMotionPackets={} candidateMotionPackets={}",
			ordinary.beforeHit, candidate.beforeHit, ordinary.afterHit, candidate.afterHit, ordinaryMotion, candidateMotion,
			ordinary.trace.peakOwnerRise(), candidate.trace.peakOwnerRise(), ordinary.trace.count("client-motion-processed"), candidate.trace.count("client-motion-processed"));
		Wildercord.LOGGER.info("STONE_HINGE_OWNER_NEGATIVE expected_incompatibility=observed movement_gate=NOT_PROVEN gameplay_enabled=false conditional_shared_rest_ticks=120");
	}

	private Result scenario(ClientGameTestContext context, boolean proposed) {
		String name = proposed ? "server-step-plus-motion-negative" : "ordinary-native-hit-control";
		ServerPlayer[] body = new ServerPlayer[1]; Mob[] mob = new Mob[1]; Trace[] running = new Trace[1]; Result[] result = new Result[1];
		try (var world = context.worldBuilder().create()) {
			try {
				context.waitTicks(40); neutral(context);
				for (String command : List.of("difficulty normal", "time set midnight", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) world.getServer().runCommand(command);
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst(); body[0] = player;
					Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.StoneHingeStationaryOwnerNegativeTest#{}\",\"seed\":\"{}\"}", name, player.level().getSeed());
					for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
						player.level().setBlockAndUpdate(new BlockPos(x, 180, z), Blocks.STONE.defaultBlockState());
						for (int y = 181; y <= 188; y++) player.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
					}
					player.setGameMode(GameType.SURVIVAL); player.setPermanentlyInvulnerable(false); player.removeAllEffects();
					player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); player.setHealth(200);
					player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0); player.setAbsorptionAmount(0);
					for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) player.setItemSlot(slot, ItemStack.EMPTY);
					player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
					player.setAttached(WildercordAttachments.CIRCLES, 0); player.setNoGravity(false);
					// Setup only. The experiment starts after the real owner receives and acknowledges this ordinary arrival.
					player.teleportTo(player.level(), .5, 181, .5, Set.of(), 0, 0, false);
					mob[0] = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.COMMAND);
					check(mob[0] != null, "Native hostile attacker exists");
					mob[0].setNoAi(true); mob[0].setNoGravity(true); mob[0].setPersistenceRequired(); mob[0].setPermanentlyInvulnerable(true);
					mob[0].getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
					if (mob[0].getAttribute(Attributes.ATTACK_KNOCKBACK) != null) mob[0].getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);
					mob[0].snapTo(.5, 181, 2, 180, 0); player.level().addFreshEntity(mob[0]);
				});
				context.waitTicks(20);
				context.waitFor(client -> client.player != null && client.player.onGround() && client.player.input.keyPresses.equals(net.minecraft.world.entity.player.Input.EMPTY)
					&& client.player.getDeltaMovement().horizontalDistanceSqr() == 0, 60);
				world.getServer().waitFor(server -> body[0].onGround() && body[0].getDeltaMovement().horizontalDistanceSqr() == 0
					&& body[0].getLastClientInput().equals(net.minecraft.world.entity.player.Input.EMPTY)
					&& !((StoneHingeOwnerProbe.ConnectionState) body[0].connection).stoneHinge$awaitingTeleport(), 60);
				var owner = context.computeOnClient(client -> client.player);
				Body ownerBefore = context.computeOnClient(client -> Body.of(client.player));
				world.getServer().runOnServer(server -> {
					ServerPlayer player = body[0];
					check(player == server.getPlayerList().getPlayer(player.getUUID()) && player.connection.player == player && player.connection.hasClientLoaded()
						&& player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "Damage recipient remains the genuine connected Survival body");
					check(player.position().distanceToSqr(ownerBefore.position()) < EPS * EPS && ownerBefore.neutral() && ownerBefore.motion().horizontalDistanceSqr() == 0
						&& player.getLastClientInput().equals(net.minecraft.world.entity.player.Input.EMPTY) && player.getDeltaMovement().horizontalDistanceSqr() == 0,
						"Both actual bodies have neutral input, identical position and verified zero pre-hit horizontal velocity");
					Vec3 look = player.getViewVector(1); check(look.horizontalDistanceSqr() > .99, "Native settled view is horizontal");
					Vec3 forward = new Vec3(look.x, 0, look.z).normalize(), side = new Vec3(-forward.z, 0, forward.x);
					mob[0].setPos(player.position().add(forward.scale(1.5))); check(AuraGuard.facing(player, mob[0].position()), "Actual hostile melee source is frontal");
					Effects.readyToHurt(player); StoneHingeImpulseProbe.assertIdle();
					Trace trace = StoneHingeOwnerProbe.start(name, player, owner, ownerBefore, side); running[0] = trace;
					Body before = Body.of(player); trace.record("server-before-hit", "real Mob.doHurtTarget", null, before);
					var receipt = StoneHingeImpulseProbe.capture(player, () -> mob[0].doHurtTarget(player.level(), player));
					check(receipt.eligibleReceipt(), "Full native attack produces a qualifying nonlethal frontal wound and one default impulse: " + receipt.summary());
					Body afterHit = Body.of(player); trace.expectMotion(afterHit.motion()); trace.record("server-after-hit", receipt.summary(), before, afterHit);
					Body afterStep = afterHit;
					if (proposed) {
						Vec3 delta = side.scale(STEP), beforePosition = player.position();
						check(WallTurn.swept(player, beforePosition, beforePosition.add(delta), false), "First proposed step has a real resident clear whole-body lane");
						player.move(MoverType.PLAYER, delta); // The full original native post-hit velocity is still installed here.
						afterStep = Body.of(player); trace.record("server-step-committed", "one native move; no teleport", afterHit, afterStep);
						check(afterStep.position().subtract(beforePosition).distanceToSqr(delta) < EPS * EPS && afterStep.motion().equals(afterHit.motion())
							&& afterStep.position().y == afterHit.position().y && afterStep.fall() == afterHit.fall() && afterStep.grounded() == afterHit.grounded(),
							"One actual lateral step commits before subtraction while preserving complete native motion, Y, fall and ground state");
						var impulse = receipt.impulses().getFirst();
						Vec3 horizontalImpulse = new Vec3(impulse.after().x - impulse.before().x * .5, 0, impulse.after().z - impulse.before().z * .5);
						player.setDeltaMovement(player.getDeltaMovement().subtract(horizontalImpulse));
						check(player.getDeltaMovement().horizontalDistanceSqr() == 0 && player.getDeltaMovement().y == afterHit.motion().y,
							"Subtracting only this impulse leaves stationary pre-hit X/Z exactly zero and keeps native Y");
						var packet = new ClientboundSetEntityMotionPacket(player); trace.manual(packet);
						player.connection.send(packet); // Do not clear or alter native syncVelocity/needsSync; duplicates are evidence.
					}
					Body delivered = Body.of(player); trace.record("server-after-delivery", proposed ? "proposed subtraction plus ordinary motion packet" : "unchanged native automatic delivery", afterStep, delivered);
					result[0] = new Result(trace, before, afterHit, afterStep, delivered);
				});
				context.waitFor(client -> running[0].expectedOwnerMotion() != null, 40);
				world.getServer().waitFor(server -> running[0].hasOwnerPositionAfterMotion(), 40);
				context.waitTicks(16);
				world.getServer().runOnServer(server -> {
					Trace trace = running[0]; trace.assertHealthy();
					check(trace.events().stream().filter(event -> event.kind().equals("client-tick")).allMatch(event -> event.after().neutral()), "Native owner input stayed neutral throughout the observation");
					check(trace.expectedOwnerMotion() != null && trace.hasOwnerPositionAfterMotion(),
						"The owner processed this experiment's expected motion; a genuine subsequent LocalPlayer.sendPosition packet matches ordered native server processing");
					if (proposed) {
						boolean offsetLost = trace.provenOwnerPositions().stream().anyMatch(event -> Math.abs(trace.lateral(event.after().position())) < STEP * .25);
						check(trace.uncorrectedOwnerObservationCount() > 0, "Actual post-motion owner observations precede any correction");
						check(trace.maximumUncorrectedOwnerLateral() < STEP * .25, "The stationary owner never performed the server-only .3 lateral step before any correction");
						check(offsetLost || trace.correction(), "The missing owner step loses the server offset or requires a forbidden correction; it is never reconciliation");
						Wildercord.LOGGER.info("STONE_HINGE_OWNER_INCOMPATIBLE ownerControlledLateral={} serverOffsetLost={} correction={} sentMotion={} processedMotion={} serverPositionPackets={}",
							trace.maximumUncorrectedOwnerLateral(), offsetLost, trace.correction(), trace.count("server-motion-sent"), trace.count("client-motion-processed"), trace.count("server-owner-position-processed"));
					} else {
						check(!trace.correction(), "Ordinary native hit control needs no correction");
						check(trace.provenOwnerPositions().stream().anyMatch(event -> event.after().position().subtract(trace.serverBefore.position()).horizontalDistanceSqr() > .01), "Ordinary knockback really moves the connected owner");
					}
					trace.record("server-finish", "bounded observation ends", null, Body.of(body[0])); trace.assertHealthy();
					StoneHingeOwnerProbe.stop(trace); running[0] = null; StoneHingeImpulseProbe.assertIdle();
				});
			} finally {
				world.getServer().runOnServer(server -> {
					try { if (running[0] != null) StoneHingeOwnerProbe.stop(running[0]); }
					finally {
						running[0] = null; StoneHingeOwnerProbe.clear();
						try { StoneHingeImpulseProbe.assertIdle(); }
						finally { StoneHingeImpulseProbe.clear(); if (mob[0] != null) mob[0].discard(); }
					}
				});
			}
		}
		return result[0];
	}
	private static void neutral(ClientGameTestContext context) {
		context.getInput().releaseKey(options -> options.keyUp); context.getInput().releaseKey(options -> options.keyDown);
		context.getInput().releaseKey(options -> options.keyLeft); context.getInput().releaseKey(options -> options.keyRight);
		context.getInput().releaseKey(options -> options.keyJump); context.getInput().releaseKey(options -> options.keyShift); context.getInput().releaseKey(options -> options.keySprint);
	}
	private static boolean close(double left, double right) { return Math.abs(left - right) < EPS; }
	private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
}
