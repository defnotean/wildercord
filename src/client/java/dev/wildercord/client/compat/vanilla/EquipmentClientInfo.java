package dev.wildercord.client.compat.vanilla;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.equipment.EquipmentModel;

/**
 * 1.21.3 stand-in for the 1.21.4 {@code EquipmentClientInfo}. 1.21.3's equivalent is
 * {@link EquipmentModel}; the mod only needs the layer-type keys, so the stand-in keeps those and a
 * placeholder layer per key. Any 1.21.4-only fields are absent.
 */
public final class EquipmentClientInfo {
	public enum LayerType {
		HUMANOID, HUMANOID_LEGGINGS, WINGS, WOLF_BODY, HORSE_BODY, LLAMA_BODY;

		public EquipmentModel.LayerType vanilla() {
			return EquipmentModel.LayerType.valueOf(name());
		}

		public static LayerType of(EquipmentModel.LayerType vanilla) {
			return vanilla == null ? null : valueOf(vanilla.name());
		}
	}

	/** Placeholder for a real 1.21.4 layer; the mod reads only whether one is present. */
	public static final class Layer {}

	private final Map<LayerType, List<Layer>> layers;

	public EquipmentClientInfo(Map<LayerType, List<Layer>> layers) {
		this.layers = new EnumMap<>(layers);
	}

	public List<Layer> getLayers(LayerType type) {
		return layers.getOrDefault(type, List.of());
	}

	public Map<LayerType, List<Layer>> layers() {
		return layers;
	}
}
