package dev.wildercord.client.combat;

/** Only AvatarRenderer's primary skin model may own a body rig; armor PlayerModels stay untouched. */
public interface ArticulatedModelAccess {
	void wildercord$ownBody(boolean slim);
	boolean wildercord$bodyOwned();
	ArticulatedRig wildercord$rig();
	ArticulatedViewModel wildercord$viewModel();
	ArticulatedArmorRenderer wildercord$armor();
	void wildercord$setArmor(ArticulatedArmorRenderer armor);
}
