package dev.wildercord.client.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
public final class CinderBailiffRenderer extends MobRenderer<CinderBailiff,CinderBailiffState,CinderBailiffModel> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("cinder_bailiff"),"main");
 public CinderBailiffRenderer(EntityRendererProvider.Context c){super(c,new CinderBailiffModel(c.bakeLayer(LAYER)),.55F);}
 public CinderBailiffState createRenderState(){return new CinderBailiffState();}
 public Identifier getTextureLocation(CinderBailiffState s){return Wildercord.id("textures/entity/cinder_bailiff.png");}
 public void extractRenderState(CinderBailiff e,CinderBailiffState s,float p){super.extractRenderState(e,s,p);s.warning=Mth.lerp(p,e.warningO,e.warning);s.fan=Mth.lerp(p,e.fanO,e.fan);s.rest=Mth.lerp(p,e.restO,e.rest);s.vent=e.vent();}
 public static void init(){ModelLayerRegistry.registerModelLayer(LAYER,CinderBailiffModel::createLayer);EntityRendererRegistry.register(EmberContent.BAILIFF,CinderBailiffRenderer::new);}
}
