package dev.wildercord.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.Optional;

/**
 * Version seam for loot number providers, optional loot context params and advancement trigger player predicates.
 */
public final class McLootAndTriggers {
	private McLootAndTriggers() {}

	/** Constant loot roll provider for injected loot pools. */
	public static NumberProvider exactPoolRolls(int count) {
		return ConstantValue.exactly(count);
	}

	/** Reads an optional parameter from {@link LootContext}. */
	public static <T>  T getOptionalParam(LootContext context, LootContextParam<T> param) {
		return context.getParamOrNull(param);
	}

	/** Version-neutral wrapper around the active MC {@code SimpleInstance#player()} predicate type. */
	public record TriggerPayload<D>(Optional<ContextAwarePredicate> rawPlayer, D data)
			implements SimpleCriterionTrigger.SimpleInstance {
		@Override
		public Optional<ContextAwarePredicate> player() {
			return rawPlayer;
		}
	}

	/** Builds a criterion codec pairing optional {@code "player"} predicate with {@code dataCodec}. */
	public static <D> Codec<TriggerPayload<D>> simpleTriggerCodec(MapCodec<D> dataCodec) {
		return RecordCodecBuilder.create(instance -> instance.group(
			ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerPayload::rawPlayer),
			dataCodec.forGetter(TriggerPayload::data)
		).apply(instance, TriggerPayload::new));
	}
}
