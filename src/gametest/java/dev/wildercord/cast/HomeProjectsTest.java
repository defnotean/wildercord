package dev.wildercord.cast;
import dev.wildercord.content.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
public final class HomeProjectsTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context){try(var world=context.worldBuilder().create()){
  context.waitTicks(35);context.runOnClient(mc->mc.getWindow().setWindowed(1600,900));BlockPos at=new BlockPos(0,100,0);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var s=p.level();
   for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)s.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
   s.setBlockAndUpdate(at,WildercordBlocks.RUNIC_HEARTH.defaultBlockState());p.teleportTo(s,.5,100,-2,Set.of(),0,15,false);
   var h=(RunicHearthEntity)s.getBlockEntity(at);h.use(p,new ItemStack(Items.WHEAT));check(h.project().equals("garden"),"configure garden");
   s.setBlockAndUpdate(at.offset(1,-1,0),Blocks.FARMLAND.defaultBlockState());s.setBlockAndUpdate(at.offset(1,0,0),Blocks.WHEAT.defaultBlockState());
   charge(p,at,Runes.GROW);check(h.charge()==1,"life spell charges planter");
  });context.waitTicks(105);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var h=(RunicHearthEntity)p.level().getBlockEntity(at);
   check(((CropBlock)Blocks.WHEAT).getAge(p.level().getBlockState(at.offset(1,0,0)))>=1&&h.charge()==0,"garden spends charge to advance crop");
   h.use(p,new ItemStack(Items.AMETHYST_SHARD));charge(p,at,Runes.FIRE);
   check(h.project().equals("lantern")&&h.charge()==1,"configure and charge reading lantern");
  });
  // Charging sets a 20-tick cooldown; lantern updates run only at gameTime % 20 == 0.
  // Allow the full cooldown plus one update interval regardless of the starting tick.
  context.waitTicks(40);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION),"reading lantern grants its utility");
   var h=(RunicHearthEntity)p.level().getBlockEntity(at);h.use(p,new ItemStack(Items.FEATHER));charge(p,at,Runes.SWIFT);
  });context.waitTicks(25);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,114,-1,Set.of(),0,0,false);});context.waitTicks(40);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"chime catches a real fall");p.teleportTo(p.level(),.5,100,-2,Set.of(),0,15,false);
   var h=(RunicHearthEntity)p.level().getBlockEntity(at);h.use(p,new ItemStack(Items.ENDER_PEARL));h.use(p,new ItemStack(WildercordItems.MANA_CRYSTAL));
   charge(p,at,Runes.FIRESTORM);check(h.charge()==2,"a fusion contributes its real fire and wind ingredients");charge(p,at,Runes.FROST);check(h.charge()==3,"three distinct elements fill ritual");
  });context.waitTicks(425);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(p.getInventory().countItem(WildercordItems.TORN_PAGE)>0,"solo ritual finishes with reward");
   var h=(RunicHearthEntity)p.level().getBlockEntity(at);check(h.charge()==0,"ritual charge consumed");h.use(p,new ItemStack(WildercordItems.MANA_CRYSTAL));check(h.charge()==0,"ritual cooldown cannot be bypassed with catalyst");
  });
  BlockPos shared=at.offset(3,0,0);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var level=p.level();
   level.setBlockAndUpdate(shared,WildercordBlocks.RUNIC_HEARTH.defaultBlockState());var h=(RunicHearthEntity)level.getBlockEntity(shared);
   h.use(p,new ItemStack(Items.ENDER_PEARL));h.use(p,new ItemStack(WildercordItems.MANA_CRYSTAL));
   var friend=new Teammate(level,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("hearth-companion".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"HearthFriend"));
   friend.snapTo(2.5,100,-1);var team=server.getScoreboard().addPlayerTeam("hearth_test");server.getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);server.getScoreboard().addPlayerToTeam(friend.getScoreboardName(),team);level.addNewPlayer(friend);
   charge(p,shared,Runes.FIRE);charge(p,shared,Runes.FROST);charge(friend,shared,Runes.SHOCK);check(h.charge()==3,"teammate contributes third ritual ingredient");
  });context.waitTicks(225);
  world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var h=(RunicHearthEntity)p.level().getBlockEntity(shared);check(h.charge()==0,"cooperative ritual finishes after ten seconds");check(p.getInventory().countItem(WildercordItems.TORN_PAGE)>=2,"cooperative ritual awards its page");});
  context.takeScreenshot(TestScreenshotOptions.of("runic_hearth").disableCounterPrefix());
 }}
 private static void charge(net.minecraft.server.level.ServerPlayer p,BlockPos pos,RuneDef rune){var node=SpellCompiler.compile(List.of(Runes.TOUCH,rune)).root().groups.getFirst().effects.getFirst();Effects.apply(new Cast(p),node,new Cast.Hit(List.of(),Vec3.atCenterOf(pos),p.getLookAngle(),p.position(),pos,Direction.UP,false),1);}
 private static void check(boolean v,String label){if(!v)throw new AssertionError(label);}
 private static final class Teammate extends net.fabricmc.fabric.api.entity.FakePlayer {
  Teammate(net.minecraft.server.level.ServerLevel l,com.mojang.authlib.GameProfile p){super(l,p);}
  public net.minecraft.world.scores.PlayerTeam getTeam(){return level().getScoreboard().getPlayersTeam(getScoreboardName());}
 }
}
