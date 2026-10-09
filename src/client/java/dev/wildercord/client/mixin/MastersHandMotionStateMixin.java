package dev.wildercord.client.mixin;

import dev.wildercord.client.MastersHandMotionState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Unknown/preview states preserve vanilla until the native extractor supplies provenance. */
@Mixin(FirstPersonHandsAndItemsRenderState.class)
public abstract class MastersHandMotionStateMixin implements MastersHandMotionState {
	@Unique private boolean wildercord$equipping = true;
	@Override public boolean wildercord$mainHandEquipping() { return wildercord$equipping; }
	@Override public void wildercord$mainHandEquipping(boolean value) { wildercord$equipping = value; }
}
