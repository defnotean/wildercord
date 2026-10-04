package dev.wildercord.client.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
public final class SiltcrestRenderer extends MobRenderer<SiltcrestBittern,SiltcrestState,SiltcrestModel> {
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("siltcrest_bittern"),"main");
 public SiltcrestRenderer(EntityRendererProvider.Context c){super(c,new SiltcrestModel(c.bakeLayer(LAYER)),.3F);}
 public SiltcrestState createRenderState(){return new SiltcrestState();}
 public Identifier getTextureLocation(SiltcrestState s){return Wildercord.id("textures/entity/siltcrest_bittern.png");}
 public void extractRenderState(SiltcrestBittern e,SiltcrestState s,float p){super.extractRenderState(e,s,p);s.coil=Mth.lerp(p,e.coilO,e.coil);s.strike=Mth.lerp(p,e.strikeO,e.strike);s.preen=Mth.lerp(p,e.preenO,e.preen);s.rest=Mth.lerp(p,e.restO,e.rest);s.pose=e.pose();s.phase=e.phase();}
 public static void init(){ModelLayerRegistry.registerModelLayer(LAYER,SiltcrestModel::createLayer);EntityRendererRegistry.register(SiltcrestContent.BITTERN,SiltcrestRenderer::new);}
}
