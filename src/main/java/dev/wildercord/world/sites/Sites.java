package dev.wildercord.world.sites;

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

import java.util.*;

/**
 * The explorable sites' shared registrations. Each pack registers its own pieces and locators from its
 * {@code init()}; where each site may appear and how often live in data written by its {@code tools/sites_*.py}.
 */
public final class Sites {
	private Sites() {}

	/** Finds where one site starts in a chunk, or nothing. Pure in the context, like every structure's. */
	@FunctionalInterface
	public interface Locator {
		Optional<Structure.GenerationStub> locate(Structure.GenerationContext context);
	}

	public static final StructureType<SiteStructure> SITE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, Wildercord.id("site"),
		() -> SiteStructure.CODEC);
	/** Every explorable site, for "you are inside a site" checks. */
	public static final TagKey<Structure> ALL_SITES = TagKey.create(Registries.STRUCTURE, Wildercord.id("site"));

	private static final Map<String, Locator> LOCATORS = new LinkedHashMap<>();

	public static void register(String site, Locator locator) {
		if (LOCATORS.putIfAbsent(site, locator) != null) throw new IllegalStateException("Duplicate site " + site);
	}

	public static Locator locator(String site) {
		return LOCATORS.get(site);
	}

	public static Set<String> ids() {
		return Collections.unmodifiableSet(LOCATORS.keySet());
	}

	public static StructurePieceType piece(String id, StructurePieceType.ContextlessType type) {
		return Registry.register(BuiltInRegistries.STRUCTURE_PIECE, Wildercord.id(id), type);
	}

	public static ResourceKey<LootTable> loot(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Wildercord.id(path));
	}

	public static void init() {
		// Each pack registers its sites here in its own block.
		// ---- sites-farm pack
		dev.wildercord.world.sites.farm.FarmSites.init();
		// ---- sites-masters pack
		dev.wildercord.world.sites.masters.MasterSites.init();
		// ---- sites-wilds pack
		dev.wildercord.world.sites.wilds.WildsSites.init();
		// ---- sites-travel pack
		dev.wildercord.world.sites.travel.TravelSites.init();
		// ---- sites-water pack
		dev.wildercord.world.sites.water.WaterSites.init();
		// ---- sites-mine pack
		dev.wildercord.world.sites.mine.MineSites.init();
	}
}
