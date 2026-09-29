package dev.wildercord.social;

import dev.wildercord.chorus.ChorusRules;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.runesmith.ContractRules;
import dev.wildercord.runesmith.RuneTrades;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The Runesmith's trades and contracts, the duel state machine and chorus matching. */
class SocialRulesTest {
	private static final UUID A = new UUID(0, 1);
	private static final UUID B = new UUID(0, 2);
	private static final UUID C = new UUID(0, 3);
	private static final UUID D = new UUID(0, 4);

	// ------------------------------------------------------------------ trades

	@Test
	void shelvesNeverSellInnateRunesAndStayInTier() {
		Random random = new Random(7);
		for (int i = 0; i < 500; i++) {
			RuneDef rune = RuneTrades.random(1, 2, random).orElseThrow();
			assertTrue(rune.tier() >= 1 && rune.tier() <= 2, rune.id());
			assertFalse(Runes.innate(rune), rune.id());
		}
		assertEquals(3, RuneTrades.random(3, 3, random).orElseThrow().tier());
		for (int tier = 1; tier <= 4; tier++) {
			assertFalse(RuneTrades.pool(tier).isEmpty(), "tier " + tier);
		}
	}

	@Test
	void buybackPaysMoreForRarerRunes() {
		for (int tier = 1; tier < 4; tier++) {
			assertTrue(RuneTrades.buybackPrice(tier + 1) > RuneTrades.buybackPrice(tier));
		}
		Map<String, Integer> held = new LinkedHashMap<>();
		held.put(Runes.HEAL.id(), 3);
		held.put(Runes.ZONE.id(), 1);
		held.put(Runes.BEAM.id(), 0);
		held.put(Runes.KINDLING.id(), 1);
		// Rarest first; nothing that isn't held; never an innate rune.
		assertEquals(List.of(Runes.ZONE.id(), Runes.HEAL.id()), RuneTrades.buybacks(held));
	}

	@Test
	void rerollTakesTwoOfATierAndGivesAnUnknownOne() {
		Map<String, Integer> held = new LinkedHashMap<>();
		held.put(Runes.HEAL.id(), 2);
		held.put(Runes.HARM.id(), 1);
		held.put(Runes.BEAM.id(), 1);
		held.put(Runes.BURST.id(), 1);
		held.put(Runes.ZONE.id(), 1);
		List<RuneTrades.Pair> pairs = RuneTrades.rerolls(held);
		// Tier I: the rune held twice pays alone. Tier II: two different ones. Tier III: only one held, no reroll.
		assertEquals(2, pairs.size());
		assertEquals(new RuneTrades.Pair(Runes.HEAL.id(), Runes.HEAL.id(), 1), pairs.get(0));
		assertEquals(2, pairs.get(1).tier());
		assertNotEquals(pairs.get(1).first(), pairs.get(1).second());

		Set<String> known = new HashSet<>();
		for (RuneDef rune : RuneTrades.pool(1)) {
			known.add(rune.id());
		}
		assertTrue(RuneTrades.reroll(1, known, 5).isEmpty(), "nothing left to learn in the tier means no reroll");
		String left = RuneTrades.pool(1).getFirst().id();
		known.remove(left);
		assertEquals(Optional.of(left), RuneTrades.reroll(1, known, 5).map(RuneDef::id));
		known.clear();
		assertEquals(RuneTrades.reroll(2, known, 42), RuneTrades.reroll(2, known, 42), "the same seed keeps the same offer");
		for (long seed = 0; seed < 200; seed++) {
			RuneDef pick = RuneTrades.reroll(2, Set.of(Runes.BEAM.id()), seed).orElseThrow();
			assertEquals(2, pick.tier());
			assertNotEquals(Runes.BEAM.id(), pick.id());
		}
	}

	// ------------------------------------------------------------------ contracts

