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
	public BellWardenRenderer(EntityRendererProvider.Context c){super(c,new BellWardenModel(c.bakeLayer(LAYER)),1.3F);addLayer(new Eyes(this));}
	@Override public BellWardenRenderState createRenderState(){return new BellWardenRenderState();}
	@Override public Identifier getTextureLocation(BellWardenRenderState s){return SKIN;}
	@Override public void extractRenderState(DrownedBellWarden e,BellWardenRenderState s,float partial){
		super.extractRenderState(e,s,partial);s.move=e.move();s.elapsed=e.elapsed(partial);s.rise=BellWardenRules.rise(s.move,s.elapsed);
	}
	@Override protected void scale(BellWardenRenderState s,PoseStack pose){pose.scale(SCALE,SCALE,SCALE);}

	/** The eyes in the crack: dim and slow while it kneels, burning once it is up; guttering out as it dies. */
	private static final class Eyes extends RenderLayer<BellWardenRenderState,BellWardenModel> {
		private final RenderType type=RenderTypes.eyes(GLOW);
		Eyes(RenderLayerParent<BellWardenRenderState,BellWardenModel> parent){super(parent);}
		@Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,BellWardenRenderState s,float yRot,float xRot){
			if(s.isInvisible)return;
			float alpha=.25F+.08F*Mth.sin(s.ageInTicks*.06F)+.7F*s.rise;
			if(s.deathTime>0)alpha*=Math.max(0,1-s.deathTime/16F);
			int a=Mth.clamp((int)(alpha*255),0,255);if(a<4)return;
			collector.order(1).submitModel(getParentModel(),s,pose,type,light,OverlayTexture.NO_OVERLAY,(a<<24)|0xFFFFFF,null,s.outlineColor);
		}
	}
}
