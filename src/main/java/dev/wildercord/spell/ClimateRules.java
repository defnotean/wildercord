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
 *
 * <p>Places and times of power ride on the same table: where two ley lines cross every element is a
 * little stronger (and every spell a little cheaper), and under the open sky the moon, the hour and the
 * weather favour some elements (a full moon arcane and void, a new moon blood, noon fire, dawn and dusk
 * time, rain frost). Those last are the <em>celestial</em> shifts: the server can switch them off or
 * scale them ({@link Tuning}), and the ley crossing's bonus too.</p>
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
	/** Where two ley lines cross: every element this much stronger, and every spell this much cheaper (places_of_power.crossing_bonus). */
	public static final double CROSSING_BONUS = 0.10;

	/** The hours of the sky's conditions, on the day clock (0 is sunrise's end, 6000 noon, 18000 midnight). */
	public static final long NOON_FROM = 4800;
	public static final long NOON_TO = 7200;
	public static final long DUSK_FROM = 11800;
	public static final long DUSK_TO = 13400;
	public static final long DAWN_FROM = 22800;
	public static final long DAWN_TO = 1200;
	/** The moon's phases as the game numbers them (its phase is the day number mod 8). */
	public static final int FULL_MOON_PHASE = 0;
	public static final int NEW_MOON_PHASE = 4;

	/** Something true of where a caster stands. Its id is its lang key's end ({@code climate.wildercord.<id>}) and what the server sends. */
	public enum Condition {
		NETHER("nether"), END("end"), THUNDER("thunder"), RAIN("rain"), SNOW("snow"), HEAT("heat"), NIGHT("night"), SUN("sun"), DEEP("deep"),
		LEY("ley"),
		/** Where two ley lines cross: every element stronger, every spell cheaper. */
		LEY_CROSSING("ley_crossing"),
		/** The celestial ones: under a clear, open sky, at their hours. */
		FULL_MOON("full_moon"), NEW_MOON("new_moon"), NOON("noon"), DAWN("dawn"), DUSK("dusk");

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

	/**
	 * One condition's effect on one element: its hits times {@code factor}. A celestial shift (the moon, the
	 * hour, rain's chill) is scaled by the server's {@link Tuning#celestial}.
	 */
	public record Shift(Condition when, String element, double factor, boolean celestial) {
		public Shift(Condition when, String element, double factor) {
			this(when, element, factor, false);
		}
	}

	/**
	 * The server's say on places and times of power: the ley crossing's bonus ({@link #CROSSING_BONUS} as
	 * designed) and how strong the celestial shifts are (1 as designed, 0 for none).
	 */
	public record Tuning(double crossing, double celestial) {
		public static final Tuning DEFAULT = new Tuning(CROSSING_BONUS, 1.0);
		public static final Tuning NONE = new Tuning(0, 0);

		/** What a spell costs where two ley lines cross, as a factor on its price. */
		public double crossingCost() {
			return Math.max(0.5, 1.0 - crossing);
		}
	}

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
		new Shift(Condition.LEY, "arcane", 1.15),
		// The sky's: rain's chill, the moon, the hour.
		new Shift(Condition.RAIN, "frost", 1.10, true),
		new Shift(Condition.FULL_MOON, "arcane", 1.15, true),
		new Shift(Condition.FULL_MOON, "void", 1.15, true),
		new Shift(Condition.NEW_MOON, "blood", 1.15, true),
		new Shift(Condition.NEW_MOON, "void", 1.10, true),
		new Shift(Condition.NOON, "fire", 1.15, true),
		new Shift(Condition.DAWN, "time", 1.15, true),
		new Shift(Condition.DUSK, "time", 1.15, true));

	/** How hard {@code element} hits where these conditions hold (1 = as usual), as designed. */
	public static double factor(Collection<Condition> here, String element) {
		return factor(here, element, Tuning.DEFAULT);
	}

	/** How hard {@code element} hits where these conditions hold, with the server's tuning of the places and times of power. */
	public static double factor(Collection<Condition> here, String element, Tuning tuning) {
		if (element == null || element.isEmpty()) {
			return 1.0;
		}
		double factor = 1.0;
		for (Shift shift : SHIFTS) {
			if (shift.element().equals(element) && here.contains(shift.when())) {
				factor *= shifted(shift, tuning);
			}
		}
		if (here.contains(Condition.LEY_CROSSING)) {
			factor *= 1.0 + tuning.crossing();
		}
		return Math.max(MIN, Math.min(MAX, factor));
	}

	private static double shifted(Shift shift, Tuning tuning) {
		return shift.celestial() ? 1.0 + (shift.factor() - 1.0) * tuning.celestial() : shift.factor();
	}

	/** Every element these conditions change, favoured or hindered, in {@link Affinity#ELEMENTS} order. */
	public static Map<String, Double> factors(Collection<Condition> here) {
		return factors(here, Tuning.DEFAULT);
	}

	public static Map<String, Double> factors(Collection<Condition> here, Tuning tuning) {
		Map<String, Double> factors = new LinkedHashMap<>();
		for (String element : Affinity.ELEMENTS) {
			double factor = factor(here, element, tuning);
			if (Math.abs(factor - 1.0) > 1.0E-6) {
				factors.put(element, factor);
			}
		}
		return factors;
	}

	/** A condition's shifts as the Grimoire writes them, e.g. "fire +20%, frost -25%". */
	public static String describe(Condition condition, java.util.function.Function<String, String> elementName) {
		return describe(condition, elementName, Tuning.DEFAULT);
	}

	/**
	 * A condition's shifts as the HUD and Grimoire write them, with the server's tuning. A ley crossing's
	 * reads "all +10%", its "all" passed through {@code elementName} like an element's name.
	 */
	public static String describe(Condition condition, java.util.function.Function<String, String> elementName, Tuning tuning) {
		if (condition == Condition.LEY_CROSSING) {
			long percent = Math.round(tuning.crossing() * 100);
			return percent == 0 ? "" : elementName.apply("all") + " +" + percent + "%";
		}
		StringBuilder text = new StringBuilder();
		for (Shift shift : SHIFTS) {
			if (shift.when() == condition) {
				long percent = Math.round((shifted(shift, tuning) - 1.0) * 100);
				if (percent == 0) {
					continue;
				}
				text.append(text.isEmpty() ? "" : ", ").append(elementName.apply(shift.element())).append(percent > 0 ? " +" : " ").append(percent).append('%');
			}
		}
		return text.toString();
	}

	/** Whether a condition is one of the sky's own (the moon and the hours): their shifts scale with {@link Tuning#celestial}. */
	public static boolean celestial(Condition condition) {
		return switch (condition) {
			case FULL_MOON, NEW_MOON, NOON, DAWN, DUSK -> true;
			default -> false;
		};
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
	 * @param crossing    whether they stand where two ley lines cross (and the server counts crossings)
	 * @param moonPhase   the moon's phase, 0 (full) to 7, or -1 where there's no moon to speak of
	 * @param celestial   whether the sky's conditions count (the moon, the hours): the server's switch
	 */
	public record Surroundings(String dimension, boolean dayCycle, long timeOfDay, boolean openSky, boolean raining, boolean thundering,
			boolean rainOnThem, boolean cold, float temperature, boolean dry, int y, boolean ley, boolean crossing, int moonPhase, boolean celestial) {
		/** Surroundings as 0.7.1 knew them: no ley crossings, and the sky's hours and moon left out. */
		public Surroundings(String dimension, boolean dayCycle, long timeOfDay, boolean openSky, boolean raining, boolean thundering,
				boolean rainOnThem, boolean cold, float temperature, boolean dry, int y, boolean ley) {
			this(dimension, dayCycle, timeOfDay, openSky, raining, thundering, rainOnThem, cold, temperature, dry, y, ley, false, -1, false);
		}

		public boolean night() {
			return timeOfDay >= 13000 && timeOfDay < 23000;
		}
	}

	/** The moon's phase on a day clock counted from the world's first morning: it changes each day, through eight. */
	public static int moonPhase(long dayClock) {
		return (int) Math.floorMod(Math.floorDiv(dayClock, 24000L), 8L);
	}

	/** Which conditions hold for a caster standing there. */
	public static Set<Condition> conditions(Surroundings s) {
		EnumSet<Condition> here = EnumSet.noneOf(Condition.class);
		if (s.ley()) {
			here.add(Condition.LEY);
		}
		if (s.crossing()) {
			here.add(Condition.LEY_CROSSING);
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
		// The sky's hours and the moon: only under it, and only where there are days and nights.
		if (s.celestial() && s.dayCycle() && s.openSky()) {
			long t = s.timeOfDay();
			if (s.night() && !s.raining()) {
				if (s.moonPhase() == FULL_MOON_PHASE) {
					here.add(Condition.FULL_MOON);
				} else if (s.moonPhase() == NEW_MOON_PHASE) {
					here.add(Condition.NEW_MOON);
				}
			}
			if (!s.raining() && t >= NOON_FROM && t < NOON_TO) {
				here.add(Condition.NOON);
			}
			if (t >= DAWN_FROM || t < DAWN_TO) {
				here.add(Condition.DAWN);
			}
			if (t >= DUSK_FROM && t < DUSK_TO) {
				here.add(Condition.DUSK);
			}
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
