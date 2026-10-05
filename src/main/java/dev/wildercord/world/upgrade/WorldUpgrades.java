package dev.wildercord.world.upgrade;

import dev.wildercord.Wildercord;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Opt-in operator-controlled old-world updates. Startup only reads journals; no job is resumed automatically. */
public final class WorldUpgrades {
	private WorldUpgrades() {}
	private static final Map<MinecraftServer,Session> SESSIONS=new IdentityHashMap<>();
	private static final int PLAYER_DISTANCE=160, SPAWN_DISTANCE=256, RECENT_TICKS=1200;
	public static final int WRITE_FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS;
	private static final class Permit {
		final ServerLevel level;final BlockPos point;final BlockState replacement;boolean consumed;
		Permit(ServerLevel level,BlockPos point,BlockState replacement){this.level=level;this.point=point;this.replacement=replacement;}
	}
	private static final ThreadLocal<Permit> OWN_WRITE=new ThreadLocal<>();
	static final class Session {
		UpgradeEngine engine;Path directory;String failure="",epoch=UUID.randomUUID().toString(),lastProviders="";
		boolean enabled,manualNoClaims;String activeHash="",lastResult="Idle";boolean rollback;
		final String mods=modFingerprint();
		final Map<String,Integer> recentUntil=new HashMap<>();int recentOverflowUntil;
		String policy() {
			String providers=UpgradeClaims.fingerprint();
			if(!providers.equals(lastProviders)){lastProviders=providers;epoch=UUID.randomUUID().toString();}
			return UpgradePlan.digest((epoch+":"+mods+":"+providers+":"+enabled+":"+manualNoClaims).getBytes(StandardCharsets.UTF_8));
		}
		void disable(){enabled=false;manualNoClaims=false;activeHash="";epoch=UUID.randomUUID().toString();}
	}
	public static void init() {
		ServerLifecycleEvents.SERVER_STARTING.register(WorldUpgrades::start);
		ServerLifecycleEvents.SERVER_STOPPED.register(server->SESSIONS.remove(server));
		ServerTickEvents.END_SERVER_TICK.register(WorldUpgrades::tick);
		PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,entity)->{if(level instanceof ServerLevel serverLevel)UpgradeEdits.successfulPlayerEdit(serverLevel,pos);});
		CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->UpgradeCommands.register(dispatcher));
	}
	private static void start(MinecraftServer server) {
		var session=new Session();SESSIONS.put(server,session);
		session.directory=server.getWorldPath(LevelResource.ROOT).resolve("data/wildercord-upgrades");
		boolean retained;
		try {
			retained=false;
			if(!Files.notExists(session.directory))try(var files=Files.newDirectoryStream(session.directory)) {
				for(var file:files)if(file.getFileName().toString().endsWith(".wcu") || file.getFileName().toString().endsWith(".tmp")){retained=true;break;}
			}
		}catch(IOException|RuntimeException ex){throw new IllegalStateException("Cannot inspect retained world-upgrade recovery data; refusing startup to preserve later-edit safety. Restore access or the complete offline backup.",ex);}
		try {
			session.engine=new UpgradeEngine(new UpgradeJournal(session.directory));
		} catch(IOException|RuntimeException e) {
			if(retained)throw new IllegalStateException("Cannot read/force retained world-upgrade recovery manifests; refusing startup to avoid untracked edits. Restore access or the complete offline backup.",e);
			session.failure="Journal unavailable: "+e.getMessage();Wildercord.LOGGER.error("World upgrades disabled: journal setup unsupported; no retained recovery manifests exist",e);
		}
	}

	static Session session(MinecraftServer server) {
		var session=SESSIONS.get(server);
		if(session==null)throw new IllegalStateException("World upgrade journal has not initialized");
		if(session.engine==null)throw new IllegalStateException(session.failure);
		return session;
	}
	static String modFingerprint() {
		String list=FabricLoader.getInstance().getAllMods().stream().map(m->m.getMetadata().getId()+"="+m.getMetadata().getVersion().getFriendlyString()).sorted().toList().toString();
		return UpgradePlan.digest(list.getBytes(StandardCharsets.UTF_8));
	}
	static void enable(MinecraftServer server,boolean manual,String fingerprint) {
		var s=session(server);
		if(!s.mods.equals(fingerprint))throw new IllegalArgumentException("Installed mod fingerprint changed; read status and review again");
		if(manual && UpgradeClaims.configured())throw new IllegalStateException("Configured claim adapters cannot be bypassed with manual no-provider mode");
		if(!manual && !UpgradeClaims.configured())throw new IllegalStateException("NO_PROVIDER_CONFIGURED: deliberate manual-no-claims mode is required on no-claims servers");
		s.enabled=true;s.manualNoClaims=manual;s.epoch=UUID.randomUUID().toString();s.activeHash="";
	}
	static UpgradePlan preview(ServerLevel level,int chunkX,int y,int chunkZ) throws IOException {
		var s=session(level.getServer());var plan=UpgradeTemplates.preview(level,chunkX,y,chunkZ);
		// Scanner works with writes off and absent claim providers, but retains all other exclusions.
		String refusal=siteRefusal(level,plan,false);
		if(!refusal.isEmpty())throw new IllegalStateException(refusal);
		s.engine.preview(plan,s.policy());writePreview(s,plan);return plan;
	}
	static void writePreview(Session s,UpgradePlan plan) throws IOException {
		StringBuilder text=new StringBuilder("Wildercord exact-region preview\nHistorical provenance: UNKNOWN. Natural-looking blocks are not proof of untouched terrain.\n");
		text.append("Family: ").append(plan.family()).append("\nVersion: ").append(plan.version()).append("\nSite: ").append(plan.siteId())
			.append("\nDimension: ").append(plan.dimension()).append("\nChunk: ").append(plan.chunkX()).append(", ").append(plan.chunkZ())
			.append("\nPlan SHA-256: ").append(plan.hash()).append("\nPolicy: ").append(s.policy()).append("\nGuard cells: ").append(plan.cells().size())
			.append("\nWrites: ").append(plan.writes().size()).append("\nNo automatic claim verification in manual-no-provider mode.\nObtain an offline whole-world backup before approval; this manifest is only a bounded affected-block snapshot.\n\n");
		for(var c:plan.cells())text.append(c.changes()?"WRITE ":"GUARD ").append(c.point()).append(" ").append(c.before()).append(" -> ").append(c.after()).append('\n');
		Files.writeString(s.directory.resolve(plan.siteId()+".preview.txt"),text.toString(),StandardCharsets.UTF_8);
	}
	static ServerLevel level(MinecraftServer server,UpgradePlan plan) {
		return server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(plan.dimension())));
	}
	static UpgradeEngine.World world(ServerLevel level) {
		var s=session(level.getServer());
		return new UpgradeEngine.World() {
			public String policy(){return s.policy();}
			public String refusal(UpgradePlan plan,boolean rollback) {
				if(!s.enabled)return "Permanent writes are disabled; deliberate operator enable is required";
				if(!modFingerprint().equals(s.mods))return "Installed mod set changed; restart and review provider configuration";
				String refusal=siteRefusal(level,plan,rollback);if(!refusal.isEmpty())return refusal;
				var claims=UpgradeClaims.query(level,plan);
				if(claims==UpgradeClaims.Status.CLAIMED || claims==UpgradeClaims.Status.UNKNOWN)return "Claim adapter veto: "+claims;
				if(claims==UpgradeClaims.Status.NO_PROVIDER_CONFIGURED && !s.manualNoClaims)return "NO_PROVIDER_CONFIGURED: manual review mode is off";
				return "";
			}
			public String state(UpgradePlan.Point point) {
				var chunk=level.getChunkSource().getChunkNow(Math.floorDiv(point.x(),16),Math.floorDiv(point.z(),16));
				if(chunk==null)return "UNLOADED";return BlockStateParser.serialize(chunk.getBlockState(pos(point)));
			}
			public boolean set(UpgradePlan.Point point,String expected,String replacement) {
				var chunk=level.getChunkSource().getChunkNow(Math.floorDiv(point.x(),16),Math.floorDiv(point.z(),16));
				if(chunk==null || !state(point).equals(expected))return false;
				try {
					BlockState desired=BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK),replacement,false).blockState();
					Permit old=OWN_WRITE.get();OWN_WRITE.set(new Permit(level,pos(point),desired));
					try{return level.setBlock(pos(point),desired,WRITE_FLAGS) && state(point).equals(replacement);}
					finally{if(old==null)OWN_WRITE.remove();else OWN_WRITE.set(old);}
				}catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new IllegalStateException("Invalid immutable block state",ex);}
			}
		};
	}
	/** The only terrain reads are from the resident full chunk; structure starts/references are inspected locally. */
	static String siteRefusal(ServerLevel level,UpgradePlan plan,boolean rollback) {
		if(!level.dimension().identifier().toString().equals(plan.dimension()) || level.getSeed()!=plan.seed())return "Wrong world identity";
		if(!level.dimension().equals(Level.OVERWORLD))return "First adapter supports the Overworld only";
		var chunk=level.getChunkSource().getChunkNow(plan.chunkX(),plan.chunkZ());
		if(chunk==null || !chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL))return "Chunk is unloaded/inaccessible; deferred without loading";
		int x=plan.chunkX()*16+8,z=plan.chunkZ()*16+8;
		var session=session(level.getServer());int now=level.getServer().getTickCount();
		for(var player:level.players())if(Math.abs(player.getX()-x)<=PLAYER_DISTANCE && Math.abs(player.getZ()-z)<=PLAYER_DISTANCE) {
			session.recentUntil.entrySet().removeIf(e->e.getValue()<=now);
			if(session.recentUntil.size()<128 || session.recentUntil.containsKey(plan.siteId()))session.recentUntil.put(plan.siteId(),now+RECENT_TICKS);
			else session.recentOverflowUntil=now+RECENT_TICKS;
			return "Player nearby; region must remain clear for 60 seconds";
		}
		if(session.recentOverflowUntil>now || session.recentUntil.getOrDefault(plan.siteId(),0)>now)return "Recently active region; 60-second quiet period required";
		if(!rollback) {
			if(chunk.getInhabitedTime()>0)return "Inhabited chunk excluded; approval does not override it";
			if(UpgradeEdits.edited(level,plan.chunkX(),plan.chunkZ()))return "Known player-edited chunk excluded";
			if(!chunk.getBlockEntities().isEmpty() || !chunk.getBlockEntitiesPos().isEmpty())return "Container/block entity in chunk excluded";
			if(chunk.getAllStarts().values().stream().anyMatch(start->start.isValid()) || chunk.getAllReferences().values().stream().anyMatch(refs->!refs.isEmpty()))return "Existing structure start/reference excluded";
			var spawn=level.getRespawnData();
			if(spawn.dimension().equals(level.dimension()) && Math.abs(spawn.pos().getX()-x)<=SPAWN_DISTANCE && Math.abs(spawn.pos().getZ()-z)<=SPAWN_DISTANCE)return "Spawn buffer excluded";
		}
		int minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE;
		for(var cell:plan.cells()) {
			if(level.isOutsideBuildHeight(pos(cell.point())) || !level.getWorldBorder().isWithinBounds(pos(cell.point())))return "Region outside current build limits";
			minY=Math.min(minY,cell.point().y());maxY=Math.max(maxY,cell.point().y());
		}
		if(!level.getEntities((net.minecraft.world.entity.Entity)null,new net.minecraft.world.phys.AABB(x-4,minY,z-4,x+5,maxY+1,z+5),entity->!entity.isRemoved()).isEmpty())return "Entity in guarded volume; deferred";
		return "";
	}
	private static void tick(MinecraftServer server) {
		var s=SESSIONS.get(server);if(s==null || s.engine==null || s.activeHash.isEmpty())return;
		try {
			var entry=s.engine.findHash(s.activeHash).orElseThrow();var level=level(server,entry.plan());
			if(level==null){s.lastResult="Dimension unavailable; deferred";return;}
			var result=s.rollback?s.engine.rollback(s.activeHash,world(level),2):s.engine.step(s.activeHash,world(level),2);
			s.lastResult=result.phase()+": "+result.reason();
			if(result.complete())s.activeHash="";
		}catch(IOException|RuntimeException ex){s.activeHash="";s.lastResult="Stopped: "+ex.getMessage();Wildercord.LOGGER.error("World upgrade stopped; recovery manifest retained",ex);}
	}
	/** Durable before-change fence covers creative/noncreative players, commands, explosions and direct chunk writes. */
	public static boolean beforeExternalWrite(ServerLevel level,BlockPos point,BlockState replacement) {
		var own=OWN_WRITE.get();
		if(own!=null && !own.consumed && own.level==level && own.point.equals(point) && own.replacement.equals(replacement)){own.consumed=true;return true;}
		var s=SESSIONS.get(level.getServer());if(s==null || s.engine==null)return true;
		try{s.engine.fence(level.dimension().identifier().toString(),new UpgradePlan.Point(point.getX(),point.getY(),point.getZ()));return true;}
		catch(IOException ex){s.disable();s.lastResult="Journal failed; protected-region write refused";Wildercord.LOGGER.error("Protected upgrade region write refused: durable edit fence unavailable",ex);return false;}
	}
	static BlockPos pos(UpgradePlan.Point point){return new BlockPos(point.x(),point.y(),point.z());}
}
