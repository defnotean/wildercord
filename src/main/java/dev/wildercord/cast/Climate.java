package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.events.ManaStorm;
import dev.wildercord.config.Config;
import dev.wildercord.spell.ClimateRules;
import dev.wildercord.spell.ClimateRules.Condition;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elemental climate at runtime: which of {@link ClimateRules}' conditions hold where a player stands
 * (the Nether, a thunderstorm overhead, snow, the deep...), worked out at most once a second per
 * player, since every spell hit asks. Only players' spells feel it: it's something to learn and use,
 * and the HUD shows it, so each player is sent their conditions whenever they change.
 *
 * <p>Places and times of power are part of it: a ley crossing underfoot, the moon and the hour overhead
 * (the server's {@code places_of_power} section switches and scales them; see {@link #tuning()}). The
 * server sends its tuning with the conditions, so the HUD's lines and a spell's price read the same
 * numbers the server uses.</p>
 */
public final class Climate {
	private Climate() {}

	/**
	 * Server to client: the ids of the conditions holding where the player stands (empty: none, or climate is off),
	 * and the server's tuning of places and times of power (a ley crossing's bonus, and the celestial scale).
	 */
	public record Sync(List<String> conditions, float crossing, float celestial) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(Wildercord.id("climate"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(24)), Sync::conditions, ByteBufCodecs.FLOAT, Sync::crossing,
				ByteBufCodecs.FLOAT, Sync::celestial, Sync::new).cast();

		@Override
		public Type<Sync> type() {
			return TYPE;
		}
	}

	/** One player's conditions, and the second and dimension they were worked out for. */
	private record Cached(long second, ServerLevel level, Set<Condition> conditions) {}

	private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
	/** What each player was last sent, so a packet goes only when it changes. */
	private static final Map<UUID, Sync> SENT = new ConcurrentHashMap<>();
	/** Client side: what the server last said holds here. */
	private static volatile Set<Condition> shown = Set.of();
	/** Client side: the server's tuning of places and times of power. */
	private static volatile ClimateRules.Tuning shownTuning = ClimateRules.Tuning.DEFAULT;

	/** How hard {@code element} hits for this caster where they stand (1 for monsters, and with climate switched off). */
	public static double factor(LivingEntity caster, String element) {
		if (element.isEmpty() || !(caster instanceof ServerPlayer player) || !Config.get().elementalClimate()) {
			return 1.0;
		}
		return ClimateRules.factor(conditions(player), element, tuning());
	}

	/** The server's tuning of places and times of power: a ley crossing's bonus and the celestial scale (0 for a switched-off one). */
	public static ClimateRules.Tuning tuning() {
		var power = Config.get().power();
		if (!Config.get().elementalClimate()) {
			return ClimateRules.Tuning.NONE;
		}
		return new ClimateRules.Tuning(power.leyCrossings() ? power.crossingBonus() : 0, power.celestial() ? power.celestialMultiplier() : 0);
	}

	/**
	 * What a spell costs this player where they stand, as a factor on its price: cheaper on a ley crossing. Both sides
	 * ask (the HUD and the Cord screen show the price), the client from what the server last sent it.
	 */
	public static double costFactor(net.minecraft.world.entity.player.Player player) {
		if (player.level().isClientSide()) {
			return shown.contains(Condition.LEY_CROSSING) ? shownTuning.crossingCost() : 1.0;
		}
		if (!(player instanceof ServerPlayer server) || !Config.get().elementalClimate() || !Config.get().power().leyCrossings()) {
			return 1.0;
		}
		return conditions(server).contains(Condition.LEY_CROSSING) ? tuning().crossingCost() : 1.0;
	}

	/** The conditions where this player stands, worked out at most once a second (and again at once in a new dimension). */
	public static Set<Condition> conditions(ServerPlayer player) {
		ServerLevel level = player.level();
		long second = level.getGameTime() / 20;
		Cached cached = CACHE.get(player.getUUID());
		if (cached != null && cached.second() == second && cached.level() == level) {
			return cached.conditions();
		}
		Set<Condition> now = Set.copyOf(ClimateRules.conditions(surroundings(player)));
		CACHE.put(player.getUUID(), new Cached(second, level, now));
		return now;
	}

	/** What the world says about where the player stands, for {@link ClimateRules#conditions}. */
	static ClimateRules.Surroundings surroundings(ServerPlayer player) {
		var power = Config.get().power();
		boolean ley = LeyWalker.onLine(player) || ManaStorm.inside(player);
		boolean crossing = power.leyCrossings() && LeyWalker.atCrossing(player);
		return surroundings(player.level(), player.blockPosition(), BlockPos.containing(player.getEyePosition()), ley, crossing);
	}

	/** What the world says about a spot (feet and head height), with whether a ley line and a ley crossing run there. */
	static ClimateRules.Surroundings surroundings(ServerLevel level, BlockPos feet, BlockPos head, boolean ley, boolean crossing) {
		Biome biome = level.getBiome(feet).value();
		boolean fixedTime = level.dimensionType().hasFixedTime();
		long clock = level.getOverworldClockTime();
		long time = Math.floorMod(clock, 24000L);
		int moon = fixedTime ? -1 : ClimateRules.moonPhase(clock);
		return new ClimateRules.Surroundings(level.dimension().identifier().toString(), !fixedTime, time, level.canSeeSky(head),
			level.isRaining(), level.isThundering(), level.isRainingAt(head), biome.coldEnoughToSnow(feet, level.getSeaLevel()),
			biome.getBaseTemperature(), !biome.hasPrecipitation(), feet.getY(), ley, crossing, moon, Config.get().power().celestial());
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Sync.TYPE, Sync.CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Climate::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			CACHE.remove(handler.player.getUUID());
			SENT.remove(handler.player.getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			CACHE.clear();
			SENT.clear();
		});
	}

	/** Once a second: tell each player what holds where they stand, if that changed. */
	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		boolean on = Config.get().elementalClimate();
		ClimateRules.Tuning tuning = tuning();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!ServerPlayNetworking.canSend(player, Sync.TYPE)) {
				continue;
			}
			List<String> now = on && !player.isSpectator() ? ClimateRules.ids(conditions(player)) : List.of();
			Sync sync = new Sync(now, (float) tuning.crossing(), (float) tuning.celestial());
			Sync before = SENT.get(player.getUUID());
			// Always once on joining, so the client knows the server's tuning (a ley crossing's shimmer, a spell's price) from the start.
			if (before == null || !sync.equals(before)) {
				SENT.put(player.getUUID(), sync);
				ServerPlayNetworking.send(player, sync);
			}
		}
	}

	// ------------------------------------------------------------------ client side

	/** Client side: the server's word on where this player stands (or, with {@code null}, the connection closed). */
	public static void receive(Sync sync) {
		Set<Condition> before = shown;
		shown = sync == null ? Set.of() : Set.copyOf(ClimateRules.fromIds(sync.conditions()));
		shownTuning = sync == null ? ClimateRules.Tuning.DEFAULT : new ClimateRules.Tuning(sync.crossing(), sync.celestial());
		// What just came into force, for the HUD's lines saying why (the conditions that weren't there before).
		java.util.EnumSet<Condition> arrived = java.util.EnumSet.noneOf(Condition.class);
		arrived.addAll(shown);
		arrived.removeAll(before);
		if (!arrived.isEmpty()) {
			changedAt = System.currentTimeMillis();
		}
	}

	/** Client side: when (wall-clock millis) a new condition last came into force where this player stands. */
	private static volatile long changedAt = Long.MIN_VALUE;

	/** Client side: when a new condition last came into force (the HUD shows why for a few seconds after). */
	public static long changedAt() {
		return changedAt;
	}

	/** Client side: the server's tuning of places and times of power. */
	public static ClimateRules.Tuning shownTuning() {
		return shownTuning;
	}

	/** Client side: the conditions holding where this player stands, for the HUD and the Grimoire page. */
	public static Set<Condition> shown() {
		return shown;
	}
}
