package dev.wildercord.world.sites.emberwood;

import dev.wildercord.Wildercord;
import dev.wildercord.lore.LoreJournal;
import dev.wildercord.world.kilnwidow.KilnWidowContent;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.Sites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The Emberwood sites pack: two Emberwood Reach landmarks — the Emberwood Charcoal Yard (a brick-and-timber
 * burner's yard with a cultivated Cinder Fern ash bed, kiln draft lamp, and persistent Kiln Keeper resident)
 * and the Emberwood Kiln Court (an open ceramic kiln court where the optional Kiln Widow encounter waits).
 */
public final class EmberwoodSites {
	private EmberwoodSites() {}

	public static final String CHARCOAL_YARD = "emberwood_charcoal_yard";
	public static final String KILN_COURT = "emberwood_kiln_court";
	public static final String DRAFT_MARK_ENTRY = "learned:emberwood_draft";
	public static final Set<String> IDS = Set.of(CHARCOAL_YARD, KILN_COURT);
	public static final Map<String, Integer> BOOKS = Map.of("emberwood_yard", 4, "kiln_court", 2);

	public static final StructurePieceType YARD_PIECE = Sites.piece(CHARCOAL_YARD, (StructurePieceType.ContextlessType) CharcoalYardPiece::new);
	public static final StructurePieceType COURT_PIECE = Sites.piece(KILN_COURT, (StructurePieceType.ContextlessType) KilnCourtPiece::new);

	public static void init() {
		Sites.register(CHARCOAL_YARD, EmberwoodSites::locateCharcoalYard);
		Sites.register(KILN_COURT, EmberwoodSites::locateKilnCourt);
		KilnKeeperResident.init();
		KilnWidowContent.init();
	}

	public static void observe(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) return;
		var level = player.level();
		var start = level.structureManager().getStructureWithPieceAt(player.blockPosition(),
			holder -> holder.value() instanceof SiteStructure site && IDS.contains(site.site()));
		if (!start.isValid() || !(start.getStructure() instanceof SiteStructure site)) return;
		LoreJournal.record(player, "place:" + site.site(), true);
		if (!site.site().equals(CHARCOAL_YARD) || LoreJournal.data(player).has(DRAFT_MARK_ENTRY)) return;
		for (var piece : start.getPieces()) {
			if (!(piece instanceof CharcoalYardPiece yard)) continue;
			var lamp = yard.lampPos();
			if (!lamp.closerToCenterThan(player.position(), 6) || !level.hasChunkAt(lamp)) continue;
			var state = level.getBlockState(lamp);
			if (state.is(Blocks.REDSTONE_LAMP) && state.getValue(RedstoneLampBlock.LIT)) {
				LoreJournal.record(player, DRAFT_MARK_ENTRY, true);
				return;
			}
		}
	}

	public static Optional<Structure.GenerationStub> locateCharcoalYard(Structure.GenerationContext ctx) {
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
			b -> b.addPiece(new CharcoalYardPiece(x, ground - CharcoalYardPiece.DECK, z, Direction.SOUTH))));
	}

	public static Optional<Structure.GenerationStub> locateKilnCourt(Structure.GenerationContext ctx) {
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
			b -> b.addPiece(new KilnCourtPiece(x, ground - KilnCourtPiece.DECK, z, Direction.SOUTH))));
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
