package dev.wildercord.pet;

import com.google.gson.JsonParser;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps exactly one Cinnamon beside the configured player, including across logins and dimensions. */
final class CinnamonCompanion {
	private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("wildercord-cinnamon.json");
	private static final Map<UUID, CinnamonDog> LIVE = new HashMap<>();
	private static String owner = "";
	private static UUID ownerId;
	private static long lastModified = -1;
	private CinnamonCompanion() {}

	static void init() {
		readConfig();
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof CinnamonDog dog) {
				ServerPlayer player = configuredOwner(level.getServer());
				if (player != null && player.level() == level) adopt(player, dog);
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof CinnamonDog dog) LIVE.values().removeIf(current -> current == dog);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) return;
			if (server.getTickCount() % 100 == 0) readConfig();
			for (Map.Entry<UUID, CinnamonDog> entry : Map.copyOf(LIVE).entrySet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				CinnamonDog dog = entry.getValue();
				if (player == null || !matches(player) || dog.isRemoved() || dog.level() != player.level()) {
					dog.discard();
					LIVE.remove(entry.getKey(), dog);
				}
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (!matches(player)) continue;
				CinnamonDog current = LIVE.get(player.getUUID());
				// Repair only after configuration changes or an unusual external entity move.
				if (server.getTickCount() % 200 == 0 || current == null) {
					var nearby = player.level().getEntitiesOfClass(CinnamonDog.class, player.getBoundingBox().inflate(12));
					CinnamonDog found = nearby.stream().filter(dog -> dog != current)
						.min(java.util.Comparator.comparingInt(dog -> dog.tickCount)).orElse(null);
					if (found != null) { adopt(player, found); continue; }
				}
				if (current != null) {
					if (current.getY() < player.level().getMinY() + 2 || !player.level().noCollision(current, current.getBoundingBox())) {
						var safe = CompanionLanding.find(player.level(), player.position(), current);
						if (safe != null) { current.teleportTo(safe.x, safe.y, safe.z); current.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); }
					}
					continue;
				}
				CinnamonDog dog = CinnamonContent.CINNAMON.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
				if (dog == null) continue;
				var safe = CompanionLanding.find(player.level(), player.position(), dog);
				if (safe == null) continue;
				dog.bind(player);
				dog.snapTo(safe.x, safe.y, safe.z, player.getYRot(), 0);
				if (player.level().addFreshEntity(dog)) LIVE.put(player.getUUID(), dog);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> {
			CinnamonDog dog = LIVE.remove(handler.player.getUUID());
			if (dog != null) dog.discard();
		}));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LIVE.clear();
			lastModified = -1;
		});
	}

	private static void adopt(ServerPlayer player, CinnamonDog dog) {
		CinnamonDog previous = LIVE.put(player.getUUID(), dog);
		if (previous != null && previous != dog) previous.discard();
		dog.bind(player);
	}

	private static boolean matches(ServerPlayer player) {
		if (owner.isBlank()) return false;
		if (owner.equalsIgnoreCase("@singleplayer")) {
			var profile = player.level().getServer().getSingleplayerProfile();
			return profile != null && profile.id().equals(player.getUUID());
		}
		return ownerId != null ? player.getUUID().equals(ownerId) : player.getGameProfile().name().equalsIgnoreCase(owner);
	}

	static ServerPlayer configuredOwner(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (matches(player)) return player;
		}
		return null;
	}

	private static void readConfig() {
		try {
			if (Files.notExists(CONFIG)) {
				Files.createDirectories(CONFIG.getParent());
				Files.writeString(CONFIG, "{\n  \"owner\": \"\"\n}\n", StandardCharsets.UTF_8);
				Wildercord.LOGGER.info("Created Cinnamon owner config at {}", CONFIG);
			}
			long modified = Files.getLastModifiedTime(CONFIG).toMillis();
			if (modified == lastModified) return;
			owner = JsonParser.parseString(Files.readString(CONFIG, StandardCharsets.UTF_8)).getAsJsonObject().get("owner").getAsString().trim();
			try { ownerId = UUID.fromString(owner); } catch (IllegalArgumentException ignored) { ownerId = null; }
			lastModified = modified;
		} catch (IOException | RuntimeException e) {
			Wildercord.LOGGER.warn("Could not read Cinnamon owner config {}: {}", CONFIG, e.toString());
		}
	}
}
