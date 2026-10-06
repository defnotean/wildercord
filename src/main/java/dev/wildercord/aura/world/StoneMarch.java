package dev.wildercord.aura.world;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.party.Parties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One paid, planted action owns all three exact-tick pulses, immutable warning geometry and one attempt per participant. */
final class StoneMarch {
	private final SwordMaster master;
	private final ServerLevel level;
	private final Vec3 origin, aim, side;
	private final long began;
	private final Set<UUID> roster;
	private final Map<UUID, ServerPlayer> bodies;
	private final UUID targetId;
	private final int sequence, initialPrefix;
	private final double preparedAura;
	private final Set<UUID> attempted = new HashSet<>();
	private int prefix, consumed, resolvingPulse = -1;
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private StoneMarch(SwordMaster master, ServerPlayer target, long now, int sequence, double aura, int prefix, Vec3 aim) {
		this.master = master; level = (ServerLevel) master.level(); origin = master.position(); this.aim = aim;
		side = new Vec3(-aim.z, 0, aim.x); began = now; roster = master.challengers(); targetId = target.getUUID();
		this.sequence = sequence; preparedAura = aura; this.prefix = initialPrefix = prefix;
		var acceptedBodies = new HashMap<UUID, ServerPlayer>();
		for (UUID id : roster) if (level.getPlayerByUUID(id) instanceof ServerPlayer player) acceptedBodies.put(id, player);
		bodies = Map.copyOf(acceptedBodies);
	}

	/** Read-only preparation never reserves, pays, moves, warns or changes the ordinary planner. */
	static StoneMarch prepare(SwordMaster master, ServerPlayer target, int school, int sequence, double aura, long now, long readyAt) {
		if (!(master.level() instanceof ServerLevel level) || now != level.getGameTime() || !master.canBeginMarch(now)
			|| !lawful(master, target) || !master.hasLineOfSight(target) || !master.onGround() || !still(master)
			|| !StoneMarchRules.eligible(school, sequence, master.distanceTo(target), target.getY() - master.getY(), aura, now, readyAt)
			|| master.challengers().isEmpty() || master.challengers().size() > MastersRules.MAX_PARTICIPANTS) return null;
		Vec3 aim = target.position().subtract(master.position()).multiply(1, 0, 1).normalize();
		if (!Double.isFinite(aim.x) || !Double.isFinite(aim.z) || aim.lengthSqr() < .999) return null;
		StoneMarch opening = new StoneMarch(master, target, now, sequence, aura, StoneMarchRules.BANDS, aim);
		opening.prefix = opening.supportedPrefix(StoneMarchRules.BANDS);
		if (opening.prefix == 0 || !opening.ownerGround() || !opening.validRoster(Set.of())
			|| opening.band(target) < 0 || opening.band(target) >= opening.prefix || !opening.escapesClear()) return null;
		return new StoneMarch(master, target, now, sequence, aura, opening.prefix, aim);
	}

	/** The caller rechecks higher priorities, then revalidates this exact proposal immediately before its single debit. */
	boolean ready(ServerPlayer target, int currentSequence, double aura, long now, long readyAt) {
		return now == began && now == level.getGameTime() && !finished && currentSequence == sequence && aura == preparedAura
			&& target.getUUID().equals(targetId) && master.canBeginMarch(now) && master.level() == level
			&& master.position().equals(origin) && master.onGround() && still(master) && master.hasLineOfSight(target)
			&& StoneMarchRules.eligible(MastersRules.STONE, currentSequence, master.distanceTo(target), target.getY() - origin.y, aura, now, readyAt)
			&& validRoster(Set.of()) && ownerGround() && supportedPrefix(initialPrefix) == initialPrefix
			&& band(target) >= 0 && band(target) < prefix && escapesClear();
	}

	boolean tick(long now) {
		if (finished) return false;
		// A pulse is consumed before callbacks. Re-entry at this tick cannot add victims or run a later band.
		if (now == lastTick) return true;
		if (now != level.getGameTime() || now < began || now > began + StoneMarchRules.THIRD
			|| (lastTick == Long.MIN_VALUE ? now != began : now != lastTick + 1)
			|| !validOwner(Set.of()) || !refreshGeometry()) return cancel();
		lastTick = now;
		plant();
		int pulse = StoneMarchRules.pulse(now - began);
		if (pulse < 0) {
			if ((now - began) % StoneMarchRules.WARNING_REFRESH == 0) draw(-1, now, false);
			return true;
		}
		consumed |= 1 << pulse;
		resolvingPulse = pulse;
		draw(pulse, now, false);
		master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Feels.sound(level, origin.add(aim.scale(StoneMarchRules.start(pulse))), "aura_slash", 1, .6F + pulse * .08F);
		// Preserve Kiln's simultaneous-pulse fairness: callbacks may remove a snapshot victim, never add one.
		List<ServerPlayer> victims = roster.stream().sorted(Comparator.naturalOrder()).map(bodies::get)
			.filter(player -> player != null && canHit(player, pulse)).toList();
		Set<UUID> defeated = new HashSet<>();
		for (ServerPlayer player : victims) {
			UUID id = player.getUUID();
			if (!validOwner(defeated) || !refreshGeometry()) return cancel();
			if (level.getPlayerByUUID(id) == player && canHit(player, pulse) && attempted.add(id)) {
				Effects.withSource(master, () -> master.projected(player,
					MastersRules.damage(master.challengerCount(), MastersRules.STONE, MastersRules.Move.STONE_FAULT_MARCH)));
				if (!player.isAlive() && player.level() == level) defeated.add(id);
			}
		}
		if (!validOwner(defeated) || !refreshGeometry()) return cancel();
		resolvingPulse = -1;
		if (finished) return false;
		if (pulse == StoneMarchRules.BANDS - 1) { finished = released = true; return false; }
		return true;
	}

