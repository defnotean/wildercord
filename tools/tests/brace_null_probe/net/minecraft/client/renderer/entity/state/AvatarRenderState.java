package net.minecraft.client.renderer.entity.state;
import dev.wildercord.client.MastersArtPose;
public class AvatarRenderState {
	public int id; public float ageInTicks;
	public boolean isUsingItem, isAutoSpinAttack, isBaby;
 public net.minecraft.client.renderer.item.ItemStackRenderState itemState = new net.minecraft.client.renderer.item.ItemStackRenderState();
 public net.minecraft.client.renderer.item.ItemStackRenderState getMainHandItemState() { return itemState; }
 public float ticksUsingItem(net.minecraft.world.entity.HumanoidArm arm) { return 0; }
	public net.minecraft.world.entity.LivingEntity.SwingDescription currentSwing;
	public net.minecraft.world.entity.HumanoidArm mainArm = net.minecraft.world.entity.HumanoidArm.RIGHT;
	public MastersArtPose.Frame frame;
	public MastersArtPose.Frame getData(Object key) { return frame; }
	public net.minecraft.world.item.ItemStack item = new net.minecraft.world.item.ItemStack();
	public net.minecraft.world.item.ItemStack getMainHandItemStack() { return item; }
}
