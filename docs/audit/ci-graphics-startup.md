# Full native CI graphics startup

## Observed failure

GitHub Build run 37149262042 for commit00cd3475 completed its ordinary build, generated-assets
check, unit tests and launcher packaging successfully. Its separate full-client job was cancelled
after the90-minute limit. The retained job log establishes that no gameplay test started:
Minecraft26.3 failed its OpenGL hidden-window creation with `Couldn't find matching GLX visual`,
then its Vulkan backend failed because the installed implementation lacked `VK_KHR_surface`.
It remained at startup from19:51:14Z until cancellation at21:19:54Z.

Authoritative log: `artifacts/review/field-signature-fusions/ci-00cd-native.log`.
The outcome manifest correctly recorded all122 descriptor suites as unverified, not passed.

## Correction

The workflow installs Mesa's EGL library, selects SDL3's documented `SDL_VIDEO_FORCE_EGL=1`
route and supplies an explicit24-bit Xvfb display. SDL documents this hint as selecting EGL
instead of the platform GL context API: [SDL3 EGL hint](https://wiki.libsdl.org/SDL3/SDL_HINT_VIDEO_FORCE_EGL).
This is a targeted attempt to avoid the observed GLX visual failure; a Linux native run must
confirm it. Local Windows rendering results cannot prove the Linux correction.

`tools/run_client_ci.py` launches the same full descriptor, with animation gallery enabled by
the workflow. It does not select a shorter suite. It retains the streaming log and returns a
failure promptly if both backends fail. A single backend failure allows the normal fallback.
Only its own started process group is terminated; no unrelated Java processes are enumerated
or stopped. Gradle runs without a reusable daemon for this isolated CI invocation.

## Verification and remaining gate

Replaying the actual cancelled-job log returned the expected diagnostic failure2. Replaying
the accepted clean-projectile native log returned0 for absence of a dual-backend failure.
These are log-detector checks, not gameplay passes. The full Linux gate and EGL initialization
remain unverified until the updated workflow runs successfully on GitHub.

## Subsequent Linux run

Build run 37155328593 at commit 65256266 successfully initialized OpenGL with
Mesa 25.2.8 on the GitHub runner and entered real client gameplay testing.
The ordinary build job passed. The full native job failed after 22 minutes
at `HomeProjectsTest`, with `reading lantern grants its utility`.
The graphics startup correction is therefore verified; the full gameplay
descriptor is not green.

The retained log is
`artifacts/review/field-signature-fusions/ci-65256266-failed.log`.
The lantern's actual server code applies a 20-tick charge cooldown and updates
at `gameTime % 20 == 0`. Depending on the starting tick, a successful update can
take up to 39 ticks. The test previously waited only 25. Its development
correction checks that the lantern is configured and charged, then allows the
full 40-tick cooldown/update window. Production timing and utility are unchanged.
Focused native acceptance and a subsequent full Linux run are separate gates.

## Current combined milestone CI

At commit `c30ae0283aa10032546760bc796a6920de4abf18`, ordinary Build run
37157807760 and Player Guide run 37157807720 passed. The full Linux native
job initialized Mesa OpenGL successfully, then failed at `FungalNurseryTest`:
`Gathering the same visitor preserves its real forage rest`. The retained log
is `artifacts/review/life-fieldcraft-milestone/ci-c30ae028-failed.log`.
The corrected focused nursery fixture passed on Windows in 2 min 28 s, including
real crouch admissions, Survival dew pickup, exact forage-rest preservation and
full earned acquisition/restart. A subsequent full Linux run remains the open
gate; focused Windows passes do not establish full Linux acceptance.

## Void milestone Linux run

Build run 37160112771 at commit f4d778837e60412468bb60dfe9a1c9d7e77f9590
passed its ordinary build, unit/assets checks and packaging; Guide 37160112802
passed. The full gameplay job failed after 3 min 11 s at the first nursery
gather's new earned-inventory assertion. Real crouch/LOS admission succeeded.
The drop is a genuine ItemEntity with randomized horizontal velocity, so waiting
in place beside the moving visitor cannot establish pickup.

The corrected fixture tracks the newly emitted UUID and uses bounded actual
client movement to reach it, preserving pickup delay, ownership and real
Survival inventory provenance. Its first focused Windows pass took 2 min 23 s.
Parent review also corrected callback races by checking inventory and tracked
entity existence together; the final focused pass took 2 min 20 s; a renewed Linux descriptor
remains the open gate. Retained Linux log: artifacts/review/void-material-milestone/
ci-f4d77883-failed.log.
