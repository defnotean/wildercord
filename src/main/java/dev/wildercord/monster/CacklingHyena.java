package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * The Cackling Hyena: a scruffy spotted hyena of the savannas, high in the shoulder and low in the hip, with a bristling
 * mane down its neck and back, round ears, a dark muzzle and eyes that shine amber in the dark. They hunt in packs at
 * night, and never all at once.
 *
 * <p>The pack circles its prey at a distance, trotting round and round, cackling. One at a time, one of them crouches
 * and darts in to bite, sooner if your back is turned, and the bite hamstrings; then it falls back out to the circle and
 * another takes its turn. A dart that misses leaves it stumbling, open to a blow. Once the prey is badly hurt, the pack
 * grows bold: the circle tightens and the darts come quicker. A badly hurt hyena slinks off, tail tucked, for a while
 * before it comes back.</p>
 *
 * <p>Its pose flags: {@code WINDUP} the crouch, {@code ACTING} the dart, {@code STUNNED} a missed dart's stumble,
 * {@code ALT} falling back, {@code GUARD} slinking off.</p>
 */
public class CacklingHyena extends WildMonster {
	private long nextDart;
	private long dartAt = -1;
	private long dartUntil = -1;
	private long stumbleUntil;
	private long retreatUntil;
	private long cowerUntil;
	private long nextCower;
	private boolean bitten;
	private double angle;
	private int way = 1;
	private int repath;

