package dev.wildercord.pet;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** A peaceful, immortal little dog bound to the configured owner from the moment she appears. */
public final class CinnamonDog extends TamableAnimal {
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MOOD = net.minecraft.network.syncher.SynchedEntityData.defineId(CinnamonDog.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	private int settled, playTicks, greeting;
	public int mood() { return entityData.get(MOOD); }
	@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder); builder.define(MOOD,0);
	}
	public CinnamonDog(EntityType<? extends CinnamonDog> type, Level level) {
		super(type, level);
		setCustomName(Component.literal("Cinnamon"));
		setCustomNameVisible(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.FOLLOW_RANGE, 32);
	}

	void bind(ServerPlayer owner) {
		setTame(true, false);
		setOwner(owner);
		setOrderedToSit(owner.getAttachedOrElse(CinnamonState.SITTING, false));
		setCustomName(Component.literal("Cinnamon"));
		setCustomNameVisible(true);
		greeting = 60;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel serverLevel && (!isTame() || tickCount % 20 == 0)) {
			ServerPlayer owner = CinnamonCompanion.configuredOwner(serverLevel.getServer());
			if (owner != null && owner.level() == level() && !isOwnedBy(owner)) bind(owner);
		}
		if (!level().isClientSide()) {
			if (isOrderedToSit() && playTicks == 0 && greeting == 0) settled++; else settled = 0;
			if (playTicks > 0) playTicks--;
			if (greeting > 0) greeting--;
			entityData.set(MOOD, (settled >= 200 ? 1 : 0) | (playTicks > 0 ? 2 : 0) | (greeting > 0 ? 4 : 0));
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.15, 8, 2));
		goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.75));
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!isOwnedBy(player)) {
			if (!isTame() && !level().isClientSide()) player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.owner"));
			return InteractionResult.PASS;
		}
		if (!level().isClientSide()) {
			if (player.getItemInHand(hand).is(CinnamonContent.TOY)) {
				playTicks = 50; settled = 0;
				level().broadcastEntityEvent(this, (byte) 7);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.toy"));
				return InteractionResult.SUCCESS;
			}
			if (player.isShiftKeyDown()) {
				playTicks = 25; settled = 0;
				level().broadcastEntityEvent(this, (byte) 7);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.pet"));
				return InteractionResult.SUCCESS;
			}
			setOrderedToSit(!isOrderedToSit());
			settled = 0;
			player.setAttached(CinnamonState.SITTING, isOrderedToSit());
			player.sendOverlayMessage(Component.translatable(isOrderedToSit() ? "message.wildercord.cinnamon.sit" : "message.wildercord.cinnamon.follow"));
			getNavigation().stop();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isFood(ItemStack stack) { return false; }

	@Override
	public boolean canMate(net.minecraft.world.entity.animal.Animal partner) { return false; }

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

	@Override
	public boolean canBeLeashed() { return false; }

	@Override
	public boolean shouldBeSaved() { return false; }
}
