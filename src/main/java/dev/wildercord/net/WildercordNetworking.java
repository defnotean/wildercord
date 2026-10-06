package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.SpellCaster;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Requests from the client (the server validates every one of them), and a few notices back. */
public final class WildercordNetworking {
	private WildercordNetworking() {}

	/** Cast a spell: its index, or -1 for the selected one. */
	public record CastSpell(int spell) implements CustomPacketPayload {
		public static final Type<CastSpell> TYPE = new Type<>(Wildercord.id("cast_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CastSpell> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, CastSpell::spell, CastSpell::new).cast();

		@Override
		public Type<CastSpell> type() {
			return TYPE;
		}
	}

	public record SelectSpell(int spell) implements CustomPacketPayload {
		public static final Type<SelectSpell> TYPE = new Type<>(Wildercord.id("select_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SelectSpell> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, SelectSpell::spell, SelectSpell::new).cast();

		@Override
		public Type<SelectSpell> type() {
			return TYPE;
		}
	}

	public record EditSpell(int spell, List<String> runes, long editorSession, long editorRevision) implements CustomPacketPayload {
		public EditSpell(int spell, List<String> runes) { this(spell, runes, 0, 0); }
		public static final Type<EditSpell> TYPE = new Type<>(Wildercord.id("edit_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditSpell> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, EditSpell::spell,
			ByteBufCodecs.stringUtf8(dev.wildercord.spell.Knots.MAX_ID_LENGTH).apply(ByteBufCodecs.list(32)), EditSpell::runes,
			ByteBufCodecs.LONG, EditSpell::editorSession, ByteBufCodecs.LONG, EditSpell::editorRevision,
			EditSpell::new).cast();

		@Override
		public Type<EditSpell> type() {
			return TYPE;
		}
	}

	public record EditPassive(int slot, List<String> runes) implements CustomPacketPayload {
		public static final Type<EditPassive> TYPE = new Type<>(Wildercord.id("edit_passive"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditPassive> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, EditPassive::slot,
			ByteBufCodecs.stringUtf8(dev.wildercord.spell.Knots.MAX_ID_LENGTH).apply(ByteBufCodecs.list(16)), EditPassive::runes,
			EditPassive::new).cast();

		@Override
		public Type<EditPassive> type() {
			return TYPE;
		}
	}

	public record TogglePassive(int slot) implements CustomPacketPayload {
		public static final Type<TogglePassive> TYPE = new Type<>(Wildercord.id("toggle_passive"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TogglePassive> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, TogglePassive::slot, TogglePassive::new).cast();

		@Override
		public Type<TogglePassive> type() {
			return TYPE;
		}
	}

	/** Start (true) or release (false) charging a spell: its index, or -1 for the selected one. */
	public record ChargeSpell(int spell, boolean start) implements CustomPacketPayload {
		public static final Type<ChargeSpell> TYPE = new Type<>(Wildercord.id("charge_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ChargeSpell> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, ChargeSpell::spell, ByteBufCodecs.BOOL, ChargeSpell::start, ChargeSpell::new).cast();

		@Override
		public Type<ChargeSpell> type() {
			return TYPE;
		}
	}

	/**
	 * How well the caster traced the glyph of the charge that began at {@code chargeStart}, 0 to 1, sent
	 * just before letting it go. The client scores the trace; the server only remembers it for that
	 * charge, and believes it as far as the time the caster really spent steadying allows.
	 */
	public record TraceSpell(long chargeStart, float accuracy) implements CustomPacketPayload {
		public static final Type<TraceSpell> TYPE = new Type<>(Wildercord.id("trace_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TraceSpell> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_LONG, TraceSpell::chargeStart, ByteBufCodecs.FLOAT, TraceSpell::accuracy, TraceSpell::new).cast();

		@Override
		public Type<TraceSpell> type() {
			return TYPE;
		}
	}

	/** Give a spell a custom name ("" goes back to the automatic one). */
	public record RenameSpell(int spell, String name) implements CustomPacketPayload {
		public static final Type<RenameSpell> TYPE = new Type<>(Wildercord.id("rename_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, RenameSpell> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, RenameSpell::spell, ByteBufCodecs.stringUtf8(64), RenameSpell::name, RenameSpell::new).cast();

		@Override
		public Type<RenameSpell> type() {
			return TYPE;
		}
	}

	/** Inscribe a spell onto a scroll (costs paper, ink and mana). */
	public record InscribeScroll(int spell) implements CustomPacketPayload {
		public static final Type<InscribeScroll> TYPE = new Type<>(Wildercord.id("inscribe_scroll"));
		public static final StreamCodec<RegistryFriendlyByteBuf, InscribeScroll> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, InscribeScroll::spell, InscribeScroll::new).cast();

		@Override
		public Type<InscribeScroll> type() {
			return TYPE;
		}
	}

	/**
	 * Save, load, rename or delete a loadout, or load the next one (the quick-switch key): {@code kind} is
	 * one of {@code Loadouts.SAVE_NEW}, {@code SAVE_OVER}, {@code LOAD}, {@code RENAME}, {@code DELETE} or
	 * {@code NEXT}, {@code index} the loadout's place in the list, {@code name} a new name (saving as new,
	 * renaming). Saving always saves the server's own spellbook: no runes travel in this packet.
	 */
	public record LoadoutRequest(int kind, int index, String name) implements CustomPacketPayload {
		public static final Type<LoadoutRequest> TYPE = new Type<>(Wildercord.id("loadout"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutRequest> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LoadoutRequest::kind, ByteBufCodecs.VAR_INT, LoadoutRequest::index, ByteBufCodecs.stringUtf8(64), LoadoutRequest::name,
			LoadoutRequest::new).cast();

		@Override
		public Type<LoadoutRequest> type() {
			return TYPE;
		}
	}

	/** Server to client: a new Grimoire entry (the client shows a toast). */
	public record Discovery(String key) implements CustomPacketPayload {
		public static final Type<Discovery> TYPE = new Type<>(Wildercord.id("discovery"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Discovery> CODEC =
			StreamCodec.composite(ByteBufCodecs.stringUtf8(128), Discovery::key, Discovery::new).cast();

		@Override
		public Type<Discovery> type() {
			return TYPE;
		}
	}

	/**
	 * Server to client: the seed ley lines are drawn from. It's derived from the world seed by a
	 * one-way hash, so the world seed itself never leaves the server.
	 */
	public record LeySeed(long seed) implements CustomPacketPayload {
		public static final Type<LeySeed> TYPE = new Type<>(Wildercord.id("ley_seed"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LeySeed> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_LONG, LeySeed::seed, LeySeed::new).cast();

		@Override
		public Type<LeySeed> type() {
			return TYPE;
		}
	}

	/**
	 * A screen effect for one player: a camera shake, a field-of-view kick (a charged spell leaving
	 * the hands), an impact punch (a heavy hit landing), or the tint of a Domain around them.
	 *
	 * @param kind     {@link #SHAKE}, {@link #KICK}, {@link #PUNCH} or {@link #TINT}
	 * @param strength 0 to 1 (for TINT, the colour in the low 24 bits)
	 * @param ticks    how long it lasts
	 */
	public record ScreenFx(int kind, float strength, int ticks) implements CustomPacketPayload {
		public static final int SHAKE = 0;
		public static final int KICK = 1;
		public static final int PUNCH = 2;
		public static final int TINT = 3;
		public static final Type<ScreenFx> TYPE = new Type<>(Wildercord.id("screen_fx"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ScreenFx> CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, ScreenFx::kind, ByteBufCodecs.FLOAT, ScreenFx::strength, ByteBufCodecs.VAR_INT, ScreenFx::ticks,
				ScreenFx::new).cast();

		@Override
		public Type<ScreenFx> type() {
			return TYPE;
		}
	}

	/** The backpack key: open the backpack worn in the Backpack slot. */
	public record OpenBackpack() implements CustomPacketPayload {
		public static final Type<OpenBackpack> TYPE = new Type<>(Wildercord.id("open_backpack"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenBackpack> CODEC = StreamCodec.unit(new OpenBackpack());

		@Override
		public Type<OpenBackpack> type() {
			return TYPE;
		}
	}

	/** Spellbook rewrites (select, edit, rename, a passive's switch, loadouts) and inscribing: a burst of 20, then 10 a second. */
	private static final PacketThrottle SPELLBOOK = new PacketThrottle(20, 2);

	/** Casting (a cast, or a charge started or let go), each of which reads the spell afresh: see {@link PacketThrottle#casting()}. */
	private static final PacketThrottle CASTING = PacketThrottle.casting();

	/** Whether this spellbook-rewriting packet may be handled: a flood past the allowance is dropped. */
	private static boolean allowed(ServerPlayNetworking.Context context) {
		return SPELLBOOK.allow(context.player().getUUID(), context.server().getTickCount());
	}

	public static void init() {
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SPELLBOOK.forget(handler.player.getUUID());
			CASTING.forget(handler.player.getUUID());
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SPELLBOOK.clear();
			CASTING.clear();
		});
		PayloadTypeRegistry.clientboundPlay().register(ScreenFx.TYPE, ScreenFx.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FormationPayload.TYPE, FormationPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LifeOutcomePayload.TYPE, LifeOutcomePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TidewardCue.TYPE, TidewardCue.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NotebookPayload.TYPE, NotebookPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditPassive.TYPE, EditPassive.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TogglePassive.TYPE, TogglePassive.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TogglePassive.TYPE, (payload, context) -> {
			if (allowed(context)) {
				SpellCaster.togglePassive(context.player(), payload.slot());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(EditPassive.TYPE, (payload, context) -> {
			if (!allowed(context)) {
				return;
			}
			net.minecraft.network.chat.Component problem = SpellCaster.editPassive(context.player(), payload.slot(), payload.runes());
			if (problem != null) {
				context.player().sendOverlayMessage(problem.copy().withStyle(net.minecraft.ChatFormatting.RED));
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(CastSpell.TYPE, CastSpell.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectSpell.TYPE, SelectSpell.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditSpell.TYPE, EditSpell.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CastSpell.TYPE, (payload, context) -> {
			if (CASTING.allow(context.player().getUUID(), context.server().getTickCount())) {
				SpellCaster.cast(context.player(), payload.spell());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(SelectSpell.TYPE, (payload, context) -> {
			if (allowed(context)) {
				SpellCaster.select(context.player(), payload.spell());
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(ChargeSpell.TYPE, ChargeSpell.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RenameSpell.TYPE, RenameSpell.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(InscribeScroll.TYPE, InscribeScroll.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Discovery.TYPE, Discovery.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LeySeed.TYPE, LeySeed.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ChargeSpell.TYPE, (payload, context) -> {
			if (CASTING.allow(context.player().getUUID(), context.server().getTickCount())) {
				dev.wildercord.cast.Charging.request(context.player(), payload.spell(), payload.start());
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(TraceSpell.TYPE, TraceSpell.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TraceSpell.TYPE, (payload, context) -> {
			if (CASTING.allow(context.player().getUUID(), context.server().getTickCount())) {
				dev.wildercord.cast.Charging.trace(context.player(), payload.chargeStart(), payload.accuracy());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(RenameSpell.TYPE, (payload, context) -> {
			if (allowed(context)) {
				SpellCaster.rename(context.player(), payload.spell(), payload.name());
			}
		});
		// Limited like the rest: in creative a scroll costs nothing, and a flood would drop item after item.
		ServerPlayNetworking.registerGlobalReceiver(InscribeScroll.TYPE, (payload, context) -> {
			if (allowed(context)) {
				dev.wildercord.content.SpellScrollItem.inscribe(context.player(), payload.spell());
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(LoadoutRequest.TYPE, LoadoutRequest.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LoadoutRequest.TYPE, (payload, context) -> {
			if (!allowed(context)) {
				return;
			}
			dev.wildercord.loadout.Loadouts.Result result = dev.wildercord.loadout.Loadouts.request(context.player(), payload.kind(), payload.index(), payload.name());
			if (result != null) {
				context.player().sendOverlayMessage(result.ok() ? result.message().copy().withColor(0x7FE0F0)
					: result.message().copy().withStyle(net.minecraft.ChatFormatting.RED));
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(OpenBackpack.TYPE, OpenBackpack.CODEC);
		// Limited like the rest: each opening closes and reopens the menu, reading and writing the backpack.
		ServerPlayNetworking.registerGlobalReceiver(OpenBackpack.TYPE, (payload, context) -> {
			if (allowed(context)) {
				dev.wildercord.backpack.Backpacks.openWorn(context.player());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(EditSpell.TYPE, (payload, context) -> {
			if (!allowed(context)) {
				return;
			}
			boolean relayEdit = dev.wildercord.spell.RelayRules.containsIds(payload.runes()) || dev.wildercord.cast.RelayCircles.contains(context.player(), payload.spell());
			net.minecraft.network.chat.Component problem = SpellCaster.edit(context.player(), payload.spell(), payload.runes());
			if (relayEdit) RelayEditorReply.reply(context.player(), payload, problem);
			if (problem != null) {
				context.player().sendOverlayMessage(problem.copy().withStyle(net.minecraft.ChatFormatting.RED));
			}
		});
	}
}