	public CacklingHyena(EntityType<? extends CacklingHyena> type, Level level) {
		super(type, level);
		this.xpReward = 6;
		this.angle = random.nextDouble() * Math.PI * 2;
		this.way = random.nextBoolean() ? 1 : -1;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 16.0)
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
			@Override
			public boolean canUse() {
				return getTarget() == null && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected boolean packHunter() {
		return true;
	}

	@Override
	protected boolean hasVariants() {
		return true;
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.BLEED), List.of(Runes.DASH, Runes.REND));
	}

	/** Whether it's crouched to dart or darting. */
	public boolean darting() {
		return dartAt >= 0 || dartUntil >= 0;
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean live = target != null && target.isAlive() && Targets.canHarm(this, target);

		setState(STUNNED, now < stumbleUntil);
		setState(ALT, now < retreatUntil);
		setState(GUARD, now < cowerUntil);
		if (!live) {
			dartAt = -1;
			dartUntil = -1;
			setState(WINDUP, false);
			setState(ACTING, false);
			return;
		}

		if (now < stumbleUntil) {
			getNavigation().stop();
			return;
		}
		if (dartAt >= 0) {
			crouch(level, target, now);
			return;
		}
		if (dartUntil >= 0) {
			dart(level, target, now);
			return;
		}
		if (HyenaRules.cowers(getHealth(), getMaxHealth(), now, nextCower)) {
			cowerUntil = now + HyenaRules.COWER_TICKS;
			nextCower = cowerUntil + HyenaRules.COWER_COOLDOWN;
			setState(GUARD, true);
			playSound(SoundEvents.FOX_HURT, 0.8F, 0.6F);
		}
		if (now < cowerUntil || now < retreatUntil) {
			away(target, now < cowerUntil ? 1.4 : 1.25, now < cowerUntil ? 14 : HyenaRules.orbit(bold(target)) + 2);
			return;
		}
		circle(level, target, now);
	}

	private static boolean bold(LivingEntity target) {
		return HyenaRules.bold(target.getHealth(), target.getMaxHealth());
	}

	/** Round and round its prey, cackling, until it's its turn to dart in. */
	private void circle(ServerLevel level, LivingEntity target, long now) {
		boolean bold = bold(target);
		double radius = HyenaRules.orbit(bold);
		getLookControl().setLookAt(target, 30, 30);
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 8;
			// Start from where it stands on the circle, so it trots round rather than cutting across.
			angle = Math.atan2(getZ() - target.getZ(), getX() - target.getX()) + way * HyenaRules.ORBIT_STEP;
			if (random.nextInt(12) == 0) {
				way = -way;
			}
			double x = target.getX() + Math.cos(angle) * radius;
			double z = target.getZ() + Math.sin(angle) * radius;
			getNavigation().moveTo(x, target.getY(), z, bold ? 1.2 : 1.05);
		}
		if (random.nextInt(70) == 0) {
			cackle(level);
		}
		double distance = distanceTo(target);
		boolean backTurned = HyenaRules.backTurned(target.getLookAngle().dot(position().subtract(target.position()).normalize()));
		if (hasLineOfSight(target) && HyenaRules.darts(distance, now, nextDart, backTurned, packmateDarting(level))) {
			dartAt = now + HyenaRules.DART_WINDUP;
			nextDart = now + HyenaRules.DART_WINDUP + HyenaRules.cooldown(bold);
			setState(WINDUP, true);
			getNavigation().stop();
			face(target);
			playSound(SoundEvents.FOX_AGGRO, 0.9F, 0.7F);
		}
	}

	/** Whether another of its pack is darting in already (they take turns). */
	private boolean packmateDarting(ServerLevel level) {
		return !level.getEntitiesOfClass(CacklingHyena.class, getBoundingBox().inflate(16),
			other -> other != this && other.isAlive() && other.darting()).isEmpty();
	}

	/** Crouched low, rump up, eyes on its prey: a moment to turn and face it. */
	private void crouch(ServerLevel level, LivingEntity target, long now) {
		getNavigation().stop();
		face(target);
		if (now < dartAt) {
			return;
		}
		dartAt = -1;
		dartUntil = now + HyenaRules.DART_TICKS;
		bitten = false;
		setState(WINDUP, false);
		setState(ACTING, true);
		Vec3 to = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(HyenaRules.DART_SPEED);
		setDeltaMovement(to.x, 0.25, to.z);
		needsSync = true;
	}

	/** In at a rush: a bite if it gets there, a stumble if it doesn't. */
	private void dart(ServerLevel level, LivingEntity target, long now) {
		if (!bitten && distanceTo(target) <= HyenaRules.BITE_REACH) {
			bitten = true;
			face(target);
			if (doHurtTarget(level, target)) {
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, HyenaRules.HAMSTRING_TICKS, 0), this);
			}
			playSound(SoundEvents.FOX_BITE, 1.0F, 0.7F);
			cackle(level);
			endDart(now);
			retreatUntil = now + HyenaRules.RETREAT_TICKS;
			return;
		}
		if (now >= dartUntil) {
			endDart(now);
			stumbleUntil = now + HyenaRules.STUMBLE_TICKS;
			setState(STUNNED, true);
			playSound(SoundEvents.FOX_SNIFF, 0.8F, 0.6F);
		}
	}

	private void endDart(long now) {
		dartUntil = -1;
		setState(ACTING, false);
	}

	/** Away from its prey to about {@code range}, looking back over its shoulder. */
	private void away(LivingEntity target, double speed, double range) {
		getLookControl().setLookAt(target, 30, 30);
		if (distanceTo(target) >= range) {
			getNavigation().stop();
			return;
		}
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 6;
			Vec3 off = position().subtract(target.position()).multiply(1, 0, 1);
			if (off.lengthSqr() < 1.0E-4) {
				off = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
			}
			Vec3 spot = target.position().add(off.normalize().scale(range));
			getNavigation().moveTo(spot.x, spot.y, spot.z, speed);
		}
	}

	/** Its laugh, and the rest of the pack's after it now and then. */
	private void cackle(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 0.9F, 1.5F + random.nextFloat() * 0.3F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Caught stumbling, it takes the blow worse.
		if (state(STUNNED)) {
			damage *= 1.5F;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.FOX_SCREECH;
	}

	@Override
	public float getVoicePitch() {
		return 0.65F + random.nextFloat() * 0.15F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.FOX_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.FOX_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.WOLF_STEP.value(), 0.15F, 1.0F);
	}
}
