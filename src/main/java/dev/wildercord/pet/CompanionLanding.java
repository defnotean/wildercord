package dev.wildercord.pet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Small, loaded-area search; never generates a chunk to bring a companion back. */
final class CompanionLanding {
	private CompanionLanding() {}
	static Vec3 find(ServerLevel level, Vec3 near, CinnamonDog dog) {
		BlockPos center = BlockPos.containing(near);
		for (int radius = 0; radius <= 4; radius++) {
			for (int dy = 1; dy >= -3; dy--) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				BlockPos feet = center.offset(dx, dy, dz);
				if (!level.hasChunkAt(feet) || feet.getY() <= level.getMinY() || feet.getY() >= level.getMaxY()) continue;
				Vec3 at = Vec3.atBottomCenterOf(feet);
				var box = dog.getBoundingBox().move(at.subtract(dog.position()));
				if (!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(dog, box)) continue;
				var floor = level.getBlockState(feet.below());
				if (floor.getCollisionShape(level, feet.below()).isEmpty()) continue;
				boolean danger = false;
				for (BlockPos p : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, 1, 1))) {
					var state = level.getBlockState(p);
					if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)
						|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)
						|| state.is(Blocks.NETHER_PORTAL) || state.getFluidState().is(FluidTags.LAVA)) { danger = true; break; }
				}
				if (!danger && level.getFluidState(feet).isEmpty()) return at;
			}
		}
		return null;
	}
}
