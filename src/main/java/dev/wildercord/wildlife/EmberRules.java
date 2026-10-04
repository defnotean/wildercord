package dev.wildercord.wildlife;
/** Independent fan geometry and finite ecological clocks; no entity/world retention. */
public final class EmberRules {
 private EmberRules() {}
 public static final int WARNING=24,FAN=18,RECOVERY=40,ATTACK_REST=100,MEAL_REST=1200,BROWSE=40,JOURNEY=400,LOCAL_CAP=2,VICTIMS=12;
 public static boolean lane(double dx,double dy,double dz,double fx,double fz,int lane){
  if(lane<0 || lane>2 || !Double.isFinite(dx+dy+dz+fx+fz) || Math.abs(dy)>1.4)return false;
  double radius=Math.sqrt(dx*dx+dz*dz);if(radius<.3 || radius>4)return false;
  double along=dx*fx+dz*fz,side=dx*fz-dz*fx;if(along<=0)return false;
  double angle=Math.atan2(side,along),middle=(lane-1)*Math.PI/6;
  return Math.abs(angle-middle)<=Math.PI/12;
 }
}
