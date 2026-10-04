package dev.wildercord.wildlife;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
/** Cooling shade root, harvest retains root; no spread or world fire. */
public final class CinderFernBlock extends Block {
 public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
 public static final BooleanProperty COOLED=BooleanProperty.create("cooled");
 public CinderFernBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(AGE,0).setValue(COOLED,true));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AGE,COOLED);}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,5+s.getValue(AGE)*5,14);}
 static boolean footing(BlockState s){return s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.DIRT)||s.is(Blocks.COARSE_DIRT)||s.is(Blocks.ROOTED_DIRT)||s.is(Blocks.PODZOL);}
 @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return footing(l.getBlockState(p.below())) && l.getFluidState(p).isEmpty();}
 @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r){return !canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,l,t,p,d,np,ns,r);}
 @Override protected boolean isRandomlyTicking(BlockState s){return s.getValue(AGE)<2 || !s.getValue(COOLED);}
 @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(!s.canSurvive(l,p)||r.nextInt(16)!=0)return;var next=s;if(l.isRainingAt(p.above()) && !s.getValue(COOLED))next=s.setValue(COOLED,true);else if(s.getValue(COOLED)&&s.getValue(AGE)<2&&l.getMaxLocalRawBrightness(p)>=8)next=s.setValue(AGE,s.getValue(AGE)+1);if(!next.equals(s))l.setBlock(p,next,Block.UPDATE_CLIENTS);}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit){return harvest(s,l,p,who);}
 @Override protected InteractionResult useItemOn(ItemStack i,BlockState s,Level l,BlockPos p,Player who,InteractionHand h,BlockHitResult hit){return s.getValue(AGE)==2?harvest(s,l,p,who):super.useItemOn(i,s,l,p,who,h,hit);}
 private InteractionResult harvest(BlockState s,Level l,BlockPos p,Player who){
  if(!s.is(this)||s.getValue(AGE)!=2||!s.getValue(COOLED))return InteractionResult.PASS;
  return l instanceof ServerLevel server?harvest(s,server,p,who,EmberPlantAdmission.WORLD):InteractionResult.SUCCESS;
 }
 private static boolean admitted(ServerLevel l,BlockPos p,Player who){return who.isAlive()&&!who.isRemoved()&&who.level()==l&&!who.isSpectator()&&l.hasChunkAt(p)&&l.getWorldBorder().isWithinBounds(p)&&who.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p))<=36&&visible(l,p,who)&&who.mayBuild()&&l.mayInteract(who,p)&&!dev.wildercord.cast.Effects.isTemporary(l,p)&&l.getBlockEntity(p)==null;}
 private static boolean claim(ServerLevel l,BlockPos p,Player who){return net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(l,who,p,l.getBlockState(p),l.getBlockEntity(p));}
 // AGE0 is shorter than the cell center: aim inside the actual outline at every growth stage.
 private static boolean visible(ServerLevel l,BlockPos p,Player who){var eye=who.getEyePosition();var center=new net.minecraft.world.phys.Vec3(p.getX()+.5,p.getY()+.15,p.getZ()+.5);var hit=l.clip(new ClipContext(eye,center,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,who));return hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getBlockPos().equals(p);}
 /** Actual-world writer seam: false/stale outcomes never mint a frond, and nested same-cell harvest refuses. */
 InteractionResult harvest(BlockState old,ServerLevel l,BlockPos p,Player who,EmberPlantAdmission.Writer writer){
  if(!old.is(this)||old.getValue(AGE)!=2||!old.getValue(COOLED)||!admitted(l,p,who)||!l.getBlockState(p).equals(old)||!old.canSurvive(l,p))return InteractionResult.PASS;
  try(var lease=EmberPlantAdmission.open(l,p)){
   if(lease==null||!claim(l,p,who)||!admitted(l,p,who)||!l.getBlockState(p).equals(old)||!old.canSurvive(l,p))return InteractionResult.PASS;var next=old.setValue(AGE,0);
   if(!writer.set(l,p,next)||!admitted(l,p,who)||!l.getBlockState(p).equals(next)||!next.canSurvive(l,p))return InteractionResult.PASS;
   if(!claim(l,p,who)||!admitted(l,p,who)||!l.getBlockState(p).equals(next)||!next.canSurvive(l,p))return InteractionResult.PASS;
   popResource(l,p,new ItemStack(EmberContent.FERN_ITEM,1));dev.wildercord.cast.feel.Feels.sound(l,net.minecraft.world.phys.Vec3.atCenterOf(p),"ember_fern_pick",.45F,1);return InteractionResult.SUCCESS;
  }
 }
}
