package dev.wildercord.spell;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Draws a world's resonances from its seed. Everything here is pure and deterministic: the same seed and salt give
 * the same resonances on any machine (Java's {@link Random} is specified to the bit, and the roster is read in its
 * fixed order), so a world's magic can be worked out again, and tested, without the world.
 *
 * <p>The seed is never used as it is: it's hashed with a salt of its own (and the server owner's reroll salt) through
 * SHA-256, so neither the ley seed every client is sent (another hash of the same seed) nor anything else a client
 * sees leads back to the resonances.</p>
 *
 * <p>How one is drawn: a twist first (each world uses a twist at most once), then a spell that twist can ride, built
 * from a template (a shape, one to three effects, perhaps a modifier or an On Hit) out of the runes a caster can
 * realistically get: crafted runes of tiers I to III ({@link #inPool}). The spell must read cleanly as an ordinary
 * spell (no warnings, nothing unattached), cost what an Amethyst Cord can pay, and stay clear of the secret spells,
 * the signature fusions' pairs and the world's other resonances ({@link #problem}). Then its name and riddle are
 * made from the twist and the runes.</p>
 */
public final class ResonanceForge {
	private ResonanceForge() {}

	/** How many a world has unless its owner says otherwise. */
	public static final int DEFAULT_COUNT = 12;
	/** The most a world may have: a few short of every twist, so even a full world has some left to choose from. */
	public static final int MAX_COUNT = 24;
	/** The most mana a resonance may cost as an ordinary spell, so a Cord of the third tier can cast every one. */
	public static final int MAX_COST = 70;
	/** Tries at a spell for one twist before the next twist is taken instead. */
	private static final int TRIES = 80;

	// ------------------------------------------------------------------ the seed

	/**
	 * The generator's seed: SHA-256 of the world's seed and the reroll salt, under a label of its own. One-way, so the
	 * resonances can't be worked back from anything but the world's seed itself.
	 */
	public static long seedOf(long worldSeed, String salt, String stream) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(("wildercord:" + stream + "\n" + worldSeed + "\n" + (salt == null ? "" : salt)).getBytes(StandardCharsets.UTF_8));
			return ByteBuffer.wrap(sha.digest(), 0, 8).getLong();
		} catch (NoSuchAlgorithmException e) {
			// Every Java has SHA-256; this is only here because the API says it might not.
			throw new IllegalStateException(e);
		}
	}

	private static String idOf(long seed, int index, String twist, List<String> runes) {
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update((seed + "\n" + index + "\n" + twist + "\n" + String.join(",", runes)).getBytes(StandardCharsets.UTF_8));
			byte[] digest = sha.digest();
			return String.format(Locale.ROOT, "%02x%02x%02x%02x", digest[0], digest[1], digest[2], digest[3]);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	// ------------------------------------------------------------------ the runes a resonance is made of

	/** Shapes left out: a Mine waits half a minute for a step, too long for a world to answer. */
	private static final Set<String> SHAPES_LEFT_OUT = Set.of("mine");
	/**
	 * Effects left out: flight (its own rules about when it may lift anyone), the summons (heavy, and a world's
	 * twist on top of three wolves is too much), and Overdrive, which costs the one it lands on.
	 */
	private static final Set<String> EFFECTS_LEFT_OUT = Set.of("soar", "shades", "thunderbird", "overdrive");
	/** The only link a resonance may hold: On Hit, which every caster learns early and every reader follows. */
	private static final Set<String> LINKS = Set.of("on_hit");

	/** Whether {@code rune} may be part of a resonance: a crafted rune of tiers I to III a caster can realistically get. */
	public static boolean inPool(RuneDef rune) {
		if (!rune.id().startsWith("wildercord:") || rune.tier() < 1 || rune.tier() > 3 || !Runes.common(rune) || Knots.isKnot(rune)
				|| WovenRunes.isWoven(rune) || rune == Runes.TRIGGER) {
			return false;
		}
		String path = rune.path();
		return switch (rune.family()) {
			case SHAPE -> !SHAPES_LEFT_OUT.contains(path);
			case EFFECT -> (rune.kind() == EffectKind.HARMFUL || rune.kind() == EffectKind.HELPFUL) && !EFFECTS_LEFT_OUT.contains(path);
			case MODIFIER -> MODIFIERS.containsKey(path);
			case LINK -> LINKS.contains(path);
			case KNOT -> false;
		};
	}

	private static volatile List<RuneDef> pool;

	/** Every rune a resonance may be made of, in roster order. */
	public static List<RuneDef> pool() {
		List<RuneDef> cached = pool;
		if (cached == null) {
			List<RuneDef> runes = new ArrayList<>();
			for (RuneDef rune : Runes.all()) {
				if (inPool(rune)) {
					runes.add(rune);
				}
			}
			cached = List.copyOf(runes);
			pool = cached;
		}
		return cached;
	}

	private static List<RuneDef> family(RuneFamily family) {
		return pool().stream().filter(r -> r.family() == family).toList();
	}

	// ------------------------------------------------------------------ is a spell fit to be one?

	/**
	 * Why {@code runes} can't be a resonance of a world that already has {@code taken}, or empty when they can: they
	 * must be 3 or 4 runes of the pool, read cleanly as an ordinary spell, cost no more than {@link #MAX_COST}, and be
	 * neither a secret spell, nor hold both runes of a signature fusion (a pair the Fusion Altar already answers), nor
	 * another resonance.
	 */
	public static Optional<String> problem(List<RuneDef> runes, Collection<Resonance> taken) {
		if (runes.size() < 3 || runes.size() > 4) {
			return Optional.of("a resonance has 3 or 4 runes");
		}
		for (RuneDef rune : runes) {
			if (!inPool(rune)) {
				return Optional.of(rune.name() + " can't be part of a resonance");
			}
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		if (compiled.isEmpty() || !compiled.warnings().isEmpty()) {
			return Optional.of("it doesn't read cleanly as a spell");
		}
		if (compiled.manaCost() > MAX_COST) {
			return Optional.of("it costs too much");
		}
		if (Secrets.match(runes).isPresent()) {
			return Optional.of("it's a secret spell");
		}
		for (Fusions.Signature signature : Fusions.SIGNATURES) {
			if (runes.contains(signature.a()) && runes.contains(signature.b())) {
				return Optional.of("it holds a signature fusion's pair");
			}
		}
		for (Resonance other : taken) {
			if (other.matches(runes)) {
				return Optional.of("it's already another resonance");
			}
		}
		return Optional.empty();
	}

	// ------------------------------------------------------------------ drawing a world

	/**
	 * This world's resonances: {@code count} of them (at most {@link #MAX_COUNT}), drawn from {@code worldSeed} and the
	 * owner's reroll {@code salt}. The draw is one sequence, so the first {@code n} are the same whatever the count.
	 */
	public static List<Resonance> forge(long worldSeed, String salt, int count) {
		int wanted = Math.max(0, Math.min(MAX_COUNT, count));
		long seed = seedOf(worldSeed, salt, "resonances");
		Random random = new Random(seed);
		List<ResonanceTwists.Twist> twists = new ArrayList<>(ResonanceTwists.ALL);
		Collections.shuffle(twists, random);
		List<Resonance> out = new ArrayList<>();
		Set<String> names = new HashSet<>();
		for (ResonanceTwists.Twist twist : twists) {
			if (out.size() >= wanted) {
				break;
			}
			Optional<List<RuneDef>> runes = draw(twist, random, out);
			if (runes.isEmpty()) {
				continue;
			}
			List<String> ids = runes.get().stream().map(RuneDef::id).toList();
			String id = idOf(seed, out.size(), twist.id(), ids);
			while (taken(out, id)) {
				id = id + out.size();
			}
			String name = name(twist, runes.get(), random, names);
			names.add(name);
			out.add(new Resonance(id, name, riddle(twist, runes.get(), random), ids, twist.id(), color(twist, runes.get())));
		}
		return List.copyOf(out);
	}

	private static boolean taken(List<Resonance> out, String id) {
		return out.stream().anyMatch(r -> r.id().equals(id));
	}

	/** The spell shapes in which one may be built, as a string of family letters (S shape, E effect, M modifier, L link). */
	private static final List<String> HARMFUL_TEMPLATES = List.of("SEE", "SEM", "SME", "SEEM", "SEME", "SMEE", "SELE", "SEEE");
	private static final List<String> HELPFUL_TEMPLATES = List.of("SEE", "SEM", "SEEM", "SEME", "SME");

	/** A spell {@code twist} can ride, or empty if none turned up in {@link #TRIES} tries. */
	static Optional<List<RuneDef>> draw(ResonanceTwists.Twist twist, Random random, List<Resonance> taken) {
		ResonanceTwists.Fit fit = twist.fit();
		List<RuneDef> shapes = family(RuneFamily.SHAPE).stream().filter(s -> fit.allowsShape(s.path())).toList();
		List<RuneDef> leads = family(RuneFamily.EFFECT).stream().filter(fit::allowsLead).toList();
		List<RuneDef> others = family(RuneFamily.EFFECT).stream().filter(e -> e.kind() == fit.lead()).toList();
		List<RuneDef> modifiers = family(RuneFamily.MODIFIER);
		List<RuneDef> links = family(RuneFamily.LINK);
		if (shapes.isEmpty() || leads.isEmpty()) {
			return Optional.empty();
		}
		List<String> templates = fit.lead() == EffectKind.HELPFUL ? HELPFUL_TEMPLATES : HARMFUL_TEMPLATES;
		for (int attempt = 0; attempt < TRIES; attempt++) {
			String template = templates.get(random.nextInt(templates.size()));
			List<RuneDef> runes = new ArrayList<>(template.length());
			boolean lead = true;
			for (char part : template.toCharArray()) {
				switch (part) {
					case 'S' -> runes.add(pick(shapes, random));
					case 'E' -> {
						RuneDef effect = lead ? pick(leads, random) : pick(others, random);
						// Now and then a rune twice over (as Frost, Frost in Glacial Lance); otherwise each effect its own.
						if (!lead && runes.contains(effect) && random.nextInt(6) != 0) {
							effect = pick(others, random);
						}
						runes.add(effect);
						lead = false;
					}
					case 'M' -> runes.add(pick(modifiers, random));
					case 'L' -> runes.add(pick(links, random));
					default -> throw new IllegalStateException("template " + template);
				}
			}
			if (problem(runes, taken).isEmpty()) {
				return Optional.of(List.copyOf(runes));
			}
		}
		return Optional.empty();
	}

	/** One rune, the lower tiers likelier (3 to 2 to 1 for tiers I, II and III), so most resonances are within early reach. */
	private static RuneDef pick(List<RuneDef> from, Random random) {
		int total = 0;
		for (RuneDef rune : from) {
			total += 4 - rune.tier();
		}
		int roll = random.nextInt(total);
		for (RuneDef rune : from) {
			roll -= 4 - rune.tier();
			if (roll < 0) {
				return rune;
			}
		}
		return from.getLast();
	}

	/** The first effect's element: what the spell is "of". */
	static String element(List<RuneDef> runes) {
		for (RuneDef rune : runes) {
			if (rune.family() == RuneFamily.EFFECT && !rune.element().isEmpty()) {
				return rune.element();
			}
		}
		return "arcane";
	}

	private static int color(ResonanceTwists.Twist twist, List<RuneDef> runes) {
		return twist.color();
	}

	// ------------------------------------------------------------------ names

	/** Words for each element, the second half of a name ("wind" in "the Glasswind Rite"). */
	static final Map<String, List<String>> ELEMENT_WORDS = Map.ofEntries(
		Map.entry("fire", List.of("cinder", "flame", "pyre", "brand")),
		Map.entry("frost", List.of("rime", "snow", "hoar", "sleet")),
		Map.entry("storm", List.of("thunder", "levin", "squall", "storm")),
		Map.entry("wind", List.of("wind", "gale", "breeze", "draught")),
		Map.entry("earth", List.of("stone", "loam", "flint", "clay")),
		Map.entry("life", List.of("thorn", "sap", "briar", "leaf")),
		Map.entry("void", List.of("night", "shade", "dusk", "gloam")),
		Map.entry("arcane", List.of("star", "sigil", "wonder", "lumen")),
		Map.entry("time", List.of("hour", "dial", "vesper", "morrow")),
		Map.entry("blood", List.of("crimson", "heart", "marrow", "scarlet")));
	/** What kind of working a resonance is: the last word of its name. */
	static final List<String> RITES = List.of("Rite", "Canticle", "Litany", "Hymn", "Vigil", "Psalm", "Working", "Measure", "Verse", "Charm",
		"Liturgy", "Chant", "Invocation", "Office", "Rondel", "Covenant");

	/** "the Glasswind Rite": the twist's word, the spell's element's word, and a rite; never one the world has already. */
	static String name(ResonanceTwists.Twist twist, List<RuneDef> runes, Random random, Set<String> taken) {
		List<String> words = ELEMENT_WORDS.getOrDefault(element(runes), ELEMENT_WORDS.get("arcane"));
		String name = "";
		for (int attempt = 0; attempt < 40; attempt++) {
			String stem = twist.stems().get(random.nextInt(twist.stems().size()));
			String word = words.get(random.nextInt(words.size()));
			if (stem.toLowerCase(Locale.ROOT).endsWith(word) || word.startsWith(stem.toLowerCase(Locale.ROOT))) {
				continue;
			}
			name = "the " + stem + word + " " + RITES.get(random.nextInt(RITES.size()));
			if (!taken.contains(name)) {
				return name;
			}
		}
		// Every combination taken (it would take a world far bigger than MAX_COUNT): number it.
		return name + " " + (taken.size() + 1);
	}

	// ------------------------------------------------------------------ riddles

	/** How each shape is spoken of in a riddle. */
	static final Map<String, String> SHAPES = Map.ofEntries(
		Map.entry("self", "your own heart"), Map.entry("touch", "a hand laid on"), Map.entry("bolt", "a loosed bolt"),
		Map.entry("beam", "a line of light"), Map.entry("burst", "a breath let outward"), Map.entry("zone", "a field that keeps"),
		Map.entry("rain", "what falls from the sky"), Map.entry("arc", "a thrown arc"), Map.entry("cone", "a fan from the hand"),
		Map.entry("trail", "the road behind you"), Map.entry("wall", "a standing wall"), Map.entry("orbit", "three that circle"),
		Map.entry("ring", "a widening ring"), Map.entry("pillar", "a rising column"), Map.entry("wave", "a rolling wave"),
		Map.entry("totem", "a floating idol"), Map.entry("crescent", "a flying crescent"), Map.entry("barrage", "a flurry of blows"),
		Map.entry("orb", "a slow and heavy orb"), Map.entry("blitz", "a flash forward"), Map.entry("spark", "a darting spark"),
		Map.entry("ray", "a short ray"), Map.entry("nova", "a small star at your feet"), Map.entry("wisp", "a hunting wisp"),
		Map.entry("comet", "a heavy comet"), Map.entry("ricochet", "an orb that rebounds"), Map.entry("cluster", "a ball that breaks in five"),
		Map.entry("lance", "a lance of light"), Map.entry("sweep", "a sweeping line"), Map.entry("prism", "a prism that parts"),
		Map.entry("stream", "a steady stream"), Map.entry("glaive", "a glaive that comes home"), Map.entry("imprint", "a mark where you stood"),
		Map.entry("latch", "a thread that latches"), Map.entry("mine", "a hidden rune"),
		// ---- shapes pack
		Map.entry("furrow", "a furrow drawn"), Map.entry("plot", "a little plot"), Map.entry("seedbed", "a bed for seed"),
		Map.entry("shaft", "a shaft sunk down"), Map.entry("stairwell", "stairs into the deep"), Map.entry("corridor", "a hall cut through"),
		Map.entry("seam", "a seam across stone"), Map.entry("facade", "a face of stone"), Map.entry("dome", "a vault of sky"),
		Map.entry("footing", "the ground you stand on"), Map.entry("canopy", "a roof of leaves"), Map.entry("shoreline", "where water meets land"),
		Map.entry("perimeter", "a bound drawn round"), Map.entry("spire", "a spire raised"), Map.entry("pit", "a pit dug"),
		Map.entry("crossway", "where roads cross"), Map.entry("lodeseek", "a seeker of ore"), Map.entry("vault", "a cube of the deep"),
		Map.entry("lamplit", "lamps at the corners"), Map.entry("fissure", "a crack that runs"), Map.entry("spiral", "a winding coil"),
		Map.entry("rosette", "a flower of stone"), Map.entry("stepstones", "stones to step on"), Map.entry("causeway", "a road laid ahead"),
		Map.entry("hedgerow", "a row of hedge"), Map.entry("lattice", "a checkered field"), Map.entry("collapse", "a falling ceiling"),
		Map.entry("fan", "five spokes spread"), Map.entry("bobber", "a float on the water"), Map.entry("herd", "the herd at pasture"),
		Map.entry("fellowship", "all who walk with you"), Map.entry("saddle", "you and your mount"), Map.entry("packbond", "the pack that follows"),
		Map.entry("nursery", "the young ones"), Map.entry("shoal", "all that swim"), Map.entry("rearguard", "what is behind you"),
		Map.entry("grudge", "those who wronged you"), Map.entry("sentinel", "those who hunt your own"), Map.entry("aureole", "a crown of light about you"),
		Map.entry("tether", "a creature and its kin"), Map.entry("flock", "all that fly"));
	/** How each modifier a resonance may hold is spoken of: what it does to the rune before it. */
	static final Map<String, String> MODIFIERS = Map.ofEntries(
		Map.entry("amplify", "made greater"), Map.entry("extend", "made to last"), Map.entry("widen", "widened"),
		Map.entry("quicken", "quickened"), Map.entry("pierce", "passing through"), Map.entry("bounce", "rebounding"),
		Map.entry("split", "split in three"), Map.entry("homing", "seeking"), Map.entry("chain", "leaping on"),
		Map.entry("frugal", "spent sparingly"), Map.entry("linger", "lingering"), Map.entry("volley", "loosed again and again"),
		Map.entry("focus", "drawn tight"), Map.entry("overcharge", "filled past brimming"), Map.entry("execute", "cruel to the weak"),
		Map.entry("kindred", "shared"), Map.entry("thirst", "thirsting"), Map.entry("belated", "late, and heavier for it"));
	private static final List<String> CARRY = List.of("bearing", "dressed in", "carrying", "with");

	/**
	 * The riddle: the twist's omen and the runes in their order, each spoken of in a veiled way (shapes and modifiers
	 * by what they do, effects by name), in one of a few frames so no two worlds' riddles read alike.
	 */
	static String riddle(ResonanceTwists.Twist twist, List<RuneDef> runes, Random random) {
		String body = body(runes, CARRY.get(random.nextInt(CARRY.size())));
		String omen = twist.omen();
		return switch (random.nextInt(4)) {
			case 0 -> capital(omen) + ": " + body + ".";
			case 1 -> capital(body) + ", and " + omen + ".";
			case 2 -> "Ask for " + body + ", and " + omen + ".";
			default -> "When " + body + " is cast, " + omen + ".";
		};
	}

	/** The runes as one phrase, in their order: "a loosed bolt, split in three, carrying fire twice". */
	static String body(List<RuneDef> runes, String carry) {
		StringBuilder out = new StringBuilder();
		boolean effectSeen = false;
		boolean afterLink = false;
		boolean afterModifier = false;
		for (int i = 0; i < runes.size(); i++) {
			RuneDef rune = runes.get(i);
			switch (rune.family()) {
				case SHAPE -> out.append(SHAPES.getOrDefault(rune.path(), "a " + rune.name().toLowerCase(Locale.ROOT)));
				case MODIFIER -> {
					out.append(", ").append(MODIFIERS.getOrDefault(rune.path(), rune.name().toLowerCase(Locale.ROOT)));
					afterModifier = true;
					continue;
				}
				case LINK -> {
					out.append(", and where it strikes,");
					afterLink = true;
					continue;
				}
				case EFFECT -> {
					String name = rune.name().toLowerCase(Locale.ROOT);
					boolean twice = i + 1 < runes.size() && runes.get(i + 1) == rune;
					if (afterLink) {
						out.append(' ').append(name);
					} else if (!effectSeen) {
						out.append(afterModifier ? ", " : " ").append(carry).append(' ').append(name);
					} else {
						out.append(afterModifier ? ", then " : " and ").append(name);
					}
					if (twice) {
						out.append(" twice");
						i++;
					}
					effectSeen = true;
					afterLink = false;
				}
			}
			afterModifier = false;
		}
		return out.toString();
	}

	private static String capital(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}
}
