package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.LanternBearer;
import dev.wildercord.aura.world.LanternBearerRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Lantern-Bearer (see {@link LanternBearerModel}), well over a head taller than a player: lit by its own lantern whatever the dark,
 * a little see-through as a spirit is, gone to smoke when it fades, and not there at all while it lies in its grave.
 */
public final class LanternBearerRenderer extends MobRenderer<LanternBearer,LanternBearerRenderState,LanternBearerModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("lantern_bearer"),"main");
	private static final Identifier SKIN=Wildercord.id("textures/entity/lantern_bearer/skin.png");
	private static final Identifier GLOW=Wildercord.id("textures/entity/lantern_bearer/glow.png");
	static final float SCALE=1.6F;
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	public LanternBearerRenderer(EntityRendererProvider.Context c){super(c,new LanternBearerModel(c.bakeLayer(LAYER)),1.2F);addLayer(new Glow(this));}
	@Override public LanternBearerRenderState createRenderState(){return new LanternBearerRenderState();}
	@Override public Identifier getTextureLocation(LanternBearerRenderState s){return SKIN;}
	/** Down in its grave, or out in the dark with its lantern out, it is not there to see. */
	@Override public boolean shouldRender(LanternBearer e,Frustum frustum,double x,double y,double z,float partial){
		return e.move()!=LanternBearerRules.DORMANT&&!e.unseen()&&super.shouldRender(e,frustum,x,y,z,partial);
	}
	@Override protected int getBlockLightLevel(LanternBearer e,BlockPos pos){return 15;}
	@Override public void extractRenderState(LanternBearer e,LanternBearerRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.side=e.side();s.roused=e.roused();s.elapsed=e.elapsed(partial);s.rise=LanternBearerRules.rise(s.move,s.elapsed);
		float now=e.tickCount+partial;
		if(e.shownMove!=s.move || s.elapsed<e.shownElapsed-1){
			if(e.shownMove>=0){e.prevMove=e.shownMove;e.prevTell=e.shownTell;e.prevElapsed=e.shownElapsed;e.switchedAt=now;}
			e.shownMove=s.move;
		}
		e.shownTell=s.tell;e.shownElapsed=s.elapsed;
		boolean still=LanternBearerRules.scripted(s.move)||LanternBearerRules.scripted(e.prevMove);
		s.prevMove=e.prevMove;s.prevTell=e.prevTell;s.prevElapsed=e.prevElapsed;s.blend=still?1:Mth.clamp((now-e.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
		s.solid=solid(s);
	}
	/** Whole, but for the smoke of a fade (thinning at once and coming back as it ends), the dark of Lights Out, and the fading away of its death. */
	private static float solid(LanternBearerRenderState s){
		float a=.92F;
		if(s.move==LanternBearerRules.FADE){
			float k=s.elapsed/LanternBearerRules.FADE_TICKS;
			a*=Mth.clamp(Math.min(k<.3F?1-k/.3F*.8F:.2F,.2F+Math.max(0,k-.6F)/.4F*.8F),.2F,1);
		}
		if(s.move==LanternBearerRules.LIGHTS_OUT)a*=LanternBearerRules.lightsSolid(s.elapsed);
		if(s.deathTime>0)a*=Mth.clamp(1-(s.deathTime-6)/14F,0,1);
		return a;
	}
	@Override protected int getModelTint(LanternBearerRenderState s){return (Mth.clamp((int)(s.solid*255),0,255)<<24)|0xFFFFFF;}
	/** It crumples down into the ground in its own death, rather than flopping over sideways. */
	@Override protected float getFlipDegrees(){return 0;}
	@Override protected void scale(LanternBearerRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/**
	 * Its eyes and the flame in its lantern: the eyes catch light as it takes the lantern down; both thin with it and go out as it dies.
	 * They burn hot as it gathers a flare, breathes or holds its rite. In Lights Out they gutter as it breathes the lantern out and are
	 * dark, but for a low glimmer as it stands up behind someone.
	 */
	private static final class Glow extends RenderLayer<LanternBearerRenderState,LanternBearerModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Glow(RenderLayerParent<LanternBearerRenderState,LanternBearerModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,LanternBearerRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float lit=LanternBearerRules.holdsLantern(s.rise)?Mth.clamp((s.rise-(float)LanternBearerRules.HOOK_AT/LanternBearerRules.RISE_TICKS)*8,0,1):0;
			float alpha=(.75F+.25F*Mth.sin(s.ageInTicks*.31F)*Mth.sin(s.ageInTicks*.17F))*lit*Math.min(1,s.solid*1.2F)*lights(s);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
		private static float lights(LanternBearerRenderState s){
			// Burning hot and steady as it gathers the souls for a flare, breathes, or holds its rite.
			if((s.move==LanternBearerRules.FLARE||s.move==LanternBearerRules.RITE)&&s.elapsed<s.tell+2)return 1+.5F*Mth.clamp(s.elapsed/Math.max(1,s.tell),0,1);
			if(s.move==LanternBearerRules.BREATH&&s.elapsed>=s.tell&&s.elapsed<s.tell+LanternBearerRules.breathTicks(s.roused))return 1.4F;
			if(s.move!=LanternBearerRules.LIGHTS_OUT)return 1;
			if(s.elapsed<LanternBearerRules.SNUFF_TELL)return 1-.7F*Mth.clamp(s.elapsed/LanternBearerRules.SNUFF_TELL,0,1)+.15F*Mth.sin(s.elapsed*1.7F);
			int stage=LanternBearerRules.lightsStage(s.elapsed);
			if(stage==LanternBearerRules.OVER)return 1;
			if(stage==LanternBearerRules.GONE)return 0;
			return .3F+.15F*Mth.sin(s.ageInTicks*.9F);
		}
	}
}
