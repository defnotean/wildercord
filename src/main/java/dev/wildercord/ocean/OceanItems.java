package dev.wildercord.ocean;

import com.mojang.serialization.Codec;
import dev.wildercord.ServerState;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.compat.McArmor;
import dev.wildercord.compat.McHands;
import dev.wildercord.net.ServerDisconnects;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Registers all Deep Ocean Expansion equipment, 3D weapons/relics, materials, seafood meals,
 * custom block items, diving suit oxygen/attribute logic, and Tidecaller Conch air domes.
 */
public final class OceanItems {
	private OceanItems() {}

	public static final Holder<ArmorMaterial> ABYSSAL_SUIT_MATERIAL = McArmor.material(
		Map.of(
			ArmorItem.Type.HELMET, OceanRules.HELM_DEFENSE,
			ArmorItem.Type.CHESTPLATE, OceanRules.CHEST_DEFENSE,
			ArmorItem.Type.LEGGINGS, OceanRules.LEGS_DEFENSE,
			ArmorItem.Type.BOOTS, OceanRules.FEET_DEFENSE
		),
		OceanRules.SUIT_ENCHANTABILITY, OceanRules.SUIT_TOUGHNESS, OceanRules.SUIT_KNOCKBACK_RES,
		null, dev.wildercord.compat.McSpawnEgg.armorModel("wildercord:abyssal_diving")
	);

	// ---- Armor Set ----
	public static final Item ABYSSAL_DIVING_HELM = armor("abyssal_diving_helm", ArmorItem.Type.HELMET);
	public static final Item BATHYSPHERE_CHESTPLATE = armor("bathysphere_chestplate", ArmorItem.Type.CHESTPLATE);
	public static final Item KELP_WEAVE_GREAVES = armor("kelp_weave_greaves", ArmorItem.Type.LEGGINGS);
	public static final Item TRENCH_BOOTS = armor("trench_boots", ArmorItem.Type.BOOTS);

