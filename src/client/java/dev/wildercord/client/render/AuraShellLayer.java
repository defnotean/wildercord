package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraShellMaterial;
import dev.wildercord.client.AuraClient;
import dev.wildercord.client.combat.ArticulatedAuraShellRenderer;
import dev.wildercord.client.combat.ArticulatedModelAccess;
import dev.wildercord.client.compat.ShaderCompat;
import dev.wildercord.client.fx.MagicQuality;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;

import java.util.ArrayList;
import java.util.List;

/**
 * What the top stages of aura look like on a player, for everyone:
 * <ul>
 * <li>aura armour's shell (Form, with aura enough): the body drawn again a little larger, in a faint, slowly shimmering skin of
 * the aura's colour, flaring for a moment where a blow strikes it unless the viewer enables Reduced flash;</li>
 * <li>Aura Step's afterimages: the player as they were at points along the dash, in their aura's colour, fading behind them.</li>
 * </ul>
 * Both are vanilla's glowing-eyes and emissive translucent types, so a shader pack draws them as it draws its own glowing
 * things, and neither is in a pack's shadows (see {@link ShaderCompat}).
 */
public class AuraShellLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final ModelLayerLocation SHELL = new ModelLayerLocation(Wildercord.id("aura_shell"), "main");
	public static final ModelLayerLocation SLIM_SHELL = new ModelLayerLocation(Wildercord.id("aura_shell"), "slim");
	/** The shell's light, laid out on the skin of each model (the slim one's arms are a pixel narrower). */
	private static final RenderType SHELL_TYPE = RenderTypes.eyes(Wildercord.id("textures/entity/aura/shell.png"));
	private static final RenderType SLIM_SHELL_TYPE = RenderTypes.eyes(Wildercord.id("textures/entity/aura/shell_slim.png"));
	/** How far the shell stands off the body, in pixels. */
	private static final float STAND_OFF = 0.55F;

	/** A player's shell this frame: its colour and how bright (ARGB), absent when there's none. */
	public static final RenderStateDataKey<Integer> SHELL_GLOW = RenderStateDataKey.create(() -> "wildercord:aura_shell");
	/** A player's afterimages this frame, absent when there are none. */
	public static final RenderStateDataKey<List<Image>> IMAGES = RenderStateDataKey.create(() -> "wildercord:aura_afterimages");

	/** One afterimage to draw: where it stands relative to the player now, which way it faces, and its colour (ARGB). */
	public record Image(double dx, double dy, double dz, float yaw, int color) {}

	private final ShellModel shell;
	private final ShellModel slimShell;
	private final ArticulatedAuraShellRenderer articulated;

	public AuraShellLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		this.shell = new ShellModel(context.bakeLayer(SHELL), false);
		this.slimShell = new ShellModel(context.bakeLayer(SLIM_SHELL), true);
		PlayerModel body = getParentModel();
		ArticulatedAuraShellRenderer adapter = null;
		if (getClass() == AuraShellLayer.class && body.getClass() == PlayerModel.class
			&& body instanceof ArticulatedModelAccess owner && owner.wildercord$bodyOwned()) {
			boolean slim = owner.wildercord$rig().slim();
			try {
				adapter = new ArticulatedAuraShellRenderer(body, context.bakeLayer(slim ? SLIM_SHELL : SHELL), slim, slim ? SLIM_SHELL_TYPE : SHELL_TYPE);
			} catch (IllegalArgumentException unsupportedGeometry) {
				// Unknown geometry retains the original shell and complete body fallback.
			}
		}
		articulated = adapter;
	}

	public ArticulatedAuraShellRenderer articulated() { return articulated; }

	public static LayerDefinition createShell() {
		return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(STAND_OFF), false), 64, 64);
	}

	public static LayerDefinition createSlimShell() {
		return LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(STAND_OFF), true), 64, 64);
	}

	/** The player's model again, a little larger, with the skin's second layer left off (the shell is one skin, not two). */
	private static final class ShellModel extends PlayerModel {
		ShellModel(net.minecraft.client.model.geom.ModelPart root, boolean slim) {
			super(root, slim);
		}

		@Override
		public void setupAnim(AvatarRenderState state) {
			super.setupAnim(state);
			hat.visible = false;
			jacket.visible = false;
			leftSleeve.visible = false;
			rightSleeve.visible = false;
			leftPants.visible = false;
			rightPants.visible = false;
		}
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || ShaderCompat.shadowPass()) {
			return;
		}
		Integer glow = state.getData(SHELL_GLOW);
		if (glow != null && (articulated == null || !articulated.submitWorld(state, pose, nodes))) {
			boolean slim = state.skin != null && state.skin.model() == PlayerModelType.SLIM;
			nodes.order(1).submitModel(slim ? slimShell : shell, state, pose, slim ? SLIM_SHELL_TYPE : SHELL_TYPE, LightCoordsUtil.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, glow, null, 0);
		}
		List<Image> images = state.getData(IMAGES);
		if (images != null && !images.isEmpty() && upright(state)) {
			RenderType ghost = RenderTypes.entityTranslucentEmissive(state.skin.body().texturePath());
			for (Image image : images) {
				pose.pushPose();
				// Back out of the body's own turn and the model's flip to the player's feet, level with the world...
				pose.translate(0.0F, 1.501F, 0.0F);
				pose.scale(1 / 0.9375F, 1 / 0.9375F, 1 / 0.9375F);
				pose.scale(-1.0F, -1.0F, 1.0F);
				pose.rotateDegrees(Axis.YP, -(180.0F - state.bodyRot));
				// ...over to where the afterimage stands, turned its own way, and into the model's space again.
				pose.translate(image.dx(), image.dy(), image.dz());
				pose.rotateDegrees(Axis.YP, 180.0F - image.yaw());
				pose.scale(-1.0F, -1.0F, 1.0F);
				pose.scale(0.9375F, 0.9375F, 0.9375F);
				pose.translate(0.0F, -1.501F, 0.0F);
				// The body as it was, ghostly in the aura's colour, and its outline lit as the shell's is.
				int alpha = image.color() >>> 24;
				int ghostColor = (Math.round(alpha * 0.55F) << 24) | (image.color() & 0xFFFFFF);
				nodes.order(2).submitModel(getParentModel(), state, pose, ghost, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ghostColor, null, 0);
				boolean slim = state.skin != null && state.skin.model() == PlayerModelType.SLIM;
				nodes.order(3).submitModel(getParentModel(), state, pose, slim ? SLIM_SHELL_TYPE : SHELL_TYPE, LightCoordsUtil.FULL_BRIGHT,
					OverlayTexture.NO_OVERLAY, image.color(), null, 0);
				pose.popPose();
			}
		}
	}

	/** Afterimages are only drawn standing (not swimming, gliding, sleeping or in a spin), where the body's turn is all there is. */
	private static boolean upright(AvatarRenderState state) {
		return !state.isAutoSpinAttack && !state.isFallFlying && !state.hasPose(Pose.SWIMMING) && !state.hasPose(Pose.SLEEPING) && state.deathTime <= 0
			&& !state.isUpsideDown;
	}

	// ------------------------------------------------------------------ extraction

	/** Carries a player's shell and afterimages into their render state as it's extracted. */
	public static void extract(Avatar avatar, AvatarRenderState state, float partial) {
		state.setData(SHELL_GLOW, null);
		state.setData(IMAGES, null);
		if (!(avatar instanceof Player player) || player.isInvisible() || player.isSpectator()) {
			return;
		}
		AuraAttachments.Look look = Aura.look(player);
		AuraPresence.Look presence = AuraPresence.look(player);
		float time = player.level().getGameTime() + partial;
		if (presence.shell() && look.stage() > 0) {
			// Both shell backends receive the same material, including the viewer's Reduced flash choice.
			float since = presence.shellStruckAt() < 0 ? 99 : time - presence.shellStruckAt();
			state.setData(SHELL_GLOW, AuraShellMaterial.argb(look.color(), Mth.sin(time * 0.09F + player.getId()), since,
				MagicQuality.reducedFlash));
		}
		List<AuraClient.Afterimage> images = AuraClient.afterimages(player.getId(), time);
		if (!images.isEmpty()) {
			List<Image> out = new ArrayList<>(images.size());
			for (AuraClient.Afterimage image : images) {
				float age = (time - image.born()) / Math.max(1, image.life());
				if (age < 0 || age >= 1) {
					continue;
				}
				// A lingering afterimage (one that strikes, the Way of the Shadowstep's) holds its light, then fades as an ordinary one does.
				float tail = (float) AuraClient.AFTERIMAGE_TICKS / Math.max(1, image.life());
				float fade = image.life() > AuraClient.AFTERIMAGE_TICKS ? Math.min(1.0F, (1 - age) / tail) : 1 - age;
				float alpha = (image.life() > AuraClient.AFTERIMAGE_TICKS ? 0.75F : 0.9F) * (float) Math.pow(Math.max(0, fade), 1.4);
				int rgb = mix(image.color(), 0xFFFFFF, 0.1F);
				out.add(new Image(image.at().x - state.x, image.at().y - state.y, image.at().z - state.z, image.yaw(),
					(Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | rgb));
			}
			if (!out.isEmpty()) {
				state.setData(IMAGES, out);
			}
		}
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}
}
