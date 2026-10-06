package dev.wildercord.gametest.stonehinge;

import dev.wildercord.Wildercord;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Bounded read-only packet observation. Observer faults fail the fixture later, never skip a native operation. */
public final class StoneHingeOwnerProbe {
	private StoneHingeOwnerProbe() {}
	private static volatile Trace active;
	private static boolean registered;
	private static final ThreadLocal<PositionSend> MOVEMENT = new ThreadLocal<>();
	public interface ConnectionState { boolean stoneHinge$awaitingTeleport(); }

	public record Body(Vec3 position, Vec3 motion, double fall, boolean grounded, boolean neutral, float health, int entity, long tick) {
		public static Body of(Player player) {
			boolean neutral = player instanceof ServerPlayer server ? server.getLastClientInput().equals(Input.EMPTY)
				: player instanceof LocalPlayer client && client.input.keyPresses.equals(Input.EMPTY);
			return new Body(player.position(), player.getDeltaMovement(), player.fallDistance, player.onGround(), neutral,
				player.getHealth(), player.getId(), player.level().getGameTime());
		}
	}
	public record Event(int index, String kind, String data, Body before, Body after) {}
	private record PositionKey(String type, double x, double y, double z, boolean rotation, float yaw, float pitch, boolean ground, boolean collision) {
		static PositionKey of(ServerboundMovePlayerPacket packet) {
			return new PositionKey(packet.getClass().getSimpleName(), packet.getX(Double.NaN), packet.getY(Double.NaN), packet.getZ(Double.NaN),
				packet.hasRotation(), packet.getYRot(Float.NaN), packet.getXRot(Float.NaN), packet.isOnGround(), packet.horizontalCollision());
		}
	}
	private static final class MotionSend {
		final int ordinal;
		final Vec3 raw, wire;
		final boolean expected, manual;
		volatile boolean completed;
		MotionSend(int ordinal, ClientboundSetEntityMotionPacket packet, boolean expected, boolean manual) {
			this.ordinal = ordinal; this.raw = packet.movement(); this.expected = expected; this.manual = manual;
			var buffer = Unpooled.buffer();
			try { ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(buffer, packet); wire = ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(buffer).movement(); }
			finally { buffer.release(); }
		}
		boolean matches(Vec3 vector) { return raw.equals(vector) || wire.equals(vector); }
	}
	private record MotionReceipt(MotionSend sent, Event processed, Vec3 payload) {}
	private static final class PositionSend {
		final int ordinal;
		final PositionKey key;
		final MotionReceipt afterMotion;
		final Event event;
		volatile boolean completed;
		PositionSend(int ordinal, PositionKey key, MotionReceipt afterMotion, Event event) {
			this.ordinal = ordinal; this.key = key; this.afterMotion = afterMotion; this.event = event;
		}
		boolean qualified() { return completed && afterMotion != null && afterMotion.sent.completed && event != null && event.index > afterMotion.processed.index; }
	}
	private record PositionReceipt(PositionSend sent, Event processed) {}

