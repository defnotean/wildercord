package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Original elemental sprites with distinct motion, rather than tinted vanilla particles. */
public record MaterialOption(int style, int color, float size, int lifetime) implements ParticleOptions {
	public static final int EMBER=0, FROST=1, STORM=2, WIND=3, STONE=4, PETAL=5,
		VOID=6, ARCANE=7, TIME=8, BLOOD=9, WATER=10, VAPOUR=11;
	public MaterialOption {
		if (style < 0 || style > VAPOUR || !Float.isFinite(size) || size < .02F || size > .8F
			|| lifetime < 2 || lifetime > 80) throw new IllegalArgumentException("Invalid spell material");
		color &= 0xFFFFFF;
	}
	public static final MapCodec<MaterialOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.intRange(0, VAPOUR).fieldOf("style").forGetter(MaterialOption::style),
		Codec.INT.fieldOf("color").forGetter(MaterialOption::color),
		Codec.floatRange(.02F, .8F).fieldOf("size").forGetter(MaterialOption::size),
		Codec.intRange(2, 80).fieldOf("lifetime").forGetter(MaterialOption::lifetime)
	).apply(i, MaterialOption::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, MaterialOption> STREAM_CODEC = StreamCodec.of(
		(b,o) -> { b.writeVarInt(o.style); b.writeInt(o.color); b.writeFloat(o.size); b.writeVarInt(o.lifetime); },
		b -> new MaterialOption(b.readVarInt(), b.readInt(), b.readFloat(), b.readVarInt()));
	@Override public ParticleType<MaterialOption> getType() { return WildercordParticles.MATERIAL; }
}
