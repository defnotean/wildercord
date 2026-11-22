package dev.wildercord.compat;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.level.entity.UniquelyIdentifyable;

/**
 * Reads and writes an {@link EntityReference} through the value compat pair. 1.21.5 only carries the
 * nbt forms ({@code store(CompoundTag, String)} and {@code read(CompoundTag, String)}), so the value
 * wrappers are unwrapped here rather than at every call site.
 */
public final class McEntityReference {

	private McEntityReference() {}

	/** Records {@code reference} under {@code key}, or writes nothing when it is null. */
	public static <T extends UniquelyIdentifyable> void store(EntityReference<T> reference, McValueOutput out, String key) {
		if (reference != null) {
			reference.store(out.tag(), key);
		}
	}

	/** The reference stored under {@code key}, or null. */
	public static <T extends UniquelyIdentifyable> EntityReference<T> read(McValueInput in, String key) {
		return EntityReference.read(in.tag(), key);
	}
}
