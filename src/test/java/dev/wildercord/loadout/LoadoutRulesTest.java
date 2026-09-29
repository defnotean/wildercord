package dev.wildercord.loadout;

import dev.wildercord.cast.PassiveCaster;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.gear.SpellSlots;
import dev.wildercord.player.Spellbook;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The loadout rules: names, the limit, what stops a load, the quick-switch order, which spells a load
 * puts on their cooldown, and what a loaded loadout does with runes the player doesn't know, runes too
 * strong for the Cord worn, and sockets and rows that Cord doesn't have.
 */
class LoadoutRulesTest {
	private static List<String> ids(RuneDef... runes) {
		return Arrays.stream(runes).map(RuneDef::id).toList();
	}

	// ------------------------------------------------------------------ names and the limit

	@Test
	void namesAreTrimmedAndPlainButKeepTheirCase() {
		assertEquals("Mining", LoadoutRules.name("  Mining "));
		assertEquals("Boss fight 2", LoadoutRules.name("Boss fight 2"));
		// Formatting codes and control characters go.
		assertEquals("Red", LoadoutRules.name("§cRed"));
		assertEquals("ab", LoadoutRules.name("a\u0007b\n"));
		assertNull(LoadoutRules.name(""));
		assertNull(LoadoutRules.name("   "));
		assertNull(LoadoutRules.name("§c"));
		assertNull(LoadoutRules.name(null));
		// Long names are cut to the limit, not refused.
		assertEquals("x".repeat(LoadoutRules.MAX_NAME), LoadoutRules.name("x".repeat(LoadoutRules.MAX_NAME + 10)));
	}

	@Test
	void namesAreFoundWhateverTheirCase() {
		List<String> names = List.of("Mining", "Boss Fight");
		assertEquals(0, LoadoutRules.find(names, "mining"));
		assertEquals(1, LoadoutRules.find(names, "BOSS FIGHT"));
		assertEquals(-1, LoadoutRules.find(names, "Exploring"));
		assertEquals(-1, LoadoutRules.find(names, null));
	}

	@Test
	void sixCanBeKeptAndAnyCanBeSavedOver() {
		assertEquals(6, LoadoutRules.MAX);
		List<String> names = new ArrayList<>();
		for (int i = 0; i < LoadoutRules.MAX; i++) {
			assertEquals(LoadoutRules.Save.NEW, LoadoutRules.save(names, "Loadout " + i, LoadoutRules.MAX));
			names.add("Loadout " + i);
		}
		assertEquals(LoadoutRules.Save.FULL, LoadoutRules.save(names, "One more", LoadoutRules.MAX));
		// A full list can still be saved over by name.
		assertEquals(LoadoutRules.Save.REPLACE, LoadoutRules.save(names, "loadout 3", LoadoutRules.MAX));
	}

	// ------------------------------------------------------------------ what stops a load, and the quick switch

	@Test
	void loadingIsRefusedWhileChargingDuellingOrSealed() {
		assertEquals(LoadoutRules.Block.NONE, LoadoutRules.block(true, true, false, false, false));
		assertEquals(LoadoutRules.Block.NO_CORD, LoadoutRules.block(false, true, true, true, true));
		assertEquals(LoadoutRules.Block.DEAD, LoadoutRules.block(true, false, false, false, false));
		assertEquals(LoadoutRules.Block.CHARGING, LoadoutRules.block(true, true, true, false, false));
		assertEquals(LoadoutRules.Block.DUEL, LoadoutRules.block(true, true, false, true, false));
		assertEquals(LoadoutRules.Block.SEALED, LoadoutRules.block(true, true, false, false, true));
	}

	@Test
	void theQuickSwitchGoesRoundInOrder() {
		assertEquals(-1, LoadoutRules.next(-1, 0));
		assertEquals(0, LoadoutRules.next(-1, 3));
		assertEquals(1, LoadoutRules.next(0, 3));
		assertEquals(2, LoadoutRules.next(1, 3));
		assertEquals(0, LoadoutRules.next(2, 3));
		assertEquals(0, LoadoutRules.next(0, 1));
		// A current past the end (one was deleted) starts over.
		assertEquals(0, LoadoutRules.next(5, 3));
	}

