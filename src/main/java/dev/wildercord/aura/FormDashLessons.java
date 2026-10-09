package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.Duelist;
import dev.wildercord.aura.world.MasterVictories;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * An Ember teacher offers Cinder Lunge; a Gale teacher offers Reed Slip once Wall Turn is learned or out of reach.
 * Offers keep their original body and teacher, as for Wall Turn.
 */
public final class FormDashLessons {
	private FormDashLessons() {}
	public record Open(long nonce, int form, boolean known) implements CustomPacketPayload {
		public static final Type<Open> TYPE = new Type<>(Wildercord.id("field_form_lesson"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Open::nonce, ByteBufCodecs.VAR_INT, Open::form, ByteBufCodecs.BOOL, Open::known, Open::new).cast();
		@Override public Type<Open> type() { return TYPE; }
	}
	public record Accept(long nonce) implements CustomPacketPayload {
		public static final Type<Accept> TYPE = new Type<>(Wildercord.id("field_form_lesson_accept"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Accept> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Accept::nonce, Accept::new).cast();
		@Override public Type<Accept> type() { return TYPE; }
	}
	private record Offer(Duelist teacher, int form, ServerLevel level, long until, long nonce, dev.wildercord.aura.arts.ReleasedArtOwner owner) {}
	private static final Map<ServerPlayer, Offer> OFFERS = new IdentityHashMap<>();
	private static long nonces;
	static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Accept.TYPE, Accept.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Accept.TYPE, (packet, context) -> accept(context.player(), packet.nonce()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> OFFERS.remove(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> OFFERS.clear());
	}
	/**
	 * The field form this teacher would teach this player now, or 0. With a Gale teacher Wall Turn keeps its place first; Reed Slip
	 * goes ahead only while it is unlearned, so a learned Reed Slip never hides Wall Turn's lesson or its re-read.
	 */
	public static int form(ServerPlayer player, Duelist teacher) {
		if (teacher.method().equals(BreathingMethods.EMBER)) return FormDashRules.CINDER_LUNGE;
		if (!teacher.method().equals(BreathingMethods.GALE)) return 0;
		boolean wall = MasterForms.data(player).learned(), wallOffered = wall || MasterForms.eligibleLesson(player);
		if (!FormDash.data(player).knows(FormDashRules.REED_SLIP)) return wallOffered && !wall ? 0 : FormDashRules.REED_SLIP;
		return wallOffered ? 0 : FormDashRules.REED_SLIP;
	}
	public static boolean offer(ServerPlayer player, Duelist teacher) {
		int form = next(player, teacher);
		if (form == 0) form = form(player, teacher);
		if (form == 0 || !player.getMainHandItem().isEmpty() || !player.isShiftKeyDown() || teacher.inDuel() || teacher.leaving()
			|| !player.isAlive() || teacher.level() != player.level() || teacher.distanceToSqr(player) > 36 || !teacher.hasLineOfSight(player)) return false;
		if (!FormDash.eligible(player, form) && !FormDash.data(player).knows(form)) return false;
		OFFERS.entrySet().removeIf(entry -> entry.getKey().isRemoved() || !entry.getValue().owner().valid()
			|| entry.getValue().until() < MasterForms.now(entry.getKey()));
		long nonce = ++nonces;
		OFFERS.put(player, new Offer(teacher, form, player.level(), MasterForms.now(player) + 2400, nonce, dev.wildercord.aura.arts.ReleasedArtOwner.capture(player)));
		if (ServerPlayNetworking.canSend(player, Open.TYPE)) ServerPlayNetworking.send(player, new Open(nonce, form, FormDash.data(player).knows(form)));
		return true;
	}
	public static boolean accept(ServerPlayer player, long nonce) {
		Offer offer = OFFERS.get(player);
		if (offer == null || nonce != offer.nonce()) return false;
		OFFERS.remove(player);
		if (!offer.owner().valid() || offer.level() != player.level() || offer.until() < MasterForms.now(player) || !player.isAlive()
			|| offer.teacher().isRemoved() || !offer.teacher().isAlive() || offer.teacher().level() != player.level()
			|| offer.teacher().inDuel() || offer.teacher().leaving() || offer.teacher().distanceToSqr(player) > 36
			|| !offer.teacher().hasLineOfSight(player) || !FormDash.learn(player, offer.form())) return false;
		if (player.getInventory().getFreeSlot() >= 0) player.getInventory().add(book(offer.form()));
		player.sendSystemMessage(Component.translatable("message.wildercord.field_form.learned." + FormDash.name(offer.form())));
		return true;
	}
	/** A talk hint the player can act on now. Wall Turn's own Gale hint goes first. */
	public static void hint(ServerPlayer player, Duelist teacher) {
		int next = next(player, teacher);
		if (next != 0) { player.sendSystemMessage(Component.translatable("message.wildercord.field_form.teacher_hint." + FormDash.name(next))); return; }
		int form = form(player, teacher);
		if (form == 0 || FormDash.data(player).knows(form) || !Aura.enabled(player)) return;
		boolean cleared = MasterVictories.progress(player).cleared(FormDashRules.school(form));
		if (!FormDashRules.eligible(form, Aura.stage(player), true)) return;
		if (form == FormDashRules.REED_SLIP && !MasterForms.data(player).learned() && Aura.stage(player) >= AuraRules.SOVEREIGN) return;
		player.sendSystemMessage(Component.translatable("message.wildercord.field_form." + (cleared ? "teacher_hint." : "need_clear.") + FormDash.name(form)));
	}
	public static ItemStack book(int form) {
		String name = FormDash.name(form);
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.ITEM_MODEL, Wildercord.id(name + "_lesson"));
		String[] cover = cover(form);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(cover[0]), cover[1], 0,
			List.of(Filterable.passThrough(Component.translatable("book.wildercord." + name + ".1")),
				Filterable.passThrough(Component.translatable("book.wildercord." + name + ".2")),
				Filterable.passThrough(Component.translatable("book.wildercord." + name + ".3"))), true));
		return book;
	}
	// ---- moves pack
	/**
	 * The next lesson on this teacher's ladder, once the teacher's first forms are learned or out of reach: Ember teaches the
	 * riposte then Spell Cut, Gale the shove then Air Step, Stone the break then Plunging Strike. 0 leaves the first forms' offers.
	 */
	public static int next(ServerPlayer player, Duelist teacher) {
		var known = FormDash.data(player);
		int[] ladder;
		boolean before;
		if (teacher.method().equals(BreathingMethods.EMBER)) {
			ladder = new int[] {FormDashRules.RIPOSTE, FormDashRules.SPELL_CUT};
			before = known.knows(FormDashRules.CINDER_LUNGE) || !FormDash.eligible(player, FormDashRules.CINDER_LUNGE);
		} else if (teacher.method().equals(BreathingMethods.GALE)) {
			ladder = new int[] {FormDashRules.SHOVE, FormDashRules.AIR_STEP};
			before = (MasterForms.data(player).learned() || !MasterForms.eligibleLesson(player))
				&& (known.knows(FormDashRules.REED_SLIP) || !FormDash.eligible(player, FormDashRules.REED_SLIP));
		} else if (teacher.method().equals(BreathingMethods.STONE)) {
			ladder = new int[] {FormDashRules.GUARD_BREAK, FormDashRules.PLUNGE};
			before = MasterForms.data(player).knows(MasterForms.STONE_HINGE)
				|| !(MasterForms.testedHinge(player) && MasterForms.eligibleLesson(player, MasterForms.STONE_HINGE));
		} else return 0;
		if (!before) return 0;
		for (int form : ladder) {
			if (known.knows(form)) continue;
			return FormDash.eligible(player, form) ? form : 0;
		}
		return 0;
	}
	private static String[] cover(int form) {
		return switch (form) {
			case FormDashRules.CINDER_LUNGE -> new String[] {"Coals Before the Cut", "Hessa Vane, kiln guard"};
			case FormDashRules.RIPOSTE -> new String[] {"The Answer After the Block", "Hessa Vane, kiln guard"};
			case FormDashRules.SPELL_CUT -> new String[] {"Cutting the Thread", "Hessa Vane, kiln guard"};
			case FormDashRules.SHOVE -> new String[] {"Make Room", "Iven Reed, road tutor"};
			case FormDashRules.AIR_STEP -> new String[] {"A Step on Nothing", "Iven Reed, road tutor"};
			case FormDashRules.GUARD_BREAK -> new String[] {"The Wall Leans Back", "Orla Cairn, quarry warden"};
			case FormDashRules.PLUNGE -> new String[] {"Falling With Purpose", "Orla Cairn, quarry warden"};
			default -> new String[] {"A Reed Gives Way", "Iven Reed, road tutor"};
		};
	}
}
