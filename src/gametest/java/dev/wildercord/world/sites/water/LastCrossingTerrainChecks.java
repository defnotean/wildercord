package dev.wildercord.world.sites.water;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/** Registered locator, unmodified default-preset ground and a declared 49-candidate survey. */
final class LastCrossingTerrainChecks {
	private LastCrossingTerrainChecks() {}
	static void run(ClientGameTestContext c) {
		boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
		var camera=c.computeOnClient(mc->mc.options.getCameraType());
		try(var w=c.worldBuilder().setUseConsistentSettings(false).adjustSettings(s->{
			s.setSeed("-7620530482425397421");
			s.setWorldType(s.getNormalPresetList().stream().filter(p->p.preset()!=null&&p.preset().is(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)).findFirst().orElseThrow());
		}).create()) {
			c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s->{
				var level=s.overworld();var piece=find(level);var box=piece.getBoundingBox();
				for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++) {
					level.getChunk(x,z);piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(7),
						new BoundingBox(x*16,level.getMinY()+1,z*16,x*16+15,level.getMaxY(),z*16+15),new ChunkPos(x,z),BlockPos.ZERO);
				}
				var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);
				// Every support reaches a solid column without extending below the declared piece.
				for(int x:new int[]{4,13,21,30})for(int z:new int[]{4,13,21,30}) {
					var bottom=piece.localPosition(x,0,z);
					if(level.getBlockState(bottom).isAir()||!level.getFluidState(bottom).isEmpty())throw new AssertionError("Unfounded basin support at "+bottom);
				}
				var target=Vec3.atCenterOf(piece.localPosition(17,LastCrossingPiece.DECK+2,17));var eye=Vec3.atCenterOf(piece.localPosition(41,LastCrossingPiece.DECK+23,-18));var look=target.subtract(eye.add(0,p.getEyeHeight(),0));
				p.teleportTo(level,eye.x,eye.y,eye.z,Set.of(),(float)Math.toDegrees(Math.atan2(-look.x,look.z)),(float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())),false);
			});
			c.waitTicks(25);w.getConnection().waitForChunksRender();
			c.runOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
			c.waitTicks(4);c.takeScreenshot(TestScreenshotOptions.of("last_crossing_reedlands").disableCounterPrefix());
		} finally {c.runOnClient(mc->{mc.options.setCameraType(camera);if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();});}
	}
	private static LastCrossingPiece find(ServerLevel level) {
		var source=level.getChunkSource();var generator=source.getGenerator();var random=source.randomState();
		if(level.getSeed()!=-7620530482425397421L || !(generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator))throw new AssertionError("The terrain proof needs the recorded seed and normal noise generator");
		var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE,Wildercord.id("water_last_crossing")));
		var sampler=random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED);
		int tested=0;
		for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++) {
			var chunk=new ChunkPos((-2768>>4)+dx,(3296>>4)+dz);tested++;
			var start=holder.value().generate(holder,level.dimension(),level.registryAccess(),generator,generator.getBiomeSource(),sampler,random,
				level.getStructureTemplateManager(),level.getSeed(),chunk,0,level,holder.value().biomes()::contains);
			if(!start.isValid()) {
				var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),sampler,random,level.getStructureTemplateManager(),level.getSeed(),chunk,level,holder.value().biomes()::contains);
				var facing=Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
				var probe=new LastCrossingPiece(chunk.getMinBlockX(),0,chunk.getMinBlockZ(),facing);var samples=new java.util.ArrayList<String>();
				for(int[] xy:new int[][]{{3,3},{31,3},{3,31},{31,31},{17,17},{17,3},{3,17},{31,17},{17,31}}) {
					var at=probe.localPosition(xy[0],0,xy[1]);samples.add(WaterPiece.surface(context,at)+"/"+WaterPiece.floor(context,at));
				}
				var mid=probe.mooringPosition();var biome=context.biomeResolver().getNoiseBiome(mid.getX()>>2,16,mid.getZ()>>2);
				Wildercord.LOGGER.info("LAST_CROSSING_REFUSAL chunk={} facing={} sea={} biome={} surface/floor={}",chunk,facing,generator.getSeaLevel(),biome.unwrapKey(),samples);
				continue;
			}
			if(start.getPieces().size()!=1 || !(start.getPieces().getFirst() instanceof LastCrossingPiece piece))throw new AssertionError("Registered Last Crossing returned a foreign piece");
			Wildercord.LOGGER.info("LAST_CROSSING_TERRAIN seed={} candidate={} tested={} limit=49 bounds={}",level.getSeed(),chunk,tested,piece.getBoundingBox());
			return piece;
		}
		throw new AssertionError("No admitted Last Crossing in the declared 49-candidate natural Reedlands survey");
	}
}
