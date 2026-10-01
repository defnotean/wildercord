package dev.wildercord.client.pet;

import dev.wildercord.Wildercord;
import dev.wildercord.pet.CinnamonDog;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class CinnamonRenderer extends MobRenderer<CinnamonDog, CinnamonRenderState, CinnamonModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cinnamon"), "main");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/cinnamon.png");
	private static final Identifier SLEEPING = Wildercord.id("textures/entity/cinnamon_sleeping.png");

	public CinnamonRenderer(EntityRendererProvider.Context context) {
		super(context, new CinnamonModel(context.bakeLayer(LAYER)), 0.28F);
	}

	@Override public Identifier getTextureLocation(CinnamonRenderState state) { return state.sleeping ? SLEEPING : TEXTURE; }
	@Override public CinnamonRenderState createRenderState() { return new CinnamonRenderState(); }
	@Override public void extractRenderState(CinnamonDog dog, CinnamonRenderState state, float partial) {
		super.extractRenderState(dog, state, partial);
		state.sitting = dog.isOrderedToSit();
		state.sleeping = (dog.mood() & 1) != 0;
		state.playing = (dog.mood() & 2) != 0;
		state.greeting = (dog.mood() & 4) != 0;
	}
}
