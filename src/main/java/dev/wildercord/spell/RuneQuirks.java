package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * A world's rune quirks: a handful of small tweaks to particular runes, true in this world only ("here, Shock cracks
 * harder in the rain"). They're drawn from the world's seed alongside its resonances (a stream of their own, so the
 * number of resonances never changes them), nobody is told of them, and each is written into a caster's Grimoire the
 * first time it matters to one of their spells.
 *
 * <p>Pure data and rules: the server tells a quirk where the effect lands ({@link Attunements.Place}) and applies its
 * numbers (see {@code cast.WorldQuirks}).</p>
 */
public final class RuneQuirks {
	private RuneQuirks() {}

	public static final int DEFAULT_COUNT = 4;
	public static final int MAX_COUNT = 8;
	public static final String KEY_PREFIX = "quirk:";

	/** A stronger quirk's power, a longer one's duration, and a quirk's faint second strike, as multiples. */
	public static final double POWER = 1.2;
	public static final double DURATION = 1.35;
	public static final double ECHO_POWER = 0.3;
	/** How long after the first a quirk's faint second strike lands. */
	public static final int ECHO_TICKS = 10;

	/** What a quirk does to its rune while its condition holds. */
	public enum Kind {
		/** {@link #POWER} times the power. */
		STRONGER,
		/** {@link #DURATION} times the duration. */
		LONGER,
		/** Strikes again {@link #ECHO_TICKS} later at {@link #ECHO_POWER} of its power, once a cast. */
		ECHO
	}

	/** When a quirk holds, read from where its rune's effect lands. */
	public enum When {
		NIGHT("at night"), DAY("by day"), RAIN("in the rain"), SNOW("while snow falls"), FULL_MOON("under a full moon"),
		NEW_MOON("under a new moon"), DEEP("in the deep places"), HIGH("high on the mountains"), UNDERGROUND("under the earth"),
		NETHER("in the Nether"), END("in the End");

		public final String phrase;

		When(String phrase) {
			this.phrase = phrase;
		}

		public boolean test(Attunements.Place place) {
			boolean overworld = place.dimension().equals("minecraft:overworld");
			return switch (this) {
				case NIGHT -> overworld && place.night();
				case DAY -> overworld && !place.night();
				case RAIN -> place.precipitation().equals("rain");
				case SNOW -> place.precipitation().equals("snow");
				case FULL_MOON -> overworld && place.night() && place.moonPhase() == 0;
				case NEW_MOON -> overworld && place.night() && place.moonPhase() == 4;
				case DEEP -> place.y() < 0;
				case HIGH -> place.y() >= 120;
				case UNDERGROUND -> overworld && !place.openSky() && place.y() < 50;
				case NETHER -> place.dimension().equals("minecraft:the_nether");
				case END -> place.dimension().equals("minecraft:the_end");
			};
		}
	}

	/**
	 * One quirk.
	 *
	 * @param id   a short id, unique in its world (Grimoire key {@code quirk:<id>})
	 * @param rune the rune it changes, by id
	 * @param text what the Grimoire says once it's found ("Here, Shock cracks harder in the rain.")
	 */
	public record Quirk(String id, String rune, Kind kind, When when, String text) {
		public String key() {
			return KEY_PREFIX + id;
		}

		public boolean holds(Attunements.Place place) {
			return when.test(place);
		}
	}

	/** Conditions that suit each element, so a quirk reads as the world's nature and not a dice roll. */
	static final Map<String, List<When>> SUITS = Map.ofEntries(
		Map.entry("fire", List.of(When.DAY, When.NETHER, When.UNDERGROUND)),
		Map.entry("frost", List.of(When.SNOW, When.NIGHT, When.HIGH)),
		Map.entry("storm", List.of(When.RAIN, When.HIGH, When.NIGHT)),
		Map.entry("wind", List.of(When.HIGH, When.RAIN, When.DAY)),
		Map.entry("earth", List.of(When.UNDERGROUND, When.DEEP, When.DAY)),
		Map.entry("life", List.of(When.DAY, When.RAIN, When.FULL_MOON)),
		Map.entry("void", List.of(When.NIGHT, When.NEW_MOON, When.END)),
		Map.entry("arcane", List.of(When.NIGHT, When.FULL_MOON, When.END)),
		Map.entry("time", List.of(When.NIGHT, When.DAY, When.NEW_MOON)),
		Map.entry("blood", List.of(When.NIGHT, When.FULL_MOON, When.NETHER)));

