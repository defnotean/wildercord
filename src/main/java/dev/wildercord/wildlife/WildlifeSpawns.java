package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.wildlife.WildlifeRules.Kind;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Where and how often wildlife spawns. Each creature joins its biomes' spawn lists through Fabric's biome
 * modifications (read once as the world loads: its weight, grown by a multiplier above 1), and its spawn rule decides
 * every try as it happens (read live, so {@code /wildercord reload} takes effect at once): the creatures switches, its
 * own rarity and the multiplier below 1, the ground, the light, and how many of its kind are already near. Only
 * natural spawns are held to those; spawn eggs, {@code /summon} and spawners aren't.
 *
 * <p>The two ambient fliers spawn where vanilla's ambient spawning lands: anywhere in a column, mostly underground,
 * where bats live. Their rules only accept open air near the surface (glimmerwings at night, in reach of the ground;
 * skyrays on an open summit, to climb from), so few tries are theirs, and they share the ambient cap with the bats
 * rather than adding to how many creatures can be about.</p>
 */
public final class WildlifeSpawns {
	private WildlifeSpawns() {}

	/** What each creature may stand on to spawn (written by tools/wildlife_art.py, so a data pack can change them). */
	public static final TagKey<Block> STAG_GROUND = ground("lumen_stag");
	public static final TagKey<Block> TORTOISE_GROUND = ground("mossback_tortoise");
	public static final TagKey<Block> CINDERFOX_GROUND = ground("cinderfox");
	public static final TagKey<Block> HARE_GROUND = ground("rimehare");

	/** How far above the ground (blocks) a glimmerwing may spawn. */
	private static final int GLIMMER_REACH = 6;
	/** How far above the surface a skyray may spawn. */
	private static final int SKYRAY_REACH = 4;

	private static TagKey<Block> ground(String creature) {
		return TagKey.create(Registries.BLOCK, Wildercord.id("spawns_on/" + creature));
	}

	static void init() {
		SpawnPlacements.register(Wildlife.GLIMMERWING, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> glimmerwing(type, level, reason, pos, random));
		SpawnPlacements.register(Wildlife.LUMEN_STAG, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> creature(WildlifeRules.LUMEN_STAG, type, STAG_GROUND, level, reason, pos, random));
		SpawnPlacements.register(Wildlife.MOSSBACK_TORTOISE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> creature(WildlifeRules.MOSSBACK_TORTOISE, type, TORTOISE_GROUND, level, reason, pos, random));
		SpawnPlacements.register(Wildlife.CINDERFOX, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> creature(WildlifeRules.CINDERFOX, type, CINDERFOX_GROUND, level, reason, pos, random));
		SpawnPlacements.register(Wildlife.SKYRAY, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
			(type, level, reason, pos, random) -> skyray(type, level, reason, pos, random));
		SpawnPlacements.register(Wildlife.RIMEHARE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(type, level, reason, pos, random) -> creature(WildlifeRules.RIMEHARE, type, HARE_GROUND, level, reason, pos, random));

		join(WildlifeRules.GLIMMERWING, Wildlife.GLIMMERWING);
		join(WildlifeRules.LUMEN_STAG, Wildlife.LUMEN_STAG);
		join(WildlifeRules.MOSSBACK_TORTOISE, Wildlife.MOSSBACK_TORTOISE);
		join(WildlifeRules.CINDERFOX, Wildlife.CINDERFOX);
		join(WildlifeRules.SKYRAY, Wildlife.SKYRAY);
		join(WildlifeRules.RIMEHARE, Wildlife.RIMEHARE);
	}

	/** The biome keys a kind lives in. */
	public static Set<ResourceKey<Biome>> biomes(Kind kind) {
		return kind.biomes().stream().map(id -> ResourceKey.create(Registries.BIOME, Identifier.parse(id))).collect(Collectors.toUnmodifiableSet());
	}

	public static MobCategory category(Kind kind) {
		return kind.pool() == WildlifeRules.Pool.AMBIENT ? MobCategory.AMBIENT : MobCategory.CREATURE;
	}

	/** Adds a kind to its biomes' spawn lists, unless it's switched off as the world loads (then it isn't in them at all). */
	static void join(Kind kind, EntityType<?> type) {
		BiomeModifications.create(Wildercord.id("wildlife/" + kind.id())).add(ModificationPhase.ADDITIONS, BiomeSelectors.includeByKey(biomes(kind)),
			context -> {
				WildercordConfig.WildlifeSettings settings = Config.get().wildlife();
				if (settings.spawns(kind.id())) {
					context.getMobSpawnSettings().addSpawn(category(kind), new MobSpawnSettings.SpawnerData(type, UniformInt.of(kind.minGroup(), kind.maxGroup())),
						WildlifeRules.weight(kind, settings.spawnMultiplier()));
				}
			});
	}

	/** Whether a spawn is the world's own doing (and so held to the config, rarity and crowding). */
	private static boolean natural(EntitySpawnReason reason) {
		return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
	}

	/** The config, the kind's rarity and its crowding, for a natural spawn. */
	private static boolean allowed(Kind kind, EntityType<?> type, ServerLevelAccessor level, BlockPos pos, RandomSource random) {
		WildercordConfig.WildlifeSettings settings = Config.get().wildlife();
		if (!settings.spawns(kind.id()) || random.nextDouble() >= WildlifeRules.chance(kind, settings.spawnMultiplier())) {
			return false;
		}
		int near = level.getEntities(type, new AABB(pos).inflate(kind.crowdRange()), e -> true).size();
		return WildlifeRules.roomFor(kind, near);
	}

	static <T extends Mob> boolean creature(Kind kind, EntityType<T> type, TagKey<Block> ground, ServerLevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (!level.getBlockState(pos.below()).is(ground)) {
			return false;
		}
		boolean lit = EntitySpawnReason.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8;
		return lit && (!natural(reason) || allowed(kind, type, level, pos, random));
	}

	/** In open air at night, within reach of the ground, under the sky (a forest's leaves let enough through). */
	private static boolean glimmerwing(EntityType<Glimmerwing> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		if (!natural(reason)) {
			return true;
		}
		if (!level.getLevel().isDarkOutside() || !level.getBlockState(pos).isAir() || !level.getFluidState(pos.below()).isEmpty()) {
			return false;
		}
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
		int above = pos.getY() - ground;
		if (above < 0 || above > GLIMMER_REACH || level.getBrightness(LightLayer.SKY, pos) < 9) {
			return false;
		}
		return allowed(WildlifeRules.GLIMMERWING, type, level, pos, random);
	}

	/** On an open summit or ridge, from which it climbs to its cruise. */
	private static boolean skyray(EntityType<Skyray> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (!natural(reason)) {
			return true;
		}
		if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos.below()).isEmpty()) {
			return false;
		}
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		int above = pos.getY() - surface;
		if (above < 0 || above > SKYRAY_REACH || level.getBrightness(LightLayer.SKY, pos) < 15) {
			return false;
		}
		return allowed(WildlifeRules.SKYRAY, type, level, pos, random);
	}
}
