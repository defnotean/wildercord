package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.FrostWyrmlingRules;
import dev.wildercord.aura.world.FrostWyrmling;
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

/** The Frost Wyrmling (see {@link FrostWyrmlingModel}), its head held well above a player's, with its eyes and the frost in its horns lit whatever the light. */
public final class FrostWyrmlingRenderer extends MobRenderer<FrostWyrmling,FrostWyrmlingRenderState,FrostWyrmlingModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("frost_wyrmling"),"main");
	private static final Identifier SKIN=Wildercord.id("textures/entity/frost_wyrmling/skin.png");
	private static final Identifier GLOW=Wildercord.id("textures/entity/frost_wyrmling/glow.png");
	static final float SCALE=1.5F;
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	public FrostWyrmlingRenderer(EntityRendererProvider.Context c){super(c,new FrostWyrmlingModel(c.bakeLayer(LAYER)),1.6F);addLayer(new Eyes(this));}
	@Override public FrostWyrmlingRenderState createRenderState(){return new FrostWyrmlingRenderState();}
	@Override public Identifier getTextureLocation(FrostWyrmlingRenderState s){return SKIN;}
	@Override public void extractRenderState(FrostWyrmling e,FrostWyrmlingRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.side=e.side();s.elapsed=e.elapsed(partial);s.rise=FrostWyrmlingRules.rise(s.move,s.elapsed);
		// Blending out of the move before over a few ticks; out of its curl or into it the rise does that itself.
		float now=e.tickCount+partial;
		if(e.shownMove!=s.move || s.elapsed<e.shownElapsed-1){
			if(e.shownMove>=0){e.prevMove=e.shownMove;e.prevTell=e.shownTell;e.prevElapsed=e.shownElapsed;e.switchedAt=now;}
			e.shownMove=s.move;
		}
		e.shownTell=s.tell;e.shownElapsed=s.elapsed;
		boolean still=FrostWyrmlingRules.scripted(s.move)||FrostWyrmlingRules.scripted(e.prevMove);
		s.prevMove=e.prevMove;s.prevTell=e.prevTell;s.prevElapsed=e.prevElapsed;s.blend=still?1:Mth.clamp((now-e.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
	}
	/** Its legs give and it rolls onto its side in its own death, rather than flopping over sideways. */
	@Override protected float getFlipDegrees(){return 0;}
	@Override protected void scale(FrostWyrmlingRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/** Its eyes and the frost in its horns: dim and slow while it sleeps, bright once it is up; going out as it dies. */
	private static final class Eyes extends RenderLayer<FrostWyrmlingRenderState,FrostWyrmlingModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Eyes(RenderLayerParent<FrostWyrmlingRenderState,FrostWyrmlingModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,FrostWyrmlingRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float alpha=.2F+.1F*Mth.sin(s.ageInTicks*.07F)+.7F*s.rise;
			if(s.deathTime>0)alpha*=Math.max(0,1-s.deathTime/20F);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
	}
}
