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

/** Saved and command-forged rows must remain refused after actual storage constructors bound them. */
class ReweaveStorageBoundsTest {
    @Test void activeAndPassiveRowsCannotLoseARestrictedSuffixWhenLoaded() {
        List<String> active = ordinaryPrefix(CordTier.MAX_SOCKETS);
        active.add(REWEAVE.id());
        Spellbook book = new Spellbook(List.of(SELF.id(), HEAL.id()), List.of(active), 0, false,
            List.of(List.of(SELF.id(), SWIFT.id(), REWEAVE.id())), 0, List.of());
        assertRefused(book.spells().getFirst());
        assertRefused(book.passives().getFirst());
        assertTrue(book.spells().get(1).isEmpty());
        assertFalse(book.knows(REWEAVE.id()), "storing a code does not teach its rune");
    }

    @Test void scrollAndImbuedRowsCannotDropReweaveOrItsForbiddenModifiers() {
        for (boolean suffix : List.of(true, false)) {
            List<String> raw = suffix ? ordinaryPrefix(ScrollSpell.MAX_RUNES) : new ArrayList<>(ReweaveRules.IDS);
            while (raw.size() <= ScrollSpell.MAX_RUNES) raw.add(suffix ? REWEAVE.id() : AMPLIFY.id());
            assertRefused(new ScrollSpell(raw, "Forged", "Test").runes());
            Imbued imbued = new Imbued(raw, 3, 0, false, Imbued.NOBODY, 0);
            assertRefused(imbued.runes());
            assertRefused(imbued.withCharges(2).runes());
        }
    }

    @Test void importsAndInvalidEditableDraftsKeepTheirContentWithoutGrantingKnowledge() {
        List<String> invalid = List.of(REWEAVE.id(), HARM.id(), AMPLIFY.id());
        Spellbook book = new Spellbook(List.of(HARM.id()), List.of(invalid), 0, false);
        assertEquals(invalid, book.spells().getFirst());
        assertFalse(book.knows(REWEAVE.id()));
        List<String> imported = SpellCodes.decode("wc:reweave.harm");
        Spellbook updated = book.withSpell(1, imported);
        assertEquals(ReweaveRules.IDS, updated.spells().get(1));
        assertEquals(book.learned(), updated.learned());
        assertFalse(updated.knows(REWEAVE.id()));
        assertEquals(List.of(RELAY.id(), HARM.id()), updated.withSpell(2, List.of(RELAY.id(), HARM.id())).spells().get(2));
        assertEquals(ReweaveRules.IDS, new ScrollSpell(ReweaveRules.IDS, "", "").runes());
        assertTrue(SpellCompiler.compileStored(ReweaveRules.RUNES).isEmpty(), "preservation does not permit stored casting");
    }

    private static List<String> ordinaryPrefix(int length) {
        List<String> ids = new ArrayList<>(List.of(SELF.id()));
        while (ids.size() < length) ids.add(HEAL.id());
        return ids;
    }
    private static void assertRefused(List<String> ids) {
        assertTrue(ReweaveRules.containsIds(ids));
        assertTrue(SpellCompiler.compile(ids.stream().map(id -> Runes.get(id).orElseThrow()).toList()).isEmpty());
    }
}
