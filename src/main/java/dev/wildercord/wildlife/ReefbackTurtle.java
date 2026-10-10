package dev.wildercord.wildlife;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Reefback Turtle (0.13), the water mount: a great sea turtle with a coral-crusted shell that comes ashore on warm
 * beaches. Tamed and saddled like a horse, it plods on land, but in water it swims the way its rider looks, diving or
 * climbing, never throws its rider off under water, and keeps them breathing (see {@link ReefbackRules}). It eats kelp
 * and seagrass; two tame ones fed golden food raise a hatchling.
 */
public class ReefbackTurtle extends AbstractHorse {
	/** How far its flippers are into a swimming stroke (0 walking, 1 swimming), eased on the client for its model. */
	public float swimO, swim;

	public ReefbackTurtle(EntityType<? extends ReefbackTurtle> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 34.0).add(Attributes.MOVEMENT_SPEED, 0.14).add(Attributes.JUMP_STRENGTH, 0.4)
			.add(Attributes.ARMOR, 6.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	/** Tough and slow ashore, alike from one to the next: 30-38 health. */
	@Override
	protected void randomizeAttributes(RandomSource random) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(30 + random.nextInt(9));
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.13 + random.nextDouble() * 0.02);
		getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.4);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			swimO = swim;
			swim = WildlifeRules.approach(swim, isInWater() ? 1 : 0, 0.1F);
		} else if (isInWater() && tickCount % ReefbackRules.BREATH_EVERY == 0 && getControllingPassenger() instanceof Player rider) {
			rider.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, ReefbackRules.BREATH_TICKS, 0, true, false, true));
		}
	}

	// ------------------------------------------------------------------ swimming

	@Override
	protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		if (isInWater() && canSimulateMovement()) {
			double[] wanted = ReefbackRules.swim(player.getYRot(), player.getXRot(), player.zza, player.xxa);
			Vec3 v = getDeltaMovement();
			setDeltaMovement(ReefbackRules.ease(v.x, wanted[0]), ReefbackRules.ease(v.y, wanted[1]), ReefbackRules.ease(v.z, wanted[2]));
		}
	}

	/** In water its course is set in {@link #tickRidden}; ashore it walks like any horse. */
	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 input) {
		return isInWater() ? Vec3.ZERO : super.getRiddenInput(player, input);
	}

	@Override
	protected float getWaterSlowDown() {
		return 0.96F;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean dismountsUnderwater() {
		return false;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	// ------------------------------------------------------------------ feeding and breeding

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.KELP) || stack.is(Items.DRIED_KELP) || stack.is(Items.SEAGRASS) || stack.is(Items.GOLDEN_APPLE)
			|| stack.is(Items.GOLDEN_CARROT);
	}

	/** Kelp and seagrass heal it and calm a wild one; golden food is a horse's, and readies a tame one to breed. */
	@Override
	protected boolean handleEating(Player player, ItemStack stack) {
		if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.GOLDEN_CARROT)) {
			return super.handleEating(player, stack);
		}
		boolean wanted = getHealth() < getMaxHealth() || !isTamed() && getTemper() < getMaxTemper();
		if (getHealth() < getMaxHealth()) {
			heal(ReefbackRules.BITE_HEAL);
		}
		if (!isTamed() && getTemper() < getMaxTemper()) {
			modifyTemper(ReefbackRules.BITE_TEMPER);
		}
		if (wanted) {
			playSound(getEatingSound(), 1.0F, getVoicePitch());
		}
		return wanted;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		boolean inventory = !isBaby() && isTamed() && player.isSecondaryUseActive();
		if (!isVehicle() && !inventory) {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.isEmpty()) {
				if (isFood(stack)) {
					return fedFood(player, stack);
				}
				if (!isTamed()) {
					makeMad();
					return InteractionResult.SUCCESS;
				}
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public boolean canMate(Animal other) {
		return other != this && other instanceof ReefbackTurtle turtle && canParent() && turtle.canParent();
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		ReefbackTurtle hatchling = MountContent.REEFBACK_TURTLE.create(level, EntitySpawnReason.BREEDING);
		if (hatchling != null) {
			setOffspringAttributes(partner, hatchling);
		}
		return hatchling;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return isInWater() ? null : SoundEvents.TURTLE_AMBIENT_LAND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.TURTLE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.TURTLE_DEATH;
	}

	@Override
	protected SoundEvent getEatingSound() {
		return SoundEvents.GENERIC_EAT.value();
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.TURTLE_HURT;
	}

	@Override
	protected SoundEvent getSwimSound() {
		return SoundEvents.TURTLE_SWIM;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.7F;
	}
}
