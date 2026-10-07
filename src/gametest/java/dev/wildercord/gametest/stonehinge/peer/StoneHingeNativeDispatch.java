package dev.wildercord.gametest.stonehinge.peer;

import java.util.Objects;
import java.util.function.Supplier;

/** One immutable natural-release proof. Observers never alter or suppress an original operation. */
public final class StoneHingeNativeDispatch {
    public record Vector(double x, double y, double z) {}
    public record Body(Vector position, Vector motion, double fall, boolean grounded, boolean neutral,
                       float health, float absorption, boolean needsSync, boolean syncVelocity, boolean collision) {
        Body at(Vector position) { return new Body(position, motion, fall, grounded, neutral, health, absorption, needsSync, syncVelocity, collision); }
        Body dispatched() { return new Body(position, motion, fall, grounded, neutral, health, absorption, false, false, collision); }
    }
    /** valid includes reference identity of owner, connection, level and source, checked by the native adapter. */
    public record Frame(Body body, long gameTick, int serverTick, int ownerEntity, String ownerUuid,
                        int sourceEntity, String sourceUuid, boolean valid, boolean awaitingTeleport, int eventIndex) {}
    public record Evidence(Frame release, Frame beforeStep, Frame afterStep, Frame listenerExit, Frame trackerEntry,
                           Frame send, Frame sendExit, Frame trackerExit, int motionOrdinal, int stepCount, int trackerCount, boolean completed,
                           int trackerWitnessIndex, String trackerIdentity) {}
    private enum Phase { ARMED, LISTENER, STEP, STEPPED, READY, TRACKER, COMPLETE }
    private final Frame release;
    private final int motionOrdinal;
    private final Object expectedTracker;
    private final int trackerWitnessIndex;
    private volatile Phase phase = Phase.ARMED;
    private Frame beforeStep, afterStep, listenerExit, trackerEntry, send, sendExit, trackerExit;
    private int stepCount, trackerCount, sendCount;
    private boolean callSite, sendCompleted;
    private Object tracker, packet;
    private volatile Throwable failure;

