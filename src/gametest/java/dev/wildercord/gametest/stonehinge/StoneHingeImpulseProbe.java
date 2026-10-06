package dev.wildercord.gametest.stonehinge;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.cast.Targets;
import dev.wildercord.cast.VoidTime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Test-only observation. Never cancels damage, knockback, movement, or the existing Master-form cancellation. */
public final class StoneHingeImpulseProbe {
	private StoneHingeImpulseProbe() {}
	private static Trial active;
	private static final ThreadLocal<Hit> CURRENT = new ThreadLocal<>();
	private static final ThreadLocal<Melee> MELEE = new ThreadLocal<>();
	private static final ThreadLocal<Hit> DEFAULT_ROUTE = new ThreadLocal<>();
	private static final ThreadLocal<DefaultCall> DEFAULT_CALL = new ThreadLocal<>();
	private static final ThreadLocal<Hit> NATIVE_WRITE = new ThreadLocal<>();

	private static final class DefaultCall {
		final Hit hit;
		boolean entered;
		DefaultCall(Hit hit) { this.hit = hit; }
	}

	private static Hit matching(Hit hit, LivingEntity target, DamageSource source) {
		return hit != null && hit == CURRENT.get() && hit.trial == active && hit.trial.player == target && hit.source == source ? hit : null;
	}

	/** Only LivingEntity.hurtServer's actual default-knockback invocation can establish this route. */
	public static void defaultRoute(LivingEntity target, DamageSource source, Runnable action) {
		if (active == null) { action.run(); return; }
		Hit previous = DEFAULT_ROUTE.get(); DEFAULT_ROUTE.set(matching(CURRENT.get(), target, source));
		try { action.run(); }
		finally { if (previous == null) DEFAULT_ROUTE.remove(); else DEFAULT_ROUTE.set(previous); }
	}

	/** Only dealDefaultKnockback's exact native knockback call can arm a write, and only once. */
	public static void defaultCall(LivingEntity target, DamageSource source, Runnable action) {
		if (active == null) { action.run(); return; }
		DefaultCall previous = DEFAULT_CALL.get(); DEFAULT_CALL.set(new DefaultCall(matching(DEFAULT_ROUTE.get(), target, source)));
		try { action.run(); }
		finally { if (previous == null) DEFAULT_CALL.remove(); else DEFAULT_CALL.set(previous); }
	}

	/** Every six-argument invocation gets a scope; nested/callback invocations cannot inherit a default write. */
	public static void knockbackInvocation(LivingEntity target, DamageSource source, Runnable action) {
		if (active == null) { action.run(); return; }
		DefaultCall call = DEFAULT_CALL.get(); Hit hit = null;
		if (call != null && !call.entered) { call.entered = true; hit = matching(call.hit, target, source); }
		Hit previous = NATIVE_WRITE.get(); NATIVE_WRITE.set(hit);
		try { action.run(); }
		finally { if (previous == null) NATIVE_WRITE.remove(); else NATIVE_WRITE.set(previous); }
	}

	private static final class Melee {
		final LivingEntity attacker, target;
		DamageSource source;
		Melee(LivingEntity attacker, LivingEntity target) { this.attacker = attacker; this.target = target; }
	}

	/** This scope is installed only at the exact native Master SWEEP/THRUST release invocation by a test mixin. */
	public static float masterMelee(LivingEntity attacker, LivingEntity target, Supplier<Float> action) {
		if (active == null || active.player != target) return action.get();
		Melee previous = MELEE.get();
		MELEE.set(new Melee(attacker, target));
		try { return action.get(); }
		finally { if (previous == null) MELEE.remove(); else MELEE.set(previous); }
	}

	/** Exact vanilla Mob.doHurtTarget callsite. A handcrafted mob-attack DamageSource alone grants no provenance. */
	public static boolean nativeMelee(LivingEntity attacker, LivingEntity target, DamageSource source, Supplier<Boolean> action) {
		if (active == null || active.player != target) return action.get();
		Melee previous = MELEE.get(), scope = new Melee(attacker, target); scope.source = source; MELEE.set(scope);
		try { return action.get(); }
		finally { if (previous == null) MELEE.remove(); else MELEE.set(previous); }
	}

