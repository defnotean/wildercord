package dev.wildercord.familiar;
import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.core.particles.ParticleTypes;

/** A chosen job replaces the familiar's elemental assistance. It never adds an extra combat spell. */
public final class FamiliarRoles {
 private FamiliarRoles() {}
 public enum Role { COMPANION, SCOUT, GUARDIAN, GARDENER }
 public static final AttachmentType<String> ROLE=AttachmentRegistry.create(Wildercord.id("familiar_role"),b->b.initializer(()->"companion").persistent(Codec.STRING).copyOnDeath());
 public static Role get(ServerPlayer p){try{return Role.valueOf(p.getAttachedOrElse(ROLE,"companion").toUpperCase(java.util.Locale.ROOT));}catch(IllegalArgumentException e){return Role.COMPANION;}}
 public static int choose(ServerPlayer p,String role){Role r;try{r=Role.valueOf(role.toUpperCase(java.util.Locale.ROOT));}catch(IllegalArgumentException e){return 0;}
  p.setAttached(ROLE,r.name().toLowerCase(java.util.Locale.ROOT));p.sendSystemMessage(Component.translatable("message.wildercord.familiar_role",Component.translatable("role.wildercord."+role)));return 1;}
 /** Called at the normal assistance interval; bounded searches only visit loaded blocks. */
 static boolean help(ServerPlayer p,Wisp wisp) {
  switch(get(p)) {
   case GUARDIAN -> {
    var threat=p.getLastHurtByMob();if(threat==null||!threat.isAlive()||p.tickCount-p.getLastHurtByMobTimestamp()>100||p.hasEffect(MobEffects.ABSORPTION))return false;
    p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,0,false,true));
    dev.wildercord.cast.ElementFx.earthImpact(p.level(),p.position(),.6);return true;
   }
   case SCOUT -> {
    var enemies=p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(16),e->e instanceof net.minecraft.world.entity.monster.Enemy&&e.isAlive()&&!e.hasEffect(MobEffects.GLOWING));
    var nearest=enemies.stream().min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(p)));
    if(nearest.isEmpty())return false;nearest.get().addEffect(new MobEffectInstance(MobEffects.GLOWING,100,0,false,false),p);
    dev.wildercord.cast.Light.ray(p.level(),wisp.position(),nearest.get().getEyePosition(),0xEED499,.025F,8);return true;
   }
   case GARDENER -> {
    // Shows ripe crops without harvesting or changing a player's farm. The role spends its turn on utility.
    for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-1,-4),p.blockPosition().offset(4,1,4))) {
     if(!p.level().hasChunkAt(pos))continue;var state=p.level().getBlockState(pos);
     if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state)) {
      dev.wildercord.cast.Fx.sendParticles(p.level(), ParticleTypes.HAPPY_VILLAGER,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,5,.3,.2,.3,0);
      p.sendOverlayMessage(Component.translatable("message.wildercord.familiar_harvest"));return true;
     }
    }
    return false;
   }
   default -> {return false;}
  }
 }
}
