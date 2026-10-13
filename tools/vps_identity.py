"""Explicit VPS provenance extension; historical GitHub identities stay separate."""
import re

BASE_FIELDS = {"headSha", "checkoutSha", "workflowSha", "runId", "runAttempt", "job"}
VPS_FIELDS = {"executor", "computer"}


def extension(executor=None, computer=None):
    return {} if executor is None and computer is None else {"executor": executor, "computer": computer}


def is_vps(identity):
    fields = set(identity)
    if fields == BASE_FIELDS:
        return False
    if fields != BASE_FIELDS | VPS_FIELDS or identity["executor"] != "windows-vps":
        raise ValueError("Missing or foreign native executor provenance fields")
    if not isinstance(identity["computer"], str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,63}", identity["computer"]):
        raise ValueError("VPS evidence requires its exact computer identity")
    if not identity["headSha"] == identity["checkoutSha"] == identity["workflowSha"]:
        raise ValueError("VPS source and committed test recipe must be the same candidate")
    return True


def make_identity(head, computer, run_id, attempt, job):
    # workflowSha is the retained protocol field for the committed test recipe;
    # executor explicitly identifies this as a VPS run, never a GitHub run.
    identity = {"headSha": head, "checkoutSha": head, "workflowSha": head,
                "runId": run_id, "runAttempt": attempt, "job": job,
                **extension("windows-vps", computer)}
    is_vps(identity)
    if not re.fullmatch(r"[0-9a-f]{40}", head) or any(
            not isinstance(value, str) or not re.fullmatch(r"[1-9][0-9]*", value) for value in (run_id, attempt)):
        raise ValueError("VPS batch needs exact commit, positive run ID and attempt")
    return identity
