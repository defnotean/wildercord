package dev.wildercord.gametest;

import dev.wildercord.aura.*;
import dev.wildercord.client.combat.*;
import dev.wildercord.gametest.CounterPeerRenderMath.Part;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import java.util.*;

/** Original-runtime CPU checks only. A no-op/idle adapter cannot satisfy the new numeric receipt. */
public final class CheckCounterPeerEvidence {
    static int checks;
    public static void main(String[] ignored) throws Exception {
        var baked=net.minecraft.client.model.geom.EntityModelSet.vanilla();
        for(int move:new int[]{24,25}){
            int w=move==24?6:4,r=move==24?16:14;String art=move==24?"unmoved":"null_parry";
            for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-1,w+r})require(CounterPeerPhaseContract.phase(move,bad).equals("NONE"),"Invalid/expired phase");
            require(CounterPeerPhaseContract.phase(move,Math.nextDown((float)w)).equals("WINDUP"),"Exact windup upper boundary");
            require(CounterPeerPhaseContract.phase(move,w).equals("ACTIVE")&&CounterPeerPhaseContract.phase(move,Math.nextDown(w+1F)).equals("ACTIVE"),"Exact one-tick ACTIVE");
            require(CounterPeerPhaseContract.phase(move,w+1).equals("FOLLOW"),"No late ACTIVE labeling");
            for(boolean left:new boolean[]{false,true})for(boolean slim:new boolean[]{false,true}){
                String hand=left?"LEFT":"RIGHT";
                for(float age:new float[]{2.5F,w+.5F,w+2.5F,w+r/2F+.5F}){
                    String phase=CounterPeerPhaseContract.phase(move,age);
                    var body=ArticulatedCombatPose.samplePlayer(move,age,w,r,left);var view=ArticulatedCombatPose.view(body,left);
                    var rig=new ArticulatedRig(slim,false);
                    require(!compare(view::local,rig),"No-op articulated view cannot pass "+move+" "+phase);
                    var model=new ArticulatedViewModel(slim);model.setupAnim(new ArticulatedViewModel.Frame(view,true,true));
                    require(compare(view::local,model.rig()),"Production deferred view installs exact original source palette");
                    var before=locals(model.rig());model.setupAnim(new ArticulatedViewModel.Frame(ArticulatedCombatPose.view(ArticulatedCombatPose.NONE,left),true,true));
                    require(!CounterPeerRenderMath.compare("stale_idle_view",before,locals(model.rig())).matched(),"Actual idle adapter cannot masquerade as accepted art");
                    rig.apply(body::local);require(compare(body::local,rig),"Production rig installs exact original body palette");
                    var exact=locals(rig);rig.part(ArticulatedCombatPose.Joint.RIGHT_SOCKET).x+=.01F;
                    require(!CounterPeerRenderMath.compare("shifted_socket",exact,locals(rig)).matched(),"Actual socket mutation fails");
                    var palette=CounterPeerRenderMath.palette(view::local);var handMatrix=CounterPeerRenderMath.sword(new Matrix4f(),palette,left,slim);
                    require(!CounterPeerRenderMath.compare("noop_item",CounterPeerRenderMath.values(handMatrix),CounterPeerRenderMath.values(new Matrix4f())).matched(),"No-op articulated item transform fails");
                    var classic=MastersArtAnimation.sample(move,age,w,r);var vanilla=vanilla();var expected=CounterPeerRenderMath.classic(classic,left,vanilla);
                    require(!CounterPeerRenderMath.compare("noop_classic_body",CounterPeerRenderMath.values(expected),CounterPeerRenderMath.values(vanilla)).matched(),"Finite idle Classic body must fail");
                    float tilt=MastersArtAnimation.bladeTilt(move,age,w,classic.weight());
                    var original=new net.minecraft.client.model.player.PlayerModel(baked.bakeLayer(slim?net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM:net.minecraft.client.model.geom.ModelLayers.PLAYER),slim);
                    var arm=left?original.leftArm:original.rightArm;var source=expected.get(left?"leftArm":"rightArm");
                    arm.setPos(source.x(),source.y(),source.z());arm.setRotation(source.rx(),source.ry(),source.rz());
                    var nativeHand=new com.mojang.blaze3d.vertex.PoseStack();original.translateToHand(null,left?net.minecraft.world.entity.HumanoidArm.LEFT:net.minecraft.world.entity.HumanoidArm.RIGHT,nativeHand);
                    nativeHand.rotateDegrees(com.mojang.math.Axis.XP,-90);nativeHand.rotateDegrees(com.mojang.math.Axis.YP,180);nativeHand.translate((left?-1:1)/16F,.125F,-.625F);
                    var unturned=new Matrix4f(nativeHand.last().pose());nativeHand.translate(0,-1.327F/16,1.439F/16);nativeHand.rotateDegrees(com.mojang.math.Axis.XP,tilt);nativeHand.translate(0,1.327F/16,-1.439F/16);
                    var worldItem=CounterPeerRenderMath.classicSword(new Matrix4f(),expected,left,slim,tilt);
                    require(CounterPeerRenderMath.compare("original_vanilla_hand_and_hilt",CounterPeerRenderMath.values(worldItem),CounterPeerRenderMath.values(nativeHand.last().pose())).matched(),"Classic world oracle matches original native hand transform and authored hilt");
                    if(Math.abs(tilt)>.001)require(!CounterPeerRenderMath.compare("noop_hilt",CounterPeerRenderMath.values(worldItem),CounterPeerRenderMath.values(unturned)).matched(),"Omitted actual hilt rotation fails");
                    var transform=CounterPeerRenderMath.classicDelta(classic,left,0,0,0);
                    require(!CounterPeerRenderMath.compare("noop_classic_view",CounterPeerRenderMath.values(transform),CounterPeerRenderMath.values(new Matrix4f())).matched(),"No-op Classic view must fail");
                    var sword=CounterPeerRenderMath.nativeSword(new Matrix4f(),classic,left,0,0,0,1,0,false);
                    var idle=CounterPeerRenderMath.nativeSword(new Matrix4f(),null,left,0,0,0,0,0,false);
                    require(!CounterPeerRenderMath.compare("noop_classic_item",CounterPeerRenderMath.values(sword),CounterPeerRenderMath.values(idle)).matched(),"Idle finite Classic item must fail");
                    for(String mode:List.of("classic","articulated"))for(String role:List.of("host","peer")){
                        String name="counter_peer_"+art+"_"+mode+"_"+hand.toLowerCase(Locale.ROOT)+"_"+role+"_"+phase.toLowerCase(Locale.ROOT);
                        require(CounterPeerPhaseContract.expected(name,move,w,r,mode,hand,phase,role,role.equals("host")?"fp":"remote"),"Exact native screenshot identity");
                        require(!CounterPeerPhaseContract.expected(name,move,w,r,mode,hand,phase,role,role.equals("host")?"remote":"fp"),"Foreign camera role fails");
                        require(!CounterPeerPhaseContract.expected(name,move,w+1,r,mode,hand,phase,role,role.equals("host")?"fp":"remote"),"Timing mutation fails");
                    }
                }
            }
        }
        for(boolean left:new boolean[]{false,true})for(boolean world:new boolean[]{false,true}){
            var rotation=new org.joml.Vector3f(0,left?90:-90,(left?-1:1)*(world?55:25));
            var translation=world?new org.joml.Vector3f(0,4F/16,.5F/16):new org.joml.Vector3f(1.13F/16,3.2F/16,1.13F/16);
            var scale=new org.joml.Vector3f(world?.85F:.68F);var transform=new net.minecraft.client.resources.model.cuboid.ItemTransform(rotation,translation,scale);
            var actual=new com.mojang.blaze3d.vertex.PoseStack();transform.apply(left,actual.last());
            var expected=CounterPeerRenderMath.display(new Matrix4f(),left,rotation,translation,scale,false,new Matrix4f());
            require(CounterPeerRenderMath.compare("original_item_display",CounterPeerRenderMath.values(expected),CounterPeerRenderMath.values(actual.last().pose())).matched(),"Independent display oracle matches original ItemTransform");
            require(!CounterPeerRenderMath.compare("noop_display",CounterPeerRenderMath.values(expected),CounterPeerRenderMath.values(new Matrix4f())).matched(),"No-op deep display transform fails");
            var hilt=new org.joml.Vector3f(3.5F/16,3.5F/16,.5F);
            require(CounterPeerRenderMath.compare("displayed_hilt",CounterPeerRenderMath.point(expected,hilt),CounterPeerRenderMath.point(actual.last().pose(),hilt)).matched(),"Actual displayed original diamond hilt");
        }
        for(int move:new int[]{24,25}){
            int w=move==24?6:4,r=move==24?16:14;var original=new Object();
            var latch=new CounterPeerSourceLatch(11,move,w,r);latch.received(original,11,move,100,w,r,5);
            require(latch.require(original,100)==5,"First native received object survives phase one");
            require(latch.require(original,100)==5,"Same exact object survives later phases");
            rejected(()->latch.require(new Object(),100),"Equal-valued replacement identity cannot pass");
            rejected(()->latch.require(original,100),"Replaced-source rejection is sticky");
            var overwritten=new CounterPeerSourceLatch(11,move,w,r);
            rejected(()->overwritten.received(new Object(),11,move==24?25:24,100,w,r,5),"Wrong first source before polling is rejected");
            rejected(()->overwritten.received(original,11,move,100,w,r,6),"Later correct packet cannot rescue a wrong first source");
            rejected(()->overwritten.require(original,100),"Later correct timeline cannot rescue first-source rejection");
            var duplicate=new CounterPeerSourceLatch(11,move,w,r);duplicate.received(original,11,move,100,w,r,5);
            rejected(()->duplicate.received(original,11,move,100,w,r,6),"Even repeated same object is a duplicate native receive");
            var reissued=new CounterPeerSourceLatch(11,move,w,r);reissued.received(original,11,move,100,w,r,5);
            rejected(()->reissued.received(new Object(),11,move,100,w,r,6),"Same-valued repeated packet object fails");
            var absent=new CounterPeerSourceLatch(11,move,w,r);
            rejected(()->absent.require(original,100),"Polling an unobserved source is never enough");
        }
        var ledger=java.nio.file.Files.createTempDirectory("counter-peer-failure-ledger-");
        try{
            var identity=Map.of("role","host","nonce","00000000-0000-4000-8000-000000000001","sourceHead","a".repeat(40),"pid","1","runIdentity","pure-ledger-control");
            var terminal=ledger.resolve("host-cast-receipt-passed.properties");java.nio.file.Files.writeString(terminal,"cases=synthetic-already-passed\n");
            CounterPeerFailureLedger.reject(ledger,identity,"already-persisted-phase","late duplicate readback");
            var marker=ledger.resolve("host-counter-render-failure.properties");byte[] first=java.nio.file.Files.readAllBytes(marker);
            require(new String(first,java.nio.charset.StandardCharsets.ISO_8859_1).contains("late duplicate readback"),"Late rejection is durably visible after prior terminal success");
            CounterPeerFailureLedger.reject(ledger,identity,"later-valid-frame","another rejection");
            require(Arrays.equals(first,java.nio.file.Files.readAllBytes(marker)),"First rejection cannot be overwritten or rescued");
            require(java.nio.file.Files.readString(terminal).contains("already-passed"),"Rejection does not edit earlier evidence");
            java.nio.file.Files.delete(marker);java.nio.file.Files.writeString(marker,"foreign or malformed rejection");
            CounterPeerFailureLedger.reject(ledger,identity,"valid-frame","later rejection");
            require(java.nio.file.Files.readString(marker).equals("foreign or malformed rejection"),"Existing malformed rejection remains fatal and untouched");
            var terminated=new java.util.concurrent.atomic.AtomicBoolean();
            rejected(()->CounterPeerFailureLedger.reject(ledger.resolve("missing"),identity,"late","write denied",problem->terminated.set(true)),"Missing failure destination cannot return success");
            require(terminated.get(),"Persistence failure invokes isolated-JVM termination policy");
        }finally{try(var children=java.nio.file.Files.list(ledger)){for(var child:children.toList())java.nio.file.Files.delete(child);}java.nio.file.Files.delete(ledger);}
        for(long[] seq:new long[][]{{0,2,3},{1,1,3},{2,1,3},{1,3,3},{1,4,3}})require(!CounterPeerPhaseContract.sourceAfterAcceptance(seq[0],seq[1],seq[2]),"Missing/stale/late source fails");
        require(CounterPeerPhaseContract.sourceAfterAcceptance(1,2,3),"Actual immutable source after accepted receipt");
        require(!CounterPeerRenderMath.compare("nonfinite",List.of(1F),List.of(Float.NaN)).matched(),"Nonfinite geometry fails");
        require(!CounterPeerRenderMath.compare("missing",List.of(1F),List.of()).matched(),"Missing geometry fails");
        System.out.println("{\"checks\":"+checks+",\"passed\":true,\"native\":\"not run\",\"scope\":\"original-runtime palette replay and no-op/idle geometry negatives\"}");
    }
    static boolean compare(java.util.function.Function<ArticulatedCombatPose.Joint,ArticulatedCombatPose.Transform> expected,ArticulatedRig rig){return CounterPeerRenderMath.compare("original_palette",CounterPeerRenderMath.values(CounterPeerRenderMath.palette(expected)),locals(rig)).matched();}
    static List<Float> locals(ArticulatedRig rig){var out=new EnumMap<ArticulatedCombatPose.Joint,Part>(ArticulatedCombatPose.Joint.class);for(var j:ArticulatedCombatPose.Joint.values()){ModelPart p=rig.part(j);out.put(j,new Part(p.x,p.y,p.z,p.xRot,p.yRot,p.zRot,p.xScale,p.yScale,p.zScale,0,0,0));}return CounterPeerRenderMath.values(out);}
    static Map<String,Part> vanilla(){var out=new LinkedHashMap<String,Part>();for(String n:List.of("head","body","leftArm","rightArm","leftLeg","rightLeg")){
        float x=n.equals("leftArm")?5:n.equals("rightArm")?-5:n.equals("leftLeg")?1.9F:n.equals("rightLeg")?-1.9F:0,y=n.contains("Leg")?12:n.contains("Arm")?2:0;
        out.put(n,new Part(x,y,0,0,0,0,1,1,1,x,y,0));}return out;}
    static void rejected(Runnable call,String reason){boolean denied=false;try{call.run();}catch(AssertionError expected){denied=true;}require(denied,reason);}
    static void require(boolean yes,String reason){checks++;if(!yes)throw new AssertionError(reason);}
}
