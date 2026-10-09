package dev.wildercord.gametest;

import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersViewMotion;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.Function;

/** Independent expectations over copied numbers. No live model, state, or stack is mutated. */
public final class CrimsonMoonRenderMath {
    private CrimsonMoonRenderMath() {}
    public static final float EPSILON = .00025F;
    public record Part(float x,float y,float z,float rx,float ry,float rz,float sx,float sy,float sz,float ix,float iy,float iz) {
        Part position(float a,float b,float c){return new Part(a,b,c,rx,ry,rz,sx,sy,sz,ix,iy,iz);}
        Part rotation(float a,float b,float c){return new Part(x,y,z,a,b,c,sx,sy,sz,ix,iy,iz);}
        Matrix4f matrix(){return new Matrix4f().translation(x/16,y/16,z/16).rotateZYX(rz,ry,rx).scale(sx,sy,sz);}
    }
    public record Comparison(String operation,List<Float> expected,List<Float> actual,float maxError,boolean matched) {
        public Comparison { expected=List.copyOf(expected);actual=List.copyOf(actual); }
    }
    public record Identity(long activation,int move,boolean left,boolean master,float yaw,float pitch,float tilt,
        boolean footwork,String velocity,String pose) {}
    public static boolean same(Identity a,Identity b){return Objects.equals(a,b);}
    /** The idle contract is authoritative NONE, never a phase label on an otherwise unchecked pose. */
    public static boolean neutral(Identity identity,boolean left,float weight,ArticulatedCombatPose.Phase phase,
        Function<ArticulatedCombatPose.Joint,ArticulatedCombatPose.Transform> local){
        if(identity==null||identity.activation()!=Long.MIN_VALUE||identity.move()!=-1||identity.left()!=left||identity.master()
            ||identity.yaw()!=0||identity.pitch()!=0||identity.tilt()!=0||identity.footwork()||!identity.velocity().equals("NaN")
            ||weight!=0||phase!=ArticulatedCombatPose.Phase.NONE)return false;
        for(var joint:ArticulatedCombatPose.Joint.values())if(!Objects.equals(local.apply(joint),ArticulatedCombatPose.NONE.local(joint)))return false;
        return true;
    }
    public static ArticulatedCombatPose.ViewPose idleView(boolean left){return ArticulatedCombatPose.view(ArticulatedCombatPose.NONE,left);}
    public static boolean neutralPose(Identity identity,boolean left,ArticulatedCombatPose.Pose pose){
        return pose==ArticulatedCombatPose.NONE&&neutral(identity,left,pose.weight(),pose.phase(),pose::local);
    }
    /** Camera keys are independent of world locals. Compare their observable view against the authoritative sample. */
    public static Comparison viewSource(ArticulatedCombatPose.Pose expected,ArticulatedCombatPose.Pose observed,boolean left){
        return compare("authoritative_source_view",viewValues(ArticulatedCombatPose.view(expected,left)),viewValues(ArticulatedCombatPose.view(observed,left)));
    }
    private static List<Float> viewValues(ArticulatedCombatPose.ViewPose view){
        var result=new ArrayList<>(values(palette(view::local)));result.add(view.weight());result.add((float)view.phase().ordinal());
        result.add(view.origin().x());result.add(view.origin().y());result.add(view.origin().z());return result;
    }
    public static boolean releaseAt(long accepted,long performed){return performed==accepted+10;}
    public static boolean originalSkin(String originalTexture,String extractedTexture,String originalModel,String extractedModel,boolean materialMatches){
        return Objects.equals(originalTexture,extractedTexture)&&Objects.equals(originalModel,extractedModel)&&materialMatches;
    }
    public static boolean modelOwned(boolean exactClass,boolean bodyOwned,boolean widthMatches,boolean viewOwned,boolean shellOwned){
        return exactClass&&bodyOwned&&widthMatches&&viewOwned&&shellOwned;
    }
    public static boolean handEligible(boolean active,float height,boolean sameItem,boolean known,boolean equipping){
        return known&&!equipping&&MastersViewMotion.articulatedHandAdmission(active,height,sameItem,known,equipping).admitted();
    }



