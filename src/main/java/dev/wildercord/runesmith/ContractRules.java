package dev.wildercord.runesmith;

import dev.wildercord.spell.Feats;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	/** Today's board: the one held if it's still today's, otherwise a fresh one. */
	public static Board today(Board held, long day, long seed) {
		return held.day() == day ? held : generate(day, seed);
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
