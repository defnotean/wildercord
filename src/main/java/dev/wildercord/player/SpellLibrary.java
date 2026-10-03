package dev.wildercord.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.content.CordTier;
import dev.wildercord.gear.Gear;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.*;

/** Twenty-four named builds, independent of currently equipped Cord spell slots. Loading is atomic. */
public final class SpellLibrary {
 private SpellLibrary() {}
 public record Build(String name,List<String> runes) {
  public Build { name=SpellNames.clean(name);runes=List.copyOf(runes); }
  public static final Codec<Build> CODEC=RecordCodecBuilder.create(i->i.group(Codec.string(1,32).fieldOf("name").forGetter(Build::name),
   Codec.string(1,Knots.MAX_ID_LENGTH).listOf(1,CordTier.MAX_SOCKETS).fieldOf("runes").forGetter(Build::runes)).apply(i,Build::new));
 }
 public static final int MAX_BUILDS=24;
 public static final Codec<List<Build>> CODEC=Build.CODEC.listOf(0,MAX_BUILDS);
 public static final AttachmentType<List<Build>> BUILDS=AttachmentRegistry.create(Wildercord.id("spell_library"),b->b.initializer(List::of).persistent(CODEC).syncWith(ByteBufCodecs.fromCodec(CODEC),AttachmentSyncPredicate.targetOnly()).copyOnDeath());
 public static void init() {}
 public static List<Build> list(Player p){return p.getAttachedOrElse(BUILDS,List.of());}
 public static int save(ServerPlayer p,String name,int slot) {
  if(slot<0||slot>=dev.wildercord.gear.SpellSlots.ALL)return fail(p,"message.wildercord.library_locked");
  name=SpellNames.clean(name);if(name.isBlank())return fail(p,"message.wildercord.library_name");
  var runes=Spellbooks.get(p).spells().get(slot);if(runes.isEmpty())return fail(p,"message.wildercord.library_empty");
  var next=new ArrayList<>(list(p));String key=name;
  next.removeIf(b->b.name.equalsIgnoreCase(key));if(next.size()>=MAX_BUILDS)return fail(p,"message.wildercord.library_full");
  next.add(new Build(name,runes));p.setAttached(BUILDS,List.copyOf(next));p.sendSystemMessage(Component.translatable("message.wildercord.library_saved",name));return 1;
 }
 public static int load(ServerPlayer p,String name,int slot) {
  if(slot<0||slot>=dev.wildercord.gear.SpellSlots.ALL)return fail(p,"message.wildercord.library_locked");
  var build=list(p).stream().filter(b->b.name.equalsIgnoreCase(name)).findFirst();if(build.isEmpty())return fail(p,"message.wildercord.library_missing");
  var tier=Spellbooks.tier(p);if(tier==null||!Gear.spellOpen(p,tier,slot))return fail(p,"message.wildercord.library_locked");
  if(build.get().runes.size()>tier.sockets)return fail(p,"message.wildercord.library_fit");
  // Validate every rune before modifying either the sequence or its name. Missing add-on runes remain saved.
  for(String id:build.get().runes) {
   if(!Spellbooks.knows(p,id))return fail(p,"message.wildercord.library_unknown");
   var rune=Runes.get(id);if(rune.isEmpty()||!tier.holds(rune.get().tier()))return fail(p,"message.wildercord.library_fit");
  }
  Spellbooks.set(p,Spellbooks.get(p).withSpell(slot,build.get().runes).withName(slot,build.get().name));
  p.sendSystemMessage(Component.translatable("message.wildercord.library_loaded",build.get().name,slot+1));return 1;
 }
 public static int delete(ServerPlayer p,String name) {
  var next=new ArrayList<>(list(p));if(!next.removeIf(b->b.name.equalsIgnoreCase(name)))return fail(p,"message.wildercord.library_missing");
  p.setAttached(BUILDS,List.copyOf(next));p.sendSystemMessage(Component.translatable("message.wildercord.library_deleted",name));return 1;
 }
 public static int show(ServerPlayer p) {p.sendSystemMessage(Component.translatable("message.wildercord.library_list",list(p).size()));
  for(var build:list(p))p.sendSystemMessage(Component.translatable("message.wildercord.library_row",build.name,build.runes.size()));return 1;
 }
 private static int fail(ServerPlayer p,String key){p.sendSystemMessage(Component.translatable(key).withColor(0xEFA0A0));return 0;}
}
