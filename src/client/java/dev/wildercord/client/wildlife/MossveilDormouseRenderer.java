package dev.wildercord.client.wildlife;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
public final class MossveilDormouseRenderer extends MobRenderer<MossveilDormouse,MossveilDormouseState,MossveilDormouseModel>{
 public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("mossveil_dormouse"),"main");
 public MossveilDormouseRenderer(EntityRendererProvider.Context c){super(c,new MossveilDormouseModel(c.bakeLayer(LAYER)),.25F);}
 public MossveilDormouseState createRenderState(){return new MossveilDormouseState();}
 public Identifier getTextureLocation(MossveilDormouseState s){return Wildercord.id("textures/entity/mossveil_dormouse.png");}
 public void extractRenderState(MossveilDormouse e,MossveilDormouseState s,float p){super.extractRenderState(e,s,p);s.curl=Mth.lerp(p,e.curlO,e.curl);s.sniff=Mth.lerp(p,e.sniffO,e.sniff);}
 public static void init(){ModelLayerRegistry.registerModelLayer(LAYER,MossveilDormouseModel::createLayer);EntityRendererRegistry.register(MossveilContent.DORMOUSE,MossveilDormouseRenderer::new);}
}
