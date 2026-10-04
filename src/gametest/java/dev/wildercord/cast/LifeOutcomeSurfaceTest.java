package dev.wildercord.cast;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Pure geometry in initialized native runtime; owner/native screenshots are a separate gate. */
public final class LifeOutcomeSurfaceTest implements FabricClientGameTest {
 private record Piece(ParticleOptions option,Vec3 at){}
 @Override public void runTest(ClientGameTestContext c){
  Vec3 anchor=new Vec3(3,101,4);
  for(String rune:LifeOutcomes.RUNES)for(boolean minimal:new boolean[]{false,true})for(var moment:List.of(LifeOutcomes.Moment.APPLY,LifeOutcomes.Moment.PULSE,LifeOutcomes.Moment.TRIGGER,LifeOutcomes.Moment.END)){
   var original=draw(new LifeOutcomes.Observation(rune,moment,anchor,null,6,2,0),minimal);
   for(Vec3 normal:List.of(new Vec3(0,0,1),new Vec3(1,0,0),new Vec3(0,0,-1),new Vec3(-1,0,0),new Vec3(0,1,0))){
    var framed=draw(new LifeOutcomes.Observation(rune,moment,anchor,null,6,2,0,normal,.58),minimal);
    boolean supportedSpore=rune.equals("sporebloom")&&normal.equals(new Vec3(0,1,0));
    var reference=supportedSpore?draw(new LifeOutcomes.Observation(rune,moment,anchor,null,6,2,0,normal,0),minimal):original;
    check(reference.size()==framed.size(),"Same authored surface variant preserves piece count: "+rune);
    if(rune.equals("sporebloom")){
     long bodySeeds=original.stream().filter(p->p.option() instanceof LifeOption life&&life.style()==LifeOption.SEED).count();
     check(bodySeeds==0,"Horizontal airborne spores never borrow grounded seed coat");
     if(supportedSpore){
      long seeds=reference.stream().filter(p->p.option() instanceof LifeOption life&&life.style()==LifeOption.SEED).count();
      long tissue=reference.stream().filter(p->p.option() instanceof LifeOption life&&life.style()==LifeOption.TISSUE).count();
      check(seeds==1&&tissue==(minimal?6:10)&&reference.size()==(minimal?10:14),"Supported UP fruit keeps exact seed coat, tissue lobes and authored quality counts");
     }
    }
    Vec3 right=new Vec3(0,1,0).cross(normal);if(right.lengthSqr()<1e-6)right=new Vec3(1,0,0);else right=right.normalize();Vec3 up=normal.cross(right);
    for(int i=0;i<reference.size();i++){
     var old=reference.get(i);var changed=framed.get(i);Vec3 local=old.at().subtract(anchor);
     Vec3 expected=supportedSpore?old.at().add(normal.scale(.58)):anchor.add(normal.scale(.58)).add(right.scale(local.x)).add(up.scale(local.y)).add(normal.scale(local.z));
     check(expected.distanceToSqr(changed.at())<1e-16,"Exact outward local transform "+rune);
     if(old.option() instanceof LifeOption a){var b=(LifeOption)changed.option();
      check(a.style()==b.style()&&a.color()==b.color()&&a.size()==b.size()&&a.lifetime()==b.lifetime()&&a.spin()==b.spin(),"No material inflation or replacement "+rune);
      Vec3 drift=supportedSpore?a.drift():right.scale(a.drift().x).add(up.scale(a.drift().y)).add(normal.scale(a.drift().z));check(drift.distanceToSqr(b.drift())<1e-16,"Drift uses same frame "+rune);
     }else check(old.option().equals(changed.option())&&old.option() instanceof MaterialOption,"Supporting option unchanged "+rune);
     if(rune.equals("root_bulwark"))check(changed.at().subtract(anchor).dot(normal)>.5,"ROOT pieces lie outside actual full cube for every outward face");
    }
   }
  }
  Vec3 normal=new Vec3(-1,0,0),other=new Vec3(4,102,6),focus=anchor.add(normal.scale(.38));
  var tow=draw(new LifeOutcomes.Observation("soulbond",LifeOutcomes.Moment.APPLY,anchor,other,1,0,0,normal,.38),false);
  var tail=(LifeOption)tow.getLast().option();Vec3 expected=other.subtract(focus).normalize().scale(.018);
  check(tail.drift().distanceToSqr(expected)<1e-16,"Real secondary endpoint direction not double-rotated");
  reject(()->new LifeOutcomes.Observation("heal",LifeOutcomes.Moment.APPLY,anchor,null,1,2,0,Vec3.ZERO,.3));
  reject(()->new LifeOutcomes.Observation("heal",LifeOutcomes.Moment.APPLY,anchor,null,1,2,0,new Vec3(0,0,2),.3));
  reject(()->new LifeOutcomes.Observation("heal",LifeOutcomes.Moment.APPLY,anchor,null,1,2,0,new Vec3(0,0,1),Double.NaN));
  reject(()->new LifeOutcomes.Observation("heal",LifeOutcomes.Moment.APPLY,anchor,null,1,2,0,new Vec3(0,0,1),4.01));
 }
 private static List<Piece> draw(LifeOutcomes.Observation o,boolean minimal){var pieces=new ArrayList<Piece>();LifeOutcomes.draw(o,minimal,(p,at)->pieces.add(new Piece(p,at)));return pieces;}
 private static void reject(Runnable r){boolean refused=false;try{r.run();}catch(IllegalArgumentException expected){refused=true;}check(refused,"Invalid frame rejected");}
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
