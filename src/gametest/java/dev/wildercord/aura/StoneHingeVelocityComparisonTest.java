package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Effects;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe.Body;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe.Trace;
import dev.wildercord.gametest.stonehinge.StoneHingeVelocityExperiment;
import dev.wildercord.gametest.stonehinge.StoneHingeVelocityExperiment.Decision;
import dev.wildercord.gametest.stonehinge.StoneHingeVelocityExperiment.State;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Separate diagnostic for the revised velocity-only contract. Fixture opt-in is explicit; this is not C, a paid
 * form, a teacher, an immunity window, a distance promise, or evidence of peer/latency/ward admission correctness.
 */
public final class StoneHingeVelocityComparisonTest implements FabricClientGameTest {
	private static final double EPS = 1.0E-5;
	private enum Case {
		ORDINARY, RIGHT, LEFT, PARTIAL_RESISTANCE, ABSORPTION, WALL_CLIPPED,
		REAR, FULL_RESISTANCE, ZERO_WOUND, INVULNERABLE, CALLER_KNOCKBACK, CLOSE_AURA, INTERVENING_IMPULSE,
		FALLING_ORDINARY, FALLING_REFUSAL;
		boolean falling() { return this == FALLING_ORDINARY || this == FALLING_REFUSAL; }
		boolean control() { return this == ORDINARY || this == FALLING_ORDINARY; }
		boolean applies() { return this == RIGHT || this == LEFT || this == PARTIAL_RESISTANCE || this == ABSORPTION || this == WALL_CLIPPED; }
		int sign() { return this == LEFT ? -1 : 1; }
	}
	private record Result(Trace trace, State before, State nativeOutcome, State beforeTurn, State outcome, Decision decision) {}
	private ServerPlayer player;
	private Mob attacker;
	private Trace running;

	@Override public void runTest(ClientGameTestContext context) {
		StoneHingeOwnerProbe.register(); neutral(context);
		Map<Case, Result> results = new LinkedHashMap<>();
		try (var world = context.worldBuilder().create()) {
			try {
				context.waitTicks(40);
				for (String command : List.of("difficulty normal", "time set midnight", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) world.getServer().runCommand(command);
				world.getServer().runOnServer(server -> {
					player = server.getPlayerList().getPlayers().getFirst();
					Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.StoneHingeVelocityComparisonTest\",\"seed\":\"{}\"}", player.level().getSeed());
				});
				for (Case which : Case.values()) results.put(which, scenario(context, world, which));
				Result ordinary = results.get(Case.ORDINARY);
				check(close(results.get(Case.PARTIAL_RESISTANCE).nativeOutcome.motion().horizontalDistance(), ordinary.nativeOutcome.motion().horizontalDistance() * .5),
					"Partial resistance halves the actual native impulse before the experiment rotates it");
				for (Case which : List.of(Case.RIGHT, Case.LEFT, Case.ABSORPTION, Case.WALL_CLIPPED)) {
					Result result = results.get(which);
					check(close(result.before.motion().y, ordinary.before.motion().y) && close(result.nativeOutcome.motion().y, ordinary.nativeOutcome.motion().y),
						which + " has the same grounded native vertical input and output as the ordinary hit");
					check(close(result.trace.expectedOwnerMotion().after().motion().y, ordinary.trace.expectedOwnerMotion().after().motion().y),
						which + " owner processes the unchanged native vertical payload");
					check(result.trace.count("server-motion-sent") == ordinary.trace.count("server-motion-sent"),
						which + " retains the ordinary tracker's motion dispatch count, with no extra manual delivery");
				}
				check(results.get(Case.WALL_CLIPPED).trace.maximumUncorrectedOwnerLateral() < results.get(Case.RIGHT).trace.maximumUncorrectedOwnerLateral(),
					"The solid wall genuinely reduces owner travel after an applied rotation; there is no promised distance or first-step transaction");
				Wildercord.LOGGER.info("STONE_HINGE_VELOCITY_COMPARISON owner_cases={} owner_gate=observed peer_gate=NOT_PROVEN latency_gate=NOT_PROVEN admission_gate=NOT_PROVEN movement_gate=NOT_PROVEN gameplay_enabled=false conditional_aura=20 conditional_shared_rest_ticks=120 conditional_brace_ticks=6 conditional_catch_ticks=12", results.size());
			} finally {
				world.getServer().runOnServer(server -> {
					try { if (running != null) StoneHingeOwnerProbe.stop(running); }
					finally { running = null; StoneHingeOwnerProbe.clear();
						try { StoneHingeImpulseProbe.assertIdle(); }
						finally { StoneHingeImpulseProbe.clear(); if (attacker != null) attacker.discard(); attacker = null; }
					}
				});
			}
		} finally { player = null; }
	}

