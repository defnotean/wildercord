package dev.wildercord.client.compat;

import dev.wildercord.client.MastersHandMotionState;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** First-person hand state and renderer view across ItemInHandRenderer (26.2) and FirstPersonHandsAndItemsRenderState (26.3). */
public final class FirstPersonHandView {
	private final @Nullable FirstPersonHandsAndItemsRenderer renderer;
	private final FirstPersonHandsAndItemsRenderState state;

	private FirstPersonHandView(@Nullable FirstPersonHandsAndItemsRenderer renderer, FirstPersonHandsAndItemsRenderState state) {
		this.renderer = renderer;
		this.state = state;
	}

	public static FirstPersonHandView of(FirstPersonHandsAndItemsRenderer renderer, FirstPersonHandsAndItemsRenderState state) {
		return new FirstPersonHandView(renderer, state);
	}

	public static FirstPersonHandView of(FirstPersonHandsAndItemsRenderState state) {
		return new FirstPersonHandView(null, state);
	}

	public boolean equipKnown() {
		return state instanceof MastersHandMotionState;
	}

	public boolean mainHandEquipping() {
		return !(state instanceof MastersHandMotionState motion) || motion.wildercord$mainHandEquipping();
	}

	public ItemStack mainHandItem() {
		return state.mainHandItem;
	}

	public float mainHandHeight() {
		return state.mainHandHeight;
	}

	public float oldMainHandHeight() {
		return state.oldMainHandHeight;
	}

	public float mainHandSwapScale() {
		return state.mainHandSwapScale;
	}

	public boolean isScoping() {
		return state.isScoping;
	}

	public boolean hasMainHandMapData(ItemStack item) {
		return state.hasMainHandMapData;
	}

	public boolean isFallbackItemInvalid(ItemStack stack) {
		return !ItemStack.isSameItemSameComponents(stack, state.mainHandItem) || state.mainHandRenderState.isEmpty();
	}

	public @Nullable ItemStackRenderState initialMainHandRenderState() {
		return state.mainHandRenderState;
	}

	public @Nullable Object rendererObject() {
		return renderer;
	}

	public Object stateObject() {
		return state;
	}

	public String rendererClassName() {
		return renderer != null ? renderer.getClass().getName() : "";
	}
}
