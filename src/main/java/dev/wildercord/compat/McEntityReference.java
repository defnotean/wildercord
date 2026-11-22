package dev.wildercord.compat;

import net.minecraft.nbt.CompoundTag;

/**
 * Reads and writes an {@link EntityReference} through the value compat pair. 1.21.4 has no vanilla
 * reference class, so the store and load live on the mod's own uuid-only stand-in and are unwrapped
 * here rather than at every call site.
 */
public final class McEntityReference {

	private McEntityReference() {}

	/** Records {@code reference} under {@code key}, or writes nothing when it is null. */
	public static <T> void store(EntityReference<T> reference, McValueOutput out, String key) {
		if (reference != null) {
			reference.store(out.tag(), key);
		}
	}

	/** The reference stored under {@code key}, or null. */
	public static <T> EntityReference<T> read(McValueInput in, String key) {
		return EntityReference.read(in.tag(), key);
	}
}