	/** Bind before AuraElements can cause nested hits. Proximity, owner identity and Aura damage type are insufficient. */
	public static void createdSource(LivingEntity attacker, LivingEntity target, DamageSource source) {
		Melee melee = MELEE.get();
		if (melee != null && melee.attacker == attacker && melee.target == target && melee.source == null) melee.source = source;
	}

	public static final class Trial {
		private final ServerPlayer player;
		private final List<Hit> hits = new ArrayList<>();
		private final List<Impulse> impulses = new ArrayList<>();
		private Trial(ServerPlayer player) { this.player = player; }
		public List<Hit> hits() { return List.copyOf(hits); }
		public List<Impulse> impulses() { return List.copyOf(impulses); }
		public boolean completeHit() { return !hits.isEmpty() && hits.stream().allMatch(hit -> hit.complete); }
		public Hit onlyHit() {
			if (hits.size() != 1) throw new AssertionError("Expected one actual native hurt invocation, found " + hits.size());
			return hits.getFirst();
		}
		/** Receipt feasibility only. This is deliberately not permission to erase the original impulse. */
		public boolean eligibleReceipt() {
			if (hits.size() != 1 || impulses.size() != 1) return false;
			Hit hit = hits.getFirst(); Impulse impulse = impulses.getFirst();
			return hit.complete && hit.returned && hit.observed && !hit.contaminated && !hit.lethal
				&& hit.healthLost + hit.absorptionLost > 0 && hit.melee && hit.frontal && hit.hostile
				&& Float.isFinite(hit.healthLost) && Float.isFinite(hit.absorptionLost)
				&& finite(impulse.before) && finite(impulse.after)
				&& impulse.hit == hit && impulse.source == hit.source && impulse.horizontalMagnitude() > 1.0E-8
				&& player.isAlive() && !player.isDeadOrDying() && !VoidTime.anchored(player)
				&& player.level() == hit.level && player.position().equals(hit.position)
				&& player.getDeltaMovement().equals(impulse.after)
				&& player.connection != null && player.connection.player == player
				&& player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player
				&& !player.isCreative() && !player.isSpectator();
		}
		public String summary() {
			return "hits=" + hits.stream().map(Hit::summary).toList() + ", impulses=" + impulses;
		}
	}

	/** Assert before clearing, so teardown cannot turn an unbalanced proof into a pass. */
	public static void assertIdle() {
		if (active != null || CURRENT.get() != null || MELEE.get() != null || DEFAULT_ROUTE.get() != null
			|| DEFAULT_CALL.get() != null || NATIVE_WRITE.get() != null) throw new IllegalStateException("Leaked Stone Hinge proof scope");
	}
	/** Server-thread teardown only; keep instrumentation inert after a failed diagnostic. */
	public static void clear() {
		active = null;
		CURRENT.remove(); MELEE.remove(); DEFAULT_ROUTE.remove(); DEFAULT_CALL.remove(); NATIVE_WRITE.remove();
	}

	public static Trial start(ServerPlayer player) {
		if (active != null || CURRENT.get() != null || MELEE.get() != null || DEFAULT_ROUTE.get() != null || DEFAULT_CALL.get() != null || NATIVE_WRITE.get() != null) throw new IllegalStateException("Overlapping Stone Hinge proof trials");
		active = new Trial(player); return active;
	}
	public static void stop(Trial trial) {
		if (active != trial || CURRENT.get() != null || MELEE.get() != null || DEFAULT_ROUTE.get() != null || DEFAULT_CALL.get() != null || NATIVE_WRITE.get() != null) throw new IllegalStateException("Unbalanced native hit scope");
		active = null;
	}
	public static Trial capture(ServerPlayer player, Runnable action) {
		Trial trial = start(player);
		try { action.run(); return trial; }
		finally { stop(trial); }
	}

