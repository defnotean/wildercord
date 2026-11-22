package dev.wildercord.compat;

import java.util.Optional;
import java.util.UUID;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.LivingEntity;

/**
 * The synced-data serializer 1.21.5 registers as {@code EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE}.
 * 1.21.4 has no entity references to serialise, so this rides the uuid form: the same
 * {@link EntityReference} the mod saves, carried as an optional uuid on the wire.
 */
public final class McEntitySerializers {

	private McEntitySerializers() {}

	public static final EntityDataSerializer<Optional<EntityReference<LivingEntity>>> OPTIONAL_LIVING_ENTITY_REFERENCE =
		register(EntityDataSerializer.forValueType(
			StreamCodec.<ByteBuf, Optional<UUID>>of(
				(ByteBuf buf, Optional<UUID> reference) -> {
					if (reference.isPresent()) {
						buf.writeBoolean(true);
						buf.writeLong(reference.get().getMostSignificantBits());
						buf.writeLong(reference.get().getLeastSignificantBits());
					} else {
						buf.writeBoolean(false);
					}
				},
				(ByteBuf buf) -> buf.readBoolean() ? Optional.of(new UUID(buf.readLong(), buf.readLong())) : Optional.empty())
				.map(optional -> optional.map(id -> new EntityReference<LivingEntity>(id)),
					optional -> optional.map(EntityReference::getUUID))));

	// 1.21.4 hands out raw ids from a BiMap: an unregistered serializer has no id and
	// SynchedEntityData.defineId throws "Unregistered serializer … for N!" without this.
	private static <T> EntityDataSerializer<T> register(EntityDataSerializer<T> serializer) {
		EntityDataSerializers.registerSerializer(serializer);
		return serializer;
	}
}
