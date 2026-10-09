package dev.wildercord.aura.arts;

import dev.wildercord.cast.Targets;
import dev.wildercord.duel.Duels;
import dev.wildercord.party.Parties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** One exact conductor of released lightning. A lethal hit may conduct until that original body unloads. */
record ReleasedCounterBody(LivingEntity entity, UUID uuid, ServerLevel level) {
	static ReleasedCounterBody capture(LivingEntity entity) {
		return new ReleasedCounterBody(entity, entity.getUUID(), (ServerLevel) entity.level());
	}

	boolean conducts(ServerPlayer owner, ReleasedArtOwner lifetime) {
		if (!lifetime.valid() || lifetime.level() != level || entity.level() != level || entity.isRemoved()
			|| !entity.getUUID().equals(uuid) || level.getEntity(uuid) != entity || entity == owner || entity.isSpectator()) return false;
		Boolean duel = Duels.canHarm(owner, entity);
		return (duel != null ? duel : !Targets.isAlly(owner, entity)) && !Parties.blocksHarm(owner, entity)
			&& (!(entity instanceof Player player) || owner.canHarmPlayer(player))
			&& (!entity.isAlive() || ArtKit.harmable(owner, entity));
	}

	boolean targetFrom(ServerPlayer owner, ReleasedArtOwner lifetime, ReleasedCounterBody source, double reach) {
		return conducts(owner, lifetime) && source.conducts(owner, lifetime) && ArtKit.harmable(owner, entity)
			&& entity.getBoundingBox().getCenter().distanceToSqr(source.entity.getBoundingBox().getCenter()) < reach * reach;
	}
}
