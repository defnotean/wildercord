package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.compat.ShaderCompat;
import dev.wildercord.client.render.AuraShellLayer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.PlayerModelType;

/** One reload-owned shell adapter belonging to exactly one primary player body. */
public final class ArticulatedAuraShellRenderer {
	public static final String ENABLE_PROPERTY = "wildercord.articulated.auraShell";
	public static final RenderStateDataKey<ArticulatedAuraShellRenderer> READY = RenderStateDataKey.create(() -> "wildercord:articulated_shell_owner");
	private final PlayerModel owner;
	private final boolean slim;
	private final RenderType material;
	private final ArticulatedAuraShellGeometry world, view;

	public ArticulatedAuraShellRenderer(PlayerModel owner, ModelPart source, boolean slim, RenderType material) {
		this.owner = owner; this.slim = slim; this.material = material;
		world = new ArticulatedAuraShellGeometry(source, slim, false);
		view = new ArticulatedAuraShellGeometry(source, slim, true);
	}

	public static boolean enabled() { return ArticulatedCombat.enabled() && Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "true")); }

	/** Readiness is extracted from the exact layer list before any body or item owns a frame. */
	public boolean supports(AvatarRenderState state) {
		return enabled() && owner.getClass() == PlayerModel.class && owner instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned()
			&& access.wildercord$rig().slim() == slim && state.skin != null && (state.skin.model() == PlayerModelType.SLIM) == slim;
	}
	public static boolean compatible(AvatarRenderState state) {
		if (state.getData(AuraShellLayer.SHELL_GLOW) == null) return true;
		var adapter = state.getData(READY);
		return adapter != null && adapter.supports(state);
	}

	public boolean submitWorld(AvatarRenderState state, PoseStack stack, SubmitNodeCollector collector) {
		if (state.getData(READY) != this || !supports(state) || !ArticulatedCombat.applyPlayer(owner, state)) return false;
		Integer glow = state.getData(AuraShellLayer.SHELL_GLOW);
		if (glow == null || state.isInvisible || ShaderCompat.shadowPass()) return true;
		ArticulatedRig rig = ((ArticulatedModelAccess) owner).wildercord$rig();
		var palette = ArticulatedArmorGeometry.Palette.capture(rig);
		stack.pushPose();
		try {
			owner.root().translateAndRotate(stack);
			submit(world, palette, glow, stack, collector);
		} finally { stack.popPose(); }
		return true;
	}

	/** Both camera-space arms share the exact final view pose used by the skin, armor and hilt. */
	public boolean submitView(ArticulatedRig rig, AvatarRenderState state, PoseStack stack, SubmitNodeCollector collector) {
		if (state.getData(READY) != this || !supports(state) || ArticulatedCombat.viewFrame(state) == null) return false;
		Integer glow = state.getData(AuraShellLayer.SHELL_GLOW);
		if (glow == null || state.isInvisible || ShaderCompat.shadowPass()) return true;
		submit(view, ArticulatedArmorGeometry.Palette.capture(rig), glow, stack, collector);
		return true;
	}
	private void submit(ArticulatedAuraShellGeometry model, ArticulatedArmorGeometry.Palette palette, int glow,
			PoseStack stack, SubmitNodeCollector collector) {
		// Match AuraShellLayer exactly: eyes texture, order one, live ARGB, fullbright, no overlay
		// and outline zero. A body/team outline must not invent a separate shell outline.
		collector.order(1).submitModel(model, palette, stack, material, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow, null, 0);
	}
	public PlayerModel owner() { return owner; }
	public ArticulatedAuraShellGeometry worldModel() { return world; }
	public ArticulatedAuraShellGeometry viewModel() { return view; }
	public RenderType material() { return material; }
}
