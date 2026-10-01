package dev.wildercord.world;

import java.util.Locale;
import java.util.Optional;

/**
 * Residues: big magic leaves a lasting mark of its element where it lands. A strong spell (one whose
 * price reaches the server's threshold), an overcast, a boss's spell or now and then an element
 * reaction leaves storm-glass where lightning struck, everfrost that holds for a day, smouldering ash,
 * strange flowers, a scar of void that slowly closes... Each mark is a real block that fades back into
 * the ground on its own, and gives a reagent when it's harvested.
 *
 * <p>Pure (no Minecraft types), so the thresholds, sizes, lifetimes and where a mark may go are
 * unit-tested; {@code cast.Residues} finds the ground, asks permission and keeps the record, and
 * {@link ResidueLedger} is the record itself.</p>
 */
public final class ResidueRules {
	private ResidueRules() {}

	/** How a residue meets the ground. */
	public enum Placement {
		/** It takes the place of the natural block at the surface (everfrost, a void scar), and gives it back when it fades. */
		COVER,
		/** It lies in the air just above natural ground (ash, a flower, a glass cluster), and leaves nothing when it fades. */
		REST
	}

	/**
	 * One element's mark: its block, how it meets the ground, how long it lasts at strength 1 (in ticks; a
	 * day is 24000), and the reagent it gives.
	 */
	public enum Kind {
		SMOULDERING_ASH("fire", "smouldering_ash", Placement.REST, 12000, "cinder_ash"),
		EVERFROST("frost", "everfrost", Placement.COVER, 24000, "everfrost_shard"),
		FULGURITE("storm", "fulgurite", Placement.REST, 36000, "fulgurite_shard"),
		LINGERING_EDDY("wind", "lingering_eddy", Placement.REST, 6000, "bottled_gale"),
		RIVEN_STONE("earth", "riven_stone", Placement.COVER, 24000, "geode_grit"),
		WILDBLOOM("life", "wildbloom", Placement.REST, 24000, "wildbloom_petal"),
		VOID_SCAR("void", "void_scar", Placement.COVER, 6000, "hollow_dust"),
		STAR_GLYPH("arcane", "star_glyph", Placement.REST, 12000, "star_dust"),
		STILLED_SAND("time", "stilled_sand", Placement.REST, 12000, "hourglass_sand"),
		BLOODMOSS("blood", "bloodmoss", Placement.REST, 12000, "sanguine_bead");

		public final String element;
		/** The block's path ({@code wildercord:<path>}), also the id the record keeps. */
		public final String path;
		public final Placement placement;
		public final int lifetime;
		/** The reagent item's path. */
		public final String reagent;

		Kind(String element, String path, Placement placement, int lifetime, String reagent) {
			this.element = element;
			this.path = path;
			this.placement = placement;
			this.lifetime = lifetime;
			this.reagent = reagent;
		}

		public static Optional<Kind> byPath(String path) {
			for (Kind kind : values()) {
				if (kind.path.equals(path)) {
					return Optional.of(kind);
				}
			}
			return Optional.empty();
		}
	}

	/** The mark an element leaves, if it leaves one (every one of the ten does; a rune with no element doesn't). */
	public static Optional<Kind> kindFor(String element) {
		if (element == null || element.isEmpty()) {
			return Optional.empty();
		}
		String e = element.toLowerCase(Locale.ROOT);
		for (Kind kind : Kind.values()) {
			if (kind.element.equals(e)) {
				return Optional.of(kind);
			}
		}
		return Optional.empty();
	}

	// ------------------------------------------------------------------ when

	/** What set a residue off. */
	public enum Source {
		/** A player's spell: strong enough when its price reaches the threshold. */
		SPELL,
		/** A boss's spell (the Riftcaller, the Archivist, a dungeon's master out of its ward). */
		BOSS,
		/** An element reaction (Shatter, Overload...): sometimes, and small. */
		REACTION
	}

