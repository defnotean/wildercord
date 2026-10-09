package dev.wildercord.world.sites.wilds;

import dev.wildercord.world.sites.Sites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * The wild places: elemental ruins standing where each element is strongest, from jungle and desert to the deep dark,
 * the Nether and the End's outer islands. Their worldgen data, loot and text come from tools/sites_wilds.py.
 */
public final class WildsSites {
	private WildsSites() {}

	public static final List<String> IDS = List.of("wilds_venom_ziggurat", "wilds_dune_temple", "wilds_rime_monastery", "wilds_iron_gatehouse",
		"wilds_dawn_pavilion", "wilds_echo_post", "wilds_ember_outpost", "wilds_void_lantern");

	public static final StructurePieceType VENOM_ZIGGURAT = Sites.piece("wilds_venom_ziggurat", VenomZigguratPiece::new);
	public static final StructurePieceType DUNE_TEMPLE = Sites.piece("wilds_dune_temple", DuneTemplePiece::new);
	public static final StructurePieceType RIME_MONASTERY = Sites.piece("wilds_rime_monastery", RimeMonasteryPiece::new);
	public static final StructurePieceType IRON_GATEHOUSE = Sites.piece("wilds_iron_gatehouse", IronGatehousePiece::new);
	public static final StructurePieceType DAWN_PAVILION = Sites.piece("wilds_dawn_pavilion", DawnPavilionPiece::new);
	public static final StructurePieceType ECHO_POST = Sites.piece("wilds_echo_post", EchoPostPiece::new);
	public static final StructurePieceType EMBER_OUTPOST = Sites.piece("wilds_ember_outpost", EmberOutpostPiece::new);
	public static final StructurePieceType VOID_LANTERN = Sites.piece("wilds_void_lantern", VoidLanternPiece::new);

	static final ResourceKey<LootTable> ZIGGURAT_VAULT = Sites.loot("chests/wilds_venom_ziggurat"), ZIGGURAT_HALL = Sites.loot("chests/wilds_venom_ziggurat_hall"),
		ZIGGURAT_DARTS = Sites.loot("chests/wilds_venom_ziggurat_darts");
	static final ResourceKey<LootTable> DUNE_VAULT = Sites.loot("chests/wilds_dune_temple"), DUNE_HALL = Sites.loot("chests/wilds_dune_temple_hall"),
		DUNE_SAND = Sites.loot("chests/wilds_dune_temple_sand");
	static final ResourceKey<LootTable> RIME_VAULT = Sites.loot("chests/wilds_rime_monastery"), RIME_HALL = Sites.loot("chests/wilds_rime_monastery_hall");
	static final ResourceKey<LootTable> IRON_VAULT = Sites.loot("chests/wilds_iron_gatehouse"), IRON_HALL = Sites.loot("chests/wilds_iron_gatehouse_hall");
	static final ResourceKey<LootTable> DAWN_VAULT = Sites.loot("chests/wilds_dawn_pavilion"), DAWN_HALL = Sites.loot("chests/wilds_dawn_pavilion_hall");
	static final ResourceKey<LootTable> ECHO_VAULT = Sites.loot("chests/wilds_echo_post"), ECHO_HALL = Sites.loot("chests/wilds_echo_post_hall");
	static final ResourceKey<LootTable> EMBER_VAULT = Sites.loot("chests/wilds_ember_outpost"), EMBER_HALL = Sites.loot("chests/wilds_ember_outpost_hall");
	static final ResourceKey<LootTable> VOID_VAULT = Sites.loot("chests/wilds_void_lantern"), VOID_HALL = Sites.loot("chests/wilds_void_lantern_hall");

	public static void init() {
		Sites.register("wilds_venom_ziggurat", VenomZigguratPiece::locate);
		Sites.register("wilds_dune_temple", DuneTemplePiece::locate);
		Sites.register("wilds_rime_monastery", RimeMonasteryPiece::locate);
		Sites.register("wilds_iron_gatehouse", IronGatehousePiece::locate);
		Sites.register("wilds_dawn_pavilion", DawnPavilionPiece::locate);
		Sites.register("wilds_echo_post", EchoPostPiece::locate);
		Sites.register("wilds_ember_outpost", EmberOutpostPiece::locate);
		Sites.register("wilds_void_lantern", VoidLanternPiece::locate);
	}
}
