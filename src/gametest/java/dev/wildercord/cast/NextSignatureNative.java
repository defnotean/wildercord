package dev.wildercord.cast;
import dev.wildercord.content.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Shared native fixture only; the spells keep independent mechanics and material recipes. */
final class NextSignatureNative {
 private NextSignatureNative(){}
 static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 static void teach(ServerPlayer p){p.setGameMode(GameType.SURVIVAL);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
  var book=Spellbooks.get(p).withStarterGiven();for(var rune:Runes.all())book=book.learn(rune.id());Spellbooks.set(p,book);}
 static void floor(ServerPlayer p){for(var at:BlockPos.betweenClosed(new BlockPos(-12,100,-12),new BlockPos(20,100,20)))p.level().setBlock(at,Blocks.STONE_BRICKS.defaultBlockState(),2);teach(p);pose(p,.5,.5,0);}
 static void pose(ServerPlayer p,double x,double z,float pitch){p.teleportTo(p.level(),x,101,z,Set.<Relative>of(),0,pitch,false);}
 /** Native collision with the fixture floor establishes grounding; no onGround flag is injected. */
 static void ground(ServerPlayer p){
  double before=p.getY();p.move(MoverType.SELF,new Vec3(0,-.125,0));
  check(p.onGround() && Math.abs(p.getY()-before)<.001,"Ordinary downward native movement meets the solid fixture floor: "+p.getScoreboardName());
 }
 static void cast(ServerPlayer p,RuneDef shape,RuneDef rune){
  check(SpellCaster.edit(p,0,List.of(shape.id(),rune.id()))==null,"Actual spell editor accepts "+rune.path());
  Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);
  check(Spellbooks.mana(p)<before,"Actual Survival cast pays "+rune.path());
 }
 static Mob foe(ServerPlayer p,double x,double z){var t=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);
  check(t instanceof Mob,"Husk fixture");var mob=(Mob)t;mob.setNoAi(true);mob.snapTo(x,101,z,0,0);p.level().addFreshEntity(mob);return mob;}
 static ServerPlayer guest(ServerPlayer p,String name,boolean ally){
  check(name.length()<=16,"Native player profile name fits packet limit: "+name);
  var guest=new FakePlayer(p.level(),new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name)){
   @Override public net.minecraft.world.scores.PlayerTeam getTeam(){return level().getScoreboard().getPlayersTeam(getScoreboardName());}
   @Override public boolean isInvulnerableTo(ServerLevel level,net.minecraft.world.damagesource.DamageSource source){return false;}
  };
  teach(guest);guest.snapTo(.5,101,3.5,180,0);p.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(guest)));p.level().addNewPlayer(guest);ground(guest);
  if(ally){var scoreboard=p.level().getScoreboard();var team=scoreboard.getPlayerTeam("nextsignature_allies");if(team==null)team=scoreboard.addPlayerTeam("nextsignature_allies");
   team.setAllowFriendlyFire(false);scoreboard.addPlayerToTeam(p.getScoreboardName(),team);scoreboard.addPlayerToTeam(guest.getScoreboardName(),team);}
  return guest;
 }
 static Arrow arrow(ServerPlayer p,LivingEntity owner,boolean front){
  var arrow=new Arrow(p.level(),owner,new ItemStack(Items.ARROW),null);var at=p.getEyePosition().add(0,0,front?2.1:-2.1);
  arrow.setPos(at);arrow.setDeltaMovement(0,0,front?-1:1);arrow.setNoGravity(true);p.level().addFreshEntity(arrow);return arrow;
 }
 static void apply(Cast c,RuneDef rune,List<LivingEntity> targets,Vec3 at){
  var node=SpellCompiler.compile(List.of(Runes.SELF,rune)).root().groups.getFirst().effects.getFirst();
  Effects.apply(c,node,new Cast.Hit(new ArrayList<Entity>(targets),at,new Vec3(0,0,1),c.caster.position(),null,net.minecraft.core.Direction.UP,false));
 }
 static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
