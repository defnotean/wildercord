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
 * <p>The few settings a client shows (the cost and regeneration multipliers, whether affinities, spell mastery and aura are on,
 * the spell defences, Aura Slash's price, and whether runes start unread, for the Cord screen and HUD) are sent to each player
 * when they join and after every reload; {@link #costMultiplier}, {@link #regenMultiplier},
 * {@link #playerAffinity}, {@link #defence} and {@link #unreadRunes} answer with those on the client.</p>
 */
public final class Config {
	private Config() {}

	public static final String FILE = "wildercord.json";

	private static volatile WildercordConfig current;
	/** What the server told this client (the defaults until it does). */
	private static volatile Sync synced = Sync.DEFAULT;

	/** Server to client: the settings a client needs to show costs, regeneration, affinities and spell defences truthfully. */
	public record Sync(float costMultiplier, float regenMultiplier, boolean playerAffinity, WildercordConfig.DefenceSettings defence, boolean mastery,
			boolean masteryTraits, boolean unreadRunes, boolean aura, float slashCost, boolean forgedGear, float sashCapacity) implements CustomPacketPayload {
		public static final Sync DEFAULT = new Sync(1.0F, 1.0F, true, WildercordConfig.DefenceSettings.DEFAULTS, true, true, true, true,
			(float) WildercordConfig.AuraSettings.DEFAULTS.slashCost(), true, (float) WildercordConfig.AuraWorldSettings.DEFAULTS.sashCapacity());
		public static final Type<Sync> TYPE = new Type<>(Wildercord.id("config_sync"));
		/** The spell defences as they travel, for the Cord screen's readout. Here, before CODEC, so it exists when CODEC is made. */
		private static final StreamCodec<io.netty.buffer.ByteBuf, WildercordConfig.DefenceSettings> DEFENCE_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, WildercordConfig.DefenceSettings::spellguard, ByteBufCodecs.DOUBLE, WildercordConfig.DefenceSettings::spellguardHealth,
			ByteBufCodecs.VAR_INT, WildercordConfig.DefenceSettings::spellguardRechargeSeconds, ByteBufCodecs.DOUBLE, WildercordConfig.DefenceSettings::maxBonus,
			ByteBufCodecs.DOUBLE, WildercordConfig.DefenceSettings::armourRate, WildercordConfig.DefenceSettings::new);
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Sync::costMultiplier, ByteBufCodecs.FLOAT, Sync::regenMultiplier, ByteBufCodecs.BOOL, Sync::playerAffinity,
			DEFENCE_CODEC, Sync::defence, ByteBufCodecs.BOOL, Sync::mastery, ByteBufCodecs.BOOL, Sync::masteryTraits, ByteBufCodecs.BOOL,
			Sync::unreadRunes, ByteBufCodecs.BOOL, Sync::aura, ByteBufCodecs.FLOAT, Sync::slashCost, ByteBufCodecs.BOOL, Sync::forgedGear,
			ByteBufCodecs.FLOAT, Sync::sashCapacity, Sync::new).cast();

		static Sync of(WildercordConfig config) {
			return new Sync((float) config.manaCostMultiplier(), (float) config.manaRegenMultiplier(), config.playerAffinity(), config.defence(),
				config.mastery().enabled(), config.mastery().enabled() && config.mastery().traits(), config.unreadRunes(), config.aura().enabled(),
				(float) config.aura().slashCost(), config.auraWorld().forgedGear(), (float) config.auraWorld().sashCapacity());
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
			String text = Files.readString(path, StandardCharsets.UTF_8);
			parsed = WildercordConfig.parse(text);
			// A file from an older version gains the settings added since, at their defaults, so they can be seen and changed.
			java.util.Optional<String> grown = WildercordConfig.addMissing(text);
			if (grown.isPresent()) {
				try {
					Files.writeString(path, grown.get(), StandardCharsets.UTF_8);
					Wildercord.LOGGER.info("Added the newer settings to {} at their defaults", path);
				} catch (IOException e) {
					// A file that can't be written (a read-only mount, say) still counts as read: the owner's settings hold.
					Wildercord.LOGGER.warn("Couldn't add the newer settings to {} ({}); they run at their defaults", path, e.toString());
				}
			}
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
			// The world's own magic may have been switched, counted or rerolled: everyone's view of it is worked out again.
			dev.wildercord.cast.WorldResonances.reloaded(server);
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

	/** Whether players' affinities are on: the server's own switch, or on a client the one it was sent. */
	public static boolean playerAffinity(Player player) {
		return player != null && player.level().isClientSide() ? synced.playerAffinity() : get().playerAffinity();
	}

	/** The spell defences: the server's own, or on a client the ones it was sent. */
	public static WildercordConfig.DefenceSettings defence(Player player) {
		return player != null && player.level().isClientSide() ? synced.defence() : get().defence();
	}

	/** Whether spell mastery is on: the server's own switch, or on a client the one it was sent (the Cord screen shows ranks only then). */
	public static boolean mastery(Player player) {
		return player != null && player.level().isClientSide() ? synced.mastery() : get().mastery().enabled();
	}

	/** Whether the traits players chose for their spells take effect: the server's own switches, or on a client the one it was sent. */
	public static boolean masteryTraits(Player player) {
		return player != null && player.level().isClientSide() ? synced.masteryTraits() : get().mastery().enabled() && get().mastery().traits();
	}

	/** Whether newly learned runes start unread (see {@code spell.RuneReading}): the server's own switch, or on a client the one it was sent. */
	public static boolean unreadRunes(Player player) {
		return player != null && player.level().isClientSide() ? synced.unreadRunes() : get().unreadRunes();
	}

	/** Whether aura (the swordsman's path) works: the server's own switch, or on a client the one it was sent. */
	public static boolean aura(Player player) {
		return player != null && player.level().isClientSide() ? synced.aura() : get().aura().enabled();
	}

	/** Aura Slash's price: the server's own, or on a client the one it was sent (the HUD marks it on the aura bar). */
	public static double slashCost(Player player) {
		return player != null && player.level().isClientSide() ? synced.slashCost() : get().aura().slashCost();
	}

	/** Whether aura-forged gear and the Breath Sash do what they do for aura: the server's own switch, or on a client the one it was sent. */
	public static boolean forgedGear(Player player) {
		return player != null && player.level().isClientSide() ? synced.forgedGear() : get().auraWorld().forgedGear();
	}

	/** What the Breath Sash multiplies aura capacity by: the server's own, or on a client the one it was sent (for the aura bar). */
	public static double sashCapacity(Player player) {
		return player != null && player.level().isClientSide() ? synced.sashCapacity() : get().auraWorld().sashCapacity();
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
