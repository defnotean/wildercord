package dev.wildercord.client.compat;

import java.util.function.Function;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 1.21.1's {@code Model} is rootless and posed by hand, so this stand-in accepts the newer
 * (root, renderType) construction, keeps the root for the geometry, and renders nothing: the
 * armor rendering is inert in 1.21.1. Geometry subclasses keep their own 1.21.4-style rendering.
 */
public abstract class McModel<T> extends Model {
	protected McModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
		super(renderType);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light, int overlayCoords, int frame) {}
}
