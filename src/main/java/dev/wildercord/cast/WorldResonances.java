package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Resonance;
import dev.wildercord.spell.ResonanceForge;
import dev.wildercord.spell.ResonanceLore;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneQuirks;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This world's own magic, at runtime: its resonances and rune quirks, drawn from the world's seed (see
 * {@link ResonanceForge}), kept with the overworld's saved data along with who found each resonance first, and woken by
 * casting their exact runes.
 *
 * <p>The draw is saved rather than worked out afresh at every start, so a later version of the mod, with runes added to
 * the roster, never shifts a world's resonances under its players' feet; it's drawn again only when the owner changes
 * the count or the reroll salt (or a rune it used is gone). Nothing here is ever sent to a client: each player is sent
 * {@link ResonanceLore}'s view of it, through the {@code world_lore} attachment, and that only.</p>
 *
 * <p>The small API other systems can build on: {@link #discovered}, {@link #of}, {@link #match}, {@link #addWakeCondition}
 * (an extra condition a resonance must meet before it wakes) and {@link #onFound} (told of each find).</p>
 */
public final class WorldResonances {
	private WorldResonances() {}

	// ------------------------------------------------------------------ hooks for other systems

	/**
	 * An extra condition a resonance must meet before it wakes: a place of power it must be cast at, a reagent that must
	 * be in hand. Every condition is asked; one that doesn't concern a resonance answers true. A resonance held back by a
	 * condition isn't found and has no twist: the cast is only the ordinary spell, with a faint shimmer that something
	 * almost answered.
	 */
	@FunctionalInterface
	public interface WakeCondition {
		boolean allows(ServerPlayer caster, Resonance resonance);
	}

	/** Told of each resonance a player finds, the first time they do, and whether nobody in the world had found it before. */
	@FunctionalInterface
	public interface FoundListener {
		void found(ServerPlayer player, Resonance resonance, boolean firstInWorld);
	}

	private static final Map<String, WakeCondition> CONDITIONS = new LinkedHashMap<>();
	private static final List<FoundListener> LISTENERS = new CopyOnWriteArrayList<>();

	/** Adds (or, with the same id, replaces) a condition every resonance must meet to wake. */
	public static synchronized void addWakeCondition(String id, WakeCondition condition) {
		CONDITIONS.put(id, condition);
	}

	public static synchronized void removeWakeCondition(String id) {
		CONDITIONS.remove(id);
	}

	private static synchronized List<WakeCondition> conditions() {
		return List.copyOf(CONDITIONS.values());
	}

	public static void onFound(FoundListener listener) {
		LISTENERS.add(listener);
	}

	/** Whether {@code player} has found resonance {@code id}. Safe on both sides (it reads the synced Grimoire). */
	public static boolean discovered(Player player, String id) {
		return Heart.discovered(player, Resonance.KEY_PREFIX + id);
	}

	/** This world's resonances (server side only: never send them anywhere). Empty when the owner has switched them off. */
	public static List<Resonance> of(MinecraftServer server) {
		return enabled() ? ledger(server).resonances : List.of();
	}

	/** This world's rune quirks (server side only). Empty when the owner has switched resonances off. */
	public static List<RuneQuirks.Quirk> quirks(MinecraftServer server) {
		return enabled() ? ledger(server).quirks : List.of();
	}

	/** The resonance {@code runes} spell out exactly in this world, if any. */
	public static Optional<Resonance> match(MinecraftServer server, List<RuneDef> runes) {
		if (runes.size() < 3 || runes.size() > 4) {
			return Optional.empty();
		}
		for (Resonance resonance : of(server)) {
			if (resonance.matches(runes)) {
				return Optional.of(resonance);
			}
		}
		return Optional.empty();
	}

	public static boolean enabled() {
		return Config.get().resonances().enabled();
	}

	// ------------------------------------------------------------------ the saved draw

	/** Who found a resonance first, and when (a game time). */
	record Finding(String name, String uuid, long time) {
		static final Codec<Finding> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(Finding::name),
			Codec.STRING.fieldOf("uuid").forGetter(Finding::uuid),
			Codec.LONG.fieldOf("time").forGetter(Finding::time)
		).apply(i, Finding::new));
	}

	/** The world's draw and who found what, kept with the overworld. */
	public static final class Ledger extends SavedData {
		static final Codec<Resonance> RESONANCE = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(Resonance::id),
			Codec.STRING.fieldOf("name").forGetter(Resonance::name),
			Codec.STRING.fieldOf("riddle").forGetter(Resonance::riddle),
			Codec.STRING.listOf().fieldOf("runes").forGetter(Resonance::runes),
			Codec.STRING.fieldOf("twist").forGetter(Resonance::twist),
			Codec.INT.fieldOf("color").forGetter(Resonance::color)
		).apply(i, Resonance::new));
		static final Codec<RuneQuirks.Quirk> QUIRK = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(RuneQuirks.Quirk::id),
			Codec.STRING.fieldOf("rune").forGetter(RuneQuirks.Quirk::rune),
			Codec.STRING.xmap(Ledger::kind, Enum::name).fieldOf("kind").forGetter(RuneQuirks.Quirk::kind),
			Codec.STRING.xmap(Ledger::when, Enum::name).fieldOf("when").forGetter(RuneQuirks.Quirk::when),
			Codec.STRING.fieldOf("text").forGetter(RuneQuirks.Quirk::text)
		).apply(i, RuneQuirks.Quirk::new));
		static final Codec<Ledger> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("drawn", false).forGetter(l -> l.drawn),
			Codec.LONG.optionalFieldOf("seed", 0L).forGetter(l -> l.seed),
			Codec.STRING.optionalFieldOf("salt", "").forGetter(l -> l.salt),
			Codec.INT.optionalFieldOf("count", 0).forGetter(l -> l.count),
			Codec.INT.optionalFieldOf("quirk_count", 0).forGetter(l -> l.quirkCount),
			RESONANCE.listOf().optionalFieldOf("resonances", List.of()).forGetter(l -> l.resonances),
			QUIRK.listOf().optionalFieldOf("quirks", List.of()).forGetter(l -> l.quirks),
			Codec.unboundedMap(Codec.STRING, Finding.CODEC).optionalFieldOf("found", Map.of()).forGetter(l -> l.found)
		).apply(i, Ledger::new));
		static final SavedDataType<Ledger> TYPE = new SavedDataType<>(Wildercord.id("resonances"), Ledger::new, CODEC, null);

		private boolean drawn;
		private long seed;
		private String salt = "";
		private int count;
		private int quirkCount;
		private List<Resonance> resonances = List.of();
		private List<RuneQuirks.Quirk> quirks = List.of();
		private final Map<String, Finding> found = new HashMap<>();

		public Ledger() {
		}

		private Ledger(boolean drawn, long seed, String salt, int count, int quirkCount, List<Resonance> resonances, List<RuneQuirks.Quirk> quirks,
				Map<String, Finding> found) {
			this.drawn = drawn;
			this.seed = seed;
			this.salt = salt;
			this.count = count;
			this.quirkCount = quirkCount;
			this.resonances = List.copyOf(resonances);
			this.quirks = List.copyOf(quirks);
			this.found.putAll(found);
		}

		private static RuneQuirks.Kind kind(String name) {
			try {
				return RuneQuirks.Kind.valueOf(name);
			} catch (IllegalArgumentException e) {
				return RuneQuirks.Kind.STRONGER;
			}
		}

		private static RuneQuirks.When when(String name) {
			try {
				return RuneQuirks.When.valueOf(name);
			} catch (IllegalArgumentException e) {
				return RuneQuirks.When.NIGHT;
			}
		}

		static Ledger of(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(TYPE);
		}

		boolean drawnFor(long seed, WildercordConfig.ResonanceSettings settings) {
			return drawn && this.seed == seed && salt.equals(settings.rerollSalt()) && count == settings.count() && quirkCount == settings.quirks();
		}

		/** Whether every rune and twist the draw uses is still known: a removed add-on's, or a renamed one's, isn't. */
		boolean sound() {
			for (Resonance resonance : resonances) {
				if (resonance.defs().isEmpty() || resonance.twistDef().isEmpty()) {
					return false;
				}
			}
			for (RuneQuirks.Quirk quirk : quirks) {
				if (dev.wildercord.spell.Runes.get(quirk.rune()).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		/** Takes a fresh draw. Finds of resonances still in it are kept (a larger count keeps the first ones); the rest are forgotten. */
		void redraw(long seed, WildercordConfig.ResonanceSettings settings) {
			List<Resonance> fresh = ResonanceForge.forge(seed, settings.rerollSalt(), settings.count());
			this.drawn = true;
			this.seed = seed;
			this.salt = settings.rerollSalt();
			this.count = settings.count();
			this.quirkCount = settings.quirks();
			this.resonances = fresh;
			this.quirks = RuneQuirks.forge(seed, settings.rerollSalt(), settings.quirks());
			found.keySet().removeIf(id -> fresh.stream().noneMatch(r -> r.id().equals(id)));
			setDirty();
		}

		/** Records {@code player} as the first to find {@code id}; false if somebody already had. */
		boolean record(String id, ServerPlayer player) {
			if (found.containsKey(id)) {
				return false;
			}
			found.put(id, new Finding(player.getGameProfile().name(), player.getUUID().toString(), player.level().getGameTime()));
			setDirty();
			return true;
		}

		Map<String, String> finders() {
			Map<String, String> names = new HashMap<>();
			found.forEach((id, finding) -> names.put(id, finding.name()));
			return names;
		}
	}

	/** The seed and settings the ledger in force was prepared under: when the owner changes them, it's prepared again. */
	private record Prepared(MinecraftServer server, long seed, WildercordConfig.ResonanceSettings settings) {}

	private static volatile Prepared prepared;

	/** The world's ledger, drawn (or drawn again) as its settings need. */
	static Ledger ledger(MinecraftServer server) {
		WildercordConfig.ResonanceSettings settings = Config.get().resonances();
		long seed = server.overworld().getSeed();
		Prepared now = prepared;
		Ledger ledger = Ledger.of(server);
		if (now == null || now.server() != server || now.seed() != seed || !now.settings().equals(settings)) {
			if (!ledger.drawnFor(seed, settings) || !ledger.sound()) {
				ledger.redraw(seed, settings);
				Wildercord.LOGGER.info("Drew this world's magic: {} resonances and {} rune quirks", ledger.resonances.size(), ledger.quirks.size());
			}
			boolean changed = now != null && now.server() == server;
			prepared = new Prepared(server, seed, settings);
			WorldQuirks.forget();
			if (changed) {
				// The owner changed the settings (a reload): everyone's view of the world may have changed with them.
				refreshAll(server);
			}
		}
		return ledger;
	}

	// ------------------------------------------------------------------ waking one

	/**
	 * A spell left a player's hands: if its runes are one of this world's resonances (and every wake condition allows
	 * it), it's found, and its twist rides the cast. Called by {@link SpellCaster} for spells that aren't secrets.
	 */
	public static void wake(ServerPlayer player, List<RuneDef> runes, Cast cast) {
		if (!enabled()) {
			return;
		}
		Optional<Resonance> match = match(player.level().getServer(), runes);
		if (match.isEmpty()) {
			return;
		}
		Resonance resonance = match.get();
		for (WakeCondition condition : conditions()) {
			if (!condition.allows(player, resonance)) {
				TwistVfx.almost(player.level(), player, resonance.color());
				return;
			}
		}
		discover(player, resonance);
		TwistMagic.ride(cast, resonance);
	}

	/** The first time {@code player} casts {@code resonance}: it goes into their Grimoire, and maybe the world hears of it. */
	static boolean discover(ServerPlayer player, Resonance resonance) {
		// Its mana condenses here; the moment itself is shown below, as the toast needs its name.
		if (!Grimoire.unlock(player, resonance.key(), false)) {
			return false;
		}
		MinecraftServer server = player.level().getServer();
		boolean first = ledger(server).record(resonance.id(), player);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 60, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(capital(resonance.name())).withColor(resonance.color())));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.wildercord.resonance").withColor(0xE8E0FF)));
		Fx.sound(player.level(), player.position(), SoundEvents.BOOK_PAGE_TURN, 0.7F, 0.9F);
		Fx.sound(player.level(), player.position(), dev.wildercord.content.WildercordSounds.DISCOVERY, 1.0F, 0.85F);
		ServerPlayNetworking.send(player, new Revealed(Revealed.RESONANCE, resonance.name(), resonance.color()));
		TwistVfx.revealed(player.level(), player, resonance.color());
		Component name = Component.literal(resonance.name()).withColor(resonance.color());
		if (first && Config.get().resonances().announce()) {
			server.getPlayerList().broadcastSystemMessage(
				Component.translatable("message.wildercord.resonance_found", player.getDisplayName(), name).withColor(0xC8B8F0), false);
		} else {
			player.sendSystemMessage(Component.translatable("message.wildercord.resonance_found_you", name).withColor(0xC8B8F0));
		}
		if (first) {
			refreshAll(server);
		} else {
			refresh(player);
		}
		for (FoundListener listener : LISTENERS) {
			try {
				listener.found(player, resonance, first);
			} catch (RuntimeException e) {
				Wildercord.LOGGER.error("A resonance listener failed", e);
			}
		}
		return true;
	}

	/**
	 * A Torn Page: the riddle of one of this world's resonances the player hasn't found or read, or empty if none are
	 * left. Its name and riddle go into their Grimoire.
	 */
	public static Optional<Resonance> hint(ServerPlayer player) {
		if (!enabled()) {
			return Optional.empty();
		}
		List<Resonance> left = new ArrayList<>();
		for (Resonance resonance : of(player.level().getServer())) {
			if (!Heart.discovered(player, resonance.key()) && !Heart.discovered(player, resonance.hintKey())) {
				left.add(resonance);
			}
		}
		if (left.isEmpty()) {
			return Optional.empty();
		}
		Resonance resonance = left.get(player.getRandom().nextInt(left.size()));
		Grimoire.unlock(player, resonance.hintKey());
		refresh(player);
		return Optional.of(resonance);
	}

	/** Whether this world has a resonance riddle {@code player} hasn't read or solved. */
	public static boolean hintsLeft(ServerPlayer player) {
		for (Resonance resonance : of(player.level().getServer())) {
			if (!Heart.discovered(player, resonance.key()) && !Heart.discovered(player, resonance.hintKey())) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ what each player may know

	/** Works out what {@code player} may know of the world's magic and sends it, if it changed. */
	public static void refresh(ServerPlayer player) {
		WildercordAttachments.WorldLore lore = WildercordAttachments.WorldLore.NONE;
		MinecraftServer server = player.level().getServer();
		if (enabled() && server != null) {
			Ledger ledger = ledger(server);
			List<String> grimoire = Heart.grimoire(player);
			Map<String, String> finders = ledger.finders();
			if (!Config.get().resonances().announce()) {
				// Nobody is told of others' finds: a player sees only who found first the ones they found too.
				finders.keySet().removeIf(id -> !grimoire.contains(Resonance.KEY_PREFIX + id));
			}
			lore = new WildercordAttachments.WorldLore(ResonanceLore.views(ledger.resonances, grimoire, finders),
				ResonanceLore.quirks(ledger.quirks, grimoire), ledger.resonances.size());
		}
		if (!lore.equals(player.getAttachedOrElse(WildercordAttachments.WORLD_LORE, WildercordAttachments.WorldLore.NONE))) {
			player.setAttached(WildercordAttachments.WORLD_LORE, lore);
		}
	}

	/** After {@code /wildercord reload}: the world's draw follows the settings, and every player's view is worked out again. */
	public static void reloaded(MinecraftServer server) {
		ledger(server);
		refreshAll(server);
	}

	static void refreshAll(MinecraftServer server) {
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			refresh(player);
		}
	}

	private static String capital(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	// ------------------------------------------------------------------ the toast

	/**
	 * Server to client: something of the world's magic was revealed to this player (a resonance found, a quirk met), for
	 * its toast. It carries only the name the player has just been shown.
	 */
	public record Revealed(String kind, String name, int color) implements CustomPacketPayload {
		public static final String RESONANCE = "resonance";
		public static final String QUIRK = "quirk";
		public static final Type<Revealed> TYPE = new Type<>(Wildercord.id("revealed"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Revealed> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Revealed::kind, ByteBufCodecs.STRING_UTF8, Revealed::name, ByteBufCodecs.INT, Revealed::color, Revealed::new);

		@Override
		public Type<Revealed> type() {
			return TYPE;
		}
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Revealed.TYPE, Revealed.CODEC);
		// Drawn as the server starts, so the first cast doesn't wait for it.
		ServerLifecycleEvents.SERVER_STARTED.register(WorldResonances::ledger);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			prepared = null;
			WorldQuirks.forget();
			TwistMagic.clear();
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> refresh(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> refresh(newPlayer));
	}
}
