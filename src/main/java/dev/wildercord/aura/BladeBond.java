package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The bond's mark on the blade itself, the {@code wildercord:bonded_blade} component: who it's bonded to, how far it has grown, where
 * it was bonded, and its story. It travels with the blade (through a chest, a smithing table, a disciple's hands), so every client that
 * sees the blade can draw its glow and tell its story, and nothing about it lives anywhere the blade doesn't go. What only the server
 * keeps (whether the bond still stands, a blade on its way home) is {@code BladeRegistry}; what the swordsman keeps is
 * {@code BondedBlades.STATE}.
 *
 * <p>Pure records and codecs, so the rules' tests can build and read them.</p>
 */
public record BladeBond(Who who, Growth growth, Origin origin, History history) {
	/**
	 * Who it belongs to.
	 *
	 * @param id        the bond's own id (a blade bonded again is a new bond)
	 * @param owner     the swordsman it's bonded to
	 * @param ownerName their name as it was last seen, for its story and for anyone else who holds it
	 * @param lineage   the swordsmen it passed from, oldest first (at most {@link BladeRules#MAX_LINEAGE})
	 */
	public record Who(UUID id, UUID owner, String ownerName, List<String> lineage) {
		public Who {
			ownerName = ownerName == null ? "" : ownerName;
			lineage = lineage == null ? List.of() : List.copyOf(lineage.subList(Math.max(0, lineage.size() - BladeRules.MAX_LINEAGE), lineage.size()));
		}

		public static final Codec<Who> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(Who::id),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Who::owner),
			Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Who::ownerName),
			Codec.STRING.listOf().optionalFieldOf("lineage", List.of()).forGetter(Who::lineage)
		).apply(i, Who::new));

		public static final StreamCodec<ByteBuf, Who> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, Who::id, UUIDUtil.STREAM_CODEC, Who::owner, ByteBufCodecs.stringUtf8(64), Who::ownerName,
			ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(BladeRules.MAX_LINEAGE)), Who::lineage, Who::new);
	}

	/**
	 * How far it has grown.
	 *
	 * @param color     the aura's colour it has taken on (0xRRGGBB), its swordsman's now
	 * @param tier      its tier as it was earned (Bonded to Soulforged; a passed blade keeps it)
	 * @param resonance its resonance, gathered in every fight
	 * @param name      its name, from Named ("" before), as typed or suggested (made safe again wherever it's shown)
	 * @param namedDay  the day it took that name
	 * @param trait     its trait, from Awakened ("" while it waits to be chosen)
	 * @param traitArt  the art its Well-Worn Verse favours ("" for any other trait)
	 * @param offer     the traits it offers (drawn as it awakened, again at Soulforged)
	 * @param rechoose  whether its swordsman may change its trait for another offered, free (once, at Soulforged)
	 */
	public record Growth(int color, int tier, float resonance, String name, long namedDay, String trait, String traitArt, List<String> offer,
			boolean rechoose) {
		public static final Growth FRESH = new Growth(0, BladeRules.BONDED, 0, "", 0, "", "", List.of(), false);

		public Growth {
			tier = BladeRules.clampTier(tier);
			resonance = Math.max(0, resonance);
			name = name == null ? "" : name;
			trait = trait == null ? "" : trait;
			traitArt = traitArt == null ? "" : traitArt;
			offer = offer == null ? List.of() : List.copyOf(offer);
		}

		public static final Codec<Growth> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("color", 0).forGetter(Growth::color),
			Codec.INT.optionalFieldOf("tier", BladeRules.BONDED).forGetter(Growth::tier),
			Codec.FLOAT.optionalFieldOf("resonance", 0F).forGetter(Growth::resonance),
			Codec.STRING.optionalFieldOf("name", "").forGetter(Growth::name),
			Codec.LONG.optionalFieldOf("named_day", 0L).forGetter(Growth::namedDay),
			Codec.STRING.optionalFieldOf("trait", "").forGetter(Growth::trait),
			Codec.STRING.optionalFieldOf("trait_art", "").forGetter(Growth::traitArt),
			Codec.STRING.listOf().optionalFieldOf("offer", List.of()).forGetter(Growth::offer),
			Codec.BOOL.optionalFieldOf("rechoose", false).forGetter(Growth::rechoose)
		).apply(i, Growth::new));

		public static final StreamCodec<ByteBuf, Growth> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, Growth::color, ByteBufCodecs.VAR_INT, Growth::tier, ByteBufCodecs.FLOAT, Growth::resonance,
			ByteBufCodecs.stringUtf8(128), Growth::name, ByteBufCodecs.VAR_LONG, Growth::namedDay, ByteBufCodecs.stringUtf8(96), Growth::trait,
			ByteBufCodecs.stringUtf8(128), Growth::traitArt, ByteBufCodecs.stringUtf8(96).apply(ByteBufCodecs.list(8)), Growth::offer, ByteBufCodecs.BOOL,
			Growth::rechoose, Growth::new);

		public Growth withResonance(float resonance) {
			return new Growth(color, tier, resonance, name, namedDay, trait, traitArt, offer, rechoose);
		}

		public Growth withColor(int color) {
			return new Growth(color, tier, resonance, name, namedDay, trait, traitArt, offer, rechoose);
		}

		public Growth withTier(int tier) {
			return new Growth(color, tier, resonance, name, namedDay, trait, traitArt, offer, rechoose);
		}

		public Growth withName(String name, long day) {
			return new Growth(color, tier, resonance, name, day, trait, traitArt, offer, rechoose);
		}

		public Growth withTrait(String trait, String traitArt, boolean rechoose) {
			return new Growth(color, tier, resonance, name, namedDay, trait, traitArt, offer, rechoose);
		}

		public Growth withOffer(List<String> offer, boolean rechoose) {
			return new Growth(color, tier, resonance, name, namedDay, trait, traitArt, offer, rechoose);
		}
	}

	/**
	 * Where it was bonded.
	 *
	 * @param day       the world's day it was bonded on (from 1)
	 * @param biome     the biome's id there
	 * @param dimension the dimension's id
	 * @param x         where (block)
	 * @param method    its swordsman's method then
	 * @param how       how the bond was made: "ceremony", or an add-on's own word (a sleeping blade drawn from the rock)
	 */
	public record Origin(long day, String biome, String dimension, int x, int y, int z, String method, String how) {
		public static final Origin NOWHERE = new Origin(0, "", "", 0, 0, 0, "", "");

		public Origin {
			biome = biome == null ? "" : biome;
			dimension = dimension == null ? "" : dimension;
			method = method == null ? "" : method;
			how = how == null ? "" : how;
		}

		public static final Codec<Origin> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("day", 0L).forGetter(Origin::day),
			Codec.STRING.optionalFieldOf("biome", "").forGetter(Origin::biome),
			Codec.STRING.optionalFieldOf("dimension", "").forGetter(Origin::dimension),
			Codec.INT.optionalFieldOf("x", 0).forGetter(Origin::x),
			Codec.INT.optionalFieldOf("y", 0).forGetter(Origin::y),
			Codec.INT.optionalFieldOf("z", 0).forGetter(Origin::z),
			Codec.STRING.optionalFieldOf("method", "").forGetter(Origin::method),
			Codec.STRING.optionalFieldOf("how", "").forGetter(Origin::how)
		).apply(i, Origin::new));

		public static final StreamCodec<ByteBuf, Origin> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Origin::day, ByteBufCodecs.stringUtf8(128), Origin::biome, ByteBufCodecs.stringUtf8(128), Origin::dimension,
			ByteBufCodecs.VAR_INT, Origin::x, ByteBufCodecs.VAR_INT, Origin::y, ByteBufCodecs.VAR_INT, Origin::z, ByteBufCodecs.stringUtf8(96),
			Origin::method, ByteBufCodecs.stringUtf8(64), Origin::how, Origin::new);
	}

	/**
	 * A notable deed, kept as a line of its story.
	 *
	 * @param kind what it was ({@link BladeRules#DEEDS})
	 * @param arg  what it names (a boss's language key, a stage's id, a technique's name, a Way's id...), shown by the kind's line
	 * @param day  the world's day it happened on
	 */
	public record Deed(String kind, String arg, long day) {
		public Deed {
			kind = kind == null ? "" : kind;
			arg = arg == null ? "" : arg;
		}

		public static final Codec<Deed> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("kind").forGetter(Deed::kind),
			Codec.STRING.optionalFieldOf("arg", "").forGetter(Deed::arg),
			Codec.LONG.optionalFieldOf("day", 0L).forGetter(Deed::day)
		).apply(i, Deed::new));

		public static final StreamCodec<ByteBuf, Deed> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), Deed::kind, ByteBufCodecs.stringUtf8(160), Deed::arg, ByteBufCodecs.VAR_LONG, Deed::day, Deed::new);
	}

	/**
	 * Its story: what it has done, counted ({@link BladeRules#COUNTS}), the arts it has played by name (the most played
	 * {@link BladeRules#MAX_ARTS}), and its notable deeds (the latest {@link BladeRules#MAX_DEEDS}).
	 */
	public record History(Map<String, Integer> counts, Map<String, Integer> arts, List<Deed> deeds) {
		public static final History EMPTY = new History(Map.of(), Map.of(), List.of());

		public History {
			counts = counts == null ? Map.of() : Map.copyOf(counts);
			arts = arts == null ? Map.of() : Map.copyOf(arts);
			deeds = deeds == null ? List.of() : List.copyOf(deeds.subList(Math.max(0, deeds.size() - BladeRules.MAX_DEEDS), deeds.size()));
		}

		public static final Codec<History> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counts", Map.of()).forGetter(History::counts),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("arts", Map.of()).forGetter(History::arts),
			Deed.CODEC.listOf().optionalFieldOf("deeds", List.of()).forGetter(History::deeds)
		).apply(i, History::new));

		public static final StreamCodec<ByteBuf, History> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(32), ByteBufCodecs.VAR_INT, 32), History::counts,
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(128), ByteBufCodecs.VAR_INT, BladeRules.MAX_ARTS + 4), History::arts,
			Deed.STREAM_CODEC.apply(ByteBufCodecs.list(BladeRules.MAX_DEEDS + 2)), History::deeds, History::new);

		public int count(String id) {
			return counts.getOrDefault(id, 0);
		}

		/** The same with each of {@code more} added to its count, and each art of {@code arts} counted (the least played let go). */
		public History plus(Map<String, Integer> more, List<String> artsPlayed, List<Deed> newDeeds) {
			Map<String, Integer> c = new LinkedHashMap<>(counts);
			more.forEach((k, v) -> c.merge(k, v, Integer::sum));
			Map<String, Integer> a = arts;
			for (String art : artsPlayed) {
				a = BladeRules.countArt(a, art);
			}
			List<Deed> d = new ArrayList<>(deeds);
			d.addAll(newDeeds);
			return new History(c, a, d);
		}
	}

	public static final Codec<BladeBond> CODEC = RecordCodecBuilder.create(i -> i.group(
		Who.CODEC.fieldOf("who").forGetter(BladeBond::who),
		Growth.CODEC.optionalFieldOf("growth", Growth.FRESH).forGetter(BladeBond::growth),
		Origin.CODEC.optionalFieldOf("origin", Origin.NOWHERE).forGetter(BladeBond::origin),
		History.CODEC.optionalFieldOf("history", History.EMPTY).forGetter(BladeBond::history)
	).apply(i, BladeBond::new));

	public static final StreamCodec<ByteBuf, BladeBond> STREAM_CODEC = StreamCodec.composite(
		Who.STREAM_CODEC, BladeBond::who, Growth.STREAM_CODEC, BladeBond::growth, Origin.STREAM_CODEC, BladeBond::origin, History.STREAM_CODEC,
		BladeBond::history, BladeBond::new);

	/**
	 * What a blade remembers of a bond that ended (released, or its swordsman bonded another): its name, whose it was and how far it had
	 * grown, the {@code wildercord:former_bond} component. Nothing but a line in its tooltip.
	 */
	public record Former(String name, String ownerName, int tier) {
		public Former {
			name = name == null ? "" : name;
			ownerName = ownerName == null ? "" : ownerName;
		}

		public static final Codec<Former> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("name", "").forGetter(Former::name),
			Codec.STRING.optionalFieldOf("owner_name", "").forGetter(Former::ownerName),
			Codec.INT.optionalFieldOf("tier", 0).forGetter(Former::tier)
		).apply(i, Former::new));

		public static final StreamCodec<ByteBuf, Former> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Former::name, ByteBufCodecs.stringUtf8(64), Former::ownerName, ByteBufCodecs.VAR_INT, Former::tier,
			Former::new);
	}

	// ------------------------------------------------------------------ reading

	public UUID id() {
		return who.id();
	}

	public UUID owner() {
		return who.owner();
	}

	public boolean ownedBy(UUID player) {
		return who.owner().equals(player);
	}

	public int tier() {
		return growth.tier();
	}

	public int color() {
		return growth.color();
	}

	/** Its name as anyone is shown it (made safe again, whatever was stored), or "" before it has one. */
	public String shownName() {
		return BladeRules.cleanName(growth.name());
	}

	/** A seed of its own, for its suggested names and the order of its offered traits. */
	public long seed() {
		return who.id().getMostSignificantBits() ^ Long.rotateLeft(who.id().getLeastSignificantBits(), 17);
	}

	/** Its history as the traits read it, with its swordsman's method and Way now. */
	public BladeRules.History habits(String method, String way) {
		return new BladeRules.History(history.counts(), history.arts(), method, way);
	}

	/** What its suggested names are drawn from (its swordsman's element and Way now). */
	public BladeRules.NameSeed nameSeed(String element, String way) {
		return new BladeRules.NameSeed(element, way, BladeRules.artWord(BladeRules.favourite(history.arts())), origin.biome(),
			BladeRules.foeKind(history.counts()), seed());
	}

	// ------------------------------------------------------------------ changing (each a new value: a component is never changed in place)

	public BladeBond withGrowth(Growth growth) {
		return new BladeBond(who, growth, origin, history);
	}

	public BladeBond withHistory(History history) {
		return new BladeBond(who, growth, origin, history);
	}

	public BladeBond withWho(Who who) {
		return new BladeBond(who, growth, origin, history);
	}

	/** A fresh bond: {@code id} to {@code owner}, bonded at {@code origin}, in {@code color}. */
	public static BladeBond fresh(UUID id, UUID owner, String ownerName, int color, Origin origin) {
		return new BladeBond(new Who(id, owner, ownerName, List.of()), Growth.FRESH.withColor(color), origin, History.EMPTY);
	}
}
