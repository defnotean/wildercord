package dev.wildercord.travel;

import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every travel command's teleport goes through here, under the same rules:
 * <ul>
 *   <li>not during a duel;</li>
 *   <li>a cooldown per command (operators skip it);</li>
 *   <li>a warmup, standing still in a forming circle with a countdown above the hotbar (operators and
 *       creative players skip it): moving more than half a block, being hurt, dying, changing world
 *       or a duel starting cancels it;</li>
 *   <li>a safe landing: the destination is worked out again when the warmup ends, and a spot that's
 *       become unsafe is swapped for the nearest safe one, or refused;</li>
 *   <li>where the traveller left from becomes where {@code /back} goes;</li>
 *   <li>light and sound at both ends. Works across worlds.</li>
 * </ul>
 * Warmups and cooldowns aren't saved: a restart forgets them.
 */
public final class Teleports {
	private Teleports() {}

	/** Which command a teleport came from: each has its own cooldown. */
	public enum Kind {
		HOME("/home"), WARP("/warp"), SPAWN("/spawn"), BACK("/back"), TPA("/tpa"), RTP("/rtp");

		final String command;

		Kind(String command) {
			this.command = command;
		}
	}

	/** Where a teleport lands. */
	public record Arrival(ServerLevel level, Vec3 pos, float yaw, float pitch) {}

	/**
	 * Where a teleport goes, worked out as it happens (a player being visited may have moved, a home
	 * may have been built over). Returns null when it can't go, having told the traveller why.
	 */
	@FunctionalInterface
	public interface Destination {
		Arrival resolve(ServerPlayer traveller);
	}

