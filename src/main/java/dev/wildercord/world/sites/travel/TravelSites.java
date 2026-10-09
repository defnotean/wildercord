package dev.wildercord.world.sites.travel;

import dev.wildercord.lore.LoreJournal;
import dev.wildercord.spell.CircleVows;
import dev.wildercord.world.sites.SiteStructure;
import dev.wildercord.world.sites.Sites;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The travel pack: eight peaceful roadside sites (an inn, a watchtower, a broken bridge, a rune library, standing
 * stones, a caravan camp, a cartographer's hut and a vow circle). Each is found by walking the overworld; stepping
 * inside one writes it into the lore journal, and its books teach the Rune Catalog's Travel and Exploring runes,
 * the roads between sites and the Circle Vows.
 */
public final class TravelSites {
	private TravelSites() {}

	public static final String INN = "travel_wayfarer_inn", TOWER = "travel_watchtower", BRIDGE = "travel_broken_bridge",
		LIBRARY = "travel_rune_library", STONES = "travel_standing_stones", CARAVAN = "travel_caravan_camp",
		CARTOGRAPHER = "travel_cartographer_hut", VOWS = "travel_vow_circle";
	public static final List<String> IDS = List.of(INN, TOWER, BRIDGE, LIBRARY, STONES, CARAVAN, CARTOGRAPHER, VOWS);

	/** Every fixed book: its translation stem and page count. Pages are {@code book.wildercord.<stem>.<n>}. */
	public static final Map<String, Integer> BOOKS = new LinkedHashMap<>();
	static {
		BOOKS.put("travel_ledger", 3);
		BOOKS.put("travel_watch", 2);
		BOOKS.put("travel_toll", 2);
		BOOKS.put("travel_catalog", 3);
		BOOKS.put("travel_first_roads", 4);
		BOOKS.put("travel_survey", 3);
		BOOKS.put("travel_vows", 2);
	}
	/** The library's riddle has one page per layout, {@code book.wildercord.travel_riddle.<variant>}. */
	public static final int RIDDLES = 3;

	static StructurePieceType INN_PIECE, TOWER_PIECE, BRIDGE_PIECE, LIBRARY_PIECE, STONES_PIECE, CARAVAN_PIECE, CARTOGRAPHER_PIECE, VOWS_PIECE;

	static ResourceKey<LootTable> loot(String site) { return Sites.loot("chests/" + site); }

	public static void init() {
		INN_PIECE = Sites.piece(INN, WayfarerInnPiece::new);
		TOWER_PIECE = Sites.piece(TOWER, WatchtowerPiece::new);
		BRIDGE_PIECE = Sites.piece(BRIDGE, BrokenBridgePiece::new);
		LIBRARY_PIECE = Sites.piece(LIBRARY, RuneLibraryPiece::new);
		STONES_PIECE = Sites.piece(STONES, StandingStonesPiece::new);
		CARAVAN_PIECE = Sites.piece(CARAVAN, CaravanCampPiece::new);
		CARTOGRAPHER_PIECE = Sites.piece(CARTOGRAPHER, CartographerHutPiece::new);
		VOWS_PIECE = Sites.piece(VOWS, VowCirclePiece::new);
		Sites.register(INN, TravelPiece.flat(WayfarerInnPiece::new, 11, 12, 9, 4));
		Sites.register(TOWER, TravelPiece.flat(WatchtowerPiece::new, 5, 5, 4, 6));
		Sites.register(BRIDGE, TravelPiece.flat(BrokenBridgePiece::new, 7, 12, 8, 3));
		Sites.register(LIBRARY, TravelPiece.flat(RuneLibraryPiece::new, 7, 8, 7, 3));
		Sites.register(STONES, TravelPiece.flat(StandingStonesPiece::new, 10, 10, 8, 4));
		Sites.register(CARAVAN, TravelPiece.flat(CaravanCampPiece::new, 11, 9, 9, 3));
		Sites.register(CARTOGRAPHER, TravelPiece.flat(CartographerHutPiece::new, 5, 5, 5, 3));
		Sites.register(VOWS, TravelPiece.flat(VowCirclePiece::new, 9, 9, 8, 3));
		// Walking into a travel site writes it under Places in the lore journal (H), once.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) return;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isSpectator()) continue;
				var start = player.level().structureManager().getStructureWithPieceAt(player.blockPosition(), Sites.ALL_SITES);
				if (start.isValid() && start.getStructure() instanceof SiteStructure site && IDS.contains(site.site()))
					LoreJournal.record(player, "place:" + site.site(), true);
			}
		});
	}

	/** A written book whose pages are translated on the reader's side. */
	static ItemStack book(String stem, String title, String author) {
		List<Component> pages = new ArrayList<>();
		for (int i = 1; i <= BOOKS.get(stem); i++) pages.add(Component.translatable("book.wildercord." + stem + "." + i));
		return written(title, author, pages);
	}

	static ItemStack written(String title, String author, List<Component> pages) {
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 0,
			pages.stream().map(Filterable::passThrough).toList(), true));
		return stack;
	}

	/** The vow circle's book, written from the vows themselves so it never drifts from them. */
	static ItemStack vowBook() {
		List<Component> pages = new ArrayList<>();
		pages.add(Component.translatable("book.wildercord.travel_vows.1"));
		for (CircleVows.Vow vow : CircleVows.ALL)
			pages.add(Component.translatable("book.wildercord.travel_vows.vow", vow.numeral(), vow.first().name(), vow.first().text(),
				vow.second().name(), vow.second().text(), vow.first().id(), vow.second().id()));
		pages.add(Component.translatable("book.wildercord.travel_vows.2", CircleVows.RELEASE_LEVELS));
		return written("Vows of the Circle", "The Wayfarers", pages);
	}
}
