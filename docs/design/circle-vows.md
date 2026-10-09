# Circle Vows

Roadmap Priority 2 asks that the climb from Circle VIII to XX add choices, not only mana, regeneration and power. Before this change seven of those thirteen circles gave nothing but the flat per-circle bonus. Each of them now asks for one bounded choice.

## Which circle gives what

| Circle | New thing |
|---|---|
| VIII | Archmage perk, Relay Circle lesson, Masters trials invitation |
| IX | Vow: Wellspring (+30 max mana) or Quickening (+1.5 regen/s) |
| X | Tollgate lesson (Tempered feat) |
| XI | Vow: Keen Edge (power x1.06) or Spare Hand (cost x0.94) |
| XII | Ebb Ledger / Reweave lesson (Low Tide feat) |
| XIII | Vow: Swift Hand (cooldown x0.93) or Long Echo (duration x1.15) |
| XIV | Lifeline lesson (Starbreaker feat) |
| XV | Vow: Overcharge (power x1.10, cost x1.05) or Austerity (power x0.97, cost x0.90) |
| XVI | Excise lesson (Heartwood feat) |
| XVII | Vow: Torrent (cooldown x0.90, duration x0.90) or Vigil (duration x1.25, cooldown x1.05) |
| XVIII | Conduit lesson (Grounded feat) |
| XIX | Vow: Reservoir (+60 mana, -0.5 regen) or Spring (+2.5 regen, -20 mana) |
| XX | Vow: Crown of Power (power x1.10) or Crown of Ease (cost x0.92, cooldown x0.95) |

`CircleVowsTest.everyCircleFromTheEighthOnGrantsAChoiceOrACapability` fails if a later change leaves a circle in VIII..XX with neither a lesson/perk nor a vow, or gives one both.

## Rules

- Source: `dev.wildercord.spell.CircleVows` (pure), `dev.wildercord.cast.CircleVowCommands` (player command and chat offer), `Heart.bonuses` and `Mana.max/regen` (application).
- Saved as one int attachment `wildercord:circle_vows`, two bits per vow at a fixed index. It is persistent, synced to its owner only and kept on death. Unknown bits and the malformed "both sides" pair are dropped on read (`CircleVows.clean`), so an edited save can never hold both sides.
- **Take**: free, only while the circle is formed and not cracked, and only if that circle has no vow yet.
- **Release (respec)**: `/vow release <circle>` costs 5 experience levels (free in creative). It works while the circle is cracked, so a vow is never stuck. Choosing again afterwards is free. This is the bounded respec: one vow at a time, paid, with no refund of anything else.
- **Active only**: a vow applies only while its circle is active. Cracking (overcasting) silences the outer vows with their circles; mending restores them.
- **Bounds**: the strongest possible stack is about x1.28 power, x0.78 cost, x0.80 cooldown or x1.44 duration on one axis, always paid for by giving up the other side. Mana moves at most +90 or -20; regeneration never goes below zero.
- **Old saves**: the attachment is absent, which reads as 0 (no vows). Nothing is granted automatically. A qualified heart sees the offer the next time it meditates (once per session) and can review all vows with `/vow`.
- **Party**: vows are personal; nothing a partner holds or has chosen affects you.

## Teaching

- When a vow circle forms, chat shows the two sides as clickable buttons with hover text.
- Meditating with an open vow shows the offer once per session.
- `/vow` lists every vow, the chosen side, whether it is silent, and a click-to-suggest Release button.
- The player wiki page `gitbook/progression/heart-circles.md` has the full table.

## Open owner decisions

- The balance numbers above are first-pass values.
- The interface is chat plus `/vow`. A Cord-screen panel would be more discoverable, but there is none yet.
