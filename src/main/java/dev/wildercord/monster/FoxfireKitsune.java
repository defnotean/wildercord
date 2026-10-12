package dev.wildercord.monster;

import dev.wildercord.cast.Targets;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Foxfire Kitsune: a slim white fox of the cherry groves, bigger than any fox, with red markings on its face and three
 * great fluffy tails tipped with cold blue fire. It comes out among the blossoms at night.
 *
 * <p>From a distance it fans its tails up, the fire in their tips brightening, and flicks a wisp of foxfire at you: a
 * slow blue flame that sails straight and can be stepped out of the way of, and that burns where it lands. Up close it
 * bites. Hurt past two thirds of its health, and again past a third, it spins and splits into three of itself in a puff of
 * blue fire, and takes the place of one of its copies. The copies run and snap and throw their foxfire just as it does,
 * but their bites don't bite, their fire is cold, and any blow scatters one in a puff of smoke. Only the real one sheds
 * petals as it runs, and only the real one casts a shadow. The copies fade after a while, or once the real one is gone.</p>
 *
 * <p>Its pose flags: {@code WINDUP} the tails fanned up, {@code ACTING} the throw or the bite, {@code ALT} the split's spin.</p>
 */
public class FoxfireKitsune extends WildMonster {
	private static final EntityDataAccessor<Boolean> DATA_DECOY = SynchedEntityData.defineId(FoxfireKitsune.class, EntityDataSerializers.BOOLEAN);
	/** How fast its foxfire sails, in blocks a tick, and how long before it gutters out. */
	private static final double FIRE_SPEED = 0.55;
	private static final int FIRE_LIFE = 40;

	private long nextFoxfire;
	private long foxfireAt = -1;
	private long nextBite;
	private long actingUntil;
	private long spinUntil;
	private int splitsDone;
	private int repath;
	/** A copy's maker, by its entity id (copies aren't saved, so this needn't outlive the world). */
	private int makerId = -1;
	/** Its foxfire in flight, if any. */
	private @Nullable Vec3 firePos;
	private Vec3 fireVel = Vec3.ZERO;
	private int fireLife;

