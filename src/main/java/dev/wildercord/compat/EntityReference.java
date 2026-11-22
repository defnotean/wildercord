package dev.wildercord.compat;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/**
 * A saved reference to an entity by uuid. 1.21.5 ships {@code net.minecraft.world.entity.EntityReference};
 * 1.21.4 has nothing of the sort, so the mod carries this uuid-only stand-in. It resolves through the
 * level like the vanilla one: {@code level.getEntity(id)} on the caller's side.
 */
public final class EntityReference<T> {

	private final UUID id;

	public EntityReference(UUID id) {
		this.id = id;
	}

	public UUID getUUID() {
		return id;
	}

	/** Records the reference under {@code key}, the shape the vanilla one saves. */
	public void store(CompoundTag tag, String key) {
		tag.putUUID(key, id);
	}

	/** The reference saved under {@code key}, or null. */
	public static <T> EntityReference<T> read(CompoundTag tag, String key) {
		return tag.hasUUID(key) ? new EntityReference<>(tag.getUUID(key)) : null;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EntityReference<?> reference && reference.id.equals(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "EntityReference[" + id + "]";
	}
}
