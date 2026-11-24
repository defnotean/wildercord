package dev.wildercord.wildlife;

import dev.wildercord.aura.world.AshenStag;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.DrownedBellWarden;
import dev.wildercord.aura.world.FrostWyrmling;
import dev.wildercord.aura.world.Guardians;
import dev.wildercord.aura.world.HollowMatron;
import dev.wildercord.aura.world.LanternBearer;
import dev.wildercord.aura.world.Mojito;
import dev.wildercord.aura.world.RootcoilSerpent;
import dev.wildercord.cast.Archivist;
import dev.wildercord.cast.DungeonBoss;
import dev.wildercord.cast.DungeonEntities;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.world.cartographer.CartographerContent;
import dev.wildercord.world.cartographer.GlassCartographer;
import dev.wildercord.world.dungeons.DungeonWards;
import dev.wildercord.world.ferryman.FerrymanContent;
import dev.wildercord.world.ferryman.ReedCrownedFerryman;
import dev.wildercord.world.hollowbell.HollowBell;
import dev.wildercord.world.hollowbell.HollowBellContent;
import dev.wildercord.world.kilnwidow.KilnWidow;
import dev.wildercord.world.kilnwidow.KilnWidowContent;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Constructs the gnarled Hollow Ancient Tree and lazily carves/populates the three Rabbit-Hole
 * destinations: a Random Cave, a Wonderland Root Treasure Vault, and the Grand Boss-Rush Colosseum
 * holding all 17 Wildercord bosses.
 */
public final class HollowTreeBuilder {
	private HollowTreeBuilder() {}