	@Test
	void boardsAreThreeDifferentContractsThatChangeDaily() {
		ContractRules.Board board = ContractRules.generate(10, 99);
		assertEquals(ContractRules.COUNT, board.contracts().size());
		assertEquals(3, board.contracts().stream().map(ContractRules.Contract::kind).distinct().count());
		assertEquals(board, ContractRules.generate(10, 99), "the same day and player always get the same board");
		assertSame(board, ContractRules.today(board, 10, 99));
		assertEquals(11, ContractRules.today(board, 11, 99).day());
		boolean differs = false;
		for (long day = 11; day < 30 && !differs; day++) {
			differs = !ContractRules.generate(day, 99).contracts().equals(board.contracts());
		}
		assertTrue(differs, "boards should change from day to day");
		for (long seed = 0; seed < 100; seed++) {
			for (ContractRules.Contract c : ContractRules.generate(seed % 7, seed).contracts()) {
				assertTrue(c.target() >= 1);
				assertEquals(0, c.progress());
				assertFalse(c.claimed());
				assertTrue(ContractRules.KINDS.contains(c.kind()));
				ContractRules.Reward reward = ContractRules.Reward.parse(c.reward());
				assertTrue(Set.of("emerald", "blank_rune", "mana_crystal", "rune").contains(reward.type()), c.reward());
				assertTrue(reward.amount() >= 1);
			}
		}
	}

	@Test
	void contractsCountUpAndPayOnce() {
		ContractRules.Board board = new ContractRules.Board(3, List.of(
			new ContractRules.Contract(ContractRules.RUNEBOUND, "frost", 2, 0, false, "rune:2"),
			new ContractRules.Contract(ContractRules.LEY, "", 3, 0, false, "emerald:8"),
			new ContractRules.Contract(ContractRules.REACTION, "conduct", 1, 0, false, "blank_rune:6")));

		// The wrong element, or a kind nobody asked for, counts for nothing.
		assertFalse(ContractRules.progress(board, ContractRules.RUNEBOUND, "fire", 1).changed());
		assertFalse(ContractRules.progress(board, ContractRules.IMBUE, "", 1).changed());

		ContractRules.Progress p = ContractRules.progress(board, ContractRules.RUNEBOUND, "frost", 1);
		assertEquals(List.of(0), p.advanced());
		assertTrue(p.finished().isEmpty());
		p = ContractRules.progress(p.board(), ContractRules.RUNEBOUND, "frost", 1);
		assertEquals(List.of(0), p.finished());
		// Finished contracts stop counting.
		assertFalse(ContractRules.progress(p.board(), ContractRules.RUNEBOUND, "frost", 1).changed());
		// Progress never passes the target.
		p = ContractRules.progress(p.board(), ContractRules.LEY, "", 10);
		assertEquals(3, p.board().contracts().get(1).progress());

		ContractRules.Claim claim = ContractRules.claim(p.board());
		assertEquals(List.of(new ContractRules.Reward("rune", 2), new ContractRules.Reward("emerald", 8)), claim.rewards());
		assertTrue(claim.board().contracts().get(0).claimed());
		assertFalse(claim.board().contracts().get(2).claimed(), "an unfinished contract isn't handed in");
		assertTrue(ContractRules.claim(claim.board()).rewards().isEmpty(), "a contract pays once");

		assertEquals(new ContractRules.Reward("emerald", 8), ContractRules.Reward.parse("emerald:8"));
		assertEquals(new ContractRules.Reward("mana_crystal", 1), ContractRules.Reward.parse("mana_crystal"));
	}

	@Test
	void finishedContractsWaitThroughTheDawnUntilHandedIn() {
		ContractRules.Contract finished = new ContractRules.Contract(ContractRules.RUNEBOUND, "frost", 2, 2, false, "rune:2");
		ContractRules.Board yesterday = new ContractRules.Board(3, List.of(
			finished,
			new ContractRules.Contract(ContractRules.LEY, "", 3, 3, true, "emerald:8"),
			new ContractRules.Contract(ContractRules.REACTION, "conduct", 2, 1, false, "blank_rune:6")));
		for (long seed = 0; seed < 50; seed++) {
			ContractRules.Board today = ContractRules.today(yesterday, 4, seed);
			assertEquals(4, today.day());
			assertEquals(ContractRules.COUNT, today.contracts().size());
			assertEquals(finished, today.contracts().getFirst(), "a finished contract not handed in stays on the board");
			assertEquals(ContractRules.COUNT, today.contracts().stream().map(ContractRules.Contract::kind).distinct().count());
			assertTrue(today.contracts().stream().noneMatch(ContractRules.Contract::claimed), "handed-in contracts go at dawn");
			assertEquals(List.of(new ContractRules.Reward("rune", 2)), ContractRules.claim(today).rewards());
			// Once handed in, the next dawn brings a whole new board.
			ContractRules.Board next = ContractRules.today(ContractRules.claim(today).board(), 5, seed);
			assertEquals(ContractRules.generate(5, seed), next);
		}
		// A board of nothing but finished contracts is kept whole, never grown past three.
		ContractRules.Board full = ContractRules.generate(7, 1);
		List<ContractRules.Contract> done = new java.util.ArrayList<>();
		for (ContractRules.Contract c : full.contracts()) {
			done.add(c.advance(c.target()));
		}
		ContractRules.Board kept = ContractRules.today(new ContractRules.Board(7, done), 8, 1);
		assertEquals(done, kept.contracts());
		assertEquals(8, kept.day());
	}

