package dev.wildercord.wildlife;
import dev.wildercord.player.*;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import java.util.*;
/** Six finite discovery flags and one claimed reward, shared across sites and retained through death. */
public final class FungalInvestigation {
 private FungalInvestigation() {}
 public static final String FIRST="field:sporeback",ROOT="field:breathmark_root",AIR="field:breathmark_air",HARVEST="field:glowcap_harvest",ROOF="field:fungal_nursery",CRAFT="field:cave_breather",DONE="field:three_breathmarks";
 public static boolean knows(ServerPlayer p,String key) {return Heart.grimoire(p).contains(key);}
 public static boolean remember(ServerPlayer p,String key) {if(knows(p,key))return false;var entries=new ArrayList<>(Heart.grimoire(p));entries.add(key);p.setAttached(WildercordAttachments.GRIMOIRE,List.copyOf(entries));return true;}
 private static boolean actor(ServerPlayer p) {return p.isAlive() && !p.isSpectator() && !p.getAbilities().instabuild;}
 private static boolean visible(ServerPlayer p,BlockPos at) {var hit=p.level().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(at),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));return hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(at);}
 private static void give(ServerPlayer p,ItemStack stack) {p.getInventory().placeItemBackInInventory(stack,net.minecraft.util.Prediction.SERVER_ONLY);}
 public static void read(ServerPlayer p,BlockPos at) {
  if(!actor(p) || !p.level().hasChunkAt(at) || p.distanceToSqr(at.getX()+.5,at.getY()+.5,at.getZ()+.5)>16 || !p.level().mayInteract(p,at) || !visible(p,at))return;
  if(!(p.level().getBlockEntity(at) instanceof BreathmarkEntity e) || !e.authentic()) {p.sendOverlayMessage(Component.translatable("message.wildercord.fungal.replica"));return;}
  if(!knows(p,FIRST)) {hint(p);return;}
  int kind=p.level().getBlockState(at).getValue(BreathmarkBlock.KIND);String key=kind==0?ROOT:AIR;
  if(remember(p,key)) {give(p,new ItemStack(kind==0?FungalGarden.ROOT_NOTES:FungalGarden.AIR_NOTES));Feels.sound(p.level(),p.position(),"fungal_mark_read",.45F,1);}
  hint(p);
 }
 public static String next(ServerPlayer p) {for(var k:List.of(FIRST,ROOT,AIR,HARVEST,ROOF,CRAFT))if(!knows(p,k))return switch(k) {case FIRST -> "snail";case ROOT -> "root";case AIR -> "air";case HARVEST -> "harvest";case ROOF -> "roof";default -> "breather";};return knows(p,DONE)?"done":"return";}
 public static void hint(ServerPlayer p) {p.sendOverlayMessage(Component.translatable("message.wildercord.fungal.hint_"+next(p)));}
 public static boolean finish(ServerPlayer p,BlockPos at) {
  if(!actor(p) || !(p.getMainHandItem().is(FungalGarden.BREATHER) || p.getOffhandItem().is(FungalGarden.BREATHER)) || !p.level().hasChunkAt(at) || !p.level().getBlockState(at).is(FungalGarden.NURSERY) || p.distanceToSqr(at.getX()+.5,at.getY()+.5,at.getZ()+.5)>16 || !p.level().mayInteract(p,at) || !visible(p,at))return false;
  if(!next(p).equals("return")) {hint(p);return false;}
  remember(p,DONE);give(p,new ItemStack(FungalGarden.CONCLUSION));give(p,new ItemStack(dev.wildercord.content.WildercordItems.BLANK_RUNE,3));Feels.sound(p.level(),p.position(),"fungal_nursery_settle",.5F,1);hint(p);return true;
 }
 public static void harvested(ServerPlayer p) {if(actor(p) && knows(p,ROOT) && knows(p,AIR)) {remember(p,HARVEST);hint(p);}}
 public static void placed(ServerPlayer p) {if(actor(p) && knows(p,HARVEST)) {remember(p,ROOF);hint(p);}}
 public static void crafted(ServerPlayer p) {if(actor(p) && knows(p,ROOF)) {remember(p,CRAFT);hint(p);}}
}
