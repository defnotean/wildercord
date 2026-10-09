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
		// The base renderer uses this actual attribute scale once for both the body and its shadow.
		// Do not multiply the pose stack again: the collision box already uses the same attribute.
		state.scale = dog.getScale();
		state.sitting = dog.isOrderedToSit();
		int mood = dog.mood();
		state.sleeping = (mood & CinnamonDog.SLEEPING) != 0;
		state.playing = (mood & CinnamonDog.PLAYING) != 0;
		state.greeting = (mood & CinnamonDog.GREETING) != 0;
		state.ringing = (mood & CinnamonDog.RINGING) != 0;
		state.tongue = (mood & CinnamonDog.TONGUE) != 0;
		state.wearingBow = (mood & CinnamonDog.BOW) != 0;
		state.exhausted = (mood & CinnamonDog.EXHAUSTED) != 0;
	}
}
