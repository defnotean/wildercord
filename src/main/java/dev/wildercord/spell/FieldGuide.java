package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The Grimoire's field guide: the creatures of the world worth knowing about, and which of them a player has come
 * across. Seeing one up close writes a Grimoire key ({@code creature:wildercord:lumen_stag}) and the page lists every
 * creature met with its short entry, and the rest as a hint of where to look. It's one list for every kind of
 * creature, wildlife and monsters alike: a new creature joins with {@link #add}. Pure, shared by the server (which
 * writes the keys) and the Grimoire page (which reads them).
 *
 * <p>Text comes from the language file: an entry's name is its entity's ({@code entity.wildercord.lumen_stag}), its
 * entry {@code guide.wildercord.lumen_stag} and its hint {@code guide.wildercord.lumen_stag.hint}.</p>
 */
public final class FieldGuide {
	private FieldGuide() {}

	public static final String PREFIX = "creature:";
	/** Mana condensed toward the next Heart Circle by each creature met for the first time: a little, for getting out there. */
	public static final int REWARD = 40;

	/** Which part of the guide a creature is listed in. */
	public enum Group { WILDLIFE, MONSTER }

	/**
	 * One creature in the guide.
	 *
	 * @param type  its entity type id ({@code wildercord:lumen_stag})
	 * @param group which part of the guide it's in
	 * @param color the colour its name is written in (0xRRGGBB)
	 */
	public record Entry(String type, Group group, int color) {
		private String namespace() {
			int colon = type.indexOf(':');
			return colon < 0 ? "minecraft" : type.substring(0, colon);
		}

		private String path() {
			return type.substring(type.indexOf(':') + 1);
		}

		/** Its short entry, once met. */
		public String noteKey() {
			return "guide." + namespace() + "." + path();
		}

		/** Where to look for it, before it's met. */
		public String hintKey() {
			return noteKey() + ".hint";
		}

		/** Its Grimoire key. */
		public String key() {
			return FieldGuide.key(type);
		}
	}

	private static final List<Entry> ENTRIES = new ArrayList<>(List.of(
		new Entry("wildercord:glimmerwing", Group.WILDLIFE, 0xA8D8FF),
		new Entry("wildercord:lumen_stag", Group.WILDLIFE, 0xC8ECFF),
		new Entry("wildercord:mossback_tortoise", Group.WILDLIFE, 0x8CC866),
		new Entry("wildercord:cinderfox", Group.WILDLIFE, 0xFF8A3A),
		new Entry("wildercord:skyray", Group.WILDLIFE, 0x7FB0F0),
		new Entry("wildercord:rimehare", Group.WILDLIFE, 0xDDF4FF),
		new Entry("wildercord:bramblewalker", Group.MONSTER, 0x6FA84A),
		new Entry("wildercord:gloomstalker", Group.MONSTER, 0x8A6AD8),
		new Entry("wildercord:thunderwing_harpy", Group.MONSTER, 0xE8D86A),
		new Entry("wildercord:geode_crawler", Group.MONSTER, 0xB89AF0),
		new Entry("wildercord:bog_witch_frog", Group.MONSTER, 0x7AB05A),
		new Entry("wildercord:mana_ooze", Group.MONSTER, 0xB070F0)));

	/** Adds a creature to the guide (a later batch of creatures, or an add-on). A type already listed is left as it is. */
	public static synchronized void add(Entry entry) {
		if (byType(entry.type()).isEmpty()) {
			ENTRIES.add(entry);
		}
	}

	/** Every creature in the guide, in the order they were added. */
	public static synchronized List<Entry> all() {
		return Collections.unmodifiableList(new ArrayList<>(ENTRIES));
	}

	/** The creatures of one group, in order. */
	public static List<Entry> group(Group group) {
		return all().stream().filter(e -> e.group() == group).toList();
	}

	public static synchronized Optional<Entry> byType(String type) {
		for (Entry entry : ENTRIES) {
			if (entry.type().equals(type)) {
				return Optional.of(entry);
			}
		}
		return Optional.empty();
	}

	/** The Grimoire key for having met a kind of creature. */
	public static String key(String type) {
		return PREFIX + type;
	}

	public static boolean isKey(String key) {
		return key != null && key.startsWith(PREFIX) && key.length() > PREFIX.length();
	}

	/** The creature a key names, if it's a guide key for a creature in the guide. */
	public static Optional<Entry> entry(String key) {
		return isKey(key) ? byType(key.substring(PREFIX.length())) : Optional.empty();
	}

	/** Every creature in the guide a player has met, in the order they met them. */
	public static List<Entry> met(Collection<String> grimoire) {
		List<Entry> met = new ArrayList<>();
		for (String key : grimoire) {
			entry(key).filter(e -> !met.contains(e)).ifPresent(met::add);
		}
		return met;
	}
}
