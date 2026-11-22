package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * 1.21.3 stand-in for the 1.21.4 {@code ArmedEntityRenderState}. 1.21.3 has no such class: its
 * {@link HumanoidRenderState} already carries {@code mainArm}, {@code attackTime}, {@code attackArm}
 * and the four worn {@code *Item} stacks, so the only additions here are the arm poses 1.21.4 moved
 * onto this type. Kept so the shared client and gametest source sets compile unchanged; no vanilla
 * 1.21.3 render state extends it, so the pose fields are inert here.
 */
public class ArmedEntityRenderState extends HumanoidRenderState {
	public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
	public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
	public final ItemStackRenderState rightHandItem = new ItemStackRenderState();
	public final ItemStackRenderState leftHandItem = new ItemStackRenderState();
}
