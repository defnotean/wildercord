package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Magic that changes the world, checked in a real world: frost on water makes frosted ice, fire by
 * grass lights fire (with fire spreading on), storm into water shocks a husk standing in that water,
 * wind knocks an arrow out of the air, a Grow beside a Rampart leaves the wall standing, and a spell where
 * the caster may not build changes nothing.
 *
 * <p>Spells are applied straight to a hit at a chosen point ({@link CastEngine#onHit}), the same call
 * every shape ends in, so each check is exact. A singleplayer world has no spawn protection (only a
 * dedicated server does), so protected ground is stood in for the two ways {@code Casters.mayEdit}
 * refuses: a claim (a block-break listener saying no, as claim mods do) and a caster who can't build
 * (Adventure mode). A monster's spell is checked too: it never changes blocks.</p>
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY} or {@code WILDERCORD_CORDS_ONLY} skip it.</p>
 */
public class WildercordWorldMagicTest implements FabricClientGameTest {
	/** The stand-in claim: while set, breaking (so changing) any block inside it is refused. */
	private static volatile AABB claim;
	private static boolean listening;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!listening) {
			listening = true;
			PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
				AABB box = claim;
				return box == null || !box.contains(Vec3.atCenterOf(pos));
			});
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			var server = world.getServer();
			List<String> failures = new ArrayList<>();

			server.runOnServer(s -> {
				ServerPlayer player = player(s);
				player.setGameMode(GameType.SURVIVAL);
				ServerLevel level = player.level();
				level.getGameRules().set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 128, s);
			});
			context.waitTicks(2);

			// Frost on water: its surface freezes into frosted ice.
			String frost = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos pool = site(player, 12, 0);
				pool(player.level(), pool, 1);
				apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				BlockState state = player.level().getBlockState(pool.below());
				return state.is(Blocks.FROSTED_ICE) ? null : "frost on water should freeze it into frosted ice (found " + state + ")";
			});
			note(failures, frost);

			// Fire by grass, with fire spreading on: the grass catches.
			String fire = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos patch = site(player, 0, 12);
				meadow(player.level(), patch);
				apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atBottomCenterOf(patch), List.of());
				int fires = count(player.level(), patch, 2, state -> state.is(BlockTags.FIRE));
				douse(player.level(), patch, 3);
				return fires > 0 ? null : "fire by grass should set it alight";
			});
			note(failures, fire);

			// Storm into water: a husk standing in the same water (not struck by the spell itself) is shocked.
			UUID husk = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, -12, 0);
				pool(level, pool, 2);
				Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				// On the pool's floor, two blocks down: it stands in the water.
				mob.snapTo(pool.getX() + 0.5, pool.getY() - 2, pool.getZ() + 0.5, 0.0F, 0.0F);
				mob.setNoAi(true);
				mob.setPersistenceRequired();
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				mob.addTag("wildercord.rolled");
				level.addFreshEntity(mob);
				return mob.getUUID();
			});
			// Let it tick, so it knows it's standing in water.
			context.waitTicks(5);
			String storm = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				if (!(level.getEntity(husk) instanceof LivingEntity target) || !target.isAlive()) {
					return "the husk in the pool is gone";
				}
				if (!target.isInWater()) {
					return "the husk should be standing in the pool's water";
				}
				float before = target.getHealth();
				BlockPos pool = site(player, -12, 0);
				// Into the water two blocks from the husk.
				apply(player, List.of(Runes.TOUCH, Runes.SHOCK), Vec3.atCenterOf(pool.offset(2, 0, 0)), List.of());
				float after = target.isAlive() ? target.getHealth() : 0.0F;
				target.discard();
				return after < before ? null : "storm into water should shock a husk standing in it (health " + before + " -> " + after + ")";
			});
			note(failures, storm);

			// Wind: an arrow in flight near where it lands is knocked away, the way the wind blows.
			String wind = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos spot = site(player, 0, -12);
				Vec3 at = Vec3.atBottomCenterOf(spot).add(0, 1.5, 0);
				Entity arrow = EntityTypes.ARROW.create(level, EntitySpawnReason.COMMAND);
				arrow.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
				arrow.setNoGravity(true);
				arrow.setDeltaMovement(-1.5, 0, 0);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				arrow.addTag("wildercord.rolled");
				level.addFreshEntity(arrow);
				// The gust comes from 3 blocks to the west, so it blows east (+x), against the arrow.
				apply(player, List.of(Runes.TOUCH, Runes.PUSH), at, List.of(), at.add(-3, 0, 0));
				double x = arrow.getDeltaMovement().x;
				arrow.discard();
				return x > 0.3 ? null : "wind should knock an arrow flying at it back the other way (its speed along x is " + x + ")";
			});
			note(failures, wind);

			// A Grow beside a Rampart leaves the wall standing: a spell only asking whether it may change a block
			// (offered to claims as a break) must never set off the handler that takes a Rampart down.
			BlockPos wallSite = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				BlockPos site = site(player, 0, 24);
				meadow(player.level(), site);
				apply(player, List.of(Runes.TOUCH, Runes.RAMPART), Vec3.atBottomCenterOf(site), List.of());
				return site;
			});
			context.waitTicks(8);
			String rampart = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				int before = count(level, wallSite, 3, state -> state.is(Blocks.PACKED_MUD));
				apply(player, List.of(Runes.TOUCH, Runes.GROW), Vec3.atBottomCenterOf(wallSite), List.of());
				int after = count(level, wallSite, 3, state -> state.is(Blocks.PACKED_MUD));
				if (before == 0) {
					return "a Rampart should raise a wall of packed mud";
				}
				return after == before ? null : "a Grow beside a Rampart shouldn't take the wall down (" + before + " blocks, then " + after + ")";
			});
			note(failures, rampart);

			// Protected ground: frost in a claim freezes nothing, fire from a caster who can't build lights nothing,
			// and a monster's frost never changes blocks.
			String protectedGround = server.computeOnServer(s -> {
				ServerPlayer player = player(s);
				ServerLevel level = player.level();
				BlockPos pool = site(player, 12, 12);
				pool(level, pool, 1);
				claim = new AABB(pool).inflate(4);
				try {
					apply(player, List.of(Runes.TOUCH, Runes.FROST), Vec3.atCenterOf(pool), List.of());
				} finally {
					claim = null;
				}
				if (count(level, pool, 3, state -> state.is(Blocks.FROSTED_ICE)) > 0) {
					return "frost inside a claim shouldn't freeze the water";
				}
				BlockPos patch = site(player, -12, 12);
				meadow(level, patch);
				player.setGameMode(GameType.ADVENTURE);
				try {
					apply(player, List.of(Runes.TOUCH, Runes.FIRE), Vec3.atBottomCenterOf(patch), List.of());
				} finally {
					player.setGameMode(GameType.SURVIVAL);
				}
				int fires = count(level, patch, 2, state -> state.is(BlockTags.FIRE));
				douse(level, patch, 3);
				if (fires > 0) {
					return "fire from a caster who can't build there shouldn't light anything";
				}
				Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
				mob.snapTo(pool.getX() - 3.5, pool.getY() + 1, pool.getZ() + 0.5, 0.0F, 0.0F);
				mob.setNoAi(true);
				// Never a random Runebound (it would have more health and cast back): tests pick their monsters.
				mob.addTag("wildercord.rolled");
				level.addFreshEntity(mob);
				Cast monster = new Cast(mob);
				SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.TOUCH, Runes.FROST)).root().groups.getFirst();
				Vec3 at = Vec3.atCenterOf(pool);
				CastEngine.onHit(monster, group, new Cast.Hit(List.of(), at, new Vec3(1, 0, 0), mob.position(), null, null, false), null);
				mob.discard();
				if (count(level, pool, 3, state -> state.is(Blocks.FROSTED_ICE)) > 0) {
					return "a monster's frost shouldn't freeze the water";
				}
				return null;
			});
			note(failures, protectedGround);

			if (!failures.isEmpty()) {
				throw new AssertionError("Magic that changes the world went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			claim = null;
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void note(List<String> failures, String failure) {
		if (failure != null) {
			failures.add(failure);
		}
	}

	/** A spot on the ground {@code dx}, {@code dz} blocks from the player: the air block just above the surface. */
	private static BlockPos site(ServerPlayer player, int dx, int dz) {
		ServerLevel level = player.level();
		BlockPos at = player.blockPosition().offset(dx, 0, dz);
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
		return new BlockPos(at.getX(), top, at.getZ());
	}

	/** Digs a 5x5 pool {@code depth} deep, walled in stone, whose middle surface block is {@code centre.below()} (open air above). */
	private static void pool(ServerLevel level, BlockPos centre, int depth) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					level.setBlockAndUpdate(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
				for (int dy = 1; dy <= depth + 1; dy++) {
					boolean rim = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == depth + 1;
					level.setBlockAndUpdate(centre.offset(dx, -dy, dz), rim ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
				}
			}
		}
	}

	/** A 5x5 patch of grass blocks with short grass growing on them, around {@code centre} (the air above the surface). */
	private static void meadow(ServerLevel level, BlockPos centre) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), Blocks.SHORT_GRASS.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** How many blocks within {@code r} of {@code centre} (in a cube) pass {@code test}. */
	private static int count(ServerLevel level, BlockPos centre, int r, java.util.function.Predicate<BlockState> test) {
		int n = 0;
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (test.test(level.getBlockState(pos))) {
				n++;
			}
		}
		return n;
	}

	/** Puts out any fire around {@code centre}, so it doesn't spread through the rest of the test. */
	private static void douse(ServerLevel level, BlockPos centre, int r) {
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
			if (level.getBlockState(pos).is(BlockTags.FIRE)) {
				level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			}
		}
	}

	private static void apply(ServerPlayer player, List<RuneDef> runes, Vec3 at, List<Entity> struck) {
		apply(player, runes, at, struck, player.position());
	}

	/** Lands a spell's first group on {@code at}, as the player's, with {@code origin} where it came from. */
	private static void apply(ServerPlayer player, List<RuneDef> runes, Vec3 at, List<Entity> struck, Vec3 origin) {
		SpellPlan.Group group = SpellCompiler.compile(runes).root().groups.getFirst();
		Vec3 dir = at.subtract(origin).lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : at.subtract(origin).normalize();
		CastEngine.onHit(new Cast(player), group, new Cast.Hit(struck, at, dir, origin, null, null, false), null);
	}
}
