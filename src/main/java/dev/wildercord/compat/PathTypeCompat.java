package dev.wildercord.compat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * 1.21.10 has no fabric land-path registry: residue blocks note their path dangers here, and
 * {@code WalkNodeEvaluatorPathTypeMixin} folds them into the walk node evaluator's own answers.
 */
public final class PathTypeCompat {
	private PathTypeCompat() {}

	private static final Map<Block, PathType> WALK_DANGER = new HashMap<>();

	public static void register(Block block, PathType walkDanger, PathType swimDanger) {
		WALK_DANGER.put(block, walkDanger);
	}

	/** The danger a walking mob should read from this block, or null to leave the vanilla answer alone. */
	public static PathType walkDangerFor(BlockState state) {
		return state == null ? null : WALK_DANGER.get(state.getBlock());
	}
}
