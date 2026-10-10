package dev.wildercord.aura.world;

import dev.wildercord.aura.*;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import dev.wildercord.duel.DuelRules;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** A three-bout circuit, one challenger at a time. Only complete, clean knockout runs earn a saved choice of prize. */
public final class TournamentBoardEntity extends BlockEntity {
	private BlockPos bell;
	private long begins=-1,ends=-1,next=-1;
	private final UUID[] roster=new UUID[3];
	private final Set<UUID> winners=new LinkedHashSet<>(),pending=new LinkedHashSet<>();
	private final Map<UUID,String> choices=new LinkedHashMap<>();
	private UUID challenger,offered;
	private long offerUntil,betweenUntil;
	private int wins;
	private boolean dirty;
	public TournamentBoardEntity(BlockPos p,BlockState s){super(VillageTournaments.BOARD_ENTITY,p,s);}
	public void host(BlockPos at){bell=at.immutable();setChanged();}
	public boolean authentic(){return bell!=null;}
	public boolean open(){return level!=null && TournamentRules.open(level.getGameTime(),begins,ends);}
	public int wins(){return wins;}
	public UUID opponent(){return wins<3?roster[wins]:null;}
	public boolean pending(UUID p){return pending.contains(p);}
	public boolean hosts(UUID p){return Arrays.asList(roster).contains(p);}
	public BlockPos arena(){return worldPosition.offset(0,0,-6);}
	public BlockPos waiting(int slot){return arena().offset(-5+slot*5,0,-5);}
	public void describe(ServerPlayer p){say(p,"rules");}
	private void say(ServerPlayer p,String key,Object...args){p.sendSystemMessage(Component.translatable("message.wildercord.tournament."+key,args).withColor(0xDACAAB));}
	public void use(ServerPlayer p){
		if(!(level instanceof ServerLevel s) || p.isSpectator() || !p.isAlive() || p.distanceToSqr(Vec3.atCenterOf(worldPosition))>25)return;
		if(!Config.get().auraWorld().tournaments()){say(p,"disabled");return;}
		if(!authentic()){say(p,"empty");return;}
		if(pending.contains(p.getUUID())){prize(p);return;}
		long now=s.getGameTime();
		if(!open()){say(p,"closed");return;}
		if(winners.contains(p.getUUID())){say(p,"won_already");return;}
		if(winners.size()>=TournamentRules.LEDGER || pending.size()>=TournamentRules.LEDGER){say(p,"full");return;}
		if(challenger!=null && !challenger.equals(p.getUUID())){say(p,"busy");return;}
		if(challenger!=null){if(DuelistDuels.inDuel(p)){say(p,"fighting");return;}bout(p);return;}
		if(!Aura.enabled(p) || !Aura.holdsWeapon(p) || p.isCreative() || Spars.sparring(p) || dev.wildercord.duel.Duels.inDuel(p) || DuelistDuels.inDuel(p)){say(p,"eligible");return;}
		if(!p.getUUID().equals(offered) || now>offerUntil){offered=p.getUUID();offerUntil=now+200;describe(p);say(p,"accept");return;}
		offered=null; challenger=p.getUUID();wins=0;dirty=false;betweenUntil=now+600;setChanged();bout(p);
	}
	private boolean clearGround(ServerLevel s){
		BlockPos center=arena();
		for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){
			BlockPos p=center.offset(x,0,z);
			if(!s.hasChunkAt(p) || !s.getBlockState(p.below()).isFaceSturdy(s,p.below(),Direction.UP) || !s.getFluidState(p.below()).isEmpty())return false;
			for(int y=0;y<3;y++)if(!p.above(y).equals(worldPosition) && !s.getBlockState(p.above(y)).isAir())return false;
		}return true;
	}
	private void bout(ServerPlayer p){
		var s=(ServerLevel)level;
		if(!clearGround(s)){say(p,"obstructed");reset();return;}
		Duelist d=s.getEntity(roster[wins]) instanceof Duelist found?found:null;
		if(d==null || !d.isAlive() || !hosts(d.getUUID())){say(p,"interrupted");reset();return;}
		Vec3 center=Vec3.atBottomCenterOf(arena());
		// A fresh registration starts at the stand, six blocks from the centre; later rounds require returning to it.
		d.snapTo(center.x,center.y,center.z-3,0,0);d.getNavigation().stop();
		long event=begins;UUID id=p.getUUID();
		if(!DuelistDuels.startHosted(p,d,center,(player,won,magic,ending)->{
			if(isRemoved() || level!=s || s.getBlockEntity(worldPosition)!=this || begins!=event || !id.equals(challenger))return;
			if(!Config.get().auraWorld().tournaments() || !open()){reset();return;}
			d.leaveAt=-1;d.restUntil=s.getGameTime()+80;
			dirty|=magic;
			if(player==null || !won || ending!=DuelRules.Ending.KNOCKOUT || dirty){if(player!=null)say(player,dirty?"magic":"lost");reset();return;}
			if(TournamentRules.award(wins,won,dirty,true)){
				winners.add(id);pending.add(id);choices.put(id,defaultChoice(player));setChanged();
				dev.wildercord.cast.Grimoire.unlock(player,"aura:tournament");
				AuraExperience.earn(player,AuraWorldRules.duelXp(Aura.stage(player)),false);AuraApi.completeTrial(player,DuelistDuels.TRIAL);
				dev.wildercord.aura.BondedBlades.dueled(player,d.method().id());
			Feels.sound(s,center,"aura_tournament_victory",1,1);say(player,"victory");say(player,"choice",Component.translatable(TechniqueRules.nameKey(choices.get(id))));reset();
			}else{wins++;betweenUntil=s.getGameTime()+1200;setChanged();Feels.sound(s,center,"aura_tournament_round",.8F,1);say(player,"round",wins+1);}
		})) {say(p,"eligible");reset();return;}
		say(p,"bout",wins+1,Component.translatable(d.method().nameKey()));
	}
	private String defaultChoice(ServerPlayer p){return TechniqueRules.scrollParts().stream().filter(part->!Techniques.known(p).contains(part)).findFirst().orElse(TechniqueRules.scrollParts().getFirst());}
	private void prize(ServerPlayer p){
		List<String> parts=TechniqueRules.scrollParts();if(parts.isEmpty())return;
		String chosen=choices.getOrDefault(p.getUUID(),defaultChoice(p));if(!parts.contains(chosen))chosen=parts.getFirst();
		if(p.isShiftKeyDown()){
			chosen=parts.get(Math.floorMod(parts.indexOf(chosen)+1,parts.size()));choices.put(p.getUUID(),chosen);setChanged();
			say(p,"choice",Component.translatable(TechniqueRules.nameKey(chosen)));return;
		}
		pending.remove(p.getUUID());choices.remove(p.getUUID());setChanged();
		give(p,AuraApi.techniqueScroll(chosen));give(p,new ItemStack(AuraWorld.AURA_SHARD,3));give(p,VillageTournaments.history());
		Feels.sound(p.level(),p.position(),"aura_tournament_prize",.8F,1);say(p,"reward",Component.translatable(TechniqueRules.nameKey(chosen)));
	}
	private static void give(ServerPlayer p,ItemStack stack){if(!p.getInventory().add(stack))p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
	private void reset(){challenger=null;wins=0;dirty=false;betweenUntil=0;setChanged();}
	private void cancel(ServerLevel s){var p=challenger==null?null:s.getServer().getPlayerList().getPlayer(challenger);if(p!=null)DuelistDuels.cancelHosted(p);reset();}
	private void close(ServerLevel s){cancel(s);for(int i=0;i<3;i++){if(roster[i]!=null && s.getEntity(roster[i]) instanceof Duelist d)d.vanish(s);roster[i]=null;}setChanged();}
	private void openEvent(ServerLevel s){
		if(!clearGround(s) || !VillageTournaments.inhabited(s,bell))return;
		for(int i=0;i<3;i++)if(!s.hasChunkAt(waiting(i)))return;
		close(s);begins=s.getGameTime();ends=begins+TournamentRules.WINDOW;next=begins+TournamentRules.PERIOD;winners.clear();
		for(int i=0;i<3;i++){
			var d=AuraWorld.DUELIST.create(s,EntitySpawnReason.EVENT);if(d==null){close(s);ends=begins;return;}
			BlockPos at=waiting(i);d.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);d.setMethod(BreathingMethods.BUILT_IN.get(TournamentRules.method(worldPosition.asLong(),begins,i)));d.gather(worldPosition,i,ends);
			if(!s.addFreshEntity(d)){close(s);ends=begins;return;}roster[i]=d.getUUID();
		}
		Feels.sound(s,Vec3.atCenterOf(worldPosition),"aura_tournament_open",1,1);setChanged();
	}
	public static void tick(Level level,BlockPos pos,BlockState state,TournamentBoardEntity self){
		if(!(level instanceof ServerLevel s) || s.getGameTime()%20!=0 || !self.authentic())return;
		VillageTournaments.loaded(self);
		if(!Config.get().auraWorld().tournaments()){if(self.challenger!=null || self.roster[0]!=null){self.close(s);self.ends=Math.min(self.ends,s.getGameTime());self.setChanged();}return;}
		long now=s.getGameTime();
		if(self.open()){
			// Loaded provenance must agree. An unloaded or missing opponent interrupts, never advances the bracket.
			if(self.challenger!=null){var p=s.getServer().getPlayerList().getPlayer(self.challenger);if(p==null || p.level()!=s || !p.isAlive() || !DuelistDuels.inDuel(p) && (now>self.betweenUntil || p.distanceToSqr(Vec3.atCenterOf(self.arena()))>16*16))self.cancel(s);}
			return;
		}
		if(self.roster[0]!=null)self.close(s);
		if(now>=self.next && s.isBrightOutside() && s.players().stream().anyMatch(p->!p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(pos))<48*48))self.openEvent(s);
	}
	@Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(bell!=null)out.putLong("village_bell",bell.asLong());out.putLong("begins",begins);out.putLong("ends",ends);out.putLong("next",next);for(int i=0;i<3;i++)if(roster[i]!=null)out.store("duelist_"+i,UUIDUtil.CODEC,roster[i]);out.store("winners",UUIDUtil.CODEC.listOf(),List.copyOf(winners));out.store("pending",UUIDUtil.CODEC.listOf(),List.copyOf(pending));for(var id:pending)out.putString("choice_"+id,choices.getOrDefault(id,"echo"));}
	@Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);long b=in.getLongOr("village_bell",Long.MIN_VALUE);bell=b==Long.MIN_VALUE?null:BlockPos.of(b);begins=in.getLongOr("begins",-1);ends=in.getLongOr("ends",-1);next=in.getLongOr("next",-1);for(int i=0;i<3;i++)roster[i]=in.read("duelist_"+i,UUIDUtil.CODEC).orElse(null);winners.clear();pending.clear();choices.clear();in.read("winners",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(TournamentRules.LEDGER).forEach(winners::add);in.read("pending",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(TournamentRules.LEDGER).forEach(id->{pending.add(id);choices.put(id,in.getStringOr("choice_"+id,"echo"));});reset();}
}
