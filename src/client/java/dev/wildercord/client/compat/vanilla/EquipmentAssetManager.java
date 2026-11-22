package dev.wildercord.client.compat.vanilla;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.model.EquipmentModelSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.equipment.EquipmentModel;

/**
 * 1.21.3 stand-in for the 1.21.4 {@code EquipmentAssetManager}. In 1.21.3 the model set is
 * {@link EquipmentModelSet} and the resolved info is {@link EquipmentModel}; the stand-in translates
 * its layer keys into the shape the shared articulated-armor sources expect.
 */
public class EquipmentAssetManager {
	public static final EquipmentClientInfo MISSING = new EquipmentClientInfo(Map.of());

	private final EquipmentModelSet models;

	public EquipmentAssetManager(EquipmentModelSet models) {
		this.models = models;
	}

	public EquipmentClientInfo get(ResourceLocation id) {
		EquipmentModel model = models.get(id);
		if (model == EquipmentModelSet.MISSING_MODEL) return MISSING;
		Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers =
			new EnumMap<>(EquipmentClientInfo.LayerType.class);
		model.layers().forEach((vanilla, list) -> {
			EquipmentClientInfo.LayerType type = EquipmentClientInfo.LayerType.of(vanilla);
			if (type != null && !list.isEmpty()) layers.put(type, List.of(new EquipmentClientInfo.Layer()));
		});
		return new EquipmentClientInfo(layers);
	}
}
