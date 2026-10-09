package dev.wildercord.aura;

import dev.wildercord.cast.Targets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Stone Hinge's exact-hit receipt. Inert unless the target holds a paid catch. It never cancels or changes damage: after the
 * whole native melee resolves it may only turn that hit's one native knockback impulse, keeping Y and the native packet path.
 */
public final class StoneHingeReceipt {
	private StoneHingeReceipt() {}
	private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

	private static final class Scope {
		final LivingEntity attacker;
		final ServerPlayer player;
		final Scope previous;
		final Vec3 position;
		final boolean frontal, hostile;
		DamageSource source, impulseSource;
		Vec3 before, after;
		int hits, impulses;
		boolean returned, completed, observed, nested, grounded, deferred;
		float lost;
		Scope(LivingEntity attacker, ServerPlayer player, Scope previous) {
			this.attacker = attacker; this.player = player; this.previous = previous; position = player.position();
			frontal = MasterForms.hingeFrontal(player, attacker.position());
			hostile = attacker != player && Targets.canHarm(attacker, player);
		}
	}

	/** The whole Mob.doHurtTarget, so a caller-side enchanted impulse after hurtServer counts as a second impulse. */
	public static boolean melee(LivingEntity attacker, Entity target, Supplier<Boolean> action) {
		if (!(target instanceof ServerPlayer player) || !MasterForms.catching(player)) return action.get();
		Scope scope = open(attacker, player);
		boolean success = false;
		try { boolean result = action.get(); success = true; return result; }
		finally { close(scope, success); }
	}
	/** Only Mob.doHurtTarget's own hurtServer callsite binds the native melee source. */
	public static void meleeSource(LivingEntity attacker, Entity target, DamageSource source) {
		Scope scope = CURRENT.get();
		if (scope != null && scope.attacker == attacker && scope.player == target && scope.source == null && source.getDirectEntity() == attacker
			&& (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.PLAYER_ATTACK))) scope.source = source;
	}
	/** A Master SWEEP or THRUST release; its projected source is bound before element reactions can nest other hits. */
	public static float master(LivingEntity attacker, LivingEntity target, Supplier<Float> action) {
		if (!(target instanceof ServerPlayer player) || !MasterForms.catching(player)) return action.get();
		Scope scope = open(attacker, player);
		boolean success = false;
		try { float result = action.get(); success = true; return result; }
		finally { close(scope, success); }
	}
	public static void masterSource(LivingEntity attacker, LivingEntity target, DamageSource source) {
		Scope scope = CURRENT.get();
		if (scope != null && scope.attacker == attacker && scope.player == target && scope.source == null && source.is(Aura.DAMAGE)) scope.source = source;
	}
	private static Scope open(LivingEntity attacker, ServerPlayer player) {
		Scope previous = CURRENT.get(), scope = new Scope(attacker, player, previous);
		CURRENT.set(scope); return scope;
	}
	private static void close(Scope scope, boolean success) {
		if (scope.previous == null) CURRENT.remove(); else CURRENT.set(scope.previous);
		// Any nested native melee taints the whole outer receipt rather than letting either borrow the other's impulse.
		for (Scope outer = scope.previous; outer != null; outer = outer.previous) if (outer.player == scope.player) outer.nested = true;
		ServerPlayer player = scope.player;
		boolean turned = success && receipt(scope) && MasterForms.hinge(player, scope.attacker, scope.before, scope.after);
		if (!turned && scope.deferred) MasterForms.cancel(player);
	}
	private static boolean receipt(Scope s) {
		ServerPlayer player = s.player;
		DamageSource source = s.source;
		return source != null && s.hits == 1 && s.impulses == 1 && s.completed && s.returned && s.observed && !s.nested && s.impulseSource == source
			&& s.lost > 0 && Float.isFinite(s.lost) && s.frontal && s.hostile && s.grounded && player.onGround()
			&& !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION)
			&& player.isAlive() && !player.isDeadOrDying() && !player.isCreative() && !player.isSpectator()
			&& player.position().equals(s.position) && player.getDeltaMovement().equals(s.after);
	}

	/** Player.hurtServer: one completed native hit with the bound source. Any other hurt inside the scope taints it. */
	public static boolean hurt(Player target, DamageSource source, Supplier<Boolean> action) {
		Scope scope = CURRENT.get();
		if (scope == null || scope.player != target) return action.get();
		if (++scope.hits > 1 || source != scope.source) scope.nested = true;
		boolean returned = false, success = false;
		try { returned = action.get(); success = true; return returned; }
		finally {
			if (scope.hits == 1) { scope.returned = returned; scope.completed = success; }
			if (CURRENT.get() != scope) scope.nested = true;
		}
	}
	/** Player.actuallyHurt: health plus absorption actually lost, before healing can conceal the wound. */
	public static void wound(Player target, DamageSource source, Runnable action) {
		Scope scope = CURRENT.get();
		if (scope == null || scope.player != target || source != scope.source) { action.run(); return; }
		if (scope.observed) scope.nested = true;
		float health = target.getHealth(), absorption = target.getAbsorptionAmount();
		action.run();
		scope.observed = true;
		scope.lost = Math.max(0, health - Math.max(0, target.getHealth())) + Math.max(0, absorption - target.getAbsorptionAmount());
	}
	/** Every native knockback write on the catching body; only one, from the bound source, can be turned. */
	public static void impulse(LivingEntity entity, DamageSource source, Vec3 before, Vec3 after, boolean grounded) {
		Scope scope = CURRENT.get();
		if (scope == null || scope.player != entity) return;
		scope.impulses++; scope.impulseSource = source; scope.before = before; scope.after = after; scope.grounded = grounded;
	}
	/** The damage-cancels-form exception is only for this exact first hit inside the paid catch; the verdict comes after. */
	public static boolean defer(ServerPlayer player, DamageSource source) {
		Scope scope = CURRENT.get();
		if (scope == null || scope.player != player || source != scope.source || scope.hits != 1 || !MasterForms.catching(player)) return false;
		scope.deferred = true; return true;
	}
}
