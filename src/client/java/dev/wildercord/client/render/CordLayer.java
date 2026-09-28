package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.CastingPose;
import dev.wildercord.player.WildercordAttachments;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;

/**
 * The Cord on a player's wrist, seen by everyone: a band of cord in its tier's material, and a
 * glowing bead for each rune of the spell they have ready, in the rune's colour. The beads burn
 * brighter while the player charges, and flare for a moment after a cast.
 */
public class CordLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final ModelLayerLocation BAND = new ModelLayerLocation(Wildercord.id("cord"), "band");
	public static final ModelLayerLocation BEAD = new ModelLayerLocation(Wildercord.id("cord"), "bead");
	private static final Identifier BEAD_TEXTURE = Wildercord.id("textures/entity/cord/bead.png");

	/** Where the beads sit on the band, in the right arm's space (x, y, z in pixels). */
	private static final float[][] SPOTS = {
		{-1.0F, 8.05F, -2.45F}, {0.55F, 8.05F, -2.45F}, {-2.55F, 8.05F, -2.45F}, {-3.45F, 8.05F, -0.9F},
		{-3.45F, 8.05F, 0.9F}, {-1.0F, 8.05F, 2.45F}, {0.55F, 8.05F, 2.45F}, {-2.55F, 8.05F, 2.45F},
	};

	private final CordModel band;
	private final CordModel bead;

	public CordLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		this.band = CordModel.band(context.bakeLayer(BAND));
		this.bead = CordModel.bead(context.bakeLayer(BEAD));
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float yRot, float xRot) {
		WildercordAttachments.CordLook cord = ((CastingPose) state).wildercord$cord();
		if (cord.tier().isEmpty() || state.isInvisible) {
			return;
		}
		pose.pushPose();
		getParentModel().rightArm.translateAndRotate(pose);
		Identifier texture = Wildercord.id("textures/entity/cord/" + cord.tier() + ".png");
		nodes.submitModel(band, Unit.INSTANCE, pose, RenderTypes.entityCutout(texture), light, OverlayTexture.NO_OVERLAY, -1);
		float glow = ((CastingPose) state).wildercord$glow();
		// A slow pulse, so the beads look alive.
		float pulse = 0.85F + 0.15F * Mth.sin(state.ageInTicks * 0.12F);
		int alpha = Mth.clamp(Math.round(255 * Math.min(1, 0.55F * glow * pulse)), 0, 255);
		for (int i = 0; i < cord.beads().size() && i < SPOTS.length; i++) {
			float[] spot = SPOTS[i];
			pose.pushPose();
			pose.translate(spot[0] / 16F, spot[1] / 16F, spot[2] / 16F);
			int color = (alpha << 24) | (cord.beads().get(i) & 0xFFFFFF);
			nodes.submitModel(bead, Unit.INSTANCE, pose, RenderTypes.entityCutout(BEAD_TEXTURE), light, OverlayTexture.NO_OVERLAY, 0xFF000000 | cord.beads().get(i));
			nodes.submitModel(bead, Unit.INSTANCE, pose, RenderTypes.eyes(BEAD_TEXTURE), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
			pose.popPose();
		}
		pose.popPose();
	}
}
