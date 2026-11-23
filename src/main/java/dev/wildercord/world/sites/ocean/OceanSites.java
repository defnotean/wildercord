package dev.wildercord.world.sites.ocean;

import dev.wildercord.world.sites.Sites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Registers the 5 Deep Ocean underwater structures and their chest loot tables into {@link Sites}.
 */
public final class OceanSites {
	private OceanSites() {}

	public static final String CORAL_DOME_ID = "ocean_coral_dome_sanctuary";
	public static final String GALLEON_ID = "ocean_sunken_galleon";
	public static final String WATCHTOWER_ID = "ocean_kelp_spire_watchtower";
	public static final String FORGE_ID = "ocean_hydrothermal_forge";
	public static final String CITADEL_ID = "ocean_abyssal_citadel";

	public static final StructurePieceType CORAL_DOME_SANCTUARY = Sites.piece(
		CORAL_DOME_ID,
		(StructurePieceType.ContextlessType) CoralDomeSanctuaryPiece::new
	);
	public static final StructurePieceType SUNKEN_GALLEON = Sites.piece(
		GALLEON_ID,
		(StructurePieceType.ContextlessType) SunkenGalleonPiece::new
	);
	public static final StructurePieceType KELP_SPIRE_WATCHTOWER = Sites.piece(
		WATCHTOWER_ID,
		(StructurePieceType.ContextlessType) KelpSpireWatchtowerPiece::new
	);
	public static final StructurePieceType HYDROTHERMAL_FORGE = Sites.piece(
		FORGE_ID,
		(StructurePieceType.ContextlessType) HydrothermalForgePiece::new
	);
	public static final StructurePieceType ABYSSAL_CITADEL = Sites.piece(
		CITADEL_ID,
		(StructurePieceType.ContextlessType) AbyssalCitadelPiece::new
	);

	public static final ResourceKey<LootTable> SANCTUARY_LOOT = Sites.loot("chests/" + CORAL_DOME_ID);
	public static final ResourceKey<LootTable> GALLEON_LOOT = Sites.loot("chests/" + GALLEON_ID);
	public static final ResourceKey<LootTable> WATCHTOWER_LOOT = Sites.loot("chests/" + WATCHTOWER_ID);
	public static final ResourceKey<LootTable> FORGE_LOOT = Sites.loot("chests/" + FORGE_ID);
	public static final ResourceKey<LootTable> CITADEL_LOOT = Sites.loot("chests/" + CITADEL_ID);

	public static void init() {
		Sites.register(CORAL_DOME_ID, CoralDomeSanctuaryPiece::locate);
		Sites.register(GALLEON_ID, SunkenGalleonPiece::locate);
		Sites.register(WATCHTOWER_ID, KelpSpireWatchtowerPiece::locate);
		Sites.register(FORGE_ID, HydrothermalForgePiece::locate);
		Sites.register(CITADEL_ID, AbyssalCitadelPiece::locate);
	}
}
