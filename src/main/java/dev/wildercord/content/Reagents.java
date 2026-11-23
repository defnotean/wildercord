package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.world.ResidueRules.Kind;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import net.minecraft.tags.TagKey;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import net.minecraft.world.item.Rarity;



import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reagents: what residues give when they're harvested, one for each element. At the Fusion Altar, one laid in
 * the free rune socket beside two effects steadies or strengthens the fusion or weave (see
 * {@link dev.wildercord.spell.AltarReagents}); each also has a small use of its own (see {@link ReagentItem}).
 * Every one is in the {@link #TAG} item tag, which other features (and data packs) can check.
 */
public final class Reagents {
	private Reagents() {}

	/** Every reagent: {@code #wildercord:reagents}. */
	public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, Wildercord.id("reagents"));

	private static final Map<Kind, Item> BY_KIND = new EnumMap<>(Kind.class);

	public static final Item CINDER_ASH = register(Kind.SMOULDERING_ASH,
		dev.wildercord.compat.McBlocksAndItems.cookingFuelProperties(new Item.Properties(), 800, 1.25F));
	public static final Item EVERFROST_SHARD = register(Kind.EVERFROST, new Item.Properties());
	public static final Item FULGURITE_SHARD = register(Kind.FULGURITE, new Item.Properties());
	public static final Item BOTTLED_GALE = register(Kind.LINGERING_EDDY, new Item.Properties().stacksTo(16));
	public static final Item GEODE_GRIT = register(Kind.RIVEN_STONE, new Item.Properties());
	public static final Item WILDBLOOM_PETAL = register(Kind.WILDBLOOM, new Item.Properties());
	public static final Item HOLLOW_DUST = register(Kind.VOID_SCAR, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item STAR_DUST = register(Kind.STAR_GLYPH, new Item.Properties());
	public static final Item HOURGLASS_SAND = register(Kind.STILLED_SAND, new Item.Properties());
	public static final Item SANGUINE_BEAD = register(Kind.BLOODMOSS, new Item.Properties());

	private static Item register(Kind kind, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(kind.reagent));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new ReagentItem(kind, properties