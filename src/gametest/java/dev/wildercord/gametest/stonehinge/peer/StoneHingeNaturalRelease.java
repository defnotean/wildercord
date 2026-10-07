package dev.wildercord.gametest.stonehinge.peer;

import dev.wildercord.gametest.stonehinge.StoneHingeVelocityExperiment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A single fixture callback around a naturally released Master attack; inert unless explicitly armed by this diagnostic. */
public final class StoneHingeNaturalRelease {
    private StoneHingeNaturalRelease() {}
    private record Armed(ServerPlayer target, LivingEntity source, Consumer<StoneHingeVelocityExperiment.Strike> observer) {}
    private static Armed armed;
    private static Throwable observationFailure;
    public static void arm(ServerPlayer target, LivingEntity source, Consumer<StoneHingeVelocityExperiment.Strike> observer) {
        if (armed != null || !target.level().getServer().isSameThread()) throw new IllegalStateException("Single server-thread natural release arm");
        observationFailure = null; armed = new Armed(target, source, observer);
    }
    public static float observe(LivingEntity source, LivingEntity target, Supplier<Float> original) {
        Armed a = armed;
        if (a == null || a.target != target || a.source != source) return original.get();
        armed = null;
        float[] returned = new float[1];
        var strike = StoneHingeVelocityExperiment.capture(a.target, () -> returned[0] = original.get());
        try { a.observer.accept(strike); } catch (Throwable failure) { observationFailure = failure; }
        return returned[0];
    }
    public static void assertHealthy() { if (observationFailure != null) throw new AssertionError("Natural release observer failed", observationFailure); }
    public static void clear() { armed = null; observationFailure = null; }
}
