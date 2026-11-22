package dev.wildercord.world;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.world.dungeons.DungeonStructure;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link StructureClearance} covers every non-Oathhold structure set with exact
 * JSON parity and prevents any two same-domain structure candidates from overlapping across a
 * wide chunk sweep without mutual cancellation.
 */
class StructureClearanceTest {
	private static final Path WORLDGEN = Path.of("src/main/resources/data/wildercord/worldgen");

	@Test
	void entriesMatchAllWorldgenStructureSetsAndDungeons() throws IOException {
		Map<String, StructureClearance.Entry> entries = StructureClearance.entries();
		Set<String> seenIds = new HashSet<>();

		List<Path> setFiles;
		try (Stream<Path> files = Files.list(WORLDGEN.resolve("structure_set"))) {
			setFiles = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
		}
		for (Path setFile : setFiles) {
			String setId = setFile.getFileName().toString().replace(".json", "");
			if ("oathhold".equals(setId) || "oathhold_sector".equals(setId)) {
				assertFalse(entries.containsKey(setId), "Oathhold should not self-register in StructureClearance");
				continue;
			}
			JsonObject json = JsonParser.parseString(Files.readString(setFile)).getAsJsonObject();
			JsonObject placement = json.getAsJsonObject("placement");
			String structureId = json.getAsJsonArray("structures").get(0).getAsJsonObject()
				.get("structure").getAsString().replace("wildercord:", "");

			StructureClearance.Entry entry = entries.get(structureId);
			assertTrue(entry != null, "Missing StructureClearance entry for " + structureId);
			assertEquals(setId, entry.structureSetId(), "Mismatched structure_set ID for " + structureId);
			assertEquals(placement.get("spacing").getAsInt(), entry.spacing(), "Spacing mismatch for " + structureId);
			assertEquals(placement.get("separation").getAsInt(), entry.separation(), "Separation mismatch for " + structureId);
			assertEquals(placement.get("salt").getAsInt(), entry.salt(), "Salt mismatch for " + structureId);
			seenIds.add(structureId);
		}
		assertEquals(entries.keySet(), seenIds, "Extra or missing entries in StructureClearance");

		for (DungeonStructure.Kind kind : DungeonStructure.Kind.values()) {
			assertTrue(entries.containsKey(kind.getSerializedName()),
				"Missing DungeonStructure.Kind in StructureClearance: " + kind);
		}
		assertTrue(entries.containsKey("archive"), "Missing archive in StructureClearance");
	}

	@Test
	void sameDomainCandidatesNeverOverlapAfterClearanceSweep() {
		long seed = 20261125L;
		record Placed(StructureClearance.Entry entry, int chunkX, int chunkZ) {}
		List<Placed> survivors = new ArrayList<>();

		for (StructureClearance.Entry entry : StructureClearance.entries().values()) {
			Set<ChunkPos> seen = new HashSet<>();
			int step = entry.spacing();
			for (int cx = -64; cx < 64; cx += step) {
				for (int cz = -64; cz < 64; cz += step) {
					ChunkPos candidate = entry.placement().getPotentialStructureChunk(seed, cx, cz);
					if (!seen.add(candidate)) continue;
					if (StructureClearance.blockingPlacementCandidate(seed, entry, candidate.x(), candidate.z()).isEmpty()) {
						survivors.add(new Placed(entry, candidate.x(), candidate.z()));
					}
				}
			}
		}

		assertFalse(survivors.isEmpty(), "Expected surviving candidates across sweep");
		for (int i = 0; i < survivors.size(); i++) {
			Placed a = survivors.get(i);
			int ax = a.chunkX * 16 + 8;
			int az = a.chunkZ * 16 + 8;
			for (int j = i + 1; j < survivors.size(); j++) {
				Placed b = survivors.get(j);
				if (a.entry.domain() != b.entry.domain()) continue;
				if (a.entry.underground() != b.entry.underground()
						&& !"archive".equals(a.entry.structureId())
						&& !"archive".equals(b.entry.structureId())) {
					continue;
				}
				int req = StructureClearance.clearanceBlocks(a.entry, b.entry);
				int bx = b.chunkX * 16 + 8;
				int bz = b.chunkZ * 16 + 8;
				assertFalse(Math.abs(ax - bx) < req && Math.abs(az - bz) < req,
					"Overlapping surviving pair: " + a.entry.structureId() + "@" + a.chunkX + "," + a.chunkZ
						+ " vs " + b.entry.structureId() + "@" + b.chunkX + "," + b.chunkZ);
			}
		}
	}
}