	/** How a stronger quirk's rune reads, by element (a harmful one; a helpful one simply "gives more"). */
	static final Map<String, String> STRONGER = Map.ofEntries(
		Map.entry("fire", "burns hotter"), Map.entry("frost", "bites colder"), Map.entry("storm", "cracks harder"),
		Map.entry("wind", "blows harder"), Map.entry("earth", "lands heavier"), Map.entry("life", "stings deeper"),
		Map.entry("void", "cuts deeper"), Map.entry("arcane", "shines brighter"), Map.entry("time", "presses harder"),
		Map.entry("blood", "bleeds deeper"));

	/** The runes a quirk may fall on: effects of the resonance pool that have a number to change. */
	public static boolean quirkable(RuneDef rune) {
		return rune.family() == RuneFamily.EFFECT && ResonanceForge.inPool(rune) && (rune.has(Trait.POWER) || rune.has(Trait.DURATION));
	}

	/** This world's quirks: {@code count} of them (at most {@link #MAX_COUNT}), drawn from its seed and reroll salt. */
	public static List<Quirk> forge(long worldSeed, String salt, int count) {
		int wanted = Math.max(0, Math.min(MAX_COUNT, count));
		Random random = new Random(ResonanceForge.seedOf(worldSeed, salt, "quirks"));
		List<RuneDef> candidates = ResonanceForge.pool().stream().filter(RuneQuirks::quirkable).toList();
		List<Quirk> out = new ArrayList<>();
		Set<String> used = new HashSet<>();
		for (int attempt = 0; out.size() < wanted && attempt < wanted * 20 && !candidates.isEmpty(); attempt++) {
			RuneDef rune = candidates.get(random.nextInt(candidates.size()));
			if (!used.add(rune.id())) {
				continue;
			}
			List<Kind> kinds = new ArrayList<>();
			if (rune.has(Trait.POWER)) {
				kinds.add(Kind.STRONGER);
			}
			if (rune.has(Trait.DURATION)) {
				kinds.add(Kind.LONGER);
			}
			if (rune.has(Trait.POWER) && rune.kind() == EffectKind.HARMFUL) {
				kinds.add(Kind.ECHO);
			}
			Kind kind = kinds.get(random.nextInt(kinds.size()));
			List<When> suits = SUITS.getOrDefault(rune.element(), List.of(When.NIGHT, When.DAY));
			When when = suits.get(random.nextInt(suits.size()));
			String id = Integer.toHexString((rune.id() + "|" + kind + "|" + when + "|" + out.size()).hashCode() & 0x7FFFFFFF);
			out.add(new Quirk(id, rune.id(), kind, when, text(rune, kind, when)));
		}
		return List.copyOf(out);
	}

	/** "Here, Shock cracks harder in the rain." */
	public static String text(RuneDef rune, Kind kind, When when) {
		String does = switch (kind) {
			case STRONGER -> rune.kind() == EffectKind.HARMFUL ? STRONGER.getOrDefault(rune.element(), "strikes harder") : "gives more";
			case LONGER -> rune.kind() == EffectKind.HARMFUL ? "holds longer" : "lasts longer";
			case ECHO -> "strikes a second time, faintly,";
		};
		return "Here, " + rune.name() + " " + does + " " + when.phrase + ".";
	}

	public static Optional<Quirk> byId(List<Quirk> quirks, String id) {
		return quirks.stream().filter(q -> q.id().equals(id)).findFirst();
	}

	/** The Grimoire key's id, or empty if {@code key} isn't a quirk's. */
	public static Optional<String> idOf(String key) {
		return key.startsWith(KEY_PREFIX) ? Optional.of(key.substring(KEY_PREFIX.length()).toLowerCase(Locale.ROOT)) : Optional.empty();
	}
}
