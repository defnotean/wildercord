package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.config.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import dev.wildercord.client.fx.TidewardClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.atomic.*;

/** Honest compatibility fixtures: existing break-based claims, no invented native read-claim event. */
public final class TidewardReadAdmissionTest implements FabricClientGameTest {
 private static void use(ClientGameTestContext c,BlockPos at){c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at).add(0,.49,0),Direction.UP,at,false)));c.waitTicks(2);}
 @Override public void runTest(ClientGameTestContext c){var path=Config.path();String original;
  try{original=Files.exists(path)?Files.readString(path,StandardCharsets.UTF_8):null;}catch(IOException e){throw new UncheckedIOException(e);}
  var nestedSurvey=new AtomicBoolean(true);var nestedRead=new AtomicBoolean(true);var mode=new AtomicInteger();var owned=new AtomicReference<ServerLevel>();var target=new BlockPos(0,100,2);
  PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->{
   if(l!=owned.get() || !at.equals(target))return true;
   if(mode.get()==1)return false;
   if(mode.get()==2)l.setBlock(at,Blocks.AIR.defaultBlockState(),2);
   if(mode.get()==3)place((net.minecraft.server.level.ServerPlayer)p,40.5,101,2.5);
   if(mode.get()==4)l.setBlock(new BlockPos(0,100,0),Blocks.AIR.defaultBlockState(),2);
   if(mode.get()==5 && mode.compareAndSet(5,0))nestedSurvey.set(TidewardSurvey.survey((net.minecraft.server.level.ServerPlayer)p,new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(new BlockPos(0,100,4)).add(0,.49,0),Direction.UP,new BlockPos(0,100,4),false))));
   if(mode.get()==6 && mode.compareAndSet(6,0))nestedRead.set(TidewardReadAdmission.allows((net.minecraft.server.level.ServerPlayer)p,(ServerLevel)l,at));
   return true;
  });
  try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
   try{
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runOnServer(s -> {owned.set(s.overworld());floor(s.overworld());player(s).setGameMode(GameType.ADVENTURE);place(player(s),2.5,101,2.5);});c.waitTicks(3);
    String noEdits=WildercordConfig.DEFAULTS.toJson().replace("\"spells_edit_blocks\": true","\"spells_edit_blocks\": false");check(noEdits.contains("\"spells_edit_blocks\": false"),"Real config disables magic block edits");
    Files.writeString(path,noEdits,StandardCharsets.UTF_8);w.getServer().runCommand("wildercord reload");c.waitTicks(2);
    w.getServer().runOnServer(s -> {check(!Config.get().spellsEditBlocks() && !player(s).mayBuild(),"Genuine Adventure player has no magic edit or build permission");check(TidewardReadAdmission.allows(player(s),s.overworld(),target),"Read-only tool admission remains possible in actual Adventure with magic edits off");});
    w.getServer().runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardSurvey.ITEM)));c.waitTicks(4);
    long cues=c.computeOnClient(mc -> TidewardClient.routeAccepted());var first=new BlockPos(0,100,0);var last=new BlockPos(0,100,4);
    use(c,first);use(c,last);c.waitTicks(2);
    long paid=w.getServer().computeOnServer(s -> {check(player(s).getMainHandItem().getDamageValue()==2,"Actual Adventure client uses pay finite tool wear through the item-scoped callback");long ready=player(s).getAttachedOrElse(TidewardSurvey.READY,0L);check(ready>s.overworld().getGameTime(),"Adventure public delivery earns actual shared rest");return ready;});
    check(c.computeOnClient(mc -> TidewardClient.routeAccepted())==cues+1,"Real client Adventure delivery reaches the owner, not merely a direct gate call");
    await(c,w,s -> s.overworld().getGameTime()>=paid,85,"Actual elapsed time ends the public tool rest before another attempt");
    use(c,first);mode.set(4);use(c,last);mode.set(0);
    w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(first).isAir(),"Actual later-cell permission callback changed the already-read earlier floor");check(player(s).getMainHandItem().getDamageValue()==2 && player(s).getAttachedOrElse(TidewardSurvey.READY,0L)==paid,"Stale whole-route snapshot refusal adds no payment or deadline");s.overworld().setBlock(first,Blocks.STONE.defaultBlockState(),2);});
    check(c.computeOnClient(mc -> TidewardClient.routeAccepted())==cues+1,"A changed earlier cell produces no second success packet");
    // Genuine normal packet enters a claim callback that attempts the same pending anchor recursively.
    mode.set(5);use(c,last);mode.set(0);c.waitTicks(2);
    w.getServer().runOnServer(s -> {check(!nestedSurvey.get(),"Actual claim recursion refuses before duplicate rest, wear or publication");check(player(s).getMainHandItem().getDamageValue()==4 && player(s).getAttachedOrElse(TidewardSurvey.READY,0L)>paid,"One outer native use pays exactly one additional ordinary survey");});
    check(c.computeOnClient(mc -> TidewardClient.routeAccepted())==cues+2,"Recursive claim grants exactly one outer owner route, with no nested publication");
    mode.set(6);w.getServer().runOnServer(s -> check(TidewardReadAdmission.allows(player(s),s.overworld(),target) && !nestedRead.get(),"Real direct read callback recursion refuses its held cell while outer query remains valid"));mode.set(0);
    mode.set(1);w.getServer().runOnServer(s -> {check(!TidewardReadAdmission.allows(player(s),s.overworld(),target),"Actual existing protection callback denial still refuses reading");check(s.overworld().getBlockState(target).is(Blocks.STONE),"Claim refusal makes no terrain write");});
    mode.set(2);w.getServer().runOnServer(s -> check(!TidewardReadAdmission.allows(player(s),s.overworld(),target),"A real permission callback changing the admitted cell invalidates its snapshot"));
    mode.set(0);w.getServer().runOnServer(s -> s.overworld().setBlock(target,Blocks.STONE.defaultBlockState(),2));
    mode.set(3);w.getServer().runOnServer(s -> check(!TidewardReadAdmission.allows(player(s),s.overworld(),target),"Synchronous actual owner movement beyond range invalidates post-callback admission"));
    mode.set(0);w.getServer().runOnServer(s -> {place(player(s),2.5,101,2.5);player(s).setGameMode(GameType.SPECTATOR);check(!TidewardReadAdmission.allows(player(s),s.overworld(),target),"Actual spectator refuses without querying protected state");});
   }finally{mode.set(0);owned.set(null);if(original==null)Files.deleteIfExists(path);else Files.writeString(path,original,StandardCharsets.UTF_8);w.getServer().runCommand("wildercord reload");}
  }catch(IOException e){throw new UncheckedIOException(e);}
 }
}
