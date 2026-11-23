package dev.wildercord.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;

/** 1.21.1 compat seam for the dropped first-person hand view: every method is inert. */
public final class FirstPersonHandView {
	private final ItemInHandRenderer renderer;
	private final AbstractClientPlayer player;

	private FirstPersonHandView(ItemInHandRenderer renderer, AbstractClientPlayer player) {
		this.renderer = renderer;
		this.player = player;
	}

	public static FirstPersonHandView of(ItemInHandRenderer renderer, AbstractClientPlayer player) {
		return new FirstPersonHandView(renderer, player);
	}

	public static FirstPersonHandView of(ItemInHandRenderer renderer) {
		return new FirstPersonHandView(renderer, null);
	}

	public boolean equipKnown() { return false; }

	public boolean mainHandEquipping() { return false; }

	public ItemStack mainHandItem() { return ItemStack.EMPTY; }

	public float mainHandHeight() { return 0F; }

	public float oldMainHandHeight() { return 0F; }

	public float mainHandSwapScale() { return 1F; }

	public boolean isScoping() { return false; }

	public boolean hasMainHandMapData(ItemStack item) { return false; }

	public boolean isFallbackItemInvalid(ItemStack stack) { return false; }

	public Object initialMainHandRenderState() { return null; }

	public Object rendererObject() { return renderer; }

	public Object stateObject() { return renderer; }

	public String rendererClassName() { return renderer != null ? renderer.getClass().getName() : ""; }
}
