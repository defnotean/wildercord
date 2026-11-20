package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.monster.MonsterContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The Lantern Bathhouse: a steaming hinoki-cypress onsen and spirit shrine with warm magma-heated
 * pools, lantern bridges, hovering {@code PaperLanternSpirit} and {@code KarakuriSentinel} guardians,
 * and a sealed inner vault unlocked by Fire &amp; Frost / Wind spells.
 */
public final class LanternBathhousePiece extends DungeonPiece {
	public static final int WIDTH = 39, HEIGHT = 18, DEPTH = 48;
	public static final int F = 4;

	private static final BlockState SPRUCE = Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState DEEPSLATE = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();

	public LanternBathhousePiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.LANTERN_BATHHOUSE_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public LanternBathhousePiece(CompoundTag tag) {
		super(DungeonWorldgen.LANTERN_BATHHOUSE_PIECE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var piece = new LanternBathhousePiece(c.chunkPos().getMinBlockX(), 0, c.chunkPos().getMinBlockZ(),
			Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen = c.chunkGenerator();
		int sea = gen.getSeaLevel();
		int high = Integer.MIN_VALUE, low = Integer.MAX_VALUE;
		for (int[] p : new int[][]{{19, 22}, {6, 10}, {32, 10}, {6, 38}, {32, 38}}) {
			var wp = piece.getWorldPos(p[0], 0, p[1]);
			int surface = gen.getFirstOccupiedHeight(wp.getX(), wp.getZ(), Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
			int floor = gen.getFirstOccupiedHeight(wp.getX(), wp.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState());
			if (surface <= sea + 1 || surface - floor > 2) return Optional.empty();
			high = Math.max(high, floor);
			low = Math.min(low, floor);
		}
		if (high - low > 8) return Optional.empty();
		int ground = (high + low) / 2;
		piece.move(0, ground - 1 - F, 0);
		var entry = piece.getWorldPos(19, F + 1, 2);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entry.getX(), ground, entry.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(
			worldBox(4, F - 1, 8, 34, F + 8, 34),
			worldBox(12, F, 34, 26, F + 7, 42)
		);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		for (int x = 2; x <= 36; x++) {
			for (int z = 0; z <= 44; z++) {
				fill(level, bb, x, 0, z, x, F - 1, z, DEEPSLATE);
				set(level, bb, Blocks.GRASS_BLOCK.defaultBlockState(), x, F, z);
				fill(level, bb, x, F + 1, z, x, HEIGHT - 1, z, AIR);
			}
		}
		// Stone walk from entry to bathhouse gate.
		fill(level, bb, 16, F, 0, 22, F, 8, Blocks.POLISHED_ANDESITE.defaultBlockState());
		// Main Onsen Hall framed in stripped cypress logs with shoji clerestory panels & entrance lanterns.
		room(level, bb, 4, F, 8, 34, F + 8, 34, DEEPSLATE);
		BlockState cypress = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
		BlockState shoji = Blocks.CALCITE.defaultBlockState();
		for (int y = F + 1; y <= F + 7; y++) {
			for (int x = 4; x <= 34; x++) {
				for (int z = 8; z <= 34; z++) {
					boolean onX = x == 4 || x == 34;
					boolean onZ = z == 8 || z == 34;
					if (!onX && !onZ) {
						continue;
					}
					boolean pillar = (onX && (z - 8) % 5 == 0) || (onZ && (x - 4) % 5 == 0) || (onX && onZ);
					if (pillar) {
						set(level, bb, cypress, x, y, z);
					} else if (y >= F + 3 && y <= F + 5) {
						set(level, bb, shoji, x, y, z);
					}
				}
			}
		}
		fill(level, bb, 5, F, 9, 33, F, 33, SPRUCE);
		fill(level, bb, 17, F + 1, 8, 21, F + 4, 8, AIR);
		for (int x : new int[]{15, 23}) {
			fill(level, bb, x, F + 1, 7, x, F + 4, 7, cypress);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, F + 5, 7);
		}
		// Left and right steaming soaking basins with magma/soul-sand under water.
		for (int z = 12; z <= 30; z++) {
			for (int x = 7; x <= 14; x++) {
				set(level, bb, (x + z) % 3 == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : POLISHED, x, F - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F, z);
			}
			for (int x = 24; x <= 31; x++) {
				set(level, bb, (x + z) % 3 == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : POLISHED, x, F - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F, z);
			}
		}
		// Cypress pillars & hanging lanterns.
		for (int z : new int[]{14, 21, 28}) {
			for (int x : new int[]{16, 22}) {
				fill(level, bb, x, F + 1, z, x, F + 6, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
				set(level, bb, Blocks.LANTERN.defaultBlockState(), x, F + 7, z);
			}
		}
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 19, F + 7, 21);
		chest(level, bb, 6, F + 1, 10, DungeonWorldgen.LANTERN_BATHHOUSE_HALL, Direction.EAST, 101);

		// Sealed Vault behind Fire + Wind RuneSeal door at z = 34.
		room(level, bb, 12, F, 34, 26, F + 7, 42, DEEPSLATE);
		fill(level, bb, 13, F, 35, 25, F, 41, POLISHED);
		sealDoorAcross(level, bb, 34, 17, 21, F + 1, F + 4, RuneSealBlock.Element.FIRE, RuneSealBlock.Element.WIND);
		chest(level, bb, 17, F + 1, 40, DungeonWorldgen.LANTERN_BATHHOUSE_VAULT, Direction.NORTH, 202);
		chest(level, bb, 21, F + 1, 40, DungeonWorldgen.LANTERN_BATHHOUSE_VAULT, Direction.NORTH, 303);

		hipRoof(level, bb, 4, 8, 34, 34, F + 9, Blocks.DARK_OAK_STAIRS, DEEPSLATE, POLISHED);

		if (Config.get().monsters().spawnRate() > 0) {
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 10, F + 1, 16);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 28, F + 1, 24);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 19, F + 1, 26);
		}
	}
}
