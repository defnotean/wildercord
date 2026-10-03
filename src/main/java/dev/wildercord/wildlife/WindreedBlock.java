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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** A perennial ridge grass. Harvest the tassels; retain the root. No spreading simulation. */
public final class WindreedBlock extends Block {
	public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
	public static final net.minecraft.tags.TagKey<Block> GROUND=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,dev.wildercord.Wildercord.id("windreed_ground"));
	public WindreedBlock(Properties props) {super(props);registerDefaultState(stateDefinition.any().setValue(AGE,0));}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {b.add(AGE);}
	@Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {return Block.box(3,0,3,13,6+s.getValue(AGE)*5,13);}
	@Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p) {return l.getBlockState(p.below()).is(GROUND) && l.getFluidState(p).isEmpty();}
	@Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess ticks,BlockPos p,Direction d,BlockPos np,BlockState ns,RandomSource r) {
		return !canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,l,ticks,p,d,np,ns,r);
	}
	@Override protected boolean isRandomlyTicking(BlockState s) {return s.getValue(AGE)<2;}
	@Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r) {
		if(s.getValue(AGE)<2 && l.getMaxLocalRawBrightness(p)>=9 && l.canSeeSky(p) && r.nextInt(12)==0)l.setBlock(p,s.setValue(AGE,s.getValue(AGE)+1),Block.UPDATE_CLIENTS);
	}
	@Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player who,BlockHitResult hit) {return harvest(s,l,p,who);}
	@Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player who,InteractionHand hand,BlockHitResult hit) {
		return s.getValue(AGE)==2?harvest(s,l,p,who):super.useItemOn(stack,s,l,p,who,hand,hit);
	}
	private InteractionResult harvest(BlockState s,Level l,BlockPos p,Player who) {
		if(s.getValue(AGE)!=2 || !who.mayBuild() || !l.mayInteract(who,p))return InteractionResult.PASS;
		if(l instanceof ServerLevel server) {
			popResource(l,p,new ItemStack(HighlandContent.WINDREED,2));
			l.setBlock(p,s.setValue(AGE,0),Block.UPDATE_CLIENTS);
			dev.wildercord.cast.feel.Feels.sound(server,net.minecraft.world.phys.Vec3.atCenterOf(p),"highland_reed_harvest",.65F,1);
		}
		return InteractionResult.SUCCESS;
	}
}
