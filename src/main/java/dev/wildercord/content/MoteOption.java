package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A small soft light or a wisp of vapour, built on each client. What {@code dx}, {@code dy},
 * {@code dz} and {@code extra} mean depends on the kind:
 * <ul>
 *   <li>{@link #GLOW}: a glowing speck that drifts ({@code dx}, {@code dy}, {@code dz}) a tick, wanders
 *       by {@code extra} and twinkles;</li>
 *   <li>{@link #SEEK}: a glowing speck that flies from here to here + ({@code dx}, {@code dy},
 *       {@code dz}) over its life, spiralling {@code extra} turns round the way, with a short tail;</li>
 *   <li>{@link #BUTTERFLY}: a butterfly of light, its wings beating, fluttering along ({@code dx},
 *       {@code dy}, {@code dz}) a tick; {@code extra} is how far it strays from that line;</li>
 *   <li>{@link #CLOUD}: a soft billow of steam or smoke that swells, rises along ({@code dx},
 *       {@code dy}, {@code dz}) a tick (slowing as it goes), lingers and thins away; {@code extra} is
 *       how thick it is at its thickest (0 to 1).</li>
 * </ul>
 * Lights are drawn at full brightness and add to what's behind them; a cloud is lit like the world.
 *
 * @param color    0xRRGGBB
 * @param size     across, in blocks (a butterfly's wingspan; a cloud's once it has swelled)
 * @param lifetime ticks
 */
public record MoteOption(int kind, int color, float size, int lifetime, float dx, float dy, float dz, float extra) implements ParticleOptions {
	public static final int GLOW = 0;
	public static final int SEEK = 1;
	public static final int BUTTERFLY = 2;
	public static final int CLOUD = 3;

	public static final MapCodec<MoteOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("kind").forGetter(MoteOption::kind),
		Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(MoteOption::color),
		Codec.FLOAT.optionalFieldOf("size", 0.15F).forGetter(MoteOption::size),
		Codec.INT.optionalFieldOf("lifetime", 20).forGetter(MoteOption::lifetime),
		Codec.FLOAT.optionalFieldOf("dx", 0F).forGetter(MoteOption::dx),
		Codec.FLOAT.optionalFieldOf("dy", 0F).forGetter(MoteOption::dy),
		Codec.FLOAT.optionalFieldOf("dz", 0F).forGetter(MoteOption::dz),
		Codec.FLOAT.optionalFieldOf("extra", 0F).forGetter(MoteOption::extra)
	).apply(i, MoteOption::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, MoteOption> STREAM_CODEC = StreamCodec.of(
		(buf, o) -> {
			buf.writeVarInt(o.kind);
			buf.writeInt(o.color);
			buf.writeFloat(o.size);
			buf.writeVarInt(o.lifetime);
			buf.writeFloat(o.dx);
			buf.writeFloat(o.dy);
			buf.writeFloat(o.dz);
			buf.writeFloat(o.extra);
		},
		buf -> new MoteOption(buf.readVarInt(), buf.readInt(), buf.readFloat(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readFloat()));

	@Override
	public ParticleType<MoteOption> getType() {
		return WildercordParticles.MOTE;
	}
}
