package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.Set;

/** A dedicated test arena, leaving the player's ordinary world intact. Shared by server operators. */
public final class PracticeRoom {
 private PracticeRoom() {}
 public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,Wildercord.id("practice"));
 /** How long after a mob last hurt them a player waits to step into the arena (5 seconds). */
 private static final int COMBAT_TICKS = 100;
 private static final AttachmentType<GlobalPos> RETURN = AttachmentRegistry.create(Wildercord.id("practice_return"),
  b -> b.persistent(GlobalPos.CODEC).copyOnDeath());
 public static void init() {
  ServerTickEvents.END_SERVER_TICK.register(server -> {
   for(ServerPlayer player:server.getPlayerList().getPlayers()) if(player.level().dimension().equals(DIMENSION) && player.getY()<78)
    player.teleportTo(player.level(),.5,81,.5,Set.<Relative>of(),0,0,false);
  });
 }
 public static int enter(ServerPlayer player) {
  ServerLevel world=player.level().getServer().getLevel(DIMENSION);
  if(world==null) { player.sendSystemMessage(Component.translatable("message.wildercord.practice.restart")); return 0; }
  // The arena is an instant step away: not a way out of a fight. Leaving it puts you back where you were.
  if(!player.level().dimension().equals(DIMENSION) && !player.isCreative() && player.getLastHurtByMob()!=null && player.tickCount-player.getLastHurtByMobTimestamp()<COMBAT_TICKS) {
   player.sendSystemMessage(Component.translatable("message.wildercord.practice.in_combat")); return 0;
  }
  if(!player.level().dimension().equals(DIMENSION)) player.setAttached(RETURN,GlobalPos.of(player.level().dimension(),player.blockPosition()));
  build(world);
  player.teleportTo(world,.5,81,.5,Set.<Relative>of(),0,0,false);
  if(world.getEntitiesOfClass(TrainingDummy.class,new AABB(-12,79,-8,13,85,33)).isEmpty()) targets(world,3,false);
  player.sendSystemMessage(Component.translatable("message.wildercord.practice.enter"));
  return 1;
 }
 public static int leave(ServerPlayer player) {
  GlobalPos back=player.getAttached(RETURN);
  if(back==null || !player.level().dimension().equals(DIMENSION)) return 0;
  ServerLevel world=player.level().getServer().getLevel(back.dimension());
  if(world==null)return 0;
  var at=back.pos(); player.teleportTo(world,at.getX()+.5,at.getY(),at.getZ()+.5,Set.<Relative>of(),player.getYRot(),player.getXRot(),false);
  player.removeAttached(RETURN);
  return 1;
 }
 public static int targets(ServerLevel world,int count,boolean moving) {
  if(!world.dimension().equals(DIMENSION))return 0;
  count=Math.clamp(count,1,24);
  world.getEntitiesOfClass(TrainingDummy.class,new AABB(-12,79,-8,13,85,33)).forEach(TrainingDummy::discard);
  for(int i=0;i<count;i++) {
   var dummy=WildercordEntities.TRAINING_DUMMY.create(world,EntitySpawnReason.TRIGGERED);
   if(dummy==null)continue;
   dummy.snapTo((i%5-2)*4+.5,81,10+(i/5)*4+.5);
   dummy.setPracticeMoving(moving); world.addFreshEntity(dummy);
  }
  return count;
 }
 public static int benchmark(ServerPlayer player) {
  if(!player.level().dimension().equals(DIMENSION))return 0;
  VisualMetrics.reset();
  player.sendSystemMessage(Component.translatable("message.wildercord.practice.benchmark"));
  ServerLevel world=player.level();
  Scheduler.later(200,()-> {
   if(!player.isRemoved() && player.level()==world) player.sendSystemMessage(Component.literal(VisualMetrics.report()));
  });
  return 1;
 }
 private static void build(ServerLevel world) {
  // This arena has its own dimension. Load only its eight chunks when entering.
  for(int x=-1;x<=0;x++)for(int z=-1;z<=2;z++)world.getChunk(x,z);
  if(world.getBlockState(new BlockPos(0,79,-4)).is(Blocks.BEDROCK))return;
  for(int x=-12;x<=12;x++)for(int z=-8;z<=32;z++) {
   world.setBlock(new BlockPos(x,79,z),Blocks.BEDROCK.defaultBlockState(),3);
   world.setBlock(new BlockPos(x,80,z),(z%5==0?Blocks.CHISELED_STONE_BRICKS:Blocks.POLISHED_DEEPSLATE).defaultBlockState(),3);
   if(x==-12 || x==12 || z==-8 || z==32) world.setBlock(new BlockPos(x,81,z),Blocks.IRON_BARS.defaultBlockState(),3);
  }
  for(int x:new int[]{-10,10})for(int z:new int[]{-6,8,20,30})world.setBlock(new BlockPos(x,81,z),Blocks.SEA_LANTERN.defaultBlockState(),3);
 }
}
