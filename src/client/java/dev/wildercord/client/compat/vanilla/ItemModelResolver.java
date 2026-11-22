package dev.wildercord.client.compat.vanilla;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.3 stand-in for the 1.21.4 {@code ItemModelResolver} (1.21.3 resolves models inside
 * {@code ItemRenderer#getModel}). {@link #updateForLiving} only records what a later
 * {@link ItemStackRenderState#render} needs; no model override exists here.
 */
public class ItemModelResolver {
	public void updateForLiving(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context,
			boolean isLeftHand, LivingEntity entity) {
		state.prepare(stack, context, entity);
	}

	public boolean shouldPlaySwapAnimation(ItemStack stack) {
		return false;
	}
}
