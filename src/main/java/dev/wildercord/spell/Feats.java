package dev.wildercord.spell;

import java.util.List;

/**
 * The Grimoire: everything a caster has discovered. Entries are plain string keys, grouped by
 * a prefix: {@code reaction:shatter}, {@code secret:sunfall}, {@code feat:overcast}, {@code fusion:firestorm}. Each first
 * discovery condenses a little mana toward the next Heart Circle. Pure data, shared by the
 * server (which grants entries) and the Grimoire page (which lists them).
 */
public final class Feats {
	private Feats() {}

	public static final String LONG_SPELL_KILL = "long_spell_kill";
	public static final String OVERCAST = "overcast";
	public static final String RHYTHM = "rhythm";
	public static final String UNISON = "unison";
	public static final String CLASH = "clash";
	public static final String COLLISION = "collision";
	public static final String RUNEBOUND = "runebound";
	public static final String ARCHIVIST = "archivist";
	public static final String LEY_LINE = "ley_line";
	public static final String WELLSTONE = "wellstone";
	public static final String SEAL = "seal";
	public static final String LEANING = "leaning";
	public static final String INNATE = "innate";
	public static final String SCROLL = "scroll";
	public static final String CHARGED = "charged";
	public static final String MIRROR = "mirror";
	public static final String SHIELDBREAKER = "shieldbreaker";
	public static final String SPELLGUARD = "spellguard";
	public static final String IMBUE = "imbue";
	public static final String CONDUCTOR = "conductor";
	public static final String ICEBRIDGE = "icebridge";
	public static final String PARRY = "parry";
	public static final String WILD_SURGE = "wild_surge";
	// World events (see cast.events).
	public static final String STORMCALLER = "stormcaller";
	public static final String STARGAZER = "stargazer";
	public static final String RIFTWARDEN = "riftwarden";
	public static final String CHORUS = "chorus";
	public static final String KINDRED = "kindred";
	public static final String MENAGERIE = "menagerie";
	public static final String UPGRADE = "upgrade";
	public static final String COMBINE = "combine";
	public static final String KNOT = "knot";

	/** A feat in the Grimoire: its id, its title and how it was earned. */
	public record Feat(String id, String name, String description) {
		public String key() {
			return "feat:" + id;
		}
	}

	public static final List<Feat> FEATS = List.of(
		new Feat(CHARGED, "Full Charge", "Released a fully charged spell."),
		new Feat(LONG_SPELL_KILL, "Long Incantation", "Slew a monster with a spell of six runes or more."),
		new Feat(RHYTHM, "In Rhythm", "Chained three casts on the beat."),
		new Feat(OVERCAST, "Overcast", "Cracked a Heart Circle to cast beyond your mana."),
		new Feat(COLLISION, "Spell Collision", "Shot a spell out of the air with your own."),
		new Feat(UNISON, "Unison", "Struck the same foe as another caster, with another element, at the same moment."),
		new Feat(CLASH, "Domain Clash", "Shattered another caster's Domain with your own."),
		new Feat(RUNEBOUND, "Runebreaker", "Slew a Runebound, a monster that casts spells."),
		new Feat(MIRROR, "Mirrorfrost", "Turned an enemy's own spell back on them."),
		new Feat(LEY_LINE, "Ley Walker", "Stood on a ley line, where the world's mana runs close to the surface."),
		new Feat(WELLSTONE, "Wellkeeper", "Woke a Wellstone on a ley line."),
		new Feat(SEAL, "Sealbreaker", "Opened a Rune Seal in the Archive."),
		new Feat(ARCHIVIST, "The Last Page", "Defeated the Archivist."),
		new Feat(LEANING, "Leaning", "Cast one element so often that your magic leans toward it."),
		new Feat(INNATE, "Awakening", "Awakened your innate rune at the 1st Circle."),
		new Feat(SCROLL, "Scribe", "Inscribed a spell onto a scroll."),
		new Feat(SPELLGUARD, "Spellguard", "Your Shield stopped a spell cast at you."),
		new Feat(SHIELDBREAKER, "Shieldbreaker", "Shattered a Shield with a stronger spell."),
		new Feat(IMBUE, "Imbuer", "Imbued a spell into an item or a block."),
		new Feat(CONDUCTOR, "Conductor", "Shocked five creatures at once through the water they stood in."),
		new Feat(ICEBRIDGE, "Icebridge", "Walked across water you had frozen with a spell."),
		new Feat(PARRY, "Parry", "Raised a Shield at the last moment and turned a spell back on its caster."),
		new Feat(WILD_SURGE, "Wild Magic", "Overcast a spell and watched it twist into something else."),
		new Feat(STORMCALLER, "Stormcaller", "Cast 20 spells under a mana storm."),
		new Feat(STARGAZER, "Stargazer", "Looted a Fallen Star."),
		new Feat(RIFTWARDEN, "Riftwarden", "Closed a rift."),
		new Feat(CHORUS, "Chorus", "Cast the same spell as other casters at the same moment, and sang one great spell together."),
		new Feat(KINDRED, "Kindred", "Bonded with a wisp: your first familiar."),
		new Feat(MENAGERIE, "Menagerie", "Bonded with a wisp of every element."),
		new Feat(UPGRADE, "Honed", "Ranked up a rune at the Fusion Altar."),
		new Feat(COMBINE, "Fusion", "Fused two effects into a new one at the Fusion Altar."),
		new Feat(KNOT, "Knotted", "Tied a whole spell into one rune at the Fusion Altar."));

	/** The five element reactions, in the order the Grimoire lists them. */
	public static final List<String> REACTIONS = List.of("shatter", "conduct", "wildfire", "implode", "collapse");

	public static String reactionKey(String reaction) {
		return "reaction:" + reaction;
	}

	/** Mana condensed toward the next circle by a first discovery. */
	public static int reward(String key) {
		if (key.startsWith("secret:")) {
			return 400;
		}
		if (key.startsWith("reaction:") || key.startsWith(Fusions.KEY_PREFIX)) {
			return 150;
		}
		if (key.equals("feat:" + ARCHIVIST)) {
			return 2000;
		}
		return 250;
	}

	public static Feat feat(String id) {
		for (Feat feat : FEATS) {
			if (feat.id().equals(id)) {
				return feat;
			}
		}
		return new Feat(id, id, "");
	}

	/** Every entry a full Grimoire holds: each feat, reaction and secret spell (riddles don't count). */
	public static List<String> everyEntry() {
		java.util.List<String> keys = new java.util.ArrayList<>();
		for (Feat feat : FEATS) {
			keys.add(feat.key());
		}
		for (String reaction : REACTIONS) {
			keys.add(reactionKey(reaction));
		}
		for (Secrets.Secret secret : Secrets.ALL) {
			keys.add(secret.key());
		}
		return List.copyOf(keys);
	}

	/** Whether a Grimoire holds every entry with this prefix ({@code ""}: the whole Grimoire). */
	public static boolean complete(java.util.Collection<String> entries, String prefix) {
		for (String key : everyEntry()) {
			if (key.startsWith(prefix) && !entries.contains(key)) {
				return false;
			}
		}
		return true;
	}

	/** How many entries a Grimoire holds with this prefix, e.g. {@code "reaction:"}. */
	public static int count(java.util.Collection<String> entries, String prefix) {
		int n = 0;
		for (String entry : entries) {
			if (entry.startsWith(prefix)) {
				n++;
			}
		}
		return n;
	}
}
