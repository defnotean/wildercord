package dev.wildercord.travel;

import dev.wildercord.duel.Duels;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Teleport requests between players: {@code /tpa <player>} asks to go to them, {@code /tpahere
 * <player>} asks them to come to you. The one asked gets clickable [Accept] and [Deny] buttons and
 * has the server's {@code travel.tpa_timeout_seconds} to answer; {@code /tpcancel} takes requests back
 * and {@code /tptoggle} turns them away. A new request to the same person replaces the old one, and
 * both sides hear how each ends. Accepting starts the traveller's teleport, with its warmup, to
 * wherever the other player is when it ends. The bookkeeping is {@link TravelRules.Requests}.
 */
public final class TeleportRequests {
	private TeleportRequests() {}

	private static final TravelRules.Requests BOOK = new TravelRules.Requests();

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!BOOK.isEmpty() && server.getTickCount() % TravelRules.TICKS_PER_SECOND == 0) {
				expire(server, server.getTickCount());
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer leaving = handler.player;
			for (TravelRules.Request r : BOOK.forget(leaving.getUUID())) {
				ServerPlayer other = online(server, r.from().equals(leaving.getUUID()) ? r.to() : r.from());
				if (other != null) {
					Travel.info(other, "tpa_left", leaving.getDisplayName());
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BOOK.clear());
	}

	private static long timeout() {
		return (long) Travel.config().tpaTimeoutSeconds() * TravelRules.TICKS_PER_SECOND;
	}

	private static ServerPlayer online(MinecraftServer server, UUID id) {
		return server.getPlayerList().getPlayer(id);
	}

	// ------------------------------------------------------------------ asking

	/**
	 * {@code /tpa} ({@code here} false) or {@code /tpahere} ({@code here} true): asks {@code to} to let
	 * {@code from} come to them, or to come to {@code from}.
	 */
	public static int ask(ServerPlayer from, ServerPlayer to, boolean here) {
		if (!Travel.available(from)) {
			return 0;
		}
		if (from == to || from.getUUID().equals(to.getUUID())) {
			Travel.fail(from, "tpa_self");
			return 0;
		}
		if (Duels.inDuel(from)) {
			Travel.fail(from, "duel");
			return 0;
		}
		if (Duels.inDuel(to)) {
			Travel.fail(from, "tpa_busy", to.getDisplayName());
			return 0;
		}
		if (Travel.data(to).requestsOff()) {
			Travel.fail(from, "tpa_closed", to.getDisplayName());
			return 0;
		}
		// Asking to go somewhere you can't yet go wastes their time: say so now.
		if (!here && !Travel.operator(from)) {
			long left = Teleports.cooldown(from, Teleports.Kind.TPA);
			if (left > 0) {
				Travel.fail(from, "cooldown", Teleports.Kind.TPA.command, TravelRules.seconds(left));
				return 0;
			}
		}
		long now = from.level().getServer().getTickCount();
		TravelRules.Request replaced = BOOK.add(new TravelRules.Request(from.getUUID(), to.getUUID(), here, now));
		String name = from.getGameProfile().name();
		MutableComponent accept = Travel.button(Travel.text("tpa_accept").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), "/tpaccept " + name,
			Travel.text("tpa_accept_hover", from.getDisplayName()));
		MutableComponent deny = Travel.button(Travel.text("tpa_deny").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), "/tpdeny " + name,
			Travel.text("tpa_deny_hover"));
		to.sendSystemMessage(Travel.text(here ? "tpa_asked_here" : "tpa_asked_to", from.getDisplayName()).withColor(Travel.ARCANE)
			.append(Component.literal("  ")).append(accept).append(Component.literal(" ")).append(deny));
		if (replaced == null) {
			// A soft chime for the one asked, the first time only.
			to.level().playSound(null, to.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.6F);
		}
		Travel.info(from, "tpa_sent", to.getDisplayName(), Travel.config().tpaTimeoutSeconds());
		return 1;
	}

	// ------------------------------------------------------------------ answering

	/** {@code /tpaccept}: accepts the request from {@code from}, or with {@code from} null the newest. */
	public static int accept(ServerPlayer to, ServerPlayer from) {
		TravelRules.Request request = take(to, from);
		if (request == null) {
			return 0;
		}
		MinecraftServer server = to.level().getServer();
		ServerPlayer asker = online(server, request.from());
		if (asker == null) {
			Travel.fail(to, "tpa_gone");
			return 0;
		}
		// Nobody comes or goes from a duel, even one that began after the request.
		if (Duels.inDuel(to)) {
			Travel.fail(to, "duel");
			return 0;
		}
		if (Duels.inDuel(asker)) {
			Travel.fail(to, "tpa_busy", asker.getDisplayName());
			return 0;
		}
		Travel.good(asker, "tpa_accepted", to.getDisplayName());
		Travel.good(to, "tpa_you_accepted", asker.getDisplayName());
		ServerPlayer traveller = request.here() ? to : asker;
		ServerPlayer host = request.here() ? asker : to;
		UUID hostId = host.getUUID();
		Component where = host.getDisplayName();
		return Teleports.begin(traveller, Teleports.Kind.TPA, where, t -> {
			// Wherever the other player is when the warmup ends.
			ServerPlayer there = online(t.level().getServer(), hostId);
			if (there == null || !there.isAlive()) {
				Travel.fail(t, "tpa_gone");
				return null;
			}
			// Nobody arrives in a duel, even one the other player started during the warmup.
			if (Duels.inDuel(there)) {
				Travel.fail(t, "tpa_busy", there.getDisplayName());
				return null;
			}
			ServerLevel level = there.level();
			Vec3 pos = Landing.near(level, there.position(), t, false);
			if (pos == null) {
				Travel.fail(t, "tpa_unsafe", there.getDisplayName());
				return null;
			}
			return new Teleports.Arrival(level, pos, there.getYRot(), there.getXRot());
		}, Travel.operator(traveller));
	}

	/** {@code /tpdeny}: turns down the request from {@code from}, or with {@code from} null the newest. */
	public static int deny(ServerPlayer to, ServerPlayer from) {
		TravelRules.Request request = take(to, from);
		if (request == null) {
			return 0;
		}
		ServerPlayer asker = online(to.level().getServer(), request.from());
		if (asker != null) {
			Travel.info(asker, "tpa_denied", to.getDisplayName());
		}
		Travel.info(to, "tpa_you_denied", asker != null ? asker.getDisplayName() : Component.literal("?"));
		return 1;
	}

	/** The request {@code to} is answering, taken out of the book, or null (and they're told why). */
	private static TravelRules.Request take(ServerPlayer to, ServerPlayer from) {
		if (!Travel.available(to)) {
			return null;
		}
		MinecraftServer server = to.level().getServer();
		expire(server, server.getTickCount());
		TravelRules.Request request = BOOK.take(to.getUUID(), from == null ? null : from.getUUID());
		if (request == null) {
			if (from == null) {
				Travel.fail(to, "tpa_none");
			} else {
				Travel.fail(to, "tpa_none_from", from.getDisplayName());
			}
		}
		return request;
	}

	// ------------------------------------------------------------------ taking back and turning away

	/** {@code /tpcancel}: takes back every request {@code from} has out. */
	public static int cancel(ServerPlayer from) {
		if (!Travel.available(from)) {
			return 0;
		}
		List<TravelRules.Request> cancelled = BOOK.cancel(from.getUUID());
		if (cancelled.isEmpty()) {
			Travel.fail(from, "tpa_nothing_out");
			return 0;
		}
		MinecraftServer server = from.level().getServer();
		for (TravelRules.Request r : cancelled) {
			ServerPlayer to = online(server, r.to());
			if (to != null) {
				Travel.info(to, "tpa_withdrawn", from.getDisplayName());
				Travel.info(from, "tpa_cancelled", to.getDisplayName());
			}
		}
		return cancelled.size();
	}

	/** {@code /tptoggle}: turns requests away (dropping any waiting), or lets them in again. */
	public static int toggle(ServerPlayer player) {
		if (!Travel.available(player)) {
			return 0;
		}
		boolean off = !Travel.data(player).requestsOff();
		Travel.update(player, d -> d.withRequestsOff(off));
		if (off) {
			for (TravelRules.Request r : BOOK.refuseAll(player.getUUID())) {
				ServerPlayer asker = online(player.level().getServer(), r.from());
				if (asker != null) {
					Travel.info(asker, "tpa_closed", player.getDisplayName());
				}
			}
		}
		Travel.good(player, off ? "tpa_off" : "tpa_on");
		return 1;
	}

	// ------------------------------------------------------------------ for the rest of the mod

	/** The requests waiting for {@code player} to answer, newest first. */
	public static List<TravelRules.Request> waitingFor(ServerPlayer player) {
		return BOOK.waitingFor(player.getUUID());
	}

	/** The requests {@code player} has out. */
	public static List<TravelRules.Request> sentBy(ServerPlayer player) {
		return BOOK.sentBy(player.getUUID());
	}

	/** Lets every request that has waited long enough by {@code now} (server ticks) run out, telling both sides; returns them. */
	public static List<TravelRules.Request> expire(MinecraftServer server, long now) {
		List<TravelRules.Request> expired = BOOK.expire(now, timeout());
		for (TravelRules.Request r : expired) {
			ServerPlayer from = online(server, r.from());
			ServerPlayer to = online(server, r.to());
			if (from != null && to != null) {
				Travel.info(from, "tpa_expired_from", to.getDisplayName());
				Travel.info(to, "tpa_expired_to", from.getDisplayName());
			}
		}
		return expired;
	}
}
