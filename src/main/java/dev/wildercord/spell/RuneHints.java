package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Codex says about a rune not yet understood (see {@link RuneReading}): a short hint for an unread one, and its
 * text with the numbers veiled for a glimpsed one. The hints are made from the rune's own data (its element, family and
 * kind, and words picked out of its description), in a handful of turns of phrase so a row of them doesn't read the same,
 * and the runes everyone meets first have hints written by hand.
 */
public final class RuneHints {
	private RuneHints() {}

	/** Hints written by hand, for the runes most casters meet first, by path. */
	static final Map<String, String> WRITTEN = Map.ofEntries(
		Map.entry("self", "It points back at you. Whatever follows it happens to you."),
		Map.entry("touch", "It reaches no further than your hand, and asks to be laid on something."),
		Map.entry("bolt", "It wants to leave your hand fast and fly straight at something."),
		Map.entry("beam", "A line drawn from you to the first thing in your way, in no time at all."),
		Map.entry("burst", "It breathes out from where you stand, all around you at once."),
		Map.entry("zone", "It marks ground where you look, and the ground remembers for a while."),
		Map.entry("rain", "It looks up. Whatever you give it comes down from the sky."),
		Map.entry("domain", "It claims everything around you as its own, and holds it."),
		Map.entry("fire", "Warm to the touch, warmer the longer you hold it. It wants something to burn."),
		Map.entry("frost", "Frost gathers on it even in summer. It would slow the world down."),
		Map.entry("shock", "It prickles your fingers, and seems eager to jump to someone else."),
		Map.entry("lightning", "It tastes of the air before thunder. Something very loud sleeps in it."),
		Map.entry("heal", "It is warm, like a hand on a fevered brow. It wants to close wounds."),
		Map.entry("harm", "Plain and cold and sharp. It means to hurt, and to leave a mark."),
		Map.entry("push", "It leans away from you, as if it wants to shove the world back."),
		Map.entry("pull", "Small things roll toward it on a table. It wants to draw things in."),
		Map.entry("launch", "It feels lighter than air, and impatient. Something will leave the ground."),
		Map.entry("blink", "Look at it and it seems to be a little further off. It would take you elsewhere."),
		Map.entry("explode", "It is very still, the way a held breath is still. It will not stay that way."),
		Map.entry("freeze", "Colder than Frost, and quieter. It would make the moving stop."),
		Map.entry("meteor", "It is heavy and faintly hot, like a stone that fell from somewhere high."),
		Map.entry("gravity_well", "The dust on it drifts toward its middle. It wants everything near it."),
		Map.entry("summon", "Something in it is waiting to be called, and it is not alone."),
		Map.entry("wither", "It turns your fingers grey for a moment. Whatever it touches will not heal well."),
		Map.entry("sonic_boom", "Hold it to your ear and you hear nothing at all. That silence is waiting to break."),
		Map.entry("shield", "It feels like a door being shut. It would stand between someone and harm."),
		Map.entry("venom", "A green smell, too sweet. It wants into the blood."),
		Map.entry("root", "It grips your palm like a root grips soil, and does not want to let go."),
		Map.entry("levitate", "It floats a hair above your hand when you open it."),
		Map.entry("soar", "It tugs upward, always upward, like a kite on a string."),
		Map.entry("stasis", "It ticks, then doesn't. It would hold a moment still."),
		Map.entry("feather_fall", "It is lighter than a leaf. Whatever falls with it comes down softly."),
		Map.entry("swift", "It is never quite still in your hand. It would make someone quicker on their feet."),
		Map.entry("cleanse", "It smells of rain on stone. It would wash something away."),
		Map.entry("prolong", "It feels like a held breath. Whatever good is already on someone, it would make it stay."),
		Map.entry("amplify", "On its own it does nothing. Beside the right rune, it makes that rune more."),
		Map.entry("extend", "On its own it does nothing. Beside the right rune, it makes that rune last."),
		Map.entry("widen", "On its own it does nothing. Beside the right rune, it makes that rune reach further."),
		Map.entry("split", "It is cracked into three, and each piece hums the same note."),
		Map.entry("echo", "It hums back whatever you hum at it, a moment late."),
		Map.entry("on_hit", "It waits. What follows it happens where something is struck."),
		Map.entry("delay", "It waits. What follows it happens after a breath."),
		Map.entry("on_kill", "It waits. What follows it happens where a life ends."),
		// The newer working runes, so they read apart from one another.
		Map.entry("dew_drink", "It is always a little damp. Rain would feed whoever carries it."),
		Map.entry("lore_reading", "It squints at whatever you hold and counts what is written on it."),
		Map.entry("beacon_swell", "It hums near a beacon, and the beacon's light leans further out."),
		Map.entry("sow", "Seeds rattle in it. It wants to put them in the ground for you."),
		Map.entry("plankway", "It smells of sawdust and wants to lay a road across the gap."),
		Map.entry("polish", "It is smooth as river stone, and would make rough stone smooth too."),
		Map.entry("pitfloor", "It would lay mud over empty air, though not for long."),
		Map.entry("steady_brush", "It has an archaeologist's patience, and gentle bristles."),
		Map.entry("searing_edge", "It wants to sit on a blade and make every cut burn."),
		Map.entry("lavaseal", "Lava cools wherever it points, hardening into stone."),
		Map.entry("lamplighter", "A wick that lights other wicks: candles and campfires wake near it."),
		Map.entry("restock", "It tugs at a chest to fill your hand from it."),
		Map.entry("gravefinder", "It remembers where you fell, and would lead you back there."),
		Map.entry("lostfind", "It glances about for anything dropped and left behind."),
		Map.entry("tide_lantern", "A small light that does not mind being under water."),
		Map.entry("courtship", "It makes the animals of the farm think kindly of each other."),
		Map.entry("sentry", "It keeps watch, and makes the monsters near its mark show themselves."),
		Map.entry("soothe", "It cools a temper that was never meant for you."),
		Map.entry("bobber_bell", "A tiny bell rings in it whenever something tugs a line."),
		Map.entry("shoal_herd", "Fish turn toward it as if it were scattered bread."),
		Map.entry("brimming", "It is heavy with water, and cauldrons near it fill to the rim."),
		Map.entry("dewcatch", "Empty bottles feel fuller just sitting next to it."),
		Map.entry("herdsense", "It counts heads in a field and tells you who is ready."),
		Map.entry("stocktake", "It peers into nearby chests and sums up what is piled there."),
		Map.entry("lapis_thrift", "It pinches lapis at the enchanting table and gives some back."),
		Map.entry("orbcall", "Loose experience drifts toward it like moths to a lamp."),
		Map.entry("nightwatch", "It stays awake so you do not have to, and rings when something hunts you."),
		Map.entry("managift", "It pours out of you into another caster's well."),
		Map.entry("water_reading", "It reads the water under your line and says whether it is open."),
		Map.entry("stand_pose", "It would teach a stand of armour to strike a new pose."),
		Map.entry("sanctuary", "It draws a quiet circle that monsters will not be born in."),
		Map.entry("blockpack", "It packs nine of a thing tightly into one."),
		Map.entry("tilth", "It loosens stubborn earth into plain soil, as a hoe would."),
		Map.entry("orepluck", "It picks the ores out of the walls that show their faces."),
		Map.entry("siftfall", "Loose sand and gravel above it lose their grip."),
		Map.entry("millstone", "It grinds stone smaller, one step at a time."),
		Map.entry("village_sense", "It listens for bells and chatter, and points toward the nearest village."),
		Map.entry("arrowveil", "Arrows lose heart near it and fall out of the air."),
		Map.entry("wayfarer_hymn", "A walking song: everyone near you steps lighter until someone strikes."),
		Map.entry("spawn_bearing", "It always leans toward where the world began."),
		Map.entry("spook", "It makes your enemies remember somewhere else to be."),
		Map.entry("stronghold_compass", "It points toward the eye of the world, but will not say how far."),
		Map.entry("packtidy", "It folds your pack and joins what belongs together."),
		Map.entry("stewpot", "It smells of supper, and wants bowls and mushrooms."),
		Map.entry("gold_parley", "Piglins hear gold in it and forget why they were angry."),
		Map.entry("feastday", "It tastes of harvest bread, and there is enough for everyone near."),
		Map.entry("shore_sense", "It leans toward dry land when you are lost at sea."),
		Map.entry("lux_reading", "It weighs the light where it lands and warns if monsters could walk there."),
		Map.entry("folk_census", "It counts the villagers near you and what each of them does."),
		Map.entry("haggle", "It makes traders feel generous for a little while."));

