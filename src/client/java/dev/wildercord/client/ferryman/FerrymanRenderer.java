package dev.wildercord.client.ferryman;

import com.mojang.blaze3d.vertex.*;
import dev.wildercord.Wildercord;
import dev.wildercord.world.ferryman.ReedCrownedFerryman;
import net.minecraft.client.model.geom.ModelLayerLocation;
import dev.wildercord.client.compat.RenderCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.culling.Frustum;
import dev.wildercord.client.compat.CameraRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import static dev.wildercord.world.ferryman.FerrymanRules.*;

/** The attack marks are world geometry, so Minimal particles never removes an essential tell. */
public final class FerrymanRenderer extends dev.wildercord.client.compat.CullableMobRenderer<ReedCrownedFerryman,FerrymanRenderState,FerrymanModel> {
	public static final ModelLayerLocation LAYER=new ModelLayerLocation(Wildercord.id("reed_crowned_ferryman"),"main");
	private static final ResourceLocation SKIN=Wildercord.id("textures/entity/ferryman.png"), MARK=Wildercord.id("textures/entity/ferryman_mark.png"), GLOW=Wildercord.id("textures/entity/ferryman_glow.png");
	public FerrymanRenderer(EntityRendererProvider.Context c) {
		super(c,new FerrymanModel(c.bakeLayer(LAYER)),1.7F);
		addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<FerrymanRenderState,FerrymanModel>(this) {
			@Override public void render(PoseStack pose, MultiBufferSource buffer, int light,FerrymanRenderState state,float yRot,float xRot) {
				if(!state.isInvisible && state.deathTime<=0) dev.wildercord.client.compat.RenderCompat.submitModel(buffer, getParentModel(), state, pose, RenderType.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
			}
		});
	}
	@Override public FerrymanRenderState createRenderState() { return new FerrymanRenderState(); }
	@Override public ResourceLocation getTextureLocation(FerrymanRenderState state) { return SKIN; }
	@Override protected float getFlipDegrees() { return 0; }
	@Override protected boolean shouldRenderEntity(ReedCrownedFerryman e, Frustum frustum, double x, double y, double z, float partial) {
		return superShouldRenderEntity(e,frustum,x,y,z,partial) || frustum.isVisible(e.getBoundingBox().inflate(16,2,16));
	}
	@Override public void extractRenderState(ReedCrownedFerryman e,FerrymanRenderState s,float partial) {
		super.extractRenderState(e, s, partial); s.move=e.move(); s.elapsed=e.elapsed(partial); s.aim=e.aim(); s.markX=e.markX(); s.markZ=e.markZ(); s.acrossX=e.acrossX(); s.second=e.secondPhase();
		s.markX2=e.markX2(); s.markZ2=e.markZ2(); s.paired=e.pairedMark();
		s.hurt=e.hurtTime>0?Math.max(0,(e.hurtTime-partial)/10F):0;
	}
	@Override public void render(FerrymanRenderState s, PoseStack pose, MultiBufferSource buffer, int light) {
		CameraRenderState camera = RenderCompat.cameraState();
		super.submit(s,pose,buffer,camera);
		if(s.isInvisible || s.deathTime>0 || s.move==IDLE || s.elapsed>tell(s.move)+5) return;
		RenderCompat.submitCustomGeometry(buffer, pose,RenderType.entityTranslucentEmissive(MARK),(p,b)->marks(s,p,b));
	}
	private static void marks(FerrymanRenderState s,PoseStack.Pose p,VertexConsumer b) {
		int danger=s.elapsed>=tell(s.move)?0xEFFFF1B0:0xDDF06B91, safe=0xDD73E4D0;
		if(s.move==SWEEP) {
			for(int i=0;i<28;i++) {
				double a=s.aim-SWEEP_HALF_ANGLE+2*SWEEP_HALF_ANGLE*i/28, c=s.aim-SWEEP_HALF_ANGLE+2*SWEEP_HALF_ANGLE*(i+1)/28;
				quad(p,b,Math.sin(a)*(SWEEP_REACH-.13),Math.cos(a)*(SWEEP_REACH-.13),Math.sin(a)*SWEEP_REACH,Math.cos(a)*SWEEP_REACH,
					Math.sin(c)*SWEEP_REACH,Math.cos(c)*SWEEP_REACH,Math.sin(c)*(SWEEP_REACH-.13),Math.cos(c)*(SWEEP_REACH-.13),danger);
			}
			for(double edge:new double[]{s.aim-SWEEP_HALF_ANGLE,s.aim+SWEEP_HALF_ANGLE}) line(p,b,0,0,Math.sin(edge)*SWEEP_REACH,Math.cos(edge)*SWEEP_REACH,.09,danger);
		} else if(s.move==ANCHOR) {
			anchorRing(p,b,s.markX,s.markZ,danger);
			if(s.paired) anchorRing(p,b,s.markX2,s.markZ2,danger);
		} else {
			for(int sign:new int[]{-1,1}) {
				double side=sign*SAFE_HALF_WIDTH;
				if(s.acrossX) line(p,b,-ARENA,side,ARENA,side,.10,safe); else line(p,b,side,-ARENA,side,ARENA,.10,safe);
			}
			// Repeated crosses cannot be confused with parallel deck grain; the safe centre stays visibly empty.
			for(double side=-12;side<=12;side+=3) if(Math.abs(side)>SAFE_HALF_WIDTH) {
				for(double along=-12;along<=12;along+=3) {
					double x=s.acrossX?along:side,z=s.acrossX?side:along;
					line(p,b,x-.35,z-.35,x+.35,z+.35,.085,danger);
					line(p,b,x-.35,z+.35,x+.35,z-.35,.085,danger);
				}
			}
		}
	}
	private static void anchorRing(PoseStack.Pose p,VertexConsumer b,double mx,double mz,int color) {
		for(int i=0;i<40;i++) {
			double a=i*Math.PI*2/40,c=(i+1)*Math.PI*2/40;
			line(p,b,mx+Math.sin(a)*ANCHOR_RADIUS,mz+Math.cos(a)*ANCHOR_RADIUS,mx+Math.sin(c)*ANCHOR_RADIUS,mz+Math.cos(c)*ANCHOR_RADIUS,.10,color);
		}
		line(p,b,mx-.6,mz,mx+.6,mz,.08,color); line(p,b,mx,mz-.6,mx,mz+.6,.08,color);
	}
	private static void line(PoseStack.Pose p,VertexConsumer b,double x0,double z0,double x1,double z1,double width,int color) {
		double length=Math.hypot(x1-x0,z1-z0); if(length<.0001)return;
		double nx=-(z1-z0)/length*width,nz=(x1-x0)/length*width;
		quad(p,b,x0-nx,z0-nz,x1-nx,z1-nz,x1+nx,z1+nz,x0+nx,z0+nz,color);
	}
	private static void quad(PoseStack.Pose p,VertexConsumer b,double x0,double z0,double x1,double z1,double x2,double z2,double x3,double z3,int color) {
		vertex(p,b,x0,z0,0,0,color);vertex(p,b,x1,z1,1,0,color);vertex(p,b,x2,z2,1,1,color);vertex(p,b,x3,z3,0,1,color);
	}
	private static void vertex(PoseStack.Pose p,VertexConsumer b,double x,double z,float u,float v,int color) {
		b.addVertex(p,(float)x,.045F,(float)z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p,0,1,0);
	}
}
