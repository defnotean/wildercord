package dev.wildercord.wildlife;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Custom Japanese Sakura Village resident ({@code Tea Master} or {@code Shrine Maiden}) wearing traditional
 * kimono &amp; obi attire. Greets travellers with a bow and exchanges emeralds for {@code Suzu Cat-Bell Charms}
 * and warm fish treats for Yuki, Odie, Freya, and Kuro.
 */
public class SakuraVillager extends PathfinderMob {
	public static final int TEA_MASTER = 0;
	public static final int SHRINE_MAIDEN = 1;

	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(SakuraVillager.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_BOW_TICK = SynchedEntityData.defineId(SakuraVillager.class, EntityDataSerializers.INT);

	private static final String[] TEA_LINES = {
		"Matcha is steeped and the lanterns are lit. Yuki sleeps best by the mossy brook.",
		"Odie will nap on any sunbeam floorboard in the Cat Sanctuary — bring fish, not fury.",
		"Offer me an emerald if you need a Suzu Cat-Bell Charm for the four shrine cats!"
	};

	private static final String[] MAIDEN_LINES = {
		"Freya's pink collar bell guards the third floor of the Cat Sanctuary; listen for her chime.",
		"Kuro's notched left ear is the mark of the roof's Shadow Sentinel. Best him gently to earn all four cats' trust.",
		"Our Sakura Village welcomes all casters seeking peace between battles."
	};

	private int greetingLine;

	public SakuraVillager(EntityType<? extends SakuraVillager> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ROLE, TEA_MASTER);
		builder.define(DATA_BOW_TICK, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
			.add(Attributes.MAX_HEALTH, 24.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.65));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public int role() {
		return entityData.get(DATA_ROLE);
	}

	public void configure(int role) {
		entityData.set(DATA_ROLE, role == SHRINE_MAIDEN ? SHRINE_MAIDEN : TEA_MASTER);
		setCustomName(Component.literal(role == SHRINE_MAIDEN ? "Sakura Shrine Maiden" : "Sakura Tea Master"));
		setPersistenceRequired();
	}

	public int bowTick() {
		return entityData.get(DATA_BOW_TICK);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && bowTick() > 0) {
			entityData.set(DATA_BOW_TICK, bowTick() - 1);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, amount);
		}
		return false;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide()) {
			entityData.set(DATA_BOW_TICK, 26);
			ItemStack held = player.getItemInHand(hand);
			if (held.is(Items.EMERALD)) {
				held.consume(1, player);
				ItemStack reward = role() == TEA_MASTER
					? new ItemStack(ShrineCatContent.CAT_BELL_CHARM, 1)
					: new ItemStack(Items.COOKED_SALMON, 4);
				if (!player.getInventory().add(reward)) {
					spawnAtLocation((ServerLevel) level(), reward);
				}
				level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.35F);
				player.sendSystemMessage(Component.literal("[" + (role() == TEA_MASTER ? "Tea Master" : "Shrine Maiden") + "] Please take this gift with our blessings."));
			} else {
				String[] lines = role() == SHRINE_MAIDEN ? MAIDEN_LINES : TEA_LINES;
				String line = lines[greetingLine++ % lines.length];
				level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.7F, 1.15F);
				player.sendSystemMessage(Component.literal("[" + (role() == TEA_MASTER ? "Tea Master" : "Shrine Maiden") + "] " + line));
			}
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("SakuraRole", role());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		configure(input.getIntOr("SakuraRole", TEA_MASTER));
	}
}
