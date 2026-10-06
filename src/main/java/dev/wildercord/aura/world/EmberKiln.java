package dev.wildercord.aura.world;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One owned, stationary pulse. Bodies constrain admission/escape; this area strike never uses projectile interception. */
final class EmberKiln {
	private record Escape(UUID player, AABB corridor) {}
	private final SwordMaster master;
	private final ServerLevel level;
	private final Vec3 origin;
	private final float yaw;
	private final long began;
	private final Set<UUID> roster;
	private final List<Escape> escapes;
	private final boolean[] sectors;
	private final Set<UUID> struck = new HashSet<>();
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private EmberKiln(SwordMaster master, ServerLevel level, long now, List<Escape> escapes, boolean[] sectors, float yaw) {
		this.master = master; this.level = level; began = now; origin = master.position(); this.yaw = yaw;
		roster = master.challengers(); this.escapes = List.copyOf(escapes); this.sectors = sectors.clone();
	}

	static EmberKiln prepare(SwordMaster master, ServerPlayer target, int school, int sequence, double aura, long now, long readyAt) {
		if (!(master.level() instanceof ServerLevel level) || !master.canBeginKiln(now) || !master.canHarmParticipant(target)
			|| !master.hasLineOfSight(target) || !master.onGround() || !still(master)
			|| !EmberKilnRules.eligible(school, sequence, master.distanceTo(target), target.getY() - master.getY(), aura, now, readyAt)
			|| !supported(level, master.position())) return null;
		Vec3 origin = master.position();
		boolean[] sectors = new boolean[EmberKilnRules.SECTORS];
		java.util.Arrays.fill(sectors, true);
		int boxes = 0;
		AABB volume = new AABB(origin.x - EmberKilnRules.OUTER, origin.y + .02, origin.z - EmberKilnRules.OUTER,
			origin.x + EmberKilnRules.OUTER, origin.y + EmberKilnRules.HIGH + .1, origin.z + EmberKilnRules.OUTER);
		for (var shape : level.getBlockCollisions(master, volume)) for (AABB box : shape.toAabbs()) {
			if (++boxes > EmberKilnRules.MAX_COVER_BOXES) return null;
			for (int i = 0; i < sectors.length; i++) if (sectors[i] && EmberKilnRules.blocksSector(
				box.minX - origin.x, box.maxX - origin.x, box.minZ - origin.z, box.maxZ - origin.z, i)) sectors[i] = false;
		}
		Vec3 toward = target.position().subtract(origin);
		int selected = EmberKilnRules.sector(toward.x, toward.z);
		if (selected < 0 || !sectors[selected]) return null;
		var escapes = new ArrayList<Escape>();
		for (UUID id : master.challengers()) {
			if (!(level.getPlayerByUUID(id) instanceof ServerPlayer player) || !master.canHarmParticipant(player)) return null;
			Vec3 delta = player.position().subtract(origin);
			if (!EmberKilnRules.hits(delta.x, delta.z, delta.y)) continue;
			Vec3 destination = origin.add(delta.multiply(1, 0, 1).normalize().scale(EmberKilnRules.ESCAPE));
			AABB corridor = player.getBoundingBox().minmax(player.getBoundingBox().move(destination.subtract(player.position()))).inflate(.1, 0, .1);
			if (!master.pursuitInsideArena(corridor) || !clearEscape(master, level, id, corridor)) return null;
			escapes.add(new Escape(id, corridor));
		}
		return new EmberKiln(master, level, now, escapes, sectors, (float) Math.toDegrees(Math.atan2(-toward.x, toward.z)));
	}

	boolean tick(long now) {
		if (finished) return false;
		if (!validOwner() || now != level.getGameTime() || now < began || now > began + EmberKilnRules.TELL
			|| (lastTick == Long.MIN_VALUE ? now != began : now < lastTick || now > lastTick + 1)) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		if (!supported(level, origin) || escapes.stream().anyMatch(escape -> !clearEscape(master, level, escape.player(), escape.corridor()))) return cancel();
		master.getNavigation().stop(); master.getMoveControl().setWait(); master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		// Native travel runs after this AI step. Preserve its downward collision probe:
		// zeroing Y would clear onGround before gravity is applied for the next tick.
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(origin.x - Math.sin(Math.toRadians(yaw)) * 8, master.getEyeY(), origin.z + Math.cos(Math.toRadians(yaw)) * 8);
		if (now < began + EmberKilnRules.TELL) {
			if ((now - began) % EmberKilnRules.WARNING_REFRESH == 0) draw(false, now);
			return true;
		}
		finished = true; released = true; // Consume before damage/parry callbacks can re-enter.
		draw(true, now);
		master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Feels.sound(level, origin, "aura_slash", 1, .7F);
		// Snapshot eligible victims before the first reaction. A callback cannot move a previously safe
		// player into this already-resolving pulse, while a later escape or new cover still prevents harm.
		List<UUID> victims = roster.stream().sorted(Comparator.naturalOrder())
			.filter(id -> level.getPlayerByUUID(id) instanceof ServerPlayer player && canHit(player)).toList();
		Set<UUID> defeated = new HashSet<>();
		for (UUID id : victims) {
			if (!validOwner(defeated)) break;
			if (level.getPlayerByUUID(id) instanceof ServerPlayer player && canHit(player) && struck.add(id)) {
				Effects.withSource(master, () -> master.projected(player, EmberKilnRules.DAMAGE));
				// A defeat caused while resolving this pulse is expected, not a newly departed
				// challenger. It must not spare later UUIDs in the same simultaneous area hit.
				if (!player.isAlive() && player.level() == level) defeated.add(id);
			}
		}
		return false;
	}

