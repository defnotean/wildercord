package dev.wildercord.cooking;

import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
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
			Item.Properties properties = new Item.Properties().useBlockDescriptionPrefix()));
		return block;
	}

	public static void init() {
		CampPotBlock.init();
	}

	public static Holder<MobEffect> effect(String id) {
		return BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(id)).orElseThrow(() -> new IllegalStateException("no effect " + id));
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
		public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> wildercord$lines,TooltipFlag flag) {
		Consumer<Component> out = wildercord$lines::add;
			out.accept(Component.translatable(recipe.rare() ? "tooltip.wildercord.rare_meal" : "tooltip.wildercord.meal").withStyle(ChatFormatting.GOLD));
			for (CookingRules.Buff buff : recipe.buffs()) {
				out.accept(Component.literal(" ").append(describe(buff)).withStyle(ChatFormatting.BLUE));
			}
		}
	}
}
