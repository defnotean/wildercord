package dev.wildercord.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 1.21.8 has no {@code InteractionHand.asEquipmentSlot()}: the hand maps to the main-hand or off-hand
 * slot and nothing else. Mixed into the enum so call sites read the same on every version.
 */
public interface InteractionHandCompat {
	default EquipmentSlot asEquipmentSlot() {
		return (InteractionHand) (Object) this == InteractionHand.MAIN_HAND
			? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
	}
}