	/** How a rune of each element feels in the hand. */
	static final Map<String, List<String>> FEEL = Map.ofEntries(
		Map.entry("fire", List.of("Heat sleeps in this stone", "It smells faintly of smoke", "Something in it is always smouldering", "It is warm, and getting warmer")),
		Map.entry("frost", List.of("Your breath mists over it", "It is cold enough to ache", "Rime creeps over it when you look away", "It sounds like ice on a pond")),
		Map.entry("storm", List.of("The hair on your arm lifts near it", "A faint crackle lives inside it", "It tastes of the air before thunder", "It stings, very slightly")),
		Map.entry("wind", List.of("It is never quite still in your hand", "It whistles when you turn it", "It is lighter than stone should be", "A draught comes off it")),
		Map.entry("earth", List.of("It is heavier than it looks", "It smells of turned soil", "Grit clings to it", "It sits in the hand like a stone that wants to stay put")),
		Map.entry("life", List.of("It is warm, like something sleeping", "A green smell comes off it", "It beats, faintly, like a pulse", "Something in it wants to grow")),
		Map.entry("void", List.of("Light slides off it", "It is quieter around this stone", "Its edges go soft if you stare", "It feels like a hole you could fall into")),
		Map.entry("arcane", List.of("It glitters when you aren't looking", "Faint script swims under its surface", "It rings softly, like struck glass", "It is full of patient light")),
		Map.entry("time", List.of("It ticks, if you listen", "It feels older than it should", "The sand in it never settles", "It feels like a held breath")),
		Map.entry("blood", List.of("It is warm and faintly red", "It throbs in time with your heart", "It smells of iron", "It wants something you would rather keep")));

