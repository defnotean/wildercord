package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * A mage-hunter: a vindicator's frame in its own skin (tools/magic_art.py), a deep hood over the hat layer and a grey coat
 * stitched with broken runes.
 */
public class MageHunterRenderer extends VindicatorRenderer {
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/mage_hunter.png");

	public MageHunterRenderer(EntityRendererProvider.Context context) {
		super(context);
		// The hood: the illager model's hat layer, which vindicators leave hidden.
		getModel().getHat().visible = true;
	}

	@Override
	public Identifier getTextureLocation(IllagerRenderState state) {
		return TEXTURE;
	}
}
