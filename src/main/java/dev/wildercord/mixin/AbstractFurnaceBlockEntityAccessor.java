package dev.wildercord.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A time spell on a furnace, smoker or blast furnace jumps its smelting ahead (and burns its fuel as far). */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
	@Accessor("cookingProgress")
	int wildercord$cookingTimer();

	@Accessor("cookingProgress")
	void wildercord$setCookingTimer(int ticks);

	@Accessor("cookingTotalTime")
	int wildercord$cookingTotalTime();

	@Accessor("litTime")
	int wildercord$litTimeRemaining();

	@Accessor("litTime")
	void wildercord$setLitTimeRemaining(int ticks);
}
