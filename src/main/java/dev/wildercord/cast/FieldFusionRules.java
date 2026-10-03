package dev.wildercord.cast;

/** Fixed caps: power/radius/Extend never enlarge these per-payment limits. */
public final class FieldFusionRules {
    private FieldFusionRules() {}
    public static final int SPRING_PLANTS=8, SIEVE_ITEMS=16, TARGETS=8, THRESH_CROPS=9;
    public static final int CLOCK_TICKS=80, SKY_TICKS=80, SKY_DESCENT_TICKS=40;
    public static final double CLOCK_ESCAPE=2, SIEVE_REACH=4, MERCY_HEALTH=4;

    /** Conditions must actually be removed; a clean healthy target yields no resource. */
    public static float mercyHealing(int removedEffects, boolean extinguished, float missingHealth) {
        if (!Float.isFinite(missingHealth) || missingHealth<=0) return 0;
        int conditions=Math.min(2,Math.max(0,removedEffects))+(extinguished?1:0);
        return Math.min(missingHealth,Math.min(4,conditions*2F));
    }
    /** A return consumes the mark and never triggers on vertical falling/jumping alone. */
    public static boolean escaped(double dx,double dz) {
        return Double.isFinite(dx) && Double.isFinite(dz) && dx*dx+dz*dz>CLOCK_ESCAPE*CLOCK_ESCAPE;
    }
    /** Bounded vertical correction; horizontal motion is intentionally not an input. */
    public static double hoverVelocity(double y,double anchorY) {
        if (!Double.isFinite(y) || !Double.isFinite(anchorY)) return 0;
        return Math.max(-.10,Math.min(.10,(anchorY-y)*.18));
    }
    /** Whole recipe units that fit; never crop/delete output to fit a slot or duplicate leftovers. */
    public static int sieveUnits(int inputCount,int outputPerUnit,int freeOutputCapacity,int paidItemsLeft) {
        if(inputCount<=0 || outputPerUnit<=0 || freeOutputCapacity<=0 || paidItemsLeft<=0)return 0;
        return Math.min(Math.min(inputCount,Math.min(paidItemsLeft,SIEVE_ITEMS)),freeOutputCapacity/outputPerUnit);
    }
}
