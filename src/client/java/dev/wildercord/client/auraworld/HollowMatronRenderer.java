package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.HollowMatron;
import dev.wildercord.aura.world.HollowMatronRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The Hollow Matron (see {@link HollowMatronModel}), her legs spanning near six blocks, with her eight eyes lit red whatever the light. */
public final class HollowMatronRenderer extends MobRenderer<HollowMatron,HollowMatronRenderState,HollowMatronModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("hollow_matron"),"main");
	private static final Identifier SKIN=Wildercord.id("textures/entity/hollow_matron/skin.png");
	private static final Identifier GLOW=Wildercord.id("textures/entity/hollow_matron/glow.png");
	static final float SCALE=2.4F;
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	public HollowMatronRenderer(EntityRendererProvider.Context c){super(c,new HollowMatronModel(c.bakeLayer(LAYER)),2.0F);addLayer(new Eyes(this));}
	@Override public HollowMatronRenderState createRenderState(){return new HollowMatronRenderState();}
	@Override public Identifier getTextureLocation(HollowMatronRenderState s){return SKIN;}
	@Override public void extractRenderState(HollowMatron e,HollowMatronRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.side=e.side();s.elapsed=e.elapsed(partial);s.roused=e.roused();
		s.drop=HollowMatronRules.drop(s.move,s.elapsed);
		s.threadUp=e.anchor()-(float)Mth.lerp(partial,e.yo,e.getY());
		float now=e.tickCount+partial;
		// A new move, or the same one begun again, blends out of the last; her drop and climb do their own.
		if(e.shownMove!=s.move || s.elapsed<e.shownElapsed-1){
			if(e.shownMove>=0){e.prevMove=e.shownMove;e.prevTell=e.shownTell;e.prevElapsed=e.shownElapsed;e.switchedAt=now;}
			e.shownMove=s.move;
		}
		e.shownTell=s.tell;e.shownElapsed=s.elapsed;
		boolean still=HollowMatronRules.scripted(s.move)||HollowMatronRules.scripted(e.prevMove);
		s.prevMove=e.prevMove;s.prevTell=e.prevTell;s.prevElapsed=e.prevElapsed;s.blend=still?1:Mth.clamp((now-e.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
	}
	/** She rolls onto her back in her own death, rather than flopping over sideways. */
	@Override protected float getFlipDegrees(){return 0;}
	@Override protected void scale(HollowMatronRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/** Her eyes: dull while she hangs, smouldering red once she is down, burning and flickering once she has roused, going out as she dies. */
	private static final class Eyes extends RenderLayer<HollowMatronRenderState,HollowMatronModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Eyes(RenderLayerParent<HollowMatronRenderState,HollowMatronModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,HollowMatronRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float alpha=.35F+.1F*Mth.sin(s.ageInTicks*.05F)+.55F*s.drop;
			if(s.roused)alpha+=.25F+.15F*Mth.sin(s.ageInTicks*.3F);
			if(s.deathTime>0)alpha*=Math.max(0,1-s.deathTime/20F);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
	}
}
