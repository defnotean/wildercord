package dev.wildercord.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Version seam for loot number providers, optional loot context params and advancement trigger player predicates (Minecraft 26.3).
 */
public final class McLootAndTriggers {
	private McLootAndTriggers() {}

	/** Constant loot roll provider for injected loot pools. */
	public static Holder<ContextIntProvider> exactPoolRolls(int count) {
		return ContextIntProviders.exactly(count);
	}

	/** Reads an optional parameter from {@link LootContext}. */
	public static <T> @Nullable T getOptionalParam(LootContext context, ContextKey<T> param) {
		return context.getOptional(param);
	}

	/** Version-neutral wrapper around the active MC {@code SimpleInstance#player()} predicate type. */
	public record TriggerPayload<D>(Optional<Holder<LootItemCondition>> rawPlayer, D data)
			implements SimpleCriterionTrigger.SimpleInstance {
		@Override
		public Optional<Holder<LootItemCondition>> player() {
			return rawPlayer;
		}
	}

	/** Builds a criterion codec pairing optional {@code "player"} predicate with {@code dataCodec}. */
	public static <D> Codec<TriggerPayload<D>> simpleTriggerCodec(MapCodec<D> dataCodec) {
		return RecordCodecBuilder.create(instance -> instance.group(
			LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerPayload::rawPlayer),
			dataCodec.forGetter(TriggerPayload::data)
		).apply(instance, TriggerPayload::new));
	}
}
