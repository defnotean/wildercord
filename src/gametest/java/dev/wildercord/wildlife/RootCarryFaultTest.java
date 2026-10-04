package dev.wildercord.wildlife;
import dev.wildercord.cast.Cast;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
/** Actual write/authority callback faults. Constructed Cast objects are not mana-payment evidence. */
public final class RootCarryFaultTest implements FabricClientGameTest {
 private static final BlockPos FROM=new BlockPos(0,101,0),TO=new BlockPos(3,101,0);
 private static final AtomicBoolean DENY=new AtomicBoolean();
 private static final AtomicInteger PERMISSION_FAULT=new AtomicInteger();
 private static final AtomicInteger CLAIM_GUARD_FAULT=new AtomicInteger();private static boolean NESTED_SELECTION;private static final BlockPos ALT=FROM.offset(4,0,0);
 public void runTest(ClientGameTestContext c){PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->{
   int guard=CLAIM_GUARD_FAULT.get();
   if(guard==1&&at.equals(FROM)){CLAIM_GUARD_FAULT.set(0);NESTED_SELECTION=RootCarry.apply(new Cast((ServerPlayer)p),new Cast.Hit(List.of(),Vec3.atCenterOf(ALT),new Vec3(0,-1,0),p.position(),ALT,Direction.UP,false))==RootCarry.Result.SELECTED;}
   if(guard==2&&at.equals(FROM)){CLAIM_GUARD_FAULT.set(0);l.setBlock(TO.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);}
   if(guard==3&&at.equals(TO)){CLAIM_GUARD_FAULT.set(0);l.setBlock(FROM.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);}
   if(guard==4&&at.equals(TO)){CLAIM_GUARD_FAULT.set(0);var mark=p.getAttached(RootCarry.SELECT);p.setAttached(RootCarry.SELECT,new RootCarry.Selection(mark.dimension(),mark.at(),mark.state(),mark.until()));}
   if(guard==5&&at.equals(TO)){CLAIM_GUARD_FAULT.set(0);var sp=(ServerPlayer)p;sp.teleportTo(sp.level(),sp.getX()+.2,sp.getY(),sp.getZ(),Set.<Relative>of(),sp.getYRot(),sp.getXRot(),false);}
   if(at.equals(TO)){int fault=PERMISSION_FAULT.getAndSet(0);if(fault==1&&p instanceof ServerPlayer sp)sp.teleportTo(sp.level(),20,101,20,Set.<Relative>of(),0,0,false);else if(fault==2)l.setBlock(FROM,Blocks.STONE.defaultBlockState(),2);else if(fault==3)l.setBlock(new BlockPos(3,101,1),Blocks.STONE.defaultBlockState(),2);}
   return !DENY.get()||(!at.equals(FROM)&&!at.equals(TO));
  });
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=s.overworld();for(int x=-3;x<=7;x++)for(int z=-4;z<=4;z++){l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);for(int y=101;y<=104;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
    reset(p);l.setBlock(ALT,EmberContent.FERN.defaultBlockState(),2);NESTED_SELECTION=true;CLAIM_GUARD_FAULT.set(1);check(RootCarry.apply(new Cast(p),source())==RootCarry.Result.SELECTED&&!NESTED_SELECTION&&p.getAttached(RootCarry.SELECT).at().equals(FROM),"Actual initial claim callback cannot recursively select another eligible cell for same player");l.removeBlock(ALT,false);
    for(int guard:new int[]{2,3,4,5}){reset(p);select(p);var initial=p.getAttached(RootCarry.SELECT);CLAIM_GUARD_FAULT.set(guard);int[] count={0};check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{count[0]++;return world.setBlock(at,next,2);})==RootCarry.Result.REFUSED&&count[0]==0,"Claim callback cross-cell valid-soil change, marker replacement or small same-world displacement refuses before writes");check(l.getBlockState(FROM).equals(initial.state())&&l.getBlockState(TO).isAir()&&p.getAttachedOrElse(RootCarry.READY,0L)==0,"Whole snapshot refusal edits neither root and earns no rest");if(guard==4)check(p.getAttached(RootCarry.SELECT)!=initial,"Foreign callback marker is retained rather than overwritten or consumed");l.setBlock(FROM.below(),Blocks.DIRT.defaultBlockState(),2);l.setBlock(TO.below(),Blocks.DIRT.defaultBlockState(),2);}
    reset(p);var old=l.getBlockState(FROM);var first=new Cast(p);check(RootCarry.apply(first,source())==RootCarry.Result.SELECTED,"Actual young-root selection admitted");check(RootCarry.apply(first.pulse(),destination())==RootCarry.Result.REFUSED,"Same payment cannot select and move through a pulse");check(l.getBlockState(FROM).equals(old)&&l.getBlockState(TO).isAir(),"Shared-payment refusal edits nothing");
    reset(p);select(p);check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);return false;})==RootCarry.Result.REFUSED,"False source writer after actual removal refuses");restored(p,old);
    reset(p);select(p);check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);return !at.equals(TO);})==RootCarry.Result.REFUSED,"False destination writer after actual placement refuses");restored(p,old);
    reset(p);select(p);check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(TO))world.setBlock(FROM,old,2);return true;})==RootCarry.Result.REFUSED,"Callback restoring source cannot leave duplicate roots");restored(p,old);
    reset(p);select(p);check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(TO))world.setBlock(at,Blocks.STONE.defaultBlockState(),2);return true;})==RootCarry.Result.REFUSED,"Foreign destination replacement invalidates success");check(l.getBlockState(FROM).equals(old)&&l.getBlockState(TO).is(Blocks.STONE),"Rollback preserves callback's foreign destination and restores only owned source");noSuccess(p);
    reset(p);select(p);check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(FROM))p.teleportTo(world,20,101,20,Set.<Relative>of(),0,0,false);return true;})==RootCarry.Result.REFUSED,"Actual source departure after removal invalidates actor");restored(p,old);
    reset(p);select(p);check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(FROM))p.teleportTo(s.getLevel(net.minecraft.world.level.Level.NETHER),0,110,0,Set.<Relative>of(),0,0,false);return true;})==RootCarry.Result.REFUSED,"Actual dimension departure cannot transfer old-world root");check(l.getBlockState(FROM).equals(old)&&l.getBlockState(TO).isAir(),"Cross-world refusal restores original two cells");noSuccess(p);
    reset(p);select(p);try{check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(TO))DENY.set(true);return true;})==RootCarry.Result.REFUSED,"Fresh actual destination claim veto after write refuses");restored(p,old);}finally{DENY.set(false);}
    for(int fault:new int[]{1,2,3}){reset(p);select(p);PERMISSION_FAULT.set(fault);int[] writes={0};check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{writes[0]++;return world.setBlock(at,next,2);})==RootCarry.Result.REFUSED,"Actual permission callback movement, source replacement or obstruction refuses before mutation");check(writes[0]==0&&l.getBlockState(TO).isAir()&&l.getBlockState(FROM).equals(fault==2?Blocks.STONE.defaultBlockState():old)&&p.getAttachedOrElse(RootCarry.READY,0L)==0,"Preflight side effects never overwrite foreign source, spend success rest or execute a writer");l.setBlock(new BlockPos(3,101,1),Blocks.AIR.defaultBlockState(),2);}
    reset(p);select(p);boolean threw=false;try{RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(TO))throw new IllegalStateException("Injected real destination-writer exception");return true;});}catch(IllegalStateException expected){threw=true;}check(threw,"Actual writer exception propagates after cleanup");restored(p,old);
    reset(p);select(p);boolean[] nested={true};check(RootCarry.apply(new Cast(p),destination(),(world,at,next)->{world.setBlock(at,next,2);if(at.equals(TO))nested[0]=RootCarry.apply(new Cast(p),new Cast.Hit(List.of(),Vec3.atCenterOf(TO),new Vec3(0,-1,0),p.position(),TO,Direction.UP,false))==RootCarry.Result.SELECTED;return true;})==RootCarry.Result.MOVED,"Ordinary exact retained transfer survives refused nested operation");check(!nested[0]&&l.getBlockState(FROM).isAir()&&l.getBlockState(TO).equals(old)&&p.getAttached(RootCarry.SELECT)==null&&p.getAttachedOrElse(RootCarry.READY,0L)>s.overworld().getGameTime(),"One exact retained young root, no nested marker, finite success rest");
    reset(p);select(p);var shortBudget=new Cast(p);check(shortBudget.takeBlocks(dev.wildercord.config.Config.get().maxBlocks()-1),"Leave exactly one actual block budget");check(RootCarry.apply(shortBudget,destination())==RootCarry.Result.REFUSED,"Two-write relocation cannot use one-cell remainder");check(l.getBlockState(FROM).equals(old)&&l.getBlockState(TO).isAir(),"Insufficient budget performs zero mutations");
    reset(p);l.setBlock(FROM,old.setValue(CinderFernBlock.AGE,1),2);check(RootCarry.apply(new Cast(p),source())==RootCarry.Result.REFUSED&&p.getAttached(RootCarry.SELECT)==null,"Prepared plant is never selected or reset to young");
    for(int age:new int[]{1,2}){reset(p);select(p);var grown=old.setValue(CinderFernBlock.AGE,age);l.setBlock(FROM,grown,2);check(RootCarry.apply(new Cast(p),destination())==RootCarry.Result.REFUSED&&l.getBlockState(FROM).equals(grown)&&l.getBlockState(TO).isAir(),"Actual growth since selection invalidates marker without maturity reset");noSuccess(p);}
    reset(p);select(p);var cooled=old.setValue(CinderFernBlock.COOLED,true);l.setBlock(FROM,cooled,2);check(RootCarry.apply(new Cast(p),destination())==RootCarry.Result.REFUSED&&l.getBlockState(FROM).equals(cooled)&&l.getBlockState(TO).isAir(),"Actual cooling since selection cannot resurrect obsolete hot state");noSuccess(p);
    reset(p);l.setBlock(FROM,cooled,2);select(p);check(RootCarry.apply(new Cast(p),destination())==RootCarry.Result.MOVED&&l.getBlockState(FROM).isAir()&&l.getBlockState(TO).equals(cooled),"Separate genuine operations retain exact young cooled state as well as hot state");
   });
  }finally{DENY.set(false);PERMISSION_FAULT.set(0);CLAIM_GUARD_FAULT.set(0);NESTED_SELECTION=false;}
 }
 private static void reset(ServerPlayer p){DENY.set(false);p.teleportTo(p.level().getServer().overworld(),2,101,2.5,Set.<Relative>of(),0,0,false);p.setGameMode(GameType.SURVIVAL);p.removeAttached(RootCarry.SELECT);p.setAttached(RootCarry.READY,0L);p.level().setBlock(FROM,EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.COOLED,false),2);p.level().setBlock(TO,Blocks.AIR.defaultBlockState(),2);}
 private static Cast.Hit source(){return new Cast.Hit(List.of(),Vec3.atCenterOf(FROM),new Vec3(0,-1,0),Vec3.ZERO,FROM,Direction.UP,false);}
 private static Cast.Hit destination(){return new Cast.Hit(List.of(),Vec3.atCenterOf(TO.below()),new Vec3(0,-1,0),Vec3.ZERO,TO.below(),Direction.UP,false);}
 private static void select(ServerPlayer p){check(RootCarry.apply(new Cast(p),source())==RootCarry.Result.SELECTED,"Genuine helper source admission for callback fault fixture");}
 private static void restored(ServerPlayer p,net.minecraft.world.level.block.state.BlockState old){var l=p.level().getServer().overworld();check(l.getBlockState(FROM).equals(old)&&l.getBlockState(TO).isAir(),"Failed mutation retains exactly the original root and no destination copy");noSuccess(p);}
 private static void noSuccess(ServerPlayer p){check(p.getAttachedOrElse(RootCarry.READY,0L)==0&&p.getAttached(RootCarry.SELECT)==null,"Failed callback earns no rest or retained selection");}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
