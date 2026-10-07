package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Server-only evidence for the authored earned counters; the public four-field StringContext stays unchanged. */
public final class EarnedCounters {
	private EarnedCounters() {}
	public enum Route { STRUCK, CAUGHT, NEAREST, EMPTY }
	static boolean handles(String art) { return art.equals("backdraft") || art.equals("rooted_parry") || art.equals("glacier_mirror") || art.equals("static_riposte") || originalGeometry(art); }
	private static boolean originalGeometry(String art) { return art.equals("unmoved") || art.equals("null_parry"); }

	/** Created only at a real perfect guard, before its damage/reflection callbacks. */
	record Guard(ServerPlayer player, ReleasedArtOwner owner, ItemStack blade, String method,
			AuraGuard.Caught caught, Body attacker, long raised, long until) {
		static Guard capture(ServerPlayer player, AuraGuard.Caught caught) {
			var state = Aura.state(player);
			return new Guard(player, ReleasedArtOwner.capture(player), player.getMainHandItem(), Aura.data(player).method(),
				caught, caught.attacker() == null ? null : Body.capture(caught.attacker()), state.guardRaised(), state.guardUntil());
		}
		boolean valid(ServerPlayer current) {
			return current == player && owner.valid() && player.getMainHandItem() == blade && method.equals(Aura.data(player).method());
		}
		boolean ownsRaisedGuard(ServerPlayer current) {
			var state = Aura.state(current);
			return current == player && state.guardRaised() == raised && state.guardUntil() == until;
		}
	}

	private record Body(LivingEntity entity, UUID uuid, ServerLevel level) {
		static Body capture(LivingEntity entity) { return new Body(entity, entity.getUUID(), (ServerLevel) entity.level()); }
		boolean loaded(ServerPlayer player) {
			return entity.getUUID().equals(uuid) && !entity.isRemoved() && entity.level() == level && player.level() == level
				&& level.getEntity(uuid) == entity;
		}
		boolean boundary(ServerPlayer player, double range) {
			return loaded(player) && entity.distanceToSqr(player) <= range * range && player.hasLineOfSight(entity)
				&& hostileNow(player, entity);
		}
		boolean valid(ServerPlayer player, double range) { return boundary(player, range) && ArtKit.harmable(player, entity); }
		boolean originalBoundary(ServerPlayer player, Route route, Vec3 facing) {
			if (!loaded(player) || !hostileNow(player, entity)) return false;
			Vec3 relative = entity.position().subtract(player.position());
			return switch (route) {
				case STRUCK -> true; // The server-observed struck route deliberately has no distance or LOS cap.
				case CAUGHT -> OriginalCounterRules.caught(entity.distanceToSqr(player));
				case NEAREST -> player.getBoundingBox().inflate(5, 1.8, 5).intersects(entity.getBoundingBox())
					&& OriginalCounterRules.nearest(relative.x, relative.y, relative.z, facing.x, facing.z, entity.getBbWidth());
				case EMPTY -> false;
			};
		}
		boolean originalValid(ServerPlayer player, Route route, Vec3 facing) {
			return originalBoundary(player, route, facing) && ArtKit.harmableWithoutAim(player, entity);
		}
	}

	/** Immutable selection. Neither a newer guard nor a newly nearby foe can replace this receipt. */
	private record Accepted(Guard guard, AuraApi.StringContext context, Route route, Body body, double range,
			Vec3 facing, BooleanSupplier evidencePresent) {
		boolean ownerValid(ServerPlayer player) { return guard.valid(player) && evidencePresent.getAsBoolean(); }
		Release release(ServerPlayer player) {
			boolean original = originalGeometry(context.art().id());
			return ownerValid(player) && (context.art().id().equals("null_parry") || body == null || (original ? body.originalValid(player, route, facing) : body.valid(player, range)))
				? new Release(player, guard.owner(), context.art().id(), route, body, range, guard.caught().damage(), facing) : null;
		}
	}

