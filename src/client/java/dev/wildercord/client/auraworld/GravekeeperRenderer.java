package dev.wildercord.client.auraworld;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.Gravekeeper;
import dev.wildercord.aura.world.SwordTombRules;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

public final class GravekeeperRenderer extends MobRenderer<Gravekeeper,GravekeeperRenderState,GravekeeperModel>{
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("gravekeeper"),"main");
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	/** What each Gravekeeper last showed, kept here since the entity itself carries no record of it. */
	private static final class Mem{int shownMove=-1,shownTell,prevMove,prevTell;float shownElapsed,prevElapsed,switchedAt;}
	private final Map<Gravekeeper,Mem> memory=new WeakHashMap<>();
	public GravekeeperRenderer(EntityRendererProvider.Context c){super(c,new GravekeeperModel(c.bakeLayer(LAYER)),.75F);}
	@Override public GravekeeperRenderState createRenderState(){return new GravekeeperRenderState();}
	@Override public Identifier getTextureLocation(GravekeeperRenderState s){return Wildercord.id("textures/entity/gravekeeper.png");}
	@Override public void extractRenderState(Gravekeeper e,GravekeeperRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.elapsed=e.elapsed(partial);
		Mem m=memory.computeIfAbsent(e,k->new Mem());
		float now=e.tickCount+partial;
		// A new move, or the same one begun again, blends out of the last.
		if(m.shownMove!=s.move || s.elapsed<m.shownElapsed-1){
			if(m.shownMove>=0){m.prevMove=m.shownMove;m.prevTell=m.shownTell;m.prevElapsed=m.shownElapsed;m.switchedAt=now;}
			m.shownMove=s.move;
		}
		m.shownTell=s.tell;m.shownElapsed=s.elapsed;
		s.prevMove=m.prevMove;s.prevTell=m.prevTell;s.prevElapsed=m.prevElapsed;
		// A recovery plays out the blow before it itself (its strike snap), so it does not blend.
		s.blend=s.move==SwordTombRules.RECOVER?1:Mth.clamp((now-m.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
	}
	/** It kneels and topples in its own death, rather than flopping over sideways. */
	@Override protected float getFlipDegrees(){return 0;}
}
