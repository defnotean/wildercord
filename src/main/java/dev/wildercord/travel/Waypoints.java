package dev.wildercord.travel;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Waypoints: personal markers that never teleport anyone. {@code /waypoint add <name> [pos]} marks a
 * place (where you stand, by default) in the world you're in; {@code remove}, {@code list},
 * {@code track}/{@code untrack} and {@code share <name> <player>} (who gets a line in chat to click
 * and add it). The tracked waypoint is sent to its player ({@link Track}), whose HUD points the way
 * to it, and within {@link TravelRules#BEAM_RANGE} blocks a faint beam rises from it that only they
 * can see.
 */
public final class Waypoints {
	private Waypoints() {}

	/**
	 * Server to client: the waypoint {@code name} is tracked, at {@code x}, {@code y}, {@code z} in
	 * {@code dimension}; an empty name means none is.
	 */
	public record Track(String name, String dimension, double x, double y, double z) implements CustomPacketPayload {
		public static final Track NONE = new Track("", "", 0, 0, 0);
		public static final Type<Track> TYPE = new Type<>(Wildercord.id("track_waypoint"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Track> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(TravelRules.MAX_NAME), Track::name,
			ByteBufCodecs.stringUtf8(256), Track::dimension,
			ByteBufCodecs.DOUBLE, Track::x,
			ByteBufCodecs.DOUBLE, Track::y,
			ByteBufCodecs.DOUBLE, Track::z,
			Track::new).cast();

		public boolean none() {
			return name.isEmpty();
		}

		@Override
		public Type<Track> type() {
			return TYPE;
		}
	}

	static void init() {
		// The tracked waypoint comes back with you when you join.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (Travel.enabled() && !Travel.data(handler.player).tracked().isEmpty()) {
				sync(handler.player);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Waypoints::tick);
	}

	// ------------------------------------------------------------------ the commands

	/**
	 * {@code /waypoint add}: marks waypoint {@code raw} at {@code pos} in {@code level}, or where
	 * {@code player} stands when those are null. A name already used is moved.
	 */
	public static int add(ServerPlayer player, String raw, Vec3 pos, ServerLevel level) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		if (name == null) {
			Travel.fail(player, "bad_name", TravelRules.MAX_NAME);
			return 0;
		}
		TravelData data = Travel.data(player);
		boolean moved = data.waypoints().containsKey(name);
		if (!moved && data.waypoints().size() >= TravelRules.MAX_WAYPOINTS) {
			Travel.fail(player, "waypoints_full", TravelRules.MAX_WAYPOINTS);
			return 0;
		}
		ServerLevel in = level != null ? level : player.level();
		Spot spot = pos == null && level == null ? Spot.of(player) : Spot.at(in, pos != null ? pos : player.position());
		Travel.update(player, d -> d.withWaypoint(name, spot));
		MutableComponent track = Travel.button(Component.literal("/waypoint track " + name).withStyle(ChatFormatting.UNDERLINE),
			"/waypoint track " + name, Travel.text("waypoint_track_hover", Travel.name(name)));
		player.sendSystemMessage(Travel.text(moved ? "waypoint_moved" : "waypoint_added", Travel.name(name), spot.describe(), track).withColor(Travel.ARCANE));
		if (spot.in(player.level()) && spot.pos().distanceToSqr(player.position()) < 4) {
			TravelFx.mark(player.level(), player.position());
		}
		if (name.equals(data.tracked())) {
			sync(player);
		}
		return 1;
	}

	/** {@code /waypoint remove}. */
	public static int remove(ServerPlayer player, String raw) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		TravelData data = Travel.data(player);
		if (name == null || !data.waypoints().containsKey(name)) {
			Travel.fail(player, "waypoint_missing", Travel.name(raw));
			return 0;
		}
		Travel.update(player, d -> d.withoutWaypoint(name));
		Travel.good(player, "waypoint_removed", Travel.name(name));
		if (name.equals(data.tracked())) {
			sync(player);
		}
		return 1;
	}

	/** {@code /waypoint list}: every waypoint, each a button that tracks it. */
	public static int list(ServerPlayer player) {
		if (!Travel.available(player)) {
			return 0;
		}
		TravelData data = Travel.data(player);
		Map<String, Spot> waypoints = data.waypoints();
		if (waypoints.isEmpty()) {
			Travel.info(player, "waypoints_none");
			return 0;
		}
		List<MutableComponent> buttons = new ArrayList<>();
		waypoints.forEach((name, spot) -> {
			MutableComponent label = Travel.name(name).withStyle(ChatFormatting.UNDERLINE);
			if (name.equals(data.tracked())) {
				label = Component.literal("✦ ").withColor(Travel.ARCANE).append(label);
			}
			buttons.add(Travel.button(label, "/waypoint track " + name, Travel.text("waypoint_hover", spot.describe())));
		});
		player.sendSystemMessage(Travel.text("waypoints_list", waypoints.size()).withStyle(ChatFormatting.GRAY).append(Travel.row(buttons)));
		return waypoints.size();
	}

	/** {@code /waypoint track}: points the HUD at waypoint {@code raw}. */
	public static int track(ServerPlayer player, String raw) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		if (name == null || !Travel.data(player).waypoints().containsKey(name)) {
			Travel.fail(player, "waypoint_missing", Travel.name(raw));
			return 0;
		}
		Travel.update(player, d -> d.withTracked(name));
		sync(player);
		Travel.good(player, "waypoint_tracking", Travel.name(name));
		return 1;
	}

	/** {@code /waypoint untrack}. */
	public static int untrack(ServerPlayer player) {
		if (!Travel.available(player)) {
			return 0;
		}
		String tracked = Travel.data(player).tracked();
		if (tracked.isEmpty()) {
			Travel.fail(player, "waypoint_not_tracking");
			return 0;
		}
		Travel.update(player, d -> d.withTracked(""));
		sync(player);
		Travel.good(player, "waypoint_untracked", Travel.name(tracked));
		return 1;
	}

	/** {@code /waypoint share}: sends {@code to} a line in chat that adds waypoint {@code raw} to theirs when clicked. */
	public static int share(ServerPlayer player, String raw, ServerPlayer to) {
		if (!Travel.available(player)) {
			return 0;
		}
		String name = TravelRules.name(raw);
		Spot spot = name == null ? null : Travel.data(player).waypoints().get(name);
		if (spot == null) {
			Travel.fail(player, "waypoint_missing", Travel.name(raw));
			return 0;
		}
		if (to.getUUID().equals(player.getUUID())) {
			Travel.fail(player, "waypoint_share_self");
			return 0;
		}
		String command = String.format(Locale.ROOT, "/waypoint add %s %.2f %.2f %.2f %s", name, spot.x(), spot.y(), spot.z(), spot.dimension());
		MutableComponent add = Travel.button(Travel.text("waypoint_offer_add").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), command,
			Travel.text("waypoint_offer_hover", Travel.name(name)));
		to.sendSystemMessage(Travel.text("waypoint_offer", player.getDisplayName(), Travel.name(name), spot.describe()).withColor(Travel.ARCANE)
			.append(Component.literal("  ")).append(add));
		Travel.good(player, "waypoint_shared", Travel.name(name), to.getDisplayName());
		return 1;
	}

	// ------------------------------------------------------------------ the HUD and the beam

	/** Tells {@code player}'s client which waypoint they track (or that none is). */
	public static void sync(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, Track.TYPE)) {
			return;
		}
		TravelData data = Travel.data(player);
		Track track = data.trackedSpot()
			.map(spot -> new Track(data.tracked(), spot.dimension(), spot.x(), spot.y(), spot.z()))
			.orElse(Track.NONE);
		ServerPlayNetworking.send(player, track);
	}

	/** Every half second, a beam for each tracked waypoint whose player is near it: the shaft once a second, motes every time. */
	private static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		if (tick % 10 != 0 || !Travel.enabled()) {
			return;
		}
		boolean shaft = tick % 20 == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			TravelData data = Travel.data(player);
			if (data.tracked().isEmpty()) {
				continue;
			}
			Spot spot = data.trackedSpot().orElse(null);
			if (spot == null || !spot.in(player.level())) {
				continue;
			}
			double dx = spot.x() - player.getX();
			double dz = spot.z() - player.getZ();
			double level = dx * dx + dz * dz;
			// Close enough to see, but not standing right in it.
			if (level <= TravelRules.BEAM_RANGE * TravelRules.BEAM_RANGE && level > 2.25) {
				TravelFx.beam(player, spot.pos(), shaft);
			}
		}
	}
}
