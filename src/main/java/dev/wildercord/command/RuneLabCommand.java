package dev.wildercord.command;
import dev.wildercord.player.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.*;

/** Normal player progression tools, separate from privileged administrative commands. */
public final class RuneLabCommand {
 private RuneLabCommand() {}
 public static void init() {
  SpellLibrary.init();RuneResearch.init();dev.wildercord.cast.SpellTrials.init();
  CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(Commands.literal("runelab")
   .then(Commands.literal("research").executes(c->RuneResearch.show(c.getSource().getPlayerOrException())))
   .then(Commands.literal("notebook").executes(c->dev.wildercord.net.NotebookPayload.open(c.getSource().getPlayerOrException())))
   .then(Commands.literal("practice")
    .then(Commands.literal("enter").executes(c->dev.wildercord.cast.PracticeRoom.enter(c.getSource().getPlayerOrException())))
    .then(Commands.literal("leave").executes(c->dev.wildercord.cast.PracticeRoom.leave(c.getSource().getPlayerOrException()))))
   .then(Commands.literal("trial").then(Commands.argument("challenge",StringArgumentType.word()).suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("precision","variety","fusion"),b))
    .executes(c->dev.wildercord.cast.SpellTrials.start(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"challenge")))))
   .then(Commands.literal("familiar").then(Commands.argument("role",StringArgumentType.word())
    .suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.List.of("companion","scout","guardian","gardener"),b))
    .executes(c->dev.wildercord.familiar.FamiliarRoles.choose(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"role")))))
   .then(Commands.literal("builds").executes(c->SpellLibrary.show(c.getSource().getPlayerOrException()))
    .then(Commands.literal("save").then(Commands.argument("name",StringArgumentType.string()).then(Commands.argument("slot",IntegerArgumentType.integer(1,5))
     .executes(c->SpellLibrary.save(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"name"),IntegerArgumentType.getInteger(c,"slot")-1)))))
    .then(Commands.literal("load").then(Commands.argument("name",StringArgumentType.string()).then(Commands.argument("slot",IntegerArgumentType.integer(1,5))
     .executes(c->SpellLibrary.load(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"name"),IntegerArgumentType.getInteger(c,"slot")-1)))))
    .then(Commands.literal("delete").then(Commands.argument("name",StringArgumentType.string())
     .executes(c->SpellLibrary.delete(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"name"))))))));
 }
}
