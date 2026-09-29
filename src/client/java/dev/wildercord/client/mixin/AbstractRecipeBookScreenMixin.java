package dev.wildercord.client.mixin;

import dev.wildercord.gear.GearLayout;
import dev.wildercord.gear.GearSlot;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The gear slots' tray sits above the survival inventory's panel, and a click outside the panel drops what
 * the cursor carries. A click on the tray is inside the window, as far as that goes.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void wildercord$trayIsInside(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof InventoryScreen && GearLayout.onTray(mx - xo, my - yo, GearSlot.all().size())) {
			cir.setReturnValue(false);
		}
	}
}
