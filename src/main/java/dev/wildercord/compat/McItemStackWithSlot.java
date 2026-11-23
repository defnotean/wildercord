package dev.wildercord.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.compat.McItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

/**
 * An item and the container slot it belongs to, the shape {@code McItemStackWithSlot} has from 1.21.6.
 * 1.21.5 has no such record, so the belly of a coffer mimic stores this instead.
 */
public record McItemStackWithSlot(int slot, ItemStack stack) {

	public static final Codec<McItemStackWithSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("Slot").forGetter(McItemStackWithSlot::slot),
		ItemStack.CODEC.fieldOf("Stack").forGetter(McItemStackWithSlot::stack))
		.apply(instance, McItemStackWithSlot::new));

	/** True when the slot is inside a container of {@code size} and something is in it. */
	public boolean isValidInContainer(int size) {
		return slot >= 0 && slot < size && !stack.isEmpty();
	}
}
