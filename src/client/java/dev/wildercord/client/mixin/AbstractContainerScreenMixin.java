package dev.wildercord.client.mixin;

import dev.wildercord.client.WildercordKeys;
import dev.wildercord.menu.BackpackSlot;
import dev.wildercord.menu.GearInventorySlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Hovering an empty gear slot or the Backpack slot says what it takes, since there is no item to name it. And
 * in the inventory the backpack key opens the worn backpack, as it does in the world.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Shadow
	protected Slot hoveredSlot;

	@Shadow
	@Final
	protected AbstractContainerMenu menu;

	/** At every return, not just the last: the game's method returns early when the slot is empty, just the case this is for. */
	@Inject(method = "extractTooltip", at = @At("RETURN"))
	private void wildercord$emptyGearSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		Slot slot = this.hoveredSlot;
		if (slot == null || slot.hasItem() || !this.menu.getCarried().isEmpty()) {
			return;
		}
		List<Component> lines;
		if (slot.container instanceof GearInventorySlot.GearContainer gear) {
			lines = List.of(Component.translatable(gear.kind().nameKey()), Component.translatable(gear.kind().hintKey()).withStyle(ChatFormatting.GRAY));
		} else if (slot.container instanceof BackpackSlot.WornContainer) {
			lines = List.of(Component.translatable("gear_slot.wildercord.backpack"),
				Component.translatable("gear_slot.wildercord.backpack.hint", WildercordKeys.backpackKey()).withStyle(ChatFormatting.GRAY));
		} else {
			return;
		}
		graphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines, Optional.empty(), mouseX, mouseY);
	}

	/**
	 * Reached only when nothing on the screen took the key (a recipe book search being typed in takes every
	 * key), where the inventory key would close it.
	 */
	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;checkHotbarKeyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"),
		cancellable = true)
	private void wildercord$backpackKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		Object self = this;
		if ((self instanceof InventoryScreen || self instanceof CreativeModeInventoryScreen) && WildercordKeys.backpackMapping().matches(event)) {
			WildercordKeys.openBackpack();
			cir.setReturnValue(true);
		}
	}
}
