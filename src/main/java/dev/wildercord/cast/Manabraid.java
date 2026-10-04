package dev.wildercord.cast;
import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.player.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** One actual consensual lossy mana transfer after the normal spell price, with no resource escrow. */
public final class Manabraid {
 private Manabraid(){}
 public static final AttachmentType<Long> DONOR_READY=AttachmentRegistry.create(Wildercord.id("manabraid_donor_ready"),b->b.initializer(()->0L).persistent(Codec.LONG).copyOnDeath());
 public static final AttachmentType<Long> RECEIVE_READY=AttachmentRegistry.create(Wildercord.id("manabraid_receive_ready"),b->b.initializer(()->0L).persistent(Codec.LONG).copyOnDeath());
 private static final class Offer {
  final UUID donor,target;final ServerLevel level;final ItemStack donorCord,targetCord,donorCopy,targetCopy;final long until;boolean crouched;
  Offer(ServerPlayer p,ServerPlayer t,long now){donor=p.getUUID();target=t.getUUID();level=p.level();donorCord=Spellbooks.cord(p);targetCord=Spellbooks.cord(t);donorCopy=donorCord.copy();targetCopy=targetCord.copy();until=now+CampConcordRules.OFFER;crouched=t.isShiftKeyDown();}
 }
 private static final Map<UUID,Offer> ACTIVE=new HashMap<>();
 public static int active(){return ACTIVE.size();}
 public static boolean active(UUID donor){return ACTIVE.containsKey(donor);}
 public record Transfer(UUID donor,UUID target,int debit,int gain,float donorBefore,float targetBefore){}
 private static java.util.function.Consumer<Transfer> observer=e->{};
 /** Test observation sees actual completed resource writes; it cannot admit a gift. */
 public static AutoCloseable observe(java.util.function.Consumer<Transfer> next){var old=observer;observer=Objects.requireNonNull(next);return ()->observer=old;}
 private static boolean magic(ServerPlayer p,ServerLevel l){return CampConcordAdmission.actor(p,l)&&p.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SURVIVAL&&Spellbooks.tier(p)!=null&&!dev.wildercord.aura.Awakening.spent(p)
  &&!p.hasAttached(WildercordAttachments.CHARGE)&&!CastLock.locked(p)&&Float.isFinite(Spellbooks.mana(p))&&Spellbooks.mana(p)>=0;}
 private static boolean pair(ServerPlayer p,ServerPlayer t,ServerLevel l){return p!=t&&magic(p,l)&&magic(t,l)&&Targets.canHelp(p,t)&&!p.isShiftKeyDown()
  &&CampConcordAdmission.cell(p,l,p.blockPosition().below())&&CampConcordAdmission.cell(t,l,t.blockPosition().below())
  &&p.distanceToSqr(t)<=36&&CampConcordAdmission.cells(l,p.getBoundingBox())&&CampConcordAdmission.cells(l,t.getBoundingBox())
  &&CampConcordAdmission.ray(p,l,p.getEyePosition(),t.getBoundingBox().getCenter(),7,null);}
 private static boolean cords(Offer o,ServerPlayer p,ServerPlayer t){return Spellbooks.cord(p)==o.donorCord&&Spellbooks.cord(t)==o.targetCord
  &&ItemStack.matches(o.donorCopy,o.donorCord)&&ItemStack.matches(o.targetCopy,o.targetCord);}
 private static boolean rests(ServerPlayer p,ServerPlayer t,long now){return now>=CampConcordRules.ready(p.getAttachedOrElse(DONOR_READY,0L),now,CampConcordRules.DONOR_REST)
  &&now>=CampConcordRules.ready(t.getAttachedOrElse(RECEIVE_READY,0L),now,CampConcordRules.RECEIVER_REST);}
 private static boolean claims(ServerPlayer p,ServerPlayer t,ServerLevel l,BlockPos a,BlockPos b){return CampConcordAdmission.claim(p,l,a)&&CampConcordAdmission.claim(t,l,b);}
 static boolean offer(Cast c,LivingEntity other){
  if(!(c.caster instanceof ServerPlayer p)||!(other instanceof ServerPlayer t)||c.passive||!c.alive()||!pair(p,t,c.level)||ACTIVE.size()>=CampConcordRules.CAP||ACTIVE.containsKey(p.getUUID())||ACTIVE.values().stream().anyMatch(o->o.target.equals(t.getUUID())))return false;
  long now=CampConcordAdmission.now(p);if(now<0||now>Long.MAX_VALUE-CampConcordRules.DONOR_REST||!rests(p,t,now)||CampConcordRules.gift(Spellbooks.mana(p),Spellbooks.mana(t),Mana.max(t)).gain()==0)return false;
  try(var lease=CampConcordAdmission.Lease.open(p,t)){
   if(lease==null||!c.once("manabraid:offer"))return false;var snap=new Snapshot(p,t);var o=new Offer(p,t,now);
   if(!claims(p,t,c.level,snap.a,snap.b)||!snap.same(p,t)||!pair(p,t,c.level)||!rests(p,t,now)||!cords(o,p,t)||ACTIVE.size()>=CampConcordRules.CAP||ACTIVE.containsKey(p.getUUID())||ACTIVE.values().stream().anyMatch(v->v.target.equals(t.getUUID())))return false;
   ACTIVE.put(p.getUUID(),o);CampConcordFx.braid(p,t,CampConcordFx.OFFER,0);return true;
  }
 }
 private static final class Snapshot {
  final Vec3 ppos,tpos;final BlockPos a,b;final net.minecraft.world.level.block.state.BlockState as,bs;final ItemStack pc,tc,pcopy,tcopy;final float pm,tm;final int maximum;
  final long pr,tr;
  Snapshot(ServerPlayer p,ServerPlayer t){ppos=p.position();tpos=t.position();a=p.blockPosition().below().immutable();b=t.blockPosition().below().immutable();as=p.level().getBlockState(a);bs=t.level().getBlockState(b);
   pc=Spellbooks.cord(p);tc=Spellbooks.cord(t);pcopy=pc.copy();tcopy=tc.copy();pm=Spellbooks.mana(p);tm=Spellbooks.mana(t);maximum=Mana.max(t);pr=p.getAttachedOrElse(DONOR_READY,0L);tr=t.getAttachedOrElse(RECEIVE_READY,0L);}
  boolean same(ServerPlayer p,ServerPlayer t){return p.position().equals(ppos)&&t.position().equals(tpos)&&Spellbooks.cord(p)==pc&&Spellbooks.cord(t)==tc&&ItemStack.matches(pc,pcopy)&&ItemStack.matches(tc,tcopy)
   &&Spellbooks.mana(p)==pm&&Spellbooks.mana(t)==tm&&Mana.max(t)==maximum&&p.getAttachedOrElse(DONOR_READY,0L)==pr&&t.getAttachedOrElse(RECEIVE_READY,0L)==tr
   &&CampConcordAdmission.cell(p,p.level(),a)&&CampConcordAdmission.cell(t,t.level(),b)&&p.level().getBlockState(a).equals(as)&&t.level().getBlockState(b).equals(bs);}
 }
 private static void accept(Offer o,ServerPlayer p,ServerPlayer t,long now){
  if(!ACTIVE.remove(o.donor,o))return;
  try(var lease=CampConcordAdmission.Lease.open(p,t)){
   if(now>Long.MAX_VALUE-CampConcordRules.DONOR_REST||lease==null||!pair(p,t,o.level)||!cords(o,p,t)||!t.isShiftKeyDown()||!rests(p,t,now))return;
   var snap=new Snapshot(p,t);var gift=CampConcordRules.gift(snap.pm,snap.tm,snap.maximum);
   if(gift.gain()==0||!claims(p,t,o.level,snap.a,snap.b)||!snap.same(p,t)||!pair(p,t,o.level)||!cords(o,p,t)||!t.isShiftKeyDown()||!rests(p,t,now))return;
   // No external callback occurs between this complete authority snapshot and both attachment writes.
   p.setAttached(DONOR_READY,now+CampConcordRules.DONOR_REST);t.setAttached(RECEIVE_READY,now+CampConcordRules.RECEIVER_REST);
   Spellbooks.setMana(p,snap.pm-gift.debit());Spellbooks.setMana(t,snap.tm+gift.gain());
   CampConcordFx.braid(p,t,CampConcordFx.TRANSFER,gift.gain());observer.accept(new Transfer(o.donor,o.target,gift.debit(),gift.gain(),snap.pm,snap.tm));
  }
 }
 static void tick(MinecraftServer s){for(var o:List.copyOf(ACTIVE.values())){if(ACTIVE.get(o.donor)!=o)continue;var p=s.getPlayerList().getPlayer(o.donor);var t=s.getPlayerList().getPlayer(o.target);
  if(p==null||t==null||!pair(p,t,o.level)||!cords(o,p,t)||CampConcordAdmission.now(p)>=o.until){if(ACTIVE.remove(o.donor,o)&&p!=null&&t!=null&&p.level()==o.level&&t.level()==o.level)CampConcordFx.braid(p,t,CampConcordFx.DECLINE,0);continue;}
  boolean crouch=t.isShiftKeyDown();if(CampConcordRules.edge(o.crouched,crouch)){accept(o,p,t,CampConcordAdmission.now(p));continue;}o.crouched=crouch;
 }}
 static void depart(UUID id){ACTIVE.values().removeIf(o->o.donor.equals(id)||o.target.equals(id));}
 static void clear(){ACTIVE.clear();observer=e->{};}
}
