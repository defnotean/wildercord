package dev.wildercord.aura;

import dev.wildercord.aura.arts.ArtKit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Air Step and Plunging Strike: the slot's two aerial forms. Every accepted body position is swept against blocks, fluids,
 * hazards and wards. Each ends in one of three readable ways: a landing on safe footing, a splash into water, or a stall
 * (a hazard underfoot or nothing in time). Fall distance is never cleared, so neither form escapes fall damage.
 */
final class AerialMove extends FormDash.Move {
	private int ticks, ending = FormDashRules.IDLE;
	private boolean released;
	private AerialMove(ServerPlayer player, int form, Vec3 direction, float yaw) { super(player, form, direction, yaw); }

	/** Off the ground, out of water, once per time in the air, with a clear first stretch. A refused start costs nothing. */
	static AerialMove accept(ServerPlayer player, int form, boolean spent) {
		if (!FormDashRules.aerial(form) || !Double.isFinite(player.fallDistance) || player.isShiftKeyDown() || player.isPassenger()
			|| player.isFallFlying() || player.getAbilities().flying || player.onClimbable()
			|| !FormDashRules.airborne(player.onGround(), player.isInWater() || player.isInLava(), spent)) return null;
		Vec3 look = player.getLookAngle(), direction = new Vec3(look.x, 0, look.z);
		if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-4) {
			double rad = Math.toRadians(player.getYRot());
			direction = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
		}
		direction = direction.normalize();
		AerialMove move = new AerialMove(player, form, direction, (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
		Vec3 at = player.position();
		if (form == FormDashRules.AIR_STEP) return move.rise(at) == null ? null : move;
		// Plunging Strike needs open air below to dive through; a low hop is not enough.
		return WallTurn.swept(player, at, at.add(0, -FormDashRules.PLUNGE_MIN_DROP, 0), move.warded) ? move : null;
	}
	private Vec3 rise(Vec3 from) {
		Vec3 to = from.add(direction.scale(FormDashRules.AIR_STEP_DRIFT)).add(0, FormDashRules.AIR_STEP_RISE, 0);
		if (WallTurn.swept(player, from, to, warded)) return to;
		Vec3 up = from.add(0, FormDashRules.AIR_STEP_RISE, 0);
		return WallTurn.swept(player, from, up, warded) ? up : null;
	}
	boolean released() { return released && !aborted; }
	@Override boolean setting() { return set > 0; }
	@Override int end() { return ending == FormDashRules.IDLE ? FormDashRules.STALL : ending; }
	@Override boolean valid() {
		if (aborted || !owner.valid() || player.level() != level || !FormDash.able(player, form)) return false;
		return player.getMainHandItem() == blade && method.equals(Aura.data(player).method()) && !player.isShiftKeyDown()
			&& Double.isFinite(player.fallDistance) && (released || player.position().distanceToSqr(last) <= .64);
	}
	/** -1 aborts, 0 continues, 1 ends with {@link #end()}. */
	@Override int tick() {
		ticks++;
		if (released) {
			// Free fall under ordinary physics: watch how it comes down before the slot's own checks refuse water.
			// The client reports the ground; only real support under the reported body counts, so a stale flag cannot end the fall.
			boolean ground = player.onGround() && !level.noCollision(player, player.getBoundingBox().move(0, -.08, 0));
			int landed = FormDashRules.landing(ground && WallTurn.footing(player, player.position(), warded), player.isInWater(), ticks, FormDashRules.AIR_TIMEOUT);
			if (landed == FormDashRules.IDLE && (ground || player.isInLava())) landed = FormDashRules.STALL;
			if (landed != FormDashRules.IDLE) { ending = landed; fallRisk = Math.max(fallRisk, player.fallDistance); return 1; }
			return valid() ? 0 : -1;
		}
		if (!valid()) return -1;
		fallRisk = Math.max(fallRisk, player.fallDistance);
		if (form == FormDashRules.AIR_STEP) {
			Vec3 to = rise(player.position());
			if (to == null || !move(to) || ++step >= FormDashRules.AIR_STEP_TICKS) release();
			return 0;
		}
		if (set > 0) { set--; return move(last) ? 0 : -1; }
		Vec3 from = player.position(), to = from.add(0, -FormDashRules.PLUNGE_STRIDE, 0);
		if (WallTurn.swept(player, from, to, warded)) {
			if (!move(to)) return -1;
			if (++step >= FormDashRules.PLUNGE_TICKS) { ending = FormDashRules.STALL; return 1; }
			return 0;
		}
		// Blocked within this stride: settle onto the last clear height, then read what is underneath.
		double lo = 0, hi = FormDashRules.PLUNGE_STRIDE;
		for (int i = 0; i < 8; i++) { double mid = (lo + hi) / 2; if (WallTurn.swept(player, from, from.add(0, -mid, 0), warded)) lo = mid; else hi = mid; }
		Vec3 at = from.add(0, -lo, 0);
		if (lo > 0 && !move(at)) return -1;
		ending = WallTurn.footing(player, at, warded) ? FormDashRules.LAND : water(at) ? FormDashRules.SPLASH : FormDashRules.STALL;
		if (ending != FormDashRules.LAND) release();
		return 1;
	}
	/** Rising (or the dive's own stop) ends; ordinary physics carries the body from here, with the fall it already owed. */
	private void release() {
		released = true;
		player.setDeltaMovement(form == FormDashRules.AIR_STEP ? direction.scale(FormDashRules.AIR_STEP_DRIFT) : Vec3.ZERO);
		player.fallDistance = Math.max(player.fallDistance, fallRisk);
		player.needsSync = true;
		// The owning client moves its own body: send it the released motion, as a knockback would.
		if (player.connection != null) player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
	}
	private boolean water(Vec3 at) {
		for (double dy : new double[] {-.1, -.6}) if (level.getFluidState(BlockPos.containing(at.x, at.y + dy, at.z)).is(FluidTags.WATER)) return true;
		return false;
	}
	@Override void effect(int phase) {
		if (!owner.valid() || player.level() != level) return;
		if (phase == FormDashRules.SPLASH) { play(SoundEvents.PLAYER_SPLASH, .6F, 1.2F); return; }
		if (phase == FormDashRules.STALL) { play(SoundEvents.PLAYER_ATTACK_NODAMAGE, .7F, .7F); return; }
		if (form == FormDashRules.AIR_STEP) { play(SoundEvents.WIND_CHARGE_BURST.value(), .5F, 1.4F); return; }
		play(SoundEvents.MACE_SMASH_GROUND_HEAVY, .9F, .9F);
		int struck = 0;
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FormDashRules.PLUNGE_RADIUS, 1, FormDashRules.PLUNGE_RADIUS),
				e -> e != player && ArtKit.harmableWithoutAim(player, e))) {
			if (struck >= 6 || foe.distanceToSqr(player) > (FormDashRules.PLUNGE_RADIUS + .5) * (FormDashRules.PLUNGE_RADIUS + .5) || !visible(foe)) continue;
			AuraCombat.artStrike(player, foe, ArtKit.weapon(player) * FormDashRules.PLUNGE_SCALE * ArtKit.scale(), ArtRules.PVP_ART_CAP, false);
			struck++;
		}
	}
	private void play(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
	@Override void abort() { super.abort(); released = true; }
}
