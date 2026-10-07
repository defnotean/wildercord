package dev.wildercord.gametest.stonehinge.peer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import dev.wildercord.gametest.stonehinge.peer.StoneHingeNativeDispatch.Body;
import dev.wildercord.gametest.stonehinge.peer.StoneHingeNativeDispatch.Frame;
import dev.wildercord.gametest.stonehinge.peer.StoneHingeNativeDispatch.Vector;

/** Pure-JDK observer contract tests. These do not run Minecraft or prove native gameplay. */
public final class StoneHingeNativeDispatchTest {
    private static int checks;
    private static int rejectedScenarios;
    private static final Vector RELEASE_MOTION = new Vector(0.0, 0.36080000519752503, -0.4000000059604645);
    private static final Vector STEP_MOTION = new Vector(0.0, 0.2751840104494096, -0.2184000286221508);
    private static final Body RELEASE_BODY = new Body(new Vector(12.25, 70.0, -33.5), RELEASE_MOTION,
        0.0, false, true, 14.5f, 2.0f, true, true, false);
    private static final Body STEP_BODY = new Body(new Vector(12.25, 70.36080000519752, -33.9), STEP_MOTION,
        0.25, false, true, 14.5f, 2.0f, true, true, false);
    private static final Body LISTENER_BODY = withPosition(STEP_BODY, RELEASE_BODY.position());
    private static final Body SEND_BODY = new Body(LISTENER_BODY.position(), STEP_MOTION, LISTENER_BODY.fall(),
        false, true, 14.5f, 2.0f, false, false, false);

    private enum Point { LISTENER_ENTER, STEP_ENTER, STEP_EXIT, LISTENER_EXIT, TRACKER_ENTER, SEND_ENTER, SEND_EXIT, TRACKER_EXIT }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static Throwable caught(Runnable operation) {
        try { operation.run(); }
        catch (Throwable failure) { return failure; }
        throw new AssertionError("Expected operation to fail");
    }

    private static Frame frame(Body body, long gameTick, int serverTick, int eventIndex) {
        return new Frame(body, gameTick, serverTick, 71, "owner-generation-1", 93, "master-generation-1",
            true, false, eventIndex + 1); // Index zero is the retained pre-release tracker identity witness.
    }

    private static Frame body(Frame source, Body body) {
        return new Frame(body, source.gameTick(), source.serverTick(), source.ownerEntity(), source.ownerUuid(),
            source.sourceEntity(), source.sourceUuid(), source.valid(), source.awaitingTeleport(), source.eventIndex());
    }

    private static Frame clock(Frame source, long gameTick, int serverTick) {
        return new Frame(source.body(), gameTick, serverTick, source.ownerEntity(), source.ownerUuid(),
            source.sourceEntity(), source.sourceUuid(), source.valid(), source.awaitingTeleport(), source.eventIndex());
    }

    private static Frame index(Frame source, int eventIndex) {
        return new Frame(source.body(), source.gameTick(), source.serverTick(), source.ownerEntity(), source.ownerUuid(),
            source.sourceEntity(), source.sourceUuid(), source.valid(), source.awaitingTeleport(), eventIndex);
    }

    private static Frame invalid(Frame source, String field) {
        return new Frame(source.body(), source.gameTick(), source.serverTick(),
            source.ownerEntity() + (field.equals("owner entity") ? 1 : 0),
            field.equals("owner UUID") ? "other-owner-generation" : source.ownerUuid(),
            source.sourceEntity() + (field.equals("source entity") ? 1 : 0),
            field.equals("source UUID") ? "other-master-generation" : source.sourceUuid(),
            !field.equals("valid false") && source.valid(),
            field.equals("correction pending") || source.awaitingTeleport(), source.eventIndex());
    }

    private static Body withPosition(Body source, Vector position) {
        return new Body(position, source.motion(), source.fall(), source.grounded(), source.neutral(), source.health(),
            source.absorption(), source.needsSync(), source.syncVelocity(), source.collision());
    }

    private static Body changed(Body source, String field) {
        return new Body(field.equals("position") ? new Vector(13.25, 70.0, -33.5) : source.position(),
            field.equals("motion") ? new Vector(0.125, 0.125, 0.125) : source.motion(),
            source.fall() + (field.equals("fall") ? 1 : 0),
            field.equals("grounded") != source.grounded(),
            field.equals("neutral") != source.neutral(),
            source.health() - (field.equals("health") ? 1 : 0),
            source.absorption() - (field.equals("absorption") ? 1 : 0),
            field.equals("needsSync") != source.needsSync(),
            field.equals("syncVelocity") != source.syncVelocity(),
            field.equals("collision") != source.collision());
    }

