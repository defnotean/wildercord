package dev.wildercord.gear;

import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The casting gear a player has in their gear slots ({@link GearSlot}): staff, focus and tome. This is
 * the public way to read and change it; both sides can read, from any player or avatar (the client sees the
 * synced copy, and so does everyone who can see the wearer), and changes belong on the server.
 *
 * <p>The pieces live in the {@code wildercord:gear} attachment, so they are saved with the player.
 * On death they drop like inventory items (Curse of Vanishing destroys its own) unless keepInventory is
 * on, when they stay with the player; either way a piece is never in two places.</p>
 */
public final class GearSlots {
	private GearSlots() {}

	/** What's in this slot: empty if nothing. This is the stored stack; copy it before changing it. */
	public static ItemStack get(Entity player, GearSlot slot) {
		ItemStack stack = worn(player).get(slot.id());
		return stack == null ? ItemStack.EMPTY : stack;
	}

	/** Everything in a gear slot, in slot order (a slot with nothing in it isn't listed). */
	public static Map<GearSlot, ItemStack> equipped(Entity player) {
		Map<String, ItemStack> worn = worn(player);
		Map<GearSlot, ItemStack> out = new LinkedHashMap<>();
		if (worn.isEmpty()) {
			return out;
		}
		for (GearSlot slot : GearSlot.all()) {
			ItemStack stack = worn.get(slot.id());
			if (stack != null && !stack.isEmpty()) {
				out.put(slot, stack);
			}
		}
		return out;
	}

	/** The gear in each slot, as the rule reads it (see {@link GearBonuses#of(Map, GearDef, GearDef)}). */
	public static Map<GearSlot, GearDef> defs(Entity player) {
		Map<String, ItemStack> worn = worn(player);
		Map<GearSlot, GearDef> out = new LinkedHashMap<>();
		if (worn.isEmpty()) {
			return out;
		}
		for (GearSlot slot : GearSlot.all()) {
			GearDef def = Gear.defOf(worn.getOrDefault(slot.id(), ItemStack.EMPTY));
			if (slot.accepts(def)) {
				out.put(slot, def);
			}
		}
		return out;
	}

	/** The slot a stack goes in: empty if it isn't casting gear. */
	public static Optional<GearSlot> slotFor(ItemStack stack) {
		return Optional.ofNullable(GearSlot.of(Gear.defOf(stack)));
	}

	/** Whether this stack goes in this slot. */
	public static boolean fits(GearSlot slot, ItemStack stack) {
		return !stack.isEmpty() && slot.accepts(Gear.defOf(stack));
	}

	/**
	 * Puts a piece in a slot, one of it, replacing what was there (take that out with {@link #clear}
	 * first if it should go back to the player). An empty stack clears the slot. Returns false, changing
	 * nothing, if the piece doesn't fit the slot.
	 */
	public static boolean set(Player player, GearSlot slot, ItemStack stack) {
		if (!stack.isEmpty() && !fits(slot, stack)) {
			return false;
		}
		Map<String, ItemStack> worn = worn(player);
		ItemStack old = worn.get(slot.id());
		if (stack.isEmpty() ? old == null : old != null && ItemStack.matches(old, stack)) {
			return true;
		}
		Map<String, ItemStack> next = new LinkedHashMap<>(worn);
		if (stack.isEmpty()) {
			next.remove(slot.id());
		} else {
			next.put(slot.id(), stack.copyWithCount(1));
		}
		player.setAttached(WildercordAttachments.GEAR, Map.copyOf(next));
		return true;
	}

	/** Empties a slot and returns what was in it (empty if nothing). */
	public static ItemStack clear(Player player, GearSlot slot) {
		ItemStack old = get(player, slot);
		if (!old.isEmpty()) {
			set(player, slot, ItemStack.EMPTY);
		}
		return old;
	}

	/** Whether any gear slot holds something. */
	public static boolean any(Entity player) {
		return !worn(player).isEmpty();
	}

	private static Map<String, ItemStack> worn(Entity player) {
		return player.getAttachedOrElse(WildercordAttachments.GEAR, Map.of());
	}

	// ------------------------------------------------------------------ death

	/**
	 * A player who dies without keepInventory drops what's in their gear slots at their feet, as
	 * {@code Inventory.dropAll} does the rest (see {@code mixin.PlayerGearMixin}); Curse of Vanishing pieces
	 * are destroyed instead. The slots are emptied first, so nothing can be dropped, or carried to the
	 * respawned player, twice.
	 */
	public static void dropAll(Player player) {
		Map<String, ItemStack> worn = worn(player);
		if (worn.isEmpty()) {
			return;
		}
		player.setAttached(WildercordAttachments.GEAR, Map.of());
		for (ItemStack stack : worn.values()) {
			if (stack.isEmpty() || EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				continue;
			}
			ItemEntity drop = player.createItemStackToDrop(stack, true, false);
			if (drop != null) {
				player.level().addFreshEntity(drop);
			}
		}
	}

	public static void init() {
		// Whatever the old player still has in their slots comes with them: after a death that is only
		// what dropAll left (keepInventory, or a spectator), never a piece that dropped; alive (leaving the
		// End) Fabric has already carried everything across, and this repeats it harmlessly.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			Map<String, ItemStack> worn = worn(oldPlayer);
			if (worn.isEmpty()) {
				return;
			}
			Map<String, ItemStack> copy = new LinkedHashMap<>();
			worn.forEach((id, stack) -> copy.put(id, stack.copy()));
			newPlayer.setAttached(WildercordAttachments.GEAR, Map.copyOf(copy));
		});
	}
}
