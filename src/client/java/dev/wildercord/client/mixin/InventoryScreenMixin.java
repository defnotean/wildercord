package dev.wildercord.client.mixin;

import dev.wildercord.client.SlotWell;
import dev.wildercord.menu.CordSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Cord slot's well above the offhand slot in the survival inventory. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
	private InventoryScreenMixin() {
		super(null, null, null);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void wildercord$drawCordWell(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		SlotWell.draw(graphics, this.leftPos + CordSlot.INVENTORY_X - 1, this.topPos + CordSlot.INVENTORY_Y - 1);
	}
}
