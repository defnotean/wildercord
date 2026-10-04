package dev.wildercord.cast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pure finite resource rules shared by the twelve expedition signatures. */
public final class NextSignatureRules {
    private NextSignatureRules() {}
    public static final int ACTIVE=128, REST=160, INTERCEPT_TICKS=40;
    public static final int BELL_FIRST=20, BELL_SECOND=40, LEDGER_TICKS=60, LEDGER_GAP=8;
    public static final int FERRY_HEALTH=6, FERRY_PER_RECIPIENT=3, ESCROW_HEALTH=3, ESCROW_FLOOR=4;
    public static final int CHEST_INPUTS=16, COMPASS_CELLS=512, COMPASS_PER_TICK=32, SEAM_CELLS=3;
    public static final int LANTERN_TICKS=120, TOW_TICKS=40;
    public static final double BELL_DAMAGE=8, LEDGER_DAMAGE=6;

    /** Finite resources only. Distinct rune keys intentionally share the same authoritative payment instance. */
    public static final class Ledger {
        private final Map<String,Double> remaining=new HashMap<>();
        private final Map<String,Set<UUID>> recipients=new HashMap<>();
        private final Set<String> admitted=new HashSet<>();
        public boolean once(String key) {return admitted.add(key);}
        public boolean target(String key,UUID target,int cap) {
            if(cap<=0 || target==null)return false;
            var ids=recipients.computeIfAbsent(key,k -> new HashSet<>());
            if(ids.size()>=cap || ids.contains(target))return false;
            ids.add(target);return true;
        }
        /** Called AFTER every reaction/PvP multiplier and BEFORE Cast.admitDamage and defence. */
        public float damage(String key,double cap,float wanted) {
            if(!Double.isFinite(cap) || cap<=0 || !Float.isFinite(wanted) || wanted<=0)return 0;
            double left=remaining.computeIfAbsent(key,k -> cap);
            float accepted=(float)Math.min(left,wanted);
            remaining.put(key,Math.max(0,left-accepted));return accepted;
        }
        public float resource(String key,double cap,float wanted) {return damage(key,cap,wanted);}
        public double left(String key,double cap) {return remaining.getOrDefault(key,cap);}
        public float contribution(String key,UUID target,double globalCap,double targetCap,double hitCap,float wanted){
            if(target==null || !Float.isFinite(wanted) || wanted<=0 || !Double.isFinite(hitCap) || hitCap<=0
                || !Double.isFinite(globalCap) || globalCap<=0 || !Double.isFinite(targetCap) || targetCap<=0)return 0;
            String local=key+":"+target;
            float amount=(float)Math.min(wanted,Math.min(hitCap,Math.min(left(key,globalCap),left(local,targetCap))));
            if(amount<=0)return 0;
            float accepted=damage(key,globalCap,amount);damage(local,targetCap,accepted);return accepted;
        }
    }
    public static boolean escapedBell(double x,double z) {
        return !Double.isFinite(x) || !Double.isFinite(z) || x*x+z*z>1;
    }
    /** Reject apparent teleports rather than translating them into many movement cuts. */
    public static boolean ledgerStep(double x,double z) {
        if(!Double.isFinite(x) || !Double.isFinite(z))return false;
        double square=x*x+z*z;return square>=1 && square<=9;
    }
    public static float escrow(float casterHealth,float existingAbsorption) {
        if(!Float.isFinite(casterHealth) || !Float.isFinite(existingAbsorption))return 0;
        return Math.max(0,Math.min(ESCROW_HEALTH,Math.min(casterHealth-ESCROW_FLOOR,ESCROW_HEALTH-existingAbsorption)));
    }
    public static float ferry(float health,float maximum,float remaining) {
        if(!Float.isFinite(health) || !Float.isFinite(maximum) || !Float.isFinite(remaining))return 0;
        return Math.max(0,Math.min(FERRY_PER_RECIPIENT,Math.min(maximum-health,remaining)));
    }
    public static boolean crouchEdge(boolean previous,boolean now) {return !previous && now;}
    /** Both reservations independently constrain a drop; a matching target cannot override a foreign thrower. */
    public static boolean reservedFor(UUID player,UUID target,UUID thrower) {
        return player!=null && (target==null || target.equals(player)) && (thrower==null || thrower.equals(player));
    }
    public static double velocityImpulse(double current,double desired) {
        if(!Double.isFinite(current) || !Double.isFinite(desired))return 0;
        double impulse=desired-current;return Double.isFinite(impulse)?impulse:0;
    }
    public static double gradedCrumb(double distance) {
        if(!Double.isFinite(distance) || distance<0)return 0;
        return Math.max(.025,Math.min(.11,.11-distance*.008));
    }
    public static double tow(double distance) {
        if(!Double.isFinite(distance) || distance<.2 || distance>6)return 0;
        return Math.min(.18,distance*.065);
    }
}
