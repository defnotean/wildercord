package dev.wildercord.wildlife;
import dev.wildercord.cast.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import java.util.*;
/** Actual client construction, amphibian routes, finite rests, wake conditions and saved habitat. */
public final class ReedRefugeTest implements FabricClientGameTest {
 private static final BlockPos ROOF=new BlockPos(3,101,3),DRY=new BlockPos(-4,102,-1);
 private static LanternNewt first,second,third;private static UUID saved;private static long firstRest,secondRest,savedRest;
 @Override public void runTest(ClientGameTestContext c) {
  EcologyReturnProbeChecks.verify();
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.ReedRefugeTest\",\"seed\":\"{}\"}",l.getSeed());for(int x=-12;x<=12;x++)for(int z=-8;z<=12;z++) {l.setBlock(new BlockPos(x,100,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,101,z),x>=-5 && x<=7 && z>=0 && z<=8?Blocks.WATER.defaultBlockState():Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlock(new BlockPos(x,102,z),Blocks.AIR.defaultBlockState(),2);}
    for(var row:List.of(List.of(Items.BAMBOO,Items.BAMBOO,Items.BAMBOO,WetlandGarden.FLOSS,Items.STRING,Items.SEAGRASS,WetlandShelters.REFUGE_ITEM),List.of(Items.BOOK,WetlandGarden.FLOSS,Items.SEAGRASS,WetlandShelters.NOTES))) {
     var input=CraftingInput.of(3,2,java.util.stream.IntStream.range(0,6).mapToObj(i -> i<row.size()-1?new ItemStack(row.get(i)):ItemStack.EMPTY).toList());var r=s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);check(r.isPresent() && r.get().value().assemble(input).is(row.getLast()),"Native refuge/notes recipe");
    }
    p(s).setGameMode(GameType.SURVIVAL);p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandShelters.REFUGE_ITEM,2));aim(s,new Vec3(3.5,101,3.5),3);
   });c.waitTicks(5);place(c,ROOF.below());c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(ROOF).is(WetlandShelters.REFUGE) && s.overworld().getBlockState(ROOF).getValue(BlockStateProperties.WATERLOGGED) && p(s).getMainHandItem().getCount()==1,"Actual underwater placement retains water and consumes one roof");aim(s,Vec3.atCenterOf(DRY),2.5);});c.waitTicks(5);place(c,DRY.below());c.waitTicks(5);
   w.getServer().runOnServer(s -> {
    check(s.overworld().getBlockState(DRY).is(WetlandShelters.REFUGE) && !s.overworld().getBlockState(DRY).getValue(BlockStateProperties.WATERLOGGED) && p(s).getMainHandItem().isEmpty(),"Dry placement stays dry and consumes final roof");
    check(s.overworld().getBlockEntity(ROOF)==null,"Habitat has no ticking block entity");p(s).setGameMode(GameType.CREATIVE);p(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);aim(s,new Vec3(3.5,101.3,3.5),3);
    s.overworld().setBlock(new BlockPos(2,101,3),Blocks.GLASS.defaultBlockState(),2);first=spawn(s,.5,3.5);
   });
   awaitRest(c,w,()->first,"Newt swims around a separate obstacle into a real waterlogged roof");
   w.getServer().runOnServer(s -> {check(first.getX()>3.2 && first.getZ()>3.2 && first.getZ()<3.8,"Settled body is actually beneath roof");firstRest=first.refugeReady();check(first.getHealth()==10 && first.pearlReady()==0 && pearls(s)==0,"Rest heals nothing, grants nothing and changes no pearl clock");second=spawn(s,.5,4.5);aim(s,first.position(),3);});c.waitTicks(16);int firstId=w.getServer().computeOnServer(srv -> first.getId());check(c.computeOnClient(mc -> ((LanternNewt)mc.level.getEntity(firstId)).rest>.9F),"Actual resting pose blends on synchronized client");shot(c,"reed_refuge_sleeping_newt");c.waitTicks(30);
   w.getServer().runOnServer(s -> {check(first.resting() && !second.resting(),"A roof holds one resting visitor: first="+first.resting()+" at "+first.position()+", second="+second.resting()+" at "+second.position());});c.waitTicks(110);
   w.getServer().runOnServer(s -> {check(!first.resting() && first.refugeReady()==firstRest && first.pearlReady()==0,"Rest ends naturally without renewing its deadline");first.setNoAi(true);first.teleportTo(-3,101.1,6);s.overworld().removeBlock(new BlockPos(2,101,3),false);});
   awaitRest(c,w,()->second,"Other newt can use the vacated roof");
   w.getServer().runOnServer(s -> {secondRest=second.refugeReady();p(s).setGameMode(GameType.SURVIVAL);p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SEAGRASS,3));aim(s,second.position(),2.5);});c.waitTicks(4);feed(c,w,()->second);c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!second.resting() && second.refugeReady()==secondRest && second.pearlReady()>s.overworld().getGameTime() && p(s).getMainHandItem().getCount()==2 && pearls(s)==1,"Actual feeding wakes animal, spends one plant, gives ordinary pearl and preserves rest");second.setNoAi(true);second.teleportTo(-3,101.1,5);p(s).setGameMode(GameType.CREATIVE);p(s).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);third=spawn(s,.5,3.5);});
   w.getServer().runCommand("time set 18000");c.waitTicks(100);check(w.getServer().computeOnServer(s -> !third.resting() && third.refugeReady()==0),"Clear night is for active newts, not refuge rests");
   w.getServer().runCommand("weather rain");awaitRest(c,w,()->third,"Rainy night leads to refuge");w.getServer().runOnServer(s -> {savedRest=third.refugeReady();aim(s,third.position(),3);});c.waitTicks(16);shot(c,"reed_refuge_rainy_night");
   w.getServer().runOnServer(s -> {var plan=SpellCompiler.compile(List.of(Runes.TOUCH,Runes.TIDEBREATH));CastEngine.onHit(new Cast(p(s)),plan.root().groups.getFirst(),new Cast.Hit(List.of(third),third.position(),new Vec3(0,0,1),p(s).position(),third.blockPosition(),Direction.UP,false),null);});c.waitTicks(5);
   w.getServer().runOnServer(s -> {check(!third.resting() && third.response()>0 && third.refugeReady()==savedRest && third.pearlReady()==0,"Actual spell impact wakes without renewing rest or yielding resources");saved=third.getUUID();p(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WetlandShelters.NOTES));var book=p(s).getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT);check(book!=null && book.pages().size()==3,"Lore book has three native pages");});c.waitTicks(5);c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);check(c.computeOnClient(mc -> mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen),"Actual book use opens native reader");shot(c,"reed_refuge_field_notes");c.runOnClient(mc -> mc.gui.setScreen(null));save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(35);w.getServer().runOnServer(s -> {dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.ReedRefugeTest#reopen\",\"seed\":\"{}\"}",s.overworld().getSeed());third=(LanternNewt)s.overworld().getEntity(saved);check(third!=null && third.refugeReady()==savedRest && !third.resting(),"Full restart preserves exact refuge deadline without a phantom rest pose");check(s.overworld().getBlockState(ROOF).getValue(BlockStateProperties.WATERLOGGED) && !s.overworld().getBlockState(DRY).getValue(BlockStateProperties.WATERLOGGED),"Wet and dry habitat placements survive restart");check(third.getHealth()==10 && third.pearlReady()==0,"Restart grants no shelter reward or heal");});c.waitTicks(50);check(w.getServer().computeOnServer(s -> !third.resting() && third.refugeReady()==savedRest),"Restart cannot bypass refuge rest");
   w.getServer().runOnServer(s -> {third=spawn(s,3.5,3.5);});awaitRest(c,w,()->third,"New visitor settles before danger check");
   w.getServer().runOnServer(s -> {savedRest=third.refugeReady();third.hurtServer(s.overworld(),s.overworld().damageSources().generic(),1);});c.waitTicks(5);
   check(w.getServer().computeOnServer(s -> !third.resting() && third.frightened() && third.refugeReady()==savedRest && third.getHealth()==9),"Damage wakes immediately, preserves rest and provides no healing");
   w.getServer().runOnServer(s -> {third.setNoAi(true);third.teleportTo(-3,101.1,4);third=spawn(s,3.5,3.5);});awaitRest(c,w,()->third,"Another visitor settles before roof removal");
   w.getServer().runOnServer(s -> {savedRest=third.refugeReady();s.overworld().destroyBlock(ROOF,true);});c.waitTicks(5);
   check(w.getServer().computeOnServer(s -> !third.resting() && third.refugeReady()==savedRest && s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(ROOF).inflate(2),e -> e.getItem().is(WetlandShelters.REFUGE_ITEM)).stream().mapToInt(e -> e.getItem().getCount()).sum()==1),"Breaking roof wakes visitor and drops exactly one roof");
  }
 }
 private static LanternNewt spawn(MinecraftServer s,double x,double z) {var n=WetlandContent.NEWT.create(s.overworld(),EntitySpawnReason.COMMAND);n.snapTo(x,101.1,z,0,0);n.getRandom().setSeed(314);s.overworld().addFreshEntity(n);return n;}
 private static void awaitRest(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Supplier<LanternNewt> target,String why) {
  var probe=w.getServer().computeOnServer(s->EcologyReturnProbe.begin(EcologyReturnProbe.REED,s.overworld(),target.get()));
  try{for(int i=0;i<100;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> target.get().resting()))return;}
   throw new AssertionError(w.getServer().computeOnServer(s -> why+": "+EcologyReturnProbe.body(target.get())));
  }finally{probe.close();}
 }

 private static void feed(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Supplier<LanternNewt> n) {int id=w.getServer().computeOnServer(s -> n.get().getId());c.runOnClient(mc -> {var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});}
 private static void place(ClientGameTestContext c,BlockPos ground) {c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false)));}
 private static int pearls(MinecraftServer s) {return s.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(-12,100,-8,12,106,12),e -> e.getItem().is(WetlandContent.DUSK_PEARL)).stream().mapToInt(e -> e.getItem().getCount()).sum()+java.util.stream.IntStream.range(0,p(s).getInventory().getContainerSize()).map(i -> p(s).getInventory().getItem(i).is(WetlandContent.DUSK_PEARL)?p(s).getInventory().getItem(i).getCount():0).sum();}
 private static ServerPlayer p(MinecraftServer s) {return s.getPlayerList().getPlayers().getFirst();}
 private static void aim(MinecraftServer s,Vec3 target,double distance) {var p=p(s);if(p.getAbilities().mayfly) {p.getAbilities().flying=true;p.onUpdateAbilities();}var at=target.add(0,.2,-distance);var d=target.add(0,.15,0).subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 private static void shot(ClientGameTestContext c,String name) {c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
