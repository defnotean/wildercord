package dev.wildercord.backpack;

import dev.wildercord.Wildercord;
import dev.wildercord.menu.BackpackMenu;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Backpacks: what may go in one, the one worn in the Backpack slot, and opening one. A backpack keeps what's
 * in it in the item itself (vanilla's {@code minecraft:container} component, as a shulker box does), so it
 * keeps its contents wherever it goes: dropped, in a chest, on death, through a hopper.
 *
 * <p>Only the backpack in a player's own hand or Backpack slot is ever opened, and its menu
 * ({@link BackpackMenu}) writes every change straight back into that very stack. The menu stays open only
 * while that stack is still where it was opened from: the moment it goes anywhere else (thrown, swapped,
 * dropped on death, replaced), the menu stops taking clicks and closes, so its contents can never be taken
 * out of one copy while another walks away with them.</p>
 *
 * <p>The worn backpack lives in the {@code wildercord:backpack} attachment (saved with the player, and
 * synced only to them, since it carries what's inside), and how it looks in {@code wildercord:backpack_look}
 * (just the item and its colour, synced to everyone who can see the wearer). Like the gear slots, it drops
 * on death unless keepInventory is on.</p>
 */
public final class Backpacks {
	private Backpacks() {}

	/** Every backpack. */
	public static final TagKey<Item> BACKPACKS = TagKey.create(Registries.ITEM, Wildercord.id("backpacks"));
	/**
	 * Never let into a backpack, on top of the rule in {@link #fitsInside}: shulker boxes, bundles and backpacks
	 * are in it already, and a data pack can add other mods' containers.
	 */
	public static final TagKey<Item> NOT_FOR_BACKPACKS = TagKey.create(Registries.ITEM, Wildercord.id("not_for_backpacks"));

	/** The backpack tier of an item, or null if it isn't a backpack. */
	public static BackpackTier tierOf(ItemStack stack) {
		return stack.getItem() instanceof BackpackItem backpack ? backpack.tier : null;
	}

	public static boolean isBackpack(ItemStack stack) {
		return tierOf(stack) != null;
	}

	/**
	 * Whether a stack may go in a backpack. Nothing that holds items does, so storage can't be nested: no
	 * backpack, no shulker box, no bundle, nothing the game keeps out of container items, and no block that
	 * carries what was in it (a chest picked up with its contents). An empty chest or furnace is fine.
	 */
	public static boolean fitsInside(ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}
		if (isBackpack(stack) || !stack.getItem().canFitInsideContainerItems() || stack.is(NOT_FOR_BACKPACKS) || stack.is(ItemTags.BUNDLES)
				|| stack.getItem() instanceof BundleItem || stack.has(DataComponents.BUNDLE_CONTENTS)
				|| stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
			return false;
		}
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		return contents == null || !contents.nonEmptyItems().iterator().hasNext();
	}

	/** What a backpack holds, as fresh copies, one per slot of its tier (empty if it isn't a backpack). */
	public static NonNullList<ItemStack> contents(ItemStack backpack) {
		BackpackTier tier = tierOf(backpack);
		NonNullList<ItemStack> items = NonNullList.withSize(tier == null ? 0 : tier.slots(), ItemStack.EMPTY);
		backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
		return items;
	}

	/** How many of a backpack's slots hold something. */
	public static int used(ItemStack backpack) {
		int used = 0;
		for (var ignored : backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems()) {
			used++;
		}
		return used;
	}

	// ------------------------------------------------------------------ the Backpack slot

	/** The backpack in the wearer's Backpack slot, or empty. This is the stored stack; copy it before changing it. */
	public static ItemStack worn(Entity wearer) {
		ItemStack stack = wearer.getAttached(WildercordAttachments.BACKPACK);
		return stack == null ? ItemStack.EMPTY : stack;
	}

	/**
	 * Puts a backpack in the Backpack slot (one of it, a copy), replacing what was there; an empty stack
	 * empties the slot. Returns false, changing nothing, if it isn't a backpack.
	 */
	public static boolean wear(Entity wearer, ItemStack stack) {
		if (stack.isEmpty()) {
			wearer.removeAttached(WildercordAttachments.BACKPACK);
			wearer.removeAttached(WildercordAttachments.BACKPACK_LOOK);
			return true;
		}
		if (!isBackpack(stack)) {
			return false;
		}
		wearer.setAttached(WildercordAttachments.BACKPACK, stack.copyWithCount(1));
		wearer.setAttached(WildercordAttachments.BACKPACK_LOOK, lookOf(stack));
		return true;
	}

	/** Empties the Backpack slot and returns what was in it (empty if nothing). */
	public static ItemStack takeOff(Entity wearer) {
		ItemStack old = worn(wearer);
		wear(wearer, ItemStack.EMPTY);
		return old;
	}

	/** How the worn backpack looks to everyone (its item and colour, nothing of what's in it), or empty. */
	public static ItemStack look(Entity wearer) {
		ItemStack look = wearer.getAttached(WildercordAttachments.BACKPACK_LOOK);
		return look == null ? ItemStack.EMPTY : look;
	}

	private static ItemStack lookOf(ItemStack backpack) {
		ItemStack look = new ItemStack(backpack.getItem());
		DyedItemColor dye = backpack.get(DataComponents.DYED_COLOR);
		if (dye != null) {
			look.set(DataComponents.DYED_COLOR, dye);
		}
		return look;
	}

	// ------------------------------------------------------------------ opening

	/**
	 * Where an open backpack is: a slot of the player's own inventory ({@code 0}-{@code 8} the hotbar,
	 * {@link Inventory#SLOT_OFFHAND} the off-hand), or {@link #WORN}.
	 */
	public record Place(int slot) {
		/** The Backpack slot. */
		public static final Place WORN = new Place(-1);

		/** The slot the item in this hand is in. */
		public static Place hand(Player player, InteractionHand hand) {
			return new Place(hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND);
		}

		public boolean worn() {
			return slot < 0;
		}

		/** The stack there now. */
		public ItemStack stack(Player player) {
			return worn() ? Backpacks.worn(player) : player.getInventory().getItem(slot);
		}
	}

	/**
	 * Opens the backpack at {@code place} for its owner. Returns false if there's none there, or the player
	 * can't open anything (dead, a spectator).
	 */
	public static boolean open(ServerPlayer player, Place place) {
		if (!player.isAlive() || player.isSpectator() || !isBackpack(place.stack(player))) {
			return false;
		}
		spillOverflow(player, place.stack(player));
		BackpackTier[] opened = new BackpackTier[1];
		ExtendedMenuProvider<BackpackMenu.Opening> provider = new ExtendedMenuProvider<>() {
			private Component title = Component.empty();

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player who) {
				// Read here, not before: opening closes whatever was open first, and a backpack closing can put
				// a fresh copy of itself back in its place.
				ItemStack stack = place.stack(player);
				BackpackTier tier = tierOf(stack);
				if (tier == null) {
					return null;
				}
				title = stack.getHoverName();
				opened[0] = tier;
				return new BackpackMenu(id, inventory, player, place, stack, tier);
			}

			@Override
			public Component getDisplayName() {
				return title;
			}

			@Override
			public BackpackMenu.Opening getScreenOpeningData(ServerPlayer who) {
				return new BackpackMenu.Opening(opened[0] == null ? 1 : opened[0].rows, place.slot());
			}
		};
		if (player.openMenu(provider).isEmpty() || opened[0] == null) {
			return false;
		}
		player.level().playSound(null, player, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8F, 1.1F + player.getRandom().nextFloat() * 0.1F);
		if (opened[0] == BackpackTier.RUNEWOVEN) {
			player.level().playSound(null, player, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.3F);
		}
		return true;
	}

	/** The backpack key: opens the worn backpack, or says there's none. */
	public static void openWorn(ServerPlayer player) {
		if (!isBackpack(worn(player))) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.backpack.none").withStyle(ChatFormatting.GRAY));
			return;
		}
		open(player, Place.WORN);
	}

	/** A backpack closing: the flap going down. */
	public static void playClose(Player player) {
		player.level().playSound(null, player, SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 0.75F + player.getRandom().nextFloat() * 0.1F);
	}

	/**
	 * A backpack whose contents run past its size (only a hand-edited one could) hands the extra to its owner
	 * before it opens, rather than losing it at the first save.
	 */
	private static void spillOverflow(ServerPlayer player, ItemStack backpack) {
		BackpackTier tier = tierOf(backpack);
		ItemContainerContents contents = backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		if (tier == null || contents.size() <= tier.slots()) {
			return;
		}
		List<ItemStack> all = contents.itemCopies().toList();
		List<ItemStack> kept = new ArrayList<>(all.subList(0, tier.slots()));
		backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(kept));
		for (ItemStack extra : all.subList(tier.slots(), all.size())) {
			if (!extra.isEmpty()) {
				player.getInventory().placeItemBackInInventory(extra, net.minecraft.util.Prediction.SERVER_ONLY);
			}
		}
	}

	// ------------------------------------------------------------------ death

	/**
	 * A player who dies without keepInventory drops their worn backpack at their feet, with everything in it,
	 * as {@code Inventory.dropAll} does the rest (see {@code mixin.PlayerGearMixin}). The slot is emptied first,
	 * so it can't be dropped, or carried to the respawned player, twice.
	 */
	public static void dropWorn(Player player) {
		ItemStack worn = worn(player);
		if (worn.isEmpty()) {
			return;
		}
		takeOff(player);
		if (EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
			return;
		}
		ItemEntity drop = player.createItemStackToDrop(worn, true, false);
		if (drop != null) {
			player.level().addFreshEntity(drop);
		}
	}

	public static void init() {
		// Whatever is still in the old player's slot comes with them: after a death only what dropWorn left
		// (keepInventory), never a backpack that dropped.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			ItemStack worn = worn(oldPlayer);
			if (!worn.isEmpty()) {
				wear(newPlayer, worn);
			}
		});
	}
}