	/**
	 * Words in a description, and what an unread rune's hint says they suggest. Read in order; the first two that fit
	 * are used, and the words a rule matched are taken out before the next is read (so "can't heal" festers, and
	 * never mends).
	 */
	private static final List<String[]> SUGGESTS = List.of(
		new String[] {"no jump\\w*|can't jump", ""},
		new String[] {"teleport\\w*|swaps? places|to where the spell landed|sweeps you|carries you", "it would carry you somewhere else"},
		new String[] {"shakes? off [^.;]*|washes away [^.;]*|\\bcures?\\b", "it washes something away"},
		new String[] {"\\bstrips? (?:away |off )?(?:its |the |their |all )?(?:good|absorption|invisib|effects|wards|armou?r)\\w*|swallows? (?:each|its|the)[^.,;]*|\\bsteals? [^.,;]*",
			"it tears away what protects"},
		new String[] {"wolves|spirits?\\b|summon\\w*|fight at your side", "it does not come alone"},
		new String[] {"can't heal|cannot heal|won't close|no healing|poison\\w*|wither\\w*|\\brot\\w*", "something in it festers"},
		new String[] {"\\bheal(?:s|ed|ing)?\\b|restores?|regenerat\\w*|\\bmend\\w*", "it wants to mend"},
		new String[] {"\\bshields?\\b|absorption|resistance|\\bwards?\\b|protect\\w*|immune|no knockback", "it means to guard someone"},
		new String[] {"freez\\w*|frozen|can't move|in place", "it would hold things still"},
		new String[] {"sets? alight|\\bburn\\w*|ignit\\w*|fire damage", "it catches, and spreads"},
		new String[] {"lightning|\\barcs?\\b|jumps? to|leaps? to", "it leaps from one thing to the next"},
		new String[] {"bleed\\w*|wound\\w*", "it opens wounds"},
		new String[] {"\\bblind\\w*|darkness|\\bdim\\b|shadow\\w*", "it brings the dark"},
		new String[] {"invisib\\w*|unseen|lose track", "it hides"},
		new String[] {"\\bpush\\w*|hurl\\w*|knock\\w*|shove\\w*|fling\\w*|\\bthrow\\w*", "it shoves"},
		new String[] {"\\bpull\\w*|\\bdrag\\w*|\\bdraws?\\b|reels?\\b", "it draws things in"},
		new String[] {"\\bslow\\w*", "it drags at whatever it touches"},
		new String[] {"\\bspeed\\b|faster|\\bhaste\\b|quick\\w*", "it hurries"},
		new String[] {"\\bjump\\w*|\\bleap\\w*|float\\w*|levitat\\w*|fall damage|into the air|\\bfly\\b", "it wants you off the ground"},
		new String[] {"\\bglow\\w*|\\blight source|\\breveal\\w*|night vision", "it brings things into the light"},
		new String[] {"(?<!next )\\btime\\b|clock|rewind|longer, up to", "it meddles with the hour"},
		new String[] {"\\bmark\\w*|exposed|brittle|soaked|\\bbrand\\w*", "it leaves something behind on what it touches"},
		new String[] {"(?<![-\\d])\\bblock\\b|\\bmines?\\b|\\bores?\\b|plants?\\b|crops?|trees?\\b|bone-meal|\\bsoil", "it speaks to stone and soil"},
		new String[] {"damage", "it means harm"});

