package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Every rune's spoken syllable, and a spell's incantation: its runes' syllables in casting order. A
 * charging caster's incantation rises from them, one syllable as each rune's roundel opens on the
 * circle, so anyone close enough can read what's coming (see {@code client.fx.Incantations}).
 *
 * <p>A syllable is made from the rune's id by a small generator of arcane but readable sounds (an
 * opening, a vowel, an ending), with the best-known runes given theirs by hand. Runes of the roster
 * get theirs in roster order and never share one: a rune whose sound is taken tries the next one its
 * id makes. New runes are added after the old ones, so adding a rune never changes an older rune's
 * syllable. A rune the roster doesn't know (an add-on's, a Knot) still gets one from its id.</p>
 */
public final class Incantation {
	private Incantation() {}

	/** The openings: single sounds and a few soft clusters. */
	private static final String[] ONSETS = {"", "k", "v", "th", "s", "r", "m", "n", "l", "z", "d", "t", "h", "sh", "kh", "f", "y", "g", "b", "dr",
		"vr", "sk", "ph"};
	private static final String[] VOWELS = {"a", "e", "i", "o", "u", "ae", "ai", "ei", "au"};
	/** The endings; the empty one twice, so open syllables come up as often as closed ones of any one kind. */
	private static final String[] CODAS = {"", "", "n", "r", "s", "l", "th", "x", "m", "sh", "k"};
	private static final Set<String> CLUSTERS = Set.of("th", "sh", "kh", "dr", "vr", "sk", "ph");
	private static final Set<String> SOFT_CODAS = Set.of("", "n", "r", "s", "l");
	/**
	 * Sounds that read as words (English, mostly, and a few others) and would be taken for them in a
	 * line of script, or taken amiss: an incantation should only ever read as one.
	 */
	private static final Set<String> AVOID = Set.of(
		"ass", "sex", "fag", "nig", "gay", "cum", "tit", "fuk", "dik", "die", "kil", "hell", "damn", "nazi", "rap", "rape", "shit", "piss",
		"poo", "pee", "sus", "lol", "omg", "bum", "fat", "hoe", "ho", "ok", "no", "yes", "bra", "kys", "god", "sin", "hex", "gun", "kik", "dum",
		"lam", "nub", "ugh", "bob", "hi", "yo", "ma", "pa", "uh", "um", "oh", "ah", "eh", "ha", "he", "she", "shi", "sha", "the", "thu", "his",
		"her", "him", "dad", "mom", "boo", "fae", "hai", "in", "is", "it", "if", "of", "on", "or", "an", "as", "at", "be", "by", "do", "go",
		"me", "my", "so", "to", "up", "us", "we", "ex", "bi", "sis", "box", "mix", "fix", "six", "tux", "dix", "fax", "wax", "tax", "max", "lax",
		"sax", "nix", "fox", "sox", "sun", "fun", "run", "bun", "nun", "ten", "men", "den", "hen", "yen", "rum", "gum", "hum", "sum", "mum",
		"yum", "nil", "ill", "il", "ol", "en", "si", "aus", "der", "drei", "nein", "gel", "del", "yin", "lain", "hon", "min", "heir", "for",
		"yon", "moth", "main", "mail", "gain", "drain", "rain", "vain", "fain", "sail", "tail", "nail", "fail", "bail", "hail", "rail", "shaun",
		"dal", "ton", "gosh", "fan", "ruth", "haul", "mam", "gin", "kuk", "kok", "fen", "gaul", "dox", "meth", "has", "ye", "nosh", "ska",
		"both", "yak", "dis", "zen", "kin", "tin", "bin", "din", "fin", "sir", "fir", "lid", "rid", "kid", "bid", "mud", "bud", "dud", "dub",
		"pub", "sub", "tub", "rub", "hub", "cub", "lab", "tab", "dab", "jab", "nab", "kab", "rob", "sob", "mob", "lob", "gob", "fob", "dot",
		"lot", "hot", "not", "rot", "tot", "hat", "mat", "rat", "sat", "vat", "lit", "sit", "hit", "kit", "bit", "fit", "nit", "pit", "zit",
		"rut", "hut", "nut", "gut", "but", "tut", "mut", "lad", "mad", "sad", "bad", "had", "fad", "gad", "rad", "bed", "led", "red", "wed",
		"fed", "ted", "zed", "kill", "mill", "till", "fill", "hill", "will", "sell", "tell", "bell", "fell", "dell", "yell", "doll", "dull",
		"hull", "mull", "null", "loom", "doom", "room", "boom", "zoom", "vex", "rex", "lex", "kex", "dex", "tom", "tim", "kim", "sam", "ron",
		"don", "dan", "ben", "mel", "sal", "lou", "lee", "rae", "yael", "kay", "fay", "ray", "may", "jay", "day", "say", "hay", "lay", "nay",
		"pay", "bay", "way", "gas", "bus", "yus", "pus", "thus", "this", "that", "than", "then", "them", "they", "theirs", "dear", "deer",
		"seer", "beer", "here", "hear", "near", "fear", "gear", "rear", "sear", "tear", "year", "lear", "kar", "car", "far", "bar", "tar", "jar",
		"mar", "par", "war", "ear", "eat", "sea", "see", "tea", "zea", "lea", "kea", "fee", "bee", "tee", "vee", "gee", "nee", "ree", "mee",
		"dee", "sheer", "shear", "shoe", "shoo", "shin", "ship", "shop", "shot", "shut", "shun", "shed", "shell", "shall", "shank", "lush",
		"rush", "mush", "gush", "bush", "hush", "push", "tush", "posh", "bosh", "kosh", "mosh", "josh", "dosh", "gash", "rash", "bash", "dash",
		"cash", "lash", "mash", "sash", "hash", "wash", "fish", "dish", "wish", "kish", "mesh", "fresh", "flesh", "math", "bath", "path", "oath",
		"goth", "sloth", "kith", "pith", "with", "myth", "lith", "faith", "teeth", "sooth", "booth", "tooth", "som", "gar", "les", "ein", "gem",
		"ash", "dos", "tex", "sor", "bur", "mur", "fi", "rel", "rol", "bom", "oth", "lul", "ral");

