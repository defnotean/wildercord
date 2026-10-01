package dev.wildercord.world.dungeons;
import dev.wildercord.cast.*;
import dev.wildercord.content.dungeons.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Builds every new piece in a real level, then exercises physical controls and actual spell hooks. */
public final class ExpeditionsTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("time set 6000");
   List<BlockPos> centers=world.getServer().computeOnServer(server->{
    var p=server.getPlayerList().getPlayers().getFirst();var level=p.level();p.setGameMode(GameType.CREATIVE);
    var result=new ArrayList<BlockPos>();
    var pieces=new ArrayList<DungeonPiece>(List.of(new ClockworkCryptPiece(0,100,0,Direction.EAST),new LivingGreenhousePiece(64,100,0,Direction.NORTH),new MovingSkyRuinPiece(128,100,0,Direction.SOUTH)));
    for(int kind=0;kind<3;kind++)for(int variant=0;variant<3;variant++){
     if(pieces.get(kind).variant()==variant)continue;
     int x=kind*80,z=128+variant*64;DungeonPiece candidate;
     do{candidate=switch(kind){case 0->new ClockworkCryptPiece(x++,100,z,Direction.WEST);case 1->new LivingGreenhousePiece(x++,100,z,Direction.WEST);default->new MovingSkyRuinPiece(x++,100,z,Direction.WEST);};}while(candidate.variant()!=variant);
     pieces.add(candidate);
    }
    for(var piece:pieces) {
     var bounds=piece.getBoundingBox();
     for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++)level.getChunk(x,z);
     piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(744),bounds,new ChunkPos(bounds.minX()>>4,bounds.minZ()>>4),BlockPos.ZERO);
     int chests=0;BlockPos found=null;
     for(var at:BlockPos.betweenClosed(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ())) {
      if(level.getBlockState(at).is(Blocks.CHEST))chests++;
      if(level.getBlockEntity(at) instanceof ExpeditionMechanismEntity)found=at.immutable();
     }
     check(found!=null,"new expedition has its mechanism");check(chests>=2,"new expedition has hall and vault rewards");if(result.size()<3)result.add(found);
     if(piece instanceof ClockworkCryptPiece&&piece.variant()!=0)check(level.getBlockState(piece.localPosition(4,1,21)).isAir(),"clock variant opens its side gallery");
     if(piece instanceof LivingGreenhousePiece)check(level.getBlockState(piece.localPosition(14,1,35)).isAir()==(piece.variant()!=0),"garden variant changes its rear entrance");
     if(piece instanceof MovingSkyRuinPiece&&piece.variant()!=0)check(level.getBlockState(piece.localPosition(5,6,12)).is(Blocks.SMOOTH_QUARTZ_SLAB),"sky variant has a physical outer route");
    }
    return List.copyOf(result);
   });
   var clock=centers.get(0);var garden=centers.get(1);var sky=centers.get(2);
   var enemy=world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var level=p.level();near(p,clock);
    var mechanism=(ExpeditionMechanismEntity)level.getBlockEntity(clock);var node=SpellCompiler.compile(List.of(Runes.TOUCH,Runes.FIRE)).root().groups.getFirst().effects.getFirst();
    Effects.apply(new Cast(p),node,new Cast.Hit(List.of(),Vec3.atCenterOf(clock),p.getLookAngle(),p.position(),clock,Direction.UP,false),1);
    check(mechanism.stored()>0,"actual harmful spell is stored by the clock");
    var forward=level.getBlockState(clock).getValue(ExpeditionMechanism.FACING);var spot=clock.relative(forward,5);
    var mob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);mob.setNoAi(true);mob.snapTo(spot.getX()+.5,spot.getY(),spot.getZ()+.5,0,0);level.addFreshEntity(mob);
    float before=mob.getHealth();mechanism.use(p,ItemStack.EMPTY);check(mechanism.stored()==0,"release consumes stored attack exactly once");check(mob.getHealth()==before,"warning precedes damage");return new EnemySnapshot(mob.getId(),before);
   });
   context.waitTicks(25);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(((LivingEntity)p.level().getEntity(enemy.id())).getHealth()<enemy.health(),"stored attack lands after warning");near(p,garden);});
   for(int i=0;i<3;i++) {
    world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var mechanism=(ExpeditionMechanismEntity)p.level().getBlockEntity(garden);
     var node=SpellCompiler.compile(List.of(Runes.TOUCH,Runes.GROW)).root().groups.getFirst().effects.getFirst();
     Effects.apply(new Cast(p),node,new Cast.Hit(List.of(),Vec3.atCenterOf(garden),p.getLookAngle(),p.position(),garden,Direction.UP,false),1);
    });context.waitTicks(21);
   }
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var level=p.level();var mechanism=(ExpeditionMechanismEntity)level.getBlockEntity(garden);
    check(mechanism.completed(),"life spells cleanse garden");var forward=level.getBlockState(garden).getValue(ExpeditionMechanism.FACING);
    for(int step=2;step<=8;step++)check(level.getBlockState(garden.relative(forward,step).below()).is(Blocks.MOSS_BLOCK),"cleansing grows a usable moss path");
    mechanism.use(p,new ItemStack(Items.SHEARS));check(mechanism.completed(),"completed heart cannot be harvested again");
    BlockPos harvest=garden.offset(2,0,0);level.setBlockAndUpdate(harvest,DungeonBlocks.GARDEN.defaultBlockState());near(p,harvest);
    var other=(ExpeditionMechanismEntity)level.getBlockEntity(harvest);other.use(p,new ItemStack(Items.SHEARS));check(other.completed(),"physical harvest alternative works");
    near(p,sky);var anchor=(ExpeditionMechanismEntity)level.getBlockEntity(sky);var facing=level.getBlockState(sky).getValue(ExpeditionMechanism.FACING);int old=anchor.skyStep();
    BlockPos previous=sky.relative(facing,3).relative(facing.getClockWise(),old-1).below();check(level.getBlockState(previous).is(Blocks.IRON_BLOCK),"initial stone follows rotated sky layout");
    anchor.use(p,ItemStack.EMPTY);BlockPos next=sky.relative(facing,3).relative(facing.getClockWise(),anchor.skyStep()-1).below();
    check(level.getBlockState(previous).isAir()&&level.getBlockState(next).is(Blocks.IRON_BLOCK),"physical sky control moves collision platform");
    int step=anchor.skyStep();BlockPos blocked=sky.relative(facing,6).relative(facing.getClockWise(),(step+1)%3-1);
    level.setBlockAndUpdate(blocked,Blocks.STONE.defaultBlockState());check(!anchor.shiftPlatforms(level)&&anchor.skyStep()==step&&level.getBlockState(next).is(Blocks.IRON_BLOCK),"one obstructed landing preserves every platform and group offset");level.setBlockAndUpdate(blocked,Blocks.AIR.defaultBlockState());
    p.teleportTo(level,sky.getX()+.5,sky.getY()-5,sky.getZ()+.5,Set.<Relative>of(),0,0,false);
   });
   context.waitTicks(25);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(p.getY()>=sky.getY(),"fall recovery returns player to sky anchor");});
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.teleportTo(p.level(),sky.getX()+.5,sky.getY()-4,sky.getZ()+.5,Set.<Relative>of(),0,0,false);p.setDeltaMovement(new Vec3(0,-3,0));p.fallDistance=30;p.needsSync=true;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));});
   context.waitTicks(5);world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(p.getY()>=sky.getY()&&p.fallDistance==0&&p.getHealth()==20,"fast survival recovery clears accumulated fall damage and downward momentum");p.setGameMode(GameType.CREATIVE);});
   context.runOnClient(mc->mc.getWindow().setWindowed(1600,900));
   for(int i=0;i<centers.size();i++) {
    var at=centers.get(i);world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var facing=p.level().getBlockState(at).getValue(ExpeditionMechanism.FACING);var camera=at.relative(facing.getOpposite(),4);
     p.teleportTo(p.level(),camera.getX()+.5,at.getY()+1,camera.getZ()+.5,Set.<Relative>of(),facing.toYRot(),15,false);});context.waitTicks(10);
    context.takeScreenshot(TestScreenshotOptions.of("expedition_"+List.of("clockwork","greenhouse","sky").get(i)).disableCounterPrefix());
    if(i>0){world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setNoGravity(true);p.teleportTo(p.level(),at.getX()-25,at.getY()+24,at.getZ()-25,Set.<Relative>of(),-45,32,false);});context.waitTicks(10);
     context.takeScreenshot(TestScreenshotOptions.of("expedition_"+List.of("clockwork","greenhouse","sky").get(i)+"_overview").disableCounterPrefix());}
   }
   world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().setNoGravity(false));
  }
 }
 private static void near(net.minecraft.server.level.ServerPlayer p,BlockPos pos){p.teleportTo(p.level(),pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,Set.<Relative>of(),0,20,false);}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 private record EnemySnapshot(int id,float health) {}
}