	private boolean validOwner(Set<UUID> defeated) {
		return !finished && master.canMaintainMarch(this) && master.level() == level && validRoster(defeated)
			&& master.position().distanceToSqr(origin) <= .0025 && master.onGround() && still(master);
	}
	private boolean validRoster(Set<UUID> defeated) {
		return bodies.size() == roster.size() && roster.equals(master.challengers())
			&& roster.stream().allMatch(id -> level.getPlayerByUUID(id) instanceof ServerPlayer player && bodies.get(id) == player
			&& (lawful(master, player) || defeated.contains(id) && !player.isAlive() && player.level() == level));
	}
	private static boolean lawful(SwordMaster master, ServerPlayer player) {
		return master.canHarmParticipant(player) && Targets.canHarm(master, player) && !Parties.blocksHarm(master, player);
	}
	private int band(ServerPlayer player) {
		Vec3 delta = player.position().subtract(origin);
		return StoneMarchRules.band(delta.dot(aim), delta.dot(side), delta.y);
	}
	private boolean canHit(ServerPlayer player, int pulse) {
		return bodies.get(player.getUUID()) == player && level.getPlayerByUUID(player.getUUID()) == player
			&& pulse < prefix && !attempted.contains(player.getUUID()) && lawful(master, player) && band(player) == pulse
			&& EmberAfterburn.clear(level, master, origin.add(0, .35, 0), player.position().add(0, .35, 0));
	}
	private boolean refreshGeometry() {
		prefix = Math.min(prefix, supportedPrefix(prefix));
		return prefix > 0 && ownerGround() && escapesClear();
	}

	/** The supported and uncovered prefix can only shrink. A removed gap or thin obstacle never restores a band. */
	private int supportedPrefix(int limit) {
		for (int band = 0; band < limit; band++) {
			double low = band == 0 ? 0 : StoneMarchRules.start(band), high = StoneMarchRules.end(band);
			AABB volume = rectangle(low, high, StoneMarchRules.HALF_WIDTH, .02, StoneMarchRules.HIGH + .1);
			if (!master.pursuitInsideArena(volume) || !resident(volume) || !floorSupported(volume, low, high, StoneMarchRules.HALF_WIDTH)) return band;
			int boxes = 0;
			for (var shape : level.getBlockCollisions(master, volume)) for (AABB box : shape.toAabbs()) {
				if (++boxes > StoneMarchRules.MAX_COVER_BOXES || overlaps(box, low, high, StoneMarchRules.HALF_WIDTH)) return band;
			}
		}
		return limit;
	}

	/** Each currently threatened body has a flat, resident, unobstructed lateral route outside the fixed lane. */
	private boolean escapesClear() {
		for (UUID id : roster) {
			if (!(level.getPlayerByUUID(id) instanceof ServerPlayer player) || !player.isAlive()) continue;
			int band = band(player);
			if (band < 0 || band >= prefix || ((consumed & 1 << band) != 0 && band != resolvingPulse) || attempted.contains(id)) continue;
			Vec3 delta = player.position().subtract(origin);
			boolean clear = false;
			for (int sign : new int[] {-1, 1}) {
				double radius = (player.getBoundingBox().getXsize() + player.getBoundingBox().getZsize()) * .5;
				double shift = sign * (StoneMarchRules.HALF_WIDTH + radius + StoneMarchRules.ESCAPE_MARGIN) - delta.dot(side);
				AABB corridor = player.getBoundingBox().minmax(player.getBoundingBox().move(side.scale(shift))).inflate(.1, 0, .1);
				if (master.pursuitInsideArena(corridor) && resident(corridor) && hazardFree(corridor) && flatFloor(corridor)
					&& !level.getBlockCollisions(master, corridor).iterator().hasNext()
					&& level.getEntities(master, corridor, entity -> entity instanceof LivingEntity living && living.isAlive()
						&& !entity.isSpectator() && !entity.getUUID().equals(id)).isEmpty()) { clear = true; break; }
			}
			if (!clear) return false;
		}
		return true;
	}

