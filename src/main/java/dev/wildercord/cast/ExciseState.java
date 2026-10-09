package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Commitment/presentation are body-bound. Only absolute recovery/rest deadlines survive replacement. */
public record ExciseState(int phase, int slot, long emitter, long began, long until, Vec3 core, long serverTick, long levelTick) {
    public static final int HOLDING = 1, CUT = 2, CANCELLED = 3;
    public long clock(long clientLevelTick) { return serverTick + Math.max(0, clientLevelTick - levelTick); }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ExciseState> STREAM = StreamCodec.of((b,s) -> {
        b.writeVarInt(s.phase); b.writeVarInt(s.slot); b.writeLong(s.emitter); b.writeLong(s.began); b.writeLong(s.until);
        b.writeDouble(s.core.x); b.writeDouble(s.core.y); b.writeDouble(s.core.z); b.writeLong(s.serverTick); b.writeLong(s.levelTick);
    }, b -> new ExciseState(b.readVarInt(), b.readVarInt(), b.readLong(), b.readLong(), b.readLong(),
        new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readLong(), b.readLong()));
    public static final AttachmentType<ExciseState> VIEW = AttachmentRegistry.create(Wildercord.id("excise_view"), b -> b.syncWith(STREAM, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Long> RECOVERY = AttachmentRegistry.create(Wildercord.id("excise_recovery"),
        b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath().syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));
    public static final AttachmentType<Long> REST = AttachmentRegistry.create(Wildercord.id("excise_rest"),
        b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath().syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));
}
