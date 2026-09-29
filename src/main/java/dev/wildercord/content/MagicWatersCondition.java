package dev.wildercord.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.cast.Fishing;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * {@code wildercord:magic_waters}: passes as often as the magic at a fishing bobber allows (see {@link FishingRules}):
 * under a mana storm, on or near a ley line, or in a thunderstorm, in open water. Reads the fishing context's bobber
 * ({@code this}) and where it floats ({@code origin}); {@code multiplier} is the server's rune loot multiplier when the
 * table was loaded, as for every other rune Wildercord adds to vanilla's loot.
 */
public record MagicWatersCondition(double multiplier) implements LootItemCondition {
	public static final MapCodec<MagicWatersCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.0).forGetter(MagicWatersCondition::multiplier)
	).apply(i, MagicWatersCondition::new));

	@Override
	public MapCodec<MagicWatersCondition> codec() {
		return MAP_CODEC;
	}

	@Override
	public Set<ContextKey<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.ORIGIN, LootContextParams.THIS_ENTITY);
	}

	@Override
	public boolean test(LootContext context) {
		Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
		if (origin == null) {
			return false;
		}
		double chance = Fishing.magicWatersChance(context.getLevel(), origin, context.getOptional(LootContextParams.THIS_ENTITY), multiplier);
		return chance > 0 && context.getRandom().nextDouble() < chance;
	}
}
