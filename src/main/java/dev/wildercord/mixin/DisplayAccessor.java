package dev.wildercord.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.2: the 1.21.3+ display setters are private here; the mod drives them through this accessor. */
@Mixin(Display.class)
public interface DisplayAccessor {
	@Accessor("setTransformation")
	void wildercord$setTransformation(Transformation transformation);
	@Accessor("setTransformationInterpolationDelay")
	void wildercord$setTransformationInterpolationDelay(int ticks);
	@Accessor("setTransformationInterpolationDuration")
	void wildercord$setTransformationInterpolationDuration(int ticks);
	@Accessor("setPosRotInterpolationDuration")
	void wildercord$setPosRotInterpolationDuration(int ticks);
	@Accessor("setBillboardConstraints")
	void wildercord$setBillboardConstraints(Display.BillboardConstraints constraints);
	@Accessor("setGlowColorOverride")
	void wildercord$setGlowColorOverride(int color);
}