package dev.wildercord.cosmetic;

import dev.wildercord.cosmetic.CordStyles.Option;
import dev.wildercord.cosmetic.CordStyles.Progress;
import dev.wildercord.cosmetic.CordStyles.Style;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CordStylesTest {
	private static final Progress NEW = new Progress(0, List.of(), false, List.of());

	@Test
	void everyGroupHasItsOptions() {
		assertEquals(List.of("glass", "gold", "obsidian", "amethyst", "bone", "prismarine"), CordStyles.MATERIALS.stream().map(Option::id).toList());
		assertEquals(List.of("none", "sparks", "petals", "snow", "embers", "stars"), CordStyles.TRAILS.stream().map(Option::id).toList());
		assertEquals(17, CordStyles.GLOWS.size());
		Set<String> keys = new HashSet<>();
		for (String group : List.of(CordStyles.MATERIAL, CordStyles.GLOW, CordStyles.TRAIL)) {
			for (Option option : CordStyles.group(group)) {
				assertTrue(keys.add(option.key()), "duplicate " + option.key());
				assertSame(option, CordStyles.option(option.key()));
			}
		}
		assertNull(CordStyles.option("material:cheese"));
		assertNull(CordStyles.option("nonsense"));
	}

	@Test
	void aNewPlayerHasTheDefaults() {
		assertTrue(CordStyles.allowed(Style.DEFAULT, NEW));
		for (String group : List.of(CordStyles.MATERIAL, CordStyles.GLOW, CordStyles.TRAIL)) {
			long free = CordStyles.group(group).stream().filter(o -> CordStyles.unlocked(o, NEW)).count();
			assertEquals(1, free, group + " should have exactly one free option");
		}
	}

	@Test
	void playUnlocksCirclesFeatsAndBosses() {
		Option sparks = CordStyles.option("trail:sparks");
		Option obsidian = CordStyles.option("material:obsidian");
		Option bone = CordStyles.option("material:bone");
		Option stars = CordStyles.option("trail:stars");
		assertFalse(CordStyles.unlocked(sparks, NEW));
		assertTrue(CordStyles.unlocked(sparks, new Progress(1, List.of(), false, List.of())));
		assertFalse(CordStyles.unlocked(obsidian, new Progress(3, List.of(), false, List.of())));
		assertTrue(CordStyles.unlocked(obsidian, new Progress(4, List.of(), false, List.of())));
		assertFalse(CordStyles.unlocked(bone, new Progress(8, List.of("feat:overcast"), true, List.of())));
		assertTrue(CordStyles.unlocked(bone, new Progress(0, List.of("feat:runebound"), false, List.of())));
		assertFalse(CordStyles.unlocked(stars, new Progress(8, List.of(), false, List.of())));
		assertTrue(CordStyles.unlocked(stars, new Progress(0, List.of(), true, List.of())));
	}

	@Test
	void boughtOptionsStayBought() {
		Option gold = CordStyles.option("material:gold");
		Option red = CordStyles.option("glow:red");
		assertTrue(CordStyles.buyable(gold));
		assertTrue(CordStyles.buyable(red));
		assertFalse(CordStyles.buyable(CordStyles.option("trail:stars")));
		assertEquals("minecraft:gold_ingot", gold.unlock().what());
		assertEquals("minecraft:red_dye", red.unlock().what());
		assertFalse(CordStyles.unlocked(gold, NEW));
		Progress bought = new Progress(0, List.of(), false, List.of("material:gold"));
		assertTrue(CordStyles.unlocked(gold, bought));
		assertFalse(CordStyles.unlocked(red, bought));
	}

	@Test
	void lockedPartsFallBackToTheDefault() {
		Style wanted = new Style("gold", "red", "stars");
		assertFalse(CordStyles.allowed(wanted, NEW));
		assertEquals(Style.DEFAULT, CordStyles.sanitize(wanted, NEW));
		Progress some = new Progress(0, List.of(), true, List.of("material:gold"));
		assertEquals(new Style("gold", "spell", "stars"), CordStyles.sanitize(wanted, some));
		assertEquals(Style.DEFAULT, CordStyles.sanitize(new Style("cheese", "plaid", "fireworks"), some));
	}

	@Test
	void glowsFollowTheSpellOrAFixedColour() {
		assertEquals(-1, CordStyles.glowColor("spell"));
		assertEquals(-1, CordStyles.glowColor("plaid"));
		assertEquals(CordStyles.option("glow:cyan").color(), CordStyles.glowColor("cyan"));
		assertEquals(new Style("glass", "cyan", "none"), Style.DEFAULT.with(CordStyles.option("glow:cyan")));
		assertEquals("cyan", Style.DEFAULT.withGlow("cyan").get(CordStyles.GLOW));
	}
}
