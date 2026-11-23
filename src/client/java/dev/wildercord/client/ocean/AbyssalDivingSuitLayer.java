package dev.wildercord.client.ocean;

import net.minecraft.client.renderer.LightTexture;
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
import net.minecraft.util.Unit;

/**
 * Full 6-limb 3D Abyssal Diving Suit player render layer (256x256 sheet, 0.5x scale per limb):
 * <ul>
 *   <li>Helmet: bolted brass dome, crown vent, neck gorget, ear-ports, inner glowing HUD goggles, and unobstructed translucent glass viewport.</li>
 *   <li>Chestplate: riveted copper/teal cuirass, waist depth-gauge belt, twin back O2 tanks with manifold + dial, plus articulated pauldrons &amp; gauntlets on both arms.</li>
 *   <li>Greaves &amp; Trench Boots: independent kelp-weave thigh armor + bolted kneepads and weighted lead-soled diving boots on both legs.</li>
 * </ul>
 */
public final class AbyssalDivingSuitLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public static final ModelLayerLocation HELM_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_helm_3d"), "main");
	public static final ModelLayerLocation TANKS_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_tanks_3d"), "main");
	public static final ModelLayerLocation RIGHT_ARM_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_right_arm_3d"), "main");
	public static final ModelLayerLocation LEFT_ARM_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_left_arm_3d"), "main");
	public static final ModelLayerLocation RIGHT_LEG_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_right_leg_3d"), "main");
	public static final ModelLayerLocation LEFT_LEG_LAYER = new ModelLayerLocation(Wildercord.id("abyssal_diving_left_leg_3d"), "main");

	private static final Identifier TEXTURE = Wildercord.id("textures/entity/ocean/abyssal_diving_3d.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/ocean/abyssal_diving_3d_glow.png");
	private static final RenderType CUTOUT = RenderTypes.entityCutout(TEXTURE);
	private static final RenderType TRANSLUCENT = RenderTypes.entityTranslucent(TEXTURE);
	private static final RenderType EYES = RenderTypes.eyes(GLOW);

	private final SuitGlassModel helmModel;
	private final SuitPartModel tanksModel;
	private final SuitPartModel rightArmModel;
	private final SuitPartModel leftArmModel;
	private final LegSuitModel rightLegModel;
	private final LegSuitModel leftLegModel;

	public AbyssalDivingSuitLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		this.helmModel = new SuitGlassModel(context.bakeLayer(HELM_LAYER));
		this.tanksModel = new SuitPartModel(context.bakeLayer(TANKS_LAYER));
		this.rightArmModel = new SuitPartModel(context.bakeLayer(RIGHT_ARM_LAYER));
		this.leftArmModel = new SuitPartModel(context.bakeLayer(LEFT_ARM_LAYER));
		this.rightLegModel = new LegSuitModel(context.bakeLayer(RIGHT_LEG_LAYER));
		this.leftLegModel = new LegSuitModel(context.bakeLayer(LEFT_LEG_LAYER));
	}

	public static LayerDefinition createHelmLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Solid copper/brass shell, gorget, ear-ports, bezel (front window opening alpha=0 in texture) -> added FIRST
		root.addOrReplaceChild("helm_shell",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-10.0F, -20.0F, -10.0F, 20, 20, 20)
				.texOffs(0, 66).addBox(-7.0F, -23.0F, -7.0F, 14, 4, 14)
				.texOffs(222, 66).addBox(-3.0F, -25.5F, -3.0F, 6, 3, 6)
				.texOffs(142, 0).addBox(-11.0F, -2.0F, -11.0F, 22, 4, 22)
				.texOffs(143, 66).addBox(-7.0F, -17.0F, -11.5F, 14, 14, 3)
				.texOffs(178, 66).addBox(9.5F, -14.0F, -3.5F, 3, 7, 7)
				.texOffs(178, 66).mirror().addBox(-12.5F, -14.0F, -3.5F, 3, 7, 7)
				.texOffs(0, 85).addBox(-3.0F, -13.0F, 9.5F, 6, 6, 3),
			PartPose.ZERO);
		// Translucent glass viewport rendered LAST so inner player face & cyan HUD glow shine clearly without crossbars
		root.addOrReplaceChild("helm_glass",
			CubeListBuilder.create()
				.texOffs(199, 66).addBox(-5.0F, -15.0F, -11.8F, 10, 10, 1),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	public static LayerDefinition createTanksLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("chest_cuirass_and_tanks",
			CubeListBuilder.create()
				.texOffs(81, 0).addBox(-9.0F, -0.5F, -6.0F, 18, 20, 12)
				.texOffs(152, 41).addBox(-10.0F, 16.5F, -7.0F, 20, 4, 14)
				.texOffs(106, 85).addBox(-2.5F, 16.0F, -7.8F, 5, 5, 2)
				.texOffs(90, 41).addBox(-7.0F, 2.0F, 6.0F, 6, 16, 6)
				.texOffs(90, 41).mirror().addBox(1.0F, 2.0F, 6.0F, 6, 16, 6)
				.texOffs(19, 85).addBox(-7.0F, 5.5F, 6.5F, 14, 3, 5)
				.texOffs(19, 85).addBox(-7.0F, 13.5F, 6.5F, 14, 3, 5)
				.texOffs(121, 85).addBox(-2.0F, 5.0F, 11.2F, 4, 4, 2),
			PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	public static LayerDefinition createRightArmLayer() {
		return createArmLayer(false);
	}

	public static LayerDefinition createLeftArmLayer() {
		return createArmLayer(true);
	}

	private static LayerDefinition createArmLayer(boolean left) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		float cx = left ? 2.0F : -2.0F;
		CubeListBuilder builder = CubeListBuilder.create();
		if (left) {
			builder.texOffs(57, 66).mirror().addBox(cx - 5.5F, -4.5F, -5.5F, 11, 7, 11)
				.texOffs(115, 41).mirror().addBox(cx - 4.5F, 2.5F, -4.5F, 9, 12, 9)
				.texOffs(102, 66).mirror().addBox(cx - 5.0F, 13.5F, -5.0F, 10, 8, 10);
		} else {
			builder.texOffs(57, 66).addBox(cx - 5.5F, -4.5F, -5.5F, 11, 7, 11)
				.texOffs(115, 41).addBox(cx - 4.5F, 2.5F, -4.5F, 9, 12, 9)
				.texOffs(102, 66).addBox(cx - 5.0F, 13.5F, -5.0F, 10, 8, 10);
		}
		root.addOrReplaceChild("arm_armor", builder, PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	public static LayerDefinition createRightLegLayer() {
		return createLegLayer(false);
	}

	public static LayerDefinition createLeftLegLayer() {
		return createLegLayer(true);
	}

	private static LayerDefinition createLegLayer(boolean mirror) {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeListBuilder greaves = CubeListBuilder.create();
		CubeListBuilder boots = CubeListBuilder.create();
		if (mirror) {
			greaves.texOffs(0, 41).mirror().addBox(-5.0F, -0.5F, -5.0F, 10, 14, 10)
				.texOffs(58, 85).mirror().addBox(-4.0F, 8.5F, -6.5F, 8, 5, 3);
			boots.texOffs(41, 41).mirror().addBox(-5.5F, 14.0F, -7.5F, 11, 10, 13)
				.texOffs(81, 85).mirror().addBox(-4.5F, 19.5F, -9.8F, 9, 4, 3);
		} else {
			greaves.texOffs(0, 41).addBox(-5.0F, -0.5F, -5.0F, 10, 14, 10)
				.texOffs(58, 85).addBox(-4.0F, 8.5F, -6.5F, 8, 5, 3);
			boots.texOffs(41, 41).addBox(-5.5F, 14.0F, -7.5F, 11, 10, 13)
				.texOffs(81, 85).addBox(-4.5F, 19.5F, -9.8F, 9, 4, 3);
		}
		root.addOrReplaceChild("greaves", greaves, PartPose.ZERO);
		root.addOrReplaceChild("boots", boots, PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
		if (state.isInvisible) return;

		if (state.headEquipment.is(OceanItems.ABYSSAL_DIVING_HELM)) {
			submitLimb(pose, collector, light, state, getParentModel().head, helmModel, TRANSLUCENT);
		}

		if (state.chestEquipment.is(OceanItems.BATHYSPHERE_CHESTPLATE)) {
			submitLimb(pose, collector, light, state, getParentModel().body, tanksModel, CUTOUT);
			submitLimb(pose, collector, light, state, getParentModel().rightArm, rightArmModel, CUTOUT);
			submitLimb(pose, collector, light, state, getParentModel().leftArm, leftArmModel, CUTOUT);
		}

		boolean hasGreaves = state.legsEquipment.is(OceanItems.KELP_WEAVE_GREAVES);
		boolean hasBoots = state.feetEquipment.is(OceanItems.TRENCH_BOOTS);
		if (hasGreaves || hasBoots) {
			rightLegModel.setPartsVisible(hasGreaves, hasBoots);
			leftLegModel.setPartsVisible(hasGreaves, hasBoots);
			submitLimb(pose, collector, light, state, getParentModel().rightLeg, rightLegModel, CUTOUT);
			submitLimb(pose, collector, light, state, getParentModel().leftLeg, leftLegModel, CUTOUT);
		}
	}

	private static void submitLimb(
			PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state,
			ModelPart anchor, Model<Unit> model, RenderType baseType) {
		pose.pushPose();
		anchor.translateAndRotate(pose);
		pose.scale(0.5F, 0.5F, 0.5F);
		RenderCompat.submitModel(collector.order(0), model, Unit.INSTANCE, pose, baseType, light, OverlayTexture.NO_OVERLAY, -1, state.outlineColor);
		RenderCompat.submitModel(collector.order(1), model, Unit.INSTANCE, pose, EYES, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
		pose.popPose();
	}

	private static final class SuitPartModel extends Model<Unit> {
		SuitPartModel(ModelPart root) {
			super(root, RenderTypes::entityCutout);
		}
	}

	private static final class SuitGlassModel extends Model<Unit> {
		SuitGlassModel(ModelPart root) {
			super(root, RenderTypes::entityTranslucent);
		}
	}

	private static final class LegSuitModel extends Model<Unit> {
		private final ModelPart greaves;
		private final ModelPart boots;

		LegSuitModel(ModelPart root) {
			super(root, RenderTypes::entityCutout);
			this.greaves = root.getChild("greaves");
			this.boots = root.getChild("boots");
		}

		void setPartsVisible(boolean showGreaves, boolean showBoots) {
			this.greaves.visible = showGreaves;
			this.boots.visible = showBoots;
		}
	}
}
