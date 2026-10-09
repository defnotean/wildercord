package dev.wildercord.spell;

import dev.wildercord.content.CordTier;
import dev.wildercord.content.Imbued;
import dev.wildercord.content.ScrollSpell;
import dev.wildercord.player.Spellbook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercise the actual data constructors, where saved or command-forged input is bounded. */
class RelayStorageBoundsTest {
	@Test
	void anOverlongActiveOrPassiveRowRemainsRefusedAfterLoading() {
		List<String> active = ordinaryPrefix(CordTier.MAX_SOCKETS);
		active.add(RELAY.id());
		Spellbook book = new Spellbook(List.of(SELF.id(), HEAL.id()), List.of(active), 0, false,
			List.of(List.of(SELF.id(), SWIFT.id(), RELAY.id())), 0, List.of());
		assertRefused(book.spells().getFirst());
		assertRefused(book.passives().getFirst());
		assertTrue(book.spells().get(1).isEmpty(), "sizing of unrelated slots stays unchanged");
	}

	@Test
	void anOverlongScrollCannotDropItsForbiddenRelaySuffix() {
		List<String> raw = ordinaryPrefix(ScrollSpell.MAX_RUNES);
		raw.add(RELAY.id());
		assertRefused(new ScrollSpell(raw, "Forged", "Test").runes());
		Imbued imbued = new Imbued(raw, 3, 0, false, Imbued.NOBODY, 0);
		assertRefused(imbued.runes());
		assertRefused(imbued.withCharges(2).runes());
	}

	@Test
	void existingOrdinaryRowsAndExactRelayRowsKeepTheirContent() {
		List<String> active = List.of(RELAY.id(), HARM.id());
		Spellbook book = new Spellbook(List.of(), List.of(active), 0, false);
		assertEquals(active, book.spells().getFirst());
		List<String> ordinary = ordinaryPrefix(ScrollSpell.MAX_RUNES + 1);
		assertEquals(ordinary.subList(0, ScrollSpell.MAX_RUNES), new ScrollSpell(ordinary, "", "").runes());
	}

	private static List<String> ordinaryPrefix(int length) {
		List<String> ids = new ArrayList<>(List.of(SELF.id()));
		while (ids.size() < length) ids.add(HEAL.id());
		return ids;
	}

	private static void assertRefused(List<String> ids) {
		assertTrue(RelayRules.containsIds(ids));
		assertTrue(SpellCompiler.compile(ids.stream().map(id -> Runes.get(id).orElseThrow()).toList()).isEmpty());
	}
}
