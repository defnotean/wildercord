package dev.wildercord.client.mixin;

import dev.wildercord.client.SlotWell;
import dev.wildercord.menu.CordSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Brings the Cord slot into the creative Survival Inventory tab. The creative menu wraps
 * every inventory slot with its own layout formula, which would drop the Cord slot on the
 * hotbar, so the wrapper is moved to the free spot right of the armour, mirroring the offhand.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
	@Shadow
	public abstract boolean isInventoryOpen();

	private CreativeModeInventoryScreenMixin() {
		super(null, null, null);
	}

	@ModifyArgs(
		method = "selectTab",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"
		)
	)
	private void wildercord$placeCordSlot(Args args) {
		if (args.get(0) instanceof CordSlot) {
			args.set(2, CordSlot.CREATIVE_X);
			args.set(3, CordSlot.CREATIVE_Y);
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void wildercord$drawCordWell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		if (this.isInventoryOpen()) {
			SlotWell.draw(graphics, this.leftPos + CordSlot.CREATIVE_X - 1, this.topPos + CordSlot.CREATIVE_Y - 1);
		}
	}
}
