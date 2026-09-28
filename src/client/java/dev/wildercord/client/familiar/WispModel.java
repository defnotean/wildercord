package dev.wildercord.client.familiar;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * A wisp has no body of boxes: it's light, drawn by {@link WispRenderer} as sprites that face the
 * viewer. This empty model is only there because a mob renderer needs one.
 */
public class WispModel extends EntityModel<WispRenderState> {
	public WispModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
	}

	public static LayerDefinition createLayer() {
		return LayerDefinition.create(new MeshDefinition(), 16, 16);
	}
}
