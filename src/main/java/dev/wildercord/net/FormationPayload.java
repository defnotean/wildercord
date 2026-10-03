package dev.wildercord.net;

import dev.wildercord.Wildercord;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;

/** One event replaces many scheduled server-side decorative particle sends. Clients track aim until release. */
public record FormationPayload(int caster, String shape, List<String> runes, List<String> elements, int color, float scale,
                               int placement, float aimRange, boolean circle, List<String> glyphs) implements CustomPacketPayload {
	public static final int LEGACY=0, CASTER=1, AIMED=2, CIRCLE_ONLY=3;
	/** Compatibility for direct diagnostic/legacy secret preparations. */
	public FormationPayload(int caster,String shape,List<String> runes,List<String> elements,int color,float scale) {
		this(caster,shape,runes,elements,color,scale,LEGACY,24,true,runes);
	}
	public static final Type<FormationPayload> TYPE = new Type<>(Wildercord.id("formation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FormationPayload> CODEC = StreamCodec.of((buf, p) -> {
		buf.writeVarInt(p.caster); buf.writeUtf(p.shape, 128); buf.writeInt(p.color); buf.writeFloat(p.scale);
		buf.writeVarInt(p.runes.size()); for (String id : p.runes) buf.writeUtf(id, 4096);
		buf.writeVarInt(p.elements.size()); for (String id : p.elements) buf.writeUtf(id, 128);
		buf.writeVarInt(p.placement);buf.writeFloat(p.aimRange);buf.writeBoolean(p.circle);
		buf.writeVarInt(p.glyphs.size());for(String id:p.glyphs)buf.writeUtf(id,4096);
	}, buf -> {
		int caster = buf.readVarInt(); String shape = buf.readUtf(128); int color = buf.readInt(); float scale = buf.readFloat();
		List<String> runes = readStrings(buf, 16, 4096), elements = readStrings(buf, 10, 128);
		int placement=buf.readVarInt();float range=buf.readFloat();boolean circle=buf.readBoolean();List<String> glyphs=readStrings(buf,16,4096);
		if (!Float.isFinite(scale) || !Float.isFinite(range) || range<0 || range>128 || placement<LEGACY || placement>CIRCLE_ONLY) throw new IllegalArgumentException("Invalid formation");
		return new FormationPayload(caster, shape, List.copyOf(runes), List.copyOf(elements), color, Math.clamp(scale, 0.4F, 2F),placement,range,circle,List.copyOf(glyphs));
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
