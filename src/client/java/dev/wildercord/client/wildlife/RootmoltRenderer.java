package dev.wildercord.client.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
public final class RootmoltRenderer extends WildlifeRenderer<RootmoltStrider,RootmoltModel> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("rootmolt_strider"),"main");
 public RootmoltRenderer(EntityRendererProvider.Context c) {super(c,new RootmoltModel(c.bakeLayer(LAYER)),.48F,Wildercord.id("textures/entity/rootmolt_strider.png"));}
 @Override public void extractRenderState(RootmoltStrider e,WildlifeRenderState s,float partial) {super.extractRenderState(e,s,partial);s.watch=Mth.lerp(partial,e.warnO,e.warn);s.strike=Mth.lerp(partial,e.rakeO,e.rake);s.graze=Mth.lerp(partial,e.feedO,e.feed);}
 public static void init() {ModelLayerRegistry.registerModelLayer(LAYER,RootmoltModel::createLayer);EntityRendererRegistry.register(RootmoltContent.STRIDER,RootmoltRenderer::new);}
}
