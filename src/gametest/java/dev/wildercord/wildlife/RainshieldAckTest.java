package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import dev.wildercord.client.fx.RainshieldClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
/** Declared native ingress-delay fault using the identical actual server event; no artificial visual/catch/timing/source state. */
public final class RainshieldAckTest implements FabricClientGameTest {
 private static volatile boolean delaying;private static UUID expected;
 private static final AtomicReference<RainshieldFx.Event> RECEIPT=new AtomicReference<>();
 public static boolean hold(RainshieldFx.Event e,Minecraft mc){if(!delaying||mc.player==null||expected==null||!expected.equals(e.carrier())||!mc.player.getUUID().equals(e.carrier()))return false;if(e.beat()==0)RECEIPT.compareAndSet(null,e);return true;}
 private static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 private static void use(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.setScreen(null);mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});}
 private static void deliver(RainshieldFx.Event e,Minecraft mc){try{var method=RainshieldClient.class.getDeclaredMethod("receive",RainshieldFx.Event.class,Minecraft.class);method.setAccessible(true);method.invoke(null,e,mc);}catch(ReflectiveOperationException error){throw new AssertionError(error);}}
 private static UUID armed(){try{var field=RainshieldClient.class.getDeclaredField("armed");field.setAccessible(true);return (UUID)field.get(null);}catch(ReflectiveOperationException error){throw new AssertionError(error);}}
 public void runTest(ClientGameTestContext c){try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-7;x<=7;x++)for(int z=-4;z<=4;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}var p=p(s);p.setGameMode(GameType.SURVIVAL);dev.wildercord.player.Spellbooks.set(p,dev.wildercord.player.Spellbooks.get(p).withStarterGiven());p.teleportTo(l,.5,101,.5,Set.<Relative>of(),90,0,false);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RooksRainshield.ITEM));p.setHealth(20);expected=p.getUUID();});c.waitTicks(6);RECEIPT.set(null);delaying=true;use(c);c.waitTicks(64);
  RainshieldFx.Event original=RECEIPT.get();check(original!=null,"Gametest ingress captured an actual immutable server draw cue");
  w.getServer().runOnServer(s->{check(s.overworld().getGameTime()-original.tick()>40,"Identical genuine acknowledgement is now older than the cosmetic40tick admission");check(!RooksRainshield.active(p(s))&&p(s).getMainHandItem().getDamageValue()==0&&p(s).getAttachedOrElse(RooksRainshield.READY,0L)==0,"Real original gesture expires under held input with no renewed defense or price");});
  c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(5);use(c);c.waitTicks(16);w.getServer().runOnServer(s->check(!RooksRainshield.active(p(s)),"With all real acknowledgements withheld, same actual stack remains release-latched rather than silently reopening"));
  c.runOnClient(mc->mc.options.keyUse.setDown(false));c.waitTicks(5);c.runOnClient(mc->{delaying=false;deliver(original,mc);});c.waitTicks(4);
  use(c);c.waitTicks(16);w.getServer().runOnServer(s->check(RooksRainshield.active(p(s)),"Late actual own-draw cue after key-up closes its exact stale input latch and permits a fresh real gesture"));
  UUID next=c.computeOnClient(mc->armed());check(next!=null&&!next.equals(original.draw()),"Actual new draw obtains a distinct server nonce");
  c.runOnClient(mc->{deliver(original,mc);check(next.equals(armed()),"Older genuine server generation cannot replace current own input nonce");});c.waitTicks(3);
  w.getServer().runOnServer(s->{check(RooksRainshield.active(p(s)),"Late old acknowledgement cannot clear renewed exact server draw");var arrow=EntityTypes.ARROW.create(s.overworld(),EntitySpawnReason.COMMAND);check(arrow!=null,"Actual vanilla projectile factory");arrow.snapTo(-3.5,102.1,.5,0,0);arrow.setDeltaMovement(new Vec3(.65,0,0));arrow.setBaseDamage(2);check(s.overworld().addFreshEntity(arrow),"Actual positive arrow enters the native collision path");});c.waitTicks(12);
  w.getServer().runOnServer(s->check(p(s).getHealth()==20&&p(s).getMainHandItem().getDamageValue()==4&&p(s).getAttachedOrElse(RooksRainshield.READY,0L)>s.overworld().getGameTime(),"Delayed own-input receipt repair retains exact real arrow price and protection"));
 }finally{delaying=false;expected=null;RECEIPT.set(null);}}
}
