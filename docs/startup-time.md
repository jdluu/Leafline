# Startup time measurement (#26)

Measured on Pixel 7 (panther), Android 16, debug build, 2026-08-23.
Method: `adb shell am force-stop` then `adb shell am start -W`, reading
`TotalTime` from ActivityTaskManager (`Displayed ... +Xms` in logcat).
Five consecutive cold starts per configuration.

## Results

| Configuration | Cold start (TotalTime) |
|---|---|
| Baseline (WorkManager init + sync schedule in Application.onCreate) | 1578 / 1581 / 1591 / 1593 / 1595 ms |
| Deferred sync scheduling (this change) | 1579 / 1584 / 1587 / 1594 / 1668 ms |

System baseline for comparison: Android Settings cold start ≈ 742 ms on the
same device.

## Analysis

- Deferring `SyncWorker.schedule()` off the main thread did **not** move the
  number. WorkManager's initialization is lazy inside the enqueue call and
  the schedule itself was already cheap; the deferred version removes it
  from the startup path as a matter of hygiene, not measurable wins.
- The ~1.6 s is dominated by debug-build costs: JIT-compiling a large
  Compose + Readium class graph on first run, plus the splash screen holding
  until first composition of the Library grid (cover bitmaps decode
  asynchronously via `produceState`, so they do not block first frame).
- A release build with R8 will land substantially lower; baseline profiles
  (#27) address the remaining JIT cost for reader-open paths.

## Follow-ups

- Re-measure on a release (`minifyEnabled`) build before optimizing further.
- #27 baseline profiles are the expected lever for the remaining gap.
- No lazy-initialization changes were needed: OPDS and sync dependencies are
  already constructed lazily via `LeaflineDependencyHolder` on first access,
  and cover decoding is off the main thread.
