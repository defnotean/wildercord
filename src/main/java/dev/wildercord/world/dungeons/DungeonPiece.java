package dev.wildercord.world.dungeons;

import dev.wildercord.cast.Runebound;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.content.dungeons.DungeonBlocks;
import dev.wildercord.spell.RuneDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * What the dimension dungeons' pieces share with the Archive's: each is one piece laid out in its
 * own coordinates (x across, y up, z inward from the entrance) and turned to face a random way, and
 * every choice is a pure function of the spot, so each chunk builds its part the same way whatever
 * order chunks generate in. In a piece's own frame NORTH points inward (+z), SOUTH back out toward
 * the entrance, EAST along +x and WEST along -x.
 */
public abstract class DungeonPiece extends ScatteredFeaturePiece {
	protected static final BlockState AIR = Blocks.AIR.defaultBlockState();

	protected DungeonPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, x, y, z, width, height, depth, facing);
	}

	protected DungeonPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
	}

	// ------------------------------------------------------------------ blocks

	protected void fill(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		generateBox(level, bb, x0, y0, z0, x1, y1, z1, state, state, false);
	}

	/** A hollow box: {@code wall} on every face (floor and ceiling too), air inside. */
	protected void room(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall) {
		generateBox(level, bb, x0, y0, z0, x1, y1, z1, wall, AIR, false);
	}

	protected void set(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int y, int z) {
		placeBlock(level, state, x, y, z, bb);
	}

	/** A deterministic 0-99 for a spot, so the same column always gets the same variation. */
	protected static int noise(int x, int z) {
		int h = x * 73856093 ^ z * 19349663;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return Math.floorMod(h, 100);
	}

	protected static int noise(int x, int y, int z) {
		return noise(x * 31 + y * 7, z * 13 - y);
	}

	// ------------------------------------------------------------------ doors, chests, guards, the altar

	/**
	 * A door of Rune Seals across the piece at {@code z}, {@code x0}-{@code x1} wide and {@code y0}-{@code y1}
	 * high, checkered with two elements.
	 */
	protected void sealDoorAcross(WorldGenLevel level, BoundingBox bb, int z, int x0, int x1, int y0, int y1, RuneSealBlock.Element a,
			RuneSealBlock.Element b) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				set(level, bb, seal((x + y) % 2 == 0 ? a : b), x, y, z);
			}
		}
	}

	/** The same, set in a wall that runs along z (a door in a side wall, at {@code x}). */
	protected void sealDoorAlong(WorldGenLevel level, BoundingBox bb, int x, int z0, int z1, int y0, int y1, RuneSealBlock.Element a,
			RuneSealBlock.Element b) {
		for (int z = z0; z <= z1; z++) {
			for (int y = y0; y <= y1; y++) {
				set(level, bb, seal((z + y) % 2 == 0 ? a : b), x, y, z);
			}
		}
	}

	private static BlockState seal(RuneSealBlock.Element element) {
		return WildercordBlocks.RUNE_SEAL.defaultBlockState().setValue(RuneSealBlock.ELEMENT, element);
	}

	/** A chest facing {@code facing} (in the piece's frame), filled from {@code loot}. */
	protected void chest(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ResourceKey<LootTable> loot, Direction facing, long salt) {
		RandomSource random = RandomSource.create(boundingBox.minX() * 31L + boundingBox.minZ() * 17L + salt);
		BlockState state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
		// Turned with the piece, as every other block is (a chest given a state isn't, otherwise).
		createChest(level, bb, random, getWorldPos(x, y, z), loot, state.mirror(getMirror()).rotate(getRotation()));
	}

	/** The altar its boss rises from. */
	protected void altar(WorldGenLevel level, BoundingBox bb, DungeonAltarBlock.Kind kind, int x, int y, int z) {
		set(level, bb, DungeonBlocks.ALTAR.defaultBlockState().setValue(DungeonAltarBlock.KIND, kind), x, y, z);
	}

	/** A Runebound guard carrying {@code spell}, placed once: by the chunk its feet are in. */
	protected void guard(WorldGenLevel level, BoundingBox bb, EntityType<? extends Mob> type, int x, int y, int z, List<RuneDef> spell, boolean adept) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		Runebound.bindAtGeneration(mob, spell, adept);
		level.addFreshEntityWithPassengers(mob);
	}

	/** A plain (not Runebound) guard, placed the same way. */
	protected void sentry(WorldGenLevel level, BoundingBox bb, EntityType<? extends Mob> type, int x, int y, int z) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntityWithPassengers(mob);
	}

	/** The distance from a column to a circle's centre. */
	protected static double dist(int x, int z, int cx, int cz) {
		return Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
	}

	/** Whether a column lies on one of {@code spokes} straight lines out from a centre (a star map's rays, a forge's spokes). */
	protected static boolean onSpoke(int x, int z, int cx, int cz, int spokes, double width) {
		double a = Math.atan2(z - cz, x - cx);
		double r = dist(x, z, cx, cz);
		double step = Math.PI * 2 / spokes;
		double off = Math.abs(Math.IEEEremainder(a, step));
		return r * Math.sin(Math.min(off, Math.PI / 2)) < width;
	}
}
