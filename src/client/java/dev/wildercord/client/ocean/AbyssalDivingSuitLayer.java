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
 * 1:1 Sub-voxel 3D attachments for players wearing the Abyssal Diving Suit (128x128 sheet, 0.5x scale):
 * - Helm: full brass bathysphere dome, stepped crown cap, riveted front bezel, glowing cyan viewport
 *   glass overlaid with crossed protective cage bars, grated ear-ports, and flared bronze collar.
 * - Bathysphere Chestplate: twin vertical brass O2 cylinders, dual copper clamping straps, upper valve
 *   bridge, curved air hose running up to helm, and emissive cyan pressure gauge.
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
		// Sub-voxel 0.5x around player head [-4..4, -8..0, -4..4] -> [-8..8, -16..0, -8..8]
		root.addOrReplaceChild("porthole",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-9.0F, -17.0F, -9.0F, 18, 18, 18)
				.texOffs(74, 0).addBox(-6.0F, -19.0F, -6.0F, 12, 3, 12)
				.texOffs(0, 38).addBox(-6.5F, -14.5F, -11.5F, 13, 12, 3)
				.texOffs(34, 38).addBox(-4.5F, -12.5F, -12.5F, 9, 8, 2)
				// Protective crossed viewport grille bars
				.texOffs(58, 38).addBox(-0.5F, -13.0F, -13.2F, 1, 9, 2)
				.texOffs(66, 38).addBox(-4.5F, -9.0F, -13.2F, 9, 1, 2)
				// Circular grated side ear-ports
				.texOffs(90, 38).addBox(8.5F, -12.5F, -4.0F, 3, 8, 8)
				.texOffs(90, 38).mirror().addBox(-11.5F, -12.5F, -4.0F, 3, 8, 8)
				// Flared riveted bronze collar gorget
				.texOffs(0, 56).addBox(-10.0F, -1.0F, -9.0F, 20, 3, 18),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}

	public static LayerDefinition createTanksLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Sub-voxel 0.5x around player torso [-4..4, 0..12, -2..2] -> [-8..8, 0..24, -4..4]
		root.addOrReplaceChild("tanks",
			CubeListBuilder.create()
				// Twin back brass O2 cylinders + domed end caps
				.texOffs(0, 80).addBox(-7.0F, 3.0F, 4.2F, 6, 17, 6)
				.texOffs(0, 80).mirror().addBox(1.0F, 3.0F, 4.2F, 6, 17, 6)
				.texOffs(26, 80).addBox(-6.0F, 1.5F, 5.2F, 4, 2, 4)
				.texOffs(26, 80).mirror().addBox(2.0F, 1.5F, 5.2F, 4, 2, 4)
				.texOffs(26, 80).addBox(-6.0F, 19.5F, 5.2F, 4, 2, 4)
				.texOffs(26, 80).mirror().addBox(2.0F, 19.5F, 5.2F, 4, 2, 4)
				// Dual copper clamping straps around cylinders
				.texOffs(44, 80).addBox(-7.5F, 7.0F, 3.7F, 7, 2, 7)
				.texOffs(44, 80).mirror().addBox(0.5F, 7.0F, 3.7F, 7, 2, 7)
				.texOffs(44, 80).addBox(-7.5F, 14.0F, 3.7F, 7, 2, 7)
				.texOffs(44, 80).mirror().addBox(0.5F, 14.0F, 3.7F, 7, 2, 7)
				// Upper valve bridge, vertical air hose rising toward helm, & glowing chest/back gauge
				.texOffs(74, 80).addBox(-7.0F, 0.0F, 5.0F, 14, 3, 4)
				.texOffs(26, 90).addBox(2.5F, -8.0F, 5.5F, 2, 10, 2)
				.texOffs(36, 90).addBox(-2.5F, 5.0F, -5.2F, 5, 5, 2)
				.texOffs(36, 90).addBox(-2.5F, 3.0F, 8.8F, 5, 5, 2),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
		if (state.isInvisible) return;

		if (state.headEquipment.is(OceanItems.ABYSSAL_DIVING_HELM)) {
			pose.pushPose();
			getParentModel().head.translateAndRotate(pose);
			pose.scale(0.5F, 0.5F, 0.5F);
			RenderCompat.submitModel(collector.order(0), helmModel, Unit.INSTANCE, pose, CUTOUT, light, OverlayTexture.NO_OVERLAY, -1, state.outlineColor);
			RenderCompat.submitModel(collector.order(1), helmModel, Unit.INSTANCE, pose, EYES, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
			pose.popPose();
		}

		if (state.chestEquipment.is(OceanItems.BATHYSPHERE_CHESTPLATE)) {
			pose.pushPose();
			getParentModel().body.translateAndRotate(pose);
			pose.scale(0.5F, 0.5F, 0.5F);
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
