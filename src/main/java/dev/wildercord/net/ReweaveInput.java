package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.ReweaveFields;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A physical edge and the exact server field it addresses; never a client-supplied footprint or payment. */
public record ReweaveInput(int action, int slot, long nonce, long fieldId) implements CustomPacketPayload {
    public static final Type<ReweaveInput> TYPE = new Type<>(Wildercord.id("reweave_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReweaveInput> CODEC = StreamCodec.of((b,p) -> {
        b.writeVarInt(p.action); b.writeVarInt(p.slot); b.writeLong(p.nonce); b.writeLong(p.fieldId);
    }, b -> new ReweaveInput(b.readVarInt(), b.readVarInt(), b.readLong(), b.readLong()));
    @Override public Type<ReweaveInput> type() { return TYPE; }
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (p,c) -> ReweaveFields.input(c.player(), p.action, p.slot, p.nonce, p.fieldId));
    }
}
