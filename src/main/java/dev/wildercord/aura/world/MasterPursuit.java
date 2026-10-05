package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Statuses;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** One server-owned dash and strike. No scheduler, teleport, target transfer, or retargeting survives this attempt. */
final class MasterPursuit {
	private final SwordMaster master;
	private final ServerLevel level;
	private final UUID targetId;
	private final long chargeStart, began;
	private final Vec3 start, travel, aim;
	private Vec3 strikeOrigin;
	private double warnedReach;
	private final double damage;
	private long lastTick = Long.MIN_VALUE;
	private boolean finished, released;

	private MasterPursuit(SwordMaster master, ServerPlayer target, Vec3 travel, int discipline, long now) {
		this.master = master;
		damage = MastersRules.damage(master.challengerCount(), discipline, MastersRules.Move.PURSUIT_BREAK);
		level = (ServerLevel) master.level();
		targetId = target.getUUID();
		chargeStart = target.getAttached(WildercordAttachments.CHARGE).start();
		began = now;
		start = master.position();
		this.travel = travel;
		aim = travel.normalize();
	}

	/** The attachment is server-authored by Charging; stale, unequipped or future-dated markers are not openings. */
	static boolean liveCharge(ServerPlayer target, long now) {
		var charge = target.getAttached(WildercordAttachments.CHARGE);
		return charge != null && charge.start() <= now && now - charge.start() <= MasterPursuitRules.MAX_CHARGE_AGE
			&& charge.full() > 0 && !charge.runes().isEmpty() && Spellbooks.tier(target) != null && target.isAlive() && !target.isSpectator();
	}

	static MasterPursuit prepare(SwordMaster master, ServerPlayer target, int discipline, double aura, long now, long readyAt) {
		if (!(master.level() instanceof ServerLevel level) || !master.canHarmParticipant(target)
			|| !liveCharge(target, now) || !MasterPursuitRules.observedCharge(now - target.getAttached(WildercordAttachments.CHARGE).start())
			|| !master.hasLineOfSight(target) || !master.onGround()
			|| !MasterPursuitRules.eligible(discipline, master.distanceTo(target), target.getY() - master.getY(), aura, now, readyAt)) return null;
		Vec3 delta = target.position().subtract(master.position()).multiply(1, 0, 1);
		if (delta.lengthSqr() < 1) return null;
		var speed = master.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		double ratio = speed == null || speed.getBaseValue() <= 0 ? 1 : speed.getValue() / speed.getBaseValue();
		double distance = MasterPursuitRules.travel(discipline, delta.length(), ratio);
		Vec3 travel = delta.normalize().scale(distance);
		if (distance < .1 || !safePath(master, level, travel)) return null;
		return new MasterPursuit(master, target, travel, discipline, now);
	}

	/** False means consumed/cancelled. Even repeated ticks can spend the movement and hit budgets only once. */
	boolean tick(long now) {
		if (finished) return false;
		ServerPlayer target = level.getPlayerByUUID(targetId) instanceof ServerPlayer player ? player : null;
		long age = now - began;
		if (!master.canMaintainPursuit() || target == null || !master.canHarmParticipant(target) || !sameCharge(target, now)
			|| MasterPursuitRules.beat(age) == MasterPursuitRules.Beat.EXPIRED
			|| lastTick == Long.MIN_VALUE && now != began || lastTick != Long.MIN_VALUE && now > lastTick + 1) return cancel();
		if (now == lastTick) return true;
		lastTick = now;
		// External knockback is a real interruption, not momentum to silently erase in order to keep chasing.
		if (master.getDeltaMovement().horizontalDistanceSqr() > .0025) return cancel();
		master.getNavigation().stop();
		// PathNavigation runs before custom AI; its already queued MOVE_TO runs afterward unless explicitly cleared.
		master.getMoveControl().setWait();
		master.setSpeed(0);
		master.xxa = 0; master.yya = 0; master.zza = 0;
		master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
		face();
		var beat = MasterPursuitRules.beat(age);
		if (beat == MasterPursuitRules.Beat.DASH) {
			Vec3 expected = start.add(travel.scale(MasterPursuitRules.travelFraction(age - 1)));
			if (master.position().distanceToSqr(expected) > .04 || !master.onGround()) return cancel();
			Vec3 step = travel.scale(1.0 / MasterPursuitRules.DASH_TICKS);
			if (!safePath(master, level, step)) return cancel();
			master.move(MoverType.SELF, step); // Native collision resolution, never setPos/snapTo/teleport.
			if (master.position().distanceToSqr(expected.add(step)) > .0025) return cancel();
			if (age == MasterPursuitRules.WINDUP) Feels.sound(level, master.position(), "aura_step", 1, 1.1F);
		} else if (beat == MasterPursuitRules.Beat.WARNING && master.position().distanceToSqr(start) > .04) return cancel();
		if (age == MasterPursuitRules.WINDUP + MasterPursuitRules.DASH_TICKS) {
			strikeOrigin = master.position();
			// Capture cover with the actual landing. Removing it later cannot expose an unmarked longer strike.
			warnedReach = clippedReach(strikeOrigin);
			Feels.sound(level, strikeOrigin, "duelist_knight_windup", 1, 1.35F);
			AuraFx.banner(master, Component.translatable("boss.wildercord.master.pursuit_strike"),
				Component.translatable("message.wildercord.master.pursuit_hint"), master.auraColor(), AuraFxRules.BannerKind.ART);
		}
		if (strikeOrigin != null && master.position().distanceToSqr(strikeOrigin) > .04) return cancel();
		if (beat == MasterPursuitRules.Beat.STRIKE) {
			released = true;
			finished = true; // Consume before damage/parry callbacks can re-enter AI.
			if (strikeOrigin == null) return false;
			master.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
			AuraFx.trail(master, AuraFxRules.Stroke.THRUST, false, master.auraColor(), master.stage(), 1.1F);
			Feels.sound(level, strikeOrigin, "aura_slash", 1, .95F);
			Vec3 delta = target.position().subtract(strikeOrigin), side = new Vec3(-aim.z, 0, aim.x);
			if (MasterPursuitRules.hits(delta.dot(aim), delta.dot(side), delta.y) && delta.dot(aim) <= warnedReach
				&& master.hasLineOfSight(target) && EmberAfterburn.clear(level, master, strikeOrigin.add(0, .9, 0), target.getBoundingBox().getCenter())) {
				Effects.withSource(master, () -> {
					float dealt = master.projected(target, damage);
					if (dealt > 0 && sameCharge(target, now)) Statuses.interrupt(target);
				});
			}
			return false;
		}
		if (age % MasterPursuitRules.WARNING_REFRESH == 0 || age == MasterPursuitRules.WINDUP + MasterPursuitRules.DASH_TICKS) warn(age);
		return true;
	}

