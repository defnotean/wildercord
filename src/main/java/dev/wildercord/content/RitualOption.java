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
 * An attunement as it goes, sent at the meditating player's feet every few ticks: each client keeps
 * one ritual per player going from these (the land's magic circle turning under them, motes of its
 * colour spiralling up into the Blank Rune), brighter as {@code progress} climbs, and lets it fade
 * when they stop coming. A {@code progress} of 1 is the finish.
 *
 * @param entity   the player's id
 * @param rune     the rune the land gives
 * @param color    0xRRGGBB, the rune's
 * @param progress 0 to 1
 */
public record RitualOption(int entity, String rune, int color, float progress) implements ParticleOptions {
	public static final MapCodec<RitualOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.INT.fieldOf("entity").forGetter(RitualOption::entity),
		Codec.STRING.fieldOf("rune").forGetter(RitualOption::rune),
		Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(RitualOption::color),
		Codec.FLOAT.optionalFieldOf("progress", 0F).forGetter(RitualOption::progress)
	).apply(i, RitualOption::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, RitualOption> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RitualOption::entity,
		ByteBufCodecs.STRING_UTF8, RitualOption::rune,
		ByteBufCodecs.INT, RitualOption::color,
		ByteBufCodecs.FLOAT, RitualOption::progress,
		RitualOption::new);

	@Override
	public ParticleType<RitualOption> getType() {
		return WildercordParticles.RITUAL;
	}
}
