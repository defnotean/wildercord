package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.compat.McBlocksAndItems;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The world of aura, registered: the wandering duelist and the fallen knight, the manual pages and the Aura Shard they leave,
 * the binding of pages into a manual, the knight's loot function, the aura-forged weapons' component, and everything that runs
 * (duels, the spawners, the forged gear's hooks). The design and every number are in {@link AuraWorldRules}.
 */
public final class AuraWorld {
	private AuraWorld() {}

	/** A sword master in a travelling cloak, a head taller than nobody: a player's size. */
	public static final EntityType<Duelist> DUELIST = register("duelist",
		EntityType.Builder.of(Duelist::new, MobCategory.CREATURE).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));
	/** An old suit of armour that aura still walks in. */
	public static final EntityType<FallenKnight> FALLEN_KNIGHT = register("fallen_knight",
		EntityType.Builder.of(FallenKnight::new, MobCategory.MONSTER).sized(0.7F, 2.05F).eyeHeight(1.72F).clientTrackingRange(8));

	/** An opt-in endgame encounter; never part of natural spawning. */
	public static final EntityType<SwordMaster> SWORD_MASTER = register("sword_master",
		EntityType.Builder.of(SwordMaster::new, MobCategory.CREATURE).sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(12));

	/** A page of a breathing manual (its method a component, as a manual's is). */
	public static final Item MANUAL_PAGE = item("manual_page", p -> new ManualPageItem(p.stacksTo(16).rarity(Rarity.UNCOMMON)));
	/** A sliver of a fallen knight's aura, set hard: the smithing template for aura-forged weapons, and the sash's heart. */
	public static final Item AURA_SHARD = item("aura_shard", p -> new Lore("aura_shard", p.rarity(Rarity.UNCOMMON)));

	public static final List<Item> SPAWN_EGGS = new ArrayList<>();
	public static final Item DUELIST_SPAWN_EGG = egg("duelist", DUELIST);
	public static final Item FALLEN_KNIGHT_SPAWN_EGG = egg("fallen_knight", FALLEN_KNIGHT);
	public static final Item SWORD_MASTER_SPAWN_EGG = egg("sword_master", SWORD_MASTER);

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties();
			output.accept(VillageTournaments.history());
			for (AuraWorldRules.Forged forged : AuraWorldRules.Forged.values()) {
				if (forged == AuraWorldRules.Forged.FETCHBLADE) {
					output.accept(forged(forged, true));
				} else {
					output.accept(forged(forged, false));
					output.accept(forged(forged, true));
				}
			}
			SPAWN_EGGS.forEach(output::accept);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
		ForgedGear.init();
		DuelistDuels.init();
		DuelistSpawner.init();
		KnightSpawner.init();
	}

	/** The id of a world-of-aura texture or model. */
	public static ResourceLocation id(String path) {
		return Wildercord.id(path);
	}
}
