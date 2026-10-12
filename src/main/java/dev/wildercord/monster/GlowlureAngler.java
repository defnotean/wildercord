package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * The Glowlure Angler: a broad, flat, eyeless cave salamander of the lush caves, its skin the pale mossy green of the cave
 * floor, with a huge mouth and, on a stalk from its brow, a glowing bulb that looks for all the world like a glow berry.
 *
 * <p>It lies flat and still in the dark with only its lure lit and swaying. Whoever looks straight at the lure finds
 * themselves drifting toward it, a little at a time; looking away or sneaking breaks the pull. Come within reach and its
 * jaw drops open (the tell), then it lunges and snaps. A snap that lands bites deep and leaves you slowed; one that misses
 * leaves it sprawled and winded, jaw hanging, and blows land half again as hard. On land it is slow, and after a while
 * it gives up the chase and settles back down to wait.</p>
 *
 * <p>Its pose flags: {@code GUARD} lying in wait, {@code WINDUP} the jaw dropping, {@code ACTING} the lunge,
 * {@code STUNNED} winded.</p>
 */
public class GlowlureAngler extends WildMonster {
	private enum Phase { LURK, WINDUP, LUNGE, WINDED, CHASE }

	private Phase phase = Phase.LURK;
	private long phaseUntil;
	private long nextSnap;
	private long nextBite;
	private int chased;
	private boolean landed;

