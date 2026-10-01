package dev.wildercord.monster;

import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Targets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Bog Witch-Frog's bubble of bog poison: a wobbling, glowing green bubble lobbed in a high, slow arc. It bursts where it
 * lands (on the ground or on someone), splashing poison over everything within reach and leaving a little poisoned mist,
 * and its harm is the frog's magic, through every spell defence. Anything that hits it first (an arrow, a snowball, a
 * blade) pops it harmlessly in the air.
 */
public class BogBubble extends Projectile {
	static final float DAMAGE = 3.0F;
	static final double RADIUS = 1.7;
	static final int POISON = 100;
	private static final int LIFE = 80;
	private static final int BOG = 0x8CD84A;
	private static final EntityDataAccessor<Boolean> DATA_ENGORGED = SynchedEntityData.defineId(BogBubble.class, EntityDataSerializers.BOOLEAN);

	private int life;

	public BogBubble(EntityType<? extends BogBubble> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ENGORGED, false);
	}

	/** A bubble from a frog that has just eaten: half again as wide, and its poison stronger. */
	public void setEngorged(boolean engorged) {
		entityData.set(DATA_ENGORGED, engorged);
	}

	public boolean engorged() {
		return entityData.get(DATA_ENGORGED);
	}

	@Override
	protected double getDefaultGravity() {
		return MonsterRules.BUBBLE_GRAVITY;
	}

	@Override
	protected float getAirDrag() {
		// No drag: the frog aims its lob for exactly this flight (MonsterRules.lob).
		return 1.0F;
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 movement = getDeltaMovement();
		if (!level().isClientSide()) {
			HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
			if (hit.getType() != HitResult.Type.MISS) {
				hitTargetOrDeflectSelf(hit);
				if (isRemoved()) {
					return;
				}
			}
			if (++life > LIFE || isInWater()) {
				burst((ServerLevel) level(), position());
				return;
			}
		} else if (tickCount % 2 == 0) {
			level().addParticle(ParticleTypes.ITEM_SLIME, getX(), getY(), getZ(), 0, 0, 0);
		}
		setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
		setDeltaMovement(movement.scale(getAirDrag()));
		applyGravity();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// Never its own frog, nor another monster: it bursts on what the frog hunts.
		Entity owner = getOwner();
		return super.canHitEntity(entity) && entity != owner && owner instanceof LivingEntity frog && Targets.canHarm(frog, entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (level() instanceof ServerLevel level) {
			burst(level, result.getEntity().position().add(0, result.getEntity().getBbHeight() * 0.5, 0));
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (level() instanceof ServerLevel level) {
			burst(level, result.getLocation());
		}
	}

	/** It bursts: poison over everything within reach, a little lingering mist, and a wet pop. */
	public void burst(ServerLevel level, Vec3 at) {
		if (isRemoved()) {
			return;
		}
		boolean big = engorged();
		double radius = RADIUS * (big ? 1.5 : 1.0);
		Entity owner = getOwner();
		if (owner instanceof Mob frog) {
			for (Entity e : level.getEntities(this, new AABB(at, at).inflate(radius, radius, radius), e -> Targets.canHarm(frog, e))) {
				LivingEntity t = (LivingEntity) e;
				if (t.getBoundingBox().getCenter().distanceTo(at) > radius + t.getBbWidth() * 0.5) {
					continue;
				}
				MonsterMagic.hurt(level, frog, t, DAMAGE * (big ? 1.5F : 1.0F));
				t.addEffect(new MobEffectInstance(MobEffects.POISON, POISON, big ? 1 : 0), frog);
			}
			AreaEffectCloud mist = new AreaEffectCloud(level, at.x, at.y - 0.3, at.z);
			mist.setOwner(frog);
			mist.setRadius((float) radius * 0.7F);
			mist.setRadiusOnUse(-0.3F);
			mist.setWaitTime(10);
			mist.setDuration(60);
			mist.setRadiusPerTick(-mist.getRadius() / 60.0F);
			mist.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
			level.addFreshEntity(mist);
		}
		pop(level, at, radius);
		discard();
	}

	/** The bubble's skin giving way: a splash of green, glowing motes and a wet pop. */
	private void pop(ServerLevel level, Vec3 at, double radius) {
		level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y, at.z, 14, radius * 0.4, 0.3, radius * 0.4, 0.15);
		level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 12, radius * 0.4, 0.2, radius * 0.4, 0.1);
		Motes.burst(level, at, 10, BOG, 0.14, 18, 0.1);
		MonsterMagic.sound(level, at, "monster_bubble_pop", 1.0F, engorged() ? 0.8F : 1.0F);
	}

	// ------------------------------------------------------------------ popped in the air

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public float getPickRadius() {
		return 0.2F;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Struck before it lands (an arrow, a snowball, a blade): it pops where it is, its poison spilled for nothing.
		if (!isRemoved()) {
			pop(level, position(), RADIUS * 0.6);
			discard();
		}
		return true;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("engorged", engorged());
		output.putInt("life", life);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setEngorged(input.getBooleanOr("engorged", false));
		life = input.getIntOr("life", 0);
	}
}
