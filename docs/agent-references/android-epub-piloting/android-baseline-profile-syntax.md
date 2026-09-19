# Android Baseline Profile Syntax

A `baseline-prof.txt` file tells ART which classes and methods to AOT-compile
at install time. The format is a plain text file processed by the Android
Gradle Plugin.

## Correct class rules

```
Lcom/jdluu/leafline/LeaflineApplication/**
```

- `L<class_name>/**` — includes all methods
- No flags on class rules

## What fails

```
HSPL com/jdluu/leafline/LeaflineApplication
```

AGP rejects: `Class rules don't support flags, but 'HSP' were specified`

## Diagnostic

Only `assembleRelease` validates the profile — debug builds pass silently.

## Repair

Convert HSPL lines to valid class rules:
```python
for l in open('baseline-prof.txt'):
    if l.startswith('HSPL '):
        print(f"L{l.strip().split(' ', 1)[1]}/**")
```

## Source

Discovered 2026-08-24: HSPL flags on class rules blocked release builds.