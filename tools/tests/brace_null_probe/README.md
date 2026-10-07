# Receipt-probe adversarial callers

These minimal Minecraft-shaped callers exercise the **actual checked-in**
BraceNullCaptureProbe, BraceNullPhaseContract and BraceNullTransformOracle against
freshly compiled real samplers. They are not Minecraft, mixin application, renderer,
GPU, pixel or native acceptance evidence. They are never included in a mod source set.

The original false-accept review used the same minimal caller approach. This suite
adds genuine probe entry/submission/deferred-call ordering and independent control
outputs, then tests no-op, idle, all-999/wrong body palette, wrong/identity hand
matrix, missing baseline, missing native hand entry and borrowed-stack failures.

Control outputs use the existing pure body/view helpers and original-runtime
PoseStack/JOML/Mth to model the documented adapter operation. The checked probe's
expected result is calculated separately by BraceNullTransformOracle. The probe
never invokes MastersArtPose.apply/firstPerson to manufacture its own expected data.

The displayed-item extension additionally exercises the original native `ItemTransform`
math and simulated callback order: ItemInHandLayer entry, real item-submit entry,
nonempty displayed-quads callback, scope closure and deferred body consumption.
It rejects missing third-person items, omitted/reversed Unmoved hilt transforms,
wrong model/item/stack/hand, absent or empty deep draws, wrong displayed matrices,
detached/duplicate callbacks and swallowed failures. Runtime `ItemDisplayContext`
and quad caller shapes are explicitly stubbed to keep signed/unsigned packages
separate. No signed dependency JAR is patched or rewritten.

The separate original-runtime world-item oracle harness uses the actual vanilla
PlayerModel and ItemTransform implementations without these caller stubs.
