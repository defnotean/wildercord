package dev.wildercord.wildlife;
import dev.wildercord.config.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.*;
/** Real callback/writer faults are gate tests; the final moth and manual-use sections use genuine native AI/packets. */
public final class MoonreedPostWriteTest implements FabricClientGameTest {
 private static final BlockPos P=new BlockPos(0,30,0);
 private final AtomicReference<ServerLevel> owner=new AtomicReference<>();private final AtomicInteger mode=new AtomicInteger();private boolean nested;
 public void runTest(ClientGameTestContext c) {
  PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->{
   if(l!=owner.get() || !at.equals(P))return true;
   int m=mode.getAndSet(0);if(m==1)return false;
   if(m==2)l.setBlock(at,Blocks.AIR.defaultBlockState(),2);
   if(m==3)((ServerPlayer)p).teleportTo((ServerLevel)l,6.5,30,6.5,Set.<Relative>of(),0,0,false);
   if(m==4)nested=WetlandGarden.REED.harvest(state,(ServerLevel)l,at,p,MoonreedAdmission.WORLD)==InteractionResult.SUCCESS;
   return true;
  });
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   var original=w.getServer().computeOnServer(s->Config.get());
   try {
    w.getServer().runOnServer(s->{var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();owner.set(l);
     for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){l.setBlock(new BlockPos(x,29,z),Blocks.DIRT.defaultBlockState(),2);for(int y=30;y<=34;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setBlock(P.east().below(2),Blocks.DIRT.defaultBlockState(),2);l.setBlock(P.east().below(),Blocks.WATER.defaultBlockState(),2);p.getInventory().clearContent();reset(l,p,2);int before=items(l,p);
     mode.set(1);boolean[] wrote={false};check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,(level,at,next)->{wrote[0]=true;return level.setBlock(at,next,2);})!=InteractionResult.SUCCESS&&!wrote[0],"Real claim veto refuses before writer");check(items(l,p)==before&&l.getBlockState(P).getValue(MoonreedBlock.AGE)==2,"Claim refusal preserves mature root and zero reward");
     mode.set(2);check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,MoonreedAdmission.WORLD)!=InteractionResult.SUCCESS&&l.getBlockState(P).isAir()&&items(l,p)==before,"Real claim replacement is not overwritten or rewarded");reset(l,p,2);
     mode.set(3);check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,MoonreedAdmission.WORLD)!=InteractionResult.SUCCESS&&items(l,p)==before&&l.getBlockState(P).getValue(MoonreedBlock.AGE)==2,"Real claim departure refuses mutation/reward");reset(l,p,2);
     mode.set(4);nested=true;check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,MoonreedAdmission.WORLD)==InteractionResult.SUCCESS&&!nested&&items(l,p)==before+1,"Claim recursion cannot mint twice; outer exact harvest works");before=items(l,p);reset(l,p,2);
     check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,(level,at,next)->{level.setBlock(at,next,2);return false;})!=InteractionResult.SUCCESS&&items(l,p)==before,"False actual writer cannot earn floss");reset(l,p,2);
     check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return true;})!=InteractionResult.SUCCESS&&items(l,p)==before,"True stale writer with AIR cannot earn floss");reset(l,p,2);
     check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,(level,at,next)->{level.setBlock(at,next,2);mode.set(1);return true;})!=InteractionResult.SUCCESS&&items(l,p)==before,"Post-write claim denial earns no floss");reset(l,p,2);
     l.setBlock(P.south(),Blocks.STONE.defaultBlockState(),2);l.setBlock(P.south().above(),Blocks.STONE.defaultBlockState(),2);check(WetlandGarden.REED.harvest(l.getBlockState(P),l,P,p,MoonreedAdmission.WORLD)!=InteractionResult.SUCCESS&&items(l,p)==before,"Real wall blocks manual harvesting");l.removeBlock(P.south(),false);l.removeBlock(P.south().above(),false);
     reset(l,p,1);var touch=Vec3.atCenterOf(P).add(0,.4,0);
     check(!MoonreedBlock.pollinate(l,P,touch,(level,at,next)->{level.setBlock(at,next,2);return false;}),"False actual bloom writer reports no pollination");reset(l,p,1);
     check(!MoonreedBlock.pollinate(l,P,touch,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return true;}),"Stale bloom boolean reports no pollination");reset(l,p,1);
     check(!MoonreedBlock.pollinate(l,P,touch,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(P.above(),Blocks.STONE.defaultBlockState(),2);return true;}),"Actual roof added during bloom refuses success");l.removeBlock(P.above(),false);reset(l,p,1);
     var bud=l.getBlockState(P);boolean[] nestedBloom={true};check(MoonreedBlock.pollinate(l,P,touch,(level,at,next)->{level.setBlock(at,next,2);level.setBlock(at,bud,2);nestedBloom[0]=MoonreedBlock.pollinate(level,at,touch);level.setBlock(at,next,2);return true;})&&!nestedBloom[0],"Same-cell bloom recursion refuses; valid outer exact bloom succeeds");
     current(copy(original,Map.of("spellsEditBlocks",false)));check(!Config.get().spellsEditBlocks(),"Actual server setting disables spell terrain edits");reset(l,p,2);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    });
    c.waitTicks(5);int before=w.getServer().computeOnServer(s->items(s.overworld(),s.getPlayerList().getPlayers().getFirst()));
    c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,30.15,.5),Direction.SOUTH,P,false)));c.waitTicks(6);
    w.getServer().runOnServer(s->{var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();check(l.getBlockState(P).getValue(MoonreedBlock.AGE)==0&&items(l,p)==before+1,"Actual manual packet harvest works while spell edits are disabled");reset(l,p,1);p.setGameMode(GameType.CREATIVE);p.teleportTo(l,6.5,30,6.5,Set.<Relative>of(),0,0,false);var moth=Wildlife.GLIMMERWING.create(l,EntitySpawnReason.COMMAND);check(moth!=null,"Actual registered pollinator factory");moth.snapTo(2.5,31,.5,0,0);moth.setPersistenceRequired();l.addFreshEntity(moth);});
    boolean bloom=false;for(int i=0;i<80;i++){c.waitTicks(5);if(w.getServer().computeOnServer(s->s.overworld().getBlockState(P).is(WetlandGarden.REED)&&s.overworld().getBlockState(P).getValue(MoonreedBlock.AGE)==2)){bloom=true;break;}}
    check(bloom,"Actual unpaused Glimmerwing travels and pollinates after all lease faults; no invented maturity");
   }finally{mode.set(0);owner.set(null);w.getServer().runOnServer(s->current(original));}
  }
 }
 private static void reset(ServerLevel l,ServerPlayer p,int age){p.setGameMode(GameType.SURVIVAL);var eye=new Vec3(.5,30+p.getEyeHeight(),2.5);var d=new Vec3(.5,30.15,.5).subtract(eye);p.teleportTo(l,.5,30,2.5,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);l.setBlock(P,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,age),2);}
 private static int items(ServerLevel l,ServerPlayer p){int n=p.getInventory().countItem(WetlandGarden.FLOSS);for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(10)))if(e.getItem().is(WetlandGarden.FLOSS))n+=e.getItem().getCount();return n;}
 @SuppressWarnings("unchecked")private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();var types=new Class<?>[parts.length];var args=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();args[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig value){try{var field=Config.class.getDeclaredField("current");field.setAccessible(true);field.set(null,value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
