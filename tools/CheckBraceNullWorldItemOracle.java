import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.gametest.BraceNullTransformOracle;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Original runtime model/ItemTransform cross-checks. No Minecraft client, mixin or GPU is run. */
public final class CheckBraceNullWorldItemOracle {
	private static int checks;
	public static void main(String[] args) {
		var baked = EntityModelSet.vanilla();
		for (int move : new int[] {24, 25}) for (boolean slim : new boolean[] {false,true}) for (boolean left : new boolean[] {false,true}) {
			var rule = MastersStyleRules.animation(move);
			for (float age : new float[] {rule.windup() / 2F + .5F, rule.windup() + .5F, rule.windup() + rule.recovery() - 3.5F}) {
				var model = new PlayerModel(baked.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
				float[] baseline = values(model); var pose = MastersArtAnimation.sample(move, age, rule.windup(), rule.recovery());
				float[] wantedBody = BraceNullTransformOracle.body(pose,left,baseline,baseline);
				referenceBody(model,pose,left); BraceNullTransformOracle.requireBody(wantedBody,values(model)); checks++;
				var entry = new Matrix4f().translation(.13F,-.04F,.07F).rotateY(.18F);
				float tilt = MastersArtAnimation.bladeTilt(move,age,rule.windup(),pose.weight());
				float[] expected = BraceNullTransformOracle.worldItem(entry.get(new float[16]),wantedBody,left,slim,tilt);
				var actual = new PoseStack(); actual.last().pose().set(entry);
				model.translateToHand(null,left?HumanoidArm.LEFT:HumanoidArm.RIGHT,actual);
				actual.rotateDegrees(Axis.XP,-90); actual.rotateDegrees(Axis.YP,180); actual.translate((left?-1:1)/16F,2F/16,-10F/16);
				var unadjusted = new Matrix4f(actual.last().pose());
				actual.translate(0,-1.327F/16,1.439F/16); actual.rotateDegrees(Axis.XP,tilt); actual.translate(0,1.327F/16,-1.439F/16);
				BraceNullTransformOracle.requireHand(expected,actual.last().pose().get(new float[16])); checks++;
				if (move==24) reject(() -> BraceNullTransformOracle.requireHand(expected,unadjusted.get(new float[16])));
				var display = new ItemTransform(new Vector3f(0,left?90:-90,left?-55:55),new Vector3f(0,4F/16,.5F/16),new Vector3f(.85F));
				display.apply(left,actual.last());
				float[] shown = BraceNullTransformOracle.displayed(expected,false,left);
				BraceNullTransformOracle.requireDisplayed(shown,actual.last().pose().get(new float[16])); checks++;
				reject(() -> BraceNullTransformOracle.requireDisplayed(shown,expected));
				reject(() -> BraceNullTransformOracle.requireDisplayed(shown,new Matrix4f().get(new float[16])));
			}
		}
		for (boolean left : new boolean[] {false,true}) {
			float[] pre = new Matrix4f().translation(.1F,-.2F,-.7F).rotateY(.4F).get(new float[16]);
			var actual = new PoseStack(); actual.last().pose().set(pre);
			new ItemTransform(new Vector3f(0,left?90:-90,left?-25:25),new Vector3f(1.13F/16,3.2F/16,1.13F/16),new Vector3f(.68F)).apply(left,actual.last());
			BraceNullTransformOracle.requireDisplayed(BraceNullTransformOracle.displayed(pre,true,left),actual.last().pose().get(new float[16])); checks++;
		}
		System.out.println("Original-runtime body/hand/display/hilt oracle checks passed="+checks+"; no native client or GPU acceptance");
	}
	private static ModelPart[] parts(PlayerModel p) { return new ModelPart[] {p.body,p.head,p.rightArm,p.leftArm,p.rightLeg,p.leftLeg}; }
	private static float[] values(PlayerModel p) { float[] out=new float[36];int at=0;for(var part:parts(p))for(float v:new float[]{part.x,part.y,part.z,part.xRot,part.yRot,part.zRot})out[at++]=v;return out; }
	private static void referenceBody(PlayerModel m,MastersArtAnimation.Pose p,boolean left) {
		float w=p.weight(),side=left?-1:1; joint(m.body,p.body(),side,w);joint(left?m.leftArm:m.rightArm,p.sword(),side,w);joint(left?m.rightArm:m.leftArm,p.guard(),side,w);
		joint(left?m.rightLeg:m.leftLeg,p.frontLeg(),side,w);joint(left?m.leftLeg:m.rightLeg,p.rearLeg(),side,w);
		m.head.xRot+=p.head().x()*w;m.head.yRot+=p.head().y()*side*w;m.head.zRot+=p.head().z()*side*w;
		var h=MastersArtAnimation.boundedHead(new MastersArtAnimation.Joint(m.body.xRot,m.body.yRot,m.body.zRot),new MastersArtAnimation.Joint(m.head.xRot,m.head.yRot,m.head.zRot),w);
		m.head.xRot=h.x();m.head.yRot=h.y();m.head.zRot=h.z();
		for(var part:new ModelPart[]{m.body,m.head,m.rightArm,m.leftArm}) {var i=part.getInitialPose();var t=MastersArtAnimation.pivot(p,i.x(),i.y(),i.z(),left);part.x+=w*(t.x()-part.x);part.y+=w*(t.y()-part.y);part.z+=w*(t.z()-part.z);}
		for(var part:new ModelPart[]{m.rightLeg,m.leftLeg}){var i=part.getInitialPose();part.y+=w*(i.y()+p.lower()-part.y);part.z+=w*(i.z()+p.forward()-part.z);}
	}
	private static void joint(ModelPart p,MastersArtAnimation.Joint j,float s,float w){p.xRot+=w*(j.x()-p.xRot);p.yRot+=w*(j.y()*s-p.yRot);p.zRot+=w*(j.z()*s-p.zRot);}
	private static void reject(Runnable test){try{test.run();}catch(AssertionError expected){checks++;return;}throw new AssertionError("Invalid/no-op item transform accepted");}
}
