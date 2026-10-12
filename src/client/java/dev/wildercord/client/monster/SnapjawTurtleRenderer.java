package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.SnapjawTurtle;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Snapjaw Turtle (see {@link SnapjawTurtleModel}). */
public class SnapjawTurtleRenderer extends WildMonsterRenderer<SnapjawTurtle, SnapjawTurtleModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("snapjaw_turtle"), "main");

	public SnapjawTurtleRenderer(EntityRendererProvider.Context context) {
		super(context, new SnapjawTurtleModel(context.bakeLayer(LAYER)), 0.9F, Wildercord.id("textures/entity/snapjaw_turtle.png"));
	}
}
