package dev.wildercord.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.21.2: the 1.21.3+ display setters are private here; the mod drives them through this accessor. */
@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setTransformation")
	void wildercord$setTransformation(Transformation transformation);
	@Invoker("setTransformationInterpolationDelay")
	void wildercord$setTransformationInterpolationDelay(int ticks);
	@Invoker("setTransformationInterpolationDuration")
	void wildercord$setTransformationInterpolationDuration(int ticks);
	@Invoker("setPosRotInterpolationDuration")
	void wildercord$setPosRotInterpolationDuration(int ticks);
	@Invoker("setBillboardConstraints")
	void wildercord$setBillboardConstraints(Display.BillboardConstraints constraints);
	@Invoker("setGlowColorOverride")
	void wildercord$setGlowColorOverride(int color);
}