package dev.wildercord.net;

import dev.wildercord.Wildercord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;

/** One event replaces many scheduled server-side decorative particle sends. Clients track aim until release. */
public record FormationPayload(int caster, String shape, List<String> runes, List<String> elements, int color, float scale) implements CustomPacketPayload {
	public static final Type<FormationPayload> TYPE = new Type<>(Wildercord.id("formation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FormationPayload> CODEC = StreamCodec.of((buf, p) -> {
		buf.writeVarInt(p.caster); buf.writeUtf(p.shape, 128); buf.writeInt(p.color); buf.writeFloat(p.scale);
		buf.writeVarInt(p.runes.size()); for (String id : p.runes) buf.writeUtf(id, 4096);
		buf.writeVarInt(p.elements.size()); for (String id : p.elements) buf.writeUtf(id, 128);
	}, buf -> {
		int caster = buf.readVarInt(); String shape = buf.readUtf(128); int color = buf.readInt(); float scale = buf.readFloat();
		List<String> runes = readStrings(buf, 16, 4096), elements = readStrings(buf, 10, 128);
		if (!Float.isFinite(scale)) throw new IllegalArgumentException("Invalid formation");
		return new FormationPayload(caster, shape, List.copyOf(runes), List.copyOf(elements), color, Math.clamp(scale, 0.4F, 2F));
	});
	private static List<String> readStrings(RegistryFriendlyByteBuf buf, int maximum, int length) {
		int count = buf.readVarInt();
		if (count < 0 || count > maximum) throw new IllegalArgumentException("Formation list too long");
		var values = new java.util.ArrayList<String>(count);
		for (int i = 0; i < count; i++) values.add(buf.readUtf(length));
		return List.copyOf(values);
	}
	@Override public Type<FormationPayload> type() { return TYPE; }
}
