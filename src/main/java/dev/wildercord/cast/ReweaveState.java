package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Server-owned footprint presentation; only the separate rest survives a body/world/session transition. */
public record ReweaveState(int phase, int slot, long fieldId, long created, long expires, long warningUntil,
                           int nextBeat, Vec3 center, Vec3 end, boolean blocked, long serverTick, long levelTick) {
    public static final int DISC = 1, WARNING = 2, LANE = 3;
    public long clock(long clientLevelTick) { return serverTick + Math.max(0, clientLevelTick - levelTick); }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ReweaveState> STREAM = StreamCodec.of((b,s) -> {
        b.writeVarInt(s.phase); b.writeVarInt(s.slot); b.writeLong(s.fieldId); b.writeLong(s.created); b.writeLong(s.expires);
        b.writeLong(s.warningUntil); b.writeVarInt(s.nextBeat);
        b.writeDouble(s.center.x); b.writeDouble(s.center.y); b.writeDouble(s.center.z);
        b.writeDouble(s.end.x); b.writeDouble(s.end.y); b.writeDouble(s.end.z);
        b.writeBoolean(s.blocked); b.writeLong(s.serverTick); b.writeLong(s.levelTick);
    }, b -> new ReweaveState(b.readVarInt(), b.readVarInt(), b.readLong(), b.readLong(), b.readLong(), b.readLong(), b.readVarInt(),
        new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readBoolean(), b.readLong(), b.readLong()));
    public static final AttachmentType<ReweaveState> VIEW = AttachmentRegistry.create(Wildercord.id("reweave_view"),
        b -> b.syncWith(STREAM, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Long> REST = AttachmentRegistry.create(Wildercord.id("reweave_rest"),
        b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath().syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));
}
