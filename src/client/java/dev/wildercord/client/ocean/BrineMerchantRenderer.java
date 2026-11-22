package dev.wildercord.client.ocean;

import dev.wildercord.Wildercord;
import dev.wildercord.ocean.BrineMerchant;
import dev.wildercord.ocean.OceanRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class BrineMerchantRenderer extends MobRenderer<BrineMerchant, OceanRenderStates.BrineMerchantState, BrineMerchantModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("brine_merchant"), "main");
	private static final Identifier TEX_JEWELER = Wildercord.id("textures/entity/ocean/brine_merchant_jeweler.png");
	private static final Identifier TEX_CARTOGRAPHER = Wildercord.id("textures/entity/ocean/brine_merchant_cartographer.png");
	private static final Identifier TEX_ALCHEMIST = Wildercord.id("textures/entity/ocean/brine_merchant_alchemist.png");

	public BrineMerchantRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new BrineMerchantModel(ctx.bakeLayer(LAYER)), 0.5F);
	}

	@Override
	public OceanRenderStates.BrineMerchantState createRenderState() {
		return new OceanRenderStates.BrineMerchantState();
	}

	@Override
	public Identifier getTextureLocation(OceanRenderStates.BrineMerchantState state) {
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
}
