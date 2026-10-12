package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.MojitoRules;
import dev.wildercord.aura.world.Mojito;
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

/** Mojito (see {@link MojitoModel}), his head nearly three times a player's height, with his eyes, the runes down his blade and the magic at his sides lit whatever the light. */
public final class MojitoRenderer extends MobRenderer<Mojito,MojitoRenderState,MojitoModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("mojito"),"main");
	private static final Identifier SKIN=Wildercord.id("textures/entity/mojito/skin.png");
	private static final Identifier GLOW=Wildercord.id("textures/entity/mojito/glow.png");
	static final float SCALE=2.2F;
	/** How long one move takes to blend into the next, in ticks. */
	private static final float BLEND_TICKS=4;
	public MojitoRenderer(EntityRendererProvider.Context c){super(c,new MojitoModel(c.bakeLayer(LAYER)),2.0F);addLayer(new Eyes(this));}
	@Override public MojitoRenderState createRenderState(){return new MojitoRenderState();}
	@Override public Identifier getTextureLocation(MojitoRenderState s){return SKIN;}
	@Override public void extractRenderState(Mojito e,MojitoRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.tell=e.tell();s.side=e.side();s.elapsed=e.elapsed(partial);s.rise=MojitoRules.rise(s.move,s.elapsed);
		// Blending out of the move before over a few ticks; out of his sleep or back into it the rise does that itself.
		float now=e.tickCount+partial;
		// A new move, or the same one begun again (each slash), blends out of the last.
		if(e.shownMove!=s.move || s.elapsed<e.shownElapsed-1){
			if(e.shownMove>=0){e.prevMove=e.shownMove;e.prevTell=e.shownTell;e.prevElapsed=e.shownElapsed;e.switchedAt=now;}
			e.shownMove=s.move;
		}
		e.shownTell=s.tell;e.shownElapsed=s.elapsed;
		boolean still=s.move==MojitoRules.DORMANT||s.move==MojitoRules.RISE||s.move==MojitoRules.BED||e.prevMove==MojitoRules.DORMANT||e.prevMove==MojitoRules.RISE||e.prevMove==MojitoRules.BED;
		s.prevMove=e.prevMove;s.prevTell=e.prevTell;s.prevElapsed=e.prevElapsed;s.blend=still?1:Mth.clamp((now-e.switchedAt)/BLEND_TICKS,0,1);
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
	}
	/** His legs buckle and he rolls onto his side in his own death, rather than flipping over. */
	@Override protected float getFlipDegrees(){return 0;}
	@Override protected void scale(MojitoRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/** His mint light: a faint glimmer on the blade while he sleeps, bright once he is up, fading out as he dies. */
	private static final class Eyes extends RenderLayer<MojitoRenderState,MojitoModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Eyes(RenderLayerParent<MojitoRenderState,MojitoModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,MojitoRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float alpha=.25F+.1F*Mth.sin(s.ageInTicks*.08F)+.65F*s.rise;
			if(s.deathTime>0)alpha*=Math.max(0,1-s.deathTime/20F);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
	}
}
