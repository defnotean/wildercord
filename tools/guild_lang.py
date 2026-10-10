"""0.13 guilds and covens: the words for /guild and /coven. The rules live in src/main/java/dev/wildercord/guild/GuildRules.java."""

LANG = {}

for kind, Kind, who in (('guild', 'Guild', 'swordsmen'), ('coven', 'Coven', 'mages')):
    p = f'message.wildercord.{kind}.'
    LANG.update({
        p + 'none': f"You aren't in a {kind}. Found one with /{kind} found <name> (%s emeralds), or ask a {kind} leader to invite you.",
        p + 'show': f'{Kind}: %s (%s/%s members)',
        p + 'near': f'Fellow members close by: %s (training counts +%s%%)',
        p + 'founded': f'You founded the {kind} %s. Invite others with /{kind} invite <player>.',
        p + 'invited': f'%s invited you to the {kind} %s. The invitation lasts a minute.',
        p + 'accept_button': '[Join]',
        p + 'invite_sent': f'{Kind} invitation sent to %s.',
        p + 'joined': f'%s joined the {kind} %s.',
        p + 'left': f'You left the {kind} %s.',
        p + 'member_left': f'%s left the {kind}. It is led by %s.',
        p + 'kicked': f'%s was removed from the {kind}.',
        p + 'you_were_kicked': f'You were removed from the {kind} %s.',
        p + 'disbanded': f'The {kind} %s has been disbanded.',
        p + 'refuse.bad_name': 'A name takes 3 to 24 letters, digits, spaces, apostrophes or hyphens.',
        p + 'refuse.name_taken': 'A guild or coven already goes by that name.',
        p + 'refuse.already_in': f"That's one {kind} already: leave it first.",
        p + 'refuse.not_in': f"You aren't in a {kind}.",
        p + 'refuse.not_leader': f'Only the {kind} leader can do that.',
        p + 'refuse.full': f'The {kind} is full (%2$s members).',
        p + 'refuse.no_such': f'That {kind} is gone.',
        p + 'refuse.not_member': f"No member of your {kind} goes by that name.",
        p + 'refuse.self': "You can't do that to yourself.",
        p + 'refuse.cost': f'Founding a {kind} takes %s emeralds.',
        p + 'refuse.no_invite': f'You have no {kind} invitation waiting.',
        p + 'refuse.eligible': f'A {kind} is for {who}.' + (' Learn a breathing method first.' if kind == 'guild' else ' Wear a Cord first.'),
        p + 'refuse.their_eligible': f'A {kind} is for {who}, and they are not one yet.',
    })


def write(g):
    pass
