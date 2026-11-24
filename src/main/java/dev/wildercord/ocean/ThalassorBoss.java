package dev.wildercord.ocean;

import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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

/**
 * Thalassor, Abyssal Sovereign: 14-segment serpentine trench leviathan guarding the Abyssal Trench Citadel.
 * Progresses through 3 distinct phases (Patrol, Maelstrom + Escort Summons, Sovereign Fury Swept Hydro-Beam)
 * and respawns via its arena's Sovereign Altar.
 */
public class ThalassorBoss extends Monster {
	private static final EntityDataAccessor<Float> JAW_OPEN = SynchedEntityData.defineId(ThalassorBoss.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> ATTACK_MODE = SynchedEntityData.defineId(ThalassorBoss.class, EntityDataSerializers.INT);

	private final ServerBossEvent bossEvent = new ServerBossEvent(getType().getDescription(),
		BossEvent.BossBarColor.BLUE,
		BossEvent.BossBarOverlay.NOTCHED_10
	);

	private BlockPos arenaCenter;
	private int actionCooldown = 40;
	private int activeAttackTicks;
	private int summonCooldown = 140;

	public ThalassorBoss(EntityType<? extends ThalassorBoss> type, Level level) {
		super(type, level);
		this.xpReward = 250;
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.85F, 1.0F, false);
		setPathfindingMalus(PathType.WATER, 0.0F);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, OceanRules.THALASSOR_MAX_HP)
			.add(Attributes.ARMOR, OceanRules.THALASSOR_ARMOR)
			.add(Attributes.ATTACK_DAMAGE, OceanRules.THALASSOR_MELEE_DAMAGE)
			.add(Attributes.MOVEMENT_SPEED, 0.36)
			.add(Attributes.FOLLOW_RANGE, 48.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(JAW_OPEN, 0.0f);
		builder.define(ATTACK_MODE, 0);
	}

	public float getJawOpen() {
		return entityData.get(JAW_OPEN);
	}

	public int getAttackMode() {
		return entityData.get(ATTACK_MODE);
	}

	public void setArenaCenter(BlockPos pos) {
		this.arenaCenter = pos;
	}