	private boolean ownerGround() { return resident(master.getBoundingBox()) && hazardFree(master.getBoundingBox()) && flatFloor(master.getBoundingBox()); }
	private boolean flatFloor(AABB body) { return floorSupported(body, 0, 0, -1); }
	private boolean floorSupported(AABB area, double low, double high, double width) {
		if (!Double.isFinite(origin.y) || Math.abs(origin.y - Math.rint(origin.y)) > .01) return false;
		int y = (int) Math.rint(origin.y) - 1;
		for (int x = (int) Math.floor(area.minX); x <= (int) Math.floor(area.maxX); x++)
			for (int z = (int) Math.floor(area.minZ); z <= (int) Math.floor(area.maxZ); z++) {
				if (width >= 0 && !StoneMarchRules.overlaps(x - origin.x, x + 1 - origin.x, z - origin.z, z + 1 - origin.z, aim.x, aim.z, low, high, width)) continue;
				BlockPos floor = new BlockPos(x, y, z);
				if (!level.hasChunkAt(floor) || level.isOutsideBuildHeight(floor)) return false;
				var state = level.getBlockState(floor);
				if (!level.hasChunkAt(floor) || !state.isFaceSturdy(level, floor, Direction.UP)
					|| !level.getFluidState(floor).isEmpty() || !level.getFluidState(floor.above()).isEmpty()
					|| hazardous(state) || hazardous(level.getBlockState(floor.above()))) return false;
			}
		return true;
	}
	/** Collision-free flowers, fluids and upper-body hazards still make an escape unsafe. */
	private boolean hazardFree(AABB body) {
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(body.minX, origin.y - .05, body.minZ),
			BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
			if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return false;
			var state = level.getBlockState(pos);
			if (!state.getFluidState().isEmpty() || hazardous(state)) return false;
		}
		return true;
	}
	private static boolean hazardous(net.minecraft.world.level.block.state.BlockState state) {
		return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
			|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW)
			|| state.is(Blocks.POINTED_DRIPSTONE);
	}
	private boolean resident(AABB area) {
		if (!level.getWorldBorder().isWithinBounds(area.minX, area.minZ) || !level.getWorldBorder().isWithinBounds(area.maxX, area.maxZ)) return false;
		for (int x = StoneMarchRules.collisionMin(area.minX); x <= StoneMarchRules.collisionMax(area.maxX); x++)
			for (int z = StoneMarchRules.collisionMin(area.minZ); z <= StoneMarchRules.collisionMax(area.maxZ); z++)
				if (!level.hasChunkAt(BlockPos.containing(x, origin.y, z))) return false;
		return true;
	}
	private boolean overlaps(AABB box, double low, double high, double width) {
		return StoneMarchRules.overlaps(box.minX - origin.x, box.maxX - origin.x, box.minZ - origin.z, box.maxZ - origin.z, aim.x, aim.z, low, high, width);
	}
	private AABB rectangle(double low, double high, double width, double bottom, double top) {
		Vec3 a = point(low, -width), b = point(low, width), c = point(high, -width), d = point(high, width);
		return new AABB(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)), origin.y + bottom,
			Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z)), Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)), origin.y + top,
			Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z)));
	}
	private Vec3 point(double forward, double lateral) { return origin.add(aim.scale(forward)).add(side.scale(lateral)).add(0, .12, 0); }
	private static boolean still(SwordMaster master) {
		Vec3 velocity = master.getDeltaMovement();
		return Double.isFinite(velocity.x) && Double.isFinite(velocity.y) && Double.isFinite(velocity.z)
			&& velocity.horizontalDistanceSqr() <= .0025 && Math.abs(velocity.y) <= .1;
	}
	private void plant() {
		master.getNavigation().stop(); master.getMoveControl().setWait(); master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(origin.x + aim.x * 8, master.getEyeY(), origin.z + aim.z * 8);
	}
	private void draw(int pulse, long now, boolean cancelled) {
		for (int band = 0; band < initialPrefix; band++) {
			boolean spent = cancelled || band >= prefix || (consumed & 1 << band) != 0;
			boolean impact = band == pulse && band < prefix;
			int life = cancelled || now >= began + StoneMarchRules.THIRD ? StoneMarchRules.RECOVERY
				: StoneMarchRules.WARNING_REFRESH + 1;
			int color = spent ? 0x73E2CB : master.auraColor();
			double low = StoneMarchRules.start(band), high = StoneMarchRules.end(band), width = StoneMarchRules.HALF_WIDTH;
			Vec3 a = point(low, -width), b = point(low, width), c = point(high, -width), d = point(high, width);
			for (Vec3[] edge : new Vec3[][] {{a, b}, {a, c}, {b, d}, {c, d}}) Light.ray(level, edge[0], edge[1], color, .055, life);
			if (impact) Light.ray(level, point((low + high) * .5, -width), point((low + high) * .5, width), master.auraColor(), .14, 5);
			if (!spent) {
				// One/two/three centered crossbars identify pulse order even when reduced effects suppress particles.
				for (int mark = 1; mark <= band + 1; mark++) {
					double at = low + StoneMarchRules.LENGTH * mark / (band + 2);
					Light.ray(level, point(at, -.45), point(at, .45), color, .075, life);
				}
			}
		}
	}
	boolean released() { return released; }
	long endsAt() { return began + StoneMarchRules.END; }
	void stop() {
		if (finished) return;
		finished = true;
		draw(-1, level.getGameTime(), true);
	}
	private boolean cancel() { stop(); return false; }
}
