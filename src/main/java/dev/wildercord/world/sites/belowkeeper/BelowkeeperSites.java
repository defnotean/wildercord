package dev.wildercord.world.sites.belowkeeper;

import dev.wildercord.Wildercord;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.hollowbell.HollowBellContent;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.Sites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.*;

/**
 * The Belowkeeper sites pack: two Belowkeeper Hollows underground landmarks — the Belowkeeper Listening Station
 * (a vaulted deepslate-and-copper acoustic chamber with a damp Glowcap basin, resonance shutter lamp, and persistent
 * Belowkeeper Listener resident) and the Belowkeeper Bell Sanctum (a vaulted resonance court where the optional
 * Hollow Bell encounter waits).
 */
public final class BelowkeeperSites {
	private BelowkeeperSites() {}

	public static final String LISTENING_STATION = "belowkeeper_listening_station";
	public static final String BELL_SANCTUM = "belowkeeper_bell_sanctum";
	public static final String SHUTTER_MARK_ENTRY = "learned:belowkeeper_shutter";
	public static final Set<String> IDS = Set.of(LISTENING_STATION, BELL_SANCTUM);
	public static final Map<String, Integer> BOOKS = Map.of("belowkeeper_station", 4, "bell_sanctum", 2);

	public static final StructurePieceType STATION_PIECE = Sites.piece(LISTENING_STATION, (StructurePieceType.ContextlessType) ListeningStationPiece::new);
	public static final StructurePieceType SANCTUM_PIECE = Sites.piece(BELL_SANCTUM, (StructurePieceType.ContextlessType) BellSanctumPiece::new);

	public static void init() {
		Sites.register(LISTENING_STATION, BelowkeeperSites::locateListeningStation);
		Sites.register(BELL_SANCTUM, BelowkeeperSites::locateBellSanctum);
		BelowkeeperResident.init();
		HollowBellContent.init();
	}

	public static void observe(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) return;
		var level = player.level();
		var start = level.structureManager().getStructureWithPieceAt(player.blockPosition(),
			holder -> holder.value() instanceof SiteStructure site && IDS.contains(site.site()));
		if (!start.isValid() || !(start.getStructure() instanceof SiteStructure site)) return;
		LoreJournal.record(player, "place:" + site.site(), true);
		if (!site.site().equals(LISTENING_STATION) || LoreJournal.data(player).has(SHUTTER_MARK_ENTRY)) return;
		for (var piece : start.getPieces()) {
			if (!(piece instanceof ListeningStationPiece station)) continue;
			var lamp = station.lampPos();
			if (!lamp.closerToCenterThan(player.position(), 6) || !level.hasChunkAt(lamp)) continue;
			var state = level.getBlockState(lamp);
			if (state.is(Blocks.REDSTONE_LAMP) && state.getValue(RedstoneLampBlock.LIT)) {
				LoreJournal.record(player, SHUTTER_MARK_ENTRY, true);
				return;
			}
		}
	}

	public static Optional<Structure.GenerationStub> locateListeningStation(Structure.GenerationContext ctx) {
		int x = ctx.chunkPos().getMinBlockX() + 4, z = ctx.chunkPos().getMinBlockZ() + 4;
		int cx = x + 10, cz = z + 10;
		OptionalInt yOpt = findVaultY(ctx, x, z, 20, 10, cx, cz);
		if (yOpt.isEmpty()) return Optional.empty();
		int y = yOpt.getAsInt();
		return Optional.of(new Structure.GenerationStub(new BlockPos(cx, y, cz),
			b -> b.addPiece(new ListeningStationPiece(x, y - ListeningStationPiece.DECK, z, Direction.SOUTH))));
	}

	public static Optional<Structure.GenerationStub> locateBellSanctum(Structure.GenerationContext ctx) {
		int x = ctx.chunkPos().getMinBlockX() + 2, z = ctx.chunkPos().getMinBlockZ() + 2;
		int cx = x + 17, cz = z + 17;
		OptionalInt yOpt = findVaultY(ctx, x, z, 34, 12, cx, cz);
		if (yOpt.isEmpty()) return Optional.empty();
		int y = yOpt.getAsInt();
		return Optional.of(new Structure.GenerationStub(new BlockPos(cx, y, cz),
			b -> b.addPiece(new BellSanctumPiece(x, y - BellSanctumPiece.DECK, z, Direction.SOUTH))));
	}

	private static OptionalInt findVaultY(Structure.GenerationContext ctx, int x, int z, int span, int height, int cx, int cz) {
		int sea = ctx.chunkGenerator().getSeaLevel();
		int surface = ctx.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
		if (surface < sea) return OptionalInt.empty();
		var centerCol = ctx.chunkGenerator().getBaseColumn(cx, cz, ctx.heightAccessor(), ctx.randomState());
		if (centerCol.getBlock(surface).is(Blocks.WATER) || centerCol.getBlock(surface - 1).is(Blocks.WATER)) return OptionalInt.empty();
		for (int y = 24; y >= -24; y -= 8) {
			if (surface < y + height + 10) continue;
			var biome = ctx.biomeResolver().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(y), QuartPos.fromBlock(cz));
			if (!ctx.validBiome().test(biome)) continue;
			if (drySpan(ctx, x, z, span, y - 2, y + height)) return OptionalInt.of(y);
		}
		return OptionalInt.empty();
	}

	private static boolean drySpan(Structure.GenerationContext ctx, int x, int z, int span, int y0, int y1) {
		int[][] probes = {{x + 2, z + 2}, {x + span - 2, z + 2}, {x + 2, z + span - 2}, {x + span - 2, z + span - 2}, {x + span / 2, z + span / 2}};
		for (int[] p : probes) {
			var col = ctx.chunkGenerator().getBaseColumn(p[0], p[1], ctx.heightAccessor(), ctx.randomState());
			for (int y = y0; y <= y1; y++) {
				var state = col.getBlock(y);
				if (!state.getFluidState().isEmpty()) return false;
			}
		}
		return true;
	}

	public static ResourceKey<LootTable> loot(String site) {
		return ResourceKey.create(Registries.LOOT_TABLE, Wildercord.id("chests/" + site));
	}

	public static ItemStack book(String stem, String title, String author) {
		int count = BOOKS.getOrDefault(stem, 1);
		List<Filterable<Component>> pages = new ArrayList<>(count);
		for (int i = 1; i <= count; i++) pages.add(Filterable.passThrough(Component.translatable("book.wildercord." + stem + "." + i)));
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0, pages, true));
		return stack;
	}
}
