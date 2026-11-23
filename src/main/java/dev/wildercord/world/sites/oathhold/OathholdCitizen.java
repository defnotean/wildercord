package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.cooking.Meals;
import dev.wildercord.wildlife.WetlandGarden;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
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
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Articulated humanoid inhabitant of Oathhold spanning 16 distinct civic, guild, merchant,
 * scholarly, clerical, and working-class archetypes. Never despawns, stays tethered near its
 * assigned shop/plaza/street post, reacts to player Adventurer Rank &amp; Oathhold Wanted status,
 * and offers rank-discounted profession trades or Guild quest interactions.
 */
public class OathholdCitizen extends WanderingTrader {
	private static final EntityDataAccessor<Integer> DATA_ARCHETYPE =
		SynchedEntityData.defineId(OathholdCitizen.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SKIN_TONE =
		SynchedEntityData.defineId(OathholdCitizen.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_WAVE_TICK =
		SynchedEntityData.defineId(OathholdCitizen.class, EntityDataSerializers.INT);

	private String givenName = "Citizen";
	private int homeX;
	private int homeY;
	private int homeZ;
	private int tetherRadius = 12;
	private boolean hasHome;
	private boolean temporaryCrew;
	private @Nullable BlockPos repairTargetPos;
	private @Nullable UUID pursuitTargetId;
	private int dialogueIndex;

	public OathholdCitizen(EntityType<? extends WanderingTrader> type, Level level) {
		super(type, level);
		setDespawnDelay(0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.45)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ARCHETYPE, OathholdCitizenRules.Archetype.TOWNSFOLK.ordinal());
		builder.define(DATA_SKIN_TONE, 0);
		builder.define(DATA_WAVE_TICK, 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5, 0.55));
		goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		goalSelector.addGoal(2, new WatchmanPursuitGoal(this, 0.72));
		goalSelector.addGoal(2, new RepairWorkGoal(this, 0.62));
		goalSelector.addGoal(4, new HomeTetherGoal(this, 0.42));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.30));
		goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 0.5F));
		goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
	}

	public void triggerWorkSwing() {
		entityData.set(DATA_WAVE_TICK, 16);
	}

	public void assignRepairTarget(BlockPos pos) {
		this.repairTargetPos = pos.immutable();
	}

	public void clearRepairTarget() {
		this.repairTargetPos = null;
	}

	public void assignPursuit(UUID targetId) {
		this.pursuitTargetId = targetId;
	}

	public void clearPursuit() {
		this.pursuitTargetId = null;
	}

	public @Nullable UUID pursuitTargetId() {
		return pursuitTargetId;
	}

	public void markTemporaryCrew(boolean temp) {
		this.temporaryCrew = temp;
	}

	public boolean isTemporaryCrew() {
		return temporaryCrew;
	}

	public OathholdCitizenRules.Archetype archetype() {
		return OathholdCitizenRules.Archetype.byIndex(entityData.get(DATA_ARCHETYPE));
	}

	public int skinTone() {
		return entityData.get(DATA_SKIN_TONE);
	}

	public int waveTick() {
		return entityData.get(DATA_WAVE_TICK);
	}

	public void configure(OathholdCitizenRules.Archetype archetype, int lx, int ly, int lz, BlockPos worldHome, int tetherRadius) {
		OathholdCitizenRules.Identity id = OathholdCitizenRules.identityFor(lx, ly, lz, archetype);
		this.entityData.set(DATA_ARCHETYPE, archetype.ordinal());
		this.entityData.set(DATA_SKIN_TONE, id.skinTone());
		this.givenName = id.givenName();
		this.homeX = worldHome.getX();
		this.homeY = worldHome.getY();
		this.homeZ = worldHome.getZ();
		this.tetherRadius = Math.max(2, tetherRadius);
		this.hasHome = true;
		setCustomName(Component.literal(id.fullName()));
		setCustomNameVisible(false);
		setPersistenceRequired();
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && waveTick() > 0) {
			entityData.set(DATA_WAVE_TICK, waveTick() - 1);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		OathholdCitizenRules.Archetype role = archetype();
		if (role.essential && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			float allowed = Math.max(0.0F, getHealth() - 4.0F);
			boolean damaged = allowed > 0.0F && super.hurtServer(level, source, Math.min(amount, allowed));
			if (!damaged) {
				OathholdLaw.onEntityAttacked(level, this, source);
			}
			return damaged;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server) || hand != InteractionHand.MAIN_HAND) {
			return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		entityData.set(DATA_WAVE_TICK, 24);
		OathholdCitizenRules.Archetype role = archetype();

		if (OathholdQuests.tryDeliverToCitizen(server, this)) {
			return InteractionResult.SUCCESS;
		}

		if (OathholdLaw.isWanted(server)) {
			if ((role == OathholdCitizenRules.Archetype.PRIEST_CLERIC || role == OathholdCitizenRules.Archetype.CITY_WATCHMAN)
					&& (server.isSecondaryUseActive() || server.getMainHandItem().is(Items.EMERALD))) {
				OathholdLaw.tryPayFine(server);
				return InteractionResult.SUCCESS;
			}
			String warn = OathholdCitizenRules.dialogue(role, OathholdQuests.rank(server), true, dialogueIndex++);
			server.sendSystemMessage(Component.literal("[" + givenName + "] " + warn).withStyle(ChatFormatting.RED));
			level().playSound(null, blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.8F, 1.0F);
			return InteractionResult.SUCCESS;
		}

		if (role == OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST || role == OathholdCitizenRules.Archetype.GUILD_MASTER) {
			if (server.isSecondaryUseActive()) {
				String line = OathholdCitizenRules.dialogue(role, OathholdQuests.rank(server), false, dialogueIndex++);
				server.sendSystemMessage(Component.literal("[" + givenName + "] " + line).withStyle(ChatFormatting.GOLD));
				return InteractionResult.SUCCESS;
			}
			OathholdQuests.openBoard(server, blockPosition());
			return InteractionResult.SUCCESS;
		}

		if (role == OathholdCitizenRules.Archetype.ARENA_PROMOTER) {
			if (OathholdQuests.tryStartArenaBout(server, (ServerLevel) level(), blockPosition())) {
				return InteractionResult.SUCCESS;
			}
			OathholdQuests.openBoard(server, blockPosition());
			return InteractionResult.SUCCESS;
		}

		if (role.merchant && !isTrading()) {
			OathholdQuestRules.Rank rank = OathholdQuests.rank(server);
			this.offers = buildOffers(role, rank);
			server.sendOverlayMessage(Component.literal(givenName + " — " + role.title + " (Guild Rank " + rank.name() + " Rates)")
				.withStyle(ChatFormatting.GOLD));
			return super.mobInteract(player, hand);
		}

		String line = OathholdCitizenRules.dialogue(role, OathholdQuests.rank(server), false, dialogueIndex++);
		server.sendSystemMessage(Component.literal("[" + givenName + " — " + role.title + "] " + line).withStyle(ChatFormatting.WHITE));
		level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.7F, 1.05F);
		return InteractionResult.SUCCESS;
	}

	/** Builds rank-discounted trades tailored to each merchant profession. */
	public static MerchantOffers buildOffers(OathholdCitizenRules.Archetype role, OathholdQuestRules.Rank rank) {
		MerchantOffers offers = new MerchantOffers();
		switch (role) {
			case BLACKSMITH -> {
				buy(offers, Items.RAW_IRON, 12);
				buy(offers, Items.COAL, 16);
				sell(offers, rank, 4, new ItemStack(Items.IRON_SWORD));
				sell(offers, rank, 5, new ItemStack(Items.IRON_AXE));
				sell(offers, rank, 4, new ItemStack(Items.SHIELD));
				sell(offers, rank, 8, new ItemStack(Items.IRON_CHESTPLATE));
				sell(offers, rank, 3, new ItemStack(WildercordItems.BLANK_RUNE, 2));
			}
			case ALCHEMIST_HERBALIST -> {
				buy(offers, Items.DANDELION, 12);
				buy(offers, Items.POPPY, 12);
				buy(offers, Items.PINK_PETALS, 12);
				buy(offers, WetlandGarden.FLOSS, 4);
				sell(offers, rank, 2, new ItemStack(Items.GLOW_BERRIES, 6));
				sell(offers, rank, 3, new ItemStack(Items.HONEY_BOTTLE, 2));
				sell(offers, rank, 4, new ItemStack(Items.GOLDEN_CARROT, 3));
				sell(offers, rank, 6, new ItemStack(WildercordItems.MANA_CRYSTAL));
			}
			case BAKER_COOK -> {
				buy(offers, Items.WHEAT, 20);
				buy(offers, Items.POTATO, 16);
				sell(offers, rank, 2, new ItemStack(Items.BREAD, 6));
				sell(offers, rank, 3, new ItemStack(Items.BAKED_POTATO, 6));
				sell(offers, rank, 3, new ItemStack(Items.PUMPKIN_PIE, 3));
				addMealOffer(offers, rank, 4, "hearty_stew");
				addMealOffer(offers, rank, 4, "forager_soup");
				addMealOffer(offers, rank, 5, "hunters_skewer");
			}
			case CLOTHIER_TAILOR -> {
				buy(offers, Items.STRING, 16);
				buy(offers, Items.LEATHER, 8);
				sell(offers, rank, 3, new ItemStack(Items.LEATHER_CHESTPLATE));
				sell(offers, rank, 2, new ItemStack(Items.LEAD, 2));
				sell(offers, rank, 6, new ItemStack(Items.SADDLE));
				sell(offers, rank, 5, new ItemStack(Items.NAME_TAG));
			}
			case FISHMONGER_SAILOR -> {
				buy(offers, Items.SALMON, 8);
				buy(offers, Items.COD, 10);
				sell(offers, rank, 2, new ItemStack(Items.COOKED_SALMON, 4));
				sell(offers, rank, 2, new ItemStack(Items.COOKED_COD, 5));
				sell(offers, rank, 3, new ItemStack(Items.OAK_BOAT));
				sell(offers, rank, 5, new ItemStack(Items.COMPASS));
				sell(offers, rank, 7, new ItemStack(Items.NAUTILUS_SHELL));
			}
			case SCHOLAR_SCRIBE -> {
				buy(offers, Items.PAPER, 24);
				buy(offers, Items.LAPIS_LAZULI, 12);
				sell(offers, rank, 3, new ItemStack(Items.BOOK, 3));
				sell(offers, rank, 5, new ItemStack(Items.BOOKSHELF, 2));
				sell(offers, rank, 4, new ItemStack(Items.INK_SAC, 6));
				sell(offers, rank, 5, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
				sell(offers, rank, 3, new ItemStack(WildercordItems.BLANK_RUNE, 2));
			}
			case JEWELER_BANKER -> {
				buy(offers, Items.GOLD_INGOT, 4);
				sell(offers, rank, 2, new ItemStack(Items.GOLD_INGOT, 2));
				sell(offers, rank, 4, new ItemStack(Items.AMETHYST_SHARD, 4));
				sell(offers, rank, 6, new ItemStack(Items.CLOCK));
				sell(offers, rank, 8, new ItemStack(AuraWorld.AURA_SHARD));
			}
			case CARPENTER_MASON -> {
				buy(offers, Items.CLAY_BALL, 20);
				sell(offers, rank, 2, new ItemStack(Items.SPRUCE_PLANKS, 16));
				sell(offers, rank, 2, new ItemStack(Items.STONE_BRICKS, 16));
				sell(offers, rank, 3, new ItemStack(Items.LANTERN, 4));
				sell(offers, rank, 4, new ItemStack(Items.BRUSH));
			}
			case PRIEST_CLERIC -> {
				buy(offers, Items.ROTTEN_FLESH, 24);
				sell(offers, rank, 2, new ItemStack(Items.BREAD, 6));
				sell(offers, rank, 4, new ItemStack(Items.EXPERIENCE_BOTTLE, 3));
				sell(offers, rank, 8, new ItemStack(Items.GOLDEN_APPLE));
			}
			default -> {
			}
		}
		return offers;
	}

	private static void addMealOffer(MerchantOffers offers, OathholdQuestRules.Rank rank, int emeralds, String mealId) {
		Item meal = Meals.MEALS.get(mealId);
		if (meal != null) {
			sell(offers, rank, emeralds, new ItemStack(meal));
		}
	}

	private static void sell(MerchantOffers offers, OathholdQuestRules.Rank rank, int baseEmeralds, ItemStack stack) {
		int price = rank.tradeDiscountEmeralds(baseEmeralds);
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, price), stack, 64, 0, 0.0F));
	}

	private static void buy(MerchantOffers offers, Item item, int count) {
		offers.add(new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), 64, 0, 0.0F));
	}

	@Override
	protected void updateTrades(ServerLevel level) {
	}

	@Override
	protected void rewardTradeXp(MerchantOffer offer) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("Archetype", archetype().id);
		output.putInt("SkinTone", skinTone());
		output.putString("GivenName", givenName);
		output.putBoolean("HasHome", hasHome);
		output.putInt("HomeX", homeX);
		output.putInt("HomeY", homeY);
		output.putInt("HomeZ", homeZ);
		output.putInt("TetherRadius", tetherRadius);
		output.putBoolean("OathholdTempCrew", temporaryCrew);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		OathholdCitizenRules.Archetype role = OathholdCitizenRules.Archetype.ofId(input.getStringOr("Archetype", "townsfolk"));
		entityData.set(DATA_ARCHETYPE, role.ordinal());
		entityData.set(DATA_SKIN_TONE, input.getIntOr("SkinTone", 0));
		givenName = input.getStringOr("GivenName", "Citizen");
		hasHome = input.getBooleanOr("HasHome", false);
		homeX = input.getIntOr("HomeX", 0);
		homeY = input.getIntOr("HomeY", 64);
		homeZ = input.getIntOr("HomeZ", 0);
		tetherRadius = Math.max(2, input.getIntOr("TetherRadius", 12));
		temporaryCrew = input.getBooleanOr("OathholdTempCrew", false);
		setPersistenceRequired();
	}

	private static final class WatchmanPursuitGoal extends Goal {
		private final OathholdCitizen mob;
		private final double speed;

		WatchmanPursuitGoal(OathholdCitizen mob, double speed) {
			this.mob = mob;
			this.speed = speed;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return mob.pursuitTargetId != null && mob.level() instanceof ServerLevel sl
				&& sl.getServer().getPlayerList().getPlayer(mob.pursuitTargetId) != null;
		}

		@Override
		public boolean canContinueToUse() {
			if (mob.pursuitTargetId == null || !(mob.level() instanceof ServerLevel sl)) return false;
			ServerPlayer target = sl.getServer().getPlayerList().getPlayer(mob.pursuitTargetId);
			if (target == null || !target.isAlive() || !OathholdJustice.isPersecuted(target)) {
				mob.pursuitTargetId = null;
				return false;
			}
			return true;
		}

		@Override
		public void tick() {
			if (!(mob.level() instanceof ServerLevel sl) || mob.pursuitTargetId == null) return;
			ServerPlayer target = sl.getServer().getPlayerList().getPlayer(mob.pursuitTargetId);
			if (target == null || !OathholdJustice.isPersecuted(target)) {
				mob.pursuitTargetId = null;
				return;
			}
			mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
			mob.getNavigation().moveTo(target, speed);
		}
	}

	private static final class RepairWorkGoal extends Goal {
		private final OathholdCitizen mob;
		private final double speed;

		RepairWorkGoal(OathholdCitizen mob, double speed) {
			this.mob = mob;
			this.speed = speed;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return mob.repairTargetPos != null;
		}

		@Override
		public boolean canContinueToUse() {
			return mob.repairTargetPos != null;
		}

		@Override
		public void tick() {
			BlockPos pos = mob.repairTargetPos;
			if (pos == null) return;
			mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30.0F, 30.0F);
			double d2 = mob.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
			if (d2 > 6.25) {
				mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, speed);
			} else {
				mob.getNavigation().stop();
			}
		}
	}

	/** Version-independent stroll tether keeping merchants and guards near their assigned post. */
	private static final class HomeTetherGoal extends Goal {
		private final OathholdCitizen mob;
		private final double speed;

		HomeTetherGoal(OathholdCitizen mob, double speed) {
			this.mob = mob;
			this.speed = speed;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (!mob.hasHome || mob.repairTargetPos != null || mob.pursuitTargetId != null) return false;
			double dx = mob.getX() - (mob.homeX + 0.5);
			double dy = mob.getY() - mob.homeY;
			double dz = mob.getZ() - (mob.homeZ + 0.5);
			return dx * dx + dy * dy + dz * dz > (double) (mob.tetherRadius * mob.tetherRadius);
		}

		@Override
		public boolean canContinueToUse() {
			return !mob.getNavigation().isDone() && canUse();
		}

		@Override
		public void start() {
			mob.getNavigation().moveTo(mob.homeX + 0.5, mob.homeY, mob.homeZ + 0.5, speed);
		}
	}
}
