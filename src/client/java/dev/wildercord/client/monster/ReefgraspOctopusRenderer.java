package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.ReefgraspOctopus;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Reefgrasp Octopus (see {@link ReefgraspOctopusModel}), which fades to the dull colour of the seafloor while it hides. */
public class ReefgraspOctopusRenderer extends WildMonsterRenderer<ReefgraspOctopus, ReefgraspOctopusModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("reefgrasp_octopus"), "main");
	/** The sandy grey-brown it takes on hidden, as a tint over its ruddy skin. */
	private static final int SAND = 0xB0A48C;

	public ReefgraspOctopusRenderer(EntityRendererProvider.Context context) {
		super(context, new ReefgraspOctopusModel(context.bakeLayer(LAYER)), 0.7F, Wildercord.id("textures/entity/reefgrasp_octopus.png"));
	}

	@Override
	protected int getModelTint(MonsterRenderState state) {
		int base = state.variantTint;
		int out = 0xFF000000;
		for (int shift = 0; shift <= 16; shift += 8) {
			float channel = (base >> shift & 0xFF) / 255.0F;
			float sand = (SAND >> shift & 0xFF) / 255.0F;
			float c = channel * (1 + (sand - 1) * state.veil);
			out |= Math.round(c * 255) << shift;
		}
		return out;
	}
}
