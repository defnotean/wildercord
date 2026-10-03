package dev.wildercord.wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** A bounded search for nearby cover. No loaded-chunk scan, forced chunk loads or terrain changes. */
public final class HighlandShelterGoal extends Goal {
	private final PathfinderMob mob;
	private final BooleanSupplier allowed, wantsCover;
	private final Consumer<Boolean> settled;
	private BlockPos cover;
	private int searchAt, left;
	private boolean running;
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
		if(covered(mob,mob.blockPosition())) {cover=mob.blockPosition();return true;}
		if(mob.tickCount<searchAt)return false;
		searchAt=mob.tickCount+100;cover=null;
		// At most sixteen probes every five seconds; only consider already loaded terrain.
		for(int i=0;i<16;i++) {
			var at=mob.blockPosition().offset(mob.getRandom().nextInt(13)-6,mob.getRandom().nextInt(3)-1,mob.getRandom().nextInt(13)-6);
			if(!covered(mob,at))continue;
			var path=mob.getNavigation().createPath(at,0);
			if(path!=null && path.canReach()) {cover=at;return true;}
		}
		return false;
	}
	@Override public boolean canContinueToUse() {
		return left>0 && allowed.getAsBoolean() && wantsCover.getAsBoolean() && cover!=null && covered(mob,cover);
	}
	@Override public void start() {running=true;left=200;mob.getNavigation().moveTo(cover.getX()+.5,cover.getY(),cover.getZ()+.5,.8);}
	@Override public void tick() {
		boolean arrived=mob.distanceToSqr(cover.getX()+.5,cover.getY(),cover.getZ()+.5)<1.5;
		if(!arrived)left--;
		if(arrived)mob.getNavigation().stop();settled.accept(arrived);
	}
	@Override public boolean requiresUpdateEveryTick() {return true;}
	@Override public void stop() {running=false;cover=null;mob.getNavigation().stop();settled.accept(false);}
}
