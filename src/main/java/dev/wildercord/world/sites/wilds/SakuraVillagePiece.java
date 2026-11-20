package dev.wildercord.world.sites.wilds;

import dev.wildercord.wildlife.SakuraVillager;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatContent;
import dev.wildercord.wildlife.ShrineCatRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A separate peaceful Japanese Sakura Village standing among blossom trees: a vermilion Torii gate,
 * arched crimson bridge over a lantern-lit koi brook, a cedar Tea House with its {@code Tea Master},
 * a Cat Shrine pavilion attended by a {@code Shrine Maiden}, and all four cats ({@code Yuki}, {@code Odie},
 * {@code Freya}, {@code Kuro}) lounging peacefully on carpets and verandas.
 */
public final class SakuraVillagePiece extends WildsPiece {
	private static final int WIDTH = 45, HEIGHT = 18, DEPTH = 50;

	public SakuraVillagePiece(int x, int z, Direction facing) {
		super(WildsSites.SAKURA_VILLAGE, x, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SakuraVillagePiece(CompoundTag tag) {
		super(WildsSites.SAKURA_VILLAGE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new SakuraVillagePiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 10);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		footing(level, bb, 0, 0, WIDTH - 1, DEPTH - 1, s(Blocks.DIRT));
		fill(level, bb, 0, 0, 0, WIDTH - 1, 0, DEPTH - 1, s(Blocks.GRASS_BLOCK));
		clear(level, bb, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1);

		// Scatter pink blossom petals along the outer garden.
		for (int x = 1; x < WIDTH - 1; x++) {
			for (int z = 1; z < DEPTH - 1; z++) {
				if (noise(x, z) < 20) {
					set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
				}
			}
		}

		// Village gravel lane + vermilion Torii gate at z = 4.
		for (int z = 0; z < DEPTH - 4; z++) {
			for (int x = 20; x <= 24; x++) {
				set(level, bb, s(Blocks.SMOOTH_STONE), x, 0, z);
			}
		}
		for (int x : new int[]{19, 25}) {
			fill(level, bb, x, 1, 4, x, 5, 4, s(Blocks.DYED_TERRACOTTA.red()));
		}
		fill(level, bb, 18, 4, 4, 26, 4, 4, s(Blocks.DYED_TERRACOTTA.red()));
		fill(level, bb, 17, 6, 4, 27, 6, 4, s(Blocks.DEEPSLATE_TILES));

		// Koi stream & arched crimson bridge across z = 11..15.
		for (int x = 1; x < WIDTH - 1; x++) {
			for (int z = 11; z <= 15; z++) {
				set(level, bb, s(Blocks.SAND), x, -1, z);
				set(level, bb, s(Blocks.WATER), x, 0, z);
				set(level, bb, AIR, x, 1, z);
				if ((x == 8 || x == 36) && z == 13) {
					set(level, bb, s(Blocks.SEA_LANTERN), x, -1, z);
				} else if (noise(x, z) < 10 && Math.abs(x - 22) > 4) {
					set(level, bb, s(Blocks.LILY_PAD), x, 1, z);
				}
			}
		}
		for (int x = 20; x <= 24; x++) {
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.NORTH, false), x, 1, 11);
			set(level, bb, s(Blocks.CRIMSON_PLANKS), x, 1, 12);
			set(level, bb, s(Blocks.CRIMSON_PLANKS), x, 1, 14);
			set(level, bb, s(Blocks.CRIMSON_SLAB), x, 2, 13);
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false), x, 1, 15);
		}
		for (int z = 11; z <= 15; z++) {
			set(level, bb, s(Blocks.CRIMSON_FENCE), 20, 2, z);
			set(level, bb, s(Blocks.CRIMSON_FENCE), 24, 2, z);
		}

		// Tea House (west plaza: x = 4..17, z = 21..34).
		teaHouse(level, bb);
		// Cat Shrine Pavilion (east plaza: x = 27..40, z = 21..34).
		catShrine(level, bb);
		// Sakura trees behind buildings.
		for (int[] t : new int[][]{{6, 41}, {22, 43}, {38, 41}, {5, 6}, {39, 6}}) {
			cherryTree(level, bb, t[0], t[1]);
		}

		// Residents & all 4 real cats roaming peacefully!
		spawnVillager(level, bb, 11, 1, 27, SakuraVillager.TEA_MASTER);
		spawnVillager(level, bb, 33, 1, 27, SakuraVillager.SHRINE_MAIDEN);
		spawnVillageCat(level, bb, 19, 1, 22, ShrineCatRules.Variant.YUKI);
		spawnVillageCat(level, bb, 13, 1, 24, ShrineCatRules.Variant.ODIE);
		spawnVillageCat(level, bb, 31, 1, 24, ShrineCatRules.Variant.FREYA);
		spawnVillageCat(level, bb, 26, 1, 29, ShrineCatRules.Variant.KURO);
	}

	private void teaHouse(WorldGenLevel level, BoundingBox bb) {
		BlockState cherry = s(Blocks.CHERRY_PLANKS), paper = s(Blocks.CALCITE), log = s(Blocks.STRIPPED_CHERRY_LOG);
		fill(level, bb, 4, 0, 21, 17, 0, 34, s(Blocks.BAMBOO_PLANKS));
		for (int[] p : new int[][]{{4, 21}, {17, 21}, {4, 34}, {17, 34}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 5, p[1], log);
		}
		// Shoji walls with wide open east veranda toward center lane.
		fill(level, bb, 5, 1, 21, 16, 4, 21, paper);
		fill(level, bb, 5, 1, 34, 16, 4, 34, paper);
		fill(level, bb, 4, 1, 22, 4, 4, 33, paper);
		fill(level, bb, 9, 1, 26, 12, 1, 29, s(Blocks.CARPET.red()));
		set(level, bb, s(Blocks.LANTERN), 17, 2, 24);
		set(level, bb, s(Blocks.LANTERN), 17, 2, 31);
		hipRoof(level, bb, 4, 21, 17, 34, 6, Blocks.DARK_OAK_STAIRS, cherry, cherry);
		chest(level, bb, 6, 1, 32, WildsSites.SAKURA_HALL, Direction.NORTH, 17L);
	}

	private void catShrine(WorldGenLevel level, BoundingBox bb) {
		BlockState cherry = s(Blocks.CHERRY_PLANKS), gold = s(Blocks.POLISHED_ANDESITE);
		fill(level, bb, 27, 0, 21, 40, 0, 34, cherry);
		fill(level, bb, 30, 0, 24, 37, 0, 31, gold);
		for (int[] p : new int[][]{{28, 22}, {39, 22}, {28, 33}, {39, 33}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 5, p[1], s(Blocks.DYED_TERRACOTTA.red()));
			set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), p[0], 5, p[1]);
		}
		// Cushions for Yuki, Odie, Freya, and Kuro + central Suzu shrine bell.
		for (int[] c : new int[][]{{31, 25}, {36, 25}, {31, 30}, {36, 30}}) {
			set(level, bb, s(Blocks.CARPET.pink()), c[0], 1, c[1]);
		}
		fill(level, bb, 33, 4, 27, 34, 4, 27, cherry);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.WEST), 33, 3, 27);
		hipRoof(level, bb, 27, 21, 40, 34, 6, Blocks.CHERRY_STAIRS, cherry, s(Blocks.GOLD_BLOCK));
		chest(level, bb, 38, 1, 32, WildsSites.SAKURA_VAULT, Direction.NORTH, 91L);
	}

	private void cherryTree(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		for (int y = 1; y <= 5; y++) {
			set(level, bb, s(Blocks.CHERRY_LOG), cx, y, cz);
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 4; dy <= 6; dy++) {
					if (dx * dx + dz * dz + (dy - 5) * (dy - 5) <= 6) {
						set(level, bb, s(Blocks.CHERRY_LEAVES).setValue(LeavesBlock.PERSISTENT, true), cx + dx, dy, cz + dz);
					}
				}
			}
		}
	}

	private void spawnVillager(WorldGenLevel level, BoundingBox bb, int x, int y, int z, int role) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		SakuraVillager v = ShrineCatContent.SAKURA_VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (v == null) {
			return;
		}
		v.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		v.configure(role);
		level.addFreshEntityWithPassengers(v);
	}

	private void spawnVillageCat(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ShrineCatRules.Variant variant) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		ShrineCat cat = ShrineCatContent.SHRINE_CAT.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (cat == null) {
			return;
		}
		cat.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		cat.configureVillage(variant, pos);
		level.addFreshEntityWithPassengers(cat);
	}
}
