package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.LessonPackCasting;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Physical edges only; the server picks the floor, ally or rod and owns every placed object. */
public record LessonPackInput(int action, int slot, long nonce) implements CustomPacketPayload {
    public static final Type<LessonPackInput> TYPE = new Type<>(Wildercord.id("lesson_pack_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LessonPackInput> CODEC = StreamCodec.of((b,p) -> {
        b.writeVarInt(p.action); b.writeVarInt(p.slot); b.writeLong(p.nonce);
    }, b -> new LessonPackInput(b.readVarInt(), b.readVarInt(), b.readLong()));
    @Override public Type<LessonPackInput> type() { return TYPE; }
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (p,c) -> LessonPackCasting.input(c.player(), p.action, p.slot, p.nonce));
    }
}
