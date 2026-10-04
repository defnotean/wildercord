package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import java.util.*;
/** Actual Survival packets, finite food payment, owner curl/follow and full-world ownership/rest reopening. */
public final class MossveilCompanionTest implements FabricClientGameTest{
 private MossveilDormouse mouse;private UUID identity,owner,claim;private long feedRest,claimRest;
 public void runTest(ClientGameTestContext c){TestWorldSave partial,complete;
  try(var settings=new TidewardNative(c)){
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");
    w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,1.5,-.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,6));mouse=mouse(s.overworld(),.5,.5);identity=mouse.getUUID();owner=p.getUUID();});c.waitTicks(5);
    interact(c,mouse.getId());w.getServer().runOnServer(s->{var p=player(s);check(!mouse.isTame()&&mouse.feedings()==1&&mouse.claimant().equals(p.getUUID())&&p.getMainHandItem().getCount()==5,"First real packet reserves actual feeder and consumes one Gill");feedRest=mouse.feedReady();claimRest=mouse.claimUntil();claim=mouse.claimant();});
    interact(c,mouse.getId());w.getServer().runOnServer(s->check(player(s).getMainHandItem().getCount()==5&&mouse.feedings()==1,"Repeated packet during real rest gives no food debit/progress"));
    w.getServer().runOnServer(s->{var guest=guest(player(s));guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,2));check(!mouse.mobInteract(guest,InteractionHand.MAIN_HAND).consumesAction()&&guest.getMainHandItem().getCount()==2,"Foreign actual player cannot steal a reserved feeding or lose food");guest.discard();});partial=w.getWorldSave();
   }
   try(var w=partial.open()){
    c.waitTicks(25);w.getServer().runOnServer(s->{mouse=(MossveilDormouse)s.overworld().getEntity(identity);check(mouse!=null&&mouse.feedings()==1&&mouse.claimant().equals(claim)&&mouse.claimUntil()==claimRest&&mouse.feedReady()==feedRest,"Partial feeding claimant/progress/exact deadlines survive full server reopen");check(player(s).getUUID().equals(owner)&&player(s).getMainHandItem().getCount()==5,"Actual original payer and paid inventory survive reopening");});
    interact(c,mouse.getId());w.getServer().runOnServer(s->check(player(s).getMainHandItem().getCount()==5&&mouse.feedings()==1,"Actual packet after reopen cannot bypass retained feeding rest"));
    await(c,w,s->MossveilDormouse.clock(s.overworld())>=mouse.feedReady(),130,"Actual foreign challenge occurs after feeding rest rather than masking claim admission");
    w.getServer().runOnServer(s->{var guest=guest(player(s));guest.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,2));check(!mouse.mobInteract(guest,InteractionHand.MAIN_HAND).consumesAction()&&guest.getMainHandItem().getCount()==2&&mouse.feedings()==1&&mouse.claimant().equals(player(s).getUUID()),"Actual foreign player refuses at live reserved claim even after feeding rest expires");guest.discard();});
    for(int i=0;i<2;i++){await(c,w,s->MossveilDormouse.clock(s.overworld())>=mouse.feedReady(),130,"Real reopened feeding rest elapsed");interact(c,mouse.getId());}
    await(c,w,s->mouse.isInSittingPose(),30,"Third genuine feeding leads to ordinary owner curl");
    w.getServer().runOnServer(s->{check(mouse.isTame()&&mouse.isOwnedBy(player(s))&&player(s).getMainHandItem().getCount()==3,"Exactly three actual Gills tame the original player companion");check(!mouse.canFallInLove()&&!mouse.isFood(new ItemStack(FungalGarden.GILLS))&&mouse.getBreedOffspring(s.overworld(),mouse)==null,"No breeding or bonus offspring route");player(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var guest=guest(player(s));check(!mouse.mobInteract(guest,InteractionHand.MAIN_HAND).consumesAction()&&mouse.isOrderedToSit(),"Actual foreign empty-hand interaction cannot change owner curl");guest.discard();});c.waitTicks(3);
    interact(c,mouse.getId());w.getServer().runOnServer(s->{check(!mouse.isOrderedToSit(),"Owner's actual empty-hand packet releases curl");place(player(s),6.5,.5);});
    await(c,w,s->mouse.distanceToSqr(player(s))<9,160,"Actual unpaused custom navigation follows its owner without teleport");
    w.getServer().runOnServer(s->{check(!mouse.shouldTryTeleportToOwner(),"Companion has no catch-up teleport policy");place(player(s),mouse.getX()+1,mouse.getZ());});c.waitTicks(3);interact(c,mouse.getId());await(c,w,s->mouse.isInSittingPose(),30,"Actual owner curl settles after following");
    w.getServer().runOnServer(s->{feedRest=mouse.feedReady();check(mouse.isOwnedBy(player(s)),"Actual owned source before complete save");});complete=w.getWorldSave();
   }
   try(var w=complete.open()){c.waitTicks(25);w.getServer().runOnServer(s->{mouse=(MossveilDormouse)s.overworld().getEntity(identity);check(mouse!=null&&mouse.isOwnedBy(player(s))&&mouse.isOrderedToSit()&&mouse.feedReady()==feedRest,"Owner UUID/order/exact food rest survive full server reopening");var p=player(s);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS));});c.waitTicks(3);interact(c,mouse.getId());w.getServer().runOnServer(s->check(player(s).getMainHandItem().getCount()==1,"Full-health companion feeding refuses without printing resources or eating food"));}
  }
 }
}
