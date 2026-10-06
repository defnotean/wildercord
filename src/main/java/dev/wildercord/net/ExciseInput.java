package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.ExciseCasting;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Physical edges only; the server chooses and permanently locks one native core. */
public record ExciseInput(int action, int slot, long nonce) implements CustomPacketPayload {
    public static final Type<ExciseInput> TYPE = new Type<>(Wildercord.id("excise_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExciseInput> CODEC = StreamCodec.of((b,p) -> {
        b.writeVarInt(p.action); b.writeVarInt(p.slot); b.writeLong(p.nonce);
    }, b -> new ExciseInput(b.readVarInt(), b.readVarInt(), b.readLong()));
    @Override public Type<ExciseInput> type() { return TYPE; }
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (p,c) -> ExciseCasting.input(c.player(), p.action, p.slot, p.nonce));
    }
}
