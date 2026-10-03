# Full native CI graphics startup

## Observed failure

GitHub Build run37149262042 for commit00cd3475 completed its ordinary build, generated-assets
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
