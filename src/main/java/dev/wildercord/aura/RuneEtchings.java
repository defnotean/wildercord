package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.*;
import dev.wildercord.content.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** One lasting inscription, paid with mana each time an art or finisher wakes it. */
public final class RuneEtchings {
	private RuneEtchings() {}
	public static final DataComponentType<String> RUNE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		Wildercord.id("blade_rune"), DataComponentType.<String>builder().persistent(Codec.STRING)
			.networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
	/** Saved on the swordsman: putting a blade away, relogging or dying never buys another immediate activation. */
	public static final AttachmentType<Long> READY = AttachmentRegistry.create(Wildercord.id("blade_rune_ready"),
		b -> b.initializer(() -> 0L).persistent(Codec.LONG).copyOnDeath());
	private static boolean waking;
	public static ItemStack etch(ItemStack blade, ItemStack rune, net.minecraft.world.entity.player.Player smith) {
		if (blade.getCount()!=1 || !blade.is(BondedBlades.BONDABLE) || !blade.isDamageableItem()
			|| BondedBlades.foreign(smith,blade) || !(rune.getItem() instanceof RuneItem)
			|| RuneItem.rankOf(rune)!=1) return ItemStack.EMPTY;
		var def=RuneItem.runeOf(rune).orElse(null);
		if (!RuneEtchingRules.accepts(def) || def.id().equals(blade.get(RUNE))) return ItemStack.EMPTY;
		ItemStack out=blade.copy(); out.set(RUNE,def.id()); return out;
	}
	/** Called once per art performance, on its first successful damage, and by the finisher hook. */
	public static boolean wake(ServerPlayer p, LivingEntity foe, float dealt) {
		if (waking || !Float.isFinite(dealt) || dealt<=0 || !p.isAlive() || p.isSpectator()
			|| !foe.isAlive() || foe.isDeadOrDying() || p.level()!=foe.level() || p.distanceToSqr(foe)>32*32
			|| !Aura.enabled(p) || Aura.stage(p)<AuraRules.GLOW || Awakening.spent(p)
			|| !Aura.holdsWeapon(p) || !Targets.canHarm(p,foe) || Spars.partners(p,foe)) return false;
		ItemStack blade=p.getMainHandItem();
		if (BondedBlades.foreign(p,blade)) return false;
		var rune=Runes.get(blade.getOrDefault(RUNE,"")).orElse(null);
		if (!RuneEtchingRules.accepts(rune)) return false;
		long now=p.level().getServer().overworld().getGameTime();
		if (!RuneEtchingRules.ready(now,p.getAttachedOrElse(READY,0L))) return false;
		int price=RuneEtchingRules.price(rune);
		float mana=Spellbooks.mana(p);
		if (Spellbooks.tier(p)==null || !Float.isFinite(mana) || mana<price) return false;
		var plan=SpellCompiler.compile(List.of(Runes.TOUCH,rune));
		int rest=Math.max(RuneEtchingRules.REST,plan.cooldownTicks());
		if (now>Long.MAX_VALUE-rest) return false;
		p.setAttached(READY,now+rest);
		// Creative still pays the same price: the inscription has no separate free-resource mode.
		Spellbooks.setMana(p,mana-price);
		Unity.manaSpent(p,price);
		waking=true;
		try {
			Cast cast=new Cast(p,1,dev.wildercord.player.Heart.Bonuses.NONE,false,null,
				new Cast.Info(plan.root(),2,rune.element(),List.of(Runes.TOUCH,rune))).weigh(price);
			// An inscription is already an answer to a blade hit: it cannot buy a second resonant answer itself.
			cast.once("resonant:"+foe.getUUID());
			LivingEntity recipient=rune.kind()==EffectKind.HELPFUL || rune.kind()==EffectKind.MOVEMENT ? p : foe;
			var hit=new Cast.Hit(List.<Entity>of(recipient),recipient.getBoundingBox().getCenter(),p.getLookAngle(),
				p.getEyePosition(),rune.kind()==EffectKind.WORLD?foe.blockPosition().below():null,null,recipient==p);
			var group=plan.root().groups.getFirst();
			cast.prepareCircle(group);
			Effects.apply(cast,group.effects.getFirst(),hit);
			Grimoire.unlock(p,"aura:rune_etched_blade");
			p.sendOverlayMessage(Component.translatable("message.wildercord.blade_rune.woke",RuneItem.runeName(rune),price));
			return true;
		} finally { waking=false; }
	}
	public static void init() {
		AuraApi.onFinisher(new AuraApi.FinisherHook() {
			@Override public void landed(ServerPlayer p,LivingEntity t,AuraApi.Finisher finisher,float dealt) { wake(p,t,dealt); }
		});
	}
}