	// ------------------------------------------------------------------ duels

	@Test
	void duelCountsDownThenFightsToAKnockout() {
		DuelRules.Duel duel = new DuelRules.Duel(A, B, 100);
		assertEquals(DuelRules.Phase.COUNTDOWN, duel.phase());
		assertEquals(3, duel.countdown(100));
		assertEquals(2, duel.countdown(121));
		assertEquals(1, duel.countdown(159));
		assertFalse(duel.fighting());
		// A knockout in the countdown doesn't count.
		duel.knockout(B);
		assertEquals(DuelRules.Phase.COUNTDOWN, duel.phase());
		assertFalse(duel.tick(159));
		assertTrue(duel.tick(160));
		assertEquals(0, duel.countdown(160));
		assertTrue(duel.fighting());
		assertEquals(B, duel.opponent(A));
		assertNull(duel.opponent(C));
		duel.knockout(C);
		assertTrue(duel.fighting(), "an outsider can't be knocked out of someone else's duel");
		duel.knockout(B);
		assertEquals(DuelRules.Phase.OVER, duel.phase());
		assertEquals(A, duel.winner());
		assertEquals(B, duel.loser());
		assertEquals(DuelRules.Ending.KNOCKOUT, duel.ending());
		// Over is over.
		duel.forfeit(A, DuelRules.Ending.LOGGED_OFF);
		assertEquals(A, duel.winner());
	}

	@Test
	void leavingLoggingOffOrDyingForfeits() {
		for (DuelRules.Ending why : List.of(DuelRules.Ending.LEFT_AREA, DuelRules.Ending.LOGGED_OFF, DuelRules.Ending.DIED)) {
			DuelRules.Duel duel = new DuelRules.Duel(A, B, 0);
			duel.forfeit(A, why);
			assertEquals(DuelRules.Phase.OVER, duel.phase(), "a forfeit counts even in the countdown");
			assertEquals(B, duel.winner());
			assertEquals(A, duel.loser());
			assertEquals(why, duel.ending());
		}
		DuelRules.Duel draw = new DuelRules.Duel(A, B, 0);
		draw.tick(DuelRules.COUNTDOWN_TICKS);
		assertTrue(draw.tick(DuelRules.COUNTDOWN_TICKS + DuelRules.MAX_FIGHT_TICKS));
		assertEquals(DuelRules.Ending.DRAW, draw.ending());
		assertNull(draw.winner());

		assertThrows(IllegalArgumentException.class, () -> new DuelRules.Duel(A, A, 0));
		assertFalse(DuelRules.outside(0, 0, 30, 20));
		assertTrue(DuelRules.outside(0, 0, 30, 30));
		DuelRules.Challenge challenge = new DuelRules.Challenge(A, B, 0);
		assertFalse(challenge.expired(DuelRules.CHALLENGE_TICKS));
		assertTrue(challenge.expired(DuelRules.CHALLENGE_TICKS + 1));
	}

	// ------------------------------------------------------------------ chorus

	private static ChorusRules.Voice voice(UUID caster, String shape, long time, double x, double aimX, UUID target) {
		return voice(caster, shape, time, x, aimX, target, ChorusRules.HARMFUL);
	}

