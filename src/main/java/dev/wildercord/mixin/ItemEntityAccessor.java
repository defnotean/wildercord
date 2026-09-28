package dev.wildercord.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Who dropped an item, even when they aren't about: a Void familiar leaves anyone's drops alone. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
	@Accessor("thrower")
	EntityReference<Entity> wildercord$thrower();
}