	/** The mana a spell must cost (its list price) to leave a residue: the default for {@code residues.min_spell_cost}. */
	public static final double MIN_SPELL_COST = 30;
	/** What an overcast adds to a spell's strength (and an overcast always counts as strong). */
	public static final double OVERCAST_BONUS = 0.5;
	/** The least a boss's spell counts for. */
	public static final double BOSS_STRENGTH = 1.5;
	/** The chance a reaction leaves a residue, and how strong it is. */
	public static final double REACTION_CHANCE = 0.35;
	public static final double REACTION_STRENGTH = 1.0;
	/** Strength is read up to this; stronger spells leave no more. */
	public static final double MAX_STRENGTH = 3.0;

	/**
	 * How strong a residue a cast leaves: 0 for none, else 1 and up (1 is a spell just at the threshold, 2
	 * one costing twice that). A reaction's strength is {@link #REACTION_STRENGTH}, rolled for by the caller
	 * against {@link #REACTION_CHANCE}.
	 *
	 * @param weight   the spell's list price in mana
	 * @param minCost  the threshold ({@code residues.min_spell_cost})
	 * @param overcast whether it was paid by cracking a Heart Circle
	 */
	public static double strength(Source source, double weight, double minCost, boolean overcast) {
		double s = weight / Math.max(1, minCost);
		switch (source) {
			case REACTION -> {
				return REACTION_STRENGTH;
			}
			case BOSS -> s = Math.max(s, BOSS_STRENGTH);
			default -> {
			}
		}
		if (overcast) {
			s = Math.max(s, 1.0) + OVERCAST_BONUS;
		}
		return s < 1.0 ? 0 : Math.min(MAX_STRENGTH, s);
	}

	/** How many blocks of residue one cast leaves: 1 at strength 1, one more for every half beyond, 5 at most. */
	public static int cells(double strength) {
		if (strength < 1.0) {
			return 0;
		}
		return (int) Math.max(1, Math.min(5, 1 + Math.floor((Math.min(strength, MAX_STRENGTH) - 1.0) * 2 + 1e-9)));
	}

	/** How far from where it landed (in blocks, either way) a cast's residue may be scattered. */
	public static int spread(double strength) {
		return (int) Math.max(1, Math.min(3, 1 + Math.floor(Math.min(strength, MAX_STRENGTH))));
	}

	/**
	 * How long a residue lasts, in ticks: its kind's lifetime at strength 1, up to half again for the
	 * strongest, times the server's {@code residues.lifetime_multiplier}. Never under a minute.
	 */
	public static long lifetime(Kind kind, double strength, double multiplier) {
		double scale = 0.75 + 0.25 * Math.max(1.0, Math.min(strength, MAX_STRENGTH));
		return Math.max(1200L, Math.round(kind.lifetime * scale * Math.max(0, multiplier)));
	}

	/** Ticks one caster waits between residues (a boss's twice as long), so a fight leaves marks, not a carpet. */
	public static final int CASTER_REST = 100;
	public static final int BOSS_REST = 200;

	public static int rest(Source source) {
		return source == Source.BOSS ? BOSS_REST : CASTER_REST;
	}

	// ------------------------------------------------------------------ how many

	/** The default most residue blocks in one chunk ({@code residues.max_per_chunk}). */
	public static final int PER_CHUNK = 12;
	/** The most residue blocks within {@link #AREA_RADIUS} blocks of each other. */
	public static final int PER_AREA = 6;
	public static final int AREA_RADIUS = 4;
	/** The default most residue blocks in one dimension ({@code residues.max_per_dimension}). */
	public static final int PER_DIMENSION = 1024;
	/** The most residue blocks one caster's magic keeps in a dimension (the oldest aren't taken; new ones simply aren't left). */
	public static final int PER_OWNER = 64;

	// ------------------------------------------------------------------ where

	/** What a block is, as far as a residue cares. */
	public enum Spot {
		/** Ground the world made (grass, dirt, sand, stone, snow...): see the {@code wildercord:residue_ground} block tag. */
		NATURAL,
		/** Air, or something as easily replaced that grew there (short grass, a fern, a thin layer of snow). */
		OPEN,
		/** Anything else: built, placed, valuable, or a block with contents. */
		OTHER
	}