	// ---- Weapons & Relics ----
	public static final Item TIDE_CLEAVER_HARPOON = register("tide_cleaver_harpoon",
		p -> new HarpoonItem(p.attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.NETHERITE, 4, OceanRules.HARPOON_ATTACK_SPEED))
			.durability(1600).rarity(Rarity.EPIC)));
	public static final Item LEVIATHAN_GLAIVE = register("leviathan_glaive",
		p -> new GlaiveItem(p.attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.NETHERITE, 6, OceanRules.GLAIVE_ATTACK_SPEED))
			.durability(2048).rarity(Rarity.EPIC)));
	public static final Item SIREN_PEARL_LANTERN = register("siren_pearl_lantern",
		p -> new SirenLanternBlockItem(p.rarity(Rarity.RARE)));
	public static final Item TIDECALLER_CONCH = register("tidecaller_conch",
		p -> new ConchItem(p.stacksTo(1).durability(128).rarity(Rarity.RARE)));

	// ---- Block Items ----
	public static final Item TRENCH_ANVIL = register("trench_anvil",
		p -> new LoreBlockItem("trench_anvil", OceanBlocks.TRENCH_ANVIL, p.rarity(Rarity.RARE)));
	public static final Item SOVEREIGN_ALTAR = register("sovereign_altar",
		p -> new LoreBlockItem("sovereign_altar", OceanBlocks.SOVEREIGN_ALTAR, p.rarity(Rarity.EPIC)));

	// ---- Materials & Sea Cuisine ----
	public static final Item ABYSSAL_PEARL = lore("abyssal_pearl", Rarity.UNCOMMON);
	public static final Item LEVIATHAN_HEART_PEARL = lore("leviathan_heart_pearl", Rarity.EPIC);
	public static final Item CORAL_EDGE_FANG = lore("coral_edge_fang", Rarity.UNCOMMON);
	public static final Item VOLTAIC_JELLY_SAC = lore("voltaic_jelly_sac", Rarity.UNCOMMON);
	public static final Item ABYSSAL_IRON_PLATE = lore("abyssal_iron_plate", Rarity.RARE);
	public static final Item STAR_BARNACLE_AMBERGRIS = lore("star_barnacle_ambergris", Rarity.RARE);

	public static final Item KELP_WRAPPED_SASHIMI = register("kelp_wrapped_sashimi",
		p -> new AuraWorld.Lore("kelp_wrapped_sashimi", p
			.food(new FoodProperties.Builder()
				.nutrition(OceanRules.SASHIMI_NUTRITION)
				.saturationModifier(OceanRules.SASHIMI_SATURATION)
				.alwaysEdible()
				.effect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 180, 0), 1.0F)
				.effect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 200, 0), 1.0F)
				.build())
			.rarity(Rarity.UNCOMMON)));

	public static final Item SEA_SALT_RAMEN = register("sea_salt_ramen",
		p -> new AuraWorld.Lore("sea_salt_ramen", p
			.stacksTo(16)
			.food(new FoodProperties.Builder()
				.nutrition(OceanRules.RAMEN_NUTRITION)
				.saturationModifier(OceanRules.RAMEN_SATURATION)
				.alwaysEdible()
				.effect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0), 1.0F)
				.effect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1), 1.0F)
				.effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0), 1.0F)
				.build())
			.rarity(Rarity.RARE)));

	// ---- Player Oxygen Reserve & Active Conch Domes ----
	public static final AttachmentType<Integer> TANK_OXYGEN = AttachmentRegistry.create(
		Wildercord.id("abyssal_tank_oxygen"),
		b -> b.initializer(() -> OceanRules.CHEST_OXYGEN_MAX).persistent(Codec.INT).copyOnDeath()
	);

	private record ActiveDome(ResourceKey<Level> dimension, Vec3 center, int expiresTick) {}
	private static final Map<UUID, ActiveDome> ACTIVE_DOMES = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> LAST_GLAIVE_CHECK = new ConcurrentHashMap<>();

	private static final ResourceLocation REACH_MOD = Wildercord.id("leviathan_glaive_reach");
	private static final ResourceLocation GREAVES_MOD = Wildercord.id("kelp_weave_greaves_swim");
	private static final ResourceLocation BOOTS_MOD = Wildercord.id("trench_boots_mining");
	private static final ResourceLocation BOOTS_STEP_MOD = Wildercord.id("trench_boots_step");

	private static Item armor(String id, ArmorItem.Type type) {
		return register(id, p -> McArmor.loreArmor(id, ABYSSAL_SUIT_MATERIAL, (ArmorItem.Type) type, p.rarity(Rarity.RARE)));
	}

	private static Item lore(String id, Rarity rarity) {
		return register(id, p -> new AuraWorld.Lore(id, p.rarity(rarity)));
	}

	private static Item register(String id, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties()));
	}

	public static int wornSuitPieces(LivingEntity entity) {
		int count = 0;
		if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ABYSSAL_DIVING_HELM)) count++;
		if (entity.getItemBySlot(EquipmentSlot.CHEST).is(BATHYSPHERE_CHESTPLATE)) count++;
		if (entity.getItemBySlot(EquipmentSlot.LEGS).is(KELP_WEAVE_GREAVES)) count++;
		if (entity.getItemBySlot(EquipmentSlot.FEET).is(TRENCH_BOOTS)) count++;
		return count;
	}

	public static boolean isAbyssalRepairable(ItemStack stack) {
		return stack.is(ABYSSAL_DIVING_HELM)
			|| stack.is(BATHYSPHERE_CHESTPLATE)
			|| stack.is(KELP_WEAVE_GREAVES)
			|| stack.is(TRENCH_BOOTS)
			|| stack.is(TIDE_CLEAVER_HARPOON)
			|| stack.is(LEVIATHAN_GLAIVE);
	}

	public static void init() {
		ServerState.resetOnStop(ACTIVE_DOMES);
		ServerState.resetOnStop(LAST_GLAIVE_CHECK);
		ServerState.forgetOnDisconnect(LAST_GLAIVE_CHECK);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long tick = server.overworld().getGameTime();
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayerOceanGear(player, tick);
			}
			tickConchDomes(server);
		});

		ServerDisconnects.register((handler, server) -> removeModifiers(handler.player));

		ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(out -> List.of(
				ABYSSAL_DIVING_HELM,
				BATHYSPHERE_CHESTPLATE,
				KELP_WEAVE_GREAVES,
				TRENCH_BOOTS,
				TIDE_CLEAVER_HARPOON,
				LEVIATHAN_GLAIVE,
				SIREN_PEARL_LANTERN,
				TIDECALLER_CONCH,
				TRENCH_ANVIL,
				SOVEREIGN_ALTAR,
				ABYSSAL_PEARL,
				LEVIATHAN_HEART_PEARL,
				CORAL_EDGE_FANG,
				VOLTAIC_JELLY_SAC,
				ABYSSAL_IRON_PLATE,
				STAR_BARNACLE_AMBERGRIS,
				KELP_WRAPPED_SASHIMI,
				SEA_SALT_RAMEN
			).forEach(out::accept));
	}

	private static void tickPlayerOceanGear(ServerPlayer player, long tick) {
		boolean helm = player.getItemBySlot(EquipmentSlot.HEAD).is(ABYSSAL_DIVING_HELM);
		boolean chest = player.getItemBySlot(EquipmentSlot.CHEST).is(BATHYSPHERE_CHESTPLATE);
		boolean legs = player.getItemBySlot(EquipmentSlot.LEGS).is(KELP_WEAVE_GREAVES);
		boolean feet = player.getItemBySlot(EquipmentSlot.FEET).is(TRENCH_BOOTS);
		int suitPieces = (helm ? 1 : 0) + (chest ? 1 : 0) + (legs ? 1 : 0) + (feet ? 1 : 0);

		boolean submerged = player.isUnderWater() || player.isInWater();
		boolean holdingLantern = player.getMainHandItem().is(SIREN_PEARL_LANTERN)
			|| player.getOffhandItem().is(SIREN_PEARL_LANTERN);
		boolean holdingGlaive = player.getMainHandItem().is(LEVIATHAN_GLAIVE);

		int currentO2 = player.getAttachedOrElse(TANK_OXYGEN, OceanRules.CHEST_OXYGEN_MAX);
		int nextO2 = OceanRules.stepTankOxygen(currentO2, helm, chest, submerged,
			player.hasEffect(MobEffects.WATER_BREATHING) || player.hasEffect(MobEffects.CONDUIT_POWER), tick);
		if (chest) {
			player.setAttached(TANK_OXYGEN, nextO2);
			if (submerged && nextO2 > 0) {
				player.setAirSupply(player.getMaxAirSupply());
			}
		}

		if (submerged && (helm || holdingLantern)) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
		}
		if (submerged && suitPieces >= 4) {
			player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 80, 0, true, false, true));
		}

		syncModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MOD,
			holdingGlaive ? OceanRules.GLAIVE_REACH_BONUS : 0.0);
		syncModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, GREAVES_MOD,
			legs ? OceanRules.SUIT_GREAVES_WATER_EFFICIENCY : 0.0);
		syncModifier(player, Attributes.SUBMERGED_MINING_SPEED, BOOTS_MOD,
			feet ? OceanRules.SUIT_BOOTS_MINING_SPEED : 0.0);
		syncModifier(player, Attributes.STEP_HEIGHT, BOOTS_STEP_MOD,
			feet ? 0.25 : 0.0);

		if (holdingLantern && tick % 5 == 0) {
			repelNearbySwarmers(player);
		}
	}

	private static void repelNearbySwarmers(ServerPlayer player) {
		AABB box = player.getBoundingBox().inflate(OceanRules.LANTERN_REPEL_RADIUS);
		List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box, mob -> {
			boolean swarmer = (mob instanceof SirenVeilJelly || mob instanceof CoralSpineMako
				|| mob.getType() == EntityType.DROWNED || mob.getType() == EntityType.GUARDIAN)
				&& !BossDamage.boss(mob);
			return OceanRules.shouldRepelSwarmer(mob.distanceToSqr(player), swarmer);
		});
		for (Mob mob : mobs) {
			Vec3 away = mob.position().subtract(player.position());
			double dist = away.length();
			if (dist > 0.05) {
				Vec3 push = away.normalize().scale(0.18);
				mob.push(push.x, 0.02, push.z);
			}
		}
		if (player.level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.GLOW, player.getX(), player.getEyeY(), player.getZ(), 1, 0.3, 0.2, 0.3, 0.01);
		}
	}

	private static void tickConchDomes(net.minecraft.server.MinecraftServer server) {
		int nowTick = server.getTickCount();
		ACTIVE_DOMES.entrySet().removeIf(entry -> nowTick >= entry.getValue().expiresTick());
		for (ActiveDome dome : ACTIVE_DOMES.values()) {
			ServerLevel level = server.getLevel(dome.dimension());
			if (level == null) continue;

			if (nowTick % 4 == 0) {
				AABB box = AABB.ofSize(dome.center(), OceanRules.CONCH_DOME_RADIUS * 2.0, OceanRules.CONCH_DOME_RADIUS * 2.0, OceanRules.CONCH_DOME_RADIUS * 2.0);
				for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
					if (!OceanRules.isInsideConchDome(dome.center().x, dome.center().y, dome.center().z, living.getX(), living.getY(), living.getZ())) {
						continue;
					}
					if (living instanceof ServerPlayer sp) {
						sp.setAirSupply(sp.getMaxAirSupply());
						sp.removeEffect(MobEffects.DIG_SLOWDOWN);
						sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 45, 0, true, false, true));
					} else if ((living.getType() == EntityType.DROWNED || living.getType() == EntityType.GUARDIAN
						|| living instanceof CoralSpineMako) && !BossDamage.boss(living)) {
						Vec3 out = living.position().subtract(dome.center()).normalize().scale(0.32);
						living.push(out.x, 0.05, out.z);
					}
				}
				double r = OceanRules.CONCH_DOME_RADIUS;
				for (int i = 0; i < 10; i++) {
					double angle = (nowTick * 0.15) + i * (Math.PI * 2.0 / 10.0);
					level.sendParticles(ParticleTypes.BUBBLE_POP,
						dome.center().x + Math.cos(angle) * r,
						dome.center().y + Math.sin(nowTick * 0.2 + i) * 1.2,
						dome.center().z + Math.sin(angle) * r,
						1, 0.05, 0.05, 0.05, 0.01);
				}
			}
		}
	}

	private static void syncModifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double value) {
		var instance = player.getAttribute(attr);
		if (instance == null) return;
		var existing = instance.getModifier(id);
		if (value == 0.0) {
			if (existing != null) instance.removeModifier(id);
		} else if (existing == null || Math.abs(existing.amount() - value) > 1.0e-5) {
			instance.removeModifier(id);
			instance.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	private static void removeModifiers(ServerPlayer player) {
		syncModifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_MOD, 0.0);
		syncModifier(player, Attributes.WATER_MOVEMENT_EFFICIENCY, GREAVES_MOD, 0.0);
		syncModifier(player, Attributes.SUBMERGED_MINING_SPEED, BOOTS_MOD, 0.0);
		syncModifier(player, Attributes.STEP_HEIGHT, BOOTS_STEP_MOD, 0.0);
	}

	// ---- Custom Item Subclasses ----

	private static final class HarpoonItem extends AuraWorld.Lore {
		HarpoonItem(Properties properties) {
			super("tide_cleaver_harpoon", properties);
		}

		@Override
		public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack held = player.getItemInHand(hand);
			if (level instanceof ServerLevel server) {
				if (!player.getAbilities().instabuild) {
					held.hurtAndBreak(1, player, McHands.slot(hand));
				}
				ThrownHarpoon harpoon = new ThrownHarpoon(server, player, held);
				harpoon.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.35f, 0.6f);
				server.addFreshEntity(harpoon);
				server.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 0.95f);
				if (!player.getAbilities().instabuild) {
					held.shrink(1);
				} else {
					player.getCooldowns().addCooldown(held.getItem(), 12);
				}
			}
			return InteractionResultHolder.success(held);
		}
	}

	private static final class GlaiveItem extends AuraWorld.Lore {
		GlaiveItem(Properties properties) {
			super("leviathan_glaive", properties);
		}

		@Override
		public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack held = player.getItemInHand(hand);
			if (player.getCooldowns().isOnCooldown(held.getItem())) {
				return InteractionResultHolder.fail(held);
			}
			if (level instanceof ServerLevel server) {
				Vec3 origin = player.getEyePosition();
				Vec3 look = player.getLookAngle();
				AABB box = player.getBoundingBox().inflate(OceanRules.GLAIVE_SLASH_RANGE);
				boolean wetCaster = player.isInWaterOrRain();
				int hits = 0;

				for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
					if (!OceanRules.isInHydroSlashArc(origin.x, origin.y, origin.z, look.x, look.z, target.getX(), target.getEyeY(), target.getZ())) {
						continue;
					}
					float dmg = (wetCaster || target.isInWaterOrRain())
						? OceanRules.GLAIVE_SLASH_WET_DAMAGE
						: OceanRules.GLAIVE_SLASH_DAMAGE;
					target.hurt( server.damageSources().playerAttack(player),  dmg);
					Vec3 push = target.position().subtract(player.position()).normalize().scale(0.85);
					target.push(push.x, 0.25, push.z);
					hits++;
				}

				for (int step = -3; step <= 3; step++) {
					double rad = Math.toRadians(step * 12.0);
					double rx = look.x * Math.cos(rad) - look.z * Math.sin(rad);
					double rz = look.x * Math.sin(rad) + look.z * Math.cos(rad);
					for (double r = 2.0; r <= OceanRules.GLAIVE_SLASH_RANGE; r += 1.4) {
						server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
							origin.x + rx * r, origin.y - 0.35, origin.z + rz * r,
							2, 0.1, 0.08, 0.1, 0.02);
					}
				}
				server.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0f, 1.25f);
				if (!player.getAbilities().instabuild && hits > 0) {
					held.hurtAndBreak(1, player, McHands.slot(hand));
				}
				player.getCooldowns().addCooldown(held.getItem(), OceanRules.GLAIVE_COOLDOWN_TICKS);
			}
			return InteractionResultHolder.success(held);
		}
	}

	private static final class ConchItem extends AuraWorld.Lore {
		ConchItem(Properties properties) {
			super("tidecaller_conch", properties);
		}

		@Override
		public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
			ItemStack held = player.getItemInHand(hand);
			if (player.getCooldowns().isOnCooldown(held.getItem())) {
				return InteractionResultHolder.fail(held);
			}
			if (level instanceof ServerLevel server) {
				int expires = server.getServer().getTickCount() + OceanRules.CONCH_DOME_DURATION_TICKS;
				ACTIVE_DOMES.put(UUID.randomUUID(), new ActiveDome(server.dimension(), player.getEyePosition(), expires));
				player.setAirSupply(player.getMaxAirSupply());
				player.removeEffect(MobEffects.DIG_SLOWDOWN);
				player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 160, 0, true, false, true));
				player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 120, 0, true, false, true));
				server.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.4f, 0.72f);
				server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.65f);
				server.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getEyeY(), player.getZ(), 28, 1.8, 1.0, 1.8, 0.06);
				if (!player.getAbilities().instabuild) {
					held.hurtAndBreak(1, player, McHands.slot(hand));
				}
				player.getCooldowns().addCooldown(held.getItem(), OceanRules.CONCH_COOLDOWN_TICKS);
			}
			return InteractionResultHolder.success(held);
		}
	}

	private static final class SirenLanternBlockItem extends BlockItem {
		SirenLanternBlockItem(Properties properties) {
			super(OceanBlocks.SIREN_PEARL_LANTERN, properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
			tooltip.add(Component.translatable("item.wildercord.siren_pearl_lantern.lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8));
			tooltip.add(Component.translatable("item.wildercord.siren_pearl_lantern.use").withColor(0xB8D8A8));
		}
	}

	private static final class LoreBlockItem extends BlockItem {
		private final String id;

		LoreBlockItem(String id, net.minecraft.world.level.block.Block block, Properties properties) {
			super(block, properties);
			this.id = id;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
			tooltip.add(Component.translatable("item.wildercord." + id + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8));
			tooltip.add(Component.translatable("item.wildercord." + id + ".use").withColor(0xB8D8A8));
		}
	}
}
