package dev.wildercord.town;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.cooking.Meals;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
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

import java.util.Optional;
import java.util.Random;

/**
 * A keeper at a Wayfarer Inn: the cook, the stablemaster or a Master's emissary. Stays home, never leaves, can't be hurt, and
 * sells more as the traveller's {@linkplain BountyRules.Tier standing} with the inn grows (earned at its bounty board).
 */
public class WayfarerKeeper extends WanderingTrader {
	public enum Role {
		COOK("cook"), STABLEMASTER("stablemaster"), EMISSARY("emissary"),
		/** A wandering caravan's trader (0.13): comes to the traveller instead, and moves on after a day. See {@link Caravans}. */
		CARAVANEER("caravaneer");

		public final String id;

		Role(String id) {
			this.id = id;
		}

		static Role of(String id) {
			for (Role role : values()) if (role.id.equals(id)) return role;
			return COOK;
		}
	}

	private Role role = Role.COOK;
	/** A caravaneer's stock for each traveller, kept for its stay so a rune once bought stays bought. */
	private final java.util.Map<java.util.UUID, MerchantOffers> stock = new java.util.HashMap<>();

	public WayfarerKeeper(EntityType<? extends WanderingTrader> type, Level level) {
		super(type, level);
		setDespawnDelay(0);
	}

	public Role role() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
		setCustomName(Component.translatable("entity.wildercord.wayfarer_keeper." + role.id));
		setCustomNameVisible(true);
		setDespawnDelay(role == Role.CARAVANEER ? CaravanRules.STAY_TICKS : 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5, 0.5));
		goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
		goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
		goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, net.minecraft.world.entity.Mob.class, 8.0F));
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive() && role != Role.CARAVANEER) {
			InnRooms.open(server);
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer server && hand == InteractionHand.MAIN_HAND && !isTrading()) {
			BountyRules.Tier tier = Town.standing(server).tier();
			long seed = getUUID().getLeastSignificantBits() ^ level().getGameTime() / BountyRules.DAY;
			offers = role == Role.CARAVANEER
				? stock.computeIfAbsent(server.getUUID(), id -> offers(role, tier, getUUID().getMostSignificantBits() ^ id.getLeastSignificantBits()))
				: offers(role, tier, seed);
			BountyRules.Tier next = tier.next();
			server.sendOverlayMessage(next == null
				? Component.translatable("message.wildercord.keeper.honoured").withStyle(ChatFormatting.AQUA)
				: Component.translatable("message.wildercord.keeper.more", BountyBoardBlock.tierName(next)).withStyle(ChatFormatting.GRAY));
		}
		return super.mobInteract(player, hand);
	}

	/** What a keeper in this role sells a traveller of this standing. */
	public static MerchantOffers offers(Role role, BountyRules.Tier tier, long seed) {
		MerchantOffers offers = new MerchantOffers();
		int t = tier.ordinal();
		switch (role) {
			case COOK -> {
				buy(offers, Items.POTATO, 16);
				buy(offers, Items.CARROT, 16);
				buy(offers, Items.BEETROOT, 12);
				sell(offers, 4, new ItemStack(Meals.CAMP_POT));
				sell(offers, InnRoomRules.price(tier), new ItemStack(Town.ROOM_KEY));
				meals(offers, 3, "hearty_stew", "forager_soup", "hunters_skewer");
				if (t >= 1) meals(offers, 4, "wanderers_stew", "salmon_chowder", "honeyed_ham");
				if (t >= 2) meals(offers, 5, "emberroot_curry", "mana_risotto", "duelists_broth");
				if (t >= 3) meals(offers, 6, "starlit_consomme", "scholars_tea");
			}
			case STABLEMASTER -> {
				buy(offers, Items.WHEAT, 20);
				sell(offers, 2, new ItemStack(Items.LEAD));
				sell(offers, 2, new ItemStack(Items.HAY_BLOCK, 2));
				sell(offers, 6, new ItemStack(Items.SADDLE));
				if (t >= 1) {
					sell(offers, 3, new ItemStack(Items.GOLDEN_CARROT, 3));
					sell(offers, 5, new ItemStack(Items.NAME_TAG));
				}
				if (t >= 2) {
					sell(offers, 20, new ItemStack(Town.RIDGEBACK_DEED));
					sell(offers, 8, new ItemStack(Items.IRON_HORSE_ARMOR));
				}
				if (t >= 3) {
					sell(offers, 18, new ItemStack(Items.DIAMOND_HORSE_ARMOR));
					sell(offers, 8, new ItemStack(Items.GOLDEN_APPLE));
				}
			}
			case EMISSARY -> {
				sell(offers, 3, new ItemStack(WildercordItems.BLANK_RUNE));
				sell(offers, 6, new ItemStack(WildercordItems.MANA_CRYSTAL));
				if (t >= 1) sell(offers, 10, new ItemStack(AuraWorld.AURA_SHARD));
				if (t >= 2) sell(offers, 4, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
				if (t >= 3) {
					AuraApi.drawScrollPart("sword_tomb", new Random(seed)).ifPresent(part -> offers.add(new MerchantOffer(
						new ItemCost(Items.EMERALD, 24), Optional.of(new ItemCost(AuraWorld.AURA_SHARD, 4)), AuraApi.techniqueScroll(part), 1, 0, 0.0F)));
				}
			}
			case CARAVANEER -> {
				Random random = new Random(seed);
				for (int i = 0; i < CaravanRules.runes(tier); i++) {
					int runeTier = CaravanRules.runeTier(tier, random.nextDouble());
					sell(offers, CaravanRules.runePrice(runeTier), dev.wildercord.content.RuneItem.stack(
						dev.wildercord.cast.events.EventRules.rewardRune("caravan", runeTier, random.nextDouble())), 1);
				}
				sell(offers, 5, new ItemStack(Items.SADDLE));
				sell(offers, 3, new ItemStack(Items.LEAD, 2));
				sell(offers, 7, new ItemStack(Items.IRON_HORSE_ARMOR));
				if (t >= 2) sell(offers, 18, new ItemStack(Town.RIDGEBACK_DEED));
				sell(offers, 3, new ItemStack(Items.BLAZE_POWDER, 2));
				sell(offers, CaravanRules.SAFFRON_PRICE, new ItemStack(Meals.WAYFARER_SAFFRON, CaravanRules.SAFFRON));
				sell(offers, 2, new ItemStack(Items.CHORUS_FRUIT, 4));
				sell(offers, 2, new ItemStack(Items.GLOW_BERRIES, 8));
				sell(offers, 2, new ItemStack(Items.COCOA_BEANS, 8));
				buy(offers, Items.LEATHER, 8);
			}
		}
		return offers;
	}

	private static void meals(MerchantOffers offers, int price, String... ids) {
		for (String id : ids) {
			Item meal = Meals.MEALS.get(id);
			if (meal != null) sell(offers, price, new ItemStack(meal));
		}
	}

	private static void sell(MerchantOffers offers, int emeralds, ItemStack stack) {
		sell(offers, emeralds, stack, 64);
	}

	private static void sell(MerchantOffers offers, int emeralds, ItemStack stack, int uses) {
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), stack, uses, 0, 0.0F));
	}

	private static void buy(MerchantOffers offers, Item item, int count) {
		offers.add(new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), 64, 0, 0.0F));
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		// Rebuilt for each traveller as they open the trade, from their standing.
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Role", role.id);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		role = Role.of(input.getStringOr("Role", Role.COOK.id));
		if (role != Role.CARAVANEER) setDespawnDelay(0);
	}
}
