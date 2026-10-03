package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.mixin.ItemEntityAccessor;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual paid remote collection preserves ownership, delayed pickup and editable-ground boundaries. */
public final class CollectOwnershipTest implements FabricClientGameTest {
 private final List<ItemEntity> preserved=new ArrayList<>();
 private final Map<UUID,Vec3> origins=new HashMap<>();
 @Override public void runTest(ClientGameTestContext c){
  var deny=new AtomicBoolean(true);
  var protectedGround=new BlockPos(6,100,-1);
  PlayerBlockBreakEvents.BEFORE.register((level,p,pos,state,entity)->!deny.get() || !pos.equals(protectedGround));
  try(var world=c.worldBuilder().create()){
   c.waitTicks(40);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("fill -12 100 -12 12 100 12 polished_deepslate");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var rune:Runes.all())b=b.learn(rune.id());Spellbooks.set(p,b);p.getInventory().clearContent();
    var own=drop(p,Items.DIAMOND,2,new Vec3(2.5,101,.5));own.setTarget(p.getUUID());own.setThrower(p);
    drop(p,Items.EMERALD,3,new Vec3(2.5,101,-1.5));
    var target=drop(p,Items.GOLD_INGOT,4,new Vec3(3.5,101,.5));target.setTarget(UUID.randomUUID());preserve(target);
    var absentOwner=EntityTypes.VILLAGER.create(s.overworld(),EntitySpawnReason.COMMAND);check(absentOwner!=null,"Offline thrower reference fixture");
    var thrown=drop(p,Items.IRON_INGOT,5,new Vec3(4.5,101,.5));thrown.setThrower(absentOwner);absentOwner.discard();check(((ItemEntityAccessor)thrown).wildercord$thrower()!=null && !((ItemEntityAccessor)thrown).wildercord$thrower().getUUID().equals(p.getUUID()),"Foreign thrower UUID survives absent entity");preserve(thrown);
    var delayed=drop(p,Items.COPPER_INGOT,6,new Vec3(3.5,101,-1.5));delayed.setPickUpDelay(200);preserve(delayed);
    var never=drop(p,Items.LAPIS_LAZULI,7,new Vec3(4.5,101,-1.5));never.setNeverPickUp();preserve(never);
    preserve(drop(p,Items.REDSTONE,8,new Vec3(6.5,101,-.5)));
    preserve(drop(p,Items.AMETHYST_SHARD,9,new Vec3(7.5,101,7.5)));
    var xp=new ExperienceOrb(s.overworld(),2.5,101,1.5,7);xp.setNoGravity(true);xp.setDeltaMovement(Vec3.ZERO);s.overworld().addFreshEntity(xp);
   });c.waitTicks(6);
   int[] xpBefore={0};server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();xpBefore[0]=p.totalExperience;paid(p);});c.waitTicks(20);
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getInventory().countItem(Items.DIAMOND)==2 && p.getInventory().countItem(Items.EMERALD)==3,"Own and unowned ready goods collected through actual paid Self cast");check(p.totalExperience>xpBefore[0],"Actual experience orb collected");for(var item:preserved)check(item.isAlive() && item.position().distanceTo(origins.get(item.getUUID()))<.05,"Refused drop remains in place: "+item.getItem());check(p.getInventory().countItem(Items.GOLD_INGOT)==0 && p.getInventory().countItem(Items.IRON_INGOT)==0,"Neither targeted nor foreign-thrown goods acquired");check(p.getInventory().countItem(Items.COPPER_INGOT)==0 && p.getInventory().countItem(Items.LAPIS_LAZULI)==0,"Delay and never-pickup reservations retained");
    // Release only the ordinary pickup delay: unowned ready goods become collectable.
    var delayed=preserved.stream().filter(i->i.getItem().is(Items.COPPER_INGOT)).findFirst().orElseThrow();delayed.setNoPickUpDelay();preserved.remove(delayed);paid(p);
   });c.waitTicks(20);server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getInventory().countItem(Items.COPPER_INGOT)==6,"Ready unowned goods are admitted after reservation expires");for(var item:preserved)check(item.isAlive() && item.position().distanceTo(origins.get(item.getUUID()))<.05,"Repeated cast preserves refused ownership, claim and radius");});
  }finally{deny.set(false);preserved.clear();origins.clear();}
 }
 private void preserve(ItemEntity item){preserved.add(item);origins.put(item.getUUID(),item.position());}
 private static ItemEntity drop(ServerPlayer p,Item item,int count,Vec3 at){var e=new ItemEntity(p.level(),at.x,at.y,at.z,new ItemStack(item,count));e.setNoGravity(true);e.setDeltaMovement(Vec3.ZERO);e.setNoPickUpDelay();p.level().addFreshEntity(e);return e;}
 private static void paid(ServerPlayer p){check(SpellCaster.edit(p,0,List.of(Runes.SELF.id(),Runes.COLLECT.id()))==null,"Accepted Self Collect");Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Collect spends Survival mana");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
