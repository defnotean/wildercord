package dev.wildercord.aura;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Native collision-shape regressions for fast/fractional crescents and their ahead/lateral cutting reach. */
public final class CrescentCoverTest implements FabricClientGameTest {
	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer caster = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = caster.level();
				BlockPos base = new BlockPos(caster.getBlockX() + 20, 190, caster.getBlockZ());
				// Every scene advances the production move/cut methods on one server tick, avoiding mob AI and physics races.
				for (double speed : new double[] {1.25, 1.5}) {
					for (double offset : new double[] {.1, .5, .9}) {
						for (Vec3 direction : List.of(new Vec3(1, 0, 0), new Vec3(1, .65, .25))) {
							wallScene(level, caster, base, speed, offset, direction.normalize());
						}
					}
				}
				initialOffset(level, caster, base);
				aheadAndSideCover(level, caster, base);
				shapeScene(level, caster, base, Blocks.STONE_SLAB.defaultBlockState(), .25, true, "bottom slab lower half");
				shapeScene(level, caster, base, Blocks.STONE_SLAB.defaultBlockState(), .75, false, "clear space above a bottom slab");
				shapeScene(level, caster, base, Blocks.SHORT_GRASS.defaultBlockState(), .25, false, "passable foliage");
				shapeScene(level, caster, base, Blocks.WATER.defaultBlockState(), .25, false, "fluid with no collider");
			});
		}
	}

	private static Crescents.Flight flight(ServerPlayer caster, Vec3 origin, Vec3 aim, double speed, double width, int[] cuts) {
		return new Crescents.Flight(caster, origin, aim, 0x66CCFF, 1, 1, speed, 24, width, 6, false,
			entity -> entity instanceof Zombie, (f, target) -> { cuts[0]++; return 1; });
	}

	private static Zombie target(ServerLevel level, Vec3 center) {
		Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		check(target != null, "The native collision target is constructible");
		target.setNoAi(true);
		target.setNoGravity(true);
		target.snapTo(center.x, center.y - target.getBbHeight() / 2, center.z, 0, 0);
		level.addFreshEntity(target);
		return target;
	}

	private static void fly(Crescents.Flight flight) {
		while (!flight.done && flight.step < flight.steps) {
			Crescents.move(flight);
			if (!flight.done) Crescents.cutFrom(flight);
		}
	}

	private static void wallScene(ServerLevel level, ServerPlayer caster, BlockPos base, double speed, double offset, Vec3 aim) {
		Vec3 origin = Vec3.atLowerCornerOf(base).add(offset, .35, .5);
		int wallX = base.getX() + (speed == 1.25 ? 4 : 3);
		Vec3 middle = origin.add(aim.scale((wallX + .5 - origin.x) / aim.x));
		List<BlockPos> wall = new ArrayList<>();
		for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
			BlockPos pos = new BlockPos(wallX, (int) Math.floor(middle.y) + dy, (int) Math.floor(middle.z) + dz);
			wall.add(pos);
			level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		}
		Vec3 beyond = origin.add(aim.scale((wallX + 1.3 - origin.x) / aim.x));
		Zombie target = target(level, beyond);
		try {
			int[] cuts = {0};
			Crescents.Flight shot = flight(caster, origin, aim, speed, 2, cuts);
			fly(shot);
			String scene = "speed=" + speed + ", offset=" + offset + ", elevation=" + aim.y;
			check(shot.done && shot.blockedAt != null && shot.blockedAt.getX() == wallX,
				"A full collider stops the swept flight: " + scene);
			check(Math.abs(shot.front.x - wallX) < 1.0E-5, "The flight ends at the actual wall surface: " + scene);
			check(cuts[0] == 0 && shot.hit.isEmpty(), "Nobody behind the wall takes a cut or spends a target slot: " + scene);
		} finally {
			target.discard();
			for (BlockPos pos : wall) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
	}

	private static void initialOffset(ServerLevel level, ServerPlayer caster, BlockPos base) {
		BlockPos wall = base.offset(1, 0, 0);
		level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		try {
			Crescents.Flight shot = flight(caster, Vec3.atLowerCornerOf(base).add(.6, .75, .5), new Vec3(1, 0, 0), 1.5, 2, new int[] {0});
			check(shot.front.x > wall.getX(), "The fixture places its wall inside the initial .8-block offset");
			Crescents.move(shot);
			check(shot.done && wall.equals(shot.blockedAt) && Math.abs(shot.front.x - wall.getX()) < 1.0E-5,
				"The origin-to-initial-front segment cannot materialize a crescent beyond cover");
		} finally {
			level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
		}
	}

	private static void aheadAndSideCover(ServerLevel level, ServerPlayer caster, BlockPos base) {
		Vec3 origin = Vec3.atLowerCornerOf(base).add(.5, .75, .5);
		for (boolean lateral : new boolean[] {false, true}) {
			int[] cuts = {0};
			Crescents.Flight shot = flight(caster, origin, new Vec3(1, 0, 0), 1.5, 4, cuts);
			Crescents.move(shot);
			check(!shot.done, "The front reaches open air before late cover is placed");
			Vec3 targetCenter = shot.front.add(lateral ? new Vec3(0, 0, 1.6) : new Vec3(1.3, 0, 0));
			BlockPos wall = lateral ? base.offset(2, 0, 1) : base.offset(3, 0, 0);
			Zombie target = target(level, targetCenter);
			// Cover appearing after aim/flight commitment still protects the target from the edge's ahead/lateral reach.
			level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
			try {
				Crescents.cutFrom(shot);
				check(cuts[0] == 0 && shot.hit.isEmpty() && !shot.done,
					"A still-open front cannot cut around its " + (lateral ? "lateral" : "ahead") + " cover");
				level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
				Crescents.cutFrom(shot);
				check(cuts[0] == 1 && shot.hit.contains(target.getUUID()), "Removing that collider exposes the same in-range target");
			} finally {
				target.discard();
				level.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
			}
		}
	}

	private static void shapeScene(ServerLevel level, ServerPlayer caster, BlockPos base, BlockState state, double height, boolean blocks, String label) {
		BlockPos shape = base.offset(3, 0, 0);
		level.setBlockAndUpdate(shape.below(), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(shape, state);
		Vec3 origin = Vec3.atLowerCornerOf(base).add(.5, height, .5);
		Zombie target = target(level, origin.add(3.6, 0, 0));
		try {
			int[] cuts = {0};
			Crescents.Flight shot = flight(caster, origin, new Vec3(1, 0, 0), 1.5, 2, cuts);
			fly(shot);
			check((shot.blockedAt != null) == blocks, "Flight uses the actual collision shape for " + label);
			check(cuts[0] == (blocks ? 0 : 1), "Cut visibility preserves " + label);
		} finally {
			target.discard();
			level.setBlockAndUpdate(shape, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(shape.below(), Blocks.AIR.defaultBlockState());
		}
	}
}
