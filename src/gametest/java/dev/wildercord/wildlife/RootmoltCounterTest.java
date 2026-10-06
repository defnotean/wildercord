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
   w.getServer().runOnServer(s -> {var l=s.overworld();dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.RootmoltCounterTest\",\"seed\":\"{}\"}",l.getSeed());for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}var cap=new BlockPos(0,30,0);l.setBlock(cap.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(cap,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,2),2);p(s).setGameMode(GameType.SURVIVAL);p(s).teleportTo(l,0,30,3,Set.<Relative>of(),0,0,false);root=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);root.snapTo(-1.5,30,.5,0,0);l.addFreshEntity(root);});
   await(c,w,s -> root.mealReady()>0,"Actual living meal establishes territory");
   w.getServer().runOnServer(s -> {p(s).setGameMode(GameType.SURVIVAL);p(s).teleportTo(s.overworld(),root.getX()+2,30,root.getZ(),Set.<Relative>of(),90,0,false);p(s).setHealth(20);p(s).addEffect(new MobEffectInstance(MobEffects.SLOWNESS,600));});
   await(c,w,s -> root.pose()==RootmoltStrider.WARNING,"Raised limbs/committed line warning precedes contact");
   float[] spellHealth={0};
   w.getServer().runOnServer(s -> {var player=p(s);check(FungalInvestigation.knows(player,"field:rootmolt_meal"),"Actual alive Survival observation earns the meal fact");check(!FungalInvestigation.knows(player,"field:rootmolt_counter"),"No counter fact is injected before spell test");
    dev.wildercord.player.Spellbooks.setCord(player,new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));var book=dev.wildercord.player.Spellbooks.get(player).withStarterGiven();book=book.learn(dev.wildercord.spell.Runes.TOUCH.id()).learn(dev.wildercord.spell.Runes.HARM.id());dev.wildercord.player.Spellbooks.set(player,book);
    player.teleportTo(s.overworld(),root.getX()+2,30,root.getZ(),Set.<Relative>of(),90,30,false);check(dev.wildercord.cast.SpellCaster.edit(player,0,java.util.List.of(dev.wildercord.spell.Runes.TOUCH.id(),dev.wildercord.spell.Runes.HARM.id()))==null,"Actual editor admits Touch Harm");dev.wildercord.player.Spellbooks.setReadyAt(player,0,0);dev.wildercord.player.Spellbooks.setMana(player,100);float mana=dev.wildercord.player.Spellbooks.mana(player);spellHealth[0]=root.getHealth();dev.wildercord.cast.SpellCaster.cast(player,0);check(dev.wildercord.player.Spellbooks.mana(player)<mana,"Actual Survival spell pays before its ordinary delayed release");});
   c.waitTicks(6);w.getServer().runOnServer(s -> {check(root.getHealth()<spellHealth[0],"Actual delayed paid Touch release genuinely wounds the warning source");check(root.pose()==RootmoltStrider.RECOVERING,"Magic still interrupts the creature after actual release");check(!FungalInvestigation.knows(p(s),"field:rootmolt_counter"),"Paid magic interruption does not claim the physical melee fact");});
   awaitWarningAfterRest(c,w);
   long first=w.getServer().computeOnServer(s -> s.overworld().getGameTime());
   // A readable warning begins within four blocks; physical rake reaches only its narrower committed strip.
   // The source can naturally stroll during its full post-spell rest. Approach through genuine client input,
   // keeping the original AI, warning clock, health and native physical counter unchanged.
   approachCommittedReach(c,w);
   float before=w.getServer().computeOnServer(s -> p(s).getHealth());
   await(c,w,s -> p(s).hasEffect(RootmoltContent.TETHER),"One actual physical rake applies a short owned tether");
   w.getServer().runOnServer(s -> {check(before-p(s).getHealth()==4,"One contact deals four physical damage, not per-tick damage");check(root.holding(p(s)) && p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(root.getUUID()),"Actual source/victim ownership agrees");check(s.overworld().getGameTime()-first>=RootmoltRules.WINDUP-5,"A readable warning window precedes restraint");});
   int id=w.getServer().computeOnServer(s -> root.getId());c.runOnClient(mc -> mc.gameMode.attack(mc.player,mc.level.getEntity(id)));c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!p(s).hasEffect(RootmoltContent.TETHER) && p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Actual native physical counter releases only its owned restraint");check(p(s).hasEffect(MobEffects.SLOWNESS),"Unrelated debuff remains");check(root.pose()==RootmoltStrider.RECOVERING && root.attackReady()>s.overworld().getGameTime(),"Interrupted creature has finite recovery and attack rest");check(FungalInvestigation.knows(p(s),"field:rootmolt_counter"),"Actual native direct melee earns the physical counter observation");});
  }
 }
 /** Same 150 x 3 tick warning window. Only real forward input keeps the original target eligible. */
 private void awaitWarningAfterRest(ClientGameTestContext c,TestSingleplayerContext w){
  boolean forward=c.computeOnClient(mc->mc.options.keyUp.isDown());float[] look=c.computeOnClient(mc->new float[]{mc.player.getYRot(),mc.player.getXRot()});
  int id=w.getServer().computeOnServer(s->root.getId());long rest=w.getServer().computeOnServer(s->root.attackReady());boolean recovered=false,expired=false;Boolean moving=null;int approachReceipts=0;
  try{
   for(int i=0;i<150;i++){
    int step=i;var state=w.getServer().computeOnServer(s->new boolean[]{root.pose()!=RootmoltStrider.RECOVERING,s.overworld().getGameTime()>=rest,root.distanceToSqr(p(s))>6.25,root.pose()==RootmoltStrider.WARNING});
    if(state[0]&&!recovered){rootReceipt(w,"recovery_end_observed",step,rest);recovered=true;}
    if(state[1]&&!expired){rootReceipt(w,"rest_expiry_observed",step,rest);expired=true;}
    if(state[3]){rootReceipt(w,"warning_admitted",step,rest);return;}
    boolean walk=state[2];
    if(moving==null||moving!=walk){if(approachReceipts++<16)rootReceipt(w,walk?"approach_start":"approach_stop",step,rest);moving=walk;}
    c.runOnClient(mc->{var entity=mc.level.getEntity(id);check(entity instanceof RootmoltStrider,"Actual recovering source is client tracked");var d=entity.position().subtract(mc.player.position());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot(0);mc.options.keyUp.setDown(walk);});
    c.waitTicks(3);
    if(w.getServer().computeOnServer(s->root.pose()==RootmoltStrider.WARNING)){if(!recovered)rootReceipt(w,"recovery_end_observed",step,rest);if(!expired)rootReceipt(w,"rest_expiry_observed",step,rest);rootReceipt(w,"warning_admitted",step,rest);return;}
   }
   throw failure(w,"Next ordinary warning follows the actual post-spell attack rest");
  }finally{c.runOnClient(mc->{mc.options.keyUp.setDown(forward);mc.player.setYRot(look[0]);mc.player.setXRot(look[1]);});}
 }
 private void rootReceipt(TestSingleplayerContext w,String event,int step,long rest){
  w.getServer().runOnServer(s->{var player=p(s);System.out.println("ROOTMOLT_COUNTER_REST event="+event+" step="+step+" now="+s.overworld().getGameTime()+" pose="+root.pose()+" attackReady="+root.attackReady()+" postSpellRest="+rest+" restRemaining="+(rest-s.overworld().getGameTime())+" source="+root.position()+" victim="+player.position()+" distanceSquared="+root.distanceToSqr(player)+" target="+(root.getTarget()==null?null:root.getTarget().getUUID())+" targetIsVictim="+(root.getTarget()==player)+" nearGuardedCap="+root.nearGuardedCap(player.position())+" sight="+root.hasLineOfSight(player)+" line="+root.onLine(player.position())+" health="+player.getHealth());});
 }
 private void approachCommittedReach(ClientGameTestContext c,TestSingleplayerContext w){
  boolean forward=c.computeOnClient(mc->mc.options.keyUp.isDown());float[] look=c.computeOnClient(mc->new float[]{mc.player.getYRot(),mc.player.getXRot()});int id=w.getServer().computeOnServer(s->root.getId());
  try{
   if(w.getServer().computeOnServer(s->root.onLine(p(s).position())&&root.distanceToSqr(p(s))<=9&&p(s).onGround()))return;
   c.runOnClient(mc->{var entity=mc.level.getEntity(id);check(entity instanceof RootmoltStrider,"Actual committed source is client tracked");var d=entity.position().subtract(mc.player.position());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));mc.player.setXRot(0);mc.options.keyUp.setDown(true);});
   for(int n=0;n<20;n++){
    c.waitTicks(1);
    if(w.getServer().computeOnServer(s->root.onLine(p(s).position())&&root.distanceToSqr(p(s))<=9&&p(s).onGround()))break;
   }
   c.runOnClient(mc->mc.options.keyUp.setDown(false));
   w.getServer().runOnServer(s->{System.out.println("ROOTMOLT_COUNTER_APPROACH source="+root.position()+" victim="+p(s).position()+" pose="+root.pose()+" line="+root.onLine(p(s).position())+" distanceSquared="+root.distanceToSqr(p(s))+" health="+p(s).getHealth()+" now="+s.overworld().getGameTime());check(root.pose()==RootmoltStrider.WARNING&&root.onLine(p(s).position())&&root.distanceToSqr(p(s))<=9&&p(s).onGround(),"Real client walk reaches the physical committed strip before the original warning ends");});
  }finally{c.runOnClient(mc->{mc.options.keyUp.setDown(forward);mc.player.setYRot(look[0]);mc.player.setXRot(look[1]);});}
 }
 private void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<MinecraftServer> yes,String why) {for(int i=0;i<150;i++) {c.waitTicks(3);if(w.getServer().computeOnServer(s -> yes.test(s)))return;}throw failure(w,why);}
 private AssertionError failure(TestSingleplayerContext w,String why) {return new AssertionError(w.getServer().computeOnServer(s -> why+" sourceBody="+root.position()+" sourceDelta="+root.getDeltaMovement()+" sourceAlive="+root.isAlive()+" sourceRemoved="+root.isRemoved()+" pose="+root.pose()+" sourceGround="+root.onGround()+" target="+(root.getTarget()==null?null:root.getTarget().getUUID())+" victimBody="+p(s).position()+" victimDelta="+p(s).getDeltaMovement()+" victimAlive="+p(s).isAlive()+" victimHealth="+p(s).getHealth()+" victimCooldown="+p(s).getInvulnerableTime()+" victimGround="+p(s).onGround()+" sourceWorld="+root.level().dimension()+" victimWorld="+p(s).level().dimension()+" distanceSquared="+root.distanceToSqr(p(s))+" sight="+root.hasLineOfSight(p(s))+" onCommittedLine="+root.onLine(p(s).position())+" holding="+root.holding(p(s))+" owner="+p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER)+" tether="+p(s).getEffect(RootmoltContent.TETHER)+" attackReady="+root.attackReady()+" mealReady="+root.mealReady()+" now="+s.overworld().getGameTime()));}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
