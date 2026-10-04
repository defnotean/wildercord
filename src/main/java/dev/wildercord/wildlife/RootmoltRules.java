package dev.wildercord.wildlife;
/** Original threat's finite bounds; no per-tick damage or permanent hold. */
public final class RootmoltRules {
 private RootmoltRules() {}
 public static final int WINDUP=28,RAKE=8,HELD=32,RECOVERY=60,ATTACK_REST=180,MEAL_REST=1200,BROWSE=50;
 public static final int LOCAL_CAP=2,SEARCH_RADIUS=4,SEARCH_COLUMNS=8,PATH_BUDGET=2,JOURNEY=260;
 // Mob movement also scales forward input in26.3; purposeful speed must cover local travel within260ticks.
 public static final double VISIT_SPEED=1.0;
 public static boolean room(int count) {return count<LOCAL_CAP;}
}
