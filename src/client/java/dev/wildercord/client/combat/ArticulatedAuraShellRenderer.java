package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.compat.ShaderCompat;
import dev.wildercord.client.render.AuraShellLayer;
import dev.wildercord.client.compat.RenderStateDataKey;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.PlayerSkin.Model;
import dev.wildercord.client.render.RenderStateData;

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

	public static boolean enabled() { return dev.wildercord.client.CombatPresentation.effective().auraShell(); }

	/** Readiness is extracted from the exact layer list before any body or item owns a frame. */
	public boolean supports(PlayerRenderState state) {
		return enabled() && owner.getClass() == PlayerModel.class && owner instanceof ArticulatedModelAccess access && access.wildercord$bodyOwned()
			&& access.wildercord$rig().slim() == slim && state.skin != null && (state.skin.model() == PlayerSkin.Model.SLIM) == slim;
	}
	public static boolean compatible(PlayerRenderState state) {
		if (RenderStateData.get(state, AuraShellLayer.SHELL_GLOW) == null) return true;
		var adapter = RenderStateData.get(state, READY);
		return adapter != null && adapter.supports(state);
	}

	public boolean submitWorld(PlayerRenderState state, PoseStack stack, MultiBufferSource buffer) {
		if (RenderStateData.get(state, READY) != this || !supports(state) || !ArticulatedCombat.applyPlayer(owner, state)) return false;
		Integer glow = RenderStateData.get(state, AuraShellLayer.SHELL_GLOW);
		if (glow == null || state.isInvisible || ShaderCompat.shadowPass()) return true;
		ArticulatedRig rig = ((ArticulatedModelAccess) owner).wildercord$rig();
		var palette = ArticulatedArmorGeometry.Palette.capture(rig);
		stack.pushPose();
		try {
			owner.root().translateAndRotate(stack);
			submit(world, palette, glow, stack, buffer);
		} finally { stack.popPose(); }
		return true;
	}

	/** Both camera-space arms share the exact final view pose used by the skin, armor and hilt. */
	public boolean submitView(ArticulatedRig rig, PlayerRenderState state, PoseStack stack, MultiBufferSource buffer) {
		if (RenderStateData.get(state, READY) != this || !supports(state) || ArticulatedCombat.viewFrame(state) == null) return false;
		Integer glow = RenderStateData.get(state, AuraShellLayer.SHELL_GLOW);
		if (glow == null || state.isInvisible || ShaderCompat.shadowPass()) return true;
		submit(view, ArticulatedArmorGeometry.Palette.capture(rig), glow, stack, buffer);
		return true;
	}
	private void submit(ArticulatedAuraShellGeometry model, ArticulatedArmorGeometry.Palette palette, int glow,
			PoseStack stack, MultiBufferSource buffer) {
		// Match AuraShellLayer exactly: eyes texture, order one, live ARGB, fullbright, no overlay
		// and outline zero. A body/team outline must not invent a separate shell outline.
		dev.wildercord.client.compat.RenderCompat.submitModel(buffer, model, palette, stack, material, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow, 0);
	}
	public PlayerModel owner() { return owner; }
	public ArticulatedAuraShellGeometry worldModel() { return world; }
	public ArticulatedAuraShellGeometry viewModel() { return view; }
	public RenderType material() { return material; }
}
