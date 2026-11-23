package dev.wildercord.compat;

import dev.wildercord.compat.McStreamCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * 1.21.1 has no {@code Vec3.STREAM_CODEC}; the mod's client/server payload codecs use this
 * double-triple codec instead. Both sides use it, so the wire format is self-consistent.
 */
public final class McVec3 {
	private McVec3() {}

	public static final StreamCodec<RegistryFriendlyByteBuf, Vec3> STREAM_CODEC = McStreamCodec.composite(
		ByteBufCodecs.DOUBLE, Vec3::x,
		ByteBufCodecs.DOUBLE, Vec3::y,
		ByteBufCodecs.DOUBLE, Vec3::z,
		Vec3::new);
}
