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
 */
public final class Climate {
	private Climate() {}

	/** Server to client: the ids of the conditions holding where the player stands (empty: none, or climate is off). */
	public record Sync(List<String> conditions) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(Wildercord.id("climate"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(32).apply(ByteBufCodecs.list(16)), Sync::conditions, Sync::new).cast();

		@Override
		public Type<Sync> type() {
			return TYPE;
		}
	}

	/** One player's conditions, and the second and dimension they were worked out for. */
	private record Cached(long second, ServerLevel level, Set<Condition> conditions) {}

	private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
	/** What each player was last sent, so a packet goes only when it changes. */
	private static final Map<UUID, List<String>> SENT = new ConcurrentHashMap<>();
	/** Client side: what the server last said holds here. */
	private static volatile Set<Condition> shown = Set.of();

	/** How hard {@code element} hits for this caster where they stand (1 for monsters, and with climate switched off). */
	public static double factor(LivingEntity caster, String element) {
		if (element.isEmpty() || !(caster instanceof ServerPlayer player) || !Config.get().elementalClimate()) {
			return 1.0;
		}
		return ClimateRules.factor(conditions(player), element);
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
		ServerLevel level = player.level();
		BlockPos feet = player.blockPosition();
		BlockPos head = BlockPos.containing(player.getEyePosition());
		Biome biome = level.getBiome(feet).value();
		boolean fixedTime = level.dimensionType().hasFixedTime();
		long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
		boolean ley = LeyWalker.onLine(player) || ManaStorm.inside(player);
		return new ClimateRules.Surroundings(level.dimension().identifier().toString(), !fixedTime, time, level.canSeeSky(head),
			level.isRaining(), level.isThundering(), level.isRainingAt(head), biome.coldEnoughToSnow(feet, level.getSeaLevel()),
			biome.getBaseTemperature(), !biome.hasPrecipitation(), feet.getY(), ley);
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
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!ServerPlayNetworking.canSend(player, Sync.TYPE)) {
				continue;
			}
			List<String> now = on && !player.isSpectator() ? ClimateRules.ids(conditions(player)) : List.of();
			if (!now.equals(SENT.getOrDefault(player.getUUID(), List.of()))) {
				SENT.put(player.getUUID(), now);
				ServerPlayNetworking.send(player, new Sync(now));
			}
		}
	}

	// ------------------------------------------------------------------ client side

	/** Client side: the server's word on where this player stands (or, with {@code null}, the connection closed). */
	public static void receive(Sync sync) {
		shown = sync == null ? Set.of() : Set.copyOf(ClimateRules.fromIds(sync.conditions()));
	}

	/** Client side: the conditions holding where this player stands, for the HUD and the Grimoire page. */
	public static Set<Condition> shown() {
		return shown;
	}
}
