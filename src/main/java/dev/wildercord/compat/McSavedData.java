package dev.wildercord.compat;

import dev.wildercord.compat.McSavedDataType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Reads a {@link McSavedDataType} store off a 1.21.4 {@link DimensionDataStorage}, which takes the
 * factory and the name separately where 1.21.5 hands it a single type object.
 */
public final class McSavedData {

	private McSavedData() {}

	/** The store, created when the level has not saved one yet. */
	public static <T extends SavedData> T computeIfAbsent(DimensionDataStorage storage, McSavedDataType<T> type) {
		return storage.computeIfAbsent(type.vanilla(), type.name);
	}

	/** The store as saved, or null when the level has not written it yet. */
	public static <T extends SavedData> T get(DimensionDataStorage storage, McSavedDataType<T> type) {
		return storage.get(type.vanilla(), type.name);
	}
}
