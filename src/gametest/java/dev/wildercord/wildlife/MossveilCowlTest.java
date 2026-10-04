package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Actual recipe packet, worn preparation/payment, callback refusal and full-server shared-rest reopening. */
public final class MossveilCowlTest implements FabricClientGameTest{
 private MossveilDormouse pet;private UUID identity;private long playerRest,petRest;private final AtomicInteger fault=new AtomicInteger();private ItemStack worn;private int startPoison;private MobEffectInstance hidden;
 public void runTest(ClientGameTestContext c){TestWorldSave save;
  PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->{
   if(!(p instanceof net.minecraft.server.level.ServerPlayer sp)||!at.equals(CANOPY))return true;
   int mode=fault.getAndSet(0);
   if(mode==1)return false;
   if(mode==2){MossveilCowl.tick(sp);MossveilCowl.tick(sp);return false;}
   if(mode==3){sp.setItemSlot(EquipmentSlot.HEAD,new ItemStack(MossveilCowl.ITEM));return true;}
   if(mode==4){sp.addEffect(new MobEffectInstance(MobEffects.POISON,1000,2));return true;}
   if(mode==5){l.setBlock(CANOPY.below(2),Blocks.AIR.defaultBlockState(),2);return true;}
   if(mode==6){pet.discard();return true;}return true;
  });
  try(var settings=new TidewardNative(c)){
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
    w.getServer().runOnServer(s->{floor(s.overworld());pet=mouse(s.overworld(),.5,.5);identity=pet.getUUID();});tame(c,w,pet);
    w.getServer().runOnServer(s->{var p=player(s);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var inputs=List.of(new ItemStack(WetlandGarden.FLOSS),new ItemStack(FungalGarden.GILLS),new ItemStack(SporebackContent.DEW),ItemStack.EMPTY);for(int i=0;i<4;i++)p.inventoryMenu.getSlot(i+1).set(inputs.get(i));p.inventoryMenu.broadcastChanges();check(p.inventoryMenu.getSlot(0).getItem().is(MossveilCowl.ITEM),"Actual native recipe needs existing floss, Gill and Dew");});c.waitTicks(5);
    c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(5);
    w.getServer().runOnServer(s->{var p=player(s);check(p.getInventory().countItem(MossveilCowl.ITEM)==1,"Actual client crafting pickup produces one cowl");for(int i=1;i<=3;i++)check(p.inventoryMenu.getSlot(i).getItem().isEmpty(),"Actual recipe consumed every supplied ingredient once");ItemStack result=null;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(MossveilCowl.ITEM)){result=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);break;}check(result!=null,"Actual crafted cowl retained");p.setItemSlot(EquipmentSlot.HEAD,result);worn=result;place(p,1.5,-.5);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,1,true,false,false,new MobEffectInstance(MobEffects.POISON,1200,0)));hidden=((dev.wildercord.mixin.MossveilEffectAccess)(Object)p.getEffect(MobEffects.POISON)).mossveil$hidden();check(hidden!=null,"Positive actual filter fixture carries real weaker hidden poison");startPoison=p.getEffect(MobEffects.POISON).getDuration();});
    c.runOnClient(mc->mc.options.keyShift.setDown(true));
    await(c,w,s->player(s).getAttachedOrElse(MossveilCowl.READY,0L)>0,90,"Forty genuine stationary crouch ticks beside owned curled mouse and supported Nursery pay the cowl");
    w.getServer().runOnServer(s->{var p=player(s);var actual=p.getEffect(MobEffects.POISON);check(worn.getDamageValue()==1&&actual!=null&&actual.getAmplifier()==1&&actual.isAmbient()&&!actual.isVisible()&&!actual.showIcon(),"One actual wear preserves original poison amplifier/flags");check(((dev.wildercord.mixin.MossveilEffectAccess)(Object)actual).mossveil$hidden()==hidden&&hidden.getAmplifier()==0&&hidden.getDuration()>1000,"Successful actual in-place trim retains the weaker hidden effect chain rather than deleting its pending poison");check(startPoison-actual.getDuration()>=100&&startPoison-actual.getDuration()<=160,"Actual effect shortened by sixty additional ticks beside ordinary preparation countdown");check(p.hasEffect(MobEffects.SLOWNESS),"Actual successful filter admits its brief vulnerable slow breath");playerRest=p.getAttachedOrElse(MossveilCowl.READY,0L);petRest=pet.filterReady();check(playerRest==petRest&&playerRest>MossveilDormouse.clock(s.overworld()),"Exact shared player and pet rest reserved");});
    c.waitTicks(5);w.getServer().runOnServer(s->check(worn.getDamageValue()==1,"Repeated preparation during saved rest cannot renew free filtering"));save=w.getWorldSave();
   }
   try(var w=save.open()){
    c.waitTicks(25);w.getServer().runOnServer(s->{pet=(MossveilDormouse)s.overworld().getEntity(identity);var p=player(s);worn=p.getItemBySlot(EquipmentSlot.HEAD);check(pet!=null&&pet.isOwnedBy(p)&&pet.filterReady()==petRest&&p.getAttachedOrElse(MossveilCowl.READY,0L)==playerRest&&worn.getDamageValue()==1,"World reopen preserves actual owned source, worn payment and both exact rest deadlines");});
    c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(45);w.getServer().runOnServer(s->check(worn.getDamageValue()==1,"Actual reopened crouch cannot bypass saved rest"));
    await(c,w,s->MossveilDormouse.clock(s.overworld())>=playerRest,220,"Ordinary shared rest expires without forced clocks");
    for(int mode:new int[]{1,2,3,4,5}){
     final int selected=mode;w.getServer().runOnServer(s->{var p=player(s);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(MossveilCowl.ITEM));worn=p.getItemBySlot(EquipmentSlot.HEAD);p.removeEffect(MobEffects.POISON);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));floor(s.overworld());place(p,1.5,-.5);fault.set(selected);});
     await(c,w,s->fault.get()==0,90,"Actual pre-payment home claim callback executes fault"+mode);
     w.getServer().runOnServer(s->{var p=player(s);check(worn.getDamageValue()==0&&p.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==0&&p.getAttachedOrElse(MossveilCowl.READY,0L)==playerRest&&pet.filterReady()==petRest,"Denied/reentrant/replaced source/effect/home compatibility callback charges no wear or new rest");if(selected==4)check(p.getEffect(MobEffects.POISON).getAmplifier()==2&&p.getEffect(MobEffects.POISON).getDuration()>900,"Stronger actual callback poison remains untrimmed");});
     // Break preparation through actual client input before repairing supplied fixture geometry.
     c.runOnClient(mc->mc.options.keyShift.setDown(false));c.waitTicks(3);c.runOnClient(mc->mc.options.keyShift.setDown(true));
    }
    w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(MossveilCowl.ITEM));worn=p.getItemBySlot(EquipmentSlot.HEAD);p.removeEffect(MobEffects.POISON);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,0));place(p,1.5,-.5);});
    // Real client crouch-walking supplies sub-threshold individual steps but defeats the planted total bound.
    var beforeWalk=w.getServer().computeOnServer(s->player(s).position());c.runOnClient(mc->mc.options.keyUp.setDown(true));c.waitTicks(45);c.runOnClient(mc->mc.options.keyUp.setDown(false));
    w.getServer().runOnServer(s->check(player(s).position().distanceToSqr(beforeWalk)>.0225,"Actual client crouch-walking displaced the wearer cumulatively"));
    w.getServer().runOnServer(s->check(worn.getDamageValue()==0&&player(s).getAttachedOrElse(MossveilCowl.READY,0L)==playerRest,"Slow creep cannot earn a stationary forty-tick filter"));
   }
  }finally{fault.set(0);}
 }
}
