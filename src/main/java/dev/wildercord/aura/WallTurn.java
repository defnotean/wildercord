package dev.wildercord.aura;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** One original body's wall contact and bounded kick. Every accepted stretch sweeps its real standing collision box. */
final class WallTurn {
	private final ServerPlayer player;
	private final ServerLevel level;
	private final ReleasedArtOwner owner;
	private final ItemStack blade;
	private final String method;
	private final Vec3 start, normal;
	private final BlockPos wall;
	private final DungeonWards.MovementWard warded;
	private final long accepted;
	private Vec3 last, direction;
	private double fallRisk;
	private int step;
	private boolean kicking, aborted;

	private WallTurn(ServerPlayer player, BlockHitResult contact) {
		this.player = player; level = player.level(); owner = ReleasedArtOwner.capture(player);
		blade = player.getMainHandItem(); method = Aura.data(player).method();
		start = player.position(); last = start; wall = contact.getBlockPos().immutable();
		normal = Vec3.atLowerCornerOf(contact.getDirection().getUnitVec3i());
		warded = DungeonWards.movementWard(level, player.blockPosition());
		accepted = level.getGameTime(); fallRisk = Math.max(0, player.fallDistance);
	}
	static WallTurn accept(ServerPlayer player) {
		if (!Double.isFinite(player.fallDistance) || player.onGround() || !finite(player.position()) || !finite(player.getDeltaMovement()) || player.isShiftKeyDown()) return null;
		var warded = DungeonWards.movementWard(player.level(), player.blockPosition());
		if (!clear(player, player.getBoundingBox(), warded)) return null;
		BlockHitResult contact = contact(player);
		if (contact == null) return null;
		return new WallTurn(player, contact);
	}
	void brace() { player.setDeltaMovement(Vec3.ZERO); player.needsSync = true; }
	boolean valid() {
		if (aborted || !owner.valid() || player.level() != level || !MasterForms.able(player)) return false;
		// Registered movement predicates may synchronously cancel or retire this exact receipt while able() runs.
		return !aborted && owner.valid() && player.level() == level && player.getMainHandItem() == blade
			&& method.equals(Aura.data(player).method()) && !player.isShiftKeyDown() && !player.onGround()
			&& Double.isFinite(player.fallDistance) && finite(player.position()) && finite(player.getDeltaMovement()) && player.position().distanceToSqr(last) <= .64;
	}
	boolean kick() {
		if (kicking || !valid() || level.getGameTime() <= accepted || level.getGameTime() - accepted >= WallTurnRules.BRACE_TICKS) return false;
		BlockHitResult contact = contact(player);
		if (contact == null || !contact.getBlockPos().equals(wall)) return false;
		Vec3 motion = player.getLastClientMoveIntent();
		if (!finite(motion)) return false;
		Vec3 desired = new Vec3(motion.x, 0, motion.z);
		if (desired.lengthSqr() < .001) desired = normal;
		else desired = desired.normalize();
		// Movement selects the outgoing lane, but every kick must leave the touched face.
		desired = desired.add(normal.scale(Math.max(0, .45 - desired.dot(normal))));
		if (desired.lengthSqr() < .001) return false;
		direction = desired.normalize();
		if (!swept(player, player.position(), point(1), warded)) return false;
		kicking = true;
		return true;
	}
	boolean tick() {
		if (!valid()) return false;
		fallRisk = Math.max(fallRisk, player.fallDistance);
		if (!kicking) {
			if (level.getGameTime() - accepted >= WallTurnRules.BRACE_TICKS) return false;
			BlockHitResult contact = contact(player);
			if (contact == null || !contact.getBlockPos().equals(wall)) return false;
			return move(start);
		}
		return step < WallTurnRules.KICK_TICKS && move(point(++step));
	}
	private Vec3 point(int step) { return start.add(direction.scale(WallTurnRules.distance(step))).add(0, WallTurnRules.rise(step), 0); }
	private boolean move(Vec3 to) {
		Vec3 from = player.position();
		if (!swept(player, from, to, warded)) return false;
		// Minecraft's teleport path may clear fall state. Put back every pre-existing metre plus actual controlled descent.
		fallRisk = Math.max(fallRisk, player.fallDistance) + Math.max(0, from.y - to.y);
		player.teleportTo(level, to.x, to.y, to.z, Relative.ROTATION, 0, 0, false);
		player.fallDistance = Math.max(player.fallDistance, fallRisk);
		player.setDeltaMovement(Vec3.ZERO);
		player.needsSync = true;
		last = to;
		return true;
	}
	void abort() { aborted = true; if (owner.valid()) player.fallDistance = Math.max(player.fallDistance, fallRisk); }
	boolean finished() { return kicking && step >= WallTurnRules.KICK_TICKS; }
	boolean kicking() { return kicking; }
	int remaining() { return WallTurnRules.KICK_TICKS - step; }
	float yaw() {
		Vec3 facing = kicking ? direction : normal.scale(-1);
		return (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
	}
	private static boolean finite(Vec3 v) { return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z); }

