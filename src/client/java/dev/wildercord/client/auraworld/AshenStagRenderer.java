package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.AshenStagRules;
import dev.wildercord.aura.world.AshenStag;
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

/** The Ashen Stag (see {@link AshenStagModel}), its shoulder above a player's head, with its eyes, the cracks in its hide and its antler tips lit whatever the light. */
public final class AshenStagRenderer extends MobRenderer<AshenStag,AshenStagRenderState,AshenStagModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("ashen_stag"),"main");
	private static final Identifier SKIN=Wildercord.id("textures/entity/ashen_stag/skin.png");
	private static final Identifier GLOW=Wildercord.id("textures/entity/ashen_stag/glow.png");
	static final float SCALE=1.8F;
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	public AshenStagRenderer(EntityRendererProvider.Context c){super(c,new AshenStagModel(c.bakeLayer(LAYER)),1.5F);addLayer(new Eyes(this));}
	@Override public AshenStagRenderState createRenderState(){return new AshenStagRenderState();}
	@Override public Identifier getTextureLocation(AshenStagRenderState s){return SKIN;}
	@Override public void extractRenderState(AshenStag e,AshenStagRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.side=e.side();s.elapsed=e.elapsed(partial);s.rise=AshenStagRules.rise(s.move,s.elapsed);
		// Blending out of the move before over a few ticks; out of its bed or into it the rise does that itself.
		float now=e.tickCount+partial;
		// A new move, or the same one begun again (each run of a rush), blends out of the last.
		if(e.shownMove!=s.move || s.elapsed<e.shownElapsed-1){
			if(e.shownMove>=0){e.prevMove=e.shownMove;e.prevTell=e.shownTell;e.prevElapsed=e.shownElapsed;e.switchedAt=now;}
			e.shownMove=s.move;
		}
		e.shownTell=s.tell;e.shownElapsed=s.elapsed;
		boolean still=s.move==AshenStagRules.DORMANT||s.move==AshenStagRules.RISE||s.move==AshenStagRules.BED||e.prevMove==AshenStagRules.DORMANT||e.prevMove==AshenStagRules.RISE||e.prevMove==AshenStagRules.BED;
		s.prevMove=e.prevMove;s.prevTell=e.prevTell;s.prevElapsed=e.prevElapsed;s.blend=still?1:Mth.clamp((now-e.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
	}
	/** Its legs buckle and it topples in its own death, rather than flopping over sideways. */
	@Override protected float getFlipDegrees(){return 0;}
	@Override protected void scale(AshenStagRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/** Its embers: banked and slow while it lies bedded, flaring once it is up and breathing as it fights; guttering out as it dies. */
	private static final class Eyes extends RenderLayer<AshenStagRenderState,AshenStagModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Eyes(RenderLayerParent<AshenStagRenderState,AshenStagModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,AshenStagRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float alpha=.3F+.1F*Mth.sin(s.ageInTicks*.08F)+.6F*s.rise;
			if(s.deathTime>0)alpha*=Math.max(0,1-s.deathTime/20F);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
	}
}
