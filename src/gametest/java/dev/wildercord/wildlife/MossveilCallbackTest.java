package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.MossveilNative.*;
import dev.wildercord.client.fx.MossveilFilterClient;
import dev.wildercord.mixin.MossveilEffectAccess;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
/** Real runtime preparation with actual mapped post-wear/post-notification fault callbacks; not pure ledger tests. */
public final class MossveilCallbackTest implements FabricClientGameTest{
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c)){
  for(boolean notice:new boolean[]{false,true})for(int mode:new int[]{1,2,3,4,5,6}){int scenario=mode;
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
    var holder=new MossveilDormouse[1];var paid=new ItemStack[1];var original=new MobEffectInstance[1];var hidden=new MobEffectInstance[1];
    w.getServer().runOnServer(s->{floor(s.overworld());var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,1.5,-.5);var pet=mouse(s.overworld(),.5,.5);holder[0]=pet;
     // Supplied owner fixture isolates synchronous callback semantics; actual three-Gill packets have their separate native gate.
     pet.tame(p);pet.setOrderedToSit(true);paid[0]=new ItemStack(MossveilCowl.ITEM);p.setItemSlot(EquipmentSlot.HEAD,paid[0]);
     var weaker=new MobEffectInstance(MobEffects.POISON,1200,0);p.addEffect(new MobEffectInstance(MobEffects.POISON,600,1,true,false,false,weaker));original[0]=p.getEffect(MobEffects.POISON);hidden[0]=((MossveilEffectAccess)(Object)original[0]).mossveil$hidden();check(hidden[0]!=null,"Actual supplied poison includes a real hidden weaker chain");
    });await(c,w,s->holder[0].isInSittingPose(),30,"Ordinary supplied owner order settles through actual AI");
    long cues=c.computeOnClient(mc->MossveilFilterClient.accepted());w.getServer().runOnServer(s->MossveilNativeFaults.arm(player(s),paid[0],holder[0],scenario,notice));c.runOnClient(mc->mc.options.keyShift.setDown(true));
    await(c,w,s->MossveilNativeFaults.fired==1,90,"Actual postwear/effect notification callback fired during a genuine40-tick filter");c.waitTicks(3);
    w.getServer().runOnServer(s->{var p=player(s);var active=p.getEffect(MobEffects.POISON);
     check(paid[0].getDamageValue()==1&&p.getAttachedOrElse(MossveilCowl.READY,0L)>MossveilDormouse.clock(s.overworld())&&holder[0].filterReady()==p.getAttachedOrElse(MossveilCowl.READY,0L),"Actual callback pays one wear and shared saved rests exactly once without refunds");
     check(!p.hasEffect(MobEffects.SLOWNESS),"Callback-invalidated filter admits no false successful slow-breath outcome");
     if(scenario==3)check(active!=null&&active.getAmplifier()==2&&active.getDuration()>900,"Actual stronger same-instance Poison callback remains untrimmed after its replacement");
     else check(active==original[0]&&((MossveilEffectAccess)(Object)active).mossveil$hidden()==hidden[0]&&hidden[0].getAmplifier()==0,"Actual bounded edit/refusal preserves active identity and hidden-chain object");
     if(!notice&&scenario!=3)check(active.getDuration()>500,"Postwear authority refusal leaves active poison untrimmed beyond ordinary preparation ticks");
    });check(c.computeOnClient(mc->MossveilFilterClient.accepted())==cues,"Callback-invalidated mutation emits no success cue");
    c.runOnClient(mc->mc.options.keyShift.setDown(false));
   }finally{MossveilNativeFaults.clear();}
  }
 }}
}
