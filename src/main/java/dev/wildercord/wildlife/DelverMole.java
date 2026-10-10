package dev.wildercord.wildlife;

import dev.wildercord.cast.Casters;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Delver Mole (0.13), the burrowing mount: a huge velvet-black mole with a pink star of a nose and spade claws, found
 * on plains, meadows and in forests. Tamed and saddled like a horse, it digs its rider a tunnel through soft ground as
 * it goes (see {@link DelverRules}), level, or down or up as its rider looks, dropping what it digs, and never through
 * anything its rider couldn't break there themselves. It eats carrots, potatoes and beetroot; two tame ones fed golden
 * food raise a pup.
 */
public class DelverMole extends AbstractHorse {
	private static final EntityDataAccessor<Boolean> DIGGING = SynchedEntityData.defineId(DelverMole.class, EntityDataSerializers.BOOLEAN);
	/** Ticks it still shows as digging. */
	private int digging;
	/** How far into its digging stroke it is, eased on the client for its model. */
	public float digO, dig;

	public DelverMole(EntityType<? extends DelverMole> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.JUMP_STRENGTH, 0.5)
			.add(Attributes.ARMOR, 2.0).add(Attributes.STEP_HEIGHT, 1.0);
	}

	/** Stout and steady, a little slower than a horse: 26-34 health, 0.18-0.22 speed. */
	@Override
	protected void randomizeAttributes(RandomSource random) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(26 + random.nextInt(9));
		getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.18 + random.nextDouble() * 0.04);
		getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.45 + random.nextDouble() * 0.1);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DIGGING, false);
	}

	/** Whether it's digging now. */
	public boolean digging() {
		return entityData.get(DIGGING);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			digO = dig;
			dig = WildlifeRules.approach(dig, digging() ? 1 : 0, 0.2F);
			return;
		}
		if (level() instanceof ServerLevel level && isSaddled() && getControllingPassenger() instanceof ServerPlayer rider
			&& rider.getLastClientInput().forward() && tickCount % DelverRules.DIG_EVERY == 0 && burrow(level, rider) > 0) {
			digging = DelverRules.DIG_SHOWN;
		}
		if (digging > 0) {
			digging--;
		}
		if (digging() != digging > 0) {
			entityData.set(DIGGING, digging > 0);
		}
	}

	/**
	 * Takes a bite out of the ground ahead of it, as tall as it and its rider, tipped down or up as they look. Returns how
	 * many blocks it dug.
	 */
	public int burrow(ServerLevel level, ServerPlayer rider) {
		double[] reach = DelverRules.reach(rider.getYRot(), rider.getXRot());
		AABB box = getBoundingBox().move(reach[0], reach[1], reach[2]).expandTowards(0, rider.getBbHeight() * 0.9, 0).deflate(0.05);
		int dug = 0;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !DelverRules.digs(state.is(BlockTags.MINEABLE_WITH_SHOVEL), state.getDestroySpeed(level, pos), state.hasBlockEntity())) {
				continue;
			}
			if (Casters.mayEdit(rider, level, pos) && !rider.blockActionRestricted(level, pos, rider.gameMode()) && level.destroyBlock(pos, true, rider)) {
				dug++;
			}
		}
		return dug;
	}

	// ------------------------------------------------------------------ feeding and breeding

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT) || stack.is(Items.GOLDEN_APPLE)
			|| stack.is(Items.GOLDEN_CARROT);
	}

	/** Roots heal it and calm a wild one; golden food is a horse's, and readies a tame one to breed. */
	@Override
	protected boolean handleEating(Player player, ItemStack stack) {
		if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.GOLDEN_CARROT)) {
			return super.handleEating(player, stack);
		}
		boolean wanted = getHealth() < getMaxHealth() || !isTamed() && getTemper() < getMaxTemper();
		if (getHealth() < getMaxHealth()) {
			heal(2F);
		}
		if (!isTamed() && getTemper() < getMaxTemper()) {
			modifyTemper(3);
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
		return other != this && other instanceof DelverMole mole && canParent() && mole.canParent();
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		DelverMole pup = MountContent.DELVER_MOLE.create(level, EntitySpawnReason.BREEDING);
		if (pup != null) {
			setOffspringAttributes(partner, pup);
		}
		return pup;
	}

	// ------------------------------------------------------------------ sounds

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ROOTED_DIRT_BREAK;
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
		return SoundEvents.GENERIC_EAT.value();
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.HORSE_ANGRY;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.4F;
	}
}
