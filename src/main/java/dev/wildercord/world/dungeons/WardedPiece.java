package dev.wildercord.world.dungeons;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * A dungeon piece with rooms that can't be broken into: its boss arena and its vault. The halls on the way
 * may be dug into, but the fight and its reward are reached only through the dungeon (see {@link DungeonWards}).
 */
public interface WardedPiece {
	/** The warded rooms, their walls, floors and ceilings included, in world coordinates. */
	List<BoundingBox> wardedBoxes();
}
