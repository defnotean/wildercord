package dev.wildercord.client;

import net.minecraft.world.item.ItemStack;

/**
 * The stacks a living entity really holds, carried on its armed render state: 1.21.10 render states
 * bake only quads and keep no ItemStacks, so the renderer notes the real stacks as each state is extracted.
 */
public interface HandStacks {
	ItemStack[] wildercord$handStacks();

	void wildercord$setHandStacks(ItemStack[] stacks);
}
