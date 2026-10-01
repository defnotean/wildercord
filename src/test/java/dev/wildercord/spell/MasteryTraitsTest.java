package dev.wildercord.spell;

import dev.wildercord.spell.MasteryTraits.Hook;
import dev.wildercord.spell.MasteryTraits.Profile;
import dev.wildercord.spell.MasteryTraits.Trait;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The trait catalogue and its offers: what a spell is made of, how it has been used, and the same offer every time. */
class MasteryTraitsTest {
	private static final Profile FIRE_BOLT = Profile.of(List.of(Runes.BOLT, Runes.FIRE));
	private static final Profile FROST_BOLT = Profile.of(List.of(Runes.BOLT, Runes.FROST));
	private static final Profile HEAL = Profile.of(List.of(Runes.BURST, Runes.HEAL));
	private static final Profile BLINK = Profile.of(List.of(Runes.BOLT, Runes.BLINK));

	@Test
	void theCatalogueIsLargeAndEveryTraitIsModest() {
		List<Trait> all = MasteryTraits.builtIn();
		assertTrue(all.size() >= 40, "only " + all.size() + " traits");
		Set<String> ids = new HashSet<>();
		for (Trait t : all) {
			assertTrue(ids.add(t.id()), "two traits are called " + t.id());
			assertTrue(t.id().matches("[a-z_]+"), t.id());
			assertFalse(t.name().isBlank());
			assertTrue(t.desc().endsWith("."), t.id() + ": " + t.desc());
			switch (t.hook()) {
				case COST, COOLDOWN -> assertTrue(t.amount() >= 0.85 && t.amount() < 1, t.id());
				case DAMAGE, HEALING -> assertTrue(t.amount() > 1 && t.amount() <= 1.15, t.id());
				case CHARGE, RANGE -> assertTrue(t.amount() > 1 && t.amount() <= 1.25, t.id());
				case CHAIN -> assertTrue(t.amount() <= 0.2, t.id());
				case LEECH -> assertTrue(t.amount() <= 0.1, t.id());
				default -> {}
			}
			if (!t.circumstance().isEmpty()) {
				assertTrue(MasteryTraits.CIRCUMSTANCES.contains(t.circumstance()), t.id() + " asks for " + t.circumstance());
			}
		}
		// Every kind of hook the code answers is used, so none is left untested in play.
		Set<Hook> used = new HashSet<>();
		all.forEach(t -> used.add(t.hook()));
		assertEquals(Set.of(Hook.values()), used);
	}

	@Test
	void aSpellIsReadForWhatItIsMadeOf() {
		assertEquals(Set.of("fire"), FIRE_BOLT.elements());
		assertEquals(Set.of("projectile"), FIRE_BOLT.shapes());
		assertTrue(FIRE_BOLT.harmful() && !FIRE_BOLT.helpful());
		assertTrue(HEAL.helpful() && !HEAL.harmful());
		assertTrue(BLINK.movement());
		// An effect with no shape before it has an unwritten Self.
		assertEquals(Set.of("personal"), Profile.of(List.of(Runes.SWIFT)).shapes());
	}

	@Test
	void offersAreFilteredByWhatTheSpellIsMadeOf() {
		Map<String, Integer> none = Map.of();
		for (Trait t : MasteryTraits.builtIn()) {
			double heal = MasteryTraits.weight(t, HEAL, none, 0);
			if (t.kind() == MasteryTraits.Kind.HARMFUL) {
				assertEquals(0.0, heal, t.id() + " was offered to a heal");
			}
			if (t.elements().equals(Set.of("fire"))) {
				assertEquals(0.0, MasteryTraits.weight(t, FROST_BOLT, Map.of("rain", 50), 50), t.id() + " was offered to frost");
			}
			if (t.kind() == MasteryTraits.Kind.HELPFUL) {
				assertEquals(0.0, MasteryTraits.weight(t, FIRE_BOLT, none, 0), t.id() + " was offered to a fire bolt");
			}
		}
		assertTrue(MasteryTraits.weight(MasteryTraits.WARM_HANDS, HEAL, none, 0) > 0);
		assertTrue(MasteryTraits.weight(MasteryTraits.FEATHERSTEP, BLINK, none, 0) > 0);
		assertEquals(0.0, MasteryTraits.weight(MasteryTraits.FEATHERSTEP, FIRE_BOLT, none, 0));
	}

