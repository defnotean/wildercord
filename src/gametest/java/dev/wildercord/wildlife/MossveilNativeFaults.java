package dev.wildercord.wildlife;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Native-only synchronous callbacks around actual mapped wear and effect notification, inert unless armed. */
public final class MossveilNativeFaults{
 static UUID payer;static ItemStack worn;static MossveilDormouse source;static int mode,fired;static boolean notice;
 static void arm(ServerPlayer p,ItemStack stack,MossveilDormouse pet,int requested,boolean afterNotice){payer=p.getUUID();worn=stack;source=pet;mode=requested;fired=0;notice=afterNotice;}
 static void clear(){payer=null;worn=null;source=null;mode=0;notice=false;}
 public static void callback(ServerPlayer p,boolean afterNotice,ItemStack actual){
  if(mode==0||payer==null||!payer.equals(p.getUUID())||afterNotice!=notice||(!afterNotice&&actual!=worn))return;
  int request=mode;mode=0;fired++;
  if(request==1)source.discard();
  if(request==2)p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(MossveilCowl.ITEM));
  if(request==3)p.addEffect(new MobEffectInstance(MobEffects.POISON,1000,2));
  if(request==4)source.snapTo(source.getX()+.25,source.getY(),source.getZ(),0,0);
  if(request==5)p.teleportTo(p.level(),p.getX()+.25,p.getY(),p.getZ(),Set.<net.minecraft.world.entity.Relative>of(),p.getYRot(),p.getXRot(),false);
  if(request==6){check(source.hurtServer(p.level(),p.level().damageSources().generic(),100),"Actual admitted source death callback");check(!source.isAlive(),"Source actually died in the native callback");}
 }
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