	private static final Map<EffectKind, String> KIND = Map.of(
		EffectKind.HARMFUL, "it means harm",
		EffectKind.HELPFUL, "it would help someone",
		EffectKind.WORLD, "it works on the world itself",
		EffectKind.MOVEMENT, "it would move you",
		EffectKind.NONE, "it is waiting to be used");

	/** Where a shape sends a spell, from words in its description. */
	private static final List<String[]> SHAPE_SUGGESTS = List.of(
		new String[] {"\\bwall\\b|across where", "it stands up between you and the world"},
		new String[] {"circle you|around you|your footsteps", "it keeps close around you"},
		new String[] {"\\bflies|\\bfires\\b|\\blobs\\b|darts|drifts|\\bflash|chases|bounces|\\bball\\b|\\borb\\b|latches", "it wants to leave your hand and travel"},
		new String[] {"forward|ahead|\\brolls\\b|sweeps", "it goes out ahead of you"},
		new String[] {"\\bline\\b|\\bbeam\\b|\\bray\\b|\\blance\\b|\\bstream\\b", "it reaches out in a straight line"},
		new String[] {"\\bsky\\b|from above|strikes from", "it calls something down from above"},
		new String[] {"where you look|at the point|where you stand", "it goes where your eyes go"},
		new String[] {"within|from you|\\baround\\b", "it spreads out from where you stand"},
		new String[] {"\\byou\\b", "it is about you"});

	/** What a modifier's needed trait suggests it changes. */
	private static final Map<String, String> TRAIT_WORDS = Map.ofEntries(
		Map.entry(Trait.POWER, "strength"), Map.entry(Trait.DURATION, "how long things last"), Map.entry(Trait.RADIUS, "reach"),
		Map.entry(Trait.SPEED, "swiftness"), Map.entry(Trait.PIERCE, "passing through"), Map.entry(Trait.BOUNCE, "rebounding"),
		Map.entry(Trait.SPLIT, "one becoming many"), Map.entry(Trait.HOMING, "seeking"), Map.entry(Trait.CHAIN, "leaping between"),
		Map.entry(Trait.FRUGAL, "thrift"), Map.entry(Trait.LINGER, "coming back"), Map.entry(Trait.VOLLEY, "doing it again"),
		Map.entry(Trait.COOLDOWN, "the patience of the whole spell"), Map.entry(Trait.SHARE, "sharing"), Map.entry(Trait.CIRCLE, "the circle itself"));

