package dev.wildercord.cast;

import dev.wildercord.config.Config;
import dev.wildercord.spell.ClimateRules;
import dev.wildercord.spell.ClimateRules.Condition;
import dev.wildercord.world.LeyLines;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.Set;

/**
 * Places and times of power, asked about from anywhere: whether a spot is a place of power (a ley crossing),
 * and what's empowered there right now and why (the moon, the hour, the weather, the crossing, the rest of the
 * elemental climate). The hook for other features: a resonance that only wakes at a place of power, a rite
 * that wants a full moon. The numbers are the climate's own ({@link ClimateRules}, tuned by the server's
 * {@code places_of_power} section), so whatever asks here agrees with what a spell feels.
 */
public final class PowerPlaces {
	private PowerPlaces() {}

	/**
	 * What's empowered at a spot right now: the conditions holding there and each element's factor (only those
	 * changed; 1.2 is 20% stronger).
	 */
	public record Power(Set<Condition> conditions, Map<String, Double> factors) {
		/** Whether it's a place of power: where two ley lines cross. */
		public boolean placeOfPower() {
			return conditions.contains(Condition.LEY_CROSSING);
		}

		/** How hard {@code element} hits here (1 as usual). */
		public double factor(String element) {
			return factors.getOrDefault(element, 1.0);
		}

		/** Whether something about the sky favours an element here now (the moon, the hour). */
		public boolean celestial() {
			return conditions.stream().anyMatch(ClimateRules::celestial);
		}
	}

	/** Whether {@code pos} is a place of power: a ley crossing (they run in the Overworld only), and the server counts them. */
	public static boolean isPlaceOfPower(ServerLevel level, BlockPos pos) {
		return level.dimension() == Level.OVERWORLD && Config.get().elementalClimate() && Config.get().power().leyCrossings()
			&& LeyLines.atCrossing(LeyWalker.seed(level), pos.getX() + 0.5, pos.getZ() + 0.5);
	}

	/** What's empowered at {@code pos} right now, and why (as if a caster stood there). */
	public static Power at(ServerLevel level, BlockPos pos) {
		if (!Config.get().elementalClimate()) {
			return new Power(Set.of(), Map.of());
		}
		boolean ley = level.dimension() == Level.OVERWORLD && LeyLines.strength(LeyWalker.seed(level), pos.getX() + 0.5, pos.getZ() + 0.5) >= LeyWalker.ON_LINE;
		Set<Condition> here = Set.copyOf(ClimateRules.conditions(Climate.surroundings(level, pos, pos.above(), ley, isPlaceOfPower(level, pos))));
		return new Power(here, ClimateRules.factors(here, Climate.tuning()));
	}

	/** What's empowered where a player stands right now (the same the HUD shows them). */
	public static Power of(ServerPlayer player) {
		if (!Config.get().elementalClimate()) {
			return new Power(Set.of(), Map.of());
		}
		Set<Condition> here = Climate.conditions(player);
		return new Power(here, ClimateRules.factors(here, Climate.tuning()));
	}
}
