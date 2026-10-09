package dev.wildercord.gametest.mixin;

import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import java.util.function.Supplier;

@Mixin(value=ChunkGenerator.class,remap=false)
public interface WetlandFeatureIndexAccessor {
 @Accessor("featuresPerStep") Supplier<List<FeatureSorter.StepFeatureData>> wildercord$featuresPerStep();
}
