package dev.wildercord.mixin;

import dev.wildercord.compat.InteractionHandCompat;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;

/** Gives {@code InteractionHand} its {@code asEquipmentSlot()} shortcut. */
@Mixin(InteractionHand.class)
public interface InteractionHandCompatMixin extends InteractionHandCompat {
}
