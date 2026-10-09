package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Starlit, Hourglass and Crimson Masters in a real consented trial: an ordinary attack, a named technique and the
 * school's signature, each read once with its correct answer (no damage) and once with a wrong one (damage).
 */
public final class MastersPackBTrialTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static final int[] SCHOOLS = {MastersPackB.STARLIT, MastersPackB.HOURGLASS, MastersPackB.CRIMSON};
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Read { HOLD, WRONG, ANSWER }

	private ServerLevel level;
	private Vec3 origin;
	private SwordMaster master;
	private Challenger target;
	private long began;
	/** The challenger's planted spot and the master's held facing when the signature began. */
	private Vec3 anchor;
	private float facing;

	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level(); origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.MastersPackBTrialTest\",\"seed\":\"{}\"}", level.getSeed());
				for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR);
				observer.teleportTo(level, origin.x, origin.y + 6, origin.z - 6, Set.of(), 0, 30, false);
			});
			for (int school : SCHOOLS) {
				technique(world, school, false);
				technique(world, school, true);
				for (Read read : Read.values()) signature(world, school, read);
			}
		}
	}

	/** The first natural attack is the school's ordinary opener; then one named combo is read wrong or right. */
	private void technique(TestSingleplayerContext world, int school, boolean answer) {
		world.getServer().runOnServer(server -> setup(school, 2.5));
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP) || master.attackAnimation() == 0) return false;
			MastersRules.Move opener = MastersRules.Move.values()[master.attackAnimation() - 1];
			check(opener != MastersRules.Move.TECHNIQUE && !MastersPackB.signature(opener), "The natural opener is a plain ordinary attack: " + opener);
			check(master.attackTellTicks() >= 12 && master.attackTellTicks() <= 14, "Ordinary tell is 12 to 14 ticks: " + opener + " " + master.attackTellTicks());
			return true;
		}, 80);
		world.getServer().waitFor(server -> {
			if (get("attack") != null) return false;
			target.setHealth(HEALTH);
			place(master.position().add(0, 0, 2.5).subtract(origin));
			check(invoke(), "A named technique is admitted in reach");
			began = level.getGameTime();
			return true;
		}, 60);
		MasterTechniques.Technique combo = (MasterTechniques.Technique) get("technique");
		check(combo != null && combo.school() == school && combo.id() > 100, "The combo is one of this school's own: " + combo);
		check(master.technique() == combo.id() && master.attackAnimation() == MastersRules.Move.TECHNIQUE.ordinal() + 1, "The synced combo id is the admitted one");
		int impact = combo.impact(0);
		check(impact >= 12 && impact <= 14, "The first strike tells 12 to 14 ticks: " + combo.key() + " " + impact);
		MasterTechniques.Strike strike = combo.strikes().getFirst();
		at(world, impact - 1, () -> {
			check(target.getHealth() == HEALTH, "The tell is harmless: " + combo.key());
			if (!answer) return;
			Vec3 aim = (Vec3) get("lockedAim");
			check(aim != null, "The aim locks before the strike");
			Vec3 side = new Vec3(-aim.z, 0, aim.x), at = master.position().subtract(origin);
			switch (strike.shape()) {
				case ARC_LOW -> { target.setNoGravity(true); place(at.add(aim.scale(2)).add(0, 1, 0)); }
				case ARC_HIGH -> { target.setShiftKeyDown(true); target.setPose(Pose.CROUCHING); check(target.isCrouching(), "The fixture crouches"); }
				case LANE -> place(at.add(aim.scale(2)).add(side.scale(2.5)));
				case ARC, CIRCLE -> place(at.add(aim.scale(strike.reach() + 2.5)));
			}
		});
		at(world, impact, () -> {
			String receipt = combo.key() + " " + strike.shape() + " health=" + target.getHealth();
			if (answer) check(target.getHealth() == HEALTH, "The named answer avoids the strike: " + receipt);
			else check(target.getHealth() < HEALTH && target.getLastDamageSource() != null
				&& target.getLastDamageSource().getEntity() == master, "Standing still is hit by the owned strike: " + receipt);
			check(HEALTH - target.getHealth() <= MastersPackB.CHAIN_CAP + .01, "One strike stays within the chain cap");
			target.setNoGravity(false); target.setShiftKeyDown(false); target.setPose(Pose.STANDING);
			cleanup();
		});
	}

	private void signature(TestSingleplayerContext world, int school, Read read) {
		MastersRules.Move move = MastersPackB.signature(school);
		double distance = school == MastersPackB.STARLIT ? 4 : 3;
		world.getServer().runOnServer(server -> {
			setup(school, distance);
			set("sequence", school == MastersPackB.STARLIT ? 1 : school == MastersPackB.HOURGLASS ? 2 : 3);
		});
		world.getServer().waitFor(server -> {
			if (!master.packBPending()) return false;
			began = level.getGameTime() - (long) master.attackElapsed(0);
			anchor = target.position(); facing = master.getYRot();
			check(master.attackAnimation() == move.ordinal() + 1 && master.attackTellTicks() == move.tell,
				"Ordinary server AI admits the appended signature: " + master.attackAnimation());
			check(master.auraRemaining() <= MastersRules.AURA_MAX - MastersPackB.cost(move) + .01, "The signature pays its Aura once");
			return true;
		}, 80);
		float masterBefore = master.getMaxHealth(); // The fight starts at full health and the frenzy pays its price on its first tick.
		Object accepted = get("packB");
		switch (school) {
			case MastersPackB.STARLIT -> starlit(world, read);
			case MastersPackB.HOURGLASS -> hourglass(world, read);
			default -> crimson(world, read, masterBefore);
		}
		at(world, move.tell + 1, () -> {
			check(!master.packBPending() && !master.state(AuraFighter.WINDUP), "The form ends in an open recovery: " + move);
			check(get("packB") == null && accepted != null, "A finished form is released");
			cleanup();
		});
	}

	private void starlit(TestSingleplayerContext world, Read read) {
		int first = StarlitConstellationRules.burst(0), second = StarlitConstellationRules.burst(1);
		if (read != Read.HOLD) at(world, first - 2, () -> frame(-2, 0, 0));
		at(world, first, () -> check(lost() == (read == Read.HOLD ? StarlitConstellationRules.DAMAGE : 0),
			"The first star bursts under the challenger's start: " + read + " lost=" + lost()));
		// Dodging the first star onto the second is the wrong read: the drawn order matters.
		if (read == Read.WRONG) at(world, first + 1, () -> frame(0, StarlitConstellationRules.SPREAD, 0));
		at(world, second, () -> check(lost() == (read == Read.ANSWER ? 0 : StarlitConstellationRules.DAMAGE),
			"Standing on the next star is hit: " + read + " lost=" + lost()));
		at(world, StarlitConstellationRules.TELL, () -> check(lost() == (read == Read.ANSWER ? 0 : StarlitConstellationRules.DAMAGE),
			"Leaving each star before its turn takes nothing: " + read + " lost=" + lost()));
	}

	private void hourglass(TestSingleplayerContext world, Read read) {
		int strike = HourglassRewindRules.STRIKE, replay = HourglassRewindRules.REPLAY;
		if (read != Read.HOLD) at(world, strike - 2, () -> frame(0, 2, 0));
		at(world, strike, () -> check(close(lost(), read == Read.HOLD ? HourglassRewindRules.DAMAGE : 0),
			"The first cut hits only the lane: " + read + " lost=" + lost()));
		// The spent lane is not safe: stepping back in is the wrong read.
		at(world, strike + 1, () -> { if (read != Read.ANSWER) frame(0, 0, 0); });
		at(world, replay, () -> {
			double expected = read == Read.HOLD ? MastersPackB.CHAIN_CAP : read == Read.WRONG ? HourglassRewindRules.DAMAGE : 0;
			check(close(lost(), expected), "The replay cuts the same lane, capped per challenger: " + read + " lost=" + lost());
		});
	}

	private void crimson(TestSingleplayerContext world, Read read, float masterBefore) {
		int[] beats = CrimsonFrenzyRules.BEATS;
		at(world, 1, () -> check(close(master.getHealth(), masterBefore - CrimsonFrenzyRules.price(masterBefore)),
			"The frenzy pays its blood price up front: " + master.getHealth()));
		for (int beat = 0; beat < beats.length; beat++) {
			int b = beat;
			at(world, beats[b] - CrimsonFrenzyRules.LOCK - 1, () -> frame(0, 0, 0));
			at(world, beats[b] - 1, () -> {
				frame(0, 0, 0);
				if (read == Read.ANSWER) switch (b) {
					case 0 -> { target.setNoGravity(true); frame(0, 0, 1); }
					case 1 -> { target.setShiftKeyDown(true); target.setPose(Pose.CROUCHING); }
					case 2 -> frame(0, 2, 0);
					default -> frame(3, 0, 0);
				}
				// Crouching under a low cut is the wrong read.
				if (read == Read.WRONG && b == 0) { target.setShiftKeyDown(true); target.setPose(Pose.CROUCHING); }
				if (read == Read.WRONG && b > 0) frame(3.5, 0, 0);
			});
			at(world, beats[b], () -> {
				double expected = switch (read) {
					case HOLD -> Math.min(MastersPackB.CHAIN_CAP, (b + 1) * CrimsonFrenzyRules.DAMAGE);
					case WRONG -> CrimsonFrenzyRules.DAMAGE;
					case ANSWER -> 0;
				};
				check(close(lost(), expected), "Frenzy beat " + b + " " + CrimsonFrenzyRules.SHAPES[b] + ": " + read + " lost=" + lost());
				target.setNoGravity(false); target.setShiftKeyDown(false); target.setPose(Pose.STANDING);
				if (read != Read.ANSWER && b == 0) check(master.getHealth() > masterBefore - CrimsonFrenzyRules.price(master.getMaxHealth()) + .01,
					"A landed beat heals the master");
			});
		}
		at(world, CrimsonFrenzyRules.TELL, () -> {
			if (read == Read.ANSWER) {
				check(close(master.getHealth(), masterBefore - CrimsonFrenzyRules.price(master.getMaxHealth())), "A clean read heals nothing");
				check((long) get("recoverUntil") >= began + CrimsonFrenzyRules.TELL + CrimsonFrenzyRules.RECOVERY + CrimsonFrenzyRules.MISS_RECOVERY,
					"A clean read leaves the master open longer");
			}
		});
	}

	private void setup(int school, double distance) {
		target = new Challenger(level, "PackBTarget"); target.setGameMode(GameType.SURVIVAL);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); target.setHealth(HEALTH);
		target.snapTo(origin.x, origin.y, origin.z + distance, 180, 0); level.addNewPlayer(target);
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "The registered Master exists");
		master.setDiscipline(school);
		check(master.method().equals(MastersPackB.method(school)), "The Master teaches its own school");
		master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
		master.mobInteract(target, InteractionHand.MAIN_HAND); master.mobInteract(target, InteractionHand.MAIN_HAND);
		check(SwordMaster.ready(target) == 1, "The challenger explicitly accepts this trial");
		master.setTarget(target);
	}

	private double lost() { return HEALTH - target.getHealth(); }
	private void place(Vec3 offset) {
		target.teleportTo(level, origin.x + offset.x, origin.y + offset.y, origin.z + offset.z, Set.of(), 180, 0, false);
		target.setDeltaMovement(Vec3.ZERO);
	}
	/** Moves the challenger relative to where it stood when the form began, along and across the master's held facing. */
	private void frame(double forward, double side, double up) {
		double radians = Math.toRadians(facing);
		Vec3 f = new Vec3(-Math.sin(radians), 0, Math.cos(radians)), right = new Vec3(-f.z, 0, f.x);
		place(anchor.add(f.scale(forward)).add(right.scale(side)).add(0, up, 0).subtract(origin));
	}
	private void cleanup() {
		if (master != null) master.discard();
		if (target != null) target.discard();
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < expected) return false;
			check(now == expected, "Observe exact native frame " + age + ": expected=" + expected + ", actual=" + now); action.run(); return true;
		}, age + 20);
	}
	private boolean invoke() {
		try {
			var method = SwordMaster.class.getDeclaredMethod("beginTechnique", ServerLevel.class, net.minecraft.world.entity.LivingEntity.class, long.class);
			method.setAccessible(true);
			return (boolean) method.invoke(master, level, target, level.getGameTime());
		} catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private Object get(String name) {
		try { var field = SwordMaster.class.getDeclaredField(name); field.setAccessible(true); return field.get(master); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private void set(String name, int value) {
		try { var field = SwordMaster.class.getDeclaredField(name); field.setAccessible(true); field.setInt(master, value); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
