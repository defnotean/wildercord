package dev.wildercord.monster;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Native entity, goal, navigation and release checks. Deadlines are advanced explicitly in this deterministic fixture;
 * it does not measure live fight difficulty, movement throughput, or multiplayer outcomes.
 */
public final class WildMonsterPressureTest implements FabricClientGameTest {
	private BlockPos stage;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				stage = new BlockPos(player.getBlockX(), 180, player.getBlockZ());
				for (int x = -16; x <= 16; x++) for (int z = -20; z <= 20; z++) {
					level.setBlockAndUpdate(stage.offset(x, 0, z), Blocks.STONE.defaultBlockState());
					for (int y = 1; y <= 6; y++) level.setBlockAndUpdate(stage.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, stage.getX() + .5, stage.getY() + 1, stage.getZ() + .5, Set.of(), 0, 0, false);
				player.setHealth(player.getMaxHealth());
				gloomPressureAndSafety(level, player);
				frogPressureAndSafety(level, player);
			});
			world.getServer().runCommand("difficulty easy");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				Gloomstalker gloom = gloom(level, player, 9.75);
				Goal hunt = goal(gloom, "HuntGoal");
				hunt.start();
				int stalk = (Integer) read(hunt, "stalkTime");
				check(stalk >= 40 && stalk <= 79, "Easy's original initial stalk interval remains wired to the goal");
				gloom.discard();
				BogWitchFrog frog = frog(level, player, 5);
				frog.customServerAiStep(level);
				check(frog.swelling(), "Easy still chooses a bubble before its tongue at five blocks");
				long interval = (Long) read(frog, "nextBubble") - level.getGameTime();
				check(interval >= 80 && interval <= 129, "Easy's original bubble cadence remains wired to the entity");
				frog.discard();
				frog = frog(level, player, 17);
				check(!goal(frog, "ApproachGoal").canUse(), "The new pursuit goal is disabled on Easy");
				frog.discard();
			});
		}
	}

	private void gloomPressureAndSafety(ServerLevel level, ServerPlayer player) {
		Gloomstalker gloom = gloom(level, player, 9.75);
		check(gloom.getMaxHealth() == 24, "Ordinary Gloomstalker health is unchanged");
		Goal hunt = goal(gloom, "HuntGoal");
		hunt.start();
		int stalk = (Integer) read(hunt, "stalkTime");
		check(stalk >= 26 && stalk <= 45, "Normal's shorter initial stalk interval reaches the real goal");
		Vec3 before = gloom.position();
		hunt.tick();
		check(player.blockPosition().equals(gloom.getNavigation().getTargetPos()),
			"The old 9.5–10 block orbit gap now selects a real path to the target");
		check(gloom.getNavigation().getPath() != null, "Pursuit uses native navigation on the flat fixture");
		check(before.equals(gloom.position()), "Choosing a route does not teleport the monster");
		gloom.discard();

		gloom = gloom(level, player, 8);
		hunt = goal(gloom, "HuntGoal");
		hunt.start();
		write(hunt, "stalkTime", 0);
		hunt.tick();
		check(gloom.crouching(), "An in-range visible target triggers the pounce tell");
		check((Long) read(gloom, "phaseUntil") - level.getGameTime() == 14, "All fourteen crouch ticks remain");
		wall(level, true);
		check(!gloom.hasLineOfSight(player), "The stone wall really occludes the target");
		float health = player.getHealth();
		write(gloom, "phaseUntil", level.getGameTime());
		gloom.customServerAiStep(level);
		check(!gloom.crouching() && !gloom.pouncing() && player.getHealth() == health,
			"Moving behind cover during the tell cancels the leap without damage");
		check((Long) read(gloom, "nextPounce") - level.getGameTime() == 20, "A canceled leap cannot restart its tell immediately");
		write(hunt, "repath", 0);
		hunt.tick();
		check(player.blockPosition().equals(gloom.getNavigation().getTargetPos()), "A covered target selects ground pursuit instead of circling");
		wall(level, false);

		write(gloom, "nextPounce", 0L);
		write(hunt, "stalkTime", 0);
		hunt.tick();
		write(gloom, "phaseUntil", level.getGameTime());
		gloom.customServerAiStep(level);
		check(gloom.pouncing() && gloom.getDeltaMovement().lengthSqr() > 0, "An unobstructed tell still releases a real leap");
		write(gloom, "pounceTicks", 30);
		gloom.customServerAiStep(level);
		check(gloom.state(WildMonster.STUNNED), "A missed leap still exposes the monster");
		check((Long) read(gloom, "phaseUntil") - level.getGameTime() == 30, "All thirty missed-pounce recovery ticks remain");
		gloom.customServerAiStep(level);
		check(gloom.state(WildMonster.STUNNED), "Pressure changes cannot skip the live recovery window");
		gloom.discard();
	}

	private void frogPressureAndSafety(ServerLevel level, ServerPlayer player) {
		BogWitchFrog frog = frog(level, player, 5);
		check(frog.getMaxHealth() == 32, "Ordinary frog health is unchanged");
		frog.customServerAiStep(level);
		check(frog.state(WildMonster.ALT) && !frog.swelling(), "Normal chooses its ready tongue before the bubble at five blocks");
		check((Long) read(frog, "phaseUntil") - level.getGameTime() == 10, "Tongue priority preserves the full mouth-open tell");
		frog.discard();

		frog = frog(level, player, 8);
		frog.customServerAiStep(level);
		check(frog.swelling(), "A visible ranged target still starts the bubble tell");
		check((Long) read(frog, "phaseUntil") - level.getGameTime() == 18, "All eighteen swelling ticks remain");
		long next = (Long) read(frog, "nextBubble");
		check(next - level.getGameTime() >= 60 && next - level.getGameTime() <= 89, "Normal's bubble interval reaches the real entity");
		int bubbles = level.getEntitiesOfClass(BogBubble.class, frog.getBoundingBox().inflate(24)).size();
		wall(level, true);
		check(!frog.hasLineOfSight(player), "The target is actually behind cover");
		write(frog, "phaseUntil", level.getGameTime());
		frog.customServerAiStep(level);
		check(!frog.swelling() && level.getEntitiesOfClass(BogBubble.class, frog.getBoundingBox().inflate(24)).size() == bubbles,
			"A target taking cover during swelling does not receive a blind bubble");
		check((Long) read(frog, "nextBubble") == next, "Canceling a covered shot keeps its already reserved cooldown");
		Goal approach = goal(frog, "ApproachGoal");
		check(approach.canUse(), "A covered target activates the dedicated pursuit goal");
		approach.tick();
		long nextRepath = (Long) read(approach, "nextRepath");
		check(nextRepath > level.getGameTime(), "Navigation records a future retry rather than querying each tick");
		approach.tick();
		check((Long) read(approach, "nextRepath") == nextRepath, "An immediate repeated goal tick cannot request a new path");
		wall(level, false);
		frog.discard();

		frog = frog(level, player, 17);
		approach = goal(frog, "ApproachGoal");
		check(approach.canUse(), "A ranged kiter outside bubble reach activates pursuit");
		approach.tick();
		check(player.blockPosition().equals(frog.getNavigation().getTargetPos()), "The frog walks toward the kiter using a native path");
		check(frog.getNavigation().getPath() != null, "The kiter's route is available on flat ground");
		frog.discard();
	}

	private Gloomstalker gloom(ServerLevel level, ServerPlayer player, double distance) {
		Gloomstalker mob = MonsterContent.GLOOMSTALKER.create(level, EntitySpawnReason.COMMAND);
		check(mob != null, "Registered Gloomstalker constructs");
		place(mob, level, player, distance);
		return mob;
	}

	private BogWitchFrog frog(ServerLevel level, ServerPlayer player, double distance) {
		BogWitchFrog mob = MonsterContent.BOG_WITCH_FROG.create(level, EntitySpawnReason.COMMAND);
		check(mob != null, "Registered Bog Witch-Frog constructs");
		place(mob, level, player, distance);
		return mob;
	}

	private void place(Mob mob, ServerLevel level, ServerPlayer player, double distance) {
		// Keep these ordinary-mob witnesses out of the random Runebound roll on entity load.
		mob.addTag("wildercord.rolled");
		mob.snapTo(stage.getX() + .5, stage.getY() + 1, stage.getZ() + .5 + distance, 180, 0);
		mob.setNoAi(true);
		mob.setOnGround(true);
		mob.setTarget(player);
		level.addFreshEntity(mob);
	}

	private void wall(ServerLevel level, boolean present) {
		for (int x = -2; x <= 2; x++) for (int y = 1; y <= 4; y++) {
			level.setBlockAndUpdate(stage.offset(x, y, 4), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
		}
	}

	private static Goal goal(Mob mob, String name) {
		try {
			Field field = Mob.class.getDeclaredField("goalSelector");
			field.setAccessible(true);
			for (var wrapped : ((GoalSelector) field.get(mob)).getAvailableGoals()) {
				if (wrapped.getGoal().getClass().getSimpleName().equals(name)) return wrapped.getGoal();
			}
			throw new AssertionError("Registered goal missing: " + name);
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError(failure);
		}
	}

	private static Object read(Object owner, String name) {
		try {
			Field field = owner.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(owner);
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError(failure);
		}
	}

	private static void write(Object owner, String name, Object value) {
		try {
			Field field = owner.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(owner, value);
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError(failure);
		}
	}

	private static void check(boolean result, String message) {
		if (!result) throw new AssertionError(message);
	}
}
