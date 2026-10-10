package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Blade smithing (0.13; the numbers are {@link BladeSmithingRules}'). A Sword Master leaves a sliver of Master's Steel for every
 * swordsman credited with their fall. At an anvil, a bonded blade on the left and steel on the right tempers it one level, as far
 * as its bond allows. A tempered blade's coated blows land harder, under the same caps as everything else in {@code AuraCombat}.
 * The temper is its own component on the blade, so it travels with it (and with a passed-on bond) and never touches the bond's own
 * record.
 */
public final class BladeSmithing {
	private BladeSmithing() {}

	public static final DataComponentType<Integer> TEMPER = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("blade_temper"), DataComponentType.<Integer>builder().persistent(Codec.intRange(0, BladeSmithingRules.MAX_TEMPER))
			.networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** A sliver of a Master's blade, still ringing. */
	public static final Item MASTER_STEEL = steel();

	private static Item steel() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id("master_steel"));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).rarity(Rarity.EPIC)
			.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
				Component.translatable("item.wildercord.master_steel.use").withStyle(ChatFormatting.GRAY))))));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.accept(MASTER_STEEL));
	}

	public static int temper(ItemStack stack) {
		return stack == null || stack.isEmpty() ? 0 : stack.getOrDefault(TEMPER, 0);
	}

	/** What tempering {@code blade} with {@code steel} at an anvil makes, how much steel and how many levels it takes. */
	public record Result(ItemStack out, int steel, int levels) {}

	/**
	 * The anvil's answer for {@code blade} (left) and {@code steel} (right) under {@code smith}'s hands, or null when it isn't a
	 * temper: not a bonded blade of theirs, not enough steel, or tempered as far as its bond allows.
	 */
	public static Result temper(ItemStack blade, ItemStack steel, Player smith) {
		if (blade.getCount() != 1 || !steel.is(MASTER_STEEL) || BondedBlades.foreign(smith, blade)) return null;
		BladeBond bond = BondedBlades.bond(blade);
		if (bond == null) return null;
		int now = temper(blade);
		if (!BladeSmithingRules.canTemper(now, bond.tier())) return null;
		int next = now + 1;
		int needed = BladeSmithingRules.steel(next);
		if (steel.getCount() < needed) return null;
		ItemStack out = blade.copy();
		out.set(TEMPER, next);
		return new Result(out, needed, BladeSmithingRules.levels(next));
	}

	/** What the blade in {@code player}'s hand multiplies a coated blow's bonus by: its temper, if it is theirs and bonds are on. */
	public static double coat(ServerPlayer player, LivingEntity target) {
		if (!BondedBlades.on(player) || BondedBlades.held(player) == null) return 1.0;
		return BladeSmithingRules.coat(temper(player.getMainHandItem()), target instanceof Player);
	}

	/** A Master fell and {@code player} was credited: their share of the steel. */
	public static void reward(ServerPlayer player, boolean firstClear) {
		int count = BladeSmithingRules.reward(firstClear);
		ItemStack stack = new ItemStack(MASTER_STEEL, count);
		if (!player.getInventory().add(stack)) player.spawnAtLocation(player.level(), stack);
		player.sendSystemMessage(Component.translatable("message.wildercord.master_steel", count).withStyle(ChatFormatting.GRAY));
	}
}