	public static final class Trace {
		public final String name;
		public final ServerPlayer server;
		public final LocalPlayer owner;
		public final Body ownerBefore, serverBefore;
		public final Vec3 side;
		private final List<Event> events = new ArrayList<>();
		private final ArrayDeque<MotionSend> motionQueue = new ArrayDeque<>();
		private final ArrayDeque<PositionSend> positionQueue = new ArrayDeque<>();
		private final List<PositionReceipt> positions = new ArrayList<>();
		private volatile Packet<?> manual;
		private volatile Vec3 expectedMotion;
		private volatile MotionReceipt provenMotion;
		private volatile Throwable observationFailure;
		private int motionSerial, positionSerial;
		private Trace(String name, ServerPlayer server, LocalPlayer owner, Body ownerBefore, Vec3 side) {
			this.name = name; this.server = server; this.owner = owner; this.ownerBefore = ownerBefore;
			this.serverBefore = Body.of(server); this.side = side;
		}
		private synchronized void failed(Throwable failure) { if (observationFailure == null) observationFailure = failure; }
		public void assertHealthy() { if (observationFailure != null) throw new AssertionError("Owner observation failed: " + name, observationFailure); }
		private <T> T observe(Supplier<T> observation) {
			if (observationFailure != null) return null;
			try { return observation.get(); } catch (Throwable failure) { failed(failure); return null; }
		}
		private Body snapshot(Player player) { return observe(() -> Body.of(player)); }
		public synchronized List<Event> events() { return List.copyOf(events); }
		public synchronized Event record(String kind, String data, Body before, Body after) {
			if (observationFailure != null) return null;
			try {
				if (events.size() >= 256) { failed(new AssertionError("Bounded Stone Hinge owner trace overflow")); return null; }
				Event event = new Event(events.size(), kind, data, before, after); events.add(event);
				Wildercord.LOGGER.info("STONE_HINGE_OWNER_EVENT case={} {}", name, event); return event;
			} catch (Throwable failure) { failed(failure); return null; }
		}
		public void expectMotion(Vec3 vector) { expectedMotion = vector; }
		public void manual(ClientboundSetEntityMotionPacket packet) { manual = packet; expectedMotion = packet.movement(); }
		private synchronized MotionSend sendingMotion(ClientboundSetEntityMotionPacket packet) {
			MotionSend sent = new MotionSend(++motionSerial, packet, expectedMotion != null && expectedMotion.equals(packet.movement())
				&& (manual == null || manual == packet), manual == packet);
			motionQueue.add(sent); return sent;
		}
		private synchronized MotionSend matchingMotion(Vec3 vector) {
			MotionSend first = motionQueue.peek();
			return first != null && first.matches(vector) ? motionQueue.remove() : null;
		}
		private synchronized PositionSend sendingPosition(ServerboundMovePlayerPacket packet, Body snapshot) {
			PositionKey key = PositionKey.of(packet);
			Event event = record("client-owner-position-sent", "ordinal=" + (positionSerial + 1) + " " + key, null, snapshot);
			PositionSend sent = new PositionSend(++positionSerial, key, provenMotion, event); positionQueue.add(sent); return sent;
		}
		private synchronized PositionSend matchingPosition(ServerboundMovePlayerPacket packet) {
			PositionSend first = positionQueue.peek();
			return first != null && first.key.equals(PositionKey.of(packet)) ? positionQueue.remove() : null;
		}
		public Event first(String kind) { return events().stream().filter(event -> event.kind.equals(kind)).findFirst().orElse(null); }
		public long count(String kind) { return events().stream().filter(event -> event.kind.equals(kind)).count(); }
		public Event expectedOwnerMotion() { MotionReceipt receipt = provenMotion; return receipt != null && receipt.sent.completed ? receipt.processed : null; }
		public synchronized List<Event> provenOwnerPositions() {
			return positions.stream().filter(receipt -> receipt.sent.qualified()
				// The post-impulse rise differs from every stable pre-experiment position; an older in-flight ground packet cannot alias it.
				&& Math.abs(receipt.sent.key.y - ownerBefore.position.y) > 1.0E-4).map(PositionReceipt::processed).toList();
		}
		public boolean hasOwnerPositionAfterMotion() { return expectedOwnerMotion() != null && !provenOwnerPositions().isEmpty(); }
		public boolean correction() { return count("server-correction-sent") > 0 || count("client-correction-start") > 0; }
		public double lateral(Vec3 position) { return position.subtract(serverBefore.position).dot(side); }
		private List<Event> uncorrectedOwnerObservations() {
			MotionReceipt motion = provenMotion; if (motion == null) return List.of();
			var copy = events();
			int corrected = copy.stream().filter(event -> event.kind.equals("client-correction-start")).mapToInt(Event::index).min().orElse(Integer.MAX_VALUE);
			return copy.stream().filter(event -> event.index > motion.processed.index && event.index < corrected && event.after != null
				&& (event.kind.equals("client-owner-position-sent") || event.kind.equals("client-tick"))).toList();
		}
		public int uncorrectedOwnerObservationCount() { return uncorrectedOwnerObservations().size(); }
		public double maximumUncorrectedOwnerLateral() {
			return uncorrectedOwnerObservations().stream().mapToDouble(event -> Math.abs(event.after.position.subtract(ownerBefore.position).dot(side))).max().orElse(Double.NaN);
		}
		public double peakOwnerRise() {
			return events().stream().filter(event -> event.kind.equals("client-tick") && event.after != null)
				.mapToDouble(event -> event.after.position.y - ownerBefore.position.y).max().orElse(0);
		}
	}
	public static void register() {
		if (registered) return; registered = true;
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Trace trace = clientTrace(client);
			if (trace != null) trace.record("client-tick", "native client tick", null, trace.snapshot(client.player));
		});
	}
	public static Trace start(String name, ServerPlayer server, LocalPlayer owner, Body ownerBefore, Vec3 side) {
		if (active != null || MOVEMENT.get() != null) throw new IllegalStateException("Overlapping owner probe");
		active = new Trace(name, server, owner, ownerBefore, side); return active;
	}
	public static void stop(Trace trace) {
		try { if (active != trace || MOVEMENT.get() != null) throw new IllegalStateException("Unbalanced owner probe"); }
		finally { if (active == trace) active = null; MOVEMENT.remove(); }
	}
	public static void clear() { active = null; MOVEMENT.remove(); }
	private static Trace clientTrace(Minecraft client) {
		Trace trace = active;
		return trace != null && client.isSameThread() && client.player == trace.owner && client.level == trace.owner.level() ? trace : null;
	}
	private static Trace serverTrace(ServerPlayer player) {
		Trace trace = active;
		return trace != null && trace.server == player && player.level().getServer().isSameThread() ? trace : null;
	}

	public static void serverSend(ServerPlayer player, Packet<?> packet, Runnable original) {
		Trace trace = serverTrace(player); if (trace == null) { original.run(); return; }
		MotionSend sent = packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == player.getId()
			? trace.observe(() -> trace.sendingMotion(motion)) : null;
		original.run();
		if (sent != null) { sent.completed = true; trace.record("server-motion-sent", "ordinal=" + sent.ordinal + " manual=" + sent.manual + " raw=" + sent.raw + " wire=" + sent.wire, null, trace.snapshot(player)); }
		else trace.observe(() -> {
			if (packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundTeleportEntityPacket teleport && teleport.id() == player.getId())
				trace.record("server-correction-sent", packet.toString(), null, trace.snapshot(player));
			else if (packet instanceof ClientboundEntityPositionSyncPacket sync && sync.id() == player.getId())
				trace.record("server-entity-position-sent", packet.toString(), null, trace.snapshot(player));
			return null;
		});
	}
	public static void clientMotion(ClientboundSetEntityMotionPacket packet, Runnable original) {
		Minecraft client = Minecraft.getInstance(); Trace trace = clientTrace(client);
		if (trace == null || packet.id() != trace.owner.getId()) { original.run(); return; }
		Body before = trace.snapshot(client.player); MotionSend sent = trace.observe(() -> trace.matchingMotion(packet.movement()));
		original.run(); Body after = trace.snapshot(client.player);
		Event event = trace.record("client-motion-processed", "sendOrdinal=" + (sent == null ? 0 : sent.ordinal) + " payload=" + packet.movement(), before, after);
		if (sent != null && sent.expected && after != null && event != null && after.motion.equals(packet.movement()) && trace.provenMotion == null)
			trace.provenMotion = new MotionReceipt(sent, event, packet.movement());
	}
	public static void clientCorrection(Packet<?> packet, Runnable original) {
		Minecraft client = Minecraft.getInstance(); Trace trace = clientTrace(client);
		if (trace == null || packet instanceof ClientboundTeleportEntityPacket teleport && teleport.id() != trace.owner.getId()) { original.run(); return; }
		Body before = trace.snapshot(client.player); trace.observe(() -> trace.record("client-correction-start", packet.toString(), before, null));
		original.run(); trace.observe(() -> trace.record("client-correction-processed", packet.toString(), before, trace.snapshot(client.player)));
	}
	/** Called only at LocalPlayer.sendPosition's real send invocation; never fabricates or changes a packet. */
	public static void clientPositionSend(LocalPlayer player, Packet<?> packet, Runnable original) {
		Trace trace = clientTrace(Minecraft.getInstance());
		if (trace == null || trace.owner != player || !(packet instanceof ServerboundMovePlayerPacket position) || !position.hasPosition()) { original.run(); return; }
		PositionSend sent = trace.observe(() -> trace.sendingPosition(position, trace.snapshot(player)));
		original.run(); if (sent != null) sent.completed = true;
	}
	public static void ownerMovePacket(ServerPlayer player, ServerboundMovePlayerPacket packet, Runnable original) {
		Trace trace = serverTrace(player); if (trace == null || !packet.hasPosition()) { original.run(); return; }
		PositionSend previous = MOVEMENT.get(); MOVEMENT.set(trace.observe(() -> trace.matchingPosition(packet)));
		try {
			trace.observe(() -> trace.record("server-owner-packet-received", PositionKey.of(packet).toString(), trace.snapshot(player), null));
			original.run();
		} finally { if (previous == null) MOVEMENT.remove(); else MOVEMENT.set(previous); }
	}
	public static void ownerPosition(ServerPlayer player, double x, double y, double z, Runnable original) {
		Trace trace = serverTrace(player); PositionSend sent = MOVEMENT.get();
		if (trace == null || sent == null) { original.run(); return; }
		Body before = trace.snapshot(player); original.run();
		Event event = trace.record("server-owner-position-processed", "sendOrdinal=" + sent.ordinal + " requested=" + new Vec3(x, y, z), before, trace.snapshot(player));
		if (event != null) synchronized (trace) { trace.positions.add(new PositionReceipt(sent, event)); }
	}
}
