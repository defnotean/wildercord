package dev.wildercord.mixin;

import dev.wildercord.compat.LivingEntityCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Gives {@code LivingEntity} the table-naming {@code dropFromLootTable} overloads. */
@Mixin(LivingEntity.class)
public interface LivingEntityCompatMixin extends LivingEntityCompat {
}
