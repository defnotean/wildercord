package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/** Passive native receipt for the original ArtKit.Hits call, excluding nested ordinary SPARK damage. */
public final class CounterHitCapture {
	private CounterHitCapture() {}
	private record Hit(Object action, ServerPlayer owner, AuraApi.StringArt art, LivingEntity target) {}
	private static Hit active;
	private static Throwable failure;

	/** The GameTest wrapper executes the unchanged invocation exactly once and restores nested state even on failure. */
	public static float observe(Object action, ServerPlayer owner, AuraApi.StringArt art, LivingEntity target, Supplier<Float> original) {
		Hit previous = active;
		try { active = new Hit(action, owner, art, target); }
		catch (Throwable observerFailure) { fail(observerFailure); active = null; }
		try { return original.get(); }
		finally { active = previous; }
	}

	public static boolean direct(ServerPlayer owner, String art, LivingEntity target) {
		try {
			return active != null && active.action() != null && active.owner() == owner && active.target() == target
				&& active.art() != null && active.art().id().equals(art);
		} catch (Throwable observerFailure) { fail(observerFailure); return false; }
	}

	/** Retain the native Hits object as the action identity across its primary and delayed calls. */
	public static Object action(ServerPlayer owner, String art, LivingEntity target, Object expected) {
		try {
			if (!direct(owner, art, target)) return null;
			Object actual = active.action();
			if (expected != null && expected != actual) fail(new AssertionError("A different native Hits action reused a counter receipt"));
			return actual;
		} catch (Throwable observerFailure) { fail(observerFailure); return null; }
	}

	private static void fail(Throwable observerFailure) { if (failure == null) failure = observerFailure; }

	public static void assertIdle() {
		if (failure != null) throw new AssertionError("Native counter hit observer failed without replacing its invocation", failure);
		if (active != null) throw new AssertionError("A native counter hit receipt escaped its original invocation");
	}
}
