package dev.wildercord.client.compat;

import dev.wildercord.client.MastersHandMotionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import dev.wildercord.client.compat.vanilla.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** First-person hand state and renderer view across ItemInHandRenderer (26.2) and FirstPersonHandsAndItemsRenderState (26.3). */
public final class FirstPersonHandView {
	private final ItemInHandRenderer renderer;
	private final  AbstractClientPlayer player;

	private FirstPersonHandView(ItemInHandRenderer renderer,  AbstractClientPlayer player) {
		this.renderer = renderer;
		this.player = player;
	}

	public static FirstPersonHandView of(ItemInHandRenderer renderer,  AbstractClientPlayer player) {
		return new FirstPersonHandView(renderer, player);
	}

	public static FirstPersonHandView of(ItemInHandRenderer renderer) {
		return new FirstPersonHandView(renderer, null);
	}

	public boolean equipKnown() {
		return renderer instanceof MastersHandMotionState;
	}

	public boolean mainHandEquipping() {
		return !(renderer instanceof MastersHandMotionState motion) || motion.wildercord$mainHandEquipping();
	}

	public ItemStack mainHandItem() {
		return renderer instanceof MastersHandMotionState motion ? motion.wildercord$mainHandItem() : ItemStack.EMPTY;
	}

	public float mainHandHeight() {
		return renderer instanceof MastersHandMotionState motion ? motion.wildercord$mainHandHeight() : 0.0F;
	}

	public float oldMainHandHeight() {
		return renderer instanceof MastersHandMotionState motion ? motion.wildercord$oldMainHandHeight() : 0.0F;
	}

	public float mainHandSwapScale() {
		return RenderCompat.shouldPlaySwapAnimation(mainHandItem()) ? 1.0F : 1.0F;
	}

	public boolean isScoping() {
		return player != null && player.isScoping();
	}

	public boolean hasMainHandMapData(ItemStack item) {
		return item != null && item.has(DataComponents.MAP_ID);
	}

	public boolean isFallbackItemInvalid(ItemStack stack) {
		return !equipKnown()
			|| !ItemStack.isSameItemSameComponents(stack, mainHandItem())
			|| isScoping();
	}

	public  ItemStackRenderState initialMainHandRenderState() {
		return null;
	}

	public Object rendererObject() {
		return renderer;
	}

	public Object stateObject() {
		return renderer;
	}

	public String rendererClassName() {
		return renderer.getClass().getName();
	}
}
