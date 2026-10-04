package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** Supplied combat scene; genuine vanilla arrow flight tests the authored 70-degree TOTAL cone. */
public final class RainshieldAngleTest implements FabricClientGameTest {
 private Arrow arrow;
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c)){
  for(int degrees:new int[]{25,45})try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");
   w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);dev.wildercord.player.Spellbooks.set(p,dev.wildercord.player.Spellbooks.get(p).withStarterGiven());p.teleportTo(l,.5,101,.5,Set.<Relative>of(),90,0,false);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RooksRainshield.ITEM));p.setHealth(20);p.getFoodData().setFoodLevel(20);check(p.getAttachedOrElse(RooksRainshield.READY,0L)==0,"Fresh world has no supplied rest");});
   c.waitTicks(6);c.runOnClient(mc->{mc.gui.setScreen(null);mc.player.setYRot(90);mc.player.setXRot(0);});
   try(var held=held(c,o->o.keyUse)){
    c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(16);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(RooksRainshield.active(p)&&p.getTicksUsingItem()>=RooksRainshield.PREP&&p.onGround(),"Actual held packets prepare a planted fan before angle "+degrees);double radians=Math.toRadians(degrees);Vec3 incoming=new Vec3(Math.cos(radians),0,Math.sin(radians));Vec3 start=p.position().subtract(incoming.scale(4)).add(0,1.1,0);arrow=EntityTypes.ARROW.create(s.overworld(),EntitySpawnReason.COMMAND);check(arrow!=null,"Actual vanilla arrow factory");arrow.snapTo(start.x,start.y,start.z,0,0);arrow.setDeltaMovement(incoming.scale(.65));arrow.setBaseDamage(2);check(s.overworld().addFreshEntity(arrow),"Actual angular projectile enters the world");});
    c.waitTicks(12);w.getServer().runOnServer(s->{ServerPlayer p=s.getPlayerList().getPlayers().getFirst();long ready=p.getAttachedOrElse(RooksRainshield.READY,0L);String receipt=" angle="+degrees+" health="+p.getHealth()+" wear="+p.getMainHandItem().getDamageValue()+" ready="+ready+" now="+s.overworld().getGameTime()+" arrow="+arrow.position()+" removed="+arrow.isRemoved();
     if(degrees==25){check(arrow.isRemoved()&&p.getHealth()==20&&p.getMainHandItem().getDamageValue()==RooksRainshield.WEAR&&!RooksRainshield.active(p),"25-degree actual frontal flight is caught once:"+receipt);check(ready>s.overworld().getGameTime()&&ready<=s.overworld().getGameTime()+RooksRainshield.REST,"Positive angular catch earns finite actual rest:"+receipt);}
     else check(p.getHealth()<20&&p.getMainHandItem().getDamageValue()==0&&ready==0,"45-degree physical flight lies outside half35 cone and damages without catch price:"+receipt);
    });
   }
  }
 }}
 private static KeyHold held(ClientGameTestContext c,java.util.function.Function<net.minecraft.client.Options,net.minecraft.client.KeyMapping> key){return new KeyHold(c,key);}
 private static final class KeyHold implements AutoCloseable {
  private final ClientGameTestContext c;private final java.util.function.Function<net.minecraft.client.Options,net.minecraft.client.KeyMapping> key;
  KeyHold(ClientGameTestContext c,java.util.function.Function<net.minecraft.client.Options,net.minecraft.client.KeyMapping> key){this.c=c;this.key=key;c.getInput().holdKey(key);}
  @Override public void close(){c.getInput().releaseKey(key);}
 }
}
