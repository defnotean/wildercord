package dev.wildercord.aura.world;

import dev.wildercord.wildlife.Wildlife;
import dev.wildercord.wildlife.Rimehare;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A feathered ridge scavenger: seeks fresh casting, crouches over a fixed landing, then springs. */
public final class Galeclaw extends AuraBeast {
	private long fedUntil;
	public Galeclaw(EntityType<? extends Galeclaw> type,Level level) { super(type,level); }
	@Override public boolean gale() { return true; }
	public boolean hungry() {return dev.wildercord.wildlife.HighlandRules.hungry(level().getGameTime(),fedUntil);}
	public long fedUntil() {return fedUntil;}
	public void ate() {fedUntil=dev.wildercord.wildlife.HighlandRules.meal(level().getGameTime());}
	@Override protected void hit(ServerLevel level,LivingEntity target,float damage,double knock) {
		super.hit(level,target,damage,knock);
		if(target instanceof Rimehare && !target.isAlive()) {ate();setTarget(null);}
	}
	@Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {super.addAdditionalSaveData(out);out.putLong("fed_until",fedUntil);}
	@Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {super.readAdditionalSaveData(in);fedUntil=in.getLongOr("fed_until",0);}
	@Override protected void act(ServerLevel level) {
		if(left>0) {
			getNavigation().stop(); if(pose()==BeastRules.WARN || pose()==BeastRules.LEAP) lockFacing();
			if(pose()==BeastRules.LEAP) {
				if(horizontalCollision) { setDeltaMovement(Vec3.ZERO); pose(BeastRules.RECOVER,BeastRules.RECOVERY); dust(position(),10); sound("land"); return; }
				var step=landing.subtract(position()).multiply(1,0,1).scale(1.0/left);
				setDeltaMovement(step.add(0,.42-(16-left)*.06,0));
			}
			if(--left==0) {
				if(pose()==BeastRules.WARN) {
					beginAttack(); pose(BeastRules.LEAP,16); setDeltaMovement(landing.subtract(position()).multiply(1,0,1).scale(1/16.0).add(0,.42,0)); sound("leap");
				} else if(pose()==BeastRules.LEAP) {
					for(var p:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(3),e->e!=this && valid(e) && !(e instanceof Galeclaw)))
						if(BeastRules.leapHit(p.position().subtract(position()).multiply(1,0,1).lengthSqr(),p.getY()-getY()) && hasLineOfSight(p)) hit(level,p,5,.35);
					dust(position(),14); setDeltaMovement(Vec3.ZERO); pose(BeastRules.RECOVER,BeastRules.RECOVERY); sound("land");
				} else pose(BeastRules.IDLE,0);
			}
			return;
		}
		if(getTarget()==null && calm==0 && tickCount%20==0) {
			var caster=level.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(18),p->valid(p) && !dev.wildercord.wildlife.HighlandContent.quiet(p) && p.distanceToSqr(Vec3.atBottomCenterOf(home))<32*32 && distanceToSqr(p)<18*18 && hasLineOfSight(p) && Wildlife.castRecently(p,160)).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			if(caster!=null) setTarget(caster);
			else if(hungry() && tickCount%100==0) { var prey=level.getEntitiesOfClass(Rimehare.class,getBoundingBox().inflate(10),p->p.isAlive() && p.distanceToSqr(Vec3.atBottomCenterOf(home))<32*32 && hasLineOfSight(p)); if(!prey.isEmpty()) setTarget(prey.getFirst()); }
		}
		var target=getTarget(); if(target==null) {
			if(hungry() && tickCount%40==0) {
				var scraps=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,getBoundingBox().inflate(6),e->e.isAlive() && (e.getItem().is(net.minecraft.world.item.Items.RABBIT) || e.getItem().is(net.minecraft.world.item.Items.CHICKEN)));
				if(!scraps.isEmpty()) {
					var food=scraps.getFirst(); getNavigation().moveTo(food,1);
					if(distanceToSqr(food)<2) { food.getItem().shrink(1); if(food.getItem().isEmpty()) food.discard(); ate();heal(1);pose(BeastRules.FORAGE,40); }
				}
			}
			return;
		}
		if(distanceTo(target)>8) { getNavigation().moveTo(target,1); return; }
		face(target.position().subtract(position())); landing=target.position(); pose(BeastRules.WARN,BeastRules.WARNING); sound("warn");
		// A fixed landing marker of stone dust, placed when the crouch begins. It never follows an evading player.
		for(int i=0;i<8;i++) { double a=i*Math.PI/4; dust(landing.add(Math.cos(a)*2.25,.12,Math.sin(a)*2.25),2); }
	}
}
