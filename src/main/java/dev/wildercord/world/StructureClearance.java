package dev.wildercord.world;

import dev.wildercord.Wildercord;
import dev.wildercord.compat.McWorldgen;
import dev.wildercord.world.sites.oathhold.OathholdOrigin;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Global non-overlap guard across all non-Oathhold Wildercord structures.
 *
 * <p>Minecraft's native {@code exclusion_zone} rejects overlaps with only one referenced structure set,
 * leaving independently generated sites, dungeons, archives, and Oathhold's 2,048x2,048 megacity
 * free to intersect one another. Before any structure places a piece, it queries
 * {@link #isBlocked(Structure.GenerationContext, String)} to yield cleanly to:
 * <ul>
 *   <li>Any resolved Oathhold megacity plus a {@value #OATHHOLD_BUFFER_BLOCKS}-block outer buffer.</li>
 *   <li>Any strictly higher-priority candidate from another structure set in the same world domain
 *       whose bounding clearance intersects at a valid biome.</li>
 * </ul>
 * Strict deterministic priority ordering ensures colliding pairs never mutually cancel.</p>
 */
public final class StructureClearance {
	private StructureClearance() {}

	public static final int OATHHOLD_BUFFER_BLOCKS = 96;
	public static final int OCEAN_MARGIN_BLOCKS = 24;
	public static final int DEFAULT_MARGIN_BLOCKS = 16;

	public enum Domain {
		LAND,
		OCEAN,
		NETHER,
		END
	}

	public record Entry(
		String structureId,
		String structureSetId,
		Domain domain,
		int spacing,
		int separation,
		int salt,
		int halfExtent,
		boolean underground,
		TagKey<Biome> biomeTag,
		RandomSpreadStructurePlacement placement
	) implements Comparable<Entry> {
		@Override
		public int compareTo(Entry other) {
			int byExtent = Integer.compare(other.halfExtent, this.halfExtent);
			if (byExtent != 0) return byExtent;
			return this.structureId.compareTo(other.structureId);
		}
	}

	private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
	private static final Map<Domain, List<Entry>> BY_DOMAIN = new EnumMap<>(Domain.class);

	static {
		for (Domain d : Domain.values()) {
			BY_DOMAIN.put(d, new ArrayList<>());
		}
		register("archive", "archives", Domain.LAND, 44, 14, 20260927, 40, false);
		register("ashen_glade", "ashen_glades", Domain.LAND, 44, 16, 202610113, 34, false);
		register("astral_observatory", "astral_observatory", Domain.END, 32, 10, 20260929, 40, false);
		register("barrow_field", "barrow_fields", Domain.LAND, 44, 16, 202610116, 34, false);
		register("bell_chapel", "bell_chapels", Domain.OCEAN, 44, 16, 202610112, 34, false);
		register("belowkeeper_bell_sanctum", "belowkeeper_bell_sanctum", Domain.LAND, 36, 12, 2026101604, 24, true);
		register("belowkeeper_listening_station", "belowkeeper_listening_station", Domain.LAND, 32, 11, 2026101603, 24, true);
		register("cat_sanctuary", "cat_sanctuary", Domain.LAND, 38, 14, 20261119, 40, false);
		register("clockwork_crypt", "clockwork_crypt", Domain.LAND, 34, 12, 20261003, 28, false);
		register("drowned_scriptorium", "drowned_scriptorium", Domain.OCEAN, 40, 14, 20260930, 40, false);
		register("ember_sanctum", "ember_sanctum", Domain.NETHER, 36, 12, 20260928, 40, false);
		register("emberwood_charcoal_yard", "emberwood_charcoal_yard", Domain.LAND, 32, 11, 2026101605, 24, false);
		register("emberwood_kiln_court", "emberwood_kiln_court", Domain.LAND, 36, 12, 2026101606, 24, false);
		register("farm_apiary", "farm_apiary", Domain.LAND, 40, 12, 2026101103, 24, false);
		register("farm_granary", "farm_granary", Domain.LAND, 48, 16, 2026101107, 24, false);
		register("farm_herbalist", "farm_herbalist", Domain.LAND, 40, 12, 2026101102, 24, false);
		register("farm_mushroom_ring", "farm_mushroom_ring", Domain.LAND, 36, 10, 2026101106, 24, false);
		register("farm_orchard", "farm_orchard", Domain.LAND, 46, 14, 2026101104, 24, false);
		register("farm_scarecrow", "farm_scarecrow", Domain.LAND, 42, 12, 2026101108, 24, false);
		register("farm_shepherd", "farm_shepherd", Domain.LAND, 40, 12, 2026101105, 24, false);
		register("farm_windmill", "farm_windmill", Domain.LAND, 44, 14, 2026101101, 24, false);
		register("glacier_den", "glacier_dens", Domain.LAND, 44, 16, 202610115, 34, false);
		register("highland_cairn_lodge", "highland_cairn_lodge", Domain.LAND, 32, 11, 2026101601, 24, false);
		register("highland_survey_ring", "highland_survey_ring", Domain.LAND, 36, 12, 2026101602, 24, false);
		register("lantern_bathhouse", "lantern_bathhouse", Domain.LAND, 36, 12, 20261120, 28, false);
		register("living_greenhouse", "living_greenhouse", Domain.LAND, 32, 10, 20261004, 28, false);
		register("master_forge_dojo", "master_forge_dojo", Domain.LAND, 44, 16, 2026101001, 30, false);
		register("master_quarry_hall", "master_quarry_hall", Domain.LAND, 46, 16, 2026101003, 30, false);
		register("master_resonance_chamber", "master_resonance_chamber", Domain.LAND, 50, 18, 2026101008, 30, true);
		register("master_root_temple", "master_root_temple", Domain.LAND, 44, 14, 2026101005, 30, false);
		register("master_star_terrace", "master_star_terrace", Domain.LAND, 52, 18, 2026101007, 30, false);
		register("master_sundial_court", "master_sundial_court", Domain.LAND, 48, 16, 2026101006, 30, false);
		register("master_waterfall_shrine", "master_waterfall_shrine", Domain.LAND, 48, 16, 2026101004, 30, false);
		register("master_wind_gate", "master_wind_gate", Domain.LAND, 46, 16, 2026101002, 30, false);
		register("mine_basalt_foundry", "mine_basalt_foundry", Domain.NETHER, 30, 10, 2026101305, 24, false);
		register("mine_collapsed_delve", "mine_collapsed_delve", Domain.LAND, 40, 14, 2026101304, 24, false);
		register("mine_crystal_lab", "mine_crystal_lab", Domain.LAND, 40, 14, 2026101303, 24, true);
		register("mine_deep_vault", "mine_deep_vault", Domain.LAND, 56, 20, 2026101306, 24, true);
		register("mine_forge_hall", "mine_forge_hall", Domain.LAND, 52, 18, 2026101302, 24, false);
		register("mine_hillside_mine", "mine_hillside_mine", Domain.LAND, 36, 12, 2026101301, 24, false);
		register("mine_miners_rest", "mine_miners_rest", Domain.LAND, 30, 10, 2026101308, 24, false);
		register("mine_prospector_camp", "mine_prospector_camp", Domain.LAND, 34, 12, 2026101307, 24, false);
		register("mojito_grove", "mojito_groves", Domain.LAND, 44, 16, 202610121, 34, false);
		register("moving_sky_ruin", "moving_sky_ruin", Domain.LAND, 38, 12, 20261005, 28, false);
		register("ocean_abyssal_citadel", "ocean_abyssal_citadel", Domain.OCEAN, 48, 16, 2026112205, 48, false);
		register("ocean_coral_dome_sanctuary", "ocean_coral_dome_sanctuary", Domain.OCEAN, 36, 12, 2026112201, 38, false);
		register("ocean_hydrothermal_forge", "ocean_hydrothermal_forge", Domain.OCEAN, 34, 11, 2026112204, 30, false);
		register("ocean_kelp_spire_watchtower", "ocean_kelp_spire_watchtower", Domain.OCEAN, 30, 10, 2026112203, 24, false);
		register("ocean_sunken_galleon", "ocean_sunken_galleon", Domain.OCEAN, 32, 10, 2026112202, 36, false);
		register("old_battlefield", "old_battlefields", Domain.LAND, 64, 24, 202610021, 40, false);
		register("rootbound_maze", "rootbound_maze", Domain.LAND, 28, 9, 20261001, 28, false);
		register("serpent_grove", "serpent_groves", Domain.LAND, 44, 16, 202610111, 34, false);
		register("silk_hollow", "silk_hollows", Domain.LAND, 46, 16, 202610114, 34, false);
		register("sleeping_blade", "sleeping_blades", Domain.LAND, 92, 36, 202610023, 34, false);
		register("storm_spire", "storm_spire", Domain.LAND, 32, 10, 20261002, 28, false);
		register("sword_tomb", "sword_tombs", Domain.LAND, 76, 28, 202610022, 34, false);
		register("travel_broken_bridge", "travel_broken_bridge", Domain.LAND, 40, 14, 2026101402, 24, false);
		register("travel_caravan_camp", "travel_caravan_camp", Domain.LAND, 34, 12, 2026101405, 24, false);
		register("travel_cartographer_hut", "travel_cartographer_hut", Domain.LAND, 36, 12, 2026101406, 24, false);
		register("travel_rune_library", "travel_rune_library", Domain.LAND, 44, 16, 2026101403, 24, false);
		register("travel_standing_stones", "travel_standing_stones", Domain.LAND, 36, 12, 2026101404, 24, false);
		register("travel_vow_circle", "travel_vow_circle", Domain.LAND, 44, 16, 2026101407, 24, false);
		register("travel_watchtower", "travel_watchtower", Domain.LAND, 40, 14, 2026101401, 24, false);
		register("travel_wayfarer_inn", "travel_wayfarer_inn", Domain.LAND, 34, 12, 2026101400, 24, false);
		register("water_ice_camp", "water_ice_camp", Domain.OCEAN, 36, 12, 2026101204, 24, false);
		register("water_last_crossing", "water_last_crossing", Domain.LAND, 42, 14, 2026101510, 24, false);
		register("water_lighthouse", "water_lighthouse", Domain.LAND, 44, 16, 2026101202, 24, false);
		register("water_netweaver_village", "water_netweaver_village", Domain.LAND, 40, 14, 2026101207, 24, false);
		register("water_pier_boathouse", "water_pier_boathouse", Domain.LAND, 40, 14, 2026101205, 24, false);
		register("water_reedkeeper_landing", "water_reedkeeper_landing", Domain.LAND, 28, 9, 2026101509, 24, false);
		register("water_stilt_smokehouse", "water_stilt_smokehouse", Domain.LAND, 36, 12, 2026101201, 24, false);
		register("water_sunken_shrine", "water_sunken_shrine", Domain.OCEAN, 48, 16, 2026101206, 24, false);
		register("water_tidepool_grotto", "water_tidepool_grotto", Domain.LAND, 36, 12, 2026101208, 24, false);
		register("water_watermill", "water_watermill", Domain.LAND, 32, 10, 2026101203, 24, false);
		register("wayside_shrine", "wayside_shrines", Domain.LAND, 30, 10, 2026101209, 28, false);
		register("wilds_dawn_pavilion", "wilds_dawn_pavilion", Domain.LAND, 28, 10, 2026101505, 24, false);
		register("wilds_dune_temple", "wilds_dune_temple", Domain.LAND, 34, 12, 2026101502, 24, false);
		register("wilds_echo_post", "wilds_echo_post", Domain.LAND, 30, 10, 2026101506, 24, true);
		register("wilds_ember_outpost", "wilds_ember_outpost", Domain.NETHER, 32, 12, 2026101507, 24, true);
		register("wilds_iron_gatehouse", "wilds_iron_gatehouse", Domain.LAND, 40, 14, 2026101504, 24, false);
		register("wilds_rime_monastery", "wilds_rime_monastery", Domain.LAND, 34, 12, 2026101503, 24, false);
		register("wilds_sakura_canal_town", "wilds_sakura_canal_town", Domain.LAND, 36, 12, 20261122, 30, false);
		register("wilds_sakura_castle_town", "wilds_sakura_castle_town", Domain.LAND, 36, 12, 20261124, 30, false);
		register("wilds_sakura_onsen_village", "wilds_sakura_onsen_village", Domain.LAND, 36, 12, 20261123, 30, false);
		register("wilds_sakura_village", "wilds_sakura_village", Domain.LAND, 34, 12, 20261121, 30, false);
		register("wilds_venom_ziggurat", "wilds_venom_ziggurat", Domain.LAND, 36, 12, 2026101501, 24, false);
		register("wilds_void_lantern", "wilds_void_lantern", Domain.END, 40, 14, 2026101508, 24, false);

		for (List<Entry> group : BY_DOMAIN.values()) {
			Collections.sort(group);
		}
	}

	private static void register(
			String structureId, String structureSetId, Domain domain,
			int spacing, int separation, int salt, int halfExtent, boolean underground) {
		Entry entry = new Entry(
			structureId,
			structureSetId,
			domain,
			spacing,
			separation,
			salt,
			halfExtent,
			underground,
			TagKey.create(Registries.BIOME, Wildercord.id("has_structure/" + structureId)),
			new RandomSpreadStructurePlacement(spacing, separation, RandomSpreadType.LINEAR, salt)
		);
		ENTRIES.put(structureId, entry);
		BY_DOMAIN.get(domain).add(entry);
	}

	public static Map<String, Entry> entries() {
		return Collections.unmodifiableMap(ENTRIES);
	}

	public static int clearanceBlocks(Entry a, Entry b) {
		int margin = (a.domain == Domain.OCEAN && b.domain == Domain.OCEAN) ? OCEAN_MARGIN_BLOCKS : DEFAULT_MARGIN_BLOCKS;
		return a.halfExtent + b.halfExtent + margin;
	}

	private static boolean sharesVerticalBand(Entry a, Entry b) {
		return a.underground == b.underground || "archive".equals(a.structureId) || "archive".equals(b.structureId);
	}

	/**
	 * Returns {@code true} if {@code structureId} is blocked at {@code context}'s candidate chunk because
	 * it intersects an Oathhold megacity exclusion zone or a higher-priority sibling candidate.
	 */
	public static boolean isBlocked(Structure.GenerationContext context, String structureId) {
		Entry self = ENTRIES.get(structureId);
		if (self == null) {
			return false;
		}
		int cx = context.chunkPos().x();
		int cz = context.chunkPos().z();
		int mx = cx * 16 + 8;
		int mz = cz * 16 + 8;

		if (self.domain == Domain.LAND || self.domain == Domain.OCEAN) {
			if (OathholdOrigin.isNearAnyCity(context, mx, mz, OATHHOLD_BUFFER_BLOCKS)) {
				return true;
			}
		}

		long seed = context.seed();
		int sea = context.chunkGenerator().getSeaLevel();
		int surfaceSampleY = Math.max(sea, 64);
		int caveSampleY = Math.max(context.heightAccessor().getMinY() + 8, sea - 48);

		for (Entry other : BY_DOMAIN.get(self.domain)) {
			if (other == self) continue;
			if (other.compareTo(self) >= 0) {
				// Group is sorted highest priority first; once other is lower/equal priority, none remain.
				break;
			}
			if (!sharesVerticalBand(self, other)) {
				continue;
			}
			int req = clearanceBlocks(self, other);
			int baseRx = Math.floorDiv(cx, other.spacing);
			int baseRz = Math.floorDiv(cz, other.spacing);
			for (int drx = -1; drx <= 1; drx++) {
				for (int drz = -1; drz <= 1; drz++) {
					ChunkPos oc = other.placement.getPotentialStructureChunk(
						seed, (baseRx + drx) * other.spacing, (baseRz + drz) * other.spacing);
					int ox = oc.x() * 16 + 8;
					int oz = oc.z() * 16 + 8;
					if (Math.abs(mx - ox) < req && Math.abs(mz - oz) < req) {
						int sampleY = other.underground ? caveSampleY : surfaceSampleY;
						Holder<Biome> biome = McWorldgen.getNoiseBiome(context, ox, sampleY, oz);
						if (biome.is(other.biomeTag)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Pure deterministic placement-grid overlap check (ignoring biome eligibility) for tests and
	 * offline auditing. Returns the highest-priority higher-ranked entry whose candidate chunk lies
	 * within {@link #clearanceBlocks(Entry, Entry)} of {@code (chunkX, chunkZ)}.
	 */
	public static Optional<Entry> blockingPlacementCandidate(long seed, Entry self, int chunkX, int chunkZ) {
		int mx = chunkX * 16 + 8;
		int mz = chunkZ * 16 + 8;
		for (Entry other : BY_DOMAIN.get(self.domain)) {
			if (other == self) continue;
			if (other.compareTo(self) >= 0) break;
			if (!sharesVerticalBand(self, other)) continue;
			int req = clearanceBlocks(self, other);
			int baseRx = Math.floorDiv(chunkX, other.spacing);
			int baseRz = Math.floorDiv(chunkZ, other.spacing);
			for (int drx = -1; drx <= 1; drx++) {
				for (int drz = -1; drz <= 1; drz++) {
					ChunkPos oc = other.placement.getPotentialStructureChunk(
						seed, (baseRx + drx) * other.spacing, (baseRz + drz) * other.spacing);
					int ox = oc.x() * 16 + 8;
					int oz = oc.z() * 16 + 8;
					if (Math.abs(mx - ox) < req && Math.abs(mz - oz) < req) {
						return Optional.of(other);
					}
				}
			}
		}
		return Optional.empty();
	}
}