	/** A teleport waiting out its warmup. */
	private record Pending(Kind kind, Component where, Destination destination, Vec3 start, ResourceKey<Level> world, long started, int warmup) {}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	/** Server tick at which each player may use each command again. */
	private static final Map<UUID, EnumMap<Kind, Long>> READY_AT = new HashMap<>();

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(Teleports::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage > 0 && entity instanceof ServerPlayer player && PENDING.get(player.getUUID()) instanceof Pending pending) {
				cancel(player, pending, "hurt");
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PENDING.clear();
			READY_AT.clear();
		});
	}

	// ------------------------------------------------------------------ starting a teleport

	/**
	 * Starts a teleport of {@code player} to {@code destination}, called {@code where} in messages
	 * ("your home base", "spawn"). The destination is tried at once, so one that can't be reached says
	 * so before the wait rather than after it.
	 *
	 * @param operator whether to treat them as an operator (no warmup, no cooldown); the commands pass
	 *                 {@link Travel#operator}. Creative and spectator players skip the warmup either way.
	 * @return 1 if the teleport happened or its warmup began, 0 if not (the player has been told why)
	 */
	public static int begin(ServerPlayer player, Kind kind, Component where, Destination destination, boolean operator) {
		if (!Travel.available(player)) {
			return 0;
		}
		if (Duels.inDuel(player)) {
			Travel.fail(player, "duel");
			return 0;
		}
		if (!operator) {
			long left = cooldown(player, kind);
			if (left > 0) {
				Travel.fail(player, "cooldown", kind.command, TravelRules.seconds(left));
				return 0;
			}
		}
		Arrival first = destination.resolve(player);
		if (first == null) {
			return 0;
		}
		// A new teleport replaces one still warming up.
		PENDING.remove(player.getUUID());
		boolean instant = operator || player.isCreative() || player.isSpectator();
		int warmup = instant ? 0 : Travel.config().warmupSeconds() * TravelRules.TICKS_PER_SECOND;
		if (warmup <= 0) {
			finish(player, kind, where, first);
			return 1;
		}
		ServerLevel level = player.level();
		Pending pending = new Pending(kind, where, destination, player.position(), level.dimension(), level.getServer().getTickCount(), warmup);
		PENDING.put(player.getUUID(), pending);
		countdown(player, pending, TravelRules.seconds(warmup));
		return 1;
	}

	/** Whether {@code player} has a teleport warming up. */
	public static boolean pending(ServerPlayer player) {
		return PENDING.containsKey(player.getUUID());
	}

	/** Ticks before {@code player} may use {@code kind}'s command again (0 if they may now). */
	public static long cooldown(ServerPlayer player, Kind kind) {
		EnumMap<Kind, Long> ready = READY_AT.get(player.getUUID());
		Long at = ready == null ? null : ready.get(kind);
		return at == null ? 0 : TravelRules.remaining(player.level().getServer().getTickCount(), at);
	}

	/** Forgets {@code player}'s cooldowns (for tests, or an operator's fixing up). */
	public static void resetCooldowns(ServerPlayer player) {
		READY_AT.remove(player.getUUID());
	}

	private static long cooldownTicks(Kind kind) {
		int seconds = kind == Kind.RTP ? Travel.config().rtpCooldownSeconds() : Travel.config().cooldownSeconds();
		return (long) seconds * TravelRules.TICKS_PER_SECOND;
	}

	// ------------------------------------------------------------------ the warmup

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		long now = server.getTickCount();
		for (Map.Entry<UUID, Pending> entry : List.copyOf(PENDING.entrySet())) {
			Pending pending = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive() || player.isRemoved() || !player.level().dimension().equals(pending.world())) {
				PENDING.remove(entry.getKey(), pending);
				continue;
			}
			Vec3 d = player.position().subtract(pending.start());
			if (TravelRules.moved(d.x, d.y, d.z)) {
				cancel(player, pending, "moved");
				continue;
			}
			if (Duels.inDuel(player)) {
				cancel(player, pending, "duel");
				continue;
			}
			long elapsed = now - pending.started();
			if (elapsed >= pending.warmup()) {
				PENDING.remove(entry.getKey(), pending);
				// Worked out again now: the world (or the player being visited) may have changed.
				Arrival arrival = pending.destination().resolve(player);
				if (arrival != null) {
					finish(player, pending.kind(), pending.where(), arrival);
				}
				continue;
			}
			if (elapsed > 0 && elapsed % TravelRules.TICKS_PER_SECOND == 0) {
				countdown(player, pending, TravelRules.seconds(pending.warmup() - elapsed));
			}
			if (elapsed % 4 == 2) {
				TravelFx.mote(player.level(), pending.start(), player.getRandom());
			}
		}
	}

	/** Shows the seconds left above the hotbar, and renews the circle. */
	private static void countdown(ServerPlayer player, Pending pending, long seconds) {
		player.sendOverlayMessage(Travel.text("countdown", pending.where(), seconds).withColor(Travel.ARCANE));
		TravelFx.circle(player.level(), pending.start(), seconds);
	}

	private static void cancel(ServerPlayer player, Pending pending, String why) {
		if (PENDING.remove(player.getUUID(), pending)) {
			player.sendOverlayMessage(Travel.text("cancel." + why).withStyle(ChatFormatting.RED));
			Travel.fail(player, "cancel." + why);
			TravelFx.fizzle(player.level(), pending.start());
		}
	}

	// ------------------------------------------------------------------ the teleport itself

	private static void finish(ServerPlayer player, Kind kind, Component where, Arrival arrival) {
		ServerLevel from = player.level();
		Vec3 left = player.position();
		Travel.update(player, data -> data.withBack(Spot.of(player)));
		TravelFx.depart(from, left);
		player.stopRiding();
		player.teleportTo(arrival.level(), arrival.pos().x, arrival.pos().y, arrival.pos().z, Set.<Relative>of(), arrival.yaw(), arrival.pitch(), true);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
		TravelFx.arrive(arrival.level(), arrival.pos());
		READY_AT.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(Kind.class))
			.put(kind, arrival.level().getServer().getTickCount() + cooldownTicks(kind));
		player.sendOverlayMessage(Travel.text("arrived", where).withColor(Travel.ARCANE));
		if (kind == Kind.RTP) {
			BlockPos at = BlockPos.containing(arrival.pos());
			Travel.good(player, "rtp_landed", at.getX(), at.getY(), at.getZ());
		}
	}

	// ------------------------------------------------------------------ destinations

	/**
	 * A saved spot as a destination: its world, and the spot itself or the nearest safe one near it.
	 * {@code unsafe} is the message when there's none (given {@code where}).
	 */
	static Destination to(Spot spot, String unsafe, Component where) {
		return traveller -> {
			ServerLevel level = spot.level(traveller.level().getServer());
			if (level == null) {
				Travel.fail(traveller, "no_world");
				return null;
			}
			Vec3 pos = Landing.near(level, spot.pos(), traveller, false);
			if (pos == null) {
				Travel.fail(traveller, unsafe, where);
				return null;
			}
			return new Arrival(level, pos, spot.yaw(), spot.pitch());
		};
	}

	/** {@code /back}: to where the last teleport left from, or where they died. */
	public static int back(ServerPlayer player, boolean operator) {
		if (!Travel.available(player)) {
			return 0;
		}
		Spot spot = Travel.data(player).back().orElse(null);
		if (spot == null) {
			Travel.fail(player, "back_none");
			return 0;
		}
		Component where = Travel.text("where.back");
		return begin(player, Kind.BACK, where, to(spot, "unsafe", where), operator);
	}

	/** {@code /spawn}: the world spawn, on safe ground. */
	public static int spawn(ServerPlayer player, boolean operator) {
		if (!Travel.available(player)) {
			return 0;
		}
		Component where = Travel.text("where.spawn");
		return begin(player, Kind.SPAWN, where, traveller -> {
			MinecraftServer server = traveller.level().getServer();
			ServerLevel level = server.findRespawnDimension();
			LevelData.RespawnData data = server.getRespawnData();
			BlockPos pos = data.pos();
			Vec3 spot = Landing.near(level, Vec3.atBottomCenterOf(pos), traveller, false);
			if (spot == null) {
				// Built over, or never on the ground: the top of the world there.
				spot = Landing.surface(level, pos.getX(), pos.getZ(), traveller, false);
			}
			if (spot == null) {
				Travel.fail(traveller, "unsafe", where);
				return null;
			}
			return new Arrival(level, spot, data.yaw(), 0F);
		}, operator);
	}

	/**
	 * {@code /rtp}: somewhere random in the overworld within the server's radius of world spawn, on
	 * safe ground. The spot is found once, before the warmup, and only checked again at the end.
	 */
	public static int randomly(ServerPlayer player, boolean operator) {
		if (!Travel.available(player)) {
			return 0;
		}
		Component where = Travel.text("where.random");
		return begin(player, Kind.RTP, where, new Destination() {
			private Vec3 found;

			@Override
			public Arrival resolve(ServerPlayer traveller) {
				MinecraftServer server = traveller.level().getServer();
				ServerLevel overworld = server.overworld();
				if (found == null) {
					LevelData.RespawnData data = server.getRespawnData();
					BlockPos centre = Level.OVERWORLD.equals(data.dimension()) ? data.pos() : BlockPos.ZERO;
					found = Landing.random(overworld, centre, Travel.config().rtpRadius(), traveller.getRandom(), traveller);
				} else {
					found = Landing.near(overworld, found, traveller, true);
				}
				if (found == null) {
					Travel.fail(traveller, "rtp_failed");
					return null;
				}
				return new Arrival(overworld, found, traveller.getYRot(), 0F);
			}
		}, operator);
	}
}
