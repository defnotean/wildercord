package dev.wildercord.world.biome;

import com.mojang.datafixers.util.Pair;
import dev.wildercord.Wildercord;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.function.Consumer;

/**
 * The high-weirdness band of dark forests becomes Astral Ley-Glade: a luminous indigo-teal
 * woodland pierced by natural ley-crystal spires and leybloom clusters.
 */
public final class AstralLeyGladeClimate {
	private AstralLeyGladeClimate() {}

	public static final ResourceKey<Biome> ASTRAL_LEY_GLADE = ResourceKey.create(Registries.BIOME, Wildercord.id("astral_ley_glade"));
	public static final String JOURNAL_ENTRY = "place:astral_ley_glade";
	/** Quantized weirdness threshold keeping standard dark forests below +0.35 intact. */
	static final long MIN_WEIRDNESS = 3500;

	public static void emit(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry,
			Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> output) {
		var point = entry.getFirst();
		if (!entry.getSecond().equals(Biomes.DARK_FOREST) || point.weirdness().max() < MIN_WEIRDNESS) {
			output.accept(entry);
			return;
		}
		if (point.weirdness().min() < MIN_WEIRDNESS) {
			output.accept(Pair.of(withWeirdness(point, point.weirdness().min(), MIN_WEIRDNESS - 1), Biomes.DARK_FOREST));
		}
		output.accept(Pair.of(withWeirdness(point, Math.max(MIN_WEIRDNESS, point.weirdness().min()), point.weirdness().max()), ASTRAL_LEY_GLADE));
	}

	private static Climate.ParameterPoint withWeirdness(Climate.ParameterPoint point, long min, long max) {
		return new Climate.ParameterPoint(point.temperature(), point.humidity(), point.continentalness(),
			point.erosion(), point.depth(), new Climate.Parameter(min, max), point.offset());
	}
}
