package dev.wildercord.world.sites.trials;

import dev.wildercord.trials.CoopTrialContent;
import dev.wildercord.trials.TrialBoss;
import dev.wildercord.world.dungeons.DungeonPiece;
import dev.wildercord.world.sites.Sites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Registrations and placement helpers for Multiplayer Co-Op Trial sites. */
public final class TrialSites {
	private TrialSites() {}

	public static final String SANCTUM_ID = "trial_leyline_sanctum";
	public static final String CRUCIBLE_ID = "trial_ashen_crucible";
	public static final String ECLIPSE_ID = "trial_astral_eclipse";

	public static final StructurePieceType LEYLINE_SANCTUM = Sites.piece(
		SANCTUM_ID,
		(StructurePieceType.ContextlessType) LeylineSanctumPiece::new
	);
	public static final StructurePieceType ASHEN_CRUCIBLE = Sites.piece(
		CRUCIBLE_ID,
		(StructurePieceType.ContextlessType) AshenCruciblePiece::new
	);
	public static final StructurePieceType ASTRAL_ECLIPSE = Sites.piece(
		ECLIPSE_ID,
		(StructurePieceType.ContextlessType) AstralEclipsePiece::new
	);

	public static final ResourceKey<LootTable> TREASURY_LOOT = Sites.loot("chests/trial_leyline_treasury");
	public static final ResourceKey<LootTable> CRUCIBLE_TREASURY_LOOT = Sites.loot("chests/trial_crucible_treasury");
	public static final ResourceKey<LootTable> ECLIPSE_TREASURY_LOOT = Sites.loot("chests/trial_eclipse_treasury");

	public static void init() {
		Sites.register(SANCTUM_ID, LeylineSanctumPiece::locate);
		Sites.register(CRUCIBLE_ID, AshenCruciblePiece::locate);
		Sites.register(ECLIPSE_ID, AstralEclipsePiece::locate);
	}

	public static Set<String> allSiteIds() {
		return Set.of(SANCTUM_ID, CRUCIBLE_ID, ECLIPSE_ID);
	}

	public static @Nullable TrialBoss createBoss(ServerLevel level, String trialKind) {
		return switch (trialKind.toLowerCase(Locale.ROOT)) {
			case "leyline" -> CoopTrialContent.VAELKHOR.create(level, EntitySpawnReason.STRUCTURE);
			case "crucible" -> CoopTrialContent.KHARVOK.create(level, EntitySpawnReason.STRUCTURE);
			case "eclipse" -> CoopTrialContent.NYXARA.create(level, EntitySpawnReason.STRUCTURE);
			default -> null;
		};
	}

	/** Places a registered Co-Op Trial Sanctum at {@code origin} and binds its StructureStart for ward & gate checks. */
	public static @Nullable TrialArenaPiece placeSanctum(ServerLevel level, BlockPos origin, Direction facing, String kind) {
		Direction dir = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
		String normalized = kind.toLowerCase(Locale.ROOT);
		DungeonPiece piece = switch (normalized) {
			case "leyline" -> new LeylineSanctumPiece(
				origin.getX() - 20, origin.getY() - LeylineSanctumPiece.FLOOR - 1, origin.getZ() - 3, dir);
			case "crucible" -> new AshenCruciblePiece(
				origin.getX() - 20, origin.getY() - AshenCruciblePiece.FLOOR - 1, origin.getZ() - 3, dir);
			case "eclipse" -> new AstralEclipsePiece(
				origin.getX() - 20, origin.getY() - AstralEclipsePiece.FLOOR - 1, origin.getZ() - 3, dir);
			default -> null;
		};
		if (!(piece instanceof TrialArenaPiece arena)) {
			return null;
		}
		BoundingBox bb = piece.getBoundingBox();
		for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
			for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
				level.getChunk(cx, cz);
			}
		}
		ChunkPos startChunk = new ChunkPos(bb.minX() >> 4, bb.minZ() >> 4);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
			RandomSource.create(origin.asLong()), bb, startChunk, BlockPos.ZERO);

		var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
			.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.parse("wildercord:" + arena.structureId()))).value();
		var start = new StructureStart(structure, startChunk, 0, new PiecesContainer(List.<StructurePiece>of(piece)));
		for (int cx = bb.minX() >> 4; cx <= bb.maxX() >> 4; cx++) {
			for (int cz = bb.minZ() >> 4; cz <= bb.maxZ() >> 4; cz++) {
				var chunk = level.getChunk(cx, cz);
				chunk.addReferenceForStructure(structure, startChunk.pack());
				chunk.setStartForStructure(structure, start);
			}
		}
		return arena;
	}
}
