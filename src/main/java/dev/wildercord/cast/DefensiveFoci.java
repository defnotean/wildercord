package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.GearDef;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Defensive sidegrades. Debt and recharge time persist across logout and equipment changes. */
public final class DefensiveFoci {
 private DefensiveFoci() {}
 public static final float DELAY_SHARE = .35F;
 public static final int RECHARGE = 160;
 public record State(float owed, float installment, int delay, int reprieve, int grounding) {
  static final State EMPTY = new State(0,0,0,0,0);
  static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
   Codec.FLOAT.fieldOf("owed").forGetter(State::owed), Codec.FLOAT.fieldOf("installment").forGetter(State::installment),
   Codec.INT.fieldOf("delay").forGetter(State::delay), Codec.INT.fieldOf("reprieve").forGetter(State::reprieve),
   Codec.INT.fieldOf("grounding").forGetter(State::grounding)).apply(i,State::new));
 }
 public static final AttachmentType<State> STATE = AttachmentRegistry.create(Wildercord.id("defensive_foci"),
  b -> b.initializer(() -> State.EMPTY).persistent(State.CODEC));
 public record Origin(String type,String attacker) {
  static final Codec<Origin> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.fieldOf("type").forGetter(Origin::type),Codec.STRING.fieldOf("attacker").forGetter(Origin::attacker)).apply(i,Origin::new));
 }
 private static final AttachmentType<Origin> ORIGIN=AttachmentRegistry.create(Wildercord.id("reprieve_origin"),b->b.persistent(Origin.CODEC));
 private static net.minecraft.world.damagesource.DamageSource source(ServerPlayer p){var origin=p.getAttached(ORIGIN);if(origin==null)return p.damageSources().magic();
  try {var holder=p.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.Identifier.parse(origin.type)));
   net.minecraft.world.entity.Entity attacker=null;if(!origin.attacker.isEmpty())attacker=p.level().getEntity(java.util.UUID.fromString(origin.attacker));
   return new net.minecraft.world.damagesource.DamageSource(holder,attacker,attacker);
  }catch(IllegalArgumentException|IllegalStateException e){return p.damageSources().magic();}
 }
 public static void init() {
  ServerTickEvents.END_SERVER_TICK.register(server -> {
   for (ServerPlayer player : server.getPlayerList().getPlayers()) {
    State s = player.getAttachedOrElse(STATE,State.EMPTY);
    if (s.equals(State.EMPTY)) continue;
    float owed = player.isAlive() ? s.owed : 0;
    int delay = Math.max(0,s.delay-1);
    if (owed > .00001F && delay == 0) {
     float due = Math.min(owed,s.installment);
     // Pay an already reduced hit without passing through the focus or mod armour a second time.
     // These installments ignore vanilla's recent-hit timer but retain Resistance and absorption.
     int old = player.getInvulnerableTime(), hurtCooldown = player.damageCooldownTime;
     Effects.readyToHurt(player);
     try { SpellDefence.guarded(player, () -> player.hurtServer(player.level(),source(player),due)); }
     finally { player.setInvulnerableTime(old); player.damageCooldownTime = hurtCooldown; }
     owed = Math.max(0,owed-due); delay = owed > 0 ? 10 : 0;
     player.sendOverlayMessage(Component.translatable("message.wildercord.reprieve.debt", String.format(java.util.Locale.ROOT,"%.1f",owed)));
    }
    int reprieve = Math.max(0,s.reprieve-1), grounding = Math.max(0,s.grounding-1);
    if(owed<=0)player.removeAttached(ORIGIN);
    player.setAttached(STATE,owed<=0 && reprieve==0 && grounding==0 ? State.EMPTY : new State(owed,s.installment,delay,reprieve,grounding));
   }
  });
 }
 public static boolean available(ServerPlayer player, float amount) {
  State s=player.getAttachedOrElse(STATE,State.EMPTY);
  return !player.isCreative() && !player.isSpectator() && amount>=6 && amount<=1024 && s.reprieve==0 && s.owed<=0
   && Gear.of(player).pieces().contains(GearDef.REPRIEVE);
 }
 public static void defer(ServerPlayer player,float amount) {
  State s=player.getAttachedOrElse(STATE,State.EMPTY);
  player.setAttached(STATE,new State(amount,amount/4,20,RECHARGE,s.grounding));
  player.sendOverlayMessage(Component.translatable("message.wildercord.reprieve.held",String.format(java.util.Locale.ROOT,"%.1f",amount)));
  Light.groundRing(player.level(),player.position(),0xD6BDFF,.2,.8,.025,12);
 }
 public static void defer(ServerPlayer player,float amount,net.minecraft.world.damagesource.DamageSource source){
  String type=source.typeHolder().unwrapKey().map(k->k.identifier().toString()).orElse("minecraft:magic");
  player.setAttached(ORIGIN,new Origin(type,source.getEntity()==null?"":source.getEntity().getStringUUID()));defer(player,amount);
 }
 public static Vec3 resist(LivingEntity target,Vec3 impulse) {
  if (!(target instanceof ServerPlayer player) || impulse.lengthSqr()<.09
    || !Gear.of(player).pieces().contains(GearDef.GROUNDING)) return impulse;
  State s=player.getAttachedOrElse(STATE,State.EMPTY);
  if(s.grounding>0) return impulse;
  player.setAttached(STATE,new State(s.owed,s.installment,s.delay,s.reprieve,RECHARGE));
  player.addEffect(new MobEffectInstance(MobEffects.SPEED,40,0));
  Light.groundRing(player.level(),player.position(),0x98D5B7,.2,1.2,.035,10);
  player.sendOverlayMessage(Component.translatable("message.wildercord.grounding"));
  return impulse.scale(.30);
 }
}