	public FoxfireKitsune(EntityType<? extends FoxfireKitsune> type, Level level) {
		super(type, level);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 24.0)
			.add(Attributes.MOVEMENT_SPEED, 0.32)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DECOY, false);
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
	public List<List<RuneDef>> runeboundSpells() {
		return List.of(List.of(Runes.BOLT, Runes.FIRE), List.of(Runes.SELF, Runes.BLINK));
	}

	/** Whether this is only a copy of a kitsune, conjured as it split. */
	public boolean decoy() {
		return entityData.get(DATA_DECOY);
	}

	// ------------------------------------------------------------------ the hunt

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		long now = level.getGameTime();
		if (decoy() && fades(level)) {
			poof(level);
			discard();
			return;
		}
		flyFoxfire(level);
		setState(ALT, now < spinUntil);
		setState(ACTING, now < actingUntil);
		// Only the real one sheds petals as it runs.
		if (!decoy() && getDeltaMovement().horizontalDistanceSqr() > 0.002 && tickCount % 5 == 0) {
			level.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.6, getZ(), 1, 0.25, 0.2, 0.25, 0);
		}

		LivingEntity target = getTarget();
		boolean live = target != null && target.isAlive() && Targets.canHarm(this, target);
		if (!live) {
			foxfireAt = -1;
			setState(WINDUP, false);
			return;
		}
		if (now < spinUntil) {
			getNavigation().stop();
			return;
		}
		if (foxfireAt >= 0) {
			getNavigation().stop();
			face(target);
			if (tickCount % 3 == 0) {
				tailFire(level, 2);
			}
			if (now >= foxfireAt) {
				foxfireAt = -1;
				setState(WINDUP, false);
				throwFoxfire(level, target, now);
			}
			return;
		}

		double distance = distanceTo(target);
		if (KitsuneRules.throwsFoxfire(distance, now, nextFoxfire, hasLineOfSight(target)) && random.nextInt(3) == 0) {
			foxfireAt = now + KitsuneRules.FOXFIRE_WINDUP;
			nextFoxfire = now + KitsuneRules.FOXFIRE_WINDUP + KitsuneRules.FOXFIRE_COOLDOWN + random.nextInt(20);
			setState(WINDUP, true);
			getNavigation().stop();
			face(target);
			playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.8F, 1.4F);
			return;
		}
		if (KitsuneRules.bites(distance, now, nextBite)) {
			nextBite = now + KitsuneRules.BITE_COOLDOWN;
			actingUntil = now + 8;
			face(target);
			doHurtTarget(level, target);
			playSound(SoundEvents.FOX_BITE, 1.0F, 0.8F);
		}
		getLookControl().setLookAt(target, 30, 30);
		if (--repath <= 0 || getNavigation().isDone()) {
			repath = 6;
			getNavigation().moveTo(target, 1.15);
		}
		if (random.nextInt(90) == 0) {
			playSound(SoundEvents.FOX_SCREECH, 0.8F, 1.2F);
		}
	}

	/** Its foxfire, flicked off its tails: a slow wisp aimed at where its prey is now. */
	private void throwFoxfire(ServerLevel level, LivingEntity target, long now) {
		actingUntil = now + 10;
		Vec3 from = new Vec3(getX(), getY() + getBbHeight() * 0.8, getZ());
		Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
		firePos = from;
		fireVel = to.subtract(from).normalize().scale(FIRE_SPEED);
		fireLife = FIRE_LIFE;
		playSound(SoundEvents.FIRECHARGE_USE, 0.7F, 1.6F);
	}

	/** The wisp in flight: on in a straight line, a trail of blue fire, and a burst where it lands. */
	private void flyFoxfire(ServerLevel level) {
		if (firePos == null) {
			return;
		}
		Vec3 next = firePos.add(fireVel);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, firePos.x, firePos.y, firePos.z, 3, 0.08, 0.08, 0.08, 0.01);
		HitResult wall = level.clip(new ClipContext(firePos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		AABB reach = new AABB(firePos, next).inflate(0.5);
		List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, reach,
			other -> other.isAlive() && !(other instanceof FoxfireKitsune) && Targets.canHarm(this, other));
		if (!hit.isEmpty()) {
			LivingEntity struck = hit.get(0);
			// A copy's foxfire is cold: it looks the same, and does nothing.
			if (!decoy()) {
				struck.hurtServer(level, damageSources().indirectMagic(this, this), KitsuneRules.FOXFIRE_DAMAGE);
				struck.igniteForSeconds(KitsuneRules.FOXFIRE_BURN);
			}
			burst(level, struck.position().add(0, struck.getBbHeight() * 0.5, 0));
			return;
		}
		if (wall.getType() != HitResult.Type.MISS || --fireLife <= 0) {
			burst(level, wall.getType() != HitResult.Type.MISS ? wall.getLocation() : next);
			return;
		}
		firePos = next;
	}

	private void burst(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 14, 0.25, 0.25, 0.25, 0.04);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 0.8F, 1.3F);
		firePos = null;
	}

	/** A flicker of blue fire off the tips of its tails. */
	private void tailFire(ServerLevel level, int count) {
		Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-0.7);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + back.x, getY() + 1.1, getZ() + back.z, count, 0.3, 0.15, 0.3, 0.005);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		// A copy's jaws close on nothing.
		if (decoy()) {
			return false;
		}
		return super.doHurtTarget(level, target);
	}

	// ------------------------------------------------------------------ the split

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (decoy()) {
			// Any blow scatters a copy.
			if (!isInvulnerableTo(level, source)) {
				poof(level);
				discard();
			}
			return false;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && isAlive() && KitsuneRules.splits(getHealth(), getMaxHealth(), splitsDone)) {
			split(level);
		}
		return hurt;
	}

	/** It spins and splits into three of itself in a puff of blue fire, and takes the place of one of its copies. */
	private void split(ServerLevel level) {
		splitsDone++;
		long now = level.getGameTime();
		spinUntil = now + KitsuneRules.SPLIT_TICKS;
		foxfireAt = -1;
		setState(WINDUP, false);
		setState(ALT, true);
		Vec3 home = position();
		LivingEntity target = getTarget();
		FoxfireKitsune swap = null;
		for (int i = 0; i < KitsuneRules.DECOYS; i++) {
			FoxfireKitsune copy = MonsterContent.FOXFIRE_KITSUNE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (copy == null) {
				continue;
			}
			double angle = random.nextDouble() * Math.PI * 2;
			Vec3 spot = home.add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);
			if (!level.noCollision(copy, copy.getDimensions(copy.getPose()).makeBoundingBox(spot))) {
				spot = home;
			}
			copy.snapTo(spot.x, spot.y, spot.z, getYRot(), getXRot());
			copy.entityData.set(DATA_DECOY, true);
			copy.makerId = getId();
			copy.splitsDone = KitsuneRules.MAX_SPLITS;
			copy.setHealth(getHealth());
			copy.spinUntil = spinUntil;
			copy.nextFoxfire = now + random.nextInt(KitsuneRules.FOXFIRE_COOLDOWN);
			copy.setTarget(target);
			copy.setPersistenceRequired();
			level.addFreshEntity(copy);
			copy.setState(ALT, true);
			if (swap == null && random.nextBoolean()) {
				swap = copy;
			}
			poofAt(level, spot);
		}
		poof(level);
		if (swap != null) {
			// Trade places, so you can't keep your eye on the real one.
			Vec3 there = swap.position();
			swap.snapTo(home.x, home.y, home.z, swap.getYRot(), swap.getXRot());
			snapTo(there.x, there.y, there.z, getYRot(), getXRot());
		}
		playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
	}

	/** Whether a copy fades: its time is up, or the real one is dead or far off. */
	private boolean fades(ServerLevel level) {
		Entity maker = makerId >= 0 ? level.getEntity(makerId) : null;
		boolean alive = maker instanceof FoxfireKitsune kitsune && kitsune.isAlive() && !kitsune.decoy();
		return KitsuneRules.decoyFades(tickCount, alive, alive ? distanceTo(maker) : 0);
	}

	private void poof(ServerLevel level) {
		poofAt(level, position());
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), getSoundSource(), 1.0F, 1.5F);
	}

	private static void poofAt(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.6, at.z, 16, 0.35, 0.4, 0.35, 0.03);
		level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.6, at.z, 8, 0.3, 0.3, 0.3, 0.02);
	}

	// ------------------------------------------------------------------ copies leave nothing behind

	@Override
	public boolean shouldBeSaved() {
		return !decoy() && super.shouldBeSaved();
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return !decoy() && super.shouldDropLoot(level);
	}

	@Override
	public boolean shouldDropExperience() {
		return !decoy() && super.shouldDropExperience();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !decoy() && super.removeWhenFarAway(distance);
	}

	// ------------------------------------------------------------------ voice

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.FOX_AMBIENT;
	}

	@Override
	public float getVoicePitch() {
		return 0.8F + random.nextFloat() * 0.15F;
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
		playSound(SoundEvents.FOX_SNIFF, 0.05F, 1.6F);
	}
}
