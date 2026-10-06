package dev.wildercord.aura.arts;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/** Opt-in lifetime for a released cloud, bolt or ground field, independent of weapon and physical recovery. */
public final class ReleasedArtOwner {
	private static final class Life { boolean retired; }
	private static final Map<ServerPlayer, Life> LIVES = new WeakHashMap<>();
	private final ServerPlayer player;
	private final ServerLevel level;
	private final Life life;

	private ReleasedArtOwner(ServerPlayer player) {
		this.player = player;
		this.level = player.level();
		this.life = LIVES.computeIfAbsent(player, ignored -> new Life());
	}

	public static ReleasedArtOwner capture(ServerPlayer player) { return new ReleasedArtOwner(player); }
	public ServerLevel level() { return level; }
	/** Exact original body identity, for field admission without rebinding a previous release. */
	boolean owns(ServerPlayer owner) { return player == owner; }

	/** A retired original body can never become this release's owner again, even after a same-tick world round trip. */
	public boolean valid() {
		if (!life.retired && (!player.isAlive() || player.isRemoved() || player.level() != level
			|| level.getServer().getPlayerList().getPlayer(player.getUUID()) != player)) life.retired = true;
		return !life.retired;
	}

	private static void retire(ServerPlayer player) {
		Life life = LIVES.remove(player);
		if (life != null) life.retired = true;
	}

	public static void init() {
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> retire(player));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer player) retire(player); });
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> retire(oldPlayer));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LIVES.values().forEach(life -> life.retired = true);
			LIVES.clear();
		});
	}
}
