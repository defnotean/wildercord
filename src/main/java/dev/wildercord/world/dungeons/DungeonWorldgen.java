package dev.wildercord.world.dungeons;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The dimension dungeons' registrations. Where each may appear and how often live in data:
 * {@code worldgen/structure/<dungeon>.json}, {@code worldgen/structure_set/<dungeon>.json} and a
 * biome tag each, all written by {@code tools/dungeon_assets.py}.
 */
public final class DungeonWorldgen {
	private DungeonWorldgen() {}

	public static final StructureType<DungeonStructure> DUNGEON = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Wildercord.id("dungeon"),
		() -> DungeonStructure.CODEC);

	public static final StructurePieceType EMBER_SANCTUM_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("ember_sanctum"),
		(StructurePieceType.ContextlessType) EmberSanctumPiece::new);
	public static final StructurePieceType ASTRAL_OBSERVATORY_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("astral_observatory"),
		(StructurePieceType.ContextlessType) AstralObservatoryPiece::new);
	public static final StructurePieceType DROWNED_SCRIPTORIUM_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("drowned_scriptorium"),
		(StructurePieceType.ContextlessType) DrownedScriptoriumPiece::new);
	public static final StructurePieceType ROOTBOUND_MAZE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("rootbound_maze"),
		(StructurePieceType.ContextlessType) RootboundMazePiece::new);
	public static final StructurePieceType STORM_SPIRE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("storm_spire"),
		(StructurePieceType.ContextlessType) StormSpirePiece::new);

	/** Every dimension dungeon (not the Archive): inside one, a third of the monsters carry Cords. */
	public static final TagKey<Structure> DUNGEONS = TagKey.create(Registries.STRUCTURE, Wildercord.id("dungeon"));
	public static final StructurePieceType CLOCKWORK_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("clockwork_crypt"),(StructurePieceType.ContextlessType)ClockworkCryptPiece::new);
	public static final StructurePieceType GREENHOUSE_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("living_greenhouse"),(StructurePieceType.ContextlessType)LivingGreenhousePiece::new);
	public static final StructurePieceType SKY_RUIN_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("moving_sky_ruin"),(StructurePieceType.ContextlessType)MovingSkyRuinPiece::new);
	public static final ResourceKey<LootTable> CLOCK_HALL=loot("chests/clockwork_crypt_hall"),CLOCK_VAULT=loot("chests/clockwork_crypt_vault");
	public static final ResourceKey<LootTable> GARDEN_HALL=loot("chests/living_greenhouse_hall"),GARDEN_VAULT=loot("chests/living_greenhouse_vault");
	public static final ResourceKey<LootTable> SKY_HALL=loot("chests/moving_sky_ruin_hall"),SKY_VAULT=loot("chests/moving_sky_ruin_vault");

	public static final ResourceKey<LootTable> EMBER_HALL = loot("chests/ember_sanctum_hall");
	public static final ResourceKey<LootTable> EMBER_VAULT = loot("chests/ember_sanctum_vault");
	public static final ResourceKey<LootTable> ASTRAL_HALL = loot("chests/astral_observatory_hall");
	public static final ResourceKey<LootTable> ASTRAL_VAULT = loot("chests/astral_observatory_vault");
	public static final ResourceKey<LootTable> TIDE_HALL = loot("chests/drowned_scriptorium_hall");
	public static final ResourceKey<LootTable> TIDE_VAULT = loot("chests/drowned_scriptorium_vault");
	public static final ResourceKey<LootTable> ROOT_HALL = loot("chests/rootbound_maze_hall");
	public static final ResourceKey<LootTable> ROOT_VAULT = loot("chests/rootbound_maze_vault");
	public static final ResourceKey<LootTable> STORM_HALL = loot("chests/storm_spire_hall");
	public static final ResourceKey<LootTable> STORM_VAULT = loot("chests/storm_spire_vault");

	private static ResourceKey<LootTable> loot(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Wildercord.id(path));
	}

	public static void init() {}
	public static final StructurePieceType BATTLEFIELD_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("old_battlefield"),
		(StructurePieceType.ContextlessType)OldBattlefieldPiece::new);
	public static final ResourceKey<LootTable> BATTLEFIELD_SUPPLIES=loot("chests/old_battlefield");
	public static final StructurePieceType SWORD_TOMB_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("sword_tomb"),
		(StructurePieceType.ContextlessType)SwordTombPiece::new);
	public static final ResourceKey<LootTable> TOMB_HALL=loot("chests/sword_tomb_hall");
	public static final StructurePieceType SLEEPING_BLADE_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("sleeping_blade"),
		(StructurePieceType.ContextlessType)SleepingBladePiece::new);
	public static final StructurePieceType SERPENT_GROVE_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("serpent_grove"),
		(StructurePieceType.ContextlessType)SerpentGrovePiece::new);
	public static final ResourceKey<LootTable> GROVE_SUPPLIES=loot("chests/serpent_grove");
	public static final StructurePieceType BELL_CHAPEL_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("bell_chapel"),
		(StructurePieceType.ContextlessType)BellChapelPiece::new);
	public static final ResourceKey<LootTable> BELL_CHAPEL=loot("chests/bell_chapel");
	public static final StructurePieceType ASHEN_GLADE_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("ashen_glade"),
		(StructurePieceType.ContextlessType)AshenGladePiece::new);
	public static final ResourceKey<LootTable> ASHEN_GLADE=loot("chests/ashen_glade");
	public static final StructurePieceType SILK_HOLLOW_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("silk_hollow"),
		(StructurePieceType.ContextlessType)SilkHollowPiece::new);
	public static final ResourceKey<LootTable> SILK_HOLLOW=loot("chests/silk_hollow");
	public static final StructurePieceType GLACIER_DEN_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("glacier_den"),
		(StructurePieceType.ContextlessType)GlacierDenPiece::new);
	public static final ResourceKey<LootTable> GLACIER_DEN=loot("chests/glacier_den");
	public static final StructurePieceType BARROW_FIELD_PIECE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Wildercord.id("barrow_field"),
		(StructurePieceType.ContextlessType)BarrowFieldPiece::new);
	public static final ResourceKey<LootTable> BARROW_FIELD=loot("chests/barrow_field");
}
