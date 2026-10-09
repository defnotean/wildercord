package dev.wildercord.world.sites.masters;

import dev.wildercord.world.sites.Sites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * The Master halls and shrines: eight training places of the breathing schools, each keeping one of two related schools.
 * Each has practice dummies, a lectern on how to challenge that school's Master, a small trial and a reward chest of the
 * school's manual pages, technique scrolls and runes. Where they appear lives in {@code tools/sites_masters.py}.
 */
public final class MasterSites {
	private MasterSites() {}

	public static final List<String> IDS = List.of("master_forge_dojo", "master_wind_gate", "master_quarry_hall", "master_waterfall_shrine",
		"master_root_temple", "master_sundial_court", "master_star_terrace", "master_resonance_chamber");

	public static final StructurePieceType FORGE_DOJO = Sites.piece("master_forge_dojo", ForgeDojoPiece::new);
	public static final StructurePieceType WIND_GATE = Sites.piece("master_wind_gate", WindGatePiece::new);
	public static final StructurePieceType QUARRY_HALL = Sites.piece("master_quarry_hall", QuarryHallPiece::new);
	public static final StructurePieceType WATERFALL_SHRINE = Sites.piece("master_waterfall_shrine", WaterfallShrinePiece::new);
	public static final StructurePieceType ROOT_TEMPLE = Sites.piece("master_root_temple", RootTemplePiece::new);
	public static final StructurePieceType SUNDIAL_COURT = Sites.piece("master_sundial_court", SundialCourtPiece::new);
	public static final StructurePieceType STAR_TERRACE = Sites.piece("master_star_terrace", StarTerracePiece::new);
	public static final StructurePieceType RESONANCE_CHAMBER = Sites.piece("master_resonance_chamber", ResonanceChamberPiece::new);

	/** A school's reward table at a site. */
	public static ResourceKey<LootTable> reward(String site, String school) {
		return Sites.loot("chests/" + site + "_" + school);
	}

	/** A site's shared supplies table. */
	public static ResourceKey<LootTable> supplies(String site) {
		return Sites.loot("chests/" + site);
	}

	public static void init() {
		Sites.register("master_forge_dojo", c -> MasterSitePiece.onGround(c, ForgeDojoPiece::new, 3));
		Sites.register("master_wind_gate", c -> MasterSitePiece.onGround(c, WindGatePiece::new, 4));
		Sites.register("master_quarry_hall", c -> MasterSitePiece.onGround(c, QuarryHallPiece::new, 4));
		Sites.register("master_waterfall_shrine", c -> MasterSitePiece.onGround(c, WaterfallShrinePiece::new, 4));
		Sites.register("master_root_temple", c -> MasterSitePiece.onGround(c, RootTemplePiece::new, 4));
		Sites.register("master_sundial_court", c -> MasterSitePiece.onGround(c, SundialCourtPiece::new, 3));
		Sites.register("master_star_terrace", c -> MasterSitePiece.onGround(c, StarTerracePiece::new, 3));
		Sites.register("master_resonance_chamber", ResonanceChamberPiece::locate);
	}
}
