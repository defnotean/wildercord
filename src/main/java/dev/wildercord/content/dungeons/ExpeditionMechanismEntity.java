package dev.wildercord.content.dungeons;

import dev.wildercord.cast.*;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import java.util.Set;

/** Bounded, chunk-local expedition puzzles: stored time, a cleanse/harvest choice, moving stepping stones. */
public final class ExpeditionMechanismEntity extends BlockEntity {
	private float stored;
	private int growth,skyStep;
	private boolean completed;
	private long pausedUntil,warningAt,nextAction;
	public ExpeditionMechanismEntity(BlockPos p,BlockState s){super(DungeonBlocks.MECHANISM_ENTITY,p,s);}
	private ExpeditionMechanism.Kind kind(){return ((ExpeditionMechanism)getBlockState().getBlock()).kind;}
	private Direction forward(){return getBlockState().getValue(ExpeditionMechanism.FACING);}
	private BlockPos ahead(int blocks){return worldPosition.relative(forward(),blocks);}
	public float stored(){return stored;}
	public boolean completed(){return completed;}
	public int skyStep(){return skyStep;}
	public void use(Player player,ItemStack held) {
		if(!(level instanceof ServerLevel server)||player.distanceToSqr(Vec3.atCenterOf(worldPosition))>36)return;
		long now=server.getGameTime();if(now<nextAction)return;
		switch(kind()) {
			case CLOCK -> {
				if(stored>0) {
					Vec3 center=Vec3.atCenterOf(ahead(5));float damage=stored;stored=0;
					Sigils.ground(server,center,0xFFD177,0xFFFFFF,4,24);
					// A preview beat comes first. The player chooses its direction by turning the block at generation.
					Scheduler.later(20,()->{
						if(level!=server||isRemoved()||!server.hasChunkAt(worldPosition)||!player.isAlive()||player.isRemoved()||player.level()!=server)return;
						for(var target:server.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(4),e->e instanceof Enemy && Targets.canHarm(player,e)))
							SpellDefence.hurt(server,target,server.damageSources().indirectMagic(player,player),damage);
						Light.groundRing(server,center,0xFFD177,.3,4,.06,18);
					});
				}
				pausedUntil=now+100;warningAt=0;nextAction=now+40;stage(stored>0?1:0);
				player.sendOverlayMessage(Component.translatable("message.wildercord.clock_paused"));
			}
			case GARDEN -> {
				if(completed)return;
				if(held.is(Items.SHEARS))finishGarden(server,player,true);
				else if(held.is(Items.BONE_MEAL)) {if(!player.isCreative())held.shrink(1);grow(server,player);}
				else player.sendOverlayMessage(Component.translatable("message.wildercord.garden_hint"));
			}
			case SKY -> {boolean moved=shiftPlatforms(server);nextAction=now+40;player.sendOverlayMessage(Component.translatable(moved?"message.wildercord.sky_shift":"message.wildercord.sky_blocked"));}
		}
		setChanged();
	}
	/** Returns true only when the clock actually absorbs a harmful effect aimed at its block. */
	public boolean onSpell(Cast cast,SpellPlan.EffectNode node) {
		if(!(cast.caster instanceof ServerPlayer player)||!(level instanceof ServerLevel server)||cast.level!=server)return false;
		if(kind()==ExpeditionMechanism.Kind.CLOCK && node.effect.kind()==EffectKind.HARMFUL) {
			stored=Math.min(12,stored+(float)Math.max(1,Math.min(4,node.effect.cost()/4)));pausedUntil=server.getGameTime()+100;warningAt=0;
			stage(Math.min(4,(int)Math.ceil(stored/3)));setChanged();
			player.sendOverlayMessage(Component.translatable("message.wildercord.clock_stored",String.format(java.util.Locale.ROOT,"%.1f",stored)));return true;
		}
		if(kind()==ExpeditionMechanism.Kind.CLOCK && node.effect.element().equals("time")){use(player,ItemStack.EMPTY);}
		if(kind()==ExpeditionMechanism.Kind.GARDEN && node.effect.element().equals("life"))grow(server,player);
		if(kind()==ExpeditionMechanism.Kind.SKY && node.effect.element().equals("wind") && server.getGameTime()>=nextAction){shiftPlatforms(server);nextAction=server.getGameTime()+40;}
		return false;
	}
	private void grow(ServerLevel server,Player player) {
		if(completed||server.getGameTime()<nextAction)return;
		growth++;nextAction=server.getGameTime()+20;stage(Math.min(4,growth));setChanged();
		Vfx.emit(server,ParticleTypes.HAPPY_VILLAGER,Vec3.atCenterOf(worldPosition),16,1,.02);
		if(growth>=3)finishGarden(server,player,false);
		else player.sendOverlayMessage(Component.translatable("message.wildercord.garden_growth",growth));
	}
	private void finishGarden(ServerLevel server,Player player,boolean harvest) {
		if(completed)return;completed=true;growth=4;stage(4);
		if(!harvest)for(int step=1;step<=9;step++) {
			BlockPos p=ahead(step).below();
			if(server.hasChunkAt(p) && (server.getBlockState(p).isAir()||server.getBlockState(p).is(Blocks.WATER)))server.setBlockAndUpdate(p,Blocks.MOSS_BLOCK.defaultBlockState());
		}
		give(player,new ItemStack(harvest?WildercordItems.MANA_CRYSTAL:Items.GLOW_BERRIES,harvest?2:8));
		give(player,new ItemStack(WildercordItems.TORN_PAGE));
		if(!harvest)player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,200,0));
		if(!harvest && player instanceof ServerPlayer serverPlayer)dev.wildercord.player.RuneResearch.garden(serverPlayer);
		player.sendOverlayMessage(Component.translatable(harvest?"message.wildercord.garden_harvest":"message.wildercord.garden_cleanse"));setChanged();
	}
	private void stage(int value){if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(ExpeditionMechanism.STAGE,value),Block.UPDATE_ALL);}
	private static void give(Player player,ItemStack stack){player.getInventory().add(stack);if(!stack.isEmpty())player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
	public boolean shiftPlatforms(ServerLevel server) {
		if(level!=server||kind()!=ExpeditionMechanism.Kind.SKY)return false;
		int old=skyStep,nextStep=(skyStep+1)%3;
		Direction side=forward().getClockWise();
		// Move the group atomically; an obstruction must not strand one stone at an obsolete offset.
		for(int distance:new int[]{3,6,9}) {
			BlockPos previous=ahead(distance).relative(side,old-1).below(),next=ahead(distance).relative(side,nextStep-1).below();
			if(!server.hasChunkAt(previous)||!server.hasChunkAt(next)||!server.getBlockState(previous).is(Blocks.IRON_BLOCK)||!server.getBlockState(next).isAir()
				||!server.getBlockState(next.above()).isAir()||!server.getBlockState(next.above(2)).isAir())return false;
		}
		skyStep=nextStep;
		for(int distance:new int[]{3,6,9}) {
			BlockPos previous=ahead(distance).relative(side,old-1).below(),next=ahead(distance).relative(side,skyStep-1).below();
			if(!server.hasChunkAt(previous)||!server.hasChunkAt(next))continue;
			if(!server.getBlockState(previous).is(Blocks.IRON_BLOCK)||!server.getBlockState(next).isAir())continue;
			// Carry anyone standing on this stone before transferring its collision block.
			for(ServerPlayer p:server.getEntitiesOfClass(ServerPlayer.class,new AABB(previous).move(0,1,0).inflate(.05,.1,.05))) {
				Vec3 delta=Vec3.atCenterOf(next).subtract(Vec3.atCenterOf(previous));
				p.teleportTo(server,p.getX()+delta.x,p.getY(),p.getZ()+delta.z,Set.<Relative>of(),p.getYRot(),p.getXRot(),false);
			}
			server.setBlockAndUpdate(previous,Blocks.AIR.defaultBlockState());server.setBlockAndUpdate(next,Blocks.IRON_BLOCK.defaultBlockState());
		}
		stage(skyStep);setChanged();
		return true;
	}
	public static void tick(Level level,BlockPos pos,BlockState state,ExpeditionMechanismEntity self) {
		if(!(level instanceof ServerLevel server))return;
		if(self.kind()!=ExpeditionMechanism.Kind.SKY&&server.getGameTime()%20!=0)return;
		long now=server.getGameTime();boolean nearby=server.players().stream().anyMatch(p->!p.isSpectator()&&p.distanceToSqr(Vec3.atCenterOf(pos))<24*24);
		if(!nearby)return;
		if(self.kind()==ExpeditionMechanism.Kind.CLOCK && now>=self.pausedUntil) {
			Vec3 center=Vec3.atCenterOf(self.ahead(5));
			if(self.warningAt==0){self.warningAt=now+40;Sigils.ground(server,center,0xFFCC66,0xFFFFFF,3,42);}
			else if(now>=self.warningAt){
				for(var p:server.players())if(!p.isCreative()&&!p.isSpectator()&&p.distanceToSqr(center)<9)SpellDefence.hurt(server,p,server.damageSources().magic(),4);
				self.warningAt=0;self.pausedUntil=now+100;
			}
		}
		if(self.kind()==ExpeditionMechanism.Kind.SKY) {
			if(now%20==0&&now>=self.nextAction){self.shiftPlatforms(server);self.nextAction=now+80;}
			// A one-second poll can miss the entire catch band during a fast fall.
			for(var p:server.players())if(!p.isSpectator()&&p.getY()<pos.getY()-3 && p.getY()>pos.getY()-12 && p.distanceToSqr(Vec3.atCenterOf(self.ahead(6)))<18*18){
				p.teleportTo(server,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,Set.<Relative>of(),p.getYRot(),p.getXRot(),false);
				p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.needsSync=true;
				p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
			}
		}
	}
	@Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putFloat("stored",stored);out.putInt("growth",growth);out.putInt("sky_step",skyStep);out.putBoolean("completed",completed);out.putLong("paused_until",pausedUntil);out.putLong("next_action",nextAction);}
	@Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);stored=Math.clamp(in.getFloatOr("stored",0),0,12);growth=Math.clamp(in.getIntOr("growth",0),0,4);skyStep=Math.floorMod(in.getIntOr("sky_step",0),3);completed=in.getBooleanOr("completed",false);pausedUntil=in.getLongOr("paused_until",0);nextAction=in.getLongOr("next_action",0);}
}
