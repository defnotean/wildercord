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
 * A spell's whole magic circle, written out from its runes (see {@link dev.wildercord.spell.SpellSigil}):
 * one particle, built by each client, that opens in stages and fades at the end of its life.
 *
 * @param runes    the spell's rune ids, in order
 * @param color    0xRRGGBB for the frame, script and star (each rune's roundel is in its own colour)
 * @param radius   in blocks
 * @param yaw      the way the circle faces (see {@link SigilOption})
 * @param pitch    ... and its pitch (-90 lies flat on the ground)
 * @param lifetime ticks
 * @param rank     the spell's mastery rank (see {@code spell.MasteryRules}), 0 for a circle with none: richer as it rises
 * @param sigil    its owner's sigil's seed (see {@code spell.MasterySigil}), drawn at its centre; 0 for none
 * @param flags    the looks its traits give it (see {@code player.MasteryAttachments.Look})
 */
public record SpellCircleOption(List<String> runes, int color, float radius, float yaw, float pitch, int lifetime, int rank, long sigil, int flags)
		implements ParticleOptions {
	/** A circle with no mastery: a monster's, a scroll's, a glyph's. */
	public SpellCircleOption(List<String> runes, int color, float radius, float yaw, float pitch, int lifetime) {
		this(runes, color, radius, yaw, pitch, lifetime, 0, 0L, 0);
	}

	public static final MapCodec<SpellCircleOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.STRING.listOf().fieldOf("runes").forGetter(SpellCircleOption::runes),
		Codec.INT.optionalFieldOf("color", 0xE8C46A).forGetter(SpellCircleOption::color),
		Codec.FLOAT.optionalFieldOf("radius", 1F).forGetter(SpellCircleOption::radius),
		Codec.FLOAT.optionalFieldOf("yaw", 0F).forGetter(SpellCircleOption::yaw),
		Codec.FLOAT.optionalFieldOf("pitch", -90F).forGetter(SpellCircleOption::pitch),
		Codec.INT.optionalFieldOf("lifetime", 30).forGetter(SpellCircleOption::lifetime),
		Codec.INT.optionalFieldOf("rank", 0).forGetter(SpellCircleOption::rank),
		Codec.LONG.optionalFieldOf("sigil", 0L).forGetter(SpellCircleOption::sigil),
		Codec.INT.optionalFieldOf("flags", 0).forGetter(SpellCircleOption::flags)
	).apply(i, SpellCircleOption::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, SpellCircleOption> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), SpellCircleOption::runes,
		ByteBufCodecs.INT, SpellCircleOption::color,
		ByteBufCodecs.FLOAT, SpellCircleOption::radius,
		ByteBufCodecs.FLOAT, SpellCircleOption::yaw,
		ByteBufCodecs.FLOAT, SpellCircleOption::pitch,
		ByteBufCodecs.VAR_INT, SpellCircleOption::lifetime,
		ByteBufCodecs.VAR_INT, SpellCircleOption::rank,
		ByteBufCodecs.VAR_LONG, SpellCircleOption::sigil,
		ByteBufCodecs.VAR_INT, SpellCircleOption::flags,
		SpellCircleOption::new);

	@Override
	public ParticleType<SpellCircleOption> getType() {
		return WildercordParticles.SPELL_CIRCLE;
	}
}
