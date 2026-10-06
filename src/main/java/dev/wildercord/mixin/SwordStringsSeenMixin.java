package dev.wildercord.mixin;

import dev.wildercord.aura.SwordStrings;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sword strings, what the server sees: every swing a client makes (at a creature, a block or nothing) ends with a punch, which
 * the server notes against the player (see {@link SwordStrings#swung}), so a string the client says was played can be checked
 * against swings that really happened, and everyone else draws its trail ({@code AuraFx.swung}). (A spear's thrust sends no
 * punch: see {@link PiercingWeaponStringsMixin}.)
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SwordStringsSeenMixin {
	@Shadow
	public ServerPlayer player;

	/** Only a swing actually accepted by vanilla releases a held form; a rejected/repeated animation packet cannot. */
	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "handlePunch", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerPlayer;swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z"))
	private boolean wildercord$formSwing(ServerPlayer actor, net.minecraft.world.InteractionHand hand,
			net.minecraft.world.item.component.SwingAnimation animation, boolean sendToSource,
			com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
		boolean accepted = original.call(actor, hand, animation, sendToSource);
		if (accepted && actor.isAlive() && !actor.isSpectator()) dev.wildercord.aura.MasterForms.cancel(actor);
		return accepted;
	}

	@Inject(method = "handlePunch", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
		shift = At.Shift.AFTER))
	private void wildercord$swingSeen(ServerboundPunchPacket packet, CallbackInfo ci) {
		if (!this.player.isSpectator()) {
			SwordStrings.swung(this.player);
			// Everyone else sees the swing's trail, if the blade has aura enough to coat a blow.
			dev.wildercord.aura.AuraFx.swung(this.player, false);
			// A swing at a standard of the swordsman's crossroads strikes it (leaning toward that Way, or walking it).
			dev.wildercord.aura.Crossroads.swung(this.player);
		}
	}
}
