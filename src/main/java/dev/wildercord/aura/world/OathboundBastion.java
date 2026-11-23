package dev.wildercord.aura.world;

import dev.wildercord.cast.Light;
import dev.wildercord.cast.MoveDeck;
import dev.wildercord.cast.Sigils;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.compat.McEntities;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.monster.MonsterMagic;
import dev.wildercord.world.sites.oathhold.OathholdLaw;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Oathbound Bastion — Knight #1 (Way of the Bulwark), an unbroken champion knight clad in obsidian-iron and gold plate,
 * fur mantle, navy quilted tabard, crowned greathelm, carrying a towering Sun-Anvil Greatshield and radiant Flanged Greatmace.
 *
 * <p>All timings, damage numbers, and shield geometry live in {@link OathboundBastionRules}.</p>
 */
public final class OathboundBastion extends PathfinderMob implements Enemy {
	private static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(OathboundBastion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> BEGAN = SynchedEntityData.defineId(OathboundBastion.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Integer> TELL = SynchedEntityData.defineId(OathboundBastion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> BARRIER = SynchedEntityData.defineId(OathboundBastion.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(OathboundBastion.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> PREVIEW_ELAPSED = SynchedEntityData.defineId(OathboundBastion.class, EntityDataSerializers.FLOAT);

	private static final int HEX_GOLD = 0xFFD048;
	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** Client-side PoseBlend interpolation state (same convention as DrownedBellWarden). */
	public int shownMove = -1, prevMove = -1, shownTell, prevTell;
	public float shownElapsed, prevElapsed, switchedAt = -100.0F;

	private final MoveDeck deck = new MoveDeck(OathboundBastionRules.MEMORY);
	private final long[] lastUsed = new long[32];
	private final Set<UUID> struck = new HashSet<>();

	private float aim;
	private int moveTicksLeft;
	private int gapTicks = 10;
	private boolean anvilHitFired;
	private boolean bashHitFired;
	private boolean cleaveHitFired;
	private boolean techHitFired;
	private int techHitCount;
	private long recentDamageWindowEnd;
	private float recentDamage;

	{
		Arrays.fill(lastUsed, -9999L);
	}

	public OathboundBastion(EntityType<? extends OathboundBastion> type, Level level) {
		super(type, level);
		this.xpReward = OathboundBastionRules.XP_REWARD;
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, OathboundBastionRules.MAX_HEALTH)
			.add(Attributes.ARMOR, OathboundBastionRules.ARMOR)
			.add(Attributes.ARMOR_TOUGHNESS, OathboundBastionRules.ARMOR_TOUGHNESS)
			.add(Attributes.KNOCKBACK_RESISTANCE, OathboundBastionRules.KNOCKBACK_RESISTANCE)
			.add(Attributes.MOVEMENT_SPEED, OathboundBastionRules.MOVEMENT_SPEED)
			.add(Attributes.FOLLOW_RANGE, OathboundBastionRules.FOLLOW_RANGE)
			.add(Attributes.ATTACK_DAMAGE, 12.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MOVE, OathboundBastionRules.IDLE_MARCH);
		builder.define(BEGAN, 0L);
		builder.define(TELL, 0);
		builder.define(BARRIER, 0.15F);
		builder.define(EMPOWERED, false);
		builder.define(PREVIEW_ELAPSED, -1.0F);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.72) {
			@Override
			public boolean canUse() {
				return move() == OathboundBastionRules.IDLE_MARCH && getTarget() == null && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));

		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new OathholdLaw.LawEnforcementTargetGoal(this));
		targetSelector.addGoal(3, new OathholdLaw.CityMonsterDefenseGoal(this));
	}

	public int move() {
		return entityData.get(MOVE);
	}

	public int tell() {
		return entityData.get(TELL);
	}

	public float barrier() {
		return entityData.get(BARRIER);
	}

	public boolean empowered() {
		return entityData.get(EMPOWERED);
	}

	public float elapsed(float partialTick) {
		float preview = entityData.get(PREVIEW_ELAPSED);
		if (preview >= 0.0F) {
			return preview;
		}
		return (float) (level().getGameTime() - entityData.get(BEGAN)) + partialTick;
	}

	private long since() {
		return Math.max(0L, level().getGameTime() - entityData.get(BEGAN));
	}

	/** Holds an exact animation keyframe pose for studio/gametest screenshots while {@code NoAI} is enabled. */
	public void poseForPreview(int move, int tellTicks, float fixedElapsed, float barrier) {
		entityData.set(MOVE, move);
		entityData.set(TELL, tellTicks);
		entityData.set(BEGAN, level().getGameTime() - (long) fixedElapsed);
		entityData.set(PREVIEW_ELAPSED, fixedElapsed);
		entityData.set(BARRIER, barrier);
		moveTicksLeft = Math.max(0, OathboundBastionRules.moveDuration(move, empowered()) - (int) fixedElapsed);
		aim = getYRot();
	}

	/** Starts a live combat move from AI or test harnesses. */
	public void startMove(ServerLevel level, int nextMove, @Nullable LivingEntity target) {
		boolean emp = empowered();
		int tell = OathboundBastionRules.tellTicks(nextMove, emp);
		int duration = nextMove == OathboundBastionRules.STAGGERED
			? OathboundBastionRules.STAGGER_AXE_TICKS
			: OathboundBastionRules.moveDuration(nextMove, emp);
		startMoveWithDuration(level, nextMove, tell, duration, target);
	}

	private void startMoveWithDuration(ServerLevel level, int nextMove, int tell, int duration, @Nullable LivingEntity target) {
		entityData.set(PREVIEW_ELAPSED, -1.0F);
		entityData.set(MOVE, nextMove);
		entityData.set(TELL, tell);
		entityData.set(BEGAN, level.getGameTime());
		moveTicksLeft = duration;
		anvilHitFired = false;
		bashHitFired = false;
		cleaveHitFired = false;
		techHitFired = false;
		techHitCount = 0;
		struck.clear();
		getNavigation().stop();

		if (target != null) {
			aim = yawTo(target.position());
			face(aim);
		} else {
			aim = getYRot();
		}

		if (nextMove != OathboundBastionRules.IDLE_MARCH && nextMove != OathboundBastionRules.STAGGERED) {
			lastUsed[nextMove] = level.getGameTime();
			deck.played(nextMove);
		}

		Vec3 chest = chestPos();
		switch (nextMove) {
			case OathboundBastionRules.PHALANX_BRACE -> {
				entityData.set(BARRIER, 1.0F);
				Feels.sound(level, chest, "aura_bell_warden_shell", 1.1F, 1.15F);
				sound(level, chest, SoundEvents.SHIELD_BLOCK.value(), 1.0F, 0.8F);
			}
			case OathboundBastionRules.SHIELD_RAM -> {
				entityData.set(BARRIER, 0.85F);
				Feels.sound(level, chest, "aura_bell_warden_charge", 1.25F, 1.05F);
			}
			case OathboundBastionRules.ANVIL_SLAM -> {
				entityData.set(BARRIER, 0.35F);
				Feels.sound(level, chest, "duelist_knight_windup", 1.2F, 0.78F);
				Light.groundRing(level, position(), HEX_GOLD, 0.4, 2.8, 0.08, tell);
			}
			case OathboundBastionRules.SHIELD_BASH -> {
				entityData.set(BARRIER, 0.75F);
				Feels.sound(level, chest, "duelist_knight_windup", 1.0F, 0.95F);
			}
			case OathboundBastionRules.MACE_CLEAVE -> {
				entityData.set(BARRIER, 0.25F);
				Feels.sound(level, chest, "duelist_knight_windup", 1.1F, 0.88F);
			}
			case OathboundBastionRules.STAGGERED -> {
				entityData.set(BARRIER, 0.0F);
				sound(level, chest, SoundEvents.SHIELD_BREAK.value(), 1.3F, 0.85F);
			}
			default -> {
				OathboundTechnique tech = OathboundBastionRules.technique(nextMove);
				if (tech != null) {
					entityData.set(BARRIER, tech.archetype() == OathboundTechnique.Archetype.GUARD_BRACE ? 0.85F : 0.30F);
					Feels.sound(level, chest, "duelist_knight_windup", 1.2F, 0.9F);
				} else {
					entityData.set(BARRIER, empowered() ? 0.35F : 0.18F);
				}
			}
		}
	}

	private void enterStagger(ServerLevel level, int ticks) {
		startMoveWithDuration(level, OathboundBastionRules.STAGGERED, 0, ticks, null);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!isAlive()) {
			return;
		}
		if (entityData.get(PREVIEW_ELAPSED) >= 0.0F) {
			entityData.set(PREVIEW_ELAPSED, -1.0F);
		}

		boolean nowEmpowered = OathboundBastionRules.empowered(getHealth(), getMaxHealth());
		if (nowEmpowered != empowered()) {
			entityData.set(EMPOWERED, nowEmpowered);
			if (nowEmpowered) {
				Feels.sound(level, chestPos(), "aura_bell_warden_knell", 1.3F, 1.25F);
				Light.groundRing(level, position(), HEX_GOLD, 0.3, 3.6, 0.12, 12);
			}
		}

		if (level.getGameTime() > recentDamageWindowEnd) {
			recentDamage = 0.0F;
		}

		LivingEntity target = getTarget();
		if (target != null && !target.isAlive()) {
			setTarget(null);
			target = null;
		}

		switch (move()) {
			case OathboundBastionRules.PHALANX_BRACE -> tickPhalanx(level, target);
			case OathboundBastionRules.SHIELD_RAM -> tickShieldRam(level, target);
			case OathboundBastionRules.ANVIL_SLAM -> tickAnvilSlam(level, target);
			case OathboundBastionRules.SHIELD_BASH -> tickShieldBash(level, target);
			case OathboundBastionRules.MACE_CLEAVE -> tickMaceCleave(level, target);
			case OathboundBastionRules.STAGGERED -> tickStaggered(level);
			default -> {
				if (OathboundBastionRules.technique(move()) != null) {
					tickTechnique(level, target);
				} else {
					tickIdleMarch(level, target);
				}
			}
		}
	}

	private void tickIdleMarch(ServerLevel level, @Nullable LivingEntity target) {
		entityData.set(BARRIER, empowered() ? 0.35F : 0.18F);
		if (getDeltaMovement().horizontalDistanceSqr() > 0.0005 && tickCount % 16 == 0) {
			Feels.sound(level, position(), "duelist_knight_step", 0.9F, 0.72F + random.nextFloat() * 0.1F);
		}
		if (target == null) {
			return;
		}
		getLookControl().setLookAt(target, 30.0F, 30.0F);
		double dist = distanceTo(target);
		if (dist > 2.6) {
			if (!getNavigation().moveTo(target, 1.05)) {
				getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			}
		} else {
			getNavigation().stop();
			turnToward(yawTo(target.position()), 12.0F);
		}

		if (gapTicks > 0) {
			gapTicks--;
			return;
		}

		int next = pickNextMove(level, target, dist);
		if (next > 0) {
			startMove(level, next, target);
		}
	}

	private int pickNextMove(ServerLevel level, LivingEntity target, double dist) {
		long now = level.getGameTime();
		boolean emp = empowered();
		double[] weights = new double[OathboundBastionRules.COMBAT_MOVES.length];
		for (int i = 0; i < OathboundBastionRules.COMBAT_MOVES.length; i++) {
			int m = OathboundBastionRules.COMBAT_MOVES[i];
			long sinceLast = now - lastUsed[m];
			weights[i] = switch (m) {
				case OathboundBastionRules.PHALANX_BRACE ->
					(dist <= 9.0 && sinceLast >= 90 && (recentDamage >= 12.0F || dist >= 4.2)) ? 1.8 : 0.0;
				case OathboundBastionRules.SHIELD_RAM ->
					(dist >= 5.0 && dist <= 14.0 && sinceLast >= (emp ? 70 : 100) && hasLineOfSight(target)) ? 2.4 : 0.0;
				case OathboundBastionRules.ANVIL_SLAM ->
					(dist <= 5.5 && sinceLast >= (emp ? 75 : 105)) ? 2.2 : 0.0;
				case OathboundBastionRules.SHIELD_BASH ->
					(dist <= 3.7 && sinceLast >= (emp ? 45 : 65)) ? 2.5 : 0.0;
				case OathboundBastionRules.MACE_CLEAVE ->
					(dist <= 4.0 && sinceLast >= (emp ? 40 : 55)) ? 2.6 : 0.0;
				default -> {
					OathboundTechnique tech = OathboundBastionRules.technique(m);
					if (tech == null || (tech.empoweredOnly() && !emp) || !tech.inRange(dist)
							|| sinceLast < (emp ? 26 : 38)) {
						yield 0.0;
					}
					yield 1.5 + random.nextDouble() * 0.5;
				}
			};
		}
		return deck.pick(OathboundBastionRules.COMBAT_MOVES, weights, random.nextDouble());
	}

	private void tickPhalanx(ServerLevel level, @Nullable LivingEntity target) {
		still();
		if (target != null) {
			turnToward(yawTo(target.position()), 8.0F);
			aim = getYRot();
		}
		entityData.set(BARRIER, 1.0F);
		if (since() % 6 == 0) {
			Vec3 front = chestPos().add(ahead(getYRot()).scale(1.15));
			Sigils.layer(level, front, ahead(getYRot()), SigilOption.RING, HEX_GOLD, 0.9F, 8, 0.1F);
			level.sendParticles(ParticleTypes.WAX_ON, front.x, front.y, front.z, 2, 0.35, 0.45, 0.35, 0.02);
		}
		if (--moveTicksLeft <= 0) {
			finishMove(level, OathboundBastionRules.PHALANX_BRACE, target);
		}
	}

	private void tickShieldRam(ServerLevel level, @Nullable LivingEntity target) {
		long el = since();
		int tell = tell();
		if (el < tell) {
			still();
			if (target != null && el <= tell - 3) {
				turnToward(yawTo(target.position()), 14.0F);
				aim = getYRot();
			}
			face(aim);
			if (el % 3 == 0) {
				level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 0.3, getZ(), 4, 0.4, 0.15, 0.4, 0.04);
				dust(level, position(), 5, 0.35);
			}
			moveTicksLeft--;
			return;
		}

		face(aim);
		Vec3 forward = ahead(aim);
		if (el >= tell + OathboundBastionRules.RAM_CHARGE_TICKS) {
			still();
			if (--moveTicksLeft <= 0) {
				finishMove(level, OathboundBastionRules.SHIELD_RAM, target);
			}
			return;
		}
		move(MoverType.SELF, new Vec3(forward.x * OathboundBastionRules.RAM_SPEED, -0.15, forward.z * OathboundBastionRules.RAM_SPEED));
		setDeltaMovement(Vec3.ZERO);

		if (tickCount % 2 == 0) {
			Feels.sound(level, position(), "duelist_knight_step", 1.1F, 1.05F);
			dust(level, position().subtract(forward.scale(0.8)), 7, 0.4);
			level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 0.4, getZ(), 3, 0.3, 0.2, 0.3, 0.05);
		}

