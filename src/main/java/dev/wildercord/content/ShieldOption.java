package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * A Shield meeting a spell, sent at the shielded creature: {@link #APPEAR} (a spell flying at it is
 * close: its magic circle spawns in, in front of the spell), {@link #BLOCK} (the spell struck the
 * circle and stopped) or {@link #BREAK} (a stronger spell cracked the circle and shattered it like
 * glass, and went through). Each client builds the whole effect from these: the circle opening, its
 * ripples, or its cracks and falling shards.
 *
 * @param entity the shielded creature's id (0 if it's gone), so the circle can stay with it
 * @param dx     with {@code dy} and {@code dz}: from the creature's centre toward where the spell came from
 * @param offset how far out the circle stands from the creature's centre, along that direction
 * @param offset how far out the back circle stands from the creature's centre, along that direction
 * @param radius each circle's radius
 * @param runes  the spell that raised the Shield: every circle is its circle
 * @param layers how many circles are stacked, one behind another (a stronger Shield has more)
 * @param broken how many of them, from the front, the spell shatters (all of them when it breaks through)
 */
public record ShieldOption(int kind, int entity, int color, float dx, float dy, float dz, float offset, float radius, List<String> runes,
		int layers, int broken) implements ParticleOptions {
	public static final int BLOCK = 0;
	public static final int BREAK = 1;
	public static final int APPEAR = 2;

	public static final MapCodec<ShieldOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("kind").forGetter(ShieldOption::kind),
		Codec.INT.optionalFieldOf("entity", 0).forGetter(ShieldOption::entity),
		Codec.INT.optionalFieldOf("color", 0xFFE8C890).forGetter(ShieldOption::color),
		Codec.FLOAT.optionalFieldOf("dx", 0F).forGetter(ShieldOption::dx),
		Codec.FLOAT.optionalFieldOf("dy", 0F).forGetter(ShieldOption::dy),
		Codec.FLOAT.optionalFieldOf("dz", 1F).forGetter(ShieldOption::dz),
		Codec.FLOAT.optionalFieldOf("offset", 1F).forGetter(ShieldOption::offset),
		Codec.FLOAT.optionalFieldOf("radius", 1F).forGetter(ShieldOption::radius),
		Codec.STRING.listOf().optionalFieldOf("runes", List.of()).forGetter(ShieldOption::runes),
		Codec.INT.optionalFieldOf("layers", 1).forGetter(ShieldOption::layers),
		Codec.INT.optionalFieldOf("broken", 0).forGetter(ShieldOption::broken)
	).apply(i, ShieldOption::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ShieldOption> STREAM_CODEC = StreamCodec.of(
		(buf, o) -> {
			buf.writeVarInt(o.kind);
			buf.writeVarInt(o.entity);
			buf.writeInt(o.color);
			buf.writeFloat(o.dx);
			buf.writeFloat(o.dy);
			buf.writeFloat(o.dz);
			buf.writeFloat(o.offset);
			buf.writeFloat(o.radius);
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)).encode(buf, o.runes);
			buf.writeVarInt(o.layers);
			buf.writeVarInt(o.broken);
		},
		buf -> new ShieldOption(buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readFloat(), ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)).decode(buf), buf.readVarInt(), buf.readVarInt()));

	public ShieldOption {
		runes = List.copyOf(runes.size() > 16 ? runes.subList(0, 16) : runes);
		layers = Math.max(1, Math.min(MAX_LAYERS, layers));
		broken = Math.max(0, Math.min(layers, broken));
	}

	/** The most circles a Shield stacks. */
	public static final int MAX_LAYERS = 7;
	/** How far apart the stacked circles stand. */
	public static final float SPACING = 0.24F;

	@Override
	public ParticleType<ShieldOption> getType() {
		return WildercordParticles.SHIELD;
	}
}