	/** When a link lets the rest of the spell go, from words in its description. */
	private static final List<String[]> LINK_SUGGESTS = List.of(
		new String[] {"\\bstored?\\b|\\bimbue\\w*|\\bkeeps?\\b", "later, when something sets it off"},
		new String[] {"every third|third cast", "only now and then"},
		new String[] {"\\bthree times|\\btimes\\b|\\bevery\\b", "over and over"},
		new String[] {"\\bagain\\b|repeat\\w*", "a second time"},
		new String[] {"reaction", "when elements clash"},
		new String[] {"\\bweak", "where a foe is weak"},
		new String[] {"\\bkill\\w*|slain|\\bdies\\b", "where a life ends"},
		new String[] {"low health|half health|health drops|wounded", "when you are close to falling"},
		new String[] {"\\bhurts? you|take damage", "when you are hurt"},
		new String[] {"\\blands?\\b|ground", "when you touch the ground"},
		new String[] {"\\bsneak\\w*", "while you crouch"},
		new String[] {"airborne|in the air", "while you are in the air"},
		new String[] {"\\bwet\\b|water|\\brain\\b", "only in water or rain"},
		new String[] {"outnumber\\w*|or more enemies", "when you stand against many"},
		new String[] {"\\bhit\\b|strikes|struck", "where something is struck"},
		new String[] {"later|second", "after a breath"});

	/** Hints already made, by rune id: the Codex asks every frame a tooltip is open. */
	private static final Map<String, String> MADE = new java.util.concurrent.ConcurrentHashMap<>();

	/** The hint an unread rune shows in place of its text. */
	public static String hint(RuneDef rune) {
		return MADE.computeIfAbsent(rune.id() + "|" + rune.description(), key -> make(rune));
	}

	private static String make(RuneDef rune) {
		String written = rune.id().startsWith("wildercord:") ? WRITTEN.get(rune.path()) : null;
		if (written != null) {
			return written;
		}
		int turn = Math.floorMod(rune.id().hashCode(), 4);
		String text = rune.description().toLowerCase(Locale.ROOT);
		String hint = switch (rune.family()) {
			case EFFECT -> effect(rune, text, turn);
			case SHAPE -> shape(text, turn);
			case MODIFIER -> modifier(rune, turn);
			case LINK -> link(text, turn);
			case KNOT -> "A whole spell tied into one rune; its runes are listed beside it.";
		};
		if (Runes.innate(rune)) {
			return "It woke in your heart, and will only tell you what it is by being used. " + hint;
		}
		if (Runes.fused(rune) && !WovenRunes.isWoven(rune)) {
			return "Two magics run together in it. " + hint;
		}
		return hint;
	}

	private static String effect(RuneDef rune, String text, int turn) {
		List<String> feels = FEEL.getOrDefault(rune.element(), FEEL.get("arcane"));
		String feel = feels.get(Math.floorMod(rune.id().hashCode() >> 3, feels.size()));
		List<String> clauses = new ArrayList<>(suggestions(SUGGESTS, text, 3));
		if (rune.kind() != EffectKind.HARMFUL) {
			// A helpful rune's damage is what it turns away or hands back: it doesn't "mean harm".
			clauses.remove(KIND.get(EffectKind.HARMFUL));
		}
		if (clauses.size() > 2) {
			clauses = clauses.subList(0, 2);
		}
		if (clauses.isEmpty()) {
			clauses = List.of(KIND.get(rune.kind()));
		}
		String what = clauses.size() > 1 ? clauses.get(0) + ", and " + clauses.get(1) : clauses.get(0);
		return switch (turn) {
			case 0 -> feel + ". Cast it and see: " + what + ".";
			case 1 -> feel + ", and " + what + ".";
			case 2 -> feel + ". All you can tell is that " + what + ".";
			default -> feel + ". You have a feeling " + what + ".";
		};
	}

