package dev.wildercord.runesmith;

import com.google.common.collect.ImmutableSet;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * The Runesmith: a villager profession for rune-work. Any unemployed villager takes the job from a
 * Scribing Desk. Its trades (levels 1-5) are data under {@code data/wildercord/trade_set/runesmith}
 * and {@code villager_trade/runesmith}: Blank Runes, a rotating shelf of Tier I-II runes, Mana
 * Crystals, Torn Pages, scroll paper and ink, and a Tier III rune at Master. On top of those it buys
 * back runes you already know and rerolls pairs of them ({@link DuplicateSwap}), and its desk holds
 * your daily contracts ({@link Contracts}).
 */
public final class Runesmith {
	private Runesmith() {}

	public static final ScribingDeskBlock SCRIBING_DESK = registerDesk();

	public static final ResourceKey<PoiType> POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, Wildercord.id("runesmith"));
	public static final ResourceKey<VillagerProfession> PROFESSION = ResourceKey.create(Registries.VILLAGER_PROFESSION, Wildercord.id("runesmith"));

	private static ScribingDeskBlock registerDesk() {
		ResourceKey<net.minecraft.world.level.block.Block> key = ResourceKey.create(Registries.BLOCK, Wildercord.id("scribing_desk"));
		ScribingDeskBlock block = Registry.register(BuiltInRegistries.BLOCK, key, new ScribingDeskBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE)
			.instrument(NoteBlockInstrument.BASS)
			.sound(SoundType.WOOD)
			.strength(2.5F)
			.ignitedByLava()
			.lightLevel(state -> 5)
			.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildercord.id("scribing_desk"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON)));
		return block;
	}

	private static ResourceKey<TradeSet> trades(int level) {
		return ResourceKey.create(Registries.TRADE_SET, Wildercord.id("runesmith/level_" + level));
	}

	public static void init() {
		PoiHelper.register(POI.identifier(), 1, 1, SCRIBING_DESK);
		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, PROFESSION, new VillagerProfession(
			Component.translatable("entity.wildercord.villager.runesmith"),
			poi -> poi.is(POI),
			poi -> poi.is(POI),
			ImmutableSet.of(),
			ImmutableSet.of(),
			SoundEvents.VILLAGER_WORK_LIBRARIAN,
			Int2ObjectMap.ofEntries(
				Int2ObjectMap.entry(1, trades(1)),
				Int2ObjectMap.entry(2, trades(2)),
				Int2ObjectMap.entry(3, trades(3)),
				Int2ObjectMap.entry(4, trades(4)),
				Int2ObjectMap.entry(5, trades(5)))));
		Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Wildercord.id("random_rune"), RandomRuneFunction.MAP_CODEC);
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.insertAfter(WildercordItems.TRAINING_DUMMY, SCRIBING_DESK));
		DuplicateSwap.init();
		Contracts.init();
	}

	/** Whether this villager is a Runesmith. */
	public static boolean is(Villager villager) {
		Holder<VillagerProfession> profession = villager.getVillagerData().profession();
		return profession.is(PROFESSION);
	}
}
