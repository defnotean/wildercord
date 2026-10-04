package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Actual source departure, foreign-owner, dimension and ordinary death retire preparation without phantom protection. */
public final class MossveilLifecycleTest implements FabricClientGameTest{
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c)){
  // Supplied ownership fixture exercises actual active full-AI navigation before replacing authority.
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");var source=new MossveilDormouse[1];var foreign=new net.minecraft.server.level.ServerPlayer[1];
   w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,6.5,.5);source[0]=mouse(s.overworld(),.5,.5);source[0].tame(p);source[0].setOrderedToSit(false);});
   await(c,w,s->source[0].followingOwner(),40,"Actual unpaused owned follow goal starts toward original distant owner");
   w.getServer().runOnServer(s->{var p=player(s);var old=source[0];check(old.isOwnedBy(p)&&old.followingOwner(),"Positive actual original owner follow control precedes replacement");foreign[0]=guest(p);place(foreign[0],40.5,.5);old.setOwner(foreign[0]);check(!old.isOwnedBy(p)&&old.isOwnedBy(foreign[0]),"Actual retained creature owner reference changes during active navigation");});c.waitTicks(5);
   w.getServer().runOnServer(s->{check(!source[0].followingOwner()&&source[0].isOwnedBy(foreign[0]),"Fresh owner identity retires cached old-owner follow; distant new owner cannot trigger catch-up teleport");foreign[0].discard();});
  }
  for(int mode=0;mode<5;mode++){final int scenario=mode;try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");var source=new MossveilDormouse[1];var worn=new ItemStack[1];
   w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,1.5,-.5);source[0]=mouse(s.overworld(),.5,.5);source[0].tame(p);source[0].setOrderedToSit(true);worn[0]=new ItemStack(MossveilCowl.ITEM);p.setItemSlot(EquipmentSlot.HEAD,worn[0]);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));});await(c,w,s->source[0].isInSittingPose(),30,"Real supplied owner order settles before lifecycle preparation");
   c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(20);
   w.getServer().runOnServer(s->{var p=player(s);var pet=source[0];check(worn[0].getDamageValue()==0,"Preparation has not paid before actual lifecycle event");
    if(scenario==0)pet.snapTo(7.5,101,4.5,0,0);
    if(scenario==1){var guest=guest(p);pet.setOwner(guest);check(!pet.isOwnedBy(p)&&pet.isOwnedBy(guest),"Actual new foreign owner reference");}
    if(scenario==2){var nether=s.getLevel(Level.NETHER);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){nether.setBlock(new net.minecraft.core.BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=104;y++)nether.setBlock(new net.minecraft.core.BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}p.teleportTo(nether,.5,101,.5,Set.<Relative>of(),0,0,false);check(pet.level()==s.overworld(),"Actual owner dimension departure cannot move the companion");}
    if(scenario==3){check(pet.hurtServer(s.overworld(),s.overworld().damageSources().generic(),100),"Actual ordinary companion death admitted");check(!pet.isAlive(),"Actual dead companion source");}
    if(scenario==4){pet.setOrderedToSit(false);check(p.hurtServer(p.level(),p.level().damageSources().generic(),100),"Actual owner death admitted");check(!p.isAlive(),"Actual owner died before filter commitment");}
   });c.waitTicks(45);
   w.getServer().runOnServer(s->{var p=player(s);check(worn[0].getDamageValue()==0&&p.getAttachedOrElse(MossveilCowl.READY,0L)==0&&source[0].filterReady()==0,"Actual departed/foreign/world/dead authority produces no cowl wear/rest/protection");if(scenario==2||scenario==4)check(!source[0].followingOwner()&&source[0].level()==s.overworld(),"Actual owner departure/death retires the specific follow goal without teleport; ordinary independent wandering may continue");});c.runOnClient(mc->mc.options.keyShift.setDown(false));
  }}
 }}
}
