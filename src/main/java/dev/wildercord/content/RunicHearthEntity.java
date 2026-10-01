package dev.wildercord.content;
import dev.wildercord.cast.*;
import dev.wildercord.player.*;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.util.*;

/** Owner-configured home projects and a small shared ritual. No tickets or offline production. */
public final class RunicHearthEntity extends BlockEntity {
 private String owner="",project="lantern";
 private int charge;
 private long readyAt,ritualFinish,ritualReadyAt;
 private final Set<String> ingredients=new LinkedHashSet<>(),contributors=new LinkedHashSet<>();
 public RunicHearthEntity(BlockPos p,BlockState s){super(WildercordBlocks.HEARTH_ENTITY,p,s);}
 public String project(){return project;}public int charge(){return charge;}
 private ServerPlayer owner(ServerLevel s){try{return s.getServer().getPlayerList().getPlayer(UUID.fromString(owner));}catch(IllegalArgumentException e){return null;}}
 private boolean allowed(ServerPlayer p){var o=owner(p.level());return owner.isEmpty()||owner.equals(p.getStringUUID())||o!=null&&Targets.canHelp(o,p);}
 public void use(Player player,ItemStack held) {
  if(!(player instanceof ServerPlayer p)||!(level instanceof ServerLevel s)||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))>36)return;
  if(held.is(Items.BOOK)){dev.wildercord.net.NotebookPayload.open(p);return;}
  if(owner.isEmpty()){owner=p.getStringUUID();setChanged();}
  if(!owner.equals(p.getStringUUID())){p.sendOverlayMessage(Component.translatable("message.wildercord.hearth_owner"));return;}
  String next=held.is(Items.AMETHYST_SHARD)?"lantern":held.is(Items.WHEAT)?"garden":held.is(Items.FEATHER)?"chime":held.is(Items.ENDER_PEARL)?"ritual":null;
  if(next!=null&&ritualFinish==0){project=next;charge=0;readyAt=0;ingredients.clear();contributors.clear();setChanged();sync();}
  else if(held.is(WildercordItems.MANA_CRYSTAL)&&project.equals("ritual")&&ritualFinish==0&&s.getGameTime()>=ritualReadyAt) {
   if(!p.isCreative())held.shrink(1);ritualFinish=s.getGameTime()+400;ingredients.clear();contributors.clear();charge=0;setChanged();sync();
   p.sendSystemMessage(Component.translatable("message.wildercord.hearth_ritual"));return;
  }
  p.sendSystemMessage(Component.translatable("message.wildercord.hearth_status",Component.translatable("project.wildercord."+project),charge));
 }
 public boolean onSpell(Cast cast,SpellPlan.EffectNode node) {
  if(!(cast.caster instanceof ServerPlayer p)||level!=cast.level||!allowed(p)||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))>24*24)return false;
  long now=cast.level.getGameTime();
  var elements=dev.wildercord.spell.VisualElements.of(List.of(node.effect));
  if(project.equals("ritual")) {
   if(ritualFinish==0)return false;
   for(String element:elements)if(!element.isEmpty()&&ingredients.size()<10)ingredients.add(element);if(contributors.size()<16)contributors.add(p.getStringUUID());charge=Math.min(3,ingredients.size());sync();setChanged();return true;
  }
  if(now<readyAt||charge>=3)return false;
  boolean matches=switch(project){case "garden"->elements.contains("life");case "chime"->elements.contains("wind");default->elements.contains("fire")||elements.contains("arcane");};
  if(!matches)return false;charge++;readyAt=now+20;sync();setChanged();return true;
 }
 private void sync(){if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(RunicHearthBlock.CHARGE,charge),Block.UPDATE_ALL);}
 public static void tick(Level l,BlockPos pos,BlockState state,RunicHearthEntity h) {
  if(!(l instanceof ServerLevel s)||s.getGameTime()%5!=0||!h.project.equals("chime")&&s.getGameTime()%20!=0)return;long now=s.getGameTime();var owner=h.owner(s);
  if(owner==null||owner.level()!=s)return;
  var nearby=s.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<64&&h.allowed(p)).toList();
  if(nearby.isEmpty())return;
  if(h.project.equals("ritual")&&h.ritualFinish>0) {
   int active=(int)nearby.stream().filter(p->h.contributors.contains(p.getStringUUID())).count();
   dev.wildercord.cast.Fx.sendParticles(s, ParticleTypes.ENCHANT,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,8,.8,.5,.8,.05);
   if(h.charge>=3&&now>=h.ritualFinish-(active>=2?200:0)) {
    for(var p:nearby)if(h.contributors.contains(p.getStringUUID())){p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,0));p.sendSystemMessage(Component.translatable("message.wildercord.hearth_complete"));}
    ItemStack reward=new ItemStack(WildercordItems.TORN_PAGE);owner.getInventory().add(reward);if(!reward.isEmpty())owner.drop(reward,false,net.minecraft.util.Prediction.SERVER_ONLY);
    h.ritualFinish=0;h.charge=0;h.ritualReadyAt=now+1200;h.ingredients.clear();h.contributors.clear();h.sync();h.setChanged();
   }return;
  }
  if(h.charge==0||now<h.readyAt)return;
  boolean used=false;
  switch(h.project) {
   case "garden" -> {
    // Only a small managed garden: a ripe crop is never harvested and no terrain is replaced.
    for(var p:BlockPos.betweenClosed(pos.offset(-2,0,-2),pos.offset(2,1,2))) {
     if(!s.hasChunkAt(p))continue;var crop=s.getBlockState(p);
     if(crop.getBlock() instanceof CropBlock c&&!c.isMaxAge(crop)){s.setBlockAndUpdate(p,c.getStateForAge(c.getAge(crop)+1));used=true;break;}
    }
   }
   case "chime" -> {for(var p:nearby)if(p.fallDistance>3&&!p.hasEffect(MobEffects.SLOW_FALLING)){p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100,0));used=true;}}
   case "lantern" -> {for(var p:nearby)if(!p.hasEffect(MobEffects.NIGHT_VISION)){p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200,0,false,false));used=true;}}
   default -> {}
  }
  if(used){h.charge--;h.readyAt=now+100;h.sync();h.setChanged();dev.wildercord.cast.Fx.sendParticles(s, ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,5,.3,.2,.3,0);}
 }
 protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putString("owner",owner);out.putString("project",project);out.putInt("charge",charge);out.putLong("ready_at",readyAt);out.putLong("ritual_finish",ritualFinish);out.putLong("ritual_ready_at",ritualReadyAt);out.store("ingredients",com.mojang.serialization.Codec.STRING.listOf(0,10),List.copyOf(ingredients));out.store("contributors",com.mojang.serialization.Codec.STRING.listOf(0,16),List.copyOf(contributors));}
 protected void loadAdditional(ValueInput in){super.loadAdditional(in);owner=in.getStringOr("owner","");project=in.getStringOr("project","lantern");if(!List.of("lantern","garden","chime","ritual").contains(project))project="lantern";charge=Math.clamp(in.getIntOr("charge",0),0,3);readyAt=in.getLongOr("ready_at",0);ritualFinish=in.getLongOr("ritual_finish",0);ritualReadyAt=in.getLongOr("ritual_ready_at",0);ingredients.clear();ingredients.addAll(in.read("ingredients",com.mojang.serialization.Codec.STRING.listOf(0,10)).orElse(List.of()));contributors.clear();contributors.addAll(in.read("contributors",com.mojang.serialization.Codec.STRING.listOf(0,16)).orElse(List.of()));}
}
