package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.spell.FieldGuide;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.component.TooltipDisplay;

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
		EntityType.Builder.of(FallenKnight::new, MobCategory.MONSTER).sized(0.7F, 2.05F).eyeHeight(1.72F).clientTrackingRange(8).notInPeaceful());

	/** A page of a breathing manual (its method a component, as a manual's is). */
	public static final Item MANUAL_PAGE = item("manual_page", p -> new ManualPageItem(p.stacksTo(16).rarity(Rarity.UNCOMMON)));
	/** A sliver of a fallen knight's aura, set hard: the smithing template for aura-forged weapons, and the sash's heart. */
	public static final Item AURA_SHARD = item("aura_shard", p -> new Lore("aura_shard", p.rarity(Rarity.UNCOMMON)));

	public static final List<Item> SPAWN_EGGS = new ArrayList<>();
	public static final Item DUELIST_SPAWN_EGG = egg("duelist", DUELIST);
	public static final Item FALLEN_KNIGHT_SPAWN_EGG = egg("fallen_knight", FALLEN_KNIGHT);

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String id, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static Item item(String id, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	private static Item egg(String id, EntityType<? extends Mob> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id + "_spawn_egg"));
		Item egg = Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key)));
		SPAWN_EGGS.add(egg);
		return egg;
	}

	/** A plain drop with a line of lore and a line saying what it's for. */
	public static class Lore extends Item {
		private final String id;

		public Lore(String id, Properties properties) {
			super(properties);
			this.id = id;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable("item.wildercord." + id + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8));
			out.accept(Component.translatable("item.wildercord." + id + ".use").withColor(0xB8D8A8));
		}
	}

	/** A diamond weapon carrying a forging, as the smithing table makes it (for the creative tab, the tests and the guide). */
	public static ItemStack forged(AuraWorldRules.Forged forged, boolean netherite) {
		Item base = switch (forged) {
			case LUMENEDGE -> netherite ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD;
			case SKYREND_GLAIVE -> netherite ? Items.NETHERITE_SPEAR : Items.DIAMOND_SPEAR;
			case BULWARK_MAUL -> netherite ? Items.NETHERITE_AXE : Items.DIAMOND_AXE;
		};
		ItemStack stack = new ItemStack(base);
		stack.set(ForgedGear.FORGED, forged.id);
		stack.set(DataComponents.ITEM_MODEL, Wildercord.id(forged.id));
		stack.set(DataComponents.ITEM_NAME, Component.translatable("item.wildercord." + forged.id));
		stack.set(DataComponents.RARITY, Rarity.RARE);
		return stack;
	}

	public static void init() {
		Battlefields.init();
		SwordTombs.init();
		FabricDefaultAttributeRegistry.register(DUELIST, Duelist.createAttributes());
		FabricDefaultAttributeRegistry.register(FALLEN_KNIGHT, FallenKnight.createAttributes());
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Wildercord.id("manual_pages"), ManualPagesRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Wildercord.id("knight_method"), KnightLoot.MAP_CODEC);
		// Into the field guide: the duelist among the wanderers, the knight among the monsters.
		FieldGuide.add(new FieldGuide.Entry("wildercord:duelist", FieldGuide.Group.WANDERER, 0xE8D8B0));
		FieldGuide.add(new FieldGuide.Entry("wildercord:fallen_knight", FieldGuide.Group.MONSTER, 0x9AA8D0));
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
			for (BreathingMethod method : BreathingMethods.BUILT_IN) {
				output.accept(ManualPageItem.of(MANUAL_PAGE, method.id()));
			}
			output.accept(AURA_SHARD);
			for (int i=0;i<3;i++) output.accept(Battlefields.book(i));
			output.accept(SwordTombs.history());
			for (AuraWorldRules.Forged forged : AuraWorldRules.Forged.values()) {
				output.accept(forged(forged, false));
				output.accept(forged(forged, true));
			}
			SPAWN_EGGS.forEach(output::accept);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
		ForgedGear.init();
		DuelistDuels.init();
		DuelistSpawner.init();
		KnightSpawner.init();
	}

	/** The id of a world-of-aura texture or model. */
	public static Identifier id(String path) {
		return Wildercord.id(path);
	}
}
