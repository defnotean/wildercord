package dev.wildercord.wildlife;

import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
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

/** A perennial bank flower. Life prepares a bud; a real moth visit opens it. No spreading. */
public final class MoonreedBlock extends Block {
 public static final net.minecraft.tags.TagKey<Block> GROUND=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,dev.wildercord.Wildercord.id("moonreed_ground"));
 public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
 public MoonreedBlock(Properties p) {super(p);registerDefaultState(stateDefinition.any().setValue(AGE,0));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(AGE);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return Block.box(3,0,3,13,8+s.getValue(AGE)*4,13);}
 @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p) {return l.getFluidState(p).isEmpty() && l.getBlockState(p.below()).is(GROUND);}
 public static boolean moist(LevelReader l,BlockPos p) {
  for(var d:Direction.Plane.HORIZONTAL)if(l.getFluidState(p.relative(d)).is(FluidTags.WATER) || l.getFluidState(p.below().relative(d)).is(FluidTags.WATER) || l.getFluidState(p.below(2).relative(d)).is(FluidTags.WATER))return true;
  return l instanceof Level level && level.isRainingAt(p);
 }
 public static int surfaceHeight(LevelReader l,int x,int z) {return l.getHeight(l instanceof net.minecraft.server.level.WorldGenRegion?net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG:net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);}
 // Features run before skylight propagation. Surface height also makes a placed roof effective immediately.
 public static boolean openSky(LevelReader l,BlockPos p) {return surfaceHeight(l,p.getX(),p.getZ())<=p.getY()+1;}
 public static boolean canBloom(LevelReader l,BlockPos p,long time) {return WetlandRules.night(time) && moist(l,p) && openSky(l,p);}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r) {return canSurvive(s,l,p)?super.updateShape(s,l,t,p,d,np,ns,r):Blocks.AIR.defaultBlockState();}
 @Override protected boolean isRandomlyTicking(BlockState s) {return s.getValue(AGE)==0;}
 @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r) {if(s.getValue(AGE)==0 && canBloom(l,p,l.getOverworldClockTime()) && r.nextInt(8)==0)l.setBlock(p,s.setValue(AGE,1),Block.UPDATE_CLIENTS);}
 public static boolean pollinate(ServerLevel l,BlockPos p,Vec3 moth) {
  if(!l.hasChunkAt(p))return false;var s=l.getBlockState(p);
  if(!s.is(WetlandGarden.REED) || s.getValue(AGE)!=1 || !canBloom(l,p,l.getOverworldClockTime()) || moth.distanceToSqr(Vec3.atCenterOf(p).add(0,.4,0))>1)return false;
  l.setBlock(p,s.setValue(AGE,2),Block.UPDATE_CLIENTS);l.sendParticles(ParticleTypes.GLOW,p.getX()+.5,p.getY()+.9,p.getZ()+.5,5,.15,.12,.15,0);Feels.sound(l,Vec3.atCenterOf(p),"wetland_reed_open",.35F,1);return true;
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult h) {return harvest(s,l,p,who);}
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult h) {return stack.is(WetlandGarden.LENS)?InteractionResult.PASS:s.getValue(AGE)==2?harvest(s,l,p,who):super.useItemOn(stack,s,l,p,who,hand,h);}
 private InteractionResult harvest(BlockState s,Level l,BlockPos p,Player who) {
  if(s.getValue(AGE)!=2 || !who.mayBuild() || !l.mayInteract(who,p))return InteractionResult.PASS;
  if(l instanceof ServerLevel server) {popResource(l,p,new ItemStack(WetlandGarden.FLOSS));l.setBlock(p,s.setValue(AGE,0),Block.UPDATE_CLIENTS);Feels.sound(server,Vec3.atCenterOf(p),"wetland_reed_harvest",.55F,1);}
  return InteractionResult.SUCCESS;
 }
}
