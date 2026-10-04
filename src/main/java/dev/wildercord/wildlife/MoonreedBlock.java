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
 public static boolean pollinate(ServerLevel l,BlockPos p,Vec3 moth) {return pollinate(l,p,moth,MoonreedAdmission.WORLD);}
 private static boolean bloomContact(ServerLevel l,BlockPos p,Vec3 moth,BlockState s,int age) {
  return l.hasChunkAt(p) && l.getWorldBorder().isWithinBounds(p) && s.is(WetlandGarden.REED) && s.getValue(AGE)==age
   && s.canSurvive(l,p) && canBloom(l,p,l.getOverworldClockTime()) && moth.distanceToSqr(Vec3.atCenterOf(p).add(0,.4,0))<=1;
 }
 /** Production source is the actual registered moth, observed across the synchronous writer. */
 public static boolean pollinate(ServerLevel l,BlockPos p,Glimmerwing moth){return pollinate(l,p,moth,MoonreedAdmission.WORLD);}
 private static boolean livingSource(ServerLevel l,Glimmerwing moth,Vec3 body){
  return moth!=null&&moth.isAlive()&&!moth.isRemoved()&&moth.level()==l&&l.hasChunkAt(moth.blockPosition())
   &&l.getEntity(moth.getUUID())==moth&&moth.position().equals(body);
 }
 static boolean pollinate(ServerLevel l,BlockPos p,Glimmerwing moth,MoonreedAdmission.Writer writer){
  if(moth==null)return false;var body=moth.position();
  return pollinateObserved(l,p,body,writer,()->livingSource(l,moth,body));
 }
 /** Position-only compatibility/fault seam: does not claim actual moth identity or source-liveness authority. */
 static boolean pollinate(ServerLevel l,BlockPos p,Vec3 moth,MoonreedAdmission.Writer writer) {
  return pollinateObserved(l,p,moth,writer,()->true);
 }
 private static boolean pollinateObserved(ServerLevel l,BlockPos p,Vec3 moth,MoonreedAdmission.Writer writer,java.util.function.BooleanSupplier source) {
  if(moth==null||!Double.isFinite(moth.lengthSqr())||!source.getAsBoolean()||!l.hasChunkAt(p))return false;var old=l.getBlockState(p);
  if(!bloomContact(l,p,moth,old,1))return false;
  try(var lease=MoonreedAdmission.open(l,p)) {
   if(lease==null||!source.getAsBoolean()||!l.getBlockState(p).equals(old))return false;var next=old.setValue(AGE,2);
   if(!writer.set(l,p,next) || !source.getAsBoolean() || !l.getBlockState(p).equals(next) || !bloomContact(l,p,moth,next,2) || !source.getAsBoolean())return false;
   l.sendParticles(ParticleTypes.GLOW,p.getX()+.5,p.getY()+.9,p.getZ()+.5,5,.15,.12,.15,0);
   Feels.sound(l,Vec3.atCenterOf(p),"wetland_reed_open",.35F,1);return true;
  }
 }
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult h) {return harvest(s,l,p,who);}
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult h) {return stack.is(WetlandGarden.LENS)?InteractionResult.PASS:s.getValue(AGE)==2?harvest(s,l,p,who):super.useItemOn(stack,s,l,p,who,hand,h);}
 private InteractionResult harvest(BlockState s,Level l,BlockPos p,Player who) {
  if(!s.is(this) || s.getValue(AGE)!=2)return InteractionResult.PASS;
  return l instanceof ServerLevel server?harvest(s,server,p,who,MoonreedAdmission.WORLD):InteractionResult.SUCCESS;
 }
 private static boolean harvestBody(ServerLevel l,BlockPos p,Player who) {
  if(!(who instanceof net.minecraft.server.level.ServerPlayer) || !who.isAlive() || who.isRemoved() || who.level()!=l
   || who.isSpectator() || !who.mayBuild() || !l.hasChunkAt(p) || !l.getWorldBorder().isWithinBounds(p)
   || !l.mayInteract(who,p) || l.getBlockEntity(p)!=null || dev.wildercord.cast.Effects.isTemporary(l,p)
   || who.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>who.blockInteractionRange()*who.blockInteractionRange())return false;
  var aim=new Vec3(p.getX()+.5,p.getY()+.15,p.getZ()+.5);
  var hit=l.clip(new ClipContext(who.getEyePosition(),aim,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,who));
  return hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(p);
 }
 private static boolean harvestClaim(ServerLevel l,BlockPos p,Player who) {
  return dev.wildercord.cast.Casters.probeBreak(l,who,p,l.getBlockState(p),l.getBlockEntity(p));
 }
 /** Manual harvesting asks existing build-claim callbacks without depending on magic block-edit configuration. */
 InteractionResult harvest(BlockState old,ServerLevel l,BlockPos p,Player who,MoonreedAdmission.Writer writer) {
  if(!old.is(this) || old.getValue(AGE)!=2 || !harvestBody(l,p,who) || !l.getBlockState(p).equals(old) || !old.canSurvive(l,p))return InteractionResult.PASS;
  try(var lease=MoonreedAdmission.open(l,p)) {
   if(lease==null || !harvestClaim(l,p,who) || !harvestBody(l,p,who) || !l.getBlockState(p).equals(old) || !old.canSurvive(l,p))return InteractionResult.PASS;
   var next=old.setValue(AGE,0);
   if(!writer.set(l,p,next) || !harvestBody(l,p,who) || !l.getBlockState(p).equals(next) || !next.canSurvive(l,p))return InteractionResult.PASS;
   if(!harvestClaim(l,p,who) || !harvestBody(l,p,who) || !l.getBlockState(p).equals(next) || !next.canSurvive(l,p))return InteractionResult.PASS;
   popResource(l,p,new ItemStack(WetlandGarden.FLOSS));Feels.sound(l,Vec3.atCenterOf(p),"wetland_reed_harvest",.55F,1);
   return InteractionResult.SUCCESS;
  }
 }
}