	/** A one-use transport envelope. Only its original clash key can resume it, and only through that clash's 43 ticks. */
	static final class Attempt {
		private final Accepted accepted;
		private final EarnedCounterReservation reservation = new EarnedCounterReservation();
		private boolean paidFinalized;
		Attempt(Accepted accepted) { this.accepted = accepted; }
		AuraApi.StringContext context() { return accepted.context(); }
		boolean ownerValid(ServerPlayer player) { return accepted.ownerValid(player); }
		boolean canLowerGuard(ServerPlayer player) { return !AuraGuard.guarding(player) || accepted.guard().ownsRaisedGuard(player); }
		void lowerAfterPaid(ServerPlayer player) { if (accepted.guard().ownsRaisedGuard(player)) AuraGuard.lowerForCounter(player); }
		Release release(ServerPlayer player) { return accepted.release(player); }
		/** Finalize once only after payment callbacks and exact pending/admission revalidation. */
		boolean finalizePaid(ServerPlayer player, long acceptedAt) {
			if (paidFinalized) return false;
			paidFinalized = true;
			if (accepted.context().art().id().equals("unmoved")) {
				dev.wildercord.aura.arts.ArtWards.unmoved(player, accepted.guard().owner(), acceptedAt, ArtRules.UNMOVED_TICKS).grant();
				return true; // Revalidate after every attempted defensive mutation, even if its native callback retired the ward.
			}
			return false;
		}
		void hold(Object key, long now) { reservation.hold(key, now); }
		EarnedCounterReservation.Take take(Object key, long now) { return reservation.take(key, now); }
	}

	/** Called after the ledger consumes its authentic suffix, before clash callbacks or payment. */
	static Attempt accept(ServerPlayer player, AuraApi.StringContext context, Object earned, BooleanSupplier evidencePresent) {
		if (!(earned instanceof Guard guard) || !guard.valid(player)) return null;
		if (originalGeometry(context.art().id())) return acceptOriginal(player, context, guard, evidencePresent);
		double reach = switch (context.art().id()) {
			case "backdraft" -> ArtRules.BACKDRAFT_REACH;
			case "static_riposte" -> ArtRules.RIPOSTE_REACH;
			default -> 4;
		};
		// Explicit owner range/LOS admission and release: Mirror 6, Riposte 7; no chain-hop LOS change.
		double bound = reach + 2;
		LivingEntity selected = context.struck();
		Route route = Route.STRUCK;
		if (!visible(player, selected, bound)) {
			selected = guard.attacker() != null && guard.attacker().valid(player, bound) ? guard.attacker().entity() : null;
			route = Route.CAUGHT;
		}
		if (!visible(player, selected, bound)) {
			selected = ArtKit.arc(player, null, reach, 120, Integer.MAX_VALUE).stream()
				.filter(foe -> visible(player, foe, bound)).findFirst().orElse(null);
			route = selected == null ? Route.EMPTY : Route.NEAREST;
		}
		return new Attempt(new Accepted(guard, context, route, selected == null ? null : Body.capture(selected), bound, ArtKit.flat(player), evidencePresent));
	}
	/** Preserve the two original instant forms' struck, caught<=6, then 4-block/120-degree cone policy. */
	private static Attempt acceptOriginal(ServerPlayer player, AuraApi.StringContext context, Guard guard, BooleanSupplier evidencePresent) {
		Vec3 facing = ArtKit.flat(player);
		Body selected = context.struck() == null ? null : Body.capture(context.struck());
		Route route = Route.STRUCK;
		if (selected == null || !selected.originalValid(player, route, facing)) {
			selected = guard.attacker(); route = Route.CAUGHT;
		}
		if (selected == null || !selected.originalValid(player, route, facing)) {
			route = Route.NEAREST;
			selected = player.level().getEntities(player, player.getBoundingBox().inflate(5, 1.8, 5),
				entity -> entity instanceof LivingEntity living && Body.capture(living).originalValid(player, Route.NEAREST, facing))
				.stream().map(entity -> (LivingEntity) entity).min(java.util.Comparator.comparingDouble(player::distanceToSqr))
				.map(Body::capture).orElse(null);
			if (selected == null) route = Route.EMPTY;
		}
		return new Attempt(new Accepted(guard, context, route, selected, 6, facing, evidencePresent));
	}

	private static boolean visible(ServerPlayer player, LivingEntity entity, double range) {
		return entity != null && Body.capture(entity).valid(player, range);
	}

	/** Party/duel rules remain relevant even when the just-released hit killed its selected body. */
	private static boolean hostileNow(ServerPlayer player, LivingEntity entity) {
		Boolean duel = dev.wildercord.duel.Duels.canHarm(player, entity);
		return (duel != null ? duel : !dev.wildercord.cast.Targets.isAlly(player, entity))
			&& !dev.wildercord.party.Parties.blocksHarm(player, entity)
			&& !entity.isSpectator() && (!(entity instanceof net.minecraft.world.entity.player.Player other) || player.canHarmPlayer(other));
	}

