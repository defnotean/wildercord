package net.minecraft.client.renderer.state.level;
public class FirstPersonHandsAndItemsRenderState implements dev.wildercord.client.MastersHandMotionState {
	public net.minecraft.client.renderer.item.ItemStackRenderState mainHandRenderState = new net.minecraft.client.renderer.item.ItemStackRenderState();
 public boolean isScoping, hasMainHandMapData, equipping;
	public boolean wildercord$mainHandEquipping() { return equipping; }
	public void wildercord$mainHandEquipping(boolean value) { equipping = value; }
}
