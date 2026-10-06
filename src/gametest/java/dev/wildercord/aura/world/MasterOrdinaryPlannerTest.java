package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.Effects;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Native ordinary AI owns admission. Passive fake challengers exercise actual combat and lifecycle boundaries. */
public final class MasterOrdinaryPlannerTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, int id) { super(level, new GameProfile(new UUID(0x6f7264696e617279L, id), "Ordinary" + id)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Counter { HOLD, SIDESTEP, COVER, GUARD, PARRY, ABSORPTION, INTERRUPT, OWNED_INTERRUPT, OUTSIDE_INTERRUPT, REENTER, CALLBACK_CANCEL, NO_AI,
		TARGET_LEAVE, TARGET_DEATH, TARGET_DIMENSION, ROSTER_LEAVE, TARGET_CHANGE, OWNER_REMOVE }
	private ServerLevel level;
	private Vec3 origin;
	private SwordMaster master;
	private Challenger target, outsider;
	private final List<Challenger> party = new ArrayList<>();
	private MastersRules.Move move;
	private MasterMovePlanner.State admitted;
	private long began;
	private double paidAura;
	private int serial;
	private static boolean registered;
	private static MasterOrdinaryPlannerTest active;
	private Counter counter;
	private int callbacks;

	@Override public void runTest(ClientGameTestContext context) {
		registerCallback();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst(); level = observer.level();
				origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				for (int x = -28; x <= 48; x++) for (int z = -28; z <= 28; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR); place(observer, 0, 3, -5);
			});
			for (int school = 0; school < 3; school++) for (int phase = 0; phase < 3; phase++)
				for (int count : new int[] {1, 8}) scenario(world, school, phase, count, Counter.HOLD);
			for (Counter counter : Counter.values()) if (counter != Counter.HOLD)
				scenario(world, MastersRules.GALE, 0, counter == Counter.REENTER || counter == Counter.CALLBACK_CANCEL ? 8
					: counter == Counter.ROSTER_LEAVE || counter == Counter.TARGET_CHANGE ? 2 : 1, counter);
			staleAdmission(world);
			missedReleaseCallback(world);
		} finally { if (active == this) active = null; }
	}

	private void scenario(TestSingleplayerContext world, int school, int phase, int count, Counter counter) {
		this.counter = counter; callbacks = 0;
		beginPlanned(world, school, phase, count);
		at(world, 3, () -> {
			check(target.getHealth() == HEALTH, "A planner choice retains the complete harmless tell");
			var state = planner().state();
			master.customServerAiStep(level); master.customServerAiStep(level);
			check(planner().state().equals(state) && close(master.auraRemaining(), paidAura), "Duplicate AI ticks cannot debit or append admission twice");
			check(!invokeAdmission(planner().propose(MastersRules.Move.SWEEP, Set.of(MastersRules.Move.SWEEP), paidAura)),
				"An in-flight attack refuses a second proposal without payment");
			switch (counter) {
				case INTERRUPT -> Effects.withSource(target, () -> check(master.interruptWindup(), "An enrolled early interrupt cancels a paid ordinary warning"));
				case OWNED_INTERRUPT -> {
					Wolf companion = companion(target);
					Effects.withSource(companion, () -> check(master.interruptWindup(), "Enrolled companion ownership remains a lawful incoming source")); companion.discard();
				}
				case OUTSIDE_INTERRUPT -> {
					Wolf companion = companion(outsider);
					Effects.withSource(companion, () -> check(!master.interruptWindup(), "An unenrolled companion owner cannot interrupt this graph-selected attack")); companion.discard();
				}
				case NO_AI -> master.setNoAi(true);
				case TARGET_LEAVE -> place(target, 40, 0, 0);
				case TARGET_DEATH -> { target.setHealth(0); target.die(level.damageSources().generic()); }
				case TARGET_DIMENSION -> {
					ServerLevel other = level.getServer().getLevel(Level.NETHER); check(other != null, "A native second dimension is available");
					target.setNoGravity(true); target.teleportTo(other, origin.x, 200, origin.z, Set.of(), 180, 0, false);
				}
				case ROSTER_LEAVE -> place(party.getLast(), 40, 0, 0);
				case TARGET_CHANGE -> master.setTarget(party.getLast());
				case OWNER_REMOVE -> master.discard();
				case GUARD -> guard();
				case ABSORPTION -> { target.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(100); target.setAbsorptionAmount(100); }
				case COVER -> place(target, 0, 0, 3);
				case REENTER, CALLBACK_CANCEL -> {
					check(move != MastersRules.Move.CRESCENT, "The callback probe owns a native graph-selected melee release"); active = this;
				}
				default -> {}
			}
		});
		at(world, move.tell - MastersRules.AIM_LOCK + 1, () -> {
			if (counter == Counter.SIDESTEP) place(target, 8, 0, 1.2);
			if (counter == Counter.COVER) wall(true);
			if (counter == Counter.NO_AI) master.setNoAi(false);
		});
		at(world, move.tell - 1, () -> { if (counter == Counter.PARRY) guard(); });
		at(world, move.tell + 4, () -> {
			if (active == this) active = null;
			boolean terminal = counter == Counter.TARGET_DEATH || counter == Counter.TARGET_DIMENSION || counter == Counter.OWNER_REMOVE;
			if (counter == Counter.HOLD || counter == Counter.OUTSIDE_INTERRUPT || counter == Counter.REENTER) {
				check(target.getHealth() < HEALTH, "Holding the warned line loses health to the actual native executor: " + move);
				for (var player : party) {
					double lost = HEALTH - player.getHealth();
					check(close(lost, 0) || close(lost, MastersRules.damage(count, school, move)),
						"Every struck party member takes exactly one unchanged budget, independent of volley overlap");
				}
				check(party.stream().filter(player -> player.getHealth() < HEALTH).count() >= Math.min(2, count),
					"Native party pressure reaches multiple enrolled witnesses without assuming Crescent exceeds its existing per-flight target cap");
				check(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == master,
					"The selected ordinary executor retains native hit ownership");
			} else if (counter == Counter.CALLBACK_CANCEL) {
				check(callbacks == 1 && party.stream().filter(player -> player.getHealth() < HEALTH).count() == 1,
					"Cancellation inside the first native damage callback prevents every later roster hit");
			} else if (!terminal && counter != Counter.GUARD && counter != Counter.ROSTER_LEAVE && counter != Counter.TARGET_CHANGE)
				check(target.getHealth() == HEALTH, "The real ordinary counter avoids health damage: " + counter + "/" + move);
			if (counter == Counter.REENTER) check(callbacks == count, "Re-entered AI never repeats any party member's native strike");
			if (counter == Counter.GUARD) check(target.getHealth() > HEALTH - MastersRules.damage(count, school, move), "Held frontal guard still mitigates the selected attack");
			if (counter == Counter.ABSORPTION) check(close(target.getAbsorptionAmount(), 100 - MastersRules.damage(count, school, move)), "One admitted attack debits absorption only once");
			check(outsider.getHealth() == HEALTH, "The planner never adds an unenrolled outgoing target");
			check(planner().state().successfulDecisions() == admitted.successfulDecisions()
				&& planner().state().history().equals(admitted.history()), "Counters and native target/roster changes retain the one accepted history entry");
			if (counter == Counter.INTERRUPT || counter == Counter.OWNED_INTERRUPT || counter == Counter.CALLBACK_CANCEL || counter == Counter.NO_AI || counter == Counter.TARGET_LEAVE
				|| counter == Counter.TARGET_DEATH || counter == Counter.TARGET_DIMENSION || counter == Counter.ROSTER_LEAVE
				|| counter == Counter.TARGET_CHANGE || counter == Counter.OWNER_REMOVE)
				check(planner().state().depth() == 0, "Interruption, lifecycle, target or roster changes end the phrase: " + counter);
		});
		at(world, move.tell + move.recovery - 1, () -> {
			check(!master.state(AuraFighter.WINDUP) && !master.guarding() && close(master.auraRemaining(), paidAura),
				"Every admitted ordinary action keeps its full paid, exposed recovery, including early cancellation: " + counter);
			if (counter == Counter.INTERRUPT || counter == Counter.OWNED_INTERRUPT || counter == Counter.NO_AI) {
				check(level.getGameTime() > began + 3 + 20 && planner().state().successfulDecisions() == admitted.successfulDecisions(),
					"Before this change cancellation could retry at interrupt+20; now the original complete tell+recovery still blocks admission");
			}
			if (counter == Counter.HOLD) {
				float health = master.getHealth(); Effects.readyToHurt(master);
				check(master.hurtServer(level, level.damageSources().playerAttack(target), 4) && master.getHealth() < health,
					"An enrolled opponent can punish the last recovery frame in every phase and party size");
			}
			cleanup();
		});
	}

	/** The first opener, all of its sub-beats/recovery, and Stone's due guard execute naturally before the graph slot. */
	private void beginPlanned(TestSingleplayerContext world, int school, int phase, int count) {
		world.getServer().runOnServer(server -> setup(school, count));
		world.getServer().waitFor(server -> {
			if (!master.started()) return false;
			master.setHealth(master.getMaxHealth() * (phase == 0 ? 1 : phase == 1 ? .6F : .3F)); return true;
		}, 10);
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			move = MasterMoveCatalog.legacy().byWireId(master.attackAnimation()).orElseThrow().legacyMove();
			began = level.getGameTime() - (long) master.attackElapsed(0);
			check(planner().state().successfulDecisions() == 1 && planner().state().depth() == 0, "The original phase-aware opener enters history without a graph phrase");
			return true;
		}, 30);
		at(world, move.tell + move.recovery - 1, () -> {
			for (int i = 0; i < party.size(); i++) { Challenger player = party.get(i); player.setHealth(HEALTH); place(player, partyX(i), 0, partyZ(i)); }
			master.setTarget(target);
			check(planner().state().successfulDecisions() == 1 && !master.state(AuraFighter.WINDUP), "The graph cannot shorten an opening or its internal sub-beats");
		});
		world.getServer().waitFor(server -> {
			if (planner().state().successfulDecisions() < 2) return false;
			check(planner().state().successfulDecisions() == 2 && master.state(AuraFighter.WINDUP) && master.attackElapsed(0) <= 1,
				"Observe the exact next naturally admitted action");
			move = MasterMoveCatalog.legacy().byWireId(master.attackAnimation()).orElseThrow().legacyMove();
			check(planner().state().depth() == 1 && (move == MastersRules.Move.SWEEP || move == MastersRules.Move.THRUST || move == MastersRules.Move.CRESCENT),
				"Only a native ordinary slot reaches the bounded graph: " + move);
			admitted = planner().state(); began = level.getGameTime() - (long) master.attackElapsed(0); paidAura = master.auraRemaining();
			double expected = MastersRules.AURA_MAX - (school == MastersRules.EMBER ? EmberWakeRules.COST : MastersRules.ATTACK_COST)
				- MastersRules.ATTACK_COST - (school == MastersRules.STONE ? MastersRules.GUARD_COST : 0);
			check(close(paidAura, expected) && !master.guarding(), "Opening, mandatory guard and one ordinary start each pay exactly once");
			check(admitted.history().getLast().equals(MasterMoveCatalog.legacy().forMove(move).id()), "History commits the actual admitted authored ID");
			var replay = new MasterOrdinaryPlanner(school, admitted.encounterSeed());
			replay.observe(target.getUUID(), master.challengers());
			replay.admittedExternal(MasterMoveCatalog.legacy().byId(admitted.history().getFirst()).orElseThrow().legacyMove());
			double observedDistance = master.distanceTo(target), observedHeight = target.getBoundingBox().getCenter().y - master.slashOrigin().y;
			var eligible = MasterOrdinaryPlanner.spatialCandidates(observedDistance, observedHeight);
			var preferred = MastersRules.move(school, 1, phase, observedDistance);
			var proposal = replay.propose(preferred, eligible, paidAura + MastersRules.ATTACK_COST);
			check(proposal.move() == move && replay.admitted(proposal) && replay.state().equals(admitted), "Seed plus accepted eligibility/phase snapshot exactly replays native admission");
			dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_ORDINARY_REPLAY version={} graph={} before={} accepted={} school={} phase={} party={} preferred={} eligible={} distance={} height={} auraBefore={} target={} roster={} began={}",
				MasterOrdinaryPlanner.VERSION, proposal.graphId(), proposal.before(), admitted, school, phase, count, preferred,
				eligible.stream().sorted().toList(), observedDistance, observedHeight, paidAura + MastersRules.ATTACK_COST,
				target.getUUID(), master.challengers().stream().sorted().toList(), began);
			return true;
		}, 90);
	}

	private void staleAdmission(TestSingleplayerContext world) {
		beginPlanned(world, MastersRules.GALE, 0, 2);
		at(world, 3, () -> master.setNoAi(true));
		at(world, move.tell + move.recovery, () -> {
			// Let the original full reservation expire naturally while paused, then test a genuinely free native slot.
			master.setNoAi(false);
			var proposal = planner().propose(MastersRules.Move.SWEEP, liveEligibility(), paidAura);
			check(proposal.move() != null, "The original target has a legal proposal in a free native slot");
			var state = planner().state(); master.setTarget(party.getLast());
			planner().observe(master.getTarget().getUUID(), master.challengers());
			check(liveEligibility().contains(proposal.move()), "The replacement target still permits the same move; no busy/recovery/resource gate explains rejection");
			var changed = planner().state();
			check(!invokeAdmission(proposal) && planner().state().equals(changed) && close(master.auraRemaining(), paidAura),
				"A stale target ticket cannot change history, Aura, cooldowns or the existing commitment");
			check(state.history().equals(changed.history()) && changed.depth() == 0, "Target changes end only the phrase");
			var fresh = planner().propose(MastersRules.Move.SWEEP, liveEligibility(), paidAura);
			check(invokeAdmission(fresh) && planner().state().successfulDecisions() == changed.successfulDecisions() + 1
				&& close(master.auraRemaining(), paidAura - MastersRules.ATTACK_COST), "A fresh ticket succeeds in the same free slot and pays once");
			cleanup();
		});
	}

	/** Explicit stale-callback probe on native entities; this does not claim to simulate an actually skipped server tick. */
	private void missedReleaseCallback(TestSingleplayerContext world) {
		beginPlanned(world, MastersRules.GALE, 0, 1);
		at(world, 3, () -> {
			invoke("tickAttack", new Class<?>[] {ServerLevel.class, long.class, LivingEntity.class}, level, began + move.tell + 1, target);
			check(master.attackAnimation() == 0 && target.getHealth() == HEALTH && close(master.auraRemaining(), paidAura),
				"Previously a late ordinary callback could hit; now it expires harmlessly with its Aura still paid");
			check(planner().state().depth() == 0 && planner().state().history().equals(admitted.history()), "Late callback cancellation keeps accepted history");
			check(recoveryUntil() >= began + move.tell + 1 + move.recovery, "A late release reserves full recovery after the expired callback");
		});
		at(world, move.tell + move.recovery, () -> {
			check(level.getGameTime() < recoveryUntil() && !master.state(AuraFighter.WINDUP) && close(master.auraRemaining(), paidAura)
				&& planner().state().successfulDecisions() == admitted.successfulDecisions(), "The retained late-release deadline actually blocks another start");
			cleanup();
		});
	}

	private void setup(int school, int count) {
		serial++;
		target = add(serial * 10, partyX(0), partyZ(0)); party.add(target);
		for (int i = 1; i < count; i++) party.add(add(serial * 10 + i, partyX(i), partyZ(i)));
		outsider = add(serial * 10 + 9, 0, 2.05);
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND); check(master != null, "Registered master exists");
		master.setUUID(new UUID(0x6d61737465726d6fL, serial)); master.setDiscipline(school); master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
		for (var player : party) { master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND); }
		check(SwordMaster.ready(target) == 1, "Each native challenger explicitly enrolled before roster lock"); master.setTarget(target);
	}
	private Challenger add(int id, double x, double z) {
		var player = new Challenger(level, id); player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 180, 0); level.addNewPlayer(player); return player;
	}
	private static double partyX(int index) { return index == 0 ? 0 : index % 2 == 1 ? -.65 : .65; }
	private static double partyZ(int index) { return index == 0 ? 1.2 : 1.2 + (index - 1) / 2 * .85; }
	private void guard() {
		target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
		target.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE); target.setShiftKeyDown(true);
		target.setYRot(180); target.setYHeadRot(180); target.setYBodyRot(180); target.setXRot(0);
		check(AuraGuard.raise(target) && AuraGuard.facing(target, master.position()), "Native guard faces the admitted origin");
	}
	private Wolf companion(ServerPlayer owner) {
		Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND); check(wolf != null, "Native companion exists");
		wolf.tame(owner); wolf.setNoAi(true); wolf.snapTo(origin.x + 12, origin.y, origin.z, 0, 0); level.addFreshEntity(wolf);
		check(!master.canHarmParticipant(wolf) && master.projected(wolf, 26) == 0, "Incoming owner attribution never enrolls the companion as an outgoing victim");
		return wolf;
	}
	private void wall(boolean present) {
		for (int y = 0; y < 3; y++) level.setBlockAndUpdate(BlockPos.containing(origin).offset(0, y, 1), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void place(ServerPlayer player, double x, double y, double z) {
		player.teleportTo(level, origin.x + x, origin.y + y, origin.z + z, Set.of(), 180, 0, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			if (level.getGameTime() < expected) return false;
			check(level.getGameTime() == expected, "Observe exact ordinary frame " + age + ": " + level.getGameTime() + " vs " + expected);
			action.run(); return true;
		}, age + 30);
	}
	private MasterOrdinaryPlanner planner() {
		try { var field = SwordMaster.class.getDeclaredField("ordinaryPlanner"); field.setAccessible(true); return (MasterOrdinaryPlanner) field.get(master); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private boolean invokeAdmission(MasterOrdinaryPlanner.Proposal proposal) {
		return (boolean) invoke("tryBeginOrdinary", new Class<?>[] {ServerLevel.class, LivingEntity.class, long.class, MasterOrdinaryPlanner.Proposal.class}, level, master.getTarget(), level.getGameTime(), proposal);
	}
	@SuppressWarnings("unchecked") private Set<MastersRules.Move> liveEligibility() {
		return (Set<MastersRules.Move>) invoke("ordinaryEligibility", new Class<?>[] {ServerLevel.class, LivingEntity.class, long.class}, level, master.getTarget(), level.getGameTime());
	}
	private long recoveryUntil() {
		try { var field = SwordMaster.class.getDeclaredField("recoverUntil"); field.setAccessible(true); return field.getLong(master); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private Object invoke(String name, Class<?>[] types, Object... args) {
		try { Method method = SwordMaster.class.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(master, args); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private void cleanup() {
		if (active == this) active = null;
		if (master != null) master.discard();
		for (var player : party) player.discard(); party.clear(); if (outsider != null) outsider.discard(); wall(false);
	}
	private static void registerCallback() {
		if (registered) return; registered = true;
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			var fixture = active;
			if (fixture == null || source.getEntity() != fixture.master || !fixture.party.contains(entity)) return;
			check(++fixture.callbacks <= fixture.party.size(), "A native reentrant callback cannot exceed the attack-wide hit budget");
			var before = fixture.planner().state(); double aura = fixture.master.auraRemaining();
			fixture.master.customServerAiStep(fixture.level);
			check(fixture.planner().state().equals(before) && close(fixture.master.auraRemaining(), aura), "Reentrant AI cannot select or pay again");
			if (fixture.counter == Counter.CALLBACK_CANCEL) fixture.master.setNoAi(true);
		});
	}
	private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
