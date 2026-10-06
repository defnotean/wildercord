package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Controller checks supplement the original, unsteered 50-tick Wildlife flee acceptance. */
public final class RimehareHopChecks {
	private RimehareHopChecks() {}

	public static void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(25);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule random_tick_speed 0");
			world.getServer().runOnServer(server -> {
				arena(server.overworld());
				var viewer = server.getPlayerList().getPlayers().getFirst();
				viewer.setGameMode(GameType.SPECTATOR);
				viewer.teleportTo(server.overworld(), 0, 106, 0, Set.<Relative>of(), 0, 30, false);
			});
			for (float yaw : new float[] {45, 315, 180}) straight(context, world, yaw);
			terrain(context, world);
			stopAndNoAi(context, world);
			water(context, world);
		}
	}

	private static void straight(ClientGameTestContext context, TestSingleplayerContext world, float yaw) {
		ObservedHare hare = spawn(context, world, yaw);
		world.getServer().runOnServer(server -> route(hare, new Vec3(.5, 100, -12.5)));
		for (int tick = 0; tick < 100; tick++) {
			context.waitTicks(1);
			boolean done = world.getServer().computeOnServer(server -> {
				check(Math.abs(Mth.wrapDegrees(hare.getYRot() - hare.previousYaw)) <= 90.01,
					"Ground turns retain the native 90-degree bound: " + hare.receipt());
				hare.previousYaw = hare.getYRot();
				return hare.getNavigation().isDone();
			});
			if (done) break;
		}
		int bounds = world.getServer().computeOnServer(server -> {
			check(!hare.hops.isEmpty(), "The real controller must bound with initial yaw " + yaw);
			Hop first = hare.hops.getFirst();
			check(Math.abs(Mth.wrapDegrees(first.yaw() - 180)) < 1,
				"First takeoff must face the north path after either turn: " + first);
			check(Math.abs(first.position().x - .5) < .05,
				"An unfinished ground turn must not accelerate sideways: " + first);
			check(hare.hops.size() >= 2 && hare.landingRecoveries > 0,
				"A real flight and grounded recovery must both occur: " + hare.receipt());
			check(hare.shortestRecovery >= 3, "A landing must get at least three grounded navigation ticks: " + hare.receipt());
			check(hare.recoveryTicks >= 3 && hare.brakedRecoveryTicks > 0 && hare.maxRecoveryStep <= 1.000001,
				"Fast recovery must brake through native friction before it skips a waypoint cell: " + hare.receipt());
			check(hare.getNavigation().isDone(), "Native waypoints must finish their finite route: " + hare.receipt());
			check(hare.position().distanceTo(new Vec3(.5, 100, -12.5)) < 1.5,
				"Completing the finite route must not launch away from its destination: " + hare.receipt());
			return hare.hops.size();
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			check(hare.hops.size() == bounds, "An expired path must not rebound from residual horizontal velocity");
			check(hare.onGround() && hare.position().distanceTo(new Vec3(.5, 100, -12.5)) < 1.5,
				"The expired route must settle by its destination: " + hare.receipt());
			hare.discard();
		});
	}

	private static void terrain(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			// A full-height wall plus damaging floor makes the direct line unusable; native A* chooses the detour.
			for (int x = -2; x <= 2; x++) for (int y = 100; y <= 102; y++)
				level.setBlock(new BlockPos(x, y, -4), Blocks.STONE_BRICKS.defaultBlockState(), 2);
			for (int x = -4; x <= -3; x++) for (int z = -5; z <= -3; z++)
				level.setBlock(new BlockPos(x, 99, z), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
			// The destination itself is half-height terrain, so completion must actually reach its surface.
			for (int x = -18; x <= 18; x++) for (int z = -18; z <= -9; z++)
				level.setBlock(new BlockPos(x, 100, z), Blocks.STONE_SLAB.defaultBlockState(), 2);
		});
		ObservedHare hare = spawn(context, world, 0);
		world.getServer().runOnServer(server -> {
			route(hare, new Vec3(.5, 100.5, -12.5));
			Path path = hare.getNavigation().getPath();
			check(hare.getNavigation().getClass() == GroundPathNavigation.class,
				"Rimehare must retain unmodified vanilla ground navigation");
			for (int i = 0; i < path.getNodeCount(); i++) {
				var node = path.getNode(i);
				check(node.type != PathType.FIRE && node.type != PathType.FIRE_IN_NEIGHBOR && node.type != PathType.BLOCKED,
					"Native route retains hazard/collision filters: " + node + "/" + node.type);
			}
		});
		for (int tick = 0; tick < 200; tick++) {
			context.waitTicks(1);
			boolean done = world.getServer().computeOnServer(server -> {
				check(hare.getHealth() == hare.getMaxHealth(), "The native detour must avoid the damaging floor: " + hare.receipt());
				check(hare.level().noCollision(hare), "Bounds must respect the wall and terrain collision: " + hare.receipt());
				return hare.getNavigation().isDone();
			});
			if (done) break;
		}
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			check(hare.getNavigation().isDone() && hare.position().distanceTo(new Vec3(.5, 100.5, -12.5)) < 1.5,
				"Native path must finish around the wall, hazard and half-height footing: " + hare.receipt());
			check(hare.onGround() && Math.abs(hare.getY() - 100.5) < .001
				&& server.overworld().getBlockState(hare.blockPosition()).is(Blocks.STONE_SLAB),
				"The completed route must actually settle on the half-height destination: " + hare.receipt());
			hare.discard();
			arena(server.overworld());
		});
	}

	private static void stopAndNoAi(ClientGameTestContext context, TestSingleplayerContext world) {
		ObservedHare hare = spawn(context, world, 180);
		world.getServer().runOnServer(server -> route(hare, new Vec3(.5, 100, -12.5)));
		context.waitTicks(3);
		int beforeStop = world.getServer().computeOnServer(server -> {
			check(!hare.hops.isEmpty(), "Stop regression begins with an actual bound");
			hare.getNavigation().stop();
			return hare.hops.size();
		});
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			check(hare.hops.size() == beforeStop && hare.onGround(), "Stopping intent must end bounds after the current flight");
			hare.setNoAi(true);
			route(hare, new Vec3(.5, 100, -12.5));
			hare.setDeltaMovement(.12, 0, 0);
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			check(hare.hops.size() == beforeStop && hare.onGround(), "NoAI must not bound despite a path and residual movement");
			hare.discard();
		});
		ObservedHare recovering = spawn(context, world, 180);
		world.getServer().runOnServer(server -> route(recovering, new Vec3(.5, 100, -12.5)));
		int stopBounds = -1;
		for (int tick = 0; tick < 50; tick++) {
			context.waitTicks(1);
			stopBounds = world.getServer().computeOnServer(server -> {
				if (recovering.lastBrakedRecoveryTick != recovering.tickCount || !recovering.onGround()) return -1;
				// Stop in the callback that observes braking; retain every subsequent server tick's input.
				recovering.getNavigation().stop();
				recovering.observeStoppedInput = true;
				recovering.stoppedSpeed = recovering.getDeltaMovement().horizontalDistanceSqr();
				return recovering.hops.size();
			});
			if (stopBounds >= 0) break;
		}
		check(stopBounds >= 0, "Recovery stop must observe an actual native braking tick");
		int beforeRecoveryStop = stopBounds;
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			check(recovering.hops.size() == beforeRecoveryStop && recovering.onGround()
				&& recovering.observedStoppedTicks > 0 && recovering.acceleratedStoppedTicks == 0
				&& recovering.zza == 0 && recovering.getDeltaMovement().horizontalDistanceSqr() < .001,
				"Stopping during recovery must consume the old move command: " + recovering.receipt());
			recovering.discard();
		});
		ObservedHare idle = spawn(context, world, 0);
		world.getServer().runOnServer(server -> idle.setDeltaMovement(.12, 0, 0));
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			check(idle.hops.isEmpty(), "Residual movement without a path must not invent bounding intent");
			idle.discard();
		});
	}

	private static void water(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = 100; y <= 102; y++)
				level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 2);
		});
		ObservedHare hare = world.getServer().computeOnServer(server -> {
			ObservedHare swimmer = new ObservedHare(server.overworld());
			swimmer.snapTo(.5, 100, .5, 0, 0);
			server.overworld().addFreshEntity(swimmer);
			return swimmer;
		});
		context.waitTicks(20);
		world.getServer().runOnServer(server -> {
			check(hare.isInWater() && hare.getY() > 100.5, "Native FloatGoal must still lift the hare in deep water: " + hare.receipt());
			check(hare.hops.isEmpty(), "Deep water must use native liquid jumps, never additional land bounds");
			hare.discard();
		});
	}

	private static ObservedHare spawn(ClientGameTestContext context, TestSingleplayerContext world, float yaw) {
		ObservedHare hare = world.getServer().computeOnServer(server -> {
			ObservedHare created = new ObservedHare(server.overworld());
			created.snapTo(.5, 100, .5, yaw, 0);
			created.previousYaw = yaw;
			server.overworld().addFreshEntity(created);
			return created;
		});
		context.waitTicks(3);
		world.getServer().runOnServer(server -> check(hare.onGround() && hare.hops.isEmpty(), "Idle test animal must settle naturally before its route"));
		return hare;
	}

	private static void route(ObservedHare hare, Vec3 destination) {
		check(hare.getNavigation().moveTo(destination.x, destination.y, destination.z, 2.6)
			&& hare.getNavigation().getPath().canReach(), "Native pathfinder must admit the fixture route: " + hare.receipt());
		// The native overload allows one block of reach; keep both that actual endpoint and the requested
		// destination in the receipt so completion and post-arrival drift cannot be confused.
		check(hare.getNavigation().getPath().getEndNode().asBlockPos().distManhattan(hare.getNavigation().getPath().getTarget()) <= 1,
			"Native destination admission must retain the one-block reach range: " + hare.receipt());
	}

	private static void arena(ServerLevel level) {
		for (int x = -18; x <= 18; x++) for (int z = -18; z <= 18; z++) {
			level.setBlock(new BlockPos(x, 99, z), Blocks.SNOW_BLOCK.defaultBlockState(), 2);
			for (int y = 100; y <= 104; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private record Hop(int tick, float yaw, Vec3 position) {}

	/** Supplied routes isolate the actual Rimehare controls; only autonomous destination selection is disabled. */
	private static final class ObservedHare extends Rimehare {
		private final List<Hop> hops = new ArrayList<>();
		private int landed = -1, shortestRecovery = Integer.MAX_VALUE, landingRecoveries;
		private int recoveryTicks, brakedRecoveryTicks, lastBrakedRecoveryTick = -1;
		private double maxRecoveryStep, stoppedSpeed;
		private boolean observeStoppedInput;
		private int observedStoppedTicks, acceleratedStoppedTicks;
		private boolean airborne;
		private float previousYaw;

		private ObservedHare(ServerLevel level) {
			super(Wildlife.RIMEHARE, level);
		}

		@Override
		protected void registerGoals() {
			goalSelector.addGoal(1, new FloatGoal(this));
		}

		@Override
		public void aiStep() {
			boolean wasGrounded = onGround();
			Vec3 positionBefore = position();
			double speedBefore = getDeltaMovement().horizontalDistanceSqr();
			int landingBefore = landed;
			super.aiStep();
			if (wasGrounded && onGround() && landingBefore >= 0 && tickCount - landingBefore <= 3) {
				recoveryTicks++;
				maxRecoveryStep = Math.max(maxRecoveryStep, position().subtract(positionBefore).horizontalDistance());
				if (zza == 0 && xxa == 0 && getDeltaMovement().horizontalDistanceSqr() < speedBefore) {
					brakedRecoveryTicks++;
					lastBrakedRecoveryTick = tickCount;
				}
			}
			if (observeStoppedInput) {
				observedStoppedTicks++;
				double speed = getDeltaMovement().horizontalDistanceSqr();
				if (zza != 0 || xxa != 0 || speed > stoppedSpeed + 1e-8) acceleratedStoppedTicks++;
				stoppedSpeed = speed;
			}
			if (!onGround()) airborne = true;
			if (onGround() && airborne && getDeltaMovement().y <= 0) {
				landed = tickCount;
				airborne = false;
			}
		}

		@Override
		public void jumpFromGround() {
			if (landed >= 0) {
				shortestRecovery = Math.min(shortestRecovery, tickCount - landed);
				landingRecoveries++;
				landed = -1;
			}
			hops.add(new Hop(tickCount, getYRot(), position()));
			super.jumpFromGround();
		}

		private String receipt() {
			Path path = getNavigation().getPath();
			return "tick=" + tickCount + ", position=" + position() + ", velocity=" + getDeltaMovement() + ", hops=" + hops
				+ ", recovery=" + shortestRecovery + ", recoveryTicks=" + recoveryTicks
				+ ", brakedRecoveryTicks=" + brakedRecoveryTicks + ", maxRecoveryStep=" + maxRecoveryStep
				+ ", stoppedTicks=" + observedStoppedTicks + ", acceleratedStoppedTicks=" + acceleratedStoppedTicks + ", path=" + path
				+ (path == null ? "" : ", node=" + path.getNextNodeIndex() + "/" + path.getNodeCount()
					+ ", done=" + path.isDone() + ", target=" + path.getTarget() + ", end=" + path.getEndNode());
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
