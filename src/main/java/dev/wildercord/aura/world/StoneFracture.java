package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A stationary, fixed-facing brace and one separately warned reply, owned by its live trial. */
final class StoneFracture {
	private final SwordMaster master;
	private final ServerLevel level;
	private final UUID targetId;
	private final long began;
	private final Vec3 origin, aim;
	private long lastTick = Long.MIN_VALUE;
	private double warnedReach;
	private boolean warned, finished, released;

	private StoneFracture(SwordMaster master, ServerPlayer target, long now) {
		this.master = master;
		level = (ServerLevel) master.level();
		targetId = target.getUUID();
		began = now;
		origin = master.position();
		aim = target.position().subtract(origin).multiply(1, 0, 1).normalize();
	}

	static StoneFracture prepare(SwordMaster master, ServerPlayer target, int discipline, int sequence, double aura, long now, long readyAt) {
		if (!(master.level() instanceof ServerLevel) || !master.canBeginFracture(now) || !master.canHarmParticipant(target)
			|| !master.hasLineOfSight(target) || !master.onGround() || master.getDeltaMovement().horizontalDistanceSqr() > .0025
			|| !StoneFractureRules.eligible(discipline, sequence, master.distanceTo(target), target.getY() - master.getY(), aura, now, readyAt)) return null;
		return new StoneFracture(master, target, now);
	}

	boolean tick(long now) {
		if (finished) return false;
		ServerPlayer target = level.getPlayerByUUID(targetId) instanceof ServerPlayer player ? player : null;
		long age = now - began;
		if (!master.canMaintainFracture() || target == null || !master.canHarmParticipant(target)
			|| StoneFractureRules.beat(age) == StoneFractureRules.Beat.EXPIRED
			|| lastTick == Long.MIN_VALUE && now != began || lastTick != Long.MIN_VALUE && now > lastTick + 1
			|| !master.onGround() || master.position().distanceToSqr(origin) > .04
			|| master.getDeltaMovement().horizontalDistanceSqr() > .0025) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		master.getNavigation().stop();
		master.getMoveControl().setWait();
		master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		face();
		var beat = StoneFractureRules.beat(age);
		if (age == StoneFractureRules.PLANT && !master.raiseGuard()) return cancel();
		if (beat == StoneFractureRules.Beat.BRACE && !master.guarding()) return cancel();
		if (age == StoneFractureRules.PLANT + StoneFractureRules.BRACE) {
			master.dropGuard();
			warned = true;
			warnedReach = clippedReach();
			Feels.sound(level, origin, "duelist_knight_windup", 1, .65F);
			AuraFx.banner(master, Component.translatable("boss.wildercord.master.fracture_reply"),
				Component.translatable("message.wildercord.master.fracture_hint"), master.auraColor(), AuraFxRules.BannerKind.ART);
		}
		if (beat == StoneFractureRules.Beat.REPLY) {
			finished = released = true; // Consume first: parry callbacks must not replay a strike.
			if (!warned) return false;
			master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			AuraFx.trail(master, AuraFxRules.Stroke.THRUST, false, master.auraColor(), master.stage(), 1.1F);
			Feels.sound(level, origin, "aura_slash", 1, .75F);
			Vec3 delta = target.position().subtract(origin), side = new Vec3(-aim.z, 0, aim.x);
			if (StoneFractureRules.hits(delta.dot(aim), delta.dot(side), delta.y) && delta.dot(aim) <= warnedReach
				&& master.hasLineOfSight(target) && EmberAfterburn.clear(level, master, origin.add(0, .9, 0), target.getBoundingBox().getCenter())) {
				Effects.withSource(master, () -> master.projected(target,
					MastersRules.damage(master.challengerCount(), MastersRules.STONE, MastersRules.Move.STONE_FRACTURE)));
			}
			return false;
		}
		if (age % StoneFractureRules.WARNING_REFRESH == 0 || age == StoneFractureRules.PLANT + StoneFractureRules.BRACE) warn(age);
		return true;
	}

	boolean released() { return released; }
	boolean replyWarned() { return warned; }
	boolean bracing(long now) { return !finished && StoneFractureRules.beat(now - began) == StoneFractureRules.Beat.BRACE; }

	/** Rear punishment belongs to the brace, never to the final committed reply. */
	boolean rearHit(DamageSource source, long now) {
		Vec3 from = source instanceof dev.wildercord.cast.RelayDamageSource ? source.getSourcePosition() : source.getDirectEntity() != null ? source.getDirectEntity().position() : source.getSourcePosition();
		return bracing(now) && from != null && StoneFractureRules.rear(from.subtract(origin).dot(aim));
	}

	private boolean cancel() { finished = true; return false; }

	private void face() {
		float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(origin.x + aim.x * 8, master.getEyeY(), origin.z + aim.z * 8);
	}

	private void warn(long age) {
		int life = Math.min(StoneFractureRules.WARNING_REFRESH + 1, (int) (StoneFractureRules.TELL - age));
		Vec3 side = new Vec3(-aim.z, 0, aim.x), feet = origin.add(0, .12, 0);
		if (!warned) {
			// A short front crossbar announces the brace; it does not claim the later attack lane.
			Vec3 front = feet.add(aim.scale(.8));
			Light.ray(level, front.subtract(side.scale(.7)), front.add(side.scale(.7)), master.auraColor(), .08, life);
		} else {
			Vec3 edge = side.scale(StoneFractureRules.HALF_WIDTH), end = feet.add(aim.scale(warnedReach));
			for (int sign : new int[] {-1, 1}) Light.ray(level, feet.add(edge.scale(sign)), end.add(edge.scale(sign)), master.auraColor(), .07, life);
			Light.ray(level, end.subtract(edge), end.add(edge), master.auraColor(), .07, life);
		}
	}

	/** Cover clips the committed warning, so removing it can never reveal unmarked damage. */
	private double clippedReach() {
		double reach = StoneFractureRules.REACH;
		Vec3 side = new Vec3(-aim.z, 0, aim.x);
		for (double offset : new double[] {-StoneFractureRules.HALF_WIDTH, 0, StoneFractureRules.HALF_WIDTH}) {
			Vec3 start = origin.add(side.scale(offset)).add(0, .9, 0), end = start.add(aim.scale(reach));
			var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, master));
			if (hit.getType() != HitResult.Type.MISS) reach = Math.min(reach, start.distanceTo(hit.getLocation()));
		}
		return reach;
	}
}
