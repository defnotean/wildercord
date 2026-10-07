package dev.wildercord.gametest.stonehinge.peer;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;

import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

/** Passive GameTest-only bridge from an accepted real owner position to original tracker bytes and a remote body. */
public final class StoneHingePeerProbe {
    private StoneHingePeerProbe() {}
    private static volatile Host host;
    private static volatile Peer peer;
    private static final ThreadLocal<ServerEntity> TRACKER = new ThreadLocal<>();
    private static boolean registered;
    public record Identity(String nonce, String profile, String name, String ownerUuid, int ownerEntity, int ownerGeneration,
                           String peerUuid, int peerEntity, int peerGeneration) {}
    public record Vector(double x, double y, double z) {
        static Vector of(Vec3 v) { return new Vector(v.x, v.y, v.z); }
        public Vec3 vec() { return new Vec3(x, y, z); }
    }
    public record PacketProof(int ordinal, String kind, String sha256, Vector target, Vector body, long tick,
                              int ownerAcceptanceIndex, int ownerSendOrdinal, boolean originalTracker, boolean interpolating) {}
    public record InterpolationStep(int packetOrdinal, long tick, Vector before, Vector after, Vector target) {}
    public record ClockProof(int observations, long firstTick, long lastTick, int tickTransitions, int nativeTimeResyncs, long maximumGapNanos) {}
    public record Report(Identity identity, List<PacketProof> packets, ClockProof clock, Vector finalPosition,
                         boolean correction, StoneHingeOwnerProbe.ChainEvidence ownerChain, List<InterpolationStep> interpolation, String error) {}
    private static final class Clock {
        int observations, tickTransitions, nativeTimeResyncs; long firstTick = Long.MIN_VALUE, lastTick = Long.MIN_VALUE, previousNanos, maximumGapNanos;
        synchronized void sample(long tick) {
            long now = System.nanoTime();
            if (observations != 0) {
                if (tick < lastTick) nativeTimeResyncs++;
                if (tick != lastTick) tickTransitions++;
                maximumGapNanos = Math.max(maximumGapNanos, now - previousNanos);
            } else firstTick = tick;
            observations++; lastTick = tick; previousNanos = now;
        }
        synchronized ClockProof proof() { return new ClockProof(observations, firstTick, lastTick, tickTransitions, nativeTimeResyncs, maximumGapNanos); }
    }
    private abstract static class Observation {
        final Identity identity;
        final List<PacketProof> packets = new ArrayList<>();
        final Clock clock = new Clock();
        volatile Throwable failure;
        volatile boolean correction;
        Observation(Identity identity) { this.identity = identity; }
        synchronized <T> T observe(Supplier<T> operation) {
            if (failure != null) return null;
            try { return operation.get(); } catch (Throwable error) { failure = error; return null; }
        }
        synchronized void add(String kind, String digest, Vec3 target, Player body, int acceptance, int send, boolean tracker) {
            if (packets.size() >= 256) throw new AssertionError("Bounded peer packet ledger overflow");
            packets.add(new PacketProof(packets.size() + 1, kind, digest, Vector.of(target), Vector.of(body.position()),
                body.level().getGameTime(), acceptance, send, tracker, body.isInterpolating()));
        }
        void healthy() { if (failure != null) throw new AssertionError("Passive peer observation failed", failure); }
    }
    private static final class Host extends Observation {
        final ServerPlayer owner, observer;
        final StoneHingeOwnerProbe.Trace ownerTrace;
        Host(Identity identity, ServerPlayer owner, ServerPlayer observer, StoneHingeOwnerProbe.Trace trace) {
            super(identity); this.owner = owner; this.observer = observer; ownerTrace = trace;
        }
    }
    private static final class Peer extends Observation {
        final Player owner, observer;
        final List<InterpolationStep> interpolation = new ArrayList<>();
        Vec3 previousPosition;
        Peer(Identity identity, Player owner, Player observer) { super(identity); this.owner = owner; this.observer = observer; }
    }
    public static void register() {
        if (registered) return; registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Host h = host;
            if (h != null && h.owner.level().getServer() == server) h.observe(() -> {
                check(server.getPlayerList().getPlayer(h.owner.getUUID()) == h.owner && h.owner.connection.player == h.owner
                    && server.getPlayerList().getPlayer(h.observer.getUUID()) == h.observer && h.observer.connection.player == h.observer,
                    "Both armed server body generations remain current");
                h.clock.sample(server.overworld().getGameTime()); return null;
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Peer p = peer;
            if (p != null) p.observe(() -> {
                check(client.player == p.observer && client.level == p.owner.level()
                    && client.level.getPlayerByUUID(p.owner.getUUID()) == p.owner, "Both armed peer body generations remain current");
                p.clock.sample(client.level.getGameTime());
                if (!p.packets.isEmpty() && p.previousPosition != null) {
                    PacketProof packet = p.packets.getLast(); Vec3 current = p.owner.position();
                    if (packet.interpolating && current.distanceToSqr(p.previousPosition) > 1.0E-12
                        && current.distanceToSqr(packet.target.vec()) < p.previousPosition.distanceToSqr(packet.target.vec())) {
                        check(p.interpolation.size() < 256, "Bounded native interpolation ledger");
                        p.interpolation.add(new InterpolationStep(packet.ordinal, client.level.getGameTime(), Vector.of(p.previousPosition), Vector.of(current), packet.target));
                    }
                    p.previousPosition = current;
                }
                return null;
            });
        });
    }
    public static void startHost(Identity id, ServerPlayer owner, ServerPlayer observer, StoneHingeOwnerProbe.Trace trace) {
        check(host == null && peer == null, "No overlapping peer observations");
        verify(id, owner, observer); host = new Host(id, owner, observer, trace);
    }
    public static void startPeer(Identity id) {
        check(host == null && peer == null, "No overlapping peer observations");
        Minecraft mc = Minecraft.getInstance(); Player owner = mc.level.getPlayerByUUID(UUID.fromString(id.ownerUuid));
        verify(id, owner, mc.player); check(owner != mc.player, "A distinct remote body is observed"); peer = new Peer(id, owner, mc.player);
    }
    private static void verify(Identity id, Player owner, Player observer) {
        check(owner != null && observer != null && owner.getUUID().toString().equals(id.ownerUuid) && owner.getId() == id.ownerEntity
            && observer.getUUID().toString().equals(id.peerUuid) && observer.getId() == id.peerEntity && id.ownerGeneration == 1 && id.peerGeneration == 1,
            "Exact nonce/case current body identities");
    }
    public static Report hostReport() {
        Host h = host; check(h != null, "Host trace exists"); h.healthy(); h.ownerTrace.assertHealthy();
        synchronized (h) { return new Report(h.identity, List.copyOf(h.packets), h.clock.proof(), Vector.of(h.owner.position()), h.correction || h.ownerTrace.correction(), h.ownerTrace.chainEvidence(), List.of(), null); }
    }
    public static Report peerReport() {
        Peer p = peer; check(p != null, "Peer trace exists"); p.healthy();
        synchronized (p) { return new Report(p.identity, List.copyOf(p.packets), p.clock.proof(), Vector.of(p.owner.position()), p.correction, null, List.copyOf(p.interpolation), null); }
    }
    public static void clear() { host = null; peer = null; TRACKER.remove(); }
    /** The scope is only the original ServerEntity.sendChanges invocation, never a manually broadcast packet. */
    public static void tracker(ServerEntity tracker, Entity entity, Runnable original) {
        Host h = host;
        if (h == null || h.owner != entity || !entity.level().getServer().isSameThread()) { original.run(); return; }
        ServerEntity previous = TRACKER.get(); TRACKER.set(tracker);
        try { original.run(); } finally { if (previous == null) TRACKER.remove(); else TRACKER.set(previous); }
    }
    public static boolean originalTracker(ServerPlayer owner) { Host h = host; return h != null && h.owner == owner && TRACKER.get() != null && owner.level().getServer().isSameThread(); }
    public static void serverSend(ServerPlayer recipient, Packet<?> packet, Runnable original) {
        Host h = host;
        if (h == null || recipient != h.observer || !recipient.level().getServer().isSameThread()) { original.run(); return; }
        ServerEntity tracker = TRACKER.get();
        // Observation errors never suppress or replay the original native operation.
        Object[] observation = h.observe(() -> {
            if (packet instanceof ClientboundTeleportEntityPacket t && t.id() == h.owner.getId()) h.correction = true;
            if (tracker == null || !positionPacket(packet, h.owner)) return null;
            var accepted = h.ownerTrace.provenOwnerPositions();
            if (accepted.isEmpty()) return null;
            var last = accepted.getLast();
            if (!last.after().position().equals(h.owner.position())) return null;
            Vec3 target = target(packet, tracker.getPositionBase());
            check(target.distanceToSqr(last.after().position()) <= 3.0 / (4096.0 * 4096.0), "Original tracker follows exact accepted owner position within native quantization");
            int send = Integer.parseInt(last.data().substring("sendOrdinal=".length(), last.data().indexOf(' ')));
            return new Object[] { packet.getClass().getSimpleName(), digest(packet), target, last.index(), send };
        });
        original.run();
        if (observation != null) h.observe(() -> { h.add((String) observation[0], (String) observation[1], (Vec3) observation[2], h.owner,
            (Integer) observation[3], (Integer) observation[4], true); return null; });
    }
    public static void peerPacket(Packet<?> packet, Runnable original) {
        Peer p = peer; Minecraft mc = Minecraft.getInstance();
        if (p == null || !mc.isSameThread() || mc.level != p.owner.level()) { original.run(); return; }
        Object[] observation = p.observe(() -> {
            if (packet instanceof ClientboundTeleportEntityPacket t && t.id() == p.owner.getId()) p.correction = true;
            if (!positionPacket(packet, p.owner)) return null;
            return new Object[] { packet.getClass().getSimpleName(), digest(packet), target(packet, p.owner.getPositionCodec().getBase()) };
        });
        original.run();
        if (observation != null) p.observe(() -> {
            Vec3 target = (Vec3) observation[2];
            check(p.owner.getPositionCodec().getBase().equals(target), "Native peer handler applies the exact decoded position codec target");
            p.add((String) observation[0], (String) observation[1], target, p.owner, -1, -1, false); p.previousPosition = p.owner.position(); return null;
        });
    }
    private static boolean positionPacket(Packet<?> packet, Player owner) {
        return packet instanceof ClientboundMoveEntityPacket move && move.hasPosition() && move.getEntity(owner.level()) == owner
            || packet instanceof ClientboundEntityPositionSyncPacket sync && sync.id() == owner.getId();
    }
    private static Vec3 target(Packet<?> packet, Vec3 base) {
        if (packet instanceof ClientboundEntityPositionSyncPacket sync) return sync.position().endPosition();
        VecDeltaCodec codec = new VecDeltaCodec(); codec.setBase(base);
        return ((ClientboundMoveEntityPacket) packet).getPositionDelta().decode(codec).endPosition();
    }
    /** Hash each original codec payload including entity ID, preserving every path step and rotation bit. */
    private static String digest(Packet<?> packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            switch (packet) {
                case ClientboundMoveEntityPacket.Pos p -> ClientboundMoveEntityPacket.Pos.STREAM_CODEC.encode(buffer, p);
                case ClientboundMoveEntityPacket.PosRot p -> ClientboundMoveEntityPacket.PosRot.STREAM_CODEC.encode(buffer, p);
                case ClientboundEntityPositionSyncPacket p -> ClientboundEntityPositionSyncPacket.STREAM_CODEC.encode(buffer, p);
                default -> throw new AssertionError("Unrecognized native position packet");
            }
            check(buffer.readableBytes() <= 16384, "Bounded native movement payload");
            byte[] bytes = new byte[buffer.readableBytes()]; buffer.getBytes(buffer.readerIndex(), bytes);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
        finally { buffer.release(); }
    }
    public static void verifyReports(Report server, Report client, boolean moved) {
        check(server.identity.equals(client.identity) && !server.correction && !client.correction, "Exact profile/case/bodies with no experiment correction or teleport");
        verifyClock(server.clock); verifyClock(client.clock);
        var chain = server.ownerChain; check(chain != null && client.ownerChain == null, "Only host owns native owner/server chain evidence");
        if (moved) {
            var motion = chain.motion();
            check(motion != null && motion.completed() && !motion.manual() && motion.originalTracker() && motion.sendStartIndex() >= 0 && motion.sendStartIndex() < motion.appliedIndex() && motion.applied().equals(motion.wire()), "Genuine owner applied exact original encoded native motion");
        }
        int next = 0; boolean interpolated = false;
        for (PacketProof sent : server.packets) {
            check(sent.originalTracker && sent.ownerAcceptanceIndex >= 0 && sent.ownerSendOrdinal > 0, "Native tracker packet is causally after a real accepted owner position");
            var accepted = chain.positions().stream().filter(p -> p.acceptanceIndex() == sent.ownerAcceptanceIndex && p.sendOrdinal() == sent.ownerSendOrdinal).findFirst().orElseThrow();
            check(accepted.completed() && accepted.motionOrdinal() == chain.motion().sendOrdinal() && accepted.sentIndex() > chain.motion().appliedIndex()
                && accepted.acceptanceIndex() > accepted.sentIndex() && accepted.requested().equals(accepted.sentPosition()) && accepted.requested().equals(accepted.acceptedPosition())
                && sent.target.vec().distanceToSqr(accepted.acceptedPosition()) <= 3.0 / (4096.0 * 4096.0),
                "Same real position send is natively accepted before original tracked encoded delivery");
            while (next < client.packets.size() && !same(sent, client.packets.get(next))) next++;
            check(next < client.packets.size(), "Peer processes each causally qualified original tracker payload in order");
            PacketProof received = client.packets.get(next);
            interpolated |= received.interpolating && client.interpolation.stream().anyMatch(step -> step.packetOrdinal == received.ordinal
                && step.target.equals(received.target) && step.after.vec().distanceToSqr(step.before.vec()) > 1.0E-12
                && step.after.vec().distanceToSqr(step.target.vec()) < step.before.vec().distanceToSqr(step.target.vec()));
            next++;
        }
        check(!moved || !server.packets.isEmpty() && interpolated, "Real owner movement requires qualified original tracker delivery and a native peer interpolation step");
        check(server.finalPosition.vec().distanceToSqr(client.finalPosition.vec()) < 0.002 * 0.002,
            "Independent peer interpolates to the eventual accepted native position, not a same-wall-clock comparison");
    }
    public static boolean converged(Report server) {
        Report local = peerReport();
        return local.finalPosition.vec().distanceToSqr(server.finalPosition.vec()) < 0.002 * 0.002
            && (server.packets.isEmpty() || local.packets.stream().anyMatch(p -> same(server.packets.getLast(), p)));
    }
    private static boolean same(PacketProof left, PacketProof right) {
        return left.kind.equals(right.kind) && left.sha256.equals(right.sha256)
            && left.target.vec().distanceToSqr(right.target.vec()) <= 3.0 / (4096.0 * 4096.0);
    }
    private static void verifyClock(ClockProof clock) {
        check(clock.observations >= 4 && clock.tickTransitions >= 3 && clock.maximumGapNanos <= 2_000_000_000L,
            "Actual native ticks advance throughout bounded measurement; thread stalls are not delay evidence");
    }
    private static void check(boolean condition, String reason) { if (!condition) throw new AssertionError(reason); }
}
