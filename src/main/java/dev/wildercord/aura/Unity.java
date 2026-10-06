package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.net.PacketThrottle;
import dev.wildercord.player.*;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** One brief performance of both paths. Prices, remaining allowances and rest survive saves. */
public final class Unity {
	private Unity() {}
	public static final Codec<UnityRules.State> CODEC=RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.optionalFieldOf("since",-1L).forGetter(UnityRules.State::since),
		Codec.LONG.optionalFieldOf("until",0L).forGetter(UnityRules.State::until),
		Codec.LONG.optionalFieldOf("ready_at",0L).forGetter(UnityRules.State::readyAt),
		Codec.FLOAT.optionalFieldOf("mana_returned",0F).forGetter(UnityRules.State::manaReturned),
		Codec.FLOAT.optionalFieldOf("aura_returned",0F).forGetter(UnityRules.State::auraReturned)
	).apply(i,UnityRules.State::new));
	public static final StreamCodec<ByteBuf,UnityRules.State> STREAM=StreamCodec.composite(
		ByteBufCodecs.VAR_LONG,UnityRules.State::since,ByteBufCodecs.VAR_LONG,UnityRules.State::until,
		ByteBufCodecs.VAR_LONG,UnityRules.State::readyAt,ByteBufCodecs.FLOAT,UnityRules.State::manaReturned,
		ByteBufCodecs.FLOAT,UnityRules.State::auraReturned,UnityRules.State::new);
	public static final AttachmentType<UnityRules.State> STATE=AttachmentRegistry.create(Wildercord.id("unity"),
		b -> b.initializer(() -> UnityRules.State.NONE).persistent(CODEC).copyOnDeath().syncWith(STREAM,AttachmentSyncPredicate.targetOnly()));
	public record Activate() implements CustomPacketPayload {
		public static final Type<Activate> TYPE=new Type<>(Wildercord.id("unity"));
		public static final StreamCodec<RegistryFriendlyByteBuf,Activate> STREAM_CODEC=StreamCodec.unit(new Activate());
		@Override public Type<Activate> type() { return TYPE; }
	}
	private static final PacketThrottle packets=new PacketThrottle(2,20);
	private static boolean converting;
	public static UnityRules.State state(Player p) { return p.getAttachedOrElse(STATE,UnityRules.State.NONE); }
	public static long now(Player p) { return p instanceof ServerPlayer s?s.level().getServer().overworld().getGameTime():p.level().getGameTime(); }
	public static boolean active(Player p) { return state(p).active(now(p)); }
	/** A stable reason key for the page and server response, or null when ready. */
	public static String refusal(Player p) {
		if (!p.isAlive() || p.isSpectator() || p.isCreative()) return "survival";
		if (!Aura.enabled(p) || Aura.stage(p)<UnityRules.STAGE || Heart.active(p)<UnityRules.CIRCLES || Spellbooks.tier(p)==null) return "paths";
		if (Awakening.spent(p) || dev.wildercord.aura.arts.ArtWards.silenced(p)) return "spent";
		if (Spars.sparring(p) || dev.wildercord.aura.world.DuelistDuels.inDuel(p)) return "spar";
		if (!Aura.holdsWeapon(p)) return "weapon";
		long now=now(p);
		if (active(p)) return "active";
		if (now<state(p).readyAt() || now<0 || now>Long.MAX_VALUE-UnityRules.REST) return "rest";
		if (!Float.isFinite(Spellbooks.mana(p)) || !Float.isFinite(Aura.aura(p))
			|| Spellbooks.mana(p)<UnityRules.MANA_PRICE || Aura.aura(p)<UnityRules.AURA_PRICE) return "price";
		return null;
	}
	public static boolean begin(ServerPlayer p) {
        if (dev.wildercord.cast.ExciseCasting.blocking(p)) return false;
		String why=refusal(p);
		if (why!=null) { p.sendOverlayMessage(Component.translatable("message.wildercord.unity."+why));return false; }
		// Written after both payments: activation cannot refund itself.
		Spellbooks.setMana(p,Spellbooks.mana(p)-UnityRules.MANA_PRICE);
		Aura.spend(p,UnityRules.AURA_PRICE,"unity");
		p.setAttached(STATE,UnityRules.begin(now(p)));
		Grimoire.unlock(p,"aura:unity"); UnityFx.begin(p);
		p.sendOverlayMessage(Component.translatable("message.wildercord.unity.begin"));return true;
	}
	private static boolean working(ServerPlayer p) {
		return active(p) && p.isAlive() && !p.isSpectator() && !p.isCreative() && Aura.enabled(p)
			&& Aura.stage(p)>=UnityRules.STAGE && Heart.active(p)>=UnityRules.CIRCLES && Spellbooks.tier(p)!=null
			&& !Awakening.spent(p) && !dev.wildercord.aura.arts.ArtWards.silenced(p) && Aura.holdsWeapon(p)
			&& !Spars.sparring(p) && !dev.wildercord.aura.world.DuelistDuels.inDuel(p);
	}
	/** Only call after actual mana payment, never from restoration, Blood Price, passive or echo events. */
	public static void manaSpent(ServerPlayer p,double paid) {
		if (converting || !working(p)) return;
		var s=state(p);float got=UnityRules.convert(paid,UnityRules.MANA_TO_AURA,s.auraReturned(),UnityRules.AURA_CAP,Aura.capacity(p)-Aura.aura(p));
		if (got<=0) return;
		converting=true;
		try { float actual=(float)Aura.giveBack(p,got);p.setAttached(STATE,s.returned(0,actual));if(actual>0)UnityFx.flow(p,true); }
		finally { converting=false; }
	}
	private static void auraSpent(ServerPlayer p,double paid,String reason,boolean backlash) {
		if (converting || backlash || "unity".equals(reason) || !working(p)) return;
		var s=state(p);float got=UnityRules.convert(paid,UnityRules.AURA_TO_MANA,s.manaReturned(),UnityRules.MANA_CAP,Mana.max(p)-Spellbooks.mana(p));
		if(got<=0)return;
		converting=true;
		try { float before=Spellbooks.mana(p);Mana.restore(p,got);float actual=Math.max(0,Spellbooks.mana(p)-before);
			p.setAttached(STATE,s.returned(actual,0));if(actual>0)UnityFx.flow(p,false); }
		finally { converting=false; }
	}
	public static void stop(ServerPlayer p) { var s=state(p);if(s.since()>=0 && s.until()>s.since())p.setAttached(STATE,s.stop()); }
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Activate.TYPE,Activate.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Activate.TYPE,(payload,ctx) -> { if(packets.allow(ctx.player().getUUID(),ctx.server().getTickCount()))begin(ctx.player()); });
		AuraApi.onSpend(Unity::auraSpent);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for(var p:server.getPlayerList().getPlayers()) {
				var s=state(p);if(s.since()<0 || s.until()<=s.since())continue;
				if(!working(p)) {stop(p);UnityFx.end(p);continue;}
				if((now(p)-s.since())%20==0)UnityFx.breathe(p);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> {stop(h.player);packets.forget(h.player.getUUID());});
		// An integrated shutdown can save the player before its disconnect callback. Stop before that save,
		// and also close a loaded window at join (including an abrupt shutdown's last autosave).
		ServerLifecycleEvents.SERVER_STOPPING.register(s -> s.getPlayerList().getPlayers().forEach(Unity::stop));
		ServerPlayConnectionEvents.JOIN.register((h,sender,s) -> stop(h.player));
		ServerPlayerEvents.AFTER_RESPAWN.register(Aura.AFTER_COPY,(old,p,alive) -> {if(!alive)stop(p);});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> packets.clear());
	}
}
