package dev.wildercord.client.combat;

import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;

import java.util.Set;

/** The resolved asset can add geometry-bearing vanilla layers without changing the item or slot bake. */
public final class ArticulatedArmorAssets {
	private ArticulatedArmorAssets() {}
	// Stock netherite also defines non-player equipment. These are never submitted by AvatarRenderer.
	private static final Set<LayerType> STOCK_LAYERS = Set.of(LayerType.HUMANOID, LayerType.HUMANOID_LEGGINGS,
		LayerType.HUMANOID_BABY, LayerType.HORSE_BODY, LayerType.NAUTILUS_BODY);

	/** Texture, dye and trim replacements remain valid; wings and unrecognized geometry use fallback. */
	public static boolean supported(EquipmentClientInfo asset) {
		return asset != null && !asset.getLayers(LayerType.HUMANOID).isEmpty()
			&& !asset.getLayers(LayerType.HUMANOID_LEGGINGS).isEmpty()
			&& asset.layers().entrySet().stream().allMatch(entry -> entry.getValue().isEmpty() || STOCK_LAYERS.contains(entry.getKey()));
	}
}
