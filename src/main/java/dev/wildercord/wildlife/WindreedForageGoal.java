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
	private static final int[] HEIGHTS={0,1,-1};
	private final PathfinderMob mob;
	private final BooleanSupplier allowed;
	private final Consumer<Boolean> feeding;
	private BlockPos food;
	private net.minecraft.world.level.pathfinder.Path route;
	private int searchAt,left,chew,retryAt,retries;
	private boolean running;
	private final HabitatSweep sweep=new HabitatSweep(4);
	public WindreedForageGoal(PathfinderMob mob,BooleanSupplier allowed,Consumer<Boolean> feeding) {
		this.mob=mob;this.allowed=allowed;this.feeding=feeding;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
	}
	public boolean running() {return running;}
	private boolean ripe(BlockPos at) {var l=mob.level();return l.hasChunkAt(at) && l.getBlockState(at).is(HighlandContent.REED) && l.getBlockState(at).getValue(WindreedBlock.AGE)==2;}
	@Override public boolean canUse() {
		if(!allowed.getAsBoolean() || !mob.onGround() || mob.tickCount<searchAt)return false;
		searchAt=mob.tickCount+20;food=null;route=null;
		// Navigation may return no reachable path for the cell already occupied. Eat here without asking it to move.
		if(ripe(mob.blockPosition())) {food=mob.blockPosition();return true;}
		var here=mob.blockPosition();sweep.anchor(here.getX(),here.getY(),here.getZ());int paths=0;
		// Eight columns per second, current footing first: at most 24 cells and two path attempts.
		for(int i=0;i<8;i++) {
			var offset=sweep.next();if(offset==null)break;
			for(int dy:HEIGHTS) {
				var at=new BlockPos(sweep.x()+offset.x(),sweep.y()+dy,sweep.z()+offset.z());
				if(!ripe(at))continue;var path=mob.getNavigation().createPath(at,0);paths++;
				if(path!=null && path.canReach()) {food=at;route=path;return true;}
				if(paths==2)return false;
			}
		}
		return false;
	}
	@Override public void start() {running=true;left=160;chew=0;retries=0;retryAt=mob.tickCount+20;feeding.accept(false);if(route!=null)mob.getNavigation().moveTo(route,.7);}
	private void approach() {var path=mob.getNavigation().createPath(food,0);if(path!=null && path.canReach())mob.getNavigation().moveTo(path,.7);}
	@Override public boolean canContinueToUse() {return left>0 && chew<40 && allowed.getAsBoolean() && food!=null && ripe(food);}
	@Override public void tick() {
		left--;boolean near=mob.distanceToSqr(food.getX()+.5,food.getY(),food.getZ()+.5)<2.25
			&& mob.level().clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(food),
				net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
		feeding.accept(near);
		if(!near) {
			chew=0;
			// A pose transition or collision can stop navigation. Retry finitely, never on every AI tick.
			if(mob.getNavigation().isDone() && mob.tickCount>=retryAt && retries<4) {retries++;retryAt=mob.tickCount+20;approach();}
		}
		if(near) {mob.getNavigation().stop();if(++chew==40) {
			if(mob.level() instanceof ServerLevel l && l.getGameRules().get(GameRules.MOB_GRIEFING) && ripe(food))l.setBlock(food,l.getBlockState(food).setValue(WindreedBlock.AGE,1),Block.UPDATE_CLIENTS);
			searchAt=mob.tickCount+1200;
		}}
	}
	@Override public boolean requiresUpdateEveryTick() {return true;}
	@Override public void stop() {running=false;food=null;route=null;mob.getNavigation().stop();feeding.accept(false);}
}
