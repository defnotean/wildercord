package dev.wildercord.net;

import dev.wildercord.Wildercord;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Exact-request reconciliation; a rejected older edit never overwrites a newer local draft. */
public record RelayEditorReply(int slot, String request, List<String> accepted, String reason, long session, long revision) implements CustomPacketPayload {
	public static final Type<RelayEditorReply> TYPE = new Type<>(Wildercord.id("relay_edit_result"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RelayEditorReply> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RelayEditorReply::slot, ByteBufCodecs.stringUtf8(64), RelayEditorReply::request,
		ByteBufCodecs.stringUtf8(dev.wildercord.spell.Knots.MAX_ID_LENGTH).apply(ByteBufCodecs.list(12)), RelayEditorReply::accepted,
		ByteBufCodecs.stringUtf8(1024), RelayEditorReply::reason, ByteBufCodecs.LONG, RelayEditorReply::session,
		ByteBufCodecs.LONG, RelayEditorReply::revision, RelayEditorReply::new).cast();
	@Override public Type<RelayEditorReply> type() { return TYPE; }
	public static String key(List<String> ids) {
		try {
			var digest = java.security.MessageDigest.getInstance("SHA-256");
			for (String id : ids) {
				byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
				digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array()); digest.update(bytes);
			}
			return java.util.HexFormat.of().formatHex(digest.digest());
		} catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
	}
	public static void init() { PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC); }
	public static void reply(ServerPlayer player, WildercordNetworking.EditSpell edit, net.minecraft.network.chat.Component problem) {
		int slot = edit.spell();
		if (slot < 0 || slot >= dev.wildercord.gear.SpellSlots.ALL) return;
		String reason = problem == null ? "" : problem.getString();
		if (reason.length() > 1000) reason = reason.substring(0, 1000) + "…";
		ServerPlayNetworking.send(player, new RelayEditorReply(slot, key(edit.runes()), Spellbooks.get(player).spells().get(slot), reason, edit.editorSession(), edit.editorRevision()));
	}
}
