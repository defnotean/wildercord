package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A bounded search for nearby cover. No loaded-chunk scan, forced chunk loads or terrain changes. */
public final class HighlandShelterGoal extends Goal {
	private static final int[] HEIGHTS={0,1,-1};
	private final PathfinderMob mob;
	private final BooleanSupplier allowed, wantsCover;
	private final Consumer<Boolean> settled;
	private BlockPos cover;
	private net.minecraft.world.level.pathfinder.Path route;
	private int searchAt, left,retryAt,retries;
	private boolean running;
	private final HabitatSweep sweep=new HabitatSweep(6);
	public HighlandShelterGoal(PathfinderMob mob,BooleanSupplier allowed,BooleanSupplier wantsCover,Consumer<Boolean> settled) {
		this.mob=mob;this.allowed=allowed;this.wantsCover=wantsCover;this.settled=settled;
		setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
	}
	public boolean running() {return running;}
	public static boolean covered(PathfinderMob mob,BlockPos at) {
		var level=mob.level();
		return level.hasChunkAt(at) && !level.canSeeSky(at) && level.getFluidState(at).isEmpty()
			&& level.getBlockState(at.below()).isSolidRender() && level.getBlockState(at).getCollisionShape(level,at).isEmpty()
			&& level.getBlockState(at.above()).getCollisionShape(level,at.above()).isEmpty();
	}
	@Override public boolean canUse() {
		if(!allowed.getAsBoolean() || !wantsCover.getAsBoolean())return false;
		// Recognize cover already over the body as soon as lighting settles; the expensive nearby search stays throttled.
		if(covered(mob,mob.blockPosition())) {cover=mob.blockPosition();route=null;return true;}
		if(!mob.onGround() || mob.tickCount<searchAt)return false;
		searchAt=mob.tickCount+20;cover=null;route=null;var here=mob.blockPosition();sweep.anchor(here.getX(),here.getY(),here.getZ());int paths=0;
		// Eight nearby columns per second, at most 24 loaded cells and two path attempts.
		for(int i=0;i<8;i++) {
			var offset=sweep.next();if(offset==null)break;
			for(int dy:HEIGHTS) {
				var at=new BlockPos(sweep.x()+offset.x(),sweep.y()+dy,sweep.z()+offset.z());
				if(!covered(mob,at))continue;var path=mob.getNavigation().createPath(at,0);paths++;
				if(path!=null && path.canReach()) {cover=at;route=path;return true;}
				if(paths==2)return false;
			}
		}
		return false;
	}
	@Override public boolean canContinueToUse() {
		return left>0 && allowed.getAsBoolean() && wantsCover.getAsBoolean() && cover!=null && covered(mob,cover);
	}
	@Override public void start() {running=true;left=200;retries=0;retryAt=mob.tickCount+20;if(route!=null)mob.getNavigation().moveTo(route,.8);}
	@Override public void tick() {
		boolean arrived=mob.distanceToSqr(cover.getX()+.5,cover.getY(),cover.getZ()+.5)<1.5 && covered(mob,mob.blockPosition());
		if(!arrived)left--;
		if(arrived)mob.getNavigation().stop();settled.accept(arrived);
		if(!arrived && mob.getNavigation().isDone() && mob.tickCount>=retryAt && retries<4) {
			retries++;retryAt=mob.tickCount+20;var path=mob.getNavigation().createPath(cover,0);
			if(path!=null && path.canReach())mob.getNavigation().moveTo(path,.8);
		}
	}
	@Override public boolean requiresUpdateEveryTick() {return true;}
	@Override public void stop() {running=false;cover=null;route=null;mob.getNavigation().stop();settled.accept(false);}
}
