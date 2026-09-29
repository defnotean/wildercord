package dev.wildercord.runesmith;

import dev.wildercord.spell.Feats;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The Runesmith's daily contracts, as plain rules (no Minecraft types, so they're unit-tested):
 * three a day for each player, the same three all day however often the board is read, new ones
 * at dawn. Each asks for something done with magic, counts up as it's done, and pays out once,
 * at a Scribing Desk.
 */
public final class ContractRules {
	private ContractRules() {}

	/** Contracts on the board each day. */
	public static final int COUNT = 3;

	/** What a contract asks for. Its {@code arg} narrows it: an element, or a reaction. */
	public static final String RUNEBOUND = "runebound";
	public static final String REACTION = "reaction";
	public static final String LEY = "ley";
	public static final String IMBUE = "imbue";
	public static final String SPELL_KILLS = "spell_kills";
	public static final String ELEMENT_CASTS = "element_casts";

	public static final List<String> KINDS = List.of(RUNEBOUND, REACTION, LEY, IMBUE, SPELL_KILLS, ELEMENT_CASTS);
	/** Elements a contract may ask for: ones with plenty of harmful runes in the first two tiers. */
	public static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "arcane");

	/** What a contract pays: {@code emerald}, {@code blank_rune}, {@code mana_crystal} (a count each) or {@code rune} (its tier). */
	public record Reward(String type, int amount) {
		public static Reward parse(String text) {
			int colon = text.indexOf(':');
			if (colon < 0) {
				return new Reward(text, 1);
			}
			try {
				return new Reward(text.substring(0, colon), Integer.parseInt(text.substring(colon + 1)));
			} catch (NumberFormatException e) {
				return new Reward(text.substring(0, colon), 1);
			}
		}

		@Override
		public String toString() {
			return type + ":" + amount;
		}
	}

	/** One contract: what it asks, how far along it is, and whether it's been handed in. */
	public record Contract(String kind, String arg, int target, int progress, boolean claimed, String reward) {
		public boolean done() {
			return progress >= target;
		}

		public Contract advance(int amount) {
			return new Contract(kind, arg, target, Math.min(target, progress + amount), claimed, reward);
		}

		public Contract claim() {
			return new Contract(kind, arg, target, progress, true, reward);
		}

		public boolean matches(String kind, String arg) {
			return this.kind.equals(kind) && (this.arg.isEmpty() || this.arg.equals(arg));
		}
	}

	/** One player's board: the day it was written for, and its contracts. */
	public record Board(long day, List<Contract> contracts) {
		public static final Board EMPTY = new Board(Long.MIN_VALUE, List.of());

		public Board {
			contracts = List.copyOf(contracts);
		}
	}

	/** Three different contracts for {@code day}, the same every time for the same seed. */
	public static Board generate(long day, long seed) {
		Random random = new Random(seed * 31 + day * 0x9E3779B97F4A7C15L);
		List<String> kinds = new ArrayList<>(KINDS);
		java.util.Collections.shuffle(kinds, random);
		List<Contract> contracts = new ArrayList<>();
		for (String kind : kinds.subList(0, COUNT)) {
			contracts.add(contract(kind, random));
		}
		return new Board(day, contracts);
	}

	private static Contract contract(String kind, Random random) {
		return switch (kind) {
			case RUNEBOUND -> new Contract(kind, pick(ELEMENTS, random), 3 + random.nextInt(3), 0, false, new Reward("rune", 2).toString());
			case REACTION -> new Contract(kind, pick(Feats.REACTIONS, random), 2 + random.nextInt(3), 0, false, new Reward("blank_rune", 6).toString());
			case LEY -> new Contract(kind, "", 15 + 5 * random.nextInt(3), 0, false, new Reward("emerald", 8).toString());
			case IMBUE -> new Contract(kind, "", 1, 0, false, new Reward("mana_crystal", 1).toString());
			case SPELL_KILLS -> new Contract(kind, "", 10 + 5 * random.nextInt(3), 0, false, new Reward("emerald", 6).toString());
			default -> new Contract(ELEMENT_CASTS, pick(ELEMENTS, random), 20 + 5 * random.nextInt(3), 0, false, new Reward("blank_rune", 4).toString());
		};
	}

	private static String pick(List<String> options, Random random) {
		return options.get(random.nextInt(options.size()));
	}

	/**
	 * Today's board: the one held, until a later day comes. Time turned back (an operator's
	 * {@code /time set}) keeps the board as it is, so its contracts can't be handed in twice. A
	 * contract finished but not yet handed in stays on the new board, in place of one of the new
	 * ones, until it's handed in: a reward earned is never lost to the dawn.
	 */
	public static Board today(Board held, long day, long seed) {
		if (day <= held.day()) {
			return held;
		}
		Board fresh = generate(day, seed);
		List<Contract> contracts = new ArrayList<>();
		for (Contract contract : held.contracts()) {
			if (contract.done() && !contract.claimed() && contracts.size() < COUNT) {
				contracts.add(contract);
			}
		}
		if (contracts.isEmpty()) {
			return fresh;
		}
		// The new ones fill the rest, each of a kind not already on the board.
		for (Contract contract : fresh.contracts()) {
			if (contracts.size() < COUNT && contracts.stream().noneMatch(c -> c.kind().equals(contract.kind()))) {
				contracts.add(contract);
			}
		}
		return new Board(day, contracts);
	}

	/** What {@link #progress} changed: the new board, and which contracts moved and which were finished by it. */
	public record Progress(Board board, List<Integer> advanced, List<Integer> finished) {
		public boolean changed() {
			return !advanced.isEmpty();
		}
	}

	/** Counts {@code amount} toward every unfinished contract of this kind (and element or reaction). */
	public static Progress progress(Board board, String kind, String arg, int amount) {
		List<Contract> next = new ArrayList<>(board.contracts());
		List<Integer> advanced = new ArrayList<>();
		List<Integer> finished = new ArrayList<>();
		for (int i = 0; i < next.size(); i++) {
			Contract contract = next.get(i);
			if (amount <= 0 || contract.done() || !contract.matches(kind, arg)) {
				continue;
			}
			Contract moved = contract.advance(amount);
			next.set(i, moved);
			advanced.add(i);
			if (moved.done()) {
				finished.add(i);
			}
		}
		return new Progress(advanced.isEmpty() ? board : new Board(board.day(), next), advanced, finished);
	}

	/**
	 * What counts toward the casting contracts, for one player: a cast (for its elements, and a ley
	 * line under it) and a reaction count only once a spell of theirs lands on a real creature (a
	 * living thing that isn't a Training Dummy), so casting at nothing or at a dummy earns nothing.
	 *
	 * <p>A cast waits up to {@link #CAST_WINDOW} ticks for its spell to land (a bolt is in the air a
	 * while); a reaction must land in the same tick. However many creatures one spell strikes at
	 * once, it credits one cast. Not thread-safe; the server keeps one per player.</p>
	 */
	public static final class Credit {
		/** How long a cast waits for its spell to land on something. */
		public static final int CAST_WINDOW = 100;

		/** A cast waiting to land: its elements, whether it was cast on a ley line, and when. */
		public record Cast(Set<String> elements, boolean ley, long time) {}

		/** What a call made count: casts, and reactions. */
		public record Credited(List<Cast> casts, List<String> reactions) {
			public static final Credited NONE = new Credited(List.of(), List.of());

			public boolean isEmpty() {
				return casts.isEmpty() && reactions.isEmpty();
			}
		}

		private final ArrayDeque<Cast> casts = new ArrayDeque<>();
		private final List<String> reactions = new ArrayList<>();
		private long reactionTick = Long.MIN_VALUE;
		/** The tick of the last real hit, whether a cast was credited in it, and whether it's a hit no cast has used yet. */
		private long hitTick = Long.MIN_VALUE;
		private boolean creditedThisTick;
		private boolean spareHit;

		/** A spell was cast. Counts at once when a hit already landed this tick (an instant spell), otherwise waits. */
		public Credited cast(long now, Set<String> elements, boolean ley) {
			trim(now);
			Cast cast = new Cast(Set.copyOf(elements), ley, now);
			if (hitTick == now && spareHit && !creditedThisTick) {
				spareHit = false;
				creditedThisTick = true;
				return new Credited(List.of(cast), List.of());
			}
			casts.addLast(cast);
			return Credited.NONE;
		}

		/** A reaction was set off. Counts when a real hit lands this same tick (before or after). */
		public Credited reaction(long now, String reaction) {
			if (hitTick == now) {
				return new Credited(List.of(), List.of(reaction));
			}
			if (reactionTick != now) {
				reactions.clear();
				reactionTick = now;
			}
			reactions.add(reaction);
			return Credited.NONE;
		}

		/** A spell of the player's landed on a real creature. */
		public Credited hit(long now) {
			trim(now);
			if (hitTick != now) {
				hitTick = now;
				creditedThisTick = false;
				spareHit = false;
			}
			List<String> reacted = List.of();
			if (reactionTick == now && !reactions.isEmpty()) {
				reacted = List.copyOf(reactions);
				reactions.clear();
			}
			List<Cast> landed = List.of();
			if (!creditedThisTick) {
				if (casts.isEmpty()) {
					spareHit = true;
				} else {
					landed = List.of(casts.pollFirst());
					creditedThisTick = true;
				}
			}
			return landed.isEmpty() && reacted.isEmpty() ? Credited.NONE : new Credited(landed, reacted);
		}

		/** Whether nothing is waiting any more (so the server can let this go). */
		public boolean idle(long now) {
			trim(now);
			return casts.isEmpty() && now - hitTick > 1 && now - reactionTick > 1;
		}

		private void trim(long now) {
			while (!casts.isEmpty() && now - casts.peekFirst().time() > CAST_WINDOW) {
				casts.pollFirst();
			}
		}
	}

	/** Hands in every finished contract: the board with them marked claimed, and what they pay. */
	public record Claim(Board board, List<Reward> rewards) {}

	public static Claim claim(Board board) {
		List<Contract> next = new ArrayList<>(board.contracts());
		List<Reward> rewards = new ArrayList<>();
		for (int i = 0; i < next.size(); i++) {
			Contract contract = next.get(i);
			if (contract.done() && !contract.claimed()) {
				next.set(i, contract.claim());
				rewards.add(Reward.parse(contract.reward()));
			}
		}
		return new Claim(rewards.isEmpty() ? board : new Board(board.day(), next), rewards);
	}
}
