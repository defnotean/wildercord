package dev.wildercord.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.4 keeps the last-hurt player behind a protected field with no getter; the wave's
 * justice code reads it back when a pardon has to stick. */
@Mixin(LivingEntity.class)
public interface LivingEntityLastHurtAccessor {
	@Accessor("lastHurtByPlayer")
	net.minecraft.world.entity.player.Player wildercord$lastHurtByPlayer();
}
