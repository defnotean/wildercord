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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** One lateral step and one locked reply. No target polling alters its accepted geometry. */
final class GaleReprise {
	private final SwordMaster master;
	private final ServerLevel level;
	private final UUID targetId;
	private final long began;
	private final Vec3 start, travel, aim;
	private Vec3 replyOrigin;
	private double warnedReach;
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private GaleReprise(SwordMaster master, ServerPlayer target, Vec3 travel, long now) {
		this.master = master;
		level = (ServerLevel) master.level();
		targetId = target.getUUID();
		began = now;
		start = master.position();
		this.travel = travel;
		// Aim at the original point from the accepted landing, before either warning or movement.
		aim = target.position().subtract(start.add(travel)).multiply(1, 0, 1).normalize();
	}

	static GaleReprise prepare(SwordMaster master, ServerPlayer target, int discipline, int sequence, double aura, long now, long readyAt) {
		if (!(master.level() instanceof ServerLevel level) || !master.canBeginReprise(now) || !master.canHarmParticipant(target)
			|| !master.hasLineOfSight(target) || !master.onGround() || master.getDeltaMovement().horizontalDistanceSqr() > .0025
			|| !GaleRepriseRules.eligible(discipline, sequence, master.distanceTo(target), target.getY() - master.getY(), aura, now, readyAt)) return null;
		Vec3 toward = target.position().subtract(master.position()).multiply(1, 0, 1).normalize();
		var speed = master.getAttribute(Attributes.MOVEMENT_SPEED);
		double ratio = speed == null || speed.getBaseValue() <= 0 ? 1 : speed.getValue() / speed.getBaseValue();
		double distance = GaleRepriseRules.travel(ratio);
		// Always use the same visible side. An obstruction declines this form, never switches sides mid-tell.
		Vec3 travel = new Vec3(-toward.z, 0, toward.x).scale(distance);
		if (distance < .1 || !MasterPursuit.safePath(master, level, travel)) return null;
		return new GaleReprise(master, target, travel, now);
	}

	boolean tick(long now) {
		if (finished) return false;
		ServerPlayer target = level.getPlayerByUUID(targetId) instanceof ServerPlayer player ? player : null;
		long age = now - began;
		if (!master.canMaintainReprise() || target == null || !master.canHarmParticipant(target)
			|| GaleRepriseRules.beat(age) == GaleRepriseRules.Beat.EXPIRED
			|| lastTick == Long.MIN_VALUE && now != began || lastTick != Long.MIN_VALUE && now > lastTick + 1) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		if (master.getDeltaMovement().horizontalDistanceSqr() > .0025) return cancel();
		master.getNavigation().stop();
		master.getMoveControl().setWait();
		master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		face();
		var beat = GaleRepriseRules.beat(age);
		if (beat == GaleRepriseRules.Beat.STEP) {
			Vec3 expected = start.add(travel.scale(GaleRepriseRules.travelFraction(age - 1)));
			if (master.position().distanceToSqr(expected) > .04 || !master.onGround()) return cancel();
			Vec3 step = travel.scale(1.0 / GaleRepriseRules.STEP_TICKS);
			if (!MasterPursuit.safePath(master, level, step)) return cancel();
			master.move(MoverType.SELF, step);
			if (master.position().distanceToSqr(expected.add(step)) > .0025) return cancel();
			if (age == GaleRepriseRules.GATHER) Feels.sound(level, master.position(), "aura_step", 1, 1.25F);
		} else if (beat == GaleRepriseRules.Beat.GATHER && master.position().distanceToSqr(start) > .04) return cancel();
		if (age == GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS) {
			if (!master.onGround() || master.position().distanceToSqr(start.add(travel)) > .04) return cancel();
			replyOrigin = master.position();
			warnedReach = clippedReach(replyOrigin);
			Feels.sound(level, replyOrigin, "duelist_knight_windup", 1, 1.5F);
			AuraFx.banner(master, Component.translatable("boss.wildercord.master.crosswind_reply"),
				Component.translatable("message.wildercord.master.crosswind_hint"), master.auraColor(), AuraFxRules.BannerKind.ART);
		}
		if (replyOrigin != null && master.position().distanceToSqr(replyOrigin) > .04) return cancel();
		if (beat == GaleRepriseRules.Beat.REPLY) {
			released = true;
			finished = true; // Consume the one strike before guard/parry callbacks can re-enter AI.
			if (replyOrigin == null) return false;
			master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			AuraFx.trail(master, AuraFxRules.Stroke.THRUST, false, master.auraColor(), master.stage(), 1);
			Feels.sound(level, replyOrigin, "aura_slash", 1, 1.2F);
			Vec3 delta = target.position().subtract(replyOrigin), side = new Vec3(-aim.z, 0, aim.x);
			if (GaleRepriseRules.hits(delta.dot(aim), delta.dot(side), delta.y) && delta.dot(aim) <= warnedReach
				&& master.hasLineOfSight(target) && EmberAfterburn.clear(level, master, replyOrigin.add(0, .9, 0), target.getBoundingBox().getCenter())) {
				Effects.withSource(master, () -> master.projected(target, GaleRepriseRules.DAMAGE));
			}
			return false;
		}
		if (age % GaleRepriseRules.WARNING_REFRESH == 0 || age == GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS) warn(age);
		return true;
	}

	boolean released() { return released; }
	boolean replyWarned() { return replyOrigin != null; }

	private boolean cancel() {
		finished = true;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		return false;
	}

	private void face() {
		float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
		master.setYRot(yaw); master.setYHeadRot(yaw); master.setYBodyRot(yaw);
		master.getLookControl().setLookAt(master.getX() + aim.x * 8, master.getEyeY(), master.getZ() + aim.z * 8);
	}

	private void warn(long age) {
		int life = Math.min(GaleRepriseRules.WARNING_REFRESH + 1, (int) (GaleRepriseRules.TELL - age));
		if (replyOrigin == null) {
			Vec3 edge = new Vec3(-travel.z, 0, travel.x).normalize().scale(.45);
			for (int sign : new int[] {-1, 1}) {
				Vec3 from = start.add(edge.scale(sign)).add(0, .12, 0);
				Light.ray(level, from, from.add(travel), master.auraColor(), .055, life);
			}
		} else {
			Vec3 side = new Vec3(-aim.z, 0, aim.x).scale(GaleRepriseRules.HALF_WIDTH);
			Vec3 from = replyOrigin.add(0, .12, 0), end = from.add(aim.scale(warnedReach));
			for (int sign : new int[] {-1, 1}) Light.ray(level, from.add(side.scale(sign)), end.add(side.scale(sign)), master.auraColor(), .07, life);
			Light.ray(level, end.subtract(side), end.add(side), master.auraColor(), .07, life);
		}
	}

	/** Cover at the warning clips the accepted lane; removing it cannot reveal unmarked reach. */
	private double clippedReach(Vec3 origin) {
		double reach = GaleRepriseRules.REACH;
		Vec3 side = new Vec3(-aim.z, 0, aim.x);
		for (double offset : new double[] {-GaleRepriseRules.HALF_WIDTH, 0, GaleRepriseRules.HALF_WIDTH}) {
			Vec3 start = origin.add(side.scale(offset)).add(0, .9, 0), end = start.add(aim.scale(reach));
			var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, master));
			if (hit.getType() != HitResult.Type.MISS) reach = Math.min(reach, start.distanceTo(hit.getLocation()));
		}
		return reach;
	}
}