	private Result scenario(ClientGameTestContext context, TestSingleplayerContext world, Case which) {
		neutral(context);
		world.getServer().runOnServer(server -> prepare(which));
		context.waitTicks(20);
		context.waitFor(client -> client.player != null && client.player.onGround() && client.player.input.keyPresses.equals(Input.EMPTY)
			&& client.player.getDeltaMovement().horizontalDistanceSqr() == 0, 60);
		world.getServer().waitFor(server -> player.onGround() && player.getDeltaMovement().horizontalDistanceSqr() == 0
			&& player.getLastClientInput().equals(Input.EMPTY) && !((StoneHingeOwnerProbe.ConnectionState) player.connection).stoneHinge$awaitingTeleport(), 60);
		if (which.falling()) {
			world.getServer().runOnServer(server -> {
				for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) player.level().setBlockAndUpdate(new BlockPos(x, 192, z), Blocks.AIR.defaultBlockState());
			});
			context.waitFor(client -> !client.player.onGround() && client.player.fallDistance > 0 && client.player.getDeltaMovement().y < 0, 20);
			world.getServer().waitFor(server -> !player.onGround() && player.fallDistance > 0 && player.getDeltaMovement().y < 0, 20);
		}
		var owner = context.computeOnClient(client -> client.player);
		Body ownerBefore = context.computeOnClient(client -> Body.of(client.player));
		Result[] result = new Result[1];
		world.getServer().runOnServer(server -> {
			check(player == server.getPlayerList().getPlayer(player.getUUID()) && player.connection.player == player && player.connection.hasClientLoaded()
				&& player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "The recipient is the genuine connected Survival body");
			check(ownerBefore.neutral() && player.getLastClientInput().equals(Input.EMPTY)
				&& ownerBefore.motion().horizontalDistanceSqr() == 0 && player.getDeltaMovement().horizontalDistanceSqr() == 0,
				"Both actual bodies have neutral input and verified zero horizontal velocity before the experiment");
			check(which.falling() || ownerBefore.position().distanceToSqr(player.position()) < EPS * EPS, "Grounded owner/server positions agree before the hit");
			Vec3 look = player.getViewVector(1); check(look.horizontalDistanceSqr() > .99, "Observed native view is horizontal");
			Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
			check(forward.distanceToSqr(new Vec3(0, 0, 1)) < EPS * EPS, "The observed view, including head yaw, points down the prepared lane");
			Vec3 side = new Vec3(-forward.z, 0, forward.x).scale(which.sign());
			attacker.setPos(player.position().add(forward.scale(which == Case.REAR ? -1.5 : 1.5)));
			check(AuraGuard.facing(player, attacker.position()) == (which != Case.REAR), "Real source geometry matches the tested frontal/rear condition");
			verifyPreparedResources(which, "before-hit-after-settle");
			Effects.readyToHurt(player); StoneHingeImpulseProbe.assertIdle();
			Trace trace = StoneHingeOwnerProbe.start("velocity-" + which.name().toLowerCase(java.util.Locale.ROOT), player, owner, ownerBefore, side); running = trace;
			trace.record("server-before-hit", "real full attack; explicit test-only velocity opt-in", null, Body.of(player));
			var strike = StoneHingeVelocityExperiment.capture(player, () -> {
				if (which == Case.CLOSE_AURA) player.hurtServer(player.level(), player.level().damageSources().source(Aura.DAMAGE, attacker, attacker), 8);
				else attacker.doHurtTarget(player.level(), player);
			});
			trace.record("server-after-hit", strike.receipt().summary() + ", resourcesBefore=" + strike.before()
				+ ", resourcesAfterNativeHit=" + strike.nativeOutcome() + ", maxAbsorption=" + player.getMaxAbsorption(), null, Body.of(player));
			if (which == Case.INTERVENING_IMPULSE) player.knockback(.2, 1, 0, player.damageSources().mobAttack(attacker), 0);
			State beforeTurn = State.of(player);
			Decision decision = which.control() ? null : strike.turn(which.sign());
			State outcome = State.of(player);
			check(which.applies() ? decision == Decision.APPLIED : which.control() || decision == (which.falling() ? Decision.AIRBORNE
				: which == Case.INTERVENING_IMPULSE ? Decision.STATE_CHANGED : Decision.RECEIPT_REFUSED), "Exact expected experiment decision for " + which + ": " + decision);
			if (which.applies()) verifyRotation(strike, beforeTurn, outcome, which.sign());
			else check(outcome.equals(beforeTurn), "Admission refusal/control preserves every native state field and the complete original impulse");
			if (!which.control()) {
				check(strike.turn(which.sign()) == Decision.DUPLICATE && State.of(player).equals(outcome), "A duplicate attempt cannot rotate, restore, or replay any impulse");
			}
			if (which == Case.ABSORPTION) check(strike.receipt().onlyHit().healthLost() == 0 && strike.receipt().onlyHit().absorptionLost() > 0
				&& outcome.health() == strike.before().health() && outcome.absorption() < strike.before().absorption(),
				"Native absorption payment remains lost after rotation: before=" + strike.before() + ", native=" + strike.nativeOutcome() + ", after=" + outcome);
			if (which == Case.ORDINARY || which.falling()) check(strike.receipt().eligibleReceipt(), "The actual control/falling melee otherwise has a qualifying native wound and impulse");
			if (which == Case.REAR) check(strike.receipt().onlyHit().healthLost() > 0 && !strike.receipt().onlyHit().frontal()
				&& strike.receipt().onlyHit().facingDot() < 0 && strike.receipt().impulses().size() == 1, "The rear refusal retains a real rear wound and native impulse");
			if (which == Case.FULL_RESISTANCE) check(strike.receipt().onlyHit().healthLost() > 0 && strike.receipt().impulses().isEmpty()
				&& outcome.syncVelocity(), "Full resistance refuses the impulse while native accepted damage still requests its original motion synchronization");
			if (which == Case.ZERO_WOUND) check(strike.receipt().onlyHit().healthLost() == 0 && strike.receipt().onlyHit().absorptionLost() == 0
				&& strike.receipt().impulses().size() == 1, "Resistance V refuses the catch despite an actual native impulse because no wound was accepted");
			if (which == Case.INVULNERABLE) check(strike.receipt().impulses().isEmpty() && outcome.health() == strike.before().health(), "Native invulnerability accepts neither wound nor impulse");
			if (which == Case.CALLER_KNOCKBACK) check(strike.receipt().onlyHit().healthLost() > 0 && strike.receipt().impulses().size() == 2
				&& strike.receipt().impulses().getLast().hit() == null, "The full native attack includes the separate unassociated enchanted caller impulse");
			if (which == Case.CLOSE_AURA) check(strike.receipt().onlyHit().healthLost() > 0 && !strike.receipt().onlyHit().melee()
				&& strike.receipt().impulses().size() == 1, "The close Aura wound and impulse do not establish real melee provenance");
			if (which.falling()) check(strike.before().fall() > 0 && strike.nativeOutcome().motion().y == strike.before().motion().y
				&& outcome.fall() == strike.before().fall(), "Real descending physics and accumulated fall are preserved exactly by native hit and airborne refusal");
			trace.expectMotion(outcome.motion());
			trace.record("server-after-turn", "decision=" + decision + " originalTrackerOnly=true before=" + beforeTurn + " after=" + outcome, null, Body.of(player));
			result[0] = new Result(trace, strike.before(), strike.nativeOutcome(), beforeTurn, outcome, decision);
		});
		boolean hasImpulse = result[0].outcome.motion().horizontalDistanceSqr() > 0;
		boolean expectsDelivery = result[0].outcome.syncVelocity() || hasImpulse;
		if (expectsDelivery) {
			context.waitFor(client -> running.expectedOwnerMotion() != null, 40);
		}
		if (hasImpulse) {
			world.getServer().waitFor(server -> running.hasOwnerPositionAfterMotion(), 40);
		}
		context.waitTicks(20);
		if (which.falling()) world.getServer().waitFor(server -> player.onGround(), 40);
		world.getServer().runOnServer(server -> {
			Trace trace = running; trace.assertHealthy();
			check(!trace.correction(), "Velocity-only experiment/control never uses a correction, teleport or ACK reconciliation");
			check(trace.events().stream().filter(event -> event.kind().equals("client-tick")).allMatch(event -> event.after().neutral()), "Genuine owner input stayed neutral");
			check(trace.events().stream().filter(event -> event.kind().equals("server-motion-sent")).allMatch(event -> event.data().contains("manual=false")), "Only native original tracker dispatch is observed");
			if (hasImpulse) {
				check(trace.expectedOwnerMotion() != null && trace.hasOwnerPositionAfterMotion(), "This native motion's actual owner application precedes a genuine positional send and native server processing");
				check(trace.uncorrectedOwnerObservationCount() > 0, "At least one actual owner movement sample exists");
				if (which.applies() && which != Case.WALL_CLIPPED) check(trace.provenOwnerPositions().stream()
					.anyMatch(event -> trace.lateral(event.after().position()) > .05), "The connected owner actually moves in the selected lateral direction");
				if (which == Case.WALL_CLIPPED) {
					check(trace.events().stream().anyMatch(event -> event.kind().equals("client-tick") && event.after().horizontalCollision()), "The real owner reports native horizontal collision against the prepared solid wall");
					check(trace.maximumUncorrectedOwnerLateral() <= .201, "Native wall collision clips the applied rotation to the actual initial clearance");
				}
			} else if (expectsDelivery) {
				check(trace.expectedOwnerMotion() != null && trace.expectedOwnerMotion().after().motion().horizontalDistanceSqr() == 0,
					"Accepted damage's original zero-horizontal synchronization reaches the owner without granting a deflection or free movement");
				check(trace.uncorrectedOwnerObservationCount() > 0 && trace.maximumUncorrectedOwnerLateral() == 0,
					"Actual owner samples after the zero-horizontal native packet stay laterally stationary");
			} else check(trace.count("server-motion-sent") == 0 && trace.count("client-motion-processed") == 0,
				"Native invulnerability requests no hit-motion delivery; the fixture introduces none");
			if (which.falling()) check(player.onGround() && player.getHealth() < result[0].nativeOutcome.health(), "The actual owner lands and still pays native fall damage");
			trace.record("server-finish", "case=" + which + " nativeDispatches=" + trace.count("server-motion-sent") + " decision=" + result[0].decision, null, Body.of(player));
			trace.assertHealthy(); StoneHingeOwnerProbe.stop(trace); running = null; StoneHingeImpulseProbe.assertIdle();
			Wildercord.LOGGER.info("STONE_HINGE_VELOCITY_CASE case={} decision={} native={} outcome={} ownerLateral={} ownerPeakRise={} movement_gate=NOT_PROVEN", which, result[0].decision,
				result[0].nativeOutcome, result[0].outcome, trace.maximumUncorrectedOwnerLateral(), trace.peakOwnerRise());
		});
		return result[0];
	}

	private void prepare(Case which) {
		if (attacker != null) attacker.discard();
		for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
			player.level().setBlockAndUpdate(new BlockPos(x, 180, z), Blocks.STONE.defaultBlockState());
			for (int y = 181; y <= 196; y++) player.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
		}
		if (which == Case.WALL_CLIPPED) for (int z = -4; z <= 4; z++) for (int y = 181; y <= 186; y++) player.level().setBlockAndUpdate(new BlockPos(-1, y, z), Blocks.STONE.defaultBlockState());
		if (which.falling()) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) player.level().setBlockAndUpdate(new BlockPos(x, 192, z), Blocks.STONE.defaultBlockState());
		player.setGameMode(GameType.SURVIVAL); player.setPermanentlyInvulnerable(which == Case.INVULNERABLE); player.removeAllEffects(); player.setNoGravity(false);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); player.setHealth(200);
		// Native setAbsorptionAmount clamps immediately to this attribute; configure the fixture cap before funding it.
		player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(which == Case.ABSORPTION ? 32 : 0);
		player.setAbsorptionAmount(which == Case.ABSORPTION ? 32 : 0);
		verifyPreparedResources(which, "immediately-funded");
		player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(which == Case.FULL_RESISTANCE ? 1 : which == Case.PARTIAL_RESISTANCE ? .5 : 0);
		for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE); player.setAttached(WildercordAttachments.CIRCLES, 0);
		if (which == Case.ZERO_WOUND) player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 4));
		// Setup arrival only, outside every trace. Genuine owner/server motion must settle before the real hit.
		player.teleportTo(player.level(), .5, which.falling() ? 193 : 181, .5, Set.of(), 0, 0, false);
		attacker = EntityTypes.ZOMBIE.create(player.level(), EntitySpawnReason.COMMAND); check(attacker != null, "Real hostile attacker exists");
		attacker.setNoAi(true); attacker.setNoGravity(true); attacker.setPersistenceRequired(); attacker.setPermanentlyInvulnerable(true);
		attacker.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
		if (attacker.getAttribute(Attributes.ATTACK_KNOCKBACK) != null) attacker.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(0);
		if (which == Case.CALLER_KNOCKBACK) {
			ItemStack blade = new ItemStack(Items.DIAMOND_SWORD);
			blade.enchant(player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK), 1);
			attacker.setItemSlot(EquipmentSlot.MAINHAND, blade);
		}
		attacker.snapTo(.5, which.falling() ? 193 : 181, 2, 180, 0); player.level().addFreshEntity(attacker);
		Wildercord.LOGGER.info("STONE_HINGE_VELOCITY_CASE_SETUP {\"suite\":\"dev.wildercord.aura.StoneHingeVelocityComparisonTest\",\"case\":\"{}\",\"seed\":\"{}\",\"setup\":\"test-only velocity opt-in; controls and payment not exercised\"}", which, player.level().getSeed());
	}
	private void verifyPreparedResources(Case which, String phase) {
		float expectedAbsorption = which == Case.ABSORPTION ? 32 : 0;
		Wildercord.LOGGER.info("STONE_HINGE_VELOCITY_RESOURCES case={} phase={} health={} absorption={} maxAbsorption={}",
			which, phase, player.getHealth(), player.getAbsorptionAmount(), player.getMaxAbsorption());
		check(player.getHealth() == 200 && player.getMaxAbsorption() == expectedAbsorption && player.getAbsorptionAmount() == expectedAbsorption,
			"The real fixture body retains its declared health and absorption resources at " + phase + " for " + which);
	}
	private static void verifyRotation(StoneHingeVelocityExperiment.Strike strike, State before, State after, int sign) {
		var impulse = strike.receipt().impulses().getFirst();
		Vec3 retained = new Vec3(impulse.before().x * .5, 0, impulse.before().z * .5);
		Vec3 nativeHorizontal = new Vec3(impulse.after().x, 0, impulse.after().z).subtract(retained);
		Vec3 changedHorizontal = new Vec3(after.motion().x, 0, after.motion().z).subtract(retained);
		check(close(nativeHorizontal.length(), changedHorizontal.length()) && Math.abs(nativeHorizontal.dot(changedHorizontal)) < EPS
			&& close(changedHorizontal.x, sign * nativeHorizontal.z) && close(changedHorizontal.z, -sign * nativeHorizontal.x), "Only the measured post-resistance native impulse rotates by one quarter-turn with unchanged magnitude");
		check(after.position().equals(before.position()) && after.motion().y == before.motion().y && after.fall() == before.fall() && after.grounded() == before.grounded()
			&& after.health() == before.health() && after.absorption() == before.absorption() && after.needsSync() == before.needsSync() && after.syncVelocity() == before.syncVelocity(),
			"The one write preserves current native Y, position, fall, ground, wound, absorption and both original synchronization flags");
	}
	private static void neutral(ClientGameTestContext context) {
		context.getInput().releaseKey(options -> options.keyUp); context.getInput().releaseKey(options -> options.keyDown);
		context.getInput().releaseKey(options -> options.keyLeft); context.getInput().releaseKey(options -> options.keyRight);
		context.getInput().releaseKey(options -> options.keyJump); context.getInput().releaseKey(options -> options.keyShift); context.getInput().releaseKey(options -> options.keySprint);
	}
	private static boolean close(double left, double right) { return Math.abs(left - right) < EPS; }
	private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
}
