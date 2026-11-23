package dev.wildercord.client.oathhold;

import dev.wildercord.Wildercord;
import dev.wildercord.world.sites.oathhold.OathholdCitizen;
import dev.wildercord.world.sites.oathhold.OathholdCitizenRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders {@link OathholdCitizen} across all 16 medieval capital archetypes. */
public class OathholdCitizenRenderer
		extends MobRenderer<OathholdCitizen, OathholdCitizenRenderState, OathholdCitizenModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("oathhold_citizen"), "main");

	private static final ResourceLocation[] TEXTURES;

	static {
		OathholdCitizenRules.Archetype[] all = OathholdCitizenRules.Archetype.values();
		TEXTURES = new ResourceLocation[all.length];
		for (int i = 0; i < all.length; i++) {
			TEXTURES[i] = Wildercord.id("textures/entity/oathhold_citizen_" + all[i].id + ".png");
		}
	}

	public OathholdCitizenRenderer(EntityRendererProvider.Context context) {
		super(context, new OathholdCitizenModel(context.bakeLayer(LAYER)), 0.45F);
	}

	@Override
	public OathholdCitizenRenderState createRenderState() {
		return new OathholdCitizenRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(OathholdCitizenRenderState state) {
		return TEXTURES[Math.floorMod(state.archetype, TEXTURES.length)];
	}

	@Override
	public void extractRenderState(OathholdCitizen entity, OathholdCitizenRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.archetype = entity.archetype().ordinal();
		state.skinTone = entity.skinTone();
		state.waveTick = entity.waveTick();
	}
}
