package dev.wildercord.world.sites.water;

import dev.wildercord.world.sites.Sites;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.lore.LoreJournal;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The water pack: nine places on rivers, swamps, coasts, the sea floor and frozen lakes, for anglers and those who
 * breathe the Tide. Where each appears and how often is in tools/sites_water.py; what each looks like is its piece.
 */
public final class WaterSites {
	private WaterSites() {}
	public static final String COAST_LIGHT_ENTRY = "learned:coast_light";
	public static final java.util.Set<String> IDS = java.util.Set.of("water_reedkeeper_landing", "water_stilt_smokehouse",
		"water_lighthouse", "water_watermill", "water_ice_camp", "water_pier_boathouse", "water_sunken_shrine",
		"water_netweaver_village", "water_tidepool_grotto");
	public static final StructurePieceType REEDKEEPER_LANDING = Sites.piece("water_reedkeeper_landing", (StructurePieceType.ContextlessType) ReedkeeperLandingPiece::new);
	static final ResourceKey<LootTable> REEDKEEPER_LOOT = Sites.loot("chests/water_reedkeeper_landing");

	public static final StructurePieceType STILT_SMOKEHOUSE = Sites.piece("water_stilt_smokehouse", (StructurePieceType.ContextlessType) StiltSmokehousePiece::new);
	public static final StructurePieceType LIGHTHOUSE = Sites.piece("water_lighthouse", (StructurePieceType.ContextlessType) LighthousePiece::new);
	public static final StructurePieceType WATERMILL = Sites.piece("water_watermill", (StructurePieceType.ContextlessType) WatermillPiece::new);
	public static final StructurePieceType ICE_CAMP = Sites.piece("water_ice_camp", (StructurePieceType.ContextlessType) IceCampPiece::new);
	public static final StructurePieceType PIER_BOATHOUSE = Sites.piece("water_pier_boathouse", (StructurePieceType.ContextlessType) PierBoathousePiece::new);
	public static final StructurePieceType SUNKEN_SHRINE = Sites.piece("water_sunken_shrine", (StructurePieceType.ContextlessType) SunkenShrinePiece::new);
	public static final StructurePieceType NETWEAVER_VILLAGE = Sites.piece("water_netweaver_village", (StructurePieceType.ContextlessType) NetweaverVillagePiece::new);
	public static final StructurePieceType TIDEPOOL_GROTTO = Sites.piece("water_tidepool_grotto", (StructurePieceType.ContextlessType) TidepoolGrottoPiece::new);

	static final ResourceKey<LootTable> SMOKEHOUSE_LOOT = Sites.loot("chests/water_stilt_smokehouse");
	static final ResourceKey<LootTable> LIGHTHOUSE_LOOT = Sites.loot("chests/water_lighthouse");
	static final ResourceKey<LootTable> WATERMILL_LOOT = Sites.loot("chests/water_watermill");
	static final ResourceKey<LootTable> ICE_CAMP_LOOT = Sites.loot("chests/water_ice_camp");
	static final ResourceKey<LootTable> BOATHOUSE_LOOT = Sites.loot("chests/water_pier_boathouse");
	static final ResourceKey<LootTable> SHRINE_LOOT = Sites.loot("chests/water_sunken_shrine");
	static final ResourceKey<LootTable> NETWEAVER_LOOT = Sites.loot("chests/water_netweaver_village");
	static final ResourceKey<LootTable> GROTTO_LOOT = Sites.loot("chests/water_tidepool_grotto");

	public static void init() {
		Sites.register("water_reedkeeper_landing", ReedkeeperLandingPiece::locate);
		ReedkeeperResident.init();
		Sites.register("water_stilt_smokehouse", StiltSmokehousePiece::locate);
		Sites.register("water_lighthouse", LighthousePiece::locate);
		Sites.register("water_watermill", WatermillPiece::locate);
		Sites.register("water_ice_camp", IceCampPiece::locate);
		Sites.register("water_pier_boathouse", PierBoathousePiece::locate);
		Sites.register("water_sunken_shrine", SunkenShrinePiece::locate);
		Sites.register("water_netweaver_village", NetweaverVillagePiece::locate);
		Sites.register("water_tidepool_grotto", TidepoolGrottoPiece::locate);
	}

	/** One bounded structure lookup in the journal's existing poll; no terrain edits or block-volume search. */
	public static void observe(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) return;
		var level = player.level();
		var start = level.structureManager().getStructureWithPieceAt(player.blockPosition(),
			holder -> holder.value() instanceof SiteStructure site && IDS.contains(site.site()));
		if (!start.isValid() || !(start.getStructure() instanceof SiteStructure site)) return;
		LoreJournal.record(player, "place:" + site.site(), true);
		if (!site.site().equals("water_lighthouse") || LoreJournal.data(player).has(COAST_LIGHT_ENTRY)) return;
		for (var piece : start.getPieces()) {
			if (!(piece instanceof LighthousePiece lighthouse)) continue;
			var lamp = lighthouse.lampPosition();
			if (!lamp.closerToCenterThan(player.position(), 6) || !level.hasChunkAt(lamp)) continue;
			var state = level.getBlockState(lamp);
			if (state.is(Blocks.REDSTONE_LAMP) && state.getValue(RedstoneLampBlock.LIT)) {
				LoreJournal.record(player, COAST_LIGHT_ENTRY, true);
				return;
			}
		}
	}
}
