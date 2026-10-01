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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * An immortal little dog bound to the configured owner from the moment she appears. She goes after anyone who hurts her
 * owner, her collar bell jingles every so often so you can find her by ear, now and then she pokes the tip of her tongue
 * out, and she can wear a bow.
 */
public final class CinnamonDog extends TamableAnimal {
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MOOD = net.minecraft.network.syncher.SynchedEntityData.defineId(CinnamonDog.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	/** Mood bits, synced for her model: asleep, playing, greeting, bell ringing, tongue out, wearing her bow. */
	public static final int SLEEPING = 1, PLAYING = 2, GREETING = 4, RINGING = 8, TONGUE = 16, BOW = 32;
	/** How long a jingle lasts, and when in it each of its three little dings sounds. */
	private static final int RING_TICKS = 12;
	private int settled, playTicks, greeting, ringTicks, tongueTicks;
	private int nextRing = 100 + getRandom().nextInt(300);
	private boolean bow;
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
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ATTACK_DAMAGE, 3);
	}

	void bind(ServerPlayer owner) {
		setTame(true, false);
		setOwner(owner);
		setOrderedToSit(owner.getAttachedOrElse(CinnamonState.SITTING, false));
		bow = owner.getAttachedOrElse(CinnamonState.BOW, false);
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
			ring();
			// The tip of her tongue, for a few seconds about every minute and a half (asleep too).
			if (tongueTicks > 0) tongueTicks--;
			else if (getRandom().nextInt(1800) == 0) tongueTicks = 60 + getRandom().nextInt(80);
			entityData.set(MOOD, (settled >= 200 ? SLEEPING : 0) | (playTicks > 0 ? PLAYING : 0) | (greeting > 0 ? GREETING : 0)
				| (ringTicks > 0 ? RINGING : 0) | (tongueTicks > 0 ? TONGUE : 0) | (bow ? BOW : 0));
		}
	}

	/** Her collar bell: three quick, high dings every 20 to 40 seconds, loud enough to find her by from a way off. */
	private void ring() {
		if (ringTicks > 0) {
			ringTicks--;
			int ding = RING_TICKS - 1 - ringTicks;
			if (ding % 3 == 0 && ding < 9) {
				level().playSound(null, getX(), getY() + 0.5, getZ(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL,
					net.minecraft.sounds.SoundSource.NEUTRAL, 1.6F - ding * 0.12F, 1.85F + getRandom().nextFloat() * 0.15F);
			}
			return;
		}
		if (--nextRing <= 0) {
			nextRing = 400 + getRandom().nextInt(400);
			if (CinnamonCompanion.bell()) ringTicks = RING_TICKS;
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.15, 8, 2));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 7));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		// Whoever lays a hand on her owner gets bitten (not while she's sitting, like any tame dog).
		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target == owner || target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) {
			return false;
		}
		if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() == owner) {
			return false;
		}
		return !(target instanceof Player victim && owner instanceof Player master && !master.canHarmPlayer(victim));
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
			ItemStack held = player.getItemInHand(hand);
			if (held.is(CinnamonContent.BOW) && !bow) {
				wearBow(player, true);
				held.consume(1, player);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.bow_on"));
				return InteractionResult.SUCCESS;
			}
			if (held.is(Items.SHEARS) && bow) {
				wearBow(player, false);
				ItemStack ribbon = new ItemStack(CinnamonContent.BOW);
				if (!player.getInventory().add(ribbon)) spawnAtLocation((ServerLevel) level(), ribbon);
				player.sendOverlayMessage(Component.translatable("message.wildercord.cinnamon.bow_off"));
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

	/** Sticks her tongue out for {@code ticks} and jingles her bell now (for the game tests' pictures). */
	public void showOff(int ticks) {
		tongueTicks = ticks;
		ringTicks = RING_TICKS;
	}

	/** Puts her bow on or takes it off, remembered with her owner. */
	private void wearBow(Player player, boolean on) {
		bow = on;
		settled = 0;
		player.setAttached(CinnamonState.BOW, on);
		level().playSound(null, getX(), getY(), getZ(), on ? net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value() : net.minecraft.sounds.SoundEvents.SHEARS_SNIP,
			net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.3F);
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