	private static ChorusRules.Voice voice(UUID caster, String shape, long time, double x, double aimX, UUID target, String kind) {
		return new ChorusRules.Voice(caster, shape, time, "minecraft:overworld", x, 64, 0, aimX, 64, 10, target, kind);
	}

	@Test
	void chorusNeedsTheSameShapePlaceAndMoment() {
		String bolt = Runes.BOLT.id();
		ChorusRules.Voice first = voice(A, bolt, 0, 0, 0, null);
		assertTrue(ChorusRules.joins(first, voice(B, bolt, 20, 3, 2, null)));
		assertFalse(ChorusRules.joins(first, voice(B, bolt, 21, 3, 2, null)), "more than a second later");
		assertFalse(ChorusRules.joins(first, voice(B, Runes.BEAM.id(), 5, 3, 2, null)), "a different shape");
		assertFalse(ChorusRules.joins(first, voice(B, bolt, 5, 13, 2, null)), "too far apart");
		assertFalse(ChorusRules.joins(first, voice(B, bolt, 5, 3, 8, null)), "aimed somewhere else");
		assertFalse(ChorusRules.joins(first, voice(A, bolt, 5, 0, 0, null)), "one caster is no chorus");
		assertFalse(ChorusRules.joins(voice(A, Runes.SELF.id(), 0, 0, 0, null), voice(B, Runes.SELF.id(), 1, 1, 0, null)), "Self is always solo");
		assertFalse(ChorusRules.joins(first, new ChorusRules.Voice(B, bolt, 5, "minecraft:the_nether", 3, 64, 0, 2, 64, 10, null, ChorusRules.HARMFUL)));
		// The same foe counts even when the aim points drift apart.
		assertTrue(ChorusRules.joins(voice(A, bolt, 0, 0, 0, D), voice(B, bolt, 5, 3, 9, D)));
		// Aiming at each other isn't singing together.
		assertFalse(ChorusRules.joins(voice(A, bolt, 0, 0, 0, B), voice(B, bolt, 5, 3, 1, null)));
	}

	@Test
	void choirGrowsToThreeVoicesAndFades() {
		String burst = Runes.BURST.id();
		ChorusRules.Choir choir = new ChorusRules.Choir();
		assertEquals(1, choir.offer(voice(A, burst, 0, 0, 0, null)).size());
		List<ChorusRules.Voice> two = choir.offer(voice(B, burst, 10, 2, 1, null));
		assertEquals(List.of(A, B), two.stream().map(ChorusRules.Voice::caster).toList());
		// A voice already in the chorus starts a new one instead of joining twice.
		assertEquals(1, choir.offer(voice(A, burst, 12, 0, 0, null)).size());
		List<ChorusRules.Voice> three = choir.offer(voice(C, burst, 25, 4, 1, null));
		assertEquals(3, three.size(), "within a second of the last voice, not the first");
		assertEquals(C, three.getLast().caster());
		choir.forget(100);
		assertEquals(0, choir.size());

		assertEquals(1.0, ChorusRules.power(1));
		assertEquals(1.5, ChorusRules.power(2));
		assertEquals(2.0, ChorusRules.power(3));
		assertEquals(2.0, ChorusRules.power(6), "capped");
		assertEquals(0, ChorusRules.extra(1));
		assertEquals(2, ChorusRules.extra(5));
	}

