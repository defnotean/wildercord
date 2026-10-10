# Sparring and lineage


## What it is

Two ways to grow with other players. A **spar** is a safe practice fight. A **master and disciple** bond lets a strong
swordsman teach a newer one over time. Both start with a deliberate gesture, and either player can leave.

## Sparring

### How to start

Hold a blade, **sneak and use it on the other player** to salute them. They accept by saluting back within **20 seconds**.
A normal attack never accepts.

Both players need a breathing method, a blade, at least half their health, and a short break from combat: ten seconds after
taking damage, thirty seconds after fighting a player or finishing a bout. Creative and spectator players can't spar. End any
awakening first.

### How it works

Four cloth standards mark the ring. After a **three-second count-in**, fight with blades and Aura. Spells, projectiles and
pets can't hurt your partner.

| How it ends | Result |
|---|---|
| Bring your partner to one heart | A safe knockout; nobody dies |
| Walk out of the ring | You concede |
| Two minutes pass | A draw |
| Someone else hurts you | The spar is called off |

Afterwards, damage and harmful effects from your partner are undone and spent aura is refunded. Momentum resets, and an
awakening ends.

A spar gives experience only after **ten seconds** of fighting in which both players landed a hit. Up to **three spars per
partner per Minecraft day** give experience. Spars don't grow bonded blades, technique ranks or mastery.

## Clashes

When two Aura Slashes meet, or certain opposing arts collide, they can **lock**. A small rhythm panel above the hotbar shows
three beats.

**Press Attack once on each beat.** A perfect press scores 2, a good press 1, a miss 0. Extra presses lose a point, so
don't spam. The higher score wins and its strike carries on at **80%** damage. A tie breaks both.

The Way of the Blade gets slightly wider timing windows and wins ties against fighters without that edge.

![Two Aura crescents locked ahead of the swordsman, with a compact three-beat rhythm panel above the hotbar and a clear crosshair](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/aura-clash-rhythm.png)

## Masters and disciples

### How to get it

- A **master** must have reached **Form**.
- A **disciple** must be at least **two stages below** the master, or not have started Aura yet.
- A master can have **three** disciples at once. A disciple has one master.

The master **holds a blade, sneaks and stands still** until the breathing stance settles. The disciple **kneels** (sneaks)
close in front, facing the master. Both hold still through the **eight-second** ceremony. Standing up, turning away or
breaking the stance cancels it.

A disciple with no breathing method learns the master's. A disciple with a different method gets its manual and can choose.

### How to use it

- **Stay close:** within 24 blocks, the disciple gains normal Aura experience **25% faster**.
- **Take a lesson:** kneel before your master again. A five-second lesson teaches one technique part they know and you
  don't, helps you find your crossroads, or counts as practice. One lesson per Minecraft day.
- **Beat your master:** a knockout in a spar can count as a waiting breakthrough trial for a stage below your master's.
- **Give back:** each of your breakthroughs gives your master **25%** of the experience, saved for them if they're offline.
- **Receive their blade:** a master can [pass their bonded blade](bonded-blade.md#passing-it-on)
  to you.
- **Graduate:** when you reach your master's stage, the bond ends with honour and their record remembers you.

## The Lineage tab

On the Aura page, the **Lineage** tab shows your master, your next lesson, your disciples, graduates, and your spar wins,
losses and draws. Gold marks people nearby, green marks people online.

To end a bond, press **Release** and then **Confirm** within five seconds. Either player can do it, even if the other is offline.

![The Lineage tab shows a nearby disciple, spar records, lesson information, honoured records and a Release control](https://raw.githubusercontent.com/defnotean/wildercord/main/wiki/assets/images/aura-lineage.png)

## Server settings

`aura` section: `sparring`, `spar_ring_radius`, `spars_per_day`, `spar_xp`, `mentorship`, `max_disciples`, `disciple_gain`,
`master_share`, `clashes`, `clash_carry`. Turning a feature off keeps saved records.

See also: [Aura](aura.md) · [Ways](ways.md) ·
[Techniques](techniques.md)

Mages have their own version: see [Mentoring](../social/mentoring.md).
