package dev.wildercord.aura.arts;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;


/** Opt-in lifetime for a released cloud, bolt or ground field, independent of weapon and physical recovery. */
public final class ReleasedArtOwner {
	private static final ReleasedOwnerLifetimes<ServerPlayer, ServerLevel> LIVES = new ReleasedOwnerLifetimes<>();
	private final ServerPlayer player;
	private final ServerLevel level;
	private final ReleasedOwnerLifetimes.Life<ServerLevel> life;

	private ReleasedArtOwner(ServerPlayer player) {
		this.player = player;
		this.level = player.level();
		this.life = LIVES.capture(player, level);
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
		LIVES.retire(player);
	}

	/**
	 * Called at the native world mutation, before any AFTER-level listener can capture a destination release.
	 * Each actual departure retires the current generation. A later outer event cannot retire a nested return's fresh generation.
	 */
	public static WorldChange changingWorld(ServerPlayer player, ServerLevel destination) {
		var change = LIVES.begin(player, player.level(), destination);
		if (change.changesWorld) ArtWards.worldChanging(player);
		return new WorldChange(player, change);
	}

	/** Called immediately after this setter's native level assignment, before its later instrumentation. */
	public static void assignedWorld(ServerPlayer player) { LIVES.assigned(player); }

	/** Scoped around exactly one unchanged native setter invocation, including nested calls and exceptions. */
	public static final class WorldChange implements AutoCloseable {
		private final ServerPlayer player;
		private final ReleasedOwnerLifetimes.Change<ServerPlayer, ServerLevel> change;
		private WorldChange(ServerPlayer player, ReleasedOwnerLifetimes.Change<ServerPlayer, ServerLevel> change) {
			this.player = player; this.change = change;
		}
		@Override public void close() {
			LIVES.finish(change, player.level());
			if (change.changesWorld) ArtWards.worldChanged(player);
		}
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer player) retire(player); });
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> retire(oldPlayer));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LIVES.clear();
		});
	}
}