	@Test
	void chorusOnlyJoinsAlliesSingingTheSameKindOfSpell() {
		String burst = Runes.BURST.id();
		ChorusRules.Voice harm = voice(A, burst, 0, 0, 0, null, ChorusRules.HARMFUL);
		assertFalse(ChorusRules.joins(harm, voice(B, burst, 5, 2, 1, null, ChorusRules.HELPFUL)), "a heal doesn't join an attack");
		assertTrue(ChorusRules.joins(voice(A, burst, 0, 0, 0, null, ChorusRules.HELPFUL), voice(B, burst, 5, 2, 1, null, ChorusRules.HELPFUL)));
		assertFalse(ChorusRules.joins(voice(A, burst, 0, 0, 0, null, ChorusRules.MIXED), voice(B, burst, 5, 2, 1, null, ChorusRules.MIXED)),
			"a spell that both harms and heals sings alone");
		assertEquals(ChorusRules.MIXED, ChorusRules.kind(true, true));
		assertEquals(ChorusRules.HELPFUL, ChorusRules.kind(false, true));
		assertEquals(ChorusRules.OTHER, ChorusRules.kind(false, false));

		// Someone who isn't an ally starts a chorus of their own instead of taking over another's.
		ChorusRules.Choir choir = new ChorusRules.Choir();
		java.util.function.BiPredicate<UUID, UUID> allies = (x, y) -> !(x.equals(C) || y.equals(C));
		assertEquals(1, choir.offer(voice(A, burst, 0, 0, 0, null), allies).size());
		assertEquals(1, choir.offer(voice(C, burst, 5, 2, 1, null), allies).size(), "a rival can't join");
		assertEquals(2, choir.offer(voice(B, burst, 10, 1, 1, null), allies).size(), "an ally still can");
		// A chorus with a rival in it can't be joined by the rival's foe either.
		ChorusRules.Choir other = new ChorusRules.Choir();
		other.offer(voice(C, burst, 0, 0, 0, null), allies);
		assertEquals(1, other.offer(voice(A, burst, 5, 2, 1, null), allies).size());
	}

	// ------------------------------------------------------------------ farming the Runesmith

	@Test
	void runesmithNeverTakesConjuredRankedOrUncommonRunes() {
		assertTrue(RuneTrades.pool(3).contains(Runes.LIGHTNING), "the shelf still sells Lightning");
		assertFalse(RuneTrades.takes(Runes.LIGHTNING), "a lightning rod makes Lightning runes from blanks: never bought back");
		assertFalse(RuneTrades.takes(Runes.KINDLING), "an innate rune");
		assertTrue(RuneTrades.takes(Runes.HEAL));
		for (RuneDef rune : Runes.all()) {
			if (RuneTrades.takes(rune)) {
				assertTrue(RuneTrades.pool(rune.tier()).contains(rune), rune.id() + " is taken but never sold");
			}
		}
		Map<String, Integer> held = new LinkedHashMap<>();
		held.put(Runes.LIGHTNING.id(), 64);
		held.put(Runes.HEAL.id(), 1);
		assertEquals(List.of(Runes.HEAL.id()), RuneTrades.buybacks(held), "Lightning is never offered for");
		assertTrue(RuneTrades.rerolls(Map.of(Runes.LIGHTNING.id(), 64)).isEmpty(), "two Lightning runes don't pay for a reroll");

		assertTrue(RuneTrades.plainRank(null));
		assertTrue(RuneTrades.plainRank(1));
		assertFalse(RuneTrades.plainRank(2));
		assertFalse(RuneTrades.plainRank(3));

		assertEquals(RuneTrades.DAILY_BUYBACKS, RuneTrades.buybacksLeft(Long.MIN_VALUE, 0, 5), "nothing sold yet");
		assertEquals(RuneTrades.DAILY_BUYBACKS - 3, RuneTrades.buybacksLeft(5, 3, 5));
		assertEquals(0, RuneTrades.buybacksLeft(5, RuneTrades.DAILY_BUYBACKS + 2, 5));
		assertEquals(RuneTrades.DAILY_BUYBACKS, RuneTrades.buybacksLeft(5, RuneTrades.DAILY_BUYBACKS, 6), "a new day, a fresh allowance");
	}

	@Test
	void turningTimeBackKeepsTheBoard() {
		ContractRules.Board board = ContractRules.generate(10, 99);
		ContractRules.Board claimed = new ContractRules.Board(10, board.contracts().stream()
			.map(c -> new ContractRules.Contract(c.kind(), c.arg(), c.target(), c.target(), true, c.reward())).toList());
		assertSame(claimed, ContractRules.today(claimed, 9, 99), "time turned back mustn't bring handed-in contracts back");
		assertSame(claimed, ContractRules.today(claimed, 0, 99));
		assertEquals(11, ContractRules.today(claimed, 11, 99).day(), "a later day brings a new board");
		assertEquals(3, ContractRules.today(ContractRules.Board.EMPTY, 3, 99).day());
	}

