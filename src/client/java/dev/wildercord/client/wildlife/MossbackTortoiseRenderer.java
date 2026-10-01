package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.wildlife.MossbackTortoise;
import dev.wildercord.wildlife.WildlifeRules.Garden;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A mossback tortoise, and the garden on its shell: three real plants of its land (their own block models, drawn
 * small) rooted in the moss of its crown, following the shell as it rocks and settles. A baby's shell is bare.
 */
public final class MossbackTortoiseRenderer extends WildlifeRenderer<MossbackTortoise, MossbackTortoiseModel> {
	private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

	/** One plant: what it is, where on the crown (shell pixels, x and z), how big, and which way it's turned. */
	private record Plant(BlockState block, float x, float z, float size, float turn) {}

	private static final Map<Garden, List<Plant>> GARDENS = new EnumMap<>(Garden.class);

	static {
		GARDENS.put(Garden.SWAMP, List.of(
			new Plant(Blocks.BLUE_ORCHID.defaultBlockState(), -2.2F, -2.5F, 0.55F, 20),
			new Plant(Blocks.BROWN_MUSHROOM.defaultBlockState(), 2.4F, 1.2F, 0.45F, -35),
			new Plant(Blocks.BLUE_ORCHID.defaultBlockState(), -0.8F, 3.2F, 0.45F, 70)));
		GARDENS.put(Garden.MANGROVE, List.of(
			new Plant(Blocks.MANGROVE_PROPAGULE.defaultBlockState(), -1.6F, -1.8F, 0.6F, 15),
			new Plant(Blocks.LILY_PAD.defaultBlockState(), 1.2F, 2.0F, 0.6F, 40),
			new Plant(Blocks.RED_MUSHROOM.defaultBlockState(), 2.6F, -2.6F, 0.4F, -20)));
		GARDENS.put(Garden.JUNGLE, List.of(
			new Plant(Blocks.FERN.defaultBlockState(), -1.8F, -1.6F, 0.6F, 10),
			new Plant(Blocks.DANDELION.defaultBlockState(), 2.4F, 1.6F, 0.45F, -40),
			new Plant(Blocks.FERN.defaultBlockState(), -0.4F, 3.2F, 0.45F, 65)));
	}

	private final BlockModelResolver blocks;

	public MossbackTortoiseRenderer(EntityRendererProvider.Context context) {
		super(context, new MossbackTortoiseModel(context.bakeLayer(WildlifeRenderers.MOSSBACK_TORTOISE)), 0.95F,
			WildlifeRenderers.texture("mossback_tortoise"));
		this.blocks = context.getBlockModelResolver();
		Identifier glow = WildlifeRenderers.texture("mossback_tortoise_glow");
		glow(state -> glow);
		addLayer(new GardenLayer(this));
	}

	@Override
	public void extractRenderState(MossbackTortoise tortoise, WildlifeRenderState state, float partial) {
		super.extractRenderState(tortoise, state, partial);
		state.hide = Mth.lerp(partial, tortoise.hideO, tortoise.hide);
		state.variant = tortoise.garden().ordinal();
		// Glowing specks in its moss, like lichen's, come out after dark.
		state.glow = 0.75F * WildlifeRenderers.night(tortoise.level());
		List<Plant> plants = GARDENS.get(tortoise.garden());
		for (int i = 0; i < state.garden.length; i++) {
			blocks.update(state.garden[i], plants.get(i).block(), DISPLAY);
		}
	}

	/** The garden: each plant set on the crown, rooted, turned and sized as it grows there. */
	private static final class GardenLayer extends RenderLayer<WildlifeRenderState, MossbackTortoiseModel> {
		GardenLayer(RenderLayerParent<WildlifeRenderState, MossbackTortoiseModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WildlifeRenderState state, float yRot, float xRot) {
			if (state.isBaby || state.isInvisible) {
				return;
			}
			List<Plant> plants = GARDENS.get(Garden.byId(state.variant));
			int overlay = LivingEntityRenderer.getOverlayCoords(state, 0);
			for (int i = 0; i < plants.size(); i++) {
				Plant plant = plants.get(i);
				poseStack.pushPose();
				getParentModel().toCrown(poseStack);
				poseStack.translate(plant.x() / 16F, 0, plant.z() / 16F);
				// Upright again (the model is drawn upside down), sized, and rooted at its base.
				poseStack.scale(-plant.size(), -plant.size(), plant.size());
				poseStack.rotateDegrees(Axis.YP, plant.turn());
				// A plant sways a little on the walking shell.
				poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.ageInTicks * 0.08F + i * 2) * 3);
				poseStack.translate(-0.5F, 0, -0.5F);
				state.garden[i].submit(poseStack, collector, light, overlay, state.outlineColor);
				poseStack.popPose();
			}
		}
	}
}
