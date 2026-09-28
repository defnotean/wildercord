package dev.wildercord.client.render;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.TrainingDummy;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

public class TrainingDummyRenderer extends LivingEntityRenderer<TrainingDummy, DummyRenderState, DummyModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("training_dummy"), "main");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/training_dummy.png");

	public TrainingDummyRenderer(EntityRendererProvider.Context context) {
		super(context, new DummyModel(context.bakeLayer(LAYER)), 0.45F);
	}

	@Override
	public Identifier getTextureLocation(DummyRenderState state) {
		return TEXTURE;
	}

	@Override
	public DummyRenderState createRenderState() {
		return new DummyRenderState();
	}

	@Override
	public void extractRenderState(TrainingDummy dummy, DummyRenderState state, float partial) {
		super.extractRenderState(dummy, state, partial);
		state.hurt = Math.max(0, dummy.hurtTime - partial);
		// It never falls over or turns red: it's a dummy.
		state.hasRedOverlay = false;
		state.deathTime = 0;
	}

	@Override
	protected boolean shouldShowName(TrainingDummy dummy, double distanceSq) {
		return dummy.hasCustomName() && distanceSq < 24 * 24;
	}
}
