package dev.wildercord.cast;

import dev.wildercord.content.PhysicalBlocks;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 18 remaining paid runtime cases, each in a fresh world to prevent ward contamination.
 * Never invokes Effects.apply or owner gameplay methods. Reflection reads admissions only;
 * success assertions depend on damage/healing/blocks/movement, not a fabricated HIT packet.
 * Observation assertions require the real owner hooks.
 */
public final class LifeOutcomeStatefulTest implements FabricClientGameTest {
 private static final List<String> CASES=List.of("reversal","bramble","haven","fortune","bloom","soulbond","second_wind","lifebloom","root_bulwark","bloomstep","stitchtime","vinelash","ancient_seed","moonpetal","sporebloom","glowvine","rootsnare","drowse");
 private static ServerPlayer actor;
 private LifeOutcomeGallery gallery;
 private static LivingEntity target;private static Arrow incoming,outgoing;private static double before;private static Vec3 from;
 private static final List<LifeOwnerEvents.Event> observed=new ArrayList<>();
 @Override public void runTest(ClientGameTestContext c){try(var view=new LifeOutcomeGallery(c,"life_outcome_stateful")){gallery=view;for(String rune:CASES)one(c,rune);}finally{gallery=null;}}
 private void one(ClientGameTestContext c,String rune){
  try(var world=c.worldBuilder().create()){
   gallery.waitTicks(30);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_health_regeneration false");
   server.runCommand("fill -12 100 -12 12 100 24 polished_deepslate");server.runCommand("fill -5 101 18 5 108 18 stone");
   boolean aimed=Set.of("root_bulwark","bloomstep","vinelash","drowse").contains(rune);
   gallery.prepare(aimed?CameraType.FIRST_PERSON:CameraType.THIRD_PERSON_FRONT,aimed?0:-90,aimed?0:-15);
   server.runOnServer(s->{actor=gallery.actor(s);var p=player(s);p.setGameMode(GameType.SURVIVAL);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);p.setHealth(12);p.removeAllEffects();observed.clear();LifeOwnerEvents.observe(e->{observed.add(e);gallery.observe(e);});});gallery.waitTicks(8);
   switch(rune){
    case "reversal" -> {
     server.runOnServer(s->{var p=player(s);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{var p=player(s);p.hurtServer(s.overworld(),s.overworld().damageSources().magic(),100);check(p.isAlive()&&p.getHealth()>0&&DeathsDoor.resting(p)>0,"Actual lethal blow spends Reversal once");paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->check(!map(Wards.class,"REVERSAL").containsKey(player(s).getUUID()),"DeathsDoor refuses new Reversal"));
    }
    case "bramble" -> {
     server.runOnServer(s->{var p=player(s);target=foe(p,2.5);before=target.getHealth();paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->player(s).hurtServer(s.overworld(),s.overworld().damageSources().mobAttack(target),2));gallery.waitTicks(8);
     server.runOnServer(s->check(target.getHealth()<before,"Real incoming mob attack triggers Bramble retaliation"));
    }
    case "haven" -> {
     server.runOnServer(s->{var p=player(s);target=foe(p,9);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{var p=player(s);incoming=new Arrow(EntityTypes.ARROW,s.overworld());incoming.setOwner(target);incoming.setPos(.5,102,4);incoming.setDeltaMovement(0,0,-.4);s.overworld().addFreshEntity(incoming);outgoing=new Arrow(EntityTypes.ARROW,s.overworld());outgoing.setOwner(p);outgoing.setPos(.5,102,2);outgoing.setDeltaMovement(0,0,.4);s.overworld().addFreshEntity(outgoing);});gallery.waitTicks(3);
     server.runOnServer(s->{check(incoming.getDeltaMovement().z>0,"Haven reflects an actual inbound foreign projectile");check(outgoing.getDeltaMovement().z>0,"Haven leaves allied outgoing projectile alone");});
    }
    case "fortune" -> {
     server.runOnServer(s->{var p=player(s);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{var p=player(s);check(map(Innates.class,"FORTUNE").containsKey(p.getUUID()),"Fortune admits real finite mark");target=foe(p,2.5);s.overworld().getRandom().setSeed(90210);boolean lucky=false;
      for(int i=0;i<24;i++){target.setHealth(target.getMaxHealth());target.damageCooldownTime=0;float hp=target.getHealth();target.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p),2);if(hp-target.getHealth()>2.01)lucky=true;}
      check(lucky,"Seeded actual melee damage takes a real lucky branch");
      check(observed.stream().anyMatch(e->e.rune().equals("fortune")&&e.moment()==LifeOwnerEvents.Moment.TRIGGER&&e.delta()<0&&target.getUUID().equals(e.recipient())),"Lucky echo publishes actual extra damage at victim");});gallery.waitTicks(205);
     server.runOnServer(s->{var p=player(s);Object until=map(Innates.class,"FORTUNE").get(p.getUUID());check(until==null||((Long)until)<s.overworld().getGameTime(),"Fortune cannot remain active past its actual duration");});
     // The extra XP branch remains a separate native gate; this verifies actual seeded melee damage.
    }
    case "bloom" -> {
     server.runOnServer(s->{var p=player(s);target=pet(p,2);target.setHealth(4);paid(p,"self",rune);});gallery.waitTicks(8);
     server.runOnServer(s->{check(player(s).hasEffect(MobEffects.REGENERATION),"Bloom helps real direct recipient");check(target.hasEffect(MobEffects.REGENERATION),"Bloom reaches a real owned ally by pollen hop");});
    }
    case "soulbond" -> {
     server.runOnServer(s->{var p=player(s);target=pet(p,2);before=target.getHealth();paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{var p=player(s);p.hurtServer(s.overworld(),s.overworld().damageSources().magic(),4);check(target.getHealth()<before,"Soulbond transfers an actual wound to admitted owned partner");});gallery.waitTicks(3);server.runOnServer(s->target.discard());gallery.waitTicks(22);
     server.runOnServer(s->{var p=player(s);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->check(!map(FusedLife.class,"BONDS").containsKey(player(s).getUUID()),"Removed partner and solo cast cannot retain bond"));
    }
    case "second_wind" -> {
     server.runOnServer(s->{var p=player(s);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{var p=player(s);p.hurtServer(s.overworld(),s.overworld().damageSources().magic(),100);check(p.isAlive()&&p.getHealth()>0&&map(FusedLife.class,"SPENT").containsKey(p.getUUID()),"Second Wind answers real lethal event once");paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->check(!map(FusedLife.class,"WINDS").containsKey(player(s).getUUID()),"Spent lockout prevents duplicate save"));
    }
    case "lifebloom" -> {
     server.runOnServer(s->{var p=player(s);p.setHealth(2);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{before=player(s).getHealth();check(before>2,"Lifebloom actual immediate heal after preparation release");});gallery.waitTicks(25);
     server.runOnServer(s->{var p=player(s);check(p.getHealth()>before,"Lifebloom actual scheduled heal");paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->check(map(FusedLife.class,"BLOOMS").size()==1,"Renewal retains one real bloom"));gallery.waitTicks(110);
     server.runOnServer(s->check(map(FusedLife.class,"BLOOMS").isEmpty(),"Finite bloom terminates after true pulses and burst"));
    }
    case "root_bulwark" -> {
     server.runOnServer(s->paid(player(s),"bolt",rune));gallery.waitTicks(35);
     server.runOnServer(s->{check(count(s.overworld(),PhysicalBlocks.ROOT)>0,"Root Bulwark places real leased root cells");before=PhysicalMagic.active();check(before>0,"Actual cell lease tracked");});gallery.waitTicks(175);
     server.runOnServer(s->check(count(s.overworld(),PhysicalBlocks.ROOT)==0&&PhysicalMagic.active()==0,"Finite root lease restores actual cells"));
    }
    case "bloomstep" -> {
     server.runOnServer(s->{var p=player(s);from=p.position();paid(p,"bolt",rune);});gallery.waitTicks(35);
     server.runOnServer(s->{var p=player(s);check(p.position().distanceTo(from)>3&&p.position().distanceTo(from)<=32,"Bloomstep lands caster at real safe bounded destination");check(p.hasEffect(MobEffects.REGENERATION),"Bloomstep applies regeneration at actual arrival");});
    }
    case "stitchtime" -> {
     server.runOnServer(s->{var p=player(s);paid(p,"self",rune);});gallery.waitTicks(6);
     server.runOnServer(s->{var p=player(s);p.hurtServer(s.overworld(),s.overworld().damageSources().magic(),4);before=p.getHealth();});gallery.waitTicks(90);
     server.runOnServer(s->{var p=player(s);check(p.getHealth()>before&&map(SignatureWards.class,"STITCHES").isEmpty(),"Stitchtime closes recorded real wound and removes finite mark");});
    }
    case "vinelash" -> {
     server.runOnServer(s->{var p=player(s);target=foe(p,10.5);((Mob)target).setNoAi(false);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);before=target.getHealth();from=target.position();paid(p,"bolt",rune);});gallery.waitTicks(20);
     server.runOnServer(s->{check(target.getHealth()<before&&target.hasEffect(MobEffects.SLOWNESS),"Vinelash damages and slows actual struck foe");check(target.getZ()<from.z,"Real tendon impulse pulls victim toward caster");});
    }
    case "ancient_seed" -> {
     server.runOnServer(s->{var p=player(s);crop(s.overworld(),1,0,0);paid(p,"self",rune);});gallery.waitTicks(10);
     int first=server.computeOnServer(s->s.overworld().getBlockState(new BlockPos(1,101,0)).getValue(CropBlock.AGE));gallery.waitTicks(245);
     server.runOnServer(s->check(s.overworld().getBlockState(new BlockPos(1,101,0)).getValue(CropBlock.AGE)>first,"Ancient Seed changes real crop across delayed sweeps"));
    }
    case "moonpetal" -> {
     server.runOnServer(s->{var p=player(s);p.setHealth(4);target=foe(p,2.5);before=target.getHealth();paid(p,"self",rune);});gallery.waitTicks(8);
     server.runOnServer(s->{check(player(s).getHealth()>4,"Moonpetal really heals ally");check(target.getHealth()<before,"Moonpetal really cuts separate hostile recipient");});
    }
    case "sporebloom" -> {
     server.runOnServer(s->{var p=player(s);p.getFoodData().setFoodLevel(4);target=foe(p,2.5);before=target.getHealth();paid(p,"self",rune);});gallery.waitTicks(35);
     server.runOnServer(s->{check(player(s).getFoodData().getFoodLevel()>4,"Sporebloom feeds actual allied player");check(target.getHealth()<before,"Sporebloom scheduled custom spores hurt actual foe");});
    }
    case "glowvine" -> {
     server.runCommand("fill -4 106 -4 4 106 4 stone");server.runOnServer(s->paid(player(s),"self",rune));gallery.waitTicks(8);
     server.runOnServer(s->check(count(s.overworld(),Blocks.CAVE_VINES)+count(s.overworld(),Blocks.CAVE_VINES_PLANT)>0,"Glowvine produces actual supported hanging plant"));
    }
    case "rootsnare" -> {
     server.runOnServer(s->{var p=player(s);target=foe(p,2.5);before=target.getHealth();paid(p,"self",rune);});gallery.waitTicks(8);
     server.runOnServer(s->{check(target.getHealth()<before&&target.hasEffect(MobEffects.SLOWNESS),"Rootsnare actually harms and holds foe");before=target.getHealth();});gallery.waitTicks(110);
     server.runOnServer(s->check(target.getHealth()==before,"Stationary snared victim accumulates no invented travel tax"));
    }
    case "drowse" -> {
     server.runOnServer(s->{var p=player(s);target=foe(p,10.5);paid(p,"bolt",rune);});gallery.waitTicks(18);
     server.runOnServer(s->{check(CraftedRunes.asleep(target),"Drowse actually admits nonboss sleeper");target.hurtServer(s.overworld(),s.overworld().damageSources().magic(),1);check(!CraftedRunes.asleep(target),"Real damage wakes actual sleeper");});
    }
    default -> throw new AssertionError("Unwritten native case "+rune);
   }
   server.runOnServer(s->{
    var own=observed.stream().filter(e->e.rune().equals(rune)&&e.moment()!=LifeOwnerEvents.Moment.REFUSED).toList();
    check(!own.isEmpty(),"Actual owner publishes observed outcome for "+rune);
    check(own.stream().allMatch(e->e.level()==s.overworld()&&Double.isFinite(e.anchor().lengthSqr())),"Every observation has real loaded world anchor");
    if(Set.of("vinelash","sporebloom","rootsnare","bramble").contains(rune))check(own.stream().anyMatch(e->e.delta()<0&&target!=null&&target.getUUID().equals(e.recipient())),"Actual damaged recipient owns negative delta");
    if(Set.of("ancient_seed","glowvine","root_bulwark").contains(rune))check(own.stream().anyMatch(e->e.recipient()==null&&e.units()==1),"Successful edited cell owns material observation");
    if(rune.equals("bloomstep"))check(own.stream().anyMatch(e->e.recipient()!=null&&e.recipient().equals(player(s).getUUID())&&e.secondary()!=null&&e.secondary().distanceTo(from)<.1),"Movement observation records actual departure and caster");
   });
  }finally{LifeOwnerEvents.clear();observed.clear();target=null;incoming=null;outgoing=null;before=0;from=null;actor=null;}
 }
 private static ServerPlayer player(net.minecraft.server.MinecraftServer s){return actor;}
 private static Pillager foe(ServerPlayer p,double z){var t=EntityTypes.PILLAGER.create(p.level(),EntitySpawnReason.COMMAND);check(t!=null,"Real hostile fixture");t.setPos(.5,101,z);t.setNoAi(true);p.level().addFreshEntity(t);return t;}
 private static Wolf pet(ServerPlayer p,double z){var t=EntityTypes.WOLF.create(p.level(),EntitySpawnReason.COMMAND);check(t!=null,"Real consenting owned ally");t.tame(p);t.setNoAi(true);t.setPos(.5,101,z);p.level().addFreshEntity(t);return t;}
 private static void crop(net.minecraft.server.level.ServerLevel l,int x,int z,int age){l.setBlock(new BlockPos(x,100,z),Blocks.FARMLAND.defaultBlockState(),3);l.setBlock(new BlockPos(x,101,z),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,age),3);}
 private static int count(net.minecraft.server.level.ServerLevel l,Block b){int n=0;for(var p:BlockPos.betweenClosed(-6,101,-6,6,108,20))if(l.getBlockState(p).is(b))n++;return n;}
 private static Map<?,?> map(Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return (Map<?,?>)f.get(null);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private void paid(ServerPlayer p,String...paths){gallery.beforePayment(p);check(SpellCaster.edit(p,0,Arrays.stream(paths).map(x->"wildercord:"+x).toList())==null,"Real learned spell accepted");Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float start=Spellbooks.mana(p);for(String path:paths){var rune=Runes.get("wildercord:"+path).orElseThrow();if(Runes.innate(rune)){p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,"");SpellCaster.cast(p,0);check(Spellbooks.mana(p)==start,"Learned foreign innate refuses before payment: "+path);p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,rune.id());}}SpellCaster.cast(p,0);check(Spellbooks.mana(p)<start,"Actual Survival payment");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
