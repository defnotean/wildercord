package dev.wildercord.cast.events;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;

import java.util.ArrayList;
import java.util.List;

/**
 * The numbers behind the world events (mana storms, fallen stars and rift sieges): how often each
 * is rolled, how long it lasts, how big its waves are and what it gives. Every tunable lives here,
 * in one place, so a config file can take them over later. Pure Java with no Minecraft types, so
 * the rolls and tables are unit-tested.
 *
 * <p>Rolls take numbers in [0, 1) rather than a random source, so a test can ask exactly what a
 * given roll gives.</p>
 */
public final class EventRules {
	private EventRules() {}

	/**
	 * Whether the server rolls events on its own at all (the command starts them either way). The
	 * server's config can switch every event off as well, the command's included: see
	 * {@code WorldEvents.enabled}.
	 */
	public static final boolean ENABLED = true;

	public static final int DAY = 24000;
	/** Night, on the day clock: 13000 to 23000. */
	public static final int NIGHT_START = 13000;
	public static final int NIGHT_END = 23000;
	public static final int NIGHT = NIGHT_END - NIGHT_START;

	/** How often (in ticks) each player's surroundings are rolled for a new event. */
	public static final int CHECK_INTERVAL = 600;

	// ------------------------------------------------------------------ mana storms

	/** Near a ley line, a region sees a storm about this often, in days. */
	public static final double STORM_EVERY_DAYS = 3.0;
	/** A region that just had a storm won't get another for this long. */
	public static final int STORM_REGION_COOLDOWN = DAY * 3 / 2;
	/** A region is a square this many blocks across (a power of two, as a shift). */
	public static final int REGION_SHIFT = 9;
	/** Storms raging at once, in all the worlds. */
	public static final int MAX_STORMS = 2;
	public static final int STORM_MIN_TICKS = 3 * 60 * 20;
	public static final int STORM_MAX_TICKS = 5 * 60 * 20;
	/** How far from its heart a storm reaches, in blocks. */
	public static final double STORM_RADIUS = 96;
	/** How far from a player a storm looks for a ley line to gather over. */
	public static final int STORM_SEARCH = 48;
	/** Mana regeneration inside a storm: +100%. */
	public static final float STORM_REGEN_BONUS = 1.0F;
	/** Spell cost inside a storm: 25% less. */
	public static final double STORM_COST = 0.75;
	/** The chance a cast inside a storm surges. */
	public static final double SURGE_CHANCE = 0.10;
	/** Surged spells: this much more power. */
	public static final double SURGE_POWER = 1.6;
	/** Casts inside storms for the Stormcaller feat. */
	public static final int STORMCALLER_CASTS = 20;
	/** Casts under one storm before its surges may crystallise one of its runes. */
	public static final int STORM_RUNE_CASTS = 10;
	/** The chance that a surge, from then on, crystallises one of the storm's runes. */
	public static final double STORM_RUNE_CHANCE = 1.0 / 3.0;

	// ------------------------------------------------------------------ fallen stars

	/** At night, a player sees a star fall near them about once in this many nights. */
	public static final double STAR_EVERY_NIGHTS = 5.0;
	/** After a star falls in a world, none falls there for this long. */
	public static final int STAR_COOLDOWN = DAY / 2;
	public static final int STAR_MIN_DISTANCE = 60;
	public static final int STAR_MAX_DISTANCE = 150;
	/** The light column over a fallen star shows for this long, so players can race to it. */
	public static final int STAR_BEACON_TICKS = 5 * 60 * 20;
	/** An unlooted star fades after this long. */
	public static final int STAR_LIFETIME = 20 * 60 * 20;
	/** The guards appear when a player comes this close. */
	public static final double STAR_GUARD_WAKE = 40;
	/** The crater's radius, in blocks, and how many blocks it may change at most. */
	public static final double CRATER_RADIUS = 3.6;
	public static final int CRATER_MAX_BLOCKS = 64;
	/** Experience in a fallen star. */
	public static final int STAR_XP = 30;

	// ------------------------------------------------------------------ rift sieges

