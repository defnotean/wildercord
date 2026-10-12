package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Reefgrasp Octopus: a big mottled octopus of the seas, its mantle ruddy and warty, its eight long arms ringed with pale
 * suckers, and gold eyes with a slot of a pupil.
 *
 * <p>It lies on the seafloor with its arms spread, coloured to the sand, and waits. Swim close and its arms rise, then reach
 * out and wrap round you: held, you're drawn down to it, squeezed now and then, and the breath goes out of you three times
 * as fast, till it lets go, or you hurt it enough that it does, or you get out of the water. Hit it and it squirts a cloud
 * of ink that blinds everyone near, lets go of whatever it held, and jets off backwards, to settle somewhere else and hide
 * again. Roused, it comes after you and bites with its beak. Out of the water it dries out.</p>
 *
 * <p>Its pose flags: {@code VEILED} hidden on the seafloor, {@code WINDUP} its arms rising to reach, {@code ACTING} the reach
 * or a bite, {@code ALT} holding on, {@code GUARD} jetting away in its ink.</p>
 */
public class ReefgraspOctopus extends WildMonster {
	private long grabAt = -1;
	private long nextGrab;
	private long nextBite;
	private long nextInk;
	private long actingUntil;
	private long jetUntil = -1;
	private long roused;
	private int repath;
	private @Nullable LivingEntity held;
	private int heldTicks;
	private float harm;
	private Vec3 jet = Vec3.ZERO;

