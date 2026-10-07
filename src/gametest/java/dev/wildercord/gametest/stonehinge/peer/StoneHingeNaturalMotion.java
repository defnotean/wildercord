package dev.wildercord.gametest.stonehinge.peer;

import dev.wildercord.gametest.stonehinge.StoneHingeOwnerProbe;
import dev.wildercord.gametest.stonehinge.StoneHingeVelocityExperiment;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Passive, one-release adapter; it never sends packets, changes motion, or calls a native tick itself. */
public final class StoneHingeNaturalMotion {
    private StoneHingeNaturalMotion() {}
    private static volatile Observation active;
    private static final class Observation {
        final StoneHingeOwnerProbe.Trace trace;
        final LivingEntity source;
        final ServerPlayer owner;
        final ServerGamePacketListenerImpl connection;
        final ServerLevel level;
        final ServerEntity tracker;
        final StoneHingeNativeDispatch proof;
        Observation(StoneHingeOwnerProbe.Trace trace, LivingEntity source) {
            this.trace = trace; this.source = source; owner = trace.server; connection = owner.connection; level = owner.level();
            tracker = StoneHingePeerProbe.retainedTracker(owner);
            proof = new StoneHingeNativeDispatch(frame("natural-release"), 1, tracker, StoneHingePeerProbe.trackerWitnessIndex(owner));
        }
        StoneHingeNativeDispatch.Frame frame(String kind) {
            var body = StoneHingeOwnerProbe.Body.of(owner);
            var state = StoneHingeVelocityExperiment.State.of(owner);
            var event = trace.record(kind, "source=" + source.getUUID() + " serverTick=" + level.getServer().getTickCount(), null, body);
            boolean valid = trace.current() && level.getServer().isSameThread() && owner.level() == level
                && owner.connection == connection && connection.player == owner && level.getServer().getPlayerList().getPlayer(owner.getUUID()) == owner
                && owner.isAlive() && !owner.isRemoved() && source.level() == level && !source.isRemoved()
                && level.getEntity(source.getId()) == source && StoneHingePeerProbe.retainedTracker(owner) == tracker;
            return new StoneHingeNativeDispatch.Frame(new StoneHingeNativeDispatch.Body(vector(body.position()), vector(body.motion()),
                body.fall(), body.grounded(), body.neutral(), body.health(), state.absorption(), state.needsSync(), state.syncVelocity(), body.horizontalCollision()),
                body.tick(), level.getServer().getTickCount(), owner.getId(), owner.getUUID().toString(), source.getId(), source.getUUID().toString(),
                valid, ((StoneHingeOwnerProbe.ConnectionState) connection).stoneHinge$awaitingTeleport(), event == null ? -1 : event.index(), body.pose());
        }
    }
    public static void arm(StoneHingeOwnerProbe.Trace trace, LivingEntity source, StoneHingeVelocityExperiment.Strike strike) {
        if (active != null || !trace.server.level().getServer().isSameThread() || !strike.receipt().eligibleReceipt()
            || strike.receipt().onlyHit().source().getEntity() != source
            || !strike.nativeOutcome().equals(StoneHingeVelocityExperiment.State.of(trace.server))
            || trace.count("server-motion-start") != 0) throw new AssertionError("One exact eligible natural release before any motion dispatch");
        // Eligibility intentionally belongs to the immediate strike; physics will change the live motion.
        active = new Observation(trace, source);
        active.proof.healthy();
    }
    private static Observation pending(Entity entity) {
        Observation value = active;
        return value != null && value.owner == entity && value.proof.pending() ? value : null;
    }
    public static boolean listener(ServerGamePacketListenerImpl listener, Supplier<Boolean> original) {
        Observation value = pending(listener.player); if (value == null) return original.get();
        connection(listener);
        value.proof.observe(() -> value.proof.listenerEnter(value.frame("natural-listener-start")));
        boolean result = value.proof.original(original);
        value.proof.observe(() -> value.proof.listenerExit(value.frame("natural-listener-end"), result));
        return result;
    }
    public static void callSite(ServerGamePacketListenerImpl listener, ServerPlayer player, Runnable original) {
        Observation value = pending(listener.player); if (value == null) { original.run(); return; }
        connection(listener);
        if (player != value.owner) value.proof.invalidate("Owning listener invoked another player's physics");
        value.proof.callSiteEnter();
        try { value.proof.original(() -> { original.run(); return null; }); }
        finally { value.proof.callSiteExit(); }
    }
    public static void step(ServerPlayer player, Runnable original) {
        Observation value = pending(player); if (value == null) { original.run(); return; }
        value.proof.observe(() -> value.proof.stepEnter(value.frame("natural-step-start")));
        value.proof.original(() -> { original.run(); return null; });
        value.proof.observe(() -> value.proof.stepExit(value.frame("natural-step-end")));
    }
    public static void tracker(ServerPlayer player, ServerEntity tracker, Runnable original) {
        Observation value = pending(player); if (value == null) { original.run(); return; }
        value.proof.observe(() -> value.proof.trackerEnter(value.frame("natural-tracker-start"), tracker));
        value.proof.original(() -> { original.run(); return null; });
        value.proof.observe(() -> value.proof.trackerExit(value.frame("natural-tracker-end"), tracker));
    }
    /** Null means this is an ordinary fixture; a natural fixture can never fall back to immediate-vector matching. */
    public static Boolean expected(StoneHingeOwnerProbe.Trace trace, ServerEntity tracker, ClientboundSetEntityMotionPacket packet, int ordinal, boolean manual) {
        Observation value = active; if (value == null || value.trace != trace) return null;
        if (!value.proof.pending()) return false;
        boolean[] expected = {false};
        value.proof.observe(() -> expected[0] = value.proof.sendEnter(value.frame("natural-motion-start"), tracker, packet,
            packet.id(), ordinal, vector(packet.movement()), manual));
        return expected[0];
    }
    public static void sent(ServerPlayer player, ClientboundSetEntityMotionPacket packet) {
        Observation value = pending(player);
        if (value != null) value.proof.observe(() -> value.proof.sendExit(value.frame("natural-motion-end"), packet));
    }
    public static void ownerPacket(ServerGamePacketListenerImpl connection, ServerboundMovePlayerPacket packet,
                                   Supplier<StoneHingeNativeDispatch.OwnerSend> sent,
                                   Supplier<StoneHingeNativeDispatch.OwnerPacket> received, Runnable original) {
        Observation value = pending(connection.player); if (value == null) { original.run(); return; }
        connection(connection);
        value.proof.observe(() -> value.proof.ownerPacketEnter(value.frame("natural-owner-packet-start"), sent.get(), received.get(), packet));
        value.proof.original(() -> { original.run(); return null; });
        value.proof.observe(() -> value.proof.ownerPacketExit(value.frame("natural-owner-packet-end"), packet));
    }
    public static void otherMotion(ServerPlayer player, ClientboundSetEntityMotionPacket packet) {
        Observation value = pending(player);
        if (value != null && packet.id() != player.getId() && StoneHingePeerProbe.currentTracker(player) != null)
            value.proof.invalidate("Wrong entity motion inside the original owner tracker");
    }
    public static void connection(ServerGamePacketListenerImpl connection) {
        Observation value = pending(connection.player);
        if (value != null && connection != value.connection) value.proof.invalidate("Native operation used a different owner connection");
    }
    public static void originalSend(ServerPlayer player, Runnable original) {
        Observation value = pending(player);
        if (value == null) original.run(); else value.proof.original(() -> { original.run(); return null; });
    }
    public static void contaminate(Entity player, String reason) {
        Observation value = pending(player); if (value != null) value.proof.invalidate(reason);
    }
    public static void healthy(StoneHingeOwnerProbe.Trace trace) {
        Observation value = active; if (value != null && value.trace == trace) value.proof.healthy();
    }
    public static StoneHingeNativeDispatch.Evidence evidence(StoneHingeOwnerProbe.Trace trace) {
        Observation value = active; return value != null && value.trace == trace ? value.proof.evidence() : null;
    }
    public static String diagnostic(StoneHingeOwnerProbe.Trace trace) {
        Observation value = active;
        return value == null || value.trace != trace ? "unarmed" : value.proof.phase() + " release=" + value.proof.releaseMotion() + " step=" + value.proof.stepMotion();
    }
    public static void clear() { active = null; }
    private static StoneHingeNativeDispatch.Vector vector(Vec3 value) { return new StoneHingeNativeDispatch.Vector(value.x, value.y, value.z); }
}
