package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.OceanItems;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
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
import net.minecraft.util.Unit;

/**
 * Renders 3D physical attachments on players wearing the Abyssal Diving Suit:
 * - Helm: front riveted bronze frame, 4x4 glowing cyan viewport, and grated ear-ports.
 * - Bathysphere Chestplate: twin vertical 3x8x2 brass oxygen tanks, valve bridge, and cyan pressure gauge.
 */
public final class AbyssalDivingSuitLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final ModelLayerLocation HELM_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_helm_3d"), "main");
	public static final ModelLayerLocation TANKS_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_tanks_3d"), "main");

	private static final Identifier TEXTURE = Wildercord.id("textures/entity/ocean/abyssal_diving_3d.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/ocean/abyssal_diving_3d_glow.png");
	private static final RenderType CUTOUT = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType EYES = RenderTypes.eyes(GLOW);

	private final SuitPartModel helmModel;
	private final SuitPartModel tanksModel;

	public AbyssalDivingSuitLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context ctx) {
		super(parent);
		this.helmModel = new SuitPartModel(ctx.bakeLayer(HELM_LAYER));
		this.tanksModel = new SuitPartModel(ctx.bakeLayer(TANKS_LAYER));
	}

	public static LayerDefinition createHelmLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("porthole",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0F, -7.0F, -5.0F, 6, 6, 1)
				.texOffs(14, 0).addBox(-2.0F, -6.0F, -6.2F, 4, 4, 2)
				.texOffs(26, 0).addBox(4.0F, -6.0F, -2.0F, 2, 4, 4)
				.texOffs(26, 0).mirror().addBox(-6.0F, -6.0F, -2.0F, 2, 4, 4),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static LayerDefinition createTanksLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("tanks",
			CubeListBuilder.create()
				.texOffs(0, 16).addBox(-3.5F, 1.5F, 2.1F, 3, 8, 2)
				.texOffs(0, 16).mirror().addBox(0.5F, 1.5F, 2.1F, 3, 8, 2)
				.texOffs(10, 16).addBox(-3.0F, 0.5F, 2.1F, 6, 2, 2)
				.texOffs(26, 16).addBox(-1.0F, 3.0F, -2.6F, 2, 2, 1),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
		if (state.isInvisible) return;

		if (state.headEquipment.is(OceanItems.ABYSSAL_DIVING_HELM)) {
			pose.pushPose();
			getParentModel().head.translateAndRotate(pose);
			RenderCompat.submitModel(collector.order(0), helmModel, Unit.INSTANCE, pose, CUTOUT, light, OverlayTexture.NO_OVERLAY, -1, state.outlineColor);
			RenderCompat.submitModel(collector.order(1), helmModel, Unit.INSTANCE, pose, EYES, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
			pose.popPose();
		}

		if (state.chestEquipment.is(OceanItems.BATHYSPHERE_CHESTPLATE)) {
			pose.pushPose();
			getParentModel().body.translateAndRotate(pose);
			RenderCompat.submitModel(collector.order(0), tanksModel, Unit.INSTANCE, pose, CUTOUT, light, OverlayTexture.NO_OVERLAY, -1, state.outlineColor);
			RenderCompat.submitModel(collector.order(1), tanksModel, Unit.INSTANCE, pose, EYES, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
			pose.popPose();
		}
	}

	private static final class SuitPartModel extends Model<Unit> {
		SuitPartModel(ModelPart root) {
			super(root, RenderTypes::entityCutout);
		}
	}
}
