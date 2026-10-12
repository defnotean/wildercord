package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Cragcleaver Ram: a huge shaggy grey ram of the windswept hills, bigger than a horse at the shoulder, with a dark face
 * and two great ridged horns curled round past its jaw.
 *
 * <p>Up close it butts, throwing you back. Further off it stops, lowers its head and paws the ground, its eyes on you, then
 * charges straight and fast, and whatever it hits is thrown a long way. Its aim sets a moment before it goes, so a step aside
 * at the last moment lets it thunder past. Run it into a wall or a cliff and its horns stick in the rock: it stands dazed a
 * few seconds, seeing stars, and blows land half again as hard.</p>
 *
 * <p>Its pose flags: {@code WINDUP} pawing, {@code ACTING} the charge or a butt, {@code STUNNED} dazed against the rock.</p>
 */
public class CragcleaverRam extends WildMonster {
	private enum Phase { ROAM, PAW, CHARGE, DAZED }

	private Phase phase = Phase.ROAM;
	private long phaseUntil;
	private long nextCharge;
	private long nextButt;
	private long buttingUntil;
	private int run;
	private int repath;
	private Vec3 aim = Vec3.ZERO;

	public CragcleaverRam(EntityType<? extends CragcleaverRam> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 44.0)
			.add(Attributes.MOVEMENT_SPEED, 0.26)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 28.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				return getTarget() == null && phase == Phase.ROAM && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.PUSH), List.of(Runes.SELF, Runes.HASTE));
	}

	/** Whether it stands dazed with its horns in the rock. */
	public boolean dazed() {
		return phase == Phase.DAZED;
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);

		switch (phase) {
			case PAW -> paw(level, target, hunting, now);
			case CHARGE -> charge(level, now);
			case DAZED -> {
				getNavigation().stop();
				if (now % 6 == 0) {
					level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 3, 0.35, 0.1, 0.35, 0.02);
				}
				if (now >= phaseUntil) {
					phase = Phase.ROAM;
					playSound(SoundEvents.GOAT_AMBIENT, 1.0F, 0.6F);
				}
			}
			default -> roam(level, target, hunting, now);
		}
		setState(WINDUP, phase == Phase.PAW);
		setState(ACTING, phase == Phase.CHARGE || now < buttingUntil);
		setState(STUNNED, phase == Phase.DAZED);
	}

	private void roam(ServerLevel level, @Nullable LivingEntity target, boolean hunting, long now) {
		if (!hunting) {
			return;
		}
		double distance = distanceTo(target);
		getLookControl().setLookAt(target, 30, 30);
		if (RamRules.charges(distance, now, nextCharge, clearRun(target))) {
			phase = Phase.PAW;
			phaseUntil = now + RamRules.PAW_TICKS;
			getNavigation().stop();
			playSound(SoundEvents.GOAT_PREPARE_RAM, 1.4F, 0.6F);
			return;
		}
		if (RamRules.butts(distance, now, nextButt)) {
			nextButt = now + RamRules.BUTT_COOLDOWN;
			buttingUntil = now + 8;
			face(target);
			if (doHurtTarget(level, target)) {
				MonsterMagic.knock(target, position(), RamRules.BUTT_KNOCK);
			}
			playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, 0.8F);
		}
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 10;
			getNavigation().moveTo(target, 1.1);
		}
	}

	/** Head low, a hoof scraping the ground; its aim follows its prey, then sets just before it goes. */
	private void paw(ServerLevel level, @Nullable LivingEntity target, boolean hunting, long now) {
		getNavigation().stop();
		if (!hunting) {
			phase = Phase.ROAM;
			return;
		}
		long left = phaseUntil - now;
		if (RamRules.aiming(left)) {
			face(target);
			Vec3 d = target.position().subtract(position()).multiply(1, 0, 1);
			aim = d.lengthSqr() < 1.0E-4 ? getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
		}
		if (now % 8 == 0) {
			BlockState under = level.getBlockState(blockPosition().below());
			if (!under.isAir()) {
				Vec3 hoof = position().add(aim.scale(0.7));
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), hoof.x, getY() + 0.1, hoof.z, 6, 0.15, 0.05, 0.15, 0.1);
			}
			playSound(SoundEvents.GOAT_STEP, 1.0F, 0.6F);
		}
		if (left <= 0) {
			phase = Phase.CHARGE;
			run = 0;
			nextCharge = now + RamRules.CHARGE_COOLDOWN;
			playSound(SoundEvents.GOAT_LONG_JUMP, 1.4F, 0.5F);
		}
	}

	/** Thundering straight along its aim, throwing whatever it hits, till it runs out, hits something or meets a wall. */
	private void charge(ServerLevel level, long now) {
		getNavigation().stop();
		run++;
		setDeltaMovement(aim.x * RamRules.CHARGE_SPEED, getDeltaMovement().y, aim.z * RamRules.CHARGE_SPEED);
		float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		if (run % 2 == 0) {
			BlockState under = level.getBlockState(blockPosition().below());
			if (!under.isAir()) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 4, 0.4, 0.05, 0.4, 0.05);
			}
		}
		boolean struck = false;
		for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.3),
			e -> e != this && e.isAlive() && !(e instanceof CragcleaverRam) && Targets.canHarm(this, e))) {
			if (hit.hurtServer(level, damageSources().mobAttack(this), RamRules.CHARGE_DAMAGE)) {
				MonsterMagic.shove(hit, aim.scale(RamRules.CHARGE_KNOCK).add(0, 0.55, 0));
			}
			struck = true;
		}
		if (struck) {
			playSound(SoundEvents.GOAT_RAM_IMPACT, 1.6F, 0.6F);
			buttingUntil = now + 8;
			phase = Phase.ROAM;
			setDeltaMovement(getDeltaMovement().scale(0.2));
		} else if (RamRules.dazes(horizontalCollision, run)) {
			daze(level, now);
		} else if (RamRules.spent(run, getDeltaMovement().horizontalDistance())) {
			phase = Phase.ROAM;
		}
	}

	/** Into the rock: horns stuck, dazed. */
	private void daze(ServerLevel level, long now) {
		phase = Phase.DAZED;
		phaseUntil = now + RamRules.DAZE_TICKS;
		setDeltaMovement(getDeltaMovement().multiply(-0.2, 1, -0.2));
		BlockPos wall = BlockPos.containing(position().add(aim.scale(getBbWidth() * 0.5 + 0.5)).add(0, 0.8, 0));
		BlockState rock = level.getBlockState(wall);
		if (rock.isAir()) {
			rock = level.getBlockState(wall.below());
		}
		if (!rock.isAir()) {
			Vec3 at = Vec3.atCenterOf(wall);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, rock), at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.15);
		}
		playSound(SoundEvents.GOAT_RAM_IMPACT, 2.0F, 0.5F);
		playSound(SoundEvents.GOAT_HORN_BREAK, 0.8F, 0.6F);
	}

	/** A straight run at its prey: it can see it and it stands on much the same level. */
	private boolean clearRun(LivingEntity target) {
		return hasLineOfSight(target) && Math.abs(target.getY() - getY()) < 2.5;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return super.hurtServer(level, source, RamRules.hurt(damage, dazed()));
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return phase == Phase.ROAM ? SoundEvents.GOAT_AMBIENT : null;
	}

	@Override
	public float getVoicePitch() {
		return 0.55F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GOAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GOAT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.GOAT_STEP, 0.6F, 0.6F);
	}
}
