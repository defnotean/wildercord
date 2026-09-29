# Loadouts

Saved Cord setups a player can save, name, load and delete: from the Cord screen's panel, a
quick-switch key, or `/loadout`. The code is in `dev.wildercord.loadout`, wired up by
`Loadouts.init()` from `Wildercord.onInitialize`; the client side is `client.LoadoutPanel`, drawn by
`CordScreen`, and the key in `WildercordKeys`. The player guide is
[wiki/spellcraft/loadouts.md](../../wiki/spellcraft/loadouts.md).

## The pieces

| Class | What it is |
|---|---|
| `LoadoutRules` | Pure rules, unit-tested (`LoadoutRulesTest`): `MAX` (6), `MAX_NAME` (24), `name`, `find`, `save` (new, replace or full), `block` (what stops a load), `next` (the quick switch), `afterDelete`, `readyAfterLoad` (cooldowns). |
| `Loadout` | One saved setup: a name, every spell row (`SpellSlots.ALL` of them, the tome's included), the spells' custom names, the passives, `passivesOff` and `selected`. Sized exactly like `Spellbook` (via `Spellbook.sized`). `Loadout.of(name, book)` takes one from a spellbook; `applyTo(book)` puts it back. |
| `LoadoutData` | A player's list and `current` (the one last saved or loaded, -1 for none). Immutable, like `Spellbook`. |
| `Loadouts` | The runtime: the `wildercord:loadouts` attachment (`DATA`), and `save`, `saveOver`, `rename`, `delete`, `load`, `next`, each returning a `Result(ok, message)` the caller shows. |
| `LoadoutCommands` | `/loadout save <name>`, `load <name>`, `delete <name>`, `list` (and `/loadout` alone), for everyone. Names are greedy strings (spaces allowed), suggested from the player's own. |

The attachment is persistent, synced to its player only (the panel reads it), and copied on death.

## Trust

The client never sends runes. `WildercordNetworking.LoadoutRequest(kind, index, name)` carries a
kind (`Loadouts.SAVE_NEW`, `SAVE_OVER`, `LOAD`, `RENAME`, `DELETE`, `NEXT`), an index into the
player's list and, for saving as new and renaming, a name (at most 64 characters on the wire, cleaned
to 24 by `LoadoutRules.name`). It goes through the same `PacketThrottle` as every other spellbook
rewrite (a burst of 20, then 10 a second). The server:

- **saves its own spellbook** (`Loadout.of(name, Spellbooks.get(player))`), so a loadout can only
  hold what the spellbook held, and that only ever held what `SpellCaster.edit` let in;
- checks the index (`gone` if it's not there any more), the name (`bad_name`, `name_taken`) and the
  limit (`full`; saving over one, by index or by name, is always allowed);
- **loads by rearranging rune ids only.** `Loadout.applyTo` keeps the spellbook's `learned` list as it
  is, so every rule that decides what fires still decides it: `SpellCaster.activeSockets` (learned,
  loaded, a tier the Cord holds, inside its sockets, a row it has) and `PassiveCaster.activeRunes`.
  Runes that fail them stay threaded but quiet, exactly as after changing to a smaller Cord, and wake
  when the rune is learned or a bigger Cord is worn. A modified client can't load anything into a
  working state that it couldn't thread.

More than `MAX` loadouts in a player's data (a server that lowered the limit) are kept; only saving a
new one past it is refused.

## Loading

`Loadouts.load(player, index)`:

1. `blocked(player)`: `LoadoutRules.block` over a worn Cord, alive and not a spectator, not charging
   (`WildercordAttachments.CHARGE`), not in a duel (`Duels.inDuel`, countdown included) and not sealed
   in a Cryostasis (`SpellCaster.sealed`, which asks `FusedFrostWards`). Each has its own message.
2. The firing runes of every slot before and after (`SpellCaster.activeRunes` on the old and new
   spellbook), and each slot's cooldown as a cast would work it out (`Heart.cooldownTicks` with the
   secret-spell factor; 0 for nothing to cast).
3. `LoadoutRules.readyAfterLoad`: a slot whose firing runes changed, and that isn't cooling down
   already, is ready at `now + cooldown`; every other slot keeps its time. Swapping loadouts can't
   reset or shorten a cooldown, and a spell swapped in has to wait as if just cast.
4. `Spellbooks.set` and `Spellbooks.setReadyAt` for the slots that changed; `current` becomes the
   index. Passives need nothing extra: `PassiveCaster` charges a changed passive's first second as it
   starts.
5. The answer names the loadout and, if any runes stay quiet (`Loadouts.quiet`: threaded runes that
   `activeSockets` / `PassiveCaster.activeRunes` leave out), how many.

`next` loads `LoadoutRules.next(current, size)` and answers with `switched` ("Loadout: Mining (2 of 4)").

## The client

`LoadoutPanel` is a compact panel over the `CordScreen` window, in its local space and with its
sprites (`cord/panel`, `cord/inset`, `cord/row`, `cord/row_selected`, `cord/tab`, `cord/tab_active`).
`CordScreen` draws its badge (three bars, on `hud/badge`) at the end of the family tabs row under the
help badge, which cost the search box 16 pixels (`SEARCH_W` 74); the header had no room left on a
Copper or Amethyst Cord. `Ctrl`+`L` also opens it. While it's open the window behind is drawn with the
mouse far away (no hovers) and dimmed, and the panel takes every click, key and character: a click
outside closes it, typing never reaches the search.

Rows: the number (cyan for `current`), the name, up to five icons of the selected spell (or the first
spell with runes), then Load, Save current here (⇩), Rename (✎) and Delete (✕). Save here and Delete
ask for a second click within 3 seconds. The first empty row is "+ Save current as new", which offers
"Loadout N" as the name. Names being typed are checked on the client too (empty, taken, full) and the
refusal shown under the list; the server's answers come above the hotbar.

After Load, the panel remembers the spellbook it was sent from and closes; `CordScreen.tick` asks
`LoadoutPanel.loaded(book)` and, once a different spellbook has synced, re-reads its rows
(`readBook`), so the screen shows and edits the loaded spells. If nothing changes within 3 seconds
(refused, or identical), it stops waiting.

`WildercordKeys` registers **Next loadout** (`key.wildercord.next_loadout`, unbound), which sends
`LoadoutRequest(NEXT)` while no screen is open.

Test hooks on `CordScreen`: `loadoutsPoint()`, `loadoutsOpen()`, `loadoutPoint(row, button)` and
`saveNewLoadoutPoint()`.

## Text

Every string is in `LOADOUT_LANG` in `tools/generate_assets.py`: `message.wildercord.loadout.*` for
the server's answers, `screen.wildercord.loadouts.*` for the panel and badge, and
`key.wildercord.next_loadout`.

## Tests

- `src/test/.../loadout/LoadoutRulesTest`: names, the limit, the blocks, the quick switch, deleting,
  cooldowns, and a loadout applied to a spellbook with unknown runes, a Twine Cord, locked rows and
  oversized data (checked with the real `SpellCaster.activeSockets`).
- `src/gametest/.../WildercordLoadoutsTest`: the panel driven with the mouse and keyboard (save as
  new, rename, load back, the screen showing the loaded spells), a forgotten rune and an Echo loadout
  on a Twine Cord loading quiet, cooldowns (changed, unchanged, already cooling, and a real cast
  refused), refused while charging (server and packet), the limit of six and deleting twice, the
  quick switch's order, and the screenshots `loadouts_panel` and `loadouts_panel_854x480` at GUI
  scale 2.
