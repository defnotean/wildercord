# Reed Rattle: physical wetland counterplay

The Reed Rattle is one new functional exploration/support tool. It adds no new
creature, ability, signature fusion, lore text or completed ecosystem.

## Admission and tradeoffs

Crouching with a rattle and interacting with a visible, living Reedback Crab within
three blocks answers its warning claws. A successful answer calms it for six seconds,
spends one of forty-eight uses and starts a saved twenty-second player rest. Different
hands, replacement instruments and relogging cannot bypass that player rest. The
crab shares its separate response deadline with magical answers.

Standing, blocked sight, distance, spectator mode, an existing rest and the wrong
attack phase refuse without spending durability or changing either deadline. A
committed sweep remains dangerous, and ordinary harm breaks calm. The tool uses
physical pebble percussion; it spends no mana and draws no magic circle.

## Artwork and acquisition

The crafted chamber has stepped clay courses, four diagonal beveled corners,
exposed river pebbles, reed nodes, floss ties and an olive chevron. Five original
material textures and a synthesized clay/pebble/fibre shake accompany it. The
recipe uses Moonreed Floss, two clay balls, two bamboo and string. The unlocked
recipe, inventory model and compact held instrument are covered by the native suite.

## Evidence

`ReedRattleTest` first passed in one minute. Actual screenshot review found a flowing
test pond that obscured the crab and an oversized held instrument. The fixture now
has an enclosed pond behind a dry bank; the model has beveled corners and a smaller
held transform. The final full suite passed in **58 seconds**.

The suite exercises the real interaction packet, loaded crafting recipe, actual
warning and committed sweep, standing/distance/blocked/spectator refusals, calm and
durability, replacement/offhand rests, shared magical response refusal, full world
shutdown/reopen and restored item cooldown, finite expiry, final-use break and harm
breaking calm. Opening the inventory released crouch; the fixture now restores and
synchronizes real client crouch before its admitted interaction.

Original inspected final captures and logs are under
`artifacts/review/parallel-living-world/reed-rattle`: `final-native.log`,
`reed_rattle_warning.png`, `reed_rattle_settled.png`, `reed_rattle_inventory.png`.
These show a controlled native bank, not naturally generated populations.

Actual two-client multiplayer, natural wetland encounter frequency, shader review
and sustained simulation measurements remain open. Shared response checks through
the authoritative creature hook do not prove a remote two-player encounter.
