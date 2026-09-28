package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.Archivist;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.state.EvokerRenderState;
import net.minecraft.resources.Identifier;

/** The Archivist: the illager shape, a head taller, in a deep robe with gold trim and glowing eyes. */
public class ArchivistRenderer extends EvokerRenderer<Archivist> {
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/archivist.png");
	private static final float SCALE = 1.25F;

	public ArchivistRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.6F;
	}

	@Override
	public Identifier getTextureLocation(EvokerRenderState state) {
		return TEXTURE;
	}

	@Override
	protected void scale(EvokerRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
