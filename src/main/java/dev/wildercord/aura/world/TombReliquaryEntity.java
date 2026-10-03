package dev.wildercord.aura.world;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.feel.Feels;
import dev.wildercord.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** One keeper per authentic site; saved outcome and claim ledger prevent repeat loot. */
public final class TombReliquaryEntity extends BlockEntity {
	private boolean authentic,cleared;
	private UUID guardian;
	private int missingTicks;
	private final Set<UUID> participants=new LinkedHashSet<>(),claimed=new LinkedHashSet<>();
	public TombReliquaryEntity(BlockPos p,BlockState s){super(SwordTombs.RELIQUARY_ENTITY,p,s);}
	public void awaken(){authentic=true;setChanged();}
	public boolean authentic(){return authentic;}
	public boolean cleared(){return cleared;}
	public UUID guardian(){return guardian;}
	public BlockPos arena(){return worldPosition.relative(getBlockState().getValue(TombReliquary.FACING).getOpposite(),8);}
	public void use(ServerPlayer p) {
		if(!(level instanceof ServerLevel server) || p.isSpectator() || !p.isAlive() || p.distanceToSqr(Vec3.atCenterOf(worldPosition))>25 || !Config.get().auraWorld().swordTombs())return;
		if(!authentic){p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.empty"));return;}
		if(cleared){claim(p);return;}
		if(guardian!=null){p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.active"));return;}
		if(!SwordTombRules.gate(Aura.stage(p),AuraRules.EDGE,Aura.enabled(p),Aura.holdsWeapon(p))){
			p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.challenge"));return;
		}
		if(server.getDifficulty()==Difficulty.PEACEFUL){p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.peaceful"));return;}
		var home=arena();
		if(!server.hasChunkAt(home) || !server.getBlockState(home).isAir() || !server.getBlockState(home.above()).isAir() || !server.getBlockState(home.above(2)).isAir()){
			p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.obstructed"));return;
		}
		var keeper=SwordTombs.KEEPER.create(server,EntitySpawnReason.TRIGGERED);if(keeper==null)return;
		keeper.bind(worldPosition,home);keeper.snapTo(home.getX()+.5,home.getY(),home.getZ()+.5,0,0);
		if(!server.addFreshEntity(keeper))return;
		guardian=keeper.getUUID();missingTicks=0;participants.clear();state(1);setChanged();
		Feels.sound(server,Vec3.atCenterOf(worldPosition),"aura_tomb_awake",1,1);
		p.sendSystemMessage(Component.translatable("message.wildercord.tomb.wakes"));
	}
	public void participant(ServerPlayer p){if(participants.size()<64 && p.level()==level && p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(arena()))<24*24 && participants.add(p.getUUID()))setChanged();}
	public void defeated(UUID id){if(!authentic || cleared || !Objects.equals(guardian,id))return;cleared=true;guardian=null;state(2);setChanged();}
	private void claim(ServerPlayer p) {
		if(!participants.contains(p.getUUID()) || !claimed.add(p.getUUID())){p.sendOverlayMessage(Component.translatable("message.wildercord.tomb.spent"));return;}
		setChanged();
		// Two different scrolls; the favoured tomb source already excludes non-scrollable parts.
		var random=new Random(worldPosition.asLong()^p.getUUID().getLeastSignificantBits());
		var first=AuraApi.drawScrollPart("sword_tomb",random).orElse("echo");
		var second=AuraApi.drawScrollPart("sword_tomb",random).orElse("sunder");
		if(second.equals(first))second=first.equals("echo")?"sunder":"echo";
		give(p,AuraApi.techniqueScroll(first));give(p,AuraApi.techniqueScroll(second));
		give(p,new ItemStack(AuraWorld.AURA_SHARD,3));give(p,SwordTombs.history());
		dev.wildercord.cast.Grimoire.unlock(p,"aura:sword_tomb");
		Feels.sound(p.level(),p.position(),"aura_tomb_reward",.8F,1);
		p.sendSystemMessage(Component.translatable("message.wildercord.tomb.reward"));
	}
	private static void give(ServerPlayer p,ItemStack s){if(!p.getInventory().add(s))p.drop(s,false,net.minecraft.util.Prediction.SERVER_ONLY);}
	private void state(int value){if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(TombReliquary.STATE,value),3);}
	public static void tick(Level level,BlockPos pos,BlockState state,TombReliquaryEntity self){
		if(!(level instanceof ServerLevel server) || self.guardian==null || server.getGameTime()%20!=0)return;
		if(server.getEntity(self.guardian)!=null){self.missingTicks=0;return;}
		// Wait for the entire leashed arena to load. Never load a chunk or respawn a dormant entity remotely.
		var center=self.arena();for(int dx:new int[]{-16,16})for(int dz:new int[]{-16,16})if(!server.hasChunkAt(center.offset(dx,0,dz)))return;
		if(server.players().stream().noneMatch(p->!p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(center))<32*32))return;
		self.missingTicks+=20;if(self.missingTicks<200)return;
		self.guardian=null;self.participants.clear();self.missingTicks=0;self.state(0);self.setChanged();
	}
	@Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putBoolean("authentic",authentic);out.putBoolean("cleared",cleared);if(guardian!=null)out.store("guardian",UUIDUtil.CODEC,guardian);out.store("participants",UUIDUtil.CODEC.listOf(),List.copyOf(participants));out.store("claimed",UUIDUtil.CODEC.listOf(),List.copyOf(claimed));}
	@Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);authentic=in.getBooleanOr("authentic",false);cleared=in.getBooleanOr("cleared",false);guardian=cleared?null:in.read("guardian",UUIDUtil.CODEC).orElse(null);participants.clear();claimed.clear();in.read("participants",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(64).forEach(participants::add);in.read("claimed",UUIDUtil.CODEC.listOf()).orElse(List.of()).stream().limit(64).forEach(claimed::add);missingTicks=0;}
}
