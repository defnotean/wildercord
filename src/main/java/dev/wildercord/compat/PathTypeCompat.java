package dev.wildercord.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * 1.21.10 has no fabric land-path registry: residue blocks note their path dangers here, and
 * {@code WalkNodeEvaluatorPathTypeMixin} folds them into the walk node evaluator's own answers.
 */
public final class PathTypeCompat {
	private PathTypeCompat() {}

	// Parallel arrays, not a map: compat classes stay free of cache collections (see verify_port).
	private static Block[] blocks = new Block[0];
	private static PathType[] walkDangers = new PathType[0];

	public static synchronized void register(Block block, PathType walkDanger, PathType swimDanger) {
		Block[] nextBlocks = new Block[blocks.length + 1];
		PathType[] nextDangers = new PathType[walkDangers.length + 1];
		System.arraycopy(blocks, 0, nextBlocks, 0, blocks.length);
		System.arraycopy(walkDangers, 0, nextDangers, 0, walkDangers.length);
		nextBlocks[blocks.length] = block;
		nextDangers[walkDangers.length] = walkDanger;
		blocks = nextBlocks;
		walkDangers = nextDangers;
	}

	/** The danger a walking mob should read from this block, or null to leave the vanilla answer alone. */
	public static PathType walkDangerFor(BlockState state) {
		if (state == null) {
			return null;
		}
		Block block = state.getBlock();
		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i] == block) {
				return walkDangers[i];
			}
		}
		return null;
	}
}
