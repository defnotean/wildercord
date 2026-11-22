package dev.wildercord.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.21.2: the block display state is private here; the mod reads and writes it through this accessor. */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
	@Invoker("setBlockState")
	void wildercord$setBlockState(BlockState state);
	@Invoker("getBlockState")
	BlockState wildercord$getBlockState();
}