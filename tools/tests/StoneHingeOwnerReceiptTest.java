package dev.wildercord.gametest.stonehinge.peer;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import dev.wildercord.gametest.stonehinge.peer.StoneHingeNativeDispatch.*;

/** Pure-JDK adversarial receipt fixtures. No native gameplay or transport is executed here. */
public final class StoneHingeOwnerReceiptTest {
    private static int checks, rejected;
    private static final Pose POSE = new Pose(0, 0, 0, 0, "STANDING");
    private static final Vector POSITION = new Vector(12.25, 70, -33.5);
    private static final Vector MOTION = new Vector(0, .2751840104494096, -.2184000286221508);
    private static final Body RELEASE = new Body(POSITION, new Vector(0, .36080000519752503, -.4000000059604645), 0, false, true, 14, 2, true, true, false);
    private static final Body STEP = new Body(new Vector(12.25, 70.36080000519752, -33.9), MOTION, .25, false, true, 14, 2, true, true, false);
    private static final Body RESTORED = STEP.at(POSITION);
    private static final OwnerPacket PACKET = new OwnerPacket("Pos", POSITION, false, 0, 0, true, false, "1".repeat(64));
    private static final OwnerBody OWNER = new OwnerBody(POSITION, new Vector(0, 0, 0), 0, true, true, 20, 71, 100, false, POSE);
    private static final OwnerSend SENT = new OwnerSend(3, 2, 3, 71, "owner", true, OWNER, PACKET);
    private static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    private static Throwable failure(Runnable run) {
        try { run.run(); } catch (Throwable failure) { return failure; }
        throw new AssertionError("Expected failure");
    }
    @SuppressWarnings("unchecked") private static <T extends Record> T change(T value, String name, Object replacement) {
        try {
            RecordComponent[] fields = value.getClass().getRecordComponents();
            Class<?>[] types = new Class<?>[fields.length]; Object[] args = new Object[fields.length]; boolean found = false;
            for (int i = 0; i < fields.length; i++) {
                types[i] = fields[i].getType(); args[i] = fields[i].getAccessor().invoke(value);
                if (fields[i].getName().equals(name)) { args[i] = replacement; found = true; }
            }
            check(found, "Known record field " + name);
            return (T) value.getClass().getDeclaredConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static Frame frame(Body body, boolean next, int index) {
        return new Frame(body, next ? 101 : 100, next ? 201 : 200, 71, "owner", 93, "master", true, false, index, POSE);
    }
    private static final class Fixture {
        final Object tracker = new Object(), packet = new Object();
        final Frame release = frame(RELEASE, false, 10), entry = frame(RESTORED, true, 16), returned = frame(RESTORED.grounded(true), true, 18);
        final StoneHingeNativeDispatch proof = new StoneHingeNativeDispatch(release, 1, tracker, 0);
        void ready() {
            proof.listenerEnter(frame(RELEASE, false, 11)); proof.callSiteEnter();
            proof.stepEnter(frame(RELEASE, false, 12)); proof.stepExit(frame(STEP, false, 13)); proof.callSiteExit();
            proof.listenerExit(frame(RESTORED, false, 14), false); proof.healthy();
        }
        void enter() { proof.ownerPacketEnter(entry, SENT, PACKET, packet); }
        void exit() { proof.ownerPacketExit(returned, packet); }
        void finish() {
            Body tracked = returned.body(), sent = tracked.dispatched();
            proof.trackerEnter(frame(tracked, true, 20), tracker);
            Object motion = new Object();
            proof.sendEnter(frame(sent, true, 21), tracker, motion, 71, 1, MOTION, false);
            proof.sendExit(frame(sent, true, 22), motion); proof.trackerExit(frame(sent, true, 23), tracker);
        }
    }
    private static void rejects(String label, Consumer<Fixture> action) {
        Fixture f = new Fixture(); f.ready(); action.accept(f);
        Throwable first = failure(f.proof::healthy); check(first instanceof AssertionError && first.getCause() != null, label);
        Throwable cause = first.getCause();
        AtomicInteger calls = new AtomicInteger();
        check(f.proof.original(() -> calls.incrementAndGet()) == 1 && calls.get() == 1, "Observer cannot suppress original " + label);
        f.enter(); f.exit(); f.finish();
        check(failure(f.proof::evidence).getCause() == cause, "No later packet or tracker rescue " + label); rejected++;
    }
    private static Object mutated(Record source, RecordComponent field) {
        try {
            Object old = field.getAccessor().invoke(source);
            return switch (field.getName()) {
                case "position" -> new Vector(Math.nextUp(POSITION.x()), POSITION.y(), POSITION.z());
                case "motion" -> new Vector(Math.nextUp(MOTION.x()), MOTION.y(), MOTION.z());
                default -> old instanceof Boolean b ? !b : old instanceof Double d ? Math.nextUp(d)
                    : old instanceof Float f ? Math.nextUp(f) : old instanceof Integer i ? i + 1
                    : old instanceof Long l ? l + 1 : "other";
            };
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static void exactBodies() {
        for (RecordComponent field : Body.class.getRecordComponents()) {
            rejects("handler entry changes " + field.getName(), f -> f.proof.ownerPacketEnter(
                change(f.entry, "body", change(f.entry.body(), field.getName(), mutated(f.entry.body(), field))), SENT, PACKET, f.packet));
            rejects("handler return changes " + field.getName(), f -> { f.enter(); f.proof.ownerPacketExit(
                change(f.returned, "body", change(f.returned.body(), field.getName(), mutated(f.returned.body(), field))), f.packet); });
            rejects("tracker after receipt changes " + field.getName(), f -> { f.enter(); f.exit(); f.proof.trackerEnter(
                frame(change(f.returned.body(), field.getName(), mutated(f.returned.body(), field)), true, 20), f.tracker); });
        }
        for (String field : List.of("ownerEntity", "ownerUuid", "sourceEntity", "sourceUuid", "valid", "awaitingTeleport", "gameTick", "serverTick")) {
            RecordComponent component = java.util.Arrays.stream(Frame.class.getRecordComponents()).filter(c -> c.getName().equals(field)).findFirst().orElseThrow();
            rejects("entry identity or clock " + field, f -> f.proof.ownerPacketEnter(change(f.entry, field, mutated(f.entry, component)), SENT, PACKET, f.packet));
            rejects("return identity or clock " + field, f -> { f.enter(); f.proof.ownerPacketExit(change(f.returned, field, mutated(f.returned, component)), f.packet); });
        }
        rejects("entry precedes listener", f -> f.proof.ownerPacketEnter(change(f.entry, "eventIndex", 14), SENT, PACKET, f.packet));
        rejects("return precedes entry", f -> { f.enter(); f.proof.ownerPacketExit(change(f.returned, "eventIndex", 16), f.packet); });
        rejects("tracker precedes return", f -> { f.enter(); f.exit(); f.proof.trackerEnter(frame(f.returned.body(), true, 18), f.tracker); });
        rejects("null handler entry", f -> f.proof.ownerPacketEnter(null, SENT, PACKET, f.packet));
        rejects("null handler return", f -> { f.enter(); f.proof.ownerPacketExit(null, f.packet); });
    }
    private static void signedZeroBodies() {
        Vector negativeZero = new Vector(-0.0, MOTION.y(), MOTION.z());
        check(!MOTION.equals(negativeZero), "Java vector record distinguishes IEEE signed zero");
        rejects("handler entry signed-zero motion", f -> f.proof.ownerPacketEnter(
            change(f.entry, "body", change(f.entry.body(), "motion", negativeZero)), SENT, PACKET, f.packet));
        rejects("handler return signed-zero motion", f -> { f.enter(); f.proof.ownerPacketExit(
            change(f.returned, "body", change(f.returned.body(), "motion", negativeZero)), f.packet); });
        rejects("tracker after receipt signed-zero motion", f -> { f.enter(); f.exit(); f.proof.trackerEnter(
            frame(change(f.returned.body(), "motion", negativeZero), true, 20), f.tracker); });
        rejects("no-packet tracker signed-zero motion", f -> f.proof.trackerEnter(
            frame(change(RESTORED, "motion", negativeZero), true, 20), f.tracker));
    }
    private static void exactPose() {
        for (String component : List.of("yaw", "pitch", "headYaw", "bodyYaw", "stance")) {
            List<?> mutations = component.equals("stance") ? List.of("CROUCHING", "SWIMMING", "")
                : List.of(1.0f, -0.0f, Float.NaN, Float.POSITIVE_INFINITY);
            for (Object mutation : mutations) {
                Pose changedPose = change(POSE, component, mutation);
                rejects("owner handler entry pose " + component + "=" + mutation, f -> f.proof.ownerPacketEnter(
                    change(f.entry, "pose", changedPose), SENT, PACKET, f.packet));
                rejects("owner handler return pose " + component + "=" + mutation, f -> { f.enter();
                    f.proof.ownerPacketExit(change(f.returned, "pose", changedPose), f.packet); });
                rejects("receipt tracker pose " + component + "=" + mutation, f -> { f.enter(); f.exit();
                    f.proof.trackerEnter(change(frame(f.returned.body(), true, 20), "pose", changedPose), f.tracker); });
                rejects("no-packet tracker pose " + component + "=" + mutation, f ->
                    f.proof.trackerEnter(change(frame(RESTORED, true, 20), "pose", changedPose), f.tracker));
                rejects("send changes pose " + component + "=" + mutation, f -> { f.enter(); f.exit();
                    f.proof.trackerEnter(frame(f.returned.body(), true, 20), f.tracker);
                    f.proof.sendEnter(change(frame(f.returned.body().dispatched(), true, 21), "pose", changedPose),
                        f.tracker, new Object(), 71, 1, MOTION, false); });
                rejects("send return changes pose " + component + "=" + mutation, f -> { f.enter(); f.exit();
                    f.proof.trackerEnter(frame(f.returned.body(), true, 20), f.tracker);
                    f.proof.sendEnter(frame(f.returned.body().dispatched(), true, 21), f.tracker, f.packet, 71, 1, MOTION, false);
                    f.proof.sendExit(change(frame(f.returned.body().dispatched(), true, 22), "pose", changedPose), f.packet); });
                rejects("tracker return changes pose " + component + "=" + mutation, f -> { f.enter(); f.exit();
                    f.proof.trackerEnter(frame(f.returned.body(), true, 20), f.tracker);
                    f.proof.sendEnter(frame(f.returned.body().dispatched(), true, 21), f.tracker, f.packet, 71, 1, MOTION, false);
                    f.proof.sendExit(frame(f.returned.body().dispatched(), true, 22), f.packet);
                    f.proof.trackerExit(change(frame(f.returned.body().dispatched(), true, 23), "pose", changedPose), f.tracker); });
            }
        }
        rejects("missing handler pose", f -> f.proof.ownerPacketEnter(change(f.entry, "pose", null), SENT, PACKET, f.packet));
        rejects("missing saved owner pose", f -> f.proof.ownerPacketEnter(f.entry,
            change(SENT, "body", change(OWNER, "pose", null)), PACKET, f.packet));
        // A real physics/listener result can have any finite pose. Retain it unchanged rather than normalizing it.
        Pose retained = new Pose(-0.0f, 14, -37, 82, "CROUCHING");
        Fixture f = new Fixture();
        f.proof.listenerEnter(frame(RELEASE, false, 11)); f.proof.callSiteEnter();
        f.proof.stepEnter(frame(RELEASE, false, 12));
        f.proof.stepExit(change(frame(STEP, false, 13), "pose", retained)); f.proof.callSiteExit();
        f.proof.listenerExit(change(frame(RESTORED, false, 14), "pose", retained), false);
        f.proof.ownerPacketEnter(change(f.entry, "pose", retained), SENT, PACKET, f.packet);
        f.proof.ownerPacketExit(change(f.returned, "pose", retained), f.packet);
        f.proof.trackerEnter(change(frame(f.returned.body(), true, 20), "pose", retained), f.tracker);
        Body sent = f.returned.body().dispatched(); Object motion = new Object();
        f.proof.sendEnter(change(frame(sent, true, 21), "pose", retained), f.tracker, motion, 71, 1, MOTION, false);
        f.proof.sendExit(change(frame(sent, true, 22), "pose", retained), motion);
        f.proof.trackerExit(change(frame(sent, true, 23), "pose", retained), f.tracker);
        Evidence evidence = f.proof.evidence();
        check(evidence.listenerExit().pose() == retained && evidence.ownerReceipt().handlerEntry().pose() == retained
            && evidence.ownerReceipt().handlerReturn().pose() == retained && evidence.trackerEntry().pose() == retained,
            "Immutable native pose preserved exactly, including nonzero angles, negative zero and Pose enum value");
    }

    private static void coherentPoseRestorationFailures() {
        for (String component : List.of("yaw", "pitch", "headYaw", "bodyYaw", "stance")) {
            List<?> mutations = component.equals("stance") ? List.of("CROUCHING") : List.of(1.0f, -0.0f);
            for (Object mutation : mutations) {
                Pose altered = change(POSE, component, mutation);
                Fixture f = new Fixture();
                f.proof.listenerEnter(frame(RELEASE, false, 11)); f.proof.callSiteEnter();
                f.proof.stepEnter(frame(RELEASE, false, 12)); f.proof.stepExit(frame(STEP, false, 13)); f.proof.callSiteExit();
                f.proof.listenerExit(change(frame(RESTORED, false, 14), "pose", altered), false);
                // Keep every downstream pose coherent: only the raw-step-to-listener boundary can reject this edit.
                f.proof.ownerPacketEnter(change(f.entry, "pose", altered), SENT, PACKET, f.packet);
                f.proof.ownerPacketExit(change(f.returned, "pose", altered), f.packet);
                f.proof.trackerEnter(change(frame(f.returned.body(), true, 20), "pose", altered), f.tracker);
                Body sent = f.returned.body().dispatched(); Object motion = new Object();
                f.proof.sendEnter(change(frame(sent, true, 21), "pose", altered), f.tracker, motion, 71, 1, MOTION, false);
                f.proof.sendExit(change(frame(sent, true, 22), "pose", altered), motion);
                f.proof.trackerExit(change(frame(sent, true, 23), "pose", altered), f.tracker);
                Throwable cause = failure(f.proof::healthy).getCause();
                check(cause != null && cause.getMessage().equals("Only native listener position restoration follows doTick"),
                    "Coherent downstream " + component + "=" + mutation + " cannot replace raw-step pose");
                check(failure(f.proof::evidence).getCause() == cause, "Raw-step pose failure stays latched");
                rejected++;
            }
        }
    }

    private static void exactSend() {
        for (var pair : List.of(new Object[]{"ordinal", 0}, new Object[]{"sentIndex", -1}, new Object[]{"sentIndex", 3},
            new Object[]{"completedIndex", -1}, new Object[]{"completedIndex", 2}, new Object[]{"completedIndex", 10},
            new Object[]{"completedIndex", 11}, new Object[]{"ownerEntity", 72}, new Object[]{"ownerUuid", "other"},
            new Object[]{"sameSenderAndConnection", false}, new Object[]{"body", null}, new Object[]{"packet", null})) {
            rejects("send " + pair[0] + "=" + pair[1], f -> f.proof.ownerPacketEnter(f.entry, change(SENT, (String) pair[0], pair[1]), PACKET, f.packet));
        }
        for (var pair : List.of(new Object[]{"position", new Vector(POSITION.x(), Math.nextUp(POSITION.y()), POSITION.z())},
            new Object[]{"grounded", false}, new Object[]{"neutral", false}, new Object[]{"horizontalCollision", true},
            new Object[]{"entity", 72}, new Object[]{"tick", 101L}, new Object[]{"fall", Double.NaN}, new Object[]{"health", Float.NaN},
            new Object[]{"motion", new Vector(Double.NaN, 0, 0)})) {
            rejects("sender snapshot " + pair[0], f -> f.proof.ownerPacketEnter(f.entry, change(SENT, "body", change(OWNER, (String) pair[0], pair[1])), PACKET, f.packet));
        }
        for (var pair : List.of(new Object[]{"type", "PosRot"}, new Object[]{"position", new Vector(POSITION.x(), POSITION.y(), Math.nextUp(POSITION.z()))},
            new Object[]{"rotation", true}, new Object[]{"yaw", 1f}, new Object[]{"yaw", -0.0f}, new Object[]{"pitch", 1f}, new Object[]{"pitch", -0.0f},
            new Object[]{"grounded", false}, new Object[]{"collision", true}, new Object[]{"sha256", "bad"})) {
            OwnerPacket altered = change(PACKET, (String) pair[0], pair[1]);
            rejects("changed received key " + pair[0], f -> f.proof.ownerPacketEnter(f.entry, SENT, altered, f.packet));
            rejects("matching but prohibited key " + pair[0], f -> f.proof.ownerPacketEnter(f.entry, change(SENT, "packet", altered), altered, f.packet));
        }
        rejects("different well-formed digest", f -> f.proof.ownerPacketEnter(f.entry, SENT, change(PACKET, "sha256", "2".repeat(64)), f.packet));
        rejects("no FIFO match", f -> f.proof.ownerPacketEnter(f.entry, null, PACKET, f.packet));
        rejects("null incoming key", f -> f.proof.ownerPacketEnter(f.entry, SENT, null, f.packet));
        rejects("null native packet", f -> f.proof.ownerPacketEnter(f.entry, SENT, PACKET, null));
    }
    private static void exactOrder() {
        rejects("second packet while handling", f -> { f.enter(); f.enter(); });
        rejects("second packet after return", f -> { f.enter(); f.exit(); f.enter(); });
        rejects("return without handler", Fixture::exit);
        rejects("duplicate handler return", f -> { f.enter(); f.exit(); f.exit(); });
        rejects("different native packet returned", f -> { f.enter(); f.proof.ownerPacketExit(f.returned, new Object()); });
        rejects("tracker before handler return", f -> { f.enter(); f.finish(); });
        rejects("extra doTick inside handler", f -> { f.enter(); f.proof.stepEnter(f.entry); });
        rejects("extra doTick after handler", f -> { f.enter(); f.exit(); f.proof.stepEnter(f.returned); });
        rejects("wrong packet followed by exact packet", f -> { f.proof.ownerPacketEnter(f.entry, SENT, change(PACKET, "collision", true), f.packet); f.enter(); });
        for (boolean error : List.of(false, true)) rejects("native handler exception " + error, f -> {
            f.enter(); AtomicInteger calls = new AtomicInteger();
            Throwable nativeFailure = error ? new AssertionError("native owner handler") : new IllegalStateException("native owner handler");
            check(failure(() -> f.proof.original(() -> { calls.incrementAndGet();
                if (nativeFailure instanceof Error value) throw value; throw (RuntimeException) nativeFailure;
            })) == nativeFailure && calls.get() == 1, "Native exception identity and exactly one invocation");
            f.exit();
        });
        Fixture early = new Fixture(); early.enter(); check(failure(early.proof::healthy).getCause() != null, "Packet before READY rejected"); rejected++;
    }
    public static void main(String[] args) {
        Fixture f = new Fixture(); f.ready(); f.enter(); f.exit(); f.finish();
        Evidence evidence = f.proof.evidence(); OwnerReceipt receipt = evidence.ownerReceipt();
        check(receipt.originalSend() == SENT && receipt.received() == PACKET && receipt.handlerEntry() == f.entry && receipt.handlerReturn() == f.returned,
            "Immutable exact sender, decoded packet, handler entry and successful return retained");
        check(!receipt.handlerEntry().body().grounded() && receipt.handlerReturn().body().grounded(), "Only proven packet ground bit reconciles");
        check(receipt.handlerReturn().body().motion().equals(STEP.motion()), "Native post-step motion stays exact");
        check(evidence.trackerEntry().body().equals(receipt.handlerReturn().body()), "Whole handler return reaches original tracker");
        exactBodies(); signedZeroBodies(); exactPose(); coherentPoseRestorationFailures(); exactSend(); exactOrder();
        System.out.println("Stone Hinge owner receipt: " + checks + " checks passed; " + rejected
            + " rejected scenarios (pure-JDK sequencing only; no Minecraft gameplay claim)");
    }
}
