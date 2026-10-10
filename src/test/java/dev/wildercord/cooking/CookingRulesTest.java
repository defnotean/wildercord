package dev.wildercord.cooking;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CookingRulesTest {
	@Test
	void aboutTwentyMealsEachNamedAndBuffed() throws Exception {
		assertTrue(CookingRules.RECIPES.size() >= 20);
		String lang = Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"));
		Set<String> ids = new HashSet<>();
		for (CookingRules.Recipe recipe : CookingRules.RECIPES) {
			assertTrue(ids.add(recipe.id()), recipe.id() + " twice");
			assertFalse(recipe.buffs().isEmpty(), recipe.id());
			assertTrue(lang.contains("\"item.wildercord." + recipe.id() + "\""), recipe.id() + " has no name");
			assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/textures/item/" + recipe.id() + ".png")), recipe.id());
		}
	}

	@Test
	void mealsKeepYouGoingButNeverHitHarder() {
		for (CookingRules.Recipe recipe : CookingRules.RECIPES) {
			for (CookingRules.Buff buff : recipe.buffs()) {
				assertNotEquals("minecraft:strength", buff.effect(), recipe.id());
				assertTrue(buff.seconds() > 0 && buff.seconds() <= 600, recipe.id());
				assertTrue(buff.level() >= 1 && buff.level() <= (recipe.rare() ? 3 : 2), recipe.id());
			}
		}
	}

	@Test
	void thePotCooksWhatYouPickedOrTheRichest() {
		Map<String, Integer> pack = Map.of("minecraft:beef", 1, "minecraft:potato", 1, "minecraft:carrot", 1, "minecraft:porkchop", 1);
		assertEquals("hearty_stew", CookingRules.pick(pack, null).id());
		assertEquals("hunters_skewer", CookingRules.pick(pack, "hunters_skewer").id());
		assertEquals("hearty_stew", CookingRules.pick(pack, "archmages_feast").id());
		assertNull(CookingRules.pick(Map.of(), null));
		assertNull(CookingRules.next(Map.of(), null));
		assertNotEquals(CookingRules.next(pack, null).id(), CookingRules.next(pack, CookingRules.next(pack, null).id()).id());
	}

	@Test
	void rareMealsNeedARareIngredientAndAreNeverCookedUnasked() {
		var rare = CookingRules.RECIPES.stream().filter(CookingRules.Recipe::rare).toList();
		assertTrue(rare.size() >= 4);
		for (CookingRules.Recipe recipe : rare) {
			assertTrue(recipe.needs().stream().anyMatch(n -> n.item().equals(CookingRules.SAFFRON)), recipe.id() + " needs saffron");
			assertTrue(recipe.needs().stream().filter(n -> n.item().startsWith("wildercord:")).count() >= 2, recipe.id() + " needs a rare ingredient");
		}
		Map<String, Integer> pack = Map.of("wildercord:giant_heart", 1, "minecraft:beef", 1, "minecraft:potato", 1, "minecraft:carrot", 1,
			CookingRules.SAFFRON, 1);
		assertEquals("hearty_stew", CookingRules.pick(pack, null).id(), "the pot never spends a giant's heart unasked");
		assertEquals("titans_roast", CookingRules.pick(pack, "titans_roast").id());
		Map<String, Integer> onlyRare = Map.of("wildercord:giant_heart", 1, "minecraft:beef", 1, "minecraft:potato", 1, CookingRules.SAFFRON, 1);
		assertNull(CookingRules.pick(onlyRare, null));
		assertEquals("titans_roast", CookingRules.next(onlyRare, null).id(), "but it can be picked from the cookbook");
		assertTrue(CookingRules.recipe("titans_roast").nutrition() > CookingRules.recipe("archmages_feast").nutrition());
	}

	@Test
	void focusIsCapped() {
		assertEquals(1, CookingRules.focusedCost(0));
		assertEquals(1 - 2 * CookingRules.FOCUSED_DISCOUNT, CookingRules.focusedCost(5), 1e-9);
	}
}
