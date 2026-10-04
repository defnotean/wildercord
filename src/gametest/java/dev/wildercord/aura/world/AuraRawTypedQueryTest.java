package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real indexed rejected prefixes and exact-source exclusion; admission counts, not a timing benchmark. */
public final class AuraRawTypedQueryTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");
  w.getServer().runOnServer(s->{var l=s.overworld();var viewer=s.getPlayerList().getPlayers().getFirst();viewer.setGameMode(GameType.CREATIVE);viewer.teleportTo(l,20,101,20,Set.<Relative>of(),0,0,false);
   for(int x=-5;x<=8;x++)for(int z=-5;z<=8;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
   var area=new AABB(-5,100,-5,8,106,8);var calls=new AtomicInteger();var scraps=new ArrayList<ItemEntity>();
   for(int i=0;i<256;i++){var e=new ItemEntity(l,i%4+.5,101,i/4%4+.5,new ItemStack(Items.STONE));check(l.addFreshEntity(e),"Actual rejected item admitted to native index");scraps.add(e);}
   var food=new ItemEntity(l,.5,101,.5,new ItemStack(Items.RABBIT));check(l.addFreshEntity(food),"Actual eligible food admitted to native index");
   var saturated=AuraBeastQueries.complete(l,ItemEntity.class,area,e->{calls.incrementAndGet();return e.getItem().is(Items.RABBIT);});
   check(saturated.isEmpty()&&calls.get()==0,"256 real nonfood items saturate raw pool before semantic predicates or hidden eligible food selection");
   for(int i=11;i<scraps.size();i++)scraps.get(i).discard();calls.set(0);
   var admitted=AuraBeastQueries.complete(l,ItemEntity.class,area,e->{calls.incrementAndGet();return e.getItem().is(Items.RABBIT);});
   check(calls.get()==12&&admitted.size()==1&&admitted.getFirst()==food,"Actual twelve-item complete pool retains ordinary food selection after bounded filtering");
   for(var e:scraps)if(!e.isRemoved())e.discard();food.discard();
   var source=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);check(source!=null,"Actual known source factory");source.setNoAi(true);source.snapTo(.5,101,.5,0,0);check(l.addFreshEntity(source),"Actual known source admitted to native index");
   var victims=new ArrayList<Zombie>();for(int i=0;i<12;i++){var e=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Actual normal receiver factory");e.setCustomName(net.minecraft.network.chat.Component.literal("Native supplied Aura receiver"));e.setNoAi(true);e.snapTo(i%4+.5,101,i/4+.5,0,0);check(l.addFreshEntity(e),"Actual normal receiver indexed");victims.add(e);}
   var rejected=new ArrayList<Stonehorn>();for(int i=0;i<256;i++){var e=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Actual same-species factory");e.setNoAi(true);e.snapTo(i%4+.5,101,i/4%4+.5,0,0);check(l.addFreshEntity(e),"Actual same-species rejected receiver indexed");rejected.add(e);}
   calls.set(0);var dense=AuraBeastQueries.complete(l,LivingEntity.class,area,source,e->{calls.incrementAndGet();return source.valid(e)&&!(e instanceof Stonehorn);});
   check(dense.isEmpty()&&calls.get()==0,"256 real same-species receivers count before semantic attack exclusions; only exact unique source is excluded before cap");
   for(var e:rejected)e.discard();calls.set(0);
   var normal=AuraBeastQueries.complete(l,LivingEntity.class,area,source,e->{calls.incrementAndGet();return source.valid(e)&&!(e instanceof Stonehorn);});
   check(calls.get()==12&&normal.size()==12&&new HashSet<>(normal).equals(new HashSet<>(victims)),"Actual unique source plus twelve ordinary receivers preserves intended complete twelve-peer attack admission");
  });
 }}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
