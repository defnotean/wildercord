package dev.wildercord.client.mixin;

import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.SwordStringsClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sword strings read from the player's own swings, where vanilla makes them: as an attack begins (what the player was doing:
 * how full the swing was, crouching, in the air, sprinting, what's under the crosshair) and once the swing has really gone, its
 * packets sent (the punch every swing ends with, or a spear's thrust). An attack that doesn't swing (still recovering from a miss,
 * hands busy, a spectator) never reaches the second. See {@link SwordStringsClient}; the same two moments draw the swing's blade
 * trail ({@link AuraFxClient}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftStringsMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void wildercord$strokeBegins(CallbackInfoReturnable<Boolean> cir) {
		if (dev.wildercord.client.AuraSocialClient.press()) {
			cir.setReturnValue(false);
			return;
		}
		// Aura's trail first: it reads the cue marks the string reader is about to use up.
		AuraFxClient.attackBegins((Minecraft) (Object) this);
		SwordStringsClient.attackBegins((Minecraft) (Object) this);
	}

	@Inject(method = "startAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", shift = At.Shift.AFTER))
	private void wildercord$stroke(CallbackInfoReturnable<Boolean> cir) {
		SwordStringsClient.swung((Minecraft) (Object) this, false);
		AuraFxClient.swung((Minecraft) (Object) this, false);
	}

	@Inject(method = "startAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;piercingAttack(Lnet/minecraft/world/item/component/SwingAnimation;Lnet/minecraft/world/item/component/PiercingWeapon;)V",
		shift = At.Shift.AFTER))
	private void wildercord$thrust(CallbackInfoReturnable<Boolean> cir) {
		SwordStringsClient.swung((Minecraft) (Object) this, true);
		AuraFxClient.swung((Minecraft) (Object) this, true);
	}
}
