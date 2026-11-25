package dev.wildercord.world.sites.trials;

import dev.wildercord.trials.CoopTrialContent;
import dev.wildercord.trials.KharvokRules;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * 41x32x41 warded Ashen Crucible foundry colosseum housing Kharvok, four interactive Crucible Vents,
 * smokestack bastion towers, chained foundry lanterns, and a rear Trial Treasury.
 */
public final class AshenCruciblePiece extends DungeonPiece implements TrialArenaPiece {
	public static final int SIZE = 41;
	public static final int HEIGHT = 32;
	public static final int FLOOR = 6;

	public AshenCruciblePiece(int x, int y, int z, Direction facing) {
		super(TrialSites.ASHEN_CRUCIBLE, x, y, z, SIZE, HEIGHT, SIZE, facing);
	}

	public AshenCruciblePiece(CompoundTag tag) {
		super(TrialSites.ASHEN_CRUCIBLE, tag);
	}

	@Override
	public String trialKind() {
		return "crucible";
	}

	@Override
	public String structureId() {
		return TrialSites.CRUCIBLE_ID;
	}

	@Override
	public BlockPos gatePosition() {
		return localPosition(20, FLOOR + 1, 3);
	}

	@Override
	public BlockPos arenaCenter() {
		return localPosition(20, FLOOR + 1, 20);
	}