	/** Scoped to the single active frame; later cosmetics never regain harm or healing authority. */
	public static final class Release implements ArtHitScope.Boundary {
		private final ServerPlayer player;
		private final ReleasedArtOwner owner;
		private final String art;
		private final Route route;
		private final Body body;
		private final double range, damage;
		private final Vec3 facing;
		private boolean retired, primaryLost;
		private final java.util.Map<LivingEntity, Body> touched = new java.util.IdentityHashMap<>();
		private Release(ServerPlayer player, ReleasedArtOwner owner, String art, Route route, Body body, double range, double damage, Vec3 facing) {
			this.player = player; this.owner = owner; this.art = art; this.route = route; this.body = body; this.range = range; this.damage = damage; this.facing = facing;
		}
		public String art() { return art; }
		public boolean originalGeometry() { return EarnedCounters.originalGeometry(art); }
		public Route route() { return route; }
		public LivingEntity target() { return body == null ? null : body.entity(); }
		public double caughtDamage() { return damage; }
		/** A released lethal hit may finish its surrounding pulse; a changed live target cannot authorize later mutations. */
		public boolean valid() {
			if (!retired && (!owner.valid() || body != null && (originalGeometry()
				? !art.equals("null_parry") && (primaryLost || !body.originalBoundary(player, route, facing)
					|| body.entity().isAlive() && !body.originalValid(player, route, facing))
				: !body.boundary(player, range) || body.entity().isAlive() && !body.valid(player, range)))) retired = true;
			return !retired;
		}
		/** Nested rune consequences inherit the active callback boundary, then keep their ordinary released lifetime. */
		public boolean linkedAlive() {
			return owner.valid() && !retired && (MastersArts.earnedCounter(player) != this || valid());
		}
		public boolean linkedAdmits(net.minecraft.world.entity.Entity target) {
			return linkedAlive() && (MastersArts.earnedCounter(player) != this || target == player
				|| target instanceof LivingEntity living && permits(living));
		}
		public boolean permits(LivingEntity target) {
			return valid() && (originalGeometry() ? target != null && (body == null || target != body.entity() || primaryValid()) && touched.computeIfAbsent(target, Body::capture).loaded(player)
				&& ArtKit.harmableWithoutAim(player, target) && hostileNow(player, target) : visible(player, target, range));
		}
		/** Null's pulse is independent: a lost immutable primary never cancels or redirects the radial release. */
		public boolean primaryValid() {
			if (body == null || !valid() || primaryLost) return false;
			if (!(originalGeometry() ? body.originalValid(player, route, facing) : body.valid(player, range))) primaryLost = true;
			return !primaryLost;
		}
		private boolean primaryAfterDamage(LivingEntity target) {
			if (!originalGeometry() || body == null || target != body.entity()) return true;
			if (!primaryLost && (!body.originalBoundary(player, route, facing)
				|| target.isAlive() && !body.originalValid(player, route, facing))) primaryLost = true;
			return !primaryLost;
		}

		/** A launched landing watcher keeps exact body/owner permission, without rechecking throw distance or pose. */
		public BooleanSupplier landingPermission(LivingEntity target) {
			Body landed = Body.capture(target);
			return () -> owner.valid() && landed.loaded(player) && ArtKit.harmableWithoutAim(player, target) && hostileNow(player, target);
		}

		/** Mirror's original secondary chill cone has no LOS rule; only its selected primary gains the new range/LOS boundary. */
		public boolean permitsMirrorCollateral(LivingEntity target) {
			return art.equals("glacier_mirror") && valid() && target != null && target != player && target.isAlive()
				&& Body.capture(target).loaded(player) && !dev.wildercord.aura.arts.ArtFields.blocksRetiredHarm(player, target)
				&& dev.wildercord.cast.Targets.canHarm(player, target) && hostileNow(player, target);
		}
		/** Post-damage checks also cover callbacks which replace the body, teleport it or change its party. */
		public boolean afterDamage(LivingEntity target) {
			return valid() && target != null && primaryAfterDamage(target) && target.level() == owner.level() && !target.isRemoved()
				&& owner.level().getEntity(target.getUUID()) == target
				&& (!originalGeometry() || touched.computeIfAbsent(target, Body::capture).loaded(player))
				&& hostileNow(player, target) && (originalGeometry() || target.distanceToSqr(player) <= range * range && player.hasLineOfSight(target))
				&& (!target.isAlive() || permits(target));
		}
	}
}
