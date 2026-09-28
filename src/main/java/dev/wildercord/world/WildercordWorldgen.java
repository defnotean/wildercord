package dev.wildercord.world;

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
 * The Archive's registrations. Where it may appear and how often live in data:
 * {@code data/wildercord/worldgen/structure/archive.json} and {@code structure_set/archives.json}.
 */
public final class WildercordWorldgen {
	private WildercordWorldgen() {}

	public static final StructureType<ArchiveStructure> ARCHIVE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Wildercord.id("archive"),
		() -> ArchiveStructure.CODEC);

	public static final StructurePieceType ARCHIVE_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id("archive"),
		(StructurePieceType.ContextlessType) ArchivePiece::new);

	public static final TagKey<Structure> ARCHIVES = TagKey.create(Registries.STRUCTURE, Wildercord.id("archive"));

	public static final ResourceKey<LootTable> LIBRARY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Wildercord.id("chests/archive_library"));
	public static final ResourceKey<LootTable> VAULT_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Wildercord.id("chests/archive_vault"));

	public static void init() {}
}