    private static final class Fixture {
        final Frame release = frame(RELEASE_BODY, 100, 200, 0);
        final Frame listener = frame(RELEASE_BODY, 100, 200, 1);
        final Frame before = frame(RELEASE_BODY, 100, 200, 2);
        final Frame after = frame(STEP_BODY, 100, 200, 3);
        final Frame exit = frame(LISTENER_BODY, 100, 200, 4);
        final Frame tracking = frame(LISTENER_BODY, 101, 201, 5);
        final Frame sending = frame(SEND_BODY, 101, 201, 6);
        final Frame sentExit = frame(SEND_BODY, 101, 201, 7);
        final Frame trackerExit = frame(SEND_BODY, 101, 201, 8);
        final Object tracker = new Object();
        final Object packet = new Object();
        final StoneHingeNativeDispatch dispatch;

        Fixture() { dispatch = new StoneHingeNativeDispatch(release, 1, tracker, 0); }
        Fixture(Frame release, int ordinal) { dispatch = new StoneHingeNativeDispatch(release, ordinal, tracker, 0); }
        Fixture(boolean missingTracker, int witnessIndex) {
            dispatch = new StoneHingeNativeDispatch(release, 1, missingTracker ? null : tracker, witnessIndex);
        }

        Frame at(Point point) {
            return switch (point) {
                case LISTENER_ENTER -> listener;
                case STEP_ENTER -> before;
                case STEP_EXIT -> after;
                case LISTENER_EXIT -> exit;
                case TRACKER_ENTER -> tracking;
                case SEND_ENTER -> sending;
                case SEND_EXIT -> sentExit;
                case TRACKER_EXIT -> trackerExit;
            };
        }

        void prepare(Point point) {
            if (point == Point.LISTENER_ENTER) return;
            dispatch.listenerEnter(listener);
            dispatch.callSiteEnter();
            if (point == Point.STEP_ENTER) return;
            dispatch.stepEnter(before);
            if (point == Point.STEP_EXIT) return;
            dispatch.stepExit(after);
            dispatch.callSiteExit();
            if (point == Point.LISTENER_EXIT) return;
            dispatch.listenerExit(exit, false);
            if (point == Point.TRACKER_ENTER) return;
            dispatch.trackerEnter(tracking, tracker);
            if (point == Point.SEND_ENTER) return;
            send();
            if (point == Point.SEND_EXIT) return;
            dispatch.sendExit(sentExit, packet);
        }

        void apply(Point point, Frame frame) {
            switch (point) {
                case LISTENER_ENTER -> dispatch.listenerEnter(frame);
                case STEP_ENTER -> dispatch.stepEnter(frame);
                case STEP_EXIT -> dispatch.stepExit(frame);
                case LISTENER_EXIT -> dispatch.listenerExit(frame, false);
                case TRACKER_ENTER -> dispatch.trackerEnter(frame, tracker);
                case SEND_ENTER -> dispatch.sendEnter(frame, tracker, packet, release.ownerEntity(), 1, STEP_MOTION, false);
                case SEND_EXIT -> dispatch.sendExit(frame, packet);
                case TRACKER_EXIT -> dispatch.trackerExit(frame, tracker);
            }
        }

        boolean send() {
            return dispatch.sendEnter(sending, tracker, packet, release.ownerEntity(), 1, STEP_MOTION, false);
        }

        void complete() {
            prepare(Point.SEND_ENTER);
            check(send(), "The exact selected send is accepted");
            dispatch.sendExit(sentExit, packet);
            dispatch.trackerExit(trackerExit, tracker);
            dispatch.healthy();
        }

