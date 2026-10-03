package dev.wildercord.client.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
public final class SporebackRenderer extends WildlifeRenderer<SporebackSnail,SporebackModel> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("sporeback_snail"),"main");
 public SporebackRenderer(EntityRendererProvider.Context c) {super(c,new SporebackModel(c.bakeLayer(LAYER)),.3F,WildlifeRenderers.texture("sporeback_snail"));}
 @Override public void extractRenderState(SporebackSnail e,WildlifeRenderState s,float partial) {super.extractRenderState(e,s,partial);s.rest=Mth.lerp(partial,e.hideO,e.hide);s.graze=e.pose()==1?1:0;s.bow=e.dew()?1:0;}
 public static void init() {ModelLayerRegistry.registerModelLayer(LAYER,SporebackModel::createLayer);EntityRendererRegistry.register(SporebackContent.SNAIL,SporebackRenderer::new);}
}
