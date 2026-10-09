package dev.wildercord.world.sites.mine;

import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.Sites;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The mining sites: mines, caves, forges and crystals, each pointing at the mining and crafting runes and the Iron and Stone
 * breaths. Data (where, how often, loot) comes from tools/sites_mine.py. Walking into one writes it in the lore journal.
 */
public final class MineSites {
	private MineSites() {}

	public static final StructurePieceType HILLSIDE_MINE = Sites.piece("mine_hillside_mine", HillsideMinePiece::new);
	public static final StructurePieceType FORGE_HALL = Sites.piece("mine_forge_hall", ForgeHallPiece::new);
	public static final StructurePieceType CRYSTAL_LAB = Sites.piece("mine_crystal_lab", CrystalLabPiece::new);
	public static final StructurePieceType COLLAPSED_DELVE = Sites.piece("mine_collapsed_delve", CollapsedDelvePiece::new);
	public static final StructurePieceType BASALT_FOUNDRY = Sites.piece("mine_basalt_foundry", BasaltFoundryPiece::new);
	public static final StructurePieceType DEEP_VAULT = Sites.piece("mine_deep_vault", DeepVaultPiece::new);
	public static final StructurePieceType PROSPECTOR_CAMP = Sites.piece("mine_prospector_camp", ProspectorCampPiece::new);
	public static final StructurePieceType MINERS_REST = Sites.piece("mine_miners_rest", MinersRestPiece::new);

	static final ResourceKey<LootTable> HILLSIDE = Sites.loot("chests/mine_hillside_mine"), HILLSIDE_TALLY = Sites.loot("chests/mine_hillside_mine_tally");
	static final ResourceKey<LootTable> FORGE = Sites.loot("chests/mine_forge_hall"), FORGE_MASTERWORK = Sites.loot("chests/mine_forge_hall_masterwork");
	static final ResourceKey<LootTable> LAB = Sites.loot("chests/mine_crystal_lab");
	static final ResourceKey<LootTable> DELVE = Sites.loot("chests/mine_collapsed_delve"), DELVE_CACHE = Sites.loot("chests/mine_collapsed_delve_cache");
	static final ResourceKey<LootTable> FOUNDRY = Sites.loot("chests/mine_basalt_foundry");
	static final ResourceKey<LootTable> VAULT = Sites.loot("chests/mine_deep_vault"), VAULT_ANTECHAMBER = Sites.loot("chests/mine_deep_vault_antechamber");
	static final ResourceKey<LootTable> CAMP = Sites.loot("chests/mine_prospector_camp");
	static final ResourceKey<LootTable> REST = Sites.loot("chests/mine_miners_rest");

	/** How often players are checked for standing in a mining site, in ticks. */
	static final int JOURNAL_TICKS = 40;

	public static void init() {
		Sites.register("mine_hillside_mine", HillsideMinePiece::locate);
		Sites.register("mine_forge_hall", ForgeHallPiece::locate);
		Sites.register("mine_crystal_lab", CrystalLabPiece::locate);
		Sites.register("mine_collapsed_delve", CollapsedDelvePiece::locate);
		Sites.register("mine_basalt_foundry", BasaltFoundryPiece::locate);
		Sites.register("mine_deep_vault", DeepVaultPiece::locate);
		Sites.register("mine_prospector_camp", ProspectorCampPiece::locate);
		Sites.register("mine_miners_rest", MinersRestPiece::locate);
		ServerTickEvents.END_SERVER_TICK.register(MineSites::tick);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % JOURNAL_TICKS != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator()) continue;
			var start = player.level().structureManager().getStructureWithPieceAt(player.blockPosition(), Sites.ALL_SITES);
			if (start.isValid() && start.getStructure() instanceof SiteStructure site && site.site().startsWith("mine_")) {
				LoreJournal.record(player, "place:" + site.site(), true);
			}
		}
	}
}
