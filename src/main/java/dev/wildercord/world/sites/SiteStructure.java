package dev.wildercord.world.sites;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * An explorable site: one structure type, told by its data ({@code "site": "farm_windmill"}) which site it is.
 * Each site's locator lives in its pack and is looked up in {@link Sites}; an unknown id simply never generates.
 */
public class SiteStructure extends Structure {
	public static final MapCodec<SiteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		settingsCodec(instance),
		Codec.STRING.fieldOf("site").forGetter(structure -> structure.site)
	).apply(instance, SiteStructure::new));

	private final String site;

	public SiteStructure(Structure.StructureSettings settings, String site) {
		super(settings);
		this.site = site;
	}

	public String site() {
		return site;
	}

	@Override
	protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		var locator = Sites.locator(site);
		return locator == null ? Optional.empty() : locator.locate(context);
	}

	@Override
	public StructureType<?> type() {
		return Sites.SITE;
	}
}
