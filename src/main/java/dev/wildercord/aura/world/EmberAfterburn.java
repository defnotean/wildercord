package dev.wildercord.aura.world;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One immutable, Master-owned wake. There are no detached tasks, terrain fire, player fields or ownership transfers. */
final class EmberAfterburn {
	private final SwordMaster master;
	private final ServerLevel level;
	private final Vec3 origin, aim, side;
	private record Lane(double offset, Vec3 start, double reach) {}
	private final List<Lane> lanes;
	private final long warnedAt;
	private final Set<UUID> struck = new HashSet<>();
	private boolean finished, warned;
	private long drawnAt = Long.MIN_VALUE;

	EmberAfterburn(SwordMaster master, Vec3 origin, Vec3 aim, int participants, long now) {
		this.master = master;
		this.level = (ServerLevel) master.level();
		this.origin = origin;
		this.aim = aim.multiply(1, 0, 1).normalize();
		this.side = new Vec3(-this.aim.z, 0, this.aim.x);
		this.lanes = EmberWakeRules.lanes(participants).stream().map(offset -> {
			Vec3 start = origin.add(side.scale(offset)).add(this.aim.scale(EmberWakeRules.START));
			double reach = clear(level, master, origin.add(0, .9, 0), start.add(0, .9, 0))
				? EmberWakeRules.START + start.distanceTo(clipped(start, EmberWakeRules.RANGE)) : 0;
			return new Lane(offset, start, reach);
		}).filter(lane -> lane.reach() > EmberWakeRules.START).toList();
		this.warnedAt = now;
	}

	/** Returns false once cancelled, expired or consumed; calling twice cannot repeat a hit. */
	boolean tick(long now) {
		if (finished) return false;
		if (!master.canMaintainAfterburn() || master.level() != level || now > warnedAt + EmberWakeRules.AFTERBURN_TELL
			|| aim.lengthSqr() < .99 || now < warnedAt) {
			finished = true;
			return false;
		}
		if (!warned && now != warnedAt) {
			finished = true;
			return false;
		}
		if (!EmberWakeRules.ignites(now, warnedAt)) {
			if (now != drawnAt && (now - warnedAt) % EmberWakeRules.WARNING_REFRESH == 0) {
				draw(false, now);
				drawnAt = now;
				warned = true;
			}
			return true;
		}
		// Consume before effects: parries, death and callbacks may re-enter encounter code.
		finished = true;
		draw(true, now);
		Feels.sound(level, origin, "aura_slash", 1, .65F);
		for (ServerPlayer player : level.players()) {
			if (!master.canHarmParticipant(player)) continue;
			Vec3 delta = player.position().subtract(origin);
			for (Lane lane : lanes) {
				double forward = delta.dot(aim);
				if (!EmberWakeRules.hits(forward, delta.dot(side) - lane.offset(), delta.y)
					|| forward > EmberWakeRules.START + lane.start().distanceTo(clipped(lane.start(), lane.reach()))) continue;
				Vec3 start = lane.start();
				if (!clear(level, master, origin.add(0, .9, 0), start.add(0, .9, 0))
					|| !clear(level, master, start.add(0, .9, 0), player.getBoundingBox().getCenter())) continue;
				if (struck.add(player.getUUID())) Effects.withSource(master, () -> master.projected(player, EmberWakeRules.AFTERBURN_DAMAGE));
				break;
			}
		}
		return false;
	}

	private void draw(boolean impact, long now) {
		int color = master.auraColor();
		int life = impact ? 5 : (int) Math.min(EmberWakeRules.WARNING_REFRESH + 1, warnedAt + EmberWakeRules.AFTERBURN_TELL - now);
		for (Lane lane : lanes) {
			Vec3 start = lane.start();
			if (!clear(level, master, origin.add(0, .9, 0), start.add(0, .9, 0))) continue;
			Vec3 end = clipped(start, lane.reach());
			Vec3 low = start.add(0, .12, 0), high = end.add(0, .12, 0);
			// Two edges show the actual safe boundary; short crossbars distinguish a wake from a flying crescent.
			for (int sign : new int[] {-1, 1}) {
				Vec3 offset = side.scale(sign * EmberWakeRules.HALF_WIDTH);
				Light.ray(level, low.add(offset), high.add(offset), color, impact ? .16 : .055, life);
			}
			for (double at = 0; at <= low.distanceTo(high); at += 1.5) {
				Vec3 point = low.add(aim.scale(at));
				Light.ray(level, point.subtract(side.scale(EmberWakeRules.HALF_WIDTH)), point.add(side.scale(EmberWakeRules.HALF_WIDTH)), color, .045, life);
				if (impact) Motes.burst(level, point.add(0, .4, 0), 3, color, .06, 8, .05);
			}
		}
	}

	/** Cover can shorten a warned lane, but removing it cannot expose a longer, previously unmarked hit area. */
	private Vec3 clipped(Vec3 start, double reach) {
		Vec3 end = start.add(aim.scale(reach - EmberWakeRules.START));
		var block = level.clip(new ClipContext(start.add(0, .9, 0), end.add(0, .9, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, master));
		return block.getType() == HitResult.Type.MISS ? end : block.getLocation().add(0, -.9, 0);
	}

	/** Short-lived outlines stop refreshing on cancellation rather than leaving a ghost warning for the whole tell. */
	static void warnCut(SwordMaster master, Vec3 origin, Vec3 aim, int remaining) {
		ServerLevel level = (ServerLevel) master.level();
		int color = master.auraColor(), life = Math.min(EmberWakeRules.WARNING_REFRESH + 1, remaining);
		Vec3 side = new Vec3(-aim.z, 0, aim.x), feet = origin.add(0, .1, 0);
		for (int sign : new int[] {-1, 1}) {
			Vec3 shoulder = feet.add(aim.scale(2.75)).add(side.scale(sign * 3));
			Light.ray(level, feet.add(side.scale(sign * .8)), shoulder, color, .055, life);
			Light.ray(level, shoulder, feet.add(aim.scale(4)).add(side.scale(sign * 3)), color, .055, life);
		}
		Light.ray(level, feet.add(aim.scale(4)).add(side.scale(-3)), feet.add(aim.scale(4)).add(side.scale(3)), color, .055, life);
	}

	static boolean clear(ServerLevel level, SwordMaster master, Vec3 from, Vec3 to) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, master)).getType() == HitResult.Type.MISS;
	}
}
