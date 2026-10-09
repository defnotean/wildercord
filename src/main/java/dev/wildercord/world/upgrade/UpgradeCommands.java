package dev.wildercord.world.upgrade;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Explicit operator workflow. Every destructive command names the complete immutable preview hash. */
public final class UpgradeCommands {
	private UpgradeCommands() {}
	@FunctionalInterface private interface Action {String run(CommandContext<CommandSourceStack> context) throws Exception;}
	private static int run(CommandContext<CommandSourceStack> context,Action action) {
		try{String result=action.run(context);context.getSource().sendSuccess(()->Component.literal(result),false);return 1;}
		catch(Exception ex){context.getSource().sendFailure(Component.literal("World upgrade refused: "+ex.getMessage()));return 0;}
	}
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		var root=Commands.literal("wildercord-upgrade").requires(Commands.hasPermission(Commands.LEVEL_OWNERS));
		root.then(Commands.literal("status").executes(c->run(c,ctx->{
			var s=WorldUpgrades.session(ctx.getSource().getServer());
			return "Writes: "+(s.enabled?"enabled this session":"OFF")+"; claims: "+(UpgradeClaims.configured()?"adapters configured":s.manualNoClaims?"MANUAL REVIEW, no automatic claim verification":"NO_PROVIDER_CONFIGURED")
				+"; journal sites: "+s.engine.entries().size()+"; "+s.lastResult+"\nInstalled-mod fingerprint: "+s.mods+"\nProvider policy: "+s.policy();
		})));
		root.then(Commands.literal("disable").executes(c->run(c,ctx->{WorldUpgrades.session(ctx.getSource().getServer()).disable();return "Writes disabled. Existing manifests and rollback snapshots retained.";})));
		root.then(enable("enable_adapters",false));
		root.then(enable("declare_no_external_claims",true));
		root.then(Commands.literal("preview").then(Commands.argument("chunk_x",IntegerArgumentType.integer(-1874999,1874999))
			.then(Commands.argument("floor_y",IntegerArgumentType.integer(-2032,2031)).then(Commands.argument("chunk_z",IntegerArgumentType.integer(-1874999,1874999))
			.executes(c->run(c,ctx->{
				var p=WorldUpgrades.preview(ctx.getSource().getLevel(),IntegerArgumentType.getInteger(ctx,"chunk_x"),IntegerArgumentType.getInteger(ctx,"floor_y"),IntegerArgumentType.getInteger(ctx,"chunk_z"));
				var first=p.cells().getFirst().point();var last=p.cells().getLast().point();
				return "PREVIEW ONLY: Wayfarer Training Pavilion, "+p.writes().size()+" inert writes, "+p.dimension()+", guarded region "+first+" to "+last
					+". Historical provenance UNKNOWN. Review every WRITE/GUARD in data/wildercord-upgrades/"+p.siteId()+".preview.txt and make an offline whole-world backup.\nExact preview hash: "+p.hash();
			}))))));
		var site=Commands.literal("preview_site");
		for(var family:UpgradeCatalog.ALL)if(family.decision()==UpgradeCatalog.Decision.BOUNDED_ADAPTER)
			site.then(Commands.literal(family.id().substring(family.id().indexOf(':')+1)).then(Commands.argument("chunk_x",IntegerArgumentType.integer(-1874999,1874999))
				.then(Commands.argument("floor_y",IntegerArgumentType.integer(-2032,2031)).then(Commands.argument("chunk_z",IntegerArgumentType.integer(-1874999,1874999))
				.executes(c->run(c,ctx->{
					var p=WorldUpgrades.preview(ctx.getSource().getLevel(),family.id(),IntegerArgumentType.getInteger(ctx,"chunk_x"),IntegerArgumentType.getInteger(ctx,"floor_y"),IntegerArgumentType.getInteger(ctx,"chunk_z"));
					var first=p.cells().getFirst().point();var last=p.cells().getLast().point();
					return "PREVIEW ONLY: "+family.id()+" v"+p.version()+", "+p.writes().size()+" writes, "+p.dimension()+", guarded region "+first+" to "+last
						+(family.unique()?". Unique encounter: one per "+family.structureSet()+" spread region; no entity is spawned":"")
						+". Historical provenance UNKNOWN. Review every WRITE/GUARD in data/wildercord-upgrades/"+p.siteId()+".preview.txt and make an offline whole-world backup.\nExact preview hash: "+p.hash();
				}))))));
		root.then(site);
		root.then(Commands.literal("catalog").executes(c->run(c,ctx->{
			var text=new StringBuilder("Reviewed worldgen catalog:");
			for(var f:UpgradeCatalog.ALL)text.append("\n").append(f.decision()).append(" ").append(f.id()).append(": ").append(f.reason());
			return text.toString();
		})));
		root.then(Commands.literal("sites").executes(c->run(c,ctx->{
			var data=UpgradeSites.of(ctx.getSource().getServer().overworld());var all=data.sites();
			var text=new StringBuilder("Site records: "+all.size()+(data.saturated()?" (SATURATED: unique encounters refused)":""));
			all.stream().limit(32).forEach(s->text.append("\n").append(s.origin()).append(" ").append(s.family()).append(" v").append(s.version()).append(" ").append(s.state()).append(" chunk ").append(s.chunkX()).append(",").append(s.chunkZ()));
			return text.toString();
		})));
		root.then(authorize("approve",false,false));
		root.then(authorize("reauthorize",true,false));
		root.then(authorize("reauthorize_rollback",true,true));
		root.then(job("resume",false));root.then(job("rollback",true));
		root.then(Commands.literal("inspect").then(Commands.argument("hash",StringArgumentType.word()).executes(c->run(c,ctx->{
			var s=WorldUpgrades.session(ctx.getSource().getServer());var e=s.engine.findHash(hash(ctx)).orElseThrow(()->new IllegalArgumentException("Unknown full hash"));
			return e.phase()+"; "+e.plan().family()+" v"+e.plan().version()+"; chunk "+e.plan().chunkX()+","+e.plan().chunkZ()+"; guarded edits "+e.edits().size()
				+"; original history UNKNOWN; operator "+e.operator()+"; backup reference "+e.backup()+"; applied means observed, not an atomic chunk/journal save.";
		}))));
		dispatcher.register(root);
	}
	private static LiteralArgumentBuilder<CommandSourceStack> enable(String name,boolean manual) {
		return Commands.literal(name).then(Commands.argument("installed_mod_fingerprint",StringArgumentType.word()).executes(c->run(c,ctx->{
			WorldUpgrades.enable(ctx.getSource().getServer(),manual,StringArgumentType.getString(ctx,"installed_mod_fingerprint"));
			return manual?"Manual-no-provider mode enabled for this session. You declared no external claims system. This lacks automatic claim verification; review every exact region. Preview again before approval."
				:"Claim-adapter mode enabled for this session. Every adapter must return CLEAR. Preview again before approval.";
		})));
	}
	private static LiteralArgumentBuilder<CommandSourceStack> authorize(String name,boolean recovery,boolean rollback) {
		return Commands.literal(name).then(Commands.argument("hash",StringArgumentType.word())
			.then(Commands.literal("accept_unknown_history_and_confirm_offline_backup")
			.then(Commands.argument("backup_reference",StringArgumentType.word()).executes(c->run(c,ctx->{
				var server=ctx.getSource().getServer();var s=WorldUpgrades.session(server);
				if(!s.activeHash.isEmpty())throw new IllegalStateException("Another bounded transaction is queued; disable cancels scheduling without discarding its journal");
				var entry=s.engine.findHash(hash(ctx)).orElseThrow(()->new IllegalArgumentException("Unknown full hash"));
				var level=WorldUpgrades.level(server,entry.plan());if(level==null)throw new IllegalStateException("Dimension unavailable");
				String operator=ctx.getSource().getTextName(),backup=StringArgumentType.getString(ctx,"backup_reference");
				if(recovery) {
					if(rollback)s.engine.reauthorizeRollback(hash(ctx),operator,backup,WorldUpgrades.world(level));
					else s.engine.reauthorize(hash(ctx),operator,backup,true,WorldUpgrades.world(level));
					return "Exact manifest reauthorized under current claim/provider policy. Run "+(rollback?"rollback ":"resume ")+hash(ctx)+" explicitly.";
				}
				s.engine.approve(hash(ctx),operator,backup,true,WorldUpgrades.world(level));s.activeHash=hash(ctx);s.rollback=false;UpgradeSites.mirror(level,s.engine.entries());
				return "Exact region approved and queued, at most two writes per tick. Original history remains UNKNOWN. Use inspect/status; unloading or player approach defers work.";
			})))));
	}
	private static LiteralArgumentBuilder<CommandSourceStack> job(String name,boolean rollback) {
		return Commands.literal(name).then(Commands.argument("hash",StringArgumentType.word()).executes(c->run(c,ctx->{
			var s=WorldUpgrades.session(ctx.getSource().getServer());
			if(!s.activeHash.isEmpty())throw new IllegalStateException("A transaction is already queued");
			var e=s.engine.findHash(hash(ctx)).orElseThrow(()->new IllegalArgumentException("Unknown full hash"));
			if(e.phase()==UpgradeJournal.Phase.PREVIEW)throw new IllegalStateException("Preview has no placement approval");
			if(!e.policy().equals(s.policy()))throw new IllegalStateException("Restart/provider change requires explicit "+(rollback?"reauthorize_rollback":"reauthorize"));
			s.activeHash=hash(ctx);s.rollback=rollback;return (rollback?"Conflict-aware rollback":"Journaled resume")+" queued for the exact manifest; at most two writes per tick.";
		})));
	}
	private static String hash(CommandContext<CommandSourceStack> context){return StringArgumentType.getString(context,"hash");}
}