    public StoneHingeNativeDispatch(Frame release, int motionOrdinal, Object expectedTracker, int trackerWitnessIndex) {
        this.release = Objects.requireNonNull(release); this.motionOrdinal = motionOrdinal;
        this.expectedTracker = expectedTracker; this.trackerWitnessIndex = trackerWitnessIndex;
        observe(() -> { valid(release); check(motionOrdinal == 1, "Natural release has no earlier owner motion");
            check(expectedTracker != null && trackerWitnessIndex >= 0 && trackerWitnessIndex < release.eventIndex,
                "Original owner tracker was observed before the natural release"); });
    }
    public boolean pending() { return phase != Phase.COMPLETE; }
    public String phase() { return phase.name(); }
    public Vector releaseMotion() { return release.body.motion; }
    public Vector stepMotion() { return afterStep == null ? null : afterStep.body.motion; }
    public void observe(Runnable observation) {
        if (failure != null) return;
        try { observation.run(); } catch (Throwable error) { fail(error); }
    }
    private synchronized void fail(Throwable error) { if (failure == null) failure = error; }
    public void healthy() { if (failure != null) throw new AssertionError("Natural native dispatch failed in " + phase, failure); }
    /** Both successful and throwing native operations execute once, even after an observer failed. */
    public <T> T original(Supplier<T> operation) {
        try { return operation.get(); }
        catch (Throwable error) { fail(error); throw error; }
    }
    public void invalidate(String reason) { if (pending()) observe(() -> { throw new AssertionError(reason); }); }
    public void listenerEnter(Frame frame) {
        observe(() -> { check(phase == Phase.ARMED, "Exactly the first listener tick after release"); sameMoment(frame, release);
            check(frame.body.equals(release.body), "Release state reaches native listener unchanged"); phase = Phase.LISTENER; });
    }
    public void callSiteEnter() {
        observe(() -> { check(phase == Phase.LISTENER && !callSite, "One original listener doTick callsite"); callSite = true; });
    }
    public void stepEnter(Frame frame) {
        observe(() -> { check(phase == Phase.LISTENER && callSite && stepCount == 0, "Exactly one non-nested native doTick at the listener callsite");
            sameMoment(frame, release); check(frame.body.equals(release.body), "Exact immediate strike state precedes the physics step");
            check(frame.eventIndex > release.eventIndex, "Release observation precedes physics"); beforeStep = frame; stepCount++; phase = Phase.STEP; });
    }
    public void stepExit(Frame frame) {
        observe(() -> { check(phase == Phase.STEP && callSite, "The original physics step completed once"); sameMoment(frame, release);
            check(frame.body.health == release.body.health && frame.body.absorption == release.body.absorption,
                "Native step preserves the captured wound and absorption");
            check(frame.eventIndex > beforeStep.eventIndex, "Native step completion follows entry"); afterStep = frame; phase = Phase.STEPPED; });
    }
    public void callSiteExit() {
        observe(() -> { check(phase == Phase.STEPPED && callSite, "The original callsite completed its single step"); callSite = false; });
    }
    public void listenerExit(Frame frame, boolean disconnected) {
        observe(() -> { check(phase == Phase.STEPPED && !callSite && !disconnected, "Native listener completes before tracker"); sameMoment(frame, release);
            check(frame.body.equals(afterStep.body.at(beforeStep.body.position)), "Only native listener position restoration follows doTick");
            check(frame.eventIndex > afterStep.eventIndex, "Listener restoration follows physics"); listenerExit = frame; phase = Phase.READY; });
    }
    public void trackerEnter(Frame frame, Object tracker) {
        observe(() -> { check(phase == Phase.READY && trackerCount == 0 && tracker == expectedTracker, "The retained original tracker follows one native step");
            valid(frame); check(frame.gameTick == release.gameTick + 1 && frame.serverTick == release.serverTick + 1,
                "Original dispatch is the next native server tick");
            check(frame.body.equals(listenerExit.body), "Completed native step reaches the tracker unchanged");
            check(frame.eventIndex > listenerExit.eventIndex && frame.body.syncVelocity, "Native hit synchronization reaches next tracker after listener");
            this.tracker = tracker; trackerEntry = frame; trackerCount++; phase = Phase.TRACKER; });
    }
    public boolean sendEnter(Frame frame, Object tracker, Object packet, int entity, int ordinal, Vector raw, boolean manual) {
        observe(() -> { check(phase == Phase.TRACKER && tracker == this.tracker && packet != null && sendCount == 0,
                "One owner motion in the selected original tracker invocation"); sameMoment(frame, trackerEntry);
            check(entity == release.ownerEntity && ordinal == motionOrdinal && !manual, "Exact original owner motion identity and ordinal");
            check(raw.equals(afterStep.body.motion) && raw.equals(listenerExit.body.motion) && raw.equals(trackerEntry.body.motion)
                && raw.equals(frame.body.motion), "Packet is exactly the previously observed native step motion");
            check(frame.body.equals(trackerEntry.body.dispatched()) && frame.eventIndex > trackerEntry.eventIndex,
                "Only native tracker sync-flag clearing precedes packet send");
            this.packet = packet; send = frame; sendCount++; });
        return failure == null && phase == Phase.TRACKER && this.packet == packet;
    }
    public void sendExit(Frame frame, Object packet) {
        observe(() -> { check(phase == Phase.TRACKER && this.packet == packet && sendCount == 1 && !sendCompleted,
                "Exact original owner send completed once"); sameMoment(frame, send);
            check(frame.body.equals(send.body) && frame.eventIndex > send.eventIndex, "Owner identity and state survive original send completion");
            sendExit = frame; sendCompleted = true; });
    }
    public void trackerExit(Frame frame, Object tracker) {
        observe(() -> { check(phase == Phase.TRACKER && this.tracker == tracker && sendCount == 1 && sendCompleted,
                "Selected tracker completed exactly one owner motion, without later packet search"); sameMoment(frame, sendExit);
            check(frame.body.equals(sendExit.body) && frame.eventIndex > sendExit.eventIndex, "Owner identity and state survive original tracker completion");
            trackerExit = frame; phase = Phase.COMPLETE; });
    }
    public Evidence evidence() {
        healthy(); check(phase == Phase.COMPLETE, "Natural dispatch proof is complete");
        return new Evidence(release, beforeStep, afterStep, listenerExit, trackerEntry, send, sendExit, trackerExit, motionOrdinal, stepCount, trackerCount, true,
            trackerWitnessIndex, Integer.toUnsignedString(System.identityHashCode(expectedTracker)));
    }
    private void sameMoment(Frame frame, Frame expected) {
        valid(frame); check(frame.gameTick == expected.gameTick && frame.serverTick == expected.serverTick, "Exact native operation clock");
    }
    private void valid(Frame frame) {
        check(frame != null && frame.valid && !frame.awaitingTeleport && frame.body.neutral && frame.eventIndex >= 0, "Same live owner, connection, world and Master without correction or input");
        check(frame.ownerEntity == release.ownerEntity && frame.ownerUuid.equals(release.ownerUuid)
            && frame.sourceEntity == release.sourceEntity && frame.sourceUuid.equals(release.sourceUuid), "Unchanged owner and source generations");
        check(Double.isFinite(frame.body.fall) && Float.isFinite(frame.body.health) && Float.isFinite(frame.body.absorption), "Finite native state");
        for (Vector vector : new Vector[] { frame.body.position, frame.body.motion })
            check(Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z), "Finite native body");
    }
    private static void check(boolean condition, String reason) { if (!condition) throw new AssertionError(reason); }
}
