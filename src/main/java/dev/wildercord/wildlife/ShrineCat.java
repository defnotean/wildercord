package dev.wildercord.wildlife;

import dev.wildercord.cast.Targets;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

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
		getUUID(),
		Component.literal("Shrine Guardian"),
		BossEvent.BossBarColor.PINK,
		BossEvent.BossBarOverlay.NOTCHED_10
	);

	private @Nullable BlockPos homePos;
	private @Nullable BlockPos sanctuaryHeart;
	private boolean sanctuaryFinished;
	private int specialCooldown = 50;
	private int perkCooldown = 20;
	private int companionPowerCooldown = 80;
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
			.add(Attributes.MAX_HEALTH, 36.0)
			.add(Attributes.MOVEMENT_SPEED, 0.30)
			.add(Attributes.ATTACK_DAMAGE, 4.0)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.35);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.42F));
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				int r = getRole();
				return (r == ShrineCatRules.ROLE_GUARDIAN || r == ShrineCatRules.ROLE_TAMED) && super.canUse();
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

		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		targetSelector.addGoal(3, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				int r = getRole();
				return (r == ShrineCatRules.ROLE_GUARDIAN || r == ShrineCatRules.ROLE_TAMED) && super.canUse();
			}
		});
		targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
			(target, lvl) -> getRole() == ShrineCatRules.ROLE_TAMED && isTame()));
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
		setPersistenceRequired();
		var maxHp = getAttribute(Attributes.MAX_HEALTH);
		if (maxHp != null) {
			maxHp.setBaseValue(v.trialHealth);
		}
		var atk = getAttribute(Attributes.ATTACK_DAMAGE);
		if (atk != null) {
			atk.setBaseValue(v.baseDamage);
		}
		setHealth(getMaxHealth());
		setLoafing(v == ShrineCatRules.Variant.ODIE);
		setOrderedToSit(v == ShrineCatRules.Variant.FREYA);
	}

	/** Configures this cat as a peaceful inhabitant of the Japanese Sakura Village. */
	public void configureVillage(ShrineCatRules.Variant v, BlockPos home) {
		setVariant(v);
		setRole(ShrineCatRules.ROLE_VILLAGE);
		this.homePos = home.immutable();
		this.sanctuaryFinished = true;
		setPersistenceRequired();
		setLoafing(v == ShrineCatRules.Variant.ODIE);
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
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
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
		if (ShrineCatRules.grantsDungeonPerk(role) && !sanctuaryFinished && sanctuaryHeart != null) {
			checkHeartFinished(level);
		}
		if (role == ShrineCatRules.ROLE_CLEARED && sanctuaryHeart != null && --perkCooldown <= 0) {
			perkCooldown = 40;
			grantSanctuaryPerks(level);
		}
		if (role == ShrineCatRules.ROLE_TAMED && isTame() && getOwner() instanceof LivingEntity owner) {
			tickTamedPower(level, owner);
		}
	}

	private void tickGuardianTrial(ServerLevel level) {
		bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
		Player challenger = level.getNearestPlayer(getX(), getY(), getZ(), 18.0, p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
		for (ServerPlayer player : level.players()) {
			boolean sameFloor = Math.abs(player.getY() - getY()) < 5.5 && player.distanceToSqr(this) <= 22.0 * 22.0;
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
		setTarget(challenger);
		if (specialCooldown > 0) {
			specialCooldown--;
			return;
		}
		specialCooldown = 65;
		performVariantTrialMove(level, challenger);
	}

	private void performVariantTrialMove(ServerLevel level, Player challenger) {
		triggerAction();
		ShrineCatRules.Variant v = variant();
		float dmg = ShrineCatRules.chillTrialDamage(v, true);
		switch (v) {
			case YUKI -> {
				level.playSound(null, blockPosition(), SoundEvents.CAT_HISS, SoundSource.HOSTILE, 0.9F, 1.25F);
				level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 14, 1.4, 0.3, 1.4, 0.04);
				Vec3 leap = challenger.position().subtract(position()).multiply(1, 0, 1);
				if (leap.lengthSqr() > 0.01) {
					setDeltaMovement(leap.normalize().scale(0.65).add(0, 0.25, 0));
				}
				if (distanceToSqr(challenger) <= 16.0) {
					challenger.hurtServer(level, damageSources().mobAttack(this), dmg);
					challenger.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, true));
				}
			}
			case ODIE -> {
				level.playSound(null, blockPosition(), SoundEvents.CAT_PURR, SoundSource.HOSTILE, 1.0F, 0.85F);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.4, getZ(), 16, 1.6, 0.25, 1.6, 0.03);
				if (distanceToSqr(challenger) <= 20.0) {
					challenger.hurtServer(level, damageSources().mobAttack(this), dmg);
					Vec3 push = challenger.position().subtract(position()).multiply(1, 0, 1);
					if (push.lengthSqr() > 0.01) {
						Vec3 p = push.normalize().scale(0.55);
						challenger.push(p.x, 0.28, p.z);
					}
				}
			}
			case FREYA -> {
				level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.0F, 1.45F);
				level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 0.7, getZ(), 12, 1.8, 0.4, 1.8, 0.1);
				if (distanceToSqr(challenger) <= 24.0) {
					challenger.hurtServer(level, damageSources().indirectMagic(this, this), dmg);
					Vec3 away = challenger.position().subtract(position()).multiply(1, 0, 1);
					if (away.lengthSqr() > 0.01) {
						Vec3 push = away.normalize().scale(0.75);
						challenger.push(push.x, 0.25, push.z);
					}
				}
			}
			case KURO -> {
				level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.4F);
				level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 14, 0.5, 0.3, 0.5, 0.04);
				Vec3 behind = challenger.position().subtract(challenger.getLookAngle().multiply(1, 0, 1).normalize().scale(1.8));
				BlockPos dst = BlockPos.containing(behind);
				if (level.getBlockState(dst).isAir() && level.getBlockState(dst.above()).isAir() && !level.getBlockState(dst.below()).isAir()) {
					teleportTo(behind.x, dst.getY(), behind.z);
				}
				if (distanceToSqr(challenger) <= 14.0) {
					challenger.hurtServer(level, damageSources().mobAttack(this), dmg);
				}
			}
		}
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
		if (role == ShrineCatRules.ROLE_GUARDIAN) {
			float nextHealth = getHealth() - amount;
			if (ShrineCatRules.shouldYieldInTrial(nextHealth, getMaxHealth())) {
				yieldInTrial(level);
				return true;
			}
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
		level.playSound(null, blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.1F, 1.0F);
		level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), 16, 0.6, 0.4, 0.6, 0.05);
		ShrineCatRules.Variant v = variant();
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) <= 48.0 * 48.0) {
				player.sendSystemMessage(Component.literal("[" + v.displayName + "] Purrs contentedly — Sanctuary Perk unlocked: " + v.dungeonPerk + "!"));
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
				player.sendSystemMessage(Component.literal("The Cat Sanctuary is cleared! Feed Yuki, Odie, Freya, or Kuro fish or use a Suzu Cat-Bell Charm to tame them!"));
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
					foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true));
					foe.hurtServer(level, damageSources().mobAttack(this), 3.0F);
					level.sendParticles(ParticleTypes.SNOWFLAKE, foe.getX(), foe.getEyeY(), foe.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
				}
			}
			case ODIE -> {
				triggerAction();
				if (owner.getHealth() < owner.getMaxHealth()) {
					owner.heal(2.0F);
				}
				owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
				owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 140, 0, true, false));
				level.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 1.1, owner.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
			}
			case FREYA -> {
				triggerAction();
				level.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 0.6F, 1.6F);
				owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 0, true, false));
				level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 0.6, getZ(), 6, 0.7, 0.2, 0.7, 0.05);
				for (Monster mob : level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(4.5), LivingEntity::isAlive)) {
					Vec3 away = mob.position().subtract(position()).multiply(1, 0, 1);
					if (away.lengthSqr() > 0.01) {
						Vec3 push = away.normalize().scale(0.5);
						mob.push(push.x, 0.2, push.z);
					}
					mob.hurtServer(level, damageSources().indirectMagic(this, owner), 3.0F);
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
					player.sendOverlayMessage(Component.literal(variant().displayName + " is still guarding Floor " + variant().sanctuaryFloor + "!"));
				}
				return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			if (bellItem || fishItem) {
				if (level() instanceof ServerLevel server) {
					if (!sanctuaryFinished && sanctuaryHeart != null) {
						checkHeartFinished(server);
					}
					if (!ShrineCatRules.canTame(role, sanctuaryFinished, bellItem)) {
						player.sendOverlayMessage(Component.literal("Finish all 4 Sanctuary floors (defeat Kuro on Floor 4) before taming!"));
						return InteractionResult.SUCCESS_SERVER;
					}
					if (fishItem) {
						held.consume(1, player);
					}
					tame(player);
					setRole(ShrineCatRules.ROLE_TAMED);
					setLoafing(false);
					setOrderedToSit(false);
					setTarget(null);
					triggerAction();
					server.broadcastEntityEvent(this, (byte) 7);
					server.playSound(null, blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0F, 1.15F);
					player.sendSystemMessage(Component.literal("You tamed " + variant().displayName + "! Unique Power: " + variant().tamedPower));
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
					player.sendOverlayMessage(Component.literal(variant().displayName + (isLoafing() ? " curls into a cozy loaf." : " perks up!")));
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
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("ShrineCatVariant", entityData.get(DATA_VARIANT));
		output.putInt("ShrineCatRole", getRole());
		output.putBoolean("ShrineCatLoafing", isLoafing());
		output.putBoolean("SanctuaryFinished", sanctuaryFinished);
		if (homePos != null) {
			output.store("ShrineCatHome", BlockPos.CODEC, homePos);
		}
		if (sanctuaryHeart != null) {
			output.store("SanctuaryHeart", BlockPos.CODEC, sanctuaryHeart);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ShrineCatRules.Variant v = ShrineCatRules.Variant.byId(input.getIntOr("ShrineCatVariant", 0));
		setVariant(v);
		setRole(input.getIntOr("ShrineCatRole", isTame() ? ShrineCatRules.ROLE_TAMED : ShrineCatRules.ROLE_VILLAGE));
		setLoafing(input.getBooleanOr("ShrineCatLoafing", false));
		sanctuaryFinished = input.getBooleanOr("SanctuaryFinished", isTame());
		homePos = input.read("ShrineCatHome", BlockPos.CODEC).orElse(null);
		sanctuaryHeart = input.read("SanctuaryHeart", BlockPos.CODEC).orElse(null);
		if (getRole() != ShrineCatRules.ROLE_GUARDIAN) {
			bossBar.removeAllPlayers();
		}
	}
}
