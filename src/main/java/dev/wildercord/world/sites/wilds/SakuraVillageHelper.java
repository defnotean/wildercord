package dev.wildercord.world.sites.wilds;

import dev.wildercord.wildlife.SakuraVillager;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatContent;
import dev.wildercord.wildlife.ShrineCatRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Shared placement helpers used across all four Sakura Village archetypes so residents,
 * Shrine Cats, and non-waterlogged tabletop vessels follow one consistent contract.
 */
final class SakuraVillageHelper {
	private SakuraVillageHelper() {}

	static BlockState dryTeaCups(int count) {
		return Blocks.SEA_PICKLE.defaultBlockState()
			.setValue(SeaPickleBlock.PICKLES, Math.max(1, Math.min(4, count)))
			.setValue(SeaPickleBlock.WATERLOGGED, false);
	}

	static void spawnVillager(WorldGenLevel level, BoundingBox bb, BlockPos pos, int role) {
		if (!bb.isInside(pos)) {
			return;
		}
		SakuraVillager villager = ShrineCatContent.SAKURA_VILLAGER.create(level.getLevel(), MobSpawnType.STRUCTURE);
		if (villager == null) {
			return;
		}
		villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		villager.configure(role);
		level.addFreshEntityWithPassengers(villager);
	}

	static void spawnCat(WorldGenLevel level, BoundingBox bb, BlockPos pos, ShrineCatRules.Variant variant) {
		if (!bb.isInside(pos)) {
			return;
		}
		ShrineCat cat = ShrineCatContent.SHRINE_CAT.create(level.getLevel(), MobSpawnType.STRUCTURE);
		if (cat == null) {
			return;
		}
		cat.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		cat.configureVillage(variant, pos);
		level.addFreshEntityWithPassengers(cat);
	}
}
