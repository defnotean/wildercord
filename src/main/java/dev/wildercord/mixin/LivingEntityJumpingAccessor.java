package dev.wildercord.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.5 ships setJumping but no isJumping: the wave's ridden-input code reads the field back. */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpingAccessor {
	@Accessor("jumping")
	boolean wildercord$isJumping();
}
