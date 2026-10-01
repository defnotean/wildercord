package dev.wildercord.client.render;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.RootGuardian;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class RootGuardianRenderer extends DungeonBossRenderer<RootGuardian,RootGuardianModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("root_guardian"),"main");
	public RootGuardianRenderer(EntityRendererProvider.Context context) {
		super(context,new RootGuardianModel(context.bakeLayer(LAYER)),.9F,Wildercord.id("textures/entity/root_guardian.png"));
		glow(Wildercord.id("textures/entity/root_guardian_glow.png"), s->white((.3F+.7F*s.exposed)*alive(s)));
	}
}
