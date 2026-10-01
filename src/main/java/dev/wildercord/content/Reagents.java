package dev.wildercord.content;

import dev.wildercord.Wildercord;
import dev.wildercord.world.ResidueRules.Kind;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

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

	public static final Item CINDER_ASH = register(Kind.SMOULDERING_ASH, new Item.Properties()
		// Ash that never quite went out: it burns in a furnace, briefly but hot (four items, a quarter faster).
		.component(DataComponents.COOKING_FUEL, new CookingFuel(new ResolvableInt.Constant(800), new ResolvableFloat.Constant(1.25F))));
	public static final Item EVERFROST_SHARD = register(Kind.EVERFROST, new Item.Properties());
	public static final Item FULGURITE_SHARD = register(Kind.FULGURITE, new Item.Properties());
	public static final Item BOTTLED_GALE = register(Kind.LINGERING_EDDY, new Item.Properties().stacksTo(16)
		.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.2F).animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK)
			.hasConsumeParticles(false).onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600))).build())
		.usingConvertsTo(Items.GLASS_BOTTLE));
	public static final Item GEODE_GRIT = register(Kind.RIVEN_STONE, new Item.Properties());
	public static final Item WILDBLOOM_PETAL = register(Kind.WILDBLOOM, new Item.Properties());
	public static final Item HOLLOW_DUST = register(Kind.VOID_SCAR, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item STAR_DUST = register(Kind.STAR_GLYPH, new Item.Properties());
	public static final Item HOURGLASS_SAND = register(Kind.STILLED_SAND, new Item.Properties());
	public static final Item SANGUINE_BEAD = register(Kind.BLOODMOSS, new Item.Properties());

	private static Item register(Kind kind, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(kind.reagent));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new ReagentItem(kind, properties.setId(key)));
		BY_KIND.put(kind, item);
		return item;
	}

	/** The reagent one element's residue gives. */
	public static Item item(Kind kind) {
		return BY_KIND.get(kind);
	}

	/** The residue a reagent comes from (and so its element), if the stack is one. */
	public static Optional<Kind> kindOf(ItemStack stack) {
		return stack.getItem() instanceof ReagentItem reagent ? Optional.of(reagent.kind) : Optional.empty();
	}

	public static boolean is(ItemStack stack) {
		return stack.getItem() instanceof ReagentItem;
	}

	/** In the order of the elements, for the creative tab. */
	public static java.util.Collection<Item> all() {
		return BY_KIND.values();
	}

	public static void init() {
	}
}