	/** Why a residue can't go somewhere (null when it can). */
	public enum Refusal { UNLOADED, OUTSIDE_WORLD, PROTECTED, WARDED, TEMPORARY, BUILT, NO_GROUND, COVERED }

	/**
	 * Everything about one block a residue might take.
	 *
	 * @param here       the block it would take (for COVER, the ground block itself; for REST, the space above the ground)
	 * @param below      the block under {@code here}
	 * @param above      the block over {@code here}
	 * @param sturdy     whether {@code below} has a full top face to lie on
	 * @param temporary  whether {@code here} is a spell's passing block or already a residue
	 * @param contents   whether {@code here} holds a block entity (a chest, a sign, a spawner)
	 * @param permitted  whether whoever set it off may change blocks there (claims, spawn protection,
	 *                   {@code casting.spells_edit_blocks}, the residue switch, and for monsters the mob-griefing rule)
	 * @param warded     whether it's inside a dungeon's ward
	 * @param loaded     whether its chunk is loaded
	 * @param inWorld    whether it's inside the world border and the build height
	 */
	public record Site(Spot here, Spot below, Spot above, boolean sturdy, boolean temporary, boolean contents, boolean permitted, boolean warded,
			boolean loaded, boolean inWorld) {}

	/**
	 * Whether a residue may take {@code site}. It never replaces anything a player built or placed: a COVER
	 * residue takes only natural ground open to the sky above it, a REST residue only open air over sturdy
	 * natural ground; and never inside a ward, a claim it isn't allowed, or a spell's passing block.
	 */
	public static Refusal judge(Placement placement, Site site) {
		if (!site.loaded()) {
			return Refusal.UNLOADED;
		}
		if (!site.inWorld()) {
			return Refusal.OUTSIDE_WORLD;
		}
		if (site.warded()) {
			return Refusal.WARDED;
		}
		if (site.temporary()) {
			return Refusal.TEMPORARY;
		}
		if (site.contents()) {
			return Refusal.BUILT;
		}
		if (placement == Placement.COVER) {
			if (site.here() != Spot.NATURAL) {
				return site.here() == Spot.OPEN ? Refusal.NO_GROUND : Refusal.BUILT;
			}
			if (site.above() != Spot.OPEN) {
				return Refusal.COVERED;
			}
		} else {
			if (site.here() != Spot.OPEN) {
				return site.here() == Spot.NATURAL ? Refusal.COVERED : Refusal.BUILT;
			}
			if (site.below() != Spot.NATURAL || !site.sturdy()) {
				return site.below() == Spot.OTHER ? Refusal.BUILT : Refusal.NO_GROUND;
			}
		}
		// Asked last: a claim or protection mod hears the question only for a block the residue could really take.
		return site.permitted() ? null : Refusal.PROTECTED;
	}

	// ------------------------------------------------------------------ how it fades

	/** How far through its life a residue is: 0 just left, 1 due to fade. */
	public static double age(long placedAt, long due, long now) {
		if (due <= placedAt) {
			return 1;
		}
		return Math.max(0, Math.min(1, (now - placedAt) / (double) (due - placedAt)));
	}

	/** The stage (0 to 3) a residue that shows its age is at: a void scar narrows through these as it closes. */
	public static int stage(long placedAt, long due, long now) {
		return (int) Math.min(3, Math.floor(age(placedAt, due, now) * 4));
	}

	/** How many times a wildbloom's line may spread (the first flowers are generation 0, their seedlings 1...). */
	public static final int BLOOM_GENERATIONS = 2;
	/** How far a wildbloom spreads, in blocks either way. */
	public static final int BLOOM_REACH = 2;

	/**
	 * When a wildbloom's seedling fades: never after its parent, and never later than a seedling's own
	 * lifetime from now, so a meadow can't outlive the spell that grew it.
	 */
	public static long seedlingDue(long parentDue, long now, long ownLifetime) {
		return Math.min(parentDue, now + ownLifetime);
	}
}
