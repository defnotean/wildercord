package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
import net.minecraft.server.level.ServerLevel;

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
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties() {
		int count = BladeSmithingRules.reward(firstClear);
		ItemStack stack = new ItemStack(MASTER_STEEL, count);
		if (!player.getInventory().add(stack)) player.spawnAtLocation(stack);
		player.sendSystemMessage(Component.translatable("message.wildercord.master_steel", count).withStyle(ChatFormatting.GRAY));
	}
}
