package dev.wildercord.content;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Cord made from the one below it ({@code wildercord:cord_upgrade}): crafted like any shapeless
 * recipe (the old Cord and the materials, anywhere in the grid), but the new Cord keeps everything
 * the old one carried, its enchantments, its name and the rest, the way vanilla's transmuting
 * recipes keep a shulker box's contents. The matching and the recipe book's display are a plain
 * shapeless recipe's.
 */
public final class CordUpgradeRecipe extends NormalCraftingRecipe {
	public static final MapCodec<CordUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
		CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
		ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
		Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.ingredients)
	).apply(i, CordUpgradeRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CordUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
		Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
		CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
		ItemStackTemplate.STREAM_CODEC, o -> o.result,
		Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
		CordUpgradeRecipe::new);
	public static final RecipeSerializer<CordUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final ItemStackTemplate result;
	private final List<Ingredient> ingredients;
	/** The same recipe as a plain shapeless one: it does the matching and the recipe book's display. */
	private final ShapelessRecipe shapeless;

	public CordUpgradeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result, List<Ingredient> ingredients) {
		super(commonInfo, bookInfo);
		this.result = result;
		this.ingredients = ingredients;
		this.shapeless = new ShapelessRecipe(commonInfo, bookInfo, result, ingredients);
	}

	public static void init() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Wildercord.id("cord_upgrade"), SERIALIZER);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return shapeless.matches(input, level);
	}

	/** The new Cord, carrying over whatever the old one in the grid held. */
	@Override
	public ItemStack assemble(CraftingInput input) {
		for (int slot = 0; slot < input.size(); slot++) {
			ItemStack stack = input.getItem(slot);
			if (stack.getItem() instanceof CordItem) {
				return TransmuteRecipe.createWithOriginalComponents(result, stack);
			}
		}
		return result.create();
	}

	@Override
	public List<RecipeDisplay> display() {
		return shapeless.display();
	}

	@Override
	protected PlacementInfo createPlacementInfo() {
		return shapeless.placementInfo();
	}

	@Override
	public RecipeSerializer<CordUpgradeRecipe> getSerializer() {
		return SERIALIZER;
	}
}
