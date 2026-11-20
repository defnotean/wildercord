package dev.wildercord.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.compat.McLootAndTriggers;
import dev.wildercord.content.CordTier;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Fusions;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Wildercord's advancement criteria. Most look at the player's state rather than at an event
 * ("knows 50 runes", "has formed the 4th Circle", "has the feat Overcast"), so firing them again
 * at any time only grants what has been earned: that is how the tree catches up on login with
 * players who got there before it existed. {@link Advancements} fires them.
 *
 * <ul>
 *   <li>{@code wildercord:feat} {@code {"feat": "overcast"}}: the Grimoire holds that feat</li>
 *   <li>{@code wildercord:grimoire} {@code {"entry": "reaction:shatter"}}, {@code {"prefix": "secret:", "count": 1}}
 *       or {@code {"prefix": "secret:", "all": true}} (every entry with the prefix; no prefix: the whole Grimoire), or
 *       {@code {"signatures": 5}} (that many signature fusions found)</li>
 *   <li>{@code wildercord:heart_circle} {@code {"level": 4}}: has formed at least that many circles</li>
 *   <li>{@code wildercord:runes_known} {@code {"count": 50}} or {@code {"all": true}} (every rune you can find or make, and your own innate one;
 *       counted like the Heart Circles count them: Knots and runes of add-ons that aren't loaded don't count)</li>
 *   <li>{@code wildercord:cord} {@code {"tier": "copper"}}: wears that Cord or a better one</li>
 *   <li>{@code wildercord:moment} {@code {"moment": "glyph"}}: something just happened (see {@link Advancements})</li>
 * </ul>
 */
public final class WildercordTriggers {
	private WildercordTriggers() {}

	public static final FeatTrigger FEAT = register("feat", new FeatTrigger());
	public static final GrimoireTrigger GRIMOIRE = register("grimoire", new GrimoireTrigger());
	public static final HeartCircleTrigger HEART_CIRCLE = register("heart_circle", new HeartCircleTrigger());
	public static final RunesKnownTrigger RUNES_KNOWN = register("runes_known", new RunesKnownTrigger());
	public static final CordTrigger CORD = register("cord", new CordTrigger());
	public static final MomentTrigger MOMENT = register("moment", new MomentTrigger());

	/** Loads the class, registering the triggers (before any data pack is read). */
	public static void init() {}

	private static <T extends SimpleCriterionTrigger<?>> T register(String name, T trigger) {
		return Registry.register(BuiltInRegistries.TRIGGER_TYPES, Wildercord.id(name), trigger);
	}

	// ------------------------------------------------------------------ feats

	public static final class FeatTrigger extends SimpleCriterionTrigger<McLootAndTriggers.TriggerPayload<FeatTrigger.Data>> {
		@Override
		public Codec<McLootAndTriggers.TriggerPayload<Data>> codec() {
			return Data.CODEC;
		}

		public void trigger(ServerPlayer player, Collection<String> grimoire) {
			trigger(player, instance -> grimoire.contains("feat:" + instance.data().feat()));
		}

		public record Data(String feat) {
			public static final Codec<McLootAndTriggers.TriggerPayload<Data>> CODEC = McLootAndTriggers.simpleTriggerCodec(
				RecordCodecBuilder.mapCodec(i -> i.group(Codec.STRING.fieldOf("feat").forGetter(Data::feat)).apply(i, Data::new)));
		}
	}

	// ------------------------------------------------------------------ the Grimoire

	public static final class GrimoireTrigger extends SimpleCriterionTrigger<McLootAndTriggers.TriggerPayload<GrimoireTrigger.Data>> {
		@Override
		public Codec<McLootAndTriggers.TriggerPayload<Data>> codec() {
			return Data.CODEC;
		}

		public void trigger(ServerPlayer player, Collection<String> grimoire) {
			trigger(player, instance -> instance.data().matches(grimoire));
		}

		public record Data(Optional<String> entry, String prefix, int count, boolean all, int signatures) {
			public static final Codec<McLootAndTriggers.TriggerPayload<Data>> CODEC = McLootAndTriggers.simpleTriggerCodec(
				RecordCodecBuilder.mapCodec(i -> i.group(
					Codec.STRING.optionalFieldOf("entry").forGetter(Data::entry),
					Codec.STRING.optionalFieldOf("prefix", "").forGetter(Data::prefix),
					Codec.INT.optionalFieldOf("count", 1).forGetter(Data::count),
					Codec.BOOL.optionalFieldOf("all", false).forGetter(Data::all),
					Codec.INT.optionalFieldOf("signatures", 0).forGetter(Data::signatures)
				).apply(i, Data::new)));

			public boolean matches(Collection<String> grimoire) {
				if (entry.isPresent()) {
					return grimoire.contains(entry.get());
				}
				// Signature fusions share element fusions' "fusion:" keys, so they're counted by their own list.
				if (signatures > 0) {
					return Fusions.signaturesFound(grimoire) >= signatures;
				}
				if (all) {
					return Feats.complete(grimoire, prefix);
				}
				return Feats.count(grimoire, prefix) >= count;
			}
		}
	}

