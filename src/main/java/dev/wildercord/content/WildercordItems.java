package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.backpack.BackpackItem;
import dev.wildercord.backpack.BackpackTier;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Function;

public final class WildercordItems {
	private WildercordItems() {}

	public static final Item RUNE = register("rune", RuneItem::new, new Item.Properties().stacksTo(16));
	/** A Knot: a whole spell tied into one rune at the Fusion Altar. The same {@code wildercord:rune} component says which. */
	public static final Item KNOT = register("knot", RuneItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item BLANK_RUNE = register("blank_rune", Item::new, new Item.Properties());
	public static final Item MANA_CRYSTAL = register("mana_crystal", ManaCrystalItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final CordItem TWINE_CORD = cord(CordTier.TWINE, Rarity.COMMON);
	public static final CordItem COPPER_CORD = cord(CordTier.COPPER, Rarity.COMMON);
	public static final CordItem AMETHYST_CORD = cord(CordTier.AMETHYST, Rarity.UNCOMMON);
	public static final CordItem ECHO_CORD = cord(CordTier.ECHO, Rarity.RARE);
	public static final Item SPELL_SCROLL = register("spell_scroll", SpellScrollItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item TORN_PAGE = register("torn_page", TornPageItem::new, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
	public static final Item TRAINING_DUMMY = register("training_dummy", TrainingDummyItem::new, new Item.Properties().stacksTo(16));
	public static final BackpackItem BACKPACK = backpack(BackpackTier.BACKPACK, Rarity.COMMON);
	public static final BackpackItem REINFORCED_BACKPACK = backpack(BackpackTier.REINFORCED, Rarity.COMMON);
	public static final BackpackItem RUNEWOVEN_BACKPACK = backpack(BackpackTier.RUNEWOVEN, Rarity.UNCOMMON);

	public static final CreativeModeTab TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		Wildercord.id("wildercord"),
		FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.wildercord"))
			.icon(() -> RuneItem.stack(Runes.LIGHTNING))
			.displayItems((parameters, output) -> {
				output.accept(TWINE_CORD);
				output.accept(COPPER_CORD);
				output.accept(AMETHYST_CORD);
				output.accept(ECHO_CORD);
				output.accept(BLANK_RUNE);
				output.accept(MANA_CRYSTAL);
				output.accept(TORN_PAGE);
				output.accept(TRAINING_DUMMY);
				output.accept(BACKPACK);
				output.accept(REINFORCED_BACKPACK);
				output.accept(RUNEWOVEN_BACKPACK);
				dev.wildercord.gear.GearItems.all().forEach(output::accept);
				output.accept(WildercordBlocks.WELLSTONE);
				output.accept(WildercordBlocks.RUNE_SEAL);
				output.accept(WildercordBlocks.ARCHIVE_LECTERN);
				output.accept(WildercordBlocks.FUSION_ALTAR);
				for (var potion : java.util.List.of(WildercordEffects.CLARITY_POTION, WildercordEffects.LONG_CLARITY_POTION, WildercordEffects.STRONG_CLARITY_POTION,
						WildercordEffects.MANA_POTION, WildercordEffects.STRONG_MANA_POTION)) {
					output.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION, potion));
					output.accept(net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.SPLASH_POTION, potion));
				}
				Runes.all().forEach(rune -> output.accept(RuneItem.stack(rune)));
			})
			.build()
	);

	private static CordItem cord(CordTier tier, Rarity rarity) {
		// Enchantable so Reservoir, Wellspring and Siphon can go on a Cord at the table, anvil or with books.
		return register(tier.key + "_cord", p -> new CordItem(tier, p), new Item.Properties().stacksTo(1).rarity(rarity).enchantable(15));
	}

	private static BackpackItem backpack(BackpackTier tier, Rarity rarity) {
		// One to a slot, empty to begin with (an emptied one matches a new one); the runes keep the best one from burning.
		// The game's own list of what's inside is left out of the tooltip, which lists it itself (see BackpackItem).
		Item.Properties properties = new Item.Properties().stacksTo(1).rarity(rarity)
			.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
			.component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true));
		if (tier == BackpackTier.RUNEWOVEN) {
			properties.fireResistant();
		}
		return register(tier.path, p -> new BackpackItem(tier, p), properties);
	}

	private static <T extends Item> T register(String path, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static CordTier tierOf(ItemStack stack) {
		return stack.getItem() instanceof CordItem cord ? cord.tier : null;
	}

	public static void init() {}
}
