package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
/** Real AI contact on a fixture target: cleanse, removed source and immunity never leave owned control. */
public final class RootmoltControlLifecycleTest implements FabricClientGameTest {
 private RootmoltStrider source;private Zombie immune,callbackVictim,replacement;private float immuneHealth,callbackHealth,replacementHealth;private boolean mutateContact,replaceContact;private int invalidLaterProviders;
 public void runTest(ClientGameTestContext c) {
  RootmoltContent.ROOT_RESISTANCE.register((victim,r) -> {if(mutateContact && r==source && victim==callbackVictim){mutateContact=false;victim.snapTo(10,30,.5,0,0);}return false;});
  RootmoltContent.ROOT_RESISTANCE.register((victim,r) -> {if(replaceContact && r==source && victim==callbackVictim){replaceContact=false;r.setTarget(replacement);}return false;});
  RootmoltContent.ROOT_RESISTANCE.register((victim,r) -> {if(r==source && victim==callbackVictim)invalidLaterProviders++;return false;});
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,29,z),Blocks.STONE.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),y==33?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}p(s).setGameMode(GameType.SURVIVAL);p(s).teleportTo(l,2,30,.5,Set.<Relative>of(),90,0,false);p(s).setHealth(20);p(s).addEffect(new MobEffectInstance(MobEffects.SLOWNESS,600));spawn(s,p(s));});
   await(c,w,s -> p(s).hasEffect(RootmoltContent.TETHER),"Real AI makes first controlled contact");
   w.getServer().runOnServer(s -> p(s).removeEffect(RootmoltContent.TETHER));c.waitTicks(3);
   w.getServer().runOnServer(s -> {check(!source.holding(p(s)) && p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Cleansed control releases ownership and the source");check(p(s).hasEffect(MobEffects.SLOWNESS),"Cleanse does not clear unrelated effects");source.discard();spawn(s,p(s));});
   c.waitTicks(12);await(c,w,s -> p(s).hasEffect(RootmoltContent.TETHER),"Second independent actual contact");
   w.getServer().runOnServer(s -> source.discard());c.waitTicks(2);
   w.getServer().runOnServer(s -> {check(!p(s).hasEffect(RootmoltContent.TETHER) && p(s).getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Removed source releases its exact victim immediately");p(s).setGameMode(GameType.CREATIVE);immune=new Zombie(EntityTypes.ZOMBIE,s.overworld()) { @Override public boolean canBeAffected(MobEffectInstance e) {return !e.getEffect().equals(RootmoltContent.TETHER) && super.canBeAffected(e);} };immune.setNoAi(true);immune.setCustomName(net.minecraft.network.chat.Component.literal("Rootmolt immunity witness"));immune.snapTo(2,30,.5,0,0);s.overworld().addFreshEntity(immune);immuneHealth=immune.getHealth();check(immuneHealth==immune.getMaxHealth(),"Actual post-add full-health Rootmolt immunity witness baseline");spawn(s,immune);});
   await(c,w,s -> immune.getHealth()<immuneHealth,"Actual physical rake still lands on a tether-immune victim");c.waitTicks(2);
   w.getServer().runOnServer(s -> {check(!immune.hasEffect(RootmoltContent.TETHER) && immune.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Rejected effect cannot retain transient owner");check(!source.holding(immune) && source.pose()==RootmoltStrider.RECOVERING,"Immunity ends the one contact with finite recovery");});
   w.getServer().runOnServer(s -> {source.discard();callbackVictim=new Zombie(EntityTypes.ZOMBIE,s.overworld());callbackVictim.setNoAi(true);callbackVictim.setCustomName(net.minecraft.network.chat.Component.literal("Rootmolt callback wound witness"));callbackVictim.snapTo(2,30,.5,0,0);s.overworld().addFreshEntity(callbackVictim);callbackHealth=callbackVictim.getHealth();check(callbackHealth==callbackVictim.getMaxHealth(),"Actual post-add full-health Rootmolt callback wound witness baseline");invalidLaterProviders=0;mutateContact=true;spawn(s,callbackVictim);});
   await(c,w,s -> !mutateContact,"Real physical contact invokes the deliberately moving equipment provider");c.waitTicks(2);
   w.getServer().runOnServer(s -> {check(callbackVictim.getHealth()<callbackHealth && callbackVictim.getX()==10,"Callback fixture preserves the actual wound and actual displacement");check(invalidLaterProviders==0,"Later equipment provider is refused after earlier provider moves the contact");check(!callbackVictim.hasEffect(RootmoltContent.TETHER) && callbackVictim.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER) && !source.holding(callbackVictim),"A false-returning callback cannot authorize stale control after displacement");});
   w.getServer().runOnServer(s -> {source.discard();callbackVictim=new Zombie(EntityTypes.ZOMBIE,s.overworld());callbackVictim.setNoAi(true);callbackVictim.setCustomName(net.minecraft.network.chat.Component.literal("Rootmolt callback wound witness"));callbackVictim.snapTo(2,30,.5,0,0);s.overworld().addFreshEntity(callbackVictim);callbackHealth=callbackVictim.getHealth();check(callbackHealth==callbackVictim.getMaxHealth(),"Actual post-add full-health Rootmolt callback wound witness baseline");replacement=new Zombie(EntityTypes.ZOMBIE,s.overworld());replacement.setNoAi(true);replacement.setCustomName(net.minecraft.network.chat.Component.literal("Rootmolt replacement witness"));replacement.snapTo(2.2,30,.5,0,0);s.overworld().addFreshEntity(replacement);replacementHealth=replacement.getHealth();check(replacementHealth==replacement.getMaxHealth(),"Actual post-add full-health Rootmolt replacement witness baseline");invalidLaterProviders=0;replaceContact=true;spawn(s,callbackVictim);});
   await(c,w,s -> !replaceContact,"Actual first wound invokes target-replacing resistance callback");c.waitTicks(4);
   w.getServer().runOnServer(s -> {check(callbackVictim.getHealth()<callbackHealth,"The original real wound remains: baseline="+callbackHealth+" actual="+callbackVictim.getHealth()+" armor="+callbackVictim.getArmorValue());check(replacement.getHealth()==replacementHealth,"Replacement target cannot take a second wound from the consumed rake: baseline="+replacementHealth+" actual="+replacement.getHealth());check(invalidLaterProviders==0,"Later equipment cannot charge an invalid replaced-target contact");check(source.pose()==RootmoltStrider.RECOVERING && !replacement.hasEffect(RootmoltContent.TETHER),"Consumed contact recovers without replacement control");});
  }finally{mutateContact=false;replaceContact=false;immune=null;callbackVictim=null;replacement=null;source=null;}
 }
 private void spawn(MinecraftServer s,LivingEntity target) {source=RootmoltContent.STRIDER.create(s.overworld(),EntitySpawnReason.COMMAND);source.snapTo(0,30,.5,0,0);s.overworld().addFreshEntity(source);source.setTarget(target);}
 private void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<MinecraftServer> yes,String why) {for(int i=0;i<120;i++){c.waitTicks(3);if(w.getServer().computeOnServer(yes::test))return;}throw new AssertionError(why);}
 private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
