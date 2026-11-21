package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.monster.ManaOoze;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Mana Ooze (see {@link ManaOozeModel}): sized and squished like a slime, its inside and outside both drawn through
 * vanilla's translucent entity pass (as a slime's outer jelly is), and its core and mote glowing through the glowing-eyes
 * pass, brighter the fuller it is and flaring each time it drinks a spell.
 */
public class ManaOozeRenderer extends MobRenderer<ManaOoze, ManaOozeModel.State, ManaOozeModel> {
	public static final ModelLayerLocation INNER = new ModelLayerLocation(Wildercord.id("mana_ooze"), "main");
	public static final ModelLayerLocation OUTER = new ModelLayerLocation(Wildercord.id("mana_ooze"), "outer");
	private static final ResourceLocation TEXTURE = Wildercord.id("textures/entity/mana_ooze.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/mana_ooze_glow.png");

	public ManaOozeRenderer(EntityRendererProvider.Context context) {
		// 26.1.2 has no AbstractCubeMobRenderer: a plain MobRenderer with the slime shadow radius.
		super(context, new ManaOozeModel(context.bakeLayer(INNER)), 0.5F);
		ManaOozeModel outer = new ManaOozeModel(context.bakeLayer(OUTER));
		// The heart glows: brighter the fuller it is, flaring as it drinks.
		addLayer(new RenderLayer<>(this) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource buffer, int light, ManaOozeModel.State state, float yRot, float xRot) {
				if (state.isInvisible) {
					return;
				}
				float strength = Mth.clamp(0.7F + 0.3F * state.fill + 0.5F * state.flash, 0, 1) * (state.deathTime > 0 ? 0.4F : 1);
				int argb = ((int) (strength * 255) << 24) | 0xFFFFFF;
				dev.wildercord.client.compat.RenderCompat.submitModel(buffer, getParentModel(), state, poseStack, RenderType.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, argb, dev.wildercord.client.compat.RenderCompat.outlineOf(state));
			}
		});
		// The clear outer jelly, last, over everything inside it.
		addLayer(new RenderLayer<>(this) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource buffer, int light, ManaOozeModel.State state, float yRot, float xRot) {
				if (state.isInvisible) {
					return;
				}
				outer.setupAnim(state);
				dev.wildercord.client.compat.RenderCompat.submitModel(buffer, outer, state, poseStack, RenderType.entityTranslucent(TEXTURE), light, LivingEntityRenderer.getOverlayCoords(state, 0.0F), dev.wildercord.client.compat.RenderCompat.outlineOf(state));
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(ManaOozeModel.State state) {
		return TEXTURE;
	}

	@Override
	public ManaOozeModel.State createRenderState() {
		return new ManaOozeModel.State();
	}

	@Override
	public void extractRenderState(ManaOoze ooze, ManaOozeModel.State state, float partial) {
		super.extractRenderState(ooze, state, partial);
		// The cube-mob superclass is gone on 26.1.2: carry the slime's squash and size by hand.
		state.squish = Mth.lerp(partial, ooze.oSquish, ooze.squish);
		state.size = ooze.getSize();
		state.fill = ooze.fill();
		long since = ooze.level().getGameTime() - ooze.drankAt();
		state.flash = since >= 0 && since < 10 ? 1 - (since + partial) / 10.0F : 0;
	}

	@Override
	protected void scale(ManaOozeModel.State state, PoseStack poseStack) {
		// Mirrors 26.1.2 SlimeRenderer#scale: near-full-size to dodge z-fighting, then the squish stretch.
		poseStack.scale(0.999F, 0.999F, 0.999F);
		poseStack.translate(0.0F, 0.001F, 0.0F);
		float size = (float) state.size;
		float squish = state.squish * size * 0.5F + 1.0F;
		float stretch = 1.0F / (squish + 1.0F);
		poseStack.scale(stretch * size, size / stretch, stretch * size);
	}
}
