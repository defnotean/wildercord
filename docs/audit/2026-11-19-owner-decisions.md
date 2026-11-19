# Owner decisions — 2026-11-19

One page for the calls the polish loop cannot make on its own. Each item has the context, the
options as I see them, and what I would pick. Answer inline or in chat; implementation follows in
the next rounds with tests and receipts as usual.

## 1. Party leadership when the leader leaves for good

`party/Parties.java`, `PartyRules.java`. Today a party keeps its leader slot even if that player
never comes back; nobody can take over without a disband and re-form.

- **Promote automatically:** longest-serving online member becomes leader when the leader has been
  offline past a cutoff (say 3 days).
- **Vote or claim:** `/party claim` takes leadership after the same cutoff.
- **Leave it:** parties are casual; disband-and-reform is fine.

Pick: promote automatically, with the promotion announced in chat.

## 2. Offline members and the guild training bonus

`guild/Guilds.java`. The bonus scales with members near each other, so an offline member counts
for nothing — which makes big guilds feel smaller than they are.

- **Keep online-only** (honest numbers, current behaviour).
- **Count offline members at a reduced weight** (say half), capped.
- **Count everyone** and accept the grind-away of the incentive to log in together.

Pick: online-only, but show "3 of 9 online" on `/guild info` so the gap is visible.

## 3. Logging out of a duel

`duel/Duels.java`, `DuelRules.java`. A duelist who logs out mid-duel currently just… leaves; the
duel hangs until timeout.

- **Instant loss** on logout (harsh on lag/restarts, exploitable by ping-bombing).
- **Short grace:** 60 s to return, then the logout counts as a loss.
- **No contest:** void the duel, no rating change (invite griefing).

Pick: 60-second grace, then loss; both duelists see the timer.

## 4. Dungeon boss reset after a full-party wipe

The 0.14 boss rework gave staged deaths and clearer wind-ups; the audit asks whether a boss should
come back at full health when the whole party dies and re-enter later.

- **Full reset** (retry-friendly; bosses are encounters, not attrition walls).
- **Keep damage**, slow regen while unclaimed (rewards chip damage; risks third-party cheese).
- **Full reset after N minutes**, persisted damage before that.

Pick: full reset, with a short re-engagement cooldown so the wipe still costs something.

## 5. Masters of Tomorrow leftovers

From the archived roadmap (now `docs/archive/MASTERS_OF_TOMORROW_ROADMAP.md`):

- **Shared BIND reward** — decide what the parties get when a bind completes together.
- **Vow balance and the `/vow` UI** — the seven odd-circle vows work via commands; a UI is a
  screen of work. Decide: keep commands-only, or schedule the screen.
- **Stone Hinge** — still behind `aura.experimental_stone_hinge`. Decide: ship it, or cut the
  config and the code.
- **Tide / Iron / Dune poses** — articulated poses for the three newer signatures. Art work;
  decide whether it is on the 0.15 list or parked.

## 6. Keybinds

Four keybind categories (wildercord, masters_arts, master_forms, lore) and Master Form on **C**
clashes with vanilla's *Save Toolbar Activator* in creative.

- **Rebind Master Form** to e.g. `X` (players know C).
- **Merge the four categories** into one (cleaner options screen, loses grouping).
- **Leave both** as they are.

Pick: rebind to `X` and merge `master_forms` into `masters_arts`; keep `lore` separate.

## 7. Party bounty escorts

`town/BountyEscorts.java`. The escort bounty is strictly solo: the llama is tagged to one UUID,
only that player can start it, and a party member walking the llama home earns nothing (and can
even lose the bounty for the owner if the llama dies on their watch). The Ferryman fight went
cooperative this round; escorts are the other party-shaped activity.

- **Party-share the escort:** any party member may escort, arrival credits the owner (or splits
  coin), and the llama is lost only if no party member is around to save it.
- **Companion credit:** keep ownership solo but pay a small coin share to members within range at
  arrival.
- **Leave it solo:** escorts are a personal ledger; parties have the Ferryman and dungeons.

Pick: companion credit first (small change, no ownership rewrite), party-share later if players
actually run escorts in parties.