	/** At night near a settled place, a player sees a rift open about once in this many nights. */
	public static final double RIFT_EVERY_NIGHTS = 6.0;
	/** After a rift in a world, none opens there for this long. */
	public static final int RIFT_COOLDOWN = DAY;
	/** Block entities of a home (chests, furnaces, beds...) within a few chunks that count as a settled place. */
	public static final int SETTLED_BLOCK_ENTITIES = 6;
	public static final int WAVES = 3;
	/** When each wave comes, in ticks after the rift opens (sooner if the last one is beaten). */
	public static final int[] WAVE_AT = {60, 900, 1800};
	/** A wave beaten early brings the next this soon. */
	public static final int WAVE_EARLY = 100;
	/** After the last wave, a rift nobody closes closes itself after this long. */
	public static final int RIFT_LINGER = 90 * 20;
	/** A rift with nobody near it this long closes itself. */
	public static final int RIFT_ABANDONED = 30 * 20;
	/** How many different elements of spell, striking the rift, seal it. */
	public static final int RIFT_SEAL_ELEMENTS = 3;
	/** A rift can only be sealed once this wave has come: before that it's too raw to close. */
	public static final int RIFT_SEAL_FROM_WAVE = 2;
	/** The most monsters in one wave (the Riftcaller comes on top). */
	public static final int MAX_WAVE = 8;
	public static final int RIFT_XP = 60;

	// ------------------------------------------------------------------ rolling

	/** The chance, per check, of an event that should come once every {@code everyTicks} of eligible time. */
	public static double chancePerCheck(double everyTicks) {
		return everyTicks <= 0 ? 1.0 : Math.min(1.0, CHECK_INTERVAL / everyTicks);
	}

	public static double stormChance() {
		return chancePerCheck(STORM_EVERY_DAYS * DAY);
	}

	public static double starChance() {
		return chancePerCheck(STAR_EVERY_NIGHTS * NIGHT);
	}

	public static double riftChance() {
		return chancePerCheck(RIFT_EVERY_NIGHTS * NIGHT);
	}

	public static boolean night(long dayTime) {
		long t = Math.floorMod(dayTime, DAY);
		return t >= NIGHT_START && t <= NIGHT_END;
	}

	/** A storm's length for a roll: 3 to 5 minutes. */
	public static int stormTicks(double roll) {
		return STORM_MIN_TICKS + (int) Math.floor(clamp(roll) * (STORM_MAX_TICKS - STORM_MIN_TICKS + 1));
	}

	/** The region a block position falls in, as one number. */
	public static long region(int x, int z) {
		return ((long) (x >> REGION_SHIFT) << 32) ^ ((z >> REGION_SHIFT) & 0xFFFFFFFFL);
	}

	// ------------------------------------------------------------------ surges

	/** What a surging spell does. */
	public enum Surge {
		/** Nothing: the spell goes off as cast. */
		NONE,
		/** It swells: much more power. */
		BIGGER,
		/** A random element rides along with it. */
		ELEMENT,
		/** It goes off again a moment later, for free. */
		ECHO,
		/** It kicks back at its caster (a little). */
		BACKFIRE
	}

	/**
	 * Whether a cast inside a storm surges ({@code roll}), and how ({@code pick}): swelling is the
	 * likeliest, then a stray element, a free echo, and now and then a backfire.
	 */
	public static Surge surge(double roll, double pick) {
		if (roll >= SURGE_CHANCE) {
			return Surge.NONE;
		}
		if (pick < 0.35) {
			return Surge.BIGGER;
		}
		if (pick < 0.60) {
			return Surge.ELEMENT;
		}
		if (pick < 0.85) {
			return Surge.ECHO;
		}
		return Surge.BACKFIRE;
	}

	/** The power a surge multiplies its spell by. */
	public static double surgePower(Surge surge) {
		return surge == Surge.BIGGER ? SURGE_POWER : 1.0;
	}

	/**
	 * Whether a surge crystallises one of the storm's own runes (Manaburn or Manatide) for its caster:
	 * only a surge, only once they've cast {@link #STORM_RUNE_CASTS} spells under this storm ({@code
	 * casts}, this one included), one time in three ({@code roll}), and never twice from one storm.
	 */
	public static boolean stormRune(Surge surge, int casts, boolean already, double roll) {
		return surge != Surge.NONE && !already && casts >= STORM_RUNE_CASTS && roll < STORM_RUNE_CHANCE;
	}

	// ------------------------------------------------------------------ waves

	/** Monsters in a wave (1 to 3) with this many players near the rift; the Riftcaller comes on top in the last. */
	public static int waveSize(int wave, int players) {
		int extra = Math.max(0, players - 1);
		int size = switch (wave) {
			case 1 -> 3 + extra;
			case 2 -> 4 + extra;
			default -> 4 + extra * 2;
		};
		return Math.max(1, Math.min(MAX_WAVE, size));
	}

