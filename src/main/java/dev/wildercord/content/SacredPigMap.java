package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Coordinates to the Sacred Pig village, carried on a boss-dropped paper. */
public record SacredPigMap(int x, int z) {
	public static final Codec<SacredPigMap> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("x").forGetter(SacredPigMap::x),
		Codec.INT.fieldOf("z").forGetter(SacredPigMap::z)
	).apply(i, SacredPigMap::new));

	public static final StreamCodec<ByteBuf, SacredPigMap> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SacredPigMap::x,
		ByteBufCodecs.VAR_INT, SacredPigMap::z,
		SacredPigMap::new);
}