	private static String shape(String text, int turn) {
		List<String> where = suggestions(SHAPE_SUGGESTS, text, 1);
		String clause = where.isEmpty() ? "it decides where a spell goes" : where.get(0);
		return switch (turn) {
			case 0 -> "A shape, not a magic: " + clause + ". Whatever you thread after it rides along.";
			case 1 -> "It decides where, never what: " + clause + ".";
			case 2 -> capital(clause) + ". Thread something after it and find out the rest.";
			default -> "It carries magic rather than making it; " + clause + ".";
		};
	}

	private static String modifier(RuneDef rune, int turn) {
		String word = TRAIT_WORDS.getOrDefault(rune.needs(), "something about its neighbour");
		return switch (turn) {
			case 0 -> "It changes the rune before it: something to do with " + word + ".";
			case 1 -> "It leans on its neighbour to the left. " + capital(word) + ", perhaps.";
			case 2 -> "On its own it does nothing; beside the right rune, it has to do with " + word + ".";
			default -> "It reaches back along the cord for a rune to change, and it has to do with " + word + ".";
		};
	}

	private static String link(String text, int turn) {
		List<String> when = suggestions(LINK_SUGGESTS, text, 1);
		String clause = when.isEmpty() ? "when the moment is right" : when.get(0);
		return switch (turn) {
			case 0 -> "It waits. The rest of the spell goes off " + clause + ".";
			case 1 -> "A hinge in the spell: what follows comes " + clause + ".";
			case 2 -> "It holds the rest of the spell back, and lets it go " + clause + ".";
			default -> "Everything after it waits, until it doesn't: " + clause + ".";
		};
	}

	private static List<String> suggestions(List<String[]> table, String text, int most) {
		List<String> out = new ArrayList<>();
		String left = text;
		for (String[] row : table) {
			if (out.size() >= most) {
				break;
			}
			Matcher matcher = Pattern.compile(row[0]).matcher(left);
			if (matcher.find()) {
				// A rule with nothing to say only spends its words: "no jumping" is not a leap.
				if (!row[1].isEmpty() && !out.contains(row[1])) {
					out.add(row[1]);
				}
				// What a rule read is spent: "can't heal" festers, and the "heal" in it never mends.
				left = matcher.replaceAll(" ");
			}
		}
		return out;
	}

	private static String capital(String text) {
		return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	// ------------------------------------------------------------------ a glimpsed rune: numbers veiled

	/** A piece of a glimpsed rune's text: words as they are, or a number the caster hasn't made out yet. */
	public record Piece(String text, boolean veiled) {}

	/** Numbers (with their percent signs and "x"s) and the larger Roman numerals, as a glimpse veils them. */
	private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?%?x?|\\b(?:II|III|IV|VI?)\\b");

	/** A rune's text cut into pieces, every number in it veiled: what a glimpsed rune shows. */
	public static List<Piece> glimpse(String description) {
		List<Piece> pieces = new ArrayList<>();
		Matcher matcher = NUMBER.matcher(description);
		int at = 0;
		while (matcher.find()) {
			if (matcher.start() > at) {
				pieces.add(new Piece(description.substring(at, matcher.start()), false));
			}
			pieces.add(new Piece(matcher.group(), true));
			at = matcher.end();
		}
		if (at < description.length()) {
			pieces.add(new Piece(description.substring(at), false));
		}
		return List.copyOf(pieces);
	}

	/** A glimpse as plain text, each veiled number shown as {@code veil} (for searching, and the tests). */
	public static String glimpseText(String description, String veil) {
		StringBuilder out = new StringBuilder();
		for (Piece piece : glimpse(description)) {
			out.append(piece.veiled() ? veil : piece.text());
		}
		return out.toString();
	}
}
