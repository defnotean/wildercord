package dev.wildercord.guild;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mentoring (0.13): the mage's counterpart to a swordsman's lineage. A mage of the 10th circle or past it can take up to three
 * apprentices who haven't reached the 5th. An apprentice casting within 24 blocks of their mentor condenses a quarter more,
 * the same pace a disciple near their master learns. Every circle an apprentice forms pays their mentor in Mana Crystals, and
 * at the 10th circle the apprentice graduates and the bond ends on its own. Only the early circles go faster, so the long
 * road to the 20th stays as long as it was. Pure rules, so the ledger, the commands and the tests agree.
 */
public final class MentorRules {
	private MentorRules() {}

	public static final int MENTOR_FROM = 10;
	/** An apprentice is taken before this circle. */
	public static final int TAKE_BELOW = 5;
	/** An apprentice graduates on forming this circle. */
	public static final int GRADUATE_AT = 10;
	public static final int MAX_APPRENTICES = 3;
	public static final double NEAR = 24;
	public static final double NEAR_GAIN = 1.25;

	public enum Refusal { NONE, SELF, MENTOR_TOO_YOUNG, APPRENTICE_TOO_FAR_ALONG, ALREADY_APPRENTICED, MENTOR_IS_APPRENTICE, FULL }

	/** Whether a mentor of {@code mentorCircles} with {@code apprentices} may take one of {@code apprenticeCircles}. */
	public static Refusal refusal(boolean self, int mentorCircles, int apprenticeCircles, int apprentices, boolean apprenticed, boolean mentorApprenticed) {
		if (self) return Refusal.SELF;
		if (mentorCircles < MENTOR_FROM) return Refusal.MENTOR_TOO_YOUNG;
		if (mentorApprenticed) return Refusal.MENTOR_IS_APPRENTICE;
		if (apprenticeCircles >= TAKE_BELOW) return Refusal.APPRENTICE_TOO_FAR_ALONG;
		if (apprenticed) return Refusal.ALREADY_APPRENTICED;
		if (apprentices >= MAX_APPRENTICES) return Refusal.FULL;
		return Refusal.NONE;
	}

	/** What an apprentice's condensed mana counts for, near their mentor or not. */
	public static double gain(boolean near) {
		return near ? NEAR_GAIN : 1;
	}

	/** Mana Crystals a mentor takes when their apprentice forms circle {@code n}: one, two from the 5th. */
	public static int tutelage(int n) {
		return n >= TAKE_BELOW ? 2 : 1;
	}

	public static boolean graduates(int n) {
		return n >= GRADUATE_AT;
	}

	/** Who is apprenticed to whom. */
	public static final class Book {
		private final Map<UUID, UUID> mentorOf = new HashMap<>();

		public Book() {}

		public Book(Map<UUID, UUID> mentorOf) {
			this.mentorOf.putAll(mentorOf);
		}

		public Map<UUID, UUID> all() {
			return Map.copyOf(mentorOf);
		}

		public UUID mentorOf(UUID apprentice) {
			return mentorOf.get(apprentice);
		}

		public List<UUID> apprenticesOf(UUID mentor) {
			return mentorOf.entrySet().stream().filter(e -> e.getValue().equals(mentor)).map(Map.Entry::getKey).sorted().toList();
		}

		public void take(UUID mentor, UUID apprentice) {
			mentorOf.put(apprentice, mentor);
		}

		/** Ends whatever bond is between the two, either way round. */
		public boolean end(UUID a, UUID b) {
			return mentorOf.remove(a, b) || mentorOf.remove(b, a);
		}
	}

	/**
	 * What mentors have earned: the highest circle each apprentice has paid out for (a circle formed again, after a
	 * retempering say, pays nothing more), and crystals held for a mentor who was away when they were earned.
	 */
	public static final class Tutelage {
		private final Map<UUID, Integer> paidThrough = new HashMap<>();
		private final Map<UUID, Integer> owed = new HashMap<>();

		public Tutelage() {}

		public Tutelage(Map<UUID, Integer> paidThrough, Map<UUID, Integer> owed) {
			this.paidThrough.putAll(paidThrough);
			this.owed.putAll(owed);
		}

		public Map<UUID, Integer> paidThrough() {
			return Map.copyOf(paidThrough);
		}

		public Map<UUID, Integer> owed() {
			return Map.copyOf(owed);
		}

		/** {@code apprentice} formed circle {@code n}: the crystals it earns their mentor, nothing if it was paid for before. */
		public int earn(UUID apprentice, int n) {
			if (n <= paidThrough.getOrDefault(apprentice, 0)) return 0;
			paidThrough.put(apprentice, n);
			return tutelage(n);
		}

		/** Holds {@code crystals} for {@code mentor} until they're back. */
		public void owe(UUID mentor, int crystals) {
			if (crystals > 0) owed.merge(mentor, crystals, Integer::sum);
		}

		/** Takes what's held for {@code mentor}: zero if nothing. */
		public int collect(UUID mentor) {
			Integer held = owed.remove(mentor);
			return held == null ? 0 : held;
		}
	}
}
