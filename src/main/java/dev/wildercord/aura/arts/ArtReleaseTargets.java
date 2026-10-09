package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Server-only acceptance receipts for the two target-bearing fixed releases. No client victim or release-time reselection. */
public final class ArtReleaseTargets {
	private ArtReleaseTargets() {}
	private static final double HAIL_ANCHOR_RANGE = 7, HAIL_PRIORITY_RANGE = 5.5;
	private static final double SKY_OBSERVED_RANGE = 8.5, SKY_CONE_RANGE = 6, SKY_CONE_DEGREES = 90;
	private static final double CONE_HEIGHT = 2.2, SKY_TRACKING_RANGE = 4;

	public enum Route { HAIL_OBSERVED, SKY_OBSERVED, SKY_CONE, GROUND }

	/** Copy the accepted geometry; the mutable body reference is usable only after identity and admission checks. */
	private record Body(LivingEntity entity, UUID uuid, ServerLevel level, Vec3 acceptedPosition, AABB acceptedBounds) {
		static Body capture(LivingEntity entity) {
			return new Body(entity, entity.getUUID(), (ServerLevel) entity.level(), entity.position(), entity.getBoundingBox());
		}
		boolean valid(ServerPlayer player, boolean visible) {
			return entity.isAlive() && !entity.isRemoved() && entity.level() == level && player.level() == level
				&& level.getEntity(uuid) == entity && ArtKit.harmable(player, entity) && (!visible || player.hasLineOfSight(entity));
		}
	}

	/** Immutable release result: Hailfall stays here; Skyfall alone may track this same body strictly under four blocks. */
	public record Release(String art, Route route, Vec3 point, LivingEntity direct, LivingEntity tracking,
			ServerLevel level, long acceptedTick, List<Integer> marks) {
		public Vec3 skyfallPoint(ServerPlayer player) {
			return tracking != null && tracking.isAlive() && !tracking.isRemoved() && tracking.level() == level
				&& level.getEntity(tracking.getUUID()) == tracking && ArtKit.harmable(player, tracking)
				&& tracking.distanceToSqr(point) < SKY_TRACKING_RANGE * SKY_TRACKING_RANGE ? tracking.position() : point;
		}
	}

	public sealed interface Accepted permits Hailfall, Skyfall {
		boolean ownerValid(ServerPlayer player);
		Release release(ServerPlayer player);
	}

	/** Hailfall's observed cloud route and its independent accepted direct-priority right are deliberately separate. */
	private record Hailfall(Commit commit, Body body, boolean directPriority) implements Accepted {
		@Override public boolean ownerValid(ServerPlayer player) { return commit.valid(player); }
		@Override public Release release(ServerPlayer player) {
			if (!commit.valid(player)) return null;
			if (body == null) return ground(commit, player, HAIL_ANCHOR_RANGE, true);
			if (!body.valid(player, true) || body.entity().distanceToSqr(player) >= HAIL_ANCHOR_RANGE * HAIL_ANCHOR_RANGE) return null;
			LivingEntity direct = directPriority && body.entity().distanceToSqr(player) < HAIL_PRIORITY_RANGE * HAIL_PRIORITY_RANGE ? body.entity() : null;
			return commit.released(Route.HAIL_OBSERVED, body.entity().position(), direct, null);
		}
	}

	/** A cone-selected body never receives the observed-last-hit route's wider radius at release. */
	private record Skyfall(Commit commit, Body body, Route route) implements Accepted {
		@Override public boolean ownerValid(ServerPlayer player) { return commit.valid(player); }
		@Override public Release release(ServerPlayer player) {
			if (!commit.valid(player)) return null;
			if (body == null) return ground(commit, player, SKY_CONE_RANGE, false);
			if (!body.valid(player, true)) return null;
			if (route == Route.SKY_OBSERVED ? body.entity().distanceToSqr(player) >= SKY_OBSERVED_RANGE * SKY_OBSERVED_RANGE
				: !cone(player, body.entity(), commit.aim(), body.acceptedBounds().getXsize())) return null;
			return commit.released(route, body.entity().position(), null, body.entity());
		}
	}

