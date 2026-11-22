package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Registers the four Shrine Cats ({@link ShrineCat}: Yuki, Odie, Freya, Kuro), the Japanese Sakura
 * Village residents ({@link SakuraVillager}), and the Suzu Cat-Bell Charm used to befriend and call them.
 */
public final class ShrineCatContent {
	private ShrineCatContent() {}

	private static final ResourceKey<EntityType<?>> SHRINE_CAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("shrine_cat"));
	public static final EntityType<ShrineCat> SHRINE_CAT = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SHRINE_CAT_KEY,
		EntityType.Builder.<ShrineCat>of(ShrineCat::new, MobCategory.CREATURE)
			.sized(0.65F, 0.72F)
			.eyeHeight(0.55F)
			.clientTrackingRange(10)
			.build(SHRINE_CAT_KEY)
	);

	private static final ResourceKey<EntityType<?>> SAKURA_VILLAGER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id("sakura_villager"));
	public static final EntityType<SakuraVillager> SAKURA_VILLAGER = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		SAKURA_VILLAGER_KEY,
		EntityType.Builder.<SakuraVillager>of(SakuraVillager::new, MobCategory.CREATURE)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.62F)
			.clientTrackingRange(8)
			.build(SAKURA_VILLAGER_KEY)
	);

	private static final ResourceKey<Item> CAT_BELL_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("cat_bell_charm"));
	public static final Item CAT_BELL_CHARM = Registry.register(
		BuiltInRegistries.ITEM,
		CAT_BELL_KEY,
		new CatBellCharmItem(new Item.Properties().setId(CAT_BELL_KEY).stacksTo(16).rarity(Rarity.UNCOMMON))
	);

	private static final ResourceKey<Item> SHRINE_CAT_EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("shrine_cat_spawn_egg"));
	public static final Item SHRINE_CAT_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		SHRINE_CAT_EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(SHRINE_CAT, new Item.Properties().setId(SHRINE_CAT_EGG_KEY))
	);

	private static final ResourceKey<Item> SAKURA_VILLAGER_EGG_KEY = ResourceKey.create(Registries.ITEM, Wildercord.id("sakura_villager_spawn_egg"));
	public static final Item SAKURA_VILLAGER_EGG = Registry.register(
		BuiltInRegistries.ITEM,
		SAKURA_VILLAGER_EGG_KEY,
		dev.wildercord.compat.McSpawnEgg.create(SAKURA_VILLAGER, new Item.Properties().setId(SAKURA_VILLAGER_EGG_KEY))
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(SHRINE_CAT, ShrineCat.createAttributes());
		FabricDefaultAttributeRegistry.register(SAKURA_VILLAGER, SakuraVillager.createAttributes());
		FieldGuide.add(new FieldGuide.Entry("wildercord:shrine_cat", FieldGuide.Group.WILDLIFE, 0xE6A15C));
		FieldGuide.add(new FieldGuide.Entry("wildercord:sakura_villager", FieldGuide.Group.WANDERER, 0xF2B8C6));
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		ItemGroupEvents.modifyEntriesEvent(tab).register(output -> {
			output.accept(CAT_BELL_CHARM);
			output.accept(SHRINE_CAT_EGG);
			output.accept(SAKURA_VILLAGER_EGG);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
			output.accept(SHRINE_CAT_EGG);
			output.accept(SAKURA_VILLAGER_EGG);
		});
	}

	/** Calls tamed Shrine Cats back to the holder's side when rung in the air, or tames eligible cats on direct use. */
	public static final class CatBellCharmItem extends Item {
		public CatBellCharmItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.5F);
			int recalled = 0;
			if (level instanceof ServerLevel server) {
				recalled += ShrineCat.ensureQuartetReunited(server, player, player.isShiftKeyDown());
			}
			List<ShrineCat> mine = level.getEntitiesOfClass(ShrineCat.class, player.getBoundingBox().inflate(48.0),
				cat -> cat.isTame() && cat.isOwnedBy(player));
			for (ShrineCat cat : mine) {
				if (cat.isOrderedToSit()) {
					cat.setOrderedToSit(false);
				}
				if (cat.distanceToSqr(player) > 25.0) {
					cat.teleportTo(player.getX() + (level.getRandom().nextDouble() - 0.5) * 1.5, player.getY(),
						player.getZ() + (level.getRandom().nextDouble() - 0.5) * 1.5);
				}
				recalled++;
			}
			if (recalled > 0) {
				player.displayClientMessage(Component.literal("Rang the Suzu Cat-Bell Charm — called " + recalled + " companion cat(s)."), true);
			}
			return InteractionResult.SUCCESS_SERVER;
		}
	}
}