	@Test
	void deletingKeepsTrackOfTheOneLastLoaded() {
		assertEquals(1, LoadoutRules.afterDelete(1, 3));
		assertEquals(-1, LoadoutRules.afterDelete(2, 2));
		assertEquals(2, LoadoutRules.afterDelete(3, 0));
		assertEquals(-1, LoadoutRules.afterDelete(-1, 0));
	}

	// ------------------------------------------------------------------ cooldowns

	@Test
	void changedSpellsStartTheirCooldownUnlessAlreadyCooling() {
		long now = 1000;
		List<List<String>> before = List.of(ids(Runes.BOLT, Runes.HARM), ids(Runes.SELF, Runes.SWIFT), ids(Runes.BOLT), List.of(), ids(Runes.BOLT));
		List<List<String>> after = List.of(ids(Runes.BOLT, Runes.FIRE), ids(Runes.SELF, Runes.SWIFT), ids(Runes.BOLT, Runes.PUSH), ids(Runes.BOLT), List.of());
		// Spell 3 is cooling down already (ready at 1100); the others are ready.
		long[] readyAt = {0, 0, 1100, 0, 900};
		// As the server works it out: nothing to cast, no cooldown.
		long[] ready = LoadoutRules.readyAfterLoad(before, after, readyAt, now, slot -> after.get(slot).isEmpty() ? 0 : 40 + slot);
		assertEquals(now + 40, ready[0], "a changed spell that was ready starts its cooldown");
		assertEquals(0, ready[1], "an unchanged spell stays ready");
		assertEquals(1100, ready[2], "a spell cooling down keeps its time, neither reset nor stretched");
		assertEquals(now + 43, ready[3], "an empty spell filled starts its cooldown");
		assertEquals(900, ready[4], "a spell emptied has nothing to cool down");
		assertArrayEquals(new long[] {0, 0, 1100, 0, 900}, readyAt, "the times passed in aren't changed");
	}

	@Test
	void aSpellWithNothingToCastNeverCoolsDown() {
		long[] ready = LoadoutRules.readyAfterLoad(List.of(List.of()), List.of(ids(Runes.AMPLIFY)), new long[] {0}, 50, slot -> 0);
		assertEquals(0, ready[0]);
	}

	// ------------------------------------------------------------------ what a load does with quiet runes

	/** An Echo Cord's setup: a Tier III rune, twelve sockets in spell 1, four spells, the tome's fifth and a passive. */
	private static Spellbook echoBook(List<String> learned) {
		Spellbook book = new Spellbook(learned, List.of(), 0, true);
		List<String> twelve = new ArrayList<>(ids(Runes.BOLT, Runes.HARM, Runes.LIGHTNING));
		while (twelve.size() < CordTier.MAX_SOCKETS) {
			twelve.add(Runes.PUSH.id());
		}
		return book.withSpell(0, twelve)
			.withSpell(1, ids(Runes.SELF, Runes.SWIFT))
			.withSpell(3, ids(Runes.BOLT, Runes.FIRE))
			.withSpell(SpellSlots.TOME, ids(Runes.BOLT, Runes.PUSH))
			.withName(1, "Zoom")
			.withPassive(0, ids(Runes.SELF, Runes.NIGHT_EYE))
			.withPassiveOn(0, false)
			.withSelected(3);
	}

	@Test
	void aLoadoutRemembersTheWholeCord() {
		List<String> learned = ids(Runes.BOLT, Runes.HARM, Runes.LIGHTNING, Runes.PUSH, Runes.SELF, Runes.SWIFT, Runes.FIRE, Runes.NIGHT_EYE);
		Spellbook book = echoBook(learned);
		Loadout loadout = Loadout.of("Everything", book);
		Spellbook cleared = new Spellbook(learned, List.of(), 0, true);
		Spellbook loaded = loadout.applyTo(cleared);
		assertEquals(book.spells(), loaded.spells());
		assertEquals(book.names(), loaded.names());
		assertEquals(book.passives(), loaded.passives());
		assertEquals(book.passivesOff(), loaded.passivesOff());
		assertEquals(3, loaded.selected());
		assertEquals(book, loaded);
	}

