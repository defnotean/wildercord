package dev.wildercord.client.render;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.StormConductor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class StormConductorRenderer extends DungeonBossRenderer<StormConductor,StormConductorModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("storm_conductor"),"main");
	public StormConductorRenderer(EntityRendererProvider.Context context) {
		super(context,new StormConductorModel(context.bakeLayer(LAYER)),.8F,Wildercord.id("textures/entity/storm_conductor.png"));
		glow(Wildercord.id("textures/entity/storm_conductor_glow.png"), s->white((.7F+.3F*s.casting-.4F*s.exposed)*alive(s)));
	}
}
