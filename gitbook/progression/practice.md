# Spell practice and visual diagnostics

Players can use `/runelab practice enter` and `/runelab practice leave` to visit and leave the dedicated practice dimension. Operators can also use `/wildercord practice enter` and `/wildercord practice leave`, with the additional target and diagnostic commands below. The arena uses ordinary spell, mana, loadout, and defence rules. Entering does not replace your equipment, teach runes, or change your game mode. Leaving restores your saved departure point, including after a logout in the arena.

- `reset`: replace practice targets with three stationary dummies.
- `moving`: use three dummies that move from side to side.
- `stress <1-24>`: create up to 24 moving dummies.
- `benchmark`: reset server visual counters, then report them after ten seconds of casting.
- `/wildercord visualstats`: inspect particle packet construction, delivery counts, limited decorations, waiting scheduled parts, and compiled plan cache activity.
- `/wildercord visualstats reset`: clear the visual counters.

Dummy nameplates display the existing five-second damage rate and burst total. These are damage measurements against the dummy, rather than estimates of damage against an equipped player. The benchmark counters cover the server as a whole; other players casting during the sample also contribute. They do not measure GPU frame time or promise a frame-rate improvement.

The room loads eight chunks in its own dimension. It does not build over your normal world. Fallen players are returned to the platform. Practice target replacement is disabled outside this dimension. The first launch after installing the feature must load its new dimension; an already running server needs a restart.

In Minecraft Controls, assign a key to **Magic visual settings**. Your own formation effects and other casters' formations each have Full, Balanced, and Minimal options. Reduced flash dims shaped light, while Camera motion controls Wildercord shake and field-of-view punches. Combat geometry and warning circles remain present. Settings are local in `config/wildercord-visuals.json`.

The compiler retains at most 256 recent plans. Rune ranks are part of the key, each caller receives a separate mutable graph, and `/wildercord reload` and server shutdown clear the cache.

## Optional Survival trials


Enter and leave with `/runelab practice enter` and `/runelab practice leave`. In Survival, `/runelab trial precision`, `variety` or `fusion` begins a ninety-second attempt with a total spell budget of 100 mana.

* **Precision:** make five casts and land five spell hits on practice dummies, with each hit at most six damage.
* **Variety:** cast four different shapes and land five dummy spell hits.
* **Fusion:** cast three different named or woven fusions and land five dummy spell hits.

Actual magic/indirect-magic damage on dummies counts; melee punches do not. Leaving the practice dimension, death, Creative/Spectator mode, time expiry or exceeding the budget ends an attempt. The first completion of each trial awards one Torn Page; repeat runs still announce success but cannot farm rewards. These trials never gate ordinary rune progression.
