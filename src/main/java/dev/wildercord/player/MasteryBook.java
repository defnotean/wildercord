package dev.wildercord.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.spell.MasteryRules;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A player's spell mastery: a record for each spell (each exact sequence of runes, see {@link MasteryRules#key}) they
 * have used to some purpose, the {@link MasteryRules#MAX_RECORDS} most recently used kept. Immutable, like the
 * {@link Spellbook}: every change returns a new book, which is what makes the attachment save and sync it. Saved with
 * the player, kept through death and synced to that player only (see {@link MasteryAttachments}).
 */
public record MasteryBook(List<Entry> entries) {
	public static final MasteryBook EMPTY = new MasteryBook(List.of());

	public MasteryBook {
		entries = List.copyOf(entries);
	}

	/**
	 * One spell's record.
	 *
	 * @param key      the spell's identity (its rune ids in order)
	 * @param xp       experience earned for real
	 * @param practice experience learned on training dummies and in the practice arena (held to {@link MasteryRules#PRACTICE_CAP})
	 * @param traits   the trait in each slot, one for each of ranks II to V ("" while empty)
	 * @param borrowed the slots whose trait was taught by an inscribed scroll and not yet earned (one bit a slot)
	 * @param changed  the slots that have used their one change of mind (a re-roll or an unbinding)
	 * @param offer    the traits offered for the next slot to fill, or empty when nothing is waiting
	 * @param counters how many of its counted casts were made in each circumstance (rain, night, underground...)
	 * @param casts    how many casts were counted
	 * @param seed     its sigil's seed (its owner's, or for a taught spell its teacher's)
	 * @param used     the game time it was last used, so the least recently used is forgotten first
	 * @param teacher  who inscribed the scroll it was taught from, or ""
	 */
	public record Entry(String key, double xp, double practice, List<String> traits, int borrowed, int changed, List<String> offer,
			Map<String, Integer> counters, int casts, long seed, long used, String teacher) {
		public Entry {
			List<String> slots = new ArrayList<>(MasteryRules.SLOTS);
			for (int i = 0; i < MasteryRules.SLOTS; i++) {
				slots.add(i < traits.size() && traits.get(i) != null ? traits.get(i) : "");
			}
			traits = List.copyOf(slots);
			offer = List.copyOf(offer);
			counters = Map.copyOf(counters);
			borrowed &= (1 << MasteryRules.SLOTS) - 1;
			changed &= (1 << MasteryRules.SLOTS) - 1;
		}

		/** A fresh record for {@code key}, drawing its sigil from {@code seed}. */
		public static Entry fresh(String key, long seed, long now) {
			return new Entry(key, 0, 0, List.of(), 0, 0, List.of(), Map.of(), 0, seed, now, "");
		}

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("key").forGetter(Entry::key),
			Codec.DOUBLE.optionalFieldOf("xp", 0.0).forGetter(Entry::xp),
			Codec.DOUBLE.optionalFieldOf("practice", 0.0).forGetter(Entry::practice),
			Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(Entry::traits),
			Codec.INT.optionalFieldOf("borrowed", 0).forGetter(Entry::borrowed),
			Codec.INT.optionalFieldOf("changed", 0).forGetter(Entry::changed),
			Codec.STRING.listOf().optionalFieldOf("offer", List.of()).forGetter(Entry::offer),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(Entry::counters),
			Codec.INT.optionalFieldOf("casts", 0).forGetter(Entry::casts),
			Codec.LONG.optionalFieldOf("seed", 1L).forGetter(Entry::seed),
			Codec.LONG.optionalFieldOf("used", 0L).forGetter(Entry::used),
			Codec.STRING.optionalFieldOf("teacher", "").forGetter(Entry::teacher)
		).apply(i, Entry::new));

		/** Everything it has learned: real experience and practice together. */
		public double total() {
			return xp + practice;
		}

		public int rank() {
			return MasteryRules.rank(total());
		}

		/** Whether {@code slot}'s trait is the caster's own (chosen, or a borrowed one kept when its rank came). */
		public boolean settled(int slot) {
			return !traits.get(slot).isEmpty() && (borrowed & (1 << slot)) == 0;
		}

		public boolean borrowed(int slot) {
			return !traits.get(slot).isEmpty() && (borrowed & (1 << slot)) != 0;
		}

		public boolean changed(int slot) {
			return (changed & (1 << slot)) != 0;
		}

		/** The first slot its rank has opened that isn't settled yet, or -1: a trait waiting to be chosen. */
		public int pendingSlot() {
			int rank = rank();
			for (int slot = 0; slot < MasteryRules.SLOTS; slot++) {
				if (rank >= MasteryRules.rankOf(slot) && !settled(slot)) {
					return slot;
				}
			}
			return -1;
		}

		/** Every trait that works on it: its own and borrowed ones alike. */
		public List<String> active() {
			return traits.stream().filter(t -> !t.isEmpty()).toList();
		}

		/** Its own traits only (what an inscription may carry). */
		public List<String> own() {
			List<String> out = new ArrayList<>();
			for (int slot = 0; slot < MasteryRules.SLOTS; slot++) {
				if (settled(slot)) {
					out.add(traits.get(slot));
				}
			}
			return out;
		}

		public Entry withXp(double xp, double practice) {
			return new Entry(key, xp, practice, traits, borrowed, changed, offer, counters, casts, seed, used, teacher);
		}

		public Entry withTrait(int slot, String trait, boolean isBorrowed) {
			List<String> next = new ArrayList<>(traits);
			next.set(slot, trait);
			int b = isBorrowed ? borrowed | (1 << slot) : borrowed & ~(1 << slot);
			return new Entry(key, xp, practice, next, b, changed, offer, counters, casts, seed, used, teacher);
		}

		public Entry withOffer(List<String> offer) {
			return new Entry(key, xp, practice, traits, borrowed, changed, offer, counters, casts, seed, used, teacher);
		}

		public Entry withChanged(int slot) {
			return new Entry(key, xp, practice, traits, borrowed, changed | (1 << slot), offer, counters, casts, seed, used, teacher);
		}

		/** This record with {@code more} casts counted, and {@code counts} more of them in each circumstance. */
		public Entry counted(Map<String, Integer> counts, int more) {
			Map<String, Integer> next = new HashMap<>(counters);
			counts.forEach((c, n) -> next.merge(c, n, Integer::sum));
			return new Entry(key, xp, practice, traits, borrowed, changed, offer, next, casts + more, seed, used, teacher);
		}

		public Entry usedAt(long now) {
			return new Entry(key, xp, practice, traits, borrowed, changed, offer, counters, casts, seed, now, teacher);
		}

		public Entry taught(long seed, String teacher) {
			return new Entry(key, xp, practice, traits, borrowed, changed, offer, counters, casts, seed, used, teacher);
		}
	}

	public static final Codec<MasteryBook> CODEC = Entry.CODEC.listOf().xmap(MasteryBook::new, MasteryBook::entries);
	public static final StreamCodec<ByteBuf, MasteryBook> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

	public Optional<Entry> entry(String key) {
		for (Entry e : entries) {
			if (e.key().equals(key)) {
				return Optional.of(e);
			}
		}
		return Optional.empty();
	}

	/**
	 * This book with {@code entry} in it (replacing any record of the same spell). Past {@link MasteryRules#MAX_RECORDS}
	 * the least recently used record is forgotten, but never one of {@code keep} (the spells threaded on the Cord).
	 */
	public MasteryBook with(Entry entry, Collection<String> keep) {
		List<Entry> next = new ArrayList<>(entries.size() + 1);
		for (Entry e : entries) {
			if (!e.key().equals(entry.key())) {
				next.add(e);
			}
		}
		next.add(entry);
		while (next.size() > MasteryRules.MAX_RECORDS) {
			Optional<Entry> oldest = next.stream()
				.filter(e -> !e.key().equals(entry.key()) && !keep.contains(e.key()))
				.min(Comparator.comparingLong(Entry::used));
			if (oldest.isEmpty()) {
				break;
			}
			next.remove(oldest.get());
		}
		return new MasteryBook(next);
	}

	/** The spells waiting for a trait to be chosen. */
	public List<Entry> pending() {
		return entries.stream().filter(e -> e.pendingSlot() >= 0).toList();
	}
}
