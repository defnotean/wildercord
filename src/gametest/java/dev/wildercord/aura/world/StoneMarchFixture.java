package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/** Shared native fixture: enrollment and ordinary AI own every acceptance. Private combat state is observed, never edited. */
final class StoneMarchFixture {
	final ServerLevel level;
	final Vec3 origin;
	final SwordMaster master;
	final List<? extends ServerPlayer> party;
	private final List<Vec3> staging;
	StoneMarch accepted;
	long began;
	double paidAura;
	int admittedSequence;
	long priorOrdinal;

	StoneMarchFixture(ServerLevel level, Vec3 origin, List<? extends ServerPlayer> party) {
		this.level = level; this.origin = origin; this.party = List.copyOf(party);
		staging = party.stream().map(ServerPlayer::position).toList();
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "Registered Stone Master exists");
		master.plainOrdinaryOnly(); master.setDiscipline(MastersRules.STONE); master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
		for (ServerPlayer player : party) { master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND); }
		check(SwordMaster.ready(party.getFirst()) == 1, "Every challenger explicitly enrolls in the actual trial");
		master.setTarget(party.getFirst());
	}

	void await(TestSingleplayerContext world) {
		double[] before = {master.auraRemaining()};
		long[] ordinal = {0};
		world.getServer().waitFor(server -> {
			if (master.marchPending()) {
				check(master.attackAnimation() == 10 && master.attackElapsed(0) <= 1, "Natural ordinary fallback admits the appended ID10");
				began = level.getGameTime() - (long) master.attackElapsed(0); accepted = (StoneMarch) field(master, "march");
				paidAura = master.auraRemaining(); admittedSequence = (int) field(master, "sequence"); priorOrdinal = ordinal[0];
				check(close(before[0] - paidAura, StoneMarchRules.COST), "Natural admission has exactly one 30-Aura debit");
				check(admittedSequence % 4 == 3 && readyAt() == began + StoneMarchRules.COOLDOWN, "The ordinary cadence and full rest start at acceptance");
				check(planner().state().successfulDecisions() == priorOrdinal + 1 && planner().state().depth() == 0
					&& planner().state().history().getLast().equals("wildercord:master/stone_fault_march"), "External admission records one ID and ends the ordinary phrase");
				for (ServerPlayer player : party) { player.setHealth(200); player.setAbsorptionAmount(0); }
				return true;
			}
			// Keep this fixture's named challenger selected across the native between-attack rotation.
			// This does not change sequence, timing, Aura, guards, the planner or any accepted aim.
			master.setTarget(party.getFirst());
			for (int i = 0; i < party.size(); i++) {
				ServerPlayer player = party.get(i); Vec3 place = staging.get(i);
				player.setHealth(player.getMaxHealth());
				if (player.position().distanceToSqr(place) > .0001)
					player.teleportTo(level, place.x, place.y, place.z, Set.of(), 180, 10, false);
				player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
			}
			before[0] = master.auraRemaining();
			MasterOrdinaryPlanner planner = planner(); ordinal[0] = planner == null ? 0 : planner.state().successfulDecisions();
			return false;
		}, 2400);
		check(master.position().distanceToSqr(origin) < .003 && master.onGround() && !master.guarding(), "Acceptance belongs to a naturally grounded, exposed native body");
	}
	void at(TestSingleplayerContext world, int age, Runnable action) {
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < began + age) return false;
			check(now == began + age, "Observe exact native March tick " + age + ", actual " + (now - began)); action.run(); return true;
		}, age + 20);
	}
	void place(ServerPlayer player, double side, double forward) { place(player, side, 0, forward); }
	void place(ServerPlayer player, double side, double height, double forward) {
		player.teleportTo(level, origin.x + side, origin.y + height, origin.z + forward, Set.of(), 180, 10, false);
		player.setDeltaMovement(Vec3.ZERO);
	}
	long readyAt() { return (long) field(master, "marchReadyAt"); }
	MasterOrdinaryPlanner planner() { return (MasterOrdinaryPlanner) field(master, "ordinaryPlanner"); }
	static Object field(Object owner, String name) {
		try { var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	static boolean close(double a, double b) { return Math.abs(a - b) < .02; }
	static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
