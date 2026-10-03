package dev.wildercord.world.dungeons;

import dev.wildercord.cast.*;
import dev.wildercord.content.*;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Actual Y254 meditation, rotated stair geometry, survival walking and persistent generated vaults. */
public final class MountainStormTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c) {
  var saveHolder=new Object[1];
  UUID[] playerId=new UUID[1];
  BlockPos[] vault=new BlockPos[1];
  try(var world=c.worldBuilder().create()) {
   c.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("gamerule fall_damage false");world.getServer().runCommand("time set 6000");
   world.getServer().runCommand("fill -6 253 -6 6 253 6 stone");
   world.getServer().runCommand("fillbiome -8 248 -8 8 260 8 minecraft:stony_peaks");
   world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();playerId[0]=p.getUUID();p.setGameMode(GameType.SURVIVAL);
    p.teleportTo(p.level(),.5,254,.5,Set.<Relative>of(),0,0,false);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(WildercordItems.BLANK_RUNE,2));
    check(Attunement.placeOf(p).biome().equals("minecraft:stony_peaks"),"fixture uses the previously missing mountain biome");
   });
   c.runOnClient(mc->{mc.getWindow().setWindowed(1600,900);mc.options.guiScale().set(2);mc.resizeGui();mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(true);});
   c.waitTicks(80);
   world.getServer().runOnServer(s->check(Mana.of(s.getPlayerList().getPlayers().getFirst()).meditating(),"real crouch input starts mountain meditation"));
   shot(c,"mountain_attunement_y254");c.waitTicks(380);
   world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
    check(p.getMainHandItem().getCount()==1,"ritual consumes exactly one blank");
    check(java.util.stream.IntStream.range(0,p.getInventory().getContainerSize()).mapToObj(p.getInventory()::getItem).anyMatch(i->RuneItem.runeOf(i).map(r->r.id().equals(Runes.SUMMIT_WIND.id())).orElse(false)),"complete mountain ritual produces Summit Wind");
    check(Attunement.restLeft(p,Attunements.byId("peaks").orElseThrow(),p.level().getGameTime())>0,"mountain attunement records its finite reward rest");
   });c.runOnClient(mc->mc.options.keyShift.setDown(false));c.waitTicks(10);
   StormSpirePiece chosen=world.getServer().computeOnServer(s->{var l=s.overworld();var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);StormSpirePiece first=null;
    for(var facing:Direction.Plane.HORIZONTAL){var piece=new StormSpirePiece(100+facing.get2DDataValue()*80,100,0,facing);var b=piece.getBoundingBox();
     for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)l.getChunk(x,z);
     // Generate in chunk-sized calls, as natural generation does, to expose clipping errors.
     for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++){
      var clip=new BoundingBox(x*16,b.minY(),z*16,x*16+15,b.maxY(),z*16+15);
      piece.postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),RandomSource.create(89),clip,new ChunkPos(x,z),BlockPos.ZERO);
     }
     check(b.getYSpan()==43,"crown fits recorded structure height");
     var saved=piece.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(l));
     check(saved.getIntOr("StormLayout",0)==2&&new StormSpirePiece(saved).getBoundingBox().equals(b),"new layout and rotated box survive piece serialization");
     check(l.getBlockState(piece.localPosition(11,41,11)).getBlock() instanceof LightningRodBlock,"lightning crown exists in every rotation");
     check(l.getBlockState(piece.localPosition(11,22,18)).getValue(DungeonAltarBlock.KIND)==DungeonAltarBlock.Kind.STORM,"boss altar retained");
     check(l.getBlockState(piece.localPosition(11,22,15)).is(WildercordBlocks.RUNE_SEAL),"storm and wind vault gate retained");
     for(int[] at:new int[][]{{18,8,10},{4,15,17},{6,22,19},{17,22,19}}) check(l.getBlockState(piece.localPosition(at[0],at[1],at[2])).is(Blocks.CHEST),"four actual reward chests retained");
     for(int flight=0;flight<3;flight++)for(int step=0;step<7;step++) {
      int x=flight==1?7:4,z=flight==1?10-step:4+step,y=flight*7+1+step;
      var at=piece.localPosition(x,y,z);var block=l.getBlockState(at);var next=piece.localPosition(x,y,z+(flight==1?-1:1));
      var ascending=Direction.getNearest(next.getX()-at.getX(),0,next.getZ()-at.getZ(),null);
      check(block.getBlock() instanceof StairBlock&&block.getValue(StairBlock.FACING)==ascending,"stair faces its higher neighbor in every orientation");
      check(l.getBlockState(at.above()).isAir()&&l.getBlockState(at.above(2)).isAir(),"all stairs retain native walking headroom");
     }
     var guards=l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(b.minX(),b.minY(),b.minZ(),b.maxX()+1,b.maxY()+1,b.maxZ()+1));
     check(guards.size()==3,"chunk-clipped generation creates exactly three guards");
     // Isolate staircase traversal from guard spell knockback; combat is covered by the boss suites.
     guards.forEach(g->g.setNoAi(true));
     if(first==null)first=piece;
    }
    var old=new StormSpirePiece(500,100,0,Direction.SOUTH).createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(l));
    old.remove("StormLayout");old.putInt("Height",29);var oldBox=new BoundingBox(500,100,0,522,128,22);
    old.put("BB",BoundingBox.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,oldBox).getOrThrow());
    var restored=new StormSpirePiece(old);restored.postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),RandomSource.create(90),oldBox,new ChunkPos(31,0),BlockPos.ZERO);
    check(l.getBlockState(restored.localPosition(11,1,11)).is(Blocks.SCAFFOLDING),"old saved pieces finish with their original climb");
    check(restored.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(l)).getIntOr("StormLayout",0)==1,"legacy layout remains stable when saved again");
    return first;
   });
   vault[0]=chosen.localPosition(11,22,18);c.waitTicks(10);
   world.getServer().runOnServer(s->check(DungeonWards.warded(s.overworld(),vault[0]),"generated boss vault is warded"));
   // Walk all three flights using ordinary client movement in Survival.
   for(int flight=0;flight<3;flight++) {
    final int f=flight;world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getFoodData().setFoodLevel(20);
     var at=chosen.localPosition(f==1?7:4,f*7+1,f==1?11:3);var next=chosen.localPosition(f==1?7:4,f*7+1,f==1?10:4);
     float yaw=Direction.getNearest(next.getX()-at.getX(),0,next.getZ()-at.getZ(),null).toYRot();
     p.teleportTo(p.level(),at.getX()+.5,at.getY(),at.getZ()+.5,Set.<Relative>of(),yaw,0,false);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    });c.waitTicks(5);c.runOnClient(mc->mc.options.keyUp.setDown(true));c.waitTicks(85);c.runOnClient(mc->mc.options.keyUp.setDown(false));c.waitTicks(5);
    world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getY()>=chosen.localPosition(4,(f+1)*7+1,11).getY()-.2,"survival walking reaches floor "+(f+1)+"; actual "+p.position());});
   }
   world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);});
   c.runOnClient(mc->{if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   var b=chosen.getBoundingBox();look(c,world,new net.minecraft.world.phys.Vec3(b.minX()-23,132,b.minZ()-23),new net.minecraft.world.phys.Vec3(b.minX()+11,120,b.minZ()+11));shot(c,"storm_spire_day_overview");
   world.getServer().runCommand("time set 16000");c.waitTicks(10);shot(c,"storm_spire_night_overview");
   var stairs=chosen.localPosition(9,8,3);var center=chosen.localPosition(16,8,7);look(c,world,net.minecraft.world.phys.Vec3.atCenterOf(stairs),net.minecraft.world.phys.Vec3.atCenterOf(center));shot(c,"storm_spire_instrument_floor");
   world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setNoGravity(false);p.teleportTo(p.level(),.5,254,.5,Set.<Relative>of(),0,0,false);});
   c.runOnClient(mc->{if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   saveHolder[0]=world.getWorldSave();
  }
  try(var world=((net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave)saveHolder[0]).open()) {
   c.waitTicks(35);world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getUUID().equals(playerId[0]),"reload keeps attuned player");
    check(Attunement.restLeft(p,Attunements.byId("peaks").orElseThrow(),p.level().getGameTime())>0,"mountain reward rest survives full world restart");
    check(s.overworld().getBlockState(vault[0]).getValue(DungeonAltarBlock.KIND)==DungeonAltarBlock.Kind.STORM,"observatory altar persists");
    check(DungeonWards.warded(s.overworld(),vault[0]),"vault ward survives full restart");
   });
  }
  try(var natural=c.worldBuilder().setUseConsistentSettings(false).create()) {
   c.waitTicks(40);natural.getServer().runCommand("gamerule spawn_mobs false");natural.getServer().runCommand("time set 6000");
   BlockPos habitat=natural.getServer().computeOnServer(s->{var source=s.overworld().getChunkSource();var b=source.getGenerator().getBiomeSource().findBiomeHorizontal(1024,160,1024,6400,32,v->v.is(net.minecraft.world.level.biome.Biomes.STONY_PEAKS)||v.is(net.minecraft.world.level.biome.Biomes.JAGGED_PEAKS),RandomSource.create(912),true,source.randomState());check(b!=null,"normal terrain has mountain habitat");return b.getFirst();});
   BlockPos found=null;
   for(int i=0;i<8&&found==null;i++){
    int x=habitat.getX()+(i%4)*48,z=habitat.getZ()+(i/4)*48;
    natural.getServer().runOnServer(s->{for(int cx=(x>>4)-2;cx<=(x>>4)+2;cx++)for(int cz=(z>>4)-2;cz<=(z>>4)+2;cz++)s.overworld().getChunk(cx,cz);});
    natural.getServer().runCommand("place structure wildercord:storm_spire "+x+" 160 "+z);
    found=natural.getServer().computeOnServer(s->{for(int cx=(x>>4)-2;cx<=(x>>4)+2;cx++)for(int cz=(z>>4)-2;cz<=(z>>4)+2;cz++)for(var be:s.overworld().getChunk(cx,cz).getBlockEntities().values())if(be.getBlockState().getBlock() instanceof DungeonAltarBlock&&be.getBlockState().getValue(DungeonAltarBlock.KIND)==DungeonAltarBlock.Kind.STORM)return be.getBlockPos();return null;});
   }
   check(found!=null,"registered Storm Spire places complete altar on normal mountain terrain");var at=found;c.waitTicks(5);
   natural.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);check(DungeonWards.warded(s.overworld(),at),"command placed observatory retains vault ward");});
   c.runOnClient(mc->{if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});look(c,natural,new net.minecraft.world.phys.Vec3(at.getX()-36,at.getY()+16,at.getZ()-36),new net.minecraft.world.phys.Vec3(at.getX(),at.getY(),at.getZ()));
   natural.getConnection().waitForChunksRender();shot(c,"storm_spire_mountain_terrain");c.runOnClient(mc->{if(mc.gui.hud.isHidden())mc.gui.hud.toggle();});
  }

 }
 private static void shot(ClientGameTestContext c,String name){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.waitTicks(5);c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void look(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext w,net.minecraft.world.phys.Vec3 at,net.minecraft.world.phys.Vec3 target){
  var d=target.subtract(at);float yaw=(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));
  w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),at.x,at.y,at.z,Set.<Relative>of(),yaw,pitch,false);});c.waitTicks(15);
 }
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
