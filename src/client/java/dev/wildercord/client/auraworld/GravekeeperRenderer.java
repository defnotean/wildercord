package dev.wildercord.client.auraworld;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.Gravekeeper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class GravekeeperRenderer extends MobRenderer<Gravekeeper,GravekeeperRenderState,GravekeeperModel>{
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("gravekeeper"),"main");
	public GravekeeperRenderer(EntityRendererProvider.Context c){super(c,new GravekeeperModel(c.bakeLayer(LAYER)),.75F);}
	@Override public GravekeeperRenderState createRenderState(){return new GravekeeperRenderState();}
	@Override public Identifier getTextureLocation(GravekeeperRenderState s){return Wildercord.id("textures/entity/gravekeeper.png");}
	@Override public void extractRenderState(Gravekeeper e,GravekeeperRenderState s,float partial){super.extractRenderState(e,s,partial);s.move=e.move();s.elapsed=e.elapsed(partial);}
}
