package dev.wildercord.wildlife;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The Ridgeback Stag (0.12 "Tempering"): the mod's first mount. A broad chestnut stag of the plains, meadows and savannas,
 * ridden like a horse: climb on until it stops throwing you, saddle it, and it carries you a little faster and further than
 * most horses, with a better jump. Two tame ones fed golden food raise a calf with a blend of their parents' strengths.
 */
public class RidgebackStag extends AbstractHorse {
	public RidgebackStag(EntityType<? extends RidgebackStag> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 28.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.JUMP_STRENGTH, 0.75)
			.add(Attributes.STEP_HEIGHT, 1.1);
	}

	/** Sturdier and quicker than a horse on average, with less spread: 24-32 health, 0.25-0.33 speed, 0.65-0.9 jump. */
	@Override
	protected void randomizeAttributes(RandomSource random) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(24 + random.nextInt(9));
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25 + random.nextDouble() * 0.08);
		getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.65 + random.nextDouble() * 0.25);
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
		return other != this && other instanceof RidgebackStag stag && canParent() && stag.canParent();
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		RidgebackStag calf = MountContent.RIDGEBACK_STAG.create(level, EntitySpawnReason.BREEDING);
		if (calf != null) {
			setOffspringAttributes(partner, calf);
		}
		return calf;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HORSE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HORSE_DEATH;
	}

	@Override
	protected SoundEvent getEatingSound() {
		return SoundEvents.HORSE_EAT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.HORSE_ANGRY;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 0.85F;
	}
}
