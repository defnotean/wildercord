package dev.wildercord.mixin;

import dev.wildercord.aura.SwordStrings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sword strings, what the server sees: a player's spear thrust is a swing too (it sends no punch; see {@link SwordStringsSeenMixin}). */
@Mixin(PiercingWeapon.class)
public abstract class PiercingWeaponStringsMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void wildercord$thrustSeen(LivingEntity attacker, EquipmentSlot hand, CallbackInfo ci) {
		if (attacker instanceof ServerPlayer player) {
			SwordStrings.swung(player);
		}
	}
}