	/** The best-known runes' syllables, chosen by ear. Keys are rune paths. */
	private static final Map<String, String> TUNED = new LinkedHashMap<>();

	static {
		TUNED.put("self", "ir");
		TUNED.put("touch", "ta");
		TUNED.put("bolt", "vo");
		TUNED.put("beam", "lun");
		TUNED.put("burst", "kra");
		TUNED.put("zone", "zae");
		TUNED.put("domain", "dor");
		TUNED.put("heal", "mae");
		TUNED.put("harm", "ruk");
		TUNED.put("push", "fo");
		TUNED.put("pull", "ul");
		TUNED.put("fire", "ign");
		TUNED.put("frost", "hrim");
		TUNED.put("shock", "zar");
		TUNED.put("lightning", "thra");
		TUNED.put("shield", "vau");
		TUNED.put("blink", "phi");
		TUNED.put("explode", "brak");
		TUNED.put("wither", "nek");
		TUNED.put("light", "lis");
		TUNED.put("freeze", "isk");
		TUNED.put("meteor", "kael");
		TUNED.put("amplify", "mor");
		TUNED.put("split", "sei");
		TUNED.put("echo", "ek");
		TUNED.put("on_hit", "tai");
	}

	/** Every known rune's syllable, by id: made the first time one is asked for. */
	private static volatile Map<String, String> lexicon;

	/** A rune's syllable (lower case, two to five letters). */
	public static String syllable(String runeId) {
		String known = lexicon().get(runeId);
		return known != null ? known : made(path(runeId), new HashSet<>());
	}

	/** A spell's incantation: each rune's syllable, in casting order. */
	public static List<String> of(List<String> runeIds) {
		List<String> out = new ArrayList<>(runeIds.size());
		for (String id : runeIds) {
			out.add(syllable(id));
		}
		return out;
	}

	/** The incantation as one line, its syllables apart: "vo ign sei". */
	public static String line(List<String> runeIds) {
		return String.join(" ", of(runeIds));
	}

	private static Map<String, String> lexicon() {
		Map<String, String> built = lexicon;
		if (built == null) {
			synchronized (Incantation.class) {
				if (lexicon == null) {
					lexicon = build(Runes.all());
				}
				built = lexicon;
			}
		}
		return built;
	}

	/** Gives every rune of a roster its syllable, hand-tuned ones first, then the rest in order. */
	static Map<String, String> build(java.util.Collection<RuneDef> roster) {
		Map<String, String> out = new HashMap<>();
		Set<String> taken = new HashSet<>(TUNED.values());
		// A syllable is never a rune's own name ("hush" for Glaive would read as the Hush rune).
		for (RuneDef rune : roster) {
			taken.add(rune.path());
		}
		for (RuneDef rune : roster) {
			String tuned = TUNED.get(rune.path());
			if (tuned != null && rune.id().equals("wildercord:" + rune.path())) {
				out.put(rune.id(), tuned);
			}
		}
		for (RuneDef rune : roster) {
			if (!out.containsKey(rune.id())) {
				String made = made(rune.path(), taken);
				taken.add(made);
				out.put(rune.id(), made);
			}
		}
		return Map.copyOf(out);
	}

	/** The first sound {@code path} makes that isn't taken (after enough tries, the first it makes at all). */
	private static String made(String path, Set<String> taken) {
		String first = null;
		for (int salt = 0; salt < 4096; salt++) {
			String s = sound(path, salt);
			if (s == null) {
				continue;
			}
			if (first == null) {
				first = s;
			}
			if (!taken.contains(s)) {
				return s;
			}
		}
		return first == null ? "a" : first;
	}

	/** One try at a syllable for {@code path}: null when the pieces don't sit well together. */
	static String sound(String path, int salt) {
		long h = hash(path + "#" + salt);
		String onset = ONSETS[(int) Math.floorMod(h, (long) ONSETS.length)];
		h /= ONSETS.length;
		String vowel = VOWELS[(int) Math.floorMod(h, (long) VOWELS.length)];
		h /= VOWELS.length;
		String coda = CODAS[(int) Math.floorMod(h, (long) CODAS.length)];
		if (onset.isEmpty() && coda.isEmpty()) {
			return null;
		}
		if (vowel.length() > 1 && !SOFT_CODAS.contains(coda)) {
			return null;
		}
		if (CLUSTERS.contains(onset) && !(SOFT_CODAS.contains(coda) || coda.equals("x"))) {
			return null;
		}
		String s = onset + vowel + coda;
		if (s.length() > 5 || AVOID.contains(s)) {
			return null;
		}
		return s;
	}

	private static long hash(String text) {
		long h = 0xcbf29ce484222325L;
		for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
			h ^= b & 0xFF;
			h *= 0x100000001b3L;
		}
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		return h >>> 1;
	}

	private static String path(String id) {
		int colon = id.indexOf(':');
		return (colon >= 0 ? id.substring(colon + 1) : id).toLowerCase(Locale.ROOT);
	}
}