	public GlowlureAngler(EntityType<? extends GlowlureAngler> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.ARMOR, 2.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8) {
			@Override
			public boolean canUse() {
				return phase == Phase.CHASE && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.PUSH), List.of(Runes.TOUCH, Runes.VENOM));
	}

	/** Whether it lies in wait, its lure lit. */
	public boolean lurking() {
		return phase == Phase.LURK;
	}

	// ------------------------------------------------------------------ the wait

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		switch (phase) {
			case LURK -> lurk(level, target, hunting, now);
			case WINDUP -> {
				getNavigation().stop();
				if (target != null) {
					getLookControl().setLookAt(target, 60, 60);
				}
				if (now >= phaseUntil) {
					lunge(level, target);
				}
			}
			case LUNGE -> lunging(level, now);
			case WINDED -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					phase = hunting ? Phase.CHASE : Phase.LURK;
					chased = 0;
				}
			}
			default -> chase(level, target, hunting, now);
		}
		setState(GUARD, phase == Phase.LURK);
		setState(WINDUP, phase == Phase.WINDUP);
		setState(ACTING, phase == Phase.LUNGE);
		setState(STUNNED, phase == Phase.WINDED);
	}

	/** Lying flat and still, the lure swaying; drawing in whoever stares at it, snapping at whoever comes close. */
	private void lurk(ServerLevel level, @Nullable LivingEntity target, boolean hunting, long now) {
		getNavigation().stop();
		setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		Vec3 lure = lure();
		if (tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.GLOW, lure.x, lure.y, lure.z, 1, 0.08, 0.08, 0.08, 0);
		}
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(AnglerRules.DRAW_RANGE),
			p -> p.isAlive() && Targets.canHarm(this, p))) {
			Vec3 eyes = player.getEyePosition();
			Vec3 toward = lure.subtract(eyes);
			double distance = toward.length();
			if (distance < 1e-3) {
				continue;
			}
			double look = player.getViewVector(1).dot(toward.scale(1 / distance));
			if (AnglerRules.draws(distance, look, player.isShiftKeyDown()) && player.hasLineOfSight(this)) {
				Vec3 flat = new Vec3(toward.x, 0, toward.z).normalize();
				MonsterMagic.shove(player, flat.scale(AnglerRules.pull(distance)));
				if (tickCount % 20 == 0) {
					level.sendParticles(ParticleTypes.GLOW, eyes.x + toward.x * 0.5, eyes.y + toward.y * 0.5, eyes.z + toward.z * 0.5,
						2, 0.2, 0.2, 0.2, 0);
				}
			}
		}
		if (hunting) {
			double distance = distanceTo(target);
			if (AnglerRules.snaps(distance, now, nextSnap)) {
				windup(target, now);
			} else if (getLastHurtByMob() == target) {
				// Struck from out of reach: it gives up the wait and comes after them.
				phase = Phase.CHASE;
				chased = 0;
			}
		}
	}

	private void windup(LivingEntity target, long now) {
		phase = Phase.WINDUP;
		phaseUntil = now + AnglerRules.WINDUP_TICKS;
		face(target);
		playSound(SoundEvents.FROG_AMBIENT, 1.0F, 0.5F);
	}

	private void lunge(ServerLevel level, @Nullable LivingEntity target) {
		phase = Phase.LUNGE;
		landed = false;
		Vec3 to = target != null ? target.position().subtract(position()) : getLookAngle();
		Vec3 flat = new Vec3(to.x, 0, to.z);
		Vec3 way = flat.lengthSqr() < 1e-4 ? getLookAngle() : flat.normalize();
		setYRot((float) (Mth.atan2(way.z, way.x) * Mth.RAD_TO_DEG) - 90);
		yBodyRot = yHeadRot = getYRot();
		setDeltaMovement(way.x * AnglerRules.LUNGE_SPEED, 0.25, way.z * AnglerRules.LUNGE_SPEED);
		phaseUntil = level.getGameTime() + AnglerRules.LUNGE_TICKS;
		playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 0.7F);
	}

	/** Mid-lunge: whatever it meets first, it bites. */
	private void lunging(ServerLevel level, long now) {
		getNavigation().stop();
		if (!landed) {
			for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5),
				e -> e != this && e.isAlive() && !(e instanceof GlowlureAngler) && Targets.canHarm(this, e))) {
				if (hit.hurtServer(level, damageSources().mobAttack(this), AnglerRules.SNAP_DAMAGE)) {
					hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AnglerRules.SNAP_SLOW_TICKS, 1, false, true), this);
					playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.6F);
				}
				landed = true;
				setDeltaMovement(getDeltaMovement().multiply(0.2, 1, 0.2));
				break;
			}
		}
		if (now >= phaseUntil) {
			nextSnap = now + AnglerRules.SNAP_COOLDOWN;
			if (landed) {
				phase = Phase.CHASE;
				nextBite = now + 15;
			} else {
				phase = Phase.WINDED;
				phaseUntil = now + AnglerRules.WINDED_TICKS;
				playSound(SoundEvents.FROG_HURT, 1.0F, 0.5F);
				level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.3, getZ(), 10, 0.4, 0.1, 0.4, 0.1);
			}
			chased = 0;
		}
	}

	/** Slow on land, it goes after its prey a while, biting, snapping again when it can, then settles. */
	private void chase(ServerLevel level, @Nullable LivingEntity target, boolean hunting, long now) {
		chased++;
		if (target == null || AnglerRules.settles(chased, hunting)) {
			phase = Phase.LURK;
			chased = 0;
			return;
		}
		getLookControl().setLookAt(target, 30, 30);
		double distance = distanceTo(target);
		if (AnglerRules.snaps(distance, now, nextSnap) && distance > AnglerRules.BITE_REACH) {
			windup(target, now);
			return;
		}
		if (AnglerRules.bites(distance, now, nextBite)) {
			nextBite = now + AnglerRules.BITE_COOLDOWN;
			face(target);
			doHurtTarget(level, target);
			playSound(SoundEvents.FROG_EAT, 1.0F, 0.6F);
		}
		if (getNavigation().isDone() || tickCount % 10 == 0) {
			getNavigation().moveTo(target, 1.0);
		}
	}

	/** Where its lure hangs: out in front of its brow. */
	private Vec3 lure() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		return position().add(-Mth.sin(yaw) * 0.75, 0.85, Mth.cos(yaw) * 0.75);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hit = super.hurtServer(level, source, AnglerRules.hurt(damage, phase == Phase.WINDED));
		if (hit && phase == Phase.LURK && source.getEntity() instanceof LivingEntity attacker && Targets.canHarm(this, attacker)) {
			setTarget(attacker);
			phase = Phase.CHASE;
			chased = 0;
		}
		return hit;
	}

	@Override
	public boolean isPushable() {
		return phase != Phase.LURK && super.isPushable();
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return phase == Phase.CHASE ? SoundEvents.FROG_AMBIENT : null;
	}

	@Override
	public float getVoicePitch() {
		return 0.45F + random.nextFloat() * 0.1F;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.FROG_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.FROG_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.FROG_STEP, 0.4F, 0.6F);
	}
}
