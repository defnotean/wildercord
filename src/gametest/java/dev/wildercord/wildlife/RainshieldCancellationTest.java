package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
/** Real held-use, native walk/jump and new-stack cancellation; non-Survival native request refusal. */
public final class RainshieldCancellationTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c)){for(int kind=0;kind<6;kind++){final int mode=kind;try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-5;z<=5;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=player(s);p.teleportTo(l,.5,101,.5,Set.of(),90,0,false);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RooksRainshield.ITEM));p.setGameMode(mode==3?GameType.ADVENTURE:mode==4?GameType.CREATIVE:mode==5?GameType.SPECTATOR:GameType.SURVIVAL);p.setHealth(20);});c.waitTicks(6);c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});c.waitTicks(16);
  if(mode<3){w.getServer().runOnServer(s->check(RooksRainshield.active(player(s)),"Cancellation starts after actual planted preparation"));if(mode==0){try(var walk=held(c,o->o.keyUp)){c.waitTicks(6);}}else if(mode==1){try(var jump=held(c,o->o.keyJump)){c.waitTicks(3);}w.getServer().runOnServer(s->check(!player(s).onGround(),"Actual native jump leaves ground"));}else{w.getServer().runOnServer(s->{var old=player(s).getMainHandItem();player(s).getInventory().setItem(8,new ItemStack(RooksRainshield.ITEM));check(old!=player(s).getInventory().getItem(8),"Fresh actual copy has different stack identity");});c.waitTicks(3);c.getInput().pressKey(o->o.keyHotbarSlots[8]);c.waitTicks(3);}
   c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(2);w.getServer().runOnServer(s->check(!RooksRainshield.active(player(s)),"Actual walk/jump/hotbar change cancels prepared cover"));
  }else{w.getServer().runOnServer(s->{check(!RooksRainshield.active(player(s)),"Actual non-Survival native item request cannot start cover");check(RooksRainshield.ITEM.use(s.overworld(),player(s),InteractionHand.MAIN_HAND)==InteractionResult.FAIL,"Authoritative Item.use also refuses non-Survival actor directly");});c.runOnClient(mc->mc.options.keyUse.setDown(false));}
  w.getServer().runOnServer(s->{var p=player(s);check(p.getMainHandItem().getDamageValue()==0&&p.getAttachedOrElse(RooksRainshield.READY,0L)==0,"Refusal/cancellation cannot spend wear or reserve rest");if(mode<4){var a=EntityTypes.ARROW.create(s.overworld(),EntitySpawnReason.COMMAND);check(a!=null,"Actual vanilla arrow");a.snapTo(p.getX()-4,p.getY()+1.1,p.getZ(),0,0);a.setDeltaMovement(new Vec3(.65,0,0));a.setBaseDamage(2);s.overworld().addFreshEntity(a);}});c.waitTicks(12);if(mode<4)w.getServer().runOnServer(s->check(player(s).getHealth()<20&&player(s).getMainHandItem().getDamageValue()==0&&player(s).getAttachedOrElse(RooksRainshield.READY,0L)==0,"Normal real arrow damage remains after cancelled/unavailable defense"));
 }}}}
 private static KeyHold held(ClientGameTestContext c,java.util.function.Function<net.minecraft.client.Options,net.minecraft.client.KeyMapping> key){return new KeyHold(c,key);}
 private static final class KeyHold implements AutoCloseable {
  private final ClientGameTestContext c;private final java.util.function.Function<net.minecraft.client.Options,net.minecraft.client.KeyMapping> key;
  KeyHold(ClientGameTestContext c,java.util.function.Function<net.minecraft.client.Options,net.minecraft.client.KeyMapping> key){this.c=c;this.key=key;c.getInput().holdKey(key);}
  @Override public void close(){c.getInput().releaseKey(key);}
 }
}
