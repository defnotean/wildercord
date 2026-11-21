package dev.wildercord.compat;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.8 has no {@code ServerLevel.getEntityInAnyDimension}: a saved entity id has to be looked up
 * level by level, which is what the newer method does anyway.
 */
public final class McEntities {
	private McEntities() {}

	/** The entity with that id anywhere on the server, or null when it is gone. */
	public static Entity entityAnywhere(ServerLevel anyLevel, UUID id) {
		for (ServerLevel level : anyLevel.getServer().getAllLevels()) {
			Entity found = level.getEntity(id);
			if (found != null) {
				return found;
			}
		}
		return null;
	}
}
