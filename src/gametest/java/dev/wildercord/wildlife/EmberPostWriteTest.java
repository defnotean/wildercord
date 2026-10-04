package dev.wildercord.wildlife;
import dev.wildercord.cast.Cast;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
/** Real changed-world callback refusals. Constructed Cast faults test helper admission, not mana payment. */
public final class EmberPostWriteTest implements FabricClientGameTest {
 private static final BlockPos P=new BlockPos(0,30,0);private static final AtomicBoolean DENY=new AtomicBoolean();private CinderBailiff mob;private static int CLAIM_MODE;private static boolean NESTED_CLAIM;private static boolean NESTED_CAST;private static int CLAIM_COUNT;
 public void runTest(ClientGameTestContext c){PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->{
   if(!at.equals(P))return true;if(DENY.get())return false;
   if(CLAIM_MODE==1){CLAIM_MODE=0;NESTED_CLAIM=EmberContent.FERN.harvest(state,(ServerLevel)l,at,p,EmberPlantAdmission.WORLD)==InteractionResult.SUCCESS;}
   if(CLAIM_MODE==2){CLAIM_MODE=0;((ServerPlayer)p).teleportTo((ServerLevel)l,5.5,30,5.5,Set.<Relative>of(),0,0,false);}
   if(CLAIM_MODE==3){CLAIM_MODE=0;l.setBlock(at,Blocks.AIR.defaultBlockState(),2);}
   if(CLAIM_MODE==5){CLAIM_MODE=0;NESTED_CAST=EmberContent.affectFern(new Cast((ServerPlayer)p),at,"life");}
   if(CLAIM_MODE==6){CLAIM_MODE=0;((ServerPlayer)p).teleportTo((ServerLevel)l,6.5,30,6.5,Set.<Relative>of(),0,0,false);}
   if(CLAIM_MODE==7&&++CLAIM_COUNT==2){CLAIM_MODE=0;((ServerPlayer)p).teleportTo((ServerLevel)l,6.5,30,6.5,Set.<Relative>of(),0,0,false);}
   return true;
  });
  try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("difficulty normal");
   w.getServer().runOnServer(s->{var l=s.overworld();var who=s.getPlayerList().getPlayers().getFirst();for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){l.setBlock(new BlockPos(x,29,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}reset(l,who,2);int start=items(l,who);
    CLAIM_MODE=1;NESTED_CLAIM=true;check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,EmberPlantAdmission.WORLD)==InteractionResult.SUCCESS&&!NESTED_CLAIM,"Lease begins before real claim callback; recursive claim harvesting cannot mint twice");check(items(l,who)==start+1,"One valid outer harvest earns exactly one frond");start=items(l,who);reset(l,who,2);
    CLAIM_MODE=2;boolean[] claimWrote={false};check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{claimWrote[0]=true;return level.setBlock(at,next,2);})!=InteractionResult.SUCCESS&&!claimWrote[0],"Actual claim callback player departure refuses before writing");check(items(l,who)==start&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==2,"Departing claim preserves mature cell and reward");reset(l,who,2);
    CLAIM_MODE=3;check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{claimWrote[0]=true;return level.setBlock(at,next,2);})!=InteractionResult.SUCCESS&&!claimWrote[0],"Actual claim callback cell replacement refuses before writing");check(l.getBlockState(P).isAir()&&items(l,who)==start,"Replacement belongs to callback; no stale restore or reward");reset(l,who,2);
    l.setBlock(P.south().above(),Blocks.STONE.defaultBlockState(),2);l.setBlock(P.south(),Blocks.STONE.defaultBlockState(),2);boolean[] wrote={false};check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{wrote[0]=true;return level.setBlock(at,next,2);})!=InteractionResult.SUCCESS&&!wrote[0],"Actual wall blocks harvest before writer admission");check(items(l,who)==start&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==2,"Through-wall refusal preserves plant and reward");l.removeBlock(P.south(),false);l.removeBlock(P.south().above(),false);
    check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(P.south(),Blocks.STONE.defaultBlockState(),2);level.setBlock(P.south().above(),Blocks.STONE.defaultBlockState(),2);return true;})!=InteractionResult.SUCCESS,"Fresh callback wall prevents post-write reward");check(items(l,who)==start,"Occluded callback mints no frond");l.removeBlock(P.south(),false);l.removeBlock(P.south().above(),false);reset(l,who,2);
    check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{level.setBlock(at,next,2);return false;})!=InteractionResult.SUCCESS,"False after actual harvest write earns no frond");check(items(l,who)==start,"False writer mints zero inventory/drop items");
    reset(l,who,2);check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return true;})!=InteractionResult.SUCCESS,"Successful boolean with actual AIR is not a harvest");check(items(l,who)==start,"Replacement callback mints zero fronds");
    reset(l,who,2);check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{level.setBlock(at,next,2);who.teleportTo(level,5.5,30,5.5,Set.<Relative>of(),0,0,false);return true;})!=InteractionResult.SUCCESS,"Actual departure invalidates close harvest admission");check(items(l,who)==start,"Departed player receives no dropped reward");
    reset(l,who,2);check(EmberContent.FERN.harvest(l.getBlockState(P),l,P,who,(level,at,next)->{level.setBlock(at,next,2);DENY.set(true);return true;})!=InteractionResult.SUCCESS,"Fresh real permission veto after write refuses reward");DENY.set(false);check(items(l,who)==start,"Claim veto mints no fronds");
    reset(l,who,2);boolean[] nested={true};var old=l.getBlockState(P);check(EmberContent.FERN.harvest(old,l,P,who,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,old,2);nested[0]=EmberContent.FERN.harvest(old,level,at,who,EmberPlantAdmission.WORLD)==InteractionResult.SUCCESS;level.setBlock(at,next,2);return true;})==InteractionResult.SUCCESS,"Actual valid outer harvest still works after rejected nested attempt");check(!nested[0]&&items(l,who)==start+1,"Same-cell reentry cannot double-mint even after original AGE2 is restored");
    reset(l,who,0);CLAIM_MODE=5;NESTED_CAST=true;check(EmberContent.affectFern(new Cast(who),P,"life")&&!NESTED_CAST&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==1,"Real initial claim callback cannot reenter a fresh Cast on leased fern");
    reset(l,who,0);CLAIM_MODE=6;boolean[] castWrote={false};check(!EmberContent.affectFern(new Cast(who),P,"life",(level,at,next)->{castWrote[0]=true;return level.setBlock(at,next,2);})&&!castWrote[0]&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==0,"Actual initial claim callback displacement refuses before owner write");
    reset(l,who,0);CLAIM_MODE=7;CLAIM_COUNT=0;var departed=new Cast(who);check(!EmberContent.affectFern(departed,P,"life")&&CLAIM_COUNT==2&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==1,"Actual post-write claim callback displacement refuses success after real change");reset(l,who,0);check(!EmberContent.affectFern(departed,P,"life")&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==0,"Post-write authority failure does not refund shared once admission");
    reset(l,who,0);Cast cast=new Cast(who);check(!EmberContent.affectFern(cast,P,"life",(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return true;}),"Grow helper cannot claim AIR from successful stale writer boolean");l.setBlock(P,EmberContent.FERN.defaultBlockState(),2);check(!EmberContent.affectFern(cast,P,"life"),"Failed callback cannot renew the same payment's once budget");
    reset(l,who,0);check(!EmberContent.affectFern(new Cast(who),P,"fire",(level,at,next)->{level.setBlock(at,next,2);who.setGameMode(GameType.SPECTATOR);return true;}),"Caster eligibility is rechecked after actual Fire write");
    reset(l,who,0);check(!EmberContent.affectFern(new Cast(who),P,"life",(level,at,next)->{level.setBlock(at,next,2);DENY.set(true);return true;}),"Live changed claim refuses successful Grow helper");DENY.set(false);
    reset(l,who,0);check(EmberContent.affectFern(new Cast(who),P,"life")&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==1,"Ordinary exact retained write is still admitted");
    reset(l,who,2);who.setGameMode(GameType.CREATIVE);who.teleportTo(l,5.5,30,5.5,Set.<Relative>of(),0,0,false);mob=EmberContent.BAILIFF.create(l,EntitySpawnReason.COMMAND);mob.snapTo(2.5,30,.5,0,0);l.addFreshEntity(mob);
   });
   boolean browse=false;for(int i=0;i<180;i++){c.waitTicks(2);if(w.getServer().computeOnServer(s->mob.pose()==CinderBailiff.BROWSING)){browse=true;break;}}check(browse,"Actual AI reaches and begins visible browsing before callback tests");
   w.getServer().runOnServer(s->{var l=s.overworld();long ready=mob.mealReady();check(ready==0&&mob.home()==null,"Actual first browsing has no invented progress/deadline");var ripe=EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,2);l.setBlock(P,ripe,2);
    check(!mob.meal(l,P,(level,at,next)->{level.setBlock(at,next,2);return false;}),"False post-consumption writer refuses meal ownership");check(mob.mealReady()==ready&&mob.home()==null&&mob.restUntil()==0,"False callback grants no rest or home");
    l.setBlock(P,ripe,2);check(!mob.meal(l,P,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return true;}),"Actual removed consumed plant cannot grant meal ownership");check(mob.mealReady()==ready&&mob.home()==null,"Stale boolean cannot grant home or meal deadline");
    l.setBlock(P,ripe,2);double mx=mob.getX(),my=mob.getY(),mz=mob.getZ();check(!mob.meal(l,P,(level,at,next)->{level.setBlock(at,next,2);mob.snapTo(5.5,30,5.5,0,0);return true;}),"Real callback displacement invalidates reached meal authority");check(mob.mealReady()==ready&&mob.home()==null&&mob.restUntil()==0,"Departed browsing actor receives no home or clocks");mob.snapTo(mx,my,mz,0,0);
    l.setBlock(P,ripe,2);boolean[] nested={true};check(mob.meal(l,P,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,ripe,2);nested[0]=mob.meal(level,at);level.setBlock(at,next,2);return true;}),"Actual valid outer reached meal survives rejected same-cell reentry");check(!nested[0]&&mob.mealReady()>l.getGameTime()&&mob.home().equals(P)&&mob.restUntil()>l.getGameTime()&&l.getBlockState(P).getValue(CinderFernBlock.AGE)==1,"One exact consumed growth owns finite meal/rest/home exactly once");
   });
  }finally{DENY.set(false);CLAIM_MODE=0;NESTED_CLAIM=false;NESTED_CAST=false;CLAIM_COUNT=0;}
 }
 private static void reset(ServerLevel l,ServerPlayer p,int age){DENY.set(false);p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,.5,30,2.5,Set.<Relative>of(),0,0,false);l.setBlock(P,EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,age),2);}
 private static int items(ServerLevel l,ServerPlayer p){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(EmberContent.FERN_ITEM))n+=s.getCount();}for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(8)))if(e.getItem().is(EmberContent.FERN_ITEM))n+=e.getItem().getCount();return n;}
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
