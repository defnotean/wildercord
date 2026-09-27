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

/** Client-to-server requests. The server validates every one of them. */
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

	public record EditSpell(int spell, List<String> runes) implements CustomPacketPayload {
		public static final Type<EditSpell> TYPE = new Type<>(Wildercord.id("edit_spell"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EditSpell> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, EditSpell::spell,
			ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(32)), EditSpell::runes,
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
			ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(16)), EditPassive::runes,
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

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(EditPassive.TYPE, EditPassive.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TogglePassive.TYPE, TogglePassive.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TogglePassive.TYPE, (payload, context) -> SpellCaster.togglePassive(context.player(), payload.slot()));
		ServerPlayNetworking.registerGlobalReceiver(EditPassive.TYPE, (payload, context) -> {
			net.minecraft.network.chat.Component problem = SpellCaster.editPassive(context.player(), payload.slot(), payload.runes());
			if (problem != null) {
				context.player().sendOverlayMessage(problem.copy().withStyle(net.minecraft.ChatFormatting.RED));
			}
		});
		PayloadTypeRegistry.serverboundPlay().register(CastSpell.TYPE, CastSpell.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SelectSpell.TYPE, SelectSpell.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EditSpell.TYPE, EditSpell.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CastSpell.TYPE, (payload, context) -> SpellCaster.cast(context.player(), payload.spell()));
		ServerPlayNetworking.registerGlobalReceiver(SelectSpell.TYPE, (payload, context) -> SpellCaster.select(context.player(), payload.spell()));
		ServerPlayNetworking.registerGlobalReceiver(EditSpell.TYPE, (payload, context) -> {
			net.minecraft.network.chat.Component problem = SpellCaster.edit(context.player(), payload.spell(), payload.runes());
			if (problem != null) {
				context.player().sendOverlayMessage(problem.copy().withStyle(net.minecraft.ChatFormatting.RED));
			}
		});
	}
}
