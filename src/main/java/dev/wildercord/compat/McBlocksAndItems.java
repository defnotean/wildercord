package dev.wildercord.compat;

import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.PushReaction;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Version seam for blocks, items, tags, sounds, signs, containers, compostables and fuels.
 */
public final class McBlocksAndItems {
	private static final TagKey<Block> SAPLINGS = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("saplings"));
	// 26.1.2 has no vanilla colour-collection holders; dyed families are resolved by registry name.
	private static final List<String> DYED_SUFFIXES = List.of(
		"stained_glass_pane", "stained_glass", "glazed_terracotta", "concrete_powder",
		"terracotta", "carpet", "concrete", "wool", "candle");

	private McBlocksAndItems() {}

	/** Push reaction preventing pistons from moving authored sanctuary/mechanised blocks. */
	public static PushReaction immovablePushReaction() {
		return PushReaction.BLOCK;
	}

	/** Push reaction letting fragile cover/mark residues pop on piston contact. */
	public static PushReaction poppedPushReaction() {
		return PushReaction.DESTROY;
	}

	/** Whether {@code state} is an ore block in the active tag scheme. */
	public static boolean isOre(BlockState state) {
		return state.is(ConventionalBlockTags.ORES);
	}

	/** Whether {@code state} belongs to the vanilla saplings tag. */
	public static boolean isSapling(BlockState state) {
		return state.is(SAPLINGS);
	}

	/** Checks whether a bonemealable block accepts player/spell bonemeal at {@code pos}. */
	public static boolean isValidBonemealTarget(BonemealableBlock block, LevelReader level, BlockPos pos, BlockState state) {
		return block.isValidBonemealTarget(level, pos, state);
	}

	/** Performs one bonemeal growth step on {@code block}. */
	public static void performBonemeal(BonemealableBlock block, ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		block.performBonemeal(level, random, pos, state);
	}

	/** Extinguishes a lit campfire block cleanly across MC douse/dowse renames. */
	public static void douseCampfire(@Nullable Entity actor, LevelAccessor level, BlockPos pos, BlockState state) {
		CampfireBlock.dowse(actor, level, pos, state);
	}

	/** Primes a TNT block at {@code pos} respecting {@code TNT_EXPLODES}, returning true if primed. */
	public static boolean primeTnt(ServerLevel level, BlockPos pos, @Nullable LivingEntity igniter) {
		if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) {
			return false;
		}
		PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
		level.addFreshEntity(tnt);
		level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
		return true;
	}

	/** World level-event id used for temporary-block break particles. */
	public static int destroyBlockLevelEvent() {
		return LevelEvent.PARTICLES_DESTROY_BLOCK;
	}

	/** Unwraps a vanilla {@link SoundEvent} whether exposed directly or via {@link Holder}. */
	public static SoundEvent sound(SoundEvent event) {
		return event;
	}

	/** Unwraps a vanilla {@link SoundEvent} from a {@link Holder}. */
	public static SoundEvent sound(Holder<SoundEvent> holder) {
		return holder.value();
	}

	/** Configures furnace fuel properties when encoded via item components (no-op in 26.2). */
	public static Item.Properties cookingFuelProperties(Item.Properties properties, int burnTicks, float speedMultiplier) {
		return properties;
	}

	/** Registers furnace burn duration onto Fabric's registry event in 26.2 (no-op in 26.3). */
	public static void registerFuel(ItemLike item, int burnTicks) {
		FuelValueEvents.BUILD.register((builder, context) -> builder.add(item, burnTicks));
	}

	/** Configures medium compostability when encoded on item properties (no-op in 26.2). */
	public static Item.Properties compostableMediumProperties(Item.Properties properties) {
		return properties;
	}

	/** Registers compost chance onto vanilla's composter map in 26.2 (no-op in 26.3). */
	public static void registerCompostable(ItemLike item, float chance) {
		ComposterBlock.COMPOSTABLES.put(item, chance);
	}

	/** Copies all slots held inside an {@link ItemContainerContents} component. */
	public static List<ItemStack> containerItems(@Nullable ItemContainerContents contents) {
		return contents == null ? List.of() : contents.allItemsCopyStream().toList();
	}

	/** Number of slot entries inside an {@link ItemContainerContents} component. */
	public static int containerCount(@Nullable ItemContainerContents contents) {
		return contents == null ? 0 : contents.allItemsCopyStream().toList().size();
	}

	/** Copies all stacks held inside a {@link BundleContents} component. */
	public static List<ItemStack> bundleItems(@Nullable BundleContents bundle) {
		return bundle == null ? List.of() : bundle.itemCopyStream().toList();
	}

	/** Replaces the items inside a {@link BundleContents} with {@code items}. */
	public static BundleContents bundleWith(BundleContents previous, List<ItemStack> items) {
		return new BundleContents(items.stream().map(ItemStackTemplate::fromNonEmptyStack).toList());
	}

	/** Counts matching items across inventory and crafting grid without removing them. */
	public static int countMatchingItems(Inventory inventory, Predicate<ItemStack> predicate, Container craftSlots) {
		return inventory.clearOrCountMatchingItems(predicate, 0, craftSlots);
	}

	/** Removes up to {@code maxCount} matching items from inventory and crafting grid. */
	public static int removeMatchingItems(Inventory inventory, Predicate<ItemStack> predicate, int maxCount, Container craftSlots) {
		return inventory.clearOrCountMatchingItems(predicate, maxCount, craftSlots);
	}

	/** Writes front lines onto a sign and waxes it during structure generation. */
	public static void writeWaxedSign(SignBlockEntity sign, List<Component> lines) {
		SignText text = new SignText();
		for (int i = 0; i < lines.size(); i++) {
			text = text.setMessage(i, lines.get(i));
		}
		sign.setText(text, true);
		sign.setWaxed(true);
	}

	/** Appends {@code line} to the first blank line of the sign face {@code player} looks at and lights it. */
	public static boolean appendFacingSignLineAndGlow(SignBlockEntity sign, Player player, Component line) {
		boolean front = sign.isFacingFrontText(player);
		SignText text = sign.getText(front);
		Component[] lines = text.getMessages(false);
		int blank = -1;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].getString().isBlank()) {
				blank = i;
				break;
			}
		}
		if (blank < 0) {
			return false;
		}
		sign.setText(text.setMessage(blank, line).setHasGlowingText(true), front);
		return true;
	}

	/** Marks both front and back text of {@code sign} as glowing. */
	public static void setBothSidesGlowing(SignBlockEntity sign) {
		for (boolean front : List.of(true, false)) {
			sign.setText(sign.getText(front).setHasGlowingText(true), front);
		}
	}

	/** Base blocks of every dyeable family available for spell recolouring in the active MC version. */
	public static List<Block> dyedBlockFamilies() {
		return List.of(Blocks.WHITE_WOOL, Blocks.STAINED_GLASS, Blocks.TERRACOTTA, Blocks.STAINED_GLASS_PANE,
			Blocks.WHITE_CARPET, Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.WHITE_CONCRETE, Blocks.WHITE_CONCRETE_POWDER, Blocks.CANDLE);
	}

	/** {@code block} recoloured to {@code color} within its dyed family, or null when it isn't dyeable or already is that colour. */
	public static @Nullable Block recoloured(Block block, DyeColor color) {
		if (block == Blocks.GLASS) {
			block = Blocks.STAINED_GLASS;
		} else if (block == Blocks.GLASS_PANE) {
			block = Blocks.STAINED_GLASS_PANE;
		}
		String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
		for (String suffix : DYED_SUFFIXES) {
			String prefix = name.equals(suffix) ? "" : name.endsWith("_" + suffix)
				? name.substring(0, name.length() - suffix.length() - 1) : null;
			if (prefix == null) {
				continue;
			}
			if (!prefix.isEmpty()) {
				boolean dyedMember = false;
				for (DyeColor dye : DyeColor.values()) {
					if (dye.getName().equals(prefix)) {
						dyedMember = true;
						break;
					}
				}
				if (!dyedMember) {
					continue;
				}
			}
			Block member = BuiltInRegistries.BLOCK.get(Identifier.withDefaultNamespace(color.getName() + "_" + suffix));
			return member == block || member == Blocks.AIR ? null : member;
		}
		return null;
	}

	/** Tent roof stair block used in Ice Camp structures. */
	public static Block brownWoolStairs() {
		return Blocks.SPRUCE_STAIRS;
	}

	/** Applies skyrend glaive's heavy swing component in the active MC version. */
	public static void setGlaiveSwingAnimation(ItemStack stack, int duration) {
		stack.set(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.WHACK, duration));
	}
}
