package dev.wildercord.world.sites.oathhold;

import dev.wildercord.world.sites.Sites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Optional;

/**
 * Oathhold, the 2,048x2,048 Sovereign Megacity pack.
 *
 * <p>Combines a macro city anchor ({@code wildercord:oathhold}) for {@code /locate} discovery with
 * deterministic 128x128 tiling sectors ({@code wildercord:oathhold_sector}) so the entire metropolis
 * generates seamlessly across chunks without exceeding vanilla structure-reference bounds.</p>
 */
public final class OathholdSites {
	private OathholdSites() {}

	public static final String CITY_ID = "oathhold";
	public static final String SECTOR_ID = "oathhold_sector";
	public static final List<String> IDS = List.of(CITY_ID, SECTOR_ID);

	public static final StructurePieceType ANCHOR_PIECE = Sites.piece("oathhold_anchor", OathholdAnchorPiece::new);
	public static final StructurePieceType SECTOR_PIECE = Sites.piece("oathhold_sector", OathholdSectorPiece::new);

	public static final ResourceKey<LootTable> LOOT_TREASURY = Sites.loot("chests/oathhold_treasury");
	public static final ResourceKey<LootTable> LOOT_ARMORY = Sites.loot("chests/oathhold_armory");
	public static final ResourceKey<LootTable> LOOT_ARCANUM = Sites.loot("chests/oathhold_arcanum");
	public static final ResourceKey<LootTable> LOOT_MARKET = Sites.loot("chests/oathhold_market");
	public static final ResourceKey<LootTable> LOOT_BARRACKS = Sites.loot("chests/oathhold_barracks");

	public static void init() {
		Sites.register(CITY_ID, OathholdSites::locateCity);
		Sites.register(SECTOR_ID, OathholdSites::locateSector);
	}

	private static Optional<Structure.GenerationStub> locateCity(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		return OathholdOrigin.resolve(ctx, cp.x(), cp.z()).map(origin ->
			new Structure.GenerationStub(OathholdOrigin.anchorPos(origin), builder ->
				builder.addPiece(new OathholdAnchorPiece(origin.originX(), origin.baseY(), origin.originZ()))
			)
		);
	}

	private static Optional<Structure.GenerationStub> locateSector(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		return OathholdOrigin.findOwningCity(ctx, cp.x(), cp.z()).map(origin ->
			new Structure.GenerationStub(cp.getWorldPosition().atY(origin.baseY()), builder ->
				builder.addPiece(new OathholdSectorPiece(cp.x(), cp.z(), origin.originX(), origin.baseY(), origin.originZ()))
			)
		);
	}
}
