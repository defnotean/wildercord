package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knots: a whole spell tied into one rune at the Fusion Altar. A Knot takes one socket, costs 10%
 * less mana than the runes inside it, and anyone can learn it, even without knowing those runes.
 *
 * <p>A Knot carries its spell in its own id, so it needs nothing else to be read, traded or cast:
 * {@code wildercord:knot/<base32>}, where the base32 text is the runes (built-in runes by their path,
 * add-on runes by their full id, Knots inside by their own Knot id) joined by commas, then {@code |}
 * and the spell's name if it had one. Base32 keeps the id a valid resource path whatever runes and
 * name it holds. Knots can hold Knots, {@link #MAX_DEPTH} deep.</p>
 *
 * <p>The compiler reads a Knot as its runes (see {@link SpellCompiler}); everything else treats it
 * as one rune whose tier is the highest tier inside, so a Cord's tier limit still applies to them.</p>
 */
public final class Knots {
	private Knots() {}

	public static final String PREFIX = "wildercord:knot/";
	/** A Knot inside a Knot is 2 deep; no deeper. */
	public static final int MAX_DEPTH = 2;
	/** Runes a Knot holds directly: one spell's worth. */
	public static final int MAX_RUNES = 12;
	/** Runes a Knot holds in all, its Knots' runes included. */
	public static final int MAX_FLAT = MAX_RUNES * MAX_RUNES;
	/** Longer ids aren't read at all, so nobody can make the server decode something huge. */
	public static final int MAX_ID_LENGTH = 8192;
	/** Each rune inside a Knot costs this much of its usual mana. */
	public static final double DISCOUNT = 0.9;
	/** XP levels to tie a Knot: one per rune inside, and at least this many. */
	public static final int MIN_XP = 2;

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz234567";

	/** Read Knots, by id (empty for an id that isn't a readable Knot). Cleared when it grows too big. */
	private static final Map<String, Optional<RuneDef>> CACHE = new ConcurrentHashMap<>();
	private static final int CACHE_LIMIT = 4096;

	public static boolean isKnot(String id) {
		return id != null && id.startsWith(PREFIX);
	}

	public static boolean isKnot(RuneDef rune) {
		return rune.family() == RuneFamily.KNOT;
	}

	// ------------------------------------------------------------------ tying

	/** The id of a Knot holding {@code runes}, named {@code name} ("" for the automatic name). */
	public static String id(List<RuneDef> runes, String name) {
		StringBuilder text = new StringBuilder();
		for (RuneDef rune : runes) {
			if (!text.isEmpty()) {
				text.append(',');
			}
			text.append(compact(rune.id()));
		}
		String clean = SpellNames.clean(name == null ? "" : name);
		if (!clean.isEmpty()) {
			text.append('|').append(clean);
		}
		return PREFIX + encode(text.toString().getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Why {@code runes} can't be tied into a Knot (in plain English), or null if they can. A Knot needs
	 * a spell that does something, holds no Imbue (a stored spell can't be tied up again), and keeps
	 * Knots at most {@link #MAX_DEPTH} deep.
	 */
	public static String problem(List<RuneDef> runes) {
		if (runes.isEmpty()) {
			return "There's nothing in that spell to tie.";
		}
		if (runes.size() > MAX_RUNES) {
			return "A Knot holds " + MAX_RUNES + " runes at most.";
		}
		for (RuneDef rune : runes) {
			if (rune.is(Runes.IMBUE.id())) {
				return "A spell with Imbue can't be tied into a Knot.";
			}
		}
		// An innate rune is its caster's alone: a Knot mustn't hand it to anyone who learns the Knot.
		for (RuneDef rune : flatten(runes)) {
			if (Runes.innate(rune)) {
				return "An innate rune can't be tied into a Knot: it's yours alone.";
			}
		}
		if (1 + innerDepth(runes) > MAX_DEPTH) {
			return "Knots can only hold Knots " + MAX_DEPTH + " deep.";
		}
		if (flatten(runes).size() > MAX_FLAT) {
			return "That spell holds too many runes to tie.";
		}
		if (SpellCompiler.compile(runes).isEmpty()) {
			return "That spell doesn't do anything yet.";
		}
		return null;
	}

	/** XP levels to tie {@code runes} into a Knot: one per rune inside (a Knot's runes included), at least {@link #MIN_XP}. */
	public static int xpCost(List<RuneDef> runes) {
		return Math.max(MIN_XP, flatten(runes).size());
	}

	// ------------------------------------------------------------------ reading

	/** The rune a Knot id stands for, or empty if it isn't a Knot, doesn't decode, or holds a rune that isn't loaded. */
	public static Optional<RuneDef> def(String id) {
		if (!isKnot(id) || id.length() > MAX_ID_LENGTH) {
			return Optional.empty();
		}
		Optional<RuneDef> known = CACHE.get(id);
		if (known != null) {
			return known;
		}
		// Not computeIfAbsent: reading a Knot reads the Knots inside it, which would update the map mid-update.
		Optional<RuneDef> read = read(id);
		if (CACHE.size() >= CACHE_LIMIT) {
			CACHE.clear();
		}
		CACHE.put(id, read);
		return read;
	}

	/** The runes a Knot holds, in order (empty for anything that isn't a readable Knot). */
	public static List<RuneDef> contents(RuneDef knot) {
		if (!isKnot(knot)) {
			return List.of();
		}
		// Screens ask this every frame: the decoded runes are kept, like the Knot's own rune.
		List<RuneDef> known = CONTENTS.get(knot.id());
		if (known != null) {
			return known;
		}
		List<RuneDef> read = List.copyOf(decode(knot));
		if (CONTENTS.size() >= CACHE_LIMIT) {
			CONTENTS.clear();
		}
		CONTENTS.put(knot.id(), read);
		return read;
	}

	private static final Map<String, List<RuneDef>> CONTENTS = new ConcurrentHashMap<>();

	private static List<RuneDef> decode(RuneDef knot) {
		Parsed parsed = parse(knot.id());
		if (parsed == null) {
			return List.of();
		}
		List<RuneDef> runes = new ArrayList<>();
		for (String id : parsed.ids()) {
			Optional<RuneDef> rune = Runes.get(id);
			if (rune.isEmpty()) {
				return List.of();
			}
			runes.add(rune.get());
		}
		return runes;
	}

	/** The custom name a Knot was tied with, or "" if it uses the automatic one. */
	public static String customName(RuneDef knot) {
		Parsed parsed = isKnot(knot) ? parse(knot.id()) : null;
		return parsed == null ? "" : parsed.name();
	}

	/** How deep the Knots go in {@code runes}: 0 with no Knot, 1 with a Knot holding plain runes, and so on. */
	public static int innerDepth(List<RuneDef> runes) {
		int depth = 0;
		for (RuneDef rune : runes) {
			if (isKnot(rune)) {
				depth = Math.max(depth, 1 + innerDepth(contents(rune)));
			}
		}
		return depth;
	}

	/** {@code runes} with every Knot (and every Knot in those) replaced by the runes it holds. */
	public static List<RuneDef> flatten(List<RuneDef> runes) {
		List<RuneDef> out = new ArrayList<>();
		flatten(runes, out, 0);
		return out;
	}

	private static void flatten(List<RuneDef> runes, List<RuneDef> out, int depth) {
		for (RuneDef rune : runes) {
			if (isKnot(rune) && depth < MAX_DEPTH) {
				flatten(contents(rune), out, depth + 1);
			} else if (!isKnot(rune)) {
				out.add(rune);
			}
		}
	}

	/** The spell inside, as its tooltip shows it: "Bolt · Fire · (Healing: Self · Heal)". */
	public static String sequence(List<RuneDef> runes) {
		StringBuilder out = new StringBuilder();
		for (RuneDef rune : runes) {
			if (!out.isEmpty()) {
				out.append(" · ");
			}
			if (isKnot(rune)) {
				out.append('(').append(rune.name()).append(": ").append(sequence(contents(rune))).append(')');
			} else {
				out.append(rune.name());
			}
		}
		return out.toString();
	}

	private static Optional<RuneDef> read(String id) {
		Parsed parsed = parse(id);
		if (parsed == null || parsed.ids().isEmpty() || parsed.ids().size() > MAX_RUNES) {
			return Optional.empty();
		}
		List<RuneDef> runes = new ArrayList<>();
		int tier = 1;
		for (String inner : parsed.ids()) {
			// A Knot can't hold itself, and anything unreadable (or not loaded) makes the whole Knot silent.
			Optional<RuneDef> rune = inner.equals(id) ? Optional.empty() : Runes.get(inner);
			if (rune.isEmpty()) {
				return Optional.empty();
			}
			runes.add(rune.get());
			tier = Math.max(tier, rune.get().tier());
		}
		if (1 + innerDepth(runes) > MAX_DEPTH || runes.stream().anyMatch(r -> r.is(Runes.IMBUE.id()) || Runes.innate(r))) {
			return Optional.empty();
		}
		String name = parsed.name().isEmpty() ? SpellNames.auto(runes) : parsed.name();
		if (name.isEmpty()) {
			name = "Knot";
		}
		return Optional.of(new RuneDef(id, name, RuneFamily.KNOT, tier, 0, 1.0, "", EffectKind.NONE, Set.of(), "", sequence(runes), "knot"));
	}

	private record Parsed(List<String> ids, String name) {}

	private static Parsed parse(String id) {
		if (!isKnot(id)) {
			return null;
		}
		byte[] bytes = decode(id.substring(PREFIX.length()));
		if (bytes == null) {
			return null;
		}
		String text = new String(bytes, StandardCharsets.UTF_8);
		int bar = text.indexOf('|');
		String body = bar < 0 ? text : text.substring(0, bar);
		String name = bar < 0 ? "" : SpellNames.clean(text.substring(bar + 1));
		List<String> ids = new ArrayList<>();
		if (!body.isEmpty()) {
			for (String part : body.split(",", -1)) {
				if (part.isEmpty()) {
					return null;
				}
				ids.add(part.contains(":") ? part : "wildercord:" + part);
			}
		}
		return new Parsed(ids, name);
	}

	/** A rune id as a Knot writes it: built-in runes (Knots included) without their namespace. */
	private static String compact(String id) {
		return id.startsWith("wildercord:") ? id.substring("wildercord:".length()) : id;
	}

	// ------------------------------------------------------------------ base32 (RFC 4648, lower case, no padding)

	static String encode(byte[] bytes) {
		StringBuilder out = new StringBuilder((bytes.length * 8 + 4) / 5);
		int buffer = 0;
		int bits = 0;
		for (byte b : bytes) {
			buffer = (buffer << 8) | (b & 0xFF);
			bits += 8;
			while (bits >= 5) {
				out.append(ALPHABET.charAt((buffer >> (bits - 5)) & 31));
				bits -= 5;
			}
		}
		if (bits > 0) {
			out.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
		}
		return out.toString();
	}

	static byte[] decode(String text) {
		byte[] out = new byte[text.length() * 5 / 8];
		int buffer = 0;
		int bits = 0;
		int n = 0;
		for (int i = 0; i < text.length(); i++) {
			int v = ALPHABET.indexOf(text.charAt(i));
			if (v < 0) {
				return null;
			}
			buffer = (buffer << 5) | v;
			bits += 5;
			if (bits >= 8) {
				if (n >= out.length) {
					return null;
				}
				out[n++] = (byte) ((buffer >> (bits - 8)) & 0xFF);
				bits -= 8;
			}
		}
		return n == out.length ? out : null;
	}
}
