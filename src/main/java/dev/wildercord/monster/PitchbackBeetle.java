package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
 * The Pitchback Beetle: a great dark bark beetle of the taiga, as long as a man's arm and more, with a ridged black shell,
 * hooked red mandibles, long feelers, and on its back, swelling out behind its wing cases, a sac of amber resin that
 * glows faintly in the dark.
 *
 * <p>It keeps its distance. From a few blocks off it rears up, the sac swelling, and lobs a glob of resin in an arc: what
 * it lands on is gummed up and slowed, and a second glob while you're still gummed sticks you fast where you stand. Too
 * close and it backs away, snapping its mandibles if you follow. But fire bursts the sac in a flash of flame that catches
 * everything near it, the beetle too, and with its sac empty it has nothing left to throw: it comes on with its
 * mandibles until the sac fills again. Fire hurts it twice as hard besides.</p>
 *
 * <p>Its pose flags: {@code WINDUP} rearing up to lob, {@code ACTING} the lob or a snap of its mandibles, {@code ALT} its
 * sac burst and empty.</p>
 */
public class PitchbackBeetle extends WildMonster {
	private long lobAt = -1;
	private long nextLob;
	private long nextBite;
	private long actingUntil;
	private long refillAt;
	private int repath;

	public PitchbackBeetle(EntityType<? extends PitchbackBeetle> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 22.0)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7) {
			@Override
			public boolean canUse() {
				return getTarget() == null && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected boolean hasVariants() {
		return true;
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.VENOM), List.of(Runes.BOLT, Runes.VENOM));
	}

	private boolean sacFull(long now) {
		return now >= refillAt;
	}

	// ------------------------------------------------------------------ the fight

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		boolean full = sacFull(now);
		setState(ALT, !full);
		setState(ACTING, now < actingUntil);
		if (PitchbackRules.bursts(full, isOnFire())) {
			burst(level, now);
			full = false;
		}

		LivingEntity target = getTarget();
		if (target == null || !target.isAlive() || !Targets.canHarm(this, target)) {
			lobAt = -1;
			setState(WINDUP, false);
			return;
		}
		double distance = distanceTo(target);
		getLookControl().setLookAt(target, 30, 30);

		if (lobAt >= 0) {
			getNavigation().stop();
			face(target);
			if (!full) {
				// Burst while rearing up: nothing left to throw.
				lobAt = -1;
				setState(WINDUP, false);
			} else if (now >= lobAt) {
				lob(level, target, now);
			}
			return;
		}

		if (distance <= PitchbackRules.BITE_REACH && now >= nextBite) {
			nextBite = now + PitchbackRules.BITE_COOLDOWN;
			face(target);
			doHurtTarget(level, target);
			actingUntil = now + 6;
			setState(ACTING, true);
			playSound(SoundEvents.SPIDER_HURT, 0.7F, 1.4F);
		}

		if (PitchbackRules.lobs(distance, now, nextLob, full) && hasLineOfSight(target)) {
			lobAt = now + PitchbackRules.LOB_WINDUP;
			setState(WINDUP, true);
			getNavigation().stop();
			playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 0.5F);
			return;
		}

		if (--repath > 0 && !getNavigation().isDone()) {
			return;
		}
		repath = 10;
		if (!full) {
			// Nothing to throw: in it comes with its mandibles.
			getNavigation().moveTo(target, 1.2);
		} else if (PitchbackRules.backsOff(distance, true)) {
			Vec3 off = position().subtract(target.position()).multiply(1, 0, 1);
			if (off.lengthSqr() < 1.0E-4) {
				off = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
			}
			Vec3 spot = target.position().add(off.normalize().scale(PitchbackRules.KEEP));
			getNavigation().moveTo(spot.x, spot.y, spot.z, 1.15);
		} else if (distance > PitchbackRules.LOB_MAX - 1 || !hasLineOfSight(target)) {
			getNavigation().moveTo(target, 1.0);
		} else {
			getNavigation().stop();
		}
	}

	/** The sac snaps forward over its head and a glob of resin goes up in an arc. */
	private void lob(ServerLevel level, LivingEntity target, long now) {
		lobAt = -1;
		nextLob = now + PitchbackRules.LOB_COOLDOWN;
		actingUntil = now + 8;
		setState(WINDUP, false);
		setState(ACTING, true);
		ResinGlob glob = new ResinGlob(level, this);
		glob.setPos(getX(), getY() + getBbHeight() * 0.9, getZ());
		double dx = target.getX() - glob.getX();
		double dz = target.getZ() - glob.getZ();
		double dy = target.getY(0.4) - glob.getY();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		glob.shoot(dx, dy + PitchbackRules.lift(horizontal), dz, 1.1F, 3.0F);
		level.addFreshEntity(glob);
		playSound(SoundEvents.LLAMA_SPIT, 1.0F, 0.6F);
	}

	/** Fire reaches the sac: a flash of burning resin over everything near, itself too, and the sac empty a while. */
	private void burst(ServerLevel level, long now) {
		refillAt = now + PitchbackRules.REFILL_TICKS;
		lobAt = -1;
		setState(WINDUP, false);
		setState(ALT, true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.7F);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.6, getZ(), 40, 1.0, 0.4, 1.0, 0.08);
		level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.6, getZ(), 8, 0.6, 0.3, 0.6, 0.0);
		for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PitchbackRules.BURST_RADIUS),
			e -> e.isAlive() && e.distanceTo(this) <= PitchbackRules.BURST_RADIUS + 0.5)) {
			near.igniteForSeconds(PitchbackRules.BURST_FIRE_SECONDS);
			if (near != this) {
				near.hurtServer(level, level.damageSources().inFire(), PitchbackRules.BURST_DAMAGE);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_FIRE)) {
			damage *= PitchbackRules.FIRE_MULTIPLIER;
			long now = level.getGameTime();
			if (PitchbackRules.bursts(sacFull(now), true)) {
				burst(level, now);
			}
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.SILVERFISH_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SILVERFISH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SILVERFISH_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.SPIDER_STEP, 0.3F, 0.7F);
	}
}
