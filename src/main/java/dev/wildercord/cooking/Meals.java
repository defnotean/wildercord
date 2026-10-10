package dev.wildercord.cooking;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** The Camp Pot and its meals, one item for each {@link CookingRules#RECIPES recipe}. */
public final class Meals {
	private Meals() {}

	public static final Map<String, Item> MEALS = new LinkedHashMap<>();

	static {
		WildercordEffects.init();
		for (CookingRules.Recipe recipe : CookingRules.RECIPES) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(recipe.id()));
			Item.Properties properties = new Item.Properties().setId(key).stacksTo(CookingRules.STACK).usingConvertsTo(Items.BOWL)
				.rarity(recipe.rare() ? net.minecraft.world.item.Rarity.EPIC : net.minecraft.world.item.Rarity.COMMON)
				.food(new FoodProperties.Builder().nutrition(recipe.nutrition()).saturationModifier(recipe.saturation()).alwaysEdible().build(),
					Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(effects(recipe))).build());
			MEALS.put(recipe.id(), Registry.register(BuiltInRegistries.ITEM, key, new MealItem(recipe, properties)));
		}
	}

	public static final CampPotBlock CAMP_POT = pot();

	/** The far-off spice every rare meal needs, sold by the wandering caravans. */
	public static final Item WAYFARER_SAFFRON = saffron();

	private static Item saffron() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.parse(CookingRules.SAFFRON));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
	}

	private static CampPotBlock pot() {
		ResourceKey<net.minecraft.world.level.block.Block> key = ResourceKey.create(Registries.BLOCK, Wildercord.id("camp_pot"));
		CampPotBlock block = Registry.register(BuiltInRegistries.BLOCK, key, new CampPotBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).sound(SoundType.LANTERN).strength(2.0F, 6.0F).noOcclusion().setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Wildercord.id("camp_pot"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new net.minecraft.world.item.BlockItem(block,
			new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	public static void init() {}

	public static Holder<MobEffect> effect(String id) {
		return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(id)).orElseThrow(() -> new IllegalStateException("no effect " + id));
	}

	public static List<MobEffectInstance> effects(CookingRules.Recipe recipe) {
		List<MobEffectInstance> effects = new ArrayList<>();
		for (CookingRules.Buff buff : recipe.buffs()) {
			effects.add(new MobEffectInstance(effect(buff.effect()), buff.seconds() * 20, buff.level() - 1));
		}
		return effects;
	}

	/** "Speed II (3:00)". */
	public static Component describe(CookingRules.Buff buff) {
		Component name = Component.translatable(effect(buff.effect()).value().getDescriptionId());
		String level = switch (buff.level()) { case 1 -> ""; case 2 -> " II"; case 3 -> " III"; default -> " " + buff.level(); };
		return Component.empty().append(name).append(level + String.format(Locale.ROOT, " (%d:%02d)", buff.seconds() / 60, buff.seconds() % 60));
	}

	public static final class MealItem extends Item {
		private final CookingRules.Recipe recipe;

		MealItem(CookingRules.Recipe recipe, Properties properties) {
			super(properties);
			this.recipe = recipe;
		}

		public CookingRules.Recipe recipe() {
			return recipe;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable(recipe.rare() ? "tooltip.wildercord.rare_meal" : "tooltip.wildercord.meal").withStyle(ChatFormatting.GOLD));
			for (CookingRules.Buff buff : recipe.buffs()) {
				out.accept(Component.literal(" ").append(describe(buff)).withStyle(ChatFormatting.BLUE));
			}
		}
	}
}
