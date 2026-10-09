package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Presentation only: endpoints and lifetime are chosen by the server and never sent back by a client. */
public record RelayState(int phase, int slot, long start, long until, Vec3 focus, Vec3 end, int color) {
	public static final int PLACED = 1, WARNING = 2, RECOVERING = 3;
	public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RelayState> STREAM = StreamCodec.of((b, s) -> {
		b.writeVarInt(s.phase); b.writeVarInt(s.slot); b.writeLong(s.start); b.writeLong(s.until);
		b.writeDouble(s.focus.x); b.writeDouble(s.focus.y); b.writeDouble(s.focus.z);
		b.writeDouble(s.end.x); b.writeDouble(s.end.y); b.writeDouble(s.end.z); b.writeInt(s.color);
	}, b -> new RelayState(b.readVarInt(), b.readVarInt(), b.readLong(), b.readLong(),
		new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readInt()));
	public static final AttachmentType<RelayState> VIEW = AttachmentRegistry.create(Wildercord.id("relay_view"),
		b -> b.syncWith(STREAM, AttachmentSyncPredicate.all()));
	/** A global-world clock, persisted through reconnect and copied through death; never reset by slot changes. */
	public static final AttachmentType<Long> REST = AttachmentRegistry.create(Wildercord.id("relay_rest"),
		b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath().syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));
}
