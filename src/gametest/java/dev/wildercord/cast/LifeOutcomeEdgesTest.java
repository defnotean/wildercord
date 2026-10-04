package dev.wildercord.cast;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Acceptance of owner edge paths. Requires actual owners + growth mixin.
 * No direct effect calls and no manufactured presentation event establishes success.
 * The observer assertions follow actual effect ownership.
 */
public final class LifeOutcomeEdgesTest implements FabricClientGameTest {
 private static final List<LifeOwnerEvents.Event> events=new ArrayList<>();
 private static final List<LifeOwnerEvents.Event> quiet=new ArrayList<>();
 private static final List<Pillager> foes=new ArrayList<>();
 private static final AtomicBoolean deny=new AtomicBoolean();
 @Override public void runTest(ClientGameTestContext c){
  PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->!deny.get());
  for(String edge:List.of("timestamp_expiry","adjacent_growth","equipment_repair","reaction_cleanse","passive_regrowth","venom_spread")){
   try(var world=c.worldBuilder().create()){
    c.waitTicks(30);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_health_regeneration false");
    server.runCommand("fill -10 100 -10 10 100 20 polished_deepslate");
    server.runOnServer(s->{var p=player(s);p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);p.setHealth(p.getMaxHealth());events.clear();quiet.clear();foes.clear();LifeOwnerEvents.observe(e->{events.add(e);if(Fx.muted())quiet.add(e);});});c.waitTicks(8);
    switch(edge){
     case "timestamp_expiry" -> {
      server.runOnServer(s->paid(player(s),"self","fortune"));c.waitTicks(100);
      server.runOnServer(s->paid(player(s),"self","fortune"));c.waitTicks(115);
      server.runOnServer(s->check(events.stream().noneMatch(e->e.rune().equals("fortune")&&e.moment()==LifeOwnerEvents.Moment.END),"A renewed real timestamp does not expire at its previous deadline"));c.waitTicks(95);
      server.runOnServer(s->{check(events.stream().filter(e->e.rune().equals("fortune")&&e.moment()==LifeOwnerEvents.Moment.END).count()==1,"Fortune actual natural expiry observed exactly once");check(LifeOwnerEvents.tracked()==0,"Expired observation lease released");paid(player(s),"self","reversal");});c.waitTicks(610);
      server.runOnServer(s->check(events.stream().filter(e->e.rune().equals("reversal")&&e.moment()==LifeOwnerEvents.Moment.END).count()==1,"Reversal natural timestamp expiry observed once without a fake save"));
      server.runOnServer(s->{var p=player(s);foes.add(foe(p,.5,10.5));paid(p,"bolt","drowse");});c.waitTicks(150);
      server.runOnServer(s->{var t=foes.getFirst();check(!CraftedRunes.asleep(t),"Actual sleeper timestamp no longer active");check(events.stream().filter(e->e.rune().equals("drowse")&&e.recipient().equals(t.getUUID())&&e.moment()==LifeOwnerEvents.Moment.END).count()==1,"Drowse naturally ends once at real recipient");t.discard();foes.clear();var p=player(s);foes.add(foe(p,.5,10.5));paid(p,"bolt","drowse");});c.waitTicks(20);
      UUID dead=server.computeOnServer(s->{var t=foes.getFirst();var id=t.getUUID();check(CraftedRunes.asleep(t),"Second real sleeper admitted");t.discard();return id;});c.waitTicks(145);
      server.runOnServer(s->check(events.stream().noneMatch(e->e.rune().equals("drowse")&&dead.equals(e.recipient())&&e.moment()==LifeOwnerEvents.Moment.END),"Removed body never receives a fabricated healthy expiry"));
      server.runOnServer(s->{events.clear();paid(player(s),"self","reversal");});c.waitTicks(6);
      server.runOnServer(s->check(Wards.devourWards(player(s))>0,"Actual owner consumes the freshly paid Reversal lease before expiry"));c.waitTicks(8);
      server.runOnServer(s->check(events.stream().noneMatch(e->e.rune().equals("reversal")&&e.moment()==LifeOwnerEvents.Moment.END),"Early owner cancellation is not fabricated natural expiry"));
     }
     case "adjacent_growth" -> {
      server.runCommand("fill -4 100 -4 4 100 4 dirt");
      server.runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(1,101,0),Blocks.SHORT_GRASS.defaultBlockState());s.overworld().getRandom().setSeed(7732);paid(player(s),"self","grow");});c.waitTicks(8);
      server.runOnServer(s->{var growth=events.stream().filter(e->e.rune().equals("grow")&&e.recipient()==null).toList();check(!growth.isEmpty(),"Actual short-grass bonemeal produces observed adjacent writes");check(growth.stream().anyMatch(e->BlockPos.containing(e.anchor()).getY()==102),"Actual upper half, not only original short-grass block, owns growth material");check(growth.stream().allMatch(e->!s.overworld().getBlockState(BlockPos.containing(e.anchor())).isAir()),"Every retained growth observation corresponds to a real nonair cell");});
      server.runCommand("fill -4 101 -4 4 103 4 air");server.runOnServer(s->{s.overworld().setBlockAndUpdate(new BlockPos(1,101,0),Blocks.SHORT_GRASS.defaultBlockState());events.clear();deny.set(true);paid(player(s),"self","grow");});c.waitTicks(8);
      server.runOnServer(s->{check(events.stream().noneMatch(e->e.rune().equals("grow")&&e.moment()!=LifeOwnerEvents.Moment.REFUSED),"Claim refusal produces no false successful adjacent growth");check(s.overworld().getBlockState(new BlockPos(1,101,0)).is(Blocks.SHORT_GRASS)&&s.overworld().getBlockState(new BlockPos(1,102,0)).isAir(),"Denied bonemeal leaves original lower plant and empty upper cell unchanged");deny.set(false);});
     }
     case "equipment_repair" -> {
      server.runOnServer(s->{var p=player(s);var main=new ItemStack(Items.IRON_PICKAXE);main.setDamageValue(80);var off=new ItemStack(Items.IRON_AXE);off.setDamageValue(70);p.setItemInHand(InteractionHand.MAIN_HAND,main);p.setItemInHand(InteractionHand.OFF_HAND,off);paid(p,"self","restore");});c.waitTicks(6);
      int count=server.computeOnServer(s->{var p=player(s);var main=p.getMainHandItem();var off=p.getOffhandItem();check(main.getDamageValue()<80&&off.getDamageValue()<70,"Actual two equipped items are repaired");var repairs=events.stream().filter(e->e.rune().equals("restore")&&e.detail().startsWith("equipment:")).toList();check(repairs.size()==2&&repairs.stream().map(LifeOwnerEvents.Event::detail).distinct().count()==2,"Repair observations preserve two distinct actual slot identities");check(repairs.stream().allMatch(e->e.delta()>0&&e.recipient().equals(p.getUUID())&&e.anchor().distanceTo(p.getBoundingBox().getCenter())<1),"Actual durability regained has recipient and bounded slot anchor");paid(p,"self","restore");return repairs.size();});c.waitTicks(6);
      server.runOnServer(s->check(events.stream().filter(e->e.rune().equals("restore")&&e.detail().startsWith("equipment:")).count()==count,"Cooldown-refused equipment has no invented repair observation"));
     }
     case "reaction_cleanse" -> {
      server.runOnServer(s->{var p=player(s);Reactions.mark(p,Reactions.Mark.SHADOWED);Reactions.mark(p,Reactions.Mark.WINDSWEPT);paid(p,"self","cleanse");});c.waitTicks(6);
      server.runOnServer(s->{var p=player(s);check(Reactions.marks(p).isEmpty(),"Cleanse removes actual reaction-only marks");check(events.stream().anyMatch(e->e.rune().equals("cleanse")&&e.units()>=2&&e.delta()==0&&p.getUUID().equals(e.recipient())),"Removed reaction marks produce husks without inventing healing");events.clear();paid(p,"self","cleanse");});c.waitTicks(6);
      server.runOnServer(s->check(events.stream().noneMatch(e->e.rune().equals("cleanse")&&e.moment()!=LifeOwnerEvents.Moment.REFUSED),"Already clean full-health recipient has no successful change observation"));
     }
     case "passive_regrowth" -> {
      float[] initial={0};var paidRenewal=new AtomicBoolean();server.runOnServer(s->{var p=player(s);p.setAttached(WildercordAttachments.CIRCLES,1);p.setHealth(4);Spellbooks.setMana(p,100);initial[0]=Spellbooks.mana(p);LifeOwnerEvents.observe(e->{events.add(e);if(Fx.muted())quiet.add(e);if(e.rune().equals("regrowth")&&e.moment()==LifeOwnerEvents.Moment.RENEW&&p.getUUID().equals(e.recipient())&&Spellbooks.mana(p)<initial[0])paidRenewal.set(true);});if(Spellbooks.get(p).passiveOn(0))SpellCaster.togglePassive(p,0);check(!Spellbooks.get(p).passiveOn(0),"Fixture starts from an actually disabled passive");check(SpellCaster.editPassive(p,0,List.of(Runes.SELF.id(),Runes.REGROWTH.id()))==null,"Passive edit uses real circle and sustainability gates");SpellCaster.togglePassive(p,0);});c.waitTicks(12);
      server.runOnServer(s->{var p=player(s);check(paidRenewal.get(),"Actual automatic renewal owner callback observes paid upkeep before general mana regeneration");check(p.hasEffect(MobEffects.REGENERATION),"Real automatic passive applies actual regeneration");check(events.stream().anyMatch(e->e.rune().equals("regrowth")&&e.moment()==LifeOwnerEvents.Moment.RENEW),"Passive branch publishes only admitted regeneration");});c.waitTicks(50);
      server.runOnServer(s->{var p=player(s);check(p.getHealth()>4,"Passive regeneration really heals through vanilla ticks");check(quiet.stream().anyMatch(e->e.rune().equals("regrowth")),"Actual passive renewal is observed while Fx.quietly remains in force");SpellCaster.togglePassive(p,0);});c.waitTicks(dev.wildercord.spell.Passives.EFFECT_TICKS+5);
      server.runOnServer(s->check(!player(s).hasEffect(MobEffects.REGENERATION),"Passive off stops renewals and actual capped buff expires"));
     }
     case "venom_spread" -> {
      server.runOnServer(s->{var p=player(s);foes.add(foe(p,.5,10.5));foes.add(foe(p,1.5,10.5));foes.add(foe(p,2.5,10.5));foes.add(foe(p,3,10.5));foes.add(foe(p,-1.5,10.5));paid(p,"bolt","venom");});c.waitTicks(42);
      server.runOnServer(s->{var direct=foes.getFirst();check(direct.getHealth()<direct.getMaxHealth(),"Actual paid Venom hits real direct recipient");var hops=events.stream().filter(e->e.rune().equals("venom")&&e.moment()==LifeOwnerEvents.Moment.PULSE&&e.secondary()!=null).toList();check(hops.size()>0&&hops.size()<=3,"Real spread admissions retain max-three hop cap");for(var hop:hops){var t=foes.stream().filter(f->f.getUUID().equals(hop.recipient())).findFirst().orElseThrow();check(t.hasEffect(MobEffects.POISON)&&t.getHealth()<t.getMaxHealth(),"Each observed hop recipient actually receives poison and health damage");check(events.stream().anyMatch(e->e.rune().equals("venom")&&t.getUUID().equals(e.recipient())&&e.delta()<0),"Each hop has actual custom-scheduler health-loss evidence, beyond vanilla Poison");check(hop.secondary().distanceTo(direct.getBoundingBox().getCenter())<1,"Hop source is actual directly venomed body");}check(foes.stream().skip(1).filter(t->t.hasEffect(MobEffects.POISON)).count()<=3,"Fourth possible neighbor is not falsely admitted");});
     }
    }
   }finally{deny.set(false);LifeOwnerEvents.clear();events.clear();quiet.clear();foes.clear();}
  }
 }
 private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 private static Pillager foe(ServerPlayer p,double x,double z){var t=EntityTypes.PILLAGER.create(p.level(),EntitySpawnReason.COMMAND);check(t!=null,"Actual hostile fixture");t.setPos(x,101,z);t.setNoAi(true);p.level().addFreshEntity(t);return t;}
 private static void paid(ServerPlayer p,String...paths){check(SpellCaster.edit(p,0,Arrays.stream(paths).map(x->"wildercord:"+x).toList())==null,"Learned public spell accepted");Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float start=Spellbooks.mana(p);for(String path:paths){var rune=Runes.get("wildercord:"+path).orElseThrow();if(Runes.innate(rune)){p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,"");SpellCaster.cast(p,0);check(Spellbooks.mana(p)==start,"Learned foreign innate refuses before payment: "+path);p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,rune.id());}}SpellCaster.cast(p,0);check(Spellbooks.mana(p)<start,"Survival pays for actual cast");}
 private static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
}
