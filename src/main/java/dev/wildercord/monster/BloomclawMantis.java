package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Bloomclaw Mantis: an orchid mantis of the flower forests, meadows and plains, white flushed with pink, its walking
 * legs lobed like petals, so that perched among the flowers it passes for one more blossom.
 *
 * <p>It perches still, rocking gently as a flower does in the breeze. Whoever comes near it notices, unless they come
 * sneaking, and then only from close by; it turns to them and creeps after them, slow. Within reach it rears in its threat
 * display, forelegs raised and wings flared wide (the tell), then its forelegs snap out. Caught, its prey is held fast in
 * front of it and nibbled now and then until it lets go; one good hard blow breaks its grip and leaves it staggering. Step
 * aside from the strike and it stumbles. Staggering or stumbling, blows land on it half again as hard.</p>
 *
 * <p>Its pose flags: {@code GUARD} perched, {@code WINDUP} the threat display, {@code ACTING} the strike, {@code ALT}
 * holding on, {@code STUNNED} stumbling or staggering.</p>
 */
public class BloomclawMantis extends WildMonster {
	private enum Phase { PERCH, STALK, WINDUP, STRIKE, CLUTCH, REEL }

	private Phase phase = Phase.PERCH;
	private long phaseUntil;
	private long nextStrike;
	private int heldTicks;
	private int stalked;
	private @Nullable LivingEntity held;