	public BlockPos getArenaCenter() {
		return arenaCenter;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
		goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.85, 40));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	/** 1.21.5 pins canBreatheUnderwater; a steady air supply keeps the tidefolk breathing. */
	@Override
	protected int decreaseAirSupply(int air) {
		return air;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public void travel(Vec3 input) {
		if (isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), input);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.92));
		} else {
			super.travel(input);
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	protected void customServerAiStep() {
		ServerLevel level = (ServerLevel) level();
		super.customServerAiStep();
		bossEvent.setProgress(getHealth() / getMaxHealth());

		if (arenaCenter != null && position().distanceToSqr(Vec3.atCenterOf(arenaCenter)) > 64.0 * 64.0) {
			Vec3 back = Vec3.atCenterOf(arenaCenter).add(0.0, 4.0, 0.0).subtract(position()).normalize().scale(0.5);
			setDeltaMovement(back);
		}

		int phase = OceanRules.thalassorPhase(getHealth(), getMaxHealth());
		if (actionCooldown > 0) actionCooldown--;
		if (summonCooldown > 0) summonCooldown--;

		LivingEntity target = getTarget();
		if (activeAttackTicks > 0) {
			activeAttackTicks--;
			int mode = getAttackMode();
			if (mode == 1) {
				entityData.set(JAW_OPEN, 1.0f);
				if (target != null && target.isAlive() && activeAttackTicks % 8 == 0) {
					fireHydroBeam(server, target);
				}
			} else if (mode == 2 && activeAttackTicks % 6 == 0) {
				pulseMaelstromVortex(server);
			}
			if (activeAttackTicks == 0) {
				entityData.set(ATTACK_MODE, 0);
				entityData.set(JAW_OPEN, 0.0f);
			}
			return;
		}

		if (target == null || !target.isAlive()) return;

		if (phase >= 2 && summonCooldown <= 0) {
			summonCooldown = phase == 3 ? 220 : 300;
			int existing = server.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(32.0),
				e -> e instanceof SirenVeilJelly || e instanceof CoralSpineMako).size();
			if (existing < 4) {
				for (int i = 0; i < 2; i++) {
					Monster minion = dev.wildercord.compat.McEntities.create(i == 0 ? OceanEntities.SIREN_VEIL_JELLY : OceanEntities.CORAL_SPINE_MAKO, server, MobSpawnType.MOB_SUMMONED);
					if (minion != null) {
						minion.setPos(getX() + (i == 0 ? 4.0 : -4.0), getY() + 1.0, getZ());
						server.addFreshEntity(minion);
					}
				}
			}
		}

		if (actionCooldown <= 0 && hasLineOfSight(target)) {
			if (phase >= 2 && random.nextInt(3) == 0) {
				entityData.set(ATTACK_MODE, 2);
				activeAttackTicks = 36;
				actionCooldown = phase == 3 ? 75 : 110;
				playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
			} else {
				entityData.set(ATTACK_MODE, 1);
				activeAttackTicks = phase == 3 ? 40 : 28;
				actionCooldown = phase == 3 ? 65 : 95;
				playSound(SoundEvents.GUARDIAN_ATTACK, 1.4f, 0.55f);
			}
		}
	}

	private void fireHydroBeam(ServerLevel server, LivingEntity target) {
		Vec3 from = getEyePosition();
		Vec3 to = target.getEyePosition();
		for (int i = 1; i <= 10; i++) {
			Vec3 pt = from.lerp(to, i / 10.0);
			server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 2, 0.12, 0.12, 0.12, 0.01);
		}
		if (from.distanceToSqr(to) <= 24.0 * 24.0) {
			float dmg = OceanRules.THALASSOR_BEAM_DAMAGE * OceanRules.abyssalDamageMultiplier(OceanItems.wornSuitPieces(target));
			target.hurt( server.damageSources().indirectMagic(this, this),  dmg);
		}
	}

	private void pulseMaelstromVortex(ServerLevel server) {
		for (Player p : server.getEntitiesOfClass(Player.class, getBoundingBox().inflate(OceanRules.THALASSOR_VORTEX_RADIUS))) {
			if (!p.isAlive() || p.isSpectator()) continue;
			double[] pull = OceanRules.computePullVector(p.getX(), p.getY(), p.getZ(), getX(), getY(), getZ(), 0.7);
			p.push(pull[0], 0.08, pull[2]);
			float dmg = 6.0f * OceanRules.abyssalDamageMultiplier(OceanItems.wornSuitPieces(p));
			p.hurt( server.damageSources().magic(),  dmg);
		}
		server.sendParticles(ParticleTypes.BUBBLE_POP, getX(), getY() + 1.0, getZ(), 20, 3.5, 1.0, 3.5, 0.12);
	}

	@Override
	public boolean doHurtTarget(ServerLevel server, Entity target) {
		boolean hit = super.doHurtTarget(server, target);
		if (hit && target instanceof LivingEntity living && OceanItems.wornSuitPieces(living) >= 4) {
			living.heal((float) (OceanRules.THALASSOR_MELEE_DAMAGE * OceanRules.FULL_SUIT_ABYSSAL_DAMAGE_REDUCTION * 0.5f));
		}
		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ELDER_GUARDIAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ELDER_GUARDIAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ELDER_GUARDIAN_DEATH;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag inputTag) { McValueInput input = new McValueInput(inputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.readAdditionalSaveData(inputTag);
		Optional<BlockPos> savedArena = input.read("ArenaCenter", BlockPos.CODEC);
		arenaCenter = savedArena.orElse(null);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag outputTag) { McValueOutput output = new McValueOutput(outputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.addAdditionalSaveData(outputTag);
		if (arenaCenter != null) {
			output.store("ArenaCenter", BlockPos.CODEC, arenaCenter);
		}
	}
}