	@Test
	void offersFollowHowTheSpellWasUsed() {
		// A fire spell never cast in the rain is never offered Undying Flame...
		assertEquals(0.0, MasteryTraits.weight(MasteryTraits.UNDYING_FLAME, FIRE_BOLT, Map.of(), 40));
		assertEquals(0.0, MasteryTraits.weight(MasteryTraits.UNDYING_FLAME, FIRE_BOLT, Map.of("rain", 3), 40));
		// ...but one cast in the rain half the time often is, and more the more it was.
		double some = MasteryTraits.weight(MasteryTraits.UNDYING_FLAME, FIRE_BOLT, Map.of("rain", 10), 40);
		double lots = MasteryTraits.weight(MasteryTraits.UNDYING_FLAME, FIRE_BOLT, Map.of("rain", 30), 40);
		assertTrue(some > 0 && lots > some);
		int offered = 0;
		for (long seed = 1; seed <= 200; seed++) {
			if (MasteryTraits.offer(FIRE_BOLT, Map.of("rain", 30), 40, seed, 0, 0, Set.of(), List.of()).contains(MasteryTraits.UNDYING_FLAME.id())) {
				offered++;
			}
		}
		assertTrue(offered >= 20, "Undying Flame offered for " + offered + " of 200 rainy fire spells");
		// A spell cast at night is offered a shadowy trait.
		assertTrue(MasteryTraits.weight(MasteryTraits.NIGHTSHADE, FIRE_BOLT, Map.of("night", 20), 30) > 0);
		assertEquals(0.0, MasteryTraits.weight(MasteryTraits.NIGHTSHADE, FIRE_BOLT, Map.of("night", 1), 30));
	}

	@Test
	void anOfferIsThreeDifferentTraitsAndTheSameEveryTime() {
		List<String> first = MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, 42L, 0, 0, Set.of(), List.of());
		assertEquals(3, first.size());
		assertEquals(3, new HashSet<>(first).size());
		assertEquals(first, MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, 42L, 0, 0, Set.of(), List.of()));
		// Another rank or a re-roll draws again.
		boolean differs = false;
		for (long seed = 1; seed <= 20 && !differs; seed++) {
			differs = !MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, seed, 0, 0, Set.of(), List.of())
				.equals(MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, seed, 0, 1, Set.of(), List.of()));
		}
		assertTrue(differs);
		// What a spell already has (or the offer being re-rolled) is never offered.
		List<String> again = MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, 42L, 0, 1, Set.copyOf(first), List.of());
		assertTrue(again.stream().noneMatch(first::contains), first + " then " + again);
	}

	@Test
	void everySpellCanBeOfferedAFullHand() {
		for (RuneDef rune : Runes.all()) {
			if (rune.family() != RuneFamily.EFFECT && rune.family() != RuneFamily.SHAPE) {
				continue;
			}
			Profile profile = Profile.of(List.of(rune));
			Set<String> taken = new HashSet<>();
			// Four ranks' worth of offers, each excluding what was already chosen.
			for (int slot = 0; slot < MasteryRules.SLOTS; slot++) {
				List<String> offer = MasteryTraits.offer(profile, Map.of(), 0, rune.id().hashCode(), slot, 0, taken, List.of());
				assertEquals(3, offer.size(), rune.id() + " slot " + slot);
				taken.add(offer.getFirst());
			}
		}
	}

	@Test
	void traitsFromOutsideCanBeOffered() {
		Trait quirk = MasteryTraits.custom("testmod:echoing", "Echoing", "It echoes.", Hook.COSMETIC, 1).param("bell");
		MasteryTraits.register(quirk);
		assertThrows(IllegalArgumentException.class, () -> MasteryTraits.register(quirk));
		assertThrows(IllegalArgumentException.class, () -> MasteryTraits.register(MasteryTraits.custom("plain", "Plain", "No namespace.", Hook.COST, 0.9)));
		// The catalogue never offers it by itself; a source offering it strongly nearly always gets it in.
		int offered = 0;
		for (long seed = 1; seed <= 50; seed++) {
			assertFalse(MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, seed, 0, 0, Set.of(), List.of()).contains(quirk.id()));
			if (MasteryTraits.offer(FIRE_BOLT, Map.of(), 0, seed, 0, 0, Set.of(), List.of(new MasteryTraits.Weighted(quirk.id(), 100))).contains(quirk.id())) {
				offered++;
			}
		}
		assertTrue(offered >= 45, "offered " + offered + " of 50 times");
		assertEquals(quirk, MasteryTraits.get(quirk.id()).orElseThrow());
		assertFalse(MasteryTraits.builtIn().contains(quirk));
	}

	@Test
	void hookProductsAreHeldToTheirFloors() {
		List<String> both = List.of(MasteryTraits.THRIFTY.id(), MasteryTraits.STEADY_HANDS.id(), MasteryTraits.EMBERBORN.id());
		assertEquals(MasteryRules.COST_FLOOR, MasteryTraits.product(both, Hook.COST, MasteryRules.COST_FLOOR, 1.0), 1e-9);
		assertEquals(0.9, MasteryTraits.product(List.of(MasteryTraits.THRIFTY.id()), Hook.COST, MasteryRules.COST_FLOOR, 1.0), 1e-9);
		assertEquals(1.0, MasteryTraits.product(List.of(), Hook.COST, MasteryRules.COST_FLOOR, 1.0), 1e-9);
	}
}
