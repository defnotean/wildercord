package dev.wildercord.aura;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Opt-in synchronous consequence boundary for one released hit, separate from an earned guard's authority. */
public final class ArtHitScope {
	private ArtHitScope() {}
	public interface Boundary {
		boolean valid();
		boolean permits(LivingEntity target);
		boolean afterDamage(LivingEntity target);
		boolean linkedAlive();
		boolean linkedAdmits(Entity target);
	}
	private record Active(ServerPlayer player, Boundary boundary, Active previous) {}
	private static Active active;

	/** Ordinary hits keep exactly their existing earned-counter boundary, or none. */
	public static Boundary boundary(ServerPlayer player) {
		Boundary released = released(player);
		return released != null ? released : MastersArts.earnedCounter(player);
	}

	/** Only the separately released hit, excluding ordinary/live earned-counter authority. */
	public static Boundary released(ServerPlayer player) {
		for (Active scope = active; scope != null; scope = scope.previous()) if (scope.player() == player) return scope.boundary();
		return null;
	}

	/** A nested allied utility must not hide the original caster's still-active consequence scope. */
	public static boolean contains(ServerPlayer player, Boundary boundary) {
		for (Active scope = active; scope != null; scope = scope.previous())
			if (scope.player() == player && scope.boundary() == boundary) return true;
		return false;
	}

	/** Default-true guards for helpers which invoke addon hooks before committing a resource mutation. */
	public static boolean releasedValid(ServerPlayer player) {
		Boundary boundary = released(player);
		return boundary == null || boundary.valid();
	}
	public static boolean releasedAfter(ServerPlayer player, LivingEntity target) {
		Boundary boundary = released(player);
		return boundary == null || boundary.afterDamage(target);
	}

	/** Nested damage and casts see this same hit boundary; exceptions never leak it into another action. */
	public static void within(ServerPlayer player, Boundary boundary, Runnable hit) {
		Active previous = active;
		active = new Active(player, boundary, previous);
		try { if (boundary.valid()) hit.run(); }
		finally { active = previous; }
	}
}
