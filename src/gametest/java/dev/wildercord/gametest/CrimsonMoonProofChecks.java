package dev.wildercord.gametest;

import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.gametest.CrimsonMoonRenderMath.*;
import org.joml.Matrix4f;
import java.util.*;

/** Source-side mutation checks only; synthetic vectors and receipts never count as native screenshot evidence. */
public final class CrimsonMoonProofChecks {
    private static int checked;
    private static void check(boolean value,String reason){checked++;if(!value)throw new AssertionError(reason);}
    public static void main(String[] args){
        Map<String,Part> baseline=new LinkedHashMap<>();
        baseline.put("head",part(0,0,0,.31F,-.77F,.08F));
        baseline.put("body",part(0,0,0,.02F,.1F,.01F));
        baseline.put("leftArm",part(5.1F,2.2F,.2F,-.43F,.2F,.13F));
        baseline.put("rightArm",part(-5.1F,1.8F,-.2F,-.71F,-.18F,-.12F));
        baseline.put("leftLeg",part(1.9F,12,0,.1F,0,0));
        baseline.put("rightLeg",part(-1.9F,12,0,-.1F,0,0));
        for(boolean left:new boolean[]{false,true})for(boolean slim:new boolean[]{false,true})for(float age:new float[]{.1F,6.5F,10,14,27,29.5F}){
            var pose=ArticulatedCombatPose.samplePlayer(19,age,10,20,left);
            var expected=CrimsonMoonRenderMath.body(pose,baseline,slim);
            var stale=new EnumMap<>(expected);
            var joint=left?ArticulatedCombatPose.Joint.LEFT_FOREARM:ArticulatedCombatPose.Joint.RIGHT_FOREARM;
            var arm=stale.get(joint);stale.put(joint,arm.rotation(arm.rx()+.3F,arm.ry(),arm.rz()));
            check(!CrimsonMoonRenderMath.compare("stale_joint",CrimsonMoonRenderMath.values(expected),CrimsonMoonRenderMath.values(stale)).matched(),"Stale live joint must fail");
            var view=ArticulatedCombatPose.view(pose,left);var viewParts=CrimsonMoonRenderMath.palette(view::local);
            var actualSocket=CrimsonMoonRenderMath.socket(viewParts,left,slim);
            var wrongSocket=CrimsonMoonRenderMath.socket(viewParts,!left,slim);
            check(!CrimsonMoonRenderMath.compare("wrong_mirrored_socket",CrimsonMoonRenderMath.values(actualSocket),CrimsonMoonRenderMath.values(wrongSocket)).matched(),"Other hand's socket must fail");
            var wrongWidth=CrimsonMoonRenderMath.socket(viewParts,left,!slim);
            check(!CrimsonMoonRenderMath.compare("wrong_width",CrimsonMoonRenderMath.values(actualSocket),CrimsonMoonRenderMath.values(wrongWidth)).matched(),"Wrong width's centre shift must fail");
            if(pose.weight()>0&&pose.weight()<1)
                check(!CrimsonMoonRenderMath.compare("missing_vanilla_edge_blend",CrimsonMoonRenderMath.values(expected),
                    CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.palette(pose::local))).matched(),"Raw pose alone cannot prove partial-weight world geometry");
            var classic=MastersArtAnimation.sample(19,age,10,20);
            check(!CrimsonMoonRenderMath.compare("skipped_classic_body",CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.classic(classic,left,baseline)),
                CrimsonMoonRenderMath.values(baseline)).matched(),"Vanilla body alone cannot prove Classic motion");
            var item=CrimsonMoonRenderMath.nativeSword(new Matrix4f(),classic,left,.4F,17,-9,classic.weight(),.45F,true);
            var skipped=CrimsonMoonRenderMath.nativeSword(new Matrix4f(),null,left,.4F,17,-9,0,.45F,true);
            check(!CrimsonMoonRenderMath.compare("skipped_classic_fp",CrimsonMoonRenderMath.values(item),CrimsonMoonRenderMath.values(skipped)).matched(),"Native sword submission without Classic transform must fail");
        }
        for(boolean left:new boolean[]{false,true}){
            var idle=new Identity(Long.MIN_VALUE,-1,left,false,0,0,0,false,"NaN","idle");
            check(CrimsonMoonRenderMath.neutral(idle,left,0,ArticulatedCombatPose.Phase.NONE,ArticulatedCombatPose.NONE::local),"Authoritative idle metadata and NONE locals pass");
            var stale=ArticulatedCombatPose.samplePlayer(19,14,10,20,left);
            check(!CrimsonMoonRenderMath.neutral(idle,left,0,ArticulatedCombatPose.Phase.NONE,stale::local),"Stale non-neutral locals labeled NONE must fail");
            check(!CrimsonMoonRenderMath.neutral(idle,left,.01F,ArticulatedCombatPose.Phase.NONE,ArticulatedCombatPose.NONE::local),"Nonzero idle weight must fail");
            check(!CrimsonMoonRenderMath.neutral(idle,left,0,ArticulatedCombatPose.Phase.RECOVERY,ArticulatedCombatPose.NONE::local),"Non-NONE idle phase must fail");
            for(var bad:List.of(new Identity(501,-1,left,false,0,0,0,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,19,left,false,0,0,0,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,!left,false,0,0,0,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,left,true,0,0,0,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,left,false,1,0,0,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,left,false,0,1,0,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,left,false,0,0,1,false,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,left,false,0,0,0,true,"NaN","idle"),
                new Identity(Long.MIN_VALUE,-1,left,false,0,0,0,false,"0x0.0p0","idle")))
                check(!CrimsonMoonRenderMath.neutral(bad,left,0,ArticulatedCombatPose.Phase.NONE,ArticulatedCombatPose.NONE::local),"Stale neutral frame metadata must fail");
            var expectedIdle=CrimsonMoonRenderMath.idleView(left);
            var staleView=ArticulatedCombatPose.view(stale,left);
            check(!CrimsonMoonRenderMath.compare("stale_idle_view",CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.palette(expectedIdle::local)),
                CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.palette(staleView::local))).matched(),"Expected idle view is derived from NONE, never from stale actual locals");
        }
        for(boolean left:new boolean[]{false,true})for(float age:new float[]{6.5F,10,14,27}){
            var expected=ArticulatedCombatPose.samplePlayer(19,age,10,20,left);
            var otherCamera=ArticulatedCombatPose.samplePlayer(19,age==6.5F?10:6.5F,10,20,left);
            var wrongViewKey=withCamera(expected,otherCamera);
            check(CrimsonMoonRenderMath.compare("identical_world_locals",CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.palette(expected::local)),
                CrimsonMoonRenderMath.values(CrimsonMoonRenderMath.palette(wrongViewKey::local))).matched(),"The camera-only mutation retains identical world joints");
            check(expected.weight()==wrongViewKey.weight()&&expected.phase()==wrongViewKey.phase(),"The camera-only mutation retains weight and phase");
            check(!CrimsonMoonRenderMath.viewSource(expected,wrongViewKey,left).matched(),"Wrong ViewKey must fail independent authoritative FP comparison");
            check(CrimsonMoonRenderMath.viewSource(expected,expected,left).matched(),"Unchanged authoritative source view passes");
            var idle=new Identity(Long.MIN_VALUE,-1,left,false,0,0,0,false,"NaN","idle");
            check(!CrimsonMoonRenderMath.neutralPose(idle,left,withCamera(ArticulatedCombatPose.NONE,otherCamera)),"A copied NONE label with foreign camera key is not canonical idle");
        }
        check(CrimsonMoonRenderMath.releaseAt(500,510),"Exact server completion+10 passes");
        check(!CrimsonMoonRenderMath.releaseAt(500,509),"Server completion+9 is rejected");
        check(!CrimsonMoonRenderMath.releaseAt(500,511),"Server completion+11 is rejected");
        var identity=new Identity(501,19,false,false,12,-3,0,false,"NaN","pose");
        for(var changed:List.of(
            new Identity(502,19,false,false,12,-3,0,false,"NaN","pose"),
            new Identity(501,18,false,false,12,-3,0,false,"NaN","pose"),
            new Identity(501,19,true,false,12,-3,0,false,"NaN","pose"),
            new Identity(501,19,false,true,12,-3,0,false,"NaN","pose"),
            new Identity(501,19,false,false,13,-3,0,false,"NaN","pose"),
            new Identity(501,19,false,false,12,-2,0,false,"NaN","pose"),
            new Identity(501,19,false,false,12,-3,1,false,"NaN","pose"),
            new Identity(501,19,false,false,12,-3,0,true,"NaN","pose"),
            new Identity(501,19,false,false,12,-3,0,false,"0x0.0p0","pose"),
            new Identity(501,19,false,false,12,-3,0,false,"NaN","stale")))
            check(!CrimsonMoonRenderMath.same(identity,changed),"Changed complete frame identity must fail");
        check(CrimsonMoonRenderMath.same(identity,identity),"Exact immutable identity is accepted");
        check(CrimsonMoonRenderMath.backend("classic",false,false,true,true),"Classic effective subordinate adapters are disabled");
        check(CrimsonMoonRenderMath.backend("whole_fallback",true,false,true,true),"Only isolated funded-shell adapter failure is accepted");
        check(!CrimsonMoonRenderMath.backend("whole_fallback",false,false,true,true),"Global articulation disable cannot masquerade as shell fallback");
        check(!CrimsonMoonRenderMath.backend("whole_fallback",true,true,true,true),"Enabled shell adapter is not the requested fallback");
        check(!CrimsonMoonRenderMath.backend("whole_fallback",true,false,false,true),"Unfunded shell is not the requested fallback");
        check(!CrimsonMoonRenderMath.backend("whole_fallback",true,false,true,false),"Other incompatibility is not the requested fallback");
        check(CrimsonMoonRenderMath.item(7,7,true,true,true),"Exact actual diamond item identity is accepted");
        check(!CrimsonMoonRenderMath.item(7,8,true,true,true),"Changed item state is rejected");
        check(!CrimsonMoonRenderMath.item(7,7,false,true,true),"Wrong weapon is rejected");
        check(!CrimsonMoonRenderMath.item(7,7,true,false,true),"Nonempty offhand is rejected");
        check(!CrimsonMoonRenderMath.item(7,7,true,true,false),"Changed held components are rejected");
        check(!CrimsonMoonRenderMath.compare("nonfinite",List.of(0F),List.of(Float.NaN)).matched(),"Nonfinite geometry is rejected");
        check(CrimsonMoonRenderMath.originalSkin("original","original","wide","wide",true),"Original profile texture and actual material pass");
        check(!CrimsonMoonRenderMath.originalSkin("original","replacement","wide","wide",true),"Same-width replacement skin must fail");
        check(!CrimsonMoonRenderMath.originalSkin("original","original","wide","wide",false),"Wrong submitted material must fail even with correct extracted texture");
        check(!CrimsonMoonRenderMath.originalSkin("original","original","wide","slim",true),"Wrong original profile width must fail");
        check(CrimsonMoonRenderMath.modelOwned(true,true,true,true,true),"Exact model and shell ownership pass");
        check(!CrimsonMoonRenderMath.modelOwned(false,true,true,true,true),"Custom model cannot masquerade as shell-only fallback");
        check(!CrimsonMoonRenderMath.modelOwned(true,false,true,true,true),"Unowned body cannot masquerade as shell-only fallback");
        check(!CrimsonMoonRenderMath.modelOwned(true,true,false,true,true),"Wrong rig width cannot masquerade as shell-only fallback");
        check(!CrimsonMoonRenderMath.modelOwned(true,true,true,false,true),"Missing owned viewmodel cannot masquerade as shell-only fallback");
        check(!CrimsonMoonRenderMath.modelOwned(true,true,true,true,false),"Another model's shell cannot masquerade as shell-only fallback");
        check(CrimsonMoonRenderMath.handEligible(true,.2F,true,true,false),"Actual same-item attack dip remains eligible");
        check(CrimsonMoonRenderMath.handEligible(false,1,true,true,false),"Settled native idle remains eligible");
        check(!CrimsonMoonRenderMath.handEligible(true,.2F,true,false,false),"Unknown hand provenance cannot explain shell-only fallback");
        check(!CrimsonMoonRenderMath.handEligible(true,.2F,true,true,true),"Genuine equip cannot explain shell-only fallback");
        check(!CrimsonMoonRenderMath.handEligible(true,.2F,false,true,false),"Mismatched held item cannot explain shell-only fallback");
        check(!CrimsonMoonRenderMath.handEligible(false,.2F,true,true,false),"Unsettled neutral hand cannot explain shell-only fallback");
        check(!CrimsonMoonRenderMath.handEligible(true,Float.NaN,true,true,false),"Nonfinite hand height cannot explain shell-only fallback");

        for(float[] window:new float[][]{{6.5F,6,8},{10,10,11},{14,14,18},{27,27,30},{30,30,40}}){
            check(CrimsonMoonRenderMath.age(window[0],window[1]),"Inclusive source-age boundary");
            check(!CrimsonMoonRenderMath.age(window[0],Math.nextDown(window[1])),"Source age before window rejected");
            check(CrimsonMoonRenderMath.age(window[0],Math.nextDown(window[2])),"Last in-window age accepted");
            check(!CrimsonMoonRenderMath.age(window[0],window[2]),"Exclusive source-age boundary");
        }
        check(!CrimsonMoonRenderMath.age(10,11),"Release is strictly [10,11)");
        check(!CrimsonMoonRenderMath.age(14,12),"Old HitStop recovery cannot prove follow-through");
        scopeCleanup();
        System.out.println("PASS "+checked+" source-side Moon mutation checks; no native renderer, screenshot, observer or pixel review was run.");
    }
    /** Simulates owner/getter/cast failures without instantiating Minecraft or its renderers. */
    private static void scopeCleanup(){
        var current=new ThreadLocal<ArrayList<String>>();
        var outer=new ArrayList<>(List.of("outer"));
        for(RuntimeException failure:List.of(new NullPointerException("missing owner"),new ClassCastException("changed renderer"))){
            current.set(outer);var partial=new ArrayList<>(List.of("skin","model"));
            try{CrimsonMoonScopeGuard.initialize(current,partial,()->{throw failure;},ArrayList::clear);throw new AssertionError("Failed entry returned");}
            catch(RuntimeException actual){check(actual==failure,"Initialization preserves the exact original exception");}
            check(current.get()==outer,"Failed entry restores the exact outer scope");
            check(partial.isEmpty(),"Failed entry clears partially captured state");
        }
        current.remove();var partial=new ArrayList<>(List.of("partial"));var error=new AssertionError("owner getter failed");
        try{CrimsonMoonScopeGuard.initialize(current,partial,()->{throw error;},ArrayList::clear);throw new AssertionError("Failed error entry returned");}
        catch(AssertionError actual){check(actual==error,"Initialization preserves the exact original Error");}
        check(current.get()==null&&partial.isEmpty(),"Failure with no outer scope leaves no current scope or partial state");
        current.set(outer);partial.add("partial");var primary=new IllegalStateException("initialization failed");var cleanup=new AssertionError("cleanup failed");
        try{CrimsonMoonScopeGuard.initialize(current,partial,()->{throw primary;},state->{state.clear();throw cleanup;});throw new AssertionError("Cleanup failure returned");}
        catch(IllegalStateException actual){check(actual==primary,"Cleanup failure does not replace the original exception");
            check(actual.getSuppressed().length==1&&actual.getSuppressed()[0]==cleanup,"Cleanup failure remains diagnostic context");}
        check(current.get()==outer&&partial.isEmpty(),"Outer scope is restored even when cleanup throws");
        var successful=new ArrayList<>(List.of("ready"));
        check(CrimsonMoonScopeGuard.initialize(current,successful,()->successful.add("initialized"),ArrayList::clear)==successful,
            "Successful entry returns the installed scope");
        check(current.get()==successful&&successful.size()==2,"Successful initialization retains its new scope and data");
        CrimsonMoonScopeGuard.restore(current,outer);check(current.get()==outer,"Normal outer restoration remains exact");
        CrimsonMoonScopeGuard.restore(current,null);check(current.get()==null,"Normal empty restoration removes the current scope");
    }
    /** Detached pure test pose only: never mutates NONE, production frames, models, players or render state. */
    private static ArticulatedCombatPose.Pose withCamera(ArticulatedCombatPose.Pose world,ArticulatedCombatPose.Pose cameraSource){
        try{
            var camera=ArticulatedCombatPose.Pose.class.getDeclaredField("camera");camera.setAccessible(true);
            var constructor=ArticulatedCombatPose.Pose.class.getDeclaredConstructor(float.class,ArticulatedCombatPose.Phase.class,
                ArticulatedCombatPose.Transform[].class,camera.getType());constructor.setAccessible(true);
            var joints=ArticulatedCombatPose.Joint.values();var locals=new ArticulatedCombatPose.Transform[joints.length];
            for(int i=0;i<joints.length;i++)locals[i]=world.local(joints[i]);
            return constructor.newInstance(world.weight(),world.phase(),locals,camera.get(cameraSource));
        }catch(ReflectiveOperationException failure){throw new AssertionError("Could not build the detached camera-key mutation",failure);}
    }
    private static Part part(float x,float y,float z,float rx,float ry,float rz){return new Part(x,y,z,rx,ry,rz,1,1,1,x==5.1F?5:x==-5.1F?-5:x,y,z);}
}

