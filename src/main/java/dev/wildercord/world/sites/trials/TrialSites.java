package dev.wildercord.world.sites.trials;

import dev.wildercord.world.sites.Sites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/** Registers the Leyline Sanctum Co-Op Trial structure and chest loot table. */
public final class TrialSites {
	private TrialSites() {}

	public static final String SANCTUM_ID = "trial_leyline_sanctum";

	public static final StructurePieceType LEYLINE_SANCTUM = Sites.piece(
		SANCTUM_ID,
		(StructurePieceType.ContextlessType) LeylineSanctumPiece::new
	);

	public static final ResourceKey<LootTable> TREASURY_LOOT = Sites.loot("chests/trial_leyline_treasury");

	public static void init() {
		Sites.register(SANCTUM_ID, LeylineSanctumPiece::locate);
	}
}
