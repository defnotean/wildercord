package dev.wildercord.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1.21.1 has no {@code InteractionHand.asEquipmentSlot()}: a hand is the main-hand or off-hand slot
 * and nothing else. A static because a mixin interface is invisible to the compiler.
 */
public final class McHands {
	private McHands() {}

	public static EquipmentSlot slot(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}

	/** 1.21.1 has no move-intent accessor; the aura treats no intent as a stopped swordsman. */
	public static Vec3 lastClientMoveIntent(net.minecraft.server.level.ServerPlayer player) {
		return Vec3.ZERO;
	}

	/** How long the entity has been using its current item, counting up the way the newer versions do. */
	public static int ticksUsing(LivingEntity entity) {
		ItemStack using = entity.getUseItem();
		if (using.isEmpty()) {
			return 0;
		}
		return Math.max(0, using.getUseDuration(entity) - entity.getUseItemRemainingTicks());
	}
}