	@Test
	void castsAndReactionsCountOnlyWhenTheyLandOnSomething() {
		ContractRules.Credit credit = new ContractRules.Credit();
		// Casting at nothing: nothing counts, however many times.
		for (long t = 0; t < 10; t++) {
			assertTrue(credit.cast(t * 20, Set.of("fire"), true).isEmpty());
		}
		// Far later a spell lands: only a cast still in the air could have done it, and those are long gone.
		assertTrue(credit.hit(1000).casts().isEmpty());
		assertTrue(credit.cast(1000, Set.of("fire"), false).casts().size() == 1, "an instant spell that hit in its own tick counts at once");

		// A bolt: cast, then it lands a few ticks later. One spell striking three creatures credits one cast.
		assertTrue(credit.cast(2000, Set.of("frost"), true).isEmpty());
		ContractRules.Credit.Credited landed = credit.hit(2005);
		assertEquals(1, landed.casts().size());
		assertEquals(Set.of("frost"), landed.casts().getFirst().elements());
		assertTrue(landed.casts().getFirst().ley());
		assertTrue(credit.hit(2005).isEmpty());
		assertTrue(credit.hit(2005).isEmpty());
		// A cast that waits too long for a hit is forgotten.
		credit.cast(3000, Set.of("storm"), false);
		assertTrue(credit.hit(3000 + ContractRules.Credit.CAST_WINDOW + 1).casts().isEmpty());

		// Reactions count only when a hit lands in the same tick, before or after.
		assertTrue(credit.reaction(4000, "conduct").isEmpty());
		assertEquals(List.of("conduct"), credit.hit(4000).reactions());
		assertEquals(List.of("shatter"), credit.reaction(4000, "shatter").reactions());
		assertTrue(credit.reaction(4100, "conduct").isEmpty());
		assertTrue(credit.hit(4101).reactions().isEmpty(), "a reaction on a dummy, with a hit a tick later, doesn't count");
		assertTrue(credit.idle(5000));
	}

	// ------------------------------------------------------------------ duels: no free heal, no immunity

	@Test
	void duelsNeedCalmAndPutEveryoneBackAsTheyWere() {
		long now = 10_000;
		assertEquals(DuelRules.Refusal.NONE, DuelRules.ready(now, DuelRules.NEVER, DuelRules.NEVER, DuelRules.NEVER));
		assertEquals(DuelRules.Refusal.HURT, DuelRules.ready(now, now - 5, DuelRules.NEVER, DuelRules.NEVER));
		assertEquals(DuelRules.Refusal.NONE, DuelRules.ready(now, now - DuelRules.HURT_TICKS, DuelRules.NEVER, DuelRules.NEVER));
		assertEquals(DuelRules.Refusal.PVP, DuelRules.ready(now, now - 5, now - 300, DuelRules.NEVER), "a fight with another player comes first");
		assertEquals(DuelRules.Refusal.COOLDOWN, DuelRules.ready(now, DuelRules.NEVER, DuelRules.NEVER, now - 100));
		assertEquals(DuelRules.Refusal.NONE, DuelRules.ready(now, DuelRules.NEVER, DuelRules.NEVER, now - DuelRules.DUEL_COOLDOWN_TICKS));
		assertFalse(DuelRules.within(now, now + 50, 200), "a clock that went backwards isn't 'recently'");

		// Health and mana go back to what they were, never more; gains made meanwhile are kept.
		assertEquals(6F, DuelRules.restored(1F, 6F, 20F));
		assertEquals(9F, DuelRules.restored(9F, 6F, 20F));
		assertEquals(20F, DuelRules.restored(1F, 30F, 20F), "never over the most");
		// Effects come back with the time the duel took off them.
		assertEquals(400, DuelRules.remaining(1000, 600));
		assertEquals(0, DuelRules.remaining(500, 600));
		assertEquals(-1, DuelRules.remaining(-1, 600), "an endless effect stays endless");

		DuelRules.Duel duel = new DuelRules.Duel(A, B, 0);
		duel.tick(DuelRules.COUNTDOWN_TICKS);
		duel.interrupt();
		assertEquals(DuelRules.Phase.OVER, duel.phase());
		assertEquals(DuelRules.Ending.INTERRUPTED, duel.ending());
		assertNull(duel.winner(), "an interrupted duel counts for nobody");
		assertNull(duel.loser());
	}
}
