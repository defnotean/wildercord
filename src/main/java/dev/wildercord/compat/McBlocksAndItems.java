package dev.wildercord.compat;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Version seam for blocks, items, tags, sounds, signs, containers, compostables and fuels (Minecraft 26.3).
 */
public final class McBlocksAndItems {
	private static final List<ColorCollection<Block>> DYED_FAMILIES = List.of(
		Blocks.WOOL, Blocks.WOOL_STAIRS, Blocks.WOOL_SLAB, Blocks.STAINED_GLASS,
		Blocks.DYED_TERRACOTTA, Blocks.STAINED_GLASS_PANE, Blocks.CARPET, Blocks.GLAZED_TERRACOTTA,
		Blocks.CONCRETE, Blocks.CONCRETE_STAIRS, Blocks.CONCRETE_SLAB, Blocks.CONCRETE_POWDER, Blocks.DYED_CANDLE);

	private McBlocksAndItems() {}

	/** Push reaction preventing pistons from moving authored sanctuary/mechanised blocks. */
	public static PushReaction immovablePushReaction() {
		return PushReaction.IMMOVEABLE;
	}

	/** Push reaction letting fragile cover/mark residues pop on piston contact. */
	public static PushReaction poppedPushReaction() {
		return PushReaction.POPPED;
	}

	/** Whether {@code state} is an ore block in the active tag scheme. */
	public static boolean isOre(BlockState state) {
		return state.is(BlockTags.ORES) || state.is(ConventionalBlockTags.ORES);
	}

	/** Whether {@code state} belongs to the vanilla saplings tag. */
	public static boolean isSapling(BlockState state) {
		return state.is(BlockTags.SAPLINGS);
	}

	/** Checks whether a bonemealable block accepts player/spell bonemeal at {@code pos}. */
	public static boolean isValidBonemealTarget(BonemealableBlock block, LevelReader level, BlockPos pos, BlockState state) {
		return block.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION);
	}

	/** Performs one bonemeal growth step on {@code block}. */
	public static void performBonemeal(BonemealableBlock block, ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		block.performBonemeal(level, random, pos, state, BonemealSource.INTERACTION);
	}

	/** Extinguishes a lit campfire block cleanly across MC douse/dowse renames. */
	public static void douseCampfire(@Nullable Entity actor, LevelAccessor level, BlockPos pos, BlockState state) {
		CampfireBlock.douse(actor, level, pos, state);
	}

	/** Primes a TNT block at {@code pos} respecting {@code TNT_EXPLODES}, returning true if primed. */
	public static boolean primeTnt(ServerLevel level, BlockPos pos, @Nullable LivingEntity igniter) {
		return TntBlock.prime(level, pos, igniter, ItemStack.EMPTY);
	}

	/** World level-event id used for temporary-block break particles. */
	public static int destroyBlockLevelEvent() {
		return LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK;
	}

	/** Unwraps a vanilla {@link SoundEvent} whether exposed directly or via {@link Holder}. */
	public static SoundEvent sound(SoundEvent event) {
		return event;
	}

	/** Unwraps a vanilla {@link SoundEvent} from a {@link Holder}. */
	public static SoundEvent sound(Holder<SoundEvent> holder) {
		return holder.value();
	}

	/** Configures furnace fuel properties when encoded via item components. */
	public static Item.Properties cookingFuelProperties(Item.Properties properties, int burnTicks, float speedMultiplier) {
		return properties.component(DataComponents.COOKING_FUEL,
			new CookingFuel(new ResolvableInt.Constant(burnTicks), new ResolvableFloat.Constant(speedMultiplier)));
	}

	/** Registers furnace burn duration onto Fabric's registry event in 26.2 (no-op in 26.3). */
	public static void registerFuel(ItemLike item, int burnTicks) {}

	/** Configures medium compostability when encoded on item properties. */
	public static Item.Properties compostableMediumProperties(Item.Properties properties) {
		return properties.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM);
	}

	/** Registers compost chance onto vanilla's composter map in 26.2 (no-op in 26.3). */
	public static void registerCompostable(ItemLike item, float chance) {}

	/** Copies all slots held inside an {@link ItemContainerContents} component. */
	public static List<ItemStack> containerItems(@Nullable ItemContainerContents contents) {
		return contents == null ? List.of() : contents.itemCopies().toList();
	}

	/** Number of slot entries inside an {@link ItemContainerContents} component. */
	public static int containerCount(@Nullable ItemContainerContents contents) {
		return contents == null ? 0 : contents.size();
	}

	/** Copies all stacks held inside a {@link BundleContents} component. */
	public static List<ItemStack> bundleItems(@Nullable BundleContents bundle) {
		return bundle == null ? List.of() : bundle.itemCopies().toList();
	}

	/** Replaces the items inside a {@link BundleContents} with {@code items}. */
	public static BundleContents bundleWith(@Nullable BundleContents previous, List<ItemStack> items) {
		return (previous != null ? previous : BundleContents.EMPTY).copyWithContents(items.stream());
	}

	/** Counts matching items across inventory and crafting grid without removing them. */
	public static int countMatchingItems(Inventory inventory, Predicate<ItemStack> predicate, Container craftSlots) {
		return inventory.clearOrCountMatchingItems(predicate, true, 0, craftSlots);
	}

	/** Removes up to {@code maxCount} matching items from inventory and crafting grid. */
	public static int removeMatchingItems(Inventory inventory, Predicate<ItemStack> predicate, int maxCount, Container craftSlots) {
		return inventory.clearOrCountMatchingItems(predicate, false, maxCount, craftSlots);
	}

	/** Writes front lines onto a sign and waxes it during structure generation. */
	public static void writeWaxedSign(SignBlockEntity sign, List<Component> lines) {
		var text = SignText.EMPTY.asMutable();
		for (int i = 0; i < lines.size(); i++) {
			text.setLine(i, lines.get(i));
		}
		sign.setText(text.asImmutable(), SignTextSlot.FRONT);
		sign.setWaxed(true);
	}

	/** Appends {@code line} to the first blank line of the sign face {@code player} looks at and lights it. */
	public static boolean appendFacingSignLineAndGlow(SignBlockEntity sign, Player player, Component line) {
		SignTextSlot slot = sign.getSlotPlayerIsFacing(player);
		SignText text = sign.getText(slot);
		List<Component> lines = text.getMessages(false);
		int blank = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).getString().isBlank()) {
				blank = i;
				break;
			}
		}
		if (blank < 0) {
			return false;
		}
		sign.setText(text.asMutable().setLine(blank, line).setTextGlowing(true).asImmutable(), slot);
		return true;
	}

	/** Marks both front and back text of {@code sign} as glowing. */
	public static void setBothSidesGlowing(SignBlockEntity sign) {
		for (SignTextSlot slot : List.of(SignTextSlot.FRONT, SignTextSlot.BACK)) {
			sign.setText(sign.getText(slot).withGlowingText(true), slot);
		}
	}

	/** Vanilla dyed block families available for spell recolouring in the active MC version. */
	public static List<ColorCollection<Block>> dyedBlockFamilies() {
		return DYED_FAMILIES;
	}

	/** Tent roof stair block used in Ice Camp structures. */
	public static Block brownWoolStairs() {
		return Blocks.WOOL_STAIRS.brown();
	}

	/** Applies skyrend glaive's heavy swing component in the active MC version. */
	public static void setGlaiveSwingAnimation(ItemStack stack, int duration) {
		stack.set(DataComponents.ATTACK_ANIMATION, new SwingAnimation(SwingAnimationType.WHACK, duration));
	}
}
