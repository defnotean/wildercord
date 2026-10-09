package dev.wildercord.pet;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.DuelistDuels;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.duel.Duels;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-owned identity, saved-body recovery and one globally bounded, nonpersistent chunk ticket. */
final class CinnamonCompanion {
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("wildercord-cinnamon.json");
	private static final Map<UUID, CinnamonDog> LIVE = new HashMap<>();
	private static final Set<UUID> BLOCKED = new HashSet<>();
	private static final Set<UUID> REQUESTED = new HashSet<>();
	private static final Set<UUID> UNCERTAIN_OWNERS = new HashSet<>(), WARNED_IDENTITY = new HashSet<>();
	private static boolean uncertainOverflow;
	private static final CinnamonRecoveryRules LEASE = new CinnamonRecoveryRules();
	private static final TicketType RECOVERY = Registry.register(BuiltInRegistries.TICKET_TYPE, Wildercord.id("cinnamon_recovery"),
		new TicketType(CinnamonRecoveryRules.TICKET_TICKS, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
	private static ServerLevel ticketLevel;
	private static ChunkPos ticketChunk;
	private static CinnamonConfig config = CinnamonConfig.EMPTY;
	private static boolean stopping, recalling;
	private static long lastModified = -1;
	private CinnamonCompanion() {}

	static void init() {
		readConfig();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> { clear(); stopping = false; readConfig(); });
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof CinnamonDog dog) loaded(dog, level);
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (!(entity instanceof CinnamonDog dog)) return;
			UUID id = ownerId(dog);
			if (id == null || LIVE.get(id) != dog) return;
			changed(dog);
			var reason = dog.getRemovalReason();
			if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
				var journal = CinnamonJournal.of(level.getServer());
				var entry = journal.get(id);
				if (entry != null && entry.entity().equals(dog.getUUID())) journal.put(id, entry.retire());
			}
			LIVE.remove(id, dog);
		});
		ServerTickEvents.END_SERVER_TICK.register(CinnamonCompanion::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			CinnamonDog dog = LIVE.get(id);
			if (dog != null) changed(dog);
			if (LEASE.heldBy(id)) release();
			BLOCKED.remove(id); REQUESTED.remove(id);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			stopping = true;
			for (CinnamonDog dog : LIVE.values()) changed(dog);
			release();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
	}

	private static void loaded(CinnamonDog dog, ServerLevel level) {
		UUID id = ownerId(dog);
		if (id == null) { dog.discard(); return; }
		var entry = CinnamonJournal.of(level.getServer()).get(id);
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
		UUID marker = player == null ? null : player.getAttached(CinnamonState.IDENTITY);
		if (entry == null || CinnamonIdentityRules.status(marker, entry.entity(), false) == CinnamonIdentityRules.Status.UNCERTAIN) {
			// Preserve independently intact owned entity data while identity is uncertain. No AI, interaction or replacement.
			uncertain(id);
			if (player != null) warnIdentity(player, entry);
			return;
		}
		if (!entry.entity().equals(dog.getUUID())) {
			// A marker may itself have been reconstructed from this journal. Never use that circular agreement to erase a different owned body.
			uncertain(id); return;
		}
		if (entry.retired()) {
			if (CinnamonIdentityRules.mayRetire(dog.getUUID(), entry.entity(), marker, true)) dog.discard(); else uncertain(id);
			return;
		}
		if (player != null && marker == null) player.setAttached(CinnamonState.IDENTITY, entry.entity());
		UNCERTAIN_OWNERS.remove(id); WARNED_IDENTITY.remove(id);
		CinnamonDog previous = LIVE.get(id);
		if (previous != null && previous != dog && !previous.isRemoved()) {
			// A real cross-dimensional transfer removes its old body before the new one is indexed.
			dog.discard(); return;
		}
		LIVE.put(id, dog);
		entry.care().restore(dog);
		BLOCKED.remove(id);
		changed(dog);
	}

	static UUID ownerId(CinnamonDog dog) {
		return dog.getOwnerReference() == null ? null : dog.getOwnerReference().getUUID();
	}
	static ServerPlayer ownerOf(CinnamonDog dog) {
		UUID id = ownerId(dog);
		return id == null || !(dog.level() instanceof ServerLevel level) ? null : level.getServer().getPlayerList().getPlayer(id);
	}
	static boolean active(CinnamonDog dog) {
		ServerPlayer player = ownerOf(dog);
		if (stopping || player == null || !matches(player) || !player.isAlive() || player.isRemoved() || player.isSpectator()
			|| !dog.isAlive() || dog.isRemoved() || LIVE.get(player.getUUID()) != dog || dog.level() != player.level()) return false;
		var entry = CinnamonJournal.of(player.level().getServer()).get(player.getUUID());
		return entry != null && !entry.retired() && entry.entity().equals(dog.getUUID())
			&& CinnamonIdentityRules.status(player.getAttached(CinnamonState.IDENTITY), entry.entity(), false) == CinnamonIdentityRules.Status.REGISTERED;
	}
	/** Snapshot only the canonical object, including its final position on chunk unload. */
	static void changed(CinnamonDog dog) {
		UUID id = ownerId(dog);
		if (id == null || LIVE.get(id) != dog || !(dog.level() instanceof ServerLevel level)) return;
		var journal = CinnamonJournal.of(level.getServer());
		var entry = journal.get(id);
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
		if (entry != null && !entry.retired() && entry.entity().equals(dog.getUUID())
			&& (player == null || identity(player, entry) == CinnamonIdentityRules.Status.REGISTERED)) journal.put(id, entry.snapshot(dog));
	}
	static boolean mayRecall(ServerPlayer player) {
		if (stopping || !matches(player) || !player.isAlive() || player.isRemoved() || player.isSpectator()
			|| player.isPassenger() || Duels.inDuel(player) || DuelistDuels.inDuel(player)
			|| player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player
			|| DungeonWards.warded(player.level(), player.blockPosition())) return false;
		var nearby = new java.util.ArrayList<SwordMaster>();
		player.level().getEntities(EntityTypeTestHolder.MASTERS, master -> master.distanceToSqr(player) <= 96 * 96, nearby, 17);
		if (nearby.size() > 16) return false;
		for (SwordMaster master : nearby) if (master.canHarmParticipant(player)) return false;
		return true;
	}
	private static final class EntityTypeTestHolder {
		private static final net.minecraft.world.level.entity.EntityTypeTest<Entity, SwordMaster> MASTERS =
			net.minecraft.world.level.entity.EntityTypeTest.forClass(SwordMaster.class);
	}

	private static void tick(MinecraftServer server) {
		if (stopping || server.getTickCount() % 5 != 0) return;
		if (server.getTickCount() % 100 == 0) readConfig();
		long now = server.overworld().getGameTime();
		ServerPlayer player = configuredOwner(server);
		if (ticketLevel != null && (player == null || !LEASE.heldBy(player.getUUID()) || !mayRecall(player) || LEASE.expired(now))) {
			if (player != null && LEASE.heldBy(player.getUUID()) && LEASE.expired(now)) {
				BLOCKED.add(player.getUUID());
				if (REQUESTED.remove(player.getUUID())) message(player, LIVE.containsKey(player.getUUID()) ? "recall_blocked" : "recall_missing");
			}
			release();
		}
		if (player == null) return;
		if (!mayRecall(player)) {
			if (REQUESTED.remove(player.getUUID())) message(player, "recall_blocked");
			return;
		}
		var journal = CinnamonJournal.of(server);
		var entry = journal.get(player.getUUID());
		var identity = identity(player, entry);
		if (identity == CinnamonIdentityRules.Status.UNCERTAIN) {
			REQUESTED.remove(player.getUUID());
			if (LEASE.heldBy(player.getUUID())) release();
			warnIdentity(player, entry); return;
		}
		if (entry == null) { createFirst(player, journal); return; }
		WARNED_IDENTITY.remove(player.getUUID());
		if (player.getAttached(CinnamonState.IDENTITY) == null) player.setAttached(CinnamonState.IDENTITY, entry.entity());
		if (entry.retired()) return;
		CinnamonDog dog = LIVE.get(player.getUUID());
		if (dog == null || dog.isRemoved()) {
			Entity found = server.overworld().getEntityInAnyDimension(entry.entity());
			if (found instanceof CinnamonDog known && player.getUUID().equals(ownerId(known))) loaded(known, (ServerLevel) known.level());
			dog = LIVE.get(player.getUUID());
		}
		if (dog == null || dog.isRemoved()) {
			// An intentionally parked companion can remain saved and unloaded until her owner calls.
			if (!REQUESTED.contains(player.getUUID()) && entry.care().sitting() && entry.care().recoveryUntil() <= now
				&& entry.dimension().equals(player.level().dimension().identifier().toString())) return;
			if (!BLOCKED.contains(player.getUUID()) && !LEASE.heldBy(player.getUUID())) recoverChunk(player, entry);
			return;
		}
		changed(dog);
		boolean unsafe = dog.getY() < dog.level().getMinY() + 2 || !dog.level().noCollision(dog, dog.getBoundingBox());
		boolean requested = REQUESTED.contains(player.getUUID());
		boolean distant = dog.level() != player.level() || !dog.isOrderedToSit() && dog.distanceToSqr(player) > CinnamonRecoveryRules.FOLLOW_DISTANCE * CinnamonRecoveryRules.FOLLOW_DISTANCE;
		if (requested || unsafe || distant) {
			if (recall(player, dog, requested)) {
				REQUESTED.remove(player.getUUID()); BLOCKED.remove(player.getUUID());
				if (LEASE.heldBy(player.getUUID())) release();
				if (requested) message(player, "recall_ok");
			} else if (requested && ticketLevel == null) { REQUESTED.remove(player.getUUID()); message(player, "recall_blocked"); }
		} else if (LEASE.heldBy(player.getUUID())) release();
	}

	private static void createFirst(ServerPlayer player, CinnamonJournal journal) {
		if (!journal.canRegister(player.getUUID()) || identity(player, journal.get(player.getUUID())) != CinnamonIdentityRules.Status.NEW) return;
		CinnamonDog dog = CinnamonContent.CINNAMON.create(player.level(), EntitySpawnReason.EVENT);
		if (dog == null) return;
		dog.bind(player); // Old saves migrate their player-owned sit/bow choices once, here.
		Vec3 at = CompanionLanding.find(player.level(), player.position(), dog);
		if (at == null || !mayRecall(player) || !player.getUUID().equals(ownerId(dog))
			|| identity(player, journal.get(player.getUUID())) != CinnamonIdentityRules.Status.NEW) return;
		dog.snapTo(at.x, at.y, at.z, player.getYRot(), 0);
		var entry = new CinnamonJournal.Entry(dog.getUUID(), player.level().dimension().identifier().toString(),
			dog.chunkPosition().x(), dog.chunkPosition().z(), CinnamonJournal.Care.of(dog), 0, false);
		journal.put(player.getUUID(), entry);
		if (!player.level().addFreshEntity(dog)) journal.cancelInitial(player.getUUID(), dog.getUUID());
		else if (!dog.isRemoved() && LIVE.get(player.getUUID()) == dog) player.setAttached(CinnamonState.IDENTITY, dog.getUUID());
	}

	/** The whistle targets the server's identity, not data carried by the (freely copyable) item. */
	static void whistle(ServerPlayer player) {
		if (!matches(player)) { message(player, "recall_owner"); return; }
		if (!mayRecall(player)) { message(player, "recall_blocked"); return; }
		var journal = CinnamonJournal.of(player.level().getServer());
		var entry = journal.get(player.getUUID());
		if (identity(player, entry) == CinnamonIdentityRules.Status.UNCERTAIN) {
			if (WARNED_IDENTITY.contains(player.getUUID())) message(player, "recall_identity"); else warnIdentity(player, entry);
			return;
		}
		if (entry == null) { message(player, "recall_wait"); return; }
		if (entry.retired()) { message(player, "recall_retired"); return; }
		long now = player.level().getServer().overworld().getGameTime();
		if (now < entry.whistleUntil()) { message(player, "recall_cooldown"); return; }
		journal.put(player.getUUID(), entry.cooldown(now + CinnamonRecoveryRules.WHISTLE_TICKS));
		REQUESTED.add(player.getUUID()); BLOCKED.remove(player.getUUID());
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE,
			net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.8F);
		message(player, "recall_wait");
	}

	static void recallFollowing(CinnamonDog dog) {
		ServerPlayer player = ownerOf(dog);
		if (player != null && active(dog) && !dog.isOrderedToSit()) recall(player, dog, false);
	}
	private static boolean recall(ServerPlayer player, CinnamonDog dog, boolean follow) {
		UUID id = player.getUUID();
		if (recalling || !mayRecall(player) || dog.isRemoved() || LIVE.get(id) != dog || !id.equals(ownerId(dog)) || dog.isPassenger() || dog.isVehicle()) return false;
		var entry = CinnamonJournal.of(player.level().getServer()).get(id);
		if (entry == null || entry.retired() || !entry.entity().equals(dog.getUUID()) || !(dog.level() instanceof ServerLevel source)
			|| !source.hasChunkAt(dog.blockPosition()) || DungeonWards.warded(source, dog.blockPosition())) return false;
		recalling = true;
		try {
			Vec3 ownerPosition = player.position(), dogPosition = dog.position();
			ServerLevel ownerLevel = player.level();
			var dogBox = dog.getBoundingBox();
			long revision = dog.stateRevision();
			var sourcePermit = CompanionLanding.sourcePermit(player, source, dog);
			if (sourcePermit == null) return false;
			Vec3 at = CompanionLanding.find(ownerLevel, ownerPosition, dog);
			if (at == null || !mayRecall(player) || LIVE.get(id) != dog || !dog.isAlive() || dog.isRemoved()
				|| player.level() != ownerLevel || !player.position().equals(ownerPosition) || dog.level() != source
				|| !dog.position().equals(dogPosition) || !dog.getBoundingBox().equals(dogBox) || dog.stateRevision() != revision
				|| dog.isPassenger() || dog.isVehicle() || DungeonWards.warded(source, dog.blockPosition())
				|| !id.equals(ownerId(dog)) || identity(player, entry) != CinnamonIdentityRules.Status.REGISTERED
				|| CinnamonJournal.of(source.getServer()).get(id) != entry || !sourcePermit.valid(player)) return false;
			ServerLevel destination = player.level();
			changed(dog);
			if (source == destination) {
				dog.teleportTo(at.x, at.y, at.z); dog.setDeltaMovement(Vec3.ZERO); dog.resetFallDistance();
			} else {
				Entity moved = dog.teleport(new TeleportTransition(destination, at, Vec3.ZERO, player.getYRot(), 0, TeleportTransition.DO_NOTHING));
				if (!(moved instanceof CinnamonDog next) || next.isRemoved() || !next.getUUID().equals(entry.entity())) return false;
				dog = next;
				if (LIVE.get(id) != dog) loaded(dog, destination);
			}
			if (LIVE.get(id) != dog || dog.isRemoved()) return false;
			if (follow) dog.followOwner();
			dog.setTarget(null); dog.getNavigation().stop(); changed(dog);
			return true;
		} finally { recalling = false; }
	}

	private static void recoverChunk(ServerPlayer player, CinnamonJournal.Entry entry) {
		ServerLevel level;
		try { level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.dimension()))); }
		catch (RuntimeException invalid) { recoveryFailed(player); return; }
		if (level == null || Math.abs((long) entry.chunkX()) > 1_875_000 || Math.abs((long) entry.chunkZ()) > 1_875_000) {
			recoveryFailed(player); return;
		}
		if (!LEASE.acquire(player.getUUID(), player.level().getServer().overworld().getGameTime())) return;
		ticketLevel = level; ticketChunk = new ChunkPos(entry.chunkX(), entry.chunkZ());
		// Only the journal's existing body location. No destination tickets, searches or moving self-loading trail.
		level.getChunkSource().addTicketWithRadius(RECOVERY, ticketChunk, CinnamonRecoveryRules.TICKET_RADIUS);
	}
	private static CinnamonIdentityRules.Status identity(ServerPlayer player, CinnamonJournal.Entry entry) {
		return CinnamonIdentityRules.status(player.getAttached(CinnamonState.IDENTITY), entry == null ? null : entry.entity(),
			uncertainOverflow || UNCERTAIN_OWNERS.contains(player.getUUID()));
	}
	private static void uncertain(UUID owner) {
		if (UNCERTAIN_OWNERS.size() < CinnamonJournal.MAX_OWNERS || UNCERTAIN_OWNERS.contains(owner)) UNCERTAIN_OWNERS.add(owner);
		else uncertainOverflow = true;
	}
	private static void warnIdentity(ServerPlayer player, CinnamonJournal.Entry entry) {
		if (WARNED_IDENTITY.size() >= CinnamonJournal.MAX_OWNERS || !WARNED_IDENTITY.add(player.getUUID())) return;
		message(player, "recall_identity");
		Wildercord.LOGGER.warn("Cinnamon recovery paused for owner {}: owner marker {}, journal identity {}. Preserve entity/player data and restore matching records from backup; no replacement was created.",
			player.getUUID(), player.getAttached(CinnamonState.IDENTITY), entry == null ? "missing" : entry.entity());
	}
	private static void recoveryFailed(ServerPlayer player) {
		BLOCKED.add(player.getUUID());
		if (REQUESTED.remove(player.getUUID())) message(player, "recall_missing");
	}
	private static void release() {
		if (ticketLevel != null) ticketLevel.getChunkSource().removeTicketWithRadius(RECOVERY, ticketChunk, CinnamonRecoveryRules.TICKET_RADIUS);
		ticketLevel = null; ticketChunk = null; LEASE.clear();
	}
	private static void clear() { release(); LIVE.clear(); BLOCKED.clear(); REQUESTED.clear(); UNCERTAIN_OWNERS.clear(); WARNED_IDENTITY.clear(); uncertainOverflow = false; lastModified = -1; recalling = false; }
	private static void message(ServerPlayer player, String key) { player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon." + key)); }
	private static boolean matches(ServerPlayer player) {
		if (config.owner().isBlank()) return false;
		if (config.owner().equalsIgnoreCase("@singleplayer")) {
			var profile = player.level().getServer().getSingleplayerProfile();
			return profile != null && (profile.id().equals(player.getUUID()) || profile.name().equalsIgnoreCase(player.getGameProfile().name()));
		}
		return config.ownerId() != null ? player.getUUID().equals(config.ownerId()) : player.getGameProfile().name().equalsIgnoreCase(config.owner());
	}
	static ServerPlayer configuredOwner(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) if (matches(player)) return player;
		return null;
	}
	static boolean bell() { return config.bell(); }
	static int recoveryTickets() { return ticketLevel == null ? 0 : 1; }
	static long recoveryDeadline() { return LEASE.deadline(); }
	private static void readConfig() {
		try {
			if (Files.notExists(CONFIG)) {
				Files.createDirectories(CONFIG.getParent());
				Files.writeString(CONFIG, "{\n  \"owner\": \"\"\n}\n", StandardCharsets.UTF_8);
			}
			long modified = Files.getLastModifiedTime(CONFIG).toMillis();
			if (modified == lastModified) return;
			if (Files.size(CONFIG) > 65_536) throw new IOException("Cinnamon config exceeds 64 KiB");
			CinnamonConfig next = CinnamonConfig.parse(Files.readString(CONFIG, StandardCharsets.UTF_8));
			if (!config.owner().equalsIgnoreCase(next.owner())) { release(); REQUESTED.clear(); BLOCKED.clear(); }
			config = next; // One atomic authority update, only after every field validated.
			lastModified = modified;
		} catch (IOException | RuntimeException e) { Wildercord.LOGGER.warn("Could not read Cinnamon owner config {}: {}", CONFIG, e.toString()); }
	}
}