	// ------------------------------------------------------------------ Heart Circles

	public static final class HeartCircleTrigger extends SimpleCriterionTrigger<McLootAndTriggers.TriggerPayload<HeartCircleTrigger.Data>> {
		@Override
		public Codec<McLootAndTriggers.TriggerPayload<Data>> codec() {
			return Data.CODEC;
		}

		public void trigger(ServerPlayer player, int circles) {
			trigger(player, instance -> circles >= instance.data().level());
		}

		public record Data(int level) {
			public static final Codec<McLootAndTriggers.TriggerPayload<Data>> CODEC = McLootAndTriggers.simpleTriggerCodec(
				RecordCodecBuilder.mapCodec(i -> i.group(Codec.INT.fieldOf("level").forGetter(Data::level)).apply(i, Data::new)));
		}
	}

	// ------------------------------------------------------------------ runes known

	public static final class RunesKnownTrigger extends SimpleCriterionTrigger<McLootAndTriggers.TriggerPayload<RunesKnownTrigger.Data>> {
		@Override
		public Codec<McLootAndTriggers.TriggerPayload<Data>> codec() {
			return Data.CODEC;
		}

		/** @param innate the player's own innate rune's id ("" before it wakes) */
		public void trigger(ServerPlayer player, List<String> learned, String innate) {
			java.util.Set<String> known = new java.util.HashSet<>(learned);
			int count = Runes.countKnown(known);
			trigger(player, instance -> instance.data().all() ? knowsEveryRune(known, innate) : count >= instance.data().count());
		}

		/**
		 * Every rune you can find or make (add-ons' included, fused ones too), and your own innate rune:
		 * never the other innate runes (a caster only ever has their own) nor one with no way to get it.
		 */
		public static boolean knowsEveryRune(java.util.Set<String> learned, String innate) {
			for (RuneDef rune : Runes.all()) {
				boolean wanted = Runes.innate(rune) ? rune.id().equals(innate) : Runes.obtainable(rune);
				if (wanted && !learned.contains(rune.id())) {
					return false;
				}
			}
			return true;
		}

		public record Data(int count, boolean all) {
			public static final Codec<McLootAndTriggers.TriggerPayload<Data>> CODEC = McLootAndTriggers.simpleTriggerCodec(
				RecordCodecBuilder.mapCodec(i -> i.group(
					Codec.INT.optionalFieldOf("count", 1).forGetter(Data::count),
					Codec.BOOL.optionalFieldOf("all", false).forGetter(Data::all)
				).apply(i, Data::new)));
		}
	}

	// ------------------------------------------------------------------ Cords

	public static final class CordTrigger extends SimpleCriterionTrigger<McLootAndTriggers.TriggerPayload<CordTrigger.Data>> {
		/** A Cord tier by its key ({@code "twine"} ... {@code "echo"}); a typo fails the advancement's loading, loudly. */
		private static final Codec<CordTier> TIER = Codec.STRING.comapFlatMap(key -> {
			for (CordTier tier : CordTier.values()) {
				if (tier.key.equals(key)) {
					return DataResult.success(tier);
				}
			}
			return DataResult.error(() -> "Unknown Cord tier: " + key);
		}, tier -> tier.key);

		@Override
		public Codec<McLootAndTriggers.TriggerPayload<Data>> codec() {
			return Data.CODEC;
		}

		/** @param worn the worn Cord's tier, or null for none */
		public void trigger(ServerPlayer player, CordTier worn) {
			if (worn != null) {
				trigger(player, instance -> worn.ordinal() >= instance.data().tier().ordinal());
			}
		}

		public record Data(CordTier tier) {
			public static final Codec<McLootAndTriggers.TriggerPayload<Data>> CODEC = McLootAndTriggers.simpleTriggerCodec(
				RecordCodecBuilder.mapCodec(i -> i.group(TIER.fieldOf("tier").forGetter(Data::tier)).apply(i, Data::new)));
		}
	}

	// ------------------------------------------------------------------ moments

	public static final class MomentTrigger extends SimpleCriterionTrigger<McLootAndTriggers.TriggerPayload<MomentTrigger.Data>> {
		@Override
		public Codec<McLootAndTriggers.TriggerPayload<Data>> codec() {
			return Data.CODEC;
		}

		public void trigger(ServerPlayer player, String moment) {
			String id = moment.toLowerCase(Locale.ROOT);
			trigger(player, instance -> instance.data().moment().equals(id));
		}

		public record Data(String moment) {
			public static final Codec<McLootAndTriggers.TriggerPayload<Data>> CODEC = McLootAndTriggers.simpleTriggerCodec(
				RecordCodecBuilder.mapCodec(i -> i.group(Codec.STRING.fieldOf("moment").forGetter(Data::moment)).apply(i, Data::new)));
		}
	}
}
