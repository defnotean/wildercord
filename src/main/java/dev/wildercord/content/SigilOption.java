package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A magic circle drawn in the world: a large flat glowing sigil, tinted, facing any direction,
 * turning slowly and fading in and out. One particle per layer; stack several (a circle, a rune
 * ring, a star) for the full effect.
 *
 * @param style    which sigil: {@link #CIRCLE}, {@link #RING}, {@link #STAR}, {@link #TARGET}, {@link #CRACKED}
 *                 {@link #GLOW} (a flash of light that always faces the viewer; yaw, pitch and spin are ignored)
 *                 or {@link #BAND} (a thin plain ring of the same width at any size: one rune's ring on a spell's circle)
 * @param color    0xRRGGBB tint
 * @param size     radius in blocks
 * @param yaw      the way the circle faces, as a view direction (degrees, like an entity's yaw)
 * @param pitch    ... and pitch (-90 faces straight up: a circle lying on the ground)
 * @param lifetime ticks
 * @param spin     radians per tick around its own centre (negative turns the other way)
 */
public record SigilOption(int style, int color, float size, float yaw, float pitch, int lifetime, float spin) implements ParticleOptions {
	public static final int CIRCLE = 0;
	public static final int RING = 1;
	public static final int STAR = 2;
	public static final int TARGET = 3;
	public static final int CRACKED = 4;
	public static final int GLOW = 5;
	public static final int BAND = 6;
	public static final int STYLES = 7;

	/** A flash of light {@code size} blocks across at its brightest (see {@link #GLOW}). */
	public static SigilOption glow(int color, float size) {
		return new SigilOption(GLOW, color & 0xFFFFFF, size / 2, 0, 0, 7, 0);
	}

	public static final MapCodec<SigilOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("style").forGetter(SigilOption::style),
		Codec.INT.fieldOf("color").forGetter(SigilOption::color),
		Codec.FLOAT.fieldOf("size").forGetter(SigilOption::size),
		Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(SigilOption::yaw),
		Codec.FLOAT.optionalFieldOf("pitch", -90F).forGetter(SigilOption::pitch),
		Codec.INT.optionalFieldOf("lifetime", 40).forGetter(SigilOption::lifetime),
		Codec.FLOAT.optionalFieldOf("spin", 0.05F).forGetter(SigilOption::spin)
	).apply(i, SigilOption::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SigilOption> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SigilOption::style,
		ByteBufCodecs.INT, SigilOption::color,
		ByteBufCodecs.FLOAT, SigilOption::size,
		ByteBufCodecs.FLOAT, SigilOption::yaw,
		ByteBufCodecs.FLOAT, SigilOption::pitch,
		ByteBufCodecs.VAR_INT, SigilOption::lifetime,
		ByteBufCodecs.FLOAT, SigilOption::spin,
		SigilOption::new);

	/** A circle lying flat on the ground. */
	public static SigilOption flat(int style, int color, float size, int lifetime, float spin) {
		return new SigilOption(style, color, size, 0F, -90F, lifetime, spin);
	}

	@Override
	public ParticleType<SigilOption> getType() {
		return WildercordParticles.SIGIL;
	}
}
