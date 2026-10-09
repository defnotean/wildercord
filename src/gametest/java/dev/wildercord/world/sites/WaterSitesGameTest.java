package dev.wildercord.world.sites;

import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.sites.water.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntBinaryOperator;

/**
 * Builds every water site on terrain shaped for it (a lake, a bank, a beach, the sea floor), in chunk-sized calls as
 * natural generation does and in a different facing each, then checks what matters: chests with their loot, books on
 * lecterns, entrances open, water where it belongs and none where it doesn't (also after the water has had time to
 * flow), the conduit lit, boats and villagers placed once. Then a player walks into three of them. Last, each site is
 * placed by its own locator with /place on real terrain, and the results are logged.
 */
public final class WaterSitesGameTest implements FabricClientGameTest {
	private static final int SEA_TOP = 62;

	private interface Factory {
		DungeonPiece make(int x, int y, int z, Direction facing);
	}

	public void runTest(ClientGameTestContext c) {
		// A normal world, not the default superflat one: the mill sets its wheel by the generator's sea level.
		try (var world = c.worldBuilder().setUseConsistentSettings(false).create()) {
			c.waitTicks(40);
			world.getServer().runOnServer(s -> check(s.overworld().getChunkSource().getGenerator().getSeaLevel() == SEA_TOP + 1,
				"the test world's sea is at " + (SEA_TOP + 1)));
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set 6000");
			world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
			Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
			DungeonPiece[] built = new DungeonPiece[8];
			// Smokehouse: a lake 4 deep.
			built[0] = build(world, 0, StiltSmokehousePiece::new, facings[0], SEA_TOP - StiltSmokehousePiece.BASE, 15, 17, (x, z) -> 58);
			// Lighthouse: level ground a little over the sea.
			built[1] = build(world, 1, LighthousePiece::new, facings[1], 64 - LighthousePiece.BASE, 13, 13, (x, z) -> 64);
			// Watermill: a bank, the river behind it from z 10.
			built[2] = build(world, 2, WatermillPiece::new, facings[2], 64 - WatermillPiece.BASE, 15, 15, (x, z) -> z <= 9 ? 64 : 58);
			// Ice camp: a lake 6 deep.
			built[3] = build(world, 3, IceCampPiece::new, facings[3], SEA_TOP - IceCampPiece.BASE, 17, 17, (x, z) -> 56);
			// Pier: a beach at z 3 and under, then the sea 7 deep.
			built[4] = build(world, 4, PierBoathousePiece::new, facings[1], SEA_TOP - PierBoathousePiece.BASE, 11, 28, (x, z) -> z <= 3 ? 62 : 55);
			// Shrine: the sea floor 22 down.
			built[5] = build(world, 5, SunkenShrinePiece::new, facings[2], 40 - SunkenShrinePiece.FLOOR, 17, 17, (x, z) -> 40);
			// Net-weavers: a swamp 2 deep.
			built[6] = build(world, 6, NetweaverVillagePiece::new, facings[3], SEA_TOP - NetweaverVillagePiece.BASE, 27, 27, (x, z) -> 60);
			// Grotto: dry rock 4 over the sea.
			built[7] = build(world, 7, TidepoolGrottoPiece::new, facings[0], 66 - TidepoolGrottoPiece.BASE, 19, 19, (x, z) -> 66);

			c.waitTicks(20);
			world.getServer().runOnServer(s -> checkBuilt(s.overworld(), built, false));
			// Give the water time to go wherever it was going to go, then look again.
			visit(c, world, built[5], 8, 4, 7);
			c.waitTicks(100);
			world.getServer().runOnServer(s -> {
				checkBuilt(s.overworld(), built, true);
				var conduit = s.overworld().getBlockEntity(built[5].localPosition(8, 5, 4));
				check(conduit instanceof ConduitBlockEntity cb && cb.isActive(), "the shrine's conduit is lit by its ring");
			});
			shot(c, world, built[5], new Vec3(8, 6, -6), new Vec3(8, 5, 6), "water_sunken_shrine_court");

			// A player walks into the lighthouse, along the pier into the boathouse, and into the grotto.
			walk(c, world, built[1], 6, 7, -1, 6, 25);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				var mid = built[1].localPosition(6, 7, 6);
				check(Math.hypot(p.getX() - mid.getX() - .5, p.getZ() - mid.getZ() - .5) < 2.6, "a player walks in through the lighthouse door; at " + p.position());
			});
			walk(c, world, built[4], 5, 11, -3, 0, 125);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				check(inside(built[4], p, 20, 27), "a player walks up the steps and down the pier into the boathouse; at " + p.position());
			});
			walk(c, world, built[7], 9, 6, -2, 9, 40);
			world.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				var mid = built[7].localPosition(9, 6, 9);
				check(Math.hypot(p.getX() - mid.getX() - .5, p.getZ() - mid.getZ() - .5) < 6.5, "a player walks into the grotto; at " + p.position());
			});

			shot(c, world, built[1], new Vec3(-18, 30, -18), new Vec3(6, 20, 6), "water_lighthouse");
			shot(c, world, built[4], new Vec3(-14, 22, -6), new Vec3(5, 12, 18), "water_pier_boathouse");
			shot(c, world, built[2], new Vec3(-14, 18, -10), new Vec3(7, 10, 8), "water_watermill");
			shot(c, world, built[6], new Vec3(-10, 24, -10), new Vec3(13, 9, 13), "water_netweaver_village");
		}
		natural(c);
	}

	/** Shapes the terrain round a site (stone to {@code ground}, water up to the sea, air over it), then builds it chunk by chunk. */
	private static DungeonPiece build(TestSingleplayerContext world, int index, Factory factory, Direction facing, int y, int width, int depth,
			IntBinaryOperator ground) {
		return world.getServer().computeOnServer(s -> {
			ServerLevel l = s.overworld();
			DungeonPiece piece = factory.make(4000 + index * 96, y, 4000, facing);
			BoundingBox b = piece.getBoundingBox();
			// Kept loaded (and its entities ticking) for the rest of the test.
			s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
				"forceload add " + (b.minX() - 16) + " " + (b.minZ() - 16) + " " + (b.maxX() + 16) + " " + (b.maxZ() + 16));
			for (int cx = (b.minX() >> 4) - 1; cx <= (b.maxX() >> 4) + 1; cx++) {
				for (int cz = (b.minZ() >> 4) - 1; cz <= (b.maxZ() >> 4) + 1; cz++) {
					l.getChunk(cx, cz);
				}
			}
			// The world's own creatures nearby (the drowned of an ocean ruin, say) go, so only the site's are counted.
			var near = new AABB(b.minX(), l.getMinY(), b.minZ(), b.maxX() + 1, l.getMaxY(), b.maxZ() + 1).inflate(16, 0, 16);
			l.getEntities((Entity) null, near, e -> !(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
			for (int x = -6; x < width + 6; x++) {
				for (int z = -6; z < depth + 6; z++) {
					BlockPos column = piece.localPosition(x, 0, z);
					int top = ground.applyAsInt(x, z);
					for (int wy = 30; wy <= 120; wy++) {
						BlockState state = wy <= top ? Blocks.STONE.defaultBlockState() : wy <= SEA_TOP ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
						l.setBlock(new BlockPos(column.getX(), wy, column.getZ()), state, Block.UPDATE_CLIENTS);
					}
				}
			}
			for (int cx = b.minX() >> 4; cx <= b.maxX() >> 4; cx++) {
				for (int cz = b.minZ() >> 4; cz <= b.maxZ() >> 4; cz++) {
					var clip = new BoundingBox(cx * 16, b.minY(), cz * 16, cx * 16 + 15, b.maxY(), cz * 16 + 15);
					piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(7), clip, new ChunkPos(cx, cz), BlockPos.ZERO);
				}
			}
			check(b.maxY() < l.getMaxY(), "site " + index + " fits under the world's top");
			return piece;
		});
	}

	private static void checkBuilt(ServerLevel l, DungeonPiece[] p, boolean settled) {
		String when = settled ? " (after the water settled)" : "";
		// Smokehouse.
		chest(l, p[0], 10, 10, 5, "water_stilt_smokehouse");
		is(l, p[0], Blocks.SPRUCE_PLANKS, 7, 9, 7, "smokehouse deck");
		air(l, p[0], 7, 10, 4, "smokehouse door");
		air(l, p[0], 7, 11, 4, "smokehouse door");
		dry(l, p[0], 7, 10, 7, "smokehouse floor" + when);
		wet(l, p[0], 0, 8, 8, "the lake beside the smokehouse" + when);
		wet(l, p[0], 7, 8, 3, "the lake under the smokehouse deck" + when);
		check(l.getBlockState(p[0].localPosition(7, 8, 0)).getValue(BlockStateProperties.WATERLOGGED), "smokehouse step stands in the water");
		is(l, p[0], Blocks.CAMPFIRE, 7, 18, 13, "smokehouse signal fire");
		// Lighthouse.
		chest(l, p[1], 8, 3, 6, "water_lighthouse");
		book(l, p[1], 8, 7, 5, "lighthouse keeper's log");
		is(l, p[1], Blocks.REDSTONE_LAMP, 6, 29, 6, "lighthouse lamp");
		is(l, p[1], Blocks.LEVER, 6, 29, 7, "lighthouse lever");
		is(l, p[1], Blocks.LIGHTNING_ROD.waxed().unaffected(), 6, 36, 6, "lighthouse rod");
		is(l, p[1], Blocks.LADDER, 6, 7, 8, "lighthouse ladder foot");
		is(l, p[1], Blocks.LADDER, 6, 28, 8, "lighthouse ladder top");
		is(l, p[1], Blocks.CRACKED_STONE_BRICKS, 4, 6, 6, "lighthouse cellar stone");
		is(l, p[1], Blocks.LADDER, 4, 3, 6, "lighthouse cellar ladder");
		air(l, p[1], 6, 7, 3, "lighthouse door");
		air(l, p[1], 6, 8, 3, "lighthouse door");
		// Watermill.
		chest(l, p[2], 11, 9, 5, "water_watermill");
		is(l, p[2], Blocks.GRINDSTONE, 4, 9, 3, "mill grindstone");
		air(l, p[2], 7, 9, 1, "mill door");
		air(l, p[2], 7, 10, 1, "mill door");
		is(l, p[2], Blocks.STRIPPED_SPRUCE_LOG, 7, 12, 11, "mill wheel rim");
		wet(l, p[2], 7, 5, 12, "the river behind the mill" + when);
		counted(l, p[2], EntityTypes.VILLAGER, 1, "one miller at the mill");
		// Ice camp.
		chest(l, p[3], 8, 7, 14, "water_ice_camp");
		is(l, p[3], Blocks.PACKED_ICE, 8, 6, 10, "ice camp floor");
		is(l, p[3], Blocks.SOUL_CAMPFIRE, 8, 7, 8, "ice camp fire");
		wet(l, p[3], 3, 6, 4, "west shanty's fishing hole" + when);
		wet(l, p[3], 13, 6, 4, "east shanty's fishing hole" + when);
		dry(l, p[3], 3, 7, 4, "west shanty floor" + when);
		// Pier and boathouse.
		chest(l, p[4], 9, 13, 26, "water_pier_boathouse");
		is(l, p[4], Blocks.SPRUCE_STAIRS, 5, 11, 0, "pier's first step");
		is(l, p[4], Blocks.SPRUCE_PLANKS, 5, 12, 10, "pier deck");
		air(l, p[4], 5, 13, 20, "boathouse door");
		air(l, p[4], 5, 14, 20, "boathouse door");
		wet(l, p[4], 5, 10, 24, "boathouse slip" + when);
		dry(l, p[4], 5, 11, 24, "air over the slip" + when);
		wet(l, p[4], 5, 10, 27, "the slip's mouth to the sea" + when);
		counted(l, p[4], EntityTypes.SPRUCE_BOAT, 1, "one boat in the slip");
		// Sunken shrine.
		chest(l, p[5], 12, 6, 14, "water_sunken_shrine");
		book(l, p[5], 4, 6, 14, "shrine's Tide tablet");
		is(l, p[5], Blocks.CONDUIT, 8, 5, 4, "shrine conduit");
		wet(l, p[5], 8, 5, 4, "the conduit stands in water");
		wet(l, p[5], 8, 3, 8, "the tunnel under the hall wall" + when);
		wet(l, p[5], 8, 5, 10, "the pool in the hall floor" + when);
		dry(l, p[5], 8, 6, 10, "air over the pool" + when);
		dry(l, p[5], 8, 6, 12, "the hall" + when);
		dry(l, p[5], 5, 9, 10, "the hall's ceiling corner" + when);
		counted(l, p[5], EntityTypes.DROWNED, 2, "two drowned keep the shrine court");
		// Net-weavers.
		chest(l, p[6], 6, 10, 7, "water_netweaver_village");
		is(l, p[6], Blocks.LOOM, 5, 10, 5, "weavers' loom");
		check(l.getBlockState(p[6].localPosition(20, 10, 7)).getBlock() instanceof BedBlock
			&& l.getBlockState(p[6].localPosition(21, 10, 7)).getBlock() instanceof BedBlock, "a whole bed in the second hut");
		check(l.getBlockState(p[6].localPosition(11, 10, 20)).getBlock() instanceof BedBlock
			&& l.getBlockState(p[6].localPosition(11, 10, 21)).getBlock() instanceof BedBlock, "a whole bed in the third hut");
		for (int[] door : new int[][] {{8, 6}, {18, 8}, {12, 18}}) {
			air(l, p[6], door[0], 10, door[1], "hut door");
			air(l, p[6], door[0], 11, door[1], "hut door");
		}
		is(l, p[6], Blocks.MANGROVE_PLANKS, 12, 9, 3, "walk in");
		is(l, p[6], Blocks.MANGROVE_PLANKS, 15, 9, 9, "walk to the second hut");
		is(l, p[6], Blocks.MANGROVE_PLANKS, 12, 9, 16, "walk to the third hut");
		wet(l, p[6], 13, 6, 3, "the swamp under the walk" + when);
		counted(l, p[6], EntityTypes.VILLAGER, 2, "two weavers at home");
		// Grotto.
		chest(l, p[7], 14, 8, 12, "water_tidepool_grotto");
		book(l, p[7], 5, 6, 13, "hermit's notes");
		wet(l, p[7], 9, 5, 12, "deep pool" + when);
		wet(l, p[7], 9, 2, 12, "deep pool's bottom" + when);
		wet(l, p[7], 5, 5, 7, "shallow pool" + when);
		dry(l, p[7], 9, 6, 12, "air over the deep pool" + when);
		dry(l, p[7], 5, 6, 7, "air over the shallow pool" + when);
		air(l, p[7], 9, 8, 9, "the dome's middle");
		air(l, p[7], 9, 14, 9, "the hole in its top");
		air(l, p[7], 9, 6, 1, "grotto entrance");
		air(l, p[7], 9, 7, 1, "grotto entrance");
		is(l, p[7], Blocks.STONE, 12, 6, 11, "the step up to the ledge");
	}

	/** Each site placed by its own locator on real terrain (logged: whether a spot nearby suits it is up to the seed). */
	private static void natural(ClientGameTestContext c) {
		try (var world = c.worldBuilder().setUseConsistentSettings(false).create()) {
			c.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			String[][] sites = {
				{"water_stilt_smokehouse", "minecraft:swamp"}, {"water_lighthouse", "minecraft:beach"}, {"water_watermill", "minecraft:river"},
				{"water_ice_camp", "minecraft:frozen_ocean"}, {"water_pier_boathouse", "minecraft:beach"},
				{"water_sunken_shrine", "minecraft:lukewarm_ocean"}, {"water_netweaver_village", "minecraft:mangrove_swamp"},
				{"water_tidepool_grotto", "minecraft:stony_shore"}};
			List<String> report = new ArrayList<>();
			for (String[] site : sites) {
				String found = world.getServer().computeOnServer(s -> {
					ServerLevel l = s.overworld();
					var source = l.getChunkSource();
					var biome = source.getGenerator().getBiomeSource().findBiomeHorizontal(0, 64, 0, 6400, 32,
						b -> b.is(ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.Identifier.parse(site[1]))),
						RandomSource.create(41), true, source.randomState());
					if (biome == null) {
						return "no " + site[1] + " within 6400";
					}
					BlockPos at = biome.getFirst();
					for (int i = 0; i < 64; i++) {
						int x = at.getX() + (i % 8 - 4) * 12, z = at.getZ() + (i / 8 - 4) * 12;
						for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 2; cx++) {
							for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 2; cz++) {
								l.getChunk(cx, cz);
							}
						}
						s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
							"place structure wildercord:" + site[0] + " " + x + " 64 " + z);
						for (int cx = (x >> 4) - 2; cx <= (x >> 4) + 2; cx++) {
							for (int cz = (z >> 4) - 2; cz <= (z >> 4) + 2; cz++) {
								for (var be : l.getChunk(cx, cz).getBlockEntities().values()) {
									if (be instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null
										&& chest.getLootTable().identifier().getPath().equals("chests/" + site[0])) {
										return "placed at " + be.getBlockPos().toShortString() + " (try " + (i + 1) + ")";
									}
								}
							}
						}
					}
					return "no suitable spot in 64 tries near " + at.toShortString();
				});
				report.add(site[0] + ": " + found);
			}
			report.forEach(line -> System.out.println("[water sites] " + line));
		}
	}

	private static boolean inside(DungeonPiece piece, Entity e, int z0, int z1) {
		for (int x = 0; x <= 10; x++) {
			for (int z = z0; z <= z1; z++) {
				BlockPos at = piece.localPosition(x, 0, z);
				if (at.getX() == e.getBlockX() && at.getZ() == e.getBlockZ()) {
					return true;
				}
			}
		}
		return false;
	}

	private static void counted(ServerLevel l, DungeonPiece piece, net.minecraft.world.entity.EntityType<?> type, int expected, String label) {
		int found = count(l, piece, type);
		if (found != expected) {
			var b = piece.getBoundingBox();
			List<String> where = new ArrayList<>();
			l.getEntities(type, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(12), e -> true)
				.forEach(e -> where.add(e.blockPosition().toShortString() + " age " + e.tickCount));
			check(false, label + "; found " + found + " at " + where + " (site box " + b + ")");
		}
	}

	private static int count(ServerLevel l, DungeonPiece piece, net.minecraft.world.entity.EntityType<?> type) {
		// With a margin: villagers wander out of doors and drowned swim off a little. Only the site's own villagers count,
		// so a natural village nearby on a random seed can't change the tally.
		var b = piece.getBoundingBox();
		return l.getEntities(type, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(12),
			e -> type != EntityTypes.VILLAGER || e.entityTags().contains("wildercord.site_resident")).size();
	}

	private static void chest(ServerLevel l, DungeonPiece piece, int x, int y, int z, String loot) {
		var be = l.getBlockEntity(piece.localPosition(x, y, z));
		check(be instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null
			&& chest.getLootTable().identifier().getPath().equals("chests/" + loot), loot + " chest in place with its loot");
	}

	private static void book(ServerLevel l, DungeonPiece piece, int x, int y, int z, String label) {
		check(l.getBlockEntity(piece.localPosition(x, y, z)) instanceof LecternBlockEntity lectern && lectern.hasBook(), label + " lies on its lectern");
	}

	private static void is(ServerLevel l, DungeonPiece piece, Block block, int x, int y, int z, String label) {
		BlockState there = l.getBlockState(piece.localPosition(x, y, z));
		check(there.is(block), label + ": expected " + block + ", found " + there);
	}

	private static void air(ServerLevel l, DungeonPiece piece, int x, int y, int z, String label) {
		BlockState there = l.getBlockState(piece.localPosition(x, y, z));
		check(there.isAir(), label + " is open; found " + there);
	}

	private static void wet(ServerLevel l, DungeonPiece piece, int x, int y, int z, String label) {
		check(l.getFluidState(piece.localPosition(x, y, z)).is(FluidTags.WATER), label + " holds water");
	}

	private static void dry(ServerLevel l, DungeonPiece piece, int x, int y, int z, String label) {
		check(l.getFluidState(piece.localPosition(x, y, z)).isEmpty(), label + " is dry; found " + l.getBlockState(piece.localPosition(x, y, z)));
	}

	/** Takes the player to a spot in a site (so its chunks tick). */
	private static void visit(ClientGameTestContext c, TestSingleplayerContext world, DungeonPiece piece, int x, int y, int z) {
		world.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			var at = piece.localPosition(x, y, z);
			p.teleportTo(s.overworld(), at.getX() + .5, at.getY(), at.getZ() + .5, Set.<Relative>of(), 0, 0, false);
		});
		c.waitTicks(10);
	}

	/** A player stands at local {@code (x, y, z)} facing local {@code (x, toZ)} and holds forward for {@code ticks}. */
	private static void walk(ClientGameTestContext c, TestSingleplayerContext world, DungeonPiece piece, int x, int y, int z, int toZ, int ticks) {
		world.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			p.setGameMode(GameType.CREATIVE);
			p.getAbilities().flying = false;
			p.onUpdateAbilities();
			var at = piece.localPosition(x, y, z);
			var next = piece.localPosition(x, y, toZ);
			float yaw = Direction.getNearest(next.getX() - at.getX(), 0, next.getZ() - at.getZ(), null).toYRot();
			p.teleportTo(s.overworld(), at.getX() + .5, at.getY(), at.getZ() + .5, Set.<Relative>of(), yaw, 0, false);
			p.setDeltaMovement(Vec3.ZERO);
		});
		c.waitTicks(10);
		c.runOnClient(mc -> mc.options.keyUp.setDown(true));
		c.waitTicks(ticks);
		c.runOnClient(mc -> mc.options.keyUp.setDown(false));
		c.waitTicks(5);
	}

	/** A screenshot from local {@code from} looking at local {@code to}. */
	private static void shot(ClientGameTestContext c, TestSingleplayerContext world, DungeonPiece piece, Vec3 from, Vec3 to, String name) {
		BlockPos origin = piece.localPosition(0, 0, 0), ex = piece.localPosition(1, 0, 0), ez = piece.localPosition(0, 0, 1);
		Vec3 ux = Vec3.atLowerCornerOf(ex.subtract(origin)), uz = Vec3.atLowerCornerOf(ez.subtract(origin));
		Vec3 base = Vec3.atCenterOf(origin);
		Vec3 at = base.add(ux.scale(from.x)).add(uz.scale(from.z)).add(0, from.y, 0);
		Vec3 target = base.add(ux.scale(to.x)).add(uz.scale(to.z)).add(0, to.y, 0);
		Vec3 d = target.subtract(at);
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		world.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().getFirst();
			p.getAbilities().flying = true;
			p.onUpdateAbilities();
			p.teleportTo(s.overworld(), at.x, at.y, at.z, Set.<Relative>of(), yaw, pitch, false);
		});
		c.waitTicks(20);
		world.getConnection().waitForChunksRender();
		c.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		c.waitTicks(5);
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void check(boolean value, String label) {
		if (!value) {
			throw new AssertionError(label);
		}
	}
}
