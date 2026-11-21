package dev.wildercord.aura.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.aura.BreathingManualItem;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * {@code wildercord:manual_pages}: manual pages and a book bound into a Breathing Manual, crafted like any shapeless recipe, but
 * only when every page is of the same method, which the manual then teaches. The matching and the recipe book's display are a
 * plain shapeless recipe's; the result is a manual whose method comes from the pages.
 */
public final class ManualPagesRecipe implements CraftingRecipe {
	public static final MapCodec<ManualPagesRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(o -> o.category),
		ItemStack.CODEC.fieldOf("result").forGetter(o -> o.result),
		Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.ingredients)
	).apply(i, ManualPagesRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, ManualPagesRecipe> STREAM_CODEC = StreamCodec.composite(
		CraftingBookCategory.STREAM_CODEC, o -> o.category,
		ItemStack.STREAM_CODEC, o -> o.result,
		Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
		ManualPagesRecipe::new);
	public static final RecipeSerializer<ManualPagesRecipe> SERIALIZER = new RecipeSerializer<>() {
		@Override
		public MapCodec<ManualPagesRecipe> codec() {
			return MAP_CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ManualPagesRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	};

	private final CraftingBookCategory category;
	private final ItemStack result;
	private final List<Ingredient> ingredients;
	private final ShapelessRecipe shapeless;

	public ManualPagesRecipe(CraftingBookCategory category, ItemStack result, List<Ingredient> ingredients) {
		this.category = category;
		this.result = result;
		this.ingredients = ingredients;
		this.shapeless = new ShapelessRecipe("", category, result, ingredients);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return shapeless.matches(input, level) && method(input) != null;
	}

	/** The method every page in the grid shares, or null when they don't (or one has none). */
	static String method(CraftingInput input) {
		String method = null;
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (!(stack.getItem() instanceof ManualPageItem)) {
				continue;
			}
			String id = stack.getOrDefault(BreathingManualItem.METHOD, "");
			if (id.isEmpty() || method != null && !method.equals(id)) {
				return null;
			}
			method = id;
		}
		return method;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		String method = method(input);
		ItemStack manual = result.copy();
		if (method != null) {
			manual.set(BreathingManualItem.METHOD, method);
		}
		return manual;
	}

	@Override
	public List<RecipeDisplay> display() {
		return shapeless.display();
	}

	@Override
	public PlacementInfo placementInfo() {
		return shapeless.placementInfo();
	}

	@Override
	public CraftingBookCategory category() {
		return category;
	}

	@Override
	public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	@Override
	public RecipeSerializer<ManualPagesRecipe> getSerializer() {
		return SERIALIZER;
	}
}
