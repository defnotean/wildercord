package dev.wildercord.aura.arts;

import dev.wildercord.aura.ArtHitScope;
import dev.wildercord.aura.ArtRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/** One released jump's callback envelope. It never inherits the original earning window, owner radius or LOS. */
final class RiposteHit implements ArtHitScope.Boundary {
	private final ServerPlayer player;
	private final ReleasedArtOwner owner;
	private final ReleasedCounterBody source, target;
	private final Map<LivingEntity, ReleasedCounterBody> recipients = new IdentityHashMap<>();
	private boolean retired;

	RiposteHit(ServerPlayer player, ReleasedArtOwner owner, ReleasedCounterBody source, ReleasedCounterBody target) {
		this.player = player; this.owner = owner; this.source = source; this.target = target;
		recipients.put(target.entity(), target);
	}

	@Override public boolean valid() {
		if (!retired && (!source.conducts(player, owner) || !target.conducts(player, owner)
			|| target.entity().getBoundingBox().getCenter().distanceToSqr(source.entity().getBoundingBox().getCenter())
				>= ArtRules.RIPOSTE_REACH * ArtRules.RIPOSTE_REACH)) retired = true;
		return !retired;
	}

	@Override public boolean permits(LivingEntity entity) {
		return entity != null && valid() && recipients.computeIfAbsent(entity, ReleasedCounterBody::capture).conducts(player, owner)
			&& ArtKit.harmable(player, entity);
	}

	@Override public boolean afterDamage(LivingEntity entity) {
		ReleasedCounterBody receipt = recipients.get(entity);
		if (!valid() || receipt == null || !receipt.conducts(player, owner)) retired = true;
		return !retired;
	}

	/** Once a nested rune has left this synchronous hit, it keeps the ordinary released owner lifetime. */
	@Override public boolean linkedAlive() {
		return owner.valid() && !retired && (!ArtHitScope.contains(player, this) || valid());
	}
	@Override public boolean linkedAdmits(Entity entity) {
		return linkedAlive() && (!ArtHitScope.contains(player, this) || entity == player
			|| entity instanceof LivingEntity living && permits(living));
	}
}
