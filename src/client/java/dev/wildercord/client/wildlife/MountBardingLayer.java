package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Barding on a Wildercord mount (0.13 mount bonds): the model again, a touch larger about its body, with a texture holding
 * only the plates in greys, tinted to the horse armor it wears (a leather one to its dye).
 */
public final class MountBardingLayer<M extends EntityModel<WildlifeRenderState>> extends RenderLayer<WildlifeRenderState, M> {
	/** How much larger the barding is drawn than the body, so its plates sit over the coat. */
	private static final float GROW = 1.04F;

	private final Identifier texture;
	/** Where the body's middle is, in blocks down from the model's origin, so it grows about there. */
	private final float middle;

	public MountBardingLayer(RenderLayerParent<WildlifeRenderState, M> parent, String name, float middlePixels) {
		super(parent);
		this.texture = WildlifeRenderers.texture(name + "_barding");
		this.middle = middlePixels / 16F;
	}

	/** The tint for a horse armor, or 0 when it wears none. */
	public static int colour(ItemStack armor) {
		if (armor.isEmpty()) {
			return 0;
		}
		DyedItemColor dyed = armor.get(DataComponents.DYED_COLOR);
		if (dyed != null) {
			return dyed.rgb();
		}
		String id = BuiltInRegistries.ITEM.getKey(armor.getItem()).getPath();
		if (id.startsWith("leather")) return 0xA06540;
		if (id.startsWith("copper")) return 0xD08A5A;
		if (id.startsWith("iron")) return 0xD4D8DC;
		if (id.startsWith("golden")) return 0xF2CE52;
		if (id.startsWith("diamond")) return 0x74E2DA;
		if (id.startsWith("netherite")) return 0x5A5258;
		return 0xC8C8C8;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WildlifeRenderState state, float yRot, float xRot) {
		if (state.barding == 0 || state.isInvisible) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0, middle, 0);
		poseStack.scale(GROW, GROW, GROW);
		poseStack.translate(0, -middle, 0);
		collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.entityCutout(texture), light,
			LivingEntityRenderer.getOverlayCoords(state, 0.0F), 0xFF000000 | state.barding, null, state.outlineColor);
		poseStack.popPose();
	}
}
