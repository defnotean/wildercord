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
 * the spell defences, Aura Slash's price, whether runes start unread, and sword strings' switch and window, for the Cord screen,
 * the HUD and the string reader) are sent to each player
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
			boolean masteryTraits, boolean unreadRunes, boolean aura, float slashCost, boolean forgedGear, float sashCapacity, boolean strings,
			int stringWindow, int combat) implements CustomPacketPayload {
		/**
		 * Bits of {@link #combat}: momentum works; foes have a stance (and finishers); swordsmen can awaken. Later parts of aura's fighting
		 * add their own in the free low bits. Bits 8 to 15 carry the momentum an awakening asks for (0 to 100), which the swordsman's own
		 * client needs to know when an awakening is ready (its HUD, and whether a lone tap of the Aura key waits for a second).
		 */
		public static final int MOMENTUM = 1;
		public static final int STANCE = 2;
		public static final int AWAKENING = 4;
		/** Ways work (the crossroads, and every node in force): the Aura page and the swordsman's own reading of their nodes. */
		public static final int WAYS = 8;
		/** Techniques of one's own work (writing them, playing them): the Aura page's writing page and the string reader. */
		public static final int TECHNIQUES = 16;
		/** Bonded blades work (the ceremony, resonance, a blade's gifts): the Aura page's Blade tab and the swordsman's own prices. */
		public static final int BONDS = 32;
		/** An Awakened blade's trait works: the reader's prices and rests, the HUD's slash price. */
		public static final int BLADE_TRAITS = 64;
		/** Sparring works (a salute with the blade challenges): the swordsman's own client takes the salute for itself, not for the item. */
		public static final int SPARRING = 128;
		/** Stone Hinge is out of testing (the experimental switch): the Masters forms page offers it rather than showing it locked. */
		public static final int STONE_HINGE = 1 << 16;
		private static final int AWAKENING_MOMENTUM_SHIFT = 8;
		public static final Sync DEFAULT = new Sync(1.0F, 1.0F, true, WildercordConfig.DefenceSettings.DEFAULTS, true, true, true, true,
			(float) WildercordConfig.AuraSettings.DEFAULTS.slashCost(), true, (float) WildercordConfig.AuraWorldSettings.DEFAULTS.sashCapacity(), true,
			WildercordConfig.AuraStrings.DEFAULTS.windowTicks(), combat(WildercordConfig.AuraSettings.DEFAULTS));
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
			ByteBufCodecs.FLOAT, Sync::sashCapacity, ByteBufCodecs.BOOL, Sync::strings, ByteBufCodecs.VAR_INT, Sync::stringWindow, ByteBufCodecs.VAR_INT,
			Sync::combat, Sync::new).cast();

		static Sync of(WildercordConfig config) {
			return new Sync((float) config.manaCostMultiplier(), (float) config.manaRegenMultiplier(), config.playerAffinity(), config.defence(),
				config.mastery().enabled(), config.mastery().enabled() && config.mastery().traits(), config.unreadRunes(), config.aura().enabled(),
				(float) config.aura().slashCost(), config.auraWorld().forgedGear(), (float) config.auraWorld().sashCapacity(),
				config.aura().strings().enabled(), config.aura().strings().windowTicks(),
				combat(config.aura()));
		}

		/** The {@link #combat} bits for {@code aura}'s settings. */
		static int combat(WildercordConfig.AuraSettings aura) {
			int needed = (int) Math.round(Math.max(0, Math.min(100, aura.awakening().awakeningMomentum())));
			return (aura.momentum().momentum() ? MOMENTUM : 0) | (aura.momentum().stance() ? STANCE : 0) | (aura.awakening().awakening() ? AWAKENING : 0)
				| (aura.ways().ways() ? WAYS : 0) | (aura.techniques().techniques() ? TECHNIQUES : 0) | (aura.bonds().bonds() ? BONDS : 0)
				| (aura.bonds().traits() ? BLADE_TRAITS : 0) | (aura.sparring().sparring() ? SPARRING : 0) | needed << AWAKENING_MOMENTUM_SHIFT
				| (aura.sparring().experimentalStoneHinge() ? STONE_HINGE : 0);
		}

		/** The momentum an awakening asks for, as the bits carry it. */
		public int awakeningMomentum() {
			return (combat >> AWAKENING_MOMENTUM_SHIFT) & 0xFF;
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

	/** Whether sword strings set off arts: the server's own switch, or on a client the one it was sent (the reader stays quiet without). */
	public static boolean strings(Player player) {
		return player != null && player.level().isClientSide() ? synced.strings() : get().aura().strings().enabled();
	}

	/** Sword strings' window (ticks after the blade is ready again): the server's own, or on a client the one it was sent. */
	public static int stringWindow(Player player) {
		return player != null && player.level().isClientSide() ? synced.stringWindow() : get().aura().strings().windowTicks();
	}

	/** Whether momentum works: the server's own setting, or on a client the one it was sent. */
	public static boolean momentum(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.MOMENTUM) != 0 : get().aura().momentum().momentum();
	}

	/** Whether foes have a stance (and finishers): the server's own setting, or on a client the one it was sent. */
	public static boolean stance(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.STANCE) != 0 : get().aura().momentum().stance();
	}

	/** Whether swordsmen can awaken: the server's own setting, or on a client the one it was sent. */
	public static boolean awakening(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.AWAKENING) != 0 : get().aura().awakening().awakening();
	}

	/** Whether Ways work (the crossroads, and every node in force): the server's own setting, or on a client the one it was sent. */
	public static boolean ways(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.WAYS) != 0 : get().aura().ways().ways();
	}

	/** Whether techniques of one's own work (writing and playing them): the server's own setting, or on a client the one it was sent. */
	public static boolean techniques(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.TECHNIQUES) != 0 : get().aura().techniques().techniques();
	}

	/** Whether bonded blades work (bonding, resonance, a blade's gifts): the server's own setting, or on a client the one it was sent. */
	public static boolean bonds(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.BONDS) != 0 : get().aura().bonds().bonds();
	}

	/** Whether an Awakened blade's trait works: the server's own setting, or on a client the one it was sent. */
	public static boolean bladeTraits(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.BLADE_TRAITS) != 0 : get().aura().bonds().traits();
	}

	/** Whether swordsmen can spar (a salute answered opens the ring): the server's own setting, or on a client the one it was sent. */
	public static boolean sparring(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.SPARRING) != 0 : get().aura().sparring().sparring();
	}

	/** Whether Stone Hinge is out of testing (taught and equippable): the server's own switch, or on a client the one it was sent. */
	public static boolean stoneHinge(Player player) {
		return player != null && player.level().isClientSide() ? (synced.combat() & Sync.STONE_HINGE) != 0
			: get().aura().sparring().experimentalStoneHinge();
	}

	/** The momentum an awakening asks for: the server's own setting, or on a client the one it was sent. */
	public static double awakeningMomentum(Player player) {
		return player != null && player.level().isClientSide() ? synced.awakeningMomentum() : get().aura().awakening().awakeningMomentum();
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
