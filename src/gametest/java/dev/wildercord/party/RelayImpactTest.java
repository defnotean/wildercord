package dev.wildercord.party;

import dev.wildercord.cast.*;

import com.mojang.authlib.GameProfile;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.party.Parties;
import dev.wildercord.party.PartyRules;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Real release callbacks change admission between selection, damage and status writes. */
public final class RelayImpactTest implements FabricClientGameTest {
	private static boolean registered;
	private static ServerPlayer owner;
	private static Guest guest;
	private static Runnable onHit, afterDamage;
	private static Vec3 expectedOrigin;
	private static int incoming;
	private static final class Guest extends FakePlayer {
		Guest(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "RelayWitness")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
		@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			if (source instanceof RelayDamageSource) {
				incoming++;
				RelayCircleTest.check(source.getSourcePosition().distanceToSqr(expectedOrigin) < .0001, "Real damage source retains the accepted focus origin");
				RelayCircleTest.check(source.getEntity() == owner, "Remote origin retains real owner attribution");
			}
			return super.hurtServer(level, source, amount);
		}
	}
	@Override public void runTest(ClientGameTestContext context) {
		register();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40); world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				owner = RelayCircleTest.player(server); RelayCircleTest.prepare(owner, Runes.FROST);
				owner.level().getGameRules().set(GameRules.PVP, true, server);
				guest = new Guest(owner.level()); guest.setGameMode(GameType.SURVIVAL);
				guest.snapTo(.5,150,6.5,180,0); guest.setNoGravity(true); owner.level().addNewPlayer(guest);
				incoming=0;
			});
			context.waitTicks(3);
			for (int mode=0; mode<5; mode++) {
				int probe = mode;
				world.getServer().runOnServer(server -> {
					Parties.session(server).rules.clear(); onHit = afterDamage = null;
					guest.setHealth(guest.getMaxHealth()); guest.removeAllEffects(); guest.setTicksFrozen(0); guest.setDeltaMovement(Vec3.ZERO);
					RelayCircleTest.reset(owner, Runes.FROST); RelayCircleTest.directDown(owner);
					RelayCircleTest.check(RelayCircles.pending(owner), "Mutation fixture actually pays and places");
					expectedOrigin = owner.getAttached(RelayState.VIEW).focus();
					if (probe==1) onHit = RelayImpactTest::join;
					if (probe==2) afterDamage = RelayImpactTest::join;
					if (probe==3) onHit = () -> Spellbooks.setCord(owner, new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
					if (probe==4) afterDamage = () -> owner.teleportTo(server.getLevel(net.minecraft.world.level.Level.NETHER), .5,150,.5, java.util.Set.of(),0,0,false);
				});
				context.waitTicks(2);
				world.getServer().runOnServer(server -> { RelayCircleTest.aim(owner, guest.getBoundingBox().getCenter()); RelayCircleTest.downAfterUp(owner); });
				context.waitTicks(8);
				world.getServer().runOnServer(server -> {
					if (probe==0) RelayCircleTest.check(guest.getHealth()<guest.getMaxHealth() && guest.hasEffect(MobEffects.SLOWNESS) && guest.getTicksFrozen()>0, "Control release actually damages and freezes");
					if (probe==1 || probe==3) RelayCircleTest.check(guest.getHealth()==guest.getMaxHealth() && !guest.hasEffect(MobEffects.SLOWNESS) && guest.getTicksFrozen()==0, "SPELL_HIT mutation vetoes damage and all follow-up writes");
					if (probe==2 || probe==4) RelayCircleTest.check(guest.getHealth()<guest.getMaxHealth() && !guest.hasEffect(MobEffects.SLOWNESS) && guest.getTicksFrozen()==0, "Actual wound callback can retire permission before Frost's later status writes");
					RelayCircleTest.check(Effects.applying()==null && Effects.applyingCast()==null, "Paid impact scope is restored after nested callbacks");
					if (owner.level()!=server.overworld()) owner.teleportTo(server.overworld(), .5,150,.5, java.util.Set.of(),0,55,false);
				});
				context.waitTicks(13);
			}
			world.getServer().runOnServer(server -> {
				RelayCircleTest.check(incoming>=3, "Source-origin assertion observed genuine native damage calls");
				Parties.session(server).rules.clear(); onHit=afterDamage=null;
				guest.discard(); guest=null;
			});
			// Shock's secondary and its native reactions share the same paid receipt and current visibility.
			LivingEntity[] targets = new LivingEntity[2];
			world.getServer().runOnServer(server -> {
				for(int i=0;i<2;i++) {
					var target=EntityTypes.HUSK.create(owner.level(), EntitySpawnReason.MOB_SUMMONED); RelayCircleTest.check(target!=null,"Shock witness creates");
					target.snapTo(.5+i*2,150,6.5,180,0);target.setNoAi(true);target.setNoGravity(true);owner.level().addFreshEntity(target);targets[i]=target;
				}
				RelayCircleTest.reset(owner,Runes.SHOCK);RelayCircleTest.directDown(owner);
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {RelayCircleTest.aim(owner,targets[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});
			context.waitTicks(8);
			world.getServer().runOnServer(server -> RelayCircleTest.check(targets[0].getHealth()<targets[0].getMaxHealth()&&targets[1].getHealth()<targets[1].getMaxHealth(),"Normal Shock secondary actually travels when both sightlines permit"));
			context.waitTicks(12);
			world.getServer().runOnServer(server -> {
				for(var target:targets){target.setHealth(target.getMaxHealth());target.removeAllEffects();}
				for(int y=150;y<=153;y++)owner.level().setBlock(new BlockPos(1,y,6),Blocks.STONE.defaultBlockState(),2);
				RelayCircleTest.reset(owner,Runes.SHOCK);RelayCircleTest.directDown(owner);
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {RelayCircleTest.aim(owner,targets[0].getBoundingBox().getCenter());RelayCircleTest.downAfterUp(owner);});
			context.waitTicks(8);
			world.getServer().runOnServer(server -> RelayCircleTest.check(targets[1].getHealth()==targets[1].getMaxHealth(),"Secondary Shock cannot arc through new cover or around a hidden corner"));
		} finally {owner=null;guest=null;onHit=afterDamage=null;}
	}
	private static void join(){
		PartyRules rules=Parties.session(owner.level().getServer()).rules;long now=owner.level().getGameTime();
		RelayCircleTest.check(rules.invite(owner.getUUID(),guest.getUUID(),now)==PartyRules.Result.OK,"Callback creates real party invitation");
		RelayCircleTest.check(rules.accept(guest.getUUID(),owner.getUUID(),now)==PartyRules.Result.OK,"Callback accepts real membership");
	}
	private static void register(){
		if(registered)return;registered=true;
		WildercordEvents.SPELL_HIT.register((caster,targets,point,effects)->{if(caster==owner&&targets.contains(guest)&&onHit!=null){Runnable action=onHit;onHit=null;action.run();}});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,damage,blocked)->{if(target==guest&&source instanceof RelayDamageSource&&damage>0&&afterDamage!=null){Runnable action=afterDamage;afterDamage=null;action.run();}});
	}
}
