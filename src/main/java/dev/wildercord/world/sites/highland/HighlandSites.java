package dev.wildercord.world.sites.highland;

import dev.wildercord.Wildercord;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.cartographer.CartographerContent;
import dev.wildercord.world.sites.Sites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
 * The highland sites pack: two Windscar Highlands landmarks — the Highland Cairn Lodge (a Marchkeeper ridge
 * shelter with a cultivated leeward Windreed terrace, cairn lamp, and persistent Cairn Keeper resident) and
 * the Highland Survey Ring (an open stone meridian court where the optional Glass Cartographer encounter waits).
 */
public final class HighlandSites {
	private HighlandSites() {}

	public static final String CAIRN_LODGE = "highland_cairn_lodge";
	public static final String SURVEY_RING = "highland_survey_ring";
	public static final String WIND_MARK_ENTRY = "learned:scout_wind_mark";
	public static final List<String> IDS = List.of(CAIRN_LODGE, SURVEY_RING);
	public static final Map<String, Integer> BOOKS = Map.of("cairn_keeper", 4, "survey_ring", 2);

	public static final StructurePieceType LODGE_PIECE = piece("cairn_lodge", CairnLodgePiece::new);
	public static final StructurePieceType RING_PIECE = piece("survey_ring", SurveyRingPiece::new);

	private static StructurePieceType piece(String id, StructurePieceType.ContextlessType type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("highland_" + id), type);
	}

	public static void init() {
		Sites.register(CAIRN_LODGE, HighlandSites::locateCairnLodge);
		Sites.register(SURVEY_RING, HighlandSites::locateSurveyRing);
		CairnKeeperResident.init();
		CartographerContent.init();
	}

	public static void observe(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		var structures = level.structureManager();
		if (!structures.hasAnyStructureAt(player.blockPosition())) return;
		var pos = player.blockPosition();
		for (String site : IDS) {
			var key = ResourceKey.create(Registries.STRUCTURE, Wildercord.id(site));
			if (!structures.getStructureWithPieceAt(pos, key).isValid()) continue;
			LoreJournal.record(player, "place:" + site, true);
			if (site.equals(CAIRN_LODGE) && nearLitCairnLamp(level, pos)) {
				LoreJournal.record(player, WIND_MARK_ENTRY, true);
			}
		}
	}

	private static boolean nearLitCairnLamp(ServerLevel level, BlockPos center) {
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -4, -6), center.offset(6, 6, 6))) {
			var state = level.getBlockState(pos);
			if (state.is(Blocks.REDSTONE_LAMP) && state.getValue(RedstoneLampBlock.LIT)) return true;
		}
		return false;
	}

	public static Optional<Structure.GenerationStub> locateCairnLodge(Structure.GenerationContext ctx) {
		int x = ctx.chunkPos().getMinBlockX() + 4, z = ctx.chunkPos().getMinBlockZ() + 4;
		int sea = ctx.chunkGenerator().getSeaLevel();
		int[] heights = new int[36];
		int idx = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int dx = 0; dx <= 20; dx += 4) for (int dz = 0; dz <= 20; dz += 4) {
			int wx = x + dx, wz = z + dz;
			int h = ctx.chunkGenerator().getFirstOccupiedHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
			if (h < sea + 2) return Optional.empty();
			var col = ctx.chunkGenerator().getBaseColumn(wx, wz, ctx.heightAccessor(), ctx.randomState());
			if (col.getBlock(h).is(Blocks.WATER) || col.getBlock(h - 1).is(Blocks.WATER)) return Optional.empty();
			heights[idx++] = h;
			min = Math.min(min, h);
			max = Math.max(max, h);
		}
		if (max - min > 12) return Optional.empty();
		Arrays.sort(heights);
		int ground = heights[heights.length / 2];
		return Optional.of(new Structure.GenerationStub(new BlockPos(x, ground, z),
			b -> b.addPiece(new CairnLodgePiece(x, ground - CairnLodgePiece.DECK, z, Direction.SOUTH))));
	}

	public static Optional<Structure.GenerationStub> locateSurveyRing(Structure.GenerationContext ctx) {
		int x = ctx.chunkPos().getMinBlockX() + 2, z = ctx.chunkPos().getMinBlockZ() + 2;
		int sea = ctx.chunkGenerator().getSeaLevel();
		int[] heights = new int[36];
		int idx = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int dx = 0; dx <= 30; dx += 6) for (int dz = 0; dz <= 30; dz += 6) {
			int wx = x + dx, wz = z + dz;
			int h = ctx.chunkGenerator().getFirstOccupiedHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
			if (h < sea + 2) return Optional.empty();
			var col = ctx.chunkGenerator().getBaseColumn(wx, wz, ctx.heightAccessor(), ctx.randomState());
			if (col.getBlock(h).is(Blocks.WATER) || col.getBlock(h - 1).is(Blocks.WATER)) return Optional.empty();
			heights[idx++] = h;
			min = Math.min(min, h);
			max = Math.max(max, h);
		}
		if (max - min > 14) return Optional.empty();
		Arrays.sort(heights);
		int ground = heights[heights.length / 2];
		return Optional.of(new Structure.GenerationStub(new BlockPos(x, ground, z),
			b -> b.addPiece(new SurveyRingPiece(x, ground - SurveyRingPiece.DECK, z, Direction.SOUTH))));
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