	private boolean validOwner() { return validOwner(Set.of()); }
	private boolean validOwner(Set<UUID> defeated) {
		return master.canMaintainKiln(this) && master.level() == level && roster.equals(master.challengers())
			&& roster.stream().allMatch(id -> level.getPlayerByUUID(id) instanceof ServerPlayer p
				&& (master.canHarmParticipant(p) || defeated.contains(id) && !p.isAlive() && p.level() == level))
			&& master.position().distanceToSqr(origin) <= .0025 && master.onGround() && still(master);
	}
	private boolean canHit(ServerPlayer player) {
		Vec3 delta = player.position().subtract(origin);
		int sector = EmberKilnRules.sector(delta.x, delta.z);
		return master.canHarmParticipant(player) && EmberKilnRules.hits(delta.x, delta.z, delta.y) && sector >= 0 && sectors[sector]
			&& EmberAfterburn.clear(level, master, origin.add(0, .35, 0), player.position().add(0, .35, 0));
	}
	private static boolean still(SwordMaster master) {
		Vec3 velocity = master.getDeltaMovement();
		return Double.isFinite(velocity.x) && Double.isFinite(velocity.y) && Double.isFinite(velocity.z)
			&& velocity.horizontalDistanceSqr() <= .0025 && Math.abs(velocity.y) <= .1;
	}

	boolean released() { return released; }
	long endsAt() { return began + EmberKilnRules.TELL + EmberKilnRules.RECOVERY; }
	void stop() { finished = true; }
	private boolean cancel() { stop(); return false; }

	/** Flat, solid ground under both rims and a broad outside escape; irregular terrain declines this form. */
	private static boolean supported(ServerLevel level, Vec3 origin) {
		if (!Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)
			|| Math.abs(origin.y - Math.rint(origin.y)) > .01) return false;
		int radius = (int) Math.ceil(EmberKilnRules.ESCAPE + .5);
		BlockPos center = BlockPos.containing(origin).below();
		for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
			if (x * x + z * z > (EmberKilnRules.ESCAPE + 1) * (EmberKilnRules.ESCAPE + 1)) continue;
			BlockPos floor = center.offset(x, 0, z);
			if (!level.hasChunkAt(floor) || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				|| !level.getFluidState(floor.above()).isEmpty()) return false;
		}
		return true;
	}

	private static boolean clearEscape(SwordMaster master, ServerLevel level, UUID player, AABB corridor) {
		if (level.getBlockCollisions(master, corridor).iterator().hasNext()) return false;
		return level.getEntities(master, corridor, entity -> entity instanceof LivingEntity living && living.isAlive()
			&& !entity.isSpectator() && !entity.getUUID().equals(player)).isEmpty();
	}

	private Vec3 point(double radius, int boundary) {
		double angle = EmberKilnRules.angle(boundary);
		return origin.add(Math.cos(angle) * radius, .12, Math.sin(angle) * radius);
	}
	private void draw(boolean impact, long now) {
		int life = impact ? 5 : (int) Math.min(EmberKilnRules.WARNING_REFRESH + 1, began + EmberKilnRules.TELL - now);
		for (int i = 0; i < sectors.length; i++) {
			if (!sectors[i]) continue;
			Vec3 inner = point(EmberKilnRules.INNER, i), nextInner = point(EmberKilnRules.INNER, i + 1);
			double outline = EmberKilnRules.OUTER / Math.cos(Math.PI / EmberKilnRules.SECTORS);
			Vec3 outer = point(outline, i), nextOuter = point(outline, i + 1);
			Light.ray(level, inner, nextInner, impact ? master.auraColor() : 0x73E2CB, impact ? .12 : .065, life);
			Light.ray(level, outer, nextOuter, master.auraColor(), impact ? .12 : .065, life);
			if (impact || !sectors[Math.floorMod(i - 1, sectors.length)]) Light.ray(level, inner, outer, master.auraColor(), .065, life);
			if (!sectors[(i + 1) % sectors.length]) Light.ray(level, nextInner, nextOuter, master.auraColor(), .065, life);
		}
	}
}
