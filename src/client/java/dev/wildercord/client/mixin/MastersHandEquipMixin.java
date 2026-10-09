package dev.wildercord.client.mixin;

import dev.wildercord.aura.MastersViewMotion;
import dev.wildercord.client.MastersHandMotionState;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe actual equip provenance without altering vanilla heights, tickers, or item selection. */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class MastersHandEquipMixin {
	@Shadow private ItemStack mainHandItem;
	@Shadow private float mainHandHeight;
	@Shadow private float oMainHandHeight;
	@Unique private final MastersViewMotion.EquipTransition wildercord$equip = new MastersViewMotion.EquipTransition();
	@Unique private LocalPlayer wildercord$owner;

	// Vanilla has already performed instant compatible replacements (including ordinary damage
	// updates) at this point. A remaining identity mismatch really starts its lowering animation.
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
	private void wildercord$trackEquip(LocalPlayer player, CallbackInfo ci) {
		if (wildercord$owner != player) { wildercord$owner = player; wildercord$equip.begin(); }
		wildercord$equip.tick(mainHandItem != player.getMainHandItem(), player.isHandsBusy(), oMainHandHeight, mainHandHeight);
	}

	@Inject(method = "itemUsed", at = @At("HEAD"))
	private void wildercord$itemUsed(InteractionHand hand, CallbackInfo ci) {
		if (hand == InteractionHand.MAIN_HAND) wildercord$equip.begin();
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void wildercord$extractEquip(LocalPlayer player, float partial, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		((MastersHandMotionState) state).wildercord$mainHandEquipping(wildercord$equip.active()
			|| mainHandItem != player.getMainHandItem() || player.isHandsBusy());
	}
}
