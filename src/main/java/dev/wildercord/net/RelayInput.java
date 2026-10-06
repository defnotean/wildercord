package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.RelayCircles;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** No client origin, target, price, entitlement or completion claim is accepted. */
public record RelayInput(int action, int slot, long nonce) implements CustomPacketPayload {
	public static final Type<RelayInput> TYPE = new Type<>(Wildercord.id("relay_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RelayInput> CODEC = StreamCodec.of(
		(b, p) -> { b.writeVarInt(p.action); b.writeVarInt(p.slot); b.writeVarLong(p.nonce); },
		b -> new RelayInput(b.readVarInt(), b.readVarInt(), b.readVarLong()));
	@Override public Type<RelayInput> type() { return TYPE; }
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (p, c) -> RelayCircles.input(c.player(), p.action, p.slot, p.nonce));
	}
}
