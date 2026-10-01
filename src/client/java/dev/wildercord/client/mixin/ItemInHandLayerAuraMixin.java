package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person: while a hand's item is drawn, the blade's aura (see {@link AuraBlade}) knows whose and which hand it is. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerAuraMixin {
	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	private void wildercord$auraBegin(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm, PoseStack pose,
			SubmitNodeCollector collector, int light, CallbackInfo ci) {
		AuraBlade.beginThirdPerson(state, arm, stack);
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm, PoseStack pose,
			SubmitNodeCollector collector, int light, CallbackInfo ci) {
		AuraBlade.end();
	}
}
