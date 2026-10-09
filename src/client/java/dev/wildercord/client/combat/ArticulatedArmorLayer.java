package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Exactly one slot in the original layer list: either the captured vanilla layer or our adapter. */
public final class ArticulatedArmorLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final RenderLayer<AvatarRenderState, PlayerModel> fallback;
	private final ArticulatedArmorRenderer armor;

	private ArticulatedArmorLayer(AvatarRenderer<?> renderer, RenderLayer<AvatarRenderState, PlayerModel> fallback,
			ArticulatedArmorRenderer armor) {
		super(renderer);
		this.fallback = fallback;
		this.armor = armor;
	}

	/** Called once after AvatarRenderer construction; resource reload creates a fresh complete owner. */
	@SuppressWarnings("unchecked")
	public static void install(AvatarRenderer<?> renderer, EntityRendererProvider.Context context, boolean slim) {
		PlayerModel model = renderer.getModel();
		if (model.getClass() != PlayerModel.class || !(model instanceof ArticulatedModelAccess access)) return;
		access.wildercord$ownBody(slim);
		if (!(renderer instanceof ArticulatedRendererAccess rendererAccess)) return;
		var layers = rendererAccess.wildercord$layers();
		if (layers.stream().anyMatch(layer -> layer instanceof ArticulatedArmorLayer)) return;
		var armorLayers = layers.stream().filter(layer -> layer instanceof HumanoidArmorLayer<?, ?, ?>).toList();
		if (armorLayers.size() != 1 || armorLayers.getFirst().getClass() != HumanoidArmorLayer.class) return;
		var original = armorLayers.getFirst();
		ArticulatedArmorRenderer armor;
		try {
			armor = new ArticulatedArmorRenderer(context.getModelSet(), slim, context.getEquipmentRenderer(), context.getEquipmentAssets());
		} catch (IllegalArgumentException unsupportedGeometry) {
			// A geometry-changing mod/resource must keep the complete original layer and body gate.
			return;
		}
		layers.set(layers.indexOf(original), new ArticulatedArmorLayer(renderer,
			(RenderLayer<AvatarRenderState, PlayerModel>) original, armor));
		access.wildercord$setArmor(armor);
	}

	@Override
	public void submit(PoseStack stack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
		if (!armor.submitWorld(getParentModel(), state, stack, collector, light)) fallback.submit(stack, collector, light, state, yaw, pitch);
	}

	public RenderLayer<AvatarRenderState, PlayerModel> fallback() { return fallback; }
	public ArticulatedArmorRenderer armor() { return armor; }
}