    public static boolean backend(String mode,boolean articulated,boolean shellAdapter,boolean shell,boolean compatible){
        return compatible && switch(mode){
            case "articulated"->articulated&&shellAdapter;
            case "classic"->!articulated&&!shellAdapter;
            case "whole_fallback"->articulated&&!shellAdapter&&shell;
            default->false;
        };
    }
    public static boolean item(long expected,long actual,boolean diamond,boolean emptyOffhand,boolean sameStack){
        return expected>0&&expected==actual&&diamond&&emptyOffhand&&sameStack;
    }
    public static boolean age(float requested,float actual){
        float max=requested<10?8:requested==10?11:requested==14?18:requested==27?30:40;
        return Float.isFinite(actual)&&actual>=Math.floor(requested)&&actual<max;
    }
    public static Comparison compare(String operation,List<Float> expected,List<Float> actual){
        boolean valid=!expected.isEmpty()&&expected.size()==actual.size();float error=0;
        for(int i=0;i<Math.min(expected.size(),actual.size());i++){
            float a=expected.get(i),b=actual.get(i);
            if(!Float.isFinite(a)||!Float.isFinite(b))valid=false;else error=Math.max(error,Math.abs(a-b));
        }
        return new Comparison(operation,expected,actual,error,valid&&error<=EPSILON);
    }
    /** Enclosing visibility gates every child even when its own root has no cubes. */
    public static List<Float> outerRoot(Matrix4fc matrix,boolean visible){var out=new ArrayList<>(values(matrix));out.add(visible?1F:0F);return List.copyOf(out);}
    public static List<Float> values(Matrix4fc matrix){var out=new ArrayList<Float>();for(float v:matrix.get(new float[16]))out.add(v);return out;}
    public static List<Float> values(Map<?,Part> parts){
        var out=new ArrayList<Float>();for(var p:parts.values())for(float v:new float[]{p.x,p.y,p.z,p.rx,p.ry,p.rz,p.sx,p.sy,p.sz})out.add(v);return out;
    }
    public static Map<ArticulatedCombatPose.Joint,Part> palette(Function<ArticulatedCombatPose.Joint,ArticulatedCombatPose.Transform> source){
        var out=new EnumMap<ArticulatedCombatPose.Joint,Part>(ArticulatedCombatPose.Joint.class);
        for(var j:ArticulatedCombatPose.Joint.values()){var t=source.apply(j);out.put(j,new Part(t.x(),t.y(),t.z(),t.rotation().x(),t.rotation().y(),t.rotation().z(),1,1,1,t.x(),t.y(),t.z()));}
        return out;
    }
    public static Map<ArticulatedCombatPose.Joint,Part> body(ArticulatedCombatPose.Pose pose,Map<String,Part> vanilla,boolean slim){
        var out=palette(pose::local);float w=pose.weight(),r=1-w;
        var key=ArticulatedCombatPose.Joint.HEAD;var h=out.get(key);var v=vanilla.get("head");
        out.put(key,h.rotation(h.rx+lerp(w,v.rx,clamp(v.rx,-.65F,.65F)),h.ry+lerp(w,v.ry,clamp(v.ry,-1.1F,1.1F)),h.rz+v.rz*r));
        if(r>0)for(boolean left:new boolean[]{false,true}){
            var arm=vanilla.get(left?"leftArm":"rightArm");
            var shoulder=left?ArticulatedCombatPose.Joint.LEFT_SHOULDER:ArticulatedCombatPose.Joint.RIGHT_SHOULDER;
            var p=out.get(shoulder);out.put(shoulder,p.position(p.x+(arm.x-arm.ix)*r,p.y+(arm.y-arm.iy)*r,p.z+(arm.z-arm.iz)*r));
            var upper=left?ArticulatedCombatPose.Joint.LEFT_UPPER_ARM:ArticulatedCombatPose.Joint.RIGHT_UPPER_ARM;p=out.get(upper);
            var blend=ArticulatedCombatPose.Rotation.ZERO.toward(new ArticulatedCombatPose.Rotation(arm.rx,arm.ry,arm.rz),r);
            var q=new Quaternionf().rotationZYX(blend.z(),blend.y(),blend.x());
            var rotation=new Matrix3f().rotationZYX(p.rz,p.ry,p.rx).rotate(q).getEulerAnglesZYX(new Vector3f());
            out.put(upper,p.rotation(rotation.x,rotation.y,rotation.z));
            var socket=left?ArticulatedCombatPose.Joint.LEFT_SOCKET:ArticulatedCombatPose.Joint.RIGHT_SOCKET;p=out.get(socket);
            float x=p.x,y=p.y-.439F*r,z=p.z-.673F*r;
            if(slim){float pivot=left?-.5F:.5F;var c=new Quaternionf(q).conjugate().transform(new Vector3f(pivot,0,0));x+=(c.x-pivot)*r;y+=c.y*r;z+=c.z*r;}
            out.put(socket,p.position(x,y,z));
        }
        return out;
    }
    public static Map<String,Part> classic(MastersArtAnimation.Pose pose,boolean left,Map<String,Part> vanilla){
        var out=new LinkedHashMap<>(vanilla);if(pose==null)return out;float w=pose.weight(),side=left?-1:1;
        out.put("body",joint(out.get("body"),pose.body(),side,w));
        var h=out.get("head");var b=out.get("body");
        var head=MastersArtAnimation.boundedHead(new MastersArtAnimation.Joint(b.rx,b.ry,b.rz),
            new MastersArtAnimation.Joint(h.rx+pose.head().x()*w,h.ry+pose.head().y()*side*w,h.rz+pose.head().z()*side*w),w);
        out.put("head",h.rotation(head.x(),head.y(),head.z()));
        out.put(left?"leftArm":"rightArm",joint(out.get(left?"leftArm":"rightArm"),pose.sword(),side,w));
        out.put(left?"rightArm":"leftArm",joint(out.get(left?"rightArm":"leftArm"),pose.guard(),side,w));
        out.put(left?"rightLeg":"leftLeg",joint(out.get(left?"rightLeg":"leftLeg"),pose.frontLeg(),side,w));
        out.put(left?"leftLeg":"rightLeg",joint(out.get(left?"leftLeg":"rightLeg"),pose.rearLeg(),side,w));
        for(String name:List.of("head","body","leftArm","rightArm")){
            var p=out.get(name);var t=MastersArtAnimation.pivot(pose,p.ix,p.iy,p.iz,left);out.put(name,p.position(lerp(w,p.x,t.x()),lerp(w,p.y,t.y()),lerp(w,p.z,t.z())));
        }
        for(String name:List.of("leftLeg","rightLeg")){var p=out.get(name);out.put(name,p.position(p.x,lerp(w,p.y,p.iy+pose.lower()),lerp(w,p.z,p.iz+pose.forward())));}
        return out;
    }
    private static Part joint(Part p,MastersArtAnimation.Joint j,float side,float w){return p.rotation(lerp(w,p.rx,j.x()),lerp(w,p.ry,j.y()*side),lerp(w,p.rz,j.z()*side));}
    public static Matrix4f classicDelta(MastersArtAnimation.Pose pose,boolean left,float inverse,float yaw,float pitch){
        if(pose==null)return new Matrix4f();var v=MastersArtAnimation.view(pose,left,inverse,yaw,pitch);var t=v.transform();var g=v.grip();
        return new Matrix4f().translation(t.x(),t.y(),t.z()).translate(g.x(),g.y(),g.z()).rotateY(rad(t.yaw())).rotateX(rad(t.pitch())).rotateZ(rad(t.roll())).translate(-g.x(),-g.y(),-g.z());
    }
    public static Matrix4f nativeSword(Matrix4fc entry,MastersArtAnimation.Pose pose,boolean left,float inverse,float yaw,float pitch,float ownership,float attack,boolean whack){
        var out=new Matrix4f(entry).translate(0,MastersViewMotion.heightCompensation(inverse,ownership),0).mul(classicDelta(pose,left,inverse,yaw,pitch))
            .translate(left?-.56F:.56F,-.52F-.6F*inverse,-.72F);
        if(whack)out.mul(MastersViewMotion.fadeSwing(swing(attack,left),ownership));return out;
    }
    private static Matrix4f swing(float attack,boolean left){
        float side=left?-1:1,pi=(float)Math.PI,root=(float)Math.sqrt(attack),a=sin(root*pi),b=sin(attack*attack*pi);
        return new Matrix4f().translation(side*-.4F*a,.2F*sin(root*2*pi),-.2F*sin(attack*pi))
            .rotateY(rad(side*(45-20*b))).rotateZ(rad(-20*side*a)).rotateX(rad(-80*a)).rotateY(rad(-45*side));
    }
    public static Matrix4f viewRoot(Matrix4fc entry,ArticulatedCombatPose.ViewPose pose,float weight,float yaw,float pitch){
        yaw=clamp(yaw,-25,25)*weight;pitch=clamp(pitch,-20,20)*weight;float c=ArticulatedCombatPose.viewClearance(yaw,pitch);var p=pose.origin();
        return new Matrix4f(entry).translate(p.x(),p.y()-.8F*c,p.z()-c).rotateY(rad(-yaw)).rotateX(rad(-pitch)).scale(-1,-1,1);
    }
    public static Matrix4f socket(Map<ArticulatedCombatPose.Joint,Part> parts,boolean left,boolean slim){
        var hand=left?ArticulatedCombatPose.Joint.LEFT_HAND:ArticulatedCombatPose.Joint.RIGHT_HAND;
        var socket=left?ArticulatedCombatPose.Joint.LEFT_SOCKET:ArticulatedCombatPose.Joint.RIGHT_SOCKET;
        return world(parts,hand).translate((left?1:-1)*(slim?.5F:1F)/16,0,0).mul(parts.get(socket).matrix());
    }
    private static Matrix4f world(Map<ArticulatedCombatPose.Joint,Part> parts,ArticulatedCombatPose.Joint joint){return (joint.parent()==null?new Matrix4f():world(parts,joint.parent())).mul(parts.get(joint).matrix());}
    public static Matrix4f sword(Matrix4fc root,Map<ArticulatedCombatPose.Joint,Part> parts,boolean left,boolean slim){return new Matrix4f(root).mul(socket(parts,left,slim)).rotateX(rad(-90)).rotateY(rad(180)).translate(0,1.327F/16,-1.439F/16);}
    private static float lerp(float t,float a,float b){return a+t*(b-a);}
    private static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
    private static float rad(float degrees){return degrees*((float)Math.PI/180);}
    private static float sin(float x){return (float)Math.sin(x);}
}

