package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.BellWardenRules;
import dev.wildercord.aura.world.DrownedBellWarden;
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

/** The Drowned Bell-Warden (see {@link BellWardenModel}), near twice a player's size, with the eyes in its bell's crack lit whatever the light. */
public final class BellWardenRenderer extends MobRenderer<DrownedBellWarden,BellWardenRenderState,BellWardenModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("drowned_bell_warden"),"main");
	private static final Identifier SKIN=Wildercord.id("textures/entity/drowned_bell_warden/skin.png");
	private static final Identifier GLOW=Wildercord.id("textures/entity/drowned_bell_warden/glow.png");
	static final float SCALE=1.85F;
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	public BellWardenRenderer(EntityRendererProvider.Context c){super(c,new BellWardenModel(c.bakeLayer(LAYER)),1.3F);addLayer(new Eyes(this));}
	@Override public BellWardenRenderState createRenderState(){return new BellWardenRenderState();}
	@Override public Identifier getTextureLocation(BellWardenRenderState s){return SKIN;}
	@Override public void extractRenderState(DrownedBellWarden e,BellWardenRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.elapsed=e.elapsed(partial);s.rise=BellWardenRules.rise(s.move,s.elapsed);
		// Blending out of the move before over a few ticks; out of kneeling or into it the rise does that itself.
		float now=e.tickCount+partial;
		if(e.shownMove!=s.move){
			if(e.shownMove>=0){e.prevMove=e.shownMove;e.prevTell=e.shownTell;e.prevElapsed=e.shownElapsed;e.switchedAt=now;}
			e.shownMove=s.move;
		}
		e.shownTell=s.tell;e.shownElapsed=s.elapsed;
		boolean still=s.move==BellWardenRules.DORMANT||s.move==BellWardenRules.RISE||s.move==BellWardenRules.KNEEL||e.prevMove==BellWardenRules.DORMANT||e.prevMove==BellWardenRules.RISE||e.prevMove==BellWardenRules.KNEEL;
		s.prevMove=e.prevMove;s.prevTell=e.prevTell;s.prevElapsed=e.prevElapsed;s.blend=still?1:Mth.clamp((now-e.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;s.cracked=e.cracked();
	}
	/** It sinks and topples in its own death, rather than flopping over sideways. */
	@Override protected float getFlipDegrees(){return 0;}
	@Override protected void scale(BellWardenRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/** The eyes in the crack: dim and slow while it kneels, burning once it is up, throbbing once its bell is torn open; guttering out as it dies. */
	private static final class Eyes extends RenderLayer<BellWardenRenderState,BellWardenModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Eyes(RenderLayerParent<BellWardenRenderState,BellWardenModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,BellWardenRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float alpha=.25F+.08F*Mth.sin(s.ageInTicks*.06F)+.7F*s.rise;
			// Torn open, they throb fast and bright through the split.
			if(s.cracked)alpha=.78F+.22F*Mth.sin(s.ageInTicks*.3F);
			if(s.deathTime>0)alpha*=Math.max(0,1-s.deathTime/16F);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
	}
}
