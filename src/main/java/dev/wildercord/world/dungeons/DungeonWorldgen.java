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

	/** Every dimension dungeon (not the Archive): inside one, a third of the monsters carry Cords. */
	public static final TagKey<Structure> DUNGEONS = TagKey.create(Registries.STRUCTURE, Wildercord.id("dungeon"));

	public static final ResourceKey<LootTable> EMBER_HALL = loot("chests/ember_sanctum_hall");
	public static final ResourceKey<LootTable> EMBER_VAULT = loot("chests/ember_sanctum_vault");
	public static final ResourceKey<LootTable> ASTRAL_HALL = loot("chests/astral_observatory_hall");
	public static final ResourceKey<LootTable> ASTRAL_VAULT = loot("chests/astral_observatory_vault");

	private static ResourceKey<LootTable> loot(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Wildercord.id(path));
	}

	public static void init() {}
}
