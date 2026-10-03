package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Perennial damp-cave cap; living snail visits mature a prepared bud. */
public final class GlowcapBlock extends Block {
 public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
 public GlowcapBlock(Properties p) {super(p);registerDefaultState(stateDefinition.any().setValue(AGE,0));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(AGE);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return Block.box(3,0,3,13,5+s.getValue(AGE)*3,13);}
 public static boolean moist(LevelReader l,BlockPos p) {
  for(var d:Direction.Plane.HORIZONTAL) {var at=p.below().relative(d);if(l.hasChunkAt(at) && l.getFluidState(at).is(FluidTags.WATER))return true;}
  return false;
 }
 public static boolean footing(LevelReader l,BlockPos p) {return l.hasChunkAt(p.below()) && (l.getBlockState(p.below()).is(Blocks.MOSS_BLOCK) || l.getBlockState(p.below()).is(Blocks.CLAY));}
 public static boolean structuralCover(LevelReader l,BlockPos p) {
  for(int y=1;y<=8;y++)if(l.hasChunkAt(p.above(y)) && (l.getBlockState(p.above(y)).isSolidRender() || l.getBlockState(p.above(y)).is(FungalGarden.NURSERY)))return true;
  return false;
 }
 public static boolean conditions(LevelReader l,BlockPos p) {return footing(l,p) && moist(l,p) && structuralCover(l,p) && (!(l instanceof Level level) || level.getMaxLocalRawBrightness(p)<=8);}
 @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p) {return footing(l,p) && l.getFluidState(p).isEmpty();}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r) {return canSurvive(s,l,p)?super.updateShape(s,l,t,p,d,np,ns,r):Blocks.AIR.defaultBlockState();}
 @Override protected boolean isRandomlyTicking(BlockState s) {return s.getValue(AGE)==0;}
 @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r) {if(s.getValue(AGE)==0 && conditions(l,p) && r.nextInt(8)==0)l.setBlock(p,s.setValue(AGE,1),Block.UPDATE_CLIENTS);}
 /** Call only after a native uninterrupted Sporeback browse; excludes remote visitation. */
 static boolean visit(ServerLevel l,BlockPos p,SporebackSnail snail) {
  if(!l.hasChunkAt(p) || snail.level()!=l || !snail.isAlive() || snail.pose()!=1 || snail.distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5)>=.7)return false;
  var s=l.getBlockState(p);if(!s.is(FungalGarden.GLOWCAP) || s.getValue(AGE)!=1 || !conditions(l,p))return false;
  l.setBlock(p,s.setValue(AGE,2),Block.UPDATE_CLIENTS);Feels.sound(l,Vec3.atCenterOf(p),"fungal_cap_open",.35F,1);return true;
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult h) {
  if(s.getValue(AGE)!=2 || who.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)>20.25 || who.isSpectator() || !who.isAlive() || !who.mayBuild() || !l.mayInteract(who,p))return InteractionResult.PASS;
  if(l instanceof ServerLevel server) {popResource(l,p,new ItemStack(FungalGarden.GILLS));l.setBlock(p,s.setValue(AGE,0),Block.UPDATE_CLIENTS);if(who instanceof net.minecraft.server.level.ServerPlayer player)FungalInvestigation.harvested(player);Feels.sound(server,Vec3.atCenterOf(p),"fungal_cap_harvest",.4F,1);}return InteractionResult.SUCCESS;
 }
}
