package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.monster.ManaOoze;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Mana Ooze (see {@link ManaOozeModel}): sized and squished like a slime, its inside and outside both drawn through
 * vanilla's translucent entity pass (as a slime's outer jelly is), and its core and mote glowing through the glowing-eyes
 * pass, brighter the fuller it is and flaring each time it drinks a spell.
 */
public class ManaOozeRenderer extends AbstractCubeMobRenderer<ManaOoze, ManaOozeModel.State, ManaOozeModel> {
	public static final ModelLayerLocation INNER = new ModelLayerLocation(Wildercord.id("mana_ooze"), "main");
	public static final ModelLayerLocation OUTER = new ModelLayerLocation(Wildercord.id("mana_ooze"), "outer");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/mana_ooze.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/mana_ooze_glow.png");

	public ManaOozeRenderer(EntityRendererProvider.Context context) {
		super(context, new ManaOozeModel(context.bakeLayer(INNER)));
		ManaOozeModel outer = new ManaOozeModel(context.bakeLayer(OUTER));
		// The heart glows: brighter the fuller it is, flaring as it drinks.
		addLayer(new RenderLayer<>(this) {
			@Override
			public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, ManaOozeModel.State state, float yRot, float xRot) {
				if (state.isInvisible) {
					return;
				}
				float strength = Mth.clamp(0.45F + 0.4F * state.fill + 0.5F * state.flash, 0, 1) * (state.deathTime > 0 ? 0.4F : 1);
				int argb = ((int) (strength * 255) << 24) | 0xFFFFFF;
				collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, argb, null,
					state.outlineColor);
			}
		});
		// The clear outer jelly, last, over everything inside it.
		addLayer(new RenderLayer<>(this) {
			@Override
			public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, ManaOozeModel.State state, float yRot, float xRot) {
				if (state.isInvisible) {
					return;
				}
				outer.setupAnim(state);
				collector.order(2).submitModel(outer, state, poseStack, RenderTypes.entityTranslucent(TEXTURE), light,
					LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
			}
		});
	}

	@Override
	public Identifier getTextureLocation(ManaOozeModel.State state) {
		return TEXTURE;
	}

	@Override
	public ManaOozeModel.State createRenderState() {
		return new ManaOozeModel.State();
	}

	@Override
	public void extractRenderState(ManaOoze ooze, ManaOozeModel.State state, float partial) {
		super.extractRenderState(ooze, state, partial);
		state.fill = ooze.fill();
		long since = ooze.level().getGameTime() - ooze.drankAt();
		state.flash = since >= 0 && since < 10 ? 1 - (since + partial) / 10.0F : 0;
	}

	@Override
	protected void scale(ManaOozeModel.State state, PoseStack poseStack) {
		poseStack.scale(0.999F, 0.999F, 0.999F);
		poseStack.translate(0.0F, 0.001F, 0.0F);
		super.scale(state, poseStack);
	}
}
