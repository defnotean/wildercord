package dev.wildercord.spell;

import java.util.List;
import java.util.Optional;

/**
 * Secret spells: a handful of exact rune sequences that the runes themselves recognise. Thread
 * one in exactly this order (nothing before, between or after) and the spell becomes something
 * new, with its own name and behaviour. Nothing in the game lists them: players find them by
 * experimenting, or from the riddles on Torn Pages found in old chests. Found ones are written
 * into the Grimoire.
 *
 * <p>Every sequence is also a valid ordinary spell, so a half-finished guess never looks broken.</p>
 *
 * <p>A secret costs {@link Secret#power} times the ordinary spell's mana and recharges
 * {@link #COOLDOWN} times as long, but only once the caster has found it: until then its name, price
 * and cooldown are the ordinary spell's everywhere (anything else would give it away), and the first
 * cast, which finds it, charges that (see {@code Heart.foundSecret}).</p>
 */
public final class Secrets {
	private Secrets() {}

	/** A found secret spell's cooldown, as a multiple of the ordinary spell's. */
	public static final double COOLDOWN = 1.5;

	/**
	 * @param id       short id, e.g. {@code glacial_lance} (Grimoire key {@code secret:<id>})
	 * @param runes    the exact sequence
	 * @param color    the spell's colour in the readout and its visuals
	 * @param power    multiplier on the ordinary spell's mana cost (once found), and on what it weighs against a Shield
	 */
	public record Secret(String id, String name, List<RuneDef> runes, int color, double power, String description, String riddle) {
		public String key() {
			return "secret:" + id;
		}
	}

	public static final Secret GLACIAL_LANCE = new Secret("glacial_lance", "Glacial Lance",
		List.of(Runes.BOLT, Runes.FROST, Runes.FROST, Runes.SHOCK), 0x9FE4FF, 1.2,
		"A lance of ice flies 32 blocks through everything in its path, freezing each creature solid, then lightning leaps along the frozen line.",
		"A bolt dressed twice in winter, and a spark to wake it.");
	public static final Secret SUNFALL = new Secret("sunfall", "Sunfall",
		List.of(Runes.RAIN, Runes.FIRE, Runes.EXPLODE, Runes.AMPLIFY), 0xFFA040, 1.3,
		"A small sun sinks onto the point over two seconds, then bursts: 26 damage within 7 blocks, and the ground burns.",
		"Let the rain be fire, the fire a blast, and the blast made greater.");
	public static final Secret HORIZON_CUT = new Secret("horizon_cut", "Horizon Cut",
		List.of(Runes.CRESCENT, Runes.CLEAVE, Runes.DISMANTLE, Runes.WIDEN), 0xF4E8FF, 1.3,
		"One slash that grows to 30 blocks wide in front of you, cutting everything at once for 12 damage straight through armour, plus a tenth of its health.",
		"A crescent that cleaves and dismantles, widened to the edge of sight.");
	public static final Secret PETAL_STORM = new Secret("petal_storm", "Petal Storm",
		List.of(Runes.ORBIT, Runes.VENOM, Runes.HEAL, Runes.SPLIT_MOD), 0xFFA8D8, 1.1,
		"A storm of petals whirls around you for 8 seconds: allies within 7 blocks heal each second, enemies are poisoned and cut.",
		"Circling poison and circling mending, split into a storm.");
	public static final Secret TEMPEST_STEP = new Secret("tempest_step", "Tempest Step",
		List.of(Runes.BLITZ, Runes.SHOCK, Runes.SHOCK, Runes.PUSH), 0xFFF070, 1.2,
		"You flash from enemy to enemy, up to five of them within 12 blocks, striking each with lightning as you pass.",
		"Flash forward on two sparks and a shove.");
	public static final Secret SINGULARITY = new Secret("singularity", "Singularity",
		List.of(Runes.ORB, Runes.GRAVITY_WELL, Runes.PULL, Runes.EXPLODE), 0x9A5AF0, 1.3,
		"A black star drifts out and stops 12 blocks away. For 3 seconds it swallows everything within 9 blocks, then it collapses: 18 damage.",
		"A slow orb that drinks the world: well, pull, and burst.");
	public static final Secret ZERO_HOUR = new Secret("zero_hour", "Zero Hour",
		List.of(Runes.SELF, Runes.STASIS, Runes.ACCELERATE, Runes.EXTEND), 0xF2D98A, 1.5,
		"Time stops for everything within 20 blocks but you, for 4 seconds. Every hit meanwhile is held, and lands when time moves again.",
		"Stop time on yourself, then hurry it along, and make it last.");
	public static final Secret REBIRTH = new Secret("rebirth", "Rebirth",
		List.of(Runes.SELF, Runes.REVERSAL, Runes.FIRE, Runes.HEAL), 0xFF7040, 1.2,
		"For 60 seconds, a killing blow burns you back to life instead: full health, fire resistance, and a blast of flame (14 damage within 6 blocks).",
		"Refuse death, burn, and mend, all upon yourself.");
	public static final Secret TECTONIC_RISE = new Secret("tectonic_rise", "Tectonic Rise",
		List.of(Runes.WAVE, Runes.TREMOR, Runes.RAMPART, Runes.WIDEN), 0xC8A070, 1.2,
		"The ground erupts in a line of stone spires 20 blocks long: 10 damage, and everything they hit is thrown into the air.",
		"A wave that shakes the earth and raises walls wide.");
	public static final Secret STARLIGHT_CASCADE = new Secret("starlight_cascade", "Starlight Cascade",
		List.of(Runes.BEAM, Runes.STARFALL, Runes.STARFALL), 0xB8C8FF, 1.2,
		"A beam that calls the sky down along its length: 24 stars over 3 seconds, 7 damage each.",
		"A beam that calls the stars down twice.");

	public static final List<Secret> ALL = List.of(GLACIAL_LANCE, SUNFALL, HORIZON_CUT, PETAL_STORM, TEMPEST_STEP, SINGULARITY, ZERO_HOUR,
		REBIRTH, TECTONIC_RISE, STARLIGHT_CASCADE);

	/** The secret these runes spell out exactly, if any. */
	public static Optional<Secret> match(List<RuneDef> runes) {
		for (Secret secret : ALL) {
			if (secret.runes().size() == runes.size()) {
				boolean same = true;
				for (int i = 0; i < runes.size() && same; i++) {
					same = runes.get(i).is(secret.runes().get(i).id());
				}
				if (same) {
					return Optional.of(secret);
				}
			}
		}
		return Optional.empty();
	}

	public static Optional<Secret> byId(String id) {
		return ALL.stream().filter(s -> s.id().equals(id)).findFirst();
	}
}
