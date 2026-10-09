package dev.wildercord.pet;

import dev.wildercord.cast.Casters;
import dev.wildercord.cast.Effects;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-only landing and growth checks over the complete physical footprint, including giant Cinnamon. */
final class CompanionLanding {
	private CompanionLanding() {}
	static Vec3 find(ServerLevel level, Vec3 near, CinnamonDog dog) {
		ServerPlayer owner = CinnamonCompanion.ownerOf(dog);
		if (owner == null || owner.level() != level || near.distanceToSqr(owner.position()) > 1) return null;
		Vec3 ownerAt = owner.position(), dogAt = dog.position();
		LevelIdentity identity = new LevelIdentity(dog.level(), dog.getBoundingBox(), dog.getScale(), dog.stateRevision());
		BlockPos center = BlockPos.containing(near);
		for (int radius = 1; radius <= 4; radius++) {
			for (int dy = 1; dy >= -3; dy--) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
				if (!unchanged(owner, level, ownerAt, dog, dogAt, identity)) return null;
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				Vec3 at = Vec3.atBottomCenterOf(center.offset(dx, dy, dz));
				AABB box = dog.getBoundingBox().move(at.subtract(dog.position()));
				if (!safe(level, dog, box)) {
					if (!unchanged(owner, level, ownerAt, dog, dogAt, identity)) return null;
					continue;
				}
				if (!unchanged(owner, level, ownerAt, dog, dogAt, identity)) return null;
				Vec3 eye = owner.getEyePosition(), target = at.add(0, Math.min(0.5, box.getYsize() / 2), 0);
				if (eye.distanceToSqr(target) > 100) return null;
				boolean rayLoaded = true;
				for (int step = 0; step <= 20; step++) if (!level.hasChunkAt(BlockPos.containing(eye.lerp(target, step / 20D)))) { rayLoaded = false; break; }
				if (!rayLoaded) continue;
				// A recall cannot place her through a wall into a neighbouring protected room.
				if (level.clip(new ClipContext(eye, target,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() != HitResult.Type.MISS) continue;
				if (safe(level, dog, box) && unchanged(owner, level, ownerAt, dog, dogAt, identity)
					&& level.clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS) return at;
			}
		}
		return null;
	}
	static boolean safe(ServerLevel level, CinnamonDog dog, AABB box) {
		ServerPlayer owner = CinnamonCompanion.ownerOf(dog);
		if (owner == null || !CinnamonCompanion.mayRecall(owner)
			|| owner.level() != level || !finite(box) || box.getXsize() > 2 || box.getYsize() > 3 || box.getZsize() > 2
			|| !level.getWorldBorder().isWithinBounds(box)) return false;
		Vec3 ownerAt = owner.position(), dogAt = dog.position();
		LevelIdentity identity = new LevelIdentity(dog.level(), dog.getBoundingBox(), dog.getScale(), dog.stateRevision());
		BlockPos min = BlockPos.containing(box.minX + 1e-5, box.minY + 1e-5, box.minZ + 1e-5);
		BlockPos max = BlockPos.containing(box.maxX - 1e-5, box.maxY - 1e-5, box.maxZ - 1e-5);
		// Establish the entire region is resident before any collision/fluid/permission query can visit it.
		for (BlockPos p : BlockPos.betweenClosed(min.offset(-1, -1, -1), max.offset(1, 0, 1))) {
			if (!level.hasChunkAt(p) || level.isOutsideBuildHeight(p)) return false;
		}
		// Refuse impossible geometry before any external claim callback; retain all post-callback checks below.
		for (BlockPos p : BlockPos.betweenClosed(min.below(), new BlockPos(max.getX(), min.getY() - 1, max.getZ())))
			if (!level.getBlockState(p).isFaceSturdy(level, p, Direction.UP)) return false;
		if (!level.noCollision(dog, box) || level.containsAnyLiquid(box)) return false;
		java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> observed = new java.util.HashMap<>();
		for (BlockPos p : BlockPos.betweenClosed(min.below(), max)) {
			var before = level.getBlockState(p);
			observed.put(p.immutable(), before);
			if (danger(before) || !before.getFluidState().isEmpty() || Effects.isTemporary(level, p)
				|| DungeonWards.warded(level, p) || before.hasBlockEntity() || !level.mayInteract(owner, p)) return false;
			if (!Casters.probeBreak(level, owner, p, before, null) || level.getBlockState(p) != before
				|| !CinnamonCompanion.mayRecall(owner) || owner.level() != level) return false;
		}
		// The last external callback may have changed an earlier cell. No callbacks follow this final snapshot pass.
		if (!CinnamonCompanion.mayRecall(owner) || owner.level() != level) return false;
		for (var cell : observed.entrySet()) {
			BlockPos p = cell.getKey();
			if (!level.hasChunkAt(p) || level.getBlockState(p) != cell.getValue() || !level.mayInteract(owner, p)
				|| DungeonWards.warded(level, p) || Effects.isTemporary(level, p)) return false;
		}
		for (BlockPos p : BlockPos.betweenClosed(min.below(), new BlockPos(max.getX(), min.getY() - 1, max.getZ()))) {
			if (!level.getBlockState(p).isFaceSturdy(level, p, Direction.UP)) return false;
		}
		return owner.getUUID().equals(CinnamonCompanion.ownerId(dog)) && owner.position().equals(ownerAt) && dog.position().equals(dogAt) && dog.level() == identity.level
			&& dog.getBoundingBox().equals(identity.box) && dog.getScale() == identity.scale && dog.stateRevision() == identity.revision
			&& level.noCollision(dog, box) && !level.containsAnyLiquid(box);
	}
	private record LevelIdentity(net.minecraft.world.level.Level level, AABB box, float scale, long revision) {}
	/** Source permissions are checked without demanding safe footing: a void/stuck body still needs rescue. */
	static SourcePermit sourcePermit(ServerPlayer owner, ServerLevel level, CinnamonDog dog) {
		AABB box = dog.getBoundingBox();
		if (!finite(box) || box.getXsize() > 2 || box.getYsize() > 3 || box.getZsize() > 2) return null;
		BlockPos min = BlockPos.containing(box.minX + 1e-5, Math.clamp(box.minY, level.getMinY(), level.getMaxY() - 1), box.minZ + 1e-5);
		BlockPos max = BlockPos.containing(box.maxX - 1e-5, Math.clamp(box.maxY - 1e-5, level.getMinY(), level.getMaxY() - 1), box.maxZ - 1e-5);
		java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> observed = new java.util.HashMap<>();
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			if (!level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p) || DungeonWards.warded(level, p)
				|| !level.mayInteract(owner, p)) return null;
			var state = level.getBlockState(p); observed.put(p.immutable(), state);
			if (!Casters.probeBreak(level, owner, p, state, level.getBlockEntity(p))) return null;
		}
		for (var cell : observed.entrySet()) if (!level.hasChunkAt(cell.getKey()) || level.getBlockState(cell.getKey()) != cell.getValue()
			|| DungeonWards.warded(level, cell.getKey()) || !level.mayInteract(owner, cell.getKey())) return null;
		return new SourcePermit(level, java.util.Map.copyOf(observed));
	}
	static record SourcePermit(ServerLevel level, java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> observed) {
		boolean valid(ServerPlayer owner) {
			for (var cell : observed.entrySet()) if (!level.hasChunkAt(cell.getKey()) || level.getBlockState(cell.getKey()) != cell.getValue()
				|| DungeonWards.warded(level, cell.getKey()) || !level.mayInteract(owner, cell.getKey())) return false;
			return true;
		}
	}
	private static boolean unchanged(ServerPlayer owner, ServerLevel level, Vec3 ownerAt, CinnamonDog dog, Vec3 dogAt, LevelIdentity identity) {
		return owner.getUUID().equals(CinnamonCompanion.ownerId(dog)) && owner.level() == level && owner.position().equals(ownerAt) && dog.position().equals(dogAt) && dog.level() == identity.level
			&& dog.getBoundingBox().equals(identity.box) && dog.getScale() == identity.scale && dog.stateRevision() == identity.revision;
	}
	private static boolean finite(AABB b) {
		return Double.isFinite(b.minX) && Double.isFinite(b.minY) && Double.isFinite(b.minZ)
			&& Double.isFinite(b.maxX) && Double.isFinite(b.maxY) && Double.isFinite(b.maxZ);
	}
	private static boolean danger(net.minecraft.world.level.block.state.BlockState s) {
		return s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS)
			|| s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.NETHER_PORTAL)
			|| s.is(Blocks.END_PORTAL) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE);
	}
}
