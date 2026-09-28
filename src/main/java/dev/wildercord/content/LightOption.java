package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Shaped light for spells: a bright core with a soft coloured halo, drawn at full brightness.
 * What {@code a}, {@code b} and {@code c} mean depends on the kind:
 * <ul>
 *   <li>{@link #RING}: a ring that races out from radius {@code a} to {@code b} and fades (a
 *       shockwave), in the plane facing {@code yaw}/{@code pitch};</li>
 *   <li>{@link #RAY}: a beam from here to here + ({@code a}, {@code b}, {@code c}), always facing the
 *       viewer; it shoots out, holds and thins away;</li>
 *   <li>{@link #SLASH}: a crescent of radius {@code a} spanning {@code b} radians, centred on angle
 *       {@code roll} in the plane facing {@code yaw}/{@code pitch}; it sweeps across in {@code c}
 *       ticks, tapered at both ends, then fades as it widens;</li>
 *   <li>{@link #ORB}: a glowing orb of radius {@code a} wrapped in three turning rings;</li>
 *   <li>{@link #ARC}: a lightning arc from here to here + ({@code a}, {@code b}, {@code c}), jagged,
 *       with {@code yaw} forks branching off it, that crackles (a fresh path every tick) and gutters
 *       out; a {@code pitch} of 1 keeps it flat, jagging only sideways (skittering over water or
 *       ground), and {@code roll} scales how far it jags (0 for the usual).</li>
 * </ul>
 *
 * @param width    line width in blocks (the halo is wider)
 * @param lifetime ticks
 */
public record LightOption(int kind, int color, float a, float b, float c, float width, float yaw, float pitch, float roll, int lifetime)
		implements ParticleOptions {
	public static final int RING = 0;
	public static final int RAY = 1;
	public static final int SLASH = 2;
	public static final int ORB = 3;
	public static final int ARC = 4;

	public static final MapCodec<LightOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("kind").forGetter(LightOption::kind),
		Codec.INT.fieldOf("color").forGetter(LightOption::color),
		Codec.FLOAT.optionalFieldOf("a", 0F).forGetter(LightOption::a),
		Codec.FLOAT.optionalFieldOf("b", 1F).forGetter(LightOption::b),
		Codec.FLOAT.optionalFieldOf("c", 0F).forGetter(LightOption::c),
		Codec.FLOAT.optionalFieldOf("width", 0.1F).forGetter(LightOption::width),
		Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(LightOption::yaw),
		Codec.FLOAT.optionalFieldOf("pitch", -90F).forGetter(LightOption::pitch),
		Codec.FLOAT.optionalFieldOf("roll", 0F).forGetter(LightOption::roll),
		Codec.INT.optionalFieldOf("lifetime", 10).forGetter(LightOption::lifetime)
	).apply(i, LightOption::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, LightOption> STREAM_CODEC = StreamCodec.of(
		(buf, o) -> {
			buf.writeVarInt(o.kind);
			buf.writeInt(o.color);
			buf.writeFloat(o.a);
			buf.writeFloat(o.b);
			buf.writeFloat(o.c);
			buf.writeFloat(o.width);
			buf.writeFloat(o.yaw);
			buf.writeFloat(o.pitch);
			buf.writeFloat(o.roll);
			buf.writeVarInt(o.lifetime);
		},
		buf -> new LightOption(buf.readVarInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt()));

	@Override
	public ParticleType<LightOption> getType() {
		return WildercordParticles.LIGHT;
	}
}
