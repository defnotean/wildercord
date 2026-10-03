package dev.wildercord.aura.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A moss-coated highland grazer: warning stamp, straight charge, then a long exposed recovery. */
public final class Stonehorn extends AuraBeast {
	public Stonehorn(EntityType<? extends Stonehorn> type,Level level) { super(type,level); }
	@Override public boolean gale() { return false; }
	@Override protected void act(ServerLevel level) {
		if(left>0) {
			getNavigation().stop();
			if(pose()==BeastRules.WARN || pose()==BeastRules.CHARGE) lockFacing();
			if(pose()==BeastRules.CHARGE) {
				if(horizontalCollision) { dust(position(),16); pose(BeastRules.RECOVER,BeastRules.RECOVERY+20); sound("impact"); return; }
				setDeltaMovement(direction.scale(.55).add(0,getDeltaMovement().y,0));
				var side=new Vec3(-direction.z,0,direction.x);
				for(var p:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(2.4),e->e!=this && valid(e) && !(e instanceof Stonehorn))) {
					var d=p.position().subtract(position()); if(BeastRules.chargeHit(d.dot(direction),d.dot(side),d.y) && hasLineOfSight(p)) hit(level,p,7,.7);
				}
				if(left%4==0) dust(position(),3);
			}
			if(--left==0) {
				if(pose()==BeastRules.WARN) { beginAttack(); pose(BeastRules.CHARGE,18); sound("charge"); }
				else if(pose()==BeastRules.CHARGE) { setDeltaMovement(Vec3.ZERO); pose(BeastRules.RECOVER,BeastRules.RECOVERY); }
				else pose(BeastRules.IDLE,0);
			}
			return;
		}
		if(getTarget()==null && calm==0 && tickCount%10==0) {
			var p=level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,getBoundingBox().inflate(8),e->valid(e) && e.distanceToSqr(Vec3.atBottomCenterOf(home))<32*32 && hasLineOfSight(e)
				&& BeastRules.wary(distanceTo(e),e.isShiftKeyDown(),e.getMainHandItem().is(Items.WHEAT) || e.getOffhandItem().is(Items.WHEAT))).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			if(p!=null) setTarget(p);
		}
		var target=getTarget();
		if(target!=null) {
			if(distanceTo(target)>7) { getNavigation().moveTo(target,1); return; }
			face(target.position().subtract(position())); pose(BeastRules.WARN,BeastRules.WARNING); sound("warn"); dust(position(),8); return;
		}
		// Grazing changes its pose and sheds no automatic items or terrain. Wheat provides the deliberate reward.
		if(tickCount%100==0 && level.getBlockState(blockPosition().below()).is(Blocks.GRASS_BLOCK)) { pose(BeastRules.FORAGE,50); sound("forage"); }
	}
}
