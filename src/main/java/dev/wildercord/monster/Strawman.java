package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Strawman: a scarecrow of the plains and meadows, a stuffed sack for a head under a tattered hat, a patched coat,
 * straw for hands and sticks for legs. By day it stands where it stands, arms out, and is only a scarecrow. At night it
 * hunts, but only while nobody is looking at it. Look its way and it is stock still, in whatever way it was standing when
 * you caught it, never the same way twice; turn your back and it comes on, quickly, with a rustle of straw, and when it
 * is right behind you it strikes. Anyone looking at it holds it, so two together can watch each other's backs.
 *
 * <p>Stare it down too long from close by and the crows burst out of its sack in a black, beating racket, blinding
 * whoever was staring for a moment, and in that moment it moves. It is straw: fire takes it twice as hard.</p>
 */
public class Strawman extends WildMonster {
	private static final EntityDataAccessor<Byte> DATA_STANCE = SynchedEntityData.defineId(Strawman.class, EntityDataSerializers.BYTE);

	private boolean wasFrozen;
	private float frozenYaw;
	private int stared;
	private long nextStrike;
	private long strikeUntil;
	private long nextCrows;
	private long crowsUntil;
	private long nextRustle;
	private int repath;

	public Strawman(EntityType<? extends Strawman> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.ATTACK_DAMAGE, StrawmanRules.STRIKE_DAMAGE)
			.add(Attributes.FOLLOW_RANGE, StrawmanRules.HUNT_RANGE)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STANCE, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		// It hunts only by night; by day only a blow wakes it.
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
			(target, level) -> !level.isBrightOutside()));
	}

	@Override
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.BLIND), List.of(Runes.TOUCH, Runes.HARM));
	}

	/** Whether it's held still (someone is looking, or it's day and nothing has woken it), on either side. */
	public boolean frozen() {
		return state(VEILED);
	}

	/** Which of its stances it was caught in ({@link StrawmanRules#STANCES} of them). */
	public int stance() {
		return entityData.get(DATA_STANCE);
	}

	/** Holds it in one stance, for the tests' pictures and the guide's. */
	public void holdStance(int stance) {
		entityData.set(DATA_STANCE, (byte) Math.floorMod(stance, StrawmanRules.STANCES));
	}

	// ------------------------------------------------------------------ watched

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		Player starer = null;
		double starerDistance = Double.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || !player.isAlive()) {
				continue;
			}
			double distance = player.distanceTo(this);
			if (distance > StrawmanRules.WATCH_RANGE) {
				continue;
			}
			Vec3 look = player.getViewVector(1.0F);
			Vec3 eye = player.getEyePosition();
			// Its middle or its head in view: either holds it.
			Vec3 mid = getBoundingBox().getCenter().subtract(eye);
			Vec3 head = getEyePosition().subtract(eye);
			double dot = Math.max(StrawmanRules.lookDot(look.x, look.y, look.z, mid.x, mid.y, mid.z),
				StrawmanRules.lookDot(look.x, look.y, look.z, head.x, head.y, head.z));
			if (dot < StrawmanRules.LOOK_DOT) {
				continue;
			}
			if (StrawmanRules.watches(dot, distance, player.hasLineOfSight(this), player.hasEffect(MobEffects.BLINDNESS))
					&& distance < starerDistance) {
				starer = player;
				starerDistance = distance;
			}
		}
		LivingEntity target = getTarget();
		boolean live = target != null && target.isAlive() && Targets.canHarm(this, target);
		boolean dormant = level.isBrightOutside() && !live;
		boolean frozen = starer != null || dormant;

		if (frozen) {
			if (!wasFrozen) {
				// Caught: it's standing some new way, and turned to face whoever it was after.
				holdStance(StrawmanRules.nextStance(stance(), random.nextInt(1000)));
				frozenYaw = getYRot();
			}
			getNavigation().stop();
			setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
			setYRot(frozenYaw);
			setYBodyRot(frozenYaw);
			setYHeadRot(frozenYaw);
			setXRot(0);
			stared = starer != null ? stared + 1 : 0;
			if (starer != null && StrawmanRules.crows(stared, starerDistance, now, nextCrows)) {
				crows(level, starer, now);
			}
		} else {
			stared = 0;
			if (live) {
				hunt(level, target, now);
			} else {
				getNavigation().stop();
			}
		}
		wasFrozen = frozen;
		setState(VEILED, frozen);
		setState(WINDUP, live && !dormant);
		setState(ACTING, now < strikeUntil);
		setState(ALT, now < crowsUntil);
	}

	/** Nobody looking: it comes on fast, rustling, and strikes from right behind. */
	private void hunt(ServerLevel level, LivingEntity target, long now) {
		double distance = distanceTo(target);
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 5;
			getNavigation().moveTo(target, StrawmanRules.CREEP_SPEED);
		}
		getLookControl().setLookAt(target, 30, 30);
		if (now >= nextRustle && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
			nextRustle = now + 14 + random.nextInt(18);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.CROP_BREAK, SoundSource.HOSTILE, 0.35F, 0.6F + random.nextFloat() * 0.3F);
		}
		if (StrawmanRules.strikes(distance, false, now, nextStrike)) {
			face(target);
			nextStrike = now + StrawmanRules.STRIKE_COOLDOWN;
			strikeUntil = now + StrawmanRules.STRIKE_TICKS;
			doHurtTarget(level, target);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
		}
	}

	/** Stared down too long: its crows burst out of its sack and blind whoever was staring, for a moment. */
	private void crows(ServerLevel level, Player starer, long now) {
		nextCrows = now + StrawmanRules.CROW_COOLDOWN;
		crowsUntil = now + StrawmanRules.CROW_TICKS;
		stared = 0;
		starer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, StrawmanRules.BLIND_TICKS, 0), this);
		Vec3 head = getEyePosition();
		Vec3 toward = starer.getEyePosition().subtract(head).normalize();
		for (int i = 0; i < 24; i++) {
			Vec3 v = toward.scale(0.3 + random.nextDouble() * 0.4).add(random.nextGaussian() * 0.25, random.nextDouble() * 0.3, random.nextGaussian() * 0.25);
			level.sendParticles(ParticleTypes.SQUID_INK, head.x, head.y, head.z, 0, v.x, v.y, v.z, 1.0);
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), head.x, head.y, head.z, 12,
			0.3, 0.3, 0.3, 0.1);
		level.playSound(null, head.x, head.y, head.z, SoundEvents.PARROT_FLY, SoundSource.HOSTILE, 1.4F, 0.6F);
		level.playSound(null, head.x, head.y, head.z, SoundEvents.BAT_TAKEOFF, SoundSource.HOSTILE, 1.0F, 0.7F);
		level.playSound(null, head.x, head.y, head.z, SoundEvents.PARROT_IMITATE_PHANTOM, SoundSource.HOSTILE, 0.8F, 0.5F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage * StrawmanRules.damageScale(source.is(DamageTypeTags.IS_FIRE)));
		if (hurt) {
			Vec3 c = getBoundingBox().getCenter();
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()), c.x, c.y, c.z, 8,
				0.25, 0.4, 0.25, 0.05);
		}
		return hurt;
	}

	@Override
	public boolean isPushable() {
		return !frozen() && super.isPushable();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putByte("Stance", entityData.get(DATA_STANCE));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		holdStance(input.getByteOr("Stance", (byte) 0));
	}

	// It makes no sound of its own: only the rustle as it comes, and the straw when it's struck.
	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.CROP_BREAK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GRASS_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState block) {
		playSound(SoundEvents.GRASS_STEP, 0.25F, 0.7F);
	}
}
