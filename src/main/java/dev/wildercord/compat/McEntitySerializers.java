package dev.wildercord.compat;

import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.LivingEntity;

/**
 * The synced-data serializer 1.21.5 registers as {@code EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE}.
 * 1.21.4 has no entity references to serialise, so this rides the uuid form: the same
 * {@link EntityReference} the mod saves, carried as an optional uuid on the wire.
 */
public final class McEntitySerializers {

	private McEntitySerializers() {}

	public static final EntityDataSerializer<Optional<EntityReference<LivingEntity>>> OPTIONAL_LIVING_ENTITY_REFERENCE =
		EntityDataSerializer.forValueType(
			ByteBufCodecs.optional(ByteBufCodecs.UUID)
				.map(optional -> optional.map(EntityReference::<LivingEntity>new),
					reference -> Optional.of(reference.getUUID())));
}
