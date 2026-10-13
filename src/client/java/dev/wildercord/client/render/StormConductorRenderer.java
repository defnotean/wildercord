package dev.wildercord.client.render;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.StormConductor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
public final class StormConductorRenderer extends DungeonBossRenderer<StormConductor,StormConductorModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("storm_conductor"),"main");
	public StormConductorRenderer(EntityRendererProvider.Context context) {
		super(context,new StormConductorModel(context.bakeLayer(LAYER)),.8F,Wildercord.id("textures/entity/storm_conductor.png"));
		// Its coils burn brighter as it casts or winds up a move, and dim while it lies grounded or open.
		glow(Wildercord.id("textures/entity/storm_conductor_glow.png"), s->white((.7F+.3F*Math.max(s.casting,windup(s))-.4F*s.exposed)*alive(s)));
	}
	/** How far through the tell of its move it is, 0 to 1. */
	private static float windup(DungeonBossRenderState s) { return s.move>0&&s.tell>0?Mth.clamp(s.since/s.tell,0,1):0; }
}
