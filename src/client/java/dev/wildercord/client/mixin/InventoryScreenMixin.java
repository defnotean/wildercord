package dev.wildercord.client.mixin;

import dev.wildercord.client.GearTray;
import dev.wildercord.client.SlotWell;
import dev.wildercord.menu.CordSlot;
import dev.wildercord.client.compat.GuiGraphicsExtractor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Cord slot's well above the offhand slot in the survival inventory, and the gear slots' tray on the panel's top edge. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
	private InventoryScreenMixin() {
		super(null, null, null);
	}

	@Inject(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", at = @At("TAIL"))
	private void wildercord$drawCordWell(GuiGraphics wcGui, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(wcGui);
		SlotWell.draw(graphics, this.leftPos + CordSlot.INVENTORY_X - 1, this.topPos + CordSlot.INVENTORY_Y - 1);
		GearTray.drawSurvival(graphics, this.leftPos, this.topPos);
	}
}