	public BloomclawMantis(EntityType<? extends BloomclawMantis> type, Level level) {
		super(type, level);
		this.xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 24.0)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.ARMOR, 1.0)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.TOUCH, Runes.PUSH), List.of(Runes.TOUCH, Runes.VENOM));
	}

	// ------------------------------------------------------------------ the perch

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		LivingEntity target = getTarget();
		boolean hunting = target != null && target.isAlive() && Targets.canHarm(this, target);
		switch (phase) {
			case PERCH -> perch(level, hunting);
			case WINDUP -> {
				getNavigation().stop();
				if (target != null) {
					getLookControl().setLookAt(target, 60, 60);
					face(target);
				}
				if (tickCount % 4 == 0) {
					level.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.8, getZ(), 2, 0.3, 0.2, 0.3, 0);
				}
				if (now >= phaseUntil) {
					phase = Phase.STRIKE;
					phaseUntil = now + MantisRules.STRIKE_TICKS;
					playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.5F);
				}
			}
			case STRIKE -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					strike(level, target, now);
				}
			}
			case CLUTCH -> clutch(level, now);
			case REEL -> {
				getNavigation().stop();
				if (now >= phaseUntil) {
					phase = hunting ? Phase.STALK : Phase.PERCH;
					stalked = 0;
				}
			}
			default -> stalk(level, target, hunting, now);
		}
		setState(GUARD, phase == Phase.PERCH);
		setState(WINDUP, phase == Phase.WINDUP);
		setState(ACTING, phase == Phase.STRIKE);
		setState(ALT, phase == Phase.CLUTCH);
		setState(STUNNED, phase == Phase.REEL);
	}

	/** Still among the flowers; noticing whoever comes near, unless they come sneaking. */
	private void perch(ServerLevel level, boolean hunting) {
		getNavigation().stop();
		setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
		if (hunting) {
			phase = Phase.STALK;
			stalked = 0;
			return;
		}
		if (tickCount % 5 != 0) {
			return;
		}
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(MantisRules.NOTICE_RANGE),
			p -> p.isAlive() && Targets.canHarm(this, p))) {
			double distance = distanceTo(player);
			if (distance < best && MantisRules.notices(distance, player.isShiftKeyDown()) && hasLineOfSight(player)) {
				nearest = player;
				best = distance;
			}
		}
		if (nearest != null) {
			setTarget(nearest);
			face(nearest);
			phase = Phase.STALK;
			stalked = 0;
		}
	}

	/** Creeping after its prey, slow and swaying, rearing to strike once it is near enough. */
	private void stalk(ServerLevel level, @Nullable LivingEntity target, boolean hunting, long now) {
		stalked++;
		if (!hunting || stalked > 400 || distanceTo(target) > MantisRules.NOTICE_RANGE + 4) {
			// Lost, or gone too far: it settles back among the flowers.
			setTarget(null);
			phase = Phase.PERCH;
			stalked = 0;
			return;
		}
		getLookControl().setLookAt(target, 30, 30);
		double distance = distanceTo(target);
		if (MantisRules.strikes(distance, now, nextStrike)) {
			phase = Phase.WINDUP;
			phaseUntil = now + MantisRules.WINDUP_TICKS;
			face(target);
			getNavigation().stop();
			playSound(SoundEvents.PHANTOM_FLAP, 0.8F, 1.6F);
			return;
		}
		if (getNavigation().isDone() || tickCount % 10 == 0) {
			getNavigation().moveTo(target, distance > 6 ? 1.0 : 0.7);
		}
	}

	/** The forelegs snap out: whoever stands squarely before it, near enough, is caught. */
	private void strike(ServerLevel level, @Nullable LivingEntity target, long now) {
		nextStrike = now + MantisRules.STRIKE_COOLDOWN;
		if (target != null && target.isAlive() && Targets.canHarm(this, target)) {
			Vec3 to = target.position().subtract(position());
			Vec3 flat = new Vec3(to.x, 0, to.z);
			Vec3 look = getLookAngle();
			Vec3 lookFlat = new Vec3(look.x, 0, look.z);
			double dot = flat.lengthSqr() < 1e-4 || lookFlat.lengthSqr() < 1e-4 ? 1 : flat.normalize().dot(lookFlat.normalize());
			if (MantisRules.catches(distanceTo(target), dot)
				&& target.hurtServer(level, damageSources().mobAttack(this), MantisRules.STRIKE_DAMAGE)) {
				held = target;
				heldTicks = 0;
				phase = Phase.CLUTCH;
				playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.3F);
				return;
			}
		}
		phase = Phase.REEL;
		phaseUntil = now + MantisRules.STUMBLE_TICKS;
		playSound(SoundEvents.SILVERFISH_HURT, 0.8F, 0.6F);
	}

	/** Holding on: its prey drawn in before it and nibbled, till it lets go. */
	private void clutch(ServerLevel level, long now) {
		LivingEntity prey = held;
		getNavigation().stop();
		heldTicks++;
		if (prey == null || !prey.isAlive() || !Targets.canHarm(this, prey) || distanceTo(prey) > MantisRules.STRIKE_REACH + 1.5
			|| MantisRules.letsGo(heldTicks)) {
			let();
			phase = Phase.STALK;
			stalked = 0;
			return;
		}
		face(prey);
		Vec3 claws = position().add(getLookAngle().multiply(0.9, 0, 0.9)).add(0, 0.4, 0);
		Vec3 pull = claws.subtract(prey.position());
		if (pull.lengthSqr() > 0.2) {
			prey.setDeltaMovement(prey.getDeltaMovement().scale(0.5).add(pull.normalize().scale(0.12)));
			prey.needsSync = true;
		}
		if (MantisRules.nibbles(heldTicks)) {
			prey.hurtServer(level, damageSources().mobAttack(this), MantisRules.NIBBLE_DAMAGE);
			playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.4F);
		}
	}

	private void let() {
		held = null;
		heldTicks = 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hit = super.hurtServer(level, source, MantisRules.hurt(damage, phase == Phase.REEL));
		if (!hit) {
			return false;
		}
		if (phase == Phase.CLUTCH && MantisRules.breaksFree(damage)) {
			// One good blow and its grip breaks: it staggers back, wide open.
			let();
			phase = Phase.REEL;
			phaseUntil = level.getGameTime() + MantisRules.STAGGER_TICKS;
			level.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.7, getZ(), 8, 0.3, 0.3, 0.3, 0);
		} else if (phase == Phase.PERCH && source.getEntity() instanceof LivingEntity attacker && Targets.canHarm(this, attacker)) {
			setTarget(attacker);
			phase = Phase.STALK;
			stalked = 0;
		}
		return true;
	}

	@Override
	public boolean isPushable() {
		return phase != Phase.PERCH && super.isPushable();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return held == null && super.removeWhenFarAway(distance);
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	public float getVoicePitch() {
		return 1.3F + random.nextFloat() * 0.2F;
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
		playSound(SoundEvents.SILVERFISH_STEP, 0.2F, 1.4F);
	}
}