	@Override
	public BlockPos treasuryPosition() {
		return localPosition(20, FLOOR + 1, 36);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(0, FLOOR, 0, SIZE - 1, HEIGHT - 1, SIZE - 1));
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		var piece = new AshenCruciblePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(),
			Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos center = piece.localPosition(20, 0, 20);
		int sea = c.chunkGenerator().getSeaLevel();
		int surface = surface(c, center);
		if (surface <= sea) return Optional.empty();
		int minH = surface, maxH = surface;
		for (int[] off : new int[][]{{-14, -14}, {14, -14}, {-14, 14}, {14, 14}}) {
			int h = surface(c, center.offset(off[0], 0, off[1]));
			if (h <= sea - 2) return Optional.empty();
			minH = Math.min(minH, h);
			maxH = Math.max(maxH, h);
		}
		if (maxH - minH > 6) return Optional.empty();
		piece.move(0, minH - FLOOR, 0);
		return Optional.of(new Structure.GenerationStub(piece.arenaCenter(), b -> b.addPiece(piece)));
	}

	private static int surface(Structure.GenerationContext c, BlockPos pos) {
		return c.chunkGenerator().getFirstOccupiedHeight(pos.getX(), pos.getZ(),
			Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
		BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);

		BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		BlockState cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		BlockState basalt = Blocks.POLISHED_BASALT.defaultBlockState();
		BlockState gold = Blocks.GILDED_BLACKSTONE.defaultBlockState();
		BlockState shroom = Blocks.SHROOMLIGHT.defaultBlockState();
		BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
		BlockState grateGlass = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
		BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
		Block orangeBanner = Blocks.WALL_BANNER.orange();
		Block redBanner = Blocks.WALL_BANNER.red();

		// Circular foundry floor with safe under-glass magma channels so combat footing stays smooth
		for (int x = 0; x < SIZE; x++) {
			for (int z = 0; z < SIZE; z++) {
				int dx = x - 20;
				int dz = z - 20;
				double r = Math.hypot(dx, dz);
				if (r <= 20.5) {
					fill(level, bb, x, 0, z, x, FLOOR - 2, z, blackstone);
					boolean channel = (Math.abs(dx) == 1 || Math.abs(dz) == 1) && r < 18.0 && r > 3.5;
					if (channel) {
						set(level, bb, magma, x, FLOOR - 1, z);
						set(level, bb, grateGlass, x, FLOOR, z);
					} else {
						set(level, bb, basalt, x, FLOOR - 1, z);
						BlockState top = Math.abs(r - 12.0) < 0.9 ? shroom : (r > 17.5 ? gold : (noise(x, z) < 18 ? cracked : blackstone));
						set(level, bb, top, x, FLOOR, z);
					}
					fill(level, bb, x, FLOOR + 1, z, x, HEIGHT - 2, z, AIR);
					if (r >= 19.2 && !(Math.abs(dx) <= 2 && z <= 4)) {
						fill(level, bb, x, FLOOR + 1, z, x, FLOOR + 5, z, blackstone);
						set(level, bb, gold, x, FLOOR + 6, z);
					}
				}
			}
		}

		// Stepped foundry gatehouse & Trial Gate pedestal
		for (int step = 0; step < 3; step++) {
			int z = step;
			int h = FLOOR - 2 + step;
			fill(level, bb, 18, 0, z, 22, h - 1, z, blackstone);
			for (int x = 18; x <= 22; x++) {
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, false), x, h, z);
			}
		}
		for (int px : new int[]{16, 24}) {
			fill(level, bb, px - 1, FLOOR + 1, 0, px + 1, FLOOR + 10, 3, blackstone);
			pillar(level, bb, px, 0, FLOOR + 1, FLOOR + 12, gold, basalt, gold);
			set(level, bb, shroom, px, FLOOR + 13, 0);
		}
		fill(level, bb, 17, FLOOR + 9, 1, 23, FLOOR + 10, 2, blackstone);
		fill(level, bb, 18, FLOOR + 11, 1, 22, FLOOR + 11, 2, gold);
		hangingLantern(level, bb, 18, FLOOR + 8, 2, 1, false);
		hangingLantern(level, bb, 22, FLOOR + 8, 2, 1, false);
		banner(level, bb, orangeBanner, 17, FLOOR + 5, 4, Direction.NORTH);
		banner(level, bb, redBanner, 23, FLOOR + 5, 4, Direction.NORTH);
		set(level, bb, CoopTrialContent.TRIAL_GATE.defaultBlockState(), 20, FLOOR + 1, 3);

		// Smokestack bastion piers & hanging chains in outer perimeter
		for (int[] sp : new int[][]{{4, 13}, {4, 27}, {36, 13}, {36, 27}, {13, 36}, {27, 36}}) {
			pillar(level, bb, sp[0], sp[1], FLOOR + 1, FLOOR + 14, gold, basalt, gold);
			set(level, bb, shroom, sp[0], FLOOR + 15, sp[1]);
		}
		for (int[] ch : new int[][]{{6, 6}, {34, 6}, {6, 34}, {34, 34}}) {
			fill(level, bb, ch[0], FLOOR + 12, ch[1], ch[0], FLOOR + 20, ch[1], basalt);
			set(level, bb, shroom, ch[0], FLOOR + 16, ch[1]);
			fill(level, bb, ch[0], FLOOR + 21, ch[1], ch[0], FLOOR + 22, ch[1], chain);
			hangingLantern(level, bb, ch[0], FLOOR + 11, ch[1], 1, false);
		}

		// Four interactive Crucible Vent pylons around the arena
		for (int[] off : KharvokRules.VENT_OFFSETS) {
			int px = 20 + off[0];
			int pz = 20 + off[1];
			set(level, bb, gold, px, FLOOR + 1, pz);
			set(level, bb, CoopTrialContent.CRUCIBLE_VENT.defaultBlockState(), px, FLOOR + 2, pz);
			set(level, bb, shroom, px, FLOOR + 3, pz);
		}

		// Rear Treasury shrine & instanced Trial Treasury block
		fill(level, bb, 18, FLOOR + 1, 35, 22, FLOOR + 1, 38, basalt);
		set(level, bb, shroom, 18, FLOOR + 2, 37);
		set(level, bb, shroom, 22, FLOOR + 2, 37);
		set(level, bb, CoopTrialContent.TRIAL_TREASURY.defaultBlockState(), 20, FLOOR + 1, 36);
		chest(level, bb, 21, FLOOR + 1, 36, TrialSites.CRUCIBLE_TREASURY_LOOT, Direction.SOUTH, 20261126);
	}
}
