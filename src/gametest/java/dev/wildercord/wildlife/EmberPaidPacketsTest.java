package dev.wildercord.wildlife;

import dev.wildercord.cast.Mastery;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual recipe, block-use and paid CastSpell packets; never calls an Ember gameplay helper or creates a Cast. */
public final class EmberPaidPacketsTest implements FabricClientGameTest {
 private static final BlockPos P=new BlockPos(0,101,0), Q=P.east();
 private static final AtomicBoolean DENY=new AtomicBoolean();
 private CinderBailiff frost, freeze;
 private WildercordConfig original, baseline;
 private int price;
 public void runTest(ClientGameTestContext c){
  PlayerBlockBreakEvents.BEFORE.register((l,p,at,state,be)->!DENY.get()||(!at.equals(P)&&!at.equals(Q)));
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("difficulty normal");
   original=w.getServer().computeOnServer(s->Config.get());
   try{
    w.getServer().runOnServer(s->{baseline=copy(original,Map.of("manaRegenMultiplier",0.0));current(baseline);var l=s.overworld();var p=p(s);
     for(int x=-6;x<=18;x++)for(int z=-6;z<=6;z++){l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);for(int y=101;y<=104;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
     p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();place(p,.5,2.5,Vec3.atCenterOf(P));
     Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);
     var inputs=List.of(new ItemStack(Items.FERN),new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CHARCOAL),ItemStack.EMPTY);
     for(int i=0;i<4;i++)p.inventoryMenu.getSlot(i+1).set(inputs.get(i));p.inventoryMenu.broadcastChanges();
     check(p.inventoryMenu.getSlot(0).getItem().is(EmberContent.FERN_ITEM),"Actual registered inventory recipe resolves Cinder Fern");
    });
    c.waitTicks(5);c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,0,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(5);
    w.getServer().runOnServer(s->{var p=p(s);check(p.getInventory().countItem(EmberContent.FERN_ITEM)==1,"Real crafting-result packet earns exactly one root");for(int i=1;i<=4;i++)check(p.inventoryMenu.getSlot(i).getItem().isEmpty(),"Every actual recipe input consumed");int slot=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(EmberContent.FERN_ITEM))slot=i;check(slot>=0,"Crafted root is in actual inventory");var earned=p.getInventory().getItem(slot).copy();p.getInventory().setItem(slot,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,earned);p.inventoryMenu.broadcastChanges();});
    c.waitTicks(5);use(c,P.below(),Direction.UP,new Vec3(.5,101,.5));c.waitTicks(5);
    w.getServer().runOnServer(s->{check(age(s,P)==0&&p(s).getMainHandItem().isEmpty(),"Actual BlockItem packet plants the crafted root and consumes it");});
    aimFern(w);paid(c,w,List.of(Runes.TOUCH,Runes.GROW));check(w.getServer().computeOnServer(s->age(s,P)==1),"Actual paid Touch Grow admits one growth");
    paid(c,w,List.of(Runes.TOUCH,Runes.GROW));check(w.getServer().computeOnServer(s->age(s,P)==2),"A separate paid Grow matures the actual root");
    w.getServer().runOnServer(s->{current(copy(baseline,Map.of("spellsEditBlocks",false)));check(!Config.get().spellsEditBlocks(),"Actual live server config disables spell terrain editing before ordinary harvesting");});
    use(c,P,Direction.SOUTH,new Vec3(.5,101.15,.5));c.waitTicks(6);
    w.getServer().runOnServer(s->check(age(s,P)==0&&items(s)==1,"Actual harvest packet retains AGE0 root and awards exactly one frond across inventory plus drops"));
    w.getServer().runOnServer(s->current(baseline));
    aimFern(w);paid(c,w,List.of(Runes.TOUCH,Runes.FIRE));w.getServer().runOnServer(s->check(age(s,P)==1&&!s.overworld().getBlockState(P).getValue(CinderFernBlock.COOLED),"Actual Fire owner heats and advances root"));
    paid(c,w,List.of(Runes.TOUCH,Runes.TIDEBREATH));w.getServer().runOnServer(s->check(age(s,P)==1&&s.overworld().getBlockState(P).getValue(CinderFernBlock.COOLED),"Actual Water owner cools without a free age increase"));
    w.getServer().runOnServer(s->{s.overworld().setBlock(P,EmberContent.FERN.defaultBlockState(),2);s.overworld().setBlock(Q,EmberContent.FERN.defaultBlockState(),2);place(p(s),.5,1.5,Vec3.atCenterOf(P));});
    paid(c,w,List.of(Runes.SELF,Runes.GROW,Runes.ECHO));c.waitTicks(15);
    w.getServer().runOnServer(s->{check(age(s,P)+age(s,Q)==1,"Real delayed Echo shares one fern mutation admission across the two eligible roots");check(Spellbooks.mana(p(s))==200-price,"Echo pays the authoritative mana price once, with actual regeneration disabled");s.overworld().removeBlock(Q,false);s.overworld().setBlock(P,EmberContent.FERN.defaultBlockState(),2);});
    DENY.set(true);try{aimFern(w);paid(c,w,List.of(Runes.TOUCH,Runes.GROW));check(w.getServer().computeOnServer(s->age(s,P)==0),"Real claim veto denies actual paid Grow");}finally{DENY.set(false);}
    w.getServer().runOnServer(s->current(copy(baseline,Map.of("maxBlocks",0))));paid(c,w,List.of(Runes.TOUCH,Runes.GROW));w.getServer().runOnServer(s->{check(age(s,P)==0,"Real paid cast cannot exceed zero configured block budget");current(baseline);});
    w.getServer().runOnServer(s->{place(p(s),8.5,2.5,new Vec3(8.5,101.5,.5));frost=spawn(s,8.5);check(frost.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p(s)),1),"Actual ordinary idle hit starts retaliatory AI");});
    warning(c,w,frost);w.getServer().runOnServer(s->place(p(s),8.5,2.5,frost.getBoundingBox().getCenter()));paid(c,w,List.of(Runes.TOUCH,Runes.FROST));
    w.getServer().runOnServer(s->check(frost.pose()==CinderBailiff.RECOVERING&&frost.getTicksFrozen()>0&&frost.attackReady()>s.overworld().getGameTime(),"Paid actual Frost mutation interrupts real Warning and owns finite rest"));
    w.getServer().runOnServer(s->{place(p(s),13.5,2.5,new Vec3(13.5,101.5,.5));freeze=spawn(s,13.5);freeze.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p(s)),1);});
    warning(c,w,freeze);w.getServer().runOnServer(s->place(p(s),13.5,2.5,freeze.getBoundingBox().getCenter()));paid(c,w,List.of(Runes.TOUCH,Runes.FREEZE));
    w.getServer().runOnServer(s->{check(freeze.pose()==CinderBailiff.RECOVERING&&freeze.getTicksFrozen()>0,"Actual paid Freeze owner cools real Warning after admitting its frost state");freeze.discard();});
    w.getServer().runCommand("difficulty peaceful");c.waitTicks(25);w.getServer().runOnServer(s->check(frost.isAlive()&&!frost.isRemoved()&&frost.getTarget()==null&&frost.pose()!=CinderBailiff.WARNING&&frost.pose()!=CinderBailiff.FANNING,"Existing neutral Bailiff survives actual Peaceful and cancels aggression"));
    w.getServer().runCommand("difficulty normal");w.getServer().runOnServer(s->{
     var wildlife=copy(baseline.wildlife(),Map.of("enabled",true,"cinderBailiff",true,"spawnMultiplier",1.0));current(copy(baseline,Map.of("wildlife",wildlife)));var site=new BlockPos(4,101,0);s.overworld().setBlock(site.east(),EmberContent.FERN.defaultBlockState().setValue(CinderFernBlock.AGE,2),2);
     Long seed=null;for(long i=0;i<200;i++){long sample=951L+i*100003L;if(rule(s,site,EntitySpawnReason.NATURAL,sample)){seed=sample;break;}}check(seed!=null,"Real registered natural habitat predicate has an admitted ordinary seeded chance");
     current(copy(baseline,Map.of("wildlife",copy(wildlife,Map.of("cinderBailiff",false)))));check(!rule(s,site,EntitySpawnReason.NATURAL,seed)&&!rule(s,site,EntitySpawnReason.CHUNK_GENERATION,seed),"Actual per-species switch refuses both natural admissions");check(rule(s,site,EntitySpawnReason.COMMAND,seed),"Manual spawn remains available under species switch");
    });
   }finally{DENY.set(false);w.getServer().runOnServer(s->current(original));}
  }
 }
 private void paid(ClientGameTestContext c,TestSingleplayerContext w,List<RuneDef> runes){w.getServer().runOnServer(s->{var p=p(s);check(SpellCaster.edit(p,0,runes.stream().map(RuneDef::id).toList())==null,"Actual saved spell edit accepts candidate owner route");var compiled=SpellCompiler.compile(runes);check(!compiled.isEmpty(),"Real compiler admits owner spell");price=Heart.manaCost(p,compiled,Heart.secretCost(p,runes)*Mastery.costFactor(p,runes));check(price>0&&price<200,"Finite actual mana admission price");Spellbooks.setMana(p,200);Spellbooks.setReadyAt(p,0,0);});c.waitTicks(3);c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));c.waitTicks(4);w.getServer().runOnServer(s->{check(Spellbooks.mana(p(s))==200-price,"Actual CastSpell receiver spends one exact authoritative Survival price");check(Spellbooks.readyAt(p(s),0)>s.overworld().getGameTime(),"Actual cast owns native cooldown");});}
 private static void warning(ClientGameTestContext c,TestSingleplayerContext w,CinderBailiff b){for(int i=0;i<40;i++){c.waitTicks(1);if(w.getServer().computeOnServer(s->b.pose()==CinderBailiff.WARNING))return;}throw new AssertionError("Actual retaliatory AI must raise Warning before paid cooling");}
 private static CinderBailiff spawn(MinecraftServer s,double x){var b=EmberContent.BAILIFF.create(s.overworld(),EntitySpawnReason.COMMAND);check(b!=null,"Registered creature factory");b.snapTo(x,101,.5,0,0);b.setPersistenceRequired();s.overworld().addFreshEntity(b);return b;}
 private static void aimFern(TestSingleplayerContext w){w.getServer().runOnServer(s->place(p(s),.5,2.5,new Vec3(.5,101.15,.5)));}
 private static void place(ServerPlayer p,double x,double z,Vec3 target){Vec3 from=new Vec3(x,101+p.getEyeHeight(),z),d=target.subtract(from);float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z)),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));p.teleportTo(p.level(),x,101,z,Set.<Relative>of(),yaw,pitch,false);}
 private static void use(ClientGameTestContext c,BlockPos at,Direction face,Vec3 point){c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(point,face,at,false)));}
 private static int age(MinecraftServer s,BlockPos p){var state=s.overworld().getBlockState(p);check(state.is(EmberContent.FERN),"Actual retained fern cell exists");return state.getValue(CinderFernBlock.AGE);}
 private static int items(MinecraftServer s){int n=p(s).getInventory().countItem(EmberContent.FERN_ITEM);for(var e:s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(P).inflate(8)))if(e.getItem().is(EmberContent.FERN_ITEM))n+=e.getItem().getCount();return n;}
 private static boolean rule(MinecraftServer s,BlockPos p,EntitySpawnReason reason,long seed){return SpawnPlacements.checkSpawnRules(EmberContent.BAILIFF,s.overworld(),reason,p,RandomSource.create(seed));}
 private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 @SuppressWarnings("unchecked") private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();var types=new Class<?>[parts.length];var args=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();args[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig config){try{var field=Config.class.getDeclaredField("current");field.setAccessible(true);field.set(null,config);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
