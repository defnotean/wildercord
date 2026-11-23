package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.BrineMerchant;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class BrineMerchantRenderer extends MobRenderer<BrineMerchant, OceanRenderStates.BrineMerchantState, BrineMerchantModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("brine_merchant"), "main");
	private static final ResourceLocation TEX_JEWELER = Wildercord.id("textures/entity/ocean/brine_merchant_jeweler.png");
	private static final ResourceLocation TEX_CARTOGRAPHER = Wildercord.id("textures/entity/ocean/brine_merchant_cartographer.png");
	private static final ResourceLocation TEX_ALCHEMIST = Wildercord.id("textures/entity/ocean/brine_merchant_alchemist.png");
	private static final ResourceLocation GLOW_JEWELER = Wildercord.id("textures/entity/ocean/brine_merchant_jeweler_glow.png");
	private static final ResourceLocation GLOW_CARTOGRAPHER = Wildercord.id("textures/entity/ocean/brine_merchant_cartographer_glow.png");
	private static final ResourceLocation GLOW_ALCHEMIST = Wildercord.id("textures/entity/ocean/brine_merchant_alchemist_glow.png");

	public BrineMerchantRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new BrineMerchantModel(ctx.bakeLayer(LAYER)), 0.5F);
		addLayer(new PropGlowLayer(this));
	}

	@Override
	public OceanRenderStates.BrineMerchantState createRenderState() {
		return new OceanRenderStates.BrineMerchantState();
	}

	@Override
	public ResourceLocation getTextureLocation(OceanRenderStates.BrineMerchantState state) {
		return switch (state.profession) {
			case PEARL_JEWELER -> TEX_JEWELER;
			case ABYSSAL_CARTOGRAPHER -> TEX_CARTOGRAPHER;
			case KELP_ALCHEMIST -> TEX_ALCHEMIST;
		};
	}

	@Override
	public void extractRenderState(BrineMerchant entity, OceanRenderStates.BrineMerchantState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.profession = entity.getMerchantProfession();
	}

	@Override
	protected void scale(OceanRenderStates.BrineMerchantState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class PropGlowLayer extends RenderLayer<OceanRenderStates.BrineMerchantState, BrineMerchantModel> {
		private final RenderType glowJeweler = RenderTypes.eyes(GLOW_JEWELER);
		private final RenderType glowCartographer = RenderTypes.eyes(GLOW_CARTOGRAPHER);
		private final RenderType glowAlchemist = RenderTypes.eyes(GLOW_ALCHEMIST);

		PropGlowLayer(RenderLayerParent<OceanRenderStates.BrineMerchantState, BrineMerchantModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, OceanRenderStates.BrineMerchantState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderType type = switch (state.profession) {
				case PEARL_JEWELER -> glowJeweler;
				case ABYSSAL_CARTOGRAPHER -> glowCartographer;
				case KELP_ALCHEMIST -> glowAlchemist;
			};
			RenderCompat.submitModel(collector.order(1), getParentModel(), state, pose, type, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
		}
	}
}