        // Invoked even after failure: all five simulated original operations must still execute once.
        void replayOriginals(AtomicInteger calls) {
            dispatch.listenerEnter(listener);
            dispatch.original(() -> {
                calls.incrementAndGet();
                dispatch.callSiteEnter();
                dispatch.original(() -> {
                    calls.incrementAndGet();
                    dispatch.stepEnter(before);
                    dispatch.original(() -> { calls.incrementAndGet(); return null; });
                    dispatch.stepExit(after);
                    return null;
                });
                dispatch.callSiteExit();
                return null;
            });
            dispatch.listenerExit(exit, false);
            dispatch.trackerEnter(tracking, tracker);
            dispatch.original(() -> {
                calls.incrementAndGet();
                check(!send(), "Latched failure cannot select a later matching packet");
                dispatch.original(() -> { calls.incrementAndGet(); return null; });
                dispatch.sendExit(sentExit, packet);
                return null;
            });
            dispatch.trackerExit(trackerExit, tracker);
        }
    }

    private static Throwable assertLatched(Fixture fixture, String label) {
        Throwable result = caught(fixture.dispatch::healthy);
        check(result instanceof AssertionError && result.getCause() != null, label + ": health exposes original cause");
        Throwable cause = result.getCause();
        String phase = fixture.dispatch.phase();
        check(caught(fixture.dispatch::evidence).getCause() == cause, label + ": failed evidence keeps original cause");
        AtomicInteger observationCalls = new AtomicInteger();
        fixture.dispatch.observe(observationCalls::incrementAndGet);
        check(observationCalls.get() == 0, label + ": observer stops after the first fault");
        AtomicInteger originalCalls = new AtomicInteger();
        fixture.replayOriginals(originalCalls);
        check(originalCalls.get() == 5, label + ": observation failure never suppresses or duplicates originals");
        check(fixture.dispatch.phase().equals(phase), label + ": later matching events cannot advance failed proof");
        check(caught(fixture.dispatch::healthy).getCause() == cause, label + ": later calls never replace first cause");
        check(caught(fixture.dispatch::evidence).getCause() == cause, label + ": later matching sequence cannot recover");
        rejectedScenarios++;
        return cause;
    }

    private static void reject(String label, Consumer<Fixture> setup, Consumer<Fixture> action) {
        Fixture fixture = new Fixture();
        setup.accept(fixture);
        fixture.dispatch.healthy();
        action.accept(fixture);
        assertLatched(fixture, label);
    }

    private static void rejectFrame(String label, Point point, UnaryOperator<Frame> mutation) {
        reject(label + " at " + point, fixture -> fixture.prepare(point),
            fixture -> fixture.apply(point, mutation.apply(fixture.at(point))));
    }

    private static void completeEvidence() {
        Fixture fixture = new Fixture();
        check(fixture.dispatch.pending() && fixture.dispatch.phase().equals("ARMED"), "Release starts an incomplete proof");
        check(fixture.dispatch.releaseMotion() == RELEASE_MOTION && fixture.dispatch.stepMotion() == null,
            "Captured release and unseen step remain distinct");
        fixture.complete();
        var evidence = fixture.dispatch.evidence();
        check(!fixture.dispatch.pending() && fixture.dispatch.phase().equals("COMPLETE"), "Only tracker exit completes proof");
        check(evidence.release() == fixture.release && evidence.beforeStep() == fixture.before && evidence.afterStep() == fixture.after,
            "Evidence retains exact release and both step frames");
        check(evidence.listenerExit() == fixture.exit && evidence.trackerEntry() == fixture.tracking && evidence.send() == fixture.sending,
            "Evidence retains exact listener, next tracker and send frames");
        check(evidence.sendExit() == fixture.sentExit && evidence.trackerExit() == fixture.trackerExit,
            "Evidence retains exact state and identity after both original send and tracker return");
        check(evidence.completed() && evidence.stepCount() == 1 && evidence.trackerCount() == 1 && evidence.motionOrdinal() == 1,
            "Successful evidence attests one step, one tracker and first owner motion");
        check(evidence.trackerWitnessIndex() == 0 && evidence.trackerIdentity().equals(Integer.toUnsignedString(System.identityHashCode(fixture.tracker))),
            "Evidence binds the tracker identity observed before natural release");
        check(fixture.dispatch.stepMotion() == STEP_MOTION && !STEP_MOTION.equals(RELEASE_MOTION),
            "Observed native post-step motion is distinct from immediate release motion");
        check(evidence.listenerExit().body().position().equals(evidence.beforeStep().body().position()),
            "Listener restores pre-step position");
        check(!evidence.afterStep().body().position().equals(evidence.listenerExit().body().position()),
            "Evidence preserves post-step position before native restoration");
        check(evidence.trackerEntry().gameTick() == evidence.release().gameTick() + 1
            && evidence.trackerEntry().serverTick() == evidence.release().serverTick() + 1,
            "Original tracker is exactly next game and server tick");
        check(!evidence.send().body().needsSync() && !evidence.send().body().syncVelocity(),
            "Native tracker may clear both synchronization flags before send");
        List<Frame> frames = List.of(evidence.release(), evidence.beforeStep(), evidence.afterStep(),
            evidence.listenerExit(), evidence.trackerEntry(), evidence.send(), evidence.sendExit(), evidence.trackerExit());
        for (int i = 1; i < frames.size(); i++) {
            check(frames.get(i - 1).eventIndex() < frames.get(i).eventIndex(), "Evidence event indexes strictly increase");
        }
        fixture.dispatch.invalidate("unrelated event after completed proof");
        check(fixture.dispatch.evidence().equals(evidence), "Completed immutable evidence is unaffected by later invalidation");
        Object marker = new Object();
        AtomicInteger calls = new AtomicInteger();
        check(fixture.dispatch.original(() -> { calls.incrementAndGet(); return marker; }) == marker && calls.get() == 1,
            "Successful original returns exact object and runs once");
    }

