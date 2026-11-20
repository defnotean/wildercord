package dev.wildercord.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the protected behaviour goal selector, which 26.1.2 keeps field-only. */
@Mixin(Mob.class)
public interface MobGoalSelectorMixin {
	@Accessor("goalSelector")
	GoalSelector wildercord$goalSelector();
}
