package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Server-only evidence for the two earned counters; the public four-field StringContext stays unchanged. */
public final class EarnedCounters {
	private EarnedCounters() {}
	public enum Route { STRUCK, CAUGHT, NEAREST, EMPTY }
	static boolean handles(String art) { return art.equals("backdraft") || art.equals("rooted_parry"); }

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
	}

	/** Immutable selection. Neither a newer guard nor a newly nearby foe can replace this receipt. */
	private record Accepted(Guard guard, AuraApi.StringContext context, Route route, Body body, double range,
			BooleanSupplier evidencePresent) {
		boolean ownerValid(ServerPlayer player) { return guard.valid(player) && evidencePresent.getAsBoolean(); }
		Release release(ServerPlayer player) {
			return ownerValid(player) && (body == null || body.valid(player, range))
				? new Release(player, guard.owner(), context.art().id(), route, body, range, guard.caught().damage()) : null;
		}
	}

	/** A one-use transport envelope. Only its original clash key can resume it, and only through that clash's 43 ticks. */
	static final class Attempt {
		private final Accepted accepted;
		private final EarnedCounterReservation reservation = new EarnedCounterReservation();
		Attempt(Accepted accepted) { this.accepted = accepted; }
		AuraApi.StringContext context() { return accepted.context(); }
		boolean ownerValid(ServerPlayer player) { return accepted.ownerValid(player); }
		boolean canLowerGuard(ServerPlayer player) { return !AuraGuard.guarding(player) || accepted.guard().ownsRaisedGuard(player); }
		void lowerAfterPaid(ServerPlayer player) { if (accepted.guard().ownsRaisedGuard(player)) AuraGuard.lowerForCounter(player); }
		Release release(ServerPlayer player) { return accepted.release(player); }
		void hold(Object key, long now) { reservation.hold(key, now); }
		EarnedCounterReservation.Take take(Object key, long now) { return reservation.take(key, now); }
	}

	/** Called after the ledger consumes its authentic suffix, before clash callbacks or payment. */
	static Attempt accept(ServerPlayer player, AuraApi.StringContext context, Object earned, BooleanSupplier evidencePresent) {
		if (!(earned instanceof Guard guard) || !guard.valid(player)) return null;
		double reach = context.art().id().equals("backdraft") ? ArtRules.BACKDRAFT_REACH : 4;
		double bound = reach + 2; // Backdraft 6.5, Rooted Parry 6.0, including the formerly unbounded struck route.
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
		return new Attempt(new Accepted(guard, context, route, selected == null ? null : Body.capture(selected), bound, evidencePresent));
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
	public static final class Release {
		private final ServerPlayer player;
		private final ReleasedArtOwner owner;
		private final String art;
		private final Route route;
		private final Body body;
		private final double range, damage;
		private boolean retired;
		private Release(ServerPlayer player, ReleasedArtOwner owner, String art, Route route, Body body, double range, double damage) {
			this.player = player; this.owner = owner; this.art = art; this.route = route; this.body = body; this.range = range; this.damage = damage;
		}
		public String art() { return art; }
		public Route route() { return route; }
		public LivingEntity target() { return body == null ? null : body.entity(); }
		public double caughtDamage() { return damage; }
		/** A released lethal hit may finish its surrounding pulse; a changed live target cannot authorize later mutations. */
		public boolean valid() {
			if (!retired && (!owner.valid() || body != null && (!body.boundary(player, range)
				|| body.entity().isAlive() && !body.valid(player, range)))) retired = true;
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
			return valid() && visible(player, target, range);
		}
		/** Post-damage checks also cover callbacks which replace the body, teleport it or change its party. */
		public boolean afterDamage(LivingEntity target) {
			return valid() && target != null && target.level() == owner.level() && !target.isRemoved()
				&& owner.level().getEntity(target.getUUID()) == target
				&& hostileNow(player, target) && target.distanceToSqr(player) <= range * range && player.hasLineOfSight(target)
				&& (!target.isAlive() || permits(target));
		}
	}
}