	public ReefgraspOctopus(EntityType<? extends ReefgraspOctopus> type, Level level) {
		super(type, level);
		this.xpReward = 9;
		moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.5F, 0.1F, true);
		setPathfindingMalus(PathType.WATER, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.6)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 16.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(6, new RandomSwimmingGoal(this, 0.6, 200) {
			@Override
			public boolean canUse() {
				return getTarget() == null && held == null && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	/** In the water it swims; hiding, it settles to the seafloor; out of it, it flops. */
	@Override
	public void travel(Vec3 input) {
		if (isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), input);
			move(MoverType.SELF, getDeltaMovement());
			Vec3 motion = getDeltaMovement().scale(0.9);
			if (getNavigation().isDone() && jetUntil < 0 && !onGround()) {
				motion = motion.add(0, -0.015, 0);
			}
			setDeltaMovement(motion);
		} else {
			super.travel(input);
		}
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.VENOM), List.of(Runes.SELF, Runes.HASTE));
	}

	/** Whether it is lying hidden (for the renderer, which colours it to the seafloor). */
	public boolean hidden() {
		return state(VEILED);
	}

	private static boolean inWater(Entity entity) {
		return entity.isInWater();
	}

	// ------------------------------------------------------------------ the hide, the grab, the ink

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		setState(ACTING, now < actingUntil);

		if (!isInWater()) {
			dryOut(level, now);
		}

		if (jetUntil >= 0) {
			if (now < jetUntil) {
				setState(GUARD, true);
				setState(VEILED, false);
				getNavigation().stop();
				if (isInWater()) {
					setDeltaMovement(jet.scale(0.5));
				}
				return;
			}
			jetUntil = -1;
			setState(GUARD, false);
		}

		if (held != null) {
			hold(level, now);
			return;
		}
		setState(ALT, false);

		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		if (!hunting) {
			grabAt = -1;
			setState(WINDUP, false);
			setState(VEILED, isInWater() && getNavigation().isDone());
			return;
		}
		double distance = distanceTo(target);
		getLookControl().setLookAt(target, 30, 30);

		if (grabAt >= 0) {
			face(target);
			getNavigation().stop();
			if (now >= grabAt) {
				reach(level, target, now);
			}
			return;
		}
		if (OctopusRules.grabs(distance, now, nextGrab, false, inWater(target)) && hasLineOfSight(target)) {
			grabAt = now + OctopusRules.GRAB_WINDUP;
			setState(WINDUP, true);
			setState(VEILED, false);
			getNavigation().stop();
			playSound(SoundEvents.SLIME_SQUISH, 0.8F, 0.6F);
			return;
		}
		if (now >= roused || !isInWater()) {
			// Not roused: it lies hidden and lets you come to it.
			setState(VEILED, isInWater());
			getNavigation().stop();
			return;
		}
		setState(VEILED, false);
		if (distance <= OctopusRules.BITE_REACH && now >= nextBite) {
			nextBite = now + OctopusRules.BITE_COOLDOWN;
			face(target);
			doHurtTarget(level, target);
			actingUntil = now + 6;
			setState(ACTING, true);
		}
		if (inWater(target) && (--repath <= 0 || getNavigation().isDone())) {
			repath = 10;
			getNavigation().moveTo(target, 1.0);
		}
	}

	/** Its arms shoot out; whatever they reach they wrap round. */
	private void reach(ServerLevel level, LivingEntity target, long now) {
		grabAt = -1;
		nextGrab = now + OctopusRules.GRAB_COOLDOWN;
		actingUntil = now + 8;
		setState(WINDUP, false);
		setState(ACTING, true);
		playSound(SoundEvents.SQUID_HURT, 1.0F, 0.6F);
		if (distanceTo(target) <= OctopusRules.GRAB_REACH + 0.5 && inWater(target)) {
			held = target;
			heldTicks = 0;
			harm = 0;
			setState(ALT, true);
			roused = now + 400;
			target.hurtServer(level, damageSources().mobAttack(this), OctopusRules.SQUEEZE_DAMAGE);
		}
	}

	/** Holding on: its prey drawn down to it, squeezed, its breath drawn out, till it lets go. */
	private void hold(ServerLevel level, long now) {
		LivingEntity prey = held;
		setState(ALT, true);
		setState(VEILED, false);
		getNavigation().stop();
		heldTicks++;
		if (prey == null || !prey.isAlive() || !Targets.canHarm(this, prey) || distanceTo(prey) > OctopusRules.GRAB_REACH + 2
			|| OctopusRules.letsGo(heldTicks, harm, inWater(prey))) {
			let(now);
			return;
		}
		face(prey);
		Vec3 arms = position().add(getLookAngle().multiply(0.9, 0, 0.9)).add(0, 0.4, 0);
		Vec3 pull = arms.subtract(prey.position());
		if (pull.lengthSqr() > 0.2) {
			prey.setDeltaMovement(prey.getDeltaMovement().scale(0.5).add(pull.normalize().scale(0.12)));
			prey.needsSync = true;
		}
		if (prey.isUnderWater()) {
			prey.setAirSupply(OctopusRules.drains(prey.getAirSupply()));
		}
		if (OctopusRules.squeezes(heldTicks)) {
			prey.hurtServer(level, damageSources().mobAttack(this), OctopusRules.SQUEEZE_DAMAGE);
			actingUntil = now + 5;
			setState(ACTING, true);
			playSound(SoundEvents.SLIME_SQUISH, 0.7F, 0.5F);
		}
	}

	private void let(long now) {
		if (held != null) {
			held = null;
			nextGrab = now + OctopusRules.GRAB_COOLDOWN;
			setState(ALT, false);
		}
	}

	/** A cloud of ink that blinds all near, its grip let go, and off it jets, away from whoever struck it. */
	private void ink(ServerLevel level, long now, @Nullable Entity from) {
		nextInk = now + OctopusRules.INK_COOLDOWN;
		let(now);
		grabAt = -1;
		setState(WINDUP, false);
		roused = now;
		jetUntil = now + OctopusRules.JET_TICKS;
		Vec3 away = from == null ? new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5) : position().subtract(from.position());
		if (away.lengthSqr() < 1.0E-4) {
			away = new Vec3(0, 0, 1);
		}
		jet = away.multiply(1, 0.2, 1).normalize();
		setState(GUARD, true);
		playSound(SoundEvents.SQUID_SQUIRT, 1.2F, 0.7F);
		level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 80, 1.0, 0.8, 1.0, 0.05);
		for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(OctopusRules.INK_RADIUS),
			e -> e != this && e.isAlive() && e.distanceTo(this) <= OctopusRules.INK_RADIUS)) {
			near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, OctopusRules.INK_BLIND, 0), this);
		}
	}

	/** Out of the water: it dries out, and flops about hoping to land back in. */
	private void dryOut(ServerLevel level, long now) {
		if (now % OctopusRules.DRY_EVERY == 0) {
			hurtServer(level, damageSources().dryOut(), 1.0F);
		}
		if (onGround() && random.nextFloat() < 0.08F) {
			setDeltaMovement(getDeltaMovement().add((random.nextFloat() * 2 - 1) * 0.15, 0.35, (random.nextFloat() * 2 - 1) * 0.15));
			setYRot(random.nextFloat() * 360);
			playSound(SoundEvents.GUARDIAN_FLOP, 0.8F, 0.6F);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isAlive()) {
			long now = level.getGameTime();
			if (held != null) {
				harm += damage;
			}
			if (!source.is(net.minecraft.world.damagesource.DamageTypes.DRY_OUT) && isInWater() && OctopusRules.inks(now, nextInk)) {
				ink(level, now, source.getEntity());
			} else if (jetUntil < 0) {
				roused = now + 400;
			}
		}
		return hurt;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return held == null && super.removeWhenFarAway(distance);
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.SQUID_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SQUID_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SQUID_DEATH;
	}
}
