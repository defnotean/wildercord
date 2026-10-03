package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;
import java.util.EnumSet;
import java.util.function.*;

/** Bounded local grazing, preempted by panic, predators, courtship and shelter. No item/XP generation. */
public final class WindreedForageGoal extends Goal {
	private final PathfinderMob mob;
	private final BooleanSupplier allowed;
	private final Consumer<Boolean> feeding;
	private BlockPos food;
	private int searchAt,left,chew;
	private boolean running;
	public WindreedForageGoal(PathfinderMob mob,BooleanSupplier allowed,Consumer<Boolean> feeding) {
		this.mob=mob;this.allowed=allowed;this.feeding=feeding;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
	}
	public boolean running() {return running;}
	private boolean ripe(BlockPos at) {var l=mob.level();return l.hasChunkAt(at) && l.getBlockState(at).is(HighlandContent.REED) && l.getBlockState(at).getValue(WindreedBlock.AGE)==2;}
	@Override public boolean canUse() {
		if(!allowed.getAsBoolean() || mob.tickCount<searchAt)return false;
		searchAt=mob.tickCount+100;food=null;
		// Navigation may return no reachable path for the cell already occupied. Eat here without asking it to move.
		if(ripe(mob.blockPosition())) {food=mob.blockPosition();return true;}
		for(int i=0;i<24;i++) {
			var at=i==0?mob.blockPosition():mob.blockPosition().offset(mob.getRandom().nextInt(9)-4,mob.getRandom().nextInt(3)-1,mob.getRandom().nextInt(9)-4);
			if(!ripe(at))continue;var path=mob.getNavigation().createPath(at,0);
			if(path!=null && path.canReach()) {food=at;return true;}
		}
		return false;
	}
	@Override public void start() {running=true;left=160;chew=0;mob.getNavigation().moveTo(food.getX()+.5,food.getY(),food.getZ()+.5,.7);}
	@Override public boolean canContinueToUse() {return left>0 && chew<40 && allowed.getAsBoolean() && food!=null && ripe(food);}
	@Override public void tick() {
		left--;boolean near=mob.distanceToSqr(food.getX()+.5,food.getY(),food.getZ()+.5)<2.25;
		feeding.accept(near);
		if(near) {mob.getNavigation().stop();if(++chew==40) {
			if(mob.level() instanceof ServerLevel l && l.getGameRules().get(GameRules.MOB_GRIEFING) && ripe(food))l.setBlock(food,l.getBlockState(food).setValue(WindreedBlock.AGE,1),Block.UPDATE_CLIENTS);
			searchAt=mob.tickCount+1200;
		}}
	}
	@Override public boolean requiresUpdateEveryTick() {return true;}
	@Override public void stop() {running=false;food=null;mob.getNavigation().stop();feeding.accept(false);}
}
