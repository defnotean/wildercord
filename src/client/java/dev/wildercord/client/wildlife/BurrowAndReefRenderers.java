package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.DelverMole;
import dev.wildercord.wildlife.MountContent;
import dev.wildercord.wildlife.ReefbackTurtle;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

/** The water and burrowing mounts (0.13): each drawn larger than its model, with a saddle painted on once it wears one. */
public final class BurrowAndReefRenderers {
	private BurrowAndReefRenderers() {}

	public static final ModelLayerLocation REEFBACK_TURTLE = new ModelLayerLocation(Wildercord.id("reefback_turtle"), "main");
	public static final ModelLayerLocation DELVER_MOLE = new ModelLayerLocation(Wildercord.id("delver_mole"), "main");

	public static final class ReefbackTurtleRenderer extends WildlifeRenderer<ReefbackTurtle, ReefbackTurtleModel> {
		private static final Identifier BARE = WildlifeRenderers.texture("reefback_turtle");
		private static final Identifier SADDLED = WildlifeRenderers.texture("reefback_turtle_saddled");

		public ReefbackTurtleRenderer(EntityRendererProvider.Context context) {
			super(context, new ReefbackTurtleModel(context.bakeLayer(REEFBACK_TURTLE)), 1.0F, BARE);
			addLayer(new MountBardingLayer<>(this, "reefback_turtle", 19));
		}

		@Override
		public Identifier getTextureLocation(WildlifeRenderState state) {
			return state.variant == 1 ? SADDLED : BARE;
		}

		@Override
		public void extractRenderState(ReefbackTurtle turtle, WildlifeRenderState state, float partial) {
			super.extractRenderState(turtle, state, partial);
			state.variant = turtle.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() ? 0 : 1;
			state.swim = Mth.lerp(partial, turtle.swimO, turtle.swim);
			state.barding = MountBardingLayer.colour(turtle.getItemBySlot(EquipmentSlot.BODY));
		}

		@Override
		protected void scale(WildlifeRenderState state, PoseStack poseStack) {
			super.scale(state, poseStack);
			poseStack.scale(1.5F, 1.5F, 1.5F);
		}
	}

	public static final class DelverMoleRenderer extends WildlifeRenderer<DelverMole, DelverMoleModel> {
		private static final Identifier BARE = WildlifeRenderers.texture("delver_mole");
		private static final Identifier SADDLED = WildlifeRenderers.texture("delver_mole_saddled");

		public DelverMoleRenderer(EntityRendererProvider.Context context) {
			super(context, new DelverMoleModel(context.bakeLayer(DELVER_MOLE)), 0.9F, BARE);
			addLayer(new MountBardingLayer<>(this, "delver_mole", 15));
		}

		@Override
		public Identifier getTextureLocation(WildlifeRenderState state) {
			return state.variant == 1 ? SADDLED : BARE;
		}

		@Override
		public void extractRenderState(DelverMole mole, WildlifeRenderState state, float partial) {
			super.extractRenderState(mole, state, partial);
			state.variant = mole.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() ? 0 : 1;
			state.dig = Mth.lerp(partial, mole.digO, mole.dig);
			state.barding = MountBardingLayer.colour(mole.getItemBySlot(EquipmentSlot.BODY));
		}

		@Override
		protected void scale(WildlifeRenderState state, PoseStack poseStack) {
			super.scale(state, poseStack);
			poseStack.scale(1.4F, 1.4F, 1.4F);
		}
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(REEFBACK_TURTLE, ReefbackTurtleModel::createLayer);
		ModelLayerRegistry.registerModelLayer(DELVER_MOLE, DelverMoleModel::createLayer);
		EntityRendererRegistry.register(MountContent.REEFBACK_TURTLE, ReefbackTurtleRenderer::new);
		EntityRendererRegistry.register(MountContent.DELVER_MOLE, DelverMoleRenderer::new);
	}
}
