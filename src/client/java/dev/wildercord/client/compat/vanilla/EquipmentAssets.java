package dev.wildercord.client.compat.vanilla;

import net.minecraft.resources.ResourceLocation;

/**
 * 1.21.3 stand-in for the 1.21.4 {@code EquipmentAssets}. In 1.21.3 an item's equipment model is
 * named by a plain {@link ResourceLocation} ({@code Equippable#model()}); in 1.21.4 it became a
 * {@code ResourceKey<EquipmentAsset>}. Only the netherite id is used by the mod.
 */
public final class EquipmentAssets {
	public static final ResourceLocation NETHERITE = ResourceLocation.fromNamespaceAndPath("minecraft", "netherite");

	private EquipmentAssets() {}
}
