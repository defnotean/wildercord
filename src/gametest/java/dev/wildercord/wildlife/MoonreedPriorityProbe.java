package dev.wildercord.wildlife;

import com.google.gson.JsonObject;
import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/** Bounded test-only read receipts at actual scheduled seekLure calls. Never invokes AI or changes its inputs. */
public final class MoonreedPriorityProbe {
 public enum Mode { CASTER, FLOWER, LAMP }
 private static Session active;
 private MoonreedPriorityProbe() {}
 private static final class Session {
  final ServerLevel level;final Glimmerwing moth;final Mode mode;
  int searches,eligible,matched,violations,errors;BlockPos candidate;Vec3 caster;boolean lampAvailable;
  Session(ServerLevel level,Glimmerwing moth,Mode mode){this.level=level;this.moth=moth;this.mode=mode;}
 }
 public static synchronized void begin(ServerLevel level,Glimmerwing moth,Mode mode){active=new Session(level,moth,mode);}
 public static synchronized void clear(){active=null;}
 public static synchronized boolean observed(){return active!=null&&(active.matched>0||active.violations>0||active.errors>0);}

 public static synchronized void search(Glimmerwing moth,boolean after,BlockPos flower,Vec3 lure,int lureLeft) {
  var s=active;if(s==null||s.moth!=moth)return;
  try {
   if(!after) {
    // This test-session-only comparison runs at the real search boundary, never as an extra AI tick.
    s.searches++;s.candidate=MoonreedBlock.findBud(s.level,moth.blockPosition());
    ServerPlayer closest=null;double best=WildlifeRules.CAST_LURE_RANGE*WildlifeRules.CAST_LURE_RANGE;
    for(var player:s.level.players())if(!player.isSpectator()&&player.distanceToSqr(moth)<best&&Wildlife.castRecently(player,WildlifeRules.CAST_LURE_TICKS)){best=player.distanceToSqr(moth);closest=player;}
    s.caster=closest==null?null:closest.getEyePosition().add(0,.6,0);
    // Verify an actual air-cell light candidate in the production sample box, not just a luminous block.
    s.lampAvailable=false;
    for(int dy=-4;dy<=4;dy++) {
     var at=moth.blockPosition().offset(0,dy,0);
     if(s.level.hasChunkAt(at)&&s.level.getBlockState(at).isAir()&&s.level.getBrightness(LightLayer.BLOCK,at)>=WildlifeRules.LIGHT_LURE){s.lampAvailable=true;break;}
    }
    if(Math.floorMod(moth.tickCount+moth.getId(),40)!=0)s.violations++;
    return;
   }
   boolean eligible=switch(s.mode) {
    case CASTER -> s.caster!=null&&s.candidate!=null&&s.lampAvailable;
    case FLOWER -> s.caster==null&&s.candidate!=null&&s.lampAvailable;
    case LAMP -> s.caster==null&&s.candidate==null;
   };
   if(!eligible)return;s.eligible++;
   boolean matches=switch(s.mode) {
    case CASTER -> flower==null&&s.caster.equals(lure)&&lureLeft==80;
    case FLOWER -> s.candidate.equals(flower)&&Vec3.atCenterOf(flower).add(0,.4,0).equals(lure)&&lureLeft==100;
    case LAMP -> flower==null&&lure!=null&&lureLeft==200&&s.level.getBlockState(BlockPos.containing(lure)).isAir()&&s.level.getBrightness(LightLayer.BLOCK,BlockPos.containing(lure))>=WildlifeRules.LIGHT_LURE;
   };
   if(matches)s.matched++;
   // A lamp sample may legitimately miss. Caster/flower are deterministic whenever their inputs are eligible.
   else if(s.mode!=Mode.LAMP)s.violations++;
  }catch(Throwable ignored){s.errors++;}
 }

 public static synchronized void finish(boolean completed) {
  var s=active;active=null;
  if(s==null)throw new AssertionError("Missing native priority observation session");
  var receipt=new JsonObject();receipt.addProperty("mode",s.mode.toString());receipt.addProperty("searches",s.searches);
  receipt.addProperty("eligibleSearches",s.eligible);receipt.addProperty("matchingReturns",s.matched);
  receipt.addProperty("violations",s.violations);receipt.addProperty("observationErrors",s.errors);
  receipt.addProperty("ordinaryTicks",true);receipt.addProperty("casterStimulus","AFTER_CAST event, not paid cast");
  Wildercord.LOGGER.info("WILDERCORD_MOONREED_PRIORITY {}",receipt);
  if(!completed||s.matched==0||s.violations!=0||s.errors!=0)
   throw new AssertionError("Native scheduled "+s.mode+" priority receipt failed: "+receipt);
 }
}
