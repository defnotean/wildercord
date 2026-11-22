package dev.wildercord.client.mixin;

import dev.wildercord.client.HandStacks;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** 1.21.3 states already carry their stacks; this duck stays attached for callers that still ask for it. */
@Mixin(LivingEntityRenderState.class)
public abstract class ArmedEntityRenderStateMixin implements HandStacks {
	@Unique
	private ItemStack[] wildercord$stacks = new ItemStack[0];

	@Override
	@Unique
	public ItemStack[] wildercord$handStacks() {
		return wildercord$stacks;
	}

	@Override
	@Unique
	public void wildercord$setHandStacks(ItemStack[] stacks) {
		wildercord$stacks = stacks;
	}
}
