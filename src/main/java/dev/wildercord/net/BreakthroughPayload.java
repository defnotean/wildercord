package dev.wildercord.net;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A circle has just formed: the client shows what the breakthrough brought ({@code tribulation} when it was won in one). */
public record BreakthroughPayload(int circle, boolean tribulation) implements CustomPacketPayload {
	public static final Type<BreakthroughPayload> TYPE = new Type<>(Wildercord.id("breakthrough"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BreakthroughPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, BreakthroughPayload::circle, ByteBufCodecs.BOOL, BreakthroughPayload::tribulation, BreakthroughPayload::new);

	@Override public Type<BreakthroughPayload> type() { return TYPE; }

	public static void init() { PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC); }
}
