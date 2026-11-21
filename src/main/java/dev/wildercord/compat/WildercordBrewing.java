package dev.wildercord.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;

/** Registers Wildercord brewing mixes onto vanilla {@link PotionBrewing} for 26.2 (no JSON brewing recipes). */
public final class WildercordBrewing {
	private WildercordBrewing() {}

	public static void register(PotionBrewing.Builder builder) {
		// Mana, clarity, and warding potions
		mix(builder, "minecraft:awkward", "minecraft:amethyst_shard", "wildercord:clarity");
		mix(builder, "wildercord:clarity", "minecraft:redstone", "wildercord:long_clarity");
		mix(builder, "wildercord:clarity", "minecraft:glowstone_dust", "wildercord:strong_clarity");
		mix(builder, "minecraft:awkward", "minecraft:lapis_lazuli", "wildercord:mana");
		mix(builder, "wildercord:mana", "minecraft:glowstone_dust", "wildercord:strong_mana");
		mix(builder, "minecraft:awkward", "minecraft:tinted_glass", "wildercord:warded");
		mix(builder, "wildercord:warded", "minecraft:redstone", "wildercord:long_warded");
		mix(builder, "wildercord:warded", "minecraft:glowstone_dust", "wildercord:strong_warded");

		// Mana elixirs
		mix(builder, "wildercord:clarity", "wildercord:bottled_gale", "wildercord:torrent");
		mix(builder, "wildercord:torrent", "minecraft:redstone", "wildercord:long_torrent");
		mix(builder, "wildercord:torrent", "minecraft:glowstone_dust", "wildercord:strong_torrent");
		mix(builder, "minecraft:awkward", "wildercord:geode_grit", "wildercord:deep_well");
		mix(builder, "wildercord:deep_well", "minecraft:redstone", "wildercord:long_deep_well");
		mix(builder, "wildercord:deep_well", "minecraft:glowstone_dust", "wildercord:strong_deep_well");
		mix(builder, "minecraft:awkward", "wildercord:hourglass_sand", "wildercord:condensing");
		mix(builder, "wildercord:condensing", "minecraft:redstone", "wildercord:long_condensing");
		mix(builder, "wildercord:condensing", "minecraft:glowstone_dust", "wildercord:strong_condensing");

		// Monster drops
		mix(builder, "minecraft:awkward", "wildercord:shadow_pelt", "minecraft:invisibility");
		mix(builder, "minecraft:awkward", "wildercord:bog_gland", "minecraft:water_breathing");
		mix(builder, "minecraft:thick", "wildercord:bog_gland", "minecraft:leaping");

		// Wildlife drops
		mix(builder, "minecraft:awkward", "wildercord:glimmer_dust", "minecraft:night_vision");
		mix(builder, "minecraft:awkward", "wildercord:mossback_scute", "minecraft:turtle_master");
		mix(builder, "minecraft:awkward", "wildercord:ember_tuft", "minecraft:fire_resistance");
		mix(builder, "minecraft:awkward", "wildercord:skyray_membrane", "minecraft:slow_falling");
	}

	private static void mix(PotionBrewing.Builder builder, String inId, String reagentId, String outId) {
		var inPotion = potion(inId);
		var reagent = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(reagentId)).orElse(null);
		var outPotion = potion(outId);
		if (inPotion != null && reagent != null && outPotion != null) {
			builder.addMix(inPotion, reagent, outPotion);
		}
	}

	private static Holder<Potion> potion(String id) {
		return BuiltInRegistries.POTION.get(ResourceKey.create(Registries.POTION, ResourceLocation.parse(id))).orElse(null);
	}
}
