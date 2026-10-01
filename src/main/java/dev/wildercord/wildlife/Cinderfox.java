package dev.wildercord.wildlife;

import dev.wildercord.cast.Affinities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A cinderfox: a slight, big-eared fox of deserts and badlands whose tail ends in a living ember, glowing and shedding
 * sparks at night. Wild ones keep their distance and hunt rabbits and chickens. Offer one a rabbit (raw or cooked) and,
 * one time in three, it's yours: it follows, sits when told, and fights for you, and its bite carries embers that set a
 * creature weak to fire alight and hurt it half again. Brush a tame one and once a day it leaves an Ember Tuft.
 */
public class Cinderfox extends TamableAnimal {
	/** The day it last gave a tuft to the brush (-1: never). */
	private static final EntityDataAccessor<Long> DATA_BRUSHED = SynchedEntityData.defineId(Cinderfox.class, EntityDataSerializers.LONG);

	/** Client: how far it has sat down, eased (this tick's and last tick's). */
	public float sit, sitO;

	public Cinderfox(EntityType<? extends Cinderfox> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			sitO = sit;
			sit = WildlifeRules.approach(sit, isInSittingPose() ? 1 : 0, 0.16F);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.31).add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BRUSHED, -1L);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, p -> !isTame() && !p.isShiftKeyDown(), 9.0F, 1.2, 1.5, p -> true));
		goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.35F));
		goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.1, 10.0F, 2.0F));
		goalSelector.addGoal(7, new BreedGoal(this, 1.0));
		goalSelector.addGoal(8, new TemptGoal(this, 1.1, this::tames, true));
		goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		targetSelector.addGoal(3, new HurtByTargetGoal(this));
		targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Rabbit.class, false, null));
		targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Chicken.class, false, null));
	}

	/** What wins a wild one over: rabbit, raw or cooked. */
	public boolean tames(ItemStack stack) {
		return stack.is(Items.RABBIT) || stack.is(Items.COOKED_RABBIT);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return tames(stack) || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN);
	}

	// ------------------------------------------------------------------ taming and its owner

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isTame()) {
			if (tames(held) && !level().isClientSide()) {
				held.consume(1, player);
				if (random.nextDouble() < WildlifeRules.TAME_CHANCE) {
					tame(player);
					getNavigation().stop();
					setTarget(null);
					setOrderedToSit(true);
					level().broadcastEntityEvent(this, (byte) 7);
				} else {
					level().broadcastEntityEvent(this, (byte) 6);
				}
				return InteractionResult.SUCCESS_SERVER;
			}
			return super.mobInteract(player, hand);
		}
		if (isFood(held) && getHealth() < getMaxHealth()) {
			feed(player, hand, held, 2.0F, 3.0F);
			return InteractionResult.SUCCESS;
		}
		if (held.is(Items.BRUSH) && isOwnedBy(player)) {
			if (!level().isClientSide()) {
				if (brush((ServerLevel) level())) {
					held.hurtAndBreak(16, player, hand.asEquipmentSlot());
				} else {
					player.sendOverlayMessage(Component.translatable("message.wildercord.cinderfox.brushed"));
				}
			}
			return InteractionResult.SUCCESS;
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!result.consumesAction() && isOwnedBy(player)) {
			setOrderedToSit(!isOrderedToSit());
			jumping = false;
			getNavigation().stop();
			setTarget(null);
			return InteractionResult.SUCCESS.withoutItem();
		}
		return result;
	}

	/** A brushing: today's Ember Tuft, if it hasn't given one today. */
	public boolean brush(ServerLevel level) {
		long today = WildlifeRules.day(level.getOverworldClockTime());
		if (entityData.get(DATA_BRUSHED) == today || isBaby()) {
			return false;
		}
		entityData.set(DATA_BRUSHED, today);
		spawnAtLocation(level, new ItemStack(Wildlife.EMBER_TUFT));
		SoundEvent purr = Wildlife.sound("cinderfox_yip");
		if (purr != null) {
			level.playSound(null, getX(), getY(), getZ(), purr, SoundSource.NEUTRAL, 0.8F, 1.25F);
		}
		level.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.4, getZ(), 6, 0.25, 0.15, 0.25, 0.01);
		return true;
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) {
			return false;
		}
		return !(target instanceof Player victim && owner instanceof Player master && !master.canHarmPlayer(victim));
	}

	// ------------------------------------------------------------------ its bite

	/** Whether a creature is weak to fire (the creature affinities), so its bite sets it alight. */
	public static boolean weakToFire(Entity target) {
		TagKey<EntityType<?>> weak = Affinities.weakTo("fire");
		return weak != null && target.getType().builtInRegistryHolder().is(weak);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean weak = weakToFire(target);
		float bite = WildlifeRules.sparkBite((float) getAttributeValue(Attributes.ATTACK_DAMAGE), weak);
		boolean hit = target.hurtServer(level, damageSources().mobAttack(this), bite);
		if (hit) {
			setLastHurtMob(target);
			// A shower of sparks off its tail, and on a creature weak to fire, the embers catch.
			level.sendParticles(ParticleTypes.SMALL_FLAME, target.getX(), target.getY(0.6), target.getZ(), weak ? 10 : 3, 0.2, 0.25, 0.2, 0.02);
			if (weak) {
				target.igniteForSeconds(WildlifeRules.SPARK_BURN_SECONDS);
				SoundEvent spark = Wildlife.sound("cinderfox_spark");
				if (spark != null) {
					level.playSound(null, target.getX(), target.getY(), target.getZ(), spark, SoundSource.NEUTRAL, 0.9F, 1.0F);
				}
			}
		}
		return hit;
	}

	// ------------------------------------------------------------------ breeding

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Cinderfox kit = Wildlife.CINDERFOX.create(level, EntitySpawnReason.BREEDING);
		if (kit != null && isTame()) {
			kit.setOwnerReference(getOwnerReference());
			kit.setTame(true, true);
		}
		return kit;
	}

	@Override
	public boolean canMate(net.minecraft.world.entity.animal.Animal partner) {
		return partner != this && partner instanceof Cinderfox other && isTame() && other.isTame() && !isInSittingPose() && !other.isInSittingPose()
			&& isInLove() && other.isInLove();
	}

	// ------------------------------------------------------------------ sound

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return Wildlife.sound("cinderfox_yip");
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return Wildlife.sound("cinderfox_hurt");
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return Wildlife.sound("cinderfox_death");
	}

	@Override
	public int getAmbientSoundInterval() {
		return 240;
	}

	@Override
	protected float getSoundVolume() {
		return 0.7F;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong("brushed_day", entityData.get(DATA_BRUSHED));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_BRUSHED, input.getLongOr("brushed_day", -1L));
	}
}
