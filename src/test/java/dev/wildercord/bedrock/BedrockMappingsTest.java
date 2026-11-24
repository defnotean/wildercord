package dev.wildercord.bedrock;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockMappingsTest {
	@Test
	void loadsBundledMappingsCatalog() {
		BedrockMappings map = BedrockMappings.ensureLoaded();
		assertTrue(map.itemCount() >= 300, "Expected >=300 mapped items, got " + map.itemCount());
		assertTrue(map.runeCount() >= 700, "Expected >=700 mapped runes, got " + map.runeCount());
		assertTrue(map.blockCount() >= 60, "Expected >=60 mapped blocks, got " + map.blockCount());
		assertTrue(map.entityCount() >= 100, "Expected >=100 mapped entities, got " + map.entityCount());
		assertTrue(map.particleCount() >= 12, "Expected >=12 mapped particles, got " + map.particleCount());

		var cord = map.lookupItem("wildercord:twine_cord").orElseThrow();
		assertEquals("minecraft:blaze_rod", cord.javaCarrier());
		assertEquals("wildercord:twine_cord", cord.bedrockId());

		var selfRune = map.lookupRune("wildercord:self").orElseThrow();
		assertEquals("minecraft:paper", selfRune.javaCarrier());
		assertEquals("wildercord:rune_self", selfRune.itemModel());

		var wellstone = map.lookupBlock("wildercord:wellstone").orElseThrow();
		assertTrue(wellstone.javaCarrierState().startsWith("minecraft:note_block["),
			"Expected note_block carrier state, got " + wellstone.javaCarrierState());

		assertEquals("minecraft:evoker", map.lookupEntity("wildercord:archivist").orElseThrow());
		assertEquals("minecraft:enchant", map.lookupParticle("wildercord:sigil").orElseThrow());

		var emberBlade = map.lookupItem("wildercord:master_blade/ember").orElseThrow();
		assertEquals("minecraft:netherite_sword", emberBlade.javaCarrier());
		assertEquals("wildercord:master_blade/ember", emberBlade.itemModel());
		assertEquals("wildercord:master_blade_ember", emberBlade.bedrockId());

		assertEquals("minecraft:leather", map.lookupItem("wildercord:backpack").orElseThrow().javaCarrier());
		assertEquals("minecraft:written_book", map.lookupItem("wildercord:three_bows").orElseThrow().javaCarrier());
		assertEquals("wildercord.altar_fuse", BedrockMappings.toBedrockSoundKey("wildercord", "altar_fuse"));
	}

	@Test
	void optionalBridgeDegradesSafelyWithoutGeyserOrFloodgate() {
		assertFalse(BedrockBridge.isAnyLoaded());
		assertFalse(BedrockPlayers.isBedrock((UUID) null));
		assertFalse(BedrockPlayers.isBedrock(UUID.randomUUID()));
	}

	@Test
	void stripsSubclassEntityMetadataIdsForModdedCarriers() {
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(true, -1));
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(false, -1));
		for (int id = 0; id <= 14; id++) {
			assertTrue(BedrockMappings.shouldKeepModdedEntityDataId(true, id), "Expected LivingEntity base ID kept: " + id);
		}
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(true, 15));
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(true, 16));
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(true, 24));

		for (int id = 0; id <= 7; id++) {
			assertTrue(BedrockMappings.shouldKeepModdedEntityDataId(false, id), "Expected Entity base ID kept: " + id);
		}
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(false, 8), "Expected RuneBolt ID 8 stripped for non-living carriers");
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(false, 9));
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(false, 14));
		assertFalse(BedrockMappings.shouldKeepModdedEntityDataId(false, 16));
	}

	@Test
	void vfxBridgeMathHelpersProduceValidGeometryAndColors() {
		assertEquals(0x123456, BedrockVfxBridge.normalizeRgb(0xFF123456));
		assertTrue(BedrockVfxBridge.normalizeRgb(0) != 0);

		assertEquals(0.5F, BedrockVfxBridge.clampScale(0.1F), 1.0E-5F);
		assertEquals(3.0F, BedrockVfxBridge.clampScale(99.0F), 1.0E-5F);
		assertEquals(1.2F, BedrockVfxBridge.clampScale(Float.NaN), 1.0E-5F);

		double[][] xzBasis = BedrockVfxBridge.basisFromYawPitch(0.0F, -90.0F);
		assertEquals(0.0, xzBasis[0][1], 1.0E-6);
		assertEquals(0.0, xzBasis[1][1], 1.0E-6);
		double uLen = Math.hypot(Math.hypot(xzBasis[0][0], xzBasis[0][1]), xzBasis[0][2]);
		double vLen = Math.hypot(Math.hypot(xzBasis[1][0], xzBasis[1][1]), xzBasis[1][2]);
		double dot = xzBasis[0][0] * xzBasis[1][0] + xzBasis[0][1] * xzBasis[1][1] + xzBasis[0][2] * xzBasis[1][2];
		assertEquals(1.0, uLen, 1.0E-6);
		assertEquals(1.0, vLen, 1.0E-6);
		assertEquals(0.0, dot, 1.0E-6);

		double[] mid = BedrockVfxBridge.quadraticBezier(0.0, 2.0, 0.0, 4.0, 0.0, 0.0, 0.5);
		assertEquals(1.0, mid[0], 1.0E-6);
		assertEquals(1.0, mid[1], 1.0E-6);
		assertEquals(0.0, mid[2], 1.0E-6);
	}

	@Test
	void formatsBedrockActionbarHudSummary() {
		String full = BedrockHud.formatSummary(
			true, 120, 225, 2, "Solar Lance",
			true, 45, 100, "edge", true,
			true, true, true, true
		);
		assertTrue(full.contains("CLASH!"));
		assertTrue(full.contains("MP 120/225"));
		assertTrue(full.contains("[2] Solar Lance"));
		assertTrue(full.contains("Aura 45/100"));
		assertTrue(full.contains("Edge"));
		assertTrue(full.contains("[COAT]"));
		assertTrue(full.contains("PASSIVE"));
		assertTrue(full.contains("SILK"));
		assertTrue(full.contains("UNITY"));

		assertEquals("", BedrockHud.formatSummary(false, 0, 0, 1, "", false, 0, 0, "", false, false, false, false, false));
	}

	@Test
	void translatesWildercordLangKeysAndCompassDirections() {
		assertTrue(BedrockTextTranslator.keyCount() >= 9000, "Expected >=9000 lang keys, got " + BedrockTextTranslator.keyCount());
		assertNotNull(BedrockTextTranslator.lookup("message.wildercord.spell_empty"));
		assertEquals("Spell Spark has 50/100 MP", BedrockTextTranslator.formatTemplate("Spell %s has %3$d/%2$d MP", "Spark", 100, 50));
		assertEquals("Spell Spark has 50/100 MP", BedrockTextTranslator.formatTemplate("Spell %1$s has %3$d/%2$d MP", "Spark", 100, 50));
		assertEquals("100% ready: Go", BedrockTextTranslator.formatTemplate("100%% ready: %s", "Go"));

		assertEquals("N", BedrockHud.compassDirection(0, -10));
		assertEquals("NE", BedrockHud.compassDirection(10, -10));
		assertEquals("E", BedrockHud.compassDirection(10, 0));
		assertEquals("S", BedrockHud.compassDirection(0, 10));
		assertEquals("W", BedrockHud.compassDirection(-10, 0));
	}
}
