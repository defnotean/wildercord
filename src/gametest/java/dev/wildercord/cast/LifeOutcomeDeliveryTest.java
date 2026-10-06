package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import java.util.Arrays;
import java.util.Set;

/** native acceptance: genuine state changes via paid public SpellCaster, never direct effects.
 * Ward/lease/delayed cases are in LifeOutcomeStatefulTest and LifeOutcomeEdgesTest.
 * Requires actual owner instrumentation.
 */
public final class LifeOutcomeDeliveryTest implements FabricClientGameTest {
 private static net.minecraft.server.level.ServerPlayer actor;
 private LifeOutcomeGallery gallery;
 private static net.minecraft.server.level.ServerPlayer player(net.minecraft.server.MinecraftServer s){return actor;}
 private static final java.util.List<LifeOwnerEvents.Event> observed=new java.util.ArrayList<>();
 @Override public void runTest(ClientGameTestContext c) {
  try(var view=new LifeOutcomeGallery(c,"life_outcome_core");var world=c.worldBuilder().create()) {
   gallery=view;
   gallery.waitTicks(40);var server=world.getServer();
   server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_health_regeneration false");server.runCommand("time set 6000");
   server.runCommand("fill -16 100 -12 16 100 24 polished_deepslate");
   server.runCommand("fill -12 101 20 12 109 20 gray_concrete");
   gallery.prepare(CameraType.THIRD_PERSON_FRONT,-90,-15);
   server.runOnServer(s->{actor=gallery.actor(s);var p=player(s);p.setGameMode(GameType.SURVIVAL);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);observed.clear();LifeOwnerEvents.observe(e->{observed.add(e);gallery.observe(e);});});
   gallery.waitTicks(12);
   server.runOnServer(s->{var p=player(s);p.setHealth(4);paid(p,"self","heal");});gallery.waitTicks(8);
   server.runOnServer(s->check(player(s).getHealth()>4,"Heal actually increases recipient health"));
   gallery.waitTicks(10);
   server.runOnServer(s->{var p=player(s);p.addEffect(new MobEffectInstance(MobEffects.POISON,200));p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200));p.setTicksFrozen(120);paid(p,"self","cleanse");});gallery.waitTicks(8);
   server.runOnServer(s->{var p=player(s);check(!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.SLOWNESS)&&p.getTicksFrozen()==0,"Cleanse removes real effects and freeze");});
   gallery.waitTicks(10);
   server.runOnServer(s->{var p=player(s);p.getFoodData().setFoodLevel(4);p.addEffect(new MobEffectInstance(MobEffects.HUNGER,200));paid(p,"self","nourish");});gallery.waitTicks(8);
   server.runOnServer(s->{var p=player(s);check(p.getFoodData().getFoodLevel()>4&&!p.hasEffect(MobEffects.HUNGER),"Nourish actually feeds player and removes Hunger");});
   gallery.waitTicks(10);
   server.runOnServer(s->{var p=player(s);var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(80);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);paid(p,"self","restore");});gallery.waitTicks(8);
   int repaired=server.computeOnServer(s->{var p=player(s);int damage=p.getMainHandItem().getDamageValue();check(damage<80,"Restore actually repairs the equipped tool");return damage;});
   server.runOnServer(s->paid(player(s),"self","restore"));gallery.waitTicks(8);
   server.runOnServer(s->check(player(s).getMainHandItem().getDamageValue()==repaired,"Repair cooldown prevents second repair"));
   gallery.waitTicks(10);
   server.runOnServer(s->{var p=player(s);p.setHealth(8);p.addEffect(new MobEffectInstance(MobEffects.POISON,200));p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200));paid(p,"self","remedy");});gallery.waitTicks(8);
   server.runOnServer(s->{var p=player(s);check(!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.SLOWNESS)&&p.hasEffect(MobEffects.SPEED)&&p.hasEffect(MobEffects.REGENERATION)&&p.getHealth()>8,"Remedy really converts ailments and heals");p.removeAllEffects();});
   gallery.waitTicks(10);
   server.runOnServer(s->{var p=player(s);p.setHealth(8);p.addEffect(new MobEffectInstance(MobEffects.POISON,200));p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200));paid(p,"self","ashen_mercy");});gallery.waitTicks(8);
   server.runOnServer(s->{var p=player(s);check(!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.SLOWNESS)&&p.hasEffect(MobEffects.FIRE_RESISTANCE)&&p.getHealth()>8&&p.getHealth()<=12,"Ashen Mercy converts real removals into capped healing");p.removeAllEffects();p.setHealth(8);});
   server.runOnServer(s->paid(player(s),"self","ashen_mercy"));gallery.waitTicks(8);
   server.runOnServer(s->check(player(s).getHealth()==8,"Clean target cannot invent converted healing"));
   gallery.waitTicks(10);
   server.runOnServer(s->{var p=player(s);s.overworld().setBlock(new BlockPos(1,100,0),Blocks.FARMLAND.defaultBlockState(),3);s.overworld().setBlock(new BlockPos(1,101,0),Blocks.WHEAT.defaultBlockState(),3);paid(p,"self","grow");});gallery.waitTicks(10);
   server.runOnServer(s->check(s.overworld().getBlockState(new BlockPos(1,101,0)).getValue(CropBlock.AGE)>0,"Grow mutates an actual crop"));
   server.runOnServer(s->{s.overworld().setBlock(new BlockPos(1,101,0),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),3);paid(player(s),"self","harvest");});gallery.waitTicks(8);
   server.runOnServer(s->{var p=player(s);check(s.overworld().getBlockState(new BlockPos(1,101,0)).is(Blocks.WHEAT)&&s.overworld().getBlockState(new BlockPos(1,101,0)).getValue(CropBlock.AGE)==0,"Harvest replants actual mature crop");check(s.overworld().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4)).stream().anyMatch(e->e.getItem().is(Items.WHEAT)),"Harvest creates real wheat drops");});
   gallery.waitTicks(10);server.runOnServer(s->paid(player(s),"self","glimmer"));gallery.waitTicks(8);
   server.runOnServer(s->{int cells=0;for(var pos:BlockPos.betweenClosed(-3,100,-3,3,102,3))if(s.overworld().getBlockState(pos).is(Blocks.GLOW_LICHEN))cells++;check(cells>0,"Glimmer places actual supported lichen");});
   // A real hostile Bolt proves delivery at its struck victim, not at the casting hand.
   server.runCommand("summon pillager 0.5 101 10.5 {NoAI:1b,Silent:1b}");gallery.waitTicks(8);
   gallery.prepare(CameraType.FIRST_PERSON,0,0);
   server.runOnServer(s->paid(player(s),"bolt","venom"));gallery.waitTicks(20);
   server.runOnServer(s->{var targets=s.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.illager.Pillager.class,new net.minecraft.world.phys.AABB(-2,100,7,3,106,14));check(!targets.isEmpty()&&targets.getFirst().getHealth()<targets.getFirst().getMaxHealth()&&targets.getFirst().hasEffect(MobEffects.POISON),"Connected-owner Venom Bolt produces actual hostile damage and Poison");targets.forEach(net.minecraft.world.entity.Entity::discard);});
   gallery.waitTicks(10);gallery.prepare(CameraType.THIRD_PERSON_FRONT,-90,-15);server.runOnServer(s->{var p=player(s);p.removeAllEffects();p.setHealth(4);paid(p,"self","regrowth");});gallery.waitTicks(8);
   // Keep the existing second paid admission, now on the same connected owner, then follow vanilla stages.
   server.runOnServer(s->{var p=player(s);p.removeAllEffects();p.setHealth(4);paid(p,"self","regrowth");});gallery.waitTicks(8);
   server.runOnServer(s->check(player(s).getEffect(MobEffects.REGENERATION).getAmplifier()==0,"Regrowth begins real stageI"));gallery.waitTicks(60);
   server.runOnServer(s->check(player(s).getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Regrowth advances real stageII"));gallery.waitTicks(60);
   server.runOnServer(s->{var p=player(s);check(p.getEffect(MobEffects.REGENERATION).getAmplifier()==2&&p.getHealth()>4,"Regrowth advances real stageIII and heals");});
   server.runOnServer(s->{
    var expected=Set.of("heal","grow","regrowth","cleanse","venom","nourish","harvest","restore","glimmer","remedy","ashen_mercy");
    var actual=new java.util.HashSet<String>();for(var e:observed)if(e.moment()!=LifeOwnerEvents.Moment.REFUSED)actual.add(e.rune());
    check(actual.containsAll(expected),"All eleven real gameplay outcomes have actual owner observations: "+actual);
    check(observed.stream().filter(e->e.rune().equals("heal")).anyMatch(e->e.delta()>0),"Heal observation uses actual health gained");
    check(observed.stream().filter(e->e.rune().equals("restore")).anyMatch(e->e.detail().equals("equipment:mainhand")&&e.delta()>0),"Actual repaired mainhand owns repair event");
    check(observed.stream().filter(e->e.rune().equals("ashen_mercy")).anyMatch(e->e.units()>0&&e.delta()>0&&e.delta()<=4),"Mercy observation derives actual removal and capped healing");
   });
  }finally{LifeOwnerEvents.clear();observed.clear();actor=null;gallery=null;}
 }
 private void paid(net.minecraft.server.level.ServerPlayer p,String...paths){gallery.beforePayment(p);check(SpellCaster.edit(p,0,Arrays.stream(paths).map(s->"wildercord:"+s).toList())==null,"Accepted real delivery");Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Survival delivery spends mana");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