	/** Finds an unobstructed forest surface spot 10–30 blocks away from {@code origin}. */
	public static BlockPos findTreeSite(ServerLevel level, BlockPos origin, RandomSource random) {
		for (int pass = 0; pass < 2; pass++) {
			int minR = pass == 0 ? WhiteRabbitRules.TREE_MIN_DIST : 10;
			int maxR = pass == 0 ? WhiteRabbitRules.TREE_MAX_DIST : 24;
			for (int attempt = 0; attempt < 24; attempt++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double dist = minR + random.nextDouble() * (maxR - minR);
				int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
				int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);
				BlockPos candidate = new BlockPos(x, origin.getY(), z);
				level.getChunk(candidate);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (y <= level.getMinY() + 2 || y >= level.getMaxY() - 12) {
					continue;
				}
				BlockPos base = new BlockPos(x, y, z);
				BlockPos below = base.below();
				BlockState floor = level.getBlockState(below);
				if (!floor.isFaceSturdy(level, below, Direction.UP) || !level.getFluidState(below).isEmpty()) {
					continue;
				}
				if (DungeonWards.warded(level, base) || OathholdLaw.isInsideCityFootprint(level, base)) {
					continue;
				}
				return base;
			}
		}
		return null;
	}

	/**
	 * Places an ancient gnarled hollow oak tree whose trunk opens toward {@code facing} onto a
	 * glowing {@link RabbitHolePortalBlock} at {@code base}. Returns the portal position.
	 */
	public static BlockPos placeHollowTree(ServerLevel level, BlockPos base, Direction facing,
			WhiteRabbitRules.Outcome forcedOutcome) {
		level.getChunk(base);
		Direction front = facing.getAxis().isHorizontal() ? facing : Direction.SOUTH;
		Direction back = front.getOpposite();
		Direction left = front.getCounterClockWise();
		Direction right = front.getClockWise();

		BlockState wood = Blocks.DARK_OAK_WOOD.defaultBlockState();
		BlockState darkWall = Blocks.OBSIDIAN.defaultBlockState();
		BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();
		BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

		// Grounding foundation under the 3x3 trunk and approach steps.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -3; dy <= -1; dy++) {
					BlockPos p = base.offset(dx, dy, dz);
					if (level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()) {
						setSafe(level, p, dy == -1 ? (Math.abs(dx) + Math.abs(dz) > 2 ? moss : Blocks.ROOTED_DIRT.defaultBlockState()) : Blocks.DIRT.defaultBlockState());
					}
				}
			}
		}

		// Clear an approach apron in front of the hollow doorway so rabbit and player can enter smoothly.
		for (int step = 1; step <= 4; step++) {
			BlockPos ahead = base.relative(front, step);
			setSafe(level, ahead.below(), moss);
			for (int w = -1; w <= 1; w++) {
				BlockPos lane = ahead.relative(left, w);
				setSafe(level, lane, Blocks.AIR.defaultBlockState());
				setSafe(level, lane.above(), Blocks.AIR.defaultBlockState());
				setSafe(level, lane.above(2), Blocks.AIR.defaultBlockState());
			}
		}

		// 3x3 ancient trunk (height 7), hollowing the center doorway + deep dark backdrop behind the portal.
		for (int dy = 0; dy <= 6; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					setSafe(level, base.offset(dx, dy, dz), wood);
				}
			}
		}

		// Flared buttress roots gripping the forest floor.
		for (Direction d : List.of(back, left, right)) {
			BlockPos r1 = base.relative(d, 2);
			setSafe(level, r1.below(), moss);
			setSafe(level, r1, wood);
			if (level.getRandom().nextBoolean()) {
				setSafe(level, r1.above(), Blocks.RED_MUSHROOM.defaultBlockState());
			}
		}
		for (Direction d : List.of(left, right)) {
			BlockPos diag = base.relative(front).relative(d, 2);
			setSafe(level, diag.below(), moss);
			setSafe(level, diag, Blocks.MOSS_CARPET.defaultBlockState());
		}

		// Layered dark-oak canopy + hanging lanterns.
		for (int dy = 4; dy <= 8; dy++) {
			int radius = dy == 4 ? 3 : dy <= 6 ? 4 : dy == 7 ? 3 : 2;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) + Math.abs(dz) > radius + 2) {
						continue;
					}
					if (dy <= 6 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
						continue;
					}
					BlockPos leafPos = base.offset(dx, dy, dz);
					if (level.getBlockState(leafPos).isAir()) {
						setSafe(level, leafPos, leaves);
					}
				}
			}
		}
		BlockPos branchL = base.relative(front, 2).relative(left, 2).above(4);
		BlockPos branchR = base.relative(front, 2).relative(right, 2).above(4);
		setSafe(level, branchL, wood);
		setSafe(level, branchL.below(), lantern);
		setSafe(level, branchR, wood);
		setSafe(level, branchR.below(), lantern);

		// Hollow doorway cavity + swirling burrow portal inside base.
		BlockPos entry = base.relative(front);
		setSafe(level, entry, Blocks.AIR.defaultBlockState());
		setSafe(level, entry.above(), Blocks.AIR.defaultBlockState());
		setSafe(level, base.above(), Blocks.AIR.defaultBlockState());
		setSafe(level, base.below(), darkWall);
		setSafe(level, base.relative(back), darkWall);
		setSafe(level, base.relative(back).above(), darkWall);
		setSafe(level, entry.above(2), wood);
		BlockPos doorLantern = base.relative(front, 2).above(2);
		setSafe(level, doorLantern.above(), wood);
		setSafe(level, doorLantern, lantern);

		setSafe(level, base, WhiteRabbitContent.RABBIT_HOLE_PORTAL.defaultBlockState());
		WhiteRabbitRules.Outcome outcome = forcedOutcome != null
			? forcedOutcome
			: WhiteRabbitRules.rollOutcome(level.getRandom().nextDouble());
		if (level.getBlockEntity(base) instanceof RabbitHolePortalBlockEntity be) {
			be.configureOutcome(outcome);
		}
		return base.immutable();
	}

	/** Builds or locates a subterranean cave destination and installs a Return Burrow Portal. */
	public static BlockPos buildOrFindRandomCave(ServerLevel level, BlockPos treePortalPos, RandomSource random) {
		BlockPos landing = tryFindNaturalCave(level, treePortalPos, random);
		BlockPos returnTarget = treeReturnSpot(level, treePortalPos);
		if (landing != null) {
			BlockPos portal = landing.south(2);
			clearCavePocket(level, landing, portal);
			prepareReturnAlcove(level, portal, returnTarget, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			return landing;
		}
		int cx = treePortalPos.getX() + 72;
		int cz = treePortalPos.getZ() + 72;
		int floorY = Math.max(level.getMinY() + 18, -28);
		BlockPos center = new BlockPos(cx, floorY, cz);
		carveNaturalCaveChamber(level, center, random);
		BlockPos portal = center.offset(0, 1, 4);
		prepareReturnAlcove(level, portal, returnTarget, Blocks.STONE_BRICKS.defaultBlockState());
		return center.above();
	}

	private static void clearCavePocket(ServerLevel level, BlockPos landing, BlockPos portal) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				for (int dz = -2; dz <= 3; dz++) {
					BlockPos p = landing.offset(dx, dy, dz);
					if (!p.equals(portal)) {
						setSafe(level, p, Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		setSafe(level, landing.offset(-1, 0, 1), Blocks.AMETHYST_BLOCK.defaultBlockState());
		setSafe(level, landing.offset(1, 0, 1), Blocks.SHROOMLIGHT.defaultBlockState());
		setSafe(level, landing.offset(-1, 1, 2), Blocks.TORCH.defaultBlockState());
	}

	private static BlockPos tryFindNaturalCave(ServerLevel level, BlockPos origin, RandomSource random) {
		int minY = Math.max(level.getMinY() + 14, -32);
		int maxY = Math.min(origin.getY() - 16, 28);
		if (maxY <= minY) {
			return null;
		}
		for (int tries = 0; tries < 10; tries++) {
			int dist = 64 + random.nextInt(96);
			double ang = random.nextDouble() * Math.PI * 2.0;
			int x = origin.getX() + (int) Math.round(Math.cos(ang) * dist);
			int z = origin.getZ() + (int) Math.round(Math.sin(ang) * dist);
			level.getChunk(new BlockPos(x, minY, z));
			for (int y = maxY; y >= minY; y -= 3) {
				BlockPos p = new BlockPos(x, y, z);
				if (DungeonWards.warded(level, p) || OathholdLaw.isInsideCityFootprint(level, p)) {
					continue;
				}
				if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
					&& level.getFluidState(p.below()).isEmpty()
					&& level.getBlockState(p).isAir()
					&& level.getBlockState(p.above()).isAir()
					&& level.getBlockState(p.above(2)).isAir()
					&& noLavaNear(level, p)) {
					return p.immutable();
				}
			}
		}
		return null;
	}

	private static boolean noLavaNear(ServerLevel level, BlockPos center) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (level.getBlockState(center.offset(dx, dy, dz)).is(Blocks.LAVA)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static void carveNaturalCaveChamber(ServerLevel level, BlockPos center, RandomSource random) {
		for (int dx = -6; dx <= 6; dx++) {
			for (int dy = -1; dy <= 6; dy++) {
				for (int dz = -6; dz <= 6; dz++) {
					double r = Math.hypot(dx, dz) + Math.abs(dy - 2) * 0.85;
					BlockPos p = center.offset(dx, dy, dz);
					level.getChunk(p);
					if (DungeonWards.warded(level, p)) {
						continue;
					}
					if (dy == 0 && r <= 6.4) {
						BlockState floor = random.nextInt(5) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState()
							: (dy < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState());
						level.setBlock(p, floor, Block.UPDATE_ALL);
					} else if (dy > 0 && dy <= 5 && r <= 5.1) {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					} else if (r <= 6.8) {
						int roll = random.nextInt(18);
						BlockState wall = switch (roll) {
							case 0 -> Blocks.AMETHYST_BLOCK.defaultBlockState();
							case 1 -> Blocks.IRON_ORE.defaultBlockState();
							case 2 -> Blocks.COAL_ORE.defaultBlockState();
							default -> Blocks.STONE.defaultBlockState();
						};
						level.setBlock(p, wall, Block.UPDATE_ALL);
					}
				}
			}
		}
		// Ambient cave lighting & dripstone accents.
		level.setBlock(center.offset(-3, 4, -2), Blocks.SHROOMLIGHT.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(center.offset(3, 4, -2), Blocks.SHROOMLIGHT.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(center.offset(0, 4, 2), Blocks.SHROOMLIGHT.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(center.offset(-2, 1, -2), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(center.offset(2, 1, -2), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** Carves an ornate Wonderland Root-Vault filled with treasure chests & return portal. */
	public static BlockPos buildTreasureVault(ServerLevel level, BlockPos treePortalPos, RandomSource random) {
		int cx = treePortalPos.getX() + 48;
		int cz = treePortalPos.getZ() - 48;
		int floorY = Math.max(level.getMinY() + 18, -34);
		BlockPos center = new BlockPos(cx, floorY, cz);

		for (int dx = -6; dx <= 6; dx++) {
			for (int dy = -1; dy <= 7; dy++) {
				for (int dz = -6; dz <= 6; dz++) {
					BlockPos p = center.offset(dx, dy, dz);
					level.getChunk(p);
					if (DungeonWards.warded(level, p)) {
						continue;
					}
					boolean shell = Math.abs(dx) == 6 || Math.abs(dz) == 6 || dy <= 0 || dy >= 6;
					if (shell) {
						BlockState s;
						if (dy == 0) {
							s = Math.abs(dx) <= 2 && Math.abs(dz) <= 2
								? Blocks.GILDED_BLACKSTONE.defaultBlockState()
								: Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
						} else if (dy == 7) {
							s = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
						} else if (Math.abs(dx) == 6 && Math.abs(dz) == 6) {
							s = Blocks.DARK_OAK_WOOD.defaultBlockState();
						} else if ((dx + dz + dy) % 5 == 0) {
							s = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
						} else {
							s = Blocks.DEEPSLATE_TILES.defaultBlockState();
						}
						level.setBlock(p, s, Block.UPDATE_ALL);
					} else {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		}

		// Ornate pillars, lanterns & precious blocks.
		for (int sx : new int[] { -3, 3 }) {
			for (int sz : new int[] { -3, 3 }) {
				for (int dy = 1; dy <= 5; dy++) {
					level.setBlock(center.offset(sx, dy, sz), Blocks.DARK_OAK_WOOD.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}
		level.setBlock(center.offset(0, 5, 0), Blocks.SEA_LANTERN.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(center.offset(-2, 1, -4), Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(center.offset(2, 1, -4), Blocks.EMERALD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		for (int dx : new int[] { -2, 2 }) {
			level.setBlock(center.offset(dx, 5, 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), Block.UPDATE_ALL);
		}

		// Three rich treasure chests on the North shelf.
		List<BlockPos> chestSpots = List.of(
			center.offset(-3, 1, -4),
			center.offset(0, 1, -4),
			center.offset(3, 1, -4)
		);
		for (int idx = 0; idx < chestSpots.size(); idx++) {
			BlockPos cp = chestSpots.get(idx);
			level.setBlock(cp, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(cp) instanceof ChestBlockEntity chest) {
				stockTreasureChest(chest, idx, random);
			}
		}

		BlockPos returnPortalPos = center.offset(0, 1, 5);
		prepareReturnAlcove(level, returnPortalPos, treeReturnSpot(level, treePortalPos), Blocks.GILDED_BLACKSTONE.defaultBlockState());
		return center.offset(0, 1, 2);
	}

	private static void stockTreasureChest(ChestBlockEntity chest, int index, RandomSource random) {
		chest.setItem(0, new ItemStack(Items.DIAMOND, 2 + random.nextInt(4)));
		chest.setItem(1, new ItemStack(Items.EMERALD, 6 + random.nextInt(8)));
		chest.setItem(2, new ItemStack(WildercordItems.MANA_CRYSTAL, 2 + random.nextInt(3)));
		chest.setItem(3, new ItemStack(AuraWorld.AURA_SHARD, 2 + random.nextInt(4)));
		chest.setItem(4, new ItemStack(WildercordItems.TORN_PAGE, 1 + random.nextInt(2)));
		chest.setItem(5, new ItemStack(Items.GOLDEN_APPLE, 1 + random.nextInt(2)));
		chest.setItem(6, new ItemStack(Items.EXPERIENCE_BOTTLE, 6 + random.nextInt(6)));
		if (index == 1) {
			chest.setItem(7, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
			chest.setItem(8, new ItemStack(WildercordItems.ECHO_CORD));
		} else {
			chest.setItem(7, new ItemStack(Items.GOLD_INGOT, 4 + random.nextInt(6)));
		}
		chest.setChanged();
	}

	/**
	 * Carves the monumental subterranean Grand Boss-Rush Colosseum and summons all 17 distinct
	 * Wildercord bosses simultaneously (with a Return Burrow Portal in the South alcove).
	 */
	public static BlockPos buildAllBossesArena(ServerLevel level, BlockPos treePortalPos,
			ServerPlayer triggerPlayer, RandomSource random) {
		int cx = treePortalPos.getX() - 64;
		int cz = treePortalPos.getZ() + 64;
		int floorY = Math.max(level.getMinY() + 18, -42);
		BlockPos center = new BlockPos(cx, floorY, cz);

		int outerR = 22;
		int innerR = 20;
		for (int dx = -outerR; dx <= outerR; dx++) {
			for (int dz = -outerR; dz <= outerR; dz++) {
				double dist = Math.hypot(dx, dz);
				if (dist > outerR) {
					continue;
				}
				boolean edgeWall = dist > innerR;
				for (int dy = -1; dy <= 16; dy++) {
					BlockPos p = center.offset(dx, dy, dz);
					level.getChunk(p);
					if (DungeonWards.warded(level, p)) {
						continue;
					}
					if (dy == -1) {
						level.setBlock(p, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_ALL);
					} else if (dy == 0) {
						boolean waterBand = dist >= 10.5 && dist <= 13.5 && Math.abs(dx) > 2 && Math.abs(dz) > 2;
						if (waterBand) {
							level.setBlock(p, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
						} else if (dist <= 4.5) {
							boolean trimLantern = (dx == 0 && dz == 0) || (Math.abs(dx) == 3 && Math.abs(dz) == 3);
							level.setBlock(p, trimLantern
								? Blocks.SEA_LANTERN.defaultBlockState()
								: Blocks.GILDED_BLACKSTONE.defaultBlockState(), Block.UPDATE_ALL);
						} else {
							level.setBlock(p, (dx + dz) % 2 == 0
								? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
								: Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
						}
					} else if (dy == 16 || (edgeWall && dy >= 1 && dy <= 15)) {
						BlockState s = dy == 16 && (Math.abs(dx) + Math.abs(dz)) % 6 == 0
							? Blocks.SEA_LANTERN.defaultBlockState()
							: (edgeWall && dy % 4 == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
						level.setBlock(p, s, Block.UPDATE_ALL);
					} else {
						level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
					}
				}
			}
		}

		// Perimeter lit obelisks around the colosseum.
		for (int i = 0; i < 8; i++) {
			int[] off = WhiteRabbitRules.bossRingOffset(i, 8, 17.5);
			for (int dy = 1; dy <= 5; dy++) {
				level.setBlock(center.offset(off[0], dy, off[1]),
					dy == 5 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState(),
					Block.UPDATE_ALL);
			}
		}

		BlockPos landing = center.offset(0, 1, 14);
		BlockPos returnPortalPos = center.offset(0, 1, 18);
		prepareReturnAlcove(level, returnPortalPos, treeReturnSpot(level, treePortalPos), Blocks.GILDED_BLACKSTONE.defaultBlockState());

		if (level.getDifficulty() != Difficulty.PEACEFUL) {
			BoundingBox arenaBox = new BoundingBox(cx - 21, floorY - 1, cz - 21, cx + 21, floorY + 16, cz + 21);
			spawnAllBosses(level, center, arenaBox, triggerPlayer);
		}
		return landing;
	}

	private static void spawnAllBosses(ServerLevel level, BlockPos center, BoundingBox arenaBox,
			ServerPlayer triggerPlayer) {
		// 1-4: Encounter bosses placed within ±8 X/Z of South landing pad (cx, y+1, cz+14) so eligible() passes cleanly.
		spawnEncounterBosses(level, center, triggerPlayer);

		// 5: Archivist above central dais.
		Archivist archivist = Archivist.rise(level, center);
		tagBoss(archivist);

		// 6: Drowned Bell-Warden in the Northern water ring.
		BlockPos bellPos = center.offset(0, 1, -12);
		DrownedBellWarden bellWarden = dev.wildercord.compat.McEntities.create(Guardians.BELL_WARDEN, level, MobSpawnType.TRIGGERED);
		if (bellWarden != null) {
			bellWarden.bind(bellPos, 0.0F, arenaBox);
			tagBoss(bellWarden);
			level.addFreshEntity(bellWarden);
		}

		// 7-17: Remaining 6 Lair Guardians + 5 DungeonBosses spaced around a Northern/East/West arc.
		List<BlockPos> arcSpots = List.of(
			center.offset(-14, 1, -4),   // Rootcoil Serpent
			center.offset(-12, 1, -9),   // Lantern-Bearer
			center.offset(-7, 1, -13),   // Hollow Matron
			center.offset(7, 1, -13),    // Ashen Stag
			center.offset(12, 1, -9),    // Frost Wyrmling
			center.offset(14, 1, -4),    // Mojito
			center.offset(-10, 1, 0),    // Cinder Warden
			center.offset(-6, 1, -5),    // Root Guardian
			center.offset(0, 1, -6),     // Star-Eater
			center.offset(6, 1, -5),     // Storm Conductor
			center.offset(10, 1, 0)      // Tide Scribe
		);

		RootcoilSerpent serpent = dev.wildercord.compat.McEntities.create(Guardians.SERPENT, level, MobSpawnType.TRIGGERED);
		if (serpent != null) {
			serpent.bind(arcSpots.get(0), 225.0F);
			tagBoss(serpent);
			level.addFreshEntity(serpent);
		}

		BlockPos bearerSpot = arcSpots.get(1);
		level.setBlock(bearerSpot.above(5), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
		LanternBearer bearer = dev.wildercord.compat.McEntities.create(Guardians.LANTERN_BEARER, level, MobSpawnType.TRIGGERED);
		if (bearer != null) {
			bearer.bind(bearerSpot, 210.0F, arenaBox, bearerSpot.above(4), List.of());
			tagBoss(bearer);
			level.addFreshEntity(bearer);
		}

		BlockPos matronSpot = arcSpots.get(2);
		HollowMatron matron = dev.wildercord.compat.McEntities.create(Guardians.HOLLOW_MATRON, level, MobSpawnType.TRIGGERED);
		if (matron != null) {
			matron.bind(matronSpot.above(7), matronSpot.getY(), (float) (matronSpot.getY() + 13), 180.0F, arenaBox);
			matron.nestle(List.of(), List.of());
			tagBoss(matron);
			level.addFreshEntity(matron);
		}

		AshenStag stag = dev.wildercord.compat.McEntities.create(Guardians.ASHEN_STAG, level, MobSpawnType.TRIGGERED);
		if (stag != null) {
			stag.bind(arcSpots.get(3), 180.0F, arenaBox);
			tagBoss(stag);
			level.addFreshEntity(stag);
		}

		FrostWyrmling wyrmling = dev.wildercord.compat.McEntities.create(Guardians.FROST_WYRMLING, level, MobSpawnType.TRIGGERED);
		if (wyrmling != null) {
			wyrmling.bind(arcSpots.get(4), 150.0F, arenaBox);
			tagBoss(wyrmling);
			level.addFreshEntity(wyrmling);
		}

		Mojito mojito = dev.wildercord.compat.McEntities.create(Guardians.MOJITO, level, MobSpawnType.TRIGGERED);
		if (mojito != null) {
			mojito.bind(arcSpots.get(5), 135.0F, arenaBox);
			tagBoss(mojito);
			level.addFreshEntity(mojito);
		}

		tagBoss(DungeonBoss.spawnInArena(level, DungeonEntities.CINDER_WARDEN, arcSpots.get(6), 0.2));
		tagBoss(DungeonBoss.spawnInArena(level, DungeonEntities.ROOT_GUARDIAN, arcSpots.get(7), 0.2));
		tagBoss(DungeonBoss.spawnInArena(level, DungeonEntities.STAR_EATER, arcSpots.get(8), 1.5));
		tagBoss(DungeonBoss.spawnInArena(level, DungeonEntities.STORM_CONDUCTOR, arcSpots.get(9), 0.2));
		tagBoss(DungeonBoss.spawnInArena(level, DungeonEntities.TIDE_SCRIBE, arcSpots.get(10), 0.2));
	}

	private static void spawnEncounterBosses(ServerLevel level, BlockPos center, ServerPlayer triggerPlayer) {
		BlockPos ferrySpot = center.offset(-6, 1, 11);
		ReedCrownedFerryman ferryman = dev.wildercord.compat.McEntities.create(FerrymanContent.FERRYMAN, level, MobSpawnType.TRIGGERED);
		if (ferryman != null) {
			ferryman.bind(ferrySpot, ferrySpot.below(2));
			tagBoss(ferryman);
			level.addFreshEntity(ferryman);
			if (triggerPlayer != null) ferryman.challenge(triggerPlayer);
		}

		BlockPos cartoSpot = center.offset(6, 1, 11);
		GlassCartographer carto = dev.wildercord.compat.McEntities.create(CartographerContent.CARTOGRAPHER, level, MobSpawnType.TRIGGERED);
		if (carto != null) {
			carto.bind(cartoSpot.below(), cartoSpot.below(2));
			tagBoss(carto);
			level.addFreshEntity(carto);
			if (triggerPlayer != null) carto.challenge(triggerPlayer);
		}

		BlockPos bellSpot = center.offset(-8, 1, 6);
		HollowBell hollowBell = dev.wildercord.compat.McEntities.create(HollowBellContent.BELL, level, MobSpawnType.TRIGGERED);
		if (hollowBell != null) {
			hollowBell.bind(bellSpot.below(), bellSpot.below(2));
			tagBoss(hollowBell);
			level.addFreshEntity(hollowBell);
			if (triggerPlayer != null) hollowBell.challenge(triggerPlayer);
		}

		BlockPos widowSpot = center.offset(8, 1, 6);
		KilnWidow widow = dev.wildercord.compat.McEntities.create(KilnWidowContent.WIDOW, level, MobSpawnType.TRIGGERED);
		if (widow != null) {
			widow.bind(widowSpot.below(), widowSpot.below(2));
			tagBoss(widow);
			level.addFreshEntity(widow);
			if (triggerPlayer != null) widow.challenge(triggerPlayer);
		}
	}

	/** Re-engages boss-rush encounter bosses if a subsequent challenger steps in. */
	public static void challengeEncounterBossesNear(ServerLevel level, BlockPos landing, ServerPlayer player) {
		AABB box = new AABB(landing).inflate(28.0);
		for (ReedCrownedFerryman e : level.getEntitiesOfClass(ReedCrownedFerryman.class, box)) e.challenge(player);
		for (GlassCartographer e : level.getEntitiesOfClass(GlassCartographer.class, box)) e.challenge(player);
		for (HollowBell e : level.getEntitiesOfClass(HollowBell.class, box)) e.challenge(player);
		for (KilnWidow e : level.getEntitiesOfClass(KilnWidow.class, box)) e.challenge(player);
	}

	private static void tagBoss(Mob boss) {
		if (boss != null) {
			boss.addTag(WhiteRabbitRules.BOSS_RUSH_TAG);
			boss.setPersistenceRequired();
		}
	}

	private static BlockPos treeReturnSpot(ServerLevel level, BlockPos treePortalPos) {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos out = treePortalPos.relative(d, 3);
			if (level.getBlockState(out).isAir() && level.getBlockState(out.above()).isAir()) {
				return out.immutable();
			}
		}
		return treePortalPos.above(2).immutable();
	}

	private static void prepareReturnAlcove(ServerLevel level, BlockPos portalPos, BlockPos returnTarget, BlockState rim) {
		level.getChunk(portalPos);
		level.setBlock(portalPos.below(), rim, Block.UPDATE_ALL);
		level.setBlock(portalPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(portalPos.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(portalPos, WhiteRabbitContent.RABBIT_HOLE_PORTAL.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(portalPos) instanceof RabbitHolePortalBlockEntity be) {
			be.configureReturn(returnTarget);
		}
	}

	private static void setSafe(ServerLevel level, BlockPos pos, BlockState state) {
		if (!DungeonWards.warded(level, pos) && !OathholdLaw.isInsideCityFootprint(level, pos)) {
			level.setBlock(pos, state, Block.UPDATE_ALL);
		}
	}
}
