package dev.wildercord.aura;

import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.content.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

/** Anvil consumption, actual art strikes, native harmful/helpful effects, payment and swap refusal. */
public final class RuneEtchingsTest implements FabricClientGameTest {
	private static LivingEntity foe;
	@Override public void runTest(ClientGameTestContext c) {
		try(var w=c.worldBuilder().create()) {
			c.waitTicks(35); w.getServer().runCommand("gamerule spawn_mobs false"); w.getServer().runCommand("time set 6000");
			w.getServer().runOnServer(s -> {
				for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlock(new BlockPos(x,100,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
				var p=p(s);p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,-3,Set.<Relative>of(),0,0,false);
				p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("gale",AuraRules.GLOW,0,100,0));
				Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.setMana(p,100);
				var blade=new ItemStack(Items.DIAMOND_SWORD);blade.setDamageValue(17);blade.set(DataComponents.CUSTOM_NAME,Component.literal("The Seamkeeper"));
				var runes=RuneItem.stack(Runes.HARM);runes.setCount(3);p.experienceLevel=20;
				var menu=new AnvilMenu(91,p.getInventory(),ContainerLevelAccess.NULL);
				menu.getSlot(0).set(blade);menu.getSlot(1).set(runes);
				var output=menu.getSlot(2).getItem();check(!output.isEmpty(),"Anvil makes inscription");
				check(Runes.HARM.id().equals(output.get(RuneEtchings.RUNE)),"Effect stored");
				check(output.getDamageValue()==17 && output.getHoverName().getString().equals("The Seamkeeper"),"Name and wear preserved");
				check(menu.getCost()==5,"Five experience levels");
				menu.clicked(2,0,ContainerInput.PICKUP,p);
				check(p.experienceLevel==15,"Real result pickup charges levels");
				check(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount()==2,"One blade and one rune consumed");
				p.setItemInHand(InteractionHand.MAIN_HAND,menu.getCarried());menu.setCarried(ItemStack.EMPTY);
				check(RuneEtchings.etch(p.getMainHandItem(),RuneItem.stack(Runes.HARM),p).isEmpty(),"Duplicate inscription rejected");
				check(RuneEtchings.etch(p.getMainHandItem(),RuneItem.stack(Runes.TOUCH),p).isEmpty(),"Shape rejected");
				check(RuneEtchings.etch(p.getMainHandItem(),RuneItem.stack(Runes.HARM,2),p).isEmpty(),"Ranked rune preserved instead of discarded");
				foe=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);foe.setHealth(200);((net.minecraft.world.entity.Mob)foe).setNoAi(true);foe.snapTo(.5,101,1,180,0);s.overworld().addFreshEntity(foe);
				float before=Spellbooks.mana(p);ArtKit.hits(p,AuraFx.art(p)).raw(foe,4,null);
				check(foe.getHealth()<196,"Inscription deals native Harm beyond the art hit");
				check(Spellbooks.mana(p)==before-RuneEtchingRules.price(Runes.HARM),"Actual art charges exactly one effect");
				check(p.getAttachedOrElse(RuneEtchings.READY,0L)>s.overworld().getGameTime(),"Rest saved on player");
				check(!RuneEtchings.wake(p,foe,4),"No immediate repeat");
			});
			c.waitTicks(5);shot(c,"etched_blade_first_person");
			w.getServer().runOnServer(s -> {
				var p=p(s);p.setAttached(RuneEtchings.READY,0L);Spellbooks.setMana(p,0);
				check(!RuneEtchings.wake(p,foe,4),"No unpaid rune");check(p.getAttachedOrElse(RuneEtchings.READY,0L)==0,"Failed payment spends no cooldown");
				Spellbooks.setMana(p,100);p.setHealth(10);
				p.setItemInHand(InteractionHand.MAIN_HAND,RuneEtchings.etch(p.getMainHandItem(),RuneItem.stack(Runes.HEAL),p));
				check(RuneEtchings.wake(p,foe,4) && p.getHealth()>10,"Helpful inscription goes to swordsman");
				float before=Spellbooks.mana(p);p.setItemInHand(InteractionHand.MAIN_HAND,RuneEtchings.etch(p.getMainHandItem(),RuneItem.stack(Runes.FIRE),p));
				check(!RuneEtchings.wake(p,foe,4) && Spellbooks.mana(p)==before,"Swapping inscriptions cannot reset rest");
				p.setAttached(RuneEtchings.READY,0L);
				var hits=ArtKit.hits(p,AuraFx.art(p));p.setItemInHand(InteractionHand.MAIN_HAND,p.getMainHandItem().copy());
				foe.damageCooldownTime=0;hits.raw(foe,4,null);
				check(Spellbooks.mana(p)==before,"Delayed art cannot borrow a replacement blade");
				check(!RuneEtchings.wake(p,foe,Float.NaN) && !RuneEtchings.wake(p,foe,0),"Invalid or blocked damage cannot wake rune");
				// A native shield on the target screens the inscription, while its paid activation still rests.
				p.setItemInHand(InteractionHand.MAIN_HAND,RuneEtchings.etch(p.getMainHandItem(),RuneItem.stack(Runes.HARM),p));
				var shield=dev.wildercord.spell.SpellCompiler.compile(java.util.List.of(Runes.SELF,Runes.SHIELD));
				dev.wildercord.cast.Effects.apply(new dev.wildercord.cast.Cast(foe).weigh(24),shield.root().groups.getFirst().effects.getFirst(),
					new dev.wildercord.cast.Cast.Hit(java.util.List.of(foe),foe.position(),p.getLookAngle(),foe.position(),null,null,true));
				float health=foe.getHealth();foe.damageCooldownTime=0;
				check(RuneEtchings.wake(p,foe,4) && foe.getHealth()==health,"Shield stops actual inscription harm");
				// Round trip the complete stack through the registry-aware persistent ItemStack codec.
				var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,s.registryAccess());
				var saved=ItemStack.CODEC.encodeStart(ops,p.getMainHandItem()).getOrThrow();
				var loaded=ItemStack.CODEC.parse(ops,saved).getOrThrow();
				check(ItemStack.isSameItemSameComponents(loaded,p.getMainHandItem()),"Inscription/name/wear survive stack serialization");
				var repair=new AnvilMenu(92,p.getInventory(),ContainerLevelAccess.NULL);
				repair.getSlot(0).set(loaded.copy());repair.getSlot(1).set(new ItemStack(Items.DIAMOND));
				check(!repair.getSlot(2).getItem().isEmpty() && repair.getSlot(2).getItem().getDamageValue()<17
					&& Runes.HARM.id().equals(repair.getSlot(2).getItem().get(RuneEtchings.RUNE)),"Normal repair retains inscription");
				repair.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));repair.getSlot(1).set(loaded.copy());
				check(repair.getSlot(2).getItem().isEmpty(),"Inscribed weapon cannot be silently consumed as repair sacrifice");
			});
			c.waitTicks(10);shot(c,"etched_blade_shield");
			w.getServer().runOnServer(s -> {
				var p=p(s);var at=new BlockPos(1,101,-3);s.overworld().setBlock(at,Blocks.ANVIL.defaultBlockState(),2);
				p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,who) -> new AnvilMenu(id,inv,
					ContainerLevelAccess.create(s.overworld(),at)),Component.literal("Rune Etching")));
				p.containerMenu.getSlot(0).set(p.getMainHandItem().copy());p.containerMenu.getSlot(1).set(RuneItem.stack(Runes.FIRE));
				p.containerMenu.broadcastChanges();
			});
			c.waitTicks(10);shot(c,"etched_blade_anvil");
		}
	}
	private static ServerPlayer p(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
	private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
	private static void shot(ClientGameTestContext c,String name){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
}
