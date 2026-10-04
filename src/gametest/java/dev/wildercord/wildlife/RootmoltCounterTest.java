package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
/** Draft actual warning/contact/physical interruption/effect isolation. Relic item packets have their own future gate. */
public final class RootmoltCounterTest implements FabricClientGameTest {
 private RootmoltStrider root;
 public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}var cap=new BlockPos(0,30,0);l.setBlock(cap.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(cap,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);p(s).setGameMode(GameType.SURVIVAL);p(s).teleportTo(l,0,30,3,Set.<Relative>of(),0,0,false);root=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);root.snapTo(-1.5,30,.5,0,0);l.addFreshEntity(root);});
   await(c,w,s -> root.mealReady()>0,"Actual living meal establishes territory");
   w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.SURVIVAL);p(s).teleportTo(s.overworld(),root.getX()+2,30,root.getZ(),Set.<Relative>of(),90,0,false);p(s).setHealth(20);p(s).addEffect(new MobEffectInstance(MobEffects.SLOWNESS,600));});
   await(c,w,s -> root.pose()==RootmoltStrider.WARNING,"Raised limbs/committed line warning precedes contact");
   float[] spellHealth={0};
   w.getServer().runOnServer(s -> {var player=p(s);check(FungalInvestigation.knows(player,"field:rootmolt_meal"),"Actual alive Survival observation earns the meal fact");check(!FungalInvestigation.knows(player,"field:rootmolt_counter"),"No counter fact is injected before spell test");
    dev.wildercord.player.Spellbooks.setCord(player,new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));var book=dev.wildercord.player.Spellbooks.get(player).withStarterGiven();book=book.learn(dev.wildercord.spell.Runes.TOUCH.id()).learn(dev.wildercord.spell.Runes.HARM.id());dev.wildercord.player.Spellbooks.set(player,book);
    player.teleportTo(s.overworld(),root.getX()+2,30,root.getZ(),Set.<Relative>of(),90,30,false);check(dev.wildercord.cast.SpellCaster.edit(player,0,java.util.List.of(dev.wildercord.spell.Runes.TOUCH.id(),dev.wildercord.spell.Runes.HARM.id()))==null,"Actual editor admits Touch Harm");dev.wildercord.player.Spellbooks.setReadyAt(player,0,0);dev.wildercord.player.Spellbooks.setMana(player,100);float mana=dev.wildercord.player.Spellbooks.mana(player);spellHealth[0]=root.getHealth();dev.wildercord.cast.SpellCaster.cast(player,0);check(dev.wildercord.player.Spellbooks.mana(player)<mana,"Actual Survival spell pays before its ordinary delayed release");});
   c.waitTicks(6);w.getServer().runOnServer(s -> {check(root.getHealth()<spellHealth[0],"Actual delayed paid Touch release genuinely wounds the warning source");check(root.pose()==RootmoltStrider.RECOVERING,"Magic still interrupts the creature after actual release");check(!FungalInvestigation.knows(p(s),"field:rootmolt_counter"),"Paid magic interruption does not claim the physical melee fact");});
   await(c,w,s -> root.pose()==RootmoltStrider.WARNING,"Next ordinary warning follows the actual post-spell attack rest");
   float before=w.getServer().computeOnServer(s -> p(s).getHealth());long first=w.getServer().computeOnServer(s -> s.overworld().getGameTime());
   await(c,w,s -> p(s).hasEffect(RootmoltContent.TETHER),"One actual physical rake applies a short owned tether");
   w.getServer().runOnServer(s -> {check(before-p(s).getHealth()==4,"One contact deals four physical damage, not per-tick damage");check(root.holding(p(s)) && p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(root.getUUID()),"Actual source/victim ownership agrees");check(s.overworld().getGameTime()-first>=RootmoltRules.WINDUP-5,"A readable warning window precedes restraint");});
   int id=w.getServer().computeOnServer(s -> root.getId());c.runOnClient(mc -> mc.gameMode.attack(mc.player,mc.level.getEntity(id)));c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!p(s).hasEffect(RootmoltContent.TETHER) && p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Actual native physical counter releases only its owned restraint");check(p(s).hasEffect(MobEffects.SLOWNESS),"Unrelated debuff remains");check(root.pose()==RootmoltStrider.RECOVERING && root.attackReady()>s.overworld().getGameTime(),"Interrupted creature has finite recovery and attack rest");check(FungalInvestigation.knows(p(s),"field:rootmolt_counter"),"Actual native direct melee earns the physical counter observation");});
  }
 }
 private void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<MinecraftServer> yes,String why) {for(int i=0;i<150;i++) {c.waitTicks(3);if(w.getServer().computeOnServer(s -> yes.test(s)))return;}throw new AssertionError(why);}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
