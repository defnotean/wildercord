package dev.wildercord.cast;
import dev.wildercord.content.*;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native paid casts, actual altar transactions, finite resources and a full world reopen. */
public final class FieldFusionTest implements FabricClientGameTest {
 private static final List<RuneDef> NEW=List.of(Runes.SPRINGBED,Runes.CINDER_SIEVE,Runes.ASHEN_MERCY,Runes.CLOCKROOT,Runes.SKYLATCH,Runes.THRESHERWIND);
 private static int externalAlly;
 public void runTest(ClientGameTestContext c){
  TestWorldSave save;
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_regeneration false");w.getServer().runCommand("time set 6000");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
    for(var q:BlockPos.betweenClosed(new BlockPos(-16,100,-16),new BlockPos(40,100,40)))p.level().setBlock(q,Blocks.STONE_BRICKS.defaultBlockState(),2);
    p.setGameMode(GameType.SURVIVAL);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);pose(p,0,0,0);
    altar(p);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.COAL,2));drop(p,Items.RAW_IRON,2,new Vec3(.5,101,1.5));drop(p,Items.RAW_COPPER,1,new Vec3(1,101,1.5));cast(p,Runes.SELF,Runes.CINDER_SIEVE);
   });c.waitTicks(15);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(count(p,Items.IRON_INGOT)==2&&count(p,Items.COPPER_INGOT)==1&&count(p,Items.COAL)==1,"Paid sieve processes eligible drops and consumes one fuel");
    // No input/fuel loss when every output slot is full.
    for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(0,new ItemStack(Items.COAL,2));var raw=drop(p,Items.RAW_IRON,3,new Vec3(.5,101,1.5));var paid=new Cast(p);apply(p,paid,Runes.CINDER_SIEVE,hit(p,p.position(),null));check(raw.getItem().getCount()==3&&count(p,Items.COAL)==2,"Full inventory refusal preserves input and fuel");raw.discard();
    p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.COAL,4));var foreign=drop(p,Items.RAW_IRON,3,new Vec3(.5,101,1.5));foreign.setTarget(UUID.randomUUID());apply(p,new Cast(p),Runes.CINDER_SIEVE,hit(p,p.position(),null));check(foreign.isAlive()&&foreign.getItem().getCount()==3&&count(p,Items.COAL)==4,"Foreign targeted goods preserved");foreign.discard();
    var delayed=drop(p,Items.RAW_IRON,2,new Vec3(.5,101,1.5));delayed.setPickUpDelay(100);apply(p,new Cast(p),Runes.CINDER_SIEVE,hit(p,p.position(),null));check(delayed.getItem().getCount()==2&&count(p,Items.COAL)==4,"Pickup delay refusal preserves resources");delayed.discard();
    var inputs=drop(p,Items.RAW_IRON,32,new Vec3(.5,101,1.5));var payment=new Cast(p);apply(p,payment,Runes.CINDER_SIEVE,hit(p,p.position(),null));apply(p,payment.pulse(),Runes.CINDER_SIEVE,hit(p,p.position(),null));check(inputs.getItem().getCount()==16&&count(p,Items.IRON_INGOT)==16&&count(p,Items.COAL)==3,"One payment processes at most sixteen, even with repetition");inputs.discard();
    p.setHealth(10);p.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));p.setRemainingFireTicks(80);cast(p,Runes.SELF,Runes.ASHEN_MERCY);
   });c.waitTicks(15);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(!p.hasEffect(MobEffects.POISON)&&!p.isOnFire()&&p.hasEffect(MobEffects.FIRE_RESISTANCE),"Paid mercy consumes harmful conditions and grants brief heat ward");check(p.getHealth()<=14&&p.getHealth()>10,"Mercy heals no more than four health");
    p.removeAllEffects();p.setHealth(10);var payment=new Cast(p);apply(p,payment,Runes.ASHEN_MERCY,hit(p,p.position(),null));check(p.getHealth()==10,"Clean target cannot mint healing");p.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));apply(p,payment.pulse(),Runes.ASHEN_MERCY,hit(p,p.position(),null));check(p.hasEffect(MobEffects.POISON),"Mercy cannot renew on the same UUID/payment");p.removeAllEffects();
    pose(p,0,0,0);cast(p,Runes.SELF,Runes.SKYLATCH);
   });c.waitTicks(30);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(FieldFusions.skyMarks()==1&&p.getY()>101.3,"Paid Skylatch lifts an ally above its ordinary floor");check(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Skylatch never grants flight flags");
    var wolf=EntityTypes.WOLF.create(p.level(),EntitySpawnReason.COMMAND);check(wolf!=null,"External lift ally created");wolf.setTame(true,false);wolf.setOwner(p);wolf.setNoAi(true);wolf.setPos(2.5,101,5.5);p.level().addFreshEntity(wolf);externalAlly=wolf.getId();apply(p,new Cast(p),Runes.SKYLATCH,hit(p,wolf.position(),wolf));check(FieldFusions.skyMarks()==2,"Second eligible ally obtains its own finite latch");wolf.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200,1));});c.waitTicks(5);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var wolf=(LivingEntity)p.level().getEntity(externalAlly);check(FieldFusions.skyMarks()==1&&wolf.hasEffect(MobEffects.LEVITATION)&&wolf.getEffect(MobEffects.LEVITATION).getAmplifier()==1&&wolf.getEffect(MobEffects.LEVITATION).getDuration()>190,"External Levitation supersedes latch without being removed or shortened");wolf.discard();p.setShiftKeyDown(true);});c.waitTicks(5);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(FieldFusions.skyMarks()==0&&p.hasEffect(MobEffects.SLOW_FALLING),"Crouching releases latch with finite descent");p.setShiftKeyDown(false);p.removeAllEffects();pose(p,0,0,0);
    var mob=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);check(mob!=null,"Husk created");mob.setNoAi(true);mob.setPos(.5,101,2.5);mob.setOnGround(true);mob.addTag("fieldfusion-clock");p.level().addFreshEntity(mob);cast(p,Runes.TOUCH,Runes.CLOCKROOT);
   });c.waitTicks(14);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(FieldFusions.clockMarks()==1,"Paid touch records actual hostile ground imprint");var mob=p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(8)).stream().filter(t->t.getType()==EntityTypes.HUSK).findFirst().orElseThrow();mob.setPos(3,101,2.5);});c.waitTicks(5);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var mob=p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(8)).stream().filter(t->t.getType()==EntityTypes.HUSK).findFirst().orElseThrow();check(FieldFusions.clockMarks()==0&&mob.getX()<1,"Clockroot returns once to a checked floor");mob.discard();
    guards(p);basin(p);p.teleportTo(p.level(),10.5,102,-.5,Set.<Relative>of(),0,60,false);cast(p,Runes.TOUCH,Runes.SPRINGBED);
   });c.waitTicks(15);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(FieldFusions.clockMarks()==0,"Unsafe recorded floor releases harmlessly");var stranded=p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(20)).stream().filter(t->t.getType()==EntityTypes.HUSK).findFirst().orElseThrow();check(stranded.getX()==8.5,"Clockroot cannot return into a removed floor");stranded.discard();check(p.level().getBlockState(new BlockPos(10,101,0)).is(Blocks.WATER),"Paid Springbed fills its bounded vessel");check(((CropBlock)Blocks.WHEAT).getAge(p.level().getBlockState(new BlockPos(12,101,0)))==1,"Springbed grows existing bank crop one stage");
    for(var q:BlockPos.betweenClosed(new BlockPos(19,100,1),new BlockPos(21,100,3))){p.level().setBlockAndUpdate(q,Blocks.FARMLAND.defaultBlockState());p.level().setBlockAndUpdate(q.above(),((CropBlock)Blocks.WHEAT).getStateForAge(7));}pose(p,20,-.5,57);cast(p,Runes.TOUCH,Runes.THRESHERWIND);
   });c.waitTicks(25);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();int replanted=0;for(var q:BlockPos.betweenClosed(new BlockPos(19,101,1),new BlockPos(21,101,3)))if(p.level().getBlockState(q).is(Blocks.WHEAT)&&((CropBlock)Blocks.WHEAT).getAge(p.level().getBlockState(q))==0)replanted++;check(replanted>0&&count(p,Items.WHEAT)>0,"Paid travelling thresh collects native grain and replants using seed drops");check(FieldFusions.clockMarks()==0&&FieldFusions.skyMarks()==0,"No completed movement watchers remain");});save=w.getWorldSave();
  }
  try(var w=save.open()){c.waitTicks(30);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.level().getBlockState(new BlockPos(10,101,0)).is(Blocks.WATER),"Springbed water survives full reopen");check(count(p,Items.IRON_INGOT)==16&&count(p,Items.COAL)==3,"Processed goods and fuel survive full reopen");check(FieldFusions.clockMarks()==0&&FieldFusions.skyMarks()==0&&!p.getAbilities().mayfly,"No latch or clock survives server restart");});}
 }
 private static void guards(ServerPlayer p){
  var level=p.level();var m=EntityTypes.HUSK.create(level,EntitySpawnReason.COMMAND);check(m!=null,"Safety husk created");m.setNoAi(true);m.setPos(5.5,101,5.5);m.setOnGround(true);level.addFreshEntity(m);
  var veto=new java.util.concurrent.atomic.AtomicBoolean(true);var floor=m.blockPosition().below();net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((l,player,pos,state,entity)->!veto.get()||!pos.equals(floor));
  try{apply(p,new Cast(p),Runes.CLOCKROOT,hit(p,m.position(),m));check(FieldFusions.clockMarks()==0,"Claim veto refuses a hostile ground imprint");}finally{veto.set(false);}
  apply(p,new Cast(p),Runes.ASHEN_MERCY,hit(p,m.position(),m));check(!m.hasEffect(MobEffects.FIRE_RESISTANCE),"Hostile target cannot receive allied mercy");
  m.setPermanentlyInvulnerable(true);apply(p,new Cast(p),Runes.CLOCKROOT,hit(p,m.position(),m));check(FieldFusions.clockMarks()==0,"Permanently immune target cannot receive a forced return");m.setPermanentlyInvulnerable(false);
  var mercy=new Cast(p);for(int i=0;i<9;i++){var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);check(wolf!=null,"Owned ally created");wolf.setTame(true,false);wolf.setOwner(p);wolf.setNoAi(true);wolf.setPos(15+i*.2,101,6);level.addFreshEntity(wolf);wolf.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));apply(p,i%2==0?mercy:mercy.pulse(),Runes.ASHEN_MERCY,hit(p,wolf.position(),wolf));check(wolf.hasEffect(MobEffects.POISON)==(i==8),"One mercy payment admits only eight allied UUIDs including pulse copies");wolf.discard();}
  var boss=EntityTypes.WITHER.create(level,EntitySpawnReason.COMMAND);check(boss!=null,"Safety boss created");boss.setPos(6.5,101,5.5);boss.setOnGround(true);level.addFreshEntity(boss);apply(p,new Cast(p),Runes.CLOCKROOT,hit(p,boss.position(),boss));check(FieldFusions.clockMarks()==0,"Boss cannot receive a forced return");boss.discard();
  p.setGameMode(GameType.SPECTATOR);apply(p,new Cast(p),Runes.SKYLATCH,hit(p,p.position(),null));check(FieldFusions.skyMarks()==0,"Spectator cannot receive Skylatch");p.setGameMode(GameType.SURVIVAL);
  var far=drop(p,Items.RAW_IRON,2,p.position().add(3.5,0,3.5));int fuel=count(p,Items.COAL);apply(p,new Cast(p),Runes.CINDER_SIEVE,hit(p,p.position(),null));check(far.isAlive()&&far.getItem().getCount()==2&&count(p,Items.COAL)==fuel,"Sieve uses circular four-block reach rather than AABB corners");far.discard();
  apply(p,new Cast(p),Runes.CLOCKROOT,hit(p,m.position(),m));check(FieldFusions.clockMarks()==1,"Safe loaded ground can still receive its first imprint");level.setBlockAndUpdate(floor,Blocks.AIR.defaultBlockState());m.setPos(8.5,101,5.5);
 }
 private static void altar(ServerPlayer p){var at=new BlockPos(0,101,-3);p.level().setBlockAndUpdate(at,WildercordBlocks.FUSION_ALTAR.defaultBlockState());p.setExperienceLevels(100);
  for(var rune:NEW){var recipe=Fusions.signatureFor(rune).orElseThrow();var menu=new FusionAltarMenu(0,p.getInventory(),ContainerLevelAccess.create(p.level(),at));menu.getSlot(0).set(RuneItem.stack(recipe.a()));menu.getSlot(1).set(RuneItem.stack(recipe.b()));menu.getSlot(FusionAltarMenu.CATALYST).set(new ItemStack(Items.AMETHYST_SHARD));check(menu.plan().ready()&&menu.plan().signature()&&menu.plan().result()==rune,"Actual altar plans "+rune.path());int xp=p.experienceLevel;check(menu.clickMenuButton(p,FusionAltarMenu.BUTTON_FUSE),"Actual Fuse button accepts "+rune.path());check(RuneItem.runeOf(menu.getSlot(FusionAltarMenu.RESULT).getItem()).orElseThrow()==rune,"Actual altar result "+rune.path());check(p.experienceLevel==xp-Fusions.COMBINE_XP&&menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(FusionAltarMenu.CATALYST).getItem().isEmpty(),"Altar transaction consumes each input exactly once");}
 }
 private static void basin(ServerPlayer p){for(var q:BlockPos.betweenClosed(new BlockPos(9,100,-1),new BlockPos(12,101,2)))p.level().setBlockAndUpdate(q,Blocks.STONE_BRICKS.defaultBlockState());for(int z=0;z<2;z++)p.level().setBlockAndUpdate(new BlockPos(10,101,z),Blocks.AIR.defaultBlockState());p.level().setBlockAndUpdate(new BlockPos(12,100,0),Blocks.FARMLAND.defaultBlockState());p.level().setBlockAndUpdate(new BlockPos(12,101,0),((CropBlock)Blocks.WHEAT).getStateForAge(0));}
 private static void pose(ServerPlayer p,double x,double z,float pitch){p.teleportTo(p.level(),x+.5,101,z,Set.<Relative>of(),0,pitch,false);}
 private static void cast(ServerPlayer p,RuneDef shape,RuneDef effect){check(SpellCaster.edit(p,0,List.of(shape.id(),effect.id()))==null,"Native spell edits "+effect.path());Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Actual Survival cast pays "+effect.path());}
 private static Cast.Hit hit(ServerPlayer p,Vec3 point,LivingEntity t){return new Cast.Hit(t==null?List.of(p):List.of(t),point,new Vec3(0,-1,0),p.position(),null,Direction.UP,t==null);}
 private static void apply(ServerPlayer p,Cast cast,RuneDef effect,Cast.Hit hit){var node=SpellCompiler.compile(List.of(Runes.SELF,effect)).root().groups.getFirst().effects.getFirst();Effects.apply(cast,node,hit);}
 private static ItemEntity drop(ServerPlayer p,Item item,int count,Vec3 at){var e=new ItemEntity(p.level(),at.x,at.y,at.z,new ItemStack(item,count));e.setDeltaMovement(Vec3.ZERO);p.level().addFreshEntity(e);return e;}
 private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
