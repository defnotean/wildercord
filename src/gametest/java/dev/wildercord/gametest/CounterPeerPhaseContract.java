package dev.wildercord.gametest;

import java.nio.file.*;
import java.util.*;

/** Pair-only immutable receipt and actual post-HitStop phase policy; no clocks or palettes are assigned. */
public final class CounterPeerPhaseContract {
    private CounterPeerPhaseContract() {}
    public static final List<String> PHASES=List.of("WINDUP","ACTIVE","FOLLOW","RECOVERY");
    public static String phase(int move,float age){
        if((move!=24&&move!=25)||!Float.isFinite(age))return "NONE";
        int w=move==24?6:4,r=move==24?16:14;
        return age<0||age>=w+r?"NONE":age<w?"WINDUP":age<w+1?"ACTIVE":age<w+r/2F?"FOLLOW":"RECOVERY";
    }
    public static boolean expected(String name,int move,int windup,int recovery,String mode,String hand,String phase,String role,String view){
        return (move==24||move==25)&&windup==(move==24?6:4)&&recovery==(move==24?16:14)
            &&List.of("classic","articulated").contains(mode)&&List.of("RIGHT","LEFT").contains(hand)&&PHASES.contains(phase)
            &&((role.equals("host")&&view.equals("fp"))||(role.equals("peer")&&view.equals("remote")))
            &&name.equals("counter_peer_"+(move==24?"unmoved":"null_parry")+"_"+mode+"_"+hand.toLowerCase(Locale.ROOT)+"_"+role+"_"+phase.toLowerCase(Locale.ROOT));
    }
    public record Acceptance(String sha256,long accepted) {}
    public static Acceptance readAccepted(Path path,Map<String,String> expected){
        try {
            byte[] bytes=Files.readAllBytes(path);require(bytes.length>0&&bytes.length<16384,"Bounded accepted receipt");
            var p=new Properties();p.load(new java.io.ByteArrayInputStream(bytes));
            for(var e:expected.entrySet())require(e.getValue().equals(p.getProperty(e.getKey())),"Accepted identity: "+e.getKey());
            long accepted=CrimsonMoonClockPacing.tick(p.getProperty("acceptedTick")),caught=CrimsonMoonClockPacing.tick(p.getProperty("caughtTick"));
            require(accepted>=caught&&accepted-caught<=16&&"1".equals(p.getProperty("payments")),"One real finite earned-counter admission");
            require(p.getProperty("armedSha256","").matches("[a-f0-9]{64}"),"Peer armed before payment");
            UUID.fromString(p.getProperty("caughtAttackerUuid"));
            double paid=Double.parseDouble(p.getProperty("paid"));require(Double.isFinite(paid)&&paid>0,"Real positive counter payment");
            return new Acceptance(ArticulatedRenderReceipt.sha256(bytes),accepted);
        }catch(java.io.IOException failure){throw new AssertionError("Missing authentic accepted receipt",failure);}
    }
    public static boolean sourceAfterAcceptance(long read,long source,long extraction){return read>0&&source>read&&extraction>source;}
    public static void require(boolean yes,String reason){if(!yes)throw new AssertionError(reason);}
}