    private static void sequenceFailures() {
        reject("off-callsite step", f -> f.dispatch.listenerEnter(f.listener), f -> f.dispatch.stepEnter(f.before));
        reject("step before owning listener", f -> {}, f -> f.dispatch.stepEnter(f.before));
        reject("callsite before listener", f -> {}, f -> f.dispatch.callSiteEnter());
        reject("nested callsite", f -> f.prepare(Point.STEP_ENTER), f -> f.dispatch.callSiteEnter());
        reject("nested native step", f -> f.prepare(Point.STEP_EXIT), f -> f.dispatch.stepEnter(f.before));
        reject("second native step", f -> { f.prepare(Point.STEP_EXIT); f.dispatch.stepExit(f.after); },
            f -> f.dispatch.stepEnter(f.before));
        reject("step after callsite", f -> f.prepare(Point.LISTENER_EXIT), f -> f.dispatch.stepEnter(f.before));
        reject("extra step before tracker", f -> f.prepare(Point.TRACKER_ENTER), f -> f.dispatch.stepEnter(f.before));
        reject("nested listener", f -> f.dispatch.listenerEnter(f.listener), f -> f.dispatch.listenerEnter(f.listener));
        reject("later matching listener", f -> f.prepare(Point.TRACKER_ENTER), f -> f.dispatch.listenerEnter(f.listener));
        reject("missing step", f -> f.prepare(Point.STEP_ENTER), f -> f.dispatch.callSiteExit());
        reject("step exit without enter", f -> f.prepare(Point.STEP_ENTER), f -> f.dispatch.stepExit(f.after));
        reject("callsite exits before step returns", f -> f.prepare(Point.STEP_EXIT), f -> f.dispatch.callSiteExit());
        reject("duplicate step exit", f -> { f.prepare(Point.STEP_EXIT); f.dispatch.stepExit(f.after); },
            f -> f.dispatch.stepExit(f.after));
        reject("duplicate callsite exit", f -> f.prepare(Point.LISTENER_EXIT), f -> f.dispatch.callSiteExit());
        reject("listener exit inside callsite", f -> { f.prepare(Point.STEP_EXIT); f.dispatch.stepExit(f.after); },
            f -> f.dispatch.listenerExit(f.exit, false));
        reject("listener disconnect", f -> f.prepare(Point.LISTENER_EXIT), f -> f.dispatch.listenerExit(f.exit, true));
        reject("position restoration missing", f -> f.prepare(Point.LISTENER_EXIT),
            f -> f.dispatch.listenerExit(body(f.exit, STEP_BODY), false));
        reject("tracker before listener exit", f -> f.prepare(Point.LISTENER_EXIT), f -> f.dispatch.trackerEnter(f.tracking, f.tracker));
        reject("tracker before step", f -> f.prepare(Point.STEP_ENTER), f -> f.dispatch.trackerEnter(f.tracking, f.tracker));
        reject("null tracker", f -> f.prepare(Point.TRACKER_ENTER), f -> f.dispatch.trackerEnter(f.tracking, null));
        reject("replacement first tracker", f -> f.prepare(Point.TRACKER_ENTER), f -> f.dispatch.trackerEnter(f.tracking, new Object()));
        reject("nested or second tracker", f -> f.prepare(Point.SEND_ENTER), f -> f.dispatch.trackerEnter(f.tracking, f.tracker));
        reject("wrong tracker send", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, new Object(), f.packet, 71, 1, STEP_MOTION, false));
        reject("wrong tracker exit", f -> { f.prepare(Point.SEND_ENTER); f.send(); f.dispatch.sendExit(f.sentExit, f.packet); },
            f -> f.dispatch.trackerExit(f.trackerExit, new Object()));
        reject("tracker exit without tracker", f -> f.prepare(Point.TRACKER_ENTER), f -> f.dispatch.trackerExit(f.trackerExit, f.tracker));
    }

    private static void snapshotFailures() {
        for (Point point : Point.values()) {
            // The native adapter combines world, connection, owner and Master reference checks in valid.
            for (String field : List.of("owner entity", "owner UUID", "source entity", "source UUID", "valid false", "correction pending")) {
                rejectFrame(field, point, frame -> invalid(frame, field));
            }
            rejectFrame("non-neutral input", point, frame -> body(frame, changed(frame.body(), "neutral")));
            rejectFrame("null snapshot", point, frame -> null);
            rejectFrame("game tick advanced", point, frame -> clock(frame, frame.gameTick() + 1, frame.serverTick()));
            rejectFrame("server tick advanced", point, frame -> clock(frame, frame.gameTick(), frame.serverTick() + 1));
            rejectFrame("game tick stale", point, frame -> clock(frame, frame.gameTick() - 1, frame.serverTick()));
            rejectFrame("server tick stale", point, frame -> clock(frame, frame.gameTick(), frame.serverTick() - 1));
            rejectFrame("non-finite position", point, frame -> body(frame, withPosition(frame.body(), new Vector(Double.NaN, 70, 0))));
            rejectFrame("non-finite motion", point, frame -> body(frame, new Body(frame.body().position(),
                new Vector(0, Double.POSITIVE_INFINITY, 0), frame.body().fall(), frame.body().grounded(), frame.body().neutral(),
                frame.body().health(), frame.body().absorption(), frame.body().needsSync(), frame.body().syncVelocity(), frame.body().collision())));
        }
        for (Point point : List.of(Point.LISTENER_ENTER, Point.STEP_ENTER, Point.LISTENER_EXIT, Point.TRACKER_ENTER)) {
            for (String field : List.of("position", "motion", "fall", "grounded", "health", "absorption", "needsSync", "syncVelocity", "collision")) {
                rejectFrame("changed " + field, point, frame -> body(frame, changed(frame.body(), field)));
            }
        }
        for (String field : List.of("health", "absorption")) {
            rejectFrame("step changes " + field, Point.STEP_EXIT, frame -> body(frame, changed(frame.body(), field)));
        }
        for (Point point : List.of(Point.SEND_ENTER, Point.SEND_EXIT, Point.TRACKER_EXIT)) {
            for (String field : List.of("position", "motion", "fall", "grounded", "health", "absorption", "collision", "needsSync", "syncVelocity")) {
                rejectFrame("dispatch changes " + field, point, frame -> body(frame, changed(frame.body(), field)));
            }
        }
        for (Point point : List.of(Point.STEP_ENTER, Point.STEP_EXIT, Point.LISTENER_EXIT, Point.TRACKER_ENTER, Point.SEND_ENTER, Point.SEND_EXIT, Point.TRACKER_EXIT)) {
            int previous = switch (point) {
                case STEP_ENTER -> 1;
                case STEP_EXIT -> 3;
                case LISTENER_EXIT -> 4;
                case TRACKER_ENTER -> 5;
                case SEND_ENTER -> 6;
                case SEND_EXIT -> 7;
                case TRACKER_EXIT -> 8;
                default -> throw new AssertionError(point);
            };
            rejectFrame("duplicate evidence index", point, frame -> index(frame, previous));
            rejectFrame("reversed evidence index", point, frame -> index(frame, previous - 1));
        }
        rejectFrame("skipped next native tracker", Point.TRACKER_ENTER,
            frame -> clock(frame, frame.gameTick() + 1, frame.serverTick() + 1));
    }

    private static void sendFailures() {
        reject("absent owner send", f -> f.prepare(Point.SEND_ENTER), f -> f.dispatch.trackerExit(f.trackerExit, f.tracker));
        reject("send before selected tracker", f -> f.prepare(Point.TRACKER_ENTER), Fixture::send);
        reject("wrong owner packet entity", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 72, 1, STEP_MOTION, false));
        reject("manual owner send", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 71, 1, STEP_MOTION, true));
        reject("immediate release raw reused", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 71, 1, RELEASE_MOTION, false));
        reject("nearly equal raw is not exact", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 71, 1,
                new Vector(STEP_MOTION.x(), Math.nextUp(STEP_MOTION.y()), STEP_MOTION.z()), false));
        reject("wrong owner motion ordinal", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 71, 2, STEP_MOTION, false));
        reject("ordinal zero", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 71, 0, STEP_MOTION, false));
        reject("null raw vector", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, f.packet, 71, 1, null, false));
        reject("null packet", f -> f.prepare(Point.SEND_ENTER),
            f -> f.dispatch.sendEnter(f.sending, f.tracker, null, 71, 1, STEP_MOTION, false));
        reject("two sends before first returns", f -> { f.prepare(Point.SEND_ENTER); f.send(); },
            f -> f.dispatch.sendEnter(f.sending, f.tracker, new Object(), 71, 1, STEP_MOTION, false));
        reject("second send after first completed", f -> { f.prepare(Point.SEND_ENTER); f.send(); f.dispatch.sendExit(f.sentExit, f.packet); },
            f -> f.dispatch.sendEnter(f.sending, f.tracker, new Object(), 71, 1, STEP_MOTION, false));
        reject("same packet sent twice", f -> { f.prepare(Point.SEND_ENTER); f.send(); f.dispatch.sendExit(f.sentExit, f.packet); }, Fixture::send);
        reject("send never returned", f -> { f.prepare(Point.SEND_ENTER); f.send(); }, f -> f.dispatch.trackerExit(f.trackerExit, f.tracker));
        reject("send exit without enter", f -> f.prepare(Point.SEND_ENTER), f -> f.dispatch.sendExit(f.sentExit, f.packet));
        reject("different packet returned", f -> { f.prepare(Point.SEND_ENTER); f.send(); }, f -> f.dispatch.sendExit(f.sentExit, new Object()));
        reject("same send returned twice", f -> { f.prepare(Point.SEND_ENTER); f.send(); f.dispatch.sendExit(f.sentExit, f.packet); },
            f -> f.dispatch.sendExit(f.sentExit, f.packet));
        reject("wrong packet then later matching packet", f -> f.prepare(Point.SEND_ENTER), f -> {
            check(!f.dispatch.sendEnter(f.sending, f.tracker, new Object(), 72, 1, STEP_MOTION, false), "Wrong entity is not selected");
            check(!f.send(), "A later matching packet cannot rescue the wrong selected send");
        });
    }

    private static void invalidationFailures() {
        for (Point point : Point.values()) {
            for (String event : List.of("intervening damage", "intervening knockback", "intervening teleport correction")) {
                Fixture fixture = new Fixture();
                fixture.prepare(point);
                fixture.dispatch.invalidate(event);
                Throwable first = assertLatched(fixture, event + " before " + point);
                check(first.getMessage().equals(event), "Invalidation retains specific native event reason");
            }
        }
        Fixture releaseInvalid = new Fixture(invalid(frame(RELEASE_BODY, 100, 200, 0), "valid false"), 1);
        assertLatched(releaseInvalid, "release reference invalid");
        Fixture correctedRelease = new Fixture(invalid(frame(RELEASE_BODY, 100, 200, 0), "correction pending"), 1);
        assertLatched(correctedRelease, "release correction pending");
        Fixture nonNeutralRelease = new Fixture(body(frame(RELEASE_BODY, 100, 200, 0), changed(RELEASE_BODY, "neutral")), 1);
        assertLatched(nonNeutralRelease, "release non-neutral input");
        assertLatched(new Fixture(frame(RELEASE_BODY, 100, 200, 0), 2), "release has earlier owner motion");
        assertLatched(new Fixture(frame(RELEASE_BODY, 100, 200, 0), 0), "release ordinal is not one");
        assertLatched(new Fixture(true, 0), "no retained original tracker");
        assertLatched(new Fixture(false, -1), "missing retained tracker event");
        assertLatched(new Fixture(false, 1), "tracker chosen at release");
        assertLatched(new Fixture(false, 2), "tracker chosen after release");
        Fixture observedFailure = new Fixture();
        AssertionError original = new AssertionError("observer fault");
        observedFailure.dispatch.observe(() -> { throw original; });
        check(assertLatched(observedFailure, "observer error is latched") == original, "Original observer Throwable identity survives");
    }

    private static void missingStagesRemainIncomplete() {
        for (Point point : Point.values()) {
            Fixture fixture = new Fixture();
            fixture.prepare(point);
            fixture.dispatch.healthy();
            check(fixture.dispatch.pending(), "Missing next stage remains pending at " + point);
            Throwable failure = caught(fixture.dispatch::evidence);
            check(failure instanceof AssertionError && failure.getCause() == null,
                "Absent " + point + " cannot fabricate completion evidence");
        }
        Fixture fixture = new Fixture();
        fixture.prepare(Point.SEND_ENTER);
        fixture.send();
        fixture.dispatch.sendExit(fixture.sentExit, fixture.packet);
        check(fixture.dispatch.pending(), "A completed send alone is not tracker completion");
        check(caught(fixture.dispatch::evidence) instanceof AssertionError, "Missing native tracker return rejects proof");
        fixture.dispatch.trackerExit(fixture.trackerExit, fixture.tracker);
        check(fixture.dispatch.evidence().completed(), "An evidence read never substitutes for the remaining original tracker exit");
    }

    private static void nativeOperationFailures() {
        // Boundary-specific setup makes failures occur at the actual observer phases of each original operation.
        List<Consumer<Fixture>> stages = List.of(
            f -> f.dispatch.listenerEnter(f.listener),
            f -> f.prepare(Point.STEP_ENTER),
            f -> f.prepare(Point.STEP_EXIT),
            f -> f.prepare(Point.SEND_ENTER),
            f -> { f.prepare(Point.SEND_ENTER); f.send(); });
        List<String> labels = List.of("listener", "listener callsite", "doTick", "tracker", "owner send");
        for (int stage = 0; stage < stages.size(); stage++) {
            for (boolean error : List.of(false, true)) {
                Fixture fixture = new Fixture();
                stages.get(stage).accept(fixture);
                Throwable nativeFailure = error ? new AssertionError("native " + labels.get(stage))
                    : new IllegalStateException("native " + labels.get(stage));
                AtomicInteger calls = new AtomicInteger();
                Throwable result = caught(() -> fixture.dispatch.original(() -> {
                    calls.incrementAndGet();
                    if (nativeFailure instanceof Error failure) throw failure;
                    throw (RuntimeException) nativeFailure;
                }));
                check(result == nativeFailure && calls.get() == 1,
                    labels.get(stage) + ": original runs once and rethrows unchanged Throwable");
                check(assertLatched(fixture, "failed original " + labels.get(stage)) == nativeFailure,
                    labels.get(stage) + ": original failure permanently invalidates evidence");
                AssertionError later = new AssertionError("later original operation");
                AtomicInteger laterCalls = new AtomicInteger();
                check(caught(() -> fixture.dispatch.original(() -> { laterCalls.incrementAndGet(); throw later; })) == later,
                    "Later original errors also propagate unchanged after failure");
                check(laterCalls.get() == 1 && caught(fixture.dispatch::healthy).getCause() == nativeFailure,
                    "Later native error executes once without replacing first latched failure");
            }
        }
        Fixture fixture = new Fixture();
        AssertionError observer = new AssertionError("observation before original failure");
        fixture.dispatch.observe(() -> { throw observer; });
        RuntimeException nativeFailure = new IllegalArgumentException("original after observation failure");
        AtomicInteger calls = new AtomicInteger();
        check(caught(() -> fixture.dispatch.original(() -> { calls.incrementAndGet(); throw nativeFailure; })) == nativeFailure,
            "Observation failure never wraps or suppresses the native exception");
        check(calls.get() == 1 && assertLatched(fixture, "native failure following observation failure") == observer,
            "First observer fault remains the evidence cause while native failure propagates");
    }

    public static void main(String[] args) {
        completeEvidence();
        sequenceFailures();
        snapshotFailures();
        sendFailures();
        invalidationFailures();
        missingStagesRemainIncomplete();
        nativeOperationFailures();
        System.out.println("Stone Hinge native dispatch: " + checks + " checks passed; " + rejectedScenarios
            + " rejected scenarios (pure-JDK sequencing only; no Minecraft gameplay claim)");
    }
}
