package dev.wildercord.wildlife;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A black bobcat: a melanistic wildcat of the dark forests and old taigas, as big as a polar bear, with tufted ears, a bobbed
 * tail and pale gold eyes. A wild one keeps to itself, stalks rabbits and chickens, and turns on whatever hurts it. Offer it
 * fish (raw or cooked) and, one time in three, it's yours: it follows, sits when told, pounces on what you fight, and two
 * tame ones in love raise a kitten that is yours too.
 */
public class BlackBobcat extends TamableAnimal {
	/** Client: how far it has sat down, eased (this tick's and last tick's). */
	public float sit, sitO;

	public BlackBobcat(EntityType<? extends BlackBobcat> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			sitO = sit;
			sit = WildlifeRules.approach(sit, isInSittingPose() ? 1 : 0, 0.12F);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.45F));
		goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.1, 10.0F, 2.5F));
		goalSelector.addGoal(7, new BreedGoal(this, 1.0));
		goalSelector.addGoal(8, new TemptGoal(this, 0.9, this::tames, true));
		goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		targetSelector.addGoal(3, new HurtByTargetGoal(this));
		targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Rabbit.class, false, null));
		targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Chicken.class, false, null));
	}

	/** What wins a wild one over: fish, raw or cooked (never pufferfish). */
	public boolean tames(ItemStack stack) {
		return FoxCompanions.fish(stack);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return tames(stack);
	}

	// ------------------------------------------------------------------ taming and its owner

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isTame()) {
			if (tames(held) && !isBaby()) {
				if (!level().isClientSide()) {
					held.consume(1, player);
					if (random.nextDouble() < WildlifeRules.BOBCAT_TAME_CHANCE) {
						tame(player);
						getNavigation().stop();
						setTarget(null);
						setOrderedToSit(true);
						level().broadcastEntityEvent(this, (byte) 7);
					} else {
						level().broadcastEntityEvent(this, (byte) 6);
					}
				}
				return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
			}
			return super.mobInteract(player, hand);
		}
		if (isFood(held) && getHealth() < getMaxHealth()) {
			feed(player, hand, held, 3.0F, 4.0F);
			return InteractionResult.SUCCESS;
		}
		InteractionResult result = super.mobInteract(player, hand);
		if (!result.consumesAction() && isOwnedBy(player) && !held.is(Items.NAME_TAG)) {
			setOrderedToSit(!isOrderedToSit());
			jumping = false;
			getNavigation().stop();
			setTarget(null);
			return InteractionResult.SUCCESS.withoutItem();
		}
		return result;
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) {
			return false;
		}
		return !(target instanceof Player victim && owner instanceof Player master && !master.canHarmPlayer(victim));
	}

	// ------------------------------------------------------------------ breeding

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		BlackBobcat kitten = BobcatContent.BLACK_BOBCAT.create(level, EntitySpawnReason.BREEDING);
		if (kitten != null && isTame()) {
			kitten.setOwnerReference(getOwnerReference());
			kitten.setTame(true, true);
		}
		return kitten;
	}

	@Override
	public boolean canMate(Animal partner) {
		return partner != this && partner instanceof BlackBobcat other && isTame() && other.isTame() && !isInSittingPose() && !other.isInSittingPose()
			&& isInLove() && other.isInLove();
	}

	// ------------------------------------------------------------------ sound: an ocelot's voice, a big cat's chest

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.OCELOT_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.OCELOT_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.OCELOT_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.6F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 320;
	}
}
