package dev.wildercord.mixin;

import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.SwordStrings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Held-slot changes are events, not just impact-time comparisons: switching away and back never restores a paid windup. */
@Mixin(Inventory.class)
public abstract class AuraSelectionMixin {
	@Shadow @Final public Player player;

	@Inject(method = "setItem", at = @At("HEAD"))
	private void wildercord$contents(int slot, ItemStack stack, CallbackInfo ci) {
		Inventory inventory = (Inventory) (Object) this;
		if (slot == inventory.selected && inventory.getItem(slot) != stack) wildercord$equipmentChanged();
	}

	@Unique
	private void wildercord$equipmentChanged() {
		if (player instanceof ServerPlayer server) {
			MastersArts.cancel(server);
			dev.wildercord.aura.MasterForms.cancel(server);
			SwordStrings.weaponChanged(server);
		}
	}
}
