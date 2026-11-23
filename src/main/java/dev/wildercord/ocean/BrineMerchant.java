package dev.wildercord.ocean;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Tidefolk Brine-Merchant inhabiting Coral-Dome Sanctuaries and coastal shelves.
 * Supports 3 specialized professions (Pearl Jeweler, Abyssal Cartographer, Kelp Alchemist),
 * breathes underwater, and never despawns.
 */
public class BrineMerchant extends WanderingTrader {
	private static final EntityDataAccessor<Integer> PROFESSION = SynchedEntityData.defineId(BrineMerchant.class, EntityDataSerializers.INT);

	public BrineMerchant(EntityType<? extends WanderingTrader> type, Level level) {
		super(type, level);
		setDespawnDelay(0);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.35);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PROFESSION, 0);
	}

	public OceanRules.MerchantProfession getMerchantProfession() {
		return OceanRules.MerchantProfession.fromId(entityData.get(PROFESSION));
	}

	public void setMerchantProfession(OceanRules.MerchantProfession prof) {
		entityData.set(PROFESSION, prof.id);
		setCustomName(Component.translatable("entity.wildercord.brine_merchant." + prof.key));
		setCustomNameVisible(true);
		this.offers = buildOffers(prof);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
		goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void customServerAiStep(net.minecraft.server.level.ServerLevel server) {
		if (getDespawnDelay() > 0) {
			setDespawnDelay(0);
		}
		super.customServerAiStep(server);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer && hand == InteractionHand.MAIN_HAND && !isTrading()) {
			if (this.offers == null || this.offers.isEmpty()) {
				this.offers = buildOffers(getMerchantProfession());
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	protected void updateTrades(net.minecraft.server.level.ServerLevel server) {
		this.offers = buildOffers(getMerchantProfession());
	}

	public static MerchantOffers buildOffers(OceanRules.MerchantProfession prof) {
		MerchantOffers offers = new MerchantOffers();
		switch (prof) {
			case PEARL_JEWELER -> {
				buy(offers, OceanItems.ABYSSAL_PEARL, 1, 5);
				sell(offers, 8, new ItemStack(OceanItems.ABYSSAL_PEARL), 16);
				offers.add(new MerchantOffer(
					new ItemCost(Items.EMERALD, 12),
					Optional.of(new ItemCost(OceanItems.ABYSSAL_PEARL, 1)),
					new ItemStack(OceanItems.SIREN_PEARL_LANTERN),
					8, 4, 0.05f
				));
				sell(offers, 6, new ItemStack(OceanItems.KELP_WRAPPED_SASHIMI, 2), 24);
				sell(offers, 3, new ItemStack(Items.PRISMARINE_CRYSTALS, 4), 32);
			}
			case ABYSSAL_CARTOGRAPHER -> {
				offers.add(new MerchantOffer(
					new ItemCost(Items.EMERALD, 14),
					Optional.of(new ItemCost(OceanItems.ABYSSAL_PEARL, 1)),
					new ItemStack(OceanItems.TIDECALLER_CONCH),
					4, 5, 0.05f
				));
				sell(offers, 9, new ItemStack(Items.NAUTILUS_SHELL), 12);
				sell(offers, 5, new ItemStack(Items.SPYGLASS), 12);
				sell(offers, 4, new ItemStack(Items.COMPASS), 16);
				sell(offers, 4, new ItemStack(Items.DARK_PRISMARINE, 16), 32);
			}
			case KELP_ALCHEMIST -> {
				sell(offers, 7, new ItemStack(OceanItems.SEA_SALT_RAMEN), 16);
				sell(offers, 5, new ItemStack(OceanItems.KELP_WRAPPED_SASHIMI, 2), 24);
				sell(offers, 8, new ItemStack(OceanItems.VOLTAIC_JELLY_SAC), 16);
				sell(offers, 14, new ItemStack(OceanItems.STAR_BARNACLE_AMBERGRIS), 8);
				sell(offers, 3, new ItemStack(Items.DRIED_KELP_BLOCK, 8), 32);
				buy(offers, Items.KELP, 16, 1);
			}
		}
		return offers;
	}

	private static void sell(MerchantOffers offers, int emeralds, ItemStack result, int maxUses) {
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), result, maxUses, 2, 0.05f));
	}

	private static void buy(MerchantOffers offers, Item item, int count, int emeralds) {
		offers.add(new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD, emeralds), 32, 2, 0.05f));
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setMerchantProfession(OceanRules.MerchantProfession.fromId(input.getIntOr("OceanProfession", 0)));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("OceanProfession", getMerchantProfession().id);
	}
}
