package dev.wildercord.monster;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

/**
 * A biome elite (see {@link EliteTier}): a monster of the wilds big enough to carry a boss bar, shown to everyone near
 * while it fights and taken down when it stops, dies or goes. Once it has fought it stays in the world.
 */
public abstract class EliteMonster extends WildMonster {
	/** Who sees its bar: everyone this near while it fights, until they go this far. */
	private static final double BAR_SHOW = 24, BAR_HIDE = 32;

	private final ServerBossEvent bar;

	protected EliteMonster(EntityType<? extends EliteMonster> type, Level level, BossEvent.BossBarColor color) {
		super(type, level);
		this.bar = new ServerBossEvent(getUUID(), getType().getDescription(), color, BossEvent.BossBarOverlay.NOTCHED_10);
	}

	/** Its step among the elites. */
	public abstract EliteTier tier();

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (getTarget() == null || !getTarget().isAlive()) {
			bar.removeAllPlayers();
			return;
		}
		setPersistenceRequired();
		bar.setProgress(getHealth() / getMaxHealth());
		for (ServerPlayer player : new ArrayList<>(bar.getPlayers())) {
			if (player.level() != level || !player.isAlive() || player.distanceToSqr(this) > BAR_HIDE * BAR_HIDE) {
				bar.removePlayer(player);
			}
		}
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) < BAR_SHOW * BAR_SHOW) {
				bar.addPlayer(player);
			}
		}
	}

	@Override
	public void die(DamageSource source) {
		bar.removeAllPlayers();
		super.die(source);
	}

	@Override
	public void remove(RemovalReason reason) {
		bar.removeAllPlayers();
		super.remove(reason);
	}
}
