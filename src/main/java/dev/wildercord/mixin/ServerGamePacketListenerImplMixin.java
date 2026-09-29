package dev.wildercord.mixin;

import dev.wildercord.menu.PlacedSlot;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The creative inventory edits slots client-side and reports them with a packet the server
 * only accepts for vanilla slots 1-45. Without this, a Cord placed in creative would show on
 * the client but never reach the server. Routes the Cord slot (46) and the gear slots after it through
 * the same checks.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(
		method = "handleSetCreativeModeSlot",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
			shift = At.Shift.AFTER
		),
		cancellable = true
	)
	private void wildercord$routeExtraSlots(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
		int slotNum = packet.slotNum();
		AbstractContainerMenu menu = this.player.inventoryMenu;
		if (slotNum < 0 || slotNum >= menu.slots.size() || !(menu.getSlot(slotNum) instanceof PlacedSlot)) {
			return;
		}
		Slot slot = menu.getSlot(slotNum);
		ci.cancel();
		if (!this.player.hasInfiniteMaterials()) {
			return;
		}
		ItemStack stack = packet.itemStack();
		if (!stack.isItemEnabled(this.player.level().enabledFeatures()) || (!stack.isEmpty() && !slot.mayPlace(stack))) {
			return;
		}
		// Mirrors vanilla's own path for slots 1-45.
		slot.setByPlayer(stack.copyWithCount(Math.min(stack.getCount(), 1)));
		menu.setRemoteSlot(slotNum, slot.getItem());
		menu.broadcastChanges();
	}
}
