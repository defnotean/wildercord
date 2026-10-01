package dev.wildercord.cast;

import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Knots;
import dev.wildercord.spell.WovenRunes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** A repeatable, paid way to put the owner's awakened innate into an exact fusion. */
public final class SoulWeaving {
	private SoulWeaving(){}
	public static boolean owns(ServerPlayer player,List<RuneDef> runes){
		return player.isCreative() || Knots.flatten(runes).stream()
			.flatMap(r->WovenRunes.isWoven(r)?WovenRunes.contents(r).stream():java.util.stream.Stream.of(r))
			.noneMatch(r->Runes.innate(r)&&!Heart.innate(player).equals(r.id()));
	}
	public static void imprint(ServerPlayer player,ItemStack blank){
		var innate=Runes.get(Heart.innate(player));
		if(innate.isEmpty()||!Runes.innate(innate.get())){player.sendOverlayMessage(Component.literal("Awaken your first Heart Circle before imprinting an innate rune."));return;}
		if(!blank.is(WildercordItems.BLANK_RUNE))return;
		if(!player.hasInfiniteMaterials()&&player.experienceLevel<3){player.sendOverlayMessage(Component.literal("Imprinting your innate costs a Blank Rune and three XP levels."));return;}
		if(!player.hasInfiniteMaterials())player.giveExperienceLevels(-3);
		blank.consume(1,player);ItemStack imprint=RuneItem.stack(innate.get());
		if(!player.getInventory().add(imprint))player.drop(imprint,false,net.minecraft.util.Prediction.SERVER_ONLY);
		player.sendOverlayMessage(Component.literal("Imprinted "+innate.get().name()+". Weave it with an elemental rune and an amethyst block."));
		Vfx.emit(player.level(),SpellMaterials.of(innate.get().element(),Vfx.theme(innate.get().element()).primary(),.16F),player.position().add(0,.6,0),8,.4,.02);
	}
}