	/** How many of a wave are Adepts: none in the first, one in the second, more in the last with more players. */
	public static int adepts(int wave, int players) {
		int n = switch (wave) {
			case 1 -> 0;
			case 2 -> 1;
			default -> 1 + Math.max(0, players - 1) / 2;
		};
		return Math.min(n, waveSize(wave, players));
	}

	/** When wave {@code wave} (1 to 3) comes, in ticks after the rift opened. */
	public static int waveAt(int wave) {
		return WAVE_AT[Math.max(0, Math.min(WAVES, wave) - 1)];
	}

	// ------------------------------------------------------------------ rewards

	/** How many Runebound guard a fallen star: 2 to 4 (one of them an Adept). */
	public static int guards(double roll) {
		return 2 + Math.min(2, (int) Math.floor(clamp(roll) * 3));
	}

	/** A fallen star's rune: Tier III, or one time in four Tier IV. */
	public static int starRuneTier(double roll) {
		return roll < 0.25 ? 4 : 3;
	}

	/** A sealed rift's runes: mostly Tier II, sometimes III, rarely IV. */
	public static int riftRuneTier(double roll) {
		if (roll < 0.05) {
			return 4;
		}
		return roll < 0.40 ? 3 : 2;
	}

	/** Runes a rift gives with every wave beaten. */
	public static final int RIFT_RUNES = 2;

	/** Blank Runes a rift gives with every wave beaten: 2 to 4. */
	public static int riftBlanks(double roll) {
		return 2 + Math.min(2, (int) Math.floor(clamp(roll) * 3));
	}

	/**
	 * How much of a rift's full reward it gives with {@code cleared} of its waves beaten (every monster
	 * of the wave killed): a quarter for none (sealed early), then a quarter more for each wave.
	 */
	public static double riftShare(int cleared) {
		int c = Math.max(0, Math.min(WAVES, cleared));
		return (1.0 + c) / (1.0 + WAVES);
	}

	/** Runes a closed rift gives with {@code cleared} waves beaten: none for none, then one, and all of them for every wave. */
	public static int riftRunes(int cleared) {
		int c = Math.max(0, Math.min(WAVES, cleared));
		if (c == 0) {
			return 0;
		}
		return Math.max(1, RIFT_RUNES * c / WAVES);
	}

	/** Blank Runes a closed rift gives for a roll, with {@code cleared} waves beaten (at least one). */
	public static int riftBlanks(double roll, int cleared) {
		return Math.max(1, (int) Math.floor(riftBlanks(roll) * riftShare(cleared)));
	}

	/** Experience a closed rift gives with {@code cleared} waves beaten. */
	public static int riftXp(int cleared) {
		return (int) Math.round(RIFT_XP * riftShare(cleared));
	}

	/** Whether a closed rift gives a Mana Crystal: once at least one wave was beaten. */
	public static boolean riftCrystal(int cleared) {
		return cleared >= 1;
	}

	/**
	 * The runes a fallen star or a rift may give, of one tier. {@code source} is the event ({@code
	 * "starfall"} or {@code "rift"}): its own runes (see {@link dev.wildercord.spell.RuneSources}) of
	 * that tier, weighted three to one over every common rune of that tier, falling back to lower
	 * tiers if a tier is empty. (A mana storm gives only its own runes: see {@link #stormRune}.)
	 */
	public static List<RuneDef> rewardRunes(String source, int tier) {
		for (int t = tier; t >= 1; t--) {
			int of = t;
			List<RuneDef> own = dev.wildercord.spell.RuneSources.forSource(source).stream().filter(r -> r.tier() == of).toList();
			List<RuneDef> pool = new ArrayList<>();
			for (RuneDef rune : Runes.all()) {
				if (rune.tier() == t && Runes.common(rune)) {
					pool.add(rune);
				}
			}
			// The event's own runes: as likely, together, as three times everything else.
			if (!pool.isEmpty() && !own.isEmpty()) {
				int copies = Math.max(1, 3 * pool.size() / own.size());
				for (int i = 0; i < copies; i++) {
					pool.addAll(own);
				}
			}
			if (!pool.isEmpty()) {
				return pool;
			}
		}
		return List.of(Runes.BOLT);
	}

	/** One rune from {@link #rewardRunes} for a roll. */
	public static RuneDef rewardRune(String source, int tier, double roll) {
		List<RuneDef> pool = rewardRunes(source, tier);
		return pool.get(Math.min(pool.size() - 1, (int) Math.floor(clamp(roll) * pool.size())));
	}

	private static double clamp(double roll) {
		return Math.max(0, Math.min(0.999999, roll));
	}
}