	@Test
	void loadingNeverTeachesARuneAndUnknownRunesStayQuiet() {
		List<String> learned = ids(Runes.BOLT, Runes.HARM, Runes.LIGHTNING, Runes.PUSH, Runes.SELF, Runes.SWIFT, Runes.FIRE, Runes.NIGHT_EYE);
		Loadout loadout = Loadout.of("Everything", echoBook(learned));
		// Someone who knows Bolt and Push, but not Harm or Lightning.
		List<String> fewer = ids(Runes.BOLT, Runes.PUSH);
		Spellbook loaded = loadout.applyTo(new Spellbook(fewer, List.of(), 0, true));
		assertEquals(fewer, loaded.learned(), "loading must never teach a rune");
		assertEquals(loadout.spells().get(0), loaded.spells().get(0), "unknown runes stay threaded");
		List<Integer> firing = SpellCaster.activeSockets(loaded.spells().get(0), loaded, 0, CordTier.ECHO);
		assertFalse(firing.contains(1), "Harm isn't known, so it's quiet");
		assertFalse(firing.contains(2), "Lightning isn't known, so it's quiet");
		assertTrue(firing.contains(0) && firing.contains(3), "the runes known still fire");
		assertEquals(CordTier.MAX_SOCKETS - 2, firing.size());
	}

	@Test
	void aSmallerCordKeepsEverythingQuiet() {
		List<String> learned = ids(Runes.BOLT, Runes.HARM, Runes.LIGHTNING, Runes.PUSH, Runes.SELF, Runes.SWIFT, Runes.FIRE, Runes.NIGHT_EYE);
		Loadout loadout = Loadout.of("Everything", echoBook(learned));
		Spellbook loaded = loadout.applyTo(new Spellbook(learned, List.of(), 0, true));
		// On a Twine Cord: three sockets, one spell, Tier I only.
		CordTier twine = CordTier.TWINE;
		List<Integer> first = SpellCaster.activeSockets(loaded.spells().get(0), loaded, 0, twine);
		assertEquals(List.of(0, 1), first, "Lightning is too strong for Twine, and sockets past the third are quiet");
		assertEquals(CordTier.MAX_SOCKETS, loaded.spells().get(0).size(), "but every rune is kept");
		assertTrue(SpellCaster.activeSockets(loaded.spells().get(1), loaded, 1, twine).isEmpty(), "a row the Cord doesn't have is quiet");
		assertEquals(ids(Runes.SELF, Runes.SWIFT), loaded.spells().get(1), "and kept");
		// Fire (Tier II) on spell 4 is kept, but the row's quiet.
		assertEquals(ids(Runes.BOLT, Runes.FIRE), loaded.spells().get(3));
		assertTrue(SpellCaster.activeSockets(loaded.spells().get(3), loaded, 3, twine).isEmpty());
		// The passive keeps its runes and its switch.
		assertEquals(ids(Runes.SELF, Runes.NIGHT_EYE), loaded.passives().get(0));
		assertFalse(loaded.passiveOn(0));
		assertEquals(2, PassiveCaster.activeRunes(loaded.passives().get(0), loaded, twine).size());
		// Once a bigger Cord is worn again, it all fires.
		assertEquals(CordTier.MAX_SOCKETS, SpellCaster.activeSockets(loaded.spells().get(0), loaded, 0, CordTier.ECHO).size());
	}

	@Test
	void loadoutsKeepToTheSpellbooksSizes() {
		List<List<String>> huge = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			List<String> row = new ArrayList<>();
			for (int k = 0; k < 20; k++) {
				row.add(Runes.BOLT.id());
			}
			huge.add(row);
		}
		Loadout loadout = new Loadout("  §eBig  ", huge, List.of("x".repeat(60)), huge, -1, 7);
		assertEquals("Big", loadout.name());
		assertEquals(SpellSlots.ALL, loadout.spells().size());
		assertTrue(loadout.spells().stream().allMatch(row -> row.size() == CordTier.MAX_SOCKETS));
		assertEquals(dev.wildercord.spell.Passives.MAX, loadout.passives().size());
		assertTrue(loadout.passives().stream().allMatch(row -> row.size() == dev.wildercord.spell.Passives.SOCKETS));
		assertEquals(dev.wildercord.spell.SpellNames.MAX_LENGTH, loadout.names().getFirst().length());
		assertEquals(7 % SpellSlots.ALL, loadout.selected());
	}
}
