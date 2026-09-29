package dev.wildercord.client.mixin;

import dev.wildercord.menu.GearInventorySlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/** Hovering an empty gear slot says what it takes, since there is no item to name it. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	@Inject(method = "extractTooltip", at = @At("TAIL"))
	private void wildercord$emptyGearSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		Slot slot = this.hoveredSlot;
		if (slot == null || slot.hasItem() || !this.menu.getCarried().isEmpty() || !(slot.container instanceof GearInventorySlot.GearContainer gear)) {
			return;
		}
		graphics.setTooltipForNextFrame(Minecraft.getInstance().font, List.of(
			Component.translatable(gear.kind().nameKey()),
			Component.translatable(gear.kind().hintKey()).withStyle(ChatFormatting.GRAY)), Optional.empty(), mouseX, mouseY);
	}
}