	/** Owner/world and copied acceptance metadata; mutable equipment/eligibility is separately checked by the timeline. */
	private record Commit(ServerPlayer player, ReleasedArtOwner owner, String art, long tick, List<Integer> marks,
			Vec3 feet, Vec3 eye, Vec3 aim, Vec3 view, Vec3 point) {
		boolean valid(ServerPlayer current) { return current == player && owner.valid(); }
		Release released(Route route, Vec3 at, LivingEntity direct, LivingEntity tracking) {
			return new Release(art, route, at, direct, tracking, owner.level(), tick, marks);
		}
	}

	/** Capture once after clash admission, while the real server-observed string victim is still available. */
	public static Accepted accept(ServerPlayer player, AuraApi.StringContext context, Vec3 aim, Vec3 view) {
		String art = context.art().id();
		boolean hail = art.equals(RimeArts.HAILFALL);
		if (!hail && !art.equals(ThunderArts.SKYFALL)) throw new IllegalArgumentException("No target receipt for " + art);
		Vec3 ahead = player.position().add(aim.scale(hail ? ArtRules.HAIL_AHEAD : ArtRules.SKYFALL_AHEAD));
		Vec3 floor = ArtKit.floor(player.level(), ahead, 1.5, 3);
		Commit commit = new Commit(player, ReleasedArtOwner.capture(player), art, context.at(), List.copyOf(context.marks()),
			player.position(), player.getEyePosition(), aim, view, floor == null ? ahead : floor);
		LivingEntity observed = context.struck();
		Body body = visible(player, observed) && observed.distanceToSqr(player) < (hail ? HAIL_ANCHOR_RANGE * HAIL_ANCHOR_RANGE
			: SKY_OBSERVED_RANGE * SKY_OBSERVED_RANGE) ? Body.capture(observed) : null;
		if (hail) return body != null ? new Hailfall(commit, body, observed.distanceToSqr(player) < HAIL_PRIORITY_RANGE * HAIL_PRIORITY_RANGE)
			: ground(commit, player, HAIL_ANCHOR_RANGE, true) == null ? null : new Hailfall(commit, null, false);
		if (body != null) return new Skyfall(commit, body, Route.SKY_OBSERVED);
		LivingEntity nearest = player.level().getEntities(player, player.getBoundingBox().inflate(SKY_CONE_RANGE + 1, 1.8, SKY_CONE_RANGE + 1),
			entity -> entity instanceof LivingEntity living && visible(player, living) && cone(player, living, aim, living.getBbWidth()))
			.stream().map(entity -> (LivingEntity) entity).min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).orElse(null);
		if (nearest != null) return new Skyfall(commit, Body.capture(nearest), Route.SKY_CONE);
		return ground(commit, player, SKY_CONE_RANGE, false) == null ? null : new Skyfall(commit, null, Route.GROUND);
	}

	private static boolean visible(ServerPlayer player, LivingEntity entity) {
		return entity != null && entity.isAlive() && !entity.isRemoved() && entity.level() == player.level()
			&& player.level().getEntity(entity.getUUID()) == entity && ArtKit.harmable(player, entity) && player.hasLineOfSight(entity);
	}

	private static boolean cone(ServerPlayer player, LivingEntity entity, Vec3 aim, double width) {
		Vec3 to = entity.position().subtract(player.position());
		return Math.abs(to.y) <= CONE_HEIGHT && ArtRules.inCone(to.x, to.z, aim.x, aim.z, SKY_CONE_RANGE + width / 2, SKY_CONE_DEGREES);
	}

	/** Ground points stay fixed, but moving out of casting range or behind cover before release cancels the paid release. */
	private static Release ground(Commit commit, ServerPlayer player, double range, boolean strict) {
		double distance = player.distanceToSqr(commit.point());
		if (!commit.valid(player) || (strict ? distance >= range * range : distance > range * range)) return null;
		Vec3 end = commit.point().add(0, .1, 0);
		if (player.level().clip(new ClipContext(player.getEyePosition(), end, ClipContext.Block.COLLIDER,
			ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) return null;
		return commit.released(Route.GROUND, commit.point(), null, null);
	}
}
