package dev.wildercord.wildlife;

import dev.wildercord.content.WildercordItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import dev.wildercord.compat.McValueInput;
import dev.wildercord.compat.McValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;

/**
 * Custom Japanese Sakura Village residents across six professions ({@code Tea Master},
 * {@code Shrine Maiden}, {@code Katana Swordsmith}, {@code Lantern Artisan},
 * {@code Onsen Hostess}, and {@code Koi Canal Fisherman}). Greets travellers with a bow
 * and trades role-themed supplies for emeralds.
 */
public class SakuraVillager extends PathfinderMob {
	public static final int TEA_MASTER = 0;
	public static final int SHRINE_MAIDEN = 1;
	public static final int SWORDSMITH = 2;
	public static final int LANTERN_MAKER = 3;
	public static final int ONSEN_KEEPER = 4;
	public static final int FISHERMAN = 5;
	public static final int ROLE_COUNT = 6;

	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(SakuraVillager.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_BOW_TICK = SynchedEntityData.defineId(SakuraVillager.class, EntityDataSerializers.INT);

	private static final String[] FULL_NAMES = {
		"Sakura Tea Master",
		"Sakura Shrine Maiden",
		"Sakura Swordsmith",
		"Sakura Lantern Artisan",
		"Sakura Onsen Hostess",
		"Sakura Koi Fisherman"
	};

	private static final String[] SHORT_NAMES = {
		"Tea Master",
		"Shrine Maiden",
		"Swordsmith",
		"Lantern Artisan",
		"Onsen Hostess",
		"Koi Fisherman"
	};

	private static final String[][] ROLE_LINES = {
		{
			"Matcha is steeped and the lanterns are lit. Yuki sleeps best by the mossy brook.",
			"Odie will nap on any sunbeam floorboard in the Cat Sanctuary — bring fish, not fury.",
			"Offer me an emerald if you need a Suzu Cat-Bell Charm for the four shrine cats!"
		},
		{
			"Freya's pink collar bell guards the third floor of the Cat Sanctuary; listen for her chime.",
			"Kuro's notched left ear is the mark of the roof's Shadow Sentinel. Best him gently to earn all four cats' trust.",
			"Our Sakura Villages welcome all casters seeking peace between battles."
		},
		{
			"White steel folded under cherry petals keeps both blade and rune tablet true.",
			"I trade Blank Runes tempered at the Castle-Town dojo forge — one emerald for four!",
			"Beware the Karakuri Sentinels in the Cat Sanctuary; their clockwork shields blunt careless strikes."
		},
		{
			"Every Paper Lantern Spirit remembers the craft of old chochin-makers gone astray.",
			"Bring an emerald and I will string fresh iron lanterns for your camp or canal pier.",
			"From the Canal Harbor to the Castle-Town arcade, warm paper light keeps the mist away."
		},
		{
			"Step into the steaming rotenburo pools of our Onsen Village to wash away long travel.",
			"Towel folded, cedar tub filled — offer an emerald for crisp golden carrots from the garden.",
			"Odie loves napping beside the warm bath stones while steam rises into the blossoms."
		},
		{
			"The koi swim thick below the crimson Taiko-bashi bridges of our Canal Harbor.",
			"Fresh raw salmon is every shrine cat's favorite — trade me an emerald if your creel is empty!",
			"Keep an eye out by the watermill wheel; Yuki often watches the ripples from the pier."
		}
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

	@Override
	public  SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			 SpawnGroupData data) {
		SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
		if ((reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) && !hasCustomName()) {
			configure(random.nextInt(ROLE_COUNT));
		}
		return out;
	}

	public int role() {
		return Math.floorMod(entityData.get(DATA_ROLE), ROLE_COUNT);
	}

	public void configure(int role) {
		int r = Math.floorMod(role, ROLE_COUNT);
		entityData.set(DATA_ROLE, r);
		setCustomName(Component.literal(FULL_NAMES[r]));
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

	private static ItemStack emeraldReward(int role) {
		return switch (role) {
			case TEA_MASTER -> new ItemStack(ShrineCatContent.CAT_BELL_CHARM, 1);
			case SHRINE_MAIDEN -> new ItemStack(Items.COOKED_SALMON, 4);
			case SWORDSMITH -> new ItemStack(WildercordItems.BLANK_RUNE, 4);
			case LANTERN_MAKER -> new ItemStack(Items.LANTERN, 4);
			case ONSEN_KEEPER -> new ItemStack(Items.GOLDEN_CARROT, 3);
			default -> new ItemStack(Items.SALMON, 4);
		};
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide()) {
			entityData.set(DATA_BOW_TICK, 26);
			int r = role();
			ItemStack held = player.getItemInHand(hand);
			if (held.is(Items.EMERALD)) {
				held.consume(1, player);
				ItemStack reward = emeraldReward(r);
				if (!player.getInventory().add(reward)) {
					spawnAtLocation((ServerLevel) level(), reward);
				}
				level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.35F);
				player.displayClientMessage(Component.literal("[" + SHORT_NAMES[r] + "] Please take this gift with our blessings."), false);
			} else {
				String[] lines = ROLE_LINES[r];
				String line = lines[greetingLine++ % lines.length];
				level().playSound(null, blockPosition(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.7F, 1.15F);
				player.displayClientMessage(Component.literal("[" + SHORT_NAMES[r] + "] " + line), false);
			}
		}
		return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag outputTag) { McValueOutput output = new McValueOutput(outputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.addAdditionalSaveData(outputTag);
		output.putInt("SakuraRole", role());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag inputTag) { McValueInput input = new McValueInput(inputTag, (level() == null ? RegistryAccess.EMPTY : level().registryAccess()));
		super.readAdditionalSaveData(inputTag);
		configure(input.getIntOr("SakuraRole", TEA_MASTER));
	}
}
