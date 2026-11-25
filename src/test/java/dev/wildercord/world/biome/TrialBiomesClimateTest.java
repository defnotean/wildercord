package dev.wildercord.world.biome;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.spell.Attunements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the new Trial biomes compose cleanly with existing regional biomes and attunements. */
class TrialBiomesClimateTest {
	private static final List<BiConsumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>>> ALL_ADAPTERS = List.of(
		ReedlandClimate::emit,
		WindscarClimate::emit,
		BelowkeeperClimate::emit,
		EmberwoodClimate::emit,
		AstralLeyGladeClimate::emit,
		CinderCalderaClimate::emit
	);

	@Test
	void astralLeyGladeAndCinderCalderaComposeCommutativelyWithExistingBiomes() {
		var point = new Climate.ParameterPoint(
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			0
		);
		for (var biome : List.of(Biomes.DARK_FOREST, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS, Biomes.SWAMP, Biomes.PLAINS)) {
			List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> forward = List.of(Pair.of(point, biome));
			for (var adapter : ALL_ADAPTERS) {
				var next = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
				for (var e : forward) adapter.accept(e, next::add);
				forward = next;
			}
			List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> reverse = List.of(Pair.of(point, biome));
			for (int i = ALL_ADAPTERS.size() - 1; i >= 0; i--) {
				var next = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
				for (var e : reverse) ALL_ADAPTERS.get(i).accept(e, next::add);
				reverse = next;
			}
			assertEquals(Set.copyOf(forward), Set.copyOf(reverse), "Adapters must commute for " + biome.identifier());
			if (biome.equals(Biomes.DARK_FOREST)) {
				assertTrue(forward.stream().anyMatch(p -> p.getSecond().equals(AstralLeyGladeClimate.ASTRAL_LEY_GLADE)));
			}
			if (biome.equals(Biomes.ERODED_BADLANDS)) {
				assertTrue(forward.stream().anyMatch(p -> p.getSecond().equals(CinderCalderaClimate.CINDER_CALDERA)));
			}
		}
	}

	@Test
	void trialBiomesShareExistingAttunementsAndJournalEntries() throws Exception {
		assertEquals(15, Attunements.RULES.size());
		assertTrue(Attunements.biomeHolds("wildercord:astral_ley_glade"));
		assertTrue(Attunements.biomeHolds("wildercord:cinder_caldera"));
		assertEquals("places", LoreEntries.tab(AstralLeyGladeClimate.JOURNAL_ENTRY));
		assertEquals("places", LoreEntries.tab(CinderCalderaClimate.JOURNAL_ENTRY));
		try (var in = TrialBiomesClimateTest.class.getResourceAsStream("/assets/wildercord/lang/en_us.json")) {
			assertNotNull(in);
			var lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(lang.has("biome.wildercord.astral_ley_glade"));
			assertTrue(lang.has("biome.wildercord.cinder_caldera"));
			assertTrue(lang.has(LoreEntries.fixedKey(AstralLeyGladeClimate.JOURNAL_ENTRY)));
			assertTrue(lang.has(LoreEntries.fixedKey(CinderCalderaClimate.JOURNAL_ENTRY)));
		}
	}
}
