package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.ArchivePlacementProbe;
import dev.wildercord.world.ArchiveStructure;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ArchiveStructure.class, remap = false)
public abstract class ArchiveTerrainProbeMixin {
	@WrapOperation(method = "findGenerationPoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getFirstOccupiedHeight(IILnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/levelgen/RandomState;)I"), require = 1, expect = 1, allow = 1)
	private int wildercord$height(ChunkGenerator generator, int x, int z, Heightmap.Types map, LevelHeightAccessor height, RandomState random, Operation<Integer> original) {
		int result = original.call(generator, x, z, map, height, random);
		ArchivePlacementProbe.terrain(x, z, map, result);
		return result;
	}
}
