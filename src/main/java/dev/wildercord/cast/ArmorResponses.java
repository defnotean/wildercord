package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.gear.ElementalArmor;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.WeakHashMap;

/** Edge-triggered crouch guards, capped armour contributions and a weak, non-recursive reflected fragment. */
public final class ArmorResponses {
	private ArmorResponses() {}
	public record State(int rimeWindow,int mirrorWindow,int rimeCooldown,int mirrorCooldown,boolean sneaking) {
		static final State EMPTY=new State(0,0,0,0,false);
		static final Codec<State> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(0,12).fieldOf("rime_window").forGetter(State::rimeWindow),
			Codec.intRange(0,12).fieldOf("mirror_window").forGetter(State::mirrorWindow),
			Codec.intRange(0,100).fieldOf("rime_cooldown").forGetter(State::rimeCooldown),Codec.intRange(0,100).fieldOf("mirror_cooldown").forGetter(State::mirrorCooldown),
			Codec.BOOL.fieldOf("sneaking").forGetter(State::sneaking)).apply(i,State::new));
	}
	public static final AttachmentType<State> STATE=AttachmentRegistry.create(Wildercord.id("armor_response"),b->b.initializer(()->State.EMPTY).persistent(State.CODEC));
	private record Motion(Vec3 last,boolean moving) {}
	private static final WeakHashMap<ServerPlayer,Motion> MOTION=new WeakHashMap<>();
	private static boolean reflecting;
	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server->MOTION.clear());
		ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()) {
			var old=MOTION.get(p); boolean moving=old!=null && old.last.distanceToSqr(p.position())>.0025;
			MOTION.put(p,new Motion(p.position(),moving));
			var s=p.getAttachedOrElse(STATE,State.EMPTY);
			int rimeWindow=Math.max(0,s.rimeWindow-1),mirrorWindow=Math.max(0,s.mirrorWindow-1),rime=Math.max(0,s.rimeCooldown-1),mirror=Math.max(0,s.mirrorCooldown-1);
			boolean edge=p.isShiftKeyDown()&&!s.sneaking;
			boolean hasRime=ElementalArmor.count(p,ElementalArmor.Kind.RIMEBOUND)>0,hasMirror=ElementalArmor.count(p,ElementalArmor.Kind.MIRROR_THREAD)>0;
			if(edge && (hasRime&&rime==0 || hasMirror&&mirror==0)) {
				if(hasRime&&rime==0){rime=80;rimeWindow=12;}if(hasMirror&&mirror==0){mirror=100;mirrorWindow=12;}
				Light.groundRing(p.level(),p.position(),hasMirror?0xEDCAFF:0xA8EAFF,.3,1.1,.025,12);
				p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.wildercord.armor_guard"));
			}
			var next=new State(rimeWindow,mirrorWindow,rime,mirror,p.isShiftKeyDown());
			if(!next.equals(s))p.setAttached(STATE,next);
		}});
	}
	public static double factor(Player p) {
		double protection=ElementalArmor.count(p,ElementalArmor.Kind.STONEBOUND)*.04;
		if(p instanceof ServerPlayer sp && p.isSprinting() && MOTION.get(sp)!=null && MOTION.get(sp).moving)
			protection+=ElementalArmor.count(p,ElementalArmor.Kind.EMBERWEAVE)*.03;
		if(p.getAttachedOrElse(STATE,State.EMPTY).rimeWindow>0)protection+=ElementalArmor.count(p,ElementalArmor.Kind.RIMEBOUND)*.04;
		return 1-Math.min(.20,protection);
	}
	public static boolean mirrorReady(Player p) { return !reflecting && p.getAttachedOrElse(STATE,State.EMPTY).mirrorWindow>0 && ElementalArmor.count(p,ElementalArmor.Kind.MIRROR_THREAD)>0; }
	public static void fragment(net.minecraft.server.level.ServerLevel level,Player p,DamageSource source,float damage) {
		if (source instanceof RelayDamageSource relay && !relay.admits(p)) return;
		var s=p.getAttachedOrElse(STATE,State.EMPTY);p.setAttached(STATE,new State(s.rimeWindow,0,s.rimeCooldown,s.mirrorCooldown,s.sneaking));
		if(!(source.getEntity() instanceof LivingEntity attacker)||attacker.level()!=level||attacker.distanceTo(p)>24||!Targets.canHarm(p,attacker))return;
		Cast scoped = source instanceof RelayDamageSource relay ? RelayCircles.reflected(relay.cast(), p, attacker, p.getEyePosition()) : null;
		if (scoped != null && (!scoped.admits(attacker) || scoped.takeEntities(1) < 1)) return;
		reflecting=true;
		try {
			if (scoped == null) SpellDefence.hurt(level,attacker,level.damageSources().indirectMagic(p,p),Math.min(2,damage*.20F));
			else SpellDefence.hurt(level,attacker,level.damageSources().indirectMagic(p,p),Math.min(2,damage*.20F),scoped);
			Light.ray(level,p.getEyePosition(),attacker.getEyePosition(),0xEED5FF,.045F,8);
		} finally { reflecting=false; }
	}
}
