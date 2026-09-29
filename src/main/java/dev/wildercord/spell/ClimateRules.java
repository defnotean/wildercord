package dev.wildercord.spell;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Elemental climate: where a caster stands nudges how hard each element hits (fire burns hotter in
 * the Nether, storm crackles in a thunderstorm, frost bites harder in the snow). Modest on purpose,
 * 10 to 25%, and shown on the HUD so it can be learned. Pure: {@code cast.Climate} works out which
 * {@link Condition}s hold for a player from the world, and the client draws the same table from the
 * conditions the server sends it.
 */
public final class ClimateRules {
	private ClimateRules() {}

	/** Base temperature from which a biome that never rains counts as hot and dry (desert, badlands, savanna: 2.0). */
	public static final float HOT = 1.5F;
	/** Below this height, in the Overworld, a caster is deep underground. */
	public static final int DEEP_BELOW = 0;
	/** However conditions stack, an element's climate stays within these. */
	public static final double MIN = 0.5;
	public static final double MAX = 1.5;

	/** Something true of where a caster stands. Its id is its lang key's end ({@code climate.wildercord.<id>}) and what the server sends. */
	public enum Condition {
		NETHER("nether"), END("end"), THUNDER("thunder"), RAIN("rain"), SNOW("snow"), HEAT("heat"), NIGHT("night"), SUN("sun"), DEEP("deep"),
		LEY("ley");

		public final String id;

		Condition(String id) {
			this.id = id;
		}

		public static Optional<Condition> byId(String id) {
			for (Condition condition : values()) {
				if (condition.id.equals(id)) {
					return Optional.of(condition);
				}
			}
			return Optional.empty();
		}
	}

	/** One condition's effect on one element: its hits times {@code factor}. */
	public record Shift(Condition when, String element, double factor) {}

	public static final List<Shift> SHIFTS = List.of(
		new Shift(Condition.NETHER, "fire", 1.20),
		new Shift(Condition.NETHER, "frost", 0.75),
		new Shift(Condition.END, "void", 1.20),
		new Shift(Condition.THUNDER, "storm", 1.25),
		new Shift(Condition.RAIN, "fire", 0.90),
		new Shift(Condition.SNOW, "frost", 1.20),
		new Shift(Condition.SNOW, "fire", 0.90),
		new Shift(Condition.HEAT, "fire", 1.15),
		new Shift(Condition.HEAT, "frost", 0.90),
		new Shift(Condition.NIGHT, "void", 1.10),
		new Shift(Condition.SUN, "life", 1.10),
		new Shift(Condition.DEEP, "earth", 1.15),
		new Shift(Condition.LEY, "arcane", 1.15));

	/** How hard {@code element} hits where these conditions hold (1 = as usual). */
	public static double factor(Collection<Condition> here, String element) {
		double factor = 1.0;
		for (Shift shift : SHIFTS) {
			if (shift.element().equals(element) && here.contains(shift.when())) {
				factor *= shift.factor();
			}
		}
		return Math.max(MIN, Math.min(MAX, factor));
	}

	/** Every element these conditions change, favoured or hindered, in {@link Affinity#ELEMENTS} order. */
	public static Map<String, Double> factors(Collection<Condition> here) {
		Map<String, Double> factors = new LinkedHashMap<>();
		for (String element : Affinity.ELEMENTS) {
			double factor = factor(here, element);
			if (Math.abs(factor - 1.0) > 1.0E-6) {
				factors.put(element, factor);
			}
		}
		return factors;
	}

	/** A condition's shifts as the Grimoire writes them, e.g. "fire +20%, frost -25%". */
	public static String describe(Condition condition, java.util.function.Function<String, String> elementName) {
		StringBuilder text = new StringBuilder();
		for (Shift shift : SHIFTS) {
			if (shift.when() == condition) {
				long percent = Math.round((shift.factor() - 1.0) * 100);
				text.append(text.isEmpty() ? "" : ", ").append(elementName.apply(shift.element())).append(percent > 0 ? " +" : " ").append(percent).append('%');
			}
		}
		return text.toString();
	}

	/**
	 * What the server knows about where a caster stands.
	 *
	 * @param dimension   the dimension's id, e.g. {@code minecraft:the_nether}
	 * @param dayCycle    whether the dimension has days and nights (the Nether and the End don't)
	 * @param timeOfDay   the day clock, 0-23999 (6000 is noon, 18000 midnight)
	 * @param openSky     whether the caster can see the sky
	 * @param raining     whether it's raining (or snowing) anywhere in the dimension
	 * @param thundering  whether there's a thunderstorm
	 * @param rainOnThem  whether rain (not snow) is falling on the caster
	 * @param cold        whether it's cold enough to snow where they stand (snowy and frozen biomes, high peaks)
	 * @param temperature the biome's base temperature
	 * @param dry         whether the biome never rains
	 * @param y           the caster's height
	 * @param ley         whether they stand on a ley line or under a mana storm
	 */
	public record Surroundings(String dimension, boolean dayCycle, long timeOfDay, boolean openSky, boolean raining, boolean thundering,
			boolean rainOnThem, boolean cold, float temperature, boolean dry, int y, boolean ley) {
		public boolean night() {
			return timeOfDay >= 13000 && timeOfDay < 23000;
		}
	}

	/** Which conditions hold for a caster standing there. */
	public static Set<Condition> conditions(Surroundings s) {
		EnumSet<Condition> here = EnumSet.noneOf(Condition.class);
		if (s.ley()) {
			here.add(Condition.LEY);
		}
		if (s.dimension().equals("minecraft:the_nether")) {
			here.add(Condition.NETHER);
			return here;
		}
		if (s.dimension().equals("minecraft:the_end")) {
			here.add(Condition.END);
			return here;
		}
		// The Overworld (and any other dimension with weather and biomes of its own).
		if (s.thundering() && s.openSky()) {
			here.add(Condition.THUNDER);
		}
		if (s.rainOnThem()) {
			here.add(Condition.RAIN);
		}
		if (s.cold()) {
			here.add(Condition.SNOW);
		} else if (s.temperature() >= HOT && s.dry()) {
			here.add(Condition.HEAT);
		}
		if (s.dayCycle() && s.openSky()) {
			if (s.night()) {
				here.add(Condition.NIGHT);
			} else if (!s.raining()) {
				here.add(Condition.SUN);
			}
		}
		if (s.dimension().equals("minecraft:overworld") && s.y() < DEEP_BELOW) {
			here.add(Condition.DEEP);
		}
		return here;
	}

	/** The ids the server sends for a set of conditions, in a fixed order. */
	public static List<String> ids(Collection<Condition> here) {
		EnumSet<Condition> sorted = EnumSet.noneOf(Condition.class);
		sorted.addAll(here);
		return sorted.stream().map(c -> c.id).toList();
	}

	/** The conditions from the ids a client was sent; ids it doesn't know (from a newer server) are left out. */
	public static Set<Condition> fromIds(Collection<String> ids) {
		EnumSet<Condition> here = EnumSet.noneOf(Condition.class);
		for (String id : ids) {
			Condition.byId(id).ifPresent(here::add);
		}
		return here;
	}
}
