package dev.wildercord.aura;

import dev.wildercord.cast.*;
import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Successful damage only: shields, friendly fire, misses and corpses never prime a resonance. */
public final class ResonantStrikes {
	private ResonantStrikes() {}
	private static ResonantRules.Ledger ledger = new ResonantRules.Ledger();
	private static boolean answering;
	private static int reactions;
	private record Receipt(UUID target, ResonantRules.Hit hit) {}
	/** Scoped spell credit cannot become a fresh unbound Cast when a later blade closes the pair. */
	private static final LinkedHashMap<Receipt, Cast> SCOPED = new LinkedHashMap<>();
	public static int reactions() { return reactions; }
	/** A closed paid Relay cannot leave delayed spell credit or obstruct a later ordinary blade/spell pair. */
	public static void retire(Cast cast) {
		var entries = SCOPED.entrySet().iterator();
		while (entries.hasNext()) {
			var entry = entries.next();
			if (entry.getValue() == cast) { ledger.retire(entry.getKey().target(), entry.getKey().hit()); entries.remove(); }
		}
	}
	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> { ledger = new ResonantRules.Ledger(); reactions = 0; SCOPED.clear(); });
		ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> { ledger.forget(h.player.getUUID()); SCOPED.keySet().removeIf(key -> key.hit().player().equals(h.player.getUUID())); });
	}
	public static boolean spell(Cast cast, LivingEntity target, String element, float taken) {
		if (answering || !(cast.caster instanceof ServerPlayer p) || !cast.alive() || !cast.admits(target) || !eligible(p,target,taken)
				|| !ResonantRules.ELEMENTS.contains(element) || !cast.once("resonant:" + target.getUUID())) return false;
		return offer(p,target,element,taken,false,cast);
	}
	public static boolean blade(ServerPlayer p, LivingEntity target, float taken) {
		if (answering || !eligible(p,target,taken) || Aura.stage(p)<AuraRules.GLOW || !Aura.enabled(p) || !Aura.holdsWeapon(p)) return false;
		return offer(p,target,Aura.element(p),taken,true,null);
	}
	private static boolean eligible(ServerPlayer p, LivingEntity t, float taken) {
		return p.isAlive() && !p.isSpectator() && Float.isFinite(taken) && taken > 0 && t.isAlive() && !t.isDeadOrDying()
			&& p.level()==t.level() && p.distanceToSqr(t)<=32*32 && Targets.canHarm(p,t) && !Spars.partners(p,t);
	}
	private static boolean offer(ServerPlayer incoming, LivingEntity target, String element, float taken, boolean blade, Cast cast) {
		ServerLevel level=incoming.level();
		long now = level.getServer().overworld().getGameTime();
		SCOPED.keySet().removeIf(key -> !ResonantRules.within(now, key.hit().tick(), ResonantRules.WINDOW));
		var offered = new ResonantRules.Hit(incoming.getUUID(), element, taken, now, blade, cast != null && cast.guardedImpact());
		if (offered.scoped()) {
			SCOPED.put(new Receipt(target.getUUID(), offered), cast);
			while (SCOPED.size() > ResonantRules.LIMIT) SCOPED.pollFirstEntry();
		}
		var pair=ledger.offer(target.getUUID(),offered,(spell,sword) -> {
			Cast receipt = SCOPED.get(new Receipt(target.getUUID(), spell));
			if (spell.scoped() && (receipt == null || !receipt.alive() || !receipt.admits(target))) return false;
			ServerPlayer mage=player(level,spell.player()), striker=player(level,sword.player());
			return mage!=null && striker!=null && eligible(mage,target,spell.taken()) && eligible(striker,target,sword.taken())
				&& (mage==striker || WayBanner.ally(striker,mage)) && Aura.enabled(striker) && Aura.stage(striker)>=AuraRules.GLOW
				&& Aura.holdsWeapon(striker) && !Awakening.spent(striker) && Aura.aura(striker)>=ResonantRules.COST;
		});
		if(pair==null) return false;
		ServerPlayer mage=player(level,pair.spell().player()), striker=player(level,pair.blade().player());
		Cast scoped = SCOPED.remove(new Receipt(target.getUUID(), pair.spell()));
		if (pair.spell().scoped() && (scoped == null || !scoped.alive() || !scoped.admits(target))) return false;
		var counter = MastersArts.earnedCounter(striker);
		if (counter != null && !counter.permits(target)) return false;
		answering=true;
		try {
			Aura.spend(striker,ResonantRules.COST,"resonant_strike");
			if (counter != null && !counter.permits(target)) return false;
			String name=ResonantRules.name(pair.spell().element(),pair.blade().element());
			// Do not run Effects.hurt recursively: this bonus earns no hit resources or extra rune triggers.
			float extra=ResonantRules.damage(pair.spell().taken(),pair.blade().taken(),target instanceof Player);
			extra *= (float)(target instanceof Player ? Config.get().pvpDamageScale() : 1);
			extra *= (float)Math.max(0,Config.get().aura().damageScale());
			extra=Math.min(extra,target instanceof Player?.75F:3F);
			Cast identity=scoped!=null?scoped:cast!=null?cast:new Cast(mage);
			SpellDefence.resonantHurt(identity,target,extra);
			if (counter != null && !counter.afterDamage(target)) return false;
			if (identity.guardedImpact() && (!identity.alive() || !identity.admits(target) || Shields.blocked(identity, target))) return false;
			utility(striker,mage,target,pair);
			if (counter != null && !counter.afterDamage(target)) return false;
			ResonantVfx.play(level,striker,target,pair.spell().element(),pair.blade().element(),name);
			Component message=Component.translatable("reaction.wildercord.resonant",Component.translatable("reaction.wildercord.resonant."+name));
			striker.sendOverlayMessage(message); if(mage!=striker) mage.sendOverlayMessage(message);
			Grimoire.unlock(striker,"aura:resonant_strike"); if(mage!=striker) Grimoire.unlock(mage,"aura:resonant_strike");
			reactions++;
			return true;
		} finally { answering=false; }
	}
	private static ServerPlayer player(ServerLevel level, UUID id) { return level.players().stream().filter(p -> p.getUUID().equals(id)).findFirst().orElse(null); }
	private static void utility(ServerPlayer striker, ServerPlayer mage, LivingEntity target, ResonantRules.Pair pair) {
		if(!target.isAlive()) return;
		boolean pvp=target instanceof Player;
		switch(pair.spell().element()) {
			case "fire" -> Stance.art(striker,target,2,1,pvp?1:3);
			case "frost" -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,pvp?12:40,0,false,true),mage);
			case "storm" -> Stance.art(striker,target,4,1,pvp?2:5);
			case "wind" -> striker.addEffect(new MobEffectInstance(MobEffects.SPEED,40,0,false,true),mage);
			case "earth" -> Stance.art(striker,target,8,1,pvp?3:8);
			case "life" -> { if(!pvp && !(target instanceof TrainingDummy) && target.level().dimension()!=PracticeRoom.DIMENSION) striker.heal(Math.min(1,pair.spell().taken()*.1F)); }
			case "void" -> { if(!pvp) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0,false,true),mage); }
			case "arcane" -> Reactions.mark(target,Reactions.Mark.EXPOSED,pvp?12:40);
			case "time" -> striker.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,pvp?12:30,0,false,true),mage);
			case "blood" -> { if(!pvp) Stance.art(striker,target,4+Math.min(4,target.getMaxHealth()-target.getHealth()),1,6); }
			default -> { }
		}
	}
}