	public static final class Hit {
		final Trial trial;
		final Hit previous;
		final DamageSource source;
		final net.minecraft.server.level.ServerLevel level;
		final Vec3 position;
		final boolean melee, frontal, hostile;
		boolean returned, complete, observed, observing, contaminated, lethal;
		float healthLost, absorptionLost;
		private Hit(Trial trial, DamageSource source, Hit previous) {
			this.trial = trial; this.source = source; this.previous = previous;
			ServerPlayer player = trial.player; level = player.level(); position = player.position();
			LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
			Melee scope = MELEE.get();
			boolean exactMelee = scope != null && scope.attacker == attacker && scope.target == player && scope.source == source;
			melee = exactMelee && attacker != null && source.getDirectEntity() == attacker
				&& !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION)
				&& (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK) || source.is(Aura.DAMAGE));
			frontal = attacker != null && attacker.position().subtract(player.position()).horizontalDistanceSqr() > 1.0E-4
				&& AuraGuard.facing(player, attacker.position());
			hostile = attacker != null && attacker != player && Targets.canHarm(attacker, player);
			if (previous != null) {
				contaminated = true;
				for (Hit ancestor = previous; ancestor != null; ancestor = ancestor.previous) ancestor.contaminated = true;
			}
		}
		public boolean observed() { return observed; }
		public boolean lethal() { return lethal; }
		public boolean melee() { return melee; }
		public boolean returned() { return returned; }
		public boolean contaminated() { return contaminated; }
		public float healthLost() { return healthLost; }
		public float absorptionLost() { return absorptionLost; }
		public DamageSource source() { return source; }
		public String summary() { return "{source=" + source.getMsgId() + ", returned=" + returned + ", observed=" + observed
			+ ", healthLost=" + healthLost + ", absorptionLost=" + absorptionLost + ", lethal=" + lethal + ", melee=" + melee
			+ ", frontal=" + frontal + ", hostile=" + hostile + ", nested=" + contaminated + "}"; }
	}

	public static Hit begin(Player player, DamageSource source) {
		Trial trial = active;
		if (trial == null || trial.player != player) return null;
		Hit hit = new Hit(trial, source, CURRENT.get()); CURRENT.set(hit); trial.hits.add(hit); return hit;
	}
	public static void finish(Hit hit, boolean returned, boolean success) {
		if (hit == null) return;
		if (CURRENT.get() != hit) throw new IllegalStateException("Native hurt scope association changed");
		hit.returned = returned; hit.complete = success; hit.contaminated |= !success;
		if (hit.previous == null) CURRENT.remove(); else CURRENT.set(hit.previous);
	}
	public record Wound(Hit hit, float health, float absorption) {}
	public static Wound beginWound(Player player, DamageSource source) {
		Hit hit = CURRENT.get();
		if (hit == null || hit.trial.player != player || hit.source != source) return null;
		if (hit.observed || hit.observing) { hit.contaminated = true; return null; }
		hit.observing = true; return new Wound(hit, player.getHealth(), player.getAbsorptionAmount());
	}
	public static void finishWound(Wound wound, boolean success) {
		if (wound == null) return;
		Hit hit = wound.hit; ServerPlayer player = hit.trial.player;
		hit.observing = false; hit.observed = success;
		hit.contaminated |= !success || CURRENT.get() != hit;
		if (!success) return;
		hit.healthLost = Math.max(0, wound.health - Math.max(0, player.getHealth()));
		hit.absorptionLost = Math.max(0, wound.absorption - player.getAbsorptionAmount());
		hit.lethal = player.getHealth() <= 0 || player.isDeadOrDying();
	}

	private static boolean finite(Vec3 vector) { return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z); }

	/** hit is non-null only for that hurt invocation's native default impulse; callback/caller writes stay unassociated. */
	public record Impulse(Hit hit, DamageSource source, Vec3 before, Vec3 after, boolean grounded) {
		/** Native 26.3 halves existing horizontal velocity before adding this impulse. */
		public double horizontalMagnitude() { return Math.hypot(after.x - before.x * .5, after.z - before.z * .5); }
		@Override public String toString() { return "{associated=" + (hit != null) + ", magnitude=" + horizontalMagnitude()
			+ ", before=" + before + ", after=" + after + ", grounded=" + grounded + "}"; }
	}
	/** Called only after the exact native knockback setDeltaMovement operation actually executes. */
	public static void nativeWrite(LivingEntity entity, DamageSource source, Vec3 before, Vec3 after, boolean grounded) {
		Trial trial = active;
		if (trial == null || trial.player != entity) return;
		Hit hit = matching(NATIVE_WRITE.get(), entity, source);
		trial.impulses.add(new Impulse(hit, source, before, after, grounded));
	}
}
