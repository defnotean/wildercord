package dev.wildercord.client.wildlife;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client render state for {@link dev.wildercord.wildlife.ShrineCat} (`Yuki`, `Odie`, `Freya`, `Kuro`). */
public class ShrineCatRenderState extends LivingEntityRenderState {
	public int variant;
	public int role;
	public boolean sitting;
	public boolean loafing;
	public boolean tamed;
	public int actionTick;
	public int earFlick;
}
