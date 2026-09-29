package dev.wildercord.config;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The server's live settings: {@code config/wildercord.json}, written with every default the first
 * time it's needed and read again by {@code /wildercord reload}. Code reads {@link #get()} each time
 * it needs a value (it's cheap), so a reload takes effect at once, except loot chances, which apply
 * when loot tables next load.
 *
 * <p>The few numbers a client shows (the cost and regeneration multipliers, for the Cord screen and
 * HUD) are sent to each player when they join and after every reload; {@link #costMultiplier} and
 * {@link #regenMultiplier} answer with those on the client.</p>
 */
public final class Config {
	private Config() {}

	public static final String FILE = "wildercord.json";

	private static volatile WildercordConfig current;
	/** What the server told this client (the defaults until it does). */
	private static volatile Sync synced = Sync.DEFAULT;

	/** Server to client: the settings a client needs to show costs and regeneration truthfully. */
	public record Sync(float costMultiplier, float regenMultiplier) implements CustomPacketPayload {
		public static final Sync DEFAULT = new Sync(1.0F, 1.0F);
		public static final Type<Sync> TYPE = new Type<>(Wildercord.id("config_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Sync::costMultiplier, ByteBufCodecs.FLOAT, Sync::regenMultiplier, Sync::new).cast();

		static Sync of(WildercordConfig config) {
			return new Sync((float) config.manaCostMultiplier(), (float) config.manaRegenMultiplier());
		}

		@Override
		public Type<Sync> type() {
			return TYPE;
		}
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE);
	}

	/** The settings in force on this server (loaded the first time they're asked for). */
	public static WildercordConfig get() {
		WildercordConfig config = current;
		if (config == null) {
			synchronized (Config.class) {
				if (current == null) {
					load();
				}
				config = current;
			}
		}
		return config;
	}

	/**
	 * Reads the file (writing the defaults first if there is none) and puts it in force.
	 *
	 * @return what was wrong with it, each already fixed (empty when it was fine)
	 */
	public static synchronized List<String> load() {
		Path path = path();
		WildercordConfig.Parsed parsed;
		try {
			if (Files.notExists(path)) {
				Files.createDirectories(path.getParent());
				Files.writeString(path, WildercordConfig.DEFAULTS.toJson(), StandardCharsets.UTF_8);
				Wildercord.LOGGER.info("Wrote the default config to {}", path);
			}
			parsed = WildercordConfig.parse(Files.readString(path, StandardCharsets.UTF_8));
		} catch (IOException e) {
			Wildercord.LOGGER.warn("Couldn't read {}: {}; using the defaults", path, e.toString());
			parsed = new WildercordConfig.Parsed(WildercordConfig.DEFAULTS, List.of("couldn't read the file (" + e.getMessage() + "); using the defaults"));
		}
		for (String warning : parsed.warnings()) {
			Wildercord.LOGGER.warn("{}: {}", FILE, warning);
		}
		current = parsed.config();
		return parsed.warnings();
	}

	/** Reloads the file and tells every player the numbers their screens need. */
	public static List<String> reload(MinecraftServer server) {
		List<String> warnings = load();
		if (server != null) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				ServerPlayNetworking.send(player, Sync.of(get()));
			}
		}
		return warnings;
	}

	/** Mana cost multiplier: the server's own, or on a client the one it was sent. */
	public static double costMultiplier(Player player) {
		return player != null && player.level().isClientSide() ? synced.costMultiplier() : get().manaCostMultiplier();
	}

	/** Mana regeneration multiplier: the server's own, or on a client the one it was sent. */
	public static double regenMultiplier(Player player) {
		return player != null && player.level().isClientSide() ? synced.regenMultiplier() : get().manaRegenMultiplier();
	}

	/** Client side: the server's numbers arrived (or, with {@code null}, the connection closed). */
	public static void receive(Sync sync) {
		synced = sync == null ? Sync.DEFAULT : sync;
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Sync.TYPE, Sync.CODEC);
		// Read (or write) the file as the server starts, so its warnings show up at start and not mid-game. Loot
		// tables load (and read it) before this, so it's read once per start, fresh: a stopped server forgets it,
		// and the next world in the same game reads the file as it is then.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> get());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> current = null);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(Sync.of(get())));
	}
}
