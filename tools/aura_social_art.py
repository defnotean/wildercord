"""Authored player-facing text for Aura social play."""
LANG = {}

def add(prefix, values):
    LANG.update({prefix + k: v for k, v in values.items()})

add('screen.wildercord.aura.', {'skills_tab': 'Skills', 'arts_tab': 'Arts'})
add('aura.wildercord.clash.', {'hint': 'Clash · strike on each beat', 'even': 'Evenly matched', 'won': 'You broke through', 'lost': 'Your blade gave way', 'grade.miss': 'Miss', 'grade.good': 'Good', 'grade.perfect': 'Perfect', 'grade.fumble': 'Fumble'})
add('aura.wildercord.lineage.', {'lesson': 'Learning from %s', 'rite': 'A bond with %s', 'cancel_hint': 'Stand or turn away to cancel', 'a_disciple': 'A disciple accepted', 'disciple_of': 'Disciple of', 'graduated': 'Standing together', 'graduated_master': 'A disciple graduates'})
add('aura.wildercord.spar.', {'countdown': 'Spar begins in %s', 'active': 'Sparring with %s', 'exit': 'Walk beyond the standards to concede', 'offer': '%s salutes you', 'accept': 'Sneak + use your blade on them to accept', 'result.1': 'Victory', 'result.2': 'Bested', 'result.3': 'Evenly matched', 'result.4': 'Spar called off', 'bested': 'Bested', 'even': 'Evenly matched', 'kicker': 'A spar with %s', 'victory': 'Victory'})
add('screen.wildercord.aura.lineage.', {'tab': 'Lineage', 'title': 'Masters & disciples', 'record': 'Spars: %s won · %s lost · %s even', 'master': 'Your master', 'release': 'Release', 'confirm': 'Confirm', 'lesson_ready': 'A lesson is ready', 'lesson_wait': 'Next lesson in %s min', 'no_master': 'You have no master', 'ask': 'Kneel before a breathing master to ask.', 'disciples': 'Disciples', 'honoured': 'Honoured', 'empty': 'No names recorded yet.', 'since': 'Together since day %s', 'previous': '<', 'next': '>', 'capacity': '%s / %s disciples', 'how': 'A settled breathing stance welcomes a kneeling disciple. Either may release a bond here; press Release twice.', 'off': 'Masters and disciples are disabled on this server.'})
add('command.wildercord.aura.lineage.', {'cant': 'Cannot form this bond: %s', 'no_bond': 'You have no bond with %s.', 'self': 'You cannot be your own master.', 'state': 'Master: %s · Disciples: %s'})
add('command.wildercord.aura.spar.', {'cant': 'Cannot begin this spar: %s', 'none': 'You are not sparring.', 'not_ready': 'Both swordsmen must be ready to begin.', 'record': '%s wins · %s losses · %s draws', 'with': 'Sparring with %s'})
add('message.wildercord.aura.lineage.', {
    'asks_disciple': '%s offers to become your master. Stay kneeling to accept; stand to decline.',
    'asks_master': '%s asks to become your disciple. Keep breathing to accept; release your stance to decline.',
    'bested': 'You bested your master, %s. Their trial is yours.', 'bested_master': 'Your disciple, %s, has bested you.',
    'bested_out': 'A master’s trial requires a knockout; leaving the spar does not count.',
    'broken': 'The ceremony was interrupted. No bond was made.', 'ended': 'Your bond with %s has ended.', 'ended_by': '%s has released your bond.',
    'graduated_disciple': 'You now stand beside %s. Your bond becomes an honoured memory.', 'graduated_master_line': '%s now stands beside you. Your disciple has graduated.',
    'lesson_begins': 'A lesson for %s begins.', 'lesson_begins_disciple': 'Your lesson with %s begins.', 'lesson_given': 'You taught %s: %s.', 'lesson_later': '%s may learn again in %s minutes.',
    'lesson_nothing': 'You practised beside %s; there is no new part to learn today.', 'lesson_nothing_master': '%s practised beside you; there is no new part to teach today.',
    'lesson_part': '%s taught you: %s.', 'lesson_way': '%s helped you find your crossroads.', 'manual': '%s gave you a %s manual. Read it if you choose to change your method.',
    'share': '%s reached %s. Their road taught you %s Aura experience.', 'share_waiting': 'Your disciples’ roads taught you %s Aura experience while you were away.',
    'taken_disciple': '%s is now your master.', 'taken_disciple_how': 'Kneel before your breathing master for a daily lesson. Nearby, your Aura experience grows faster.',
    'taken_master': '%s is now your disciple.', 'taken_master_how': 'Teach them while breathing. Their breakthroughs return a share of experience to you.',
    'trial_too_high': 'To stand beside %s, you must face a trial of the world.'})
add('message.wildercord.aura.spar.', {'begins': 'Your spar with %s begins.', 'called_off': 'The spar was called off. Both swordsmen are restored.', 'challenged': '%s salutes you and asks for a spar.', 'challenged_how': 'Sneak and use your blade on them to accept. The offer expires shortly.', 'counts': 'A spar with %s can still teach you today.', 'even': 'The spar ends evenly matched.', 'for_its_own_sake': 'You have learned all you can from %s today. This spar is for practice.', 'saluted': 'You salute %s. Wait for their answering salute.', 'taught': 'The spar taught you %s Aura experience (%s / %s today with this partner).', 'taught_nothing': 'Your spar with %s was too brief or one-sided to teach experience.', 'won': '%s bested %s.', 'won_out': '%s wins as %s leaves the grounds.'})
add('message.wildercord.aura.spar.refused.', {'off': 'Sparring is disabled.', 'no_method': 'Both swordsmen need a breathing method.', 'no_weapon': 'Both swordsmen must hold blades.', 'unhurtable': 'Creative or spectating players cannot spar.', 'too_far': 'Come closer, in the same world.', 'busy': 'One swordsman is already in a spar or duel.', 'awakened': 'Let the awakening end before sparring.', 'hurt': 'Both swordsmen need at least half their health.'})
add('message.wildercord.aura.lineage.refused.', {'off': 'Masters and disciples are disabled.', 'master_stage': 'A master must reach Form.', 'gap': 'A disciple must stand at least two stages below their master.', 'full': 'This master has no room for another disciple.', 'has_master': 'This disciple already has a master.', 'already': 'This bond already exists.'})
for reason, lines in {
    'hurt': ('Wait ten seconds after taking damage to spar.', '%s was hurt recently. Wait ten seconds.'),
    'pvp': ('Wait thirty seconds after fighting a player to spar.', '%s fought a player recently. Wait thirty seconds.'),
    'cooldown': ('Wait thirty seconds between bouts.', '%s finished a bout recently. Wait thirty seconds.'),
}.items():
    LANG['message.wildercord.aura.spar.not_ready.' + reason + '.self'] = lines[0]
    LANG['message.wildercord.aura.spar.not_ready.' + reason + '.other'] = lines[1]

add("toast.wildercord.aura.", {"spar": "A swordsman's spar", "lineage": "Masters and disciples", "clash": "A clash of blades"})

LANG["screen.wildercord.aura.lineage.graduated"] = "Graduated on day %s"

LANG["message.wildercord.aura.lineage.lesson_broken"] = "The lesson was interrupted. No new part was taught."
LANG["message.wildercord.aura.spar.taught_nothing"] = "Your spar with %s was practice; the daily experience limit is reached."
