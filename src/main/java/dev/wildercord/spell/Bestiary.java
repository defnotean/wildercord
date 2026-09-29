package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The Grimoire's Bestiary: the kinds of creature a caster has struck with a spell, and what they've
 * learned of each one's affinities (see {@link Affinity}). Entries are Grimoire keys like the rest:
 * {@code bestiary:minecraft:blaze} for a creature met, and
 * {@code bestiary:minecraft:blaze|weak|frost} for a weakness found. Pure, shared by the server (which
 * writes them) and the Grimoire page (which lists them).
 */
public final class Bestiary {
	private Bestiary() {}

	public static final String PREFIX = "bestiary:";
	/** Mana condensed toward the next Heart Circle by each weakness found (resistances teach nothing to build on). */
	public static final int WEAKNESS_REWARD = 25;

	/** What a Bestiary entry records about a creature. */
	public enum Kind {
		MET("met"), WEAK("weak"), RESISTS("resists"), IMMUNE("immune");

		public final String id;

		Kind(String id) {
			this.id = id;
		}

		static Optional<Kind> byId(String id) {
			for (Kind kind : values()) {
				if (kind.id.equals(id)) {
					return Optional.of(kind);
				}
			}
			return Optional.empty();
		}
	}

	/** One entry: the creature's type id, what was learned and of which element (empty for {@link Kind#MET}). */
	public record Entry(String type, Kind kind, String element) {}

	/** The key for having met a kind of creature, by its type id ({@code minecraft:blaze}). */
	public static String metKey(String type) {
		return PREFIX + type;
	}

	/** The key for having found one of a creature's affinities. */
	public static String key(String type, Kind kind, String element) {
		return kind == Kind.MET ? metKey(type) : PREFIX + type + "|" + kind.id + "|" + element;
	}

	/** Reads a Bestiary key back, or empty for any other key (or a malformed one). */
	public static Optional<Entry> parse(String key) {
		if (key == null || !key.startsWith(PREFIX) || key.length() == PREFIX.length()) {
			return Optional.empty();
		}
		String rest = key.substring(PREFIX.length());
		String[] parts = rest.split("\\|", -1);
		if (parts.length == 1) {
			return Optional.of(new Entry(rest, Kind.MET, ""));
		}
		if (parts.length != 3 || parts[0].isEmpty() || parts[2].isEmpty()) {
			return Optional.empty();
		}
		return Kind.byId(parts[1]).filter(kind -> kind != Kind.MET).map(kind -> new Entry(parts[0], kind, parts[2]));
	}

	/** Every kind of creature met, in the order they were met. */
	public static List<String> met(Collection<String> grimoire) {
		List<String> types = new ArrayList<>();
		for (String key : grimoire) {
			parse(key).filter(e -> e.kind() == Kind.MET && !types.contains(e.type())).ifPresent(e -> types.add(e.type()));
		}
		return types;
	}

	/** The elements found for one creature and kind of affinity, in {@link Affinity#ELEMENTS} order. */
	public static List<String> known(Collection<String> grimoire, String type, Kind kind) {
		List<String> elements = new ArrayList<>();
		for (String element : Affinity.ELEMENTS) {
			if (grimoire.contains(key(type, kind, element))) {
				elements.add(element);
			}
		}
		return elements;
	}

	/** Mana a first Bestiary entry condenses: a weakness found is worth a little, the rest nothing. */
	public static int reward(String key) {
		return parse(key).map(e -> e.kind() == Kind.WEAK ? WEAKNESS_REWARD : 0).orElse(0);
	}

	/**
	 * Whether an entry is written without a toast or a sound: meeting a creature and finding what it
	 * resists (the callout over it already said so). Only a weakness found is announced.
	 */
	public static boolean quiet(String key) {
		return parse(key).map(e -> e.kind() != Kind.WEAK).orElse(false);
	}
}