	boolean released() { return released; }
	boolean strikeWarned() { return strikeOrigin != null; }

	private boolean sameCharge(ServerPlayer target, long now) {
		return liveCharge(target, now) && target.getAttached(WildercordAttachments.CHARGE).start() == chargeStart;
	}

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
		int life = Math.min(MasterPursuitRules.WARNING_REFRESH + 1, (int) (MasterPursuitRules.TELL - age));
		Vec3 side = new Vec3(-aim.z, 0, aim.x);
		if (strikeOrigin == null) {
			// Parallel runway edges show the committed body path; these cause no damage.
			for (int sign : new int[] {-1, 1}) {
				Vec3 from = start.add(side.scale(sign * .45)).add(0, .12, 0);
				Light.ray(level, from, from.add(travel), master.auraColor(), .055, life);
			}
		} else {
			Vec3 from = strikeOrigin.add(0, .12, 0), end = from.add(aim.scale(warnedReach));
			for (int sign : new int[] {-1, 1}) {
				Vec3 offset = side.scale(sign * MasterPursuitRules.HALF_WIDTH);
				Light.ray(level, from.add(offset), end.add(offset), master.auraColor(), .07, life);
			}
			Light.ray(level, end.subtract(side.scale(MasterPursuitRules.HALF_WIDTH)), end.add(side.scale(MasterPursuitRules.HALF_WIDTH)), master.auraColor(), .07, life);
		}
	}

	private double clippedReach(Vec3 origin) {
		double reach = MasterPursuitRules.REACH;
		Vec3 side = new Vec3(-aim.z, 0, aim.x);
		for (double offset : new double[] {-MasterPursuitRules.HALF_WIDTH, 0, MasterPursuitRules.HALF_WIDTH}) {
			Vec3 start = origin.add(side.scale(offset)).add(0, .9, 0), end = start.add(aim.scale(reach));
			var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, master));
			if (hit.getType() != HitResult.Type.MISS) reach = Math.min(reach, start.distanceTo(hit.getLocation()));
		}
		return reach;
	}

	/** Swept native-body checks plus dense full-footprint support probes. Terrain is never edited to make a route. */
	static boolean safePath(SwordMaster master, ServerLevel level, Vec3 offset) {
		if (!Double.isFinite(offset.x) || !Double.isFinite(offset.y) || !Double.isFinite(offset.z) || Math.abs(offset.y) > .001) return false;
		AABB swept = master.getBoundingBox().expandTowards(offset);
		if (!master.pursuitInsideArena(swept) || !level.getWorldBorder().isWithinBounds(swept.minX, swept.minZ)
			|| !level.getWorldBorder().isWithinBounds(swept.maxX, swept.maxZ)) return false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(swept.minX, swept.minY - .05, swept.minZ),
			BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ))) {
			if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return false;
			var state = level.getBlockState(pos);
			if (!state.getFluidState().isEmpty() || hazardous(state)) return false;
		}
		if (!level.noCollision(master, swept)) return false;
		int samples = Math.max(1, (int) Math.ceil(offset.length() / MasterPursuitRules.PATH_SAMPLE));
		for (int i = 0; i <= samples; i++) {
			AABB body = master.getBoundingBox().move(offset.scale((double) i / samples));
			int y = BlockPos.containing(0, body.minY - .05, 0).getY();
			for (int x = (int) Math.floor(body.minX + .001); x <= (int) Math.floor(body.maxX - .001); x++) {
				for (int z = (int) Math.floor(body.minZ + .001); z <= (int) Math.floor(body.maxZ - .001); z++) {
					BlockPos floor = new BlockPos(x, y, z);
					var state = level.getBlockState(floor);
					if (!level.hasChunkAt(floor) || !state.isSolidRender() || !state.getFluidState().isEmpty() || hazardous(state)) return false;
				}
			}
		}
		return true;
	}

	private static boolean hazardous(net.minecraft.world.level.block.state.BlockState state) {
		return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
			|| state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW)
			|| state.is(Blocks.POINTED_DRIPSTONE);
	}
}
