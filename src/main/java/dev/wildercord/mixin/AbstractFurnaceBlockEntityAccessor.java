package dev.wildercord.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A time spell on a furnace, smoker or blast furnace jumps its smelting ahead (and burns its fuel as far). */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
	@Accessor("cookingTimer")
	int wildercord$cookingTimer();

	@Accessor("cookingTimer")
	void wildercord$setCookingTimer(int ticks);

	@Accessor("cookingTotalTime")
	int wildercord$cookingTotalTime();

	@Accessor("litTimeRemaining")
	int wildercord$litTimeRemaining();

	@Accessor("litTimeRemaining")
	void wildercord$setLitTimeRemaining(int ticks);
}
