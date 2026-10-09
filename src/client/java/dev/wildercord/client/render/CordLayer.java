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
 * bead for each rune of the spell they have ready, in the rune's colour. The beads glow for a moment
 * after the Cord is put on, burn brighter while the player charges, and flare after a cast; the rest
 * of the time they show their material without glowing.
 */
public class CordLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final ModelLayerLocation BAND = new ModelLayerLocation(Wildercord.id("cord"), "band");
	public static final ModelLayerLocation SLIM_BAND = new ModelLayerLocation(Wildercord.id("cord"), "slim_band");
	public static final ModelLayerLocation BEAD = new ModelLayerLocation(Wildercord.id("cord"), "bead");
	private static final Identifier BEAD_TEXTURE = Wildercord.id("textures/entity/cord/bead.png");

	/** Where the beads sit on the band, in the right arm's space (x, y, z in pixels). */
	private static final float[][] SPOTS = {
		{-1.0F, 8.25F, -2.45F}, {0.5F, 8.25F, -2.45F}, {-2.5F, 8.25F, -2.45F}, {-3.45F, 8.25F, -0.9F},
		{-3.45F, 8.25F, 0.9F}, {-1.0F, 8.25F, 2.45F}, {0.5F, 8.25F, 2.45F}, {-2.5F, 8.25F, 2.45F},
	};
	/** The same on a slim arm, which is a pixel narrower on the outside. */
	private static final float[][] SLIM_SPOTS = {
		{-0.5F, 8.25F, -2.45F}, {0.6F, 8.25F, -2.45F}, {-1.6F, 8.25F, -2.45F}, {-2.45F, 8.25F, -0.9F},
		{-2.45F, 8.25F, 0.9F}, {-0.5F, 8.25F, 2.45F}, {0.6F, 8.25F, 2.45F}, {-1.6F, 8.25F, 2.45F},
	};

	private final CordModel band;
	private final CordModel slimBand;
	private final CordModel bead;

	public CordLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		this.band = CordModel.band(context.bakeLayer(BAND));
		this.slimBand = CordModel.band(context.bakeLayer(SLIM_BAND));
		this.bead = CordModel.bead(context.bakeLayer(BEAD));
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float yRot, float xRot) {
		WildercordAttachments.CordLook cord = ((CastingPose) state).wildercord$cord();
		if (cord.tier().isEmpty() || state.isInvisible) {
			return;
		}
		pose.pushPose();
		if (!dev.wildercord.client.combat.ArticulatedCombat.legacyArm(getParentModel(), state, net.minecraft.world.entity.HumanoidArm.RIGHT, pose))
			getParentModel().rightArm.translateAndRotate(pose);
		Identifier texture = Wildercord.id("textures/entity/cord/" + cord.tier() + ".png");
		boolean slim = state.skin != null && state.skin.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
		float[][] spots = slim ? SLIM_SPOTS : SPOTS;
		// The colour goes in as the tint; the outline is the player's own (none unless they're Glowing), so the Cord
		// never carries a glowing outline of its own.
		nodes.submitModel(slim ? slimBand : band, Unit.INSTANCE, pose, RenderTypes.entityCutout(texture), light, OverlayTexture.NO_OVERLAY, -1, null,
			state.outlineColor);
		float glow = ((CastingPose) state).wildercord$glow();
		// The player's chosen style (see CordStyleLook): the beads' material, and a fixed glow colour or the runes' own.
		dev.wildercord.cosmetic.CordStyles.Style style = dev.wildercord.client.cosmetic.CordStyleLook.of(state);
		boolean tinted = dev.wildercord.client.cosmetic.CordStyleLook.tinted(style);
		Identifier beadTexture = dev.wildercord.client.cosmetic.CordStyleLook.beadTexture(style, BEAD_TEXTURE);
		Identifier glowTexture = dev.wildercord.client.cosmetic.CordStyleLook.glowTexture(style, BEAD_TEXTURE);
		// A slow pulse, so the beads look alive.
		float pulse = 0.85F + 0.15F * Mth.sin(state.ageInTicks * 0.12F);
		float strength = dev.wildercord.client.cosmetic.CordStyleLook.glowStrength(style) * (tinted ? 0.55F : 0.8F);
		int alpha = Mth.clamp(Math.round(255 * Math.min(1, strength * glow * pulse)), 0, 255);
		for (int i = 0; i < cord.beads().size() && i < spots.length; i++) {
			float[] spot = spots[i];
			pose.pushPose();
			pose.translate(spot[0] / 16F, spot[1] / 16F, spot[2] / 16F);
			int rgb = dev.wildercord.client.cosmetic.CordStyleLook.glowColor(style, cord.beads().get(i)) & 0xFFFFFF;
			int color = (alpha << 24) | rgb;
			nodes.submitModel(bead, Unit.INSTANCE, pose, RenderTypes.entityCutout(beadTexture), light, OverlayTexture.NO_OVERLAY, tinted ? 0xFF000000 | rgb : -1,
				null, state.outlineColor);
			// The glow is light, not a thing: a shader pack's shadows leave it out.
			if (alpha > 0 && !dev.wildercord.client.compat.ShaderCompat.shadowPass()) {
				nodes.submitModel(bead, Unit.INSTANCE, pose, RenderTypes.eyes(glowTexture), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color, null, 0);
			}
			pose.popPose();
		}
		pose.popPose();
	}
}
