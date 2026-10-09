package dev.wildercord.world.sites.farm;

import dev.wildercord.world.sites.Sites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * The farmstead pack: eight peaceful working places (a windmill, an herbalist's cottage, an apiary, a walled
 * orchard, a shepherd's hut, a sunken mushroom ring, a granary with its harvest shrine and an abandoned scarecrow
 * field). Where each appears lives in tools/sites_farm.py.
 */
public final class FarmSites {
	private FarmSites() {}

	public static final List<String> IDS = List.of("farm_windmill", "farm_herbalist", "farm_apiary", "farm_orchard", "farm_shepherd",
		"farm_mushroom_ring", "farm_granary", "farm_scarecrow");

	static final StructurePieceType WINDMILL = Sites.piece("farm_windmill", WindmillPiece::new);
	static final StructurePieceType HERBALIST = Sites.piece("farm_herbalist", HerbalistPiece::new);
	static final StructurePieceType APIARY = Sites.piece("farm_apiary", ApiaryPiece::new);
	static final StructurePieceType ORCHARD = Sites.piece("farm_orchard", OrchardPiece::new);
	static final StructurePieceType SHEPHERD = Sites.piece("farm_shepherd", ShepherdPiece::new);
	static final StructurePieceType MUSHROOM_RING = Sites.piece("farm_mushroom_ring", MushroomRingPiece::new);
	static final StructurePieceType GRANARY = Sites.piece("farm_granary", GranaryPiece::new);
	static final StructurePieceType SCARECROW = Sites.piece("farm_scarecrow", ScarecrowPiece::new);

	static final ResourceKey<LootTable> WINDMILL_LOFT = Sites.loot("chests/farm_windmill");
	static final ResourceKey<LootTable> HERBALIST_BENCH = Sites.loot("chests/farm_herbalist"), HERBALIST_LOFT = Sites.loot("chests/farm_herbalist_loft");
	static final ResourceKey<LootTable> APIARY_SHED = Sites.loot("chests/farm_apiary");
	static final ResourceKey<LootTable> ORCHARD_STAND = Sites.loot("chests/farm_orchard"), ORCHARD_CACHE = Sites.loot("chests/farm_orchard_cache");
	static final ResourceKey<LootTable> SHEPHERD_HUT = Sites.loot("chests/farm_shepherd");
	static final ResourceKey<LootTable> MUSHROOM_CELLAR = Sites.loot("chests/farm_mushroom_ring");
	static final ResourceKey<LootTable> GRANARY_BARN = Sites.loot("chests/farm_granary"), GRANARY_SHRINE = Sites.loot("chests/farm_granary_shrine");
	static final ResourceKey<LootTable> SCARECROW_HOUSE = Sites.loot("chests/farm_scarecrow");

	public static void init() {
		Sites.register("farm_windmill", c -> FarmPiece.locate(c, WindmillPiece::new, 5));
		Sites.register("farm_herbalist", c -> FarmPiece.locate(c, HerbalistPiece::new, 5));
		Sites.register("farm_apiary", c -> FarmPiece.locate(c, ApiaryPiece::new, 6));
		Sites.register("farm_orchard", c -> FarmPiece.locate(c, OrchardPiece::new, 4));
		Sites.register("farm_shepherd", c -> FarmPiece.locate(c, ShepherdPiece::new, 7));
		Sites.register("farm_mushroom_ring", c -> FarmPiece.locate(c, MushroomRingPiece::new, 4));
		Sites.register("farm_granary", c -> FarmPiece.locate(c, GranaryPiece::new, 4));
		Sites.register("farm_scarecrow", c -> FarmPiece.locate(c, ScarecrowPiece::new, 5));
	}
}
