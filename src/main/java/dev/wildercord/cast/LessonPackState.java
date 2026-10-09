package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.spell.LessonPackRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Placed gates, threads and rods are body-bound; only each lesson's absolute rest deadline survives replacement. */
public record LessonPackState(int lesson, int slot, long until, Vec3 at, long serverTick, long levelTick) {
    public long clock(long clientLevelTick) { return serverTick + Math.max(0, clientLevelTick - levelTick); }
    public LessonPackRules.Lesson kind() { return lesson >= 0 && lesson < LessonPackRules.ALL.size() ? LessonPackRules.ALL.get(lesson) : null; }
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, LessonPackState> STREAM = StreamCodec.of((b,s) -> {
        b.writeVarInt(s.lesson); b.writeVarInt(s.slot); b.writeLong(s.until);
        b.writeDouble(s.at.x); b.writeDouble(s.at.y); b.writeDouble(s.at.z); b.writeLong(s.serverTick); b.writeLong(s.levelTick);
    }, b -> new LessonPackState(b.readVarInt(), b.readVarInt(), b.readLong(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readLong(), b.readLong()));
    /** The owner's own HUD readout of their newest placed lesson object. */
    public static final AttachmentType<LessonPackState> VIEW = AttachmentRegistry.create(Wildercord.id("lesson_pack_view"), b -> b.syncWith(STREAM, AttachmentSyncPredicate.targetOnly()));
    private static AttachmentType<Long> rest(String path) {
        return AttachmentRegistry.create(Wildercord.id(path + "_rest"),
            b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath().syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly()));
    }
    private static final AttachmentType<Long> TOLLGATE_REST = rest("tollgate"), LIFELINE_REST = rest("lifeline"), CONDUIT_REST = rest("conduit");
    public static AttachmentType<Long> rest(LessonPackRules.Lesson lesson) {
        return switch (lesson) { case TOLLGATE -> TOLLGATE_REST; case LIFELINE -> LIFELINE_REST; case CONDUIT -> CONDUIT_REST; };
    }
}
