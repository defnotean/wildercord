package dev.wildercord.client.mixin;

import dev.wildercord.client.render.RuneMarksLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries a Runebound's rune marks into its render state, where {@link RuneMarksLayer} draws them.
 * A render layer only ever sees the state, never the entity, and there is no event for this step.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin implements dev.wildercord.client.combat.ArticulatedRendererAccess {
	@org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
	protected java.util.List<net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?>> layers;
	@org.spongepowered.asm.mixin.Shadow
	protected net.minecraft.client.model.EntityModel<?> model;
	@Override public java.util.List<net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?>> wildercord$layers() { return layers; }
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$runeMarks(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
		RuneMarksLayer.extract(entity, state, partial);
		boolean known = (!(state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState)
			|| model.getClass() == net.minecraft.client.model.player.PlayerModel.class
				&& model instanceof dev.wildercord.client.combat.ArticulatedModelAccess owner && owner.wildercord$bodyOwned()) && layers.stream().allMatch(dev.wildercord.client.combat.ArticulatedCombat::knownLayer);
		state.setData(dev.wildercord.client.combat.ArticulatedCombat.KNOWN_LAYERS, known);
		state.setData(dev.wildercord.client.combat.ArticulatedArmorRenderer.READY,
			model instanceof dev.wildercord.client.combat.ArticulatedModelAccess access && access.wildercord$bodyOwned() && access.wildercord$armor() != null && access.wildercord$armor().supportsAssets()
				&& layers.stream().filter(layer -> layer.getClass() == dev.wildercord.client.combat.ArticulatedArmorLayer.class).count() == 1
				&& layers.stream().noneMatch(layer -> layer instanceof net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<?, ?, ?>));
	}
}