	/** A visible, horizontal collision face within reach. A decoration, liquid or opposite wall cannot be claimed by a packet. */
	private static BlockHitResult contact(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 chest = player.position().add(0, Math.min(1.2, player.getBbHeight() * .65), 0);
		BlockHitResult nearest = null;
		for (Direction face : Direction.Plane.HORIZONTAL) {
			Vec3 end = chest.add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(WallTurnRules.REACH));
			if (!loaded(level, new AABB(chest, end).inflate(.01))) continue;
			BlockHitResult hit = level.clip(new ClipContext(chest, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection().getAxis().isVertical() || dangerous(level, hit.getBlockPos())) continue;
			Vec3 visible = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(.01));
			if (!loaded(level, new AABB(player.getEyePosition(), visible).inflate(.01))) continue;
			if (level.clip(new ClipContext(player.getEyePosition(), visible, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) continue;
			if (nearest == null || chest.distanceToSqr(hit.getLocation()) < chest.distanceToSqr(nearest.getLocation())) nearest = hit;
		}
		return nearest;
	}
	/** Conservative swept volumes include every intermediate body and cell, not only an endpoint or a centre ray. */
	static boolean swept(ServerPlayer player, Vec3 from, Vec3 to, DungeonWards.MovementWard warded) {
		if (!finite(from) || !finite(to) || from.distanceToSqr(to) > 4) return false;
		AABB body = player.getBoundingBox();
		int pieces = Math.max(1, (int) Math.ceil(from.distanceTo(to) / WallTurnRules.PROBE));
		AABB previous = body;
		for (int i = 1; i <= pieces; i++) {
			AABB next = body.move(from.lerp(to, i / (double) pieces).subtract(from));
			if (!clear(player, previous.minmax(next), warded)) return false;
			previous = next;
		}
		return true;
	}
	/** Native geometry fixtures can state the expected broad ward state, but unknown or different rooms still refuse. */
	static boolean swept(ServerPlayer player, Vec3 from, Vec3 to, boolean expectedWarded) {
		var ward = DungeonWards.movementWard(player.level(), BlockPos.containing(from));
		return ward.known() && ward.warded() == expectedWarded && swept(player, from, to, ward);
	}
	private static boolean clear(ServerPlayer player, AABB box, DungeonWards.MovementWard warded) {
		ServerLevel level = player.level();
		if (!warded.known() || !loaded(level, box) || !level.getWorldBorder().isWithinBounds(box.minX, box.minZ)
			|| !level.getWorldBorder().isWithinBounds(box.maxX, box.maxZ) || !level.noCollision(player, box)) return false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
			BlockPos.containing(box.maxX - 1.0E-6, box.maxY - 1.0E-6, box.maxZ - 1.0E-6))) {
			if (dangerous(level, pos) || !DungeonWards.movementWard(level, pos).equals(warded)) return false;
		}
		return true;
	}
	private static boolean loaded(ServerLevel level, AABB box) {
		if (!Double.isFinite(box.getSize()) || box.getXsize() > 4 || box.getYsize() > 6 || box.getZsize() > 4) return false;
		if (box.minY < level.getMinY() || box.maxY >= level.getMaxY()) return false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
			if (!level.hasChunkAt(pos)) return false;
		return true;
	}
	private static boolean dangerous(ServerLevel level, BlockPos pos) {
		var state = level.getBlockState(pos);
		return !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
			|| state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)
			|| state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL);
	}
	/** The client on-ground flag alone never refills the airborne use: solid, safe support and real body room are required. */
	static boolean safeLanding(ServerPlayer player) {
		if (MasterFormMovement.occupied(player) || !player.onGround() || !finite(player.position()) || player.isPassenger() || player.getAbilities().flying || player.isFallFlying()
			|| player.getDeltaMovement().y > .01) return false;
		AABB body = player.getBoundingBox();
		var warded = DungeonWards.movementWard(player.level(), player.blockPosition());
		if (!clear(player, body, warded) || !loaded(player.level(), body.move(0, -.08, 0))
			|| player.level().noCollision(player, body.move(0, -.08, 0))) return false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, body.minY - .08, body.minZ),
			BlockPos.containing(body.maxX - 1.0E-6, body.minY - 1.0E-6, body.maxZ - 1.0E-6)))
			if (dangerous(player.level(), pos)) return false;
		return true;
	}
}
