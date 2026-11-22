package dev.wildercord.wildlife;

import dev.wildercord.cast.Targets;
import dev.wildercord.monster.KarakuriSentinel;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.monster.PaperLanternSpirit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;

/**
 * The four Shrine Cats modeled after real companions:
 * <ul>
 *   <li>{@code Yuki}: fluffy grey/brown mackerel tabby with white bib &amp; long whiskers (Floor 1)</li>
 *   <li>{@code Odie}: fluffy orange tabby with sleepy squinted eyes &amp; pink nose (Floor 2)</li>
 *   <li>{@code Freya}: plush blue-grey shorthair wearing a pink collar &amp; silver bell (Floor 3)</li>
 *   <li>{@code Kuro}: sleek jet-black shorthair with golden eyes &amp; notched left ear (Floor 4)</li>
 * </ul>
 * Inside {@code CatSanctuaryPiece} each cat hosts a chill non-lethal guardian trial, bestows a sanctuary
 * floor buff upon yielding, and becomes tameable once the dungeon trial completes.
 */
public class ShrineCat extends TamableAnimal {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(ShrineCat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(ShrineCat.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_LOAFING = SynchedEntityData.defineId(ShrineCat.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_ACTION_TICK = SynchedEntityData.defineId(ShrineCat.class, EntityDataSerializers.INT);

	private final ServerBossEvent bossBar = new ServerBossEvent(
		Component.literal("Shrine Guardian"),
		BossEvent.BossBarColor.PINK,
		BossEvent.BossBarOverlay.NOTCHED_10
	);

	private  BlockPos homePos;
	private  BlockPos sanctuaryHeart;
	private boolean sanctuaryFinished;
	private boolean wave1Summoned;
	private boolean wave2Summoned;
	private int specialCooldown = 36;
	private int perkCooldown = 20;
	private int companionPowerCooldown = 80;
	private int quartetCheckCooldown = 60;
	private int earFlickTicks;

	public ShrineCat(EntityType<? extends ShrineCat> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, 0);
		builder.define(DATA_ROLE, ShrineCatRules.ROLE_VILLAGE);
		builder.define(DATA_LOAFING, false);
		builder.define(DATA_ACTION_TICK, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 80.0)
			.add(Attributes.ARMOR, 10.0)
			.add(Attributes.MOVEMENT_SPEED, 0.30)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.42F) {
			@Override
			public boolean canUse() {
				return !isOrderedToSit() && !isLoafing() && super.canUse();
			}
		});
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				int r = getRole();
				return !isOrderedToSit() && !isLoafing()
					&& (r == ShrineCatRules.ROLE_GUARDIAN || r == ShrineCatRules.ROLE_TAMED) && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isOrderedToSit() && !isLoafing() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.05, 10.0F, 2.5F));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.78) {
			@Override
			public boolean canUse() {
				return !isLoafing() && super.canUse();
			}
		});
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return !isLoafing() && super.canUse();
			}
		});
		targetSelector.addGoal(2, new OwnerHurtTargetGoal(this) {
			@Override
			public boolean canUse() {
				return !isLoafing() && super.canUse();
			}
		});
		targetSelector.addGoal(3, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				int r = getRole();
				return !isOrderedToSit() && !isLoafing()
					&& (r == ShrineCatRules.ROLE_GUARDIAN || r == ShrineCatRules.ROLE_TAMED) && super.canUse();
			}
		});
		targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
			(target, lvl) -> !isOrderedToSit() && !isLoafing() && getRole() == ShrineCatRules.ROLE_TAMED && isTame()));
	}

	public ShrineCatRules.Variant variant() {
		return ShrineCatRules.Variant.byId(entityData.get(DATA_VARIANT));
	}

	public void setVariant(ShrineCatRules.Variant v) {
		entityData.set(DATA_VARIANT, v.id);
		setCustomName(Component.literal(v.displayName));
		setCustomNameVisible(true);
		refreshBossBarIdentity(v);
	}

	public int getRole() {
		return entityData.get(DATA_ROLE);
	}

	public void setRole(int role) {
		entityData.set(DATA_ROLE, role);
	}

	public boolean isLoafing() {
		return entityData.get(DATA_LOAFING);
	}

	public void setLoafing(boolean loafing) {
		entityData.set(DATA_LOAFING, loafing);
	}

	public int actionTick() {
		return entityData.get(DATA_ACTION_TICK);
	}

	private void triggerAction() {
		entityData.set(DATA_ACTION_TICK, 20);
	}

	public int earFlickTicks() {
		return earFlickTicks;
	}

	/** Configures this cat as a floor guardian inside CatSanctuaryPiece. */
	public void configureGuardian(ShrineCatRules.Variant v, BlockPos home, BlockPos floor4Heart) {
		setVariant(v);
		setRole(ShrineCatRules.ROLE_GUARDIAN);
		this.homePos = home.immutable();
		this.sanctuaryHeart = floor4Heart.immutable();
		this.sanctuaryFinished = false;
		this.wave1Summoned = false;
		this.wave2Summoned = false;
		setPersistenceRequired();
		var maxHp = getAttribute(Attributes.MAX_HEALTH);
		if (maxHp != null) {
			maxHp.setBaseValue(v.trialHealth);
		}
		var atk = getAttribute(Attributes.ATTACK_DAMAGE);
		if (atk != null) {
			atk.setBaseValue(v.baseDamage);
		}
		var armor = getAttribute(Attributes.ARMOR);
		if (armor != null) {
			armor.setBaseValue(8.0);
		}
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(0.34);
		}
		setHealth(getMaxHealth());
		setLoafing(v == ShrineCatRules.Variant.ODIE);
		setOrderedToSit(v == ShrineCatRules.Variant.FREYA);
		setInSittingPose(v == ShrineCatRules.Variant.FREYA);
	}

	/** Configures this cat as a peaceful inhabitant of the Japanese Sakura Village. */
	public void configureVillage(ShrineCatRules.Variant v, BlockPos home) {
		setVariant(v);
		setRole(ShrineCatRules.ROLE_VILLAGE);
		this.homePos = home.immutable();
		this.sanctuaryFinished = true;
		setPersistenceRequired();
		setLoafing(v == ShrineCatRules.Variant.ODIE);
		setOrderedToSit(v == ShrineCatRules.Variant.FREYA);
		setInSittingPose(v == ShrineCatRules.Variant.FREYA);
	}

	private void refreshBossBarIdentity(ShrineCatRules.Variant v) {
		bossBar.setName(Component.literal(v.title));
		bossBar.setColor(switch (v) {
			case YUKI -> BossEvent.BossBarColor.WHITE;
			case ODIE -> BossEvent.BossBarColor.YELLOW;
			case FREYA -> BossEvent.BossBarColor.PINK;
			case KURO -> BossEvent.BossBarColor.PURPLE;
		});
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,  SpawnGroupData data) {
		SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
		if (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) {
			if (entityData.get(DATA_VARIANT) == 0 && !hasCustomName()) {
				setVariant(ShrineCatRules.Variant.byId(random.nextInt(4)));
			} else {
				setVariant(variant());
			}
			sanctuaryFinished = true;
		}
		return out;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			if (earFlickTicks > 0) {
				earFlickTicks--;
			} else if (random.nextInt(90) == 0) {
				earFlickTicks = 8;
			}
			return;
		}
		if (actionTick() > 0) {
			entityData.set(DATA_ACTION_TICK, actionTick() - 1);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		int role = getRole();
		if (role == ShrineCatRules.ROLE_GUARDIAN) {
			tickGuardianTrial(level);
		} else {
			bossBar.removeAllPlayers();
		}
		if (role == ShrineCatRules.ROLE_VILLAGE && homePos != null) {
			double distSq = distanceToSqr(Vec3.atCenterOf(homePos));
			if (distSq > 64.0) {
				teleportTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5);
				if (variant() == ShrineCatRules.Variant.FREYA) {
					setOrderedToSit(true);
					setInSittingPose(true);
				}
			} else if (distSq > 16.0 && !isOrderedToSit()) {
				getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, 1.0);
			}
		}
		if (ShrineCatRules.grantsDungeonPerk(role) && !sanctuaryFinished && sanctuaryHeart != null) {
			checkHeartFinished(level);
		}
		if (role == ShrineCatRules.ROLE_CLEARED && sanctuaryHeart != null && --perkCooldown <= 0) {
			perkCooldown = 40;
			grantSanctuaryPerks(level);
		}
		if (role == ShrineCatRules.ROLE_TAMED && isTame() && getOwner() instanceof LivingEntity owner) {
			tickTamedPower(level, owner);
			if (sanctuaryFinished && owner instanceof Player pl
					&& (Math.abs(pl.getX()) > 500.0 || Math.abs(pl.getZ()) > 500.0)
					&& distanceToSqr(pl) <= 64.0 && --quartetCheckCooldown <= 0) {
				quartetCheckCooldown = 100;
				ensureQuartetReunited(level, pl, false);
			}
		}
	}

	private void tickGuardianTrial(ServerLevel level) {
		bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
		for (ServerPlayer sp : level.players()) {
			if (!sp.isAlive() || sp.isCreative() || sp.isSpectator()) {
				continue;
			}
			double horizSq = (sp.getX() - getX()) * (sp.getX() - getX()) + (sp.getZ() - getZ()) * (sp.getZ() - getZ());
			if (horizSq <= 24.0 * 24.0 && sp.getY() > getY() + 5.5 && sp.getY() < getY() + 26.0) {
				sp.teleportTo(level, getX(), getY(), getZ() - 3.0, Set.of(), sp.getYRot(), sp.getXRot(), false);
				level.playSound(null, sp.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 0.9F, 0.85F);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sp.getX(), sp.getY() + 1.0, sp.getZ(), 14, 0.4, 0.4, 0.4, 0.04);
				sp.displayClientMessage(Component.literal("Floor " + variant().sanctuaryFloor + " is sealed! Defeat "
					+ variant().displayName + " and her floor sentries before ascending!"), true);
			}
		}
		Player challenger = level.getNearestPlayer(getX(), getY(), getZ(), 22.0,
			p -> p instanceof Player pl && pl.isAlive() && !pl.isCreative() && !pl.isSpectator() && Math.abs(pl.getY() - getY()) < 5.5);
		for (ServerPlayer player : level.players()) {
			boolean sameFloor = Math.abs(player.getY() - getY()) < 5.5 && player.distanceToSqr(this) <= 24.0 * 24.0;
			if (sameFloor) {
				bossBar.addPlayer(player);
			} else {
				bossBar.removePlayer(player);
			}
		}
		if (challenger == null || Math.abs(challenger.getY() - getY()) >= 5.5) {
			setTarget(null);
			if (getHealth() < getMaxHealth() && tickCount % 20 == 0) {
				heal(2.0F);
			}
			if (homePos != null && distanceToSqr(Vec3.atCenterOf(homePos)) > 9.0) {
				getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, 1.0);
			}
			return;
		}
		setLoafing(false);
		setOrderedToSit(false);
		setInSittingPose(false);
		setTarget(challenger);
		if (specialCooldown > 0) {
			specialCooldown--;
			return;
		}
		specialCooldown = 36;
		performVariantTrialMove(level, challenger);
	}

	private void performVariantTrialMove(ServerLevel level, Player challenger) {
		triggerAction();
		ShrineCatRules.Variant v = variant();
		float dmg = ShrineCatRules.chillTrialDamage(v, true);
		switch (v) {
			case YUKI -> {
				level.playSound(null, blockPosition(), SoundEvents.OCELOT_AMBIENT, SoundSource.HOSTILE, 0.9F, 1.35F);
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 14, 1.4, 0.3, 1.4, 0.04);
				level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), 18, 2.0, 0.4, 2.0, 0.03);
				Vec3 leap = challenger.position().subtract(position()).multiply(1, 0, 1);
				if (leap.lengthSqr() > 0.01) {
					setDeltaMovement(leap.normalize().scale(0.75).add(0, 0.28, 0));
				}
				if (distanceToSqr(challenger) <= 25.0) {
					challenger.hurtServer(level, damageSources().mobAttack(this), dmg);
					challenger.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true));
				}
			}
			case ODIE -> {
				level.playSound(null, blockPosition(), SoundEvents.CAT_PURR, SoundSource.HOSTILE, 1.0F, 0.85F);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.4, getZ(), 20, 1.8, 0.3, 1.8, 0.04);
				level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.5, getZ(), 8, 1.2, 0.2, 1.2, 0.02);
				if (distanceToSqr(challenger) <= 25.0) {
					challenger.hurtServer(level, damageSources().mobAttack(this), dmg);
					challenger.setRemainingFireTicks(60);
					Vec3 push = challenger.position().subtract(position()).multiply(1, 0, 1);
					if (push.lengthSqr() > 0.01) {
						Vec3 p = push.normalize().scale(0.65);
						challenger.push(p.x, 0.34, p.z);
					}
				}
			}
			case FREYA -> {
				level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.1F, 1.45F);
				level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 0.7, getZ(), 16, 2.2, 0.4, 2.2, 0.1);
				level.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.9, getZ(), 18, 2.0, 0.5, 2.0, 0.05);
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 10, 1.4, 0.3, 1.4, 0.04);
				addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0, true, false));
				addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
				if (distanceToSqr(challenger) <= 28.0) {
					challenger.hurtServer(level, damageSources().indirectMagic(this, this), dmg);
					challenger.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, true));
					Vec3 away = challenger.position().subtract(position()).multiply(1, 0, 1);
					if (away.lengthSqr() > 0.01) {
						Vec3 push = away.normalize().scale(0.85);
						challenger.push(push.x, 0.30, push.z);
					}
				}
			}
			case KURO -> {
				level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.4F);
				level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 18, 0.7, 0.3, 0.7, 0.04);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.5, getZ(), 12, 0.6, 0.3, 0.6, 0.03);
				Vec3 look = challenger.getLookAngle().multiply(1, 0, 1);
				Vec3 behind = look.lengthSqr() > 0.01
					? challenger.position().subtract(look.normalize().scale(1.8))
					: position();
				BlockPos dst = BlockPos.containing(behind);
				if (level.getBlockState(dst).isAir() && level.getBlockState(dst.above()).isAir() && !level.getBlockState(dst.below()).isAir()) {
					teleportTo(behind.x, dst.getY(), behind.z);
				}
				if (distanceToSqr(challenger) <= 20.0) {
					challenger.hurtServer(level, damageSources().mobAttack(this), dmg);
					challenger.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true));
				}
			}
		}
	}

	private int countFloorSentries(ServerLevel level) {
		AABB floorBox = new AABB(getX() - 24.0, getY() - 4.5, getZ() - 24.0, getX() + 24.0, getY() + 4.5, getZ() + 24.0);
		int count = 0;
		for (Monster m : level.getEntitiesOfClass(Monster.class, floorBox, LivingEntity::isAlive)) {
			if (m instanceof KarakuriSentinel || m instanceof PaperLanternSpirit) {
				count++;
			}
		}
		return count;
	}

	private void summonReinforcements(ServerLevel level) {
		level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.0F, 0.9F);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.8, getZ(), 20, 1.4, 0.4, 1.4, 0.05);
		BlockPos sentinelPos = findOpenNearby(level, blockPosition().offset(3, 0, 0));
		BlockPos spiritPos = findOpenNearby(level, blockPosition().offset(-3, 0, 0));
		KarakuriSentinel sentinel = MonsterContent.KARAKURI_SENTINEL.create(level, EntitySpawnReason.REINFORCEMENT);
		if (sentinel != null) {
			sentinel.snapTo(sentinelPos.getX() + 0.5, sentinelPos.getY(), sentinelPos.getZ() + 0.5, getYRot(), 0.0F);
			sentinel.finalizeSpawn(level, level.getCurrentDifficultyAt(sentinelPos), EntitySpawnReason.REINFORCEMENT, null);
			level.addFreshEntity(sentinel);
		}
		PaperLanternSpirit spirit = MonsterContent.PAPER_LANTERN_SPIRIT.create(level, EntitySpawnReason.REINFORCEMENT);
		if (spirit != null) {
			spirit.snapTo(spiritPos.getX() + 0.5, spiritPos.getY() + 0.5, spiritPos.getZ() + 0.5, getYRot(), 0.0F);
			spirit.finalizeSpawn(level, level.getCurrentDifficultyAt(spiritPos), EntitySpawnReason.REINFORCEMENT, null);
			level.addFreshEntity(spirit);
		}
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) <= 32.0 * 32.0) {
				player.displayClientMessage(Component.literal("[" + variant().displayName + "] Summons Shrine Reinforcements!"), false);
			}
		}
	}

	private BlockPos findOpenNearby(ServerLevel level, BlockPos target) {
		for (int dy = 0; dy <= 1; dy++) {
			BlockPos p = target.above(dy);
			if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
				return p;
			}
		}
		return blockPosition();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.GENERIC_KILL)) {
			bossBar.removeAllPlayers();
			return super.hurtServer(level, source, amount);
		}
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)) {
			return false;
		}
		int role = getRole();
		if (role == ShrineCatRules.ROLE_CLEARED || role == ShrineCatRules.ROLE_VILLAGE) {
			return false;
		}
		if (role == ShrineCatRules.ROLE_TAMED) {
			Entity attacker = source.getEntity();
			LivingEntity owner = getOwner();
			if (attacker != null && owner != null && attacker.getUUID().equals(owner.getUUID())) {
				return false;
			}
			if (source.is(DamageTypes.THORNS) || source.is(DamageTypes.CAMPFIRE)
					|| source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.CACTUS)
					|| source.is(DamageTypes.SWEET_BERRY_BUSH)) {
				return false;
			}
			float floorHp = Math.max(12.0F, getMaxHealth() * 0.35F);
			if (getHealth() - amount <= 2.0F) {
				setHealth(Math.max(getHealth(), floorHp));
				level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.7F, 1.4F);
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.04);
				return false;
			}
		}
		if (role == ShrineCatRules.ROLE_GUARDIAN) {
			boolean bypassAegis = amount >= getMaxHealth() * 0.75F;
			if (!bypassAegis) {
				int sentries = countFloorSentries(level);
				if (sentries > 0) {
					amount *= 0.15F;
					level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.7F, 1.35F);
					level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 0.6, getZ(), 10, 0.5, 0.3, 0.5, 0.05);
					if (source.getEntity() instanceof ServerPlayer sp) {
						sp.displayClientMessage(Component.literal("[" + variant().displayName
							+ "'s Shrine Aegis] Protected while " + sentries + " floor defender(s) remain!"), true);
					}
				}
			}
			float nextHealth = getHealth() - amount;
			if (ShrineCatRules.shouldYieldInTrial(nextHealth, getMaxHealth())) {
				yieldInTrial(level);
				return true;
			}
			if (!bypassAegis) {
				float frac = nextHealth / Math.max(1.0F, getMaxHealth());
				if (!wave1Summoned && frac <= 0.65F) {
					wave1Summoned = true;
					summonReinforcements(level);
				} else if (!wave2Summoned && frac <= 0.40F) {
					wave2Summoned = true;
					summonReinforcements(level);
				}
			}
			boolean hit = super.hurtServer(level, source, amount);
			bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
			return hit;
		}
		return super.hurtServer(level, source, amount);
	}

	private void yieldInTrial(ServerLevel level) {
		setRole(ShrineCatRules.ROLE_CLEARED);
		setHealth(getMaxHealth());
		setTarget(null);
		getNavigation().stop();
		bossBar.removeAllPlayers();
		setLoafing(variant() == ShrineCatRules.Variant.ODIE);
		setOrderedToSit(variant() != ShrineCatRules.Variant.ODIE);
		triggerAction();
		level.playSound(null, blockPosition(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 1.1F, 1.1F);
		level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 16, 0.6, 0.4, 0.6, 0.05);
		ShrineCatRules.Variant v = variant();
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) <= 48.0 * 48.0) {
				player.displayClientMessage(Component.literal("[" + v.displayName + "] Purrs contentedly — Sanctuary Perk unlocked: " + v.dungeonPerk + "!"), false);
			}
		}
		grantSanctuaryPerks(level);
		if (v == ShrineCatRules.Variant.KURO) {
			markSanctuaryFinished(level);
		}
	}

	private void markSanctuaryFinished(ServerLevel level) {
		this.sanctuaryFinished = true;
		AABB box = sanctuaryHeart != null
			? new AABB(sanctuaryHeart).inflate(72.0, 64.0, 72.0)
			: getBoundingBox().inflate(72.0, 64.0, 72.0);
		for (ShrineCat other : level.getEntitiesOfClass(ShrineCat.class, box)) {
			other.sanctuaryFinished = true;
		}
		level.playSound(null, blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.85F, 1.25F);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) <= 72.0 * 72.0) {
				player.displayClientMessage(Component.literal("The Cat Sanctuary is cleared! Feed Yuki, Odie, Freya, or Kuro fish or use a Suzu Cat-Bell Charm to tame them!"), false);
			}
		}
	}

	private void checkHeartFinished(ServerLevel level) {
		if (sanctuaryHeart == null) {
			return;
		}
		for (ShrineCat other : level.getEntitiesOfClass(ShrineCat.class, new AABB(sanctuaryHeart).inflate(16.0))) {
			if (other.variant() == ShrineCatRules.Variant.KURO && other.getRole() != ShrineCatRules.ROLE_GUARDIAN) {
				sanctuaryFinished = true;
				break;
			}
		}
	}

	private void grantSanctuaryPerks(ServerLevel level) {
		AABB area = sanctuaryHeart != null
			? new AABB(sanctuaryHeart).inflate(44.0, 48.0, 44.0)
			: getBoundingBox().inflate(ShrineCatRules.SANCTUARY_BUFF_RANGE);
		List<ServerPlayer> delvers = level.getEntitiesOfClass(ServerPlayer.class, area, p -> p.isAlive() && !p.isSpectator());
		ShrineCatRules.Variant v = variant();
		for (ServerPlayer player : delvers) {
			switch (v) {
				case YUKI -> player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false));
				case ODIE -> {
					player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
					player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
				}
				case FREYA -> {
					player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, true, false));
					player.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, 0, true, false));
				}
				case KURO -> {
					player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 1, true, false));
					player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0, true, false));
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
				}
			}
		}
	}

	private void tickTamedPower(ServerLevel level, LivingEntity owner) {
		if (distanceToSqr(owner) > 256.0) {
			return;
		}
		if (--companionPowerCooldown > 0) {
			return;
		}
		companionPowerCooldown = 100;
		ShrineCatRules.Variant v = variant();
		switch (v) {
			case YUKI -> {
				owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 140, 0, true, false));
				LivingEntity foe = owner.getLastHurtByMob();
				if (foe != null && foe.isAlive() && foe.distanceToSqr(this) <= 36.0 && Targets.canHarm(owner, foe)) {
					triggerAction();
					foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 1, false, true));
					foe.hurtServer(level, damageSources().mobAttack(this), 3.0F);
					level.sendParticles(ParticleTypes.SNOWFLAKE, foe.getX(), foe.getEyeY(), foe.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
				}
			}
			case ODIE -> {
				triggerAction();
				if (owner.getHealth() < owner.getMaxHealth()) {
					owner.heal(2.0F);
				}
				owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
				owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, true, false));
				level.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 1.1, owner.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
				for (Monster mob : level.getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(8.0), LivingEntity::isAlive)) {
					boolean hostile = mob == owner.getLastHurtByMob() || mob == owner.getLastHurtMob()
						|| mob.getTarget() == owner || mob.getTarget() == this;
					if (hostile && Targets.canHarm(owner, mob)) {
						mob.setRemainingFireTicks(60);
						mob.hurtServer(level, damageSources().indirectMagic(this, owner), 4.0F);
						level.sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY() + 0.9, mob.getZ(), 10, 0.3, 0.3, 0.3, 0.03);
						break;
					}
				}
			}
			case FREYA -> {
				triggerAction();
				level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.6F, 1.6F);
				owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1, true, false));
				owner.addEffect(new MobEffectInstance(MobEffects.HASTE, 160, 0, true, false));
				owner.removeEffect(MobEffects.SLOWNESS);
				owner.removeEffect(MobEffects.WEAKNESS);
				level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 0.6, getZ(), 6, 0.7, 0.2, 0.7, 0.05);
				for (Monster mob : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(5.0), LivingEntity::isAlive)) {
					boolean hostile = mob == owner.getLastHurtByMob() || mob == owner.getLastHurtMob()
						|| mob.getTarget() == owner || mob.getTarget() == this || mob instanceof Husk;
					if (!hostile || !Targets.canHarm(owner, mob)) {
						continue;
					}
					Vec3 away = mob.position().subtract(position()).multiply(1, 0, 1);
					if (away.lengthSqr() > 0.01) {
						Vec3 push = away.normalize().scale(0.55);
						mob.push(push.x, 0.22, push.z);
					}
					mob.hurtServer(level, owner instanceof Player pl ? damageSources().playerAttack(pl) : damageSources().mobAttack(this), 3.5F);
				}
			}
			case KURO -> {
				owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 1, true, false));
				owner.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 0, true, false));
				if (level.getMaxLocalRawBrightness(owner.blockPosition()) < 9) {
					owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false));
				}
				LivingEntity prey = owner.getLastHurtMob();
				if (prey != null && prey.isAlive() && prey.distanceToSqr(this) <= 64.0 && Targets.canHarm(owner, prey)) {
					triggerAction();
					prey.hurtServer(level, damageSources().mobAttack(this), 4.0F);
					level.sendParticles(ParticleTypes.SMOKE, prey.getX(), prey.getY() + 0.8, prey.getZ(), 8, 0.3, 0.3, 0.3, 0.03);
				}
			}
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		boolean bellItem = held.is(ShrineCatContent.CAT_BELL_CHARM);
		boolean fishItem = held.is(Items.COD) || held.is(Items.SALMON) || held.is(Items.COOKED_COD)
			|| held.is(Items.COOKED_SALMON) || held.is(Items.TROPICAL_FISH);
		if (!isTame()) {
			int role = getRole();
			if (role == ShrineCatRules.ROLE_GUARDIAN) {
				if (!level().isClientSide()) {
					player.displayClientMessage(Component.literal(variant().displayName + " is still guarding Floor " + variant().sanctuaryFloor + "!"), true);
				}
				return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			if (bellItem || fishItem) {
				if (level() instanceof ServerLevel server) {
					if (!sanctuaryFinished && sanctuaryHeart != null) {
						checkHeartFinished(server);
					}
					if (!ShrineCatRules.canTame(role, sanctuaryFinished, bellItem)) {
						player.displayClientMessage(Component.literal("Finish all 4 Sanctuary floors (defeat Kuro on Floor 4) before taming!"), true);
						return InteractionResult.SUCCESS_SERVER;
					}
					if (fishItem) {
						held.consume(1, player);
					}
					tame(player);
					setRole(ShrineCatRules.ROLE_TAMED);
					setLoafing(false);
					setOrderedToSit(false);
					setInSittingPose(false);
					setTarget(null);
					var maxHp = getAttribute(Attributes.MAX_HEALTH);
					if (maxHp != null) {
						maxHp.setBaseValue(80.0);
					}
					var armor = getAttribute(Attributes.ARMOR);
					if (armor != null) {
						armor.setBaseValue(10.0);
					}
					setHealth(getMaxHealth());
					triggerAction();
					server.broadcastEntityEvent(this, (byte) 7);
					server.playSound(null, blockPosition(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 1.0F, 1.2F);
					player.displayClientMessage(Component.literal("You tamed " + variant().displayName + "! Unique Power: " + variant().tamedPower), false);
				}
				return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
		}
		if (isTame() && isOwnedBy(player)) {
			if (fishItem && getHealth() < getMaxHealth()) {
				if (!level().isClientSide()) {
					held.consume(1, player);
					heal(6.0F);
					level().broadcastEntityEvent(this, (byte) 7);
				}
				return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			if (!level().isClientSide()) {
				if (player.isShiftKeyDown()) {
					setLoafing(!isLoafing());
					setOrderedToSit(isLoafing());
					triggerAction();
					player.displayClientMessage(Component.literal(variant().displayName + (isLoafing() ? " curls into a cozy loaf." : " perks up!")), true);
				} else {
					setLoafing(false);
					setOrderedToSit(!isOrderedToSit());
					getNavigation().stop();
					setTarget(null);
				}
			}
			return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.COOKED_COD)
			|| stack.is(Items.COOKED_SALMON) || stack.is(Items.TROPICAL_FISH);
	}

	@Override
	public boolean canMate(net.minecraft.world.entity.animal.Animal partner) {
		return false;
	}

	@Override
	public  AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag outputTag) { McValueOutput output = new McValueOutput(outputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.addAdditionalSaveData(outputTag);
		output.putInt("ShrineCatVariant", entityData.get(DATA_VARIANT));
		output.putInt("ShrineCatRole", getRole());
		output.putBoolean("ShrineCatLoafing", isLoafing());
		output.putBoolean("SanctuaryFinished", sanctuaryFinished);
		output.putBoolean("Wave1Summoned", wave1Summoned);
		output.putBoolean("Wave2Summoned", wave2Summoned);
		if (homePos != null) {
			output.store("ShrineCatHome", BlockPos.CODEC, homePos);
		}
		if (sanctuaryHeart != null) {
			output.store("SanctuaryHeart", BlockPos.CODEC, sanctuaryHeart);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag inputTag) { McValueInput input = new McValueInput(inputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.readAdditionalSaveData(inputTag);
		ShrineCatRules.Variant v = ShrineCatRules.Variant.byId(input.getIntOr("ShrineCatVariant", 0));
		setVariant(v);
		setRole(input.getIntOr("ShrineCatRole", isTame() ? ShrineCatRules.ROLE_TAMED : ShrineCatRules.ROLE_VILLAGE));
		setLoafing(input.getBooleanOr("ShrineCatLoafing", false));
		sanctuaryFinished = input.getBooleanOr("SanctuaryFinished", isTame());
		wave1Summoned = input.getBooleanOr("Wave1Summoned", false);
		wave2Summoned = input.getBooleanOr("Wave2Summoned", false);
		homePos = input.read("ShrineCatHome", BlockPos.CODEC).orElse(null);
		sanctuaryHeart = input.read("SanctuaryHeart", BlockPos.CODEC).orElse(null);
		if (isTame() && getRole() == ShrineCatRules.ROLE_TAMED) {
			var maxHp = getAttribute(Attributes.MAX_HEALTH);
			if (maxHp != null && maxHp.getBaseValue() < 80.0) {
				maxHp.setBaseValue(80.0);
			}
			var armor = getAttribute(Attributes.ARMOR);
			if (armor != null && armor.getBaseValue() < 10.0) {
				armor.setBaseValue(10.0);
			}
			if (getHealth() < getMaxHealth() * 0.35F) {
				setHealth(getMaxHealth());
			}
		}
		if (getRole() != ShrineCatRules.ROLE_GUARDIAN) {
			bossBar.removeAllPlayers();
		}
	}

	/**
	 * Restores any missing member of the quartet ({@code Yuki}, {@code Odie}, {@code Freya}, {@code Kuro}) beside
	 * {@code owner} when a tamed cat is lost or when the owner calls them with the Suzu Cat-Bell Charm.
	 */
	public static int ensureQuartetReunited(ServerLevel level, Player owner, boolean forceIfAnyTamed) {
		List<ShrineCat> mine = level.getEntitiesOfClass(ShrineCat.class, owner.getBoundingBox().inflate(128.0),
			cat -> cat.isAlive() && cat.isTame() && cat.isOwnedBy(owner));
		if (mine.isEmpty()) {
			return 0;
		}
		EnumSet<ShrineCatRules.Variant> have = EnumSet.noneOf(ShrineCatRules.Variant.class);
		boolean anyFinished = false;
		for (ShrineCat cat : mine) {
			have.add(cat.variant());
			anyFinished |= cat.sanctuaryFinished;
		}
		if (!forceIfAnyTamed && (!anyFinished || have.size() < 3)) {
			return 0;
		}
		int restored = 0;
		for (ShrineCatRules.Variant v : ShrineCatRules.Variant.values()) {
			if (have.contains(v)) {
				continue;
			}
			ShrineCat cat = ShrineCatContent.SHRINE_CAT.create(level, EntitySpawnReason.COMMAND);
			if (cat == null) {
				continue;
			}
			double ox = (restored - 1) * 1.1;
			cat.snapTo(owner.getX() + ox, owner.getY(), owner.getZ() + 1.2, owner.getYRot(), 0.0F);
			cat.setVariant(v);
			cat.tame(owner);
			cat.setRole(ShrineCatRules.ROLE_TAMED);
			cat.sanctuaryFinished = true;
			cat.setPersistenceRequired();
			cat.setLoafing(false);
			cat.setOrderedToSit(false);
			cat.setInSittingPose(false);
			var maxHp = cat.getAttribute(Attributes.MAX_HEALTH);
			if (maxHp != null) {
				maxHp.setBaseValue(80.0);
			}
			var armor = cat.getAttribute(Attributes.ARMOR);
			if (armor != null) {
				armor.setBaseValue(10.0);
			}
			cat.setHealth(cat.getMaxHealth());
			cat.triggerAction();
			level.addFreshEntity(cat);
			level.playSound(null, cat.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.85F, 1.35F);
			owner.displayClientMessage(Component.literal("[" + v.displayName + "] Rings her Suzu bell and rejoins your side!"), false);
			restored++;
		}
		return restored;
	}
}