		double dmg = OathboundBastionRules.scaleDamage(OathboundBastionRules.RAM_DAMAGE, empowered());
		Vec3 impactPoint = position().add(forward.scale(1.1));
		for (LivingEntity foe : targetsIn(level, impactPoint, OathboundBastionRules.RAM_HIT_RADIUS + 0.7)) {
			if (!struck.add(foe.getUUID())) {
				continue;
			}
			landBlow(level, foe, dmg, OathboundBastionRules.RAM_KNOCKBACK, position().subtract(forward), 0.35);
		}

		if (horizontalCollision || wallAhead(level, forward)) {
			sound(level, chestPos(), SoundEvents.ANVIL_LAND, 1.4F, 0.72F);
			dust(level, position().add(forward.scale(1.2)), 20, 0.6);
			level.sendParticles(ParticleTypes.CRIT, chestPos().x, chestPos().y, chestPos().z, 12, 0.5, 0.4, 0.5, 0.25);
			enterStagger(level, OathboundBastionRules.STAGGER_WALL_TICKS);
			return;
		}

		if (--moveTicksLeft <= 0) {
			finishMove(level, OathboundBastionRules.SHIELD_RAM, target);
		}
	}

	private boolean wallAhead(ServerLevel level, Vec3 forward) {
		Vec3 probe = position().add(forward.scale(getBbWidth() * 0.55 + 0.45)).add(0, 1.1, 0);
		BlockPos bp = BlockPos.containing(probe);
		BlockState state = level.getBlockState(bp);
		return !state.getCollisionShape(level, bp).isEmpty();
	}

	private void tickAnvilSlam(ServerLevel level, @Nullable LivingEntity target) {
		still();
		long el = since();
		int tell = tell();
		if (el < tell) {
			if (target != null && el <= tell - 5) {
				turnToward(yawTo(target.position()), 12.0F);
				aim = getYRot();
			}
			face(aim);
			if (el % 4 == 0) {
				Sigils.layer(level, position().add(0, 0.06, 0), UP, SigilOption.RING, HEX_GOLD, 2.4F, 6, 0.08F);
				level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 2.6, getZ(), 3, 0.4, 0.3, 0.4, 0.04);
			}
			moveTicksLeft--;
			return;
		}

		face(aim);
		if (!anvilHitFired) {
			anvilHitFired = true;
			Vec3 impact = position().add(ahead(aim).scale(1.2));
			Feels.sound(level, impact, "aura_bell_warden_hammer", 1.4F, 0.9F);
			sound(level, impact, SoundEvents.ANVIL_LAND, 1.4F, 0.75F);
			sound(level, impact, SoundEvents.MACE_SMASH_GROUND, 1.2F, 0.8F);
			Light.groundRing(level, position(), HEX_GOLD, 0.5, OathboundBastionRules.ANVIL_SHOCKWAVE_RADIUS, 0.16, 14);
			Sigils.layer(level, position().add(0, 0.08, 0), UP, SigilOption.CIRCLE, HEX_GOLD, 3.0F, 12, 0.1F);
			dust(level, position(), 28, 1.5);
			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI * 2.0 / 16.0;
				double r = 2.6 + (i % 2) * 1.4;
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floorState(level, position())),
					getX() + Math.cos(a) * r, getY() + 0.15, getZ() + Math.sin(a) * r, 3, 0.15, 0.25, 0.15, 0.18);
			}

			boolean emp = empowered();
			for (LivingEntity foe : targetsIn(level, position(), OathboundBastionRules.ANVIL_SHOCKWAVE_RADIUS + 0.6)) {
				double dist = Math.hypot(foe.getX() - getX(), foe.getZ() - getZ());
				if (Math.abs(foe.getY() - getY()) > 2.4) {
					continue;
				}
				double dmg = OathboundBastionRules.anvilShockwaveDamage(dist, emp);
				if (dmg > 0.0) {
					landBlow(level, foe, dmg, 1.15, position(), OathboundBastionRules.ANVIL_LIFT);
				}
			}
		}

		if (--moveTicksLeft <= 0) {
			finishMove(level, OathboundBastionRules.ANVIL_SLAM, target);
		}
	}

	private void tickShieldBash(ServerLevel level, @Nullable LivingEntity target) {
		still();
		long el = since();
		int tell = tell();
		if (el < tell) {
			if (target != null && el <= tell - 3) {
				turnToward(yawTo(target.position()), 14.0F);
				aim = getYRot();
			}
			face(aim);
			moveTicksLeft--;
			return;
		}

		face(aim);
		if (!bashHitFired) {
			bashHitFired = true;
			Vec3 forward = ahead(aim);
			Feels.sound(level, chestPos(), "aura_bell_warden_swing", 1.25F, 1.0F);
			Feels.sound(level, chestPos(), "duelist_clash", 1.1F, 0.85F);
			Light.slash(level, position().add(0, 1.35, 0), UP, forward, HEX_GOLD, 3.2, 2.35, 0.18, 5, 9);
			double dmg = OathboundBastionRules.scaleDamage(OathboundBastionRules.BASH_DAMAGE, empowered());
			for (LivingEntity foe : targetsIn(level, position(), OathboundBastionRules.BASH_REACH + 0.6)) {
				double dist = Math.hypot(foe.getX() - getX(), foe.getZ() - getZ());
				double relYaw = yawTo(foe.position()) - getYRot();
				if (OathboundBastionRules.inCone(dist, foe.getY() - getY(), relYaw, OathboundBastionRules.BASH_REACH, OathboundBastionRules.BASH_HALF_ANGLE_DEG)) {
					if (landBlow(level, foe, dmg, OathboundBastionRules.BASH_KNOCKBACK, position(), 0.25)) {
						foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, OathboundBastionRules.BASH_SLOW_TICKS, 1, false, true), this);
					}
				}
			}
		}

		if (--moveTicksLeft <= 0) {
			finishMove(level, OathboundBastionRules.SHIELD_BASH, target);
		}
	}

	private void tickMaceCleave(ServerLevel level, @Nullable LivingEntity target) {
		still();
		long el = since();
		int tell = tell();
		if (el < tell) {
			if (target != null && el <= tell - 3) {
				turnToward(yawTo(target.position()), 14.0F);
				aim = getYRot();
			}
			face(aim);
			moveTicksLeft--;
			return;
		}

		face(aim);
		if (!cleaveHitFired) {
			cleaveHitFired = true;
			Vec3 forward = ahead(aim);
			Feels.sound(level, chestPos(), "aura_bell_warden_slam", 1.15F, 1.15F);
			Light.slash(level, position().add(0, 1.45, 0), UP, forward, 0xFFAC28, 3.4, 1.85, 0.16, 4, 8);
			Vec3 arc = position().add(forward.scale(2.0)).add(0, 1.2, 0);
			level.sendParticles(ParticleTypes.FLAME, arc.x, arc.y, arc.z, 10, 0.5, 0.35, 0.5, 0.04);
			double dmg = OathboundBastionRules.scaleDamage(OathboundBastionRules.CLEAVE_DAMAGE, empowered());
			for (LivingEntity foe : targetsIn(level, position(), OathboundBastionRules.CLEAVE_REACH + 0.6)) {
				double dist = Math.hypot(foe.getX() - getX(), foe.getZ() - getZ());
				double relYaw = yawTo(foe.position()) - getYRot();
				if (OathboundBastionRules.inCone(dist, foe.getY() - getY(), relYaw, OathboundBastionRules.CLEAVE_REACH, OathboundBastionRules.CLEAVE_HALF_ANGLE_DEG)) {
					landBlow(level, foe, dmg, OathboundBastionRules.CLEAVE_KNOCKBACK, position(), 0.18);
				}
			}
		}

		if (--moveTicksLeft <= 0) {
			finishMove(level, OathboundBastionRules.MACE_CLEAVE, target);
		}
	}

	private void tickTechnique(ServerLevel level, @Nullable LivingEntity target) {
		OathboundTechnique tech = OathboundBastionRules.technique(move());
		if (tech == null) {
			tickIdleMarch(level, target);
			return;
		}
		long el = since();
		int tell = tell();
		boolean emp = empowered();
		if (el < tell) {
			still();
			if (target != null && el <= tell - 3) {
				aim = Mth.rotLerp(0.22F, aim, yawTo(target.position()));
			}
			face(aim);
			if (el % 4 == 0) {
				OathboundCombat.windupFx(level, this, tech, aim, el);
			}
			moveTicksLeft--;
			return;
		}
		face(aim);
		long active = el - tell;
		OathboundCombat.motion(this, tech, aim, active);
		if (tech.motion() == OathboundTechnique.Motion.PLANTED) {
			still();
		}
		if (!techHitFired && active >= tech.hitDelay()) {
			techHitFired = true;
			techHitCount = 1;
			OathboundCombat.strike(level, this, tech, aim, struck, emp, OathboundBastionRules.PIERCE);
			OathboundCombat.strikeFx(level, this, tech, aim);
		} else if (tech.shape() == OathboundTechnique.Shape.MULTI_HIT && techHitCount < tech.hits()
				&& active >= tech.hitDelay() + techHitCount * 4L) {
			techHitCount++;
			OathboundCombat.strike(level, this, tech, aim, struck, emp, OathboundBastionRules.PIERCE);
		}
		if (tech.motion() == OathboundTechnique.Motion.DASH && active % 2 == 0) {
			dust(level, position(), 4, 0.3);
		}
		if (--moveTicksLeft <= 0) {
			finishMove(level, move(), target);
		}
	}

	private void tickStaggered(ServerLevel level) {
		still();
		entityData.set(BARRIER, 0.0F);
		if (moveTicksLeft % 6 == 0) {
			Vec3 top = chestPos().add(0, 1.1, 0);
			level.sendParticles(ParticleTypes.CRIT, top.x, top.y, top.z, 3, 0.35, 0.15, 0.35, 0.05);
		}
		if (--moveTicksLeft <= 0) {
			startMove(level, OathboundBastionRules.IDLE_MARCH, getTarget());
			gapTicks = 8;
		}
	}

	private void finishMove(ServerLevel level, int completedMove, @Nullable LivingEntity target) {
		// Below 50% HP, Shield-Bash occasionally chains straight into Anvil Slam.
		if (empowered() && completedMove == OathboundBastionRules.SHIELD_BASH && target != null
			&& target.isAlive() && distanceTo(target) <= 5.5 && random.nextDouble() < 0.45) {
			startMove(level, OathboundBastionRules.ANVIL_SLAM, target);
			return;
		}
		startMove(level, OathboundBastionRules.IDLE_MARCH, target);
		gapTicks = (empowered() ? OathboundBastionRules.MOVE_GAP_EMPOWERED : OathboundBastionRules.MOVE_GAP_TICKS) + random.nextInt(5);
	}

	@Override
	public ProjectileDeflection deflection(Projectile projectile) {
		if (!(level() instanceof ServerLevel server)) {
			return super.deflection(projectile);
		}
		boolean frontal = OathboundBastionRules.frontal(yawTo(projectile.position()) - getYRot());
		if (!OathboundBastionRules.deflectsProjectile(move(), frontal)) {
			return super.deflection(projectile);
		}
		Entity shooter = projectile.getOwner();
		double speed = Math.min(2.6, Math.max(0.9, projectile.getDeltaMovement().length()) * 1.15);
		Feels.sound(server, chestPos(), "aura_bell_warden_ring", 1.2F, 1.15F);
		sound(server, chestPos(), SoundEvents.SHIELD_BLOCK.value(), 1.1F, 1.1F);
		server.sendParticles(ParticleTypes.WAX_ON, chestPos().x, chestPos().y, chestPos().z, 10, 0.4, 0.4, 0.4, 0.08);
		return McEntities.projectileDeflection((turned, by, rng) -> {
			Vec3 back;
			if (shooter != null && shooter.isAlive() && shooter.level() == turned.level()) {
				back = shooter.getBoundingBox().getCenter().subtract(turned.position());
			} else {
				back = ahead(getYRot());
			}
			if (back.lengthSqr() < 1.0E-4) {
				back = ahead(getYRot());
			}
			turned.setOwner(this);
			turned.setDeltaMovement(back.normalize().scale(speed));
			McEntities.syncVelocity(turned);
		});
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (damage <= 0 || source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, damage);
		}
		if (source.is(DamageTypes.IN_WALL)) {
			return false;
		}

		Vec3 srcPos = source.getSourcePosition();
		if (srcPos == null && source.getDirectEntity() != null) {
			srcPos = source.getDirectEntity().position();
		}
		boolean frontal = srcPos != null && OathboundBastionRules.frontal(yawTo(srcPos) - getYRot());
		int curMove = move();

		// Belt-and-suspenders projectile block during Phalanx Brace for direct hurt calls.
		if (source.getDirectEntity() instanceof Projectile && OathboundBastionRules.deflectsProjectile(curMove, frontal)) {
			Feels.sound(level, chestPos(), "aura_bell_warden_ring", 1.1F, 1.15F);
			return false;
		}

		// Heavy axes shatter Phalanx Brace and stagger the Bastion.
		if (!source.is(DamageTypeTags.IS_EXPLOSION)
			&& source.getDirectEntity() instanceof LivingEntity attacker
			&& attacker.getMainHandItem().is(ItemTags.AXES)
			&& OathboundBastionRules.axeBreaksGuard(curMove, frontal)) {
			if (attacker instanceof Player player) {
				MonsterMagic.tell(player, Component.translatable("message.wildercord.aura_world.guard_broken", getDisplayName()).withColor(0xFFD54A));
			}
			enterStagger(level, OathboundBastionRules.STAGGER_AXE_TICKS);
			boolean brokenHit = super.hurtServer(level, source, damage);
			if (brokenHit) {
				level.sendParticles(ParticleTypes.CRIT, chestPos().x, chestPos().y, chestPos().z, 14, 0.5, 0.5, 0.5, 0.25);
			}
			return brokenHit;
		}

		boolean inAnvilRecovery = curMove == OathboundBastionRules.ANVIL_SLAM && since() >= tell();
		float scaled = damage * (float) OathboundBastionRules.damageMultiplier(curMove, frontal, inAnvilRecovery);
		if (curMove == OathboundBastionRules.PHALANX_BRACE && frontal) {
			sound(level, chestPos(), SoundEvents.SHIELD_BLOCK.value(), 1.0F, 0.9F);
			level.sendParticles(ParticleTypes.WAX_ON, chestPos().x, chestPos().y, chestPos().z, 6, 0.3, 0.4, 0.3, 0.05);
		}
		boolean hurt = super.hurtServer(level, source, scaled);
		if (hurt) {
			long now = level.getGameTime();
			recentDamage = (now <= recentDamageWindowEnd ? recentDamage : 0.0F) + scaled;
			recentDamageWindowEnd = now + 40L;
		}
		return hurt;
	}

	private boolean landBlow(ServerLevel level, LivingEntity foe, double damage, double shove, Vec3 from, double lift) {
		if (!GuardianBite.land(level, this, foe, (float) damage, OathboundBastionRules.PIERCE)) {
			return false;
		}
		if (shove > 0) {
			foe.knockback(shove, from.x - foe.getX(), from.z - foe.getZ(), level.damageSources().mobAttack(this), 0.0F);
		}
		if (lift > 0) {
			foe.setDeltaMovement(foe.getDeltaMovement().add(0.0, lift, 0.0));
		}
		McEntities.syncVelocity(foe);
		return true;
	}

	private List<LivingEntity> targetsIn(ServerLevel level, Vec3 center, double radius) {
		AABB box = new AABB(center, center).inflate(radius, 3.0, radius);
		LivingEntity currentTarget = getTarget();
		return level.getEntitiesOfClass(LivingEntity.class, box, e ->
			e.isAlive() && e != this && !(e instanceof OathboundBastion)
				&& (e instanceof Player p && !p.isCreative() && !p.isSpectator()
					|| e instanceof IronGolem
					|| (currentTarget != null && e == currentTarget)));
	}

	private Vec3 chestPos() {
		return position().add(0.0, getBbHeight() * 0.56, 0.0);
	}

	private void still() {
		getNavigation().stop();
		Vec3 v = getDeltaMovement();
		setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
	}

	private void dust(ServerLevel level, Vec3 at, int count, double spread) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floorState(level, at)),
			at.x, at.y + 0.1, at.z, count, spread, 0.1, spread, 0.15);
	}

	private BlockState floorState(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
		return state.isAir() ? Blocks.STONE_BRICKS.defaultBlockState() : state;
	}

	private void sound(ServerLevel level, Vec3 at, SoundEvent event, float vol, float pitch) {
		level.playSound(null, at.x, at.y, at.z, event, SoundSource.HOSTILE, vol, pitch);
	}

	private Vec3 ahead(float yaw) {
		double rad = Math.toRadians(yaw);
		return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
	}

	private float yawTo(Vec3 at) {
		return (float) Math.toDegrees(Math.atan2(-(at.x - getX()), at.z - getZ()));
	}

	private void face(float yaw) {
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	private void turnToward(float targetYaw, float maxStep) {
		face(getYRot() + Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -maxStep, maxStep));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return WildercordSounds.kit("duelist_knight_ambient");
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return WildercordSounds.kit("duelist_knight_hurt");
	}

	@Override
	protected SoundEvent getDeathSound() {
		return WildercordSounds.kit("duelist_knight_death");
	}

	@Override
	public float getVoicePitch() {
		return 0.78F + random.nextFloat() * 0.08F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putBoolean("bastion_empowered", empowered());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		boolean emp = in.getBooleanOr("bastion_empowered", false)
			|| OathboundBastionRules.empowered(getHealth(), getMaxHealth());
		entityData.set(EMPOWERED, emp);
		entityData.set(MOVE, OathboundBastionRules.IDLE_MARCH);
		entityData.set(TELL, 0);
		entityData.set(PREVIEW_ELAPSED, -1.0F);
		entityData.set(BARRIER, emp ? 0.35F : 0.18F);
	}
}
