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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Gulper Pitcher: a carnivorous plant of the jungles as tall as a man, a great green-and-red pitcher on a rosette of
 * leaves, its rim beaded with nectar and a lid over its mouth, and a long vine coiled at its foot. It is rooted where it
 * grew and never moves.
 *
 * <p>Asleep its lid is shut and it only sways and drips. Come near (crouched, much nearer) and it stirs and opens. Out of
 * reach of its mouth it throws its lid wide and rears its vine back, a moment's tell, then lashes it out and hauls whoever
 * it catches in towards its mouth; at its mouth, it slams its lid down on them, and the acid of its pool poisons. It is a
 * plant: fire takes it twice as hard.</p>
 *
 * <p>Its pose flags: {@code ALT} stirred and open, {@code WINDUP} the vine reared back, {@code ACTING} the vine lashed out,
 * {@code GUARD} the bite.</p>
 */
public class GulperPitcher extends WildMonster {
	private long nextBite;
	private long biteUntil;
	private long nextLash;
	private long lashAt = -1;
	private long lashUntil;
	private int idle;

	public GulperPitcher(EntityType<? extends GulperPitcher> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.MOVEMENT_SPEED, 0.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.ATTACK_DAMAGE, GulperPitcherRules.BITE_DAMAGE)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.FOLLOW_RANGE, 12.0);
	}

	@Override
	protected void registerGoals() {
		// Rooted: no walking, no floating off. It only watches for who comes near (see below).
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
			(target, level) -> GulperPitcherRules.notices(target.distanceTo(this), target.isShiftKeyDown())));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.PULL), List.of(Runes.TOUCH, Runes.VENOM));
	}

	/** Whether it's stirred, its lid open. */
	public boolean open() {
		return state(ALT);
	}

	// ------------------------------------------------------------------ the pitcher

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		hold();
		LivingEntity target = getTarget();
		boolean live = target != null && target.isAlive() && Targets.canHarm(this, target)
			&& distanceTo(target) <= GulperPitcherRules.LASH_RANGE + 3;
		if (target != null && !live && (!target.isAlive() || distanceTo(target) > GulperPitcherRules.LASH_RANGE + 3)) {
			setTarget(null);
		}

		setState(GUARD, now < biteUntil);
		setState(ACTING, now < lashUntil);
		if (random.nextInt(open() ? 30 : 60) == 0) {
			drip(level);
		}

		if (lashAt >= 0) {
			setState(WINDUP, true);
			if (live) {
				face(target);
			}
			if (now >= lashAt) {
				lash(level, live ? target : null, now);
			}
			return;
		}
		setState(WINDUP, false);

		if (live) {
			idle = 0;
			if (!open()) {
				stir(level);
			}
			face(target);
			getLookControl().setLookAt(target, 30, 30);
			strike(level, target, now);
		} else if (open() && ++idle >= GulperPitcherRules.SETTLE_TICKS) {
			idle = 0;
			setState(ALT, false);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.HOSTILE, 0.8F, 0.7F);
		}
	}

	/** Rooted where it grew. */
	private void hold() {
		getNavigation().stop();
		setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
	}

	/** Its lid lifts. */
	private void stir(ServerLevel level) {
		setState(ALT, true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.HOSTILE, 0.8F, 0.6F);
		// Just woken it can bite, but its vine is a moment behind.
		nextLash = Math.max(nextLash, level.getGameTime() + GulperPitcherRules.LASH_WINDUP);
	}

	/** Bites at its mouth, or rears its vine for anyone further out. */
	private void strike(ServerLevel level, LivingEntity target, long now) {
		switch (GulperPitcherRules.attack(distanceTo(target), hasLineOfSight(target), now, nextBite, nextLash)) {
			case 1 -> {
				nextBite = now + GulperPitcherRules.BITE_COOLDOWN;
				biteUntil = now + GulperPitcherRules.BITE_TICKS;
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.0F, 0.5F);
				if (doHurtTarget(level, target)) {
					target.addEffect(new MobEffectInstance(MobEffects.POISON, GulperPitcherRules.ACID_TICKS, GulperPitcherRules.ACID_LEVEL), this);
				}
			}
			case 2 -> {
				// The vine reared back over its lid: a tell, and a moment to get out of reach.
				lashAt = now + GulperPitcherRules.LASH_WINDUP;
				nextLash = now + GulperPitcherRules.LASH_COOLDOWN;
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.CAVE_VINES_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);
			}
			default -> { }
		}
	}

	/** The vine lashed out: whoever is still in reach and in the open is struck and hauled in. */
	private void lash(ServerLevel level, @Nullable LivingEntity target, long now) {
		lashAt = -1;
		lashUntil = now + GulperPitcherRules.LASH_TICKS;
		setState(WINDUP, false);
		setState(ACTING, true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.0F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.0F, 1.4F);
		if (target == null) {
			return;
		}
		double distance = distanceTo(target);
		if (distance > GulperPitcherRules.LASH_RANGE + 0.75 || !hasLineOfSight(target)) {
			return;
		}
		target.hurtServer(level, damageSources().mobAttack(this), GulperPitcherRules.LASH_DAMAGE);
		double pull = GulperPitcherRules.pull(distance) * (1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5);
		Vec3 toward = new Vec3(getX() - target.getX(), 0, getZ() - target.getZ());
		if (pull > 0 && toward.lengthSqr() > 1.0E-4) {
			toward = toward.normalize().scale(pull);
			target.setDeltaMovement(toward.x, GulperPitcherRules.PULL_LIFT, toward.z);
			target.needsSync = true;
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.JUNGLE_LEAVES.defaultBlockState()),
			target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
	}

	/** Nectar off the rim, and from the vine. */
	private void drip(ServerLevel level) {
		double angle = random.nextDouble() * Math.PI * 2;
		level.sendParticles(ParticleTypes.FALLING_NECTAR, getX() + Math.cos(angle) * 0.35, getY() + 1.3, getZ() + Math.sin(angle) * 0.35,
			1, 0, 0, 0, 0);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return super.hurtServer(level, source, GulperPitcherRules.damage(damage, source.is(DamageTypeTags.IS_FIRE)));
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
		// Rooted: it shoves nothing aside, and nothing shoves it.
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return open() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.AZALEA_LEAVES_STEP;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GRASS_BREAK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AZALEA_LEAVES_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
	}
}
